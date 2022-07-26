package esa.mo.nmf.apps.saasyml.service;

import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;

import jsat.DataSet;
import jsat.distributions.Normal;
import jsat.utils.GridDataGenerator;
import jsat.utils.random.RandomUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

/**
 * Abstract Class that employs the JSAT library inside the IPipeLineLayer
 *
 * It contains general methods
 *
 * @author Dr. Cesar Guzman
 */
public abstract class PipeLineAbstractJSAT implements IPipeLineLayer{

    private static Logger logger = LoggerFactory.getLogger(PipeLineAbstractJSAT.class);

    /**********************************/
    /************ ATTRIBUTES **********/
    /**********************************/

    // information of the experimenter and dataset
    private int expId;
    private int datasetId;

    // to use the thread JVM
    protected boolean thread = false;

    // to serialize the model
    protected boolean serialize = false;
    private String modelPath = "./models/";
    private String formatDate = "yyyy-MM-dd hh-mm-ss";
    private String modelFileName = modelPath + "E{EXPID}-D{DATASETID}-{MODEL_NAME}-{DATE}.model"; // {THREAD}

    // data set to train and test
    protected DataSet train = null;
    protected DataSet test = null;

    // full path of the model serialized
    protected String modelPathSerialized;

    // name and type of the model
    protected String modelName = "";
    protected MLPipeLineFactory.TypeModel typeModel = MLPipeLineFactory.TypeModel.Unknown;


    /***********************************/
    /************ CONSTRUCTOR **********/
    /***********************************/

    /**
     * Constructor
     *
     * @param thread boolean variable that holds the activation of the thread
     * @param serialize boolean variable that holds if we should serialize the model or not
     * @param modelName String that holds the name of the model
     * @param typeModel TypeModel that holds the kind of model
     */
    public PipeLineAbstractJSAT(int expId, int datasetId, boolean thread, boolean serialize, String modelName,
            MLPipeLineFactory.TypeModel typeModel) {

        this.expId = expId;
        this.datasetId = datasetId;
        this.serialize = serialize;
        this.thread = thread;
        this.modelName = modelName;
        this.typeModel = typeModel;

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

    public abstract void build();

    public void build(Object[] parameters){
        this.build();
    }

    public abstract void train();
    
    public String getModelPathSerialized() {
        if (serialize) {
            // retrieve the path of the model
            return this.modelPathSerialized;
        }
        return "";
    }

    public void setModelPathSerialized(String modelPathStored) {
        this.modelPathSerialized = modelPathStored;
    }

    public abstract List<Object> inference();

    /*****************************************/
    /************ PROTECTED METHODS **********/
    /*****************************************/

    // protected abstract void deserializeModel(String path);


    /***************************************/
    /************ PRIVATE METHODS **********/
    /***************************************/

    /**
     * Generate randomly the train and test dataset
     *
     * For testing propose
     */
    private void generateRandomDataset() {

        switch (typeModel) {
            default:
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
                    train = new GridDataGenerator(new Normal(), 1, 1, 1).generateData(N);
                }

                if (test == null) {
                    logger.info("Generate test dataset: ");
                    test = new GridDataGenerator(new Normal(10, 1.0), 1, 1, 1).generateData(N);
                }
                break;
        }
        
    }

    /**
     * Function to serialize the model in a file
     *
     * @param model that holds the model to serialize
     * @return full path name of the model
     */
    protected String serializeModel(Object model) {

        String date = new SimpleDateFormat(this.formatDate).format(new Date());
        String pathToSerializedModel = this.modelFileName.replace("{EXPID}", Integer.toString(this.expId));
        pathToSerializedModel = pathToSerializedModel.replace("{DATASETID}", Integer.toString(this.datasetId));
        pathToSerializedModel = pathToSerializedModel.replace("{MODEL_NAME}", this.modelName);
        pathToSerializedModel = pathToSerializedModel.replace("{DATE}", date);
        // pathToSerializedModel = pathToSerializedModel.replace("{THREAD}", (this.thread)?"1":"0");

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pathToSerializedModel));) {
            oos.writeObject(model);
        }catch (Exception e){ logger.debug("Error serializing the model"); }

        return pathToSerializedModel;
    }

}