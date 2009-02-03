#!/bin/sh

#Adjust the following
if [ -n "MAVEN_HOME" ]; then
	M2_HOME=$MAVEN_HOME
fi
if [ -z "M2_HOME" ]; then
	echo "ERROR: MAVEN_HOME or M2_HOME is mandatory"
	exit -1
fi

mavenExe=${M2_HOME}/bin/mvn
if [ ! -x "$mavenExe" ]; then
	echo "ERROR: $mavenExe is not executable"
	exit -1
fi

if [ -z "$1" ] || [ ! -d "$1" ]; then
	echo "ERROR: $0 needs the source it folder as first parameter"
	exit -1
fi
IT_HOME=`cd $1 && pwd`

if [ -z "$2" ] || [ ! -d "$2" ]; then
	echo "ERROR: $0 needs the out folder as second parameter"
	exit -1
fi
IT_OUT=`cd $2 && pwd`

M2_REPO=$IT_OUT/m2repo
echo "INFO: MAVEN Repo=$M2_REPO"
# Need a clean repo
if [ -d $M2_REPO ] ; then
  rm -rf $M2_REPO
fi

mkdir $M2_REPO

export MAVEN_OPTS="-Xmx512m"
#export MAVEN_OPTS="-Xmx512m -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# find all pom.xml files
# First revert to clean SVN state
svn revert -R $IT_HOME
allPom=$IT_OUT/allItPom.txt
find $IT_HOME -name "pom.xml" > $allPom
pomList=( $(cat "$allPom") )
nbTests=${#pomList[@]}
index=0
echo "INFO: Converting port 8080 to 8081 in all pom.xml in $IT_HOME"
while [ "$index" -lt "$nbTests" ]; do
	pomFile="${pomList[$index]}"
	mv $pomFile $pomFile.orig
	sed -e "s/8080/8081/g;" $pomFile.orig > $pomFile
	let "index=$index+1"
done

index=0
echo "INFO: Runing deploy on $nbTests integration tests in $IT_HOME"
while [ "$index" -lt "$nbTests" ]; do
	pomFile="${pomList[$index]}"
	itTestFolder=`dirname $pomFile`
	itOutLog=$IT_OUT/test.$index.log
	echo "Log of $pomFile" > $itOutLog
	echo "INFO: Running maven deploy in $itTestFolder"
	cd $itTestFolder && \
	$mavenExe -Dorg.apache.maven.user-settings=$IT_HOME/settings.xml -Dmaven.repo.local=$M2_REPO deploy >> $itOutLog 2> $itOutLog.err
	let "index=$index+1"
done

