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

import aprs.framework.database.Slot;
import aprs.framework.database.PartsTray;
import aprs.framework.AprsJFrame;
import aprs.framework.PddlAction;
import aprs.framework.Utils;
import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupJPanel;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbType;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.QuerySet;
import aprs.framework.database.Tray;
import aprs.framework.kitinspection.KitInspectionJInternalFrame;
import aprs.framework.optaplanner.OpDisplayJPanel;
import aprs.framework.optaplanner.actionmodel.OpAction;
import aprs.framework.optaplanner.actionmodel.OpActionPlan;
import aprs.framework.optaplanner.actionmodel.OpActionType;
import aprs.framework.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import crcl.base.ActuateJointType;
import crcl.base.ActuateJointsType;
import crcl.base.AngleUnitEnumType;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLStatusType;
import crcl.base.DwellType;
import crcl.base.JointSpeedAccelType;
import crcl.base.JointStatusType;
import crcl.base.JointStatusesType;
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
import crcl.ui.XFuture;
import crcl.ui.client.PendantClientInner;
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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.awt.Color;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.BadLocationException;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.Collection;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;

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

    public int getCrclNumber() {
        return crclNumber.get();
    }

    private static String posNameToType(String partName) {
        if (partName.startsWith("empty_slot")) {
            int for_index = partName.indexOf("_for_");
            if (for_index > 0) {
                partName = partName.substring(for_index + 5);
            }
        }
        if (partName.startsWith("part_")) {
            partName = partName.substring(5);
        }
        int in_index = partName.indexOf("_in_");
        if (in_index > 0) {
            return partName.substring(0, in_index);
        }
        return partName;
    }

    public List<PddlAction> opActionsToPddlActions(List<? extends OpAction> listIn, int start) {
        List<PddlAction> ret = new ArrayList<>();
        for (int i = start; i < listIn.size(); i++) {
            OpAction opa = listIn.get(i);
            String name = opa.getName();
            if ("start".equals(name)) {
                continue;
            }
            int dindex = name.indexOf("-[");
            if (dindex > 0) {
                String type = name.substring(0, dindex);
                String args[] = name.substring(dindex + 2).split("[,{}\\-\\[\\]]+");
                ret.add(new PddlAction("", type, args, "" + opa.cost()));
            }
        }
        return ret;
    }

    private Solver<OpActionPlan> solver;

    /**
     * Get the value of solver
     *
     * @return the value of solver
     */
    public Solver<OpActionPlan> getSolver() {
        return solver;
    }

    /**
     * Set the value of solver
     *
     * @param solver new value of solver
     */
    public void setSolver(Solver<OpActionPlan> solver) {
        this.solver = solver;
    }

    public List<OpAction> pddlActionsToOpActions(List<? extends PddlAction> listIn, int start) throws SQLException {
        return pddlActionsToOpActions(listIn, start, null, null,null);
    }

    int skippedActions = 0;

    private List<OpAction> pddlActionsToOpActions(List<? extends PddlAction> listIn, int start, int endl[], List<OpAction> skippedOpActionsList,List<PddlAction> skippedPddlActionsList) throws SQLException {
        List<OpAction> ret = new ArrayList<>();
        boolean moveOccurred = false;
        PointType lookForPt = getLookForXYZ();
        ret.add(new OpAction("start", lookForPt.getX(), lookForPt.getY(), OpActionType.START, "NONE"));
        boolean skipNextPlace = false;
        skippedActions = 0;
        for (int i = start; i < listIn.size(); i++) {
            PddlAction pa = listIn.get(i);

            switch (pa.getType()) {
                case "take-part":
                case "fake-take-part":
                case "test-part-position":
                    String partName = pa.getArgs()[takePartArgIndex];
                    PoseType partPose = getPose(partName);
                    if (null == partPose) {
                        if (skipMissingParts) {
                            skippedActions++;
                            if(null != skippedPddlActionsList) {
                                skippedPddlActionsList.add(pa);
                            }
                            skipNextPlace = true;
                        } else {
                            throw new IllegalStateException("null == partPose: partName=" + partName + ",start=" + start + ",i=" + i + ",pa=" + pa);
                        }
                    } else {
                        PointType partPt = partPose.getPoint();
                        ret.add(new OpAction(pa.toString(), partPt.getX(), partPt.getY(), OpActionType.PICKUP, posNameToType(partName)));
                        if (!moveOccurred) {
                            if (null != endl && endl.length > 0) {
                                endl[0] = i;
                            }
                            moveOccurred = true;
                        }
                        skipNextPlace = false;
                    }
                    break;

                case "place-part":
                    String slotName = pa.getArgs()[placePartSlotArgIndex];
                    PoseType slotPose = getPose(slotName);
                    if (null == slotPose) {
                        if (skipMissingParts) {
                            skippedActions++;
                            if(null != skippedPddlActionsList) {
                                skippedPddlActionsList.add(pa);
                            }
                            skipNextPlace = true;
                        } else {
                            throw new IllegalStateException("null == slotPose: slotName=" + slotName + ",start=" + start + ",i=" + i + ",pa=" + pa);
                        }
                    } else {
                        PointType slotPt = slotPose.getPoint();
                        OpAction placePartOpAction = new OpAction(pa.toString(), slotPt.getX(), slotPt.getY(), OpActionType.DROPOFF, posNameToType(slotName));
                        if (!skipNextPlace) {
                            ret.add(placePartOpAction);
                            if (!moveOccurred) {
                                if (null != endl && endl.length > 0) {
                                    endl[0] = i;
                                }
                                moveOccurred = true;
                            }
                        } else {
                            skippedActions++;
                            if (null != skippedOpActionsList) {
                                skippedOpActionsList.add(placePartOpAction);
                            }
                            skipNextPlace = false;
                        }
                    }
                    break;

                case "look-for-part":
                case "look-for-parts":
                    if (true) {
                        if (null != endl && endl.length > 1) {
                            endl[1] = i;
                        }
                        return ret;
                    }
                    break;

                default:
                    break;
            }
        }
        if (null != endl && endl.length > 1) {
            endl[1] = listIn.size();
        }
        return ret;
    }

    private java.sql.Connection dbConnection;
    private DbSetup dbSetup;
    private boolean closeDbConnection = true;
    private QuerySet qs;
    private final List<String> TakenPartList = new ArrayList<>();
    private Set<Slot> EmptySlotSet;
    private List<PoseType> PlacePartSlotPoseList = null;
    private boolean takeSnapshots = false;
    private final AtomicInteger crclNumber = new AtomicInteger();
    private final ConcurrentMap<String, PoseType> poseCache = new ConcurrentHashMap<>();
    private volatile KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

    private volatile boolean pauseInsteadOfRecover;

    /**
     * Get the value of pauseInsteadOfRecover
     *
     * @return the value of pauseInsteadOfRecover
     */
    public boolean isPauseInsteadOfRecover() {
        return pauseInsteadOfRecover;
    }

    /**
     * Set the value of pauseInsteadOfRecover
     *
     * @param pauseInsteadOfRecover new value of pauseInsteadOfRecover
     */
    public void setPauseInsteadOfRecover(boolean pauseInsteadOfRecover) {
        this.pauseInsteadOfRecover = pauseInsteadOfRecover;
    }

    static private class KitToCheck {

        String name;
        Map<String, String> slotMap;
    }
    private final ConcurrentLinkedDeque<KitToCheck> kitsToCheck = new ConcurrentLinkedDeque<>();

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
            logger.log(Level.SEVERE, null, ex);
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
                    logger.log(Level.SEVERE, null, ex);
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
            logger.log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
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
     * @return future providing status on when the connection is complete.
     */
    public XFuture<Void> setDbSetup(DbSetup dbSetup) {

        this.dbSetup = dbSetup;
        if (null != this.dbSetup && this.dbSetup.isConnected()) {
            if (null == dbSetup.getDbType() || DbType.NONE == dbSetup.getDbType()) {
                throw new IllegalArgumentException("dbSetup.getDbType() =" + dbSetup.getDbType());
            }
            if (dbConnection == null) {
                XFuture<Void> ret = new XFuture<>("PddlActionToCrclGenerator.setDbSetup");
                try {
                    final StackTraceElement stackTraceElemArray[] = Thread.currentThread().getStackTrace();
                    DbSetupBuilder.connect(dbSetup).handle(
                            "PddlActionToCrclGenerator.handleDbConnect",
                            (Connection c, Throwable ex) -> {
                                if (null != c) {
                                    Utils.runOnDispatchThread(() -> {
                                        setDbConnection(c);
                                        ret.complete(null);
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
                                    if (null != aprsJFrame) {
                                        if (aprsJFrame.isEnableDebugDumpstacks()) {
                                            Thread.dumpStack();
                                        }
                                        aprsJFrame.setTitleErrorString("Database error: " + ex.toString());
                                    }
                                    ret.completeExceptionally(ex);
                                }
                                return c;
                            });
                    System.out.println("PddlActionToCrclGenerator connected to database of type " + dbSetup.getDbType() + " on host " + dbSetup.getHost() + " with port " + dbSetup.getPort());
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                return ret;
            }
            return XFuture.completedFutureWithName("setDbSetup.(dbConnection!=null)", null);
        } else {
            setDbConnection(null);
            return XFuture.completedFutureWithName("setDbSetup.setDbConnnection(null)", null);
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

    private final AtomicInteger lastIndex = new AtomicInteger();
    private volatile List<PddlAction> lastActionsList = null;

    public boolean atLastIndex() {
        final int idx = getLastIndex();
        if (idx == 0 && lastActionsList == null) {
            return true;
        }
        if (null == lastActionsList) {
            throw new IllegalStateException("null == lastActionsList");
        }
        return idx >= lastActionsList.size() - 1;
    }

    /**
     * Get the value of index of the last PDDL action planned.
     *
     * @return the value of lastIndex
     */
    public int getLastIndex() {
        return lastIndex.get();
    }

    private void setLastActionsIndex(List<PddlAction> actionsList, int index) {

        this.lastActionsList = actionsList;
        lastIndex.set(index);
    }

    private int incLastActionsIndex(List<PddlAction> actionsList) {

        this.lastActionsList = actionsList;
        return lastIndex.incrementAndGet();
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

    private PddlAction getNextPlacePartAction(int lastIndex, List<PddlAction> actions) {
        for (int i = lastIndex + 1; i < actions.size(); i++) {
            PddlAction action = actions.get(i);
            switch (action.getType()) {
                case "place-part":
                    return action;
                case "look-for-part":
                case "look-for-parts":
                case "end-program":
                    return null;
            }
        }
        return null;
    }

    /**
     * Set state/history/cache variables back to their initial values.
     */
    public void reset() {
        lastAcbi.set(null);
        this.lastTakenPart = null;
        this.unitsSet = false;
        this.rotSpeedSet = false;
        setLastActionsIndex(null, 0);
        clearPoseCache();
        clearLastRequiredPartsMap();
    }

    private OpDisplayJPanel opDisplayJPanelSolution;

    /**
     * Get the value of opDisplayJPanelSolution
     *
     * @return the value of opDisplayJPanelSolution
     */
    public OpDisplayJPanel getOpDisplayJPanelSolution() {
        return opDisplayJPanelSolution;
    }

    /**
     * Set the value of opDisplayJPanelSolution
     *
     * @param opDisplayJPanelSolution new value of opDisplayJPanelSolution
     */
    public void setOpDisplayJPanelSolution(OpDisplayJPanel opDisplayJPanelSolution) {
        this.opDisplayJPanelSolution = opDisplayJPanelSolution;
    }

    private OpDisplayJPanel opDisplayJPanelInput;

    /**
     * Get the value of opDisplayJPanelInput
     *
     * @return the value of opDisplayJPanelInput
     */
    public OpDisplayJPanel getOpDisplayJPanelInput() {
        return opDisplayJPanelInput;
    }

    /**
     * Set the value of opDisplayJPanelInput
     *
     * @param opDisplayJPanelInput new value of opDisplayJPanelInput
     */
    public void setOpDisplayJPanelInput(OpDisplayJPanel opDisplayJPanelInput) {
        this.opDisplayJPanelInput = opDisplayJPanelInput;
    }

    public List<MiddleCommandType> generate(List<PddlAction> actions, int startingIndex, Map<String, String> options, int startSafeAbortRequestCount)
            throws IllegalStateException, SQLException, InterruptedException, ExecutionException, IOException, PendantClientInner.ConcurrentBlockProgramsException {
        return generate(actions, startingIndex, options, startSafeAbortRequestCount, true, null);
    }

    /**
     * Generate a list of CRCL commands from a list of PddlActions starting with
     * the given index, using the provided optons.
     *
     * @param actions list of PDDL Actions
     * @param startingIndex starting index into list of PDDL actions
     * @param options options to use as commands are generated
     * @param startSafeAbortRequestCount abort request count taken when higher
     * level action was started this method will immediately abort if the
     * request count is now already higher
     * @param replan run optaplanner to replan provided actions
     * @param origActions actions before being passed through optaplanner
     * @return list of CRCL commands
     *
     * @throws IllegalStateException if database not connected
     * @throws SQLException if query of the database failed
     */
    public List<MiddleCommandType> generate(List<PddlAction> actions, int startingIndex, Map<String, String> options, int startSafeAbortRequestCount, boolean replan, List<PddlAction> origActions)
            throws IllegalStateException, SQLException, InterruptedException, ExecutionException, IOException, PendantClientInner.ConcurrentBlockProgramsException {

        if (null != solver && replan) {
            return runOptaPlanner(actions, startingIndex, options, startSafeAbortRequestCount);
        }

        this.startSafeAbortRequestCount = startSafeAbortRequestCount;
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        if (aprsJFrame.isRunningCrclProgram()) {
            throw new IllegalStateException("already running crcl while trying to generate it");
        }
        List<MiddleCommandType> cmds = new ArrayList<>();
        int blockingCount = aprsJFrame.startBlockingCrclPrograms();

        ActionCallbackInfo acbi = lastAcbi.get();
        if (null != acbi) {
            if (startingIndex < acbi.actionIndex) {
                if (startingIndex != 0 || acbi.actionIndex < acbi.getActionsSize() - 2) {
                    String errString = "generate called with startingIndex=" + startingIndex + " and acbi.actionIndex=" + acbi.actionIndex + ", lastIndex=" + lastIndex + ", acbi=" + acbi.toString();
                    System.err.println(errString);
                    aprsJFrame.setTitleErrorString(errString);
                    throw new IllegalStateException(errString);
                }
            }
        }
        try {

            this.options = options;
            final int crclNumber = this.crclNumber.incrementAndGet();

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
//            if (debug) {
//                Thread.dumpStack();
//                System.out.println("debug generate");
//            }

            List<PhysicalItem> newItems = null;
            if (startingIndex > 0 && null != acbi && null != acbi.action) {
                switch (acbi.action.getType()) {

                    case "look-for-part":
                    case "look-for-parts":
                        newItems = waitForCompleteVisionUpdates("generate(start=" + startingIndex + ",crclNumber=" + crclNumber + ")", lastRequiredPartsMap);

                        assert (newItems != null) :
                                "newItems == null";

                        synchronized (poseCache) {
                            for (PhysicalItem item : newItems) {
                                poseCache.put(item.getFullName(), item.getPose());
                            }
                        }
                        break;

                    default:
                        break;
                }
            }

            takeSnapshots("plan", "generate(start=" + startingIndex + ",crclNumber=" + crclNumber + ")", null, null);
            final List<PddlAction> fixedActionsCopy = Collections.unmodifiableList(new ArrayList<>(actions));
            final List<PddlAction> fixedOrigActionsCopy = (origActions == null) ? null : Collections.unmodifiableList(new ArrayList<>(actions));

            for (this.setLastActionsIndex(actions, startingIndex); getLastIndex() < actions.size(); incLastActionsIndex(actions)) {

                final int idx = getLastIndex();
                PddlAction action = actions.get(idx);
                System.out.println("action = " + action);
                takeSnapshots("plan", "gc_actions.get(" + idx + ")=" + action, null, null);
//            try {
                String start_action_string = "start_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                addTakeSnapshots(cmds, start_action_string, null, null, this.crclNumber.get());
                switch (action.getType()) {
                    case "take-part":
                        takePart(action, cmds, getNextPlacePartAction(idx, actions));
                        break;

                    case "fake-take-part":
                        fakeTakePart(action, cmds);
                        break;

                    case "test-part-position":
                        testPartPosition(action, cmds);
                        break;
                    case "look-for-part":
                    case "look-for-parts":
                        lookForParts(action, cmds, (idx < 2),
                                doInspectKit ? (idx == actions.size() - 1) : (idx >= actions.size() - 2)
                        );
                        actionToCrclIndexes[idx] = cmds.size();
                        actionToCrclLabels[idx] = "";
                        actionToCrclTakenPartsNames[idx] = this.lastTakenPart;
                         {
                            String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addMarkerCommand(cmds, end_action_string,
                                    (CrclCommandWrapper wrapper) -> {
                                        notifyActionCompletedListeners(idx, action, wrapper, fixedActionsCopy, fixedOrigActionsCopy);
                                    });
                        }
                        return cmds;

                    case "end-program":
                        endProgram(action, cmds);
                        actionToCrclIndexes[idx] = cmds.size();
                        actionToCrclLabels[idx] = "";
                        actionToCrclTakenPartsNames[idx] = this.lastTakenPart;
                         {
                            String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addMarkerCommand(cmds, end_action_string,
                                    (CrclCommandWrapper wrapper) -> {
                                        notifyActionCompletedListeners(idx, action, wrapper, fixedActionsCopy, fixedOrigActionsCopy);
                                    });
                        }
                        return cmds;

                    case "place-part":
                        placePart(action, cmds);
                        break;

                    case "pause":
                        pause(action, cmds);
                        break;

                    case "inspect-kit": {
                        assert (startingIndex == idx) : "inspect-kit startingIndex(" + startingIndex + ") != lastIndex(" + idx + ")";
                        if (doInspectKit) {
                            try {
                                inspectKit(action, cmds);
                            } catch (Exception ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    break;

                    case "clear-kits-to-check":
                        clearKitsToCheck(action, cmds);
                        break;

                    case "add-kit-to-check":
                        addKitToCheck(action, cmds);
                        break;

                    case "check-kits":
                        checkKits(action, cmds);
                        break;

                    default:
                        throw new IllegalArgumentException("unrecognized action " + action + " at index " + idx);
                }

                actionToCrclIndexes[idx] = cmds.size();
                actionToCrclLabels[idx] = "";
                actionToCrclTakenPartsNames[idx] = this.lastTakenPart;
                String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                addMarkerCommand(cmds, end_action_string,
                        (CrclCommandWrapper wrapper) -> {
                            notifyActionCompletedListeners(idx, action, wrapper, fixedActionsCopy, fixedOrigActionsCopy);
                        });
                addTakeSnapshots(cmds, end_action_string, null, null, this.crclNumber.get());
            }
            if (aprsJFrame.isRunningCrclProgram()) {
                throw new IllegalStateException("already running crcl while trying to generate it");
            }
        } finally {
            aprsJFrame.stopBlockingCrclPrograms(blockingCount);
        }
        return cmds;
    }

    private static final AtomicInteger ropCount = new AtomicInteger();

    private List<MiddleCommandType> runOptaPlanner(List<PddlAction> actions, int startingIndex, Map<String, String> options1, int startSafeAbortRequestCount1) throws IllegalStateException, InterruptedException, SQLException, ExecutionException, IOException, PendantClientInner.ConcurrentBlockProgramsException {
        int rc = ropCount.incrementAndGet();
        System.out.println("runOptaPlanner: rc = " + rc);
        long t0 = System.currentTimeMillis();
        this.options = options1;
        if (actions.size() < 1) {
            throw new IllegalArgumentException("actions.size() = " + actions.size() + ",rc=" + rc);
        }
        List<PddlAction> fullReplanPddlActions = optimizePddlActionsWithOptaPlanner(actions, startingIndex);
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActions || fullReplanPddlActions.size() < 1) {
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActions);
        }
        if (fullReplanPddlActions == actions) {
            return generate(actions, startingIndex, options1, startSafeAbortRequestCount1, false, null);
        }
        List<PddlAction> copyFullReplanPddlActions = new ArrayList<>(fullReplanPddlActions);
        List<PddlAction> origActions = new ArrayList<>(actions);
        long t1 = System.currentTimeMillis();
        System.out.println("runOptaPlanner: (t1-t0) = " + (t1 - t0) + ",rc=" + rc);
        synchronized (actions) {
            actions.clear();
            actions.addAll(fullReplanPddlActions);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("runOptaPlanner: (t2-t0) = " + (t2 - t0) + ",rc=" + rc);
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActions || actions.size() < 1) {
            System.out.println("copyFullReplanPddlActions = " + copyFullReplanPddlActions);
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActions);
        }
        List<MiddleCommandType> newCmds = generate(actions, startingIndex, options1, startSafeAbortRequestCount1, false, origActions);
        long t3 = System.currentTimeMillis();
        System.out.println("runOptaPlanner: (t3-t0) = " + (t3 - t0) + ",rc=" + rc);
        return newCmds;
    }

    private List<PddlAction> optimizePddlActionsWithOptaPlanner(List<PddlAction> actions, int startingIndex) throws SQLException {
        int endl[] = new int[2];
        PointType lookForPt = getLookForXYZ();
        if (null == lookForPt) {
            throw new IllegalStateException("null == lookForPT, startingIndex=" + startingIndex + ", actions=" + actions);
        }
        List<OpAction> skippedOpActionsList = new ArrayList<>(); 
        List<PddlAction> skippedPddlActionsList = new ArrayList<>();
        List<OpAction> opActions = pddlActionsToOpActions(actions, startingIndex, endl, skippedOpActionsList,skippedPddlActionsList);
        if (opActions.size() < 3) {
            logger.warning("opActions.size()=" + opActions.size());
            return actions;
        }
        if(skippedActions > 0) {
            System.out.println("skippedPddlActionsList = " + skippedPddlActionsList);
        }
        OpActionPlan inputPlan = new OpActionPlan();
        inputPlan.setActions(opActions);
        inputPlan.getEndAction().setLocation(new Point2D.Double(lookForPt.getX(), lookForPt.getY()));
        inputPlan.initNextActions();
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(inputPlan);
        double inScore = (score.getSoftScore() / 1000.0);
        OpActionPlan solvedPlan = solver.solve(inputPlan);
        double solveScore = (solvedPlan.getScore().getSoftScore() / 1000.0);
        for (int i = 0; i < skippedOpActionsList.size(); i++) {
            OpAction skippedAction = skippedOpActionsList.get(i);
            for (int j = 0; j < 10; j++) {
                OpAction repAction = opActions.get(j);
                if (skippedAction.getActionType() == repAction.getActionType()
                        && skippedAction.getPartType().equals(repAction.getPartType())) {
                    List<OpAction> altActions = new ArrayList<>(opActions);
                    altActions.set(j, skippedAction);
                    OpActionPlan altInputPlan = new OpActionPlan();
                    altInputPlan.setActions(opActions);
                    altInputPlan.getEndAction().setLocation(new Point2D.Double(lookForPt.getX(), lookForPt.getY()));
                    altInputPlan.initNextActions();
                    OpActionPlan altSolvedPlan = solver.solve(altInputPlan);
                    double altSolvedScore = (altSolvedPlan.getScore().getSoftScore() / 1000.0);
                    if (altSolvedScore > solveScore) {
                        solvedPlan = altSolvedPlan;
                        solveScore = altSolvedScore;
                    }
                }
            }
        }
        System.out.println("Score improved:" + (solveScore - inScore));
        if (Math.abs(solveScore - inScore) > 0.1) {
            List<PddlAction> fullReplanPddlActions = new ArrayList<>();
            List<PddlAction> skippedPddlActions = actions.subList(0, startingIndex > endl[0] ? startingIndex : endl[0]);
            System.out.println("skippedPddlActions = " + skippedPddlActions);
            fullReplanPddlActions.addAll(skippedPddlActions);
            List<PddlAction> newPddlActions = opActionsToPddlActions(solvedPlan.orderedActions(), 0);
            System.out.println("newPddlActions = " + newPddlActions);
            List<PddlAction> replacedPddlActions = actions.subList(endl[0], endl[1]);
            System.out.println("replacedPddlActions = " + replacedPddlActions);
            fullReplanPddlActions.addAll(newPddlActions);
            List<PddlAction> laterPddlActions = actions.subList(endl[1], actions.size());
            System.out.println("laterPddlActions = " + laterPddlActions);
            fullReplanPddlActions.addAll(laterPddlActions);
            if (null != this.opDisplayJPanelInput) {
                if (null == opDisplayJPanelInput.getOpActionPlan()) {
                    this.opDisplayJPanelInput.setOpActionPlan(inputPlan);
                    this.opDisplayJPanelInput.setLabel("Input : " + String.format("%.1f mm ", -inScore));
                }
            }
            if (null != this.opDisplayJPanelSolution) {

                this.opDisplayJPanelSolution.setOpActionPlan(solvedPlan);
                this.opDisplayJPanelSolution.setLabel("Output : " + String.format("%.1f mm ", -solveScore));
            }
            return fullReplanPddlActions;
        }
        return actions;
    }

    private void clearKitsToCheck(PddlAction action, List<MiddleCommandType> cmds) {
        kitsToCheck.clear();
    }

    private void pause(PddlAction action, List<MiddleCommandType> cmds) {
        addMarkerCommand(cmds, "pause", x -> aprsJFrame.pause());
    }

    private void addKitToCheck(PddlAction action, List<MiddleCommandType> cmds) {
        KitToCheck kit = new KitToCheck();
        kit.name = action.getArgs()[0];
        kit.slotMap = Arrays.stream(action.getArgs(), 1, action.getArgs().length)
                .map(arg -> arg.split("="))
                .collect(Collectors.toMap(array -> array[0], array -> array[1]));
        kitsToCheck.add(kit);
    }

    private List<String> getPartTrayInstancesFromSkuName(String skuName) {
        try {
            return qs.getPartsTrays(skuName).stream()
                    .map(PartsTray::getPartsTrayName)
                    .collect(Collectors.toList());
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private final ConcurrentMap<String, List<String>> skuNameToInstanceNamesMap = new ConcurrentHashMap<>();

    private List<String> getKitInstanceNames(String kitName) {
        return skuNameToInstanceNamesMap.computeIfAbsent(kitName, this::getPartTrayInstancesFromSkuName);
    }

    private List<Slot> getAbsSlotListForKitInstance(String kitSkuName, String kitInstanceName) {
        try {
            PoseType pose = getPose(kitInstanceName);
            System.out.println("pose = " + pose);
            Tray tray = new Tray(kitSkuName, pose, 0);
            tray.setType("KT");
            return aprsJFrame.getSlots(tray)
                    .stream()
                    .filter(slot -> slot.getType().equals("S"))
                    .peek(slot -> {
                        slot.setVxi(xAxis.getI());
                        slot.setVxj(xAxis.getJ());
                        slot.setVxk(xAxis.getK());
                        slot.setVzi(zAxis.getI());
                        slot.setVzj(zAxis.getJ());
                        slot.setVzk(zAxis.getK());
                    })
                    .collect(Collectors.toList());
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void checkKits(PddlAction action, List<MiddleCommandType> cmds) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, IOException {
        List<PhysicalItem> newItems = waitForCompleteVisionUpdates("checkKits", lastRequiredPartsMap);

        assert (newItems != null) :
                "newItems == null";

        synchronized (poseCache) {
            for (PhysicalItem item : newItems) {
                poseCache.put(item.getFullName(), item.getPose());
            }
        }
        takeSnapshots("plan", "checkKits-", null, "");

        List<PhysicalItem> parts = newItems.stream()
                .filter(x -> !x.getName().startsWith("empty_slot"))
                .filter(x -> !x.getName().contains("vessel"))
                .collect(Collectors.toList());
        System.out.println("parts = " + parts);
        Map<String, List<Slot>> kitInstanceAbsSlotMap = new HashMap<>();

        List<KitToCheck> kitsToFix = new ArrayList<>(kitsToCheck);
        Set<String> matchedKitInstanceNames = new HashSet<>();

        try {
            for (KitToCheck kit : kitsToCheck) {
                List<String> kitInstanceNames = getKitInstanceNames(kit.name);
                for (String kitInstanceName : kitInstanceNames) {

                    if (matchedKitInstanceNames.contains(kitInstanceName)) {
                        continue;
                    }
                    List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                            (String n) -> getAbsSlotListForKitInstance(kit.name, n));

                    takeSimViewSnapshot(aprsJFrame.createTempFile("absSlots_" + kitInstanceName, ".PNG"), absSlots);
                    boolean allSlotsCorrect = true;
                    for (Slot absSlot : absSlots) {
                        String absSlotPrpName = absSlot.getPrpName();
                        PhysicalItem closestItem = parts.stream()
                                .min(Comparator.comparing(absSlot::distFromXY))
                                .orElse(null);
                        String itemSkuName = "empty";
                        if (null != closestItem && closestItem.distFromXY(absSlot) < absSlot.getDiameter() / 2.0) {
                            itemSkuName = closestItem.origName;
                        }
                        if (!kit.slotMap.get(absSlotPrpName).equals(itemSkuName)) {
                            allSlotsCorrect = false;
                            break;
                        }
                    }
                    if (allSlotsCorrect) {
                        kitsToFix.remove(kit);
                        matchedKitInstanceNames.add(kitInstanceName);
                        break;
                    }
                }
                System.out.println("matchedKitInstanceNames = " + matchedKitInstanceNames);
                System.out.println("kitsToFix = " + kitsToFix);
                System.out.println("");
            }
            Map<String, Integer> prefixCountMap = new HashMap<>();
            if (!kitsToFix.isEmpty()) {
                if (pauseInsteadOfRecover) {
                    pause(action, cmds);
                } else {

                    for (KitToCheck kit : kitsToFix) {
                        List<String> kitInstanceNames = getKitInstanceNames(kit.name);
                        for (String kitInstanceName : kitInstanceNames) {

                            if (matchedKitInstanceNames.contains(kitInstanceName)) {
                                continue;
                            }
                            List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                                    (String n) -> getAbsSlotListForKitInstance(kit.name, n));

                            takeSimViewSnapshot(aprsJFrame.createTempFile("absSlots_" + kitInstanceName, ".PNG"), absSlots);
                            for (Slot absSlot : absSlots) {
                                String absSlotPrpName = absSlot.getPrpName();
                                PhysicalItem closestItem = parts.stream()
                                        .min(Comparator.comparing(absSlot::distFromXY))
                                        .orElse(null);
                                String itemSkuName = "empty";
                                if (null != closestItem && closestItem.distFromXY(absSlot) < absSlot.getDiameter() / 2.0) {
                                    itemSkuName = closestItem.origName;
                                }
                                String slotItemSkuName = kit.slotMap.get(absSlotPrpName);
                                if (null != slotItemSkuName && !slotItemSkuName.equals(itemSkuName)) {

                                    if (!itemSkuName.equals("empty")) {
                                        takePartByPose(cmds, visionToRobotPose(closestItem.getPose()));
                                        String shortSkuName = itemSkuName;
                                        if (shortSkuName.startsWith("sku_")) {
                                            shortSkuName = shortSkuName.substring(4);
                                        }
                                        if (shortSkuName.startsWith("part_")) {
                                            shortSkuName = shortSkuName.substring(5);
                                        }
                                        String slotPrefix = "empty_slot_for_" + shortSkuName + "_in_" + shortSkuName + "_vessel";
                                        int count = prefixCountMap.compute(slotPrefix,
                                                (String prefix, Integer c) -> (c == null) ? 1 : (c + 1));
                                        lastTakenPart = closestItem.getName();
                                        placePartBySlotName(slotPrefix + "_" + count, cmds, action);
                                    }
                                    if (!slotItemSkuName.equals("empty")) {
                                        String shortSkuName = slotItemSkuName;
                                        if (shortSkuName.startsWith("sku_")) {
                                            shortSkuName = shortSkuName.substring(4);
                                        }
                                        String partNamePrefix = shortSkuName + "_in_pt";
                                        int count = prefixCountMap.compute(partNamePrefix,
                                                (String prefix, Integer c) -> (c == null) ? 1 : (c + 1));
                                        takePartByName(partNamePrefix + "_" + count, null, cmds);
                                        placePartByPose(cmds, visionToRobotPose(absSlot.getPose()));
                                    }
                                }
                            }
                            matchedKitInstanceNames.add(kitInstanceName);
                        }
                        System.out.println("matchedKitInstanceNames = " + matchedKitInstanceNames);
                        System.out.println("kitsToFix = " + kitsToFix);
                        System.out.println("");
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
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

    private double testTransSpeed = 50.0;

    /**
     * Get the value of testTransSpeed
     *
     * @return the value of testTransSpeed
     */
    public double getTestTransSpeed() {
        return testTransSpeed;
    }

    /**
     * Set the value of testTransSpeed
     *
     * @param testTransSpeed new value of testTransSpeed
     */
    public void setTestTransSpeed(double testTransSpeed) {
        this.testTransSpeed = testTransSpeed;
    }

    private double skipLookDwellTime = (5.0);
    private double afterMoveToLookForDwellTime = 5.0;
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
    public PoseType visionToRobotPose(PoseType poseIn) {
        PoseType pout = poseIn;
        if (null != getPositionMaps()) {
            for (PositionMap pm : getPositionMaps()) {
                pout = pm.correctPose(pout);
            }
        }
        pout.setXAxis(xAxis);
        pout.setZAxis(zAxis);
        return pout;
    }

    public PointType visionToRobotPoint(PointType poseIn) {
        PointType pout = poseIn;
        if (null != getPositionMaps()) {
            for (PositionMap pm : getPositionMaps()) {
                pout = pm.correctPoint(pout);
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
        if (null == aprsJFrame && null != parentPddlExecutorJPanel) {
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

    public void takeSimViewSnapshot(File f, PointType point, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, point, label);
        }
    }

    public void takeSimViewSnapshot(File f, PmCartesian pt, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, pt, label);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(imgLabel, pose, label);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PointType point, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(imgLabel, point, label);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PmCartesian pt, String label) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(imgLabel, pt, label);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint items to paint in the snapshot image
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(f, itemsToPaint);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint) throws IOException {
        if (null != aprsJFrame) {
            aprsJFrame.takeSimViewSnapshot(imgLabel, itemsToPaint);
        }
    }

    private List<PhysicalItem> poseCacheToDetectedItemList() {
        List<PhysicalItem> l = new ArrayList<>();
        synchronized (poseCache) {
            for (Entry<String, PoseType> entry : poseCache.entrySet()) {
                l.add(PhysicalItem.newPhysicalItemNamePoseVisionCycle(entry.getKey(), entry.getValue(), 0));
            }
        }
        return Collections.unmodifiableList(l);
    }

    private List<PhysicalItem> posesToDetectedItemList(Collection<PoseType> poses) {
        List<PhysicalItem> l = new ArrayList<>();
        int i = 0;
        for (PoseType pose : poses) {
            i++;
            l.add(PhysicalItem.newPhysicalItemNamePoseVisionCycle("pose_" + i, pose, 0));
        }
        l.addAll(poseCacheToDetectedItemList());
        return Collections.unmodifiableList(l);
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
    public void addTakeSnapshots(List<MiddleCommandType> out,
            String title, final PoseType pose, String label, final int crclNumber) {
        if (takeSnapshots) {
            addMarkerCommand(out, title, x -> {
                final int curCrclNumber = PddlActionToCrclGenerator.this.crclNumber.get();
                if (crclNumber != curCrclNumber) {
                    aprsJFrame.setTitleErrorString("crclNumber mismatch " + crclNumber + "!=" + curCrclNumber);
                }
                takeSnapshots("exec", title, pose, label);
            });
        }
    }

    public void takeSnapshots(String prefix, String title, PoseType pose, String label) {
        if (takeSnapshots) {
            try {
                String fullTitle = title + "_crclNumber-" + String.format("%03d", crclNumber.get()) + "_action-" + String.format("%03d", getLastIndex());
                takeSimViewSnapshot(aprsJFrame.createTempFile(prefix + "_" + fullTitle, ".PNG"), pose, label);
                takeDatabaseViewSnapshot(aprsJFrame.createTempFile(prefix + "_db_" + fullTitle, ".PNG"));
                takeSimViewSnapshot(aprsJFrame.createTempFile(prefix + "_pc_" + fullTitle, ".PNG"), poseCacheToDetectedItemList());
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Get a run prefix useful for naming/identifying snapshot files.
     *
     * @return run prefix
     */
    public String getRunPrefix() {
        return getRunName() + Utils.getDateTimeString() + "_" + String.format("%03d", crclNumber) + "action-" + String.format("%03d", lastIndex);
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

        PoseType pose = getPose(partName);
        pose = visionToRobotPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        testPartPositionByPose(out, pose);
        lastTakenPart = partName;
    }

    private void setCommandId(CRCLCommandType cmd) {
        Utils.setCommandID(cmd, incrementAndGetCommandId());
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

    private void kitInspectionJInternalFrameKitTitleLabelSetText(String text) {
        final KitInspectionJInternalFrame kitFrame = kitInspectionJInternalFrame;
        if (null != kitFrame) {
            Utils.runOnDispatchThread(() -> {
                try {
                    kitInspectionJInternalFrame.getKitTitleLabel().setText(text);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    private void addToInspectionResultJTextPane(String text) {
        final KitInspectionJInternalFrame kitFrame = kitInspectionJInternalFrame;
        if (null != kitFrame) {
            Utils.runOnDispatchThread(() -> {
                try {
                    kitFrame.addToInspectionResultJTextPane(text);
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            });
        }
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
    public void inspectKit(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, BadLocationException, InterruptedException, ExecutionException, IOException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        if (action.getArgs().length < 2) {
            throw new IllegalArgumentException("action = " + action + " needs at least two arguments: kitSku inspectionID");
        }
        if (PlacePartSlotPoseList.isEmpty()) {
            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;No place part slots added. </h3><br>");
            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Inspection Aborted</h3><br>");
            takeSnapshots("plan", "PlacePartSlotPoseList.isEmpty()-inspect-kit-", null, "");
            return;
        }
        waitForCompleteVisionUpdates("inspectKit", lastRequiredPartsMap);
        takeSnapshots("plan", "inspect-kit-", null, "");

//        addTakeSnapshots(out, "inspect-kit-", null, "");
        String kitSku = action.getArgs()[0];
        String inspectionID = action.getArgs()[1];
        MessageType msg = new MessageType();
        msg.setMessage("inspect-kit " + kitSku + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(msg);
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
                try {
//                    takeSimViewSnapshot("inspectKit.correctPartsTray",
//                            new PmCartesian(correctPartsTray.getX(), correctPartsTray.getY(), 0),
//                            correctPartsTray.getPartsTrayName());
                    takeSimViewSnapshot("inspectKit.correctPartsTray.partsTrayPose",
                            new PmCartesian(correctPartsTray.getPartsTrayPose().getPoint().getX(), correctPartsTray.getPartsTrayPose().getPoint().getY(), 0),
                            correctPartsTray.getPartsTrayName());
                    takeSimViewSnapshot("inspectKit.correctPartsTray.slotList",
                            correctPartsTray.getSlotList());
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
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
                    assert (indexLastUnderscore >= 0) :
                            ("TakenPartList=" + TakenPartList + " contains invalid tmpPartName=" + tmpPartName + " from part_in_pt=" + part_in_pt);

                    String part_in_kt = tmpPartName.substring(0, indexLastUnderscore);
                    assert (part_in_kt.indexOf('_') > 0) :
                            ("part_in_kt=" + part_in_kt + ",tmpPartName=" + tmpPartName + " from part_in_pt=" + part_in_pt + ", indexLastUnderscore=" + indexLastUnderscore);

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
                    addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #7ef904\">&nbsp;&nbsp;The kit is complete</h3><br>");
                } else {
                    try {
                        takeSimViewSnapshot(aprsJFrame.createTempFile("inspectKit-slotList", ".PNG"), slotList);
                        takeSimViewSnapshot(aprsJFrame.createTempFile("inspectKit-EmptySlotSet", ".PNG"), EmptySlotSet);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    //if (part_in_kt_found) {
                    TakenPartList.clear();
                    int nbofmissingparts = partDesignPartCount - numberOfPartsInKit;
                    addToInspectionResultJTextPane("<h3 style=\"background-color: #ffb5b5; color: #ffffff\">&nbsp;&nbsp;The kit is missing " + nbofmissingparts + " part(s)</h3>");
                    for (Slot s : EmptySlotSet) {
                        addToInspectionResultJTextPane("&nbsp;&nbsp;Slot " + s.getSlotName() + " is missing a part of type " + s.getPartSKU() + "<br>");
                    }
                    if (nbofmissingparts == 1) {
                        kitInspectionJInternalFrameKitTitleLabelSetText("Missing " + nbofmissingparts + " part. Getting the new part.");
                    } else {
                        kitInspectionJInternalFrameKitTitleLabelSetText("Missing " + nbofmissingparts + " parts. Getting the new parts.");
                    }
                    addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;Recovering from failures</h2>");
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
                            addToInspectionResultJTextPane("&nbsp;&nbsp;Getting a list of part_in_pt for sku " + partSKU + "<br>");

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
                                // addToInspectionResultJTextPane("Could not find part_in_pt for sku " + partSKU + " from the database<b
                                addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Could not find part_in_pt for sku " + partSKU + " from the database</h3><br>");
                                addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Recovery Aborted</h3><br>");
                            }
                        }
                    } else {
                        addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Could not find parts in_pt from the database</h3><br>");
                        addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Recovery Aborted</h3><br>");
                    }
                    addToInspectionResultJTextPane("<br>");
                }
            } else {
                addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;The system could not identify the kit tray that was built. (kitSku=" + kitSku + ") </h3><br>");
                addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Inspection Aborted</h3><br>");
                System.err.println("Trying to get correctPartsTray again ...");
                correctPartsTray = findCorrectKitTray(kitSku);
                System.out.println("msg = " + msg);
            }
        }
        if (inspectionID.contains("0")) {
            PlacePartSlotPoseList.clear();
            PlacePartSlotPoseList = null;
            correctPartsTray = null;
        }
    }

    private double getVisionToDBRotationOffset() {
        assert (null != this.aprsJFrame) :
                ("null == this.aprsJFrame");

        return this.aprsJFrame.getVisionToDBRotationOffset();
    }

//    private List<DetectedItem>  partsTrayListToDetectedItemList(List<PartsTray> listIn) {
//        List<DetectedItem>  listOut = new ArrayList<>();
//        for(PartsTray partsTray : listIn) {
//            listOut.add(new PhysicalItem(partsTray.getPartsTrayName(), partsTray.getRotation(), partsTray.getX(), partsTray.getY()));
//        }
//        return listOut;
//    }
    /**
     * Function that finds the correct kit tray from the database using the sku
     * kitSku
     *
     * @param kitSku sku of kit
     * @return tray from database
     * @throws SQLException if query fails
     */
    private PartsTray findCorrectKitTray(String kitSku) throws SQLException {

        List<PartsTray> dpuPartsTrayList = aprsJFrame.getPartsTrayList();

        //-- retrieveing from the database all the parts trays that have the sku kitSku
        List<PartsTray> partsTraysList = getPartsTrays(kitSku);

        List<PhysicalItem> partsTrayListItems = new ArrayList<>();
        List<PhysicalItem> dpuPartsTrayListItems = new ArrayList<>();

        /*
        System.out.println("-Checking parts trays");
        for (int i = 0; i < partsTraysList.size(); i++) {
            PartsTray partsTray = partsTraysList.get(i);
            System.out.println("-Parts tray: " + partsTray.getPartsTrayName());
        }
         */
        for (int i = 0; i < partsTraysList.size(); i++) {

            PartsTray partsTray = partsTraysList.get(i);

            //-- getting the pose for the parts tray 
            PoseType partsTrayPose = qs.getPose(partsTray.getPartsTrayName());

//            partsTrayPose = visionToRobotPose(partsTrayPose);
            System.out.println("-Checking parts tray [" + partsTray.getPartsTrayName() + "] :(" + partsTrayPose.getPoint().getX() + "," + partsTrayPose.getPoint().getY() + ")");
            partsTray.setpartsTrayPose(partsTrayPose);
            double partsTrayPoseX = partsTrayPose.getPoint().getX();
            double partsTrayPoseY = partsTrayPose.getPoint().getY();
            double partsTrayPoseZ = partsTrayPose.getPoint().getZ();

            double rotation = 0;
            //-- Read partsTrayList
            //-- Assign rotation to myPartsTray by comparing poses from vision vs database
            //System.out.print("-Assigning proper rotation: ");
            System.out.println("-Comparing with other parts trays from vision");
            for (int c = 0; c < dpuPartsTrayList.size(); c++) {
                PartsTray pt = dpuPartsTrayList.get(c);

                PointType dpuPartsTrayPoint = point(pt.getX(), pt.getY(), 0);

//                dpuPartsTrayPoint = visionToRobotPoint(dpuPartsTrayPoint);
                double ptX = dpuPartsTrayPoint.getX();
                double ptY = dpuPartsTrayPoint.getY();
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
                if (c >= dpuPartsTrayListItems.size()) {
                    dpuPartsTrayListItems.add(new Tray(pt.getPartsTrayName(), pt.getRotation(), ptX, ptY));
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
            System.out.println("-Checking slots");
            List<Slot> slotList = partsTray.getSlotList();
            int count = 0;
            for (int j = 0; j < slotList.size(); j++) {
                Slot slot = slotList.get(j);
                double x_offset = slot.getX_OFFSET() * 1000;
                double y_offset = slot.getY_OFFSET() * 1000;
                double slotX = partsTrayPoseX + x_offset * Math.cos(angle) - y_offset * Math.sin(angle);
                double slotY = partsTrayPoseY + x_offset * Math.sin(angle) + y_offset * Math.cos(angle);
                double slotZ = partsTrayPoseZ;
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
                    if (distance < 5.0) {
                        count++;
                    }
                }
            }
            try {
                takeSimViewSnapshot(aprsJFrame.createTempFile("PlacePartSlotPoseList", ".PNG"), posesToDetectedItemList(PlacePartSlotPoseList));
                takeSimViewSnapshot(aprsJFrame.createTempFile("partsTray.getSlotList", ".PNG"), slotList);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            partsTrayListItems.add(new Tray(partsTray.getPartsTrayName(), partsTray.getRotation(), partsTrayPoseX, partsTrayPoseY));
            if (count > 0) {
                try {
                    takeSimViewSnapshot(aprsJFrame.createTempFile("dpuPartsTrayList", ".PNG"), dpuPartsTrayListItems);
                    takeSimViewSnapshot(aprsJFrame.createTempFile("partsTrayList", ".PNG"), partsTrayListItems);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                correctPartsTray = partsTray;
                System.out.println("Found partstray: " + partsTray.getPartsTrayName());
                return partsTray;
            }
        }
        try {
            takeSimViewSnapshot(aprsJFrame.createTempFile("dpuPartsTrayList", ".PNG"), dpuPartsTrayListItems);
            takeSimViewSnapshot(aprsJFrame.createTempFile("partsTrayList", ".PNG"), partsTrayListItems);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        System.err.println("findCorrectKitTray(" + kitSku + ") returning null. partsTraysList=" + partsTraysList);
        return null;
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
                System.out.print("-------- " + newPartInKt);
                if (checkPartInSlot(newPartInKt, slot)) {
                    System.out.println("-------- Located in slot");
                    nbOfOccupiedSlots++;
                } else {
                    System.out.println("-------- Not located in slot");
                }
            }
            //part_in_kt_found=true;
        } else {
            addToInspectionResultJTextPane("&nbsp;&nbsp;No part_in_kt of type " + partInKt + " was found in the database<br>");
            //part_in_kt_found=false;
        }
        return nbOfOccupiedSlots;
    }

    private double kitInspectDistThreshold = 20.0;

    /**
     * Get the value of kitInspectDistThreshold
     *
     * @return the value of kitInspectDistThreshold
     */
    public double getKitInspectDistThreshold() {
        return kitInspectDistThreshold;
    }

    /**
     * Set the value of kitInspectDistThreshold
     *
     * @param kitInspectDistThreshold new value of kitInspectDistThreshold
     */
    public void setKitInspectDistThreshold(double kitInspectDistThreshold) {
        this.kitInspectDistThreshold = kitInspectDistThreshold;
    }

    private Boolean checkPartInSlot(String partName, Slot slot) throws SQLException {
        Boolean isPartInSlot = false;
        PoseType posePart = getPose(partName);
        if (null == posePart) {
            throw new IllegalStateException("getPose(" + partName + ") returned null");
        }
//        posePart = visionToRobotPose(posePart);
        double partX = posePart.getPoint().getX();
        double partY = posePart.getPoint().getY();
        double slotX = slot.getSlotPose().getPoint().getX();
        double slotY = slot.getSlotPose().getPoint().getY();
        System.out.println(":(" + partX + "," + partY + ")");
        double distance = Math.hypot(partX - slotX, partY - slotY);
        System.out.print("-------- Distance = " + distance);
        // compare finalres with a specified tolerance value of 6.5 mm
        double threshold = 20;
        if (distance < kitInspectDistThreshold) {
            isPartInSlot = true;
            // System.out.println("---- Part " + partName + " : (" + partX + "," + partY + ")");
            // System.out.println("---- Slot " + slot.getSlotName() + " : (" + slotX + "," + slotY + ")");
            // System.out.println("---- Distance between part and slot = " + dist);
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
        if (null != parentPddlExecutorJPanel) {
            return this.parentPddlExecutorJPanel.getForceFakeTakeFlag();
        }
        return false;
    }

    private void setFakeTakePart(boolean _newValue) {
        if (null != parentPddlExecutorJPanel) {
            this.parentPddlExecutorJPanel.setForceFakeTakeFlag(_newValue);
        }
    }

    /**
     * Add commands to the list that will take a given part.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @param nextPlacePartAction action to be checked to see if part should be
     * skipped
     * @throws IllegalStateException if database is not connected
     * @throws SQLException if database query fails
     */
    public void takePart(PddlAction action, List<MiddleCommandType> out, PddlAction nextPlacePartAction) throws IllegalStateException, SQLException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];

        if (null != kitInspectionJInternalFrame) {
            kitInspectionJInternalFrame.setKitImage("init");
            kitInspectionJInternalFrame.getKitTitleLabel().setText("Building kit");
            setCorrectKitImage();
        }

        takePartByName(partName, nextPlacePartAction, out);
    }

    public void takePartByName(String partName, PddlAction nextPlacePartAction, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        PoseType pose = getPose(partName);
        if (takeSnapshots) {
            takeSnapshots("plan", "take-part-" + partName + "", pose, partName);
        }
        if (null == pose) {
            if (skipMissingParts) {
                lastTakenPart = null;
                takeSnapshots("plan", "skipping-take-part-" + partName + "", pose, partName);
                PoseType poseCheck = getPose(partName);
                System.out.println("poseCheck = " + poseCheck);
                return;
            } else {
                throw new IllegalStateException("getPose(" + partName + ") returned null");
            }
        }
        if (skipMissingParts) {
            if (null != nextPlacePartAction) {
                String slot = nextPlacePartAction.getArgs()[0];
                PoseType slotPose = getPose(slot);
                if (null == slotPose) {
                    lastTakenPart = null;
                    takeSnapshots("plan", "skipping-take-part-next-slot-not-available-" + partName + "", pose, partName);
                    return;
                }
            }
        }
        pose = visionToRobotPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        takePartByPose(out, pose);
        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, (CrclCommandWrapper ccw) -> {
            System.out.println(markerMsg + " at " + new Date());
            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
        });
        lastTakenPart = partName;
        //inspectionList.add(partName);
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
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
        msg.setMessage("fake-take-part " + partName + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(msg);
        out.add(msg);

        PoseType pose = getPose(partName);
        pose = visionToRobotPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        fakeTakePartByPose(out, pose);
        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, x -> {
            System.out.println(markerMsg + " at " + new Date());
            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
        });
        lastTakenPart = partName;
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
    }

    public void takePartRecovery(String partName, List<MiddleCommandType> out) throws SQLException, BadLocationException {
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }

        if (partName.indexOf('_') < 0) {
            throw new IllegalArgumentException("partName must contain an underscore: partName=" + partName);
        }
        checkSettings();
        MessageType msg = new MessageType();
        msg.setMessage("take-part-recovery " + partName + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(msg);
        out.add(msg);

        PoseType pose = getPose(partName);
        if (takeSnapshots) {
            takeSnapshots("plan", "take-part-recovery-" + partName + "", pose, partName);
        }
        if (null == pose) {
            if (skipMissingParts) {
                lastTakenPart = null;
                return;
            } else {
                throw new IllegalStateException("getPose(" + partName + ") returned null");
            }
        }

        pose = visionToRobotPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        takePartByPose(out, pose);

        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, x -> {
            System.out.println(markerMsg + " at " + new Date());
            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
        });
        lastTakenPart = partName;
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
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
    public PoseType getPose(String posename) throws SQLException, IllegalStateException {
        final AtomicReference<Exception> getNewPoseFromDbException = new AtomicReference<>();
        synchronized (poseCache) {
            PoseType pose = poseCache.computeIfAbsent(posename,
                    (String key) -> {
                        try {
                            return getNewPoseFromDb(key);
                        } catch (Exception ex) {
                            getNewPoseFromDbException.set(ex);
                        }
                        return null;
                    }
            );
            Exception ex = getNewPoseFromDbException.getAndSet(null);
            if (null != ex) {
                if (ex instanceof SQLException) {
                    throw new SQLException(ex);
                } else if (ex instanceof RuntimeException) {
                    throw new RuntimeException(ex);
                } else if (ex instanceof IllegalStateException) {
                    throw new IllegalStateException(ex);
                }
                throw new IllegalStateException(ex);
            }
            if (null != pose) {
                for (Entry<String, PoseType> entry : poseCache.entrySet()) {
                    if (entry.getKey().equals(posename)) {
                        continue;
                    }
                    PointType point = pose.getPoint();
                    PointType entryPoint = entry.getValue().getPoint();
                    double diff = CRCLPosemath.diffPoints(point, entryPoint);
                    if (diff < 15.0) {
                        String errMsg = "two poses in cache are too close : posename=" + posename + ",pose=" + CRCLPosemath.toString(point) + ", entry=" + entry + ", entryPoint=" + CRCLPosemath.toString(entryPoint);
                        takeSnapshots("err", errMsg, pose, posename);
                        throw new IllegalStateException("two poses in cache are too close : posename=" + posename + ",pose=" + CRCLPosemath.toString(point) + ", entry=" + entry + ", entryPoint=" + CRCLPosemath.toString(entryPoint));
                    }
                }
            } else {
                System.err.println("getPose(" + posename + ") returning null.");
                PoseType poseCheck = debugGetNewPoseFromDb(posename);
                System.out.println("poseCheck = " + poseCheck);
            }
            return pose;
        }
    }

    private PoseType getNewPoseFromDb(String posename) throws SQLException {
        qs.setAprsJFrame(aprsJFrame);
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
        return pose;
    }

    private PoseType debugGetNewPoseFromDb(String posename) throws SQLException {
        boolean origDebug = qs.isDebug();
        qs.setDebug(true);
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
        qs.setDebug(origDebug);
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

    volatile PoseType lastTestApproachPose = null;

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

        if (null != lastTestApproachPose) {
            addSetSlowSpeed(cmds);
            addMoveTo(cmds, lastTestApproachPose, false);
            lastTestApproachPose = null;
        } else {
            addSlowLimitedMoveUpFromCurrent(cmds);
        }
        PoseType approachPose = CRCLPosemath.copy(pose);
        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);
        lastTestApproachPose = approachPose;

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ() + takeZOffset);

        addSetFastTestSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowTestSpeed(cmds);

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

    private void addCheckedOpenGripper(List<MiddleCommandType> cmds) {
        addOptionalOpenGripper(cmds, (CrclCommandWrapper ccw) -> {
            if (aprsJFrame.isObjectViewSimulated()) {
                double distToPart = aprsJFrame.getClosestRobotPartDistance();
                if (distToPart < dropOffMin) {
                    String errString
                            = "Can't take part when distance of " + distToPart + "  less than  " + dropOffMin;
                    double recheckDistance = aprsJFrame.getClosestRobotPartDistance();
                    System.out.println("recheckDistance = " + recheckDistance);
                    this.aprsJFrame.setTitleErrorString(errString);
                    this.aprsJFrame.pause();
                    return;
                }
            }
        });
    }

    private double dropOffMin = 25;

    /**
     * Get the value of dropOffMin
     *
     * @return the value of dropOffMin
     */
    public double getDropOffMin() {
        return dropOffMin;
    }

    /**
     * Set the value of dropOffMin
     *
     * @param dropOffMin new value of dropOffMin
     */
    public void setDropOffMin(double dropOffMin) {
        this.dropOffMin = dropOffMin;
    }

    private double pickupDistMax = 25;

    /**
     * Get the value of pickupDistMax
     *
     * @return the value of pickupDistMax
     */
    public double getPickupDistMax() {
        return pickupDistMax;
    }

    /**
     * Set the value of pickupDistMax
     *
     * @param pickupDistMax new value of pickupDistMax
     */
    public void setPickupDistMax(double pickupDistMax) {
        this.pickupDistMax = pickupDistMax;
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
        lastTestApproachPose = null;

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ() + takeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true);

        addSettleDwell(cmds);

        addOptionalCloseGripper(cmds, (CrclCommandWrapper ccw) -> {
            if (aprsJFrame.isObjectViewSimulated()) {
                double distToPart = aprsJFrame.getClosestRobotPartDistance();
                if (distToPart > pickupDistMax) {
                    String errString
                            = "Can't take part when distance of " + distToPart + "  exceeds " + pickupDistMax;
                    this.aprsJFrame.setTitleErrorString(errString);
                    this.aprsJFrame.pause();
                    SetEndEffectorType seeCmd = (SetEndEffectorType) ccw.getWrappedCommand();
                    seeCmd.setSetting(1.0);
                    setFakeTakePart(false);
                    return;
                }
            }
            if (getForceFakeTakeFlag()) {
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
                logger.log(Level.SEVERE, null, ex);
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

        String skipLookDwellTimeString = options.get("skipLookDwellTime");
        if (null != skipLookDwellTimeString && skipLookDwellTimeString.length() > 0) {
            try {
                skipLookDwellTime = Double.parseDouble(skipLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        String afterMoveToLookForDwellTimeString = options.get("afterMoveToLookForDwellTime");
        if (null != afterMoveToLookForDwellTimeString && afterMoveToLookForDwellTimeString.length() > 0) {
            try {
                afterMoveToLookForDwellTime = Double.parseDouble(afterMoveToLookForDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                numberFormatException.printStackTrace();
            }
        }

        // afterMoveToLookForDwellTime
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

        String testTransSpeedString = options.get("testTransSpeed");
        if (null != testTransSpeedString && testTransSpeedString.length() > 0) {
            try {
                testTransSpeed = Double.parseDouble(testTransSpeedString);
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

        String kitInspectDistThresholdString = options.get("kitInspectDistThreshold");
        if (null != kitInspectDistThresholdString && kitInspectDistThresholdString.length() > 0) {
            try {
                kitInspectDistThreshold = Double.parseDouble(kitInspectDistThresholdString);
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
        setCommandId(openGripperCmd);
        openGripperCmd.setSetting(1.0);
        cmds.add(openGripperCmd);

    }

    private void addOptionalOpenGripper(List<MiddleCommandType> cmds, CRCLCommandWrapperConsumer cb) {
        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
        setCommandId(openGripperCmd);
        openGripperCmd.setSetting(1.0);
        addOptionalCommand(openGripperCmd, cmds, cb);
    }

    PoseType copyAndAddZ(PoseType pose_in, double offset, double limit) {
        PoseType out = CRCLPosemath.copy(aprsJFrame.getCurrentPose());
        out.getPoint().setZ(Math.min(limit, out.getPoint().getZ() + offset));
        return out;
    }

    private void addMoveUpFromCurrent(List<MiddleCommandType> cmds, double offset, double limit) {

        MessageType origMessageCmd = new MessageType();
        origMessageCmd.setMessage("moveUpFromCurrent" + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        addOptionalCommand(origMessageCmd, cmds, (CrclCommandWrapper wrapper) -> {
            MiddleCommandType cmd = wrapper.getWrappedCommand();
            if (cmd instanceof MoveToType) {
                MoveToType mtCmd = (MoveToType) cmd;
                PoseType pose = aprsJFrame.getCurrentPose();
                if (pose == null || pose.getPoint() == null || pose.getPoint().getZ() >= (limit - 1e-6)) {
                    MessageType messageCommand = new MessageType();
                    messageCommand.setMessage("moveUpFromCurrent NOT needed." + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
                    wrapper.setWrappedCommand(messageCommand);
                } else {
                    MoveToType moveToCmd = new MoveToType();
                    moveToCmd.setEndPosition(copyAndAddZ(pose, offset, limit));
                    moveToCmd.setMoveStraight(true);
                    wrapper.setWrappedCommand(moveToCmd);
                }
            }
        });
    }

    private void addOptionalCloseGripper(List<MiddleCommandType> cmds, CRCLCommandWrapperConsumer cb) {
        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        closeGrippeerCmd.setSetting(0.0);
        addOptionalCommand(closeGrippeerCmd, cmds, cb);
    }

    private void addCloseGripper(List<MiddleCommandType> cmds) {
        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        setCommandId(closeGrippeerCmd);
        closeGrippeerCmd.setSetting(0.0);
        cmds.add(closeGrippeerCmd);
    }

    private void addMoveTo(List<MiddleCommandType> cmds, PoseType poseAbove, boolean straight) {
        MoveToType moveAboveCmd = new MoveToType();
        setCommandId(moveAboveCmd);
        moveAboveCmd.setEndPosition(poseAbove);
        moveAboveCmd.setMoveStraight(straight);
        cmds.add(moveAboveCmd);
        atLookForPosition = false;
    }

    private void addSetSlowSpeed(List<MiddleCommandType> cmds) {
        SetTransSpeedType stst = new SetTransSpeedType();
        setCommandId(stst);
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
            setCommandId(srs);
            srs.setRotSpeed(rsa);
            cmds.add(srs);
            rotSpeedSet = true;
        }

        SetTransSpeedType stst = new SetTransSpeedType();
        setCommandId(stst);
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(fastTransSpeed);
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private void addSetFastTestSpeed(List<MiddleCommandType> cmds) {

        if (!rotSpeedSet) {
            SetRotSpeedType srs = new SetRotSpeedType();
            RotSpeedAbsoluteType rsa = new RotSpeedAbsoluteType();
            rsa.setSetting(rotSpeed);
            setCommandId(srs);
            srs.setRotSpeed(rsa);
            cmds.add(srs);
            rotSpeedSet = true;
        }

        SetTransSpeedType stst = new SetTransSpeedType();
        setCommandId(stst);
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(Math.min(fastTransSpeed, testTransSpeed));
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private void addSetSlowTestSpeed(List<MiddleCommandType> cmds) {

        if (!rotSpeedSet) {
            SetRotSpeedType srs = new SetRotSpeedType();
            RotSpeedAbsoluteType rsa = new RotSpeedAbsoluteType();
            rsa.setSetting(rotSpeed);
            setCommandId(srs);
            srs.setRotSpeed(rsa);
            cmds.add(srs);
            rotSpeedSet = true;
        }

        SetTransSpeedType stst = new SetTransSpeedType();
        setCommandId(stst);
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(Math.min(slowTransSpeed, testTransSpeed));
        stst.setTransSpeed(tas);
        cmds.add(stst);
    }

    private boolean unitsSet = false;

    private void addSetUnits(List<MiddleCommandType> cmds) {
        if (!unitsSet) {
            SetLengthUnitsType slu = new SetLengthUnitsType();
            slu.setUnitName(LengthUnitEnumType.MILLIMETER);
            setCommandId(slu);
            cmds.add(slu);

            SetAngleUnitsType sau = new SetAngleUnitsType();
            sau.setUnitName(AngleUnitEnumType.DEGREE);
            setCommandId(sau);
            cmds.add(sau);
            unitsSet = true;
        }
    }

    PointType getLookForXYZ() {
        if (null == options) {
            logger.warning("getLookForXYZ : null == options");
            return null;
        }
        String lookforXYZSring = options.get("lookForXYZ");
        if (null == lookforXYZSring) {
            return null;
        }
        String lookForXYZFields[] = lookforXYZSring.split(",");
        if (lookForXYZFields.length < 3) {
            return null;
        }
        return point(Double.parseDouble(lookForXYZFields[0]), Double.parseDouble(lookForXYZFields[1]), Double.parseDouble(lookForXYZFields[2]));
    }
    private final Logger logger = Logger.getLogger(PddlActionToCrclGenerator.class.getName());

    private volatile boolean atLookForPosition = false;

    public void addMoveToLookForPosition(List<MiddleCommandType> out) {

        String useLookForJointString = options.get("useJointLookFor");
        boolean useLookForJoint = (null != useLookForJointString && useLookForJointString.length() > 0 && Boolean.valueOf(useLookForJointString));
        String lookForJointsString = options.get("lookForJoints");
        if (null == lookForJointsString || lookForJointsString.length() < 1) {
            useLookForJoint = false;
        }

        addOpenGripper(out);
        addSlowLimitedMoveUpFromCurrent(out);
        addSetFastSpeed(out);
        if (!useLookForJoint) {
            PoseType pose = new PoseType();
            PointType pt = getLookForXYZ();
            if (null == pt) {
                throw new IllegalStateException("getLookForXYZ() returned null: options.get(\"lookForXYZ\") = " + options.get("lookForXYZ"));
            }
            pose.setPoint(pt);
            pose.setXAxis(xAxis);
            pose.setZAxis(zAxis);
            addMoveTo(out, pose, false);
        } else {
            addJointMove(out, lookForJointsString);
        }
        addMarkerCommand(out, "set atLookForPosition true", x -> {
            atLookForPosition = true;
        });
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
        setCommandId(ajCmd);
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
        atLookForPosition = false;
    }

    private volatile Thread clearPoseCacheThread = null;
    private volatile StackTraceElement clearPoseCacheTrace[] = null;

    public void clearPoseCache() {
        clearPoseCacheThread = Thread.currentThread();
        clearPoseCacheTrace = clearPoseCacheThread.getStackTrace();
        synchronized (poseCache) {
            poseCache.clear();
        }
    }

    public Map<String, PoseType> getPoseCache() {
        synchronized (poseCache) {
            return Collections.unmodifiableMap(poseCache);
        }
    }

    public boolean checkAtLookForPosition() {
        checkSettings();
        String useLookForJointString = options.get("useJointLookFor");
        boolean useLookForJoint = (null != useLookForJointString && useLookForJointString.length() > 0 && Boolean.valueOf(useLookForJointString));
        String lookForJointsString = options.get("lookForJoints");
        if (null == lookForJointsString || lookForJointsString.length() < 1) {
            useLookForJoint = false;
        }
        if (!useLookForJoint) {
            PointType lookForPoint = getLookForXYZ();
            if (null == lookForPoint) {
                throw new IllegalStateException("getLookForXYZ() returned null: options.get(\"lookForXYZ\") = " + options.get("lookForXYZ"));
            }
            PointType currentPoint = aprsJFrame.getCurrentPosePoint();
            if (null == currentPoint) {
//                System.err.println("checkAtLookForPosition: getCurrentPosePoint() returned null");
                return false;
            }
            double diff = CRCLPosemath.diffPoints(currentPoint, lookForPoint);
            return diff < 2.0;
        } else {
            CRCLStatusType curStatus = aprsJFrame.getCurrentStatus();
            if (curStatus == null) {
                return false;
            }
            JointStatusesType jss = curStatus.getJointStatuses();
            if (jss == null) {
                return false;
            }
            List<JointStatusType> l = jss.getJointStatus();
            String jointPosStrings[] = lookForJointsString.split("[,]+");
            for (int i = 0; i < jointPosStrings.length; i++) {
                final int number = i + 1;
                JointStatusType js = l.stream().filter(x -> x.getJointNumber() == number).findFirst().orElse(null);
                if (null == js) {
                    return false;
                }
                double jpos = Double.parseDouble(jointPosStrings[i]);
                if (Math.abs(jpos - js.getJointPosition()) > 2.0) {
                    return false;
                }
            }
            return true;
        }
    }

    private Map<String, Integer> lastRequiredPartsMap = null;

    public void clearLastRequiredPartsMap() {
        if (null != lastRequiredPartsMap) {
            lastRequiredPartsMap.clear();
        }
        lastRequiredPartsMap = null;
    }

    private void endProgram(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (atLookForPosition) {
            atLookForPosition = checkAtLookForPosition();
        }
        if (!atLookForPosition) {
            addMoveToLookForPosition(out);
        }
        TakenPartList.clear();
    }

    private void lookForParts(PddlAction action, List<MiddleCommandType> out, boolean firstAction, boolean lastAction) throws IllegalStateException, SQLException {

        lastTestApproachPose = null;
        checkSettings();
        if (null == qs) {
            throw new IllegalStateException("Database not setup and connected.");
        }
        if (null == kitInspectionJInternalFrame) {
            kitInspectionJInternalFrame = aprsJFrame.getKitInspectionJInternalFrame();
        }

        Map<String, Integer> requiredPartsMap = new HashMap<>();
        if (null != action.getArgs()) {
            for (int i = 0; i < action.getArgs().length; i++) {
                String arg = action.getArgs()[i];
                int eindex = arg.indexOf('=');
                if (eindex > 0) {
                    String name = arg.substring(0, eindex);
                    String valString = arg.substring(eindex + 1);
                    requiredPartsMap.put(name, Integer.valueOf(valString));
                }
            }
        }

        if (null != lastRequiredPartsMap && requiredPartsMap.isEmpty()) {
            requiredPartsMap.putAll(lastRequiredPartsMap);
        } else if (!requiredPartsMap.isEmpty()) {
            lastRequiredPartsMap = requiredPartsMap;
        }
        final Map<String, Integer> immutableRequiredPartsMap = Collections.unmodifiableMap(requiredPartsMap);
        addMarkerCommand(out, "clearPoseCache", x -> {
            clearPoseCache();
        });
        if (atLookForPosition) {
            atLookForPosition = checkAtLookForPosition();
        }
        if (!atLookForPosition) {
            addMoveToLookForPosition(out);
            addAfterMoveToLookForDwell(out);
            addMarkerCommand(out, "enableVisionToDatabaseUpdates", x -> {
                aprsJFrame.setEnableVisionToDatabaseUpdates(true, immutableRequiredPartsMap);
            });
            if (firstAction) {
                addFirstLookDwell(out);
            } else if (lastAction) {
                addLastLookDwell(out);
            } else {
                addLookDwell(out);
            }
        } else {
            addMarkerCommand(out, "enableVisionToDatabaseUpdates", x -> {
                aprsJFrame.setEnableVisionToDatabaseUpdates(true, immutableRequiredPartsMap);
            });
            addSkipLookDwell(out);
        }

        addMarkerCommand(out, "lookForParts.waitForCompleteVisionUpdates", x -> {
            try {
                waitForCompleteVisionUpdates("lookForParts", immutableRequiredPartsMap);
            } catch (InterruptedException | ExecutionException | IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        });
        addTakeSnapshots(out, "lookForParts-" + ((action.getArgs().length == 1) ? action.getArgs()[0] : ""), null, "", this.crclNumber.get());
        if (action.getArgs().length >= 1) {
            if (action.getArgs()[0].startsWith("1")) {
                addMarkerCommand(out, "Inspecting kit", x -> {
                    kitInspectionJInternalFrameKitTitleLabelSetText("Inspecting kit");
                    addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR:" + messageColorH3 + "\">&nbsp;&nbsp;Inspecting kit</h2>");
                });

            } else if (action.getArgs()[0].startsWith("0")) {
                addMarkerCommand(out, "Building kit", x -> {
                    kitInspectionJInternalFrameKitTitleLabelSetText("Building kit");
                    addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;Building kit</h2>");
                });
            } else if (action.getArgs()[0].startsWith("2")) {
                addMarkerCommand(out, "All Tasks Completed", x -> {
                    kitInspectionJInternalFrameKitTitleLabelSetText("All Tasks Completed");
                    addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;All tasks completed</h2>");
                });
            }
        } else {
            TakenPartList.clear();
        }
    }

    private List<PhysicalItem> waitForCompleteVisionUpdates(String prefix, Map<String, Integer> requiredPartsMap) throws InterruptedException, ExecutionException, IOException {
        XFuture<List<PhysicalItem>> xfl = aprsJFrame.getSingleVisionToDbUpdate();
        aprsJFrame.refreshSimView();
        while (!xfl.isDone()) {
            if (!aprsJFrame.isEnableVisionToDatabaseUpdates()) {
                System.err.println("VisionToDatabaseUpdates not enabled as expected.");
                aprsJFrame.setEnableVisionToDatabaseUpdates(true, requiredPartsMap);
            }
            Thread.sleep(50);
            aprsJFrame.refreshSimView();
        }
        List<PhysicalItem> l = xfl.get();
        aprsJFrame.takeSimViewSnapshot(aprsJFrame.createTempFile(prefix + "_waitForCompleteVisionUpdates", ".PNG"), l);
//            aprsJFrame.getUpdatesFinished().join();
        takeDatabaseViewSnapshot(aprsJFrame.createTempFile(prefix + "_waitForCompleteVisionUpdates_new_database", ".PNG"));
        clearPoseCache();
        return l;
    }

    private void addSlowLimitedMoveUpFromCurrent(List<MiddleCommandType> out) {
        addSetSlowSpeed(out);
        double limit = Double.POSITIVE_INFINITY;
        PointType pt = getLookForXYZ();
        if (null != pt) {
            limit = pt.getZ();
        }
        addMoveUpFromCurrent(out, approachZOffset, limit);
    }

//    private void addOpenGripper(List<MiddleCommandType> out, PoseType pose) {
//        SetEndEffectorType openGripperCmd = new SetEndEffectorType();
//        openGripperCmd.setCommandID(BigInteger.valueOf(out.size() + 2));
//        openGripperCmd.setSetting(double.ONE);
//    }
    private void addLookDwell(List<MiddleCommandType> out) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
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
        private final int startSafeAbortRequestCount;

        public PlacePartInfo(PddlAction action, int pddlActionIndex, int outIndex, int startSafeAbortRequestCount) {
            this.action = action;
            this.pddlActionIndex = pddlActionIndex;
            this.outIndex = outIndex;
            this.startSafeAbortRequestCount = startSafeAbortRequestCount;
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

        public int getStartSafeAbortRequestCount() {
            return startSafeAbortRequestCount;
        }

        @Override
        public String toString() {
            return "{action(" + pddlActionIndex + ")=" + outIndex + ":" + action + '}';
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
        String slotName = action.getArgs()[placePartSlotArgIndex];

        placePartBySlotName(slotName, out, action);

    }

    private volatile int startSafeAbortRequestCount;

    public void placePartBySlotName(String slotName, List<MiddleCommandType> out, PddlAction action) throws IllegalStateException, SQLException {
        PoseType pose = getPose(slotName);
        if (null != pose) {
            PlacePartSlotPoseList.add(pose);
        }
        if (skipMissingParts && lastTakenPart == null) {
            takeSnapshots("plan", "skipping-place-part-" + getLastTakenPart() + "-in-" + slotName + "", pose, slotName);
            PoseType poseCheck = getPose(slotName);
            System.out.println("poseCheck = " + poseCheck);
            return;
        }
        final String msg = "placed part " + getLastTakenPart() + " in " + slotName;
        if (takeSnapshots) {
            takeSnapshots("plan", "place-part-" + getLastTakenPart() + "in-" + slotName + "", pose, slotName);
        }
        if (pose == null) {
            if (skipMissingParts && null != lastTakenPart) {
                PoseType origPose = poseCache.get(lastTakenPart);
                if (null != origPose) {
                    origPose = visionToRobotPose(origPose);
                    origPose.setXAxis(xAxis);
                    origPose.setZAxis(zAxis);
                    placePartByPose(out, origPose);
                    takeSnapshots("plan", "returning-" + getLastTakenPart() + "_no_pose_for_" + slotName, origPose, lastTakenPart);
                    final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount);
                    addMarkerCommand(out, msg,
                            ((CrclCommandWrapper wrapper) -> {
                                addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                                System.out.println(msg + " completed at " + new Date());
                                ppi.setWrapper(wrapper);
                                notifyPlacePartConsumers(ppi);
                            }));
                    return;
                }
            }
            throw new IllegalStateException("getPose(" + slotName + ") returned null");
        }
        pose = visionToRobotPose(pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        placePartByPose(out, pose);
        final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount);
        addMarkerCommand(out, msg,
                ((CrclCommandWrapper wrapper) -> {
                    addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                    System.out.println(msg + " completed at " + new Date());
                    ppi.setWrapper(wrapper);
                    notifyPlacePartConsumers(ppi);
                }));
        return;
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

        final String msg = "placed part (recovery) in " + slotName;
        if (takeSnapshots) {
            if (takeSnapshots) {
                takeSnapshots("plan", "place-part-recovery-in-" + slotName + "", pose, slotName);
            }
        }
        if (pose == null) {
            throw new IllegalStateException("getPose(" + slotName + ") returned null");
        }

        pose = visionToRobotPose(pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        PlacePartSlotPoseList.add(pose);
        placePartByPose(out, pose);
        final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount);
        addMarkerCommand(out, msg,
                ((CrclCommandWrapper wrapper) -> {
                    System.out.println(msg + " completed at " + new Date());
                    ppi.setWrapper(wrapper);
                    notifyPlacePartConsumers(ppi);
                    addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
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

        PoseType approachPose = CRCLPosemath.copy(pose);
        lastTestApproachPose = null;

        //System.out.println("Z= " + pose.getPoint().getZ());
        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);

        PoseType placePose = CRCLPosemath.copy(pose);
        placePose.getPoint().setZ(pose.getPoint().getZ() + placeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, placePose, true);

        addSettleDwell(cmds);

        addCheckedOpenGripper(cmds);

        addSettleDwell(cmds);

        addMoveTo(cmds, approachPose, true);

        addSettleDwell(cmds);

        this.lastTakenPart = null;
    }

    private void addSettleDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(settleDwellTime);
        cmds.add(dwellCmd);
    }
// addAfterMoveToLookForDwell

    private void addAfterMoveToLookForDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(afterMoveToLookForDwellTime);
        cmds.add(dwellCmd);
    }

    private void addSkipLookDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(skipLookDwellTime);
        cmds.add(dwellCmd);
    }

    private void addFirstLookDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(firstLookDwellTime);
        cmds.add(dwellCmd);
    }

    private void addLastLookDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(lastLookDwellTime);
        cmds.add(dwellCmd);
    }

    private void addMarkerCommand(List<MiddleCommandType> cmds, String message, CRCLCommandWrapperConsumer cb) {
        MessageType messageCmd = new MessageType();
        messageCmd.setMessage(message + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(messageCmd);
        CrclCommandWrapper wrapper = CrclCommandWrapper.wrapWithOnDone(messageCmd, cb);
        cmds.add(wrapper);
    }

    private void addOptionalCommand(MiddleCommandType optCmd, List<MiddleCommandType> cmds, CRCLCommandWrapperConsumer cb) {
        setCommandId(optCmd);
        CrclCommandWrapper wrapper = CrclCommandWrapper.wrapWithOnStart(optCmd, cb);
        wrapper.setCommandID(optCmd.getCommandID());
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
        private final CrclCommandWrapper wrapper;
        private final List<PddlAction> actions;
        private final List<PddlAction> origActions;
        private final int actionsSize;

        public ActionCallbackInfo(int actionIndex, PddlAction action, CrclCommandWrapper wrapper, List<PddlAction> actions, List<PddlAction> origActions) {
            this.actionIndex = actionIndex;
            this.action = action;
            this.wrapper = wrapper;
            this.actions = actions;
            this.origActions = origActions;
            actionsSize = this.actions.size();
        }

        public int getActionsSize() {
            return actionsSize;
        }

        public int getActionIndex() {
            return actionIndex;
        }

        public PddlAction getAction() {
            return action;
        }

        public CrclCommandWrapper getWrapper() {
            return wrapper;
        }

        public List<PddlAction> getActions() {
            return actions;
        }

        public List<PddlAction> getOrigActions() {
            return origActions;
        }

        @Override
        public String toString() {
            return "ActionCallbackInfo{" + "actionIndex=" + actionIndex + ", action=" + action + ", wrapper=" + wrapper + ", actions=" + actions + '}';
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

    private AtomicReference<ActionCallbackInfo> lastAcbi = new AtomicReference<>();

    private void notifyActionCompletedListeners(int actionIndex, PddlAction action, CrclCommandWrapper wrapper, List<PddlAction> actions, List<PddlAction> origActions) {
        ActionCallbackInfo acbi = new ActionCallbackInfo(actionIndex, action, wrapper, actions, origActions);
        lastAcbi.set(acbi);
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
