package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.factories.FactoryMLModels;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

/**
 * Class that uses the JSAT library inside the PipeLine
 *
 * @author Dr. Cesar Guzman
 */
public class PipeLineJSAT implements IPipeLineLayer{

    private static Logger logger = LoggerFactory.getLogger(PipeLineJSAT.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    // to use the thread JVM
    private boolean thread = false;

    // to serialize the model
    private boolean serialize = false;
    private String modelPath = "./models/";
    private String formatDate = "yyyy-MM-dd hh-mm-ss";
    private String modelFileName = modelPath+"{MODEL_NAME}-{THREAD}-{DATE}.model";

    // data set to train and test
    private DataSet train = null;
    private DataSet test = null;

    // name and type of the model
    private String modelNameToExecute = "";
    private FactoryMLModels.TypeModel typeModel = FactoryMLModels.TypeModel.Unknown;


    /***********************************/
    /************ CONSTRUCTOR **********/
    /***********************************/

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     */
    public PipeLineJSAT(boolean thread, boolean serialize){

        this.serialize = serialize;
        this.thread = thread;

        if (serialize){
            try{
                Files.createDirectories(Paths.get(modelPath));
            }catch (Exception ex){
                logger.debug("Error creating the folder model");
            }
        }
    }

    /**************************************/
    /************ PUBLIC METHODS **********/
    /**************************************/

    public void setDataSet(DataSet train, DataSet test) {
        // assign the train dataset
        this.train = train;

        // assign the test dataset
        this.test = test;

        // generate random dataset
        generateRandomDataset();
    }

    public void build(String modelName){
        this.modelNameToExecute = modelName;
        this.typeModel = FactoryMLModels.getTypeModel(modelName);
    }

    public void build(String type, String[] parameters){
        this.build(type);
    }

    public void train(){

    }

    public void inference(){
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


    /***************************************/
    /************ PRIVATE METHODS **********/
    /***************************************/

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

}