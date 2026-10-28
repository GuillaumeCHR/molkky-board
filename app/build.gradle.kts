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
        versionCode = 6
        versionName = "0.6.0"
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
            buildConfigField("boolean", "FEEDBACK", "false")
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
 * Engagement « aucune requête réseau » sur un seul téléphone : la permission INTERNET ne doit pas figurer dans le
 * manifeste fusionné, y compris par une bibliothèque. Quand la partie à côté l'ajoutera (jalon S2), ce contrôle
 * cédera la place au test « aucune adresse hors du réseau local ».
 */
abstract class CheckNoInternetPermission : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifest: RegularFileProperty

    @TaskAction
    fun check() {
        val forbidden = "android.permission.INTERNET"
        if (forbidden in manifest.get().asFile.readText()) {
            throw GradleException(
                "$forbidden est déclarée dans le manifeste fusionné : « aucune requête réseau » serait faux.",
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = "check${variant.name.replaceFirstChar { it.uppercase() }}NoInternet"
        val check =
            tasks.register<CheckNoInternetPermission>(name) {
                manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            }
        tasks.named("check") { dependsOn(check) }
    }
}
