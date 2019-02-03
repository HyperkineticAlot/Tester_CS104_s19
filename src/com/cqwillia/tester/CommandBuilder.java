package com.cqwillia.tester;

import java.io.File;

public class CommandBuilder
{
    private String test;

    public CommandBuilder(String t)
    {
        test = t;
    }

    /* This function creates and builds:
        - a Makefile if necessary for compiling the test script
        - a sequence of commands including compilation and execution with valgrind
       Each of these operations is based on the identity of the test being executed by the
       parent Tester, stored in the private field test.
     */
    public String[] build(String comm, File inDir, File outDir)
    {
        return null;
    }

    public void setTest(String t)
    {
        test = t;
    }
}
