package com.chedifier.netsword.ssl.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.chedifier.netsword.ssl.Configuration;
import com.chedifier.netsword.ssl.MyHandshakeCompletedListener;
import com.chedifier.netsword.ssl.SocketIO;

public class Client {
    private SSLContext sslContext;  
    private int port = 0;  
    private String host = null;  
    private SSLSocket socket;  
    private Properties p;  
      
    public Client(){  
        try {  
            p = Configuration.getConfig();  
              
            sslContext = Auth.getSSLContext();  
            SSLSocketFactory factory = (SSLSocketFactory) sslContext.getSocketFactory();    
            socket = (SSLSocket)factory.createSocket();   
            String[] pwdsuits = socket.getSupportedCipherSuites();  
              
            socket.setEnabledCipherSuites(pwdsuits);  
              
            socket.setUseClientMode(true);  
            
            host = p.getProperty("serverHost");
            port = Integer.valueOf(p.getProperty("serverListenPort","0"));
              
            SocketAddress address = new InetSocketAddress(host, port);  
            socket.connect(address, 0);  
              
            MyHandshakeCompletedListener listener = new MyHandshakeCompletedListener();  
            socket.addHandshakeCompletedListener(listener);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
      
    public void request(){  
        try{  
            String encoding = p.getProperty("socketStreamEncoding");  
              
            DataOutputStream output = SocketIO.getDataOutput(socket);  
            String user = "name";  
            byte[] bytes = user.getBytes(encoding);  
            int length = bytes.length;  
            int pwd = 123;  
              
              
            output.write(length);  
            output.write(bytes);  
            output.write(pwd);  
              
            DataInputStream input = SocketIO.getDataInput(socket);  
            length = input.readShort();  
            bytes = new byte[length];  
            input.read(bytes);  
              
        }catch(Exception e){  
            e.printStackTrace();  
        }finally{  
            try {  
                socket.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
      
    public static void main(String[] args){  
        Client client = new Client();  
        client.request();  
    } 
}
