package esa.mo.nmf.apps;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;


import org.ccsds.moims.mo.mc.aggregation.structures.AggregationParameterValueList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;

import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.mc.impl.provider.AggregationInstance;
import esa.mo.nmf.commonmoadapter.CompleteAggregationReceivedListener;



public class AggregationWriter implements CompleteAggregationReceivedListener {    

    // logger
    private static final Logger LOGGER = Logger.getLogger(AggregationWriter.class.getName());

    // constructor
    public AggregationWriter() { 
        try{
            // connect to the database
            DatabaseManager.getInstance().connect();

        } catch(Exception e) {
            // log error
            LOGGER.log(Level.SEVERE, "Error initializing database connection: fetched training data will not persist.", e);
        }
    }

    
    @Override
    public void onDataReceived(AggregationInstance aggregationInstance) {

        // check that aggregation instance is not null
        if (aggregationInstance == null) {
            LOGGER.log(Level.WARNING, "Received null aggregation instance.");
            return;
        }

        // get aggregation id
        String aggId = aggregationInstance.getName().getValue();

        // check that connection exists
        if(DatabaseManager.getInstance().getConnection() == null) {
            LOGGER.log(Level.WARNING, "No database connection for for aggregation " + aggId + ": fetched training data will not persist."); 
            return;
        }

        // check that connection is opened
        try {
            if(DatabaseManager.getInstance().getConnection().isClosed()) {
                LOGGER.log(Level.WARNING, "Database connection is closed for for aggregation " + aggId + ": fetched training data will not persist.");
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while checking if connection is closed.", e);
            return;
        }
        
                
        // fetch and persist param values
        try {  
            // get experiment id
            int expId = Utils.getExpIdFromAggId(aggId);

            // get dataset id
            int datasetId = Utils.getDatasetIdFromAggId(aggId);

            List<String> paramNames = ApplicationManager.getInstance().getParamNames(expId, datasetId);

            // if the data counter/rank is not currently set but it has already been set and incremented from a previous run
            // then fetch the latest counter value from the database so we can resume where we left off
            if(ApplicationManager.getInstance().getReceivedDataCounter(expId, datasetId) < 0) {

                // fetch last counter value from db
                int lastCounter = DatabaseManager.getInstance().getLastCounterValue(expId, datasetId);

                // set the lat counter value for the given experiment id and dataset id
                ApplicationManager.getInstance().setReceivedDataCounter(expId, datasetId, lastCounter);
            }

            // increment the counter assoaciated with this fresh training dataset we just received
            int receivedDataCounter = ApplicationManager.getInstance().incrementReceivedDataCounter(expId, datasetId);
        
            // get aggregation timestamp and parameter values
            Long timestamp = aggregationInstance.getTimestamp().getValue();
            List<Pair<Integer, String>> paramValues = getParameterValues(aggregationInstance);

            // insert training data values into database
            DatabaseManager.getInstance().insertTrainingData(expId, datasetId, paramNames, receivedDataCounter, paramValues, timestamp);

        

        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching and writing parameters for aggregation " + aggId + ".", e);
        }
    }
    
    public List<Pair<Integer, String>> getParameterValues(AggregationInstance aggregationInstance) {

        // the list that will contain all the param values
        List<Pair<Integer, String>> paramValues = new ArrayList<Pair<Integer, String>>();

        // the aggregration param value list
        AggregationParameterValueList aggParamValueList =
                aggregationInstance.getAggregationValue().getParameterSetValues().get(0).getValues();

        // populate the list that will be returned
        for (int i = 0; i < aggParamValueList.size(); i++) {
            ParameterValue paramValue = aggParamValueList.get(i).getValue();
            
            // get the parameter type short form and the string value
            Integer paramTypeShortForm = paramValue.getTypeShortForm();
            String paramValueStr = HelperAttributes.attribute2string(paramValue.getRawValue());

            // add to list a key-value pair
            paramValues.add(new Pair<Integer, String>(paramTypeShortForm, paramValueStr));
        }

        // return the list
        return paramValues;
    }
}
