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
public class AddNumbers implements BrokerChain {
    private BrokerChain nextInChain;

    @Override
    public void setNext(BrokerChain nextChain) {
        nextInChain = nextChain;
    }

    @Override
    public void calculate(Numbers request) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    if (request.getcalculationWanted().equals("add")) {
        System.out.println(
                request.getNumber1() + " + " + request.getNumber2() + " = " + ( request.getNumber1() + request.getNumber2())
        );
    }
    else {
        nextInChain.calculate(request);
    }
    }
}




