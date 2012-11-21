package com.baidu.cafe.local;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-21
 * @version
 * @todo
 */
public class NetworkUtils {

    private static void print(String message) {
        if (Log.IS_DEBUG && message != null) {
            Log.i("SnapshotHelper", message);
        }
    }

    public int getUidByPid(int pid) {
        int uid = -1;
        try {
            String uidString = new ShellExecute().execute(String.format("cat /proc/%s/status", pid), "/").grep("Uid")
                    .get(0);
            uid = Integer.valueOf(uidString.split(" ")[1]);
        } catch (Exception e) {
            print("Get uid failed!");
            e.printStackTrace();
        }
        return uid;
    }

    public int getProcessRcv(int pid) {
        int rcv = 0;
        return rcv;
    }
}
