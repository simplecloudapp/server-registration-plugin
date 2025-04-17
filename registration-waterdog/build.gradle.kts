
repositories {
    maven("https://repo.waterdog.dev/releases/")
    maven("https://repo.waterdog.dev/snapshots/")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}
dependencies {
    api(project(":registration-shared"))
    compileOnly("dev.waterdog.waterdogpe:waterdog:2.0.3-SNAPSHOT")
    compileOnly(libs.simplecloud.controller)
    compileOnly(libs.kotlin.coroutines)
}
