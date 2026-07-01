package me.fireballs.brady.core.tag;

import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jctools.maps.NonBlockingHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TagTracker {

    private final List<TagLayer> layers;
    private final Map<ViewKey, TagView> views = new NonBlockingHashMap<>();

    private final Queue<Runnable> actions = new ConcurrentLinkedQueue<>();

    public TagTracker(List<TagLayer> layers) {
        this.layers = layers;
    }

    public void startTracking(User viewer, int targetId) {
        if (viewer.getEntityId() == targetId) return;
        ViewKey key = new ViewKey(viewer.getEntityId(), targetId);
        actions.add(() -> views.putIfAbsent(key, new TagView(viewer)));
    }

    public void stopTracking(int viewerId, int[] targetIds) {
        actions.add(() -> {
            for (int targetId : targetIds) {
                TagView view = views.remove(new ViewKey(viewerId, targetId));
                if (view != null) view.destroy();
            }
        });
    }

    public Optional<TagView> getView(int viewerId, int targetId) {
        return Optional.ofNullable(views.get(new ViewKey(viewerId, targetId)));
    }

    public void tick() {
        Runnable action;
        while ((action = actions.poll()) != null) {
            action.run();
        }

        Map<Integer, Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .collect(Collectors.toMap(Player::getEntityId, player -> player));

        Map<Integer, Map<String, String>> texts = new HashMap<>();

        views.entrySet().removeIf(entry -> {
            Player viewer = onlinePlayers.get(entry.getKey().viewerId());
            Player target = onlinePlayers.get(entry.getKey().targetId());

            if (viewer == null) return true;

            if (target == null || !target.isValid()) {
                entry.getValue().destroy();
                return true;
            }

            Map<String, String> targetTexts = texts.computeIfAbsent(target.getEntityId(), _ -> renderTexts(target));
            entry.getValue().update(target, viewer, layers, targetTexts);
            return false;
        });
    }

    private Map<String, String> renderTexts(Player target) {
        Map<String, String> texts = new HashMap<>();
        layers.forEach(l -> texts.put(l.id(), l.text().apply(target)));
        return texts;
    }

    public void cleanup(int entityId) {
        actions.add(() -> views.entrySet().removeIf(entry -> {
            ViewKey key = entry.getKey();

            if (key.targetId() == entityId) {
                entry.getValue().destroy();
                return true;
            }

            return key.viewerId() == entityId;
        }));
    }

    // todo: override hashcode and equals if map performance is poor, see https://github.com/jOOQ/jOOQ/issues/18935
    private record ViewKey(int viewerId, int targetId) {}
}
