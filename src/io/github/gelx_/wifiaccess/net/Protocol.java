package io.github.gelx_.wifiaccess.net;

import java.nio.ByteBuffer;

/**
 * Created by Falk on 08.10.2014.
 */
public class Protocol {

    public static final short REGISTERUSER = 1,
                              GETUSER = 2,
                              GETUSERS = 3,
                              RESPUSER = 4,
                              RESPUSERS = 5;


    public static ByteBuffer packPacket(short packetID, byte[] data){
        ByteBuffer buffer = ByteBuffer.allocateDirect(6 + data.length);
        buffer.putShort(packetID);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public static ByteBuffer packPacketNoData(short packetID){
        ByteBuffer buffer = ByteBuffer.allocateDirect(6);
        buffer.putShort(packetID);
        buffer.putInt(0);
        buffer.flip();
        return buffer;
    }
}
