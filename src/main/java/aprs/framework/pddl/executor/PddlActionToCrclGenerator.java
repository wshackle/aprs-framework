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
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.QuerySet;
import crcl.base.MessageType;
import crcl.base.MiddleCommandType;
import crcl.base.MoveToType;
import crcl.base.PoseType;
import crcl.base.SetEndEffectorType;
import crcl.utils.CRCLPosemath;
import static crcl.utils.CRCLPosemath.vector;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private Map<String, String> queriesMap;

    {
        queriesMap = new HashMap<String, String>();
        queriesMap.put("getPose", "MATCH pointpath=(source { name:{1} } ) -[:hasPhysicalLocation_RefObject]-> (n) -[r2:hasPoseLocation_Pose] ->(pose) -  [r1:hasPose_Point] -> (p:Point),\n"
                + "xaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\n"
                + "zaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\n"
                + "return source.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk");
        queriesMap.put("setPose",
                "MERGE (thing:SolidObject { name:{1} } )\n"
                + "merge (thing) - [:hasPhysicalLocation_RefObject] -> (pl:PhysicalLocation)\n"
                + "merge (pl) - [:hasPoseLocation_Pose] -> (pose:PoseLocation)\n"
                + "merge (pose) - [:hasPose_Point] -> (pt:Point)\n"
                + "merge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\n"
                + "merge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\n"
                + "set pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4}\n"
                + "set xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\n"
                + "set zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}"
        );
    }

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
                qs = new QuerySet(dbSetup.getDbType(), dbConnection, queriesMap);
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
    
    public List<MiddleCommandType> generate(List<PddlAction> actions) {
        List<MiddleCommandType> out = new ArrayList<>();
        actionToCrclIndexes = new int[actions.size()];
        actionToCrclLabels = new String[actions.size()];
        for (int i = 0; i < actions.size(); i++) {

            actionToCrclIndexes[i] = out.size();
            PddlAction action = actions.get(i);
            System.out.println("action = " + action);
            try {
                switch (action.getType()) {
                    case "take-part":
                        takePart(action, out);
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
                actionToCrclLabels[i] = "Error";
            }
        }
        return out;
    }

    private void takePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        PoseType pose = qs.getPose(action.getArgs()[1]);
        pose.setZAxis(vector(0,0,-1.0));
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

    
    private void placePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        PoseType pose = qs.getPose(action.getArgs()[6]);
        pose.setZAxis(vector(0,0,-1.0));
        
        PoseType poseAbove = CRCLPosemath.copy(pose);
        poseAbove.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));
        MoveToType moveAboveCmd = new MoveToType();
        moveAboveCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        moveAboveCmd.setEndPosition(poseAbove);
        out.add(moveAboveCmd);
        
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
