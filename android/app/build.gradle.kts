import java.util.Properties
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val configPath = localProperties.getProperty("module.config")
if (configPath != null) {
    val configFile = file("${configPath}/config.gradle")
    if (configFile.exists()) {
        apply(from = configFile)
    }
}

android {
    namespace = "com.simple.phonetics"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ipa.english.phonetics"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        versionCode = getGitVersionCode()
        versionName = "1.${SimpleDateFormat("yy.MM.dd").format(Date())}.${getGitVersionCode()}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        println("-------------------------------------------")
        println("🚀 Building EPhonetics")
        println("📌 Version Code: ${versionCode}")
        println("📌 Version Name: ${versionName}")
        println("-------------------------------------------")

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BRANCH", "\"${getGitBranchName()}\"")
    }

    signingConfigs {
        create("keystore") {
            keyAlias = if (project.hasProperty("keyAlias")) project.extra["keyAlias"] as String else ""
            keyPassword = if (project.hasProperty("keyPassword")) project.extra["keyPassword"] as String else ""
            val storeFilePath = if (project.hasProperty("storeFile")) project.extra["storeFile"] as String else null
            storeFile = storeFilePath?.let { file(it) }
            storePassword = if (project.hasProperty("storePassword")) project.extra["storePassword"] as String else ""
        }
    }

    buildTypes {
        getByName("debug") {
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "EPhoDebug")
            buildConfigField("boolean", "DEBUG", "true")
            signingConfig = signingConfigs.getByName("keystore")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("release") {
            resValue("string", "app_name", "EPhonetics")
            buildConfigField("boolean", "DEBUG", "false")
            signingConfig = signingConfigs.getByName("keystore")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    dynamicFeatures += listOf(
        ":feature:mlkit",
        ":feature:campaign",
        ":feature:ipa_voice_en",
        ":feature:thanks",
        ":feature:reminder",
        ":feature:subscription",
        ":feature:pronunciation_assessment"
    )

    configurations.all {
        resolutionStrategy {
            force("com.google.protobuf:protobuf-javalite:3.25.1")
            force("com.google.auto.service:auto-service:1.1.1")
            force("com.google.auto.service:auto-service-annotations:1.1.1")
        }
    }
}

dependencies {
    for (key in localProperties.stringPropertyNames()) {
        if (key.startsWith("module.")) {
            val moduleName = key.substring("module.".length)
            if (findProject(":$moduleName") != null) {
                implementation(project(":$moduleName"))
            }
        }
    }

    implementation(project(":library:ipa"))
    implementation(project(":library:word"))
    implementation(project(":library:phonetic"))
    implementation(project(":library:okhttp-cache"))

    api(libs.bundles.koin)
    api(libs.bundles.jackson)
    api(libs.bundles.androidx.core)
    api(libs.bundles.androidx.lifecycle)
    api(libs.androidx.core.splashscreen)
    api(libs.bundles.retrofit)

    api(libs.ha.base)
    api(libs.ha.android)
    api(libs.ha.size)
    api(libs.ha.task)
    api(libs.ha.event)
    api(libs.ha.state)
    api(libs.ha.image)
    api(libs.ha.theme)
    api(libs.ha.string)
    api(libs.ha.service)
    api(libs.ha.coroutines)
    api("com.github.hoanganhtuan95ptit.core:node-engine")

    api(libs.ha.analytics)
    debugImplementation(libs.ha.analytics.log)
    releaseImplementation(libs.ha.analytics.firebase)

    api(libs.ha.crashlytics)
    debugImplementation(libs.ha.crashlytics.log)
    releaseImplementation(libs.ha.crashlytics.firebase)

    implementation(libs.ha.detect2)
    implementation(libs.ha.translate2)
    implementation(libs.ha.adapter)
    kapt(libs.ha.adapter.processor)
    implementation(libs.ha.deeplink)
    kapt(libs.ha.deeplink.processor)
    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ads)
    implementation(libs.firebase.perf)
    releaseImplementation(libs.firebase.config)
    releaseImplementation(libs.firebase.analytics)
    releaseImplementation(libs.firebase.crashlytics)

    api(libs.bundles.glide)

    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    implementation(libs.bundles.play.services)

    implementation(libs.google.flexbox)
    implementation(libs.google.material)
    implementation(libs.auto.service.annotations)
    kapt(libs.auto.service.processor)

    implementation(libs.lottie)
    implementation(libs.permissionx)
    implementation(libs.keyboardvisibilityevent)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation("com.github.hoanganhtuan95ptit.core:glide-loader")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
}

fun getGitHash(): String {
    return runGitCommand(listOf("git", "rev-parse", "--short", "HEAD"), "dev") as String
}

fun getGitBranchName(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

fun getGitVersionCode(): Int {
    return runGitCommand(listOf("git", "rev-list", "--count", "HEAD"), 1) as Int
}

fun runGitCommand(command: List<String>, defaultValue: Any): Any {
    return try {
        val process = ProcessBuilder(command)
            .directory(rootProject.rootDir)
            .redirectErrorStream(true)
            .start()

        process.waitFor()

        if (process.exitValue() == 0) {
            val output = process.inputStream.bufferedReader().readText().trim()
            if (output.isNotEmpty()) {
                if (defaultValue is Int) output.toInt() else output
            } else defaultValue
        } else defaultValue
    } catch (e: Exception) {
        println("⚠️ Git command failed: ${e.message}")
        defaultValue
    }
}
