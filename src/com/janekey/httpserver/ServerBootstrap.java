package com.janekey.httpserver;

import com.janekey.httpserver.http.HttpFilter;
import com.janekey.httpserver.net.Acceptor;
import com.janekey.httpserver.net.Handler;

import java.net.InetSocketAddress;

/**
 * User: janekey
 * Date: 14-11-14
 * Time: 上午11:51
 */
public class ServerBootstrap {

    public static void main(String[] args) {
        Acceptor acceptor = new Acceptor("httpserver", new HttpFilter(), new Handler(), new InetSocketAddress(12345));
        acceptor.listen();
    }

}
