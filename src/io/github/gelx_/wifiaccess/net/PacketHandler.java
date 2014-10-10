package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.database.DB_users;
import io.github.gelx_.wifiaccess.net.Protocol.*;

/**
 * Created by Falk on 08.10.2014.
 */
public class PacketHandler {

    //TODO: Add threading

    public void handlePacket(Packet packet){

        switch (packet.getID()){
            case 1: RegisterUserPacket registerUserPacket = (RegisterUserPacket) packet;
                    DB_users user = registerUserPacket.getUser();
                    //TODO: Do something with it;
                    break;
            case 2: GetUserPacket getUserPacket = (GetUserPacket) packet;
                    String name = getUserPacket.getName();
                    //TODO: Respond with user!
                    break;
            case 3: //TODO: Respond with users!
                    break;
            default: throw new IllegalArgumentException("Packet with id " + packet.getID() + " is not meant to be received!");
        }
    }

}
