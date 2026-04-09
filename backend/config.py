from typing import List

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # API
    API_KEY: str
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # Database
    SQLITE_DB_PATH: str = "./data/sfa.db"

    # Vanna / LLM (Anthropic)
    ANTHROPIC_API_KEY: str
    LLM_MODEL: str = "claude-sonnet-4-20250514"

    # ChromaDB (vector store for Vanna training data)
    CHROMADB_PATH: str = "./data/chromadb"

    # CORS
    CORS_ORIGINS: List[str] = ["*"]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
