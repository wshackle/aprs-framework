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
import aprs.framework.database.DbSetupJPanel;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbType;
import aprs.framework.database.QuerySet;
import aprs.framework.kitinspection.KitInspectionJInternalFrame;
import crcl.base.ActuateJointType;
import crcl.base.ActuateJointsType;
import crcl.base.AngleUnitEnumType;
import crcl.base.DwellType;
import crcl.base.JointSpeedAccelType;
import crcl.base.LengthUnitEnumType;
import crcl.base.MessageType;
import crcl.base.MiddleCommandType;
import crcl.base.MoveToType;
import crcl.base.PointType;
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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.awt.Color;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.BadLocationException;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is responsible for generating CRCL Commands and Programs from PDDL
 * Action(s).
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlActionToCrclGenerator implements DbSetupListener, AutoCloseable {

    /**
     * Returns the run name which is useful for identifying the run in log files
     * or saving snapshot files.
     *
     * @return the run name
     */
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
    private final List<String> TakenPartList = new ArrayList<>();
    private Set<Slot> EmptySlotSet;
    private List<PoseType> PlacePartSlotPoseList = null;
    private boolean takeSnapshots = false;
    private int crclNumber = 0;
    private final ConcurrentMap<String, PoseType> poseCache = new ConcurrentHashMap<>();
    private KitInspectionJInternalFrame kitInspectionJInternalFrame = null;
    PartsTray correctPartsTray = null;
    Boolean part_in_pt_found = false;
    Boolean part_in_kt_found = false;
    //String messageColorH3 = "#ffcc00";
    // top panel orange [255,204,0]
    String messageColorH3 = "#e0e0e0";
    Color warningColor = new Color(100, 71, 71);

    /**
     * Get the value of takeSnapshots
     *
     * When takeSnaphots is true image files will be saved when most actions are
     * planned and some actions are executed. This may be useful for debugging.
     *
     * @return the value of takeSnapshots
     */
    public boolean isTakeSnapshots() {
        return takeSnapshots;
    }

    /**
     * Set the value of takeSnapshots
     *
     * When takeSnaphots is true image files will be saved when most actions are
     * planned and some actions are executed. This may be useful for debugging.
     *
     * @param takeSnapshots new value of takeSnapshots
     */
    public void setTakeSnapshots(boolean takeSnapshots) {
        this.takeSnapshots = takeSnapshots;
    }

    private List<PositionMap> positionMaps = null;

    /**
     * Get the list of PositionMap's used to transform or correct poses from the
     * database before generating CRCL Commands to be sent to the robot.
     *
     * PositionMaps are similar to transforms in that they can represent
     * position offsets or rotations. But they can also represent changes in
     * scale or localized corrections due to distortion or imperfect kinematics.
     *
     * @return list of position maps.
     */
    public List<PositionMap> getPositionMaps() {
        return positionMaps;
    }

    /**
     * Set the list of PositionMap's used to transform or correct poses from the
     * database before generating CRCL Commands to be sent to the robot.
     *
     * PositionMaps are similar to transforms in that they can represent
     * position offsets or rotations. But they can also represent changes in
     * scale or localized corrections due to distortion or imperfect kinematics.
     *
     * @param errorMap list of position maps.
     */
    public void setPositionMaps(List<PositionMap> errorMap) {
        this.positionMaps = errorMap;
    }

//    public List<TraySlotDesign> getAllTraySlotDesigns() throws SQLException {
//        return qs.getAllTraySlotDesigns();
//    }
//
//    public List<TraySlotDesign> getSingleTraySlotDesign(String partDesignName, String trayDesignName) throws SQLException {
//        return qs.getSingleTraySlotDesign(partDesignName, trayDesignName);
//    }
//
//    public void setSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
//        qs.setSingleTraySlotDesign(tsd);
//    }
//
//    public void newSingleTraySlotDesign(TraySlotDesign tsd) throws SQLException {
//        qs.newSingleTraySlotDesign(tsd);
//    }
    /**
     * Check if this is connected to the database.
     *
     * @return is this generator connected to the database.
     */
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

    /**
     * Get the database connection being used.
     *
     * @return the database connection
     */
    public Connection getDbConnection() {
        return dbConnection;
    }

    private boolean debug;

    /**
     * Get the value of debug
     *
     * When debug is true additional messages will be printed to the console.
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * When debug is true additional messages will be printed to the console.
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        this.qs.setDebug(debug);
    }

    /**
     * Set the database connection to use.
     *
     * @param dbConnection new database connection to use
     */
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

    /**
     * Get the database setup object.
     *
     * @return database setup object.
     */
    public DbSetup getDbSetup() {
        return dbSetup;
    }

    /**
     * Set the database setup object.
     *
     * @param dbSetup new database setup object to use.
     */
    public void setDbSetup(DbSetup dbSetup) {

        this.dbSetup = dbSetup;
        if (null != this.dbSetup && this.dbSetup.isConnected()) {
            if (null == dbSetup.getDbType() || DbType.NONE == dbSetup.getDbType()) {
                throw new IllegalArgumentException("dbSetup.getDbType() =" + dbSetup.getDbType());
            }
            if (dbConnection == null) {
                try {
                    final StackTraceElement stackTraceElemArray[] = Thread.currentThread().getStackTrace();
                    DbSetupBuilder.connect(dbSetup).handle((c, ex) -> {
                        if (null != c) {
                            Utils.runOnDispatchThread(() -> {
                                setDbConnection(c);
                            });
                        }
                        if (null != ex) {
                            Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, null, ex);
                            System.err.println("Called from :");
                            for (int i = 0; i < stackTraceElemArray.length; i++) {
                                System.err.println(stackTraceElemArray[i]);
                            }
                            System.err.println("");
                            System.err.println("Exception handled at ");
                            Thread.dumpStack();
                            if (null != aprsJFrame) {
                                aprsJFrame.setTitleErrorString("Database error: " + ex.toString());
                            }
                        }
                        return c;
                    });
                    System.out.println("PddlActionToCrclGenerator connected to database of type " + dbSetup.getDbType() + " on host " + dbSetup.getHost() + " with port " + dbSetup.getPort());
                } catch (Exception ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            setDbConnection(null);
        }
    }

    private double approachZOffset = 50.0;
    private double placeZOffset = 5.0;
    private double takeZOffset = 0.0;

    private String actionToCrclTakenPartsNames[] = null;
    private int visionCycleNewDiffThreshold = 3;

    /**
     * Get an array of strings and null values relating each action to the last
     * part expected to have been taken after that action.
     *
     * @return array of taken part names
     */
    public String[] getActionToCrclTakenPartsNames() {
        return actionToCrclTakenPartsNames;
    }

    private int actionToCrclIndexes[] = null;

    /**
     * Get an array of indexes into the CRCL program associated with each PDDL
     * action.
     *
     * @return array of indexes into the CRCL program
     */
    public int[] getActionToCrclIndexes() {
        return actionToCrclIndexes;
    }

    private String actionToCrclLabels[] = null;

    /**
     * Get an array of strings with labels for each PDDL action.
     *
     * @return array of labels
     */
    public String[] getActionToCrclLabels() {
        return actionToCrclLabels;
    }

    private Map<String, String> options = null;

    private int lastIndex;

    /**
     * Get the value of index of the last PDDL action planned.
     *
     * @return the value of lastIndex
     */
    public int getLastIndex() {
        return lastIndex;
    }

    /**
     * Set the value ofindex of the last PDDL action planned.
     *
     * @param lastIndex new value of lastIndex
     */
    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

    /**
     * Get a map of options as a name to value map.
     *
     * @return options
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Set the options with a name to value map.
     *
     * @param options new value of options map
     */
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    private boolean doInspectKit = false;
    private boolean requireNewPoses = false;

    /**
     * Generate a list of CRCL commands from a list of PddlActions starting with
     * the given index, using the provided optons.
     *
     * @param actions list of PDDL Actions
     * @param startingIndex starting index into list of PDDL actions
     * @param options options to use as commands are generated
     * @return list of CRCL commands
     *
     * @throws IllegalStateException if database not connected
     * @throws SQLException if query of the database failed
     */
    public List<MiddleCommandType> generate(List<PddlAction> actions, int startingIndex, Map<String, String> options)
            throws IllegalStateException, SQLException {

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
            this.unitsSet = false;
            this.rotSpeedSet = false;
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
                    lookForParts(action, cmds, (lastIndex < 2),
                            doInspectKit ? (lastIndex == actions.size() - 1) : (lastIndex >= actions.size() - 2)
                    );
                    actionToCrclIndexes[lastIndex] = cmds.size();
                    actionToCrclLabels[lastIndex] = "";
                    actionToCrclTakenPartsNames[lastIndex] = this.lastTakenPart;
                    final int markerIndex = lastIndex;
                    addMarkerCommand(cmds, "end action " + markerIndex + ": " + action.getType() + " " + Arrays.toString(action.getArgs()),
                            (CrclCommandWrapper wrapper) -> {
                                notifyActionCompletedListeners(markerIndex, action);
                            });
                    return cmds;

                case "place-part":
                    placePart(action, cmds);
                    break;

                case "inspect-kit": {
                    if (doInspectKit) {
                        try {
                            inspectKit(action, cmds);
                        } catch (BadLocationException ex) {
                            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
                
                default:
                    throw new IllegalArgumentException("unrecognized action "+action+" at index "+lastIndex);
            }

            actionToCrclIndexes[lastIndex] = cmds.size();
            actionToCrclLabels[lastIndex] = "";
            actionToCrclTakenPartsNames[lastIndex] = this.lastTakenPart;
            final int markerIndex = lastIndex;
            addMarkerCommand(cmds, "end action " + markerIndex + ": " + action.getType() + " " + Arrays.toString(action.getArgs()),
                    (CrclCommandWrapper wrapper) -> {
                        notifyActionCompletedListeners(markerIndex, action);
                    });
        }
        return cmds;
    }

    private final Map<String, PoseType> returnPosesByName = new HashMap<>();

    private String lastTakenPart = null;

    private String getLastTakenPart() {
        return lastTakenPart;
    }

    private double slowTransSpeed = 75.0;

    /**
     * Get the value of slowTransSpeed
     *
     * @return the value of slowTransSpeed
     */
    public double getSlowTransSpeed() {
        return slowTransSpeed;
    }

    /**
     * Set the value of slowTransSpeed
     *
     * @param slowTransSpeed new value of slowTransSpeed
     */
    public void setSlowTransSpeed(double slowTransSpeed) {
        this.slowTransSpeed = slowTransSpeed;
    }

    private double lookDwellTime = 5.0;

    /**
     * Get the value of lookDwellTime
     *
     * @return the value of lookDwellTime
     */
    public double getLookDwellTime() {
        return lookDwellTime;
    }

    /**
     * Set the value of lookDwellTime
     *
     * @param lookDwellTime new value of lookDwellTime
     */
    public void setLookDwellTime(double lookDwellTime) {
        this.lookDwellTime = lookDwellTime;
    }

    /**
     * Adds commands to the list to return a part to the location it was taken
     * from. The part name must match a name of a part in the returnPosesByName
     * map.
     *
     * @param part name of part to be returned
     * @param out list of commands to append to
     */
    public void returnPart(String part, List<MiddleCommandType> out) {
        placePartByPose(out, returnPosesByName.get(part));
    }

    private double fastTransSpeed = 250.0;

    /**
     * Get the value of fastTransSpeed
     *
     * @return the value of fastTransSpeed
     */
    public double getFastTransSpeed() {
        return fastTransSpeed;
    }

    /**
     * Set the value of fastTransSpeed
     *
     * @param fastTransSpeed new value of fastTransSpeed
     */
    public void setFastTransSpeed(double fastTransSpeed) {
        this.fastTransSpeed = fastTransSpeed;
    }

    private double firstLookDwellTime = (5.0);

    /**
     * Get the value of firstLookDwellTime
     *
     * @return the value of firstLookDwellTime
     */
    public double getFirstLookDwellTime() {
        return firstLookDwellTime;
    }

    /**
     * Set the value of firstLookDwellTime
     *
     * @param firstLookDwellTime new value of firstLookDwellTime
     */
    public void setFirstLookDwellTime(double firstLookDwellTime) {
        this.firstLookDwellTime = firstLookDwellTime;
    }

    private double lastLookDwellTime = (1.0);

    /**
     * Get the value of lastLookDwellTime
     *
     * @return the value of lastLookDwellTime
     */
    public double getLastLookDwellTime() {
        return lastLookDwellTime;
    }

    /**
     * Set the value of lastLookDwellTime
     *
     * @param lastLookDwellTime new value of lastLookDwellTime
     */
    public void setLastLookDwellTime(double lastLookDwellTime) {
        this.lastLookDwellTime = lastLookDwellTime;
    }

    private double settleDwellTime = (0.25);

    /**
     * Get the value of settleDwellTime
     *
     * @return the value of settleDwellTime
     */
    public double getSettleDwellTime() {
        return settleDwellTime;
    }

    /**
     * Set the value of settleDwellTime
     *
     * @param settleDwellTime new value of settleDwellTime
     */
    public void setSettleDwellTime(double settleDwellTime) {
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

    /**
     * Modify the given pose by applying all of the currently added position
     * maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
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
        if(null == aprsJFrame && null != parentPddlExecutorJPanel) {
            aprsJFrame = parentPddlExecutorJPanel.getAprsJFrame();
        }
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

    private PddlExecutorJPanel parentPddlExecutorJPanel = null;

    /**
     * Get the value of parentPddlExecutorJPanel
     *
     * @return the value of parentPddlExecutorJPanel
     */
    public PddlExecutorJPanel getParentPddlExecutorJPanel() {
        return parentPddlExecutorJPanel;
    }

    /**
     * Set the value of parentPddlExecutorJPanel
     *
     * @param parentPddlExecutorJPanel new value of parentPddlExecutorJPanel
     */
    public void setParentPddlExecutorJPanel(PddlExecutorJPanel parentPddlExecutorJPanel) {
        this.parentPddlExecutorJPanel = parentPddlExecutorJPanel;
        setAprsJFrame(parentPddlExecutorJPanel.getAprsJFrame());
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param pose optional pose to mark or null
     * @param label optional label for pose or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, PoseType pose, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, pose, label);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @throws IOException if writing the file fails
     */
    public void takeDatabaseViewSnapshot(File f) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.startVisionToDbNewItemsImageSave(f);
        }
    }

    /**
     * Add a marker command that will cause a snapshot to be taken when the CRCL
     * command would be executed.
     *
     * @param out list of commands to append to
     * @param title title to add to snapshot filename
     * @param pose optional pose to highlight in snapshot or null
     * @param label optional label for highlighted pose or null.
     */
    public void addTakeSimViewSnapshot(List<MiddleCommandType> out,
            String title, final PoseType pose, String label) {
        if (takeSnapshots) {
            final String filename = getRunPrefix() + title;
            addMarkerCommand(out, title, x -> {
                try {
                    takeSimViewSnapshot(File.createTempFile(filename, ".PNG"), pose, label);
                    takeDatabaseViewSnapshot(File.createTempFile(filename + "_new_database_items", ".PNG"));
                } catch (IOException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    /**
     * Get a run prefix useful for naming/identifying snapshot files.
     *
     * @return run prefix
     */
    public String getRunPrefix() {
        return getRunName() + String.format("_%09d_", System.currentTimeMillis()) + String.format("%03d", crclNumber) + "-action-" + String.format("%03d", lastIndex);
    }

    private final AtomicLong commandId = new AtomicLong(100 * (System.currentTimeMillis() % 200));

    public final long incrementAndGetCommandId() {
        return commandId.incrementAndGet();
    }

    /**
     * Add commands to the list that will test a given part position by opening
     * the gripper and moving to that position but not actually taking the part.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @throws IllegalStateException if database is not connected
     * @throws SQLException if database query fails
     */
    public void testPartPosition(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[0];
        MessageType msg = new MessageType();
        msg.setMessage("take-part " + partName);
        msg.setCommandID(incrementAndGetCommandId());
        out.add(msg);

        PoseType pose = getPose(partName);
        pose = correctPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        testPartPositionByPose(out, pose);
        lastTakenPart = partName;
    }

    //-- contains all the slots that do not have a part
    private ArrayList nonFilledSlotList = new ArrayList();

    public void setCorrectKitImage() {
        String kitinspectionImageKitPath = kitInspectionJInternalFrame.getKitinspectionImageKitPath();
        String kitImage = kitInspectionJInternalFrame.getKitImage();
        String kitStatusImage = kitinspectionImageKitPath + "/" + kitImage + ".png";
        System.out.println("kitStatusImage " + kitStatusImage);
        kitInspectionJInternalFrame.getKitImageLabel().setIcon(kitInspectionJInternalFrame.createImageIcon(kitStatusImage));
    }

    /**
     * Inspects a finished kit to check if it is complete
     *
     * @param action PDDL Action
     * @param out list of commands to append to
     * @throws IllegalStateException if database is not connected
     * @throws SQLException if query fails
     * @throws javax.swing.text.BadLocationException when there are bad
     * locations within a document model
     */
    public void inspectKit(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, BadLocationException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        if(action.getArgs().length < 2) {
            throw new IllegalArgumentException("action = "+action+" needs at least two arguments: kitSku inspectionID");
        }
        String kitSku = action.getArgs()[0];
        String inspectionID = action.getArgs()[1];
        MessageType msg = new MessageType();
        msg.setMessage("inspect-kit " + kitSku);
        msg.setCommandID(incrementAndGetCommandId());
        out.add(msg);

        //-- inspect-kit takes an sku as argument
        //-- We want to identify which was just built
        //-- To do so, we use the poses stored in PlacePartSlotPoseList and
        //-- we look for the kit tray in the database for which one of the slots
        //-- has at least one pose in the list
        if (null != PlacePartSlotPoseList) {
            if (null == correctPartsTray) {

                correctPartsTray = findCorrectKitTray(kitSku);
            }
            if (null != correctPartsTray) {
                EmptySlotSet = new HashSet<Slot>();
                int numberOfPartsInKit = 0;

                System.out.println("\n\n---Inspecting kit tray " + correctPartsTray.getPartsTrayName());
                int partDesignPartCount = getPartDesignPartCount(kitSku);

                //-- replace all _pt from TakenPartList with _kt
                //-- Get all part that contains "in_kt" in their names
                //-- from the database
                for (int i = 0; i < TakenPartList.size(); i++) {
                    String part_in_pt = TakenPartList.get(i);
                    String tmpPartName = part_in_pt.replace("in_pt", "in_kt");
                    int indexLastUnderscore = tmpPartName.lastIndexOf("_");
                    if(indexLastUnderscore < 0) {
                        throw new IllegalStateException("TakenPartList="+TakenPartList+" contains invalid tmpPartName="+tmpPartName+" from part_in_pt="+part_in_pt);
                    }
                    String part_in_kt = tmpPartName.substring(0, indexLastUnderscore);
                    TakenPartList.set(i, part_in_kt);
                }

                //-- Get all the slots for the current parts tray
                List<Slot> slotList = correctPartsTray.getSlotList();
                for (int j = 0; j < slotList.size(); j++) {
                    Slot slot = slotList.get(j);
                    double slotx = slot.getSlotPose().getPoint().getX();
                    double sloty = slot.getSlotPose().getPoint().getY();
                    //System.out.println(slot.getSlotName() + ":(" + x_offset + "," + y_offset + ")");
                    System.out.println("++++++ " + slot.getSlotName() + ":(" + slotx + "," + sloty + ")");

                    //-- we want to filter out from TakenPartList parts that
                    //-- do not match slot part sku
                    //-- e.g., In TakenPartList keep only parts that contain part_large_gear
                    //-- if part sku for this slot is sku_part_large_gear
                    String partSKU = slot.getPartSKU();
                    if (partSKU.startsWith("sku_")) {
                        partSKU = partSKU.substring(4).concat("_in_kt");
                    }
                    if (checkPartTypeInSlot(partSKU, slot) == 1) {
                        numberOfPartsInKit++;
                    } else {
                        EmptySlotSet.add(slot);
                    }
                }
                if (!EmptySlotSet.isEmpty()) {
                    kitInspectionJInternalFrame.setKitImage(getKitResultImage(EmptySlotSet));
                } else {
                    kitInspectionJInternalFrame.setKitImage("complete");
                }
                setCorrectKitImage();
                if (numberOfPartsInKit == partDesignPartCount) {
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #7ef904\">&nbsp;&nbsp;The kit is complete</h3><br>");
                } else {
                    //if (part_in_kt_found) {
                    TakenPartList.clear();
                    int nbofmissingparts = partDesignPartCount - numberOfPartsInKit;
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"background-color: #ffb5b5; color: #ffffff\">&nbsp;&nbsp;The kit is missing " + nbofmissingparts + " part(s)</h3>");
                    for (Slot s : EmptySlotSet) {
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;Slot " + s.getSlotName() + " is missing a part of type " + s.getPartSKU() + "<br>");
                    }
                    if (nbofmissingparts == 1) {
                        kitInspectionJInternalFrame.getKitTitleLabel().setText("Missing " + nbofmissingparts + " part. Getting the new part.");
                    } else {
                        kitInspectionJInternalFrame.getKitTitleLabel().setText("Missing " + nbofmissingparts + " parts. Getting the new parts.");
                    }
                    try {
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;Recovering from failures</h2>");
                    } catch (BadLocationException ex) {
                        Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Map<String, List<String>> partSkuMap = new HashMap();

                    //-- Build a map where the key is the part sku for a slot
                    //-- and the value is an arraylist of part_in_pt
                    for (Slot s : EmptySlotSet) {

                        List<String> allPartsInPt = new ArrayList();
                        String partSKU = s.getPartSKU();
                        if (partSKU.startsWith("sku_")) {
                            partSKU = partSKU.substring(4).concat("_in_pt");
                        }
                        //-- Querying the database 20 times for part_in_pt
                        for (int i = 0; i < 20; i++) {
                            if (allPartsInPt.isEmpty()) {
                                allPartsInPt = getAllPartsInPt(partSKU);
                            }
                        }
                        partSkuMap.put(partSKU, allPartsInPt);
                    }

                    if (partSkuMap.size() > 0) {
                        for (Slot s : EmptySlotSet) {
                            String partSKU = s.getPartSKU();
                            if (partSKU.startsWith("sku_")) {
                                partSKU = partSKU.substring(4).concat("_in_pt");
                            }
                            kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;Getting a list of part_in_pt for sku " + partSKU + "<br>");

                            //-- get list of part_in_pt based on the part sku
                            List<String> listOfParts = partSkuMap.get(partSKU);
                            //-- get the first element in this list and then remove it from the list
                            if (listOfParts.size() > 0) {
                                String partInPt = listOfParts.get(0);
                                //--remove the first element
                                listOfParts.remove(0);
                                //-- update the list in the map with the modified list
                                partSkuMap.put(partSKU, listOfParts);
                                //-- perform pick-and-place actions
                                takePartRecovery(partInPt, out);
                                PddlAction takepartrecoveryaction = PddlAction.parse("(place-part " + s.getSlotName() + ")");
                                placePartRecovery(takepartrecoveryaction, s, out);
                            } else {
                                // kitInspectionJInternalFrame.addToInspectionResultJTextPane("Could not find part_in_pt for sku " + partSKU + " from the database<b
                                kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Could not find part_in_pt for sku " + partSKU + " from the database</h3><br>");
                                kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Recovery Aborted</h3><br>");
                            }
                        }
                    } else {
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Could not find parts in_pt from the database</h3><br>");
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Recovery Aborted</h3><br>");
                    }
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane("<br>");
                }
            } else {
                kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;The system could not identify the kit tray that was built</h3><br>");
                kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Inspection Aborted</h3><br>");
            }
        }
        if (inspectionID.contains("0")) {
            PlacePartSlotPoseList.clear();
            PlacePartSlotPoseList = null;
            correctPartsTray = null;
        }
    }

    private double getVisionToDBRotationOffset() {
        if (null == this.aprsJFrame) {
            throw new IllegalStateException("null == this.aprsJFrame");
        }
        return this.aprsJFrame.getVisionToDBRotationOffset();
    }

    /**
     * Function that finds the correct kit tray from the database using the sku
     * kitSku
     *
     * @param kitSku sku of kit
     * @return tray from database
     * @throws SQLException if query fails
     */
    private PartsTray findCorrectKitTray(String kitSku) throws SQLException {
        PartsTray correctPartsTray = null;

        List<PartsTray> dpuPartsTrayList = DatabasePoseUpdater.partsTrayList;
        //-- retrieveing from the database all the parts trays that have the sku kitSku
        List<PartsTray> partsTraysList = getPartsTrays(kitSku);

        /*
        System.out.println("--Checking parts trays");
        for (int i = 0; i < partsTraysList.size(); i++) {
            PartsTray partsTray = partsTraysList.get(i);
            System.out.println("--Parts tray: " + partsTray.getPartsTrayName());
        }
         */
        for (int i = 0; i < partsTraysList.size(); i++) {

            if (null == correctPartsTray) {
                PartsTray partsTray = partsTraysList.get(i);

                //-- getting the pose for the parts tray 
                PoseType partsTrayPose = qs.getPose(partsTray.getPartsTrayName());

                partsTrayPose = correctPose(partsTrayPose);
                System.out.println("--Checking parts tray [" + partsTray.getPartsTrayName() + "] :(" + partsTrayPose.getPoint().getX() + "," + partsTrayPose.getPoint().getY() + ")");
                partsTray.setpartsTrayPose(partsTrayPose);
                double partsTrayPoseX = partsTrayPose.getPoint().getX();
                double partsTrayPoseY = partsTrayPose.getPoint().getY();

                double rotation = 0;
                //-- Read partsTrayList
                //-- Assign rotation to myPartsTray by comparing poses from vision vs database
                //System.out.print("--Assigning proper rotation: ");
                System.out.println("--Comparing with other parts trays from vision");
                for (int c = 0; c < dpuPartsTrayList.size(); c++) {
                    PartsTray pt = dpuPartsTrayList.get(c);
                    double ptX = pt.getX();
                    double ptY = pt.getY();
                    System.out.println("    Parts tray:(" + pt.getX() + "," + pt.getY() + ")");
                    System.out.println("    Rotation:(" + pt.getRotation() + ")");
                    //-- Check if X for parts trays are close enough
                    //double diffX = Math.abs(partsTrayPoseX - ptX);
                    //System.out.println("diffX= "+diffX);
                    /*
                if (diffX < 1E-7) {
                    //-- Check if Y for parts trays are close enough
                    double diffY = Math.abs(partsTrayPoseY - ptY);
                    //System.out.println("diffY= "+diffY);
                    if (diffY < 1E-7) {
                        rotation=pt.getRotation();
                        partsTray.setRotation(pt.getRotation());
                    }
                }
                     */

                    double distance = Math.hypot(partsTrayPoseX - ptX, partsTrayPoseY - ptY);
                    System.out.println("    Distance = " + distance + "\n");
                    if (distance < 2) {
                        rotation = pt.getRotation();
                        partsTray.setRotation(rotation);
                    }
                }

                //rotation = partsTray.getRotation();
                //System.out.println(rotation);
                //-- retrieve the rotationOffset
                double rotationOffset = getVisionToDBRotationOffset();

                System.out.println("rotationOffset " + rotationOffset);
                System.out.println("rotation " + partsTray.getRotation());
                //-- compute the angle
                double angle = normAngle(partsTray.getRotation() + rotationOffset);

                //-- Get list of slots for this parts tray
                System.out.println("--Checking slots");
                List<Slot> slotList = partsTray.getSlotList();
                int count = 0;
                for (int j = 0; j < slotList.size(); j++) {
                    Slot slot = slotList.get(j);
                    double x_offset = slot.getX_OFFSET() * 1000;
                    double y_offset = slot.getY_OFFSET() * 1000;
                    double slotX = partsTrayPoseX + x_offset * Math.cos(angle) - y_offset * Math.sin(angle);
                    double slotY = partsTrayPoseY + x_offset * Math.sin(angle) + y_offset * Math.cos(angle);
                    double slotZ = -146; // FIXME : hard-coded Z offset
                    PointType slotPoint = new PointType();
                    slotPoint.setX(slotX);
                    slotPoint.setY(slotY);
                    slotPoint.setZ(slotZ);
                    PoseType slotPose = new PoseType();
                    slotPose.setPoint(slotPoint);
                    slot.setSlotPose(slotPose);

                    System.out.println("+++ " + slot.getSlotName() + ":(" + slotX + "," + slotY + ")");
                    //-- compare this slot pose with the ones in PlacePartSlotPoseList
                    for (int k = 0; k < PlacePartSlotPoseList.size(); k++) {
                        PoseType pose = PlacePartSlotPoseList.get(k);
                        System.out.println("      placepartpose :(" + pose.getPoint().getX() + "," + pose.getPoint().getY() + ")");
                        double distance = Math.hypot(pose.getPoint().getX() - slotX, pose.getPoint().getY() - slotY);
                        System.out.println("         Distance = " + distance + "\n");
                        if (distance < 2.0) {
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    correctPartsTray = partsTray;
                    System.out.println("Found partstray: " + correctPartsTray.getPartsTrayName());

                }
            }
        }
        return correctPartsTray;
    }

    public void updateInspectionFrame() {

    }

    private String getKitResultImage(Set<Slot> list) {
        String kitResultImage = "";
        List<Integer> idList = new ArrayList();
        for (Slot slot : list) {
            int id = slot.getID();
            idList.add(id);
        }
        if (!idList.isEmpty()) {
            Collections.sort(idList);
        } else {
            System.out.println("idList is empty");
        }
        for (Integer s : idList) {
            kitResultImage += s;
        }

        return kitResultImage;
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

    private int checkPartTypeInSlot(String partInKt, Slot slot) throws SQLException, BadLocationException {
        int nbOfOccupiedSlots = 0;
        int counter = 0;
        List<String> allPartsInKt = new ArrayList();
        //-- queries the database 10 times to make sure we are not missing some part_in_kt
        for (int i = 0; i < 20; i++) {
            if (allPartsInKt.isEmpty()) {

                allPartsInKt = getAllPartsInKt(partInKt);
            }
        }
        if (!allPartsInKt.isEmpty()) {
            for (int i = 0; i < allPartsInKt.size(); i++) {
                String newPartInKt = allPartsInKt.get(i);
                System.out.print("--------- " + newPartInKt);
                if (checkPartInSlot(newPartInKt, slot)) {
                    System.out.println("--------- Located in slot");
                    nbOfOccupiedSlots++;
                } else {
                    System.out.println("--------- Not located in slot");
                }
            }
            //part_in_kt_found=true;
        } else {
            kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;No part_in_kt of type " + partInKt + " was found in the database<br>");
            //part_in_kt_found=false;
        }
        return nbOfOccupiedSlots;
    }

    private Boolean checkPartInSlot(String partName, Slot slot) throws SQLException {
        Boolean isPartInSlot = false;
        PoseType posePart = getPose(partName);
        posePart = correctPose(posePart);
        double partX = posePart.getPoint().getX();
        double partY = posePart.getPoint().getY();
        double slotX = slot.getSlotPose().getPoint().getX();
        double slotY = slot.getSlotPose().getPoint().getY();
        System.out.println(":(" + partX + "," + partY + ")");
        double distance = Math.hypot(partX - slotX, partY - slotY);
        System.out.print("--------- Distance = " + distance);
        // compare finalres with a specified tolerance value of 6.5 mm
        double threshold = 20;
        if (distance < threshold) {
            isPartInSlot = true;
            // System.out.println("----- Part " + partName + " : (" + partX + "," + partY + ")");
            // System.out.println("----- Slot " + slot.getSlotName() + " : (" + slotX + "," + slotY + ")");
            // System.out.println("----- Distance between part and slot = " + dist);
        }
        return isPartInSlot;
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

    private boolean skipMissingParts = false;

    private boolean getForceFakeTakeFlag() {
        if(null != parentPddlExecutorJPanel) {
            return this.parentPddlExecutorJPanel.getForceFakeTakeFlag();
        }
        return false;
    }

    private void setFakeTakePart(boolean _newValue) {
        if(null != parentPddlExecutorJPanel) {
            this.parentPddlExecutorJPanel.setForceFakeTakeFlag(_newValue);
        }
    }

    /**
     * Add commands to the list that will take a given part.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @throws IllegalStateException if database is not connected
     * @throws SQLException if database query fails
     */
    public void takePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];
        MessageType msg = new MessageType();
        msg.setMessage("take-part " + partName);
        msg.setCommandID(incrementAndGetCommandId());
        out.add(msg);

        if (null != kitInspectionJInternalFrame) {
            kitInspectionJInternalFrame.setKitImage("init");
            kitInspectionJInternalFrame.getKitTitleLabel().setText("Building kit");
            setCorrectKitImage();
        }

        PoseType pose = getPose(partName);
        if (takeSnapshots) {
            try {
                takeSimViewSnapshot(File.createTempFile(getRunPrefix() + "-take-part-" + partName + "-", ".PNG"), pose, partName);
            } catch (IOException ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null == pose) {
            if (skipMissingParts) {
                lastTakenPart = null;
                return;
            } else {
                throw new IllegalStateException("getPose(" + partName + ") returned null");
            }
        }
        pose = correctPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        takePartByPose(out, pose);
        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, (CrclCommandWrapper ccw) -> {
            System.out.println(markerMsg + " at " + new Date());
            if (null != kitInspectionJInternalFrame) {
                try {
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
                } catch (BadLocationException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        lastTakenPart = partName;
        //inspectionList.add(partName);
        TakenPartList.add(partName);
    }

    /**
     * Add commands to the list that will go through the motions to take a given
     * part but skip closing the gripper.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @throws IllegalStateException if database is not connected
     * @throws SQLException if database query fails
     */
    public void fakeTakePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];
        MessageType msg = new MessageType();
        msg.setMessage("take-part " + partName);
        msg.setCommandID(incrementAndGetCommandId());
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
            if (null != kitInspectionJInternalFrame) {
                try {
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
                } catch (BadLocationException ex) {
                    Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        lastTakenPart = partName;
        TakenPartList.add(partName);
    }

    public void takePartRecovery(String partName, List<MiddleCommandType> out) throws SQLException, BadLocationException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }

        checkSettings();
        MessageType msg = new MessageType();
        msg.setMessage("take-part-recovery " + partName);
        msg.setCommandID(incrementAndGetCommandId());
        out.add(msg);

        PoseType pose = getPose(partName);
        if (takeSnapshots) {
            try {
                takeSimViewSnapshot(File.createTempFile(getRunPrefix() + "-take-part-recovery-" + partName + "-", ".PNG"), pose, partName);
            } catch (IOException ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null == pose) {
            if (skipMissingParts) {
                lastTakenPart = null;
                return;
            } else {
                throw new IllegalStateException("getPose(" + partName + ") returned null");
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
            try {
                kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
            } catch (BadLocationException ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        lastTakenPart = partName;
        TakenPartList.add(partName);
    }

    /**
     * Get the pose associated with a given name.
     *
     * The name could refer to a part, tray or slot. Poses are also cached until
     * a look-for-parts action clears the cache.
     *
     * @param posename name of position to get
     * @return pose of part,tray or slot
     *
     * @throws SQLException if query fails.
     */
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
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
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

    public List<String> getAllPartsInPt(String name) throws SQLException {
        List<String> partsInPtList = new ArrayList<>(qs.getAllPartsInPt(name));

        return partsInPtList;
    }

    /**
     * Add commands to the list that will test a given part position by opening
     * the gripper and moving to that position but not actually taking the part.
     *
     * @param cmds list of commands to append to
     * @param pose pose to test
     */
    public void testPartPositionByPose(List<MiddleCommandType> cmds, PoseType pose) {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ() + takeZOffset);

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

    /**
     * Add commands to the list that will take a part at a given pose.
     *
     * @param cmds list of commands to append to
     * @param pose pose where part is expected
     */
    public void takePartByPose(List<MiddleCommandType> cmds, PoseType pose) {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ() + takeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true);

        addSettleDwell(cmds);

        addOptionalCloseGripper(cmds, (CrclCommandWrapper ccw) -> {
            if(getForceFakeTakeFlag()) {
                SetEndEffectorType seeCmd = (SetEndEffectorType) ccw.getWrappedCommand();
                seeCmd.setSetting(1.0);
                setFakeTakePart(false);
            }
        });

        addSettleDwell(cmds);

        addMoveTo(cmds, approachPose, true);

        addSettleDwell(cmds);
    }

    /**
     * Add commands to the list that will go through the motions to take a part
     * at a given pose but not close the gripper to actually take the part.
     *
     * @param cmds list of commands to append to
     * @param pose pose where part is expected
     */
    public void fakeTakePartByPose(List<MiddleCommandType> cmds, PoseType pose) {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ() + takeZOffset);

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

    private double rotSpeed = 30.0;

    /**
     * Get the value of rotSpeed
     *
     * @return the value of rotSpeed
     */
    public double getRotSpeed() {
        return rotSpeed;
    }

    /**
     * Set the value of rotSpeed
     *
     * @param rotSpeed new value of rotSpeed
     */
    public void setRotSpeed(double rotSpeed) {
        this.rotSpeed = rotSpeed;
    }

    private void checkSettings() {
        String rpyString = options.get("rpy");
        if (null != rpyString && rpyString.length() > 0) {
            try {
                String rpyFields[] = rpyString.split("[, \t]+");
                if (rpyFields.length == 3) {
                    rpy = new PmRpy();
                    rpy.r = Math.toRadians(Double.parseDouble(rpyFields[0]));
                    rpy.p = Math.toRadians(Double.parseDouble(rpyFields[1]));
                    rpy.y = Math.toRadians(Double.parseDouble(rpyFields[2]));
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
                approachZOffset = Double.parseDouble(approachZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String placeZOffsetString = options.get("placeZOffset");
        if (null != placeZOffsetString && placeZOffsetString.length() > 0) {
            try {
                placeZOffset = Double.parseDouble(placeZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String takeZOffsetString = options.get("takeZOffset");
        if (null != takeZOffsetString && takeZOffsetString.length() > 0) {
            try {
                takeZOffset = Double.parseDouble(takeZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String settleDwellTimeString = options.get("settleDwellTime");
        if (null != settleDwellTimeString && settleDwellTimeString.length() > 0) {
            try {
                settleDwellTime = Double.parseDouble(settleDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String lookDwellTimeString = options.get("lookDwellTime");
        if (null != lookDwellTimeString && lookDwellTimeString.length() > 0) {
            try {
                lookDwellTime = Double.parseDouble(lookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String firstLookDwellTimeString = options.get("firstLookDwellTime");
        if (null != firstLookDwellTimeString && firstLookDwellTimeString.length() > 0) {
            try {
                firstLookDwellTime = Double.parseDouble(firstLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String lastLookDwellTimeString = options.get("lastLookDwellTime");
        if (null != lastLookDwellTimeString && lastLookDwellTimeString.length() > 0) {
            try {
                lastLookDwellTime = Double.parseDouble(lastLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String fastTransSpeedString = options.get("fastTransSpeed");
        if (null != fastTransSpeedString && fastTransSpeedString.length() > 0) {
            try {
                fastTransSpeed = Double.parseDouble(fastTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String rotSpeedString = options.get("rotSpeed");
        if (null != rotSpeedString && rotSpeedString.length() > 0) {
            try {
                rotSpeed = Double.parseDouble(rotSpeedString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String jointSpeedString = options.get("jointSpeed");
        if (null != jointSpeedString && jointSpeedString.length() > 0) {
            try {
                jointSpeed = Double.parseDouble(jointSpeedString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String jointAccelString = options.get("jointAccel");
        if (null != jointAccelString && jointAccelString.length() > 0) {
            try {
                jointAccel = Double.parseDouble(jointAccelString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String slowTransSpeedString = options.get("slowTransSpeed");
        if (null != slowTransSpeedString && slowTransSpeedString.length() > 0) {
            try {
                slowTransSpeed = Double.parseDouble(slowTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }
        String takePartArgIndexString = options.get("takePartArgIndex");
        if (null != takePartArgIndexString && takePartArgIndexString.length() > 0) {
            this.takePartArgIndex = Integer.parseInt(takePartArgIndexString);
        }
        String placePartSlotArgIndexString = options.get("placePartSlotArgIndex");
        if (null != placePartSlotArgIndexString && placePartSlotArgIndexString.length() > 0) {
            this.placePartSlotArgIndex = Integer.parseInt(placePartSlotArgIndexString);
        }
        String takeSnapshotsString = options.get("takeSnapshots");
        if (null != takeSnapshotsString && takeSnapshotsString.length() > 0) {
            takeSnapshots = Boolean.valueOf(takeSnapshotsString);
        }
        String doInspectKitString = options.get("doInspectKit");
        if (null != doInspectKitString && doInspectKitString.length() > 0) {
            doInspectKit = Boolean.valueOf(doInspectKitString);
        }
        String requireNewPosesString = options.get("requireNewPoses");
        if (null != requireNewPosesString && requireNewPosesString.length() > 0) {
            requireNewPoses = Boolean.valueOf(requireNewPosesString);
        }
        String skipMissingPartsString = options.get("skipMissingParts");
        if (null != skipMissingPartsString && skipMissingPartsString.length() > 0) {
            skipMissingParts = Boolean.valueOf(skipMissingPartsString);
        }
        String visionCycleNewDiffThresholdString = options.get("visionCycleNewDiffThreshold");
        if (null != visionCycleNewDiffThresholdString && visionCycleNewDiffThresholdString.length() > 0) {
            visionCycleNewDiffThreshold = Integer.parseInt(visionCycleNewDiffThresholdString);
        }
    }

    private void addOpenGripper(List<MiddleCommandType> cmds) {
        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
        openGripperCmd.setCommandID(incrementAndGetCommandId());
        openGripperCmd.setSetting(1.0);
        cmds.add(openGripperCmd);
    }

    private void addOptionalCloseGripper(List<MiddleCommandType> cmds,CRCLCommandWrapperConsumer cb) {
        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        closeGrippeerCmd.setSetting(0.0);
        addOptionalCommand(closeGrippeerCmd, cmds, cb);
    }
    
    private void addCloseGripper(List<MiddleCommandType> cmds) {
        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        closeGrippeerCmd.setCommandID(incrementAndGetCommandId());
        closeGrippeerCmd.setSetting(0.0);
        cmds.add(closeGrippeerCmd);
    }

    private void addMoveTo(List<MiddleCommandType> cmds, PoseType poseAbove, boolean straight) {
        MoveToType moveAboveCmd = new MoveToType();
        moveAboveCmd.setCommandID(incrementAndGetCommandId());
        moveAboveCmd.setEndPosition(poseAbove);
        moveAboveCmd.setMoveStraight(straight);
        cmds.add(moveAboveCmd);
    }

    private void addSetSlowSpeed(List<MiddleCommandType> cmds) {
        SetTransSpeedType stst = new SetTransSpeedType();
        stst.setCommandID(incrementAndGetCommandId());
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(slowTransSpeed);
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private boolean rotSpeedSet = false;

    private void addSetFastSpeed(List<MiddleCommandType> cmds) {

        if (!rotSpeedSet) {
            SetRotSpeedType srs = new SetRotSpeedType();
            RotSpeedAbsoluteType rsa = new RotSpeedAbsoluteType();
            rsa.setSetting(rotSpeed);
            srs.setCommandID(incrementAndGetCommandId());
            srs.setRotSpeed(rsa);
            cmds.add(srs);
            rotSpeedSet = true;
        }

        SetTransSpeedType stst = new SetTransSpeedType();
        stst.setCommandID(incrementAndGetCommandId());
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(fastTransSpeed);
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private boolean unitsSet = false;

    private void addSetUnits(List<MiddleCommandType> cmds) {
        if (!unitsSet) {
            SetLengthUnitsType slu = new SetLengthUnitsType();
            slu.setUnitName(LengthUnitEnumType.MILLIMETER);
            slu.setCommandID(incrementAndGetCommandId());
            cmds.add(slu);

            SetAngleUnitsType sau = new SetAngleUnitsType();
            sau.setUnitName(AngleUnitEnumType.DEGREE);
            sau.setCommandID(incrementAndGetCommandId());
            cmds.add(sau);
            unitsSet = true;
        }
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
            pose.setPoint(point(Double.parseDouble(lookForXYZFields[0]), Double.parseDouble(lookForXYZFields[1]), Double.parseDouble(lookForXYZFields[2])));
            pose.setXAxis(xAxis);
            pose.setZAxis(zAxis);
            addMoveTo(out, pose, false);
        } else {
            addJointMove(out, lookForJointsString);
        }
    }

    private double jointSpeed = 5.0;

    /**
     * Get the value of jointSpeed
     *
     * @return the value of jointSpeed
     */
    public double getJointSpeed() {
        return jointSpeed;
    }

    /**
     * Set the value of jointSpeed
     *
     * @param jointSpeed new value of jointSpeed
     */
    public void setJointSpeed(double jointSpeed) {
        this.jointSpeed = jointSpeed;
    }

    private double jointAccel = 100.0;

    /**
     * Get the value of jointAccel
     *
     * @return the value of jointAccel
     */
    public double getJointAccel() {
        return jointAccel;
    }

    /**
     * Set the value of jointAccel
     *
     * @param jointAccel new value of jointAccel
     */
    public void setJointAccel(double jointAccel) {
        this.jointAccel = jointAccel;
    }

    private void addJointMove(List<MiddleCommandType> out, String jointVals) {
        ActuateJointsType ajCmd = new ActuateJointsType();
        ajCmd.setCommandID(out.size() + 2);
        ajCmd.getActuateJoint().clear();
        String jointPosStrings[] = jointVals.split("[,]+");
        for (int i = 0; i < jointPosStrings.length; i++) {
            ActuateJointType aj = new ActuateJointType();
            JointSpeedAccelType jsa = new JointSpeedAccelType();
            jsa.setJointAccel(jointAccel);
            jsa.setJointSpeed(jointSpeed);
            aj.setJointDetails(jsa);
            aj.setJointNumber(i + 1);
            aj.setJointPosition(Double.parseDouble(jointPosStrings[i]));
            ajCmd.getActuateJoint().add(aj);
        }
        out.add(ajCmd);
    }

    public void clearPoseCache() {
        poseCache.clear();
    }

    public Map<String, PoseType> getPoseCache() {
        return Collections.unmodifiableMap(poseCache);
    }

    private void lookForParts(PddlAction action, List<MiddleCommandType> out, boolean firstAction, boolean lastAction) throws IllegalStateException, SQLException {

        checkSettings();
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        if (null == kitInspectionJInternalFrame) {
            kitInspectionJInternalFrame = aprsJFrame.getKitInspectionJInternalFrame();
        }
        addMoveToLookForPosition(out);

        if (firstAction) {
            addFirstLookDwell(out);
        } else if (lastAction) {
            addLastLookDwell(out);
        } else {
            addLookDwell(out);
        }

        addTakeSimViewSnapshot(out, "-look-for-parts-", null, "");
        addMarkerCommand(out, "clear pose cache", x -> this.clearPoseCache());
        if (action.getArgs().length == 1) {
            if (action.getArgs()[0].startsWith("1")) {
                addMarkerCommand(out, "", x -> {
                    kitInspectionJInternalFrame.getKitTitleLabel().setText("Inspecting kit");
                    try {
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR:" + messageColorH3 + "\">&nbsp;&nbsp;Inspecting kit</h2>");
                    } catch (BadLocationException ex) {
                        Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

            } else if (action.getArgs()[0].startsWith("0")) {
                addMarkerCommand(out, "", x -> {
                    kitInspectionJInternalFrame.getKitTitleLabel().setText("Building kit");
                    try {
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;Building kit</h2>");
                    } catch (BadLocationException ex) {
                        Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            } else if (action.getArgs()[0].startsWith("2")) {
                addMarkerCommand(out, "", x -> {
                    kitInspectionJInternalFrame.getKitTitleLabel().setText("All Tasks Completed");
                    try {
                        kitInspectionJInternalFrame.addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;All tasks completed</h2>");
                    } catch (BadLocationException ex) {
                        Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }

        }
    }

//    private void addOpenGripper(List<MiddleCommandType> out, PoseType pose) {
//        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
//        openGripperCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
//        openGripperCmd.setSetting(double.ONE);
//    }
    private void addLookDwell(List<MiddleCommandType> out) {
        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(out.size() + 2);
        dwellCmd.setDwellTime(lookDwellTime);
        out.add(dwellCmd);
    }

    /**
     * Holds information associated with a place part action
     */
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

    /**
     * Register a consumer to be notified when parts are placed.
     *
     * @param consumer consumer to be notified
     */
    public void addPlacePartConsumer(Consumer<PlacePartInfo> consumer) {
        placePartConsumers.add(consumer);
    }

    /**
     * Remove a previously registered consumer.
     *
     * @param consumer consumer to be removed
     */
    public void removePlacePartConsumer(Consumer<PlacePartInfo> consumer) {
        placePartConsumers.remove(consumer);
    }

    /**
     * Notify all consumers that a place-part action has been executed.
     *
     * @param ppi info to be passed to consumers
     */
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

    /**
     * Add commands to the place whatever part is currently in the gripper in a
     * given slot.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @throws IllegalStateException if database is not connected
     * @throws SQLException if database query fails
     */
    private void placePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        if (null == PlacePartSlotPoseList) {
            PlacePartSlotPoseList = new ArrayList();
        }
        checkSettings();
        if (skipMissingParts && lastTakenPart == null) {
            return;
        }
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
        if (pose == null) {
            throw new IllegalStateException("getPose(" + slotName + ") returned null");
        }

        pose = correctPose(pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        PlacePartSlotPoseList.add(pose);
        placePartByPose(out, pose);
        final PlacePartInfo ppi = new PlacePartInfo(action, lastIndex, out.size());
        addMarkerCommand(out, msg,
                ((CrclCommandWrapper wrapper) -> {
                    try {
                        if (null != kitInspectionJInternalFrame) {
                            kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                        }
                        System.out.println(msg + " completed at " + new Date());
                        ppi.setWrapper(wrapper);
                        notifyPlacePartConsumers(ppi);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }));
    }

    private void placePartRecovery(PddlAction action, Slot slot, List<MiddleCommandType> out) throws IllegalStateException, SQLException, BadLocationException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        if (null == PlacePartSlotPoseList) {
            PlacePartSlotPoseList = new ArrayList();
        }
        checkSettings();
        String slotName = action.getArgs()[0];
        PoseType pose = slot.getSlotPose();

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
        PlacePartSlotPoseList.add(pose);
        placePartByPose(out, pose);
        final PlacePartInfo ppi = new PlacePartInfo(action, lastIndex, out.size());
        addMarkerCommand(out, msg,
                ((CrclCommandWrapper wrapper) -> {
                    System.out.println(msg + " completed at " + new Date());
                    ppi.setWrapper(wrapper);
                    notifyPlacePartConsumers(ppi);
                    if (null != kitInspectionJInternalFrame) {
                        try {
                            kitInspectionJInternalFrame.addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                        } catch (BadLocationException ex) {
                            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
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

    /**
     * Add commands to the place whatever part is currently in the gripper in a
     * slot at the given pose.
     *
     * @param cmds list of commands to append to
     * @param pose pose where part will be placed.
     */
    public void placePartByPose(List<MiddleCommandType> cmds, PoseType pose) {

        checkSettings();

        PoseType poseAbove = CRCLPosemath.copy(pose);
        //System.out.println("Z= " + pose.getPoint().getZ());
        poseAbove.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);

        PoseType placePose = CRCLPosemath.copy(pose);
        placePose.getPoint().setZ(pose.getPoint().getZ() + placeZOffset);

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
        dwellCmd.setCommandID(incrementAndGetCommandId());
        dwellCmd.setDwellTime(settleDwellTime);
        cmds.add(dwellCmd);
    }

    private void addFirstLookDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(incrementAndGetCommandId());
        dwellCmd.setDwellTime(firstLookDwellTime);
        cmds.add(dwellCmd);
    }

    private void addLastLookDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        dwellCmd.setCommandID(incrementAndGetCommandId());
        dwellCmd.setDwellTime(lastLookDwellTime);
        cmds.add(dwellCmd);
    }

    private void addMarkerCommand(List<MiddleCommandType> cmds, String message, CRCLCommandWrapperConsumer cb) {
        MessageType messageCmd = new MessageType();
        messageCmd.setMessage(message);
        messageCmd.setCommandID(incrementAndGetCommandId());
        CrclCommandWrapper wrapper = CrclCommandWrapper.wrapWithOnDone(messageCmd, cb);
        cmds.add(wrapper);
    }

    private void addOptionalCommand(MiddleCommandType optCmd, List<MiddleCommandType> cmds, CRCLCommandWrapperConsumer cb) {
        optCmd.setCommandID(incrementAndGetCommandId());
        CrclCommandWrapper wrapper = CrclCommandWrapper.wrapWithOnStart(optCmd, cb);
        cmds.add(wrapper);
    }

    @Override
    public void accept(DbSetup setup) {
        this.setDbSetup(setup);
    }

    /**
     * Class to hold information about a given action to be passed to callback
     * methods when the action is executed.
     */
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

    /**
     * Register a listener to be notified when any action is executed.
     *
     * @param listener listner to be added.
     */
    public void addActionCompletedListener(Consumer<ActionCallbackInfo> listener) {
        actionCompletedListeners.add(listener);
    }

    /**
     * Remove a previously registered listener
     *
     * @param listener listener to be removed.
     */
    public void removeActionCompletedListener(Consumer<ActionCallbackInfo> listener) {
        actionCompletedListeners.remove(listener);
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
        super.finalize();
    }

}
