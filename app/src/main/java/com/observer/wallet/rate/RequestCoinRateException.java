package com.observer.wallet.rate;

/**
 * Created by furszy on 7/5/17.
 */
public class RequestCoinRateException extends Exception {
    public RequestCoinRateException(String message) {
        super(message);
    }

    public RequestCoinRateException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestCoinRateException(Exception e) {
        super(e);
    }
}
