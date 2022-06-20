package esa.mo.nmf.apps.saasyml.test;

import esa.mo.nmf.apps.saasyml.SaaSyML;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;


import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.ClassificationDataSet;
import jsat.distributions.Normal;
import jsat.utils.GridDataGenerator;
import jsat.utils.random.RandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SaaSy ML test
 *
 * @author Dr. Cesar Guzman
 */
public class SaaSyMLTest {

    private static Logger logger = LoggerFactory.getLogger(SaaSyMLTest.class);

    /**
     * test the classifier ML model
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public static void testClassifier(boolean thread, boolean serialize) {

        String modelName = "LogisticRegressionDCD";

        // instantiate the class
        SaaSyML saasyml = new SaaSyML(thread, serialize);

        // subscribe to the service
        saasyml.subscribe(1, modelName);

        logger.info("Generate training dataset...");
        DataSet train = GenerateDataset.get2ClassLinear(200, RandomUtil.getRandom());

        // upload the train dataset
        saasyml.upload(train);

        // start training
        saasyml.train();

        // and testing the model
        saasyml.execute();
    }

    /**
     * test the clustering ML model
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public static void testClustering(boolean thread, boolean serialize) {

        String modelName = "FLAME";

        // instantiate the class
        SaaSyML saasyml = new SaaSyML(thread, serialize);

        // subscribe to the service
        saasyml.subscribe(2, modelName);

        logger.info("Generate training dataset...");
        GridDataGenerator gdg = new GridDataGenerator(new Normal(0, 0.05), new Random(12), 2, 5);
        SimpleDataSet train = gdg.generateData(100);

        // upload the train dataset
        saasyml.upload(train);

        // start training
        saasyml.train();

        // and testing the model
        saasyml.execute();

    }

    /**
     * test the Outlier ML model
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public static void testOutlier(boolean thread, boolean serialize) {

        String modelName = "IsolationForest";

        // instantiate the class
        SaaSyML saasyml = new SaaSyML(thread, serialize);

        // subscribe to the service
        saasyml.subscribe(1, modelName);

        logger.info("Generate training dataset...");
        int N = 5000;
        SimpleDataSet train = new GridDataGenerator(new Normal(), 1,1,1).generateData(N);

        // upload the train dataset
        saasyml.upload(train);

        // start training
        saasyml.train();

        // and testing the model
        saasyml.execute();
    }

    /**
     * test in one execution all the different settings
     */
    public static void testSettings() {

        List<List<String>> list_test = new ArrayList<List<String>>();
        list_test.add(Arrays.asList("1", "2", "3"));
        list_test.add(Arrays.asList("1", "2", "3"));
        list_test.add(Arrays.asList("1", "2", "3"));
        list_test.add(Arrays.asList("1"));
        list_test.add(Arrays.asList("2"));
        list_test.add(Arrays.asList("3"));
        list_test.add(Arrays.asList("1", "2"));
        list_test.add(Arrays.asList("1", "3"));
        list_test.add(Arrays.asList("2", "3"));
        list_test.add(Arrays.asList("1", "2", "3", "1", "2", "3", "1", "2", "3", "1", "2", "3"));

        boolean[] list_thread = new boolean[]{
                false, false, true, false, false, false, false, false, false, false
        };

        boolean[] list_serialize = new boolean[]{
                false, true, true, true, true, true, true, true, true, false
        };

        int length = list_thread.length;

        long[] time_execution = new long[length];

        // for each setting to test
        for (int index = 0; index < length; index++) {

            boolean thread = list_thread[index];
            boolean serialize = list_serialize[index];

            // get the initial time
            time_execution[index] = System.nanoTime();

            for (String s : list_test.get(index)) {

                if (s.equals("1")) {
                    logger.info("************* Testing Classifier **************");
                    SaaSyMLTest.testClassifier(thread, serialize);
                }

                if (s.equals("2")) {
                    logger.info("************* Testing Clustering **************");
                    SaaSyMLTest.testClustering(thread, serialize);
                }

                if (s.equals("3")) {
                    logger.info("************* Testing Outlier **************");
                    SaaSyMLTest.testOutlier(thread, serialize);
                }
            }

            // store the final time execution
            time_execution[index] = (System.nanoTime() - time_execution[index]);
        }

        logger.info("\n************* Final time execution of settings **************");
        // for each setting to test
        for (int index = 0; index < length; index++) {
            double convert = (double) time_execution[index] / 1_000_000_000;
            // long convert = TimeUnit.SECONDS.convert(time_execution[index], TimeUnit.NANOSECONDS);

            logger.info((index+1)+ " : " + convert + " sec");
        }
    }
}
