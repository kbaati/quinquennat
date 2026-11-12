"""Thin, stateless HTTP layer over OpenFisca-France. No product logic here."""

import threading
from contextlib import asynccontextmanager
from importlib.metadata import version

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from app import simulation

@asynccontextmanager
async def lifespan(_: FastAPI):
    # Loading the legislation takes a few seconds: do it before the first request, without blocking /sante.
    threading.Thread(target=simulation.baseline_system, daemon=True).start()
    yield


app = FastAPI(title="Quinquennat - moteur", docs_url=None, redoc_url=None, lifespan=lifespan)


def versions() -> dict[str, str]:
    return {
        "openfisca_core": version("openfisca-core"),
        "openfisca_france": version("openfisca-france"),
    }


class Foyer(BaseModel):
    id: str = Field(min_length=1)
    situation: dict


class SimulerRequest(BaseModel):
    annee: int = Field(default=simulation.DEFAULT_YEAR, ge=2020, le=2030)
    reforme: dict[str, float] = {}
    foyers: list[Foyer] = Field(min_length=1, max_length=100)


class ParametresRequest(BaseModel):
    annee: int = Field(default=simulation.DEFAULT_YEAR, ge=2020, le=2030)
    chemins: list[str] = Field(max_length=100)


@app.get("/sante")
def sante() -> dict[str, str]:
    return {"status": "ok", **versions()}


@app.post("/simuler")
def simuler(request: SimulerRequest) -> dict:
    try:
        foyers = simulation.simulate(
            [foyer.model_dump() for foyer in request.foyers], request.reforme, request.annee)
    except simulation.InvalidRequest as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    return {"annee": request.annee, "versions": versions(), "foyers": foyers}


@app.post("/parametres")
def parametres(request: ParametresRequest) -> dict:
    """Current legislation values, so that callers never hard-code them."""
    try:
        valeurs = simulation.parameter_values(request.chemins, request.annee)
    except simulation.InvalidRequest as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    return {"annee": request.annee, "versions": versions(), "valeurs": valeurs}
