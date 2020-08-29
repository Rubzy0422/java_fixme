package com.wtc.FixmeMarket;

import lombok.Data;

@Data
public class Instrument {
	String Name;
    float Price;
    int Shares;
    int MaxShares;

    public Instrument(String Name, float Price, int Shares) {
        this.Name = Name;
        this.Price = Price;
        this.Shares = Shares;
        this.MaxShares = 1000;
	}

    // private int GenerateMaxShares() {
    //     int randnum = (int) Math.random() * 1000;
    //     return (randnum > 0) ? randnum : 1;
    // }
}
