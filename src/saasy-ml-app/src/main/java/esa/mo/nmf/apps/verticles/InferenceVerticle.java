package esa.mo.nmf.apps.verticles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.Constants;
import esa.mo.nmf.apps.PropertiesManager;
import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jsat.SimpleDataSet;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import jsat.distributions.Normal;
import jsat.linear.DenseVector;

public class InferenceVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(InferenceVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // inference classifier
        vertx.eventBus().consumer(Constants.LABEL_CONSUMER_INFERENCE_CLASSIFIER, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());
            LOGGER.log(Level.INFO, "Started inference.classifier");

            // parse the Json payload
            final int expId = payload.getInteger(Constants.LABEL_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.LABEL_DATASETID).intValue();
            final JsonArray data = payload.getJsonArray(Constants.LABEL_DATA);
            final JsonArray models = payload.getJsonArray(Constants.LABEL_MODELS);

            // prepare the test data of the classifier
            ClassificationDataSet[] test = prepareClassifierTestData(data);

            // for each model 
            Iterator<Object> iter = models.iterator();
            while (iter.hasNext()) {
                JsonObject model = (JsonObject) iter.next();

                String path = model.getString(Constants.LABEL_PATH);
                String type = model.getString(Constants.LABEL_TYPE);
                // get data from the configuration
                boolean thread = PropertiesManager.getInstance().getThread();
                if (model.containsKey(Constants.LABEL_THREAD) && model.getBoolean(Constants.LABEL_THREAD) != null) {
                    thread = model.getBoolean(Constants.LABEL_THREAD);
                }

                // create the pipeline with the minimun 

                // for each test data

                    // set the full path of the model

                    // do the inference
                    
                    // store the inference in a list
            }
                    
            // retrieve the response
            
        });

    }

    private ClassificationDataSet[] prepareClassifierTestData(JsonArray data) {
        
        // variables to generate the class randomly
        Random rand = new Random();
        int classification = 2;

        // create the lists of tests
        List<ClassificationDataSet> tests = new ArrayList<ClassificationDataSet>();

        // fetch data
        data.forEach(dataset -> {
            JsonArray ds = (JsonArray) dataset;

            // total number of parameters
            int total_columns = ds.size();

            // create the test data
            double[] tempTestData = new double[total_columns];
            tests.add(new ClassificationDataSet(total_columns, new CategoricalData[0], new CategoricalData(classification)));
            int index = tests.size()-1;

            int count = 0;

            // iterate through the parameters
            Iterator<Object> iter = ds.iterator();
            while (iter.hasNext()) {
                JsonObject p = (JsonObject) iter.next();

                // store the values of the parameters
                tempTestData[count++] = Double.parseDouble(p.getString(Constants.LABEL_VALUE)); // TRAIN
            }
            
            // create the data point of the test data
            tests.get(index).addDataPoint(new DenseVector(tempTestData), new int[0], rand.nextInt(classification));

        });

        // retrieve the list of tests
        return tests.toArray(new ClassificationDataSet[0]);
    }
}
