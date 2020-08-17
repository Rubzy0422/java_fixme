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
public class Numbers {
    private int number1;
    private int number2;
    
    public String calculationWanted;
    
    public Numbers(int newnum1, int newnum2, String calcWanted) {
        number1 = newnum1;
        number2 = newnum2;
        calculationWanted = calcWanted;
    }
   
    public int getNumber1() { return number1; }
    public int getNumber2() { return number2; }
    public String getcalculationWanted() { return calculationWanted; }
}



