package com.cqwillia.tester;

import com.cqwillia.tester.exceptions.AngleExpressionException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class CommandBuilder
{
    private static final Map<String, String> dependencies;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("split_test", "split.o");
        m.put("ulliststr_test", "ulliststr.o");
        m.put("ulliststr_ops_test", "ulliststr.o");
        dependencies = Collections.unmodifiableMap(m);
    }

    /* This function creates and builds:
        - a Makefile if necessary for compiling the test script
        - a sequence of commands including compilation and execution with valgrind
       Each of these operations is based on the identity of the test being executed by the
       parent Tester, stored in the private field test.
     */
    public static void build(String[] prefs, PrintStream console)
    {
        String thisDeps = dependencies.get(prefs[Tester.I_TESTNAME]);

        //If there is no script directory, create the script directory (this shouldn't happen...)
        String sep = System.getProperty("file.separator");
        File binDir = new File("scripts" + sep + "bin");
        File srcDir = new File("scripts" + sep + "src");
        if(!binDir.exists() || !binDir.isDirectory())
        {
            binDir.mkdirs();
            srcDir.mkdirs();
            console.println(binDir.getName() + " has been created. Don't delete the script directory!");
        }

        //create the makefile in the user.dir/scripts directory
        File makefile = new File("scripts", "Makefile");
        try
        {
            if(!makefile.exists()) makefile.createNewFile();
        } catch(IOException e)
        {
            e.printStackTrace(console);
            return;
        }

        /* Begin writing to the Makefile.
           The Makefile has the all target, which relies on each dependency listed
           in the static Map dependencies plus the test script itself.
         */
        try(BufferedWriter makeWriter = new BufferedWriter(new PrintWriter(new FileOutputStream(makefile.getPath()))))
        {
            //Copy the test script into the scripts/src directory
            File testScriptFile = new File(prefs[Tester.I_TESTPATH]);
            String testPathFromMake = "src" + sep + testScriptFile.getName();
            Files.copy(testScriptFile.toPath(), Paths.get("scripts" + sep + testPathFromMake),
                    StandardCopyOption.REPLACE_EXISTING);

            //write the all block
            makeWriter.write("all: " + thisDeps + " ");
            makeWriter.write(testPathFromMake);
            makeWriter.newLine();
            makeWriter.write("\tg++ -g " + testPathFromMake);
            for(String dep : thisDeps.split(" "))
            {
                String b = "bin" + sep + dep;
                makeWriter.write(" " + b);
            }
            makeWriter.write(" -o bin" + sep + prefs[Tester.I_TESTNAME]);
            makeWriter.newLine();
            makeWriter.newLine();

            //write a block for each dependency for this test
            for(String dep : thisDeps.split(" "))
            {
                //Copy the dependency script into the scripts/src directory
                File depScriptFile = new File(prefs[Tester.I_WDIR] + sep + dep.substring(0, dep.length()-2) + ".cpp");
                File depScriptH = new File(prefs[Tester.I_WDIR] + sep + dep.substring(0, dep.length()-2) + ".h");
                String depPathFromMake = "src" + sep + depScriptFile.getName();
                Files.copy(depScriptFile.toPath(), Paths.get("scripts" + sep + depPathFromMake),
                        StandardCopyOption.REPLACE_EXISTING);

                makeWriter.write(dep + ": " + depPathFromMake);
                if(depScriptH.isFile())
                {
                    String hPathFromMake = "src" + sep + depScriptH.getName();
                    Files.copy(depScriptH.toPath(), Paths.get("scripts" + sep + hPathFromMake),
                            StandardCopyOption.REPLACE_EXISTING);

                    makeWriter.write(" " + hPathFromMake);
                }
                makeWriter.newLine();
                makeWriter.write("\tg++ -g -c " + depPathFromMake + " -o " + "bin" + sep + dep);
                makeWriter.newLine();
                makeWriter.newLine();
            }
        } catch(Exception f)
        {
            f.printStackTrace(console);
            return;
        }

        //Create a ProcessBuilder in the userDir/scripts directory and use it to make
        ProcessBuilder maker = new ProcessBuilder("make");
        maker.directory(new File("scripts"));
        console.println("Making script executable for test " + prefs[Tester.I_TESTNAME]);
        try
        {
            maker.start();
        } catch(IOException e)
        {
            console.println("ERROR: Failed to start makefile process.");
        }
    }

    public static void run(String[] prefs, String comm, PrintStream console)
    {
        try
        {
            File inDir = new File(prefs[Tester.I_INDIR]);
            File outDir = new File(prefs[Tester.I_OUTDIR]);

            if(!outDir.exists())
            {
                if(!outDir.mkdirs())
                {
                    console.println("ERROR: Specified output directory " + prefs[Tester.I_OUTDIR] + " does not exist " +
                            "and cannot be created by the Java Virtual Machine. Please create it manually.");
                    return;
                }
            }

            ArrayList<String> valFailed = new ArrayList<>();
            boolean valError = false;
            Map<String, File> parsed = new HashMap<>();
            TesterLogic.parseCommand(comm, inDir, outDir, console, parsed);
            Set<String> commands = parsed.keySet();
            for(String c : commands)
            {
                String[] arguments = c.split(" ");
                ProcessBuilder builder = new ProcessBuilder(arguments);
                console.println("Conducting test " + c);
                Process p = builder.start();
                BufferedReader consoleIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader consoleErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                console.println("Writing outcome of test to " + parsed.get(c).toPath().toString());
                BufferedWriter fileOut = new BufferedWriter(new FileWriter(parsed.get(c)));
                String consoleLine;
                while((consoleLine = consoleIn.readLine()) != null)
                {
                    fileOut.write(consoleLine);
                    fileOut.newLine();
                }
                fileOut.close();

                console.println("Reading valgrind log");
                String errLine;
                while((errLine = consoleErr.readLine()) != null)
                {
                    if(errLine.contains("blocks are definitely lost") && !valFailed.contains(parsed.get(c).toString()))
                    {
                        valError = true;
                        console.println("Valgrind error in trial corresponding to output file " + parsed.get(c));
                        valFailed.add(parsed.get(c).toString());
                    }
                }
            }

            console.println("Comparing outcomes between output file and reference solutions.");
            TesterLogic.compareResults(outDir, new File(prefs[Tester.I_REFDIR]), console);
            if(valError)
            {
                String valWarning = "WARNING: Valgrind errors detected in trials corresponding to output files ";
                for(String s : valFailed)
                {
                    valWarning += s + ", ";
                }
                valWarning = valWarning.substring(0, valWarning.length()-2) + ".";
                console.println(valWarning);
            }
            else
            {
                console.println("No valgrind errors detected.");
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

    public static String getExecPath(String testExec)
    {
        String sep = System.getProperty("file.separator");
        return "scripts" + sep + "bin" + sep + testExec;
    }
}
