package com.automart;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.net.URI;
import java.net.InetSocketAddress;
import java.io.*;

public class TestHome {
    public static void main(String[] args) throws Exception {
        HttpExchange dummyExchange = new HttpExchange() {
            @Override public Headers getRequestHeaders() { return new Headers(); }
            @Override public Headers getResponseHeaders() { return new Headers(); }
            @Override public URI getRequestURI() { return URI.create("/"); }
            @Override public String getRequestMethod() { return "GET"; }
            @Override public com.sun.net.httpserver.HttpContext getHttpContext() { return null; }
            @Override public void close() {}
            @Override public InputStream getRequestBody() { return null; }
            @Override public OutputStream getResponseBody() { return null; }
            @Override public void sendResponseHeaders(int code, long len) {}
            @Override public InetSocketAddress getRemoteAddress() { return null; }
            @Override public int getResponseCode() { return 200; }
            @Override public InetSocketAddress getLocalAddress() { return null; }
            @Override public String getProtocol() { return "HTTP/1.1"; }
            @Override public Object getAttribute(String name) { return null; }
            @Override public void setAttribute(String name, Object value) {}
            @Override public void setStreams(InputStream i, OutputStream o) {}
            @Override public com.sun.net.httpserver.HttpPrincipal getPrincipal() { return null; }
        };
        System.out.println(AutoMartApplication.homeContent(dummyExchange));
    }
}
