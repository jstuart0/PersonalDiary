"""Celery application for background tasks"""

from celery import Celery
from app.config import settings

# Create Celery app
celery_app = Celery(
    "personal_diary",
    broker=settings.celery_broker_url,
    backend=settings.celery_result_backend,
    include=["app.tasks"],  # Import task modules
)

# Configure Celery
celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
    task_track_started=True,
    task_time_limit=30 * 60,  # 30 minutes
    task_soft_time_limit=25 * 60,  # 25 minutes
    worker_prefetch_multiplier=1,
    worker_max_tasks_per_child=1000,
)

# Configure periodic tasks (beat schedule)
celery_app.conf.beat_schedule = {
    "sync-facebook-posts": {
        "task": "app.tasks.sync_facebook_posts",
        "schedule": 3600.0,  # Every hour
    },
    "cleanup-expired-media": {
        "task": "app.tasks.cleanup_expired_media",
        "schedule": 86400.0,  # Every 24 hours
    },
}

if __name__ == "__main__":
    celery_app.start()
