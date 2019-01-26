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
import java.util.concurrent.Flow;

public class Tester
{
    private TesterInterface gui;
    private File testDirectory;
    private ArrayList<String> preferences;
    private ArrayList<String> defaultPrefs;

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
    private static final String PREF_PATH = "default.PREFERENCES";

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
        //read from preferences file into preferences arraylist and default preferences arraylist
        defaultPrefs = new ArrayList<>(7);
        preferences = new ArrayList<>(7);
        for(int i = 0; i < 7; i++)
        {
            defaultPrefs.add("");
            preferences.add("");
        }
        try
        {
            BufferedReader prefRead = new BufferedReader(new FileReader(PREF_PATH));
            String line;
            while((line = prefRead.readLine()) != null)
            {
                String[] split = line.split("=");
                if(split.length == 1) continue;
                String[] left = split[0].split("\\.");
                switch(left[1])
                {
                    case "workingDirectory":
                        setPref(left[0], split[1], I_WDIR);
                    case "homeworkNumber":
                        setPref(left[0], split[1], I_HWNUM);
                    case "testName":
                        setPref(left[0], split[1], I_TESTNAME);
                    case "inputDirectory":
                        setPref(left[0], split[1], I_INDIR);
                    case "outputDirectory":
                        setPref(left[0], split[1], I_OUTDIR);
                    case "referenceDirectory":
                        setPref(left[0], split[1], I_REFDIR);
                    case "testPath":
                        setPref(left[0], split[1], I_TESTPATH);
                }
            }
        }
        catch(FileNotFoundException f) { f.printStackTrace(); }
        catch(IOException e) {}
        for (int i = 0; i < 7; i++)
        {
            if(preferences.get(i).equals(""))
            {
                preferences.set(i, defaultPrefs.get(i));
            }
        }

        //initialise the gui
        try
        {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    gui = new TesterInterface();
                }
            });
        } catch(Exception e) { e.printStackTrace(); }
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
                    /*System.out.println("Input redirection to console threw an IO exception");
                    e.printStackTrace();*/
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


    private void updateField(Field f, String s)
    {
        File file = new File(s);
        switch(f)
        {
            case WORKING_DIRECTORY:
                if (preferences.get(I_WDIR).equals(s)) return;
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
                if (preferences.get(I_INDIR).equals(s)) return;
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
                if (preferences.get(I_OUTDIR).equals(s)) return;
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
                if (preferences.get(I_REFDIR).equals(s)) return;
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
                if (preferences.get(I_TESTPATH).equals(s)) return;
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

    private void restoreAllFields()
    {
        gui.restore(Field.WORKING_DIRECTORY, preferences.get(I_WDIR));
        gui.restore(Field.INPUT_DIR, preferences.get(I_INDIR));
        gui.restore(Field.OUTPUT_DIR, preferences.get(I_OUTDIR));
        gui.restore(Field.REFERENCE_DIR, preferences.get(I_REFDIR));
        gui.restore(Field.HOMEWORK_NUM, preferences.get(I_HWNUM));
        gui.restore(Field.TEST_NAME, preferences.get(I_TESTNAME));
        gui.restore(Field.TEST_PATH, preferences.get(I_TESTPATH));
    }

    private void restoreAllDefaults()
    {
        gui.restore(Field.WORKING_DIRECTORY, defaultPrefs.get(I_WDIR));
        gui.restore(Field.INPUT_DIR, defaultPrefs.get(I_INDIR));
        gui.restore(Field.OUTPUT_DIR, defaultPrefs.get(I_OUTDIR));
        gui.restore(Field.REFERENCE_DIR, defaultPrefs.get(I_REFDIR));
        gui.restore(Field.HOMEWORK_NUM, defaultPrefs.get(I_HWNUM));
        gui.restore(Field.TEST_NAME, defaultPrefs.get(I_TESTNAME));
        gui.restore(Field.TEST_PATH, defaultPrefs.get(I_TESTPATH));
    }

    //helper function - sets preference pref in default or saved preferences based on s
    private void setPref(String s, String pref, int i)
    {
        if(s.equals("default")) defaultPrefs.set(i, pref);
        else if(s.equals("saved")) preferences.set(i, pref);
    }

    private String getField(Field f)
    {
        switch(f)
        {
            case WORKING_DIRECTORY:
                return preferences.get(I_WDIR);
            case HOMEWORK_NUM:
                return preferences.get(I_HWNUM);
            case TEST_NAME:
                return preferences.get(I_TESTNAME);
            case INPUT_DIR:
                return preferences.get(I_INDIR);
            case OUTPUT_DIR:
                return preferences.get(I_OUTDIR);
            case REFERENCE_DIR:
                return preferences.get(I_REFDIR);
            case TEST_PATH:
                return preferences.get(I_TESTPATH);
        }

        return null;
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

        private JComboBox<String> hwNum;
        private JComboBox<String> testName;

        protected TesterInterface()
        {
            //set default values for tester window
            super("CS 104 Spring 2018 Test Case Manager v0.0.2");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(800, 600));
            setResizable(false);
            setLayout(new GridLayout(3, 1));

            //initialise console text area and add to pane
            consoleWindow = new JTextArea();
            consoleWindow.setEditable(false);
            getContentPane().add(consoleWindow);

            /*//construct the menuBar and menu items, then add them to the frame
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
            //prefLayout.rowHeights = new int[] { 70, 100, 30, 30, 70 };
            prefPanel.setLayout(prefLayout);

            //initialise working directory input and mounting panel
            JPanel workPanel = new JPanel();
            workPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            workDir = new JTextField(getField(Field.WORKING_DIRECTORY), 55);
            workPanel.add(new JLabel("Working directory:"));
            workPanel.add(workDir);
            GridBagConstraints workConstraints = new GridBagConstraints();
            setConstraints(workConstraints, 0, 0);
            workConstraints.gridwidth = 2;
            prefPanel.add(workPanel, workConstraints);

            //initialise dropdown menus for homework and test name, and pin to mounting panels
            JPanel hwPanel = new JPanel();
            hwPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
            hwNum = new JComboBox<>( new String[]{ "Homework 2" } );
            hwPanel.add(new JLabel("Homework number:"));
            hwPanel.add(hwNum);
            GridBagConstraints hwConstraints = new GridBagConstraints();
            setConstraints(hwConstraints, 0, 1);
            prefPanel.add(hwPanel, hwConstraints);

            JPanel testPanel = new JPanel();
            testName = new JComboBox<>( new String[]{"split", "ulliststr"});
            testPanel.add(new JLabel("Test name:"));
            testPanel.add(testName);
            GridBagConstraints testConstraints = new GridBagConstraints();
            setConstraints(testConstraints, 1, 1);
            prefPanel.add(testPanel, testConstraints);

            //initialise "directory panels" for paths to input, output, reference directories, and test script path
            inDir = new JTextField(getField(Field.INPUT_DIR), 20);
            makeDirectoryPanel(prefPanel, inDir, "Input directory:", 0, 2);

            outDir = new JTextField(getField(Field.OUTPUT_DIR), 20);
            makeDirectoryPanel(prefPanel, outDir, "Output directory:", 1, 2);

            refDir = new JTextField(getField(Field.REFERENCE_DIR), 20);
            makeDirectoryPanel(prefPanel, refDir, "Reference directory:", 0, 3);

            testPath = new JTextField(getField(Field.TEST_PATH), 20);
            makeDirectoryPanel(prefPanel, testPath, "Test script path:", 1, 3);

            //initialise buttons and button panel
            JPanel buttonPanel = new JPanel();
            JButton restoreButton = new JButton("Restore defaults");
            restoreButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    restoreAllDefaults();
                }
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    restoreAllFields();
                }
            });
            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateField(Field.WORKING_DIRECTORY, workDir.getText());
                    updateField(Field.INPUT_DIR, inDir.getText());
                    updateField(Field.OUTPUT_DIR, outDir.getText());
                    updateField(Field.REFERENCE_DIR, refDir.getText());
                    updateField(Field.TEST_PATH, testPath.getText());

                    updateField(Field.HOMEWORK_NUM, (String) hwNum.getSelectedItem());
                    updateField(Field.TEST_NAME, (String) testName.getSelectedItem());
                }
            });
            buttonPanel.add(restoreButton);
            buttonPanel.add(cancelButton);
            buttonPanel.add(saveButton);
            GridBagConstraints buttonConstraints = new GridBagConstraints();
            buttonConstraints.gridwidth = 2;
            setConstraints(buttonConstraints, 0, 4);
            prefPanel.add(buttonPanel, buttonConstraints);

            getContentPane().add(prefPanel);

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

        private void setConstraints(GridBagConstraints c, int x, int y)
        {
            c.gridx = x;
            c.gridy = y;
            c.weighty = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
        }

        private void makeDirectoryPanel(JComponent parent, JTextField dirField, String label, int x, int y)
        {
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            panel.add(new JLabel(label));
            panel.add(dirField);
            GridBagConstraints c = new GridBagConstraints();
            setConstraints(c, x, y);
            parent.add(panel, c);
        }
    }
}
