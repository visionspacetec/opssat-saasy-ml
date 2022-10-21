package esa.mo.nmf.apps.saasyml.factories;

import jsat.classifiers.Classifier;
import jsat.classifiers.linear.ALMA2;
import jsat.classifiers.linear.kernelized.ALMA2K;
import jsat.classifiers.linear.BBR;
import jsat.classifiers.linear.LinearSGD;
import jsat.classifiers.linear.LogisticRegressionDCD;
import jsat.classifiers.linear.PassiveAggressive;
import jsat.classifiers.linear.SCD;
import jsat.classifiers.linear.SDCA;
import jsat.classifiers.linear.SPA;
import jsat.classifiers.linear.BBR.Prior;
import jsat.distributions.kernels.KernelTrick;
import jsat.lossfunctions.LogisticLoss;
import jsat.math.optimization.stochastic.AdaGrad;
import jsat.math.optimization.stochastic.GradientUpdater;
import jsat.math.optimization.stochastic.RMSProp;
import jsat.math.optimization.stochastic.SimpleSGD;

/**
 * Factory to build Classifier ML models, identify the ML model type
 *
 * Used as a static class
 *
 * @author Dr. Cesar Guzman
 */
public class ModelClassifierFactory {

    static GradientUpdater[] updaters = new GradientUpdater[] {
        new SimpleSGD(),
        new AdaGrad(),
        new RMSProp() };
        
    /**
     * Generate classifier model ALMA2
     * @param epoch an integer that holds number of epochs
     * @return Classifier model
     */
    public static Classifier buildModelALMA2(int epochs) {
        ALMA2 alma = new ALMA2();
        alma.setEpochs(epochs);
        return alma;
    }

    /**
     * Generate classifier model Passive Aggresive
     * @param mode  Controls which version of the Passive Aggressive update is used
     * @param eps range for numerical prediction. If it is within range of the given value, no error will be incurred. 
     * @param epochs  number of whole iterations through the training set that will be performed for training
     * @param C Increasing the value of this parameter increases the aggressiveness of the algorithm. It must be a positive 
     * value. This parameter essentially performs a type of regularization on the updates
     * @return Classifier model
     */
    public static Classifier buildModelPassiveAggresive(PassiveAggressive.Mode mode, double eps, int epochs, int C) {
        PassiveAggressive pa = new PassiveAggressive();
        pa.setMode(mode);
        pa.setEps(eps);
        pa.setEpochs(epochs);
        pa.setC(C);
        return pa;
    }

    /**
     * Generate classifier model SCD
     * @param loss the loss function to use
     * @param regularization the regularization term to used
     * @param iterations the number of iterations to perform
     * @return Classifier model
     */
    public static Classifier buildModelSCD(LogisticLoss loss, double regularization, int iterations) {
        return new SCD(loss, regularization, iterations); // regularization: 1e-6
    }

    /**
     * Generate classifier model Logisitic Regression DCD
     * @param useBais whether or not an implicit bias term should be added to the model. 
     * @return Classifier model
     */
    public static Classifier buildModelLogisticRegressionDCD(boolean useBais) {
        LogisticRegressionDCD lr = new LogisticRegressionDCD();
        lr.setUseBias(useBais);
        return lr;
    }

    /**
     * Generate classifier model BBR
     * @param regularization the regularization penalty to apply
     * @param maxIterations the maximum number of training iterations to perform
     * @param prior the prior to apply for regularization
     * @return Classifier model
     */
    public static Classifier buildModelBBR(double regularization, int maxIterations, Prior controlRegularization) {
       return new BBR(regularization, maxIterations, controlRegularization);
    }

    /**
     * Generate classifier model ALMA2K
     * @param kernel the kernel function to use
     * @param alpha the alpha parameter of ALMA
     * @return Classifier model
     */
    public static Classifier buildModelALMA2K(KernelTrick kernelTrick, double alpha) {
        return new ALMA2K(kernelTrick, alpha);
    }

    /**
     * Generate classifier model SGD
     * @param loss the loss function to use
     * @param lambda0 the L<sub>2</sub> regularization term
     * @param lambda1 the L<sub>1</sub> regularization term
     * @param useBais whether or not an implicit bias term should be added to the model
     * @param epochs1  number of whole iterations through the training set that will be performed for training
     * @param eta initial learning rate &eta; to use. It should generally be in (0, 1), but any positive value is acceptable. 
     * @param epochs2  number of whole iterations through the training set that will be performed for training
     * @param indexUpdater gradient updater to use
     * @return Classifier model
     */
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

    /**
     * Generate classifier model SDCA
     * @param loss the loss function to use
     * @param e_out the tolerance parameter for convergence. Smaller values will be more exact, but larger values will converge faster.
     * @param lambda the regularization term, where larger values indicate a larger regularization penalty. 
     * @param alpha  the value in [0, 1] for determining the regularization penalty's interpolation between pure L<sub>2</sub> and L<sub>1</sub> regularization.
     * @return Classifier model
     */
    public static Classifier buildModelSDCA(LogisticLoss loss, double e_out, double lambda, int alpha) 
    {
        SDCA sdca = new SDCA();
        sdca.setLoss(loss);
        sdca.setTolerance(e_out);
        sdca.setLambda(lambda);
        sdca.setAlpha(alpha);
        return sdca;
    }

    /**
     * Generate classifier model SPA
     * @param useBais whether or not an implicit bias term should be added to the model. 
     * @return Classifier model
     */
    public static Classifier buildModelSPA(boolean useBais) {
        SPA spa = new SPA();
        spa.setUseBias(useBais);
        return spa;
    }
}