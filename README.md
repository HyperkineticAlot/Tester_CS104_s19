# Tester_CS104_s19

**Author**: Cameron Williams

**Version**: 0.0.2

**Release date**: 29 January 2019

Looking for help or to report a bug? Email me at cqwillia@usc.edu,
post a comment on this repository, or reply to the latest Piazza thread!

# Quick start guide

The CS104 Tester is a desktop Java application designed to manage and
automate testing for 104 homework problems with ungraded test cases. To
get started with the first test implemented into Tester, Homework 2's
split, begin by cloning this repository into the cs104 directory on your
Virtual Machine. In order for Tester to work as intended, it's easiest 
if you run it from inside your hw-<name>/hw2 directory. Copy the 
`cs104/Tester_CS104_s19` directory into that folder.

To run a java application, you'll need to install the Java Development Kit.
From the command line, type `sudo apt install default-jdk`. After typing
your user password and 'y', you should get something like this:

![alt text](https://i.imgur.com/yYEgFWU.png "Installing java on a clean VM")

Next, you'll need to compile the code inside the project directory
`~/.../hw-<name>/hw2/Tester_CS104_s19`. Open a terminal window, point it at
that directory, and enter the commands

```
javac src/com/cqwillia/tester/*.java src/com/cqwillia/tester/exceptions/*.java
java -cp src com.cqwillia.tester.TestRunner
```

(The generic operator * tells javac to compile all .java files in the given
directory.) After typing these commands, you should see the Tester window
open:

![alt text](https://i.imgur.com/0diHr4i.png "Executing TestRunner main()")

The first time that you run Tester, each field should be exactly as shown in
the image. Each subsequent time that you launch it, this fields will be filled
in with extended paths specific to your machine, so close Tester and relaunch
it. You can do this by entering `java -cp src com.cqwillia.tester.TestRunner`,
or by defining a new shell script with `subl startup.sh` and pasting the two
commands above into the file, then executing the script with `bash startup.sh`.

There is one final step necessary before using Tester on your code for split.
Using your file system navigator, copy `Tester_CS104_s19/test/scripts/split_test.cpp`
into the `hw-<name>/hw2` directory. Then, press the "Save and Run" button at
the bottom of the Tester window to run the split test cases.

![alt text](https://i.imgur.com/F8yRDDW.png "Running Tester")

This guide will be updated with more detailed information on how to customise
Tester in the coming days (when it's not midnight).
