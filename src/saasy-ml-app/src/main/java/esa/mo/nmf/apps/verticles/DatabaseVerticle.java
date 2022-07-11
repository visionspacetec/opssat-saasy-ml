package esa.mo.nmf.apps.verticles;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
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
import java.sql.Statement;
import java.sql.Timestamp;


import org.sqlite.SQLiteConfig;
import esa.mo.nmf.apps.PropertiesManager;

public class DatabaseVerticle extends AbstractVerticle {

    // logger
    private static final Logger LOGGER = Logger.getLogger(DatabaseVerticle.class.getName());

    // database connection
    private Connection conn = null;

    // sql query strings
    private final String SQL_INSERT_TRAINING_DATA = "INSERT INTO training_data(exp_id, dataset_id, param_name, data_type, value, timestamp) VALUES(?, ?, ?, ?, ?, ?)";
    private final String SQL_DELETE_TRAINING_DATA = "DELETE FROM training_data WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_COUNT_TRAINING_DATA = "SELECT count(*) FROM  training_data WHERE exp_id = ? AND dataset_id = ?";
    private final String SQL_SELECT_TRAINING_DATA = "SELECT * FROM training_data WHERE exp_id = ? AND dataset_id = ? ORDER BY timestamp DESC";
    private final String SQL_CREATE_TABLE_TRAINING_DATA = 
        "CREATE TABLE IF NOT EXISTS training_data(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "param_name TEXT NOT NULL, " +
            "data_type INTEGER NOT NULL, " +
            "value TEXT NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL" +
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
                    
                }else {
                    this.conn = null;
                    throw new Exception("Failed to create database connection");
                }
            } catch (Exception e) {
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
        vertx.eventBus().consumer("saasyml.training.data.save", msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();
            final JsonArray data = payload.getJsonArray("data");

            List<Pair<Integer, String>> paramValues = new ArrayList<Pair<Integer, String>>();
            List<String> paramNames = new ArrayList<String>();
            List<Long> timestamps = new ArrayList<Long>();

            try {
                // the prepared statement
                PreparedStatement prep = this.conn.prepareStatement(
                    SQL_INSERT_TRAINING_DATA
                );

                // fetch data
                data.forEach(dataset -> {
                    JsonArray ds = (JsonArray) dataset;

                    ds.forEach(param -> {
                        JsonObject p = (JsonObject) param;

                        // parameter name
                        paramNames.add(p.getString("name"));

                        // parameter type and value value
                        int dataType = p.getInteger("dataType");
                        String value = p.getString("value");
                        paramValues.add(new Pair<Integer, String>(dataType, value));
                        
                        // the timestamp
                        timestamps.add(p.getLong("timestamp"));
                    });
                });

                // create the prepared statement and execute
                try {
                    this.setInsertTrainingDataPreparedStatementParameters(prep, expId, datasetId, paramNames, paramValues, timestamps);
                    prep.executeBatch();

                    // auto-trigger training if the payload is configured to do so
                    if(payload.containsKey("training")) {

                        // the training parameters can be for more than one algorithm
                        final JsonArray trainings = payload.getJsonArray("training");

                        // trigger training for each request
                        for(int i = 0; i < trainings.size(); i++) {
                            final JsonObject t = trainings.getJsonObject(i);

                            // fetch training algorithm selection
                            String type = t.getString("type");

                            // trigger training
                            vertx.eventBus().send("saasyml.training." + type, payload);
                        }
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to insert training data into the database.", e);
                }  
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to insert training data into the database", e);
            }

            // response
            msg.reply("saved training data.");
        });

        // delete training data
        vertx.eventBus().consumer("saasyml.training.data.delete", msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();

            try {
                this.deleteTrainingData(expId, datasetId);
                
                // response
                msg.reply("deleted training data.");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to delete training data from the database", e);

                // response
                msg.reply("error deleting training data.");
            }
        });

        // count training data
        vertx.eventBus().consumer("saasyml.training.data.count", msg -> {

            // the request payload (Json)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the Json payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();

            // count training data records
            int counter = -1;
            try {
                counter = this.countTrainingData(expId, datasetId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to count training data rows in the database", e);
            }

            // response
            JsonObject response = new JsonObject();
            response.put("count", counter);
            msg.reply(response);
            
        });

        // select training data
        vertx.eventBus().consumer("saasyml.training.data.select", msg -> {

            // the request payload (JSON)
            JsonObject payload = (JsonObject) (msg.body());

            // parse the JSON payload
            final int expId = payload.getInteger("expId").intValue();
            final int datasetId = payload.getInteger("datasetId").intValue();

            // select training data records
            JsonArray data = null;
            try {
                data = this.selectTrainingData(expId, datasetId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while trying to get training data in the database", e);
            }
            
            // response
            JsonObject response = new JsonObject();
            response.put("data", data);
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
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_DELETE_TRAINING_DATA);

        // set satement parameters
        ps.setInt(1, expId); // experiment id
        ps.setInt(2, datasetId); // dataset id

        // execute the delete statement
        ps.executeUpdate();
    }

    private int countTrainingData(int expId, int datasetId) throws Exception {
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_COUNT_TRAINING_DATA);

        // set satement parameters
        ps.setInt(1, expId); // experiment id
        ps.setInt(2, datasetId); // dataset id

        // execute the delete statement
        ResultSet rs = ps.executeQuery();

        // return the result
        rs.next();
        return rs.getInt(1);
    }
    
    private JsonArray selectTrainingData(int expId, int datasetId) throws Exception {
        
        // create the prepared statement
        PreparedStatement ps = this.conn.prepareStatement(SQL_SELECT_TRAINING_DATA);

        // set statement parameters
        ps.setInt(1, expId);
        ps.setInt(2, datasetId);

        // execute the select statement
        ResultSet rs = ps.executeQuery();

        // return the result
        return toJSON(rs);

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

        // returm true if the table exists and false if it does not
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

    public void closeConnection() throws Exception {
        if(this.conn != null && !this.conn.isClosed()){
            this.conn.close();
        }
    }

    public Connection getConnection() {
        return this.conn;
    }
}
