plugins {
    `java-library`
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(libs.shadow)
    implementation(libs.kotlin)
}
