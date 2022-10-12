#!/bin/sh
echo "Running test train data fetch"

sleep 1

# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 2, \"labels\": {\"0\": true}, \"interval\": 1, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }"

# time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 2, \"labels\": {\"1\": true}, \"interval\": 1, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }"


# experimenter id 213

# datasetId 1
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 100, \"labels\": {\"0\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 100, \"labels\": {\"1\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

# dataset id 2
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"iterations\": 100, \"labels\": {\"0\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"iterations\": 100, \"labels\": {\"1\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"iterations\": 100, \"labels\": {\"2\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &


# dataset id 3
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"iterations\": 100, \"labels\": {\"0\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"iterations\": 100, \"labels\": {\"1\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &


# dataset id 4
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"iterations\": 100, \"labels\": {\"0\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"iterations\": 100, \"labels\": {\"1\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"iterations\": 100, \"labels\": {\"2\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }"
