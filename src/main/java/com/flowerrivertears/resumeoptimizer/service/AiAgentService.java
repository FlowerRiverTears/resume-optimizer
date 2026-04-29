package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.*;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    @Autowired
    private ChatModel chatModel;

    @Autowired
    @Qualifier("openaiChatModel")
    private ChatModel openaiChatModel;

    @Autowired
    @Qualifier("dashscopeChatModel")
    private ChatModel dashscopeChatModel;

    @Autowired
    @Qualifier("deepseekChatModel")
    private ChatModel deepseekChatModel;

    @Autowired
    @Qualifier("minimaxChatModel")
    private ChatModel minimaxChatModel;

    @Autowired
    private RagService ragService;

    @Autowired
    private WeightedRetrievalService weightedRetrievalService;

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的简历优化AI助手。你的职责是：
            1. 分析简历与岗位要求的匹配度
            2. 提供针对性的简历优化建议
            3. 帮助用户理解技能差距并给出学习路径
            4. 基于检索到的数据，给出精准的求职建议
            
            请用专业、友好的语气回答问题。回答需要基于事实数据，不要编造信息。
            如果检索到的数据不足以回答问题，请如实告知用户。
            """;

    private static final String RESUME_ANALYSIS_PROMPT = """
            基于以下简历分析数据，请提供专业的解读和建议：
            
            匹配度：{{matchScore}}%
            ATS评分：{{atsScore}}
            匹配技能：{{matchedSkills}}
            缺失技能：{{missingSkills}}
            技能差距：{{skillGaps}}
            
            请从以下角度分析：
            1. 整体匹配度评价
            2. 关键缺失技能的影响
            3. 优化建议的优先级
            4. 短期可提升的方向
            """;

    private static final String SKILL_SEARCH_PROMPT = """
            用户正在查找关于"{{query}}"的信息。
            
            以下是检索到的相关数据：
            {{searchResults}}
            
            请基于以上数据，回答用户的问题。如果数据不足，请说明需要补充哪些信息。
            """;

    public AiChatResponse chat(AiChatRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("AI Chat request: message='{}', provider={}, keyId={}",
                request.getMessage(), request.getProvider(), request.getKeyId());

        ChatModel model = resolveModel(request.getProvider(), request.getKeyId(),
                request.getApiKey(), request.getBaseUrl(), request.getModelName());

        List<SearchResult> searchResults = ragService.search(request.getMessage(), 5);

        if (weightedRetrievalService.isEnabled() && !searchResults.isEmpty()) {
            searchResults = weightedRetrievalService.applyWeights(
                    searchResults, request.getMessage(), null, null);
        }

        String context = buildContext(searchResults, request.getContext());
        String fullPrompt = buildChatPrompt(request.getMessage(), context);
        String answer = model.chat(fullPrompt);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("AI Chat response generated in {}ms", elapsed);

        return AiChatResponse.builder()
                .answer(answer)
                .provider(request.getProvider() != null ? request.getProvider() : "default")
                .model(getModelName(request.getProvider()))
                .searchResults(searchResults)
                .responseTimeMs(elapsed)
                .build();
    }

    public AiChatResponse analyzeWithAi(String resumeText, String jobDescription,
                                         String provider, String keyId) {
        long startTime = System.currentTimeMillis();
        log.info("AI-enhanced resume analysis request: provider={}, keyId={}", provider, keyId);

        ragService.ingestResumeData(resumeText, jobDescription);

        AnalysisRequest analysisRequest = new AnalysisRequest();
        analysisRequest.setResumeText(resumeText);
        analysisRequest.setJobDescription(jobDescription);
        AnalysisResponse analysisResponse = resumeAnalysisService.analyze(analysisRequest);

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("matchScore", analysisResponse.getMatchScore());
        templateVars.put("atsScore", analysisResponse.getAtsScore());
        templateVars.put("matchedSkills", String.join(", ", analysisResponse.getFoundKeywords()));
        templateVars.put("missingSkills", String.join(", ", analysisResponse.getMissingKeywords()));
        templateVars.put("skillGaps", analysisResponse.getSkillGaps().stream()
                .map(g -> g.getSkill() + "(" + g.getImportance() + "/5)")
                .collect(Collectors.joining(", ")));

        PromptTemplate promptTemplate = PromptTemplate.from(RESUME_ANALYSIS_PROMPT);
        Prompt prompt = promptTemplate.apply(templateVars);

        ChatModel model = resolveModel(provider, keyId, null, null, null);
        String aiAnalysis = model.chat(prompt.text());

        List<SearchResult> searchResults = ragService.search(
                "简历优化 " + String.join(" ", analysisResponse.getMissingKeywords()), 3);

        long elapsed = System.currentTimeMillis() - startTime;

        return AiChatResponse.builder()
                .answer(aiAnalysis)
                .provider(provider != null ? provider : "default")
                .model("langchain-workflow")
                .searchResults(searchResults)
                .responseTimeMs(elapsed)
                .build();
    }

    public AiChatResponse searchWithAi(String query, Set<String> matchedSkills,
                                        Set<String> jobSkills, String provider, String keyId) {
        long startTime = System.currentTimeMillis();
        log.info("AI-enhanced search: query='{}', provider={}, keyId={}", query, provider, keyId);

        List<SearchResult> searchResults = ragService.search(query, 5);

        if (weightedRetrievalService.isEnabled()) {
            searchResults = weightedRetrievalService.applyWeights(
                    searchResults, query, matchedSkills, jobSkills);
        }

        String searchContext = searchResults.stream()
                .map(r -> "[来源: " + r.getSource() + ", 相关度: " +
                        String.format("%.2f", r.getWeightedScore()) + "] " + r.getContent())
                .collect(Collectors.joining("\n\n"));

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("query", query);
        templateVars.put("searchResults", searchContext);

        PromptTemplate promptTemplate = PromptTemplate.from(SKILL_SEARCH_PROMPT);
        Prompt prompt = promptTemplate.apply(templateVars);

        ChatModel model = resolveModel(provider, keyId, null, null, null);
        String aiAnswer = model.chat(prompt.text());

        long elapsed = System.currentTimeMillis() - startTime;

        return AiChatResponse.builder()
                .answer(aiAnswer)
                .provider(provider != null ? provider : "default")
                .model("rag-search")
                .searchResults(searchResults)
                .responseTimeMs(elapsed)
                .build();
    }

    public ConversationalRetrievalChain buildConversationalChain(String keyId) {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(ragService.getEmbeddingModel())
                .maxResults(5)
                .minScore(0.5)
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        ChatModel model = keyId != null && apiKeyService.hasKey(keyId)
                ? apiKeyService.getChatModelForKey(keyId) : chatModel;

        return ConversationalRetrievalChain.builder()
                .chatModel(model)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(chatMemory)
                .build();
    }

    private ChatModel resolveModel(String provider, String keyId,
                                    String apiKey, String baseUrl, String modelName) {
        if (keyId != null && apiKeyService.hasKey(keyId)) {
            log.info("Using user-provided key: keyId={}", keyId);
            return apiKeyService.getChatModelForKey(keyId);
        }

        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Using inline API key: provider={}", provider);
            return apiKeyService.createChatModelWithKey(apiKey, provider, baseUrl, modelName);
        }

        return selectPreconfiguredModel(provider);
    }

    private ChatModel selectPreconfiguredModel(String provider) {
        if (provider == null) return chatModel;
        return switch (provider.toLowerCase()) {
            case "openai" -> openaiChatModel;
            case "dashscope" -> dashscopeChatModel;
            case "deepseek" -> deepseekChatModel;
            case "minimax" -> minimaxChatModel;
            default -> chatModel;
        };
    }

    private String getModelName(String provider) {
        if (provider == null) return "default";
        return switch (provider.toLowerCase()) {
            case "openai" -> "gpt-4o-mini";
            case "dashscope" -> "qwen-plus";
            case "deepseek" -> "deepseek-chat";
            case "minimax" -> "MiniMax-M2.7";
            default -> "default";
        };
    }

    private String buildContext(List<SearchResult> searchResults, String extraContext) {
        StringBuilder sb = new StringBuilder();
        if (extraContext != null && !extraContext.isBlank()) {
            sb.append("用户提供的上下文：\n").append(extraContext).append("\n\n");
        }
        if (!searchResults.isEmpty()) {
            sb.append("检索到的相关数据：\n");
            for (SearchResult r : searchResults) {
                sb.append("- [").append(r.getSource()).append("] ")
                        .append(r.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildChatPrompt(String message, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        if (!context.isEmpty()) {
            prompt.append(context).append("\n\n");
        }
        prompt.append("用户问题：").append(message);
        return prompt.toString();
    }
}
