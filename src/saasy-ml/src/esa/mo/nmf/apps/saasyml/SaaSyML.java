package esa.mo.nmf.apps.saasyml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * SaaSy ML implementation of the Control layer.
 *
 * @author Dr. Cesar Guzman
 */
public class SaaSyML {

    private static Logger logger = LoggerFactory.getLogger(SaaSyML.class);


    /**
     * Empty constructor
     */
    public SaaSyML() { }

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     */
    public SaaSyML(boolean thread) {
    }

    /**
     * Main function
     * @param args a set of string parameters
     */
    public static void main(String[] args) {

        // how to use information
        String howToUse = "$ java -jar saasyml-control-layer-0.1.0-SNAPSHOT.jar -thread [1 | true] -s [1 | true] -tests [1 2 3 | Classifier Cluster Outlier]";

    }

}