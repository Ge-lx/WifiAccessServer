package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.database.DB_users;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by Falk on 08.10.2014.
 */
public class Protocol {

    public abstract class Packet{
        protected byte[] data;

        public Packet(byte[] data){
            this.data = data; //TODO: data after constructor so updateData() is possible
        }

        public abstract short getID();
        public byte[] getData(){ return data;}
    }

    public final class RegisterUserPacket extends Packet{
        private DB_users user;
        public RegisterUserPacket(DB_users user){
            super(user.toBytes());
            this.user = user;
        }
        public DB_users getUser() {
            return user;
        }
        public short getID(){
            return 1;
        }
    }


    /*public static final short REGISTERUSER = 1,
                              GETUSER = 2,
                              GETUSERS = 3,
                              RESPUSER = 4,
                              RESPUSERS = 5;*/


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
