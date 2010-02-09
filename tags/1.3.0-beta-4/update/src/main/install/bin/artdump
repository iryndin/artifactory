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

echo Verifying that we are runing java 5 or 6u4
javaVersion=`$JAVACMD -version 2>&1 | egrep "java version" | egrep -e "1\.[56]"`
if [ -z "$javaVersion" ]; then
    $JAVACMD -version
    echo "** ERROR: The Java of $JAVACMD version is not 1.5 or 1.6"
    exit 1
else
    # Check that if version 6 the update is 4 or above for JAXB 2.1
    updateRevision=`echo $javaVersion | sed -ne "s/.*1\.6_\(.*\).*/\1/p;"`
    if [ -n "$updateRevision" ] && [ "$updateRevision" -lt "04" ]; then
      $JAVACMD -version
      echo "** ERROR: The Java of $JAVACMD version is 1.6 but update below 4"
      echo "** ERROR: Artifactory uses JAXB 2.1 which is included in update 4 and above"
      exit 1
    fi
fi

JAVA_OPTIONS="-Xmx500m"
UPDATE_DIR=`dirname $0`/..
UPDATE_DIR=`cd $UPDATE_DIR && pwd`

echo Starting Artifactory Update Manager
exec "$JAVACMD" $JAVA_OPTIONS -cp "$UPDATE_DIR/artifactory-update.jar:$UPDATE_DIR/lib/*" org.artifactory.update.ArtifactoryUpdate "$@"
