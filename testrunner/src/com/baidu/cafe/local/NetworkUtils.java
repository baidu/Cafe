/*
 * Copyright (C) 2012 Baidu.com Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.cafe.local;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Build;

import com.baidu.cafe.utils.ShellExecute;
import com.baidu.cafe.utils.ShellExecute.CallBack;
import com.baidu.cafe.utils.Strings;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-21
 * @version
 * @todo
 */
class NetworkUtils {
    private final static String   MODE_RCV           = "tcp_rcv";
    private final static String   MODE_SND           = "tcp_snd";
    private final static String[] NETWORK_CARD_TYPES = new String[] { "eth0:", "wlan0:",
            "tiwlan0:", "svnet0:", "rmnet0:", "mlan0:" };

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
            ArrayList<String> uidString = new ShellExecute().execute(
                    String.format("cat /proc/%s/status", pid), "/").console.grep("Uid").strings;
            uid = Integer.valueOf(uidString.get(0).split("\t")[1]);
        } catch (Exception e) {
            print("Get uid failed!");
            e.printStackTrace();
        }
        return uid;
    }

    private static int getPidRowNumber() {
        String psHead = new ShellExecute().execute("ps", "/").console.strings.get(0);
        String[] psHeadRow = psHead.split(" ");
        int rowNumber = 0;
        for (int i = 0; i < psHeadRow.length; i++) {
            if ("".equals(psHeadRow[i])) {
                continue;
            }
            rowNumber++;
            if ("PID".equals(psHeadRow[i])) {
                //                print("PID ROW NUMBER: " + rowNumber);
                return rowNumber;
            }
        }
        return 0;
    }

    public static ArrayList<Integer> getPidsByPackageName(String packageName) {
        int pidRowNumber = getPidRowNumber();
        if (pidRowNumber == 0) {
            print("pidRowNumber failed!");
        }
        ArrayList<Integer> pids = new ArrayList<Integer>();
        ArrayList<String> pidStrings = new ShellExecute().execute("ps", "/").console.grep(
                packageName).getRow("\\s{1,}", pidRowNumber).strings;
        for (String pid : pidStrings) {
            try {
                pids.add(Integer.valueOf(pid));
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            ArrayList<String> ret = new ShellExecute().execute(
                    String.format("cat /proc/uid_stat/%s/%s", uid, mode), "/").console.strings;
            if (ret.size() > 0) {
                traffic = Integer.valueOf(ret.get(0));
            } else {
                print(String.format("Failed: cat /proc/uid_stat/%s/%s", uid, mode));
            }
        } else {
            Strings netString = new ShellExecute().execute(
                    String.format("cat /proc/%s/net/dev", pid), "/").console;
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

    public static int getPackageRcv(final String packageName) {
        Integer ret = ShellExecute.doInTimeout(new CallBack<Integer>() {
            @Override
            public Integer runInTimeout() throws InterruptedException {
                return getPackageTraffic(packageName, MODE_RCV);
            }
        }, 1000);
        if (null == ret) {
            print("getPackageRcv timeout over 1000 !!!");
            return 0;
        }
        return ret;
    }

    public static int getPackageSnd(final String packageName) {
        Integer ret = ShellExecute.doInTimeout(new CallBack<Integer>() {
            @Override
            public Integer runInTimeout() throws InterruptedException {
                return getPackageTraffic(packageName, MODE_SND);
            }
        }, 1000);
        if (null == ret) {
            print("getPackageSnd timeout over 1000 !!!");
            return 0;
        }
        return ret;
    }

    /**
     * download via a url
     * 
     * @param url
     * @param outputStream
     *            openFileOutput("networktester.download",
     *            Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE)
     */
    public static void httpDownload(String url, OutputStream outputStream) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpClientParams.setCookiePolicy(httpClient.getParams(),
                    CookiePolicy.BROWSER_COMPATIBILITY);
            httpClient.execute(new HttpGet(url)).getEntity().writeTo(outputStream);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
