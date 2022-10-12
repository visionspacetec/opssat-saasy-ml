#!/bin/sh
echo "Running test train classifier models no thread"

sleep 1


# dataset Id 1
cat "$1/$2/train/results-download-classifier-models-toinference-E213-D1-thread.json" | python3 -c "import sys, json; print(json.load(sys.stdin)['response']['models'])"

echo $var

# dataset id 2

# dataset id 3

# dataset id 4


## perform the inference

# # dataset Id 1
# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/inference/ -payloadFile "em-inference-datasetid1.json" -output "$1/$2/inference/results-inference-E213-D1.json" &

# # dataset id 2
# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/inference/ -payloadFile "em-inference-datasetid2.json" -output "$1/$2/inference/results-inference-E213-D2.json" &

# # dataset id 3
# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/inference/ -payloadFile "em-inference-datasetid3.json" -output "$1/$2/inference/results-inference-E213-D3.json" &

# # dataset id 4
# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/inference/ -payloadFile "em-inference-datasetid4.json" -output "$1/$2/inference/results-inference-E213-D4.json" &

# # dataset id 5
# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$3/api/v1/inference/ -payloadFile "em-inference-datasetid5.json" -output "$1/$2/inference/results-inference-E213-D5.json" &
