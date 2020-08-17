package com.wtc.FixmeRouter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.log4j.Log4j;

/**
 * @author Ruben
 */

// Create 2 Executor Services (1. Market on Port 5001, 2. For Broker on Port 5000)
@Log4j
public class FixmeRouter {

    public FixmeRouter() {
        // Listen for Market
        Runnable MarketServer = new Runnable(){
            public void run(){
                new RouterServer("localhost", 5001);
            }
        };
        // Thread MarketThread = new Thread(MarketServer);
        // MarketThread.start();
        
        // Listen For Broker
        
        Runnable BrokerServer = new Runnable(){
            public void run(){
                new RouterServer("localhost", 5000);
            }
        };
        //Thread BrokerThread = new Thread(BrokerServer);
        //BrokerThread.start();

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
