package esa.mo.nmf.apps.saasyml.api.verticles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import esa.mo.nmf.apps.saasyml.api.Constants;
import esa.mo.nmf.apps.PropertiesManager;
import esa.mo.nmf.apps.saasyml.common.IPipeLineLayer;
import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory;
import esa.mo.nmf.apps.saasyml.factories.MLPipeLineFactory.TypeModel;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jsat.DataSet;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.ClassificationDataSet;
import jsat.linear.DenseVector;
import jsat.regression.RegressionDataSet;

public class InferenceVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(InferenceVerticle.class.getName());

    @Override
    public void start() throws Exception {

        // inference classifier
        vertx.eventBus().consumer(Constants.BASE_ADDRESS_INFERENCE, msg -> {

            try{
                // the request payload (Json)
                JsonObject payload = (JsonObject) (msg.body());
                LOGGER.log(Level.INFO, "Started " + Constants.BASE_ADDRESS_INFERENCE);

                // parse the Json payload
                final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
                final int datasetId = 1000;
                final JsonArray data = payload.getJsonArray(Constants.KEY_DATA);
                final JsonArray models = payload.getJsonArray(Constants.KEY_MODELS);

                // for efficiency we store the data in a hashmap
                HashMap<TypeModel, DataSet[]> setTestData = new HashMap<TypeModel, DataSet[]>(4);

                // for each model 
                Iterator<Object> iter = models.iterator();
                while (iter.hasNext()) {
                    JsonObject model = (JsonObject) iter.next();

                    String filePath = model.getString(Constants.KEY_FILEPATH);
                    String type = model.getString(Constants.KEY_TYPE);
                    boolean thread = PropertiesManager.getInstance().getThread();
                    if (model.containsKey(Constants.KEY_THREAD) && model.getBoolean(Constants.KEY_THREAD) != null) {
                        thread = model.getBoolean(Constants.KEY_THREAD);
                    }
                    boolean serialize = true;
                    TypeModel typeModel = TypeModel.valueOf(type);

                    // prepare and store the test data of the inference                    
                    if (!setTestData.containsKey(typeModel)) {
                        setTestData.put(typeModel, prepareOneTestData(data, typeModel));
                    }

                    // get the test data
                    DataSet[] testDataset = setTestData.get(typeModel);

                    // create the pipeline with the minimun 
                    IPipeLineLayer saasyml = MLPipeLineFactory.createPipeLine(expId, datasetId, thread, serialize, "test-" + type, typeModel);

                    // set the full path of the model
                    saasyml.setModelPathSerialized(filePath);

                    // for each test data
                    for (DataSet test : testDataset) {

                        // set the test data
                        saasyml.setDataSet(null, test);

                        // do the inference
                        List<Object> objects = saasyml.inference();

                        List<Double> inference = objects.stream()
                                .filter(element -> element instanceof Integer)
                                .map(element -> (Double) element).collect(Collectors.toList());

                        // store the inference in a list
                        // if (!model.containsKey(Constants.LABEL_INFERENCE))
                        model.put(Constants.KEY_INFERENCE, inference);

                    }
                }
                
                LOGGER.log(Level.INFO, "Stoped "+Constants.BASE_ADDRESS_INFERENCE);

                // retrieve the response
                JsonObject response = new JsonObject();
                response.put(Constants.KEY_EXPID, expId);
                response.put(Constants.KEY_MODELS, models);
                msg.reply(response);

                
            } catch (Exception e) {
                // log
                LOGGER.log(Level.SEVERE, "Failed to inference data.", e);

                // response: error
                msg.reply("Failed to inference data.");
            }
            
        });

    }

    private DataSet[] prepareOneTestData(JsonArray data, TypeModel typeModel) throws Exception {

        switch (typeModel) {
            case Classifier:
                return prepareClassifierOneTestData(data);
            case Cluster:
                return prepareClusterOneTestData(data);
            case Outlier:
                return prepareOutlierOneTestData(data);
            case Regressor:
                return prepareRegressorOneTestData(data);
            default:
            case Unknown:
                throw new Exception("There is not type of model");
        }

    }

    private DataSet[] prepareRegressorOneTestData(JsonArray data) {

        // create the lists of tests
        List<DataSet> tests = new ArrayList<DataSet>();

        int total_columns = ((JsonArray) data.getValue(0)).size();
        tests.add(new RegressionDataSet(total_columns, new CategoricalData[0]));
        int index = 0;

        // fetch data
        data.forEach(dataset -> {
            JsonArray ds = (JsonArray) dataset;

            // create the test data
            double[] tempTestData = new double[total_columns];

            int count = 0;

            // iterate through the parameters
            Iterator<Object> iter = ds.iterator();
            while (iter.hasNext()) {
                JsonObject p = (JsonObject) iter.next();

                // store the values of the parameters
                tempTestData[count++] = Double.parseDouble(p.getString(Constants.KEY_VALUE));
            }

            // create the data point of the test data
            ((RegressionDataSet) tests.get(index)).addDataPoint(
                new DenseVector(tempTestData), 
                new int[0],
                0.0);

        });        

        // retrieve the list of tests
        return tests.toArray(new DataSet[0]);
	}

	private DataSet[] prepareOutlierOneTestData(JsonArray data) {
        // create the lists of tests
        List<DataSet> tests = new ArrayList<DataSet>();
        
        // retrieve the list of tests
        return tests.toArray(new DataSet[0]);
	}

	private DataSet[] prepareClusterOneTestData(JsonArray data) {
        // create the lists of tests
        List<DataSet> tests = new ArrayList<DataSet>();

        // retrieve the list of tests
        return tests.toArray(new DataSet[0]);
	}

	private DataSet[] prepareClassifierOneTestData(JsonArray data) {
        // variables to generate the class randomly
        Random rand = new Random();
        int classification = 1;

        // create the lists of tests
        List<DataSet> tests = new ArrayList<DataSet>();

        int total_columns = ((JsonArray) data.getValue(0)).size();
        tests.add(new ClassificationDataSet(total_columns, new CategoricalData[0], new CategoricalData(classification)));
        int index = 0;

        // fetch data
        data.forEach(dataset -> {
            JsonArray ds = (JsonArray) dataset;

            // create the test data
            double[] tempTestData = new double[total_columns];

            int count = 0;

            // iterate through the parameters
            Iterator<Object> iter = ds.iterator();
            while (iter.hasNext()) {
                JsonObject p = (JsonObject) iter.next();

                // store the values of the parameters
                tempTestData[count++] = Double.parseDouble(p.getString(Constants.KEY_VALUE));
            }

            // create the data point of the test data
            ((ClassificationDataSet) tests.get(index)).addDataPoint(new DenseVector(tempTestData), new int[0],
                    rand.nextInt(classification));

        });

        // retrieve the list of tests
        return tests.toArray(new DataSet[0]);
    }

    private DataSet[] prepareClassifierManyTestData(JsonArray data) {
        
        // variables to generate the class randomly
        Random rand = new Random();
        int classification = 2;

        // create the lists of tests
        List<DataSet> tests = new ArrayList<DataSet>();

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
                tempTestData[count++] = Double.parseDouble(p.getString(Constants.KEY_VALUE)); 
            }
            
            // create the data point of the test data
            ((ClassificationDataSet)tests.get(index)).addDataPoint(new DenseVector(tempTestData), new int[0], rand.nextInt(classification));

        });

        // retrieve the list of tests
        return tests.toArray(new DataSet[0]);
    }
}
