package com.reider745.proxy.waterzote.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.reider745.proxy.packet.impl.ConnectPlayerPacket;
import com.reider745.proxy.waterzote.InnerCoreProxy;
import com.reider745.proxy.waterzote.util.NetworkUtil;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.codec.v422.packet.InnerCorePacket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@Setter
public class InnerCoreSession {
    private static final Gson GSON = new Gson();

    private final Map<String, Consumer<String>> initializations = new HashMap<>();
    private final BedrockSession session;
    private long lastPing = System.currentTimeMillis();
    private long playerId;

    public InnerCoreSession(BedrockSession session) {
        this.session = session;

        var info = InnerCoreProxy.getInstance().getServerInfo();

        initializations.put("system.inner_core_build", (data) -> {
            final JsonObject json = GSON.fromJson(data, JsonObject.class);

            if(!info.getVersionName().equals("any") && json.get("versionCode").getAsInt() != info.getVersionCode()) {
                throw new RuntimeException("not support inner core version, please install: " + info.getPackName() + " " + info.getVersionName());
            }
        });

        initializations.put("system.mod_list", (data) -> {
            final JsonArray mods = GSON.fromJson(data, JsonObject.class).get("list").getAsJsonArray();

            if(mods.size() != info.getMods().size()) {
                throw new RuntimeException("mod list not match");
            }

            for(JsonElement modElement : mods) {
                final JsonObject mod = modElement.getAsJsonObject();

                if(!InnerCoreProxy.getInstance().hasMod(mod.get("name").getAsString(), mod.get("version").getAsString())) {
                    throw new RuntimeException("invalid mod: " + mod.get("name").getAsString() + " " + mod.get("version").getAsString());
                }
            }
        });

        initializations.put("system.player_entity", (data) -> {
            try {
                playerId = Long.parseLong(data);
            } catch (NumberFormatException ignored) {
                throw new RuntimeException("invalid player packet data: system.player_entity");
            }
        });
    }

    public void onReceive(InnerCorePacket packet) {
        if(packet.getName().equals("system.native_ping")) {
            lastPing = System.currentTimeMillis();
            this.session.sendPacketImmediately(InnerCoreProxy.getInstance().getPong());
            return;
        }

        if(packet.getName().equals("system.request_info")) {
            try {
                session.sendPacketImmediately(InnerCoreProxy.getInstance().getServerResponse());
                session.sendPacketImmediately(InnerCoreProxy.getInstance().getIdMap());
                session.sendPacketImmediately(InnerCoreProxy.getInstance().getClientAwaitingInit());
            } catch (Throwable e) {
                this.disconnect("not found server information");
            }
            return;
        }

        Consumer<String> func = this.initializations.remove(packet.getName());
        if(func != null) {
            try {
                func.accept(new String(packet.getBytes(), StandardCharsets.UTF_8));

                if (this.initializations.isEmpty()) {
                    this.session.sendPacketImmediately(InnerCoreProxy.getInstance().getClientConnectionAllowed());
                }
            } catch (Throwable e) {
                this.disconnect(e.getMessage());
            }
        } else {
            this.disconnect("not found initialization packet: " + packet.getName());
        }
    }

    public void onConnectedServer(ProxiedPlayer player, ServerInfo info) {
        InnerCoreProxy.getInstance().getServers().get(info.getServerName()).getServerConnection().sendPacket(new ConnectPlayerPacket(player.getName(), playerId));
    }

    public void disconnect(String reason) {
        InnerCoreProxy.getInstance().getProxy().getLogger().info("Disconnect: " + reason);
        this.session.sendPacketImmediately(NetworkUtil.buildPacket("system.client_disconnect", reason));
        this.session.disconnect(reason);
    }
}
