package com.reider745.proxy.event;

import com.reider745.proxy.packet.Packet;

import java.io.DataInputStream;
import java.io.IOException;

public interface IBuilderPacket {
    Packet build(DataInputStream is) throws IOException;
}
