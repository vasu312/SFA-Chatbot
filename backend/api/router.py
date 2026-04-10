import logging

from fastapi import APIRouter, Depends

from api.dependencies import verify_api_key
from api.schemas import (
    ChatRequest,
    ChatResponse,
    ColumnInfo,
    HealthResponse,
    SchemaResponse,
    SummaryResponse,
    TableSchema,
)
from core.database import check_connection, get_summary_stats, get_table_schema
from core.vanna_client import generate_and_execute
from config import settings

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
    llm_ready = bool(settings.ANTHROPIC_API_KEY)
    return HealthResponse(
        status="healthy" if (db_ok and llm_ready) else "degraded",
        database_connected=db_ok,
        vanna_ready=llm_ready,
    )


@router.get("/summary", response_model=SummaryResponse)
async def summary():
    """Return day and month summary stats based on the most recent data date."""
    data = get_summary_stats()
    return SummaryResponse(
        day=data["day"],
        month=data["month"],
        reference_date=data["reference_date"],
        top_salesman=data.get("top_salesman"),
        top_outlet=data.get("top_outlet"),
        top_product=data.get("top_product"),
        top_route=data.get("top_route"),
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
