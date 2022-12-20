# Camera FDIR Plugin
This module contains a sample plugin implementation of the [`ExpectedLabels`](https://github.com/visionspacetec/opssat-saasy-ml/blob/main/src/saasy-ml-app-plugins-api/src/main/java/esa/mo/nmf/apps/saasyml/plugins/api/ExpectedLabels.java) interface that replicates the [OrbitAI](https://github.com/georgeslabreche/opssat-orbitai) experiment.

## Sample Requests
The Camera FDIR Plugin is used to train a model in the 6D input space using as training data the photodiode measurements captured at each surface of the spacecraft. The following API request is invoked to enable the training data feed subscription:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/subscribe
```

The `labelsPlugin` payload property is set to the  `CameraStateLabels` extension point implementation in the `CameraFdirPlugin` plugin class to determine and return the expected labels:

```json
{
  "expId": 123,
  "datasetId": 1,
  "iterations": 10,
  "interval": 2,
  "labelsPlugin": "esa.mo.nmf.apps.saasyml.plugins.CameraFdirPlugin.CameraStateLabels",
  "params": ["CADC0884", "CADC0886", "CADC0888", "CADC0890", "CADC0892", "CADC0894"]
}
```

## References
The following are references to the OrbitAI experiment.

### Plain Text
Labrèche, G., Evans, D., Marszk, D., Mladenov, T., Shiradhonkar, V., Soto, T., & Zelenevskiy, V. (2022). OPS-SAT Spacecraft Autonomy with TensorFlow Lite, Unsupervised Learning, and Online Machine Learning]. _2022 IEEE Aerospace Conference (AERO)_, 2022, pp. 1-17, doi: [10.1109/AERO53065.2022.9843402](https://ieeexplore.ieee.org/document/9843402).

### BibTex
```bibtex
@INPROCEEDINGS{9843402,
  author={Labrèche, Georges and Evans, David and Marszk, Dominik and Mladenov, Tom and Shiradhonkar, Vasundhara and Soto, Tanguy and Zelenevskiy, Vladimir},
  booktitle={2022 IEEE Aerospace Conference (AERO)}, 
  title={OPS-SAT Spacecraft Autonomy with TensorFlow Lite, Unsupervised Learning, and Online Machine Learning}, 
  year={2022},
  volume={},
  number={},
  pages={1-17},
  doi={10.1109/AERO53065.2022.9843402}}
```