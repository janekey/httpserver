package com.janekey.httpserver.http;

import com.janekey.httpserver.net.Filter;
import com.janekey.httpserver.net.Handler;
import com.janekey.httpserver.net.Session;

import java.nio.ByteBuffer;

/**
 * User: janekey
 * Date: 14-11-18
 * Time: 下午4:54
 */
public class HttpFilter implements Filter {

    private HttpDecoder httpDecoder = new HttpDecoder();

    @Override
    public void decode(Session session, ByteBuffer buffer, Handler handler) throws Throwable {
        httpDecoder.decode(buffer, session, handler);
    }

    @Override
    public void encode(Session session, ByteBuffer buffer) {

    }
}
