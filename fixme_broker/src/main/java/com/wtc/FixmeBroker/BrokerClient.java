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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Data;
import lombok.extern.log4j.Log4j;

/*
 * @author Ruben
 */
@Log4j
@Data
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
        thread = new InputThread(this);

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
            log.info(result);
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
                    if (msg.equals("STOP"))
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
    private BrokerClient bc;

    public InputThread(BrokerClient brokerClient) {
        this.bc = brokerClient;
    }

    private String GenerateCheckSum(String fixmsg)
    {
        int sum=0;
        char[] bs = fixmsg.toCharArray();
        for (char b : bs)
            sum += (b == '|') ? 1 : b;
        return String.format("%03d" , sum % 256);
    } 

    @Override
    public void run() {
  
        String msg;
        while (true) {
            System.out.print("Message: ");
            try {
                msg = input.readLine().replace(" ", "");
                if (msg.equalsIgnoreCase("Exit") || msg.equalsIgnoreCase("Quit") || msg.equalsIgnoreCase("Stop"))
                {
                    MsgQueue.add("STOP");
                }
                else {
                    msg = '|' + msg + "|40=2|" + "49=" + bc.getUUID() + "|";
                    String fixMsg = "8=FIX.4.2|9=" + (msg.length()) + msg;
                    fixMsg = checktagvalues(fixMsg);
                    if (fixMsg != null) {
                        fixMsg += "10=" + GenerateCheckSum(fixMsg) + '|';
                        MsgQueue.add(fixMsg);
                    }
                }
            }
            catch ( IOException e) {
               log.error(e.getMessage());
            }
        }
   }

    private String checktagvalues(String fixMsg) {
        String[] tags = fixMsg.split("\\|");
        List<String> requiredTagKeys = new ArrayList<>(Arrays.asList("8", "9", "35", "54", "38", "49", "55", "44", "40", "460")); //Ensure contains buy/sell instrument, market, price
        HashMap<String, String> recievedTags = new HashMap<String, String>();
        for (String tag : tags) {
            String[] tagVal = tag.split("=");
            if (tagVal.length == 2) {
                recievedTags.put(tagVal[0], tagVal[1]);
            }
        }
        if (recievedTags.keySet().containsAll(requiredTagKeys)) {
            // Loop through tags and ensure they have appropriate values :D 
            boolean validate = true;
            for (String key : recievedTags.keySet()) {
                validate = KeySetValueValidate(key, recievedTags.get(key));
                if (!validate)
                    break;
            }
            if (validate) {
                return BuildString(recievedTags);
            }
            return null;
        }
        else {
            log.error("[WARNING] INVALID MESSAGE, please Ensure to add the tags: [35,38,44,54,55,460]");
            return null;
        }
    }

    private String BuildString(HashMap<String, String> recievedTags) {
        String[] Key_array = new String[recievedTags.size()];
        String[] Value_array = new String[recievedTags.size()];

        recievedTags.keySet().toArray(Key_array);
        recievedTags.values().toArray(Value_array);
        String str = "";
        bubbleSort(Key_array, Value_array);
        for (int i = 0 ; i < Key_array.length; i++) {
            str += Key_array[i] + '=' + Value_array[i] + '|';
        }
        return str;
    }

    static void bubbleSort(String[] arr1, String[] arr2) {
        int n = arr1.length;
        String temp = "";
        String temp2 = "";
  
        for(int i = 0; i < n; i++) {
           for(int j=1; j < (n-i); j++) {
            if(  Integer.parseInt(arr1[j-1]) > Integer.parseInt(arr1[j]) ) {
            //   if(arr1[j-1].compareTo(arr1[j]) > 0) {
                 temp = arr1[j-1];
                 temp2 = arr2[j-1];

                 arr1[j-1] = arr1[j];
                 arr2[j-1] = arr2[j];

                 arr1[j] = temp;
                 arr2[j] = temp2;
              }
           }
        }
     }

    private boolean KeySetValueValidate(String key, String value) {
        switch (key)
        {
            case "8": {
                if (value.equals("FIX.4.2"))
                    return true;
                else 
                {
                    log.error("[WARNING] only FIX.4.2 Is supported");
                    return false;
                }
            }
            case "9": {
                try {
                    if (Integer.parseInt(value) > 0)
                        return true;
                    else {
                        log.error("[WARNING] Body Length must be positive!");
                        return false;
                    }
                }
                catch (Exception e) {
                    return false;
                }
            }
            case "35": {
                if (value.equals("D") || value.equals("8"))
                    return true;
                else {
                    log.error("[WARNING] Message Type is not supported!");
                    return false;
                }
               
            }
            case "54": {
                if ((value.equals("1") || value.equals("2")))
                    return true;
                else {
                    log.error("[WARNING] Message Must be Buy or sell is not supported!");
                    return false;
                }
            }
            case "38": {
                try {
                    if (Integer.parseInt(value) > 0)
                        return true;
                    else {
                        log.error("[WARNING] Order Quantity must be positive!");
                        return false;
                    }
                }
                catch (Exception e) {
                    log.error("[WARNING] Order Quantity must be a positive Number!");
                    return false;
                }
            }
            case "44": {
                try {
                    if ( Float.parseFloat(value) > 0)
                        return true;
                    else 
                    {
                        log.error("[WARNING] Price must be a positive Number!");
                        return false;
                    }
                }
                catch (Exception e) {
                    log.error("[WARNING] Price must be a positive Number!");
                    return false;
                }
            }
            case "55": {
                if (!value.isEmpty())
                    return true;
                else 
                    {
                        log.error("[WARNING] Target Name Must be Entered!");
                        return false;
                    }
            }
            case "40": {
                if (value.equals("2"))
                    return true;
                else 
                {
                    log.error("[WARNING] OrdType Is Not Supported!");
                    return false;
                }
            }
            case "460": {
                if (!value.isEmpty())
                    return true;
                else 
                {
                    log.error("[WARNING] product / Instrument Name Must be Entered!");
                    return false;
                }
            }
            // case "52": {
            //     return true;
            // }
            case "10": {
                log.error("[WARNING] Checksum must be calculated don't enter it");
                return false;
            }
            default:
                return true;
            // 9 35 54 38 55 40 10 52
        }
    }

} 