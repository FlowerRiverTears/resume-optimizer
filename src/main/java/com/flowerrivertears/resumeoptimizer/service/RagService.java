package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.config.VectorStoreConfig;
import com.flowerrivertears.resumeoptimizer.model.SearchResult;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private VectorStoreConfig vectorStoreConfig;

    private final EmbeddingModel embeddingModel;

    private final Map<String, String> knowledgeBase = new ConcurrentHashMap<>();

    public RagService() {
        log.info("Initializing local embedding model (all-MiniLM-L6-v2)");
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public void ingestDocument(String content, String source, String category) {
        log.info("Ingesting document: source={}, category={}, length={}", source, category, content.length());

        TextSegment segment = TextSegment.from(content,
                dev.langchain4j.data.document.Metadata.from(Map.of(
                        "source", source != null ? source : "unknown",
                        "category", category != null ? category : "general"
                )));

        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);

        knowledgeBase.put(source != null ? source : "doc-" + System.currentTimeMillis(), content);
        log.info("Document ingested successfully. Total knowledge base size: {}", knowledgeBase.size());
    }

    public void ingestDocuments(List<String> contents, String source, String category) {
        int chunkSize = vectorStoreConfig.getDocument().getChunkSize();
        int chunkOverlap = vectorStoreConfig.getDocument().getChunkOverlap();

        DocumentSplitter splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);

        List<TextSegment> allSegments = new ArrayList<>();
        List<Embedding> allEmbeddings = new ArrayList<>();

        for (String content : contents) {
            Document doc = Document.from(content,
                    dev.langchain4j.data.document.Metadata.from(Map.of(
                            "source", source != null ? source : "unknown",
                            "category", category != null ? category : "general"
                    )));

            List<TextSegment> segments = splitter.split(doc);
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                allSegments.add(segment);
                allEmbeddings.add(embedding);
            }
        }

        embeddingStore.addAll(allEmbeddings, allSegments);
        log.info("Batch ingested {} segments from source: {}", allSegments.size(), source);
    }

    public List<SearchResult> search(String query, int maxResults) {
        return search(query, maxResults, vectorStoreConfig.getVectorStore().getMinScore());
    }

    public List<SearchResult> search(String query, int maxResults, double minScore) {
        log.info("Searching: query='{}', maxResults={}, minScore={}", query, maxResults, minScore);
        long startTime = System.currentTimeMillis();

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        List<SearchResult> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            String source = segment.metadata() != null ?
                    segment.metadata().getString("source") : "unknown";
            String category = segment.metadata() != null ?
                    segment.metadata().getString("category") : "general";

            results.add(SearchResult.builder()
                    .content(segment.text())
                    .score(match.score())
                    .source(source)
                    .category(category)
                    .weightedScore(match.score())
                    .build());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Search completed: found {} results in {}ms", results.size(), elapsed);
        return results;
    }

    public void ingestResumeData(String resumeText, String jobDescription) {
        if (resumeText != null && !resumeText.isBlank()) {
            ingestDocument(resumeText, "resume", "resume");
        }
        if (jobDescription != null && !jobDescription.isBlank()) {
            ingestDocument(jobDescription, "job-description", "job");
        }
    }

    public void clearKnowledgeBase() {
        log.info("Clearing knowledge base");
        knowledgeBase.clear();
    }

    public int getKnowledgeBaseSize() {
        return knowledgeBase.size();
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
