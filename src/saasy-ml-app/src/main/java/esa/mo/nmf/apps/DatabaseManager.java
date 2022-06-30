package esa.mo.nmf.apps;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

public class DatabaseManager {

    // logger
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static volatile DatabaseManager instance;
    private static Object mutex = new Object();

    // database connection
    private Connection conn = null;
    
    // hide the constructor
    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        // the local variable result seems unnecessary but it's there to improve performance
        // in cases where the instance is already initialized (most of the time), the volatile field is only accessed once (due to "return result;" instead of "return instance;").
        // this can improve the method’s overall performance by as much as 25 percent.
        // source: https://www.journaldev.com/171/thread-safety-in-java-singleton-classes
        DatabaseManager result = instance;
        
        // enforce Singleton design pattern
        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new DatabaseManager();
            }
        }
        
        // return singleton instance
        return result;
    }

    public Connection connect() throws Exception {
        if(this.conn == null)
        {
            try {
                // register the database driver
                Class.forName(PropertiesManager.getInstance().getDatabaseDriver());
                
                // create the connection with the diven database connection configuration
                this.conn = DriverManager.getConnection(PropertiesManager.getInstance().getDatabaseUrl());
                
                if (this.conn != null) {
                    // log error
                    LOGGER.log(Level.INFO, "Database connection created successfully.");
    
                    // check if training data table exists and create it if it does not.
                    boolean tableExists = this.trainingDataTableExists();
                    if(!tableExists) {
                        LOGGER.log(Level.INFO, "Create the training data table already exists.");
                        this.createTrainingDataTable();
                    }else {
                        LOGGER.log(Level.INFO, "The training data table already exists.");
                    }
                   
                }else {
                    this.conn = null;
                    throw new Exception("Failed to create database connection");
                }
            } catch (Exception e) {
    
                // close connection in case it is open despite the exception.
                if(this.conn != null && !this.conn.isClosed()) {
                    this.conn.close();
                    this.conn = null;
                }
    
                
            }            
        }

        return this.conn;
    }

    public void closeConnection() throws Exception {
        if(this.conn != null && !this.conn.isClosed()){
            this.conn.close();
        }
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

        // the sql string to create the table
        String sql = "CREATE TABLE IF NOT EXISTS training_data(" +
            "exp_id INTEGER NOT NULL, " +
            "dataset_id INTEGER NOT NULL, " +
            "param_name TEXT NOT NULL, " +
            "rank INTEGER NOT NULL, " +
            "data_type INTEGER NOT NULL, " +
            "value TEXT NOT NULL, " +
            "timestamp TIMESTAMP NOT NULL, " +
            "PRIMARY KEY(exp_id, dataset_id, param_name, rank)" +
        ")";

        // execute the statement to create the table
        stmt.executeUpdate(sql);

        // close the statement
        stmt.close();
    }

    public int getLastCounterValue(int expId, int datasetId) throws Exception {
        // create the prepared statement to fetch the latst counter value from the database
        PreparedStatement psCounter = this.conn.prepareStatement(
            "SELECT rank FROM training_data WHERE exp_id = ? AND dataset_id = ? ORDER BY rowid DESC LIMIT 1"
        );

        // set the filter parameters
        psCounter.setInt(1, expId);
        psCounter.setInt(2, datasetId);

        // execute the query
        ResultSet rsCounter = psCounter.executeQuery();

        // if a result is returned then for the given experiment id and dataset id:
        //   - there is a counter value we left off from a previous training data collection run.
        //   - we can resume the counter from that latest counter value.
        if(rsCounter.next()) {
            return rsCounter.getInt(1);
        }else{
            return -1;
        }
    }

    public void insertTrainingData(int expId, int datasetId, List<String> paramNames, int receivedDataCounter, List<Pair<Integer, String>> paramValues, Long timestamp) throws Exception {
        // create the prepared statement
        PreparedStatement prep = conn.prepareStatement(
            "INSERT INTO training_data(exp_id, dataset_id, param_name, rank, data_type, value, timestamp) VALUES(?, ?, ?, ?, ?, ?, ?)"
        );

        // prepare a prepared statement for each fetched parameter
        for (int i = 0; i < paramValues.size(); i++) {

            // set satement parameters
            prep.setInt(1, expId); // experiment id
            prep.setInt(2, datasetId); // dataset id
            prep.setString(3, paramNames.get(i)); // get param name
            prep.setInt(4, receivedDataCounter); // the rank
            prep.setInt(5, paramValues.get(i).getKey()); // the data type short form
            prep.setString(6, paramValues.get(i).getValue()); // the param name value as a string
            prep.setTimestamp(7, new Timestamp(timestamp)); // the timestamp returned by NMF marking when the data was fetched

            // add to batch
            prep.addBatch();
        }

        // execute the batch insert into the table
        prep.executeBatch();
    }

    public Connection getConnection() {
        return this.conn;
    }
}
