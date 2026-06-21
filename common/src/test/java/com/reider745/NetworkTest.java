package com.reider745;

import com.reider745.proxy.packet.impl.ResponsePacket;
import com.reider745.proxy.service.impl.PacketDecoderServiceImpl;
import com.reider745.proxy.service.impl.PacketEncoderServiceImpl;
import com.reider745.proxy.service.impl.RegisterPacketImplService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class NetworkTest {
    @Test
    public void testResponsePacket() {
        final ResponsePacket packet = new ResponsePacket();
        packet.setIp("test");

        final PacketDecoderServiceImpl decoderService = new PacketDecoderServiceImpl(new RegisterPacketImplService());
        final PacketEncoderServiceImpl encoderService = new PacketEncoderServiceImpl();

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(out);

            encoderService.encode(dos, packet);

            byte[] test = out.toByteArray();

            dos.close();
            out.close();

            System.out.println(test.length);

            final ByteArrayInputStream ios = new ByteArrayInputStream(test);
            final DataInputStream dis = new DataInputStream(ios);

            System.out.println(decoderService.decode(dis));

            dis.close();
            ios.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
