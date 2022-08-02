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
            int classification = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.classifier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.LABEL_ALGORITHM); 
            boolean thread = PropertiesManager.getInstance().getThread();
            if (payload.containsKey(Constants.LABEL_THREAD) && payload.getBoolean(Constants.LABEL_THREAD) != null) {
                thread = payload.getBoolean(Constants.LABEL_THREAD);
            }
            boolean serialize = PropertiesManager.getInstance().getSerialize();
            /* Does not make sense because we want always to store the model
            if (payload.containsKey("serialize") && payload.getBoolean("serialize") != null) {
                serialize = payload.getBoolean("serialize");
            }*/
            
            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model
            saasyml.build();
            
            // 1. Train model using the expId and datasetId to fetch traning data that was stored from AggregationWrite.

            // 1.1. get data using expId and datasetId
            try{

                // build Json payload object with just expId and datasetId
                JsonObject selectPayload = new JsonObject();
                selectPayload.put(Constants.LABEL_EXPID, expId);
                selectPayload.put(Constants.LABEL_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_COUNT_COLUMNS, selectPayload, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists, we get it
                    if (response.containsKey(Constants.LABEL_COUNT)) {
                        selectPayload.put("total_cols", response.getInteger(Constants.LABEL_COUNT).intValue());
                    }
                
                    // select all the data from the dataset
                    vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_SELECT, selectPayload, selectReply -> {

                        // get the response from the select
                        JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                        // the total number of columns
                        int total_columns = selectPayload.getInteger("total_cols");

                        // if the response_select object contains the data, we can continue
                        if(selectResponse.containsKey(Constants.LABEL_DATA)) {
        
                            // 1.2. Prepare data
                            
                            // get the data with the result of the select
                            final JsonArray data = selectResponse.getJsonArray(Constants.LABEL_DATA);

                            // create variables for the classification
                            double[] tempTrainData = new double[total_columns]; // TRAIN
                            ClassificationDataSet train = new ClassificationDataSet(total_columns,
                                    new CategoricalData[0], new CategoricalData(classification)); // TRAIN

                            // variable to control the number of columns
                            int colCount = 0;

                            // iterate throughout all the data
                            for (int pos = 0; pos < data.size(); pos++) {

                                // get the Json Object and store the value
                                JsonObject object = data.getJsonObject(pos);
                                tempTrainData[colCount++] = Double.parseDouble(object.getString(Constants.LABEL_VALUE)); // TRAIN

                                // if colcount is equal to total columns, we add a new row
                                if (colCount == total_columns) {

                                     // TRAIN
                                    // we add a data point to our train dataset 
                                    // with a random value of the class Y
                                    train.addDataPoint(new DenseVector(tempTrainData), new int[0], rand.nextInt(classification));
                                    
                                    // we restart the count of cols to zero and the temporal train data
                                    colCount = 0;
                                    // TRAIN
                                    tempTrainData = new double[total_columns]; 
                                }
                            }

                            LOGGER.log(Level.INFO, "First row of the generated train data: " + train.getDataPoint(0).getNumericalValues().toString());

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
                });

            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to get training data.", e);

                // response: error
                msg.reply("Failed to get training data.");
            }
        });

        // train outlier
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_TRAINING_OUTLIER, msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "training.outlier: triggered by the following POST request: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final String algorithm = payload.getString("algorithm");

            msg.reply(String.format("training: outlier %s %s", algorithm));
            
        });

        // train clustering. 
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_TRAINING_CLUSTERING, msg -> {

            // variables to generate the class randomly
            Random rand = new Random();
            int classification = 2;

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started training.clustering");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final String algorithm = payload.getString(Constants.LABEL_ALGORITHM); 
            boolean thread = PropertiesManager.getInstance().getThread();
            if (payload.containsKey(Constants.LABEL_THREAD) && payload.getBoolean(Constants.LABEL_THREAD) != null) {
                thread = payload.getBoolean(Constants.LABEL_THREAD);
            }
            boolean serialize = PropertiesManager.getInstance().getSerialize();

            // create the pipeline
            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);

            // build the model
            saasyml.build(new Integer[] {classification});
            
            // Train the model 
            try{

                // build Json payload object with just expId and datasetId
                JsonObject selectPayload = new JsonObject();
                selectPayload.put(Constants.LABEL_EXPID, expId);
                selectPayload.put(Constants.LABEL_DATASETID, datasetId);

                // get the total number of columns 
                vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_COUNT_COLUMNS, selectPayload, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists, we get it
                    if (response.containsKey("count")) {
                        selectPayload.put("total_cols", response.getInteger("count").intValue());
                    }
                
                    // select all the data from the dataset
                    vertx.eventBus().request(Constants.LABEL_CONSUMER_DATA_SELECT, selectPayload, selectReply -> {

                        // get the response from the select
                        JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                        // the total number of columns
                        int total_columns = selectPayload.getInteger("total_cols");

                        // if the response_select object contains the data, we can continue
                        if(selectResponse.containsKey(Constants.LABEL_DATA)) {
        
                            // 1.2. Prepare data
                            
                            // get the data with the result of the select
                            final JsonArray data = selectResponse.getJsonArray(Constants.LABEL_DATA);

                            // create variables for the classification
                            double[] tempTrainData = new double[total_columns]; // TRAIN
                            CategoricalData[] catDataInfo = new CategoricalData[] { new CategoricalData(classification)} ; // TRAIN
        
                            List<DataPoint> dataPoints = new ArrayList<DataPoint>();
                            Normal noiseSource = new Normal(0, 0.05);

                            // variable to control the number of columns
                            int colCount = 0;

                            // iterate throughout all the data
                            for (int pos = 0; pos < data.size(); pos++) {

                                // get the Json Object and store the value
                                JsonObject object = data.getJsonObject(pos);
                                tempTrainData[colCount++] = Double.parseDouble(object.getString(Constants.LABEL_VALUE)); // TRAIN

                                // if colcount is equal to total columns, we add a new row
                                if (colCount == total_columns) {

                                    // TRAIN
                                    // we add a data point to our train dataset 
                                    // with a random value of the class Y
                                    dataPoints.add(new DataPoint(new DenseVector(tempTrainData), new int[] { rand.nextInt(classification) }, catDataInfo));

                                    // we restart the count of cols to zero and the temporal train data
                                    colCount = 0;
                                    // TRAIN
                                    tempTrainData = new double[total_columns];
                                }
                            }
                            
                            SimpleDataSet train = new SimpleDataSet(dataPoints);

                            LOGGER.log(Level.INFO, "First row of the generated train data: " + train.getDataPoint(0).getNumericalValues().toString());

                            // upload the train dataset
                            saasyml.setDataSet(train, null);
        
                            LOGGER.log(Level.INFO, "Executed method setDataSet");
                            
                            // 1.3. Here we enter ML pipeline for the given algorithm
                            // 2. Serialize and save the resulting model.
                            // Make sure it is uniquely identifiable with expId and datasetId, maybe as part
                            // of the toGround folder file system:
                            try{
                                saasyml.train();
                            } catch (Exception e) {
                                // log
                                LOGGER.log(Level.SEVERE, "Failed to get training data.", e);
                
                                // response: error
                                msg.reply("Failed to get training data.");
                            }

                            LOGGER.log(Level.INFO, "Executed method train");

                             // 3. Return a message with a path to the serialized model
                             JsonObject selectReponseReply = new JsonObject();
                             selectReponseReply.put(Constants.LABEL_TYPE, "cluster");
                             selectReponseReply.put(Constants.LABEL_ALGORITHM, algorithm);
                             selectReponseReply.put(Constants.LABEL_MODEL_PATH, saasyml.getModelPathSerialized());
                             msg.reply(selectReponseReply);
                
                        }
                    });
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
