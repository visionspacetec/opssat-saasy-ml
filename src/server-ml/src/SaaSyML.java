import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.Classifier;
import jsat.classifiers.DataPoint;
import jsat.classifiers.DataPointPair;
import jsat.clustering.Clusterer;
import jsat.distributions.Normal;
import jsat.utils.GridDataGenerator;
import jsat.utils.IntSet;
import jsat.utils.random.RandomUtil;
import saasyml.dataset.utils.GenerateDataset;
import saasyml.factories.FactoryMLModels;

import java.util.List;
import java.util.Random;
import java.util.Set;

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
        }

    }

    /**
     * Main function
     * @param args a set of string parameters
     */
    public static void main(String[] args) {

        System.out.println("************* Testing Classifier **************");
        testClassifier();

        System.out.println("************* Testing Clustering **************");
        testClustering();
    }

    /**
     * test the classifier ML model
     */
    private static void testClassifier() {
        // instantiate the class
        SaaSyML saasyml = new SaaSyML();

        // subscribe to the service
        saasyml.subscribe(1, "LogisticRegressionDCD");

        System.out.println("Generate the training dataset: ");
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
    private static void testClustering() {
        // instantiate the class
        SaaSyML saasyml = new SaaSyML();

        // subscribe to the service
        saasyml.subscribe(2, "FLAME");

        System.out.println("Generate training dataset: ");
        GridDataGenerator gdg = new GridDataGenerator(new Normal(0, 0.05), new Random(12), 2, 5);
        SimpleDataSet train = gdg.generateData(100);

        // upload the train dataset
        saasyml.upload(train);

        // deactivate the thread
        saasyml.setThread(false);

        // start training and testing the model
        saasyml.execute();

    }
}