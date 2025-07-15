package me.fireballs.brady.core

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder

fun createOrNull(url: String?): WebhookClient? {
    url ?: return null
    if (!WebhookClientBuilder.WEBHOOK_PATTERN.matcher(url).matches()) return null
    return WebhookClientBuilder(url).build()
}
