pluginManagement {
    repositories {
        google()
        @Suppress("DEPRECATION")
        jcenter()
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        @Suppress("DEPRECATION")
        jcenter()
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EPhonetics"

include(":app")

include(":feature:mlkit")
include(":feature:thanks")
include(":feature:campaign")
include(":feature:reminder")
include(":feature:subscription")
include(":feature:ipa_voice_en")
include(":feature:pronunciation_assessment")

include(":library:ipa")
include(":library:word")
include(":library:phonetic")
include(":library:okhttp-cache")

val localPropertiesFile = File(settings.rootDir, "local.properties")
if (localPropertiesFile.exists()) {
    val properties = java.util.Properties()
    localPropertiesFile.inputStream().use { properties.load(it) }

    properties.forEach { key, path ->
        val keyStr = key.toString()
        if (keyStr.startsWith("module.")) {
            val moduleName = keyStr.substring("module.".length)
            val modulePath = File(path.toString())

            if (modulePath.exists()) {
                include(":$moduleName")
                project(":$moduleName").projectDir = modulePath
            }
        }
    }
}

includeBuild("/Users/hoanganhtuan/AndroidStudioProjects/LayoutNode") {
    dependencySubstitution {
        substitute(module("com.github.hoanganhtuan95ptit.core:glide-loader")).using(project(":glide-loader"))
        substitute(module("com.github.hoanganhtuan95ptit.core:node-engine")).using(project(":node-engine"))
    }
}
