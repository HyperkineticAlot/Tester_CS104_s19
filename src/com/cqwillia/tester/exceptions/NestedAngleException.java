package com.cqwillia.tester.exceptions;

public class NestedAngleException extends Exception
{
    private final static String message = "Nested angle brackets in command string";

    public NestedAngleException()
    {
        super(message);
    }

    public NestedAngleException(Throwable t)
    {
        super(message, t);
    }
}
