plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.simple.feature.reminder"
}

dependencies {
    implementation(project(":app"))

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.permissionx)

    implementation(libs.google.material)

    implementation(libs.bundles.room)
    implementation("androidx.test:monitor:1.8.0")
    kapt(libs.room.compiler)

    implementation(libs.ha.adapter)
    kapt(libs.ha.adapter.processor)
    implementation(libs.ha.deeplink)
    kapt(libs.ha.deeplink.processor)
    implementation(libs.ha.autobind)
    kapt(libs.ha.autobind.processor)
}
