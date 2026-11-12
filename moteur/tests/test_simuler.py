"""Plausibility of the current situation (J1) and consistency of parametric reforms (J2)."""

import pytest

IR = "impot_revenu.bareme_ir_depuis_1945.bareme.brackets[{}].rate"
RSA = "prestations_sociales.solidarite_insertion.minima_sociaux.rsa.rsa_m.montant_de_base_du_rsa"
BMAF = "prestations_sociales.prestations_familiales.bmaf.bmaf"


def test_every_household_gets_every_amount(simuler, foyers):
    result = simuler()

    assert list(result) == [foyer["id"] for foyer in foyers]
    for foyer in result.values():
        assert set(foyer["avant"]) == {
            "revenu_disponible", "impot_revenu", "csg", "rsa", "prime_activite",
            "allocations_familiales", "aides_logement"}
        assert all(amount >= 0 for name, amount in foyer["avant"].items() if name != "impot_revenu")


def test_disposable_incomes_are_plausible(simuler):
    avant = {household_id: foyer["avant"] for household_id, foyer in simuler().items()}

    # Wide brackets on purpose: they catch a broken situation (income counted monthly instead of
    # yearly, missing benefit, wrong household composition), not a legislative update.
    assert 9_000 < avant["seul-sans-revenu"]["revenu_disponible"] < 13_000
    assert 17_000 < avant["seul-smic"]["revenu_disponible"] < 23_000
    assert 35_000 < avant["cadre-celibataire"]["revenu_disponible"] < 50_000
    assert 40_000 < avant["couple-median-deux-enfants"]["revenu_disponible"] < 60_000
    assert 12_000 < avant["retraite-modeste"]["revenu_disponible"] < 17_000
    assert avant["couple-aise-sans-enfant"]["revenu_disponible"] > 100_000


def test_taxes_and_benefits_go_to_the_expected_households(simuler):
    avant = {household_id: foyer["avant"] for household_id, foyer in simuler().items()}

    assert avant["seul-sans-revenu"]["rsa"] > 5_000
    assert avant["seul-sans-revenu"]["impot_revenu"] == 0
    assert avant["parent-isole-sans-revenu"]["rsa"] > avant["seul-sans-revenu"]["rsa"]
    assert avant["seul-smic"]["prime_activite"] > 0
    assert avant["seul-smic"]["rsa"] == 0
    assert avant["cadre-celibataire"]["impot_revenu"] > 4_000
    assert avant["cadre-celibataire"]["prime_activite"] == 0
    assert avant["couple-aise-sans-enfant"]["impot_revenu"] > avant["cadre-celibataire"]["impot_revenu"]
    assert avant["couple-median-deux-enfants"]["allocations_familiales"] > 1_000
    assert avant["couple-trois-enfants"]["allocations_familiales"] \
        > avant["couple-median-deux-enfants"]["allocations_familiales"]
    assert avant["seul-smic"]["allocations_familiales"] == 0
    assert avant["couple-retraites-aises"]["impot_revenu"] > 0


def test_income_tax_is_computed_jointly_for_couples(simuler):
    avant = {household_id: foyer["avant"] for household_id, foyer in simuler().items()}

    # Two earners at 5 500 and 4 800 with two children pay less than twice a single earner at 5 500.
    assert avant["couple-cadres-deux-enfants"]["impot_revenu"] < 2 * avant["cadre-celibataire"]["impot_revenu"]


def test_empty_reform_changes_nothing(simuler):
    for foyer in simuler({}).values():
        assert foyer["apres"] == foyer["avant"]


@pytest.mark.parametrize("bracket, current", [(1, 0.11), (2, 0.30), (3, 0.41), (4, 0.45)])
def test_raising_an_income_tax_rate_never_lowers_income_tax(simuler, bracket, current):
    result = simuler({IR.format(bracket): current + 0.05})

    for household_id, foyer in result.items():
        assert foyer["apres"]["impot_revenu"] >= foyer["avant"]["impot_revenu"], household_id
        assert foyer["apres"]["revenu_disponible"] <= foyer["avant"]["revenu_disponible"], household_id
    assert any(f["apres"]["impot_revenu"] > f["avant"]["impot_revenu"] for f in result.values())


def test_raising_rsa_only_helps_and_reaches_households_without_income(simuler):
    result = simuler({RSA: 800})

    for household_id, foyer in result.items():
        assert foyer["apres"]["revenu_disponible"] >= foyer["avant"]["revenu_disponible"], household_id
    assert result["seul-sans-revenu"]["apres"]["rsa"] > result["seul-sans-revenu"]["avant"]["rsa"]
    assert result["cadre-celibataire"]["apres"] == result["cadre-celibataire"]["avant"]


def test_family_allowance_base_only_affects_families(simuler):
    result = simuler({BMAF: 600})

    assert result["couple-median-deux-enfants"]["apres"]["allocations_familiales"] \
        > result["couple-median-deux-enfants"]["avant"]["allocations_familiales"]
    assert result["seul-smic"]["apres"] == result["seul-smic"]["avant"]


def test_lever_catalogue_matches_openfisca(client, leviers):
    """Every lever points to an existing parameter and displays its real current value."""
    paths = [levier["parametre"] for levier in leviers]

    response = client.post("/parametres", json={"chemins": paths})

    assert response.status_code == 200, response.text
    valeurs = response.json()["valeurs"]
    for levier in leviers:
        assert levier["valeurActuelle"] * levier["facteur"] == pytest.approx(valeurs[levier["parametre"]]), levier["id"]
        assert levier["min"] <= levier["valeurActuelle"] <= levier["max"], levier["id"]


def test_unknown_parameter_is_rejected(client, foyers):
    payload = {"reforme": {"impot_revenu.n_existe_pas": 1}, "foyers": foyers[:1]}

    response = client.post("/simuler", json=payload)

    assert response.status_code == 400
    assert "Paramètre inconnu" in response.json()["detail"]


def test_non_scalar_parameter_is_rejected(client, foyers):
    payload = {"reforme": {"impot_revenu.bareme_ir_depuis_1945.bareme": 1}, "foyers": foyers[:1]}

    assert client.post("/simuler", json=payload).status_code == 400


def test_unknown_variable_is_rejected(client):
    foyer = {"id": "x", "situation": {
        "individus": {"a": {"variable_inventee": 1}},
        "menage": {"personne_de_reference": ["a"]},
        "famille": {"parents": ["a"]},
        "foyer_fiscal": {"declarants": ["a"]}}}

    response = client.post("/simuler", json={"foyers": [foyer]})

    assert response.status_code == 400
    assert "Variable inconnue" in response.json()["detail"]
