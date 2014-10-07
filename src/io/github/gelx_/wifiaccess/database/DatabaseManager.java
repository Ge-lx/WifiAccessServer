package io.github.gelx_.wifiaccess.database;

import io.github.gelx_.wifiaccess.WifiAccess;

import javax.xml.transform.Result;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.sql.*;
import java.util.Properties;

/**
 * Created by Falk on 06.10.2014.
 */
public class DatabaseManager {

    public static final String CONFIGNAME = "db.properties";
    public static final String TABLENAME = "users";

    private Connection dbConn;

    public DatabaseManager(){


        File configfile = new File(System.getProperty("user.dir") + System.lineSeparator() + CONFIGNAME);
        if(!configfile.exists()){
            WifiAccess.LOGGER.info("Config file not found. Copying default config!");
            try {
                File defaultconfig = new File(getClass().getClassLoader().getResource(CONFIGNAME).toURI() );
                if(!configfile.createNewFile()){
                    WifiAccess.LOGGER.severe("Could not create new default config!");
                    Files.copy(defaultconfig.toPath(), new FileOutputStream(configfile));
                    WifiAccess.LOGGER.info("Default config copied!");
                }
            } catch (URISyntaxException | IOException e) {
                WifiAccess.LOGGER.severe("Could not copy default config! " + e.getMessage());
                System.exit(1);
            }
        }
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configfile));
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Error reading properties! " + e.getMessage());
            System.exit(1);
        }
        String host = properties.getProperty("host");
        if(host == null){
            WifiAccess.LOGGER.info("Key \"host\" not found! Using \"127.0.0.1\"");
            host="127.0.0.1";
        }
        int port;
        try{
            port = Integer.parseInt(properties.getProperty("port", "3306"));
        }catch(NumberFormatException e){
            WifiAccess.LOGGER.severe("Port is not a number. Using 3306");
            port = 3306;
        }
        if(properties.getProperty("user") == null){
            WifiAccess.LOGGER.info("Key \"user\" not found! Using \"mysql\"");
            properties.setProperty("user", "mysql");
        }
        if(properties.getProperty("password") == null){
            WifiAccess.LOGGER.info("Key \"password\" not found! Using \"mysql\"");
            properties.setProperty("password", "mysql");
        }
        String dbname = properties.getProperty("dbname");
        if(dbname == null){
            WifiAccess.LOGGER.info("Key \"dbname\" not found! Using \"wifiaccess\"");
        }

        WifiAccess.LOGGER.info("Config successfully parsed!");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
        try {
            this.dbConn = DriverManager.getConnection(url, properties);
        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Could not connect to database: " + url);
            System.exit(1);
        }

        WifiAccess.LOGGER.info("Connected to database: " + url);
        WifiAccess.LOGGER.info("Validating connection... (max 10s)");
        try {
            if(!this.dbConn.isValid(10))
                throw new SQLException("isValid() returned false");
        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Connection to database unsuccessful " + e.getMessage());
            System.exit(1);
        }


        //Prepare statements

        /*this.selectUserByName = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + " WHERE name = ?;");
        this.selectUserByMac = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + " WHERE mac = ?;");
        this.selectExpiredUsers = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + " WHERE expires < ?");
        */
    }

    public DB_users getUserByName(String name){
        try {
            PreparedStatement selectUserByName = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + " WHERE name = ?;");
            selectUserByName.setString(1, name);
            ResultSet result = selectUserByName.executeQuery();
            if(!result.first()){
                WifiAccess.LOGGER.info("Requested non-existent user " + name);
                return null;
            }
            String mac = result.getString("mac");
            long expires = result.getLong("expires");

            result.close();

            try {
                return new DB_users(name, mac, expires);
            } catch (IllegalArgumentException e) {
                WifiAccess.LOGGER.info("User " + name + " has invalid data: " + e.getMessage());
                return null;
            }

        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Error querying user by name: " + e.getMessage());
            return null;
        }
    }

    //TODO: Add methods for interaction with project
    //TODO: Add database interface (statements...) | in progress...
}
