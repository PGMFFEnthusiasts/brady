package me.fireballs.share.effects;

import org.bukkit.potion.PotionEffectType;

public record PotionEffectApplication(PotionEffectType effectType, int potency) { }