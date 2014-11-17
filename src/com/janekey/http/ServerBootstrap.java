package com.janekey.http;

import com.janekey.http.net.Acceptor;
import com.janekey.http.net.Filter;
import com.janekey.http.net.Handler;
import com.janekey.http.net.Session;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * User: p_qizheng
 * Date: 14-11-14
 * Time: 上午11:51
 */
public class ServerBootstrap {

    public static void main(String[] args) {
        Acceptor acceptor = new Acceptor("httpserver",
                new Filter() {
                    @Override
                    public void decode(Session session, ByteBuffer buffer) {
                        while (buffer.hasRemaining())
                            System.out.print((char) buffer.get());
//                        System.out.println();
                    }

                    @Override
                    public void encode(Session session, ByteBuffer buffer) {

                    }
                }, new Handler(), new InetSocketAddress(12345));
        acceptor.listen();
    }

}
