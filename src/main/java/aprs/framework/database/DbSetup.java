/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.framework.database;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Information for providing database setup information.
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface DbSetup {

    /**
     * Get the database type eg MySQL, Neo4J etc.
     * 
     * @return database type
     */
    public DbType getDbType();

    /**
     * Get the host name for connecting to the database.
     * @return host name
     */
    public String getHost();

    /**
     * Get the TCP IP port number for connecting to the database.
     * 
     * @return port number
     */
    public int getPort();

    /**
     * Get the optional name for the particular database.
     * MySQL can have multiple databases with different names on the same port/host.
     * 
     * @return name of the database.
     */
    public String getDbName();

    /**
     * Get the user/account name that will be used for logging into the database.
     * 
     * @return user name
     */
    public String getDbUser();

    /**
     * Get the password needed to connect to the database.
     * 
     * @return password.
     */
    public char[] getDbPassword();

    /**
     * Determine if we are currently already connected to this database.
     * 
     * @return connected state
     */
    public boolean isConnected();
    
    /**
     * Get a map from the various database independent  but APRS specific
     * types of queries to the query information objects needed to make
     * that type of query on the current database.
     * 
     * @return map of query types to query info
     */
    public Map<DbQueryEnum,DbQueryInfo> getQueriesMap();
    
    /**
     * Determine if this setup is/should be taken from an internal
     * queries resource directory.
     * 
     * @return setup source internal.
     */
    public boolean isInternalQueriesResourceDir();
    
    /**
     * Get the directory name either in an external file system or 
     * an internal resource where the static setup information is 
     * stored.
     * 
     * @return queries directory.
     */
    public String getQueriesDir();
    
    /**
     * Has the user requested additional logging to debug database setup.
     * 
     * @return  user wants debug info
     */
    public boolean isDebug();

    /**
     * Get the maximum time in seconds that a driver will wait while attempting
     * to connect to a database once the driver has been identified.
     * Values less than zero imply no timeout.
     * (Currently ignored for Neo4J).
     * 
     * @return login timeout
     */
    public int getLoginTimeout();
    
    /**
     * Optional script that can be executed to start the database server.
     * 
     * @return server start script or null
     */
    @Nullable public String getStartScript();
}
