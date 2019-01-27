package com.cqwillia.tester;

import java.io.*;

public class TestRunner
{
    public static void main(String[] args)
    {
        /*String comm = "test\\readwrite_test <input>";
        File inDir = new File("test/input");
        File outDir = new File("test/output");

        Tester test = new Tester();
        test.runTest(comm, inDir, outDir);*/

        Tester test = new Tester();
        test.printToConsole("Juice");
    }
}
