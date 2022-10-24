package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;

import jsat.SimpleDataSet;
import jsat.outlier.Outlier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

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
     * @param datasetId
     * @param expId
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     * @param modelName String that holds the name of the model
     * @param typeModel TypeModel that holds the kind of model
     */
    public PipeLineOutlierJSAT(int expId, int datasetId, boolean thread, boolean serialize, String modelName, MLPipeLineFactory.TypeModel typeModel){
        super(expId, datasetId, thread, serialize, modelName, typeModel);
    }

    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void build() {
        // build the model
        this.model = MLPipeLineFactory.buildModelOutlier(this.modelName);
    }

    public void build(Object[] parameters) {
        this.build();
    }

    public void train() {
        // train the model
        this.model.fit((SimpleDataSet) train, thread);

        if (serialize){
            // serialize the model
            this.modelPathSerialized = serializeModel(model);
        }
    }

    public List<Object> inference() {

        if (serialize){
            // deserialize the model
            this.model = deserializeOutlier(modelPathSerialized);
        }

        List<Object> result = new ArrayList<Object>();

        // score the test data with the trained model
        Supplier<DoubleStream> recordsOutliersTest = () -> ((SimpleDataSet) test).getDataPoints().stream()
                .mapToDouble(model::score);
        
        // store only the score that are outliers (any value less than )
        Supplier<DoubleStream> outliers = () -> recordsOutliersTest.get().filter(x -> x < 0);

        // We store the score of all outliers
        // 
        // should we store only the outliers? If so, we should change recordsOutliersTest -> outliers
        recordsOutliersTest.get().forEach(n -> result.add(n));

        // get the number of outliers
        double numOutliers = outliers.get().count();

        logger.info("original vs inference : " + (numOutliers / test.size()) + " vs " + 0.1); // Better say 90% are outliers!

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
    private Outlier deserializeOutlier(String modelPathSerialized) {

        Outlier model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(modelPathSerialized));) {
            model = (Outlier) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }

}