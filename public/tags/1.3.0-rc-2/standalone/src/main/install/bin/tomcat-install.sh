#!/bin/sh

# Please make these to links to actual install before running the script
ARTIFACTORY_HOME=/opt/artifactory/current
TOMCAT_HOME=/opt/tomcat/artifactory

curUser=`id -nu`
if [ "$curUser" != "root" ]
then
    echo
    echo -e "\033[31m** ERROR: Only root user can install artifactory on Tomcat and set it as a service\033[0m"
    echo
    exit 1
fi

curDir=`dirname $0`
curDir=`cd $curDir; pwd`

$curDir/install.sh

tomFiles=$curDir/../misc/tomcat
cp -i $tomFiles/artifactory /etc/init.d/artifactory
chmod a+x /etc/init.d/artifactory
cp $tomFiles/setenv.sh $TOMCAT_HOME/bin
chmod a+x $TOMCAT_HOME/bin/*
cp $tomFiles/server.xml $TOMCAT_HOME/conf
mkdir -p $TOMCAT_HOME/conf/Catalina/localhost
cp $tomFiles/artifactory.xml $TOMCAT_HOME/conf/Catalina/localhost
chown -R artifactory $TOMCAT_HOME/conf
chown -R artifactory $TOMCAT_HOME/webapps
mv $TOMCAT_HOME/logs $TOMCAT_HOME/logs.orig
ln -s /var/log/artifactory $TOMCAT_HOME/logs
if [ -d $TOMCAT_HOME/work ];then
	chown -R artifactory $TOMCAT_HOME/work
fi
if [ ! -d $TOMCAT_HOME/temp ];then
        mkdir $TOMCAT_HOME/temp  
fi
chown -R artifactory $TOMCAT_HOME/temp	
