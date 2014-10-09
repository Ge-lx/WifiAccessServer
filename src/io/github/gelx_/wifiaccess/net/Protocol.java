package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.database.DB_users;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by Falk on 08.10.2014.
 */
public class Protocol {

    public abstract class Packet{
        public abstract short getID();
        public abstract byte[] getData();
    }

    public final class RegisterUserPacket extends Packet{
        private DB_users user;
        private byte[] data;

        public RegisterUserPacket(byte[] data){
            this.data = data;
            this.user = DB_users.fromBytes(data);
        }

        public DB_users getUser() {
            return user;
        }
        public short getID(){
            return 1;
        }
        public byte[] getData(){
            return data;
        }
    }

    public final class GetUserPacket extends Packet{
        private String name;
        private byte[] data;

        public GetUserPacket(byte[] data){
            this.data = data;
            this.name = new String(data, Charset.defaultCharset());
        }

        public String getName(){
            return name;
        }
        public short getID(){
            return 2;
        }
        public byte[] getData(){
            return data;
        }
    }

    public final class GetUsers extends Packet{
        public short getID(){
           return 3;
        }
        public byte[] getData(){
           return new byte[0];
        }
    }

    public final class RespUsers extends Packet{
        private DB_users user;
        private byte[] data;

        public RespUsers(DB_users user){
            this.user = user;
            this.data = user.toBytes();
        }

        public DB_users getUser(){
            return user;
        }
        public short getID(){
            return 4;
        }
        public byte[] getData(){
            return data;
        }
    }

    //RESPUSERS ID 5

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
