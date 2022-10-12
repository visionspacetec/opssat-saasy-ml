package esa.mo.nmf.apps.saasyml.api.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import esa.mo.nmf.apps.ApplicationManager;
import esa.mo.nmf.apps.PropertiesManager;
import esa.mo.nmf.apps.saasyml.api.Constants;

// TODO:
// this class does not adhere to the Dependency Inversion Principle. 
// 
// - consider depend on abstractions
public class FetchTrainingDataVerticle extends AbstractVerticle {
  
    // logger
    private static final Logger LOGGER = Logger.getLogger(FetchTrainingDataVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // log
        LOGGER.log(Level.INFO, "Starting a Verticle instance with deployment id " + this.deploymentID() + ".");

        // subscribe to a training data feed
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_SUBSCRIBE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, String.format("The POST %s request payload: %s", Constants.ADDRESS_DATA_SUBSCRIBE, payload.toString()));

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final double interval = payload.getInteger(Constants.KEY_INTERVAL).doubleValue();

            // iterations payload parameter is optional, set it to -1 if it wasn't provided
            final int iterations = payload.containsKey(Constants.KEY_ITERATIONS)
                    ? payload.getInteger(Constants.KEY_ITERATIONS).intValue() : -1;

            // create list of training data param names from JsonArray
            List<String> paramNameList = createArrayFromJsonArray(payload.getJsonArray(Constants.KEY_PARAMS));

            LOGGER.log(Level.INFO, "[TRACE LOG] JSON payload parsed");

            // build Json payload object with just expId and datasetId
            // this will be used for the training data count request
            JsonObject payloadCount = new JsonObject();
            payloadCount.put(Constants.KEY_EXPID, expId);
            payloadCount.put(Constants.KEY_DATASETID, datasetId);

            try {

                LOGGER.log(Level.INFO, "[TRACE LOG] Before addParamNames");

                // keep track of param names
                // we need this because the response object received in the onReceivedData listener does not reference the parameter names
                ApplicationManager.getInstance().addParamNames(expId, datasetId, paramNameList);

                LOGGER.log(Level.INFO, "[TRACE LOG] After addParamNames");

                try {
                        
                    LOGGER.log(Level.INFO, "[TRACE LOG] Before createAggregationHandler");
                    
                    // create aggregation handler and subscribe the parameter feed
                    ApplicationManager.getInstance().createAggregationHandler(expId, datasetId, interval, paramNameList,
                            true);

                    LOGGER.log(Level.INFO, String.format("[TRACE LOG] Created aggregation handler"));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, String.format("[TRACE LOG] Exception creating aggregation handler", e));
                }

                // check periodically when to stop fetching data if "interations" is set and > 0
                if (iterations > 0) {

                    // set the periodic timer            
                    int periodicTimer = PropertiesManager.getInstance().getFetchTrainingDataVerticlePeriodicTimer();

                    
                    vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT, payloadCount, reply -> {
                        JsonObject response = (JsonObject) (reply.result().body());

                        if (response.containsKey(Constants.KEY_COUNT)) {
                            payloadCount.put(Constants.KEY_STARTED_COUNT, response.getInteger(Constants.KEY_COUNT).intValue());
                        }
                    });
                    
                    // register periodic timer
                    vertx.setPeriodic(periodicTimer, id -> {
                        vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT, payloadCount, reply -> {
                            JsonObject response = (JsonObject) (reply.result().body());

                            // response object is somehow does not contain the expected parameter (impossible?)
                            // stop timer if this happens
                            if (!response.containsKey(Constants.KEY_COUNT)) {
                                vertx.cancelTimer(id);

                            } else {
                                int startedCount = payloadCount.getInteger(Constants.KEY_STARTED_COUNT).intValue();
                                int count = response.getInteger(Constants.KEY_COUNT).intValue() - startedCount;

                                // get training data row count
                                // fixme: dividing by paramNameList.size() will break if the number of params change from one data fetching session to another for
                                // the same expId and datasetId
                                int counter = count / paramNameList.size();

                                // the counter is set to -1 if there was an error while attempting to query the database
                                // stop timer if  this happens
                                if (counter < 0) {
                                    vertx.cancelTimer(id);
                                } else if (counter >= iterations) {

                                    // target number of training dataset has been achieved
                                    // unsubscribe from the training data feed
                                    try {
                                        try {
                                            // disable parameter feed
                                            ApplicationManager.getInstance().enableSupervisorParametersSubscription(expId,
                                            datasetId, false);
                        
                                            LOGGER.log(Level.INFO, String.format("[TRACE LOG] Disabled parameter feed"));
                                        } catch (Exception e) {
                                            LOGGER.log(Level.SEVERE, String.format("[TRACE LOG] Exception  Disable parameter feed", e));
                                        }

                                        // remove the aggregation handler from the map
                                        ApplicationManager.getInstance().removeAggregationHandler(expId, datasetId);

                                        // auto-trigger training if the payload is configured to do so
                                        if (payload.containsKey("training")) {

                                            // the training parameters can be for more than one algorithm
                                            final JsonArray trainings = payload.getJsonArray("training");

                                            // trigger training for each request
                                            for (int i = 0; i < trainings.size(); i++) {
                                                final JsonObject t = trainings.getJsonObject(i);

                                                // fetch training algorithm selection
                                                String type = t.getString(Constants.KEY_TYPE);

                                                // build JSON payload object 
                                                JsonObject trainPayload = new JsonObject();
                                                trainPayload.put(Constants.KEY_EXPID, expId);
                                                trainPayload.put(Constants.KEY_DATASETID, datasetId);
                                                trainPayload.put(Constants.KEY_ALGORITHM,
                                                        t.getString(Constants.KEY_ALGORITHM));
                                                trainPayload.put(Constants.KEY_THREAD,
                                                        t.getBoolean(Constants.KEY_THREAD));

                                                // trigger training
                                                // resulting model will be saved in the filesystem and referenced from the database
                                                vertx.eventBus().send(Constants.BASE_ADDRESS_TRAINING + "." + type, trainPayload);
                                                
                                            }
                                        }

                                        LOGGER.log(Level.INFO, String.format("[TRACE LOG] Stop periodic check"));

                                        // can now stop this periodic check
                                        vertx.cancelTimer(id);

                                    } catch (Exception e) {
                                        LOGGER.log(Level.SEVERE, "Failed to unsubscribe from training data feed.", e);
                                    }
                                }
                            }
                        });
                    });
                }

                // response: success
                msg.reply("Successfully subscribed to training data feed.");

            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to start Aggregation Handler.", e);

                // response: error
                msg.reply("Failed to subscribe to training data feed.");
            }
        });

        // unsubscribe to a training data feed
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_UNSUBSCRIBE, msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "The POST request payload: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            try {
                // disable parameter feed
                ApplicationManager.getInstance().enableSupervisorParametersSubscription(expId, datasetId, false);

                // remove from map
                ApplicationManager.getInstance().removeAggregationHandler(expId, datasetId);

                // response: success
                msg.reply("Successfully unsubscribed to training data feed.");

            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to unsubscribe from training data feed.", e);

                // response: error
                msg.reply("Failed to unsubscribe from training data feed.");
            }
        });
    }

    /**
     * Convert a jsonArray to an Array List
     * @param jsonArray holds the json array
     * @return converted JsonArray to Array List
     */
    private List<String> createArrayFromJsonArray(JsonArray jsonArray) {

        // create the variable to hold the array list
        List<String> paramNameList = new ArrayList<String>();

        jsonArray.forEach(e -> {
            paramNameList.add((String) e);
        });
        /*for (int i = 0; i < jsonArray.size(); i++) {
            paramNameList.add(jsonArray.getString(i));
        }*/
           
        // retrieve the Array list
        return paramNameList;
    }

    private Map<String, Boolean> createLabelMapFromJsonObject(JsonObject jsonObject) {

        // the labels (expected output) map
        Map<String, Boolean> labelMap = new HashMap<String, Boolean>();

        // iterator to loop through the json object's <key, value> pairs
        Iterator<Map.Entry<String, Object>> iter = jsonObject.iterator();
        
        // put <key, value> pairs in the map
        while(iter.hasNext()){
            Map.Entry<String, Object> entry = iter.next();
            labelMap.put(entry.getKey(), (Boolean)entry.getValue());
        }

        // return the map
        return labelMap;
    }
}