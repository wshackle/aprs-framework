/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
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
package aprs.database;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author shackle
 */
public class CsvDbSetup implements DbSetup {

    public  CsvDbSetup() throws IOException {
        this.queriesMap = DbSetupBuilder.readResourceQueriesDirectory("aprs/database/neo4j/v2/");
    }
    @Override
    public DbType getDbType() {
        return DbType.NONE;
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public String getDbName() {
        return "";
    }

    @Override
    public String getDbUser() {
        return "";
    }

    @Override
    public char[] getDbPassword() {
        return "".toCharArray();
    }

    private boolean connected = false;

    @Override
    public boolean isConnected() {
        return connected;
    }

    private final Map<DbQueryEnum, DbQueryInfo>  queriesMap;
    
    @Override
    public Map<DbQueryEnum, DbQueryInfo> getQueriesMap() {
        return queriesMap;
    }

    @Override
    public boolean isInternalQueriesResourceDir() {
        return false;
    }

    @Override
    public String getQueriesDir() {
        return "";
    }

    @Override
    public boolean isDebug() {
        return false;
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public String getStartScript() {
        return "";
    }
}
