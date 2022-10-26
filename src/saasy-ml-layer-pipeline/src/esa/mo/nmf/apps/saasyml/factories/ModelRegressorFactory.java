package esa.mo.nmf.apps.saasyml.factories;

import jsat.regression.KernelRidgeRegression;
import jsat.regression.MultipleLinearRegression;
import jsat.regression.OrdinaryKriging;
import jsat.regression.Regressor;
import jsat.regression.RidgeRegression;

/**
 * Factory to build Regressor ML models
 * 
 * @author Liliana Medina
 */
public class ModelRegressorFactory {
    
    /**
     * Generate regressor model KernelRidgeRegression
     * @return Regressor model
     */
    public static Regressor buildModelKernelRidgeRegression() {
        KernelRidgeRegression krr = new KernelRidgeRegression();
        return krr;
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
     * Generate regressor model OrdinaryKriging
     * @return Regressor model
     */
    public static Regressor buildModelOrdinaryKriging() {
        OrdinaryKriging okr = new OrdinaryKriging();
        return okr;
    }

    /**
     * Generate regressor model RidgeRegression
     * @return Regressor model
     */
    public static Regressor buildModelRidgeRegression() {
        RidgeRegression rr = new RidgeRegression();
        return rr;
    }
}
