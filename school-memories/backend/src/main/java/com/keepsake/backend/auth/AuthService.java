package com.keepsake.backend.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.auth.AuthDtos.AuthResponse;
import com.keepsake.backend.auth.AuthDtos.LoginRequest;
import com.keepsake.backend.auth.AuthDtos.RegisterRequest;
import com.keepsake.backend.auth.GoogleIdTokenVerifier.GoogleUser;
import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.common.MailService;
import com.keepsake.backend.security.JwtService;
import com.keepsake.backend.school.ClassSet;
import com.keepsake.backend.school.ClassSetRepository;
import com.keepsake.backend.school.School;
import com.keepsake.backend.school.SchoolRepository;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

@Service
public class AuthService {

    @Value("${keepsake.school.name:Character Training Secondary School}")
    private String configuredSchoolName;

    @Value("${keepsake.school.invite-code:}")
    private String configuredInviteCode;

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSetRepository classSetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthService(UserRepository userRepository,
                       SchoolRepository schoolRepository,
                       ClassSetRepository classSetRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MailService mailService,
                       GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.classSetRepository = classSetRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (!configuredInviteCode.isBlank()
                && !configuredInviteCode.equals(req.inviteCode() == null ? "" : req.inviteCode().trim())) {
            throw ApiException.forbidden("A valid school invite code is required");
        }
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.isBlank()) {
            throw ApiException.badRequest("Email is required");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        if (req.password() == null || req.password().length() < 8) {
            throw ApiException.badRequest("Password must be at least 8 characters");
        }
        if (req.fullName() == null || req.fullName().isBlank()) {
            throw ApiException.badRequest("Full name is required");
        }

        School school = schoolRepository.findByNameIgnoreCase(configuredSchoolName)
                .orElseThrow(() -> ApiException.badRequest("Unknown school"));
        ClassSet set = null;
        if (req.classSetId() != null) {
            set = classSetRepository.findById(req.classSetId())
                    .filter(cs -> cs.getSchool().getId().equals(school.getId()))
                    .orElseThrow(() -> ApiException.badRequest("Unknown set for this school"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setNickname(clean(req.nickname()));
        user.setSchool(school);
        user.setClassSet(set);
        user.setGraduationYear(req.graduationYear());
        user.setUsername(uniqueUsername(email, user.getFullName()));
        user.setVerified(false);
        user.setActive(true);
        user.setEmailVerificationToken(UUID.randomUUID().toString().replace("-", ""));
        user.setVerificationTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        userRepository.save(user);

        String verifyPath = "/verify-email?token=" + user.getEmailVerificationToken();
        mailService.sendVerificationLink(email, verifyPath);
        // The link is only returned by the API in dev mode (mail disabled);
        // with real mail the user receives it in their inbox instead.
        String verifyLink = mailService.isEnabled() ? null : verifyPath;

        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, AuthResponse.identity(user), verifyLink);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!user.isActive()) {
            throw ApiException.forbidden("This account has been deactivated");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, AuthResponse.identity(user), null);
    }

    /**
     * Signs in (or creates) a user from a Google ID token. The email is the
     * identity key: a Google sign-in joins an existing password account with the
     * same email. New accounts have no school yet — the frontend routes them to
     * onboarding so they can pick one.
     */
    @Transactional
    public AuthResponse googleLogin(String idToken) {
        GoogleUser google = googleIdTokenVerifier.verify(idToken);

        User user = userRepository.findByEmailIgnoreCase(google.email()).orElse(null);
        if (user == null) {
            String email = google.email().trim().toLowerCase();
            String fullName = google.name() == null || google.name().isBlank()
                    ? email.substring(0, email.indexOf('@'))
                    : google.name().trim();
            user = new User();
            user.setEmail(email);
            // Random unguessable password: this account can only sign in via Google.
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setFullName(fullName);
            user.setUsername(uniqueUsername(email, fullName));
            user.setAvatarUrl(google.picture());
            user.setVerified(google.emailVerified());
            user.setActive(true);
            userRepository.save(user);
        } else {
            if (!user.isActive()) {
                throw ApiException.forbidden("This account has been deactivated");
            }
            // Google verified this email; adopt a profile picture if the user has none.
            user.setVerified(true);
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && google.picture() != null) {
                user.setAvatarUrl(google.picture());
            }
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, AuthResponse.identity(user), null);
    }

    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest("Missing verification token");
        }
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification link"));
        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            throw ApiException.badRequest("This verification link has expired");
        }
        user.setVerified(true);
        user.setEmailVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            user.setPasswordResetToken(UUID.randomUUID().toString().replace("-", ""));
            user.setResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            userRepository.save(user);
            mailService.sendPasswordResetLink(user.getEmail(), "/reset-password?token=" + user.getPasswordResetToken());
        });
        // Always return success to avoid leaking which emails exist.
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw ApiException.badRequest("Missing reset token");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest("Password must be at least 8 characters");
        }
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw ApiException.badRequest("This reset link has expired");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private static String clean(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    /** Derives a safe, unique username from the email or name (e.g. ada.obi, ada.obi2). */
    private String uniqueUsername(String email, String fullName) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase()
                .replaceAll("[^a-z0-9_.]", "")
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");
        if (base.length() < 3) {
            base = fullName.toLowerCase().replaceAll("[^a-z0-9]", "").replaceAll("\\s+", "");
        }
        if (base.length() < 3) {
            base = "classmate";
        }
        if (base.length() > 30) {
            base = base.substring(0, 30);
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}
