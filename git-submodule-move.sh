#!/bin/sh

SCRIPT=`readlink -f $0`
DIR=`dirname $SCRIPT`
JAR=$DIR/target/scala-2.10/git-submodule-move-assembly-0.1-SNAPSHOT.jar

if [ ! -r $JAR ]; then
	echo "No jar found, rebuilding..."
	( cd $DIR; sbt -q -batch assembly; )
fi

java -jar $JAR $@

