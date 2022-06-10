package esa.mo.nmf.apps.saasyml.common;

import jsat.DataSet;

/**
 * Interface of the Service layer.
 *
 * @author Dr. Cesar Guzman
 */
public interface IServiceLayer {

    /**
     * PUT / subscribe an experimenter app
     *
     * @param id_user id of the user
     */
    public void subscribe(int id);

    /**
     * PUT / upload the train dataset adding the test dataset as null
     *
     * @param train abstract class of the train DataSet
     */
    public void upload(DataSet train);

    /**
     * PUT / upload the train and test dataset
     *
     * @param train abstract class of the train DataSet
     * @param test abstract class of the test DataSet
     */
    public void upload(DataSet train, DataSet test);

    /**
     * PUT / change the type of train and the parameters (e.g., group and name of the algorithm)
     *
     * @param type of inference to realize (e.g., classification, Outlier, etc)
     * @param set of parameters
     */
    public void train(String type, String[] parameters);

    /**
     * Start training and testing (inference) the model
     */
    public void execute();
}
