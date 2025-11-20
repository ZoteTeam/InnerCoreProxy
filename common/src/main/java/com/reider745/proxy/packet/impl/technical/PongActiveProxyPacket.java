package com.reider745.proxy.packet.impl.technical;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PongActiveProxyPacket extends Packet {
    public PongActiveProxyPacket(DataInputStream in) {
        super(PacketType.PongActiveProxy);
    }

    public PongActiveProxyPacket() {
        super(PacketType.PongActiveProxy);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {}
}
