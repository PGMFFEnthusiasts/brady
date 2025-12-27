package me.fireballs.share.effects;

import org.bukkit.potion.PotionEffectType;

public interface PersistentEffectRegistrationSubscriber {
    default void onEffectAdd(PotionEffectApplication potionEffectApplication) {}
    default void onEffectRemove(PotionEffectType potionEffectType) {}
}
