package esa.mo.nmf.apps.verticles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class InferenceVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(InferenceVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // inference classifier
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_INFERENCE_CLASSIFIER, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started inference.classifier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();

            // get the data 

            // get the models 

            // for each model 

                // create the pipeline with the minimun 

                // do the inference
                
                // store the inference in the same json
                
            // retrieve the response

            
            
            
            
            
            
            
            
            final String algorithm = payload.getString("algorithm"); 

            // get data from the configuration
            boolean thread = PropertiesManager.getInstance().getThread();
            if (payload.containsKey("thread") && payload.getBoolean("thread") != null) {
                thread = payload.getBoolean("thread");
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
                selectPayload.put("expId", expId);
                selectPayload.put("datasetId", datasetId);

                // get the total number of columns 
                vertx.eventBus().request("saasyml.training.data.count_columns", selectPayload, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists, we get it
                    if (response.containsKey("count")) {
                        selectPayload.put("total_cols", response.getInteger("count").intValue());
                    }
                
                    // select all the data from the dataset
                    vertx.eventBus().request("saasyml.training.data.select", selectPayload, selectReply -> {

                        // get the response from the select
                        JsonObject selectResponse = (JsonObject) (selectReply.result().body());

                        // the total number of columns
                        int total_columns = selectPayload.getInteger("total_cols");
                        // LOGGER.log(Level.INFO, "total columns : " + total_columns);

                        // if the response_select object contains the data, we can continue
                        if(selectResponse.containsKey("data")) {
        
                            // 1.2. Prepare data
                            
                            // get the data with the result of the select
                            final JsonArray data = selectResponse.getJsonArray("data");

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
                                tempTrainData[colCount++] = Double.parseDouble(object.getString("value")); // TRAIN

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

                            // 3. Return a message with unique identifiers of the serizalized model (or
                            // maybe just a path to it?)
                            // store path to model in response of reply
                            JsonObject selectReponseReply = new JsonObject();
                            selectReponseReply.put("model_path", saasyml.getModelPathSerialized());
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

            // msg.reply(String.format("training: classifier %s %s", group, algorithm));
        });

        // inference outlier
        vertx.eventBus().consumer("saasyml.inference.outlier", msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "training.outlier: triggered by the following POST request: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final String algorithm = payload.getString("algorithm");

            msg.reply(String.format("training: outlier %s %s", algorithm));
            
        });

        // inference clustering
        vertx.eventBus().consumer("saasyml.inference.clustering", msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "inference.outlier: triggered by the following POST request: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final String algorithm = payload.getString("algorithm");

            msg.reply(String.format("inference: outlier %s %s", algorithm));

        });

    }
}
