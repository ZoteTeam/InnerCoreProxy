package com.reider745.proxy.event;

import com.reider745.proxy.network.Connection;
import com.reider745.proxy.packet.Packet;

public interface IHandlePacketReceive {
    void handle(Connection connection, Packet packet);
}
