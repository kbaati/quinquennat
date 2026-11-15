# Bilan du projet « quinquennat » — à évaluer

Document de passation, écrit le 13 novembre 2026 (projet mis en pause le 15 novembre 2026) par l'agent qui a construit le MVP, à destination d'un autre
agent chargé de l'évaluer. Il contient : ce qui a été demandé, ce qui existe, ce qui n'a pas été vérifié, les
questions restées ouvertes, et l'avis du porteur du projet sur le résultat.

## 1. Ce qu'on vous demande d'évaluer

Le porteur du projet (Khaled, développeur backend Java senior, seul sur le projet) n'est **pas satisfait du
résultat**. Ses mots : le rendu est « basique », et il ne sait pas « si c'est vraiment une bonne idée » ni si
« les gens vont l'utiliser ».

Il attend donc un avis sur trois points :

1. **L'idée** : un tel outil a-t-il une chance d'être utilisé pendant la campagne présidentielle d'avril 2027 ?
   Par qui, et pourquoi reviendrait-on dessus ou le partagerait-on ?
2. **Le rendu** : qu'est-ce qui le rend « basique », et que faudrait-il changer en priorité ?
3. **La suite** : continuer tel quel, changer d'angle, ou arrêter. Et répondre aux questions ouvertes de la
   section 7.

## 2. Le projet en une phrase

Un site public français, « Programme en bac à sable » : l'utilisateur règle quelques leviers fiscaux et
sociaux, puis voit l'effet en euros par an sur une vingtaine de foyers types (calcul OpenFisca-France), avec un
témoignage fictif par foyer. Objectif : être en ligne avant la campagne d'avril 2027.

## 3. Contraintes imposées par le porteur (non négociables)

- **Neutralité** : aucun parti, aucun candidat, aucun jugement de valeur, dans l'interface, le code et les prompts.
- **Pas un sondage** : jamais d'intention de vote ; réactions étiquetées partout « témoignage fictif généré par IA ».
- **Honnêteté sur la portée** : « foyers types », jamais « échantillon représentatif » ; pas de coût budgétaire
  national en v1.
- **Transparence** : page Méthode, chaque chiffre traçable.
- **Aucune donnée personnelle** : pas de compte, pas de cookie de suivi, pas d'analytics tiers.
- **Vérifier plutôt que deviner** les variables et paramètres OpenFisca.
- **Accord préalable** pour toute dépense, tout service payant, toute action irréversible, tout déploiement.
- Secrets en variables d'environnement uniquement ; modèle et clé Anthropic jamais en dur.
- Code et commits en anglais ; interface, README et page Méthode en français.
- Architecture imposée : monorepo `moteur/` (Python, FastAPI, OpenFisca), `backend/` (Java 21, Spring Boot,
  Maven), `frontend/` (React, Vite, TypeScript), un `docker-compose.yml`.

Ces contraintes pèsent sur l'attrait du produit : voir la section 8.

## 4. Où est le code

- Dépôt public : https://github.com/kbaati/quinquennat (AGPL-3.0)
- `main` contient les jalons J0 à J5 (PR 1 à 6 fusionnées, CI verte). Rien n'est déployé.
- Lancer : `docker compose up --build`, puis http://localhost:3000. Rien n'est déployé.

## 5. Ce qui existe

| Jalon | Contenu | État |
|---|---|---|
| J0 | Squelette, Docker, CI GitHub Actions | Fusionné |
| J1 | Moteur : `POST /simuler` calcule avant/après pour chaque foyer | Fusionné |
| J2 | Backend : leviers, validation, simulations stockées, limite de débit | Fusionné |
| J3 | Interface : pages Composer et Résultats, lien de partage | Fusionné |
| J4 | Témoignages fictifs | Fusionné |
| J5 | Page Méthode, navigation | Fusionné |

**Parcours utilisateur.** Page Composer : 9 curseurs en 5 groupes (seuil et 4 taux du barème de l'impôt sur
le revenu, CSG déductible sur l'activité, RSA, prime d'activité, base des allocations familiales). Bouton
« Simuler ». Page Résultats : rappel du programme, phrase « N foyers y gagnent, N y perdent », graphique en
barres horizontales des gains et pertes, tableau détaillé, témoignages fictifs, lien de partage `/s/<id>`.
Page Méthode : ce que l'outil est et n'est pas, mode de calcul, leviers avec leur paramètre OpenFisca, foyers
types, limites, données personnelles.

**Données.** 23 foyers types (`donnees/foyers.json`), 9 leviers (`donnees/leviers.json`), année simulée 2025.
Montants calculés : revenu disponible, impôt sur le revenu, CSG, RSA, prime d'activité, allocations familiales,
aides au logement.

**Technique.**
- Moteur : Python 3.11, FastAPI, OpenFisca-France 176.1.10, OpenFisca-Core 44.7.1. Réforme paramétrique,
  tous les foyers dans une seule simulation vectorisée, caches en mémoire.
- Backend : Java 21, Spring Boot 4.1.2, PostgreSQL 17 (H2 en test), Flyway. API : `GET /api/leviers`,
  `GET /api/foyers`, `POST /api/simulations`, `GET /api/simulations/{id}`,
  `GET /api/simulations/{id}/reactions`, `GET /api/sante`, OpenAPI sur `/api/openapi`.
- Témoignages : interface `ReactionGenerator`, une implémentation factice (phrases gabarit, aucun appel IA,
  active par défaut) et une implémentation Anthropic (SDK Java officiel, sortie structurée). Cache en base par
  empreinte (générateur, programme, foyer), plafond de 2 000 générations par jour, interrupteur
  `REACTIONS_ENABLED`. Le prompt ne contient que le foyer et ses montants en euros.
- Frontend : React 19, Vite, TypeScript, sans bibliothèque de routage ni de graphiques. CSS écrit à la main,
  thème clair et sombre, une colonne de 46 rem.

**Tests** (tous verts en CI) : 16 pytest, 33 JUnit dont un parcours de bout en bout, 9 Vitest.

**Performance mesurée en local.** Première simulation après démarrage : une dizaine de secondes. Nouvelle
combinaison de leviers : environ 4 secondes. Combinaison déjà calculée : quasi instantané.

## 6. Ce qui n'a pas été vérifié ou reste faible

- **L'implémentation Anthropic n'a jamais appelé la vraie API** (pas de clé). Elle compile, c'est tout. Les
  témoignages visibles aujourd'hui sont des phrases gabarit répétitives, pas de l'IA.
- **Les montants des foyers ne sont pas recoupés** un par un avec un simulateur officiel. Ils sont plausibles
  et couverts par des tests à larges fourchettes.
- **Aucun test avec de vrais utilisateurs.** Personne d'autre que le porteur n'a vu le site.
- **Aucun travail de design** au-delà d'une mise en page sobre : pas d'identité visuelle, pas d'illustration,
  pas de page d'accueil, pas d'aperçu de partage pour les réseaux sociaux.
- **4 secondes d'attente** par nouvelle simulation, sans retour visuel autre qu'un bouton « Calcul en cours… ».
- Le levier CSG ne porte que sur la part déductible (6,8 %).
- Erreur de l'agent sur J0 : la PR 1 a été fusionnée alors que la CI backend était rouge ; corrigé en J1.
- L'interface s'appelle `ReactionGenerator` et non `GenerateurDeReactions` comme dans le cahier des charges
  (règle « noms de classes en anglais »).

## 7. Questions ouvertes posées au porteur, sans réponse

1. ~~Fusionner la PR 6~~ : fait, à la demande du porteur, qui a ensuite mis le projet en pause.
2. **Hébergement** : deux options chiffrées, aucune retenue.
   - Option A, un seul VPS avec le `docker-compose` actuel (Hetzner CX23 ou OVH VPS-1) : environ 8 à 10 € par
     mois. Sauvegardes et administration à la main. Recommandée par l'agent.
   - Option B, Scaleway managé (Serverless Containers et PostgreSQL managé) : environ 50 € par mois avec le
     moteur toujours démarré, 12 à 15 € s'il s'endort (mais 15 à 30 secondes de réveil).
   - Ces prix viennent surtout de comparateurs, pas des grilles officielles.
3. **Spring Boot 4.1.2 ou retour en 3.5** ? Spring Initializr ne proposait plus la 3.x ; resté en 4.1.2 par défaut.
4. **Clé et modèle Anthropic** : faut-il activer les vrais témoignages, et avec quel modèle (le coût en dépend) ?
5. **Nom de l'interface** : garder `ReactionGenerator` ou revenir au nom français ?

## 8. Avis de l'agent qui a construit le MVP

À prendre comme un point de vue, pas comme une conclusion : rien de ce qui suit n'a été testé auprès d'utilisateurs.

- **Le MVP est solide techniquement et mince comme produit.** Le cahier des charges décrivait des jalons
  d'ingénierie ; aucun ne portait sur l'attrait, le design ou la diffusion. Le résultat reflète cela.
- **Le rendu basique est réel et s'explique** : une colonne de curseurs, un graphique, un tableau. Rien ne
  donne envie d'essayer avant d'avoir cliqué, et rien ne se partage bien (le lien mène à un tableau).
- **L'utilisateur ne se voit pas dans l'outil.** Il regarde 23 foyers types, pas le sien. « Et moi, combien ? »
  est sans doute la question qui fait revenir, et l'outil n'y répond pas. Y répondre sans collecter de données
  est possible (calcul sans stockage), mais ce n'est pas dans le périmètre actuel.
- **Les leviers sont abstraits.** « Taux de la 2e tranche » parle peu. Des programmes préréglés seraient plus
  parlants, mais la règle de neutralité interdit de les rattacher à des candidats, ce qui retire l'accroche la
  plus évidente en période de campagne.
- **Les témoignages fictifs sont le point le plus fragile.** En version gabarit ils sont répétitifs ; en
  version IA ils coûtent, n'ont jamais été testés, et un texte inventé à côté de chiffres sourcés peut affaiblir
  la crédibilité de l'ensemble plutôt que la renforcer.
- **Des simulateurs existent déjà** (simulateurs officiels, outils bâtis sur OpenFisca, comparateurs de
  programmes publiés par la presse à chaque campagne). L'agent n'a fait aucune étude de l'existant : c'est à
  vérifier avant de décider.
- **Ce qui est réutilisable quelle que soit la décision** : le moteur de réforme paramétrique, le catalogue de
  foyers et de leviers, le stockage des simulations partageables, la chaîne de tests et la CI.

## 9. Pistes à évaluer (aucune n'est engagée)

- Tester l'idée avant d'investir davantage : montrer le site à 5 à 10 personnes hors du milieu technique et
  observer ce qu'elles font.
- Ajouter « mon foyer » : quelques questions, un calcul, rien de stocké.
- Refaire la page Résultats autour d'une image partageable et d'un message en une phrase.
- Réduire ou retirer les témoignages fictifs.
- Travail de design (identité, page d'accueil, mobile d'abord).
- Arrêter ici et garder le moteur pour un autre usage.
