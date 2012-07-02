#!/bin/bash

# DESCRIPTION: make, install and run testcase for Cafe
# USAGE: put Cafe/ at ANDROID_TOP and ./install.sh
# REPORTING BUGS: luxiaoyu01@baidu.com
# TODO: support multi-device on one pc

SRC=`pwd`
ANDROID_TOP=$SRC/../
SLEEP_TIME=5
HAS_DEVICE=""
CPU_NUMBER=`cat /proc/cpuinfo | grep processor | wc -l`

function usage()
{
	echo "usage: $0"
	echo "       -t [testcase path]: compile, install and run testcase."
	echo "		    For example, ./install.sh -t testcase/ResManagerTest/cafe_tests"
	echo "       -n: Generate a normal cafe and arms which can be used on non-yi system."
	echo "       -h: help"
}

function init()
{
	# init env
	cd $ANDROID_TOP
	. build/envsetup.sh
}

function run_testcase()
{
	init
	echo "run $1"
	cd $SRC/$1
	rm -rf .cafe_tmp
	mm -j$CPU_NUMBER > .cafe_tmp 2>&1
	if [ ! 0 -eq $? ];then
		cat .cafe_tmp
		exit 0;
	fi
	apk=`grep "LOCAL_PACKAGE_NAME" Android.mk | awk -F " " '{print $3}'`
	apk_install=`cat .cafe_tmp | grep "$apk.apk"`
	apk_path=${apk_install##"Install: "}
	adb install -r $ANDROID_TOP/$apk_path
	target=`grep "package=" AndroidManifest.xml | awk -F "\"" '{print $2}'`

	run_function=""
	if [ ! -z $2 ];then
		run_function="-e class ""$2"
	fi
	adb shell am instrument $run_function -w $target/com.baidu.cafe.cafetestrunner.InstrumentationTestRunner
}

function generate_yi_arms()
{
	backup
	modify_testservice

	init
	make_cafe
	make_arms

	revert
}

function backup()
{
	cd $SRC
	rm -rf backup
	mkdir -p backup

	cp testservice/Android.mk backup
	cp testservice/AndroidManifest.xml backup
}

function revert()
{
	cd $SRC
	cp backup/Android.mk testservice
	cp backup/AndroidManifest.xml testservice
	rm -rf backup
}

function modify_testservice()
{
	# add LOCAL_CERTIFICATE
	replace_line "testservice/Android.mk" "#LOCAL_CERTIFICATE := platform" "LOCAL_CERTIFICATE := platform"

	# add sharedUserId
	replace_line "testservice/AndroidManifest.xml" "package" "      package=\"com\.baidu\.cafe\.remote\" android:sharedUserId=\"android\.uid\.system\">"
}

function replace_line()
{
	file=$1
	key=$2
	value=$3

	line_number=`sed -n "/^.*${key}.*$/=" $file`
	sed -i -e"${line_number}s/.*/${value}/" $file
}

function make_cafe()
{
	# make cafe.jar
	cd $SRC/testrunner
	mm clean-cafe
	rm -rf $ANDROID_TOP/out/target/common/obj/JAVA_LIBRARIES/cafe_intermediates
	mm -j$CPU_NUMBER
	#adb push cafexml.xml /system/etc/permissions
	#adb push $ANDROID_TOP/out/target/product/generic/system/framework/cafe.jar /system/framework

	# make android-web-driver.jar
	cd $SRC/webapp
	mm -j$CPU_NUMBER

	# cp classes-jarjar.jar for development
	cd $SRC
	rm -rf out
	mkdir -p out
	cd out
	cp $ANDROID_TOP/out/target/common/obj/JAVA_LIBRARIES/cafe_intermediates/classes.jar .
	mv classes.jar cafe.jar
	cp $ANDROID_TOP/out/target/common/obj/JAVA_LIBRARIES/android-web-driver_intermediates/classes.jar .
	mv classes.jar android-web-driver.jar
}

function make_arms()
{
	cd $SRC/testservice
	rm -rf $ANDROID_TOP/out/target/common/obj/APPS/Cafe_intermediates
	mm clean-Cafe
	mm -j$CPU_NUMBER | grep Install | while read LINE
do
	apk=`echo "$LINE"`
	apkPath=${apk#"Install: "};
	cp ${ANDROID_TOP}/${apkPath} $SRC/out
done
	cp $SRC/cafe_setup.bat $SRC/out
	cp $SRC/cafe_setup.sh $SRC/out
}

HAS_DEVICE=`adb devices | grep -v ^$ | grep -v List`
while getopts "hty" option
do
	case $option in
		h)
			usage
			exit 0
			;;  
		t)  
			if [ ! -z "$HAS_DEVICE" ];then
				run_testcase $2 $3
			else
				echo "There is no device to run testcase!"
			fi
			exit 0
			;;  
		y)
			generate_yi_arms
			mv out/Cafe.apk out/Cafe_Yi.apk
			exit 0
			;;  
	esac
done

init
make_cafe
make_arms

