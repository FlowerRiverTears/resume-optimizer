package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.AnalysisRequest;
import com.flowerrivertears.resumeoptimizer.model.AnalysisResponse;
import com.flowerrivertears.resumeoptimizer.model.SkillGap;
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

        String lower = text.toLowerCase();

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
     * 匹配度计算逻辑：
     * 1. matched = 简历技能 ∩ 职位技能（简历实际拥有的技能）
     * 2. missing = 职位技能 - 简历技能（简历没有的技能）
     * 3. 对于 OR 类别（如 Vue/React/Angular），如果简历有其中任何一个：
     *    - matched 添加简历拥有的类别技能
     *    - missing 只移除简历拥有的技能，不移除整个类别
     * 4. 匹配度 = matched数 / 职位技能数 × 100
     */
    private MatchResult calculateMatch(Set<String> resumeSkills, Set<String> jobSkills) {
        Set<String> matched = new HashSet<>();
        Set<String> missing = new HashSet<>();

        // 1. 计算交集（匹配技能）- 简历实际拥有的技能
        matched.addAll(resumeSkills);
        matched.retainAll(jobSkills);

        // 2. 计算差集（缺失技能）- 职位要求但简历没有的技能
        missing.addAll(jobSkills);
        missing.removeAll(resumeSkills);

        // 3. 按类别处理 OR 关系
        // 只影响匹配度计算，不改变 matched/missing 的基本定义
        for (Map.Entry<String, Set<String>> entry : SKILL_CATEGORIES.entrySet()) {
            Set<String> categorySkills = entry.getValue();

            // 职位要求中有哪些属于这个类别
            Set<String> requiredInCategory = new HashSet<>(jobSkills);
            requiredInCategory.retainAll(categorySkills);

            if (requiredInCategory.isEmpty()) continue;

            // 简历中有哪些属于这个类别
            Set<String> resumeInCategory = new HashSet<>(resumeSkills);
            resumeInCategory.retainAll(categorySkills);

            // 如果简历有这个类别的技能（OR关系满足）
            if (!resumeInCategory.isEmpty()) {
                // matched 添加简历拥有的类别技能（用于计算匹配度）
                matched.addAll(resumeInCategory);
                // missing 只移除简历实际拥有的技能（不是整个类别）
                missing.removeAll(resumeInCategory);
            }
        }

        // 4. 计算匹配分数
        int matchedRequirements = matched.size();
        int totalRequirements = jobSkills.size();

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
                                            Set<String> matched, Set<String> missing, int matchScore,
                                            Map<String, AnalysisResponse.CategoryScore> categoryScores) {
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
        if (words < 300) {
            suggestions.add("简历内容较少，建议补充更多项目细节（建议300字以上）");
        } else if (words > 1500) {
            suggestions.add("简历内容较长，建议精简到1-2页");
        }

        // 分类针对性建议
        if (categoryScores != null && !categoryScores.isEmpty()) {
            // 找出得分最低的分类
            String lowestCategory = null;
            int lowestScore = 100;
            for (Map.Entry<String, AnalysisResponse.CategoryScore> entry : categoryScores.entrySet()) {
                AnalysisResponse.CategoryScore cs = entry.getValue();
                // 忽略不要求该分类的（totalCount=0 表示不要求）
                if (cs.getTotalCount() > 0 && cs.getScore() < lowestScore) {
                    lowestScore = cs.getScore();
                    lowestCategory = entry.getKey();
                }
            }

            // 根据最低分类给出针对性建议
            if (lowestCategory != null && lowestScore < 50) {
                String categorySuggestion = getCategorySuggestion(lowestCategory, categoryScores.get(lowestCategory));
                if (categorySuggestion != null) {
                    suggestions.add(categorySuggestion);
                }
            }
        }

        // 缺失技能建议 - 根据实际缺失的技能生成针对性建议
        if (!missing.isEmpty()) {
            StringBuilder skillSuggestion = new StringBuilder("建议补充: ");
            List<String> missingList = new ArrayList<>(missing);

            // 按类别组织缺失技能
            Set<String> missingBackend = new HashSet<>(Arrays.asList(
                "c#", "java", "springboot", "springcloud", "nodejs", "python",
                "webapi", "restful", "api", "graphql", "jwt", "kafka", "rabbitmq",
                "swagger", "mvc", "ef", "linq", "asp.net"
            ));
            Set<String> missingFrontend = new HashSet<>(Arrays.asList(
                "vue", "react", "angular", "javascript", "typescript", "jquery", "bootstrap"
            ));
            Set<String> missingDb = new HashSet<>(Arrays.asList(
                "mysql", "sqlserver", "postgresql", "mongodb", "redis", "oracle", "kafka", "rabbitmq"
            ));
            Set<String> missingDevOps = new HashSet<>(Arrays.asList(
                "git", "docker", "kubernetes", "linux", "nginx", "jenkins", "ci/cd", "maven", "gradle"
            ));

            Set<String> currentMissing = new HashSet<>(missing);

            // 检查各类别
            currentMissing.retainAll(missingBackend);
            if (!currentMissing.isEmpty()) {
                skillSuggestion.append("后端(").append(String.join(", ", currentMissing)).append(") ");
            }
            currentMissing = new HashSet<>(missing);
            currentMissing.retainAll(missingFrontend);
            if (!currentMissing.isEmpty()) {
                skillSuggestion.append("前端(").append(String.join(", ", currentMissing)).append(") ");
            }
            currentMissing = new HashSet<>(missing);
            currentMissing.retainAll(missingDb);
            if (!currentMissing.isEmpty()) {
                skillSuggestion.append("数据库/中间件(").append(String.join(", ", currentMissing)).append(") ");
            }
            currentMissing = new HashSet<>(missing);
            currentMissing.retainAll(missingDevOps);
            if (!currentMissing.isEmpty()) {
                skillSuggestion.append("DevOps(").append(String.join(", ", currentMissing)).append(") ");
            }

            String suggestion = skillSuggestion.toString().trim();
            if (!suggestion.equals("建议补充:")) {
                suggestions.add(suggestion);
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

    /**
     * 根据分类获取针对性建议
     */
    private String getCategorySuggestion(String category, AnalysisResponse.CategoryScore cs) {
        if (cs.getTotalCount() == 0) return null; // 不要求该分类

        String categoryName = getCategoryName(category);
        int score = cs.getScore();
        int matched = cs.getMatchedCount();
        int total = cs.getTotalCount();
        int missing = total - matched;

        if (score >= 80) {
            return categoryName + "技能匹配良好，继续保持";
        } else if (score >= 50) {
            return categoryName + "技能基本匹配，但缺少 " + missing + " 项，建议加强";
        } else if (score >= 30) {
            return categoryName + "技能缺口较大，建议系统学习相关技术栈";
        } else {
            return categoryName + "技能严重不足，建议从基础开始学习并补充项目经验";
        }
    }

    /**
     * 获取分类中文名称
     */
    private String getCategoryName(String category) {
        switch (category) {
            case "frontend": return "前端";
            case "backend": return "后端";
            case "database": return "数据库/中间件";
            case "devops": return "DevOps/工具";
            default: return "其他";
        }
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
        if (words >= 300 && words <= 1500) score += 20;
        else if (words >= 150) score += 10;

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

    // ==================== 分类评分计算（用于雷达图）====================
    /**
     * 计算各技能分类的匹配度
     * 确保分类互斥，每个技能只属于一个分类
     */
    private Map<String, AnalysisResponse.CategoryScore> calculateCategoryScores(
            Set<String> resumeSkills, Set<String> jobSkills) {
        Map<String, AnalysisResponse.CategoryScore> categoryScores = new LinkedHashMap<>();

        // 定义互斥的技能分类（每个技能只属于一个分类）
        Map<String, Set<String>> chartCategories = new LinkedHashMap<>();
        chartCategories.put("frontend", new HashSet<>(Arrays.asList(
            "vue", "react", "angular", "javascript", "typescript", "html", "css",
            "jquery", "bootstrap", "ajax", "es6", "sass", "less", "webpack"
        )));
        chartCategories.put("backend", new HashSet<>(Arrays.asList(
            "java", "c#", "python", "springboot", "springcloud", "nodejs",
            "asp.net", "django", "flask", "api", "webapi", "restful",
            "graphql", "grpc", "websocket", "jwt", "oauth"
        )));
        chartCategories.put("database", new HashSet<>(Arrays.asList(
            "mysql", "sqlserver", "postgresql", "mongodb", "redis", "oracle",
            "elasticsearch", "sql", "kafka", "rabbitmq", "activemq"
        )));
        chartCategories.put("devops", new HashSet<>(Arrays.asList(
            "git", "docker", "kubernetes", "linux", "jenkins", "nginx",
            "ci/cd", "tomcat", "aws", "aliyun", "azure", "gcp", "maven", "gradle"
        )));

        // 统计各类别的匹配情况
        for (Map.Entry<String, Set<String>> entry : chartCategories.entrySet()) {
            String categoryName = entry.getKey();
            Set<String> categorySkills = entry.getValue();

            // 职位要求中属于该分类的技能
            Set<String> requiredInCategory = new HashSet<>(jobSkills);
            requiredInCategory.retainAll(categorySkills);

            // 简历中属于该分类的技能
            Set<String> matchedInCategory = new HashSet<>(resumeSkills);
            matchedInCategory.retainAll(categorySkills);

            int totalCount = requiredInCategory.size();
            int matchedCount = matchedInCategory.size();

            // 计算得分：如果职位没有该分类要求，得分100（不要求）；否则按匹配率计算
            int score;
            if (totalCount == 0) {
                score = 100; // 不要求该分类技能，视为满分
            } else {
                score = (int) Math.round(100.0 * matchedCount / totalCount);
                score = Math.min(100, Math.max(0, score)); // 确保在0-100范围内
            }

            categoryScores.put(categoryName, AnalysisResponse.CategoryScore.builder()
                    .category(categoryName)
                    .matchedCount(matchedCount)
                    .totalCount(totalCount)
                    .score(score)
                    .build());
        }

        return categoryScores;
    }

    // ==================== 技能差距分析 ====================
    /**
     * 生成技能差距详情列表
     */
    private List<SkillGap> analyzeSkillGaps(Set<String> resumeSkills, Set<String> jobSkills) {
        List<SkillGap> skillGaps = new LinkedList<>();

        // 计算缺失技能
        Set<String> missingSkills = new HashSet<>(jobSkills);
        missingSkills.removeAll(resumeSkills);

        // 技能重要性映射
        Map<String, Integer> importanceMap = new HashMap<>();
        // 高重要性技能
        Arrays.asList("vue", "react", "angular", "java", "c#", "python",
            "springboot", "mysql", "postgresql", "docker", "git",
            "restful", "api", "kubernetes").forEach(s -> importanceMap.put(s, 5));
        // 中等重要性
        Arrays.asList("javascript", "typescript", "html", "css", "redis",
            "mongodb", "linux", "nodejs", "springcloud", "aws").forEach(s -> importanceMap.put(s, 4));
        // 一般重要性
        Arrays.asList("jquery", "bootstrap", "webpack", "gradle", "maven",
            "nginx", "jenkins", "ci/cd", "github", "gitlab").forEach(s -> importanceMap.put(s, 3));

        // 学习建议模板
        Map<String, String> suggestionMap = new HashMap<>();
        suggestionMap.put("vue", "建议学习 Vue3 组合式 API 和 Pinia 状态管理");
        suggestionMap.put("react", "建议学习 React Hooks 和 Redux 状态管理");
        suggestionMap.put("angular", "建议学习 Angular 组件化和 RxJS");
        suggestionMap.put("java", "建议深入学习 Spring Boot 和微服务架构");
        suggestionMap.put("c#", "建议深入学习 .NET Core 和 Entity Framework");
        suggestionMap.put("python", "建议学习 Django/Flask 框架");
        suggestionMap.put("mysql", "建议学习 MySQL 优化和索引原理");
        suggestionMap.put("postgresql", "建议学习 PostgreSQL 高级特性和性能优化");
        suggestionMap.put("mongodb", "建议学习 MongoDB 聚合管道和数据建模");
        suggestionMap.put("redis", "建议学习 Redis 持久化和分布式缓存");
        suggestionMap.put("docker", "建议学习 Docker Compose 和容器编排");
        suggestionMap.put("kubernetes", "建议学习 K8s 部署和 Helm Charts");
        suggestionMap.put("git", "建议掌握 Git 高级操作和分支管理策略");
        suggestionMap.put("restful", "建议学习 RESTful API 设计规范");
        suggestionMap.put("api", "建议学习 API 设计和 Swagger 文档编写");
        suggestionMap.put("springboot", "建议学习 Spring Boot 自动配置原理");
        suggestionMap.put("springcloud", "建议学习 Spring Cloud 微服务组件");
        suggestionMap.put("aws", "建议学习 AWS 云服务架构设计");
        suggestionMap.put("linux", "建议学习 Linux 系统管理和 Shell 脚本");
        suggestionMap.put("javascript", "建议学习 ES6+ 新特性和异步编程");
        suggestionMap.put("typescript", "建议学习 TypeScript 类型系统和装饰器");
        suggestionMap.put("kafka", "建议学习 Kafka 消息队列特性和流处理");
        suggestionMap.put("rabbitmq", "建议学习 RabbitMQ 消息队列和异步通信");
        suggestionMap.put("swagger", "建议学习 Swagger/OpenAPI 文档自动生成");
        suggestionMap.put("mvc", "建议学习 MVC 架构模式和分层设计");
        suggestionMap.put("ef", "建议学习 Entity Framework  ORM 框架");
        suggestionMap.put("linq", "建议学习 LINQ 语言集成查询");
        suggestionMap.put("asp.net", "建议深入学习 ASP.NET Core 框架");

        for (String skill : missingSkills) {
            String category = getSkillCategory(skill);
            int importance = importanceMap.getOrDefault(skill, 3);
            String suggestion = suggestionMap.getOrDefault(skill,
                "建议系统学习 " + skill + " 相关技术栈");

            SkillGap gap = SkillGap.builder()
                    .skill(skill)
                    .category(category)
                    .importance(importance)
                    .reason("简历中未体现该技能")
                    .suggestion(suggestion)
                    .build();

            skillGaps.add(gap);
        }

        // 按重要性排序
        skillGaps.sort((a, b) -> b.getImportance() - a.getImportance());

        return skillGaps;
    }

    /**
     * 获取技能所属分类（互斥分类）
     */
    private String getSkillCategory(String skill) {
        // 使用 Set 进行快速查找
        Set<String> frontendSkills = new HashSet<>(Arrays.asList(
            "vue", "react", "angular", "javascript", "typescript", "html", "css",
            "jquery", "bootstrap", "ajax", "es6", "sass", "less", "webpack",
            "jqueryui", "tailwindcss", "materialui", "elementui", "antd", "nuxt", "nextjs"
        ));
        Set<String> backendSkills = new HashSet<>(Arrays.asList(
            "java", "c#", "python", "springboot", "springcloud", "nodejs",
            "asp.net", "django", "flask", "api", "webapi", "restful",
            "graphql", "grpc", "websocket", "jwt", "oauth", "lambda", "linq",
            "rabbitmq", "kafka", "mvc", "ef", "swagger", "openapi"
        ));
        Set<String> databaseSkills = new HashSet<>(Arrays.asList(
            "mysql", "sqlserver", "postgresql", "mongodb", "redis", "oracle",
            "elasticsearch", "sql", "memcache", "sqlite", "mariadb", "kafka", "rabbitmq"
        ));
        Set<String> devopsSkills = new HashSet<>(Arrays.asList(
            "git", "docker", "kubernetes", "linux", "jenkins", "nginx",
            "ci/cd", "tomcat", "aws", "aliyun", "azure", "gcp",
            "github", "gitlab", "svn", "helm", "github-actions", "gitlab-ci",
            "maven", "gradle"
        ));

        if (frontendSkills.contains(skill)) return "frontend";
        if (backendSkills.contains(skill)) return "backend";
        if (databaseSkills.contains(skill)) return "database";
        if (devopsSkills.contains(skill)) return "devops";
        return "tools";
    }

    // ==================== 主方法 ====================
    public AnalysisResponse analyze(AnalysisRequest request) {
        String resumeText = request.getResumeText();
        String jobDesc = request.getJobDescription();

        // 提取技能
        Set<String> resumeSkills = extractSkills(resumeText);
        Set<String> jobSkills = extractSkills(jobDesc);

        log.info("========== 简历分析 ==========");
        log.info("简历技能 ({}): {}", resumeSkills.size(), resumeSkills);
        log.info("职位技能 ({}): {}", jobSkills.size(), jobSkills);

        // 计算匹配
        MatchResult match = calculateMatch(resumeSkills, jobSkills);

        // 计算分类评分（用于雷达图）
        Map<String, AnalysisResponse.CategoryScore> categoryScores =
            calculateCategoryScores(resumeSkills, jobSkills);

        // 分析技能差距
        List<SkillGap> skillGaps = analyzeSkillGaps(resumeSkills, jobSkills);

        // 生成建议
        List<String> suggestions = generateSuggestions(resumeText, resumeSkills,
                match.matched, match.missing, match.score, categoryScores);

        // 生成优化提示
        List<AnalysisResponse.OptimizationTip> optimizationTips = generateOptimizationTips(
                resumeText, jobSkills, skillGaps);

        // ATS评分
        int atsScore = calculateAtsScore(resumeText, resumeSkills);

        // 结构分析
        AnalysisResponse.StructureInfo structure = analyzeStructure(resumeText);

        // 关键词词频分析
        Map<String, Integer> keywordFrequency = analyzeKeywordFrequency(resumeText, jobSkills);

        AnalysisResponse response = AnalysisResponse.builder()
                .atsScore(atsScore)
                .matchScore(match.score)
                .foundKeywords(new ArrayList<>(match.matched))
                .missingKeywords(new ArrayList<>(match.missing))
                .suggestions(suggestions)
                .keywordFrequency(keywordFrequency)
                .structure(structure)
                .categoryScores(categoryScores)
                .skillGaps(skillGaps)
                .optimizationTips(optimizationTips)
                .build();

        log.info("========== 分析结果 ==========");
        log.info("匹配技能 ({}): {}", match.matched.size(), match.matched);
        log.info("缺失技能 ({}): {}", match.missing.size(), match.missing);
        log.info("匹配度: {}% ({}/{})", match.score, match.matched.size(), jobSkills.size());
        log.info("分类评分: {}", categoryScores);
        log.info("建议: {}", suggestions);

        return response;
    }

    // ==================== 关键词词频分析 ====================
    private Map<String, Integer> analyzeKeywordFrequency(String resumeText, Set<String> jobSkills) {
        Map<String, Integer> frequency = new HashMap<>();
        String lower = resumeText.toLowerCase();

        for (String skill : jobSkills) {
            // 计算技能在简历中出现的次数
            String pattern = "\\b" + skill.replace("+", "\\+") + "\\b";
            int count = 0;
            try {
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(lower);
                while (m.find()) count++;
            } catch (Exception e) {
                // 忽略正则错误
            }
            if (count > 0) {
                frequency.put(skill, count);
            }
        }

        return frequency;
    }

    // ==================== 生成优化提示 ====================
    private List<AnalysisResponse.OptimizationTip> generateOptimizationTips(
            String resumeText, Set<String> jobSkills, List<SkillGap> skillGaps) {
        List<AnalysisResponse.OptimizationTip> tips = new ArrayList<>();

        // 结构优化提示
        if (!containsKeyword(resumeText, SUMMARY_KW)) {
            tips.add(AnalysisResponse.OptimizationTip.builder()
                    .type("add")
                    .originalText("")
                    .suggestedText("【个人简介】一段 2-3 行的个人简介，突出您的核心优势和职业目标")
                    .reason("简历缺少个人简介部分，这会影响招聘方的第一印象")
                    .build());
        }

        if (!containsKeyword(resumeText, EXP_KW)) {
            tips.add(AnalysisResponse.OptimizationTip.builder()
                    .type("add")
                    .originalText("")
                    .suggestedText("【工作经历】详细描述每段工作的职责和成就，使用量化数据")
                    .reason("简历缺少工作经历部分，这是招聘方最关注的内容")
                    .build());
        }

        // 技能差距优化提示
        for (SkillGap gap : skillGaps.stream().limit(3).toList()) {
            tips.add(AnalysisResponse.OptimizationTip.builder()
                    .type("add")
                    .originalText("")
                    .suggestedText("【建议添加】" + gap.getSkill() + " 相关技能描述")
                    .reason(gap.getSuggestion())
                    .build());
        }

        // 长度优化
        int words = resumeText.split("\\s+").length;
        if (words < 300) {
            tips.add(AnalysisResponse.OptimizationTip.builder()
                    .type("improve")
                    .originalText("简历内容偏少")
                    .suggestedText("建议补充更多项目细节和成果描述，每段经历建议 3-5 个要点")
                    .reason("简历内容较少，建议丰富至 300-500 字")
                    .build());
        }

        return tips;
    }
}
