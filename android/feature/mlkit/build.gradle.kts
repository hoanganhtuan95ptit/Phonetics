plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.feature.mlkit"
}

dependencies {
    implementation(project(":app"))

    implementation(libs.bundles.mlkit)

    implementation(libs.ha.service)
    implementation(libs.ha.detect2.mlkit)
    implementation(libs.ha.translate2.mlkit)

    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)
}
