# Resume Optimizer - Project Documentation

**English** | **[中文](./README.md)**

---

> This document is the complete project documentation, including project background, tech stack, core features, quick start, configuration, and full API reference.
> For a brief version, see [Root README.md](../README.md)

## Project Overview

Resume Optimizer is an AI-powered intelligent resume analysis and optimization system, integrating LLM large models, RAG retrieval-augmented generation, and ATS scoring to help job seekers improve resume quality, match job requirements, and identify skill gaps.

The system uses a frontend-backend separation architecture. The backend is based on Spring Boot 4.0.5 + LangChain4j 1.0.1, and the frontend is based on Vue.js 3.5.30 + ECharts 5.5.0, supporting multiple LLM providers (OpenAI, DashScope, DeepSeek, MiniMax).

## Project Background

### Pain Points

Job seekers face the following challenges:

1. **Inconsistent Resume Quality** - Lack of professional resume writing guidance, difficulty highlighting personal strengths
2. **Low Job Match Rate** - Unaware of core skill requirements for target positions, resume disconnected from job
3. **Unclear Skill Gaps** - Don't know where the gaps are between themselves and target positions
4. **Vague Optimization Direction** - Lack of specific improvement suggestions and learning paths
5. **ATS System Filtering** - Resume format and keywords don't meet ATS system requirements, automatically filtered out

### Solution

This project provides the following core features through AI technology:

| Feature | Description |
|---------|-------------|
| Resume Parsing | Support PDF, Word, TXT format resume upload and parsing |
| Intelligent Analysis | Keyword and semantic analysis of resume content, skill extraction, structure analysis |
| ATS Scoring | LLM+RAG deep scoring, multi-dimensional analysis (job match/structure/content/keywords) |
| Job Matching | Analyze resume-job match rate, generate skill radar charts and bar charts |
| AI Chat | LLM-based intelligent Q&A and resume generation, with thinking process display |
| RAG Retrieval | Retrieval-augmented generation, hybrid search (vector + keyword) + weighted ranking |
| Skill Gap Analysis | Identify missing skills and provide learning suggestions and resource links |
| Optimization Suggestions | Priority-sorted targeted improvement suggestions (High/Medium/Low) |
| Template Comparison | Side-by-side comparison of original vs. optimized resume, with skill highlighting |
| PDF Export | Export analysis reports to PDF format |

## Tech Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Primary development language |
| Spring Boot | 4.0.5 | Backend framework |
| LangChain4j | 1.0.1 | LLM integration framework (OpenAI compatible protocol) |
| Apache PDFBox | 3.0.0 | PDF file parsing |
| Apache POI | 5.2.5 | Word file parsing |
| DJL | 0.31.1 | Local vector model inference (all-MiniLM-L6-v2) |
| Lombok | - | Code simplification (@Data, @Builder) |

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| Vue.js | 3.5.30 | Frontend framework (Composition API + `<script setup>`) |
| Vite | 7.3.1 | Build tool |
| ECharts | 5.5.0 | Data visualization (radar chart / bar chart) |
| vue-echarts | 7.0.0 | Vue ECharts integration |
| Custom Markdown Parser | - | AI response rendering (no third-party dependencies) |

### AI Model Support

| Provider | Model | Base URL | Purpose |
|----------|-------|----------|---------|
| OpenAI | GPT-4o-mini | https://api.openai.com/v1 | Primary chat model |
| Alibaba DashScope | Qwen-Plus | https://dashscope.aliyuncs.com/compatible-mode/v1 | Alternative domestic model |
| DeepSeek | DeepSeek-Chat | https://api.deepseek.com | Alternative domestic model (supports thinking process) |
| Minimax | MiniMax-M2.7 | https://api.minimaxi.com/v1 | Alternative domestic model |
| Local Model | all-MiniLM-L6-v2 | - | Vector embedding model (DJL inference) |

## Core Features

### 1. Resume Parsing & Upload

- Support PDF, DOCX, TXT formats
- Automatic text content extraction
- File size limit 10MB
- File type whitelist validation
- Three input methods: file upload (drag & drop), text paste, sample resume

### 2. ATS Comprehensive Scoring (LLM+RAG Deep Analysis)

ATS scoring is the core feature of this system, achieving dynamic analysis through LLM + RAG deep retrieval:

| Scoring Dimension | Weight | Description |
|-------------------|--------|-------------|
| Job Match | 40% | Skill-to-position match degree |
| Structure Score | 30% | Resume structure completeness (contact/summary/experience/education/skills) |
| Content Score | 30% | Content richness and keyword density |

**Scoring Grades:**

| Score | Grade | Description |
|-------|-------|-------------|
| 90-100 | Excellent | Excellent resume quality, highly matched with position |
| 75-89 | Good | Good resume quality, basically matches position requirements |
| 60-74 | Average | Average resume quality, some skills need supplementation |
| 40-59 | Needs Improvement | Resume needs significant improvement |
| 0-39 | Needs Rewrite | Resume has large gap from position requirements |

**Analysis Flow:**

```
User enters API Key → Upload resume → Click analyze
    │
    ├── Has Key → LLM + RAG Deep Analysis
    │   ├── RAG Retrieval: ingestResumeData → hybridSearch → rerank
    │   ├── Basic Analysis: ResumeAnalysisService.analyze (skill extraction/matching/gaps)
    │   ├── Build Prompt: resume + job + retrieval results + basic analysis
    │   ├── Call LLM → Return JSON scoring
    │   └── Parse JSON → Display results
    │
    └── No Key → Basic Analysis (auto-infer skills)
        ├── Identify skill keywords from resume text (100+ skill library)
        ├── Infer common missing skills (based on skill categories)
        └── Calculate match rate and scoring
```

### 3. AI Agent Chat

- Intelligent Q&A based on resume content (auto-inject resume context)
- Resume generation and optimization suggestions (intelligently identify "generate resume" requests)
- Skill gap analysis and learning path recommendations
- Thinking process separation display (DeepSeek model's `<think/>` tags)
- RAG retrieval results display (source/category/relevance score)
- Quick question buttons (analyze pros/cons, generate resume, job matching, etc.)
- Multi-model selection (switch via saved API Keys)

### 4. RAG Retrieval Augmentation

- **Hybrid Search**: Vector search (semantic similarity) + Keyword search (TF-IDF)
- **Reranking**: Combined vector score (60%) + keyword score (25%) + content relevance (15%)
- **Semantic Chunking**: Semantic chunking by paragraphs, with overlap support
- **Chinese Tokenization**: Bigram tokenization + stopword filtering
- **Four-dimension Weight Configuration**: Skill match (35%) + Semantic similarity (30%) + Category relevance (20%) + Experience level (15%)

### 5. API Key Management

- In-memory secure storage (ConcurrentHashMap), not persisted
- Support 4 provider validations (OpenAI/DashScope/DeepSeek/MiniMax)
- Frontend real-time linkage (Key validation automatically enables ATS scoring and AI chat)
- Advanced settings (custom Base URL and model name)
- Key masking display (show first 4 and last 4 characters only)

### 6. Template Comparison

- Side-by-side comparison view (original resume vs. optimized resume)
- Skill highlighting (matched in green / missing in red strikethrough)
- One-click apply optimized template
- Copy original resume to clipboard

## Quick Start

### Requirements

- JDK 17+
- Node.js 20.19+ or 22.12+
- Maven 3.6+

### Start Backend

```bash
cd resume-optimizer

# Configure environment variables (optional, can also enter API Key in frontend)
export OPENAI_API_KEY=your-api-key

# Start service
./mvnw spring-boot:run
```

Backend service runs at `http://localhost:9000`

### Start Frontend

```bash
cd resume-optimizer-frontend

# Install dependencies
npm install

# Start in development mode
npm run dev
```

Frontend service runs at `http://localhost:5173`

### Usage Flow

1. Open frontend page at `http://localhost:5173`
2. Enter and validate API Key in the "AI Model Configuration" panel
3. Upload resume file (PDF/DOCX/TXT) or paste text
4. Edit resume content, optionally enter target job description
5. Click "Start Analysis" to view ATS scoring report
6. View skill radar chart, keyword details, optimization suggestions
7. Use "Template Comparison" to view optimized resume
8. Switch to "AI Assistant" for intelligent conversation

## Configuration

### LLM Model Configuration

Configure in `application.yaml`:

```yaml
ai:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 4096
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-plus
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.com/v1
      model-name: MiniMax-M2.7
```

### RAG Configuration

```yaml
ai:
  rag:
    embedding:
      provider: local
    vector-store:
      type: in-memory
      max-results: 5
      min-score: 0.5
```

### Weight Configuration

```yaml
ai:
  weight:
    enabled: true
    dimensions:
      skill-match: 0.35
      semantic-similarity: 0.30
      category-relevance: 0.20
      experience-level: 0.15
```

## API Reference

### Resume Parsing

```
POST /api/parse
Content-Type: multipart/form-data

Parameter: file (PDF/DOCX/TXT, max 10MB)
Response: { content: "parsed text content" }
```

### Resume Analysis

```
POST /api/analyze
Content-Type: application/json

Parameters: { resumeText, jobDescription }
Response: {
  foundKeywords, missingKeywords, skillGaps,
  categoryScores, structureAnalysis, suggestions
}
```

### ATS Comprehensive Scoring

```
POST /api/ats-score
Content-Type: application/json

Parameters: { resumeText, jobDescription, provider, keyId }
Response: {
  overallScore, grade, gradeDescription,
  jobMatchScore, structureScore, contentScore, keywordScore,
  matchedSkills, missingSkills, matchedCount, missingCount,
  categoryDetails, skillGapDetails, optimizationSuggestions,
  responseTimeMs, provider, keyId
}
```

### AI Chat

```
POST /api/ai/chat
Parameters: { message, provider, keyId, context }
Response: { answer, thinking, searchResults, provider, model, responseTimeMs }
```

### AI Resume Generation

```
POST /api/ai/generate-resume
Parameters: { userInput, provider, keyId }
Response: { answer, searchResults, provider, model, responseTimeMs }
```

### AI Deep Analysis

```
POST /api/ai/analyze?keyId=xxx
Parameters: { resumeText, jobDescription }
Response: { answer, thinking, searchResults, provider, model, responseTimeMs }
```

### RAG Knowledge Base Management

```
POST   /api/ai/rag/ingest       # Single document ingestion
POST   /api/ai/rag/ingest-batch # Batch document ingestion
POST   /api/ai/rag/search       # Knowledge base search
DELETE /api/ai/rag/clear        # Clear knowledge base
```

### Weight Configuration Management

```
GET    /api/ai/weight/config            # Get current weight configuration
PUT    /api/ai/weight/config            # Update weight configuration
POST   /api/ai/weight/config/{name}     # Save custom weight configuration
GET    /api/ai/weight/config/{name}     # Get custom weight configuration
DELETE /api/ai/weight/config/{name}     # Delete custom weight configuration
```

### API Key Management

```
POST   /api/ai/keys/validate    # Validate API Key
GET    /api/ai/keys/list        # List stored Keys
GET    /api/ai/keys/{keyId}     # Get specified Key info
DELETE /api/ai/keys/{keyId}     # Delete specified Key
GET    /api/ai/keys/providers   # Get supported provider list
```

### Resume Optimization & Templates

```
POST /api/optimize              # Generate optimized resume and comparison view
GET  /api/templates/{type}      # Get resume template (default/tech/senior)
```

### Health Check

```
GET /api/health                 # Resume service health check
GET /api/ai/health              # AI service health check
```

## Documentation Index

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System architecture design |
| [FRONTEND.md](./FRONTEND.md) | Frontend development specifications |
| [SECURITY.md](./SECURITY.md) | Security specifications |
| [STYLE.md](./STYLE.md) | Code style guide |
| [GUIDE.md](./GUIDE.md) | Documentation design principles |

## License

This project is for learning and research purposes only.

---

**Note**: This project uses AI technology to process user data. All data is processed locally to protect user privacy. API Keys are stored in memory only and cleared on application restart.
