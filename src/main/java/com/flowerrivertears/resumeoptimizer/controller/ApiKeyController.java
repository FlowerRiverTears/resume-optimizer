package com.flowerrivertears.resumeoptimizer.controller;

import com.flowerrivertears.resumeoptimizer.model.*;
import com.flowerrivertears.resumeoptimizer.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai/keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidateResponse> validateKey(@RequestBody ApiKeyValidateRequest request) {
        ApiKeyValidateResponse response = apiKeyService.validateAndStoreKey(request);
        if (response.isValid()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ApiKeyInfo>> listKeys() {
        return ResponseEntity.ok(apiKeyService.listKeys());
    }

    @GetMapping("/{keyId}")
    public ResponseEntity<ApiKeyInfo> getKeyInfo(@PathVariable String keyId) {
        ApiKeyInfo info = apiKeyService.getKeyInfo(keyId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Map<String, Object>> removeKey(@PathVariable String keyId) {
        Map<String, Object> response = new LinkedHashMap<>();
        boolean removed = apiKeyService.removeKey(keyId);
        response.put("success", removed);
        response.put("message", removed ? "API Key 已删除: " + keyId : "未找到该 Key ID");
        return removed ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, String>>> getSupportedProviders() {
        return ResponseEntity.ok(apiKeyService.getSupportedProviders());
    }
}
