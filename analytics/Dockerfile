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

# Honoured by both the health check and the server below, so the platform can
# assign the port. Defaults to 8000 for docker-compose.yml.
# HOST stays IPv4 by default because a plain Docker bridge may have no IPv6;
# set HOST=:: if a platform's private network resolves to IPv6 only.
ENV PORT=8000 \
    HOST=0.0.0.0

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD curl -fsS "http://localhost:${PORT}/health" || exit 1

# The service is internal only: it is never published to the browser, and it
# rejects every request unless ANALYTICS_INTERNAL_TOKEN matches the backend's.
# `exec` keeps uvicorn as PID 1 so it receives stop signals directly.
CMD ["sh", "-c", "exec uvicorn app.main:app --host ${HOST} --port ${PORT}"]
