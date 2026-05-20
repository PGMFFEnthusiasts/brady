package me.fireballs.brady.tools.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.Component;

public class WrapperPlayServerUpdateSign extends PacketWrapper<WrapperPlayServerUpdateSign> {
    private Vector3i blockPosition;
    private Component[] textLines;

    public WrapperPlayServerUpdateSign(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerUpdateSign(Vector3i blockPosition, Component[] textLines) {
        super(PacketType.Play.Server.UPDATE_SIGN);
        this.blockPosition = blockPosition;
        this.textLines = textLines;
    }

    @Override
    public void read() {
        blockPosition = readBlockPosition();
        textLines = new Component[4];
        for (int i = 0; i < 4; i++) {
            textLines[i] = readComponent();
        }
    }

    @Override
    public void write() {
        writeBlockPosition(blockPosition);
        for (int i = 0; i < 4; i++) {
            writeComponent(textLines[i]);
        }
    }

    @Override
    public void copy(WrapperPlayServerUpdateSign wrapper) {
        blockPosition = wrapper.blockPosition;
        textLines = wrapper.textLines.clone();
    }

    public Vector3i getBlockPosition() {
        return blockPosition;
    }

    public void setBlockPosition(Vector3i blockPosition) {
        this.blockPosition = blockPosition;
    }

    public Component[] getTextLines() {
        return textLines;
    }

    public void setTextLines(Component[] textLines) {
        this.textLines = textLines;
    }
}