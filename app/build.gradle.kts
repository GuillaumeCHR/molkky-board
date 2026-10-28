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
            throw GradleException("$forbidden est déclarée dans le manifeste fusionné : l'engagement « aucune requête réseau » serait faux.")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val check =
            tasks.register<CheckNoInternetPermission>("check${variant.name.replaceFirstChar { it.uppercase() }}NoInternet") {
                manifest.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST))
            }
        tasks.named("check") { dependsOn(check) }
    }
}
