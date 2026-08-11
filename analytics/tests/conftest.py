import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app

TEST_TOKEN = "test-internal-token"


@pytest.fixture(autouse=True)
def configured_internal_token():
    settings.analytics_internal_token = TEST_TOKEN
    yield
    settings.analytics_internal_token = ""


@pytest.fixture
def client():
    return TestClient(app)
