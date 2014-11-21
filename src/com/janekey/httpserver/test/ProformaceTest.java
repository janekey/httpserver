package com.janekey.httpserver.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: janekey
 * Date: 14-11-20
 * Time: 下午6:30
 */
public class ProformaceTest {
    static String url = "http://10.3.145.6:12345/";
//    static String url = "http://www.qq.com/";
    static AtomicLong num = new AtomicLong(0);
    static AtomicLong num2 = new AtomicLong(0);

    public static void main(String[] args) {
        int threadSize = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(threadSize);
        for (int i = 0; i < threadSize; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                    try {
                        HttpClient httpClient = new HttpClient(url);
                        String content = httpClient.get();
                        if (!"this is a page".equals(content)) {
                            System.out.println("failed(" + num.incrementAndGet() + "):" + content);
                        }
                    } catch (Throwable th) {
                        System.out.println("error:" + num.incrementAndGet());
                        th.printStackTrace();
                    }
                    }
                }
           });
        }
    }

}
