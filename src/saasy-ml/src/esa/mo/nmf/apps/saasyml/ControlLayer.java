package esa.mo.nmf.apps.saasyml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import jsat.DataSet;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.service.PipeLineJSAT;



/**
 * SaaSy ML implementation of the Control layer.
 *
 * @author Dr. Cesar Guzman
 */
public class ControlLayer {

    private static Logger logger = LoggerFactory.getLogger(ControlLayer.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    private IPipeLineLayer pipeline = null;


    /***********************************/
    /************ CONSTRUCTOR **********/
    /***********************************/

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public ControlLayer(boolean thread, boolean serialize) {
        pipeline = new PipeLineJSAT(thread, serialize);
    }


    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void build(String modelName){
        pipeline.build(modelName);
    }

    public void setDataSet(DataSet train, DataSet test){
        pipeline.setDataSet(train, test);
    }

    public void train() {
        pipeline.train();
    }

    public void execute(){
        pipeline.inference();
    }

}