package com.wtc.FixmeRouter.RouterChain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.wtc.FixmeRouter.FixmeRouter;

import lombok.extern.log4j.Log4j;

public class ClientChain
{ 
    Processor chain; 
  
    public ClientChain(){ 
        buildChain(); 
    } 
  
    private void buildChain(){ 
        chain = new MarketProcessor(new BrokerProcessor(null)); 
    } 
  
    public void ForwardChain() {
        chain = new ChecksumValidateProcessor(new SetDestinationProcessor(new DispatchMessageProcessor(null)));
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
            request.getCc().ForwardChain();
            request.getCc().process(request);
            // super.process(request);
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
            if (!request.getMessage().isEmpty())
            {
                String[] MessageParts = request.getMessage().split(" ");
                if (MessageParts[0].equals("REGISTER-MARKET")) {
                    FixmeRouter.getMarketNameLinks().put(MessageParts[1], request.getSc());
                    super.process(request);
                }
                else {
                    request.getCc().ForwardChain();
                    request.getCc().process(request);
                }
            }  
           
        }  
        else
        {  
            super.process(request);  
        }  
    }  
}

@Log4j
class ChecksumValidateProcessor extends Processor {

    public ChecksumValidateProcessor(Processor processor) {
        super(processor);
    }

    public void process(ClientMessage request)  
    {  

        String Check = request.getMessage().substring(request.getMessage().length() - 4 ,request.getMessage().length() - 1);
        
        String body = request.getMessage().substring(0, request.getMessage().length() - 7 );
        if (checksumsMatch(body, Check))
        {
            super.process(request);
        }
        else {
            log.info("Invalid Checksum provided for FIX message, Ignoring Request");
        }
    
    }

    private boolean checksumsMatch(String body, String check) {
        int sum=0;
        char[] bs = body.toCharArray();
        for (char b : bs)
            sum += (b == '|') ? 1 : b;
        int checksum = sum % 256;
        String BodyChecksum = String.format("%03d" , checksum);
        return  BodyChecksum.equals(check);
    }
}

@Log4j
class SetDestinationProcessor extends Processor {

    public SetDestinationProcessor(Processor processor) {
        super(processor);
    }

    public void process(ClientMessage request)  
    {  
        // MARKET NAME Will be the MARKETS ID
        if (request.getClient().startsWith("M"))
        {
            // So this need to route to scl
            String[] tags = request.getMessage().split("\\|");
            String ForwardName = "";
            for (String tag : tags) {
                if (tag.split("=")[0].equals("49"))
                {
                    ForwardName = tag.split("=")[1];
                    break;
                }    
            }
            if (FixmeRouter.getScl().containsKey(ForwardName))
            {
                SocketChannel sc = FixmeRouter.getScl().get(ForwardName);
                request.setSc(sc);
                super.process(request);
            }
            else {
                try {
                    String msg = "[SERVER WARNING] Could not Find this Broker";
                    request.getSc().write(ByteBuffer.wrap(msg.getBytes()));
                }
                catch (IOException ioe)
                {
                    log.error("Could not write to client:" + ioe.getMessage());
                }
            }
        }
        else if (request.getClient().startsWith("B"))
        {
            // So this routes to Markets
            String[] tags = request.getMessage().split("\\|");
            String ForwardName = "";
            for (String tag : tags) {
                if (tag.split("=")[0].equals("55"))
                {
                    ForwardName = tag.split("=")[1];
                    break;
                }    
            }
            if (FixmeRouter.getMarketNameLinks().containsKey(ForwardName))
            {
                SocketChannel sc = FixmeRouter.getMarketNameLinks().get(ForwardName);
                request.setSc(sc);
                super.process(request);
            }
            else {
                try {
                    String msg = "[SERVER WARNING] Could not Find this market";
                    request.getSc().write(ByteBuffer.wrap(msg.getBytes()));
                }
                catch (IOException ioe)
                {
                    log.error("Could not write to client:" + ioe.getMessage());
                }
            }
        }
    }
}

@Log4j
class DispatchMessageProcessor extends Processor {

    public DispatchMessageProcessor(Processor processor) {
        super(processor);
    }

    public void process(ClientMessage request)  
    {  
        try {
            request.getSc().write(ByteBuffer.wrap(request.getMessage().getBytes()));
        }
        catch (IOException ioe) {
            log.error("Could not forward message to Destination: " + ioe.getMessage());
        }
        super.process(request);
    }
}
