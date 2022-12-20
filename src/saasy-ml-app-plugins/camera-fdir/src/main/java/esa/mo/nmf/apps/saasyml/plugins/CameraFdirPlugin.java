package esa.mo.nmf.apps.saasyml.plugins;

import java.util.Map;
import java.util.HashMap;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.pf4j.Extension;

import java.util.logging.Level;
import java.util.logging.Logger;

import esa.mo.nmf.apps.saasyml.plugins.api.ExpectedLabels;

public class CameraFdirPlugin extends Plugin {

    // Logger
    private static final Logger LOGGER = Logger.getLogger(CameraFdirPlugin.class.getName());

    public CameraFdirPlugin(PluginWrapper wrapper) {
        super(wrapper);
        // you can use "wrapper" to have access to the plugin context (plugin manager, descriptor, ...)
    }

    /**
     * This extension calculates the expected label for the classification training described in Section 5
     * of the following publication (subsection: Training a FDIR Model): 
     * 
     * https://ieeexplore.ieee.org/document/9843402
     * 
     * Open access available here: 
     * 
     * https://www.researchgate.net/publication/362628759_OPS-SAT_Spacecraft_Autonomy_with_TensorFlow_Lite_Unsupervised_Learning_and_Online_Machine_Learning
     * 
     * The spacecraft is equipped with 6 photodiodes that measurethe sun elevation angles on each surface.
     * 
     * These measurements range from 0° to 90°. The former indicates nosunlight on the surface and the 
     * latter direct sunlight with thesun positioned at the zenith with respect to the surface fromwhich 
     * the photodiode measurement is taken.
     * 
     * The photodiodeIds as well as their location on the spacecraft and their datapool parameter names are:
     *  
     *  (Photodiode Id, OPS-SAT bodyframe surface, Datapool parameter name) =
     *      PD1, +X, CADC0884
     *      PD2, -X, CADC0886
     *      PD3, +Y, CADC0888
     *      PD4, -Y, CADC0890
     *      PD5, +Z, CADC0892
     *      PD6, -Z, CADC0894 
     */
    @Extension
    public static class CameraStateLabels implements ExpectedLabels {

        // The PD6 elevation angle is set in the CADC0894 parameter and given in gradient.
        private static final String PARAM_NAME_PD6 = "CADC0894";

        /**
         * Photodiode elevation threshold for the HD Camera: FOV 18.63 deg (in lens specs) and 21 deg (in ICD).
         * Elevation threshold is 90 deg - (FOV + margin) = 60 deg (1.0472 rad).
         * Photodiode elevation angle measured above this threshold indicate that the camera lens is exposed to sunlight.
         */
        private static final double PD6_ELEVATION_THRESHOLD_HD_CAM = 1.0472f;

        /** 
         * Returns the labels ON and OFF associated to a PD6 elevation for the HD camera located on the -Z surface.
         * We want to turn the camera OFF when the lens is exposed to sunlight.
         * We want to keep the camera ON when the lens is not exposed to sunlight.
         *      Label 0 represents OFF.
         *      Label 1 represents ON.
         */
        public Map<String, Boolean> getLabels(Map<String, Double> params) {

            // return null if the expected paramete
            if(!params.containsKey(PARAM_NAME_PD6)){
                LOGGER.log(Level.WARNING, "No expected labels created: the given parameter is an invalid input.");
                return null;
            }

            // the label map that we will set and return
            Map<String, Boolean> labelMap = new HashMap<String, Boolean>();

            // if elevation is greater than the threshold: camera should be OFF
            if(params.get(PARAM_NAME_PD6).doubleValue() > PD6_ELEVATION_THRESHOLD_HD_CAM){
                labelMap.put("0", true);
                labelMap.put("1", false);
            } else {
                // if elevation is not greater than the threshold: camera should be ON
                labelMap.put("0", false);
                labelMap.put("1", true);
            }
            return labelMap;
        }
    }    
}
