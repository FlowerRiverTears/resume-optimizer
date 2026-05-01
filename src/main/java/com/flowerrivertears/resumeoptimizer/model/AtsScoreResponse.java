package com.flowerrivertears.resumeoptimizer.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AtsScoreResponse {

    private int overallScore;
    private String grade;
    private String gradeDescription;

    private JobMatchScore jobMatchScore;
    private StructureScore structureScore;
    private ContentScore contentScore;
    private KeywordScore keywordScore;

    private List<String> matchedSkills;
    private List<String> missingSkills;
    private int matchedCount;
    private int missingCount;

    private Map<String, CategoryDetail> categoryDetails;
    private List<SkillGapDetail> skillGapDetails;
    private List<OptimizationSuggestion> optimizationSuggestions;

    private long responseTimeMs;
    private String provider;
    private String model;
    private String keyId;

    @Data
    @Builder
    public static class JobMatchScore {
        private int score;
        private String level;
        private String description;
        private int totalRequired;
        private int matchedRequired;
    }

    @Data
    @Builder
    public static class StructureScore {
        private int score;
        private String level;
        private boolean hasContact;
        private boolean hasSummary;
        private boolean hasExperience;
        private boolean hasEducation;
        private boolean hasSkills;
        private int wordCount;
        private String wordCountLevel;
    }

    @Data
    @Builder
    public static class ContentScore {
        private int score;
        private String level;
        private int skillCount;
        private int keywordDensity;
        private String densityLevel;
    }

    @Data
    @Builder
    public static class KeywordScore {
        private int score;
        private String level;
        private int totalKeywords;
        private int matchedKeywords;
        private double matchRate;
    }

    @Data
    @Builder
    public static class CategoryDetail {
        private String name;
        private int score;
        private String level;
        private int matched;
        private int total;
        private List<String> matchedSkills;
        private List<String> missingSkills;
    }

    @Data
    @Builder
    public static class SkillGapDetail {
        private String skill;
        private String category;
        private int importance;
        private String reason;
        private String suggestion;
        private List<String> learningResources;
    }

    @Data
    @Builder
    public static class OptimizationSuggestion {
        private String type;
        private String title;
        private String description;
        private String priority;
        private String impact;
    }
}
