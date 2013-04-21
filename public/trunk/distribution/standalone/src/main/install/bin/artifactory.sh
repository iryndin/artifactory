#!/bin/bash
#
# Startup script for Artifactory in Tomcat Servlet Engine
#

#
errorArtHome() {
    echo
    echo -e "\033[31m** $1\033[0m"
    echo
    exit 1
}

checkArtHome() {
    if [ -z "$ARTIFACTORY_HOME" ] || [ ! -d "$ARTIFACTORY_HOME" ]; then
        errorArtHome "ERROR: Artifactory home folder not defined or does not exists at $ARTIFACTORY_HOME"
    fi
}

checkTomcatHome() {
    if [ -z "$TOMCAT_HOME" ] || [ ! -d "$TOMCAT_HOME" ]; then
        errorArtHome "ERROR: Tomcat Artifactory folder not defined or does not exists at $TOMCAT_HOME"
    fi
    export CATALINA_HOME="$TOMCAT_HOME"
}

checkArtUser() {
    # User under which tomcat will run
    if [ -z "$ARTIFACTORY_USER" ]; then
        # Will run as current user (may be root!!!)
        ARTIFACTORY_USER=$USER
    fi
}

createLogsLink() {
    if [ ! -L "$TOMCAT_HOME/logs" ];
    then
        mkdir -p $ARTIFACTORY_HOME/logs/catalina || errorArtHome "Could not create dir $ARTIFACTORY_HOME/logs/catalina"
        ln -s $ARTIFACTORY_HOME/logs/catalina $TOMCAT_HOME/logs || \
            errorArtHome "Could not create link from $TOMCAT_HOME/logs to $ARTIFACTORY_HOME/logs/catalina"
    fi
}

start() {
    export CATALINA_OPTS="$JAVA_OPTIONS -Dartifactory.home=$ARTIFACTORY_HOME -Dfile.encoding=UTF8"

    [ -x $TOMCAT_HOME/bin/catalina.sh ] || chmod +x $TOMCAT_HOME/bin/*.sh

    if [ -z "$@" ];
    then
        #default to catalina.sh run
        $TOMCAT_HOME/bin/catalina.sh run
    else
        #create $ARTIFACTORY_HOME/run
        if [ -n "$ARTIFACTORY_PID" ];
        then
            mkdir -p $(dirname "$ARTIFACTORY_PID") || \
                errorArtHome "Could not create dir for $ARTIFACTORY_PID";
        fi
        $TOMCAT_HOME/bin/catalina.sh "$@"
    fi
}

check() {
    if [ -f $ARTIFACTORY_PID ]; then
        echo "Artifactory is running, on pid="`cat $ARTIFACTORY_PID`
        echo ""
        exit 0
    fi

    echo "Checking arguments to Artifactory: "
    echo "ARTIFACTORY_HOME     =  $ARTIFACTORY_HOME"
    echo "ARTIFACTORY_USER     =  $ARTIFACTORY_USER"
    echo "TOMCAT_HOME          =  $TOMCAT_HOME"
    echo "ARTIFACTORY_PID      =  $ARTIFACTORY_PID"
    echo "JAVA_HOME            =  $JAVA_HOME"
    echo "JAVA_OPTIONS         =  $JAVA_OPTIONS"
    echo

    exit 1
}

#
artBinDir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export ARTIFACTORY_HOME="$(cd "$(dirname "${artBinDir}")" && pwd)"
artDefaultFile="$artBinDir/artifactory.default"

. $artDefaultFile || errorArtHome "ERROR: $artDefaultFile does not exist or not executable"

if [ "x$1" = "xcheck" ]; then
    check
fi

checkArtHome
checkTomcatHome
checkArtUser
createLogsLink

start "$@"