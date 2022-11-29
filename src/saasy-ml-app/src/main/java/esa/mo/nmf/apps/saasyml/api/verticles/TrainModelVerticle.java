package esa.mo.nmf.apps.saasyml.api.verticles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.saasyml.api.Constants;
import esa.mo.nmf.apps.PropertiesManager;
import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jsat.SimpleDataSet;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import jsat.distributions.Normal;
import jsat.linear.DenseVector;
import jsat.regression.RegressionDataSet;

// TODO:
//      - this class does not scale well
//      - consider refactoring using a factory pattern
//      - dedicated class for each training algorithm
public class TrainModelVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(TrainModelVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // log
        LOGGER.log(Level.INFO, "Starting a " + this.getClass().getSimpleName() + " Verticle instance with deployment id " + this.deploymentID() + ".");

        // train classifier
        vertx.eventBus().consumer(Constants.ADDRESS_TRAINING_CLASSIFIER, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.classifier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.KEY_ALGORITHM);

            boolean thread = (payload.containsKey(Constants.KEY_THREAD)
                    && payload.getBoolean(Constants.KEY_THREAD) != null) ? payload.getBoolean(Constants.KEY_THREAD)
                            : PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // the train model will be serialized and saved as a file in the filesystem
            // a reference to the file as well as some metadata will be stored in the database
            // collect all metadata in a Json object
            JsonObject modelMetadata = new JsonObject();
            modelMetadata.put(Constants.KEY_EXPID, expId);
            modelMetadata.put(Constants.KEY_DATASETID, datasetId);
            modelMetadata.put(Constants.KEY_TYPE, Constants.KEY_MODEL_CLASSIFIER);
            modelMetadata.put(Constants.KEY_ALGORITHM, algorithm);

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model 
            saasyml.build();

            // fetch training data and expected labels/clases and then train the model 
            try {

                // get the expected labels
                vertx.eventBus().request(Constants.ADDRESS_LABELS_SELECT_DISTINCT, payload, distinctLabelsResponse -> {
                    JsonObject distinctLabelsJson = (JsonObject) (distinctLabelsResponse.result().body());
                    final JsonArray distinctLabelsJsonArray = distinctLabelsJson.getJsonArray(Constants.KEY_DATA);

                    // check if we actually have expected labels
                    if (distinctLabelsJsonArray.size() > 0) {

                        // get the training input dimension
                        vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payload,
                                dimensionResponse -> {
                                    JsonObject dimensionJson = (JsonObject) (dimensionResponse.result().body());

                                    // check if we actually have training input
                                    if (dimensionJson.containsKey(Constants.KEY_COUNT)
                                            && dimensionJson.getInteger(Constants.KEY_COUNT) > 0) {

                                        // the dimension count
                                        int dimensions = dimensionJson.getInteger(Constants.KEY_COUNT).intValue();

                                        // select all the expected labels/classes
                                        vertx.eventBus().request(Constants.ADDRESS_LABELS_SELECT, payload,
                                                labelsResponse -> {
                                                    JsonObject labelsResponseJson = (JsonObject) (labelsResponse.result().body());
                                                    final JsonArray labelsJsonArray = labelsResponseJson.getJsonArray(Constants.KEY_DATA);

                                                    // select all the training data
                                                    vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT,
                                                            payload, trainingDataResponse -> {
                                                                JsonObject trainingDataResponseJson = (JsonObject) (trainingDataResponse.result().body());
                                                                final JsonArray trainingDataJsonArray = trainingDataResponseJson.getJsonArray(Constants.KEY_DATA);

                                                                // the JSAT training input dataset object
                                                                ClassificationDataSet train = new ClassificationDataSet(
                                                                        dimensions,
                                                                        new CategoricalData[0], new CategoricalData(distinctLabelsJsonArray.size()));

                                                                // the map that will contain all the datapoints and their expected labels
                                                                SortedMap<Integer, JsonObject> trainingDatasetMap = trainDataToSortedMap(trainingDataJsonArray, dimensions);

                                                                // collect all the expected labels into the training dataset map
                                                                trainingDatasetMap = trainDataGetExpectedLabes(trainingDatasetMap, labelsJsonArray);

                                                                // create the data points for training
                                                                for (Map.Entry<Integer, JsonObject> entry : trainingDatasetMap.entrySet()) {

                                                                    if (entry.getValue().containsKey(Constants.KEY_LABEL)) {

                                                                        // build the training data point array
                                                                        // FIXME: training datapoints values are not always of type double
                                                                        // but maybe it's ok to always process as double?
                                                                        // see esa.mo.helpertools.helpers.HelperAttributes.attribute2double(Attribute in)
                                                                        double[] trainingDatapointArray = new double[dimensions];

                                                                        for (int d = 0; d < dimensions; d++) {
                                                                            // FIXME: training datapoints values are not always of type double
                                                                            // but maybe it's ok to always process as double?
                                                                            // see esa.mo.helpertools.helpers.HelperAttributes.attribute2double(Attribute in
                                                                            trainingDatapointArray[d] = Double.valueOf(
                                                                                    entry.getValue().getJsonArray(
                                                                                            Constants.KEY_DATA_POINT)
                                                                                            .getString(d));
                                                                        }

                                                                        // add the data point array as a dense vector and set the expected label
                                                                        train.addDataPoint(
                                                                                new DenseVector(trainingDatapointArray),
                                                                                new int[0],
                                                                                entry.getValue()
                                                                                        .getInteger(Constants.KEY_LABEL)
                                                                                        .intValue());
                                                                    }
                                                                }

                                                                try {
                                                                    // upload the training dataset
                                                                    saasyml.setDataSet(train, null);

                                                                    // enter ML pipeline for the given algorithm
                                                                    // serialize and save the resulting model
                                                                    saasyml.train();

                                                                    // the path to the model file will be stored in the database
                                                                    modelMetadata.put(Constants.KEY_FILEPATH,
                                                                            saasyml.getModelPathSerialized());

                                                                } catch (Exception e) {
                                                                    // the error message will be stored to the database
                                                                    modelMetadata.put(Constants.KEY_ERROR,
                                                                            e.getMessage());
                                                                }

                                                                // save model file path or error message in the models metadata table
                                                                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE,
                                                                        modelMetadata);
                                                            });
                                                });
                                    } else {
                                        // the error message
                                        String errorMsg = "No training dataset input was found to train classifier model.";

                                        // log error message
                                        LOGGER.log(Level.SEVERE, errorMsg);

                                        // save error message in the models metadata table
                                        modelMetadata.put(Constants.KEY_ERROR, errorMsg);
                                        vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                                    }
                                });
                    } else {
                        // the error message
                        String errorMsg = "Missing expected labels to train classifier model.";

                        // log error message
                        LOGGER.log(Level.SEVERE, errorMsg);

                        // save error message in the models metadata table
                        modelMetadata.put(Constants.KEY_ERROR, errorMsg);
                        vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                    }

                    LOGGER.log(Level.INFO,
                            "Trained model will be stored in the filesystem and referenced from the database once training is complete.");
                });

            } catch (Exception e) {
                // the error message
                String errorMsg = "Failed to get training data.";

                // log
                LOGGER.log(Level.SEVERE, errorMsg, e);

                // save error message in the models metadata table
                modelMetadata.put(Constants.KEY_ERROR, errorMsg + ": " + e.getMessage());
                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
            }

            // response
            msg.reply("Training the model(s) has been triggered. Query the " + Constants.ENDPOINT_MODELS
                    + " endpoint for training status.");
        });

        // train outlier
        vertx.eventBus().consumer(Constants.ADDRESS_TRAINING_OUTLIER, msg -> {

            // variables to generate the class randomly
            Random rand = new Random();
            int totalClasses = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.outlier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.KEY_ALGORITHM);

            boolean thread = (payload.containsKey(Constants.KEY_THREAD)
                    && payload.getBoolean(Constants.KEY_THREAD) != null) ? payload.getBoolean(Constants.KEY_THREAD)
                            : PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // the train model will be serialized and saved as a file in the filesystem
            // a reference to the file as well as some metadata will be stored in the database
            // collect all metadata in a Json object
            JsonObject modelMetadata = new JsonObject();
            modelMetadata.put(Constants.KEY_EXPID, expId);
            modelMetadata.put(Constants.KEY_DATASETID, datasetId);
            modelMetadata.put(Constants.KEY_TYPE, Constants.KEY_MODEL_OUTLIER);
            modelMetadata.put(Constants.KEY_ALGORITHM, algorithm);

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
            saasyml.build();

            // train the model 
            try {

                // get the training input dimension
                vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payload, dimensionResponse -> {
                    JsonObject dimensionJson = (JsonObject) (dimensionResponse.result().body());

                    // check if we actually have training input
                    if (dimensionJson.containsKey(Constants.KEY_COUNT)
                            && dimensionJson.getInteger(Constants.KEY_COUNT) > 0) {

                        // the dimension count
                        int dimensions = dimensionJson.getInteger(Constants.KEY_COUNT).intValue();

                        // select all the training data
                        vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payload,
                                trainingDataResponse -> {
                                    // get the response from the select
                                    JsonObject trainingDataResponseJson = (JsonObject) (trainingDataResponse.result()
                                            .body());
                                    final JsonArray trainingDataJsonArray = trainingDataResponseJson.getJsonArray(Constants.KEY_DATA);

                                    // create variables for the k
                                    double[] tempTrainData = new double[dimensions];

                                    List<DataPoint> dataPoints = new ArrayList<DataPoint>();

                                    // variable to control the number of columns
                                    int colCount = 0;

                                    // iterate throughout all the data
                                    for (int pos = 0; pos < trainingDataJsonArray.size(); pos++) {

                                        // get the Json Object and store the value
                                        tempTrainData[colCount++] = colCount
                                                + Double.valueOf(trainingDataJsonArray.getJsonObject(pos).getString(Constants.KEY_VALUE));

                                        // if colcount is equal to total columns, we add a new row
                                        if (colCount == dimensions) {

                                            // add a data point to our train dataset 
                                            // with a random value of the class Y
                                            dataPoints.add(new DataPoint(new DenseVector(tempTrainData),
                                                    new int[] { rand.nextInt(totalClasses) },
                                                    new CategoricalData[] { new CategoricalData(totalClasses) }));

                                            // restart the count of cols to zero and the temporal train data
                                            colCount = 0;
                                            tempTrainData = new double[dimensions];
                                        }
                                    }

                                    LOGGER.log(Level.INFO, "Data points: " + dataPoints.toString());

                                    try {
                                        // create the train dataset
                                        SimpleDataSet train = new SimpleDataSet(dataPoints);

                                        // upload the train dataset
                                        saasyml.setDataSet(train, null);

                                        // enter ML pipeline for the given algorithm
                                        // serialize and save the resulting model
                                        saasyml.train();

                                        // the path to the model file will be stored in the database
                                        modelMetadata.put(Constants.KEY_FILEPATH, saasyml.getModelPathSerialized());

                                    } catch (Exception e) {
                                        // the error message will be stored to the database
                                        modelMetadata.put(Constants.KEY_ERROR, e.getMessage());
                                    }

                                    // save model file path or error message in the models metadata table
                                    vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                                });
                    } else {
                        // the error message
                        String errorMsg = "No training dataset input was found to train outlier model.";

                        // log error message
                        LOGGER.log(Level.SEVERE, errorMsg);

                        // save error message in the models metadata table
                        modelMetadata.put(Constants.KEY_ERROR, errorMsg);

                        // save model file path or error message in the models metadata table
                        vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                    }
                });

            } catch (Exception e) {
                // the error message
                String errorMsg = "Failed to get training data.";

                // log
                LOGGER.log(Level.SEVERE, errorMsg, e);

                // save error message in the models metadata table
                modelMetadata.put(Constants.KEY_ERROR, errorMsg + ": " + e.getMessage());
                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
            }

            // response
            msg.reply("Training the model(s) has been triggered. Query the " + Constants.ENDPOINT_MODELS
                    + " endpoint for training status.");

        });

        // train clustering. 
        vertx.eventBus().consumer(Constants.ADDRESS_TRAINING_CLUSTER, msg -> {

            // variables to generate the class randomly
            Random rand = new Random();         

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.cluster");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.KEY_ALGORITHM);
            final int clusterNumber = payload.getInteger(Constants.KEY_CLUSTER_NUMBER).intValue();

            LOGGER.log(Level.INFO, "Required number of clusters: " + clusterNumber);

            boolean thread = (payload.containsKey(Constants.KEY_THREAD)
                    && payload.getBoolean(Constants.KEY_THREAD) != null) ? payload.getBoolean(Constants.KEY_THREAD)
                            : PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // the train model will be serialized and saved as a file in the filesystem
            // a reference to the file as well as some metadata will be stored in the database
            // collect all metadata in a Json object
            JsonObject modelMetadata = new JsonObject();
            modelMetadata.put(Constants.KEY_EXPID, expId);
            modelMetadata.put(Constants.KEY_DATASETID, datasetId);
            modelMetadata.put(Constants.KEY_TYPE, Constants.KEY_MODEL_CLUSTER);
            modelMetadata.put(Constants.KEY_ALGORITHM, algorithm);

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // Train the model 
            try {

                // build the model with parameters
                saasyml.build(new Integer[] { clusterNumber });

                // get the total number of columns 
                vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payload, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists and it is different from zero, continue
                    if (response.containsKey(Constants.KEY_COUNT)
                            && response.getInteger(Constants.KEY_COUNT) > 0) {

                        payload.put(Constants.KEY_COUNT, response.getInteger(Constants.KEY_COUNT).intValue());

                        // the total number of columns
                        int dimensions = payload.getInteger(Constants.KEY_COUNT);

                        // select all the data from the dataset
                        vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payload, trainingDataResponse -> {

                            // get the response
                            JsonObject trainingDataResponseJson = (JsonObject) (trainingDataResponse.result().body());

                            // if the response object contains the data, we can continue
                            if (trainingDataResponseJson.containsKey(Constants.KEY_DATA)) {

                                LOGGER.log(Level.INFO, "Prepare data ");

                                // get the data with the result of the select
                                final JsonArray data = trainingDataResponseJson.getJsonArray(Constants.KEY_DATA);

                                // Initialize predictors
                                List<DataPoint> dataPoints = new ArrayList<DataPoint>();
                                double[] trainDataRow = new double[dimensions];   
                                // TODO DataPoints need this but won't use its value for training, create issue
                                CategoricalData[] catDataInfo = new CategoricalData[] { 
                                    new CategoricalData(clusterNumber) 
                                };

                                // variable to control the number of columns
                                int colCount = 0;

                                // iterate throughout all the rows in the data
                                for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {

                                    // get the Json Object and store the value
                                    JsonObject object = data.getJsonObject(rowIndex);
                                    trainDataRow[colCount++] = Double.valueOf(object.getString(Constants.KEY_VALUE));

                                    // if colcount is equal to total columns, we add a new row
                                    if (colCount == dimensions) {

                                        // we add a data point to our train dataset 
                                        // with a random value of the class Y, it won't use it
                                        dataPoints.add(new DataPoint(
                                            new DenseVector(trainDataRow),
                                            new int[] { rand.nextInt(clusterNumber) },
                                            catDataInfo));

                                        // we restart the count of cols to zero and the temporal train data
                                        colCount = 0;
                                        trainDataRow = new double[dimensions];
                                    }
                                }

                                LOGGER.log(Level.INFO, "Data points: " + dataPoints.toString());
                                SimpleDataSet train = new SimpleDataSet(dataPoints);

                                try{
                                    // upload the train dataset
                                    saasyml.setDataSet(train, null);

                                    // Train, serialize and save the resulting model
                                    LOGGER.log(Level.INFO, "Executed method train");
                                    saasyml.train();

                                    // Return a message with a path to the serialized model
                                    modelMetadata.put(Constants.KEY_FILEPATH, saasyml.getModelPathSerialized());
                                } catch (Exception e) {
                                    modelMetadata.put(Constants.KEY_ERROR, e.getMessage());
                                }
                                
                                // save model file path or error message in the models metadata table
                                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                            }
                        });
                    } else {
                        String errorMsg = "Failed to get the total number of rows.";

                        LOGGER.log(Level.SEVERE, errorMsg);

                        modelMetadata.put(Constants.KEY_ERROR, errorMsg);
                        vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                    }
                });

            } catch (Exception e) {
                String errorMsg = "Failed to get training data.";

                LOGGER.log(Level.SEVERE, errorMsg, e);

                modelMetadata.put(Constants.KEY_ERROR, errorMsg);
                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
            }
            // response
            msg.reply("Training the model(s) has been triggered. Query the " + Constants.ENDPOINT_MODELS
                    + " endpoint for training status.");
        });

        // train regression
        vertx.eventBus().consumer(Constants.ADDRESS_TRAINING_REGRESSOR, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.regressor");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.KEY_ALGORITHM);

            boolean thread = (payload.containsKey(Constants.KEY_THREAD)
                    && payload.getBoolean(Constants.KEY_THREAD) != null) ? payload.getBoolean(Constants.KEY_THREAD)
                            : PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // the train model will be serialized and saved as a file in the filesystem
            // a reference to the file as well as some metadata will be stored in the database
            // collect all metadata in a Json object
            JsonObject modelMetadata = new JsonObject();
            modelMetadata.put(Constants.KEY_EXPID, expId);
            modelMetadata.put(Constants.KEY_DATASETID, datasetId);
            modelMetadata.put(Constants.KEY_TYPE, Constants.KEY_MODEL_REGRESSOR);
            modelMetadata.put(Constants.KEY_ALGORITHM, algorithm);

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
            saasyml.build();

            // fetch training data and expected values and then train the model 
            try {

                // get the expected values
                vertx.eventBus().request(Constants.ADDRESS_LABELS_SELECT_DISTINCT, payload, distinctLabelsResponse -> {
                    JsonObject distinctLabelsJson = (JsonObject) (distinctLabelsResponse.result().body());
                    final JsonArray distinctLabelsJsonArray = distinctLabelsJson.getJsonArray(Constants.KEY_DATA);

                    // check if we actually have expected labels
                    if (distinctLabelsJsonArray.size() > 0) {

                        // get the training input dimension
                        vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payload,
                                dimensionResponse -> {
                                    JsonObject dimensionJson = (JsonObject) (dimensionResponse.result().body());

                                    // check if we actually have training input
                                    if (dimensionJson.containsKey(Constants.KEY_COUNT)
                                            && dimensionJson.getInteger(Constants.KEY_COUNT) > 0) {

                                        // the dimension count
                                        int dimensions = dimensionJson.getInteger(Constants.KEY_COUNT).intValue();

                                        // select all the expected values
                                        vertx.eventBus().request(Constants.ADDRESS_LABELS_SELECT, payload,
                                                labelsResponse -> {
                                                    JsonObject labelsResponseJson = (JsonObject) (labelsResponse
                                                            .result().body());
                                                    final JsonArray expectedValuesJsonArray = labelsResponseJson
                                                            .getJsonArray(Constants.KEY_DATA);

                                                    // select all the training data
                                                    vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT,
                                                            payload, trainingDataResponse -> {
                                                                JsonObject trainingDataResponseJson = (JsonObject) (trainingDataResponse
                                                                        .result().body());
                                                                final JsonArray trainingDataJsonArray = trainingDataResponseJson
                                                                        .getJsonArray(Constants.KEY_DATA);

                                                                // the JSAT training input dataset object
                                                                RegressionDataSet train = new RegressionDataSet(
                                                                        dimensions,
                                                                        new CategoricalData[0]);

                                                                // the map that will contain all the datapoints and their expected labels
                                                                SortedMap<Integer, JsonObject> trainingDatasetMap = trainDataToSortedMap(trainingDataJsonArray, dimensions);

                                                                // collect all the expected labels into the training dataset map
                                                                trainingDatasetMap = trainDataGetExpectedLabes(trainingDatasetMap, expectedValuesJsonArray);

                                                                // create the data points for training
                                                                for (Map.Entry<Integer, JsonObject> entry : trainingDatasetMap
                                                                        .entrySet()) {

                                                                    if (entry.getValue()
                                                                            .containsKey(Constants.KEY_LABEL)) {

                                                                        // build the training data point array
                                                                        double[] trainingDatapointArray = new double[dimensions];

                                                                        for (int d = 0; d < dimensions; d++) {
                                                                            trainingDatapointArray[d] = Double.valueOf(
                                                                                    entry.getValue().getJsonArray(
                                                                                            Constants.KEY_DATA_POINT)
                                                                                            .getString(d));
                                                                        }

                                                                        // add the data point array as a dense vector and set the expected value
                                                                        train.addDataPoint(
                                                                                new DenseVector(trainingDatapointArray),
                                                                                new int[0],
                                                                                entry.getValue()
                                                                                        .getDouble(Constants.KEY_LABEL)
                                                                                        .doubleValue());
                                                                    }
                                                                }

                                                                try {
                                                                    // upload the training dataset
                                                                    saasyml.setDataSet(train, null);

                                                                    // enter ML pipeline for the given algorithm
                                                                    // serialize amd save the resulting model
                                                                    saasyml.train();

                                                                    // the path to the model file will be stored in the database
                                                                    modelMetadata.put(Constants.KEY_FILEPATH,
                                                                            saasyml.getModelPathSerialized());

                                                                } catch (Exception e) {
                                                                    // the error message will be stored to the database
                                                                    modelMetadata.put(Constants.KEY_ERROR,
                                                                            e.getMessage());
                                                                }

                                                                // save model file path or error message in the models metadata table
                                                                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE,
                                                                        modelMetadata);
                                                            });
                                                });
                                    } else {
                                        // the error message
                                        String errorMsg = "No training dataset input was found to train regressor model.";

                                        // log error message
                                        LOGGER.log(Level.SEVERE, errorMsg);

                                        // save error message in the models metadata table
                                        modelMetadata.put(Constants.KEY_ERROR, errorMsg);
                                        vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                                    }
                                });
                    } else {
                        // the error message
                        String errorMsg = "Missing expected target values to train regressor model.";

                        // log error message
                        LOGGER.log(Level.SEVERE, errorMsg);

                        // save error message in the models metadata table
                        modelMetadata.put(Constants.KEY_ERROR, errorMsg);
                        vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
                    }

                    LOGGER.log(Level.INFO,
                            "Trained model will be stored in the filesystem and referenced from the database once training is complete.");
                });

            } catch (Exception e) {
                // the error message
                String errorMsg = "Failed to get training data.";

                // log
                LOGGER.log(Level.SEVERE, errorMsg, e);

                // save error message in the models metadata table
                modelMetadata.put(Constants.KEY_ERROR, errorMsg + ": " + e.getMessage());
                vertx.eventBus().send(Constants.ADDRESS_MODELS_SAVE, modelMetadata);
            }

            // response
            msg.reply("Training the model(s) has been triggered. Query the " + Constants.ENDPOINT_MODELS
                    + " endpoint for training status.");

        });

    }

    /**
     * Function that add the expected labels from Json Expected label to the SortedMap
     * 
     * @param trainingDatasetMap sorted map by timestamp with the training data
     * @param labelsJsonArray Json with the expected labels of the training data
     * 
     * @return sorted map by timestamp with the train data
     */
    private SortedMap<Integer, JsonObject> trainDataGetExpectedLabes(SortedMap<Integer, JsonObject> trainingDatasetMap,
            JsonArray labelsJsonArray) {

        labelsJsonArray.forEach(e -> {

            JsonObject labelRow = (JsonObject) e;
            
            int labelTimestamp = labelRow.getInteger(Constants.KEY_TIMESTAMP).intValue();

            if (trainingDatasetMap.containsKey(labelTimestamp)) {
                int label = labelRow.getInteger(Constants.KEY_LABEL).intValue();
                trainingDatasetMap.get(labelTimestamp).put(Constants.KEY_LABEL, label);
            }
        });

        return trainingDatasetMap;
    }

    /**
     * Function that convert the train data from JSON to SortedMap
     * 
     * @param trainingDataJsonArray Json with the database information
     * @param dimensions total number of columns in the database
     * 
     * @return a sorted map by timestamp with the training data
     */
    private SortedMap<Integer, JsonObject> trainDataToSortedMap(JsonArray trainingDataJsonArray, int dimensions) {
        
        SortedMap<Integer, JsonObject> trainingDatasetMap = new TreeMap<Integer, JsonObject>();

        // the training input data point array
        JsonArray trainingDatapointJsonArray = new JsonArray();

        // collect all data points into the training dataset map
        for (int trainingDatasetIndex = 0; trainingDatasetIndex < trainingDataJsonArray.size(); trainingDatasetIndex++) {

            // fetch a parameter value from the training data point
            JsonObject trainingDataRow = trainingDataJsonArray.getJsonObject(trainingDatasetIndex);

            // include it in the training data point array
            trainingDatapointJsonArray.add(trainingDataRow.getString(Constants.KEY_VALUE));

            // keep adding data to the data point until all parameter values have been added for the given training input dimension
            if ((trainingDatasetIndex + 1) % dimensions == 0) {

                // get the timestamp for the current training dataset input
                int trainingDatasetTimestamp = trainingDataRow.getInteger(Constants.KEY_TIMESTAMP).intValue();

                JsonObject trainingDatasetJsonObject = new JsonObject();
                trainingDatasetJsonObject.put(Constants.KEY_DATA_POINT, trainingDatapointJsonArray);
                trainingDatasetMap.put(trainingDatasetTimestamp, trainingDatasetJsonObject);

                // reset the training data point json array
                trainingDatapointJsonArray = new JsonArray();
            }
        }
        
        return trainingDatasetMap;
    }

}
