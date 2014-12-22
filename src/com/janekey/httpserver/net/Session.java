package com.janekey.httpserver.net;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: janekey
 * Date: 14-11-14
 * Time: 下午5:21
 */
public class Session {

    public static final ByteBuffer CLOSE_FLAG = ByteBuffer.allocate(0);
    private static final int INIT_READ_BUFF_SIZE = 1024;
    private static final int MAX_READ_BUFF_SIZE = 64 * 1024;
    private static final int MIN_READ_BUFF_SIZE = 8;

    private Map<String, Object> attributes;
    private ConcurrentLinkedQueue<ByteBuffer> writeQueue;
    private int id;
    private int readBufferSize;
    private int readBytes;
    private int writeBytes;
    private SelectionKey selectionKey;
    private Processor processor;
    private long openTime;
    private long lastReadTime;
    private long lastWriteTime;
    private int ioTimeoutMillis;
    private volatile boolean isOpen;

    public Session(int sessionId, Processor processor, SelectionKey selectionKey, int ioTimeoutMillis) {
        this.readBufferSize = INIT_READ_BUFF_SIZE;
        this.processor = processor;
        this.selectionKey = selectionKey;
        this.id = sessionId;
        attributes = new HashMap<String, Object>();
        readBytes = 0;
        writeBytes = 0;
        openTime = 0;
        lastReadTime = 0;
        lastWriteTime = 0;
        writeQueue = new ConcurrentLinkedQueue<ByteBuffer>();
        isOpen = true;
        this.ioTimeoutMillis = ioTimeoutMillis;
        System.out.println("============session:" + sessionId + "================");
    }

    public void write(ByteBuffer message) {
        try {
            if (isOpen) {
                writeQueue.offer(message);
                scheduleWrite(true);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public void write(byte[] message) {
        try {
            if (isOpen) {
                ByteBuffer bb = ByteBuffer.allocate(message.length);
                writeQueue.offer(bb.put(message));
                scheduleWrite(true);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
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

    public void increaseWriteBytes(int bytes) {
        writeBytes += bytes;
    }

    public ConcurrentLinkedQueue<ByteBuffer> getWriteQueue() {
        return writeQueue;
    }

    public void scheduleWrite(boolean isWake) {
        SelectionKey tmpSelectionKey = selectionKey;
        processor.scheduleWrite(tmpSelectionKey, isWake);
    }

    public void close(boolean immediately) {
        isOpen = false;
        if (immediately) {
            writeQueue.clear();
            SelectionKey tmpSelectionKey = selectionKey;
            if (tmpSelectionKey != null) processor.scheduleCancel(tmpSelectionKey);
        } else {
            writeQueue.offer(CLOSE_FLAG);
            scheduleWrite(true);
        }
    }

    public SocketAddress getRemoteAddress() {
        return ((SocketChannel) selectionKey.channel()).socket().getRemoteSocketAddress();
    }

    public int getIoTimeoutMillis() {
        return ioTimeoutMillis;
    }

    public long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(long openTime) {
        this.openTime = openTime;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public void setLastWriteTime(long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }
}
