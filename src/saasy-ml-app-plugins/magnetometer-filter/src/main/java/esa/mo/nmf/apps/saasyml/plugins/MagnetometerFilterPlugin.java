package esa.mo.nmf.apps.saasyml.plugins;

import java.util.Map;
import java.util.HashMap;
import java.lang.Math;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.pf4j.Extension;

import esa.mo.nmf.apps.saasyml.plugins.api.ExpectedLabels;

/**
 * This plugn is to train a binary classification mode that filters out magnetometer measurements 
 * with diple moments that are not within a preset threshold.
 * 
 * Two ExpectedLabels classes are implemented:
 *  - MagnetometerSingleDipoleMomentFilter: used to train a model in 1D input space for a single dipole moment training input (x, y, OR z).
 *  - MagnetometerAllDipoleMomentFilter: used to train a model in 3D input space with all dipole moments as traing input (x, y, AND z).
 */
public class MagnetometerFilterPlugin extends Plugin {

    // Logger
    private static final Logger LOGGER = Logger.getLogger(MagnetometerFilterPlugin.class.getName());

    /**
     * We want to filter out magnetometer measurements with diple moments that are either:
     *  - lesser than or equal to -5e-5.
     *  - greater than or equal to 5e-5.  
     */
    private static final double MAGNETOMETER_FILTER_THRESHOLD = 5.0E-5;

    // The datapool parameters for the measured magnetometer values.
    private static final String[] MAGNETOMETER_PARAM_NAMES = new String[] {
        "CADC0872", // Dipole Moment X.
        "CADC0873", // Dipole Moment Y.
        "CADC0874"  // Dipole Moment Z.
    };
    
    /**
     * Plugin constructor.
     */
    public MagnetometerFilterPlugin(PluginWrapper wrapper) {
        super(wrapper);
        // you can use "wrapper" to have access to the plugin context (plugin manager, descriptor, ...)
    }

    /**
     * Determine the expected labels on whether or not given magnetometer measurements need to be discarded or not.
     * Magnetometer measurements is discarded if one of the dipole moment's value is M >= 5e-5 or M <= -5e-5.
     */
    @Extension
    public static class MagnetometerFilterSingleDipoleMoment implements ExpectedLabels {

        /** 
         * Returns a "0" or "1" label indicating whether or the given single dipole moment magnetometer
         * measurements has a value that is beyond the threshold value (M >= 5e-5 or M <= -5e-5).
         * 
         *      Label 0 means keep the measurement (-5e-5 < M < 5e-5 ).
         *      Label 1 means filter out the measurement (M >= 5e-5 or M <= -5e-5).
         */
        public Map<String, Boolean> getLabels(Map<String, Double> params) {

            // We expect only a single value representing ONE dipole measurement (x, y, or z).
            if(params.size() != 1){
                LOGGER.log(Level.WARNING, "No expected labels created: only one dipole moment measurement is expected as input.");
                return null;
            }

            // Return null if the expected training data is not given as the plugin's input params.
            boolean valid = false;
            for (String paramName : MAGNETOMETER_PARAM_NAMES) {
                if(params.containsKey(paramName)){
                    valid = true;
                    break;
                }
            }

            if(!valid){
                LOGGER.log(Level.WARNING, "No expected labels created: the given parameter is an invalid input.");
                return null;
            }
            

            // Get dipole moment param name and value.
            Map.Entry<String, Double> entry = params.entrySet().iterator().next();
            String dipoleMomentParamName = entry.getKey(); // Not used.
            double dipoleMomentValue = entry.getValue().doubleValue();

            // The expected label map that we will set and return.
            Map<String, Boolean> labelMap = new HashMap<String, Boolean>();            

            // Return the "0" label if measurements need to be kept.
            if(Math.abs(dipoleMomentValue) >= MAGNETOMETER_FILTER_THRESHOLD) {
                labelMap.put("0", false);
                labelMap.put("1", true);

            } else {
                // Return the "1" label if measurements need to be filtered out.
                labelMap.put("0", true);
                labelMap.put("1", false);
            }

            // Return expected labels.
            return labelMap;
        }
    }

    /**
     * Determine the expected labels on whether or not given magnetometer measurements need to be discarded or not.
     * Magnetometer measurements are discarded if at least one of dipole moment value is x >= 5e-5 or x <= -5e-5.
     */
    @Extension
    public static class MagnetometerFilterAllDipoleMoment implements ExpectedLabels {

        /** 
         * Returns a "0" or "1" label indicating whether or not at least one of the three magnetomer
         * measurements contains a value that is beyond the threshold value (M >= 5e-5 or M <= -5e-5).
         * 
         *      Label 0 means keep the measurement (-5e-5 < M < 5e-5 ).
         *      Label 1 means filter out the measurement (M >= 5e-5 or M <= -5e-5).
         */
        public Map<String, Boolean> getLabels(Map<String, Double> params) {

            // Return null if the expected training data is not given as the plugin's input params.
            for (String paramName : MAGNETOMETER_PARAM_NAMES) {
                if(!params.containsKey(paramName)){
                    LOGGER.log(Level.WARNING, "No expected labels created: the '" + paramName + "' parameter is an invalid input.");
                    return null;
                }
            }

            // The expected label map that we will set and return.
            Map<String, Boolean> labelMap = new HashMap<String, Boolean>();

            // Set the expected label map based on parameter values.
            for (String paramName : MAGNETOMETER_PARAM_NAMES) {

                // Get the dipole moment value.
                double dipoleMomentValue = params.get(paramName).doubleValue();

                // Return the "1" label if measurements need to be filtered out.
                if(Math.abs(dipoleMomentValue) >= MAGNETOMETER_FILTER_THRESHOLD){
                    labelMap.put("0", false);
                    labelMap.put("1", true);
                    
                    // Return expected labels.
                    return labelMap;
                }
            }

            // Return the "0" label if measurements need to be kept.
            labelMap.put("0", true);
            labelMap.put("1", false);

            // Return expected labels.
            return labelMap;
        }
    }
}
