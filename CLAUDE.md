# Molkky Board — prompt de référence

> Prompt de référence : `01-specifications-molkky-board.md` (version du 2026-10-28, « Partie à côté »), **mis à jour
> d'après les retours des tests sur téléphone** (27-28 octobre) : coach et mode débutant supprimés, placement en vue
> globale, intro courte retirée, icône podium au lieu du geste, emblèmes au trait au lieu des emoji, vignettes sans QR
> code ni masquage des noms, voix et sons coupés en silencieux seulement, écran en portrait, code publié sous GPL v3.
> Les passages concernés sont marqués « (retour du test) ». Les versions précédentes restent dans l'historique git.
> Les décisions prises au fil des phases sont dans la section `## Décisions` en fin de fichier ;
> le pourquoi des choix est raconté dans `02-journal-decisions.md`.


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
  - liens profonds : schéma `molkkyboard://`, puis App Links vérifiés sur le domaine du site de l'app
    (à définir au plus tard en phase 3 bis, voir « Partie à côté ») ;
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
une même partie depuis plusieurs téléphones. Quand les téléphones sont sur le même terrain
(le cas le plus courant), ils se synchronisent **directement entre eux, sans serveur, sans compte
et sans Internet** : c'est la « Partie à côté ». Des comptes et un serveur ne servent qu'au jeu
à distance et aux groupes (Hall of Fame partagé).

Une analyse des avis des apps concurrentes (octobre 2026) a fait ressortir leurs défauts récurrents :
prise en main confuse (on ne sait pas comment démarrer, tutoriel vidéo trop rapide), impossibilité
de corriger un lancer une fois le joueur suivant passé, ordre de passage figé, taps accidentels,
parties perdues à la fermeture de l'app, scores à deux chiffres tronqués en grande police,
historique incomplet, demande d'une saisie numérique rapide. Molkky Board doit être irréprochable
sur chacun de ces points. Nos différenciateurs :
- **multi-téléphones en temps réel** (rare sur Android) : rejoindre une partie en moins de 10 secondes
  via QR code, **sans compte ni serveur** quand les téléphones sont côte à côte ;
- **aucune publicité, aucun traceur**, aucune donnée collectée en mode sans compte ;
- équipes nommées avec Hall of Fame, mode famille, annonces vocales ;
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
   - la notion de **tour** : un tour est terminé quand chaque joueur ou équipe encore en jeu a lancé une fois ;
   - la fin de partie et le déroulé d'une série de manches ;
   - la **formation de départ** des quilles (schéma ASCII avec les numéros, vue depuis la ligne de lancer),
     la distance de lancer et la manière de relever les quilles ;
   - les **cas ambigus** (quille appuyée sur une autre ou sur le mölkky, quille sortie du terrain,
     faute de pied…), qui alimenteront la FAQ de l'écran « Règles du jeu ».
3. Liste les variantes courantes (ex. pas d'élimination, retour à 0 au lieu de 25,
   objectif différent) : elles deviendront des options paramétrables.
4. Écris `docs/SCORING_SPEC.md` : la spécification du moteur de score sous forme de
   cas de test (entrée = état + lancer, sortie = nouvel état), y compris :
   - la correction ou la suppression d'un lancer ancien et le recalcul complet qui en découle
     (un retour à 25 ou une élimination qui disparaissent, un vainqueur qui change) ;
   - l'ajout d'un joueur en cours de partie, le retrait d'un joueur, le changement d'ordre ;
   - le **numéro du tour courant**, déduit du journal (et non stocké), y compris quand un joueur est
     éliminé, retiré ou ajouté en retard (il rejoint le tour en cours) et quand on corrige un lancer passé ;
   - les deux modes de saisie (quilles touchées / saisie rapide) ;
   - les scores de départ et objectifs personnalisés du mode famille.

👉 Arrête-toi et montre-moi RULES.md et SCORING_SPEC.md.

---

## Phase 2 — Architecture (utilise le plan mode)
Propose une architecture et justifie les choix. Base suggérée, à challenger :
- **Kotlin + Jetpack Compose + Material 3**, une seule Activity, Navigation Compose ;
- **Architecture** : MVVM / UDF, modules `core:scoring` (Kotlin pur, sans dépendance Android),
  `core:data`, `core:designsystem`, `core:sync`, `feature:*` ;
- **Moteur de score** : fonction pure qui recalcule tout l'état d'une partie à partir de son
  **journal d'événements** (lancers, corrections, ajouts/retraits de joueurs, changements d'ordre).
  C'est ce modèle qui permet l'undo, la correction de n'importe quel lancer passé, la synchro
  multi-appareils et les statistiques rétroactives. Les règles (objectif, pénalité, élimination,
  futures variantes de jeu) sont une **stratégie** injectée dans le moteur ;
- **Stockage local** : Room, fonctionnement 100 % hors ligne en mode « un seul téléphone » ;
  le journal est persisté à chaque événement validé, pour qu'aucune partie ne soit jamais perdue ;
- **Synchronisation multi-téléphones** (`core:sync`) : un **protocole d'événements unique**
  (messages versionnés, proposition d'événement → validation par une autorité → diffusion ordonnée)
  et une **abstraction de transport** avec deux implémentations :
  - **réseau local, téléphone hôte comme autorité** (phase 3 bis, « Partie à côté ») ;
  - **serveur distant comme autorité** (phase 4).
  La validation d'un événement (droits, tour, cohérence) est une fonction Kotlin pure partagée,
  appelée par l'hôte local et réutilisable côté serveur. L'UI et le moteur de score ignorent quel
  transport est utilisé ;
- **Comptes + multi-appareils à distance** : compare Supabase (Auth + Postgres + Realtime, hébergement UE possible)
  et Firebase (Auth + Firestore). J'ai un profil cybersécurité/RGPD : je privilégie un hébergement UE,
  la minimisation des données et des règles d'accès strictes (RLS ou security rules).
  Intègre dans la comparaison la question de la **souveraineté** : un hébergement en région UE chez un
  fournisseur soumis au droit américain (Cloud Act) n'est pas équivalent à un fournisseur européen.
  Évalue aussi l'option **Supabase auto-hébergé** chez un hébergeur européen (Scaleway, OVHcloud…)
  avec son coût d'exploitation réel pour un projet personnel. Fais une recommandation argumentée ;
- **Gestion des conflits** : deux téléphones qui saisissent en même temps — journal d'événements
  ordonnés par l'autorité (téléphone hôte ou serveur), l'état étant recalculé à partir du journal.
  Précise comment une correction d'un lancer ancien se propage aux autres téléphones ;
- **Rejoindre une partie** : QR code dans les deux modes ; en plus, code court à 6 caractères pour
  le jeu à distance (phase 4) ;
- **Injection** : Hilt ; **minSdk** 26, **targetSdk** la dernière version stable (vérifie ses
  conséquences sur l'accès au réseau local, voir phase 3 bis).

Produis `docs/ARCHITECTURE.md` et un plan d'implémentation découpé en jalons.

👉 Arrête-toi pour validation.

---

## Phase 3 — MVP local (un seul téléphone)

### Création de partie
- Joueurs ou équipes nommées (voir « Équipes nommées »), options de règles repliées dans « Règles avancées »
- **Ordre de passage par glisser-déposer**, quel que soit l'ordre d'ajout des joueurs
- Profils de joueurs locaux réutilisables, avec un **emblème au trait** par défaut (banque de 16 icônes, retour du test)
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
- **Rappel du tour courant** (« Tour 1 », « Tour 2 »…) toujours visible près du joueur actif
- **Tableau des scores en temps réel**, ouvert par l'**icône podium** à gauche du bandeau des scores
  (ou d'un tap sur ce bandeau) ; pas de geste de glissement (retour du test) : score courant de chaque joueur ou équipe, sans quitter la partie
- **Animation d'élimination** quand un joueur ou une équipe fait son troisième raté consécutif
- **Undo** du dernier lancer en un tap (bouton dédié), avec snackbar. Pas de geste de balayage pour
  l'undo, afin qu'il ne puisse pas être confondu avec l'ouverture du tableau des scores
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

---

## Phase 3 bis — Partie à côté : plusieurs téléphones, sans serveur ni compte

### Principe
- Cas d'usage : les téléphones sont **physiquement sur le même terrain**. L'organisateur crée la
  partie ; son téléphone devient l'**hôte**, c'est-à-dire l'autorité qui ordonne et valide le journal,
  exactement le rôle que tiendra le serveur en phase 4
- **Aucun serveur tiers, aucun compte, aucun accès Internet nécessaire.** Les données de la partie
  ne quittent jamais les téléphones de la partie
- Le **QR code** affiché par l'hôte sert de canal d'appairage : il porte l'adresse de l'hôte,
  l'empreinte de son certificat et un jeton d'invitation à usage unique
- **Uniquement des briques open source**, aucune dépendance aux Google Play services
  (Google Nearby Connections est écarté : propriétaire, dépendant des Play services, et il collecte
  des métriques d'usage). La pile reste compatible avec une future distribution F-Droid
- Objectif : **rejoindre en moins de 10 secondes** (scan, fenêtre de connexion Wi-Fi d'Android,
  validation par l'organisateur)

👉 **Point d'arrêt technique avant l'implémentation complète** : réalise d'abord un prototype minimal
(serveur Ktor CIO en WebSocket sur TLS avec certificat auto-signé sur Android, hotspot local,
connexion de l'invité par `WifiNetworkSpecifier`, épinglage de l'empreinte) et teste-le sur deux
appareils réels (le Fairphone et un second téléphone ; l'émulateur ne sait pas créer de point d'accès).
Présente-moi le résultat, les limites rencontrées et les versions de bibliothèques retenues.

### Modes réseau
| Mode | Quand | Conditions |
|---|---|---|
| **Même Wi-Fi** | L'hôte est connecté à un réseau Wi-Fi | Toutes versions d'Android. Certains réseaux (Wi-Fi invité, réseaux publics) isolent les appareils entre eux : en cas d'échec de connexion, proposer le mode point d'accès |
| **Point d'accès local** | Dehors, sans Wi-Fi (cas le plus fréquent) | **Hôte sous Android 13 ou plus** (`WifiManager.startLocalOnlyHotspot`, réseau sans Internet). **Invité sous Android 10 ou plus** : connexion automatique par `WifiNetworkSpecifier`, après confirmation dans la fenêtre système ; l'invité garde ses données mobiles. Invité sous Android 8 ou 9 : afficher le nom du réseau et le mot de passe à saisir à la main |

- Le mode est choisi automatiquement et modifiable par l'organisateur
- **Hôte sous Android 12 ou moins : mode « même Wi-Fi » uniquement.** Sur ces versions, le point d'accès
  local exige la permission de localisation précise, que l'app ne demande jamais
- L'invité attache ses connexions au réseau de la partie (objet `Network` obtenu par la requête),
  pour que rien ne parte par les données mobiles

### Permissions (demandées au moment de l'usage, avec une phrase d'explication)
- **Appareils à proximité** : `NEARBY_WIFI_DEVICES` déclarée avec `usesPermissionFlags="neverForLocation"`,
  nécessaire au point d'accès local à partir d'Android 13
- **Accès au réseau local** : avec un targetSdk 37 (Android 17) ou plus, Android bloque par défaut le
  trafic vers le réseau local et exige la permission d'exécution `ACCESS_LOCAL_NETWORK`, côté hôte
  comme côté invité. Elle appartient au même groupe « Appareils à proximité ». Sous Android 16, le
  mécanisme est en opt-in et s'appuie sur `NEARBY_WIFI_DEVICES`. **Vérifie l'état exact de ces règles
  au moment de l'implémentation** et teste le refus de la permission
- **Service de premier plan** de type `connectedDevice` (+ `FOREGROUND_SERVICE_CONNECTED_DEVICE`)
  pour que l'hôte reste joignable écran éteint. Vérifie que ce type et ses prérequis d'exécution
  correspondent bien à ce cas d'usage, et prépare la justification demandée par la Play Console
- `INTERNET` (sockets), `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_NETWORK_STATE` (permissions normales)
- Caméra pour le scan (déjà prévue)
- **Jamais** `ACCESS_FINE_LOCATION` ni `ACCESS_COARSE_LOCATION`
- Si une permission est refusée : message clair, et la partie continue sur le seul téléphone de l'hôte

### Parcours de référence (sert de base aux tests)
1. **L'organisateur crée la partie** : *Nouvelle partie* → joueurs → interrupteur « Plusieurs téléphones »
   → *Inviter*. En coulisse : choix du mode réseau, démarrage éventuel du point d'accès, génération de la
   paire de clés et du certificat de la partie, démarrage du serveur sur un port aléatoire et sur
   l'interface locale uniquement, création du jeton d'invitation, passage en service de premier plan,
   affichage du QR code (luminosité poussée) et de la liste des téléphones connectés
2. **L'invité scanne** : *Rejoindre* (ou l'appareil photo du téléphone) → scan → fenêtre système
   « Se connecter au réseau » si point d'accès → l'app vérifie l'empreinte du certificat et envoie sa
   demande (jeton, pseudo, avatar). Il voit « En attente de l'organisateur… »
3. **L'organisateur accepte** : carte « 🦊 Léa veut rejoindre la partie » avec trois choix :
   *C'est Léa* (associer le téléphone à un joueur existant), *Nouveau joueur* (règles d'ajout en
   cours de partie), *Refuser*. L'hôte remet un jeton de session et le journal complet ; l'invité
   enregistre le journal dans Room et affiche l'écran de jeu à jour. Un nouveau QR code s'affiche
   pour le suivant
4. **Pendant la partie** : chaque téléphone propose ses événements, l'hôte les valide avec le moteur
   de score, les numérote, les enregistre et les diffuse à tous, auteur compris. Chaque téléphone
   recalcule l'état et joue localement les animations (« 12 ! », élimination, victoire), ce qui les
   synchronise. Une saisie concurrente devenue invalide est refusée avec un message
   (« Ce n'est plus le tour de Léa »). Une correction suit le même circuit et s'affiche chez les autres
   (« Lancer de Léa corrigé »)
5. **Coupure** : bandeau « Connexion perdue, reconnexion… » chez l'invité, saisie suspendue sur ce
   téléphone, reconnexion automatique avec le jeton de session et le dernier numéro reçu ; l'hôte
   renvoie seulement ce qui manque, sans nouvelle validation humaine
6. **Fin** : animation de victoire sur tous les téléphones. L'hôte arrête le serveur, ferme le point
   d'accès, invalide les jetons, supprime la clé de la partie et quitte le service de premier plan ;
   les invités libèrent le réseau. Chaque téléphone garde la partie dans son historique, avec un
   identifiant de partie commun. « Revanche » réutilise les sessions en cours sans rescanner

### Protocole
- WebSocket sur **TLS 1.3**, serveur **Ktor (moteur CIO)** sur l'hôte, client **Ktor (moteur CIO)**
  sur l'invité ; messages JSON (kotlinx-serialization) avec un champ de version, taille bornée
  (64 Ko par message), nombre de téléphones borné (12)
- **Contenu du QR code** : lien `https://<domaine du site>/join#v=1&h=<ip>&p=<port>&fp=<empreinte>&t=<jeton>`,
  plus `ssid` et `psk` en mode point d'accès. Les paramètres sont **après le `#`** : un navigateur
  n'envoie jamais cette partie au serveur web. Avec l'app installée, les App Links vérifiés ouvrent
  directement Molkky Board, sans réseau ; sans l'app, la page du site propose de l'installer.
  En attendant le domaine, schéma `molkkyboard://join#…`
- Messages :

| Message | Sens | Contenu |
|---|---|---|
| `JOIN_REQUEST` | invité → hôte | jeton d'invitation, pseudo, emblème au trait |
| `JOIN_PENDING` | hôte → invité | — |
| `WELCOME` | hôte → invité | jeton de session, rôle, joueur associé, identifiant de partie, journal complet, dernier numéro |
| `JOIN_REJECTED` | hôte → invité | motif : refusé, jeton expiré ou déjà utilisé, partie pleine, version incompatible |
| `RESUME` | invité → hôte | jeton de session, dernier numéro reçu |
| `EVENT_PROPOSAL` | invité → hôte | identifiant unique de l'événement, numéro de base, contenu (lancer, correction, suppression, gestion des joueurs) |
| `EVENT` | hôte → tous | numéro d'ordre, identifiant unique, appareil auteur, contenu, horodatage |
| `EVENT_REJECTED` | hôte → auteur | identifiant unique, code : `NOT_YOUR_TURN`, `FORBIDDEN`, `INVALID`, `STALE` |
| `DEVICE_REMOVED` | hôte → invité | l'organisateur a exclu ce téléphone |
| `GAME_ENDED` | hôte → tous | fin de partie, fermeture de la session |
| `PING` / `PONG` | les deux | maintien de la connexion |

- **Idempotence** : l'identifiant unique de chaque proposition permet à l'hôte d'ignorer un doublon
  renvoyé après une reconnexion
- Droits : chacun saisit les lancers du joueur associé à son téléphone et peut corriger son dernier
  lancer ; l'organisateur peut tout corriger et gérer les joueurs (ordre, ajout, retrait, exclusion d'un téléphone)

### Sécurité
- **Authentification de l'hôte** : certificat auto-signé généré pour chaque partie (Bouncy Castle ou
  Android Keystore, à trancher au prototype), empreinte SHA-256 dans le QR code, **épinglage strict**
  côté invité, aucune exception ni repli en cas d'écart (« Ce n'est pas le téléphone de l'organisateur »).
  La clé est conservée dans l'Android Keystore pendant la partie, puis supprimée
- **Jeton d'invitation** : 128 bits d'aléa cryptographique, à usage unique, valable 2 minutes,
  renouvelé après chaque usage, comparé à temps constant. Le QR code est considéré comme **observable**
  (photo par-dessus l'épaule, capture d'écran) : il ne contient aucun secret durable
- **Validation humaine** de chaque arrivée par l'organisateur
- **Jeton de session** : 256 bits, propre à chaque téléphone, valable jusqu'à la fin de la partie,
  révocable (exclusion), stocké chiffré avec une clé du Keystore et exclu des sauvegardes Android
- Le serveur n'écoute que sur l'interface du réseau de la partie, refuse tout ce qui n'est pas TLS,
  et s'arrête à la fin de la partie ou à l'abandon
- **Validation par l'hôte de chaque événement** avec la même fonction que la validation serveur de la
  phase 4 : appareil accepté, droits, tour, lancer cohérent ; champs texte bornés et sans caractères
  de contrôle ; limitation de débit par téléphone
- Seuls le pseudo, l'emblème au trait et les événements de jeu transitent. **Les photos de
  joueurs ne sont jamais transmises** : les autres téléphones affichent l'avatar
- Aucune crypto maison : TLS standard (Conscrypt) et primitives Tink

### Comportements limites
- **Hôte en arrière-plan ou écran éteint** : le service de premier plan garde le serveur actif ; la
  notification « Partie en cours » indique le nombre de téléphones connectés
- **L'app de l'hôte redémarre** : le serveur repart avec le même certificat ; si l'adresse IP a changé,
  les invités rescannent le QR code et sont reconnus par leur jeton de session, sans nouvelle validation
- **L'hôte quitte définitivement** : les invités gardent la partie en lecture seule
  (transfert du rôle d'hôte : backlog P3)
- **Pas de saisie manuelle de code dans ce mode** : un code court ne peut pas porter l'empreinte de
  sécurité. En secours (caméra refusée, QR illisible), l'organisateur ajoute le joueur sur son propre
  téléphone et saisit pour lui
- Le Hall of Fame local de chaque téléphone compte la partie ; l'identifiant commun évitera les
  doublons quand les groupes seront synchronisés (phase 4)

### Interface
- Création de partie : interrupteur « Plusieurs téléphones »
- Écran « Inviter » : grand QR code, mode réseau en cours (modifiable), liste des téléphones connectés
  avec leur joueur associé, cartes de demandes en attente, action « Exclure »
- Écran « Rejoindre » : viseur de scan, états clairs (connexion au réseau, vérification, en attente,
  refusé), aide en cas d'échec (« Rapprochez-vous », « Passez en point d'accès »)
- Écran de jeu : petite pastille de connexion (« 3 téléphones », ou « Hors ligne » en corail chez l'invité)
- Accessibilité : description TalkBack du QR code et des états de connexion ; tous les messages
  lisibles jusqu'au facteur de police 2,0

### Tests
- Unitaires : sérialisation et version du protocole, validation de chaque proposition (tous les codes
  de refus), idempotence, rattrapage après coupure, ordonnancement de deux propositions concurrentes,
  correction d'un lancer ancien propagée à tous
- Intégration JVM : un hôte et deux invités sur la boucle locale avec TLS réel ; refus d'une mauvaise
  empreinte, d'un jeton expiré, d'un jeton déjà utilisé, d'un téléphone exclu ; reconnexion
- Sur appareils réels (manuel, scénarios dans `docs/qa/PARTIE_A_COTE.md`) : même Wi-Fi, point d'accès
  local, invité Android 8-9, refus de chaque permission, hôte écran éteint 10 minutes, hôte qui redémarre
  l'app, temps pour rejoindre mesuré (objectif : moins de 10 s)
- **Vie privée** : pendant une partie à côté, aucune connexion vers une adresse hors du réseau local
  (adresses privées et lien-local uniquement), vérifié par un test et par logcat

---

## Phase 4 — Comptes et multi-appareils à distance
> Décision ouverte : la « Partie à côté » couvre le jeu sur un même terrain sans compte. Cette phase
> pourrait être reportée en V2. À trancher avec moi avant de la commencer.

- Réutilise le protocole et la validation de `core:sync` : seul le transport change (le serveur
  devient l'autorité à la place du téléphone hôte)
- Inscription / connexion (e-mail + lien magique ou mot de passe ; Google en option) ;
- Création d'une partie partagée à distance, invitation par code court ou QR : **rejoindre en moins de 10 secondes** ;
- Synchronisation temps réel ; un appareil hors ligne rattrape son retard en revenant ;
- Rôles simples : chacun saisit son propre lancer et peut corriger son dernier lancer ;
  l'organisateur peut corriger n'importe quel lancer et gérer les joueurs (ordre, ajout, retrait) ;
- Toute correction est visible des autres téléphones (petite notification « Lancer de Léa corrigé ») ;
- Les joueurs invités sans compte restent possibles ;
- Groupes et Hall of Fame de groupe synchronisés.

## Phase 5 — Build
- Génère un APK debug avec `./gradlew assembleDebug` et indique son chemin ;
- Prépare la configuration d'un build release signé (keystore hors du dépôt, secrets non commités) ;
- Pour le Play Store, le livrable est un **Android App Bundle** (`./gradlew bundleRelease`) avec
  **Play App Signing** : Google conserve la clé de signature, je garde la clé d'envoi (upload key).
  Explique-moi où stocker et sauvegarder cette clé ;
- Versionnement : `versionCode` incrémenté à chaque envoi, `versionName` en SemVer (1.0.0…).

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
- Backend distant selon la décision de la phase 2 :
  - Supabase : **supabase-kt** (modules auth, postgrest, realtime) + client **Ktor** (moteur OkHttp)
  - Firebase : **Firebase BoM** (auth, firestore)
- Repository pattern : Room reste la source de vérité de l'UI, et la synchro se fait en arrière-plan (offline-first)
- **Aucun SDK de publicité, d'analytics ou de crash reporting tiers** (voir « Sécurité et vie privée »)
- **Tink** (ou l'Android Keystore directement) pour chiffrer les jetons de session stockés localement
- **AboutLibraries** pour la liste des licences open source dans les crédits
  (pas le plugin oss-licenses de Google, qui dépend des Google Play services)

### Partie à côté (réseau local, phase 3 bis) — uniquement des briques open source
- **Ktor server** (moteur CIO, plugin WebSockets) sur l'hôte et **Ktor client** (moteur CIO,
  WebSockets) sur l'invité — Apache 2.0
- **Conscrypt** (fournisseur TLS intégré à Android) pour TLS 1.3 ; `X509TrustManager` d'épinglage par empreinte
- **Bouncy Castle** (`bcpkix`, licence MIT) pour générer le certificat auto-signé de la partie,
  sauf si le prototype montre que l'Android Keystore suffit
- **Tink** pour les jetons et les comparaisons à temps constant
- API Android (AOSP) : `WifiManager.startLocalOnlyHotspot`, `WifiNetworkSpecifier`,
  `ConnectivityManager`, service de premier plan
- Références de conception (spécifications publiques, **ne copier aucun code**) : protocole LocalSend
  (HTTPS auto-signé identifié par empreinte) et Bramble QR Code Protocol de Briar (appairage par QR code,
  QR considéré comme observable). Le code de Briar est sous GPL
- Écarté : Google Nearby Connections (propriétaire, Play services, métriques d'usage)

### QR code et invitation
- Génération : **zxing core**
- Scan : **CameraX** (`ImageAnalysis`) + décodage **zxing-cpp** (wrapper Android, licence Apache 2.0,
  activement maintenu, aucune connexion réseau). Analyse limitée à la zone de cadrage et à quelques images
  par seconde, gestion des reflets d'écran (contraste, tentative en image inversée)
- Pas de ML Kit : même en version embarquée, il envoie des statistiques d'usage à Google, ce qui
  contredit notre engagement « aucun traceur »
- Contenu du QR code : voir « Phase 3 bis — Protocole » (partie à côté) ; jeton d'invitation long
  pour le jeu à distance (phase 4)
- **Saisie manuelle du code à 6 caractères** proposée sur l'écran de scan pour le **jeu à distance**,
  en secours (soleil direct, écran rayé, caméra refusée). Elle n'existe pas pour la partie à côté
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
1. **Accueil** : logotype « Molkky Board » **compact** en haut à gauche (même hauteur qu'un titre de
   barre d'application, environ 22sp, sans dominer l'écran), grand bouton « Nouvelle partie »,
   bouton « Rejoindre » (scan du QR code), partie en cours à reprendre, historique, Hall of Fame
2. **Création de partie** : ajout des joueurs en chips colorées, glisser-déposer pour l'ordre,
   options de règles repliées dans « Règles avancées », interrupteur « Plusieurs téléphones »,
   bouton de lancement bien visible
3. **Écran de jeu** (le plus important) :
   - en haut, le joueur ou l'équipe actif en très grand avec son score et ses ratés, et une pastille
     discrète « Tour 3 » à côté du nom. Au changement de tour, la pastille bascule avec un ressort
     (sans interrompre la saisie) ;
   - au centre, la grille des quilles (un tap couche une quille, un second tap la relève)
     ou le pavé rapide selon le mode, avec une bascule discrète entre les deux ;
   - un aperçu en direct, « +7 → 38 », avant validation ;
   - en bas, deux gros boutons : « Raté » (corail) et « Valider » (safran) ;
   - un bandeau horizontal avec les autres joueurs et leur score, précédé d'une **icône podium** qui ouvre le
     tableau des scores (retour du test : le geste de glissement ne fonctionnait pas) ;
   - en partie multi-téléphones, une petite pastille de connexion (« 3 téléphones » ou « Hors ligne ») ;
   - le **tableau des scores** : en touchant l'icône podium ou le bandeau, une feuille
     (bottom sheet) monte et couvre l'écran de saisie. Elle affiche :
     - le numéro du tour en titre ;
     - une ligne par joueur ou équipe, **classée par score** : rang, pastille de couleur et emblème,
       nom, score en grands chiffres tabulaires, points restants pour 50 (« reste 12 »), ratés (0/3) ;
     - le joueur actif surligné en safran, les éliminés grisés et barrés en bas de liste ;
     - en équipes, le score de l'équipe, et ses joueurs dépliables en dessous.
     Les scores s'y mettent à jour en direct, y compris quand un autre téléphone saisit un lancer ou
     corrige un lancer passé (phases 3 bis et 4). On la referme en glissant vers le bas, en touchant à
     l'extérieur ou avec le bouton retour ; la saisie en cours n'est pas perdue. La feuille est accessible
     sans geste (tap, action TalkBack « Afficher le tableau des scores ») et lisible jusqu'au facteur
     de police 2,0 ;
   - l'undo en bouton discret avec snackbar, et l'accès au déroulé et à la gestion des joueurs
4. **Déroulé de la partie** : liste chronologique des lancers, groupée par tour (« Tour 1 », « Tour 2 »…),
   chaque lancer modifiable ou supprimable
5. **Fin de partie** : animation de victoire, classement, stats de la manche, bouton « Revanche »
6. **Stats** : graphiques Vico, records personnels
7. **Réglages** : mode de saisie, annonces vocales, sons, revoir la prise en main, « Plein soleil »,
   données (sauvegarde, export, suppression)
8. **À propos** : logotype Molkky Board, version, mention « Yogi Studio », encart « Vie privée »
   (voir « Sécurité et vie privée »), crédits (bibliothèques open source via AboutLibraries, polices),
   carte « Licence et contact » (logiciel libre sous GNU GPL v3, lien vers le code source, noms et logos réservés,
   contact **yogistudiofr@gmail.com** avec un bouton « Écrire à Yogi Studio »), liens vers la politique de
   confidentialité, les conditions d'utilisation et le site de l'app
9. **Inviter / Rejoindre** (partie à côté) : voir « Phase 3 bis — Interface »

### Mouvement et retours
- Physique de ressort (`spring`) partout ; aucune animation linéaire
- La quille bascule avec une légère rotation quand on la sélectionne
- À chaque lancer validé, les quilles touchées s'envolent hors de la grille (≈ 0,4 s) puis reviennent (retour du
  test) ; le « 12 » garde sa propre animation
- Le score défile comme un compteur quand il change, y compris lors d'un recalcul après correction
- En cas de dépassement de 50, un « shake » horizontal suivi de la retombée animée à 25
- Lancer à 12 points : animation « 12 ! » (voir « Animation “12 !” »)
- Au deuxième raté consécutif, l'indicateur de ratés pulse en corail (avertissement)
- Au troisième raté, animation d'élimination (voir « Animation d'élimination »)
- Retours haptiques : léger à la sélection, confirmé à la validation, double vibration à l'élimination
- Respecter le réglage système « Réduire les animations »

### Accessibilité et usage en extérieur
- Contraste AA minimum, AAA sur les scores
- Zones tactiles de 56dp minimum, utilisables à une main
- Option « Plein soleil » : contraste renforcé
- Grandes tailles de police prises en charge jusqu'au facteur 2,0 sans troncature
- Descriptions TalkBack sur chaque quille et chaque score
- Écran maintenu allumé pendant une partie (`keepScreenOn`)
- Écran bloqué en **portrait** (retour du test : le paysage boguait ; mode paysage en V2)
- Contrôles d'accessibilité automatiques (Accessibility Test Framework via Roborazzi) dans les tests

👉 Avant de coder les écrans, crée le module `core:designsystem` (thème, tokens, typographie,
composants de base : PinView, QuickPad, ScoreRing, PlayerChip, BigActionButton, RoundBadge,
ScoreboardRow) avec des `@Preview`
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
  800 vert forêt, « Board » en safran, les deux mots sur une même ligne de base. Deux tailles :
  grande pour l'intro, **compacte** (environ 22sp) pour l'en-tête de l'accueil, alignée à gauche

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
- Pas de version courte (retour du test) : l'intro complète est toujours jouée, passable d'un tap ; la signature
  Yogi Studio dure environ 1,5 s
- « Réduire les animations » activé : simple fondu du logo Yogi Studio vers le menu (environ 1 s)

### Storyboard (≈ 4,5 s, entièrement en Canvas Compose, sans librairie d'animation externe)
Thème : **le strike**, les 12 quilles renversées d'un seul lancer de mölkky. Le traitement emprunte
les codes du **manga** (lignes de vitesse, image d'impact figée, onomatopée graphique, quilles qui
filent hors champ et disparaissent en scintillant) mais reste dans la direction « Nordic Bold » :
aplats de la palette, formes arrondies, **pas de trame, pas d'encrage noir épais, pas de texture**.
Les quilles restent **entières** : elles ne se brisent pas, elles s'envolent.

1. **0 – 0,6 s — Mise en place** : les 12 quilles apparaissent dans leur formation de départ
   sur fond bouleau, avec un léger rebond (spring)
2. **0,6 – 1,1 s — Le lancer** : le mölkky arrive du bas de l'écran en tournoyant. Des **lignes de
   vitesse** (traits fins vert forêt et safran, convergents vers les quilles) apparaissent derrière lui
   et s'accélèrent ; léger zoom avant de la « caméra » pour l'anticipation
3. **1,1 – 2,4 s — Le strike** :
   - **image d'impact** : au contact, l'animation se fige environ 100 ms. Fond en aplat safran
     avec un éclat radial (étoile à pointes arrondies), les quilles en silhouettes vert forêt,
     tremblement de l'écran et vibration forte ;
   - **onomatopée** : « 12 ! » en Bricolage Grotesque 800, dans une bulle éclatée safran et corail,
     qui claque avec un ressort puis s'efface. Ce composant est réutilisé en jeu (voir « Animation “12 !” ») ;
   - **l'envol** : les 12 quilles, intactes et numérotées, sont projetées dans toutes les directions en
     tournoyant, chacune avec une petite traînée de vitesse. La plupart sortent par les bords de l'écran ;
     2 ou 3 filent vers le fond en rétrécissant et disparaissent dans un **scintillement** (petite étoile
     safran qui clignote une fois, clin d'œil au gag classique du manga) ;
   - le mölkky retombe et roule hors champ
4. **2,4 – 3,5 s — La signature** : l'écran se vide et revient au fond bouleau calme. Le logo Yogi
   Studio apparaît au centre (le cercle safran se trace, puis le personnage apparaît avec un rebond),
   avec la mention « Yogi Studio présente »
5. **3,5 – 4,5 s — Le titre et l'ouverture** : le logo Yogi Studio s'efface, et le logotype
   **« Molkky Board »** entre en claquant au centre, porté par une dernière salve de lignes de vitesse.
   Après un court temps de pause, il glisse et se réduit vers l'en-tête compact de l'accueil, en haut
   à gauche, pendant que le menu apparaît derrière (transition à éléments partagés : c'est le même
   logotype qui sert de titre à l'accueil)

### Contraintes
- 60 images/s visées sur une machine modeste : nombre d'éléments animés borné (12 quilles, leurs
  traînées, au plus 40 lignes de vitesse), aucune allocation dans la boucle de dessin, calcul des
  trajectoires précalculé ou en arithmétique simple
- **Sécurité photosensible** : un seul flash d'impact, sans clignotement répété (moins de 3 flashs par
  seconde, critère WCAG 2.3.1), et le fond safran remplace un flash blanc plein écran
- Résultat déterministe (graine aléatoire fixe) pour que les tests de capture Roborazzi soient stables
- Fonctionne en mode clair et sombre (fond `#14201A` en sombre)
- Son d'impact optionnel, désactivé par défaut, coupé en mode silencieux (pas en vibreur)
- Test UI : vérifier qu'un tap passe bien l'animation et que le menu s'affiche même si le chargement échoue
- Montre-moi une capture de chacune des 5 étapes (via `android screen capture` ou Roborazzi),
  dont l'image d'impact figée, avant de finaliser les réglages de timing

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
- Sur une partie partagée (à côté ou à distance), l'animation se joue **simultanément sur tous les téléphones connectés**
- Si une correction de lancer annule la victoire, l'écran de fin se referme et la partie reprend
- Version allégée, un simple fondu avec le trophée, si l'option système « Réduire les animations » est active

### Animation « 12 ! »
- Déclenchée quand un lancer validé rapporte **12 points**, de l'une des deux façons possibles :
  - **la quille n° 12 seule** : sur la grille, la quille 12 bascule et file hors champ avec ses lignes
    de vitesse, puis la bulle « 12 ! » claque ;
  - **les 12 quilles d'un coup** (le strike) : version courte de l'intro, les 12 quilles de la grille
    s'envolent hors champ, puis la bulle « 12 ! ». Ce cas plus rare a droit à un petit bandeau « Strike ! »
    sous la bulle.
- Version **simplifiée** de l'intro : environ 1 s, image d'impact figée raccourcie (≈ 60 ms), au plus
  12 quilles et 20 lignes de vitesse, passable d'un tap, sans jamais bloquer la saisie du lancer suivant
  (les quilles reviennent en place en fondu une fois l'animation finie)
- Vibration forte brève, son optionnel désactivé par défaut, annonce vocale « Douze ! » si activée
- Priorités avec les autres animations :
  - si le 12 fait atteindre **exactement 50**, la bulle « 12 ! » s'affiche brièvement puis s'enchaîne
    avec l'animation de victoire ;
  - si le 12 fait **dépasser 50**, pas de « 12 ! » : seul le retour à 25 est mis en scène.
- En mode de saisie rapide, la même animation se joue au-dessus du pavé (la quille 12 seule ou les
  12 quilles stylisées)
- Sur une partie partagée, l'animation se joue sur tous les téléphones ; si une correction transforme
  le lancer, rien n'est rejoué, seul le score est recalculé
- « Réduire les animations » : la bulle « 12 ! » apparaît en fondu, sans quilles qui s'envolent
- Les « 12 » et les strikes sont des **moments partageables** et alimentent les records secondaires
  (plus grand nombre de « 12 ») et le badge « Sniper »
- Capture Roborazzi de la bulle « 12 ! » et des deux variantes

### Animation d'élimination
- Déclenchée quand un joueur ou une équipe fait son **troisième raté consécutif** (ou selon la règle
  d'équipe retenue en phase 1), au moment du « Valider »
- Séquence de 1,5 à 2 secondes, ton humoristique mais sans moquerie, que l'on peut passer d'un tap :
  1. le troisième point de l'indicateur de ratés se remplit en corail et les trois points claquent ensemble ;
  2. la carte du joueur ou de l'équipe tremble, puis **sa quille-avatar bascule et tombe** hors de la carte
     (même langage visuel que l'intro : lignes de vitesse, petite traînée) ;
  3. un tampon **« Éliminé »** en corail se pose en biais sur la carte avec un rebond ;
  4. la carte se désature et glisse en bas du bandeau des scores et du tableau des scores.
- Double vibration (déjà prévue pour l'élimination) et annonce vocale « Léa est éliminée » si activée
- Son optionnel, désactivé par défaut
- Le tour passe ensuite au joueur suivant, sans action supplémentaire
- Si l'élimination désigne un vainqueur (dernier joueur ou dernière équipe en jeu, selon les règles
  de la phase 1), elle s'enchaîne directement avec l'animation de victoire
- Sur une partie partagée, l'animation se joue **sur tous les téléphones connectés**
- Si une correction de lancer annule l'élimination, la séquence se joue à l'envers en version courte
  (le tampon se décolle, la carte reprend sa couleur et sa place)
- « Réduire les animations » : la carte passe en grisé avec le tampon, en fondu simple
- Lien « Pourquoi ? » sous la carte éliminée, vers la règle des trois ratés (voir « Guide des règles »)
- Test de capture Roborazzi de l'état final et test UI de l'annulation par correction

### Annonces vocales
- Option « Annonces vocales » en synthèse vocale Android hors ligne, désactivée par défaut :
  « Tour 4 », « Au tour de Léa, il lui faut 7 », « Retour à 25 ! », « Léa est éliminée »,
  « Victoire de l'équipe Les Bouleaux »
- Volume et débit réglables ; coupées **en mode silencieux seulement** (elles jouent en vibreur, retour du test) ;
  pas d'annonce pendant TalkBack si cela fait doublon
- En partie multi-téléphones, les annonces ne sont jouées que sur le téléphone de l'organisateur
  par défaut, pour éviter l'écho (réglable)

### Coach de fin de partie — supprimé (retour du test, 2026-10-27)
- Jugé envahissant : ni « Il te faut 7 », ni quille surlignée, ni zone de danger. À la place, la **tension avant la
  sanction** : à un raté de l'élimination (ou du retour en arrière en Ludique), la carte du joueur bat comme un
  cœur avec « Dernière chance · 1 raté = éliminé »

### Guide des règles et placement des quilles
Objectif : quelqu'un qui n'a jamais joué doit pouvoir installer le jeu et lancer une partie
correcte avec l'app seule.

**Écran « Règles du jeu »** (accessible depuis l'accueil, le menu de l'écran de jeu et les réglages)
- Contenu issu de `docs/RULES.md` (phase 1), reformulé simplement, en sections courtes et illustrées :
  1. **Le matériel** : 12 quilles numérotées, la quille de lancer (le mölkky), la ligne de lancer ;
  2. **L'installation** : formation de départ des quilles, distance de lancer ;
  3. **Le lancer** : lancer à la cuillère, depuis la ligne, à tour de rôle ;
  4. **Compter les points** : une quille seule = son numéro ; plusieurs quilles = le nombre de quilles ;
     une quille appuyée sur une autre ou sur le mölkky ne compte pas ;
  5. **Relever les quilles** : debout, à l'endroit exact où elles sont tombées ;
  6. **Gagner** : atteindre exactement 50 ; au-delà, retour à 25 ;
  7. **Les ratés** : trois lancers consécutifs sans quille tombée = élimination ;
  8. **Jouer en équipes** et **enchaîner les manches** ;
  9. **Les variantes** activables dans l'app (en renvoyant vers les options correspondantes)
- Chaque règle est illustrée par un **petit schéma animé** dessiné avec les composants du design system
  (PinView), pas par des images figées : quille seule qui tombe = son numéro, trois quilles qui tombent = 3,
  dépassement de 50 et retour à 25, trois ratés et élimination
- Un sommaire en haut, une recherche simple, et une section « Questions fréquentes » (quille couchée sur
  une autre, quille sortie du terrain, faute de pied…) alimentée par les cas ambigus relevés en phase 1
- Lisible en plein soleil et hors ligne

**Schéma interactif de placement des quilles**
- La **formation de départ** est affichée telle que vue depuis la ligne de lancer, quilles serrées en
  4 rangées (selon les règles officielles : 1-2 devant, puis 3-10-4, puis 5-11-12-6, et 7-9-8 au fond ;
  **vérifie cette formation dans les sources de la phase 1**)
- Les **12 quilles d'emblée**, en vue épurée depuis la ligne de lancer (retour du test : le pas à pas était trop
  long et sans plus-value)
- Indication de la **distance de lancer** (3 à 4 mètres selon les règles retenues) avec un repère concret
  (« environ 4 grands pas d'adulte »), et rappel qu'on peut la réduire en mode famille

**Actions rapides pour les débutants**
- **Rappel en début de partie** : après « Lancer la partie », une carte « Prêts à jouer ? » propose
  « Voir le placement des quilles » ou « C'est parti ». Elle s'affiche par défaut lors des 3 premières
  parties, puis seulement si l'utilisateur l'a réactivée (case « Ne plus afficher » et réglage dédié)
- **Bouton « Quilles »** permanent mais discret sur l'écran de jeu : ouvre le schéma de placement en
  surimpression (bottom sheet) sans quitter la partie, utile entre deux manches
- **Entre deux manches** : rappel « Replacez les quilles en formation de départ » avec le schéma miniature
- **Pas de mode débutant** (retour du test : trop de messages)
- **Raccourcis de l'icône de l'app** (appui long sur l'icône) : « Nouvelle partie », « Placer les quilles »,
  « Règles du jeu », « Rejoindre une partie »
- Accès aux règles depuis le menu de jeu et depuis l'explication d'une élimination (« Pourquoi ? ») ; la belle et
  le Mölkkout ont leur section et leur démo, et l'écran de fin explique qui commence la belle

**Contraintes**
- Les textes de règles sont des chaînes externalisées, prêtes pour la traduction
- TalkBack : chaque quille du schéma a une description de sa position (« Quille 12, troisième rangée, au centre droit »)
- Captures Roborazzi du schéma de placement et de chaque illustration de règle

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
- **Local** : toutes les parties jouées sur ce téléphone, y compris les parties à côté auxquelles il a participé
- **Groupe** : un groupe (famille, amis, club) réunit des comptes. Son Hall of Fame est partagé et synchronisé entre ses membres. Une partie partagée est rattachée à un groupe à sa création, ce rattachement étant facultatif
- Filtres : tout le temps, cette année, ce mois-ci

Contraintes techniques :
- **Toutes les statistiques sont calculées à partir du journal des lancers** et non stockées en dur, afin de pouvoir ajouter de nouveaux classements plus tard en les appliquant rétroactivement
- Seules les parties terminées normalement comptent. Les corrections faites **pendant** la partie sont normales
  et n'ont aucun effet sur les records. Une partie abandonnée, ou modifiée **après** sa fin, est marquée et
  exclue des records, mais reste dans l'historique
- Les lancers saisis en mode rapide comptent pour tous les classements sauf ceux qui portent sur une quille précise
- Une même partie présente sur plusieurs téléphones (même identifiant de partie) n'est comptée qu'une fois
  dans un Hall of Fame de groupe
- Tests unitaires sur le calcul des classements, notamment pour les égalités (départage par date : le premier à l'avoir réalisé garde le record)

### Notification de partie en cours
- Notification persistante : numéro du tour, à qui c'est le tour et les scores, avec action rapide « Saisir le lancer »
- Sur le téléphone hôte d'une partie à côté, c'est aussi la notification du service de premier plan :
  elle indique le nombre de téléphones connectés et propose « Arrêter le partage »

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
- **« 12 ! »** : quille 12 seule ou strike des 12 quilles (vignette avec la bulle « 12 ! »)
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
  (« 50 en 7 lancers », « victoire au tour 6 »), date, lieu s'il a été saisi
- Signature systématique et discrète : « Comptez vos parties avec » et le logotype **Molkky Board**, **sans QR
  code** (retour du test). Une vignette ne porte jamais de QR d'invitation à une partie à côté
- **Photo du moment** (façon Strava) : photo prise ou choisie par l'auteur, plein cadre, avec le moment, l'équipe,
  le score, les lancers et le tour en blanc ; aucune permission, rien n'est conservé, aucune métadonnée
- Rendu directement depuis des composables Compose capturés en bitmap (`GraphicsLayer`), sans serveur

### Mini-vidéos (reels / stories)
- Clip vertical 9:16 de **6 à 10 secondes**, généré **sur le téléphone** à partir du journal de la partie :
  1. ouverture avec le strike, les quilles qui s'envolent hors champ (reprise courte de l'animation de l'intro) ;
  2. **la course vers 50** : les scores de chaque équipe progressent lancer après lancer sous forme de
     barres ou de jauges animées, tour après tour (« Tour 1 », « Tour 2 »…), avec les retours à 25 et
     les éliminations mis en scène (tampon « Éliminé » de l'animation d'élimination) ;
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
- Les vignettes n'affichent **que les pseudos et les emblèmes**, jamais les photos de profil des joueurs (seule la
  photo du moment, choisie par l'auteur, peut y figurer)
- **Pas de masquage des noms** (retour du test : jugé inutile)
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

### Partie à côté
- **P3 — Transfert du rôle d'hôte** à un autre téléphone quand l'organisateur doit partir
- **P3 — Transport Bluetooth** (RFCOMM) en secours, quand ni Wi-Fi ni point d'accès ne sont possibles
- **P3 — Mode spectateur à côté** : un téléphone ou une tablette rejoint en lecture seule (tableau d'affichage)

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

### Modération
- **P2 — Modération légère** des noms d'équipes et pseudos par l'organisateur du groupe

---

## Sécurité et vie privée
Je suis professionnel de la cybersécurité et attaché à la protection des données : la sécurité
et la vie privée sont des **exigences de premier rang**, au même niveau que l'expérience utilisateur.
Référentiel cible : **OWASP MASVS v2** (niveau L1 pour toute l'app, contrôles renforcés sur
l'authentification et le stockage), vérifié avec le guide de test **OWASP MASTG**.
Les recommandations de la **CNIL** pour les applications mobiles servent de référence pour la partie RGPD.

### Analyse de risques (avant la phase 3 bis)
- Rédige `docs/security/THREAT_MODEL.md`, une analyse de risques légère inspirée d'**EBIOS Risk Manager** :
  biens essentiels (comptes, données de groupe, intégrité des scores et des records), sources de risque
  (joueur tricheur, curieux qui énumère les codes d'invitation, attaquant opportuniste sur les comptes,
  contenu malveillant dans les noms d'équipe), scénarios et mesures retenues
- Pour la partie à côté, couvre au minimum : QR code photographié ou capturé, téléphone qui se fait passer
  pour l'hôte (homme du milieu sur le Wi-Fi ou le point d'accès), invité exclu qui tente de revenir,
  rejeu d'événements, client modifié qui force un score, hôte qui quitte la partie, inondation de messages
- Mets-le à jour à chaque fonctionnalité qui ajoute des données ou une surface d'exposition
  (partie à côté, partage, lien spectateur, groupes, photos)

### Minimisation et cycle de vie des données
- **Mode sans compte : aucune donnée ne quitte les téléphones de la partie.** En partie sur un seul
  téléphone, aucun trafic réseau ; en partie à côté, uniquement du trafic chiffré vers les autres
  téléphones de la partie, sur le réseau local, et **aucun trafic vers Internet**
  (vérifiable : test automatisé et logcat, voir « Tests et preuves »)
- En partie à côté, seuls le pseudo, l'emblème au trait et les événements de jeu sont transmis ;
  jamais de photo, jamais d'identifiant de l'appareil
- Mode compte : seulement l'e-mail (pour l'authentification), un pseudo, l'avatar et les données de parties.
  L'e-mail n'est **jamais** visible des autres joueurs
- Pas de nom réel, pas de date de naissance, pas de localisation, pas d'identifiant publicitaire,
  pas de lecture des contacts
- Les photos de joueurs restent **sur le téléphone** par défaut. Si elles sont synchronisées un jour :
  stockage privé, URL signées à durée limitée, métadonnées EXIF supprimées avant envoi
- **Durées de conservation** définies et appliquées automatiquement côté serveur : comptes inactifs
  depuis 3 ans prévenus puis supprimés, données des parties supprimées avec le compte, codes
  d'invitation expirés purgés. En partie à côté : clés et jetons supprimés à la fin de la partie
- **Suppression** de compte effective sous 30 jours maximum, y compris dans les sauvegardes
  (documenter la rotation des sauvegardes)
- **Portabilité** : export JSON complet des données de l'utilisateur
- Rédige `docs/privacy/REGISTRE.md` (registre simplifié des traitements : finalités, base légale,
  catégories de données, durées, sous-traitants, localisation) et la liste publique des sous-traitants
  pour le site web. La partie à côté n'implique aucun sous-traitant

### Aucun traceur, et le prouver
- Zéro SDK publicitaire, d'analytics, d'attribution ou de crash reporting tiers ; pas de Firebase,
  pas de ML Kit, pas de Google Nearby, pas de Facebook SDK, pas de police téléchargée depuis Google Fonts
  à l'exécution
- **Rapports de plantage** : uniquement à la demande de l'utilisateur. Après un plantage, proposer
  « Envoyer le rapport par e-mail » avec un aperçu du contenu (trace technique, version, modèle,
  aucune donnée de partie ni identifiant). Rien n'est envoyé automatiquement
- Contrôle en CI : un script échoue si une dépendance connue pour embarquer un traceur apparaît
  dans le graphe de dépendances (liste de signatures inspirée d'Exodus Privacy)
- Après publication, faire analyser l'app par **Exodus Privacy** et afficher le résultat
  (0 traceur) sur le site web

### Encart « Vie privée » dans l'écran « À propos »
Texte court et sincère, en français et en anglais, par exemple :
- « Molkky Board ne contient **aucune publicité, aucun traceur publicitaire, aucun outil de mesure
  d'audience** et ne revend aucune donnée. »
- **[Proposition à valider, remplace « Sans compte, tout reste sur votre téléphone. »]**
  « Sans compte, vos parties restent sur vos téléphones : quand plusieurs téléphones jouent ensemble,
  ils échangent directement entre eux, en chiffré, sans passer par Internet. Avec un compte, seules les
  données nécessaires au jeu à distance sont stockées, en Europe [à adapter selon l'hébergeur retenu]. »
- « Vous pouvez exporter ou supprimer vos données à tout moment. »
- Lien vers la politique de confidentialité, la liste des sous-traitants et le rapport Exodus Privacy
Chaque affirmation de cet encart doit être **vraie et vérifiable** : si un choix technique la rend
fausse, préviens-moi au lieu de modifier le texte.

### Backend et API (phase 4)
- **Row Level Security** (ou équivalent) activée sur toutes les tables, avec un refus par défaut ;
  un joueur ne lit que les parties et groupes dont il est membre
- La clé de service (service role) n'est **jamais** dans l'app ; les opérations privilégiées passent
  par des fonctions serveur
- **Validation côté serveur de chaque événement** de partie (même fonction que l'hôte de la partie à côté) :
  l'auteur est bien membre de la partie, le lancer est cohérent (0 à 12 quilles, numéros valides),
  la séquence est respectée, les droits de correction sont vérifiés. Le serveur recalcule l'état ;
  un client modifié ne peut pas forcer un score ou un record
- **Codes d'invitation** : générés avec un aléa cryptographique, alphabet sans caractères ambigus,
  expiration (24 h par défaut), révocables par l'organisateur, et **limitation du nombre d'essais**
  par compte et par adresse IP pour empêcher l'énumération. Le QR code contient un jeton plus long
  que le code court
- Limitation de débit sur l'authentification, la création de parties et l'envoi d'événements
- Noms d'équipes, pseudos et textes libres : longueur bornée, caractères de contrôle refusés,
  échappement systématique à l'affichage (notamment sur la page spectateur web)
- Journalisation de sécurité côté serveur (connexions, échecs, suppressions de compte) sans données
  de partie, conservée 6 mois
- Sauvegardes chiffrées, dans l'UE, avec un test de restauration documenté

### Authentification
- Connexion par **lien magique** ou **passkey** (Credential Manager) ; mot de passe en option, avec
  vérification contre les mots de passe compromis si le backend le permet ; pas de SMS
- Jetons d'accès courts, rotation des jetons de rafraîchissement, déconnexion révoquant le jeton,
  et action « Se déconnecter de tous les appareils »
- Liste des appareils connectés visible dans les réglages du compte
- Confirmation renforcée pour la suppression du compte (nouvelle authentification récente)

### Application Android
- **Transport vers le backend** : HTTPS uniquement, trafic en clair interdit dans `network_security_config`.
  Pas d'épinglage de certificat strict vers le backend (risque de panne lors des rotations du fournisseur) ;
  documente ce choix dans le modèle de menaces
- **Transport en partie à côté** : TLS 1.3 avec **épinglage strict** de l'empreinte lue dans le QR code
  (certificat éphémère, aucune rotation à craindre). Aucun trafic en clair, y compris sur le réseau local
- **Stockage local** : jetons de session chiffrés avec une clé de l'Android Keystore ; base Room
  protégée par le chiffrement du système de fichiers Android (données peu sensibles, choix à documenter)
- **Sauvegarde Android** : `dataExtractionRules` excluant les jetons de session et les clés de la
  sauvegarde cloud et du transfert d'appareil ; les parties locales peuvent être sauvegardées
- **Composants exportés** : le strict minimum ; liens profonds via **App Links vérifiés**
  (`autoVerify`) et validation stricte de tous les paramètres entrants, y compris ceux du QR code
- `FileProvider` limité au seul dossier des fichiers de partage, fichiers temporaires nettoyés
- Aucune WebView ; pas de chargement de code dynamique
- Release : `debuggable=false`, R8 activé, aucun log (Timber sans arbre en release), pas de
  données sensibles dans les messages d'erreur
- Protection contre le **tapjacking** (`filterTouchesWhenObscured`) sur les actions sensibles :
  suppression de compte, déconnexion de tous les appareils, acceptation d'un téléphone en partie à côté
- Permissions demandées au moment de leur usage, avec une explication ; l'app fonctionne si elles sont refusées
- Pas de Play Integrity API (elle implique des échanges avec Google) ; l'intégrité des scores repose
  sur la validation par l'autorité (téléphone hôte ou serveur)

### Page spectateur web
- Lecture seule, accès par jeton non devinable et expirant à la fin de la partie
- **Content Security Policy** stricte, aucun script ni police tiers, aucun cookie, aucune mesure d'audience
- Seuls les pseudos et scores sont exposés, jamais les e-mails

### Chaîne d'approvisionnement et CI
- **Vérification des dépendances Gradle** (`verification-metadata.xml`) et verrouillage des versions
- Validation du wrapper Gradle dans la CI
- Analyse des vulnérabilités des dépendances (**osv-scanner**) et génération d'un **SBOM** CycloneDX à chaque release
- Analyse statique de sécurité : règles de sécurité de detekt et Android Lint, **MobSF** en analyse
  statique sur l'AAB de release, **semgrep** sur le code serveur
- Mises à jour de dépendances automatisées (Dependabot ou Renovate) avec revue manuelle
- Dépôt GitHub : protection de la branche principale, détection de secrets (secret scanning et
  push protection), aucun secret dans le dépôt, secrets de CI limités au strict nécessaire
- La clé d'envoi Play Store n'est **jamais** dans la CI ni dans le dépôt

### Tests et preuves
- Tests automatisés des règles d'accès du backend (un utilisateur ne peut ni lire ni modifier
  la partie d'un autre groupe, un code expiré ou révoqué est refusé, la limitation d'essais fonctionne)
- Test qu'une partie sur un seul téléphone n'émet **aucune requête réseau**, et qu'une partie à côté
  ne contacte **aucune adresse hors du réseau local**
- Tests de sécurité de la partie à côté : mauvaise empreinte, jeton expiré ou déjà utilisé, téléphone
  exclu, rejeu d'un événement, proposition hors tour ou hors droits, message trop gros
- Avant la publication : revue complète selon la checklist **MASVS** (`docs/security/MASVS_CHECKLIST.md`,
  avec le statut de chaque contrôle et sa justification), puis je ferai moi-même un test d'intrusion léger,
  y compris sur la partie à côté
- **Divulgation de vulnérabilités** : `SECURITY.md` dans le dépôt et `/.well-known/security.txt`
  sur le site, avec une adresse de contact dédiée

### Ouverture (P3)
- **Fait le 2026-10-28** : code publié sous **GNU GPL v3** sur un dépôt GitHub public ; noms, logos et icône hors
  licence (`TRADEMARKS.md`). Reste à étudier la distribution sur **F-Droid** (preuve forte de l'absence de traceurs ;
  la partie à côté, entièrement open source et sans Play services, y reste compatible)

---

## Publication sur le Google Play Store
Cette section décrit tout ce qu'il faut préparer pour publier Molkky Board. Le code et les
documents doivent être prêts **avant** l'ouverture du test fermé. Les règles de Google évoluent :
avant chaque étape, vérifie les exigences en vigueur dans l'aide Play Console et signale-moi tout écart
avec cette section.

### Décisions déjà prises
- **Compte développeur personnel**, nom de développeur affiché : **Yogi Studio**
  (frais uniques de 25 $, vérification d'identité faite par moi dans la Play Console)
- Adresse e-mail de contact publique **dédiée** : **yogistudiofr@gmail.com** (créée le 2026-10-28)
- **Aucun revenu** : app gratuite, sans publicité, sans achat intégré, sans abonnement
- Statut au sens du DSA européen : **non-commerçant**
- **Public cible : adultes et adolescents, pas les enfants.** Tranches d'âge déclarées : 13 ans et plus
  (à confirmer avec moi au moment du questionnaire). L'app n'entre donc pas dans le programme « Familles »

### Conséquences de « pas destiné aux enfants »
- La fiche du store et l'app ne doivent pas donner l'impression de cibler les enfants : visuels,
  mascottes et textes de la fiche orientés « jeu entre amis et en famille », sans esthétique enfantine
- Les mascottes d'avatar restent sobres et dans la direction « Nordic Bold »
- Pas de collecte de date de naissance ; les conditions d'utilisation indiquent l'âge minimum
- Le mode famille / handicap reste présenté comme une option d'équilibrage entre joueurs de niveaux différents

### « Paye-moi un café »
- Je souhaite éventuellement un lien de soutien volontaire dans l'écran « À propos » (crédits)
- **À vérifier avant toute implémentation** : la politique de paiements de Google Play encadre les dons
  et les liens vers des moyens de paiement externes. Résume-moi la règle en vigueur et propose l'option conforme
- Option par défaut, sûre : **aucun lien de don dans l'app**. Le lien « Paye-moi un café » figure uniquement
  sur le site web de l'app (voir plus bas), et l'écran « À propos » renvoie simplement vers ce site
- Vérifie aussi si recevoir des dons change mon statut DSA (commerçant ou non) ou les informations
  affichées publiquement sur ma fiche développeur, et préviens-moi

### Site web minimal de l'app
- Un dépôt **public** séparé `molkky-board-site` publié avec GitHub Pages (le dépôt du code reste privé ;
  GitHub Pages n'est pas disponible gratuitement sur un dépôt privé), en français et en anglais :
  - page d'accueil courte (présentation, captures, lien vers le Play Store) ;
  - **page `/join`** : cible des liens d'invitation de la partie à côté quand l'app n'est pas installée
    (propose de l'installer ; elle ne lit ni n'envoie jamais les paramètres placés après le `#`) ;
  - fichier `/.well-known/assetlinks.json` pour les App Links vérifiés ;
  - **politique de confidentialité** (URL exigée par le Play Store) ;
  - **page de demande de suppression de compte et des données**, accessible sans réinstaller l'app
    (exigence Google pour les apps qui proposent des comptes) ;
  - conditions d'utilisation, avec l'âge minimum ;
  - mentions « Yogi Studio » et, éventuellement, le lien de soutien
- La politique de confidentialité décrit exactement ce que fait l'app : rien n'est collecté en mode
  sans compte ; en partie à côté, échanges directs et chiffrés entre les téléphones, sans serveur ;
  en mode compte, e-mail, pseudo et données de parties, hébergés dans l'UE (selon le backend retenu),
  avec les durées de conservation et la procédure de suppression

### Exigences techniques
- Format **AAB** et Play App Signing (voir phase 5)
- **targetSdk** au niveau exigé par Google au moment de l'envoi (Android 16 / API 36 minimum
  pour les nouvelles apps depuis le 31 août 2026). Avec un targetSdk 37, gérer la permission d'accès
  au réseau local (voir phase 3 bis)
- Suppression du compte **dans l'app** et **via le site web**
- Icône adaptative avec variante monochrome
- Permissions minimales, chacune justifiée : caméra (scan du QR code, et photo de joueur en option),
  notifications (partie en cours), vibration, **appareils à proximité / réseau local** (partie à côté,
  déclarée sans usage de localisation), service de premier plan de type `connectedDevice` (téléphone hôte).
  Pas d'accès aux contacts, **pas de localisation**

### Fiche du Play Store (à préparer dans `docs/store/`, en français et en anglais)
- Titre (30 caractères maximum), description courte (80), description longue (4 000)
- Icône 512×512, image de présentation 1024×500
- Au moins 4 captures d'écran téléphone, générées à partir des tests Roborazzi
  (accueil, écran de jeu, fin de partie, Hall of Fame), éventuellement avec un bandeau de texte
- Une vignette de partage d'exemple pour illustrer la fonction sociale
- Mettre en avant « plusieurs téléphones, sans compte ni Internet »

### Déclarations dans la Play Console (prépare les réponses dans `docs/store/declarations.md`)
- **Sécurité des données** : aucune donnée collectée ni partagée en mode sans compte ; en mode compte,
  la liste exacte des données, leur finalité, le chiffrement en transit et la possibilité de suppression.
  **À vérifier** : la façon dont Google qualifie un transfert direct entre téléphones, sans serveur,
  en partie à côté (a priori ni collecte ni partage) ; résume-moi la règle avant de remplir le formulaire
- **Service de premier plan** : justification du type `connectedDevice` (vidéo de démonstration si demandée)
- **Classification du contenu** (questionnaire IARC)
- **Public cible et contenu** : 13 ans et plus, pas d'attrait spécifique pour les enfants
- **Publicités** : aucune
- **Accès à l'app** : comment un testeur de Google peut tester les fonctions avec compte
  (compte de démonstration dédié, sans données réelles)
- **Statut DSA** : non-commerçant

### Test fermé obligatoire (compte personnel)
- Au moins **12 testeurs inscrits pendant 14 jours consécutifs**. Si leur nombre passe sous 12,
  le compteur repart à zéro : viser 15 à 20 testeurs pour garder de la marge
- Prépare un **kit testeurs** dans `docs/store/testeurs.md` : message d'invitation, marche à suivre pour
  rejoindre le test, scénarios à jouer (partie solo, partie en équipes, **partie à côté en Wi-Fi et en
  point d'accès**, partage d'une vignette), et un formulaire simple de retour
- Ajoute dans l'app (version de test uniquement) un bouton « Envoyer un retour » qui ouvre un e-mail
  pré-rempli avec la version et le modèle de téléphone, sans autre donnée
- Tiens un **journal des retours et des corrections** pendant le test : il servira à remplir le
  questionnaire d'accès à la production demandé par Google à la fin des 14 jours

### Marque
- Avant l'envoi, vérifie la marque « Mölkky » (bases EUIPO et INPI) et dis-moi si le nom
  « Molkky Board » présente un risque. Prépare un **nom de repli** sans le mot « Molkky »,
  et assure-toi qu'un changement de nom ne demande de modifier que des chaînes et des visuels
  (l'identifiant `com.yogistudio.molkkyboard`, lui, ne changera jamais)

### Distribution avant le Play Store
- Pendant le développement, l'app est installée via `adb` sur mes appareils
- Google renforce la vérification des développeurs pour les apps installées hors Play Store
  (déploiement mondial prévu en 2027). Pour distribuer l'APK à mes proches avant la publication,
  évalue le **compte de distribution limitée** pour amateurs (jusqu'à 20 appareils, sans frais)
  par rapport au test fermé du Play Store, qui reste la voie privilégiée

### Checklist finale
Génère `docs/store/CHECKLIST.md` reprenant tous les points de cette section, avec leur statut,
et tiens-la à jour jusqu'à la publication.

---

## Contraintes de qualité
- Le moteur de score doit avoir une couverture de tests complète, alignée sur SCORING_SPEC.md,
  y compris les corrections de lancers anciens et les changements de joueurs en cours de partie ;
- Le protocole de synchronisation (`core:sync`) a une couverture de tests complète : validation,
  ordonnancement, idempotence, rattrapage et cas de sécurité ;
- Pas de clé ni de secret dans le code ;
- Commits réguliers et explicites ; un README qui explique comment builder et lancer Molkky Board ;
- Interface en français, avec des chaînes externalisées pour pouvoir traduire plus tard.

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

### Spécification du 2026-10-26
- 2026-10-26 — `CLAUDE.md` reprend `01-specifications-molkky-board.md` (le `prompt-molkky.md` du 25 n'avait pas été recopié en entier : sécurité et vie privée, Play Store, guide des règles, zxing-cpp au lieu de ML Kit, souveraineté… sont désormais présents).
- Changements du 26 (intro « strike » manga, pastille « Tour N », logotype compact, tableau des scores, undo en bouton, animations « 12 ! » et d'élimination) : analyse d'impact présentée, **en attente de validation** avant toute modification.

### Tour courant et modes de règles (2026-10-26)
- 2026-10-26 — Tour courant validé (`RULES.md` § 6.1, `SCORING_SPEC.md` § 16) : dérivé du journal, figé sur le tour de l'événement décisif, un retardataire rejoint le tour en cours, Mölkkout à part. Fait (commit 4d63fe5).
- 2026-10-26 — Changements du 26 validés et intégrés à J4 avant le test sur téléphone : pastille « Tour N », tableau des scores, « 12 ! », élimination, logotype compact. Intro « strike » et mise à jour d'`ARCHITECTURE.md` (zxing-cpp, souveraineté) juste après le test.
- 2026-10-26 — **Deux modes de règles** (`RULES.md` § 8.1, `SCORING_SPEC.md` § 17) : **Officiel** par défaut (verrouillé, interrupteur « Règlement de tournoi IMO ») et **Ludique** (exceptions ; par défaut sans élimination et mordu ignoré ; mode famille rangé ici). Réglages › « Règles du mode Ludique » pour les valeurs par défaut, modifiables pour une seule partie. Mode déduit de `GameConfig` (non stocké) ; records « purs » = mode Officiel uniquement. Remplace les préréglages « Classique » / « Tournoi IMO » de l'écran de création.

### Jouer (J4, 2e livraison — 2026-10-26)
- Fait : pastille « Tour N » (`RoundBadge`, bascule en ressort), bandeau des scores à poignée (tirer vers le haut ou toucher, action TalkBack) qui ouvre le **tableau des scores** (`ScoreboardRow`, classement par score, égalités au même rang, hors jeu grisés en bas, membres dépliables), animation **« 12 ! »** (`ImpactBurst` réutilisable par l'intro ; quille 12 seule ou strike, ≈ 1 s, la grille s'efface puis revient, un 12 gagnant claque avant l'écran de fin, pas de célébration si le 12 dépasse la cible), **animation d'élimination** (tampon `EliminatedStamp`, avatar qui tombe, « Pourquoi ? », jouée à l'envers si une annulation la défait), ratés qui pulsent quand le prochain serait sanctionné, logotype compact (22 sp) en haut à gauche de l'accueil.
- Corrigé : en mode Ludique sans élimination, l'aperçu n'annonce plus « éliminé » au 3e raté.
- Déroulé fait (2026-10-26) : feuille plein écran dans l'écran de jeu (même ViewModel, pour que les animations suivent une correction), groupée par tour ; un lancer se corrige (grille ou pavé, « Enregistrer » obligatoire) ou se supprime (confirmation : les lancers suivants sont réattribués par position) ; une correction refusée (`CORRECTION_INVALIDATES_LOG`) est expliquée ; la partie se rouvre ou se clôt selon le nouveau résultat ; accessible aussi depuis l'écran de fin. Lecture seule si la partie est abandonnée.
- Gestion des joueurs faite (2026-10-26) : feuille « ⋮ › Gérer les joueurs » ; ordre par boutons monter/descendre (accessibles, une main), modifiable seulement entre deux tours, avec confirmation ; retardataire (nouvelle équipe en fin d'ordre, à 0, première couleur libre, lance à la fin du tour) ; retrait avec confirmation (classé « parti », le dernier en lice gagne). Le glisser-déposer reste prévu pour la création de partie.
- Création de partie faite (2026-10-26) : solo ou équipes ; chaque joueur ajouté devient un **profil local** réutilisable (« joueurs habituels » en un tap) avec un avatar emoji distinct, photo facultative (appareil photo ou galerie, réduite à 512 px et réencodée : aucune métadonnée EXIF) ; équipes nommées (🎲 parmi ~50 noms, 8 couleurs, emblème en emoji), répartition en alternance, ⇄ pour changer d'équipe, **reprise d'une équipe connue** par ses joueurs (`memberKey`) ; **glisser-déposer** (poignée ≡, actions TalkBack monter/descendre) de l'ordre des joueurs, des équipes et des lanceurs ; **mode famille** en Ludique (score de départ, objectif, jamais éliminé, rappel de distance).
- Écran « Règles du jeu » fait (2026-10-26) : 9 sections courtes (matériel, installation avec la formation dessinée par la grille de quilles, lancer, points, relever, gagner, ratés, équipes, variantes) + FAQ des cas ambigus ; en feuille depuis le menu de jeu, en écran depuis Réglages. Schémas animés, placement pas à pas, recherche, raccourcis et accès depuis l'accueil : plus tard.
- Restent au J4 (après le test sur téléphone) : coach de fin de partie, première partie guidée (test < 30 s). Les avatars ne sont pas encore affichés sur l'écran de jeu (initiales).
- 2026-10-26 — Demandé : **tests de performance, latence et réactivité** (démarrage, saisie d'un lancer, animations sans saccade, aucune latence perceptible). À planifier : Macrobenchmark (démarrage à froid, temps de trame sur l'écran de jeu), Baseline Profiles (déjà au J12, à avancer), tests de temps de réponse du moteur et des dépôts, et questions dédiées dans les plans de test sur téléphone.
- Machine : émulateur + Gradle en parallèle saturent les 7,6 Go ; arrêter l'émulateur pendant les builds lourds.

### Conventions de code
- **Base de données** : toute modification du schéma Room impose d'augmenter la version, d'écrire la migration (`Migrations.kt`) et un test de migration (`MigrationTest`, schémas exportés dans `core/database/schemas`). Origine : plantage au lancement sur téléphone (2026-10-26, table `game_player` ajoutée au J4 sans migration).
- Documentation et UI en français ; identifiants de code en anglais.

### Clôture du J4 (2026-10-27)
- Test sur téléphone de fin de J4 : sections 1 à 6 jouées, corrections vérifiées le soir même (`docs/tests/J4-resultats.md`). Une partie complète se joue de bout en bout. Sections 7 à 11 non jouées : leurs points (fin de partie, réglages, fiabilité, performance) passent dans le plan de test du J5.
- Tranché au J4 : « Raté » sélectionne et seul « Valider » enregistre ; le tableau des scores s'ouvre par le bouton « Scores » ou d'un tap sur le bandeau (le geste de glissement ne marche pas sur téléphone : à retirer ou revoir) ; un nouvel ordre demandé en plein tour est mis en attente et appliqué au début du tour suivant ; « + Ajouter un joueur » dans chaque carte d'équipe ; règles du jeu regroupées en parties avec intertitres.
- 609 tests unitaires verts au moment de la clôture.
- **Reportés** au moment de la clôture : coach de fin de partie, première partie guidée, geste du bandeau des scores, avatars sur l'écran de jeu (faits le 2026-10-27, voir plus bas), intro « strike » (spec du 26), `ARCHITECTURE.md` à mettre à jour (zxing-cpp au lieu de ML Kit, souveraineté).

### Fin de partie et mémoire (J5 — 2026-10-27)
- `core:stats` (Kotlin pur) : résumés de manche, victoires, victoire la plus rapide, « Nouveau record », série, statistiques de joueur ; tout est recalculé depuis les journaux (`docs/STATS_SPEC.md`, 27 cas). « Nouveau record » seulement si un record existait déjà (la toute première victoire n'est pas annoncée).
- Base Room en **version 3** : table `molkkout_throw` (journal du Mölkkout), migration 2 → 3 et son test.
- Une partie en plusieurs manches **reste ouverte** jusqu'à la fin du match (reprise depuis l'accueil) ; une partie close = match fini ou abandonné. Création : « Une manche » (défaut) ou « Match en 2 manches » (belle aux points, Mölkkout si égalité ; en IMO, alternance sans Mölkkout).
- Revanche : mêmes équipes et règles, ordre au choix sur l'écran de fin (rotation par défaut, identique, le dernier commence, le vainqueur joue en dernier) ; la série des mêmes équipes s'affiche dès deux manches consécutives.
- Victoire : « 50 » qui explose dans la couleur de l'équipe, confettis-quilles en Canvas (graine fixe), nom et trophée, double vibration, bandeaux « Partie parfaite » et « Nouveau record » ; fondu si « Réduire les animations ».
- Historique (`feature:history`), Hall of Fame et statistiques (`feature:stats`, Vico 3.3.1) ; statistiques limitées aux joueurs habituels (profils locaux).
- Notification de partie en cours : locale (aucun push), permission `POST_NOTIFICATIONS` proposée une fois avec explication, intention explicite vers l'activité (pas de lien profond), identifiant de partie validé (UUID). Réglage pour la désactiver.
- Annonces vocales : TTS du téléphone, muettes en silencieux/vibreur et sous TalkBack ; débit et volume réglables. Formulation neutre « Léa est hors jeu » (pas d'accord en genre deviné).
- L'APK ne déclare pas la permission `INTERNET` : aucune requête réseau possible en mode local.
- Plan de test : `docs/tests/J5-plan-de-test.md` (15 questions en QCM).
- Non fait au J5 : son de victoire (optionnel, désactivé par défaut) ; le réglage « Sons » reste sans effet.

### Restes du J4 (faits le 2026-10-27, après le J5, test sur téléphone à venir)
- Coach de fin de partie : à 12 points ou moins de la cible, « Il te faut 7 : la quille 7 seule, ou 7 quilles », quille surlignée sur la grille, « Zone de danger » à 3 points ou moins ; un tap ouvre les règles ; désactivable (Réglages › Coach).
- Première partie guidée en **4 bulles** (`GuideBubble`, design system) : ajouter les joueurs (création), toucher les quilles, « Valider », annuler. Les bulles ne captent que leurs boutons (pas de `Surface`, qui bloquait la rangée du fond : défaut trouvé par le test) ; « Passer le guide » et Réglages › « Revoir la prise en main ». Test Robolectric : premier lancer validé en deux taps sur l'écran de jeu.
- Bandeau des scores : le glissement vertical est détecté à la passe initiale (les puces et le défilement horizontal ne l'interceptent plus) ; testé par un balayage simulé. Le bouton « Scores » reste.
- Avatars en jeu : emoji du profil pour un joueur seul, emblème pour une équipe (banque déplacée dans `core:designsystem`). Les photos ne sont pas encore affichées en jeu.
- Sélection vidée avant l'écriture d'un lancer : le joueur suivant n'hérite plus, même un instant, des quilles du précédent.
- Intro « strike » faite (2026-10-27) : storyboard de la spec du 26 (lignes de vitesse, image figée sur aplat safran, quilles intactes qui s'envolent, scintillement, signature, logotype qui claque). Version courte ≈ 1,7 s. **Timings à valider sur téléphone** (captures des étapes dans `feature/intro/src/test/screenshots`).
- 2026-10-27 — Retour sur téléphone : intro jugée bonne ; **bulle « 12 ! » retirée de l'intro** (n'apportait rien). Elle reste en jeu pour les lancers à 12.
- `ARCHITECTURE.md` mis à jour : zxing-cpp au lieu de ML Kit ; section souveraineté (CLOUD Act, Supabase auto-hébergé chez Scaleway/OVHcloud, coût et exploitation). **Point ouvert pour le J8** : rester sur Supabase Cloud Paris ou basculer en auto-hébergé avant l'ouverture au public (l'encart « Vie privée » en dépend).

### Partage social (J6 — 2026-10-27, en attente de validation : captures + vidéo)
- Moments détectés depuis le journal (`core:stats`, `STATS_SPEC.md` § 8) : record, partie parfaite, remontée (≥ 15 points), strike, victoire, « 12 ! », retour depuis 49, élimination ; un seul par type, le plus fort proposé en premier.
- `feature:share` : 4 modèles (Podium, Tableau de score, Moment fort, Aïe !) × 3 formats (9:16 par défaut, 4:5, 1:1), composables capturés en 1 080 px (`GraphicsLayer`), densité fixe (identiques sur tous les téléphones), QR code zxing vers la **fiche Play Store** (pas de lien profond).
- Vie privée : pseudos et emoji seulement, **aucune photo** sur les vignettes (l'option d'activer les photos n'est pas proposée) ; noms masquables (« Joueur N ») ; PNG sans métadonnées ; `FileProvider` limité à `cache/share`, fichiers de plus d'une heure supprimés ; aucune requête réseau.
- Mini-vidéo MP4 (Media3 Transformer 1.11.1, H.264 ≈ 4 Mbit/s, muette, 8,8 s) et GIF 360 × 640 (encodeur maison) ; progression et annulation. Émulateur : 3,0 Mo, 19 à 28 s d'encodage logiciel — **le temps sur téléphone (objectif < 10 s) reste à mesurer**.
- Raccourcis : feuille de partage Android + WhatsApp direct. **Instagram Stories** exige un identifiant d'app Meta (compte développeur Meta) et **TikTok** son SDK Share Kit (SDK tiers, contraire à « aucun SDK ») : non intégrés, en attente de décision ; les deux restent accessibles par la feuille de partage.
- « Partager ce moment » sur l'écran de fin, sous la forme d'un bouton secondaire, une seule proposition par partie, jamais avant la revanche.
- Non faits au J6 : « Défier » (code à 6 caractères, Phase 4), bilan du mois / de la saison de groupe (groupes en Phase 4), sons des clips, mesure de viralité (à proposer avant toute implémentation, côté serveur), mise à jour du modèle de menaces (à créer avant la Phase 4, avec le partage).

### Confiance et langues (J7 — 2026-10-27)
- **Sauvegarde / restauration** (Réglages › Données) : fichier JSON lisible (`molkky-board-backup` v1) avec profils, équipes et parties (journaux complets, l'état se recalcule) ; il sert aussi d'**export RGPD**. Les photos ne sont pas incluses (elles restent sur le téléphone). Sélecteur de documents Android : aucune permission de stockage.
- Restauration **en ajout seul**, en une transaction : une partie déjà présente est ignorée, jamais écrasée. Le fichier est traité comme non fiable : 20 Mo au plus, identifiants et textes bornés sans caractères de contrôle, manches contiguës, journaux rejoués, identifiants d'événements uniques, champs réencodés par l'app ; une partie invalide est écartée et comptée dans le bilan.
- **Supprimer toutes mes données** (confirmation) : parties, profils, équipes, photos et cache de partage ; les réglages sont conservés.
- **Traduction anglaise complète** (`values-en` dans chaque module, ~600 chaînes, mêmes clés et mêmes paramètres vérifiés). « Ludique » = *Casual*, « Valider » = *OK* (« Confirm » était tronqué en police 2,0). Captures Roborazzi en anglais (jeu, jeu en police 2,0, règles, création en équipes).
- Langue (demande du 2026-10-27) : choix **🇫🇷 Français / 🇬🇧 English** seulement, « Langue du téléphone » retirée. Sans choix explicite, l'app suit un téléphone en anglais et reste en français sinon (textes par défaut en français) ; la case cochée montre la langue affichée. Annonces vocales dans la langue de l'app.
- Vitrine du design system retirée des Réglages (validée, sans intérêt pour l'utilisateur), demande du 2026-10-27.
- Tests : Robolectric fixé en français par défaut (`qualifiers=fr` et préfixe `fr-`), sinon les tests passaient en anglais ; les dates des captures sont désormais en français.
- CI : **Android Lint** ajouté (`lintDebug`) ; corrigé : ressources lues via `LocalContext` dans le partage (textes figés après un changement de langue).

### Phase 4 mise de côté, le reste d'abord (2026-10-27)
- Demandé le 2026-10-27 : **multi-téléphones (Phase 4) mis de côté**, hébergement non tranché (Supabase Cloud, VPS UE ou NAS pour le développement ; détail dans la réponse du jour). On fait tout ce qui ne dépend pas d'un serveur, par lots : aides aux débutants, À propos, sons, compléments, sécurité et CI, Phase 5, documents Play Store.
- Lot 1 — aides aux débutants (fait) :
  - **Placer les quilles** pas à pas (`PlacementGuide`) : apparition quille par quille de 1-2 à 7-9-8, position en clair (« Quille 12 : troisième rangée, au centre droit »), lue par TalkBack et à voix haute sur demande, « Tout afficher », vue de côté avec la distance. Dans les règles, à l'accueil, en feuille pendant la partie (bouton « Quilles » de la barre), entre deux manches (schéma miniature) et en raccourci de l'icône.
  - **« Prêts à jouer ? »** au début de la première manche : 3 premières parties, puis seulement si réactivée (« Ne plus afficher », Réglages) ; propose le **mode débutant** (rappel « Relevez les quilles » après chaque lancer, explication une seule fois au premier dépassement et au premier deuxième raté).
  - **Règles** : recherche (sans accents), sommaire, schémas animés (points, dépassement et retour à 25, trois ratés) joués deux fois puis figés (pas d'animation sans fin), « Rejouer ».
  - **Raccourcis de l'icône** (dynamiques, dans la langue de l'app) : Nouvelle partie, Placer les quilles, Règles du jeu ; « Rejoindre une partie » viendra avec la Phase 4. Intention explicite, extra validé contre une liste fermée.
  - Titre « Partie » masqué en police ≥ 1,3 (les boutons de la barre passent avant).
- Lot 2 — À propos (fait) : encart **« Vie privée »** limité à ce qui est vrai aujourd'hui (aucune publicité ni traceur ni mesure d'audience ; tout reste sur le téléphone, l'app ne demande même pas l'accès à Internet ; sauvegarde, export et suppression dans Réglages › Données) ; la phrase « avec un compte… » viendra avec la Phase 4. Crédits : polices (SIL OFL 1.1) et **AboutLibraries 15.3.0** en mode hors ligne (aucun accès réseau au build, 164 bibliothèques, licences Apache 2.0 / BSD-3 / MIT). Liens vers la politique de confidentialité, les conditions et le site : **en attente du site** (dépôt public `molkky-board-site`, à créer avec ton accord).
- **Rapport de plantage à la demande** : le dernier plantage est écrit sur le téléphone (version, Android, modèle, types d'exception et pile ; **messages d'exception omis**, aucune donnée de partie) ; au retour sur l'accueil, l'app en montre tout le contenu et propose « Envoyer par e-mail » ou « Ne pas envoyer » (les deux l'effacent). Rien n'est envoyé automatiquement.
- **« Envoyer un retour »** (versions de test seulement, `BuildConfig.FEEDBACK`) : e-mail prérempli avec la version et le modèle. **À faire par toi : créer l'adresse de contact dédiée** (`SUPPORT_EMAIL` est vide : l'utilisateur saisit le destinataire en attendant).
- Lot 3 — **Sons maison** (fait) : choc de l'intro, « 12 ! », victoire, élimination ; synthétisés par `tools/sounds/generate.py` (aucun échantillon tiers, libres de droits, déterministes), 125 Ko. Réglage « Sons » désactivé par défaut, muets en silencieux et en vibreur (`SoundPool`, usage « jeu »).
- Lot 4 — Compléments (fait, `STATS_SPEC.md` § 9) : **records secondaires** dans le Hall of Fame (« Autres records » : plus de « 12 » en une partie, plus longue série sans raté, plus de retours à 25, plus belle remontée ; règles officielles, période filtrée) et **badges** dans Statistiques (première victoire, partie parfaite, Phénix, Sniper ; les badges à décrocher sont grisés). Totaux « 12 » et retours à 25 par joueur.
- 2026-10-27 — **Version 0.5.0 (build 5)**, demandée.
- **Crédits** (texte fourni le 2026-10-27, ton geek et léger) : écran « Crédits » depuis À propos, défilement façon générique (arrêté au premier tap ou glissement, statique avec « Réduire les animations »), bloc « terminal » vert forêt en Inter à chiffres tabulaires lu par TalkBack comme une phrase, version et build lus depuis `BuildConfig` (« ./gradlew assembleDebug » en debug, « assembleRelease » en release, pour rester vrai), scène post-générique avec le scintillement de l'intro joué une seule fois.
  - **Signalé, non affiché** : « Aux testeurs du test fermé… » est faux tant que le test fermé n'a pas eu lieu ; la ligne sera ajoutée après.
  - **À vérifier** : « Mölkky® est une marque de Tactic Games Oy » (contrôle EUIPO/INPI prévu avant la publication) ; « Traceurs : 0 » sera adossé au contrôle CI des dépendances (lot sécurité, à venir).
- 2026-10-27 — Corrigé : « Voir le générique » n'ouvrait rien (action jamais branchée dans la navigation ; le paramètre n'a plus de valeur par défaut). Générique aligné sur le texte fourni : « ✨ » de la scène post-générique, bloc « terminal » vert forêt aussi en thème sombre. Textes modifiés à la demande : « Compilée avec amour, café et… », « Aucun arbre n'a été abattu pour réaliser cette application. »
- 2026-10-27 — **« Introduction courte » retirée des Réglages** (jugée inutile) : l'intro complète est toujours jouée.

- 2026-10-27 — Section « Open source » retirée du générique (redondante avec À propos › Bibliothèques open source, qui reste).
- 2026-10-27 — **Crédits intégrés à « À propos »** dans un bloc à part (plus d'écran dédié ni de défilement automatique ; scintillement final quand on arrive en bas) ; la liste des bibliothèques reste sur son écran. Retirés à la demande : « exit 0 » ; « Cookies » affiche « 0 (pas les crêpes) ». Statistiques : grands chiffres pour les nombres seulement.

### Test de rattrapage J4 → 0.5.0 (2026-10-27, `docs/tests/rattrapage-J4-0.5-resultats.md`)
- Validés sur téléphone : intro, accueil, raccourcis, bouton « Quilles », victoire, revanche, match, historique, Hall of Fame, autres records, badges, partage (moment, vidéo < 10 s, GIF), notification, données (sauvegarde, restauration, suppression), anglais, règles, fluidité, reprise, grande police.
- Décisions : **masquage des noms au partage retiré** (écart volontaire à la spec « Vie privée ») ; **QR code retiré des vignettes** (écart volontaire à la boucle virale) ; « open source » = construite avec des bibliothèques open source, code privé ; voix et sons coupés **en silencieux seulement** ; écran bloqué en portrait ; placement en vue globale (pas à pas et vue de côté retirés) ; geste du bandeau des scores retiré au profit d'une icône podium ; avatars emoji jugés enfantins (à remplacer) ; « Mordu compté comme raté » retiré des règles Ludique.
- Corrections C1–C16 et nouvelles fonctions N1 (photo du moment) et N2 (petite animation à chaque lancer) : voir le compte rendu.

- 2026-10-27 — **Coach de fin de partie et mode débutant supprimés** (jugés inutiles au test ; écarts volontaires à la spec). « Mordu compté comme raté » retiré des règles Ludique ; **écran bloqué en portrait**.
- Corrections restantes du test (à reprendre) : C1 signature Yogi Studio, C2 icône podium et geste retiré, C3 placement en vue globale, C6 avatars sobres, C8 « Terminer », C9 voix et sons en vibreur, C10 Mölkkout expliqué, C11 QR retiré, C12 « aucune donnée collectée », C13 accessibilité, C15 tremblement du bon joueur, C16 caractères de mise en forme ; D1 masquage des noms retiré ; N1 photo du moment, N2 animation par lancer.
- 2026-10-27 — Demandé : **N3 animation de tension** quand le joueur actif est à un raté de la sanction (après `missLimit − 1` ratés, et seulement si un raté de plus est sanctionné : en Ludique, selon la règle choisie — élimination, retour à 0 ou à la moitié ; rien si « on continue » ou joueur exempté). À jouer au moment de son lancer, plus marquée que la pulsation actuelle des ratés, sans bloquer la saisie, avec une version calme si « Réduire les animations ».

### Corrections du test de rattrapage (faites le 2026-10-28)
- Faits : C1 signature Yogi Studio à 1,5 s (fin de l'envol raccourcie, total 4,5 s), C2 icône podium à gauche du bandeau (geste et mot « Scores » retirés), C3 placement en vue globale (pas à pas, voix et vue de côté retirés), C8 bouton « Terminer », C9 voix et sons coupés en silencieux seulement (et première annonce plus perdue pendant l'initialisation du moteur vocal), C10 belle et Mölkkout expliqués (écran de fin à 1-1, section et démo dans les règles), C11 QR retiré des vignettes et du clip, C12 « Aucune donnée collectée » en tête d'À propos + bibliothèques open source, C15 autocollant « Aïe ! » sur le joueur qui a dépassé, C16 caractères de mise en forme filtrés à la saisie et refusés à la restauration, D1 masquage des noms retiré.
- Restent (jalon J7b) : C6 avatars sobres, C13 accessibilité, N1 photo du moment, N2 animation à chaque lancer, N3 tension avant la sanction.

### Spécification du 2026-10-28 — « Partie à côté »
- 2026-10-28 — `CLAUDE.md`, `01-specifications-molkky-board.md` et `02-journal-decisions.md` reprennent la version du 28 (fusion sans toucher à cette section).
- Décidé par la spec : **phase 3 bis** (plusieurs téléphones sur le même terrain, sans serveur ni compte ni Internet, le téléphone de l'organisateur est l'autorité) ; module `core:sync` (protocole unique, abstraction de transport, validation pure partagée avec le futur serveur) ; briques open source uniquement, **Google Nearby écarté** ; jamais de localisation ; pas de code court dans ce mode ; photos jamais transmises ; **prototype sur deux téléphones réels avant l'implémentation complète**.
- **En attente de validation** : révision d'`ARCHITECTURE.md` (§ 6 bis, § 9 jalons J7b, A1, P0, S1–S3, R1–R4) et premier `docs/security/THREAT_MODEL.md`.
- Constat technique (vérifié dans le code de Ktor) : le moteur **serveur CIO ne gère pas TLS** ; options à départager au prototype (Ktor Netty, ou `SSLServerSocket` de la plateforme avec les trames WebSocket de Ktor) ; client Ktor sur moteur OkHttp proposé.
- Points non tranchés (porteur du projet) : nouvelle formulation de l'encart « Vie privée » ; report de la phase 4 en V2 ; qualification du transfert direct dans le formulaire « Sécurité des données ».
- 2026-10-28 — **Écarts de vie privée de la partie à côté acceptés** (permission `INTERNET` et permissions réseau dans l'APK). Rappel : `INTERNET`, Wi-Fi et service de premier plan sont des permissions normales, jamais demandées à l'écran ; seules « Appareils à proximité » (hôte en point d'accès, Android 13+), « Réseau local » (Android 17) et la caméra le sont. La garantie « rien vers Internet » repose donc sur le test d'adresses locales. Formulation proposée pour À propos, à appliquer quand `INTERNET` sera ajoutée (S2) : « Aucune donnée collectée. Molkky Board ne se connecte jamais à Internet. Quand plusieurs téléphones jouent ensemble, ils échangent directement entre eux, en chiffré, sur le réseau local. » (**formulation validée le 2026-10-28**).
- 2026-10-28 — Ordre validé : **J7b d'abord** (restes du test de rattrapage), puis A1, P0… selon la révision d'`ARCHITECTURE.md`.
- 2026-10-28 — Choix sur planches : **C6 = emblèmes au trait** (16 icônes `LineEmblems`, blanc sur la couleur du joueur ou de l'équipe, même banque pour joueurs et équipes) à la place des emoji ; **N1 = photo du moment façon Strava** (photo plein cadre, voile sombre, titre, score, lancers et tour en blanc, logotype discret).
- 2026-10-28 — **J7b fait** : C6 emblèmes au trait (anciens emoji lus sans migration), C13 contrôles d'accessibilité automatiques (ATF via Roborazzi : poignée des feuilles 64 dp, pastilles de couleur 56 dp, libellés), N1 photo du moment (modèle « Photo », sans permission ni conservation), N2 envol des quilles à chaque lancer, N3 tension avant la sanction. **Version 0.6.0 (build 6)**. Plan de test : `docs/tests/J7b-plan-de-test.md` (20 questions).
- 2026-10-28 — **A1 fait** (adaptations pour plusieurs appareils, branche principale) : module `core:sync` avec `validate` (droits par rôle, tour, moteur, `STALE`) et `GameCommand` (événement, manche suivante, Mölkkout, revanche ; idempotentes) ; `GameRepository.apply` est l'unique porte d'entrée ; base Room v4 (`game.origin`, `authorDevice`) avec migration testée ; animations et annonces déduites du changement d'état ; tâche `check<Variante>NoInternet` en CI. Prochaine étape : prototype P0 (branche `proto/partie-a-cote`, deux téléphones).
- 2026-10-28 — **Licence : GNU GPL v3 « ou toute version ultérieure »** (choix du porteur du projet ; remplace D2 « code privé »). `LICENSE` (texte officiel), `TRADEMARKS.md` (noms « Molkky Board » et « Yogi Studio », logos et icône hors licence ; un dérivé redistribué change de nom, d'icône et d'identifiant), section Licence du README. Compatible F-Droid. Restent à faire : passer le dépôt en public (sur accord explicite), mention de la licence dans À propos, en-têtes de licence éventuels dans les fichiers source, accord de contribution si des contributions extérieures sont acceptées.
- 2026-10-28 — **Dépôt GitHub public** (`GuillaumeCHR/molkky-board`, GPL v3). Avant publication : historique réécrit (auteur → adresse « noreply » GitHub, journal de plantage JVM retiré), push forcé, tag `v0.6.0` déplacé ; ce dépôt local signe désormais avec l'adresse « noreply ». Sauvegarde de l'historique d'origine : `~/molkky-avant-nettoyage-2026-10-28.bundle`. À propos mentionne la licence.
- 2026-10-28 — **Adresse de contact dédiée : yogistudiofr@gmail.com** : destinataire des retours et rapports de plantage (`SUPPORT_EMAIL`), ligne « Contact » d'À propos, README, `TRADEMARKS.md`, `SECURITY.md` (divulgation de vulnérabilités).
- 2026-10-28 — **Spec de CLAUDE.md alignée sur les retours des tests** (passages marqués « retour du test ») : coach et mode débutant supprimés, placement en vue globale, intro courte retirée (code compris), icône podium, emblèmes au trait, vignettes sans QR ni masquage, voix et sons coupés en silencieux seulement, portrait, envol des quilles, tension avant la sanction, photo du moment, GPL v3, contact. `01-specifications-molkky-board.md` reste la version d'origine.
- 2026-10-28 — À propos : carte « Licence et contact » (GPL v3, lien vers le code source, marques réservées, yogistudiofr@gmail.com et bouton « Écrire à Yogi Studio »).
