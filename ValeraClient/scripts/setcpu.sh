#!/bin/bash

CPU_FREQ=350000
CMD_PREFIX="adb shell "

adb root

$CMD_PREFIX " echo userspace >  /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
$CMD_PREFIX " echo userspace >  /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor"

$CMD_PREFIX " cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies"
#350000 700000 920000 1200000

$CMD_PREFIX " echo $CPU_FREQ > /sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed"
$CMD_PREFIX " echo $CPU_FREQ > /sys/devices/system/cpu/cpu1/cpufreq/scaling_setspeed"

$CMD_PREFIX " cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq "
$CMD_PREFIX " cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq "
