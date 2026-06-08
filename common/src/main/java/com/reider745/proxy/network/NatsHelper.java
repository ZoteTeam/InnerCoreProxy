package com.reider745.proxy.network;


import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.service.impl.PacketDecoderServiceImpl;
import com.reider745.proxy.service.impl.PacketEncoderServiceImpl;
import com.reider745.proxy.service.impl.RegisterPacketImplService;
import io.nats.client.*;

import java.io.*;
import java.util.function.Consumer;

public class NatsHelper implements AutoCloseable {
    private final PacketDecoderServiceImpl packetDecoderService;
    private final PacketEncoderServiceImpl packetEncoderService;
    private final Connection connection;

    public NatsHelper(String url, String token) {
        this.packetDecoderService = new PacketDecoderServiceImpl(new RegisterPacketImplService());
        this.packetEncoderService = new PacketEncoderServiceImpl();

        try {
            this.connection = Nats.connect(new Options.Builder()
                    .server(url)
                    .maxReconnects(-1)
                    .token(token.toCharArray())
                    .build());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void publishRaw(String subject, byte[] data) {
        try {
            connection.publish(subject, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publish(String subject, Packet packet) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(out);

            this.packetEncoderService.encode(dos, packet);
            this.publishRaw(subject, out.toByteArray());

            dos.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setHandlerRaw(String subject, Consumer<byte[]> handler) {
        Dispatcher dispatcher = this.connection.createDispatcher(m -> {
            handler.accept(m.getData());
        });
        dispatcher.subscribe(subject);
    }

    public void setHandler(String subject, Consumer<Packet> handler) {
        this.setHandlerRaw(subject, data -> {
            try {
                final ByteArrayInputStream ios = new ByteArrayInputStream(data);
                final DataInputStream dis = new DataInputStream(ios);

                handler.accept(this.packetDecoderService.decode(dis));

                dis.close();
                ios.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
