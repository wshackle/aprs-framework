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

import aprs.framework.AprsJFrame;
import aprs.framework.pddl.executor.TraySlotDesign;
import aprs.framework.spvision.UpdateResults;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.VectorType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The query set class implements methods to make various common database
 * queries.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class QuerySet implements QuerySetInterface {

    private final java.sql.Connection dbConnection;
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
    public Connection getDbConnection() {
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
            Logger.getLogger(QuerySet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Create a new QuerySet.
     *
     * @param dbtype database type
     * @param con database connection
     * @param queriesMap map of queries info
     * @throws SQLException if query fails
     */
    public QuerySet(
            DbType dbtype,
            java.sql.Connection con,
            Map<DbQueryEnum, DbQueryInfo> queriesMap) throws SQLException {
        this.dbtype = dbtype;
        this.dbConnection = con;
        if (null == con) {
            throw new IllegalArgumentException("connection is null");
        }
        if (con.isClosed()) {
            throw new IllegalArgumentException("connection is already closed");
        }
        if (null == queriesMap) {
            throw new IllegalArgumentException("queriesMap is null");
        }
        this.setQueryInfo = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE);
        if (null == setQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.SET_SINGLE_POSE);
        }
        this.getQueryInfo = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE);
        if (null == getQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_SINGLE_POSE);
        }
        this.getPartDesignCountQueryInfo = queriesMap.get(DbQueryEnum.GET_PARTDESIGN_PART_COUNT);
        if (null == getPartDesignCountQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_PARTDESIGN_PART_COUNT);
        }
        this.getAllPartsInKtQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_PARTS_IN_KT);
        if (null == getAllPartsInKtQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_ALL_PARTS_IN_KT);
        }
        this.getAllPartsInPtQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_PARTS_IN_PT);
        if (null == getAllPartsInPtQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_ALL_PARTS_IN_PT);
        }
        this.getTraySlotsFromKitSkuQueryInfo = queriesMap.get(DbQueryEnum.GET_TRAY_SLOTS_FROM_KIT_SKU);
        if (null == getTraySlotsFromKitSkuQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_TRAY_SLOTS_FROM_KIT_SKU);
        }
        this.getPartsTraysQueryInfo = queriesMap.get(DbQueryEnum.GET_PARTSTRAYS);
        if (null == getPartsTraysQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_PARTSTRAYS);
        }
        this.getSlotsQueryInfo = queriesMap.get(DbQueryEnum.GET_SLOTS);
        if (null == getSlotsQueryInfo) {
            throw new IllegalArgumentException("queriesMap has no entry for " + DbQueryEnum.GET_SLOTS);
        }

        String getPartDesignPartCountQueryString = getPartDesignCountQueryInfo.getQuery();
        if (null == getPartDesignPartCountQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getPartDesignPartCountQueryString");
        }
        getPartDesignPartCountStatement = con.prepareStatement(getPartDesignPartCountQueryString);

        String getAllPartsInKtQueryString = getAllPartsInKtQueryInfo.getQuery();
        if (null == getAllPartsInKtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInKtQueryString");
        }
        getAllPartsInKtStatement = con.prepareStatement(getAllPartsInKtQueryString);

        String getAllPartsInPtQueryString = getAllPartsInPtQueryInfo.getQuery();
        if (null == getAllPartsInPtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInPtQueryString");
        }
        getAllPartsInPtStatement = con.prepareStatement(getAllPartsInPtQueryString);

        String getTraySlotsFromKitSkuQueryString = getTraySlotsFromKitSkuQueryInfo.getQuery();
        if (null == getTraySlotsFromKitSkuQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getTraySlotsFromKitSkuQueryString");
        }
        getTraySlotsFromKitSkuStatement = con.prepareStatement(getTraySlotsFromKitSkuQueryString);
        String getPoseQueryString = getQueryInfo.getQuery();
        if (null == getPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getPose");
        }
        getPoseStatement = con.prepareStatement(getPoseQueryString);

        if (null == getAllPartsInKtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInKtQueryString");
        }
        getAllPartsInKtStatement = con.prepareStatement(getAllPartsInKtQueryString);

        if (null == getAllPartsInPtQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getAllPartsInPtQueryString");
        }
        getAllPartsInPtStatement = con.prepareStatement(getAllPartsInPtQueryString);

        String setPoseQueryString = setQueryInfo.getQuery();
        if (null == setPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain setPose");
        }
        setPoseStatement = con.prepareStatement(setPoseQueryString);
        DbQueryInfo getAllTraySlogDesignsQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_TRAY_SLOT_DESIGNS);
        if (null != getAllTraySlogDesignsQueryInfo) {
            String getAllTrayDesignsQueryString = getAllTraySlogDesignsQueryInfo.getQuery();
            if (null != getAllTrayDesignsQueryString) {
                this.getAllTrayDesignsStatement = con.prepareStatement(getAllTrayDesignsQueryString);
            }
        }

        DbQueryInfo getSingleTraySlogDesignQueryInfo = queriesMap.get(DbQueryEnum.GET_SINGLE_TRAY_SLOT_DESIGN);
        if (null != getSingleTraySlogDesignQueryInfo) {
            String getSingleTrayDesignQueryString = getSingleTraySlogDesignQueryInfo.getQuery();
            if (null != getSingleTrayDesignQueryString) {
                this.getSingleTrayDesignStatement = con.prepareStatement(getSingleTrayDesignQueryString);
            }
        }
        DbQueryInfo setSingleTraySlogDesignQueryInfo = queriesMap.get(DbQueryEnum.SET_SINGLE_TRAY_SLOT_DESIGN);
        if (null != setSingleTraySlogDesignQueryInfo) {
            String setSingleTrayDesignQueryString = setSingleTraySlogDesignQueryInfo.getQuery();
            if (null != setSingleTrayDesignQueryString) {
                this.setSingleTrayDesignStatement = con.prepareStatement(setSingleTrayDesignQueryString);
            }
        }
        DbQueryInfo newSingleTraySlogDesignQueryInfo = queriesMap.get(DbQueryEnum.NEW_SINGLE_TRAY_SLOT_DESIGN);
        if (null != newSingleTraySlogDesignQueryInfo) {
            String newSingleTrayDesignQueryString = newSingleTraySlogDesignQueryInfo.getQuery();
            if (null != newSingleTrayDesignQueryString) {
                this.newSingleTrayDesignStatement = con.prepareStatement(newSingleTrayDesignQueryString);
            }
        }
        DbQueryInfo getPartsTraysQueryInfo = queriesMap.get(DbQueryEnum.GET_PARTSTRAYS);
        if (null != getPartsTraysQueryInfo) {
            String getPartsTraysQueryString = getPartsTraysQueryInfo.getQuery();
            if (null != getPartsTraysQueryString) {
                this.getPartsTraysStatement = con.prepareStatement(getPartsTraysQueryString);
            }
        }
        DbQueryInfo getSlotsQueryInfo = queriesMap.get(DbQueryEnum.GET_SLOTS);
        if (null != getSlotsQueryInfo) {
            String getSlotsQueryString = getSlotsQueryInfo.getQuery();
            if (null != getSlotsQueryString) {
                this.getSlotsStatement = con.prepareStatement(getSlotsQueryString);
            }
        }
        this.queriesMap = queriesMap;

    }

    private final DbType dbtype;
    private final Map<DbQueryEnum, DbQueryInfo> queriesMap;
    private java.sql.PreparedStatement getPoseStatement;
    private java.sql.PreparedStatement getPartDesignPartCountStatement;
    private java.sql.PreparedStatement setPoseStatement;
    private java.sql.PreparedStatement getAllTrayDesignsStatement;
    private java.sql.PreparedStatement getSingleTrayDesignStatement;
    private java.sql.PreparedStatement setSingleTrayDesignStatement;
    private java.sql.PreparedStatement newSingleTrayDesignStatement;
    private java.sql.PreparedStatement getTraySlotsFromKitSkuStatement;
    private java.sql.PreparedStatement getAllPartsInKtStatement;
    private java.sql.PreparedStatement getAllPartsInPtStatement;
    private java.sql.PreparedStatement getPartsTraysStatement;
    private java.sql.PreparedStatement getSlotsStatement;

    private boolean closed = false;

    private String getQueryFormat() {
        switch (dbtype) {
            case MYSQL:
                return "?";

            case NEO4J:
                return "{%d}";
        }
        throw new IllegalStateException("no query format for dbtype=" + dbtype);
    }

    private static String trimQuotes(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    private void setQueryStringParam(java.sql.PreparedStatement stmnt,
            DbQueryInfo queryInfo,
            DbParamTypeEnum type,
            String value,
            Map<Integer, Object> map) throws SQLException {

        if (!queryInfo.getParamPosMap().containsKey(type)) {
            throw new IllegalArgumentException("No entry for type=" + type + " in params=" + Arrays.toString(queryInfo.getParams()));
        }
        int index = queryInfo.getParamPosMap().get(type);
        if (null != map) {
            map.put(index, value);
        }
        stmnt.setString(index, value);

    }

    private void setQueryIntParam(java.sql.PreparedStatement stmnt,
            DbQueryInfo queryInfo,
            DbParamTypeEnum type,
            int value,
            Map<Integer, Object> map) throws SQLException {

        if (!queryInfo.getParamPosMap().containsKey(type)) {
            throw new IllegalArgumentException("No entry for type=" + type + " in params=" + Arrays.toString(queryInfo.getParams()));
        }
        int index = queryInfo.getParamPosMap().get(type);
        if (null != map) {
            map.put(index, value);
        }
        stmnt.setInt(index, value);

    }

    private void setQueryDoubleParam(java.sql.PreparedStatement stmnt,
            DbQueryInfo queryInfo,
            DbParamTypeEnum type,
            double value,
            Map<Integer, Object> map) throws SQLException {

        if (!queryInfo.getParamPosMap().containsKey(type)) {
            throw new IllegalArgumentException("No entry for type=" + type + " in params=" + Arrays.toString(queryInfo.getParams()));
        }
        int index = queryInfo.getParamPosMap().get(type);
        if (null != map) {
            map.put(index, value);
        }
        stmnt.setDouble(index, value);
    }

    private String createExpectedQueryString(
            DbQueryInfo queryInfo,
            Map<Integer, Object> map
    ) throws SQLException {
        String queryFormat = getQueryFormat();
        DbParamTypeEnum paramTypes[] = queryInfo.getParams();
        String qString = queryInfo.getQuery();
        if (null != paramTypes) {
            for (int i = 0; i < paramTypes.length; i++) {
                qString = qString.replace(
                        String.format(queryFormat, i + 1),
                        String.format(this.expectQueryItemFormat,
                                map.get(i + 1).toString())
                );
            }
        }
        return qString;
    }

    private String getQueryResultString(ResultSet rs, DbQueryInfo queryInfo, DbParamTypeEnum type) throws SQLException {
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

    private String getPoseQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    private String getAllPartsInKtQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    private String getAllPartsInPtQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    private String getTraySlotsFromKitSkuQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getTraySlotsFromKitSkuQueryInfo, type);
    }

    private String getPartsTraysQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getPartsTraysQueryInfo, type);
    }

    private String getSlotsQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
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
     */
    public ArrayList<String> getAllPartsInKt(String name) throws SQLException {
        ArrayList<String> partsInKtList = new ArrayList();
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }

        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getAllPartsInKtQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_PARTS_IN_KT);
        setQueryStringParam(getAllPartsInKtStatement, getAllPartsInKtQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getAllPartsInKtQueryInfo, map);
        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }

        try (ResultSet rs = getAllPartsInKtStatement.executeQuery()) {
            //int c = 0;
            while (rs.next()) {
                //c++;
                String nameCheckString = getAllPartsInKtQueryResultString(rs, DbParamTypeEnum.NAME);
                //System.out.println("nameCheckString = " + nameCheckString);
                partsInKtList.add(nameCheckString);
            }
        }
        return partsInKtList;
    }

    public ArrayList<String> getAllPartsInPt(String name) throws SQLException {
        ArrayList<String> partsInPtList = new ArrayList();
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }

        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getAllPartsInPtQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_PARTS_IN_PT);
        setQueryStringParam(getAllPartsInPtStatement, getAllPartsInPtQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getAllPartsInPtQueryInfo, map);

        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }

        try (ResultSet rs = getAllPartsInPtStatement.executeQuery()) {
            //int c = 0;
            while (rs.next()) {
                //c++;
                String nameCheckString = getAllPartsInPtQueryResultString(rs, DbParamTypeEnum.NAME);
                //System.out.println("nameCheckString = " + nameCheckString);
                partsInPtList.add(nameCheckString);
            }
        }
        return partsInPtList;
    }

    public int getPartDesignPartCount(String name) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        int count = 0;
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getCountQueryInfo = queriesMap.get(DbQueryEnum.GET_PARTDESIGN_PART_COUNT);
        setQueryStringParam(getPartDesignPartCountStatement, getCountQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getCountQueryInfo, map);
        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }

        try (ResultSet rs = getPartDesignPartCountStatement.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getPartDesignPartCount for name=" + name + ",\n   simQuery=" + simQuery);
            }
        }
        return count;
    }

    public List<PartsTray> getPartsTrays(String name) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        assert (null != getPartsTraysStatement) :
                ("null == getPartsTraysStatement");

        List<PartsTray> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getPartsTraysQueryInfo = queriesMap.get(DbQueryEnum.GET_PARTSTRAYS);
        setQueryStringParam(getPartsTraysStatement, getPartsTraysQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getPartsTraysQueryInfo, map);

        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }
        
        try (ResultSet rs = getPartsTraysStatement.executeQuery()) {
            while (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        System.out.println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        System.out.println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        System.out.println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        System.out.println("o = " + o);
                    }
                }

                String partsTrayName = getPartsTraysQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    System.out.println("partsTrayName = " + partsTrayName);
                }
                PartsTray partstray = new PartsTray(partsTrayName);
                //-- getting node id
                int id = getQueryResultInt(rs, getPartsTraysQueryInfo, DbParamTypeEnum.NODE_ID);
                partstray.setNodeID(id);
                if (debug) {
                    System.out.println("id = " + id);
                }
                //-- getting design
                String design = getPartsTraysQueryResultString(rs, DbParamTypeEnum.TRAY_DESIGN_NAME);
                partstray.setPartsTrayDesign(design);
                if (debug) {
                    System.out.println("design = " + design);
                }

                String sku = getPartsTraysQueryResultString(rs, DbParamTypeEnum.SKU_NAME);
                partstray.setPartsTraySku(sku);
                if (debug) {
                    System.out.println("sku = " + sku);
                }
                //-- gettig complete
                String complete = trimQuotes(getPartsTraysQueryResultString(rs, DbParamTypeEnum.TRAY_COMPLETE));
                partstray.setPartsTrayComplete(Boolean.valueOf(complete));
                if (debug) {
                    if (partstray.getPartsTrayComplete()) {
                        System.out.println("complete = true");
                    } else {
                        System.out.println("complete = false");
                    }
                }
                //-- get a list of slots for this parts tray
                partstray.setSlotList(getSlots(partsTrayName));
                //displayPartsTray(partstray);
                list.add(partstray);
            }
        }
        return list;
    }

    public List<Slot> getSlots(String name) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        assert (null != getPartsTraysStatement) :
                ("null == getPartsTraysStatement");

        List<Slot> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getSlotsQueryInfo = queriesMap.get(DbQueryEnum.GET_SLOTS);
        setQueryStringParam(getSlotsStatement, getSlotsQueryInfo, DbParamTypeEnum.NAME, name, map);
        String simQuery = createExpectedQueryString(getSlotsQueryInfo, map);

        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }

        try (ResultSet rs = getSlotsStatement.executeQuery()) {
            while (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        System.out.println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        System.out.println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        System.out.println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        System.out.println("o = " + o);
                    }
                }

                //-- slot name
                String SlotName = getSlotsQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    System.out.println("SlotName = " + SlotName);
                }
                Slot slot = new Slot(SlotName);

                //-- ID of the slot
                String slot_id = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.SLOT_ID));
                int slotInt = Integer.parseInt(slot_id);
                slot.setID(slotInt);
                if (debug) {
                    System.out.println("slot_id = " + slot_id);
                }

                //-- is slot occupied?
                String occupied = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.SLOT_OCCUPIED));
                slot.setSlotOccupied(Boolean.valueOf(occupied));
                if (debug) {
                    if (slot.getSlotOccupied()) {
                        System.out.println("occupied = true");
                    } else {
                        System.out.println("occupied = false");
                    }
                }
                //-- part sku that goes in this slot
                String partSku = getSlotsQueryResultString(rs, DbParamTypeEnum.SKU_NAME);
                if (debug) {
                    System.out.println("partSku = " + partSku);
                }
                slot.setPartSKU(partSku);

                //-- X offset
                String xString = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.X));
                double xDouble = Double.parseDouble(xString);
                if (debug) {
                    System.out.println("xString = " + xDouble);
                }
                slot.setX_OFFSET(xDouble);

                //-- Y offset
                String yString = trimQuotes(getSlotsQueryResultString(rs, DbParamTypeEnum.Y));
                double yDouble = Double.parseDouble(yString);
                if (debug) {
                    System.out.println("yString = " + yDouble);
                }
                slot.setY_OFFSET(yDouble);

                list.add(slot);
            }
        }
        return list;
    }

    /*
    public void displayPartsTray(PartsTray partsTray) {
        System.out.println("PartsTray: " + partsTray.getPartsTrayName());
        System.out.println("node id: " + partsTray.getNodeID());
        System.out.println("PartsTray design: " + partsTray.getPartsTrayDesign());
        if (partsTray.getPartsTrayComplete()) {
            System.out.println("PartsTray is complete");
        } else {
            System.out.println("PartsTray is not complete");
        }

        List<Slot> slots = partsTray.getSlotList();
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            System.out.println("Slot name: " + slot.getSlotName());
            System.out.println("Slot external shape model file name:" + slot.getExternalShapeModelFileName());
            System.out.println("Slot external shape model format name:" + slot.getExternalShapeModelFormatName());
            System.out.println("Slot part sku:" + slot.getPartSKU());
            System.out.println("Slot id:" + slot.getID());
            System.out.println("Slot x offset:" + slot.getX_OFFSET());
            System.out.println("Slot y offset:" + slot.getY_OFFSET());
            if (slot.getSlotOccupied()) {
                System.out.println("Slot is occupied");
            } else {
                System.out.println("Slot is not occupied");
            }
        }
    }
     */
    @Override
    public PoseType getPose(String name) throws SQLException {
        return getPose(name, false, 0);
    }

    private AprsJFrame aprsJFrame = null;

    public AprsJFrame getAprsJFrame() {
        return aprsJFrame;
    }

    public void setAprsJFrame(AprsJFrame aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
    }
    
    public PoseType getPose(String name, boolean requireNew, int visionCycleNewDiffThreshold) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        PoseType pose = new PoseType();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getPoseQueryInfo = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE);
        setQueryStringParam(getPoseStatement, getPoseQueryInfo, DbParamTypeEnum.NAME, name, map);

        String simQuery = createExpectedQueryString(getPoseQueryInfo, map);
        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }
        try (ResultSet rs = getPoseStatement.executeQuery()) {
            if (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        System.out.println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        System.out.println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        System.out.println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        System.out.println("o = " + o);
                    }
                }
                String nameCheckString = getPoseQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    System.out.println("nameCheckString = " + nameCheckString);
                }
                assert (nameCheckString.equals(name)) :
                    ("returned name " + nameCheckString + " does not match requested name " + name);
               
                String xString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.X));
                if (debug) {
                    System.out.println("xString = " + xString);
                }
                PointType point = new PointType();
                double x = Double.parseDouble(xString);
                point.setX(x);
                String yString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Y));
                if (debug) {
                    System.out.println("yString = " + yString);
                }
                double y = Double.parseDouble(yString);
                point.setY(y);
                String zString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Z));
                if (debug) {
                    System.out.println("zString = " + zString);
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
                    System.out.println("vxiString = " + vxiString);
                }
                double vxi = Double.parseDouble(vxiString);
                xAxis.setI(vxi);
                String vxjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXJ));
                if (debug) {
                    System.out.println("vxjString = " + vxjString);
                }
                double vxj = Double.parseDouble(vxjString);
                xAxis.setJ(vxj);
                String vxkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXK));
                if (debug) {
                    System.out.println("vxkString = " + vxkString);
                }
                double vxk = Double.parseDouble(vxkString);
                xAxis.setK(vxk);
                pose.setXAxis(xAxis);
                VectorType zAxis = new VectorType();
                String vziString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZI));
                if (debug) {
                    System.out.println("vziString = " + vziString);
                }
                double vzi = Double.parseDouble(vziString);
                zAxis.setI(vzi);
                String vzjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZJ));
                if (debug) {
                    System.out.println("vziString = " + vzjString);
                }
                double vzj = Double.parseDouble(vzjString);
                zAxis.setJ(vzj);
                String vzkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZK));
                if (debug) {
                    System.out.println("vzkString = " + vzkString);
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
                    System.out.println("visionCycleString = " + visionCycleString);
                }
                String maxVisionCycleString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.MAX_VISIONCYCLE));
                int maxVisionCycle = -1;
                if (null != maxVisionCycleString) {
                    maxVisionCycle = Integer.parseInt(maxVisionCycleString);
                }
                if (debug) {
                    System.out.println("maxVisionCycleString = " + maxVisionCycleString);
                }
                 
                if (requireNew && maxVisionCycle > visionCycle + visionCycleNewDiffThreshold) {
//                    printDebugUpdateResultsMap(name);
                    return null;
                }
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getPose for name=" + name + ", simQuery=" + simQuery);
            }
//            if (rs.next()) {
//                String nameCheckString = rs.getString(1);
//                System.out.println("nameCheckString = " + nameCheckString);
//                int count =1;
//                while(rs.next()) {
//                    System.out.println("rs.getString(1) = " + rs.getString(1));
//                    count++;
//                    System.out.println("count = " + count);
//                }
//                throw new IllegalStateException("More than one result for name=" + name);
//            }
        }
        return pose;
    }

    private void printDebugUpdateResultsMap(String name) {
        if(null != aprsJFrame) {
            Map<String,UpdateResults> updateMap = aprsJFrame.getDbUpdatesResultMap();
            UpdateResults ur = updateMap.get(name);
            System.out.println("ur = " + ur);
            System.out.println("updateMap = " + updateMap);
            for(Map.Entry<String,UpdateResults> entry : updateMap.entrySet()) {
                PhysicalItem item = entry.getValue().getLastDetectedItem();
                if(null != item) {
                    System.out.println(entry.getKey()+"\t"+item.getVisioncycle());
                } else {
                    System.out.println(entry.getKey()+"\tnull");
                }
            }
        }
    }

    
     public List<PhysicalItem>  getAllNewParts( int visionCycleNewDiffThreshold) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        PoseType pose = new PoseType();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getAllNewPoseQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_NEW_POSE);
        List<PhysicalItem> ret = new ArrayList<>();
        String simQuery = createExpectedQueryString(getAllNewPoseQueryInfo, map);
        if (debug) {
            System.out.println("simQuery = " + simQuery);
        }
        try (ResultSet rs = getPoseStatement.executeQuery()) {
            if (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    if (debug) {
                        System.out.println("j = " + j);
                    }
                    String cname = meta.getColumnName(j);
                    if (debug) {
                        System.out.println("cname = " + cname);
                    }
                    String type = meta.getColumnTypeName(j);
                    if (debug) {
                        System.out.println("type = " + type);
                    }
                    Object o = rs.getObject(j);
                    if (debug) {
                        System.out.println("o = " + o);
                    }
                }
                String name = getPoseQueryResultString(rs, DbParamTypeEnum.NAME);
                if (debug) {
                    System.out.println("nameCheckString = " + name);
                }
               
                String xString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.X));
                if (debug) {
                    System.out.println("xString = " + xString);
                }
                PointType point = new PointType();
                double x = Double.parseDouble(xString);
                point.setX(x);
                String yString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Y));
                if (debug) {
                    System.out.println("yString = " + yString);
                }
                double y = Double.parseDouble(yString);
                point.setY(y);
                String zString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Z));
                if (debug) {
                    System.out.println("zString = " + zString);
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
                    System.out.println("vxiString = " + vxiString);
                }
                double vxi = Double.parseDouble(vxiString);
                xAxis.setI(vxi);
                String vxjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXJ));
                if (debug) {
                    System.out.println("vxjString = " + vxjString);
                }
                double vxj = Double.parseDouble(vxjString);
                xAxis.setJ(vxj);
                String vxkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXK));
                if (debug) {
                    System.out.println("vxkString = " + vxkString);
                }
                double vxk = Double.parseDouble(vxkString);
                xAxis.setK(vxk);
                pose.setXAxis(xAxis);
                VectorType zAxis = new VectorType();
                String vziString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZI));
                if (debug) {
                    System.out.println("vziString = " + vziString);
                }
                double vzi = Double.parseDouble(vziString);
                zAxis.setI(vzi);
                String vzjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZJ));
                if (debug) {
                    System.out.println("vziString = " + vzjString);
                }
                double vzj = Double.parseDouble(vzjString);
                zAxis.setJ(vzj);
                String vzkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZK));
                if (debug) {
                    System.out.println("vzkString = " + vzkString);
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
                    System.out.println("visionCycleString = " + visionCycleString);
                }
                String maxVisionCycleString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.MAX_VISIONCYCLE));
                int maxVisionCycle = -1;
                if (null != maxVisionCycleString) {
                    maxVisionCycle = Integer.parseInt(maxVisionCycleString);
                }
                if (debug) {
                    System.out.println("maxVisionCycleString = " + maxVisionCycleString);
                }
                if (maxVisionCycle > visionCycle + visionCycleNewDiffThreshold) {
                    return null;
                }
                PhysicalItem item =  PhysicalItem.newPhysicalItemNamePoseVisionCycle(name, pose, visionCycle);
                ret.add(item);
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getAllNewParts, simQuery=" + simQuery);
            }
//            if (rs.next()) {
//                String nameCheckString = rs.getString(1);
//                System.out.println("nameCheckString = " + nameCheckString);
//                int count =1;
//                while(rs.next()) {
//                    System.out.println("rs.getString(1) = " + rs.getString(1));
//                    count++;
//                    System.out.println("count = " + count);
//                }
//                throw new IllegalStateException("More than one result for name=" + name);
//            }
        }
        return ret;
    }
     
    public void setSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        assert (null != setSingleTrayDesignStatement) :
            ("null == getAllTrayDesignsStatement");
        
        List<TraySlotDesign> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo setSingleTraySlotDesignQueryInfo = queriesMap.get(DbQueryEnum.SET_SINGLE_TRAY_SLOT_DESIGN);
        setQueryStringParam(setSingleTrayDesignStatement, setSingleTraySlotDesignQueryInfo, DbParamTypeEnum.PART_DESIGN_NAME, tsd.getPartDesignName(), map);
        setQueryStringParam(setSingleTrayDesignStatement, setSingleTraySlotDesignQueryInfo, DbParamTypeEnum.TRAY_DESIGN_NAME, tsd.getTrayDesignName(), map);
        setQueryIntParam(setSingleTrayDesignStatement, setSingleTraySlotDesignQueryInfo, DbParamTypeEnum.SLOT_DESIGN_ID, tsd.getID(), map);
        setQueryDoubleParam(setSingleTrayDesignStatement, setSingleTraySlotDesignQueryInfo, DbParamTypeEnum.X_SLOT_OFFSET, tsd.getX_OFFSET(), map);
        setQueryDoubleParam(setSingleTrayDesignStatement, setSingleTraySlotDesignQueryInfo, DbParamTypeEnum.Y_SLOT_OFFSET, tsd.getY_OFFSET(), map);
        setSingleTrayDesignStatement.execute();
    }


    @Override
    public void close() throws Exception {
        closed = true;
        if (null != getPoseStatement) {
            getPoseStatement.close();
            getPoseStatement = null;
        }
        if (null != setPoseStatement) {
            setPoseStatement.close();
            setPoseStatement = null;
        }
        if (null != getAllTrayDesignsStatement) {
            getAllTrayDesignsStatement.close();
            getAllTrayDesignsStatement = null;
        }
        if (null != getSingleTrayDesignStatement) {
            getSingleTrayDesignStatement.close();
            getSingleTrayDesignStatement = null;
        }
        if (null != setSingleTrayDesignStatement) {
            setSingleTrayDesignStatement.close();
            setSingleTrayDesignStatement = null;
        }
        if (null != newSingleTrayDesignStatement) {
            newSingleTrayDesignStatement.close();
            newSingleTrayDesignStatement = null;
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
    }

    @Override
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
        setPoseStatement.setString(1, name);

        Map<Integer, Object> map = new TreeMap<>();
        setPoseQueryStringParam(DbParamTypeEnum.NAME, name, map);
        setPoseQueryStringParam(DbParamTypeEnum.SKU_NAME, toSku(name), map);
        PointType point = pose.getPoint();
        setPoseQueryDoubleParam(DbParamTypeEnum.X, point.getX(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.Y, point.getY(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.Z, point.getZ(), map);
        VectorType xAxis = pose.getXAxis();
        setPoseQueryDoubleParam(DbParamTypeEnum.VXI, xAxis.getI(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VXJ, xAxis.getJ(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VXK, xAxis.getK(), map);
        VectorType zAxis = pose.getZAxis();
        setPoseQueryDoubleParam(DbParamTypeEnum.VZI, zAxis.getI(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VZJ, zAxis.getJ(), map);
        setPoseQueryDoubleParam(DbParamTypeEnum.VZK, zAxis.getK(), map);
        String simQuery = createExpectedQueryString(setQueryInfo, map);
        if(debug) {
            System.out.println("setPose("+name+","+pose+"): simQuery = " + simQuery);
        }
        
        int update_count = setPoseStatement.executeUpdate();
        if(debug) {
            System.out.println("update_count = " + update_count);
        }
    }
}
