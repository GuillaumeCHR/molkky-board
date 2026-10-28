# Publier une version de Molkky Board

Procédure pour construire une version signée et l'envoyer sur le Play Store. La **clé d'envoi** (upload key) ne va
jamais dans le dépôt ni dans la CI : elle reste sur ton PC et dans tes sauvegardes.

## Principe : Play App Signing

- Google conserve la **clé de signature de l'application**, celle que voient les téléphones. Tu ne la manipules jamais.
- Tu gardes la **clé d'envoi**, qui ne sert qu'à prouver à Google que l'AAB vient de toi.
- Si la clé d'envoi est perdue ou volée, elle se **réinitialise** depuis la Play Console (Intégrité de l'application
  › Signature de l'application › Demander la réinitialisation de la clé d'importation). L'app publiée n'est pas
  touchée. Tant que tu n'en as pas, il est impossible d'envoyer une mise à jour : d'où la sauvegarde ci-dessous.

## 1. Créer la clé d'envoi (une seule fois)

```bash
tools/release/create-upload-key.sh
```

Le script crée, **hors du dépôt**, dans `~/.config/molkkyboard/` (droits 600, lisibles par toi seul) :

| Fichier | Contenu |
|---|---|
| `upload-key.jks` | la clé (RSA 4096, PKCS12, valable 30 ans, alias `upload`) |
| `upload-key.properties` | le chemin de la clé, l'alias et le mot de passe (généré au hasard, jamais affiché) |

Il affiche l'empreinte SHA-256 du certificat. Elle n'est pas secrète : note-la, elle permet de vérifier plus tard
que c'est bien cette clé que la Play Console attend. Le script refuse d'écraser une clé existante.

## 2. Sauvegarder la clé (tout de suite)

Les deux fichiers vont ensemble : la clé sans le mot de passe ne sert à rien, et l'inverse non plus.

1. **Gestionnaire de mots de passe** (Bitwarden, KeePassXC…) : une entrée « Molkky Board — clé d'envoi Play » avec
   `upload-key.jks` en pièce jointe et le contenu de `upload-key.properties` dans les notes.
2. **Copie hors ligne chiffrée** : par exemple une clé USB avec un conteneur VeraCrypt ou une archive chiffrée,
   rangée ailleurs que le PC.
3. Ne jamais l'envoyer par e-mail, ni la mettre dans un dépôt git, un cloud non chiffré ou la CI.

Pour vérifier une sauvegarde : restaure les deux fichiers dans un dossier temporaire et lance
`keytool -list -keystore upload-key.jks` avec le mot de passe. L'empreinte doit être celle notée à l'étape 1.

## 3. Numéro de version

Dans `app/build.gradle.kts` :

- `versionCode` : **+1 à chaque envoi** sur le Play Store, quel que soit le canal (test fermé ou production). Google
  refuse un numéro déjà utilisé.
- `versionName` : SemVer (`0.8.0`, puis `1.0.0` pour la première version publique).

Committe le changement de version, puis pose un tag : `git tag -a v1.0.0 -m "Molkky Board 1.0.0 (build N)"`.

## 4. Construire

```bash
./gradlew --stop
./gradlew --no-configuration-cache --max-workers=1 detekt ktlintCheck lintDebug test \
  :app:checkReleasePermissions
python3 -I tools/trackers/check_trackers.py app/build/outputs/apk/debug/app-debug.apk
./gradlew --no-configuration-cache --max-workers=1 :app:bundleRelease
```

- Le fichier d'envoi est `app/build/outputs/bundle/release/app-release.aab`.
- Avec `~/.config/molkkyboard/upload-key.properties`, l'AAB est signé avec la clé d'envoi. Sans ce fichier, la release
  se construit **sans signature** (utile pour vérifier R8, inutilisable sur le Play Store).
- Un autre emplacement du fichier de propriétés se donne par la variable `MOLKKY_UPLOAD_KEY_PROPERTIES`.
- Machine modeste : la minification R8 prend environ 5 minutes et beaucoup de mémoire ; ferme l'émulateur et le
  navigateur avant.

Vérifier la signature :

```bash
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep SHA256
```

## 5. Tester la release avant l'envoi

L'AAB ne s'installe pas directement. Pour tester exactement ce qui sera envoyé, construis l'APK release signé
(`./gradlew :app:assembleRelease`) et installe-le sur un téléphone :
`adb install -r app/build/outputs/apk/release/app-release.apk`. Ce build porte l'identifiant
`com.yogistudio.molkkyboard` (sans `.debug`) : il s'installe à côté de la version de debug, sans en reprendre les
données. Vérifie au minimum une partie complète, la partie à côté (même Wi-Fi et point d'accès), le partage et la
sauvegarde.

## 6. Envoyer

1. Play Console › Molkky Board › Tests › **Test fermé** (puis Production plus tard) › Créer une version.
2. Au premier envoi, accepte Play App Signing (proposé par défaut).
3. Envoie `app-release.aab`, ajoute les notes de version (français et anglais), enregistre, puis envoie pour examen.

## 7. Version de test sur GitHub (facultatif)

Les pré-versions GitHub (`v0.x`) contiennent l'APK de debug, sans clé :

```bash
gh release create v0.8.0 molkky-board-0.8.0-debug.apk --prerelease --title "Molkky Board 0.8.0 (build 8)" \
  --notes-file notes.md
```
