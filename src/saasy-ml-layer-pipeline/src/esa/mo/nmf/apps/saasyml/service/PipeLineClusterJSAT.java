package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;

import jsat.classifiers.DataPoint;
import jsat.clustering.Clusterer;
import jsat.utils.IntSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

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

    // number of k-neightbors
    private int k = 4;

    /***********************************/
    /************ CONSTRUCTOR **********/
    /***********************************/

    /**
     * Constructor
     * @param datasetId
     * @param expId
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     * @param modelName String that holds the name of the model
     * @param typeModel TypeModel that holds the kind of model
     */
    public PipeLineClusterJSAT(int expId, int datasetId, boolean thread, boolean serialize, String modelName, MLPipeLineFactory.TypeModel typeModel){
        super(expId, datasetId, thread, serialize, modelName, typeModel);
    }

    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void build(){
        // build the model
        this.model = MLPipeLineFactory.buildModelCluster(this.modelName, this.k);
    }

    public void build(Object[] parameters) {
        this.k = (int) parameters[0];
        this.build();
    }

    public void train(){
        // train the model
        this.clusters = model.cluster(train);

        if (serialize){
            // serialize the model
            this.modelPathSerialized = serializeModel(model);
        }
    }

    public List<Object> inference(){

        if (serialize){
            // deserialize the model
            this.model = deserializeCluster(modelPathSerialized);
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

        return null;
    }


    /***************************************/
    /************ PRIVATE METHODS **********/
    /***************************************/

    /**
     * Function to deserialize a model
     * @param modelPathSerialized full path name of the serialized model
     * @return the model
     */
    private Clusterer deserializeCluster(String modelPathSerialized) {

        Clusterer model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(modelPathSerialized));) {
            model = (Clusterer) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }
}