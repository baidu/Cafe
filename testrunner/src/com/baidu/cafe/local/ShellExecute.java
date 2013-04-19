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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-30
 * @version
 * @todo
 */
public class ShellExecute {

    public class CommandResult {
        public int     ret     = 0;
        public Strings console = new Strings(new ArrayList<String>());

        public CommandResult() {
        }

    }

    /**
     * execute shell command on device
     * 
     * @param command
     *            e.g. "ls -l"
     * @param directory
     *            the directory where the command is executed. e.g. "/sdcard/"
     * @return std of the command
     */
    public CommandResult execute(String command, String directory) {
        CommandResult cr = new CommandResult();
        BufferedReader in = null;
        try {
            Process process = Runtime.getRuntime().exec(command, null, new File(directory));
            cr.ret = process.waitFor();

            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                cr.console.strings.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return cr;
    }

    public interface CallBack<T> {
        T runInTimeout() throws InterruptedException;
    }

    private final static long INTERVAL = 50;

    /**
     * perform a function in timeout; return function's return value if function
     * is over in timeout, or else return null
     * 
     * @param <T>
     * @param callBack
     * @param timeout
     * @return null means reach timeout;
     */
    public static <T> T doInTimeout(CallBack<T> callBack, long timeout) {
        final CallBack<T> fCallBack = callBack;
        ExecutorService exs = Executors.newCachedThreadPool();

        Future<T> future = exs.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    return fCallBack.runInTimeout();
                } catch (InterruptedException e) {
                    print("Timeout: Exiting by Exception");
                }

                return null;
            }
        });

        long end = System.currentTimeMillis() + timeout;

        while (true) {
            if (null == future) {
                return null;
            }

            if (System.currentTimeMillis() > end) {
                future.cancel(true);
                return null;
            }

            try {
                if (future.isDone()) {
                    return future.get();
                }
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public CommandResult execute(final String command, final String directory, long timeout) {
        CommandResult ret = (CommandResult) doInTimeout(new CallBack<CommandResult>() {
            @Override
            public CommandResult runInTimeout() throws InterruptedException {
                return execute(command, directory);
            }
        }, timeout);
        return ret;
    }

    private static void print(String message) {
        if (Log.IS_DEBUG) {
            Log.i("ShellExecute", message);
        }
    }

}
