# CivilisationUltimate 1.0 — serveur RP complet

Un seul JAR pour réunir les systèmes du serveur Civilisation.

## Inclus
- Grand Hub monumental existant + arrivée obligatoire au Hub
- Monde RP `world` séparé
- Économie : liquide + banque + paiements + historique + salaire quotidien
- `/banque` uniquement dans la zone bancaire
- Métiers avec candidature et validation staff (les joueurs ne peuvent pas simplement se donner un métier)
- Parcelles 4x4 chunks = 64x64 blocs, achat ou location 24h, protection
- Staff hiérarchisé : HELPER, MODO, ADMIN, SUPER_ADMIN, FONDA
- Panel Staff `/staff` et panel Admin `/civadmin`
- Modération : vanish, freeze, TP, kick, ban temporaire, validation métier, grades
- Sécurité RP : caméra, alarme, coffre-fort achetables
- Persistance YAML dans `plugins/CivilisationUltimate/core-data.yml`

## Commandes
`/civilisation` — Hub
`/civilisation rp` — monde RP
`/civilisation menu` — menu principal
`/banque` — banque en zone bancaire
`/job` — métiers
`/parcelle` — immobilier
`/pay <joueur> <montant>` — paiement liquide
`/staff` — panel staff
`/staff setrank <joueur> <grade>` — grade (SUPER_ADMIN requis)
`/staff jobapprove <joueur> <metier>` — valider un métier
`/staff freeze <joueur>`
`/staff vanish`
`/staff tp <joueur>`
`/staff kick <joueur>`
`/staff ban <joueur>`
`/eco give|take <joueur> <montant>`
`/securite` — boutique de sécurité RP
`/civadmin` — administration avancée

## Build
GitHub Actions utilise Java 25 + Gradle 9.1 et Paper API 26.2 build 121. Paper recommande Java 25 pour 26.2+ et la documentation actuelle utilise la coordonnée `26.2.build.+` pour l'API.

## Installation
Mettre le JAR généré dans `/home/container/plugins/` puis redémarrer.

**Sauvegarde conseillée avant migration.** Le Hub V3 est généré uniquement lorsque son marqueur de construction n'existe pas.
