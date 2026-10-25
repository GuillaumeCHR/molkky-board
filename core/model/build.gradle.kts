plugins {
    alias(libs.plugins.molkky.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
