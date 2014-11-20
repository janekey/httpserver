package com.janekey.httpserver.test;

import com.janekey.httpserver.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * User: janekey
 * Date: 14-11-20
 * Time: 下午6:34
 */
public class HttpClient {
    private HttpURLConnection connection;
    private static final String BOUNDARY = "---------------------------7db15a14291cce";
    private String lineSeparator;
    private static final byte[] EMPTY_BUFFER = new byte[0];

    public HttpClient(String url) throws IOException {
        URL u = new URL(url);
        connection = (HttpURLConnection) u.openConnection();
//        lineSeparator =	java.security.AccessController.doPrivileged(
//                new sun.security.action.GetPropertyAction("line.separator"));
        lineSeparator = "\r\n";
    }

    private void initHead() {
//        connection.setRequestProperty("Accept", accept);
//        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
//        if (referer != null) connection.setRequestProperty("Referer", referer);
//        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
//        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    }

    private void initMultipartHead() {
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    }

    private void writeData(String data) throws IOException {
        OutputStream outputStream = connection.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(data);
        writer.close();
    }

    private void writeData(byte[] buffer) throws IOException {
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(buffer);
        outputStream.close();
    }

    private String readData() throws IOException {
        InputStream is = connection.getInputStream();
        int available = is.available();
        byte[] buffer = new byte[available];
        String bufferString = null;
        if (is.read(buffer) != -1) {
            bufferString = new String(buffer);
        }
        is.close();
        return bufferString;
    }

    private byte[] readOriginalData() throws IOException {
        InputStream is = connection.getInputStream();
        int available = is.available();
        byte[] buffer = new byte[available];
        if (is.read(buffer) != -1) {
            return buffer;
        } else {
            return EMPTY_BUFFER;
        }
    }

    public String post(String data) throws IOException {
        initHead();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        if (data != null) {
            writeData(data);
        }
        return readData();
    }

    public String get() throws IOException {
        initHead();

        connection.setDoOutput(false);
        connection.setRequestMethod("GET");

        return readData();
    }

    public byte[] getOriginal() throws IOException {
        initHead();

        connection.setDoOutput(false);
        connection.setRequestMethod("GET");

        return readOriginalData();
    }

    public String multipart(String name, String fileName, byte[] data) throws IOException {
        initMultipartHead();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        //multipart/form-data body buffer
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 200);

        StringBuilder string = new StringBuilder();
        string.append("--").append(BOUNDARY).append(lineSeparator);
        string.append("Content-Disposition: form-data; name=\"").append(name).append("\"; filename=\"")
                .append(fileName).append("\"").append(lineSeparator);
        string.append("Content-Type: image/jpeg").append(lineSeparator).append(lineSeparator);

        buffer.put(StringUtil.getUTF8Bytes(string.toString()));
        buffer.put(data);

        string.delete(0, string.length());
        string.append(lineSeparator).append("--").append(BOUNDARY).append("--").append(lineSeparator);
        buffer.put(StringUtil.getUTF8Bytes(string.toString()));
        writeData(buffer.array());
        return readData();
    }
}
