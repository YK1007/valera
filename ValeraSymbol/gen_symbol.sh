#!/bin/bash

output=symbol.txt

rm -f $output > /dev/null

for jar in `find . -name *.jar`
do
    name=`basename $jar`
    nicename=$(echo $jar | cut -c 2-)
    rm -rf tmp > /dev/null
    unzip $jar -d tmp > /dev/null
    if [ -e tmp/classes.dex ]; then
        echo "processing $jar ..."
        dexdump -d tmp/classes.dex > tmp/$name.dexdump

        echo "library=$nicename" >> $output
        cat tmp/$name.dexdump | grep " |\[.*\] " >> $output
    else
        echo "WARNING: $jar has not classes.dex, ignore it."
    fi
    rm -rf tmp > /dev/null
done
