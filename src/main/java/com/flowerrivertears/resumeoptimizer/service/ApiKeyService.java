package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private static final Map<String, ProviderDefaults> PROVIDER_DEFAULTS = new LinkedHashMap<>();

    static {
        PROVIDER_DEFAULTS.put("openai", new ProviderDefaults(
                "https://api.openai.com/v1", "gpt-4o-mini"));
        PROVIDER_DEFAULTS.put("dashscope", new ProviderDefaults(
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"));
        PROVIDER_DEFAULTS.put("deepseek", new ProviderDefaults(
                "https://api.deepseek.com", "deepseek-chat"));
        PROVIDER_DEFAULTS.put("minimax", new ProviderDefaults(
                "https://api.minimaxi.com/v1", "MiniMax-M2.7"));
    }

    private final Map<String, StoredKey> keyStore = new ConcurrentHashMap<>();

    public ApiKeyValidateResponse validateAndStoreKey(ApiKeyValidateRequest request) {
        String provider = normalizeProvider(request.getProvider());
        ProviderDefaults defaults = PROVIDER_DEFAULTS.getOrDefault(provider,
                new ProviderDefaults("https://api.openai.com/v1", "gpt-4o-mini"));

        String baseUrl = request.getBaseUrl() != null && !request.getBaseUrl().isBlank()
                ? request.getBaseUrl() : defaults.baseUrl;
        String modelName = request.getModelName() != null && !request.getModelName().isBlank()
                ? request.getModelName() : defaults.modelName;

        log.info("Validating API key: provider={}, model={}, baseUrl={}", provider, modelName, baseUrl);

        try {
            ChatModel testModel = createValidationModel(request.getApiKey(), baseUrl, modelName);

            String testResponse = testModel.chat("Hi, reply with just 'OK'.");

            if (testResponse != null && !testResponse.isBlank()) {
                String keyId = generateKeyId(provider);
                StoredKey stored = new StoredKey(
                        keyId, provider, modelName, baseUrl, request.getApiKey(),
                        LocalDateTime.now(), LocalDateTime.now());
                keyStore.put(keyId, stored);

                log.info("API key validated and stored: keyId={}, provider={}", keyId, provider);

                return ApiKeyValidateResponse.builder()
                        .valid(true)
                        .provider(provider)
                        .modelName(modelName)
                        .message("API Key 验证成功，已保存")
                        .keyId(keyId)
                        .build();
            } else {
                return ApiKeyValidateResponse.builder()
                        .valid(false)
                        .provider(provider)
                        .modelName(modelName)
                        .message("API Key 验证失败：模型返回空响应")
                        .build();
            }
        } catch (Exception e) {
            log.warn("API key validation failed: {}", e.getMessage());
            return ApiKeyValidateResponse.builder()
                    .valid(false)
                    .provider(provider)
                    .modelName(modelName)
                    .message("API Key 验证失败：" + extractErrorMessage(e.getMessage()))
                    .build();
        }
    }

    public ChatModel getChatModelForKey(String keyId) {
        StoredKey stored = keyStore.get(keyId);
        if (stored == null) {
            throw new IllegalArgumentException("无效的 Key ID: " + keyId);
        }
        stored.lastUsedAt = LocalDateTime.now();
        return createChatModel(stored.apiKey, stored.baseUrl, stored.modelName);
    }

    public ChatModel createChatModelWithKey(String apiKey, String provider, String baseUrl, String modelName) {
        provider = normalizeProvider(provider);
        ProviderDefaults defaults = PROVIDER_DEFAULTS.getOrDefault(provider,
                new ProviderDefaults("https://api.openai.com/v1", "gpt-4o-mini"));

        String effectiveBaseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : defaults.baseUrl;
        String effectiveModel = modelName != null && !modelName.isBlank() ? modelName : defaults.modelName;

        return createChatModel(apiKey, effectiveBaseUrl, effectiveModel);
    }

    public List<ApiKeyInfo> listKeys() {
        List<ApiKeyInfo> result = new ArrayList<>();
        for (Map.Entry<String, StoredKey> entry : keyStore.entrySet()) {
            StoredKey stored = entry.getValue();
            result.add(ApiKeyInfo.builder()
                    .keyId(stored.keyId)
                    .provider(stored.provider)
                    .modelName(stored.modelName)
                    .baseUrl(stored.baseUrl)
                    .maskedKey(maskKey(stored.apiKey))
                    .valid(true)
                    .createdAt(stored.createdAt)
                    .lastUsedAt(stored.lastUsedAt)
                    .build());
        }
        return result;
    }

    public boolean removeKey(String keyId) {
        return keyStore.remove(keyId) != null;
    }

    public ApiKeyInfo getKeyInfo(String keyId) {
        StoredKey stored = keyStore.get(keyId);
        if (stored == null) return null;
        return ApiKeyInfo.builder()
                .keyId(stored.keyId)
                .provider(stored.provider)
                .modelName(stored.modelName)
                .baseUrl(stored.baseUrl)
                .maskedKey(maskKey(stored.apiKey))
                .valid(true)
                .createdAt(stored.createdAt)
                .lastUsedAt(stored.lastUsedAt)
                .build();
    }

    public boolean hasKey(String keyId) {
        return keyStore.containsKey(keyId);
    }

    public List<Map<String, String>> getSupportedProviders() {
        List<Map<String, String>> providers = new ArrayList<>();
        for (Map.Entry<String, ProviderDefaults> entry : PROVIDER_DEFAULTS.entrySet()) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("provider", entry.getKey());
            info.put("defaultBaseUrl", entry.getValue().baseUrl);
            info.put("defaultModel", entry.getValue().modelName);
            providers.add(info);
        }
        return providers;
    }

    private ChatModel createChatModel(String apiKey, String baseUrl, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(4096)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    private ChatModel createValidationModel(String apiKey, String baseUrl, String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(64)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    private String normalizeProvider(String provider) {
        if (provider == null) return "openai";
        String lower = provider.toLowerCase().trim();
        if (lower.contains("minimax") || lower.contains("min-max") || lower.equals("m2.7")) {
            return "minimax";
        }
        if (lower.contains("dashscope") || lower.contains("qwen") || lower.contains("通义")) {
            return "dashscope";
        }
        if (lower.contains("deepseek")) {
            return "deepseek";
        }
        return lower;
    }

    private String generateKeyId(String provider) {
        return provider + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private String extractErrorMessage(String message) {
        if (message == null) return "未知错误";
        if (message.contains("401")) return "API Key 无效或已过期";
        if (message.contains("403")) return "无权限访问该模型";
        if (message.contains("404")) return "模型不存在或API地址错误";
        if (message.contains("429")) return "请求过于频繁，请稍后重试";
        if (message.contains("500")) return "服务器内部错误";
        if (message.contains("Connection")) return "网络连接失败，请检查API地址";
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }

    private static class StoredKey {
        String keyId;
        String provider;
        String modelName;
        String baseUrl;
        String apiKey;
        LocalDateTime createdAt;
        LocalDateTime lastUsedAt;

        StoredKey(String keyId, String provider, String modelName, String baseUrl,
                  String apiKey, LocalDateTime createdAt, LocalDateTime lastUsedAt) {
            this.keyId = keyId;
            this.provider = provider;
            this.modelName = modelName;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.createdAt = createdAt;
            this.lastUsedAt = lastUsedAt;
        }
    }

    private static class ProviderDefaults {
        String baseUrl;
        String modelName;

        ProviderDefaults(String baseUrl, String modelName) {
            this.baseUrl = baseUrl;
            this.modelName = modelName;
        }
    }
}
