# Resume Optimizer

**English** | **[中文](./README.md)**

---

An AI-powered intelligent resume analysis and optimization system, integrating LLM large models, RAG retrieval-augmented generation, and ATS scoring to help job seekers improve resume quality, match job requirements, and identify skill gaps.

The system uses a frontend-backend separation architecture. The backend is based on Spring Boot 4.0.5 + LangChain4j 1.0.1, and the frontend is based on Vue.js 3.5.30 + ECharts 5.5.0, supporting multiple LLM providers (OpenAI, DashScope, DeepSeek, MiniMax).

## Core Features

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

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| Vue.js | 3.5.30 | Frontend framework (Composition API + `<script setup>`) |
| Vite | 7.3.1 | Build tool |
| ECharts | 5.5.0 | Data visualization (radar chart / bar chart) |

### AI Model Support

| Provider | Model | Purpose |
|----------|-------|---------|
| OpenAI | GPT-4o-mini | Primary chat model |
| Alibaba DashScope | Qwen-Plus | Alternative domestic model |
| DeepSeek | DeepSeek-Chat | Alternative domestic model (supports thinking process) |
| Minimax | MiniMax-M2.7 | Alternative domestic model |
| Local Model | all-MiniLM-L6-v2 | Vector embedding model (DJL inference) |

## Quick Start

### Requirements

- JDK 17+
- Node.js 20.19+ or 22.12+
- Maven 3.6+

### Start Backend

```bash
cd resume-optimizer
./mvnw spring-boot:run
```

Backend service runs at `http://localhost:9000`

### Start Frontend

```bash
cd resume-optimizer-frontend
npm install
npm run dev
```

Frontend service runs at `http://localhost:5173`

### Usage Flow

1. Open frontend page at `http://localhost:5173`
2. Enter and validate API Key in the "AI Model Configuration" panel
3. Upload resume file (PDF/DOCX/TXT) or paste text
4. Click "Start Analysis" to view ATS scoring report
5. View skill radar chart, keyword details, and optimization suggestions
6. Switch to "AI Assistant" for intelligent conversation

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/parse` | POST | Resume file parsing |
| `/api/analyze` | POST | Resume text analysis |
| `/api/ats-score` | POST | ATS comprehensive scoring (LLM+RAG) |
| `/api/optimize` | POST | Generate optimized resume |
| `/api/ai/chat` | POST | AI intelligent chat |
| `/api/ai/generate-resume` | POST | AI resume generation |
| `/api/ai/analyze` | POST | AI deep analysis |
| `/api/ai/rag/ingest` | POST | RAG document ingestion |
| `/api/ai/rag/search` | POST | RAG knowledge base search |
| `/api/ai/keys/validate` | POST | API Key validation |
| `/api/health` | GET | Health check |

For detailed API documentation, see [docs/README.en.md](./docs/README.en.md)

## Project Structure

```
resume-optimizer/                    # Backend project
├── src/main/java/.../resumeoptimizer/
│   ├── config/                      # Configuration (CORS/LLM/Vector Store)
│   ├── controller/                  # Controllers (AI/Key/Resume)
│   ├── model/                       # Data models
│   ├── service/                     # Services (AI/RAG/ATS/Weight/Analysis/Template)
│   └── util/                        # Utilities (File parsing)
├── docs/                            # Project documentation
└── pom.xml                          # Maven configuration

resume-optimizer-frontend/           # Frontend project
├── src/
│   ├── components/                  # Vue components (7)
│   ├── api.js                       # API interface definitions
│   ├── markdown.js                  # Markdown parser
│   └── App.vue                      # Root component
└── vite.config.js                   # Vite configuration
```

## Documentation Index

| Document | Description |
|----------|-------------|
| [docs/README.en.md](./docs/README.en.md) | Project background and complete API documentation |
| [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) | System architecture design |
| [docs/FRONTEND.md](./docs/FRONTEND.md) | Frontend development specifications |
| [docs/SECURITY.md](./docs/SECURITY.md) | Security specifications |
| [docs/STYLE.md](./docs/STYLE.md) | Code style guide |
| [docs/GUIDE.md](./docs/GUIDE.md) | Documentation design principles |

## Privacy Statement

- All resume data is processed locally only, not uploaded to third parties
- API Keys are stored in memory only and cleared on application restart
- Resume content is not persisted

## License

This project is for learning and research purposes only.
