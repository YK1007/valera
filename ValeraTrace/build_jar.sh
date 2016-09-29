#!/bin/bash

JAR=ValeraTool.jar

rm -f $JAR

cp MANIFEST.MF bin

cd bin

list=`find . -name *.class`

jar cvfm $JAR MANIFEST.MF $list
mv $JAR ..

cd ..
