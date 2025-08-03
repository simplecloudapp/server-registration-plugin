import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "0.0.2"

    repositories {
        mavenCentral()
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://libraries.minecraft.net")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
    }


    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        dependsOn("processResources")
        dependencies {
            include(project(":registration-shared"))
        }
        archiveFileName.set("${project.name}.jar")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        expand("version" to project.version,
            "name" to project.name)
    }
}
