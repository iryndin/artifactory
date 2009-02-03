#!/bin/sh

if [ -n "$JAVA_HOME"  ] ; then
   if [ -x "$JAVA_HOME/bin/java" ] ; then
       JAVACMD="$JAVA_HOME/bin/java"
   else
       echo "** ERROR: java under JAVA_HOME=$JAVA_HOME cannot be executed"
       exit 1
   fi
else
   JAVACMD=`which java 2> /dev/null `
   if [ -z "$JAVACMD" ] ; then
       JAVACMD=java
   fi
fi

# Verify that it is java 6
javaVersion=`$JAVACMD -version 2>&1 | grep "java version" | grep "1.6"`
if [ -z "$javaVersion" ]; then
    $JAVACMD -version
    echo "** ERROR: The Java of $JAVACMD version is not 1.6"
    exit 1
fi

if [ -z "$ARTIFACTORY_HOME" ]; then
    ARTIFACTORY_HOME=`dirname "$0"`/..
fi

JAVA_OPTIONS="$JAVA_OPTIONS -Djetty.home=$ARTIFACTORY_HOME -Dartifactory.home=$ARTIFACTORY_HOME"
JAVA_OPTIONS="$JAVA_OPTIONS -Dlog4j.configuration=file:$ARTIFACTORY_HOME/etc/log4j.properties"

exec "$JAVACMD" $JAVA_OPTIONS -cp "$ARTIFACTORY_HOME/artifactory.jar:$ARTIFACTORY_HOME/lib/*" org.artifactory.webapp.main.Main "$@"
