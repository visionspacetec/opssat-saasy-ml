package esa.mo.nmf.apps.saasyml.api.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import esa.mo.nmf.apps.ApplicationManager;
import esa.mo.nmf.apps.PropertiesManager;
import esa.mo.nmf.apps.saasyml.api.Constants;

// TODO:
// this class does not adhere to the Dependency Inversion Principle. 
// 
// - consider depend on abstractions
public class FetchDatapoolParamsVerticle extends AbstractVerticle {
  
    // logger
    private static final Logger LOGGER = Logger.getLogger(FetchDatapoolParamsVerticle.class.getName());

    private boolean subscribe(JsonObject payload) {
        // the request payload (Json)
        LOGGER.log(Level.INFO, String.format("The POST %s request payload: %s", Constants.ADDRESS_DATA_SUBSCRIBE, payload.toString()));

        // parse the Json payload
        final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
        final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
        final double interval = payload.getInteger(Constants.KEY_INTERVAL).doubleValue();
        final String KEY_ID = "-" + expId + "-" + datasetId;
        
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

                LOGGER.log(Level.INFO, String.format("[TRACE LOG] Iterations %d", iterations));

                // set the periodic timer
                int periodicTimer = PropertiesManager.getInstance().getFetchDatapoolParamsVerticlePeriodicTimer();

                // init default values for simulate wait mutex
                payloadCount.put(Constants.KEY_TO_WAIT_IN_MILLISECONDS, Constants.MILLISECONDS_TO_WAIT_DEFAULT);
                // payloadCount.put(Constants.KEY_TO_SIMULATE_WAIT_MUTEX, false);

                // get the started data count and initialize some values
                vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT, payloadCount, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    if (response.containsKey(Constants.KEY_COUNT)) {
                        payloadCount.put(Constants.KEY_STARTED_COUNT + KEY_ID, response.getInteger(Constants.KEY_COUNT).intValue());
                        payloadCount.put(Constants.KEY_PREVIOUS_COUNTER + KEY_ID, 0);
                        payloadCount.put(Constants.KEY_TRIES + KEY_ID, 0);
                        payloadCount.put(Constants.KEY_MAX_TRIES, 4);
                    }
                });

                // register periodic timer
                vertx.setPeriodic(periodicTimer, id -> {

                        vertx.setTimer(payloadCount.getInteger(Constants.KEY_TO_WAIT_IN_MILLISECONDS), id2 -> {
                        
                            // if simulate wait mutex is not activate
                            // if (Boolean.FALSE.equals(payloadCount.getBoolean(Constants.KEY_TO_SIMULATE_WAIT_MUTEX))){
                                
                                // reset the default value 
                                payloadCount.put(Constants.KEY_TO_WAIT_IN_MILLISECONDS, Constants.MILLISECONDS_TO_WAIT_DEFAULT);
                                // payloadCount.put(Constants.KEY_TO_SIMULATE_WAIT_MUTEX, false);
        
                                vertx.eventBus().request(Constants.ADDRESS_DATA_COUNT, payloadCount, reply -> {
        
                                    Message<Object> result = reply.result();
        
                                    // stop timer if the result is null. I do not know the exact reason but is happening
                                    if (result == null){
                                        vertx.cancelTimer(id);
                                    } else {
                                        JsonObject response = (JsonObject) (result.body());
        
                                        // response object somehow does not contain the expected parameter (impossible?)
                                        // stop timer if this happens
                                        if (!response.containsKey(Constants.KEY_COUNT)) {
                                            vertx.cancelTimer(id);
                                        } else {
        
                                            int startedCount = payloadCount.getInteger(Constants.KEY_STARTED_COUNT + KEY_ID).intValue();
                                        
                                            // get the number of database rows introduced so far
                                            int count = response.getInteger(Constants.KEY_COUNT).intValue() - startedCount;
        
                                            // fixme: dividing by paramNameList.size() will break if the number of params change from one data fetching session to another for
                                            // the same expId and datasetId
                                            int counter = count / paramNameList.size();
        
                                            int previousCounter = payloadCount.getInteger(Constants.KEY_PREVIOUS_COUNTER + KEY_ID);
        
                                            LOGGER.log(Level.INFO, String.format("[E%d-D%d] Count %d, Counter %d, previous counter %d and number of rows in database when fetch data train started %d", expId, datasetId, count, counter, previousCounter, startedCount));
        

                                            int tries = 0;
                                            if (counter != previousCounter){
                                                // add current counter as previous counter
                                                payloadCount.put(Constants.KEY_PREVIOUS_COUNTER + KEY_ID, counter);
                                            } else {
                                                tries = payloadCount.getInteger(Constants.KEY_TRIES + KEY_ID);
                                                ++tries;
                                                LOGGER.log(Level.INFO, String.format("[E%d-D%d] We have not received new data. Number of tries %d/4", expId, datasetId, tries));
                                            }
                                            payloadCount.put(Constants.KEY_TRIES + KEY_ID, tries);

        
                                            // if we reached the maximum number of tries, we change the value in KEY_TO_WAIT_IN_MILLISECONDS 
                                            if (tries > payloadCount.getInteger(Constants.KEY_MAX_TRIES)) {
                                                LOGGER.log(Level.INFO, String.format("[E%d-D%d] Maximum number of tries reached. We sleep %d milliseconds.", expId, datasetId, Constants.MILLISECONDS_TO_WAIT));
                                                payloadCount.put(Constants.KEY_TO_WAIT_IN_MILLISECONDS, Constants.MILLISECONDS_TO_WAIT);
                                                payloadCount.put(Constants.KEY_TRIES + KEY_ID, 0);
                                                // we change the simulate wait mutex to true
                                                // payload.put(Constants.KEY_TO_SIMULATE_WAIT_MUTEX, true);
                                            } else {
                                                // the counter is set to -1 if there was an error while attempting to query the database
                                                // stop timer if  this happens
                                                if (counter < 0) {
                                                    vertx.cancelTimer(id);
                                                } else if (counter >= iterations) {
        
                                                    LOGGER.log(Level.INFO, String.format("[TRACE LOG] Counter higher than iterations"));
        
                                                    // target number of training dataset has been achieved
                                                    // unsubscribe from the training data feed
                                                    try {
                                                        try {
                                                            // disable parameter feed
                                                            ApplicationManager.getInstance().enableSupervisorParametersSubscription(
                                                                expId, datasetId, false);
                                        
                                                            LOGGER.log(Level.INFO, String.format("[TRACE LOG] Disabled parameter feed"));
                                                        } catch (Exception e) {
                                                            LOGGER.log(Level.SEVERE, String.format("[TRACE LOG] Exception Disable parameter feed", e));
                                                        }
        
                                                        // remove the aggregation handler from the map
                                                        ApplicationManager.getInstance().removeAggregationHandler(expId, datasetId);
        
                                                        // auto-trigger training if the payload is configured to do so
                                                        if (payload.containsKey(Constants.KEY_TRAINING)) {
        
                                                            // the training parameters can be for more than one algorithm
                                                            final JsonArray trainings = payload.getJsonArray(Constants.KEY_TRAINING);
        
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
        
                                        }
                                    }
                                });

                            //} else {
                            //    LOGGER.log(Level.INFO, String.format("[E%d-D%d] I am waiting, can not continue  ----  :(", expId, datasetId));
                            //}
                        });

                });
            }

            // response: success
            return true;

        } catch (Exception e) {
            // log
            LOGGER.log(Level.SEVERE, "Failed to start Aggregation Handler.", e);

            // response: error
            return false;
        }
    }


    @Override
    public void start() throws Exception {

        // log
        LOGGER.log(Level.INFO, "Starting a " + this.getClass().getSimpleName() + " Verticle instance with deployment id " + this.deploymentID() + ".");


        // subscribe to a training data feed
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_SUBSCRIBE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // subscribe to the feed
            if(subscribe(payload)) {
                msg.reply("Successfully subscribed to training data feed.");
            }else {
                msg.reply("Failed to subscribe to training data feed.");
            }
        });

        // inference classifier
        vertx.eventBus().consumer(Constants.BASE_ADDRESS_INFERENCE_SUBSCRIBE, msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // mark as inference feed
            ApplicationManager.getInstance().markInferenceFeed(expId, datasetId);

            // save the models to use as inference for later
            ApplicationManager.getInstance().addInferenceFeedModels(expId, datasetId, payload.getJsonArray(Constants.KEY_MODELS));

            // subscribe to the feed
            if(subscribe(payload)) {
                msg.reply("Successfully subscribed to inference feed.");
            }else {
                msg.reply("Failed to subscribe to inference feed.");
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