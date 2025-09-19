plugins {
    id("gradleBuild.shadow")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core)

    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.platform.bukkit)
    compileOnlyApi(libs.adventure.text.serializer.plain)
    api(libs.adventure.text.serializer.ansi) {
        exclude("net.kyori", "adventure-api")
    }

    compileOnlyApi(libs.luckperms.api)
    compileOnlyApi(libs.tc.oc.pgm.core)
    compileOnlyApi(libs.app.ashcon.sportpaper)

    api(libs.mccoroutine.bukkit.core)
    api(libs.mccoroutine.bukkit.api)
    api(libs.com.github.retrooper.packetevents.spigot) {
        exclude("net.kyori", "adventure-api")
        exclude("net.kyori", "adventure-nbt")
        exclude("net.kyori", "adventure-key")
        exclude("net.kyori", "examination-api")
        exclude("net.kyori", "examination-string")
        exclude("net.kyori", "adventure-text-serializer-gson")
        exclude("net.kyori", "adventure-text-serializer-legacy")
        exclude("net.kyori", "adventure-text-serializer-json-legacy-impl")
    }

    api(libs.nbtapi)

    api(libs.jda)
    api(libs.jda.ktx)
    api(libs.discord.webhooks)
    api(libs.logback)

    api(libs.nats)
    api(libs.postgres)
}
