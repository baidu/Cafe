#!/bin/bash 
source ../util.sh

_PWD=`pwd`
ANDROID_TOP=$_PWD/../..

start_monkey_server()
{
	kill_android_process_by_name "$serial" monkey
	$ADB shell monkey --port 4938 --ignore-crashes --ignore-security-exceptions\
        --ignore-native-crashes -v -v > /dev/null 2>&1 &
	PID_MONKEY=$!
	end_with_script "$PID_MONKEY"
	echo ""
}

cd $ANDROID_TOP
. build/envsetup.sh
cd $_PWD/TestCafe
cp $_PWD/../out/cafe.jar libs
apk=`mm -j5 | grep "Install:" | awk '{print $2}'`
# suppose there is only one android device
serial=`adb devices | grep -v ^$ | grep -v List | awk '{print $1}'`
ADB="adb -s $serial"
start_monkey_server
$ADB shell service call window 1 i32 4939
$ADB install -r $_PWD/../out/Cafe.apk
$ADB install -r $_PWD/Demo/bin/CafeDemo.apk
$ADB install -r $ANDROID_TOP/$apk
$ADB logcat -c
$ADB shell am instrument -w \
com.example.demo.test/com.zutubi.android.junitreport.JUnitReportTestRunner &
$ADB logcat 
