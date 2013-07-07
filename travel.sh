#!/bin/bash 
source testutils/utils.sh

_PWD=`pwd`
_BUILD="$_PWD/build"
_TOOLS="$_PWD/tools"
_TESTS="$_PWD/tests"
_PROJECT="$_BUILD/CafeTraveler"
SERIAL_NUMBER="$1"
ADB="adb -s $SERIAL_NUMBER"
TARGET="$2"

usage()
{
	echo "usage: $0"
	echo "		 For example, ./travel.sh [serial_number] [target_apk]"
}

resign_apk()
{       
    echo "resign apk [$1] ..."
    java -jar $_TOOLS/signapk.jar $_TOOLS/testkey.x509.pem $_TOOLS/testkey.pk8 "$1" $_BUILD/$2.apk
} 

get_cafe_jar()
{
    cd $_PWD/downloads
    latest_zip=`ls | tail -1f`
    path=${latest_zip%.*}
    prefix=${path##*/}
    rm -rf $prefix
    unzip -q $latest_zip 
    cd $prefix
    mv *.apk $_BUILD/Cafe.apk
    mv `ls *.jar | grep -v doc` $_PROJECT/libs/cafe.jar
}

build()
{     
    # parse apk
    target_apk="$_BUILD/target.apk"
    dump=`aapt dump badging $target_apk`
    target_package=`echo "$dump" | grep "package: name" | awk -F "'" '{print $2}'`
    echo "target_package: $target_package"
    test_package="com.baidu.cafe.test"
    echo "test_package: $test_package"
    launchable_class=`echo "$dump" | grep launchable | awk -F "'" '{print $2}' | head -1`
    echo "launchable_class: $launchable_class"

    # install
    $ADB uninstall $target_package 2>&1 | :
    reliable_install "$SERIAL_NUMBER" "$target_apk"
    assert $?

    # modify template project
    cd $_BUILD
    rm -rf $_PROJECT
    cp -r $_TESTS/TestTravel $_PROJECT
    cd $_PROJECT
    sed -i 's/{test_package}/'"$test_package"'/g' AndroidManifest.xml
    sed -i 's/{target_package}/'"$target_package"'/g' AndroidManifest.xml
    sed -i 's/{test_apk}/'"CafeTraveler"'/g' Android.mk
    main_java_file="src/com/baidu/cafe/test/CafeTraveler.java"
    sed -i 's/{launcher_class}/'"$launchable_class"'/g' "$main_java_file"
    sed -i 's/{target_package}/'"$target_package"'/g' "$main_java_file"
}

compile()
{
    cd $_PROJECT
    android update project -p ./ -t 11
    echo "build CafeTraveler.apk ..."
    ant debug > .ant_tmp 2>&1
    if [ ! 0 -eq $? ]; then
        cat .ant_tmp
        exit 1
    fi   
    rm .ant_tmp
    cd bin
    resign_apk "CafeTraveler-debug.apk" "CafeTraveler"
    traveler="$_BUILD/CafeTraveler.apk"
    $ADB uninstall $test_package 2>&1 | :
    reliable_install "$SERIAL_NUMBER" "$traveler"
    assert $?
}

run()
{
    start_monkey_server "$SERIAL_NUMBER" 
    $ADB shell service call window 2
    $ADB shell service call window 1 i32 4939
    $ADB logcat -c                  
    $ADB logcat  > $SERIAL_NUMBER.logcat &  
    logcat_pid=$!                   
    echo "$ADB shell am instrument -e class com.baidu.cafe.test.CafeTraveler#test_travel -w $test_package/com.baidu.cafe.CafeTestRunner"
    $ADB shell am instrument -e class com.baidu.cafe.test.CafeTraveler#test_travel -w $test_package/com.baidu.cafe.CafeTestRunner
    kill -9 $logcat_pid             
    cd $_BUILD
    $ADB pull /data/data/$target_package/files/ .
}

if [ -z "$1" -o -z "$2" ];then
    usage
    exit 1
fi

rm -rf build
mkdir -p build

resign_apk "$TARGET" "target"
build
get_cafe_jar
compile
run




