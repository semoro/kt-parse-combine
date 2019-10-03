pluginManagement {
    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
        gradlePluginPortal()
    }
}

rootProject.name = "kt-parse-combine"

include(":runtime", ":transform", ":benchmark")