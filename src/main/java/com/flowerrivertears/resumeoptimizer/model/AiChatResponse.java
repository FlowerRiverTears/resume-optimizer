package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String answer;
    private String thinking;
    private String provider;
    private String model;
    private List<SearchResult> searchResults;
    private long responseTimeMs;
}
