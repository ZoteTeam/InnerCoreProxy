package com.reider745.proxy.service.impl;

import com.reider745.proxy.packet.Packet;
import com.reider745.proxy.service.PacketEncoderService;

import java.io.DataOutputStream;
import java.io.IOException;

public class PacketEncoderServiceImpl implements PacketEncoderService {
    @Override
    public void encode(DataOutputStream out, Packet packet) throws IOException {
        out.writeByte(packet.getType().ordinal());
        packet.write(out);
    }
}
