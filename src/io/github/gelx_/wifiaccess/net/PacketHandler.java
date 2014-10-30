package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.WifiAccess;
import io.github.gelx_.wifiaccess.database.DB_users;
import io.github.gelx_.wifiaccess.database.DatabaseManager;
import io.github.gelx_.wifiaccess.net.Protocol.*;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Falk on 08.10.2014.
 */
public class PacketHandler implements Runnable{

    private BlockingDeque<Packet> packetQueue = new LinkedBlockingDeque<>(50);
    private Thread thread;
    private ClientHandler client;

    private DatabaseManager databaseManager;

    public PacketHandler(ClientHandler client){
        databaseManager = client.getConnection().getDatabase();

        this.thread = new Thread(this);
        this.client = client;
        thread.start();
    }

    public void queuePacket(Packet packet){
        if(!packetQueue.offer(packet)){
            WifiAccess.LOGGER.severe("Could not handle packet: Queue overflow!");
        }
    }

    public void run(){
        while(!Thread.interrupted()){
            try {
                this.handlePacket(packetQueue.take());
                //For debug
                //this.handlePacketDebug(packetQueue.take());
            } catch (InterruptedException e) {
                WifiAccess.LOGGER.info("PacketHandler interrupted!");
            }
        }
    }

    public void stop(){
        this.thread.interrupt();
    }

    public void handlePacket(Packet packet){

        switch (packet.getID()){
            case 1: RegisterUserPacket registerUserPacket = (RegisterUserPacket) packet;
                    DB_users registerUser = registerUserPacket.getUser();
                    databaseManager.addUser(registerUser);
                    break;
            case 2: GetUserPacket getUserPacket = (GetUserPacket) packet;
                    String name = getUserPacket.getName();
                    DB_users user1 = databaseManager.getUserByName(name);
                    RespUserPacket response1 = new RespUserPacket(packet.getAddress(), user1);
                    client.queuePacketForWrite(response1);
                    break;
            case 3: List<DB_users> usersList = databaseManager.getUsers();
                    DB_users[] users = usersList.toArray(new DB_users[usersList.size()]);
                    RespUsersPacket response2 = new RespUsersPacket(packet.getAddress(), users);
                    client.queuePacketForWrite(response2);
                    break;
            case 6: DelUserPacket delUserPacket = (DelUserPacket) packet;
                    String delName = delUserPacket.getName();
                    databaseManager.deleteUser(delName);
                    break;
            default: WifiAccess.LOGGER.info("No handling for packet with ID " + packet.getID() + " implemented!");
        }
    }

}
