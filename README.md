# Chat with Docs - RAG Application

A production ready Retrieval Augmented Generation (RAG) system built with Spring AI. This application enables users to upload multiple documents, organize them into isolated chat sessions, and have conversations with any LLM provider based on the uploaded content.

## Overview

This application implements a complete RAG pipeline for document based question answering. It leverages vector embeddings and semantic search to provide accurate, context aware responses from uploaded documents.

Key capabilities include session based document isolation, multi document uploads, conversation history, and support for multiple AI providers including local (Ollama) and cloud (OpenAI) options.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Deployment](#deployment)
- [License](#license)

## Features

### Session Based Document Management
Each chat session acts as an isolated container for documents. Documents uploaded to one session are not visible or searchable from other sessions. This enables multiple independent research contexts within the same application.

### Multi Document Support
Upload and process multiple documents simultaneously. The system handles each file independently, providing detailed status for successful and failed uploads. All documents in a session contribute to the knowledge base for that session.

### Conversation History
The system maintains conversation history within each session, enabling context aware follow up questions. Previous exchanges are included in the prompt to provide continuity across messages.

### Provider Agnostic AI Integration
Switch between AI providers without code changes. The application supports Ollama for local execution and OpenAI for cloud based processing. Additional providers can be added through Spring AI abstractions.

### Cross Document Querying
When asking questions, the system searches across all documents in the session and synthesizes information from multiple sources. Responses include source attribution showing which documents contributed to the answer.

## Technology Stack

### Core Framework
- Spring Boot 3.5.7
- Spring AI 1.0.3
- Java 25
- Maven

### AI Components
- Ollama (default): llama3.2:1b for chat, nomic-embed-text for embeddings
- OpenAI (optional): gpt-4o-mini for chat, text-embedding-3-small for embeddings
- Spring AI Abstractions: ChatModel, EmbeddingModel, VectorStore

### Data Layer
- PostgreSQL 18 with PGVector extension
- Spring Data JPA
- HikariCP connection pooling

### Document Processing
- Apache Tika for multi format document parsing
- Apache PDFBox for PDF text extraction
- Spring AI Document Readers and Text Splitters

## Architecture

### RAG Pipeline

Document Ingestion Flow:
```
PDF Upload -> Text Extraction -> Chunking -> Embedding -> Vector Storage
```

Query Processing Flow:
```
User Query -> Session Filter -> Vector Search -> Context Building -> LLM -> Response
```

### Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| SessionController | HTTP endpoints for session management |
| DocumentController | HTTP endpoints for document uploads |
| ChatController | HTTP endpoints for chat queries |
| SessionService | Session lifecycle and vector cleanup |
| DocumentService | Document processing and embedding storage |
| ChatService | RAG orchestration with session filtering |
| VectorStore | PGVector operations with metadata filtering |
| ChatModel | LLM integration (provider agnostic) |
| EmbeddingModel | Text embedding generation |

### Session Isolation

Sessions provide logical isolation through metadata filtering. Each document chunk stored in the vector database includes a sessionId in its metadata.  When querying, the system filters results to only include chunks from the requested session.

```
Session A: [doc1.pdf, doc2.pdf] -> Vectors with sessionId="A"
Session B: [doc3.pdf, doc4.pdf] -> Vectors with sessionId="B"

Query in Session A -> Only searches vectors where sessionId="A"
```

## Prerequisites

### Required Software

Java Development Kit version 21 or higher:
```bash
java -version
```

Maven (or use the included wrapper):
```bash
mvn -version
```

Docker for PostgreSQL deployment:
```bash
docker --version
```

Ollama for local LLM execution (if using Ollama provider):
```bash
ollama --version
```

### System Requirements

- Memory: Minimum 8GB RAM (16GB recommended for Ollama)
- Disk: 10GB free space for models and vector storage
- Network: Internet connection for initial setup and OpenAI provider

## Installation

### Clone Repository

```bash
git clone https://github.com/kxng0109/chat-with-your-docs.git
cd chat-with-your-docs
```

### Environment Configuration

Create your environment file from the template.

Linux and macOS:
```bash
cp .env.example .env
```

Windows Command Prompt:
```cmd
copy .env.example .env
```

Windows PowerShell:
```powershell
Copy-Item .env.example .env
```

Edit the . env file and configure your settings:
```properties
POSTGRES_PASSWORD=your_secure_password_here

# For OpenAI provider
OPENAI_API_KEY=sk-your-api-key-here
```

### Database Setup

Start PostgreSQL with PGVector extension:

Linux and macOS:
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

Windows Command Prompt:
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

Verify the container is running:
```bash
docker ps
```

### AI Model Setup

For Ollama (default local provider):
```bash
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

Verify installation:
```bash
ollama list
```

For OpenAI, ensure your API key is configured in the .env file.

### Build Application

Linux and macOS:
```bash
./mvnw clean install
```

Windows:
```cmd
mvnw. cmd clean install
```

### Start Application

Linux and macOS:
```bash
./mvnw spring-boot:run
```

Windows:
```cmd
mvnw.cmd spring-boot:run
```

The application will be available at http://localhost:8080

## Configuration

### AI Provider Selection

Set the provider in application.properties or via environment variable:

```properties
# Use local Ollama (default)
spring.ai.provider=ollama

# Use OpenAI
spring.ai.provider=openai
```

### Ollama Configuration

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama. chat.options.model=llama3.2:1b
spring.ai.ollama.chat. options.temperature=0.7
spring.ai.ollama.embedding.options.model=nomic-embed-text:v1.5
```

### OpenAI Configuration

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai. chat.options.model=gpt-4o-mini
spring. ai.openai.chat.options.temperature=0.7
spring.ai.openai.embedding. options.model=text-embedding-3-small
```

### Vector Store Configuration

```properties
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.distance-type=cosine_distance
spring. ai.vectorstore.pgvector.index-type=hnsw
```

Note: When switching between Ollama (768 dimensions) and OpenAI (1536 dimensions), you must update the dimensions setting and recreate the vector store table.

## Usage

### Create a Session

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"name": "Research Project", "description": "AI research documents"}'
```

Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Research Project",
  "description": "AI research documents",
  "documentCount": 0,
  "messageCount": 0,
  "createdAt": "2025-01-15T10:30:00"
}
```

### Upload Documents

Upload multiple documents to a session:
```bash
curl -X POST http://localhost:8080/api/sessions/{sessionId}/documents \
  -H "Content-Type: multipart/form-data" \
  -F "files=@document1.pdf" \
  -F "files=@document2.pdf" \
  -F "files=@document3.docx"
```

Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "totalFiles": 3,
  "successfulUploads": 3,
  "failedUploads": 0,
  "totalChunksCreated": 127,
  "totalProcessingTimeMs": 5430,
  "documents": [
    {"filename": "document1.pdf", "success": true, "chunksCreated": 45},
    {"filename": "document2.pdf", "success": true, "chunksCreated": 52},
    {"filename": "document3.docx", "success": true, "chunksCreated": 30}
  ],
  "message": "All documents processed successfully"
}
```

### Chat with Documents

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "question": "What are the main findings across all documents?",
    "topK": 10,
    "includeHistory": true
  }'
```

Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "answer": "Based on the documents, the main findings include.. .",
  "sources": ["Relevant excerpt from document1.. .", "Relevant excerpt from document2..."],
  "sourceDocuments": ["document1.pdf", "document2.pdf"],
  "question": "What are the main findings across all documents?",
  "processingTimeMs": 2340
}
```

### Delete a Session

```bash
curl -X DELETE http://localhost:8080/api/sessions/{sessionId}
```

This removes the session, all document metadata, conversation history, and vector embeddings.

## API Reference

### Session Management

#### Create Session
- Endpoint: POST /api/sessions
- Body: {"name": "string", "description": "string"} (both optional)
- Response: SessionResponse with generated sessionId

#### Get Session
- Endpoint: GET /api/sessions/{sessionId}
- Query Parameter: includeDocuments (boolean, default false)
- Response: SessionResponse with session details

#### List Sessions
- Endpoint: GET /api/sessions
- Response: Array of SessionResponse

#### Delete Session
- Endpoint: DELETE /api/sessions/{sessionId}
- Response: 204 No Content

### Document Management

#### Upload Documents
- Endpoint: POST /api/sessions/{sessionId}/documents
- Content Type: multipart/form-data
- Form Field: files (array of files)
- Response: MultiDocumentUploadResponse

#### Upload Single Document
- Endpoint: POST /api/sessions/{sessionId}/documents/single
- Content Type: multipart/form-data
- Form Field: file
- Response: MultiDocumentUploadResponse

### Chat

#### Send Message
- Endpoint: POST /api/chat
- Body:
```json
{
  "sessionId": "string (required)",
  "question": "string (required)",
  "topK": "integer (optional, default 5, range 1 to 20)",
  "includeHistory": "boolean (optional, default true)",
  "historyLimit": "integer (optional, default 10, range 1 to 50)"
}
```
- Response: ChatResponse with answer and sources

## Project Structure

```
chat-with-your-docs/
├── src/
│   ├── main/
│   │   ├── java/io/github/kxng0109/chatwithdocs/
│   │   │   ├── ChatWithDocsApplication.java
│   │   │   ├── config/
│   │   │   │   └── AiModelConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ChatController.java
│   │   │   │   ├── DocumentController.java
│   │   │   │   └── SessionController.java
│   │   │   ├── service/
│   │   │   │   ├── ChatService. java
│   │   │   │   ├── DocumentService. java
│   │   │   │   └── SessionService.java
│   │   │   ├── model/
│   │   │   │   ├── ChatRequest.java
│   │   │   │   ├── ChatResponse.java
│   │   │   │   ├── DocumentUploadResponse.java
│   │   │   │   ├── MultiDocumentUploadResponse.java
│   │   │   │   ├── SessionCreateRequest.java
│   │   │   │   └── SessionResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── ChatMessage.java
│   │   │   │   ├── ChatSession.java
│   │   │   │   └── SessionDocument.java
│   │   │   ├── repository/
│   │   │   │   ├── ChatMessageRepository.java
│   │   │   │   ├── ChatSessionRepository.java
│   │   │   │   └── SessionDocumentRepository.java
│   │   │   └── exception/
│   │   │       ├── DocumentProcessingException.java
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       └── SessionNotFoundException.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/io/github/kxng0109/chatwithdocs/
├── . env.example
├── . gitignore
├── docker-compose.yml
├── pom.xml
├── LICENSE
└── README.md
```

## Testing

### Run All Tests

Linux and macOS:
```bash
./mvnw test
```

Windows:
```cmd
mvnw. cmd test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=ChatServiceTest
```

## Deployment

### Docker Compose

Start all services:
```bash
docker-compose up -d
```

View logs:
```bash
docker-compose logs -f
```

Stop services:
```bash
docker-compose down
```

### Environment Variables for Production

```bash
POSTGRES_USERNAME=production_user
POSTGRES_PASSWORD=secure_production_password
SPRING_AI_PROVIDER=openai
OPENAI_API_KEY=sk-production-key
VECTOR_DIMENSIONS=1536
```

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Acknowledgments

- Spring AI Team for the AI integration framework
- Ollama Project for accessible local LLM execution
- PGVector Team for PostgreSQL vector similarity search
- Apache Software Foundation for Tika and PDFBox