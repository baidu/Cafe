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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.view.View;
import android.graphics.Bitmap;

/**
 * @author yuboyang@baidu.com
 * @date 2011-1-14
 * @version
 * @todo
 */
public class SnapshotHelper {

    private static void print(String message) {
        if (Log.IS_DEBUG && message != null) {
            Log.i("SnapshotHelper", message);
        }
    }

    private static void outputToFile(String savePath, Bitmap bitmap) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(savePath);
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            }
        } catch (Exception e) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e2) {
                }
            }
            FileUtils.setPermissions(savePath, FileUtils.S_IRWXU | FileUtils.S_IRWXG
                    | FileUtils.S_IRWXO, -1, -1);
        }
    }

    /**
     * Take a Snapshot of a View
     * 
     * @param view
     *            Target view
     * @param savePath
     *            Image Path
     */
    static public void takeViewSnapshot(View view, String savePath) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();

        outputToFile(savePath, bitmap);
    }

    /**
     * Take a Snapshot, default image is 320x480
     * 
     * @param savePath
     */
    static public void takeSnapshot(String savePath) {
        takeSnapshot(savePath, 320, 480);
    }

    static public void takeSnapshot(String savePath, int screenWidth, int screenHeight) {
        savePath += ".jpg";
        print(savePath + " " + screenWidth + " " + screenHeight);

        File fileSrc = null;
        FileInputStream in = null;
        try {
            fileSrc = new File("/dev/graphics/fb0");
            in = new FileInputStream(fileSrc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        byte buffer[] = new byte[1000000];

        if (in == null) {
            return;
        }

        try {
            in.read(buffer);
        } catch (IOException e) {
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

        int[] colors = new int[500000];
        int r = 0, g = 0, b = 0;
        int ii = 0;
        for (int i = 0; i < screenWidth * screenHeight; i++) {
            ii = i * 2;
            r = (buffer[ii + 1] & 0xf8) << 16;
            g = (((buffer[ii + 1] & 0x7) << 3) | ((buffer[ii] & 0xe0) >> 5)) << 10;
            b = (buffer[ii] & 0x1f) << 3;
            colors[i] = r | g | b;
        }

        Bitmap bitmap = Bitmap.createBitmap(colors, screenWidth, screenHeight,
                Bitmap.Config.RGB_565);
        outputToFile(savePath, bitmap);
    }
}
