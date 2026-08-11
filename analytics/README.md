# AutoCare Analytics Service

Internal FastAPI service that computes deterministic vehicle analytics
(costs, fuel efficiency, trends) from sanitized data sent by the Java
backend. It has no database access and is never exposed to browsers.

See the repository root `README.md` and `docs/ARCHITECTURE.md` for the
full picture, and `app/calculations.py` for every calculation assumption.
