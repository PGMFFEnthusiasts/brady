import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("gradleBuild.java")
    id("com.gradleup.shadow")
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

        exclude("META-INF/INDEX.LIST", "META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        exclude("META-INF/LICENSE*", "META-INF/NOTICE*")
        exclude("META-INF/proguard/**", "META-INF/DEPENDENCIES")
        exclude("DebugProbesKt.bin")
    }

    assemble {
        dependsOn(shadowJar)
    }
}
