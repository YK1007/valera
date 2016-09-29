#!/bin/bash

# App specific config option
PKG=
MAIN=
TRACING=0
NETWORK_REPLAY=0
APP_HOME_DIR=


MODE=NONE
OPTION=option.txt
RESULT=result
EVENT_BIN=inputevent.bin
IO_BIN=io.bin

BLACKLIST=
TRACEACTION=
SCHEDULE=

# Constants
NOFILE="No such file or directory"


#TRACE_Z=valera.trace.z

print_config()
{
    echo "Configuration Options" > $RESULT/config.log
    echo "PKG = $PKG" >> $RESULT/config.log
    echo "MAIN = $MAIN" >> $RESULT/config.log
    echo "APP_HOME_DIR = $APP_HOME_DIR" >> $RESULT/config.log
    echo "TRACING = $TRACING" >> $RESULT/config.log
    echo "NETWORK_REPLAY = $NETWORK_REPLAY" >> $RESULT/config.log
    echo "MODE = $MODE" >> $RESULT/config.log
}

preprocess()
{
    # Create valera folder for this app
    adb shell mkdir $APP_HOME_DIR/valera
    adb shell chmod 777 $APP_HOME_DIR/valera

    # Upload tracing actions
    echo "TRACEACTION = $TRACEACTION"
    if [ ! -z $TRACEACTION ]; then
        adb push $TRACEACTION $APP_HOME_DIR/valera/trace_action.txt
    fi
    # Upload schedule
    echo "SCHEDULE = $SCHEDULE"
    if [ ! -z $SCHEDULE ]; then
        adb push $SCHEDULE $APP_HOME_DIR/valera/schedule.txt
    fi
    # Upload blacklist for tracing
    echo "BLACKLIST = $BLACKLIST"
    if [ ! -z $BLACKLIST ]; then
        adb push $BLACKLIST $APP_HOME_DIR/valera/blacklist.txt
    fi

    # Translate RERAN events
    #java -jar bin/reranTranslate.jar reranInputEvent.txt reranReplayEvent.txt
    #adb push reranReplayEvent.txt /sdcard/valera/reranReplayEvent.txt
}

reset()
{
    echo "Clear APP data: $PKG"
    adb shell pm clear $PKG

    # AnyMemo
    adb shell rm /sdcard/anymemo/*
}

start_app()
{
    adb shell am start -n $PKG/$MAIN
}

record()
{
    print_config;
    reset;
    preprocess;

    # Native Level Config Option
    echo $PKG > $OPTION
    echo "record" >> $OPTION
    echo "tracing=$TRACING" >> $OPTION
    echo "blacklist=$APP_HOME_DIR/valera/blacklist.txt" >> $OPTION
    # Java Level Config Option
    echo "trace_action=$APP_HOME_DIR/valera/trace_action.txt" >> $OPTION
    echo "schedule=$APP_HOME_DIR/valera/schedule.txt" >> $OPTION
    echo "network_replay=$NETWORK_REPLAY" >> $OPTION
    adb push $OPTION $APP_HOME_DIR/valera/option.txt

    adb logcat -c;
    start_app;

    # Use RERAN
    #sleep 5
    #adb shell /data/local/reran.exe /sdcard/valera/reranReplayEvent.txt &

    adb logcat | tee $RESULT/record.log

}

replay()
{
    print_config;
    reset;
    preprocess;

    # Native Level Config Option
    echo $PKG > $OPTION
    echo "replay" >> $OPTION
    echo "tracing=$TRACING" >> $OPTION
    echo "blacklist=$APP_HOME_DIR/valera/blacklist.txt" >> $OPTION
    # Java Level Config Option
    echo "trace_action=$APP_HOME_DIR/valera/trace_action.txt" >> $OPTION
    echo "schedule=$APP_HOME_DIR/valera/schedule.txt" >> $OPTION
    echo "network_replay=$NETWORK_REPLAY" >> $OPTION
    adb push $OPTION $APP_HOME_DIR/valera/option.txt

    echo "Pushing $EVENT_BIN"
    adb push $RESULT/valera/$EVENT_BIN $APP_HOME_DIR/valera/$EVENT_BIN

    #echo "Pushing io.index"
    #adb push $RESULT/valera/io.index $APP_HOME_DIR/valera/io.index

    echo "Pushing io.zip"
    adb push $RESULT/valera/io.zip $APP_HOME_DIR/valera/io.zip
    adb shell /data/local/busybox unzip $APP_HOME_DIR/valera/io.zip -d $APP_HOME_DIR/valera
#    for data in `ls $RESULT/valera/io.bin.*`
#    do
#        file=`basename $data`
#        adb push $RESULT/valera/$file $APP_HOME_DIR/valera/$file
#    done

    adb logcat -c;
    start_app;
    adb logcat | tee $RESULT/replay.log
}

kill_app()
{
    adb shell am force-stop $PKG
}

fetch()
{
    kill_app;

    if [ $MODE = RECORD ]
    then
        echo "Fetching recorded $EVENT_BIN"
        file=`adb shell ls $APP_HOME_DIR/valera/$EVENT_BIN`
        if [[ ! $file == *$NOFILE* ]]; then
            adb pull $APP_HOME_DIR/valera/$EVENT_BIN $RESULT/valera/$EVENT_BIN
        fi

        echo "Fetching recorded $IO_BIN"
        file=`adb shell ls $APP_HOME_DIR/valera/$IO_BIN`
        if [[ ! $file == *$NOFILE* ]]; then
            adb pull $APP_HOME_DIR/valera/$IO_BIN $RESULT/valera/$IO_BIN
            java -jar ./bin/ValeraTool.jar iorewrite $RESULT/valera/$IO_BIN
            cd $RESULT/valera
            zip io.zip io.index io.bin.*
            rm io.index io.bin.*
            cd -
        fi

        echo "Fetching record.trace.z"
        file=`adb shell ls $APP_HOME_DIR/valera/record.trace.z`
        if [[ ! $file == *$NOFILE* ]]; then
            adb pull $APP_HOME_DIR/valera/record.trace.z $RESULT/valera/record.trace.z
        fi
        if [ -f $RESULT/valera/record.trace.z ]; then
            ./bin/inflate_trace $RESULT/valera/record.trace.z $RESULT/record.trace
        fi
    elif [ $MODE = REPLAY ]
    then
        echo "Fetching replay.trace.z"
        file=`adb shell ls $APP_HOME_DIR/valera/replay.trace.z`
        if [[ ! $file == *$NOFILE* ]]; then
            adb pull $APP_HOME_DIR/valera/replay.trace.z $RESULT/valera/replay.trace.z
        fi
        if [ -f $RESULT/valera/replay.trace.z ]; then
            ./bin/inflate_trace $RESULT/valera/replay.trace.z $RESULT/replay.trace
        fi
    else
        echo "Fetch error: current mode is $MODE ."
    fi
}

postprocess()
{
    echo "Extract record.trace.z and replay.trace.z"
    ./bin/inflate_trace $RESULT/valera/record.trace.z $RESULT/record.trace
    ./bin/inflate_trace $RESULT/valera/replay.trace.z $RESULT/replay.trace

    echo "Extrace recorded actions"
    java -jar ./bin/ValeraTool.jar process_trace $RESULT/record.trace > $RESULT/record.actions

    echo "Extrace replayed actions"
    java -jar ./bin/ValeraTool.jar process_trace $RESULT/replay.trace > $RESULT/replay.actions

    echo "Compare recorded and replayed actions"
    java -jar ./bin/ValeraTool.jar compare_events $RESULT/record.trace $RESULT/replay.trace
}

# trap ctrl-c and call ctrl_c()
trap ctrl_c INT

function ctrl_c() {
    echo "** Trapped CTRL-C"
    fetch;
    exit 0;
}


# Main
CMD=
CONFIG=

usage()
{
    echo "Usage: $0 --config=<config> --blacklist=<blacklist.txt> --traceaction=<trace_action.txt> \
        --schedule=<schedule.txt> --result=<result_folder> --cmd=<cmd>"
    echo "config = <your app config file>"
    echo "cmd = [record | replay | kill | postprocess]"
}

OPTION_FORMAT=`getopt -o h --long config:,cmd:,blacklist::,traceaction::,schedule::,result -- "$@"`
if [ $? != 0 ]
then
    exit 1
fi
eval set -- "$OPTION_FORMAT"

while true ; do
    case "$1" in
        --config)
            CONFIG="$2";
            source $CONFIG;
            shift 2;
            ;;
        --cmd)
            CMD="$2";
            if [ -z $CMD ]; then
                echo "Empty cmd argument."; usage; exit 0;
            fi
            shift 2;
            ;;
        --blacklist)
            BLACKLIST="$2";
            if [ -z $BLACKLIST ]; then
                echo "Empty blacklist argument."; usage; exit 0;
            fi
            shift 2;
            ;;
        --traceaction)
            TRACEACTION="$2";
            if [ -z $TRACEACTION ]; then
                echo "Empty traceaction argument."; usage; exit 0;
            fi
            shift 2;
            ;;
        --schedule)
            SCHEDULE="$2";
            if [ -z $SCHEDULE ]; then
                echo "Empty schedule argument."; usage; exit 0;
            fi
            shift 2;
            ;;
        --result)
            RESULT="$2";
            if [ -z $RESULT ]; then
                echo "Empty result argument."; usage; exit 0;
            fi
            ;;
        -h)
            usage; shift; exit 0;
            ;;
        --) shift; break;
            ;;
        *)
            echo "Option format error. $1";
            usage; exit 1;
            ;;
    esac
done

# Check given app config file.
if [ -z "$CONFIG" ]; then
    echo "Missing configure file.";
    usage;
    exit 1;
fi

# Check given cmd option.
if [ -z "$CMD" ]; then
    echo "Missing command.";
    usage;
    exit 1;
fi

# Create result folder if not exist.
if [ ! -d $RESULT ]
then
    mkdir $RESULT
fi

case "$CMD" in
    record)
        MODE=RECORD;
        record;
        ;;
    replay)
        MODE=REPLAY
        replay;
        ;;
    kill)
        kill_app;
        ;;
    fetch)
        MODE=RECORD;
        fetch;
        ;;
    postprocess)
        postprocess;
        ;;
    *)
        echo "Invalid cmd option.";
        usage;
        exit 1;
        ;;
esac

