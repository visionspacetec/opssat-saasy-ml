package esa.mo.nmf.apps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ccsds.moims.mo.mal.structures.LongList;

import esa.mo.nmf.apps.saasyml.api.utils.Pair;

public class ApplicationManager {

    private static volatile ApplicationManager instance;
    private static Object mutex = new Object();

    // map to track parameter names for the requested data
    // we need this because the response object received in the onReceivedData listener does not reference the parameter names
    private Map<Pair<Integer, Integer>, List<String>> paramNamesMap;

    // map to track parameter ids for the requested data
    // we need this because the response object received in the onReceivedData listener does not reference the parameter ids
    private Map<Pair<Integer, Integer>, LongList> paramIdsMap;

    // map to track the label (expected output) for the requested data
    private Map<Pair<Integer, Integer>, Map<String, Boolean>> labelMap;

    // map to track the label pluglins
    private Map<Pair<Integer, Integer>, String> extensionClasspathMap;
    
    // map that contains all instances of aggregation handlers
    private Map<Pair<Integer, Integer>, AggregationHandler> aggregationHandlerMap;


    // hide the constructor
    private ApplicationManager() {
        this.paramNamesMap = new ConcurrentHashMap<Pair<Integer, Integer>, List<String>>();
        this.paramIdsMap = new ConcurrentHashMap<Pair<Integer, Integer>, LongList>();
        this.labelMap = new ConcurrentHashMap<Pair<Integer, Integer>, Map<String, Boolean>>();
        this.extensionClasspathMap = new ConcurrentHashMap<Pair<Integer, Integer>, String>();
        this.aggregationHandlerMap = new ConcurrentHashMap<Pair<Integer, Integer>, AggregationHandler>();
    }

    public static ApplicationManager getInstance() {
        // the local variable result seems unnecessary but it's there to improve performance
        // in cases where the instance is already initialized (most of the time), the volatile field is only accessed once (due to "return result;" instead of "return instance;").
        // this can improve the method’s overall performance by as much as 25 percent.
        // source: https://www.journaldev.com/171/thread-safety-in-java-singleton-classes
        ApplicationManager result = instance;
        
        // enforce Singleton design pattern
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new ApplicationManager();
            }
        }
        
        // return singleton instance
        return result;
    }

    public void addParamNames(int expId, int datasetId, List<String> paramNames) {
        this.paramNamesMap.put(new Pair<Integer, Integer>(expId, datasetId), paramNames);
    }

    public List<String> getParamNames(int expId, int datasetId) {
        return this.paramNamesMap.get(new Pair<Integer, Integer>(expId, datasetId));
    }

    public void addParamIds(int expId, int datasetId, LongList paramIds) {
        this.paramIdsMap.put(new Pair<Integer, Integer>(expId, datasetId), paramIds);
    }

    public LongList getParamIds(int expId, int datasetId) {
        return this.paramIdsMap.get(new Pair<Integer, Integer>(expId, datasetId));
    }

    public void addLabels(int expId, int datasetId, Map<String, Boolean> labelMap) {
        this.labelMap.put(new Pair<Integer, Integer>(expId, datasetId), labelMap);
    }

    public Map<String, Boolean> getLabels(int expId, int datasetId) {
        return this.labelMap.get(new Pair<Integer, Integer>(expId, datasetId));
    }

    public synchronized void addExtensionClasspath(int expId, int datasetId, String extensionClasspath) {
        this.extensionClasspathMap.put(new Pair<Integer, Integer>(expId, datasetId), extensionClasspath);
    }

    public String getExtentionClasspath(int expId, int datasetId) {
        Pair<Integer, Integer> tokenToFind = new Pair<Integer, Integer>(expId, datasetId);
        if (!this.extensionClasspathMap.containsKey(tokenToFind))
            return null;
        String extensionClasspath = this.extensionClasspathMap.get(tokenToFind);
        return extensionClasspath.equals("null")?null:extensionClasspath;
    }

    public void addAggregationHandler(int expId, int datasetId, AggregationHandler aggregationHandler) {
        this.aggregationHandlerMap.put(new Pair<Integer, Integer>(expId, datasetId), aggregationHandler);
    }

    public void removeAggregationHandler(int expId, int datasetId) {
        aggregationHandlerMap.remove(new Pair<Integer, Integer>(expId, datasetId));
    }

    public AggregationHandler getAggregationHandler(int expId, int datasetId) {
        return this.aggregationHandlerMap.get(new Pair<Integer, Integer>(expId, datasetId));
    }

    public void enableSupervisorParametersSubscription(int expId, int datasetId, boolean enable) throws Exception {
        Pair<Integer, Integer> id = new Pair<Integer, Integer>(expId, datasetId);
        if(this.aggregationHandlerMap.containsKey(id)){
            this.aggregationHandlerMap.get(id).enableSupervisorParametersSubscription(enable);
        }
    }

    public AggregationHandler createAggregationHandler(int expId, int datasetId, double interval, List<String> paramNameList, boolean subscribeToFeed) throws Exception{
        // the key to access the aggregation handler from the map
        Pair<Integer, Integer> id = new Pair<Integer, Integer>(expId, datasetId);

        // create aggregation handler if it doesn't already exist
        if(!this.aggregationHandlerMap.containsKey(id)){
            
            AggregationHandler aggregationHandler = new AggregationHandler(expId, datasetId, interval, paramNameList);

            // subscribe or unsubscribe to the parameter feed
            aggregationHandler.enableSupervisorParametersSubscription(subscribeToFeed);
            
            // put aggregation handler in map
            this.aggregationHandlerMap.put(id, aggregationHandler);
        }

        // return the aggregation handler
        return this.aggregationHandlerMap.get(id);
    }
}    