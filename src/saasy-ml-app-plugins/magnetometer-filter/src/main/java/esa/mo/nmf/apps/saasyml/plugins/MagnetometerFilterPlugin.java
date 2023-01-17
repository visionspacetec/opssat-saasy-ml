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
 * with magnetic field values that are not within a preset range.
 * 
 * Two ExpectedLabels classes are implemented:
 *  - MagnetometerFilterMagneticFieldSingleAxis: used to train a model in 1D input space for a single magnetic field axis as training input (X, Y, or Z).
 *  - MagnetometerFilterMagneticFieldAllAxes: used to train a model in 3D input space with all magnetic field axes as training input (X, Y, and Z).
 */
public class MagnetometerFilterPlugin extends Plugin {

    // Logger
    private static final Logger LOGGER = Logger.getLogger(MagnetometerFilterPlugin.class.getName());

    /**
     * We want to filter out magnetometer measurements with magnetic field values that are either:
     *  - lesser than or equal to -5e-5.
     *  - greater than or equal to 5e-5.
     */
    private static final double MAGNETOMETER_FILTER_THRESHOLD = 5.0E-5;

    // The datapool parameters for the measured magnetometer values.
    private static final String[] MAGNETOMETER_PARAM_NAMES = new String[] {
        "CADC0872", // Magnetic Field X-Axis.
        "CADC0873", // Magnetic Field Y-Axis.
        "CADC0874"  // Magnetic Field Z-Axis.
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
     * Magnetometer measurements is discarded if one of the magnetic field values is x >= 5e-5 or x <= -5e-5.
     */
    @Extension
    public static class MagnetometerFilterMagneticFieldSingleAxis implements ExpectedLabels {

        /** 
         * Returns a "0" or "1" label indicating whether the given magnetic field axis value
         * has a value that is beyond the threshold value (x >= 5e-5 or x <= -5e-5).
         * 
         *      Label 0 means keep the measurement (-5e-5 < x < 5e-5 ).
         *      Label 1 means filter out the measurement (x >= 5e-5 or x <= -5e-5).
         */
        public Map<String, Boolean> getLabels(Map<String, Double> params) {

            // We expect only a single value representing ONE magnetic field axis measurement (X, Y, or Z).
            if(params.size() != 1){
                LOGGER.log(Level.WARNING, "No expected labels created: only one magnetic field axis measurement is expected as input.");
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
            

            // Get magnetic field axis param name and value.
            Map.Entry<String, Double> entry = params.entrySet().iterator().next();
            String magneticFieldAxisParamName = entry.getKey(); // Not used.
            double magneticFieldAxis = entry.getValue().doubleValue();

            // The expected label map that we will set and return.
            Map<String, Boolean> labelMap = new HashMap<String, Boolean>();            

            // Return the "0" label if measurements need to be kept.
            if(Math.abs(magneticFieldAxis) >= MAGNETOMETER_FILTER_THRESHOLD) {
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
     * Magnetometer measurements are discarded if at least one magnetic field axis value is x >= 5e-5 or x <= -5e-5.
     */
    @Extension
    public static class MagnetometerFilterMagneticFieldAllAxes implements ExpectedLabels {

        /** 
         * Returns a "0" or "1" label indicating whether or not at least one of the three magnetomer
         * measurements contains a value that is beyond the threshold value (x >= 5e-5 or x <= -5e-5).
         * 
         *      Label 0 means keep the measurement (-5e-5 < x < 5e-5 ).
         *      Label 1 means filter out the measurement (x >= 5e-5 or x <= -5e-5).
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

                // Get the magnetic field axis value
                double magneticFieldAxisValue = params.get(paramName).doubleValue();

                // Return the "1" label if measurements need to be filtered out.
                if(Math.abs(magneticFieldAxisValue) >= MAGNETOMETER_FILTER_THRESHOLD){
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
