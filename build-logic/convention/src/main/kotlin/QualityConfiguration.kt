import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** ktlint + detekt, appliqués par tous les convention plugins. */
internal fun Project.configureQuality() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        version.set(libs.version("ktlint"))
        android.set(true)
        filter { exclude { it.file.path.contains("/build/") } }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = false
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }

    // detekt 1.23 embarque son propre compilateur Kotlin (2.0.21) : on l'empêche d'être aligné
    // sur la version du projet, ce qui casserait son exécution.
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
        }
    }
}
