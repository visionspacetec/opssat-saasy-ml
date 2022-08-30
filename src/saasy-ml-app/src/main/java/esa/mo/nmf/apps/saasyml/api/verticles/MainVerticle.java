package esa.mo.nmf.apps.saasyml.api.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.AppMCAdapter;
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
    public void start() throws Exception {

        // add data received listener
        AppMCAdapter.getInstance().addDataReceivedListener(vertx);

        // set app http server port
        int port = PropertiesManager.getInstance().getPort();

        // deploy verticles
        deployVerticles();

        // define router and api paths
        Router router = createRouterAndAPIPaths();

        // create handler and listen port
        vertx.createHttpServer().requestHandler(router).listen(port);

    }

    /**
     * Function to create and deploy the verticles
     * 
     * TODO: make this configurable from a config file.
     * deployment options for multi-core and multi-threded goodness
     * specify the number of verticle instances that you want to deploy
     * this is useful for scaling easily across multiple cores
     */
    private void deployVerticles() {

        // get the name of the Verticles
        String[] simpleNames = new String[] { FetchTrainingDataVerticle.class.getSimpleName(),
                TrainModelVerticle.class.getSimpleName(), DatabaseVerticle.class.getSimpleName(),
                InferenceVerticle.class.getSimpleName() };
                
        // get the canonical path/name of the Verticles
        String[] classNames = new String[] { FetchTrainingDataVerticle.class.getCanonicalName(),
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
                .handler(this::trainingDataSubscribe);

        // route for training data feed unsubscription
        router.post(Constants.ENDPOINT_DATA_UNSUBSCRIBE)
                .handler(BodyHandler.create())
                .handler(this::trainingDataUnsubscribe);

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
        
        // route for inference 
        router.post(Constants.ENDPOINT_INFERENCE)
                .handler(BodyHandler.create())
                .handler(this::inference);

        return router;
    }

    /**
     * Function to Subscribe to training data feed
     * 
     * @param ctx body context of the request
     */
    void trainingDataSubscribe(RoutingContext ctx) {

        // payload
        JsonObject payload = ctx.getBodyAsJson();

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_SUBSCRIBE, payload, reply -> {
                // return response from the verticle
                ctx.request().response().end((String) reply.result().body());
            });
        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while subscribing to a training data feed.");

            // error response
            ctx.request().response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * unsubscribe to training data feed
     * 
     * @param ctx body context of the request
     */
    void trainingDataUnsubscribe(RoutingContext ctx) {

        // payload
        JsonObject payload = ctx.getBodyAsJson();

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_UNSUBSCRIBE, payload, reply -> {
                // return response from the verticle
                ctx.request().response().end((String) reply.result().body());
            });
        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while unsubscribing to a training data feed.");

            // error response
            ctx.request().response()
                .putHeader("content-type", "application/json; charset=utf-8")
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

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_SAVE, payload, reply -> {
                // return response from the verticle
                ctx.request().response().end((String) reply.result().body());
            });

        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while saving training data.");

            // error response
            ctx.request().response()
                    .putHeader("content-type", "application/json; charset=utf-8")
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

        try {
            
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_TRAINING_DATA_SELECT, payload, reply -> {
                JsonObject json = (JsonObject) reply.result().body();

                json = prepareDownloadResponse(json);
                json.put(Constants.KEY_EXPID, payload.getInteger(Constants.KEY_EXPID));
                json.put(Constants.KEY_DATASETID, payload.getInteger(Constants.KEY_DATASETID));
                
                // TODO: temporal code for the EM Session. Remove later
                // json.put(Constants.KEY_DATASETID, payload.getInteger(Constants.KEY_DATASETID) + 1);
                
                /*JsonArray jsonArray = new JsonArray();
                JsonObject object = new JsonObject();
                object.put(Constants.LABEL_TYPE, "classifier");
                object.put(Constants.LABEL_ALGORITHM, "aode");
                jsonArray.add(object);
                json.put(Constants.LABEL_TRAINING, jsonArray);*/
                
                // temporal code for the EM Session. Remove later
                // And replace for this code:
                // json.put(Constants.LABEL_DATASETID, payload.getInteger(Constants.LABEL_DATASETID));

                ctx.request().response()
                    .putHeader("Content-Type", "application/json; charset=utf-8")
                    .end(json.encode());
            });

        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while saving training data.");

            // error response
            ctx.request().response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(responseMap));
        }
    }

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

                // TODO: temporal code for the EM Session. Remove later
                // JsonObject labelObject = new JsonObject();
                // labelObject.put(Constants.KEY_NAME, "label");
                // labelObject.put(Constants.KEY_VALUE, String.valueOf(new Random().nextInt(4)));
                // labelObject.put(Constants.KEY_DATA_TYPE, object.getValue("data_type"));
                // labelObject.put(Constants.KEY_TIMESTAMP, object.getLong(Constants.KEY_TIMESTAMP));
                // newData.add(labelObject);
                // Temporal code for the EM Session. Remove later

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

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request(Constants.ADDRESS_DATA_DELETE, payload, reply -> {
                // return response from the verticle
                ctx.request().response().end((String) reply.result().body());
            });

        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while deleting training data.");
            // error response
            ctx.request().response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(responseMap));
        }
    }

    /**
     * Function to train the model
     * 
     * @param ctx body context of the request
     */
    void trainingModel(RoutingContext ctx) {

        // response map
        Map<String, String> resMap = new HashMap<String, String>();

        // get the payload
        JsonObject payload = ctx.getBodyAsJson();

        // get api request url params
        // e.g. /api/v1/training/classifier/classifier.bayesian.aode
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
                JsonObject json = (JsonObject) reply.result().body();            
                ctx.request().response().putHeader("Content-Type", "application/json; charset=utf-8").end(json.encode());
            });
        } catch (Exception e) {
            // error object
            resMap.put(Constants.KEY_RESPONSE, "error");
            resMap.put(Constants.KEY_MESSAGE, "unsupported or invalid training request");            
            ctx.request().response().end("Error message with JSON: " + (String) Json.encodePrettily(resMap));
        }
    }

    /**
     * use a trained model to execute an inference
     * 
     * @param ctx
     */
    void inference(RoutingContext ctx) {

        // response map
        Map<String, String> resMap = new HashMap<String, String>();

        // get the payload
        JsonObject payload = ctx.getBodyAsJson();

        // get api request url payload
        // e.g. /api/v1/inference/

        // forward request to event bus
        try {
            vertx.eventBus().request(Constants.BASE_ADDRESS_INFERENCE, payload, reply -> {
                JsonObject json = (JsonObject) reply.result().body();
                ctx.request().response().putHeader("Content-Type", "application/json; charset=utf-8").end(json.encode());
            });

        } catch (Exception e) {
            // error object
            resMap.put(Constants.KEY_RESPONSE, "error");
            resMap.put(Constants.KEY_MESSAGE, "unsupported or invalid inference request");
            ctx.request().response().end("Error message with JSON: " + (String) Json.encodePrettily(resMap));
        }
    }

}
