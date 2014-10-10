package io.github.gelx_.wifiaccess.net;

import java.nio.ByteBuffer;
import io.github.gelx_.wifiaccess.net.packets.*;

/**
 * Created by Falk on 08.10.2014.
 */
public class Protocol {

    public static ByteBuffer packPacket(Packet packet){
        ByteBuffer buffer = ByteBuffer.allocateDirect(6 + packet.getData().length);
        buffer.putShort(packet.getID());
        buffer.putInt(packet.getData().length);
        buffer.put(packet.getData());
        buffer.flip();
        return buffer;
    }

    public static Packet unpackPacket(short id, ByteBuffer data){
        switch (id){
            case 1: return new RegisterUserPacket(data.array());
            case 2: return new GetUserPacket(data.array());
            case 3: return new GetUsersPacket();
            default: throw new IllegalArgumentException("That packetID is not meant to be received!");
        }
    }
}
