package esa.mo.nmf.apps;

public final class Constants {

    // hide constructor to restrict instantiation
    private Constants() {}

    // experiment id
    public static final int EXPERIMENT_ID = 213;

    // labels 
    public static final String LABEL_TYPE = "type";
    public static final String LABEL_ALGORITHMS = "algorithm";
    public static final String LABEL_REQUEST = "request";
    public static final String LABEL_RESPONSE = "response";
    public static final String LABEL_MESSAGE = "message";
    public static final String LABEL_EXPID = "expId";
    public static final String LABEL_DATASETID = "datasetId";
    public static final String LABEL_DATA = "data";
    public static final String LABEL_NAME = "name";
    public static final String LABEL_DATA_TYPE = "dataType";
    public static final String LABEL_VALUE = "value";
    public static final String LABEL_TIMESTAMP = "timestamp";
    public static final String LABEL_TRAINING = "training";

    // labels for the endpoints
    public static final String LABEL_ENDPOINT_DATA_SUBSCRIBE = "/api/v1/training/data/subscribe";
    public static final String LABEL_ENDPOINT_DATA_UNSUBSCRIBE = "/api/v1/training/data/unsubscribe";
    public static final String LABEL_ENDPOINT_DATA_SAVE = "/api/v1/training/data/save";
    public static final String LABEL_ENDPOINT_DATA_DELETE = "/api/v1/training/data/delete";
    public static final String LABEL_ENDPOINT_TRAINING = "/api/v1/training/:type/";
    public static final String LABEL_ENDPOINT_TRAINING_ALGORITHM = LABEL_ENDPOINT_TRAINING + ":algorithm/";
    public static final String LABEL_ENDPOINT_INFERENCE = "/api/v1/inference/";

    // labels to consume the verticles
    public static final String LABEL_CONSUMER_TRAINING = "saasyml.training";
    public static final String LABEL_CONSUMER_DATA_SAVE = LABEL_CONSUMER_TRAINING + ".data.save";
    public static final String LABEL_CONSUMER_DATA_DELETE = LABEL_CONSUMER_TRAINING + ".data.delete";
    public static final String LABEL_CONSUMER_DATA_COUNT = LABEL_CONSUMER_TRAINING + ".data.count";
    public static final String LABEL_CONSUMER_DATA_COUNT_COLUMNS = LABEL_CONSUMER_TRAINING + ".data.count_columns";
    public static final String LABEL_CONSUMER_DATA_SELECT = LABEL_CONSUMER_TRAINING + ".data.select";
    public static final String LABEL_CONSUMER_DATA_UNSUBSCRIBE = LABEL_CONSUMER_TRAINING + ".data.unsubscribe";
    public static final String LABEL_CONSUMER_DATA_SUBSCRIBE = LABEL_CONSUMER_TRAINING + ".data.subscribe";

    public static final String LABEL_CONSUMER_INFERENCE = "saasyml.inference";
    public static final String LABEL_CONSUMER_INFERENCE_CLASSIFIER = LABEL_CONSUMER_INFERENCE + ".classifier";

    

}