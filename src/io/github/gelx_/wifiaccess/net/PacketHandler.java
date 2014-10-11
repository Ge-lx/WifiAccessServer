package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.WifiAccess;
import io.github.gelx_.wifiaccess.database.DB_users;
import io.github.gelx_.wifiaccess.net.Protocol.*;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Falk on 08.10.2014.
 */
public class PacketHandler implements Runnable{

    private BlockingDeque<Packet> packetQueue = new LinkedBlockingDeque<>(50);
    private Thread thread;

    public PacketHandler(){
        this.thread = new Thread(this);
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
                    //TODO: Do something with it;
                    break;
            case 2: GetUserPacket getUserPacket = (GetUserPacket) packet;
                    String name = getUserPacket.getName();
                    //TODO: Respond with user!
                    break;
            case 3: //TODO: Respond with users!
                    break;
            case 4: RespUserPacket respUserPacket = (RespUserPacket) packet;
                    DB_users respUser = respUserPacket.getUser();
                    //TODO: Do something with it!
                    break;
            case 5: RespUsersPacket respUsersPacket = (RespUsersPacket) packet;
                    DB_users[] respUsers = respUsersPacket.getUsers();
                    //TODO: Do something with it!
                    break;
            default: WifiAccess.LOGGER.info("No handling for packet with ID " + packet.getID() + " implemented!");
        }
    }

}
