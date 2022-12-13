# Plugins Interfaces

Defines _Interfaces_ that concrete plugin classes must implement. Currently, the plugin architecture supports implementing user-defined logic that outputs expected labels when training classification models.

## Plugin Framework
A plugin is a way for a third party to extend the functionality of an application by implementing extension points declared as standardized interfaces. Adopting the [Plugin Framework for Java (PF4J)](https://pf4j.org/) thus shifts application development from producing a monolithic systems to embracing modularity. Plugins have their own independent development life-cycle and can be loaded by an application during runtime without a redeployment. PF4J is used in SaaSyML to run custom supervised learning algorithms that determine the expected labels associated with a given training dataset.

## Expected Labels
Supervised learning uses labeled datasets to train algorithms that classify data or predict outcomes. Pairing training data with expected labels is thus a requirement for SaaSyML's classification algorithms. The app cannot determine these labels as they must be provided by the users while training data is collected. A common plugin interface is provided that defines how the plugin logic can receive the parameter inputs that it requires as well as how the labels should be returned as outputs. The plugin logic is executed for every iteration of fetched trained data and the returned label is stored in the application's database. A plugin is packaged as a .jar or .zip file, deployed to a dedicated plugins directory, and loaded by SaaSyML during runtime.

The [`ExpectedLabels`](https://github.com/visionspacetec/opssat-saasy-ml/blob/main/src/saasy-ml-app-plugins-api/src/main/java/esa/mo/nmf/apps/saasyml/plugins/api/ExpectedLabels.java) interface defines a simple `getLabels` method which takes a Map of parameter values as an argument: 

```java
import org.pf4j.ExtensionPoint;
import java.util.Map;

public interface ExpectedLabels extends ExtensionPoint {
    public Map<String, Boolean> getLabels(Map<String, Double> params);
}
```

## Future Work
The flexibility of the plugin architecture suggests a future refactor of SaaSyML so that integrating [JSAT](https://github.com/EdwardRaff/JSAT) training algorithms is plugin-based rather the monolithically coupled in the application [ML pipeline](../saasy-ml-layer-pipeline/). This would facilitate iterative integration of [JSAT](https://github.com/EdwardRaff/JSAT) training algorithms and enable inclusion of other third-party ML libraries.