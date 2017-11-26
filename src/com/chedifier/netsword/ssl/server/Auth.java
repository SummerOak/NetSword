package com.chedifier.netsword.ssl.server;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.chedifier.netsword.ExceptionHandler;
import com.chedifier.netsword.ssl.Configuration;

public class Auth {

	public static SSLContext getSSLContext() {
		Properties p = Configuration.getConfig();
		String protocol = p.getProperty("protocol");
		String serverCer = p.getProperty("serverCer");
		String serverCerPwd = p.getProperty("serverCerPwd");
		String serverKeyPwd = p.getProperty("serverKeyPwd");

		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(serverCer), serverCerPwd.toCharArray());

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, serverKeyPwd.toCharArray());
			KeyManager[] kms = keyManagerFactory.getKeyManagers();

			TrustManager[] tms = null;
			if (Configuration.getConfig().getProperty("authority").equals("2")) {
				String serverTrustCer = p.getProperty("serverTrustCer");
				String serverTrustCerPwd = p.getProperty("serverTrustCerPwd");

				keyStore = KeyStore.getInstance("JKS");
				keyStore.load(new FileInputStream(serverTrustCer), serverTrustCerPwd.toCharArray());

				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
				trustManagerFactory.init(keyStore);
				tms = trustManagerFactory.getTrustManagers();
			}
			SSLContext sslContext = SSLContext.getInstance(protocol);
			sslContext.init(kms, tms, null);
			
			return sslContext;
		}catch (Throwable t) {
			ExceptionHandler.handleException(t);
		}

		return null;
	}
}
