package com.janekey.httpserver;

import com.janekey.httpserver.net.Acceptor;
import com.janekey.httpserver.net.Filter;
import com.janekey.httpserver.net.Handler;
import com.janekey.httpserver.net.Session;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * User: janekey
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
                        System.out.println();

                        ByteBuffer bb = ByteBuffer.allocate(100);
                        bb.put("new=".getBytes());
                        bb.flip();
                        session.write(bb);
                    }

                    @Override
                    public void encode(Session session, ByteBuffer buffer) {

                    }
                }, new Handler(), new InetSocketAddress(12345));
        acceptor.listen();
    }

}
