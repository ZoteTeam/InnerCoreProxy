package com.reider745.proxy.network;

import com.reider745.proxy.event.IHandlePacketReceive;
import com.reider745.proxy.event.IHandlerConnectionServer;
import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.impl.technical.AuthServerPacket;
import com.reider745.proxy.packet.impl.technical.PingActiveProxyPacket;
import com.reider745.proxy.packet.impl.technical.PongActiveProxyPacket;
import com.reider745.proxy.service.PacketDecoderService;
import com.reider745.proxy.service.PacketEncoderService;
import com.reider745.proxy.service.PacketRegistriesService;
import com.reider745.proxy.service.impl.PacketDecoderServiceImpl;
import com.reider745.proxy.service.impl.PacketEncoderServiceImpl;
import com.reider745.proxy.service.impl.RegisterPacketImplService;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
public class InnerCoreProxyServer implements Cloneable {
    private final PacketRegistriesService registries;
    private final PacketDecoderService decoder;
    private final PacketEncoderService encoder;

    private final List<IHandlerConnectionServer> handlerConnectionServers = new ArrayList<>();
    private final List<IHandlePacketReceive> handlers = new CopyOnWriteArrayList<>();

    private final List<Connection> connections = new ArrayList<>();
    private final Side side;
    private final InetAddress address;
    private final int port;
    private boolean running;


    @Setter
    private long timeout = 1000;
    @Setter
    private long periodReconnect = 400;
    @Setter
    private String token = "";
    @Setter
    private String serverName = "non";
    private long periodPing = 50;
    private Consumer<String> log;

    public InnerCoreProxyServer(Side side, InetAddress address, int port, Consumer<String> log) {
        this.registries = new RegisterPacketImplService();
        this.decoder = new PacketDecoderServiceImpl(registries);
        this.encoder = new PacketEncoderServiceImpl();

        this.side = side;
        this.address = address;
        this.port = port;

        this.log = log;

        this.addHandler((connection, packet) -> {
            if (packet instanceof PongActiveProxyPacket) {
                connection.setLastTimePong(System.currentTimeMillis());
            }

            if (packet instanceof PingActiveProxyPacket) {
                connection.sendPacket(new PongActiveProxyPacket());
            }
        });
    }

    private void clearConnections() {
        synchronized (connections) {
            for (Connection connection : connections) {
                connection.setActive(false);
            }
            this.connections.clear();
        }
    }

    public void start() throws IOException {
        stop();

        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(periodPing);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                synchronized (connections) {
                    var it = connections.iterator();
                    while (it.hasNext()) {
                        final var connection = it.next();

                        try {
                            if (System.currentTimeMillis() - connection.getLastTimePong() <= timeout && connection.isActive() && running) {
                                connection.sendPacket(new PingActiveProxyPacket());
                            } else {
                                connection.setActive(false);
                                it.remove();
                            }
                        } catch (Exception ignored) {
                            connection.setActive(false);
                            it.remove();
                        }
                    }
                }
            }
        }).start();

        if(side == Side.SERVER) {
            running = true;

            final ServerSocket server = new ServerSocket(port, 50, address);

            while(running) {
                try {
                    Socket socket = server.accept();

                    if(running && socket != null) {
                        Connection connection = new Connection(this, socket);

                        final Packet g = connection.readPacket();

                        if(g instanceof AuthServerPacket packet) {
                            if(this.token.equals(packet.getToken())) {
                                log.accept("New Connection");
                                connection.setServerName(packet.getServerName());

                                this.addConnection(connection);
                                connection.start();
                            } else {
                                log.accept("Blocked connection invalid token");
                            }
                        } else {
                            log.accept("Blocked connection first packet not AuthServerPacket");
                            connection.setActive(false);
                        }
                    }

                } catch (IOException | InterruptedException ignored) {}
            }

            server.close();
        } else if(side == Side.HANDLED) {
            running = true;

            try {
                Connection connection;
                while (running) {
                    try {
                        final Socket socket = new Socket(address, port);
                        connection = new Connection(this, socket);

                        log.accept("Try connected.");

                        connection.sendPacket(new AuthServerPacket(serverName, token));
                        connection.start();

                        this.addConnection(connection);

                        while (connection.isActive()) {
                            Thread.yield();
                        }

                        this.clearConnections();
                    } catch (IOException ignored) {}
                    try {
                        Thread.sleep(periodReconnect);
                    } catch (Exception ignored) {}
                }
            } catch (Throwable e) {
                running = false;
                throw e;
            }
        } else {
            throw new IllegalStateException("Side is not handled");
        }
    }

    private void addConnection(Connection connection) {
        synchronized (connections) {
            for(IHandlerConnectionServer handlerConnectionServer : handlerConnectionServers) {
                handlerConnectionServer.onConnect(connection);
            }
            connections.add(connection);
        }
    }

    public void stop() {
        this.clearConnections();
        this.running = false;
    }

    public void addHandler(IHandlePacketReceive handler) {
        handlers.add(handler);
    }

    public void addHandler(IHandlerConnectionServer handler) {
        handlerConnectionServers.add(handler);
    }

    public void setPeriodPing(long periodPing) {
        this.periodPing = periodPing;
        this.timeout = Math.min(periodPing * 2, timeout);
    }

    @Nullable
    public Connection getServerConnection() {
        if(this.side == Side.HANDLED && !this.connections.isEmpty()) {
            synchronized (connections) {
                return this.connections.get(0);
            }
        }
        return null;
    }

    @Override
    public InnerCoreProxyServer clone() throws CloneNotSupportedException {
        var copy = new InnerCoreProxyServer(side, address, port, log);
        copy.setToken(this.token);
        return copy;
    }
}
