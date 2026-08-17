package com.keepsake.backend.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keepsake.backend.auth.AuthDtos.AuthResponse;
import com.keepsake.backend.auth.AuthDtos.LoginRequest;
import com.keepsake.backend.auth.AuthDtos.RegisterRequest;
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

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ClassSetRepository classSetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                       SchoolRepository schoolRepository,
                       ClassSetRepository classSetRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.classSetRepository = classSetRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
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

        School school = schoolRepository.findById(req.schoolId())
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

        String verifyLink = "/verify-email?token=" + user.getEmailVerificationToken();
        mailService.sendVerificationLink(email, verifyLink);

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
