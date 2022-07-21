# OPS-SAT SaaSyML App

An NMF App for the OPS-SAT spacecraft. The app uses ML to train AI models with the spacecraft's OBSW datapool parameters as training data. 

# Table of Content

- [Requirements](#requirements)
- [Quick Install](#quick-install)
- [Long Install](#long-install)
- [Run App](#run-app)
- [Known Issue](#known-issue)
- [API](#api)
- [References](#references)

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

1. Clone the SaaSyML App and NMF repositories

    ```shell
    $ git clone https://github.com/visionspacetec/opssat-saasy-ml.git
    $ git clone https://github.com/visionspacetec/opssat-saasy-ml-nanosat-mo-framework.git
    $ cd opssat-saasy-ml/src/saasy-ml-app
    ```

2. Copy **build.bat.template** to a new **build.bat** file and modify the <FULL_PATH> to match the environment

    ```powershell
    :: Set variables
    SET PROJECT_DIR=<FULL_PATH>\opssat-saasy-ml\src\saasy-ml-app
    SET NMF_SDK_PACKAGE_DIR=<FULL_PATH>\opssat-saasy-ml-nmf\sdk\sdk-package
    ```

3. Modify the **con/config.properties** file to set the desired app configurations.

4. Modify the **sdk/sdk-package/pom.xml** copy instruction to match the environment's location

    ```xml
    <copy todir="${esa.nmf.sdk.assembly.outputdir}/home/saasy-ml">
      <fileset dir="${basedir}/src/main/resources/space-common"/>
      <fileset dir="${basedir}/src/main/resources/space-app-root"/>
      <fileset dir="${basedir}/../../../opssat-saasy-ml/src/saasy-ml-app/conf"/>
    </copy>
    ```

5. Execute **./build.bat** to build all the Apps or **./build.bat 1** to build the Apps and execute the Supervisor and CTT.


## Long Install

1. Install the SaaSyML App

    ```shell
    $ git clone https://github.com/visionspacetec/opssat-saasy-ml.git
    $ cd opssat-saasy-ml/src/saasy-ml-app
    $ mvn install
    $ cd .. && cd .. 
    ```

2. Install NMF

    ```shell
    $ git clone https://github.com/visionspacetec/opssat-saasy-ml-nanosat-mo-framework.git
    $ cd opssat-saasy-ml-nanosat-mo-framework
    $ mvn install
    ```

    If in step 2 the app was cloned to a different folder name than the default **opssat-saasy-ml**, then the following copy configuration in **sdk/sdk-package/pom.xml** must be updated:

    ```xml
    <copy todir="${esa.nmf.sdk.assembly.outputdir}/home/saasy-ml">
    <fileset dir="${basedir}/src/main/resources/space-common"/>
    <fileset dir="${basedir}/src/main/resources/space-app-root"/>
    <fileset dir="${basedir}/../../../opssat-saasy-ml/src/saasy-ml-app/conf"/>
    </copy>
    ```

3. Deploy the SaaSyML App

    ```shell
    $ cd sdk/sdk-package/
    $ mvn install
    ```

4. Supervisor and CTT

    Open a second terminal window to run both the Supervisor and the Consumer Test Tool (CTT).

    The Supervisor:

    ```shell
    $ cd target/nmf-sdk-2.1.0-SNAPSHOT/home/nmf/nanosat-mo-supervisor-sim
    $ ./nanosat-mo-supervisor-sim.sh 
    ```

    - The Supervisor outputs a URI on the console.
    - This URI follows the pattern `maltcp://<SUPERVISOR_HOST>:<SUPERVISOR_PORT>/nanosat-mo-supervisor-Directory`.

    The CTT:
    ```shell
    $ cd target/nmf-sdk-2.1.0-SNAPSHOT/home/nmf/consumer-test-tool
    $ ./consumer-test-tool.sh
    ```

## Run App

- Paste the URI given by the Supervisor into the **Communication Settings** field of the CTT.
- Click the **Fetch information** button.
- Click the **Connect to Selected Provider** button.
- A new tab appears: **nanosat-mo-supervisor**. 
- Select the **saasy-ml** app under the **Apps Launcher Servce" table.
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

Make several of these requests with different values for `expId`, `datasetId`, `interval`, and `params`. The fetched values will persist in a sqlite database file configured in the config.properties file. To auto-trigger training the model(s) as soon as the target dataset iterations has been met:

```json
{
    "expId": 123,
    "datasetId": 1,
    "iterations": 10,
    "interval": 2,
    "params": ["GNC_0005", "GNC_0011", "GNC_0007"],
    "training": [
        {
            "type": "classifier",
            "group": "bayesian",
            "algorithm": "aode",
            "thread" : false
        },
        {
            "type": "classifier",
            "group": "boosting",
            "algorithm": "bagging"
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

### Send training data
In some cases, clients generate their own training data to send to the app to train a model:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/data/save
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "data": [
        [
            {
                "name": "PARAM_10",
                "value": "1001",
                "dataType": 1,
                "timestamp": 1656803525000
            },
            {
                "name": "PARAM_20",
                "value": "2001",
                "dataType": 1,
                "timestamp": 1656803525000
            },
            {
                "name": "PARAM_30",
                "value": "3001",
                "dataType": 1,
                "timestamp": 1656803525000
            }
        ],
        [
            {
                "name": "PARAM_10",
                "value": "1002",
                "dataType": 1,
                "timestamp": 1656804525000
            },
            {
                "name": "PARAM_20",
                "value": "2002",
                "dataType": 1,
                "timestamp": 1656804525000
            },
            {
                "name": "PARAM_30",
                "value": "3002",
                "dataType": 1,
                "timestamp": 1656804525000
            }
        ],
        [
            {
                "name": "PARAM_10",
                "value": "1003",
                "dataType": 1,
                "timestamp": 1656805525000
            },
            {
                "name": "PARAM_20",
                "value": "2003",
                "dataType": 1,
                "timestamp": 1656805525000
            },
            {
                "name": "PARAM_30",
                "value": "3003",
                "dataType": 1,
                "timestamp": 1656805525000
            }
        ]
    ],
    "training": [
        {
            "type": "classifier",
            "group": "bayesian",
            "algorithm": "aode"
        },
        {
            "type": "classifier",
            "group": "boosting",
            "algorithm": "bagging"
        }
    ]
}
```

The training parameter is optional and used to auto-trigger training the model(s).

### Delete data

Make an POST request to the following endpoint:

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

### Train a model

Make an POST request to the following endpoints:

```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/:type/:group/:algorithm
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1
}
```


```
http://<SUPERVISOR_HOST>:<APP_PORT>/api/v1/training/:type/
```

Sample payload:

```json
{
    "expId": 123,
    "datasetId": 1,
    "group": "baye", 
    "algorithm": "RandomForest"
}
```

## References

- [The NMF quick start guide](https://nanosat-mo-framework.readthedocs.io/en/latest/quickstart.html)
- [The NMF deployment guide](https://nanosat-mo-framework.readthedocs.io/en/latest/apps/packaging.html)
- [Vert.x Core Manual](https://vertx.io/docs/vertx-core/java/)