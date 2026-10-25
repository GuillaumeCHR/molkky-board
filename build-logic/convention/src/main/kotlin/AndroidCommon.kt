import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

/** Réglages communs aux modules Android (application et bibliothèques). */
internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.all { test ->
            // Robolectric (SDK 36) sur JDK 21 accède à jdk.internal.access.
            test.jvmArgs(
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
            )
            test.maxHeapSize = "1536m"
        }
    }
    configureKotlin()
    configureQuality()
}
