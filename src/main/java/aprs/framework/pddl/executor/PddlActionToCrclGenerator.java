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
package aprs.framework.pddl.executor;

import aprs.framework.PddlAction;
import aprs.framework.database.DbQueryEnum;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.QuerySet;
import crcl.base.DwellType;
import crcl.base.MessageType;
import crcl.base.MiddleCommandType;
import crcl.base.MoveToType;
import crcl.base.PoseType;
import crcl.base.SetEndEffectorType;
import crcl.utils.CRCLPosemath;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlActionToCrclGenerator implements DbSetupListener, AutoCloseable {

    private java.sql.Connection dbConnection;
    private DbSetup dbSetup;
    private boolean closeDbConnection = true;
    private QuerySet qs;

    public boolean isConnected() {
        try {
            return null != dbConnection && null != qs && !dbConnection.isClosed();
        } catch (SQLException ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean isCloseDbConnection() {
        return closeDbConnection;
    }

    public void setCloseDbConnection(boolean closeDbConnection) {
        this.closeDbConnection = closeDbConnection;
    }

    public Connection getDbConnection() {
        return dbConnection;
    }

//    private Map<DbQueryEnum, String> queriesMap;
//
//    {
//        queriesMap = new EnumMap<DbQueryEnum, String>(DbQueryEnum.class);
//        queriesMap.put(DbQueryEnum.GET_SINGLE_POSE, "MATCH pointpath=(source { name:{1} } ) -[:hasPhysicalLocation_RefObject]-> (n) -[r2:hasPoseLocation_Pose] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
//                + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
//                + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
//                + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk");
//        queriesMap.put(DbQueryEnum.SET_SINGLE_POSE,
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
//    }

    public void setDbConnection(Connection dbConnection) {
        try {
            if (null != this.dbConnection && dbConnection != this.dbConnection && closeDbConnection) {
                try {
                    this.dbConnection.close();

                } catch (SQLException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            this.dbConnection = dbConnection;
            if (null != dbConnection && null != dbSetup) {
                qs = new QuerySet(dbSetup.getDbType(), dbConnection, dbSetup.getQueriesMap());
            } else if (qs != null) {
                qs.close();
                qs = null;
            }
            System.out.println("qs = " + qs);
        } catch (SQLException ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public DbSetup getDbSetup() {
        return dbSetup;
    }

    public void setDbSetup(DbSetup dbSetup) {
        this.dbSetup = dbSetup;
        if (null != this.dbSetup && this.dbSetup.isConnected()) {
            if (dbConnection == null) {
                try {
                    setDbConnection(DbSetupBuilder.connect(dbSetup));
                    System.out.println("PddlActionToCrclGenerator connected to database of on host " + dbSetup.getHost() + " with port " + dbSetup.getPort());
                } catch (SQLException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            setDbConnection(null);
        }
    }

    BigDecimal approachZOffset = BigDecimal.valueOf(30.0);

    private int actionToCrclIndexes[] = null;

    public int[] getActionToCrclIndexes() {
        return actionToCrclIndexes;
    }

    private String actionToCrclLabels[] = null;

    public String[] getActionToCrclLabels() {
        return actionToCrclLabels;
    }

    private Map<String, String> options = null;

        private boolean lookForDone = false;

    /**
     * Get the value of lookForDone
     *
     * @return the value of lookForDone
     */
    public boolean isLookForDone() {
        return lookForDone;
    }

    /**
     * Set the value of lookForDone
     *
     * @param lookForDone new value of lookForDone
     */
    public void setLookForDone(boolean lookForDone) {
        this.lookForDone = lookForDone;
    }

        private int lastIndex;

    /**
     * Get the value of lastIndex
     *
     * @return the value of lastIndex
     */
    public int getLastIndex() {
        return lastIndex;
    }

    /**
     * Set the value of lastIndex
     *
     * @param lastIndex new value of lastIndex
     */
    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

    public List<MiddleCommandType> generate(List<PddlAction> actions, int startingIndex,  Map<String, String> options) {
        this.options = options;
        List<MiddleCommandType> out = new ArrayList<>();
        if(null == actionToCrclIndexes || actionToCrclIndexes.length != actions.size()) {
            actionToCrclIndexes = new int[actions.size()];
        }
        if(null == actionToCrclLabels || actionToCrclLabels.length != actions.size()) {
            actionToCrclLabels = new String[actions.size()];
        }
        for (lastIndex = startingIndex; lastIndex < actions.size(); lastIndex++) {
            actionToCrclIndexes[lastIndex] = out.size();
            PddlAction action = actions.get(lastIndex);
            System.out.println("action = " + action);
            try {
                switch (action.getType()) {
                    case "take-part":
                        takePart(action, out);
                        break;
                    case "look-for-part":
                        if (!isLookForDone()) {
                            lookForPart(action, out);
                            return out;
                        }
                        break;

                    case "place-part":
                        placePart(action, out);
                        break;
                }
            } catch (Exception ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                MessageType message = new MessageType();
                message.setCommandID(BigInteger.valueOf(out.size() + 2));
                message.setMessage(ex.toString());
                out.add(message);
                actionToCrclLabels[lastIndex] = "Error";
            }
        }
        return out;
    }

    private void takePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        MessageType msg = new MessageType();
        msg.setMessage("take-part "+action.getArgs()[1]);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);
        
        PoseType pose = qs.getPose(action.getArgs()[1]);
        pose.setZAxis(vector(0, 0, -1.0));
        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
        openGripperCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        openGripperCmd.setSetting(BigDecimal.ONE);
        out.add(openGripperCmd);

        PoseType poseAbove = CRCLPosemath.copy(pose);
        poseAbove.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));
        MoveToType moveAboveCmd = new MoveToType();
        moveAboveCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveAboveCmd.setEndPosition(poseAbove);
        out.add(moveAboveCmd);

        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        dwellCmd.setDwellTime(BigDecimal.valueOf(3.0));
        out.add(dwellCmd);
        
        MoveToType moveToCmd = new MoveToType();
        moveToCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveToCmd.setEndPosition(pose);
        out.add(moveToCmd);

        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        closeGrippeerCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        closeGrippeerCmd.setSetting(BigDecimal.ZERO);
        out.add(closeGrippeerCmd);

        MoveToType moveAboveCmd2 = new MoveToType();
        moveAboveCmd2.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveAboveCmd2.setEndPosition(poseAbove);
        out.add(moveAboveCmd2);
    }

    private void lookForPart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        PoseType pose = new PoseType();
        String lookforXYZSring = options.get("lookForXYZ");
        String lookForXYZFields[] = lookforXYZSring.split(",");

        pose.setPoint(point(Double.valueOf(lookForXYZFields[0]), Double.valueOf(lookForXYZFields[1]), Double.valueOf(lookForXYZFields[2])));
        pose.setXAxis(vector(1, 0, 0));
        pose.setZAxis(vector(0, 0, -1));
        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
        openGripperCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        openGripperCmd.setSetting(BigDecimal.ONE);
        MoveToType moveToCmd = new MoveToType();
        moveToCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveToCmd.setEndPosition(pose);
        out.add(moveToCmd);

        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        dwellCmd.setDwellTime(BigDecimal.valueOf(3.0));
        out.add(dwellCmd);
        
        MessageType msg = new MessageType();
        msg.setMessage("look-for "+action.getArgs()[1] + " from "+lookforXYZSring);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);
        
    }

    private void placePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        PoseType pose = qs.getPose(action.getArgs()[6]);
        pose.setZAxis(vector(0, 0, -1.0));

        PoseType poseAbove = CRCLPosemath.copy(pose);
        poseAbove.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));
        MoveToType moveAboveCmd = new MoveToType();
        moveAboveCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveAboveCmd.setEndPosition(poseAbove);
        out.add(moveAboveCmd);

        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        dwellCmd.setDwellTime(BigDecimal.valueOf(3.0));
        out.add(dwellCmd);
        
        MoveToType moveToCmd = new MoveToType();
        moveToCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveToCmd.setEndPosition(pose);
        out.add(moveToCmd);

        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
        openGripperCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        openGripperCmd.setSetting(BigDecimal.ONE);
        out.add(openGripperCmd);

        MoveToType moveAboveCmd2 = new MoveToType();
        moveAboveCmd2.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveAboveCmd2.setEndPosition(poseAbove);
        out.add(moveAboveCmd2);
    }

    @Override
    public void accept(DbSetup setup) {
        this.setDbSetup(setup);

    }

    @Override
    public void close() throws Exception {
        if (closeDbConnection && null != dbConnection) {
            dbConnection.close();
            dbConnection = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.close();
        } catch (Throwable t) {
            // Deliberately ignored.
        }
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

}
