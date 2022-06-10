package esa.mo.nmf.apps.saasyml.common;

/**
 * Interface of the IPipeLine layer.
 *
 * @author Dr. Cesar Guzman
 */
public interface IPipeLineLayer {

    /**
     * Build the model with the parameters (e.g., group and name of the algorithm)
     *
     * @param type of inference to realize (e.g., classification, Outlier, etc)
     * @param set of parameters
     */
    public void build(String type, String[] parameters);

    /**
     * Start train of the model
     */
    public void train();

    /**
     * Start inference the model
     */
    public void inference();

}