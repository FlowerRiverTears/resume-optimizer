package com.flowerrivertears.resumeoptimizer.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.rag")
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    private EmbeddingConfig embedding = new EmbeddingConfig();
    private VectorStoreConfigProps vectorStore = new VectorStoreConfigProps();
    private DocumentConfig document = new DocumentConfig();

    public static class EmbeddingConfig {
        private String provider = "local";
        private OpenAiEmbeddingConfig openai = new OpenAiEmbeddingConfig();

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public OpenAiEmbeddingConfig getOpenai() { return openai; }
        public void setOpenai(OpenAiEmbeddingConfig openai) { this.openai = openai; }
    }

    public static class OpenAiEmbeddingConfig {
        private String apiKey = "sk-your-api-key";
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "text-embedding-3-small";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
    }

    public static class VectorStoreConfigProps {
        private String type = "in-memory";
        private int maxResults = 5;
        private double minScore = 0.5;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }
    }

    public static class DocumentConfig {
        private int chunkSize = 500;
        private int chunkOverlap = 50;

        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Initializing InMemory EmbeddingStore");
        return new InMemoryEmbeddingStore<>();
    }

    public EmbeddingConfig getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingConfig embedding) { this.embedding = embedding; }
    public VectorStoreConfigProps getVectorStore() { return vectorStore; }
    public void setVectorStore(VectorStoreConfigProps vectorStore) { this.vectorStore = vectorStore; }
    public DocumentConfig getDocument() { return document; }
    public void setDocument(DocumentConfig document) { this.document = document; }
}
