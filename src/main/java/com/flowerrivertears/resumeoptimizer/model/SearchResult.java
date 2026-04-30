package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String content;
    private double score;
    private String source;
    private String category;
    private double weightedScore;
    private String retrievalMethod;
    private int chunkIndex;
    private double keywordScore;
    private double vectorScore;
    private double rerankScore;
}
