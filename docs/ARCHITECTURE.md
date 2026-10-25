# Architecture — Molkky Board

> Validée le 2026-10-25 (Phase 2). Règles : [`RULES.md`](RULES.md) · moteur : [`SCORING_SPEC.md`](SCORING_SPEC.md).
> Décisions résumées dans `CLAUDE.md` § Décisions.

## Vue d'ensemble

```mermaid
flowchart TB
  app[app] --> features
  subgraph features[feature:*]
    home & setup & game & summary & history & stats & halloffame & players & settings & auth & join & groups
  end
  features --> data[core:data]
  features --> ui[core:ui] --> ds[core:designsystem]
  data --> stats[core:stats<br/>Kotlin pur] --> scoring
  data --> scoring[core:scoring<br/>Kotlin pur] --> model[core:model<br/>Kotlin pur]
  data --> db[(core:database<br/>Room)]
  data --> store[core:datastore]
  data --> net[core:network<br/>supabase-kt]
  net -. HTTPS / WSS .-> sb[(Supabase Paris<br/>Postgres + RLS + Realtime)]
```

## Flux d'un lancer en partie partagée

```mermaid
sequenceDiagram
  participant UI as Écran de jeu
  participant R as Room (source de vérité)
  participant S as SyncEngine (WorkManager)
  participant B as Supabase append_event()
  participant O as Autres téléphones
  UI->>R: insert event (PENDING, uuid)
  R-->>UI: Flow events → replay() → UiState (optimiste)
  S->>B: append_event(expected_seq = tête connue)
  alt tête à jour
    B-->>S: seq attribué
    S->>R: SYNCED + serverSeq
    B-->>O: Realtime (filtré par RLS)
  else SEQ_CONFLICT
    S->>B: select events where seq > lastSeq
    S->>R: insert événements distants
    S->>S: rejouer l'événement en attente
    alt encore valide
      S->>B: renvoi avec nouveau expected_seq
    else invalide (NOT_YOUR_TURN…)
      S->>R: REJECTED → snackbar
    end
  end
```

## Environnement de développement

Vérifié le 2026-10-25 : OpenJDK 21, Android SDK (`android-37.0`, build-tools 36), Android CLI, émulateur `Medium_Phone_API_37.0`. Gradle 9.8.2 (wrapper), AGP 9.4.1, Kotlin 2.4.21. CLI Supabase + Docker à installer au J6.

## 1. Socle — base suggérée conservée
- Kotlin + Compose + Material 3, une seule Activity, Navigation Compose à routes typées, MVVM/UDF
  (le ViewModel expose un `StateFlow<UiState>` et reçoit des `Action`s). Hilt, KSP.
- minSdk 26 ; targetSdk et compileSdk = dernière API stable, vérifiée au J0.
- **Challenge** : j'ajoute **WorkManager** (+ hilt-work) au catalogue, absent de la stack. Il
  garantit l'envoi des lancers saisis hors ligne même si l'app est tuée. Tout le reste de la stack
  demandée est repris tel quel.

## 2. Modules
```
build-logic/            convention plugins (android-library, compose, hilt, jvm-library, feature, test)
app/                    Activity, NavHost, injection globale
core:model              Kotlin pur — identifiants, Player, Team, GameConfig (types partagés)
core:scoring            Kotlin pur — moteur (reduce/fold), Mölkkout, Match ; dépend seulement de
                        core:model et kotlinx-serialization. Structuré pour passer en KMP plus tard
core:stats              Kotlin pur — résumés de partie et classements (Hall of Fame) calculés depuis les journaux
core:designsystem       thème Nordic Bold, tokens, typo, PinView, ScoreRing, PlayerChip, BigActionButton
core:ui                 composables métier partagés (grille de quilles, bandeau joueurs)
core:database           Room (entités, DAO, migrations exportées)
core:datastore          DataStore Preferences (réglages, préréglage de règles, « Plein soleil »)
core:network            [Phase 4] supabase-kt + Ktor OkHttp, DTO
core:data               repositories (source de vérité = Room) + SyncEngine [Phase 4]
core:testing            fakes, règles de test, données de test
feature:intro | home | setup | game | summary | history | stats | halloffame | players | settings
feature:auth | join | groups   [Phase 4]
```
Règles de dépendance : `feature:*` → `core:data`, `core:ui`, `core:designsystem` uniquement ; les
features ne dépendent jamais les unes des autres ; `core:scoring` et `core:stats` ne connaissent pas Android.

## 3. Moteur de score (`core:scoring`)
- `reduce(SetState, Event): Result<SetState, ScoringError>` est pure ; `replay(config, teams, log)`
  est l'unique façon d'obtenir un état.
- Le **journal est en ajout seul** : `Undo(targetId)` et `Correct(targetId, replacement)` sont
  eux-mêmes des événements. Le rejeu calcule d'abord le *journal effectif*, puis le plie. Cela rend la
  synchro triviale (on n'efface jamais rien côté serveur) et garde une **piste d'audit** des
  corrections.
- Un événement invalide au rejeu (par exemple un lancer arrivé après la fin) est **ignoré de façon
  déterministe** et signalé : tous les appareils convergent vers le même état.
- Tests : un test JUnit 5 paramétré par cas de `SCORING_SPEC.md` (identifiants P1…X7 dans le nom
  des tests), plus des tests de propriétés maison (journaux aléatoires valides : score toujours dans
  [0, cible], « ajout puis Undo = identité »). Couverture vérifiée par **Kover** (`koverVerify`, en CI) :
  100 % des lignes et ≥ 98 % des branches. Les classes `@Serializable` sans logique sont exclues de la mesure
  (code généré) ; les 5 branches restantes sont inatteignables (tests de nullité générés par Kotlin sur `turn`).

## 4. Données locales (offline-first)
- Tables Room : `player_profile` (profils locaux réutilisables), `game` (config sérialisée en JSON,
  statut, code de partage nullable), `game_team`, `game_player`, `game_event` (identifiant UUID
  généré sur l'appareil, `localSeq`, `serverSeq` nullable, `syncState` PENDING/SYNCED/REJECTED,
  payload JSON), `team` (identité d'équipe : nom, couleur, emblème, `member_key` unique, voir § 10).
  **Aucun résultat ni statistique n'est stocké en dur** (la table `set_result` prévue initialement est supprimée).
- L'état d'une partie n'est **pas stocké** : `Flow<List<GameEvent>>` → `replay()` → `UiState`
  (moins de 200 événements par manche, recalcul négligeable). Les stats et le Hall of Fame sont
  calculés par `core:stats` à partir des journaux (§ 10).
- Le mode « un seul téléphone » fonctionne sans compte ni réseau : les modules réseau sont injectés
  mais inactifs tant qu'on ne partage pas la partie.

## 5. Backend : recommandation **Supabase (région UE)**

| Critère | Supabase | Firebase |
|---|---|---|
| Hébergement UE | Projet entier (Auth incluse) dans une région UE : Francfort, Paris, Stockholm… | Firestore en UE possible, mais **Firebase Auth traite les données aux États-Unis** (passer à Identity Platform pour aller plus loin) |
| Modèle d'accès | **RLS Postgres** : SQL testable (pgTAP), versionné en migrations | Security Rules : langage dédié, bon émulateur, moins expressif pour les jointures de rôles |
| Journal ordonné | séquence par partie attribuée **dans une fonction SQL transactionnelle** : ordre total garanti | transactions Firestore possibles, mais compteur unique par partie, coût en lectures, logique côté client ou Cloud Functions |
| Temps réel | Realtime `postgres_changes`, **filtré par RLS** | listeners Firestore, très mûrs, avec un cache hors ligne intégré (gros atout) |
| Hors ligne | à notre charge : Room + boîte d'envoi (on le veut de toute façon, Room étant la source de vérité) | natif, mais doublonne avec Room |
| Réversibilité | open source, **auto-hébergeable** chez un hébergeur UE (Scaleway, OVH…) : sortie du CLOUD Act possible plus tard | enfermement propriétaire |
| Coûts / pièges | l'offre gratuite **met en pause un projet inactif depuis 7 jours** | gratuit généreux |

**Choix : Supabase en région UE, Paris (`eu-west-3`)**, pour quatre raisons : toutes les données restent en UE, RLS est
auditable et testable, l'ordre du journal est garanti par une transaction SQL, et l'auto-hébergement
reste possible. Le principal atout de Firebase (cache hors ligne) ne sert pas ici, puisque Room est
déjà la source de vérité. Risque résiduel : Supabase Inc. est une société américaine (même
exposition que Google) ; l'auto-hébergement est la porte de sortie.

### Schéma (migrations dans `supabase/migrations/`)
- `profiles(user_id PK → auth.users, display_name, color)` : rien d'autre (pas d'avatar distant en v1,
  les avatars restent locaux).
- `games(id, owner_id, config jsonb, status, join_code UNIQUE, join_code_expires_at, created_at)`.
- `game_members(game_id, user_id, role organizer|player)`.
- `game_players(game_id, player_id, team_id, order, display_name, user_id NULL)` : `user_id` est NULL
  pour un invité sans compte.
- `game_events(game_id, seq, id uuid UNIQUE, kind, payload jsonb, author_id, client_ts, server_ts)`.
- **Pas d'INSERT direct** : RLS n'autorise que le SELECT aux membres. Toute écriture passe par des
  fonctions `security definer` :
  - `append_event(game_id, event_id, expected_seq, kind, payload)`. Elle vérifie que l'auteur est
    membre ; qu'un `Throw` ou une `Fault` porte sur un joueur lié à l'auteur, sauf si l'auteur est
    organisateur (les invités sans compte sont saisis par l'organisateur) ; que `Undo` et `Correct`
    viennent de l'organisateur, ou de l'auteur du dernier lancer pour un Undo immédiat. Elle verrouille
    la partie (`SELECT … FOR UPDATE`) et compare `expected_seq` à la tête du journal ; si c'est bon,
    elle insère avec `seq + 1`. L'appel est idempotent sur `event_id`.
  - `create_shared_game(...)`, `join_game(code)` (avec limitation de débit), `leave_game`,
    `delete_my_account()`.
- Tests RLS avec pgTAP (`supabase test db`) : un non-membre ne lit rien ; un joueur ne saisit pas pour
  un autre ; un joueur ne corrige pas.

## 6. Synchro et conflits
- **Saisie** : l'événement est écrit dans Room (PENDING) et l'UI se met à jour tout de suite (état
  optimiste). Le `SyncEngine` (WorkManager, contrainte réseau) appelle ensuite
  `append_event(expected_seq = dernier serverSeq connu)`.
- **Conflit** (deux téléphones en même temps) : le serveur attribue l'ordre. Le perdant reçoit
  `SEQ_CONFLICT`, récupère les événements manquants, puis **rejoue** son événement en attente
  par-dessus :
  - s'il est encore valide (par exemple un lancer d'une autre équipe saisi dans le bon ordre), il est
    renvoyé avec le nouveau `expected_seq` ;
  - s'il est devenu invalide (le même lancer saisi deux fois → `NOT_YOUR_TURN`), il passe en
    REJECTED et un snackbar s'affiche : « Lancer déjà saisi par Bruno ».
- **Temps réel** : abonnement Realtime au canal `game_events:game_id=eq.X` tant que l'écran de jeu est
  ouvert.
- **Retour en ligne** : `select … where seq > lastSeq`, puis vidage de la boîte d'envoi. Room reste
  l'unique source de l'UI : le réseau ne fait qu'alimenter Room.

## 7. Comptes, invitation, rôles
- **Auth** : **code OTP à 6 chiffres reçu par e-mail** (le lien magique a besoin d'un deep link, reporté) et mot de passe en option ; Google via
  **Credential Manager** → `signInWithIdToken`. **Connexion anonyme** pour un invité qui veut suivre
  la partie sur son propre téléphone ; il peut convertir son compte plus tard.
- **Code de partie** : 6 caractères, alphabet de 31 symboles sans caractères ambigus
  (pas de 0/O, 1/I/L), soit environ 887 millions de combinaisons. Il expire à la fin de la partie ou
  après 24 h, avec limitation de débit sur `join_game`.
- **QR** : contient **uniquement le code** (texte `ABC123`), généré avec zxing et scanné avec CameraX
  et ML Kit (version embarquée) ; le scan remplit le champ « Rejoindre ». **Les liens d'invitation
  (`molkky://` ou App Links `https://`) sont reportés** : aucun intent-filter de deep link en v1, donc
  aucune surface d'entrée externe. À reprendre quand un domaine existera.
- **Rôles** : l'organisateur peut tout faire (corriger, saisir pour les invités, clore la partie) ; un
  joueur saisit ses propres lancers. Pas d'interface d'administration en v1.

## 8. Sécurité et RGPD (par conception)
- Données collectées : e-mail (Auth), pseudo, couleur, lancers. Ni localisation, ni contacts, ni
  analytics. Pas de Crashlytics (au besoin plus tard : ACRA auto-hébergé, sur consentement).
- Clé `anon` publique dans le `BuildConfig` (elle est faite pour ça) : la sécurité repose
  entièrement sur RLS et les RPC. Jamais de clé `service_role` dans l'app.
- Tokens stockés via le stockage chiffré de supabase-kt (EncryptedSharedPreferences / Keystore). Pas
  de logs en release (Timber sans arbre), règles R8 actives.
- Droit à l'effacement : `delete_my_account()` anonymise les `game_players` (le nom devient « Joueur
  supprimé ») et supprime le profil et le compte. Purge des parties partagées inactives après N mois
  (paramètre à fixer). Politique de confidentialité exigée par le Play Store, à rédiger au J9.
- `android:allowBackup` limité par des `dataExtractionRules`, permission CAMERA demandée au moment
  du scan uniquement, `network_security_config` en HTTPS seulement.

## 9. Jalons
| # | Jalon | Contenu | Arrêt pour validation |
|---|---|---|---|
| J0 | Squelette | build-logic, `libs.versions.toml` (versions stables vérifiées), app vide, ktlint, detekt, CI GitHub Actions (tests, lint, assembleDebug, APK en artefact) | — |
| J1 | Moteur | `core:model` et `core:scoring` en TDD sur SCORING_SPEC, tests de propriétés | rapport de tests |
| J2 | Design system | thème, tokens, polices, PinView, ScoreRing, PlayerChip, BigActionButton, `@Preview` clair/sombre/« Plein soleil » | **captures Roborazzi** (demandé) |
| J3 | Données locales | Room, DataStore, repositories, tests DAO (Robolectric) | — |
| J4 | Jouer | feature:setup (joueurs, équipes nommées avec couleur, emblème et nom au hasard 🎲, reprise d'une équipe existante, ordre par glisser-déposer, règles avancées) et feature:game (grille, aperçu « +7 → 38 », Raté/Valider, undo + snackbar, haptique, keepScreenOn) | démo APK |
| J5 | Fin du MVP | animation de victoire (Canvas, version réduite), feature:summary (classement, **revanche** : mêmes équipes, ordre inversé), history (déroulé des lancers), stats (Vico), `core:stats` + **Hall of Fame local** (classements testés), players, match en plusieurs manches et Mölkkout, **écran d'intro Yogi Studio** (feature:intro, Canvas, chargement en parallèle) et « À propos ». **Fin de la Phase 3** | APK |
| J6 | Backend | projet Supabase UE, migrations, RLS, RPC, tests pgTAP | revue du SQL / de RLS |
| J7 | Comptes | feature:auth (OTP, mot de passe, Google, anonyme), profil, suppression du compte | — |
| J8 | Partage | création partagée, code et QR (code seul), SyncEngine, Realtime, rattrapage hors ligne, gestion des conflits | test à deux téléphones |
| J9 | Rôles, groupes et durcissement | correction par l'organisateur, invités, **groupes et Hall of Fame de groupe**, animation de victoire synchronisée, limitation de débit, confidentialité. **Fin de la Phase 4** | — |
| J10 | Finitions et build | Baseline Profiles, audit d'accessibilité (TalkBack, contraste), APK debug, préparation de la release (Phase 5) | APK |
| J11 | Compléments optionnels | records secondaires, badges, carte de résultat partageable : **uniquement après validation** | oui, avant de commencer |

## 10. Équipes nommées, victoire, Hall of Fame et groupes

### Identité d'équipe
- `TeamIdentity.memberKey` = empreinte (SHA-256) de la liste **triée** des identifiants de joueurs. À la
  création d'une partie, si une équipe locale (ou du groupe) a le même `memberKey`, l'app propose de la
  reprendre (nom, couleur, emblème) ; sinon une nouvelle équipe est créée. Une équipe d'un seul
  joueur = ce joueur (pas de doublon dans l'onglet Équipes).
- Les identifiants de joueurs doivent être stables : profil local (UUID), compte (`user_id`) ou
  **invité de groupe** (UUID créé par un membre, nom seulement) pour que le palmarès s'accumule
  entre téléphones.
- Noms au hasard : liste d'environ 50 noms en ressource `strings` (`core:ui`), tirage sans
  répétition immédiate. Emblèmes : une petite banque de vecteurs dans `core:designsystem`.

### Statut d'une partie (dérivé du journal)
- Nouvel événement `Abandon` (organisateur ou téléphone local) → statut `ABANDONED`.
- `corrected = true` dès que le journal effectif contient un `Correct`. Un `Undo` du dernier lancer
  est une rectification de saisie, **pas** une correction.
- **Éligible aux records** = manche `FINISHED` ∧ ¬`ABANDONED` ∧ ¬`corrected`. Les autres restent dans
  l'historique avec un badge (« abandonnée », « corrigée »).

### `core:stats` (Kotlin pur, tout depuis les journaux)
- `summarize(game, effectiveLog) → GameSummary` (vainqueur, lancers par équipe et par joueur, date,
  éligibilité…) puis `rank(summaries, scope, period, teamSize?) → classements`. Rien n'est persisté :
  un nouveau classement s'applique rétroactivement à tout l'historique.
- Si les performances l'exigent un jour, un cache **jetable** clé = (gameId, dernier événement,
  `STATS_VERSION`) pourra être ajouté ; ce n'est jamais une source de vérité.
- Classements v1 : victoires (+ parties jouées, taux) ; **victoire la plus rapide** = nombre de lancers
  de l'équipe gagnante dans la manche (fautes comprises, Mölkkout exclu). 5 lancers = badge « partie
  parfaite ». **Seules les manches aux règles standard** (cible 50, dépassement → 25) entrent dans ce
  record ; le nombre de victoires compte toutes les manches éligibles. **Onglet Joueurs : parties en solo
  uniquement** ; les parties en équipe vont dans l'onglet Équipes (filtre par taille). Égalité → la date la plus ancienne garde le record, puis l'identifiant de partie pour
  rester déterministe.
- Filtres : périmètre (local / groupe), période (tout / année / mois, en fuseau local), taille d'équipe.
- « 🏆 Nouveau record ! » : à la fin de la manche, comparaison des classements avant et après
  l'ajout de la partie.
- Tests : une table de cas dans `docs/STATS_SPEC.md` (à écrire au J5), y compris les égalités et
  l'exclusion des parties corrigées ou abandonnées.

### Animation de victoire
- Déclenchée quand l'UiState passe à `FINISHED` **en direct**. À la réouverture d'une partie déjà
  finie, on affiche l'écran de fin sans animation.
- Partie partagée : chaque téléphone la déclenche à la réception, via Realtime, de l'événement
  gagnant (décalage < 1 s en pratique ; pas d'horloge commune). Un téléphone hors ligne qui rattrape
  plus tard l'événement affiche le résultat sans animation, si l'événement date de plus de 30 s.
- Canvas Compose (confettis en forme de quilles, ressorts), haptique, son optionnel désactivé par
  défaut (`SoundPool`, réglage dans DataStore), fondu simple si « Réduire les animations » est actif.

### Groupes (Phase 4)
- Supabase : `groups(id, name, created_by)`, `group_members(group_id, user_id, role admin|member)`,
  `group_guests(group_id, guest_id, display_name)`, `games.group_id NULL`, `teams(group_id, member_key,
  name, color, emblem)`.
- RLS : un membre du groupe **lit** les parties, événements et équipes du groupe ; il n'écrit que
  dans les parties dont il est membre (inchangé). Pour rejoindre un groupe, même mécanisme qu'une
  partie : code à 6 caractères + RPC `join_group` avec limitation de débit.
- Le Hall of Fame du groupe est calculé **sur chaque téléphone** par `core:stats`, après
  synchronisation incrémentale (par `seq`) des journaux des parties du groupe. Il n'y a pas de calcul
  côté serveur, donc pas de duplication du moteur.
- RGPD : quitter un groupe supprime l'accès ; la suppression du compte anonymise les joueurs ; un
  admin peut supprimer un invité de groupe (son nom est remplacé par « Joueur supprimé »).

## 11. Préparation du backlog avancé (sans l'implémenter)

Le backlog (`CLAUDE.md`) ne démarre qu'après le MVP. Dès maintenant, quelques choix peu coûteux évitent une refonte :

- `GameConfig` accepte des **surcharges par équipe** (score de départ, cible, exemption d'élimination) pour le mode handicap ; un drapeau dérivé `handicap` exclut des records « purs ».
- Le jeu de quilles et la grille passent par une abstraction de skin ; les couleurs d'équipe sont toujours accompagnées de l'emblème (daltonisme).
- Aucune chaîne en dur : tout passe par `strings.xml` (fr par défaut, en/fi plus tard).
- `game.place` (texte libre, facultatif) prévu dans le schéma.
- Un `GameSession` partagé (Flow d'UiState) servira à la notification, au widget et au tableau d'affichage.
- Tous les futurs classements (Elo, face-à-face, carte de précision) se branchent sur `core:stats`.
- Lien spectateur et liens d'invitation nécessitent un domaine : reportés ensemble.

## Points ouverts (non bloquants, à trancher au jalon indiqué)
- ~~Liens d'invitation~~ : reportés après la v1 ; on rejoint par code ou QR contenant le code (décidé le 2026-10-25).
- ~~Région Supabase~~ : **Paris (`eu-west-3`)** (décidé le 2026-10-25).
- **Durée de conservation** des parties partagées (J9).
- ~~Fin de la Phase 5~~ : configuration du build release signé (prompt complété le 2026-10-25).

## Vérification par jalon

- `./gradlew test detekt ktlintCheck assembleDebug` au vert en local et en CI.
- J2 : `./gradlew recordRoborazziDebug`, captures validées par le porteur du projet.
- J6 : `supabase test db` (pgTAP, RLS).
- J8 : scénario à deux appareils — saisie simultanée, mode avion puis rattrapage.
