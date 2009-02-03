#!/bin/sh

pushd .
cd `dirname $0`/..

if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD=`which java 2> /dev/null `
    if [ -z "$JAVACMD" ] ; then
        JAVACMD=java
    fi
fi

exec "$JAVACMD" -Dlog4j.configuration=file:etc/log4j.properties -jar artifactory.jar "$@"
popd
