import re

VERSION = re.compile(r"^\d+\.\d+\.\d+")


def test_sante_reports_openfisca_versions(client):
    response = client.get("/sante")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert VERSION.match(body["openfisca_core"])
    assert VERSION.match(body["openfisca_france"])
