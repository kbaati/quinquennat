"""Thin, stateless HTTP layer over OpenFisca-France. No product logic here."""

from importlib.metadata import version

from fastapi import FastAPI

app = FastAPI(title="Quinquennat - moteur", docs_url=None, redoc_url=None)


@app.get("/sante")
def sante() -> dict[str, str]:
    return {
        "status": "ok",
        "openfisca_core": version("openfisca-core"),
        "openfisca_france": version("openfisca-france"),
    }
