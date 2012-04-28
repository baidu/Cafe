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

package com.baidu.arms.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-30
 * @version
 * @todo
 */
public class ShellExecute {

    /**
     * execute shell command on device
     * 
     * @param command
     *            e.g. "ls -l"
     * @param directory
     *            the directory where the command is executed. e.g. "/sdcard/"
     * @return ret of the command
     */
    public static String execute(String[] command, String directory) {
        StringBuilder result = new StringBuilder("");
        Log.d("ShellExecute", arrayToString(command));

        BufferedReader in = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);

            if (directory != null) {
                builder.directory(new File(directory));
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
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

        return result.toString();
    }

    private static String arrayToString(String[] array) {
        StringBuffer sb = new StringBuffer();
        for (String s : array) {
            sb.append(s + " ");
        }
        return sb.toString();
    }

}
