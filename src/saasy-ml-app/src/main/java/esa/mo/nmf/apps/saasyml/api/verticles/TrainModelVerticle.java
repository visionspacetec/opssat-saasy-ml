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

// TODO:
//      - this class does not scale well
//      - consider refactoring using a factory pattern
//      - dedicated class for each training algorithm
public class TrainModelVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(TrainModelVerticle.class.getName());

    @Override
    public void start() throws Exception {

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
            /* Does not make sense because we want always to store the model
            if (payload.containsKey("serialize") && payload.getBoolean("serialize") != null) {
                serialize = payload.getBoolean("serialize");
            }*/

            // the train model will be serialized and saved as a file in the filesystem
            // a reference to the file as well as some metadata will be stored in the database
            // collect all metadata in a Json object
            JsonObject modelMetadata = new JsonObject();
            modelMetadata.put(Constants.KEY_EXPID, expId);
            modelMetadata.put(Constants.KEY_DATASETID, datasetId);
            modelMetadata.put(Constants.KEY_TYPE, "classifier");
            modelMetadata.put(Constants.KEY_ALGORITHM, algorithm);

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
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
                        vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payload, dimensionResponse -> {
                            JsonObject dimensionJson = (JsonObject) (dimensionResponse.result().body());
                        
                            // check if we actually have training input
                            if (dimensionJson.containsKey(Constants.KEY_COUNT) && dimensionJson.getInteger(Constants.KEY_COUNT) > 0) {

                                // the dimension count
                                int dimensions = dimensionJson.getInteger(Constants.KEY_COUNT).intValue();
                                
                                // select all the expected labels/classes
                                vertx.eventBus().request(Constants.ADDRESS_LABELS_SELECT, payload, labelsResponse -> {
                                    JsonObject labelsResponseJson = (JsonObject) (labelsResponse.result().body());
                                    final JsonArray labelsJsonArray = labelsResponseJson.getJsonArray(Constants.KEY_DATA);

                                    // select all the training data
                                    vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payload, trainingDataResponse -> {
                                        JsonObject trainingDataResponseJson = (JsonObject) (trainingDataResponse.result().body());
                                        final JsonArray trainingDataJsonArray = trainingDataResponseJson.getJsonArray(Constants.KEY_DATA);

                                        // the JSAT training input dataset object
                                        ClassificationDataSet train = new ClassificationDataSet(
                                            dimensions, 
                                            new CategoricalData[0], new CategoricalData(distinctLabelsJsonArray.size())
                                        );   

                                        // the training input data point array
                                        JsonArray trainingDatapointJsonArray = new JsonArray();

                                        // the map that will contain all the datapoints and their expected labels
                                        SortedMap<Integer, JsonObject> trainingDatasetMap = new TreeMap<Integer, JsonObject>();

                                        // collect all data points into the training dataset map
                                        for (int trainingDatasetIndex = 0; trainingDatasetIndex < trainingDataJsonArray.size(); trainingDatasetIndex++) {

                                            // fetch a parameter value from the training data point
                                            JsonObject trainingDataRow = trainingDataJsonArray.getJsonObject(trainingDatasetIndex);

                                            // include it in the training data point array
                                            trainingDatapointJsonArray.add(trainingDataRow.getString(Constants.KEY_VALUE));

                                            // keep adding data to the data point until all parameter values have been added for the given training input dimension
                                            if ((trainingDatasetIndex+1) % dimensions == 0) {

                                                // get the timestamp for the current training dataset input
                                                int trainingDatasetTimestamp = trainingDataRow.getInteger(Constants.KEY_TIMESTAMP).intValue();

                                                JsonObject trainingDatasetJsonObject = new JsonObject();
                                                trainingDatasetJsonObject.put(Constants.KEY_DATA_POINT, trainingDatapointJsonArray);
                                                trainingDatasetMap.put(trainingDatasetTimestamp, trainingDatasetJsonObject);

                                                // reset the traunung data point json array
                                                trainingDatapointJsonArray = new JsonArray();
                                            }
                                        }

                                        // collect all the expected labels into the training dataset map
                                        for (int datasetLabelIndex = 0; datasetLabelIndex < labelsJsonArray.size(); datasetLabelIndex++) {
                                            JsonObject labelRow = labelsJsonArray.getJsonObject(datasetLabelIndex);
                                            int labelTimestamp = labelRow.getInteger(Constants.KEY_TIMESTAMP).intValue();

                                            if(trainingDatasetMap.containsKey(labelTimestamp)){
                                                int label = labelRow.getInteger(Constants.KEY_LABEL).intValue();
                                                trainingDatasetMap.get(labelTimestamp).put(Constants.KEY_LABEL, label);
                                            }
                                        }

                                        // create the data points for training
                                        for (Map.Entry<Integer, JsonObject> entry : trainingDatasetMap.entrySet()) {

                                            if (entry.getValue().containsKey(Constants.KEY_LABEL)) {
                                                
                                                // build the training data point array
                                                // FIXME: training datapoints values are not always of type double
                                                // but maybe it's ok to always process as double?
                                                // see esa.mo.helpertools.helpers.HelperAttributes.attribute2double(Attribute in)
                                                double[] trainingDatapointArray = new double[dimensions];

                                                for(int d = 0; d < dimensions; d++){
                                                    // FIXME: training datapoints values are not always of type double
                                                    // but maybe it's ok to always process as double?
                                                    // see esa.mo.helpertools.helpers.HelperAttributes.attribute2double(Attribute in
                                                    trainingDatapointArray[d] = Double.valueOf(entry.getValue().getJsonArray(Constants.KEY_DATA_POINT).getString(d));
                                                }

                                                // add the data point array as a dense vector and set the expected label
                                                train.addDataPoint(
                                                    new DenseVector(trainingDatapointArray),
                                                    new int[0],
                                                    entry.getValue().getInteger(Constants.KEY_LABEL).intValue());
                                            }
                                        }

                                        try {
                                            // upload the training dataset
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

                    LOGGER.log(Level.INFO, "Trained model will be stored in the filesystem and referenced from the database once training is complete.");

                    // response
                    msg.reply("Successfully trained model. Request the endpoint models to see the final results.");
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
            boolean thread = (payload.containsKey(Constants.KEY_THREAD) && payload.getBoolean(Constants.KEY_THREAD) != null)? payload.getBoolean(Constants.KEY_THREAD): PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
            saasyml.build(new Integer[] {totalClasses});
            
            // Train the model 
            try{

                // build Json payload object with just expId and datasetId
                JsonObject payloadSelect = new JsonObject();
                payloadSelect.put(Constants.KEY_EXPID, expId);
                payloadSelect.put(Constants.KEY_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payloadSelect, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists and it is different from zero, continue
                    if (response.containsKey(Constants.KEY_COUNT)
                            && response.getInteger(Constants.KEY_COUNT) > 0) {
                        
                        payloadSelect.put(Constants.KEY_COUNT, response.getInteger(Constants.KEY_COUNT).intValue());
                    
                        // select all the data from the dataset
                        vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payloadSelect, selectReply -> {

                            // get the response from the select
                            JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                            // if the response_select object contains the data, we can continue
                            if(selectResponse.containsKey(Constants.KEY_DATA)) {

                                // 1.2. Prepare data

                                // the total number of columns
                                int total_columns = payloadSelect.getInteger(Constants.KEY_COUNT);
                                
                                // get the data with the result of the select
                                final JsonArray data = selectResponse.getJsonArray(Constants.KEY_DATA);

                                // create variables for the k
                                double[] tempTrainData = new double[total_columns]; // TRAIN
                                CategoricalData[] catDataInfo = new CategoricalData[] { new CategoricalData(totalClasses)} ; 
            
                                List<DataPoint> dataPoints = new ArrayList<DataPoint>();
                                Normal noiseSource = new Normal();

                                // variable to control the number of columns
                                int colCount = 0;

                                // iterate throughout all the data
                                for (int pos = 0; pos < data.size(); pos++) {

                                    // get the Json Object and store the value
                                    JsonObject object = data.getJsonObject(pos);
                                    tempTrainData[colCount++] = colCount + Double.valueOf(object.getString(Constants.KEY_VALUE));;

                                    // if colcount is equal to total columns, we add a new row
                                    if (colCount == total_columns) {

                                        // we add a data point to our train dataset 
                                        // with a random value of the class Y
                                        dataPoints.add(new DataPoint(new DenseVector(tempTrainData), new int[] { rand.nextInt(totalClasses) }, catDataInfo));

                                        // we restart the count of cols to zero and the temporal train data
                                        colCount = 0;
                                        tempTrainData = new double[total_columns];
                                    }
                                }
                                
                                LOGGER.log(Level.INFO, "Data points: " + dataPoints.toString());
                                
                                SimpleDataSet train = new SimpleDataSet(dataPoints);

                                // upload the train dataset
                                saasyml.setDataSet(train, null);
                                
                                // 1.3. Here we enter ML pipeline for the given algorithm
                                // 2. Serialize and save the resulting model.
                                // Make sure it is uniquely identifiable with expId and datasetId, maybe as part
                                // of the toGround folder file system:
                                LOGGER.log(Level.INFO, "Executed method train");
                                saasyml.train();

                                // 3. Return a message with a path to the serialized model
                                JsonObject selectReponseReply = new JsonObject();
                                selectReponseReply.put(Constants.KEY_TYPE, "outlier");
                                selectReponseReply.put(Constants.KEY_ALGORITHM, algorithm);
                                selectReponseReply.put(Constants.KEY_MODEL_PATH, saasyml.getModelPathSerialized());
                                msg.reply(selectReponseReply);
                                
                            }
                        });
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to get the total number of rows.");

                        // response: error
                        msg.reply("Failed to get the total number of rows.");
                    }
                });

            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to get training data.", e);

                // response: error
                msg.reply("Failed to get training data.");
            }

        });

        // train clustering. 
        vertx.eventBus().consumer(Constants.ADDRESS_TRAINING_CLUSTER, msg -> {

            // variables to generate the class randomly
            Random rand = new Random();
            int totalClasses = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.cluster");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.KEY_ALGORITHM);
            boolean thread = (payload.containsKey(Constants.KEY_THREAD)
                    && payload.getBoolean(Constants.KEY_THREAD) != null) ? payload.getBoolean(Constants.KEY_THREAD)
                            : PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
            saasyml.build(new Integer[] { totalClasses });

            // Train the model 
            try {

                // build Json payload object with just expId and datasetId
                JsonObject payloadSelect = new JsonObject();
                payloadSelect.put(Constants.KEY_EXPID, expId);
                payloadSelect.put(Constants.KEY_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, payloadSelect, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists and it is different from zero, continue
                    if (response.containsKey(Constants.KEY_COUNT)
                            && response.getInteger(Constants.KEY_COUNT) > 0) {

                        payloadSelect.put(Constants.KEY_COUNT, response.getInteger(Constants.KEY_COUNT).intValue());

                        // select all the data from the dataset
                        vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payloadSelect, selectReply -> {

                            // get the response from the select
                            JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                            // if the response_select object contains the data, we can continue
                            if (selectResponse.containsKey(Constants.KEY_DATA)) {

                                LOGGER.log(Level.INFO, "Prepare data ");

                                // 1.2. Prepare data

                                // the total number of columns
                                int total_columns = payloadSelect.getInteger(Constants.KEY_COUNT);

                                // get the data with the result of the select
                                final JsonArray data = selectResponse.getJsonArray(Constants.KEY_DATA);

                                // create variables for the k
                                double[] tempTrainData = new double[total_columns]; // TRAIN
                                CategoricalData[] catDataInfo = new CategoricalData[] {
                                        new CategoricalData(totalClasses) };

                                List<DataPoint> dataPoints = new ArrayList<DataPoint>();
                                Normal noiseSource = new Normal(0, 0.05);

                                // variable to control the number of columns
                                int colCount = 0;

                                // iterate throughout all the data
                                for (int pos = 0; pos < data.size(); pos++) {

                                    // get the Json Object and store the value
                                    JsonObject object = data.getJsonObject(pos);
                                    tempTrainData[colCount++] = colCount
                                            + Double.valueOf(object.getString(Constants.KEY_VALUE));
                                    ;

                                    // if colcount is equal to total columns, we add a new row
                                    if (colCount == total_columns) {

                                        // we add a data point to our train dataset 
                                        // with a random value of the class Y
                                        dataPoints.add(new DataPoint(new DenseVector(tempTrainData),
                                                new int[] { rand.nextInt(totalClasses) }, catDataInfo));

                                        // we restart the count of cols to zero and the temporal train data
                                        colCount = 0;
                                        tempTrainData = new double[total_columns];
                                    }
                                }

                                LOGGER.log(Level.INFO, "Data points: " + dataPoints.toString());

                                SimpleDataSet train = new SimpleDataSet(dataPoints);

                                // upload the train dataset
                                saasyml.setDataSet(train, null);

                                // 1.3. Here we enter ML pipeline for the given algorithm
                                // 2. Serialize and save the resulting model.
                                // Make sure it is uniquely identifiable with expId and datasetId, maybe as part
                                // of the toGround folder file system:
                                LOGGER.log(Level.INFO, "Executed method train");
                                saasyml.train();

                                // 3. Return a message with a path to the serialized model
                                JsonObject selectReponseReply = new JsonObject();
                                selectReponseReply.put(Constants.KEY_TYPE, "cluster");
                                selectReponseReply.put(Constants.KEY_ALGORITHM, algorithm);
                                selectReponseReply.put(Constants.KEY_MODEL_PATH, saasyml.getModelPathSerialized());
                                msg.reply(selectReponseReply);

                            }
                        });
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to get the total number of rows.");

                        // response: error
                        msg.reply("Failed to get the total number of rows.");
                    }
                });

            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to get training data.", e);

                // response: error
                msg.reply("Failed to get training data.");
            }
        });

    }

}
