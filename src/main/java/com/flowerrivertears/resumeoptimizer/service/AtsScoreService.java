package com.flowerrivertears.resumeoptimizer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerrivertears.resumeoptimizer.model.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AtsScoreService {

    private static final Logger log = LoggerFactory.getLogger(AtsScoreService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private RagService ragService;

    @Autowired
    private WeightedRetrievalService weightedRetrievalService;

    @Autowired
    private ApiKeyService apiKeyService;

    private static final String ATS_PROMPT = """
            你是一个专业的简历ATS评分系统。请根据以下信息，对简历进行深度评分分析。
            
            ## 简历内容
            {{resumeText}}
            
            ## 职位描述
            {{jobDescription}}
            
            ## 检索到的市场数据
            {{searchResults}}
            
            ## 基础分析结果
            {{basicAnalysis}}
            
            请严格按照以下JSON格式输出分析结果（不要输出任何其他内容，不要用markdown包裹）：
            
            {
              "overallScore": 85,
              "grade": "优秀",
              "gradeDescription": "简历质量优秀，与岗位高度匹配",
              "jobMatchScore": {
                "score": 78,
                "level": "良好",
                "description": "技能匹配度较好，部分核心技能需要加强",
                "totalRequired": 12,
                "matchedRequired": 9
              },
              "structureScore": {
                "score": 90,
                "level": "优秀",
                "hasContact": true,
                "hasSummary": true,
                "hasExperience": true,
                "hasEducation": true,
                "hasSkills": true,
                "wordCount": 800,
                "wordCountLevel": "适中"
              },
              "contentScore": {
                "score": 82,
                "level": "良好",
                "skillCount": 12,
                "keywordDensity": 35,
                "densityLevel": "适中"
              },
              "keywordScore": {
                "score": 75,
                "level": "良好",
                "totalKeywords": 12,
                "matchedKeywords": 9,
                "matchRate": 0.75
              },
              "matchedSkills": ["java", "springboot", "mysql", "vue"],
              "missingSkills": ["kubernetes", "redis"],
              "matchedCount": 9,
              "missingCount": 3,
              "categoryDetails": {
                "frontend": {
                  "name": "前端开发",
                  "score": 80,
                  "level": "良好",
                  "matched": 3,
                  "total": 4,
                  "matchedSkills": ["vue", "javascript", "html"],
                  "missingSkills": ["typescript"]
                },
                "backend": {
                  "name": "后端开发",
                  "score": 85,
                  "level": "良好",
                  "matched": 4,
                  "total": 5,
                  "matchedSkills": ["java", "springboot", "mysql", "c#"],
                  "missingSkills": ["springcloud"]
                },
                "database": {
                  "name": "数据库",
                  "score": 70,
                  "level": "一般",
                  "matched": 2,
                  "total": 3,
                  "matchedSkills": ["mysql", "sqlserver"],
                  "missingSkills": ["redis"]
                },
                "devops": {
                  "name": "DevOps",
                  "score": 40,
                  "level": "待改进",
                  "matched": 1,
                  "total": 3,
                  "matchedSkills": ["git"],
                  "missingSkills": ["docker", "kubernetes"]
                }
              },
              "skillGapDetails": [
                {
                  "skill": "kubernetes",
                  "category": "devops",
                  "importance": 4,
                  "reason": "容器化部署是现代开发标配",
                  "suggestion": "学习Kubernetes基础，从Docker开始",
                  "learningResources": ["Kubernetes官方文档", "K8s实战教程"]
                }
              ],
              "optimizationSuggestions": [
                {
                  "type": "skill",
                  "title": "补充容器化技能",
                  "description": "建议学习Docker和Kubernetes",
                  "priority": "高",
                  "impact": "提升匹配度30%"
                }
              ]
            }
            
            评分标准：
            - overallScore: 综合考虑匹配度、结构、内容、关键词，0-100分
            - jobMatchScore.score: 根据技能匹配比例计算，0-100分
            - structureScore.score: 根据简历结构完整性计算，0-100分
            - contentScore.score: 根据内容丰富度和关键词密度计算，0-100分
            - keywordScore.score: 根据关键词匹配率计算，0-100分
            - grade: 90+优秀, 75+良好, 60+一般, 40+待改进, 0+需重写
            - 所有分数必须基于简历实际内容动态计算，不要给固定值
            """;

    public AtsScoreResponse calculateAtsScore(String resumeText, String jobDescription,
                                               String provider, String keyId) {
        long startTime = System.currentTimeMillis();
        log.info("Starting ATS score calculation: keyId={}, provider={}", keyId, provider);

        if (keyId == null || keyId.isBlank()) {
            log.warn("No API key provided, falling back to basic analysis");
            return calculateBasicAtsScore(resumeText, jobDescription, provider);
        }

        try {
            ChatModel chatModel = apiKeyService.getChatModelForKey(keyId);
            AtsScoreResponse response = calculateLlmEnhancedAtsScore(chatModel, resumeText, jobDescription, provider);
            response.setKeyId(keyId);
            return response;
        } catch (Exception e) {
            log.error("LLM enhanced ATS score failed, falling back to basic: {}", e.getMessage());
            return calculateBasicAtsScore(resumeText, jobDescription, provider);
        }
    }

    private AtsScoreResponse calculateLlmEnhancedAtsScore(ChatModel chatModel, String resumeText,
                                                           String jobDescription, String provider) {
        long startTime = System.currentTimeMillis();

        try {
            ragService.ingestResumeData(resumeText, jobDescription);
        } catch (Exception e) {
            log.warn("RAG ingest failed: {}", e.getMessage());
        }

        String searchContext = "";
        try {
            List<SearchResult> searchResults = ragService.search(
                "岗位要求 技能匹配 ATS评分 " + (jobDescription != null ? jobDescription : resumeText),
                5, 0.3
            );

            if (weightedRetrievalService.isEnabled() && !searchResults.isEmpty()) {
                searchResults = weightedRetrievalService.applyWeights(
                    searchResults, "ATS评分分析", null, null
                );
            }

            searchContext = searchResults.stream()
                .map(r -> "[来源: " + r.getSource() + "] " + r.getContent())
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("RAG search failed: {}", e.getMessage());
        }

        AnalysisResponse basicAnalysis = null;
        String basicAnalysisStr = "无基础分析结果";
        try {
            basicAnalysis = resumeAnalysisService.analyze(
                AnalysisRequest.builder()
                    .resumeText(resumeText)
                    .jobDescription(jobDescription != null ? jobDescription : "")
                    .build()
            );
            basicAnalysisStr = formatBasicAnalysis(basicAnalysis);
        } catch (Exception e) {
            log.warn("Basic analysis failed: {}", e.getMessage());
        }

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("resumeText", resumeText != null ? resumeText : "");
        templateVars.put("jobDescription", jobDescription != null && !jobDescription.isBlank() ? jobDescription : "未提供职位描述，请根据简历内容推断合适的岗位要求");
        templateVars.put("searchResults", searchContext.isEmpty() ? "无检索结果" : searchContext);
        templateVars.put("basicAnalysis", basicAnalysisStr);

        PromptTemplate promptTemplate = PromptTemplate.from(ATS_PROMPT);
        Prompt prompt = promptTemplate.apply(templateVars);

        String rawResponse = chatModel.chat(prompt.text());

        AtsScoreResponse response = parseLlmResponse(rawResponse, basicAnalysis, resumeText);

        long elapsed = System.currentTimeMillis() - startTime;
        response.setResponseTimeMs(elapsed);
        response.setProvider(provider != null ? provider : "llm-enhanced");

        log.info("LLM ATS score completed in {}ms, score={}", elapsed, response.getOverallScore());
        return response;
    }

    private String formatBasicAnalysis(AnalysisResponse basic) {
        StringBuilder sb = new StringBuilder();
        sb.append("ATS评分: ").append(basic.getAtsScore()).append("\n");
        sb.append("匹配度: ").append(basic.getMatchScore()).append("\n");

        if (basic.getFoundKeywords() != null) {
            sb.append("已匹配关键词: ").append(String.join(", ", basic.getFoundKeywords())).append("\n");
        }
        if (basic.getMissingKeywords() != null) {
            sb.append("缺失关键词: ").append(String.join(", ", basic.getMissingKeywords())).append("\n");
        }

        if (basic.getStructure() != null) {
            sb.append("结构分析: 联系方式=").append(basic.getStructure().isHasContactInfo())
              .append(", 简介=").append(basic.getStructure().isHasSummary())
              .append(", 经历=").append(basic.getStructure().isHasExperience())
              .append(", 教育=").append(basic.getStructure().isHasEducation())
              .append(", 技能=").append(basic.getStructure().isHasSkills())
              .append(", 字数=").append(basic.getStructure().getTotalWords()).append("\n");
        }

        if (basic.getCategoryScores() != null) {
            sb.append("分类评分:\n");
            basic.getCategoryScores().forEach((k, v) ->
                sb.append("  ").append(k).append(": 分数=").append(v.getScore())
                  .append(", 匹配=").append(v.getMatchedCount())
                  .append("/总数=").append(v.getTotalCount()).append("\n")
            );
        }

        return sb.toString();
    }

    private AtsScoreResponse parseLlmResponse(String rawResponse, AnalysisResponse basicFallback, String resumeText) {
        try {
            String json = rawResponse.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            return buildResponseFromMap(data);
        } catch (Exception e) {
            log.error("Failed to parse LLM response as JSON: {}", e.getMessage());
            if (basicFallback != null) {
                return buildFallbackResponse(basicFallback, resumeText);
            }
            return buildEmptyResponse();
        }
    }

    @SuppressWarnings("unchecked")
    private AtsScoreResponse buildResponseFromMap(Map<String, Object> data) {
        int overallScore = getIntValue(data, "overallScore", 0);
        String grade = getStringValue(data, "grade", getGrade(overallScore));
        String gradeDescription = getStringValue(data, "gradeDescription", getGradeDescription(overallScore));

        Map<String, Object> jobMatchMap = (Map<String, Object>) data.get("jobMatchScore");
        AtsScoreResponse.JobMatchScore jobMatch = buildJobMatchFromMap(jobMatchMap, overallScore);

        Map<String, Object> structureMap = (Map<String, Object>) data.get("structureScore");
        AtsScoreResponse.StructureScore structure = buildStructureFromMap(structureMap);

        Map<String, Object> contentMap = (Map<String, Object>) data.get("contentScore");
        AtsScoreResponse.ContentScore content = buildContentFromMap(contentMap);

        Map<String, Object> keywordMap = (Map<String, Object>) data.get("keywordScore");
        AtsScoreResponse.KeywordScore keyword = buildKeywordFromMap(keywordMap);

        List<String> matchedSkills = getListValue(data, "matchedSkills");
        List<String> missingSkills = getListValue(data, "missingSkills");

        Map<String, Object> categoryMap = (Map<String, Object>) data.get("categoryDetails");
        Map<String, AtsScoreResponse.CategoryDetail> categoryDetails = buildCategoryDetailsFromMap(categoryMap);

        List<Map<String, Object>> skillGapList = (List<Map<String, Object>>) data.get("skillGapDetails");
        List<AtsScoreResponse.SkillGapDetail> skillGapDetails = buildSkillGapsFromMap(skillGapList);

        List<Map<String, Object>> suggestionList = (List<Map<String, Object>>) data.get("optimizationSuggestions");
        List<AtsScoreResponse.OptimizationSuggestion> suggestions = buildSuggestionsFromMap(suggestionList);

        return AtsScoreResponse.builder()
            .overallScore(overallScore)
            .grade(grade)
            .gradeDescription(gradeDescription)
            .jobMatchScore(jobMatch)
            .structureScore(structure)
            .contentScore(content)
            .keywordScore(keyword)
            .matchedSkills(matchedSkills)
            .missingSkills(missingSkills)
            .matchedCount(matchedSkills != null ? matchedSkills.size() : 0)
            .missingCount(missingSkills != null ? missingSkills.size() : 0)
            .categoryDetails(categoryDetails)
            .skillGapDetails(skillGapDetails)
            .optimizationSuggestions(suggestions)
            .build();
    }

    private AtsScoreResponse.JobMatchScore buildJobMatchFromMap(Map<String, Object> map, int fallbackScore) {
        if (map == null) {
            return AtsScoreResponse.JobMatchScore.builder()
                .score(fallbackScore).level(getGrade(fallbackScore))
                .description("基于综合分析").totalRequired(0).matchedRequired(0).build();
        }
        int score = getIntValue(map, "score", fallbackScore);
        return AtsScoreResponse.JobMatchScore.builder()
            .score(score)
            .level(getStringValue(map, "level", getGrade(score)))
            .description(getStringValue(map, "description", ""))
            .totalRequired(getIntValue(map, "totalRequired", 0))
            .matchedRequired(getIntValue(map, "matchedRequired", 0))
            .build();
    }

    private AtsScoreResponse.StructureScore buildStructureFromMap(Map<String, Object> map) {
        if (map == null) {
            return AtsScoreResponse.StructureScore.builder().score(0).level("需重写")
                .hasContact(false).hasSummary(false).hasExperience(false)
                .hasEducation(false).hasSkills(false).wordCount(0).wordCountLevel("偏少").build();
        }
        int score = getIntValue(map, "score", 0);
        return AtsScoreResponse.StructureScore.builder()
            .score(score)
            .level(getStringValue(map, "level", getGrade(score)))
            .hasContact(getBoolValue(map, "hasContact"))
            .hasSummary(getBoolValue(map, "hasSummary"))
            .hasExperience(getBoolValue(map, "hasExperience"))
            .hasEducation(getBoolValue(map, "hasEducation"))
            .hasSkills(getBoolValue(map, "hasSkills"))
            .wordCount(getIntValue(map, "wordCount", 0))
            .wordCountLevel(getStringValue(map, "wordCountLevel", "偏少"))
            .build();
    }

    private AtsScoreResponse.ContentScore buildContentFromMap(Map<String, Object> map) {
        if (map == null) return AtsScoreResponse.ContentScore.builder().score(0).level("需重写")
            .skillCount(0).keywordDensity(0).densityLevel("偏少").build();
        int score = getIntValue(map, "score", 0);
        return AtsScoreResponse.ContentScore.builder()
            .score(score)
            .level(getStringValue(map, "level", getGrade(score)))
            .skillCount(getIntValue(map, "skillCount", 0))
            .keywordDensity(getIntValue(map, "keywordDensity", 0))
            .densityLevel(getStringValue(map, "densityLevel", "偏少"))
            .build();
    }

    private AtsScoreResponse.KeywordScore buildKeywordFromMap(Map<String, Object> map) {
        if (map == null) return AtsScoreResponse.KeywordScore.builder().score(0).level("需重写")
            .totalKeywords(0).matchedKeywords(0).matchRate(0).build();
        int score = getIntValue(map, "score", 0);
        int total = getIntValue(map, "totalKeywords", 0);
        int matched = getIntValue(map, "matchedKeywords", 0);
        double rate = total > 0 ? (double) matched / total : 0;
        return AtsScoreResponse.KeywordScore.builder()
            .score(score)
            .level(getStringValue(map, "level", getGrade(score)))
            .totalKeywords(total)
            .matchedKeywords(matched)
            .matchRate(rate)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, AtsScoreResponse.CategoryDetail> buildCategoryDetailsFromMap(Map<String, Object> map) {
        if (map == null) return new LinkedHashMap<>();
        Map<String, AtsScoreResponse.CategoryDetail> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                int score = getIntValue(detail, "score", 0);
                result.put(entry.getKey(), AtsScoreResponse.CategoryDetail.builder()
                    .name(getStringValue(detail, "name", entry.getKey()))
                    .score(score)
                    .level(getStringValue(detail, "level", getGrade(score)))
                    .matched(getIntValue(detail, "matched", 0))
                    .total(getIntValue(detail, "total", 0))
                    .matchedSkills(getListValue(detail, "matchedSkills"))
                    .missingSkills(getListValue(detail, "missingSkills"))
                    .build());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<AtsScoreResponse.SkillGapDetail> buildSkillGapsFromMap(List<Map<String, Object>> list) {
        if (list == null) return new ArrayList<>();
        List<AtsScoreResponse.SkillGapDetail> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            result.add(AtsScoreResponse.SkillGapDetail.builder()
                .skill(getStringValue(item, "skill", ""))
                .category(getStringValue(item, "category", ""))
                .importance(getIntValue(item, "importance", 3))
                .reason(getStringValue(item, "reason", ""))
                .suggestion(getStringValue(item, "suggestion", ""))
                .learningResources(getListValue(item, "learningResources"))
                .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<AtsScoreResponse.OptimizationSuggestion> buildSuggestionsFromMap(List<Map<String, Object>> list) {
        if (list == null) return new ArrayList<>();
        List<AtsScoreResponse.OptimizationSuggestion> result = new ArrayList<>();
        for (Map<String, Object> item : list) {
            result.add(AtsScoreResponse.OptimizationSuggestion.builder()
                .type(getStringValue(item, "type", ""))
                .title(getStringValue(item, "title", ""))
                .description(getStringValue(item, "description", ""))
                .priority(getStringValue(item, "priority", "中"))
                .impact(getStringValue(item, "impact", ""))
                .build());
        }
        return result;
    }

    private AtsScoreResponse calculateBasicAtsScore(String resumeText, String jobDescription, String provider) {
        AnalysisResponse basic = resumeAnalysisService.analyze(
            AnalysisRequest.builder()
                .resumeText(resumeText)
                .jobDescription(jobDescription != null ? jobDescription : "")
                .build()
        );

        AtsScoreResponse response = buildFallbackResponse(basic, resumeText);
        response.setProvider(provider != null ? provider : "basic");
        return response;
    }

    private AtsScoreResponse buildFallbackResponse(AnalysisResponse basic, String resumeText) {
        List<String> matchedSkills = basic.getFoundKeywords() != null ? basic.getFoundKeywords() : new ArrayList<>();
        List<String> missingSkills = basic.getMissingKeywords() != null ? basic.getMissingKeywords() : new ArrayList<>();

        boolean hasEmptyJobDesc = (basic.getMatchScore() == 0 && matchedSkills.isEmpty() && missingSkills.isEmpty());

        if (hasEmptyJobDesc && !matchedSkills.isEmpty()) {
            // should not happen, but guard
        } else if (hasEmptyJobDesc) {
            matchedSkills = inferMatchedSkillsFromResume(basic, resumeText);
            missingSkills = inferMissingSkillsFromResume(matchedSkills);
        }

        int matchedCount = matchedSkills.size();
        int missingCount = missingSkills.size();
        int totalKeywords = matchedCount + missingCount;
        double matchRate = totalKeywords > 0 ? (double) matchedCount / totalKeywords : 0;
        int matchScore = (int) (matchRate * 100);

        Map<String, AtsScoreResponse.CategoryDetail> categoryDetails = buildCategoryDetailsWithInference(basic, matchedSkills, missingSkills);

        int structureScoreVal = calculateStructureScoreValue(basic);
        int contentScoreVal = calculateContentScoreValue(matchedSkills);
        int keywordScoreVal = matchScore;
        int overallScore = (int) (matchScore * 0.4 + structureScoreVal * 0.3 + contentScoreVal * 0.3);
        overallScore = Math.min(100, Math.max(0, overallScore));

        return AtsScoreResponse.builder()
            .overallScore(overallScore)
            .grade(getGrade(overallScore))
            .gradeDescription(getGradeDescription(overallScore))
            .jobMatchScore(AtsScoreResponse.JobMatchScore.builder()
                .score(matchScore)
                .level(getGrade(matchScore))
                .description(matchScore >= 80 ? "技能高度匹配" : matchScore >= 60 ? "技能基本匹配" : matchScore >= 40 ? "部分技能匹配" : "技能匹配度较低")
                .totalRequired(totalKeywords)
                .matchedRequired(matchedCount)
                .build())
            .structureScore(buildStructureScore(basic, resumeText))
            .contentScore(AtsScoreResponse.ContentScore.builder()
                .score(contentScoreVal)
                .level(getGrade(contentScoreVal))
                .skillCount(matchedCount)
                .keywordDensity(Math.min(matchedCount * 5, 100))
                .densityLevel(matchedCount >= 8 ? "丰富" : matchedCount >= 4 ? "适中" : "偏少")
                .build())
            .keywordScore(AtsScoreResponse.KeywordScore.builder()
                .score(keywordScoreVal)
                .level(getGrade(keywordScoreVal))
                .totalKeywords(totalKeywords)
                .matchedKeywords(matchedCount)
                .matchRate(matchRate)
                .build())
            .matchedSkills(matchedSkills)
            .missingSkills(missingSkills)
            .matchedCount(matchedCount)
            .missingCount(missingCount)
            .categoryDetails(categoryDetails)
            .skillGapDetails(buildSkillGapDetailsFromSkills(missingSkills))
            .optimizationSuggestions(buildOptimizationSuggestionsV2(basic, resumeText, missingSkills))
            .build();
    }

    private List<String> inferMatchedSkillsFromResume(AnalysisResponse basic, String resumeText) {
        List<String> skills = new ArrayList<>();
        if (basic.getFoundKeywords() != null && !basic.getFoundKeywords().isEmpty()) {
            return basic.getFoundKeywords();
        }

        String lower = resumeText.toLowerCase();
        Map<String, String[]> skillPatterns = new LinkedHashMap<>();
        skillPatterns.put("Vue.js", new String[]{"vue"});
        skillPatterns.put("JavaScript", new String[]{"javascript", "js"});
        skillPatterns.put("HTML5", new String[]{"html"});
        skillPatterns.put("CSS3", new String[]{"css"});
        skillPatterns.put("jQuery", new String[]{"jquery"});
        skillPatterns.put("Java", new String[]{"java"});
        skillPatterns.put("Spring Boot", new String[]{"springboot", "spring boot"});
        skillPatterns.put("MyBatis", new String[]{"mybatis", "mybatis-plus"});
        skillPatterns.put("C#", new String[]{"c#", "csharp", "asp.net"});
        skillPatterns.put("ASP.NET Core", new String[]{"asp.net core", "aspnetcore"});
        skillPatterns.put("Python", new String[]{"python"});
        skillPatterns.put("MySQL", new String[]{"mysql"});
        skillPatterns.put("SQL Server", new String[]{"sql server", "sqlserver"});
        skillPatterns.put("Redis", new String[]{"redis"});
        skillPatterns.put("Git", new String[]{"git"});
        skillPatterns.put("Docker", new String[]{"docker"});
        skillPatterns.put("Linux", new String[]{"linux"});
        skillPatterns.put("Node.js", new String[]{"nodejs", "node.js"});
        skillPatterns.put("RESTful API", new String[]{"restful", "rest api"});
        skillPatterns.put("Maven", new String[]{"maven"});

        for (Map.Entry<String, String[]> entry : skillPatterns.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (lower.contains(pattern)) {
                    skills.add(entry.getKey());
                    break;
                }
            }
        }
        return skills;
    }

    private List<String> inferMissingSkillsFromResume(List<String> matchedSkills) {
        List<String> commonRequired = Arrays.asList(
            "Docker", "Kubernetes", "Redis", "Spring Cloud", "TypeScript",
            "Nginx", "CI/CD", "微服务", "消息队列"
        );
        List<String> missing = new ArrayList<>();
        Set<String> matchedLower = matchedSkills.stream()
            .map(s -> s.toLowerCase())
            .collect(Collectors.toSet());

        for (String skill : commonRequired) {
            boolean found = false;
            for (String m : matchedLower) {
                if (m.contains(skill.toLowerCase()) || skill.toLowerCase().contains(m)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(skill);
            }
        }
        return missing.stream().limit(5).collect(Collectors.toList());
    }

    private Map<String, AtsScoreResponse.CategoryDetail> buildCategoryDetailsWithInference(
            AnalysisResponse basic, List<String> matchedSkills, List<String> missingSkills) {
        Map<String, AtsScoreResponse.CategoryDetail> details = new LinkedHashMap<>();

        Map<String, String> categoryNames = new LinkedHashMap<>();
        categoryNames.put("frontend", "前端开发");
        categoryNames.put("backend", "后端开发");
        categoryNames.put("database", "数据库");
        categoryNames.put("devops", "DevOps");

        Map<String, Set<String>> categorySkillMap = new LinkedHashMap<>();
        categorySkillMap.put("frontend", new HashSet<>(Arrays.asList("vue", "vue.js", "javascript", "js", "html5", "html", "css3", "css", "jquery", "typescript")));
        categorySkillMap.put("backend", new HashSet<>(Arrays.asList("java", "spring boot", "springboot", "mybatis", "c#", "asp.net core", "aspnetcore", "python", "node.js", "nodejs", "restful api", "restful", "maven")));
        categorySkillMap.put("database", new HashSet<>(Arrays.asList("mysql", "sql server", "sqlserver", "redis", "mongodb", "postgresql", "oracle")));
        categorySkillMap.put("devops", new HashSet<>(Arrays.asList("git", "docker", "kubernetes", "linux", "nginx", "ci/cd", "jenkins")));

        for (Map.Entry<String, Set<String>> entry : categorySkillMap.entrySet()) {
            String cat = entry.getKey();
            Set<String> catSkills = entry.getValue();

            List<String> matchedInCat = matchedSkills.stream()
                .filter(s -> catSkills.stream().anyMatch(cs -> s.toLowerCase().contains(cs) || cs.contains(s.toLowerCase())))
                .collect(Collectors.toList());
            List<String> missingInCat = missingSkills.stream()
                .filter(s -> catSkills.stream().anyMatch(cs -> s.toLowerCase().contains(cs) || cs.contains(s.toLowerCase())))
                .collect(Collectors.toList());

            int matched = matchedInCat.size();
            int total = matched + missingInCat.size();
            if (total == 0) total = matched > 0 ? matched : 1;

            int score = total > 0 ? (int) (100.0 * matched / total) : (matched > 0 ? 100 : 0);

            if (basic.getCategoryScores() != null && basic.getCategoryScores().containsKey(cat)) {
                AnalysisResponse.CategoryScore cs = basic.getCategoryScores().get(cat);
                if (cs.getTotalCount() > 0) {
                    score = cs.getScore();
                    matched = cs.getMatchedCount();
                    total = cs.getTotalCount();
                }
            }

            details.put(cat, AtsScoreResponse.CategoryDetail.builder()
                .name(categoryNames.get(cat))
                .score(score)
                .level(getGrade(score))
                .matched(matched)
                .total(total)
                .matchedSkills(matchedInCat)
                .missingSkills(missingInCat)
                .build());
        }

        return details;
    }

    private int calculateStructureScoreValue(AnalysisResponse basic) {
        int score = 0;
        if (basic.getStructure() != null) {
            if (basic.getStructure().isHasContactInfo()) score += 20;
            if (basic.getStructure().isHasSummary()) score += 15;
            if (basic.getStructure().isHasExperience()) score += 25;
            if (basic.getStructure().isHasEducation()) score += 15;
            if (basic.getStructure().isHasSkills()) score += 10;
            int words = basic.getStructure().getTotalWords();
            if (words >= 300 && words <= 1500) score += 15;
            else if (words >= 150) score += 8;
        }
        return score;
    }

    private int calculateContentScoreValue(List<String> matchedSkills) {
        int count = matchedSkills.size();
        return Math.min(count * 6, 60) + Math.min(count * 3, 40);
    }

    private List<AtsScoreResponse.SkillGapDetail> buildSkillGapDetailsFromSkills(List<String> missingSkills) {
        List<AtsScoreResponse.SkillGapDetail> details = new ArrayList<>();
        for (String skill : missingSkills) {
            details.add(AtsScoreResponse.SkillGapDetail.builder()
                .skill(skill)
                .category(getSkillCategory(skill))
                .importance(4)
                .reason("简历中未体现该技能")
                .suggestion("建议系统学习 " + skill + " 相关技术栈")
                .learningResources(List.of("官方文档", "实战教程"))
                .build());
        }
        return details;
    }

    private List<AtsScoreResponse.OptimizationSuggestion> buildOptimizationSuggestionsV2(
            AnalysisResponse basic, String resumeText, List<String> missingSkills) {
        List<AtsScoreResponse.OptimizationSuggestion> suggestions = new ArrayList<>();
        if (basic.getStructure() != null) {
            if (!basic.getStructure().isHasContactInfo())
                suggestions.add(AtsScoreResponse.OptimizationSuggestion.builder().type("structure").title("添加联系方式").description("简历缺少联系方式，建议添加邮箱和电话").priority("高").impact("提升ATS评分10分").build());
            if (!basic.getStructure().isHasSummary())
                suggestions.add(AtsScoreResponse.OptimizationSuggestion.builder().type("structure").title("添加个人简介").description("建议添加2-3行个人简介，突出核心优势").priority("中").impact("提升ATS评分8分").build());
        }
        if (!missingSkills.isEmpty()) {
            List<String> topMissing = missingSkills.stream().limit(3).toList();
            suggestions.add(AtsScoreResponse.OptimizationSuggestion.builder().type("skill").title("补充缺失技能").description("建议补充: " + String.join(", ", topMissing)).priority("高").impact("提升匹配度" + (topMissing.size() * 10) + "%").build());
        }
        suggestions.add(AtsScoreResponse.OptimizationSuggestion.builder().type("content").title("量化成果描述").description("建议使用数字量化项目成果，如\"提升性能30%\"").priority("中").impact("提升简历吸引力").build());
        return suggestions;
    }

    private AtsScoreResponse buildEmptyResponse() {
        return AtsScoreResponse.builder()
            .overallScore(0).grade("需重写").gradeDescription("无法分析简历内容")
            .jobMatchScore(AtsScoreResponse.JobMatchScore.builder().score(0).level("需重写")
                .description("分析失败").totalRequired(0).matchedRequired(0).build())
            .structureScore(AtsScoreResponse.StructureScore.builder().score(0).level("需重写")
                .hasContact(false).hasSummary(false).hasExperience(false)
                .hasEducation(false).hasSkills(false).wordCount(0).wordCountLevel("偏少").build())
            .contentScore(AtsScoreResponse.ContentScore.builder().score(0).level("需重写")
                .skillCount(0).keywordDensity(0).densityLevel("偏少").build())
            .keywordScore(AtsScoreResponse.KeywordScore.builder().score(0).level("需重写")
                .totalKeywords(0).matchedKeywords(0).matchRate(0).build())
            .matchedSkills(new ArrayList<>()).missingSkills(new ArrayList<>())
            .matchedCount(0).missingCount(0)
            .categoryDetails(new LinkedHashMap<>())
            .skillGapDetails(new ArrayList<>())
            .optimizationSuggestions(new ArrayList<>())
            .build();
    }

    private int calculateOverallScore(AnalysisResponse basic) {
        int atsScore = basic.getAtsScore();
        int matchScore = basic.getMatchScore();
        int structureScore = 0;
        if (basic.getStructure() != null) {
            if (basic.getStructure().isHasContactInfo()) structureScore += 20;
            if (basic.getStructure().isHasSummary()) structureScore += 15;
            if (basic.getStructure().isHasExperience()) structureScore += 25;
            if (basic.getStructure().isHasEducation()) structureScore += 15;
            if (basic.getStructure().isHasSkills()) structureScore += 10;
            int words = basic.getStructure().getTotalWords();
            if (words >= 300 && words <= 1500) structureScore += 15;
            else if (words >= 150) structureScore += 8;
        }
        int overall = (int) (atsScore * 0.4 + matchScore * 0.4 + structureScore * 0.2);
        return Math.min(100, Math.max(0, overall));
    }

    private String getGrade(int score) {
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "待改进";
        return "需重写";
    }

    private String getGradeDescription(int score) {
        if (score >= 90) return "简历质量优秀，与岗位高度匹配，可以大胆投递";
        if (score >= 75) return "简历质量良好，基本匹配岗位要求，建议微调后投递";
        if (score >= 60) return "简历质量一般，部分技能需要补充，建议针对性优化";
        if (score >= 40) return "简历需要较多改进，建议重新梳理技能和经历";
        return "简历与岗位要求差距较大，建议重新编写";
    }

    private AtsScoreResponse.StructureScore buildStructureScore(AnalysisResponse basic, String text) {
        boolean hasContact = basic.getStructure() != null && basic.getStructure().isHasContactInfo();
        boolean hasSummary = basic.getStructure() != null && basic.getStructure().isHasSummary();
        boolean hasExperience = basic.getStructure() != null && basic.getStructure().isHasExperience();
        boolean hasEducation = basic.getStructure() != null && basic.getStructure().isHasEducation();
        boolean hasSkills = basic.getStructure() != null && basic.getStructure().isHasSkills();
        int score = calculateStructureScoreValue(basic);
        int words = basic.getStructure() != null ? basic.getStructure().getTotalWords() : 0;
        return AtsScoreResponse.StructureScore.builder()
            .score(score).level(getGrade(score))
            .hasContact(hasContact).hasSummary(hasSummary).hasExperience(hasExperience)
            .hasEducation(hasEducation).hasSkills(hasSkills)
            .wordCount(words).wordCountLevel(words >= 300 && words <= 1500 ? "适中" : words < 300 ? "偏少" : "偏多")
            .build();
    }

    private String getSkillCategory(String skill) {
        Set<String> frontend = Set.of("vue", "vue.js", "react", "angular", "javascript", "typescript", "html", "html5", "css", "css3", "jquery", "bootstrap", "ajax", "webpack");
        Set<String> backend = Set.of("java", "python", "c#", "go", "springboot", "spring boot", "spring", "nodejs", "node.js", "django", "flask", "api", "restful", "graphql", "asp.net", "asp.net core", "mvc", "mybatis", "maven");
        Set<String> database = Set.of("mysql", "postgresql", "mongodb", "redis", "oracle", "sqlserver", "sql server", "elasticsearch", "kafka", "rabbitmq");
        Set<String> devops = Set.of("docker", "kubernetes", "git", "linux", "nginx", "jenkins", "aws", "azure", "aliyun", "ci/cd");
        String s = skill.toLowerCase();
        if (frontend.stream().anyMatch(s::contains)) return "frontend";
        if (backend.stream().anyMatch(s::contains)) return "backend";
        if (database.stream().anyMatch(s::contains)) return "database";
        if (devops.stream().anyMatch(s::contains)) return "devops";
        return "tools";
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultValue; }
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private boolean getBoolValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return "true".equalsIgnoreCase(val.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> getListValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) return (List<String>) val;
        return new ArrayList<>();
    }
}
