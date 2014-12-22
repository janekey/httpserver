package com.janekey.httpserver.net;

import java.nio.ByteBuffer;

/**
 * User: janekey
 * Date: 14-11-14
 * Time: 下午5:21
 */
public interface Filter {

    /**
     * 解码:读取数据时进行过滤
     */
    public void decode(Session session, ByteBuffer buffer, Handler handler) throws Throwable;

    /**
     * 编码:输出数据时进行编码
     */
    public void encode(Session session, ByteBuffer buffer);

}
