import secrets

from fastapi import Depends, FastAPI, Header, HTTPException

from .calculations import build_report
from .config import settings
from .models import AnalyticsReport, VehicleAnalyticsRequest

app = FastAPI(
    title="AutoCare Analytics",
    version="1.0.0",
    description="Internal deterministic analytics service. Consumed only by the "
    "AutoCare Java backend, never directly by browsers.",
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


def require_internal_token(x_internal_token: str | None = Header(default=None)) -> None:
    """Constant-time check of the shared internal token.

    Fails closed: when the token is not configured, every request is
    rejected rather than silently exposing the service.
    """
    expected = settings.analytics_internal_token
    if not expected or not x_internal_token or not secrets.compare_digest(
        x_internal_token, expected
    ):
        raise HTTPException(status_code=401, detail="Invalid internal token")


@app.post("/internal/v1/analytics/vehicle", response_model=AnalyticsReport)
def analyze_vehicle(
    request: VehicleAnalyticsRequest, _: None = Depends(require_internal_token)
) -> AnalyticsReport:
    return build_report(request)
