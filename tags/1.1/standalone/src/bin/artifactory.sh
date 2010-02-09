#!/bin/sh

pushd .
cd `dirname $0`/..
exec $JAVA_HOME/bin/java.exe -Dlog4j.configuration=file:etc/log4j.properties -jar artifactory.jar
popd
