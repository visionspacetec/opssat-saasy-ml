package esa.mo.nmf.apps.saasyml.api;

public final class Constants {

    // hide constructor to restrict instantiation
    private Constants() {}

    // experiment id
    public static final int EXPERIMENT_ID = 213;

    // labels 
    public static final String KEY_CONTENT_TYPE = "content-type";
    public static final String KEY_CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ALGORITHM = "algorithm";
    public static final String KEY_REQUEST = "request";
    public static final String KEY_RESPONSE = "response";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXPID = "expId";
    public static final String KEY_DATASETID = "datasetId";
    public static final String KEY_DATA = "data";
    public static final String KEY_NAME = "name";
    public static final String KEY_PARAM_NAME = "param_name";
    public static final String KEY_DATA_TYPE = "dataType";
    public static final String KEY_DATA_POINT = "datapoint";
    public static final String KEY_VALUE = "value";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_TRAINING = "training";
    public static final String KEY_MODELS = "models";
    public static final String KEY_PATH = "path";
    public static final String KEY_THREAD = "thread";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_ITERATIONS = "iterations";
    public static final String KEY_LABEL = "label";
    public static final String KEY_LABELS = "labels";
    public static final String KEY_LABELS_PLUGIN = "labelsPlugin";
    public static final String KEY_CLUSTER_NUMBER = "clusterNumber";
    public static final String KEY_PARAMS = "params";
    public static final String KEY_COUNT = "count";
    public static final String KEY_DIMENSIONS = "dimensions";
    public static final String KEY_MODEL_PATH = "model_path";
    public static final String KEY_INFERENCE = "inference";
    public static final String KEY_FORMAT_TO_INFERENCE = "formatToInference";
    public static final String KEY_ERROR = "error";
    public static final String KEY_FILEPATH = "filepath";

    public static final String KEY_MODEL_CLASSIFIER = "Classifier";
    public static final String KEY_MODEL_REGRESSOR = "Regressor";
    public static final String KEY_MODEL_OUTLIER = "Outlier";
    public static final String KEY_MODEL_CLUSTER = "Cluster";

    // values
    public static final String VALUE_ERROR = "error";
    public static final String VALUE_SUCCESS = "success";

    // labels for the endpoints
    public static final String ENDPOINT_API = "/api/";
    public static final String ENDPOINT_VERSION = ENDPOINT_API + "v1/";
    public static final String ENDPOINT_DATA_SUBSCRIBE = ENDPOINT_VERSION + "training/data/subscribe";
    public static final String ENDPOINT_DATA_UNSUBSCRIBE = ENDPOINT_VERSION + "training/data/unsubscribe";
    public static final String ENDPOINT_DATA_SAVE = ENDPOINT_VERSION + "training/data/save";
    public static final String ENDPOINT_DATA_DOWNLOAD = ENDPOINT_VERSION + "training/data/download";
    public static final String ENDPOINT_DATA_DELETE = ENDPOINT_VERSION + "training/data/delete";
    public static final String ENDPOINT_TRAINING = ENDPOINT_VERSION + "training/:type";
    public static final String ENDPOINT_TRAINING_ALGORITHM = ENDPOINT_TRAINING + "/:algorithm";
    public static final String ENDPOINT_MODELS = ENDPOINT_VERSION + "download/models";
    public static final String ENDPOINT_INFERENCE = ENDPOINT_VERSION + "inference";
    public static final String ENDPOINT_INFERENCE_SUBSCRIBE = ENDPOINT_INFERENCE + "/subscribe";
    public static final String ENDPOINT_INFERENCE_UNSUBSCRIBE = ENDPOINT_INFERENCE + "/unsubscribe" ;

    // labels to consume the verticles
    public static final String BASE_ADDRESS_TRAINING = "saasyml.training";
    public static final String ADDRESS_TRAINING_CLASSIFIER = BASE_ADDRESS_TRAINING + ".classifier";
    public static final String ADDRESS_TRAINING_REGRESSOR = BASE_ADDRESS_TRAINING + ".regressor";
    public static final String ADDRESS_TRAINING_CLUSTER = BASE_ADDRESS_TRAINING + ".cluster";
    public static final String ADDRESS_TRAINING_OUTLIER = BASE_ADDRESS_TRAINING + ".outlier";
    public static final String ADDRESS_DATA_SAVE = BASE_ADDRESS_TRAINING + ".data.save";
    public static final String ADDRESS_DATA_DOWNLOAD = BASE_ADDRESS_TRAINING + ".data.download";
    public static final String ADDRESS_DATA_DELETE = BASE_ADDRESS_TRAINING + ".data.delete";
    public static final String ADDRESS_DATA_COUNT = BASE_ADDRESS_TRAINING + ".data.count";
    public static final String ADDRESS_LABELS = BASE_ADDRESS_TRAINING + ".labels";
    public static final String ADDRESS_LABELS_SELECT_DISTINCT = BASE_ADDRESS_TRAINING + ".labels.distinct";
    public static final String ADDRESS_DATA_COUNT_DIMENSIONS = BASE_ADDRESS_TRAINING + ".data.count_columns";
    public static final String ADDRESS_TRAINING_DATA_SELECT = BASE_ADDRESS_TRAINING + ".data.select";
    public static final String ADDRESS_LABELS_SELECT = BASE_ADDRESS_TRAINING + ".labels.select";
    public static final String ADDRESS_DATA_UNSUBSCRIBE = BASE_ADDRESS_TRAINING + ".data.unsubscribe";
    public static final String ADDRESS_DATA_SUBSCRIBE = BASE_ADDRESS_TRAINING + ".data.subscribe";

    public static final String BASE_ADDRESS_MODELS = "saasyml.models";
    public static final String ADDRESS_MODELS_SAVE = BASE_ADDRESS_MODELS + ".save";
    public static final String ADDRESS_MODELS_SELECT = BASE_ADDRESS_MODELS + ".select";


    public static final String BASE_ADDRESS_INFERENCE = "saasyml.inference";
    public static final String BASE_ADDRESS_INFERENCE_SUBSCRIBE = BASE_ADDRESS_INFERENCE + ".subscribe";
    public static final String BASE_ADDRESS_INFERENCE_UNSUBSCRIBE = BASE_ADDRESS_INFERENCE + ".unsubscribe";
    public static final String ADDRESS_INFERENCE_CLASSIFIER = BASE_ADDRESS_INFERENCE + ".classifier";    

}