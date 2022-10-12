#!/bin/sh
echo "Running test download train data"

sleep 1

# datasetId 1
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/training/data/download/ -payload "{ \"expId\": 213, \"datasetId\": 1 }" -output "$1/$2/subscribe/results-download-E213-D1.json" &

# dataset id 2
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/training/data/download/ -payload "{ \"expId\": 213, \"datasetId\": 2 }" -output "$1/$2/subscribe/results-download-E213-D2.json" &

# dataset id 3
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/training/data/download/ -payload "{ \"expId\": 213, \"datasetId\": 3 }" -output "$1/$2/subscribe/results-download-E213-D3.json" &

# dataset id 4
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/training/data/download/ -payload "{ \"expId\": 213, \"datasetId\": 4 }" -output "$1/$2/subscribe/results-download-E213-D4.json" &

# dataset id 5
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/training/data/download/ -payload "{ \"expId\": 213, \"datasetId\": 5 }" -output "$1/$2/subscribe/results-download-E213-D5.json" &
