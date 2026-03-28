package com.flowerrivertears.resumeoptimizer.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResponse {
    private int atsScore;
    private int matchScore;
    private List<String> foundKeywords;
    private List<String> missingKeywords;
    private List<String> suggestions;
    private Map<String, Integer> keywordFrequency;
    private StructureInfo structure;

    @Data
    @Builder
    public static class StructureInfo {
        private boolean hasContactInfo;
        private boolean hasSummary;
        private boolean hasExperience;
        private boolean hasEducation;
        private boolean hasSkills;
        private int totalWords;
    }
}
