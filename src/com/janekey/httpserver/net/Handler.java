package com.janekey.httpserver.net;

/**
 * User: janekey
 * Date: 14-11-14
 * Time: 下午5:23
 */
public interface Handler {

    public void doReceived(Session session, Object message) throws Throwable;

}
