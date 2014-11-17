package com.janekey.http.net;

/**
 * User: p_qizheng
 * Date: 14-11-14
 * Time: 下午5:21
 */
public class Session {

    private static final int INIT_READ_BUFF_SIZE=1024;
    private static final int MAX_READ_BUFF_SIZE = 64 * 1024;

    private int readBufferSize;

    public Session() {
        this.readBufferSize = INIT_READ_BUFF_SIZE;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }
}
