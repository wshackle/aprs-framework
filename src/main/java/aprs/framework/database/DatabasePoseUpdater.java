
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.jdbc.Driver;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
class DatabasePoseUpdater implements AutoCloseable {

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
    
    public DatabasePoseUpdater(Connection con,DbType dbtype, boolean sharedConnection) throws SQLException {
        this.dbtype = dbtype;
        this.con = con;
        this.sharedConnection = sharedConnection;
        setupStatements();
    }
    
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
                update_statement = con.prepareStatement(
                        "update Point p, Vector vx, Vector vz "
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
                        + " where _NAME = ? ) )");

                query_all_statement = con.prepareStatement(
                        "select name,X,Y,VXX,VXY from DirectPose");
                break;

            case NEO4J:
                useBatch = false;
                update_statement = con.prepareStatement(NEO4J_MERGE_STATEMENT_STRING);

//                addnew_statement = con.prepareStatement(db);
                query_all_statement = con.prepareStatement("MATCH pointpath=(source) -[r0]-> (n) -[r2:hasPoseLocation_Pose] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
                        + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
                        + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
                        + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,xaxis.hasVector_I as vxx,xaxis.hasVector_J as vxy");
                xindexes = new int[]{2};
                yindexes = new int[]{3};
                rotCosIndexes = new int[]{4};
                rotSinIndexes = new int[]{5};
                nameIndexes = new int[]{1};
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
        }
    }

    private void setupConnection(String host, int port , String db, String username, String password) throws SQLException {
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
    
    public DatabasePoseUpdater(String host, int port, String db, String username, String password, DbType dbtype) throws SQLException {
        this.dbtype = dbtype;
        sharedConnection = false;
        setupConnection(host, port, db, username, password);
        setupStatements();
    }
    
    public static final String NEO4J_MERGE_STATEMENT_STRING = "merge (source:SolidObject {name: {1} })\n"
            + "merge (source) - [:hasPhysicalLocation_RefObject] -> (pl:PhysicalLocation)\n"
            + "merge (pl) - [:hasPoseLocation_Pose] -> (pose:PoseLocation)\n"
            + "merge (pose) - [:hasPose_Point] -> (pt:Point)\n"
            + "merge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\n"
            + "merge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\n"
            + "on create set zaxis.hasVector_I=0.0,zaxis.hasVector_J=0.0,zaxis.hasVector_K=1.0\n"
            + "set pt.hasPoint_X= {2},pt.hasPoint_Y= {3}\n"
            + "set xaxis.hasVector_I={4}, xaxis.hasVector_J={5}, xaxis.hasVector_K=0.0";

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

    private static double fix(String s) {
        String peices[] = s.trim().split("[^0-9E+-.]+");
        String fixed = (peices[0].length() > 0 || peices.length < 2) ? peices[0] : peices[1];
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
        try (ResultSet rs = query_all_statement.executeQuery()) {
            if (null != rs) {
                while (rs.next()) {
                    if (debug) {
                        for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
                            String columnName = rs.getMetaData().getColumnName(j);
                            String val = rs.getString(columnName);
                            String str = "(" + j + "/" + rs.getMetaData().getColumnCount() + ") columnName = " + columnName + ", val = " + val;
                            if (null != displayInterface) {
                                displayInterface.addLogMessage(str);
                            }
                        }
                    }
                    l.add(new PoseQueryElem(rs.getString("name"),
                            fix(rs.getString("x")),
                            fix(rs.getString("y")),
                            fix(rs.getString("vxx")),
                            fix(rs.getString("vxy"))
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
            if(null != update_statement) {
                update_statement.close();
                update_statement = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if(null != query_all_statement) {
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
            if(null != Main.getDisplayInterface()) {
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

    public void updateVisionList(List<DetectedItem> list) {
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
                if (updates > 0 && useBatch) {
                    update_statement.addBatch();
                }
                List<Object> paramsList = poseParamsToStatement(ci.name + "_" + (ci.repeats + 1), ci.x, ci.y, ci.rotation);
                if (null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("update_statement = " + update_statement.toString() + "\n");

                    if (debug && dbtype == DbType.NEO4J) {
                        String updateStringFilled
                                = NEO4J_MERGE_STATEMENT_STRING;
                        for (int paramIndex = 1; paramIndex < paramsList.size(); paramIndex++) {
                            updateStringFilled
                                    = updateStringFilled.replace("{" + paramIndex + "}", Objects.toString(paramsList.get(paramIndex)));
                        }
                        displayInterface.addLogMessage("updateStringFilled = " + updateStringFilled + "\n");
                    }
//                    displayInterface.addLogMessage("update_statement.query = "+update_statement.+"\n");

                }
                if (useBatch) {
                    updates++;
                } else {
                    update_statement.execute();
//                    poses_updated += update_statement.getUpdateCount();
                    poses_updated++;
                }
            }
            if (null != displayInterface && displayInterface.isDebug()) {
                displayInterface.addLogMessage("end updateVisionList");
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

    int xindexes[] = {1};
    int yindexes[] = {2};
    int rotCosIndexes[] = {3};
    int rotSinIndexes[] = {4};
    int nameIndexes[] = {5, 6, 7};

    private List<Object> poseParamsToStatement(String name, double x, double y, double rotation) throws SQLException {
        ArrayList<Object> params = new ArrayList<>();
        for (int i : xindexes) {
            update_statement.setDouble(i, x);
            while(params.size()< i+1) {
                params.add(null);
            }
            params.set(i, x);
        }
        for (int i : yindexes) {
            update_statement.setDouble(i, y);
            while(params.size()< i+1) {
                params.add(null);
            }
            params.set(i, y);
        }
        for (int i : rotCosIndexes) {
            double crot = Math.cos(rotation);
            update_statement.setDouble(i, crot);
            while(params.size()< i+1) {
                params.add(null);
            }
            params.set(i, crot);
        }
        for (int i : rotSinIndexes) {
            double srot = Math.sin(rotation);
            update_statement.setDouble(i, srot);
            while(params.size()< i+1) {
                params.add(null);
            }
            params.set(i, srot);
        }
        for (int i : nameIndexes) {
            update_statement.setString(i, name);
            while(params.size()< i+1) {
                params.add(null);
            }
            params.set(i, name);
        }
        return params;
    }

    public boolean updatePose(String name, double x, double y, double rotation) throws SQLException {
        this.poseParamsToStatement(name, x, y, rotation);
        VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
        if (null != displayInterface && displayInterface.isDebug()) {
            displayInterface.addLogMessage(update_statement.toString());
        }
        boolean ex_result = update_statement.execute();
        if (null != displayInterface && displayInterface.isDebug()) {
            displayInterface.addLogMessage("execute() returned   " + ex_result
                    + ", update count = " + update_statement.getUpdateCount());
        }
        return ex_result;
    }

}
