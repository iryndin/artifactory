#!/bin/sh

if [ -n "$1" ]; then
    ARTIFACTORY_USER=$1
fi
if [ -z "$ARTIFACTORY_USER" ]; then
    ARTIFACTORY_USER=artifactory
fi

echo
echo "Installing artifactory as a Unix service that will run as user ${ARTIFACTORY_USER}"

curUser=`id -nu`
if [ "$curUser" != "root" ]
then
    echo
    echo -e "\033[31m** ERROR: Only root user can install artifactory as a service\033[0m"
    echo
    exit 1
fi

curdir=`dirname $0`
cd $curdir/..
if [ ! $? ]; then
    echo
    echo -e "\033[31m** ** ERROR: Cannot go to ARTIFACTORY_HOME=$curdir/..\033[0m"
fi

ARTIFACTORY_HOME=`pwd`
echo
echo "Installing artifactory with home ${ARTIFACTORY_HOME}"

echo -n "Creating user ${ARTIFACTORY_USER}..."
artifactoryUsername=`id -nu ${ARTIFACTORY_USER}`
if [ "$artifactoryUsername" == "${ARTIFACTORY_USER}" ]; then
    echo -n "already exists..."
else
    echo -n "creating..."
    useradd -m ${ARTIFACTORY_USER}
    if [ ! $? ]; then
        echo -e "\033[31m** ERROR\033[0m"
        echo
        exit 1
    fi
fi
echo -e "\033[32mDONE\033[0m"

echo
echo -n "Copying configuration files to /etc/artifactory..."
if [ -L ${ARTIFACTORY_HOME}/etc ]; then
    echo -n "already exists, no change..."
else
    echo -n "copying..."
    mkdir -p /etc/artifactory && \
    mv ${ARTIFACTORY_HOME}/etc/* /etc/artifactory && \
    \rm -rf ${ARTIFACTORY_HOME}/etc && \
    ln -s /etc/artifactory etc && \
    etcOK=true
    if [ ! $etcOK ]; then
        echo
        echo -e "\033[31m** ERROR\033[0m"
        echo
        exit 1
    fi
fi
echo -e "\033[32mDONE\033[0m"

echo -n "Creating environment file /etc/artifactory/default..."
if [ -e /etc/artifactory/default ]; then
    echo -n "already exists, no change..."
else
    # Populating the /etc/artifactory/default with ARTIFACTORY_HOME and ARTIFACTORY_USER
    echo -n "creating..."
    cat ${ARTIFACTORY_HOME}/bin/artifactory.default > /etc/artifactory/default && \
    echo "export ARTIFACTORY_HOME=${ARTIFACTORY_HOME}" >> /etc/artifactory/default && \
    echo "export ARTIFACTORY_USER=${ARTIFACTORY_USER}" >> /etc/artifactory/default && \
    etcDefaultOK=true
    if [ ! $etcDefaultOK ]; then
        echo -e "\033[31m** ERROR\033[0m"
        echo
        exit 1
    fi
fi
echo -e "\033[32mDONE\033[0m"
echo "** INFO: Please edit the files in /etc/artifactory to set the correct environment"
echo "Especially /etc/artifactory/default that defines ARTIFACTORY_HOME, JAVA_HOME and JAVA_OPTIONS"
echo
echo -n "Creating link ${ARTIFACTORY_HOME}/logs to /var/log/artifactory..."
if [ -L ${ARTIFACTORY_HOME}/logs ]; then
    echo -n "already a link..."
else
    echo -n "creating..."
    mkdir -p /var/log/artifactory && \
    \rm -rf ${ARTIFACTORY_HOME}/logs && \
    ln -s /var/log/artifactory logs && \
    logsOK=true
    if [ ! $logsOK ]; then
        echo -e "\033[31m** ERROR\033[0m"
        echo
        exit 1
    fi
fi
echo -e "\033[32mDONE\033[0m"

echo
echo -n "Setting file permissions to etc, logs, work, data and backup..."
chown ${ARTIFACTORY_USER} -R /etc/artifactory && \
chmod 755 -R /etc/artifactory && \
chown ${ARTIFACTORY_USER} -R ${ARTIFACTORY_HOME}/logs/ && \
chmod u+w -R ${ARTIFACTORY_HOME}/logs/ && \
mkdir -p ${ARTIFACTORY_HOME}/work/ && \
chown ${ARTIFACTORY_USER} ${ARTIFACTORY_HOME}/work/ && \
chmod u+w -R ${ARTIFACTORY_HOME}/work/ && \
mkdir -p ${ARTIFACTORY_HOME}/backup/ && \
chown ${ARTIFACTORY_USER} ${ARTIFACTORY_HOME}/backup/ && \
chmod u+w ${ARTIFACTORY_HOME}/backup/ && \
mkdir -p ${ARTIFACTORY_HOME}/data/ && \
chown ${ARTIFACTORY_USER} -R ${ARTIFACTORY_HOME}/data/ && \
chmod u+w -R ${ARTIFACTORY_HOME}/data/ && \
permChangeOK=true
if [ ! $permChangeOK ]; then
    echo -e "\033[31m** ERROR\033[0m"
    echo
    exit 1
fi
echo -e "\033[32mDONE\033[0m"
echo
echo -n "Copying the init.d/artifactory script..."
if [ -e /etc/init.d/artifactory ]; then
    echo -n "already exists..."
else
    echo -n "copying..."
    cp ${ARTIFACTORY_HOME}/bin/artifactoryctl /etc/init.d/artifactory
    if [ ! $? ]; then
        echo -e "\033[31m** ERROR\033[0m"
        echo
        exit 1
    fi
fi
echo -e "\033[32mDONE\033[0m"
echo
# Try update-rc.d for debian/ubuntu else use chkconfig
if [ -x /usr/sbin/update-rc.d ]; then
    echo "Initializing artifactory service with update-rc.d..."
    update-rc.d artifactory defaults && \
    chkconfigOK=true
else
    echo "Initializing artifactory service with chkconfig..."
    chkconfig --add artifactory && \
    chkconfig artifactory on && \
    chkconfig --list artifactory && \
    chkconfigOK=true
fi
if [ ! $chkconfigOK ]; then
    echo -e "\033[31m** ERROR\033[0m"
    echo
    exit 1
fi
echo -e "\033[32mDONE\033[0m"
echo
echo -e "\033[32m************ SUCCESS *****************\033[0m"
echo "Installation of Artifactory completed"
echo "you can now check installation by running:"
echo "> service artifactory check"
echo
echo "Then activate artifactory with:"
echo "> service artifactory start"
echo
