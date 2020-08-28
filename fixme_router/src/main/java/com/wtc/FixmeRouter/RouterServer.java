/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wtc.FixmeRouter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.wtc.FixmeRouter.RouterChain.ClientChain;
import com.wtc.FixmeRouter.RouterChain.ClientMessage;

import lombok.extern.log4j.Log4j;

/**
 * @author Ruben
 *         https://www.developer.com/java/data/what-is-non-blocking-socket-programming-in-java.html
 */

@Log4j
public class RouterServer {
    boolean runServer = true;
    InetAddress host;
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    SelectionKey key;
    static Set<SelectionKey> selectedKeys;
    Iterator<SelectionKey> iterator;
    SocketChannel sc;
    ClientChain cc;
    ByteBuffer bb;
    String result;
    String sUUID;
    boolean firstRead = true;
    String _port;
    static int i = 50000;

    RouterServer(final String _host, final int port) {
        try {
            host = InetAddress.getByName(_host);
            selector = Selector.open();
            cc = new ClientChain();
            
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(host, port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        
            key = null;

            while (runServer) {
                if (selector.select() <= 0)
                    continue;
                selectedKeys = selector.selectedKeys();

                iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    key = (SelectionKey) iterator.next();
                    if (key.isAcceptable()) {
                        RegisterNewClient();
                    }  
                    if (key.isReadable()) {
                        ReadFromClient();    
                    }
                   
                    iterator.remove();
                }
            }
        } catch (final IOException e) {
            log.error(e.getMessage());
        } catch (final CancelledKeyException Cke) {
            log.error(Cke.getMessage());
        }
    }

    private void RegisterNewClient() throws IOException {
        sc = serverSocketChannel.accept();
        _port = sc.getLocalAddress().toString().split(":")[1];
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
        if (_port.equals("5001"))
            sUUID = 'M' + Integer.toString(i++);
        else
            sUUID = 'B' + Integer.toString(i++);
        //  UUID SEND TO CLIENT
        sc.write(ByteBuffer.wrap(sUUID.getBytes()));
        FixmeRouter.scl.put(sUUID, sc);
        log.info("Connection Accepted: " + sUUID + sc.getRemoteAddress() + "  " + sUUID + "\n");
    }

    private void ReadFromClient() throws IOException {
        sc = (SocketChannel) key.channel();
        // Current Channel
        bb = ByteBuffer.allocate(1024);
        sc.read(bb);
        result = new String(bb.array()).trim();
        
        if (result.length() > 8)
        {
            String _UUID = result.substring(1, 7);
            String messString = result.substring(9);
            // System.out.println(_UUID + " - [" + messString + ']');
            cc.process(new ClientMessage(cc, _UUID, messString, sc));
        }
        if (result.length() <= 0) {
            sc.close();
            log.info("Connection closed...");
        }
    }
}
