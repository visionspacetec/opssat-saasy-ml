#!/bin/sh
echo "Running test train classifier models with thread"

sleep 1


# datasetId 1
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"LogisticRegressionDCD\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"LinearBatch\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"PassiveAggressive\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"BBR\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"ALMA2K\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"NewGLMNET\", \"thread\" : true }" &

# dataset id 2
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"LogisticRegressionDCD\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"LinearBatch\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"PassiveAggressive\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"BBR\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"ALMA2K\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"NewGLMNET\", \"thread\" : true }" &

# dataset id 3
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"LogisticRegressionDCD\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"LinearBatch\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"PassiveAggressive\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"BBR\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"ALMA2K\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"NewGLMNET\", \"thread\" : true }" &

# dataset id 4
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"LogisticRegressionDCD\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"LinearBatch\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"PassiveAggressive\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"BBR\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"ALMA2K\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"NewGLMNET\", \"thread\" : true }" &

# dataset id 5
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"LogisticRegressionDCD\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"LinearBatch\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"PassiveAggressive\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"BBR\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"ALMA2K\", \"thread\" : true }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"NewGLMNET\", \"thread\" : true }" &


