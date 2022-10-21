package esa.mo.nmf.apps.saasyml.factories;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.service.PipeLineClassifierJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineClusterJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineOutlierJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineRegressorJSAT;
import jsat.classifiers.Classifier;
import jsat.classifiers.linear.BBR;
import jsat.classifiers.linear.NHERD;
import jsat.classifiers.linear.PassiveAggressive;
import jsat.classifiers.linear.SCW;
import jsat.classifiers.linear.StochasticSTLinearL1;
import jsat.clustering.Clusterer;
import jsat.distributions.kernels.RBFKernel;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.lossfunctions.LogisticLoss;
import jsat.outlier.DensityOutlier;
import jsat.outlier.IsolationForest;
import jsat.outlier.LOF;
import jsat.outlier.LinearOCSVM;
import jsat.outlier.LoOP;
import jsat.outlier.Outlier;
import jsat.regression.KernelRidgeRegression;
import jsat.regression.MultipleLinearRegression;
import jsat.regression.OrdinaryKriging;
import jsat.regression.Regressor;
import jsat.regression.RidgeRegression;

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
        Regressor,
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
            case Regressor:
                return new PipeLineRegressorJSAT(expId, datasetId, thread, serialize, modelName, typeModel);
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

            // no working properly
            case "ALMA2":
                return ModelClassifierFactory.buildModelALMA2(5);
            case "PassiveAggressive":
                return ModelClassifierFactory.buildModelPassiveAggresive(PassiveAggressive.Mode.PA, 0.00001, 10000, 20);
            case "SCD":
                return ModelClassifierFactory.buildModelSCD(new LogisticLoss(), 1e-6, 100); 
            case "AROW":
                return ModelClassifierFactory.buildModelAROW(1, true);
            case "NHERD":
                return ModelClassifierFactory.buildModelNHERD(1, NHERD.CovMode.PROJECT); // FULL,DROP, PROJECT no working, 
            case "SCW" : 
                return ModelClassifierFactory.buildModelSCW(0.9, SCW.Mode.SCWI, false);

            
                
            // working properly
            case "ALMA2K":
                return ModelClassifierFactory.buildModelALMA2K(new RBFKernel(0.5), 0.8);
            default:
            case "LogisticRegressionDCD":
                return ModelClassifierFactory.buildModelLogisticRegressionDCD(true); 
            case "BBR":
                return ModelClassifierFactory.buildModelBBR(0.01, 1000, BBR.Prior.GAUSSIAN);
            case "LinearSGD":
                return ModelClassifierFactory.buildModelLinearSGD(new LogisticLoss(), 1e-4, 1e-5, true, 50, 0.5, 100, 2);
            case "SDCA":
                return ModelClassifierFactory.buildModelSDCA(new LogisticLoss(), 1e-10, 0.005, 0); 
            case "SPA":
                return ModelClassifierFactory.buildModelSPA(true); 
            case "LinearBatch":
                return ModelClassifierFactory.buildModelLinearBatch(new LogisticLoss(), 1e-4);
            case "LinearL1SCD":
                return ModelClassifierFactory.buildModelLinearL1SCD(1000, 1e-14, StochasticSTLinearL1.Loss.LOG, true);
            case "NewGLMNET" : 
                return ModelClassifierFactory.buildModelNewGLMNET();
                            

            // not working
            // Liliana to check these again
            case "SMIDAS" : 
                return ModelClassifierFactory.buildModelSMIDAS(0.1);
            case "STGD" : 
                return ModelClassifierFactory.buildModelSTGD(5, 0.1, Double.POSITIVE_INFINITY, 0.1);
            case "StochasticMultinomialLogisticRegression":
                return ModelClassifierFactory.buildModelStochasticMultinomialLogisticRegression();

        
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
            case "FLAME":
                return ModelClusteringFactory.buildModelFLAME(new EuclideanDistance(), k, 800); 
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
            case "IsolationForest":
                return new IsolationForest();
            case "DensityOutlier":
                return new DensityOutlier();
            case "LOF":
                return new LOF();  
            case "LoOP":
                return new LoOP();
            case "LinearOCSVM": // test
                return new LinearOCSVM();
        }
    }

    /**
     * Generate Regressor models
     * @param modelName a string that holds the name of the model to create
     * @return Regressor model
     */
    public static Regressor buildModelRegressor(String modelName) {

        switch (modelName){
            default:
            case "KernelRidgeRegression":
                return new KernelRidgeRegression();
            case "MultipleLinearRegression":
                return new MultipleLinearRegression();
            case "OrdinaryKriging":
                return new OrdinaryKriging();
            case "RidgeRegression":
                return new RidgeRegression();            
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

            case "FLAME":
                return TypeModel.Cluster;
            
            case "IsolationForest": 
            case "DensityOutlier":
            case "LOF":        
            case "LoOP":       
            case "LinearOCSVM":
                return TypeModel.Outlier;

            case "KernelRidgeRegression":
            case "MultipleLinearRegression":
            case "OrdinaryKriging":
            case "RidgeRegression":
                return TypeModel.Regressor;
        }

        return TypeModel.Unknown;
    }
}