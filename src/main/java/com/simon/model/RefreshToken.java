package com.simon.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Entity
@Table(name = "RefreshTokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private String tokenHash;

    private LocalDateTime issuedAt = LocalDateTime.now();
    private LocalDateTime expiresAt;
    private Boolean revoked = false;
    private String replacedByToken;
}
