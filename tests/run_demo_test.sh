#!/bin/bash 
source ../util.sh

_PWD=`pwd`
ANDROID_TOP=$_PWD/../..
APK=""
QUERY="767E5EA6"

start_monkey_server()
{
	kill_android_process_by_name "$serial" monkey
	$ADB shell monkey --port 4938 --ignore-crashes --ignore-security-exceptions\
        --ignore-native-crashes -v -v > /dev/null 2>&1 &
	PID_MONKEY=$!
	end_with_script "$PID_MONKEY"
	echo ""
}

compile()
{
    cd $ANDROID_TOP
    . build/envsetup.sh
    cd $_PWD/$target
    cp $_PWD/../out/cafe.jar libs
    # get test package
    rm -f .install
    mm -j5 > .install 2>&1
    if [ $? -ne 0 ];then
        cat .install
        exit 1
    fi
    apk=`cat .install | grep "Install:" | awk '{print $2}'`
    APK="$ANDROID_TOP/$apk"
    rm -f .install
    mkdir -p $_PWD/out
    echo "$_PWD"
    cp $APK $_PWD/out
}

run()
{
    ADB="adb -s $serial"
    start_monkey_server
    $ADB shell service call window 1 i32 4939
    $ADB logcat -c
    $ADB logcat  > $serial.locat &
    logcat_pid=$!
    $ADB shell am instrument -e custom "$QUERY" -w \
        $test_package/com.zutubi.android.junitreport.JUnitReportTestRunner 
    kill -9 $logcat_pid
    $ADB pull /data/data/$package_name/files/$package_name.jpg .
}

while getopts "rca" option
do
	case $option in
		r)  
            #APK=$_PWD/$2
            serial="$2"
            package_name="$3"
            test_package="$package_name.test"
            QUERY="$4"
            echo "serial_number:$serial"
            echo "package_name:$package_name"
            echo "query:$QUERY"
            run 
			exit 0
			;;  
		c)
            target="$2"
            compile 
			exit 0
			;;  
		a)
            ls | grep Test | while read dir
        do
            target=$dir
            echo "compile $dir"
            compile "$dir"
        done
        exit 0
        ;;  
esac
done

target="$1"
compile
serial="HT068P801969"
#serial="0146BF540701201A"
ADB="adb -s $serial"
$ADB install -r $_PWD/Cafe.apk
$ADB install -r $APK
test_package=`aapt dump badging $APK | grep "package:" | awk -F "'" '{print $2}'`
echo "$test_package"
run
