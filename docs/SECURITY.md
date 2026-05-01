# 安全规范

## 概述

本文档定义了简历优化器项目的安全规范，涵盖 API Key 管理、数据保护、输入验证、跨域安全等方面。

## API Key 安全

### 存储策略

API Key 采用**内存存储**策略，不持久化到数据库或文件系统：

```java
@Service
public class ApiKeyService {
    private final Map<String, ApiKeyInfo> keyStore = new ConcurrentHashMap<>();
    
    public String storeKey(String provider, String apiKey, String baseUrl, String modelName) {
        String keyId = UUID.randomUUID().toString();
        keyStore.put(keyId, ApiKeyInfo.builder()
            .keyId(keyId)
            .provider(provider)
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .createdAt(LocalDateTime.now())
            .build());
        return keyId;
    }
}
```

### 安全原则

| 原则 | 描述 | 实现方式 |
|-----|------|---------|
| 不持久化 | API Key 仅存储在内存中，不写入数据库或文件 | ConcurrentHashMap，重启清空 |
| 不记录日志 | API Key 不出现在日志输出中 | 日志中使用 maskKey() 脱敏 |
| 不传输给前端 | 返回给前端的只有 keyId，不包含实际 Key | ApiKeyInfo 返回时过滤 apiKey 字段 |
| 验证后存储 | 只有验证通过的 Key 才会存储 | 发送测试请求验证有效性 |

### 验证机制

```java
public ApiKeyValidateResponse validateKey(ApiKeyValidateRequest request) {
    try {
        ChatModel testModel = createChatModelWithKey(
            request.getApiKey(),
            request.getProvider(),
            request.getBaseUrl(),
            request.getModelName()
        );
        
        String response = testModel.chat("Hi");
        
        return ApiKeyValidateResponse.builder()
            .valid(true)
            .provider(request.getProvider())
            .build();
    } catch (Exception e) {
        log.warn("API Key validation failed: {}", e.getMessage());
        return ApiKeyValidateResponse.builder()
            .valid(false)
            .message(extractErrorMessage(e))
            .build();
    }
}
```

### 提供商归一化

```java
private String normalizeProvider(String provider) {
    if (provider == null) return "openai";
    String lower = provider.toLowerCase();
    if (lower.contains("qwen") || lower.contains("dashscope")) return "dashscope";
    if (lower.contains("deepseek")) return "deepseek";
    if (lower.contains("minimax") || lower.contains("minimaxi")) return "minimax";
    return lower;
}
```

### 错误信息提取

```java
private String extractErrorMessage(Exception e) {
    String msg = e.getMessage();
    if (msg.contains("401")) return "API Key 无效或已过期";
    if (msg.contains("403")) return "没有访问权限";
    if (msg.contains("404")) return "模型不存在或API地址错误";
    if (msg.contains("429")) return "请求过于频繁，请稍后重试";
    if (msg.contains("500")) return "AI 服务内部错误";
    if (msg.contains("ConnectException")) return "网络连接失败，请检查API地址";
    return "API Key 验证失败：" + msg;
}
```

## 数据保护

### 数据处理原则

1. **本地处理** - 所有简历数据仅在本地服务器处理，不上传到第三方
2. **不存储简历** - 简历内容不持久化存储，仅在会话期间保留在内存
3. **即时清理** - 处理完成后及时清理临时数据
4. **最小化传输** - AI 对话只传输简历摘要（前500字），不传输全文

### 敏感信息处理

```java
log.info("Processing resume for user: {}", maskEmail(userEmail));

private String maskEmail(String email) {
    if (email == null || !email.contains("@")) return "***";
    String[] parts = email.split("@");
    return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
}

private String maskKey(String apiKey) {
    if (apiKey == null || apiKey.length() <= 8) return "***";
    return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
}
```

### 文件上传安全

```java
public class FileParser {
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    public static String parse(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        
        return doParse(file);
    }
}
```

## 输入验证

### 参数校验

```java
@RestController
public class AiAgentController {
    
    @PostMapping("/api/ai/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        
        if (request.getMessage().length() > 10000) {
            throw new IllegalArgumentException("消息长度超过限制");
        }
        
        if (request.getProvider() != null) {
            Set<String> validProviders = Set.of("openai", "dashscope", "deepseek", "minimax");
            if (!validProviders.contains(request.getProvider().toLowerCase())) {
                throw new IllegalArgumentException("无效的提供商");
            }
        }
        
        return aiAgentService.chat(request);
    }
}
```

### XSS 防护

前端使用自定义 Markdown 解析器，自动转义 HTML：

```javascript
function escapeHtml(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
}
```

**防护层级：**

| 层级 | 措施 | 描述 |
|-----|------|-----|
| 解析器 | escapeHtml() | 所有文本内容经过 HTML 转义 |
| 代码块 | escapeHtml(text) | 代码块内容单独转义 |
| 链接 | escapeHtml(url) | URL 参数转义，防止 javascript: 协议 |
| 图片 | escapeHtml(src) | 图片源地址转义 |
| 行内代码 | 先提取保护 | 行内代码先提取，避免被其他规则影响 |

## 跨域安全 (CORS)

### CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

### 生产环境建议

```java
.allowedOrigins("https://your-domain.com")

@Value("${app.cors.allowed-origins}")
private String[] allowedOrigins;
```

## 请求限流

### 实现方案

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        String clientId = getClientId(request);
        RateLimiter limiter = limiters.computeIfAbsent(clientId, 
            k -> RateLimiter.create(10.0));
        
        if (!limiter.tryAcquire()) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁，请稍后重试");
            return false;
        }
        
        return true;
    }
}
```

## 错误处理

### 统一异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
            .body(Map.of("error", "服务器内部错误"));
    }
}
```

### 敏感信息屏蔽

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
    String message = e instanceof BusinessException 
        ? e.getMessage() 
        : "服务器内部错误";
    
    log.error("Error occurred", e);
    
    return ResponseEntity.internalServerError()
        .body(Map.of("error", message));
}
```

## 安全检查清单

### 开发阶段

- [x] API Key 不记录到日志（使用 maskKey 脱敏）
- [x] 敏感数据不返回给前端（只返回 keyId）
- [x] 输入参数进行校验（非空/长度/提供商白名单）
- [x] 文件上传进行类型和大小检查
- [x] Markdown 解析器内置 XSS 防护
- [x] 链接添加 target="_blank" rel="noopener"

### 部署阶段

- [ ] CORS 配置为生产域名
- [ ] 启用 HTTPS
- [ ] 配置请求限流
- [ ] 设置安全响应头
- [ ] 环境变量管理敏感配置

### 运维阶段

- [ ] 定期更新依赖版本
- [ ] 监控异常请求
- [ ] 定期审计日志
- [ ] 制定应急响应计划

## 安全响应头

```java
@Component
public class SecurityHeaderFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, 
                        ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");
        
        chain.doFilter(request, response);
    }
}
```

## 环境变量管理

### 敏感配置

```yaml
ai:
  llm:
    openai:
      api-key: ${OPENAI_API_KEY:}
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:}
    minimax:
      api-key: ${MINIMAX_API_KEY:}
```

### 部署配置

```bash
export OPENAI_API_KEY=sk-xxx
export DASHSCOPE_API_KEY=sk-xxx
export DEEPSEEK_API_KEY=sk-xxx
export MINIMAX_API_KEY=sk-xxx

java -jar resume-optimizer.jar
```

---

**重要提示**: 安全是一个持续的过程，需要定期审查和更新安全措施。
