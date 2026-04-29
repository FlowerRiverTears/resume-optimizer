package com.flowerrivertears.resumeoptimizer;

import com.flowerrivertears.resumeoptimizer.model.*;
import com.flowerrivertears.resumeoptimizer.service.ApiKeyService;
import com.flowerrivertears.resumeoptimizer.service.RagService;
import com.flowerrivertears.resumeoptimizer.service.WeightedRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiAgentIntegrationTest {

    @Autowired
    private RagService ragService;

    @Autowired
    private WeightedRetrievalService weightedRetrievalService;

    @Autowired
    private ApiKeyService apiKeyService;

    private static final String SAMPLE_RESUME = """
            姓名：张三
            邮箱：zhangsan@example.com
            
            个人简介
            5年Java后端开发经验，精通SpringBoot、MySQL、Redis等技术栈。
            
            工作经历
            高级Java开发工程师 - ABC科技有限公司
            - 负责核心交易系统开发，使用SpringBoot微服务架构
            - 主导数据库优化项目，查询效率提升50%
            - 熟练使用Redis缓存和Docker容器化部署
            
            技能特长
            - 编程语言：Java、Python、JavaScript
            - 框架：Spring、SpringBoot、MyBatis、Vue
            - 数据库：MySQL、PostgreSQL、Redis、MongoDB
            - 工具：Git、Docker、Kubernetes、Linux
            """;

    private static final String SAMPLE_JOB = """
            职位：高级Java开发工程师
            岗位要求：
            - 5年以上Java开发经验
            - 精通Spring、SpringBoot微服务框架
            - 熟悉MySQL、Redis数据库
            - 有Docker、Kubernetes容器化经验
            - 了解分布式系统、高并发处理
            - 有电商、金融项目经验优先
            """;

    @BeforeEach
    void setUp() {
        ragService.clearKnowledgeBase();
    }

    @Test
    void testRagIngestAndSearch() {
        ragService.ingestDocument(SAMPLE_RESUME, "test-resume", "resume");
        ragService.ingestDocument(SAMPLE_JOB, "test-job", "job");

        assertTrue(ragService.getKnowledgeBaseSize() >= 2);

        List<SearchResult> results = ragService.search("Java开发经验", 3);
        assertNotNull(results);
    }

    @Test
    void testRagIngestResumeData() {
        ragService.ingestResumeData(SAMPLE_RESUME, SAMPLE_JOB);

        assertTrue(ragService.getKnowledgeBaseSize() >= 2);
    }

    @Test
    void testRagSearchWithMinScore() {
        ragService.ingestDocument(SAMPLE_RESUME, "test-resume", "resume");

        List<SearchResult> results = ragService.search("SpringBoot微服务", 5, 0.0);
        assertNotNull(results);
    }

    @Test
    void testRagBatchIngest() {
        List<String> contents = Arrays.asList(
                "Java开发工程师需要掌握SpringBoot框架",
                "前端开发需要熟悉Vue或React",
                "数据库优化是后端开发的重要技能"
        );

        ragService.ingestDocuments(contents, "skills-knowledge", "skill");

        List<SearchResult> results = ragService.search("Java框架", 3);
        assertNotNull(results);
    }

    @Test
    void testWeightConfigDefaults() {
        WeightConfig config = weightedRetrievalService.getCurrentConfig();

        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertNotNull(config.getDimensions());
        assertTrue(config.getDimensions().containsKey(WeightConfig.DIM_SKILL_MATCH));
        assertTrue(config.getDimensions().containsKey(WeightConfig.DIM_SEMANTIC_SIMILARITY));
        assertTrue(config.getDimensions().containsKey(WeightConfig.DIM_CATEGORY_RELEVANCE));
        assertTrue(config.getDimensions().containsKey(WeightConfig.DIM_EXPERIENCE_LEVEL));
    }

    @Test
    void testWeightConfigUpdate() {
        Map<String, Double> newWeights = new HashMap<>();
        newWeights.put(WeightConfig.DIM_SKILL_MATCH, 0.5);
        newWeights.put(WeightConfig.DIM_SEMANTIC_SIMILARITY, 0.3);
        newWeights.put(WeightConfig.DIM_CATEGORY_RELEVANCE, 0.1);
        newWeights.put(WeightConfig.DIM_EXPERIENCE_LEVEL, 0.1);

        WeightConfig config = weightedRetrievalService.updateWeights(newWeights);

        assertNotNull(config);
        assertEquals(0.5, config.getDimensions().get(WeightConfig.DIM_SKILL_MATCH), 0.01);
    }

    @Test
    void testWeightConfigNormalization() {
        WeightConfig config = WeightConfig.builder()
                .enabled(true)
                .dimensions(new HashMap<>(Map.of(
                        WeightConfig.DIM_SKILL_MATCH, 2.0,
                        WeightConfig.DIM_SEMANTIC_SIMILARITY, 2.0,
                        WeightConfig.DIM_CATEGORY_RELEVANCE, 4.0,
                        WeightConfig.DIM_EXPERIENCE_LEVEL, 2.0
                )))
                .build();

        config.normalize();

        double sum = config.getDimensions().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001);
    }

    @Test
    void testWeightedSearchResults() {
        ragService.ingestDocument(SAMPLE_RESUME, "test-resume", "resume");
        ragService.ingestDocument(SAMPLE_JOB, "test-job", "job");

        List<SearchResult> rawResults = ragService.search("Java开发", 5, 0.0);

        if (!rawResults.isEmpty()) {
            Set<String> matchedSkills = new HashSet<>(Arrays.asList("java", "springboot", "mysql"));
            Set<String> jobSkills = new HashSet<>(Arrays.asList("java", "springboot", "mysql", "docker", "kubernetes"));

            List<SearchResult> weightedResults = weightedRetrievalService.applyWeights(
                    rawResults, "Java开发", matchedSkills, jobSkills);

            assertNotNull(weightedResults);
            assertEquals(rawResults.size(), weightedResults.size());

            for (int i = 1; i < weightedResults.size(); i++) {
                assertTrue(weightedResults.get(i - 1).getWeightedScore() >=
                        weightedResults.get(i).getWeightedScore());
            }
        }
    }

    @Test
    void testCustomWeightConfig() {
        WeightConfig customConfig = WeightConfig.builder()
                .enabled(true)
                .dimensions(new HashMap<>(Map.of(
                        WeightConfig.DIM_SKILL_MATCH, 0.4,
                        WeightConfig.DIM_SEMANTIC_SIMILARITY, 0.3,
                        WeightConfig.DIM_CATEGORY_RELEVANCE, 0.2,
                        WeightConfig.DIM_EXPERIENCE_LEVEL, 0.1
                )))
                .build();

        weightedRetrievalService.saveCustomConfig("test-config", customConfig);

        WeightConfig retrieved = weightedRetrievalService.getCustomConfig("test-config");
        assertNotNull(retrieved);

        Map<String, WeightConfig> allConfigs = weightedRetrievalService.getAllCustomConfigs();
        assertTrue(allConfigs.containsKey("test-config"));

        weightedRetrievalService.deleteCustomConfig("test-config");
        assertNull(weightedRetrievalService.getCustomConfig("test-config"));
    }

    @Test
    void testSearchResultBuilder() {
        SearchResult result = SearchResult.builder()
                .content("测试内容")
                .score(0.85)
                .source("test-source")
                .category("resume")
                .weightedScore(0.78)
                .build();

        assertEquals("测试内容", result.getContent());
        assertEquals(0.85, result.getScore());
        assertEquals("test-source", result.getSource());
        assertEquals("resume", result.getCategory());
        assertEquals(0.78, result.getWeightedScore());
    }

    @Test
    void testAiChatRequestBuilder() {
        AiChatRequest request = AiChatRequest.builder()
                .message("帮我分析简历")
                .context("Java开发岗位")
                .provider("openai")
                .keyId("test-key-123")
                .apiKey("sk-test-key")
                .baseUrl("https://api.openai.com/v1")
                .modelName("gpt-4o-mini")
                .build();

        assertEquals("帮我分析简历", request.getMessage());
        assertEquals("openai", request.getProvider());
        assertEquals("test-key-123", request.getKeyId());
        assertEquals("sk-test-key", request.getApiKey());
    }

    @Test
    void testAiChatResponseBuilder() {
        List<SearchResult> results = Arrays.asList(
                SearchResult.builder().content("结果1").score(0.9).build()
        );

        AiChatResponse response = AiChatResponse.builder()
                .answer("分析结果")
                .provider("minimax")
                .model("MiniMax-M2.7")
                .searchResults(results)
                .responseTimeMs(1500)
                .build();

        assertEquals("分析结果", response.getAnswer());
        assertEquals("minimax", response.getProvider());
        assertEquals("MiniMax-M2.7", response.getModel());
        assertEquals(1, response.getSearchResults().size());
    }

    @Test
    void testApiKeyValidateRequestBuilder() {
        ApiKeyValidateRequest request = ApiKeyValidateRequest.builder()
                .apiKey("sk-test-minimax-key")
                .provider("minimax")
                .baseUrl("https://api.minimax.io/v1")
                .modelName("MiniMax-M2.7")
                .build();

        assertEquals("sk-test-minimax-key", request.getApiKey());
        assertEquals("minimax", request.getProvider());
        assertEquals("https://api.minimax.io/v1", request.getBaseUrl());
        assertEquals("MiniMax-M2.7", request.getModelName());
    }

    @Test
    void testApiKeyValidateResponseBuilder() {
        ApiKeyValidateResponse response = ApiKeyValidateResponse.builder()
                .valid(true)
                .provider("minimax")
                .modelName("MiniMax-M2.7")
                .message("API Key 验证成功")
                .keyId("minimax-abc12345")
                .build();

        assertTrue(response.isValid());
        assertEquals("minimax", response.getProvider());
        assertEquals("minimax-abc12345", response.getKeyId());
    }

    @Test
    void testApiKeyInfoBuilder() {
        ApiKeyInfo info = ApiKeyInfo.builder()
                .keyId("minimax-abc12345")
                .provider("minimax")
                .modelName("MiniMax-M2.7")
                .baseUrl("https://api.minimax.io/v1")
                .maskedKey("sk-t****key")
                .valid(true)
                .build();

        assertEquals("minimax-abc12345", info.getKeyId());
        assertEquals("minimax", info.getProvider());
        assertEquals("sk-t****key", info.getMaskedKey());
    }

    @Test
    void testSupportedProviders() {
        List<Map<String, String>> providers = apiKeyService.getSupportedProviders();

        assertNotNull(providers);
        assertTrue(providers.size() >= 4);

        boolean hasMinimax = providers.stream()
                .anyMatch(p -> "minimax".equals(p.get("provider")));
        assertTrue(hasMinimax, "Should include minimax provider");

        boolean hasOpenai = providers.stream()
                .anyMatch(p -> "openai".equals(p.get("provider")));
        assertTrue(hasOpenai, "Should include openai provider");
    }

    @Test
    void testApiKeyInvalidKey() {
        ApiKeyValidateRequest request = ApiKeyValidateRequest.builder()
                .apiKey("sk-invalid-key-12345")
                .provider("openai")
                .build();

        ApiKeyValidateResponse response = apiKeyService.validateAndStoreKey(request);

        assertFalse(response.isValid());
        assertNotNull(response.getMessage());
    }
}
