plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.feature.pronunciation_assessment"
}

dependencies {
    implementation(project(":app"))
    implementation(project(":library:phonetic"))

    implementation(libs.google.material)

    implementation(libs.permissionx)
    implementation(libs.onnxruntime)

    implementation("com.github.hoanganhtuan95ptit.core:glide-loader")

    implementation(libs.ha.service)

    implementation(libs.ha.adapter)
    kapt(libs.ha.adapter.processor)
    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)
}
