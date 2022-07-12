package esa.mo.nmf.apps.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class TrainModelVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(TrainModelVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // train classifier
        vertx.eventBus().consumer("saasyml.training.classifier", msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "training.classifier: triggered by the following POST request: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final String group = payload.getString("group");
            final String algorithm = payload.getString("algorithm");

            // todo:
            
            // 1. Train model using the expId and datasetId to fetch traning data
            // that was stored from AggregationWrite.

            // 1.1. get data using expId and datasetId
            try{

                // build Json payload object with just expId and datasetId
                JsonObject selectPayload = new JsonObject();
                selectPayload.put("expId", expId);
                selectPayload.put("datasetId", datasetId);
                
                vertx.eventBus().request("saasyml.training.data.select", selectPayload, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());
                    
                    // response object contains the data
                    if(response.containsKey("data")){    
    
                        // 1.2. prepare data 

                        // name of the model
                        String modelName = "LogisticRegressionDCD";

                        // instantiate the class
                        IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(thread, serialize, modelName);

                        logger.info("Generate training dataset...");
                        DataSet train = GenerateDataset.get2ClassLinear(200, RandomUtil.getRandom());

                        // build the model
                        saasyml.build(modelName);

                        // upload the train dataset
                        saasyml.setDataSet(train, null);

                        // start training the model
                        saasyml.train();

                        // 1.3. train model 
                        // Here we enter ML pipeline for the given algorithm

                    }
                });
            } catch(Exception e){
                // log
                LOGGER.log(Level.SEVERE, "Failed to get training data.", e);

                // response: error
                msg.reply("Failed to get training data.");
            } 


            // 2. Serialize and save the resulting model.
            // Make sure it is uniquely identifiable with expId and datasetId, maybe as part
            // of the toGround folder file system:
            //
            // 3. Return a message with unique identifiers of the serizalized model (or
            // maybe just a path to it?)

            msg.reply(String.format("training: classifier %s %s", group, algorithm));
        });

        // train outlier
        vertx.eventBus().consumer("saasyml.training.outlier", msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "training.outlier: triggered by the following POST request: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final String group = payload.getString("group");
            final String algorithm = payload.getString("algorithm");

            msg.reply(String.format("training: outlier %s %s", group, algorithm));
            
        });

        // train clustering
        vertx.eventBus().consumer("saasyml.training.clustering", msg -> {
            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO,"training.clustering: triggered by the following POST request: " + payload.toString());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final String group = payload.getString("group");
            final String algorithm = payload.getString("algorithm");

            msg.reply(String.format("training: clustering %s %s", group, algorithm));
        });

    }
}
