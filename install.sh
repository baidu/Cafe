#!/bin/bash

# DESCRIPTION: make, install and run testcase for Cafe
# USAGE: put Cafe/ at ANDROID_TOP and ./install.sh
# REPORTING BUGS: luxiaoyu01@baidu.com
# TODO: support multi-device on one pc

SRC=`pwd`
ANDROID_TOP=$SRC/../
SLEEP_TIME=5
HAS_DEVICE=""
CPU_NUMBER=`cat /proc/cpuinfo|grep processor|wc -l`

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
	adb devices

	if [ -z "$HAS_DEVICE" ];then
		echo "No device is on line!"
		echo "Arms.apk & ArmsProxy.apk will not be installed to device!!!"
	else
		adb root
		sleep $SLEEP_TIME

		#### chmod for screencap
		adb shell chmod 666  /dev/graphics/fb0
		adb shell chmod 666 /sys/class/leds/lcd-backlight/brightness
	fi
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

function generate_normal_arms()
{
	backup
	modify_testservice
	modify_testrunner

	init
	make_cafe
	make_install_arms

	revert
}

function backup()
{
	cd $SRC
	rm -rf backup
	mkdir -p backup

	cp testservice/Android.mk backup
	mv backup/Android.mk backup/Android.mk.arms
	cp testservice/AndroidManifest.xml backup
	cp testservice/src/com/baidu/cafe/remote/UILib.java backup
	cp testservice/src/com/baidu/cafe/remote/Arms.java backup

	cp testrunner/Android.mk backup
	cp testrunner/src/com/baidu/cafe/local/LocalLib.java backup
	cp testrunner/src/com/baidu/cafe/local/YiOperator.java backup
}

function revert()
{
	cd $SRC

	cp backup/Android.mk.arms testservice
	mv testservice/Android.mk.arms testservice/Android.mk
	cp backup/AndroidManifest.xml testservice
	cp backup/UILib.java testservice/src/com/baidu/cafe/remote
	cp backup/Arms.java testservice/src/com/baidu/cafe/remote

	cp backup/Android.mk testrunner
	cp backup/LocalLib.java testrunner/src/com/baidu/cafe/local
	cp backup/YiOperator.java testrunner/src/com/baidu/cafe/local

	rm -rf backup
}

function modify_testservice()
{
	# delete LOCAL_CERTIFICATE
	replace_line "testservice/Android.mk" "LOCAL_CERTIFICATE" "#LOCAL_CERTIFICATE := platform"

	# delete sharedUserId
	replace_line "testservice/AndroidManifest.xml" "sharedUserId" "      package=\"com\.baidu\.cafe\.remote\">"
	#cp testservice/AndroidManifest.xml .

	# modify the first UILib.java#EVENT_SENDER to USE_MONKEY
	replace_line "testservice/src/com/baidu/cafe/remote/UILib.java" "private final static int .*EVENT_SENDER" "    private final static int EVENT_SENDER   = USE_MONKEY;"

	# delete Arms.java#ServiceManager.addService
	replace_line "testservice/src/com/baidu/cafe/remote/Arms.java" "ServiceManager.addService" " "
}

function modify_testrunner()
{
	# remove yi from testrunner
	replace_line "testrunner/Android.mk" "LOCAL_JAVA_LIBRARIES" "LOCAL_JAVA_LIBRARIES := android.test.runner"
	replace_line "testrunner/Android.mk" "LOCAL_USE_YI_RES" "#LOCAL_USE_YI_RES := true"

	# modify LocalLib
	replace_line "testrunner/src/com/baidu/cafe/local/LocalLib.java" "extends YiOperator" "\/\/public class LocalLib extends YiOperator { \/\/ B"
	replace_line "testrunner/src/com/baidu/cafe/local/LocalLib.java" "extends SoloEx" "public class LocalLib extends SoloEx { \/\/ A"

	rm testrunner/src/com/baidu/cafe/local/YiOperator.java
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

function make_install_arms()
{
	if [ ! -z "$HAS_DEVICE" ];then
		adb uninstall com.baidu.cafe.remote
		adb uninstall com.baidu.arms.proxy
	fi

	cd $SRC/testservice
	rm -rf $ANDROID_TOP/out/target/common/obj/APPS/Arms_intermediates
	rm -rf $ANDROID_TOP/out/target/common/obj/APPS/ArmsProxy_intermediates
	mm clean-Cafe
	mm -j$CPU_NUMBER | grep Install | while read LINE
do
	apk=`echo "$LINE"`
	apkPath=${apk#"Install: "};
	if [ ! -z "$HAS_DEVICE" ];then
		adb install ${ANDROID_TOP}/${apkPath}
	fi
	cp ${ANDROID_TOP}/${apkPath} $SRC/out
done

if [ ! -z "$HAS_DEVICE" ];then
	start_monkey_server
fi
}

function start_monkey_server()
{
	cd $SRC
	monkey=`adb shell ps | grep monkey`
	pid=`echo $monkey | awk -F " " '{print $2}'`
	if [ ! -z "$pid" ]; then 
		adb shell "kill -9 $pid"
	fi
	adb shell monkey --port 4938 --ignore-crashes --ignore-security-exceptions --ignore-native-crashes -v -v > .monkey.log &
	sleep $SLEEP_TIME
	echo ""
}


HAS_DEVICE=`adb devices | grep -v ^$ | grep -v List`
while getopts "htnc" option
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
		n)
			echo "Begin to generate a normal arms.."
			generate_normal_arms
			exit 0
			;;  
	esac
done

init
make_cafe
make_install_arms

