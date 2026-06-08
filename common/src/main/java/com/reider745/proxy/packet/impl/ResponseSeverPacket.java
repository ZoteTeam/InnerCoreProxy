package com.reider745.proxy.packet.impl;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Getter
public class ResponseSeverPacket extends Packet {
    private final String serverId;

    public ResponseSeverPacket(DataInputStream dis) throws IOException {
        super(PacketType.Response);

        this.serverId = dis.readUTF();
    }

    public ResponseSeverPacket(String serverId) {
        super(PacketType.Response);

        this.serverId = serverId;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(serverId);
    }
}
