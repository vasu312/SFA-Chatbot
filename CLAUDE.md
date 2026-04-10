# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SFA Chatbot is a full-stack Sales Force Automation chat application. Users ask natural language questions on an Android app, which sends them to a FastAPI backend. The backend uses Anthropic Claude (via direct SDK calls) to convert questions to SQL, executes against a SQLite database, and returns structured results. A separate dashboard endpoint provides pre-computed day/month summary statistics.

## Commands

### Backend
```bash
cd backend
pip install -r requirements.txt
cp .env.example .env              # then fill in API_KEY and ANTHROPIC_API_KEY
python3 data/create_sample_db.py  # create/reset SQLite DB with sample data (inserts today's data dynamically)
python3 main.py                   # starts uvicorn on 0.0.0.0:8000
```

Test endpoints manually:
```bash
curl http://localhost:8000/api/v1/health -H "X-API-Key: <key>"
curl -X POST http://localhost:8000/api/v1/chat -H "X-API-Key: <key>" -H "Content-Type: application/json" -d '{"question": "Show all products"}'
curl http://localhost:8000/api/v1/summary -H "X-API-Key: <key>"
```

Swagger docs at `http://localhost:8000/docs`.

### Android
Open `android/` in Android Studio, sync Gradle, build and run. The emulator uses `http://10.0.2.2:8000/` to reach the host machine's backend; physical devices use the host machine's LAN IP (set via `BASE_URL` in `build.gradle.kts`). API key is set in `android/app/build.gradle.kts` via `buildConfigField`.

## Architecture

### Backend (FastAPI + Anthropic Claude)

**Request flow:** `main.py` → `api/router.py` → `core/vanna_client.py` → SQLite

- **`core/vanna_client.py`** — Heart of the NL-to-SQL system. On startup, `_build_system_prompt()` loads `data/training/` (DDL, business docs, sample Q&A pairs) and embeds them directly into an Anthropic Claude system prompt — no ChromaDB or vector store is used. `generate_and_execute(question)` is the main entry point: detects conversational messages (greetings, thanks) and short-circuits them; otherwise calls `_generate_sql(question)` → validates SELECT-only → `_run_sql(sql)` → returns dict. Never raises; errors go in the response `error` field.
- **`core/anthropic_chat.py`** — Vanna compatibility layer implementing `VannaBase` abstract methods using the Anthropic SDK. Currently unused by `vanna_client.py`; kept for potential future Vanna integration.
- **`api/router.py`** — Four endpoints under `/api/v1`: `POST /chat` (NL query), `GET /health`, `GET /schema`, `GET /summary`. All require `X-API-Key` header.
- **`api/schemas.py`** — Pydantic request/response models: `ChatRequest`, `ChatResponse`, `HealthResponse`, `SummaryStats`, `TopPerformer`, `SummaryResponse`, `TableSchema`, `ColumnInfo`.
- **`core/database.py`** — Read-only SQLite connections (`?mode=ro` URI). `get_summary_stats()` runs the dashboard query (day/month orders, visits, top performers). Used by `/health`, `/schema`, and `/summary` endpoints; `vanna_client.py` manages its own connection for query execution.
- **`config.py`** — Pydantic Settings loaded from `.env`. Required vars: `API_KEY`, `ANTHROPIC_API_KEY`. Optional: `LLM_MODEL` (default `claude-sonnet-4-20250514`), `SQLITE_DB_PATH`, `CHROMADB_PATH` (legacy, unused).

**Safety layers:** SELECT-only SQL guard in `vanna_client.py` + read-only SQLite connection in `database.py`.

### Android (Kotlin / Jetpack Compose / MVVM)

**Data flow:** `ChatActivity` → `ChatViewModel` → `ChatRepository` → (Room + Retrofit)

- **Room is the single source of truth.** The UI observes `Flow` from Room, never reads API responses directly. Both user messages and system responses are persisted.
- **`ChatActivity`** — Single activity. Hosts a `ModalNavigationDrawer` (side nav) and switches between two composable screens via a sealed class: `Screen.Chat` and `Screen.Dashboard`. Back handler closes the drawer or navigates back to Dashboard.
- **`ChatScreen`** — Compose UI for the chat interface. Displays message bubbles (user: gradient blue; system: card with optional table), shimmer loading animation, bottom input bar, and 8 suggested question chips. Table rendering supports smart column widths, horizontal scroll, numeric right-alignment, and currency formatting.
- **`DashboardScreen`** — Compose UI showing pre-computed stats from `GET /summary`. Two summary cards (Today / This Month) with orders, value, visits, lines sold. Four top-performer cards (Salesman, Outlet, Product, Route). Decorative gradient background with refresh button.
- **`DrawerContent`** — Compose navigation drawer. Header with app branding, Dashboard nav link, New Chat button, and a `HISTORY` section listing all conversations (rename and delete inline).
- **`ChatViewModel`** — Exposes `messages: StateFlow`, `conversations: StateFlow`, `isLoading: StateFlow`, `summary: StateFlow`. Handles conversation lifecycle: `createNewConversation()`, `switchConversation()`, `updateConversationTitle()`, `deleteConversation()`, `refreshSummary()`. Auto-titles a conversation from its first message (up to 40 chars). No DI framework; uses `ChatViewModelFactory` with manual injection from `SfaChatbotApplication`.
- **`ChatRepository`** — Orchestrates the full flow: saves user message to Room immediately (optimistic), calls API, saves response or error to Room. Also handles conversation CRUD and `fetchSummary()`. Errors are saved as system messages in chat.
- **`ApiClient`** — OkHttp with auth interceptor (injects `X-API-Key`), 30s connect timeout, 120s read timeout (LLM inference can be slow). BODY-level logging interceptor.
- **`NetworkResult`** — Sealed class: `Success<T>`, `Error(message, code)`, `Loading`.

### Database Schema

#### SQLite (backend — 8 tables)

SFA domain: `routes`, `products`, `salesmen`, `outlets`, `salesman_routes`, `orders`, `visits`, `order_items`. Boolean fields (`is_active`, `has_order`) are stored as `'true'`/`'false'` strings. IDs are VARCHAR, not integers. Timestamps are ISO 8601 text. `create_sample_db.py` inserts sample data dated Apr 1–9, 2026 **plus** today's transactions dynamically so the dashboard always shows current-day metrics.

#### Room (Android — 2 tables)

- **`conversations`** — `id`, `title`, `createdAt`, `updatedAt`
- **`chat_messages`** — `id`, `conversation_id` (FK), `content`, `is_user`, `sql`, `table_json`, `timestamp`

Room version 3; uses `fallbackToDestructiveMigration()` for development.

### Vanna Training

Training data lives in `backend/data/training/`: `ddl.sql` (schema DDL), `documentation.md` (business context, data conventions, query patterns), `sample_queries.json` (10 example question-SQL pairs). At startup, `_build_system_prompt()` reads all three files and embeds them directly into the Anthropic Claude system prompt for in-context learning. There is no ChromaDB persistence — the system prompt is rebuilt from files on every cold start.
