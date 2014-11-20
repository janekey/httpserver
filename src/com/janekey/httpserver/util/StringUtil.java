package com.janekey.httpserver.util;

import java.nio.charset.Charset;

/**
 * User: janekey
 * Date: 14-11-19
 * Time: 上午11:26
 */
public class StringUtil {

    /**UTF-8的Charset*/
    public static final Charset UTF_8 = Charset.forName("UTF-8");

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

    public static byte[] getUTF8Bytes(String s) {
        if (s != null && s.length() >= 0) {
            return s.getBytes(UTF_8);
        }
        return null;
    }

}
