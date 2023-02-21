package esa.mo.nmf.apps.saasyml.api.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.sqlite.SQLiteConfig;

import esa.mo.nmf.apps.PropertiesManager;

public class SqliteHelper {

    // logger
    private static final Logger LOGGER = Logger.getLogger(SqliteHelper.class.getName());

    private static volatile Connection connection = null;
    private static Object mutex = new Object();

    public static Connection getConnection() throws Exception {

        Connection result = connection;

        // enforce singleton design pattern
        if(result == null) {

            synchronized (mutex) {

                // register the database driver
                Class.forName(PropertiesManager.getInstance().getDatabaseDriver());
    
                // create the connection with the database connection configuration given in the app's config file
                SQLiteConfig config = new SQLiteConfig();

                // get and set the database journal mode
                SQLiteConfig.JournalMode journalMode = SqliteHelper.getJournalMode(PropertiesManager.getInstance().getDatabaseJournalMode());
                if(journalMode != null) {
                    LOGGER.log(Level.INFO, "Set database journal mode to {0}.", journalMode.toString());
                    config.setJournalMode(journalMode);
                }else {
                    LOGGER.log(Level.INFO, "Use default database journal mode.");
                }

                // set the journal size limit
                int journalSizeLimit = PropertiesManager.getInstance().getDatabaseJournalSizeLimit();

                // the missing "r" in JouRnal for setJounalSizeLimit is probably a typo in the sqlite-jdbc library:
                // https://www.javadoc.io/doc/org.xerial/sqlite-jdbc/3.32.3.3/org/sqlite/SQLiteConfig.html#setJounalSizeLimit-int-
                config.setJounalSizeLimit(journalSizeLimit);
                
                // set the connection
                result = connection;

                if (result == null) {
                    
                    connection = result = DriverManager.getConnection(PropertiesManager.getInstance().getDatabaseUrl(), config.toProperties());
                
                }
            }
        }

        // return singleton instance
        return result;
    }

    private static SQLiteConfig.JournalMode getJournalMode(String mode){
        if(mode.equalsIgnoreCase("DELETE")){
            // in the Delete mode, the rollback journal is deleted at the conclusion of each transaction.
            return SQLiteConfig.JournalMode.DELETE;

        } else if(mode.equalsIgnoreCase("TRUNCATE")){
            // in the Truncate mode rollback journal file is truncated instead of deleting when the transaction is commited.
            // may be used for gaining better performance, because on many systems, truncating a file is much faster than deleting the file.
            return SQLiteConfig.JournalMode.TRUNCATE;

        } else if(mode.equalsIgnoreCase("PERSIST")){
            // in the Persist mode rollback journal file is not deleted when the transaction is commited.
            // its first block filled with zeroes to prevent other connections rolling back from this journal.
            // may optimize performance on platforms where deleting or truncating a file is much more expensive than overwriting the first block of a file with zeros.
            return SQLiteConfig.JournalMode.PERSIST;

        } else if(mode.equalsIgnoreCase("MEMORY")){
            // the MEMORY journaling mode stores the rollback journal in volatile RAM.
            // it may be used to reduce disk I/O but that decreases database safety and integrity.
            // if the application using SQLite crashes in the middle of a transaction in this mode, the database file may become corrupt.
            return SQLiteConfig.JournalMode.MEMORY;

        } else if(mode.equalsIgnoreCase("WAL")){
            // in the WAL mode, Write-Ahead Logging is used instead of the standard rollback journal.
            // this means that when database data is updated, the original content is preserved in the database file,
            // and the changes are appended into a separate WAL file and eventually are transferred to the database.

            // by default a single write to the database locks the database for a short time, nothing, even reading, can access the database file at all.
            // use the WAL option is available to enable reading and writing can proceed concurrently.
            return SQLiteConfig.JournalMode.WAL;

        } else if(mode.equalsIgnoreCase("OFF")){
            // in this mode rollback journal is completely disabled. The ROLLBACK command does not work; it behaves in an undefined way.
            // don't use the ROLLBACK command in this mode.
            return SQLiteConfig.JournalMode.OFF;
        }

        // unrecognized journal mode
        return null;
    }
}
