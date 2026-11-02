# Revue OWASP MASVS v2 — Molkky Board

> Référentiel : OWASP MASVS v2, niveau **L1** pour toute l'app, contrôles renforcés sur le stockage et
> l'authentification de la partie à côté ; méthode de vérification : OWASP MASTG. Périmètre : **V1 sans compte**
> (phase 4 reportée en V2), version **0.8.5 (build 13)**. Première revue : 2026-11-02, par lecture du code et tests
> automatisés. Le test d'intrusion léger du porteur du projet reste à faire (`CHECKLIST.md`).
>
> Statuts : ✅ conforme · 🛠️ corrigé lors de cette revue · ⚠️ écart accepté (justifié) · ⬜ à faire · — sans objet.

## MASVS-STORAGE — stockage des données sensibles

| Contrôle | Statut | Constat et justification |
|---|---|---|
| STORAGE-1 : données sensibles stockées de façon sûre | ✅ | Données de jeu (pseudos, scores, journaux) dans Room, **non chiffrée** : peu sensibles, protégées par le chiffrement du système de fichiers Android (choix documenté, `THREAT_MODEL.md` § 5). Clé de la partie à côté dans l'**Android Keystore** (EC P-256, non exportable), supprimée à la fin du partage. **Jeton de session de l'invité gardé en mémoire seulement**, jamais écrit sur le disque : plus strict que la spec (« chiffré avec une clé du Keystore »), au prix d'une partie rejointe qui repasse en lecture seule si l'app de l'invité est tuée (il rescanne le QR code) |
| STORAGE-2 : pas de fuite de données sensibles | ✅ | Sauvegarde cloud Android **désactivée** pour tous les domaines (`data_extraction_rules.xml`) ; transfert d'appareil à appareil autorisé (parties locales), qui n'emporte ni clé du Keystore ni jeton (en mémoire). Aucun log en release (Timber planté en debug seulement, aucun `android.util.Log` dans le code). Photos réencodées sans EXIF ; vignettes PNG sans métadonnées ; `FileProvider` limité à `cache/share`, purgé après une heure. Rapport de plantage : messages d'exception omis, aucune donnée de partie, envoi uniquement à la demande |

## MASVS-CRYPTO — cryptographie

| Contrôle | Statut | Constat et justification |
|---|---|---|
| CRYPTO-1 : cryptographie forte et à jour | 🛠️ | TLS de la plateforme (Conscrypt), aucune crypto maison. **Corrigé** : protocoles et suites **imposés** des deux côtés — TLS 1.3 (`AES-GCM`, `CHACHA20-POLY1305`), repli TLS 1.2 limité à `ECDHE_ECDSA` + AEAD pour les invités sous Android 8-9 ; TLS 1.0 / 1.1, CBC, RC4, 3DES et échanges de clés RSA refusés (`STRICT_PROTOCOLS`, `STRICT_SUITES`, `ConnectionSpec.RESTRICTED_TLS`). Testé : TLS 1.3 négocié, TLS 1.2 + GCM accepté, toute suite CBC refusée |
| CRYPTO-2 : gestion des clés conforme | ✅ | Clé de partie générée dans le Keystore pour chaque partage, certificat valable ± 1 jour, supprimée à la fin. Jetons : `SecureRandom`, 128 bits (invitation) et 256 bits (session), comparés à temps constant (`MessageDigest.isEqual`) |

## MASVS-AUTH — authentification et autorisation

| Contrôle | Statut | Constat et justification |
|---|---|---|
| AUTH-1 : protocoles d'authentification sûrs | ✅ | Pas de compte en V1. Partie à côté : hôte authentifié par **épinglage strict** de l'empreinte SHA-256 du QR code, sans repli (`PinnedTrustManager`) ; invité authentifié par jeton d'invitation à usage unique valable 2 min, puis **validation humaine** par l'organisateur, puis jeton de session révocable |
| AUTH-2 : authentification locale sûre | — | aucune authentification locale |
| AUTH-3 : opérations sensibles protégées | ✅ | Droits vérifiés par l'hôte pour chaque proposition (fonction pure `validate`, partagée avec le futur serveur) : rôles compteur, arbitre, organisateur ; gestion des joueurs et abandon réservés à l'organisateur |

## MASVS-NETWORK — communications

| Contrôle | Statut | Constat et justification |
|---|---|---|
| NETWORK-1 : trafic chiffré selon les bonnes pratiques | 🛠️ | `cleartextTrafficPermitted="false"` partout ; WebSocket uniquement en `wss://` ; serveur lié à l'adresse du réseau de la partie, port aléatoire. **Corrigé** : (1) taille des **trames** bornée à 64 Ko par le serveur avant lecture (`Draft_6455`), auparavant seule la taille du message reçu était contrôlée, après mise en mémoire ; (2) une connexion qui ne présente aucun jeton valable est **fermée au bout de 15 s** (un intrus pouvait occuper indéfiniment les 24 places en envoyant des `Ping`) ; (3) l'invité **accuse la fermeture** décidée par l'hôte (exclusion, refus) au lieu d'attendre le délai réseau. Débit limité à 20 messages/s par connexion, 24 connexions au plus, 12 invités |
| NETWORK-2 : identité du point distant vérifiée | ✅ | Épinglage strict de l'empreinte ; le nom d'hôte n'est pas vérifié (adresse IP locale sans nom, l'empreinte suffit) : choix documenté dans `Transport.kt`. Testé : une autre empreinte est refusée à la poignée de main, sans nouvelle tentative |

## MASVS-PLATFORM — interaction avec la plateforme

| Contrôle | Statut | Constat et justification |
|---|---|---|
| PLATFORM-1 : IPC sûrs | ✅ | Seule l'activité principale est exportée (lanceur) ; services, `FileProvider` et fournisseur d'avatars non exportés ; `ProfileInstallReceiver` (AndroidX) protégé par `DUMP`. **Aucun intent-filter de lien profond** en V1. Extras des raccourcis comparés à une liste fermée, identifiant de partie de la notification validé (UUID) |
| PLATFORM-2 : WebView sûres | — | aucune WebView |
| PLATFORM-3 : interface sûre | 🛠️ | **Corrigé** : protection contre le **tapjacking** (`ObscuredTouchGuard`, `filterTouchesWhenObscured`) sur l'écran « Gérer les téléphones » (accepter un téléphone dans la partie) et sur la confirmation « Supprimer toutes mes données ». Caractères de contrôle et de mise en forme (inversion du sens de lecture…) filtrés à la saisie et à la réception |

## MASVS-CODE — qualité du code

| Contrôle | Statut | Constat et justification |
|---|---|---|
| CODE-1 : plateforme à jour | ✅ | targetSdk 37, minSdk 26 |
| CODE-2 : mécanisme de mise à jour | ✅ | Play Store ; protocole versionné (`PROTOCOL_VERSION = 2`) : un téléphone d'une autre version est refusé avec un message clair |
| CODE-3 : dépendances sans vulnérabilité connue | 🟡 | Dependabot (alertes et correctifs) actif sur le dépôt ; contrôle « aucun traceur » en CI. **À faire** : osv-scanner, SBOM CycloneDX, vérification des dépendances Gradle (`verification-metadata.xml`) |
| CODE-4 : entrées non fiables validées | ✅ | Messages du protocole : 64 Ko, JSON décodé avec un schéma fermé, champs texte bornés ; lien d'invitation : adresses IPv4 privées seulement, longueurs exactes de l'empreinte et du jeton ; fichier de sauvegarde : 20 Mo, champs bornés, journaux rejoués, restauration en ajout seul |

## MASVS-RESILIENCE — résistance à l'analyse et à la modification

| Contrôle | Statut | Constat et justification |
|---|---|---|
| RESILIENCE-1 à 4 | ⚠️ | Hors niveau L1. R8 (minification) activé en release, `debuggable=false`. **Pas de Play Integrity** ni de détection de root : l'intégrité des scores repose sur l'autorité (hôte, futur serveur) qui revalide chaque événement ; un hôte modifié ne peut tricher que dans sa propre partie (`THREAT_MODEL.md` N5) |

## MASVS-PRIVACY — vie privée

| Contrôle | Statut | Constat et justification |
|---|---|---|
| PRIVACY-1 : minimisation de l'accès aux données | ✅ | Aucune localisation (`neverForLocation`), pas de contacts ; permissions par liste blanche vérifiée en CI (`checkReleasePermissions`) ; caméra et « appareils à proximité » demandées au moment de l'usage |
| PRIVACY-2 : pas d'identification de l'utilisateur | ✅ | Ni compte, ni identifiant publicitaire, ni identifiant d'appareil transmis ; en partie à côté, seuls le pseudo, l'emblème et les événements de jeu circulent |
| PRIVACY-3 : transparence | ✅ | Encart « Vie privée » d'À propos, politique de confidentialité sur le site, aucune donnée collectée déclarée dans la Play Console |
| PRIVACY-4 : contrôle par l'utilisateur | ✅ | Export (sauvegarde JSON), restauration, suppression de toutes les données depuis Réglages ; aucun traceur (contrôle Exodus en CI : 0) |

## Suites à donner

1. Chaîne d'approvisionnement (CODE-3) : osv-scanner et SBOM CycloneDX en CI, vérification des dépendances Gradle.
2. Test d'intrusion léger par le porteur du projet, en particulier sur la partie à côté (scénarios N1–N11 du modèle de
   menaces).
3. Analyse statique MobSF sur l'AAB de release avant la production.
