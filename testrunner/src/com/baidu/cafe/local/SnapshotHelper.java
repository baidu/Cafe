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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.webkit.WebView;

/**
 * @author yuboyang@baidu.com, luxiaoyu01@baidu.com
 * @date 2011-1-14
 * @version
 * @todo
 */
class SnapshotHelper {

    private static void print(String message) {
        if (Log.IS_DEBUG && message != null) {
            Log.i("SnapshotHelper", message);
        }
    }

    private static void outputToFile(String savePath, Bitmap bitmap) {
        print("savePath:" + savePath);
        FileOutputStream fos = null;
        try {
            fos = LocalLib.mInstrumentation.getTargetContext().openFileOutput(savePath,
                    Context.MODE_WORLD_READABLE);
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //            FileUtils.setPermissions(savePath, FileUtils.S_IRWXU | FileUtils.S_IRWXG
            //                    | FileUtils.S_IRWXO, -1, -1);
        }
    }

    /**
     * Take a whole snapshot of a WebView.
     * 
     * @param webView
     *            target webview
     * @param savePath
     *            e.g. /sdcard/webview.jpg
     */
    public static void takeWebViewSnapshot(WebView webView, String savePath) {
        Picture picture = webView.capturePicture();
        Bitmap bmp = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(),
                Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmp);
        picture.draw(c);
        outputToFile(savePath, bmp);
    }

    public static void dumpPic(WebView view, String filename) {
        Picture picture = view.capturePicture();
        print("[picture]width: " + picture.getWidth() + ", height:" + picture.getHeight()
                + "| [view]width:" + view.getWidth() + ", contentheight:" + view.getContentHeight());
        if (picture.getWidth() == 0 || picture.getHeight() == 0) {
            print("something error!");
            return;
        }

        int width, height;
        //        if (Build.VERSION.SDK_INT < 17) {
        float i = view.getScale();
        print("scale: " + i);
        width = (int) (picture.getWidth() * i);
        height = (int) (picture.getHeight() * i);
        //        } else {
        //            float sx = view.getScaleX();
        //            float sy = view.getScaleY();
        //            print("scaleX: " + sx + "scaleY: " + sy);
        //            width = (int) (picture.getWidth() * sx);
        //            height = (int) (picture.getHeight() * sy);
        //        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        //picture.draw( c );
        //c.drawPicture(picture);
        view.draw(c);
        outputToFile(filename, bitmap);
    }

    /**
     * Take a Snapshot of a View
     * 
     * @param view
     *            Target view
     * @param savePath
     *            Image Path
     */
    static public void takeViewSnapshot(final View view, final String savePath) {
        if (null == view) {
            print("null == view at takeViewSnapshot");
            return;
        }
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();

        if (null == bitmap) {
            print("null == bitmap at takeViewSnapshot");
            return;
        }
        if (LocalLib.mTheLastClick[0] != -1 && LocalLib.mTheLastClick[1] != -1) {
            bitmap = pressPointer(bitmap, LocalLib.mTheLastClick[0], LocalLib.mTheLastClick[1]);
            LocalLib.mTheLastClick[0] = -1;
            LocalLib.mTheLastClick[1] = -1;
        }
        outputToFile(savePath, bitmap);
        view.destroyDrawingCache();
    }

    /**
     * Take a Snapshot, default image is 320x480
     * 
     * @param savePath
     */
    static public void takeScreenshot(String savePath) {
        takeScreenshot(savePath, 320, 480);
    }

    static public void takeScreenshot(String savePath, int screenWidth, int screenHeight) {
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

    /**
     * press a pointer on a image
     * 
     * @param canvas
     * @param small
     * @param x
     * @param y
     * @param alpha
     * @return
     */
    public static Bitmap pressPointer(Bitmap bitmap, int x, int y) {
        try {
            String pngPath = LocalLib.mInstrumentation.getTargetContext().getFilesDir().toString()
                    + "/pointer.png";
            InputStream in = new FileInputStream(pngPath);
            Bitmap pointer = new BitmapDrawable(in).getBitmap();
            Canvas canvas = new Canvas(bitmap);
            int offsetX = 21;
            int offsetY = 21;
            canvas.drawBitmap(pointer, x - offsetX, y - offsetY, null);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
