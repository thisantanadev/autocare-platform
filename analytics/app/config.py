from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Environment-driven configuration.

    ``analytics_internal_token`` guards the internal API. When it is not
    configured the service fails closed: every analytics request is rejected.
    """

    analytics_internal_token: str = ""


settings = Settings()
