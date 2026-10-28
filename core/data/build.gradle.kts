plugins {
    alias(libs.plugins.molkkyboard.android.library)
    alias(libs.plugins.molkkyboard.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yogistudio.molkkyboard.core.data"
}

dependencies {
    api(projects.core.scoring)
    api(projects.core.sync)
    api(projects.core.stats)
    api(projects.core.datastore)
    implementation(projects.core.database)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
