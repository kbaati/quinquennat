import json
import os
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app

# In the Docker test image the data files are copied to /srv/donnees; in the repo they sit beside moteur/.
DATA_DIR = Path(os.environ.get("DONNEES_DIR", Path(__file__).resolve().parents[2] / "donnees"))


@pytest.fixture(scope="session")
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture(scope="session")
def foyers() -> list[dict]:
    return json.loads((DATA_DIR / "foyers.json").read_text(encoding="utf-8"))["foyers"]


@pytest.fixture(scope="session")
def leviers() -> list[dict]:
    return json.loads((DATA_DIR / "leviers.json").read_text(encoding="utf-8"))["leviers"]


@pytest.fixture(scope="session")
def simuler(client, foyers):
    def run(reforme: dict | None = None) -> dict[str, dict]:
        payload = {"reforme": reforme or {}, "foyers": [{"id": f["id"], "situation": f["situation"]} for f in foyers]}
        response = client.post("/simuler", json=payload)
        assert response.status_code == 200, response.text
        return {foyer["id"]: foyer for foyer in response.json()["foyers"]}

    return run
