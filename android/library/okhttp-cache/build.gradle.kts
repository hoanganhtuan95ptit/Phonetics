plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.okhttp.cache"
}

dependencies {
    implementation(libs.bundles.koin)
    implementation(libs.bundles.jackson)
    api(libs.bundles.retrofit)

    implementation(libs.ha.base)

    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    implementation(libs.androidx.startup)
}
