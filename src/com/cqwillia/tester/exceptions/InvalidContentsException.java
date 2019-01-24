package com.cqwillia.tester.exceptions;

public class InvalidContentsException extends AngleExpressionException
{
    private static final String message = "Invalid expression between angle brackets";

    public InvalidContentsException()
    {
        super(message);
    }

    public InvalidContentsException(Throwable t)
    {
        super(message, t);
    }
}
