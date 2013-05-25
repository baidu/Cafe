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
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * One MonkeyNetwork object can only send one monkey event!
 * 
 * @author chengzhenyu@baidu.com
 * @date 2012-3-30
 * @version
 * @todo
 */
public class MonkeyNetwork {
    public final static String  DOWN             = "down";
    public final static String  MOVE             = "move";
    public final static String  UP               = "up";

    private final static String MONKEY_SERVER_IP = "127.0.0.1";
    private final static int    MONKEY_PORT      = 4938;
    private final static int    EVENT_TIMEOUT    = 10 * 1000;
    private BufferedReader      mIn              = null;
    private BufferedWriter      mOut             = null;
    private Socket              mSocket          = null;
    private boolean             mIsDone          = false;

    public MonkeyNetwork() {
    }

    /**
     * Command to send touch events to the input system. format: touch
     * [down|up|move] [x] [y] example: touch down 120 120; touch move 140 140;
     * touch up 140 140
     * 
     * @param type
     *            touch type: down, up, or move
     * @param x
     *            x-coordinates
     * @param y
     *            y-coordinates
     */
    public void touch(String type, int x, int y) {
        String command = String.format("%s %s %s %s", "touch", type, x, y);
        sendCommand(command);
    }

    /**
     * Command to send Key events to the input system. format: key [down|up]
     * [keycode] example:key down 82 key up 82
     * 
     * @param type
     *            key type: down or up
     * @param keyCode
     *            key code
     */
    public void key(String type, int keyCode) {
        String command = String.format("%s %s %s", "key", type, keyCode);
        sendCommand(command);
    }

    public void type(String str) {
        String command = String.format("%s %s", "type", str);
        sendCommand(command);
    }

    public void done() {
        sendCommand("done");
    }

    private void sendCommand(final String command) {
        new Thread(new Runnable() {
            public void run() {
                mSocket = new Socket();
                try {
                    mSocket.connect(new InetSocketAddress(MONKEY_SERVER_IP, MONKEY_PORT));
                    mOut = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                    mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream(),
                            "utf-8"));
                    if (mOut == null || mIn == null) {
                        Log.print("ERROR! mOut or mIn is null.");
                        return;
                    }
                    mOut.write(command);
                    mOut.newLine();
                    mOut.flush();

                    while (true) {
                        String line;
                        if ((line = mIn.readLine()) == null || "OK".equalsIgnoreCase(line)) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (mSocket != null) {
                        try {
                            mSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mIsDone = true;
                }
            }
        },"sendCommandToMonkeyServer").start();
        waitForDone();
    }

    private void waitForDone() {
        long timeout = System.currentTimeMillis() + EVENT_TIMEOUT;
        while (true) {
            try {
                if (mIsDone) {
                    break;
                }
                if (System.currentTimeMillis() > timeout) {
                    Log.print("waitForDone timeout !");
                    break;
                }
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Start monkey requires android.permission.SET_ACTIVITY_WATCHER which is owned by shell not app_xx
    private void start(final int port) {
        new Thread(new Runnable() {
            public void run() {
                // ShellExecute.execute(new String[] { "monkey", "--port", String.format("%s", port), "-v", "-v" }, "/");
            }
        }).start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
