# Build context: the repository root (the tests need donnees/).
FROM python:3.11-slim AS base
WORKDIR /srv
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY moteur/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY moteur/app app

# docker build -f moteur/Dockerfile --target test -t quinquennat-moteur-test .
# docker run --rm quinquennat-moteur-test
FROM base AS test
COPY moteur/requirements-dev.txt .
RUN pip install --no-cache-dir -r requirements-dev.txt
COPY moteur/tests tests
COPY donnees donnees
ENV DONNEES_DIR=/srv/donnees
CMD ["pytest", "-q", "-p", "no:cacheprovider"]

FROM base AS runtime
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
