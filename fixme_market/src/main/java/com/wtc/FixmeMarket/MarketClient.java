/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wtc.FixmeMarket;

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
import java.util.HashMap;
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
public class MarketClient {
    protected String UUID;
    protected SelectionKey key = null;
    private InputThread thread;
    private ExecutorService InputThread;
    private boolean firstConnect = true;
    MarketObj _MarketClient;

    MarketClient(String ip, MarketObj MarketClient) throws UnknownHostException, IOException {
        this._MarketClient = MarketClient;
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(ip), 5001);
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

    // public static Boolean processReadySet(Set readySet) throws Exception {
    public Boolean processReadySet(Set<SelectionKey> readySet) throws IOException {
        Iterator<SelectionKey> iterator = null;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            this.key = iterator.next(); // (SelectionKey) cast
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
            ParseFix(result);
        }
        if (this.key.isWritable()) {
            String msg = null;
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = null;

            if (firstConnect) {
                msg = '[' + UUID + "] " + "REGISTER-MARKET " + _MarketClient.getName();
                bb = ByteBuffer.wrap(msg.getBytes());
                sc.write(bb);
                firstConnect = false;
            }

            // Clean Exit
            if (!thread.MsgQueue.isEmpty()) {
                // Itterate through message Queue and Send them
                for (int i = 0; i < thread.MsgQueue.size(); ++i) {
                    msg = thread.MsgQueue.get(i);
                    if (msg.equals("exit") || msg.equals("quit")) {
                        InputThread.shutdown();
                        sc.close();
                        System.exit(0);
                    }
                    else {
                        msg = '[' + UUID + "] " + msg;
                        log.info(msg);
                        bb = ByteBuffer.wrap(msg.getBytes());
                        sc.write(bb);
                    }
                }
                thread.MsgQueue.clear();
            }
        }
        return false;
    }

    private String FinalizeFixMessage(String msg, String BrokerClientID) {
        msg = '|' + msg + "|49=" + BrokerClientID + "|";
        String fixMsg = "8=FIX.4.2|9=" + (msg.length()) + msg;
        if (fixMsg != null) {
            fixMsg += "10=" + GenerateCheckSum(fixMsg) + '|';
        }
        return fixMsg;
    }

    private void ParseFix(String result) {
        HashMap<String, String> KeyValuePair = new HashMap<>();
        // Step 1. Break into segments 
        String[] msgParts = result.split("\\|");
        for (String msg :msgParts)
        {
            // Step 2. Break into Key Value Pairs
            String[] Key_Val = msg.split("=");
            KeyValuePair.put(Key_Val[0], Key_Val[1]);
        }

        // NEXT STEPS:
        // Market Instrument Exists?
        if (KeyValuePair.containsKey("460"))
        {
            boolean found = false; 
            String ProductName = KeyValuePair.get("460");
            for (Instrument product : _MarketClient.getInstruments())
            {
                if (product.getName().equals(ProductName))
                {
                    BuyorSell(KeyValuePair.get("38"), KeyValuePair.get("44"),KeyValuePair.get("54"), KeyValuePair.get("49"), product);
                    found= true;
                    break;
                }
            }
            if (!found)
            {
                // Write Instrument Not found To router
                String msg = "35=8|39=2|55=" + _MarketClient.getName() + "|58=We Do not have such a Instrument";
                String Fixresponse = FinalizeFixMessage(msg,  KeyValuePair.get("49"));
                thread.MsgQueue.add(Fixresponse);
            }
        }
    }

    private void BuyorSell(String sShareAmount, String sPrice, String BuyorSell, String BrokerID, Instrument product) {
        int shareAmount = 0;
        Float Price = 0.0f;
        try {
            shareAmount= Integer.parseInt(sShareAmount);
            Price = Float.parseFloat(sPrice);
        }
        catch (NumberFormatException nfe) {
            log.error("Error BUY-or-SELL:" + nfe.getMessage());
        }
        // BUY
        if (BuyorSell.equals("1"))
            BuyShares(product, shareAmount, Price, BrokerID);
        // SELL
        if (BuyorSell.equals("2"))
            SellShares(product, shareAmount, Price, BrokerID);
    }

    public Boolean processConnect(SelectionKey key) {
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


    //    SINCE THIS IS A PRICE LIMIT, WE'll be getting the max amount of shares for the price but stop if we hit max
    public int GetMaxShares(float captial, float ppu, int max) {
        int Amount = (int)Math.floor(captial / ppu);
        return (Amount > max) ? max : Amount; 
    }

    private String GenerateCheckSum(String fixmsg)
    {
        int sum=0;
        char[] bs = fixmsg.toCharArray();
        for (char b : bs)
            sum += (b == '|') ? 1 : b;
        return String.format("%03d" , sum % 256);
    } 

    public void BuyShares(Instrument ins, int ShareAmount, Float Price, String BrokerID) {
        int MaxSharesAmount = GetMaxShares(Price, ins.getPrice(), ShareAmount);
        if (MaxSharesAmount <= 0 || MaxSharesAmount > ins.getShares())
        {
            String msg = "35=8|39=2|55=" + _MarketClient.getName() + "|58=We can not provide you with this amount of instruments, we can provide a saldo of: "+ ins.getShares() +"|460=" + ins.getName();
            String Fixresponse = FinalizeFixMessage(msg,  BrokerID);
            thread.MsgQueue.add(Fixresponse);
        }
        else {
            // Write Back Success and remove some shares :D 
            float totalPrice = MaxSharesAmount * ins.getPrice();
            String msg = "35=8|39=10|55=" + _MarketClient.getName() + "|58=You've Bought " + MaxSharesAmount + " instruments of "+ ins.getName() + " at :" + totalPrice + "|460=" + ins.getName();
            String Fixresponse = FinalizeFixMessage(msg,  BrokerID);
            thread.MsgQueue.add(Fixresponse);

            ins.setShares(ins.getShares() - MaxSharesAmount);
        }
    }

    public void SellShares(Instrument ins, int ShareAmount, Float Price, String BrokerID) {
        // Max Shares to sell
        int MaxSharesAmount = GetMaxShares(Price, ins.getPrice(), ShareAmount);
        if (MaxSharesAmount <= 0 || (MaxSharesAmount + ins.getShares()) > ins.getMaxShares())
        {
            String msg = "35=8|39=2|55=" + _MarketClient.getName() + "|58=We can not buy this amount of this instruments from you We can buy a saldo of:" + (ins.getMaxShares() - ins.getShares()) + "|460=" + ins.getName();
            String Fixresponse = FinalizeFixMessage(msg,  BrokerID);
            thread.MsgQueue.add(Fixresponse);
        }
        else {
            // Write Back Success and add some shares :D 
            float totalPrice = MaxSharesAmount * ins.getPrice();
            String msg = "35=8|39=10|55=" + _MarketClient.getName() + "|58=You've Sold " + MaxSharesAmount + " instruments of "+ ins.getName() + " at :" + totalPrice + "|460=" + ins.getName();           
            String Fixresponse = FinalizeFixMessage(msg,  BrokerID);
            thread.MsgQueue.add(Fixresponse);

            ins.setShares(ins.getShares() + MaxSharesAmount);
        }
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
                if (msg.equals("exit") || msg.equals("quit"))
                    MsgQueue.add(msg);
            } catch ( IOException e) {
               log.error(e.getMessage());
            }
        }
   }
} 