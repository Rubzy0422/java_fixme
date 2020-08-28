package com.wtc.FixmeRouter.RouterChain;

import java.nio.channels.SocketChannel;

import lombok.Data;

@Data
public class ClientMessage 
{  
    private String client;
    private String Message;
    private ClientChain cc;
    private SocketChannel sc;

    public ClientMessage(ClientChain cc, String UUID, String Message, SocketChannel sc)  
    {
        this.cc = cc;  
        this.client = UUID;  
        this.Message = Message;
        this.sc = sc;
    }  
  
}