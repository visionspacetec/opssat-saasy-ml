# Plugins Implementations
This module collects class implementations for interfaces defined in [saasy-ml-app-plugins-api](../saasy-ml-app-plugins-api). SaaSyML users must implement classes in this module that extends the [`ExpectedLabels`](https://github.com/visionspacetec/opssat-saasy-ml/blob/main/src/saasy-ml-app-plugins-api/src/main/java/esa/mo/nmf/apps/saasyml/plugins/api/ExpectedLabels.java) interface in [saasy-ml-app-plugins-api](../saasy-ml-app-plugins-api/) to train classification models with expected label that are determined by custom logic executed while training data is being fetched.

## Getting Started
Read [the documentation](https://pf4j.org/doc/getting-started.html) on getting started with the Plugin Framework for Java (PF4J).

## Instructions
To implement and run new plugins:
1. Create a new module directory as the plugin directory.
2. Implement the plugin class(es) and the necessary files under the plugin directory.
3. List the module in the `saasy-ml-app-plugins` parent pom.xml.
4. Run `mvn clean install` from the `saasy-ml-app-plugins` parent directory.
5. Copy the `x-all.jar` build artifacts into [`saasy-ml-app/plugins`](../saasy-ml-app/plugins/).
6. Build and run `saasy-ml-app`.
7. Set the `labelsPlugin` JSON payload property when invoking the API requests to enable the training data feed subscription or to save training data provided by the user.

## Example API Request
The following custom expected label plugins are available as reference implementations:
- [Camera FDIR Plugin](camera-fdir). 
- [Magnetometer Filter Plugin](magnetometer-filter).