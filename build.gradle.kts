import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven {
            url = uri("https://libraries.minecraft.net")
        }
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlinTest)
        implementation(rootProject.libs.kotlinJvm)
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")

        val externalRelocatePath = "app.simplecloud.external"
        relocate("android", "${externalRelocatePath}.android")
        relocate("google", "${externalRelocatePath}.google")
        relocate("io", "${externalRelocatePath}.io")
        relocate("org", "${externalRelocatePath}.org")
        relocate("build", "${externalRelocatePath}.build")
        relocate("src", "${externalRelocatePath}.src")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        expand("version" to project.version,
            "name" to project.name)
    }
}

tasks.processResources {
    expand("version" to project.version,
        "name" to project.name)
}

tasks.test {
    useJUnitPlatform()
}