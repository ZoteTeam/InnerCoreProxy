package com.reider745.proxy.packet.impl;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Getter
public class ConnectPlayerPacket extends Packet {
    private String username;

    public ConnectPlayerPacket(DataInputStream in) throws IOException {
        super(PacketType.Connect);

        this.username = in.readUTF();
    }

    public ConnectPlayerPacket(String username) {
        super(PacketType.Connect);

        this.username = username;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(username);
    }
}
