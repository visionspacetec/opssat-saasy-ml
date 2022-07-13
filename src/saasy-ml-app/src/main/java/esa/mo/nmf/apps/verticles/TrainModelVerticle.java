package esa.mo.nmf.apps.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.PropertiesManager;
import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.dataset.utils.GenerateDataset;
import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.impl.TotpAuthHandlerImpl;
import jsat.DataSet;
import jsat.utils.random.RandomUtil;

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

                // get the total number of columns 
                vertx.eventBus().request("saasyml.training.data.count_columns", selectPayload, reply -> {
                    JsonObject response = (JsonObject) (reply.result().body());

                    // if the total number of columns exists, we get it
                    if (response.containsKey("count")) {
                        selectPayload.put("total_cols", response.getInteger("count").intValue());
                    }
                
                    // TODO QUESTION. Can the above eventbus be called asynchronous?, if so, we can move the following eventBus out
                    vertx.eventBus().request("saasyml.training.data.select", selectPayload, reply_select -> {
                        JsonObject response_select = (JsonObject) (reply_select.result().body());

                        int total_columns = selectPayload.getInteger("total_cols");
                        
                        LOGGER.log(Level.INFO, "total columns : " + total_columns);
                    
                        // response_select object contains the data
                        if(response_select.containsKey("data")) {
        
                            // 1.2. Prepare data 
                            final JsonArray data = response_select.getJsonArray("data");

                            long colCount = 0;
                            int rowCount = 1;
                            LOGGER.log(Level.INFO, String.format("row %d", rowCount));
                            for (int pos = 0; pos < data.size(); pos++) {
                                
                                // get the Json Object 
                                JsonObject object = data.getJsonObject(pos);

                                colCount++;

                                LOGGER.log(Level.INFO, String.format("%d %s %s (%s)", colCount, object.getString("param_name"), object.getString("value"), object.getString("data_type")));                            

                                if (colCount == total_columns){
                                    rowCount++;
                                    colCount = 0;
                                    LOGGER.log(Level.INFO, String.format("row %d", rowCount));
                                }
                            }

                            DataSet train = GenerateDataset.get2ClassLinear(200, RandomUtil.getRandom());
                            LOGGER.log(Level.INFO, "Generated train data: \n" + train.toString());

                            // name of the algorithm
                            // String algorithm = "LogisticRegressionDCD";

                            // instantiate the class
                            // get data from the configuration
                            boolean thread = PropertiesManager.getInstance().getThread();
                            boolean serialize = PropertiesManager.getInstance().getSerialize();
                            IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, algorithm);


                            // build the model
                            saasyml.build(algorithm);

                            // upload the train dataset
                            saasyml.setDataSet(train, null);

                            // start training the model
                            saasyml.train();

                            // 1.3. train model 
                            // Here we enter ML pipeline for the given algorithm

                        }
                    });
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
