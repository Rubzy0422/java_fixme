package com.wtc.FixmeRouter.RouterChain;

import lombok.Data;

@Data
public class ClientMessage 
{  
    private String client;
    private String Message;
  
    public ClientMessage(String UUID, String Message)  
    {  
        this.client = UUID;  
        this.Message = Message;
    }  
  
}