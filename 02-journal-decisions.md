# Journal des décisions — Molkky Board

Historique de la conception, dans l'ordre où les décisions ont été prises (25 octobre 2026).
La spécification à jour fait foi : ce journal explique **pourquoi** elle est ainsi.

## Le projet
- Application Android native pour compter les points du Mölkky, simple, intuitive et soignée.
- Plusieurs joueurs peuvent suivre et saisir une même partie depuis plusieurs téléphones,
  d'où des comptes utilisateurs et une synchronisation temps réel.
- Développée avec **Claude Code**, par phases avec validation à chaque étape :
  règles → architecture → MVP local → comptes et multi-appareils → build.
- La spécification sert de prompt initial et est recopiée dans `molkky/CLAUDE.md` pour que
  Claude Code la recharge à chaque session.

## Identité
- **Nom de l'application : Molkky Board.** Éditeur : **Yogi Studio**.
- Identifiant : `com.yogistudio.molkkyboard`. Le dossier local reste `molkky`.
- « Mölkky » désigne le jeu ; « Molkky Board » désigne l'application.
- Vérification du nom : aucune app « Molkky Board » trouvée ; un jeu de société japonais porte ce nom
  (autre catégorie). **« Mölkky » est une marque protégée** (Tactic Games Oy) : à vérifier
  (EUIPO, INPI) avant publication, avec un nom de repli sans « Molkky ».
- Logo Yogi Studio : une quille de Mölkky en posture de yoga dans un cercle zen safran,
  avec des éclats de bois. Ce n'est pas l'icône de l'app, qui reste à concevoir.

## Choix techniques
- Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Navigation Compose typée.
- Moteur de score en Kotlin pur, qui recalcule l'état à partir d'un **journal d'événements**.
  Ce choix permet l'undo, la correction de n'importe quel lancer passé, la synchronisation
  multi-appareils et les statistiques rétroactives.
- **Toutes les statistiques sont calculées depuis le journal**, jamais stockées en dur, pour
  pouvoir ajouter des classements plus tard sans migration.
- Backend : Supabase ou Firebase, à trancher en phase 2. Critères : hébergement UE, minimisation,
  RLS, et **souveraineté** (Cloud Act) — option Supabase auto-hébergé chez Scaleway/OVHcloud à évaluer.
- QR code : génération avec **zxing core**, scan avec **CameraX + zxing-cpp**.
  ML Kit écarté car il envoie des statistiques d'usage à Google même en version embarquée.
  Saisie manuelle du code à 6 caractères en secours.
- Licences open source : **AboutLibraries** (le plugin de Google dépend des Play services).
- Animations en Compose natif, sans Lottie. Tests Roborazzi pour les captures.
- Test sur l'émulateur du PC d'abord, sur le Fairphone ensuite.

## Direction artistique « Nordic Bold »
- Design scandinave, bois de bouleau évoqué par la couleur et la forme, sans texture réaliste.
- Palette : bouleau `#F4EBDD`, vert forêt `#1F4D3A`, safran `#F2A93B`, corail `#E8604C`.
- Polices : Bricolage Grotesque (titres, scores), Inter (texte). Scores énormes en chiffres tabulaires.
- Mouvements en ressort, retours haptiques, respect de « Réduire les animations ».

## Écran de démarrage
- Animation de 4,5 s maximum, passable d'un tap : quilles en formation, lancer du mölkky,
  **strike** (les 12 quilles renversées d'un coup), apparition de Yogi Studio, puis le logotype
  « Molkky Board » qui glisse vers l'en-tête de l'accueil.
- *26 octobre* : les quilles ne volent plus en éclats. Elles restent entières et s'envolent hors de
  l'écran dans un style manga (image d'impact figée, lignes de vitesse, onomatopée « 12 ! »,
  scintillement), mais toujours en aplats « Nordic Bold », sans trame ni encrage. Le premier
  storyboard trahissait l'idée d'origine, qui était un strike.
- Le chargement de l'app se fait en tâche de fond, indépendamment de l'animation.

## Fonctionnalités ajoutées au fil de la conception
- **Équipes nommées** (nom, couleur, emblème, nom aléatoire amusant) dont le palmarès s'accumule.
- **Animation de victoire** (confettis en forme de quilles), synchronisée sur tous les téléphones.
- **Hall of Fame** joueurs et équipes : victoires, victoire la plus rapide (5 lancers = partie parfaite).
- **Coach de fin de partie**, **mode famille / handicap**, **annonces vocales**.
- **Guide des règles** illustré et **schéma interactif de placement des quilles**, avec rappel en
  début de partie pour les débutants et raccourcis sur l'icône de l'app.
- **Partage social** : vignettes 9:16 / 4:5 / 1:1 et mini-reels de 6 à 10 s générés sur le téléphone,
  avec logotype et QR code pour faire connaître l'app. Pseudos et avatars emoji par défaut, jamais de
  photo sans action explicite, pas de musique sous droits.

## Ajouts du 26 octobre 2026
- **Rappel du tour courant** (« Tour 3 ») sur l'écran de jeu, calculé depuis le journal comme le reste.
  Il est repris dans le déroulé, les annonces vocales, la notification et le partage.
- **Logotype compact** (environ 22sp) en haut à gauche de l'accueil : le titre prenait trop de place.
- **Tableau des scores en temps réel** : on tire vers le haut le bandeau des scores pour afficher le
  classement complet (score, reste à faire, ratés, éliminés), puis on le referme sans perdre la saisie.
  Conséquence : l'**undo** devient un bouton et non un geste, pour qu'il ne soit pas confondu avec ce
  glissement. Un tap et une action TalkBack ouvrent aussi le tableau, pour ne pas dépendre d'un geste.
- Onomatopée de l'intro retenue : **« 12 ! »**. Elle est reprise en jeu dans une **animation « 12 ! »**
  simplifiée (environ 1 s) à chaque lancer de 12 points : quille 12 seule qui s'envole, ou les 12 quilles
  pour un strike. La victoire passe avant, et un 12 qui fait dépasser 50 ne déclenche pas la célébration.
- **Animation d'élimination** au troisième raté (tampon « Éliminé », quille-avatar qui tombe), réversible
  si une correction annule l'élimination, synchronisée sur tous les téléphones.

## Ajouts du 28 octobre 2026 — « Partie à côté » (multi-téléphones sans serveur)
- **Constat** : le cas le plus courant est celui de téléphones sur le même terrain. Un serveur et des
  comptes n'y sont pas nécessaires ; ils ajoutent des données personnelles, un sous-traitant et un point
  de panne quand il n'y a pas de réseau mobile.
- **Décision** : nouvelle **phase 3 bis**. Le téléphone de l'organisateur devient l'**hôte** (autorité qui
  valide et ordonne le journal, le rôle prévu pour le serveur). Les autres téléphones le rejoignent en
  scannant un QR code, via le même Wi-Fi ou un **point d'accès local sans Internet** créé par l'hôte.
- **Architecture** : un module `core:sync` porte un protocole d'événements unique et une abstraction
  de transport (réseau local en phase 3 bis, serveur en phase 4). La validation des événements est une
  fonction pure partagée entre l'hôte local et le futur serveur.
- **Briques uniquement open source** : Ktor (serveur et client CIO, WebSockets), Conscrypt (TLS 1.3),
  Bouncy Castle ou Android Keystore pour le certificat de partie, Tink, zxing, API Android AOSP.
  **Google Nearby Connections écarté** : propriétaire, dépendant des Play services, et il collecte des
  métriques d'usage. La pile reste compatible F-Droid.
- **Sécurité inspirée de LocalSend et du protocole d'appairage de Briar** (spécifications lues, aucun code
  copié ; Briar est sous GPL) : certificat éphémère dont l'empreinte est dans le QR code et épinglée par
  l'invité ; QR code considéré comme **observable**, donc sans secret durable (jeton à usage unique, 2 min) ;
  **validation humaine** de chaque arrivée par l'organisateur ; jeton de session par téléphone.
- **Pas de code court à saisir** dans ce mode : il ne peut pas porter l'empreinte du certificat.
  En secours, l'organisateur saisit pour le joueur sur son propre téléphone.
- **Pas de localisation, jamais** : le point d'accès local exige la localisation précise sur Android 12 et
  moins, donc ces hôtes n'ont que le mode « même Wi-Fi ». À partir d'Android 13, `NEARBY_WIFI_DEVICES`
  avec `neverForLocation`.
- **Android 17** : avec un targetSdk 37, l'accès au réseau local passe derrière la permission d'exécution
  `ACCESS_LOCAL_NETWORK`. Prévu dans la spec, à revérifier à l'implémentation.
- Service de premier plan `connectedDevice` sur l'hôte (sa notification est celle de la partie en cours).
- Les photos de joueurs ne transitent jamais ; seuls pseudo, avatar emoji ou mascotte et événements de jeu.
- QR code sous forme de lien `https://…/join#…` : paramètres après le `#`, jamais envoyés au site ;
  App Links vérifiés si l'app est installée, page d'installation sinon.
- Le QR code des vignettes de partage n'est jamais un QR d'invitation à une partie en cours.
- Un prototype sur deux appareils réels est exigé avant l'implémentation complète.

### Points ouverts (à trancher)
1. **Encart « Vie privée »** : « Sans compte, tout reste sur votre téléphone » devient inexact dès que
   plusieurs téléphones jouent ensemble. Nouvelle formulation proposée dans la spec, **non validée**.
2. **Phase 4 (comptes, serveur)** : la partie à côté couvre l'essentiel sans compte. Reporter les comptes,
   les groupes et le jeu à distance en V2 simplifierait la V1 (RGPD, registre, page de suppression de
   compte, accès de test pour Google). **Non décidé.**
3. **Formulaire « Sécurité des données »** : vérifier comment Google qualifie un transfert direct entre
   téléphones sans serveur.

## Leçons tirées de la concurrence
Les avis des apps existantes ont fait ajouter au MVP : deux modes de saisie (quilles / pavé rapide),
correction de n'importe quel lancer, réordonnancement des joueurs à tout moment, première partie guidée
au lieu d'une vidéo, reprise automatique des parties, prise en charge des grandes polices.
Détails dans `04-analyse-concurrence.md`.

## Publication
- **Compte développeur personnel**, nom affiché « Yogi Studio ».
- **Public : 13 ans et plus, pas les enfants** (pas de programme « Familles »).
- **Aucun revenu**, statut DSA non-commerçant. Un éventuel « Paye-moi un café » : uniquement sur le
  site web tant que la politique de paiements de Google n'a pas été vérifiée.
- Site web public séparé (`molkky-board-site`, GitHub Pages) : politique de confidentialité,
  suppression de compte, conditions d'utilisation, page `/join` et `assetlinks.json` pour les
  invitations. Le dépôt du code reste privé.
- Test fermé obligatoire : 12 testeurs pendant 14 jours (viser 15 à 20).
- Détails dans `05-publication-play-store.md`.

## Sécurité et vie privée
- Référentiel **OWASP MASVS v2**, recommandations CNIL, analyse de risques inspirée d'EBIOS RM.
- **Aucune donnée ne quitte les téléphones de la partie sans compte** : rien du tout sur un seul
  téléphone, uniquement du trafic chiffré sur le réseau local en partie à côté. Aucun traceur ni SDK
  tiers, rapports de plantage uniquement à la demande de l'utilisateur.
- Validation de chaque événement de partie par l'autorité (téléphone hôte ou serveur) :
  intégrité des scores et des records.
- Encart « Vie privée » dans l'écran « À propos », dont chaque affirmation doit rester vraie.
- Open source et F-Droid envisagés plus tard (P3).

## Prochaines étapes
1. Copier la spécification dans `~/molkky` et lancer Claude Code (phase 1 : règles).
2. Valider `RULES.md` et `SCORING_SPEC.md`, puis l'architecture (choix du backend).
3. Créer le dépôt GitHub privé `molkky-board` avec `gh`.
4. Recruter les testeurs du futur test fermé.
5. Trancher les trois points ouverts de la partie à côté, puis faire réaliser le prototype réseau local.
