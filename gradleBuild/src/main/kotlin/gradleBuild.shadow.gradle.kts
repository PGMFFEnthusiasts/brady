import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("gradleBuild.java")
    id("com.gradleup.shadow")
    `maven-publish`
}

publishing.publications {
    register<MavenPublication>("Release") {
        from(components["java"])
    }
}

tasks {
    jar {
        archiveClassifier.set("dev")
    }

    withType<ShadowJar> {
        archiveClassifier.set("")

        relocate("com.github.retrooper", "me.fireballs.packetevents.api")
        relocate("io.github.retrooper", "me.fireballs.packetevents.impl")
        relocate("de.tr7zw.changeme.nbtapi", "me.fireballs.nbtapi")
        relocate("io.nats.client", "me.fireballs.nats.client")

        exclude("META-INF/INDEX.LIST", "META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        exclude("META-INF/LICENSE*", "META-INF/NOTICE*")
        exclude("META-INF/proguard/**", "META-INF/DEPENDENCIES")
        exclude("DebugProbesKt.bin")
    }

    assemble {
        dependsOn(shadowJar)
    }
}
