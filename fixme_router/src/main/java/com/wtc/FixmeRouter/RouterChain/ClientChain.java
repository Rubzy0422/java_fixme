package com.wtc.FixmeRouter.RouterChain;


public class ClientChain
{ 
    Processor chain; 
  
    public ClientChain(){ 
        buildChain(); 
    } 
  
    private void buildChain(){ 
        chain = new MarketProcessor(new BrokerProcessor(null)); 
    } 
  
    public void process(ClientMessage request) { 
        chain.process(request); 
    }  
}
  
abstract class Processor  
{  
    private Processor processor; 
  
    public Processor(Processor processor){ 
        this.processor = processor; 
    }; 
      
    public void process(ClientMessage request){ 
        if(processor != null) 
            processor.process(request); 
    };  
}  

class BrokerProcessor extends Processor  
{  
    public BrokerProcessor(Processor processor){ 
        super(processor); 
          
    } 
  
    public void process(ClientMessage request)  
    {  
        if (request.getClient().startsWith("B"))  
        {  
            System.out.println("BrokerProcessor UUID: " +   request.getClient());
        }  
        else
        {  
            super.process(request);  
        }  
    }  
}  
  
class MarketProcessor extends Processor  
{  
    public MarketProcessor(Processor processor){ 
        super(processor); 
    } 
  
    public void process(ClientMessage request)  
    {  
        if (request.getClient().startsWith("M"))  
        {  
            System.out.println("MarketProcessor UUID: " + request.getClient());  
        }  
        else
        {  
            super.process(request);  
        }  
    }  
}

