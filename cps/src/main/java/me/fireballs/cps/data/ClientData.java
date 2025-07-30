package me.fireballs.cps.data;

import java.util.LinkedList;
import java.util.Queue;

public class ClientData {
    private final Queue<Integer> clicks = new LinkedList<>();

    private int tick;
    private int lastSent;

    public void handleClick() {
        clicks.add(tick);
    }

    public void handleTick() {
        int threshold = tick++ - 20;
        while (!clicks.isEmpty() && clicks.peek() < threshold) clicks.poll();
    }

    public int getLastSent() {
        return lastSent;
    }

    public void setLastSent(int lastSent) {
        this.lastSent = lastSent;
    }

    public int getCPS() {
        return clicks.size();
    }
}