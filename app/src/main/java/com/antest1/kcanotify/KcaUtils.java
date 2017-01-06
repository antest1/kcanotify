package com.antest1.kcanotify;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by Gyeong Bok Lee on 2017-01-07.
 */

public class KcaUtils {
    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ");
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }
}
