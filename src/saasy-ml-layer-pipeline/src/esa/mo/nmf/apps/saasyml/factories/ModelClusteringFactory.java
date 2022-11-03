package esa.mo.nmf.apps.saasyml.factories;

import java.util.Random;

import jsat.clustering.CLARA;
import jsat.clustering.Clusterer;
import jsat.clustering.DBSCAN;
import jsat.clustering.FLAME;
import jsat.clustering.GapStatistic;
import jsat.clustering.HDBSCAN;
import jsat.clustering.KClusterer;
import jsat.clustering.LSDBC;
import jsat.clustering.MeanShift;
import jsat.clustering.OPTICS;
import jsat.clustering.PAM;
import jsat.clustering.SeedSelectionMethods;
import jsat.clustering.dissimilarity.ClusterDissimilarity;
import jsat.clustering.dissimilarity.LanceWilliamsDissimilarity;
import jsat.clustering.evaluation.ClusterEvaluation;
import jsat.clustering.hierarchical.DivisiveGlobalClusterer;
import jsat.clustering.hierarchical.DivisiveLocalClusterer;
import jsat.clustering.hierarchical.NNChainHAC;
import jsat.clustering.hierarchical.SimpleHAC;
import jsat.clustering.kmeans.GMeans;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.linear.distancemetrics.DenseSparseMetric;
import jsat.linear.distancemetrics.DistanceMetric;

/**
 * Factory to build Clustering ML models
 *
 * Used as a static class
 *
 * @author Dr. Cesar Guzman
 * @author Liliana Medina
 */
public class ModelClusteringFactory {

    /**
     * Generate clusterer model CLARA
     * @param sampleCount number of times PAM will be applied to different samples from the data set.
     * @return Clusterer model
     */
    public static Clusterer buildModelCLARA() {
        CLARA clara = new CLARA();
        return clara;
    }

    /**
     * Generate clusterer model DBSCAN
     * @param dm distance metric
     * @return Clusterer model
     */
    public static Clusterer buildModelDBSCAN(DistanceMetric dm) { 
        return new DBSCAN(dm);        
    }

    /**
     * Generate clusterer model DivisiveGlobalClusterer
     * @param baseClusterer base KClusterer
     * @param clusterEvaluation cluster evaluation
     * @return Clusterer model
     */
    public static Clusterer buildModelDivisiveGlobalClusterer(KClusterer baseClusterer, ClusterEvaluation clusterEvaluation) {
        return new DivisiveGlobalClusterer(baseClusterer, clusterEvaluation);
    }

    /**
     * Generate clusterer model DivisiveLocalClusterer
     * @param baseClusterer base KClusterer
     * @param clusterEvaluation cluster evaluation
     * @return Clusterer model
     */
    public static Clusterer buildModelDivisiveLocalClusterer(KClusterer baseClusterer, ClusterEvaluation clusterEvaluation) {
        return new DivisiveLocalClusterer(baseClusterer, clusterEvaluation);
    }

    /**
     * Generate clusterer model FLAME
     * @param metric the distance metric to use
     * @param k the number of neighbors to consider
     * @param iterations the maximum number of iterations to perform
     * @return Clusterer model
     */
    public static Clusterer buildModelFLAME(DenseSparseMetric metric, int k, int iterations) { 
        return new FLAME(metric, k, iterations);
    }

    /**
     * Generate clusterer model GapStatistic
     * @param baseKClusterer base clustering algorithm
     * @return Clusterer model
     */
    public static Clusterer buildModelGapStatistic(KClusterer baseKClusterer) {
        return new GapStatistic(baseKClusterer);
    }

    /**
     * Generate clusterer model GMeans
     * @return Clusterer model
     */
    public static Clusterer buildModelGMeans() {
        return new GMeans();
    }

    /**
     * Generate clusterer model HamerlyKMeans
     * @return Clusterer model
     */
    public static Clusterer buildModelHamerlyKMeans() { 
        return new HamerlyKMeans();
    }

    /**
     * Generate clusterer model HDBSCAN
     * @return Clusterer model
     */
    public static Clusterer buildModelHDBSCAN() { 
        return new HDBSCAN();        
    }

    /**
     * Generate clusterer model LSDBC
     * @return Clusterer model
     */
    public static Clusterer buildModelLSDBC() { 
        return new LSDBC();        
    }

    /**
     * Generate clusterer model MeanShift
     * @return Clusterer model
     */
    public static Clusterer buildModelMeanShift() {
        return new MeanShift();
    }

    /**
     * Generate clusterer model NNChainHAC
     * @param distMeasure the dissimilarity measure to use
     * @return Clusterer model
     */
    public static Clusterer buildModelNNChainHAC(LanceWilliamsDissimilarity distMeasure) {
        return new NNChainHAC(distMeasure);
    }

    /**
     * Generate clusterer model SimpleHAC
     * @param distMeasure dissimilarity measure
     * @return Clusterer model
     */
    public static Clusterer buildModelSimpleHAC(ClusterDissimilarity distMeasure) {
        return new SimpleHAC(distMeasure);
    }

    /**
     * Generate clusterer model OPTICS
     * @return Clusterer model
     */
    public static Clusterer buildModelOPTICS() {
        return new OPTICS();
    }

    /**
     * Generate clusterer model PAM
     * @param dm distance metric
     * @param rand 
     * @param seedSelection seed selection method
     * @return Clusterer model
     */
    public static Clusterer buildModelPAM(DistanceMetric dm, Random rand, SeedSelectionMethods.SeedSelection seedSelection) {
        return new PAM(dm, rand, seedSelection);
    }
    
}