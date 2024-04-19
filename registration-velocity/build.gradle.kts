plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":registration-shared"))
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}