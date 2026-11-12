# Quinquennat

Un bac à sable pour composer un programme fiscal et social, puis voir son effet sur le budget
d'une vingtaine de foyers types, calculé avec les règles socio-fiscales françaises
([OpenFisca-France](https://github.com/openfisca/openfisca-france)).

> **État : jalons J0 à J4.** On peut composer un programme, le simuler, lire des témoignages fictifs
> et partager le résultat. Restent à venir : la page Méthode et la mise en ligne (J5).

## Ce que l'outil est, et ce qu'il n'est pas

- Il calcule un avant / après en euros par an pour des **foyers types**. Ce n'est pas un
  échantillon représentatif de la population, et il ne chiffre pas de coût budgétaire national.
- Ce n'est **pas un sondage** : aucune intention de vote, aucune estimation d'opinion.
  Les réactions affichées sont des **témoignages fictifs**, étiquetés comme tels partout.
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

Puis ouvrir <http://localhost:3000>, régler quelques leviers et cliquer sur « Simuler ». La première
simulation prend quelques secondes (chargement de la législation). Les ports se changent en copiant `.env.example` en `.env`.

## Témoignages fictifs

Pour chaque foyer type dont le budget change, l'outil affiche un court témoignage fictif, un ressenti
de −2 à +2 et la raison chiffrée. Le texte est écrit à partir du foyer et de son écart en euros,
rien d'autre : ni le programme, ni aucun contexte politique ne sont transmis au générateur.

| Variable               | Rôle                                                                 |
| ---------------------- | -------------------------------------------------------------------- |
| `REACTIONS_ENABLED`    | `false` désactive entièrement les témoignages                        |
| `REACTIONS_GENERATOR`  | `fake` (défaut : phrases d'exemple, aucun appel à une IA) ou `anthropic` |
| `REACTIONS_MAX_PER_DAY`| Plafond de témoignages générés par jour (défaut : 2000)              |
| `ANTHROPIC_API_KEY`    | Clé API, requise avec `anthropic`. Jamais dans le dépôt              |
| `ANTHROPIC_MODEL`      | Identifiant du modèle, requis avec `anthropic`                       |

Chaque témoignage est généré une seule fois par couple (programme, foyer), puis relu depuis la base.
L'étiquette affichée dit comment le texte a été produit : « généré par IA » ou « texte d'exemple automatique ».

## Lancer les tests

```bash
# Moteur (pytest, dans Docker : aucune installation locale de Python)
docker build -f moteur/Dockerfile --target test -t quinquennat-moteur-test .
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
