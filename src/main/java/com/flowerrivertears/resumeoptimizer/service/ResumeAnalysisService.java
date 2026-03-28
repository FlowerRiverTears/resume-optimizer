package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.AnalysisRequest;
import com.flowerrivertears.resumeoptimizer.model.AnalysisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    // ==================== 技能库 ====================
    // 核心技能库 - 更全面
    private static final Set<String> SKILLS = new HashSet<>(Arrays.asList(
        // 编程语言
        "java", "c#", "python", "c++", "go", "rust", "php", "ruby", "swift", "kotlin",
        // Java 生态
        "springboot", "springcloud", "spring", "springmvc", "springframework",
        "mybatis", "hibernate", "jpa", "dubbo", "zookeeper",
        "j2ee", "jdk", "jvm", "maven", "gradle",
        // .NET 生态 - 扩充
        "asp.net", "asp.net core", "asp.netcore", "aspnetcore", "netcore",
        "winform", "wpf", "wcf", "wf",
        "ef", "ef core", "efcore", "entityframework", "entity framework",
        "linq", "lambda", "lambda expressions",
        "async", "await", "async/await", "delegates", "generics", "泛型",
        "razor", "blazor", "xamarin", "unity",
        // 前端框架
        "vue", "vue.js", "vuejs", "vue2", "vue3",
        "react", "reactjs", "react.js", "reactnative",
        "angular", "angularjs", "svelte",
        "jquery", "jqueryui",
        "bootstrap", "tailwindcss", "materialui",
        "elementui", "element-ui", "antd", "antdesign",
        "nuxt", "nuxtjs", "nextjs", "next.js",
        "html", "html5", "css", "css3", "sass", "less",
        "javascript", "typescript", "es6", "ajax", "json",
        // 数据库
        "mysql", "sqlserver", "sql server", "postgresql", "postgre", "postgres",
        "mongodb", "mongo", "redis", "oracle", "sqlite", "mariadb", "sql",
        "elasticsearch", "memcache", "memcached",
        // DevOps
        "git", "github", "gitlab", "svn",
        "docker", "kubernetes", "k8s", "helm",
        "jenkins", "ci/cd", "gitlab-ci", "github-actions",
        "linux", "nginx", "apache", "tomcat", "iis",
        "aws", "aws ec2", "aws s3", "azure", "gcp", "aliyun", "阿里云",
        // 消息队列
        "kafka", "rabbitmq", "activemq", "rocketmq", "pulsar",
        // 其他后端
        "nodejs", "node.js", "express", "koa", "nestjs",
        "django", "flask", "fastapi", "tornado",
        "graphql", "grpc", "websocket", "restful", "rest", "api",
        "jwt", "oauth", "oauth2", "shiro", "cas", "sso",
        "swagger", "openapi", "postman",
        "microservices", "微服务", "分布式", "缓存", "消息队列",
        "junit", "junit5", "testing", "单元测试", "test", "pytest",
        "mvc", "webapi", "web api", "ddd", "cqrs"
    ));

    // 同义词映射（归一化到标准词）
    private static final Map<String, String> NORM = new HashMap<>();
    static {
        // .NET - 统一归一化到 c#
        NORM.put("csharp", "c#");
        NORM.put("c#", "c#");
        NORM.put("asp.net", "c#");
        NORM.put("asp.net core", "c#");
        NORM.put("asp.netcore", "c#");
        NORM.put("aspnetcore", "c#");
        NORM.put("netcore", "c#");
        NORM.put(".net", "c#");
        NORM.put("dotnet", "c#");
        // .NET 特性 - 也归到 c#
        NORM.put("entity framework", "c#");
        NORM.put("entityframework", "c#");
        NORM.put("ef core", "c#");
        NORM.put("efcore", "c#");
        NORM.put("linq", "c#");
        NORM.put("lambda expressions", "c#");
        NORM.put("lambda", "c#");
        NORM.put("generics", "c#");
        NORM.put("泛型", "c#");
        NORM.put("async", "c#");
        NORM.put("await", "c#");
        NORM.put("async/await", "c#");
        NORM.put("async await", "c#");

        // Java
        NORM.put("j2ee", "java");
        NORM.put("j2se", "java");
        NORM.put("jdk", "java");
        NORM.put("spring framework", "spring");
        NORM.put("springboot", "springboot");
        NORM.put("spring boot", "springboot");
        NORM.put("springcloud", "springcloud");
        NORM.put("spring cloud", "springcloud");
        NORM.put("spring mvc", "springmvc");
        NORM.put("mybatis-plus", "mybatis");

        // 前端
        NORM.put("vue.js", "vue");
        NORM.put("vuejs", "vue");
        NORM.put("vue 2", "vue");
        NORM.put("vue 3", "vue");
        NORM.put("vue2", "vue");
        NORM.put("vue3", "vue");
        NORM.put("react.js", "react");
        NORM.put("reactjs", "react");
        NORM.put("react native", "reactnative");
        NORM.put("reactnative", "reactnative");
        NORM.put("angularjs", "angular");
        NORM.put("angular.js", "angular");
        NORM.put("nuxt.js", "nuxt");
        NORM.put("nuxtjs", "nuxt");
        NORM.put("next.js", "nextjs");
        NORM.put("nextjs", "nextjs");
        // UI框架
        NORM.put("element-ui", "elementui");
        NORM.put("element ui", "elementui");
        NORM.put("ant design", "antd");
        NORM.put("antd", "antd");
        NORM.put("material-ui", "materialui");
        NORM.put("materialui", "materialui");

        // 数据库
        NORM.put("sql server", "sqlserver");
        NORM.put("sqlserver", "sqlserver");
        NORM.put("postgresql", "postgresql");
        NORM.put("postgre", "postgresql");
        NORM.put("postgres", "postgresql");
        NORM.put("mongodb", "mongodb");
        NORM.put("mongo", "mongodb");

        // 工具
        NORM.put("k8s", "kubernetes");
        NORM.put("github", "git");
        NORM.put("gitlab", "git");
        NORM.put("ci/cd", "ci/cd");
        NORM.put("cicd", "ci/cd");
        NORM.put("webapi", "webapi");
        NORM.put("web api", "webapi");
        NORM.put("rest", "restful");
        NORM.put("es6", "javascript");
        NORM.put("html5", "html");
        NORM.put("css3", "css");
    }

    // 技能类别（用于理解 OR 关系）
    private static final Map<String, Set<String>> SKILL_CATEGORIES = new HashMap<>();
    static {
        // 前端框架类别（满足其中一个即可）
        Set<String> frontend = new HashSet<>(Arrays.asList("vue", "react", "angular"));
        SKILL_CATEGORIES.put("frontend", frontend);

        // 数据库类别（满足其中一个即可）
        Set<String> databases = new HashSet<>(Arrays.asList("mysql", "sqlserver", "postgresql", "mongodb", "oracle", "redis"));
        SKILL_CATEGORIES.put("database", databases);

        // .NET 核心技能（只要有 c# 相关就算满足）
        Set<String> dotnetCore = new HashSet<>(Arrays.asList(
            "c#", "asp.net", "asp.netcore",
            "linq", "lambda", "generics", "async", "await",
            "ef", "efcore", "mvc", "webapi"
        ));
        SKILL_CATEGORIES.put("dotnet", dotnetCore);

        // Java 核心技能
        Set<String> javaCore = new HashSet<>(Arrays.asList("java", "springboot", "springcloud"));
        SKILL_CATEGORIES.put("java", javaCore);
    }

    // 结构关键词
    private static final Set<String> EXP_KW = new HashSet<>(Arrays.asList(
        "experience", "work", "employment", "position", "job",
        "工作经历", "项目经历", "工作经验", "项目经验", "职责"
    ));
    private static final Set<String> EDU_KW = new HashSet<>(Arrays.asList(
        "education", "degree", "university", "college", "school",
        "教育", "学历", "大学", "学院", "专业", "本科", "硕士", "博士"
    ));
    private static final Set<String> CONTACT_KW = new HashSet<>(Arrays.asList(
        "email", "phone", "tel", "mobile", "邮箱", "电话", "微信", "qq"
    ));
    private static final Set<String> SUMMARY_KW = new HashSet<>(Arrays.asList(
        "summary", "profile", "about", "个人简介", "简介", "自我评价", "优势", "求职意向"
    ));

    // ==================== 提取技能 ====================
    private Set<String> extractSkills(String text) {
        Set<String> skills = new HashSet<>();
        if (text == null || text.isEmpty()) return skills;

        // 1. 先提取多词技术栈（按固定顺序匹配，避免短词优先匹配）
        String[][] multiWordSkills = {
            // .NET
            {"asp.net core", "asp.net"},
            {"entity framework", "ef"},
            {"ef core", "ef"},
            {"c#", "c#"},
            {"async/await", "async"},
            // Java
            {"spring boot", "springboot"},
            {"spring cloud", "springcloud"},
            // 前端
            {"vue.js", "vue"},
            {"react.js", "react"},
            {"angular.js", "angular"},
            {"element ui", "elementui"},
            {"ant design", "antd"},
            // 数据库
            {"sql server", "sqlserver"},
        };

        String lower = text.toLowerCase();
        for (String[] skill : multiWordSkills) {
            String keyword = skill[0];
            if (lower.contains(keyword)) {
                addSkill(skills, skill[1]);
            }
        }

        // 2. 处理 C#
        if (lower.contains("c#") || lower.contains("csharp")) {
            addSkill(skills, "c#");
        }

        // 3. 处理特殊符号（# 号等）
        lower = lower.replace("#", "").replace(".", " ");

        // 4. 单词技能（处理纯单词）
        String cleaned = lower
            .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ");

        for (String word : cleaned.split("\\s+")) {
            word = word.trim();
            if (!word.isEmpty() && word.length() >= 2) {
                addSkill(skills, word);
            }
        }

        return skills;
    }

    private void addSkill(Set<String> skills, String word) {
        if (word == null || word.isEmpty()) return;
        word = word.toLowerCase().trim();

        // 先检查 NORM 归一化
        String normalized = NORM.get(word);
        if (normalized != null) {
            skills.add(normalized);
            return;
        }

        // 直接检查技能库
        if (SKILLS.contains(word)) {
            skills.add(word);
        }
    }

    // ==================== 计算匹配度 ====================
    /**
     * 计算匹配度 - 简化版
     * 逻辑：
     * 1. 简历技能和职位要求技能的交集 = 匹配技能
     * 2. 职位要求技能 - 交集 = 缺失技能
     * 3. 匹配度 = 匹配技能数 / 职位要求技能数 × 100
     */
    private MatchResult calculateMatch(Set<String> resumeSkills, Set<String> jobSkills) {
        Set<String> matched = new HashSet<>();
        Set<String> missing = new HashSet<>();

        // 1. 计算交集（匹配技能）
        matched.addAll(resumeSkills);
        matched.retainAll(jobSkills);

        // 2. 计算差集（缺失技能）
        missing.addAll(jobSkills);
        missing.removeAll(resumeSkills);

        // 3. 按类别处理 OR 关系
        int totalRequirements = jobSkills.size();
        int matchedRequirements = matched.size();

        // 对于 OR 类别（如 Vue/React/Angular），如果简历有一个，就算匹配
        for (Map.Entry<String, Set<String>> entry : SKILL_CATEGORIES.entrySet()) {
            Set<String> categorySkills = entry.getValue();

            // 职位要求中有哪些属于这个类别
            Set<String> requiredInCategory = new HashSet<>(jobSkills);
            requiredInCategory.retainAll(categorySkills);

            if (requiredInCategory.isEmpty()) continue;

            // 简历中有哪些属于这个类别
            Set<String> resumeInCategory = new HashSet<>(resumeSkills);
            resumeInCategory.retainAll(categorySkills);

            // 如果简历有这个类别的技能，就算满足
            if (!resumeInCategory.isEmpty()) {
                // 添加匹配的（之前可能被误删的）
                for (String skill : resumeInCategory) {
                    if (!matched.contains(skill)) {
                        matched.add(skill);
                    }
                }
                // 从缺失中移除这个类别的其他技能
                missing.removeAll(requiredInCategory);
            }
        }

        // 4. 重新计算
        matchedRequirements = matched.size();
        totalRequirements = jobSkills.size();

        // 5. 计算匹配分数
        int score;
        if (jobSkills.isEmpty()) {
            score = 0;
        } else {
            score = (int) Math.round((double) matchedRequirements / totalRequirements * 100);
        }

        // 限制缺失技能数量
        Set<String> limitedMissing = missing.stream()
                .sorted()
                .limit(5)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new MatchResult(matched, limitedMissing, score);
    }

    static class MatchResult {
        Set<String> matched;
        Set<String> missing;
        int score;

        MatchResult(Set<String> matched, Set<String> missing, int score) {
            this.matched = matched;
            this.missing = missing;
            this.score = score;
        }
    }

    // ==================== 生成建议 ====================
    private List<String> generateSuggestions(String text, Set<String> resumeSkills,
                                            Set<String> matched, Set<String> missing, int matchScore) {
        List<String> suggestions = new ArrayList<>();

        // 结构建议
        if (!containsKeyword(text, SUMMARY_KW)) {
            suggestions.add("建议添加简历摘要/个人简介，突出您的核心优势");
        }
        if (!containsKeyword(text, EXP_KW)) {
            suggestions.add("建议添加工作经历/项目经历部分");
        }
        if (!containsKeyword(text, EDU_KW)) {
            suggestions.add("建议添加教育背景信息");
        }
        if (!containsKeyword(text, CONTACT_KW)) {
            suggestions.add("建议添加联系方式（邮箱、电话）");
        }

        // 长度建议
        int words = text.split("\\s+").length;
        if (words < 150) {
            suggestions.add("简历内容较少，建议补充更多项目细节");
        } else if (words > 1000) {
            suggestions.add("简历内容较长，建议精简到1-2页");
        }

        // 技能建议
        if (!missing.isEmpty()) {
            // 核心技能缺失
            if (missing.contains("c#") || missing.contains("asp.net")) {
                suggestions.add("【重要】建议补充 ASP.NET Core / C# 相关经验");
            }
            // 前端框架缺失
            if (missing.contains("vue") || missing.contains("react") || missing.contains("angular")) {
                suggestions.add("建议补充前端框架（Vue/React/Angular）经验");
            }
            // 数据库缺失
            if (missing.contains("mysql") || missing.contains("sqlserver")) {
                suggestions.add("建议补充数据库（MySQL/SQL Server）使用经验");
            }
        }

        // 匹配度反馈
        if (matchScore >= 80) {
            suggestions.add(0, "简历与岗位高度匹配，可以大胆投递！");
        } else if (matchScore >= 60) {
            suggestions.add(0, "简历基本匹配岗位要求，建议微调后投递");
        } else if (matchScore < 40) {
            suggestions.add("简历与岗位要求差距较大，建议针对性补充相关技能");
        }

        return suggestions;
    }

    private boolean containsKeyword(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.toLowerCase().contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    // ==================== ATS评分 ====================
    private int calculateAtsScore(String text, Set<String> skills) {
        int score = 0;

        // 技能 (40分)
        score += Math.min(skills.size() * 4, 40);

        // 结构 (30分)
        if (containsKeyword(text, CONTACT_KW)) score += 10;
        if (containsKeyword(text, SUMMARY_KW)) score += 5;
        if (containsKeyword(text, EXP_KW)) score += 10;
        if (containsKeyword(text, EDU_KW)) score += 5;

        // 长度 (20分)
        int words = text.split("\\s+").length;
        if (words >= 200 && words <= 800) score += 20;
        else if (words >= 100) score += 10;

        // 格式 (10分)
        if (!text.contains("!!!") && !text.contains("???")) score += 10;

        return Math.min(score, 100);
    }

    private AnalysisResponse.StructureInfo analyzeStructure(String text) {
        return AnalysisResponse.StructureInfo.builder()
                .hasContactInfo(containsKeyword(text, CONTACT_KW))
                .hasSummary(containsKeyword(text, SUMMARY_KW))
                .hasExperience(containsKeyword(text, EXP_KW))
                .hasEducation(containsKeyword(text, EDU_KW))
                .hasSkills(true)
                .totalWords(text.split("\\s+").length)
                .build();
    }

    // ==================== 主方法 ====================
    public AnalysisResponse analyze(AnalysisRequest request) {
        String resumeText = request.getResumeText();
        String jobDesc = request.getJobDescription();

        log.info("========== 收到分析请求 ==========");
        log.info("简历: {}", resumeText);
        log.info("职位: {}", jobDesc);

        // 提取技能
        Set<String> resumeSkills = extractSkills(resumeText);
        Set<String> jobSkills = extractSkills(jobDesc);

        // 计算匹配
        MatchResult match = calculateMatch(resumeSkills, jobSkills);

        // 生成建议
        List<String> suggestions = generateSuggestions(resumeText, resumeSkills,
                match.matched, match.missing, match.score);

        // ATS评分
        int atsScore = calculateAtsScore(resumeText, resumeSkills);

        // 结构分析
        AnalysisResponse.StructureInfo structure = analyzeStructure(resumeText);

        AnalysisResponse response = AnalysisResponse.builder()
                .atsScore(atsScore)
                .matchScore(match.score)
                .foundKeywords(new ArrayList<>(match.matched))
                .missingKeywords(new ArrayList<>(match.missing))
                .suggestions(suggestions)
                .keywordFrequency(new HashMap<>())
                .structure(structure)
                .build();

        log.info("========== 分析结果 ==========");
        log.info("简历技能: {}", resumeSkills);
        log.info("职位技能: {}", jobSkills);
        log.info("匹配技能: {}", match.matched);
        log.info("缺失技能: {}", match.missing);
        log.info("匹配度: {}%", match.score);
        log.info("ATS评分: {}", atsScore);
        log.info("建议: {}", suggestions);
        log.info("============================");

        return response;
    }
}
