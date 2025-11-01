"""Main FastAPI application"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.database import close_db, init_db

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    # Startup
    logger.info(f"Starting {settings.app_name} in {settings.app_env} mode")
    await init_db()
    logger.info("Database initialized")

    yield

    # Shutdown
    logger.info("Shutting down application")
    await close_db()
    logger.info("Database connections closed")


# Create FastAPI application
app = FastAPI(
    title=settings.app_name,
    description="Privacy-first diary platform with dual-tier encryption",
    version="0.1.0",
    docs_url=f"{settings.api_v1_prefix}/docs",
    redoc_url=f"{settings.api_v1_prefix}/redoc",
    openapi_url=f"{settings.api_v1_prefix}/openapi.json",
    lifespan=lifespan,
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=settings.cors_credentials,
    allow_methods=settings.cors_methods,
    allow_headers=settings.cors_headers,
)


# Health check endpoint
@app.get("/health", tags=["Health"])
async def health_check():
    """Health check endpoint for monitoring"""
    return JSONResponse(
        status_code=200,
        content={
            "status": "healthy",
            "app": settings.app_name,
            "version": "0.1.0",
            "environment": settings.app_env,
        },
    )


# Root endpoint
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint with API information"""
    return {
        "message": f"Welcome to {settings.app_name}",
        "version": "0.1.0",
        "docs": f"{settings.api_v1_prefix}/docs",
        "health": "/health",
    }


# Import and include routers
from app.routers import auth, entries, media, search, integrations

app.include_router(auth.router, prefix=f"{settings.api_v1_prefix}/auth", tags=["Authentication"])
app.include_router(entries.router, prefix=f"{settings.api_v1_prefix}/entries", tags=["Entries"])
app.include_router(media.router, prefix=f"{settings.api_v1_prefix}/media", tags=["Media"])
app.include_router(search.router, prefix=f"{settings.api_v1_prefix}/search", tags=["Search"])
app.include_router(integrations.router, prefix=f"{settings.api_v1_prefix}/integrations", tags=["Integrations"])


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
        log_level=settings.log_level.lower(),
    )
