plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.feature.subscription"
}

dependencies {
    implementation(project(":app"))
    implementation(project(":library:okhttp-cache"))

    implementation(libs.ha.adapter)
    kapt(libs.ha.adapter.processor)
    implementation(libs.ha.deeplink)
    kapt(libs.ha.deeplink.processor)
    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)

    implementation(libs.billing.ktx)
    implementation("com.github.hoanganhtuan95ptit.core:glide-loader")
}
