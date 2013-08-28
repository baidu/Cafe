#!/bin/bash

# DESCRIPTION: make, install and run testcase for Cafe
# USAGE: put Cafe/ at ANDROID_TOP and ./make.sh
# REPORTING BUGS: luxiaoyu01@baidu.com

SRC=`pwd`
ANDROID_TOP=$SRC/../
SLEEP_TIME=5
HAS_DEVICE=""
CPU_NUMBER=`cat /proc/cpuinfo | grep processor | wc -l`
MODE_PROGUARD="false"

usage()
{
	echo "usage: $0"
	echo "       -t [testcase path]: compile, install and run testcase."
	echo "		    For example, ./install.sh -t testcase/ResManagerTest/cafe_tests"
	echo "       -h: help"
}

init()
{
	# init env
	cd $ANDROID_TOP
	. build/envsetup.sh
}

run_testcase()
{
	init
	echo "run $1"
	cd $SRC/$1
	rm -rf .cafe_tmp
	mm -j$CPU_NUMBER > .cafe_tmp 2>&1
	if [ 0 -ne $? ];then
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

make_cafe()
{
	# make cafe.jar
    cafe_intermediates="$ANDROID_TOP/out/target/common/obj/JAVA_LIBRARIES/cafe_intermediates"
	cd $SRC/testrunner
	mm clean-cafe
	rm -rf $cafe_intermediates
	mm -j$CPU_NUMBER
	if [ 0 -ne $? ];then
		exit 1;
	fi
	#adb push cafexml.xml /system/etc/permissions
	#adb push $ANDROID_TOP/out/target/product/generic/system/framework/cafe.jar /system/framework

	# cp classes-jarjar.jar for development
	cd $SRC
	rm -rf out
	mkdir -p out
    cd out
    if [ "$MODE_PROGUARD" == "true" ];then
        cafe_jar="proguard.classes.jar"
    else
        cafe_jar="classes.jar"
    fi
    cp $cafe_intermediates/$cafe_jar .
    mv $cafe_jar cafe.jar

    # add WebElementRecorder.js to cafe.jar
    mkdir -p test
    cp cafe.jar test
    cd test
    unzip cafe.jar
    rm cafe.jar
    cp $SRC/testrunner/src/com/baidu/cafe/local/record/WebElementRecorder.js com/baidu/cafe/local/record
    jar cvf cafe.jar com/*
    cp cafe.jar ../
    cd ../
    rm -rf test

    # generate javadoc
    # added by zhangjunjun@baidu.com
    cd $SRC/testrunner
    javadoc -classpath ../out/cafe.jar:libs/android.jar -d ../doc \
        src/com/baidu/cafe/local/LocalLib.java src/com/baidu/cafe/remote/Armser.java
    cd $SRC/doc
    jar cf ../out/cafe_doc.jar *
    rm -rf $SRC/doc

    #cp $ANDROID_TOP/out/target/common/obj/JAVA_LIBRARIES/android-web-driver_intermediates/classes.jar .
	#mv classes.jar android-web-driver.jar
}

make_arms()
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
	cp $SRC/testutils/cafe_setup.bat $SRC/out
	cp $SRC/testutils/cafe_setup.sh $SRC/out
	cp $SRC/testutils/utils.sh $SRC/out
}

compile()
{
    init
    make_cafe
    make_arms
}

modify_cafe_for_proguard()
{
    cd $SRC

    # backup
    rm -rf backup
    mkdir -p backup
    android_mk="testrunner/Android.mk"
    viewrecorder_java="testrunner/src/com/baidu/cafe/local/record/ViewRecorder.java"
    cafereplay_java="tests/TestRecord/src/com/example/demo/test/CafeReplay.java"
    cp $android_mk backup
    cp $viewrecorder_java backup
    cp $cafereplay_java backup

    # modify
    sed -i 's/#LOCAL_PROGUARD_ENABLED := full/'"LOCAL_PROGUARD_ENABLED := full"'/g' $android_mk
    sed -i 's/#LOCAL_PROGUARD_FLAG_FILES := proguard.flags/'"LOCAL_PROGUARD_FLAG_FILES := proguard.flags"'/g' $android_mk 
    sed -i 's/protected void setUp() throws Exception/'"protected void setUp()"'/g' $viewrecorder_java
    sed -i 's/protected void tearDown() throws Exception/'"protected void tearDown()"'/g' $viewrecorder_java
    sed -i 's/protected void setUp() throws Exception/'"protected void setUp()"'/g' $cafereplay_java
    sed -i 's/protected void tearDown() throws Exception/'"protected void tearDown()"'/g' $cafereplay_java
}

revert()
{
    cd $SRC
    cp backup/Android.mk testrunner
    cp backup/ViewRecorder.java testrunner/src/com/baidu/cafe/local/record/
    cp backup/CafeReplay.java tests/TestRecord/src/com/example/demo/test/
    rm -rf backup
}

HAS_DEVICE=`adb devices | grep -v ^$ | grep -v List`
while getopts "htp" option
do
	case $option in
		h)
			usage
			exit 0
			;;  
		t)  
			if [ ! -z "$HAS_DEVICE" ];then
                cd $SRC/tests
                cp ../out/Cafe.apk .
                ./run_demo_test.sh $2 "$3"
			else
				echo "There is no device to run testcase!"
			fi
			exit 0
			;;  
		p)
            MODE_PROGUARD="true"
            modify_cafe_for_proguard
            compile
            revert
            exit 0
			;;  
	esac
done

compile
