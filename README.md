# resume-optimizer

Spring Boot backend for resume analysis and optimization tool.

## Features

- Skill extraction from resumes and job descriptions
- Intelligent matching based on rule engine
- Multi-dimensional analysis (ATS score, structure, keywords)
- Personalized suggestions based on missing skills
- Template generation with comparison view
- Privacy-first: local processing, no external API calls

## Tech Stack

- Spring Boot 4.0
- Apache PDFBox
- Apache POI
- Maven

## Quick Start

### Requirements
- JDK 17+
- Maven 3.6+

### Build
```bash
./mvnw clean package
```

### Run
```bash
./mvnw spring-boot:run
```

Service starts at `http://localhost:9000`.

## API Endpoints

### Health Check
```
GET /api/health
```

### Parse Resume
```
POST /api/parse
Content-Type: multipart/form-data
file: resume file (PDF/DOCX/TXT)
```

### Analyze Resume
```
POST /api/analyze
Content-Type: application/json

{
  "resumeText": "resume content",
  "jobDescription": "job description"
}
```

Returns: atsScore, matchScore, foundKeywords, missingKeywords, categoryScores, skillGaps, suggestions, structure

### Generate Optimized Resume
```
POST /api/optimize
Content-Type: application/json

{
  "resumeText": "resume",
  "matchedSkills": ["git", "vue"],
  "missingSkills": ["react", "docker"],
  "skillGaps": [...]
}
```

### Get Template
```
GET /api/templates/{type}
type: default | tech | senior
```



