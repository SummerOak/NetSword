# What's it
  It's a proxy based on Socks5 protocol and implemented in java. As we all known, a proxy contains 2 parts: local and server. local take over requests and relay it to server, server send these requests to remote and relay data received from remote back to local. In this proxy, these two parts are implemented into one runnable jar, and data transmits between local and server are disguised for some purpose(THE GREAT WALL). This project depends on [CloudLadder](https://github.com/SummerOak/CloudLadder), all protocol related business are handled in that project. This project is just an UI implement.
  
# How to use it

  1. Download [NetSword](https://github.com/SummerOak/NetSword/releases/download/v1.0/NetSword.zip) and config setting.txt properly;
  2. Run command "java -jar NetSword.jar s" to start the server, If you are running it on Ubuntu and you want it run permanently(such as restart automatically after a crash), you can add netsword.service to /etc/systemd/system and start it using systemctl command;
  3. Before start the local part, You need to enable the socks5 proxy of system/browser: 
  
      1) For users of Mac, this step can be skipped since netsword.sh will do it for you, or you can turn socks5 in System Preferences - NetWork - Advanced - Proxies; 
      2) For users of windows, you can't enable socks5 proxy in system settings, so you need a proxy extentions for your browser, such as [SwitchyOmega](https://github.com/FelisCatus/SwitchyOmega) for Google Chrome&Firefox; 
      
  4. After enable socks5 proxy, run command "java -jar NetSword.jar c" to start the local;
  5. If you start up NetSword sucessfully, open a website you can view net requests in NetSword like below:
  ![](https://github.com/SummerOak/NetSword/blob/master/local.png?raw=true)
