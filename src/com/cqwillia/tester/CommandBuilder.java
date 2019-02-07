package com.cqwillia.tester;

import java.io.*;
import java.nio.file.Paths;
import java.util.Map;

public final class CommandBuilder
{
    private static final Map<String, String> dependencies;
    static {
        dependencies = Map.of("split_test", "scripts/bin/split.o",
                              "ulliststr_test", "scripts/bin/ulliststr.o",
                              "ulliststr_ops_test", "scripts/bin/ulliststr.o");
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
        String userDir = System.getProperty("user.dir");
        String relativeWDir = Paths.get(userDir).relativize(Paths.get(prefs[Tester.I_WDIR])).toString();

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

        //create the makefile in the userDir/scripts directory
        File makefile = new File(binDir.getParentFile(), "Makefile");
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
            //write the all block
            makeWriter.write("all: " + thisDeps);
            makeWriter.write(prefs[Tester.I_TESTPATH]);
            makeWriter.newLine();
            makeWriter.write("\tg++ -g " + prefs[Tester.I_TESTPATH] + " " +
                    thisDeps + " -o " + binDir.getPath() + sep + prefs[Tester.I_TESTNAME]);
            makeWriter.newLine();
            makeWriter.newLine();

            //write a block for each dependency for this test
            for(String dep : thisDeps.split(" "))
            {
                String depScriptPath = relativeWDir + sep + dep.substring(0, dep.length()-2) + ".cpp";
                makeWriter.write(dep + ": " + depScriptPath);
                makeWriter.newLine();
                makeWriter.write("\tg++ -g -c " + depScriptPath + " -o " + binDir.getPath() + sep + dep);
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
        maker.directory(binDir.getParentFile());
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
