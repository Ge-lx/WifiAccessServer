package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.database.DB_users;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Falk on 08.10.2014.
 */
public class Protocol {

    public abstract static class Packet{

        private SocketAddress address;

        public Packet(SocketAddress address){
            this.address = address;
        }

        public SocketAddress getAddress(){
            return address;
        }
        public abstract short getID();
        public abstract byte[] getData();
    }

    public static class RegisterUserPacket extends Packet{
        private DB_users user;
        private byte[] data;

        public RegisterUserPacket(SocketAddress address, byte[] data){
            super(address);
            this.data = data;
            this.user = DB_users.fromBytes(data);
        }

        public RegisterUserPacket(SocketAddress address, DB_users user){
            super(address);
            this.user = user;
            this.data = user.toBytes();
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

    public static class GetUserPacket extends Packet{
        private String name;
        private byte[] data;

        public GetUserPacket(SocketAddress addresses, byte[] data){
            super(addresses);
            this.data = data;
            this.name = new String(data, Charset.defaultCharset()).trim();
        }

        public GetUserPacket(SocketAddress address, String name){
            super(address);
            this.name = name;
            this.data = Charset.defaultCharset().encode(name).array();
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

    public static class GetUsersPacket extends Packet{

        public GetUsersPacket(SocketAddress address){
            super(address);
        }

        public short getID(){
           return 3;
        }
        public byte[] getData(){
           return new byte[0];
        }
    }

    public static class RespUserPacket extends Packet{
        private DB_users user;
        private byte[] data;

        public RespUserPacket(SocketAddress address, DB_users user){
            super(address);
            this.user = user;
            this.data = user.toBytes();
        }

        public RespUserPacket(SocketAddress address, byte[] data){
            super(address);
            this.data = data;
            this.user = DB_users.fromBytes(data);
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

    public static class RespUsersPacket extends Packet{

        private DB_users[] users;
        private byte[] data;

        public RespUsersPacket(SocketAddress address, DB_users[] users){
            super(address);
            this.users = users;
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            for(DB_users user : users){
                byte[] userData = user.toBytes();
                buffer.putInt(userData.length);
                buffer.put(userData);
            }
            byte[] compacted = new byte[buffer.position()];
            buffer.flip();
            buffer.get(compacted);
            this.data = compacted;
        }

        public RespUsersPacket(SocketAddress address, byte[] data){
            super(address);
            this.data = data;
            List<DB_users> usersList = new ArrayList<>();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while(buffer.remaining() > 0){
                int userLength = buffer.getInt();
                byte[] userData = new byte[userLength];
                buffer.get(userData);
                usersList.add(DB_users.fromBytes(userData));
            }
            this.users = usersList.toArray(new DB_users[usersList.size()]);
        }

        public DB_users[] getUsers(){
            return users;
        }
        public short getID(){
            return 5;
        }
        public byte[] getData(){
            return data;
        }
    }

    public static class DelUserPacket extends Packet{

        private String name;
        private byte[] data;

        public DelUserPacket(SocketAddress address, String name){
            super(address);
            this.name = name;
            this.data = Charset.defaultCharset().encode(name).array();
        }

        public DelUserPacket(SocketAddress address, byte[] data){
            super(address);
            this.data = data;
            this.name = new String(data, Charset.defaultCharset()).trim();
        }

        public String getName(){
            return name;
        }
        public short getID(){
            return 6;
        }
        public byte[] getData(){
            return data;
        }
    }

    public static ByteBuffer packPacket(Packet packet){
        ByteBuffer buffer = ByteBuffer.allocate(6 + packet.getData().length);
        buffer.putShort(packet.getID());
        buffer.putInt(packet.getData().length);
        buffer.put(packet.getData());
        buffer.flip();
        return buffer;
    }

    public static Packet unpackPacket(SocketAddress address, short id, ByteBuffer data){
        switch (id){
            case 1: return new RegisterUserPacket(address, data.array());
            case 2: return new GetUserPacket(address, data.array());
            case 3: return new GetUsersPacket(address);
            case 4: return new RespUserPacket(address, data.array());
            case 5: return new RespUsersPacket(address, data.array());
            case 6: return new DelUserPacket(address, data.array());
            default: throw new IllegalArgumentException("Received unknown PacketID: " + id);
        }
    }

}
