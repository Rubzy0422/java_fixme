/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wtc.FixmeRouter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
    
    ByteBuffer bb;
    String result;
    String sUUID;
    static int i = 50000;

    RouterServer(final String _host, final int port) {
        try {
            host = InetAddress.getByName(_host);
            selector = Selector.open();
            
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
                    if (key.isWritable()) {
                        WriteToClient();
                    }
                    
                    if (key.isReadable()) {
                        ReadFromClient();    
                    }
                   
                    iterator.remove();
                }
            }
            // scl.forEach((key, value) -> System.out.println(key + ":" + value));

        } catch (final IOException e) {
            log.error(e.getMessage());
        } catch (final CancelledKeyException Cke) {
            log.error(Cke.getMessage());
        }
    }

    private void RegisterNewClient() throws IOException {
        sc = serverSocketChannel.accept();
        final String _port = sc.getLocalAddress().toString().split(":")[1];
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
        if (_port.equals("5001"))
            sUUID = 'M' + Integer.toString(i++);
        else
            sUUID = 'B' + Integer.toString(i++);
        sc.write(ByteBuffer.wrap(sUUID.getBytes()));
        FixmeRouter.scl.put(sUUID, sc);
        log.info("Connection Accepted: " + sUUID + sc.getRemoteAddress() + "  " + sUUID + "\n");
    }

    private void WriteToClient() throws IOException {
        System.out.print("Enter Message: ");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String msg = input.readLine();
        msg = "[SERVER]" + msg;
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
        sc.write(bb);
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
            System.out.println(_UUID + " - [" + messString + ']');
     

            for ( String key : FixmeRouter.scl.keySet() ) {
                System.out.println( key );
            }

            if (messString.length() == 6) {
                SocketChannel _sc;
                // SEND TO BROKER FROM MARKET 
                _sc = FixmeRouter.scl.get(messString);    
                ByteBuffer bb = ByteBuffer.wrap(messString.getBytes());
                _sc.write(bb);
                _sc = null;
            }

            if (result.equals("[M5000] STOPSERVER PASS:Password!@123"))
            {
                runServer = false;
                System.exit(0);
            }
        }
        if (result.length() <= 0) {
            sc.close();
            log.info("Connection closed...");
        }
    }
}
