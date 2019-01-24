package com.cqwillia.tester;

import javax.swing.*;

import com.cqwillia.tester.exceptions.AngleExpressionException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tester
{
    private TesterInterface gui;
    private PrintStream console;
    private File testDirectory;

    public Tester()
    {
        console = System.out;
        testDirectory = new File("test");

    }

    public Tester(PrintStream c, String dir)
    {
        console = c;
        gui = null;
        testDirectory = new File(dir);
    }

    public void runTest(String comm, File inDir, File outDir)
    {
        runCommands(comm, inDir, outDir);
    }

    private void runCommands(String comm, File inDir, File outDir)
    {
        try
        {
            Map<String, File> parsed = new HashMap<>();
            TesterLogic.parseCommand(comm, inDir, outDir, console, parsed);
            Set<String> commands = parsed.keySet();
            for(String c : commands)
            {
                String[] arguments = c.split(" ");
                ProcessBuilder builder = new ProcessBuilder(arguments);
                //builder.directory(testDirectory);
                Process p = builder.start();
                byte[] consoleOutput = new byte[256];
                p.getInputStream().read(consoleOutput);
                Files.write(parsed.get(c).toPath(), consoleOutput);
            }
        }
        catch(AngleExpressionException a)
        {
            console.println("ERROR: Syntax failure. " + a.getMessage());
        }
        catch(IOException e)
        {
            console.println("ERROR: Failed to create output file:");
            e.printStackTrace(console);
        }
    }

    private class TesterInterface extends JFrame
    {
        protected JTextPane consoleWindow;
    }
}
