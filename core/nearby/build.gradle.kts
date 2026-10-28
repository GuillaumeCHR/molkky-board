// Partie à côté (phase 3 bis) : transport WebSocket sur TLS entre téléphones, point d'accès local, certificat de
// partie, service de premier plan, et gestionnaire des sessions (hôte ou invité) branché sur la passerelle.
plugins {
    alias(libs.plugins.molkkyboard.android.library)
    alias(libs.plugins.molkkyboard.hilt)
}

android {
    namespace = "com.yogistudio.molkkyboard.core.nearby"
}

dependencies {
    api(projects.core.sync)
    implementation(projects.core.data)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)
    implementation(libs.java.websocket)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    // Certificat logiciel pour les tests TLS sur la JVM (le Keystore Android n'y existe pas).
    testImplementation(libs.bouncycastle.pkix)
}
