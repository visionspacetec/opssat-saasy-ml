# Magnetometer Filter Plugin
- Magnetometer measurements on-board OPS-SAT have historically contained spikes that need to be discarded.
- We can train classification models so to detect these outliers.
- The datapool parameters used as training data are `CADC0872`, `CADC0873`, and `CADC0874` for magnetic field values measured by the cADCS' magnetometer.

## Historical Reference
- Sample useful range of dates would be September 2021 to March 2022.
- The most days without any spikes were around the second half of November.
- Generally the spikes we bother filtering out are those that are not within the `-5e-5 < x < 5e-5` range.

![Magnetometer Measurements](references/magnetometer_measurements.png)

## Sample Requests
The Magnetometer Filter Plugin is used to train a model in either the 1D input space or 3D input space using as training data the magnetometer's magnetic field measurements. The following API request is invoked to enable the training data feed subscription:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/subscribe
```

The `labelsPlugin` payload property is set to the `MagnetometerFilterMagneticFieldSingleAxis` or `MagnetometerFilterMagneticFieldAllAxes` extension point implementation in the `MagnetometerFilterPlugin` plugin class to determine and return the expected labels:
- `MagnetometerFilterMagneticFieldSingleAxis`: used to train a model in 1D input space for a single magnetic field axis as training input (X, Y, *or* Z).
- `MagnetometerFilterMagneticFieldAllAxes`: used to train a model in 3D input space with all magnetic field axes as training input (X, Y, *and* Z).

### X-Axis
Enable a training data feed in the 1D input space with the magnetic field's X-axis values fetched from the `CADC0872` datapool parameter:

```json
{
    "expId": 1,
    "datasetId": 1,
    "iterations": 100,
    "interval": 2,
    "labelsPlugin": "esa.mo.nmf.apps.saasyml.plugins.MagnetometerFilterPlugin.MagnetometerFilterMagneticFieldSingleAxis",
    "params": ["CADC0872"]
}
```

### Y-Axis
Enable a training data feed in the 1D input space with the magnetic field's Y-axis values fetched from the `CADC0873` datapool parameter:

```json
{
    "expId": 2,
    "datasetId": 1,
    "iterations": 1800,
    "interval": 2,
    "labelsPlugin": "esa.mo.nmf.apps.saasyml.plugins.MagnetometerFilterPlugin.MagnetometerFilterMagneticFieldSingleAxis",
    "params": ["CADC0873"]
}
```

### Z-Axis
Enable a training data feed in the 1D input space with the magnetic field's Z-axis values fetched from the `CADC0874` datapool parameter:

```json
{
    "expId": 3,
    "datasetId": 1,
    "iterations": 1800,
    "interval": 2,
    "labelsPlugin": "esa.mo.nmf.apps.saasyml.plugins.MagnetometerFilterPlugin.MagnetometerFilterMagneticFieldSingleAxis",
    "params": ["CADC0874"]
}
```

### X-, Y-, and Z-Axis
Enable a training data feed in the 3D input space with the magnetic field's X-, Y-, and Z-axis values fetched from the `CADC0872`, `CADC0873`, and `CADC0874` datapool parameters:

```json
{
    "expId": 4,
    "datasetId": 1,
    "iterations": 1800,
    "interval": 2,
    "labelsPlugin": "esa.mo.nmf.apps.saasyml.plugins.MagnetometerFilterPlugin.MagnetometerFilterMagneticFieldAllAxes",
    "params": ["CADC0872", "CADC0873", "CADC0874"]
}
```
