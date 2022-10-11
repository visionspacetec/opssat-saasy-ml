#!/bin/sh
echo "Running test train classifier models no thread"

sleep 1

# experimenter id 213

# dataset Id 1
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"formatToInference\": false }" -output "$1/$2/train/results-download-classifier-models-E213-D1.json" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"formatToInference\": true }" -output "$1/$2/train/results-download-classifier-models-toinference-E213-D1.json" &


# dataset id 2
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"formatToInference\": false }" -output "$1/$2/train/results-download-classifier-models-E213-D2.json" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"formatToInference\": true }" -output "$1/$2/train/results-download-classifier-models-toinference-E213-D2.json" &


# dataset id 3
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"formatToInference\": false }" -output "$1/$2/train/results-download-classifier-models-E213-D3.json" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"formatToInference\": true }" -output "$1/$2/train/results-download-classifier-models-toinference-E213-D3.json" &


# dataset id 4
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"formatToInference\": false }" -output "$1/$2/train/results-download-classifier-models-E213-D4.json" &
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/download/models/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"formatToInference\": true }" -output "$1/$2/train/results-download-classifier-models-toinference-E213-D4.json" &
