# Chat with Docs - RAG Application

A production-ready **Retrieval Augmented Generation (RAG)** system built with Spring AI, demonstrating enterprise-grade document processing and semantic question-answering capabilities.

## Overview

This application implements a complete RAG pipeline for document-based question answering. It leverages vector embeddings and semantic search to provide accurate, context-aware responses from uploaded documents.

**Purpose**: Educational project demonstrating Spring AI framework capabilities, RAG architecture patterns, and vector database integration.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Deployment](#deployment)
- [AI Provider Configuration](#ai-provider-configuration)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Features

- **Multi-Document Support**: Upload and process multiple PDF documents simultaneously
- **Semantic Search**: Vector-based similarity search for accurate information retrieval
- **Contextual Answers**: AI-generated responses with source attribution
- **Provider Agnostic**: Abstraction layer supports multiple AI providers (Ollama, OpenAI, Azure, etc.)
- **Local-First**: Default configuration runs entirely locally without external API dependencies
- **RESTful API**: Clean, well-documented REST endpoints
- **Scalable Architecture**: Designed for production deployment
- **Extensible**: Plugin architecture for custom document processors and AI models

## Technology Stack

### Core Framework
- **Spring Boot** 3.5.7 - Application framework
- **Spring AI** - AI integration framework
- **Java** 25 - Programming language
- **Maven** - Build and dependency management

### AI Components
- **Ollama** - Default LLM provider (llama3.2:1b for chat, nomic-embed-text for embeddings)
- **Spring AI Abstractions** - Provider-agnostic interfaces (ChatModel, EmbeddingModel)

**Note**: While this implementation uses Ollama for local execution, the application architecture supports seamless integration with commercial providers including OpenAI, Azure OpenAI, Anthropic Claude, Google Vertex AI, and AWS Bedrock through configuration changes only.

### Data Layer
- **PostgreSQL** 18 - Primary database
- **PGVector** - Vector similarity search extension
- **HikariCP** - Connection pooling

### Document Processing
- **Apache Tika** - Multi-format document parsing
- **Apache PDFBox** - PDF text extraction
- **Spring AI Document Readers** - Unified document abstraction

### Development Tools
- **Lombok** - Boilerplate reduction
- **SLF4J/Logback** - Logging framework
- **Spring Boot DevTools** - Development utilities
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **TestContainers** - Integration testing with Docker

## Architecture

### RAG Pipeline

**Document Ingestion**
```
PDF Upload → Text Extraction → Chunking → Embedding → Vector Storage
```

**Query Processing**
```
User Query → Embedding → Vector Search → Context Building → LLM → Response
```

### Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| `DocumentController` | HTTP endpoint handling for document uploads |
| `ChatController` | HTTP endpoint handling for query requests |
| `DocumentService` | Business logic for document processing |
| `ChatService` | RAG query orchestration and response generation |
| `VectorStore` | Vector database operations (PGVector) |
| `ChatModel` | LLM integration (provider-agnostic) |
| `EmbeddingModel` | Text embedding generation (provider-agnostic) |

### Project Structure

```
chat-with-your-docs/
├── .github/
│   └── workflows/
│       └── ci.yml                           # CI/CD pipeline
├── .mvn/                                      # Maven wrapper
├── src/
│   ├── main/
│   │   ├── java/io/github/kxng0109/chatwithdocs/
│   │   │   ├── ChatWithDocsApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── DocumentController.java
│   │   │   │   └── TestController.java
│   │   │   ├── service/
│   │   │   │   ├── ChatService.java
│   │   │   │   └── DocumentService.java
│   │   │   ├── model/
│   │   │   │   ├── ChatRequest.java
│   │   │   │   ├── ChatResponse.java
│   │   │   │   └── DocumentUploadResponse.java
│   │   │   └── exception/
│   │   │       ├── DocumentProcessingException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/io/github/kxng0109/chatwithdocs/
│       │   ├── ChatWithDocsApplicationIntegrationTest.java
│       │   ├── controller/
│       │   │   ├── ChatControllerTest.java
│       │   │   └── DocumentControllerTest.java
│       │   └── service/
│       │       ├── ChatServiceTest.java
│       │       └── DocumentServiceTest.java
│       └── resources/
│           └── application-test.properties
├── .env.example                               # Environment variables template
├── .gitignore                                 # Git ignore rules
├── mvnw                                       # Maven wrapper (Linux/macOS)
├── mvnw.cmd                                   # Maven wrapper (Windows)
├── pom.xml                                    # Maven dependencies
├── LICENSE                                    # MIT License
└── README.md                                  # This file
```

## Prerequisites

### Required Software

- **JDK**: Version 21 or higher
  ```bash
  java -version
  ```

- **Maven**:
  ```bash
  mvn -version
  ```

- **Docker**: For PostgreSQL deployment
  ```bash
  docker --version
  docker ps
  ```

- **Ollama**: For local LLM execution (default configuration)
  ```bash
  # Download from https://ollama.com/download
  ollama --version
  ```

### System Requirements

- **Memory**: Minimum 8GB RAM (16GB recommended for optimal performance)
- **Disk**: 10GB free space (for models and vector storage)
- **Network**: Internet connection for initial model downloads

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/kxng0109/chat-with-your-docs.git
cd chat-with-your-docs
```

### 2. Environment Configuration

Create your environment file from the template:

**Linux/macOS:**
```bash
cp .env.example .env
```

**Windows (Command Prompt):**
```cmd
copy .env.example .env
```

**Windows (PowerShell):**
```powershell
Copy-Item .env.example .env
```

Edit `.env` and configure your settings. The `.env.example` file contains all available configuration options with detailed comments. At minimum, update:

```properties
# Recommended: Use a strong password
POSTGRES_PASSWORD=your_secure_password_here

# Only if using cloud providers instead of Ollama
# OPENAI_API_KEY=your_api_key_here
```

**Security Warning**: Never commit the `.env` file. It contains sensitive credentials and is already included in `.gitignore`.

### 3. Database Setup

Start PostgreSQL with PGVector extension:

```bash
docker run -d \
  --name postgres-vectordb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vectordb \
  -p 5432:5432 \
  --restart unless-stopped \
  pgvector/pgvector:pg18
```

**Windows (Command Prompt):**
```cmd
docker run -d ^
  --name postgres-vectordb ^
  -e POSTGRES_USER=postgres ^
  -e POSTGRES_PASSWORD=postgres ^
  -e POSTGRES_DB=vectordb ^
  -p 5432:5432 ^
  --restart unless-stopped ^
  pgvector/pgvector:pg18
```

**Windows (PowerShell):**
```powershell
docker run -d `
  --name postgres-vectordb `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=vectordb `
  -p 5432:5432 `
  --restart unless-stopped `
  pgvector/pgvector:pg18
```

**Verify container status:**

```bash
docker ps
```

Look for `postgres-vectordb` in the output.

### 4. AI Model Setup

Pull required Ollama models:

```bash
# Chat model (1B parameters, ~1.3GB)
ollama pull llama3.2:1b

# Embedding model (~300MB)
ollama pull nomic-embed-text
```

Verify installation:
```bash
ollama list
```

Expected output:
```
NAME                    ID              SIZE
llama3.2:1b                            1.3 GB
nomic-embed-text                       274 MB
```

### 5. Application Build

```bash
./mvnw clean install
```

**Windows (PowerShell):**
```powershell
.\mvnw.cmd clean install
```

**Windows (Command Prompt):**
```cmd
mvnw.cmd clean install
```

### 6. Application Startup

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Windows (Command Prompt):**
```cmd
mvnw.cmd spring-boot:run
```

Application will be available at: `http://localhost:8080`

Verify startup in logs:
```
Started ChatWithDocsApplication in X.XXX seconds
```

## Usage

### Document Upload

Upload single or multiple PDF documents for processing.

**Single Document Upload (cURL)**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/document.pdf"
```

**Using cURL (Windows - Command Prompt):**
```cmd
curl -X POST http://localhost:8080/api/documents/upload ^
  -H "Content-Type: multipart/form-data" ^
  -F "file=@C:\path\to\document.pdf"
```

**Response Format**
```json
{
  "filename": "technical_specification.pdf",
  "chunksCreated": 42,
  "chunksStored": 42,
  "message": "Document processed successfully",
  "processingTimeMs": 3847
}
```

**Processing Details**:
- Documents are split into ~300-token chunks with 50-token overlap
- Each chunk generates a 768-dimensional embedding vector
- Chunks are stored with metadata (filename, timestamp, index) in PostgreSQL
- All documents share a unified vector space for cross-document querying

### Question Answering

Query uploaded documents using natural language.

**Basic Query (cURL)**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the key technical specifications?",
    "topK": 5
  }'
```

**Using cURL (Windows - Command Prompt):**
```cmd
curl -X POST http://localhost:8080/api/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"question\": \"What are the key technical specifications?\", \"topK\": 5}"
```

**Advanced Query with Custom Parameters**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Compare performance metrics across all documents",
    "topK": 10
  }'
```

**Response Format**
```json
{
  "answer": "Based on the technical specifications, the key metrics include...",
  "sources": [
    "Chunk from document A containing relevant specification...",
    "Chunk from document B with performance data...",
    "Chunk from document C discussing metrics..."
  ],
  "question": "What are the key technical specifications?",
  "processingTimeMs": 1842
}
```

**Query Parameters**:
- `question` (required, string): Natural language query
- `topK` (optional, integer, default: 5): Number of relevant chunks to retrieve

**Cross-Document Querying**:
When multiple documents are uploaded, the system:
1. Searches across all documents simultaneously
2. Ranks chunks by semantic similarity regardless of source
3. Synthesizes information from multiple documents
4. Provides unified answers with source attribution

## API Reference

### Document Management

#### Upload Document

**Endpoint**: `POST /api/documents/upload`

**Request**:
- Method: `POST`
- Content-Type: `multipart/form-data`
- Body Parameter: `file` (PDF document)

**Response**: `200 OK`
```json
{
  "filename": "string",
  "chunksCreated": "integer",
  "chunksStored": "integer",
  "message": "string",
  "processingTimeMs": "long"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid file format or missing file
- `413 Payload Too Large`: File exceeds 10MB limit
- `500 Internal Server Error`: Processing failure

### Chat Interface

#### Ask Question

**Endpoint**: `POST /api/chat`

**Request**:
- Method: `POST`
- Content-Type: `application/json`
- Body:
```json
{
  "question": "string (required)",
  "topK": "integer (optional, default: 5)"
}
```

**Response**: `200 OK`
```json
{
  "answer": "string",
  "sources": ["string"],
  "question": "string",
  "processingTimeMs": "long"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid request format or empty question
- `500 Internal Server Error`: Query processing failure


## Testing

### Test Structure

The project includes comprehensive test coverage across multiple layers. See the project structure section above for test file locations.

**Test Files:**
- `DocumentControllerTest.java` - REST endpoint tests for document upload
- `ChatControllerTest.java` - REST endpoint tests for chat queries
- `DocumentServiceTest.java` - Business logic tests for document processing
- `ChatServiceTest.java` - RAG pipeline tests
- `ChatWithDocsApplicationIntegrationTest.java` - End-to-end integration tests

### Running Tests

#### Execute All Tests

```bash
./mvnw test
```

#### Run Specific Test Class

```bash
./mvnw test -Dtest=DocumentServiceTest
```

## Deployment

### Docker Deployment

#### Build Docker Image

```bash
docker build -t chat-with-docs:latest .
```

#### Run with Docker Compose

```bash
docker-compose up -d
```

**Verify services:**
```bash
docker-compose ps
```

**View logs:**
```bash
docker-compose logs -f
```

**Stop services:**
```bash
docker-compose down
```

## AI Provider Configuration

This application uses Spring AI's abstraction layer, enabling seamless integration with multiple AI providers without code changes.

### Current Implementation: Ollama

**Configuration**: See `application.properties` and `.env.example` for Ollama settings.

**Advantages**:
- Completely local execution
- No API costs
- Data privacy (no external transmission)
- Offline capability

### Alternative Provider: OpenAI
**Dependency Change**: Update `pom.xml` to replace ollama with openai.

**Configuration**: Set `OPENAI_API_KEY` and other relevant environment variables in `.env` file.

**No Code Changes Required**: Spring AI abstractions (`ChatModel`, `EmbeddingModel`) remain identical.

### Alternative Provider: Azure OpenAI

**Dependency Change**: Update `pom.xml` to use azure.

**Configuration**: Set Azure-specific environment variables in `.env` file.

### Provider Selection Criteria

**Choose Ollama if**:
- Running locally or on-premises
- Data privacy is critical
- Zero API costs required
- Offline operation needed

**Choose Commercial Provider if**:
- Highest quality responses required
- Scalability beyond local resources
- Enterprise support needed
- Advanced model capabilities required (GPT-4, Claude 3, etc.)

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) file for full terms.

## Acknowledgments

### Technologies
- **Spring AI Team** - Excellent abstraction layer for AI integration
- **Ollama Project** - Making local LLM execution accessible
- **PGVector Team** - Efficient vector similarity search for PostgreSQL
- **Apache Software Foundation** - Tika and PDFBox document processing libraries

### Resources
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama Documentation](https://github.com/ollama/ollama)
- [PGVector Documentation](https://github.com/pgvector/pgvector)

### Community
- Open-source contributors
- Spring AI community for feedback and improvements
- RAG research community for architectural patterns

---