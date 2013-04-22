/*
 * Copyright (C) 2011 Baidu.com Inc
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

package com.baidu.cafe.remote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-17
 * @version
 * @todo
 */
public class Client {
    private String           mServerIP = null;
    private final static int TIMEOUT   = 3;

    /**
     * @param serverIP
     *            IP of the server which client talks with
     */
    public Client(String serverIP) {
        boolean isReachable = false;
        try {
            isReachable = InetAddress.getByName(serverIP).isReachable(TIMEOUT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isReachable) {
                mServerIP = serverIP;
                Log.d("Client", serverIP + " is reachable!");
            }
        }
    }

    /**
     * @param command
     *            a command string which is run on server
     * @return the result string of the command
     */
    public String runCmdOnServer(String command) {
        Socket socket = null;
        String ret = null;
        PrintWriter out = null;
        BufferedReader in = null;

        Log.d("Client", "run [" + command + "] at " + mServerIP);

        try {
            socket = new Socket(InetAddress.getByName(mServerIP), 7777);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // write to server
            out.println(command);
            // read result string from server
            ret = in.readLine();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }
}
