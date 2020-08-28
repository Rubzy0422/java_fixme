package com.wtc.FixmeBroker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import lombok.extern.log4j.Log4j;

/**
 * @author Ruben
 */
@Log4j
public class FixmeBroker {

    public FixmeBroker() {

            System.out.println("Please Select a host:");
            try {
                BufferedReader buffreader = new BufferedReader(new InputStreamReader(System.in));
                String host = buffreader.readLine().trim();
                if (!host.isEmpty())
                {
                    new BrokerClient(host);
                }
            }
            catch (UnknownHostException ex)
            {
                log.error(ex.getMessage());
            }
            catch (IOException ex)
            {
                log.error(ex.getMessage());
            }
    }

    public static void main(String args[]) throws UnknownHostException, IOException {
        new FixmeBroker();
    }

}

