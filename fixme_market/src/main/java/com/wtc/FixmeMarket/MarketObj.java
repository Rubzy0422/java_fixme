package com.wtc.FixmeMarket;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.extern.log4j.Log4j;

@Data
@Log4j
public class MarketObj {
    String Name;
    List<Instrument> Instruments;

    MarketObj(String Name) {
        this.Name = Name;
        Instruments = new ArrayList<Instrument>();
    }

    public void AddNewInstrument(String Name, float Price, int Shares) {
        // If instrument in Market then Update Instrument
        boolean inInstruments = false;
        for (Instrument ins : this.Instruments){
            if (ins.getName().equals(Name)) {
                ins.setPrice(Price);
                ins.setShares(Shares);
                inInstruments = true;
                break; 
            }
        }
        // If not in Market
        if (!inInstruments) {
            Instruments.add(new Instrument(Name, Price, Shares));
        }
    }
}