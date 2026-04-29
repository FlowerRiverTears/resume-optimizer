package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyValidateResponse {
    private boolean valid;
    private String provider;
    private String modelName;
    private String message;
    private String keyId;
}
