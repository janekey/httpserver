package com.janekey.http.net;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: p_qizheng
 * Date: 14-11-14
 * Time: 下午5:21
 */
public class Processor implements Runnable {

    protected static final int SELECT_TIMEOUT = 100;

    protected Selector selector;
    private Filter filter;
    private Handler handler;
    protected ConcurrentLinkedQueue<SocketChannel> toRegister;
    protected ConcurrentLinkedQueue<SelectionKey> toCancel;
    protected ConcurrentLinkedQueue<SelectionKey> toWrite;

    public Processor(String name, Filter filter, Handler handler, int num) {
        try {
            this.filter = filter;
            this.handler = handler;
            selector = Selector.open();
            toRegister = new ConcurrentLinkedQueue<SocketChannel>();
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
            while (true) {
                int n = selector.select(SELECT_TIMEOUT);

                SocketChannel socketChannel;
                while ((socketChannel = toRegister.poll()) != null) register(socketChannel);

                if (n > 0) {
                    Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
                    SelectionKey key;
                    while (readyKeys.hasNext()) {
                        key = readyKeys.next();
                        if (key.isReadable()) read(key, 0);//todo read
                        if (key.isWritable()) ;//todo write
                        readyKeys.remove();
                    }
                }

                SelectionKey cancelableKey;
                while ((cancelableKey = toCancel.poll()) != null) cancel(cancelableKey);
            }
        } catch (Throwable th) {
            Logger.log(th, "Processor run error");
        }
    }

    public void scheduleRegister(SocketChannel socketChannel) {
        toRegister.offer(socketChannel);
        selector.wakeup();
    }

    protected void register(SocketChannel socketChannel) {
        try {
            socketChannel.configureBlocking(false);
            SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
            Session session = new Session();//todo
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
//                session.setLastReadTime(now);
                session.increaseReadBytes(read);
            } else {
                session.decreaseReadBufferSize();
            }
            if (n < 0) {
//                session.close(false);
            }
        } catch (Throwable th) {
//            session.close(true);
            th.printStackTrace();
            throw new RuntimeException(th);
//            handlerExecutor.exceptionCaught(session, t);
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
}
