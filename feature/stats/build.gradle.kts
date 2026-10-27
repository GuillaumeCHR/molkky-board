plugins {
    alias(libs.plugins.molkkyboard.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.yogistudio.molkkyboard.feature.stats"
}

dependencies {
    implementation(libs.vico.compose.m3)
    testImplementation(projects.core.database)
}
