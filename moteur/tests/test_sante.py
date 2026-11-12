import re

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

VERSION = re.compile(r"^\d+\.\d+\.\d+")


def test_sante_reports_openfisca_versions():
    response = client.get("/sante")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert VERSION.match(body["openfisca_core"])
    assert VERSION.match(body["openfisca_france"])


def test_openfisca_france_is_importable():
    # Guards against a broken install: the health endpoint only reads package metadata.
    from openfisca_france import FranceTaxBenefitSystem  # noqa: F401
