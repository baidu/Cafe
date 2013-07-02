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

package com.baidu.cafe.local.traveler;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Random;

import android.graphics.Bitmap;
import android.view.View;

import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.utils.ReflectHelper;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-12-26
 * @version
 * @todo
 */
public class Util {
    private final static String BASE = "abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * @param <T>
     * @param array1
     * @param array2
     * @return difference set of two arraies
     */
    public static <T> ArrayList<T> subArray(ArrayList<T> array1, ArrayList<T> array2) {
        ArrayList<T> array = new ArrayList<T>();
        array.addAll(array1);

        for (T t : array2) {
            if (array.contains(t)) {
                array.remove(t);
            } else {
                array.add(t);
            }
        }

        return array;
    }

    public interface Equal<T> {
        boolean isEqual(T t1, T t2);
    }

    /**
     * @param <T>
     * @param arrayList1
     * @param arrayList2
     * @param equal
     * @return
     */
    public static <T> boolean isArraySame(AbstractList<T> arrayList1, AbstractList<T> arrayList2,
            Equal<T> equal) {
        if (arrayList1.size() != arrayList2.size()) {
            return false;
        }

        for (int i = 0; i < arrayList1.size(); i++) {
            if (!equal.isEqual(arrayList1.get(i), arrayList2.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param view
     * @return
     */
    public static Bitmap getViewSnapshot(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        return view.getDrawingCache();
    }

    public static String getReturnValueName(Class<?> targetClass, int ret) {
        try {
            ArrayList<String> names = ReflectHelper.getFieldNameByValue(targetClass.newInstance(),
                    null, int.class, ret);
            for (String str : names) {
                if (str.startsWith("RET")) {
                    return str;
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double getDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.abs(x1 - x2) * Math.abs(x1 - x2) + Math.abs(y1 - y2)
                * Math.abs(y1 - y2));
    }

    public static String getRandomString(int length) {
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(BASE.charAt(random.nextInt(BASE.length())));
        }
        return sb.toString();
    }
}
