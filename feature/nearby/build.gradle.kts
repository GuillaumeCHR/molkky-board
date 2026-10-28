plugins {
    alias(libs.plugins.molkkyboard.android.feature)
}

android {
    namespace = "com.yogistudio.molkkyboard.feature.nearby"
}

dependencies {
    implementation(projects.core.nearby)
    implementation(libs.androidx.activity.compose)
    // QR code : génération (zxing core) et lecture (CameraX + zxing-cpp, sur le téléphone, sans réseau ni ML Kit).
    implementation(libs.zxing.core)
    implementation(libs.zxing.cpp)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}
