package com.cqwillia.tester.exceptions;

public class UnpairedAngleException extends AngleExpressionException
{
    private final static String opMessage = "Unpaired opening angle bracket in command string";
    private final static String clMessage = "Unpaired closing angle bracket in command string";

    public enum UnpairType
    {
        OPENING, CLOSING
    }

    private UnpairType type;

    public UnpairedAngleException(UnpairType t)
    {
        super();
        type = t;
    }

    public UnpairedAngleException(UnpairType t, Throwable th)
    {
        super(th);
        type = t;
    }

    @Override
    public String getMessage()
    {
        if(type == UnpairType.CLOSING) return clMessage;
        if(type == UnpairType.OPENING) return opMessage;
        return "Unpaired angle bracket in command string";
    }
}
