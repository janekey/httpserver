package com.janekey.httpserver.http;

import com.janekey.httpserver.net.Session;
import com.janekey.httpserver.util.StringUtil;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: janekey
 * Date: 14-11-18
 * Time: 下午5:18
 */
public class HttpRequestImpl {
    int status, headLength, offset;
    String method, requestURI, queryString;
    String protocol = "HTTP/1.1";

    PipedInputStream pipedInputStream = new PipedInputStream();
    PipedOutputStream pipedOutputStream;
    Map<String, String> headMap = new HashMap<String, String>();
    Session session;

    public HttpRequestImpl(Session session) {
        this.session = session;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getMethod() {
        return method;
    }

    public String getHeader(String name) {
        return headMap.get(name.toLowerCase());
    }

    public Enumeration<String> getHeaderNames() {
        return new Enumeration<String>() {
            private Iterator<String> iterator = headMap.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    public String getProtocol() {
        return protocol;
    }

    public int getContentLength() {
        String len = getHeader("Content-Length");
        return StringUtil.isNotEmpty(len) ? Integer.parseInt(len) : 0;
    }
}
