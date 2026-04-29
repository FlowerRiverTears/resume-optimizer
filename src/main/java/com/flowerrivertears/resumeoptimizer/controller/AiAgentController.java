package com.flowerrivertears.resumeoptimizer.controller;

import com.flowerrivertears.resumeoptimizer.model.*;
import com.flowerrivertears.resumeoptimizer.service.AiAgentService;
import com.flowerrivertears.resumeoptimizer.service.ApiKeyService;
import com.flowerrivertears.resumeoptimizer.service.RagService;
import com.flowerrivertears.resumeoptimizer.service.WeightedRetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiAgentController {

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private RagService ragService;

    @Autowired
    private WeightedRetrievalService weightedRetrievalService;

    @Autowired
    private ApiKeyService apiKeyService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        AiChatResponse response = aiAgentService.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-resume")
    public ResponseEntity<AiChatResponse> generateResume(@RequestBody Map<String, Object> request) {
        String userInput = (String) request.get("userInput");
        String provider = (String) request.get("provider");
        String keyId = (String) request.get("keyId");
        AiChatResponse response = aiAgentService.generateResume(userInput, provider, keyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze")
    public ResponseEntity<AiChatResponse> analyzeWithAi(
            @RequestBody AnalysisRequest request,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String keyId) {
        AiChatResponse response = aiAgentService.analyzeWithAi(
                request.getResumeText(), request.getJobDescription(), provider, keyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<AiChatResponse> searchWithAi(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        String provider = (String) request.get("provider");
        String keyId = (String) request.get("keyId");
        @SuppressWarnings("unchecked")
        List<String> matchedList = (List<String>) request.get("matchedSkills");
        @SuppressWarnings("unchecked")
        List<String> jobList = (List<String>) request.get("jobSkills");

        Set<String> matchedSkills = matchedList != null ? new HashSet<>(matchedList) : null;
        Set<String> jobSkills = jobList != null ? new HashSet<>(jobList) : null;

        AiChatResponse response = aiAgentService.searchWithAi(query, matchedSkills, jobSkills, provider, keyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rag/ingest")
    public ResponseEntity<Map<String, Object>> ingestDocument(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String content = (String) request.get("content");
            String source = (String) request.get("source");
            String category = (String) request.get("category");

            ragService.ingestDocument(content, source, category);

            response.put("success", true);
            response.put("message", "文档已成功导入知识库");
            response.put("knowledgeBaseSize", ragService.getKnowledgeBaseSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/rag/ingest-batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<String> contents = (List<String>) request.get("contents");
            String source = (String) request.get("source");
            String category = (String) request.get("category");

            ragService.ingestDocuments(contents, source, category);

            response.put("success", true);
            response.put("message", "批量文档已成功导入知识库");
            response.put("count", contents.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/rag/search")
    public ResponseEntity<List<SearchResult>> searchRag(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int maxResults = request.containsKey("maxResults") ?
                ((Number) request.get("maxResults")).intValue() : 5;
        double minScore = request.containsKey("minScore") ?
                ((Number) request.get("minScore")).doubleValue() : 0.5;

        List<SearchResult> results = ragService.search(query, maxResults, minScore);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/rag/clear")
    public ResponseEntity<Map<String, Object>> clearKnowledgeBase() {
        ragService.clearKnowledgeBase();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "知识库已清空");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weight/config")
    public ResponseEntity<WeightConfig> getWeightConfig() {
        return ResponseEntity.ok(weightedRetrievalService.getCurrentConfig());
    }

    @PutMapping("/weight/config")
    public ResponseEntity<WeightConfig> updateWeightConfig(@RequestBody Map<String, Double> dimensions) {
        WeightConfig config = weightedRetrievalService.updateWeights(dimensions);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/weight/config/{name}")
    public ResponseEntity<Map<String, Object>> saveWeightConfig(
            @PathVariable String name, @RequestBody WeightConfig config) {
        weightedRetrievalService.saveCustomConfig(name, config);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "权重配置已保存: " + name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weight/config/{name}")
    public ResponseEntity<WeightConfig> getCustomWeightConfig(@PathVariable String name) {
        WeightConfig config = weightedRetrievalService.getCustomConfig(name);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @GetMapping("/weight/configs")
    public ResponseEntity<Map<String, WeightConfig>> getAllCustomConfigs() {
        return ResponseEntity.ok(weightedRetrievalService.getAllCustomConfigs());
    }

    @DeleteMapping("/weight/config/{name}")
    public ResponseEntity<Map<String, Object>> deleteCustomWeightConfig(@PathVariable String name) {
        weightedRetrievalService.deleteCustomConfig(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "权重配置已删除: " + name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("service", "AI Agent Service");
        response.put("knowledgeBaseSize", ragService.getKnowledgeBaseSize());
        response.put("weightEnabled", weightedRetrievalService.isEnabled());
        response.put("weightDimensions", weightedRetrievalService.getDimensions());
        response.put("storedApiKeys", apiKeyService.listKeys().size());
        response.put("supportedProviders", apiKeyService.getSupportedProviders());
        return ResponseEntity.ok(response);
    }
}
