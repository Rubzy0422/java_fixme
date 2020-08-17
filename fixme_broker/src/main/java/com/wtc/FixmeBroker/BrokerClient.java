/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wtc.FixmeBroker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.log4j.Log4j;

/*
 * @author Ruben
 */
@Log4j
public class BrokerClient {
    protected String UUID;
    protected SelectionKey key = null;
    private  InputThread thread;
    private ExecutorService InputThread;

    BrokerClient( String ip) throws UnknownHostException, IOException {
         InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(ip), 5000);
         Selector selector = Selector.open();
         SocketChannel sc = SocketChannel.open();

        // Set Socket Options
        sc.configureBlocking(false);
        sc.connect(addr);
        sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        InputThread = Executors.newSingleThreadExecutor();
        thread = new InputThread();

        // START INPUT THREAD 
        InputThread.execute(thread);
        while (true) {
            if (selector.select() > 0) {
                 Boolean doneStatus = processReadySet(selector.selectedKeys());
                if (doneStatus) {
                    break;
                }
            }
        }
        sc.close();
    }

    // static
    public Boolean processConnect( SelectionKey key) {
         SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                sc.finishConnect();
            }
            // SET UUID
            ByteBuffer bb = ByteBuffer.allocate(1024);
            sc.read(bb);
            this.UUID = new String(bb.array()).trim();
            log.info(this.UUID);

        } catch ( IOException e) {
            key.cancel();
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // public static Boolean processReadySet(Set readySet) throws Exception {
    public Boolean processReadySet( Set<SelectionKey> readySet) throws IOException {
        Iterator<SelectionKey> iterator = null;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            this.key = iterator.next(); //(SelectionKey) cast
            iterator.remove();
        }
        if (this.key.isConnectable()) {
             Boolean connected = processConnect(this.key);
            if (!connected) {
                return true;
            }
        }
        if (this.key.isReadable()) {
             SocketChannel sc = (SocketChannel) this.key.channel();
             ByteBuffer bb = ByteBuffer.allocate(1024);
            sc.read(bb);
             String result = new String(bb.array()).trim();
            System.out.println("Message received from Server: " + result + " Message length= " + result.length());
        }
        if (this.key.isWritable()) {
            if (!thread.MsgQueue.isEmpty())
            {
                String msg = null;
                 SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer bb = null;
       
                // Itterate through message Queue and Send them
                for (int i = 0; i < thread.MsgQueue.size(); ++i) { 		      
                    msg = thread.MsgQueue.get(i);
                    if (msg.equals("exit") || msg.equals("quit"))
                    {
                        InputThread.shutdown();
                        sc.close();
                        System.exit(0);
                    }
                    msg = '[' + UUID + "] " + msg;
                    bb = ByteBuffer.wrap(msg.getBytes());
                    sc.write(bb);
                   
                }
                thread.MsgQueue.clear();
            }
        }
        return false;
    }
}

@Log4j
class InputThread implements Runnable {
    private static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    public List<String> MsgQueue = new ArrayList<String>();

    @Override
    public void run() {
        String msg;
        while (true) {
            System.out.print("Message: ");
            try {
                msg = input.readLine();
                MsgQueue.add(msg);
            } catch ( IOException e) {
               log.error(e.getMessage());
            }
        }
   }
} 