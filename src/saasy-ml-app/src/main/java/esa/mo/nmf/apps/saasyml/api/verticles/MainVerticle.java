package esa.mo.nmf.apps.saasyml.api.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.DeploymentOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.AppMCAdapter;
import esa.mo.nmf.apps.ApplicationManager;
import esa.mo.nmf.apps.ExtensionManager;
import esa.mo.nmf.apps.saasyml.api.Constants;
import esa.mo.nmf.apps.PropertiesManager;


/**
 * Main Verticle of the API. 
 * 
 * This class defines all the routes of our API. 
 * 
 * @author Georges Labreche
 * @author Cesar Guzman
 */
public class MainVerticle extends AbstractVerticle {
    private static final Logger LOGGER = Logger.getLogger(MainVerticle.class.getName());

    // constructor
    public MainVerticle() {
        vertx = Vertx.vertx();
    }

    @Override
    public void stop() throws Exception {
        // stop and unload the plugins
        ExtensionManager.getInstance().stopPlugins();
    }

    @Override
    public void start() throws Exception {

        // log
        LOGGER.log(Level.INFO, "Starting the " + this.getClass().getSimpleName() + " Verticle instance.");

        // load and start the plugins
        ExtensionManager.getInstance().startPlugins();

        // add data received listener
        AppMCAdapter.getInstance().addDataReceivedListener(vertx);

        // set app http server port
        int port = PropertiesManager.getInstance().getPort();

        // deploy verticles
        deployVerticles();

        // define router and api paths
        Router router = createRouterAndAPIPaths();

        createFailureHandler(router);

        // create handler and listen port
        vertx.createHttpServer().requestHandler(router).listen(port);

    }

    /**
     * Function to create and deploy the verticles
     * 
     * deployment options for multi-core and multi-threded goodness
     * specify the number of verticle instances that you want to deploy
     * this is useful for scaling easily across multiple cores
     */
    private void deployVerticles() {

        // get the name of the Verticles
        String[] simpleNames = new String[] { FetchDatapoolParamsVerticle.class.getSimpleName(),
                TrainModelVerticle.class.getSimpleName(), DatabaseVerticle.class.getSimpleName(),
                InferenceVerticle.class.getSimpleName() };
                
        // get the canonical path/name of the Verticles
        String[] classNames = new String[] { FetchDatapoolParamsVerticle.class.getCanonicalName(),
                TrainModelVerticle.class.getCanonicalName(), DatabaseVerticle.class.getCanonicalName(),
                InferenceVerticle.class.getCanonicalName() };

        // total number of Verticles
        int length = simpleNames.length;

        // create and deploy the Verticles
        LOGGER.log(Level.INFO, "Deploying Verticles");
        for (int index = 0; index < length; index++) {

            // create the Verticle as deployment Options
            DeploymentOptions deployOpt = new DeploymentOptions().setWorker(true)
                    .setInstances(PropertiesManager.getInstance().getVerticalInstanceCount(simpleNames[index]));

            // deploy the verticle
            vertx.deployVerticle(classNames[index], deployOpt);
        }
    }

    /**
     * Function to create Router and defines API paths 
     * 
     * For each API path, we define the handler's function.
     * 
     * @return defined Router with API paths
     */
    private Router createRouterAndAPIPaths() {
        
        // define router and API paths
        Router router = Router.router(vertx);

        // todo: validate json payload against schema, see
        // https://vertx.io/docs/vertx-web-validation/java/

        // route for training data feed subscription
        router.post(Constants.ENDPOINT_DATA_SUBSCRIBE)
                .handler(BodyHandler.create())
                .handler(this::subscribeDatapoolParameterTrainingDataFeed);

        // route for training data feed unsubscription
        router.post(Constants.ENDPOINT_DATA_UNSUBSCRIBE)
                .handler(BodyHandler.create())
                .handler(this::unsubscribeDatapoolParameterFeed);

        // route for downloading training data feed 
        router.post(Constants.ENDPOINT_DATA_DOWNLOAD)
                .handler(BodyHandler.create())
                .handler(this::trainingDataDownload);

        // route for uploading custom training data feed 
        router.post(Constants.ENDPOINT_DATA_SAVE)
                .handler(BodyHandler.create())
                .handler(this::trainingDataSave);

        // route for deleting training data feed 
        router.post(Constants.ENDPOINT_DATA_DELETE)
                .handler(BodyHandler.create())
                .handler(this::trainingDataDelete);

        // route for train type model, the given algorithm is pass by parameter
        router.post(Constants.ENDPOINT_TRAINING)
                .handler(BodyHandler.create())
                .handler(this::trainingModel);

        // route for train model using given algorithm
        router.post(Constants.ENDPOINT_TRAINING_ALGORITHM)
                .handler(BodyHandler.create())
                .handler(this::trainingModel);

        // route for models metadata
        router.post(Constants.ENDPOINT_MODELS)
                .handler(BodyHandler.create())
                .handler(this::fetchTrainingModels);
                
        // route for inference 
        router.post(Constants.ENDPOINT_INFERENCE)
                .handler(BodyHandler.create())
                .handler(this::inference);

        // route to subscribe to inference feed
        router.post(Constants.ENDPOINT_INFERENCE_SUBSCRIBE)
                .handler(BodyHandler.create())
                .handler(this::subscribeDatapoolParameterInferenceFeed);

        // route to unsubscribe to inference feed
        router.post(Constants.ENDPOINT_INFERENCE_UNSUBSCRIBE)
                .handler(BodyHandler.create())
                .handler(this::unsubscribeDatapoolParameterFeed);

        return router;
    }

    /**
     * Function to create failure handler of the Router  
     * 
     * For any API endpoint, we define the failure handler
     * 
     * @return defined Router with API paths and failure handlers
     */
    private void createFailureHandler(Router router) {

        router.post("/*").failureHandler(rc -> {
            LOGGER.log(Level.SEVERE, "Handling failure");
            
            // to convert trace into a String:
            StringWriter sw = new StringWriter();
 
            // create a PrintWriter
            PrintWriter pw = new PrintWriter(sw);

            Throwable failure = rc.failure();
            String message = "There is an Internal Error";
            if (failure != null) {
                message = failure.getMessage();
                failure.printStackTrace(pw);
                LOGGER.log(Level.SEVERE, sw.toString());
            }
    
            HttpServerResponse response = rc.response();
            response.setStatusCode(rc.statusCode()).setStatusMessage(message);
            response.end();    
        });

    }

    /**
     * Function to Subscribe to training data feed
     * 
     * @param ctx body context of the request
     */
    void subscribeDatapoolParameterTrainingDataFeed(RoutingContext ctx) {

        // payload
        JsonObject payload = ctx.getBodyAsJson();

        // response map
        Map<String, String> responseMap = new HashMap<String, String>();

        try {

            // parse expected labels from payload if it has been set in the payload
            // can either me a expected label object or the classpath of the plugin extension that needs to be executed
            parseExpectedLabelsOrPlugins(payload);

            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_SUBSCRIBE, payload, reply -> {
                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, reply.result().body().toString());

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
            });
        } catch (Exception e) {
            // error response message
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "error while subscribing to a training data feed.");

            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        } 
    }

    /**
     * unsubscribe to training data or inference feed
     * 
     * @param ctx body context of the request
     */
    void unsubscribeDatapoolParameterFeed(RoutingContext ctx) {

        // payload
        JsonObject payload = ctx.getBodyAsJson();

        // response 
        Map<String, String> responseMap = new HashMap<String, String>();

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_UNSUBSCRIBE, payload, reply -> {
                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, reply.result().body().toString());

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
            });
        } catch (Exception e) {
            // error response message
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "error while unsubscribing to datapool parameter feed.");

            // error response
            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * save custom training data
     * 
     * @param ctx body context of the request
     */
    void trainingDataSave(RoutingContext ctx) {
        // payload
        JsonObject payload = ctx.getBodyAsJson();

        // response
        Map<String, String> responseMap = new HashMap<String, String>();

        try {

            // parse expected labels from payload if it has been set in the payload
            // can either me a expected label object or the classpath of the plugin extension that needs to be executed
            parseExpectedLabelsOrPlugins(payload);

            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_SAVE, payload, reply -> {
                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, reply.result().body().toString());

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
            });

        } catch (Exception e) {
            // error response message
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "error while saving training data.");

            // error response
            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }
    }
    
    /**
     * download training data
     * 
     * @param ctx body context of the request
     */
    void trainingDataDownload(RoutingContext ctx) {
        // payload
        JsonObject payload = ctx.getBodyAsJson();

        // response
        Map<String, Object> responseMap = new HashMap<String, Object>();

        try {

            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payload, reply -> {
                JsonObject json = (JsonObject) reply.result().body();

                json = prepareDownloadResponse(json);
                json.put(Constants.KEY_EXPID, payload.getInteger(Constants.KEY_EXPID));
                json.put(Constants.KEY_DATASETID, payload.getInteger(Constants.KEY_DATASETID));

                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, json);

                ctx.request().response()
                        .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                        .end(Json.encodePrettily(responseMap));
            });

        } catch (Exception e) {
            // error response message
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "error while download the training data.");

            // error response
            ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * Function to prepare the final output to the user
     * 
     * @param json text with the information of the training data
     * @return JSON text with the final output. It contains the training data
     */
    private JsonObject prepareDownloadResponse(JsonObject json) {

        JsonArray newData = new JsonArray();
        JsonArray finalResponse = new JsonArray();

        JsonArray data = json.getJsonArray(Constants.KEY_DATA);

        // variable to control the number of columns
        String timestamp = "";

        // iterate throughout all the data
        for (int pos = 0; pos < data.size(); pos++) {

            // get the Json Object and store the value
            JsonObject object = data.getJsonObject(pos);

            String cur_timestamp = object.getString(Constants.KEY_TIMESTAMP);
            if (timestamp == "") {
                timestamp = cur_timestamp;
            }

            // create a new JsonObject
            JsonObject newObject = new JsonObject();
            newObject.put(Constants.KEY_NAME, object.getValue("param_name"));
            newObject.put(Constants.KEY_VALUE, object.getValue(Constants.KEY_VALUE));
            newObject.put(Constants.KEY_DATA_TYPE, object.getValue("data_type"));
            newObject.put(Constants.KEY_TIMESTAMP, object.getLong(Constants.KEY_TIMESTAMP));
            
            // if colcount is equal to total columns, we add a new row
            if (!timestamp.equals(cur_timestamp)) {
                timestamp = cur_timestamp;

                finalResponse.add(newData.copy());
                newData = new JsonArray();
            }

            newData.add(newObject);
        }

        JsonObject finalObject = new JsonObject();
        finalObject.put(Constants.KEY_DATA, finalResponse);
        
        return finalObject;
    }

    /**
     * delete training data
     * 
     * @param ctx body context of the request
     */
    void trainingDataDelete(RoutingContext ctx) {
        // payload
        JsonObject payload = ctx.getBodyAsJson();

        // response
        Map<String, String> responseMap = new HashMap<String, String>();

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_DELETE, payload, reply -> {
                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, reply.result().body().toString());

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
            });

        } catch (Exception e) {
            // error response message
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "error while deleting training data.");
            
            // error response
            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * Function to train the model
     * 
     * @param ctx body context of the request
     */
    void trainingModel(RoutingContext ctx) { 

        // get the payload
        JsonObject payload = ctx.getBodyAsJson();

        // response map
        Map<String, String> responseMap = new HashMap<String, String>();

        // get api request url params
        // e.g. /api/v1/training/classifier/aode
        // type is "classifier"
        // algorithm is "aode"

        String type = ctx.pathParam(Constants.KEY_TYPE);
        String algorithm = ctx.pathParam(Constants.KEY_ALGORITHM);        
        if (algorithm != null)
            payload.put(Constants.KEY_ALGORITHM, algorithm);

        // forward request to event bus
        try { 
            // now make the training request
            vertx.eventBus().request(Constants.BASE_ADDRESS_TRAINING + "." + type, payload, reply -> {                
                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, reply.result().body().toString());

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
            });
        } catch (Exception e) {
            // error object
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "unsupported or invalid training request");            
            
            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * fetch info on trained models
     * 
     * @param ctx body context of the request
     */
    void fetchTrainingModels(RoutingContext ctx) {
        // get the payload
        JsonObject payload = ctx.getBodyAsJson();

        LOGGER.log(Level.INFO, "[TRACE LOG] Fetch training models");

        // response 
        Map<String, Object> responseMap = new HashMap<String, Object>();
        
        try {

            // forward request to event bus
            vertx.eventBus().request(Constants.ADDRESS_MODELS_SELECT, payload, reply -> {
                JsonObject json = (JsonObject) reply.result().body();

                // return response from the verticle
                responseMap.put(Constants.KEY_RESPONSE, json);

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
                    
            });
        } catch (Exception e) {   
            // error response message
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "unsupported or invalid models request");

            // error response
            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * use a trained model to execute an inference
     * 
     * @param ctx body context of the request
     */
    void inference(RoutingContext ctx) {

        // get the payload
        JsonObject payload = ctx.getBodyAsJson();

        // response map
        Map<String, String> responseMap = new HashMap<String, String>();

        // get api request url payload
        // e.g. /api/v1/inference

        // forward request to event bus
        try {
            vertx.eventBus().request(Constants.BASE_ADDRESS_INFERENCE, payload, reply -> {
                JsonObject json = (JsonObject) reply.result().body();

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(json.encode());
            });

        } catch (Exception e) {
            // error object
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "unsupported or invalid inference request");

            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }
    }


    /**
     * Subscribe to a datapool parameter feed and execute inferences
     * @param ctx context of the request
     */
    void subscribeDatapoolParameterInferenceFeed(RoutingContext ctx) {
        // get the payload
        JsonObject payload = ctx.getBodyAsJson();

        // response map
        Map<String, String> responseMap = new HashMap<String, String>();

        // forward request to event bus
        try {
            vertx.eventBus().request(Constants.BASE_ADDRESS_INFERENCE_SUBSCRIBE, payload, reply -> {
                responseMap.put(Constants.KEY_RESPONSE, reply.result().body().toString());

                ctx.request().response()
                    .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                    .end(Json.encodePrettily(responseMap));
            });

        } catch (Exception e) {
            // error object
            responseMap.put(Constants.KEY_RESPONSE, Constants.VALUE_ERROR);
            responseMap.put(Constants.KEY_MESSAGE, "unsupported or invalid inference subscription request");

            ctx.request().response()
                .putHeader(Constants.KEY_CONTENT_TYPE, Constants.KEY_CONTENT_TYPE_JSON)
                .end(Json.encodePrettily(responseMap));
        }

    }


    /**
     * Function to parse the expected labels or the plugins. 
     * 
     * The plugins are classes that allow the user to provide their own labels based on the explanatory data
     * 
     * @param payload Json text received from the user
     */
    private void parseExpectedLabelsOrPlugins(JsonObject payload) {

        // parse experiment id and dataset id
        final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
        final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

        // the labels map for the expected output
        // this will be read when inserting the fetched training data
        // the labels will be inserted into their own labels table
        if (payload.containsKey(Constants.KEY_LABELS)) {
            final Map<String, Boolean> labelMap = createLabelMapFromJsonObject(
                    payload.getJsonObject(Constants.KEY_LABELS));
            ApplicationManager.getInstance().addLabels(expId, datasetId, labelMap);
            ApplicationManager.getInstance().addExtensionClasspath(expId, datasetId, "null");
        } else {
            // the plugins map to calculate the expected output
            // this will be read when inserting the fetched training data
            // the plugins will be inserted as extension classpath into their own table
            if (payload.containsKey(Constants.KEY_LABELS_PLUGIN)) {
                // identifier for plugin to calculate the expected label 
                if (ApplicationManager.getInstance().getLabels(expId, datasetId) != null) {
                    ApplicationManager.getInstance().getLabels(expId, datasetId).clear();
                }
                ApplicationManager.getInstance().addExtensionClasspath(expId, datasetId,
                        payload.getString(Constants.KEY_LABELS_PLUGIN));
            }
        }
    }
    
    /**
     * Function to translate the JsonObject to a Map
     * 
     * @param jsonObject Json text with the information of the labels
     * @return a hash map with the information of the JsonObject
     */
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