#!/bin/sh
echo "Running test train classifier models no thread"

sleep 1


# dataset Id 1
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"LogisticRegressionDCD\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"LinearBatch\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"PassiveAggressive\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"BBR\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"ALMA2K\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"algorithm\" : \"NewGLMNET\" }" &

# dataset id 2
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"LogisticRegressionDCD\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"LinearBatch\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"PassiveAggressive\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"BBR\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"ALMA2K\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"algorithm\" : \"NewGLMNET\" }" &

# dataset id 3
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"LogisticRegressionDCD\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"LinearBatch\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"PassiveAggressive\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"BBR\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"ALMA2K\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"algorithm\" : \"NewGLMNET\" }" &

# dataset id 4
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"LogisticRegressionDCD\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"LinearBatch\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"PassiveAggressive\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"BBR\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"ALMA2K\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"algorithm\" : \"NewGLMNET\" }" &

# dataset id 5
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"LogisticRegressionDCD\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"LinearBatch\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"PassiveAggressive\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"BBR\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"ALMA2K\" }" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/classifier/ -payload "{ \"expId\": 213, \"datasetId\": 5, \"algorithm\" : \"NewGLMNET\" }" &
