#!/bin/sh

cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

DIRNAME=`dirname $0`

LOG4J_CONF=`cd $DIRNAME/../config/;pwd`
LOG4J_CONF="$LOG4J_CONF/log4j.properties"

JAR=$DIRNAME/../dependency-viewer.jar

if $cygwin ; then
    [ -n "$LOG4J_CONF" ] &&
        LOG4J_CONF=`cygpath --windows "$LOG4J_CONF"`
fi

JAVA_OPTS="$JAVA_OPTS -Dconfig.file.path=$DIRNAME/../config/viewer.properties -Dlog4j.configuration=file:/$LOG4J_CONF"

if [ -n "$MAVEN_HOME" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dmaven.home=$MAVEN_HOME"
else
  if [ -n "$M2_HOME" ]; then
    JAVA_OPTS="$JAVA_OPTS -Dmaven.home=$M2_HOME"
  fi
fi

#JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

java $JAVA_OPTS -cp $JAR org.jfrog.maven.viewer.Main $*
