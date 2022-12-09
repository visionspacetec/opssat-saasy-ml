# Client API

A client app to send test API requests to the SaaSyML NMF App.

## Install 

```shell
$ mvn install
```

The jar application will be generated in the **target** folder.

## Use

```shell
$ java -jar saasy-ml-app-api-client-0.1.1-SNAPSHOT-jar-with-dependencies.jar -server [URL] -payload [JSON] (optional -payloadFile [JSON_FILE]) -output [FILE_OUTPUT]";
```

where: 

- server: the complete server address.
- payload: information to send to the API.
- payloadFile: to send the information from a JSON file
- output: file to store the result received from the server

The following is an example:

```shell
$ java -jar saasy-ml-app-api-client-0.1.1.jar -server http://$1/api/v1/training/data/subscribe/ -payload "{ \"expId\": 213, \"datasetId\": 1, \"iterations\": 100, \"labels\": {\"0\": true}, \"interval\": 2, \"params\": [\"GNC_0005\", \"GNC_0011\", \"GNC_0007\", \"GNC_0013\", \"GNC_0016\"] }" 
```
