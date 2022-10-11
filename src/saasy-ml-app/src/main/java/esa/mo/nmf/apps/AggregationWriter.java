package esa.mo.nmf.apps;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import esa.mo.nmf.apps.saasyml.api.utils.Pair;
import esa.mo.nmf.apps.saasyml.plugins.api.ExpectedLabels;
import esa.mo.nmf.commonmoadapter.CompleteAggregationReceivedListener;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.pf4j.PluginManager;

public class AggregationWriter implements CompleteAggregationReceivedListener {    

    // logger
    private static final Logger LOGGER = Logger.getLogger(AggregationWriter.class.getName());

    // the vertx object
    private Vertx vertx;

    // the plugin manager
    private PluginManager pluginManager;

    // the requested plugin
    private ExpectedLabels plugin;

    // services
    private ArchiveConsumerServiceImpl archiveService;
    private ParameterConsumerServiceImpl paramService;
    

    // make inaccessible the default constructor
    private AggregationWriter(){}

    // constructor
    public AggregationWriter(Vertx vertx, PluginManager pluginManager) { 
        try{
            this.vertx = vertx;
            this.pluginManager = pluginManager;

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

            // get the label plugin classpath
            String labelPlugin = ApplicationManager.getInstance().getLabelPlugin(expId, datasetId);

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

            // create map of param name and double values in case we need it for the plugin
            Map<String, Double> pluginParamInputMap = new HashMap<String, Double>();

            for(int i = 0; i < paramNames.size(); i++) {

                if(labelPlugin != null) {
                    try {
                        // populate extension input map
                        pluginParamInputMap.put(paramNames.get(i), new Double(paramValues.get(i).getValue()));
                    } catch (Exception e) {
                        labelPlugin = null;
                        LOGGER.log(Level.SEVERE, "The expected labels plugin cannot be invoked because the fetched parameter values are uncastable to the Double type", e);
                    }  
                }                              

                // populate the Json array
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

            if(!pluginParamInputMap.isEmpty()){
                if(labelPlugin != null){
    
                    // retrieve the extensions for Expected Labels extension point
                    List<ExpectedLabels> expectedLabelsPlugins = pluginManager.getExtensions(ExpectedLabels.class);
                    //LOGGER.log(Level.INFO, "Found " + expectedLabelsPlugins.size() + " extensions for expected labels.");
    
                    // pick the extension specified by the request
                    for (ExpectedLabels p : expectedLabelsPlugins) {
                        if(p.getClass().getCanonicalName().equals(labelPlugin)){
                            //LOGGER.log(Level.INFO, "Fetched the following plugin: " + p.getClass().getCanonicalName());
                            this.plugin = p;
                            break;
                        }
                    }
    
                    // check if requested extension was found
                    if(this.plugin == null){
                        if(ApplicationManager.getInstance().getLabels(expId, datasetId) != null){
                            ApplicationManager.getInstance().getLabels(expId, datasetId).clear();
                        }
                        LOGGER.log(Level.SEVERE, "Could not retrieve plugin extension " + labelPlugin + ". The fetched training data will be persisted without expected labels.");
                    } else {
                        // if requested plugin was found then set the label
                        Map<String, Boolean> expectedLabelsMap = this.plugin.getLabels(pluginParamInputMap);
                        ApplicationManager.getInstance().addLabels(expId, datasetId, expectedLabelsMap);
                    }
                }
            }
            
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
