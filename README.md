# Quinquennat

Un bac à sable pour composer un programme fiscal et social, puis voir son effet sur le budget
d'une vingtaine de foyers types, calculé avec les règles socio-fiscales françaises
([OpenFisca-France](https://github.com/openfisca/openfisca-france)).

> **État : jalon J0 (squelette).** Le site n'affiche pour l'instant que l'état des services.

## Ce que l'outil est, et ce qu'il n'est pas

- Il calcule un avant / après en euros par an pour des **foyers types**. Ce n'est pas un
  échantillon représentatif de la population, et il ne chiffre pas de coût budgétaire national.
- Ce n'est **pas un sondage** : aucune intention de vote, aucune estimation d'opinion.
- Il est **neutre** : aucun parti, aucun candidat, aucun jugement de valeur.
- Il ne collecte **aucune donnée personnelle** : pas de compte, pas de cookie de suivi,
  pas d'outil de mesure d'audience tiers.

## Architecture

| Module      | Rôle                                                        | Technologie                    |
| ----------- | ----------------------------------------------------------- | ------------------------------ |
| `moteur/`   | Calcul socio-fiscal, sans état ni logique produit           | Python, FastAPI, OpenFisca     |
| `backend/`  | API publique, catalogue de leviers, simulations partageables | Java 21, Spring Boot, Maven    |
| `frontend/` | Interface en français                                       | React, Vite, TypeScript        |

Le frontend n'appelle que le backend ; le backend appelle le moteur par HTTP.

## Lancer le projet

Prérequis : Docker.

```bash
docker compose up --build
```

Puis ouvrir <http://localhost:3000> : la page affiche l'état du backend et du moteur, avec les
versions d'OpenFisca. Les ports se changent en copiant `.env.example` en `.env`.

## Lancer les tests

```bash
# Moteur (pytest, dans Docker : aucune installation locale de Python)
docker build --target test -t quinquennat-moteur-test moteur
docker run --rm quinquennat-moteur-test

# Backend (JUnit, Java 21 requis ; Maven est fourni par le Maven Wrapper)
cd backend && ./mvnw verify

# Frontend (Vitest, Node 24 requis)
cd frontend && npm ci && npm test
```

## Développement du frontend

```bash
cd frontend && npm run dev
```

Le serveur Vite relaie `/api` vers le backend sur `http://localhost:8080`.

## Licence

[AGPL-3.0](LICENSE), comme OpenFisca-France.
