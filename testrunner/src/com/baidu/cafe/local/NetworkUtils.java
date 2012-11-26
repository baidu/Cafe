package com.baidu.cafe.local;

import java.util.ArrayList;

import android.os.Build;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-21
 * @version
 * @todo
 */
public class NetworkUtils {
    private final static String   MODE_RCV           = "tcp_rcv";
    private final static String   MODE_SND           = "tcp_snd";
    private final static String[] NETWORK_CARD_TYPES = new String[] { "eth0:", "wlan0:", "tiwlan0:", "svnet0:",
            "rmnet0:", "mlan0:"                     };

    public NetworkUtils() {

    }

    private static void print(String message) {
        if (Log.IS_DEBUG && message != null) {
            Log.i("NetworkUtils", message);
        }
    }

    public static int getUidByPid(int pid) {
        int uid = -1;
        try {
            ArrayList<String> uidString = new ShellExecute().execute(String.format("cat /proc/%s/status", pid), "/").console
                    .grep("Uid").strings;
            uid = Integer.valueOf(uidString.get(0).split("\t")[1]);
        } catch (Exception e) {
            print("Get uid failed!");
            e.printStackTrace();
        }
        return uid;
    }

    public static ArrayList<Integer> getPidsByPackageName(String packageName) {
        ArrayList<Integer> pids = new ArrayList<Integer>();
        ArrayList<String> pidStrings = new ShellExecute().execute("ps", "/").console.grep(packageName).getRow(
                "\\s{1,}", 2).strings;
        for (String pid : pidStrings) {
            pids.add(Integer.valueOf(pid));
        }

        return pids;
    }

    /**
     * invoked 100 times costs 4800ms on Nexus One
     * 
     * @param packageName
     * @param mode
     * @return
     */
    private static int getPackageTraffic(String packageName, String mode) {
        if ((!MODE_RCV.equals(mode)) && (!MODE_SND.equals(mode))) {
            print("mode invaild:" + mode);
            return -1;
        }

        int traffic = 0;
        ArrayList<Integer> pids = getPidsByPackageName(packageName);
        if (pids.size() < 1) {
            print("pids.size() < 1;get pids by [" + packageName + "] failed");
            return -1;
        }
        int pid = pids.get(0);

        if (Build.VERSION.SDK_INT >= 14) {// API Level: 14. Android 4.0
            int uid = getUidByPid(pid);
            if (-1 == uid) {
                print("-1 == uid");
                return -1;
            }
            String ret = new ShellExecute().execute(String.format("cat /proc/uid_stat/%s/%s", uid, mode), "/").console.strings
                    .get(0);
            traffic = Integer.valueOf(ret);
        } else {
            Strings netString = new ShellExecute().execute(String.format("cat /proc/%s/net/dev", pid), "/").console;
            int rcv = 0;
            int snd = 0;
            for (String networkCard : NETWORK_CARD_TYPES) {
                Strings netLine = netString.grep(networkCard);
                if (netLine.strings.size() != 1) {
                    continue;
                }
                rcv += Integer.valueOf(netLine.getRow("\\s{1,}", 2).strings.get(0));
                snd += Integer.valueOf(netLine.getRow("\\s{1,}", 10).strings.get(0));
            }
            if (MODE_RCV.equals(mode)) {
                traffic = rcv;
            } else if (MODE_SND.equals(mode)) {
                traffic = snd;
            }
        }
        return traffic;
    }

    public static int getPackageRcv(String packageName) {
        return getPackageTraffic(packageName, MODE_RCV);
    }

    public static int getPackageSnd(String packageName) {
        return getPackageTraffic(packageName, MODE_SND);
    }
}
