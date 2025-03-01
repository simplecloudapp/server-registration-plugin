plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":registration-shared"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
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

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("ddD5Gif1")
    versionNumber.set(rootProject.version.toString())
    versionType.set("beta")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4"
    )
    loaders.add("velocity")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}