package com.keepsake.backend.auth;

import java.time.LocalDateTime;

import com.keepsake.backend.school.School;
import com.keepsake.backend.user.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String fullName,
            String nickname,
            @NotNull Long schoolId,
            Long classSetId,
            Integer graduationYear) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record GoogleLoginRequest(@NotBlank String idToken) {
    }

    /** Whether the Google sign-in button should be shown, and with which client id. */
    public record OAuthConfig(boolean enabled, String clientId) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    /** Auth response: JWT + identity; verifyLink is only present in dev (mail disabled). */
    public record AuthResponse(String token, Identity user, String verifyLink) {

        public static Identity identity(User u) {
            School school = u.getSchool();
            return new Identity(
                    u.getId(), u.getEmail(), u.getFullName(), u.getNickname(), u.getUsername(),
                    u.getAvatarUrl(), u.getBio(),
                    school != null ? school.getId() : null,
                    school != null ? school.getName() : null,
                    u.getGraduationYear(),
                    u.getRole().name(),
                    u.isVerified(),
                    u.getCreatedAt());
        }
    }

    public record Identity(
            Long id, String email, String fullName, String nickname, String username,
            String avatarUrl, String bio,
            Long schoolId, String schoolName, Integer graduationYear,
            String role, boolean verified, LocalDateTime createdAt) {
    }
}
