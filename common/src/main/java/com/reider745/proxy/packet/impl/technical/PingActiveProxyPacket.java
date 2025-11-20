package com.reider745.proxy.packet.impl.technical;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PingActiveProxyPacket extends Packet {
    public PingActiveProxyPacket(DataInputStream in) {
        super(PacketType.PingActiveProxy);
    }

    public PingActiveProxyPacket() {
        super(PacketType.PingActiveProxy);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {}
}
