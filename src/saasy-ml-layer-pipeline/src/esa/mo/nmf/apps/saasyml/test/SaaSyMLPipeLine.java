package esa.mo.nmf.apps.saasyml.test;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.service.PipeLineAbstractJSAT;

import jsat.DataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * SaaSy ML implementation of the SaaSy ML PipeLine
 *
 * @author Dr. Cesar Guzman
 */
public class SaaSyMLPipeLine {

    private static Logger logger = LoggerFactory.getLogger(SaaSyMLPipeLine.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    /***********************************/
    /************ CONSTRUCTOR **********/
    /***********************************/

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public SaaSyMLPipeLine() {
    }


    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    /**
     * Main function
     * @param args a set of string parameters
     */
    public static void main(String[] args) {

        // how to use information
        String howToUse = "$ java -jar saasyml-pipeline-0.1.0-SNAPSHOT.jar -thread [1 | true] -s [1 | true] -tests [1 2 3 | Classifier Cluster Outlier]";

        // if the thread is available or not
        boolean thread = false;

        // if the serialize is available or not
        boolean serialize = false;

        // set of tests to execute
        List<String> tests_list = null;

        // if there is no arguments, we add by default three tests
        if (args.length < 1) {

            tests_list =  new ArrayList<String>() {
                {
                    add("1");
                    add("2");
                    add("3");
                }
            };

        } else {
            // otherwise, we get the tests by command line

            // we stored all in a tuple <o, v>, where o is an option and v is a set or values (tests)
            final Map<String, List<String>> params = new HashMap<>();

            // for each argument, we take the values
            for (int index = 0; index < args.length; index++) {

                final String arg = args[index];

                if (arg.charAt(0) == '-') {
                    if (arg.length() < 2) {
                        System.err.println("Error at argument " + arg);
                        return;
                    }

                    tests_list = new ArrayList<>();
                    params.put(arg.substring(1), tests_list);

                } else {
                    if (tests_list != null) {
                        tests_list.add(arg);
                    } else {
                        System.err.println("Illegal parameter usage");
                    }
                }
            }

            // if it is empty or id does not contain tests option, we stop
            if (params.isEmpty() || !params.containsKey("tests")) {
                tests_list =  new ArrayList<String>() {
                    {
                        add("1");
                        add("2");
                        add("3");
                    }
                };
            }else{
                // get the tests list
                tests_list = params.get("tests");
            }

            // if it contains the thread option, we added it. By Default, it is false
            if (params.containsKey("thread")) {
                String option = params.get("thread").get(0);
                if ("1".equalsIgnoreCase(option) || "true".equalsIgnoreCase(option))
                    thread = true;
            }

            // if it contains the serialize (s) option, we added it. By Default, it is false
            if (params.containsKey("s")) {
                String option = params.get("s").get(0);
                if ("1".equalsIgnoreCase(option) || "true".equalsIgnoreCase(option))
                    serialize = true;
            }
        }

        logger.info("************* ************************************ ****");
        logger.info("************* Configuration of Execution **************");
        logger.info("- thread: "+thread);
        logger.info("- serialize: "+ serialize);
        logger.info("- tests: "+ tests_list.toString());
        logger.info("************* ************************************ ****\n");

        // for each test, we execute it
        for (String s : tests_list) {

            if (s.equals("1") || s.equals("Classifier")) {
                logger.info("************* Testing Classifier **************");
                SaaSyMLPipeLineTest.testClassifier(thread, serialize);
            }

            if (s.equals("2") || s.equals("Cluster")) {
                logger.info("************* Testing Clustering **************");
                SaaSyMLPipeLineTest.testClustering(thread, serialize);
            }

            if (s.equals("3") || s.equals("Outlier")) {
                logger.info("************* Testing Outlier **************");
                SaaSyMLPipeLineTest.testOutlier(thread, serialize);
            }
        }

        System.out.println("\nHelp of use:\n" + howToUse);

        // SaaSyMLTest.testSettings();
    }

}