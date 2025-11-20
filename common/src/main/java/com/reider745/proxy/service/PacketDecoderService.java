package com.reider745.proxy.service;

import com.reider745.proxy.packet.Packet;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public interface PacketDecoderService {
    @Nullable
    Packet decode(DataInputStream is) throws IOException;
}
