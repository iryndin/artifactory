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
javaVersion=`$JAVACMD -version 2>&1 | grep "java version" | egrep -e "1\.[56]"`
if [ -z "$javaVersion" ]; then
    $JAVACMD -version
    echo "** ERROR: The Java of $JAVACMD version is not 1.5 or 1.6"
    exit 1
fi

if [ -z "$ARTIFACTORY_HOME" ]; then
    ARTIFACTORY_HOME=`dirname "$0"`/..
fi

JAVA_OPTIONS="$JAVA_OPTIONS -Djetty.home=$ARTIFACTORY_HOME -Dartifactory.home=$ARTIFACTORY_HOME"
#Command for discovering all the jar files under lib, to specify the Classpath. Unnecessary at the moment.
#CP=`find $ARTIFACTORY_HOME/lib -name '*.jar'|xargs -l1 -iW echo -n 'W:'`

echo "Runing: exec $JAVACMD $JAVA_OPTIONS -cp \"$ARTIFACTORY_HOME/artifactory.jar\" org.artifactory.standalone.main.Main $@"
exec "$JAVACMD" $JAVA_OPTIONS -cp "$ARTIFACTORY_HOME/artifactory.jar" org.artifactory.standalone.main.Main "$@"