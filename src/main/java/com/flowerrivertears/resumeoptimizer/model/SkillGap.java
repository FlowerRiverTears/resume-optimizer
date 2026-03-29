package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGap {
    private String skill;           // 技能名称
    private String category;        // 分类 (frontend/backend/database/devops)
    private int importance;         // 重要程度 1-5
    private String reason;          // 缺失原因说明
    private String suggestion;       // 学习建议
}
