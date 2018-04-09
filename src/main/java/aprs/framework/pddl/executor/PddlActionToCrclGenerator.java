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
import static aprs.framework.optaplanner.actionmodel.OpActionType.END;
import static aprs.framework.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.framework.optaplanner.actionmodel.OpActionType.START;
import aprs.framework.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import crcl.base.ActuateJointType;
import crcl.base.ActuateJointsType;
import crcl.base.AngleUnitEnumType;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLStatusType;
import crcl.base.CloseToolChangerType;
import crcl.base.DwellType;
import crcl.base.JointSpeedAccelType;
import crcl.base.JointStatusType;
import crcl.base.JointStatusesType;
import crcl.base.LengthUnitEnumType;
import crcl.base.MessageType;
import crcl.base.MiddleCommandType;
import crcl.base.MoveToType;
import crcl.base.OpenToolChangerType;
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
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLException;
import crcl.utils.CrclCommandWrapper;
import crcl.utils.CRCLPosemath;
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
import crcl.utils.CRCLSocket;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.xml.sax.SAXException;
import rcs.posemath.PmException;
import rcs.posemath.PmPose;
import rcs.posemath.Posemath;

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

    public List<PddlAction> opActionsToPddlActions(OpActionPlan plan, int start) {
        List<? extends OpAction> listIn = plan.getEffectiveOrderedList(false);
        List<PddlAction> ret = new ArrayList<>();
        double accelleration = 100.0;
        double maxSpeed = fastTransSpeed;
        OUTER:
        for (int i = start; i < listIn.size(); i++) {
            OpAction opa = listIn.get(i);
            if (null != opa.getActionType()) {
                switch (opa.getActionType()) {
                    case START:
                    case FAKE_DROPOFF:
                        continue;
                        
                    case FAKE_PICKUP:
                        i++;
                        continue;
                        
                    case END:
                        break OUTER;
                    default:
                        break;
                }
            }
            if (i < listIn.size() - 1
                    && opa.getActionType() == PICKUP
                    && listIn.get(i + 1).getActionType() == FAKE_DROPOFF) {
                continue;
            }
            String name = opa.getName();
            if ("start".equals(name)) {
                continue;
            }
            int dindex = name.indexOf("-[");
            if (dindex > 0) {
                String type = name.substring(0, dindex);
                String args[] = name.substring(dindex + 2).split("[,{}\\-\\[\\]]+");
                ret.add(new PddlAction("", type, args, "" + opa.cost(plan)));
            } else {
                throw new IllegalArgumentException("i=" + i + ",name=" + name + ",opa=" + opa + ",listIn=" + listIn + ",start=" + start);
            }
        }
        return ret;
    }

    @Nullable
    private Solver<OpActionPlan> solver;

    /**
     * Get the value of solver
     *
     * @return the value of solver
     */
    @Nullable
    public Solver<OpActionPlan> getSolver() {
        return solver;
    }

    /**
     * Set the value of solver
     *
     * @param solver new value of solver
     */
    public void setSolver(@Nullable Solver<OpActionPlan> solver) {
        this.solver = solver;
    }

    public @Nullable
    List<OpAction> pddlActionsToOpActions(List<? extends PddlAction> listIn, int start) throws SQLException {
        return pddlActionsToOpActions(listIn, start, null, null, null);
    }

    private volatile int skippedActions = 0;

    private boolean getReverseFlag() {
        if (aprsJFrame != null) {
            return aprsJFrame.isReverseFlag();
        }
        return false;
    }

    private List<OpAction> pddlActionsToOpActions(
            List<? extends PddlAction> listIn, int start, int endl @Nullable [], @Nullable List<OpAction> skippedOpActionsList, @Nullable List<PddlAction> skippedPddlActionsList) throws SQLException {
        return pddlActionsToOpActions(
                listIn,
                start,
                endl,
                skippedOpActionsList,
                skippedPddlActionsList,
                getLookForXYZ(),
                takePartArgIndex,
                skipMissingParts,
                getReverseFlag(),
                this::getPose);
    }

    private static interface GetPoseFunction {

        @Nullable
        public PoseType apply(String name, boolean ignoreNull) throws SQLException;
    }

    boolean inKitTrayByName(String name) {
        return !name.endsWith("_in_pt") && !name.contains("_in_pt_") && 
                (name.contains("_in_kit_") || name.contains("_in_kt_") || name.endsWith("in_kt"));
    }
    
    private List<OpAction> pddlActionsToOpActions(
            List<? extends PddlAction> listIn,
            int start,
            int endl @Nullable [],
            @Nullable List<OpAction> skippedOpActionsList,
            @Nullable List<PddlAction> skippedPddlActionsList,
            @Nullable PointType lookForPt,
            int takePartArgIndex,
            boolean skipMissingParts,
            boolean reverseFlag,
            GetPoseFunction getPose) throws SQLException {
        List<OpAction> ret = new ArrayList<>();
        boolean moveOccurred = false;
        if (null == lookForPt) {
            throw new IllegalStateException("lookForPt == null");
        }
        ret.add(new OpAction("start", lookForPt.getX(), lookForPt.getY(), OpActionType.START, "NONE",true));
        boolean skipNextPlace = false;
        skippedActions = 0;
        OpAction takePartOpAction = null;
        PddlAction takePartPddlAction = null;
        for (int i = start; i < listIn.size(); i++) {
            PddlAction pa = listIn.get(i);

            switch (pa.getType()) {
                case "take-part":
                case "fake-take-part":
                case "test-part-position":
                    if (!moveOccurred) {
                        if (null != endl && endl.length > 0) {
                            endl[0] = i;
                        }
                        moveOccurred = true;
                    }
                    String partName = pa.getArgs()[takePartArgIndex];
                    PoseType partPose = getPose.apply(partName, reverseFlag);
                    if (null == partPose) {
                        if (skipMissingParts) {
                            skippedActions++;
                            if (null != skippedPddlActionsList) {
                                skippedPddlActionsList.add(pa);
                            }
                            skipNextPlace = true;
                            takePartOpAction = null;
                            takePartPddlAction = pa;
                        } else {
                            throw new IllegalStateException("null == partPose: partName=" + partName + ",start=" + start + ",i=" + i + ",pa=" + pa);
                        }
                    } else {
                        PointType partPt = partPose.getPoint();
                        takePartOpAction = new OpAction(pa.toString(), partPt.getX(), partPt.getY(), OpActionType.PICKUP, posNameToType(partName),inKitTrayByName(partName));
                        takePartPddlAction = pa;
                        skipNextPlace = false;
                    }
                    break;

                case "place-part":
                    if (!moveOccurred) {
                        if (null != endl && endl.length > 0) {
                            endl[0] = i;
                        }
                        moveOccurred = true;
                    }
                    if (null == takePartOpAction
                            || null == takePartPddlAction
                            || skipNextPlace) {
                        if (skipMissingParts) {
                            skippedActions++;
                            if (null != skippedPddlActionsList) {
                                skippedPddlActionsList.add(pa);
                            }
                            skipNextPlace = true;
                        } else {
                            throw new IllegalStateException("takePart not set :,start=" + start + ",i=" + i + ",pa=" + pa);
                        }
                    } else {
                        String slotName = pa.getArgs()[placePartSlotArgIndex];
                        PoseType slotPose = getPose.apply(slotName, reverseFlag);
                        if (null == slotPose) {
                            if (skipMissingParts) {
                                skippedActions += 2;
                                if (null != skippedPddlActionsList) {
                                    skippedPddlActionsList.add(takePartPddlAction);
                                    skippedPddlActionsList.add(pa);
                                }
                                if (null != skippedOpActionsList) {
                                    skippedOpActionsList.add(takePartOpAction);
                                }
                            } else {
                                throw new IllegalStateException("null == slotPose: slotName=" + slotName + ",start=" + start + ",i=" + i + ",pa=" + pa);
                            }
                        } else {
                            PointType slotPt = slotPose.getPoint();
                            OpAction placePartOpAction = new OpAction(pa.toString(), slotPt.getX(), slotPt.getY(), OpActionType.DROPOFF, posNameToType(slotName),inKitTrayByName(slotName));
                            ret.add(takePartOpAction);
                            ret.add(placePartOpAction);
                        }
                    }
                    skipNextPlace = false;
                    takePartPddlAction = null;
                    takePartOpAction = null;
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

    private java.sql.@MonotonicNonNull Connection dbConnection;
    private @MonotonicNonNull
    DbSetup dbSetup;
    private boolean closeDbConnection = true;
    private @MonotonicNonNull
    QuerySet qs;
    private final List<String> TakenPartList = new ArrayList<>();
    private @MonotonicNonNull
    Set<Slot> EmptySlotSet;
    private final List<PoseType> PlacePartSlotPoseList = new ArrayList<>();
    private boolean takeSnapshots = false;
    private final AtomicInteger crclNumber = new AtomicInteger();
    private final ConcurrentMap<String, PoseType> poseCache = new ConcurrentHashMap<>();
    private volatile @MonotonicNonNull
    KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

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

    static private class KitToCheckInstanceInfo {

        final String instanceName;
        final Map<String, PhysicalItem> closestItemMap;
        final List<Slot> absSlots;
        final Map<String, String> itemSkuMap;

        private @Nullable
        String failedAbsSlotPrpName;
        private @Nullable
        String failedItemSkuName;
        int failedSlots;

        public int getFailedSlots() {
            return failedSlots;
        }

        public KitToCheckInstanceInfo(String instanceName, List<Slot> absSlots) {
            this.instanceName = instanceName;
            this.absSlots = absSlots;
            closestItemMap = new HashMap<>();
            itemSkuMap = new HashMap<>();
        }

        @Override
        public String toString() {
            return "KitToCheckInstanceInfo{" + "instanceName=" + instanceName + ", failedAbsSlotPrpName=" + failedAbsSlotPrpName + ", failedItemSkuName=" + failedItemSkuName + '}';
        }

    }

    static private class KitToCheck {

        final String name;
        final Map<String, String> slotMap;

        @Nullable
        List<String> kitInstanceNames;

        Map<String, KitToCheckInstanceInfo> instanceInfoMap = Collections.emptyMap();

        public KitToCheck(String name, Map<String, String> slotMap) {
            this.name = name;
            this.slotMap = slotMap;
        }

        @Override
        public String toString() {
            return "KitToCheck{" + "name=" + name + ", instanceInfoMap=" + instanceInfoMap + '}';
        }

        int getMaxDiffFailedSlots() {
            int minFailedSlots = instanceInfoMap
                    .values()
                    .stream()
                    .mapToInt(KitToCheckInstanceInfo::getFailedSlots)
                    .min()
                    .orElse(0);
            int maxFailedSlots = instanceInfoMap
                    .values()
                    .stream()
                    .mapToInt(KitToCheckInstanceInfo::getFailedSlots)
                    .max()
                    .orElse(0);
            int diff = maxFailedSlots - minFailedSlots;
            System.out.println("diff = " + diff);
            return diff;
        }

    }
    private final ConcurrentLinkedDeque<KitToCheck> kitsToCheck = new ConcurrentLinkedDeque<>();

    @Nullable
    private PartsTray correctPartsTray = null;

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

    private List<PositionMap> positionMaps = Collections.emptyList();

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

    public static interface PoseProvider {

        public List<PhysicalItem> getNewPhysicalItems();

        @Nullable
        public PoseType getPose(String name);

        public List<String> getInstanceNames(String skuName);
    }

    private @Nullable
    PoseProvider externalPoseProvider = null;

    /**
     * Get the value of externalPoseProvider
     *
     * @return the value of externalPoseProvider
     */
    public @Nullable
    PoseProvider getExternalPoseProvider() {
        return externalPoseProvider;
    }

    /**
     * Set the value of externalPoseProvider
     *
     * @param externalPoseProvider new value of externalPoseProvider
     */
    public void setExternalPoseProvider(PoseProvider externalPoseProvider) {
        this.externalPoseProvider = externalPoseProvider;
    }

    /**
     * Check if this is connected to the database.
     *
     * @return is this generator connected to the database.
     */
    public synchronized boolean isConnected() {
        if (null != externalPoseProvider) {
            return true;
        }
        try {
            if (null == dbConnection) {
                return false;
            }
            if (dbConnection.isClosed()) {
                return false;
            }
            if (null == qs) {
                return false;
            }
            if (dbConnection != qs.getDbConnection()) {
                return false;
            }
            if (!qs.isConnected()) {
                return false;
            }
            return true;
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
    public java.sql.@Nullable Connection getDbConnection() {
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
        if (null != qs) {
            this.qs.setDebug(debug);
        }
    }

    /**
     * Set the database connection to use.
     *
     * @param dbConnection new database connection to use
     */
    public synchronized void setDbConnection(java.sql.@Nullable Connection dbConnection) {
        try {
            if (null != this.dbConnection && dbConnection != this.dbConnection && closeDbConnection) {
                try {
                    if (!this.dbConnection.isClosed()) {
                        this.dbConnection.close();
                    }
                    if (null != qs) {
                        qs.close();
                    }
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            if (null != dbConnection) {
                this.dbConnection = dbConnection;
            }
            if (null != dbConnection && null != dbSetup) {
                qs = new QuerySet(dbSetup.getDbType(), dbConnection, dbSetup.getQueriesMap());
            } else if (qs != null) {
                qs.close();
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
    @Nullable
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
                            (java.sql.Connection c, Throwable ex) -> {
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
                                        setTitleErrorString("Database error: " + ex.toString());
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

    private @Nullable
    String actionToCrclTakenPartsNames[] = new String[0];
    private int visionCycleNewDiffThreshold = 3;

    /**
     * Get an array of strings and null values relating each action to the last
     * part expected to have been taken after that action.
     *
     * @return array of taken part names
     */
    public @Nullable
    String[] getActionToCrclTakenPartsNames() {
        return actionToCrclTakenPartsNames;
    }

    private int actionToCrclIndexes[] = new int[0];

    /**
     * Get an array of indexes into the CRCL program associated with each PDDL
     * action.
     *
     * @return array of indexes into the CRCL program
     */
    public int[] getActionToCrclIndexes() {
        return actionToCrclIndexes;
    }

    private String actionToCrclLabels[] = new String[0];

    /**
     * Get an array of strings with labels for each PDDL action.
     *
     * @return array of labels
     */
    public String[] getActionToCrclLabels() {
        return actionToCrclLabels;
    }

    private Map<String, String> options = Collections.emptyMap();

    private final AtomicInteger lastIndex = new AtomicInteger();
    @Nullable
    private volatile List<PddlAction> lastActionsList = null;

    private volatile int lastAtLastIndexIdx = -1;
    @Nullable
    private volatile List<PddlAction> lastAtLastIndexList = null;
    private volatile int lastAtLastIndexRepPos = -1;

    public boolean atLastIndex() {
        final int idx = getLastIndex();
        if (idx == 0 && lastActionsList == null) {
            lastAtLastIndexIdx = idx;
            lastAtLastIndexList = null;
            lastAtLastIndexRepPos = 1;
            return true;
        }
        if (null == lastActionsList) {
            throw new IllegalStateException("null == lastActionsList");
        }
        boolean ret = idx >= lastActionsList.size() - 1;
        if (ret) {
            lastAtLastIndexList = new ArrayList<>(lastActionsList);
            lastAtLastIndexIdx = idx;
            lastAtLastIndexRepPos = 2;
        }
        return ret;
    }

    /**
     * Get the value of index of the last PDDL action planned.
     *
     * @return the value of lastIndex
     */
    public int getLastIndex() {
        return lastIndex.get();
    }

    @Nullable
    private volatile Thread setLastActionsIndexThread = null;
    private volatile StackTraceElement setLastActionsIndexTrace @Nullable []  = null;
    private volatile StackTraceElement firstSetLastActionsIndexTrace @Nullable []  = null;

//    private final ConcurrentLinkedDeque<StackTraceElement[]> setLastActionsIndexTraces
//            = new ConcurrentLinkedDeque<>();
    private void setLastActionsIndex(@Nullable List<PddlAction> actionsList, int index) {
        final Thread curThread = Thread.currentThread();
        if (null == setLastActionsIndexThread) {
            setLastActionsIndexThread = curThread;
            setLastActionsIndexTrace = curThread.getStackTrace();
            firstSetLastActionsIndexTrace = setLastActionsIndexTrace;
        } else if (setLastActionsIndexThread != curThread) {
            logger.log(Level.FINE, "setLastActionsIndexThread changed from {0} to {1} ", new Object[]{setLastActionsIndexThread, curThread});
        }
        if (lastActionsList != actionsList || index != lastIndex.get()) {
            setLastActionsIndexTrace = curThread.getStackTrace();
//            setLastActionsIndexTraces.add(setLastActionsIndexTrace);
            if (null != actionsList) {
                this.lastActionsList = new ArrayList<>(actionsList);
            } else {
                this.lastActionsList = null;
            }
            lastIndex.set(index);
        }
    }

    private int incLastActionsIndex() {
//        this.lastActionsList = ((null != actionsList)?new ArrayList<>(actionsList):null);
        return lastIndex.incrementAndGet();
    }

    /**
     * Get a map of options as a name to value map.
     *
     * @return options
     */
    @Nullable
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

    @Nullable
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

    public void partialReset() {
        lastAcbi.set(null);
        this.unitsSet = false;
        this.rotSpeedSet = false;
        this.genThread = null;
        setLastActionsIndex(null, 0);
        clearLastRequiredPartsMap();
    }

    /**
     * Set state/history/cache variables back to their initial values.
     */
    public void reset() {
        partialReset();
        this.lastTakenPart = null;
        clearPoseCache();
    }

    @MonotonicNonNull
    private OpDisplayJPanel opDisplayJPanelSolution;

    /**
     * Get the value of opDisplayJPanelSolution
     *
     * @return the value of opDisplayJPanelSolution
     */
    @Nullable
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

    @MonotonicNonNull
    private OpDisplayJPanel opDisplayJPanelInput;

    /**
     * Get the value of opDisplayJPanelInput
     *
     * @return the value of opDisplayJPanelInput
     */
    @Nullable
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
     * @return list of CRCL commands
     *
     * @throws IllegalStateException if database not connected
     * @throws SQLException if query of the database failed
     * @throws java.lang.InterruptedException another thread called
     * Thread.interrupt() while retrieving data from database
     *
     * @throws PendantClientInner.ConcurrentBlockProgramsException pendant
     * client in a state blocking program execution
     * @throws java.util.concurrent.ExecutionException exception occurred in
     * another thread servicing the waitForCompleteVisionUpdates
     * @throws crcl.utils.CRCLException a failure occurred while composing or
     * sending a CRCL command.
     * @throws rcs.posemath.PmException failure occurred while computing a pose
     * such as an invalid transform
     *
     */
    public List<MiddleCommandType> generate(List<PddlAction> actions, int startingIndex, Map<String, String> options, int startSafeAbortRequestCount)
            throws IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        assert null != aprsJFrame : "(null == aprsJFrame)";
        GenerateParams gparams = new GenerateParams();
        gparams.actions = actions;
        gparams.startingIndex = startingIndex;
        gparams.options = options;
        gparams.startSafeAbortRequestCount = startSafeAbortRequestCount;
        gparams.replan = !aprsJFrame.isCorrectionMode();
        return generate(gparams);
//        return generate(actions, startingIndex, options, startSafeAbortRequestCount, true, null, null);
    }

    private boolean diffActions(List<PddlAction> acts1, List<PddlAction> acts2) {
        if (acts1.size() != acts2.size()) {
            System.out.println("acts1.size() != acts2.size(): acts1.size()=" + acts1.size() + ", acts2.size()=" + acts2.size());
            return true;
        }
        for (int i = 0; i < acts1.size(); i++) {
            PddlAction act1 = acts1.get(i);
            PddlAction act2 = acts2.get(i);
            if (!Objects.equals(act1.asPddlLine(), act2.asPddlLine())) {
                System.out.println("acts1.get(i) != acts2.get(i): i=" + i + ",acts1.get(i)=" + acts1.get(i) + ", acts2.get(i)=" + acts2.get(i));
                return true;
            }
        }
        return false;
    }

    @Nullable
    private volatile Thread genThread = null;
    private volatile StackTraceElement genThreadSetTrace @Nullable []  = null;

    private static boolean cmdsContainNonWrapper(List<MiddleCommandType> cmds) {
        for (int i = 0; i < cmds.size(); i++) {
            MiddleCommandType cmd = cmds.get(i);
            if (cmd instanceof SetLengthUnitsType) {
                continue;
            }
            if (cmd instanceof SetAngleUnitsType) {
                continue;
            }
            if (!(cmd instanceof CrclCommandWrapper)) {
                return true;
            }
        }
        return false;
    }

    private void processCommands(List<MiddleCommandType> cmds) {
        if (cmds.isEmpty()) {
            return;
        }
        long cmd0Id = cmds.get(0).getCommandID();
        for (int i = 0; i < cmds.size(); i++) {
            MiddleCommandType cmd = cmds.get(i);
            if (cmd instanceof SetLengthUnitsType) {
                continue;
            }
            if (cmd instanceof SetAngleUnitsType) {
                continue;
            }
            if (!(cmd instanceof CrclCommandWrapper)) {
                throw new IllegalArgumentException("list contains non wrapper commands");
            }
            CrclCommandWrapper wrapper = (CrclCommandWrapper) cmd;
            wrapper.notifyOnStartListeners();
            wrapper.notifyOnDoneListeners();
        }
        cmds.clear();
        if (cmd0Id > 1) {
            commandId.set(cmd0Id - 1);
        }
        addSetUnits(cmds);
    }

    private static class RunOptoToGenerateReturn {

        int newIndex;
    }

    /**
     * Class which stores items that used to be separate parameters to the
     * private implementation of generate
     *
     * actions list of PDDL Actions startingIndex starting index into list of
     * PDDL actions options options to use as commands are generated
     * startSafeAbortRequestCount abort request count taken when higher level
     * action was started this method will immediately abort if the request
     * count is now already higher replan run optaplanner to replan provided
     * actions origActions actions before being passed through optaplanner
     * newItems optional list of newItems if the list has already been retreived
     */
    private class GenerateParams {

        @MonotonicNonNull
        List<PddlAction> actions;
        int startingIndex;
        @MonotonicNonNull
        Map<String, String> options;
        int startSafeAbortRequestCount;
        boolean replan;
        boolean optiplannerUsed;

        @MonotonicNonNull
        List<PddlAction> origActions;
        @MonotonicNonNull
        List<PhysicalItem> newItems;
        @Nullable
        RunOptoToGenerateReturn runOptoToGenerateReturn;

        boolean newItemsRecieved;
        final int startingVisionUpdateCount;

        public GenerateParams() {
            this.startingVisionUpdateCount = visionUpdateCount.get();
        }
    }

    private boolean manualAction;

    /**
     * Get the value of manualAction
     *
     * @return the value of manualAction
     */
    public boolean isManualAction() {
        return manualAction;
    }

    /**
     * Set the value of manualAction
     *
     * @param manualAction new value of manualAction
     */
    public void setManualAction(boolean manualAction) {
        this.manualAction = manualAction;
    }

    @MonotonicNonNull
    private volatile List<List<PddlAction>> takePlaceActions = null;

    private static void checkTakePlaceActions(@Nullable List<List<PddlAction>> tplist, List<PddlAction> fullList) {
        if (null == tplist) {
            return;
        }
        int tplistTotal = 0;
        for (int i = 0; i < tplist.size() - 1; i++) {
            List<PddlAction> l = tplist.get(i);
            for (PddlAction act : l) {
                if (act.getExecuted()) {
                    tplistTotal++;
                }
            }
        }
        if (tplist.size() > 0) {
            tplistTotal += tplist.get(tplist.size() - 1).size();
        }
        int fullListTotal = 0;
        for (PddlAction act : fullList) {
            switch (act.getType()) {
                case "take-part":
                case "place-part":
                    fullListTotal++;
                    break;
            }
        }
        if (tplistTotal > fullListTotal) {
            long time = System.currentTimeMillis();
            System.err.println("tplistTotal > fullListTotal : redundant or repeated actions suspected");
            for (int i = 0; i < tplist.size(); i++) {
                System.out.println("i = " + i);
                for (PddlAction act : tplist.get(i)) {
                    System.out.println((act.getExecuted() ? (time - act.getExecTime()) : "--") + " : " + act.asPddlLine());
                }
            }
            System.err.println("tplistTotal > fullListTotal : redundant or repeated actions suspected");
        }
    }

    /**
     * Generate a list of CRCL commands from a list of PddlActions starting with
     * the given index, using the provided optons.
     *
     * @param gparams object containing the parameters of the method
     * @return list of CRCL commands
     *
     * @throws IllegalStateException if database not connected
     * @throws SQLException if query of the database failed
     * @throws java.lang.InterruptedException another thread called
     * Thread.interrupt() while retrieving data from database
     *
     * @throws java.util.concurrent.ExecutionException exception occurred in
     * another thread servicing the waitForCompleteVisionUpdates
     */
    private List<MiddleCommandType> generate(GenerateParams gparams)
            throws IllegalStateException, SQLException, InterruptedException, PendantClientInner.ConcurrentBlockProgramsException, ExecutionException, CRCLException, PmException {

        assert (null != this.aprsJFrame) : "null == aprsJFrame";
        assert (null != gparams.options) : "null == gparams.options";
        assert (null != gparams.actions) : "null == gparams.actions";
        AprsJFrame localAprsJFrame = this.aprsJFrame;
        if (null == localAprsJFrame) {
            throw new IllegalStateException("aprsJframe is null");
        }
        final Thread curThread = Thread.currentThread();
        if (null == genThread) {
            genThread = curThread;
            genThreadSetTrace = curThread.getStackTrace();
        } else if (genThread != curThread) {
            System.out.println("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
            throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
        }
        if (null != solver && gparams.replan) {
            return runOptaPlanner(gparams.actions, gparams.startingIndex, gparams.options, gparams.startSafeAbortRequestCount);
        }

        this.startSafeAbortRequestCount = gparams.startSafeAbortRequestCount;
        checkDbReady();
        if (localAprsJFrame.isRunningCrclProgram()) {
            throw new IllegalStateException("already running crcl while trying to generate it");
        }
        List<MiddleCommandType> cmds = new ArrayList<>();
        int blockingCount = localAprsJFrame.startBlockingCrclPrograms();

        ActionCallbackInfo acbi = lastAcbi.get();
        if (null != acbi && includeEndNormalActionMarker && includeEndProgramMarker && includeSkipNotifyMarkers) {
            if (gparams.startingIndex < acbi.actionIndex) {
                if (gparams.startingIndex != 0 || acbi.actionIndex < acbi.getActionsSize() - 2) {
                    System.out.println("Thread.currentThread() = " + Thread.currentThread());
                    boolean actionsChanged = diffActions(gparams.actions, acbi.actions);
                    System.out.println("actionsChanged = " + actionsChanged);
                    String errString = "generate called with startingIndex=" + gparams.startingIndex + ",acbi.getActionsSize()=" + acbi.getActionsSize() + " and acbi.actionIndex=" + acbi.actionIndex + ", lastIndex=" + lastIndex + ", acbi.action.=" + acbi.action;
                    System.err.println(errString);
                    System.err.println("acbi = " + acbi);
                    localAprsJFrame.setTitleErrorString(errString);
                    throw new IllegalStateException(errString);
                }
            }
        }
        try {

            this.options = gparams.options;
            final int currentCrclNumber = this.crclNumber.incrementAndGet();

            if (null == actionToCrclIndexes || actionToCrclIndexes.length != gparams.actions.size()) {
                actionToCrclIndexes = new int[gparams.actions.size()];
            }
            for (int i = gparams.startingIndex; i < actionToCrclIndexes.length; i++) {
                actionToCrclIndexes[i] = -1;
            }
            if (null == actionToCrclLabels || actionToCrclLabels.length != gparams.actions.size()) {
                actionToCrclLabels = new String[gparams.actions.size()];
            }
            for (int i = gparams.startingIndex; i < actionToCrclLabels.length; i++) {
                actionToCrclLabels[i] = "UNDEFINED";
            }
            if (null == actionToCrclTakenPartsNames || actionToCrclTakenPartsNames.length != gparams.actions.size()) {
                actionToCrclTakenPartsNames = new String[gparams.actions.size()];
            }
            if (gparams.startingIndex == 0) {
                if (!manualAction) {
                    this.lastTakenPart = null;
                }
                takePlaceActions = new ArrayList<>();
            }
            List<PddlAction> newTakePlaceList = new ArrayList<>();
            if (null != takePlaceActions) {
                takePlaceActions.add(newTakePlaceList);
            }
            addSetUnits(cmds);

            String waitForCompleteVisionUpdatesCommentString = "generate(start=" + gparams.startingIndex + ",crclNumber=" + currentCrclNumber + ")";
            boolean isNewRetArray[] = new boolean[1];
            gparams.newItems = updateStalePoseCache(gparams.startingIndex, acbi, gparams.newItems, waitForCompleteVisionUpdatesCommentString, isNewRetArray);
            if (isNewRetArray[0]) {
                gparams.newItemsRecieved = true;
            }
            takeSnapshots("plan", "generate(start=" + gparams.startingIndex + ",crclNumber=" + currentCrclNumber + ")", null, null);
            final List<PddlAction> fixedActionsCopy = Collections.unmodifiableList(new ArrayList<>(gparams.actions));
            final List<PddlAction> fixedOrigActionsCopy = (gparams.origActions == null) ? null : Collections.unmodifiableList(new ArrayList<>(gparams.actions));

            int skipStartIndex = -1;
            int skipEndIndex = -1;
            List<String> skippedParts = new ArrayList<>();
            List<String> foundParts = new ArrayList<>();
            if (gparams.optiplannerUsed && null != this.opDisplayJPanelInput && null != this.opDisplayJPanelSolution) {
                String inputLabel = this.opDisplayJPanelInput.getLabel();
                String outputLabel = this.opDisplayJPanelSolution.getLabel();
                if (null != inputLabel && inputLabel.length() > 0
                        && null != outputLabel && outputLabel.length() > 0) {
                    addMessageCommand(cmds, "OptaPlanner: " + outputLabel + ", " + inputLabel);
                }
            }
            for (this.setLastActionsIndex(gparams.actions, gparams.startingIndex); getLastIndex() < gparams.actions.size(); incLastActionsIndex()) {

                final int idx = getLastIndex();
                PddlAction action = gparams.actions.get(idx);
                System.out.println("action = " + action);
                if (skipMissingParts) {
                    boolean needSkip = false;
                    switch (action.getType()) {
                        case "take-part":
                            if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                                logger.log(Level.WARNING, "newItems.isEmpty() on take-part for run " + getRunName());
                            }
                            String partName = action.getArgs()[takePartArgIndex];
                            PoseType pose = getPose(partName, getReverseFlag());
                            if (pose == null) {
                                skippedParts.add(partName);
                                recordSkipTakePart(partName, pose);
                                skipEndIndex = idx;
                                if (skipStartIndex < 0) {
                                    skipStartIndex = idx;
                                }
                                needSkip = true;
                            } else {
                                foundParts.add(partName);
                                skipEndIndex = -1;
                                skipStartIndex = -1;
                                needSkip = false;
                            }
                            break;

                        case "place-part":
                            if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                                logger.log(Level.WARNING, "newItems.isEmpty() on place-part for run " + getRunName());
                            }
                            String slotName = action.getArgs()[placePartSlotArgIndex];
                            if (null == lastTakenPart) {
                                PoseType slotPose = getPose(slotName, getReverseFlag());
                                recordSkipPlacePart(slotName, slotPose);
                                skipEndIndex = idx;
                                if (skipStartIndex < 0) {
                                    skipStartIndex = idx;
                                }
                                needSkip = true;
                            } else {
                                skipEndIndex = -1;
                                skipStartIndex = -1;
                                needSkip = false;
                            }
                            break;

                        default:
                            break;
                    }
                    if (needSkip) {
                        if (includeSkipNotifyMarkers) {
                            String skip_action_string = "skip_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addNotifyMarker(cmds, skip_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                        }
                        continue;
                    }
                }
                takeSnapshots("plan", "gc_actions.get(" + idx + ")=" + action, null, null);
                String start_action_string = "start_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                addTakeSnapshots(cmds, start_action_string, null, null, this.crclNumber.get());
                switch (action.getType()) {
                    case "take-part":
                        newTakePlaceList.add(action);
                        if (null != takePlaceActions) {
                            checkTakePlaceActions(takePlaceActions, gparams.actions);
                        }
                        if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                            logger.log(Level.WARNING, "newItems.isEmpty() on take-part for run " + getRunName());
                        }
                        takePart(action, cmds, getNextPlacePartAction(idx, gparams.actions));
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
                                doInspectKit ? (idx == gparams.actions.size() - 1) : (idx >= gparams.actions.size() - 2)
                        );
                        updateActionToCrclArrays(idx, cmds);
                         {
                            String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addNotifyMarker(cmds, end_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                        }
                        if (idx != 0 || cmdsContainNonWrapper(cmds)) {
                            if (!skippedParts.isEmpty()) {
                                System.out.println("foundParts = " + foundParts);
                                System.out.println("skippedParts = " + skippedParts);
                                System.out.println("poseCache.keySet() = " + poseCache.keySet());
                            }
                            return cmds;
                        } else {
                            long cmd0Id = -1;
                            if (!cmds.isEmpty()) {
                                cmd0Id = cmds.get(0).getCommandID();
                                try {
                                    processCommands(cmds);
                                } catch (Throwable thrown) {
                                    thrown.printStackTrace();
                                    throw thrown;
                                }
                            }
                            if (!gparams.newItemsRecieved && visionUpdateCount.get() == gparams.startingVisionUpdateCount) {
                                gparams.newItems = checkNewItems(waitForCompleteVisionUpdatesCommentString);
                            }
                            logger.log(Level.FINE, "Processed wrapper only commands without sending to robot.");
                            if (null != gparams.runOptoToGenerateReturn) {
                                gparams.runOptoToGenerateReturn.newIndex = idx + 1;
                                if (!skippedParts.isEmpty()) {
                                    System.out.println("foundParts = " + foundParts);
                                    System.out.println("skippedParts = " + skippedParts);
                                    System.out.println("poseCache.keySet() = " + poseCache.keySet());
                                }
                                if (cmd0Id > 1) {
                                    commandId.set(cmd0Id - 1);
                                }
                                return Collections.emptyList();
                            }
                        }
                        break;

                    case "goto-tool-changer-approach":
                        gotoToolChangerApproach(action, cmds);
                        break;

                    case "goto-tool-changer-pose":
                        gotoToolChangerPose(action, cmds);
                        break;

                    case "drop-tool":
                        dropTool(action, cmds);
                        break;

                    case "pickup-tool":
                        pickupTool(action, cmds);
                        break;

                    case "end-program":
                        endProgram(action, cmds);
                        updateActionToCrclArrays(idx, cmds);
                        if (includeEndProgramMarker) {
                            String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addNotifyMarker(cmds, end_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                        }
                        if (!skippedParts.isEmpty()) {
                            System.out.println("foundParts = " + foundParts);
                            System.out.println("skippedParts = " + skippedParts);
                            System.out.println("poseCache.keySet() = " + poseCache.keySet());
                        }
                        return cmds;

                    case "place-part":
                        newTakePlaceList.add(action);
                        checkTakePlaceActions(takePlaceActions, gparams.actions);
                        if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                            logger.log(Level.WARNING, "newItems.isEmpty() on place-part for run " + getRunName());
                        }
                        placePart(action, cmds);
                        if (!includeEndNormalActionMarker) {
                            String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addNotifyMarker(cmds, end_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                        }
                        break;

                    case "pause":
                        pause(action, cmds);
                        break;

                    case "inspect-kit": {
                        assert (gparams.startingIndex == idx) : "inspect-kit startingIndex(" + gparams.startingIndex + ") != lastIndex(" + idx + ")";
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
                updateActionToCrclArrays(idx, cmds);
                action.setPlanTime();
                String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());

                if (includeEndNormalActionMarker) {
                    addNotifyMarker(cmds, end_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                }
                addTakeSnapshots(cmds, end_action_string, null, null, this.crclNumber.get());
                if (!skippedParts.isEmpty()) {
                    System.out.println("foundParts = " + foundParts);
                    System.out.println("skippedParts = " + skippedParts);
                    System.out.println("poseCache.keySet() = " + poseCache.keySet());
                }
            }
            if (localAprsJFrame.isRunningCrclProgram()) {
                throw new IllegalStateException("already running crcl while trying to generate it");
            }
        } catch (Exception ex) {
            System.err.println("getRunName() = " + getRunName());
            System.err.println("poseCache.keySet() = " + poseCache.keySet());
            if (null != gparams.newItems) {
                System.err.println("newItems.size()=" + gparams.newItems.size());
                System.err.println("newItems=");
                for (PhysicalItem newItem : gparams.newItems) {
                    System.err.println(newItem.getName() + " : " + newItem.x + "," + newItem.y);
                }
            }
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("");
            System.err.println("");
            throw new IllegalStateException(ex);
        } finally {
            localAprsJFrame.stopBlockingCrclPrograms(blockingCount);
        }
        return cmds;
    }

    private boolean includeEndNormalActionMarker = true;

    /**
     * Get the value of includeEndNormalActionMarker
     *
     * @return the value of includeEndNormalActionMarker
     */
    public boolean isIncludeEndNormalActionMarker() {
        return includeEndNormalActionMarker;
    }

    /**
     * Set the value of includeEndNormalActionMarker
     *
     * @param includeEndNormalActionMarker new value of
     * includeEndNormalActionMarker
     */
    public void setIncludeEndNormalActionMarker(boolean includeEndNormalActionMarker) {
        this.includeEndNormalActionMarker = includeEndNormalActionMarker;
    }

    private boolean includeEndProgramMarker = false;

    /**
     * Get the value of includeEndProgramMarker
     *
     * @return the value of includeEndProgramMarker
     */
    public boolean isIncludeEndProgramMarker() {
        return includeEndProgramMarker;
    }

    /**
     * Set the value of includeEndProgramMarker
     *
     * @param includeEndProgramMarker new value of includeEndProgramMarker
     */
    public void setIncludeEndProgramMarker(boolean includeEndProgramMarker) {
        this.includeEndProgramMarker = includeEndProgramMarker;
    }

    private boolean includeSkipNotifyMarkers = false;

    /**
     * Get the value of includeSkipNotifyMarkers
     *
     * @return the value of includeSkipNotifyMarkers
     */
    public boolean isIncludeSkipNotifyMarkers() {
        return includeSkipNotifyMarkers;
    }

    /**
     * Set the value of includeSkipNotifyMarkers
     *
     * @param includeSkipNotifyMarkers new value of includeSkipNotifyMarkers
     */
    public void setIncludeSkipNotifyMarkers(boolean includeSkipNotifyMarkers) {
        this.includeSkipNotifyMarkers = includeSkipNotifyMarkers;
    }

    public void addNotifyMarker(List<MiddleCommandType> cmds, String end_action_string, final int idx, PddlAction action, final List<PddlAction> fixedActionsCopy, @Nullable List<PddlAction> fixedOrigActionsCopy) {
        addMarkerCommand(cmds, end_action_string,
                (CrclCommandWrapper wrapper) -> {
                    notifyActionCompletedListeners(idx, action, wrapper, fixedActionsCopy, fixedOrigActionsCopy);
                });
    }

    private void updateActionToCrclArrays(final int idx, List<MiddleCommandType> cmds) {
        if (null != actionToCrclIndexes) {
            actionToCrclIndexes[idx] = cmds.size();
        }
        if (null != actionToCrclLabels) {
            actionToCrclLabels[idx] = "";
        }
        if (null != actionToCrclTakenPartsNames) {
            actionToCrclTakenPartsNames[idx] = this.lastTakenPart;
        }
    }

    private List<PhysicalItem> updateStalePoseCache(int startingIndex, @Nullable ActionCallbackInfo acbi, @Nullable List<PhysicalItem> newItems, String waitForCompleteVisionUpdatesCommentString, boolean isNetRetArray[]) throws InterruptedException, ExecutionException {
        if (startingIndex > 0 && null != acbi && null != acbi.action) {
            switch (acbi.action.getType()) {

                case "look-for-part":
                case "look-for-parts":
                    if (null == newItems) {
                        newItems = newPoseItems(waitForCompleteVisionUpdatesCommentString);
                        if (null != isNetRetArray && isNetRetArray.length == 1) {
                            isNetRetArray[0] = true;
                        }
                    }
                    break;

                default:
                    break;
            }
        }
        if (null == newItems) {
            return Collections.emptyList();
        }
        return newItems;
    }

    public List<PhysicalItem> newPoseItems(String waitForCompleteVisionUpdatesCommentString) throws InterruptedException, ExecutionException {
        List<PhysicalItem> newItems = checkNewItems(waitForCompleteVisionUpdatesCommentString);
        assert (newItems != null) :
                "newItems == null";
        synchronized (poseCache) {
            for (PhysicalItem item : newItems) {
                String fullName = item.getFullName();
                if (null != fullName) {
                    poseCache.put(fullName, item.getPose());
                }
            }
        }
        return newItems;
    }

    private List<PhysicalItem> checkNewItems(String waitForCompleteVisionUpdatesCommentString) throws InterruptedException, ExecutionException {
        List<PhysicalItem> newItems;
        if (null == externalPoseProvider) {
            newItems = waitForCompleteVisionUpdates(waitForCompleteVisionUpdatesCommentString, lastRequiredPartsMap, 15_000);
        } else {
            newItems = externalPoseProvider.getNewPhysicalItems();
        }
        return newItems;
    }

    private static final AtomicInteger ropCount = new AtomicInteger();

    private boolean isLookForPartsAction(PddlAction action) {
        switch (action.getType()) {
            case "look-for-part":
            case "look-for-parts":
                return true;

            default:
                return false;
        }
    }

    private int firstLookForPartsIndex(List<PddlAction> l, int startingIndex) {
        for (int i = startingIndex; i < l.size(); i++) {
            if (isLookForPartsAction(l.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMoveAction(PddlAction action) {
        switch (action.getType()) {
            case "place-part":
            case "take-part":
            case "fake-take-part":
            case "test-part-position":
                return true;

            default:
                return false;
        }
    }

    private int firstMoveIndex(List<PddlAction> l, int startingIndex) {
        for (int i = startingIndex; i < l.size(); i++) {
            if (isMoveAction(l.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private List<MiddleCommandType> runOptaPlanner(
            List<PddlAction> actions,
            int startingIndex, Map<String, String> options1,
            int startSafeAbortRequestCount1)
            throws IllegalStateException, InterruptedException, SQLException, PendantClientInner.ConcurrentBlockProgramsException, ExecutionException, CRCLException, PmException {
        assert (null != this.aprsJFrame) : "null == aprsJFrame";
        assert (null != this.solver) : "null == solver";
        int lfpIndex = firstLookForPartsIndex(actions, startingIndex);
        int mIndex = firstMoveIndex(actions, startingIndex);
        GenerateParams gparams = new GenerateParams();
        gparams.actions = actions;
        gparams.startingIndex = startingIndex;
        gparams.options = options1;
        gparams.startSafeAbortRequestCount = startSafeAbortRequestCount1;
        if (mIndex < 0
                || (lfpIndex >= 0 && mIndex > lfpIndex)) {
            gparams.replan = false;
            gparams.runOptoToGenerateReturn = new RunOptoToGenerateReturn();
            List<MiddleCommandType> l = generate(gparams);
            if (!l.isEmpty()
                    || null == gparams.runOptoToGenerateReturn
                    || gparams.runOptoToGenerateReturn.newIndex <= startingIndex) {
                return l;
            }
            startingIndex = gparams.runOptoToGenerateReturn.newIndex;
            gparams.startingIndex = startingIndex;
        }
        gparams.runOptoToGenerateReturn = null;
        int rc = ropCount.incrementAndGet();
//        System.out.println("runOptaPlanner: rc = " + rc);
        long t0 = System.currentTimeMillis();
        this.options = options1;
        if (actions.size() < 1) {
            throw new IllegalArgumentException("actions.size() = " + actions.size() + ",rc=" + rc);
        }
        List<PhysicalItem> newItems = null;

        ActionCallbackInfo acbi = lastAcbi.get();
        String waitForCompleteVisionUpdatesCommentString = "runOptaPlanner(start=" + startingIndex + ",crclNumber=" + crclNumber + ")";
        boolean isNewRetArray[] = new boolean[1];
        newItems = updateStalePoseCache(startingIndex, acbi, newItems, waitForCompleteVisionUpdatesCommentString, isNewRetArray);
        gparams.newItems = newItems;
        if (null == newItems) {
            gparams.replan = false;
            return generate(gparams);
        }
        if (isNewRetArray[0]) {
            gparams.newItemsRecieved = true;
        }
        List<PddlAction> fullReplanPddlActions = optimizePddlActionsWithOptaPlanner(actions, startingIndex, newItems);
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActions || fullReplanPddlActions.size() < 1) {
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActions);
        }
        if (fullReplanPddlActions == actions) {
            gparams.replan = false;
            return generate(gparams);
        }
        List<PddlAction> copyFullReplanPddlActions = new ArrayList<>(fullReplanPddlActions);
        List<PddlAction> origActions = new ArrayList<>(actions);
        synchronized (actions) {
            actions.clear();
            actions.addAll(fullReplanPddlActions);
        }
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActions || actions.size() < 1) {
            System.out.println("copyFullReplanPddlActions = " + copyFullReplanPddlActions);
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActions);
        }
        if (debug) {
            showPddlActionsList(actions);
        }
        gparams.optiplannerUsed = true;
        List<MiddleCommandType> newCmds = generate(gparams);
        if (debug) {
            showCmdList(newCmds);
        }
        return newCmds;
    }

    private void showPddlActionsList(List<PddlAction> actions) throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            sb.append(i);
            sb.append(" ");
            sb.append(actions.get(i).asPddlLine());
            sb.append("\n");
        }
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                String actionsText = MultiLineStringJPanel.editText(sb.toString());
            });
        } catch (InvocationTargetException ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showCmdList(List<MiddleCommandType> newCmds) throws InterruptedException {
        StringBuilder cmdSb = new StringBuilder();
        for (int i = 0; i < newCmds.size(); i++) {
            try {
                cmdSb.append(i);
                cmdSb.append(" ");
                cmdSb.append(CRCLSocket.getUtilSocket().commandToSimpleString(newCmds.get(i)));
                cmdSb.append("\n");
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                String newCmdsText = MultiLineStringJPanel.editText(cmdSb.toString());
            });
        } catch (InvocationTargetException ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private volatile @Nullable
    List<OpAction> lastSkippedOpActionsList;
    private volatile @Nullable
    List<PddlAction> lastSkippedPddlActionsList;
    private volatile @Nullable
    List<OpAction> lastOpActionsList;
    private volatile @Nullable
    List<PhysicalItem> lastOptimizePddlItems;
    private volatile @Nullable
    List<String> lastOptimizePddlItemNames;
    private volatile @Nullable
    List<PddlAction> lastOptimizePreStartPddlActions;
    private volatile @Nullable
    List<PddlAction> lastOptimizeReplacedPddlActions;
    private volatile @Nullable
    List<PddlAction> lastOptimizeNewPddlActions;
    private volatile @Nullable
    List<PddlAction> lastOptimizeLaterPddlActions;
    private volatile @Nullable
    Thread optoThread = null;

    private static <T> Stream<T> nonNullStream(T object) {
        if (null != object) {
            return Stream.of(object);
        }
        return Stream.empty();
    }

    private static <T, R> Function<T, Stream<R>> nonNullFunction(Function<T, R> function) {
        return function
                .andThen(PddlActionToCrclGenerator::nonNullStream);
    }

    private static String badScores(double solveScore, double inScore) {
        return "solveScore < inScore : solveScore=" + solveScore + " inscore=" + inScore;
    }

    private List<PddlAction> optimizePddlActionsWithOptaPlanner(
            List<PddlAction> actions,
            int startingIndex,
            List<PhysicalItem> items) throws SQLException, InterruptedException, ExecutionException {
        assert (null != this.aprsJFrame) : "null == aprsJFrame";
        Solver<OpActionPlan> solverToRun = this.solver;
        if (null == solverToRun) {
            return actions;
        }
        int endl[] = new int[2];
        PointType lookForPt = getLookForXYZ();
        if (null == lookForPt) {
            throw new IllegalStateException("null == lookForPT, startingIndex=" + startingIndex + ", actions=" + actions);
        }
        List<OpAction> skippedOpActionsList = new ArrayList<>();
        List<PddlAction> skippedPddlActionsList = new ArrayList<>();
        List<OpAction> opActions = pddlActionsToOpActions(actions, startingIndex, endl, skippedOpActionsList, skippedPddlActionsList);

        List<String> itemNames = items.stream()
                .flatMap(nonNullFunction(PhysicalItem::getFullName))
                .collect(Collectors.toList());
        lastOptimizePddlItemNames = itemNames;
        if (optoThread == null) {
            optoThread = Thread.currentThread();
        }
        if (Thread.currentThread() != optoThread) {
            throw new IllegalStateException("!Thread.currentThread() != optoThread: optoThread=" + optoThread + ", Thread.currentThread() =" + Thread.currentThread());
        }
        if (true /*!getReverseFlag() */) {
            MutableMultimap<String, PhysicalItem> availItemsMap
                    = Lists.mutable.ofAll(items)
                    .select(item -> item.getType().equals("P") && item.getName().contains("_in_pt"))
                    .groupBy(item -> posNameToType(item.getName()));

            MutableMultimap<String, PddlAction> takePartMap
                    = Lists.mutable.ofAll(actions.subList(endl[0], endl[1]))
                            .select(action -> action.getType().equals("take-part") && !inKitTrayByName(action.getArgs()[takePartArgIndex]))
                            .groupBy(action -> posNameToType(action.getArgs()[takePartArgIndex]));

            for (String partTypeName : takePartMap.keySet()) {
                MutableCollection<PhysicalItem> thisPartTypeItems
                        = availItemsMap.get(partTypeName);
                MutableCollection<PddlAction> thisPartTypeActions
                        = takePartMap.get(partTypeName);
                if (thisPartTypeItems.size() > thisPartTypeActions.size() && thisPartTypeActions.size() > 0) {
                    for (PhysicalItem item : thisPartTypeItems) {
                        if (0 == thisPartTypeActions.count(action -> action.getArgs()[takePartArgIndex].equals(item.getFullName()))) {
                            opActions.add(new OpAction("take-part" + "-" + Arrays.toString(new String[]{item.getFullName()}), item.x, item.y, OpActionType.PICKUP, partTypeName,inKitTrayByName(item.getFullName())));
                        }
                    }
                }
            }
            Set<String> typeSet = items
                    .stream()
                    .map(PhysicalItem::getType)
                    .collect(Collectors.toSet());

            System.out.println("typeSet = " + typeSet);
            MutableMultimap<String, PhysicalItem> availSlotsMap
                    = Lists.mutable.ofAll(items)
                            .select(item -> item.getType().equals("ES")
                            && item.getName().startsWith("empty_slot_")
                            && !item.getName().contains("_in_kit_"))
                            .groupBy(item -> posNameToType(item.getName()));

            MutableMultimap<String, PddlAction> placePartMap
                    = Lists.mutable.ofAll(actions.subList(endl[0], endl[1]))
                            .select(action -> action.getType().equals("place-part") && !inKitTrayByName(action.getArgs()[placePartSlotArgIndex]))
                            .groupBy(action -> posNameToType(action.getArgs()[placePartSlotArgIndex]));

            for (String partTypeName : placePartMap.keySet()) {
                MutableCollection<PhysicalItem> thisPartTypeSlots
                        = availSlotsMap.get(partTypeName);
                MutableCollection<PddlAction> thisPartTypeActions
                        = placePartMap.get(partTypeName);
                if (thisPartTypeSlots.size() > thisPartTypeActions.size() && thisPartTypeActions.size() > 0) {
                    for (PhysicalItem item : thisPartTypeSlots) {
                        if (0 == thisPartTypeActions.count(action -> action.getArgs()[takePartArgIndex].equals(item.getFullName()))) {
                            opActions.add(new OpAction("place-part" + "-" + Arrays.toString(new String[]{item.getFullName()}), item.x, item.y, OpActionType.DROPOFF, partTypeName,inKitTrayByName(item.getFullName())));
                        }
                    }
                }
            }
        }
        if (opActions.size() < 3) {
            logger.warning("opActions.size()=" + opActions.size());
            return actions;
        }
        if (skippedActions > 0) {
            System.out.println("skippedActions = " + skippedActions);
            System.out.println("skippedPddlActionsList.size() = " + skippedPddlActionsList.size());
            System.out.println("skippedPddlActionsList = " + skippedPddlActionsList);
            System.out.println("skippedOpActionsList = " + skippedOpActionsList);
            System.out.println("items = " + items);
            System.out.println("itemNames = " + itemNames);
            if (debug || skippedActions % 2 == 1) {
                System.err.println("actions.size() = " + actions.size() + ", skippedActions=" + skippedActions);
                int recheckEndl[] = new int[2];
                List<OpAction> recheckSkippedOpActionsList = new ArrayList<>();
                List<PddlAction> recheckSkippedPddlActionsList = new ArrayList<>();
                List<OpAction> recheckOpActions = pddlActionsToOpActions(actions, startingIndex, recheckEndl, recheckSkippedOpActionsList, recheckSkippedPddlActionsList);
                System.out.println("recheckOpActions = " + recheckOpActions);
                System.out.println("recheckEndl = " + Arrays.toString(recheckEndl));
                System.out.println("recheckSkippedPddlActionsList = " + recheckSkippedPddlActionsList);
                System.out.println("recheckSkippedOpActionsList = " + recheckSkippedOpActionsList);
            }
        }
        if (skippedActions != skippedPddlActionsList.size()) {
            System.err.println("skippedPddlActionsList = " + skippedPddlActionsList);
            System.err.println("actions = " + actions);
            throw new IllegalStateException("skippedPddlActionsList.size() = " + skippedPddlActionsList.size() + ",actions.size() = " + actions.size() + ", skippedActions=" + skippedActions);
        }
        OpActionPlan inputPlan = new OpActionPlan();
        inputPlan.setUseDistForCost(false);
        inputPlan.setAccelleration(fastTransSpeed);
        inputPlan.setMaxSpeed(fastTransSpeed);
        inputPlan.setStartEndMaxSpeed(2 * fastTransSpeed);
        inputPlan.setActions(opActions);
        inputPlan.getEndAction().setLocation(new Point2D.Double(lookForPt.getX(), lookForPt.getY()));
        inputPlan.initNextActions();
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(inputPlan);
        double inScore = (score.getSoftScore() / 1000.0);
        OpActionPlan solvedPlan = solverToRun.solve(inputPlan);
        HardSoftLongScore hardSoftLongScore = solvedPlan.getScore();
        assert (null != hardSoftLongScore) : "solvedPlan.getScore() returned null";
        double solveScore = (hardSoftLongScore.getSoftScore() / 1000.0);
//        System.out.println("Score improved:" + (solveScore - inScore));
        if (null != this.opDisplayJPanelInput) {
            if (null == opDisplayJPanelInput.getOpActionPlan()) {
                this.opDisplayJPanelInput.setOpActionPlan(inputPlan);
                if (inputPlan.isUseDistForCost()) {
                    this.opDisplayJPanelInput.setLabel("Input : " + String.format("%.1f mm ", -inScore));
                } else {
                    this.opDisplayJPanelInput.setLabel("Input : " + String.format("%.2f s ", -inScore));
                }
            }
        }
        double scoreDiff = solveScore - inScore;

        if (null != this.opDisplayJPanelSolution) {
            this.opDisplayJPanelSolution.setOpActionPlan(solvedPlan);
            if (solvedPlan.isUseDistForCost()) {
                this.opDisplayJPanelSolution.setLabel("Output : " + String.format("%.1f mm ", -solveScore));
            } else {
                this.opDisplayJPanelSolution.setLabel("Output : " + String.format("%.2f s ", -solveScore));
            }
        }
        assert scoreDiff >= 0 : badScores(solveScore, inScore);
        if (true /* scoreDiff > 0.1*/) {
            List<PddlAction> fullReplanPddlActions = new ArrayList<>();
            List<PddlAction> preStartPddlActions = new ArrayList<>(actions.subList(0, startingIndex > endl[0] ? startingIndex : endl[0]));
            fullReplanPddlActions.addAll(preStartPddlActions);
            List<PddlAction> newPddlActions = opActionsToPddlActions(solvedPlan, 0);
            List<PddlAction> replacedPddlActions = new ArrayList<>(actions.subList(endl[0], endl[1]));
//            int sizeDiff = Math.abs(newPddlActions.size() - replacedPddlActions.size());
//            if (sizeDiff != skippedPddlActionsList.size() || newPddlActions.size() < 1) {
//                System.out.println("sizeDiff = " + sizeDiff);
//                System.out.println("endl = " + Arrays.toString(endl));
//                System.out.println("preStartPddlActions.size() = " + preStartPddlActions.size());
//                System.out.println("preStartPddlActions = " + preStartPddlActions);
//                System.out.println("newPddlActions.size() = " + newPddlActions.size());
//                System.out.println("newPddlActions = " + newPddlActions);
//                System.out.println("replacedPddlActions.size() = " + replacedPddlActions.size());
//                System.out.println("replacedPddlActions = " + replacedPddlActions);
//
//                System.err.println("newPddlActions.size() = " + newPddlActions.size() + ",actions.size() = " + actions.size() + ", skippedActions=" + skippedActions);
//                int recheckEndl[] = new int[2];
//                List<OpAction> recheckSkippedOpActionsList = new ArrayList<>();
//                List<PddlAction> recheckSkippedPddlActionsList = new ArrayList<>();
//                List<OpAction> recheckOpActions = pddlActionsToOpActions(actions, startingIndex, recheckEndl, recheckSkippedOpActionsList, recheckSkippedPddlActionsList);
//
//                System.out.println("recheckOpActions = " + recheckOpActions);
//                System.out.println("recheckEndl = " + Arrays.toString(recheckEndl));
//                System.out.println("recheckSkippedPddlActionsList = " + recheckSkippedPddlActionsList);
//                System.out.println("recheckSkippedOpActionsList = " + recheckSkippedOpActionsList);
//                throw new IllegalStateException("newPddlActions.size() = " + newPddlActions.size() + ",replacedPddlActions.size() = " + replacedPddlActions.size() + ", skippedActions=" + skippedActions);
//            }
            fullReplanPddlActions.addAll(newPddlActions);
            fullReplanPddlActions.addAll(skippedPddlActionsList);
            List<PddlAction> laterPddlActions = new ArrayList<>(actions.subList(endl[1], actions.size()));
            fullReplanPddlActions.addAll(laterPddlActions);

            lastSkippedOpActionsList = skippedOpActionsList;
            lastSkippedPddlActionsList = skippedPddlActionsList;
            lastOpActionsList = opActions;
            lastOptimizePddlItems = items;
            lastOptimizePreStartPddlActions = preStartPddlActions;
            lastOptimizeReplacedPddlActions = replacedPddlActions;
            lastOptimizeNewPddlActions = newPddlActions;
            lastOptimizeLaterPddlActions = laterPddlActions;
//            if (fullReplanPddlActions.size() != actions.size()) {
//                throw new IllegalStateException("skippedPddlActionsList.size() = " + skippedPddlActionsList.size() + ",actions.size() = " + actions.size() + ", skippedActions=" + skippedActions);
//            }
            return fullReplanPddlActions;
        }
        return actions;
    }

    private void clearKitsToCheck(PddlAction action, List<MiddleCommandType> cmds) {
        kitsToCheck.clear();
    }

    private void checkedPause() {
        if (null != aprsJFrame) {
            aprsJFrame.pause();
        }
    }

    private void pause(PddlAction action, List<MiddleCommandType> cmds) {
        addMarkerCommand(cmds, "pause", x -> checkedPause());
    }

    private void addKitToCheck(PddlAction action, List<MiddleCommandType> cmds) {

        String kitName = action.getArgs()[0];
        Map<String, String> kitSlotMap
                = Arrays.stream(action.getArgs(), 1, action.getArgs().length)
                .map(arg -> arg.split("="))
                .collect(Collectors.toMap(array -> array[0], array -> array[1]));
        KitToCheck kit = new KitToCheck(kitName, kitSlotMap);
        kitsToCheck.add(kit);
    }

    private List<String> getPartTrayInstancesFromSkuName(String skuName) {
        try {
            if (null != externalPoseProvider) {
                return externalPoseProvider.getInstanceNames(skuName);
            }
            if (null == qs) {
                throw new IllegalStateException("QuerySet is null");
            }
            List<String> names = new ArrayList<>();
            for (PartsTray tray : qs.getPartsTrays(skuName)) {
                String name = tray.getPartsTrayName();
                if (null != name) {
                    names.add(name);
                }
            }
            return names;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return Collections.emptyList();
    }

    private final ConcurrentMap<String, List<String>> skuNameToInstanceNamesMap = new ConcurrentHashMap<>();

    private List<String> getKitInstanceNames(String kitName) {
        return skuNameToInstanceNamesMap.computeIfAbsent(kitName, this::getPartTrayInstancesFromSkuName);
    }

    private List<Slot> getAbsSlotListForKitInstance(String kitSkuName, String kitInstanceName) {
        try {
            PoseType pose = getPose(kitInstanceName);
            System.out.println("pose = " + pose);
            if (null == pose) {
                return Collections.emptyList();
            }
            Tray tray = new Tray(kitSkuName, pose, 0);
            tray.setType("KT");
            if (null != aprsJFrame) {
                return aprsJFrame.getSlots(tray, false)
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
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return Collections.emptyList();
    }

    private void checkKits(PddlAction action, List<MiddleCommandType> cmds)
            throws IllegalStateException, SQLException, InterruptedException, ExecutionException, CRCLException, PmException {
        List<PhysicalItem> newItems;
        if (null == externalPoseProvider) {
            newItems = waitForCompleteVisionUpdates("checkKits", lastRequiredPartsMap, 15_000);
        } else {
            newItems = externalPoseProvider.getNewPhysicalItems();
        }
        assert (newItems != null) : "newItems == null : @AssumeAssertion(nullness)";
        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";

        synchronized (poseCache) {
            for (PhysicalItem item : newItems) {
                String fullName = item.getFullName();
                if (null != fullName) {
                    poseCache.put(fullName, item.getPose());
                }
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
                kit.kitInstanceNames = getKitInstanceNames(kit.name);
                Map<String, KitToCheckInstanceInfo> kitInstanceInfoMap = new HashMap<>();
                kit.instanceInfoMap = kitInstanceInfoMap;
                for (String kitInstanceName : kit.kitInstanceNames) {

                    List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                            (String n) -> getAbsSlotListForKitInstance(kit.name, n));
                    KitToCheckInstanceInfo info = new KitToCheckInstanceInfo(kitInstanceName, absSlots);
                    kitInstanceInfoMap.put(kitInstanceName, info);
                    if (matchedKitInstanceNames.contains(kitInstanceName)) {
                        continue;
                    }
                    if (snapshotsEnabled()) {
                        takeSimViewSnapshot(createTempFile("absSlots_" + kitInstanceName, ".PNG"), absSlots);
                    }
                    boolean allSlotsCorrect = true;
                    for (Slot absSlot : absSlots) {
                        String absSlotPrpName = absSlot.getPrpName();
                        PhysicalItem closestItem = parts.stream()
                                .min(Comparator.comparing(absSlot::distFromXY))
                                .orElse(null);
                        if (null == closestItem) {
                            logger.log(Level.SEVERE, "closetItem == null in checkKits");
                            break;
                        }
                        info.closestItemMap.put(absSlotPrpName, closestItem);
                        String itemSkuName = "empty";
                        if (closestItem.distFromXY(absSlot) < absSlot.getDiameter() / 2.0) {
                            itemSkuName = closestItem.origName;
                        }
                        info.itemSkuMap.put(absSlotPrpName, itemSkuName);
                        if (!Objects.equals(kit.slotMap.get(absSlotPrpName), itemSkuName)) {
                            info.failedAbsSlotPrpName = absSlotPrpName;
                            info.failedItemSkuName = itemSkuName;
                            info.failedSlots++;
                            allSlotsCorrect = false;
                        }
                    }
                    if (allSlotsCorrect) {
                        kitsToFix.remove(kit);
                        matchedKitInstanceNames.add(kitInstanceName);
                        break;
                    }
                }
                if (debug) {
                    System.out.println("matchedKitInstanceNames = " + matchedKitInstanceNames);
                    System.out.println("kitsToFix = " + kitsToFix);
                    System.out.println("");
                }
            }

            if (optoThread == null) {
                optoThread = Thread.currentThread();
            }
            if (Thread.currentThread() != optoThread) {
                throw new IllegalStateException("!Thread.currentThread() != optoThread: optoThread=" + optoThread + ", Thread.currentThread() =" + Thread.currentThread());
            }
            if (!kitsToFix.isEmpty()) {
                System.out.println("kitsToFix = " + kitsToFix);
                printLastOptoInfo();
                if (pauseInsteadOfRecover && !aprsJFrame.isCorrectionMode()) {
                    for (KitToCheck kit : kitsToFix) {
                        for (KitToCheckInstanceInfo info : kit.instanceInfoMap.values()) {
                            if (null != info.failedAbsSlotPrpName && null != info.failedItemSkuName) {
                                JOptionPane.showMessageDialog(this.aprsJFrame, kit.name + " needs " + kit.slotMap.get(info.failedAbsSlotPrpName) + " instead of " + info.failedItemSkuName + " in " + info.failedAbsSlotPrpName);
                                break;
                            }
                        }
                    }
                    pause(action, cmds);
                } else {
                    Map<String, Integer> prefixCountMap = new HashMap<>();
                    Map<String, List<String>> itemsNameMap = new HashMap<>();
                    Collections.sort(kitsToFix, Comparators.byIntFunction(KitToCheck::getMaxDiffFailedSlots));
                    List<PddlAction> correctiveActions = new ArrayList<>();
                    for (KitToCheck kit : kitsToFix) {
                        List<KitToCheckInstanceInfo> infoList = new ArrayList<>(kit.instanceInfoMap.values());
                        Collections.sort(infoList, Comparators.byIntFunction(KitToCheckInstanceInfo::getFailedSlots));
                        for (KitToCheckInstanceInfo info : infoList) {
                            String kitInstanceName = info.instanceName;
                            if (matchedKitInstanceNames.contains(kitInstanceName)) {
                                continue;
                            }
                            List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                                    (String n) -> getAbsSlotListForKitInstance(kit.name, n));

                            if (snapshotsEnabled()) {
                                takeSimViewSnapshot(createTempFile("absSlots_" + kitInstanceName, ".PNG"), absSlots);
                            }
                            for (Slot absSlot : absSlots) {
                                String absSlotPrpName = absSlot.getPrpName();
                                PhysicalItem closestItem = parts.stream()
                                        .min(Comparator.comparing(absSlot::distFromXY))
                                        .orElse(null);
                                if (null == closestItem) {
                                    logger.log(Level.SEVERE, "closetItem == null in checkKits");
                                    break;
                                }
                                String itemSkuName = "empty";
                                if (closestItem.distFromXY(absSlot) < absSlot.getDiameter() / 2.0) {
                                    itemSkuName = closestItem.origName;
                                }
                                String slotItemSkuName = kit.slotMap.get(absSlotPrpName);
                                if (null != slotItemSkuName && !slotItemSkuName.equals(itemSkuName)) {

                                    if (!itemSkuName.equals("empty")) {
////                                        takePartByPose(cmds, visionToRobotPose(closestItem.getPose()));
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
////                                        placePartBySlotName(slotPrefix + "_" + count, cmds, action);
                                        correctiveActions.add(new PddlAction("", "take-part", new String[]{closestItem.getFullName()}, ""));
                                        correctiveActions.add(new PddlAction("", "place-part", new String[]{slotPrefix + "_" + count}, ""));
                                    }
                                    if (!slotItemSkuName.equals("empty")) {
                                        String shortSkuName = slotItemSkuName;
                                        if (shortSkuName.startsWith("sku_")) {
                                            shortSkuName = shortSkuName.substring(4);
                                        }
                                        final String finalShortSkuName = shortSkuName;
                                        List<String> partNames
                                                = itemsNameMap.computeIfAbsent(finalShortSkuName,
                                                        k -> partNamesListForShortSkuName(newItems, k));
                                        System.out.println("partNames = " + partNames);
                                        if (partNames.isEmpty()) {
                                            System.out.println("");
                                            System.out.println("No partnames for finalShortSkuName=" + finalShortSkuName);
                                            System.out.println("newItems = " + newItems);
                                            List<String> newItemsFullNames = newItems
                                                    .stream()
                                                    .map(PhysicalItem::getFullName)
                                                    .collect(Collectors.toList());
                                            System.out.println("newItemsFullNames = " + newItemsFullNames);
                                            System.out.println("itemsNameMap = " + itemsNameMap);
                                            System.out.println("slotItemSkuName = " + slotItemSkuName);
                                            System.out.println("itemSkuName = " + itemSkuName);
                                            List<String> recalcPartNames
                                                    = partNamesListForShortSkuName(newItems, finalShortSkuName);
                                            System.out.println("recalcPartNames = " + recalcPartNames);
                                            throw new IllegalStateException("No partnames for finalShortSkuName=" + finalShortSkuName
                                                    + ",slotItemSkuName = " + slotItemSkuName
                                                    + ",itemSkuName = " + itemSkuName
                                                    + ",newItems = " + newItems
                                                    + ",itemsNameMap = " + itemsNameMap);
                                        }
//                                        String partNamePrefix = shortSkuName + "_in_pt";
//                                        int count = prefixCountMap.compute(partNamePrefix,
//                                                (String prefix, Integer c) -> (c == null) ? 1 : (c + 1));
                                        String partName = partNames.remove(0);
                                        correctiveActions.add(new PddlAction("", "take-part", new String[]{partName}, ""));
                                        String slotName = absSlot.getFullName();
                                        System.out.println("slotName = " + slotName);
//                                        boolean inCache = poseCache.keySet().contains(slotName);
//                                        if(!inCache) {
//                                                System.out.println("poseCache.keySet() = " + poseCache.keySet());
//                                                System.out.println("inCache = " + inCache);
//                                                
//                                        }
                                        PoseType slotPose = visionToRobotPose(absSlot.getPose());
                                        double min_dist = Double.POSITIVE_INFINITY;
                                        String closestKey = null;
                                        for (Entry<String, PoseType> entry : poseCache.entrySet()) {
                                            double dist = CRCLPosemath.diffPosesTran(slotPose, entry.getValue());
                                            if (dist < min_dist) {
                                                closestKey = entry.getKey();
                                                min_dist = dist;
                                            }
                                        }
                                        if (closestKey == null || min_dist > 6.0) {
                                            System.out.println("closestKey = " + closestKey);
                                            System.out.println("min_dist = " + min_dist);
                                            for (Entry<String, PoseType> entry : poseCache.entrySet()) {
                                                double dist = CRCLPosemath.diffPosesTran(slotPose, entry.getValue());
                                                System.out.println("entry.getKey = " + entry.getKey()+", dist="+dist);
                                            }
                                            throw new IllegalStateException("absSlotPose for " + slotName + " not in poseCache keys=" + poseCache.keySet());
                                        }
                                        correctiveActions.add(new PddlAction("", "place-part", new String[]{closestKey}, ""));

//                                        takePartByName(partName, null, cmds);
//                                        placePartByPose(cmds, visionToRobotPose(absSlot.getPose()));
                                    }
                                }
                            }
                            matchedKitInstanceNames.add(kitInstanceName);
                            break;
                        }
                        System.out.println("matchedKitInstanceNames = " + matchedKitInstanceNames);
                        System.out.println("kitsToFix = " + kitsToFix);
                        System.out.println("");
                    }
                    List<PddlAction> optimizedCorrectiveActions
                            = optimizePddlActionsWithOptaPlanner(correctiveActions, 0, newItems);
                    CORRECT_ACTIONS_LOOP:
                    for (int caIndex = 0; caIndex < optimizedCorrectiveActions.size(); caIndex++) {
                        PddlAction correctiveAction = optimizedCorrectiveActions.get(caIndex);
                        switch (correctiveAction.getType()) {
                            case "take-part":
                                String partName = correctiveAction.getArgs()[takePartArgIndex];
                                if (partName.contains("in_pt")) {
                                    if (caIndex < optimizedCorrectiveActions.size() - 1) {
                                        PddlAction nextAction = optimizedCorrectiveActions.get(caIndex + 1);
                                        String nextSlotName = nextAction.getArgs()[placePartSlotArgIndex];
                                        if (!nextSlotName.contains("kit")) {
                                            System.out.println("nextSlotName = " + nextSlotName);
                                            caIndex++;
                                            continue CORRECT_ACTIONS_LOOP;
                                        }
                                    }
                                }
                                takePartByName(partName, null, cmds);
                                break;

                            case "place-part":
                                String slotName = correctiveAction.getArgs()[placePartSlotArgIndex];
                                placePartBySlotName(slotName, cmds, correctiveAction);  //ByName(slotName, null, cmds);
                                break;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private List<String> partNamesListForShortSkuName(List<PhysicalItem> newItems, final String finalShortSkuName) {
        List<String> partNames
                = newItems.stream()
                .filter(item -> item.getType().equals("P"))
                .flatMap(item -> {
                    String fullName = item.getFullName();
                    if (null != fullName) {
                        return Stream.of(fullName);
                    }
                    return Stream.empty();
                })
                .filter(name2 -> name2.contains(finalShortSkuName) && !name2.contains("_in_kt_"))
                .sorted()
                .collect(Collectors.toList());
        return partNames;
    }

    private void printLastOptoInfo() {
        try {
            if (null != lastSkippedPddlActionsList) {
                System.out.println("lastSkippedPddlActionsList = " + lastSkippedPddlActionsList.size() + " : " + lastSkippedPddlActionsList);
            }
            if (null != lastOpActionsList) {
                System.out.println("lastOpActionsList = " + lastOpActionsList.size() + " : " + lastOpActionsList);
            }
            if (null != lastSkippedOpActionsList) {
                System.out.println("lastSkippedOpActionsList = " + lastSkippedOpActionsList.size() + " : " + lastSkippedOpActionsList);
            }
            if (null != lastOptimizePddlItems) {
                System.out.println("lastOptimizePddlItems = " + lastOptimizePddlItems.size() + " : " + lastOptimizePddlItems);
            }
            if (null != lastOptimizePddlItemNames) {
                Collections.sort(lastOptimizePddlItemNames);
                System.out.println("lastOptimizePddlItemNames = " + lastOptimizePddlItemNames.size() + " : " + lastOptimizePddlItemNames);
            }
            if (null != lastOptimizePreStartPddlActions) {
                System.out.println("lastOptimizePreStartPddlActions = " + lastOptimizePreStartPddlActions.size() + " : " + lastOptimizePreStartPddlActions);
            }
            if (null != lastOptimizeReplacedPddlActions) {
                System.out.println("lastOptimizeReplacedPddlActions = " + lastOptimizeReplacedPddlActions.size() + " : " + lastOptimizeReplacedPddlActions);
            }
            if (null != lastOptimizeNewPddlActions) {
                System.out.println("lastOptimizeNewPddlActions = " + lastOptimizeNewPddlActions.size() + " : " + lastOptimizeNewPddlActions);
            }
            if (null != lastOptimizeLaterPddlActions) {
                System.out.println("lastOptimizeLaterPddlActions = " + lastOptimizeLaterPddlActions.size() + " : " + lastOptimizeLaterPddlActions);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
        }
    }

    private final Map<String, PoseType> returnPosesByName = new HashMap<>();

    @Nullable
    private String lastTakenPart = null;

    @Nullable
    private String getLastTakenPart() {
        return lastTakenPart;
    }

    private double verySlowTransSpeed = 25.0;

    /**
     * Get the value of verySlowTransSpeed
     *
     * @return the value of verySlowTransSpeed
     */
    public double getVerySlowTransSpeed() {
        return verySlowTransSpeed;
    }

    /**
     * Set the value of verySlowTransSpeed
     *
     * @param verySlowTransSpeed new value of verySlowTransSpeed
     */
    public void setVerySlowTransSpeed(double verySlowTransSpeed) {
        this.verySlowTransSpeed = verySlowTransSpeed;
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
        PoseType returnPose = returnPosesByName.get(part);
        if (null == returnPose) {
            throw new IllegalArgumentException("part=" + part + " not found in returnPosesByName map");
        }
        placePartByPose(out, returnPose);
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
        List<PositionMap> lpm = getPositionMaps();
        if (null != lpm) {
            for (PositionMap pm : lpm) {
                if (null != pm) {
                    pout = pm.correctPose(pout);
                }
            }
        }
        pout.setXAxis(xAxis);
        pout.setZAxis(zAxis);
        return pout;
    }

    public PointType visionToRobotPoint(PointType poseIn) {
        PointType pout = poseIn;
        List<PositionMap> lpm = getPositionMaps();
        if (null != lpm) {
            for (PositionMap pm : lpm) {
                if (null != pm) {
                    pout = pm.correctPoint(pout);
                }
            }
        }
        return pout;
    }

    @MonotonicNonNull
    private AprsJFrame aprsJFrame;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    public @Nullable
    AprsJFrame getAprsJFrame() {
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

    @MonotonicNonNull
    private PddlExecutorJPanel parentPddlExecutorJPanel = null;

    /**
     * Get the value of parentPddlExecutorJPanel
     *
     * @return the value of parentPddlExecutorJPanel
     */
    @Nullable
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
    public void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) throws IOException {
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

    public void takeSimViewSnapshot(String imgLabel, @Nullable PmCartesian pt, @Nullable String label) throws IOException {
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

    private List<PhysicalItem> posesToDetectedItemList(@Nullable Collection<PoseType> poses) {
        List<PhysicalItem> l = new ArrayList<>();
        int i = 0;
        if (null != poses) {
            for (PoseType pose : poses) {
                i++;
                l.add(PhysicalItem.newPhysicalItemNamePoseVisionCycle("pose_" + i, pose, 0));
            }
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

    private void setTitleErrorString(String errString) {
        if (null != aprsJFrame) {
            aprsJFrame.setTitleErrorString(errString);
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
     * @param crclNumber number incremented with each new CRCL program generated
     * (used for catching some potential concurrency problems)
     *
     */
    public void addTakeSnapshots(List<MiddleCommandType> out,
            String title, @Nullable PoseType pose, @Nullable String label, int crclNumber) {
        if (snapshotsEnabled()) {
            addMarkerCommand(out, title, x -> {
                final int curCrclNumber = PddlActionToCrclGenerator.this.crclNumber.get();
                if (crclNumber != curCrclNumber) {
                    setTitleErrorString("crclNumber mismatch " + crclNumber + "!=" + curCrclNumber);
                }
                takeSnapshots("exec", title, pose, label);
            });
        }
    }

    private boolean snapshotsEnabled() {
        return takeSnapshots && aprsJFrame != null && aprsJFrame.snapshotsEnabled();
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        if (suffix.endsWith(".PNG")) {
            System.out.println("suffix = " + suffix);
        }
        if (null != aprsJFrame) {
            return aprsJFrame.createTempFile(prefix, suffix);
        }
        return Utils.createTempFile(prefix, suffix);
    }

    public void takeSnapshots(String prefix, String title, @Nullable PoseType pose, @Nullable String label) {
        if (snapshotsEnabled()) {
            try {
                String fullTitle = title + "_crclNumber-" + String.format("%03d", crclNumber.get()) + "_action-" + String.format("%03d", getLastIndex());
                takeSimViewSnapshot(createTempFile(prefix + "_" + fullTitle, ".PNG"), pose, label);
                if (null == externalPoseProvider) {
                    takeDatabaseViewSnapshot(createTempFile(prefix + "_db_" + fullTitle, ".PNG"));
                }
                takeSimViewSnapshot(createTempFile(prefix + "_pc_" + fullTitle, ".PNG"), poseCacheToDetectedItemList());
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
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     * @throws SQLException if database query fails
     */
    public void testPartPosition(PddlAction action, List<MiddleCommandType> out) throws SQLException, CRCLException, PmException {
        checkDbReady();
        checkSettings();
        String partName = action.getArgs()[0];

        PoseType pose = getPose(partName);
        if (null == pose) {
            logger.log(Level.WARNING, "no pose for " + partName);
            return;
        }
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

    public void setCorrectKitImage() {
        if (null != kitInspectionJInternalFrame) {
            String kitinspectionImageKitPath = kitInspectionJInternalFrame.getKitinspectionImageKitPath();
            String kitImage = kitInspectionJInternalFrame.getKitImage();
            String kitStatusImage = kitinspectionImageKitPath + "/" + kitImage + ".png";
            System.out.println("kitStatusImage " + kitStatusImage);
            Icon icon = kitInspectionJInternalFrame.createImageIcon(kitStatusImage);
            if (null != icon) {
                kitInspectionJInternalFrame.getKitImageLabel().setIcon(icon);
            }
        }
    }

    @SuppressWarnings("nullness")
    private void kitInspectionJInternalFrameKitTitleLabelSetText(String text) {
        if (null != kitInspectionJInternalFrame) {
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
    }

    @SuppressWarnings("nullness")
    private void addToInspectionResultJTextPane(String text) {
        if (null != kitInspectionJInternalFrame) {
            Utils.runOnDispatchThread(() -> {
                try {
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane(text);
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
     *
     * @throws java.lang.InterruptedException interrupted with
     * Thread.interrupt()
     *
     * @throws java.util.concurrent.ExecutionException wrapped exception in
     * another thread servicing waitForCompleteVisionUpdates
     *
     */
    private void inspectKit(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, BadLocationException, InterruptedException, ExecutionException, CRCLException, PmException {
        checkDbReady();
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
        if (null == externalPoseProvider) {
            waitForCompleteVisionUpdates("inspectKit", lastRequiredPartsMap, 15_000);
        }
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
        if (null == correctPartsTray) {
            correctPartsTray = findCorrectKitTray(kitSku);
        }
        PartsTray tray = this.correctPartsTray;
        if (null != tray) {
            try {
                PoseType trayPose = tray.getPartsTrayPose();
                if (null != trayPose) {
                    takeSimViewSnapshot("inspectKit.correctPartsTray.partsTrayPose",
                            new PmCartesian(
                                    trayPose.getPoint().getX(),
                                    trayPose.getPoint().getY(), 0),
                            tray.getPartsTrayName());
                }
                takeSimViewSnapshot("inspectKit.correctPartsTray.slotList",
                        tray.getSlotList());
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            EmptySlotSet = new HashSet<Slot>();
            int numberOfPartsInKit = 0;

            System.out.println("\n\n---Inspecting kit tray " + tray.getPartsTrayName());
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
            List<Slot> slotList = tray.getSlotList();
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
                if (null == partSKU) {
                    logger.log(Level.WARNING, "slot has null partSKU : slot={0}", slot);
                    continue;
                }
                if (partSKU.startsWith("sku_")) {
                    partSKU = partSKU.substring(4).concat("_in_kt");
                }
                if (checkPartTypeInSlot(partSKU, slot) == 1) {
                    numberOfPartsInKit++;
                } else {
                    EmptySlotSet.add(slot);
                }
            }
            if (null != kitInspectionJInternalFrame) {
                if (!EmptySlotSet.isEmpty()) {
                    kitInspectionJInternalFrame.setKitImage(getKitResultImage(EmptySlotSet));
                } else {
                    kitInspectionJInternalFrame.setKitImage("complete");
                }
            }
            setCorrectKitImage();
            if (numberOfPartsInKit == partDesignPartCount) {
                addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #7ef904\">&nbsp;&nbsp;The kit is complete</h3><br>");
            } else {
                try {
                    if (snapshotsEnabled()) {
                        takeSimViewSnapshot(createTempFile("inspectKit-slotList", ".PNG"), slotList);
                        if (null != EmptySlotSet) {
                            takeSimViewSnapshot(createTempFile("inspectKit-EmptySlotSet", ".PNG"), EmptySlotSet);
                        }
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                //if (part_in_kt_found) {
                TakenPartList.clear();
                int nbofmissingparts = partDesignPartCount - numberOfPartsInKit;
                addToInspectionResultJTextPane("<h3 style=\"background-color: #ffb5b5; color: #ffffff\">&nbsp;&nbsp;The kit is missing " + nbofmissingparts + " part(s)</h3>");
                if (null != EmptySlotSet) {
                    for (Slot s : EmptySlotSet) {
                        addToInspectionResultJTextPane("&nbsp;&nbsp;Slot " + s.getSlotName() + " is missing a part of type " + s.getPartSKU() + "<br>");
                    }
                }
                if (nbofmissingparts == 1) {
                    kitInspectionJInternalFrameKitTitleLabelSetText("Missing " + nbofmissingparts + " part. Getting the new part.");
                } else {
                    kitInspectionJInternalFrameKitTitleLabelSetText("Missing " + nbofmissingparts + " parts. Getting the new parts.");
                }
                addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;Recovering from failures</h2>");
                Map<String, List<String>> partSkuMap = new HashMap<>();

                //-- Build a map where the key is the part sku for a slot
                //-- and the value is an arraylist of part_in_pt
                for (Slot s : EmptySlotSet) {

                    List<String> allPartsInPt = new ArrayList<>();
                    String partSKU = s.getPartSKU();
                    if (null == partSKU) {
                        logger.log(Level.WARNING, "slot has null partSKU : slot={0}", s);
                        continue;
                    }
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
                        if (null == partSKU) {
                            logger.log(Level.WARNING, "slot has null partSKU : slot={0}", s);
                            continue;
                        }
                        if (partSKU.startsWith("sku_")) {
                            partSKU = partSKU.substring(4).concat("_in_pt");
                        }
                        addToInspectionResultJTextPane("&nbsp;&nbsp;Getting a list of part_in_pt for sku " + partSKU + "<br>");

                        //-- get list of part_in_pt based on the part sku
                        List<String> listOfParts = partSkuMap.get(partSKU);
                        //-- get the first element in this list and then remove it from the list
                        if (null != listOfParts && listOfParts.size() > 0) {
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
        if (inspectionID.contains("0")) {
            if (null != PlacePartSlotPoseList) {
                PlacePartSlotPoseList.clear();
            }
            correctPartsTray = null;
        }
    }

    private double getVisionToDBRotationOffset() {
        assert (null != this.aprsJFrame) : "null == this.aprsJFrame: @AssumeAssertion(nullness)";
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
    @Nullable
    private PartsTray findCorrectKitTray(String kitSku) throws SQLException {

        assert (null != this.aprsJFrame) : "null == this.aprsJFrame: @AssumeAssertion(nullness)";
        assert (null != this.qs) : "null == this.qs: @AssumeAssertion(nullness)";
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

            String trayName = partsTray.getPartsTrayName();
            if (null == trayName) {
                logger.log(Level.WARNING, "partsTray has null partsTrayName : " + partsTray);
                continue;
            }
            //-- getting the pose for the parts tray 
            PoseType partsTrayPose = qs.getPose(trayName);

            if (partsTrayPose == null) {
                logger.log(Level.WARNING, "recieve null pose for {0}", partsTray.getPartsTrayName());
                continue;
            }
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
                    String name = pt.getPartsTrayName();
                    if (null != name) {
                        dpuPartsTrayListItems.add(new Tray(name, pt.getRotation(), ptX, ptY));
                    } else {
                        logger.log(Level.WARNING, "partsTray has null name : pt={0}", pt);
                    }
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
                if (snapshotsEnabled()) {
                    takeSimViewSnapshot(createTempFile("PlacePartSlotPoseList", ".PNG"), posesToDetectedItemList(PlacePartSlotPoseList));
                    takeSimViewSnapshot(createTempFile("partsTray.getSlotList", ".PNG"), slotList);
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            if (null != trayName) {
                partsTrayListItems.add(new Tray(trayName, partsTray.getRotation(), partsTrayPoseX, partsTrayPoseY));
            } else {
                logger.log(Level.WARNING, "partsTray has null partsTrayName : {0}", partsTray);
            }
            if (count > 0) {
                try {
                    if (snapshotsEnabled()) {
                        takeSimViewSnapshot(createTempFile("dpuPartsTrayList", ".PNG"), dpuPartsTrayListItems);
                        takeSimViewSnapshot(createTempFile("partsTrayList", ".PNG"), partsTrayListItems);
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                correctPartsTray = partsTray;
                System.out.println("Found partstray: " + partsTray.getPartsTrayName());
                return partsTray;
            }
        }
        try {
            if (snapshotsEnabled()) {
                takeSimViewSnapshot(createTempFile("dpuPartsTrayList", ".PNG"), dpuPartsTrayListItems);
                takeSimViewSnapshot(createTempFile("partsTrayList", ".PNG"), partsTrayListItems);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        System.err.println("findCorrectKitTray(" + kitSku + ") returning null. partsTraysList=" + partsTraysList);
        return null;
    }

    private String getKitResultImage(Set<Slot> list) {
        String kitResultImage = "";
        List<Integer> idList = new ArrayList<>();
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
        List<String> allPartsInKt = new ArrayList<>();
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

    private int toolChangePosArgIndex = 0;

    /**
     * Get the value of toolChangePosArgIndex
     *
     * @return the value of toolChangePosArgIndex
     */
    public int getToolChangePosArgIndex() {
        return toolChangePosArgIndex;
    }

    /**
     * Set the value of toolChangePosArgIndex
     *
     * @param toolChangePosArgIndex new value of toolChangePosArgIndex
     */
    public void setToolChangePosArgIndex(int toolChangePosArgIndex) {
        this.toolChangePosArgIndex = toolChangePosArgIndex;
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
     *
     * @throws IllegalStateException if database state is not consistent, eg
     * part not in the correct slot
     * @throws SQLException if database query fails
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    public void takePart(PddlAction action, List<MiddleCommandType> out, @Nullable PddlAction nextPlacePartAction) throws IllegalStateException, SQLException, CRCLException, PmException {
        checkDbReady();
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];

        if (null != kitInspectionJInternalFrame) {
            kitInspectionJInternalFrame.setKitImage("init");
            kitInspectionJInternalFrame.getKitTitleLabel().setText("Building kit");
            setCorrectKitImage();
        }

        takePartByName(partName, nextPlacePartAction, out);
    }

    public void takePartByName(String partName, @Nullable PddlAction nextPlacePartAction, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {
        PoseType pose = getPose(partName);
        if (takeSnapshots) {
            takeSnapshots("plan", "take-part-" + partName + "", pose, partName);
        }
        if (null == pose) {
            if (skipMissingParts) {
                recordSkipTakePart(partName, pose);
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
                    takeSnapshots("plan", "skipping-take-part-next-slot-not-available-" + slot + "-for-part-" + partName + "", pose, partName);
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

    private void recordSkipTakePart(String partName, @Nullable PoseType pose) throws IllegalStateException, SQLException {
        lastTakenPart = null;
        takeSnapshots("plan", "skipping-take-part-" + partName + "", pose, partName);
//        PoseType poseCheck = getPose(partName);
//        System.out.println("poseCheck = " + poseCheck);
    }

    /**
     * Add commands to the list that will go through the motions to take a given
     * part but skip closing the gripper.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     *
     * @throws IllegalStateException if database state is not consistent, eg
     * part not in the correct slot
     * @throws SQLException if database query fails
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    public void fakeTakePart(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {
        checkDbReady();
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];
        MessageType msg = new MessageType();
        msg.setMessage("fake-take-part " + partName + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(msg);
        out.add(msg);

        PoseType pose = getPose(partName);
        if (null == pose) {
            logger.log(Level.WARNING, "no pose for " + partName);
            return;
        }
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

    public void takePartRecovery(String partName, List<MiddleCommandType> out) throws SQLException, BadLocationException, CRCLException, PmException {
        checkDbReady();

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

    private volatile boolean getPoseFailedOnce = false;

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
    @Nullable
    public PoseType getPose(String posename) throws SQLException, IllegalStateException {
        return getPose(posename, false);
    }

    @Nullable
    private PoseType getPose(String posename, boolean ignoreNull) throws SQLException, IllegalStateException {
        if (null != externalPoseProvider) {
            return externalPoseProvider.getPose(posename);
        }
        final AtomicReference<@Nullable Exception> getNewPoseFromDbException = new AtomicReference<>();
        synchronized (poseCache) {
            PoseType pose = poseCache.computeIfAbsent(posename,
                    new Function<String, PoseType>() {
                @SuppressWarnings("nullness")
                @Override
                public PoseType apply(String key) {
                    try {
                        return getNewPoseFromDb(key);
                    } catch (SQLException ex) {
                        getNewPoseFromDbException.set(ex);
                    }
                    return null;
                }
            });

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
                        String errMsg = "two poses in cache are too close : diff=" + diff + " posename=" + posename + ",pose=" + CRCLPosemath.toString(point) + ", entry=" + entry + ", entryPoint=" + CRCLPosemath.toString(entryPoint);
                        takeSnapshots("err", errMsg, pose, posename);
                        throw new IllegalStateException(errMsg);
                    }
                }
            } else if (!ignoreNull) {
                if (debug) {
                    System.err.println("getPose(" + posename + ") returning null.");
                }
                if (debug || !getPoseFailedOnce) {
                    System.err.println("rerunning query for " + posename + " with debug flags");
                    PoseType poseCheck = debugGetNewPoseFromDb(posename);
                    System.out.println("poseCheck = " + poseCheck);
                    getPoseFailedOnce = true;
                }
            }
            return pose;
        }
    }

    @Nullable
    private PoseType getNewPoseFromDb(String posename) throws SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }

        qs.setAprsJFrame(aprsJFrame);
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
        return pose;
    }

    @Nullable
    private PoseType debugGetNewPoseFromDb(String posename) throws SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }

        boolean origDebug = qs.isDebug();
        qs.setDebug(true);
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
        qs.setDebug(origDebug);
        return pose;
    }

    public List<PartsTray> getPartsTrays(String name) throws SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        List<PartsTray> list = new ArrayList<>(qs.getPartsTrays(name));

        return list;
    }

    public int getPartDesignPartCount(String kitName) throws SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        int count = qs.getPartDesignPartCount(kitName);
        return count;
    }

    public List<String> getAllPartsInKt(String name) throws SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";

        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        List<String> partsInKtList = new ArrayList<>(qs.getAllPartsInKt(name));

        return partsInKtList;
    }

    public List<String> getAllPartsInPt(String name) throws SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        List<String> partsInPtList = new ArrayList<>(qs.getAllPartsInPt(name));

        return partsInPtList;
    }

    @Nullable
    volatile PoseType lastTestApproachPose = null;

    private final ConcurrentMap<String, String> toolChangerJointValsMap
            = new ConcurrentHashMap<>();

    public void putToolChangerJointVals(String key, String value) {
        toolChangerJointValsMap.put(key, value);
    }

    @Nullable
    public String getToolChangerJointVals(String key) {
        return toolChangerJointValsMap.get(key);
    }

    public void removeToolChangerJointVals(String key) {
        toolChangerJointValsMap.remove(key);
    }

    public void clearToolChangerJointVals() {
        toolChangerJointValsMap.clear();
    }

    /**
     * Add commands to the list that will test a given part position by opening
     * the gripper and moving to that position but not actually taking the part.
     *
     * @param cmds list of commands to append to
     * @param pose pose to test
     */
    private void testPartPositionByPose(List<MiddleCommandType> cmds, PoseType pose) throws CRCLException, PmException {

        addOpenGripper(cmds);

        checkSettings();

        PoseType lastApproachPose = this.lastTestApproachPose;
        if (null != lastApproachPose) {
            addSetSlowSpeed(cmds);
            addMoveTo(cmds, lastApproachPose, false);
            lastTestApproachPose = null;
        } else {
            addSlowLimitedMoveUpFromCurrent(cmds);
        }
        PoseType approachPose = addZToPose(pose, approachZOffset);

//        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);
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

    private static PoseType addZToPose(PoseType pose, double zOffset) throws CRCLException, PmException {
        PmCartesian cart = new PmCartesian(0, 0, -zOffset);
        PmPose pmPose = CRCLPosemath.toPmPose(pose);
        PmCartesian newTranPos = new PmCartesian();
        Posemath.pmPoseCartMult(pmPose, cart, newTranPos);
//        System.out.println("newTranPos = " + newTranPos);
//        PmCartesian newTran = pmPose.tran.add(newTranPos);
        PmPose approachPmPose = new PmPose(newTranPos, pmPose.rot);
        PoseType approachPose = CRCLPosemath.toPose(approachPmPose);
        return approachPose;
    }

    private void addCheckedOpenGripper(List<MiddleCommandType> cmds) {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";

        addOptionalOpenGripper(cmds, (CrclCommandWrapper ccw) -> {
            AprsJFrame af = aprsJFrame;
            assert (af != null) : "af == null : @AssumeAssertion(nullness)";
            if (af.isObjectViewSimulated()) {
                double distToPart = af.getClosestRobotPartDistance();
                if (distToPart < dropOffMin) {
                    String errString
                            = "Can't take part when distance of " + distToPart + "  less than  " + dropOffMin;
                    double recheckDistance = af.getClosestRobotPartDistance();
                    System.out.println("recheckDistance = " + recheckDistance);
                    setTitleErrorString(errString);
                    checkedPause();
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
     *
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    public void takePartByPose(List<MiddleCommandType> cmds, PoseType pose) throws CRCLException, PmException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = addZToPose(pose, approachZOffset);

        lastTestApproachPose = null;

        PoseType takePose = CRCLPosemath.copy(pose);
        takePose.getPoint().setZ(pose.getPoint().getZ() + takeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true);

        addSettleDwell(cmds);

        addOptionalCloseGripper(cmds, (CrclCommandWrapper ccw) -> {
            AprsJFrame af = aprsJFrame;
            assert (af != null) : "af == null : @AssumeAssertion(nullness)";
            if (af.isObjectViewSimulated()) {
                double distToPart = af.getClosestRobotPartDistance();
                if (distToPart > pickupDistMax) {
                    String errString
                            = "Can't take part when distance of " + distToPart + "  exceeds " + pickupDistMax;
                    setTitleErrorString(errString);
                    checkedPause();
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
        addSetFastSpeed(cmds);
        addMoveTo(cmds, approachPose, true);
    }

    /**
     * Add commands to the list that will go through the motions to take a part
     * at a given pose but not close the gripper to actually take the part.
     *
     * @param cmds list of commands to append to
     * @param pose pose where part is expected
     *
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    public void fakeTakePartByPose(List<MiddleCommandType> cmds, PoseType pose) throws CRCLException, PmException {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = addZToPose(pose, approachZOffset);

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
        String verySlowTransSpeedString = options.get("verySlowTransSpeed");
        if (null != verySlowTransSpeedString && verySlowTransSpeedString.length() > 0) {
            try {
                verySlowTransSpeed = Double.parseDouble(verySlowTransSpeedString);
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
        String pauseInsteadOfRecoverString = options.get("pauseInsteadOfRecover");
        if (null != pauseInsteadOfRecoverString && pauseInsteadOfRecoverString.length() > 0) {
            pauseInsteadOfRecover = Boolean.valueOf(pauseInsteadOfRecoverString);
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
        openGripperCmd.setSetting(1.0);
        addOptionalCommand(openGripperCmd, cmds, cb);
    }

    PoseType copyAndAddZ(PoseType pose_in, double offset, double limit) {
        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        PoseType currentPose = aprsJFrame.getCurrentPose();
        if (null == currentPose) {
            throw new IllegalStateException("currentPose is null");
        }
        PoseType out = CRCLPosemath.copy(currentPose);
        out.getPoint().setZ(Math.min(limit, out.getPoint().getZ() + offset));
        return out;
    }

    private void addMoveUpFromCurrent(List<MiddleCommandType> cmds, double offset, double limit) {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";

        MessageType origMessageCmd = new MessageType();
        origMessageCmd.setMessage("moveUpFromCurrent" + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        addOptionalCommand(origMessageCmd, cmds, (CrclCommandWrapper wrapper) -> {
            MiddleCommandType cmd = wrapper.getWrappedCommand();
            AprsJFrame af = aprsJFrame;
            assert (af != null) : "af == null : @AssumeAssertion(nullness)";
            PoseType pose = af.getCurrentPose();
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

    private void addOpenToolChanger(List<MiddleCommandType> cmds) {
        OpenToolChangerType openToolChangerCmd = new OpenToolChangerType();
        setCommandId(openToolChangerCmd);
        cmds.add(openToolChangerCmd);
    }

    private void addCloseToolChanger(List<MiddleCommandType> cmds) {
        CloseToolChangerType openToolChangerCmd = new CloseToolChangerType();
        setCommandId(openToolChangerCmd);
        cmds.add(openToolChangerCmd);
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

    private void addSetVerySlowSpeed(List<MiddleCommandType> cmds) {
        SetTransSpeedType stst = new SetTransSpeedType();
        setCommandId(stst);
        TransSpeedAbsoluteType tas = new TransSpeedAbsoluteType();
        tas.setSetting(verySlowTransSpeed);
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

    @Nullable
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

    public void addMoveToLookForPosition(List<MiddleCommandType> out, boolean firstAction) {

        String useLookForJointString = options.get("useJointLookFor");
        boolean useLookForJoint = (null != useLookForJointString && useLookForJointString.length() > 0 && Boolean.valueOf(useLookForJointString));
        String lookForJointsString = options.get("lookForJoints");
        if (null == lookForJointsString || lookForJointsString.length() < 1) {
            useLookForJoint = false;
        }

        addOpenGripper(out);
        if (firstAction) {
            addSlowLimitedMoveUpFromCurrent(out);
        }
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
            assert (null != lookForJointsString) : "@AssumeAssertion(nullness)";
            addJointMove(out, lookForJointsString, 1.0);
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

    private void addJointMove(List<MiddleCommandType> out, String jointVals, double speedScale) {
        ActuateJointsType ajCmd = new ActuateJointsType();
        setCommandId(ajCmd);
        ajCmd.getActuateJoint().clear();
        String jointPosStrings[] = jointVals.split("[,]+");
        for (int i = 0; i < jointPosStrings.length; i++) {
            ActuateJointType aj = new ActuateJointType();
            JointSpeedAccelType jsa = new JointSpeedAccelType();
            jsa.setJointAccel(jointAccel * speedScale);
            jsa.setJointSpeed(jointSpeed * speedScale);
            aj.setJointDetails(jsa);
            aj.setJointNumber(i + 1);
            aj.setJointPosition(Double.parseDouble(jointPosStrings[i]));
            ajCmd.getActuateJoint().add(aj);
        }
        out.add(ajCmd);
        atLookForPosition = false;
    }

    @Nullable
    private volatile Thread clearPoseCacheThread = null;
    private volatile StackTraceElement clearPoseCacheTrace @Nullable []  = null;

    public void clearPoseCache() {
        clearPoseCacheThread = Thread.currentThread();
        clearPoseCacheTrace = clearPoseCacheThread.getStackTrace();
        synchronized (poseCache) {
            poseCache.clear();
        }
    }

    @Nullable
    private volatile Thread putPoseCacheThread = null;
    private volatile StackTraceElement putPoseCacheTrace @Nullable []  = null;

    public void putPoseCache(String name, PoseType pose) {
        putPoseCacheThread = Thread.currentThread();
        putPoseCacheTrace = putPoseCacheThread.getStackTrace();
        synchronized (poseCache) {
            poseCache.put(name, CRCLPosemath.copy(pose));
        }
    }

    public Map<String, PoseType> getPoseCache() {
        synchronized (poseCache) {
            return Collections.unmodifiableMap(poseCache);
        }
    }

    public boolean checkAtLookForPosition() {
        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
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
            assert (null != lookForJointsString) : "@AssumeAssertion(nullness)";
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

    private Map<String, Integer> lastRequiredPartsMap = Collections.emptyMap();

    public void clearLastRequiredPartsMap() {
        if (null != lastRequiredPartsMap) {
            lastRequiredPartsMap.clear();
        }
    }

    private void endProgram(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        if (atLookForPosition) {
            atLookForPosition = checkAtLookForPosition();
        }
        if (!atLookForPosition) {
            addMoveToLookForPosition(out, false);
        }
        TakenPartList.clear();
    }

    void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        if (null != aprsJFrame) {
            aprsJFrame.setEnableVisionToDatabaseUpdates(enableDatabaseUpdates, requiredParts);
        }
    }

    private void lookForParts(PddlAction action, List<MiddleCommandType> out, boolean firstAction, boolean lastAction) throws IllegalStateException, SQLException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";
        lastTestApproachPose = null;
        checkSettings();
        checkDbReady();
        if (null == kitInspectionJInternalFrame) {
            KitInspectionJInternalFrame newKitInspectionJInternalFrame
                    = aprsJFrame.getKitInspectionJInternalFrame();
            if (null != newKitInspectionJInternalFrame) {
                this.kitInspectionJInternalFrame = newKitInspectionJInternalFrame;
            }
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
            addMoveToLookForPosition(out, firstAction);
            addAfterMoveToLookForDwell(out, firstAction, lastAction);
            if (null == externalPoseProvider) {
                addMarkerCommand(out, "enableVisionToDatabaseUpdates", x -> {
                    setEnableVisionToDatabaseUpdates(true, immutableRequiredPartsMap);
                });
            }
        } else if (null == externalPoseProvider) {
            addMarkerCommand(out, "enableVisionToDatabaseUpdates", x -> {
                setEnableVisionToDatabaseUpdates(true, immutableRequiredPartsMap);
            });
        } //            addSkipLookDwell(out, firstAction, lastAction);
        if (null == externalPoseProvider) {
            addMarkerCommand(out, "lookForParts.waitForCompleteVisionUpdates", x -> {
                try {
                    waitForCompleteVisionUpdates("lookForParts", immutableRequiredPartsMap, 15_000);
                } catch (InterruptedException | ExecutionException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
            });
        }
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

    private double approachToolChangerZOffset = 150.0;

    /**
     * Get the value of approachToolChangerZOffset
     *
     * @return the value of approachToolChangerZOffset
     */
    public double getApproachToolChangerZOffset() {
        return approachToolChangerZOffset;
    }

    /**
     * Set the value of approachToolChangerZOffset
     *
     * @param approachToolChangerZOffset new value of approachToolChangerZOffset
     */
    public void setApproachToolChangerZOffset(double approachToolChangerZOffset) {
        this.approachToolChangerZOffset = approachToolChangerZOffset;
    }

    private void gotoToolChangerApproach(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {

        lastTestApproachPose = null;
        checkSettings();
        checkDbReady();
        String toolChangerPosName = action.getArgs()[toolChangePosArgIndex];
        addGotoToolChangerApproachByName(out, toolChangerPosName);
    }

    private void addGotoToolChangerApproachByName(List<MiddleCommandType> out, String toolChangerPosName) throws PmException, CRCLException, SQLException, IllegalStateException {
        addSlowLimitedMoveUpFromCurrent(out);
        String jointVals = getToolChangerJointVals(toolChangerPosName);
        if (null != jointVals && jointVals.length() > 0) {
            addDwell(out, 1.0);
            addJointMove(out, jointVals, 0.2);
            addDwell(out, 1.0);
        } else {
            PoseType pose = getPose(toolChangerPosName);
            if (null == pose) {
                throw new IllegalStateException("no pose for " + toolChangerPosName);
            }
            gotoToolChangerApproachByPose(pose, out);
        }
    }

    private void gotoToolChangerApproachByPose(PoseType pose, List<MiddleCommandType> out) throws CRCLException, PmException {
        PoseType approachPose = approachPoseFromToolChangerPose(pose);
        addSetSlowSpeed(out);
        addMoveTo(out, approachPose, false);
    }

    public PoseType approachPoseFromToolChangerPose(PoseType pose) throws PmException, CRCLException {
        PoseType approachPose = addZToPose(pose, approachToolChangerZOffset);
        return approachPose;
    }

    private void dropTool(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {
        lastTestApproachPose = null;
        String toolChangerPosName = action.getArgs()[toolChangePosArgIndex];
        checkSettings();
        checkDbReady();
        PoseType pose = getPose(toolChangerPosName);
        if (null == pose) {
            throw new IllegalStateException("no pose for " + toolChangerPosName);
        }
        addGotoToolChangerApproachByName(out, toolChangerPosName);
        addSetSlowSpeed(out);
        addMoveTo(out, pose, false);
        addOpenToolChanger(out);
        gotoToolChangerApproachByPose(pose, out);
    }

    private void pickupTool(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {
        lastTestApproachPose = null;
        String toolChangerPosName = action.getArgs()[toolChangePosArgIndex];
        checkSettings();
        checkDbReady();
        PoseType pose = getPose(toolChangerPosName);
        if (null == pose) {
            throw new IllegalStateException("no pose for " + toolChangerPosName);
        }
        addGotoToolChangerApproachByName(out, toolChangerPosName);
        addSetVerySlowSpeed(out);
        addMoveTo(out, pose, false);
        addCloseToolChanger(out);
        gotoToolChangerApproachByPose(pose, out);
    }

    private void gotoToolChangerPose(PddlAction action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {

        lastTestApproachPose = null;
        String toolChangerPosName = action.getArgs()[toolChangePosArgIndex];
        checkSettings();
        checkDbReady();
        PoseType pose = getPose(toolChangerPosName);
        if (null == pose) {
            throw new IllegalStateException("no pose for " + toolChangerPosName);
        }
        addSetSlowSpeed(out);
        addMoveTo(out, pose, false);
    }

    private void checkDbReady() throws IllegalStateException {
        if (null == externalPoseProvider) {
            if (null == qs) {
                throw new IllegalStateException("Database not setup and connected.");
            }
        }
    }

    private final AtomicInteger visionUpdateCount = new AtomicInteger();

    private List<PhysicalItem> waitForCompleteVisionUpdates(String prefix, Map<String, Integer> requiredPartsMap, long timeoutMillis)
            throws InterruptedException, ExecutionException {

        assert (aprsJFrame != null) : "aprsJFrame == null : @AssumeAssertion(nullness)";

        String runName = getRunName();
        visionUpdateCount.incrementAndGet();

        XFuture<List<PhysicalItem>> xfl = aprsJFrame.getSingleVisionToDbUpdate();
        int startSimViewRefreshCount = aprsJFrame.getSimViewRefreshCount();
        int startSimViewPublishCount = aprsJFrame.getSimViewPublishCount();
        aprsJFrame.refreshSimView();
        long t0 = System.currentTimeMillis();
        int waitCycle = 0;
        long last_t1 = t0;

        while (!xfl.isDone()) {
            waitCycle++;
            long t1 = System.currentTimeMillis();
            if (timeoutMillis > 0 && t1 - t0 > timeoutMillis) {
                System.out.println("");
                System.err.println("waitForCompleteVisionUpdates " + prefix + " timed out");
                System.err.println("runName=" + getRunName());
                long updateTime = aprsJFrame.getLastSingleVisionToDbUpdateTime();
                long timeSinceUpdate = t1 - updateTime;
                System.err.println("timeSinceUpdate = " + timeSinceUpdate);
                long notifyTime = aprsJFrame.getSingleVisionToDbNotifySingleUpdateListenersTime();
                long timeSinceNotify = t1 - notifyTime;
                System.err.println("timeSinceNotify = " + timeSinceNotify);
                System.err.println("xfl = " + xfl);
                System.err.println("waitCycle = " + waitCycle);
                long lastCycleTime = (t1 - last_t1);
                System.err.println("lastCycleTime = " + lastCycleTime);
                long simViewRefreshTime = aprsJFrame.getLastSimViewRefreshTime();
                long simViewPublishTime = aprsJFrame.getLastSimViewRefreshTime();
                long timeSinceRefresh = simViewRefreshTime - t1;
                System.err.println("timeSinceRefresh = " + timeSinceRefresh);
                long timeSincePublish = simViewPublishTime - t1;
                System.err.println("timeSincePublish = " + timeSincePublish);
                int simViewRefreshCount = aprsJFrame.getSimViewRefreshCount();
                int simViewPublishCount = aprsJFrame.getSimViewPublishCount();
                int simViewRefreshCountDiff = simViewRefreshCount - startSimViewRefreshCount;
                System.err.println("simViewRefreshCountDiff = " + simViewRefreshCountDiff);
                int simViewPublishCountDiff = simViewPublishCount - startSimViewPublishCount;
                System.err.println("simViewPublishCountDiff = " + simViewPublishCountDiff);

                String errMsg = runName + " : waitForCompleteVisionUpdates(" + prefix + ",..." + timeoutMillis + ") timedout. xfl=" + xfl;
                System.err.println(errMsg);
                throw new RuntimeException(errMsg);
            }
            last_t1 = t1;
            if (xfl.isDone()) {
                break;
            }
            if (!aprsJFrame.isEnableVisionToDatabaseUpdates()) {
                if (xfl.isDone()) {
                    break;
                }
                System.err.println("VisionToDatabaseUpdates not enabled as expected.");
                setEnableVisionToDatabaseUpdates(true, requiredPartsMap);
            }
            if (xfl.isDone()) {
                break;
            }
            Thread.sleep(50);
            if (xfl.isDone()) {
                break;
            }
            aprsJFrame.refreshSimView();
            if (aprsJFrame.isClosing()) {
                return Collections.emptyList();
            }
        }
        if (aprsJFrame.isClosing()) {
            return Collections.emptyList();
        }
        List<PhysicalItem> l = xfl.get();
        if (l.isEmpty()) {
            logger.warning(getRunName() + ": waitForCompleteVisionUpdates returing empty list");
        }
        try {
            if (snapshotsEnabled()) {
                aprsJFrame.takeSimViewSnapshot(createTempFile(prefix + "_waitForCompleteVisionUpdates", ".PNG"), l);
                takeDatabaseViewSnapshot(createTempFile(prefix + "_waitForCompleteVisionUpdates_new_database", ".PNG"));
            }
        } catch (IOException ioException) {
            logger.log(Level.SEVERE, null, ioException);
        }
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
//    private void addLookDwell(List<MiddleCommandType> out) {
//        DwellType dwellCmd = new DwellType();
//        setCommandId(dwellCmd);
//        dwellCmd.setDwellTime(lookDwellTime);
//        out.add(dwellCmd);
//    }
    /**
     * Holds information associated with a place part action
     */
    public static class PlacePartInfo {

        private final PddlAction action;
        private final int pddlActionIndex;
        private final int outIndex;
        @Nullable
        private CrclCommandWrapper wrapper = null;
        private final int startSafeAbortRequestCount;

        public PlacePartInfo(PddlAction action, int pddlActionIndex, int outIndex, int startSafeAbortRequestCount) {
            this.action = action;
            this.pddlActionIndex = pddlActionIndex;
            this.outIndex = outIndex;
            this.startSafeAbortRequestCount = startSafeAbortRequestCount;
        }

        @Nullable
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
        checkDbReady();
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
            recordSkipPlacePart(slotName, pose);
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

    private void recordSkipPlacePart(String slotName, @Nullable PoseType pose) throws IllegalStateException, SQLException {
        takeSnapshots("plan", "skipping-place-part-" + getLastTakenPart() + "-in-" + slotName + "", pose, slotName);
//        PoseType poseCheck = getPose(slotName);
//        System.out.println("poseCheck = " + poseCheck);
    }

    private void placePartRecovery(PddlAction action, Slot slot, List<MiddleCommandType> out) throws IllegalStateException, SQLException, BadLocationException {
        checkDbReady();
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

    @Nullable
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

//        addSettleDwell(cmds);
        addSetSlowSpeed(cmds);

        addMoveTo(cmds, placePose, true);

        addSettleDwell(cmds);

        addCheckedOpenGripper(cmds);

        addSettleDwell(cmds);
        addSetFastSpeed(cmds);
        addMoveTo(cmds, approachPose, true);

//        addSettleDwell(cmds);
        this.lastTakenPart = null;
    }

    private void addDwell(List<MiddleCommandType> cmds, double time) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(time);
        cmds.add(dwellCmd);
    }

    private void addSettleDwell(List<MiddleCommandType> cmds) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(settleDwellTime);
        cmds.add(dwellCmd);
    }

    private void addAfterMoveToLookForDwell(List<MiddleCommandType> cmds, boolean firstLook, boolean lastLook) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        if (firstLook) {
            dwellCmd.setDwellTime(afterMoveToLookForDwellTime + firstLookDwellTime);
        } else if (lastLook) {
            dwellCmd.setDwellTime(afterMoveToLookForDwellTime + lastLookDwellTime);
        } else {
            dwellCmd.setDwellTime(afterMoveToLookForDwellTime);
        }
        cmds.add(dwellCmd);
    }

    private void addSkipLookDwell(List<MiddleCommandType> cmds, boolean firstLook, boolean lastLook) {
        DwellType dwellCmd = new DwellType();
        setCommandId(dwellCmd);
        dwellCmd.setDwellTime(skipLookDwellTime);
        cmds.add(dwellCmd);
    }

    private void addMarkerCommand(List<MiddleCommandType> cmds, String message, CRCLCommandWrapperConsumer cb) {
        MessageType messageCmd = new MessageType();
        messageCmd.setMessage(message + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(messageCmd);
        CrclCommandWrapper wrapper = CrclCommandWrapper.wrapWithOnDone(messageCmd, cb);
        cmds.add(wrapper);
    }

    private void addMessageCommand(List<MiddleCommandType> cmds, String message) {
        MessageType messageCmd = new MessageType();
        messageCmd.setMessage(message);
        setCommandId(messageCmd);
        cmds.add(messageCmd);
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
        @Nullable
        private final List<PddlAction> origActions;
        private final int actionsSize;

        public ActionCallbackInfo(int actionIndex, PddlAction action, CrclCommandWrapper wrapper, List<PddlAction> actions, @Nullable List<PddlAction> origActions) {
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

        @Nullable
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

    private AtomicReference<@Nullable ActionCallbackInfo> lastAcbi = new AtomicReference<>();

    private void notifyActionCompletedListeners(int actionIndex, PddlAction action, CrclCommandWrapper wrapper, List<PddlAction> actions, @Nullable List<PddlAction> origActions) {
        ActionCallbackInfo acbi = new ActionCallbackInfo(actionIndex, action, wrapper, actions, origActions);
        lastAcbi.set(acbi);
        action.setExecTime();
        for (Consumer<ActionCallbackInfo> listener : actionCompletedListeners) {
            listener.accept(acbi);
        }
    }

    @Override
    public void close() throws SQLException {
        SQLException firstSQLException = null;
        try {
            if (closeDbConnection && null != dbConnection) {
                dbConnection.close();
            }
        } catch (SQLException sQLException) {
            firstSQLException = sQLException;
        }
        if (null != qs) {
            qs.close();
        }
        if (null != firstSQLException) {
            throw new SQLException(firstSQLException);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        try {
            this.close();
        } catch (Throwable t) {
            // Deliberately ignored.
        }
        super.finalize();
    }

}
