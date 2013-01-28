#!/bin/bash 
source ../util.sh

_PWD=`pwd`
ANDROID_TOP=$_PWD/../..
APK=""
QUERY="100"

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

make_project()
{
	# NOTICE: $1 must ends with .apk    
	path=${1%.*}
	apk_name=${path##*/}
	echo "apk_name: $apk_name"
	target_apk=$1
	dump=`aapt dump badging $target_apk`
	target_package=`echo "$dump" | grep "package: name" | awk -F "'" '{print $2}'`
	echo "target_package: $target_package"
	test_package="$target_package"".test"
	echo "test_package: $test_package"
	launchable_class=`echo "$dump" | grep launchable | awk -F "'" '{print $2}' | head -1`
	echo "launchable_class: $launchable_class"

	# install
	ADB="adb -s $serial"
	$ADB uninstall $target_package
	$ADB install $target_apk

	# modify template project
	project_dir="CafeRecorder"
	rm -rf $project_dir
	cp -r TestRecord $project_dir
	cd $project_dir
	sed -i 's/{test_package}/'"$test_package"'/g' AndroidManifest.xml
	sed -i 's/{target_package}/'"$target_package"'/g' AndroidManifest.xml
	sed -i 's/{test_apk}/'"Recorder"'/g' Android.mk
	main_java_file="src/com/example/demo/test/TestCafe.java"
	sed -i 's/{launcher_class}/'"$launchable_class"'/g' "$main_java_file"
	sed -i 's/{target_package}/'"$target_package"'/g' "$main_java_file"
}

run()
{
	ADB="adb -s $serial"
	start_monkey_server
    $ADB shell service call window 1 i32 4939
    $ADB logcat -c
    $ADB logcat  > $serial.locat &
    logcat_pid=$!
    $ADB shell am instrument -e custom "$QUERY"  -w $test_package/com.zutubi.android.junitreport.JUnitReportTestRunner 
    kill -9 $logcat_pid
    $ADB pull /data/data/$package_name/files/$package_name.jpg .
}

serial="$2"
serial="HT068P801969"
package_name="$3"
test_package="$package_name.test"
QUERY="$4"
echo "serial_number:$serial"
echo "package_name:$package_name"
echo "query:$QUERY"

while getopts "cardm" option
do
	case $option in
		r)  
            #APK=$_PWD/$2
            test_case="test_query"
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
		d)
            test_case="test_dump"
            run
			exit 0
            ;;  
		m)
			make_project $2
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
test_case="test_dump"
echo "$test_case"
QUERY="100"
echo "QUERY:$QUERY"
run
