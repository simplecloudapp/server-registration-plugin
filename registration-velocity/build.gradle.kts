
dependencies {
    api(project(":registration-shared"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly(libs.simplecloud.event.velocity)
}

tasks.named("compileKotlin") {
    dependsOn("generateBuildConfig")
}

tasks.register("generateBuildConfig") {
    val outputDir = project.layout.buildDirectory.dir("generated/source/buildConfig").get().asFile
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()
        val buildConfigFile = outputDir.resolve("BuildConstants.kt")
        buildConfigFile.writeText("""
            object BuildConstants {
                const val MODULE_NAME = "${project.name}"
                const val VERSION = "${rootProject.version}"
            }
        """.trimIndent())
    }
}