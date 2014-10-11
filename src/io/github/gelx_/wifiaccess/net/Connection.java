package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.WifiAccess;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.SocketHandler;

/**
 * Created by Gelx on 10.10.2014.
 */
public class Connection{

    private Selector readSelector;
    private ServerSocketChannel serverChannel;
    private HashMap<SocketAddress, SocketChannel> channels = new HashMap<>();

    private PacketHandler handler;

    private LinkedBlockingQueue<Protocol.Packet> outputQueue = new LinkedBlockingQueue<>();
    private Thread receiveThread, sendThread;

    public Connection(InetSocketAddress bindAddress){
        try {
            this.readSelector = Selector.open();
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Could not open Selector! " + e.getMessage());
            System.exit(1);
        }
        try {
            this.serverChannel = ServerSocketChannel.open();
            serverChannel.bind(bindAddress);
            serverChannel.configureBlocking(false);
            serverChannel.register(readSelector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Could not initialize sockets! " + e.getMessage());
            System.exit(1);
        }

        handler = new PacketHandler();

        this.receiveThread = new Thread(new Runnable() {
            public void run() { runReader(); } });
        this.sendThread = new Thread( new Runnable(){
            public void run() { runSender(); } });
        receiveThread.start();
        sendThread.start();
    }

    public void runReader(){
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4); //Max 4 byte for int

        while(!Thread.interrupted()){
            try {
                readSelector.select();

                Iterator<SelectionKey> iterator = readSelector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();

                    try {
                        SocketChannel channel = null;
                        if (key.channel() instanceof SocketChannel)
                            channel = (SocketChannel) key.channel();

                        if (key.isAcceptable()) {
                            //Accept connection
                            ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
                            SocketChannel clientChannel = keyChannel.accept();
                            //Add connection to sendlist and selector
                            channels.put(clientChannel.getRemoteAddress(), clientChannel);
                            clientChannel.configureBlocking(false);
                            clientChannel.register(readSelector, SelectionKey.OP_READ);
                            WifiAccess.LOGGER.info("New client connected!");

                        } else if (key.isReadable()) {
                            //Read packID (short -> 2b)
                            readToBuffer(readBuffer, channel, 2);
                            short packetID = readBuffer.getShort();
                            //Read dataSize (int -> 4b)
                            readToBuffer(readBuffer, channel, 4);
                            int dataSize = readBuffer.getInt();
                            //Read data (size -> dataSize)
                            ByteBuffer dataBuffer = ByteBuffer.allocate(dataSize);
                            readToBuffer(dataBuffer, channel, dataSize);

                            //Hand off to packetHandler
                            handler.queuePacket(Protocol.unpackPacket(channel.getRemoteAddress(), packetID, dataBuffer));
                            WifiAccess.LOGGER.info("Received packet!");
                        }
                    }catch(EOFException e){
                        String hostname = ((InetSocketAddress) ((SocketChannel) key.channel()).getRemoteAddress()).getHostString();//ridiculous
                        WifiAccess.LOGGER.info("Connection with " + hostname + " closed!");
                        key.channel().close();
                        channels.remove(key.channel());
                    }

                    iterator.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
                WifiAccess.LOGGER.severe("Error in Connection! " + e.getMessage());
                Thread.currentThread().interrupt();
            }

        }

        WifiAccess.LOGGER.severe("No Socket is ready for operations after 10sec. Aborting.");

        for (SocketChannel channel : channels.values()) {
            try {
                channel.close();
            } catch (IOException e) {
                WifiAccess.LOGGER.severe("Exception while closing clientsockets! " + e.getMessage());
            }
        }
        try {
            serverChannel.close();
        }catch(IOException e){
            WifiAccess.LOGGER.severe("Exception while closing serversocket! " + e.getMessage());
        }
        try {
            readSelector.close();
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Exception while closing selector! " + e.getMessage());
        }

    }

    public void runSender(){
        while(!Thread.interrupted()){
            try {
                Protocol.Packet packet = outputQueue.take();
                SocketChannel channel = channels.get(packet.getAddress());
                if(channel.isConnected() && channel.isOpen()) {
                    try {
                        channel.write(Protocol.packPacket(packet));
                    } catch (IOException e) {
                        WifiAccess.LOGGER.severe("Could not send packet to " + packet.getAddress());
                    }
                }
            } catch (InterruptedException e) {
                WifiAccess.LOGGER.info("Sending thread was interrupted!");
                break;
            }
        }
    }

    public void closeConnections(){
        sendThread.interrupt();
        receiveThread.interrupt();
    }

    public void queuePacketForWrite(Protocol.Packet packet){
        outputQueue.add(packet);
    }

    private void readToBuffer(ByteBuffer buffer, SocketChannel channel, int length) throws IOException {
        buffer.clear();
        buffer.limit(length);
        while(buffer.remaining() > 0)
            try {
                channel.read(buffer);
            }catch(IOException e){
                throw new EOFException("IOException while reading!");
            }
        buffer.flip();
    }

}
