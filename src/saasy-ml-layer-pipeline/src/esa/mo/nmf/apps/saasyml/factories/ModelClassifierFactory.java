package esa.mo.nmf.apps.saasyml.factories;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.service.PipeLineClassifierJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineClusterJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineOutlierJSAT;
import esa.mo.nmf.apps.saasyml.service.PipeLineRegressorJSAT;
import jsat.classifiers.Classifier;
import jsat.classifiers.linear.ALMA2;
import jsat.classifiers.linear.kernelized.ALMA2K;
import jsat.classifiers.linear.AROW;
import jsat.classifiers.linear.BBR;
import jsat.classifiers.linear.LinearBatch;
import jsat.classifiers.linear.LinearL1SCD;
import jsat.classifiers.linear.LinearSGD;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.classifiers.linear.NHERD;
import jsat.classifiers.linear.NewGLMNET;
import jsat.classifiers.linear.PassiveAggressive;
import jsat.classifiers.linear.SCD;
import jsat.classifiers.linear.SCW;
import jsat.classifiers.linear.SDCA;
import jsat.classifiers.linear.SMIDAS;
import jsat.classifiers.linear.SPA;
import jsat.classifiers.linear.STGD;
import jsat.classifiers.linear.StochasticMultinomialLogisticRegression;
import jsat.classifiers.linear.StochasticSTLinearL1;
import jsat.classifiers.linear.BBR.Prior;
import jsat.clustering.Clusterer;
import jsat.clustering.FLAME;
import jsat.distributions.kernels.KernelTrick;
import jsat.distributions.kernels.RBFKernel;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.lossfunctions.HingeLoss;
import jsat.lossfunctions.LogisticLoss;
import jsat.math.optimization.stochastic.AdaGrad;
import jsat.math.optimization.stochastic.GradientUpdater;
import jsat.math.optimization.stochastic.RMSProp;
import jsat.math.optimization.stochastic.SimpleSGD;
import jsat.lossfunctions.SquaredLoss;
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
public class ModelClassifierFactory {

    static GradientUpdater[] updaters = new GradientUpdater[] { new SimpleSGD(), new AdaGrad(),
            new RMSProp() };
        
    /**
     * Generate classifier model ALMA2
     * @param modelName a string that holds name of the model to create
     * @return Classifier model
     */
    public static Classifier buildModelALMA2(int epochs) {
        ALMA2 alma = new ALMA2();
        alma.setEpochs(epochs);
        return alma;
    }

    public static Classifier buildModelPassiveAggresive(PassiveAggressive.Mode mode, double eps, int epochs, int C) {
        PassiveAggressive pa = new PassiveAggressive();
        pa.setMode(mode);
        pa.setEps(eps);
        pa.setEpochs(epochs);
        pa.setC(C);
        return pa;
    }

    public static Classifier buildModelSCD(LogisticLoss loss, double regularization, int iterations) {
        return new SCD(loss, regularization, iterations); // regularization: 1e-6
    }

    public static Classifier buildModelLogisticRegressionDCD(boolean useBais) {
        LogisticRegressionDCD lr = new LogisticRegressionDCD();
        lr.setUseBias(useBais);
        return lr;
    }

    public static Classifier buildModelBBR(double regularization, int maxIterations, Prior controlRegularization) {
       return new BBR(regularization, maxIterations, controlRegularization);
    }

    public static Classifier buildModelALMA2K(KernelTrick kernelTrick, double alpha) {
        return new ALMA2K(kernelTrick, alpha);
    }

    public static Classifier buildModelLinearSGD(LogisticLoss loss, double lambda0, double lambda1, boolean useBais,
        int epochs1, double eta, int epochs2, int indexUpdater) {

        LinearSGD linearsgd = new LinearSGD(loss, lambda0, lambda1);
        linearsgd.setUseBias(useBais);

        linearsgd.setGradientUpdater(updaters[indexUpdater]);

        //SGD needs more iterations/data to learn a really close fit
        linearsgd.setEpochs(epochs1);
        if (!(updaters[indexUpdater] instanceof SimpleSGD))//the others need a higher learning rate than the default
        {
            linearsgd.setEta(eta);
            linearsgd.setEpochs(epochs2);//more iters b/c RMSProp probably isn't the best for this overly simple problem
        }

        return linearsgd;
    }

    public static Classifier buildModelSDCA(LogisticLoss loss, double e_out, double lambda, int alpha) 
    {
        SDCA sdca = new SDCA();
        sdca.setLoss(loss);
        sdca.setTolerance(e_out);
        sdca.setLambda(lambda);
        sdca.setAlpha(alpha);
        return sdca;
    }

    public static Classifier buildModelSPA(boolean useBais) {
        SPA spa = new SPA();
        spa.setUseBias(useBais);
        return spa;
    }

    
}