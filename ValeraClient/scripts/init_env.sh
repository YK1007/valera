#!/bin/bash

adb root
adb push ./bin/busybox-armv7l /data/local/busybox
adb shell chmod 0755 /data/local/busybox
