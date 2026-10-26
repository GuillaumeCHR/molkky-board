plugins {
    alias(libs.plugins.molkkyboard.android.library)
    alias(libs.plugins.molkkyboard.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.yogistudio.molkkyboard.core.database"
    // Les schémas exportés servent aux tests de migration.
    sourceSets {
        getByName("test") { assets.directories.add("$projectDir/schemas") }
    }
}

room {
    // Schémas exportés et versionnés : base des migrations après la première publication.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
