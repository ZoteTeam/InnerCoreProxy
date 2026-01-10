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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

@Getter
@Setter
public class InnerCoreSession {
    private static final Gson GSON = new Gson();

    private static final Map<String, BiFunction<InnerCoreSession, String, CompletableFuture<Boolean>>> INITS = new HashMap<>();

    public static void register(String name, BiFunction<InnerCoreSession, String, CompletableFuture<Boolean>> action) {
        INITS.put(name, action);
    }

    private final Map<String, Function<String, CompletableFuture<Boolean>>> initializations = new ConcurrentHashMap<>();
    private final BedrockSession session;
    private long lastPing = System.currentTimeMillis();

    public InnerCoreSession(BedrockSession session) {
        this.session = session;

        var info = InnerCoreProxy.getInstance().getServerInfo();

        initializations.put("system.inner_core_build", (data) -> {
            final JsonObject json = GSON.fromJson(data, JsonObject.class);

            if (!info.getVersionName().equals("any") && json.get("versionCode").getAsInt() != info.getVersionCode()) {
                throw new RuntimeException("not support inner core version, please install: " + info.getPackName() + " " + info.getVersionName());
            }

            return CompletableFuture.completedFuture(true);
        });

        initializations.put("system.mod_list", (data) -> {
            final JsonArray mods = GSON.fromJson(data, JsonObject.class).get("list").getAsJsonArray();

            if (mods.size() != info.getMods().size()) {
                throw new RuntimeException("mod list not match " + mods);
            }

            for (JsonElement modElement : mods) {
                final JsonObject mod = modElement.getAsJsonObject();

                if (!InnerCoreProxy.getInstance().hasMod(mod.get("name").getAsString(), mod.get("version").getAsString())) {
                    throw new RuntimeException("invalid mod: " + mod.get("name").getAsString() + " " + mod.get("version").getAsString());
                }
            }

            return CompletableFuture.completedFuture(true);
        });

        INITS.forEach((name, consumer) -> {
            initializations.put(name, (data) -> consumer.apply(this, data));
        });
    }

    public void onReceive(InnerCorePacket packet) {
        if (packet.getName().equals("system.native_ping")) {
            lastPing = System.currentTimeMillis();
            this.session.sendPacketImmediately(InnerCoreProxy.getInstance().getPong());
            return;
        }

        if (packet.getName().equals("system.request_info")) {
            try {
                session.sendPacketImmediately(InnerCoreProxy.getInstance().getServerResponse());
                session.sendPacketImmediately(InnerCoreProxy.getInstance().getIdMap());
                session.sendPacketImmediately(InnerCoreProxy.getInstance().getClientAwaitingInit());
            } catch (Throwable e) {
                this.disconnect("not found server information");
            }
            return;
        }

        Function<String, CompletableFuture<Boolean>> func = this.initializations.get(packet.getName());
        if (func != null) {
            try {
                func.apply(new String(packet.getBytes(), StandardCharsets.UTF_8)).thenAccept(result -> {
                    if (result) {
                        try {
                            this.initializations.remove(packet.getName());

                            if (this.initializations.isEmpty()) {
                                this.session.sendPacketImmediately(InnerCoreProxy.getInstance().getClientConnectionAllowed());
                            }
                        } catch (Throwable e) {
                            this.disconnect(e.getMessage());
                        }
                    }
                });
            } catch (Throwable e) {
                this.disconnect(e.getMessage());
            }
        } else {
            this.disconnect("not found initialization packet: " + packet.getName());
        }
    }

    public void onConnectedServer(ProxiedPlayer player, ServerInfo info) {
        if(!initializations.isEmpty()) {
            player.disconnect("initializations failed " + initializations.keySet());
            return;
        }
        InnerCoreProxy.getInstance().getServers().get(info.getServerName()).getServerConnection().sendPacket(new ConnectPlayerPacket(player.getName()));
    }

    public void disconnect(String reason) {
        InnerCoreProxy.getInstance().getProxy().getLogger().info("Disconnect: " + reason);
        this.session.sendPacketImmediately(NetworkUtil.buildPacket("system.client_disconnect", reason));
        this.session.disconnect(reason);
    }
}
