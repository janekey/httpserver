package com.janekey.httpserver.net;

/**
 * User: janekey
 * Date: 14-11-17
 * Time: 上午10:19
 */
public class Logger {

    public static void log(Object ... obj) {
        if (obj != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(obj[0]);
            if (obj.length > 1) {
                for (int i = 1; i < obj.length; i++)
                    sb.append("|").append(obj[i]);
            }
            System.out.println(sb.toString());
        }
    }

    public static void log(Throwable t, Object ... obj) {
        log(obj);
        t.printStackTrace();
    }

}
