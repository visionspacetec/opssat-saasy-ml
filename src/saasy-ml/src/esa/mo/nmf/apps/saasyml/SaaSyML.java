package esa.mo.nmf.apps.saasyml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import esa.mo.nmf.apps.saasyml.test.ServiceLayerTest;
import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.Classifier;
import jsat.classifiers.DataPoint;
import jsat.classifiers.DataPointPair;
import jsat.clustering.Clusterer;
import jsat.distributions.Normal;
import jsat.outlier.Outlier;
import jsat.utils.GridDataGenerator;
import jsat.utils.IntSet;
import jsat.utils.random.RandomUtil;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;
import esa.mo.nmf.apps.saasyml.factories.FactoryMLModels;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

// import org.apache.logging.log4j.Logger;
// import org.apache.logging.log4j.LogManager;

/**
 * SaaSy ML implementation of the SaaSy ML entry point.
 *
 * @author Dr. Cesar Guzman
 */
public class SaaSyML {

    // private static Logger logger = LogManager.getLogger(esa.mo.nmf.apps.saasyml.ServiceLayer.class);
    private static Logger logger = LoggerFactory.getLogger(ServiceLayer.class);

    private boolean thread = false;
    private String modelNameToExecute = "";

    private DataSet train = null;
    private DataSet test = null;
    private FactoryMLModels.TypeModel typeModel = FactoryMLModels.TypeModel.Unknown;


    // Attributes for serialize the model
    private boolean serialize = false;
    private String modelPath = "./models/";
    private String formatDate = "yyyy-MM-dd hh-mm-ss";
    private String modelFileName = modelPath+"{MODEL_NAME}-{THREAD}-{DATE}.model";


    /**
     * Empty constructor
     */
    public SaaSyML() { }

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     */
    public SaaSyML(boolean thread) {
        this.thread = thread;
    }

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public SaaSyML(boolean thread, boolean serialize) {
        this.thread = thread;
        this.serialize = serialize;

        if (serialize){
            try{
                Files.createDirectories(Paths.get(modelPath));
            }catch (Exception ex){
                logger.debug("Error creating the folder model");
            }
        }

    }

    /**
     * PUT / subscribe an experimenter app specifying the name of the model
     *
     * @param id_user id of the user
     * @param modelName name of the model
     */
    public void subscribe(int id_user, String modelName) {
        this.modelNameToExecute = modelName;
        this.typeModel = FactoryMLModels.getTypeModel(modelName);
    }

    /**
     * PUT / upload the train dataset adding the test dataset as null
     *
     * @param train abstract class of the train dataset
     */
    public void upload(DataSet train) {
        upload(train, null);
    }

    /**
     * PUT / upload the train and test dataset
     *
     * @param train abstract class of the train dataset
     * @param test abstract class of the test dataset
     */
    public void upload(DataSet train, DataSet test) {

        // assign the train dataset
        this.train = train;

        // assign the test dataset
        this.test = test;

        // generate random dataset
        generateRandomDataset();

    }

    /**
     * PUT / set the thread
     *
     * @param thread true if the JSAT library can execute in parallel
     */
    public void setThread(boolean thread) {
        this.thread = thread;
    }

    /**
     * Start training and testing the model
     */
    public void execute() {

        switch (typeModel) {
            case Classifier:
                classier();
                break;
            case Cluster:
                cluster();
                break;
            case Outlier:
                outlier();
                break;
        }

    }

    /**
     * Execute ML classifier
     *
     * Train and test the model
     */
    private void classier() {
        // build the model
        Classifier model = FactoryMLModels.buildClassifier(modelNameToExecute);

        // train the model
        model.train((ClassificationDataSet) train, thread);

        if (serialize){
            // serialize the model
            String pathToSerializedModel = serializeModel(model);

            // deserialize the model
            model = deserializeClassifier(pathToSerializedModel);
        }

        // test the model
        for(DataPointPair<Integer> dpp : ((ClassificationDataSet)test).getAsDPPList()){
            logger.info(dpp.getPair().longValue()+ " vs " + model.classify(dpp.getDataPoint()).mostLikely());
        }
    }

    /**
     * Function to serialize the model in a file
     *
     * @param model that holds the model to serialize
     * @return full path name of the model
     */
    private String serializeModel(Object model) {

        String date = new SimpleDateFormat(this.formatDate).format(new Date());
        String pathToSerializedModel = this.modelFileName.replace("{MODEL_NAME}", this.modelNameToExecute);
        pathToSerializedModel = pathToSerializedModel.replace("{DATE}", date);
        pathToSerializedModel = pathToSerializedModel.replace("{THREAD}", (this.thread)?"1":"0");

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pathToSerializedModel));) {
            oos.writeObject(model);
        }catch (Exception e){ logger.debug("Error serializing the model"); }

        return pathToSerializedModel;
    }

    /**
     * Function to deserialize a model
     * @param pathToSerializedModel full path name of the model
     * @return the model
     */
    private Classifier deserializeClassifier(String pathToSerializedModel) {

        Classifier model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(pathToSerializedModel));) {
            model = (Classifier) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }

    /**
     * Execute ML cluster
     *
     * Train and test the model
     */
    private void cluster() {
        // build the model
        Clusterer model = FactoryMLModels.buildCluster(modelNameToExecute);

        if (serialize){
            // serialize the model
            String pathToSerializedModel = serializeModel(model);

            // deserialize the model
            model = deserializeCluster(pathToSerializedModel);
        }

        // train the model
        List<List<DataPoint>> clusters = model.cluster(train);

        // test the model
        Set<Integer> seenBefore = new IntSet();
        for(List<DataPoint> cluster :  clusters)
        {
            int thisClass = cluster.get(0).getCategoricalValue(0);

            if (!seenBefore.contains(thisClass)) {
                for(DataPoint dp : cluster) {
                    logger.info(thisClass + " vs " + dp.getCategoricalValue(0));
                }
            }
        }
    }

    /**
     * Function to deserialize a model
     * @param pathToSerializedModel full path name of the model
     * @return the model
     */
    private Clusterer deserializeCluster(String pathToSerializedModel) {

        Clusterer model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(pathToSerializedModel));) {
            model = (Clusterer) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }

    /**
     * Execute ML classifier
     *
     * Train and test the model
     */
    private void outlier() {
        // build the model
        Outlier model = FactoryMLModels.buildOutlier(modelNameToExecute);

        // train the model
        model.fit((SimpleDataSet) train, thread);

        if (serialize){
            // serialize the model
            String pathToSerializedModel = serializeModel(model);

            // deserialize the model
            model = deserializeOutlier(pathToSerializedModel);
        }

        // test the model
        double numOutliersInTrain = ((SimpleDataSet)train).getDataPoints().stream().mapToDouble(model::score).filter(x -> x < 0).count();
        logger.info((numOutliersInTrain / train.size()) + " vs " + 0.05);//Better say something like 95% are inlines!

        double numOutliersInOutliers = ((SimpleDataSet)test).getDataPoints().stream().mapToDouble(model::score).filter(x -> x < 0).count();
        logger.info((numOutliersInOutliers / test.size()) + " vs " + 0.1);//Better say 90% are outliers!

    }

    /**
     * Function to deserialize a model
     * @param pathToSerializedModel full path name of the model
     * @return the model
     */
    private Outlier deserializeOutlier(String pathToSerializedModel) {

        Outlier model = null;

        try (ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(pathToSerializedModel));) {
            model = (Outlier) objectinputstream.readObject();
        } catch (Exception e){ logger.debug("Error deserializing the model"); }

        return model;
    }

    /**
     * Generate randomly the train and test dataset
     *
     * For testing propose
     */
    private void generateRandomDataset() {

        switch (typeModel){
            case Classifier:
                if (train == null) {
                    logger.info("Generate train dataset: ");
                    train = GenerateDataset.get2ClassLinear(200, RandomUtil.getRandom());
                }

                if (test == null) {
                    logger.info("Generate test dataset: ");
                    test = GenerateDataset.get2ClassLinear(10, RandomUtil.getRandom());
                }
                break;

            case Cluster:
                if (train == null) {
                    logger.info("Generate train dataset: ");
                    GridDataGenerator gdg = new GridDataGenerator(new Normal(0, 0.05), new Random(12), 2, 5);
                    train = gdg.generateData(100);
                }
                break;

            case Outlier:
                int N = 5000;
                if (train == null) {
                    logger.info("Generate train dataset: ");
                    train = new GridDataGenerator(new Normal(), 1,1,1).generateData(N);
                }

                if (test == null) {
                    logger.info("Generate test dataset: ");
                    test = new GridDataGenerator(new Normal(10, 1.0), 1,1,1).generateData(N);
                }
                break;
        }

    }

    /**
     * Main function
     * @param args a set of string parameters
     */
    public static void main(String[] args) {

        // how to use information
        String howToUse = "$ java -jar saasyml-0.1.0-SNAPSHOT.jar -thread [1 | true] -s [1 | true] -tests [1 2 3 | Classifier Cluster Outlier]";

        // if the thread is available or not
        boolean thread = false;

        // if the serialize is available or not
        boolean serialize = false;

        // set of tests to execute
        List<String> tests_list = null;

        // if there is no arguments, we add by default three tests
        if (args.length < 1) {

            tests_list =  new ArrayList<String>() {
                {
                    add("1");
                    add("2");
                    add("3");
                }
            };

        } else {
            // otherwise, we get the tests by command line

            // we stored all in a tuple <o, v>, where o is an option and v is a set or values (tests)
            final Map<String, List<String>> params = new HashMap<>();

            // for each argument, we take the values
            for (int index = 0; index < args.length; index++) {

                final String arg = args[index];

                if (arg.charAt(0) == '-') {
                    if (arg.length() < 2) {
                        System.err.println("Error at argument " + arg);
                        return;
                    }

                    tests_list = new ArrayList<>();
                    params.put(arg.substring(1), tests_list);

                } else {
                    if (tests_list != null) {
                        tests_list.add(arg);
                    } else {
                        System.err.println("Illegal parameter usage");
                    }
                }
            }

            // if it is empty or id does not contain tests option, we stop
            if (params.isEmpty() || !params.containsKey("tests")) {
                tests_list =  new ArrayList<String>() {
                    {
                        add("1");
                        add("2");
                        add("3");
                    }
                };
            }else{
                // get the tests list
                tests_list = params.get("tests");
            }

            // if it contains the thread option, we added it. By Default, it is false
            if (params.containsKey("thread")) {
                String option = params.get("thread").get(0);
                if ("1".equalsIgnoreCase(option) || "true".equalsIgnoreCase(option))
                    thread = true;
            }

            // if it contains the serialize (s) option, we added it. By Default, it is false
            if (params.containsKey("s")) {
                String option = params.get("s").get(0);
                if ("1".equalsIgnoreCase(option) || "true".equalsIgnoreCase(option))
                    serialize = true;
            }
        }

        logger.info("************* ************************************ ****");
        logger.info("************* Configuration of Execution **************");
        logger.info("- thread: "+thread);
        logger.info("- serialize: "+ serialize);
        logger.info("- tests: "+ tests_list.toString());
        logger.info("************* ************************************ ****\n");

        // for each test, we execute it
        for (String s : tests_list) {

            if (s.equals("1") || s.equals("Classifier")) {
                logger.info("************* Testing Classifier **************");
                ServiceLayerTest.testClassifier(thread, serialize);
            }

            if (s.equals("2") || s.equals("Cluster")) {
                logger.info("************* Testing Clustering **************");
                ServiceLayerTest.testClustering(thread, serialize);
            }

            if (s.equals("3") || s.equals("Outlier")) {
                logger.info("************* Testing Outlier **************");
                ServiceLayerTest.testOutlier(thread, serialize);
            }
        }

        System.out.println("\nHelp of use:\n" + howToUse);

        // ServiceLayerTest.testSettings();
    }

}