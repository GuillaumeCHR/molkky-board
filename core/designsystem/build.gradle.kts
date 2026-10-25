plugins {
    alias(libs.plugins.molkkyboard.android.library)
    alias(libs.plugins.molkkyboard.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.yogistudio.molkkyboard.core.designsystem"
}

roborazzi {
    // Captures de référence versionnées, relues à chaque validation visuelle.
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
