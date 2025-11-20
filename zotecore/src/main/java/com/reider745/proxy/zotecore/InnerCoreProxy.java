package com.reider745.proxy.zotecore;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.reider745.InnerCoreServer;
import com.reider745.api.ReflectHelper;
import com.reider745.proxy.data.ModInfo;
import com.reider745.proxy.network.InnerCoreProxyServer;
import com.reider745.proxy.network.Side;
import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.impl.ConnectPlayerPacket;
import com.reider745.proxy.packet.impl.RequestPacket;
import com.reider745.proxy.packet.impl.ResponsePacket;
import com.zhekasmirnov.apparatus.mcpe.NativeNetworking;
import com.zhekasmirnov.apparatus.modloader.ApparatusMod;
import com.zhekasmirnov.apparatus.multiplayer.Network;
import com.zhekasmirnov.apparatus.multiplayer.NetworkConfig;
import com.zhekasmirnov.apparatus.multiplayer.channel.data.NativeDataChannel;
import com.zhekasmirnov.apparatus.multiplayer.mod.IdConversionMap;
import com.zhekasmirnov.apparatus.multiplayer.mod.MultiplayerModList;
import com.zhekasmirnov.apparatus.multiplayer.server.ConnectedClient;
import com.zhekasmirnov.innercore.api.biomes.CustomBiome;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InnerCoreProxy extends PluginBase {
    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        try {
            final Config config = this.getConfig();
            final InnerCoreProxyServer server = new InnerCoreProxyServer(
                    Side.SERVER,
                    InetAddress.getByName(config.exists("ip") ? Server.getInstance().getIp() : config.getString("ip")),
                    config.getInt("port")
            );

            server.setServerName(Server.getInstance().getName());
            server.setToken(config.getString("token"));

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
                if(client.getChannelInterface().getChannel() instanceof NativeDataChannel channel) {
                    final String username = channel.getClient();
                    final Long uid = connectedPlayers.remove(username);

                    if(uid != null) {
                        client.playerUid = uid;

                        ConnectedClient.OnStateChangedListener originalStateChanged = ReflectHelper.getField(client, "stateChangedListener");
                        client.setStateChangedListener(((client1, newState) -> {
                            if(newState == ConnectedClient.ClientState.INITIALIZING) {
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

            server.addHandler(((connection, packet) -> {
                if(packet instanceof RequestPacket) {
                    connection.sendPacket(response);
                } else if(packet instanceof ConnectPlayerPacket connectPlayerPacket) {
                    connectedPlayers.put(connectPlayerPacket.getUsername(), connectPlayerPacket.getUid());
                }
            }));
            new Thread(() -> {
                try {
                    server.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
