# Molkky Board

Application Android de gestion de parties de Mölkky : saisie des lancers, calcul automatique des
scores, règles officielles et variantes, historique et Hall of Fame. Éditeur : Yogi Studio (`com.yogistudio.molkkyboard`).

- Règles : [`docs/RULES.md`](docs/RULES.md)
- Moteur de score : [`docs/SCORING_SPEC.md`](docs/SCORING_SPEC.md)
- Architecture et jalons : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## Prérequis

- JDK 21
- Android SDK avec la plateforme `android-37` (`$ANDROID_HOME` défini, ou `sdk.dir` dans
  `local.properties`)
- Gradle n'a pas besoin d'être installé : le wrapper (`./gradlew`) télécharge la bonne version.

## Construire

```bash
./gradlew :app:assembleDebug        # APK : app/build/outputs/apk/debug/app-debug.apk
./gradlew test                      # tests unitaires
./gradlew :core:scoring:koverHtmlReport   # couverture du moteur (core/scoring/build/reports/kover/html)
./gradlew ktlintCheck detekt        # lint (ktlintFormat corrige le style automatiquement)
```

## Lancer sur l'émulateur

```bash
android emulator start Medium_Phone_API_37.0
./gradlew :app:installDebug
adb shell am start -n com.yogistudio.molkkyboard.debug/com.yogistudio.molkkyboard.MainActivity
```

## Modules

| Module | Rôle |
|---|---|
| `build-logic/` | convention plugins Gradle (Android, Compose, Hilt, Kotlin pur, lint) |
| `app` | Activity unique, navigation, injection |
| `core:model` | types partagés (Kotlin pur) |
| `core:scoring` | moteur de score : rejeu d'un journal d'événements (Kotlin pur) |

Les versions de toutes les dépendances sont centralisées dans `gradle/libs.versions.toml`.
