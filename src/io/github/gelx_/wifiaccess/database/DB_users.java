package io.github.gelx_.wifiaccess.database;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Falk on 07.10.2014.
 */
public class DB_users {

    public static final Pattern MACREGEX = Pattern.compile("^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");

    private String name;
    private String mac;
    private long expires;

    public DB_users(String name, String mac, long expires){
        if(name == null || mac == null)
            throw new IllegalArgumentException("Name or mac may not be null!");
        this.name = name;
        if(! MACREGEX.matcher(mac).matches()){
            throw new IllegalArgumentException("Given mac address is not valid: " + mac);
        }
        this.mac = mac;
        this.expires = expires;
    }

    public DB_users(String name, String mac, int expiresIn) {
        this(name, mac, System.currentTimeMillis() + (expiresIn * 3600L));
    }

    public String getName(){
        return this.name;
    }
    public String getMac() {
        return this.mac;
    }
    public long getExpires() {
        return this.expires;
    }
    public boolean isExpired() {
        return this.expires <= System.currentTimeMillis();
    }

    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocateDirect(128);
        buffer.put(Charset.defaultCharset().encode(mac));
        buffer.putLong(expires);
        buffer.put(Charset.defaultCharset().encode(name));
        int index = buffer.position();
        buffer.limit(index);
        buffer.flip();
        byte[] data = new byte[index];
        buffer.get(data);
        return data;
    }

    public static DB_users fromBytes(byte[] data){
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] macBytes = new byte[17];
        buffer.get(macBytes);
        String mac = new String(macBytes, Charset.defaultCharset());

        long expires = buffer.getLong();
        byte[] nameBytes = new byte[buffer.remaining()];
        buffer.get(nameBytes);
        String name = new String(nameBytes, Charset.defaultCharset());
        return new DB_users(name, mac, expires);
    }
}
