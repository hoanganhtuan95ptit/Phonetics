plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.phonetic"
}

dependencies {
    implementation(libs.bundles.koin)
    implementation(libs.bundles.jackson)

    implementation(libs.ha.base)
    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)

    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    implementation(libs.androidx.startup)
}
