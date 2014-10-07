package io.github.gelx_.wifiaccess.database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Falk on 07.10.2014.
 */
public class DB_users {

    private static final Pattern MACREGEX = Pattern.compile("^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");

    private String name;
    private String mac;
    private long expires;

    public DB_users(String name, String mac, long expires){
        this.name = name;
        if(! MACREGEX.matcher(mac).matches()){
            throw new IllegalArgumentException("Given mac address is not valid: " + mac);
        }
        this.mac = mac;
        if(System.currentTimeMillis() >= expires){
            throw new IllegalArgumentException("Expiredate is in the past!");
        }
        this.expires = expires;
    }



}
