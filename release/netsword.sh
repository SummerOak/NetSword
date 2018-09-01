

function getProperty {
   KEY=$1
   VALUE=`cat ./settings.txt | grep "$KEY" | cut -d'=' -f2`
   echo $VALUE
}

function enableSocksProxy {

   networksetup -listallnetworkservices | while read networkservice; do

      echo "turn $networkservice proxy on"
      networksetup -setsocksfirewallproxy $networkservice 127.0.0.1 $PORT > /dev/null 2>&1
      networksetup -setsocksfirewallproxystate $networkservice on > /dev/null 2>&1

   done
}

function disableSocksProxy {

   networksetup -listallnetworkservices | while read networkservice; do

      echo "turn $networkservice proxy off"
      networksetup -setsocksfirewallproxy $networkservice "" "" > /dev/null 2>&1
      networksetup -setsocksfirewallproxystate networkservice off > /dev/null 2>&1

   done
}

IS_SERVER=$(getProperty "is_server")
for arg in "$@" 
do
   if [ "$arg"x = "s"x ]; then
      IS_SERVER="1"
      break
   else
      if [ "$arg"x = "c"x ]; then
         IS_SERVER="0"
         break
      fi
   fi
done

if [ "$IS_SERVER"x = "1"x ]; then
   PORT=$(getProperty "server_port")
   echo "runing server on port: $PORT"
   nohup java -jar NetSword.jar $* > nohub.log
else
   PORT=$(getProperty "local_port")
   echo "runing client on port: $PORT"
   enableSocksProxy
   java -jar NetSword.jar $*  > /dev/null 2>&1
   disableSocksProxy
fi





