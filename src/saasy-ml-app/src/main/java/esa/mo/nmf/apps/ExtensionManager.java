package esa.mo.nmf.apps;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import esa.mo.nmf.apps.saasyml.plugins.api.ExpectedLabels;


public class ExtensionManager {

    private static final Logger LOGGER = Logger.getLogger(ExtensionManager.class.getName());

    private static volatile ExtensionManager instance;
    private static Object mutex = new Object();

    // the plugin manager
    private PluginManager pluginManager;

    // hide the constructor
    private ExtensionManager() {
        // load the plugins
        Path pluginsDir = Paths.get("plugins");
        this.pluginManager = new DefaultPluginManager(pluginsDir);
    }

    public static ExtensionManager getInstance() {
        // the local variable result seems unnecessary but it's there to improve performance
        // in cases where the instance is already initialized (most of the time), the volatile field is only accessed once (due to "return result;" instead of "return instance;").
        // this can improve the method’s overall performance by as much as 25 percent.
        // source: https://www.journaldev.com/171/thread-safety-in-java-singleton-classes
        ExtensionManager result = instance;
        
        // enforce Singleton design pattern
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new ExtensionManager();
            }
        }
        
        // return singleton instance
        return result;
    }

    public synchronized Map<String, Boolean> getExpectedLabels(int expId, int datasetId, Map<String, Double> extensionInputMap){
        ExpectedLabels extension = null;

        // get the label plugin classpath
        String extensionClasspath = ApplicationManager.getInstance().getExtentionClasspath(expId, datasetId);

        // retrieve the extensions for Expected Labels extension point
        List<ExpectedLabels> expectedLabelsPlugins = this.pluginManager.getExtensions(ExpectedLabels.class);
        //LOGGER.log(Level.INFO, "Found " + expectedLabelsPlugins.size() + " extensions for expected labels.");

        // pick the extension specified by the request
        for (ExpectedLabels p : expectedLabelsPlugins) {
            if(p.getClass().getCanonicalName().equals(extensionClasspath)){
                //LOGGER.log(Level.INFO, "Fetched the following plugin: " + p.getClass().getCanonicalName());
                extension = p;
                break;
            }
        }

        // check if requested extension was found
        /**
        if(extension == null){
            if(ApplicationManager.getInstance().getLabels(expId, datasetId) != null){
                ApplicationManager.getInstance().getLabels(expId, datasetId).clear();
            }
            LOGGER.log(Level.SEVERE, "Could not retrieve plugin extension " + extensionClasspath + ". The fetched training data will be persisted without expected labels.");
            
        } else {
            // if requested plugin was found then return the expected label
            return extension.getLabels(extensionInputMap);
        } */

        if(extension != null) {
            return extension.getLabels(extensionInputMap);
        }

        return null;
    }

    public void startPlugins(){
        // load and start the plugins
        this.pluginManager.loadPlugins();
        this.pluginManager.startPlugins();
    }

    public void stopPlugins(){
        // stop and unload the plugins
        pluginManager.stopPlugins();
        pluginManager.unloadPlugins();
    }
    
}
