package com.wtc.FixmeMarket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

/**
 * @author Ruben
 */
public class FixmeMarket {
    public FixmeMarket() {
        System.out.println("Please Select a host:");
        try {
            BufferedReader buffreader = new BufferedReader(new InputStreamReader(System.in));
            String host = buffreader.readLine().trim();
            System.out.println("Please Provide a MarketID followed by Instruments (defined as [Name:Price:StockCount] splitted by spaces)");
            MarketObj Marketclient = createMarket(buffreader);

            
            if (!host.isEmpty() && Marketclient != null && Marketclient.Instruments.size() > 0)
            {
                System.out.println(Marketclient);
                new MarketClient(host, Marketclient);
            }
        }
        catch (UnknownHostException ex)
        {
            System.out.println(ex.getMessage());
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }
        // System.exit(0);
    }

  
    private MarketObj createMarket(BufferedReader buffreader) throws IOException{
        try {
            MarketObj marketobj = null;
            String Input = buffreader.readLine().trim();
            if (!Input.isEmpty()) {
                String[] sMarket = Input.trim().split(" ");
                String MarketName = sMarket[0];
                marketobj = new MarketObj(MarketName);
                for (int i = 1; i < sMarket.length; i ++)  {
                    String MarketParts[] = sMarket[i].split(";");
                    if (MarketParts.length == 3) {
                        String MarketInstrumentName = MarketParts[0];
                        float MarketInstrumentPrice = Math.round(Float.parseFloat(MarketParts[1]) * 100) / 100; // Accepts 0.00 as a number
                        int  MarketInstrumentShares = Integer.parseInt(MarketParts[2]);
                        marketobj.AddNewInstrument( MarketInstrumentName, MarketInstrumentPrice, MarketInstrumentShares);
                    }
                }
                // Market be split into 3 bits 
            }
            return marketobj;
        }
        catch (NumberFormatException NFE) {
            System.out.println(NFE.getMessage());
            return null;
        }
    }

    public static void main(String args[]) {
        new FixmeMarket();
    }
       
}