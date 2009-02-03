#!/bin/sh

usage()
{
  echo ci: Clean new installation from zip
  echo u:  Update bin dir
  echo r:  Run either idit, idi or jfrog test
  echo h:  Run usage on art update java
}

full()
{
  echo Clean new install
  rm -rf artifactory-update-1.2.6-SNAPSHOT && \
  unzip ../../1.2.x/update/target/artifactory-update-1.2.6-SNAPSHOT.zip && \
  runUsage
}

upd()
{
  echo Updating bin dir
  cp ../../1.2.x/update/src/main/install/bin/*.* ./artifactory-update-1.2.6-SNAPSHOT/bin
}

runUsage()
{
  echo runing artifactory Update no parameters
  ./artifactory-update-1.2.6-SNAPSHOT/bin/artdump
}

run()
{
  if [ -z "$2" ]; then
    usage
  else
    echo Runing Artifactory Update on $2
    ./artifactory-update-1.2.6-SNAPSHOT/bin/artdump $2 $3 $4
  fi
}

if [ -z "$1" ]; then
	usage
else
  if [ "$1" == "ci" ]; then
    full
  elif [ "$1" == "u" ]; then
    upd
  elif [ "$1" == "r" ]; then
    run
  elif [ "$1" == "h" ]; then
    runUsage
  else
    usage
  fi
fi