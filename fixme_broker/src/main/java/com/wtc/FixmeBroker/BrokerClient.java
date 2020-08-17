package com.wtc.FixmeBroker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import lombok.extern.log4j.Log4j;

/**
/**
 *
 * @author Ruben
 */
@Log4j
public class BrokerClient {
    private static BufferedReader input = null;
    private Iterator iterator;
    private InetSocketAddress addr;
    private Selector selector;
    private SocketChannel sc;
    private Boolean doneStatus;
    private static ByteBuffer bb = ByteBuffer.allocate(1024);
    private static String UUID;

    BrokerClient(final String ip) {
        try {
            addr = new InetSocketAddress(InetAddress.getByName(ip), 5000);
            selector = Selector.open();
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(addr);
            sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                if (selector.select() > 0) {
                    doneStatus = processReadySet(selector.selectedKeys());
                    if (doneStatus) {
                        break;
                    }
                }
            }
            sc.close();
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
    }

    public Boolean processReadySet(Set readySet) throws IOException {
        SelectionKey key = null;
        iterator = null;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
        }
        if (key.isConnectable()) {
            Boolean connected = processConnect(key);
            if (!connected) {
                return true;
            }
        }
        if (key.isReadable()) {
            sc = (SocketChannel) key.channel();
            sc.read(bb);
            String result = new String(bb.array()).trim();
            log.info("[Server]: " + result);
        }
        if (key.isWritable()) {
            System.out.print("Message: ");
            String msg = input.readLine();
            if (msg.equalsIgnoreCase("quit") || msg.equalsIgnoreCase("exit") || msg.equalsIgnoreCase("q")) {
                return true;
            }
            msg = '[' + UUID + "] " + msg;
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
            sc.write(bb);
        }
        return false;
    }

    public static Boolean processConnect(final SelectionKey key) {
        final SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                // SET ID
                sc.finishConnect();
                sc.read(bb);
                UUID = new String(bb.array()).trim();
				// log.info(UUID);
            }
        } catch (final IOException e) {
         key.cancel();
         e.printStackTrace();
         return false;
      }
      return true;
   }
}