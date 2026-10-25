plugins {
    alias(libs.plugins.molkky.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)
}
