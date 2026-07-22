plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.feature.ipa_voice_en"
}

dependencies {
    implementation(project(":app"))
    implementation(project(":library:ipa"))

    implementation(libs.ha.service)

    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)
}
