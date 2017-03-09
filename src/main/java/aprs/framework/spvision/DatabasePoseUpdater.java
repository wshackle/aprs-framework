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
import aprs.framework.pddl.executor.PartsTray;
import crcl.ui.XFuture;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
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
    private PreparedStatement pre_vision_clean_statement;
    private PreparedStatement get_tray_slots_statement;
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
    public static List<PartsTray> partsTrayList;
    public static double myRotationOffset=0;
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
    private String preVisionCleanStatementString;
    private String updatePartsTrayStatementString;
    private String updateKitTrayStatementString;
    private String getTraySlotsQueryString;

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
                break;

            case NEO4J:
                useBatch = false;
                break;
        }
        updateStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE).getQuery();
        getTraySlotsQueryString = queriesMap.get(DbQueryEnum.GET_TRAY_SLOTS).getQuery();
        get_tray_slots_statement = con.prepareStatement(getTraySlotsQueryString);
        queryAllString = queriesMap.get(DbQueryEnum.GET_ALL_POSE).getQuery();
        update_statement = con.prepareStatement(updateStatementString);
        updateKitTrayStatementString = queriesMap.get(DbQueryEnum.SET_SINGLE_KT_POSE).getQuery();
        preVisionCleanStatementString = queriesMap.get(DbQueryEnum.PRE_VISION_CLEAN_DB).getQuery();
        if (null != preVisionCleanStatementString) {
            pre_vision_clean_statement = con.prepareStatement(preVisionCleanStatementString);
        }
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

        getTraySlotsParamTypes = queriesMap.get(DbQueryEnum.GET_TRAY_SLOTS).getParams();

        query_all_statement = con.prepareStatement(queryAllString);
        querySingleString = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getQuery();
        queryDeleteSinglePoseString = queriesMap.get(DbQueryEnum.DELETE_SINGLE_POSE).getQuery();
        get_single_statement = con.prepareStatement(querySingleString);
        getSingleParamTypes = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE).getParams();
        if (null == getSingleParamTypes) {
            throw new IllegalStateException("Queries Map does not have param types for GET_SINGLE_POSE.");
        }
        if (null == updateParamTypes) {
            throw new IllegalStateException("Queries Map does not have param types for SET_SINGLE_POSE.");
        }
        if (null == getTraySlotsParamTypes) {
            throw new IllegalStateException("Queries Map does not have param types for GET_TRAY_SLOTS.");
        }
    }

    private XFuture<Void> setupConnection(String host, int port, String db, String username, String password, boolean debug) throws SQLException {
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
                    } catch (Throwable ex) {
                        Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                })
                .handle((Void x, Throwable ex) -> {
                    if (null != ex) {
                        Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
                });
    }

    private DbParamTypeEnum updateParamTypes[] = null;// NEO4J_MERGE_STATEMENT_PARAM_TYPES;
    private DbParamTypeEnum getTraySlotsParamTypes[] = null;// NEO4J_MERGE_STATEMENT_PARAM_TYPES;
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

    private List<PoseQueryElem> getDirectPoseList() {
        switch (dbtype) {
            case MYSQL:
            case NEO4J:
                return getJdbcDirectPoseList();
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

    private boolean forceUpdates = true;

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

    private final ConcurrentHashMap<String, List<DetectedItem>> offsetsMap = new ConcurrentHashMap<>();

    public List<DetectedItem> getSlotOffsets(DetectedItem tray) {
        String tray_name = tray.name;
        if (tray_name.startsWith("sku_")) {
            tray_name = tray_name.substring(4);
        }
        return offsetsMap.computeIfAbsent(tray_name, (String name) -> getSlotOffsetsNew(tray));
    }

    public List<DetectedItem> getSlotOffsetsNew(DetectedItem tray) {
        List<DetectedItem> ret = new ArrayList<>();
        try {
            List<Object> paramsList = poseParamsToStatement(tray, getTraySlotsParamTypes, get_tray_slots_statement);
            String getTraySlotsQueryStringFilled = fillQueryString(getTraySlotsQueryString, paramsList);
            boolean exec_result = get_tray_slots_statement.execute();
            if (exec_result) {
                try (ResultSet rs = get_tray_slots_statement.getResultSet()) {
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("get_tray_slots_statement.getResultSet() = " + rs + "\r\n");
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
                            resultMap.put(name, value);
                        }
                        if (null != displayInterface
                                && displayInterface.isDebug()
                                && resultMap.keySet().size() > 0) {
                            displayInterface.addLogMessage("resultMap=" + resultMap.toString() + System.lineSeparator());
                        }
                        String name = resultMap.get("name");
                        String sku_name = resultMap.get("sku_name");
                        double x = fix(rs, "x") * 1000.0;
                        double y = fix(rs, "y") * 1000.0;
                        String short_sku_name = sku_name;
                        if (short_sku_name.startsWith("sku_")) {
                            short_sku_name = short_sku_name.substring(4);
                        }

                        if (short_sku_name.startsWith("part_")) {
                            short_sku_name = short_sku_name.substring(5);
                        }
                        DetectedItem item = new DetectedItem(short_sku_name, 0, x, y);
                        item.fullName = name;
                        item.slotForSkuName = sku_name;
                        ret.add(item);
                    }
                }
            }
            if (ret.size() < 1) {
                System.err.println("");
                System.err.println("Can't get items for tray " + tray);
                System.err.println("getTraySlotsQueryStringFilled=");
                System.err.println(getTraySlotsQueryStringFilled);
                System.err.println("Returned 0 items.");
                System.err.println("");
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    public double normAngle(double angleIn) {
        double angleOut = angleIn;
        if (angleOut > Math.PI) {
            angleOut -= 2 * Math.PI * ((int) (angleIn / Math.PI));
        } else if (angleOut < -Math.PI) {
            angleOut += 2 * Math.PI * ((int) (-1.0 * angleIn / Math.PI));
        }
        return angleOut;
    }

    private double rotationOffset = 0.0;

    /**
     * Get the value of rotationOffset
     *
     * @return the value of rotationOffset
     */
    public double getRotationOffset() {
        return rotationOffset;
    }

    /**
     * Set the value of rotationOffset
     *
     * @param rotationOffset new value of rotationOffset
     */
    public void setRotationOffset(double rotationOffset) {
        this.rotationOffset = rotationOffset;
    }

    public List<DetectedItem> getSlots(DetectedItem tray) {
        List<DetectedItem> offsets = getSlotOffsets(tray);
        List<DetectedItem> ret = new ArrayList<>();
        String tray_name = tray.name;
        if (tray_name.startsWith("sku_")) {
            tray_name = tray_name.substring(4);
        }
        tray.totalSlotsCount = offsets.size();
        for (DetectedItem offsetItem : offsets) {

            String sku_name = offsetItem.name;
            String name = offsetItem.fullName;
            double x = offsetItem.x;
            double y = offsetItem.y;
            double angle = normAngle(tray.rotation + rotationOffset);
            double offsetMag = offsetItem.mag();
            if (tray.maxSlotDist < offsetMag) {
                tray.maxSlotDist = offsetMag;
            }
            DetectedItem item = new DetectedItem(name, 0,
                    tray.x + x * Math.cos(angle) - y * Math.sin(angle),
                    tray.y + x * Math.sin(angle) + y * Math.cos(angle)
            );
            item.type = "SLOT";
            item.tray = tray;
            item.slotForSkuName = offsetItem.slotForSkuName;
            item.visioncycle = tray.visioncycle;
            ret.add(item);
            item = new DetectedItem("empty_slot_for_" + sku_name + "_in_" + tray_name, 0,
                    tray.x + x * Math.cos(angle) - y * Math.sin(angle),
                    tray.y + x * Math.sin(angle) + y * Math.cos(angle));
            item.type = "EMPTY_SLOT";
            item.tray = tray;
            item.visioncycle = tray.visioncycle;
            item.slotForSkuName = offsetItem.slotForSkuName;
            ret.add(item);
        }
        return ret;
    }

    public double closestDist(DetectedItem subject, List<DetectedItem> l) {
        return l.stream()
                .mapToDouble(item -> subject.distFromXY(item))
                .min()
                .orElse(Double.POSITIVE_INFINITY);
    }

    private volatile ArrayList<DetectedItem> prevParts = new ArrayList<>();

    public List<DetectedItem> findEmptySlots(List<DetectedItem> slots, List<DetectedItem> parts) {
        final long timestamp = System.currentTimeMillis();
        prevParts
                = prevParts.stream()
                .filter((DetectedItem prevPart) -> timestamp - prevPart.timestamp < 10000)
                .filter((DetectedItem prevPart) -> closestDist(prevPart, parts) < 25.0)
                .collect(Collectors.toCollection(() -> new ArrayList<DetectedItem>()));
        prevParts.addAll(parts);
        return slots.stream()
                .filter(slot -> closestDist(slot, prevParts) > 25.0)
                .collect(Collectors.toList());
    }

    private List<DetectedItem> findAllEmptyTraySlots(List<DetectedItem> trays, List<DetectedItem> parts) {
        List<DetectedItem> emptySlots = new ArrayList<>();
        int count = 1;
        for (DetectedItem tray : trays) {

            tray.fullName = tray.name;
            if (tray.fullName.startsWith("sku_")) {
                tray.fullName = tray.fullName.substring(4);
            }
            tray.fullName = tray.fullName + "_" + count;
            count++;
            List<DetectedItem> slots = getSlots(tray);
            emptySlots.addAll(findEmptySlots(slots, parts));
        }
        return emptySlots;
    }

    public List<DetectedItem> findBestEmptyTraySlots(List<DetectedItem> kitTrays, List<DetectedItem> parts) {
        List<DetectedItem> emptySlots = findAllEmptyTraySlots(kitTrays, parts);
        for (DetectedItem kitTrayItem : kitTrays) {
            kitTrayItem.emptySlotsList
                    = emptySlots.stream()
                    .filter((DetectedItem slotItem) -> "EMPTY_SLOT".equals(slotItem.type))
                    .filter((DetectedItem slotItem) -> slotItem.tray == kitTrayItem)
                    .collect(Collectors.toList());
            kitTrayItem.emptySlotsCount = kitTrayItem.emptySlotsList.size();
        }
        kitTrays.sort((DetectedItem tray1, DetectedItem tray2) -> Long.compare(tray1.emptySlotsCount, tray2.emptySlotsCount));
        int min_non_zero_tray = 0;
        for (int i = 0; i < kitTrays.size(); i++) {
            DetectedItem kitTray = kitTrays.get(i);
            kitTray.kitTrayNum = i;
            if (kitTray.emptySlotsCount < 1 && i < kitTrays.size() - 1) {
                min_non_zero_tray = i + 1;
            }
        }
        final int mnzt = min_non_zero_tray;
        return emptySlots
                .stream()
                .filter((DetectedItem slotItem) -> "EMPTY_SLOT".equals(slotItem.type))
                .filter((DetectedItem slotItem) -> slotItem.tray != null && slotItem.tray.kitTrayNum == mnzt)
                .collect(Collectors.toList());
    }

    private boolean doPrefEmptySlotsFiltering = true;

    public List<DetectedItem> addEmptyTraySlots(List<DetectedItem> itemList) {
//        List<DetectedItem> partsTrays
//                = list.stream()
//                        .filter((DetectedItem item) -> "PT".equals(item.type))
//                        .collect(Collectors.toList());
        List<DetectedItem> kitTrays
                = itemList.stream()
                .filter((DetectedItem item) -> "KT".equals(item.type))
                .collect(Collectors.toList());
        List<DetectedItem> partTrays
                = itemList.stream()
                .filter((DetectedItem item) -> "PT".equals(item.type))
                .collect(Collectors.toList());
        List<DetectedItem> parts
                = itemList.stream()
                .filter((DetectedItem item) -> "P".equals(item.type))
                .collect(Collectors.toList());
        List<DetectedItem> fullList = new ArrayList<>();
        List<DetectedItem> bestEmptySlots = findBestEmptyTraySlots(kitTrays, parts);
        fullList.addAll(kitTrays);
        fullList.addAll(partTrays);
        fullList.addAll(parts);
        fullList.addAll(bestEmptySlots);
        return fullList;
    }

    private static double distDiff(DetectedItem val, List<DetectedItem> targets) {
        if (null == targets || targets.size() < 1) {
            return val.mag();
        }
        if (targets.size() == 1) {
            return val.distFromXY(targets.get(0));
        } else {
            return val.distFromXY(targets.get(0)) - val.distFromXY(targets.get(1));
        }
    }

    public int bestIndex(DetectedItem item, List<DetectedItem> slotList) {
        int bestIndexRet = Integer.MAX_VALUE;
        double minDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < slotList.size(); i++) {
            DetectedItem slot = slotList.get(i);
            if (!slot.slotForSkuName.equals(item.origName)) {
                continue;
            }
            double dist = item.distFromXY(slot);
            if (dist < minDist) {
                minDist = dist;
                bestIndexRet = i;
            }
        }
        return bestIndexRet;
    }

    private int updateCount = 0;

    public List<DetectedItem> updateVisionList(List<DetectedItem> inList,
            boolean addRepeatCountsToName,
            boolean keepFullNames) {
        myRotationOffset=getRotationOffset();
       
        partsTrayList =  new ArrayList();
        List<DetectedItem> itemsToVerify = new ArrayList<>();
        List<DetectedItem> returnedList = new ArrayList<>();
        try {
            if (updateCount < 1) {
                pre_vision_clean_statement.execute();
            }
            updateCount++;
            List<DetectedItem> partsTrays
                    = inList.stream()
                    .filter((DetectedItem item) -> "PT".equals(item.type))
                    .collect(Collectors.toList());
            List<DetectedItem> kitTrays
                    = inList.stream()
                    .filter((DetectedItem item) -> "KT".equals(item.type))
                    .collect(Collectors.toList());

            List<DetectedItem> list = inList;
            for (int i = 0; i < list.size(); i++) {
                DetectedItem ci = list.get(i);
                if (null == ci || ci.name.compareTo("*") == 0) {
                    continue;
                }
                if (ci.name.startsWith("sku_")) {
                    ci.name = ci.name.substring(4);
                }
                if ("P".equals(ci.type)) {
                    if (ci.insideKitTray || inside(kitTrays, ci, 10)) {
                        ci.name = ci.name + "_in_kt";
                        if (!keepFullNames) {
                            ci.fullName = ci.name;
                        }
                        ci.insideKitTray = true;
                    } else if (ci.insidePartsTray || inside(partsTrays, ci, 68)) {
                        ci.name = ci.name + "_in_pt";
                        if (!keepFullNames) {
                            ci.fullName = ci.name;
                        }
                        ci.insidePartsTray = true;
                    }
                }
                if (!keepFullNames) {
                    if (ci.name != null && ci.name.length() > 0 && (ci.fullName == null || ci.fullName.length() < 1)) {
                        ci.fullName = ci.name;
                    }
                }
            }
            if (doPrefEmptySlotsFiltering) {

                List<DetectedItem> parts
                        = inList.stream()
                        .filter((DetectedItem item) -> "P".equals(item.type))
                        .collect(Collectors.toList());
                List<DetectedItem> emptySlots
                        = list.stream()
                        .filter((DetectedItem item) -> "EMPTY_SLOT".equals(item.type))
                        .collect(Collectors.toList());
                Comparator<DetectedItem> kitComparator
                        = comparingLong((DetectedItem kt) -> (kt.emptySlotsCount < 1) ? Long.MAX_VALUE : kt.emptySlotsCount);
                kitTrays.sort(kitComparator);
                List<DetectedItem> firstSortParts = new ArrayList<>();
                firstSortParts.addAll(parts);
                firstSortParts.sort(comparingDouble((DetectedItem p) -> distDiff(p, kitTrays)));
                Map<DetectedItem, List<DetectedItem>> partsByTray
                        = new TreeMap<>(kitComparator);
                List<DetectedItem> matchedParts = new ArrayList<>();
                for (int i = 0; i < kitTrays.size(); i++) {
                    DetectedItem kit = kitTrays.get(i);
                    // System.out.println("kit fullname::: "+kit.fullName);

                    //System.out.println("kit x::: "+kit.getX());
                    //System.out.println("kit y::: "+kit.getY());
                    //System.out.println("kit rotation::: "+kit.rotation);
                    PartsTray partstray = new PartsTray(kit.fullName);
                    partstray.setX(kit.getX());
                    partstray.setY(kit.getY());
                    partstray.setRotation(kit.rotation);

                    partsTrayList.add(partstray);
                    if (null == kit.emptySlotsList) {
                        continue;
                    }
                    List<DetectedItem> slotFillers = new ArrayList<>();
                    for (int j = 0; j < kit.emptySlotsList.size(); j++) {
                        DetectedItem slot = kit.emptySlotsList.get(j);

                        DetectedItem bestSlotFiller
                                = firstSortParts.stream()
                                .filter((DetectedItem p) -> p.insidePartsTray)
                                .filter((DetectedItem p) -> Objects.equals(p.origName, slot.slotForSkuName))
                                .findFirst()
                                .orElse(null);
                        if (null != bestSlotFiller) {
                            slotFillers.add(bestSlotFiller);
                            firstSortParts.remove(bestSlotFiller);
                        }
                    }
                    slotFillers.sort(comparingInt((DetectedItem slotFiller) -> bestIndex(slotFiller, kit.emptySlotsList)));
                    matchedParts.addAll(slotFillers);
                }
                list = new ArrayList<>();
                list.addAll(matchedParts);
                list.addAll(firstSortParts);
                list.addAll(partsTrays);
                list.addAll(kitTrays);
                list.addAll(emptySlots);
            }
            long t0_nanos = System.nanoTime();
            long t0_millis = System.currentTimeMillis();
            int updates = 0;
            synchronized (this) {
//                if (delOnUpdate) {
//                    for (DetectedItem item : list) {
//                        deletePose(item.fullName);
//                        deletePose(item.name);
//                    }
//                }
                if (null == update_statement) {
                    throw new IllegalStateException("update_statement == null");
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
                    if (null == stmnt) {
                        throw new IllegalStateException("stmt == null");
                    }
                    if (addRepeatCountsThisItem) {
                        ci.repeats = repeatsMap.compute(ci.name, (String name, Integer reps) -> (reps != null) ? (reps + 1) : 0);
                        if(!keepFullNames) {
                            ci.fullName = ci.name + "_" + (ci.repeats + 1);
                        }
                    }
                    returnedList.add(ci);
                    List<Object> paramsList = poseParamsToStatement(ci, updateParamTypes, stmnt);
                    String updateStringFilled = fillQueryString(statementString, paramsList);
                    ci.setQuery = updateStringFilled;
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
                            stmnt.addBatch();
                            batchUrs.add(ur);
                            updates++;
                        } else {
                            boolean exec_result = stmnt.execute();
                            if (null != displayInterface && displayInterface.isDebug()) {
                                displayInterface.addLogMessage(" update_statement.execute() returned  " + exec_result + "\r\n");
                            }
                            if (exec_result) {
                                try (ResultSet rs = stmnt.getResultSet()) {
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
                                updatedCount = stmnt.getUpdateCount();
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

    private static boolean inside(List<DetectedItem> trays, DetectedItem ci, double threshhold) {
        return trays.stream().anyMatch((DetectedItem tray) -> tray.dist(ci) < tray.maxSlotDist + threshhold);
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
        if (null == paramTypes) {
            throw new IllegalArgumentException("paramTypes is null");
        }
        if (null == stmnt) {
            throw new IllegalArgumentException("stmnt is null");
        }
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
                    stmnt.setDouble(index, item.vxk);
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
        if (!sku.startsWith("sku_")) {
            sku = "sku_" + name;
        }
        return sku;
    }

    private final ExecutorService pqExecServ = Executors.newSingleThreadExecutor();

    public XFuture<List<PoseQueryElem>> queryDatabase() throws InterruptedException, ExecutionException {
        return XFuture.supplyAsync(() -> Collections.unmodifiableList(getDirectPoseList()), pqExecServ);
    }

}
