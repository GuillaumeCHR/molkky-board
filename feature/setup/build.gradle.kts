plugins {
    alias(libs.plugins.molkkyboard.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.yogistudio.molkkyboard.feature.setup"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    testImplementation(projects.core.database)
}
