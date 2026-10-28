import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Module d'écran (`feature:*`) : bibliothèque Android + Compose + Hilt, branchée sur le design system et la couche
 * de données. Les features ne dépendent jamais les unes des autres.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("molkkyboard.android.library")
        pluginManager.apply("molkkyboard.android.compose")
        pluginManager.apply("molkkyboard.hilt")
        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:data"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))

            add("testImplementation", libs.library("junit4"))
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("roborazzi"))
            add("testImplementation", libs.library("roborazzi-compose"))
            add("testImplementation", libs.library("roborazzi-accessibility-check"))
            add("testImplementation", libs.library("androidx-test-core"))
            add("testImplementation", libs.library("androidx-test-ext-junit"))
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("turbine"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
        }
    }
}
