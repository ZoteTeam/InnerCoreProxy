package com.reider745.proxy.service;

import com.reider745.proxy.packet.Packet;

import java.io.DataOutputStream;
import java.io.IOException;

public interface PacketEncoderService {
    void encode(DataOutputStream out, Packet packet) throws IOException;
}
