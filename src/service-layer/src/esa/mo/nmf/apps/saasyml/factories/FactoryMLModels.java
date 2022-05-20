package esa.mo.nmf.apps.saasyml.factories;

import jsat.classifiers.Classifier;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.clustering.Clusterer;
import jsat.clustering.FLAME;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.outlier.IsolationForest;
import jsat.outlier.Outlier;

/**
 * Factory to create ML models and identify the ML model type
 *
 * Used as a static class
 *
 * @author Dr. Cesar Guzman
 */
public class FactoryMLModels {

    /**
     * Define the type of models
     */
    public enum TypeModel {
        Classifier,
        Cluster,
        Outlier,
        Unknown,
    }

    /**
     * generate classifier models
     *
     * @param modelName a string that holds name of the model to create
     * @return Classifier model
     */
    public static Classifier buildClassifier(String modelName) {

        switch (modelName){
            default:
            case "LogisticRegressionDCD" : return new LogisticRegressionDCD();
        }
    }

    /**
     * generate cluster models
     *
     * @param modelName a string that holds the name of the model to create
     * @return Clusterer model
     */
    public static Clusterer buildCluster(String modelName) {

        switch (modelName){
            default:
            case "FLAME" : return new FLAME(new EuclideanDistance(), 30, 800);
        }
    }

    /**
     * generate Outlier models
     *
     * @param modelName a string that holds the name of the model to create
     * @return Outlier model
     */
    public static Outlier buildOutlier(String modelName) {

        switch (modelName){
            default:
            case "IsolationForest" : return new IsolationForest();
        }
    }

    /**
     * Given the name of the model, retrieve the type of model
     * @param modelName a string that holds the name of the model
     * @return TypeModel type of model
     */
    public static TypeModel getTypeModel(String modelName) {

        switch (modelName){
            case "LogisticRegressionDCD" :  return TypeModel.Classifier;
            case "FLAME" :                  return TypeModel.Cluster;
            case "IsolationForest":         return TypeModel.Outlier;
        }

        return TypeModel.Unknown;
    }
}
