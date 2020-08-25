package com.wtc.FixmeRouter;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.log4j.Log4j;

/**
 * @author Ruben
 */

@Log4j
public class FixmeRouter {
    static HashMap<String, SocketChannel> scl;
    
    public FixmeRouter() {
        scl = new HashMap<>();
        // Listen for Market
        Runnable MarketServer = new Runnable(){
            public void run(){
                new RouterServer("localhost", 5001);
            }
        };
        Runnable BrokerServer = new Runnable(){
            public void run(){
                new RouterServer("localhost", 5000);
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(20);
        try {
            pool.execute(MarketServer);
            pool.execute(BrokerServer);
        }
        catch (NullPointerException npe)
        {
            log.error(npe.getMessage());
        }
        catch(RejectedExecutionException Ree)
        {
            log.error(Ree.getMessage());
        }
    }

    public static void main(String args[]) {
        new FixmeRouter();
    }

}



// https://www.geeksforgeeks.org/chain-responsibility-design-pattern/