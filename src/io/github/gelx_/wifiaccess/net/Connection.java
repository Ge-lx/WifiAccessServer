package io.github.gelx_.wifiaccess.net;

import io.github.gelx_.wifiaccess.WifiAccess;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by Gelx on 10.10.2014.
 */
public class Connection implements Runnable{

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private List<SocketChannel> channels = new ArrayList<>();

    private PacketHandler handler;

    private HashMap<SocketAddress, Stack<ByteBuffer>> outputQueue = new HashMap<>();

    public Connection(InetSocketAddress bindAddress){
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Could not open Selector! " + e.getMessage());
            System.exit(1);
        }
        try {
            this.serverChannel = ServerSocketChannel.open();
            serverChannel.bind(bindAddress);
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Could not initialize sockets! " + e.getMessage());
        }

        handler = new PacketHandler();

        new Thread(this).run();
    }

    public void run(){
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4); //Max 4 byte for int

        while(!Thread.interrupted()){
            try {
                if (selector.select(10 * 1000) == 0) {
                    WifiAccess.LOGGER.severe("No Socket is ready for operations after 10sec. Aborting.");

                    for (SocketChannel channel : channels) {
                        try {
                            channel.close();
                        } catch (IOException e) {
                            WifiAccess.LOGGER.severe("Exception while closing sockets! " + e.getMessage());
                        }
                    }
                    try {
                        serverChannel.close();
                    }catch(IOException e){
                        WifiAccess.LOGGER.severe("Exception while closing sockets! " + e.getMessage());
                    }
                    selector.close();
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    SocketChannel channel = (SocketChannel) key.channel();


                    if(!key.isValid()){
                        String hostname = ((InetSocketAddress)((SocketChannel)key.channel()).getRemoteAddress()).getHostString();//ridiculous
                        WifiAccess.LOGGER.info("Connection with " + hostname  + " closed by remote host!");
                        channels.remove(key.channel());

                    }

                    if(key.isAcceptable()){
                        //Accept connection
                        ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = keyChannel.accept();
                        //Add connection to selector
                        channels.add(clientChannel);
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        //Add stack in output-queue
                        outputQueue.put(clientChannel.getRemoteAddress(), new Stack<ByteBuffer>());

                    }else if(key.isWritable()){
                        //Check output-queue for address and write if not empty
                        Stack<ByteBuffer> outputStack = outputQueue.get(channel.getRemoteAddress());
                        if(!outputStack.isEmpty()){
                            channel.write(outputStack.pop());
                        }
                    }else if(key.isReadable()){
                        //Read packID (short -> 2b)
                        readToBuffer(readBuffer, channel, 2);
                        short packetID = readBuffer.getShort();
                        //Read dataSize (int -> 4b)
                        readToBuffer(readBuffer, channel, 4);
                        int dataSize = readBuffer.getInt();
                        //Read data (size -> dataSize)
                        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(dataSize);
                        readToBuffer(dataBuffer, channel, dataSize);

                        //Hand off to packetHandler
                        handler.handlePacket(Protocol.unpackPacket(channel.getRemoteAddress(), packetID, dataBuffer));
                    }

                    iterator.remove();
                }
            } catch (IOException e) {
                WifiAccess.LOGGER.severe("Error in Connection! " + e.getMessage());
                Thread.currentThread().interrupt();
            }

        }

        WifiAccess.LOGGER.severe("No Socket is ready for operations after 10sec. Aborting.");

        for (SocketChannel channel : channels) {
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
            selector.close();
        } catch (IOException e) {
            WifiAccess.LOGGER.severe("Exception while closing selector! " + e.getMessage());
        }

    }

    public void queuePacketForWrite(Protocol.Packet packet){
        if(!outputQueue.containsKey(packet.getAddress()))
            throw new IllegalArgumentException("No connection with that adderss!");
        outputQueue.get(packet.getAddress()).add(Protocol.packPacket(packet));
    }

    private void readToBuffer(ByteBuffer buffer, SocketChannel channel, int length) throws IOException {
        buffer.clear();
        buffer.limit(length);
        while(buffer.remaining() > 0)
            channel.read(buffer);
        buffer.flip();
    }

}
