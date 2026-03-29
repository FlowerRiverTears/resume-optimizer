package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.SkillGap;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简历模板生成服务
 * 生成优化版简历模板，带高亮标记
 */
@Service
public class ResumeTemplateService {

    // ==================== 模板段落 ====================
    private static final Map<String, String> TEMPLATE_SECTIONS = new LinkedHashMap<>();

    static {
        // 个人简介模板
        TEMPLATE_SECTIONS.put("summary", """
            个人简介

            拥有X年开发经验的软件工程师，专注于现代Web开发技术。具备扎实的编程基础和良好的代码风格，热衷于技术钻研与团队协作。善于解决复杂问题，追求高性能、高可用的系统设计。

            【优化建议】根据您的实际经历修改年数和技术方向，突出您的独特优势。
            """);

        // 项目经历模板
        TEMPLATE_SECTIONS.put("project", """
            项目名称：XXX系统
            项目时间：2023.01 - 2023.06
            项目描述：简要描述项目背景、目标和您的角色
            技术栈：使用的关键技术（不超过5个）
            工作职责：
            • 负责模块A的开发，实现了XXX功能
            • 优化算法，性能提升XX%
            • 解决XX技术难题，获得XXX成果
            项目成果：
            • 系统上线后，用户量达XX万
            • 获得客户/领导好评

            【优化建议】使用STAR法则（Situation-Task-Action-Result）描述项目经历，量化您的成果。
            """);

        // 工作经历模板
        TEMPLATE_SECTIONS.put("experience", """
            公司名称 | 职位名称
            2020.07 - 至今

            • 负责公司核心产品的技术研发工作
            • 主导XX系统架构设计，技术选型
            • 带领X人团队完成XX项目交付
            • 优化核心模块，性能提升XX%
            • 建立代码规范和Code Review流程

            【优化建议】突出您对团队的贡献和业务价值，使用具体数字支撑。
            """);

        // 技能清单模板
        TEMPLATE_SECTIONS.put("skills", """
            技能清单

            编程语言：Java / Python / JavaScript
            前端技术：Vue.js / React / HTML5 / CSS3
            后端框架：Spring Boot / Django / Node.js
            数据库：MySQL / Redis / MongoDB
            开发工具：Git / Docker / Linux

            【优化建议】按熟练程度分类，控制在8-12项核心技能。
            """);
    }

    // ==================== 核心方法 ====================

    /**
     * 生成优化版简历
     * @param originalResume 原始简历文本
     * @param missingSkills 缺失技能列表
     * @param skillGaps 技能差距详情
     * @return 带标记的优化版简历（HTML格式）
     */
    public String generateOptimizedResume(String originalResume,
                                          Set<String> missingSkills,
                                          List<SkillGap> skillGaps) {
        StringBuilder optimized = new StringBuilder();

        // 分析原始简历结构
        Map<String, String> sections = parseResumeSections(originalResume);

        // 生成优化后的简历
        optimized.append("<!-- 优化版简历 -->\n\n");

        // 1. 联系信息（保留或提示添加）
        if (sections.containsKey("contact")) {
            optimized.append(sections.get("contact"));
        } else {
            optimized.append("[📝 建议添加联系方式]\n");
            optimized.append("姓名 | 手机号 | 邮箱\n\n");
        }

        // 2. 个人简介（优化或添加）
        if (sections.containsKey("summary")) {
            optimized.append("<div class='section-original'>\n");
            optimized.append("<h3>个人简介</h3>\n");
            optimized.append(highlightSkills(sections.get("summary"), missingSkills));
            optimized.append("\n</div>\n");
        } else {
            optimized.append("<div class='section-suggestion'>\n");
            optimized.append("<h3>个人简介（建议添加）</h3>\n");
            optimized.append("<pre>").append(TEMPLATE_SECTIONS.get("summary")).append("</pre>\n");
            optimized.append("</div>\n");
        }

        // 3. 技能清单（优化显示）
        optimized.append("<div class='section-skills'>\n");
        optimized.append("<h3>技能清单</h3>\n");
        optimized.append(generateSkillsSection(originalResume, missingSkills, skillGaps));
        optimized.append("</div>\n\n");

        // 4. 项目经历（保留或建议添加）
        if (sections.containsKey("project")) {
            optimized.append("<div class='section-original'>\n");
            optimized.append("<h3>项目经历</h3>\n");
            optimized.append(highlightSkills(sections.get("project"), missingSkills));
            optimized.append("\n</div>\n");
        } else {
            optimized.append("<div class='section-suggestion'>\n");
            optimized.append("<h3>项目经历（建议添加）</h3>\n");
            optimized.append("<pre>").append(TEMPLATE_SECTIONS.get("project")).append("</pre>\n");
            optimized.append("</div>\n");
        }

        // 5. 工作经历（保留）
        if (sections.containsKey("experience")) {
            optimized.append("<div class='section-original'>\n");
            optimized.append("<h3>工作经历</h3>\n");
            optimized.append(highlightSkills(sections.get("experience"), missingSkills));
            optimized.append("\n</div>\n");
        }

        // 6. 教育背景（保留）
        if (sections.containsKey("education")) {
            optimized.append("<div class='section-original'>\n");
            optimized.append("<h3>教育背景</h3>\n");
            optimized.append(sections.get("education"));
            optimized.append("\n</div>\n");
        }

        // 7. 技能补充建议
        if (!missingSkills.isEmpty()) {
            optimized.append("\n<div class='section-gaps'>\n");
            optimized.append("<h3>建议补充的技能</h3>\n");
            optimized.append("<ul>\n");
            for (SkillGap gap : skillGaps.stream().limit(5).toList()) {
                optimized.append("<li>");
                optimized.append("<strong>").append(gap.getSkill()).append("</strong>");
                optimized.append(" - ").append(gap.getSuggestion());
                optimized.append("</li>\n");
            }
            optimized.append("</ul>\n");
            optimized.append("</div>\n");
        }

        return optimized.toString();
    }

    /**
     * 解析简历结构
     */
    private Map<String, String> parseResumeSections(String resume) {
        Map<String, String> sections = new LinkedHashMap<>();

        if (resume == null || resume.isEmpty()) {
            return sections;
        }

        String[] lines = resume.split("\n");
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();

        Set<String> sectionHeaders = new HashSet<>(Arrays.asList(
            "个人简介", "简介", "自我评价", "summary", "profile",
            "项目经历", "项目经验", "project",
            "工作经历", "工作经验", "work", "experience",
            "教育背景", "教育经历", "education",
            "技能", "技能清单", "skills", "技术栈",
            "联系方式", "contact"
        ));

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 检查是否是章节标题
            final String currentLine = line;
            boolean isHeader = sectionHeaders.stream().anyMatch(h ->
                currentLine.toLowerCase().contains(h.toLowerCase()));

            if (isHeader && currentLine.length() < 20) {
                // 保存上一个章节
                if (currentSection != null && currentContent.length() > 0) {
                    sections.put(currentSection, currentContent.toString().trim());
                }
                // 开始新章节
                currentSection = currentLine.toLowerCase();
                currentContent = new StringBuilder();
            } else {
                currentContent.append(currentLine).append("\n");
            }
        }

        // 保存最后一个章节
        if (currentSection != null && currentContent.length() > 0) {
            sections.put(currentSection, currentContent.toString().trim());
        }

        return sections;
    }

    /**
     * 高亮显示技能
     */
    private String highlightSkills(String text, Set<String> skills) {
        if (text == null || skills == null) return text;

        String result = text;
        for (String skill : skills) {
            // 简单替换，不使用正则以避免特殊字符问题
            result = result.replaceAll("(?i)(" + Pattern.quote(skill) + ")",
                "<mark class='skill-matched'>$1</mark>");
        }
        return result;
    }

    /**
     * 生成技能部分
     */
    private String generateSkillsSection(String resume, Set<String> missingSkills,
                                        List<SkillGap> skillGaps) {
        StringBuilder sb = new StringBuilder();

        // 从原始简历提取现有技能
        Set<String> existingSkills = extractSkillsFromText(resume);

        // 显示现有技能（高亮匹配的）
        sb.append("<p><strong>已掌握：</strong></p>\n");
        sb.append("<ul class='skills-list'>\n");
        for (String skill : existingSkills) {
            String cssClass = missingSkills.contains(skill) ? "skill-matched" : "skill-keep";
            sb.append("<li class='").append(cssClass).append("'>").append(skill).append("</li>\n");
        }
        sb.append("</ul>\n");

        // 显示建议添加的技能
        if (!skillGaps.isEmpty()) {
            sb.append("<p><strong>建议补充：</strong></p>\n");
            sb.append("<ul class='skills-suggestion'>\n");
            for (SkillGap gap : skillGaps.stream().limit(5).toList()) {
                sb.append("<li class='skill-add'>").append(gap.getSkill());
                sb.append(" <span class='importance'>（重要性：").append(gap.getImportance()).append("/5）</span>");
                sb.append("</li>\n");
            }
            sb.append("</ul>\n");
        }

        return sb.toString();
    }

    /**
     * 从文本提取技能关键词
     */
    private Set<String> extractSkillsFromText(String text) {
        Set<String> skills = new HashSet<>();

        if (text == null) return skills;

        // 常见技能列表
        Set<String> commonSkills = new HashSet<>(Arrays.asList(
            "Java", "Python", "JavaScript", "TypeScript", "C++", "Go", "Rust",
            "Vue", "React", "Angular", "Node.js", "Spring Boot", "Django",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Docker", "Kubernetes",
            "Git", "Linux", "AWS", "RESTful", "API", "HTML", "CSS"
        ));

        for (String skill : commonSkills) {
            if (text.toLowerCase().contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        }

        return skills;
    }

    /**
     * 生成对比视图数据
     */
    public Map<String, Object> generateComparisonView(String originalResume,
                                                       Set<String> matchedSkills,
                                                       Set<String> missingSkills) {
        Map<String, Object> comparison = new LinkedHashMap<>();

        // 原简历关键信息
        comparison.put("originalLength", originalResume != null ? originalResume.length() : 0);
        comparison.put("originalWordCount", originalResume != null ?
            originalResume.split("\\s+").length : 0);

        // 匹配度统计
        comparison.put("matchedCount", matchedSkills.size());
        comparison.put("missingCount", missingSkills.size());
        comparison.put("matchedSkills", matchedSkills);
        comparison.put("missingSkills", missingSkills);

        // 生成优化建议摘要
        List<String> suggestions = new ArrayList<>();

        if (missingSkills.size() > 0) {
            suggestions.add("建议添加 " + missingSkills.size() + " 项关键技能的相关描述");
        }

        if (originalResume == null || originalResume.length() < 300) {
            suggestions.add("简历内容偏少，建议补充更多项目细节");
        }

        comparison.put("suggestions", suggestions);

        return comparison;
    }
}
