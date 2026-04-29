package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyValidateRequest {
    private String apiKey;
    private String provider;
    private String baseUrl;
    private String modelName;
}
