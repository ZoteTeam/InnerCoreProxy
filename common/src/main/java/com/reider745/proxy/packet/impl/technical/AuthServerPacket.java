package com.reider745.proxy.packet.impl.technical;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Getter
public class AuthServerPacket extends Packet {
    private final String serverName;
    private final String token;

    public AuthServerPacket(DataInputStream in) throws IOException {
        super(PacketType.AuthServer);

        this.serverName = in.readUTF();
        this.token = in.readUTF();
    }

    public AuthServerPacket(String serverName, String token) {
        super(PacketType.AuthServer);

        this.serverName = serverName;
        this.token = token;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(serverName);
        out.writeUTF(token);
    }
}
