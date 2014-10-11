package io.github.gelx_.wifiaccess;

import io.github.gelx_.wifiaccess.net.Connection;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * Created by Falk on 06.10.2014.
 */
public class WifiAccess {

    public static final Logger LOGGER = Logger.getLogger("WifiAccess");
    public static final String VERSION = "0.1DEV";

    private static Connection connection;

    public static void main(String... args){

        LOGGER.info("Starting WifiAccess v" + VERSION);

        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8881);
        connection = new Connection(address);

        //TODO: Write main
    }

    public static Connection getConnection(){
        return connection;
    }

}
