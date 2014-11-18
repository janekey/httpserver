package com.janekey.http.util;

/**
 * User: p_qizheng
 * Date: 14-11-18
 * Time: 下午3:29
 */
public class Millisecond100Clock {

    private static final TimeProvider TIME_PROVIDER = new TimeProvider(100);
    static {
        TIME_PROVIDER.start();
    }

    public static long currentTimeMillis() {
        return TIME_PROVIDER.currentTimeMillis();
    }

    public static void stop() {
        TIME_PROVIDER.stop();
    }
}
