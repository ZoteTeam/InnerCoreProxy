package com.reider745.proxy.waterzote.listener;

import com.reider745.proxy.waterzote.InnerCoreProxy;
import com.reider745.proxy.waterzote.network.InnerCoreSession;
import dev.waterdog.waterdogpe.event.defaults.*;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.BedrockSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SessionHandler {
    private final Map<BedrockSession, InnerCoreSession> sessions = new ConcurrentHashMap<>();

    public void onReceivePacket(InnerCoreReceivePacketEvent event) {
        if(event.getPacket().getName().equals("system.client_disconnect")) {
            sessions.remove(event.getSession());
            event.getSession().disconnect();
            return;
        }

        sessions.computeIfAbsent(event.getSession(), InnerCoreSession::new).onReceive(event.getPacket());
    }


    public void onConnectedServer(ServerConnectedEvent event) {
        try {
            sessions.computeIfAbsent(event.getPlayer().getConnection(), InnerCoreSession::new).onConnectedServer(event.getPlayer(), event.getTargetServer());
        } catch (Exception e) {
            InnerCoreProxy.getInstance().getLogger().error("Failed to connect to " + event.getTargetServer().getServerName(), e);
            event.getPlayer().disconnect();
        }
    }

    public void onQuit(PlayerDisconnectedEvent event) {
        final ProxiedPlayer player = event.getPlayer();
        if(player != null) {
            sessions.remove(player.getConnection()).disconnect(event.getReason());
        }
    }
}
