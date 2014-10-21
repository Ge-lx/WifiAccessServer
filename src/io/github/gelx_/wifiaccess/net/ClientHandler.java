package io.github.gelx_.wifiaccess.net;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Created by Gelx on 13.10.2014.
 */
public class ClientHandler {

    private static final Logger LOG = Logger.getLogger("ClientHandler");

    private SSLSocket socket;
    private PacketHandler handler;

    private Thread recvThread, sendThread;

    private BlockingQueue<Protocol.Packet> writeQueue = new LinkedBlockingQueue<>(50);

    public ClientHandler(SSLSocket socket){
        this.socket = socket;
        socket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
            @Override
            public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
                LOG.info("Handshake completed!");
                recvThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runRecv();
                    }
                });
                sendThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runSend();
                    }
                });
                recvThread.start();
                sendThread.start();
            }
        });
        try {
            socket.startHandshake();
        } catch (IOException e) {
            LOG.severe("Error while handshaking: " + e.getMessage());
        }
        handler = new PacketHandler(this);
    }

    public void queuePacketForWrite(Protocol.Packet packet){
        if(!writeQueue.offer(packet))
            LOG.severe("Could not write packet! Queue overflow!");
    }

    public void runRecv(){
        DataInputStream inputStream;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            LOG.severe("Could not get inputstream! " + e.getMessage());
            return;
        }
        while(!Thread.interrupted()){
            try {
                short packetID = inputStream.readShort();
                int dataLength = inputStream.readInt();
                byte[] data = new byte[dataLength];
                inputStream.readFully(data);
                handler.queuePacket(Protocol.unpackPacket(socket.getRemoteSocketAddress(), packetID, ByteBuffer.wrap(data)));
            } catch (EOFException e){
                LOG.info("Connection with " + ((InetSocketAddress)socket.getRemoteSocketAddress()).getHostString() + " closed by remote host!") ;
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                LOG.severe("Error while reading packet! " + e.getMessage());
            }
        }
        try {
            sendThread.interrupt();
            socket.close();
        } catch (IOException e) {
            LOG.severe("Error while closing socket! " + e.getMessage());
        }
    }

    public void runSend(){
        DataOutputStream outputStream;
        try {
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            LOG.severe("Could not get outputStream! " + e.getMessage());
            return;
        }
        while(!Thread.interrupted()){
            try {
                Protocol.Packet packet = writeQueue.take();
                outputStream.write(Protocol.packPacket(packet).array());
            } catch (InterruptedException e) {
                LOG.info("Sending thread interrupted!");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                LOG.severe("Could not write packet! " + e.getMessage());
            }
        }
    }

    public void close(){
        handler.stop();
        recvThread.interrupt();//Also closes socket and interrupts sender
    }

}
