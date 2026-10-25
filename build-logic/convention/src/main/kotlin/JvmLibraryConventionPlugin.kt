import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies

/** Module Kotlin pur (sans Android), testé avec JUnit Jupiter. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        configureKotlin()
        configureQuality()

        tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
        }
        dependencies {
            add("testImplementation", platform(libs.library("junit-bom")))
            add("testImplementation", libs.library("junit-jupiter"))
            add("testImplementation", libs.library("junit-jupiter-params"))
            add("testRuntimeOnly", libs.library("junit-platform-launcher"))
        }
    }
}
