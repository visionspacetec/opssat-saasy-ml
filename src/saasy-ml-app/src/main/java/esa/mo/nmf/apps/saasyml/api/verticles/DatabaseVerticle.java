package esa.mo.nmf.apps.saasyml.api.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import javafx.application.Application;
import javafx.util.Pair;

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
import esa.mo.nmf.apps.saasyml.api.Constants;
import esa.mo.nmf.apps.PropertiesManager;

public class DatabaseVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(DatabaseVerticle.class.getName());

    // database connection
    private Connection conn = null;

    // sql query strings
    private final String SQL_INSERT_TRAINING_DATA = "INSERT INTO training_data(exp_id, dataset_id, param_name, data_type, value, timestamp) VALUES(?, ?, ?, ?, ?, ?)";
    private final String SQL_INSERT_LABELS = "INSERT INTO labels(exp_id, dataset_id, timestamp, label) VALUES(?, ?, ?, ?)";
    private final String SQL_COUNT_TRAINING_DATA = "SELECT count(*) FROM  training_data WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_COUNT_COLUMNS_TRAINING_DATA = "SELECT count(*) FROM  training_data WHERE exp_id = ? AND dataset_id = ? AND param_name != 'label' GROUP BY timestamp LIMIT 1"; // "SELECT count(DISTINCT param_name) FROM training_data WHERE exp_id=? AND dataset_id=?" 
    private final String SQL_SELECT_TRAINING_DATA = "SELECT * FROM training_data WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp ASC";
    private final String SQL_SELECT_LABELS = "SELECT * FROM labels WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp ASC";
    private final String SQL_SELECT_DISTINCT_LABELS = "SELECT DISTINCT label FROM labels  WHERE exp_id = ? AND dataset_id = ? ORDER BY label ASC";
    private final String SQL_DELETE_TRAINING_DATA = "DELETE FROM training_data WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_DELETE_LABELS = "DELETE FROM labels WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_CREATE_TABLE_TRAINING_DATA = 
        "CREATE TABLE IF NOT EXISTS training_data(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "param_name TEXT NOT NULL, " +
            "data_type INTEGER NOT NULL, " +
            "value TEXT NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL" +
        ")";
    private final String SQL_CREATE_TABLE_LABELS = 
        "CREATE TABLE IF NOT EXISTS labels(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL, " +
            "label INTEGER NOT NULL " +
        ")";

    public Connection connect() throws Exception {
        if(this.conn == null || this.conn.isClosed())
        {
            try {

                // register the database driver
                Class.forName(PropertiesManager.getInstance().getDatabaseDriver());
                
                // create the connection with the diven database connection configuration
                // by default a single write to the database locks the database for a short time, nothing, even reading, can access the database file at all.
                // use the "Write Ahead Logging" (WAL) option is available to enable reading and writing can proceed concurrently.
                SQLiteConfig config = new SQLiteConfig();
                config.setJournalMode(SQLiteConfig.JournalMode.WAL);
                this.conn = DriverManager.getConnection(PropertiesManager.getInstance().getDatabaseUrl(), config.toProperties());
                
                if (this.conn != null) {
                    // log error
                    LOGGER.log(Level.INFO, "Database connection created successfully.");
    
                    // check if training data table exists and create it if it does not.
                    if(!this.trainingDataTableExists()) {
                        this.createTrainingDataTable();
                        LOGGER.log(Level.INFO, "Created the training data table.");
                    }else {
                        LOGGER.log(Level.INFO, "The training data table already exists.");
                    }

                    // check if the labels table exists and create it if it does not.
                    if(!this.labelsTableExists()) {
                        this.createLabelsTable();
                        LOGGER.log(Level.INFO, "Created the labels table.");
                    }else {
                        LOGGER.log(Level.INFO, "The labels table already exists.");
                    }
                    
                }else {
                    this.conn = null;
                    throw new Exception("Failed to create database connection");
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "[Error] It was not possible to create the connection");
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
            LOGGER.log(Level.INFO, "Database connection initialization failed: training data will not persist.", e);
        }
    }

    @Override
    public void start() throws Exception {

        // save training data
        vertx.eventBus().consumer(Constants.ADDRESS_DATA_SAVE, msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger(Constants.KEY_EXPID).intValue();
            final int datasetId = payload.getInteger(Constants.KEY_DATASETID).intValue();
            final JsonArray data = payload.getJsonArray(Constants.KEY_DATA);

            List<Pair<Integer, String>> paramValues = new ArrayList<Pair<Integer, String>>();
            List<String> paramNames = new ArrayList<String>();
            List<Long> timestamps = new ArrayList<Long>();

            // TODO: Labels are not being considered

            // the prepared statement
            PreparedStatement prep = null;

            try { 

                // fetch data
                data.forEach(dataset -> {
                    JsonArray ds = (JsonArray) dataset;

                    ds.forEach(param -> {
                        JsonObject p = (JsonObject) param;

                        // parameter name
                        paramNames.add(p.getString(Constants.KEY_NAME));

                        // parameter type and value value
                        int dataType = p.getInteger(Constants.KEY_DATA_TYPE);
                        String value = p.getString(Constants.KEY_VALUE);
                        paramValues.add(new Pair<Integer, String>(dataType, value));
                        
                        // the timestamp
                        timestamps.add(p.getLong(Constants.KEY_TIMESTAMP));
                    });
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error preparing training data and labels to insert into the database.", e);

                // response
                msg.reply("error preparing training data to insert into the database.");
                return;
            }

            // create the prepared statement and execute
            try {

                // no commit per statement
                this.conn.setAutoCommit(false);

                // insert training data
                prep = this.conn.prepareStatement(
                    SQL_INSERT_TRAINING_DATA
                );

                this.setInsertTrainingDataPreparedStatementParameters(prep, expId, datasetId, paramNames, paramValues, timestamps);
                prep.executeBatch();
                prep.close();

                // insert labels
                prep = this.conn.prepareStatement(
                    SQL_INSERT_LABELS
                );

                this.setInsertLabelsPreparedStatementParameters(prep, expId, datasetId, timestamps.get(0));
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
                data = this.selectTrainingData(expId, datasetId);
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
                data = this.selectLabels(expId, datasetId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get labels from the database.", e);
            }
            
            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_DATA, data);
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
                data = this.selectDistinctLabels(expId, datasetId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get labels from the database.", e);
            }
            
            // response
            JsonObject response = new JsonObject();
            response.put(Constants.KEY_DATA, data);
            msg.reply(response);

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

            if(label.getValue()) {

                // set satement parameters
                prep.setInt(1, expId); // experiment id
                prep.setInt(2, datasetId); // dataset id
                prep.setTimestamp(3, new Timestamp(timestamp)); // the timestamp returned by NMF marking when the data was fetched
                prep.setInt(4, Integer.parseInt(label.getKey())); // the label value
                
                // add to batch
                prep.addBatch();

                // successfully set the expected label
                // can exit function
                return;
            }
            
        }

        // if code execution reaches here it means that not label was given for a training dataset input, log it as an error
        LOGGER.log(Level.WARNING, "Expected label was not set so training dataset input will discarded as model training input");
    }

    /**
    private void insertTrainingData(int expId, int datasetId, List<String> paramNames, List<Pair<Integer, String>> paramValues, List<Long>timestamps) throws Exception {
        // create the prepared statement
        PreparedStatement prep = this.conn.prepareStatement(
            SQL_INSERT_TRAINING_DATA
        );

        setInsertTrainingDataPreparedStatementParameters(prep, expId, datasetId, paramNames, paramValues, timestamps);
            
        // execute the prepared statement
        prep.executeBatch();
    } */

    private void deleteTrainingData(int expId, int datasetId) throws Exception {
        
        // when deleting training data we need to also delete their labels in the labels table
        // we have two execute 2 delete statements in 2 different tables
        // we can't have a sitation where one query fails and the other succeeds
        // manage this with transactions

        // no commit per statement
        this.conn.setAutoCommit(false); 

        // the prepared statement object that will be used for both training data and labels deletes
        PreparedStatement ps = null;

        try {    
            // init the prepared statement to delete the training data
            ps = this.conn.prepareStatement(SQL_DELETE_TRAINING_DATA);

            // set satement parameters
            ps.setInt(1, expId); // experiment id
            ps.setInt(2, datasetId); // dataset id

            // execute the delete statement to delete training data
            ps.executeUpdate();

            // close
            ps.close();

            // init the prepared statement to delete the labels
            ps = this.conn.prepareStatement(SQL_DELETE_LABELS);

            // set satement parameters
            ps.setInt(1, expId); // experiment id
            ps.setInt(2, datasetId); // dataset id

            // execute the delete statement to delete labels
            ps.executeUpdate();

            // close
            ps.close();

        } catch(SQLException e){
            // an error has occured: rollback
            LOGGER.log(Level.SEVERE, "Error executing the training data and labels delete transaction: rolling back", e);
            this.conn.rollback();

        } finally {
            try {
                // reset auto-commit to true
                this.conn.setAutoCommit(true); 

                if(ps != null && !ps.isClosed()) {
                    // cleanup resources
                    ps.close();
                }
            }
            catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error cleaning up", e);
            }
        }  
    }

    private void deleteLabels(int expId, int datasetId) throws Exception {
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_DELETE_LABELS);

        // set satement parameters
        ps.setInt(1, expId); // experiment id
        ps.setInt(2, datasetId); // dataset id

        // execute the delete statement
        ps.executeUpdate();
        ps.close();
    }

    private int executeCountQuery(int expId, int datasetId, String querySQL) throws Exception {
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(querySQL);

        // set satement parameters
        ps.setInt(1, expId); // experiment id
        ps.setInt(2, datasetId); // dataset id

        // execute the delete statement
        ResultSet rs = ps.executeQuery();

        // todo: ps.close()?

        // return the result        
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            LOGGER.log(Level.SEVERE, "Error in the Query. Please, check the statement parameters");
            return -1;
        }
    }

    private JsonArray selectTrainingData(int expId, int datasetId) throws Exception {
        
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_SELECT_TRAINING_DATA);

        // set statement parameters
        ps.setInt(1, expId);
        ps.setInt(2, datasetId);

        // execute the select statement
        ResultSet rs = ps.executeQuery();

        // ps.close();

        // return the result
        return toJSON(rs);

    }

    private JsonArray selectLabels(int expId, int datasetId) throws Exception {
        
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_SELECT_LABELS);

        // set statement parameters
        ps.setInt(1, expId);
        ps.setInt(2, datasetId);

        // execute the select statement
        ResultSet rs = ps.executeQuery();

        // ps.close();

        // return the result
        return toJSON(rs);
    }

    private JsonArray selectDistinctLabels(int expId, int datasetId) throws Exception {
        
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_SELECT_DISTINCT_LABELS);

        // set statement parameters
        ps.setInt(1, expId);
        ps.setInt(2, datasetId);

        // execute the select statement
        ResultSet rs = ps.executeQuery();

        // build the result JSON
        JsonArray resultJson = toJSON(rs);

        // close the prepared statement
        ps.close();

        // return the result
        return resultJson;
    }

    private JsonArray toJSON(ResultSet rs) {
        JsonArray json = new JsonArray();

        try {

            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();
    
            while (rs.next()) {
                JsonObject obj = new JsonObject();
    
                for (int i = 1; i <= numColumns; i++) {
                    obj.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                json.add(obj);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while trying to get training data in the database", e);
        }
        
        return json;
    }

    private boolean trainingDataTableExists() throws Exception {

        // search for table macthing expected table name
        DatabaseMetaData md = this.conn.getMetaData();
        ResultSet tables = md.getTables(null, null, "training_data", null);

        // return true if the table exists and false if it does not
        return tables.next() ? true : false;
    }

    private void createTrainingDataTable() throws Exception {

        // create a statement
        Statement stmt = this.conn.createStatement();

        // execute the statement to create the table
        stmt.executeUpdate(SQL_CREATE_TABLE_TRAINING_DATA);

        // close the statement
        stmt.close();
    }

    private boolean labelsTableExists() throws Exception {

        // search for table macthing expected table name
        DatabaseMetaData md = this.conn.getMetaData();
        ResultSet tables = md.getTables(null, null, "labels", null);

        // return true if the table exists and false if it does not
        return tables.next() ? true : false;
    }

    private void createLabelsTable() throws Exception {

        // create a statement
        Statement stmt = this.conn.createStatement();

        // execute the statement to create the table
        stmt.executeUpdate(SQL_CREATE_TABLE_LABELS);

        // close the statement
        stmt.close();
    }

    public void closeConnection() throws Exception {
        if(this.conn != null && !this.conn.isClosed()){
            this.conn.close();
        }
    }

    public Connection getConnection() {
        return this.conn;
    }
}
