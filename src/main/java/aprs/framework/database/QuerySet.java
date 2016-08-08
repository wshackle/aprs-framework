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

import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.VectorType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class QuerySet implements QuerySetInterface {

    public QuerySet(DbType dbtype, java.sql.Connection con, Map<DbQueryEnum, String> queriesMap) throws SQLException {
        this.dbtype = dbtype;
        getPoseQueryString = queriesMap.get(DbQueryEnum.GET_SINGLE_POSE);
        if (null == getPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain getPose");
        }
        getPoseStatement = con.prepareStatement(getPoseQueryString);
        setPoseQueryString = queriesMap.get(DbQueryEnum.SET_SINGLE_POSE);
        if (null == setPoseQueryString) {
            throw new IllegalArgumentException("queriesMap does not contain setPose");
        }
        setPoseStatement = con.prepareStatement(setPoseQueryString);
    }

    private final DbType dbtype;
    java.sql.PreparedStatement getPoseStatement;
    private final String getPoseQueryString;
    java.sql.PreparedStatement setPoseStatement;
    private final String setPoseQueryString;
    private boolean closed = false;

    private static String trimQuotes(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    @Override
    public PoseType getPose(String name) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        PoseType pose = new PoseType();

        getPoseStatement.setString(1, name);
        String simQuery = getPoseQueryString
                .replace("{1}", name);
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
                String nameCheckString = rs.getString(1);
                System.out.println("nameCheckString = " + nameCheckString);
                if (!nameCheckString.equals(name)) {
                    throw new IllegalStateException("returned name " + nameCheckString + " does not match requested name " + name);
                }
                String xString = trimQuotes(rs.getString(2));
                System.out.println("xString = " + xString);
                PointType point = new PointType();
                point.setX(new BigDecimal(xString));
                String yString = trimQuotes(rs.getString(3));
                System.out.println("yString = " + yString);
                point.setY(new BigDecimal(yString));
                String zString = trimQuotes(rs.getString(4));
                System.out.println("zString = " + zString);
                if(null != zString) {
                    point.setZ(new BigDecimal(zString));
                } else {
                    point.setZ(BigDecimal.ZERO);
                }
                pose.setPoint(point);
                VectorType xAxis = new VectorType();
                String vxiString = trimQuotes(rs.getString(5));
                System.out.println("vxiString = " + vxiString);
                xAxis.setI(new BigDecimal(vxiString));
                String vxjString = trimQuotes(rs.getString(6));
                System.out.println("vxiString = " + vxjString);
                xAxis.setJ(new BigDecimal(vxjString));
                String vxkString = trimQuotes(rs.getString(7));
                System.out.println("vxkString = " + vxkString);
                xAxis.setK(new BigDecimal(vxkString));
                pose.setXAxis(xAxis);
                VectorType zAxis = new VectorType();
                String vziString = trimQuotes(rs.getString(8));
                System.out.println("vziString = " + vziString);
                zAxis.setI(new BigDecimal(vziString));
                String vzjString = trimQuotes(rs.getString(9));
                System.out.println("vziString = " + vzjString);
                zAxis.setJ(new BigDecimal(vzjString));
                String vzkString = trimQuotes(rs.getString(10));
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

    @Override
    public void close() throws Exception {
        closed = true;
        if (null != getPoseStatement) {
            getPoseStatement.close();
            getPoseStatement = null;
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

    @Override
    public void setPose(String name, PoseType pose) throws SQLException {
        if (closed) {
            throw new IllegalStateException("QuerySet already closed.");
        }
        if (null == pose || null == pose.getPoint() || null == pose.getXAxis() || null == pose.getZAxis()) {
            throw new IllegalArgumentException("pose must not be null and must not have null point,xaxis or zaxis");
        }
        setPoseStatement.setString(1, name);
        setPoseStatement.setDouble(2, pose.getPoint().getX().doubleValue());
        setPoseStatement.setDouble(3, pose.getPoint().getY().doubleValue());
        setPoseStatement.setDouble(4, pose.getPoint().getZ().doubleValue());
        setPoseStatement.setDouble(5, pose.getXAxis().getI().doubleValue());
        setPoseStatement.setDouble(6, pose.getXAxis().getJ().doubleValue());
        setPoseStatement.setDouble(7, pose.getXAxis().getK().doubleValue());
        setPoseStatement.setDouble(8, pose.getZAxis().getI().doubleValue());
        setPoseStatement.setDouble(9, pose.getZAxis().getJ().doubleValue());
        setPoseStatement.setDouble(10, pose.getZAxis().getK().doubleValue());
        String simQuery = setPoseQueryString
                .replace("{1}", name)
                .replace("{2}", pose.getPoint().getX().toString())
                .replace("{3}", pose.getPoint().getY().toString())
                .replace("{4}", pose.getPoint().getZ().toString())
                .replace("{5}", pose.getXAxis().getI().toString())
                .replace("{6}", pose.getXAxis().getJ().toString())
                .replace("{7}", pose.getXAxis().getK().toString())
                .replace("{8}", pose.getZAxis().getI().toString())
                .replace("{9}", pose.getZAxis().getJ().toString())
                .replace("{10}", pose.getZAxis().getK().toString());
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
