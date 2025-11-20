package com.reider745.proxy.service;

import com.reider745.proxy.event.IBuilderPacket;
import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.packet.PacketType;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.util.function.Function;

public interface PacketRegistriesService {
    void registerPacket(PacketType id, IBuilderPacket packet);

    @Nullable
    IBuilderPacket getBuilderPacket(@Nullable PacketType id);
}
