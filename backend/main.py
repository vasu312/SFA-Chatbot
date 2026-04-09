import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.router import router
from config import settings
from core.vanna_client import get_vanna

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="SFA Chatbot API",
    description="Natural language to SQL chatbot for Sales Force Automation data",
    version="1.0.0",
    docs_url="/docs",
    redoc_url=None,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

# Include API router
app.include_router(router)


@app.on_event("startup")
async def startup():
    """Initialize Vanna on startup so the first request is fast."""
    logger.info("Starting SFA Chatbot API...")
    try:
        get_vanna()
        logger.info("Vanna initialized successfully.")
    except Exception as e:
        logger.error(f"Failed to initialize Vanna: {e}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host=settings.HOST, port=settings.PORT, reload=True)
