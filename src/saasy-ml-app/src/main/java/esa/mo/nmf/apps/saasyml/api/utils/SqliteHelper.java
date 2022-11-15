package esa.mo.nmf.apps.saasyml.api.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.sqlite.SQLiteConfig;

import esa.mo.nmf.apps.PropertiesManager;

public class SqliteHelper {

    private static volatile Connection connection = null;
    private static Object mutex = new Object();

    public static Connection getConnection() throws Exception {

        Connection result = connection;

        // enforce singleton design pattern
        if(result == null) {

            synchronized (mutex) {

                // register the database driver
                Class.forName(PropertiesManager.getInstance().getDatabaseDriver());
    
                // create the connection with the diven database connection configuration
                // by default a single write to the database locks the database for a short time, nothing, even reading, can access the database file at all.
                // use the "Write Ahead Logging" (WAL) option is available to enable reading and writing can proceed concurrently.
                SQLiteConfig config = new SQLiteConfig();
                config.setJournalMode(SQLiteConfig.JournalMode.WAL);
                
                result = connection;

                if (result == null) {
                    
                    connection = result = DriverManager.getConnection(PropertiesManager.getInstance().getDatabaseUrl(), config.toProperties());
                
                }
            }
        }

        // return singleton instance
        return result;
    }
}
