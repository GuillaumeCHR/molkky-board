import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.molkkyboard.android.application)
    alias(libs.plugins.molkkyboard.android.compose)
    alias(libs.plugins.molkkyboard.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.yogistudio.molkkyboard"

    defaultConfig {
        applicationId = "com.yogistudio.molkkyboard"
        versionCode = 10
        versionName = "0.8.2"
    }

    // Clé d'envoi Play Store (upload key) : jamais dans le dépôt ni dans la CI. Ses coordonnées sont lues dans un
    // fichier local (voir docs/RELEASE.md) ; sans lui, la release est construite sans signature.
    val uploadKey =
        providers
            .environmentVariable("MOLKKY_UPLOAD_KEY_PROPERTIES")
            .orElse(
                providers.provider { "${System.getProperty("user.home")}/.config/molkkyboard/upload-key.properties" },
            ).map { file(it) }
            .get()
    if (uploadKey.isFile) {
        val props = Properties().apply { uploadKey.inputStream().use { load(it) } }
        signingConfigs.create("upload") {
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // « Envoyer un retour » : versions de test seulement.
            buildConfigField("boolean", "FEEDBACK", "true")
        }
        release {
            // Activé pour le test fermé : ./gradlew -Pmolkky.feedback=true :app:bundleRelease
            val feedback = providers.gradleProperty("molkky.feedback").orNull == "true"
            buildConfigField("boolean", "FEEDBACK", feedback.toString())
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

// Licences des bibliothèques, collectées au build sans aucun accès réseau (rien n'est téléchargé).
aboutLibraries {
    offlineMode = true
}

dependencies {
    implementation(projects.core.scoring)
    implementation(projects.core.designsystem)
    implementation(projects.feature.intro)
    implementation(projects.feature.setup)
    implementation(projects.feature.game)
    implementation(projects.feature.history)
    implementation(projects.feature.stats)
    implementation(projects.feature.share)
    implementation(projects.feature.nearby)
    implementation(projects.core.nearby)
    implementation(projects.core.data)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.aboutlibraries.compose.m3)

    testImplementation(libs.junit4)
}

/**
 * Permissions de l'APK : une liste blanche, chacune justifiée (fiche Play Store, `THREAT_MODEL.md`). Toute autre
 * permission, y compris apportée par une bibliothèque (localisation, contacts, identifiant publicitaire…), fait échouer
 * le build. INTERNET ne sert qu'aux sockets de la partie à côté, sur le réseau local.
 */
abstract class CheckPermissions : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun check() {
        val allowed =
            setOf(
                // Partie à côté : sockets locales, Wi-Fi, point d'accès (sans localisation), réseau local (Android 17).
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.CHANGE_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.CHANGE_WIFI_STATE",
                "android.permission.NEARBY_WIFI_DEVICES",
                "android.permission.ACCESS_LOCAL_NETWORK",
                // Hôte joignable écran éteint ; notification de partie en cours.
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE",
                "android.permission.POST_NOTIFICATIONS",
                // Scan du QR code d'invitation, photo de joueur.
                "android.permission.CAMERA",
                // WorkManager (AndroidX).
                "android.permission.WAKE_LOCK",
            )
        val declared =
            Regex("<uses-permission[^>]*?android:name=\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
                .findAll(manifest.get().asFile.readText())
                .map { it.groupValues[1] }
                .filterNot { it.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }
                .toSet()
        val unexpected = declared - allowed
        if (unexpected.isNotEmpty()) throw GradleException("Permissions non prévues : $unexpected")
        if ("neverForLocation" !in manifest.get().asFile.readText()) {
            throw GradleException("NEARBY_WIFI_DEVICES doit être déclarée avec neverForLocation.")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = "check${variant.name.replaceFirstChar { it.uppercase() }}Permissions"
        val check =
            tasks.register<CheckPermissions>(name) {
                manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            }
        tasks.named("check") { dependsOn(check) }
    }
}
