#!/bin/bash 
_PWD=`pwd`
ANDROID_TOP=$_PWD/../..
echo $ANDROID_TOP
cd $ANDROID_TOP
. build/envsetup.sh
cd $_PWD/TestCafe
cp $_PWD/../out/cafe.jar libs
apk=`mm -j5 | grep "Install:" | awk '{print $2}'`
# suppose there is only one android device
serial=`adb devices | grep -v ^$ | grep -v List | awk '{print $1}'`
adb -s $serial install -r $_PWD/Demo/bin/CafeDemo.apk
adb -s $serial install -r $ANDROID_TOP/$apk
adb -s $serial logcat -c
adb -s $serial shell am instrument -w \
com.example.demo.test/com.zutubi.android.junitreport.JUnitReportTestRunner &
adb -s $serial logcat 
