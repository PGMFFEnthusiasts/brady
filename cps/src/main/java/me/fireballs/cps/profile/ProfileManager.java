package me.fireballs.cps.profile;

import org.jctools.maps.NonBlockingHashMapLong;

import java.util.Map;

public class ProfileManager {

    private final Map<Long, Profile> profiles = new NonBlockingHashMapLong<>();

    public Profile getProfile(long entityId) {
        return profiles.get(entityId);
    }

    public void addProfile(long entityId) {
        profiles.put(entityId, new Profile());
    }

    public void removeProfile(long entityId) {
        Profile profile = profiles.remove(entityId);
        if (profile == null) return;
        profile.destroy();
    }
}
