package com.flowerrivertears.resumeoptimizer.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "ai.llm")
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    private String provider = "openai";
    private OpenAiProvider openai = new OpenAiProvider();
    private OpenAiProvider dashscope = new OpenAiProvider();
    private OpenAiProvider deepseek = new OpenAiProvider();
    private OpenAiProvider minimax = new OpenAiProvider();

    public static class OpenAiProvider {
        private String apiKey = "sk-your-api-key";
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "gpt-4o-mini";
        private double temperature = 0.7;
        private int maxTokens = 2048;
        private int timeout = 60;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    @Bean
    @Primary
    public ChatModel chatModel() {
        OpenAiProvider active = getActiveProvider();
        log.info("Initializing ChatModel: provider={}, model={}, baseUrl={}",
                provider, active.getModelName(), active.getBaseUrl());

        return OpenAiChatModel.builder()
                .apiKey(active.getApiKey())
                .baseUrl(active.getBaseUrl())
                .modelName(active.getModelName())
                .temperature(active.getTemperature())
                .maxTokens(active.getMaxTokens())
                .timeout(Duration.ofSeconds(active.getTimeout()))
                .build();
    }

    @Bean("openaiChatModel")
    public ChatModel openaiChatModel() {
        log.info("Initializing OpenAI ChatModel: model={}, baseUrl={}",
                openai.getModelName(), openai.getBaseUrl());
        return buildModel(openai);
    }

    @Bean("dashscopeChatModel")
    public ChatModel dashscopeChatModel() {
        log.info("Initializing DashScope ChatModel: model={}, baseUrl={}",
                dashscope.getModelName(), dashscope.getBaseUrl());
        return buildModel(dashscope);
    }

    @Bean("deepseekChatModel")
    public ChatModel deepseekChatModel() {
        log.info("Initializing DeepSeek ChatModel: model={}, baseUrl={}",
                deepseek.getModelName(), deepseek.getBaseUrl());
        return buildModel(deepseek);
    }

    @Bean("minimaxChatModel")
    public ChatModel minimaxChatModel() {
        log.info("Initializing Minimax ChatModel: model={}, baseUrl={}",
                minimax.getModelName(), minimax.getBaseUrl());
        return buildModel(minimax);
    }

    private ChatModel buildModel(OpenAiProvider p) {
        return OpenAiChatModel.builder()
                .apiKey(p.getApiKey())
                .baseUrl(p.getBaseUrl())
                .modelName(p.getModelName())
                .temperature(p.getTemperature())
                .maxTokens(p.getMaxTokens())
                .timeout(Duration.ofSeconds(p.getTimeout()))
                .build();
    }

    private OpenAiProvider getActiveProvider() {
        return switch (provider.toLowerCase()) {
            case "dashscope" -> dashscope;
            case "deepseek" -> deepseek;
            case "minimax" -> minimax;
            default -> openai;
        };
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public OpenAiProvider getOpenai() { return openai; }
    public void setOpenai(OpenAiProvider openai) { this.openai = openai; }
    public OpenAiProvider getDashscope() { return dashscope; }
    public void setDashscope(OpenAiProvider dashscope) { this.dashscope = dashscope; }
    public OpenAiProvider getDeepseek() { return deepseek; }
    public void setDeepseek(OpenAiProvider deepseek) { this.deepseek = deepseek; }
    public OpenAiProvider getMinimax() { return minimax; }
    public void setMinimax(OpenAiProvider minimax) { this.minimax = minimax; }
}
