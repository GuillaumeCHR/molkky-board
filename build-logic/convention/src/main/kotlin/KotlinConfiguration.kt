import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal val JAVA_VERSION = JavaVersion.VERSION_17

/** Cible JVM 17, compilée avec le JDK installé (21) : aucune toolchain à télécharger. */
internal fun Project.configureKotlin() {
    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    tasks.withType(KotlinCompilationTask::class.java).configureEach {
        val options = compilerOptions
        if (options is KotlinJvmCompilerOptions) {
            options.jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
