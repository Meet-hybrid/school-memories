package com.keepsake.backend.auth;

/**
 * Verifies a Google ID token (from Google Identity Services) and returns the
 * profile claims Keepsake needs. Implementations must fully validate the token
 * (signature, issuer, audience, expiry) before returning a {@link GoogleUser}.
 */
public interface GoogleIdTokenVerifier {

    /**
     * @param idToken the raw credential from Google's sign-in flow
     * @return the verified profile claims
     * @throws com.keepsake.backend.common.ApiException when the token is invalid or
     *                                                  Google sign-in is not configured
     */
    GoogleUser verify(String idToken);

    /** Verified profile claims extracted from a Google ID token. */
    record GoogleUser(String sub, String email, boolean emailVerified, String name, String picture) {
    }
}
