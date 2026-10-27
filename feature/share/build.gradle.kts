plugins {
    alias(libs.plugins.molkkyboard.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.yogistudio.molkkyboard.feature.share"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.zxing.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)
    testImplementation(projects.core.database)

    // Encodage vidéo : vrai encodeur matériel, testé sur émulateur ou téléphone.
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
