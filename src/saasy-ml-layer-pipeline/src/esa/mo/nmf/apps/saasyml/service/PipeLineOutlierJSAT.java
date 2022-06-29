package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.distributions.Normal;
import jsat.outlier.Outlier;
import jsat.utils.GridDataGenerator;
import jsat.utils.IntSet;
import jsat.utils.random.RandomUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

/**
 * Class that uses the JSAT library inside the PipeLine
 *
 * @author Dr. Cesar Guzman
 */
public class PipeLineOutlierJSAT extends PipeLineAbstractJSAT{

    private static Logger logger = LoggerFactory.getLogger(PipeLineOutlierJSAT.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    private Outlier model = null;

    /***********************************/
    /************ CONSTRUCTOR **********/
    /***********************************/

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     * @param modelName String that holds the name of the model
     * @param typeModel TypeModel that holds the kind of model
     */
    public PipeLineOutlierJSAT(boolean thread, boolean serialize, String modelName, MLPipeLineFactory.TypeModel typeModel){
        super(thread, serialize, modelName, typeModel);
    }

    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void build(String modelName){
        // build the model
        this.model = MLPipeLineFactory.buildModelOutlier(this.modelName);
    }

    public void build(String type, String[] parameters){
        this.build(type);
    }

    public void train(){
        // train the model
        this.model.fit((SimpleDataSet) train, thread);
    }

    public void inference(){

        if (serialize){
            // serialize the model
            String pathToSerializedModel = serializeModel(model);

            // deserialize the model
            this.model = deserializeOutlier(pathToSerializedModel);
        }

        // test the model
        double numOutliersInTrain = ((SimpleDataSet)train).getDataPoints().stream().mapToDouble(model::score).filter(x -> x < 0).count();
        logger.info((numOutliersInTrain / train.size()) + " vs " + 0.05);//Better say something like 95% are inlines!

        double numOutliersInOutliers = ((SimpleDataSet)test).getDataPoints().stream().mapToDouble(model::score).filter(x -> x < 0).count();
        logger.info((numOutliersInOutliers / test.size()) + " vs " + 0.1);//Better say 90% are outliers!

    }


    /***************************************/
    /************ PRIVATE METHODS **********/
    /***************************************/

    /**
     * Function to deserialize a model
     * @param pathToSerializedModel full path name of the model
     * @return the model
     */
    private Outlier deserializeOutlier(String pathToSerializedModel) {

        Outlier model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(pathToSerializedModel));) {
            model = (Outlier) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }

}