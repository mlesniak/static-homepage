#!/bin/bash

path=$1
cmd=static

mvn clean compile assembly:single
echo "java -jar $path/static-1.0-SNAPSHOT-jar-with-dependencies.jar \$*" >$path/$cmd
chmod a+x $path/$cmd
cp target/static-1.0-SNAPSHOT-jar-with-dependencies.jar $path


