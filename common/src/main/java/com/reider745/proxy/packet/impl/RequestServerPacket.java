package com.reider745.proxy.packet.impl;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestServerPacket extends Packet {
    public RequestServerPacket() {
        super(PacketType.RequestServer);
    }

    public RequestServerPacket(DataInputStream dis) {
        this();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {}
}
