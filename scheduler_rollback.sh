


JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home"

APP_HOME=/Users/xiaojing/code2/code/DistributedTrans
APP_MAINCLASS=com.xiaojing.distributed.SchedulerRollback
APP_NAME=ScheduleRollback


XMS='-Xms1024M'
XMX='-Xms2048M'
XMN='-Xms512M'

X_PERM_SIZE="-XX:PermSize=64M"
X_MAX_PERM_SIZE="-XX:MaxPermSize=128M"
X_SERVIVOR_RATIO="-XX:SurvivorRatio=4"

JVM_OPTS=""

psid=0
 
checkpid() {
   javaps=`$JAVA_HOME/bin/jps -l | grep $APP_MAINCLASS`
 
   if [ -n "$javaps" ]; then
      psid=`echo $javaps | awk '{print $1}'`
   else
      psid=0
   fi
}



daemon(){
  while [ true ]; do
    sleep 1
    checkpid
    if [[ $psid -eq 0 ]]; then
      start
    fi
  done
}

kill_loop(){
  while [ true ]; do
    sleep 4
    stop
  done
}



start() {
   checkpid
 
   if [ $psid -ne 0 ]; then
      echo "================================"
      echo "warn: $APP_MAINCLASS already started! (pid=$psid)"
      echo "================================"
   else
      echo -n "Starting $APP_MAINCLASS ..."
      nohup $JAVA_HOME/bin/java $JAVA_OPTS  -Djava.ext.dirs=$APP_HOME/target:$JAVA_HOME/jre/lib/ext   $APP_MAINCLASS  >> scheduler.out 2>&1 &
      checkpid
      if [ $psid -ne 0 ]; then
         echo "(pid=$psid) [OK]"
      else
         echo "[Failed]"
      fi
   fi
}

stop() {
   checkpid
 
   if [ $psid -ne 0 ]; then
      echo -n "Stopping $APP_MAINCLASS ...(pid=$psid) "
      kill -9 $psid
      if [ $? -eq 0 ]; then
         echo "[OK]"
      else
         echo "[Failed]"
      fi
 
      checkpid
      if [ $psid -ne 0 ]; then
         stop
      fi
   else
      echo "================================"
      echo "warn: $APP_MAINCLASS is not running"
      echo "================================"
   fi
}


info() {
   echo "System Information:"
   echo "****************************"
   echo `head -n 1 /etc/issue`
   echo `uname -a`
   echo
   echo "JAVA_HOME=$JAVA_HOME"
   echo `$JAVA_HOME/bin/java -version`
   echo
   echo "APP_HOME=$APP_HOME"
   echo "APP_MAINCLASS=$APP_MAINCLASS"
   echo "****************************"
}

status() {
   checkpid
 
   if [ $psid -ne 0 ];  then
      echo "$APP_MAINCLASS is running! (pid=$psid)"
   else
      echo "$APP_MAINCLASS is not running"
   fi
}


case "$1" in
   'start')
      start
      ;;
   'stop')
     stop
     ;;
   'restart')
     stop
     start
     ;;
   'status')
     status
     ;;
   'info')
     info
     ;;
   'daemon')
     daemon
     ;;
  'kill')
     kill_loop
     ;;
  *)
     echo "Usage: $0 {start|stop|restart|status|info}"
     exit 1
esac
exit 0
