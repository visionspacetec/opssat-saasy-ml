#!/bin/sh
echo "Running EM session tests"

# executing the client for the experimenter id 213 and datasetId 1
time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 10, \"labels\": {\"0\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" & time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 10, \"labels\": {\"1\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

# dataset id 2

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"iterations\": 10, \"labels\": {\"3\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"iterations\": 10, \"labels\": {\"4\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 2, \"iterations\": 10, \"labels\": {\"5\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

# dataset id 3

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"iterations\": 10, \"labels\": {\"6\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 3, \"iterations\": 10, \"labels\": {\"7\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

# dataset id 4

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"iterations\": 10, \"labels\": {\"8\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"iterations\": 10, \"labels\": {\"9\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }" &

time java -jar saasy-ml-app-api-client-0.1.1.jar -server http://192.168.178.40:9999/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 4, \"iterations\": 10, \"labels\": {\"10\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\"] }"
