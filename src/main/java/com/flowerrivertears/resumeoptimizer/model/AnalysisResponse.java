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

    // 新增：技能分类匹配度（用于雷达图）
    private Map<String, CategoryScore> categoryScores;

    // 新增：技能差距详情列表
    private List<SkillGap> skillGaps;

    // 新增：优化版简历（带高亮标记的HTML）
    private String optimizedResume;

    // 新增：优化建议详情
    private List<OptimizationTip> optimizationTips;

    @Data
    @Builder
    public static class CategoryScore {
        private String category;        // 分类名称
        private int matchedCount;       // 匹配技能数
        private int totalCount;        // 要求技能数
        private int score;             // 匹配度百分比
    }

    @Data
    @Builder
    public static class OptimizationTip {
        private String type;           // 类型: add/improve/delete
        private String originalText;    // 原文
        private String suggestedText;   // 建议修改
        private String reason;         // 修改原因
    }

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
