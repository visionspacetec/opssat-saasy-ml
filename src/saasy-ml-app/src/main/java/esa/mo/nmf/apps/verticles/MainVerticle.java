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

        // todo: make this configurable from a config file.
        // deployment options for multi-core and multi-threded goodness
        // specify the number of verticle instances that you want to deploy
        // this is useful for scaling easily across multiple cores
        DeploymentOptions[] deployOpts = new DeploymentOptions[3];
        String[] simpleNames = new String[] { FetchTrainingDataVerticle.class.getSimpleName(),
                        TrainModelVerticle.class.getSimpleName(), DatabaseVerticle.class.getSimpleName() };
        String[] classNames = new String[] { "esa.mo.nmf.apps.verticles.FetchTrainingDataVerticle",
                        "esa.mo.nmf.apps.verticles.TrainModelVerticle", "esa.mo.nmf.apps.verticles.DatabaseVerticle"};

        // create and deploy the verticles
        LOGGER.log(Level.INFO, "Deploying Verticles for port "+ port);
        for (int index = 0; index < 3; index++) {
            deployOpts[index].setWorker(true).setInstances(PropertiesManager.getInstance().getVerticalInstanceCount(simpleNames[index]));
            vertx.deployVerticle(classNames[index], deployOpts[index]);
        }

        // define router and api paths
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

        // listen
        vertx.createHttpServer().requestHandler(router).listen(port);
    }

    /**
     * Subscribe to training data feed
     * 
     * @param ctx
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
     * @param ctx
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
     * @param ctx
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
     * @param ctx
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
     * train a model
     * 
     * @param ctx
     */
    void trainingModel(RoutingContext ctx) {

        // response map
        Map<String, String> resMap = new HashMap<String, String>();

        // payload
        JsonObject payload = ctx.getBodyAsJson();

        // get api request url params
        // e.g. /api/v1/training/classifier/classifier.bayesian.aode
        // type is "classifier"
        // group is "bayesian"
        // algorithm is "aode"

        String type = ctx.pathParam("type");
        String group = ctx.pathParam("group");
        String algorithm = ctx.pathParam("algorithm");
        if (group != null)
            payload.put("group", group);
        if (algorithm != null)
            payload.put("algorithm", algorithm);

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
