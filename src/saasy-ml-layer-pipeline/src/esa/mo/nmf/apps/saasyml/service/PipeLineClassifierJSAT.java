package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;

import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.Classifier;
import jsat.classifiers.DataPointPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that uses the JSAT library inside the PipeLine
 *
 * @author Dr. Cesar Guzman
 */
public class PipeLineClassifierJSAT extends PipeLineAbstractJSAT {

    private static Logger logger = LoggerFactory.getLogger(PipeLineClassifierJSAT.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    private Classifier model = null;


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
    public PipeLineClassifierJSAT(int expId, int datasetId, boolean thread, boolean serialize, String modelName, MLPipeLineFactory.TypeModel typeModel){
        super(expId, datasetId, thread, serialize, modelName, typeModel);
    }

    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void build(){
        // build the model using the factory pattern
        this.model = MLPipeLineFactory.buildModelClassifier(this.modelName);
    }

    public void build(Object[] parameters){
        this.build();
    }

    public void train() {
        // train the model
        model.train((ClassificationDataSet) train, thread);

        if (serialize) {
            // serialize the model
            this.modelPathSerialized = serializeModel(model);
        }
    }

    public List<Object> inference(){
        if (serialize){
            // deserialize the model
            this.model = deserializeClassifier(this.modelPathSerialized);
        }

        // test the model
        List<Object> result = new ArrayList<Object>();
        for(DataPointPair<Integer> dpp : ((ClassificationDataSet)test).getAsDPPList()){
            logger.info(String.valueOf(dpp.getPair().longValue()));
            int classify = model.classify(dpp.getDataPoint()).mostLikely();
            logger.info(dpp.getPair().longValue()+ " vs " + classify);
            result.add(classify);
        }
        return result;
    }

    /***************************************/
    /************ PRIVATE METHODS **********/
    /***************************************/

    /**
     * Function to deserialize a model
     * @param modelPathSerialized full path name of the serialized model
     * @return the model
     */
    private Classifier deserializeClassifier(String modelPathSerialized) {

        Classifier model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(modelPathSerialized));) {
            model = (Classifier) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }
}