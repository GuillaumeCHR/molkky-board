import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** À appliquer après le plugin Android (application ou bibliothèque). */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true
        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            add("implementation", bom)
            add("implementation", libs.library("androidx-compose-ui"))
            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            add("testImplementation", bom)
        }
    }
}
