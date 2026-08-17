package com.keepsake.backend.auth;

import com.keepsake.backend.auth.AuthDtos.AuthResponse;
import com.keepsake.backend.auth.AuthDtos.ForgotPasswordRequest;
import com.keepsake.backend.auth.AuthDtos.GoogleLoginRequest;
import com.keepsake.backend.auth.AuthDtos.Identity;
import com.keepsake.backend.auth.AuthDtos.LoginRequest;
import com.keepsake.backend.auth.AuthDtos.RegisterRequest;
import com.keepsake.backend.auth.AuthDtos.ResetPasswordRequest;
import com.keepsake.backend.common.ApiException;
import com.keepsake.backend.user.User;
import com.keepsake.backend.user.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final String googleClientId;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          @Value("${keepsake.google.client-id:}") String googleClientId) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest req) {
        return authService.googleLogin(req.idToken());
    }

    /** Public: tells the frontend whether to render the Google button and with which client id. */
    @GetMapping("/oauth-config")
    public AuthDtos.OAuthConfig oauthConfig() {
        boolean enabled = !googleClientId.isBlank();
        return new AuthDtos.OAuthConfig(enabled, enabled ? googleClientId : null);
    }

    @PostMapping("/logout")
    public void logout() {
        // Stateless JWT auth: the client simply discards the token.
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.requestPasswordReset(req.email());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public Identity me(Authentication authentication) {
        UserDetails details = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findById(Long.valueOf(details.getUsername()))
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        return AuthResponse.identity(user);
    }
}
