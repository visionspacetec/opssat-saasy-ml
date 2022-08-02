package esa.mo.nmf.apps.saasyml.factories;

import javax.swing.SwingUtilities;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.service.PipeLineClassifierJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineClusterJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineOutlierJSAT;

import jsat.classifiers.Classifier;
import jsat.classifiers.linear.ALMA2;
import jsat.classifiers.linear.AROW;
import jsat.classifiers.linear.BBR;
import jsat.classifiers.linear.LinearBatch;
import jsat.classifiers.linear.LinearL1SCD;
import jsat.classifiers.linear.LinearSGD;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.classifiers.linear.NHERD;
import jsat.classifiers.linear.NewGLMNET;
import jsat.classifiers.linear.OWA;
import jsat.classifiers.linear.PassiveAggressive;
import jsat.classifiers.linear.SCD;
import jsat.classifiers.linear.SCW;
import jsat.classifiers.linear.SDCA;
import jsat.classifiers.linear.SMIDAS;
import jsat.classifiers.linear.SPA;
import jsat.classifiers.linear.STGD;
import jsat.classifiers.linear.StochasticMultinomialLogisticRegression;
import jsat.classifiers.linear.StochasticSTLinearL1;
import jsat.clustering.Clusterer;
import jsat.clustering.FLAME;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.lossfunctions.LogisticLoss;
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

        return createPipeLine(expId, datasetId, thread, serialize, modelName, typeModel);
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
            // bayesian classifiers
            // boosting classifiers
            // imbalance classifiers
            // knn classifiers
            // linear classifiers
            case "ALMA2" : return new ALMA2();
            case "AROW" : return new AROW(1, true);
            case "BBR" : return new BBR(0.01, 1000, BBR.Prior.GAUSSIAN);
            case "LinearBatch" : return new LinearBatch();
            case "LinearL1SCD" : return new LinearL1SCD();
            case "LinearSGD" : return new LinearSGD();
            default:
            case "LogisticRegressionDCD" : return new LogisticRegressionDCD();
            case "NewGLMNET" : return new NewGLMNET();
            case "NHERD" : return new NHERD(1, NHERD.CovMode.FULL);
            case "PassiveAggressive" : return new PassiveAggressive();
            case "SCD" : return new SCD(new LogisticLoss(), 1e-6, 100);
            case "SCW" : return new SCW();
            case "SDCA" : return new SDCA();
            case "SMIDAS" : return new SMIDAS(0.1);
            case "SPA" : return new SPA();
            case "STGD" : return new STGD(5, 0.1, Double.POSITIVE_INFINITY, 0.1);
            case "StochasticMultinomialLogisticRegression": return new StochasticMultinomialLogisticRegression();
        
            // neuralnetwork
            // svm classifiers
            // tress classifiers
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
            case "ALMA2" :
            case "AROW" :
            case "BBR" : 
            case "LinearBatch" : 
            case "LinearL1SCD" : 
            case "LinearSGD" :
            case "LogisticRegressionDCD" : 
            case "NewGLMNET" :
            case "NHERD" : 
            case "PassiveAggressive" :
            case "SCD" :
            case "SCW" : 
            case "SDCA" :
            case "SMIDAS" :
            case "SPA" :
            case "STGD" : 
            case "StochasticMultinomialLogisticRegression":
                return TypeModel.Classifier;
            case "FLAME" :                  return TypeModel.Cluster;
            case "IsolationForest":         return TypeModel.Outlier;
        }

        return TypeModel.Unknown;
    }
}