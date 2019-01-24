package com.cqwillia.tester;

public class TestRunner
{
    public static void main(String[] args)
    {
        String exprTest = "<input> is input, while <output> is output";
        String ifile = "input.txt";
        String ofile = "output.txt";
        String cmd = "";

        try {
            cmd = TesterLogic.generateCommand(exprTest, ifile, ofile);
        } catch(Exception e) { e.printStackTrace(); }

        System.out.println(cmd);
    }
}
