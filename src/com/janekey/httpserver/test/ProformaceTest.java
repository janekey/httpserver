package com.janekey.httpserver.test;

/**
 * User: janekey
 * Date: 14-11-20
 * Time: 下午6:30
 */
public class ProformaceTest {

    public static void main(String[] args) {
        String url = "http://localhost:12345/";
        try {
            HttpClient httpClient = new HttpClient(url);
            String content = httpClient.get();
            System.out.println(content);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

}
