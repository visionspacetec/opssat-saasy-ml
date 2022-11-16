package esa.mo.nmf.apps.saasyml.api.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;


import org.sqlite.SQLiteConfig;

import esa.mo.nmf.apps.ApplicationManager;
import esa.mo.nmf.apps.ExtensionManager;
import esa.mo.nmf.apps.saasyml.api.Constants;
import esa.mo.nmf.apps.saasyml.api.utils.Pair;
import esa.mo.nmf.apps.saasyml.api.utils.SqliteHelper;
import esa.mo.nmf.apps.PropertiesManager;

public class DatabaseVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(DatabaseVerticle.class.getName());

    // database connection
    private Connection conn = null;

    // table names
    private static final String TABLE_TRAINING_DATA = "training_data";
    private static final String TABLE_MODELS = "models";
    private static final String TABLE_LABELS = "labels";

    // sql query strings
    private final String SQL_INSERT_TRAINING_DATA = "INSERT INTO "+TABLE_TRAINING_DATA+"(exp_id, dataset_id, param_name, data_type, value, timestamp) VALUES(?, ?, ?, ?, ?, ?)";
    private final String SQL_INSERT_LABELS = "INSERT INTO "+TABLE_LABELS+"(exp_id, dataset_id, timestamp, label) VALUES(?, ?, ?, ?)";
    private final String SQL_INSERT_MODELS = "INSERT INTO "+TABLE_MODELS+"(exp_id, dataset_id, timestamp, type, algorithm, filepath, error) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private final String SQL_COUNT_TRAINING_DATA = "SELECT count(*) FROM  "+TABLE_TRAINING_DATA+" WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_COUNT_COLUMNS_TRAINING_DATA = "SELECT count(*) FROM  "+TABLE_TRAINING_DATA+" WHERE exp_id = ? AND dataset_id = ? AND param_name != 'label' GROUP BY timestamp LIMIT 1"; // "SELECT count(DISTINCT param_name) FROM training_data WHERE exp_id=? AND dataset_id=?" 
    private final String SQL_SELECT_TRAINING_DATA = "SELECT * FROM "+TABLE_TRAINING_DATA+" WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp ASC";
    private final String SQL_SELECT_LABELS = "SELECT * FROM "+TABLE_LABELS+" WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp ASC";
    private final String SQL_SELECT_MODELS = "SELECT exp_id as expId, dataset_id as datasetId, timestamp, type, algorithm, filepath, error FROM "+TABLE_MODELS+" WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp DESC";
    private final String SQL_SELECT_MODELS_TO_INFERENCE = "SELECT type, filepath, error FROM "+TABLE_MODELS+" WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp DESC";
    private final String SQL_SELECT_DISTINCT_LABELS = "SELECT DISTINCT label FROM "+TABLE_LABELS+"  WHERE exp_id = ? AND dataset_id = ? ORDER BY label ASC";
    private final String SQL_DELETE_TRAINING_DATA = "DELETE FROM "+TABLE_TRAINING_DATA+" WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_DELETE_LABELS = "DELETE FROM "+TABLE_LABELS+" WHERE exp_id = ? AND dataset_id = ?";
    
    private final String SQL_CREATE_TABLE_TRAINING_DATA = 
        "CREATE TABLE IF NOT EXISTS "+TABLE_TRAINING_DATA+"(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "param_name TEXT NOT NULL, " +
            "data_type INTEGER NOT NULL, " +
            "value TEXT NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL" +
        ")";

    private final String SQL_CREATE_TABLE_LABELS = 
        "CREATE TABLE IF NOT EXISTS "+TABLE_LABELS+"(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL, " +
            "label INTEGER NOT NULL" +
        ")";

    private final String SQL_CREATE_TABLE_MODELS = 
        "CREATE TABLE IF NOT EXISTS "+TABLE_MODELS+"(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL, " +
            "type TEXT NOT NULL, " +
            "algorithm TEXT NOT NULL, " +
            "filepath TEXT, " +
            "error TEXT" +
                    ")";

    public synchronized Connection connect() throws Exception {

        if(this.conn == null || this.conn.isClosed())
        {
            try {
                // get a unique connection of the database
                this.conn = SqliteHelper.getConnection();

                // log error
                LOGGER.log(Level.INFO, "Database connection {0} created successfully.", this.conn);

                List<Pair<String, String>> pairTableAndSQLCreate = Arrays.asList(
                    new Pair<String, String>(TABLE_TRAINING_DATA, SQL_CREATE_TABLE_TRAINING_DATA), 
                    new Pair<String, String>(TABLE_LABELS, SQL_CREATE_TABLE_LABELS), 
                    new Pair<String, String>(TABLE_MODELS, SQL_CREATE_TABLE_MODELS));
                
                for (Pair<String, String> pair : pairTableAndSQLCreate) {    
                    // check if training data table exists and create it if it does not.
                    if(!this.tableExists(pair.getKey())) {
                        this.createTable(pair.getValue());
                        LOGGER.log(Level.INFO, "Created the {0} table.", pair.getKey());
                    }else {
                        LOGGER.log(Level.INFO, "The {0} table already exists.", pair.getKey());
                    }
                }
                    
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[Error] It was not possible to create the connection", e);
                // close connection in case it is open despite the exception.
                this.closeConnection();
            }            
        }

        return this.conn;
    }

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);

        // connect to the database
        try {
            this.connect();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database connection initialization failed: training data will not persist.", e);
        }
    }

    @Override
    public void start() throws Exception {

        LOGGER.log(Level.INFO, "Starting a Verticle instance with deployment id {0}", this.deploymentID());

        // save training data
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_SAVE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final JsonArray data = payload.getJsonArray(Constants.KEY_DATA);

            // get the extension classpath if it has been set so we know to calculate expected labels if they rely on a plugin
            String extensionClasspath = ApplicationManager.getInstance().getExtentionClasspath(expId, datasetId);

            // create map of param name and double values in case we need it for the plugin
            Map<String, Double> extensionInputMap = new HashMap<String, Double>();

            // the expected labels retrived from the extension
            LinkedHashMap<Long, Map<String, Boolean>> expectedLabelsExtMap = new LinkedHashMap<Long, Map<String, Boolean>>();

            List<Pair<Integer, String>> paramValues = new ArrayList<Pair<Integer, String>>();
            List<String> paramNames = new ArrayList<String>();
            List<Long> paramTimestamps = new ArrayList<Long>();
            Long currentParamTimestamp = null;

            // collect timestamps for the labels table
            List<Long> labelTimestamps = new ArrayList<Long>();
            Long currentLabelTimestamp = null;
   
            try { 

                // fetch data
                for(int i = 0; i < data.size(); i++){
                    JsonArray ds = data.getJsonArray(i);

                    // reset labels timestamp flags
                    currentParamTimestamp = null;
                    currentLabelTimestamp = null;

                    for(int j = 0; j < ds.size(); j++){
                        JsonObject p = ds.getJsonObject(j);

                        // parameter name
                        paramNames.add(p.getString(Constants.KEY_NAME));

                        // parameter type and value value
                        int dataType = p.getInteger(Constants.KEY_DATA_TYPE);
                        String value = p.getString(Constants.KEY_VALUE);
                        paramValues.add(new Pair<Integer, String>(dataType, value));
                        
                        // fetch the param timestamp in milliseconds
                        // if no timestamp is given then set one
                        if(currentParamTimestamp == null){
                            if(p.containsKey(Constants.KEY_TIMESTAMP)){
                                currentParamTimestamp = p.getLong(Constants.KEY_TIMESTAMP);
                            }else {
                                // FIXME: inconsistent time value storage, millisecond vs nanoseconds
                                // if we have to set our own time then it needs to be in nanoseconds
                                // this is because we want to make sure each timestamp value is unique
                                currentParamTimestamp = System.nanoTime();
                            }
                        }

                        paramTimestamps.add(currentParamTimestamp);

                        // set the label timestamp if it has not been set
                        if(currentLabelTimestamp == null){
                            currentLabelTimestamp = currentParamTimestamp;
                            labelTimestamps.add(currentLabelTimestamp);
                        }

                        if(extensionClasspath != null){
                            try {
                                // populate extension input map
                                extensionInputMap.put(p.getString(Constants.KEY_NAME), new Double(p.getString(Constants.KEY_VALUE)));
                            } catch (Exception e) {
                                extensionInputMap.clear();
                                LOGGER.log(Level.SEVERE, "The expected labels plugin cannot be invoked because the fetched parameter values are uncastable to the Double type", e);
                            }  
                        }
                    }

                    // If we are meant to fetch the expected labels from a plugin then do that now
                    if(!extensionInputMap.isEmpty()){
                        Map<String, Boolean> expLbls = ExtensionManager.getInstance().getExpectedLabels(expId, datasetId, extensionInputMap);
                        if(expLbls != null){
                            expectedLabelsExtMap.put(currentLabelTimestamp, expLbls);
                            extensionInputMap.clear();
                        }else{
                            LOGGER.log(Level.SEVERE, "Could not retrieve expected label from extension {0}. The fetched training data will be persisted without expected labels.", extensionClasspath );
                        }
                    }

                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error preparing training data and labels to insert into the database.", e);

                // response
                msg.reply("error preparing training data to insert into the database.");
                return;
            }

            
            // the prepared statement
            PreparedStatement prep = null;

            // create the prepared statement and execute
            try {

                // no commit per statement
                this.conn.setAutoCommit(false);

                // init the prepared statement to insert training data
                prep = this.conn.prepareStatement(
                    SQL_INSERT_TRAINING_DATA
                );

                this.setInsertTrainingDataPreparedStatementParameters(prep, expId, datasetId, paramNames, paramValues, paramTimestamps);
                prep.executeBatch();
                prep.close();

                // re-init the prepared statement to insert labels
                prep = this.conn.prepareStatement(
                    SQL_INSERT_LABELS
                );

                // if expected labels are fetcched from the extension
                if(extensionClasspath != null) {
                    for (Map.Entry<Long, Map<String, Boolean>> entry : expectedLabelsExtMap.entrySet()){
                        // set the expected label calculate by the extension
                        ApplicationManager.getInstance().addLabels(expId, datasetId, entry.getValue());
    
                        // persist the expected label in the database
                        this.setInsertLabelsPreparedStatementParameters(prep, expId, datasetId, entry.getKey());
                    }
                } else {
                    // if labels are provided as part of the request payload
                    for(Long t : labelTimestamps){
                        this.setInsertLabelsPreparedStatementParameters(prep, expId, datasetId, t);
                    }
                }
                

                prep.executeBatch();
                prep.close();

            } catch (Exception e) {
                try {

                    // reset auto-commit to true
                    this.conn.setAutoCommit(true); 

                    // an error has occured: rollback
                    LOGGER.log(Level.SEVERE, "Failed to insert training data and labels into the database: rolling back.", e);
                    this.conn.rollback();

                    // response
                    msg.reply("failed to insert training data and labels into the database.");
                    return;

                } catch(SQLException sqle) {
                    LOGGER.log(Level.SEVERE, "Error rolling back.", sqle);

                    // response
                    msg.reply("failed to insert training data and labels, could not rollback.");
                    return;
                } 
            }

            // cleanup resources
            try {
                // reset auto-commit to true
                this.conn.setAutoCommit(true); 

                if(prep != null && !prep.isClosed()) {
                    
                    prep.close();
                }
            }
            catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error cleaning up.", e);
            }


            // auto-trigger training if the payload is configured to do so
            if (payload.containsKey(Constants.KEY_TRAINING)) {

                // the training parameters can be for more than one algorithm
                final JsonArray trainings = payload.getJsonArray(Constants.KEY_TRAINING);

                // trigger training for each request
                for (int i = 0; i < trainings.size(); i++) {
                    final JsonObject t = trainings.getJsonObject(i);

                    // fetch training algorithm selection
                    String type = t.getString(Constants.KEY_TYPE);

                    // trigger training
                    vertx.eventBus().send(Constants.BASE_ADDRESS_TRAINING + "." + type, payload);
                }
            }
           
            // response
            msg.reply("saved training data.");
        });

        // delete training data
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_DELETE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            try {
                this.deleteTrainingData(expId, datasetId);
                
                // response
                msg.reply("deleted training data.");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to delete training data from the database.", e);

                // response
                msg.reply("error deleting training data.");
            }
        });

        // count training data
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_COUNT, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // count training data records
            int counter = -1;
            try {
                counter = this.executeCountQuery(expId, datasetId, SQL_COUNT_TRAINING_DATA);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to count training data rows in the database.", e);
            }

            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_COUNT, counter);
            msg.reply(response);
            
        });

        // count columns in training data
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_COUNT_DIMENSIONS, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // count training data records
            int counter = -1;
            try {
                counter = this.executeCountQuery(expId, datasetId, SQL_COUNT_COLUMNS_TRAINING_DATA);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to count training data rows in the database.", e);
            }

            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_COUNT, counter);
            msg.reply(response);
            
        });

        // select training data
        vertx.eventBus().consumer(Constants.ADDRESS_TRAINING_DATA_SELECT, msg -> {

            // the request payload (JSON)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the JSON payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // select training data records
            JsonArray data = null;
            try {
                data = this.selectQueryByExpIdAndDatasetId(expId, datasetId, SQL_SELECT_TRAINING_DATA);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get training data from the database.", e);
            }
            
            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_DATA, data);
            msg.reply(response);

        });

        // select labels
        vertx.eventBus().consumer(Constants.ADDRESS_LABELS_SELECT, msg -> {

            // the request payload (JSON)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the JSON payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // select labels records
            JsonArray data = null;
            try {
                data = this.selectQueryByExpIdAndDatasetId(expId, datasetId, SQL_SELECT_LABELS);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get labels from the database.", e);
            }
            
            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_DATA, data);
            msg.reply(response);

        });

        // select labels
        vertx.eventBus().consumer(Constants.ADDRESS_MODELS_SELECT, msg -> {

            // the request payload (JSON)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the JSON payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // response
            JsonObject response = new JsonObject();

            // determine the query to perform between "select all the data in the models" or "select the needed to the inference"
            // With format to inference, we add the expId outside. To see all the fields of the dataset, we should execute with formatToInference = false
            String sqlQuery = SQL_SELECT_MODELS;            
            if (payload.containsKey(Constants.KEY_FORMAT_TO_INFERENCE)
                && payload.getBoolean(Constants.KEY_FORMAT_TO_INFERENCE).booleanValue())
            {
                sqlQuery = SQL_SELECT_MODELS_TO_INFERENCE;
                response.put(Constants.KEY_EXPID, payload.getInteger(Constants.KEY_EXPID));
            }

            // select models records
            JsonArray models = null;
            try {
                models = this.selectQueryByExpIdAndDatasetId(expId, datasetId, sqlQuery);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get models from the database.", e);
            }
            
            response.put(Constants.KEY_MODELS, models);

            msg.reply(response);

        });

        // select labels
        vertx.eventBus().consumer(Constants.ADDRESS_LABELS_SELECT_DISTINCT, msg -> {

            // the request payload (JSON)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the JSON payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();

            // select labels records
            JsonArray data = null;
            try {
                data = this.selectQueryByExpIdAndDatasetId(expId, datasetId, SQL_SELECT_DISTINCT_LABELS);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get labels from the database.", e);
            }
            
            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_DATA, data);
            msg.reply(response);

        });


        // save model metadata
        vertx.eventBus().consumer(Constants.ADDRESS_MODELS_SAVE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final String type = payload.getString(Constants.KEY_TYPE);
            final String algorithm = payload.getString(Constants.KEY_ALGORITHM);
            final String filepath = payload.getString(Constants.KEY_FILEPATH);
            final String error = payload.getString(Constants.KEY_ERROR);

            // insert training data
            try (PreparedStatement prep = this.conn.prepareStatement(SQL_INSERT_MODELS)) {

                // set satement parameters
                prep.setInt(1, expId); // experiment id
                prep.setInt(2, datasetId); // dataset id
                prep.setTimestamp(3, new Timestamp(System.currentTimeMillis())); // the timestamp returned by NMF marking when the data was fetched
                prep.setString(4, type); // the type of training algorithm that was used to train the model (e.g. classifier)
                prep.setString(5, algorithm); // the training algorithm that was used to train the model (e.g. AROW)
                prep.setString(6, filepath); // the model filepath (none if an error was thrown during training)
                prep.setString(7, error); // the error message (if an error was thrown during training)
                
                prep.executeUpdate();
                
                // log and respond
                String message = String.format("Saved model metadata for (expId, datasetId) = (%d, %d)", expId, datasetId);
                LOGGER.log(Level.INFO, message);
                msg.reply(message);

            } catch (Exception e) {
                String errorMsg ="Error while trying to save model metadata in the database.";
                LOGGER.log(Level.SEVERE, errorMsg, e);

                // response
                msg.reply(errorMsg);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        this.closeConnection();
    }

    private void setInsertTrainingDataPreparedStatementParameters(PreparedStatement prep,
        int expId, int datasetId, List<String> paramNames, List<Pair<Integer, String>> paramValues, List<Long>timestamps) throws Exception {
            
        // prepare a prepared statement for each fetched parameter
        for (int i = 0; i < paramValues.size(); i++) {

            // set satement parameters
            prep.setInt(1, expId); // experiment id
            prep.setInt(2, datasetId); // dataset id
            prep.setString(3, paramNames.get(i)); // get param name
            prep.setInt(4, paramValues.get(i).getKey()); // the data type short form
            prep.setString(5, paramValues.get(i).getValue()); // the param name value as a string
            prep.setTimestamp(6, new Timestamp(timestamps.get(i))); // the timestamp returned by NMF marking when the data was fetched

            // add to batch
            prep.addBatch();
        }
    }

    private void setInsertLabelsPreparedStatementParameters(PreparedStatement prep,
        int expId, int datasetId, Long timestamp) throws Exception {

        // fetch the label map for the given experiment and dataset ids
        Map<String, Boolean> labelMap = ApplicationManager.getInstance().getLabels(expId, datasetId);
        
        if (labelMap == null){
            return;
        }

        // iterator to iterate through the label map
        Iterator<Map.Entry<String, Boolean>> iter = labelMap.entrySet().iterator();
          
        // prepare a prepared statement for each batch of training data fetched for the given experiment and dataset id
        while(iter.hasNext()){
            Map.Entry<String, Boolean> label = iter.next();

            if(Boolean.TRUE.equals(label.getValue())) {

                // set satement parameters
                prep.setInt(1, expId); // the experiment id
                prep.setInt(2, datasetId); // the dataset id
                prep.setTimestamp(3, new Timestamp(timestamp)); // the timestamp marking when the data was fetched
                prep.setInt(4, Integer.parseInt(label.getKey())); // the label value
                
                // add to batch
                prep.addBatch();
                
                // successfully set the expected labels
                // can exit function
                return;
            }
            
        }

        // if code execution reaches here it means that not label was given for a training dataset input, log it as an error
        LOGGER.log(Level.WARNING, "Expected label was not set so training dataset input will be discarded as a model training input");
    }

    private void deleteTrainingData(int expId, int datasetId) throws Exception {
        
        // when deleting training data we need to also delete their labels in the labels table
        // we have two execute 2 delete statements in 2 different tables
        // we can't have a sitation where one query fails and the other succeeds
        // manage this with transactions

        // no commit per statement
        this.conn.setAutoCommit(false); 

        // delete the training data
        deleteTable(expId, datasetId, SQL_DELETE_TRAINING_DATA);

        // no commit per statement
        this.conn.setAutoCommit(false);
        
        // delete the labels
        deleteTable(expId, datasetId, SQL_DELETE_LABELS);

    }

    private void deleteTable(int expId, int datasetId, String SQLQuery) throws Exception {
        
        // create the prepared statement
        try (PreparedStatement ps = this.conn.prepareStatement(SQLQuery)) {

            // set satement parameters
            ps.setInt(1, expId); // experiment id
            ps.setInt(2, datasetId); // dataset id

            // execute the delete statement
            ps.executeUpdate();
            ps.close();

        } catch(SQLException e){
            // an error has occured: rollback
            LOGGER.log(Level.SEVERE, "Error executing the training data and labels delete transaction: rolling back", e);
            this.conn.rollback();
        } finally {
            try {
                // reset auto-commit to true
                this.conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error cleaning up", e);
            }
        }
    }

    private int executeCountQuery(int expId, int datasetId, String querySQL) throws Exception {
        // create the prepared statement
        try (PreparedStatement ps = this.conn.prepareStatement(querySQL)) {

            // set satement parameters
            ps.setInt(1, expId); // experiment id
            ps.setInt(2, datasetId); // dataset id

            // execute the delete statement
            try (ResultSet rs = ps.executeQuery()) {

                // return the result        
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    LOGGER.log(Level.SEVERE, "Error in the Query. Please, check the statement parameters");
                    return -1;
                }
            }

        }  catch (java.sql.SQLException e) {
            throw new Exception("The query did not work.");
        }
    }

    private JsonArray selectQueryByExpIdAndDatasetId(int expId, int datasetId, String SQL_QUERY) throws Exception {
        
        // create the prepared statement
        try (PreparedStatement ps = this.conn.prepareStatement(SQL_QUERY)) {

            // set statement parameters
            ps.setInt(1, expId);
            ps.setInt(2, datasetId);

            // execute the select statement
            try (ResultSet rs = ps.executeQuery()) {

                // return the result
                return toJSON(rs);
            }
        }
    }

    private JsonArray toJSON(ResultSet rs) {
        JsonArray json = new JsonArray();

        try {

            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();
    
            while (rs.next()) {
                JsonObject obj = new JsonObject();
    
                for (int i = 1; i <= numColumns; i++) {
                    if (rs.getObject(i) != null){
                        obj.put(rsmd.getColumnName(i), rs.getObject(i));
                    }
                }
                json.add(obj);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while trying to get training data in the database", e);
        }
        
        return json;
    }

    private boolean tableExists(String tableName) throws Exception {

        // search for table macthing expected table name
        DatabaseMetaData md = this.conn.getMetaData();
        ResultSet tables = md.getTables(null, null, tableName, null);

        // return true if the table exists and false if it does not
        return tables.next() ? true : false;
    }

    private void createTable(String SQL_QUERY) throws Exception {

        // create a statement
        try (Statement stmt = this.conn.createStatement()) {

            // execute the statement to create the table
            stmt.executeUpdate(SQL_QUERY);
        }
    }

    public void closeConnection() throws Exception {
        if(this.conn != null && !this.conn.isClosed()){
            this.conn.close();
        }
    }

}