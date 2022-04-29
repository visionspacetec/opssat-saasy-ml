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
import saasyml.dataset.utils.GenerateDataset;
import saasyml.factories.FactoryMLModels;

import java.util.*;

/**
 * SaaSy ML implementation of the service layer.
 *
 * @author Dr. Cesar Guzman
 */
public class SaaSyML {

    private boolean thread = false;
    private String modelName = "";

    private DataSet train = null;
    private DataSet test = null;
    private FactoryMLModels.TypeModel typeModel = FactoryMLModels.TypeModel.Unknown;

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
     * PUT / subscribe an experimenter app specifying the name of the model
     *
     * @param id_user id of the user
     * @param modelName name of the model
     */
    public void subscribe(int id_user, String modelName) {
        this.modelName = modelName;
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
        Classifier model = FactoryMLModels.buildClassifier(modelName);

        // train the model
        model.train((ClassificationDataSet) train, thread);

        // test the model
        for(DataPointPair<Integer> dpp : ((ClassificationDataSet)test).getAsDPPList()){
            System.out.println(dpp.getPair().longValue()+ " vs " + model.classify(dpp.getDataPoint()).mostLikely());
        }
    }

    /**
     * Execute ML cluster
     *
     * Train and test the model
     */
    private void cluster() {
        // build the model
        Clusterer model = FactoryMLModels.buildCluster(modelName);

        // train the model
        List<List<DataPoint>> clusters = model.cluster(train);

        // test the model
        Set<Integer> seenBefore = new IntSet();
        for(List<DataPoint> cluster :  clusters)
        {
            int thisClass = cluster.get(0).getCategoricalValue(0);

            if (!seenBefore.contains(thisClass)) {
                for(DataPoint dp : cluster) {
                    System.out.println(thisClass + " vs " + dp.getCategoricalValue(0));
                }
            }
        }
    }

    /**
     * Execute ML classifier
     *
     * Train and test the model
     */
    private void outlier() {
        // build the model
        Outlier model = FactoryMLModels.buildOutlier(modelName);

        // train the model
        model.fit((SimpleDataSet) train, thread);

        // test the model
        double numOutliersInTrain = ((SimpleDataSet)train).getDataPoints().stream().mapToDouble(model::score).filter(x -> x < 0).count();
        System.out.println((numOutliersInTrain / train.size()) + " vs " + 0.05);//Better say something like 95% are inlines!

        double numOutliersInOutliers = ((SimpleDataSet)test).getDataPoints().stream().mapToDouble(model::score).filter(x -> x < 0).count();
        System.out.println((numOutliersInOutliers / test.size()) + " vs " + 0.1);//Better say 90% are outliers!

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
                    System.out.println("Generate train dataset: ");
                    train = GenerateDataset.get2ClassLinear(200, RandomUtil.getRandom());
                }

                if (test == null) {
                    System.out.println("Generate test dataset: ");
                    test = GenerateDataset.get2ClassLinear(10, RandomUtil.getRandom());
                }
                break;

            case Cluster:
                if (train == null) {
                    System.out.println("Generate train dataset: ");
                    GridDataGenerator gdg = new GridDataGenerator(new Normal(0, 0.05), new Random(12), 2, 5);
                    train = gdg.generateData(100);
                }
                break;

            case Outlier:
                int N = 5000;
                if (train == null) {
                    System.out.println("Generate train dataset: ");
                    train = new GridDataGenerator(new Normal(), 1,1,1).generateData(N);
                }

                if (test == null) {
                    System.out.println("Generate test dataset: ");
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
        String howToUse = "$ java -jar saasyml-server-0.1.0-SNAPSHOT.jar -thread [1 | true] -tests [1 2 3 | Classifier Cluster Outlier]";

        // if the thread is available or not
        boolean thread = false;

        // set of tests to execute
        List<String> tests_list = null;

        // if there is no arguments, we add by default three tests
        if (args.length < 1) {

            System.out.println("************* ************************************ ************ **************");
            System.out.println("************* Executing all the three tests with thread = false **************");
            System.out.println("************* ************************************ ************ **************");

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
                System.err.println("Error at argument ");
                return;
            }

            // if it contains the thread option, we added it. By Default it is false
            if (params.containsKey("thread")) {
                thread = Boolean.parseBoolean(params.get("thread").get(0));
            }

            // get the tests list
            tests_list = params.get("tests");
        }

        // for each test, we execute it
        for (String s : tests_list) {

            if (s.equals("1") || s.equals("Classifier")) {
                System.out.println("************* Testing Classifier **************");
                testClassifier(thread);
            }

            if (s.equals("2") || s.equals("Cluster")) {
                System.out.println("************* Testing Clustering **************");
                testClustering(thread);
            }

            if (s.equals("3") || s.equals("Outlier")) {
                System.out.println("************* Testing Outlier **************");
                testOutlier(thread);
            }
        }

        System.out.println("\nHelp of use:\n" + howToUse);
    }

    /**
     * test the classifier ML model
     */
    private static void testClassifier(boolean thread) {
        // instantiate the class
        SaaSyML saasyml = new SaaSyML(thread);

        // subscribe to the service
        saasyml.subscribe(1, "LogisticRegressionDCD");

        System.out.println("Generate training dataset...");
        ClassificationDataSet train = GenerateDataset.get2ClassLinear(200, RandomUtil.getRandom());

        // upload the train dataset
        saasyml.upload(train);

        // deactivate the thread
        saasyml.setThread(false);

        // start training and testing the model
        saasyml.execute();
    }

    /**
     * test the clustering ML model
     */
    private static void testClustering(boolean thread) {
        // instantiate the class
        SaaSyML saasyml = new SaaSyML(thread);

        // subscribe to the service
        saasyml.subscribe(2, "FLAME");

        System.out.println("Generate training dataset...");
        GridDataGenerator gdg = new GridDataGenerator(new Normal(0, 0.05), new Random(12), 2, 5);
        SimpleDataSet train = gdg.generateData(100);

        // upload the train dataset
        saasyml.upload(train);

        // deactivate the thread
        saasyml.setThread(false);

        // start training and testing the model
        saasyml.execute();

    }

    /**
     * test the Outlier ML model
     */
    private static void testOutlier(boolean thread) {
        // instantiate the class
        SaaSyML saasyml = new SaaSyML(thread);

        // subscribe to the service
        saasyml.subscribe(1, "IsolationForest");

        System.out.println("Generate training dataset...");
        int N = 5000;
        SimpleDataSet train = new GridDataGenerator(new Normal(), 1,1,1).generateData(N);

        // upload the train dataset
        saasyml.upload(train);

        // deactivate the thread
        saasyml.setThread(false);

        // start training and testing the model
        saasyml.execute();
    }
}