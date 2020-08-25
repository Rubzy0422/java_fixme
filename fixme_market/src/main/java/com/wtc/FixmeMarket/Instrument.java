package com.wtc.FixmeMarket;

import lombok.Data;

@Data
public class Instrument {
	String Name;
    float Price;
    int Shares;

    public Instrument(String Name, float Price, int Shares) {
        this.Name = Name;
        this.Price = Price;
        this.Shares = Shares;
	}
}
