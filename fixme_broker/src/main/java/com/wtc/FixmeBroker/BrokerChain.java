/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wtc.FixmeBroker;

/**
 *
 * @author Ruben
 */
public interface BrokerChain {
//    Objects receive data
    public void setNext(BrokerChain nextChain);
    
    public void calculate(Numbers request);
}



