plugins {
    alias(libs.plugins.molkkyboard.android.library)
    alias(libs.plugins.molkkyboard.hilt)
}

android {
    namespace = "com.yogistudio.molkkyboard.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
