package com.baidu.cafe.local;

import java.util.ArrayList;

/**
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

    public Strings getRow(String regularExpression, int rowNumber) throws ArrayIndexOutOfBoundsException {
        ArrayList<String> ret = new ArrayList<String>();
        for (String line : strings) {
            String[] rows = line.split(regularExpression);
            if (rows.length < rowNumber) {
                throw new ArrayIndexOutOfBoundsException(String.format("rows.length(%s) < rowNumber(%s) line:%s",
                        rows.length, rowNumber, line));
            }
            ret.add(rows[rowNumber - 1]);
        }
        return new Strings(ret);
    }
}
