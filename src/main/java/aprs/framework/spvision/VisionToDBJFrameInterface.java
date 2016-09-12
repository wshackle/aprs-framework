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
package aprs.framework.spvision;

import aprs.framework.database.CommonJFrameInterface;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.DbType;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.PoseQueryElem;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface VisionToDBJFrameInterface extends CommonJFrameInterface {

    public void setAquiring(String s);

    public void updateInfo(List<DetectedItem> _list, String line);

    public void updataPoseQueryInfo(final List<PoseQueryElem> _list);
    
    public void updateResultsMap(final Map<String, UpdateResults> _map);

    public boolean isDebug();

    public void addLogMessage(String stmnt);

    public void addLogMessage(Exception exception);

    public void setVisionConnected(boolean _val);

    public void setDBConnected(boolean _val);

    public void setLastCommand(String c);

    public void setCommandConnected(boolean _val);

    public void updateFromArgs(Map<String, String> _argsMap);

    public void setVisible(boolean v);

    public Map<String, String> updateArgsMap();
    
    public java.sql.Connection getSqlConnection();
    
    public DbType getDbType();
    
    public void setSqlConnection(java.sql.Connection connection, DbType dbtype) throws java.sql.SQLException;
    
    public Callable<DbSetupPublisher> getDbSetupSupplier();

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier);
    
    public void connectVision();
}
