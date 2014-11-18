package com.janekey.httpserver.net;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * User: janekey
 * Date: 14-11-14
 * Time: 下午5:21
 */
public class Acceptor implements Runnable {

    protected static final int DEFAULT_IO_TIMEOUT_MILLIS = 30000;//原30秒

    protected Processor[] processors;
    protected Selector selector;
    private Filter filter;                  //过滤器
    protected Handler handler;              //处理器
    protected String name;                  //名称

    public Acceptor(String name, Filter filter, Handler handler, InetSocketAddress bindAddress) {
        try {
            selector = Selector.open();
            this.name = name;
            this.filter = filter;
            this.handler = handler;

            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(bindAddress);
            SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            key.attach(DEFAULT_IO_TIMEOUT_MILLIS);
        } catch (Throwable th) {
            Logger.log(th, "Acceptor Constructor Error");
            throw new RuntimeException(th);
        }
    }

    @Override
    public void run() {
        try {
            int sessionId = 0;
            while (true) {
                int n = selector.select();
                if (n > 0) {
                    Iterator<SelectionKey> readyKeys = selector.selectedKeys().iterator();
                    while (readyKeys.hasNext()) {
                        SelectionKey key = readyKeys.next();
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverChannel.accept();

                        int processorNum = Math.abs(sessionId) % processors.length;
                        int ioTimeoutMillis = (Integer) key.attachment();
                        processors[processorNum].scheduleRegister(socketChannel, sessionId, ioTimeoutMillis);
                        sessionId++;
                        readyKeys.remove();
                    }
                }
            }
        } catch (Throwable th) {
            Logger.log(th, "Acceptor run error");
        }
    }

    public synchronized void listen() {
        try {
            int processorCount = Runtime.getRuntime().availableProcessors() + 1;
            processors = new Processor[processorCount];
            for (int i = 0; i < processorCount; i++) processors[i] = new Processor(name, filter, handler, i);

            new Thread(this, name + "-Acceptor").start();
        } catch (Throwable th) {
            Logger.log(th, "Acceptor listen error");
            throw new RuntimeException(th);
        }
    }
}
