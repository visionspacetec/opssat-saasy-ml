package esa.mo.nmf.apps.saasyml.common;

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
    public void build(String type, String[] parameters);
    public void build(String type);

    /**
     * Start train of the model
     */
    public void train();

    /**
     * Start inference the model
     */
    public void inference();

}