#!/bin/sh

# Please make these to links to actual install before running the script
ARTIFACTORY_HOME=/opt/artifactory/current
TOMCAT_HOME=/opt/tomcat/artifactory

curDir=`dirname $0`
curDir=`cd $curDir; pwd`

$curDir/install.sh

tomFiles=$curDir/../misc/Tomcat
cp $tomFiles/artifactory /etc/init.d/artifactory
chmod a+x /etc/init.d/artifactory
cp $tomFiles/setenv.sh $TOMCAT_HOME/bin
chmod a+x $TOMCAT_HOME/bin/setenv.sh
cp $tomFiles/server.xml $TOMCAT_HOME/conf
mkdir -p $TOMCAT_HOME/conf/Catalina/localhost
cp $tomFiles/artifactory.xml $TOMCAT_HOME/conf/Catalina/localhost

chown -R artifactory $TOMCAT_HOME
