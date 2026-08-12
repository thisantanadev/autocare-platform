FROM python:3.12-slim
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && \
    apt-get install --no-install-recommends -y curl && \
    rm -rf /var/lib/apt/lists/* && \
    useradd --system --create-home --shell /usr/sbin/nologin analytics

# Runtime dependencies are pinned to the same floors declared in
# pyproject.toml; they are installed first so app changes reuse this layer.
RUN pip install --no-cache-dir \
    "fastapi>=0.115" \
    "uvicorn[standard]>=0.30" \
    "pydantic>=2.8" \
    "pydantic-settings>=2.4"

COPY app/ app/
USER analytics

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD curl -fsS http://localhost:8000/health || exit 1

# The service is internal only: it is never published to the browser, and it
# rejects every request unless ANALYTICS_INTERNAL_TOKEN matches the backend's.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
