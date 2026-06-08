package com.reider745.proxy.zotecore;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.ConfigSection;
import com.reider745.InnerCoreServer;
import com.reider745.api.ReflectHelper;
import com.reider745.proxy.data.ModInfo;
import com.reider745.proxy.network.NatsHelper;
import com.reider745.proxy.packet.impl.ConnectPlayerPacket;
import com.reider745.proxy.packet.impl.ResponsePacket;
import com.reider745.proxy.zotecore.common.DumpServerInfoCommand;
import com.zhekasmirnov.apparatus.modloader.ApparatusMod;
import com.zhekasmirnov.apparatus.multiplayer.Network;
import com.zhekasmirnov.apparatus.multiplayer.channel.data.NativeDataChannel;
import com.zhekasmirnov.apparatus.multiplayer.mod.IdConversionMap;
import com.zhekasmirnov.apparatus.multiplayer.mod.MultiplayerModList;
import com.zhekasmirnov.apparatus.multiplayer.server.ConnectedClient;
import com.zhekasmirnov.innercore.api.biomes.CustomBiome;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InnerCoreProxy extends PluginBase {
    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        this.getServer().getCommandMap().register(getName(), new DumpServerInfoCommand());

        final ConfigSection nats = this.getConfig().getSection("nats");
        NatsHelper natsHelper = new NatsHelper(nats.getString("url"), nats.getString("token"));

        final ResponsePacket response = new ResponsePacket();

        // pack
        response.setPackName(InnerCoreServer.getName());
        response.setVersionName(InnerCoreServer.getVersionName());
        response.setVersionCode(InnerCoreServer.getVersionCode());

        //socket
        response.setSocketEnabled(InnerCoreServer.isSocketEnabled());
        response.setPortSever(Network.getSingleton().getServer().getConfig().getDefaultPort());

        CustomBiome.getAllCustomBiomes().forEach((name, biome) -> {
            response.addBiome(name, biome.id);
        });

        response.setIdMap(ReflectHelper.getField(IdConversionMap.getSingleton(), "localIdMap"));

        ReflectHelper.<List<ApparatusMod>>getField(MultiplayerModList.getSingleton(), "modList").forEach(mod -> {
            response.getMods().add(new ModInfo(mod.getInfo().getString("name"), mod.getInfo().getString("version")));
        });

        final Map<String, Long> connectedPlayers = new ConcurrentHashMap<>();

        Network.getSingleton().getServer().addOnClientConnectionRequestedListener((client) -> {
            if (client.getChannelInterface().getChannel() instanceof NativeDataChannel channel) {
                final String username = channel.getClient();
                final Long uid = connectedPlayers.remove(username);

                if (uid != null) {
                    ConnectedClient.OnStateChangedListener originalStateChanged = ReflectHelper.getField(client, "stateChangedListener");
                    client.setStateChangedListener(((client1, newState) -> {
                        if (newState == ConnectedClient.ClientState.INITIALIZING) {
                            Set<String> remainingInitializationPackets = ReflectHelper.getField(client, "remainingInitializationPackets");
                            remainingInitializationPackets.clear();
                        }

                        originalStateChanged.onStateChanged(client1, newState);
                    }));
                } else {
                    client.disconnect("network transfer error");
                }
            } else {
                client.disconnect("not support protocol connection");
            }
        });


        final String serverId = this.getConfig().getString("server-id", "");

        response.setServerId(serverId);

        if (serverId.isEmpty()) throw new RuntimeException("server-id is empty");

        natsHelper.setHandler("server.connection." + serverId, packet -> {
            if(packet instanceof ConnectPlayerPacket connectPlayerPacket) {
                this.getServer().getLogger().info("Connection for WaterZote - " + connectPlayerPacket.getUsername());
                connectedPlayers.put(connectPlayerPacket.getUsername(), 0L);
            }
        });

        natsHelper.setHandler("server.request." + serverId, packet -> {
            natsHelper.publish("server.response", response);
        });
    }
}
