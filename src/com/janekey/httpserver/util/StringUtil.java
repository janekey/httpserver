package com.janekey.httpserver.util;

/**
 * User: janekey
 * Date: 14-11-19
 * Time: 上午11:26
 */
public class StringUtil {

    public static boolean isNotEmpty(String str) {
        if (str != null && str.length() > 0) {
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        return !isNotEmpty(str);
    }

}
