import logging

from fastapi import APIRouter, Depends

from api.dependencies import verify_api_key
from api.schemas import (
    ChatRequest,
    ChatResponse,
    ColumnInfo,
    HealthResponse,
    SchemaResponse,
    TableSchema,
)
from core.database import check_connection, get_table_schema
from core.vanna_client import generate_and_execute, get_vanna

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", dependencies=[Depends(verify_api_key)])


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Process a natural language question and return SQL query results."""
    logger.info(f"Chat request: {request.question[:100]}")
    result = generate_and_execute(request.question)
    return ChatResponse(question=request.question, **result)


@router.get("/health", response_model=HealthResponse)
async def health():
    """Health check endpoint."""
    db_ok = check_connection()
    vanna_ok = get_vanna() is not None
    return HealthResponse(
        status="healthy" if (db_ok and vanna_ok) else "degraded",
        database_connected=db_ok,
        vanna_ready=vanna_ok,
    )


@router.get("/schema", response_model=SchemaResponse)
async def schema():
    """Return database table and column information."""
    tables = get_table_schema()
    return SchemaResponse(
        tables=[
            TableSchema(
                name=t["name"],
                columns=[ColumnInfo(**c) for c in t["columns"]],
            )
            for t in tables
        ]
    )
