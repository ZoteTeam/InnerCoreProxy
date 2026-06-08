package com.reider745.proxy.network;

import com.reider745.proxy.packet.impl.RequestServerPacket;
import com.reider745.proxy.packet.impl.ResponseSeverPacket;

import java.util.HashSet;
import java.util.Set;

public class ServerList {
    private final Set<String> servers = new HashSet<>();

    public ServerList(NatsHelper natsHelper) {
        natsHelper.setHandler("list.add", (packet) -> {
            if(packet instanceof ResponseSeverPacket response) {
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

    public Set<String> getServers() {
        return servers;
    }
}
