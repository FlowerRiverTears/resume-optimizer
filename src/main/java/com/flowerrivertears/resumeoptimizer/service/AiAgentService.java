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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    private static final Pattern THINKING_PATTERN = Pattern.compile("<think[^>]*>([\\s\\S]*?)</think\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern THINKING_TAG_OPEN = Pattern.compile("<think[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern THINKING_TAG_CLOSE = Pattern.compile("</think\\s*>", Pattern.CASE_INSENSITIVE);

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
            5. 根据用户技能生成专业简历
            
            请用专业、友好的语气回答问题。回答需要基于事实数据，不要编造信息。
            如果检索到的数据不足以回答问题，请如实告知用户。
            回答要完整详细，不要中途截断。
            """;

    private static final String RESUME_GENERATE_PROMPT = """
            请根据以下信息，生成一份完整的专业简历。要求格式规范、内容充实、语言专业。
            
            用户技能和经历：
            {{userInput}}
            
            检索到的相关岗位要求：
            {{searchResults}}
            
            请生成包含以下部分的完整简历：
            1. 基本信息（姓名、联系方式等占位符）
            2. 求职意向（根据技能推荐最匹配的岗位）
            3. 教育背景
            4. 专业技能（分类列出，标注熟练度）
            5. 项目经历（每个项目包含：项目名称、技术栈、职责描述、项目成果）
            6. 自我评价
            
            注意：
            - 根据检索到的岗位要求，突出匹配的技能
            - 项目描述要具体，包含量化成果
            - 技能分类要清晰（后端/前端/数据库/工具/其他）
            """;

    private static final String DEEP_ANALYSIS_PROMPT = """
            请对以下简历进行深度AI分析，基于检索到的岗位市场数据给出专业建议。
            
            简历内容：
            {{resumeText}}
            
            职位描述：
            {{jobDescription}}
            
            知识库检索结果（岗位市场数据）：
            {{searchResults}}
            
            请从以下维度进行深度分析：
            
            1. 🎯 岗位匹配分析
               - 根据知识库数据，该简历最匹配哪些岗位？
               - 匹配度评估（高/中/低）
               - 与市场同类岗位要求的差距
            
            2. 💪 核心竞争力
               - 最突出的3个技术优势
               - 差异化竞争力分析
            
            3. 📈 技能提升建议
               - 短期可提升（1-3个月）：具体学什么、怎么学
               - 中期提升方向（3-6个月）：进阶路径
               - 长期发展规划：职业成长建议
            
            4. 📝 简历优化建议
               - 需要补充的关键词
               - 项目描述的改进方向
               - 技能呈现的优化方式
            
            5. 🔍 市场趋势
               - 相关岗位的市场需求
               - 薪资参考范围
               - 行业发展方向
            """;

    private static final String SKILL_SEARCH_PROMPT = """
            用户正在查找关于"{{query}}"的信息。
            
            以下是检索到的相关数据：
            {{searchResults}}
            
            请基于以上数据，回答用户的问题。如果数据不足，请说明需要补充哪些信息。
            回答要完整详细。
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
        String rawAnswer = model.chat(fullPrompt);

        ParsedResponse parsed = parseThinkingContent(rawAnswer);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("AI Chat response generated in {}ms, thinking={}, answer={}chars",
                elapsed, parsed.thinking != null ? parsed.thinking.length() : 0, parsed.answer.length());

        return AiChatResponse.builder()
                .answer(parsed.answer)
                .thinking(parsed.thinking)
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

        List<SearchResult> searchResults = ragService.search(
                "岗位要求 技能匹配 " + (jobDescription != null ? jobDescription : resumeText), 5, 0.3);

        if (weightedRetrievalService.isEnabled() && !searchResults.isEmpty()) {
            searchResults = weightedRetrievalService.applyWeights(
                    searchResults, "岗位匹配分析", null, null);
        }

        String searchContext = searchResults.stream()
                .map(r -> "[来源: " + r.getSource() + ", 相关度: " +
                        String.format("%.2f", r.getWeightedScore()) + "] " + r.getContent())
                .collect(Collectors.joining("\n\n"));

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("resumeText", resumeText);
        templateVars.put("jobDescription", jobDescription != null ? jobDescription : "未提供");
        templateVars.put("searchResults", searchContext.isEmpty() ? "暂无额外检索数据" : searchContext);

        PromptTemplate promptTemplate = PromptTemplate.from(DEEP_ANALYSIS_PROMPT);
        Prompt prompt = promptTemplate.apply(templateVars);

        ChatModel model = resolveModel(provider, keyId, null, null, null);
        String rawAnswer = model.chat(prompt.text());

        ParsedResponse parsed = parseThinkingContent(rawAnswer);

        long elapsed = System.currentTimeMillis() - startTime;

        return AiChatResponse.builder()
                .answer(parsed.answer)
                .thinking(parsed.thinking)
                .provider(provider != null ? provider : "default")
                .model("rag-deep-analysis")
                .searchResults(searchResults)
                .responseTimeMs(elapsed)
                .build();
    }

    public AiChatResponse generateResume(String userInput, String provider, String keyId) {
        long startTime = System.currentTimeMillis();
        log.info("AI resume generation request: provider={}, keyId={}", provider, keyId);

        List<SearchResult> searchResults = ragService.search(
                "岗位要求 " + userInput, 5, 0.3);

        if (weightedRetrievalService.isEnabled() && !searchResults.isEmpty()) {
            searchResults = weightedRetrievalService.applyWeights(
                    searchResults, userInput, null, null);
        }

        String searchContext = searchResults.stream()
                .map(r -> "[来源: " + r.getSource() + "] " + r.getContent())
                .collect(Collectors.joining("\n"));

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("userInput", userInput);
        templateVars.put("searchResults", searchContext.isEmpty() ? "暂无额外数据" : searchContext);

        PromptTemplate promptTemplate = PromptTemplate.from(RESUME_GENERATE_PROMPT);
        Prompt prompt = promptTemplate.apply(templateVars);

        ChatModel model = resolveModel(provider, keyId, null, null, null);
        String rawAnswer = model.chat(prompt.text());

        ParsedResponse parsed = parseThinkingContent(rawAnswer);

        long elapsed = System.currentTimeMillis() - startTime;

        return AiChatResponse.builder()
                .answer(parsed.answer)
                .thinking(parsed.thinking)
                .provider(provider != null ? provider : "default")
                .model("resume-generator")
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
        String rawAnswer = model.chat(prompt.text());

        ParsedResponse parsed = parseThinkingContent(rawAnswer);

        long elapsed = System.currentTimeMillis() - startTime;

        return AiChatResponse.builder()
                .answer(parsed.answer)
                .thinking(parsed.thinking)
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

    private ParsedResponse parseThinkingContent(String rawAnswer) {
        if (rawAnswer == null) {
            return new ParsedResponse("", null);
        }

        String thinking = null;
        String answer = rawAnswer;

        Matcher matcher = THINKING_PATTERN.matcher(rawAnswer);
        if (matcher.find()) {
            thinking = matcher.group(1).trim();
            answer = THINKING_PATTERN.matcher(rawAnswer).replaceAll("").trim();
        } else {
            String partial = rawAnswer;
            Matcher openMatcher = THINKING_TAG_OPEN.matcher(partial);
            if (openMatcher.find()) {
                int thinkStart = openMatcher.start();
                String afterThink = rawAnswer.substring(thinkStart);
                Matcher closeMatcher = THINKING_TAG_CLOSE.matcher(afterThink);

                if (closeMatcher.find()) {
                    int thinkContentStart = openMatcher.end();
                    int thinkContentEnd = thinkStart + closeMatcher.start();
                    thinking = rawAnswer.substring(thinkContentStart, thinkContentEnd).trim();
                    answer = rawAnswer.substring(0, thinkStart).trim();
                    if (answer.isEmpty()) {
                        answer = rawAnswer.substring(thinkStart + closeMatcher.end()).trim();
                    }
                } else {
                    thinking = rawAnswer.substring(openMatcher.end()).trim();
                    answer = rawAnswer.substring(0, thinkStart).trim();
                }
            }
        }

        if (answer.isEmpty() && thinking != null && !thinking.isEmpty()) {
            answer = thinking;
            thinking = null;
        }

        return new ParsedResponse(answer, thinking);
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

    private static class ParsedResponse {
        String answer;
        String thinking;

        ParsedResponse(String answer, String thinking) {
            this.answer = answer;
            this.thinking = thinking;
        }
    }
}
