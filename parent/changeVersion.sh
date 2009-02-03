#!/bin/sh

curdir=`dirname $0`

oldVersion=1.2.1-SNAPSHOT

if [ -z "$1" ]; then
    echo "Usage: $0 newVersion [pom file]"
    echo "if pom file is empty will apply to all POM files"
    exit 1
fi

newVersion=$1

if [ -z "$2" ]; then
    find $curdir/.. -name "pom.xml" -exec $curdir/changeVersion.sh $newVersion \{\} \;
else
    if [ -e "$2.old" ]; then
        \rm -f $2.old
    fi
    if [ "$newVersion" = "clean" ]; then
        # Just did .old cleaning so exiting
        exit 0;
    fi

    sed -e "s/$oldVersion/$newVersion/g;" $2 > $2.new && \
    mv $2 $2.old && \
    mv $2.new $2
fi

