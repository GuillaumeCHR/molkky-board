plugins {
    alias(libs.plugins.molkkyboard.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.yogistudio.molkkyboard.feature.history"
}

dependencies {
    testImplementation(projects.core.database)
}
