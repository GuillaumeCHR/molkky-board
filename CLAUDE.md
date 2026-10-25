# Molkky Board — prompt de référence

> Prompt de référence : copie intégrale de `prompt-molkky.md` (version du 2026-10-25 au soir, qui intègre
> aussi la spec de l'écran de démarrage). Les versions précédentes restent dans l'historique git.
> Les décisions prises au fil des phases sont dans la section `## Décisions` en fin de fichier.

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

## Identité de l'application
- **Nom de l'application : Molkky Board** (écrit exactement ainsi, avec une majuscule à chaque mot)
- **Éditeur : Yogi Studio**
- Applique ce nom de façon cohérente dans tout le projet :
  - nom affiché sous l'icône du lanceur et dans les réglages Android (`app_name` = « Molkky Board ») ;
  - identifiant d'application : `com.yogistudio.molkkyboard` (namespace et applicationId) ;
  - projet Gradle : `rootProject.name = "MolkkyBoard"` ;
  - code : classe `MolkkyBoardApp` (Application), thème `MolkkyBoardTheme`, préfixe des composants
    de design `MB` si un préfixe est nécessaire ;
  - liens profonds : schéma `molkkyboard://` (domaine des App Links à définir en phase 4) ;
  - canaux de notification, titre de l'écran d'accueil, écran « À propos », README, carte de résultat
    partageable, page spectateur web, annonces vocales, métadonnées Play Store ;
  - écran de démarrage (voir la section dédiée).
- Le dossier local reste `molkky` ; ne le renomme pas
- Le mot « Mölkky » (avec tréma) désigne **le jeu** dans les textes de l'interface et les règles ;
  « Molkky Board » désigne **l'application**

## Contexte et positionnement
Je veux une application Android native, simple, intuitive et soignée visuellement,
pour gérer des parties de Mölkky : saisie des lancers, calcul automatique des scores,
application des règles, historique. Plusieurs joueurs doivent pouvoir suivre et saisir
une même partie depuis plusieurs téléphones, ce qui implique des comptes utilisateurs
et une synchronisation temps réel.

Une analyse des avis des apps concurrentes (octobre 2026) a fait ressortir leurs défauts récurrents :
prise en main confuse (on ne sait pas comment démarrer, tutoriel vidéo trop rapide), impossibilité
de corriger un lancer une fois le joueur suivant passé, ordre de passage figé, taps accidentels,
parties perdues à la fermeture de l'app, scores à deux chiffres tronqués en grande police,
historique incomplet, demande d'une saisie numérique rapide. Molkky Board doit être irréprochable
sur chacun de ces points. Nos différenciateurs :
- **multi-téléphones en temps réel** (rare sur Android) : rejoindre une partie en moins de 10 secondes via QR code ;
- **aucune publicité, aucun traceur**, aucune donnée collectée en mode sans compte ;
- équipes nommées avec Hall of Fame, mode famille, coach de fin de partie, annonces vocales ;
- **partage social** : chaque partie devient en deux taps une vignette ou un mini-reel prêt à publier,
  qui fait connaître l'app et invite à rejoindre une partie.

Travaille par phases. À la fin de chaque phase, résume ce qui est fait et attends ma
validation avant de passer à la suivante.

---

## Phase 1 — Récupérer et formaliser les règles
1. Recherche les règles officielles du Mölkky. Sources à privilégier : la Fédération
   Française de Mölkky et le détenteur actuel de la marque en Finlande (Tactic Games, qui a
   repris les droits de Tuoterengas / Lahden Paikka). Croise au moins deux sources et signale les divergences.
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
   cas de test (entrée = état + lancer, sortie = nouvel état), y compris :
   - la correction ou la suppression d'un lancer ancien et le recalcul complet qui en découle
     (un retour à 25 ou une élimination qui disparaissent, un vainqueur qui change) ;
   - l'ajout d'un joueur en cours de partie, le retrait d'un joueur, le changement d'ordre ;
   - les deux modes de saisie (quilles touchées / saisie rapide) ;
   - les scores de départ et objectifs personnalisés du mode famille.

👉 Arrête-toi et montre-moi RULES.md et SCORING_SPEC.md.

---

## Phase 2 — Architecture (utilise le plan mode)
Propose une architecture et justifie les choix. Base suggérée, à challenger :
- **Kotlin + Jetpack Compose + Material 3**, une seule Activity, Navigation Compose ;
- **Architecture** : MVVM / UDF, modules `core:scoring` (Kotlin pur, sans dépendance Android),
  `core:data`, `core:designsystem`, `feature:*` ;
- **Moteur de score** : fonction pure qui recalcule tout l'état d'une partie à partir de son
  **journal d'événements** (lancers, corrections, ajouts/retraits de joueurs, changements d'ordre).
  C'est ce modèle qui permet l'undo, la correction de n'importe quel lancer passé, la synchro
  multi-appareils et les statistiques rétroactives. Les règles (objectif, pénalité, élimination,
  futures variantes de jeu) sont une **stratégie** injectée dans le moteur ;
- **Stockage local** : Room, fonctionnement 100 % hors ligne en mode « un seul téléphone » ;
  le journal est persisté à chaque événement validé, pour qu'aucune partie ne soit jamais perdue ;
- **Comptes + multi-appareils** : compare Supabase (Auth + Postgres + Realtime, hébergement UE possible)
  et Firebase (Auth + Firestore). J'ai un profil cybersécurité/RGPD : je privilégie un hébergement UE,
  la minimisation des données et des règles d'accès strictes (RLS ou security rules).
  Fais une recommandation argumentée ;
- **Gestion des conflits** : deux téléphones qui saisissent en même temps — journal d'événements
  ordonnés côté serveur, l'état étant recalculé à partir du journal. Précise comment une correction
  d'un lancer ancien se propage aux autres téléphones ;
- **Rejoindre une partie** : code court à 6 caractères + QR code ;
- **Injection** : Hilt ; **minSdk** 26, **targetSdk** la dernière version stable.

Produis `docs/ARCHITECTURE.md` et un plan d'implémentation découpé en jalons.

👉 Arrête-toi pour validation.

---

## Phase 3 — MVP local (un seul téléphone)

### Création de partie
- Joueurs ou équipes nommées (voir « Équipes nommées »), options de règles repliées dans « Règles avancées »
- **Ordre de passage par glisser-déposer**, quel que soit l'ordre d'ajout des joueurs
- Profils de joueurs locaux réutilisables, avec avatar **emoji ou mascotte** par défaut
  (aucune photo nécessaire) et photo **depuis l'appareil photo ou la galerie** en option
- Le bouton « Lancer la partie » devient évident dès que deux joueurs sont ajoutés

### Écran de jeu
- **Deux modes de saisie**, au choix dans les réglages et modifiables en cours de partie :
  - **Quilles** (par défaut) : grille des 12 quilles, on touche celles qui sont tombées.
    C'est ce mode qui alimente les statistiques par quille ;
  - **Rapide** : pavé de gros boutons 0 à 12, avec un sélecteur « quille seule / nombre de quilles ».
    Les statistiques par quille ne sont pas disponibles pour ces lancers
- Bouton « Raté », aperçu en direct « +7 → 38 », et **rien n'est enregistré sans « Valider »** :
  aucun tap accidentel ne doit faire passer au joueur suivant
- Score de chacun bien visible, joueur courant mis en avant, ratés consécutifs (0/3)
- **Undo** du dernier lancer en un geste, avec snackbar
- **Déroulé de la partie** accessible depuis l'écran de jeu : on peut **modifier ou supprimer
  n'importe quel lancer passé**, et tout est recalculé (scores, retours à 25, éliminations, vainqueur)
- Gestion des joueurs en cours de partie : **changer l'ordre** entre deux tours (avec confirmation),
  **ajouter un joueur en retard** (il démarre à 0), **retirer un joueur** qui part

### Fin de partie, historique et statistiques
- Écran de fin avec animation de victoire, classement, stats de la manche et bouton « Revanche »
- Ordre pour la manche suivante paramétrable : identique, rotation, le dernier commence,
  le vainqueur joue en dernier
- **Historique complet et fiable** : chaque partie terminée y apparaît, avec son déroulé ;
  on peut supprimer une partie (avec confirmation)
- Statistiques simples : moyenne par lancer, taux de ratés, parties gagnées

### Prise en main
- **Pas de vidéo tutoriel** : une première partie guidée avec 3 à 4 bulles contextuelles maximum
  (ajouter les joueurs, toucher les quilles, valider, annuler), que l'on peut passer et revoir depuis les réglages
- Test UI dédié : un nouvel utilisateur arrive au premier lancer validé en moins de 30 secondes

### Fiabilité
- **Reprise automatique** d'une partie en cours après fermeture de l'app, redémarrage du téléphone
  ou mort du processus (test instrumenté dédié)
- **Grandes tailles de police** : aucun score à deux chiffres coupé ou tronqué jusqu'à un facteur
  de taille de police système de 2,0 (captures Roborazzi à 1,0, 1,5 et 2,0)

## Phase 4 — Comptes et multi-appareils
- Inscription / connexion (e-mail + lien magique ou mot de passe ; Google en option) ;
- Création d'une partie partagée, invitation par code ou QR : **rejoindre en moins de 10 secondes** ;
- Synchronisation temps réel ; un appareil hors ligne rattrape son retard en revenant ;
- Rôles simples : chacun saisit son propre lancer et peut corriger son dernier lancer ;
  l'organisateur peut corriger n'importe quel lancer et gérer les joueurs (ordre, ajout, retrait) ;
- Toute correction est visible des autres téléphones (petite notification « Lancer de Léa corrigé ») ;
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
- **androidx.core:core-splashscreen** pour l'écran de démarrage système
- **Coil 3** pour les photos de joueurs ; sélecteur de photos Android (Photo Picker) pour la galerie
- **Vico** pour les graphiques de statistiques
- **TextToSpeech** Android (hors ligne) pour les annonces vocales
- **Media3 Transformer** pour l'encodage des mini-vidéos de partage ; `FileProvider` pour partager les fichiers générés
- Animations : API Compose natives (`animate*AsState`, `AnimatedContent`, `updateTransition`, physique `spring`) ; pas de Lottie sauf besoin avéré

### Données et injection
- **Hilt** (+ hilt-navigation-compose)
- **Room** pour le stockage local, **DataStore (Preferences)** pour les réglages
- Backend selon la décision de la phase 2 :
  - Supabase : **supabase-kt** (modules auth, postgrest, realtime) + client **Ktor** (moteur OkHttp)
  - Firebase : **Firebase BoM** (auth, firestore)
- Repository pattern : Room reste la source de vérité de l'UI, et la synchro distante se fait en arrière-plan (offline-first)
- **Aucun SDK de publicité, d'analytics ou de crash reporting tiers**

### QR code et invitation
- Génération : **zxing core**
- Scan : **CameraX** + **ML Kit Barcode Scanning** (version embarquée, qui ne dépend pas des Google Play services pour la détection)
- **Android App Links** pour qu'un lien d'invitation ouvre directement la partie

### Qualité
- Tests : **JUnit 5**, **Turbine** (Flow), **MockK**, **Compose UI Test**, **Robolectric** si nécessaire
- Tests de captures d'écran : **Roborazzi** (écran de jeu en clair et en sombre, à plusieurs tailles de police)
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
- Score du joueur actif : 96sp minimum, qui s'adapte sans troncature aux grandes tailles de police

### Formes et composants
- Coins arrondis à 24-32dp sur les cartes et en pilule sur les boutons
- **Quilles** : 12 cylindres stylisés aux extrémités biseautées, dessinés en Canvas/vector,
  numérotés, et disposés selon la formation de départ officielle
- **Pavé de saisie rapide** : 13 gros boutons (0 à 12) en grille, cohérents avec le style des quilles
- **Jauge de progression** : un anneau ou une barre épaisse par joueur, de 0 à 50,
  qui vire au safran à l'approche de 50 et pulse quand il manque exactement un lancer gagnant
- Indicateur de ratés : 3 petits points qui se remplissent en corail
- Ombres douces et peu nombreuses ; la hiérarchie vient surtout de la couleur et de la taille

### Écrans clés
1. **Accueil** : logotype « Molkky Board » en en-tête, grand bouton « Nouvelle partie »,
   bouton « Rejoindre » (code/QR), partie en cours à reprendre, historique, Hall of Fame
2. **Création de partie** : ajout des joueurs en chips colorées, glisser-déposer pour l'ordre,
   options de règles repliées dans « Règles avancées », bouton de lancement bien visible
3. **Écran de jeu** (le plus important) :
   - en haut, le joueur ou l'équipe actif en très grand avec son score et ses ratés ;
   - au centre, la grille des quilles (un tap couche une quille, un second tap la relève)
     ou le pavé rapide selon le mode, avec une bascule discrète entre les deux ;
   - un aperçu en direct, « +7 → 38 », avant validation ;
   - en bas, deux gros boutons : « Raté » (corail) et « Valider » (safran) ;
   - un bandeau horizontal avec les autres joueurs et leur score ;
   - l'undo en geste ou en bouton discret avec snackbar, et l'accès au déroulé et à la gestion des joueurs
4. **Déroulé de la partie** : liste chronologique des lancers, chacun modifiable ou supprimable
5. **Fin de partie** : animation de victoire, classement, stats de la manche, bouton « Revanche »
6. **Stats** : graphiques Vico, records personnels
7. **Réglages** : mode de saisie, annonces vocales, sons, version courte de l'intro,
   revoir la prise en main, « Plein soleil », données (export, suppression)
8. **À propos** : logotype Molkky Board, version, mention « Yogi Studio »

### Mouvement et retours
- Physique de ressort (`spring`) partout ; aucune animation linéaire
- La quille bascule avec une légère rotation quand on la sélectionne
- Le score défile comme un compteur quand il change, y compris lors d'un recalcul après correction
- En cas de dépassement de 50, un « shake » horizontal suivi de la retombée animée à 25
- Retours haptiques : léger à la sélection, confirmé à la validation, double vibration à l'élimination
- Respecter le réglage système « Réduire les animations »

### Accessibilité et usage en extérieur
- Contraste AA minimum, AAA sur les scores
- Zones tactiles de 56dp minimum, utilisables à une main
- Option « Plein soleil » : contraste renforcé
- Grandes tailles de police prises en charge jusqu'au facteur 2,0 sans troncature
- Descriptions TalkBack sur chaque quille et chaque score
- Écran maintenu allumé pendant une partie (`keepScreenOn`)

👉 Avant de coder les écrans, crée le module `core:designsystem` (thème, tokens, typographie,
composants de base : PinView, QuickPad, ScoreRing, PlayerChip, BigActionButton) avec des `@Preview`
clair/sombre, et montre-moi les captures Roborazzi pour validation.

---

## Écran de démarrage et éditeur « Yogi Studio »

### Éditeur et logos
- L'éditeur de l'application est **Yogi Studio**. Les fichiers de marque sont dans `docs/brand/` :
  `yogi-studio-icon.svg` (logo carré) et `yogi-studio-logo.svg` (logo horizontal avec le nom).
  Convertis-les en VectorDrawable pour l'app (pas de PNG si le vectoriel suffit)
- « Yogi Studio » apparaît dans l'animation de démarrage, dans l'écran « À propos »
  (avec la version de l'app) et dans les métadonnées de publication
- Le logo Yogi Studio n'est **pas** l'icône de l'application : l'icône de **Molkky Board** reste à concevoir
  séparément, dans la direction artistique « Nordic Bold » (proposition attendue : une quille stylisée
  et un « MB » ou un tableau de score, lisible en 48dp, avec variante monochrome)
- Logotype **Molkky Board** à créer pour l'intro et l'en-tête de l'accueil : « Molkky » en Bricolage Grotesque
  800 vert forêt, « Board » en safran, les deux mots sur une même ligne de base

### Principe technique
- L'écran de démarrage système d'Android 12+ (API SplashScreen) est très limité en durée et en animation :
  il n'affiche que l'icône de l'app sur fond bouleau `#F4EBDD`, le temps strictement nécessaire
- Il enchaîne sans coupure visible (mêmes fond et position) sur un **écran d'intro Compose**
  qui joue l'animation complète
- **Chargement en tâche de fond** pendant l'animation : initialisation de Room, lecture du DataStore,
  préchargement des polices et des données de l'accueil, détection d'une partie en cours à reprendre,
  restauration de la session et synchronisation si un compte est connecté. Le chargement démarre
  dès `Application.onCreate` et ne dépend jamais de l'animation
- Passage au menu quand **l'animation ET le chargement** sont terminés :
  - si le chargement finit avant, l'animation va à son terme (pas d'attente supplémentaire) ;
  - s'il est plus long, le logo Yogi Studio reste affiché avec un indicateur discret ;
  - si le chargement échoue, le menu s'ouvre quand même en mode hors ligne, avec un message non bloquant
- S'il existe une partie en cours, l'accueil propose de la reprendre en premier
- Durée totale de l'animation : **4,5 s maximum**, passable d'un tap à tout moment
  (on saute alors à la transition vers le menu)
- À partir du deuxième lancement, proposer dans les réglages une version courte (environ 1,5 s :
  impact puis logotype) ; l'animation complète reste la valeur par défaut
- « Réduire les animations » activé : simple fondu du logo Yogi Studio vers le menu (environ 1 s)

### Storyboard (≈ 4,5 s, entièrement en Canvas Compose, sans librairie d'animation externe)
1. **0 – 0,6 s — Mise en place** : les 12 quilles apparaissent dans leur formation de départ
   sur fond bouleau, avec un léger rebond (spring)
2. **0,6 – 1,2 s — Le lancer** : la quille de lancer (le mölkky) arrive du bas de l'écran
   en tournoyant, avec une traînée de mouvement
3. **1,2 – 2,6 s — L'impact** : les quilles volent en éclats. Chaque quille se fragmente en
   8 à 15 éclats de bois (polygones irréguliers dans les teintes bois de la palette),
   projetés avec une physique simple (vitesse initiale, rotation, gravité, rebond amorti au sol).
   Petit tremblement de l'écran et vibration forte au moment de l'impact. Les numéros des
   quilles s'envolent avec les éclats
4. **2,6 – 3,6 s — La signature** : pendant que les éclats retombent et s'estompent,
   le logo Yogi Studio apparaît au centre (le cercle safran se trace, puis le personnage
   apparaît avec un rebond), accompagné de la mention « Yogi Studio présente »
5. **3,6 – 4,5 s — Le titre et l'ouverture** : le logo Yogi Studio s'efface, et les derniers éclats
   de bois s'assemblent en vol pour former le logotype **« Molkky Board »** au centre. Après un court
   temps de pause, le logotype glisse et se réduit vers l'en-tête de l'accueil pendant que le menu
   apparaît derrière (transition à éléments partagés : c'est le même logotype qui sert de titre à l'accueil)

### Contraintes
- 60 images/s visées sur une machine modeste : nombre de particules borné (≈ 150 au total),
  aucune allocation dans la boucle de dessin, calcul des trajectoires précalculé ou en arithmétique simple
- Résultat déterministe (graine aléatoire fixe) pour que les tests de capture Roborazzi soient stables
- Fonctionne en mode clair et sombre (fond `#14201A` en sombre)
- Son d'impact optionnel, désactivé par défaut, respectant le mode silencieux du téléphone
- Test UI : vérifier qu'un tap passe bien l'animation et que le menu s'affiche même si le chargement échoue
- Montre-moi une capture de chacune des 5 étapes (via `android screen capture` ou Roborazzi)
  avant de finaliser les réglages de timing

---

## Spécifications complémentaires (V1)

### Équipes nommées
- Une partie se joue en solo ou en équipes. Chaque équipe a un **nom**, une **couleur** (palette des 8 teintes) et un **emblème** (une icône simple parmi une petite banque : quille, feuille, éclair, vague…)
- Bouton « 🎲 » qui propose un nom d'équipe amusant au hasard (liste de ~50 noms en français autour du Mölkky, du bois et de la Bretagne/Finlande), toujours modifiable
- Une équipe est identifiée par **l'ensemble de ses joueurs**. Si les mêmes joueurs se regroupent, l'app propose de reprendre l'équipe existante et son nom, pour que son palmarès s'accumule
- Ordre de passage alterné entre les équipes, et rotation des lanceurs au sein de chaque équipe ; les deux ordres sont modifiables par glisser-déposer
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
- Si une correction de lancer annule la victoire, l'écran de fin se referme et la partie reprend
- Version allégée, un simple fondu avec le trophée, si l'option système « Réduire les animations » est active

### Annonces vocales
- Option « Annonces vocales » en synthèse vocale Android hors ligne, désactivée par défaut :
  « Au tour de Léa, il lui faut 7 », « Retour à 25 ! », « Léa est éliminée », « Victoire de l'équipe Les Bouleaux »
- Volume et débit réglables ; respecte le mode silencieux ; pas d'annonce pendant TalkBack si cela fait doublon

### Coach de fin de partie
- À partir de 38 points, afficher ce qu'il faut pour gagner (« Il te faut 7 : la quille 7 seule, ou 7 quilles »),
  surligner la quille gagnante sur la grille, et signaler la zone de danger quand le risque de dépasser 50 est élevé
- Désactivable dans les réglages

### Règles consultables
- Écran « Règles » accessible en un tap pendant la partie, avec des schémas animés
  (formation de départ, retour à 25, élimination)

### Mode famille / handicap
- Score de départ personnalisé par joueur, objectif réduit, élimination désactivable pour certains joueurs,
  rappel de la distance de lancer conseillée
- Ces parties sont exclues des records « purs » mais comptent dans les victoires

### Hall of Fame
Écran dédié, accessible depuis l'accueil, avec des onglets **Joueurs** et **Équipes**.

Classements :
- **Nombre de victoires**, avec le nombre de parties jouées et le taux de victoire
- **Victoire la plus rapide**, en nombre de lancers de l'équipe ou du joueur gagnant.
  Le minimum théorique est de 5 lancers (4 × 12 + 2) : une victoire en 5 lancers est une
  « partie parfaite » et mérite un badge spécial
- Pour l'onglet Équipes, préciser le nombre de joueurs de l'équipe et proposer un filtre par taille d'équipe

Chaque record affiche la date de la partie et un lien vers son déroulé.

Périmètre :
- **Local** : toutes les parties jouées sur ce téléphone
- **Groupe** : un groupe (famille, amis, club) réunit des comptes. Son Hall of Fame est partagé et synchronisé entre ses membres. Une partie partagée est rattachée à un groupe à sa création, ce rattachement étant facultatif
- Filtres : tout le temps, cette année, ce mois-ci

Contraintes techniques :
- **Toutes les statistiques sont calculées à partir du journal des lancers** et non stockées en dur, afin de pouvoir ajouter de nouveaux classements plus tard en les appliquant rétroactivement
- Seules les parties terminées normalement comptent. Les corrections faites **pendant** la partie sont normales
  et n'ont aucun effet sur les records. Une partie abandonnée, ou modifiée **après** sa fin, est marquée et
  exclue des records, mais reste dans l'historique
- Les lancers saisis en mode rapide comptent pour tous les classements sauf ceux qui portent sur une quille précise
- Tests unitaires sur le calcul des classements, notamment pour les égalités (départage par date : le premier à l'avoir réalisé garde le record)

### Notification de partie en cours
- Notification persistante : à qui c'est le tour et les scores, avec action rapide « Saisir le lancer »

### Données et confiance
- **Contrôle des données (RGPD)** : export JSON, suppression du compte et des données depuis l'app,
  minimisation (un pseudo suffit, pas de nom réel obligatoire)
- **Sauvegarde / restauration** locale (fichier) pour le mode sans compte

### Compléments (après le reste de la V1, avec validation)
- **Records secondaires** : plus grand nombre de « 12 » réussis, plus longue série sans raté, nombre de retours à 25, plus belle remontée
- **Badges** de joueur : première victoire, partie parfaite, « Phénix » (victoire après un retour à 25), « Sniper » (trois 12 dans une partie)
- **Revanche** : relancer en un tap une partie avec les mêmes équipes, selon la règle d'ordre de la manche suivante

---

## Partage social et viralité (V1)
Objectif : chaque partie doit pouvoir devenir en deux taps une image ou une courte vidéo
assez belle pour être publiée en story, en reel ou dans un groupe WhatsApp, et chaque
publication doit donner envie d'installer Molkky Board ou de rejoindre une partie.

### Moments partageables (détectés automatiquement à partir du journal)
- **Victoire** : vainqueur, score final, classement, nombre de lancers
- **Record** battu ou **partie parfaite** (victoire en 5 lancers)
- **Remontée** : victoire après avoir été mené de 15 points ou plus
- **Moments malheureux** (ton humoristique) : retour à 25 à 49 points, élimination après trois ratés
- **Bilan** : du mois ou de la saison de groupe (victoires, records, meilleur lanceur)
- Après une partie, l'écran de fin propose le moment le plus fort avec un bouton « Partager » bien visible ;
  le partage est **toujours proposé, jamais imposé**, et ne bloque jamais l'enchaînement « Revanche »

### Vignettes image
- Formats : **9:16** (stories, 1080×1920), **4:5** (fil Instagram, 1080×1350) et **1:1** (1080×1080) ;
  le format par défaut est 9:16
- 3 à 4 modèles graphiques dans la direction « Nordic Bold » (par exemple « Podium », « Tableau de score »,
  « Record », « Aïe ! » pour les moments malheureux), choisis automatiquement selon le moment et modifiables
  d'un swipe dans l'aperçu
- Contenu : titre du moment, noms et couleurs des équipes, emblèmes ou avatars, score, chiffre clé
  (« 50 en 7 lancers »), date, lieu s'il a été saisi
- Signature systématique et discrète : logotype **Molkky Board** et un **QR code** ou un lien court
  (vers la fiche de l'app, ou vers une invitation à rejoindre le groupe si l'auteur le choisit)
- Rendu directement depuis des composables Compose capturés en bitmap (`GraphicsLayer`), sans serveur

### Mini-vidéos (reels / stories)
- Clip vertical 9:16 de **6 à 10 secondes**, généré **sur le téléphone** à partir du journal de la partie :
  1. ouverture avec les quilles qui volent en éclats (reprise courte de l'animation de l'intro) ;
  2. **la course vers 50** : les scores de chaque équipe progressent lancer après lancer sous forme de
     barres ou de jauges animées, avec les retours à 25 et les éliminations mis en scène ;
  3. le lancer décisif au ralenti, puis le vainqueur avec la pluie de confettis-quilles ;
  4. carton final avec le score, le logotype Molkky Board et le QR code
- Encodage MP4 H.264 hors ligne : images rendues en Canvas puis encodées avec **Media3 Transformer**
  (ou MediaCodec), avec une barre de progression et la possibilité d'annuler ; viser moins de 10 s
  de génération sur un téléphone de milieu de gamme
- Son : effets sonores maison (impact, confettis) libres de droits, ou vidéo muette ;
  **aucune musique sous droits** (l'utilisateur ajoute sa musique dans Instagram ou TikTok)
- Option « Sans vidéo » : génération d'un GIF animé court, plus léger, pour les messageries

### Partage
- Feuille de partage Android standard avec le fichier généré (via `FileProvider`), plus des raccourcis
  directs vers Instagram Stories, WhatsApp et TikTok lorsqu'ils sont installés (vérifier les intents
  de partage officiels de chaque app et ce qu'ils exigent avant de les intégrer)
- Possibilité d'enregistrer dans la galerie
- Texte d'accompagnement proposé et modifiable, par exemple
  « 🏆 Les Bouleaux gagnent en 7 lancers ! Partie comptée avec Molkky Board #molkky »

### Boucle virale
- Le QR code et le lien des vignettes mènent soit à la fiche de l'app, soit, au choix de l'auteur,
  à une **invitation à rejoindre son groupe** (accès au Hall of Fame commun et aux prochaines parties)
- Bouton « **Défier** » : envoie un lien qui propose une revanche aux mêmes équipes
- Pas de récompense contre le partage et pas de relances insistantes : une seule suggestion par partie

### Vie privée (prioritaire sur la viralité)
- Par défaut, les vignettes n'affichent **que les pseudos et les avatars emoji ou mascotte**, jamais de photo
- Les photos de joueurs n'apparaissent que si l'auteur l'active explicitement, pour la vignette en cours
- Avant de partager une partie qui contient d'autres joueurs, rappel discret :
  « Les autres joueurs sont d'accord pour apparaître ? », avec possibilité de masquer un nom
  (remplacé par « Joueur 2 »)
- Aucune métadonnée de localisation dans les fichiers générés (EXIF supprimées)
- **Aucun SDK de tracking** : la mesure de la viralité, si elle est souhaitée, se limite à un compteur
  anonyme d'ouvertures des liens d'invitation côté serveur, sans identifiant de l'utilisateur ni de l'appareil.
  Propose-moi l'option la plus respectueuse avant de l'implémenter

### Tests
- Captures Roborazzi de chaque modèle dans les trois formats, avec des noms longs, 2 et 6 équipes,
  et en mode « noms masqués »
- Test d'encodage vidéo sur l'émulateur : durée, résolution et taille du fichier (moins de 8 Mo pour 10 s)

---

## Backlog V2 et au-delà
Règle : ne commencer une fonctionnalité de cette liste qu'une fois la V1 terminée.
Avant chaque lot, proposer un découpage et attendre ma validation.
Priorités : **P2** = V2, **P3** = plus tard.

### Aide au jeu
- **P2 — Saisie vocale** hors ligne (« sept », « deux quilles », « raté », « annule »), sans envoi audio sur un serveur, avec confirmation visuelle
- **P3 — Commentateur** : phrases humoristiques contextuelles ajoutées aux annonces vocales

### Modes de jeu
- **P2 — Mode entraînement solo** : défis et suivi de progression
- **P2 — Tournoi** : poules puis élimination directe, arbre du tableau, plusieurs terrains en parallèle
- **P3 — Variantes de jeu** proposées par certains concurrents (par exemple « Killer » ou « mort subite »),
  chacune étant une stratégie de règles branchée sur le même moteur
- **P3 — Saisons** de groupe avec podium de fin de saison
- **P3 — Défi du jour** commun aux membres d'un groupe

### Statistiques et classement
- **P2 — Classement Elo** au sein d'un groupe (ou modèle type TrueSkill pour les équipes), avec courbe d'évolution
- **P2 — Face-à-face** entre deux joueurs ou équipes
- **P2 — Carte de précision** : fréquence de chaque quille touchée seule, sur la formation de quilles (lancers en mode « Quilles » uniquement)
- **P3 — Statistiques par terrain** (lieu saisi manuellement, pas de GPS par défaut)

### Affichage et partage
- **P2 — Mode paysage** de l'écran de jeu, avec la manche en cours et la précédente côte à côte
- **P2 — Mode tableau d'affichage** paysage pour tablette, en lecture seule
- **P2 — Lien spectateur** web en lecture seule, alimenté en temps réel
- **P2 — Widget d'écran d'accueil** (Jetpack Glance)
- **P2 — Export** de l'historique en CSV
- **P3 — Photos** d'équipe et souvenirs de partie

### Personnalisation
- **P2 — Habillages de quilles** : bois naturel, couleurs vives, monochrome
- **P2 — Mode daltonien** : couleurs d'équipe doublées d'un motif ou d'un emblème
- **P3 — Langues** : français, anglais, finnois

### Publication
- **P2 — Préparation Play Store** : vérification préalable du nom au regard de la marque « Mölkky »
  (prévoir un nom de repli sans ce mot), icône adaptative avec variante monochrome, politique de confidentialité,
  formulaire « Sécurité des données » (aucune donnée collectée en mode sans compte, aucune publicité),
  captures générées à partir des tests Roborazzi
- **P2 — Modération légère** des noms d'équipes et pseudos par l'organisateur du groupe

---

## Contraintes de qualité
- Le moteur de score doit avoir une couverture de tests complète, alignée sur SCORING_SPEC.md,
  y compris les corrections de lancers anciens et les changements de joueurs en cours de partie ;
- Pas de clé ni de secret dans le code ;
- Commits réguliers et explicites ; un README qui explique comment builder et lancer Molkky Board ;
- Interface en français, avec des chaînes externalisées pour pouvoir traduire plus tard.

---

## Décisions

> Mise à jour à chaque phase. Format : date — décision (statut).

### Règles (Phase 1 — validée le 2026-10-25)
- 2026-10-25 — Interprétations ⚠ de `RULES.md` / `SCORING_SPEC.md` acceptées : faute « mauvaise équipe » = un seul raté (prochain lancer régulier) + règle des 37 appliquée au moment de la faute ; égalité au Mölkkout = tour supplémentaire complet ; préréglage par défaut « Classique ».
- 2026-10-25 — Convention documentation/UI en français, identifiants en anglais : acceptée.
- 2026-10-25 — Référence : règlement de tournoi IMO (30/03/2019) + règles consensuelles molkky.world + copie FFMO (2017). Détail et divergences dans `docs/RULES.md`.
- 2026-10-25 — Deux préréglages proposés : « Classique » (par défaut) et « Tournoi IMO » ; toutes les variantes de `RULES.md` § 8 deviennent des options de `GameConfig`.
- 2026-10-25 — Le compteur de ratés est tenu par équipe ; l'état d'une manche est toujours recalculé par rejeu du journal des événements (ce qui donne l'undo et la correction).

### Architecture / backend (Phase 2 — validée le 2026-10-25, détail dans `docs/ARCHITECTURE.md`)
- 2026-10-25 — Modules : `core:{model, scoring, designsystem, ui, database, datastore, network, data, testing}`, `feature:{home, setup, game, summary, history, stats, players, settings, auth, join}`, `build-logic/`. Les features ne dépendent pas entre elles ; `core:scoring`/`core:model` en Kotlin pur.
- 2026-10-25 — Journal d'événements en ajout seul (Undo et Correct sont des événements) ; l'état est toujours obtenu par `replay()` ; un événement invalide est ignoré de façon déterministe.
- 2026-10-25 — Offline-first : Room est l'unique source de vérité de l'UI ; l'état de partie n'est pas stocké, seuls les événements et les `set_result` le sont.
- 2026-10-25 — Backend : **Supabase en région UE — Paris (`eu-west-3`)**. Aucune écriture directe : RLS n'autorise que le SELECT aux membres, les écritures passent par des RPC `security definer` (`append_event` avec `expected_seq`, idempotente sur l'UUID). Tests RLS avec pgTAP.
- 2026-10-25 — Conflits : ordre attribué par le serveur ; en cas de `SEQ_CONFLICT`, le client rattrape le journal et rejoue l'événement en attente, qui est renvoyé ou passe en REJECTED.
- 2026-10-25 — Ajout de WorkManager (+ hilt-work) à la stack pour la boîte d'envoi.
- 2026-10-25 — Auth : **code OTP reçu par e-mail** (pas de lien magique tant qu'il n'y a pas de deep link), mot de passe en option, Google via Credential Manager, connexion anonyme pour les invités. Code de partie de 6 caractères sur 31 symboles, expirant (24 h / fin de partie), avec limitation de débit.
- 2026-10-25 — RGPD : profil minimal (pseudo, couleur), pas d'analytics ni de Crashlytics, avatars uniquement locaux, `delete_my_account()` avec anonymisation, pas de logs en release.
- 2026-10-25 — **Liens d'invitation (schéma `molkky://` ou App Links) reportés à plus tard.** En v1, on rejoint une partie en saisissant le code à 6 caractères ; le QR code ne contient que ce code (le scan remplit le champ) et l'app ne déclare aucun intent-filter de deep link. Pas de domaine pour l'instant.
- Point ouvert : durée de conservation (J9).

### Équipes, Hall of Fame, groupes (spécifications complémentaires — 2026-10-25, détail dans `docs/ARCHITECTURE.md` § 10)
- Nouveau module `core:stats` (Kotlin pur) : toutes les stats et tous les classements sont calculés depuis les journaux. La table `set_result` est abandonnée ; un éventuel cache sera jetable et versionné.
- Identité d'équipe = `memberKey` (SHA-256 des identifiants de joueurs triés) ; une équipe d'un joueur = ce joueur.
- Statut dérivé : nouvel événement `Abandon` ; `corrected` = présence d'un `Correct` (un Undo ne compte pas). Seules les manches FINISHED, ni abandonnées ni corrigées, sont éligibles aux records. Cas S1–S6 ajoutés à `SCORING_SPEC.md`.
- Victoire la plus rapide = lancers de l'équipe gagnante dans la manche (fautes comprises, Mölkkout exclu) ; à égalité, la date la plus ancienne l'emporte, puis l'identifiant de partie.
- Animation de victoire déclenchée seulement en direct (y compris à la réception Realtime) ; pas d'animation à la réouverture ni pour un rattrapage de plus de 30 s.
- Groupes : tables `groups`, `group_members`, `group_guests` et `teams` par groupe, `games.group_id` facultatif ; les membres du groupe ont un accès en lecture ; le Hall of Fame de groupe est calculé sur chaque téléphone.
- 2026-10-25 — Validé : le record de vitesse ne compte que les règles standard (cible 50, → 25) ; l'onglet Joueurs ne prend que les parties en solo ; la revanche (mêmes équipes, ordre inversé) est avancée au J5.
- Jalons : équipes nommées → J4 ; animation et Hall of Fame local → J5 ; groupes → J9 ; compléments optionnels → J11, après validation.

### Backlog avancé (reçu le 2026-10-25, à ne démarrer qu'après le MVP et les specs complémentaires)
- Préparer le backlog sans l'implémenter (2026-10-25) :
  - `GameConfig` accepte des **surcharges par équipe** (score de départ, cible, exemption d'élimination) pour le mode handicap ; un drapeau dérivé `handicap` exclut des records « purs ».
  - Le jeu de quilles et la grille passent par une abstraction de skin ; les couleurs d'équipe sont toujours accompagnées de l'emblème (daltonisme).
  - Aucune chaîne en dur : tout passe par `strings.xml` (fr par défaut, en/fi plus tard).
  - `game.place` (texte libre, facultatif) prévu dans le schéma.
  - Un `GameSession` partagé (Flow d'UiState) servira à la notification, au widget et au tableau d'affichage.
  - Tous les futurs classements (Elo, face-à-face, carte de précision) se branchent sur `core:stats`.
  - Lien spectateur et liens d'invitation nécessitent un domaine : reportés ensemble.

### Prompt et environnement (2026-10-25)
- 2026-10-25 — Fusion de `prompt-molkky.md` dans ce fichier : Phase 5 complétée (configuration du build release signé, keystore hors dépôt), ajout de la section « Environnement de développement » et des « Contraintes de qualité » (README, commits réguliers, aucun secret dans le code, couverture complète du moteur de score).
- Builds en ligne de commande uniquement (Android Studio sert seulement pour les `@Preview`) ; réglages Gradle sobres pour un PC peu puissant.
- Tests visuels sur émulateur créé via Android CLI : après chaque build → installation, lancement, `android screen capture`, vérification avant présentation.

### Écran de démarrage et Yogi Studio (spec reçue le 2026-10-25, `prompt-splashscreen.md`)
- Éditeur : Yogi Studio. Logo distinct de l'icône de Molkky Board (Nordic Bold, à concevoir : quille stylisée + « MB » ou tableau de score, lisible en 48 dp).
- Étape 5 de l'intro (mise à jour du 2026-10-25) : les éclats s'assemblent en logotype « Molkky Board », qui glisse ensuite vers l'en-tête de l'accueil (élément partagé).
- SplashScreen système minimal (icône sur bouleau) → écran d'intro Compose (≤ 4,5 s, passable d'un tap) ; navigation vers l'accueil quand animation ET chargement sont finis ; chargement lancé depuis `Application.onCreate`.
- Planification proposée : intro au J5 (après le design system J2 et DataStore J3) ; « À propos » au J5 ; captures des 5 étapes avant de finaliser les timings.
- Fichiers de marque reçus le 2026-10-25 et rangés dans `docs/brand/`. Conversion au J5 : l'icône en VectorDrawable (l'arc à tirets devient un `trimPath`), le texte « Yogi STUDIO » du logo est rendu en Compose avec Inter (un VectorDrawable ne gère pas le texte).

### Squelette et moteur (Phase 3, J0–J1 — 2026-10-25)
- 2026-10-25 — J1 validé (les 5 choix de `SCORING_SPEC.md` § 14 acceptés) ; style ktlint `ktlint_official` conservé.
- J0 : Gradle 9.8.2, AGP 9.4.1 (Kotlin intégré), Kotlin 2.4.21, KSP 2.3.12, compileSdk/targetSdk 37, JVM cible 17 compilée avec le JDK 21.
- Material 3 stable = 1.4.0 : les composants Expressive (1.5) ne sont pas encore stables, donc non utilisés. detekt 1.23.8 (la 2.0 est en alpha).
- Style : ktlint `ktlint_official`, detekt avec clauses de garde autorisées ; JUnit Jupiter (JUnit 6) pour les modules Kotlin purs.
- J1 : Undo/Correct ciblent un `EventId` ; précisions de règles dans `SCORING_SPEC.md` § 14. Couverture Kover : 100 % des lignes, ≥ 97 % des branches depuis le moteur v2 (seuil vérifié en CI).

### Identité « Molkky Board » (reçue le 2026-10-25)
- Nom de l'app : **Molkky Board** ; « Mölkky » (avec tréma) désigne le jeu dans les textes. Éditeur : Yogi Studio.
- Renommage appliqué le 2026-10-25 : applicationId et packages `com.yogistudio.molkkyboard` (`.debug` en debug), modules `com.yogistudio.molkkyboard.core.*`, `rootProject.name = "MolkkyBoard"`, `MolkkyBoardApp`, `MolkkyBoardTheme` (fonction + objet d'accès aux tokens), préfixe `MB` des tokens (`MBColors`, `MBTypography`, `MBMotion`, `MBDimens`), convention plugins `molkkyboard.*`. Dossier local `molkky` inchangé.
- Schéma `molkkyboard://` réservé ; les liens profonds restent reportés (aucun intent-filter en v1, décision du 2026-10-25 maintenue).
- Logotype « Molkky Board » (Bricolage 800, « Molkky » vert forêt + « Board » safran, même ligne de base) : composant `MolkkyBoardLogotype` du design system, livré au J2. Icône du lanceur : proposition au J5 avec l'intro.
- Play Store (P2) : vérifier le nom au regard de la marque « Mölkky », prévoir un nom de repli.

### Prompt du 2026-10-25 (soir) — décisions validées le même jour
- Moteur v2 (`SCORING_SPEC.md` § 15) : **attribution des lancers par position** dans le journal ; saisie « Quilles » ou « Rapide » ; `Delete` de n'importe quel lancer ; joueurs ajoutés (en fin d'ordre, à 0), retirés (classés derniers, « parti »), réordonnés **entre deux tours** ; règles sous forme de stratégie `ScoringRules`.
- Records : seules les modifications **après la fin** excluent une partie ; l'annulation immédiate du lancer gagnant est tolérée.
- Partage social : QR et « Défier » vers la **fiche Play Store** + code à 6 caractères ; App Links quand un domaine existera.
- Anglais : sélecteur de langue au J3, traduction complète au J7 (fin de V1).
- Jalons replanifiés (`ARCHITECTURE.md` § 9) : J1b moteur v2, J3 données, J4 jeu, J5 fin de partie, J6 partage social, J7 confiance et langues, puis Phase 4.

### Navigation, langues, tests (2026-10-25)
- Navigation validée : **barre du bas** (Accueil, Historique, Hall of Fame, Réglages) ; « À propos » dans Réglages ; écran de jeu en plein écran avec un menu « ⋮ » (déroulé, joueurs, règles, abandon). Squelette en place dans `app` (commit 536e3fb), écrans déplacés dans `feature:*` au J4.
- Jauge : 3 pulsations quand la victoire est à un lancer, puis anneau fixe (une animation infinie provoquait des ANR et coûterait de la batterie).
- **Anglais demandé** (en plus du français) avec un **choix de langue dans les Réglages** : `values-en/strings.xml`, langue par application (`localeConfig` sur Android 13+, AppCompat `setApplicationLocales` en dessous). À planifier dans les jalons.
- Tests manuels : émulateur sans fenêtre (`--headless`) pour mes vérifications ; tests de l'utilisateur sur son téléphone.

### Écran de démarrage (avancé à la demande, 2026-10-25 — commit 328de9a)
- Fait : icône adaptative proposée (quille dans une jauge de score, variante monochrome), SplashScreen système, logo Yogi Studio en Compose, intro en 5 étapes (`feature:intro`), `StartupLoader`, logotype partagé vers l'accueil, captures des 5 étapes et tests de comportement.
- 2026-10-25 — Retour du test sur Fairphone 6 : animation jugée bonne ; demandé plus spectaculaire → quilles +30 %, explosion qui sort de l'écran en moins d'une seconde, puis **fondu** du logotype Molkky Board avant le menu (remplace l'assemblage des éclats de l'étape 5). Signature Yogi Studio conservée entre les deux (à confirmer).
- En attente de validation : timings, icône de l'app.
- Restent à faire (dépendances) : version courte à partir du 2e lancement et réglage associé (DataStore, J3) ; son d'impact optionnel ; vrais chargements (Room, DataStore, partie en cours, session).

### Écran de jeu sans défilement (retour du test sur Fairphone 6, 2026-10-25)
- L'intro fonctionne bien sur le téléphone.
- **L'écran de jeu doit tenir sur un seul écran, sans défilement** : composant `FitToScreen` (réduction homothétique si la place manque, taps et accessibilité compris). Au J4, l'écran de jeu sera aussi rendu adaptatif (grille de quilles dimensionnée selon la hauteur disponible), `FitToScreen` restant le filet de sécurité.

### Données locales (J3 — 2026-10-25)
- Modules `core:database` (Room v1, schémas exportés dans `core/database/schemas`), `core:datastore` (réglages), `core:data` (dépôts `GameRepository`, `SettingsRepository`).
- Journal : chaque événement est validé par le moteur (`checkInput` ou rejeu) **avant** d'être écrit, dans une transaction courte ; un événement refusé n'est jamais écrit. L'état est recalculé par rejeu à chaque changement.
- Reprise après mort du processus : testée en fermant puis rouvrant la base depuis son fichier (Robolectric). Le test instrumenté sur appareil viendra avec l'écran de jeu (J4).
- Langue par application : `AppCompatActivity` + `AppCompatDelegate.setApplicationLocales`, `locales_config.xml` (fr, en) ; l'anglais affiche encore le français jusqu'à la traduction (J7).
- Intro courte (≈ 1,6 s, impact puis logotype) au choix à partir du 2e lancement ; l'écran de démarrage système reste affiché le temps de lire les réglages.

### Jouer (J4, 1re livraison — 2026-10-25)
- `feature:setup` (joueurs par nom, ordre au hasard, préréglage Classique / Tournoi IMO, lancement dès 2 joueurs) et `feature:game` (vrai écran de jeu branché sur le moteur et Room).
- Écran de jeu adaptatif : `PinGrid` dimensionne les quilles selon la place disponible ; pavé rapide dans `FitToScreen` ; bandeau des autres joueurs (scores jamais tronqués).
- « Raté » **sélectionne** le raté, seul « Valider » enregistre. Undo en barre de titre et dans la snackbar.
- Dépassement : animation « Aïe ! Retour à 25 » (autocollant corail penché, carte qui tremble, score qui dégringole), demandée le 2026-10-25.
- Accueil : carte « Partie en cours » (scores, à qui le tour, « Reprendre »).
- Demandé le 2026-10-25 : **tableau récapitulatif de série** quand les mêmes équipes enchaînent les parties (victoires, scores de chaque partie, total) — prévu au J5 avec « Revanche », calculé depuis les journaux.
- Test sur le téléphone de l'utilisateur à la fin du J4 (les deux livraisons).

### Conventions de code
- Documentation et UI en français ; identifiants de code en anglais.
