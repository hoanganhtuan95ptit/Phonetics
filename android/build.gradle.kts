plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
}

subprojects {
    afterEvaluate {
        val hasAndroidLibrary = plugins.hasPlugin("com.android.library")
        val hasDynamicFeature = plugins.hasPlugin("com.android.dynamic-feature")
        
        if (hasAndroidLibrary || hasDynamicFeature) {
            val android = extensions.findByName("android") as? com.android.build.gradle.BaseExtension
            android?.apply {
                val compileSdkVal = libs.versions.compileSdk.get().toInt()
                val minSdkVal = libs.versions.minSdk.get().toInt()
                val targetSdkVal = libs.versions.targetSdk.get().toInt()

                compileSdkVersion(compileSdkVal)

                defaultConfig {
                    minSdk = minSdkVal
                    targetSdk = targetSdkVal
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                (this as? org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions)?.apply {
                    jvmTarget = "17"
                }

                buildFeatures.viewBinding = true
                buildFeatures.buildConfig = true
            }
        }
    }
}
