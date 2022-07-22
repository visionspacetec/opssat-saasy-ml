package esa.mo.nmf.apps.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import esa.mo.nmf.apps.ApplicationManager;
import esa.mo.nmf.apps.Constants;

public class FetchTrainingDataVerticle extends AbstractVerticle {
  
    // logger
    private static final Logger LOGGER = Logger.getLogger(FetchTrainingDataVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // log
        LOGGER.log(Level.INFO, "Starting a Verticle instance with deployment id " + this.deploymentID() + ".");

        // subscribe to a training data feed
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_DATA_SUBSCRIBE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject)(msg.body());
            LOGGER.log(Level.INFO, "The POST request payload: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final double interval = payload.getInteger("interval").doubleValue();
            
            // iterations payload parameter is optional, set it to -1 if it wasn't provided
            final int iterations = payload.containsKey("iterations") ? payload.getInteger("iterations").intValue() : -1;
            
            // create list of training data param names from JsonArray
            final JsonArray trainingDataParamNameJsonArray = payload.getJsonArray("params");

            List<String> paramNameList = new ArrayList<String>();
            for(int i = 0; i < trainingDataParamNameJsonArray.size(); i++){
                paramNameList.add(trainingDataParamNameJsonArray.getString(i));
            }

            // build Json payload object with just expId and datasetId
            // this will be used for the training data count request
            JsonObject countPayload = new JsonObject();
            countPayload.put(Constants.LABEL_EXPID, expId);
            countPayload.put(Constants.LABEL_DATASETID, datasetId);

            try {

                // keep track of param names
                // we need this because the response object received in the onReceivedData listener does not reference the parameter names
                ApplicationManager.getInstance().addParamNames(expId, datasetId, paramNameList);

                // create aggregation handler and subscribe the parameter feed
                ApplicationManager.getInstance().createAggregationHandler(expId, datasetId, interval, paramNameList, true);

                // check periodically when to stop fetching datam if "interations" is set and > 0
                if(iterations > 0)
                {
                    // register periodic timer
                    vertx.setPeriodic(500, id -> {
                        vertx.eventBus().request("saasyml.training.data.count", countPayload, reply -> {
                            JsonObject response = (JsonObject)(reply.result().body());

                            // response object is somehow does not contain the expected parameter (impossible?)
                            // stop timer if this happens
                            if(!response.containsKey("count")){    
                                vertx.cancelTimer(id);

                            } else {
                                // get training data row count
                                // fixme: dividing by paramNameList.size() will break if the number of params change during from one data fetching session to another for
                                // the same expId and datasetId
                                int counter = response.getInteger("count").intValue() / paramNameList.size();

                                // the counter is set to -1 if there was an error while attempting to query the database
                                // stop timer if  this happens
                                if(counter < 0) {
                                    vertx.cancelTimer(id);
                                }
                                else if(counter >= iterations) {

                                    // target number of training dataset has been achieved
                                    // unsubscribe from the training data feed
                                    try {
                                        // disable parameter feed
                                        ApplicationManager.getInstance().enableSupervisorParametersSubscription(expId, datasetId, false);
            
                                        // remove the aggregation handler from the map
                                        ApplicationManager.getInstance().removeAggregationHandler(expId, datasetId);

                                        // auto-trigger training if the payload is configured to do so
                                        if(payload.containsKey("training")) {

                                            // the training parameters can be for more than one algorithm
                                            final JsonArray trainings = payload.getJsonArray("training");

                                            // trigger training for each request
                                            for(int i = 0; i < trainings.size(); i++) {
                                                final JsonObject t = trainings.getJsonObject(i);

                                                // fetch training algorithm selection
                                                String type = t.getString("type");

                                                // build JSON payload object 
                                                JsonObject trainPayload = new JsonObject();
                                                trainPayload.put("expId", expId);
                                                trainPayload.put("datasetId", datasetId);
                                                trainPayload.put("group", t.getString("group"));
                                                trainPayload.put("algorithm", t.getString("algorithm"));
                                                trainPayload.put("thread", t.getBoolean("thread"));

                                                // trigger training
                                                // vertx.eventBus().send("saasyml.training." + type, trainPayload);
                                                vertx.eventBus().request("saasyml.training." + type, trainPayload,
                                                        trainReply -> {
                                                    
                                                            JsonObject trainResponse = (JsonObject) (trainReply.result().body());
                                                            if (trainResponse.containsKey("model_path")) {
                                                                LOGGER.log(Level.INFO, "model path: " + trainResponse.getString("model_path") + ".");
                                                            }
  
                                                });
                                            }
                                        }

                                        // can now stop this periodic check
                                        vertx.cancelTimer(id);

                                    } catch(Exception e) {
                                        LOGGER.log(Level.SEVERE, "Failed to unsubscribe from training data feed.", e);
                                    }
                                }
                            }
                        });    
                    });
                }

                // response: success
                msg.reply("Successfully subscribed to training data feed.");

            } catch(Exception e){
                // log
                LOGGER.log(Level.SEVERE, "Failed to start Aggregation Handler.", e);

                // response: error
                msg.reply("Failed to subscribe to training data feed.");
            } 
        });

        // unsubscribe to a training data feed
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_DATA_UNSUBSCRIBE, msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject)(msg.body());
            LOGGER.log(Level.INFO, "The POST request payload: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();

            try {
                // disable parameter feed
                ApplicationManager.getInstance().enableSupervisorParametersSubscription(expId, datasetId, false);

                // remove from map
                ApplicationManager.getInstance().removeAggregationHandler(expId, datasetId);


                // response: success
                msg.reply("Successfully unsubscribed to training data feed.");

            } catch(Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to unsubscribe from training data feed.", e);

                // response: error
                msg.reply("Failed to unsubscribe from training data feed.");
            }
        });    
    }
}