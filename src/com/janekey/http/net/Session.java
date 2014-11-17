package com.janekey.http.net;

/**
 * User: p_qizheng
 * Date: 14-11-14
 * Time: 下午5:21
 */
public class Session {

    private static final int INIT_READ_BUFF_SIZE = 2;
    private static final int MAX_READ_BUFF_SIZE = 64 * 1024;
    private static final int MIN_READ_BUFF_SIZE = 8;

    private int readBufferSize;
    private int readBytes;
    private int writeBytes;

    public Session() {
        this.readBufferSize = INIT_READ_BUFF_SIZE;
        readBytes=0;
        writeBytes=0;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public void increaseReadBufferSize() {
        if (readBufferSize < MAX_READ_BUFF_SIZE) readBufferSize = readBufferSize << 1;
    }

    public void decreaseReadBufferSize() {
        if (readBufferSize > MIN_READ_BUFF_SIZE) readBufferSize = readBufferSize >>> 1;
    }

    public void increaseReadBytes(int bytes) {
        readBytes += bytes;
    }
}
