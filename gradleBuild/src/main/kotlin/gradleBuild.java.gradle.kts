plugins {
    kotlin("jvm")
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.pgm.fyi/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
}

group = "me.fireballs.brady"
version = "1.0"

java.sourceCompatibility = JavaVersion.VERSION_23

kotlin {
    jvmToolchain(23)
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
