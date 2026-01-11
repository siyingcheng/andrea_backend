package com.simon.dto;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AuthResponses {
    private String accessToken;
    private long expiresIn;
    private String tokenType = "Bearer";
}

