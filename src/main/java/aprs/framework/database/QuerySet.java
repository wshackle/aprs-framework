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

import aprs.framework.pddl.executor.TraySlotDesign;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.VectorType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class QuerySet implements QuerySetInterface {

    public QuerySet(
            DbType dbtype,
            java.sql.Connection con,
            Map<DbQueryEnum, DbQueryInfo> queriesMap) throws SQLException {
        this.dbtype = dbtype;
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
        String getPoseQueryString = getQueryInfo.getQuery();
        if (null == getPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getPose");
        }
        getPoseStatement = con.prepareStatement(getPoseQueryString);
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
        this.queriesMap = queriesMap;

    }

    private final DbType dbtype;
    private final Map<DbQueryEnum, DbQueryInfo> queriesMap;
    private java.sql.PreparedStatement getPoseStatement;
    private java.sql.PreparedStatement setPoseStatement;
    private java.sql.PreparedStatement getAllTrayDesignsStatement;
    private java.sql.PreparedStatement getSingleTrayDesignStatement;
    private java.sql.PreparedStatement setSingleTrayDesignStatement;
    private java.sql.PreparedStatement newSingleTrayDesignStatement;
    
    
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
                qString = qString.replace(String.format(queryFormat, i), map.get(i + 1).toString());
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
            int index = Integer.valueOf(qname);
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
            int index = Integer.valueOf(qname);
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
            int index = Integer.valueOf(qname);
            return rs.getDouble(index);
        }
        return rs.getDouble(qname);
    }

    private String getPoseQueryResultString(ResultSet rs, DbParamTypeEnum type) throws SQLException {
        return getQueryResultString(rs, getQueryInfo, type);
    }

    @Override
    public PoseType getPose(String name) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        PoseType pose = new PoseType();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getPoseQueryInfo = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE);
        setQueryStringParam(getPoseStatement, getPoseQueryInfo, DbParamTypeEnum.NAME, name, map);
//        getPoseStatement.setString(1, name);
        String simQuery = createExpectedQueryString(getPoseQueryInfo, map);
        System.out.println("simQuery = " + simQuery);
        try (ResultSet rs = getPoseStatement.executeQuery()) {
            if (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    System.out.println("j = " + j);
                    String cname = meta.getColumnName(j);
                    System.out.println("cname = " + cname);
                    String type = meta.getColumnTypeName(j);
                    System.out.println("type = " + type);
                    Object o = rs.getObject(j);
                    System.out.println("o = " + o);
                }
                String nameCheckString = getPoseQueryResultString(rs, DbParamTypeEnum.NAME);
                System.out.println("nameCheckString = " + nameCheckString);
                if (!nameCheckString.equals(name)) {
                    throw new IllegalStateException("returned name " + nameCheckString + " does not match requested name " + name);
                }
                String xString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.X));
                System.out.println("xString = " + xString);
                PointType point = new PointType();
                point.setX(new BigDecimal(xString));
                String yString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Y));
                System.out.println("yString = " + yString);
                point.setY(new BigDecimal(yString));
                String zString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.Z));
                System.out.println("zString = " + zString);
                if (null != zString) {
                    point.setZ(new BigDecimal(zString));
                } else {
                    point.setZ(BigDecimal.ZERO);
                }
                pose.setPoint(point);
                VectorType xAxis = new VectorType();
                String vxiString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXI));
                System.out.println("vxiString = " + vxiString);
                xAxis.setI(new BigDecimal(vxiString));
                String vxjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXJ));
                System.out.println("vxiString = " + vxjString);
                xAxis.setJ(new BigDecimal(vxjString));
                String vxkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VXK));
                System.out.println("vxkString = " + vxkString);
                xAxis.setK(new BigDecimal(vxkString));
                pose.setXAxis(xAxis);
                VectorType zAxis = new VectorType();
                String vziString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZI));
                System.out.println("vziString = " + vziString);
                zAxis.setI(new BigDecimal(vziString));
                String vzjString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZJ));
                System.out.println("vziString = " + vzjString);
                zAxis.setJ(new BigDecimal(vzjString));
                String vzkString = trimQuotes(getPoseQueryResultString(rs, DbParamTypeEnum.VZK));
                System.out.println("vzkString = " + vzkString);
                zAxis.setK(new BigDecimal(vzkString));
                pose.setZAxis(zAxis);
            } else {
                throw new IllegalStateException("Database returned empty ResultSet for query to getPose for name=" + name);
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

    public void setSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        if(null == setSingleTrayDesignStatement) {
            throw new IllegalStateException("null == getAllTrayDesignsStatement");
        }
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
    
    public void newSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        if(null == newSingleTrayDesignStatement) {
            throw new IllegalStateException("null == getAllTrayDesignsStatement");
        }
        List<TraySlotDesign> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo newSingleTraySlotDesignQueryInfo = queriesMap.get(DbQueryEnum.NEW_SINGLE_TRAY_SLOT_DESIGN);
        setQueryStringParam(newSingleTrayDesignStatement, newSingleTraySlotDesignQueryInfo, DbParamTypeEnum.PART_DESIGN_NAME, tsd.getPartDesignName(), map);
        setQueryStringParam(newSingleTrayDesignStatement, newSingleTraySlotDesignQueryInfo, DbParamTypeEnum.TRAY_DESIGN_NAME, tsd.getTrayDesignName(), map);
//        setQueryIntParam(newSingleTrayDesignStatement, setSingleTraySlotDesignQueryInfo, DbParamTypeEnum.SLOT_DESIGN_ID, tsd.getID(), map);
        setQueryDoubleParam(newSingleTrayDesignStatement, newSingleTraySlotDesignQueryInfo, DbParamTypeEnum.X_SLOT_OFFSET, tsd.getX_OFFSET(), map);
        setQueryDoubleParam(newSingleTrayDesignStatement, newSingleTraySlotDesignQueryInfo, DbParamTypeEnum.Y_SLOT_OFFSET, tsd.getY_OFFSET(), map);
        String newQuery = createExpectedQueryString(newSingleTraySlotDesignQueryInfo, map);
        System.out.println("simQuery = " + newQuery);
        newSingleTrayDesignStatement.execute();
    }
    
    public List<TraySlotDesign> getSingleTraySlotDesign(String partDesignName, String trayDesignName) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        if(null == getSingleTrayDesignStatement) {
            throw new IllegalStateException("null == getAllTrayDesignsStatement");
        }
        List<TraySlotDesign> list = new ArrayList<>();
        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getSingleTraySlotDesignQueryInfo = queriesMap.get(DbQueryEnum.GET_SINGLE_TRAY_SLOT_DESIGN);
        setQueryStringParam(getSingleTrayDesignStatement, getSingleTraySlotDesignQueryInfo, DbParamTypeEnum.PART_DESIGN_NAME, partDesignName, map);
        setQueryStringParam(getSingleTrayDesignStatement, getSingleTraySlotDesignQueryInfo, DbParamTypeEnum.TRAY_DESIGN_NAME, trayDesignName, map);
//        getPoseStatement.setString(1, name);
//        String simQuery = createExpectedQueryString(getAllTraySlogDesignsQueryInfo, map);
//        System.out.println("simQuery = " + simQuery);
        try (ResultSet rs = getSingleTrayDesignStatement.executeQuery()) {
            while (rs.next()) {
//                ResultSetMetaData meta = rs.getMetaData();
//                for (int j = 1; j <= meta.getColumnCount(); j++) {
//                    System.out.println("j = " + j);
//                    String cname = meta.getColumnName(j);
//                    System.out.println("cname = " + cname);
//                    String type = meta.getColumnTypeName(j);
//                    System.out.println("type = " + type);
//                    Object o = rs.getObject(j);
//                    System.out.println("o = " + o);
//                }
                int id = getQueryResultInt(rs, getSingleTraySlotDesignQueryInfo, DbParamTypeEnum.SLOT_DESIGN_ID);
                TraySlotDesign traySlotDesign = new TraySlotDesign(id);
                traySlotDesign.setPartDesignName(partDesignName);
                traySlotDesign.setTrayDesignName(trayDesignName);
                double x_offset = getQueryResultDouble(rs, getSingleTraySlotDesignQueryInfo, DbParamTypeEnum.X_SLOT_OFFSET);
                traySlotDesign.setX_OFFSET(x_offset);
                double y_offset = getQueryResultDouble(rs, getSingleTraySlotDesignQueryInfo, DbParamTypeEnum.Y_SLOT_OFFSET);
                traySlotDesign.setY_OFFSET(y_offset);
                list.add(traySlotDesign);
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
        return list;
    }

    
    public List<TraySlotDesign> getAllTraySlotDesigns() throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        if(null == getAllTrayDesignsStatement) {
            throw new IllegalStateException("null == getAllTrayDesignsStatement");
        }
        List<TraySlotDesign> list = new ArrayList<>();
//        Map<Integer, Object> map = new TreeMap<>();
        DbQueryInfo getAllTraySlotDesignsQueryInfo = queriesMap.get(DbQueryEnum.GET_ALL_TRAY_SLOT_DESIGNS);
//        setQueryStringParam(getSingleTrayDesignStatement, getSIngleTraySlogDesignQueryInfo, DbParamTypeEnum.PART_DESIGN_NAME, partDesignName, map);
//        setQueryStringParam(getSingleTrayDesignStatement, getSIngleTraySlogDesignQueryInfo, DbParamTypeEnum.TRAY_DESIGN_NAME, trayDesignName, map);
//        getPoseStatement.setString(1, name);
//        String simQuery = createExpectedQueryString(getAllTraySlogDesignsQueryInfo, map);
//        System.out.println("simQuery = " + simQuery);
        try (ResultSet rs = getAllTrayDesignsStatement.executeQuery()) {
            while (rs.next()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int j = 1; j <= meta.getColumnCount(); j++) {
                    System.out.println("j = " + j);
                    String cname = meta.getColumnName(j);
                    System.out.println("cname = " + cname);
                    String type = meta.getColumnTypeName(j);
                    System.out.println("type = " + type);
                    Object o = rs.getObject(j);
                    System.out.println("o = " + o);
                }
                int id = getQueryResultInt(rs, getAllTraySlotDesignsQueryInfo, DbParamTypeEnum.SLOT_DESIGN_ID);
                TraySlotDesign traySlotDesign = new TraySlotDesign(id);
                String partDesignName = getQueryResultString(rs, getAllTraySlotDesignsQueryInfo, DbParamTypeEnum.PART_DESIGN_NAME);
                traySlotDesign.setPartDesignName(partDesignName);
                String trayDesignName = getQueryResultString(rs, getAllTraySlotDesignsQueryInfo, DbParamTypeEnum.TRAY_DESIGN_NAME);
                traySlotDesign.setTrayDesignName(trayDesignName);
                double x_offset = getQueryResultDouble(rs, getAllTraySlotDesignsQueryInfo, DbParamTypeEnum.X_SLOT_OFFSET);
                traySlotDesign.setX_OFFSET(x_offset);
                double y_offset = getQueryResultDouble(rs, getAllTraySlotDesignsQueryInfo, DbParamTypeEnum.Y_SLOT_OFFSET);
                traySlotDesign.setY_OFFSET(y_offset);
                list.add(traySlotDesign);
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
        return list;
    }

    @Override
    public void close() throws Exception {
        closed = true;
        if (null != getPoseStatement) {
            getPoseStatement.close();
            getPoseStatement = null;
        }
        if(null != setPoseStatement) {
            setPoseStatement.close();
            setPoseStatement = null;
        }
        if(null != getAllTrayDesignsStatement) {
            getAllTrayDesignsStatement.close();
            getAllTrayDesignsStatement = null;
        }
        if(null != getSingleTrayDesignStatement) {
            getSingleTrayDesignStatement.close();
            getSingleTrayDesignStatement = null;
        }
        if(null != setSingleTrayDesignStatement) {
            setSingleTrayDesignStatement.close();
            setSingleTrayDesignStatement = null;
        }
        if(null != newSingleTrayDesignStatement) {
            newSingleTrayDesignStatement.close();
            newSingleTrayDesignStatement = null;
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

    private void setPoseQueryStringParam(DbParamTypeEnum type, String value, Map<Integer, Object> map) throws SQLException {
        setQueryStringParam(setPoseStatement, setQueryInfo, type, value, map);
    }

//    private void setPoseQueryDoubleParam(DbParamTypeEnum type, double value, Map<Integer, Object> map) throws SQLException {
//        setQueryDoubleParam(setPoseStatement, setQueryInfo, type, value, map);
//    }
    private void setPoseQueryDoubleParam(DbParamTypeEnum type, BigDecimal value, Map<Integer, Object> map) throws SQLException {
        setQueryDoubleParam(setPoseStatement, setQueryInfo, type, value.doubleValue(), map);
    }

    //TODO-
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
//        String simQuery = setPoseQueryString
//                .replace("{1}", name)
//                .replace("{2}", pose.getPoint().getX().toString())
//                .replace("{3}", pose.getPoint().getY().toString())
//                .replace("{4}", pose.getPoint().getZ().toString())
//                .replace("{5}", pose.getXAxis().getI().toString())
//                .replace("{6}", pose.getXAxis().getJ().toString())
//                .replace("{7}", pose.getXAxis().getK().toString())
//                .replace("{8}", pose.getZAxis().getI().toString())
//                .replace("{9}", pose.getZAxis().getJ().toString())
//                .replace("{10}", pose.getZAxis().getK().toString());
        String simQuery = createExpectedQueryString(setQueryInfo, map);
        System.out.println("simQuery = " + simQuery);
        int update_count = setPoseStatement.executeUpdate();
        System.out.println("update_count = " + update_count);
    }

//    public static void main(String[] args) throws SQLException {
//        DbSetup setup = new DbSetupBuilder()
//                .type(DbType.NEO4J)
//                .host("localhost")
//                .port(7480)
//                .user("neo4j")
//                .passwd("password".toCharArray())
//                .build();
//        Connection con = DbSetupBuilder.connect(setup);
//        Map<String, String> queriesMap = new HashMap<String, String>();
//        queriesMap.put("getPose", "MATCH pointpath=(source { name:{1} } ) -[:hasPhysicalLocation_RefObject]-> (n) -[r2:hasPoseLocation_Pose] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
//                + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
//                + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
//                + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk");
//        queriesMap.put("setPose",
//                "MERGE (thing:SolidObject { name:{1} } )\n"
//                + "merge (thing) - [:hasPhysicalLocation_RefObject] -> (pl:PhysicalLocation)\n"
//                + "merge (pl) - [:hasPoseLocation_Pose] -> (pose:PoseLocation)\n"
//                + "merge (pose) - [:hasPose_Point] -> (pt:Point)\n"
//                + "merge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\n"
//                + "merge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\n"
//                + "set pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4}\n"
//                + "set xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\n"
//                + "set zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}"
//        );
//        PoseType poseIn = pose(point(1.0, 2.0, 3.0), vector(1.0, 0.0, 0.0), vector(0.0, 0.0, 1.0));
//        QuerySet qs = new QuerySet(setup.getDbType(), con, queriesMap);
//
//        qs.setPose("robot_1", poseIn);
//
//        PoseType poseOut = qs.getPose("robot_1");
//        System.out.println("poseOut = " + CRCLPosemath.toString(poseOut));
//    }
}
