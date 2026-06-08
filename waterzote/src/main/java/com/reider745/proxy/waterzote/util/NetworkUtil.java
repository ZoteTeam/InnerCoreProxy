package com.reider745.proxy.waterzote.util;

import com.google.gson.Gson;
import org.cloudburstmc.protocol.bedrock.codec.v422.packet.InnerCorePacket;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.nio.charset.StandardCharsets;

public class NetworkUtil {
    private static final Gson GSON = new Gson();

    public static InnerCorePacket buildPacket(String name, Object json) {
        final InnerCorePacket packet = new InnerCorePacket();
        packet.setFormatId(json instanceof CharSequence ? 2 : 1);
        packet.setName(name);

        final byte[] data = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        packet.setBytesLength(data.length);
        packet.setBytes(data);

        return packet;
    }

    public static InnerCorePacket buildPacket(String name, JSONObject json) {
        final InnerCorePacket packet = new InnerCorePacket();
        packet.setFormatId(1);
        packet.setName(name);

        final byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
        packet.setBytesLength(data.length);
        packet.setBytes(data);

        return packet;
    }
}
