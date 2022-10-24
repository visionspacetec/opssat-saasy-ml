package esa.mo.nmf.apps.saasyml.factories;

import jsat.outlier.Outlier;
import jsat.outlier.DensityOutlier;
import jsat.outlier.IsolationForest;
import jsat.outlier.LinearOCSVM;
import jsat.outlier.LOF;
import jsat.outlier.LoOP;

/**
 * Factory to build Outlier ML models
 *
 * Used as a static class
 *
 * @author Dr. Cesar Guzman
 */
public class ModelOutlierFactory {

    public static Outlier buildModelDensityOutlier() {
        return new DensityOutlier();
    }

    public static Outlier buildModelIsolationForest() { 
        return new IsolationForest();
    }

    public static Outlier buildModelLinearOCSVM() {
        return new LinearOCSVM();
    }

    public static Outlier buildModelLOF() {
        return new LOF();  
    }

    public static Outlier buildModelLoOP() {
        return new LoOP();
    }
    
}