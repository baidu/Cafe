#!/bin/bash

adb shell service call window 2
adb shell service call window 1 i32 4939
monkey=`adb shell ps | grep monkey`
pid=`echo $monkey | awk -F " " '{print $2}'`
if [ ! -z "$pid" ]; then 
	adb shell "kill -9 $pid"
fi
adb shell monkey --port 4938 --ignore-crashes --ignore-security-exceptions --ignore-native-crashes -v -v > .monkey_log &
echo ""
