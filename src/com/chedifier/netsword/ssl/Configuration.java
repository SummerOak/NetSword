package com.chedifier.netsword.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
	private static Properties config;  
          
    public static Properties getConfig(){  
        try{  
            if(null == config){  
                File configFile = new File("./ssl/conf/conf.properties");  
                if(configFile.exists() && configFile.isFile()  
                        && configFile.canRead()){  
                    InputStream input = new FileInputStream(configFile);  
                    config = new Properties();  
                    config.load(input);  
                }  
            }  
        }catch(Exception e){  
            e.printStackTrace();
            config = new Properties();  
            config.setProperty("protocol", "TLSV1");  
            config.setProperty("serverCer", "./ssl/cert/server.jks");  
            config.setProperty("serverCerPwd", "1234sp");  
            config.setProperty("serverKeyPwd", "1234kp");  
            config.setProperty("serverTrustCer", "./ssl/cert/serverTrust.jks");  
            config.setProperty("serverTrustCerPwd", "1234sp");  
            config.setProperty("clientCer", "./ssl/cert/client.jks");  
            config.setProperty("clientCerPwd", "1234sp");  
            config.setProperty("clientKeyPwd", "1234kp");  
            config.setProperty("clientTrustCer", "./ssl/cert/clientTrust.jks");  
            config.setProperty("clientTrustCerPwd", "1234sp");  
            config.setProperty("serverHost", "localhost");
            config.setProperty("serverListenPort", "16677");  
            config.setProperty("serverThreadPoolSize", "5");  
            config.setProperty("serverRequestQueueSize", "10");  
            config.setProperty("socketStreamEncoding", "UTF-8");  
        }  
        return config;  
    } 
}
