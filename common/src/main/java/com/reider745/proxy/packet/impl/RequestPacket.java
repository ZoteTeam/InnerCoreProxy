package com.reider745.proxy.packet.impl;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestPacket extends Packet {
    public RequestPacket(DataInputStream in) {
        super(PacketType.Request);
    }

    public RequestPacket() {
        super(PacketType.Request);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {}
}
