# Projet : application Android de gestion de parties de Mölkky

## Étape 0 — Mémoire du projet
Le projet se trouve dans le dossier `molkky` (le répertoire courant ; vérifie-le avec `pwd`).
Avant toute autre action, enregistre l'intégralité de ce prompt dans `molkky/CLAUDE.md`
pour qu'il te serve de référence permanente à chaque session. Ajoute en fin de fichier
une section `## Décisions` que tu mettras à jour au fil des phases (choix d'architecture
validés, backend retenu, options de règles, conventions de code).
Si un `CLAUDE.md` existe déjà, ne l'écrase pas : fusionne les contenus et montre-moi le résultat.

## Environnement de développement (déjà installé)
- Machine Linux (base Ubuntu 24.04), ressources limitées (PC portable ancien) : garde des réglages
  Gradle raisonnables (`org.gradle.jvmargs` modéré, pas de parallélisme excessif) et préviens-moi
  si un build devient trop lourd
- **Android Studio** est installé (Flatpak). Je ne le lance que pour consulter les `@Preview` :
  ne compte pas dessus pendant tes builds, travaille en ligne de commande
- **JDK 21** (OpenJDK), **Android SDK** dans `~/Android/Sdk` (`$ANDROID_HOME` défini),
  `adb` du SDK en priorité dans le PATH
- **Android CLI** (`android`) est installé : utilise-le à la place de sdkmanager/avdmanager
  (`android sdk install`, `android emulator create/list/start`, `android install`, `android run`,
  `android screen capture`…). En cas de doute sur une commande : `android <commande> -h`
- KVM est actif ; mon utilisateur est dans les groupes `kvm` et `plugdev`
- `git` et `gh` (GitHub CLI) sont installés ; le dépôt git est initialisé dans `molkky`

Commence par vérifier rapidement l'environnement (`java -version`, `android info`,
`echo $ANDROID_HOME`, `adb devices`) et signale-moi tout ce qui manque.

**Tests de l'app** : je veux tester sur un émulateur sur ce PC (mon téléphone viendra plus tard).
Crée l'émulateur avec Android CLI s'il n'existe pas. Après chaque build, installe et lance l'app,
puis utilise `android screen capture` pour vérifier visuellement l'interface avant de me la présenter.

---

## Contexte
Je veux une application Android native, simple, intuitive et soignée visuellement,
pour gérer des parties de Mölkky : saisie des lancers, calcul automatique des scores,
application des règles, historique. Plusieurs joueurs doivent pouvoir suivre et saisir
une même partie depuis plusieurs téléphones, ce qui implique des comptes utilisateurs
et une synchronisation temps réel.

Travaille par phases. À la fin de chaque phase, résume ce qui est fait et attends ma
validation avant de passer à la suivante.

---

## Phase 1 — Récupérer et formaliser les règles
1. Recherche les règles officielles du Mölkky. Sources à privilégier : la Fédération
   Française de Mölkky et l'organisation finlandaise d'origine (Mölkky / Tuoterengas).
   Croise au moins deux sources et signale les divergences.
2. Rédige un fichier `docs/RULES.md` (reformulé, pas de copie de texte) couvrant au minimum :
   - le décompte : une seule quille tombée = valeur de la quille ; plusieurs quilles = nombre de quilles tombées ;
   - l'objectif de 50 points exactement, et le retour à 25 en cas de dépassement ;
   - l'élimination après 3 lancers consécutifs sans faire tomber de quille (et ce qui se passe en équipe) ;
   - les lancers mordants / fautes (pied sur la ligne, etc.) si les règles officielles en prévoient ;
   - le jeu en équipes et l'ordre de passage ;
   - la fin de partie et le déroulé d'une série de manches.
3. Liste les variantes courantes (ex. pas d'élimination, retour à 0 au lieu de 25,
   objectif différent) : elles deviendront des options paramétrables.
4. Écris `docs/SCORING_SPEC.md` : la spécification du moteur de score sous forme de
   cas de test (entrée = état + lancer, sortie = nouvel état).

👉 Arrête-toi et montre-moi RULES.md et SCORING_SPEC.md.

---

## Phase 2 — Architecture (utilise le plan mode)
Propose une architecture et justifie les choix. Base suggérée, à challenger :
- **Kotlin + Jetpack Compose + Material 3**, une seule Activity, Navigation Compose ;
- **Architecture** : MVVM / UDF, modules `core:scoring` (Kotlin pur, sans dépendance Android),
  `core:data`, `core:designsystem`, `feature:*` ;
- **Moteur de score** : machine à états immuable, entièrement testée (JUnit), qui gère
  aussi l'annulation du dernier lancer (undo) ;
- **Stockage local** : Room, fonctionnement 100 % hors ligne en mode « un seul téléphone » ;
- **Comptes + multi-appareils** : compare Supabase (Auth + Postgres + Realtime, hébergement UE possible)
  et Firebase (Auth + Firestore). J'ai un profil cybersécurité/RGPD : je privilégie un hébergement UE,
  la minimisation des données et des règles d'accès strictes (RLS ou security rules).
  Fais une recommandation argumentée ;
- **Gestion des conflits** : deux téléphones qui saisissent en même temps — propose un modèle
  (journal d'événements de lancers ordonnés côté serveur, l'état étant recalculé à partir du journal) ;
- **Rejoindre une partie** : code court à 6 caractères + QR code ;
- **Injection** : Hilt ; **minSdk** 26, **targetSdk** la dernière version stable.

Produis `docs/ARCHITECTURE.md` et un plan d'implémentation découpé en jalons.

👉 Arrête-toi pour validation.

---

## Phase 3 — MVP local (un seul téléphone)
- Créer une partie : joueurs ou équipes, ordre de passage, options de règles ;
- Écran de jeu : grille des 12 quilles (sélection tactile des quilles tombées),
  bouton « raté », saisie en quelques gestes, score de chacun bien visible,
  joueur courant mis en avant, indicateur des ratés consécutifs (0/3) ;
- Undo du dernier lancer ;
- Écran de fin + historique des parties + quelques statistiques simples
  (moyenne par lancer, taux de ratés, parties gagnées) ;
- Profils de joueurs locaux réutilisables.

## Phase 4 — Comptes et multi-appareils
- Inscription / connexion (e-mail + lien magique ou mot de passe ; Google en option) ;
- Création d'une partie partagée, invitation par code ou QR ;
- Synchronisation temps réel ; un appareil hors ligne rattrape son retard en revenant ;
- Rôles simples : l'organisateur peut corriger les scores, les autres peuvent saisir leur propre lancer ;
- Les joueurs invités sans compte restent possibles.

## Phase 5 — Build
- Génère un APK debug avec `./gradlew assembleDebug` et indique son chemin ;
- Prépare la configuration d'un build release signé (keystore hors du dépôt, secrets non commités).

---

## Stack technique (versions : toujours la dernière stable, centralisées dans `gradle/libs.versions.toml`)

### Fondations
- **Kotlin** (dernière stable) + **KSP** (pas de kapt)
- **Gradle Kotlin DSL** + version catalog + convention plugins dans `build-logic/`
- **Coroutines + Flow** pour toute l'asynchronie ; **kotlinx-serialization** pour le JSON
- **kotlinx-datetime** pour les horodatages des lancers

### UI
- **Jetpack Compose** via le **Compose BOM**
- **Material 3** (composants Expressive si disponibles en stable), sans couleurs dynamiques : on impose notre identité visuelle
- **Navigation Compose** avec routes typées (@Serializable)
- **androidx.core:core-splashscreen** pour l'écran de démarrage
- **Coil 3** pour les avatars de joueurs
- **Vico** pour les graphiques de statistiques
- Animations : API Compose natives (`animate*AsState`, `AnimatedContent`, `updateTransition`, physique `spring`) ; pas de Lottie sauf besoin avéré

### Données et injection
- **Hilt** (+ hilt-navigation-compose)
- **Room** pour le stockage local, **DataStore (Preferences)** pour les réglages
- Backend selon la décision de la phase 2 :
  - Supabase : **supabase-kt** (modules auth, postgrest, realtime) + client **Ktor** (moteur OkHttp)
  - Firebase : **Firebase BoM** (auth, firestore)
- Repository pattern : Room reste la source de vérité de l'UI, et la synchro distante se fait en arrière-plan (offline-first)

### QR code et invitation
- Génération : **zxing core**
- Scan : **CameraX** + **ML Kit Barcode Scanning** (version embarquée, qui ne dépend pas des Google Play services pour la détection)
- **Android App Links** pour qu'un lien d'invitation ouvre directement la partie

### Qualité
- Tests : **JUnit 5**, **Turbine** (Flow), **MockK**, **Compose UI Test**, **Robolectric** si nécessaire
- Tests de captures d'écran : **Roborazzi** (sur l'écran de jeu, en clair et en sombre)
- Lint : **ktlint** + **detekt**, branchés dans la CI
- **Baseline Profiles** pour la fluidité au démarrage et sur l'écran de jeu
- **Timber** pour les logs (aucun log en release)
- CI : workflow **GitHub Actions** (tests, lint, assembleDebug, APK en artefact)

---

## Direction artistique : « Nordic Bold »

Style moderne et graphique, inspiré du design scandinave et du bois de bouleau :
des aplats francs, des formes très arrondies, des chiffres énormes et des micro-animations
généreuses. Ni skeuomorphisme ni texture bois réaliste : on évoque le bois par la couleur et la forme.

### Palette (définir les tokens dans `core:designsystem`)
- Fond clair : bouleau `#F4EBDD`, surfaces `#FFFAF2`
- Primaire : vert forêt profond `#1F4D3A`
- Accent : safran `#F2A93B` (joueur actif, validation)
- Secondaire : corail `#E8604C` (raté, dépassement de 50, élimination)
- Texte : `#1B1B18`
- Mode sombre : fond `#14201A`, surfaces `#1E2B24`, accents conservés mais légèrement désaturés
- Une couleur de pastille par joueur/équipe, à choisir dans une palette de 8 teintes harmonisées et accessibles

### Typographie (polices Google Fonts embarquées dans l'APK)
- Titres et scores : **Bricolage Grotesque** (ou Space Grotesk), graisse 700-800
- Texte courant : **Inter**
- Chiffres tabulaires (`fontFeatureSettings = "tnum"`) pour que les scores ne « sautent » pas
- Score du joueur actif : 96sp minimum

### Formes et composants
- Coins arrondis à 24-32dp sur les cartes et en pilule sur les boutons
- **Quilles** : 12 cylindres stylisés aux extrémités biseautées, dessinés en Canvas/vector,
  numérotés, et disposés selon la formation de départ officielle
- **Jauge de progression** : un anneau ou une barre épaisse par joueur, de 0 à 50,
  qui vire au safran à l'approche de 50 et pulse quand il manque exactement un lancer gagnant
- Indicateur de ratés : 3 petits points qui se remplissent en corail
- Ombres douces et peu nombreuses ; la hiérarchie vient surtout de la couleur et de la taille

### Écrans clés
1. **Accueil** : grand bouton « Nouvelle partie », bouton « Rejoindre » (code/QR), parties en cours, historique, Hall of Fame
2. **Création de partie** : ajout des joueurs en chips colorées, glisser-déposer pour l'ordre, options de règles repliées dans « Règles avancées »
3. **Écran de jeu** (le plus important) :
   - en haut, le joueur ou l'équipe actif en très grand avec son score et ses ratés ;
   - au centre, la grille des quilles, où un tap couche une quille et un second tap la relève ;
   - un aperçu en direct, « +7 → 38 », avant validation ;
   - en bas, deux gros boutons : « Raté » (corail) et « Valider » (safran) ;
   - un bandeau horizontal avec les autres joueurs et leur score ; l'undo en geste ou en bouton discret avec snackbar
4. **Fin de partie** : animation de victoire (voir plus bas), classement, stats de la manche, bouton « Revanche »
5. **Stats** : graphiques Vico, records personnels

### Mouvement et retours
- Physique de ressort (`spring`) partout ; aucune animation linéaire
- La quille bascule avec une légère rotation quand on la sélectionne
- Le score défile comme un compteur quand il change
- En cas de dépassement de 50, un « shake » horizontal suivi de la retombée animée à 25
- Retours haptiques : léger à la sélection, confirmé à la validation, double vibration à l'élimination
- Respecter le réglage système « Réduire les animations »

### Accessibilité et usage en extérieur
- Contraste AA minimum, AAA sur les scores
- Zones tactiles de 56dp minimum, utilisables à une main
- Option « Plein soleil » : contraste renforcé
- Descriptions TalkBack sur chaque quille et chaque score
- Écran maintenu allumé pendant une partie (`keepScreenOn`)

👉 Avant de coder les écrans, crée le module `core:designsystem` (thème, tokens, typographie,
composants de base : PinView, ScoreRing, PlayerChip, BigActionButton) avec des `@Preview`
clair/sombre, et montre-moi les captures Roborazzi pour validation.

---

## Spécifications complémentaires

### Équipes nommées
- Une partie se joue en solo ou en équipes. Chaque équipe a un **nom**, une **couleur** (palette des 8 teintes) et un **emblème** (une icône simple parmi une petite banque : quille, feuille, éclair, vague…)
- Bouton « 🎲 » qui propose un nom d'équipe amusant au hasard (liste de ~50 noms en français autour du Mölkky, du bois et de la Bretagne/Finlande), toujours modifiable
- Une équipe est identifiée par **l'ensemble de ses joueurs**. Si les mêmes joueurs se regroupent, l'app propose de reprendre l'équipe existante et son nom, pour que son palmarès s'accumule
- Ordre de passage alterné entre les équipes, et rotation des lanceurs au sein de chaque équipe
- Le nom et la couleur de l'équipe s'affichent partout : écran de jeu, bandeau des scores, fin de partie, Hall of Fame

### Animation de victoire
- Déclenchée au moment où une équipe ou un joueur atteint exactement 50 points
- Séquence de 2 à 3 secondes, que l'on peut passer d'un tap :
  1. le 50 « explose » en grand dans la couleur de l'équipe ;
  2. une pluie de confettis en forme de petites quilles, aux couleurs de l'équipe (dessinée en Canvas Compose, sans librairie externe) ;
  3. le nom de l'équipe apparaît en grand avec un trophée ;
  4. une double vibration de célébration retentit.
- Si un record est battu pendant la partie, ajouter un bandeau « 🏆 Nouveau record ! »
- Son de victoire optionnel, désactivé par défaut et réglable
- Sur une partie partagée, l'animation se joue **simultanément sur tous les téléphones connectés**
- Version allégée, un simple fondu avec le trophée, si l'option système « Réduire les animations » est active

### Hall of Fame
Écran dédié, accessible depuis l'accueil, avec des onglets **Joueurs** et **Équipes**.

Classements :
- **Nombre de victoires**, avec le nombre de parties jouées et le taux de victoire
- **Victoire la plus rapide**, en nombre de lancers de l'équipe ou du joueur gagnant.
  Le minimum théorique est de 5 lancers (4 × 12 + 2) : une victoire en 5 lancers est une
  « partie parfaite » et mérite un badge spécial
- Pour l'onglet Équipes, préciser le nombre de joueurs de l'équipe et proposer un filtre par taille d'équipe

Chaque record affiche la date de la partie et un lien vers son détail (déroulé des lancers).

Périmètre :
- **Local** : toutes les parties jouées sur ce téléphone
- **Groupe** : un groupe (famille, amis, club) réunit des comptes. Son Hall of Fame est partagé et synchronisé entre ses membres. Une partie partagée est rattachée à un groupe à sa création, ce rattachement étant facultatif
- Filtres : tout le temps, cette année, ce mois-ci

Contraintes techniques :
- **Toutes les statistiques sont calculées à partir du journal des lancers** et non stockées en dur, afin de pouvoir ajouter de nouveaux classements plus tard en les appliquant rétroactivement
- Seules les parties terminées normalement comptent. Une partie abandonnée ou dont des scores ont été corrigés manuellement est marquée et exclue des records, mais reste dans l'historique
- Tests unitaires sur le calcul des classements, notamment pour les égalités (départage par date : le premier à l'avoir réalisé garde le record)

### Compléments optionnels (après le reste, avec validation)
- **Records secondaires** : plus grand nombre de « 12 » réussis, plus longue série sans raté, nombre de retours à 25, plus belle remontée
- **Badges** de joueur : première victoire, partie parfaite, « Phénix » (victoire après un retour à 25), « Sniper » (trois 12 dans une partie)
- **Carte de résultat partageable** : image générée (score final, équipes, durée, record éventuel) envoyée via le partage Android
- **Revanche** : relancer en un tap une partie avec les mêmes équipes et l'ordre de passage inversé

---

## Backlog de fonctionnalités avancées
Règle : ne commencer une fonctionnalité de cette liste qu'une fois le MVP et les specs
complémentaires terminés. Avant chaque lot, proposer un découpage et attendre ma validation.
Priorités : **P1** = V1 enrichie, **P2** = V2, **P3** = plus tard.

### Aide au jeu
- **P1 — Coach de fin de partie** : à partir de 38 points, afficher ce qu'il faut pour gagner
  (« Il te faut 7 : la quille 7 seule, ou 7 quilles »), surligner la quille gagnante sur la grille,
  et signaler la zone de danger quand le risque de dépasser 50 est élevé
- **P1 — Règles consultables** en un tap pendant la partie, avec des schémas animés
- **P2 — Saisie vocale** hors ligne (« sept », « deux quilles », « raté », « annule »), sans envoi audio sur un serveur, avec confirmation visuelle
- **P3 — Commentateur** : petites phrases humoristiques contextuelles, en synthèse vocale si activé

### Modes de jeu
- **P1 — Mode famille / handicap** : score de départ personnalisé par joueur, objectif réduit,
  élimination désactivable pour certains joueurs, rappel de la distance de lancer conseillée.
  Ces parties sont exclues des records « purs » mais comptent dans les victoires
- **P2 — Mode entraînement solo** : défis et suivi de progression
- **P2 — Tournoi** : poules puis élimination directe, arbre du tableau, plusieurs terrains en parallèle
- **P3 — Saisons** de groupe avec podium de fin de saison
- **P3 — Défi du jour** commun aux membres d'un groupe

### Statistiques et classement
- **P2 — Classement Elo** au sein d'un groupe (ou modèle type TrueSkill pour les équipes), avec courbe d'évolution
- **P2 — Face-à-face** entre deux joueurs ou équipes
- **P2 — Carte de précision** : fréquence de chaque quille touchée seule, sur la formation de quilles
- **P3 — Statistiques par terrain** (lieu saisi manuellement, pas de GPS par défaut)

### Affichage et partage
- **P1 — Notification de partie en cours** avec action rapide « Saisir le lancer »
- **P2 — Mode tableau d'affichage** paysage pour tablette, en lecture seule
- **P2 — Lien spectateur** web en lecture seule, alimenté en temps réel
- **P2 — Widget d'écran d'accueil** (Jetpack Glance)
- **P3 — Photos** d'équipe et souvenirs de partie

### Personnalisation
- **P2 — Habillages de quilles** : bois naturel, couleurs vives, monochrome
- **P2 — Mode daltonien** : couleurs d'équipe doublées d'un motif ou d'un emblème
- **P3 — Langues** : français, anglais, finnois

### Confiance, données et publication
- **P1 — Contrôle des données (RGPD)** : export JSON, suppression du compte et des données depuis l'app,
  minimisation (un pseudo suffit)
- **P1 — Sauvegarde / restauration** locale (fichier) pour le mode sans compte
- **P2 — Préparation Play Store** : icône adaptative avec variante monochrome, politique de confidentialité,
  formulaire « Sécurité des données », captures générées à partir des tests Roborazzi
- **P2 — Modération légère** des noms d'équipes et pseudos par l'organisateur du groupe

---

## Contraintes de qualité
- Le moteur de score doit avoir une couverture de tests complète, alignée sur SCORING_SPEC.md ;
- Pas de clé ni de secret dans le code ;
- Commits réguliers et explicites ; un README qui explique comment builder et lancer l'app ;
- Interface en français, avec des chaînes externalisées pour pouvoir traduire plus tard.
