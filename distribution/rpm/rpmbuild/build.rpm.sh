#!/bin/bash -e

TOMCAT_FILE_NAME=apache-tomcat-7.0.11.tar.gz
TOMCAT_DISTRO_URL=http://repo.jfrog.org/artifactory/distributions/org/apache/tomcat/$TOMCAT_FILE_NAME
TOMCAT_DOWNLOAD_DIR=/tmp

if [ ! -f $TOMCAT_DOWNLOAD_DIR/$TOMCAT_FILE_NAME ]; then

  ###
  # CHECK FOR WGET RUNTIME
  ###

  WGET_EXISTS=`whereis wget`
  if [ -z "$WGET_EXISTS" ]; then

    echo
    echo "Error: wget (required for the retrieval of the Tomcat distribution) could be found. Please make sure to install it before proceeding."
    exit 1
  fi

  echo
  echo "Downloading the tomcat distribution from $TOMCAT_DISTRO_URL to $TOMCAT_DOWNLOAD_DIR..."
  RESPONSE=`wget -nv --timeout=30 -O $TOMCAT_DOWNLOAD_DIR/$TOMCAT_FILE_NAME $TOMCAT_DISTRO_URL 2>&1`

  if [[ "${RESPONSE}" == *ERROR* ]]; then

    echo
    echo "Error: Unable to download the tomcat distro."
    exit 1
  fi
fi

echo
echo "Extracting the tomcat distro to ${3}/SOURCES."
tar -xvf $TOMCAT_DOWNLOAD_DIR/$TOMCAT_FILE_NAME -C ${3}/SOURCES/

echo "Cleaning unnessecarry files."
rm -rf ${3}/SOURCES/apache-tomcat-7.0.11/webapps
rm -rf ${3}/SOURCES/apache-tomcat-7.0.11/logs
rm -rf ${3}/SOURCES/apache-tomcat-7.0.11/temp
rm -rf ${3}/SOURCES/apache-tomcat-7.0.11/work

ARTIFACTORY_VERSION=`echo "${1}" | tr -d "-"`

rpmbuild -bb \
--define="_tmppath ${3}/tmp" \
--define="_topdir $PWD" \
--define="_rpmdir ${3}" \
--define="buildroot ${3}/BUILDROOT" \
--define="_sourcedir ${3}/SOURCES" \
--define="artifactory_version ${ARTIFACTORY_VERSION}" \
--define="filename_prefix ${2}" \
--define="full_version ${1}" \
SPECS/artifactory.spec
