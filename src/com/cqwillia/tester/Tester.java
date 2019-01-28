package com.cqwillia.tester;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

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
    private String[] preferences;
    private String[] defaultPrefs;

    private BufferedReader reader;
    private PrintStream console;
    private String command;

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
    private static final String PREF_PATH = "d.PREFERENCES";

    public Tester()
    {
        init();
    }

    public Tester(String dir)
    {
        gui = null;

        init();
    }

    private void init()
    {
        //read from preferences file into preferences arraylist and default preferences arraylist
        defaultPrefs = new String[7];
        preferences = new String[7];
        for(int i = 0; i < 7; i++)
        {
            defaultPrefs[i] = "";
            preferences[i] = "";
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
                        break;
                    case "homeworkNumber":
                        setPref(left[0], split[1], I_HWNUM);
                        break;
                    case "testName":
                        setPref(left[0], split[1], I_TESTNAME);
                        break;
                    case "inputDirectory":
                        setPref(left[0], split[1], I_INDIR);
                        break;
                    case "outputDirectory":
                        setPref(left[0], split[1], I_OUTDIR);
                        break;
                    case "referenceDirectory":
                        setPref(left[0], split[1], I_REFDIR);
                        break;
                    case "testPath":
                        setPref(left[0], split[1], I_TESTPATH);
                        break;
                }
            }
        }
        catch(FileNotFoundException f) { f.printStackTrace(); }
        catch(IOException e) {}
        for (int i = 0; i < 7; i++)
        {
            if(preferences[i].equals(""))
            {
                preferences[i] = defaultPrefs[i];
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
                deinit();
            }
        });

        defaultCommand();

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
                while(!Thread.interrupted())
                {
                    printLine(reader);
                }
            }

            private void printLine(BufferedReader out)
            {
                try
                {
                    gui.consoleWindow.append(out.readLine());
                    gui.consoleWindow.append("\n");
                } catch(Exception e)
                {
                    try
                    {
                        Thread.sleep(100);
                    } catch (InterruptedException i) { i.printStackTrace(); }
                }
            }
        };

        new Thread(consoleUpdater).start();

        console.println("Window initialisation complete.");
    }

    private void deinit()
    {
        //write preferences to .preferences file
        try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PREF_PATH), "utf-8")))
        {
            for(int i = 0; i < 7; i++)
            {
                writePref("default", i, defaultPrefs[i], out);
            }
            for(int i = 0; i < 7; i++)
            {
                writePref("saved", i, preferences[i], out);
            }
        } catch(FileNotFoundException f)
        {
            console.println("Preference file missing or misplaced.");
            f.printStackTrace(console);
        } catch(Exception e)
        {
            System.out.println("Error writing preferences to preference file.");
        }

        try
        {
            reader.close();
        } catch(IOException e) { e.printStackTrace(); }
    }

    public void printToConsole(String s)
    {
        console.println(s);
    }

    public void runTest(String comm)
    {
        command = comm;

        File inDir = new File(preferences[I_INDIR]);
        File outDir = new File(preferences[I_OUTDIR]);

        runCommands(comm, inDir, outDir);
    }


    private void updateField(Field f, String s)
    {
        File file = new File(s);
        switch(f)
        {
            case WORKING_DIRECTORY:
                if (preferences[I_WDIR].equals(s)) return;
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences[I_WDIR] = absolute;
                        console.println("Working directory set to " + absolute + ".");
                    } catch (IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided working directory.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences[I_WDIR]);
                    }
                }
                else
                {
                    console.println("WARNING: Provided working directory " + s + " does not exist.");
                    gui.restore(f, preferences[I_WDIR]);
                }
                break;

            case HOMEWORK_NUM:
                if(s.equals(preferences[I_HWNUM])) return;
                preferences[I_HWNUM] = s;
                console.println("Preparing to run test cases for " + s + ".");
                break;

            case TEST_NAME:
                if(s.equals(preferences[I_TESTNAME])) return;
                preferences[I_TESTNAME] = s;
                console.println("Preparing to run test cases for trial "+s+" from "+preferences[I_HWNUM]+".");
                break;

            case INPUT_DIR:
                if (preferences[I_INDIR].equals(s)) return;
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences[I_INDIR] = absolute;
                        console.println("Input directory set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided input directory.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences[I_INDIR]);
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences[I_WDIR]).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences[I_INDIR] = absolute;
                        console.println("Input directory set to " + absolute + ".");
                    }
                    else
                    {
                        console.println("WARNING: Provided input directory " + s + " could not be resolved.");
                        gui.restore(f, preferences[I_INDIR]);
                    }
                }
                break;

            case OUTPUT_DIR:
                if (preferences[I_OUTDIR].equals(s)) return;
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences[I_OUTDIR] = absolute;
                        console.println("Output directory set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided output directory.");
                        e.printStackTrace(console);
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences[I_WDIR]).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences[I_OUTDIR] = absolute;
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
                            preferences[I_OUTDIR] = absolute;
                            console.println("Output directory set to " + absolute + ".");
                        } catch(IOException e)
                        {
                            console.println("WARNING: Failed to get canonical path for newly created output directory.");
                            e.printStackTrace(console);
                            gui.restore(f, preferences[I_OUTDIR]);
                        }
                    }
                }
                break;

            case REFERENCE_DIR:
                if (preferences[I_REFDIR].equals(s)) return;
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences[I_REFDIR] = absolute;
                        console.println("Reference directory set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided reference directory.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences[I_REFDIR]);
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences[I_WDIR]).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences[I_REFDIR] = absolute;
                        console.println("Reference directory set to " + absolute + ".");
                    }
                    else
                    {
                        console.println("WARNING: Provided reference directory " + s + " could not be resolved.");
                        gui.restore(f, preferences[I_REFDIR]);
                    }
                }
                break;

            case TEST_PATH:
                if (preferences[I_TESTPATH].equals(s)) return;
                if(!s.substring(s.length()-4).equals(".cpp"))
                {
                    console.println("Test script path must end in \".cpp\".");
                    return;
                }
                if(Files.exists(file.toPath()))
                {
                    try {
                        String absolute = file.getCanonicalPath();
                        preferences[I_TESTPATH] = absolute;
                        console.println("Test script path set to " + absolute + ".");
                    } catch(IOException e) {
                        console.println("ERROR: Failed to obtain canonical path for provided test script.");
                        e.printStackTrace(console);
                        gui.restore(f, preferences[I_TESTPATH]);
                    }
                }
                //also allow paths given relative to the working directory
                else
                {
                    Path fromWkDir = Paths.get(preferences[I_WDIR]).resolve(file.toPath());
                    if(Files.exists(fromWkDir))
                    {
                        String absolute = fromWkDir.toAbsolutePath().toString();
                        preferences[I_TESTPATH] = absolute;
                        console.println("Test script path set to " + absolute + ".");
                    }
                    else
                    {
                        console.println("WARNING: Provided test script path " + s + " could not be resolved.");
                        gui.restore(f, preferences[I_TESTPATH]);
                    }
                }
                break;
        }
    }

    private void restoreAllFields()
    {
        gui.restore(Field.WORKING_DIRECTORY, preferences[I_WDIR]);
        gui.restore(Field.INPUT_DIR, preferences[I_INDIR]);
        gui.restore(Field.OUTPUT_DIR, preferences[I_OUTDIR]);
        gui.restore(Field.REFERENCE_DIR, preferences[I_REFDIR]);
        gui.restore(Field.HOMEWORK_NUM, preferences[I_HWNUM]);
        gui.restore(Field.TEST_NAME, preferences[I_TESTNAME]);
        gui.restore(Field.TEST_PATH, preferences[I_TESTPATH]);
    }

    private void restoreAllDefaults()
    {
        gui.restore(Field.WORKING_DIRECTORY, defaultPrefs[I_WDIR]);
        gui.restore(Field.INPUT_DIR, defaultPrefs[I_INDIR]);
        gui.restore(Field.OUTPUT_DIR, defaultPrefs[I_OUTDIR]);
        gui.restore(Field.REFERENCE_DIR, defaultPrefs[I_REFDIR]);
        gui.restore(Field.HOMEWORK_NUM, defaultPrefs[I_HWNUM]);
        gui.restore(Field.TEST_NAME, defaultPrefs[I_TESTNAME]);
        gui.restore(Field.TEST_PATH, defaultPrefs[I_TESTPATH]);
    }

    //helper function - sets preference pref in default or saved preferences based on s
    private void setPref(String s, String pref, int i)
    {
        if(s.equals("default")) defaultPrefs[i] = pref;
        else if(s.equals("saved")) preferences[i] = pref;
    }

    private void writePref(String prefix, int i, String value, BufferedWriter out) throws IOException
    {
        String pref = prefix + ".";
        switch(i)
        {
            case I_WDIR:
                pref += "workingDirectory=";
                break;
            case I_HWNUM:
                pref += "homeworkNumber=";
                break;
            case I_TESTNAME:
                pref += "testName=";
                break;
            case I_INDIR:
                pref += "inputDirectory=";
                break;
            case I_OUTDIR:
                pref += "outputDirectory=";
                break;
            case I_REFDIR:
                pref += "referenceDirectory=";
                break;
            case I_TESTPATH:
                pref += "testPath=";
                break;
        }
        pref += value;
        out.write(pref);
        out.newLine();
    }

    private String getField(Field f)
    {
        switch(f)
        {
            case WORKING_DIRECTORY:
                return preferences[I_WDIR];
            case HOMEWORK_NUM:
                return preferences[I_HWNUM];
            case TEST_NAME:
                return preferences[I_TESTNAME];
            case INPUT_DIR:
                return preferences[I_INDIR];
            case OUTPUT_DIR:
                return preferences[I_OUTDIR];
            case REFERENCE_DIR:
                return preferences[I_REFDIR];
            case TEST_PATH:
                return preferences[I_TESTPATH];
        }

        return null;
    }

    private void defaultCommand()
    {
        try
        {
            command = "valgrind --tool=memcheck --leak-check=yes ./";
            command += Paths.get(preferences[I_WDIR]).relativize(Paths.get(preferences[I_TESTPATH])).toString();
            command += " <input>";
            gui.setCommand(command);
        } catch(IllegalArgumentException e)
        {
            System.out.println("Test script path could not be relativised against working directory.");
            command = "valgrind --tool=memcheck --leak-check=yes ./";
            command += preferences[I_TESTPATH];
            command += " <input>";
            gui.setCommand(command);
        }
    }

    private void runCommands(String comm, File inDir, File outDir)
    {
        try
        {
            Map<String, File> parsed = new HashMap<>();
            TesterLogic.parseCommand(comm, inDir, outDir, console, parsed, Paths.get(preferences[I_WDIR]));
            Set<String> commands = parsed.keySet();
            for(String c : commands)
            {
                String[] arguments = c.split(" ");
                ProcessBuilder builder = new ProcessBuilder(arguments);
                builder.directory(new File(preferences[I_WDIR]));
                console.println("Conducting test " + c);
                Process p = builder.start();
                byte[] consoleOutput = new byte[256];
                p.getInputStream().read(consoleOutput);
                console.println("Writing outcome of test to " + parsed.get(c).toPath().toString());
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

        private JTextField commandField;

        protected TesterInterface()
        {
            //set default values for tester window
            super("CS 104 Spring 2018 Test Case Manager v0.0.2");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(800, 600));
            setResizable(false);
            GridBagLayout layout = new GridBagLayout();
            layout.rowHeights = new int[]{ 300, 300, 100 };
            setLayout(layout);

            //initialise console text area and add to pane
            consoleWindow = new JTextArea();
            consoleWindow.setEditable(false);
            JScrollPane scroll = new JScrollPane(consoleWindow);
            GridBagConstraints scrollConstraints = new GridBagConstraints();
            scrollConstraints.fill = GridBagConstraints.BOTH;
            scrollConstraints.weightx = scrollConstraints.weighty = 1;
            getContentPane().add(scroll, scrollConstraints);
            DefaultCaret caret = (DefaultCaret) consoleWindow.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

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

            GridBagConstraints prefConstraints = new GridBagConstraints();
            prefConstraints.gridx = 0;
            prefConstraints.gridy = 1;
            prefConstraints.fill = GridBagConstraints.BOTH;
            prefConstraints.weightx = prefConstraints.weighty = 1;
            getContentPane().add(prefPanel, prefConstraints);

            JPanel execPanel = new JPanel();
            commandField = new JTextField(35);
            execPanel.setLayout(new BorderLayout());
            execPanel.add(new JLabel("Command:"), BorderLayout.NORTH);
            execPanel.add(commandField, BorderLayout.CENTER);
            JPanel execButPanel = new JPanel();
            JButton defCommand = new JButton("Default command");
            JButton saveCommand = new JButton("Save and run");
            defCommand.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    defaultCommand();
                }
            });
            saveCommand.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    runTest(commandField.getText());
                }
            });
            execButPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            execButPanel.add(defCommand);
            execButPanel.add(saveCommand);
            execPanel.add(execButPanel, BorderLayout.SOUTH);

            GridBagConstraints execConstraints = new GridBagConstraints();
            execConstraints.gridx = 0;
            execConstraints.gridy = 2;
            execConstraints.fill = GridBagConstraints.HORIZONTAL;
            execConstraints.insets = new Insets(0, 20, 0, 20);
            execConstraints.weightx = 1;
            getContentPane().add(execPanel, execConstraints);

            setVisible(true);
        }

        protected void restore(Field f, String s)
        {
            switch(f)
            {
                case WORKING_DIRECTORY:
                    workDir.setText(s);
                    break;
                case INPUT_DIR:
                    inDir.setText(s);
                    break;
                case OUTPUT_DIR:
                    outDir.setText(s);
                    break;
                case REFERENCE_DIR:
                    refDir.setText(s);
                    break;
                case TEST_PATH:
                    testPath.setText(s);
                    break;
            }
        }

        protected void setCommand(String s)
        {
            commandField.setText(s);
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
