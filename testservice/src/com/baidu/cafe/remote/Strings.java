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

package com.baidu.cafe.remote;

import java.util.ArrayList;

/**
 * A String util class.
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-21
 * @version
 * @todo
 */
public class Strings {
    public ArrayList<String> strings = null;

    public Strings(ArrayList<String> strings) {
        this.strings = strings;
    }

    public Strings grep(String str) {
        ArrayList<String> ret = new ArrayList<String>();
        for (String line : strings) {
            if (line.contains(str)) {
                ret.add(line);
            }
        }
        return new Strings(ret);
    }

    public Strings getRow(String regularExpression, int rowNumber)
            throws ArrayIndexOutOfBoundsException {
        ArrayList<String> ret = new ArrayList<String>();
        for (String line : strings) {
            String[] rows = line.trim().split(regularExpression);
            if (rows.length < rowNumber) {
                throw new ArrayIndexOutOfBoundsException(String.format(
                        "rows.length(%s) < rowNumber(%s) line:%s", rows.length, rowNumber, line));
            }
            ret.add(rows[rowNumber - 1]);
        }
        return new Strings(ret);
    }

    /**
     * e.g. transfer "767E" to "\u767E"
     * 
     * @param unicodeString
     * @return "" means failed
     */
    public static String unicodeStringToUnicode(String unicodeString) {
        try {
            char[] unicode = new char[unicodeString.length() / 4];
            for (int i = 0, j = 0; i < unicodeString.length(); i += 4, j++) {
                unicode[j] = (char) Integer.parseInt(unicodeString.substring(i, i + 4), 16);
            }
            return new String(unicode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
