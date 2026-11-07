package com.shipmonk.testingday.client;

public class FixerClientException extends RuntimeException
{

    public FixerClientException(String message)
    {
        super(message);
    }

    public FixerClientException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
