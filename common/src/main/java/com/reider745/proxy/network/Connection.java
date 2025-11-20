package com.reider745.proxy.network;

import com.reider745.proxy.event.IHandlePacketReceive;
import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.impl.technical.PingActiveProxyPacket;
import com.reider745.proxy.packet.impl.technical.PongActiveProxyPacket;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class Connection extends Thread {
    private final UUID uuid = UUID.randomUUID();

    private final InnerCoreProxyServer server;
    private final Socket socket;

    private final DataInputStream input;
    private final DataOutputStream output;

    private long lastTimePong = System.currentTimeMillis();
    private boolean active = true;
    private String serverName = "non auth";

    public Connection(InnerCoreProxyServer server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;

        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    @Nullable
    public Packet readPacket() throws InterruptedException, IOException {
        while(input.available() <= 0) {
            if(!active) {
                return null;
            }

            Thread.sleep(1L);
        }

        return server.getDecoder().decode(input);
    }

    @Override
    public void run() {
        while(active) {
            try {
                final var packet = readPacket();
                if(packet != null) {
                    handlePacket(packet);
                }
            } catch (IOException | InterruptedException ignored) {
                active = false;
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void handlePacket(Packet packet) {
        for(IHandlePacketReceive handler : server.getHandlers()) {
            handler.handle(this, packet);
        }
    }

    public synchronized void sendPacket(Packet packet) {
        try {
            server.getEncoder().encode(output, packet);
            output.flush();
        } catch (IOException e) {
            active = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}

