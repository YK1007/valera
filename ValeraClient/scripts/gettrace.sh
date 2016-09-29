#!/bin/bash

APP=$1

adb pull /data/local/zygote.dexload
adb pull /data/data/$APP/valera.dexload
adb pull /data/data/$APP/valera.trace
