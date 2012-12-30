#!/usr/bin/env bash

if [ -n "$1" ]; then
    ARTIFACTORY_USER=$1
fi
if [ -z "$ARTIFACTORY_USER" ]; then
    ARTIFACTORY_USER=artifactory
fi

echo
echo "Installing artifactory as a Unix service that will run as user ${ARTIFACTORY_USER} "

errorArtHome() {
    echo
    echo -e "** ** ERROR: $1 "
    echo
    exit 1
}

curUser=
if [ -x "/usr/xpg4/bin/id" ]
then
    curUser=`/usr/xpg4/bin/id -nu`
else
    curUser=`id -nu`
fi
if [ "$curUser" != "root" ]
then
    errorArtHome "Only root user can install artifactory as a service"
fi

if [ "$0" = "." ] || [ "$0" = "source" ]; then
    errorArtHome "Cannot execute script with source $0"
fi

curdir="`dirname $0`" || errorArtHome "Cannot find ARTIFACTORY_HOME=$curdir/.."
curdir="`cd $curdir; pwd`" || errorArtHome "Cannot finddefau ARTIFACTORY_HOME=$curdir/.."
cd "$curdir/.." || errorArtHome "Cannot go to ARTIFACTORY_HOME=$curdir/.."

ARTIFACTORY_HOME="`pwd`"
if [ -z "$ARTIFACTORY_HOME" ] || [ "$ARTIFACTORY_HOME" = "/" ]; then
    errorArtHome "ARTIFACTORY_HOME cannot be the root folder"
fi

echo
echo "Installing artifactory with home ${ARTIFACTORY_HOME}"

echo -n "Creating user ${ARTIFACTORY_USER}..."
artifactoryUsername=`getent passwd ${ARTIFACTORY_USER} | awk -F: '{print $1}'`
if [ "$artifactoryUsername" = "${ARTIFACTORY_USER}" ]; then
    echo -n "already exists..."
else
    echo -n "creating..."
    useradd -m -s `which bash` ${ARTIFACTORY_USER}
    if [ ! $? ]; then
        echo -e "** ERROR"
        echo
        exit 1
    fi
fi
echo " DONE"

echo
echo -n "Checking configuration link and files in /etc/artifactory..."
if [ -L ${ARTIFACTORY_HOME}/etc ]; then
    echo -n "already exists, no change..."
else
    echo
    echo -n "Moving configuration dir etc to etc.original"
    mv ${ARTIFACTORY_HOME}/etc ${ARTIFACTORY_HOME}/etc.original && \
    etcOK=true
    if [ ! $etcOK ]; then
       echo
       echo -e " ** ERROR"
       echo
       exit 1
    fi
    echo -e " DONE"
    if [ ! -d /etc/artifactory ]; then
        echo -n "creating dir /etc/artifactory..."
        mkdir -p /etc/artifactory && \
        etcOK=true
    fi
    if [ $etcOK = true ]; then
        echo -n "creating the link and updating dir..."
        ln -s /etc/artifactory etc && \
        cp -R ${ARTIFACTORY_HOME}/etc.original/* /etc/artifactory/ && \
        etcOK=true
    fi
    if [ ! $etcOK ]; then
        echo
        echo -e " ** ERROR"
        echo
        exit 1
    fi
fi
echo -e " DONE"

echo -n "Creating environment file /etc/artifactory/default..."
if [ -e /etc/artifactory/default ]; then
    echo -n "already exists, no change...  "
    echo -e "\033[33m*** Make sure your default file is up to date ***"
else
    # Populating the /etc/artifactory/default with ARTIFACTORY_HOME and ARTIFACTORY_USER
    echo -n "creating..."
    cat ${ARTIFACTORY_HOME}/bin/artifactory.default > /etc/artifactory/default && \
    echo "export ARTIFACTORY_HOME=${ARTIFACTORY_HOME}" >> /etc/artifactory/default && \
    echo "export ARTIFACTORY_USER=${ARTIFACTORY_USER}" >> /etc/artifactory/default && \
    etcDefaultOK=true
    if [ ! $etcDefaultOK ]; then
        echo -e " ** ERROR"
        echo
        exit 1
    fi
fi
echo -e " DONE"
echo "** INFO: Please edit the files in /etc/artifactory to set the correct environment"
echo "Especially /etc/artifactory/default that defines ARTIFACTORY_HOME, JAVA_HOME and JAVA_OPTIONS"
echo
echo -n "Creating link ${ARTIFACTORY_HOME}/logs to /var/log/artifactory..."
if [ -L ${ARTIFACTORY_HOME}/logs ]; then
    echo -n "already a link..."
else
    echo -n "creating..."
    artLogFolder=/var/log/artifactory
    logsOK=false
    if [ ! -d "$artLogFolder" ]; then
        mkdir -p $artLogFolder && \
        logsOK=true
    else
        logsOK=true
    fi
    if $logsOK; then
        if [ -d ${ARTIFACTORY_HOME}/logs ]; then
            mv ${ARTIFACTORY_HOME}/logs ${ARTIFACTORY_HOME}/logs.orig
        fi
        ln -s /var/log/artifactory logs && \
        logsOK=true
    fi
    if [ ! $logsOK ]; then
        echo -e " ** ERROR"
        echo
        exit 1
    fi
fi
echo -e " DONE"

echo
echo -n "Setting file permissions to etc, logs, work, data and backup..."
chown -R ${ARTIFACTORY_USER} /etc/artifactory && \
chmod -R 755 /etc/artifactory && \
chown -R ${ARTIFACTORY_USER} ${ARTIFACTORY_HOME}/logs/ && \
chmod -R u+w ${ARTIFACTORY_HOME}/logs/ && \
mkdir -p ${ARTIFACTORY_HOME}/work/ && \
chown ${ARTIFACTORY_USER} ${ARTIFACTORY_HOME}/work/ && \
chmod -R u+w ${ARTIFACTORY_HOME}/work/ && \
mkdir -p ${ARTIFACTORY_HOME}/backup/ && \
chown ${ARTIFACTORY_USER} ${ARTIFACTORY_HOME}/backup/ && \
chmod u+w ${ARTIFACTORY_HOME}/backup/ && \
mkdir -p ${ARTIFACTORY_HOME}/data/ && \
chown -R ${ARTIFACTORY_USER} ${ARTIFACTORY_HOME}/data/ && \
chmod -R u+w ${ARTIFACTORY_HOME}/data/ && \
permChangeOK=true
if [ ! $permChangeOK ]; then
    echo -e " ** ERROR"
    echo
    exit 1
fi
echo -e " DONE"
echo
echo -n "Copying the init.d/artifactory script..."
if [ -e /etc/init.d/artifactory ]; then
    echo -n "already exists..."
else
    echo -n "copying..."
    cp ${ARTIFACTORY_HOME}/bin/artifactoryctl /etc/init.d/artifactory
    if [ ! $? ]; then
        echo -e " ** ERROR"
        echo
        exit 1
    fi
fi
echo -e " DONE"
echo
# Try update-rc.d for debian/ubuntu else use chkconfig
if [ -x /usr/sbin/update-rc.d ]; then
    echo "Initializing artifactory service with update-rc.d..."
    update-rc.d artifactory defaults && \
    chkconfigOK=true
elif [ -x /usr/sbin/chkconfig ] || [ -x /sbin/chkconfig ]; then
    echo "Initializing artifactory service with chkconfig..."
    chkconfig --add artifactory && \
    chkconfig artifactory on && \
    chkconfig --list artifactory && \
    chkconfigOK=true
else
    ln -s /etc/init.d/artifactory /etc/rc3.d/S99artifactory && \
    chkconfigOK=true
fi
if [ ! $chkconfigOK ]; then
    echo -e " ** ERROR"
    echo
    exit 1
fi
echo -e " DONE"
echo
echo -e "************ SUCCESS *****************"
echo "Installation of Artifactory completed"
echo "you can now check installation by running:"
echo "> service artifactory check" or
echo "> /etc/init.d/artifactory check"
echo
echo "Then activate artifactory with:"
echo "> service artifactory start" or
echo "> /etc/init.d/artifactory start"
echo
