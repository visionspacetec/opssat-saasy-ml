package esa.mo.nmf.apps.saasyml.api;

public final class Constants {

    // hide constructor to restrict instantiation
    private Constants() {}

    // experiment id
    public static final int EXPERIMENT_ID = 213;

    // labels 
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
    public static final String KEY_PARAMS = "params";
    public static final String KEY_COUNT = "count";
    public static final String KEY_DIMENSIONS = "dimensions";
    public static final String KEY_MODEL_PATH = "model_path";
    public static final String KEY_INFERENCE = "inference";
    public static final String KEY_ERROR = "error";

    // labels for the endpoints
    public static final String ENDPOINT_DATA_SUBSCRIBE = "/api/v1/training/data/subscribe";
    public static final String ENDPOINT_DATA_UNSUBSCRIBE = "/api/v1/training/data/unsubscribe";
    public static final String ENDPOINT_DATA_SAVE = "/api/v1/training/data/save";
    public static final String ENDPOINT_DATA_DOWNLOAD = "/api/v1/training/data/download";
    public static final String ENDPOINT_DATA_DELETE = "/api/v1/training/data/delete";
    public static final String ENDPOINT_TRAINING = "/api/v1/training/:type";
    public static final String ENDPOINT_TRAINING_ALGORITHM = ENDPOINT_TRAINING + "/:algorithm";
    public static final String ENDPOINT_INFERENCE = "/api/v1/inference";

    // labels to consume the verticles
    public static final String BASE_ADDRESS_TRAINING = "saasyml.training";
    public static final String ADDRESS_TRAINING_CLASSIFIER = BASE_ADDRESS_TRAINING + ".classifier";
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

    public static final String BASE_ADDRESS_INFERENCE = "saasyml.inference";
    public static final String ADDRESS_INFERENCE_CLASSIFIER = BASE_ADDRESS_INFERENCE + ".classifier";




    

}