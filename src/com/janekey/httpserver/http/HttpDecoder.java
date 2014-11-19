package com.janekey.httpserver.http;

import com.janekey.httpserver.net.Logger;
import com.janekey.httpserver.net.Session;
import com.janekey.httpserver.util.StringUtil;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * User: janekey
 * Date: 14-11-19
 * Time: 上午9:43
 */
public class HttpDecoder {

    public static final String HTTP_REQUEST = "http_req";
    public static final String REMAIN_DATA = "remain_data";
    private static final byte LINE_LIMIT = '\n';
    public static final String ENCODING = "UTF-8";//?

    private int maxRequestLineLength = 8 * 1024,
            maxRequestHeadLength = 16 * 1024,
            maxRangeNum = 8,
            writeBufferSize = 8 * 1024;
    private long maxUploadLength = 64 * 1024;

    private AbstractHttpDecoder[] httpDecode = new AbstractHttpDecoder[]{new RequestLineDecoder(), new HeadDecoder(), new BodyDecoder()};

    public void decode(ByteBuffer buf, Session session) throws Throwable {
        ByteBuffer now = getBuffer(buf, session);
        HttpRequestImpl req = getHttpRequestImpl(session);
        httpDecode[req.status].decode0(now, session, req);
    }

    private ByteBuffer getBuffer(ByteBuffer buf, Session session) {
        ByteBuffer now = buf;
        ByteBuffer prev = (ByteBuffer) session.getAttribute(REMAIN_DATA);

        if (prev != null) {
            session.removeAttribute(REMAIN_DATA);
            now = (ByteBuffer) ByteBuffer.allocate(prev.remaining() + buf.remaining())
                    .put(prev).put(buf).flip();
        }
        return now;
    }

    private HttpRequestImpl getHttpRequestImpl(Session session) {
        HttpRequestImpl req = (HttpRequestImpl) session.getAttribute(HTTP_REQUEST);
        if (req == null) {
            req = new HttpRequestImpl(session);
            session.setAttribute(HTTP_REQUEST, req);
        }
        return req;
    }

    private abstract class AbstractHttpDecoder {

        private void decode0(ByteBuffer now, Session session, HttpRequestImpl req) throws Throwable {
            boolean next = decode(now, session, req);
            if (next)
                next(now.slice(), session, req);
            else
                save(now, session);
        }

        private void save(ByteBuffer buf, Session session) {
            if (buf.hasRemaining())
                session.setAttribute(REMAIN_DATA, buf);
        }

        private void next(ByteBuffer buf, Session session,
                          HttpRequestImpl req) throws Throwable {
            req.status++;
            if (req.status < httpDecode.length) {
                req.offset = 0;
                httpDecode[req.status].decode0(buf, session, req);
            }
        }

        protected void finish(Session session, HttpRequestImpl req) {
            session.removeAttribute(REMAIN_DATA);
            session.removeAttribute(HTTP_REQUEST);
            req.status = httpDecode.length;
        }

        protected void responseError(Session session,
                                     HttpRequestImpl req, int httpStatus, String content) {
            finish(session, req);
//            req.response.scheduleSendError(httpStatus, content);
//            req.commitAndAllowDuplicate();//业务逻辑处理
            System.out.println("AbstractHttpDecoder.responseError()");
        }

        protected void response(Session session, HttpRequestImpl req) {
            finish(session, req);
//            req.commitAndAllowDuplicate();//业务逻辑处理
        }

        protected abstract boolean decode(ByteBuffer buf, Session session, HttpRequestImpl req) throws Throwable;
    }

    private class RequestLineDecoder extends AbstractHttpDecoder {

        @Override
        public boolean decode(ByteBuffer buf, Session session,
                              HttpRequestImpl req) throws Throwable {
            if (req.offset >= maxRequestLineLength) {
                String msg = "request line length is " + req.offset
                        + ", it more than " + maxRequestLineLength
                        + "|" + session.getRemoteAddress();
                Logger.log(msg);
                responseError(session, req, 414, msg);//414:Request-URI Too Long
                return true;
            }

            int len = buf.remaining();
            for (; req.offset < len; req.offset++) {
                if (buf.get(req.offset) == LINE_LIMIT) {
                    byte[] data = new byte[req.offset + 1];
                    buf.get(data);
                    String requestLine = new String(data, ENCODING).trim();
                    if (StringUtil.isEmpty(requestLine)) {
                        String msg = "request line length is 0|" + session.getRemoteAddress();
                        Logger.log(msg);
                        responseError(session, req, 400, msg);//400:Bad Request
                        return true;
                    }

                    String[] reqLine = requestLine.split(" ");
                    if (reqLine.length != 3) {
                        String msg = "request line format error: "
                                + requestLine + "|"
                                + session.getRemoteAddress();
                        Logger.log(msg);
                        responseError(session, req, 400, msg);//400:Bad Request
                        return true;
                    }

                    int s = reqLine[1].indexOf('?');
                    req.method = reqLine[0].toUpperCase();
                    if (s > 0) {
                        req.requestURI = reqLine[1].substring(0, s);
                        req.queryString = reqLine[1].substring(s + 1, reqLine[1].length());
                    } else {
                        req.requestURI = reqLine[1];
                    }
                    req.protocol = reqLine[2];
                    return true;
                }
            }
            return false;
        }

    }

    private class HeadDecoder extends AbstractHttpDecoder {

        @Override
        public boolean decode(ByteBuffer buf, Session session, HttpRequestImpl req) throws Throwable {
            int len = buf.remaining();

            for (int i = req.offset, p = 0; i < len; i++) {
                if (buf.get(i) == LINE_LIMIT) {
                    int parseLen = i - p + 1;
                    req.headLength += parseLen;

                    if (req.headLength >= maxRequestHeadLength) {
                        String msg = "request head length is " + req.headLength
                                + ", it more than "
                                + maxRequestHeadLength + "|"
                                + session.getRemoteAddress() + "|"
                                + req.getRequestURI();
                        Logger.log(msg);
                        responseError(session, req, 400, msg);
                        return true;
                    }

                    byte[] data = new byte[parseLen];
                    buf.get(data);
                    String headLine = new String(data, ENCODING).trim();
                    p = i + 1;

                    if (StringUtil.isEmpty(headLine)) {
                        if (!req.getMethod().equals("POST") && !req.getMethod().equals("PUT"))
                            System.out.println("empty.response()");
                            response(session, req);
                        return true;
                    } else {
                        int h = headLine.indexOf(':');
                        if (h <= 0) {
                            String msg = "head line format error: " + headLine
                                    + "|" + session.getRemoteAddress() + "|"
                                    + req.getRequestURI();
                            Logger.log(msg);
                            responseError(session, req, 400, msg);
                            return true;
                        }

                        String name = headLine.substring(0, h).toLowerCase().trim();
                        String value = headLine.substring(h + 1).trim();
                        req.headMap.put(name, value);
                        req.offset = len - i - 1;

                        if (name.equals("expect") && value.startsWith("100-") && req.getProtocol().equals("HTTP/1.1"))
                            response100Continue(session);
                    }
                }
            }
            return false;
        }

        private void response100Continue(Session session) throws UnsupportedEncodingException {
            session.write(ByteBuffer.wrap("HTTP/1.1 100 Continue\r\n\r\n".getBytes(ENCODING)));
        }
    }

    private class BodyDecoder extends AbstractHttpDecoder {
        @Override
        public boolean decode(ByteBuffer buf, Session session,
                              HttpRequestImpl req) throws Throwable {
            int contentLength = req.getContentLength();
            if (contentLength > 0) {
                if (contentLength > maxUploadLength) {
                    String msg = "body length is " + contentLength
                            + " , it more than " + maxUploadLength
                            + "|" + session.getRemoteAddress() + "|"
                            + req.getRequestURI();
                    Logger.log(msg);
                    responseError(session, req, 400, msg);
                    return true;
                }

                if (req.pipedOutputStream == null)
                    req.pipedOutputStream = new PipedOutputStream(req.pipedInputStream);

//                req.commit();//业务逻辑
                System.out.println("BodyDecoder.decode()");

                req.offset += buf.remaining();
                byte[] data = new byte[buf.remaining()];
                buf.get(data);
                try {
                    req.pipedOutputStream.write(data);
                } catch (IOException e) {
                    Logger.log("receive body data error", e);
                    req.pipedOutputStream.close();
                }

                if (req.offset >= contentLength) {
                    req.pipedOutputStream.close();
                    finish(session, req);
                    return true;
                }
            } else {
                System.out.println("content=0 response()");
                response(session, req);
                return true;
            }
            return false;
        }
    }
}
