package com.reider745.proxy.service.impl;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;
import com.reider745.proxy.service.PacketDecoderService;
import com.reider745.proxy.service.PacketRegistriesService;

import java.io.DataInputStream;
import java.io.IOException;

public class PacketDecoderServiceImpl implements PacketDecoderService {
    private final PacketRegistriesService packetRegistriesService;

    public PacketDecoderServiceImpl(PacketRegistriesService packetRegistriesService) {
        this.packetRegistriesService = packetRegistriesService;
    }

    @Override
    public Packet decode(DataInputStream is) throws IOException {
        final byte id = is.readByte();

        if(id >= 0 && id < PacketType.values().length) {
            final var builder = packetRegistriesService.getBuilderPacket(PacketType.values()[id]);

            if(builder != null) {
                return builder.build(is);
            }
        }

        return null;
    }
}
