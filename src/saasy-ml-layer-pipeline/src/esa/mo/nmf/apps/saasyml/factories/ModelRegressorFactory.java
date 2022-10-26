package esa.mo.nmf.apps.saasyml.factories;

import java.util.List;

import jsat.distributions.kernels.KernelTrick;
import jsat.distributions.multivariate.MultivariateKDE;
import jsat.regression.AveragedRegressor;
import jsat.regression.KernelRLS;
import jsat.regression.KernelRidgeRegression;
import jsat.regression.MultipleLinearRegression;
import jsat.regression.NadarayaWatson;
import jsat.regression.OrdinaryKriging;
import jsat.regression.RANSAC;
import jsat.regression.Regressor;
import jsat.regression.RidgeRegression;
import jsat.regression.StochasticGradientBoosting;
import jsat.regression.StochasticRidgeRegression;

/**
 * Factory to build Regressor ML models
 * 
 * @author Liliana Medina
 */
public class ModelRegressorFactory {

    /**
     * Generate regressor model AveragedRegressor
     * @param voters The array of voting regressors
     * @return Regressor model
     */
    public static Regressor buildModelAveragedRegressor(List<Regressor> voters) {
        AveragedRegressor avgr = new AveragedRegressor(voters);
        return avgr;
    }
    
    /**
     * Generate regressor model KernelRidgeRegression
     * @return Regressor model
     */
    public static Regressor buildModelKernelRidgeRegression() {
        KernelRidgeRegression krr = new KernelRidgeRegression();
        return krr;
    }

    /**
     * Generate regressor model KernelRLS
     * @param k the kernel trick to use
     * @param errorTolerance the tolerance for errors in the projection
     * @return Regressor model
     */
    public static Regressor buildModelKernelRLS(KernelTrick k, double errorTolerance) {
        KernelRLS krls = new KernelRLS(k, errorTolerance);
        return krls;
    }

    /**
     * Generate regressor model MultipleLinearRegression
     * @return Regressor model
     */
    public static Regressor buildModelMultipleLinearRegression() {
        MultipleLinearRegression mlr = new MultipleLinearRegression();
        return mlr;
    }

    /**
     * Generate regressor model NadarayaWatson
     * @param kde multivariate kernel density estimator
     * @return Regressor model
     */
    public static Regressor buildModelNadarayaWatson(MultivariateKDE kde) {
        NadarayaWatson nw = new NadarayaWatson(kde);
        return nw;
    }    

    /**
     * Generate regressor model OrdinaryKriging
     * @return Regressor model
     */
    public static Regressor buildModelOrdinaryKriging() {
        OrdinaryKriging okr = new OrdinaryKriging();
        return okr;
    }

    /**
     * Generate regressor model OrdinaryKriging.PowVariogram
     * @return Regressor model
     */
    public static Regressor buildModelOrdinaryKrigingPowVariogram() {
        OrdinaryKriging pv = new OrdinaryKriging(new OrdinaryKriging.PowVariogram());
        return pv;
    }

    /** 
     * Generate regressor model RANSAC
     * @param baseRegressor the model to fit using RANSAC
     * @param iterations the number of iterations of the algorithm to perform
     * @param initialTrainSize the number of points to seed each iteration of 
     * training with
     * @param minResultSize the minimum number of inliers to make it into the 
     * model to be considered a possible fit. 
     * @param maxPointError the maximum allowed absolute difference in the 
     * output of the model and the true value for the data point to be added to
     * the inlier set. 
     * @return Regressor model
     */
    public static Regressor buildModelRANSAC(Regressor baseRegressor, int iterations, int initialTrainSize, int minResultSize, double maxPointError) {
        RANSAC ransac = new RANSAC(baseRegressor, iterations, initialTrainSize, minResultSize, maxPointError);
        return ransac;
    }

    /**
     * Generate regressor model RidgeRegression
     * @return Regressor model
     */
    public static Regressor buildModelRidgeRegression() {
        RidgeRegression rr = new RidgeRegression();
        return rr;
    }

    /**
     * Generate regressor model StochasticGradientBoosting
     * @param weakLearner the weak learner to fit to the residuals in each iteration
     * @param maxIterations the maximum number of algorithm iterations to perform
     * @return Regressor model
     */
    public static Regressor buildModelStochasticGradientBoosting(Regressor weakLearner, int maxIterations) {
        StochasticGradientBoosting sgb = new StochasticGradientBoosting(weakLearner, maxIterations);
        return sgb;
    }

    /**
     * Generate regressor model StochasticRidgeRegression
     * @param lambda the regularization term 
     * @param epochs the number of training epochs to perform
     * @param batchSize the batch size for updates
     * @param learningRate the learning rate 
     * @return Regressor model
     */
    public static Regressor buildModelStochasticRidgeRegression(double lambda, int epochs, int batchSize, double learningRate) {
        StochasticRidgeRegression srr = new StochasticRidgeRegression(lambda, epochs, batchSize, learningRate);
        return srr;
    }

}
