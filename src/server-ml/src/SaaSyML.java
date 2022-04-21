import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.Classifier;
import jsat.classifiers.DataPointPair;
import jsat.utils.random.RandomUtil;
import saasyml.dataset.generate.FixedProblems;
import saasyml.factories.FactoryMLModels;

public class SaaSyML {

    private boolean thread = false;
    private String modelName = "";

    private ClassificationDataSet train = null;
    private ClassificationDataSet test = null;

    public SaaSyML(){

    }

    public void subscribe(int id_user, String modelName) {
        this.modelName = modelName;
    }

    public void upload(ClassificationDataSet train) {

        upload(train, null);

    }

    public void upload(ClassificationDataSet train, ClassificationDataSet test) {

        // assign the train dataset
        this.train = train;

        // assign the test dataset
        this.test = test;

        // generate random dataset
        generateRandomDataset();

    }

    public void setThread(boolean thread) {
        this.thread = thread;
    }

    private void execute() {

        // build the model
        Classifier lr = FactoryMLModels.build(modelName);

        // train the model
        lr.train(train, thread);

        // test the model
        for(DataPointPair<Integer> dpp : test.getAsDPPList()){
            System.out.println(dpp.getPair().longValue()+ " vs " + lr.classify(dpp.getDataPoint()).mostLikely());
        }
    }

    private void generateRandomDataset() {

        if (train == null) {
            System.out.println("Generate the training dataset: ");
            train = FixedProblems.get2ClassLinear(200, RandomUtil.getRandom());
        }

        if (test == null) {
            System.out.println("Generate the test dataset: ");
            test = FixedProblems.get2ClassLinear(200, RandomUtil.getRandom());
        }

    }

    public static void main(String[] args) {

        SaaSyML saasyml = new SaaSyML();

        saasyml.subscribe(1, "LogisticRegressionDCD");

        System.out.println("Generate the training dataset: ");
        ClassificationDataSet train = FixedProblems.get2ClassLinear(200, RandomUtil.getRandom());

        saasyml.upload(train);

        saasyml.setThread(false);

        saasyml.execute();

    }
}