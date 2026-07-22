plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.ipa"
}

dependencies {
    implementation(libs.bundles.koin)
    implementation(libs.bundles.jackson)

    implementation(libs.ha.base)

    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    implementation(libs.androidx.startup)
}
