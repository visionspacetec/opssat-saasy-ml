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
    
    /**
     * Generate outlier model DensityOutlier
     * @return Outlier model
     */
    public static Outlier buildModelDensityOutlier() {
        return new DensityOutlier();
    }

    /**
     * Generate outlier model IsolationForest
     * @return Outlier model
     */
    public static Outlier buildModelIsolationForest() { 
        return new IsolationForest();
    }

    /**
     * Generate outlier model LinearOCSVM
     * @return Outlier model
     */
    public static Outlier buildModelLinearOCSVM() {
        return new LinearOCSVM();
    }

    /**
     * Generate outlier model LOF
     * @return Outlier model
     */
    public static Outlier buildModelLOF() {
        return new LOF();  
    }

    /**
     * Generate outlier model LoOP
     * @return Outlier model
     */
    public static Outlier buildModelLoOP() {
        return new LoOP();
    }
    
}