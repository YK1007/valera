#!/bin/sh

EVENTRACER_HOME=/extra/project/eventracer_android/EventRacerAndroid
ACTIONLOG=$1

$EVENTRACER_HOME/bin/eventracer/webapp/raceanalyzer $ACTIONLOG
