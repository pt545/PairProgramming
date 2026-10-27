# Pair Programming for Software Development (ECM2414)


## Structure of this project

(/Main) Contains the source code for this project.

(/TestingSuite) Contains the source code and test input files for the tests. There is also a subfolder meant to store the test's output.

(/lib) Contains the Jar file necessary to run tests.

(/bin) Contains the class files for this project.

(/) Contains the executable jar file for this project and its input files. It also contains a batch file (its function is detailed in the next section) and the written report.

## Compiling and Running the Test Suite

This project used JDK-22 its development and JUnit 5 in its testing. However the only requirement to run these tests is to have Java installed in your computer.


The following command will execute a Batch file which will in turn run the tests.
```
./TestSuiteCommandLine
```
Note: this command should be run from this project's root directory. 

## Notes about the input text files

The files pack.txt, rand.txt, rand2.txt, rand5.txt and three.txt are all input files.

Their functions are as respectively:an immediate win pack for four players, a file with a random sequence of cards for four players, another randomised file for four players, a randomised pack for five players and a randomised pack for three players.