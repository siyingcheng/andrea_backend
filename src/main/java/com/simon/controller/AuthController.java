package com.simon.controller;

import com.simon.dto.AuthRequests;
import com.simon.dto.AuthResponses;
import com.simon.model.RefreshToken;
import com.simon.model.User;
import com.simon.service.TokenService;
import com.simon.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody AuthRequests.RegisterRequest req) {
        if (userService.findByUsername(req.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username exists");
        }
        userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody AuthRequests.LoginRequest req, HttpServletResponse response) {
        User u = userService.findByUsername(req.getUsername());
        if (u == null || !userService.checkPassword(u, req.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        String access = tokenService.generateAccessToken(u);
        RefreshToken rt = tokenService.createRefreshToken(u);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", rt.getReplacedByToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(60L * 60L * 24L * 30L)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok(new AuthResponses(access, tokenService.getAccessExpiresIn(), "Bearer"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("refreshToken".equals(c.getName())) {
                    token = c.getValue();
                }
            }
        }
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");
        var opt = tokenService.findByTokenHash(token);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        RefreshToken rt = opt.get();
        if (rt.getRevoked()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Revoked refresh token");
        if (rt.getExpiresAt().isBefore(java.time.LocalDateTime.now()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Expired");
        String access = tokenService.generateAccessToken(rt.getUser());
        return ResponseEntity.ok(new AuthResponses(access, tokenService.getAccessExpiresIn(), "Bearer"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("refreshToken".equals(c.getName())) {
                    token = c.getValue();
                }
            }
        }
        if (token != null) {
            var opt = tokenService.findByTokenHash(token);
            opt.ifPresent(tokenService::revoke);
        }
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@Validated @RequestBody AuthRequests.PasswordResetRequest req) {
        return ResponseEntity.ok().body(java.util.Map.of("message", "If the email exists, a reset link has been sent."));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Validated @RequestBody AuthRequests.PasswordResetConfirm req) {
        return ResponseEntity.ok().body(java.util.Map.of("message", "Password updated"));
    }
}

