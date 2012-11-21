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
    private final static String MODE_RCV = "tcp_rcv";
    private final static String MODE_SND = "tcp_snd";

    public NetworkUtils() {

    }

    private static void print(String message) {
        if (Log.IS_DEBUG && message != null) {
            Log.i("SnapshotHelper", message);
        }
    }

    public int getUidByPid(int pid) {
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

    public ArrayList<Integer> getPidsByPackageName(String packageName) {
        ArrayList<Integer> pids = new ArrayList<Integer>();
        ArrayList<String> pidStrings = new ShellExecute().execute("ps", "/").console.grep(packageName).getRow("\t", 2).strings;
        for (String pid : pidStrings) {
            pids.add(Integer.valueOf(pid));
        }

        return pids;
    }

    /**
     * @param packageName
     * @return
     */
    private int getPackageTraffic(String packageName, String mode) {
        int traffic = 0;
        int uid = getUidByPid(getPidsByPackageName(packageName).get(0));
        if (-1 == uid) {
            return -1;
        }

        if (Build.VERSION.SDK_INT >= 14) {// API Level: 14. Android 4.0
            String ret = new ShellExecute().execute(String.format("cat /proc/uid_stat/%s/%s", uid, mode), "/").console.strings
                    .get(0);
            traffic = Integer.valueOf(ret);
        }
        return traffic;
    }

    public int getPackageRcv(String packageName) {
        return getPackageTraffic(packageName, MODE_RCV);
    }

    public int getPackageSnd(String packageName) {
        return getPackageTraffic(packageName, MODE_SND);
    }
}
