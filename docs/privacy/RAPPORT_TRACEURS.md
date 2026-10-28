# Rapport de traceurs — Molkky Board 0.8.1 (build 9)

> **Résultat : 0 traceur.** Contrôle du 2026-10-28, avec les 428 signatures de code publiées par
> [Exodus Privacy](https://reports.exodus-privacy.eu.org/fr/trackers/) (liste récupérée le 2026-10-28).

Ce rapport n'est **pas** un rapport officiel d'Exodus Privacy : il reproduit leur méthode, sur le PC du développeur,
avant la publication. Le rapport officiel sera demandé dès que l'app sera sur le Play Store, et son lien remplacera
ou complétera celui-ci.

## Méthode

Exodus Privacy détecte un traceur quand un nom de classe de l'APK correspond à la signature de code d'un traceur
connu (par exemple `com.google.firebase.analytics.` ou `com.facebook.appevents.`). Le script
[`tools/trackers/check_trackers.py`](../../tools/trackers/check_trackers.py) fait la même chose : il lit la table des
chaînes de chaque fichier `classes*.dex` de l'APK et la compare à la copie datée de leur liste
([`exodus-trackers.json`](../../tools/trackers/exodus-trackers.json)).

- L'APK **debug** (non minifié) est le contrôle qui compte : les noms de classes des bibliothèques y sont intacts.
- L'APK **release** est minifié par R8 : il est contrôlé aussi, mais un traceur renommé n'y serait pas forcément vu.
- Un auto-test vérifie que la détection fonctionne (des classes Firebase Analytics et Facebook factices sont bien
  repérées).
- Le contrôle tourne dans la CI (GitHub Actions) à chaque push sur `main` et à chaque pull request : il échoue si un
  traceur apparaît.

Pour le reproduire :

```bash
./gradlew :app:assembleDebug
python3 -I tools/trackers/check_trackers.py --self-test
python3 -I tools/trackers/check_trackers.py app/build/outputs/apk/debug/app-debug.apk
```

## Résultats

| APK | Classes analysées | Traceurs |
|---|---|---|
| debug 0.8.1 (`com.yogistudio.molkkyboard.debug`) | 38 627 | **0** |
| release 0.8.1 (`com.yogistudio.molkkyboard`), minifié | 8 152 | **0** |
| auto-test de détection | — | OK |

## Permissions de la version release

La liste est une liste blanche vérifiée à chaque build (`:app:checkReleasePermissions`). Aucune localisation, aucun
contact, aucun identifiant publicitaire.

| Permission | Pourquoi |
|---|---|
| `INTERNET` | sockets de la partie à côté, sur le réseau local uniquement (aucun serveur) |
| `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE` | rattacher la partie au réseau Wi-Fi de l'hôte |
| `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | point d'accès local ou même Wi-Fi |
| `NEARBY_WIFI_DEVICES` (`neverForLocation`) | point d'accès local sur l'hôte (Android 13+), sans usage de localisation |
| `ACCESS_LOCAL_NETWORK` | accès au réseau local exigé à partir d'Android 17 |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | l'hôte reste joignable écran éteint |
| `CAMERA` | scan du QR code, photo de joueur facultative |
| `POST_NOTIFICATIONS` | notification de partie en cours |
| `WAKE_LOCK` | ajoutée par Media3 (encodage de la mini-vidéo de partage) |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | permission interne ajoutée par AndroidX, propre à l'app |

Les permissions demandées à l'écran (et seulement au moment de l'usage) sont la caméra, les notifications,
« Appareils à proximité » et « Réseau local ». Les autres sont des permissions normales, accordées à l'installation.
