package com.janekey.httpserver.http;

import com.janekey.httpserver.net.Handler;
import com.janekey.httpserver.net.Session;

/**
 * User: p_qizheng
 * Date: 14-12-22
 * Time: 下午3:26
 */
public class HttpHandler implements Handler {
    @Override
    public void doReceived(Session session, Object message) throws Throwable {
        HttpRequestImpl request = (HttpRequestImpl) message;

        if (request.response.system) { // 系统错误响应
            request.response.outSystemData(session);
        } else {
            //逻辑 controller
//            servController.dispatcher(request, request.response);
            request.response.outData(session);
        }
    }
}
