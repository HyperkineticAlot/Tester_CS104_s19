package com.cqwillia.tester;

import com.cqwillia.tester.exceptions.NestedAngleException;
import com.cqwillia.tester.exceptions.UnpairedAngleException;

public class TesterLogic {
    public static String parseCommand(String c)
    {

    }

    private static String generateCommand(String c, String ifile, String ofile)
            throws NestedAngleException, UnpairedAngleException {
        String command = "";

        for (int i = 0; i < c.length(); i++) {
            char curr = c.charAt(i);
            if (curr != '<') {
                command += curr;
                continue;
            }
            if (curr == '>')
                throw new UnpairedAngleException(UnpairedAngleException.UnpairType.CLOSING);

            String expr = "";
            i++;
            try {
                for (; c.charAt(i) != '>'; i++) {
                    if (c.charAt(i) == '<')
                        throw new NestedAngleException();

                    expr += c.charAt(i);
                }
            } catch (IndexOutOfBoundsException out) {
                throw new UnpairedAngleException(UnpairedAngleException.UnpairType.OPENING);
            }

            if(expr.equals("input")) command += ifile;
            else if(expr.equals("output")) command += ofile;
            else
            {

            }
        }

        return command;
    }
}
