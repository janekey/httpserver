package com.janekey.httpserver.http;

import com.janekey.httpserver.net.Logger;
import com.janekey.httpserver.net.Session;
import com.janekey.httpserver.util.StringUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: janekey
 * Date: 14-11-20
 * Time: 上午11:55
 */
public class HttpResponseImpl {

    private static final SimpleDateFormat GMT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private HttpRequestImpl request;
    private int status, bufferSize;
    private String characterEncoding, shortMessage;
    private Map<String, String> headMap = new HashMap<String, String>();
    private List<Cookie> cookies = new LinkedList<Cookie>();

    public HttpResponseImpl(HttpRequestImpl request, String characterEncoding) {
        this.request = request;
        this.characterEncoding = characterEncoding;

        setStatus(200);
        setHeader("Server", "HttpServer");
    }

    public byte[] getHeadData() {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getProtocol()).append(' ').append(status).append(' ')
                .append(shortMessage).append("\r\n");

        for (String name : headMap.keySet())
            sb.append(name).append(": ").append(headMap.get(name))
                    .append("\r\n");

        for (Cookie cookie : cookies) {
            sb.append("Set-Cookie: ").append(cookie.getName()).append('=')
                    .append(cookie.getValue());

            if (StringUtil.isNotEmpty(cookie.getComment()))
                sb.append(";Comment=").append(cookie.getComment());

            if (StringUtil.isNotEmpty(cookie.getDomain()))
                sb.append(";Domain=").append(cookie.getDomain());

            if (cookie.getMaxAge() >= 0)
                sb.append(";Max-Age=").append(cookie.getMaxAge());

            String path = StringUtil.isEmpty(cookie.getPath()) ? "/" : cookie
                    .getPath();
            sb.append(";Path=").append(path);

            if (cookie.getSecure())
                sb.append(";Secure");

            sb.append(";Version=").append(cookie.getVersion()).append("\r\n");
        }

        sb.append("\r\n");
        String head = sb.toString();
        return stringToByte(head);
    }

    public byte[] stringToByte(String str) {
        byte[] ret = null;
        try {
            ret = str.getBytes(characterEncoding);
        } catch (UnsupportedEncodingException e) {
            Logger.log(e, "string to bytes");
        }
        return ret;
    }

    public void outData(Session session) throws IOException {
        boolean keepAlive = !"close".equals(headMap.get("Connection")) && request.isKeepAlive();
        setHeader("Date", GMT_FORMAT.format(new Date()));
        setHeader("Connection", keepAlive ? "keep-alive" : "close");

        String content = "this is a page";
        setHeader("Content-Length", String.valueOf(content.length()));
        byte[] b1 = getHeadData();
        byte[] b2 = content.getBytes(characterEncoding);
        ByteBuffer buffer = ByteBuffer.allocate(b1.length + b2.length);
        buffer.put(b1);
        buffer.put(b2);
        buffer.flip();
        session.write(buffer);
//        session.write(ByteBuffer.wrap(getHeadData()));
//        session.write(ByteBuffer.wrap(content.getBytes(characterEncoding)));
        System.out.println("out***");
        if (!request.isKeepAlive())
            session.close(false);
    }

    public void setStatus(int status) {
        this.status = status;
        this.shortMessage = Constants.STATUS_CODE.get(status);
    }

    public void setHeader(String name, String value) {
        headMap.put(name, value);
    }

    public void addHeader(String name, String value) {
        String v = headMap.get(name);
        if (v != null) {
            v += "," + value;
            setHeader(name, v);
        } else
            setHeader(name, value);
    }

    public void removeHeader(String name) {
        headMap.remove(name);
    }
}
