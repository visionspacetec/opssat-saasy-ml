# Plugins Implementations

This module collects class implementations for interfaces defined in [saasy-ml-app-plugins-api](../saasy-ml-app-plugins-api). 

## Expected Labels Interface
SaaSyML users must implement classes in this module that extends the [`ExpectedLabels`](https://github.com/visionspacetec/opssat-saasy-ml/blob/main/src/saasy-ml-app-plugins-api/src/main/java/esa/mo/nmf/apps/saasyml/plugins/api/ExpectedLabels.java) interface in [saasy-ml-app-plugins-api](../saasy-ml-app-plugins-api/) to train classification models with expected label that are determined by custom logic executed while training data is being fetched.

## Example
This module contains the [`CameraStateLabels`](https://github.com/visionspacetec/opssat-saasy-ml/blob/main/src/saasy-ml-app-plugins/camera-fdir/src/main/java/esa/mo/nmf/apps/saasyml/plugins/CameraStateLabels.java) class as a sample implementation of the [`ExpectedLabels`](https://github.com/visionspacetec/opssat-saasy-ml/blob/main/src/saasy-ml-app-plugins-api/src/main/java/esa/mo/nmf/apps/saasyml/plugins/api/ExpectedLabels.java) interface. This reference replicates the [OrbitAI](https://github.com/georgeslabreche/opssat-orbitai) experiment:

```java
@Extension
public class CameraStateLabels implements ExpectedLabels {

    // The PD6 elevation angle is set in the CADC0894 parameter and given in gradient.
    private static final String PARAM_NAME_PD6 = "CADC0894";

    /**
     * Photodiode elevation threshold for the HD Camera: FOV 18.63 deg (in lens specs) and 21 deg (in ICD).
     * Elevation threshold is 90 deg - (FOV + margin) = 60 deg (1.0472 rad).
     * Photodiode elevation angle measured above this threshold indicate that the camera lens is exposed to sunlight.
     */
    private static final float PD6_ELEVATION_THRESHOLD_HD_CAM = 1.0472f;

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
            return null;
        }

        // the label map that we will set and return
        Map<String, Boolean> labelMap = new HashMap<String, Boolean>();

        // if elevation is greater than the threshold: camera should be OFF
        if(params.get(PARAM_NAME_PD6) > PD6_ELEVATION_THRESHOLD_HD_CAM){
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
```