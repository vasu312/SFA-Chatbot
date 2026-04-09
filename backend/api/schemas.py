from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    question: str = Field(
        ...,
        min_length=1,
        max_length=1000,
        description="Natural language question about the SFA data",
    )


class ChatResponse(BaseModel):
    status: str
    question: str
    generated_sql: Optional[str] = None
    results: List[Dict] = []
    row_count: int = 0
    columns: List[str] = []
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    database_connected: bool
    vanna_ready: bool


class ColumnInfo(BaseModel):
    name: str
    type: str


class TableSchema(BaseModel):
    name: str
    columns: List[ColumnInfo]


class SchemaResponse(BaseModel):
    tables: List[TableSchema]
