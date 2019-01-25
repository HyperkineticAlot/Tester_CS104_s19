package com.cqwillia.tester;

import javax.swing.*;

import com.cqwillia.tester.exceptions.AngleExpressionException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
    private File testDirectory;

    private BufferedReader reader;
    private PrintStream console;

    public Tester()
    {
        testDirectory = new File("test");

        init();
    }

    public Tester(String dir)
    {
        gui = null;
        testDirectory = new File(dir);

        init();
    }

    private void init()
    {
        //initialise the gui
        gui = new TesterInterface();

        //generate piped input and output streams to handle data flow into console
        PipedOutputStream out = new PipedOutputStream();
        console = new PrintStream(out, true);
        try
        {
            PipedInputStream in = new PipedInputStream(out);
            reader = new BufferedReader(new InputStreamReader(in));
        } catch(IOException e) { e.printStackTrace(); return; }

        //initialise the console PrintStream to print input to the console JTextArea
        Runnable consoleUpdater = new Runnable()
        {
            @Override
            public void run()
            {
                String line;

                try
                {
                    line = reader.readLine();
                    while(line != null)
                    {
                        gui.consoleWindow.append(line);
                        line = reader.readLine();
                    }
                } catch(IOException e)
                {
                    System.out.println("Input redirection to console threw an IO exception");
                    e.printStackTrace();
                }
            }
        };

        new Thread(consoleUpdater).start();
    }

    public void printToConsole(String s)
    {
        console.println(s);
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
        protected JTextArea consoleWindow;

        protected TesterInterface()
        {
            //set default values for tester window
            super("CS 104 Spring 2018 Test Case Manager v0.0.2");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(300, 600);
            setResizable(false);

            //initialise console text area and add to pane
            consoleWindow = new JTextArea();
            consoleWindow.setEditable(false);
            getContentPane().add(consoleWindow);

            //construct the menuBar and menu items, then add them to the frame
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            fileMenu.setMnemonic(KeyEvent.VK_F);
            JMenuItem preferencesItem = new JMenuItem("Preferences");
            preferencesItem.setMnemonic(KeyEvent.VK_P);
            preferencesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, ActionEvent.CTRL_MASK));
            preferencesItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Preferences was clicked!");
                }
            });
            fileMenu.add(preferencesItem);
            menuBar.add(fileMenu);
            setJMenuBar(menuBar);

            setVisible(true);
        }
    }
}
