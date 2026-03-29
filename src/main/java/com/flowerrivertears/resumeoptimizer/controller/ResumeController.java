package com.flowerrivertears.resumeoptimizer.controller;

import com.flowerrivertears.resumeoptimizer.model.AnalysisRequest;
import com.flowerrivertears.resumeoptimizer.model.AnalysisResponse;
import com.flowerrivertears.resumeoptimizer.model.SkillGap;
import com.flowerrivertears.resumeoptimizer.service.ResumeAnalysisService;
import com.flowerrivertears.resumeoptimizer.service.ResumeTemplateService;
import com.flowerrivertears.resumeoptimizer.util.FileParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ResumeController {

    @Autowired
    private ResumeAnalysisService analysisService;

    @Autowired
    private FileParser fileParser;

    @Autowired
    private ResumeTemplateService templateService;

    /**
     * 简历文件上传解析
     */
    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            String content = fileParser.parseFile(file);
            response.put("success", true);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 简历文本分析
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalysisRequest request) {
        AnalysisResponse result = analysisService.analyze(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成优化版简历
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeResume(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String resumeText = (String) request.get("resumeText");
            @SuppressWarnings("unchecked")
            List<String> missingSkillsList = (List<String>) request.get("missingSkills");
            Set<String> missingSkills = missingSkillsList != null ?
                new HashSet<>(missingSkillsList) : new HashSet<>();

            // 重建 SkillGap 列表
            List<SkillGap> skillGaps = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> gapsData = (List<Map<String, Object>>) request.get("skillGaps");
            if (gapsData != null) {
                for (Map<String, Object> gapData : gapsData) {
                    SkillGap gap = SkillGap.builder()
                            .skill((String) gapData.get("skill"))
                            .category((String) gapData.get("category"))
                            .importance(gapData.get("importance") != null ?
                                ((Number) gapData.get("importance")).intValue() : 3)
                            .suggestion((String) gapData.get("suggestion"))
                            .reason((String) gapData.get("reason"))
                            .build();
                    skillGaps.add(gap);
                }
            }

            // 生成优化版简历
            String optimizedResume = templateService.generateOptimizedResume(
                resumeText, missingSkills, skillGaps);

            // 生成对比视图数据
            @SuppressWarnings("unchecked")
            List<String> matchedSkillsList = (List<String>) request.get("matchedSkills");
            Set<String> matchedSkills = matchedSkillsList != null ?
                new HashSet<>(matchedSkillsList) : new HashSet<>();

            Map<String, Object> comparison = templateService.generateComparisonView(
                resumeText, matchedSkills, missingSkills);

            response.put("success", true);
            response.put("optimizedResume", optimizedResume);
            response.put("comparison", comparison);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取简历模板
     */
    @GetMapping("/templates/{type}")
    public ResponseEntity<Map<String, Object>> getTemplate(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();

        Map<String, String> templates = new HashMap<>();
        templates.put("default", """
            【个人简介】
            简要描述您的背景、优势和职业目标

            【技能清单】
            • 编程语言：XXX
            • 前端技术：XXX
            • 后端框架：XXX
            • 数据库：XXX

            【项目经历】
            项目名称：
            技术栈：
            项目描述：
            工作职责：

            【工作经历】
            公司名称 | 职位
            工作时间：
            工作职责：

            【教育背景】
            学校名称 | 专业 | 时间
            """);

        templates.put("tech", """
            【技术概览】
            3年+开发经验，精通XXX，熟悉XXX

            【核心技能】
            后端：Java, Spring Boot, MySQL
            前端：Vue.js, React
            工具：Git, Docker, Linux

            【项目经验】
            1. XXX系统（2023）
               技术栈：XXX
               成果：XXX

            【教育背景】
            XXX大学 | 计算机科学 | 2019-2023
            """);

        templates.put("senior", """
            【个人优势】
            - X年大型项目架构经验
            - 主导过XX项目，技术团队XX人
            - 在XX领域有深入研究

            【专业技能】
            架构设计：高并发、微服务、分布式系统
            技术栈：Java/Go + Spring Cloud + K8s
            软技能：团队管理、技术规划

            【工作经历】
            高级工程师/技术负责人
            - 负责XX系统架构设计与实现
            - 优化系统性能，提升XX%
            - 带领团队完成XX项目交付
            """);

        response.put("template", templates.getOrDefault(type, templates.get("default")));
        response.put("type", type);

        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Resume Optimizer API is running");
        return ResponseEntity.ok(response);
    }
}
