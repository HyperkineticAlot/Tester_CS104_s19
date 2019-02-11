package com.cqwillia.tester;

import com.cqwillia.tester.exceptions.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

final class TesterLogic {

    /* throws IOException when creating new output files */
    @SuppressWarnings("null") //null-checking is performed on inDir and outDir by Tester
    static void parseCommand(String c, File inDir, File outDir, PrintStream console, Map<String, File> commands)
            throws AngleExpressionException, IOException
    {
        File[] inFiles = inDir.listFiles();
        File[] outFiles = outDir.listFiles();
        //custom test cases where there may be no need for an input file
        if(inFiles == null)
        {
            if(c.contains("<input>"))
            {
                console.println("ERROR: Command contains <input> token, but input directory is empty.");
                return;
            }
            if(outFiles == null)
            {
                File defOutput = new File(outDir + System.getProperty("file.separator") + "output_01.txt");
                defOutput.createNewFile();
                console.println("New output file output_01.txt generated for command without input file.");
                commands.put(generateCommand(c, null, Paths.get(defOutput.getCanonicalPath())), defOutput);
                return;
            }
            commands.put(generateCommand(c, null, Paths.get(outFiles[0].getCanonicalPath())), outFiles[0]);
        }

        //define new map subclasses to issue warnings when filenames with duplicate numbers are added
        Map<Integer, File> inKeys = new HashMap<Integer, File>()
        {
            @Override
            public File put(Integer key, File value)
            {
                if(containsKey(key))
                    console.println("WARNING: Input file " + value.getName() + " represents the addition of a duplicate"
                     +" key and will override input file " + get(key).getName() + ".");
                return super.put(key, value);
            }
        };

        Map<Integer, File> outKeys = new HashMap<Integer, File>()
        {
            @Override
            public File put(Integer key, File value)
            {
                if(containsKey(key))
                    console.println("WARNING: Output file " + value.getName() + " represents the addition of a duplicate"
                    +" key and will override output file " + get(key).getName() + ".");
                return super.put(key, value);
            }
        };

        //read integer keys from file name into maps
        readKeys(inFiles, inKeys, console);
        readKeys(outFiles, outKeys, console);

        //iterate over every key in the input domain
        for(Map.Entry<Integer, File> entry : inKeys.entrySet())
        {
            Integer nextKey = entry.getKey();
            File nextInFile = inKeys.get(nextKey);
            File nextOutFile = outKeys.get(nextKey);
            //if an output file has already been created, pair the two files and generate a command for the pair
            if(outKeys.containsKey(nextKey)) {
                try {
                    commands.put(generateCommand(c, Paths.get(nextInFile.getCanonicalPath()),
                            Paths.get(nextOutFile.getCanonicalPath())), nextOutFile);
                } catch (AngleExpressionException e) {
                    console.println(e.getMessage());
                    throw e;
                }
            }
            //if there is no paired output file, generate a new output file "output_<key>" and pair them
            else
            {
                File file = new File(outDir.getPath() + System.getProperty("file.separator") + "output_"
                    + nextKey.toString() + ".txt");
                try
                {
                    if(file.createNewFile())
                    {
                        console.println("File " + file.getName() + " has been created as a paired output file to "
                                + inKeys.get(nextKey) + ".");
                        try{
                            commands.put(generateCommand(c, Paths.get(nextInFile.getCanonicalPath()),
                                    Paths.get(file.getCanonicalPath())), file);
                        }
                        catch(AngleExpressionException e)
                        {
                            console.println(e.getMessage());
                            throw e;
                        }
                    }
                    else
                        console.println("File " + file.getName() + " already exists. This is a bug! Please report "
                            + "it on Piazza or by emailing me at cqwillia@usc.edu.");
                }
                catch(IOException e)
                {
                    e.printStackTrace(console);
                    throw e;
                }
            }
        }
    }

    private static String generateCommand(String c, Path ifile, Path ofile)
            throws AngleExpressionException {
        StringBuilder command = new StringBuilder();

        //parse for opening angle brackets
        for (int i = 0; i < c.length(); i++) {
            char curr = c.charAt(i);

            //unpaired closing angle bracket
            if (curr == '>')
                throw new UnpairedAngleException(UnpairedAngleException.UnpairType.CLOSING);

            //any non angle-bracket character remains unchanged
            if (curr != '<') {
                command.append(curr);
                continue;
            }

            //generate the expression between angle brackets
            StringBuilder expr = new StringBuilder();
            i++; //moves to the character after the opening angle bracket
            try {
                //traverse the string to find the closing angle bracket
                for (; c.charAt(i) != '>'; i++) {
                    if (c.charAt(i) == '<')
                        throw new NestedAngleException();

                    expr.append(c.charAt(i));
                }
            } catch (IndexOutOfBoundsException out) {
                throw new UnpairedAngleException(UnpairedAngleException.UnpairType.OPENING);
            }

            Path userPath = Paths.get(System.getProperty("user.dir"));
            if(expr.toString().equals("input")) command.append(userPath.relativize(ifile));
            else if(expr.toString().equals("output")) command.append(userPath.relativize(ofile));
            else
            {
                throw new InvalidContentsException();
            }
        }

        return command.toString();
    }

    static void compareResults(File outDir, File refDir, PrintStream console)
    {
        File[] outFiles = outDir.listFiles();
        File[] refFiles = refDir.listFiles();
        if(refFiles == null)
        {
            console.println("No reference files available.");
            return;
        }

        Map<Integer, File> outKeys = new HashMap<Integer, File>()
        {
            @Override
            public File put(Integer key, File value)
            {
                if(containsKey(key))
                    console.println("WARNING: Output file " + value.getName() + " represents the addition of a duplicate"
                            +" key and will override output file " + get(key).getName() + "in comparison to reference.");
                return super.put(key, value);
            }
        };

        Map<Integer, File> refKeys = new HashMap<Integer, File>()
        {
            @Override
            public File put(Integer key, File value)
            {
                if(containsKey(key))
                    console.println("WARNING: Reference file " + value.getName() + " represents the addition of a duplicate"
                            +" key and will override reference file " + get(key).getName() + ".");
                return super.put(key, value);
            }
        };

        readKeys(outFiles, outKeys, console);
        readKeys(refFiles, refKeys, console);

        //notify the user of missing output files or missing reference files
        for(Integer i : outKeys.keySet())
        {
            if (!refKeys.containsKey(i))
            {
                console.println("No corresponding reference file found to check output from " +
                        outKeys.get(i).getName() + ".");
            }
        }

        //additionally, test every result for which a reference file exists
        for(Integer i : refKeys.keySet())
        {
            if(!outKeys.containsKey(i))
            {
                console.println("No corresponding test result found to compare against reference " +
                        refKeys.get(i).getName() + ".");
                continue;
            }

            BufferedReader readOut;
            BufferedReader readRef;
            try
            {
                readOut = new BufferedReader(new InputStreamReader(new FileInputStream(outKeys.get(i))));
                readRef = new BufferedReader(new InputStreamReader(new FileInputStream(refKeys.get(i))));
            }
            catch (IOException e)
            {
                console.println("Error reading from output file " + outKeys.get(i) + " or reference file "
                    + refKeys.get(i) + ".");
                continue;
            }

            int refLines = 0;
            int outLines = 0;
            boolean correct = true;
            try
            {
                String nextRef;
                while((nextRef = readRef.readLine()) != null)
                {
                    if(!nextRef.equals(readOut.readLine())) correct = false;
                    if(!nextRef.isEmpty()) refLines++;
                }
            } catch(IOException e) {}
            try
            {
                readOut.close();
                readOut = new BufferedReader(new InputStreamReader(new FileInputStream(outKeys.get(i))));
                String nextOut;
                while((nextOut = readOut.readLine()) != null)
                {
                    if(!nextOut.isEmpty()) outLines++;
                }
                if(outLines != refLines) correct = false;

                String result = "Trial number " + i.toString() + (correct ? " succeeded" : " failed") + " after comparison" +
                        " between files " + outKeys.get(i) + " and " + refKeys.get(i);

                console.println(result);
                readRef.close();
                readOut.close();
            } catch(IOException e) {}
        }
    }

    private static void readKeys(File[] files, Map<Integer, File> keys, PrintStream console)
    {
        //assign each file a code based on the first collection of less than 9 digits in its filename
        if(files == null) return;
        for(File f : files)
        {
            if(!f.isFile()) continue; //disregard subdirectories
            String filename = f.getName();
            StringBuilder filecode = new StringBuilder(); //stores each digit of the first number in the file name
            for(int i = 0; i < filename.length(); i++) //traverse each character of the file name
            {
                if(Character.isDigit(filename.charAt(i))) //when a digit is found, loop to the end of the number
                {
                    while(i < filename.length() && Character.isDigit(filename.charAt(i)) && filecode.length() < 9)
                    {
                        filecode.append(filename.charAt(i));
                        i++;
                    }
                    break; //only stores first contiguous number in filename
                }
            }

            //if no digits in the filename, warn the user and proceed to the next file
            if(filecode.length() == 0)
            {
                console.println("WARNING: File " + f.getName() + " contains no digits in the filename, and " +
                        "thus cannot be coded. It will not be added to test file commands.");
                continue;
            }

            Integer code = Integer.parseInt(filecode.toString());
            keys.put(code, f);
        }
    }
}
