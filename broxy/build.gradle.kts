plugins {
    id("gradleBuild.shadow")
    kotlin("kapt") version "2.2.0"
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
