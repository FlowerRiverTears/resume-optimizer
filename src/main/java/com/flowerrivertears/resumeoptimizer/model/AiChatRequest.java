package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    private String message;
    private String context;
    private String provider;
    private List<Map<String, String>> history;
    private String keyId;
    private String apiKey;
    private String baseUrl;
    private String modelName;
}
