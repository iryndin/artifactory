#!/bin/bash

###
# READ DEFAULT ENV VARS
###
ARTIFACTORY_HOME=""
TOMCAT_HOME=""
. /etc/artifactory/default

# Interactively configures MySQL for use by Artifactory

###
# CHECK FOR ROOT
###

CURRENT_USER=`id -nu`
if [ "$CURRENT_USER" != "root" ]; then
  echo
  echo "ERROR: This tool can only be used when logged in as root."
  echo
  exit 1
fi

echo
echo "########################################################"
echo "# Welcome to the Artifactory MySQL configuration tool! #"
echo "########################################################"

###
# CHECK FOR MYSQL RUNTIME
###

MYSQL_EXISTS=`whereis mysql`
if [ -z "$MYSQL_EXISTS" ]; then
  echo
  echo "Unable to find MySQL. Please make sure it installed and available before continuing."
  echo "Press enter to quit..."
  read
  exit 0
fi

###
# CHECK THAT NEEDED FILES EXIST
###
ARTIFACTORY_SYS_PROPS="$ARTIFACTORY_HOME/etc/artifactory.system.properties"
MYSQL_REPO_CONFIG="$ARTIFACTORY_HOME/etc/repo/filesystem-mysql/repo.xml"
TOMCAT_LIB="$TOMCAT_HOME/lib"

if [ ! -f "$ARTIFACTORY_SYS_PROPS" ]; then
  echo
  echo "Unable to find Artifactory system properties file at '${ARTIFACTORY_SYS_PROPS}'. Cannot continue."
  echo "Press enter to quit..."
  read
  exit 1
fi

if [ ! -f "$MYSQL_REPO_CONFIG" ]; then
  echo
  echo "Unable to find Artifactory MySQL repo configuration file at '${MYSQL_REPO_CONFIG}'. Cannot continue."
  echo "Press enter to quit..."
  read
  exit 1
fi

if [ ! -d "$TOMCAT_LIB" ]; then
  echo
  echo "Unable to find Artifactory MySQL repo configuration file at '${MYSQL_REPO_CONFIG}'. Cannot continue."
  echo "Press enter to quit..."
  read
  exit 1
fi

###
# CHECK FOR EXISTING STARTUP SCRIPT AND RUNNING SERVICE
###

STARTUP_SCRIPT=/etc/init.d/artifactory
if [ -f $STARTUP_SCRIPT ]; then
  SERVICE_STATUS="`${STARTUP_SCRIPT} status`"
  if [[ ! "$SERVICE_STATUS" =~ .*[sS]topped.* ]]; then
    echo
    echo "Stopping the Artifactory service..."
    $STARTUP_SCRIPT stop || exit $?
  fi
fi

###
# CHECK FOR EXISTING DATA FOLDER
###

DATA_FOLDER="$ARTIFACTORY_HOME/data"
if [ -d "$DATA_FOLDER" ]; then
  echo
  echo "Please notice: An existing Artifactory data folder has been found at '${DATA_FOLDER}' and can be kept aside."
  read -p "Continue [Y/n]? " CONTINUE_INSTALL
  
  if [[ "${CONTINUE_INSTALL}" =~ [nN] ]]; then
    echo
    echo "Please make sure to move aside the current data folder before continuing."
    echo "Press enter to quit..."
    read
    exit 0
  fi

  BACKUP_DATA_FOLDER=${DATA_FOLDER}.backup
  echo
  echo "Moving the Artifactory data folder to '${BACKUP_DATA_FOLDER}'. You may remove it later."
  mv ${DATA_FOLDER} ${BACKUP_DATA_FOLDER} && \
   chown artifactory:artifactory ${BACKUP_DATA_FOLDER} || exit $?
fi

###
# PROMPT FOR MYSQL ADMIN USER
###

echo
read -p "Please enter the MySQL server admin username [root]: " MYSQL_ADMIN_USERNAME
if [ -z "$MYSQL_ADMIN_USERNAME" ]; then
  MYSQL_ADMIN_USERNAME="root"
fi

###
# PROMPT FOR MYSQL ADMIN PASSWORD
###

echo
read -s -p "Please enter the MySQL server admin password: " MYSQL_ADMIN_PASSWORD

###
# CONFIGURE MYSQL
###

DEFAULT_DATABASE_USERNAME=artifactory_user
echo
read -p "Please enter the Artifactory database username [$DEFAULT_DATABASE_USERNAME]: " ARTIFACTORY_DATABASE_USERNAME
if [ -z "$ARTIFACTORY_DATABASE_USERNAME" ]; then
  ARTIFACTORY_DATABASE_USERNAME="$DEFAULT_DATABASE_USERNAME"
fi

DEFAULT_DATABASE_PASSWORD=password
echo
read -s -p "Please enter the Artifactory database password [$DEFAULT_DATABASE_PASSWORD]: " ARTIFACTORY_DATABASE_PASSWORD
if [ -z "$ARTIFACTORY_DATABASE_PASSWORD" ]; then
  ARTIFACTORY_DATABASE_PASSWORD="$DEFAULT_DATABASE_PASSWORD"
fi

echo
echo "Creating the Artifactory MySQL user and database..."

MYSQL_LOGIN="mysql -u$MYSQL_ADMIN_USERNAME"

if [ ! -z "$MYSQL_ADMIN_PASSWORD" ];then
  MYSQL_LOGIN="$MYSQL_LOGIN -p$MYSQL_ADMIN_PASSWORD"
fi

$MYSQL_LOGIN <<EOF
CREATE DATABASE IF NOT EXISTS artifactory CHARACTER SET=utf8;
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP,ALTER,INDEX on artifactory.* TO '$ARTIFACTORY_DATABASE_USERNAME'@'localhost' IDENTIFIED BY '$ARTIFACTORY_DATABASE_PASSWORD';
FLUSH PRIVILEGES;
QUIT
EOF

if [ $? -ne 0 ]; then
    echo "Failed to execute MySQL setup."
    exit 1
fi

###
# SWAP REPO CONFIGURATION FILE
###

sed -r --in-place 's/#artifactory.jcr.configDir/artifactory.jcr.configDir/' "$ARTIFACTORY_SYS_PROPS" && \
sed -r --in-place 's/artifactory.jcr.configDir=.*/artifactory.jcr.configDir=repo\/filesystem-mysql/' "$ARTIFACTORY_SYS_PROPS" || exit $?

###
# EDIT THE MYSQL CONFIG IF NEEDED
###

if [ "$ARTIFACTORY_DATABASE_USERNAME" != "$DEFAULT_DATABASE_USERNAME" ]; then
  sed -r --in-place "s/<param name=\"user\" value=.*\/>/<param name=\"user\" value=\"$ARTIFACTORY_DATABASE_USERNAME\"\/>/" "$MYSQL_REPO_CONFIG" || exit $?
fi

if [ "$ARTIFACTORY_DATABASE_PASSWORD" != "$DEFAULT_DATABASE_PASSWORD" ]; then
  sed -r --in-place "s/<param name=\"password\" value=.*\/>/<param name=\"password\" value=\"$ARTIFACTORY_DATABASE_PASSWORD\"\/>/" "$MYSQL_REPO_CONFIG" || exit $?
fi

###
# DOWNLOAD THE MYSQL CONNECTOR
###

JDBC_VERSION=5.1.18
JDBC_JAR=mysql-connector-java-$JDBC_VERSION.jar
ROOT_REPO="http://repo.jfrog.org/artifactory/remote-repos"

downloadMysqlConnector() {
  echo
  if which --skip-alias wget; then
    echo "wget found"
  else
    echo
    echo "Error: Unable to find wget: Try running \"yum install wget\" and then retry the MySQL configuration"
    echo "Press enter to quit..."
    read
    exit 1
  fi

  echo
  echo "Downloading $JDBC_JAR to $TOMCAT_LIB..."
  RESPONSE=`wget -nv --timeout=30 -O $TOMCAT_LIB/$JDBC_JAR $ROOT_REPO/mysql/mysql-connector-java/$JDBC_VERSION/$JDBC_JAR 2>&1`
  if [ $? -ne 0 ] || [[ "${RESPONSE}" == *ERROR* ]]; then
    echo
    echo "Error: Unable to download the MySQL JDBC connector. ERROR:"
    echo "$RESPONSE"
    echo "Please place it manually under $TOMCAT_LIB before running Artifactory."
    echo "Press enter to quit..."
    read
    exit 1
  fi
}

copyOrDownloadMysqlConnector() {
  read -p "Does the current server has access to the Internet? [Y/n]" INTERNET_ACCESS
  if [[ "$INTERNET_ACCESS" =~ [nN] ]]; then
    echo
    read -p "Please provide a local path to $JDBC_JAR?" JDBC_JAR_PATH
    cp $JDBC_JAR_PATH $TOMCAT_LIB || exit $?
  else
    downloadMysqlConnector || exit $?
  fi
}

existingConnector="`ls -A $TOMCAT_LIB | grep "mysql-connector-java-"`"
if [ "$existingConnector" == "$JDBC_JAR" ]; then
  echo "Found correct MySQL JDBC connector [$existingConnector]."
else
  if [ -n "$existingConnector" ]; then
      echo
      echo "Found existing MySQL JDBC connector [$existingConnector]."
      read -p "Do you want to change it? [y/N]" CHANGE_MYSQL
      if [[ "${CHANGE_MYSQL}" =~ [yY] ]]; then
          echo "Removing existing MySQL JDBC connector [$existingConnector]."
          rm $TOMCAT_LIB/$existingConnector && copyOrDownloadMysqlConnector || exit $?
      fi
  else
      echo
      echo "No MySQL JDBC connector found. Download or copy one needed."
      copyOrDownloadMysqlConnector || exit $?
  fi
fi

echo
echo "Configuration completed successfully!"
echo "You can now start up the Artifactory service to use Artifactory with MySQL."
echo "Press enter to exit..."
read
exit 0
