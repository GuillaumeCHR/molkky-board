plugins {
    alias(libs.plugins.molkkyboard.android.application)
    alias(libs.plugins.molkkyboard.android.compose)
    alias(libs.plugins.molkkyboard.hilt)
}

android {
    namespace = "com.yogistudio.molkkyboard"

    defaultConfig {
        applicationId = "com.yogistudio.molkkyboard"
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
    implementation(projects.core.designsystem)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.timber)
}
