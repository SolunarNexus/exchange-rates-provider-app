package com.shipmonk.testingday.resourceaccess.exception;

public class FixerResourceAccessException extends RuntimeException
{

    public FixerResourceAccessException(String message)
    {
        super(message);
    }

    public FixerResourceAccessException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
