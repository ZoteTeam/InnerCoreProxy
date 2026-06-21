package com.reider745.proxy.waterzote;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.reider745.proxy.data.ModInfo;
import com.reider745.proxy.network.NatsHelper;
import com.reider745.proxy.network.ServerList;
import com.reider745.proxy.packet.impl.RequestPacket;
import com.reider745.proxy.packet.impl.ResponsePacket;
import com.reider745.proxy.waterzote.json.SeverDetectionJson;
import com.reider745.proxy.waterzote.listener.SessionHandler;
import com.reider745.proxy.waterzote.util.NetworkUtil;
import dev.waterdog.waterdogpe.event.defaults.*;
import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.utils.config.Configuration;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.codec.v422.packet.InnerCorePacket;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class InnerCoreProxy extends Plugin {
    private static final Gson GSON = new Gson();
    @Getter
    private static InnerCoreProxy instance;

    private final InnerCorePacket pong = NetworkUtil.buildPacket("system.native_pong", "");
    private final InnerCorePacket clientAwaitingInit = NetworkUtil.buildPacket("system.client_awaiting_init", "");
    private final InnerCorePacket clientConnectionAllowed = NetworkUtil.buildPacket("system.client_connection_allowed", "");

    private InnerCorePacket serverResponse;
    private InnerCorePacket idMap;
    private List<ModInfo> mods;
    private String packName;
    private String versionName;
    private int version;
    private ServerList serverList;

    private NatsHelper natsHelper;

    public boolean hasMod(String name, String version) {
        if (serverResponse == null) {
            return false;
        }

        for (ModInfo mod : mods) {
            if (mod.name().equals(name) && mod.version().equals(version)) {
                return true;
            }
        }
        return false;
    }

    public InnerCoreProxy() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.saveResource("config.yml");
        this.loadConfig();

        {
            final Configuration config = this.getConfig();

            this.packName = config.getString("pack.name");
            this.versionName = config.getString("pack.version-name");
            this.version = config.getInt("pack.version-code");

            this.natsHelper = new NatsHelper(config.getString("nats.url"), config.getString("nats.token"));
        }

        try {
            final Map<String, Integer> biomes = new HashMap<>();

            new JsonParser().parse(Files.readString(new File(this.getDataFolder(), "biomes.json").toPath())).getAsJsonObject().entrySet().forEach(entry -> {
               biomes.put(entry.getKey(), entry.getValue().getAsInt());
            });

            this.mods = GSON.fromJson(Files.readString(new File(this.getDataFolder(), "mods.json").toPath()), new TypeToken<ArrayList<ModInfo>>() {}.getType());

            this.serverResponse = NetworkUtil.buildPacket("system.server_detection", new SeverDetectionJson(true, 0, biomes, false));
            this.idMap = NetworkUtil.buildPacket("system.id_map", new JsonParser().parse(Files.readString(new File(this.getDataFolder(), "id_map.json").toPath())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final SessionHandler handler = new SessionHandler();

        this.getProxy().getEventManager().subscribe(ServerConnectedEvent.class, handler::onConnectedServer);
        this.getProxy().getEventManager().subscribe(InnerCoreReceivePacketEvent.class, handler::onReceivePacket);
        this.getProxy().getEventManager().subscribe(PlayerDisconnectedEvent.class, handler::onQuit);


        this.serverList = new ServerList(natsHelper);
        this.getProxy().getScheduler().scheduleRepeating(serverList::onUpdate, 60, true);
    }

}
