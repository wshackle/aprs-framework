
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

import aprs.framework.spvision.VisionToDBJFrameInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DatabasePoseUpdater implements AutoCloseable {

    private Connection con;
    private PreparedStatement update_statement;
//    private PreparedStatement addnew_statement;
    private PreparedStatement query_all_statement;
//    private org.neo4j.driver.v1.Driver neo4jJavaDriver;
//    private Session neo4jSession;

    private final DbType dbtype;
    private boolean useBatch;

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

    public DatabasePoseUpdater(Connection con, DbType dbtype, boolean sharedConnection,Map<DbQueryEnum,String> queriesMap) throws SQLException {
        this.dbtype = dbtype;
        this.con = con;
        this.sharedConnection = sharedConnection;
        this.queriesMap = queriesMap;
        setupStatements();
    }

    private String queryAllString;
    private String mergeStatementString;
    
    private void setupStatements() throws SQLException {
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
                mergeStatementString = MYSQL_UPDATE_STRING;
                update_statement = con.prepareStatement(mergeStatementString);
                queryAllString = MYSQL_QUERY_ALL_STRING;
                query_all_statement = con.prepareStatement(queryAllString);

                updateParamTypes = MYSQL_UPDATE_PARAM_TYPES;

                break;

            case NEO4J:
                useBatch = false;
                mergeStatementString = NEO4J_MERGE_STATEMENT_STRING;
                update_statement = con.prepareStatement(mergeStatementString);

//                addnew_statement = con.prepareStatement(db);
                queryAllString = NEO4J_QUERY_ALL_POSES_QUERY_STRING;
                query_all_statement = con.prepareStatement(queryAllString);

                updateParamTypes = NEO4J_MERGE_STATEMENT_PARAM_TYPES;
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
    private static final String MYSQL_QUERY_ALL_STRING = "select name,X,Y,Z,VXX,VXY from DirectPose";
    private static final String NEO4J_QUERY_ALL_POSES_QUERY_STRING = "MATCH pointpath=(source) -[:hasPhysicalLocation_RefObject]-> (n) -[r2] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
            + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
            + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
            + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z,xaxis.hasVector_I as vxx,xaxis.hasVector_J as vxy";

    private void setupConnection(String host, int port, String db, String username, String password) throws SQLException {
        switch (dbtype) {
            case MYSQL:
                useBatch = true;
                break;

            case NEO4J:
                useBatch = false;
                break;
        }
        con = DbSetupBuilder.setupConnection(dbtype, host, port, db, username, password);
    }
    
    final private Map<DbQueryEnum,String> queriesMap;

    public DatabasePoseUpdater(String host, int port, String db, String username, String password, DbType dbtype, Map<DbQueryEnum,String> queriesMap) throws SQLException {
        this.dbtype = dbtype;
        sharedConnection = false;
        this.queriesMap = queriesMap;
        setupConnection(host, port, db, username, password);
        setupStatements();
    }

    private static final String MYSQL_UPDATE_STRING
            = "update Point p, Vector vx, Vector vz "
            + " set p.hasPoint_X = ?, p.hasPoint_Y = ?, vx.hasVector_I = ?, vx.hasVector_J = ?"
            + ", vx.hasVector_K=0, vz.hasVector_I=0,vz.hasVector_J=0,vz.hasVector_K=1"
            + " where p._NAME = ("
            + " select hasPoseLocation_Point from PoseLocation"
            + " where _NAME =  ("
            + "  select hasSolidObject_PrimaryLocation from SolidObject"
            + " where _NAME = ? ) )"
            + " and "
            + "  vx._NAME = ("
            + " select hasPoseLocation_XAxis from PoseLocation "
            + " where _NAME =  ("
            + " select hasSolidObject_PrimaryLocation from SolidObject"
            + " where _NAME = ? ) )"
            + " and "
            + "  vz._NAME = ("
            + " select hasPoseLocation_ZAxis from PoseLocation "
            + " where _NAME =  ("
            + " select hasSolidObject_PrimaryLocation from SolidObject"
            + " where _NAME = ? ) )";

    private static final ParamTypeEnum MYSQL_UPDATE_PARAM_TYPES[] = {
        ParamTypeEnum.X, // 1
        ParamTypeEnum.Y, // 2
        ParamTypeEnum.VXI, // 3
        ParamTypeEnum.VXJ, // 4
        ParamTypeEnum.NAME, // 5
        ParamTypeEnum.NAME, // 6
        ParamTypeEnum.NAME // 7
    };

    public static final String NEO4J_MERGE_STATEMENT_STRING = "merge (source:SolidObject { name:{2} })\n"
            + "merge (source) - [:hasPhysicalLocation_RefObject] -> (pl:PhysicalLocation)\n"
            + "merge (pl) - [:hasPoseLocation_Pose] -> (pose:PoseLocation)\n"
            + "merge (pose) - [:hasPose_Point] -> (pt:Point)\n"
            + "merge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\n"
            + "merge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\n"
            + "set pt.hasPoint_X= {3},pt.hasPoint_Y= {4},pt.hasPoint_Z={5}\n"
            + "set xaxis.hasVector_I={6}, xaxis.hasVector_J={7}, xaxis.hasVector_K={8}"
            + "set zaxis.hasVector_I={9}, zaxis.hasVector_J={10},zaxis.hasVector_K={11}\n";

    private static final ParamTypeEnum NEO4J_MERGE_STATEMENT_PARAM_TYPES[] = {
        ParamTypeEnum.TYPE, // 1
        ParamTypeEnum.NAME, // 2
        ParamTypeEnum.X, // 3
        ParamTypeEnum.Y, // 4
        ParamTypeEnum.Z, // 5
        ParamTypeEnum.VXI, // 6
        ParamTypeEnum.VXJ, // 7
        ParamTypeEnum.VXK, // 8
        ParamTypeEnum.VZI, // 9
        ParamTypeEnum.VZJ, // 10
        ParamTypeEnum.VZK// 11

    };

    private ParamTypeEnum updateParamTypes[] = NEO4J_MERGE_STATEMENT_PARAM_TYPES;

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
//            VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
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
//        VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
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
    public List<PoseQueryElem> getDirectPoseList() {
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

    private List<PoseQueryElem> getJdbcDirectPoseList() {
        List<PoseQueryElem> l = new ArrayList<PoseQueryElem>();
        VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
        if (null != displayInterface) {
            debug = displayInterface.isDebug();
        }
        if(debug) {
            displayInterface.addLogMessage("Sending query:"+System.lineSeparator());
            displayInterface.addLogMessage(queryAllString);
            displayInterface.addLogMessage(""+System.lineSeparator());
        }
        try (ResultSet rs = query_all_statement.executeQuery()) {
            if (null != rs) {
                while (rs.next()) {
                    if (debug) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
                            String columnName = rs.getMetaData().getColumnName(j);
                            String val = rs.getString(columnName);
                            String str = "{ (" + j + "/" + rs.getMetaData().getColumnCount() + ") columnName = " + columnName + ", val = " + val+" }, ";
                            sb.append(str);
                        }
                        if (null != displayInterface) {
                            displayInterface.addLogMessage(sb.toString());
                        }
                    }
                    l.add(new PoseQueryElem(rs.getString("name"),
                            fix(rs, "x"),
                            fix(rs, "y"),
                            fix(rs, "z"),
                            fix(rs, "vxx"),
                            fix(rs, "vxy")
                    ));
                }
            }
        } catch (SQLException sQLException) {
            sQLException.printStackTrace();
        }
        return l;
    }

    @Override
    public void close() {

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
            if (null != con && !sharedConnection) {
                con.close();
                con = null;
            }
        } catch (Exception exception) {
            if (null != Main.getDisplayInterface()) {
                Main.getDisplayInterface().addLogMessage(exception);
            } else {
                exception.printStackTrace();
            }
        }
//        if (null != neo4jJavaDriver) {
//            neo4jJavaDriver.close();
//            neo4jJavaDriver = null;
//        }
//        if (null != neo4jSession) {
//            neo4jSession.close();
//            neo4jSession = null;
//        }
    }

    protected void finalize() {
        close();
    }

    public static volatile int poses_updated = 0;
    public static volatile List<PoseQueryElem> displayList = null;

    public void updateVisionList(List<DetectedItem> list, boolean addRepeatCountsToName) {
        try {
            if (null == update_statement) {
                return;
            }
            int updates = 0;
            VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
            if (null != displayInterface && displayInterface.isDebug()) {
                debug = true;
                displayInterface.addLogMessage("Begin updateVisionList");
            } else {
                debug = false;
            }
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
                List<Object> paramsList = poseParamsToStatement(ci);
                if (null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("update_statement = \r\n" + update_statement.toString() + "\r\n");

                    if (debug && dbtype == DbType.NEO4J) {
                        String updateStringFilled
                                = NEO4J_MERGE_STATEMENT_STRING;
                        for (int paramIndex = 1; paramIndex < paramsList.size() + 1; paramIndex++) {
                            updateStringFilled
                                    = updateStringFilled.replace("{" + paramIndex + "}", Objects.toString(paramsList.get(paramIndex - 1)));
                        }
                        displayInterface.addLogMessage("updateStringFilled = " + updateStringFilled + "\n");
                    }
//                    displayInterface.addLogMessage("update_statement.query = "+update_statement.+"\n");

                }
                if (useBatch) {
                    update_statement.addBatch();
                    updates++;
                } else {
                    boolean exec_result = update_statement.execute();
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("update_statement.execute() returned = " + exec_result + "\r\n");
                        displayInterface.addLogMessage("update_statement.getUpdateCount() returned = " + update_statement.getUpdateCount() + "\r\n");
                    }
//                    poses_updated += update_statement.getUpdateCount();
                    poses_updated++;
                }
            }
            if (null != displayInterface && displayInterface.isDebug()) {
                displayInterface.addLogMessage("poses_updated=" + poses_updated);
                displayInterface.addLogMessage("end updateVisionList");
                displayInterface.addLogMessage("updates=" + updates);
                displayInterface.addLogMessage("useBatch=" + useBatch);
            }
            if (updates > 0 && useBatch) {
                int ia[] = update_statement.executeBatch();
                if (null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Batch returns : " + Arrays.toString(ia));
                }
                updates = 0;
                for (int i : ia) {
                    updates += i;
                }
                poses_updated += updates;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
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
    private static enum ParamTypeEnum {
        TYPE, NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK;
    }

    private List<Object> poseParamsToStatement(DetectedItem item) throws SQLException {
        ArrayList<Object> params = new ArrayList<>();
        for (int i = 0; i < updateParamTypes.length; i++) {
            ParamTypeEnum paramTypeEnum = updateParamTypes[i];
            int index = i + 1;
            switch (paramTypeEnum) {
                case TYPE:
                    params.add("SolidObject");
                    update_statement.setString(index, "SolidObject");
                    break;

                case NAME:
                    params.add(item.fullName);
                    update_statement.setString(index, item.fullName);
                    break;

                case X:
                    params.add(item.x);
                    update_statement.setDouble(index, item.x);
                    break;

                case Y:
                    params.add(item.y);
                    update_statement.setDouble(index, item.y);
                    break;

                case Z:
                    params.add(item.z);
                    update_statement.setDouble(index, item.z);
                    break;

                case VXI:
                    params.add(item.vxi);
                    update_statement.setDouble(index, item.vxi);
                    break;

                case VXJ:
                    params.add(item.vxj);
                    update_statement.setDouble(index, item.vxj);
                    break;

                case VXK:
                    params.add(item.vxk);
                    update_statement.setDouble(index, item.vxk);
                    break;

                case VZI:
                    params.add(item.vzi);
                    update_statement.setDouble(index, item.vzi);
                    break;

                case VZJ:
                    params.add(item.vzj);
                    update_statement.setDouble(index, item.vzj);
                    break;

                case VZK:
                    params.add(item.vzk);
                    update_statement.setDouble(index, item.vzk);
                    break;

                default:
                    params.add(null);
                    break;
            }
        }
        return params;
    }

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
//        VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
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
