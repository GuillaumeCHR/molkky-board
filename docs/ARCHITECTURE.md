# Architecture — Molkky Board

> Validée le 2026-10-25 (Phase 2). Règles : [`RULES.md`](RULES.md) · moteur : [`SCORING_SPEC.md`](SCORING_SPEC.md).
> Décisions résumées dans `CLAUDE.md` § Décisions.
> **Révision du 2026-10-28 (spec « Partie à côté », phase 3 bis) : en attente de validation.** Nouveautés : § 6 bis
> (`core:sync`), § 7 (QR code), § 9 (jalons), points ouverts. Modèle de menaces : [`security/THREAT_MODEL.md`](security/THREAT_MODEL.md).

## Vue d'ensemble

```mermaid
flowchart TB
  app[app] --> features
  subgraph features[feature:*]
    home & setup & game & summary & history & stats & halloffame & players & settings & auth & join & groups
  end
  features --> data[core:data]
  data --> sync[core:sync<br/>Kotlin pur : protocole,<br/>validation, hôte, invité]
  sync --> scoring
  data --> lan[core:sync-lan<br/>JVM : WebSocket + TLS<br/>épinglage]
  data --> droid[core:sync-android<br/>point d'accès, Keystore,<br/>service de premier plan]
  lan -. WSS sur réseau local .-> peers[(Autres téléphones<br/>de la partie)]
  features --> ui[core:ui] --> ds[core:designsystem]
  data --> stats[core:stats<br/>Kotlin pur] --> scoring
  data --> scoring[core:scoring<br/>Kotlin pur] --> model[core:model<br/>Kotlin pur]
  data --> db[(core:database<br/>Room)]
  data --> store[core:datastore]
  data --> net[core:network<br/>Phase 4, à trancher]
  net -. HTTPS / WSS .-> sb[(Serveur distant<br/>Phase 4)]
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
core:designsystem       thème Nordic Bold, tokens, typo, PinView, QuickPad, ScoreRing, PlayerChip, BigActionButton,
                        FitToScreen, logotype, logo Yogi Studio
core:ui                 composables métier partagés (grille de quilles, bandeau joueurs)
core:database           Room (entités, DAO, migrations exportées)
core:datastore          DataStore Preferences (réglages, préréglage de règles, « Plein soleil »)
core:sync               [3 bis] Kotlin pur — protocole versionné, fonction de validation partagée, logique d'hôte
                        (numérotation, idempotence, rattrapage) et d'invité, abstraction `Transport`
core:sync-lan           [3 bis] JVM — transport réseau local : serveur et client WebSocket sur TLS, épinglage
                        de l'empreinte ; testable sur la boucle locale en JVM
core:sync-android       [3 bis] Android — point d'accès local, `WifiNetworkSpecifier`, liaison au `Network`,
                        certificat de partie (Keystore), service de premier plan `connectedDevice`, permissions
core:network            [Phase 4] client du serveur distant (même protocole `core:sync`, autre transport)
core:data               repositories (source de vérité = Room) + SyncEngine [Phase 4]
core:share              [J6] rendu des vignettes (Compose → bitmap) et encodage des mini-vidéos
core:testing            fakes, règles de test, données de test
feature:intro | home | setup | game | summary | history | stats | halloffame | players | settings | share | rules
feature:nearby          [3 bis] « Inviter » (QR, demandes, téléphones connectés) et « Rejoindre » (scan
                        CameraX + zxing-cpp)
feature:auth | groups   [Phase 4]
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
- **Moteur v2** (validé le 2026-10-25, détail dans `SCORING_SPEC.md` § 15) :
  - un lancer est **attribué par sa position** dans le journal effectif (le lanceur du moment) ; le champ
    `by` n'est vérifié qu'à la saisie (`checkInput`). Supprimer un doublon réattribue la suite aux bons joueurs ;
  - `ThrowResult = Pins | Points` : mode « Quilles » (stats par quille) et mode « Rapide » ;
  - nouveaux événements `Delete`, `AddPlayer`, `RemovePlayer`, `Reorder` (entre deux tours) ;
  - les règles sont une **stratégie** `ScoringRules` (implémentation `StandardRules(config)`) : de futures
    variantes de jeu se brancheront sur le même journal ;
  - `modifiedAfterFinish` remplace `corrected` pour l'éligibilité aux records.
- Tests : un test JUnit 5 paramétré par cas de `SCORING_SPEC.md` (identifiants P1…X7 dans le nom
  des tests), plus des tests de propriétés maison (journaux aléatoires valides : score toujours dans
  [0, cible], « ajout puis Undo = identité »). Couverture vérifiée par **Kover** (`koverVerify`, en CI) :
  100 % des lignes et ≥ 97 % des branches (moteur v2 : 97,5 %). Les classes `@Serializable` sans logique sont
  exclues de la mesure (code généré) ; les branches restantes sont inatteignables (tests de nullité générés par
  Kotlin, cas « liste vide » de `minOf` alors qu'une manche en cours a au moins deux équipes en lice).

## 4. Données locales (offline-first)
- Tables Room : `player_profile` (profils locaux réutilisables), `game` (config sérialisée en JSON,
  statut, code de partage nullable), `game_team`, `game_player`, `game_event` (identifiant UUID
  généré sur l'appareil, `localSeq`, `serverSeq` nullable, `syncState` PENDING/SYNCED/REJECTED,
  payload JSON), `team` (identité d'équipe : nom, couleur, emblème, `member_key` unique, voir § 10).
  **Aucun résultat ni statistique n'est stocké en dur** (la table `set_result` prévue initialement est supprimée).
- L'état d'une partie n'est **pas stocké** : `Flow<List<GameEvent>>` → `replay()` → `UiState`
  (moins de 200 événements par manche, recalcul négligeable). Les stats et le Hall of Fame sont
  calculés par `core:stats` à partir des journaux (§ 10).
- **Aucune partie perdue** : chaque événement validé est écrit dans Room avant que l'UI n'avance (transaction
  courte). Au démarrage, `StartupLoader` détecte une partie en cours et l'accueil propose de la reprendre ;
  test instrumenté dédié (mort du processus, redémarrage du téléphone).
- Réglages (DataStore) : mode de saisie, langue, « Plein soleil », annonces vocales, sons, coach, intro courte,
  compteur de lancements, prise en main vue.
- Le mode « un seul téléphone » fonctionne sans compte ni réseau : les modules réseau sont injectés
  mais inactifs tant qu'on ne partage pas la partie.
- **Changement imposé par la partie à côté** : l'APK devra déclarer `INTERNET` (sockets, même locales). La
  garantie « aucune requête réseau » ne repose donc plus sur l'absence de la permission mais sur un test
  (voir § 6 bis, « Ce qui est testable ») ; la phrase d'À propos « n'a même pas accès à Internet » devient fausse
  (point ouvert « Vie privée »).

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

### Souveraineté et auto-hébergement (complément du 2026-10-27)

Héberger en région UE chez un fournisseur soumis au droit américain **n'équivaut pas** à un fournisseur
européen : Supabase Inc. (États-Unis) et son infrastructure (AWS) relèvent du **CLOUD Act**, qui permet aux
autorités américaines d'exiger des données quel que soit le lieu de stockage. Firebase (Google) a la même
exposition, aggravée par Firebase Auth traité aux États-Unis.

| Option | Souveraineté | Coût mensuel (ordre de grandeur, **à vérifier au J8**) | Exploitation |
|---|---|---|---|
| Supabase Cloud, Paris | données en UE, **exposées au CLOUD Act** | gratuit (projet mis en pause après une semaine sans activité, sans sauvegarde à restaurer) ; offre payante ≈ 25 $ | nulle : mises à jour, sauvegardes, TLS gérés |
| **Supabase auto-hébergé** chez Scaleway ou OVHcloud (sociétés françaises) | hors CLOUD Act pour le fournisseur ; c'est toi l'hébergeur | VPS 2 vCPU / 4 Go ≈ 10 à 20 €, + stockage objet pour les sauvegardes chiffrées ≈ 1 à 3 €, + envoi d'e-mails (OTP) via un service UE (palier gratuit chez plusieurs) | réelle : Docker Compose (Postgres, GoTrue, Realtime, Kong…), mises à jour de sécurité, sauvegardes et **test de restauration**, supervision, certificats ; compter **2 à 4 h par mois**, plus les incidents |
| Postgres managé UE + API maison | maximale | similaire | développement de l'auth et du temps réel à notre charge : écarté |

**Recommandation** : développer les jalons J8 à J11 sur **Supabase Cloud Paris** (aucune exploitation, on
valide le produit), en n'utilisant que des briques portables (Postgres standard, RLS, RPC SQL, supabase-kt). Avant
l'ouverture au public, **basculer sur un Supabase auto-hébergé chez Scaleway ou OVHcloud** si l'engagement
« hébergé en Europe, hors CLOUD Act » doit figurer dans l'encart « Vie privée » ; sinon, rester sur le cloud
et l'écrire honnêtement (« en Europe, chez un sous-traitant américain »). La bascule ne touche que l'URL et
la clé `anon` de l'app, les migrations et les données. **Décision à prendre avec toi au J8.**

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
- **Corrections** : `Correct`, `Delete` et les événements de joueurs sont des événements comme les autres,
  ajoutés au journal serveur. Chaque téléphone les reçoit, rejoue le journal et compare l'ancien et le nouvel
  état : un lancer passé modifié affiche « Lancer de Léa corrigé » ; si le vainqueur change, l'écran de fin se
  referme et la partie reprend partout.
- Le contrôle « c'est bien mon tour » se fait **à la saisie** et au rejeu de la boîte d'envoi après un
  `SEQ_CONFLICT` (le lanceur attendu a-t-il changé ?), pas au rejeu du journal (attribution par position).
- **Temps réel** : abonnement Realtime au canal `game_events:game_id=eq.X` tant que l'écran de jeu est
  ouvert.
- **Retour en ligne** : `select … where seq > lastSeq`, puis vidage de la boîte d'envoi. Room reste
  l'unique source de l'UI : le réseau ne fait qu'alimenter Room.

## 6 bis. Partie à côté : `core:sync` (phase 3 bis, proposition du 2026-10-28)

Le téléphone de l'organisateur devient l'**autorité** : il valide, numérote, enregistre et diffuse chaque
événement, exactement le rôle prévu pour le serveur en phase 4. Les autres téléphones ne font que proposer.

### Flux proposition → validation → numérotation → diffusion

```mermaid
sequenceDiagram
  participant G as Invité (UI + Room)
  participant T as Transport (WSS local)
  participant H as Hôte : Authority (core:sync)
  participant R as Room de l'hôte
  participant O as Autres invités
  G->>T: EVENT_PROPOSAL(eventId, baseSeq, contenu)
  T->>H: (session authentifiée : appareil, rôle, joueur associé)
  H->>H: déjà vu ? (eventId) → renvoyer le résultat mémorisé
  H->>H: validate(journal, proposition, droits) — fonction pure
  alt accepté
    H->>R: écrire avec seq = tête + 1 (transaction)
    H-->>G: EVENT(seq, eventId, appareil auteur, contenu, horodatage)
    H-->>O: EVENT(...)
    G->>G: Room ← EVENT (dans l'ordre des seq) → replay() → UI, animations
  else refusé
    H-->>G: EVENT_REJECTED(eventId, NOT_YOUR_TURN | FORBIDDEN | INVALID | STALE)
  end
```

- **Une seule porte d'entrée** : les saisies de l'organisateur passent par la même `Authority`, via un transport
  en mémoire. L'hôte n'a donc pas de chemin privilégié qui échapperait à la validation.
- **Pas d'écriture optimiste chez l'invité** : il n'écrit dans Room que les `EVENT` reçus, dans l'ordre des numéros
  (sur un réseau local, l'aller-retour se compte en millisecondes). Pendant l'attente, le bouton « Valider »
  affiche un état d'envoi ; en cas de refus, un message (« Ce n'est plus le tour de Léa »). Ce choix supprime la
  boîte d'envoi et le rejeu après conflit prévus au § 6 pour le serveur ; il sera réexaminé pour le jeu à
  distance, où la latence est plus forte.
- **Numéro d'ordre** : `seq` par manche, attribué par l'hôte. Il est stocké dans la colonne `serverSeq` existante
  de `game_event` (renommée `authoritySeq` à la migration) ; l'invité ordonne son journal par ce numéro.
- **`baseSeq` et `STALE`** : la proposition indique le dernier numéro que l'invité connaissait. La validation se
  fait toujours sur le journal **courant** de l'hôte. Un lancer reste acceptable s'il est encore valide (c'est
  bien le tour de ce joueur) ; une correction ou une suppression visant un état dépassé (`baseSeq` en retard et
  cible modifiée entre-temps) est refusée en `STALE`.
- **Idempotence** : l'hôte garde, pour la partie, la table `eventId → seq | code de refus`. Une proposition
  renvoyée après une coupure reçoit la même réponse, sans être appliquée deux fois. L'`EventId` (UUID) existe
  déjà dans le moteur.
- **Rattrapage** : `RESUME(jeton de session, dernier seq reçu)` → l'hôte renvoie les `EVENT` manquants (ou le
  journal complet si l'invité est trop en retard), sans nouvelle validation humaine. Un trou dans les numéros
  côté invité déclenche la même demande.
- **Corrections** : `Correct`, `Delete`, `Undo` et la gestion des joueurs sont des événements comme les autres.
  Chaque téléphone rejoue le journal et compare l'état avant et après : « Lancer de Léa corrigé », écran de fin
  qui se referme si le vainqueur change. Les **animations sont déduites du changement d'état** et non plus de
  l'action locale (voir adaptations).
- **Actions de match** : manche suivante, lancers et annulation du Mölkkout, revanche et abandon deviennent des
  contenus de proposition (aujourd'hui ce sont des écritures directes dans Room, hors journal).
- **Droits** (dans `validate`) : un invité saisit pour le joueur associé à son téléphone et peut corriger ou
  annuler **son dernier lancer** ; l'organisateur peut tout corriger, gérer les joueurs, exclure un téléphone.
  Bornes : 64 Ko par message, 12 téléphones, textes bornés et filtrés (`withoutUnsafeChars`), débit limité par
  téléphone.
- **Format** : JSON (kotlinx-serialization, déjà utilisé pour le journal), champ `v` de version, messages en
  `sealed interface` sérialisée. CBOR n'apporterait qu'un gain de taille négligeable ici, et le module CBOR de
  kotlinx-serialization garde des API expérimentales : **proposition JSON**, à confirmer.

### Où vit quoi

| Élément | Module | Testable en JVM pur |
|---|---|---|
| Messages, versions, (dé)sérialisation | `core:sync` | oui |
| `validate(journal, proposition, appareil)` : droits, tour, cohérence, `STALE` (réutilise `checkInput` et `replay`) | `core:sync` | oui |
| `HostSession` (numérotation, idempotence, rattrapage, appareils, jetons) et `GuestSession` (ordre, trous, reconnexion) | `core:sync` | oui |
| Serveur et client WebSocket, TLS, gestionnaire de confiance qui épingle l'empreinte | `core:sync-lan` | oui, hôte + 2 invités sur `127.0.0.1` avec un vrai TLS |
| Point d'accès local, `WifiNetworkSpecifier`, liaison des sockets au `Network`, mode « même Wi-Fi » | `core:sync-android` | non : deux téléphones réels |
| Certificat de partie (Keystore), stockage chiffré des jetons de session | `core:sync-android` | non (Robolectric partiel) |
| Service de premier plan `connectedDevice` (= notification de partie en cours de l'hôte) | `core:sync-android` | non |
| Écrans « Inviter » et « Rejoindre », scan | `feature:nearby` | Robolectric + Roborazzi |

### Choix techniques vérifiés le 2026-10-28, et ceux qui restent ouverts

- **Le serveur Ktor CIO ne gère pas TLS** : son code lève `UnsupportedOperationException("CIO Engine does not
  currently support HTTPS…")` dès qu'un connecteur HTTPS est configuré. La spec (« serveur Ktor CIO en WebSocket sur
  TLS ») n'est donc **pas réalisable telle quelle**. Options à départager au prototype :
  1. **Ktor, moteur Netty**, qui gère `sslConnector` : même code serveur qu'en phase 4, mais Ktor ne liste Netty
     que pour la JVM (Android non documenté) et l'APK grossit ; à mesurer ;
  2. **`SSLServerSocket` de la plateforme (Conscrypt) + trames WebSocket de Ktor** (`ktor-websockets`, sans moteur
     HTTP) : la poignée de main HTTP « Upgrade » s'écrit à la main (≈ 100 lignes à tester sévèrement), TLS reste
     standard ;
  3. en secours, **Java-WebSocket** (MIT) sur un `SSLContext` de la plateforme ; dernière version 1.6.0, en
     décembre 2024 : maintenance faible.
- **Client** : préférer **Ktor client avec le moteur OkHttp** (Apache 2.0) au moteur CIO. OkHttp utilise le TLS de la
  plateforme (Conscrypt, TLS 1.3 à partir d'Android 10) et accepte un `X509TrustManager` d'épinglage ; le moteur CIO
  s'appuie sur l'implémentation TLS propre à Ktor (`ktor-network-tls`), dont la prise en charge de TLS 1.3 reste
  à vérifier, et qui ressemble trop à de la « crypto maison ».
- **TLS 1.3 sous Android 8 et 9** : le Conscrypt intégré ne fait que TLS 1.2 sur ces versions. Il faut soit
  embarquer `conscrypt-android` 2.5.3 (dernière version, août 2024, bibliothèques natives : quelques Mo par
  architecture), soit accepter TLS 1.2 (ECDHE + AEAD) avec ces invités. **À trancher.**
- **Certificat** : l'Android Keystore génère lui-même un certificat auto-signé pour une paire de clés (sujet, numéro
  de série et validité réglables) et la clé n'est pas exportable. **Proposition : Keystore**, sans dépendance, avec
  Bouncy Castle (`bcpkix-jdk18on` 1.86) en `testImplementation` seulement, pour les tests JVM. À valider au prototype
  (un `SSLContext` serveur adossé à une clé du Keystore doit fonctionner sous Android 8 à 17).
- **Versions** : Ktor **3.6.0** (16/09/2026) ; compatibilité avec minSdk 26 et Kotlin 2.4 à vérifier au prototype
  (désucrage éventuel).
- **Accès au réseau local** : avec **targetSdk 37 (déjà le nôtre)**, Android 17 bloque par défaut tout le trafic local
  (connexions sortantes **et entrantes**, sur toutes les API réseau) sans la permission d'exécution
  `ACCESS_LOCAL_NETWORK` (groupe « Appareils à proximité »). Sous Android 16, le blocage n'est actif que sur opt-in
  (`adb shell am compat enable RESTRICT_LOCAL_NETWORK`). La documentation ne dit rien d'une exception pour un réseau
  créé par l'app (point d'accès local, `WifiNetworkSpecifier`) : **à tester sur un appareil Android 17**. La
  permission n'est demandée qu'à partir de l'API 37.
- **Point d'accès local** : à partir de l'API 33, `NEARBY_WIFI_DEVICES` (avec `neverForLocation`) ; en dessous,
  `ACCESS_FINE_LOCATION`, que l'app ne demande jamais : hôte sous Android 12 ou moins = « même Wi-Fi » seulement.
- **Service de premier plan `connectedDevice`** : `FOREGROUND_SERVICE_CONNECTED_DEVICE`, et l'un des prérequis est
  de déclarer `CHANGE_NETWORK_STATE` ou `CHANGE_WIFI_STATE`, déjà nécessaires : condition remplie. Usage décrit par
  Android : « interactions avec des appareils externes par Bluetooth, NFC, IR, USB ou réseau ». Justification à
  préparer pour la Play Console.

### Adaptations de l'existant (avant la phase 3 bis)
Voir le jalon **A1** au § 9 : validation extraite en fonction pure, enveloppe d'événement (auteur, `seq`), actions
de match dans le journal, animations déduites de l'état, migration Room v4, test réseau.

## 7. Comptes, invitation, rôles
- **Auth** : **code OTP à 6 chiffres reçu par e-mail** (le lien magique a besoin d'un deep link, reporté) et mot de passe en option ; Google via
  **Credential Manager** → `signInWithIdToken`. **Connexion anonyme** pour un invité qui veut suivre
  la partie sur son propre téléphone ; il peut convertir son compte plus tard.
- **Code de partie** : 6 caractères, alphabet de 31 symboles sans caractères ambigus
  (pas de 0/O, 1/I/L), soit environ 887 millions de combinaisons. Il expire à la fin de la partie ou
  après 24 h, avec limitation de débit sur `join_game`.
- **QR (révision du 2026-10-28)** : en partie à côté, le QR porte un lien `…/join#v=1&h=…&p=…&fp=…&t=…` (plus
  `ssid`/`psk` en point d'accès), sans code court ; pour le jeu à distance (phase 4), il porte le code. Ce qui suit
  reste valable pour le scan. Ancienne règle : le QR contient **uniquement le code** (texte `ABC123`), généré avec **zxing core** et scanné avec
  **CameraX (`ImageAnalysis`) + zxing-cpp** (wrapper Android, Apache 2.0, aucune connexion réseau) ; analyse
  limitée à la zone de cadrage et à quelques images par seconde, nouvel essai en image inversée et contraste
  renforcé contre les reflets d'écran. **Pas de ML Kit** : même embarqué, il envoie des statistiques d'usage
  à Google. La **saisie manuelle du code** reste toujours proposée sur l'écran de scan. Le scan remplit le
  champ « Rejoindre » (objectif : rejoindre en moins de 10 s). **Les liens profonds restent reportés** (schéma `molkkyboard://` réservé) : aucun intent-filter en V1.
- **Partage social (décision du 2026-10-25)** : le QR des vignettes et « Défier » mènent à la **fiche Play
  Store** ; l'invitation de groupe ou la revanche passent par le **code à 6 caractères** écrit dans le message.
  Vrais liens (App Links) quand un domaine sera acquis, en même temps que le lien spectateur.
- **Rôles** : chacun saisit son propre lancer et peut corriger **son dernier lancer** ; l'organisateur peut
  corriger ou supprimer n'importe quel lancer, gérer les joueurs (ordre, ajout, retrait), saisir pour les
  invités et clore la partie. Pas d'interface d'administration en V1.

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

> Replanifiés le 2026-10-25 après l'élargissement de la V1 (prompt du soir). Fait : J0 à J7 (V1 locale).
> **Révision proposée le 2026-10-28** (phase 3 bis « Partie à côté »), en attente de validation : J7b, A1, P0, S1 à
> S3 insérés ; l'ancienne Phase 4 (J8 à J11) devient R1 à R4, dont le report en V2 n'est pas tranché.

| # | Jalon | Contenu | Appareils | Arrêt pour validation |
|---|---|---|---|---|
| J0–J7 ✅ | V1 locale | squelette, moteur, design system, données, jeu, fin de partie, partage, confiance et langues | — | — |
| J7b ✅ (test sur téléphone à faire) | Restes du test de rattrapage | C6 avatars sobres (captures à choisir), C13 accessibilité automatique, N2 animation à chaque lancer, N3 tension avant la sanction, N1 photo du moment (maquette d'abord) | émulateur, puis ton téléphone | captures + QCM |
| **A1** ✅ | Adaptations pour plusieurs appareils (branche principale) | `validate()` extraite du dépôt en fonction pure (`core:sync`, squelette) ; enveloppe d'événement (`seq` d'autorité, appareil auteur, horodatage) ; manche suivante, Mölkkout, revanche dans le journal ; animations et annonces déduites du changement d'état ; migration Room v4 et test de migration ; test « l'APK ne déclare pas `INTERNET` » remplacé plus tard par le test d'adresses locales | émulateur | rapport de tests |
| **P0** | Prototype **jetable**, branche `proto/partie-a-cote` | serveur WebSocket sur TLS (options du § 6 bis : Netty, `SSLServerSocket` + trames Ktor), certificat auto-signé (Keystore), point d'accès local, invité par `WifiNetworkSpecifier`, épinglage de l'empreinte, un message aller-retour ; mesures : temps pour rejoindre, taille de l'APK, comportement sous Android 17 (`ACCESS_LOCAL_NETWORK`) | **deux téléphones réels** (Fairphone 6 + un second, idéalement une autre version d'Android) | **résultat, limites, versions retenues** ; rien ne passe sur la branche principale sans accord |
| S1 | Protocole et autorité | `core:sync` complet (messages, `HostSession`, `GuestSession`, droits, idempotence, rattrapage, `STALE`), `core:sync-lan` avec tests d'intégration JVM (hôte + 2 invités, vrai TLS, mauvaise empreinte, jeton expiré ou réutilisé, téléphone exclu, reconnexion), couverture complète | JVM | rapport de tests |
| S2 | Réseau Android | `core:sync-android` : modes « même Wi-Fi » et point d'accès, permissions au moment de l'usage (refus testés), service de premier plan, certificat de partie, jetons chiffrés exclus des sauvegardes ; lien `molkkyboard://join#…` (App Links quand le domaine existera) | deux téléphones réels | démo |
| S3 | Écrans et robustesse | interrupteur « Plusieurs téléphones », « Inviter » (QR, demandes, exclusion), « Rejoindre » (CameraX + zxing-cpp), pastille de connexion, coupure et reconnexion, revanche partagée, annonces sur l'hôte seulement, notification ; `docs/qa/PARTIE_A_COTE.md` ; mise à jour du modèle de menaces | deux téléphones réels (+ un Android 8-9 si possible) | QCM sur téléphones |
| R1–R4 | Ancienne Phase 4 (backend, comptes, jeu à distance, groupes) | réutilise `core:sync`, seul le transport change ; **report en V2 à trancher** ; le choix Supabase est à revoir (la validation en Kotlin ne s'exécute pas dans une fonction SQL) | — | — |
| J12 | Finitions et build | Baseline Profiles, audit d'accessibilité, build release signé (Phase 5) | téléphone | APK |

Ce qui exige deux téléphones réels : P0, S2 et S3 (l'émulateur ne crée pas de point d'accès, et deux émulateurs
sont isolés chacun derrière leur propre réseau virtuel). Tout le protocole (S1) se teste en JVM sur la boucle locale.

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
- Les corrections **pendant** la partie sont normales (`hasCorrections`, simple mention dans l'historique).
- `modifiedAfterFinish` : correction, suppression ou annulation acceptée alors que la manche était terminée,
  sauf l'annulation immédiate du lancer gagnant (validé le 2026-10-25).
- **Éligible aux records** = manche `FINISHED` ∧ ¬`ABANDONED` ∧ ¬`modifiedAfterFinish`. Les autres restent dans
  l'historique avec un badge (« abandonnée », « modifiée après la fin »).
- Les lancers en mode « Rapide » comptent partout, sauf dans les classements portant sur une quille précise.

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
- Tests : une table de cas dans `docs/STATS_SPEC.md` (écrite au J5), y compris les égalités et
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
- **Partie à côté (2026-10-28)** : serveur TLS (le moteur CIO ne gère pas TLS), TLS 1.3 sous Android 8-9 (embarquer
  Conscrypt ou non), Keystore ou Bouncy Castle, JSON ou CBOR : à trancher au prototype P0.
- **Encart « Vie privée »**, **report de la Phase 4 en V2**, **formulaire « Sécurité des données »** : à trancher
  par le porteur du projet (spec du 2026-10-28).
- ~~Liens d'invitation~~ : reportés après la v1 ; on rejoint par code ou QR contenant le code (décidé le 2026-10-25).
- ~~Région Supabase~~ : **Paris (`eu-west-3`)** (décidé le 2026-10-25).
- **Durée de conservation** des parties partagées (J11).
- **Domaine** pour les App Links et le lien spectateur : à acquérir plus tard (décision du 2026-10-25).
- ~~Fin de la Phase 5~~ : configuration du build release signé (prompt complété le 2026-10-25).

## Vérification par jalon

- `./gradlew test detekt ktlintCheck assembleDebug` au vert en local et en CI.
- J2 : `./gradlew recordRoborazziDebug`, captures validées par le porteur du projet.
- J6 : `supabase test db` (pgTAP, RLS).
- J8 : scénario à deux appareils — saisie simultanée, mode avion puis rattrapage.
