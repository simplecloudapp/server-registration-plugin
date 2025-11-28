dependencies {
    implementation(libs.bundles.configurate) {
//        exclude(group = "org.jetbrains.kotlin")
//        exclude(group = "org.jetbrains.kotlinx")
    }

    api(libs.simplecloud.api)
    implementation(libs.kotlin.coroutines)
}
