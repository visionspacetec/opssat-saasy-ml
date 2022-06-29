package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;

import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.Clusterer;
import jsat.distributions.Normal;
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
public class PipeLineClusterJSAT extends PipeLineAbstractJSAT{

    private static Logger logger = LoggerFactory.getLogger(PipeLineClusterJSAT.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    private Clusterer model = null;

    // clusters getting during the train model
    private List<List<DataPoint>> clusters;

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
    public PipeLineClusterJSAT(boolean thread, boolean serialize, String modelName, MLPipeLineFactory.TypeModel typeModel){
        super(thread, serialize, modelName, typeModel);
    }

    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void build(String modelName){
        // build the model
        this.model = MLPipeLineFactory.buildModelCluster(this.modelName);
    }

    public void build(String type, String[] parameters){
        this.build(type);
    }

    public void train(){
        // train the model
        this.clusters = model.cluster(train);
    }

    public void inference(){

        if (serialize){
            // serialize the model
            String pathToSerializedModel = serializeModel(model);

            // deserialize the model
            this.model = deserializeCluster(pathToSerializedModel);
        }

        // test the model
        Set<Integer> seenBefore = new IntSet();
        for(List<DataPoint> cluster : this.clusters)
        {
            int thisClass = cluster.get(0).getCategoricalValue(0);

            if (!seenBefore.contains(thisClass)) {
                for(DataPoint dp : cluster) {
                    logger.info(thisClass + " vs " + dp.getCategoricalValue(0));
                }
            }
        }
    }


    /***************************************/
    /************ PRIVATE METHODS **********/
    /***************************************/

    /**
     * Function to deserialize a model
     * @param pathToSerializedModel full path name of the model
     * @return the model
     */
    private Clusterer deserializeCluster(String pathToSerializedModel) {

        Clusterer model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(pathToSerializedModel));) {
            model = (Clusterer) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }
}