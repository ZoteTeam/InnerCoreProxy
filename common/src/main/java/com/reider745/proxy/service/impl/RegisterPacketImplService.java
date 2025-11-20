package com.reider745.proxy.service.impl;

import com.reider745.proxy.event.IBuilderPacket;
import com.reider745.proxy.packet.PacketType;
import com.reider745.proxy.packet.impl.ConnectPlayerPacket;
import com.reider745.proxy.packet.impl.ResponsePacket;
import com.reider745.proxy.packet.impl.technical.AuthServerPacket;
import com.reider745.proxy.packet.impl.technical.PingActiveProxyPacket;
import com.reider745.proxy.packet.impl.technical.PongActiveProxyPacket;
import com.reider745.proxy.packet.impl.RequestPacket;
import com.reider745.proxy.service.PacketRegistriesService;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

public class RegisterPacketImplService implements PacketRegistriesService {
    private final Byte2ObjectMap<IBuilderPacket> packets = new Byte2ObjectOpenHashMap<>();

    @SneakyThrows
    public RegisterPacketImplService(){
        this.registerPacket(PacketType.Request, RequestPacket::new);
        this.registerPacket(PacketType.Response, ResponsePacket::new);
        this.registerPacket(PacketType.Connect, ConnectPlayerPacket::new);

        this.registerPacket(PacketType.AuthServer, AuthServerPacket::new);
        this.registerPacket(PacketType.PingActiveProxy, PingActiveProxyPacket::new);
        this.registerPacket(PacketType.PongActiveProxy, PongActiveProxyPacket::new);
    }

    @Override
    public void registerPacket(PacketType type, IBuilderPacket packet) {
        packets.put((byte) type.ordinal(), packet);
    }

    @Override
    public @Nullable IBuilderPacket getBuilderPacket(@Nullable PacketType id) {
        if(id == null) return null;
        return packets.get((byte) id.ordinal());
    }
}
