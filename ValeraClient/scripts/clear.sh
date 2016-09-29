#!/bin/bash

APP=pe.moe.nori


adb shell am force-stop $APP
adb shell pm clear $APP
