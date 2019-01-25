package com.cqwillia.tester;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import com.cqwillia.tester.exceptions.AngleExpressionException;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tester
{
    private TesterInterface gui;
    private File testDirectory;
    private ArrayList<String> preferences;

    private BufferedReader reader;
    private PrintStream console;

    public enum Field
    {
        WORKING_DIRECTORY, HOMEWORK_NUM, TEST_NAME,
        INPUT_DIR, OUTPUT_DIR, REFERENCE_DIR, TEST_PATH
    }

    private static final int I_WDIR = 0;
    private static final int I_HWNUM = 1;
    private static final int I_TESTNAME = 2;
    private static final int I_INDIR = 3;
    private static final int I_OUTDIR = 4;
    private static final int I_REFDIR = 5;
    private static final int I_TESTPATH = 6;

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
        //read from preferences file into preferences arraylist
        preferences = new ArrayList<> (7);

        //initialise the gui
        gui = new TesterInterface();
        gui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                deinit();
            }
        });

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

    private void deinit()
    {
        //write preferences to .preferences file
    }

    public void printToConsole(String s)
    {
        console.println(s);
    }

    public void runTest(String comm, File inDir, File outDir)
    {
        runCommands(comm, inDir, outDir);
    }


    public void updateField(Field f, String s)
    {
        File file = new File(s);
        switch(f)
        {
            case WORKING_DIRECTORY:
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences.set(I_WDIR, absolute);
                        console.println("Working directory set to " + absolute + ".");
                    } catch (IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided working directory.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences.get(I_WDIR));
                    }
                }
                else
                {
                    console.println("WARNING: Provided working directory " + s + " does not exist.");
                    gui.restore(f, preferences.get(I_WDIR));
                }

            case HOMEWORK_NUM:
                preferences.set(I_HWNUM, s);

            case TEST_NAME:
                preferences.set(I_TESTNAME, s);

            case INPUT_DIR:
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences.set(I_INDIR, absolute);
                        console.println("Input directory set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided input directory.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences.get(I_INDIR));
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences.get(I_WDIR)).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences.set(I_INDIR, absolute);
                        console.println("Input directory set to " + absolute + ".");
                    }
                    else
                    {
                        console.println("WARNING: Provided input directory " + s + " could not be resolved.");
                        gui.restore(f, preferences.get(I_INDIR));
                    }
                }

            case OUTPUT_DIR:
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences.set(I_OUTDIR, absolute);
                        console.println("Output directory set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided output directory.");
                        e.printStackTrace(console);
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences.get(I_WDIR)).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences.set(I_OUTDIR, absolute);
                        console.println("Output directory set to " + absolute + ".");
                    }
                    //if the path for output directory cannot be resolved, create a new directory for output
                    else
                    {
                        console.println("WARNING: Provided output directory " + s + " could not be resolved. Creating" +
                                " output directory at specified path...");
                        file.mkdirs();
                        try
                        {
                            String absolute = file.getCanonicalPath();
                            preferences.set(I_OUTDIR, absolute);
                            console.println("Output directory set to " + absolute + ".");
                        } catch(IOException e)
                        {
                            console.println("WARNING: Failed to get canonical path for newly created output directory.");
                            e.printStackTrace(console);
                            gui.restore(f, preferences.get(I_OUTDIR));
                        }
                    }
                }

            case REFERENCE_DIR:
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences.set(I_REFDIR, absolute);
                        console.println("Reference directory set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided reference directory.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences.get(I_REFDIR));
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences.get(I_WDIR)).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences.set(I_REFDIR, absolute);
                        console.println("Reference directory set to " + absolute + ".");
                    }
                    else
                    {
                        console.println("WARNING: Provided reference directory " + s + " could not be resolved.");
                        gui.restore(f, preferences.get(I_REFDIR));
                    }
                }

            case TEST_PATH:
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences.set(I_TESTPATH, absolute);
                        console.println("Test script path set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided test script.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences.get(I_TESTPATH));
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences.get(I_WDIR)).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences.set(I_TESTPATH, absolute);
                        console.println("Test script path set to " + absolute + ".");
                    }
                    else
                    {
                        console.println("WARNING: Provided test script path " + s + " could not be resolved.");
                        gui.restore(f, preferences.get(I_TESTPATH));
                    }
                }
        }
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

        private JTextField workDir;
        private JTextField inDir;
        private JTextField outDir;
        private JTextField refDir;
        private JTextField testPath;

        protected TesterInterface()
        {
            //set default values for tester window
            super("CS 104 Spring 2018 Test Case Manager v0.0.2");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(800, 600);
            setResizable(false);
            setLayout(new GridLayout(3, 1));

            /*//initialise console text area and add to pane
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
            setJMenuBar(menuBar);*/

            JPanel prefPanel = new JPanel();
            Border border = prefPanel.getBorder();
            Border bevel = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
            prefPanel.setBorder(BorderFactory.createCompoundBorder(border, bevel));
            GridBagLayout prefLayout = new GridBagLayout();
            prefLayout.columnWidths = new int[] { 370, 370 };
            prefLayout.rowHeights = new int[] { 50, 50, 30, 30, 30, 70 };


            add(prefPanel);

            setVisible(true);
        }

        protected void restore(Field f, String s)
        {
            switch(f)
            {
                case WORKING_DIRECTORY:
                    workDir.setText(s);
                case INPUT_DIR:
                    inDir.setText(s);
                case OUTPUT_DIR:
                    outDir.setText(s);
                case REFERENCE_DIR:
                    refDir.setText(s);
                case TEST_PATH:
                    testPath.setText(s);
            }
        }
    }
}
