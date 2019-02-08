package com.cqwillia.tester;

import java.io.*;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        File[] inFiles = new File(prefs[Tester.I_INDIR]).listFiles();
        String thisDeps = dependencies.get(prefs[Tester.I_TESTNAME]);

        if(inFiles == null)
        {
            /* HANDLE NO INPUT CASES HERE */
            return;
        }

        //If there is no script directory, create the script directory (this shouldn't happen...)
        String sep = System.getProperty("file.separator");
        File binDir = new File("scripts" + sep + "bin");
        if(!binDir.exists() || !binDir.isDirectory())
        {
            binDir.mkdirs();
            console.println(binDir.getName() + " has been created. Don't delete the script directory!");
        }

        //create the makefile in the working directory
        File makefile = new File(new File(prefs[Tester.I_WDIR]), "Makefile");
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
            String canTestPath = new File(prefs[Tester.I_TESTPATH]).getCanonicalPath();
            //write the all block
            makeWriter.write("all: " + thisDeps);
            makeWriter.write(canTestPath);
            makeWriter.newLine();
            makeWriter.write("\tg++ -g " + canTestPath + " ");
            for(String dep : thisDeps.split(" "))
            {
                String can = "scripts" + sep + "bin" + sep + dep;
                can = new File(can).getCanonicalPath();
                makeWriter.write(can + " ");
            }
            makeWriter.write(" -o " + binDir.getCanonicalPath() + sep + prefs[Tester.I_TESTNAME]);
            makeWriter.newLine();
            makeWriter.newLine();

            //write a block for each dependency for this test
            for(String dep : thisDeps.split(" "))
            {
                String depScriptPath = dep.substring(0, dep.length()-2) + ".cpp";
                makeWriter.write(dep + ": " + depScriptPath);
                makeWriter.newLine();
                makeWriter.write("\tg++ -g -c " + depScriptPath + " -o " + binDir.getCanonicalPath() + sep + dep);
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
        maker.directory(new File(prefs[Tester.I_WDIR]));
        console.println("Making script executable for test " + prefs[Tester.I_TESTNAME]);
        try
        {
            maker.start();
        } catch(IOException e)
        {
            console.println("ERROR: Failed to start makefile process.");
        }
    }
}
