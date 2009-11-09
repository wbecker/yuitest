/*
 * YUI Test Selenium Driver
 * Author: Nicholas C. Zakas <nzakas@yahoo-inc.com>
 * Copyright (c) 2009, Yahoo! Inc. All rights reserved.
 * Code licensed under the BSD License:
 *     http://developer.yahoo.net/yui/license.txt
 */
package com.yahoo.platform.yui.selenium;

import jargs.gnu.CmdLineParser;
import java.io.*;
import java.util.Date;
import java.util.Properties;

/**
 * Main YUI Test Coverage class.
 * @author Nicholas C. Zakas
 */
public class YUITestSeleniumDriver {

    public static void main(String args[]) {

        //----------------------------------------------------------------------
        // Initialize command line parser
        //----------------------------------------------------------------------
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option verboseOpt = parser.addBooleanOption('v', "verbose");
        CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
        CmdLineParser.Option errorOnFailOpt = parser.addBooleanOption("erroronfail");
        CmdLineParser.Option silentOpt = parser.addBooleanOption("silent");
        CmdLineParser.Option confOpt = parser.addStringOption("conf");
        CmdLineParser.Option hostOpt = parser.addStringOption("host");
        CmdLineParser.Option portOpt = parser.addStringOption("port");
        CmdLineParser.Option browsersOpt = parser.addStringOption("browsers");
        CmdLineParser.Option yuitestOpt = parser.addStringOption("yuitest");
        CmdLineParser.Option testsOpt = parser.addStringOption("tests");


        Reader in = null;
        Writer out = null;
        boolean verbose = false;

        try {

            parser.parse(args);

            //Help option
            Boolean help = (Boolean) parser.getOptionValue(helpOpt);
            if (help != null && help.booleanValue()) {
                usage();
                System.exit(0);
            }

            //Verbose option
            verbose = parser.getOptionValue(verboseOpt) != null;

            //load default properties from configuration file
            Properties properties = new Properties();
            properties.load(YUITestSeleniumDriver.class.getResourceAsStream("default.properties"));

            //conf option
            String confFile = (String) parser.getOptionValue(confOpt);
            if (confFile != null){
                if (verbose){
                    System.err.println("[INFO] Loading configuration properties from " + confFile);
                }
                properties.load(new FileInputStream(confFile));
            }

            //load all command-line properties, which override everything else

            //silent option
            boolean silent = parser.getOptionValue(silentOpt) != null;
            if (silent){
                properties.setProperty(SeleniumDriver.CONSOLE_MODE, "silent");
            }

            //host option
            String host = (String) parser.getOptionValue(hostOpt);
            if (host != null){
                properties.setProperty(SeleniumDriver.SELENIUM_HOST, host);
                if (verbose){
                    System.err.println("[INFO] Using command line value for " + SeleniumDriver.SELENIUM_HOST + ": " + host);
                }
            }

            //port option
            String port = (String) parser.getOptionValue(portOpt);
            if (port != null){
                properties.setProperty(SeleniumDriver.SELENIUM_PORT, port);
                if (verbose){
                    System.err.println("[INFO] Using command line value for " + SeleniumDriver.SELENIUM_PORT + ": " + port);
                }
            }

            //browsers option
            String browsers = (String) parser.getOptionValue(browsersOpt);
            if (browsers != null){
                properties.setProperty(SeleniumDriver.SELENIUM_BROWSERS, browsers);
                if (verbose){
                    System.err.println("[INFO] Using command line value for " + SeleniumDriver.SELENIUM_BROWSERS + ": " + browsers);
                }
            }

            //YUI Test version option
            String yuitest = (String) parser.getOptionValue(yuitestOpt);
            if (yuitest != null){
                properties.setProperty(SeleniumDriver.YUITEST_VERSION, yuitest);
                if (verbose){
                    System.err.println("[INFO] Using command line value for " + SeleniumDriver.YUITEST_VERSION + ": " + yuitest);
                }
            }

            //create a new selenium driver with the properties
            SeleniumDriver driver = new SeleniumDriver(properties, verbose);
            TestResult[] results = null;

            //if --tests is specified, run just those tests
            String testFile = (String) parser.getOptionValue(testsOpt);
            if (testFile != null){
                TestConfig config = new TestConfig();
                config.load(new FileInputStream(testFile));

                if (verbose){
                    System.err.println("[INFO] Using tests from " + testFile + ".");
                }

                results = driver.runTests(config);
            } else {

                //see if there are any test files
                String[] testFiles = parser.getRemainingArgs();
                if (testFiles.length > 0){

                    if (verbose){
                        System.err.println("[INFO] Using tests from command line.");
                    }

                    TestPageGroup group = new TestPageGroup("", 
                            Integer.parseInt(properties.getProperty(SeleniumDriver.YUITEST_VERSION)),
                            Integer.parseInt(properties.getProperty(SeleniumDriver.SELENIUM_WAIT_FOR_DONE)));

                    for (int i=0; i < testFiles.length; i++){
                        TestPage page = new TestPage(group, testFiles[i],
                            Integer.parseInt(properties.getProperty(SeleniumDriver.YUITEST_VERSION)),
                            Integer.parseInt(properties.getProperty(SeleniumDriver.SELENIUM_WAIT_FOR_DONE)));
                        group.add(page);
                    }

                    results = driver.runTests(group);
                } else {
                    if (verbose){
                        System.err.println("[INFO] No tests specified to run, existing..");
                    }
                }

            }


            //output result files
            if (results != null){
                TestResultFileGenerator generator = new TestResultFileGenerator(properties, verbose);
                generator.generateAll(results, new Date());
            }
            
        } catch (CmdLineParser.OptionException e) {

            usage();
            System.exit(1);

        } catch (Exception e) {

            System.err.println("[ERROR] " + e.getMessage());

            if (verbose){
                e.printStackTrace();
            }
            
            System.exit(1);

//        } catch (Exception e) {
//
//            e.printStackTrace();
//            // Return a special error code used specifically by the web front-end.
//            System.exit(2);
        
        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void usage() {
        System.out.println(
                "\nUsage: java -jar yuitest-selenium-driver-x.y.z.jar [options] [test files]\n\n"

                        + "Global Options\n"
                        + "  -h, --help                Displays this information.\n"
                        + "  --browsers <browsers>     Run tests in these browseres (comma-delimited).\n"
                        + "  --conf <file>             Load options from <file>.\n"
//                        + "  --erroronfail             Indicates that a test failure should cause\n"
//                        + "                            an error to be reported to the console.\n"
                        + "  --host <host>             Use the Selenium host <host>.\n"
                        + "  --port <port>             Use <port> port on the Selenium host.\n"
                        + "  --silent                  Don't output test results to the console.\n"
                        + "  --tests <file>            Loads test info from <file>.\n"
                        + "  --yuitest <version>       The version of YUI Test to use (2 or 3).\n"
                        + "  -v, --verbose             Display informational messages and warnings.\n\n");
    }


}