package io.github.gelx_.wifiaccess.database;

import io.github.gelx_.wifiaccess.WifiAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Falk on 06.10.2014.
 */
public class DatabaseManager {

    public static final String CONFIGNAME = "db.properties";
    public static final String TABLENAME = "users";

    private Connection dbConn; //java.sql.Connection, NOT ../net/Connection

    public DatabaseManager(){
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }

        File configfile = new File(System.getProperty("user.dir") + "/" + CONFIGNAME);
        if(!configfile.exists()){
            WifiAccess.LOGGER.info("Config file not found. Copying default config!");
            try {
                File defaultconfig = new File(getClass().getClassLoader().getResource(CONFIGNAME).getFile() );
                if(!configfile.createNewFile()){
                    WifiAccess.LOGGER.severe("Could not create new default config!");
                } else {
                    Files.copy(defaultconfig.toPath(), new FileOutputStream(configfile));
                    WifiAccess.LOGGER.info("Default config copied!");
                }
            } catch ( IOException e) {
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
            WifiAccess.LOGGER.info("Key \"user\" not found! Using \"wifiaccess\"");
            properties.setProperty("user", "wifiaccess");
        }
        if(properties.getProperty("password") == null){
            WifiAccess.LOGGER.info("Key \"password\" not found! Using \"wifiaccess\"");
            properties.setProperty("password", "wifiaccess");
        }
        String dbname = properties.getProperty("dbname");
        if(dbname == null){
            WifiAccess.LOGGER.info("Key \"dbname\" not found! Using \"wifiaccess\"");
            dbname = "wifiaccess";
        }

        WifiAccess.LOGGER.info("Config successfully parsed!");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
        try {
            this.dbConn = DriverManager.getConnection(url, properties);
        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Could not connect to database: " + url + ": "  +e.getMessage());
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
            String code = result.getString("code");

            result.close();
            try {
                return new DB_users(name, mac, expires, code);
            } catch (IllegalArgumentException e) {
                WifiAccess.LOGGER.info("User " + name + " has invalid data: " + e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Error querying user by name: " + e.getMessage());
            return null;
        }
    }

    public DB_users getUserByMac(String mac){
        if(!DB_users.MACPATTERN.matcher(mac).matches()){
            throw new IllegalArgumentException("Given mac address is invalid: " + mac);
        }
        try {
            PreparedStatement selectUserByMac = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + " WHERE mac=?;");
            selectUserByMac.setString(1, mac);
            ResultSet result = selectUserByMac.executeQuery();
            if(!result.first()){
                WifiAccess.LOGGER.info("Requested user for unknown mac: " + mac);
                return null;
            }
            String name = result.getString("name");
            long expires = result.getLong("expires");
            String code = result.getString("code");

            result.close();
            try{
                return new DB_users(name, mac, expires, code);
            } catch (IllegalArgumentException e) {
                WifiAccess.LOGGER.info("User " + name + " has invalid data: " + e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            WifiAccess.LOGGER.info("Error querying for user by mac: " + e.getMessage());
            return null;
        }
    }

    public List<DB_users> getExpiredUsers(){
        try {
            PreparedStatement selectExpiredUsers = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + " WHERE expires<?");
            selectExpiredUsers.setLong(1, System.currentTimeMillis());
            ResultSet result = selectExpiredUsers.executeQuery();

            List<DB_users> users = new ArrayList<>();
            if(!result.first()){
                WifiAccess.LOGGER.info("No expired users");
                result.close();
                return users;
            }
            do{
                String name = result.getString("name");
                String mac = result.getString("mac");
                long expires = result.getLong("expires");
                String code = result.getString("code");
                try {
                    users.add(new DB_users(name, mac, expires, code));
                } catch (IllegalArgumentException e) {
                    WifiAccess.LOGGER.info("User " + name + " has invalid data: " + e.getMessage());
                }
            }while(result.next());
            result.close();

            return users;
        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Error querying for expired users: " + e.getMessage());
            return null;
        }
    }

    public List<DB_users> getUsers(){
        try {
            PreparedStatement selectAllUsers = dbConn.prepareStatement("SELECT * FROM " + TABLENAME + ";");
            ResultSet result = selectAllUsers.executeQuery();

            List<DB_users> users = new ArrayList<>();
            if(!result.first()){
                WifiAccess.LOGGER.info("No users!");
                result.close();
                return users;
            }
            do{
                String name = result.getString("name");
                String mac = result.getString("mac");
                long expires = result.getLong("expires");
                String code = result.getString("code");
                try{
                    users.add(new DB_users(name, mac, expires, code));
                } catch (IllegalArgumentException e) {
                    WifiAccess.LOGGER.info("User " + name + " has invalid data: " + e.getMessage());
                }
            }while(result.next());
            result.close();

            return users;
        } catch (SQLException e) {
            WifiAccess.LOGGER.severe("Error querying for users: " + e.getMessage());
            return null;
        }
    }

    public void addUser(DB_users user) {
        try {
            PreparedStatement insertUser = dbConn.prepareStatement("INSERT INTO " + TABLENAME + "(name,mac,expires,code) VALUES (?,?,?,?);");
            insertUser.setString(1, user.getName());
            insertUser.setString(2, user.getMac());
            insertUser.setLong(3, user.getExpires());
            insertUser.setString(4, user.getCode());
            if(!insertUser.execute()){
                WifiAccess.LOGGER.info("Could not insert new user into database!");
                return;
            }
        } catch (SQLException e) {
            WifiAccess.LOGGER.info("Error inserting new user into database: " + e.getMessage());
        }
    }

    public void deleteUser(String name){
        try {
            PreparedStatement deleteUser = dbConn.prepareStatement("DELETE FROM " + TABLENAME + " WHERE name=?;");
            deleteUser.setString(1, name);
            if(deleteUser.executeUpdate() == 0){
                WifiAccess.LOGGER.info("Could not delete user " + name);
            }
        } catch (SQLException e) {
            WifiAccess.LOGGER.info("Error deleting user from database: " + e.getMessage());
        }
    }
}
