# SaaSy ML Pipeline

The machine learning pipeline that invokes JSAT algorithms.

## Install 

From within ```saasy-ml-layer-pipeline```:

```shell
$ mvn install
```

The jar application will be generated in the **target** folder.

## Use

From within ```saasy-ml-layer-pipeline\target```:


```shell
"$ java -jar saasyml-pipeline-0.1.0-SNAPSHOT.jar -thread [1 | true] -serialize [1 | true] -tests [1 2 3 4 | Classifier Cluster Outlier Regressor]
```

where: 

- thread: to use threads in the pipeline.
- serialize: to serialize the models.
- tests: builds, trains, and performs inference with a model. 

Available model options are **Classifier**, **Cluster**, **Outlier**, and **Regressor**:

- Classifier: builds and tests a **LogisticRegressionDCD** model.
- Cluster: builds and tests a **FLAME** model.
- Outlier: builds and tests a **IsolationForest** model.
- Regressor: builds and tests a **MultipleLinearRegression** model.

For more details about the algorithms, check the JSAT API documentation at http://www.edwardraff.com/jsat_docs/JSAT-0.0.8-javadoc/.

To change the algorithms used in the tests, modify ```SaaSyMLPipeLineTest```.

The following is an example to build and test a regressor model:

```shell
$ java -jar saasyml-pipeline-0.1.0-SNAPSHOT.jar -tests 4
```

A **logs** folder, with **.log** files, will be created inside the folder from where the command above is run. 