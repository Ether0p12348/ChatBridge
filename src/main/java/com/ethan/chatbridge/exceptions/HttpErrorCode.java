package com.ethan.chatbridge.exceptions;

public class HttpErrorCode extends Exception {
    private final int statusCode;

    public HttpErrorCode(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpErrorCode(int statusCode) {
        super("Http Error Code: " + statusCode);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "TranslationException{" +
                "statusCode=" + statusCode +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
