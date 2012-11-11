#!/bin/bash -e

if [ -z "$4" ]; then

    echo
    echo "Error: Usage is $0 productName fullVersion releaseNumber outBuildDir"
    exit 1
fi
FILENAME_PREFIX="$1"
FULL_VERSION="$2"
RELEASE_NUMBER="$3"
OUT_BUILD_DIR="$4"

TOMCAT_NAME=apache-tomcat-7.0.27
TOMCAT_FILE_NAME=$TOMCAT_NAME.tar.gz
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

curDir="`dirname $0`"
curDir="`cd $curDir; pwd`"
RPM_SOURCES_DIR="$OUT_BUILD_DIR/SOURCES"

if [ -z "$OUT_BUILD_DIR" ] || [ ! -d "$OUT_BUILD_DIR" ]; then

    echo
    echo "Error: The output directory $OUT_BUILD_DIR does not exists!"
    exit 1
fi

echo
echo "Extracting the tomcat distro to $RPM_SOURCES_DIR."
tar -xvf $TOMCAT_DOWNLOAD_DIR/$TOMCAT_FILE_NAME -C $RPM_SOURCES_DIR/ || exit $?

echo "Cleaning unnessecarry files."
cd $RPM_SOURCES_DIR/$TOMCAT_NAME && \
rm -rf $RPM_SOURCES_DIR/$TOMCAT_NAME/webapps && \
rm -rf $RPM_SOURCES_DIR/$TOMCAT_NAME/logs && \
rm -rf $RPM_SOURCES_DIR/$TOMCAT_NAME/temp && \
rm -rf $RPM_SOURCES_DIR/$TOMCAT_NAME/work || exit $?

ARTIFACTORY_VERSION=`echo "$FULL_VERSION" | sed 's/SNAPSHOT/devel/g; s/-/./g;'`

cd $curDir && rpmbuild -bb \
--define="_tmppath $OUT_BUILD_DIR/tmp" \
--define="_topdir $PWD" \
--define="_rpmdir $OUT_BUILD_DIR" \
--define="buildroot $OUT_BUILD_DIR/BUILDROOT" \
--define="_sourcedir $RPM_SOURCES_DIR" \
--define="artifactory_version $ARTIFACTORY_VERSION" \
--define="artifactory_release $RELEASE_NUMBER" \
--define="filename_prefix $FILENAME_PREFIX" \
--define="full_version $FULL_VERSION" \
SPECS/artifactory.spec
