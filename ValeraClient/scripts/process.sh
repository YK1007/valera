#!/bin/bash

grep -i valera log | tail -n +3 > tmp
cd ValeraTrace > /dev/null
java -ea TraceProcessing < ../tmp > ../valera.txt
cd - > /dev/null
