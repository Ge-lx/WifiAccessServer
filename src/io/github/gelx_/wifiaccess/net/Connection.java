package io.github.gelx_.wifiaccess.net;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Connection{

    private static final Logger LOG = Logger.getLogger("Connection");

    private SSLServerSocket serverSocket;
    private Thread serverThread;
    private List<ClientHandler> clientHandlers = new ArrayList<>();

    public Connection(SocketAddress bindAddress){

        try {
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket();
        } catch (IOException e) {
            LOG.severe("Could not create serverSocket! " + e.getMessage());
            throw new RuntimeException(e);
        }
        try {
            serverSocket.bind(bindAddress);
        } catch (IOException e) {
            LOG.severe("Could not bind serverSocket! " + e.getMessage());
            throw new RuntimeException(e);
        }

        serverThread = new Thread( new Runnable(){
                public void run(){
                    runAcceptor();
                }
            } );
        serverThread.start();
    }

    public void runAcceptor(){
        while(!Thread.interrupted()){
            SSLSocket clientSocket;
            try {
                clientSocket = (SSLSocket) serverSocket.accept();
                LOG.info("Client connected!");
            } catch (IOException e) {
                LOG.severe("Error while waiting for client-connection! " + e.getMessage());
                break;
            }
            clientHandlers.add(new ClientHandler(clientSocket));
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOG.severe("Error while closing serverSocket! " + e.getMessage());
        }
    }

    public void close(){
        serverThread.interrupt();//Also closes socket
        for(ClientHandler client : clientHandlers){
            client.close();
        }
    }
}