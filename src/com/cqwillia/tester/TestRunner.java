package com.cqwillia.tester;

import com.cqwillia.tester.exceptions.AngleExpressionException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestRunner
{
    public static void main(String[] args)
    {
        String comm = "src/../test/readwrite_test <input> <output>";
        File inDir = new File("src/../test/input");
        File outDir = new File("src/../test/output");
        ArrayList<String> commands = new ArrayList<>();
        try
        {
            commands = TesterLogic.parseCommand(comm, inDir, outDir, System.out);
        }
        catch(AngleExpressionException e)
        {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        for(String s : commands)
        {
            System.out.println(s);
        }
    }
}
