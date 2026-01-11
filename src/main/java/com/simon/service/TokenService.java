package com.simon.service;

import com.simon.model.RefreshToken;
import com.simon.model.User;
import com.simon.repository.RefreshTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-exp:900}")
    private long accessTokenSeconds;

    @Value("${app.jwt.refresh-exp-days:30}")
    private long refreshExpDays;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTokenSeconds)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public long getAccessExpiresIn() {
        return accessTokenSeconds;
    }

    public RefreshToken createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        String tokenHash = hash(token);
        RefreshToken rt = new RefreshToken()
                .setUser(user)
                .setTokenHash(tokenHash)
                .setIssuedAt(LocalDateTime.now())
                .setExpiresAt(LocalDateTime.now().plusDays(refreshExpDays))
                .setRevoked(false);
        refreshTokenRepository.save(rt);
        rt.setReplacedByToken(token);
        return rt;
    }

    private String hash(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes()).toString();
    }

    public Optional<RefreshToken> findByTokenHash(String token) {
        return refreshTokenRepository.findByTokenHash(hash(token));
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}

