plugins {
    id("gradleBuild.shadow")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core)
    api(libs.kotlinx.coroutines.core)

    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.platform.bukkit)
    compileOnlyApi(libs.adventure.text.serializer.plain)
    api(libs.adventure.text.serializer.ansi)

    compileOnlyApi(libs.luckperms.api)
    compileOnlyApi(libs.tc.oc.pgm.core)
    compileOnlyApi(libs.app.ashcon.sportpaper)

    api(libs.mccoroutine.bukkit.core)
    api(libs.mccoroutine.bukkit.api)
    api(libs.com.github.retrooper.packetevents.spigot)

    api(libs.nbtapi)

    api(libs.jda)
    api(libs.logback)
}
