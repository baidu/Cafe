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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class FileUtil {
    private static final String TAG = "FileUtil";

    //   /mnt/sdcard/DCIM/ScreenShot/SCR_20111010_162653.png
    public static ArrayList<String> listDir(String directory, final String extension) {
        if (null == directory) {
            Log.e(TAG, "parameter is null");
            return null;
        }

        File dir = new File(directory);

        if (false == dir.exists()) {
            Log.d(TAG, "start to create " + dir.getAbsolutePath());
            dir.mkdir();
        }

        if (false == dir.exists()) {
            Log.e(TAG, "cannot access " + dir.getAbsolutePath());
            return null;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (null == extension) {
                    return new File(dir.getAbsoluteFile() + "/" + name).isFile();
                } else {
                    if (matchExtension(name, extension))
                        return true;
                    else
                        return false;
                }
            }
        };
        String[] files = (filter == null ? dir.list() : dir.list(filter));
        java.util.Arrays.sort(files);
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < files.length; ++i)
            list.add(dir.getAbsolutePath() + "/" + files[i]);
        return list;
    }

    public static ArrayList<String> getDiff(ArrayList<String> large, ArrayList<String> small) {
        ArrayList<String> result = new ArrayList<String>();
        if (large == null && small == null) {
            return null;
        } else if (large == null) {
            return (ArrayList<String>) small.clone();
        } else if (small == null) {
            return (ArrayList<String>) large.clone();
        }
        for (int i = 0; i < large.size(); i++) {
            if (!small.contains(large.get(i))) {
                result.add(large.get(i));
            }
        }
        return result;
    }

    private static boolean matchExtension(String filename, String regexExt) {
        if (null == filename || null == regexExt) {
            Log.e(TAG, "parameter is null");
            return false;
        }

        if (filename.length() < 4) {
            Log.e(TAG, "filename is invalid (" + filename + ")");
            return false;
        }

        String extension = filename.substring(filename.length() - 3, filename.length());
        Pattern pattern = Pattern.compile(regexExt);
        Matcher matcher = pattern.matcher(extension);

        return matcher.find();
    }

    public static boolean rename(String filename, String prefix) {
        File file = new File(filename);
        if (!file.exists())
            return false;
        String newname = file.getParent() + "/" + prefix + "_" + file.getName();
        file.renameTo(new File(newname));
        return true;
    }
}
