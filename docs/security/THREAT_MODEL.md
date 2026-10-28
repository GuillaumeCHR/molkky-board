# Modèle de menaces — Molkky Board

> Analyse de risques légère inspirée d'EBIOS Risk Manager. Première version : 2026-10-28, **avant la phase 3 bis
> (« Partie à côté »)**. À mettre à jour à chaque fonctionnalité qui ajoute des données ou une surface
> d'exposition (partie à côté, partage, lien spectateur, comptes, groupes, photos).
> Statut : **proposition, en attente de validation.** Les mesures marquées ✅ existent déjà dans l'app ; les autres
> sont prévues au jalon indiqué (`ARCHITECTURE.md` § 9).

## 1. Périmètre

| Mode | Surface d'exposition |
|---|---|
| Un seul téléphone (V1 actuelle) | aucune réseau (`INTERNET` non déclarée) ; fichiers de sauvegarde importés ; partage d'images et de vidéos ; notification ; raccourcis de l'icône |
| Partie à côté (phase 3 bis) | serveur WebSocket sur TLS ouvert par le téléphone hôte sur le réseau local ou son point d'accès ; QR code d'invitation ; lien `molkkyboard://join#…` |
| Jeu à distance, comptes, groupes (phase 4) | **à compléter** si la phase est maintenue en V1 |

## 2. Biens essentiels

| Bien | Besoin | Commentaire |
|---|---|---|
| Intégrité des scores, du journal et des records | intégrité | c'est la promesse de l'app ; un client modifié ne doit pas pouvoir forcer un score |
| Disponibilité de la partie en cours | disponibilité | une partie ne doit jamais être perdue, ni bloquée par un autre téléphone |
| Pseudos, avatars, historique des joueurs | confidentialité (faible) | données peu sensibles, mais personnelles ; jamais de photo transmise |
| Absence de trafic vers Internet | confidentialité, confiance | engagement public « aucune donnée collectée » |

Biens supports : téléphone hôte (clé de partie, jetons de session, journal Room), téléphones invités, réseau
local ou point d'accès, QR code affiché.

## 3. Sources de risque

| Source | Motivation | Capacité |
|---|---|---|
| Joueur tricheur | gagner, battre un record | app modifiée, rejeu de messages |
| Curieux sur le même Wi-Fi | voir, perturber | scan de ports, homme du milieu sur un Wi-Fi partagé |
| Personne qui photographie le QR code | rejoindre sans y être invitée | photo par-dessus l'épaule, capture d'écran partagée |
| Invité exclu | revenir dans la partie | réutilisation de son jeton ou d'un ancien QR |
| Contenu malveillant dans un nom | casser l'affichage, tromper | caractères de contrôle, inversion du sens de lecture, noms très longs |
| Fichier de sauvegarde piégé | corrompre la base | JSON forgé |

## 4. Scénarios et mesures

### Un seul téléphone (existant)

| # | Scénario | Mesures | Risque résiduel |
|---|---|---|---|
| L1 | Fichier de sauvegarde forgé (taille, champs, journal incohérent) | ✅ 20 Mo maximum, identifiants et textes bornés, caractères de contrôle et de mise en forme refusés, journaux rejoués, restauration en ajout seul et en une transaction | faible |
| L2 | Nom qui inverse le sens de lecture ou contient des caractères invisibles | ✅ filtrés à la saisie, refusés à la restauration (C16, 2026-10-28) | faible |
| L3 | Fuite de données par une bibliothèque | ✅ aucune permission `INTERNET` ; aucun SDK d'analytics ni de publicité ; contrôle CI des dépendances prévu (lot sécurité) | faible |
| L4 | Métadonnées dans une image partagée | ✅ PNG régénérés sans métadonnées ; photos réencodées sans EXIF ; `FileProvider` limité au cache de partage, purgé | faible |

### Partie à côté (phase 3 bis)

| # | Scénario (spec du 2026-10-28) | Mesures prévues | Risque résiduel |
|---|---|---|---|
| N1 | **QR code photographié ou capturé**, puis utilisé par un tiers | QR considéré comme observable : jeton d'invitation de 128 bits, à usage unique, valable 2 min, renouvelé après chaque usage, comparé à temps constant ; **validation humaine** de chaque arrivée (« 🦊 Léa veut rejoindre ») ; aucun secret durable dans le QR | faible : le tiers doit être à portée du réseau et l'organisateur doit l'accepter |
| N2 | **Téléphone qui se fait passer pour l'hôte** (homme du milieu sur le Wi-Fi ou le point d'accès, faux point d'accès au même nom) | certificat éphémère par partie, empreinte SHA-256 dans le QR, **épinglage strict** sans repli (« Ce n'est pas le téléphone de l'organisateur ») ; TLS standard (Conscrypt) ; en point d'accès, mot de passe WPA2/WPA3 généré par Android et transmis par le QR | faible ; QR lui-même falsifié (affiche substituée) : hors périmètre, le QR est lu sur l'écran de l'organisateur |
| N3 | **Invité exclu qui tente de revenir** | jeton de session de 256 bits par téléphone, révoqué à l'exclusion ; `DEVICE_REMOVED` ; une nouvelle arrivée repasse par la validation humaine | faible |
| N4 | **Rejeu d'événements** (message capturé ou renvoyé) | TLS (pas de capture en clair) ; idempotence par `eventId` (un doublon reçoit la même réponse sans être appliqué) ; numérotation par l'hôte ; `STALE` pour une correction fondée sur un état dépassé | faible |
| N5 | **Client modifié qui force un score** ou saisit pour un autre | l'hôte valide chaque proposition avec la fonction pure partagée (appareil accepté, droits, tour, lancer cohérent) et recalcule l'état ; l'invité ne fait que proposer ; seul l'hôte numérote | faible ; **un hôte modifié** peut tout falsifier dans sa partie : accepté (c'est l'organisateur), les records de groupe en tiendront compte en phase 4 |
| N6 | **Hôte qui quitte la partie** (départ, batterie, app tuée) | service de premier plan ; redémarrage avec le même certificat et reconnaissance par jeton ; sinon partie en lecture seule chez les invités, chacun garde son historique | moyen (disponibilité) : transfert du rôle d'hôte en P3 |
| N7 | **Inondation de messages** (déni de service local) | 64 Ko par message, 12 téléphones au plus, débit limité par téléphone, fermeture des connexions non authentifiées après délai, serveur sur l'interface du réseau de la partie uniquement et port aléatoire | faible à moyen sur un Wi-Fi public : le mode point d'accès est conseillé |
| N8 | Trafic qui part par les données mobiles ou vers Internet | l'invité lie ses sockets au `Network` de la partie ; test automatisé (adresses privées et lien-local uniquement) et contrôle logcat | faible |
| N9 | Fuite des jetons ou de la clé de partie | clé dans l'Android Keystore, supprimée à la fin ; jetons chiffrés avec une clé du Keystore, exclus des sauvegardes Android (`dataExtractionRules`) ; aucun jeton dans les journaux (pas de logs en release) | faible |
| N10 | Lien `molkkyboard://join#…` forgé ouvert depuis une autre app | validation stricte de tous les paramètres (version, adresse privée uniquement, port, longueur exacte de l'empreinte et du jeton) ; aucune action sans le parcours normal (vérification de l'empreinte, validation humaine) | faible |
| N11 | Pseudo malveillant transmis par un invité | même filtrage que la saisie locale (`withoutUnsafeChars`, longueur bornée), appliqué par l'hôte à la réception | faible |
| N12 | Photo d'un joueur transmise aux autres | jamais : seuls pseudo, avatar emoji ou mascotte et événements transitent | nul |

## 5. Choix documentés

- **Pas d'épinglage vers un backend** (phase 4) : risque de panne lors des rotations de certificat du fournisseur.
  **Épinglage strict en partie à côté** : le certificat est éphémère, il n'y a aucune rotation à craindre.
- **Base Room non chiffrée** : données peu sensibles, protégées par le chiffrement du système de fichiers Android.
- **Pas de Play Integrity** : l'intégrité des scores repose sur la validation par l'autorité (hôte ou serveur).

## 6. À compléter

- Phase 4 (comptes, serveur, groupes, codes à 6 caractères, énumération), si elle reste dans la V1.
- Lien spectateur web, photos synchronisées : quand ces fonctions seront planifiées.
- Après le prototype P0 : confirmer N2 (épinglage), N7 (limites) et N8 (liaison au `Network`) sur appareils réels.
