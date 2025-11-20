package com.reider745.proxy.waterzote;

import com.reider745.proxy.data.ModInfo;
import com.reider745.proxy.network.InnerCoreProxyServer;
import com.reider745.proxy.network.Side;
import com.reider745.proxy.packet.impl.RequestPacket;
import com.reider745.proxy.packet.impl.ResponsePacket;
import com.reider745.proxy.waterzote.json.SeverDetectionJson;
import com.reider745.proxy.waterzote.listener.SessionHandler;
import com.reider745.proxy.waterzote.util.NetworkUtil;
import dev.waterdog.waterdogpe.event.defaults.*;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoType;
import dev.waterdog.waterdogpe.plugin.Plugin;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.codec.v422.packet.InnerCorePacket;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Getter
public class InnerCoreProxy extends Plugin {
    @Getter
    private static InnerCoreProxy instance;

    private final InnerCorePacket pong = NetworkUtil.buildPacket("system.native_pong", "");
    private final InnerCorePacket clientAwaitingInit = NetworkUtil.buildPacket("system.client_awaiting_init", "");
    private final InnerCorePacket clientConnectionAllowed = NetworkUtil.buildPacket("system.client_connection_allowed", "");
    private final Map<String, InnerCoreProxyServer> servers = new HashMap<>();

    private InnerCorePacket serverResponse;
    private InnerCorePacket idMap;
    private ResponsePacket serverInfo;


    public boolean hasMod(String name, String version) {
        if (serverResponse == null) {
            return false;
        }

        for (ModInfo mod : serverInfo.getMods()) {
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
        {
            this.saveResource("config.yml");
            this.loadConfig();

            for (ServerInfo info : this.getProxy().getServers()) {
                if (info.getServerType() == ServerInfoType.BEDROCK) {
                    try {
                        final Map<String, Object> serverConfig = (Map<String, Object>) this.getConfig().get(info.getServerName());
                        InetAddress address = serverConfig.containsKey("ip") ? InetAddress.getByName(serverConfig.get("ip").toString()) : info.getAddress().getAddress();
                        InnerCoreProxyServer proxyServer = new InnerCoreProxyServer(
                                Side.HANDLED,
                                address,
                                (int) serverConfig.get("port")
                        );
                        proxyServer.setToken(serverConfig.get("token").toString());
                        this.servers.put(info.getServerName(), proxyServer);
                    } catch (Exception ignored) {
                        throw new RuntimeException("Failed to load config for " + info.getServerName());
                    }
                }
            }
        }

        // get information from server
        try {
            final InnerCoreProxyServer server = this.servers.get(this.servers.keySet().iterator().next()).clone();
            final SessionHandler handler = new SessionHandler();

            server.addHandler((connection, packet) -> {
                if (packet instanceof ResponsePacket response) {
                    this.serverInfo = response;

                    this.serverResponse = NetworkUtil.buildPacket("system.server_detection", new SeverDetectionJson(
                            response.isServer(),
                            response.getPortSever(),
                            response.getBiomes(),
                            response.isSocketEnabled()
                    ));
                    this.idMap = NetworkUtil.buildPacket("system.id_map", response.getIdMap());

                    server.stop();
                }
            });

            server.addHandler(connection -> {
                connection.sendPacket(new RequestPacket());
            });

            this.getProxy().getEventManager().subscribe(ServerConnectedEvent.class, handler::onConnectedServer);
            this.getProxy().getEventManager().subscribe(InnerCoreReceivePacketEvent.class, handler::onReceivePacket);
            this.getProxy().getEventManager().subscribe(PlayerDisconnectedEvent.class, handler::onQuit);

            this.getProxy().getLogger().info("Request server information, please turn on the server");

            server.start();
            server.getHandlerConnectionServers().clear();
            server.getHandlers().clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        servers.forEach((name, server) -> {
           new Thread(() -> {
               try {
                   getLogger().info("Starting server " + name);
                   server.start();
               } catch (Exception ignored) {
                   getLogger().error("Failed to start server " + name, ignored);
               }
               throw new RuntimeException("Failed to start server " + name);
           }).start();
        });
    }
}
