"""Before/after computation of household situations with OpenFisca-France.

All households of a request are merged into one vectorised simulation. A parametric
reform is a mapping {parameter path: value}, applied from January 1st of the simulated year.
"""

import hashlib
import json
import re
import threading
from collections import OrderedDict
from functools import lru_cache

from openfisca_core.parameters import Parameter, ParameterNode, ParameterScale
from openfisca_core.periods import DateUnit
from openfisca_core.reforms import Reform
from openfisca_core.simulation_builder import SimulationBuilder
from openfisca_france import FranceTaxBenefitSystem

DEFAULT_YEAR = 2025
# Benefits depend on past resources (up to two years back), so inputs are repeated over three years.
HISTORY_YEARS = 2

# Output name -> (OpenFisca variable, sign applied so that taxes are reported as positive amounts).
OUTPUTS = {
    "revenu_disponible": ("revenu_disponible", 1),
    "impot_revenu": ("impot_revenu_restant_a_payer", -1),
    "csg": ("csg", -1),
    "rsa": ("rsa", 1),
    "prime_activite": ("ppa", 1),
    "allocations_familiales": ("af", 1),
    "aides_logement": ("aide_logement", 1),
}

# Singular key in a household situation -> (OpenFisca plural entity key).
GROUP_ENTITIES = {"menage": "menages", "famille": "familles", "foyer_fiscal": "foyers_fiscaux"}
ROLE_KEYS = {
    "personne_de_reference", "conjoint", "enfants", "autres",  # menage
    "parents",  # famille (shares "enfants")
    "declarants", "personnes_a_charge",  # foyer_fiscal
}

_SEGMENT = re.compile(r"^([A-Za-z_][A-Za-z0-9_]*)(?:\[(\d+)\])?$")
_SEPARATOR = "::"


class InvalidRequest(ValueError):
    """The request references something OpenFisca-France does not know, or is malformed."""


@lru_cache(maxsize=1)
def baseline_system() -> FranceTaxBenefitSystem:
    return FranceTaxBenefitSystem()


def resolve_parameter(root: ParameterNode, path: str) -> Parameter:
    """Finds a scalar parameter from a path such as ``a.b.bareme.brackets[1].rate``."""
    node = root
    for segment in path.split("."):
        match = _SEGMENT.match(segment)
        if not match:
            raise InvalidRequest(f"Chemin de paramètre mal formé : {path}")
        name, index = match.group(1), match.group(2)
        if isinstance(node, ParameterNode) and name in node.children:
            node = node.children[name]
        elif isinstance(node, ParameterScale) and name == "brackets" and index is not None:
            if int(index) >= len(node.brackets):
                raise InvalidRequest(f"Tranche inexistante : {path}")
            node = node.brackets[int(index)]
            continue
        elif not isinstance(node, (ParameterNode, ParameterScale, Parameter)) and name in ("rate", "threshold", "amount"):
            node = getattr(node, name, None)  # a bracket of a scale
        else:
            raise InvalidRequest(f"Paramètre inconnu : {path}")
        if index is not None:
            raise InvalidRequest(f"Chemin de paramètre mal formé : {path}")
    if not isinstance(node, Parameter):
        raise InvalidRequest(f"Le chemin ne désigne pas un paramètre scalaire : {path}")
    return node


def parameter_values(paths: list[str], year: int) -> dict[str, float]:
    """Baseline values at the end of the year, i.e. the most recent legislation known for it."""
    root = baseline_system().parameters
    return {path: resolve_parameter(root, path)(f"{year}-12-31") for path in paths}


_reform_cache: OrderedDict[str, object] = OrderedDict()
_result_cache: OrderedDict[str, list[dict]] = OrderedDict()
_cache_lock = threading.Lock()
_REFORM_CACHE_SIZE = 16
_RESULT_CACHE_SIZE = 256


def _fingerprint(value) -> str:
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def _cached(cache: OrderedDict, key: str, size: int, compute):
    with _cache_lock:
        if key in cache:
            cache.move_to_end(key)
            return cache[key]
    value = compute()
    with _cache_lock:
        cache[key] = value
        while len(cache) > size:
            cache.popitem(last=False)
    return value


def _reformed_system(reform: dict[str, float], year: int):
    baseline = baseline_system()
    for path in reform:  # fail early, with a clear message
        resolve_parameter(baseline.parameters, path)

    def modifier(parameters):
        for path, value in reform.items():
            resolve_parameter(parameters, path).update(start=f"{year}-01-01", value=value)
        return parameters

    class ParametricReform(Reform):
        def apply(self):
            self.modify_parameters(modifier_function=modifier)

    return ParametricReform(baseline)


def _periods(variable, year: int) -> list[str]:
    years = range(year - HISTORY_YEARS, year + 1)
    if variable.definition_period == DateUnit.ETERNITY:
        return ["ETERNITY"]
    if variable.definition_period == DateUnit.YEAR:
        return [str(y) for y in years]
    if variable.definition_period == DateUnit.MONTH:
        return [f"{y}-{month:02d}" for y in years for month in range(1, 13)]
    raise InvalidRequest(f"Période non gérée pour la variable {variable.name}")


def _expand_variables(system, entity_key: str, values: dict, year: int) -> dict:
    """Turns {variable: value per definition period} into OpenFisca's {variable: {period: value}}."""
    expanded = {}
    for name, value in values.items():
        variable = system.variables.get(name)
        if variable is None:
            raise InvalidRequest(f"Variable inconnue : {name}")
        if variable.entity.key != entity_key:
            raise InvalidRequest(f"La variable {name} porte sur « {variable.entity.key} », pas sur « {entity_key} »")
        expanded[name] = {period: value for period in _periods(variable, year)}
    return expanded


def _merged_situation(system, households: list[dict], year: int) -> dict:
    situation = {"individus": {}, "menages": {}, "familles": {}, "foyers_fiscaux": {}}
    seen = set()
    for household in households:
        household_id = household["id"]
        if _SEPARATOR in household_id or household_id in seen:
            raise InvalidRequest(f"Identifiant de foyer invalide ou en double : {household_id}")
        seen.add(household_id)
        source = household["situation"]
        persons = source.get("individus") or {}
        if not persons:
            raise InvalidRequest(f"Le foyer {household_id} n'a aucun individu")
        for name, values in persons.items():
            situation["individus"][f"{household_id}{_SEPARATOR}{name}"] = _expand_variables(
                system, "individu", values, year)
        for singular, plural in GROUP_ENTITIES.items():
            group = source.get(singular)
            if group is None:
                raise InvalidRequest(f"Le foyer {household_id} n'a pas d'entité « {singular} »")
            roles = {}
            variables = {}
            for key, value in group.items():
                if key in ROLE_KEYS:
                    for name in value:
                        if name not in persons:
                            raise InvalidRequest(f"Individu inconnu dans le foyer {household_id} : {name}")
                    roles[key] = [f"{household_id}{_SEPARATOR}{name}" for name in value]
                else:
                    variables[key] = value
            situation[plural][household_id] = {**roles, **_expand_variables(system, singular, variables, year)}
    return situation


def _compute(system, households: list[dict], year: int) -> list[dict]:
    try:
        simulation = SimulationBuilder().build_from_entities(system, _merged_situation(system, households, year))
    except InvalidRequest:
        raise
    except Exception as error:  # OpenFisca's own validation errors (bad enum value, bad role...)
        raise InvalidRequest(f"Situation refusée par OpenFisca : {error}") from error

    household_ids = [household["id"] for household in households]
    results = {household_id: {} for household_id in household_ids}
    for output, (variable_name, sign) in OUTPUTS.items():
        variable = system.variables[variable_name]
        if variable.definition_period == DateUnit.YEAR:
            values = simulation.calculate(variable_name, str(year))
        else:
            values = simulation.calculate_add(variable_name, str(year))
        totals = dict.fromkeys(household_ids, 0.0)
        for entity_id, value in zip(simulation.populations[variable.entity.key].ids, values):
            totals[str(entity_id).split(_SEPARATOR)[0]] += float(value) * sign
        for household_id, total in totals.items():
            results[household_id][output] = round(total, 2) + 0.0  # "+ 0.0" turns -0.0 into 0.0
    return [results[household_id] for household_id in household_ids]


def simulate(households: list[dict], reform: dict[str, float], year: int = DEFAULT_YEAR) -> list[dict]:
    """Returns, for each household in order, {"id", "avant": {...}, "apres": {...}}."""
    households_key = _fingerprint([year, households])
    before = _cached(_result_cache, households_key, _RESULT_CACHE_SIZE,
                     lambda: _compute(baseline_system(), households, year))
    if reform:
        reform_key = _fingerprint([year, reform])
        system = _cached(_reform_cache, reform_key, _REFORM_CACHE_SIZE, lambda: _reformed_system(reform, year))
        after = _cached(_result_cache, households_key + reform_key, _RESULT_CACHE_SIZE,
                        lambda: _compute(system, households, year))
    else:
        after = before
    return [
        {"id": household["id"], "avant": dict(avant), "apres": dict(apres)}
        for household, avant, apres in zip(households, before, after)
    ]
