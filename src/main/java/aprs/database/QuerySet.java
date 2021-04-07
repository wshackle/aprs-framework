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

import static aprs.database.DbCsvBackup.executeQuery;
import static aprs.misc.AprsCommonLogger.println;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.VectorType;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static crcl.utils.CRCLUtils.requireNonNull;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The query set class implements methods to make various common database
 * queries.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class QuerySet implements QuerySetInterface {

    private final @Nullable
    Connection dbConnection;
    private String expectQueryItemFormat = "\'%s\'";

    public String getExpectQueryItemFormat() {
        return expectQueryItemFormat;
    }

    public void setExpectQueryItemFormat(String expectQueryItemFormat) {
        this.expectQueryItemFormat = expectQueryItemFormat;
    }

    /**
     * Get the database connection being used.
     *
     * @return database connection
     */
    public @Nullable
    Connection getDbConnection() {
        return dbConnection;
    }

    /**
     * Check if currently connected to a database.
     *
     * @return if currently connected to database
     */
    public boolean isConnected() {
        try {
            return !closed && null != dbConnection && !dbConnection.isClosed();
        } catch (SQLException ex) {
            Logger.getLogger(QuerySet.class.getName()).log(Level.SEVERE, "", ex);
        }
        return false;
    }

    public final boolean useCsvFilesInsteadOfDatabase;

    /**
     * Create a new QuerySet.
     *
     * @param dbtype database type
     * @param con database connection
     * @param queriesMap map of queries info
     * @param taskName task name used in error/debug messages
     * @param useCsvFilesInsteadOfDatabase queries will use flat csv files instead of database
     * @throws SQLException if query fails
     */
    public QuerySet(
            DbType dbtype,
            @Nullable Connection con,
            Map<DbQueryEnum, DbQueryInfo> queriesMap,
            String taskName,
            boolean useCsvFilesInsteadOfDatabase) throws SQLException {
        this.dbtype = dbtype;
        this.dbConnection = con;
        this.taskName = taskName;
        this.useCsvFilesInsteadOfDatabase = useCsvFilesInsteadOfDatabase;
//        if (null == con) {
//            throw new IllegalArgumentException("connection is null");
//        }
        if (null != con && con.isClosed()) {
            throw new IllegalArgumentException("connection is already closed");
        }
        if (null == queriesMap) {
            throw new IllegalArgumentException("queriesMap is null");
        }
        this.setQueryInfo = queryMap(queriesMap, DbQueryEnum.SET_SINGLE_POSE);
        this.getQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_SINGLE_POSE);
        this.getPartDesignCountQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_PARTDESIGN_PART_COUNT);
        this.getAllPartsInKtQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_ALL_PARTS_IN_KT);
        this.getAllPartsInPtQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_ALL_PARTS_IN_PT);
        this.getTraySlotsFromKitSkuQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_TRAY_SLOTS_FROM_KIT_SKU);
        this.getPartsTraysQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_PARTSTRAYS);
        this.getSlotsQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_SLOTS);

        String getPartDesignPartCountQueryString = getPartDesignCountQueryInfo.getQuery();
        if (null == getPartDesignPartCountQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getPartDesignPartCountQueryString");
        }
        if (con != null) {
            getPartDesignPartCountStatement = con.prepareStatement(getPartDesignPartCountQueryString);
        }

        String getAllPartsInKtQueryString = getAllPartsInKtQueryInfo.getQuery();
        if (null == getAllPartsInKtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInKtQueryString");
        }
        if (con != null) {
            getAllPartsInKtStatement = con.prepareStatement(getAllPartsInKtQueryString);
        }

        String getAllPartsInPtQueryString = getAllPartsInPtQueryInfo.getQuery();
        if (null == getAllPartsInPtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInPtQueryString");
        }
        if (con != null) {
            getAllPartsInPtStatement = con.prepareStatement(getAllPartsInPtQueryString);
        }

        String getTraySlotsFromKitSkuQueryString = getTraySlotsFromKitSkuQueryInfo.getQuery();
        if (null == getTraySlotsFromKitSkuQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getTraySlotsFromKitSkuQueryString");
        }

        if (con != null) {
            getTraySlotsFromKitSkuStatement = con.prepareStatement(getTraySlotsFromKitSkuQueryString);
        }

        String getPoseQueryString = getQueryInfo.getQuery();
        if (null == getPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getPose");
        }

        if (con != null) {
            getPoseStatement = con.prepareStatement(getPoseQueryString);
        }
        if (null == getAllPartsInKtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInKtQueryString");
        }

        if (con != null) {
            getAllPartsInKtStatement = con.prepareStatement(getAllPartsInKtQueryString);
        }

        if (null == getAllPartsInPtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInPtQueryString");
        }
        if (con != null) {
            getAllPartsInPtStatement = con.prepareStatement(getAllPartsInPtQueryString);
        }

        String setPoseQueryString = setQueryInfo.getQuery();
        if (null == setPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain setPose");
        }
        if (con != null) {
            setPoseStatement = con.prepareStatement(setPoseQueryString);
        }

        String getPartsTraysQueryString = getPartsTraysQueryInfo.getQuery();
        if (null == getPartsTraysQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain query for  getPartsTrays");
        }
        if (con != null) {
            this.getPartsTraysStatement = con.prepareStatement(getPartsTraysQueryString);
        }
        String getSlotsQueryString = getSlotsQueryInfo.getQuery();
        if (null == getSlotsQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain query for getSlots");
        }
        if (con != null) {
            this.getSlotsStatement = con.prepareStatement(getSlotsQueryString);
        }
        this.queriesMap = queriesMap;
    }

    private static DbQueryInfo queryMap(Map<DbQueryEnum, DbQueryInfo> queriesMap1, DbQueryEnum qtype) throws IllegalArgumentException {
        DbQueryInfo qi = queriesMap1.get(qtype);
        if (null == qi) {
            throw new IllegalArgumentException("queriesMap has no entry for " + qtype);
        }
        return qi;
    }

    private final DbType dbtype;
    private final Map<DbQueryEnum, DbQueryInfo> queriesMap;
    private @Nullable
    PreparedStatement getPoseStatement;
    private @Nullable
    PreparedStatement getPartDesignPartCountStatement;
    private @Nullable
    PreparedStatement setPoseStatement;
    private @Nullable
    PreparedStatement getTraySlotsFromKitSkuStatement;
    private @Nullable
    PreparedStatement getAllPartsInKtStatement;
    private @Nullable
    PreparedStatement getAllPartsInPtStatement;
    private @Nullable
    PreparedStatement getPartsTraysStatement;
    private @Nullable
    PreparedStatement getSlotsStatement;

    private boolean closed = false;

    private String getQueryFormat() {
        switch (dbtype) {
            case MYSQL:
                return "?";

            case NEO4J:
            case NONE:
            default:
                return "{%d}";
        }
//        throw new IllegalStateException("no query format for dbtype=" + dbtype);
    }

    private static String trimQuotes(@Nullable String s) {
        if (null == s) {
            return "";
        }
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    private void setQueryStringParam(@Nullable PreparedStatement stmnt,
            DbQueryInfo queryInfo,
            DbParamTypeEnum type,
            String value,
            Map<Integer, Object> map) throws SQLException {

        Map<DbParamTypeEnum, Integer> posParamMap = queryInfo.getParamPosMap();
        if (null == posParamMap) {
            throw new IllegalArgumentException("QueryInfo has no posParamMap type=" + type + ", value=" + value + ", params=" + Arrays.toString(queryInfo.getParams()));
        }
        if (!posParamMap.containsKey(type)) {
            throw new IllegalArgumentException("No entry for type=" + type + " in params=" + Arrays.toString(queryInfo.getParams()));
        }
        int index = posParamMap.get(type);
        if (null != map) {
            map.put(index, value);
        }
        if (null != stmnt) {
            stmnt.setString(index, value);
        }

    }

    private void setQueryDoubleParam(@Nullable PreparedStatement stmnt,
            DbQueryInfo queryInfo,
            DbParamTypeEnum type,
            double value,
            Map<Integer, Object> map) throws SQLException {

        Map<DbParamTypeEnum, Integer> paramPosMap = queryInfo.getParamPosMap();
        if (null == paramPosMap) {
            throw new IllegalArgumentException("No paramPoseMap in queryInfo=" + queryInfo);
        }
        if (!paramPosMap.containsKey(type)) {
            throw new IllegalArgumentException("No entry for type=" + type + " in params=" + Arrays.toString(queryInfo.getParams()));
        }
        int index = paramPosMap.get(type);
        if (null != map) {
            map.put(index, value);
        }
        if (null != stmnt) {
            stmnt.setDouble(index, value);
        }
    }

    private String createExpectedQueryString(
            DbQueryInfo queryInfo,
            Map<Integer, Object> map
    ) {
        String queryFormat = getQueryFormat();
        DbParamTypeEnum paramTypes[] = queryInfo.getParams();
        String qString = queryInfo.getQuery();
        if (null != paramTypes) {
            for (int i = 0; i < paramTypes.length; i++) {
                Object o = map.get(i + 1);
                if (null != o) {
                    qString = qString.replace(
                            String.format(queryFormat, i + 1),
                            String.format(this.expectQueryItemFormat,
                                    o.toString())
                    );
                }
            }
        }
        return qString;
    }

    private @Nullable
    String getQueryResultString(ResultSet rs, DbQueryInfo queryInfo, DbParamTypeEnum type) throws SQLException {
        Map<DbParamTypeEnum, String> map = queryInfo.getResults();
        String qname = map.get(type);
        if (null == qname) {
            throw new IllegalArgumentException("No entry for type " + type + " in map =" + map);
        }
        if (Character.isDigit(qname.charAt(0))) {
            int index = Integer.parseInt(qname);
            return rs.getString(index);
        }
        return rs.getString(qname);
    }

    private int getQueryResultInt(ResultSet rs, DbQueryInfo queryInfo, DbParamTypeEnum type) throws SQLException {
        Map<DbParamTypeEnum, String> map = queryInfo.getResults();
        String qname = map.get(type);
        if (null == qname) {
            throw new IllegalArgumentException("No entry for type " + type + " in map =" + map);
        }
        if (Character.isDigit(qname.charAt(0))) {
            int index = Integer.parseInt(qname);
            return rs.getInt(index);
        }
        return rs.getInt(qname);
    }

    private double getQueryResultDouble(ResultSet rs, DbQueryInfo queryInfo, DbParamTypeEnum type) throws SQLException {
        Map<DbParamTypeEnum, String> map = queryInfo.getResults();
        String qname = map.get(type);
        if (null == qname) {
            throw new IllegalArgumentException("No entry for type " + type + " in map =" + map);
        }
        if (Character.isDigit(qname.charAt(0))) {
            int index = Integer.parseInt(qname);
            return rs.getDouble(index);
        }
        return rs.getDouble(qname);
    }

    private @Nullable
    String getPoseQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    private @Nullable
    String getAllPartsInKtQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    private @Nullable
    String getAllPartsInPtQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    private @Nullable
    String getTraySlotsFromKitSkuQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getTraySlotsFromKitSkuQueryInfo, type);
    }

    private @Nullable
    String getPartsTraysQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getPartsTraysQueryInfo, type);
    }

    private @Nullable
    String getSlotsQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getSlotsQueryInfo, type);
    }
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

    /**
     * Get all parts_in_kt from the database
     *
     * @param name name of kit tray
     * @return a list of all the parts that has "parts_in_kt" in their names
     * @throws java.sql.SQLException if query fails
     * @throws java.io.IOException csv files do not exist
     */
    public List<String> getAllPartsInKt(String name) throws SQLException, IOException {
        ArrayList<String> partsInKtList = new ArrayList<>();
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }

        Map<Integer, Object> map = new TreeMap<>();
        setQueryStringParam(getAllPartsInKtStatement, getAllPartsInKtQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getAllPartsInKtQueryInfo, map);
        if (debug) {
            println("simQuery = " + simQuery);
        }

        try (ResultSet rs = executeQuery(getAllPartsInKtStatement, simQuery, "getAllPartsInKT_" + name, taskName, this.dbConnection == null)) {
            //int c = 0;
            while (rs.next()) {
                //c++;
                String nameCheckString = getAllPartsInKtQueryResultString(rs, DbParamTypeEnum.NAME);
                //println("nameCheckString = " + nameCheckString);
                if (null != nameCheckString) {
                    partsInKtList.add(nameCheckString);
                }
            }
        }
        return partsInKtList;
    }

    public ArrayList<String> getAllPartsInPt(String name) throws SQLException, IOException {
        ArrayList<String> partsInPtList = new ArrayList<>();
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }

        Map<Integer, Object> map = new TreeMap<>();
        setQueryStringParam(getAllPartsInPtStatement, getAllPartsInPtQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getAllPartsInPtQueryInfo, map);

        if (debug) {
            println("simQuery = " + simQuery);
        }

        try (ResultSet rs = executeQuery(getAllPartsInPtStatement, simQuery, "getAllPartsInPt_" + name, taskName, this.dbConnection == null)) {
            //int c = 0;
            while (rs.next()) {
                //c++;
                String nameCheckString = getAllPartsInPtQueryResultString(rs, DbParamTypeEnum.NAME);
                //println("nameCheckString = " + nameCheckString);
                if (null != nameCheckString) {
                    partsInPtList.add(nameCheckString);
                }
            }
        }
        return partsInPtList;
    }

    public int getPartDesignPartCount(String name) throws SQLException, IOException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        int count = 0;
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getCountQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_PARTDESIGN_PART_COUNT);
        setQueryStringParam(getPartDesignPartCountStatement, getCountQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getCountQueryInfo, map);
        if (debug) {
            println("simQuery = " + simQuery);
        }

        try (ResultSet rs = executeQuery(getPartDesignPartCountStatement, simQuery, "getPartDesignPartCount_" + name, taskName, this.dbConnection == null)) {
            if (rs.next()) {
                count = rs.getInt(1);
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getPartDesignPartCount for name=" + name + ",\n   simQuery=" + simQuery);
            }
        }
        return count;
    }

    public List<PartsTray> getPartsTrays(String name) throws SQLException, IOException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }

        debug = true;
        List<PartsTray> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        setQueryStringParam(getPartsTraysStatement, getPartsTraysQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getPartsTraysQueryInfo, map);

        if (debug) {
            println("name=" + name + ", simQuery = " + simQuery);
        }

        try (ResultSet rs = executeQuery(getPartsTraysStatement, simQuery, "getPartsTrays_" + name, taskName, this.dbConnection == null)) {
            while (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        println("o = " + o);
                    }
                }

                String partsTrayName = getPartsTraysQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    println("partsTrayName = " + partsTrayName);
                }
                if (null == partsTrayName) {
                    throw new IllegalStateException("result is missing name for query :" + simQuery);
                }
                PartsTray partstray = new PartsTray(partsTrayName);
                //-- getting node id
                int id = getQueryResultInt(rs, getPartsTraysQueryInfo, DbParamTypeEnum.NODE_ID);
                partstray.setNodeID(id);
                if (debug) {
                    println("id = " + id);
                }
                //-- getting design
                String design = getPartsTraysQueryResultString(rs, DbParamTypeEnum.TRAY_DESIGN_NAME);
                if (null == design) {
                    throw new IllegalStateException("result is missing design name for query :" + simQuery);
                }
                partstray.setPartsTrayDesign(design);
                if (debug) {
                    println("design = " + design);
                }

                String sku = getPartsTraysQueryResultString(rs, DbParamTypeEnum.SKU_NAME);
                if (null == sku) {
                    throw new IllegalStateException("result is missing sku for query :" + simQuery);
                }
                partstray.setPartsTraySku(sku);
                if (debug) {
                    println("sku = " + sku);
                }
                //-- gettig complete
                String complete = trimQuotes(getPartsTraysQueryResultString(rs, DbParamTypeEnum.TRAY_COMPLETE));
                if (null == complete) {
                    throw new IllegalStateException("result is missing complet info for query :" + simQuery);
                }
                partstray.setPartsTrayComplete(Boolean.valueOf(complete));
//                if (debug) {
//                    if (partstray.getPartsTrayComplete()) {
//                        println("complete = true");
//                    } else {
//                        println("complete = false");
//                    }
//                }
                //-- get a list of slots for this parts tray
                partstray.setSlotList(getSlots(partsTrayName));
                //displayPartsTray(partstray);
                list.add(partstray);
            }
        }
        if (debug) {
            System.out.println("list = " + list);
        }
        return list;
    }

    private List<Slot> getSlots(String name) throws SQLException, IOException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
//        assert (null != getPartsTraysStatement) :
//                ("null == getPartsTraysStatement");

        List<Slot> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getSlotsQueryInfoFromMap = queryMap(queriesMap, DbQueryEnum.GET_SLOTS);
        setQueryStringParam(getSlotsStatement, getSlotsQueryInfoFromMap, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getSlotsQueryInfoFromMap, map);

        if (debug) {
            println("simQuery = " + simQuery);
        }

        try (ResultSet rs = executeQuery(getSlotsStatement, simQuery, "getSlots_" + name, taskName, this.dbConnection == null)) {
            while (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        println("o = " + o);
                    }
                }

                //-- slot name
                String SlotName = getSlotsQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    println("SlotName = " + SlotName);
                }
                if (null == SlotName) {
                    throw new IllegalStateException("result is missing slotName for query :" + simQuery);
                }
                Slot slot = new Slot(SlotName);

                //-- ID of the slot
                String slot_id = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.SLOT_ID));
                int slotInt = Integer.parseInt(slot_id);
                slot.setID(slotInt);
                if (debug) {
                    println("slot_id = " + slot_id);
                }

                //-- is slot occupied?
                String occupied = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.SLOT_OCCUPIED));
                slot.setSlotOccupied(Boolean.valueOf(occupied));
                if (debug) {
                    if (slot.getSlotOccupied()) {
                        println("occupied = true");
                    } else {
                        println("occupied = false");
                    }
                }
                //-- part sku that goes in this slot
                String partSku = getSlotsQueryResultString(rs, DbParamTypeEnum.SKU_NAME);
                if (debug) {
                    println("partSku = " + partSku);
                }
                if (null == partSku) {
                    Logger.getLogger(QuerySet.class.getName()).log(Level.WARNING, "no partSKU in results for SlotName={0}", SlotName);
                    continue;
                }
                slot.setPartSKU(partSku);

                //-- X offset
                String xString = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.X));
                double xDouble = Double.parseDouble(xString);
                if (debug) {
                    println("xString = " + xDouble);
                }
                slot.setX_OFFSET(xDouble);

                //-- Y offset
                String yString = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.Y));
                double yDouble = Double.parseDouble(yString);
                if (debug) {
                    println("yString = " + yDouble);
                }
                slot.setY_OFFSET(yDouble);

                list.add(slot);
            }
        }
        return list;
    }

    @Override
    public @Nullable
    PoseType getPose(String name) throws SQLException, IOException {
        return getPose(name, false, 0);
    }

    private final String taskName;

    public @Nullable
    PoseType getPose(String name, boolean requireNew, int visionCycleNewDiffThreshold) throws SQLException, IOException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        PoseType pose = new PoseType();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getPoseQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_SINGLE_POSE);
        setQueryStringParam(getPoseStatement, getPoseQueryInfo, DbParamTypeEnum.NAME, name, map);

        String simQuery = createExpectedQueryString(getPoseQueryInfo, map);
        if (debug) {
            println("Debuging getPose(" + name + "," + requireNew + "," + visionCycleNewDiffThreshold + ")");
            println("simQuery = \"\n" + simQuery + "\n\"\n");
            if (null != dbConnection) {
                String connectionUrl = dbConnection.getMetaData().getURL();
                println("connectionUrl = " + connectionUrl);
            }
            println("");
        }
        try (ResultSet rs = executeQuery(getPoseStatement, simQuery, "getPose_" + name, taskName, this.dbConnection == null)) {
            if (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        println("o = " + o);
                    }
                }
                String nameCheckString = getPoseQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    println("nameCheckString = " + nameCheckString);
                }
                assert (null != nameCheckString) : "nameCheckString == null : @AssumeAssertion(nullness)";
                assert (nameCheckString.equals(name)) :
                        ("returned name " + nameCheckString + " does not match requested name " + name);

                String xString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.X));
                if (debug) {
                    println("xString = " + xString);
                }
                PointType point = new PointType();
                double x = Double.parseDouble(xString);
                point.setX(x);
                String yString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Y));
                if (debug) {
                    println("yString = " + yString);
                }
                double y = Double.parseDouble(yString);
                point.setY(y);
                String zString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Z));
                if (debug) {
                    println("zString = " + zString);
                }
                if (null != zString) {
                    double z = Double.parseDouble(zString);
                    point.setZ(z);
                } else {
                    point.setZ(0.0);
                }
                pose.setPoint(point);
                VectorType xAxis = new VectorType();
                String vxiString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXI));
                if (debug) {
                    println("vxiString = " + vxiString);
                }
                double vxi = Double.parseDouble(vxiString);
                xAxis.setI(vxi);
                String vxjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXJ));
                if (debug) {
                    println("vxjString = " + vxjString);
                }
                double vxj = Double.parseDouble(vxjString);
                xAxis.setJ(vxj);
                String vxkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXK));
                if (debug) {
                    println("vxkString = " + vxkString);
                }
                double vxk = Double.parseDouble(vxkString);
                xAxis.setK(vxk);
                pose.setXAxis(xAxis);
                VectorType zAxis = new VectorType();
                String vziString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZI));
                if (debug) {
                    println("vziString = " + vziString);
                }
                double vzi = Double.parseDouble(vziString);
                zAxis.setI(vzi);
                String vzjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZJ));
                if (debug) {
                    println("vziString = " + vzjString);
                }
                double vzj = Double.parseDouble(vzjString);
                zAxis.setJ(vzj);
                String vzkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZK));
                if (debug) {
                    println("vzkString = " + vzkString);
                }
                double vzk = Double.parseDouble(vzkString);
                zAxis.setK(vzk);
                pose.setZAxis(zAxis);
                String visionCycleString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VISIONCYCLE));
                int visionCycle = -1;
                if (null != visionCycleString) {
                    visionCycle = Integer.parseInt(visionCycleString);
                }
                if (debug) {
                    println("visionCycleString = " + visionCycleString);
                }
                String maxVisionCycleString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.MAX_VISIONCYCLE));
                int maxVisionCycle = -1;
                if (null != maxVisionCycleString) {
                    maxVisionCycle = Integer.parseInt(maxVisionCycleString);
                }
                if (debug) {
                    println("maxVisionCycleString = " + maxVisionCycleString);
                }

                if (requireNew && maxVisionCycle > visionCycle + visionCycleNewDiffThreshold) {
//                    printDebugUpdateResultsMap(name);
                    if (debug) {
                        println("returning null from getPose because (maxVisionCycle > visionCycle + visionCycleNewDiffThreshold)  where maxVisionCycle=" + maxVisionCycle + ",visionCycle=" + visionCycle + ",visionCycleNewDiffThreshold=" + visionCycleNewDiffThreshold);
                        println("End Debuging getPose(" + name + "," + requireNew + "," + visionCycleNewDiffThreshold + ")");
                        println("");
                        Thread.dumpStack();
                    }
                    return null;
                }
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getPose for name=" + name + ", simQuery=" + simQuery);
            }
            if (debug) {
                println("End Debuging getPose(" + name + "," + requireNew + "," + visionCycleNewDiffThreshold + ")");
                println("");
            }
//            if (rs.next()) {
//                String nameCheckString = rs.getString(1);
//                println("nameCheckString = " + nameCheckString);
//                int count =1;
//                while(rs.next()) {
//                    println("rs.getString(1) = " + rs.getString(1));
//                    count++;
//                    println("count = " + count);
//                }
//                throw new IllegalStateException("More than one result for name=" + name);
//            }
        }
        return pose;
    }

    @SuppressWarnings("try")
    private static class QsResultSet implements AutoCloseable {

        private final ResultSet resultSet;
        private final CSVPrinter printer;

        public QsResultSet(ResultSet resultSet, CSVPrinter printer) {
            this.resultSet = resultSet;
            this.printer = printer;
        }

        public ResultSet getResultSet() {
            return resultSet;
        }

        public CSVPrinter getPrinter() {
            return printer;
        }

        @Override
        public void close() throws Exception {
            resultSet.close();
            printer.close();
        }
    }

    public List<PhysicalItem> getAllNewParts(int visionCycleNewDiffThreshold) throws SQLException, IOException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        PoseType pose = new PoseType();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getAllNewPoseQueryInfo = queryMap(queriesMap, DbQueryEnum.GET_ALL_NEW_POSE);
        List<PhysicalItem> ret = new ArrayList<>();
        String simQuery = createExpectedQueryString(getAllNewPoseQueryInfo, map);
        if (debug) {
            println("simQuery = " + simQuery);
        }
        try (ResultSet rs = executeQuery(getPoseStatement, simQuery, "getAllNewParts_" + visionCycleNewDiffThreshold, taskName, this.dbConnection == null)) {
            if (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        println("o = " + o);
                    }
                }
                String name = getPoseQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    println("nameCheckString = " + name);
                }
                if (name == null) {
                    throw new IllegalStateException("Query result has null name");
                }
                String xString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.X));
                if (debug) {
                    println("xString = " + xString);
                }
                PointType point = new PointType();
                double x = Double.parseDouble(xString);
                point.setX(x);
                String yString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Y));
                if (debug) {
                    println("yString = " + yString);
                }
                double y = Double.parseDouble(yString);
                point.setY(y);
                String zString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Z));
                if (debug) {
                    println("zString = " + zString);
                }
                if (null != zString) {
                    double z = Double.parseDouble(zString);
                    point.setZ(z);
                } else {
                    point.setZ(0.0);
                }
                pose.setPoint(point);
                VectorType xAxis = new VectorType();
                String vxiString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXI));
                if (debug) {
                    println("vxiString = " + vxiString);
                }
                double vxi = Double.parseDouble(vxiString);
                xAxis.setI(vxi);
                String vxjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXJ));
                if (debug) {
                    println("vxjString = " + vxjString);
                }
                double vxj = Double.parseDouble(vxjString);
                xAxis.setJ(vxj);
                String vxkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXK));
                if (debug) {
                    println("vxkString = " + vxkString);
                }
                double vxk = Double.parseDouble(vxkString);
                xAxis.setK(vxk);
                pose.setXAxis(xAxis);
                VectorType zAxis = new VectorType();
                String vziString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZI));
                if (debug) {
                    println("vziString = " + vziString);
                }
                double vzi = Double.parseDouble(vziString);
                zAxis.setI(vzi);
                String vzjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZJ));
                if (debug) {
                    println("vziString = " + vzjString);
                }
                double vzj = Double.parseDouble(vzjString);
                zAxis.setJ(vzj);
                String vzkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZK));
                if (debug) {
                    println("vzkString = " + vzkString);
                }
                double vzk = Double.parseDouble(vzkString);
                zAxis.setK(vzk);
                pose.setZAxis(zAxis);
                String visionCycleString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VISIONCYCLE));
                int visionCycle = -1;
                if (null != visionCycleString) {
                    visionCycle = Integer.parseInt(visionCycleString);
                }
                if (debug) {
                    println("visionCycleString = " + visionCycleString);
                }
                String maxVisionCycleString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.MAX_VISIONCYCLE));
                int maxVisionCycle = -1;
                if (null != maxVisionCycleString) {
                    maxVisionCycle = Integer.parseInt(maxVisionCycleString);
                }
                if (debug) {
                    println("maxVisionCycleString = " + maxVisionCycleString);
                }
                if (maxVisionCycle > visionCycle + visionCycleNewDiffThreshold) {
                    return Collections.emptyList();
                }
                PhysicalItem item = PhysicalItem.newPhysicalItemNamePoseVisionCycle(name, pose, visionCycle);
                ret.add(item);
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getAllNewParts, simQuery=" + simQuery);
            }
//            if (rs.next()) {
//                String nameCheckString = rs.getString(1);
//                println("nameCheckString = " + nameCheckString);
//                int count =1;
//                while(rs.next()) {
//                    println("rs.getString(1) = " + rs.getString(1));
//                    count++;
//                    println("count = " + count);
//                }
//                throw new IllegalStateException("More than one result for name=" + name);
//            }
        }
        return ret;
    }

    @Override
    public void close() {
        try {
            closed = true;
            if (null != getPoseStatement) {
                getPoseStatement.close();
                getPoseStatement = null;
            }
            if (null != setPoseStatement) {
                setPoseStatement.close();
                setPoseStatement = null;
            }
            if (null != getTraySlotsFromKitSkuStatement) {
                getTraySlotsFromKitSkuStatement.close();
                getTraySlotsFromKitSkuStatement = null;
            }
            if (null != getPartsTraysStatement) {
                getPartsTraysStatement.close();
                getPartsTraysStatement = null;
            }
            if (null != getSlotsStatement) {
                getSlotsStatement.close();
                getSlotsStatement = null;
            }
            if (null != getAllPartsInKtStatement) {
                getAllPartsInKtStatement.close();
                getAllPartsInKtStatement = null;
            }
            if (null != getAllPartsInPtStatement) {
                getAllPartsInPtStatement.close();
                getAllPartsInPtStatement = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(QuerySet.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    @Override
    @SuppressWarnings({"FinalizeDeclaration", "deprecation"})
    protected void finalize() throws Throwable {
        try {
            this.close();
        } catch (Throwable t) {
            // not mu
        }
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

    private final DbQueryInfo setQueryInfo;
    private final DbQueryInfo getQueryInfo;
    private final DbQueryInfo getPartDesignCountQueryInfo;
    private final DbQueryInfo getAllPartsInKtQueryInfo;
    private final DbQueryInfo getAllPartsInPtQueryInfo;
    private final DbQueryInfo getTraySlotsFromKitSkuQueryInfo;
    private final DbQueryInfo getPartsTraysQueryInfo;
    private final DbQueryInfo getSlotsQueryInfo;

    private void setPoseQueryStringParam(DbParamTypeEnum type, String value, Map<Integer, Object> map) throws SQLException {
        setQueryStringParam(setPoseStatement, setQueryInfo, type, value, map);
    }

//    private void setPoseQueryDoubleParam(DbParamTypeEnum type, double value, Map<Integer, Object> map) throws SQLException {
//        setQueryDoubleParam(setPoseStatement, setQueryInfo, type, value, map);
//    }
    private void setPoseQueryDoubleParam(DbParamTypeEnum type, BigDecimal value, Map<Integer, Object> map) throws SQLException {
        setQueryDoubleParam(setPoseStatement, setQueryInfo, type, value.doubleValue(), map);
    }

    private void setPoseQueryDoubleParam(DbParamTypeEnum type, double value, Map<Integer, Object> map) throws SQLException {
        setQueryDoubleParam(setPoseStatement, setQueryInfo, type, value, map);
    }

    private static String toSku(String name) {
        String sku = name;
        if (!sku.startsWith("sku_")) {
            sku = "sku_" + sku;
        }
        return sku;
    }

    @Override
    public void setPose(String name, PoseType pose) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        if (null == pose || null == pose.getPoint() || null == pose.getXAxis() || null == pose.getZAxis()) {
            throw new IllegalArgumentException("pose must not be null and must not have null point,xaxis or zaxis");
        }
        final PreparedStatement setPoseStatement1 = setPoseStatement;
        if(null == setPoseStatement1) {
            throw new NullPointerException("setPoseStatement");
        }
        setPoseStatement1.setString(1, name);

        Map<Integer, Object> map = new TreeMap<>();
        setPoseQueryStringParam(DbParamTypeEnum.NAME, name, map);
        setPoseQueryStringParam(DbParamTypeEnum.SKU_NAME, toSku(name), map);
        PointType point = requireNonNull(pose.getPoint(), "pose.getPoint()");
        setPoseQueryDoubleParam(DbParamTypeEnum.X, point.getX(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.Y, point.getY(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.Z, point.getZ(), map);
        VectorType xAxis = requireNonNull(pose.getXAxis(), "pose.getXAxis()");
        setPoseQueryDoubleParam(DbParamTypeEnum.VXI, xAxis.getI(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VXJ, xAxis.getJ(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VXK, xAxis.getK(), map);
        VectorType zAxis = requireNonNull(pose.getZAxis(), "pose.getZAxis()");
        setPoseQueryDoubleParam(DbParamTypeEnum.VZI, zAxis.getI(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VZJ, zAxis.getJ(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VZK, zAxis.getK(), map);
        String simQuery = createExpectedQueryString(setQueryInfo, map);
        if (debug) {
            println("setPose(" + name + "," + pose + "): simQuery = " + simQuery);
        }

        int update_count = setPoseStatement1.executeUpdate();
        if (debug) {
            println("update_count = " + update_count);
        }
    }
}
