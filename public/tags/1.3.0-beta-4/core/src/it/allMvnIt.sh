#!/bin/sh

IT_HOME=`dirname "$0"`
IT_HOME=`cd $IT_HOME && pwd`

mavenExe=$IT_HOME/mvnit

IT_OUT=$IT_HOME/out
mkdir $IT_OUT

# find all pom.xml files
allPom=$IT_OUT/allItPom.txt
find $IT_HOME -name "pom.xml" > $allPom
pomList=( $(cat "$allPom") )
nbTests=${#pomList[@]}
index=0
echo "INFO: Runing install on $nbTests integration tests in $IT_HOME"
while [ "$index" -lt "$nbTests" ]; do
	pomFile="${pomList[$index]}"
	itTestFolder=`dirname $pomFile`
	itOutLog=$IT_OUT/test.$index.log
	echo "Log of $pomFile" > $itOutLog
	echo "INFO: Running maven install in $itTestFolder"
	cd $itTestFolder && \
	$mavenExe "$@" 2>&1 | tee $itOutLog
	let "index=$index+1"
done

