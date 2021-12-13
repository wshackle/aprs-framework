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
package aprs.database.vision;

import aprs.database.*;
import aprs.misc.SlotOffsetProvider;
import aprs.misc.Utils;
import aprs.system.AprsSystem;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static aprs.database.DbCsvBackup.executeQuery;
import static aprs.database.DbSetupBuilder.DEFAULT_LOGIN_TIMEOUT;
import static aprs.misc.AprsCommonLogger.println;
import static crcl.utils.CRCLUtils.requireNonNull;
import static java.util.Comparator.*;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("unused")
public class DatabasePoseUpdater implements AutoCloseable, SlotOffsetProvider {

    public boolean isConnected() {
        if (this.aprsSystem.isUseCsvFilesInsteadOfDatabase()) {
            return true;
        }
        try {
            return null != con && !con.isClosed();
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
            return false;
        }
    }
    private final AprsSystem aprsSystem;

    private @MonotonicNonNull
    Connection con;
    private @MonotonicNonNull
    PreparedStatement update_statement;
    private @MonotonicNonNull
    PreparedStatement pre_vision_clean_statement;
    private @MonotonicNonNull
    PreparedStatement get_tray_slots_statement;
    private @MonotonicNonNull
    PreparedStatement update_parts_tray_statement;
    private @MonotonicNonNull
    PreparedStatement update_kit_tray_statement;
    private @MonotonicNonNull
    PreparedStatement query_all_statement;
    private @MonotonicNonNull
    PreparedStatement query_all_new_statement;
    private @MonotonicNonNull
    PreparedStatement get_single_statement;

//    private org.neo4j.driver.v1.Driver neo4jJavaDriver;
//    private Session neo4jSession;
    private final DbType dbtype;
    private boolean useBatch;

    private boolean verify = false;

    private long totalUpdateTimeMillis;
    final private List<PartsTray> partsTrayList = new ArrayList<>();

    public List<PartsTray> getPartsTrayList() {
        return partsTrayList;
    }

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

    public @Nullable
    Connection getSqlConnection() {
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
    private final DbSetup dbsetup;

    @SuppressWarnings({"nullness","initialization"})
    public DatabasePoseUpdater(
            @Nullable Connection con,
            DbType dbtype,
            boolean sharedConnection,
            DbSetup dbsetup,
            String taskname,
            AprsSystem aprsSystem) throws SQLException {
        this.dbtype = dbtype;
        this.con = con;
        this.sharedConnection = sharedConnection;
        this.dbsetup = dbsetup;
        this.taskname = taskname;
        if (dbtype == DbType.NONE) {
            this.queriesMap = Collections.emptyMap();
        } else {
            Map<DbQueryEnum, DbQueryInfo> qm = dbsetup.getQueriesMap();
            if (null == qm) {
                throw new IllegalArgumentException("dbsetup has null queriesMap");
            }
            this.queriesMap = qm;
            setupStatements();
        }
        this.aprsSystem = aprsSystem;
    }

    private @MonotonicNonNull
    String queryAllString;
    private @MonotonicNonNull
    String queryAllNewString;
    private @MonotonicNonNull
    String querySingleString;
    private @MonotonicNonNull
    String queryDeleteSinglePoseString;
    private @MonotonicNonNull
    String updateStatementString;
    private @MonotonicNonNull
    String preVisionCleanStatementString;
    private @MonotonicNonNull
    String updatePartsTrayStatementString;
    private @MonotonicNonNull
    String updateKitTrayStatementString;
    private @MonotonicNonNull
    String getTraySlotsQueryString;

    private static DbParamTypeEnum[] getQueryParams(Map<DbQueryEnum, DbQueryInfo> queriesMap, DbQueryEnum key) {
        DbQueryInfo info = queriesMap.get(key);
        if (null != info) {
            return info.getParams();
        }
        throw new IllegalStateException("no query params for " + key);
    }

    private static String getQueryString(Map<DbQueryEnum, DbQueryInfo> queriesMap, DbQueryEnum key) {
        DbQueryInfo info = queriesMap.get(key);
        if (null != info) {
            return info.getQuery();
        }
        throw new IllegalStateException("no query string for " + key);
    }

    @SuppressWarnings("SameParameterValue")
    private static Map<DbParamTypeEnum, String> getQueryResultsMap(Map<DbQueryEnum, DbQueryInfo> queriesMap, DbQueryEnum key) {
        DbQueryInfo info = queriesMap.get(key);
        if (null != info) {
            return info.getResults();
        }
        throw new IllegalStateException("no query string for " + key);
    }

    private Connection getCheckedConnection() throws IllegalStateException {
        Connection ret = con;
        if (null == ret) {
            throw new IllegalStateException("connection is null");
        }
        return ret;
    }

    private void setupStatements() throws SQLException {
        closed = false;
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
        Connection connection = getCheckedConnection();

        getTraySlotsQueryString = getQueryString(queriesMap, DbQueryEnum.GET_TRAY_SLOTS);
        get_tray_slots_statement = connection.prepareStatement(getTraySlotsQueryString);

        updateStatementString = getQueryString(queriesMap, DbQueryEnum.SET_SINGLE_POSE);
        update_statement = connection.prepareStatement(updateStatementString);

        updateKitTrayStatementString = getQueryString(queriesMap, DbQueryEnum.SET_SINGLE_KT_POSE);
        preVisionCleanStatementString = getQueryString(queriesMap, DbQueryEnum.PRE_VISION_CLEAN_DB);
        if (null != preVisionCleanStatementString) {
            pre_vision_clean_statement = connection.prepareStatement(preVisionCleanStatementString);
        }
        if (updateKitTrayStatementString != null && updateKitTrayStatementString.length() > 0) {
            update_kit_tray_statement = connection.prepareStatement(updateKitTrayStatementString);
        } else {
            update_kit_tray_statement = update_statement;
            updateKitTrayStatementString = updateStatementString;
        }
        updatePartsTrayStatementString = getQueryString(queriesMap, DbQueryEnum.SET_SINGLE_PT_POSE);
        if (updateKitTrayStatementString != null && updateKitTrayStatementString.length() > 0) {
            update_parts_tray_statement = connection.prepareStatement(updatePartsTrayStatementString);
        } else {
            update_parts_tray_statement = update_statement;
            updatePartsTrayStatementString = updateStatementString;
        }
        updateParamTypes = getQueryParams(queriesMap, DbQueryEnum.SET_SINGLE_POSE);
        getTraySlotsParamTypes = getQueryParams(queriesMap, DbQueryEnum.GET_TRAY_SLOTS);

        queryAllString = getQueryString(queriesMap, DbQueryEnum.GET_ALL_POSE);
        query_all_statement = connection.prepareStatement(queryAllString);

        queryAllNewString = getQueryString(queriesMap, DbQueryEnum.GET_ALL_NEW_POSE);
        query_all_new_statement = connection.prepareStatement(queryAllNewString);

        querySingleString = getQueryString(queriesMap, DbQueryEnum.GET_SINGLE_POSE);
        get_single_statement = connection.prepareStatement(querySingleString);

        queryDeleteSinglePoseString = getQueryString(queriesMap, DbQueryEnum.DELETE_SINGLE_POSE);
        getSingleParamTypes = getQueryParams(queriesMap, DbQueryEnum.GET_SINGLE_POSE);
    }

    private XFutureVoid setupConnection(String host, int port, String db, String username, String password, boolean debug) {
        closed = false;
        switch (dbtype) {
            case MYSQL:
                useBatch = true;
                break;

            case NEO4J:
                useBatch = false;
                break;

            case NONE:
            default:
                break;
        }
        return DbSetupBuilder.setupConnection(dbtype, host, port, db, username, password, debug, DEFAULT_LOGIN_TIMEOUT)
                .thenAccept(c -> con = c)
                .thenRun(() -> println("DatabasePoseUpdater connected to database of type " + dbtype + " on host " + host + " with port " + port));
    }

    final private Map<DbQueryEnum, DbQueryInfo> queriesMap;

    public static XFuture<@Nullable DatabasePoseUpdater> createDatabasePoseUpdater(String host,
            int port,
            String db,
            String username,
            String password,
            DbType dbtype,
            DbSetup dbsetup,
            boolean debug,
            String taskname,
            AprsSystem aprsSystem) {

        DatabasePoseUpdater dpu;
        try {
            dpu = new DatabasePoseUpdater(host, port, db, username, password, dbtype, dbsetup, debug, taskname, aprsSystem);
            if (dbtype == DbType.NONE) {
                return XFuture.completedFuture(dpu);
            }
            return dpu.
                    setupConnection(host, port, db, username, password, debug)
                    .thenRun(() -> {
                        try {
                            dpu.setupStatements();
                        } catch (Throwable ex) {
                            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
                            throw new RuntimeException(ex);
                        }
                    })
                    .handle(
                            "DatabasePoseUpdater.handleDbConnect",
                            (Void x, Throwable ex) -> {
                                if (null != ex) {
                                    Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
                                    return null;
                                }
                                return dpu;
                            });
        } catch (Exception ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
            XFuture<@Nullable DatabasePoseUpdater> xf = new XFuture<>("createDatabasePoseUpdaterExeption");
            xf.completeExceptionally(ex);
            return xf;
        }
    }

    private DatabasePoseUpdater(
            String host,
            int port,
            String db,
            String username,
            String password,
            DbType dbtype,
            DbSetup dbsetup,
            boolean debug,
            String taskname,
            AprsSystem aprsSystem) {
        this.dbtype = dbtype;
        sharedConnection = false;
        this.dbsetup = dbsetup;
        this.queriesMap = dbsetup.getQueriesMap();
        this.taskname = taskname;
        this.aprsSystem = aprsSystem;
    }

    private DbParamTypeEnum updateParamTypes[] = new DbParamTypeEnum[0];// NEO4J_MERGE_STATEMENT_PARAM_TYPES;
    private DbParamTypeEnum getTraySlotsParamTypes[] = new DbParamTypeEnum[0];// NEO4J_MERGE_STATEMENT_PARAM_TYPES;
    private DbParamTypeEnum getSingleParamTypes[] = new DbParamTypeEnum[0];//NEO4J_MERGE_STATEMENT_PARAM_TYPES;

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

    private static double fixDouble(ResultSet rs, String colLabel) throws SQLException {
        String s = rs.getString(colLabel);
        if (null == s) {
            return 0.0;
        }
        String peices[] = s.trim().split("[^0-9E+-.]+");
        String fixed = (peices[0].length() > 0 || peices.length < 2) ? s : peices[1];
        return Double.parseDouble(fixed);
    }

    private static int fixInt(ResultSet rs, String colLabel) throws SQLException {
        String s = rs.getString(colLabel);
        if (null == s) {
//            VisionToDBJFrameInterface displayInterface = DbMain.getDisplayInterface();
//            if (null != displayInterface) {
//                displayInterface.addLogMessage("Null return to from rs.getString(\""+colLabel+"\")");
//            } else {
//                System.err.println("Null return to from rs.getString(\""+colLabel+"\")");
//            }
            return -1;
        }
        String peices[] = s.trim().split("[^0-9E+-.]+");
        String fixed = (peices[0].length() > 0 || peices.length < 2) ? s : peices[1];
        return Integer.parseInt(fixed);
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

    private List<PoseQueryElem> getNewDirectPoseList() {
        switch (dbtype) {
            case MYSQL:
            case NEO4J:
                return getNewJdbcDirectPoseList();
            default:
                throw new IllegalStateException("getDirectPoseList not implemented for dbtype=" + dbtype);
        }
    }
    private @MonotonicNonNull
    VisionToDBJFrameInterface displayInterface;

    /**
     * Get the value of displayInterface
     *
     * @return the value of displayInterface
     */
    public @Nullable
    VisionToDBJFrameInterface getDisplayInterface() {
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

    private static String getParamFromResultMap(Map<DbParamTypeEnum, String> resultMap, DbParamTypeEnum type) {
        String ret = resultMap.get(type);
        if (null == ret) {
            throw new IllegalArgumentException("null result for parameter type=" + type + " in map : " + resultMap);
        }
        return ret;
    }

    private final String taskname;

    private ResultSet getReultsSet(java.sql.PreparedStatement preparedStatement, String simQuery, String name) throws SQLException, IOException {
        File homeDir = Utils.file(System.getProperty("user.home"));
        File queriesDir = Utils.file(homeDir, "aprsQueries");
        File sysQueriesDir = Utils.file(queriesDir, taskname);
        File dir = Utils.file(sysQueriesDir, name.toString());
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File resultsFile = File.createTempFile("results", ".csv", dir);
        System.out.println("resultsFile = " + resultsFile);
        File queryFile = Utils.file(dir, "query" + resultsFile.getName().substring(7, resultsFile.getName().length() - 4) + ".txt");
        try (PrintWriter pw = new PrintWriter(queryFile)) {
            pw.println(simQuery);
        }
        ResultSet rs = preparedStatement.getResultSet();
        FileWriter out = new FileWriter(resultsFile);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(rs))) {
            printer.printRecords(rs);
        }
        rs = preparedStatement.executeQuery();
        return rs;
    }

    private List<PoseQueryElem> getJdbcDirectPoseList() {
        List<PoseQueryElem> l = new ArrayList<>();
        if (null != displayInterface) {
            debug = displayInterface.isDebug();
            if (debug && null != queryAllString) {
                displayInterface.addLogMessage("Sending query:" + System.lineSeparator());
                displayInterface.addLogMessage(queryAllString);
                displayInterface.addLogMessage("" + System.lineSeparator());
            }
        }
        PreparedStatement statement = this.query_all_statement;
        if (null == statement) {
            throw new IllegalStateException("query_all_statement is null");
        }
        List<String> colNames = null;
        int rowCount = 0;
        try (ResultSet rs = statement.executeQuery()) {
            if (null != rs) {
                ResultSetMetaData meta = rs.getMetaData();
                colNames = new ArrayList<>();
                for (int colIndex = 1; colIndex < meta.getColumnCount(); colIndex++) {
                    colNames.add(meta.getColumnName(colIndex));
                }
                while (rs.next()) {
                    rowCount++;
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
                            = getQueryResultsMap(queriesMap, DbQueryEnum.GET_ALL_POSE);
                    String name = rs.getString("name");
                    if (null == name) {
                        Logger.getLogger(DatabasePoseUpdater.class.getName()).warning("result set for getJdbcDirectPoseList for row " + rowCount + " has null name");
                        continue;
                    }
                    l.add(new PoseQueryElem(name,
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.X)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.Y)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.Z)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.VXI)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.VXJ)),
                            fixInt(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.VISIONCYCLE))
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

    private List<PoseQueryElem> getNewJdbcDirectPoseList() {
        if (null == queryAllNewString) {
            throw new IllegalStateException("queryAllNewString == null");
        }
        if (null == query_all_new_statement) {
            throw new IllegalStateException("query_all_new_statement == null");
        }
        List<PoseQueryElem> l = new ArrayList<>();
        if (null != displayInterface) {
            debug = displayInterface.isDebug();
            if (debug) {
                displayInterface.addLogMessage("Sending query:" + System.lineSeparator());
                displayInterface.addLogMessage(queryAllNewString);
                displayInterface.addLogMessage("" + System.lineSeparator());
            }
        }

        List<String> colNames = null;
        String logMsg = null;
        int rowCount = 0;
        try (ResultSet rs = query_all_new_statement.executeQuery()) {
            if (null != rs) {
                ResultSetMetaData meta = rs.getMetaData();
                colNames = new ArrayList<>();
                for (int colIndex = 1; colIndex < meta.getColumnCount(); colIndex++) {
                    colNames.add(meta.getColumnName(colIndex));
                }
                while (rs.next()) {
                    rowCount++;
                    if (debug) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
                            String columnName = rs.getMetaData().getColumnName(j);
                            String val = rs.getString(columnName);
                            String str = "{ (" + j + "/" + rs.getMetaData().getColumnCount() + ") columnName = " + columnName + ", val = " + val + " }, ";
                            sb.append(str);
                        }
                        logMsg = sb.toString();
                        if (null != displayInterface) {
                            displayInterface.addLogMessage(logMsg);
                        }
                    }
                    Map<DbParamTypeEnum, String> resultMap
                            = getResultParamMap(queriesMap, DbQueryEnum.GET_ALL_NEW_POSE);
                    String name = rs.getString("name");
                    if (null == name) {
                        Logger.getLogger(DatabasePoseUpdater.class.getName()).warning("result set for getNewJdbcDirectPoseList for row " + rowCount + " has null name");
                        continue;
                    }
                    l.add(new PoseQueryElem(name,
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.X)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.Y)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.Z)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.VXI)),
                            fixDouble(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.VXJ)),
                            fixInt(rs, getParamFromResultMap(resultMap, DbParamTypeEnum.VISIONCYCLE))
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            if (null != displayInterface) {
                displayInterface.addLogMessage(exception.getMessage() + System.lineSeparator());
                displayInterface.addLogMessage("rowCount = " + rowCount + System.lineSeparator());
                displayInterface.addLogMessage("colNames = " + colNames + System.lineSeparator());
                displayInterface.addLogMessage("queryAllNewString = " + System.lineSeparator() + queryAllNewString + System.lineSeparator());
            } else {
                System.err.println("colNames = " + colNames);
                System.err.println("rowCount=\n" + rowCount);
                System.err.println("queryAllNewString=\n" + queryAllNewString);
                System.err.println("logMsg=" + logMsg);
                System.err.println();
                println();
            }
        }
        return l;
    }

    public @Nullable
    String getURL() {
        try {
            if (null != con) {
                return con.getMetaData().getURL();
            }
        } catch (SQLException sQLException) {
            sQLException.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "DatabasePoseUpdater{" + getURL() + '}';
    }

    public String getDetailString() {
        return "DatabasePoseUpdater{" + "con=" + con + ", dbtype=" + dbtype + ", useBatch=" + useBatch + ", verify=" + verify + ", totalUpdateTimeMillis=" + totalUpdateTimeMillis + ", maxUpdateTimeMillis=" + maxUpdateTimeMillis + ", totalUpdateTimeNanos=" + totalUpdateTimeNanos + ", maxUpdateTimeNanos=" + maxUpdateTimeNanos + ", totalUpdates=" + totalUpdates + ", totalListUpdates=" + totalListUpdates + ", sharedConnection=" + sharedConnection + ", queryAllString=" + queryAllString + ", querySingleString=" + querySingleString + ", mergeStatementString=" + updateStatementString + ", updateParamTypes=" + Arrays.toString(updateParamTypes) + ", getSingleParamTypes=" + Arrays.toString(getSingleParamTypes) + ", debug=" + debug + '}';
    }

    private volatile boolean closed = false;

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void close() {

        if (!closed) {
            println("Closing " + this);
        }
        try {
            if (null != update_statement) {
                update_statement.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
        }
        try {
            if (null != query_all_statement) {
                query_all_statement.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
        }
        try {
            if (null != get_single_statement) {
                get_single_statement.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
        }
        try {
            if (null != con && !sharedConnection) {
                con.close();
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
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
        }
        closed = true;
    }

    @Override
    protected void finalize() {
        close();
        try {
            super.finalize();
        } catch (Throwable ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    public static final AtomicInteger poses_updated = new AtomicInteger();

    private final Map<String, UpdateResults> updateResultsMap = new ConcurrentHashMap<>();

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

    private final ConcurrentHashMap<String, List<Slot>> offsetsMap = new ConcurrentHashMap<>();

    /**
     * Get a list of slots with names and relative position offsets for a given
     * kit or parts tray name.
     *
     * @param name name of the type of kit or slot tray
     * @param ignoreEmpty if false no slots being found logs a verbose error
     * message and throws IllegalStateException (good for fail fast) or if true
     * simply returns an empty list (good or display or when multiple will be
     * checked.
     *
     * @return list of slots with relative position offsets.
     */
    @Override
    public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
        if (dbtype != DbType.NONE) {
            if (null == getTraySlotsParamTypes) {
                throw new IllegalArgumentException("getTraySlotsParamTypes is null");
            }
            if (null == get_tray_slots_statement) {
                throw new IllegalArgumentException("get_tray_slots_statement is null");
            }
        }
        return getSlotOffsets(new Tray(name), ignoreEmpty);
    }

    private List<Slot> getSlotOffsets(Tray tray, boolean ignoreEmpty) {
        if (dbtype != DbType.NONE) {
            if (null == getTraySlotsParamTypes) {
                throw new IllegalArgumentException("getTraySlotsParamTypes is null");
            }
            if (null == get_tray_slots_statement) {
                throw new IllegalArgumentException("get_tray_slots_statement is null");
            }
        }
        String tray_name = tray.getName();
        if (tray_name.startsWith("sku_")) {
            tray_name = tray_name.substring(4);
        }
        List<Slot> l = offsetsMap.computeIfAbsent(tray_name, (String name) -> getSlotOffsetsNew(tray, ignoreEmpty));
        if (l == null && getSlotOffsetsNewSqlException != null) {
            Exception ex = getSlotOffsetsNewSqlException;
            getSlotOffsetsNewSqlException = null;
            throw new IllegalStateException("failed to get offsets for " + tray_name, ex);
        }
        return l;
    }

    private static class CheckedStringMap<K> {

        private final Map<K, String> map;

        Map<K, String> getMap() {
            return map;
        }

        CheckedStringMap(Map<K, String> map) {
            this.map = map;
        }

        String get(K key) {
            String value = map.get(key);
            if (value == null) {
                throw new IllegalStateException("no entry for " + key + " in " + map.toString());
            }
            return value;
        }

        void put(K key, String value) {
            map.put(key, value);
        }
    }
    private volatile @Nullable
    Exception getSlotOffsetsNewSqlException = null;

    private final ConcurrentHashMap<String, Integer> failuresMap = new ConcurrentHashMap<>();
    private static final List<Slot> failedSlotOffsets = Collections.emptyList();

    @SuppressWarnings("SynchronizeOnNonFinalField")
    private List<Slot> getSlotOffsetsNew(PhysicalItem tray, boolean ignoreEmpty) {
        if (null == getTraySlotsParamTypes) {
            throw new IllegalStateException("getTraySlotsParamTypes is null");
        }

        if (dbtype != DbType.NONE) {
            if (null == getTraySlotsQueryString) {
                throw new IllegalStateException("getTraySlotsQueryString is null");
            }
            if (null == get_tray_slots_statement) {
                throw new IllegalStateException("get_tray_slots_statement is null");
            }
        }
        Integer failuresI = failuresMap.get(tray.getName());
        if (failuresI != null && failuresI.compareTo(2) > 0) {
            return failedSlotOffsets;
        }
        getSlotOffsetsNewSqlException = null;
        List<Slot> ret = new ArrayList<>();
        try {
            String trayFullName = tray.getFullName();
            if (null == trayFullName || !tray.isFullNameSet()) {
                trayFullName = tray.getName() + "_1";
                tray.setFullName(trayFullName);
                if (trayFullName.startsWith("sku_")) {
                    trayFullName = trayFullName.substring(4);
                    tray.setFullName(trayFullName);
                }
            }
            String getTraySlotsQueryStringFilled = null;
            if (getTraySlotsParamTypes != null && getTraySlotsParamTypes.length > 0) {
                if (null == get_tray_slots_statement) {
//                System.out.println("null == get_tray_slots_statement");
                    throw new NullPointerException("get_tray_slots_statement");
                }
                if (null == getTraySlotsQueryString) {
                    throw new NullPointerException("getTraySlotsQueryString");
                }
                List<Object> paramsList = poseParamsToStatement(tray, getTraySlotsParamTypes, get_tray_slots_statement);
                getTraySlotsQueryStringFilled = fillQueryString(getTraySlotsQueryString, paramsList);
            }
            try {
                PrintStream ps = dbQueryLogPrintStream;
                if (!enableDatabaseUpdates && ps != null) {
                    ps.println();
                    ps.println(getTraySlotsQueryStringFilled);
                    ps.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//                boolean exec_result = get_tray_slots_statement.execute();
//                if (exec_result) {
            try (ResultSet rs = executeQuery(get_tray_slots_statement, getTraySlotsQueryStringFilled, "getSlotOffsetsNew_" + trayFullName, taskname, this.aprsSystem.isUseCsvFilesInsteadOfDatabase(), this.aprsSystem.getSysQueriesDir())) {
                if (null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("get_tray_slots_statement.getResultSet() = " + rs + "\r\n");
                }
                List<Map<String, String>> resultSetMapList = new ArrayList<>();
                while (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    CheckedStringMap<String> resultMap = new CheckedStringMap<>(new LinkedHashMap<>());
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("meta.getColumnCount() = " + meta.getColumnCount() + "\r\n");
                    }
                    for (int j = 1; j <= meta.getColumnCount(); j++) {
                        String name = meta.getColumnName(j);
                        // After upgrading to neo4j-jdbc 3.0  started getting 
                        // java.lang.UnsupportedOperationException: Method getObject in class org.neo4j.jdbc.ResultSet is not yet implemented.
//                                String value = rs.getObject(name, Object.class).toString();
                        String value = rs.getString(name);
                        if (null != name && null != value) {
                            resultMap.put(name, value);
                        }
                    }
                    if (null != displayInterface
                            && displayInterface.isDebug()
                            && resultMap.getMap().keySet().size() > 0) {
                        displayInterface.addLogMessage("resultMap=" + resultMap.toString() + System.lineSeparator());
                    }
                    String name = resultMap.get("name");
                    String sku_name = resultMap.get("sku_name");
                    String prp_name = resultMap.get("prp_name");
                    String tray_name = resultMap.get("tray_name");

                    assert (tray_name != null) : "tray_name == null";
                    assert (tray_name.equals(tray.getFullName())) :
                            ("!tray_name.equals(tray.getName()) tray_name=" + tray_name + ", tray=" + tray);
                    if (prp_name.startsWith("part_ref_and_pose_")) {
                        prp_name = prp_name.substring("part_ref_and_pose_".length());
                    }
                    double x = fixDouble(rs, "x") * 1000.0;
                    double y = fixDouble(rs, "y") * 1000.0;

                    String short_sku_name = sku_name;
                    if (short_sku_name.startsWith("sku_")) {
                        short_sku_name = short_sku_name.substring(4);
                    }

                    if (short_sku_name.startsWith("part_")) {
                        short_sku_name = short_sku_name.substring(5);
                    }
                    Slot offsetItem = new Slot(short_sku_name, 0, x, y);
                    offsetItem.setX_OFFSET(x);
                    offsetItem.setY_OFFSET(y);
                    offsetItem.setPrpName(prp_name);
                    offsetItem.setFullName(name);
                    offsetItem.setSlotForSkuName(sku_name);
                    if (null != getTraySlotsQueryStringFilled) {
                        offsetItem.setNewSlotQuery(getTraySlotsQueryStringFilled);
                    }
                    offsetItem.setNewSlotOffsetResultMap(resultMap.getMap());
                    offsetItem.setTray(tray);
                    if (resultMap.getMap().containsKey("diameter")) {
                        double diameter = fixDouble(rs, "diameter") * 1000.0;
                        offsetItem.setDiameter(diameter);
                    }
                    ret.add(offsetItem);
                }
            }
//                }
            if (ret.isEmpty()) {
                if (ignoreEmpty) {
                    return failedSlotOffsets;
                }
                System.err.println();
                System.err.println("Can't get items for tray " + tray);
                System.err.println("getTraySlotsQueryStringFilled=");
                System.err.println(getTraySlotsQueryStringFilled);
                System.err.println("Returned 0 items.");
                System.err.println("url=" + getURL());
                System.err.println();
                int failures = failuresMap.compute(tray.getName(), (name, count) -> (count == null) ? 1 : (count + 1));
                if (failures < 2) {
                    throw new IllegalStateException("Can't get items for tray" + tray + " url=" + getURL() + " getTraySlotsQueryStringFilled=\n" + getTraySlotsQueryStringFilled);
                } else {
                    return failedSlotOffsets;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
            getSlotOffsetsNewSqlException = ex;
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(ret);
    }

    private double normAngle(double angleIn) {
        if (!Double.isFinite(angleIn) || angleIn > 10.0 || angleIn < -10.0) {
            throw new IllegalArgumentException("angleIn=" + angleIn + " (must be in radians)");
        }
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
        this.rotationOffset = normAngle(rotationOffset);
    }

    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        return absSlotFromTrayAndOffset(tray, offsetItem, this.rotationOffset);
    }

    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffsetParam) {
        String name = offsetItem.getFullName();
        if (name == null || name.length() < 1 || !offsetItem.isFullNameSet()) {
            throw new IllegalStateException("bad fullname for offsetItem=" + offsetItem);
        }
        double x = offsetItem.x;
        double y = offsetItem.y;
        double angle = normAngle(tray.getRotation() + rotationOffsetParam);

        Slot item = new Slot(name, angle,
                tray.x + x * Math.cos(angle) - y * Math.sin(angle),
                tray.y + x * Math.sin(angle) + y * Math.cos(angle)
        );
        item.setDiameter(offsetItem.getDiameter());
        item.setType("S");
        item.setTray(tray);
        String slotForSkuName = offsetItem.getSlotForSkuName();
        if (null != slotForSkuName) {
            item.setSlotForSkuName(slotForSkuName);
        } else {
            warning("slotForSkuName is null in {0}", offsetItem);
        }
        item.setVisioncycle(tray.getVisioncycle());
        item.setPrpName(offsetItem.getPrpName());
        item.setZ(tray.z);
        item.setVxi(tray.getVxi());
        item.setVxj(tray.getVxj());
        item.setVxk(tray.getVxk());
        item.setVzi(tray.getVzi());
        item.setVzj(tray.getVzj());
        item.setVzk(tray.getVzk());
        item.setRotation(tray.getRotation());
        return item;
    }

    public List<Slot> getSlots(Tray tray) {
        return getSlots(tray, this);
    }

    private static void warning(String string, Object... args) {
        Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.WARNING, string, args);
    }

    private static List<Slot> getSlots(Tray tray, SlotOffsetProvider sop) {
        List<Slot> offsets = sop.getSlotOffsets(tray.getName(), false);
        List<Slot> ret = new ArrayList<>();
        String tray_name = tray.getName();
        if (tray_name.startsWith("sku_")) {
            tray_name = tray_name.substring(4);
        }
        if (offsets == null) {
            return Collections.emptyList();
        }
        tray.setTotalSlotsCount(offsets.size());
        tray.setAbsSlotList(new ArrayList<>());
        for (Slot offsetItem : offsets) {
            PhysicalItem offsetItemTray = offsetItem.getTray();
            if (null == offsetItemTray) {
                throw new IllegalStateException("null == offsetItem.getTray() : offsetItem=" + offsetItem + ", tray=" + tray);
            }
            if (offsetItemTray != tray) {
                if (!Objects.equals(offsetItemTray.getName(), tray.getName())
                        && !Objects.equals("sku_" + offsetItemTray.getName(), tray.getName())) {
                    throw new IllegalStateException("Offset seems to belong to the wrong tray : offsetItem=" + offsetItem + ", offsetItemTray=" + offsetItemTray + ", tray=" + tray);
                }
            }

            double offsetMag = offsetItem.mag();
            if (tray.getMaxSlotDist() < offsetMag) {
                tray.setMaxSlotDist(offsetMag);
            }
            String sku_name = offsetItem.getName();
            Slot item = sop.absSlotFromTrayAndOffset(tray, offsetItem);
            if (null == item) {
                warning("absSlotFromTrayAndOffset returned  is null for ({0},{1})", new Object[]{tray, offsetItem});
                continue;
            }
            ret.add(item);
            tray.getAbsSlotList().add(item);
            String composedName = "empty_slot_for_" + sku_name + "_in_" + tray_name;
            if (tray.getType().equals("KT")) {
                try {
                    String slotIndexString = offsetItem.getSlotIndexString();
                    int slot_index = 0;
                    if (null == slotIndexString) {
                        String prpName = offsetItem.getPrpName();
                        if (null != prpName) {
                            int uindex = prpName.lastIndexOf('_');
                            slot_index = Integer.parseInt(prpName.substring(uindex + 1).trim());
                            slotIndexString = "" + slot_index;
                            offsetItem.setSlotIndexString(slotIndexString);
                        }
                    } else {
                        slot_index = Integer.parseInt(slotIndexString);
                    }
                    if (slot_index > 0) {
                        composedName = "empty_slot_" + slotIndexString + "_for_" + sku_name + "_in_" + tray_name;
                    }
                } catch (Exception exception) {
                    System.err.println("offsetItem=" + offsetItem);
                    exception.printStackTrace();
                }
            }
            item = new Slot(composedName, item.getRotation(),
                    item.x,
                    item.y);
            item.setType("ES");
            item.setTray(tray);
            item.setVisioncycle(tray.getVisioncycle());
            String offsetItemSlotForSkuName = offsetItem.getSlotForSkuName();
            if (null != offsetItemSlotForSkuName) {
                item.setSlotForSkuName(offsetItemSlotForSkuName);
            }
            item.setPrpName(offsetItem.getPrpName());
            ret.add(item);
        }
        return ret;
    }

    private static double closestDist(PhysicalItem subject, List<PhysicalItem> l) {
        return l.stream()
                .mapToDouble(subject::distFromXY)
                .min()
                .orElse(Double.POSITIVE_INFINITY);
    }

    private final ArrayList<PhysicalItem> prevParts = new ArrayList<>();

    private static List<Slot> findEmptySlots(List<Slot> slots, List<PhysicalItem> parts, @Nullable List<PhysicalItem> prevParts) {
        final long timestamp = System.currentTimeMillis();

        List<PhysicalItem> newPrevParts
                = (prevParts == null) ? new ArrayList<>()
                        : prevParts.stream()
                                .filter((PhysicalItem prevPart) -> timestamp - prevPart.getTimestamp() < 10000)
                                .filter((PhysicalItem prevPart) -> closestDist(prevPart, parts) < 25.0)
                                .collect(Collectors.toCollection(ArrayList::new));
        newPrevParts.addAll(parts);
        if (null != prevParts) {
            prevParts.clear();
            prevParts.addAll(newPrevParts);
        }
        return slots.stream()
                .filter(slot -> closestDist(slot, newPrevParts) > 25.0)
                .collect(Collectors.toList());
    }

    private static List<Slot> findAllEmptyTraySlots(List<Tray> trays, List<PhysicalItem> parts, SlotOffsetProvider sop, @Nullable List<PhysicalItem> prevParts) {
        List<Slot> emptySlots = new ArrayList<>();
        ConcurrentHashMap<String, Integer> nameCountMap = new ConcurrentHashMap<>();
        for (Tray tray : trays) {
            int count = nameCountMap.compute(tray.getName(), (name, value) -> {
                if (null == value) {
                    return 1;
                } else {
                    return value + 1;
                }
            });
            String trayFullName = tray.getName();
            if (trayFullName.startsWith("sku_")) {
                trayFullName = trayFullName.substring(4);
            }
            trayFullName = trayFullName + "_" + count;
            tray.setFullName(trayFullName);
            List<Slot> slots = getSlots(tray, sop);
            if (null != slots) {
                emptySlots.addAll(findEmptySlots(slots, parts, prevParts));
            }
        }
        return emptySlots;
    }

    private static boolean checkKitTrayNumber(Slot slotItem, int mnzt) {
        PhysicalItem slotItemTray = slotItem.getTray();
        if (null == slotItemTray) {
            return false;
        }
        return slotItemTray.getKitTrayNum() == mnzt;
    }

    private static List<Slot> findBestEmptyTraySlots(
            List<Tray> kitTrays,
            List<PhysicalItem> parts,
            SlotOffsetProvider sop,
            @Nullable List<PhysicalItem> prevParts) {
        List<Slot> emptySlots = findAllEmptyTraySlots(kitTrays, parts, sop, prevParts);
        for (PhysicalItem kitTrayItem : kitTrays) {
            kitTrayItem.setEmptySlotsList(emptySlots.stream()
                    .filter((PhysicalItem slotItem) -> "ES".equals(slotItem.getType()))
                    .filter((PhysicalItem slotItem) -> slotItem.getTray() == kitTrayItem)
                    .collect(Collectors.toList()));
            kitTrayItem.setEmptySlotsCount(kitTrayItem.getEmptySlotsList().size());
        }
        kitTrays.sort(Comparator.comparingLong((ToLongFunction<PhysicalItem>) PhysicalItem::getEmptySlotsCount));
        int min_non_zero_tray = 0;
        for (int i = 0; i < kitTrays.size(); i++) {
            PhysicalItem kitTray = kitTrays.get(i);
            kitTray.setKitTrayNum(i);
            if (kitTray.getEmptySlotsCount() < 1 && i < kitTrays.size() - 1) {
                min_non_zero_tray = i + 1;
            }
        }
        final int mnzt = min_non_zero_tray;
        List<Slot> firstTraySlots = emptySlots
                .stream()
                .filter((Slot slotItem) -> "ES".equals(slotItem.getType()))
                .filter((Slot slotItem) -> checkKitTrayNumber(slotItem, mnzt))
                .collect(Collectors.toList());
        List<Slot> otherTraySlots = emptySlots
                .stream()
                .filter((Slot slotItem) -> "ES".equals(slotItem.getType()))
                .filter((Slot slotItem) -> !checkKitTrayNumber(slotItem, mnzt))
                .collect(Collectors.toList());
        List<Slot> orderedEmptySlots = new ArrayList<>();
        orderedEmptySlots.addAll(firstTraySlots);
        orderedEmptySlots.addAll(otherTraySlots);
        return orderedEmptySlots;
    }

    @SuppressWarnings("CanBeFinal")
    private boolean doPrefEmptySlotsFiltering = true;

    public List<PhysicalItem> addEmptyTraySlots(List<PhysicalItem> itemList) {
        return addEmptyTraySlots(itemList, this, prevParts);
    }

    private static List<PhysicalItem> addEmptyTraySlots(
            List<PhysicalItem> inputItems,
            SlotOffsetProvider sop,
            @Nullable List<PhysicalItem> prevParts) {

        List<Tray> kitTrays
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "KT".equals(item.getType()))
                        .filter(Tray.class::isInstance)
                        .map(Tray.class::cast)
                        .collect(Collectors.toList());
        List<Tray> partTrays
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "PT".equals(item.getType()))
                        .filter(Tray.class::isInstance)
                        .map(Tray.class::cast)
                        .collect(Collectors.toList());
        List<PhysicalItem> parts
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "P".equals(item.getType()))
                        .collect(Collectors.toList());
        List<PhysicalItem> fullList = new ArrayList<>();
        List<Slot> bestKitTrayEmptySlots = findBestEmptyTraySlots(kitTrays, parts, sop, prevParts);
        List<Slot> bestPartTrayEmptySlots = findBestEmptyTraySlots(partTrays, parts, sop, prevParts);
        fullList.addAll(kitTrays);
        fullList.addAll(partTrays);
        fullList.addAll(parts);
        fullList.addAll(bestKitTrayEmptySlots);
        fullList.addAll(bestPartTrayEmptySlots);
        return fullList;
    }

    private static double distDiff(PhysicalItem val, List<? extends PhysicalItem> targets) {
        if (null == targets || targets.size() < 1) {
            return val.mag();
        }
        if (targets.size() == 1) {
            return val.distFromXY(targets.get(0));
        } else {
            return val.distFromXY(targets.get(0)) - val.distFromXY(targets.get(1));
        }
    }

    private static int bestIndex(PhysicalItem item, List<PhysicalItem> slotList) {
        int bestIndexRet = Integer.MAX_VALUE;
        double minDist = Double.POSITIVE_INFINITY;
        for (int i = 0; i < slotList.size(); i++) {
            PhysicalItem slot = slotList.get(i);
            if (!Objects.equals(slot.getSlotForSkuName(), item.origName)) {
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

    private volatile boolean enableDatabaseUpdates = false;

    /**
     * Get the value of enableDatabaseUpdates
     *
     * @return the value of enableDatabaseUpdates
     */
    public boolean isEnableDatabaseUpdates() {
        return enableDatabaseUpdates;
    }

    private volatile @Nullable
    PrintStream dbQueryLogPrintStream = null;

    private List<String> requiredParts = Collections.emptyList();

    /**
     * Get the value of requiredParts
     *
     * @return the value of requiredParts
     */
    public List<String> getRequiredParts() {
        return requiredParts;
    }

    /**
     * Set the value of requiredParts
     *
     * @param requiredParts new value of requiredParts
     */
    public void setRequiredParts(List<String> requiredParts) {
        this.requiredParts = requiredParts;
    }

    /**
     * Set the value of enableDatabaseUpdates
     *
     * @param enableDatabaseUpdates new value of enableDatabaseUpdates
     */
    public void setEnableDatabaseUpdates(boolean enableDatabaseUpdates) {
        this.enableDatabaseUpdates = enableDatabaseUpdates;
        if (enableDatabaseUpdates) {
            println("enableDatabaseUpdates = " + enableDatabaseUpdates);
        }
        updateResultsMap.clear();
        try {
            if (null != dbQueryLogPrintStream) {
                PrintStream ps = dbQueryLogPrintStream;
                dbQueryLogPrintStream = null;
                ps.close();
            }
            if (enableDatabaseUpdates) {
                File dbQueriesDir = Utils.file(Utils.getlogFileDir(), "dbQueries");
                dbQueriesDir.mkdirs();
                PrintStream ps = new PrintStream(new FileOutputStream(Utils.createTempFile("db_" + dbsetup.getPort(), "_log.txt", dbQueriesDir)));
                for (Entry<String, List<Slot>> offsetEntry : offsetsMap.entrySet()) {
                    ps.println();
                    ps.println(commentStartString + " offsetsMap.key =" + offsetEntry.getKey());
                    List<Slot> l = offsetEntry.getValue();
                    ps.println(commentStartString + " offsetsMap.value =" + l);
                    if (!l.isEmpty() && null != l.get(0) && null != l.get(0).getNewSlotQuery()) {
                        ps.println();
                        ps.println(l.get(0).getNewSlotQuery());
                        ps.println();
                    }
                    ps.println();
                    dbQueryLogPrintStream = ps;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private volatile @Nullable
    List<PhysicalItem> lastEnabledUpdateList = null;

    public @Nullable
    List<PhysicalItem> getLastEnabledUpdateList() {
        if (null == lastEnabledUpdateList) {
            return null;
        }
        return Collections.unmodifiableList(lastEnabledUpdateList);
    }

    private int last_max_vision_cycle = -1;

    private String commentStartString = "// ### ";

    /**
     * Get the value of commentStartString
     *
     * @return the value of commentStartString
     */
    public String getCommentStartString() {
        return commentStartString;
    }

    /**
     * Set the value of commentStartString
     *
     * @param commentStartString new value of commentStartString
     */
    public void setCommentStartString(String commentStartString) {
        this.commentStartString = commentStartString;
    }

    private double noTrayOffsetZ = -10.0;

    /**
     * Get the value of noTrayOffsetZ
     *
     * @return the value of noTrayOffsetZ
     */
    public double getNoTrayOffsetZ() {
        return noTrayOffsetZ;
    }

    /**
     * Set the value of noTrayOffsetZ
     *
     * @param noTrayOffsetZ new value of noTrayOffsetZ
     */
    public void setNoTrayOffsetZ(double noTrayOffsetZ) {
        this.noTrayOffsetZ = noTrayOffsetZ;
    }

    public List<PhysicalItem> updateVisionList(List<PhysicalItem> inList,
            boolean addRepeatCountsToName,
            boolean keepFullNames) {
        final boolean useCsv = this.aprsSystem.isUseCsvFilesInsteadOfDatabase();

        if (!useCsv) {
            if (null == update_statement) {
                throw new IllegalStateException("update_statement is null");
            }
            if (null == pre_vision_clean_statement) {
                throw new IllegalStateException("pre_vision_clean_statement is null");
            }
            if (null == updateStatementString) {
                throw new IllegalStateException("updateStatementString is null");
            }
            if (null == updatePartsTrayStatementString) {
                throw new IllegalStateException("updatePartsTrayStatementString is null");
            }
            if (null == updateKitTrayStatementString) {
                throw new IllegalStateException("updateKitTrayStatementString is null");
            }
        }
        partsTrayList.clear();
        List<PhysicalItem> itemsToVerify = new ArrayList<>();
        List<PhysicalItem> returnedList = new ArrayList<>();
        final boolean edu = this.enableDatabaseUpdates && !useCsv;
        try {
            PrintStream ps = dbQueryLogPrintStream;
            if (edu && null != ps) {
                ps.println();
                ps.println(commentStartString + " updateVisionList : start dateTimeString = " + Utils.getDateTimeString());
                ps.println(commentStartString + " updateVisionList : inList = " + inList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (updateCount < 1 && !useCsv) {
                if (null == pre_vision_clean_statement) {
                    throw new NullPointerException("pre_vision_clean_statement");
                }
                pre_vision_clean_statement.execute();
            }
            updateCount++;
            List<PhysicalItem> list
                    = preProcessItemList(inList, false, keepFullNames, doPrefEmptySlotsFiltering, addRepeatCountsToName, partsTrayList);
            for (PhysicalItem item : inList) {
                if ("P".equals(item.getType()) && !item.isInsideKitTray() && !item.isInsidePartsTray()) {
                    item.z += noTrayOffsetZ;
                }
            }
            if (list.isEmpty()) {
                System.err.println("updateVisionList : list is empty.");
                return returnedList;
            }
            int max_vision_cycle = Integer.MIN_VALUE;
            int min_vision_cycle = Integer.MAX_VALUE;
            for (PhysicalItem item : list) {
                if (!forceUpdates) {
                    if (item.getVisioncycle() <= last_max_vision_cycle) {
                        throw new IllegalStateException("item=" + item + " has vision cycle <= last_max_vision_cycle " + last_max_vision_cycle);
                    }
                }
                if (max_vision_cycle < item.getVisioncycle()) {
                    max_vision_cycle = item.getVisioncycle();
                }
                if (min_vision_cycle > item.getVisioncycle()) {
                    min_vision_cycle = item.getVisioncycle();
                }
            }
            assert (max_vision_cycle == min_vision_cycle) :
                    ("max_vision_cycle(" + max_vision_cycle + ") != min_vision_cycle(" + min_vision_cycle + ") in list= " + list);

            try {
                PrintStream ps = dbQueryLogPrintStream;
                if (edu && null != ps) {
                    ps.println(commentStartString + " updateVisionList : max_vision_cycle = " + max_vision_cycle);
                    ps.println(commentStartString + " updateVisionList : min_vision_cycle = " + min_vision_cycle);
                    ps.println(commentStartString + " updateVisionList : last_max_vision_cycle = " + last_max_vision_cycle);
                    ps.println(commentStartString + " updateVisionList : list = " + list);
                    ps.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            last_max_vision_cycle = max_vision_cycle;
            long t0_nanos = System.nanoTime();
            long t0_millis = System.currentTimeMillis();
            int updates = 0;
            synchronized (this) {
                if (!useCsv) {
                    assert (null != update_statement) :
                            ("update_statement == null");
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
                Map<String, Integer> repeatsMap = new HashMap<>();
                for (PhysicalItem ci : list) {
                    if (null == ci || ci.getName().compareTo("*") == 0) {
                        continue;
                    }
                    PreparedStatement stmnt = update_statement;
                    String statementString = updateStatementString;
                    if (null != ci.getType()) {
                        switch (ci.getType()) {
                            case "PT":
                                stmnt = update_parts_tray_statement;
                                statementString = updatePartsTrayStatementString;
                                break;

                            case "KT":
                                stmnt = update_kit_tray_statement;
                                statementString = updateKitTrayStatementString;
                                break;
                        }
                    }
                    final String updateStringFilled;
                    if (!useCsv) {
                        if (null == stmnt) {
                            throw new RuntimeException("stmt == null");
                        }
                        if (null == statementString) {
                            throw new RuntimeException("stmt == statementString");
                        }
                        List<Object> paramsList = poseParamsToStatement(ci, updateParamTypes, stmnt);
                        updateStringFilled = fillQueryString(statementString, paramsList);
                    } else {
                        updateStringFilled="useCsv";
                    }
                    ci.setSetQuery(updateStringFilled);
                    returnedList.add(ci);
                    UpdateResults ur = updateResultsMap.get(ci.getFullName());

                    String ciFullName = ci.getFullName();
                    if (null == ciFullName) {
                        ciFullName = ci.getName();
                    }
                    if (null != ur) {
                        if (!forceUpdates
                                && Math.abs(ur.getX() - ci.x) < 1e-6
                                && Math.abs(ur.getY() - ci.y) < 1e-6
                                && Math.abs(ur.getRotation() - ci.getRotation()) < 1e-6) {
                            skippedUpdates.add(ciFullName);

                            continue;
                        }
                    } else {
                        ur = new UpdateResults(ciFullName);
                    }
                    itemsToVerify.add(ci);
                    ur.setException(null);
                    ur.setVerified(false);

                    try {

                        if (edu) {
                            ur.setLastDetectedItem(ci);
                            if (null != displayInterface && displayInterface.isDebug()) {
                                displayInterface.addLogMessage("updateStringFilled = \r\n" + updateStringFilled + "\r\n");
                            }
                            updates = internalDatabaseUpdate(requireNonNull(stmnt, "stmt"), batchUrs, ur, updates, updatedCount);
                            ur.setUpdateStringFilled(updateStringFilled);
                            ur.incrementStatementExecutionCount();
                        } else {
                            updates = 0;
                        }
                    } catch (Exception exception) {
                        ur.setException(exception);
                    }

                    updateResultsMap.put(ciFullName, ur);
                    if (null != ur.getException()) {
                        throw new RuntimeException("ur=" + ur, ur.getException());
                    }
                }
                if (null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Skipped updates = " + skippedUpdates);
                }
                if (updates > 0 && useBatch && edu) {
                    if (null == update_statement) {
                        throw new NullPointerException("update_statement");
                    }
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
                    poses_updated.addAndGet(updates);
                }
            }
            if (!edu) {
                if (null != displayInterface) {
                    displayInterface.updatePerformanceLine();
                }
                return returnedList;
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
            try {
                PrintStream ps = dbQueryLogPrintStream;
                if (edu && null != ps) {
                    ps.println();
                    ps.println(commentStartString + " updateVisionList : end dateTimeString = " + Utils.getDateTimeString());
                    ps.println(commentStartString + " updateVisionList : millis_diff = " + millis_diff);
                    ps.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            totalListUpdates++;
            totalUpdates = poses_updated.get();
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
        List<PhysicalItem> lcopy = new ArrayList<>(returnedList);
        lastEnabledUpdateList = lcopy;
        return returnedList;
    }

    public static List<PhysicalItem> processItemList(List<PhysicalItem> inputItems, SlotOffsetProvider sop) {
        List<PhysicalItem> listWithEmptySlots
                = DatabasePoseUpdater.addEmptyTraySlots(inputItems, sop, null);
        return DatabasePoseUpdater.preProcessItemList(listWithEmptySlots);
    }

    private static List<PhysicalItem> preProcessItemList(List<PhysicalItem> inputItems) {
        return preProcessItemList(inputItems, true, false, true, true, null);
    }

    private static List<PhysicalItem> preProcessItemList(List<PhysicalItem> inputItems, boolean keepNames, boolean keepFullNames, boolean doPrefEmptySlotsFiltering, boolean addRepeatCountsToName, @Nullable List<PartsTray> partsTrayList) {
        List<Tray> partsTrays
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "PT".equals(item.getType()))
                        .filter(Tray.class::isInstance)
                        .map(Tray.class::cast)
                        .collect(Collectors.toList());
        List<Tray> kitTrays
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "KT".equals(item.getType()))
                        .filter(Tray.class::isInstance)
                        .map(Tray.class::cast)
                        .collect(Collectors.toList());
        normalizeNames(inputItems, kitTrays, keepNames, keepFullNames, partsTrays);
        List<PhysicalItem> outList = null;
        if (doPrefEmptySlotsFiltering) {
            outList = filterEmptySlots(inputItems, kitTrays, partsTrays, partsTrayList);
        }
        if (null == outList) {
            outList = new ArrayList<>(inputItems);
        }

        Map<String, Integer> repeatsMap = new HashMap<>();
        for (PhysicalItem ci : outList) {
            if (null == ci || ci.getName().compareTo("*") == 0) {
                continue;
            }
            boolean addRepeatCountsThisItem = addRepeatCountsToName;
            if (addRepeatCountsThisItem) {
                ci.setRepeats((int) repeatsMap.compute(ci.getName(), (String name, Integer reps) -> (reps != null) ? (reps + 1) : 0));
                if (!keepFullNames) {
                    String origFullName = ci.getFullName();
                    String fn = origFullName;
                    if (fn == null || fn.length() < 1) {
                        fn = ci.getName();
                    }
                    while (fn.length() > 1 && (Character.isDigit(fn.charAt(fn.length() - 1)) || fn.charAt(fn.length() - 1) == '_')) {
                        fn = fn.substring(0, fn.length() - 1);
                    }
                    ci.setFullName(fn + "_" + (ci.getRepeats() + 1));
                }
            }
        }
        return outList;
    }

    private static void normalizeNames(
            List<? extends PhysicalItem> inputItems,
            List<Tray> kitTrays,
            boolean keepNames,
            boolean keepFullNames,
            List<Tray> partsTrays) {
        for (PhysicalItem ci : inputItems) {
            if (null == ci || ci.getName().compareTo("*") == 0) {
                continue;
            }
            String name = ci.getName();
            if (name.startsWith("sku_")) {
                name = name.substring(4);
                if (!keepNames) {
                    ci.setName(name);
                }
            }
            if ("P".equals(ci.getType())) {
                if (ci.isInsideKitTray() || inside(kitTrays, ci, 10)) {
                    if (!name.contains("_in_kt") && !name.contains("_in_pt")) {
                        name = name + "_in_kt";
                    } else {
                        throw new RuntimeException("adding in_kt twice name=" + name);
                    }
                    if (!keepNames) {
                        ci.setName(name);
                    }
                    if (!keepFullNames) {
                        ci.setFullName(name);
                    }
                    ci.setInsideKitTray(true);
                } else if (ci.isInsidePartsTray() || inside(partsTrays, ci, 10)) {
                    if (!name.contains("_in_kt") && !name.contains("_in_pt")) {
                        name = name + "_in_pt";
                    } else {
                        throw new RuntimeException("adding in_pt twice name=" + name);
                    }
                    if (!keepNames) {
                        ci.setName(name);
                    }
                    if (!keepFullNames) {
                        ci.setFullName(name);
                    }
                    ci.setInsidePartsTray(true);
                }
            }
            if (!keepFullNames) {
                String ciFullName = ci.getFullName();
                if (name != null && name.length() > 0 && (ciFullName == null || ciFullName.length() < 1 || !ci.isFullNameSet())) {
                    ci.setFullName(name);
                }
            }
        }
    }

    private static List<PhysicalItem> filterEmptySlots(List<PhysicalItem> inputItems, List<Tray> kitTrays, List<Tray> partsTrays, @Nullable List<PartsTray> partsTrayOutList) {
        List<PhysicalItem> parts
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "P".equals(item.getType()))
                        .collect(Collectors.toList());
        List<PhysicalItem> emptySlots
                = inputItems.stream()
                        .filter((PhysicalItem item) -> "ES".equals(item.getType()))
                        .collect(Collectors.toList());
        Comparator<PhysicalItem> kitComparator
                = comparingLong((PhysicalItem kt) -> (kt.getEmptySlotsCount() < 1) ? Long.MAX_VALUE : kt.getEmptySlotsCount());
        kitTrays.sort(kitComparator);
        List<PhysicalItem> firstSortParts = new ArrayList<>(parts);
        firstSortParts.sort(comparingDouble((PhysicalItem p) -> distDiff(p, kitTrays)));
        Map<PhysicalItem, List<PhysicalItem>> partsByTray
                = new TreeMap<>(kitComparator);
        List<PhysicalItem> matchedParts = new ArrayList<>();
        for (Tray kitTray : kitTrays) {
            PhysicalItem kit = kitTray;
            String kitFullName = kit.getFullName();
            if (null == kitFullName) {
                kitFullName = kit.getName();
            }
            PartsTray partstray = new PartsTray(kitFullName);
            partstray.setX(kit.getX());
            partstray.setY(kit.getY());
            partstray.setRotation(kit.getRotation());

            if (null != partsTrayOutList) {
                partsTrayOutList.add(partstray);
            }
            if (null == kit.getEmptySlotsList()) {
                continue;
            }
            List<PhysicalItem> slotFillers = new ArrayList<>();
            for (int j = 0; j < kit.getEmptySlotsList().size(); j++) {
                PhysicalItem slot = kit.getEmptySlotsList().get(j);

                PhysicalItem bestSlotFiller
                        = firstSortParts.stream()
                                .filter(PhysicalItem::isInsidePartsTray)
                                .filter((PhysicalItem p) -> Objects.equals(p.origName, slot.getSlotForSkuName()))
                                .findFirst()
                                .orElse(null);
                if (null != bestSlotFiller) {
                    slotFillers.add(bestSlotFiller);
                    firstSortParts.remove(bestSlotFiller);
                }
            }
            slotFillers.sort(comparingInt((PhysicalItem slotFiller) -> bestIndex(slotFiller, kit.getEmptySlotsList())));
            matchedParts.addAll(slotFillers);
        }
        List<PhysicalItem> list = new ArrayList<>();
        list.addAll(matchedParts);
        list.addAll(firstSortParts);
        list.addAll(partsTrays);
        list.addAll(kitTrays);
        list.addAll(emptySlots);
        return list;
    }

    private int internalDatabaseUpdate(PreparedStatement stmnt, List<UpdateResults> batchUrs, UpdateResults ur, int updates, int updatedCount) {
        try {
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
                                        updatedCount = Integer.parseInt(value);
                                    } catch (NumberFormatException nfe) {
                                        Logger.getLogger(DatabasePoseUpdater.class
                                                .getName()).log(Level.SEVERE, "", nfe);
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
                poses_updated.incrementAndGet();
            }
        } catch (Exception ex) {
            if (null != displayInterface && displayInterface.isDebug()) {
                displayInterface.addLogMessage("Exception :  " + ex + "\r\n");
                displayInterface.addLogMessage("ur = " + ur + "\r\n");
            }
            Logger.getLogger(DatabasePoseUpdater.class.getName()).log(Level.SEVERE, "", ex);
            System.err.println("ur = " + ur);
        }
        return updates;
    }

    private static boolean inside(List<Tray> trays, PhysicalItem ci, double threshhold) {
        return trays.stream().anyMatch((Tray tray) -> tray.insideAbsSlot(ci, threshhold));
    }

    public void deletePose(String name) throws SQLException {
        if (null == queryDeleteSinglePoseString) {
            throw new IllegalStateException("queryDeleteSinglePoseString is null");
        }
        try (PreparedStatement stmnt = getCheckedConnection().prepareStatement(queryDeleteSinglePoseString)) {
            if (null != stmnt) {
                stmnt.setString(1, name);
            }
            stmnt.execute();
        }
    }

    private static String getResultParam(Map<DbParamTypeEnum, String> resultParamMap, DbParamTypeEnum type) {
        String resultString = resultParamMap.get(type);
        if (null == resultString) {
            throw new IllegalArgumentException("parameter for type=" + type + " not found in map:" + resultParamMap);
        }
        return resultString;
    }

    private static Map<DbParamTypeEnum, String> getResultParamMap(Map<DbQueryEnum, DbQueryInfo> queriesMap, DbQueryEnum queryEnum) {
        DbQueryInfo queryInfo = queriesMap.get(queryEnum);
        if (null == queryInfo) {
            throw new IllegalArgumentException("can not find queryInfo for " + queryEnum + " in map: " + queriesMap);
        }
        return queryInfo.getResults();
    }

    private void verifyVisionList(List<PhysicalItem> list, boolean addRepeatCountsToName) {
        try {
            if (null == get_single_statement) {
                return;
            }
            if (null == querySingleString) {
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
                for (PhysicalItem ci : list) {
                    if (null == ci || ci.getName().compareTo("*") == 0) {
                        continue;
                    }
                    String ciFullName = ci.getFullName();
                    if (ci.getName() != null && ci.getName().length() > 0 && (ciFullName == null || ciFullName.length() < 1 || !ci.isFullNameSet())) {
                        ciFullName = ci.getName();
                        ci.setFullName(ciFullName);
                    }
                    if (null == ciFullName) {
                        ciFullName = ci.getName();
                    }
                    if (addRepeatCountsToName) {
                        ciFullName = ci.getName() + "_" + (ci.getRepeats() + 1);
                        ci.setFullName(ciFullName);
                    }
                    List<Object> paramsList = poseParamsToStatement(ci, getSingleParamTypes, get_single_statement);
                    String verifyQueryStringFilled = fillQueryString(querySingleString, paramsList);
                    UpdateResults ur = updateResultsMap.get(ci.getFullName());
                    if (null == ur) {
                        ur = new UpdateResults(ciFullName);
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
                                        = getResultParamMap(queriesMap, DbQueryEnum.GET_SINGLE_POSE);
                                while (rs.next()) {
                                    double x = fixDouble(rs, getResultParam(resultParamMap, DbParamTypeEnum.X));
                                    double y = fixDouble(rs, getResultParam(resultParamMap, DbParamTypeEnum.Y));
                                    double vxi = fixDouble(rs, getResultParam(resultParamMap, DbParamTypeEnum.VXI));
                                    double vxj = fixDouble(rs, getResultParam(resultParamMap, DbParamTypeEnum.VXJ));
                                    if (Math.abs(x - ci.x) < 1e-6
                                            && Math.abs(y - ci.y) < 1e-6
                                            && Math.abs(Math.cos(ci.getRotation()) - vxi) < 1e-6
                                            && Math.abs(Math.sin(ci.getRotation()) - vxj) < 1e-6) {
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
                                            Logger.getLogger(DatabasePoseUpdater.class
                                                    .getName()).log(Level.SEVERE, "", exception);
                                        }
                                        try {
                                            if (null == value) {
                                                value = Objects.toString(rs.getObject(name, Object.class));
                                            }
                                        } catch (Exception exception) {
                                            Logger.getLogger(AprsSystem.class
                                                    .getName()).log(Level.SEVERE, "", exception);
                                        }
                                        if (j == 1 && verifiedCount < 0 && name.startsWith("count")) {
                                            try {
                                                if (null != value) {
                                                    verifiedCount = Integer.parseInt(value);
                                                }
                                            } catch (NumberFormatException nfe) {
                                                Logger.getLogger(DatabasePoseUpdater.class
                                                        .getName()).log(Level.SEVERE, "", nfe);
                                            }
                                        }
                                        if (null != value) {
                                            resultMap.put(name, value);
                                        }
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
                    updateResultsMap.put(ciFullName, ur);
                    if (null != ur.getVerifyException()) {
                        throw new RuntimeException("ur=" + ur, ur.getVerifyException());
                    }
                }
            }

            VisionToDBJFrameInterface di = this.displayInterface;
            if (null != di) {
                if (di.isDebug()) {
                    long t1 = System.nanoTime();
                    di.addLogMessage(String.format("verifyVisionList took %.3f seconds\n", (1e-9 * (t1 - t0))));
                }
                di.updateResultsMap(updateResultsMap);
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
            if (queryStringFilled.contains("{" + paramIndex + "}")) {
                queryStringFilled
                        = queryStringFilled.replace("{" + paramIndex + "}", Objects.toString(paramsList.get(paramIndex - 1)));
            } else if (queryStringFilled.contains("?")) {
                queryStringFilled
                        = queryStringFilled.replace("?", Objects.toString(paramsList.get(paramIndex - 1)));
            }
        }
        try {
            PrintStream ps = dbQueryLogPrintStream;
            if (enableDatabaseUpdates && ps != null) {
                ps.println();
                ps.println(queryStringFilled);
                ps.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return queryStringFilled;
    }

    private List<Object> poseParamsToStatement(PhysicalItem item, DbParamTypeEnum paramTypes[], @Nullable PreparedStatement stmnt) throws SQLException {
        if (null == paramTypes) {
            throw new IllegalArgumentException("paramTypes is null");
        }
        if (dbtype != DbType.NONE) {
            if (null == stmnt) {
                throw new IllegalArgumentException("stmnt is null");
            }
        }
        ArrayList<Object> params = new ArrayList<>();
        for (int i = 0; i < paramTypes.length; i++) {
            DbParamTypeEnum paramTypeEnum = paramTypes[i];
            int index = i + 1;
            switch (paramTypeEnum) {
                case TYPE:
                    params.add("SolidObject");
                    if (null != stmnt) {
                        stmnt.setString(index, "SolidObject");
                    }
                    break;

                case NAME:
                    String quotedName = "\"" + item.getFullName() + "\"";
                    params.add(quotedName);
                    if (null != stmnt) {
                        stmnt.setString(index, item.getFullName());
                    }
                    break;

                case SKU_NAME:
                    String sku = toSku(item.getName());
                    String quotedSKU = "\"" + sku + "\"";
                    params.add(quotedSKU);
                    if (null != stmnt) {
                        stmnt.setString(index, sku);
                    }
                    break;

                case X:
                    params.add(item.x);
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.x);
                    }
                    break;

                case Y:
                    params.add(item.y);
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.y);
                    }
                    break;

                case Z:
                    params.add(item.z);
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.z);
                    }
                    break;

                case VXI:
                    params.add(item.getVxi());
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.getVxi());
                    }
                    break;

                case VXJ:
                    params.add(item.getVxj());
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.getVxj());
                    }
                    break;

                case VXK:
                    params.add(item.getVxk());
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.getVxk());
                    }
                    break;

                case VZI:
                    params.add(item.getVzi());
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.getVzi());
                    }
                    break;

                case VZJ:
                    params.add(item.getVzj());
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.getVzj());
                    }
                    break;

                case VZK:
                    params.add(item.getVzk());
                    if (null != stmnt) {
                        stmnt.setDouble(index, item.getVzk());
                    }
                    break;

                case VISIONCYCLE:
                    params.add(item.getVisioncycle());
                    if (null != stmnt) {
                        stmnt.setInt(index, item.getVisioncycle());
                    }
                    break;

                default:
                    throw new IllegalArgumentException("invalid paramTypeEnum=" + paramTypeEnum + " at index " + index + " in " + Arrays.toString(paramTypes));
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

    private final ExecutorService pqExecServ = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("pqExecServ." + thread.getName() + this.toString());
            thread.setDaemon(true);
            return thread;
        }
    });

    public XFuture<List<PoseQueryElem>> queryDatabase() {
        return XFuture.supplyAsync("queryDatabase", () -> Collections.unmodifiableList(getDirectPoseList()), pqExecServ);
    }

    public XFuture<List<PoseQueryElem>> queryDatabaseNew() {
        return XFuture.supplyAsync("queryDatabaseNew." + this.toString(), () -> Collections.unmodifiableList(getNewDirectPoseList()), pqExecServ);
    }

}
