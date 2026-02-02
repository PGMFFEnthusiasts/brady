plugins {
    id("gradleBuild.shadow")
    kotlin("kapt") version "2.2.0"
}

// thank the tank
java.sourceCompatibility = JavaVersion.VERSION_21

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.kotlinx.coroutines.core)

    api(libs.mccoroutine.velocity.core)
    api(libs.mccoroutine.velocity.api)

    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)

    api(libs.nats)
    api(libs.jda)
    api(libs.jda.ktx)
    api(libs.logback)
}
