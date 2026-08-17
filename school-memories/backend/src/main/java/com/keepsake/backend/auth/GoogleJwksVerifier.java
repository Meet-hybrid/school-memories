package com.keepsake.backend.auth;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.keepsake.backend.common.ApiException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;

/**
 * Verifies Google ID tokens locally against Google's public key set
 * (https://www.googleapis.com/oauth2/v3/certs), so every login is a fast
 * local check rather than a round-trip to Google.
 *
 * The key set is fetched lazily and cached for 24 hours; if a token arrives
 * with an unknown {@code kid} the cache is refreshed once to pick up a rotation.
 */
@Service
public class GoogleJwksVerifier implements GoogleIdTokenVerifier {

    private static final String CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ACCEPTED_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final String clientId;
    private final RestClient restClient;

    private volatile CachedKeys cached;

    public GoogleJwksVerifier(@Value("${keepsake.google.client-id:}") String clientId,
                              RestClient.Builder restClientBuilder) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public GoogleUser verify(String idToken) {
        if (clientId.isBlank()) {
            throw ApiException.badRequest("Google sign-in is not configured on this server");
        }
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(this::locateKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
            if (!ACCEPTED_ISSUERS.contains(claims.getIssuer())) {
                throw ApiException.unauthorized("Invalid Google sign-in");
            }
            if (!claims.getAudience().contains(clientId)) {
                throw ApiException.unauthorized("Invalid Google sign-in");
            }
            String email = claims.get("email", String.class);
            if (email == null || email.isBlank()) {
                throw ApiException.unauthorized("Your Google account has no email address");
            }
            Boolean emailVerified = claims.get("email_verified", Boolean.class);
            if (Boolean.FALSE.equals(emailVerified)) {
                throw ApiException.unauthorized("Your Google email is not verified");
            }
            return new GoogleUser(claims.getSubject(), email.toLowerCase(Locale.ROOT),
                    emailVerified == null || emailVerified,
                    claims.get("name", String.class),
                    claims.get("picture", String.class));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.unauthorized("Invalid Google sign-in");
        }
    }

    private Key locateKey(Header header) {
        Object kid = header.get("kid");
        Map<String, Key> keys = currentKeys();
        Key key = kid == null ? null : keys.get(kid.toString());
        if (key == null) {
            // Google may have rotated keys since our cache was filled; refresh once.
            keys = refreshKeys();
            key = kid == null ? null : keys.get(kid.toString());
        }
        if (key == null) {
            throw new JwtException("Unknown Google signing key");
        }
        return key;
    }

    private Map<String, Key> currentKeys() {
        CachedKeys snapshot = cached;
        if (snapshot != null && snapshot.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return snapshot.keys();
        }
        return refreshKeys();
    }

    private synchronized Map<String, Key> refreshKeys() {
        CachedKeys snapshot = cached;
        if (snapshot != null && snapshot.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return snapshot.keys();
        }
        String json = restClient.get().uri(CERTS_URL).retrieve().body(String.class);
        JwkSet set = Jwks.setParser().build().parse(json);
        Map<String, Key> keys = new HashMap<>();
        for (Jwk<?> jwk : set.getKeys()) {
            if (jwk.getId() != null) {
                keys.put(jwk.getId(), jwk.toKey());
            }
        }
        if (keys.isEmpty()) {
            throw new IllegalStateException("Google JWKS contained no usable keys");
        }
        cached = new CachedKeys(Instant.now(), keys);
        return keys;
    }

    private record CachedKeys(Instant fetchedAt, Map<String, Key> keys) {
    }
}
