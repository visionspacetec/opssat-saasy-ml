package esa.mo.nmf.apps.saasyml.common;

import java.util.List;

import jsat.DataSet;

/**
 * Interface of the IPipeLine layer.
 *
 * @author Dr. Cesar Guzman
 */
public interface IPipeLineLayer {

    /**
     * Set train and test dataset
     *
     * @param train abstract class of the train DataSet
     * @param test abstract class of the test DataSet
     */
    public void setDataSet(DataSet train, DataSet test);

    /**
     * Build the model with the parameters (e.g., group and name of the algorithm)
     *
     * @param type of inference to realize (e.g., classification, Outlier, etc)
     * @param set of parameters
     */
    public void build(Object[] parameters);
    public void build();

    /**
     * Start train of the model
     */
    public void train();
    
    /**
     * Retrieve the path of the serialized model
     * @return string that holds the path of the serialized model
     */
    public String getModelPathSerialized();
    
    /**
     * Set the path of the serialized model
     * @return string that holds the path of the serialized model
     */
    public void setModelPathSerialized(String modelPathSerialized);

    /**
     * Begin the inference process using the trained model
     * @return 
     */
    public List<Object> inference();

}