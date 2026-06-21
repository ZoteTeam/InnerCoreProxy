package com.reider745.proxy.network;

import com.reider745.proxy.packet.impl.RequestPacket;
import com.reider745.proxy.packet.impl.RequestServerPacket;
import com.reider745.proxy.packet.impl.ResponsePacket;
import com.reider745.proxy.packet.impl.ResponseSeverPacket;
import lombok.Getter;

import java.util.*;

@Getter
public class ServerList {
    private final Set<String> servers = new HashSet<>();
    private final Map<String, ResponsePacket> serverListPackets = new HashMap<>();
    private final NatsHelper natsHelper;

    public ServerList(NatsHelper natsHelper) {
        this.natsHelper = natsHelper;

        natsHelper.setHandler("list.add", (packet) -> {
            if(packet instanceof ResponseSeverPacket response) {
                servers.add(response.getServerId());
            }
        });

        natsHelper.setHandler("server.response", (packet) -> {
            if(packet instanceof ResponsePacket response) {
                serverListPackets.put(response.getServerId(), response);
                servers.add(response.getServerId());
            }
        });

        natsHelper.publish("list.request", new RequestServerPacket());
    }

    public ServerList(String serverId, NatsHelper natsHelper) {
        this(natsHelper);

        final ResponseSeverPacket responseSeverPacket = new ResponseSeverPacket(serverId);

        natsHelper.publish("list.add", responseSeverPacket);

        natsHelper.setHandler("list.request", (packet) -> {
           natsHelper.publish("list.add", responseSeverPacket);
        });
    }

    public void onUpdate() {
        var servers = new ArrayList<>(this.servers);

        this.serverListPackets.clear();
        this.servers.clear();

        RequestPacket requestPacket = new RequestPacket();

        for(String serverId : servers) {
            natsHelper.publish("server.request." + serverId, requestPacket);
        }
    }
}
