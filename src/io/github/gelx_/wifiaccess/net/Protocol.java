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

    public final class RespUser extends Packet{
        private DB_users user;
        private byte[] data;

        public RespUser(DB_users user){
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

    public final class RespUsers extends Packet{

        private DB_users[] users;
        private byte[] data;

        public RespUsers(DB_users[] users){
            this.users = users;
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            for(DB_users user : users){
                buffer.put(user.toBytes());
            }
            byte[] compacted = new byte[buffer.position()];
            buffer.flip();
            buffer.get(compacted);
            this.data = compacted;
        }

        public short getID(){
            return 5;
        }
        public byte[] getData(){
            return data;
        }
    }

    public static ByteBuffer packPacket(Packet packet){
        ByteBuffer buffer = ByteBuffer.allocateDirect(6 + packet.getData().length);
        buffer.putShort(packet.getID());
        buffer.putInt(packet.getData().length);
        buffer.put(packet.getData());
        buffer.flip();
        return buffer;
    }
}
