package esa.mo.nmf.apps;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

import org.ccsds.moims.mo.mc.aggregation.structures.AggregationParameterValueList;
import org.ccsds.moims.mo.mc.parameter.ParameterHelper;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterValue;

import esa.mo.com.impl.consumer.ArchiveConsumerServiceImpl;
import esa.mo.com.impl.provider.ArchivePersistenceObject;
import esa.mo.com.impl.util.HelperArchive;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.mc.impl.consumer.ParameterConsumerServiceImpl;
import esa.mo.mc.impl.provider.AggregationInstance;
import esa.mo.nmf.apps.saasyml.api.Constants;
import esa.mo.nmf.commonmoadapter.CompleteAggregationReceivedListener;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class AggregationWriter implements CompleteAggregationReceivedListener {    

    // logger
    private static final Logger LOGGER = Logger.getLogger(AggregationWriter.class.getName());

    // the vertx object
    private Vertx vertx;

    // services
    private ArchiveConsumerServiceImpl archiveService;
    private ParameterConsumerServiceImpl paramService;

    // make inaccessible the default constructor
    private AggregationWriter(){}

    // constructor
    public AggregationWriter(Vertx vertx) { 
        try{
            this.vertx = vertx;
            this.archiveService = AppMCAdapter.getInstance().getSupervisorSMA().getCOMServices().getArchiveService();
            this.paramService = AppMCAdapter.getInstance().getSupervisorSMA().getMCServices().getParameterService();

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
                
        // fetch and persist param values
        try {  
            // get experiment id
            int expId = Utils.getExpIdFromAggId(aggId);

            // get dataset id
            int datasetId = Utils.getDatasetIdFromAggId(aggId);

            // get the parameter names
            List<String> paramNames = ApplicationManager.getInstance().getParamNames(expId, datasetId);
        
            // get aggregation timestamp and parameter values
            Long timestamp = aggregationInstance.getTimestamp().getValue();
            List<Pair<Integer, String>> paramValues = getParameterValues(expId, datasetId, aggregationInstance);

            // the payload json object that will be parsed to persist the training data into the database
            JsonObject payload = new JsonObject();
            payload.put("expId", expId);
            payload.put("datasetId", datasetId);

            // build the params array
            JsonArray params = new JsonArray();

            for(int i = 0; i < paramNames.size(); i++){
                JsonObject param = new JsonObject();
                param.put("name", paramNames.get(i));
                param.put("value", paramValues.get(i).getValue());
                param.put("dataType", paramValues.get(i).getKey());
                param.put("timestamp", timestamp);
                params.add(param);
            }

            // need to wrap everything in an array because that's what the database verticle expects
            JsonArray data = new JsonArray();
            data.add(params);

            // put the training data array into the payload json object
            payload.put("data", data);
            
            // send the payload to the database verticle
            this.vertx.eventBus().send(Constants.ADDRESS_DATA_SAVE, payload);   

        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching and writing parameters for aggregation " + aggId + ".", e);
        }
    }
    
    public List<Pair<Integer, String>> getParameterValues(int expId, int datasetId, AggregationInstance aggregationInstance) {

        // the list that will contain all the param values
        List<Pair<Integer, String>> paramValues = new ArrayList<Pair<Integer, String>>();

        // the aggregration param value list
        AggregationParameterValueList aggParamValueList =
                aggregationInstance.getAggregationValue().getParameterSetValues().get(0).getValues();

        // populate the list that will be returned
        for (int i = 0; i < aggParamValueList.size(); i++) {
            ParameterValue paramValue = aggParamValueList.get(i).getValue();
            
            try {
                ArchivePersistenceObject comObject = HelperArchive.getArchiveCOMObject(
                    this.archiveService.getArchiveStub(),
                    ParameterHelper.PARAMETERDEFINITION_OBJECT_TYPE, 
                    this.paramService.getConnectionDetails().getDomain(), 
                    ApplicationManager.getInstance().getParamIds(expId, datasetId).get(i));

                if(comObject != null){
                    // get the data type
                    ParameterDefinitionDetails pDef = (ParameterDefinitionDetails) comObject.getObject();

                    // get the string value of the parameter
                    String paramValueStr = HelperAttributes.attribute2string(paramValue.getRawValue());

                    // add to list a key-value pair
                    paramValues.add(new Pair<Integer, String>(new Integer(pDef.getRawType().intValue()), paramValueStr));

                }else{
                    LOGGER.log(Level.SEVERE, "Failed to fetch the parameter COM object, training data will not be persisted.");
                }

            } catch (Exception e){
                LOGGER.log(Level.WARNING, "Failed to fetch the parameter data type, training data will not be persisted.", e);
            }

        }

        // return the list
        return paramValues;
    }
}
