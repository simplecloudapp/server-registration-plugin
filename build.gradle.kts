import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotversion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin"
    version = if (commitHash != null) snapshotversion else baseVersion

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
        compileOnly(rootProject.libs.kotlin.jvm)
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
            exclude(dependency("app.simplecloud.controller:controller-api"))
            exclude(dependency("app.simplecloud.controller:controller-shared"))
            exclude(dependency("app.simplecloud:simplecloud-pubsub"))
        }

        archiveFileName.set("${project.name}.jar")

        relocate("com.google.protobuf", "app.simplecloud.relocate.google.protobuf")
        relocate("com.google.common", "app.simplecloud.relocate.google.common")
        relocate("io.grpc", "app.simplecloud.relocate.io.grpc")

        relocate("org.incendo", "app.simplecloud.plugin.registration.relocate.incendo")
        relocate("org.spongepowered", "app.simplecloud.plugin.registration.relocate.spongepowered")
        relocate("app.simplecloud.plugin.api", "app.simplecloud.plugin.registration.relocate.plugin.api")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.processResources {
        expand(
            "version" to project.version,
            "name" to project.name
        )
    }
}
