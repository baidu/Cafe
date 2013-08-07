#!/bin/bash

#################################################
########## Android Shell Lib ####################
#################################################

# REPORTING BUGS: luxiaoyu01@baidu.com


########## alias & export ################################
alias 'ps?'='ps ax | grep '
export HISTTIMEFORMAT="%F %T "


#
# Run a command with timeout.
#
# DESCRIPTION:
# If it terminates normally, run_with_timeout will return the return value 
# of cmd string as soon as possible. If it is over time, the command process 
# will be killed including its children processes. This function will product 
# two additional processes. Process A will execute the command string at background. 
# Process B will wait for process A and get return value of command string.
# And B is the father of A. The stderr and stdout of command string will be 
# redirected to the stdout of run_with_timeout as a convenience. 
#
# ret == 99 means wrong parameters
# ret == 100 means timeout
# ret == others value means it's the ret of cmd string
run_with_timeout() # $cmd_str, $timeout
{
	cmd_str=$1
	timeout=$2

	if [[ -z "$cmd_str" || -z "$timeout" ]];then
		log "Parameters has error! cmd: [$cmd_str] timeout: [$timeout]."
		return 99
	fi

	log "Run [$cmd_str] with timeout [$timeout]."
	current_pid=$$"`date +%s%N`"
	run_at_background "$cmd_str" $current_pid &

	end=$((`date +%s` + $timeout))
	while [ $(date +%s) -le $end ]
	do
		if [ -e ".current_pid$current_pid" ];then
			cmd_pid=`cat ."current_pid"$current_pid`
			if [ -z $cmd_pid ];then
				rm -f ."current_pid"$current_pid
				return 0
			fi
			ps=`ps -ef | grep $cmd_pid | grep -v grep`
			if [ -z "$ps" -a -e ".$cmd_pid" ];then
				# normally terminate
				ret=`cat .$cmd_pid`
				rm -f ."current_pid"$current_pid
				rm -f .$cmd_pid
				return $ret
			fi 
		fi
		sleep 1
	done

	# timeout
	echo "[`cat_cmd_by_pid $cmd_pid`] has been timeout[$timeout]!"
	kill_family $cmd_pid
	rm -f .$cmd_pid
	rm -f ."current_pid"$current_pid
	return 100
}

# NOTICE: This is a internal function for run_with_timeout().
# $1 is cmd string
# $2 is current_pid
run_at_background() # $cmd_str, $current_pid
{
	cmd_str=$1
	current_pid=$2

	$cmd_str 2>&1 & 
	pid=$!
	echo $pid > ."current_pid"$current_pid
	wait $pid
	echo $? > .$pid
}

#
# kill pid family from son to father
#
# NOTICE:eats error message of kill -9
kill_family() # $pid_ancestor
{
	pstree -p $1 | awk -F"[()]" '{for(i=0;i<=NF;i++)if($i~/[0-9]+/)print $i}' \
    | sed '/[|+-]/d' | grep -o "[0-9]\+" | sort -nr | uniq | xargs kill -9  \
    2>&1 > /dev/null
}

# ret == 0 means boot success
# ret == 1 means boot failed
# ret == 2 means no such device
wait_for_device_boot_completed() # $serial_number
{
	wait_for_boot_completed $1 "device"
	return $?
}

# ret == 0 means boot success
# ret == 1 means boot failed
# ret == 2 means no such device
wait_for_boot_completed() # $serial_number, $target
{
	ADB="adb -s $1"
	target=$2
	timeout=300

	run_with_timeout "$ADB wait-for-devices" 300
	if [ $? -eq 100 ];then
		return 2
	fi

	if [ "$target" == "device" ];then
		check_point="$ADB shell getprop dev.bootcomplete|grep 1"
	elif [ "$target" == "system" ];then
		$ADB logcat -c
		check_point="$ADB logcat -d AlertService:D *:S | grep BOOT_COMPLETED"
	fi

	end=$((`date +%s` + $timeout))
	while [ $(date +%s) -le $end ]
	do   
		eval $check_point
		if [ $? -eq 0 ]; then 
			# boot success
            sleep 5
			log "[$1] [$target] boot completed !"
			return 0
		fi  
		log "Waiting for [$1] [$target] boot completed..."
		sleep 2
	done

	# timeout
	log "Wait for [$1] [$target] boot timeout!"
	return 1
}

log() # $msg
{
	echo "[`date +%Y-%m-%d\ %H:%M:%S`] $1"
}

#
# grep some processes and kill them
#
kill_by_including() # $grep_str
{
	ps aux | grep "$1" | awk -F " " '{print $2}' | xargs kill -9 
}

#
# Watch a process and terminate it when invoker script is over.
# It is useful for some scenes that some pids should be killed 
# when the invoker script is killed.
end_with_script() # $target_pid
{
	target_pid=$1
	if [ -z "$target_pid" ];then
		echo "pid is null"
		return
	else
		end_with_pid "$target_pid" &
	fi
}

# NOTICE: This is a internal function for end_with_script().
end_with_pid() # $target_pid
{
	cmd_pid="$1"
	pid=$$
	invoker_cmd=`cat_cmd_by_pid $pid`
	cmd=`cat_cmd_by_pid $cmd_pid`

	while :
	do
		cmd_ret=`ps aux | grep $cmd_pid | grep -v grep`
		if [ -z "$cmd_ret" ];then
			log "[$cmd] is over."
			break
		fi
		target_ret=`ps aux | grep $pid | grep -v grep`
		if [ -z "$target_ret" ];then
			log "[$cmd] was killed by end_with_script because [$invoker_cmd] is dead."
			kill -9 $cmd_pid
			break
		fi
		sleep 1
	done
}

cat_cmd_by_pid() # $pid
{
	ps ax | grep $1 | grep -v grep | cut -d: -f 2 | cut -d " " -f 2-
}

#
# Install apk to device with -r .
# If installation failed including "device not found" and "waiting for device", 
# script will return 1 as soon as possible.
#
# NOTICE: if $timeout is empty, it will be filled by apk size 
#
# ret == 0 means install success 
# ret == 1 menas install failed
safe_install() # $SERIAL_NUMBER, $apk, $timeout
{ 
	SERIAL_NUMBER=$1
	apk=$2
	timeout=$3
	ADB="adb -s $SERIAL_NUMBER"

	touch_device "$SERIAL_NUMBER"
	if [ $? -ne 0 ];then
		return 1
	fi	

	if [ ! -e "$apk" ];then
		log "[$apk] does not exist!"
		return 1
	fi

    if [ -z "$3" ];then
        timeout=`get_install_timeout "$apk"`
        log "timeout is set to $timeout defaultly by apk size"
    fi

	log "Install [$apk] on [$SERIAL_NUMBER] in [$timeout]"

	# install at background for "waiting for device"
	install_log="$SERIAL_NUMBER.install"
	rm -f $install_log
	$ADB install -r "$apk" > $install_log 2>&1 &
	pid=$!
	end=$((`date +%s` + $timeout))

	while [ $(date +%s) -le $end ]
	do
		ps=`ps aux | grep $pid | grep -v grep`
		if [ -z "$ps" ];then
			# normally terminate
			ret=`cat $install_log | grep Success`
			cat $install_log
			rm -f $install_log
			if [ -z "$ret" ]; then
				log "Install [$apk] has error!"
				return 1
			fi  

			# install success
			return 0
		fi
		sleep 1

		# check waiting for device
		waiting=`cat $install_log | grep "waiting for device"`
		if [ ! -z "$waiting" ];then
			log "waiting for device[$SERIAL_NUMBER]!"
			kill -9 $pid
			return 1
		fi
	done

	# timeout
	log "Install timeout[$timeout]!!!"
	kill -9 $pid
	return 1
}

#
# touch_device before uninstallation in case of "waiting for device"
#
# ret == 0 means uninstall success
# ret == 1 menas uninstall failed
safe_uninstall() # $SERIAL_NUMBER, $package
{
	SERIAL_NUMBER=$1
	package=$2
	ADB="adb -s $SERIAL_NUMBER"

	touch_device "$SERIAL_NUMBER"
	if [ $? -ne 0 ];then
		return 1
	fi	
	$ADB uninstall $package
	return $?
}

#
# check whether the target device is online
#
# ret == 0 means device is online
# ret == 1 means device is not online
touch_device() # $SERIAL_NUMBER
{
	SERIAL_NUMBER=$1
	ADB="adb -s $SERIAL_NUMBER"
	device=`$ADB get-state | grep device`
	if [ -z "$device" ]; then
		log "error: device[$SERIAL_NUMBER] not found"
		return 1
	fi  
	return 0
}

# e.g. timeout=`get_install_timeout $apk`
get_install_timeout() # $apk
{
	apk=$1

	if [ ! -e "$apk" ];then
		log "[$apk] does not exist!"
		return 1
	fi

	apksize=`ls -l "$apk" | awk '{print $5}'`
	apksize_m=`echo $apksize/1024/1024+5 | bc`
	timeoutValue=`echo $apksize_m*7 | bc`
	echo "$timeoutValue"
}

# "su -c" is not supported perfect by some phone, so this function put cmd in 
# script and execute the script by "su -c"
execute_with_root() # $serial_number, $cmd
{
	ADB="adb -s $1"
	cmd=$2

	# make sh
	rm -f su.sh
	echo '#!/system/bin/sh' > su.sh
	echo -e "$cmd" >> su.sh
	chmod +x su.sh

	# execute sh
	$ADB push su.sh /data/local/tmp
	$ADB shell su -c "/data/local/tmp/su.sh"
	rm su.sh
}

factory_reset_with_root() # $serial_number
{
	ADB="adb -s $1"

	execute_with_root "$1" '
	cd /cache \n
	rm -r recovery \n
	mkdir recovery \n
	cd recovery \n
	echo "--wipe_data" > command \n
	'
	echo "reboot for recovery..."
	$ADB reboot recovery
}

#
# mount system to rw
#
# ret == 0 means mount success
# ret == 1 means mount failed
mount_system_with_root() # $serial_number
{
	ADB="adb -s $1"

	path=`adb shell mount | grep system | awk '{print $1}'`
	execute_with_root "$1" "mount -o remount $path /system"
	ret=`adb shell mount | grep system | awk '{print $4}' | awk -F "," '{print $1}'`
	if [ "$ret" == "rw" ];then
		return 0
	else
		return 1
	fi
}

#
# connect wifi to special network
# 
# $network is in /data/misc/wifi/wpa_supplicant.conf 
# e.g.
# network={
# ssid="FreeAP06"
# key_mgmt=NONE
# priority=2
# }
connect_wifi_with_root() # $serial_number, $network
{
	serial_number=$1
	network=$2
	ADB="adb -s $serial_number"

	# pull wpa_supplicant.conf
	execute_with_root "$serial_number" '
	cp /data/misc/wifi/wpa_supplicant.conf /data/local/tmp/ \n
	chmod 777 /data/local/tmp/wpa_supplicant.conf
	'
	$ADB pull /data/local/tmp/wpa_supplicant.conf .

	# modify wpa_supplicant.conf
	sed -i '/network={/,/}/d' wpa_supplicant.conf
	echo "$network" >> wpa_supplicant.conf

	# push wpa_supplicant.conf
	$ADB push wpa_supplicant.conf /data/local/tmp
	rm wpa_supplicant.conf
	execute_with_root "$serial_number" '
	cp /data/local/tmp/wpa_supplicant.conf /data/misc/wifi/ \n
	chmod 660 /data/misc/wifi/wpa_supplicant.conf \n
	chown system.wifi /data/misc/wifi/wpa_supplicant.conf \n
	'

	echo "restart wifi service..."
	execute_with_root "$serial_number" '
	service call wifi 13 i32 0
	service call wifi 13 i32 1
	'	
}

#
# push $apk to /system/app, and reboot system for enable it.
#
set_system_app_with_root() # $serial_number, $apk
{
	serial_number=$1
	mount_system_with_root "$serial_number"
	adb -s $serial_number push $apk /system/app
	execute_with_root "$serial_number" '
	stop
	start
	'
	log "Waiting for Android Runtime reboot..."
	wait_for_system_boot_completed "$serial_number"
}

# ret == 0 means boot success
# ret == 1 means boot failed
# ret == 2 means no such device
wait_for_system_boot_completed() # $serial_number
{
	wait_for_boot_completed $1 "system"
	return $?
}

# transfer time to second
# NOTICE: This is a internal function for kill_by().
to_second() # $time_string
{
	time_string="$1"
	time_second=0
	day=0
	hour=0
	minute=0
	second=0

	has_day=`echo "$time_string" | grep -`
	if [ ! -z "$has_day" ];then
		day=`echo "$time_string" | awk -F "-" '{print $1}'`
		time_string=`echo "$time_string" | awk -F "-" '{print $2}'`
	fi
	OLD_IFS="$IFS"
	IFS=":"
	arr=($time_string)
	if [ ${#arr[*]} -eq 3 ];then
		hour=${arr[0]}
		minute=${arr[1]}
		second=${arr[2]}
	else
		minute=${arr[0]}
		second=${arr[1]}
	fi

	time_second=`echo $time_second+$second | bc`
	time_second=`echo $time_second+$minute*60 | bc`
	time_second=`echo $time_second+$hour*60*60 | bc`
	time_second=`echo $time_second+$day*60*60*24 | bc`

	# TODO: WHEN OVERFLOW ?
}

#
# Kill processes by name and timeout.
# NOTICE: Name can not be short than 4 char.
#
# ret == 1 means name is too short
# ret == 0 means success
kill_by() # $name $timeout
{
	name=$1
	timeout=$2
	target_count=0

	if [ ! -z "$name" ];then
		len=`expr length "$name"` 
		if [ $len -lt 4 ];then
			log "[$name] is too short. It can not be less than 4 char."
			return 1
		fi
	fi

	ps axo etime,pid,cmd | grep : | { # {} is very important for target_count works outside while block
	while read process
	do
		echo "$process" | grep -q "$name"
		if [ $? -ne 0 ];then
			continue
		fi
		run_time=`echo "$process" | awk '{print $1}'`
		# e.g. 4-04:42:18
		to_second "$run_time"
		if [ $time_second -gt $timeout ];then
			log "KILL [$process] for over time [$time_second]s!"
			pid_overtime=`echo "$process" | awk '{print $2}'`
			kill -9 $pid_overtime
			target_count=`expr $target_count + 1`
		fi
	done
	echo "$target_count processes name contain [$name] over [$timeout]s has been killed!"
}

return 0
}

#
# get package name of apk
#
# package name is in $ret_get_package_name
# ret == 1 means parameter error
# ret == 0 means success
get_package_name() # $apk
{
	apk=$1
	ret_get_package_name=""

	if [ ! -e "$apk" ];then
		cat "$apk"
		return 1
	fi
	ret_get_package_name=`aapt dump badging $apk | grep "package: name" | awk -F "'" '{print $2}'`
	return 0
}

#
# get launchable activity from apk
# 
# $1 is apk path
# $2 is whether getting short name of target activity
#
# launchable activity is in $ret_get_launchable_activity
# ret == 1 means error
# ret == 0 means success
get_launchable_activity() # $apk $isShort
{
	apk=$1
	isShort=$2
	ret_get_launchable_activity=""

	if [ ! -e "$apk" ];then
		cat "$apk"
		return 1
	fi
	if [ "$isShort" == true ] || [ "$isShort" == false ];then
		:
	else
		log "Parameter error!"
		log "isShort:$isShort"
		return 1
	fi

	xml=`aapt dump xmlstrings $apk AndroidManifest.xml`
	line=`echo "$xml" | grep -n "android.intent.action.MAIN" | awk -F ":" '{print $1}'`
	if [ -z "$line" ];then
		log "Can not find launchable activity!"
	fi

	############### xml example ##############
	# String #24: com.baidu.news.MainActivity
	# String #25: intent-filter
	# String #26: action
	# String #27: android.intent.action.MAIN
	##########################################
	# we need to get line 24 by line 27
	activity=`echo "$xml" | sed -n "$(($line - 3))"p | awk '{print $3}'`

	get_package_name "$apk"
	if [ -z "$ret_get_package_name" ];then
		log "Can not get package name of [$apk]!"
		return 1
	else
		package_name=$ret_get_package_name
	fi

	if [ "$isShort" == "true" ];then
		# start with .
		if [ "${activity:0:1}" == "." ];then
			ret_get_launchable_activity=$activity
		else
			# remove package name
			ret_get_launchable_activity=${activity##*$package_name}
		fi
	else
		if [ "${activity:0:1}" == "." ];then
			# add package name
			ret_get_launchable_activity=$package_name$activity
		else
			ret_get_launchable_activity=$activity
		fi
	fi
	#echo "$ret_get_launchable_activity"
	return 0
}

#
# try to install apk to device `$times` times
# $times is best to 2.
#
# ret == 0 means install success 
# ret == 1 means install failed 
reliable_install() # $SERIAL_NUMBER, $apk, $times
{
    SERIAL_NUMBER=$1
    apk=$2
    times=$3
    if [ -z "$3"] || [$times -lt 1 ];then
        log "Try times is set to [2] defaultly."
        times=2
    fi
    timeout=`get_install_timeout "$apk"`

    for((i=0;i<$times;i++));do
        safe_install $SERIAL_NUMBER $apk $timeout
        if [ 0 -ne $? ];then
            log "Install again!"
        else
            return 0
        fi
    done
    return 1
}

kill_android_process_by_name() # $serial_number, $name
{
    ADB="adb -s $1"
    name="$2"
	pid=`$ADB shell ps | grep "$name" | awk -F " " '{print $2}'`
	echo "#!/system/bin/sh" > kill_package.sh
	echo "kill -9 $pid" >> kill_package.sh
	chmod +x kill_package.sh
	$ADB push kill_package.sh /data/local/tmp > /dev/null 2>&1
    rm kill_package.sh
	run_with_timeout "$ADB shell su -c \"/data/local/tmp/kill_package.sh\"" 5
}

#
# Result is in $ret_display_width & $ret_display_height
#
get_display_width_height() # $serial_number
{
    ADB="adb -s $1"
    ret_display_width=""
    ret_display_height=""

    result_display=`$ADB shell dumpsys window | grep DisplayWidth`
    if [ -z "$result_display" ];then
        result_init=`$ADB shell dumpsys window | grep "Display.*init=" |\
            awk '{print $2}' | awk -F "=" '{print $2}'`
        ret_display_width=`echo $result_init | awk -F "x" '{print $1}'`
        ret_display_height=`echo $result_init | awk -F "x" '{print $2}'`
    else
        ret_display_width=`echo $result_display | awk '{print $1}' | awk -F "=" '{print $2}'`
        ret_display_height=`echo $result_display | awk '{print $2}' | awk -F "=" '{print $2}'`
    fi
}

#
# Result is in $ret_device_name
#
get_device_name() # $serial_number
{
    ret_device_name=`adb -s $1 shell getprop ro.product.model`
}

#
# transfer special char to json normal format
#
string_to_json_format() # $ret_json
{
    ret_json=$1
    ret_json=${ret_json//\\/\\\\} # \ 
    #ret_json=${ret_json//\//\\\/} # / 
    ret_json=${ret_json//\'/\\\'} # \' (not strictly needed ?)
    ret_json=${ret_json//\"/\\\"} # \" 
    ret_json=${ret_json//   /\\t} # \t (tab)
    ret_json=${ret_json///\\\n} # \n (newline)
    ret_json=${ret_json//^M/\\\r} # \r (carriage return)
    #ret_json=${ret_json//^L/\\\f} # \f (form feed)
    #ret_json=${ret_json//^H/\\\b} # \b (backspace)
}

#
# NOTICE: This function should be used after call cafe_setup.sh
# invoke api from Cafe.apk
#
invoke_cafe_api() # $serial_number $function $parameter $timeout
{
    ADB="adb -s $1"
    function="$2"
    parameter="$3"
    timeout="$4"

    if [ ! -z "$parameter" ];then
       parameter="-e parameter \"$parameter\""
    fi

    if [ -z "$4" ];then
        timeout=60
        log "timeout is set to 60s defaultly."
    fi

    $ADB shell service call window 1 i32 4939
    $ADB logcat -c
    $ADB shell am startservice -a com.baidu.cafe.remote.action.name.COMMAND \
        -e function "$function" $parameter
    
    # wait for invoking complete
    end=$((`date +%s` + $timeout))
	while [ $(date +%s) -le $end ]
	do
        completed=`$ADB logcat -d Arms:I *:S | grep "invoke completed"`
        if [ ! -z "$completed" ];then
            # get result
            $ADB logcat -d ArmsBinder:I *:S | \
                awk -F ":" '{for(i=2;i<NF;i++) printf "%s",$i;print $NF}'
            $ADB logcat -d Arms:I *:S | awk -F ":" '{print $2}'
            return
        fi
		sleep 1
	done
    echo "invoke_cafe_api timeout [$timeout]!"
}

#
# return cpu number
#
get_cpu_number()
{
    return `cat /proc/cpuinfo | grep processor | wc -l`
}

#
# Graphical tree of sub-directories
#
draw_sub_dir()
{
    ls -R | grep ":$" | sed -e 's/:$//' -e 's/[^-][^\/]*\//--/g' -e 's/^/   /' -e 's/-/|/'
}

#
# Define a quick calculator function
#
# Example:
# ? 1/8
#
? () 
{ 
    echo "$*" | bc -l;
}

#
# run a command and dump output to dump.png
#
dump_to_png()
{
    $* | convert label:@- dump.png
}

#
# Display a list of committers sorted by the frequency of commits
#
svn_sort_committer()
{
    svn log -q | grep "|" | awk "{print \$3}" | sort | uniq -c | sort -nr
}

#
count_code_line()
{
    find -name "*.$1" -exec wc -l {} \; | awk '{a += $1}END{print a}'
}

#
# rm .svn recursively
#
rm_svn()
{
    rm -rf `find . -type d -name .svn`
}

#
# start monkey server at port 4938
#
start_monkey_server() # $serial_number
{
    ADB="adb -s $1"

    kill_android_process_by_name "$1" monkey
    $ADB shell monkey --port 4938 --ignore-crashes --ignore-security-exceptions\
        --ignore-native-crashes -v -v > /dev/null 2>&1 &
    PID_MONKEY=$!
    end_with_script "$PID_MONKEY"
    echo ""
}

assert() # $value
{
    if [ ! 0 -eq $1 ]; then
        exit 1
    fi
}

#
# get apk certificate infomation from its classes.dex
#
# ret == 1 means failed 
# ret == 0 means success
#
get_apk_certs() # $apk
{
    apk=$1
    if [ -z "$apk" ];then
cat << !HELP
格式举例：X.509, CN=Jane Smith, OU=Java Software, O=Sun, L=cup, S=ca, C=us (jane)
CN 名字
OU 单位
O  组织
L  城市
S  州（省）
C  国家

注意：这些列有可能在生成签名的时候没有填写，所以可能为空。

s = signature was verified
m = entry is listed in manifest
k = at least one certificate was found in keystore
!HELP
        return 1
    fi
    info=`jarsigner -verify -verbose -certs $apk`
    ret_get_apk_certs=$?
    if [ $ret_get_apk_certs -ne 0 ];then
        echo "$info"
    else
        echo "$info" | grep classes.dex -A 3
    fi

    return $ret_get_apk_certs
}

#
# convert all pngs to gif in current dir
#
# NEEDED: which convert
#
# NOTICE: gif_name must be short name without directory name 
#
png2gif() # $gif_name
{
    convert -delay 100 -loop 0 *.png $1.gif
}

#
# convert gif to pngs under dir "pngs"
#
# NEEDED: which gif2png
#
# NOTICE: gif_name must be short name without directory name 
#
my_gif2png() # $gif_name
{
    gif_name=$1
    gif_name_prefix=${gif_name%.gif*}

    gif2png $gif_name

    # change name
    rm -rf pngs
    mkdir -p pngs
    mv $gif_name_prefix.p* pngs
    cd pngs
    ls *.p* | grep -v png | while read line
do
    new_name="$gif_name_prefix-${line##*p}.png"
    echo "mv $line to $new_name"
    mv $line "$new_name"
done
}

#
# $python_code must have a root dictionary named result
#
generate_json() # $filename $python_code
{
file_name=/tmp/`date +%s%N`.py
cat>$file_name<<EOF
#!/usr/bin/env python
import json
### json code ###
$2
### json code over ###
interjson = json.dumps(result)
rjson = None
try:
    rjson = open("$1",'w')
    rjson.write("%s" % interjson)
except IOError,e:
    pass
EOF
echo "python $file_name"
python $file_name
}

