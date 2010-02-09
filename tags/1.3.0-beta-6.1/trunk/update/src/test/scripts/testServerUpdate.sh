#!/bin/sh

stillSomeJava=true
while [ $stillSomeJava = true ]; do
	echo "INFO: Killing All Java process and waiting for 10 seconds"
	stillSomeJava=false
	killall java && stillSomeJava=true && sleep 10
done

echo "INFO: Removing old artifactory folders"
rm -rf artifactory*

echo "INFO: Unziping delivery files"
unzip ../../1.3.0-final/standalone/target/artifactory-1.3.0-SNAPSHOT.zip
unzip ../../1.3.0-final/cli/target/artifactory-cli-1.3.0-SNAPSHOT.zip

echo "INFO: Running the mock Server"
nohup ~/bin/mock-server.sh 2>&1 > ./mockServer/mockServer.log &

echo "INFO: Starting artifactory"
artifactoryRunFolder=current
pidFile=artifactory.pid
logFile=./current/logs/consoleout.log
nohup ./current/bin/artifactory.sh 2>&1 > $logFile &

echo $! > $pidFile
echo "INFO: Artifactory Runing on PID `cat $pidFile`"
echo "INFO: Waiting for the started message..."
# Waiting for no exception
started=false
nbSeconds=1
# Just wait 2 seconds anyway
sleep 1
while [ $started = false ] && [ $nbSeconds -lt 30 ]; do
	sleep 1
    let "nbSeconds = $nbSeconds + 1"
	hasException=`grep Exception $logFile`
	hasStarted=`grep "Started SelectChannelConnector@0.0.0.0:8081" $logFile`
	if [ -n "$hasStarted" ]; then
		started=true
	fi
done

if [ $started = false ]; then
	echo "ERROR: Jetty server of $artifactoryRunFolder did not start in 30 seconds"
	return 1
fi
if [ -n "$hasException" ]; then
	echo "WARN: Artifactory $artifactoryRunFolder generated Exception messages"
	tail -200 $logFile
	#return 1
fi
echo "INFO: Artifactory version $1 started succesfully. Log file at $logFile"

echo "INFO: doing the import"
./cli/bin/artadmin import ../export/beta5Test/ --timeout 3600 --symlinks --username admin --password password
