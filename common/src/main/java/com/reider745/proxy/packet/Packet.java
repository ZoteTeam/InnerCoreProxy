package com.reider745.proxy.packet;

import lombok.Getter;

import java.io.DataOutputStream;
import java.io.IOException;

@Getter
public abstract class Packet {
    private final PacketType type;

    public Packet(PacketType type) {
        this.type = type;
    }

    public abstract void write(DataOutputStream out) throws IOException;
}
