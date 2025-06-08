package me.fireballs.share.data;

import java.util.LinkedList;
import java.util.Queue;

public class ClientData {
    private final Queue<Integer> clicks = new LinkedList<>();

    private int tick;
    private int lastDispatch;

    public void handleClick() {
        clicks.add(tick);
    }

    public void handleTick() {
        int threshold = tick++ - 20;
        while (!clicks.isEmpty() && clicks.peek() < threshold) clicks.poll();
    }

    public int getLastDispatch() {
        return lastDispatch;
    }

    public void setLastDispatch(int lastDispatch) {
        this.lastDispatch = lastDispatch;
    }

    public int getCPS() {
        return clicks.size();
    }
}