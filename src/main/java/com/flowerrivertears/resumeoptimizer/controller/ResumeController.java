package com.flowerrivertears.resumeoptimizer.controller;

import com.flowerrivertears.resumeoptimizer.model.AnalysisRequest;
import com.flowerrivertears.resumeoptimizer.model.AnalysisResponse;
import com.flowerrivertears.resumeoptimizer.service.ResumeAnalysisService;
import com.flowerrivertears.resumeoptimizer.util.FileParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResumeController {

    @Autowired
    private ResumeAnalysisService analysisService;

    @Autowired
    private FileParser fileParser;

    /**
     * 简历文件上传解析
     */
    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            String content = fileParser.parseFile(file);
            response.put("success", true);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 简历文本分析
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalysisRequest request) {
        AnalysisResponse result = analysisService.analyze(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Resume Optimizer API is running");
        return ResponseEntity.ok(response);
    }
}
