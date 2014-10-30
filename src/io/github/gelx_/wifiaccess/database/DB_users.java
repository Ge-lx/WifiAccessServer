package io.github.gelx_.wifiaccess.database;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Created by Falk on 07.10.2014.
 */
public class DB_users {

    public static final Pattern MACPATTERN = Pattern.compile("^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");
    public static final Pattern CODEPATTERN = Pattern.compile("^[0-9A-Z]{6}$");

    private String name;
    private String mac;
    private String code;
    private long expires;

    public DB_users(String name, String mac, long expires, String code){
        if(name == null || mac == null)
            throw new IllegalArgumentException("Name or mac may not be null!");
        if(code == null)
            code = generateCode();
        if(! MACPATTERN.matcher(mac).matches()){
            throw new IllegalArgumentException("Given mac address is not valid: " + mac);
        }
        if(! CODEPATTERN.matcher(code).matches()){
            throw new IllegalArgumentException("Given code is not valid: " + code);
        }
        this.name = name;
        this.mac = mac;
        this.code = code;
        this.expires = expires;
    }

    /**
     * Creates a new user with wildcard mac and random code
     * @param name
     * @param expiresIn
     */
    public DB_users(String name, int expiresIn) {
        this(name, "00:00:00:00:00:00", System.currentTimeMillis() + (expiresIn * 3600000L), generateCode());
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
    public String getCode() {
        return this.code;
    }

    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocateDirect(128);
        buffer.put(Charset.defaultCharset().encode(mac));
        buffer.putLong(expires);
        buffer.put(Charset.defaultCharset().encode(code));
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
        String mac = new String(macBytes, Charset.defaultCharset()).trim();

        long expires = buffer.getLong();

        byte[] codeBytes = new byte[6];
        buffer.get(codeBytes);
        String code = new String(codeBytes, Charset.defaultCharset()).trim();
        byte[] nameBytes = new byte[buffer.remaining()];
        buffer.get(nameBytes);
        String name = new String(nameBytes, Charset.defaultCharset()).trim();
        return new DB_users(name, mac, expires, code);
    }

    private static String generateCode(){
        Random random = new Random();
        String code = new String();
        for(int i = 0; i < 6; i++){
            if(random.nextInt(2) == 1){
                code += random.nextInt(10);
            }else{
                code += (char) random.nextInt(26) + 65;
            }
        }
        return code;
    }
}
