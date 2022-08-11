package esa.mo.nmf.apps.verticles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ldap.Rdn;

import esa.mo.nmf.apps.Constants;
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

public class TrainModelVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(TrainModelVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // train classifier
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_TRAINING_CLASSIFIER, msg -> {

            // variables to generate the class randomly
            Random rand = new Random();
            int k = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.classifier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.LABEL_ALGORITHM);
            boolean thread = (payload.containsKey(Constants.LABEL_THREAD)
                    && payload.getBoolean(Constants.LABEL_THREAD) != null) ? payload.getBoolean(Constants.LABEL_THREAD)
                            : PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();
            /* Does not make sense because we want always to store the model
            if (payload.containsKey("serialize") && payload.getBoolean("serialize") != null) {
                serialize = payload.getBoolean("serialize");
            }*/

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
            saasyml.build();

            // Train the model 
            try {

                // build Json payload object with just expId and datasetId
                JsonObject payloadSelect = new JsonObject();
                payloadSelect.put(Constants.LABEL_EXPID, expId);
                payloadSelect.put(Constants.LABEL_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_COUNT_COLUMNS, payloadSelect, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists and it is different from zero, continue
                    if (response.containsKey(Constants.LABEL_COUNT) && response.getInteger(Constants.LABEL_COUNT) > 0) {

                        payloadSelect.put(Constants.LABEL_COUNT, response.getInteger(Constants.LABEL_COUNT).intValue());

                        // select all the data from the dataset
                        vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_SELECT, payloadSelect, selectReply -> {

                            // get the response from the select
                            JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                            // if the response_select object contains the data, we can continue
                            if (selectResponse.containsKey(Constants.LABEL_DATA)) {

                                // 1.2. Prepare data

                                // the total number of columns
                                int total_columns = payloadSelect.getInteger(Constants.LABEL_COUNT);

                                // get the data with the result of the select
                                final JsonArray data = selectResponse.getJsonArray(Constants.LABEL_DATA);

                                // create variables for the k
                                double[] tempTrainData = new double[total_columns]; // TRAIN
                                ClassificationDataSet train = new ClassificationDataSet(total_columns,
                                        new CategoricalData[0], new CategoricalData(k)); // TRAIN

                                // variable to control the number of columns
                                int colCount = 0;

                                // variable to keep track of the label class
                                int labelClass = -1;

                                // iterate throughout all the data
                                for (int pos = 0; pos < data.size(); pos++) {

                                    // get the Json Object and store the value
                                    JsonObject object = data.getJsonObject(pos);
                                    if (object.getString(Constants.LABEL_PARAM_NAME).trim().toLowerCase()
                                            .equals(Constants.LABEL_LABEL)) {
                                        labelClass = Integer.valueOf(object.getString(Constants.LABEL_VALUE));
                                    } else {
                                        tempTrainData[colCount++] = Double.valueOf(object.getString(Constants.LABEL_VALUE)); // TRAIN
                                    }

                                    // if colcount is equal to total columns, we add a new row
                                    if (colCount == total_columns) {

                                        if (labelClass == -1) {
                                            labelClass = rand.nextInt(k);
                                        }

                                        // TRAIN
                                        // we add a data point to our train dataset 
                                        // with a random value of the class Y
                                        train.addDataPoint(new DenseVector(tempTrainData), new int[0], labelClass);

                                        // we restart the count of cols to zero and the temporal train data
                                        colCount = 0;
                                        // TRAIN
                                        tempTrainData = new double[total_columns];
                                        labelClass = -1;
                                    }
                                }

                                LOGGER.log(Level.INFO, "First row of the generated train data: "
                                        + train.getDataPoint(0).getNumericalValues().toString());

                                // upload the train dataset
                                saasyml.setDataSet(train, null);

                                // 1.3. Here we enter ML pipeline for the given algorithm
                                // 2. Serialize and save the resulting model.
                                // Make sure it is uniquely identifiable with expId and datasetId, maybe as part
                                // of the toGround folder file system:
                                saasyml.train();

                                // 3. Return a message with a path to the serialized model
                                JsonObject selectReponseReply = new JsonObject();
                                selectReponseReply.put(Constants.LABEL_TYPE, "classifier");
                                selectReponseReply.put(Constants.LABEL_ALGORITHM, algorithm);
                                selectReponseReply.put(Constants.LABEL_MODEL_PATH, saasyml.getModelPathSerialized());
                                msg.reply(selectReponseReply);

                            }
                        });
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to get the total number of columns");
                    }

                });

            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to get training data.", e);

                // response: error
                msg.reply("Failed to get training data.");
            }

            LOGGER.log(Level.INFO, "Finished training.classifier");
        });

        // train outlier
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_TRAINING_OUTLIER, msg -> {
            // variables to generate the class randomly
            Random rand = new Random();
            int totalClasses = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.outlier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.LABEL_ALGORITHM); 
            boolean thread = (payload.containsKey(Constants.LABEL_THREAD) && payload.getBoolean(Constants.LABEL_THREAD) != null)? payload.getBoolean(Constants.LABEL_THREAD): PropertiesManager.getInstance().getThread();
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model with parameters
            saasyml.build(new Integer[] {totalClasses});
            
            // Train the model 
            try{

                // build Json payload object with just expId and datasetId
                JsonObject payloadSelect = new JsonObject();
                payloadSelect.put(Constants.LABEL_EXPID, expId);
                payloadSelect.put(Constants.LABEL_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_COUNT_COLUMNS, payloadSelect, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists and it is different from zero, continue
                    if (response.containsKey(Constants.LABEL_COUNT)
                            && response.getInteger(Constants.LABEL_COUNT) > 0) {
                        
                        payloadSelect.put(Constants.LABEL_COUNT, response.getInteger(Constants.LABEL_COUNT).intValue());
                    
                        // select all the data from the dataset
                        vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_SELECT, payloadSelect, selectReply -> {

                            // get the response from the select
                            JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                            // if the response_select object contains the data, we can continue
                            if(selectResponse.containsKey(Constants.LABEL_DATA)) {

                                // 1.2. Prepare data

                                // the total number of columns
                                int total_columns = payloadSelect.getInteger(Constants.LABEL_COUNT);
                                
                                // get the data with the result of the select
                                final JsonArray data = selectResponse.getJsonArray(Constants.LABEL_DATA);

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
                                    tempTrainData[colCount++] = colCount + Double.valueOf(object.getString(Constants.LABEL_VALUE));;

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
                                selectReponseReply.put(Constants.LABEL_TYPE, "outlier");
                                selectReponseReply.put(Constants.LABEL_ALGORITHM, algorithm);
                                selectReponseReply.put(Constants.LABEL_MODEL_PATH, saasyml.getModelPathSerialized());
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
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_TRAINING_CLUSTER, msg -> {

            // variables to generate the class randomly
            Random rand = new Random();
            int totalClasses = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.cluster");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.LABEL_ALGORITHM);
            boolean thread = (payload.containsKey(Constants.LABEL_THREAD)
                    && payload.getBoolean(Constants.LABEL_THREAD) != null) ? payload.getBoolean(Constants.LABEL_THREAD)
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
                payloadSelect.put(Constants.LABEL_EXPID, expId);
                payloadSelect.put(Constants.LABEL_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_COUNT_COLUMNS, payloadSelect, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists and it is different from zero, continue
                    if (response.containsKey(Constants.LABEL_COUNT)
                            && response.getInteger(Constants.LABEL_COUNT) > 0) {

                        payloadSelect.put(Constants.LABEL_COUNT, response.getInteger(Constants.LABEL_COUNT).intValue());

                        // select all the data from the dataset
                        vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_SELECT, payloadSelect, selectReply -> {

                            // get the response from the select
                            JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                            // if the response_select object contains the data, we can continue
                            if (selectResponse.containsKey(Constants.LABEL_DATA)) {

                                LOGGER.log(Level.INFO, "Prepare data ");

                                // 1.2. Prepare data

                                // the total number of columns
                                int total_columns = payloadSelect.getInteger(Constants.LABEL_COUNT);

                                // get the data with the result of the select
                                final JsonArray data = selectResponse.getJsonArray(Constants.LABEL_DATA);

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
                                            + Double.valueOf(object.getString(Constants.LABEL_VALUE));
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
                                selectReponseReply.put(Constants.LABEL_TYPE, "cluster");
                                selectReponseReply.put(Constants.LABEL_ALGORITHM, algorithm);
                                selectReponseReply.put(Constants.LABEL_MODEL_PATH, saasyml.getModelPathSerialized());
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
