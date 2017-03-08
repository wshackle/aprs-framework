/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
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

import aprs.framework.AprsJFrame;
import aprs.framework.spvision.DatabasePoseUpdater;
import aprs.framework.PddlAction;
import aprs.framework.Utils;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.QuerySet;
import aprs.framework.kitinspection.Inspection;
import aprs.framework.spvision.VisionSocketClient;
import aprs.framework.spvision.VisionToDBJPanel;
import aprs.framework.spvision.VisionToDbMainJFrame;
import crcl.base.ActuateJointType;
import crcl.base.ActuateJointsType;
import crcl.base.AngleUnitEnumType;
import crcl.base.DwellType;
import crcl.base.JointSpeedAccelType;
import crcl.base.LengthUnitEnumType;
import crcl.base.MessageType;
import crcl.base.MiddleCommandType;
import crcl.base.MoveToType;
import crcl.base.PoseType;
import crcl.base.RotSpeedAbsoluteType;
import crcl.base.SetAngleUnitsType;
import crcl.base.SetEndEffectorType;
import crcl.base.SetLengthUnitsType;
import crcl.base.SetRotSpeedType;
import crcl.base.SetTransSpeedType;
import crcl.base.TransSpeedAbsoluteType;
import crcl.base.VectorType;
import crcl.utils.CrclCommandWrapper;
import crcl.utils.CRCLPosemath;
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
import java.util.Arrays;
import rcs.posemath.PmCartesian;
import rcs.posemath.PmRpy;
import crcl.utils.CrclCommandWrapper.CRCLCommandWrapperConsumer;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.vector;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;


/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlActionToCrclGenerator implements DbSetupListener, AutoCloseable {

    public static class ReturnPartInfo {

        String partName;
        PoseType pickupPose;

    }

    public String getRunName() {
        if (null != aprsJFrame) {
            return aprsJFrame.getRunName();
        } else {
            return "";
        }
    }

    private java.sql.Connection dbConnection;
    private DbSetup dbSetup;
    private boolean closeDbConnection = true;
    private QuerySet qs;
    private List<String> TakenPartList = new ArrayList();
    private Set<Slot> EmptySlotSet;
    private Inspection jframe;
    private JTextArea InspectionResultJTextArea;

    private boolean takeSnapshots = false;
    private int crclNumber = 0;
    private final ConcurrentMap<String, PoseType> poseCache = new ConcurrentHashMap<>();

    /**
     * Get the value of takeSnapshots
     *
     * @return the value of takeSnapshots
     */
    public boolean isTakeSnapshots() {
        return takeSnapshots;
    }

    /**
     * Set the value of takeSnapshots
     *
     * @param takeSnapshots new value of takeSnapshots
     */
    public void setTakeSnapshots(boolean takeSnapshots) {
        this.takeSnapshots = takeSnapshots;
    }

    private List<PositionMap> positionMaps = null;

    public List<PositionMap> getPositionMaps() {
        return positionMaps;
    }

    public void setPositionMaps(List<PositionMap> errorMap) {
        this.positionMaps = errorMap;
    }

    public List<TraySlotDesign> getAllTraySlotDesigns() throws SQLException {
        return qs.getAllTraySlotDesigns();
    }

    public List<TraySlotDesign> getSingleTraySlotDesign(String partDesignName, String trayDesignName) throws SQLException {
        return qs.getSingleTraySlotDesign(partDesignName, trayDesignName);
    }

    public void setSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
        qs.setSingleTraySlotDesign(tsd);
    }

    public void newSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
        qs.newSingleTraySlotDesign(tsd);
    }

    public synchronized boolean isConnected() {
        try {
            return null != dbConnection
                    && null != qs
                    && !dbConnection.isClosed()
                    && qs.getDbConnection() == dbConnection
                    && qs.isConnected();
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
        this.qs.setDebug(debug);
    }

    public synchronized void setDbConnection(Connection dbConnection) {
        try {
            if (null != this.dbConnection && dbConnection != this.dbConnection && closeDbConnection) {
                try {
                    this.dbConnection.close();
                    if (null != qs) {
                        qs.close();
                    }
                    qs = null;
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
                    DbSetupBuilder.connect(dbSetup).thenAccept(c -> {
                        Utils.runOnDispatchThread(() -> {
                            setDbConnection(c);
                        });
                    });
                    System.out.println("PddlActionToCrclGenerator connected to database of type " + dbSetup.getDbType() + " on host " + dbSetup.getHost() + " with port " + dbSetup.getPort());
                } catch (SQLException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            setDbConnection(null);
        }
    }

    BigDecimal approachZOffset = BigDecimal.valueOf(50.0);
    BigDecimal placeZOffset = BigDecimal.valueOf(5.0);
    BigDecimal takeZOffset = BigDecimal.valueOf(0.0);

    private String actionToCrclTakenPartsNames[] = null;

    public String[] getActionToCrclTakenPartsNames() {
        return actionToCrclTakenPartsNames;
    }

    private int actionToCrclIndexes[] = null;

    public int[] getActionToCrclIndexes() {
        return actionToCrclIndexes;
    }

    private String actionToCrclLabels[] = null;

    public String[] getActionToCrclLabels() {
        return actionToCrclLabels;
    }

    private Map<String, String> options = null;

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

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public List<MiddleCommandType> generate(List<PddlAction> actions, int startingIndex, Map<String, String> options) throws IllegalStateException, SQLException {
        this.options = options;
        crclNumber++;
        List<MiddleCommandType> cmds = new ArrayList<>();
        if (null == actionToCrclIndexes || actionToCrclIndexes.length != actions.size()) {
            actionToCrclIndexes = new int[actions.size()];
        }
        for (int i = startingIndex; i < actionToCrclIndexes.length; i++) {
            actionToCrclIndexes[i] = -1;
        }
        if (null == actionToCrclLabels || actionToCrclLabels.length != actions.size()) {
            actionToCrclLabels = new String[actions.size()];
        }
        for (int i = startingIndex; i < actionToCrclLabels.length; i++) {
            actionToCrclLabels[i] = "UNDEFINED";
        }
        if (null == actionToCrclTakenPartsNames || actionToCrclTakenPartsNames.length != actions.size()) {
            actionToCrclTakenPartsNames = new String[actions.size()];
        }
        if (startingIndex == 0) {
            this.lastTakenPart = null;
        }
        addSetUnits(cmds);
        for (lastIndex = startingIndex; lastIndex < actions.size(); lastIndex++) {

            PddlAction action = actions.get(lastIndex);
            System.out.println("action = " + action);
//            try {
            switch (action.getType()) {
                case "take-part":
                    takePart(action, cmds);
                    break;

                case "fake-take-part":
                    fakeTakePart(action, cmds);
                    break;

                case "test-part-position":
                    testPartPosition(action, cmds);
                    break;
                case "look-for-part":
                case "look-for-parts":
                    lookForParts(action, cmds);
                    actionToCrclIndexes[lastIndex] = cmds.size();
                    actionToCrclLabels[lastIndex] = "";
                    actionToCrclTakenPartsNames[lastIndex] = this.lastTakenPart;
                    return cmds;

                case "place-part":
                    placePart(action, cmds);
                    break;

                //case "inspect-kit":
                    //inspectKit(action, cmds);
                 //   break;
            }

            actionToCrclIndexes[lastIndex] = cmds.size();
            actionToCrclLabels[lastIndex] = "";
            actionToCrclTakenPartsNames[lastIndex] = this.lastTakenPart;
            final int markerIndex = lastIndex;
            addMarkerCommand(cmds, "end action " + markerIndex + ": " + action.getType(),
                    (CrclCommandWrapper wrapper) -> {
                        notifyActionCompletedListeners(markerIndex, action);
                    });
        }
        return cmds;
    }

    private final Map<String, PoseType> returnPosesByName = new HashMap<>();

    public Map<String, PoseType> getReturnPosesByName() {
        return returnPosesByName;
    }

    private String lastTakenPart = null;

    public String getLastTakenPart() {
        return lastTakenPart;
    }

    private BigDecimal slowTransSpeed = BigDecimal.valueOf(75.0);

    ;

    /**
     * Get the value of slowTransSpeed
     *
     * @return the value of slowTransSpeed
     */
    public BigDecimal getSlowTransSpeed() {
        return slowTransSpeed;
    }

    /**
     * Set the value of slowTransSpeed
     *
     * @param slowTransSpeed new value of slowTransSpeed
     */
    public void setSlowTransSpeed(BigDecimal slowTransSpeed) {
        this.slowTransSpeed = slowTransSpeed;
    }

    private BigDecimal lookDwellTime = BigDecimal.valueOf(3.0);

    /**
     * Get the value of lookDwellTime
     *
     * @return the value of lookDwellTime
     */
    public BigDecimal getLookDwellTime() {
        return lookDwellTime;
    }

    /**
     * Set the value of lookDwellTime
     *
     * @param lookDwellTime new value of lookDwellTime
     */
    public void setLookDwellTime(BigDecimal lookDwellTime) {
        this.lookDwellTime = lookDwellTime;
    }

    public void returnPart(String part, List<MiddleCommandType> out) {
        placePartByPose(out, returnPosesByName.get(part));
    }

    private BigDecimal fastTransSpeed = BigDecimal.valueOf(250.0);

    /**
     * Get the value of fastTransSpeed
     *
     * @return the value of fastTransSpeed
     */
    public BigDecimal getFastTransSpeed() {
        return fastTransSpeed;
    }

    /**
     * Set the value of fastTransSpeed
     *
     * @param fastTransSpeed new value of fastTransSpeed
     */
    public void setFastTransSpeed(BigDecimal fastTransSpeed) {
        this.fastTransSpeed = fastTransSpeed;
    }

    private BigDecimal settleDwellTime = new BigDecimal(0.25);

    /**
     * Get the value of settleDwellTime
     *
     * @return the value of settleDwellTime
     */
    public BigDecimal getSettleDwellTime() {
        return settleDwellTime;
    }

    /**
     * Set the value of settleDwellTime
     *
     * @param settleDwellTime new value of settleDwellTime
     */
    public void setSettleDwellTime(BigDecimal settleDwellTime) {
        this.settleDwellTime = settleDwellTime;
    }

    private VectorType xAxis = vector(1.0, 0.0, 0.0);

    private PmRpy rpy = new PmRpy();

    /**
     * Get the value of rpy
     *
     * @return the value of rpy
     */
    public PmRpy getRpy() {
        return rpy;
    }

    /**
     * Set the value of rpy
     *
     * @param rpy new value of rpy
     */
    public void setRpy(PmRpy rpy) {
        this.rpy = rpy;
    }

    public PoseType correctPose(PoseType poseIn) {
        PoseType pout = poseIn;
        if (null != getPositionMaps()) {
            for (PositionMap pm : getPositionMaps()) {
                pout = pm.correctPose(pout);
            }
        }
        return pout;
    }

    private AprsJFrame aprsJFrame;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    public AprsJFrame getAprsJFrame() {
        return aprsJFrame;
    }

    /**
     * Set the value of aprsJFrame
     *
     * @param aprsJFrame new value of aprsJFrame
     */
    public void setAprsJFrame(AprsJFrame aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
    }

    public void takeSimViewSnapshot(File f, PoseType pose, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, pose, label);
        }
    }

    public void addTakeSimViewSnapshot(List<MiddleCommandType> out,
            String title, final PoseType pose, String label) {
        if (takeSnapshots) {
            final String filename = getRunPrefix() + title;
            addMarkerCommand(out, title, x -> {
                try {
                    takeSimViewSnapshot(File.createTempFile(filename, ".PNG"), pose, label);
                } catch (IOException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    public String getRunPrefix() {
        return getRunName() + String.format("%03d", crclNumber) + "-action-" + String.format("%03d", lastIndex);
    }

    public void testPartPosition(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[1];
        MessageType msg = new MessageType();
        msg.setMessage("take-part " + partName);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);

        PoseType pose = getPose(partName);
        pose = correctPose(pose);
        returnPosesByName.put(action.getArgs()[1], pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        testPartPositionPose(out, pose);
        lastTakenPart = partName;
    }

    //-- contains all the slots that do not have a part
    private ArrayList nonFilledSlotList = new ArrayList();

    public void showInspection(PartsTray kittray) {

        String imageToUse = jframe.getKitImage();
        String externalShape = kittray.getExternalShape();
        if (externalShape.contains(".")){
            int dotIndex = externalShape.indexOf(".");
            if (dotIndex != -1)
                externalShape = externalShape.substring(0, dotIndex);
        }
        String pathToImage = "/aprs/framework/screensplash/" +externalShape+"/"+imageToUse + ".png";
        System.out.println("pathToImage " + pathToImage);
        jframe.setTitle(kittray.getPartsTrayName());
        jframe.getKitTitleLabel().setText("Inspecting " + kittray.getPartsTrayName());
        jframe.getKitImageLabel().setIcon(new javax.swing.ImageIcon(getClass().getResource(pathToImage)));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        jframe.setLocation(screenSize.width / 2 - jframe.getSize().width / 2, screenSize.height / 2 - jframe.getSize().height / 2);
        jframe.pack();
        jframe.setVisible(true);
    }

    private void addToInspectionResultJTextArea(String text) {
        InspectionResultJTextArea.append(text + "\n");
    }
    /**
     * @brief Inspects a finished kit to check if it is complete
     * @param action
     * @param out
     * @throws IllegalStateException
     * @throws SQLException
     */
   public Set<Slot> inspectKit(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        
        //-- This is a list of parts trays retrieved from a public static variable
        //-- stored in DatabasePoseUpdater
        //-- This variable has X,Y, and rotation for all parts trays
        List<PartsTray> partsTrayList = DatabasePoseUpdater.partsTrayList;

        checkSettings();

        String kitSku = action.getArgs()[0];
        MessageType msg = new MessageType();

        msg.setMessage("inspect-kit " + kitSku);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);

        //-- Get the number of parts expected to be in this kit using the kit part design

        int partDesignPartCount = getPartDesignPartCount(kitSku);
        System.out.println("\n\n---Inspecting " + kitSku);
        System.out.println("---" + kitSku + " should contain " + partDesignPartCount + " parts.");
        //-- Cypher query to retrieve all parts trays with the kit sku
        List<PartsTray> myPartsTraysList = getPartsTrays(kitSku);

        //-- replace all _pt with _kt in TakenPartList
        //-- This is to get all part that contains "in_kt" in their names
        //-- from the database
        for (int i = 0; i < TakenPartList.size(); i++) {
            String part_in_pt = TakenPartList.get(i);
            String tmpPartName = part_in_pt.replace("in_pt", "in_kt");
            int indexLastUnderscore = tmpPartName.lastIndexOf("_");
            String part_in_kt = tmpPartName.substring(0, indexLastUnderscore);
            TakenPartList.set(i, part_in_kt);
        }
        //-- Remove duplicates from TakenPartList
        //-- TODO: use a hashmap instead of a list
        //Set<String> hs = new HashSet<>();
        //hs.addAll(TakenPartList);
        //TakenPartList.clear();
        //TakenPartList.addAll(hs);

        //-- Read myPartsTraysList and get their poses
        for (int i = 0; i < myPartsTraysList.size(); i++) {
           EmptySlotSet = new HashSet<Slot>();
            int numberOfPartsInKit = 0;
            PartsTray myPartsTray = myPartsTraysList.get(i);
            //-- jframe
            jframe = new Inspection();  
            InspectionResultJTextArea = jframe.getInspectionResultJTextArea();
            PoseType trayPose = qs.getPose(myPartsTray.getPartsTrayName());
            trayPose = correctPose(trayPose);
            BigDecimal xbd = trayPose.getPoint().getX();
            double trayX = xbd.doubleValue();
            BigDecimal ybd = trayPose.getPoint().getY();
            double trayY = ybd.doubleValue();
            System.out.println("+++ partsTray :(" + trayX + "," + trayY + ")");
           
            double rotation=0;
            //-- Read partsTrayList
            //-- Assign rotation to myPartsTray by comparing poses
            for (int c = 0; c < partsTrayList.size(); c++) {
                PartsTray pt = partsTrayList.get(c);
                double ptX = pt.getX();
                double ptY = pt.getY();
                
                //-- Check if X for parts trays are close enough
                double diffX = Math.abs(trayX - ptX);
                //System.out.println("diffX= "+diffX);
                if (diffX < 1E-7) {
                    //-- Check if Y for parts trays are close enough
                    double diffY = Math.abs(trayY - ptY);
                    //System.out.println("diffY= "+diffY);
                    if (diffY < 1E-7) {
                        myPartsTray.setRotation(pt.getRotation());
                        rotation=pt.getRotation();
                    }
                }
            }
            //double rotation = myPartsTray.getRotation();
            System.out.println("rotation " + rotation);
            
            //-- retrieve the rotationOffset
            double rotationOffset = DatabasePoseUpdater.myRotationOffset;
            //System.out.println("rotationOffset " + rotationOffset);

            //-- compute the angle
            double angle = normAngle(rotation + rotationOffset);
           
            //-- Get all the slots for the current parts tray
            List<Slot> slotList = myPartsTray.getSlotList();
            for (int j = 0; j < slotList.size(); j++) {
                Slot slot = slotList.get(j);

                double x_offset = slot.getX_OFFSET() * 1000;
                double y_offset = slot.getY_OFFSET() * 1000;
                double slotX = trayX + x_offset * Math.cos(angle) - y_offset * Math.sin(angle);
                double slotY = trayY + x_offset * Math.sin(angle) + y_offset * Math.cos(angle);
                slot.setCorrectX(slotX);
                slot.setCorrectY(slotY);
                //System.out.println(slot.getSlotName() + ":(" + x_offset + "," + y_offset + ")");
                System.out.println("++++++ "+slot.getSlotName() + ":(" + slotX + "," + slotY + ")");

                //-- we want to filter out from TakenPartList parts that
                //-- do not match slot part sku
                //-- e.g., In TakenPartList keep only parts that contain part_large_gear
                //-- if part sku for this slot is sku_part_large_gear
                String partSKU = slot.getPartSKU();
                if (partSKU.startsWith("sku_")) {
                    partSKU = partSKU.substring(4).concat("_in_kt");
                }

                numberOfPartsInKit = numberOfPartsInKit + checkPartsInSlot(partSKU, slot);
            }

            if (numberOfPartsInKit == partDesignPartCount) {
                System.out.println("Kit is complete\n\n");
                jframe.setKitImage("complete");

            } else {
                int missingParts = partDesignPartCount - numberOfPartsInKit;
                addToInspectionResultJTextArea("Kit is missing " + missingParts + " part(s):");
                System.out.println("Kit is missing " + missingParts + " part(s):");
               
                if (!EmptySlotSet.isEmpty()) {
                    List<Integer> slotID = new ArrayList();
                    for (Slot s : EmptySlotSet) {
                        System.out.println("-------- Slot "+s.getSlotName()+ " is missing a part of type "+s.getPartSKU());
                        addToInspectionResultJTextArea("Slot "+s.getSlotName()+ " is missing a part of type "+s.getPartSKU());
                        slotID.add(s.getID());
                    }
                    Collections.sort(slotID);
                    String res="";
                    for (int a=0; a<slotID.size(); a++)
                    {
                        res =res.concat(slotID.get(a).toString());
                    }
                    jframe.setKitImage(res);
                }
                System.out.println("\n\n");
            }  
            showInspection(myPartsTray);
        }
        TakenPartList.clear();
         return EmptySlotSet;
    }

   
   
   public void inspectKitSimulation(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        
        //-- This is a list of parts trays retrieved from a public static variable
        //-- stored in DatabasePoseUpdater
        //-- This variable has X,Y, and rotation for all parts trays
        List<PartsTray> partsTrayList = DatabasePoseUpdater.partsTrayList;

        checkSettings();

        String kitSku = action.getArgs()[0];
        MessageType msg = new MessageType();

        msg.setMessage("inspect-kit " + kitSku);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);        
        TakenPartList.clear();
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
       
    
private int checkPartsInSlot(String partInKt, Slot slot) throws SQLException {
        int nbOfOccupiedSlots = 0;















        List<String> allPartsInKt = getAllPartsInKt(partInKt);




        for (int i = 0; i < allPartsInKt.size(); i++) {


            String newPartInKt = allPartsInKt.get(i);
            System.out.print("--------- "+newPartInKt);

            if (checkPartInSlot(newPartInKt, slot)) {
                System.out.println("--------- Located in slot");
                nbOfOccupiedSlots++;









            } else
                EmptySlotSet.add(slot);



        }
        return nbOfOccupiedSlots;




    }


    private Boolean checkPartInSlot(String partName, Slot slot) throws SQLException {
        //System.out.println("----- Part " + partName);

        //System.out.println("----- Slot " + slot.getSlotName());
        Boolean isPartInSlot = false;

        PoseType posePart = getPose(partName);
        posePart = correctPose(posePart);
        double partX = posePart.getPoint().getX().doubleValue();
        double partY = posePart.getPoint().getY().doubleValue();
        double slotX = slot.getCorrectX();
        double slotY = slot.getCorrectY();
        double x = partX - slotX;
        double y = partY - slotY;
        System.out.println(":(" + partX + "," + partY + ")");
        // System.out.println("----- Slot " + slot.getSlotName() + " : (" + slotX + "," + slotY + ")");
        //-- compute distance between 2 points
        double powx = Math.pow(x, 2);
        double powy = Math.pow(y, 2);
        double dist = Math.sqrt(powx + powy);
        // compare finalres with a specified tolerance value of 6.5 mm
        double threshold = 6.5;
        if (dist < threshold) {






            isPartInSlot = true;

           // System.out.println("----- Part " + partName + " : (" + partX + "," + partY + ")");

           // System.out.println("----- Slot " + slot.getSlotName() + " : (" + slotX + "," + slotY + ")");

            System.out.println("----- Distance between part and slot = " + dist);


        }
        return isPartInSlot;
    }

    private static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    private int takePartArgIndex;

    /**
     * Get the value of takePartArgIndex
     *
     * @return the value of takePartArgIndex
     */
    public int getTakePartArgIndex() {
        return takePartArgIndex;
    }

    /**
     * Set the value of takePartArgIndex
     *
     * @param takePartArgIndex new value of takePartArgIndex
     */
    public void setTakePartArgIndex(int takePartArgIndex) {
        this.takePartArgIndex = takePartArgIndex;
    }

    public void takePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];
        MessageType msg = new MessageType();
        msg.setMessage("take-part " + partName);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);

        PoseType pose = getPose(partName);
        if (takeSnapshots) {
            try {
                takeSimViewSnapshot(File.createTempFile(getRunPrefix() + "-take-part-" + partName + "-", ".PNG"), pose, partName);
            } catch (IOException ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        pose = correctPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        takePartByPose(out, pose);
        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, x -> {
            System.out.println(markerMsg + " at " + new Date());
        });
        lastTakenPart = partName;
        //inspectionList.add(partName);
        TakenPartList.add(partName);
    }

    public void fakeTakePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];
        MessageType msg = new MessageType();
        msg.setMessage("take-part " + partName);
        msg.setCommandID(BigInteger.valueOf(out.size() + 2));
        out.add(msg);

        PoseType pose = getPose(partName);
        pose = correctPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        fakeTakePartByPose(out, pose);
        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, x -> {
            System.out.println(markerMsg + " at " + new Date());
        });
        lastTakenPart = partName;
        //inspectionList.add(partName);
           TakenPartList.add(partName);
    }

    public PoseType getPose(String posename) throws SQLException {
        final AtomicReference<SQLException> getNewPoseFromDbException = new AtomicReference<>();
        PoseType pose = poseCache.computeIfAbsent(posename,
                (String key) -> {
                    try {
                        return getNewPoseFromDb(key);
                    } catch (SQLException ex) {
                        getNewPoseFromDbException.set(ex);
                    }
                    return null;
                }
        );
        SQLException ex = getNewPoseFromDbException.getAndSet(null);
        if (null != ex) {
            throw ex;
        }
        return pose;
    }

    private PoseType getNewPoseFromDb(String posename) throws SQLException {
        PoseType pose = qs.getPose(posename);
        return pose;
    }

    
    public List<PartsTray> getPartsTrays(String name) throws SQLException {
        List<PartsTray> list = new ArrayList<>(qs.getPartsTrays(name));

        return list;
    }
    public int getPartDesignPartCount(String kitName) throws SQLException {
        int count = qs.getPartDesignPartCount(kitName);
        return count;
    }

    public List<String> getAllPartsInKt(String name) throws SQLException {
        List<String> partsInKtList = new ArrayList<>(qs.getAllPartsInKt(name));

        return partsInKtList;
    }

    public void testPartPositionPose(List<MiddleCommandType> cmds, PoseType pose) {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ().add(takeZOffset));

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true);

        addSettleDwell(cmds);

//        addCloseGripper(cmds);
//
//        addSettleDwell(cmds);
//
//        addMoveTo(cmds, poseAbove, true);
//
//        addSettleDwell(cmds);
    }

    public void takePartByPose(List<MiddleCommandType> cmds, PoseType pose) {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ().add(takeZOffset));

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true);

        addSettleDwell(cmds);

        addCloseGripper(cmds);

        addSettleDwell(cmds);

        addMoveTo(cmds, approachPose, true);

        addSettleDwell(cmds);
    }

    public void fakeTakePartByPose(List<MiddleCommandType> cmds, PoseType pose) {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ().add(takeZOffset));

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true);

        addSettleDwell(cmds);

//       We force a failure by skipping the step that closes the gripper  addCloseGripper(cmds);
        addSettleDwell(cmds);

        addMoveTo(cmds, approachPose, true);

        addSettleDwell(cmds);
    }

    private BigDecimal rotSpeed = BigDecimal.valueOf(30.0);

    /**
     * Get the value of rotSpeed
     *
     * @return the value of rotSpeed
     */
    public BigDecimal getRotSpeed() {
        return rotSpeed;
    }

    /**
     * Set the value of rotSpeed
     *
     * @param rotSpeed new value of rotSpeed
     */
    public void setRotSpeed(BigDecimal rotSpeed) {
        this.rotSpeed = rotSpeed;
    }

    private void checkSettings() {
        String rpyString = options.get("rpy");
        if (null != rpyString && rpyString.length() > 0) {
            try {
                String rpyFields[] = rpyString.split("[, \t]+");
                if (rpyFields.length == 3) {
                    rpy = new PmRpy();
                    rpy.r = Math.toRadians(Double.valueOf(rpyFields[0]));
                    rpy.p = Math.toRadians(Double.valueOf(rpyFields[1]));
                    rpy.y = Math.toRadians(Double.valueOf(rpyFields[2]));
                    PoseType pose = CRCLPosemath.toPoseType(new PmCartesian(), rpy);
                    xAxis = pose.getXAxis();
                    zAxis = pose.getZAxis();
                } else {
                    throw new Exception("bad rpyString = \"" + rpyString + "\", rpyFields=" + Arrays.toString(rpyFields));
                }
            } catch (Exception ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (xAxis == null) {
            xAxis = vector(1.0, 0.0, 0.0);
            zAxis = vector(0.0, 0.0, -1.0);
        }
        String approachZOffsetString = options.get("approachZOffset");
        if (null != approachZOffsetString && approachZOffsetString.length() > 0) {
            try {
                double val = Double.valueOf(approachZOffsetString);
                approachZOffset = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String placeZOffsetString = options.get("placeZOffset");
        if (null != placeZOffsetString && placeZOffsetString.length() > 0) {
            try {
                double val = Double.valueOf(placeZOffsetString);
                placeZOffset = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String takeZOffsetString = options.get("takeZOffset");
        if (null != takeZOffsetString && takeZOffsetString.length() > 0) {
            try {
                double val = Double.valueOf(takeZOffsetString);
                takeZOffset = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String settleDwellTimeString = options.get("settleDwellTime");
        if (null != settleDwellTimeString && settleDwellTimeString.length() > 0) {
            try {
                double val = Double.valueOf(settleDwellTimeString);
                settleDwellTime = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String lookDwellTimeString = options.get("lookDwellTime");
        if (null != lookDwellTimeString && lookDwellTimeString.length() > 0) {
            try {
                double val = Double.valueOf(lookDwellTimeString);
                lookDwellTime = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String fastTransSpeedString = options.get("fastTransSpeed");
        if (null != fastTransSpeedString && fastTransSpeedString.length() > 0) {
            try {
                double val = Double.valueOf(fastTransSpeedString);
                fastTransSpeed = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String rotSpeedString = options.get("rotSpeed");
        if (null != rotSpeedString && rotSpeedString.length() > 0) {
            try {
                double val = Double.valueOf(rotSpeedString);
                rotSpeed = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String jointSpeedString = options.get("jointSpeed");
        if (null != jointSpeedString && jointSpeedString.length() > 0) {
            try {
                double val = Double.valueOf(jointSpeedString);
                jointSpeed = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String jointAccelString = options.get("jointAccel");
        if (null != jointAccelString && jointAccelString.length() > 0) {
            try {
                double val = Double.valueOf(jointAccelString);
                jointAccel = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String slowTransSpeedString = options.get("slowTransSpeed");
        if (null != slowTransSpeedString && slowTransSpeedString.length() > 0) {
            try {
                double val = Double.valueOf(slowTransSpeedString);
                slowTransSpeed = BigDecimal.valueOf(val);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String takePartArgIndexString = options.get("takePartArgIndex");
        if (null != takePartArgIndexString && takePartArgIndexString.length() > 0) {
            this.takePartArgIndex = Integer.valueOf(takePartArgIndexString);
        }
        String placePartSlotArgIndexString = options.get("placePartSlotArgIndex");
        if (null != placePartSlotArgIndexString && placePartSlotArgIndexString.length() > 0) {
            this.placePartSlotArgIndex = Integer.valueOf(placePartSlotArgIndexString);
        }
        String takeSnapshotsString = options.get("takeSnapshots");
        if (null != takeSnapshotsString && takeSnapshotsString.length() > 0) {
            takeSnapshots = Boolean.valueOf(takeSnapshotsString);
        }
    }

    private void addOpenGripper(List<MiddleCommandType> cmds) {
        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
        openGripperCmd.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        openGripperCmd.setSetting(BigDecimal.ONE);
        cmds.add(openGripperCmd);
    }

    private void addCloseGripper(List<MiddleCommandType> cmds) {
        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        closeGrippeerCmd.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        closeGrippeerCmd.setSetting(BigDecimal.ZERO);
        cmds.add(closeGrippeerCmd);
    }

    private void addMoveTo(List<MiddleCommandType> cmds, PoseType poseAbove, boolean straight) {
        MoveToType moveAboveCmd = new MoveToType();
        moveAboveCmd.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        moveAboveCmd.setEndPosition(poseAbove);
        moveAboveCmd.setMoveStraight(straight);
        cmds.add(moveAboveCmd);
    }

    private void addSetSlowSpeed(List<MiddleCommandType> cmds) {
        SetTransSpeedType stst = new SetTransSpeedType();
        stst.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(slowTransSpeed);
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private void addSetFastSpeed(List<MiddleCommandType> cmds) {

        SetRotSpeedType srs = new SetRotSpeedType();
        RotSpeedAbsoluteType rsa = new RotSpeedAbsoluteType();
        rsa.setSetting(rotSpeed);
        srs.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        srs.setRotSpeed(rsa);
        cmds.add(srs);

        SetTransSpeedType stst = new SetTransSpeedType();
        stst.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(fastTransSpeed);
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private void addSetUnits(List<MiddleCommandType> cmds) {
        SetLengthUnitsType slu = new SetLengthUnitsType();
        slu.setUnitName(LengthUnitEnumType.MILLIMETER);
        slu.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        cmds.add(slu);

        SetAngleUnitsType sau = new SetAngleUnitsType();
        sau.setUnitName(AngleUnitEnumType.DEGREE);
        sau.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        cmds.add(sau);
    }

    public void addMoveToLookForPosition(List<MiddleCommandType> out) {
        String lookforXYZSring = options.get("lookForXYZ");
        String lookForXYZFields[] = lookforXYZSring.split(",");
        String useLookForJointString = options.get("useJointLookFor");
        boolean useLookForJoint = (null != useLookForJointString && useLookForJointString.length() > 0 && Boolean.valueOf(useLookForJointString));
        String lookForJointsString = options.get("lookForJoints");
        if (null == lookForJointsString || lookForJointsString.length() < 1) {
            useLookForJoint = false;
        }
        addSetFastSpeed(out);

        addOpenGripper(out);

        if (!useLookForJoint) {
            PoseType pose = new PoseType();
            pose.setPoint(point(Double.valueOf(lookForXYZFields[0]), Double.valueOf(lookForXYZFields[1]), Double.valueOf(lookForXYZFields[2])));
            pose.setXAxis(xAxis);
            pose.setZAxis(zAxis);
            addMoveTo(out, pose, false);
        } else {
            addJointMove(out, lookForJointsString);
        }
    }

    private BigDecimal jointSpeed = BigDecimal.valueOf(5.0);

    /**
     * Get the value of jointSpeed
     *
     * @return the value of jointSpeed
     */
    public BigDecimal getJointSpeed() {
        return jointSpeed;
    }

    /**
     * Set the value of jointSpeed
     *
     * @param jointSpeed new value of jointSpeed
     */
    public void setJointSpeed(BigDecimal jointSpeed) {
        this.jointSpeed = jointSpeed;
    }

    private BigDecimal jointAccel = BigDecimal.valueOf(100.0);

    /**
     * Get the value of jointAccel
     *
     * @return the value of jointAccel
     */
    public BigDecimal getJointAccel() {
        return jointAccel;
    }

    /**
     * Set the value of jointAccel
     *
     * @param jointAccel new value of jointAccel
     */
    public void setJointAccel(BigDecimal jointAccel) {
        this.jointAccel = jointAccel;
    }

    private void addJointMove(List<MiddleCommandType> out, String jointVals) {
        ActuateJointsType ajCmd = new ActuateJointsType();
        ajCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        ajCmd.getActuateJoint().clear();
        String jointPosStrings[] = jointVals.split("[,]+");
        for (int i = 0; i < jointPosStrings.length; i++) {
            ActuateJointType aj = new ActuateJointType();
            JointSpeedAccelType jsa = new JointSpeedAccelType();
            jsa.setJointAccel(jointAccel);
            jsa.setJointSpeed(jointSpeed);
            aj.setJointDetails(jsa);
            aj.setJointNumber(BigInteger.valueOf(i + 1));
            aj.setJointPosition(BigDecimal.valueOf(Double.valueOf(jointPosStrings[i])));
            ajCmd.getActuateJoint().add(aj);
        }
        out.add(ajCmd);
    }

    private void clearPoseCache() {
        poseCache.clear();
    }

    private void lookForParts(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {

        checkSettings();
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        addMoveToLookForPosition(out);

        addLookDwell(out);

        addTakeSimViewSnapshot(out, "-look-for-parts-", null, "");
        addMarkerCommand(out, "clear pose cache", x -> this.clearPoseCache());
    }

//    private void addOpenGripper(List<MiddleCommandType> out, PoseType pose) {
//        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
//        openGripperCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
//        openGripperCmd.setSetting(BigDecimal.ONE);
//    }
    private void addLookDwell(List<MiddleCommandType> out) {
        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
        dwellCmd.setDwellTime(lookDwellTime);
        out.add(dwellCmd);
    }

    public static class PlacePartInfo {

        private final PddlAction action;
        private final int pddlActionIndex;
        private final int outIndex;
        private CrclCommandWrapper wrapper = null;

        public PlacePartInfo(PddlAction action, int pddlActionIndex, int outIndex) {
            this.action = action;
            this.pddlActionIndex = pddlActionIndex;
            this.outIndex = outIndex;
        }

        public CrclCommandWrapper getWrapper() {
            return wrapper;
        }

        public void setWrapper(CrclCommandWrapper wrapper) {
            this.wrapper = wrapper;
        }

        public PddlAction getAction() {
            return action;
        }

        public int getPddlActionIndex() {
            return pddlActionIndex;
        }

        public int getOutIndex() {
            return outIndex;
        }

    }

    private ConcurrentLinkedQueue<Consumer<PlacePartInfo>> placePartConsumers = new ConcurrentLinkedQueue<>();

    public void addPlacePartConsumer(Consumer<PlacePartInfo> consumer) {
        placePartConsumers.add(consumer);
    }

    public void removePlacePartConsumer(Consumer<PlacePartInfo> consumer) {
        placePartConsumers.remove(consumer);
    }

    public void notifyPlacePartConsumers(PlacePartInfo ppi) {
        placePartConsumers.forEach(consumer -> consumer.accept(ppi));
    }

    private int placePartSlotArgIndex;

    /**
     * Get the value of placePartSlotArgIndex
     *
     * @return the value of placePartSlotArgIndex
     */
    public int getPlacePartSlotArgIndex() {
        return placePartSlotArgIndex;
    }

    /**
     * Set the value of placePartSlotArgIndex
     *
     * @param placePartSlotArgIndex new value of placePartSlotArgIndex
     */
    public void setPlacePartSlotArgIndex(int placePartSlotArgIndex) {
        this.placePartSlotArgIndex = placePartSlotArgIndex;
    }

    private void placePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String slotName = action.getArgs()[placePartSlotArgIndex];
        PoseType pose = getPose(slotName);

        final String msg = "placed part " + getLastTakenPart() + " in " + slotName;
        if (takeSnapshots) {
            try {
                takeSimViewSnapshot(File.createTempFile(getRunPrefix() + "-place-part-" + getLastTakenPart() + "-in-" + slotName + "-", ".PNG"), pose, slotName);
            } catch (IOException ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        pose = correctPose(pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);

        placePartByPose(out, pose);
        final PlacePartInfo ppi = new PlacePartInfo(action, lastIndex, out.size());
        addMarkerCommand(out, msg,
                ((CrclCommandWrapper wrapper) -> {
                    System.out.println(msg + " completed at " + new Date());
                    ppi.setWrapper(wrapper);
                    notifyPlacePartConsumers(ppi);
                }));
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    private VectorType zAxis = vector(0.0, 0.0, -1.0);

    public void placePartByPose(List<MiddleCommandType> cmds, PoseType pose) {

        checkSettings();

        PoseType poseAbove = CRCLPosemath.copy(pose);
        poseAbove.getPoint().setZ(pose.getPoint().getZ().add(approachZOffset));

        PoseType placePose = CRCLPosemath.copy(pose);
        placePose.getPoint().setZ(pose.getPoint().getZ().add(placeZOffset));

        addSetFastSpeed(cmds);

        addMoveTo(cmds, poseAbove, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, placePose, true);

        addSettleDwell(cmds);

        addOpenGripper(cmds);

        addSettleDwell(cmds);

        addMoveTo(cmds, poseAbove, true);

        addSettleDwell(cmds);

        this.lastTakenPart = null;
    }

    private void addSettleDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        dwellCmd.setDwellTime(settleDwellTime);
        cmds.add(dwellCmd);
    }

    private void addMarkerCommand(List<MiddleCommandType> cmds, String message, CRCLCommandWrapperConsumer cb) {
        MessageType messageCmd = new MessageType();
        messageCmd.setMessage(message);
        messageCmd.setCommandID(BigInteger.valueOf(cmds.size() + 2));
        CrclCommandWrapper wrapper = CrclCommandWrapper.wrapWithOnDone(messageCmd, cb);
        cmds.add(wrapper);
    }

    @Override
    public void accept(DbSetup setup) {
        this.setDbSetup(setup);
    }

    public static class ActionCallbackInfo {

        private final int actionIndex;
        private final PddlAction action;

        public ActionCallbackInfo(int actionIndex, PddlAction action) {
            this.actionIndex = actionIndex;
            this.action = action;
        }

        public int getActionIndex() {
            return actionIndex;
        }

        public PddlAction getAction() {
            return action;
        }
    }

    final private ConcurrentLinkedDeque<Consumer<ActionCallbackInfo>> actionCompletedListeners = new ConcurrentLinkedDeque<>();

    public void addActionCompletedListener(Consumer<ActionCallbackInfo> listener) {
        actionCompletedListeners.add(listener);
    }

    public void removeActionCompletedListener(Consumer<ActionCallbackInfo> listeners) {
        actionCompletedListeners.remove(listeners);
    }

    private void notifyActionCompletedListeners(int actionIndex, PddlAction action) {
        ActionCallbackInfo acbi = new ActionCallbackInfo(actionIndex, action);
        for (Consumer<ActionCallbackInfo> listener : actionCompletedListeners) {
            listener.accept(acbi);
        }
    }

    @Override
    public void close() throws Exception {
        if (closeDbConnection && null != dbConnection) {
            try {
                dbConnection.close();
            } catch (SQLException sQLException) {
            }
        }
        if (null != qs) {
            try {
                qs.close();
            } catch (Exception exception) {
            }
        }
        if (closeDbConnection) {
            dbConnection = null;
            qs = null;
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
