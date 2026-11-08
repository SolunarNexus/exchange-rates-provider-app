package com.shipmonk.testingday.resourceaccess.exception;

public class FixerRateLimitException extends FixerResourceAccessException
{

    public FixerRateLimitException(String message)
    {
        super(message);
    }

    public FixerRateLimitException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
