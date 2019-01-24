package com.cqwillia.tester;

import com.cqwillia.tester.exceptions.*;

public final class TesterLogic {

    public static String parseCommand(String c)
    {
        return "";
    }

    static String generateCommand(String c, String ifile, String ofile)
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

            if(expr.toString().equals("input")) command.append(ifile);
            else if(expr.toString().equals("output")) command.append(ofile);
            else
            {
                throw new InvalidContentsException();
            }
        }

        return command.toString();
    }
}
