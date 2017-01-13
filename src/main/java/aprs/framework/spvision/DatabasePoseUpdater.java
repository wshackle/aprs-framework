
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

import aprs.framework.database.DbParamTypeEnum;
import aprs.framework.database.DbQueryEnum;
import aprs.framework.database.DbQueryInfo;
import aprs.framework.database.DbSetupBuilder;
import static aprs.framework.database.DbSetupBuilder.DEFAULT_LOGIN_TIMEOUT;
import aprs.framework.database.DbType;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.PoseQueryElem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DatabasePoseUpdater implements AutoCloseable {

    private Connection con;
    private PreparedStatement update_statement;
    private PreparedStatement update_parts_tray_statement;
    private PreparedStatement update_kit_tray_statement;
//    private PreparedStatement addnew_statement;
    private PreparedStatement query_all_statement;
    private PreparedStatement get_single_statement;

//    private org.neo4j.driver.v1.Driver neo4jJavaDriver;
//    private Session neo4jSession;
    private final DbType dbtype;
    private boolean useBatch;

    private boolean verify = false;

    private long totalUpdateTimeMillis;

    /**
     * Get the value of totalUpdateTimeMillis
     *
     * @return the value of totalUpdateTimeMillis
     */
    public long getTotalUpdateTimeMillis() {
        return totalUpdateTimeMillis;
    }

    /**
     * Set the value of totalUpdateTimeMillis
     *
     * @param totalUpdateTimeMillis new value of totalUpdateTimeMillis
     */
    public void setTotalUpdateTimeMillis(long totalUpdateTimeMillis) {
        this.totalUpdateTimeMillis = totalUpdateTimeMillis;
    }

    private long maxUpdateTimeMillis;

    /**
     * Get the value of maxUpdateTimeMillis
     *
     * @return the value of maxUpdateTimeMillis
     */
    public long getMaxUpdateTimeMillis() {
        return maxUpdateTimeMillis;
    }

    /**
     * Set the value of maxUpdateTimeMillis
     *
     * @param maxUpdateTimeMillis new value of maxUpdateTimeMillis
     */
    public void setMaxUpdateTimeMillis(long maxUpdateTimeMillis) {
        this.maxUpdateTimeMillis = maxUpdateTimeMillis;
    }

    private long totalUpdateTimeNanos;

    /**
     * Get the value of totalUpdateTimeNanos
     *
     * @return the value of totalUpdateTimeNanos
     */
    public long getTotalUpdateTimeNanos() {
        return totalUpdateTimeNanos;
    }

    /**
     * Set the value of totalUpdateTimeNanos
     *
     * @param totalUpdateTimeNanos new value of totalUpdateTimeNanos
     */
    public void setTotalUpdateTimeNanos(long totalUpdateTimeNanos) {
        this.totalUpdateTimeNanos = totalUpdateTimeNanos;
    }

    private long maxUpdateTimeNanos;

    /**
     * Get the value of maxUpdateTimeNanos
     *
     * @return the value of maxUpdateTimeNanos
     */
    public long getMaxUpdateTimeNanos() {
        return maxUpdateTimeNanos;
    }

    /**
     * Set the value of maxUpdateTimeNanos
     *
     * @param maxUpdateTimeNanos new value of maxUpdateTimeNanos
     */
    public void setMaxUpdateTimeNanos(long maxUpdateTimeNanos) {
        this.maxUpdateTimeNanos = maxUpdateTimeNanos;
    }

    private int totalUpdates;

    /**
     * Get the value of totalUpdates
     *
     * @return the value of totalUpdates
     */
    public int getTotalUpdates() {
        return totalUpdates;
    }

    /**
     * Set the value of totalUpdates
     *
     * @param totalUpdates new value of totalUpdates
     */
    public void setTotalUpdates(int totalUpdates) {
        this.totalUpdates = totalUpdates;
    }

    private int totalListUpdates;

    /**
     * Get the value of totalListUpdates
     *
     * @return the value of totalListUpdates
     */
    public int getTotalListUpdates() {
        return totalListUpdates;
    }

    /**
     * Set the value of totalListUpdates
     *
     * @param totalListUpdates new value of totalListUpdates
     */
    public void setTotalListUpdates(int totalListUpdates) {
        this.totalListUpdates = totalListUpdates;
    }

    /**
     * Get the value of verify
     *
     * @return the value of verify
     */
    public boolean isVerify() {
        return verify;
    }

    /**
     * Set the value of verify
     *
     * @param verify new value of verify
     */
    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    private boolean delOnUpdate;

    /**
     * Get the value of delOnUpdate
     *
     * @return the value of delOnUpdate
     */
    public boolean isDelOnUpdate() {
        return delOnUpdate;
    }

    /**
     * Set the value of delOnUpdate
     *
     * @param delOnUpdate new value of delOnUpdate
     */
    public void setDelOnUpdate(boolean delOnUpdate) {
        this.delOnUpdate = delOnUpdate;
    }

    public DbType getDbType() {
        return dbtype;
    }

    public Connection getSqlConnection() {
        return con;
    }

    /**
     * Get the value of useBatch
     *
     * @return the value of useBatch
     */
    public boolean isUseBatch() {
        return useBatch;
    }

    /**
     * Set the value of useBatch
     *
     * @param useBatch new value of useBatch
     */
    public void setUseBatch(boolean useBatch) {
        this.useBatch = useBatch;
    }

    private final boolean sharedConnection;

    public DatabasePoseUpdater(
            Connection con,
            DbType dbtype,
            boolean sharedConnection,
            Map<DbQueryEnum, DbQueryInfo> queriesMap) throws SQLException {
        this.dbtype = dbtype;
        this.con = con;
        this.sharedConnection = sharedConnection;
        this.queriesMap = queriesMap;
        setupStatements();
    }

    private String queryAllString;
    private String querySingleString;
    private String queryDeleteSinglePoseString;
    private String updateStatementString;
    private String updatePartsTrayStatementString;
    private String updateKitTrayStatementString;

    private void setupStatements() throws SQLException {
        if (null == queriesMap) {
            throw new IllegalStateException("queriesMap == null");
        }
        if (null == queriesMap.get(DbQueryEnum.SET_SINGLE_POSE)) {
            throw new IllegalStateException("queriesMap.get(DbQueryEnum.SET_SINGLE_POSE) == null");
        }
        switch (dbtype) {
            case MYSQL:
                useBatch = true;
//        con.prepareStatement("update MyTable set questions=? where type = ?").
//                .setString(1,qs.toString())
//                .setString(2,type));
//        update_point_stmt = con.prepareStatement("update Point set hasPoint_X = ?, hasPoint_Y = ?"
//                + " where _NAME = ("
//                + " select hasPoseLocation_Point from PoseLocation"
//                + " where _NAME =  ("
//                + "  select hasSolidObject_PrimaryLocation from SolidObject"
//                + " where _NAME = ? ) )");
//        update_rotation_stmt = con.prepareStatement("update Vector set hasVector_I = ?, hasVector_J = ?"
//                + " where _NAME = ("
//                + " select hasPoseLocation_XAxis from PoseLocation "
//                + " where _NAME =  ("
//                + " select hasSolidObject_PrimaryLocation from SolidObject"
//                + " where _NAME = ? ) )");
//                mergeStatementString = MYSQL_UPDATE_STRING;
//                queryAllString = MYSQL_QUERY_ALL_STRING;
                updateStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE).getQuery();
                queryAllString = queriesMap.get(DbQueryEnum.GET_ALL_POSE).getQuery();
                update_statement = con.prepareStatement(updateStatementString);
                updateKitTrayStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_KT_POSE).getQuery();
                if (updateKitTrayStatementString != null && updateKitTrayStatementString.length() > 0) {
                    update_kit_tray_statement = con.prepareStatement(updateKitTrayStatementString);
                } else {
                    update_kit_tray_statement = update_statement;
                    updateKitTrayStatementString = updateStatementString;
                }
                updatePartsTrayStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_PT_POSE).getQuery();
                if (updateKitTrayStatementString != null && updateKitTrayStatementString.length() > 0) {
                    update_parts_tray_statement = con.prepareStatement(updatePartsTrayStatementString);
                } else {
                    update_parts_tray_statement = update_statement;
                    updatePartsTrayStatementString = updateStatementString;
                }
                updateParamTypes = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE).getParams();
                query_all_statement = con.prepareStatement(queryAllString);
                querySingleString = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getQuery();
                queryDeleteSinglePoseString = queriesMap.get(DbQueryEnum.DELETE_SINGLE_POSE).getQuery();
                get_single_statement = con.prepareStatement(querySingleString);
                getSingleParamTypes = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getParams();

//                updateParamTypes = MYSQL_UPDATE_PARAM_TYPES;
                break;

            case NEO4J:
                useBatch = false;
//                mergeStatementString = NEO4J_MERGE_STATEMENT_STRING;
//                queryAllString = NEO4J_QUERY_ALL_POSES_QUERY_STRING;
                updateStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE).getQuery();
                //System.out.println("mergeStatementString ---> "+mergeStatementString);
                queryAllString = queriesMap.get(DbQueryEnum.GET_ALL_POSE).getQuery();
                update_statement = con.prepareStatement(updateStatementString);

//                addnew_statement = con.prepareStatement(db);
                query_all_statement = con.prepareStatement(queryAllString);
                updateParamTypes = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE).getParams();
                querySingleString = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getQuery();
                queryDeleteSinglePoseString = queriesMap.get(DbQueryEnum.DELETE_SINGLE_POSE).getQuery();
                get_single_statement = con.prepareStatement(querySingleString);
                getSingleParamTypes = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getParams();
                updateKitTrayStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_KT_POSE).getQuery();
                if (updateKitTrayStatementString != null && updateKitTrayStatementString.length() > 0) {
                    update_kit_tray_statement = con.prepareStatement(updateKitTrayStatementString);
                } else {
                    update_kit_tray_statement = update_statement;
                    updateKitTrayStatementString = updateStatementString;
                }
                updatePartsTrayStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_PT_POSE).getQuery();
                if (updateKitTrayStatementString != null && updateKitTrayStatementString.length() > 0) {
                    update_parts_tray_statement = con.prepareStatement(updatePartsTrayStatementString);
                } else {
                    update_parts_tray_statement = update_statement;
                    updatePartsTrayStatementString = updateStatementString;
                }
//                updateParamTypes = NEO4J_MERGE_STATEMENT_PARAM_TYPES;
                break;

//            case NEO4J_BOLT:
//                neo4jJavaDriver = GraphDatabase.driver("bolt://" + host,
//                        AuthTokens.basic(username, password),
//                        Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
//                
//                neo4jSession = neo4jJavaDriver.session();
//                break;
//            case NEO4J_BOLT:
//                neo4jJavaDriver = GraphDatabase.driver("bolt://" + host,
//                        AuthTokens.basic(username, password),
//                        Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
//                
//                neo4jSession = neo4jJavaDriver.session();
//                break;
//            case NEO4J_BOLT:
//                neo4jJavaDriver = GraphDatabase.driver("bolt://" + host,
//                        AuthTokens.basic(username, password),
//                        Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
//                
//                neo4jSession = neo4jJavaDriver.session();
//                break;
//            case NEO4J_BOLT:
//                neo4jJavaDriver = GraphDatabase.driver("bolt://" + host,
//                        AuthTokens.basic(username, password),
//                        Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
//                
//                neo4jSession = neo4jJavaDriver.session();
//                break;
        }
    }
//    private static final String MYSQL_QUERY_ALL_STRING = "select name,X,Y,Z,VXX,VXY from DirectPose";
//    private static final String NEO4J_QUERY_ALL_POSES_QUERY_STRING = "MATCH pointpath=(source) -[:hasPhysicalLocation_RefObject]-> (n) -[r2] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
//            + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
//            + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
//            + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z,xaxis.hasVector_I as vxx,xaxis.hasVector_J as vxy";

    private CompletableFuture<Void> setupConnection(String host, int port, String db, String username, String password, boolean debug) throws SQLException {
        switch (dbtype) {
            case MYSQL:
                useBatch = true;
                break;

            case NEO4J:
                useBatch = false;
                break;
        }
        return DbSetupBuilder.setupConnection(dbtype, host, port, db, username, password, debug, DEFAULT_LOGIN_TIMEOUT)
                .thenAccept(c -> con = c)
                .thenRun(() -> System.out.println("DatabasePoseUpdater connected to database of type " + dbtype + " on host " + host + " with port " + port));
    }

    final private Map<DbQueryEnum, DbQueryInfo> queriesMap;

    public DatabasePoseUpdater(
            String host,
            int port,
            String db,
            String username,
            String password,
            DbType dbtype,
            Map<DbQueryEnum, DbQueryInfo> queriesMap,
            boolean debug) throws SQLException {
        this.dbtype = dbtype;
        sharedConnection = false;
        this.queriesMap = queriesMap;
        setupConnection(host, port, db, username, password, debug)
                .thenRun(() -> {
                    try {
                        setupStatements();
                    } catch (SQLException ex) {
                        Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
    }

//    private static final String MYSQL_UPDATE_STRING
//            = "update Point p, Vector vx, Vector vz "
//            + " set p.hasPoint_X = ?, p.hasPoint_Y = ?, vx.hasVector_I = ?, vx.hasVector_J = ?"
//            + ", vx.hasVector_K=0, vz.hasVector_I=0,vz.hasVector_J=0,vz.hasVector_K=1"
//            + " where p._NAME = ("
//            + " select hasPoseLocation_Point from PoseLocation"
//            + " where _NAME =  ("
//            + "  select hasSolidObject_PrimaryLocation from SolidObject"
//            + " where _NAME = ? ) )"
//            + " and "
//            + "  vx._NAME = ("
//            + " select hasPoseLocation_XAxis from PoseLocation "
//            + " where _NAME =  ("
//            + " select hasSolidObject_PrimaryLocation from SolidObject"
//            + " where _NAME = ? ) )"
//            + " and "
//            + "  vz._NAME = ("
//            + " select hasPoseLocation_ZAxis from PoseLocation "
//            + " where _NAME =  ("
//            + " select hasSolidObject_PrimaryLocation from SolidObject"
//            + " where _NAME = ? ) )";
//    private static final DbParamTypeEnum MYSQL_UPDATE_PARAM_TYPES[] = {
//        DbParamTypeEnum.X, // 1
//        DbParamTypeEnum.Y, // 2
//        DbParamTypeEnum.VXI, // 3
//        DbParamTypeEnum.VXJ, // 4
//        DbParamTypeEnum.NAME, // 5
//        DbParamTypeEnum.NAME, // 6
//        DbParamTypeEnum.NAME // 7
//    };
//    public static final String NEO4J_MERGE_STATEMENT_STRING = "merge (source:SolidObject { name:{2} })\n"
//            + "merge (source) - [:hasPhysicalLocation_RefObject] -> (pl:PhysicalLocation)\n"
//            + "merge (pl) - [:hasPoseLocation_Pose] -> (pose:PoseLocation)\n"
//            + "merge (pose) - [:hasPose_Point] -> (pt:Point)\n"
//            + "merge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\n"
//            + "merge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\n"
//            + "set pt.hasPoint_X= {3},pt.hasPoint_Y= {4},pt.hasPoint_Z={5}\n"
//            + "set xaxis.hasVector_I={6}, xaxis.hasVector_J={7}, xaxis.hasVector_K={8}"
//            + "set zaxis.hasVector_I={9}, zaxis.hasVector_J={10},zaxis.hasVector_K={11}\n";
//    private static final DbParamTypeEnum NEO4J_MERGE_STATEMENT_PARAM_TYPES[] = {
//        //        DbParamTypeEnum.TYPE, // 1
//        DbParamTypeEnum.NAME, // 1
//        DbParamTypeEnum.X, // 2
//        DbParamTypeEnum.Y, // 3
//        DbParamTypeEnum.Z, // 4
//        DbParamTypeEnum.VXI, // 5
//        DbParamTypeEnum.VXJ, // 6
//        DbParamTypeEnum.VXK, // 7
//        DbParamTypeEnum.VZI, // 8
//        DbParamTypeEnum.VZJ, // 9
//        DbParamTypeEnum.VZK// 10
//
//    };
    private DbParamTypeEnum updateParamTypes[] = null;// NEO4J_MERGE_STATEMENT_PARAM_TYPES;
    private DbParamTypeEnum getSingleParamTypes[] = null;//NEO4J_MERGE_STATEMENT_PARAM_TYPES;

    private boolean debug;

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private static double fix(ResultSet rs, String colLabel) throws SQLException {
        String s = rs.getString(colLabel);
        if (null == s) {
//            VisionToDBJFrameInterface displayInterface = DbMain.getDisplayInterface();
//            if (null != displayInterface) {
//                displayInterface.addLogMessage("Null return to from rs.getString(\""+colLabel+"\")");
//            } else {
//                System.err.println("Null return to from rs.getString(\""+colLabel+"\")");
//            }
            return 0.0;
        }
        String peices[] = s.trim().split("[^0-9E+-.]+");
        String fixed = (peices[0].length() > 0 || peices.length < 2) ? s : peices[1];
        return Double.valueOf(fixed);
    }

//    private List<PoseQueryElem> getNeo4jBoltDirectPoseList() {
//        VisionToDBJFrameInterface displayInterface = DbMain.getDisplayInterface();
//        if (null != displayInterface) {
//            debug = displayInterface.isDebug();
//        }
//        StatementResult result
//                = neo4jSession.run("MATCH pointpath=(source) -[r0]-> (n) -[r2:hasPoseLocation_Pose] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
//                        + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
//                        + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
//                        + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,xaxis.hasVector_I as vxx,xaxis.hasVector_J as vxy");
//        while (result.hasNext()) {
//            Record record = result.next();
//            if(debug) {
//                displayInterface.addLogMessage("record ="+record.asMap());
//            }
//            String name = record.get("name").asString();
//            if(debug) {
//                displayInterface.addLogMessage("name ="+name);
//            }
//        }
//        return Collections.emptyList();
//    }
    private List<PoseQueryElem> getDirectPoseList() {
        switch (dbtype) {
            case MYSQL:
            case NEO4J:
                return getJdbcDirectPoseList();

//            case NEO4J_BOLT:
//                return getNeo4jBoltDirectPoseList();
            default:
                throw new IllegalStateException("getDirectPoseList not implemented for dbtype=" + dbtype);
        }
    }

    private VisionToDBJFrameInterface displayInterface;

    /**
     * Get the value of displayInterface
     *
     * @return the value of displayInterface
     */
    public VisionToDBJFrameInterface getDisplayInterface() {
        return displayInterface;
    }

    /**
     * Set the value of displayInterface
     *
     * @param displayInterface new value of displayInterface
     */
    public void setDisplayInterface(VisionToDBJFrameInterface displayInterface) {
        this.displayInterface = displayInterface;
    }

    private List<PoseQueryElem> getJdbcDirectPoseList() {
        List<PoseQueryElem> l = new ArrayList<PoseQueryElem>();
        if (null != displayInterface) {
            debug = displayInterface.isDebug();
        }
        if (debug) {
            displayInterface.addLogMessage("Sending query:" + System.lineSeparator());
            displayInterface.addLogMessage(queryAllString);
            displayInterface.addLogMessage("" + System.lineSeparator());
        }
        List<String> colNames = null;
        try (ResultSet rs = query_all_statement.executeQuery()) {
            if (null != rs) {
                ResultSetMetaData meta = rs.getMetaData();
                colNames = new ArrayList<>();
                for (int colIndex = 1; colIndex < meta.getColumnCount(); colIndex++) {
                    colNames.add(meta.getColumnName(colIndex));
                }
                while (rs.next()) {
                    if (debug) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
                            String columnName = rs.getMetaData().getColumnName(j);
                            String val = rs.getString(columnName);
                            String str = "{ (" + j + "/" + rs.getMetaData().getColumnCount() + ") columnName = " + columnName + ", val = " + val + " }, ";
                            sb.append(str);
                        }
                        if (null != displayInterface) {
                            displayInterface.addLogMessage(sb.toString());
                        }
                    }
                    Map<DbParamTypeEnum, String> resultMap
                            = queriesMap.get(DbQueryEnum.GET_ALL_POSE).getResults();
                    l.add(new PoseQueryElem(rs.getString("name"),
                            fix(rs, resultMap.get(DbParamTypeEnum.X)),
                            fix(rs, resultMap.get(DbParamTypeEnum.Y)),
                            fix(rs, resultMap.get(DbParamTypeEnum.Z)),
                            fix(rs, resultMap.get(DbParamTypeEnum.VXI)),
                            fix(rs, resultMap.get(DbParamTypeEnum.VXJ))
                    ));
                }
            }
        } catch (SQLException sQLException) {
            sQLException.printStackTrace();
            if (null != displayInterface) {
                displayInterface.addLogMessage(sQLException.getMessage() + System.lineSeparator());
                displayInterface.addLogMessage("colNames = " + colNames + System.lineSeparator());
            } else {
                System.err.println("colNames = " + colNames);
            }
        }
        return l;
    }

    @Override
    public String toString() {
        return "DatabasePoseUpdater{" + "con=" + con + ", dbtype=" + dbtype + ", useBatch=" + useBatch + ", verify=" + verify + ", totalUpdateTimeMillis=" + totalUpdateTimeMillis + ", maxUpdateTimeMillis=" + maxUpdateTimeMillis + ", totalUpdateTimeNanos=" + totalUpdateTimeNanos + ", maxUpdateTimeNanos=" + maxUpdateTimeNanos + ", totalUpdates=" + totalUpdates + ", totalListUpdates=" + totalListUpdates + ", sharedConnection=" + sharedConnection + ", queryAllString=" + queryAllString + ", querySingleString=" + querySingleString + ", mergeStatementString=" + updateStatementString + ", updateParamTypes=" + updateParamTypes + ", getSingleParamTypes=" + getSingleParamTypes + ", debug=" + debug + '}';
    }

    @Override
    public void close() {

        System.out.println("Closing " + this);

        try {
            if (null != update_statement) {
                update_statement.close();
                update_statement = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (null != query_all_statement) {
                query_all_statement.close();
                query_all_statement = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (null != get_single_statement) {
                get_single_statement.close();
                get_single_statement = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (null != con && !sharedConnection) {
                con.close();
                con = null;
            }
        } catch (Exception exception) {
            if (null != displayInterface) {
                displayInterface.addLogMessage(exception);
            } else {
                exception.printStackTrace();
            }
        }
        pqExecServ.shutdownNow();
        try {
            pqExecServ.awaitTermination(100, TimeUnit.MILLISECONDS);
//        if (null != neo4jJavaDriver) {
//            neo4jJavaDriver.close();
//            neo4jJavaDriver = null;
//        }
//        if (null != neo4jSession) {
//            neo4jSession.close();
//            neo4jSession = null;
//        }
//        if (null != neo4jJavaDriver) {
//            neo4jJavaDriver.close();
//            neo4jJavaDriver = null;
//        }
//        if (null != neo4jSession) {
//            neo4jSession.close();
//            neo4jSession = null;
//        }
//        if (null != neo4jJavaDriver) {
//            neo4jJavaDriver.close();
//            neo4jJavaDriver = null;
//        }
//        if (null != neo4jSession) {
//            neo4jSession.close();
//            neo4jSession = null;
//        }
//        if (null != neo4jJavaDriver) {
//            neo4jJavaDriver.close();
//            neo4jJavaDriver = null;
//        }
//        if (null != neo4jSession) {
//            neo4jSession.close();
//            neo4jSession = null;
//        }
        } catch (InterruptedException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void finalize() {
        close();
    }

    public static volatile int poses_updated = 0;
    public static volatile List<PoseQueryElem> displayList = null;

    private final Map<String, UpdateResults> updateResultsMap = new HashMap<>();

    private boolean forceUpdates;

    /**
     * Get the value of forceUpdates
     *
     * @return the value of forceUpdates
     */
    public boolean isForceUpdates() {
        return forceUpdates;
    }

    /**
     * Set the value of forceUpdates
     *
     * @param forceUpdates new value of forceUpdates
     */
    public void setForceUpdates(boolean forceUpdates) {
        this.forceUpdates = forceUpdates;
    }

    /**
     * Get the value of updateResultsMap
     *
     * @return the value of updateResultsMap
     */
    public Map<String, UpdateResults> getUpdateResultsMap() {
        return updateResultsMap;
    }

    public List<DetectedItem> updateVisionList(List<DetectedItem> list, boolean addRepeatCountsToName) {
        List<DetectedItem> itemsToVerify = new ArrayList<>();
        List<DetectedItem> partsTrays
                = list.stream()
                .filter((DetectedItem item) -> "PT".equals(item.type))
                .collect(Collectors.toList());
        List<DetectedItem> kitTrays
                = list.stream()
                .filter((DetectedItem item) -> "KT".equals(item.type))
                .collect(Collectors.toList());
        List<DetectedItem> returnedList = new ArrayList<>();
        try {
            long t0_nanos = System.nanoTime();
            long t0_millis = System.currentTimeMillis();
            int updates = 0;
            synchronized (this) {
                if (delOnUpdate) {
                    for (DetectedItem item : list) {
                        deletePose(item.fullName);
                        deletePose(item.name);
                    }
                }
                if (null == update_statement) {
                    throw new IllegalStateException("update_statement == null");
                }
                if (addRepeatCountsToName) {
                    Collections.sort(list, new Comparator<DetectedItem>() {
                        @Override
                        public int compare(DetectedItem o1, DetectedItem o2) {
                            return Double.compare(1000.0 * ((int) (o1.x / 25)) + o1.y, 1000.0 * ((int) (o2.x / 25)) + o2.y);
                        }
                    });
                }
                List<UpdateResults> batchUrs = new ArrayList<>();
                if (null != displayInterface && displayInterface.isDebug()) {
                    debug = true;
                    displayInterface.addLogMessage("Begin updateVisionList");
                } else {
                    debug = false;
                }
                int updatedCount = -1;
                List<String> skippedUpdates = new ArrayList<>();
                Map<String, Integer> repeatsMap = new HashMap<String, Integer>();
                for (int i = 0; i < list.size(); i++) {
                    DetectedItem ci = list.get(i);
                    if (null == ci || ci.name.compareTo("*") == 0) {
                        continue;
                    }
                    if(ci.name.startsWith("sku_")) {
                        ci.name = ci.name.substring(4);
                    }
                    if ("P".equals(ci.type)) {
                        if (ci.insideKitTray || inside(kitTrays, ci)) {
                            ci.fullName = ci.name + "_in_kt";
                            ci.insideKitTray = true;
                        } else if (ci.insidePartsTray || inside(partsTrays, ci)) {
                            ci.fullName = ci.name + "_in_pt";
                            ci.insidePartsTray = true;
                        }
                    }
                    if (ci.name != null && ci.name.length() > 0 && (ci.fullName == null || ci.fullName.length() < 1)) {
                        ci.fullName = ci.name;
                    }
                    
                    PreparedStatement stmnt = update_statement;
                    String statementString = updateStatementString;
                    boolean addRepeatCountsThisItem = addRepeatCountsToName;
                    if (null != ci.type) {
                        switch (ci.type) {
                            case "PT":
                                stmnt = update_parts_tray_statement;
                                statementString = updatePartsTrayStatementString;
                                addRepeatCountsThisItem = true;
                                break;

                            case "KT":
                                stmnt = update_kit_tray_statement;
                                statementString = updateKitTrayStatementString;
                                addRepeatCountsThisItem = true;
                                break;
                        }
                    }
                    if (addRepeatCountsThisItem) {
                        ci.repeats = (repeatsMap.containsKey(ci.name)) ? repeatsMap.get(ci.name) : 0;
                        repeatsMap.put(ci.name, ci.repeats + 1);
                        ci.fullName = ci.name + "_" + (ci.repeats + 1);
                    }
                    returnedList.add(ci);
                    List<Object> paramsList = poseParamsToStatement(ci, updateParamTypes, stmnt);
                    String updateStringFilled = fillQueryString(statementString, paramsList);

                    UpdateResults ur = updateResultsMap.get(ci.fullName);

                    if (null != ur) {
                        if (!forceUpdates
                                && Math.abs(ur.getX() - ci.x) < 1e-6
                                && Math.abs(ur.getY() - ci.y) < 1e-6
                                && Math.abs(ur.getRotation() - ci.rotation) < 1e-6) {
                            skippedUpdates.add(ci.fullName);
                            continue;
                        }
                    } else {
                        ur = new UpdateResults(ci.fullName);
                    }
                    itemsToVerify.add(ci);
                    ur.setException(null);
                    ur.setVerified(false);

                    try {

                        ur.setLastDetectedItem(ci);

                        if (null != displayInterface && displayInterface.isDebug()) {
                            displayInterface.addLogMessage("updateStringFilled = \r\n" + updateStringFilled + "\r\n");
                        }
                        if (useBatch) {
                            update_statement.addBatch();
                            batchUrs.add(ur);
                            updates++;
                        } else {
                            boolean exec_result = update_statement.execute();
                            if (null != displayInterface && displayInterface.isDebug()) {
                                displayInterface.addLogMessage(" update_statement.execute() returned  " + exec_result + "\r\n");
                            }
                            if (exec_result) {
                                try (ResultSet rs = update_statement.getResultSet()) {
                                    if (null != displayInterface && displayInterface.isDebug()) {
                                        displayInterface.addLogMessage("update_statement.getResultSet() = " + rs + "\r\n");
                                    }
                                    List<Map<String, String>> resultSetMapList = new ArrayList<>();
                                    while (rs.next()) {
                                        ResultSetMetaData meta = rs.getMetaData();
                                        Map<String, String> resultMap = new LinkedHashMap<>();
                                        if (null != displayInterface && displayInterface.isDebug()) {
                                            displayInterface.addLogMessage("meta.getColumnCount() = " + meta.getColumnCount() + "\r\n");
                                        }
                                        for (int j = 1; j <= meta.getColumnCount(); j++) {
                                            String name = meta.getColumnName(j);
                                            String value = rs.getObject(name, Object.class).toString();
                                            if (j == 1 && updatedCount < 0 && name.startsWith("count")) {
                                                try {
                                                    updatedCount = Integer.valueOf(value);
                                                } catch (NumberFormatException nfe) {
                                                }
                                            }
                                            resultMap.put(name, value);
                                        }
                                        resultSetMapList.add(resultMap);
                                        if (null != displayInterface
                                                && displayInterface.isDebug()
                                                && resultMap.keySet().size() > 0) {
                                            displayInterface.addLogMessage("resultMap=" + resultMap.toString() + System.lineSeparator());
                                        }
                                    }
                                    ur.setLastResultSetMapList(resultSetMapList);
                                }
                            } else {
                                updatedCount = update_statement.getUpdateCount();
                            }
                            if (null != displayInterface && displayInterface.isDebug()) {
                                displayInterface.addLogMessage("update_statement.execute() returned = " + exec_result + "\r\n");
                                displayInterface.addLogMessage("update_statement.getUpdateCount() returned = " + updatedCount + "\r\n");
                            }
                            if (updatedCount > 1) {
                                updatedCount = 1;
                            }
                            ur.setUpdateCount(updatedCount);
                            ur.setTotalUpdateCount(updatedCount + ur.getTotalUpdateCount());
                            ur.setReturnedResultSet(exec_result);
//                    poses_updated += update_statement.getUpdateCount();
                            poses_updated++;
                        }
                    } catch (Exception exception) {
                        ur.setException(exception);
                    }
                    ur.setUpdateStringFilled(updateStringFilled);
                    ur.setStatementExecutionCount(ur.getStatementExecutionCount() + 1);
                    updateResultsMap.put(ci.fullName, ur);
                    if (null != ur.getException()) {
                        throw new RuntimeException("ur=" + ur, ur.getException());
                    }
                }
                if (null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Skipped updates = " + skippedUpdates);
                }
                if (updates > 0 && useBatch) {
                    int batchReturn[] = update_statement.executeBatch();
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("Batch returns : " + Arrays.toString(batchReturn));
                    }
                    updates = 0;
                    for (int batchIndex = 0; batchIndex < batchReturn.length; batchIndex++) {
                        int br = batchReturn[batchIndex];
                        updates += br;
                        UpdateResults ur = batchUrs.get(batchIndex);
                        updatedCount = br;
                        if (updatedCount > 1) {
                            updatedCount = 1;
                        }
                        ur.setUpdateCount(updatedCount);
                        ur.setTotalUpdateCount(ur.getUpdateCount() + ur.getTotalUpdateCount());
                        updateResultsMap.put(ur.name, ur);
                    }
                    poses_updated += updates;
                }
            }
            long t1_nanos = System.nanoTime();
            long t1_millis = System.currentTimeMillis();
            long millis_diff = t1_millis - t0_millis;
            if (millis_diff > 0) {
                totalUpdateTimeMillis += millis_diff;
            }
            long nanos_diff = t1_nanos - t0_nanos;
            if (nanos_diff > 0) {
                totalUpdateTimeNanos += nanos_diff;
            }
            if (nanos_diff > maxUpdateTimeNanos) {
                maxUpdateTimeNanos = nanos_diff;
            }
            if (millis_diff > maxUpdateTimeMillis) {
                maxUpdateTimeMillis = millis_diff;
            }
            totalListUpdates++;
            totalUpdates = poses_updated;
            if (null != displayInterface && displayInterface.isDebug()) {

                displayInterface.addLogMessage("poses_updated=" + poses_updated);
                displayInterface.addLogMessage("end updateVisionList");
                displayInterface.addLogMessage("updates=" + updates);
                displayInterface.addLogMessage("useBatch=" + useBatch);
                displayInterface.addLogMessage(String.format("updateVisionList took %.3f seconds\n", (1e-9 * nanos_diff)));
            }

            if (verify) {
                this.verifyVisionList(itemsToVerify, addRepeatCountsToName);
            } else if (null != displayInterface) {
                displayInterface.updateResultsMap(updateResultsMap);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            if (null != displayInterface) {
                displayInterface.updateResultsMap(updateResultsMap);
            }
        }
        return returnedList;
    }

    private static boolean inside(List<DetectedItem> trays, DetectedItem ci) {
        return trays.stream().anyMatch((DetectedItem item) -> item.dist(ci) < 65.0);
    }

    public void deletePose(String name) throws SQLException {
        try (PreparedStatement stmnt = con.prepareStatement(queryDeleteSinglePoseString)) {
            stmnt.setString(1, name);
            stmnt.execute();
        }
    }

    public void verifyVisionList(List<DetectedItem> list, boolean addRepeatCountsToName) {
        try {
            if (null == get_single_statement) {
                return;
            }
            long t0 = System.nanoTime();
            int updates = 0;

            synchronized (this) {
                List<UpdateResults> batchUrs = new ArrayList<>();
                if (null != displayInterface && displayInterface.isDebug()) {
                    debug = true;
                    displayInterface.addLogMessage("Begin updateVisionList");
                } else {
                    debug = false;
                }
                int verifiedCount = -1;
                for (int i = 0; i < list.size(); i++) {
                    DetectedItem ci = list.get(i);
                    if (null == ci || ci.name.compareTo("*") == 0) {
                        continue;
                    }
                    if (ci.name != null && ci.name.length() > 0 && (ci.fullName == null || ci.fullName.length() < 1)) {
                        ci.fullName = ci.name;
                    }
                    if (addRepeatCountsToName) {
                        ci.fullName = ci.name + "_" + (ci.repeats + 1);
                    }
                    List<Object> paramsList = poseParamsToStatement(ci, getSingleParamTypes, get_single_statement);
                    UpdateResults ur = updateResultsMap.get(ci.fullName);
                    String verifyQueryStringFilled = fillQueryString(querySingleString, paramsList);
                    if (null == ur) {
                        ur = new UpdateResults(ci.fullName);
                    }
                    ur.setVerifyException(null);
                    ur.setVerified(false);

                    try {
                        if (null != displayInterface && displayInterface.isDebug()) {
                            displayInterface.addLogMessage("verifyQueryStringFilled = \r\n" + verifyQueryStringFilled + "\r\n");
                        }

                        boolean exec_result = get_single_statement.execute();
                        if (null != displayInterface && displayInterface.isDebug()) {
                            displayInterface.addLogMessage(" get_single_statement.execute() returned  " + exec_result + "\r\n");
                        }
                        if (exec_result) {
                            try (ResultSet rs = get_single_statement.getResultSet()) {
                                if (null != displayInterface && displayInterface.isDebug()) {
                                    displayInterface.addLogMessage("get_single_statement.getResultSet() = " + rs + "\r\n");
                                }
                                List<Map<String, String>> resultSetMapList = new ArrayList<>();
                                Map<DbParamTypeEnum, String> resultParamMap
                                        = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getResults();
                                while (rs.next()) {
                                    double x = fix(rs, resultParamMap.get(DbParamTypeEnum.X));
                                    double y = fix(rs, resultParamMap.get(DbParamTypeEnum.Y));
                                    double vxi = fix(rs, resultParamMap.get(DbParamTypeEnum.VXI));
                                    double vxj = fix(rs, resultParamMap.get(DbParamTypeEnum.VXJ));
                                    if (Math.abs(x - ci.x) < 1e-6
                                            && Math.abs(y - ci.y) < 1e-6
                                            && Math.abs(Math.cos(ci.rotation) - vxi) < 1e-6
                                            && Math.abs(Math.sin(ci.rotation) - vxj) < 1e-6) {
                                        ur.setVerified(true);
                                    }
                                    ResultSetMetaData meta = rs.getMetaData();
                                    Map<String, String> resultMap = new LinkedHashMap<>();
                                    if (null != displayInterface && displayInterface.isDebug()) {
                                        displayInterface.addLogMessage("meta.getColumnCount() = " + meta.getColumnCount() + "\r\n");
                                    }
                                    for (int j = 1; j <= meta.getColumnCount(); j++) {
                                        String name = meta.getColumnName(j);
                                        String value = null;
                                        try {
                                            if (null == value) {
                                                value = rs.getString(name);
                                            }
                                        } catch (Exception exception) {
                                        }
                                        try {
                                            if (null == value) {
                                                value = Objects.toString(rs.getObject(name, Object.class));
                                            }
                                        } catch (Exception exception) {
                                        }
                                        if (j == 1 && verifiedCount < 0 && name.startsWith("count")) {
                                            try {
                                                verifiedCount = Integer.valueOf(value);
                                            } catch (NumberFormatException nfe) {
                                            }
                                        }
                                        resultMap.put(name, value);
                                    }
                                    resultSetMapList.add(resultMap);
                                    if (null != displayInterface
                                            && displayInterface.isDebug()
                                            && resultMap.keySet().size() > 0) {
                                        displayInterface.addLogMessage("resultMap=" + resultMap.toString() + System.lineSeparator());
                                    }
                                }
                                ur.setLastVerificationResultSetListMap(resultSetMapList);
                            }
                        } else {
                            verifiedCount = 0;
                        }

                    } catch (Exception exception) {
                        ur.setVerifyException(exception);
                    }
                    ur.setVerificationQueryStringFilled(verifyQueryStringFilled);
                    updateResultsMap.put(ci.fullName, ur);
                    if (null != ur.getVerifyException()) {
                        throw new RuntimeException("ur=" + ur, ur.getVerifyException());
                    }
                }
            }

            if (null != displayInterface && displayInterface.isDebug()) {
                long t1 = System.nanoTime();
                displayInterface.addLogMessage(String.format("verifyVisionList took %.3f seconds\n", (1e-9 * (t1 - t0))));
            }
            if (null != displayInterface) {
                displayInterface.updateResultsMap(updateResultsMap);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            if (null != displayInterface) {
                displayInterface.updateResultsMap(updateResultsMap);
            }
        }

    }
    //    private static class IndexSet {
    //
    //        int xindexes[];
    //        int yindexes[];
    //        int zindexes[];
    //        int vxiIndexes[];
    //        int vxjIndexes[];
    //        int vxkIndexes[];
    //        int vziIndexes[];
    //        int vzjIndexes[];
    //        int vzkIndexes[];
    //        int nameIndexes[];
    //        int typeIndexes[];
    //    }
    //    private static final IndexSet MYSQL_INDEX_SET = new IndexSet();
    //    private static final IndexSet NEO4J_INDEX_SET = new IndexSet();
    //
    //    private IndexSet curIndexSet = MYSQL_INDEX_SET;
    //    
    //    static {
    //        MYSQL_INDEX_SET.xindexes = new int[]{1};
    //        MYSQL_INDEX_SET.yindexes = new int[]{2};
    //        MYSQL_INDEX_SET.zindexes = new int[]{};
    //        MYSQL_INDEX_SET.vxiIndexes = new int[]{3};
    //        MYSQL_INDEX_SET.vxjIndexes = new int[]{4};
    //        MYSQL_INDEX_SET.vxkIndexes = new int[]{};
    //        MYSQL_INDEX_SET.vziIndexes = new int[]{};
    //        MYSQL_INDEX_SET.vzjIndexes = new int[]{};
    //        MYSQL_INDEX_SET.vzkIndexes = new int[]{};
    //        MYSQL_INDEX_SET.nameIndexes = new int[]{5, 6, 7};
    //        MYSQL_INDEX_SET.typeIndexes = new int[]{};
    //        
    //        NEO4J_INDEX_SET.xindexes = new int[]{1};
    //        NEO4J_INDEX_SET.yindexes = new int[]{2};
    //        NEO4J_INDEX_SET.zindexes = new int[]{};
    //        NEO4J_INDEX_SET.vxiIndexes = new int[]{3};
    //        NEO4J_INDEX_SET.vxjIndexes = new int[]{4};
    //        NEO4J_INDEX_SET.vxkIndexes = new int[]{};
    //        NEO4J_INDEX_SET.vziIndexes = new int[]{};
    //        NEO4J_INDEX_SET.vzjIndexes = new int[]{};
    //        NEO4J_INDEX_SET.vzkIndexes = new int[]{};
    //        NEO4J_INDEX_SET.nameIndexes = new int[]{5, 6, 7};
    //        NEO4J_INDEX_SET.typeIndexes = new int[]{};
    //    }
    //    int xindexes[] = {1};
    //    int yindexes[] = {2};
    //    int zindexes[] = {};
    //    int vxiIndexes[] = {3};
    //    int vxjIndexes[] = {4};
    //    int vxkIndexes[] = {};
    //    int vziIndexes[] = {};
    //    int vzjIndexes[] = {};
    //    int vzkIndexes[] = {};
    //    int nameIndexes[] = {5, 6, 7};
    //    int typeIndexes[] = {};
    //    private static enum DbParamTypeEnum {
    //        TYPE, NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK;
    //    }

    private String fillQueryString(String parameterizedQueryString, List<Object> paramsList) {
        String queryStringFilled
                = parameterizedQueryString;
        for (int paramIndex = 1; paramIndex < paramsList.size() + 1; paramIndex++) {
            if (queryStringFilled.indexOf("{" + paramIndex + "}") >= 0) {
                queryStringFilled
                        = queryStringFilled.replace("{" + paramIndex + "}", Objects.toString(paramsList.get(paramIndex - 1)));
            } else if (queryStringFilled.indexOf("?") >= 0) {
                queryStringFilled
                        = queryStringFilled.replace("?", Objects.toString(paramsList.get(paramIndex - 1)));
            }
        }
        return queryStringFilled;
    }

    private List<Object> poseParamsToStatement(DetectedItem item, DbParamTypeEnum paramTypes[], PreparedStatement stmnt) throws SQLException {
        ArrayList<Object> params = new ArrayList<>();
        for (int i = 0; i < paramTypes.length; i++) {
            DbParamTypeEnum paramTypeEnum = paramTypes[i];
            int index = i + 1;
            switch (paramTypeEnum) {
                case TYPE:
                    params.add("SolidObject");
                    stmnt.setString(index, "SolidObject");
                    break;

                case NAME:
                    String quotedName = "\"" + item.fullName + "\"";
                    params.add(quotedName);
                    stmnt.setString(index, item.fullName);
                    break;

                case SKU_NAME:
                    String sku = toSku(item.name);
                    String quotedSKU = "\"" + sku + "\"";
                    params.add(quotedSKU);
                    stmnt.setString(index, sku);
                    break;

                case X:
                    params.add(item.x);
                    stmnt.setDouble(index, item.x);
                    break;

                case Y:
                    params.add(item.y);
                    stmnt.setDouble(index, item.y);
                    break;

                case Z:
                    params.add(item.z);
                    stmnt.setDouble(index, item.z);
                    break;

                case VXI:
                    params.add(item.vxi);
                    stmnt.setDouble(index, item.vxi);
                    break;

                case VXJ:
                    params.add(item.vxj);
                    stmnt.setDouble(index, item.vxj);
                    break;

                case VXK:
                    params.add(item.vxk);
                    update_statement.setDouble(index, item.vxk);
                    break;

                case VZI:
                    params.add(item.vzi);
                    stmnt.setDouble(index, item.vzi);
                    break;

                case VZJ:
                    params.add(item.vzj);
                    stmnt.setDouble(index, item.vzj);
                    break;

                case VZK:
                    params.add(item.vzk);
                    stmnt.setDouble(index, item.vzk);
                    break;

                case VISIONCYCLE:
                    params.add(item.visioncycle);
                    stmnt.setInt(index, item.visioncycle);
                    break;

                default:
                    params.add(null);
                    break;
            }
        }
        return params;
    }

    private static String toSku(String name) {
        String sku = name;
        if(sku.startsWith("sku_")) {
            sku = sku.substring(4);
        }
        sku = "stock_keeping_unit_"+sku;
        return sku;
    }

    private final ExecutorService pqExecServ = Executors.newSingleThreadExecutor();

    public CompletableFuture<List<PoseQueryElem>> queryDatabase() throws InterruptedException, ExecutionException {
        return CompletableFuture.supplyAsync(() -> Collections.unmodifiableList(getDirectPoseList()), pqExecServ);
//                    new Runnable() {
//
//                @Override
//                public void run() {
//                    updating_pose_query = true;
//                    //System.out.println("----> updating_pose_query is true");
//                    final List<PoseQueryElem> l = getDirectPoseList();
//                    if (null != l && null != displayInterface) {
//                        java.awt.EventQueue.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                displayInterface.updataPoseQueryInfo(l);
//                            }
//                        });
//                    }
//                    updating_pose_query = false;
//                }
//            });
//        } else if (!updating_pose_query) {
//            throw new IllegalStateException("PoseDatabaseUpdater: " + this);
//        }
    }

    private volatile boolean updating_pose_query = false;

//    private List<Object> poseParamsToStatement(DetectedItem item) throws SQLException {
//        ArrayList<Object> params = new ArrayList<>();
//        for (int i : curIndexSet.xindexes) {
//            update_statement.setDouble(i, item.x);
//            while (params.size() < i + 1) {
//                params.add(null);
//            }
//            params.set(i, item.x);
//        }
//        for (int i : curIndexSet.yindexes) {
//            update_statement.setDouble(i, item.y);
//            while (params.size() < i + 1) {
//                params.add(null);
//            }
//            params.set(i, item.y);
//        }
//        for (int i : curIndexSet.vxiIndexes) {
//
//            double crot = Math.cos(item.rotation);
//            update_statement.setDouble(i, crot);
//            while (params.size() < i + 1) {
//                params.add(null);
//            }
//            params.set(i, crot);
//        }
//        for (int i : curIndexSet.vxjIndexes) {
//            double srot = Math.sin(item.rotation);
//            update_statement.setDouble(i, srot);
//            while (params.size() < i + 1) {
//                params.add(null);
//            }
//            params.set(i, srot);
//        }
//        for (int i : curIndexSet.nameIndexes) {
//            update_statement.setString(i, item.fullName);
//            while (params.size() < i + 1) {
//                params.add(null);
//            }
//            params.set(i, item.fullName);
//        }
//        return params;
//    }
//    public boolean updatePose(String name, double x, double y, double rotation) throws SQLException {
//        this.poseParamsToStatement(name, x, y, rotation);
//        VisionToDBJFrameInterface displayInterface = DbMain.getDisplayInterface();
//        if (null != displayInterface && displayInterface.isDebug()) {
//            displayInterface.addLogMessage(update_statement.toString());
//        }
//        boolean ex_result = update_statement.execute();
//        if (null != displayInterface && displayInterface.isDebug()) {
//            displayInterface.addLogMessage("execute() returned   " + ex_result
//                    + ", update count = " + update_statement.getUpdateCount());
//        }
//        return ex_result;
//    }
}
