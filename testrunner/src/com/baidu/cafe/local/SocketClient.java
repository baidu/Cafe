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

package com.baidu.cafe.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * @author zhouzheng03@baidu.com
 * 
 */
public class SocketClient {
    private Socket              socket = null;
    private PrintWriter         out    = null;
    private BufferedReader      in     = null;

    private static final String TAG    = "SocketClient";

    /**
     * constructor
     * 
     * @param sock
     */
    public SocketClient(Socket sock) {
        this.socket = sock;
    }

    /**
     * initial
     * 
     * @throws IOException
     */
    private void init() throws IOException {
        if (out == null) {
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        if (in == null) {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    /**
     * send msg here
     * 
     * @param msg
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    public String sendMsg(String... msgs) throws IOException {
        Log.i(TAG, "send msg begin.");
        init();
        for (String msg : msgs) {
            Log.i(TAG, "send msg " + msg);
            out.println(msg);
            out.flush();
        }

        String resp = in.readLine(); // just read one line only,ok?
        Log.i(TAG, "send msg end. response : " + resp.toString());
        return resp;
    }

    /**
     * close all
     */
    public void close() {
        if (out != null) {
            out.close();
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
