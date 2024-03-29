# OPS-SAT SaaSyML App
- The SaaSyML NMF App for the OPS-SAT spacecraft.
- The app uses ML to train AI models.
- Via API requests, training data can be sent by experimenters or fetched from the spacecraft's OBSW datapool.

## Table of Content

- [Motivation](#motivation)
- [References](#references)
- [Requirements](#requirements)
- [Quick Install](#quick-install)
- [Run App](#run-app)
- [Known Issue](#known-issue)
- [Training Data Types](training-data-types)
- [API](#api)

## Motivation

The OPS-SAT spacecraft has accomplished many firsts in space powered by open source and rapid prototyping, from the first securities trade to pioneering the use of Artificial Intelligence (AI) frameworks for on-board Machine Learning (ML) training and inferences with payload and telemetry data. The SaaSyML app presented in this project is an effort to consolidate the broad interest in running ML experiments on a spacecraft. Experimenters can use SaaSyML to subscribe to a stream of hand-picked in-flight data which are persisted as training data in an on-board database. Once enough training data is collected, users can trigger pre-implemented ML training algorithms to produce prediction models. This model can then be used to run predictions / inferences with new data inputs. SaaSyML is a Software as a Service (SaaS) app by design that is openly accessible to experiments through an Application Programming Interface (API), either from the ground station via telecommand or from their own apps running on the spacecraft. Traditional SaaS is a way of delivering an application's capabilities over the Internet — as a service. Instead of installing and maintaining their own software, users become clients who can remotely access the functionalities they need from existing third-party hosted software thus avoiding the complexities and costs of developing, hosting, and maintaining their own systems. The Web's "as-a-Service" ecosystem has rapidly matured in recent years with the developments of options such as Infrastructure as a Service (IaaS), Platform as a Service (PaaS), and Everything as a Service (XaaS). As a technology demonstrator, the SaaSyML experiment proposes to introduce this approach to software in the space segment thus contributing the concept of Satellite Platform as a Service (SaaS) to the ``as-a-Service".

## References

- [The NMF quick start guide.](https://nanosat-mo-framework.readthedocs.io/en/latest/quickstart.html)
- [The NMF deployment guide.](https://nanosat-mo-framework.readthedocs.io/en/latest/apps/packaging.html)
- [Vert.x Core Manual.](https://vertx.io/docs/vertx-core/java/)

## Requirements

- Java 8
- Maven 3.X.X

Tested environment on Windows 10:
```powershell
Apache Maven 3.8.1 
Java version: 1.8.0_291, vendor: Oracle Corporation
Default locale: en_US, platform encoding: Cp1252
OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"
```

Tested environment on Ubuntu 18.04.5 on Windows WSL:
```sh
Apache Maven 3.8.4 
Java version: 1.8.0_312, vendor: Private Build
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "5.10.16.3-microsoft-standard-wsl2", arch: "amd64", family: "unix"
```

## Quick Install

1. Clone the SaaSyML App and NMF repositories under the same parent directory:

    ```shell
    $ git clone https://github.com/visionspacetec/opssat-saasy-ml.git
    $ git clone https://github.com/visionspacetec/opssat-saasy-ml-nanosat-mo-framework.git
    ```

2. Compile the nanosat mo framework

    ```shell
    $ cd opssat-saasy-ml-nanosat-mo-framework/
    $ mvn install
    $ cd sdk/sdk-package/
    $ mvn install
    ```

3. Check athat the **sdk/sdk-package/pom.xml** copy instruction to match the local environment's file system:

    ```xml
    <!-- SaaSyML: resource and config files -->
    <copy todir="${esa.nmf.sdk.assembly.outputdir}/home/saasy-ml">
      <fileset dir="${basedir}/src/main/resources/space-common"/>
      <fileset dir="${basedir}/src/main/resources/space-app-root"/>
      <fileset dir="${basedir}/../../../opssat-saasy-ml/src/saasy-ml-app/conf"/>
    </copy>
    <!-- SaaSyML: create plugins directory and copy the plugin archives into it -->
    <mkdir dir="${esa.nmf.sdk.assembly.outputdir}/home/saasy-ml/plugins"/>
    <copy todir="${esa.nmf.sdk.assembly.outputdir}/home/saasy-ml/plugins">
      <fileset dir="${basedir}/../../../opssat-saasy-ml/src/saasy-ml-app/plugins">
        <include name="*.jar"/>
        <include name="*.zip"/>
      </fileset>
    </copy>
    ```

    And add SaaSy-ML as dependency:

    ```xml
    <!-- SaaSyML-->
    <dependency>
      <groupId>int.esa.nmf.apps</groupId>
      <artifactId>saasy-ml-app</artifactId>
      <version>${project.version}</version>
    </dependency>
    ```

3. Create the build and run files of SaaSy ML

    ```shell
    $ cd ../../../opssat-saasy-ml/src/saasy-ml-app
    ```

    Copy **build.bat.template** to a new **build.bat** file and **run.bat.template** to a new **run.bat** and modify the <FULL_PATH> in both files to match the environment. 

    ```powershell
    :: Set variables
    SET PROJECT_DIR=<FULL_PATH>\opssat-saasy-ml\src\saasy-ml-app
    SET NMF_SDK_PACKAGE_DIR=<FULL_PATH>\opssat-saasy-ml-nmf\sdk\sdk-package
    ```

4. Modify the **con/config.properties** file to set the desired app configurations.

5. Execute **./build.bat** to build all the Apps or **./build.bat 1** to build the Apps and execute the Supervisor and CTT, or **./run.bat** to only execute the Supervisor and CTT, or **./run.bat 1** to remove the Database and execute the Supervisor and CTT.


## Run App

- Paste the URI given by the Supervisor into the **Communication Settings** field of the CTT.
- Click the **Fetch information** button.
- Click the **Connect to Selected Provider** button.
- A new tab appears: **nanosat-mo-supervisor**. 
- Select the **saasy-ml** app under the *Apps Launcher Servce* table.
- Click the **runApp** button.


## Known Issue
Note: examples in this section are in PowerShell.

Situation: The App did not shutdown gracefully despite terminating the Supervisor and the CTT. 
Problem: Attempting to repeat installation step #3 to redeploy the app will result in a locked file error, e.g.:

```powershell
Failed to execute goal org.apache.maven.plugins:maven-dependency-plugin:3.1.0:copy-dependencies (copy-dependencies) on project package: Error copying artifact from C:\Users\honeycrisp\.m2\repository\com\tanagraspace\nmf\apps\saasy-ml\2.1.0-SNAPSHOT\saasy-ml-2.1.0-SNAPSHOT.jar to C:\Users\honeycrisp\Dev\Tanagra\ESA\opssat\saasy-ml\opssat-saasy-ml-nmf\sdk\sdk-package\target\nmf-sdk-2.1.0-SNAPSHOT\home\nmf\lib\saasy-ml-2.1.0-SNAPSHOT.jar: C:\Users\honeycrisp\Dev\Tanagra\ESA\opssat\saasy-ml\opssat-saasy-ml-nmf\sdk\sdk-package\target\nmf-sdk-2.1.0-SNAPSHOT\home\nmf\lib\saasy-ml-2.1.0-SNAPSHOT.jar: The process cannot access the file because it is being used by another process. -> [Help 1]
```

In this case, use the jps command to identify the process id of the culprit process (i.e. the SaaSyMLApp java process):

```powershell
> jps
55880 org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar
95404 SaaSyMLApp
150140 Jps
```

Force kill the process, e.g. in Windows Terminal:

```powershell
> taskkill /F /PID 95404
SUCCESS: The process with PID 95404 has been terminated.
```

Check that the process was indeed killed:

```powershell
> jps
111924 Jps
55880 org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar
```

Now the App can be redeployed.

## Training Data Types
Training data are stored into the database as Strings. This is so that different data types can be persisted. A `data_type` column records the parameter's type with an integer id representing a specific type.

| **Id** | **MAL Type** | **Java Type**        |
|--------|--------------|----------------------|
| 2      | Boolean      | Boolean              |
| 3      | Duration     | double               |
| 4      | Float        | Float                |
| 5      | Double       | Double               |
| 7      | Octet        | Byte                 |
| 8      | UOctet       | short                |
| 9      | Short        | Short                |
| 10     | UShort       | int                  |
| 11     | Integer      | Integer              |
| 12     | UInteger     | long                 |
| 13     | Long         | Long                 |
| 14     | ULong        | java.math.BigInteger |
| 16     | Time         | long                 |
| 17     | FineTime     | long                 |


Early versions of this service will treat all stored parameter values as doubles when training the models. As a reference, see `HelperAttributes.attribute2double(Attribute in)` in package `esa.mo.helpertools.helpers`.

## API
Ad-hoc documentation of the app's API endpoints.

### Subscribe to a training data feed

Use an API platform like [Postman](https://www.postman.com/) to make an POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/subscribe
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "iterations": 10,
    "interval": 2,
    "params": ["GNC_0005", "GNC_0011", "GNC_0007"]
}
```

**IMPORTANT**: The `iterations` parameter is used to signal when to stop the feed, i.e. as soon as the total count of fetched data stored in the database is equal or greater to `iterations` (this includes counting data fetched during past requests).

Sample payload with label values (expected output) provided by the client:

```json
{
    "expId": 123,
    "datasetId": 1,
    "iterations": 10,
    "interval": 2,
    "labels": {
        "0": true,
        "1": false,
        "2": false
    },
    "params": ["GNC_0005", "GNC_0011", "GNC_0007"]
}
```

Sample payload with label values calculated by a plugin with given fetched parameters:

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

Make several of these requests with different values for `expId`, `datasetId`, `interval`, and `params`. The fetched values will persist in a sqlite database file configured in the config.properties file. To auto-trigger training the model(s) as soon as the target dataset iterations has been met:

```json
{
    "expId": 123,
    "datasetId": 1,
    "iterations": 10,
    "interval": 2,
    "labels": {
        "0": false,
        "1": true,
        "2": false
    },
    "params": ["GNC_0005", "GNC_0011", "GNC_0007"],
    "training": [
        {
            "type": "classifier",
            "algorithm": "LogisticRegressionDCD",
            "thread" : false
        },
        {
            "type": "classifier",
            "algorithm": "AROW"
        }
    ]
}
```

Note that the `thread` attribute in the `training` is optional. It allows to train the models with threads for multi-core. 

### Unsubscribe to a training data feed

Unsubscribe to the data feed with a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/unsubscribe
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1
}
```

### Download the training data 

Download the training data with a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/download
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1
}
```

### Send training data
In some cases, clients generate their own training data to send to the app to train a model:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/save
```

Sample payload, note that:
 - The training parameter is optional and used to auto-trigger training the model(s).
 - The timestamps are optional and will be generated if not included.

```json
{
    "expId": 123,
    "datasetId": 1,
    "data": [
        [
            {
                "name": "PARAM_10",
                "value": "1001",
                "dataType": 11,
                "timestamp": 1656803525000
            },
            {
                "name": "PARAM_20",
                "value": "2001",
                "dataType": 11,
                "timestamp": 1656803525000
            },
            {
                "name": "PARAM_30",
                "value": "3001",
                "dataType": 11,
                "timestamp": 1656803525000
            }
        ],
        [
            {
                "name": "PARAM_10",
                "value": "1002",
                "dataType": 11,
                "timestamp": 1656804525000
            },
            {
                "name": "PARAM_20",
                "value": "2002",
                "dataType": 11,
                "timestamp": 1656804525000
            },
            {
                "name": "PARAM_30",
                "value": "3002",
                "dataType": 11,
                "timestamp": 1656804525000
            }
        ],
        [
            {
                "name": "PARAM_10",
                "value": "1003",
                "dataType": 11,
                "timestamp": 1656805525000
            },
            {
                "name": "PARAM_20",
                "value": "2003",
                "dataType": 11,
                "timestamp": 1656805525000
            },
            {
                "name": "PARAM_30",
                "value": "3003",
                "dataType": 11,
                "timestamp": 1656805525000
            }
        ]
    ],
    "training": [
        {
            "type": "classifier",
            "algorithm": "LogisticRegressionDCD"
        },
        {
            "type": "classifier",
            "algorithm": "AROW"
        }
    ]
}
```

Sample payload with label values (expected output) provided by the client:

```json
{
    "expId": 123,
    "datasetId": 1,
    "labels": {
        "0": false,
        "1": false,
        "2": true
    },
    "data": [
        [
            {
                "name": "PARAM_10",
                "value": "1001",
                "dataType": 11,
                "timestamp": 1656803525000
            },
            {
                "name": "PARAM_20",
                "value": "2001",
                "dataType": 11,
                "timestamp": 1656803525000
            },
            {
                "name": "PARAM_30",
                "value": "3001",
                "dataType": 11,
                "timestamp": 1656803525000
            }
        ],
        [
            {
                "name": "PARAM_10",
                "value": "1002",
                "dataType": 11,
                "timestamp": 1656804525000
            },
            {
                "name": "PARAM_20",
                "value": "2002",
                "dataType": 11,
                "timestamp": 1656804525000
            },
            {
                "name": "PARAM_30",
                "value": "3002",
                "dataType": 11,
                "timestamp": 1656804525000
            }
        ],
        [
            {
                "name": "PARAM_10",
                "value": "1003",
                "dataType": 11,
                "timestamp": 1656805525000
            },
            {
                "name": "PARAM_20",
                "value": "2003",
                "dataType": 11,
                "timestamp": 1656805525000
            },
            {
                "name": "PARAM_30",
                "value": "3003",
                "dataType": 11,
                "timestamp": 1656805525000
            }
        ]
    ],
    "training": [
        {
            "type": "classifier",
            "algorithm": "LogisticRegressionDCD"
        },
        {
            "type": "classifier",
            "algorithm": "AROW"
        }
    ]
}
```

Sample payload with the label extension provided by the client:

```json
{
    "expId": 123,
    "datasetId": 2,
    "labelsPlugin": "esa.mo.nmf.apps.saasyml.plugins.CameraFdirPlugin.CameraStateLabels",
    "data": [
        [
            {
                "name": "CADC0894",
                "value": "0.0",
                "dataType": 4,
                "timestamp": 1656803525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.1",
                "dataType": 4,
                "timestamp": 1656804525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.2",
                "dataType": 4,
                "timestamp": 1656805525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.2",
                "dataType": 4,
                "timestamp": 1656806525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.3",
                "dataType": 4,
                "timestamp": 1656807525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.4",
                "dataType": 4,
                "timestamp": 1656808525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.5",
                "dataType": 4,
                "timestamp": 1656809525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.6",
                "dataType": 4,
                "timestamp": 1656810525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.7",
                "dataType": 4,
                "timestamp": 1656811525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.8",
                "dataType": 4,
                "timestamp": 1656812525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "0.9",
                "dataType": 4,
                "timestamp": 1656813525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "1.0",
                "dataType": 4,
                "timestamp": 1656814525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "1.1",
                "dataType": 4,
                "timestamp": 1656815525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "1.2",
                "dataType": 4,
                "timestamp": 1656816525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "1.3",
                "dataType": 4,
                "timestamp": 1656817525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "1.4",
                "dataType": 4,
                "timestamp": 1656818525000
            }
        ],
        [
            {
                "name": "CADC0894",
                "value": "1.5",
                "dataType": 4,
                "timestamp": 1656819525000
            }
        ]
    ]
}
```


### Delete data

Make a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/delete
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1
}
```

### Train a classification or regression model

Make a POST request to the following endpoints:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/:type/:algorithm
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1
}
```


```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/classifier
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "algorithm": "AROW"
}
```

### Perform clustering

Make a POST request to the following endpoints:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/cluster/:algorithm
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "clusterNumber": 2
}
```


```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/cluster
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "algorithm": "GMeans",
    "clusterNumber": 2
}
```


**TODO:** filter out parameters from training.

### Fetch Models Metadata
To fetch which trained models are available for a given experiment and dataset pair, make a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/download/models
```

```json
{
    "expId": 123,
    "datasetId": 1,
    "formatToInference": true
}
```

Note that the `formatToInference` attribute is optional. It formats a valid output for inference. 


### Inference
Inferences can be made using input values directly provided by the experiment or by creating and subscribing to a datapool parameter feed. The latter will store the inference results in the app's database.

#### Input provided by experimenters
Make a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/inference
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "data": [
        [
            {
                "name": "GNC_0005",
                "value": "1001",
                "dataType": 11
            },
            {
                "name": "GNC_0011",
                "value": "2001",
                "dataType": 11
            },
            {
                "name": "GNC_0007",
                "value": "3001",
                "dataType": 11
            }
        ],
        [
            {
                "name": "GNC_0005",
                "value": "1002",
                "dataType": 11
            },
            {
                "name": "GNC_0011",
                "value": "2002",
                "dataType": 11
            },
            {
                "name": "GNC_0007",
                "value": "3002",
                "dataType": 11
            }
        ]
    ],
    "models": [
        {
            "filepath": "<FULL-PATH-OF-SERIALIZED-THE-MODEL>",
            "type": "Classifier",
            "thread" : true
        },
        {
            "filepath": "<FULL-PATH-OF-SERIALIZED-THE-MODEL>",
            "type": "Classifier"
        }
    ]

}
```

#### Input from datapool parameter feed

Make a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/inference/subscribe
```

```json
{
    "expId": 123,
    "datasetId": 3,
    "iterations": 10,
    "interval": 2,
    "params": ["CADC0884", "CADC0886", "CADC0888", "CADC0890", "CADC0892", "CADC0894"],
    "models": [
        {
            "filepath": "<FULL-PATH-OF-SERIALIZED-MODEL>",
            "type": "Classifier",
            "thread" : true
        },
        {
            "filepath": "<FULL-PATH-OF-SERIALIZED-MODEL>",
            "type": "Classifier"
        }
    ]
}
```

Unsubscribe to the inference feed with a POST request to the following endpoint:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/inference/unsubscribe
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 3
}
```

### Clustering Inference

Request for clustering inference does not require inference data.
The response of clustering inference contains the cluster assignment for observation in the train data set.

```json
{
    "expId": 123,
    "datasetId": 1,
    "models": [
        {
            "filepath": "<FULL-PATH-OF-SERIALIZED-MODEL>",
            "type": "Cluster",
            "thread" : true
        },
        {
            "filepath": "<FULL-PATH-OF-SERIALIZED-MODEL>",
            "type": "Cluster"
        }
    ]

}
```
