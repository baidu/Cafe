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

import java.util.ArrayList;

import android.view.View;
import android.view.ViewTreeObserver;

/**
 * @author ranfang@baidu.com, luxiaoyu01@baidu.com
 * @date 2012-9-17
 * @version
 * @todo
 */
class FPSTracer {
    private final static int INTERVAL      = 1000;
    private static long      mFpsStartTime = -1;
    private static long      mFpsPrevTime  = -1;
    private static int       mFpsNumFrames = 0;
    private static float     mTotalFps     = 0;
    private static int       mFpsCount     = 0;

    public static void trace(final LocalLib local) {
        final boolean threadDisable = true;
        new Thread(new Runnable() {

            @Override
            public void run() {
                int time = 0;
                ArrayList<View> decorViews = new ArrayList<View>();
                while (threadDisable) {
                    time++;
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        // eat it
                    }

                    View[] windowDecorViews = local.getWindowDecorViews();
                    if (0 == windowDecorViews.length) {
                        continue;
                    }
                    View decorView = windowDecorViews[0];
                    if (!decorViews.contains(decorView)) {
                        print("add listener at " + decorView);
                        ViewTreeObserver observer = decorView.getViewTreeObserver();
                        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                            @Override
                            public boolean onPreDraw() {
                                countFPS();
                                return true;
                            }
                        });
                        decorViews.add(decorView);
                    }

                    // print fps average 1s
                    float averageFPS = 0 == mFpsCount ? 0 : mTotalFps / mFpsCount;
                    print(time + "s: " + averageFPS);
                    modifyFPS(-1);
                    modifyFPSCount(0);
                }
            }

        }, "trace").start();
    }

    private static void countFPS() {
        long nowTime = System.currentTimeMillis();
        if (mFpsStartTime < 0) {
            mFpsStartTime = mFpsPrevTime = nowTime;
            mFpsNumFrames = 0;
        } else {
            long frameTime = nowTime - mFpsPrevTime;
            //            print("Frame time:\t" + frameTime);
            mFpsPrevTime = nowTime;
            int interval = 1000;
            if (frameTime < interval) {
                float fps = (float) 1000 / frameTime;
                //                print("FPS:\t" + fps);
                modifyFPS(fps);
                modifyFPSCount(-1);
            } else {
                // discard frameTime > interval
            }
        }
    }

    /**
     * @param fps
     *            -1 means reset mFps to 0; otherwise means add fps to mFps
     */
    synchronized private static void modifyFPS(float fps) {
        if (-1 == fps) {
            mTotalFps = 0;
        } else {
            mTotalFps += fps;
        }
    }

    /**
     * @param count
     *            -1 means mFpsCount increase; otherwise means set mFpsCount to
     *            count
     */
    synchronized private static void modifyFPSCount(int count) {
        if (-1 == count) {
            mFpsCount++;
        } else {
            mFpsCount = count;
        }
    }

    private static void print(String msg) {
        if (Log.IS_DEBUG) {
            Log.i("FPS", msg);
        }
    }
}
