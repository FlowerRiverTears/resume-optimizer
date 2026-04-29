package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyInfo {
    private String keyId;
    private String provider;
    private String modelName;
    private String baseUrl;
    private String maskedKey;
    private boolean valid;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
