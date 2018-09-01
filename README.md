# NetSword
  This is a proxy based on Socks5 protocol implemented in java. As we all known, a proxy contains 2 parts: local and server. local take over request and relay to server, server send the request to remote and send data received from remote back to local. In NetSword, the two parts are implemented into one runnable jar. And data transmits between local and server are proguarded for some purpose(THE GREAT WALL). This project depends on [CloudLadderDownload](https://github.com/SummerOak/CloudLadder), All protocol related business are handled in that project. And this project only an UI implement.
  
## Getting started

  1. Firstly, download [release](https://github.com/SummerOak/NetSword/tree/master/release) folder and config setting.txt properly;
  2. Run command "java -jar NetSword.jar s" to start the server, If you are running it on Ubuntu and you want it run permanently(such as restart automatically after a crash), you can add netsword.service to you system and use systemctl system command to start it;
  3. Before start the local part, You need make sure socks5 has open. This step can be skipped if you are running it on MAC because netsword.sh do it for you; 
  4. Finally, run command "java -jar NetSword.jar c" to start the local;
  5. If you start up NetSword sucessfully, when you open a browser and open a website you can see all the net request like this:
  ![](https://github.com/SummerOak/NetSword/blob/master/local.png?raw=true)
