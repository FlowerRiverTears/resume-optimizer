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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "些", "什么", "怎么", "如何", "吗",
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "shall", "to", "of", "in", "for",
            "on", "with", "at", "by", "from", "as", "into", "through", "and", "or"
    );

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private VectorStoreConfig vectorStoreConfig;

    private final EmbeddingModel embeddingModel;

    private final Map<String, String> knowledgeBase = new ConcurrentHashMap<>();
    private final Map<String, List<String>> keywordIndex = new ConcurrentHashMap<>();
    private final AtomicInteger chunkCounter = new AtomicInteger(0);

    public RagService() {
        log.info("Initializing local embedding model (all-MiniLM-L6-v2)");
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public void ingestDocument(String content, String source, String category) {
        log.info("Ingesting document: source={}, category={}, length={}", source, category, content.length());

        List<String> chunks = semanticChunk(content);

        List<TextSegment> allSegments = new ArrayList<>();
        List<Embedding> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int chunkIdx = chunkCounter.getAndIncrement();

            TextSegment segment = TextSegment.from(chunk,
                    dev.langchain4j.data.document.Metadata.from(Map.of(
                            "source", source != null ? source : "unknown",
                            "category", category != null ? category : "general",
                            "chunkIndex", chunkIdx,
                            "totalChunks", chunks.size(),
                            "ingestedAt", System.currentTimeMillis()
                    )));

            Embedding embedding = embeddingModel.embed(segment).content();
            allSegments.add(segment);
            allEmbeddings.add(embedding);

            indexKeywords(chunk, source + "-chunk-" + chunkIdx);
        }

        if (!allSegments.isEmpty()) {
            embeddingStore.addAll(allEmbeddings, allSegments);
        }

        knowledgeBase.put(source != null ? source : "doc-" + System.currentTimeMillis(), content);
        log.info("Document ingested: {} chunks, total KB size: {}", chunks.size(), knowledgeBase.size());
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
                            "category", category != null ? category : "general",
                            "ingestedAt", System.currentTimeMillis()
                    )));

            List<TextSegment> segments = splitter.split(doc);
            for (TextSegment segment : segments) {
                int chunkIdx = chunkCounter.getAndIncrement();
                Embedding embedding = embeddingModel.embed(segment).content();
                allSegments.add(segment);
                allEmbeddings.add(embedding);
                indexKeywords(segment.text(), source + "-chunk-" + chunkIdx);
            }
        }

        embeddingStore.addAll(allEmbeddings, allSegments);
        log.info("Batch ingested {} segments from source: {}", allSegments.size(), source);
    }

    public List<SearchResult> search(String query, int maxResults) {
        return search(query, maxResults, vectorStoreConfig.getVectorStore().getMinScore());
    }

    public List<SearchResult> search(String query, int maxResults, double minScore) {
        return hybridSearch(query, maxResults, minScore);
    }

    public List<SearchResult> hybridSearch(String query, int maxResults, double minScore) {
        log.info("Hybrid search: query='{}', maxResults={}, minScore={}", query, maxResults, minScore);
        long startTime = System.currentTimeMillis();

        List<SearchResult> vectorResults = vectorSearch(query, maxResults * 2, minScore);
        List<SearchResult> keywordResults = keywordSearch(query, maxResults * 2);

        Map<String, SearchResult> merged = new LinkedHashMap<>();

        for (SearchResult r : vectorResults) {
            String key = r.getContent().hashCode() + "_" + r.getSource();
            merged.put(key, SearchResult.builder()
                    .content(r.getContent())
                    .score(r.getScore())
                    .source(r.getSource())
                    .category(r.getCategory())
                    .weightedScore(r.getWeightedScore())
                    .retrievalMethod("vector")
                    .chunkIndex(r.getChunkIndex())
                    .vectorScore(r.getScore())
                    .keywordScore(0.0)
                    .rerankScore(0.0)
                    .build());
        }

        for (SearchResult r : keywordResults) {
            String key = r.getContent().hashCode() + "_" + r.getSource();
            if (merged.containsKey(key)) {
                SearchResult existing = merged.get(key);
                merged.put(key, SearchResult.builder()
                        .content(existing.getContent())
                        .score(existing.getScore())
                        .source(existing.getSource())
                        .category(existing.getCategory())
                        .weightedScore(existing.getWeightedScore())
                        .retrievalMethod("hybrid")
                        .chunkIndex(existing.getChunkIndex())
                        .vectorScore(existing.getVectorScore())
                        .keywordScore(r.getKeywordScore())
                        .rerankScore(0.0)
                        .build());
            } else {
                merged.put(key, SearchResult.builder()
                        .content(r.getContent())
                        .score(r.getScore())
                        .source(r.getSource())
                        .category(r.getCategory())
                        .weightedScore(r.getWeightedScore())
                        .retrievalMethod("keyword")
                        .chunkIndex(r.getChunkIndex())
                        .vectorScore(0.0)
                        .keywordScore(r.getKeywordScore())
                        .rerankScore(0.0)
                        .build());
            }
        }

        List<SearchResult> reranked = rerank(new ArrayList<>(merged.values()), query);

        List<SearchResult> finalResults = reranked.stream()
                .limit(maxResults)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Hybrid search completed: {} results (vector={}, keyword={}, merged={}, reranked={}) in {}ms",
                finalResults.size(), vectorResults.size(), keywordResults.size(), merged.size(), reranked.size(), elapsed);
        return finalResults;
    }

    private List<SearchResult> vectorSearch(String query, int maxResults, double minScore) {
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
            String source = segment.metadata() != null ? segment.metadata().getString("source") : "unknown";
            String category = segment.metadata() != null ? segment.metadata().getString("category") : "general";
            int chunkIdx = segment.metadata() != null && segment.metadata().getInteger("chunkIndex") != null
                    ? segment.metadata().getInteger("chunkIndex") : 0;

            results.add(SearchResult.builder()
                    .content(segment.text())
                    .score(match.score())
                    .source(source)
                    .category(category)
                    .weightedScore(match.score())
                    .retrievalMethod("vector")
                    .chunkIndex(chunkIdx)
                    .vectorScore(match.score())
                    .keywordScore(0.0)
                    .rerankScore(0.0)
                    .build());
        }
        return results;
    }

    private List<SearchResult> keywordSearch(String query, int maxResults) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return Collections.emptyList();

        Map<String, Double> docScores = new HashMap<>();

        for (String token : queryTokens) {
            for (Map.Entry<String, List<String>> entry : keywordIndex.entrySet()) {
                String docKey = entry.getKey();
                List<String> docTokens = entry.getValue();
                long tf = docTokens.stream().filter(t -> t.equals(token)).count();
                if (tf > 0) {
                    double idf = Math.log((double) keywordIndex.size() / (keywordIndex.values().stream()
                            .filter(tokens -> tokens.contains(token)).count() + 1));
                    docScores.merge(docKey, tf * idf, Double::sum);
                }
            }
        }

        return docScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> {
                    String docKey = entry.getKey();
                    String content = knowledgeBase.getOrDefault(
                            docKey.split("-chunk-")[0], "");
                    String source = docKey.contains("-chunk-") ?
                            docKey.split("-chunk-")[0] : docKey;
                    double normalizedScore = Math.min(1.0, entry.getValue() / 5.0);

                    return SearchResult.builder()
                            .content(content.length() > 500 ? content.substring(0, 500) : content)
                            .score(normalizedScore)
                            .source(source)
                            .category("keyword-match")
                            .weightedScore(normalizedScore)
                            .retrievalMethod("keyword")
                            .chunkIndex(0)
                            .vectorScore(0.0)
                            .keywordScore(normalizedScore)
                            .rerankScore(0.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SearchResult> rerank(List<SearchResult> results, String query) {
        List<String> queryTokens = tokenize(query);

        for (SearchResult r : results) {
            double vectorComponent = r.getVectorScore() * 0.6;
            double keywordComponent = r.getKeywordScore() * 0.25;

            List<String> contentTokens = tokenize(r.getContent());
            long overlap = queryTokens.stream()
                    .filter(contentTokens::contains)
                    .count();
            double overlapScore = queryTokens.isEmpty() ? 0 :
                    Math.min(1.0, (double) overlap / queryTokens.size());
            double contentRelevance = overlapScore * 0.15;

            double rerankScore = vectorComponent + keywordComponent + contentRelevance;

            r.setRerankScore(Math.round(rerankScore * 10000.0) / 10000.0);
            r.setWeightedScore(r.getRerankScore());
        }

        results.sort((a, b) -> Double.compare(b.getRerankScore(), a.getRerankScore()));
        return results;
    }

    private List<String> semanticChunk(String content) {
        int chunkSize = vectorStoreConfig.getDocument().getChunkSize();
        int chunkOverlap = vectorStoreConfig.getDocument().getChunkOverlap();

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String para : paragraphs) {
            if (currentChunk.length() + para.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                String overlapText = getOverlapText(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlapText);
            }
            if (currentChunk.length() > 0) currentChunk.append("\n\n");
            currentChunk.append(para);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        if (chunks.isEmpty()) {
            chunks.add(content);
        }

        return chunks;
    }

    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }

    private void indexKeywords(String content, String docKey) {
        List<String> tokens = tokenize(content);
        keywordIndex.put(docKey, tokens);
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        String[] parts = text.toLowerCase()
                .replaceAll("[^\\w\\u4e00-\\u9fff]", " ")
                .split("\\s+");

        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank() || part.length() < 2 || STOP_WORDS.contains(part)) continue;
            tokens.add(part);

            if (isChinese(part)) {
                for (int i = 0; i < part.length() - 1; i++) {
                    String bigram = part.substring(i, i + 2);
                    if (!STOP_WORDS.contains(bigram)) {
                        tokens.add(bigram);
                    }
                }
            }
        }
        return tokens;
    }

    private boolean isChinese(String text) {
        return text.chars().anyMatch(c -> c >= 0x4e00 && c <= 0x9fff);
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
        keywordIndex.clear();
    }

    public int getKnowledgeBaseSize() {
        return knowledgeBase.size();
    }

    public int getKeywordIndexSize() {
        return keywordIndex.size();
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
