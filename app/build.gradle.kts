plugins {
    alias(libs.plugins.molkky.android.application)
    alias(libs.plugins.molkky.android.compose)
    alias(libs.plugins.molkky.hilt)
}

android {
    namespace = "fr.molkky.app"

    defaultConfig {
        applicationId = "fr.molkky.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.scoring)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.timber)
}
