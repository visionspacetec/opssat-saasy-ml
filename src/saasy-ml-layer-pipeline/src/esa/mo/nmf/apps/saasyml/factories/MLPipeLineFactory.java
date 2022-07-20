package esa.mo.nmf.apps.saasyml.factories;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.service.PipeLineClassifierJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineClusterJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineOutlierJSAT;

import jsat.classifiers.Classifier;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.clustering.Clusterer;
import jsat.clustering.FLAME;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.outlier.IsolationForest;
import jsat.outlier.Outlier;

/**
 * Factory to create ML Pipeline, build ML models, identify the ML model type
 *
 * Used as a static class
 *
 * @author Dr. Cesar Guzman
 */
public class MLPipeLineFactory {

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
     * Factory Design Pattern to create PipeLine
     * @param datasetId
     * @param expId
     * @param thread that holds a boolean to activate the use of threads in the PipeLine
     * @param serialize that holds a boolean to activate the serialization of models
     * @param modelName that holds a String with the name of the model
     * @return
     */
    public static IPipeLineLayer createPipeLine(int expId, int datasetId, boolean thread, boolean serialize, String modelName){

        // detect the type of the model
        TypeModel typeModel = MLPipeLineFactory.getTypeModel(modelName);

        switch(typeModel){
            case Classifier:
                return new PipeLineClassifierJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
            case Cluster:
                return new PipeLineClusterJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
            case Outlier:
                return new PipeLineOutlierJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
            default:
                return new PipeLineClassifierJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
        }
    }

    /**
     * Factory Design Pattern to create PipeLine
     * @param datasetId
     * @param expId
     * @param thread that holds a boolean to activate the use of threads in the PipeLine
     * @param serialize that holds a boolean to activate the serialization of models
     * @param modelName that holds a String with the name of the model
     * @param typeModel that holds the type of model to instantiate
     * @return
     */
    public static IPipeLineLayer createPipeLine(int expId, int datasetId, boolean thread, boolean serialize, String modelName, TypeModel typeModel){

        switch(typeModel){
            case Classifier:
                return new PipeLineClassifierJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
            case Cluster:
                return new PipeLineClusterJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
            case Outlier:
                return new PipeLineOutlierJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
            default:
                return new PipeLineClassifierJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
        }
    }


    /**
     * Generate classifier models
     * @param modelName a string that holds name of the model to create
     * @return Classifier model
     */
    public static Classifier buildModelClassifier(String modelName) {

        switch (modelName){
            default:
            case "LogisticRegressionDCD" : return new LogisticRegressionDCD();
        }
    }

    /**
     * Generate cluster models
     * @param modelName a string that holds the name of the model to create
     * @return Clusterer model
     */
    public static Clusterer buildModelCluster(String modelName, int k) {

        switch (modelName){
            default:
            case "FLAME" : return new FLAME(new EuclideanDistance(), k, 800);
        }
    }

    /**
     * Generate Outlier models
     * @param modelName a string that holds the name of the model to create
     * @return Outlier model
     */
    public static Outlier buildModelOutlier(String modelName) {

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