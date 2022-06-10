package com.tanagraspace.vertxservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.Json;
import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

  /**
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.createHttpServer().requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
  */

  
  @Override
  public void start() throws Exception {

    // set instance count
    int port;
    try {
      port = Integer.parseInt(System.getProperty("port", "8888"));
    } catch (NumberFormatException e) {
      port = 8888;
    }

    // verbosity
    System.out.println("Port: " + port);

    // set instance count
    int instanceCount;
    try {
      instanceCount = Integer.parseInt(System.getProperty("instance.count", "1"));
    } catch (NumberFormatException e) {
      instanceCount = 1;
    }

    // verbosity
    System.out.println("Instances: " + instanceCount);

    // deployment options for multi-core and multi-threded goodness
    DeploymentOptions trainingDeployOpts = new DeploymentOptions()
      .setWorker(true)
      .setInstances(instanceCount);
    
    vertx.deployVerticle("com.tanagraspace.vertxservice.TrainingVerticle", trainingDeployOpts);
    Router router = Router.router(vertx);

    // route: training
    router.get("/api/v1/training/classifier/:group/:algorithm").handler(this::trainClassfifier);

    // route: inference
    router.get("/api/v1/inference").handler(this::inference);
      
    // listen
    vertx.createHttpServer().requestHandler(router).listen(port);
  }

  void trainClassfifier(RoutingContext ctx) {

      // response map
      Map<String, String> resMap = new HashMap<String, String>();

      // api request url params
      String group = ctx.pathParam("group");
      String algorithm = ctx.pathParam("algorithm");

      // forward request to event bus
      try
      {
        vertx.eventBus().request("saasyml.training.classifier." + group + "." + algorithm , "", reply -> {
          ctx.request().response().end((String)reply.result().body());
        });
      } catch (Exception e) {
        // error object
        resMap.put("request", "error");
        resMap.put("message", "unsupported or invalid training request");

        // error response
        ctx.request().response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(resMap));
      }
  }

  void inference(RoutingContext ctx) {
      // response map
      Map<String, String> resMap = new HashMap<String, String>();

      // populate map
      resMap.put("request", "inference");

      // response
      ctx.request().response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(resMap));
  }
}
