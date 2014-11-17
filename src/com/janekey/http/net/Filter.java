package com.janekey.http.net;

import java.nio.ByteBuffer;

/**
 * User: p_qizheng
 * Date: 14-11-14
 * Time: 下午5:21
 */
public interface Filter {

    /**
     * 解码:读取数据时进行过滤
     */
    public void decode(Session session, ByteBuffer buffer);

    /**
     * 编码:输出数据时进行编码
     */
    public void encode(Session session, ByteBuffer buffer);

}
