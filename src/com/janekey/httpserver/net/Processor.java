package com.janekey.httpserver.net;

import com.janekey.httpserver.util.Millisecond100Clock;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: janekey
 * Date: 14-11-14
 * Time: 下午5:21
 */
public class Processor implements Runnable {

    protected static final int WRITE_SPIN_COUNT = 32;
    protected static final int SELECT_TIMEOUT = 100;
    protected static final int IO_TIMEOUT_CHECK_INTERVAL = 5000;

    protected Selector selector;
    private Filter filter;
    private Handler handler;
    protected ConcurrentLinkedQueue<Ternary<Integer, SocketChannel, Integer>> toRegister;
    protected ConcurrentLinkedQueue<SelectionKey> toCancel;
    protected ConcurrentLinkedQueue<SelectionKey> toWrite;
    protected long lastIoTimeoutCheckTime;

    public Processor(String name, Filter filter, Handler handler, int num) {
        try {
            this.filter = filter;
            this.handler = handler;
            selector = Selector.open();
            toRegister = new ConcurrentLinkedQueue<Ternary<Integer, SocketChannel, Integer>>();
            toCancel = new ConcurrentLinkedQueue<SelectionKey>();
            toWrite = new ConcurrentLinkedQueue<SelectionKey>();

            new Thread(this, name + "-Processor-" + num).start();
        } catch (Throwable th) {
            Logger.log(th, "Processor Construct Error");
            throw new RuntimeException(th);
        }
    }

    @Override
    public void run() {
        try {
            lastIoTimeoutCheckTime = Millisecond100Clock.currentTimeMillis();

            while (true) {
                int n = selector.select(SELECT_TIMEOUT);
                long now = Millisecond100Clock.currentTimeMillis();

                Ternary<Integer, SocketChannel, Integer> register;
                while ((register = toRegister.poll()) != null) register(register, now);

                if (n > 0) {
                    Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
                    now = Millisecond100Clock.currentTimeMillis();
                    SelectionKey key;
                    while (readyKeys.hasNext()) {
                        key = readyKeys.next();
                        if (key.isReadable()) read(key, now);
                        if (key.isWritable()) ((Session) key.attachment()).scheduleWrite(true);
                        readyKeys.remove();
                    }
                }

                now = Millisecond100Clock.currentTimeMillis();
                SelectionKey writableKey;
                while ((writableKey = toWrite.poll()) != null) write(writableKey, now);

                SelectionKey cancelableKey;
                while ((cancelableKey = toCancel.poll()) != null) cancel(cancelableKey);

                now = Millisecond100Clock.currentTimeMillis();
                if (now - lastIoTimeoutCheckTime > IO_TIMEOUT_CHECK_INTERVAL) {
                    lastIoTimeoutCheckTime = now;
                    for (SelectionKey everyKey : selector.keys()) checkIoTimeout(everyKey, now);
                }
            }
        } catch (Throwable th) {
            Logger.log(th, "Processor run error");
        }
    }

    private void checkIoTimeout(SelectionKey key, long now) {
        if (!key.isValid()) return;
        Session session = (Session) key.attachment();
        int ioTimeoutMillis = session.getIoTimeoutMillis();
        if (ioTimeoutMillis < 0) return;
        long lastIoTime = Math.max(session.getOpenTime(), Math.max(session.getLastReadTime(), session.getLastWriteTime()));
        if (now - lastIoTime > ioTimeoutMillis) {
            Logger.log("connection io timeout", ioTimeoutMillis);
            session.close(true);
        }
    }

    public void scheduleRegister(SocketChannel socketChannel, int sessionId, int ioTimeoutMillis) {
        toRegister.offer(new Ternary<Integer, SocketChannel, Integer>(sessionId, socketChannel, ioTimeoutMillis));
        selector.wakeup();
    }

    protected void register(Ternary<Integer, SocketChannel, Integer> register, long now) {
        try {
            int sessionId = register.first;
            SocketChannel socketChannel = register.second;
            int ioTimeoutMillis = register.third;
            socketChannel.configureBlocking(false);
            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
            Session session = new Session(sessionId, this, key, ioTimeoutMillis);
            session.setOpenTime(now);
            key.attach(session);
        } catch (Throwable th) {
            Logger.log(th, "register");
        }
    }

    private void read(SelectionKey key, long now) {
        Session session = (Session) key.attachment();
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            int size = session.getReadBufferSize();
            ByteBuffer buffer = ByteBuffer.allocate(size);
            int read = 0;
            int n;
            while ((n = channel.read(buffer)) > 0) read += n;
            if (read > 0) {
                buffer.flip();
                filter.decode(session, buffer);
                if (read == size) session.increaseReadBufferSize();
                else if (read < size >>> 1) session.decreaseReadBufferSize();
                session.setLastReadTime(now);
                session.increaseReadBytes(read);
            } else {
                session.decreaseReadBufferSize();
            }
            if (n < 0) {
                session.close(false);
            }
        } catch (Throwable th) {
            session.close(true);
            th.printStackTrace();
            throw new RuntimeException(th);
        }
    }

    public void scheduleCancel(SelectionKey selectionKey) {
        toCancel.offer(selectionKey);
    }

    protected void cancel(SelectionKey selectionKey) {
        try {
            if (!selectionKey.isValid()) return;
//            Session session=(Session)selectionKey.attachment();
            selectionKey.cancel();
            selectionKey.attach(null);
            selectionKey.channel().close();
        } catch (Throwable th) {
            Logger.log(th, "processor cancel error");
        }
    }

    public void scheduleWrite(SelectionKey key, boolean isWakeUp) {
        boolean isEmpty = toWrite.isEmpty();
        toWrite.offer(key);
        if (isWakeUp && isEmpty) {
            selector.wakeup();
        }
    }

    protected void write(SelectionKey key, long now) {
        if (!key.isValid()) return;
        Session session = (Session) key.attachment();
        try {
//            session.scheduleWrite(false);
            setInterestedInWrite(key, false);
            SocketChannel channel = (SocketChannel) key.channel();
            int write = 0;
            ConcurrentLinkedQueue<ByteBuffer> queue = session.getWriteQueue();
            ByteBuffer buff = null;
            while ((buff = queue.peek()) != null) {
                if (buff == Session.CLOSE_FLAG) {
                    queue.clear();
                    scheduleCancel(key);
                    buff = null;
                    break;
                }
                int n = 0;
                for (int i = 0; i < WRITE_SPIN_COUNT; i++) if ((n = channel.write(buff)) > 0) break;
                if (n == 0) break;
                write += n;
                if (!buff.hasRemaining()) queue.poll();
            }
            if (write > 0) {
                session.setLastWriteTime(now);
                session.increaseWriteBytes(write);
            }
            if (buff != null) {
                setInterestedInWrite(key, true);
            }
        } catch (Throwable th) {
            session.close(true);
            throw new RuntimeException(th);
        }
    }

    protected void setInterestedInWrite(SelectionKey key, boolean interestedInWrite) {
        key.interestOps(interestedInWrite ? (SelectionKey.OP_READ | SelectionKey.OP_WRITE) : SelectionKey.OP_READ);
    }
}
