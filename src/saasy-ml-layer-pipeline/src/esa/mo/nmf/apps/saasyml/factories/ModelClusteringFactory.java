package esa.mo.nmf.apps.saasyml.factories;

import jsat.clustering.Clusterer;
import jsat.clustering.FLAME;
import jsat.linear.distancemetrics.DenseSparseMetric;

/**
 * Factory to build Clustering ML models
 *
 * Used as a static class
 *
 * @author Dr. Cesar Guzman
 */
public class ModelClusteringFactory {

    public static Clusterer buildModelFLAME(DenseSparseMetric metric, int k, int iterations) { 
        return new FLAME(metric, k, iterations);
    }
    
}