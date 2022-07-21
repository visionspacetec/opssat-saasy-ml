package esa.mo.nmf.apps.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.AppMCAdapter;
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
                TrainModelVerticle.class.getSimpleName(), DatabaseVerticle.class.getSimpleName() };
                
        // get the canonical path/name of the Verticles
        String[] classNames = new String[] { FetchTrainingDataVerticle.class.getCanonicalName(),
                TrainModelVerticle.class.getCanonicalName(), DatabaseVerticle.class.getCanonicalName()};

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
        router.post("/api/v1/training/data/subscribe")
                .handler(BodyHandler.create())
                .handler(this::trainingDataSubscribe);

        // route for training data feed unsubscription
        router.post("/api/v1/training/data/unsubscribe")
                .handler(BodyHandler.create())
                .handler(this::trainingDataUnsubscribe);

        // route for uploading custom training data feed 
        router.post("/api/v1/training/data/save")
                .handler(BodyHandler.create())
                .handler(this::trainingDataSave);

        // route for deleting training data feed 
        router.post("/api/v1/training/data/delete")
                .handler(BodyHandler.create())
                .handler(this::trainingDataDelete);

        // route for train type model, the given algorithm is pass by parameter
        router.post("/api/v1/training/:type/")
                .handler(BodyHandler.create())
                .handler(this::trainingModel);

        // route for train model using given algorithm
        router.post("/api/v1/training/:type/:group/:algorithm/")
                .handler(BodyHandler.create())
                .handler(this::trainingModel);
        
        // todo
        // route for inference
        // router.get("/api/v1/inference").handler(this::inference);

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
            vertx.eventBus().request("saasyml.training.data.subscribe", payload, reply -> {
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
            vertx.eventBus().request("saasyml.training.data.unsubscribe", payload, reply -> {
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
     * delete training data
     * 
     * @param ctx body context of the request
     */
    void trainingDataSave(RoutingContext ctx) {
        // payload
        JsonObject payload = ctx.getBodyAsJson();

        try {
            // forward request to event bus to be handled in the appropriate Verticle
            vertx.eventBus().request("saasyml.training.data.save", payload, reply -> {
                // return response from the verticle
                ctx.request().response().end((String) reply.result().body());
            });

        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while saving training data.");
        }
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
            vertx.eventBus().request("saasyml.training.data.delete", payload, reply -> {
                // return response from the verticle
                ctx.request().response().end((String) reply.result().body());
            });

        } catch (Exception e) {
            // error response message
            Map<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("message", "error while deleting training data.");
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
        // group is "bayesian"
        // algorithm is "aode"

        String type = ctx.pathParam("type");
        String group = ctx.pathParam("group");
        String algorithm = ctx.pathParam("algorithm");
        String thread = ctx.pathParam("thread");
        
        if (group != null)
            payload.put("group", group);
        if (algorithm != null)
            payload.put("algorithm", algorithm);
        if (thread != null)
            payload.put("thread", thread);

        // forward request to event bus
        try {
            vertx.eventBus().request("saasyml.training." + type, payload, reply -> {
                ctx.request().response().end((String) reply.result().body());
            });
        } catch (Exception e) {
            // error object
            resMap.put("request", "error");
            resMap.put("message", "unsupported or invalid training request");

            // error response
            ctx.request().response().putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(resMap));
        }
    }

    /**
     * use a trained model to execute an inference
     * 
     * @param ctx
     */
    /**
     * void inference(RoutingContext ctx) {
     * // response map
     * Map<String, String> resMap = new HashMap<String, String>();
     * 
     * // populate map
     * resMap.put("request", "inference");
     * 
     * // response
     * ctx.request().response()
     * .putHeader("content-type", "application/json; charset=utf-8")
     * .end(Json.encodePrettily(resMap));
     * }
     */

}
