dependencies {
    api(libs.bundles.configurate) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    compileOnly(libs.simplecloud.controller)
    compileOnly(libs.kotlin.coroutines)
}
