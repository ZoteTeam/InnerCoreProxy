package com.reider745.proxy.packet.impl;

import com.reider745.proxy.data.ModInfo;
import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;
import lombok.Getter;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResponsePacket extends Packet {
    private String serverId = "";

    private boolean isServer = true;
    private int portSever = 19132;
    private boolean socketEnabled = false;

    private final Map<String, Integer> biomes = new HashMap<>();

    private String packName = "";
    private String versionName = "";
    private int versionCode = 0;

    private List<ModInfo> mods = new ArrayList<>();
    private Map<String, Integer> idMap = new HashMap<>();

    public ResponsePacket(DataInputStream is) throws IOException {
        super(PacketType.Response);

        socketEnabled = is.readBoolean();
        isServer = is.readBoolean();
        portSever = is.readInt();

        int size =  is.readInt();
        for (int i = 0; i < size; i++) {
            biomes.put(is.readUTF(), is.readInt());
        }

        packName = is.readUTF();
        versionName = is.readUTF();
        versionCode = is.readInt();

        size = is.readInt();
        for (int i = 0; i < size; i++) {
            mods.add(new ModInfo(is.readUTF(), is.readUTF()));
        }

        size = is.readInt();
        for (int i = 0; i < size; i++) {
            idMap.put(is.readUTF(), is.readInt());
        }
    }

    public ResponsePacket() {
        super(PacketType.Response);
    }

    public void addBiome(String biome, int id) {
        biomes.put(biome, id);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeBoolean(socketEnabled);
        out.writeBoolean(isServer);
        out.writeInt(portSever);

        out.writeInt(biomes.size());
        for (Map.Entry<String, Integer> entry : biomes.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue());
        }

        out.writeUTF(packName);
        out.writeUTF(versionName);
        out.writeInt(versionCode);

        out.writeInt(mods.size());
        for (ModInfo modInfo : mods) {
            out.writeUTF(modInfo.name());
            out.writeUTF(modInfo.version());
        }

        out.writeInt(idMap.size());
        for (Map.Entry<String, Integer> entry : idMap.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue());
        }
    }
}
