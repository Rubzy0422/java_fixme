package com.wtc.FixmeBroker;

/**
 * @author Ruben
 */
public class FixmeBroker {

    public FixmeBroker() {
        // BrokerChain calc1 = new AddNumbers();
        // BrokerChain calc2 = new SubtractNumbers();
        // BrokerChain calc3 = new MultiplyNumbers();
        // BrokerChain calc4 = new DivideNumbers();
        
        // calc1.setNext(calc2);
        // calc2.setNext(calc3);
        // calc3.setNext(calc4);
        
        // Numbers request = new Numbers(4, 2, "Add");
        
        // calc1.calculate(request);
        new BrokerClient("localhost");
    }

    public static void main(String args[]) {
        new FixmeBroker();
    }

}

