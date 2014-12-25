package io.github.gelx_.wifiaccess;

import io.github.gelx_.wifiaccess.database.DB_users;

import java.io.IOException;

/**
 * Created by Gelx on 31.10.2014.
 */
public class IPTablesConnector {

    public static final String insertCmd = "/usr/bin/iptables -I captive_portal 1 -m mac --mac-source %mac% -j ACCEPT";
    public static final String removeCmd = "/usr/bin/iptables -D captive_portal -m mac --mac-source %mac% -j ACCEPT";

    public IPTablesConnector(){
    }

    private void allowDenyMac(String mac, boolean allow){
        if(!DB_users.MACPATTERN.matcher(mac).matches()){
            throw new IllegalArgumentException("Given mac is not a valid MAC: " + mac);
        }
        String command = allow ? insertCmd.replace("%mac%", mac) : removeCmd.replace("%mac%", mac);
        try {
            executeCommand(command);
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Could not execute command '" + command + "' ! " + e.getMessage());
        }
    }

    public void allowMac(String mac){
        allowDenyMac(mac, true);
    }
    public void denyMac(String mac){
        allowDenyMac(mac, false);
    }

    public void allowMacs(String[] macs){
        for(String mac : macs)
            allowMac(mac);
    }

    public void executeCommand(String command) throws IOException {
        Runtime.getRuntime().exec(command);
    }

}
