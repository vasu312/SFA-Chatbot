# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SFA Chatbot is a full-stack Sales Force Automation chat application. Users ask natural language questions on an Android app, which sends them to a FastAPI backend. The backend uses Vanna.ai (ChromaDB + OpenAI) to convert questions to SQL, executes against a SQLite database, and returns structured results.

## Commands

### Backend
```bash
cd backend
pip install -r requirements.txt
cp .env.example .env              # then fill in API_KEY and OPENAI_API_KEY
python3 data/create_sample_db.py  # create/reset SQLite DB with sample data
python3 main.py                   # starts uvicorn on 0.0.0.0:8000
```

Test endpoints manually:
```bash
curl http://localhost:8000/api/v1/health -H "X-API-Key: <key>"
curl -X POST http://localhost:8000/api/v1/chat -H "X-API-Key: <key>" -H "Content-Type: application/json" -d '{"question": "Show all products"}'
```

Swagger docs at `http://localhost:8000/docs`.

### Android
Open `android/` in Android Studio, sync Gradle, build and run. The emulator uses `http://10.0.2.2:8000/` to reach the host machine's backend. API key is set in `android/app/build.gradle.kts` via `buildConfigField`.

## Architecture

### Backend (FastAPI + Vanna.ai)

**Request flow:** `main.py` → `api/router.py` → `core/vanna_client.py` → SQLite

- **`core/vanna_client.py`** — Heart of the system. `SFAVanna` subclasses `ChromaDB_VectorStore` + `OpenAI_Chat`. Singleton via `get_vanna()`. On first init, trains Vanna from `data/training/` (DDL, business docs, sample Q&A pairs). `generate_and_execute(question)` is the main function: generates SQL → validates SELECT-only → executes → returns dict. Never raises; errors go in the response `error` field.
- **`api/router.py`** — Three endpoints under `/api/v1`: `POST /chat` (main query), `GET /health`, `GET /schema`. All require `X-API-Key` header.
- **`core/database.py`** — Read-only SQLite connections (`?mode=ro` URI). Used by `/health` and `/schema` endpoints only; Vanna manages its own connection for query execution.
- **`config.py`** — Pydantic Settings loaded from `.env`. Required vars: `API_KEY`, `OPENAI_API_KEY`.

**Safety layers:** SELECT-only SQL guard in vanna_client + read-only SQLite connection in database.py.

### Android (Kotlin / MVVM)

**Data flow:** `ChatActivity` → `ChatViewModel` → `ChatRepository` → (Room + Retrofit)

- **Room is the single source of truth.** The UI observes `Flow<List<ChatMessage>>` from Room, never reads API responses directly. Both user messages and system responses are persisted as `ChatMessageEntity` rows.
- **`ChatRepository`** — Orchestrates the flow: saves user message to Room immediately (optimistic), calls API, saves response to Room. Errors are saved as system messages in chat.
- **`ChatViewModel`** — Exposes `messages: StateFlow` (from Room) and `isLoading: StateFlow`. No DI framework; uses `ChatViewModelFactory` with manual injection from `SfaChatbotApplication`.
- **`ApiClient`** — OkHttp with auth interceptor (injects `X-API-Key`), 60s read timeout (LLM inference can be slow).
- **`ChatAdapter`** — `ListAdapter` with DiffUtil, two ViewHolder types for user/system bubbles.

### Database Schema (8 tables)

SFA domain: `routes`, `products`, `salesmen`, `outlets`, `salesman_routes`, `orders`, `visits`, `order_items`. Boolean fields (`is_active`, `has_order`) are stored as `'true'`/`'false'` strings. IDs are VARCHAR, not integers. Timestamps are ISO 8601 text.

### Vanna Training

Training data lives in `backend/data/training/`. Vanna is trained on DDL statements, a business documentation file (explains table relationships, data conventions, common query patterns), and 10 sample question-SQL pairs. Training is persisted in ChromaDB at `CHROMADB_PATH` and only runs once (skipped if data already exists).
