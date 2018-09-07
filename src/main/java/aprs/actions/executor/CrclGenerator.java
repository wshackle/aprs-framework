/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
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
package aprs.actions.executor;

import aprs.database.Slot;
import aprs.database.PartsTray;
import aprs.system.AprsSystem;
import aprs.misc.Utils;
import aprs.database.DbSetup;
import aprs.database.DbSetupBuilder;
import aprs.database.DbSetupJPanel;
import aprs.database.DbSetupListener;
import aprs.database.DbType;
import aprs.database.PhysicalItem;
import aprs.database.QuerySet;
import aprs.database.Tray;

import static aprs.actions.executor.ActionType.LOOK_FOR_PARTS;
import static aprs.actions.executor.ActionType.PLACE_PART;
import static aprs.actions.executor.ActionType.TAKE_PART;

import aprs.kitinspection.KitInspectionJInternalFrame;
import aprs.actions.optaplanner.display.OpDisplayJPanel;
import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.OpActionType;

import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;

import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import crcl.base.ActuateJointType;
import crcl.base.ActuateJointsType;
import crcl.base.AngleUnitEnumType;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLProgramType;
import crcl.base.CRCLStatusType;
import crcl.base.CloseToolChangerType;
import crcl.base.DwellType;
import crcl.base.EndCanonType;
import crcl.base.InitCanonType;
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
import crcl.ui.XFutureVoid;
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

import org.checkerframework.checker.guieffect.qual.SafeEffect;
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

import crcl.utils.CRCLSocket;

import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import rcs.posemath.PmException;
import rcs.posemath.PmPose;
import rcs.posemath.Posemath;

import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.vector;
import java.util.Iterator;
import static java.util.Objects.requireNonNull;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.checkerframework.checker.guieffect.qual.UIEffect;

/**
 * This class is responsible for generating CRCL Commands and Programs from PDDL
 * Action(s).
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CrclGenerator implements DbSetupListener, AutoCloseable {

    public CrclGenerator(ExecutorJPanel parentExecutorJPanel) {
        this.parentExecutorJPanel = parentExecutorJPanel;
        this.aprsSystem = parentExecutorJPanel.getAprsSystem();
    }

    /**
     * Returns the run name which is useful for identifying the run in log files
     * or saving snapshot files.
     *
     * @return the run name
     */
    private String getRunName() {
        if (null != aprsSystem) {
            return aprsSystem.getRunName();
        } else {
            return "";
        }
    }

    public int getCrclNumber() {
        return crclNumber.get();
    }

    private static final Logger LOGGER = Logger.getLogger(CrclGenerator.class.getName());

    private static final boolean DEFAULT_DEBUG = Boolean.getBoolean("CrclGenerator.debug");

    private boolean debug = DEFAULT_DEBUG;

    private void logDebug(String string) {
        if (debug) {
            LOGGER.log(Level.INFO, string);
        }
    }

    private void logError(String string) {
        LOGGER.log(Level.SEVERE, string);
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
        String tail = partName.substring(partName.length() - 2);
        if (tail.charAt(0) == '_' && Character.isDigit(tail.charAt(1))) {
            partName = partName.substring(0, partName.length() - 2);
        }
        return partName;
    }

    @SuppressWarnings("SameParameterValue")
    private List<Action> opActionsToPddlActions(OpActionPlan plan, int start) {
        List<? extends OpAction> listIn = plan.getEffectiveOrderedList(false);
        List<Action> ret = new ArrayList<>();
        OUTER:
        for (int i = start; i < listIn.size(); i++) {
            OpAction opa = listIn.get(i);
            if (null != opa.getOpActionType()) {
                switch (opa.getOpActionType()) {
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
                    && opa.getOpActionType() == PICKUP
                    && listIn.get(i + 1).getOpActionType() == FAKE_DROPOFF) {
                continue;
            }
            ActionType executorActionType = opa.getExecutorActionType();
            if (null != executorActionType) {
                ret.add(new Action(executorActionType, opa.getExecutorArgs(), opa.cost(plan)));
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
    List<OpAction> pddlActionsToOpActions(List<? extends Action> listIn, int start) throws SQLException {
        return pddlActionsToOpActions(listIn, start, null, null, null);
    }

    private final AtomicInteger skippedActions = new AtomicInteger();

    private boolean getReverseFlag() {
        if (aprsSystem != null) {
            return aprsSystem.isReverseFlag();
        }
        return false;
    }

    private List<OpAction> pddlActionsToOpActions(
            List<? extends Action> listIn, int start, int endl@Nullable [], @Nullable List<OpAction> skippedOpActionsList, @Nullable List<Action> skippedPddlActionsList) throws SQLException {
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

    private interface GetPoseFunction {

        @Nullable
        PoseType apply(String name, boolean ignoreNull) throws SQLException;
    }

    private boolean inKitTrayByName(String name) {
        return !name.endsWith("_in_pt") && !name.contains("_in_pt_")
                && (name.contains("_in_kit_") || name.contains("_in_kt_") || name.endsWith("in_kt"));
    }

    private List<OpAction> pddlActionsToOpActions(
            List<? extends Action> listIn,
            int start,
            int endl@Nullable [],
            @Nullable List<OpAction> skippedOpActionsList,
            @Nullable List<Action> skippedPddlActionsList,
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
        ret.add(new OpAction("start", lookForPt.getX(), lookForPt.getY(), OpActionType.START, "NONE", true));
        boolean skipNextPlace = false;
        skippedActions.set(0);
        OpAction takePartOpAction = null;
        Action takePartPddlAction = null;
        for (int i = start; i < listIn.size(); i++) {
            Action pa = listIn.get(i);

            switch (pa.getType()) {
                case TAKE_PART:
                case FAKE_TAKE_PART:
                case TEST_PART_POSITION:
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
                            skippedActions.incrementAndGet();
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
                        if (null == partPt) {
                            throw new IllegalStateException("pose for " + partName + " has null point property");
                        }
                        takePartOpAction = new OpAction(pa.getType(), pa.getArgs(), partPt.getX(), partPt.getY(), posNameToType(partName), inKitTrayByName(partName));
                        takePartPddlAction = pa;
                        skipNextPlace = false;
                    }
                    break;

                case PLACE_PART:
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
                            skippedActions.incrementAndGet();
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
                                skippedActions.addAndGet(2);
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
                            if (null == slotPt) {
                                throw new IllegalStateException("pose for " + slotName + " has null point property");
                            }
                            OpAction placePartOpAction = new OpAction(pa.getType(), pa.getArgs(), slotPt.getX(), slotPt.getY(), posNameToType(slotName), inKitTrayByName(slotName));
                            ret.add(takePartOpAction);
                            ret.add(placePartOpAction);
                        }
                    }
                    skipNextPlace = false;
                    takePartPddlAction = null;
                    takePartOpAction = null;
                    break;

                case LOOK_FOR_PARTS:
                    if (null != endl && endl.length > 1) {
                        endl[1] = i;
                    }
                    return ret;

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

    static private class KitToCheckFailedItemInfo {

        final private @Nullable
        String failedAbsSlotPrpName;

        final private @Nullable
        String failedItemSkuName;

        final private @Nullable
        PhysicalItem failedClosestItem;

        final private @Nullable
        Slot failedAbsSlot;

        private final double failedClosestItemDist;

        public KitToCheckFailedItemInfo(String failedAbsSlotPrpName, String failedItemSkuName, PhysicalItem failedClosestItem, Slot failedAbsSlot, double failedClosestItemDist) {
            this.failedAbsSlotPrpName = failedAbsSlotPrpName;
            this.failedItemSkuName = failedItemSkuName;
            this.failedClosestItem = failedClosestItem;
            this.failedAbsSlot = failedAbsSlot;
            this.failedClosestItemDist = failedClosestItemDist;
        }

        @Override
        public String toString() {
            return "KitToCheckFailedItemInfo{" + "failedAbsSlotPrpName=" + failedAbsSlotPrpName + ", failedItemSkuName=" + failedItemSkuName + ", failedClosestItem=" + failedClosestItem + ", failedAbsSlot=" + failedAbsSlot + ", failedClosestItemDist=" + failedClosestItemDist + '}';
        }

    };

    static private class KitToCheckInstanceInfo {

        final String instanceName;
        final Map<String, PhysicalItem> closestItemMap;
        final Map<String, String> closestItemNameMap;
        final List<Slot> absSlots;
        final Map<String, String> itemSkuMap;

        final List<KitToCheckFailedItemInfo> failedItems;

        int failedSlots;

        int getFailedSlots() {
            return failedSlots;
        }

        KitToCheckInstanceInfo(String instanceName, List<Slot> absSlots) {
            this.instanceName = instanceName;
            this.absSlots = absSlots;
            closestItemMap = new HashMap<>();
            itemSkuMap = new HashMap<>();
            closestItemNameMap = new HashMap<>();
            failedItems = new ArrayList<>();
        }

        @Override
        public String toString() {
            return "KitToCheckInstanceInfo{" + "instanceName=" + instanceName + ", closestItemNameMap=" + closestItemNameMap + ", itemSkuMap=" + itemSkuMap + ", failedSlots=" + failedSlots + '}';
        }

    }

    static private class KitToCheck {

        final String name;
        final Map<String, String> slotMap;

        @Nullable
        List<String> kitInstanceNames;

        Map<String, KitToCheckInstanceInfo> instanceInfoMap = Collections.emptyMap();

        KitToCheck(String name, Map<String, String> slotMap) {
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
            LOGGER.info("diff = " + diff);
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
    private final String messageColorH3 = "#e0e0e0";
    Color warningColor = new Color(100, 71, 71);

    /**
     * Get the value of takeSnapshots
     * <p>
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
     * <p>
     * When takeSnaphots is true image files will be saved when most actions are
     * planned and some actions are executed. This may be useful for debugging.
     *
     * @param takeSnapshots new value of takeSnapshots
     */
    public void setTakeSnapshots(boolean takeSnapshots) {
        this.takeSnapshots = takeSnapshots;
    }

    private volatile List<PositionMap> positionMaps = Collections.emptyList();

    /**
     * Get the list of PositionMap's used to transform or correct poses from the
     * database before generating CRCL Commands to be sent to the robot.
     * <p>
     * PositionMaps are similar to transforms in that they can represent
     * position offsets or rotations. But they can also represent changes in
     * scale or localized corrections due to distortion or imperfect kinematics.
     *
     * @return list of position maps.
     */
    private List<PositionMap> getPositionMaps() {
        return positionMaps;
    }

    /**
     * Set the list of PositionMap's used to transform or correct poses from the
     * database before generating CRCL Commands to be sent to the robot.
     * <p>
     * PositionMaps are similar to transforms in that they can represent
     * position offsets or rotations. But they can also represent changes in
     * scale or localized corrections due to distortion or imperfect kinematics.
     *
     * @param errorMap list of position maps.
     */
    @SafeEffect
    public synchronized void setPositionMaps(List<PositionMap> errorMap) {
        this.positionMaps = errorMap;
    }

    public interface PoseProvider {

        List<PhysicalItem> getNewPhysicalItems();

        @Nullable
        PoseType getPose(String name);

        List<String> getInstanceNames(String skuName);
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
            return qs.isConnected();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "", ex);
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

    /**
     * Get the value of debug
     * <p>
     * When debug is true additional messages will be printed to the console.
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     * <p>
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
    private synchronized void setDbConnection(java.sql.@Nullable Connection dbConnection) {
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
                    logger.log(Level.SEVERE, "", ex);
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
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "", ex);
        }

    }

    /**
     * Get the database setup object.
     *
     * @return database setup object.
     */
    @Nullable
    @SuppressWarnings({"unused"})
    public DbSetup getDbSetup() {
        return dbSetup;
    }

    private boolean dbConnectionIsClosedOrNull() {
        try {
            return dbConnection == null || dbConnection.isClosed();
        } catch (SQLException ex) {
            Logger.getLogger(CrclGenerator.class.getName()).log(Level.SEVERE, "", ex);
            return true;
        }
    }

    /**
     * Set the database setup object.
     *
     * @param dbSetup new database setup object to use.
     * @return future providing status on when the connection is complete.
     */
    public XFutureVoid setDbSetup(DbSetup dbSetup) {

        this.dbSetup = dbSetup;
        if (null != this.dbSetup && this.dbSetup.isConnected()) {
            if (null == dbSetup.getDbType() || DbType.NONE == dbSetup.getDbType()) {
                throw new IllegalArgumentException("dbSetup.getDbType() =" + dbSetup.getDbType());
            }
            if (dbConnectionIsClosedOrNull()) {
                XFutureVoid ret = new XFutureVoid("PddlActionToCrclGenerator.setDbSetup");
                try {
                    final StackTraceElement stackTraceElemArray[] = Thread.currentThread().getStackTrace();
                    DbSetupBuilder.connect(dbSetup).handle(
                            "PddlActionToCrclGenerator.handleDbConnect",
                            (java.sql.Connection c, Throwable ex) -> {
                                if (null != c) {
                                    setDbConnection(c);
                                    ret.complete(null);
                                }
                                if (null != ex) {
                                    Logger.getLogger(DbSetupJPanel.class.getName()).log(Level.SEVERE, "", ex);
                                    System.err.println("Called from :");
                                    for (StackTraceElement aStackTraceElemArray : stackTraceElemArray) {
                                        System.err.println(aStackTraceElemArray);
                                    }
                                    System.err.println();
                                    System.err.println("Exception handled at ");
                                    if (null != aprsSystem) {
                                        setTitleErrorString("Database error: " + ex.toString());
                                    }
                                    ret.completeExceptionally(ex);
                                }
                                return c;
                            });
                    logDebug("PddlActionToCrclGenerator connected to database of type " + dbSetup.getDbType() + " on host " + dbSetup.getHost() + " with port " + dbSetup.getPort());
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "", ex);
                }
                return ret;
            }
            return XFutureVoid.completedFutureWithName("setDbSetup.(dbConnection!=null)");
        } else {
            setDbConnection(null);
            return XFutureVoid.completedFutureWithName("setDbSetup.setDbConnnection(null)");
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
    private volatile List<Action> lastActionsList = null;

    private volatile int lastAtLastIndexIdx = -1;
    @Nullable
    private volatile List<Action> lastAtLastIndexList = null;
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

    @SuppressWarnings({"unused"})
    private volatile StackTraceElement setLastActionsIndexTrace@Nullable []  = null;

    @SuppressWarnings({"unused"})
    private volatile StackTraceElement firstSetLastActionsIndexTrace@Nullable []  = null;

    //    private final ConcurrentLinkedDeque<StackTraceElement[]> setLastActionsIndexTraces
//            = new ConcurrentLinkedDeque<>();
    private void setLastActionsIndex(@Nullable List<Action> actionsList, int index) {
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

    private void incLastActionsIndex() {
//        this.lastActionsList = ((null != actionsList)?new ArrayList<>(actionsList):null);
        lastIndex.incrementAndGet();
    }

    /**
     * Get a map of options as a name to value map.
     *
     * @return options
     */
    public Map<String, String> getOptions() {
        if (options == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(options);
    }

    private volatile boolean settingsChecked = false;

    /**
     * Set the options with a name to value map.
     *
     * @param options new value of options map
     */
    public synchronized void setOptions(Map<String, String> options) {
        this.options = new HashMap<>(options);
        settingsChecked = false;
    }

    private volatile boolean toolHolderOperationEnabled = true;

    public void setToolHolderOperationEnabled(boolean enable) {
        this.toolHolderOperationEnabled = enable;
        logDebug("toolHolderOperationEnabled = " + toolHolderOperationEnabled + ", getRunName() = " + getRunName());
        this.expectedJoint0Val = Double.NaN;
    }

    public boolean isToolHolderOperationEnabled() {
        return toolHolderOperationEnabled;
    }

    private boolean doInspectKit = false;
    private boolean requireNewPoses = false;

    @Nullable
    private Action getNextPlacePartAction(int lastIndex, List<Action> actions) {
        for (int i = lastIndex + 1; i < actions.size(); i++) {
            Action action = actions.get(i);
            switch (action.getType()) {
                case PLACE_PART:
                    return action;
                case LOOK_FOR_PARTS:
                case END_PROGRAM:
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
     * @throws IllegalStateException if database not connected
     * @throws SQLException if query of the database failed
     * @throws java.lang.InterruptedException another thread called
     * Thread.interrupt() while retrieving data from database
     * @throws PendantClientInner.ConcurrentBlockProgramsException pendant
     * client in a state blocking program execution
     * @throws java.util.concurrent.ExecutionException exception occurred in
     * another thread servicing the waitForCompleteVisionUpdates
     * @throws crcl.utils.CRCLException a failure occurred while composing or
     * sending a CRCL command.
     * @throws rcs.posemath.PmException failure occurred while computing a pose
     * such as an invalid transform
     */
    public List<MiddleCommandType> generate(List<Action> actions, int startingIndex, Map<String, String> options, int startSafeAbortRequestCount)
            throws IllegalStateException, SQLException, InterruptedException, ExecutionException, PendantClientInner.ConcurrentBlockProgramsException, CRCLException, PmException {
        assert null != aprsSystem : "(null == aprsSystemInterface)";
        GenerateParams gparams = new GenerateParams();
        gparams.actions = actions;
        gparams.startingIndex = startingIndex;
        gparams.options = options;
        gparams.startSafeAbortRequestCount = startSafeAbortRequestCount;
        gparams.replan = !aprsSystem.isCorrectionMode();
        return generate(gparams);
//        return generate(actions, startingIndex, options, startSafeAbortRequestCount, true, null, null);
    }

    private boolean diffActions(List<Action> acts1, List<Action> acts2) {
        if (acts1.size() != acts2.size()) {
            logDebug("acts1.size() != acts2.size(): acts1.size()=" + acts1.size() + ", acts2.size()=" + acts2.size());
            return true;
        }
        for (int i = 0; i < acts1.size(); i++) {
            Action act1 = acts1.get(i);
            Action act2 = acts2.get(i);
            if (!Objects.equals(act1.asPddlLine(), act2.asPddlLine())) {
                logDebug("acts1.get(i) != acts2.get(i): i=" + i + ",acts1.get(i)=" + acts1.get(i) + ", acts2.get(i)=" + acts2.get(i));
                return true;
            }
        }
        return false;
    }

    @Nullable
    private volatile Thread genThread = null;
    private volatile StackTraceElement genThreadSetTrace@Nullable []  = null;

    private static boolean cmdsContainNonWrapper(List<MiddleCommandType> cmds) {
        for (MiddleCommandType cmd : cmds) {
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
        for (MiddleCommandType cmd : cmds) {
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
     * <p>
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
        List<Action> actions;
        int startingIndex;
        @MonotonicNonNull
        Map<String, String> options;
        int startSafeAbortRequestCount;
        boolean replan;
        boolean optiplannerUsed;

        @MonotonicNonNull
        @SuppressWarnings({"unused"})
        List<Action> origActions;

        @MonotonicNonNull
        List<PhysicalItem> newItems;

        @Nullable
        RunOptoToGenerateReturn runOptoToGenerateReturn;

        boolean newItemsReceived;
        final int startingVisionUpdateCount;

        GenerateParams() {
            this.startingVisionUpdateCount = visionUpdateCount.get();
        }

        @Override
        public String toString() {
            return "GenerateParams{" + "actions=" + actions + ",\n startingIndex=" + startingIndex + ",\n options=" + options + ",\n startSafeAbortRequestCount=" + startSafeAbortRequestCount + ",\n\n  replan=" + replan + ",\n optiplannerUsed=" + optiplannerUsed + ",\n origActions=" + origActions + ",\n newItems=" + newItems + ",\n runOptoToGenerateReturn=" + runOptoToGenerateReturn + ",\n newItemsReceived=" + newItemsReceived + ",\n startingVisionUpdateCount=" + startingVisionUpdateCount + '}';
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
    private volatile List<List<Action>> takePlaceActions = null;

    private static void checkTakePlaceActions(@Nullable List<List<Action>> tplist, List<Action> fullList) {
        if (null == tplist) {
            return;
        }
        int tplistTotal = 0;
        for (int i = 0; i < tplist.size() - 1; i++) {
            List<Action> l = tplist.get(i);
            for (Action act : l) {
                if (act.getExecuted()) {
                    tplistTotal++;
                }
            }
        }
        if (tplist.size() > 0) {
            tplistTotal += tplist.get(tplist.size() - 1).size();
        }
        int fullListTotal = 0;
        for (Action act : fullList) {
            switch (act.getType()) {
                case TAKE_PART:
                case PLACE_PART:
                    fullListTotal++;
                    break;
            }
        }
        if (tplistTotal > fullListTotal) {
            long time = System.currentTimeMillis();
            LOGGER.warning("tplistTotal > fullListTotal : redundant or repeated actions suspected");
            for (int i = 0; i < tplist.size(); i++) {
                LOGGER.info("i = " + i);
                for (Action act : tplist.get(i)) {
                    LOGGER.info((act.getExecuted() ? (time - act.getExecTime()) : "--") + " : " + act.asPddlLine());
                }
            }
            LOGGER.warning("tplistTotal > fullListTotal : redundant or repeated actions suspected");
        }
    }

    /**
     * Generate a list of CRCL commands from a list of PddlActions starting with
     * the given index, using the provided optons.
     *
     * @param gparams object containing the parameters of the method
     * @return list of CRCL commands
     * @throws IllegalStateException if database not connected
     * @throws SQLException if query of the database failed
     * @throws java.lang.InterruptedException another thread called
     * Thread.interrupt() while retrieving data from database
     * @throws java.util.concurrent.ExecutionException exception occurred in
     * another thread servicing the waitForCompleteVisionUpdates
     */
    private synchronized List<MiddleCommandType> generate(GenerateParams gparams)
            throws IllegalStateException, SQLException, InterruptedException, PendantClientInner.ConcurrentBlockProgramsException, ExecutionException, CRCLException, PmException {

        assert (null != this.aprsSystem) : "null == aprsSystemInterface";
        assert (null != gparams.options) : "null == gparams.options";
        assert (null != gparams.actions) : "null == gparams.actions";
        AprsSystem localAprsSystem = this.aprsSystem;
        if (null == localAprsSystem) {
            throw new IllegalStateException("aprsJframe is null");
        }
        final Thread curThread = Thread.currentThread();
        if (null == genThread) {
            genThread = curThread;
            genThreadSetTrace = curThread.getStackTrace();
        } else if (genThread != curThread) {
            logError("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
            throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
        }
        List<MiddleCommandType> cmds = new ArrayList<>();
        if (null != solver && gparams.replan) {
            return runOptaPlanner(gparams.actions, gparams.startingIndex, gparams.options, gparams.startSafeAbortRequestCount);
        } else {
            String messageString = "\n"
                    + "gparams.startingIndex=" + gparams.startingIndex + "\n"
                    + "gparams.replan=" + gparams.replan + "\n"
                    + "gparams.actions.size()=" + gparams.actions.size() + "\n"
                    + "gparams.actions=" + gparams.actions + "\n"
                    + "gparams.actions.subList(gparams.startingIndex, gparams.actions.size())=" + gparams.actions.subList(gparams.startingIndex, gparams.actions.size()) + "\n";
            addMessageCommand(cmds, messageString);
//            if(gparams.startingIndex > 4) {
//                logDebug(messageString);
//            }
        }
        this.startSafeAbortRequestCount = gparams.startSafeAbortRequestCount;
        checkDbReady();
        if (localAprsSystem.isRunningCrclProgram()) {
            throw new IllegalStateException("already running crcl while trying to generate it");
        }

        int blockingCount = localAprsSystem.startBlockingCrclPrograms();

        ActionCallbackInfo acbi = lastAcbi.get();
        if (null != acbi && includeEndNormalActionMarker && includeEndProgramMarker && includeSkipNotifyMarkers) {
            int actionIndex = acbi.getActionIndex();
            if (gparams.startingIndex < actionIndex) {
                if (gparams.startingIndex != 0 || actionIndex < acbi.getActionsSize() - 2) {
                    logDebug("Thread.currentThread() = " + Thread.currentThread());
                    List<Action> actions = acbi.getActions();
                    boolean actionsChanged = diffActions(gparams.actions, actions);
                    logDebug("actionsChanged = " + actionsChanged);
                    Action action = acbi.getAction();
                    String errString = "generate called with startingIndex=" + gparams.startingIndex + ",acbi.getActionsSize()=" + acbi.getActionsSize() + " and acbi.actionIndex=" + actionIndex + ", lastIndex=" + lastIndex + ", acbi.action.=" + action;
                    System.err.println(errString);
                    System.err.println("acbi = " + acbi);
                    localAprsSystem.setTitleErrorString(errString);
                    throw new IllegalStateException(errString);
                }
            }
        }
        try {

            setOptions(gparams.options);
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
            List<Action> newTakePlaceList = new ArrayList<>();
            if (null != takePlaceActions) {
                takePlaceActions.add(newTakePlaceList);
            }
            addSetUnits(cmds);

            String waitForCompleteVisionUpdatesCommentString = "generate(start=" + gparams.startingIndex + ",crclNumber=" + currentCrclNumber + ")";
            boolean isNewRetArray[] = new boolean[1];
            gparams.newItems = updateStalePoseCache(gparams.startingIndex, acbi, gparams.newItems, waitForCompleteVisionUpdatesCommentString, isNewRetArray);
            if (isNewRetArray[0]) {
                gparams.newItemsReceived = true;
            }
            takeSnapshots("plan", "generate(start=" + gparams.startingIndex + ",crclNumber=" + currentCrclNumber + ")", null, null);
            final List<Action> fixedActionsCopy = Collections.unmodifiableList(new ArrayList<>(gparams.actions));
            final List<Action> fixedOrigActionsCopy = (gparams.origActions == null) ? null : Collections.unmodifiableList(new ArrayList<>(gparams.actions));

            int skipStartIndex = -1;
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
            checkSettings();
            for (this.setLastActionsIndex(gparams.actions, gparams.startingIndex); getLastIndex() < gparams.actions.size(); incLastActionsIndex()) {

                final int idx = getLastIndex();
                Action action = gparams.actions.get(idx);
                if (debug) {
                    logDebug("action[" + idx + "] = " + action);
                }
                if (skipMissingParts) {
                    boolean needSkip = false;
                    switch (action.getType()) {
                        case TAKE_PART:
                            if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                                logger.log(Level.WARNING, "newItems.isEmpty() on take-part for run " + getRunName());
                            }
                            String partName = action.getArgs()[takePartArgIndex];
                            PoseType pose = getPose(partName, getReverseFlag());
                            if (pose == null) {
                                skippedParts.add(partName);
                                recordSkipTakePart(partName, pose);
                                if (skipStartIndex < 0) {
                                    skipStartIndex = idx;
                                }
                                needSkip = true;
                            } else {
                                foundParts.add(partName);
                                skipStartIndex = -1;
                                needSkip = false;
                            }
                            break;

                        case PLACE_PART:
                            if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                                logger.log(Level.WARNING, "newItems.isEmpty() on place-part for run " + getRunName());
                            }
                            String slotName = action.getArgs()[placePartSlotArgIndex];
                            if (null == lastTakenPart) {
                                PoseType slotPose = getPose(slotName, getReverseFlag());
                                recordSkipPlacePart(slotName, slotPose);
                                if (skipStartIndex < 0) {
                                    skipStartIndex = idx;
                                }
                                needSkip = true;
                            } else {
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
                    case TAKE_PART:
                        newTakePlaceList.add(action);
                        if (null != takePlaceActions) {
                            checkTakePlaceActions(takePlaceActions, gparams.actions);
                        }
                        if (gparams.newItems.isEmpty() && poseCache.isEmpty()) {
                            logger.log(Level.WARNING, "newItems.isEmpty() on take-part for run " + getRunName());
                        }
                        takePart(action, cmds, getNextPlacePartAction(idx, gparams.actions));
                        break;

                    case FAKE_TAKE_PART:
                        fakeTakePart(action, cmds);
                        break;

                    case TEST_PART_POSITION:
                        testPartPosition(action, cmds);
                        break;
                    case LOOK_FOR_PARTS:
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
                                logDebug("foundParts = " + foundParts);
                                logDebug("skippedParts = " + skippedParts);
                                logDebug("poseCache.keySet() = " + poseCache.keySet());
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
                            if (!gparams.newItemsReceived && visionUpdateCount.get() == gparams.startingVisionUpdateCount) {
                                gparams.newItems = checkNewItems(waitForCompleteVisionUpdatesCommentString);
                            }
                            logger.log(Level.FINE, "Processed wrapper only commands without sending to robot.");
                            if (null != gparams.runOptoToGenerateReturn) {
                                gparams.runOptoToGenerateReturn.newIndex = idx + 1;
                                if (!skippedParts.isEmpty()) {
                                    logDebug("foundParts = " + foundParts);
                                    logDebug("skippedParts = " + skippedParts);
                                    logDebug("poseCache.keySet() = " + poseCache.keySet());
                                }
                                if (cmd0Id > 1) {
                                    commandId.set(cmd0Id - 1);
                                }
                                return Collections.emptyList();
                            }
                        }
                        break;

                    case GOTO_TOOL_CHANGER_APPROACH:
                        gotoToolChangerApproach(action, cmds);
                        break;

                    case GOTO_TOOL_CHANGER_POSE:
                        gotoToolChangerPose(action, cmds);
                        break;

                    case DROP_TOOL_BY_HOLDER:
                        dropToolByHolder(action, cmds);
                        break;

                    case DROP_TOOL_ANY:
                        dropToolAny(action, cmds);
                        break;

                    case PICKUP_TOOL_BY_HOLDER:
                        pickupToolByHolder(action, cmds);
                        break;

                    case PICKUP_TOOL_BY_TOOL:
                        pickupToolByTool(action, cmds);
                        break;

                    case SWITCH_TOOL:
                        switchTool(action, cmds);
                        break;

                    case END_PROGRAM:
                        endProgram(action, cmds);
                        updateActionToCrclArrays(idx, cmds);
                        if (includeEndProgramMarker) {
                            String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                            addNotifyMarker(cmds, end_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                        }
                        if (!skippedParts.isEmpty()) {
                            logDebug("foundParts = " + foundParts);
                            logDebug("skippedParts = " + skippedParts);
                            logDebug("poseCache.keySet() = " + poseCache.keySet());
                        }
                        return cmds;

                    case PLACE_PART:
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

                    case PAUSE:
                        pause(action, cmds);
                        break;

//                    case INSPECT_KIT: {
//                        assert (gparams.startingIndex == idx) : "inspect-kit startingIndex(" + gparams.startingIndex + ") != lastIndex(" + idx + ")";
//                        if (doInspectKit) {
//                            try {
//                                inspectKit(action, cmds);
//                            } catch (Exception ex) {
//                                logger.log(Level.SEVERE, "", ex);
//                            }
//                        }
//                    }
//                    break;
                    case CLEAR_KITS_TO_CHECK:
                        clearKitsToCheck(action, cmds, gparams);
                        break;

                    case ADD_KIT_TO_CHECK:
                        addKitToCheck(action, cmds, gparams);
                        break;

                    case SET_CORRECTION_MODE:
                        aprsSystem.setCorrectionMode(Boolean.valueOf(action.getArgs()[0]));
                        break;

                    case CHECK_KITS:
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
                    logDebug("foundParts = " + foundParts);
                    logDebug("skippedParts = " + skippedParts);
                    logDebug("poseCache.keySet() = " + poseCache.keySet());
                }
            }
            if (localAprsSystem.isRunningCrclProgram()) {
                throw new IllegalStateException("already running crcl while trying to generate it");
            }
        } catch (Exception ex) {
            System.err.println("getRunName() = " + getRunName());
            System.err.println("poseCache.keySet() = " + poseCache.keySet());
            if (null != gparams.newItems) {
                System.err.println("gparams.newItems.size()=" + gparams.newItems.size());
                System.err.println("gparams.newItems=" + gparams.newItems);
                for (PhysicalItem newItem : gparams.newItems) {
                    System.err.println(newItem.getName() + " : " + newItem.x + "," + newItem.y);
                }
            }
            Logger.getLogger(CrclGenerator.class.getName()).log(Level.SEVERE, "", ex);
            System.err.println();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new IllegalStateException(ex);
            }
        } finally {
            localAprsSystem.stopBlockingCrclPrograms(blockingCount);
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

    private void addNotifyMarker(List<MiddleCommandType> cmds, String end_action_string, final int idx, Action action, final List<Action> fixedActionsCopy, @Nullable List<Action> fixedOrigActionsCopy) {
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

        if (startingIndex > 0 && null != acbi) {
            Action acbiAction = acbi.getAction();
            if (null != acbiAction) {
                switch (acbiAction.getType()) {

                    case LOOK_FOR_PARTS:
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

    private boolean isLookForPartsAction(Action action) {
        switch (action.getType()) {
            case LOOK_FOR_PARTS:
                return true;

            default:
                return false;
        }
    }

    private int firstLookForPartsIndex(List<Action> l, int startingIndex) {
        for (int i = startingIndex; i < l.size(); i++) {
            if (isLookForPartsAction(l.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMoveAction(Action action) {
        switch (action.getType()) {
            case PLACE_PART:
            case TAKE_PART:
            case FAKE_TAKE_PART:
            case TEST_PART_POSITION:
                return true;

            default:
                return false;
        }
    }

    private int firstMoveIndex(List<Action> l, int startingIndex) {
        for (int i = startingIndex; i < l.size(); i++) {
            if (isMoveAction(l.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private List<MiddleCommandType> runOptaPlanner(
            List<Action> actions,
            int startingIndex, Map<String, String> options1,
            int startSafeAbortRequestCount1)
            throws IllegalStateException, InterruptedException, SQLException, PendantClientInner.ConcurrentBlockProgramsException, ExecutionException, CRCLException, PmException {
        assert (null != this.aprsSystem) : "null == aprsSystemInterface";
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
//        logDebug("runOptaPlanner: rc = " + rc);
        long t0 = System.currentTimeMillis();
        setOptions(options1);
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
            gparams.newItemsReceived = true;
        }
        List<Action> origActions = new ArrayList<>(actions);
        List<Action> fullReplanPddlActions = optimizePddlActionsWithOptaPlanner(actions, startingIndex, newItems);
        int skippedActionsCount = skippedActions.get();
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActionsCount || fullReplanPddlActions.size() < 1) {
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActionsCount);
        }
        if (fullReplanPddlActions == actions) {
            gparams.replan = false;
            return generate(gparams);
        }
        List<Action> copyFullReplanPddlActions = new ArrayList<>(fullReplanPddlActions);
        synchronized (actions) {
            actions.clear();
            actions.addAll(fullReplanPddlActions);
        }
        skippedActionsCount = skippedActions.get();
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActionsCount || actions.size() < 1) {
            logDebug("copyFullReplanPddlActions = " + copyFullReplanPddlActions);
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActionsCount);
        }
        if (debug) {
            showPddlActionsList(actions);
        }
        gparams.optiplannerUsed = true;
        MessageType messageCmd = new MessageType();
        String messageString
                = "\nstartingIndex=" + startingIndex + "\n"
                + "origActions.size()=" + origActions.size() + "\n"
                + "origActions=" + origActions + "\n"
                + "origActions.subList(startingIndex, origActions.size())=" + origActions.subList(startingIndex, origActions.size()) + "\n"
                + "newItems=" + newItems + "\n"
                + "copyFullReplanPddlActions.size()=" + copyFullReplanPddlActions.size() + "\n"
                + "copyFullReplanPddlActions=" + copyFullReplanPddlActions + "\n"
                + "copyFullReplanPddlActions.subList(gparams.startingIndex, origActions.size())=" + copyFullReplanPddlActions.subList(gparams.startingIndex, origActions.size()) + "\n";
        logDebug(messageString);
        messageCmd.setMessage(messageString);
        setCommandId(messageCmd);
        List<MiddleCommandType> newCmds = generate(gparams);
        newCmds.add(0, messageCmd);
        if (debug) {
            showCmdList(newCmds);
        }
        return newCmds;
    }

    private void showPddlActionsList(List<Action> actions) throws InterruptedException {
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
            Logger.getLogger(CrclGenerator.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private void showCmdList(List<MiddleCommandType> newCmds) throws InterruptedException {
        StringBuilder cmdSb = new StringBuilder();
        for (int i = 0; i < newCmds.size(); i++) {
            cmdSb.append(i);
            cmdSb.append(" ");
            cmdSb.append(CRCLSocket.commandToSimpleString(newCmds.get(i)));
            cmdSb.append("\n");
        }
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                String newCmdsText = MultiLineStringJPanel.editText(cmdSb.toString());
            });
        } catch (InvocationTargetException ex) {
            Logger.getLogger(CrclGenerator.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private volatile @Nullable
    List<OpAction> lastSkippedOpActionsList;
    private volatile @Nullable
    List<Action> lastSkippedPddlActionsList;
    private volatile @Nullable
    List<OpAction> lastOpActionsList;
    private volatile @Nullable
    List<PhysicalItem> lastOptimizePddlItems;
    private volatile @Nullable
    List<String> lastOptimizePddlItemNames;
    private volatile @Nullable
    List<Action> lastOptimizePreStartPddlActions;
    private volatile @Nullable
    List<Action> lastOptimizeReplacedPddlActions;
    private volatile @Nullable
    List<Action> lastOptimizeNewPddlActions;
    private volatile @Nullable
    List<Action> lastOptimizeLaterPddlActions;
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
                .andThen(CrclGenerator::nonNullStream);
    }

    private static String badScores(double solveScore, double inScore) {
        return "solveScore < inScore : solveScore=" + solveScore + " inscore=" + inScore;
    }

    private final AtomicInteger solverRunCount = new AtomicInteger();

    private List<Action> optimizePddlActionsWithOptaPlanner(
            List<Action> actions,
            int startingIndex,
            List<PhysicalItem> items) throws SQLException {
        assert (null != this.aprsSystem) : "null == aprsSystemInterface";

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
        List<Action> skippedPddlActionsList = new ArrayList<>();
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

            MutableMultimap<String, Action> takePartMap
                    = Lists.mutable.ofAll(actions.subList(endl[0], endl[1]))
                    .select(action -> action.getType().equals(TAKE_PART) && !inKitTrayByName(action.getArgs()[takePartArgIndex]))
                    .groupBy(action -> posNameToType(action.getArgs()[takePartArgIndex]));

            for (String partTypeName : takePartMap.keySet()) {
                MutableCollection<PhysicalItem> thisPartTypeItems
                        = availItemsMap.get(partTypeName);
                MutableCollection<Action> thisPartTypeActions
                        = takePartMap.get(partTypeName);
                if (thisPartTypeItems.size() > thisPartTypeActions.size() && thisPartTypeActions.size() > 0) {
                    for (PhysicalItem item : thisPartTypeItems) {
                        if (0 == thisPartTypeActions.count(action -> action.getArgs()[takePartArgIndex].equals(item.getFullName()))) {
                            opActions.add(new OpAction(TAKE_PART, new String[]{item.getFullName()}, item.x, item.y, partTypeName, inKitTrayByName(item.getFullName())));
                        }
                    }
                }
            }
            Set<String> typeSet = items
                    .stream()
                    .map(PhysicalItem::getType)
                    .collect(Collectors.toSet());
            if (debug) {
                logDebug("typeSet = " + typeSet);
            }
            MutableMultimap<String, PhysicalItem> availSlotsMap
                    = Lists.mutable.ofAll(items)
                    .select(item -> item.getType().equals("ES")
                            && item.getName().startsWith("empty_slot_")
                            && !item.getName().contains("_in_kit_"))
                    .groupBy(item -> posNameToType(item.getName()));

            MutableMultimap<String, Action> placePartMap
                    = Lists.mutable.ofAll(actions.subList(endl[0], endl[1]))
                    .select(action -> action.getType().equals(PLACE_PART) && !inKitTrayByName(action.getArgs()[placePartSlotArgIndex]))
                    .groupBy(action -> posNameToType(action.getArgs()[placePartSlotArgIndex]));

            for (String partTypeName : placePartMap.keySet()) {
                MutableCollection<PhysicalItem> thisPartTypeSlots
                        = availSlotsMap.get(partTypeName);
                MutableCollection<Action> thisPartTypeActions
                        = placePartMap.get(partTypeName);
                if (thisPartTypeSlots.size() > thisPartTypeActions.size() && thisPartTypeActions.size() > 0) {
                    for (PhysicalItem item : thisPartTypeSlots) {
                        if (0 == thisPartTypeActions.count(action -> action.getArgs()[takePartArgIndex].equals(item.getFullName()))) {
                            opActions.add(new OpAction(PLACE_PART, new String[]{item.getFullName()}, item.x, item.y, partTypeName, inKitTrayByName(item.getFullName())));
                        }
                    }
                }
            }
        }
        if (opActions.size() < 3) {
            logDebug("optimizePddlActionsWithOptaPlanner: small size of opActions list : opActions.size()=" + opActions.size() + ", actions=" + actions);
            return actions;
        }
        int skippedActionsCount = skippedActions.get();
        if (skippedActionsCount > 0) {
            logDebug("skippedActions = " + skippedActionsCount);
            logDebug("skippedPddlActionsList.size() = " + skippedPddlActionsList.size());
            logDebug("skippedPddlActionsList = " + skippedPddlActionsList);
            logDebug("skippedOpActionsList = " + skippedOpActionsList);
            logDebug("items = " + items);
            logDebug("itemNames = " + itemNames);
            if (debug || skippedActionsCount % 2 == 1) {
                System.err.println("actions.size() = " + actions.size() + ", skippedActions=" + skippedActionsCount);
                int recheckEndl[] = new int[2];
                List<OpAction> recheckSkippedOpActionsList = new ArrayList<>();
                List<Action> recheckSkippedPddlActionsList = new ArrayList<>();
                List<OpAction> recheckOpActions = pddlActionsToOpActions(actions, startingIndex, recheckEndl, recheckSkippedOpActionsList, recheckSkippedPddlActionsList);
                logDebug("recheckOpActions = " + recheckOpActions);
                logDebug("recheckEndl = " + Arrays.toString(recheckEndl));
                logDebug("recheckSkippedPddlActionsList = " + recheckSkippedPddlActionsList);
                logDebug("recheckSkippedOpActionsList = " + recheckSkippedOpActionsList);
            }
        }
        if (skippedActionsCount != skippedPddlActionsList.size()) {
            System.err.println("skippedPddlActionsList = " + skippedPddlActionsList);
            System.err.println("actions = " + actions);
            throw new IllegalStateException("skippedPddlActionsList.size() = " + skippedPddlActionsList.size() + ",actions.size() = " + actions.size() + ", skippedActions=" + skippedActionsCount);
        }
//        List<OpAction> opActionsCopy = new ArrayList<>(opActions);
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
        int solveCount = solverRunCount.incrementAndGet();
        try {
            OpActionPlan solvedPlan;
            inputPlan.checkActionList();
            synchronized (solverToRun) {
                solvedPlan = solverToRun.solve(inputPlan);
            }
            HardSoftLongScore hardSoftLongScore = solvedPlan.getScore();
            assert (null != hardSoftLongScore) : "solvedPlan.getScore() returned null";
            double solveScore = (hardSoftLongScore.getSoftScore() / 1000.0);
//        logDebug("Score improved:" + (solveScore - inScore));
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
                List<Action> preStartPddlActions = new ArrayList<>(actions.subList(0, startingIndex > endl[0] ? startingIndex : endl[0]));
                List<Action> fullReplanPddlActions = new ArrayList<>(preStartPddlActions);
                List<Action> newPddlActions = opActionsToPddlActions(solvedPlan, 0);
                List<Action> replacedPddlActions = new ArrayList<>(actions.subList(endl[0], endl[1]));

                fullReplanPddlActions.addAll(newPddlActions);
                fullReplanPddlActions.addAll(skippedPddlActionsList);
                List<Action> laterPddlActions = new ArrayList<>(actions.subList(endl[1], actions.size()));
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
        } catch (Exception ex) {
//            System.err.println("opActionsCopy = " + opActionsCopy);
            System.err.println("actions = " + actions);
            System.err.println("startingIndex = " + startingIndex);
            System.err.println("items = " + items);
            System.out.println("solverToRun = " + solverToRun);
            System.out.println("solveCount = " + solveCount);
            System.out.println("solverRunCount.get() = " + solverRunCount.get());
            logger.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
        return actions;
    }

    @Nullable
    private volatile GenerateParams lastClearKitsToCheckGenerateParams = null;
    private volatile int lastClearKitsToCheckGenerateParamsIndex = -1;

    @SuppressWarnings("unused")
    private void clearKitsToCheck(Action action, List<MiddleCommandType> cmds, GenerateParams gparams)
            throws InterruptedException, IOException, ExecutionException {

        if (!kitsToCheck.isEmpty()) {
            if (!recheckKitsOnly()) {
                System.err.println("lastClearKitsToCheckGenerateParams = " + lastClearKitsToCheckGenerateParams);
                System.err.println("lastClearKitsToCheckGenerateParamsIndex = " + lastClearKitsToCheckGenerateParamsIndex);
                System.err.println("lastAddKitToCheckGenerateParams = " + lastAddKitToCheckGenerateParams);
                System.err.println("lastAddKitToCheckGenerateIndex = " + lastAddKitToCheckGenerateIndex);
                System.err.println("kitsToCheck = " + kitsToCheck);
                System.err.println("gparams = " + gparams);
                System.out.println("getLastIndex() = " + getLastIndex());
                lastClearKitsToCheckGenerateParams = gparams;
                lastClearKitsToCheckGenerateParamsIndex = getLastIndex();
                throw new IllegalStateException("clearing kits to check that do not recheck");
            }
            lastClearKitsToCheckGenerateParams = gparams;
            lastClearKitsToCheckGenerateParamsIndex = getLastIndex();
            kitsToCheck.clear();
        }
    }

    private void checkedPause() {
        if (null != aprsSystem) {
            aprsSystem.pause();
        }
    }

    private void pause(Action action, List<MiddleCommandType> cmds) {
        addMarkerCommand(cmds, "pause", x -> checkedPause());
    }

    private void completeClearWayToHolder() {
        if (null != aprsSystem) {
            aprsSystem.resume();
        }
    }

    private void clearWayToHolder(String holder) {
        if (null != aprsSystem) {
            aprsSystem.pause();
            aprsSystem.clearWayToHolders(holder)
                    .thenRun(() -> completeClearWayToHolder());
        }
    }

    private void clearWayToHolder(List<MiddleCommandType> cmds, String holder) {
        addMarkerCommand(cmds, "clearWayToHolder" + holder, x -> clearWayToHolder(holder));
    }

    @Nullable
    private volatile GenerateParams lastAddKitToCheckGenerateParams = null;
    private volatile int lastAddKitToCheckGenerateIndex = -1;

    @SuppressWarnings("unused")
    private void addKitToCheck(Action action, List<MiddleCommandType> cmds, GenerateParams gparams) {

        lastAddKitToCheckGenerateParams = gparams;
        lastAddKitToCheckGenerateIndex = getLastIndex();
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
            logger.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final ConcurrentMap<String, List<String>> skuNameToInstanceNamesMap = new ConcurrentHashMap<>();

    private List<String> getKitInstanceNames(String kitName) {
        return skuNameToInstanceNamesMap.computeIfAbsent(kitName, this::getPartTrayInstancesFromSkuName);
    }

    private static <T> List<T> listFilter(List<T> listIn, Predicate<T> predicate) {
        return listIn
                .stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private List<Slot> getAbsSlotListForKitInstance(String kitSkuName, String kitInstanceName) {
        try {
            PoseType pose = getPose(kitInstanceName);
            if (debug) {
                try {
                    if (null != pose) {
                        logDebug("getAbsSlotListForKitInstance(" + kitSkuName + "," + kitInstanceName + ") : pose = " + CRCLPosemath.poseToXyzRpyString(pose));
                    } else {
                        logDebug("getAbsSlotListForKitInstance(" + kitSkuName + "," + kitInstanceName + ") : pose = null");

                    }
                } catch (Exception ex) {
                    Logger.getLogger(CrclGenerator.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            if (null == pose) {
                return Collections.emptyList();
            }
            Tray tray = new Tray(kitSkuName, pose, 0);
            tray.setType("KT");
            if (null != aprsSystem) {
                return aprsSystem.getSlots(tray, false)
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
            logger.log(Level.SEVERE, "", ex);
        }
        return Collections.emptyList();
    }

    public boolean recheckKitsOnly() throws InterruptedException, ExecutionException, IOException {
        List<PhysicalItem> newItems = checkKitsNewItems("recheckKitsOnly");
        List<PhysicalItem> parts = checkKitsItemsToParts(newItems);
        Map<String, List<Slot>> kitInstanceAbsSlotMap = new HashMap<>();

        Set<String> matchedKitInstanceNames = new HashSet<>();
        List<KitToCheck> kitsToFix = scanKitsToCheck(false, kitInstanceAbsSlotMap, matchedKitInstanceNames, parts);
        boolean empty = kitsToFix.isEmpty();
        if (!empty) {
            logError("recheckKitsOnly: kitsToFix = " + kitsToFix);
            logError("recheckKitsOnly: matchedKitInstanceNames = " + matchedKitInstanceNames);
            logError("recheckKitsOnly: kitInstanceAbsSlotMap = " + kitInstanceAbsSlotMap);
            for (KitToCheck kit : kitsToFix) {
                Map<String, KitToCheckInstanceInfo> kitInstanceInfoMap = kit.instanceInfoMap;
                for (Entry<String, KitToCheckInstanceInfo> entry : kitInstanceInfoMap.entrySet()) {
                    KitToCheckInstanceInfo info = entry.getValue();
                    for (KitToCheckFailedItemInfo failedItemInfo : info.failedItems) {
                        logError("recheckKitsOnly: failedItemInfo = " + failedItemInfo);
                        logError("recheckKitsOnly: failedItemInfo.failedAbsSlotPrpName = " + failedItemInfo.failedAbsSlotPrpName);
                        logError("recheckKitsOnly: failedItemInfo.failedItemSkuName = " + failedItemInfo.failedItemSkuName);
                    }
                    logError("recheckKitsOnly: info.failedSlots = " + info.failedSlots);
                }
            }
            kitsToFix = scanKitsToCheck(false, kitInstanceAbsSlotMap, matchedKitInstanceNames, parts);
        }
        return empty;
    }

    @SuppressWarnings("unused")
    private void checkKits(Action action, List<MiddleCommandType> cmds)
            throws IllegalStateException, SQLException, InterruptedException, ExecutionException, CRCLException, PmException {
        checkSettings();
        List<PhysicalItem> newItems = checkKitsNewItems("checkKits");
        assert (newItems != null) : "newItems == null : @AssumeAssertion(nullness)";
        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        synchronized (poseCache) {
            for (PhysicalItem item : newItems) {
                String fullName = item.getFullName();
                if (null != fullName) {
                    poseCache.put(fullName, item.getPose());
                }
            }
        }
        takeSnapshots("plan", "checkKits-", null, "");

        List<PhysicalItem> parts = checkKitsItemsToParts(newItems);
        synchronized (this) {
            List<String> newItemsFullNames = newItems
                    .stream()
                    .map(PhysicalItem::getFullName)
                    .collect(Collectors.toList());

            List<String> partsFullNames
                    = parts
                    .stream()
                    .map(PhysicalItem::getFullName)
                    .collect(Collectors.toList());
            List<String> partsInPartsTrayFullNames
                    = listFilter(partsFullNames, name2 -> !name2.contains("_in_kt_"));

            logDebug("checkKits: parts = " + parts);

            try {
                Map<String, List<Slot>> kitInstanceAbsSlotMap = new HashMap<>();

                Set<String> matchedKitInstanceNames = new HashSet<>();
                List<KitToCheck> kitsToFix = scanKitsToCheck(true, kitInstanceAbsSlotMap, matchedKitInstanceNames, parts);

                if (optoThread == null) {
                    optoThread = Thread.currentThread();
                }
                if (Thread.currentThread() != optoThread) {
                    throw new IllegalStateException("!Thread.currentThread() != optoThread: optoThread=" + optoThread + ", Thread.currentThread() =" + Thread.currentThread());
                }
                if (!kitsToFix.isEmpty()) {
                    logDebug("kitsToFix = " + kitsToFix);
                    printLastOptoInfo();
                    if (pauseInsteadOfRecover && !aprsSystem.isCorrectionMode()) {
                        StringBuilder errMsgSb = new StringBuilder();
                        takeSimViewSnapshot("checkKitsFailed", newItems);
                        String errMsgStart = aprsSystem.getRunName();
                        errMsgSb.append(errMsgStart);
                        logError("checkKits: errMsgStart=" + errMsgStart);
                        logError("checkKits: newItems = " + newItems);
                        logError("checkKits: kitInstanceAbsSlotMap = " + kitInstanceAbsSlotMap);
                        logError("checkKits: parts = " + parts);
                        logError("checkKits: matchedKitInstanceNames = " + matchedKitInstanceNames);
                        logError("checkKits: kitsToFix.size() = " + kitsToFix.size());
                        logError("checkKits: kitsToFix = " + kitsToFix);
                        for (KitToCheck kit : kitsToFix) {
                            logError("checkKits: kit = " + kit);
                            logError("checkKits: kit.slotMap = " + kit.slotMap);
                            String errMsgKitStart = " : " + kit.name + " needs ";
                            errMsgSb.append(errMsgKitStart);
                            logError("checkKits: errMsgKitStart=" + errMsgKitStart);
                            for (KitToCheckInstanceInfo info : kit.instanceInfoMap.values()) {

                                logError("checkKits: info = " + info);
                                for (KitToCheckFailedItemInfo failedItemInfo : info.failedItems) {
                                    String fiString = " " + kit.slotMap.get(failedItemInfo.failedAbsSlotPrpName) + " instead of " + failedItemInfo.failedItemSkuName + " ";
                                    errMsgSb.append(fiString);
                                    logError("checkKits: failedItemInfo. = " + failedItemInfo);
                                    logError("checkKits: failedItemInfo.failedClosestItem = " + failedItemInfo.failedClosestItem);
                                    logError("checkKits: failedItemInfo.failedClosestItemDist = " + failedItemInfo.failedClosestItemDist);
                                    logError("checkKits: failedItemInfo.failedAbsSlot = " + failedItemInfo.failedAbsSlot);
                                }

//                                JOptionPane.showMessageDialog(this.aprsSystemInterface,errmsg); 
                            }
                        }
                        CRCLProgramType program = new CRCLProgramType();
                        program.setInitCanon(new InitCanonType());
                        program.setEndCanon(new EndCanonType());
                        program.getMiddleCommand().addAll(cmds);
                        aprsSystem.logCrclProgFile(program);
                        String errMsg = errMsgSb.toString();
                        aprsSystem.setTitleErrorString(errMsg);
                        throw new IllegalStateException(errMsg);
//                        checkedPause();
                    } else {
                        Map<String, Integer> prefixCountMap = new HashMap<>();
                        Map<String, List<String>> itemsNameMap = new HashMap<>();
                        Map<String, List<String>> removedItemsNameMap = new HashMap<>();
                        kitsToFix.sort(Comparators.byIntFunction(KitToCheck::getMaxDiffFailedSlots));
                        List<Action> correctiveActions = new ArrayList<>();
                        List<String> fixLogList = new ArrayList<>();
                        for (KitToCheck kit : kitsToFix) {
                            List<KitToCheckInstanceInfo> infoList = new ArrayList<>(kit.instanceInfoMap.values());
                            infoList.sort(Comparators.byIntFunction(KitToCheckInstanceInfo::getFailedSlots));
                            for (KitToCheckInstanceInfo info : infoList) {
                                String kitInstanceName = info.instanceName;
                                if (matchedKitInstanceNames.contains(kitInstanceName)) {
                                    continue;
                                }
                                List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                                        (String n) -> getAbsSlotListForKitInstance(kit.name, n));

                                if (snapshotsEnabled()) {
                                    takeSimViewSnapshot(createImageTempFile("absSlots_" + kitInstanceName), absSlots);
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
                                    fixLogList.add("kitInstanceName=" + kitInstanceName + ",absSlotPrpName=" + absSlotPrpName + ",itemsNameMap=" + itemsNameMap + "\r\n");
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
                                            correctiveActions.add(new Action(TAKE_PART, closestItem.getFullName()));
                                            correctiveActions.add(new Action(PLACE_PART, slotPrefix + "_" + count));
                                        }
                                        if (!slotItemSkuName.equals("empty")) {
                                            String shortSkuName = slotItemSkuName;
                                            if (shortSkuName.startsWith("sku_")) {
                                                shortSkuName = shortSkuName.substring(4);
                                            }
                                            final String finalShortSkuName = shortSkuName;
                                            List<String> partNames
                                                    = itemsNameMap.computeIfAbsent(finalShortSkuName,
                                                            k -> listFilter(
                                                                    partsInPartsTrayFullNames,
                                                                    name2 -> name2.contains(k)));
                                            logDebug("checkKits: partNames = " + partNames);
                                            if (partNames.isEmpty()) {
                                                logError("No partnames for finalShortSkuName=" + finalShortSkuName);
                                                logError("fixLogList=" + fixLogList);
                                                logError("kit.slotMap = " + kit.slotMap);
                                                logError("newItems = " + newItems);
                                                logError("newItemsFullNames = " + newItemsFullNames);
                                                logError("itemsNameMap = " + itemsNameMap);
                                                logError("removedItemsMap=" + removedItemsNameMap);
                                                logError("slotItemSkuName = " + slotItemSkuName);
                                                logError("itemSkuName = " + itemSkuName);
                                                logError("partsInPartsTrayFullNames = " + partsInPartsTrayFullNames);
                                                List<String> recalcPartNames
                                                        = listFilter(
                                                                partsInPartsTrayFullNames,
                                                                name2 -> name2.contains(finalShortSkuName));
                                                logError("recalcPartNames = " + recalcPartNames);
                                                throw new IllegalStateException("No partnames for finalShortSkuName=" + finalShortSkuName
                                                        + ", absSlotPrpName=" + absSlotPrpName
                                                        + ", slotItemSkuName=" + slotItemSkuName
                                                        + ", itemSkuName=" + itemSkuName);
                                            }
//                                        String partNamePrefix = shortSkuName + "_in_pt";
//                                        int count = prefixCountMap.compute(partNamePrefix,
//                                                (String prefix, Integer c) -> (c == null) ? 1 : (c + 1));
                                            String partName = partNames.remove(0);
                                            removedItemsNameMap.compute(finalShortSkuName, (k, v) -> {
                                                if (v == null) {
                                                    v = new ArrayList<>();
                                                }
                                                v.add(partName);
                                                return v;
                                            });
                                            correctiveActions.add(new Action(TAKE_PART, partName));
                                            String slotName = absSlot.getFullName();
                                            logDebug("checkKits: slotName = " + slotName);
                                            PoseType absSlotPose = absSlot.getPose();
                                            PoseType slotPose = visionToRobotPose(absSlotPose);
                                            double min_dist = Double.POSITIVE_INFINITY;
                                            String closestKey = null;
                                            for (Entry<String, PoseType> entry : poseCache.entrySet()) {
                                                double dist = CRCLPosemath.diffPosesTran(absSlotPose, entry.getValue());
                                                if (dist < min_dist) {
                                                    closestKey = entry.getKey();
                                                    min_dist = dist;
                                                }
                                            }
                                            if (closestKey == null || min_dist > 15.0) {
                                                logError("checkKits: slotName = " + slotName);
                                                logError("checkKits: absSlot.getPose() = " + CRCLPosemath.poseToString(absSlotPose));
                                                logError("checkKits: slotPose = " + CRCLPosemath.poseToString(slotPose));
                                                logError("checkKits: postitionMaps = " + getPositionMaps());
                                                logError("closestKey = " + closestKey);
                                                logError("min_dist = " + min_dist);
                                                for (Entry<String, PoseType> entry : poseCache.entrySet()) {
                                                    PoseType entryValue = entry.getValue();
                                                    double dist = CRCLPosemath.diffPosesTran(slotPose, entryValue);
                                                    double absSlotDiff = CRCLPosemath.diffPosesTran(absSlotPose, entryValue);
                                                    PointType entryPoint = entryValue.getPoint();
                                                    if (null != entryPoint) {
                                                        logError("entry.getKey = " + entry.getKey() + ",x=" + entryPoint.getX() + ",y=" + entryPoint.getY() + ", dist=" + dist + ", absSlotPoseDiff=" + absSlotDiff);
                                                    }
                                                }
                                                throw new IllegalStateException("absSlotPose for " + slotName + " not in poseCache min_dist=" + min_dist + ", closestKet=" + closestKey + ", keys=" + poseCache.keySet());
                                            }
                                            correctiveActions.add(new Action(PLACE_PART, closestKey));

//                                        takePartByName(partName, null, cmds);
//                                        placePartByPose(cmds, visionToRobotPose(absSlot.getPose()));
                                        }
                                    }
                                }
                                matchedKitInstanceNames.add(kitInstanceName);
                                break;
                            }
                            logDebug("matchedKitInstanceNames = " + matchedKitInstanceNames);
                            logDebug("kitsToFix = " + kitsToFix);
                        }
                        List<Action> optimizedCorrectiveActions
                                = optimizePddlActionsWithOptaPlanner(correctiveActions, 0, newItems);
                        int startingGetLastIndex = getLastIndex();
                        CORRECT_ACTIONS_LOOP:
                        for (int caIndex = 0; caIndex < optimizedCorrectiveActions.size(); caIndex++) {
                            Action correctiveAction = optimizedCorrectiveActions.get(caIndex);
                            switch (correctiveAction.getType()) {
                                case TAKE_PART:
                                    String partName = correctiveAction.getArgs()[takePartArgIndex];
                                    if (partName.contains("in_pt")) {
                                        if (caIndex < optimizedCorrectiveActions.size() - 1) {
                                            Action nextAction = optimizedCorrectiveActions.get(caIndex + 1);
                                            String nextSlotName = nextAction.getArgs()[placePartSlotArgIndex];
                                            if (!nextSlotName.contains("kit")) {
                                                logDebug("nextSlotName = " + nextSlotName);
                                                caIndex++;
                                                continue CORRECT_ACTIONS_LOOP;
                                            }
                                        }
                                    }
                                    takePartByName(partName, null, cmds);
                                    break;

                                case PLACE_PART:
                                    String slotName = correctiveAction.getArgs()[placePartSlotArgIndex];
                                    placePartBySlotName(slotName, cmds, correctiveAction, action, startingGetLastIndex);  //ByName(slotName, null, cmds);
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private List<PhysicalItem> checkKitsItemsToParts(List<PhysicalItem> newItems) {
        List<PhysicalItem> parts = newItems.stream()
                .filter(x -> !x.getName().startsWith("empty_slot"))
                .filter(x -> !x.getName().contains("vessel"))
                .collect(Collectors.toList());
        return parts;
    }

    private List<PhysicalItem> checkKitsNewItems(String label) throws InterruptedException, ExecutionException {
        List<PhysicalItem> newItems;
        if (null == externalPoseProvider) {
            newItems = waitForCompleteVisionUpdates(label, lastRequiredPartsMap, 15_000);
        } else {
            newItems = externalPoseProvider.getNewPhysicalItems();
        }
        return newItems;
    }

    @Nullable
    private volatile List<KitToCheck> lastKitsToCheckCopy = null;

    private List<KitToCheck> scanKitsToCheck(boolean newCheck, Map<String, List<Slot>> kitInstanceAbsSlotMap, Set<String> matchedKitInstanceNames, List<PhysicalItem> parts) throws IOException {
        List<KitToCheck> kitsToFix = new ArrayList<>(kitsToCheck);

        if (kitsToFix.isEmpty()) {
            if (!newCheck && null != lastKitsToCheckCopy && !lastKitsToCheckCopy.isEmpty()) {
                kitsToFix = new ArrayList<>(lastKitsToCheckCopy);
            } else {
                throw new IllegalStateException("kitsToCheck is empty");
            }
        }
        List<KitToCheck> kitsToCheckCopy;
        if (newCheck) {
            kitsToCheckCopy = new ArrayList<>(kitsToFix);
            lastKitsToCheckCopy = kitsToCheckCopy;
        } else if (lastKitsToCheckCopy != null) {
            kitsToCheckCopy = lastKitsToCheckCopy;
        } else {
            throw new IllegalStateException("no lastKitsToCheckCopy to reuse");
        }
        for (KitToCheck kit : kitsToCheckCopy) {
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
                    takeSimViewSnapshot(createImageTempFile("absSlots_" + kitInstanceName), absSlots);
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
                    info.closestItemNameMap.put(absSlotPrpName, closestItem.getFullName());
                    String itemSkuName = "empty";
                    double closestItemDist = closestItem.distFromXY(absSlot);
                    if (closestItemDist < absSlot.getDiameter() / 2.0) {
                        itemSkuName = closestItem.origName;
                    }
                    info.itemSkuMap.put(absSlotPrpName, itemSkuName);
                    if (!Objects.equals(kit.slotMap.get(absSlotPrpName), itemSkuName)) {
                        KitToCheckFailedItemInfo failedItemInfo
                                = new KitToCheckFailedItemInfo(
                                        absSlotPrpName,
                                        itemSkuName,
                                        closestItem,
                                        absSlot,
                                        closestItemDist);
                        info.failedItems.add(failedItemInfo);
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
                logDebug("matchedKitInstanceNames = " + matchedKitInstanceNames);
                logDebug("kitsToFix = " + kitsToFix);
            }
        }
        return kitsToFix;
    }

//    private static List<String> partNamesListForShortSkuName(List<PhysicalItem> newItems, final String finalShortSkuName) {
//        return newItems.stream()
//                .filter(item -> item.getType().equals("P"))
//                .flatMap(item -> {
//                    String fullName = item.getFullName();
//                    if (null != fullName) {
//                        return Stream.of(fullName);
//                    }
//                    return Stream.empty();
//                })
//                .filter(name2 -> name2.contains(finalShortSkuName) && !name2.contains("_in_kt_"))
//                .sorted()
//                .collect(Collectors.toList());
//    }
    private void printLastOptoInfo() {
        try {
            if (null != lastSkippedPddlActionsList) {
                logDebug("lastSkippedPddlActionsList = " + lastSkippedPddlActionsList.size() + " : " + lastSkippedPddlActionsList);
            }
            if (null != lastOpActionsList) {
                logDebug("lastOpActionsList = " + lastOpActionsList.size() + " : " + lastOpActionsList);
            }
            if (null != lastSkippedOpActionsList) {
                logDebug("lastSkippedOpActionsList = " + lastSkippedOpActionsList.size() + " : " + lastSkippedOpActionsList);
            }
            if (null != lastOptimizePddlItems) {
                logDebug("lastOptimizePddlItems = " + lastOptimizePddlItems.size() + " : " + lastOptimizePddlItems);
            }
            if (null != lastOptimizePddlItemNames) {
                Collections.sort(lastOptimizePddlItemNames);
                logDebug("lastOptimizePddlItemNames = " + lastOptimizePddlItemNames.size() + " : " + lastOptimizePddlItemNames);
            }
            if (null != lastOptimizePreStartPddlActions) {
                logDebug("lastOptimizePreStartPddlActions = " + lastOptimizePreStartPddlActions.size() + " : " + lastOptimizePreStartPddlActions);
            }
            if (null != lastOptimizeReplacedPddlActions) {
                logDebug("lastOptimizeReplacedPddlActions = " + lastOptimizeReplacedPddlActions.size() + " : " + lastOptimizeReplacedPddlActions);
            }
            if (null != lastOptimizeNewPddlActions) {
                logDebug("lastOptimizeNewPddlActions = " + lastOptimizeNewPddlActions.size() + " : " + lastOptimizeNewPddlActions);
            }
            if (null != lastOptimizeLaterPddlActions) {
                logDebug("lastOptimizeLaterPddlActions = " + lastOptimizeLaterPddlActions.size() + " : " + lastOptimizeLaterPddlActions);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
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
    @SuppressWarnings({"unused"})
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
    @SuppressWarnings({"unused"})
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
    private PoseType visionToRobotPose(PoseType poseIn) {
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

    private final AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    final private ExecutorJPanel parentExecutorJPanel;

    /**
     * Get the value of parentExecutorJPanel
     *
     * @return the value of parentExecutorJPanel
     */
    @Nullable
    public ExecutorJPanel getParentExecutorJPanel() {
        return parentExecutorJPanel;
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param pose optional pose to mark or null
     * @param label optional label for pose or null
     */
    private void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(f, pose, label);
        }
    }

    public void takeSimViewSnapshot(File f, PointType point, String label) {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(f, point, label);
        }
    }

    public void takeSimViewSnapshot(File f, PmCartesian pt, String label) {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(f, pt, label);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String label) throws IOException {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(imgLabel, pose, label);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PointType point, String label) throws IOException {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(imgLabel, point, label);
        }
    }

    private void takeSimViewSnapshot(String imgLabel, @Nullable PmCartesian pt, @Nullable String label) throws IOException {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(imgLabel, pt, label);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint items to paint in the snapshot image
     */
    private void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(f, itemsToPaint);
        }
    }

    private void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint) throws IOException {
        if (null != aprsSystem) {
            aprsSystem.takeSimViewSnapshot(imgLabel, itemsToPaint);
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
     */
    private void takeDatabaseViewSnapshot(File f) {
        if (null != aprsSystem) {
            aprsSystem.startVisionToDbNewItemsImageSave(f);
        }
    }

    private void setTitleErrorString(String errString) {
        if (null != aprsSystem) {
            aprsSystem.setTitleErrorString(errString);
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
     */
    @SuppressWarnings("SameParameterValue")
    private void addTakeSnapshots(List<MiddleCommandType> out,
            String title, @Nullable PoseType pose, @Nullable String label, int crclNumber) {
        if (snapshotsEnabled()) {
            addMarkerCommand(out, title, x -> {
                final int curCrclNumber = CrclGenerator.this.crclNumber.get();
                if (crclNumber != curCrclNumber) {
                    setTitleErrorString("crclNumber mismatch " + crclNumber + "!=" + curCrclNumber);
                }
                takeSnapshots("exec", title, pose, label);
            });
        }
    }

    private boolean snapshotsEnabled() {
        return takeSnapshots && aprsSystem != null && aprsSystem.snapshotsEnabled();
    }

    @SuppressWarnings("SameParameterValue")
    private File createImageTempFile(String prefix) throws IOException {
        if (null != aprsSystem) {
            return aprsSystem.createTempFile(prefix, ".PNG", aprsSystem.getLogImageDir());
        }
        return Utils.createTempFile(prefix, ".PNG");
    }

    public void takeSnapshots(String prefix, String title, @Nullable PoseType pose, @Nullable String label) {
        if (snapshotsEnabled()) {
            try {
                String fullTitle = title + "_crclNumber-" + String.format("%03d", crclNumber.get()) + "_action-" + String.format("%03d", getLastIndex());
                takeSimViewSnapshot(createImageTempFile(prefix + "_" + fullTitle), pose, label);
                if (null == externalPoseProvider) {
                    takeDatabaseViewSnapshot(createImageTempFile(prefix + "_db_" + fullTitle));
                }
                takeSimViewSnapshot(createImageTempFile(prefix + "_pc_" + fullTitle), poseCacheToDetectedItemList());
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "", ex);
            }
        }
    }

    /**
     * Get a run prefix useful for naming/identifying snapshot files.
     *
     * @return run prefix
     */
    public String getRunPrefix() {
        return getRunName() + Utils.getDateTimeString() + "_" + String.format("%03d", crclNumber.get()) + "action-" + String.format("%03d", lastIndex.get());
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
    private void testPartPosition(Action action, List<MiddleCommandType> out) throws SQLException, CRCLException, PmException {
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

    private void setCorrectKitImage() {
        if (null != kitInspectionJInternalFrame) {
            String kitinspectionImageKitPath = kitInspectionJInternalFrame.getKitinspectionImageKitPath();
            String kitImage = kitInspectionJInternalFrame.getKitImage();
            String kitStatusImage = kitinspectionImageKitPath + "/" + kitImage + ".png";
            logDebug("kitStatusImage " + kitStatusImage);
            Utils.runOnDispatchThread(() -> setKitStatusIcon(kitStatusImage));
        }
    }

    @UIEffect
    private void setKitStatusIcon(String kitStatusImage) {
        if (null != kitInspectionJInternalFrame) {
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
                        logger.log(Level.SEVERE, "", ex);
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
                    logger.log(Level.SEVERE, "", ex);
                }
            });
        }
    }

//    /**
//     * Inspects a finished kit to check if it is complete
//     *
//     * @param action PDDL Action
//     * @param out list of commands to append to
//     * @throws IllegalStateException if database is not connected
//     * @throws SQLException if query fails
//     * @throws java.lang.InterruptedException interrupted with
//     * Thread.interrupt()
//     * @throws java.util.concurrent.ExecutionException wrapped exception in
//     * another thread servicing waitForCompleteVisionUpdates
//     */
//    private void inspectKit(Action action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, InterruptedException, ExecutionException, CRCLException, PmException {
//        checkDbReady();
//        checkSettings();
//        if (action.getArgs().length < 2) {
//            throw new IllegalArgumentException("action = " + action + " needs at least two arguments: kitSku inspectionID");
//        }
//        if (PlacePartSlotPoseList.isEmpty()) {
//            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;No place part slots added. </h3><br>");
//            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Inspection Aborted</h3><br>");
//            takeSnapshots("plan", "PlacePartSlotPoseList.isEmpty()-inspect-kit-", null, "");
//            return;
//        }
//        if (null == externalPoseProvider) {
//            waitForCompleteVisionUpdates("inspectKit", lastRequiredPartsMap, 15_000);
//        }
//        takeSnapshots("plan", "inspect-kit-", null, "");
//
////        addTakeSnapshots(out, "inspect-kit-", null, "");
//        String kitSku = action.getArgs()[0];
//        String inspectionID = action.getArgs()[1];
//        MessageType msg = new MessageType();
//        msg.setMessage("inspect-kit " + kitSku + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
//        setCommandId(msg);
//        out.add(msg);
//
//        //-- inspect-kit takes an sku as argument
//        //-- We want to identify which was just built
//        //-- To do so, we use the poses stored in PlacePartSlotPoseList and
//        //-- we look for the kit tray in the database for which one of the slots
//        //-- has at least one pose in the list
//        if (null == correctPartsTray) {
//            correctPartsTray = findCorrectKitTray(kitSku);
//        }
//        PartsTray tray = this.correctPartsTray;
//        if (null != tray) {
//            try {
//                PoseType trayPose = tray.getPartsTrayPose();
//                if (null != trayPose) {
//                    PointType trayPosePoint = trayPose.getPoint();
//                    if (null != trayPosePoint) {
//                        takeSimViewSnapshot("inspectKit.correctPartsTray.partsTrayPose",
//                                new PmCartesian(
//                                        trayPosePoint.getX(),
//                                        trayPosePoint.getY(), 0),
//                                tray.getPartsTrayName());
//                    }
//                }
//                takeSimViewSnapshot("inspectKit.correctPartsTray.slotList",
//                        tray.getSlotList());
//            } catch (IOException ex) {
//                logger.log(Level.SEVERE, "", ex);
//            }
//            EmptySlotSet = new HashSet<>();
//            int numberOfPartsInKit = 0;
//
//            logDebug("\n\n---Inspecting kit tray " + tray.getPartsTrayName());
//            int partDesignPartCount = getPartDesignPartCount(kitSku);
//
//            //-- replace all _pt from TakenPartList with _kt
//            //-- Get all part that contains "in_kt" in their names
//            //-- from the database
//            for (int i = 0; i < TakenPartList.size(); i++) {
//                String part_in_pt = TakenPartList.get(i);
//                String tmpPartName = part_in_pt.replace("in_pt", "in_kt");
//                int indexLastUnderscore = tmpPartName.lastIndexOf("_");
//                assert (indexLastUnderscore >= 0) :
//                        ("TakenPartList=" + TakenPartList + " contains invalid tmpPartName=" + tmpPartName + " from part_in_pt=" + part_in_pt);
//
//                String part_in_kt = tmpPartName.substring(0, indexLastUnderscore);
//                assert (part_in_kt.indexOf('_') > 0) :
//                        ("part_in_kt=" + part_in_kt + ",tmpPartName=" + tmpPartName + " from part_in_pt=" + part_in_pt + ", indexLastUnderscore=" + indexLastUnderscore);
//
//                TakenPartList.set(i, part_in_kt);
//            }
//
//            //-- Get all the slots for the current parts tray
//            List<Slot> slotList = tray.getSlotList();
//            for (Slot slot : slotList) {
//                PoseType slotPose = slot.getSlotPose();
//                PointType slotPosePoint = slotPose.getPoint();
//                if (null == slotPosePoint) {
//                    throw new IllegalStateException("pose for slot=" + slot + " has null point property");
//                }
//                double slotx = slotPosePoint.getX();
//                double sloty = slotPosePoint.getY();
//                //logDebug(slot.getSlotName() + ":(" + x_offset + "," + y_offset + ")");
//                logDebug("++++++ " + slot.getSlotName() + ":(" + slotx + "," + sloty + ")");
//
//                //-- we want to filter out from TakenPartList parts that
//                //-- do not match slot part sku
//                //-- e.g., In TakenPartList keep only parts that contain part_large_gear
//                //-- if part sku for this slot is sku_part_large_gear
//                String partSKU = slot.getPartSKU();
//                if (null == partSKU) {
//                    logger.log(Level.WARNING, "slot has null partSKU : slot={0}", slot);
//                    continue;
//                }
//                if (partSKU.startsWith("sku_")) {
//                    partSKU = partSKU.substring(4).concat("_in_kt");
//                }
//                if (checkPartTypeInSlot(partSKU, slot) == 1) {
//                    numberOfPartsInKit++;
//                } else {
//                    EmptySlotSet.add(slot);
//                }
//            }
//            if (null != kitInspectionJInternalFrame) {
//                if (!EmptySlotSet.isEmpty()) {
//                    kitInspectionJInternalFrame.setKitImage(getKitResultImage(EmptySlotSet));
//                } else {
//                    kitInspectionJInternalFrame.setKitImage("complete");
//                }
//            }
//            setCorrectKitImage();
//            if (numberOfPartsInKit == partDesignPartCount) {
//                addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #7ef904\">&nbsp;&nbsp;The kit is complete</h3><br>");
//            } else {
//                try {
//                    if (snapshotsEnabled()) {
//                        takeSimViewSnapshot(createImageTempFile("inspectKit-slotList"), slotList);
//                        if (null != EmptySlotSet) {
//                            takeSimViewSnapshot(createImageTempFile("inspectKit-EmptySlotSet"), EmptySlotSet);
//                        }
//                    }
//                } catch (IOException ex) {
//                    logger.log(Level.SEVERE, "", ex);
//                }
//                //if (part_in_kt_found) {
//                TakenPartList.clear();
//                int nbofmissingparts = partDesignPartCount - numberOfPartsInKit;
//                addToInspectionResultJTextPane("<h3 style=\"background-color: #ffb5b5; color: #ffffff\">&nbsp;&nbsp;The kit is missing " + nbofmissingparts + " part(s)</h3>");
//                if (null != EmptySlotSet) {
//                    for (Slot s : EmptySlotSet) {
//                        addToInspectionResultJTextPane("&nbsp;&nbsp;Slot " + s.getSlotName() + " is missing a part of type " + s.getPartSKU() + "<br>");
//                    }
//                }
//                if (nbofmissingparts == 1) {
//                    kitInspectionJInternalFrameKitTitleLabelSetText("Missing " + nbofmissingparts + " part. Getting the new part.");
//                } else {
//                    kitInspectionJInternalFrameKitTitleLabelSetText("Missing " + nbofmissingparts + " parts. Getting the new parts.");
//                }
//                addToInspectionResultJTextPane("<h2 style=\"BACKGROUND-COLOR: " + messageColorH3 + "\">&nbsp;&nbsp;Recovering from failures</h2>");
//                Map<String, List<String>> partSkuMap = new HashMap<>();
//
//                //-- Build a map where the key is the part sku for a slot
//                //-- and the value is an arraylist of part_in_pt
//                for (Slot s : EmptySlotSet) {
//
//                    List<String> allPartsInPt = new ArrayList<>();
//                    String partSKU = s.getPartSKU();
//                    if (null == partSKU) {
//                        logger.log(Level.WARNING, "slot has null partSKU : slot={0}", s);
//                        continue;
//                    }
//                    if (partSKU.startsWith("sku_")) {
//                        partSKU = partSKU.substring(4).concat("_in_pt");
//                    }
//                    //-- Querying the database 20 times for part_in_pt
//                    for (int i = 0; i < 20; i++) {
//                        if (allPartsInPt.isEmpty()) {
//                            allPartsInPt = getAllPartsInPt(partSKU);
//                        }
//                    }
//                    partSkuMap.put(partSKU, allPartsInPt);
//                }
//
//                if (partSkuMap.size() > 0) {
//                    for (Slot s : EmptySlotSet) {
//                        String partSKU = s.getPartSKU();
//                        if (null == partSKU) {
//                            logger.log(Level.WARNING, "slot has null partSKU : slot={0}", s);
//                            continue;
//                        }
//                        if (partSKU.startsWith("sku_")) {
//                            partSKU = partSKU.substring(4).concat("_in_pt");
//                        }
//                        addToInspectionResultJTextPane("&nbsp;&nbsp;Getting a list of part_in_pt for sku " + partSKU + "<br>");
//
//                        //-- get list of part_in_pt based on the part sku
//                        List<String> listOfParts = partSkuMap.get(partSKU);
//                        //-- get the first element in this list and then remove it from the list
//                        if (null != listOfParts && listOfParts.size() > 0) {
//                            String partInPt = listOfParts.get(0);
//                            //--remove the first element
//                            listOfParts.remove(0);
//                            //-- update the list in the map with the modified list
//                            partSkuMap.put(partSKU, listOfParts);
//                            //-- perform pick-and-place actions
//                            takePartRecovery(partInPt, out);
//                            Action takepartrecoveryaction = Action.parse("(place-part " + s.getSlotName() + ")");
//                            placePartRecovery(takepartrecoveryaction, s, out);
//                        } else {
//                            // addToInspectionResultJTextPane("Could not find part_in_pt for sku " + partSKU + " from the database<b
//                            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Could not find part_in_pt for sku " + partSKU + " from the database</h3><br>");
//                            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Recovery Aborted</h3><br>");
//                        }
//                    }
//                } else {
//                    addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Could not find parts in_pt from the database</h3><br>");
//                    addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Recovery Aborted</h3><br>");
//                }
//                addToInspectionResultJTextPane("<br>");
//            }
//        } else {
//            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;The system could not identify the kit tray that was built. (kitSku=" + kitSku + ") </h3><br>");
//            addToInspectionResultJTextPane("<h3 style=\"BACKGROUND-COLOR: #ff0000\">&nbsp;&nbsp;Inspection Aborted</h3><br>");
//            System.err.println("Trying to get correctPartsTray again ...");
//            correctPartsTray = findCorrectKitTray(kitSku);
//            logDebug("msg = " + msg);
//        }
//        if (inspectionID.contains("0")) {
//            if (null != PlacePartSlotPoseList) {
//                PlacePartSlotPoseList.clear();
//            }
//            correctPartsTray = null;
//        }
//    }
    private double getVisionToDBRotationOffset() {
        assert (null != this.aprsSystem) : "null == this.aprsSystemInterface: @AssumeAssertion(nullness)";
        return this.aprsSystem.getVisionToDBRotationOffset();
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

        assert (null != this.aprsSystem) : "null == this.aprsSystemInterface: @AssumeAssertion(nullness)";
        assert (null != this.qs) : "null == this.qs: @AssumeAssertion(nullness)";
        List<PartsTray> dpuPartsTrayList = aprsSystem.getPartsTrayList();

        //-- retrieveing from the database all the parts trays that have the sku kitSku
        List<PartsTray> partsTraysList = getPartsTrays(kitSku);

        List<PhysicalItem> partsTrayListItems = new ArrayList<>();
        List<PhysicalItem> dpuPartsTrayListItems = new ArrayList<>();

        /*
        logDebug("-Checking parts trays");
        for (int i = 0; i < partsTraysList.size(); i++) {
            PartsTray partsTray = partsTraysList.get(i);
            logDebug("-Parts tray: " + partsTray.getPartsTrayName());
        }
         */
        for (PartsTray partsTray : partsTraysList) {

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
            PointType partsTrayPosePoint = partsTrayPose.getPoint();
            if (null == partsTrayPosePoint) {
                throw new IllegalStateException("pose for tray=" + trayName + " has null point property");
            }
            logDebug("-Checking parts tray [" + partsTray.getPartsTrayName() + "] :(" + partsTrayPosePoint.getX() + "," + partsTrayPosePoint.getY() + ")");
            partsTray.setpartsTrayPose(partsTrayPose);
            double partsTrayPoseX = partsTrayPosePoint.getX();
            double partsTrayPoseY = partsTrayPosePoint.getY();
            double partsTrayPoseZ = partsTrayPosePoint.getZ();

            double rotation = 0;
            //-- Read partsTrayList
            //-- Assign rotation to myPartsTray by comparing poses from vision vs database
            //System.out.print("-Assigning proper rotation: ");
            logDebug("-Comparing with other parts trays from vision");
            for (int c = 0; c < dpuPartsTrayList.size(); c++) {
                PartsTray pt = dpuPartsTrayList.get(c);

                PointType dpuPartsTrayPoint = point(pt.getX(), pt.getY(), 0);

//                dpuPartsTrayPoint = visionToRobotPoint(dpuPartsTrayPoint);
                double ptX = dpuPartsTrayPoint.getX();
                double ptY = dpuPartsTrayPoint.getY();
                logDebug("    Parts tray:(" + pt.getX() + "," + pt.getY() + ")");
                logDebug("    Rotation:(" + pt.getRotation() + ")");
                //-- Check if X for parts trays are close enough
                //double diffX = Math.abs(partsTrayPoseX - ptX);
                //logDebug("diffX= "+diffX);
                /*
                if (diffX < 1E-7) {
                    //-- Check if Y for parts trays are close enough
                    double diffY = Math.abs(partsTrayPoseY - ptY);
                    //logDebug("diffY= "+diffY);
                    if (diffY < 1E-7) {
                        rotation=pt.getRotation();
                        partsTray.setRotation(pt.getRotation());
                    }
                }
                 */

                double distance = Math.hypot(partsTrayPoseX - ptX, partsTrayPoseY - ptY);
                logDebug("    Distance = " + distance + "\n");
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
            //logDebug(rotation);
            //-- retrieve the rotationOffset
            double rotationOffset = getVisionToDBRotationOffset();

            logDebug("rotationOffset " + rotationOffset);
            logDebug("rotation " + partsTray.getRotation());
            //-- compute the angle
            double angle = normAngle(partsTray.getRotation() + rotationOffset);

            //-- Get list of slots for this parts tray
            logDebug("-Checking slots");
            List<Slot> slotList = partsTray.getSlotList();
            int count = 0;
            for (Slot slot : slotList) {
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

                logDebug("+++ " + slot.getSlotName() + ":(" + slotX + "," + slotY + ")");
                //-- compare this slot pose with the ones in PlacePartSlotPoseList
                for (PoseType pose : PlacePartSlotPoseList) {
                    PointType point = pose.getPoint();
                    if (null == point) {
                        throw new IllegalStateException("pose on PlacePartSlotPoseList has null point property: PlacePartSlotPoseList=" + PlacePartSlotPoseList);
                    }
                    logDebug("      placepartpose :(" + point.getX() + "," + point.getY() + ")");
                    double distance = Math.hypot(point.getX() - slotX, point.getY() - slotY);
                    logDebug("         Distance = " + distance + "\n");
                    if (distance < 5.0) {
                        count++;
                    }
                }
            }
            try {
                if (snapshotsEnabled()) {
                    takeSimViewSnapshot(createImageTempFile("PlacePartSlotPoseList"), posesToDetectedItemList(PlacePartSlotPoseList));
                    takeSimViewSnapshot(createImageTempFile("partsTray.getSlotList"), slotList);
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "", ex);
            }
            if (null != trayName) {
                partsTrayListItems.add(new Tray(trayName, partsTray.getRotation(), partsTrayPoseX, partsTrayPoseY));
            } else {
                logger.log(Level.WARNING, "partsTray has null partsTrayName : {0}", partsTray);
            }
            if (count > 0) {
                try {
                    if (snapshotsEnabled()) {
                        takeSimViewSnapshot(createImageTempFile("dpuPartsTrayList"), dpuPartsTrayListItems);
                        takeSimViewSnapshot(createImageTempFile("partsTrayList"), partsTrayListItems);
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "", ex);
                }
                correctPartsTray = partsTray;
                logDebug("Found partstray: " + partsTray.getPartsTrayName());
                return partsTray;
            }
        }
        try {
            if (snapshotsEnabled()) {
                takeSimViewSnapshot(createImageTempFile("dpuPartsTrayList"), dpuPartsTrayListItems);
                takeSimViewSnapshot(createImageTempFile("partsTrayList"), partsTrayListItems);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "", ex);
        }
        System.err.println("findCorrectKitTray(" + kitSku + ") returning null. partsTraysList=" + partsTraysList);
        return null;
    }

    private String getKitResultImage(Set<Slot> list) {
        StringBuilder kitResultImage = new StringBuilder();
        List<Integer> idList = new ArrayList<>();
        for (Slot slot : list) {
            int id = slot.getID();
            idList.add(id);
        }
        if (!idList.isEmpty()) {
            Collections.sort(idList);
        } else {
            logDebug("idList is empty");
        }
        for (Integer s : idList) {
            kitResultImage.append(s);
        }

        return kitResultImage.toString();
    }

    private double normAngle(double angleIn) {
        double angleOut = angleIn;
        if (angleOut > Math.PI) {
            angleOut -= 2 * Math.PI * ((int) (angleIn / Math.PI));
        } else if (angleOut < -Math.PI) {
            angleOut += 2 * Math.PI * ((int) (-1.0 * angleIn / Math.PI));
        }
        return angleOut;
    }

    private int checkPartTypeInSlot(String partInKt, Slot slot) throws SQLException {
        int nbOfOccupiedSlots = 0;
        List<String> allPartsInKt = new ArrayList<>();
        //-- queries the database 10 times to make sure we are not missing some part_in_kt
        for (int i = 0; i < 20; i++) {
            if (allPartsInKt.isEmpty()) {

                allPartsInKt = getAllPartsInKt(partInKt);
            }
        }
        if (!allPartsInKt.isEmpty()) {
            for (String newPartInKt : allPartsInKt) {
                System.out.print("-------- " + newPartInKt);
                if (checkPartInSlot(newPartInKt, slot)) {
                    logDebug("-------- Located in slot");
                    nbOfOccupiedSlots++;
                } else {
                    logDebug("-------- Not located in slot");
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
        PointType posePartPoint = posePart.getPoint();
        if (null == posePartPoint) {
            throw new IllegalStateException("getPose(" + partName + ") returned pose with null point property");
        }
//        posePart = visionToRobotPose(posePart);
        double partX = posePartPoint.getX();
        double partY = posePartPoint.getY();
        PointType slotPosePoint = slot.getSlotPose().getPoint();
        if (null == slotPosePoint) {
            throw new IllegalStateException("slot has pose with null point property : slot=" + slot);
        }
        double slotX = slotPosePoint.getX();
        double slotY = slotPosePoint.getY();
        logDebug(":(" + partX + "," + partY + ")");
        double distance = Math.hypot(partX - slotX, partY - slotY);
        System.out.print("-------- Distance = " + distance);
        // compare finalres with a specified tolerance value of 6.5 mm
        if (distance < kitInspectDistThreshold) {
            isPartInSlot = true;
            // logDebug("---- Part " + partName + " : (" + partX + "," + partY + ")");
            // logDebug("---- Slot " + slot.getSlotName() + " : (" + slotX + "," + slotY + ")");
            // logDebug("---- Distance between part and slot = " + dist);
        }
        return isPartInSlot;
    }

    private int toolChangeToolNameArgIndex = 1;

    /**
     * Get the value of toolChangeToolNameArgIndex
     *
     * @return the value of toolChangeToolNameArgIndex
     */
    public int getToolChangeToolNameArgIndex() {
        return toolChangeToolNameArgIndex;
    }

    /**
     * Set the value of toolChangeToolNameArgIndex
     *
     * @param toolChangeToolNameArgIndex new value of toolChangeToolNameArgIndex
     */
    public void setToolChangeToolNameArgIndex(int toolChangeToolNameArgIndex) {
        this.toolChangeToolNameArgIndex = toolChangeToolNameArgIndex;
    }

    private int toolHolderNameArgIndex = 0;

    /**
     * Get the value of toolHolderNameArgIndex
     *
     * @return the value of toolHolderNameArgIndex
     */
    public int getToolHolderNameArgIndex() {
        return toolHolderNameArgIndex;
    }

    /**
     * Set the value of toolHolderNameArgIndex
     *
     * @param toolHolderNameArgIndex new value of toolHolderNameArgIndex
     */
    public void setToolHolderNameArgIndex(int toolHolderNameArgIndex) {
        this.toolHolderNameArgIndex = toolHolderNameArgIndex;
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

    private boolean useJointMovesForToolHolderApproach = true;
    private boolean skipMissingParts = false;

    private boolean getForceFakeTakeFlag() {
        if (null != parentExecutorJPanel) {
            return this.parentExecutorJPanel.getForceFakeTakeFlag();
        }
        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private void setFakeTakePart(boolean _newValue) {
        if (null != parentExecutorJPanel) {
            this.parentExecutorJPanel.setForceFakeTakeFlag(_newValue);
        }
    }

    /**
     * Add commands to the list that will take a given part.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @param nextPlacePartAction action to be checked to see if part should be
     * skipped
     * @throws IllegalStateException if database state is not consistent, eg
     * part not in the correct slot
     * @throws SQLException if database query fails
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    private void takePart(Action action, List<MiddleCommandType> out, @Nullable Action nextPlacePartAction) throws IllegalStateException, SQLException, CRCLException, PmException {
        checkDbReady();
        checkSettings();
        String partName = action.getArgs()[takePartArgIndex];

        if (null != kitInspectionJInternalFrame) {
            Utils.runOnDispatchThread(() -> updateKitImageLabel("init", "Building kit"));
        }

        takePartByName(partName, nextPlacePartAction, out);
    }

    @UIEffect
    private void updateKitImageLabel(String kitImageName, String kitTextLabel) {
        if (null != kitInspectionJInternalFrame) {
            kitInspectionJInternalFrame.setKitImage(kitImageName);
            kitInspectionJInternalFrame.getKitTitleLabel().setText(kitTextLabel);
            setCorrectKitImage();
        }
    }

    private static String getBaseName(String name) {
        String baseName = name;
        int lastCharIndex = baseName.length() - 1;
        char lastChar = baseName.charAt(lastCharIndex);
        while (lastChar == '_' || Character.isDigit(lastChar) && lastCharIndex > 1) {
            lastCharIndex--;
            lastChar = baseName.charAt(lastCharIndex);
        }
        return baseName.substring(0, lastCharIndex + 1);
    }

    private void takePartByName(String partName, @Nullable Action nextPlacePartAction, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {
        checkSettings();
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
        String basePartName = getBaseName(partName);
        if (null != basePartName && basePartName.length() > 1) {
            PoseType attachPoseOffset = trayAttachOffsetsMap.get(basePartName);
            if (null != attachPoseOffset) {
                pose = CRCLPosemath.multiply(pose, attachPoseOffset);
            }
        }
        pose = visionToRobotPose(pose);

        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        takePartByPose(out, pose, partName);
        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, (CrclCommandWrapper ccw) -> {
            logDebug(markerMsg + " at " + new Date());
            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
        });
        lastTakenPart = partName;
        //inspectionList.add(partName);
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
    }

    private void recordSkipTakePart(String partName, @Nullable PoseType pose) throws IllegalStateException {
        lastTakenPart = null;
        takeSnapshots("plan", "skipping-take-part-" + partName + "", pose, partName);
    }

    /**
     * Add commands to the list that will go through the motions to take a given
     * part but skip closing the gripper.
     *
     * @param action PDDL action
     * @param out list of commands to append to
     * @throws IllegalStateException if database state is not consistent, eg
     * part not in the correct slot
     * @throws SQLException if database query fails
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    private void fakeTakePart(Action action, List<MiddleCommandType> out) throws IllegalStateException, SQLException, CRCLException, PmException {
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
            logDebug(markerMsg + " at " + new Date());
            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
        });
        lastTakenPart = partName;
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
    }

    private void takePartRecovery(String partName, List<MiddleCommandType> out) throws SQLException, CRCLException, PmException {
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
        takePartByPose(out, pose, partName);

        String markerMsg = "took part " + partName;
        addMarkerCommand(out, markerMsg, x -> {
            logDebug(markerMsg + " at " + new Date());
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
     * <p>
     * The name could refer to a part, tray or slot. Poses are also cached until
     * a look-for-parts action clears the cache.
     *
     * @param posename name of position to get
     * @return pose of part,tray or slot
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
                    String entryKey = entry.getKey();
                    if (entryKey.equals(posename)) {
                        continue;
                    }
                    PointType point = pose.getPoint();
                    if (point == null) {
                        throw new IllegalStateException("pose has null point property");
                    }
                    PointType entryPoint = entry.getValue().getPoint();
                    if (entryPoint == null) {
                        throw new IllegalStateException("pose has null point property");
                    }
                    double diff = CRCLPosemath.diffPoints(point, entryPoint);
                    if (diff < 15.0) {

                        String errMsg = "two poses in cache are too close : diff=" + diff + " posename=" + posename + ",entry.getKey()=" + entryKey + ",pose=" + CRCLPosemath.toString(point) + ", entry=" + entry + ", entryPoint=" + CRCLPosemath.toString(entryPoint);
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
                    logDebug("poseCheck = " + poseCheck);
                    getPoseFailedOnce = true;
                }
            }
            return pose;
        }
    }

    @Nullable
    private PoseType getNewPoseFromDb(String posename) throws SQLException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }

        qs.setAprsSystem(aprsSystem);
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
        return pose;
    }

    @Nullable
    private PoseType debugGetNewPoseFromDb(String posename) throws SQLException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }

        boolean origDebug = qs.isDebug();
        qs.setDebug(true);
        PoseType pose = qs.getPose(posename, requireNewPoses, visionCycleNewDiffThreshold);
        qs.setDebug(origDebug);
        return pose;
    }

    private List<PartsTray> getPartsTrays(String name) throws SQLException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        List<PartsTray> list = new ArrayList<>(qs.getPartsTrays(name));

        return list;
    }

    private int getPartDesignPartCount(String kitName) throws SQLException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        int count = qs.getPartDesignPartCount(kitName);
        return count;
    }

    private List<String> getAllPartsInKt(String name) throws SQLException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        List<String> partsInKtList = new ArrayList<>(qs.getAllPartsInKt(name));

        return partsInKtList;
    }

    private List<String> getAllPartsInPt(String name) throws SQLException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        if (null == qs) {
            throw new IllegalStateException("QuerySet for database not initialized.(null)");
        }
        List<String> partsInPtList = new ArrayList<>(qs.getAllPartsInPt(name));

        return partsInPtList;
    }

    @Nullable
    private volatile PoseType lastTestApproachPose = null;

    private final ConcurrentMap<String, String> toolChangerJointValsMap
            = new ConcurrentHashMap<>();

    public void putToolChangerJointVals(String key, String value) {
        toolChangerJointValsMap.put(key, value);
    }

    @Nullable
    private String getToolChangerJointVals(String key) {
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

        if (null == pose) {
            throw new IllegalArgumentException("null == pose");
        }
        addOpenGripper(cmds);

        checkSettings();

        PoseType lastApproachPose = this.lastTestApproachPose;
        if (null != lastApproachPose) {
            addSetSlowSpeed(cmds);
            addMoveTo(cmds, lastApproachPose, false, "testPartPositionByPose.lastApproachPose");
            lastTestApproachPose = null;
        } else {
            addSlowLimitedMoveUpFromCurrent(cmds);
        }
        PoseType approachPose = addZToPose(pose, approachZOffset);

//        approachPose.getPoint().setZ(pose.getPoint().getZ() + approachZOffset);
        lastTestApproachPose = approachPose;

        PoseType takePose = requireNonNull(CRCLPosemath.copy(pose), "CRCLPosemath.copy(pose)");
        PointType posePoint = pose.getPoint();
        if (null == posePoint) {
            throw new IllegalStateException("pose has null point property");
        }
        PointType takePosePoint = takePose.getPoint();
        if (null == takePosePoint) {
            throw new IllegalStateException("pose has null point property");
        }
        takePosePoint.setZ(posePoint.getZ() + takeZOffset);

        addSetFastTestSpeed(cmds);

        addMoveTo(cmds, approachPose, false, "testPartPositionByPose.approachPose");

        addSettleDwell(cmds);

        addSetSlowTestSpeed(cmds);

        addMoveTo(cmds, takePose, true, "testPartPositionByPose.takePose");

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
//        logDebug("newTranPos = " + newTranPos);
//        PmCartesian newTran = pmPose.tran.add(newTranPos);
        PmPose approachPmPose = new PmPose(newTranPos, pmPose.rot);
        PoseType approachPose = CRCLPosemath.toPose(approachPmPose);
        return approachPose;
    }

    private void addCheckedOpenGripper(List<MiddleCommandType> cmds) {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        String lastPartTaken = getLastTakenPart();
        int lastIndex = getLastIndex();
        Action act = lastActionsList.get(lastIndex);
        String imgLabel = "openGripper" + lastIndex + act.asPddlLine() + "partTaken=" + lastPartTaken;
        addOptionalOpenGripper(cmds, (CrclCommandWrapper ccw) -> {
            AprsSystem af = aprsSystem;
            assert (af != null) : "af == null : @AssumeAssertion(nullness)";

            if (af.isObjectViewSimulated()) {
                double distToPart = af.getClosestRobotPartDistance();
                if (distToPart < dropOffMin) {
                    af.takeSnapshots(imgLabel);
                    String errString
                            = "Can't take part when distance to another part of " + distToPart + "  less than  " + dropOffMin;
                    double recheckDistance = af.getClosestRobotPartDistance();
                    logDebug("recheckDistance = " + recheckDistance);
                    setTitleErrorString(errString);
                    checkedPause();
                    throw new IllegalStateException(errString);
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
    @SuppressWarnings({"unused"})
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

    private static final PoseType DEFAULT_TOOL_OFFSET_POSE = CRCLPosemath.identityPose();
    private PoseType toolOffsetPose = DEFAULT_TOOL_OFFSET_POSE;

    /**
     * Get the value of toolOffsetPose
     *
     * @return the value of toolOffsetPose
     */
    public PoseType getToolOffsetPose() {
        return toolOffsetPose;
    }

    /**
     * Set the value of toolOffsetPose
     *
     * @param toolOffsetPose new value of toolOffsetPose
     */
    private void setToolOffsetPose(PoseType toolOffsetPose) {
        this.toolOffsetPose = toolOffsetPose;
    }

    private final Map<String, PoseType> toolOffsetMap = new ConcurrentHashMap<>();

    private final Map<String, String> currentToolHolderContentsMap = new ConcurrentHashMap<>();

    private final Map<String, String> expectedToolHolderContentsMap = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> possibleToolHolderContentsMap = new ConcurrentHashMap<>();

    /**
     * Get the value of expectedToolHolderContentsMap
     *
     * @return the value of expectedToolHolderContentsMap
     */
    public Map<String, Set<String>> getPossibleToolHolderContentsMap() {
        return possibleToolHolderContentsMap;
    }

    /**
     * Get the value of expectedToolHolderContentsMap
     *
     * @return the value of expectedToolHolderContentsMap
     */
    public Map<String, String> getExpectedToolHolderContentsMap() {
        return expectedToolHolderContentsMap;
    }

    /**
     * Get the value of currentToolHolderContentsMap
     *
     * @return the value of currentToolHolderContentsMap
     */
    public Map<String, String> getCurrentToolHolderContentsMap() {
        return currentToolHolderContentsMap;
    }

    /**
     * Get the value of toolOffsetMap
     *
     * @return the value of toolOffsetMap
     */
    public Map<String, PoseType> getToolOffsetMap() {
        return toolOffsetMap;
    }

    private String currentToolName = "empty";

    /**
     * Get the value of currentToolName
     *
     * @return the value of currentToolName
     */
    public String getCurrentToolName() {
        return currentToolName;
    }

    private final Map<String, PoseType> trayAttachOffsetsMap = new ConcurrentHashMap<>();

    /**
     * Get the value of trayAttachOffsetsMap
     *
     * @return the value of trayAttachOffsetsMap
     */
    public Map<String, PoseType> getTrayAttachOffsetsMap() {
        return trayAttachOffsetsMap;
    }

    private Map<String, PoseType> toolHolderPoseMap = new ConcurrentHashMap<>();

    /**
     * Get the value of toolHolderPoseMap
     *
     * @return the value of toolHolderPoseMap
     */
    public Map<String, PoseType> getToolHolderPoseMap() {
        return toolHolderPoseMap;
    }

    /**
     * Set the value of toolHolderPoseMap
     *
     * @param toolHolderPoseMap new value of toolHolderPoseMap
     */
    public void setToolHolderPoseMap(Map<String, PoseType> toolHolderPoseMap) {
        this.toolHolderPoseMap = toolHolderPoseMap;
    }

    /**
     * Set the value of currentToolName
     *
     * @param currentToolName new value of currentToolName
     */
    public void setCurrentToolName(String currentToolName) {
        String oldCurrentToolName = this.currentToolName;
        this.currentToolName = currentToolName;
        if (null != currentToolName && currentToolName.length() > 0) {
            PoseType newPose = toolOffsetMap.get(currentToolName);
            if (null != newPose) {
                setToolOffsetPose(newPose);
            }
            if (Objects.equals(currentToolName, oldCurrentToolName)) {
                return;
            }
            if (null != parentExecutorJPanel) {
                parentExecutorJPanel.setSelectedToolName(currentToolName);
            }
        }
    }

    /**
     * Add commands to the list that will take a part at a given pose.
     *
     * @param cmds list of commands to append to
     * @param pose pose where part is expected
     * @param name optional name for tracing/debug
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    public void takePartByPose(List<MiddleCommandType> cmds, PoseType pose, @Nullable String name) throws CRCLException, PmException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        PoseType poseWithToolOffset = CRCLPosemath.multiply(pose, toolOffsetPose);

        checkRobotPoseRotation(poseWithToolOffset, "poseWithToolOffset");

        logToolOffsetInfo(cmds, pose, poseWithToolOffset);

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = addZToPose(poseWithToolOffset, approachZOffset);
        checkRobotPoseRotation(approachPose, "approachPose");
        lastTestApproachPose = null;

        PoseType takePose = CRCLPosemath.copy(poseWithToolOffset);
        PointType poseWithToolOffsetPoint = poseWithToolOffset.getPoint();
        if (null == poseWithToolOffsetPoint) {
            throw new IllegalStateException("null == poseWithToolOffsetPoint");
        }
        PointType takePosePoint = takePose.getPoint();
        if (null == takePosePoint) {
            throw new IllegalStateException("null == takePosePoint");
        }
        takePosePoint.setZ(poseWithToolOffsetPoint.getZ() + takeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false, "takePartByPose.approachPose." + name);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true, "takePartByPose.takePose." + name);

        addSettleDwell(cmds);

        addOptionalCloseGripper(cmds, (CrclCommandWrapper ccw) -> {
            AprsSystem af = aprsSystem;
            assert (af != null) : "af == null : @AssumeAssertion(nullness)";
            if (af.isObjectViewSimulated()) {
                double distToPart = af.getClosestRobotPartDistance();
                if (distToPart > pickupDistMax) {
                    PointType currentPoint = af.getCurrentPosePoint();
                    if (null == currentPoint) {
                        throw new IllegalStateException("null == currentPoint");
                    }
                    PointType uncorrectedPoint = af.reverseCorrectPoint(currentPoint);
                    List<PhysicalItem> items = af.getSimItemsData();
                    String errString
                            = "Can't take part when distance of " + distToPart
                            + "  exceeds " + pickupDistMax
                            + ": currentPoint=" + CRCLPosemath.pointToString(currentPoint)
                            + ": uncorrectedPoint=" + CRCLPosemath.pointToString(uncorrectedPoint)
                            + ": \nitems=" + items;
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
        }, "takePartByPose." + name);

        addSettleDwell(cmds);
        addSetFastSpeed(cmds);
        addMoveTo(cmds, approachPose, true, "takePartByPose.approachPose.return." + name);
    }

    private void checkRobotPoseRotation(PoseType approachPose, String poseName) throws IllegalStateException {
        if ((Math.abs(Math.abs(approachPose.getXAxis().getI()) - 0.7) < 0.1 && aprsSystem.getRobotName().contains("otoman"))
                || (Math.abs(Math.abs(approachPose.getXAxis().getI()) - 1.0) < 0.1 && aprsSystem.getRobotName().contains("anuc"))) {
            String errmsg = "aprsSystem.getRobotName()=" + aprsSystem.getRobotName()
                    + ", " + poseName + ".getXAxis()=" + CRCLPosemath.vectorToPmCartesian(approachPose.getXAxis())
                    + ", xAxis=" + CRCLPosemath.vectorToPmCartesian(xAxis)
                    + ", options.get(\"rpy\")=" + options.get("rpy")
                    + ", settingsChecked=" + settingsChecked;
            aprsSystem.setTitleErrorString(errmsg);
            checkedPause();
            throw new IllegalStateException(errmsg);
        }
    }

    /**
     * Add commands to the list that will go through the motions to take a part
     * at a given pose but not close the gripper to actually take the part.
     *
     * @param cmds list of commands to append to
     * @param pose pose where part is expected
     * @throws crcl.utils.CRCLException failed to compose or send a CRCL message
     * @throws rcs.posemath.PmException failed to compute a valid pose
     */
    private void fakeTakePartByPose(List<MiddleCommandType> cmds, PoseType pose) throws CRCLException, PmException {

        addOpenGripper(cmds);

        checkSettings();
        PoseType approachPose = addZToPose(pose, approachZOffset);

        PoseType takePose = CRCLPosemath.copy(pose);
        PointType takePosePoint = takePose.getPoint();
        if (null == takePosePoint) {
            throw new IllegalStateException("null == takePosePoint");
        }
        PointType posePoint = pose.getPoint();
        if (null == posePoint) {
            throw new IllegalStateException("null == posePoint");
        }
        takePosePoint.setZ(posePoint.getZ() + takeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false, "fakeTakePartByPose.approachPose");

        addSettleDwell(cmds);

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, takePose, true, "fakeTakePartByPose.takePose");

        addSettleDwell(cmds);

        addSettleDwell(cmds);

        addMoveTo(cmds, approachPose, true, "fakeTakePartByPose.approachPose.return");

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

    private boolean checkPose(PoseType pose) {
        assert null != aprsSystem : "(null == aprsSystemInterface)";
        return aprsSystem.checkPose(pose);
    }

    private void checkSettings() {
        if (settingsChecked || options == null) {
            return;
        }
        settingsChecked = true;
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
                    if (!checkPose(pose)) {
                        throw new RuntimeException("invalid pose passed with rpy setting :" + CRCLPosemath.poseToString(pose));
                    }
                    VectorType xAxisVector = pose.getXAxis();
                    if (null == xAxisVector) {
                        throw new IllegalStateException("null == xAxisVector");
                    }
                    xAxis = xAxisVector;
                    if ((Math.abs(Math.abs(xAxis.getI()) - 0.7) < 0.1 && aprsSystem.getRobotName().contains("otoman"))
                            || (Math.abs(Math.abs(xAxis.getI()) - 1.0) < 0.1 && aprsSystem.getRobotName().contains("anuc"))) {
                        String errmsg = "aprsSystem.getRobotName()=" + aprsSystem.getRobotName()
                                + ", xAxis=" + CRCLPosemath.vectorToPmCartesian(xAxis)
                                + ", options.get(\"rpy\")=" + options.get("rpy")
                                + ", settingsChecked=" + settingsChecked;
                        aprsSystem.setTitleErrorString(errmsg);
                        checkedPause();
                        throw new IllegalStateException(errmsg);
                    }
                    VectorType zAxisVector = pose.getZAxis();
                    if (null == zAxisVector) {
                        throw new IllegalStateException("null == zAxisVector");
                    }
                    zAxis = zAxisVector;
                } else {
                    throw new Exception("bad rpyString = \"" + rpyString + "\", rpyFields=" + Arrays.toString(rpyFields));
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "", ex);
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
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String placeZOffsetString = options.get("placeZOffset");
        if (null != placeZOffsetString && placeZOffsetString.length() > 0) {
            try {
                placeZOffset = Double.parseDouble(placeZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String takeZOffsetString = options.get("takeZOffset");
        if (null != takeZOffsetString && takeZOffsetString.length() > 0) {
            try {
                takeZOffset = Double.parseDouble(takeZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String joint0DiffToleranceStringString = options.get("joint0DiffToleranceString");
        if (null != joint0DiffToleranceStringString && joint0DiffToleranceStringString.length() > 0) {
            try {
                joint0DiffTolerance = Double.parseDouble(joint0DiffToleranceStringString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String joint0DiffToleranceString = options.get("joint0DiffTolerance");
        if (null != joint0DiffToleranceString && joint0DiffToleranceString.length() > 0) {
            try {
                joint0DiffTolerance = Double.parseDouble(joint0DiffToleranceString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String settleDwellTimeString = options.get("settleDwellTime");
        if (null != settleDwellTimeString && settleDwellTimeString.length() > 0) {
            try {
                settleDwellTime = Double.parseDouble(settleDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String lookDwellTimeString = options.get("lookDwellTime");
        if (null != lookDwellTimeString && lookDwellTimeString.length() > 0) {
            try {
                lookDwellTime = Double.parseDouble(lookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String skipLookDwellTimeString = options.get("skipLookDwellTime");
        if (null != skipLookDwellTimeString && skipLookDwellTimeString.length() > 0) {
            try {
                skipLookDwellTime = Double.parseDouble(skipLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String afterMoveToLookForDwellTimeString = options.get("afterMoveToLookForDwellTime");
        if (null != afterMoveToLookForDwellTimeString && afterMoveToLookForDwellTimeString.length() > 0) {
            try {
                afterMoveToLookForDwellTime = Double.parseDouble(afterMoveToLookForDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        // afterMoveToLookForDwellTime
        String firstLookDwellTimeString = options.get("firstLookDwellTime");
        if (null != firstLookDwellTimeString && firstLookDwellTimeString.length() > 0) {
            try {
                firstLookDwellTime = Double.parseDouble(firstLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String lastLookDwellTimeString = options.get("lastLookDwellTime");
        if (null != lastLookDwellTimeString && lastLookDwellTimeString.length() > 0) {
            try {
                lastLookDwellTime = Double.parseDouble(lastLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String fastTransSpeedString = options.get("fastTransSpeed");
        if (null != fastTransSpeedString && fastTransSpeedString.length() > 0) {
            try {
                fastTransSpeed = Double.parseDouble(fastTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String testTransSpeedString = options.get("testTransSpeed");
        if (null != testTransSpeedString && testTransSpeedString.length() > 0) {
            try {
                testTransSpeed = Double.parseDouble(testTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String rotSpeedString = options.get("rotSpeed");
        if (null != rotSpeedString && rotSpeedString.length() > 0) {
            try {
                rotSpeed = Double.parseDouble(rotSpeedString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String jointSpeedString = options.get("jointSpeed");
        if (null != jointSpeedString && jointSpeedString.length() > 0) {
            try {
                jointSpeed = Double.parseDouble(jointSpeedString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String jointAccelString = options.get("jointAccel");
        if (null != jointAccelString && jointAccelString.length() > 0) {
            try {
                jointAccel = Double.parseDouble(jointAccelString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String kitInspectDistThresholdString = options.get("kitInspectDistThreshold");
        if (null != kitInspectDistThresholdString && kitInspectDistThresholdString.length() > 0) {
            try {
                kitInspectDistThreshold = Double.parseDouble(kitInspectDistThresholdString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String slowTransSpeedString = options.get("slowTransSpeed");
        if (null != slowTransSpeedString && slowTransSpeedString.length() > 0) {
            try {
                slowTransSpeed = Double.parseDouble(slowTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String verySlowTransSpeedString = options.get("verySlowTransSpeed");
        if (null != verySlowTransSpeedString && verySlowTransSpeedString.length() > 0) {
            try {
                verySlowTransSpeed = Double.parseDouble(verySlowTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.SEVERE, "", numberFormatException);
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
        String useJointMovesForToolHolderApproachString = options.get("useJointMovesForToolHolderApproach");
        if (null != useJointMovesForToolHolderApproachString && useJointMovesForToolHolderApproachString.length() > 0) {
            useJointMovesForToolHolderApproach = Boolean.valueOf(useJointMovesForToolHolderApproachString);
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

    private PoseType copyAndAddZ(PoseType poseIn, double offset, double limit) {
        PoseType poseOut = requireNonNull(CRCLPosemath.copy(poseIn), "CRCLPosemath.copy(poseIn)");
        PointType outPoint = poseOut.getPoint();
        if (null == outPoint) {
            throw new IllegalStateException("null == outPoint");
        }
        outPoint.setZ(Math.min(limit, outPoint.getZ() + offset));
        return poseOut;
    }

    private void addMoveUpFromCurrent(List<MiddleCommandType> cmds, double offset, double limit) {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        MessageType origMessageCmd = new MessageType();
        origMessageCmd.setMessage("moveUpFromCurrent" + " action=" + lastIndex + " crclNumber=" + crclNumber.get() + ",offset=" + offset + ",limit=" + limit);
        addOptionalCommand(origMessageCmd, cmds, (CrclCommandWrapper wrapper) -> {
            MiddleCommandType cmd = wrapper.getWrappedCommand();
            AprsSystem af = requireNonNull(aprsSystem, "aprsSystem");
            PoseType pose = requireNonNull(af.getCurrentPose(), "af.getCurrentPose()");
            PointType posePoint = requireNonNull(pose.getPoint(), "pose.getPoint()");
            if (posePoint.getZ() >= (limit - 1e-6)) {
                MessageType messageCommand = new MessageType();
                messageCommand.setMessage("moveUpFromCurrent NOT needed." + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
                wrapper.setWrappedCommand(messageCommand);
            } else {
                MoveToType moveToCmd = new MoveToType();
//                moveToCmd.setName("addMoveUpFromCurrent offset=" + offset + ",limit=" + limit);
                moveToCmd.setEndPosition(copyAndAddZ(pose, offset, limit));
                moveToCmd.setMoveStraight(true);
                wrapper.setWrappedCommand(moveToCmd);
            }
        });
    }

    private void addOptionalCloseGripper(List<MiddleCommandType> cmds, CRCLCommandWrapperConsumer cb, String name) {
        SetEndEffectorType closeGrippeerCmd = new SetEndEffectorType();
        closeGrippeerCmd.setName(toNonColonizedName(name));
        closeGrippeerCmd.setSetting(0.0);
        addOptionalCommand(closeGrippeerCmd, cmds, cb);
    }

    private String toNonColonizedName(String name) {
        return name.trim().replace(' ', '_').replace('=', '_');
    }

    private void addOpenToolChanger(List<MiddleCommandType> cmds) {
        addDwell(cmds, 0.1);
        OpenToolChangerType openToolChangerCmd = new OpenToolChangerType();
        setCommandId(openToolChangerCmd);
        cmds.add(openToolChangerCmd);
        addDwell(cmds, 0.1);
//        setCurrentToolName(currentToolName);
    }

    private void addCloseToolChanger(List<MiddleCommandType> cmds) {
        addDwell(cmds, 0.1);
        CloseToolChangerType openToolChangerCmd = new CloseToolChangerType();
        setCommandId(openToolChangerCmd);
        cmds.add(openToolChangerCmd);
        addDwell(cmds, 0.1);
    }

    private void addMoveTo(List<MiddleCommandType> cmds, PoseType pose, boolean straight, String message) {
        addMessageCommand(cmds, message);
        MoveToType moveCmd = new MoveToType();
        setCommandId(moveCmd);
        checkRobotPoseRotation(pose, "pose");
        if (!checkPose(pose)) {
            throw new RuntimeException("invalid pose passed to addMoveTo :" + CRCLPosemath.poseToString(pose));
        }
        moveCmd.setEndPosition(pose);
        moveCmd.setMoveStraight(straight);
        cmds.add(moveCmd);
        atLookForPosition = false;
        PointType posePoint = requireNonNull(pose.getPoint(), "pose.getPoint()");
        expectedJoint0Val = getExpectedJoint0(posePoint);
        logDebug("addMoveTo: expectedJoint0Val = " + expectedJoint0Val);
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

    private final Logger logger = Logger.getLogger(CrclGenerator.class.getName());

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
            addMoveTo(out, pose, false, "addMoveToLookForPosition");
        } else {
            assert (null != lookForJointsString) : "@AssumeAssertion(nullness)";
            double jointVals[] = jointValStringToArray(lookForJointsString);
            addPrepJointMove(jointVals, out);
            addJointMove(out, jointVals, 1.0);
        }
        addMarkerCommand(out, "set atLookForPosition true", x -> {
            atLookForPosition = true;
        });
    }

    private void addPrepJointMove(double[] jointVals, List<MiddleCommandType> out) {
        double joint0 = jointVals[0];
        double joint0diff = joint0 - expectedJoint0Val;
        addMessageCommand(out, "addJointPrepMove");
        if (toolHolderOperationEnabled && Double.isFinite(expectedJoint0Val) && Math.abs(joint0diff) > joint0DiffTolerance) {
            double jointValsCopy[] = Arrays.copyOf(jointVals, jointVals.length);
            jointValsCopy[0] = expectedJoint0Val;
            addJointMove(out, jointValsCopy, 1.0);
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

    private static double[] jointValStringToArray(String jointVals) {
        String jointPosStrings[] = jointVals.split("[,]+");
        double array[] = new double[jointPosStrings.length];
        for (int i = 0; i < jointPosStrings.length; i++) {
            array[i] = Double.parseDouble(jointPosStrings[i]);
        }
        return array;
    }

    private final Map<Double, Double> atanXYJoint0Map = new TreeMap<>();
    private volatile boolean atanXYJoint0MapLoaded = false;

    private void loadAtanXYJoint0Map() {
        if (!atanXYJoint0MapLoaded) {
            atanXYJoint0MapLoaded = true;
            atanXYJoint0Map.clear();
            for (Entry<String, PoseType> entry : this.toolHolderPoseMap.entrySet()) {
                String name = entry.getKey();
                String jointValsString = this.toolChangerJointValsMap.get(name);
                if (null != jointValsString && jointValsString.length() > 0) {
                    PoseType pose = entry.getValue();
                    PointType posePoint = requireNonNull(pose.getPoint(), "pose.getPoint()");
                    double atanxy = atanPoint(posePoint);
                    double jointVals[] = jointValStringToArray(jointValsString);
                    double joint0 = jointVals[0];
                    logDebug(name + "," + atanxy + "," + joint0);
                    atanXYJoint0Map.put(atanxy, jointVals[0]);
                }
            }
            String lookForJointsString = options.get("lookForJoints");
            if (null != lookForJointsString) {
                double jointVals[] = jointValStringToArray(lookForJointsString);
                PointType pt = getLookForXYZ();
                if (null != pt) {
                    double atanxy = atanPoint(pt);
                    double joint0 = jointVals[0];
                    logDebug("lookForJoints," + atanxy + "," + joint0);
                    atanXYJoint0Map.put(atanxy, joint0);
                }
            }
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
        }
    }

    private double atanPoint(PointType pt) {
        return Math.atan2(pt.getY(), pt.getX());
    }

    private double getExpectedJoint0(PointType pt) {
        return getExpectedJoint0(atanPoint(pt));
    }

    private double getExpectedJoint0(double atanxy) {
        if (!toolHolderOperationEnabled) {
            return Double.NaN;
        }
        loadAtanXYJoint0Map();
        double lastAtanXYfromMap = Double.NaN;
        double curAtanXYfromMap = Double.NaN;
        double lastJoint0fromMap = Double.NaN;
        double curJoint0fromMap = Double.NaN;
        Iterator<Entry<Double, Double>> antanXYMapEntryIterator
                = atanXYJoint0Map.entrySet().iterator();
        while (antanXYMapEntryIterator.hasNext()) {
            Entry<Double, Double> entry = antanXYMapEntryIterator.next();
            curAtanXYfromMap = entry.getKey();
            curJoint0fromMap = entry.getValue();
            if (curAtanXYfromMap > atanxy && Double.isFinite(lastAtanXYfromMap) && Double.isFinite(lastJoint0fromMap)) {
                break;
            }
            double diffAtan = curAtanXYfromMap - lastAtanXYfromMap;
            if (Math.abs(diffAtan) < 2 * Double.MIN_NORMAL) {
                continue;
            }
            if (antanXYMapEntryIterator.hasNext()) {
                lastAtanXYfromMap = curAtanXYfromMap;
                lastJoint0fromMap = curJoint0fromMap;
            }
        }
        if (!Double.isFinite(lastAtanXYfromMap)) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("lastAtanXYfromMap = " + lastAtanXYfromMap);
            return Double.NaN;
        }
        if (!Double.isFinite(curAtanXYfromMap)) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("curAtanXYfromMap = " + curAtanXYfromMap);
            return Double.NaN;
        }
        if (atanxy - curAtanXYfromMap > Math.PI / 4 && atanxy - lastAtanXYfromMap > Math.PI / 4) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("curAtanXYfromMap = " + curAtanXYfromMap);
            return Double.NaN;
        }
        if (atanxy - curAtanXYfromMap < -Math.PI / 4 && atanxy - lastAtanXYfromMap < -Math.PI / 4) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("curAtanXYfromMap = " + curAtanXYfromMap);
            return Double.NaN;
        }
        if (!Double.isFinite(lastJoint0fromMap)) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("lastJoint0fromMap = " + lastJoint0fromMap);
            return Double.NaN;
        }
        if (!Double.isFinite(curJoint0fromMap)) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("curJoint0fromMap = " + curJoint0fromMap);
            return Double.NaN;
        }
        double diffAtan = curAtanXYfromMap - lastAtanXYfromMap;
        logDebug("atanxy = " + atanxy);
        logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
        logDebug("curAtanXYfromMap = " + curAtanXYfromMap);
        logDebug("lastAtanXYfromMap = " + lastAtanXYfromMap);
        logDebug("curJoint0fromMap = " + curJoint0fromMap);
        logDebug("lastJoint0fromMap = " + lastJoint0fromMap);
        logDebug("diffAtan = " + diffAtan);
        if (Math.abs(diffAtan) < Double.MIN_NORMAL) {
            logDebug("atanxy = " + atanxy);
            logDebug("atanXYJoint0Map = " + atanXYJoint0Map);
            logDebug("diffAtan = " + diffAtan);
            return Double.NaN;
        }
        double diffJoint0 = curJoint0fromMap - lastJoint0fromMap;
        logDebug("diffJoint0 = " + diffJoint0);
        double ratio = (atanxy - lastAtanXYfromMap) / diffAtan;
        logDebug("ratio = " + ratio);
        double ret = lastJoint0fromMap + ratio * diffJoint0;
        logDebug("ret = " + ret);
        return ret;
    }

    private void addJointMove(List<MiddleCommandType> out, double jointVals[], double speedScale) {
        ActuateJointsType ajCmd = new ActuateJointsType();
        setCommandId(ajCmd);
        ajCmd.getActuateJoint().clear();
        for (int i = 0; i < jointVals.length; i++) {
            ActuateJointType aj = new ActuateJointType();
            JointSpeedAccelType jsa = new JointSpeedAccelType();
            jsa.setJointAccel(jointAccel * speedScale);
            jsa.setJointSpeed(jointSpeed * speedScale);
            aj.setJointDetails(jsa);
            aj.setJointNumber(i + 1);
            aj.setJointPosition(jointVals[i]);
            ajCmd.getActuateJoint().add(aj);
        }
        out.add(ajCmd);
        atLookForPosition = false;
        if (toolHolderOperationEnabled) {
            expectedJoint0Val = jointVals[0];
            logDebug("addJointMove: expectedJoint0Val = " + expectedJoint0Val);
        }
    }

    @Nullable
    private volatile Thread clearPoseCacheThread = null;
    private volatile StackTraceElement clearPoseCacheTrace@Nullable []  = null;

    public void clearPoseCache() {
        clearPoseCacheThread = Thread.currentThread();
        clearPoseCacheTrace = clearPoseCacheThread.getStackTrace();
        synchronized (poseCache) {
            poseCache.clear();
        }
    }

    @Nullable
    private volatile Thread putPoseCacheThread = null;
    private volatile StackTraceElement putPoseCacheTrace@Nullable []  = null;

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

    private boolean checkAtLookForPosition() {
        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
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
            PointType currentPoint = aprsSystem.getCurrentPosePoint();
            if (null == currentPoint) {
//                System.err.println("checkAtLookForPosition: getCurrentPosePoint() returned null");
                return false;
            }
            double diff = CRCLPosemath.diffPoints(currentPoint, lookForPoint);
            return diff < 2.0;
        } else {
            CRCLStatusType curStatus = aprsSystem.getCurrentStatus();
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

    @SuppressWarnings("unused")
    private void endProgram(Action action, List<MiddleCommandType> out) throws IllegalStateException {
        if (atLookForPosition) {
            atLookForPosition = checkAtLookForPosition();
        }
        if (!atLookForPosition) {
            addMoveToLookForPosition(out, false);
        }
        TakenPartList.clear();
    }

    @SuppressWarnings("SameParameterValue")
    private void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        if (null != aprsSystem) {
            aprsSystem.setEnableVisionToDatabaseUpdates(enableDatabaseUpdates, requiredParts);
        }
    }

    private void lookForParts(Action action, List<MiddleCommandType> out, boolean firstAction, boolean lastAction) throws IllegalStateException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        lastTestApproachPose = null;
        checkSettings();
        checkDbReady();
        if (null == kitInspectionJInternalFrame) {
            KitInspectionJInternalFrame newKitInspectionJInternalFrame
                    = aprsSystem.getKitInspectionJInternalFrame();
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
                    logger.log(Level.SEVERE, "", ex);
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
    @SuppressWarnings("unused")
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

    private void gotoToolChangerApproach(Action action, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {

        lastTestApproachPose = null;
        checkSettings();
        checkDbReady();
        String toolChangerPosName = action.getArgs()[toolHolderNameArgIndex];
        addGotoToolChangerApproachByName(out, toolChangerPosName);
    }

    private double expectedJoint0Val = Double.NaN;
    private double joint0DiffTolerance = 20.0;

    public double getJoint0DiffTolerance() {
        return joint0DiffTolerance;
    }

    public void setJoint0DiffTolerance(double joint0DiffTolerance) {
        this.joint0DiffTolerance = joint0DiffTolerance;
    }

    private void addGotoToolChangerApproachByName(List<MiddleCommandType> out, String toolChangerPosName) throws PmException, CRCLException, IllegalStateException {
        clearWayToHolder(out, toolChangerPosName);
        addMessageCommand(out, "Goto Tool Changer Approach By Name " + toolChangerPosName);
        addSlowLimitedMoveUpFromCurrent(out);
        String jointValsString = getToolChangerJointVals(toolChangerPosName);
        if (useJointMovesForToolHolderApproach && null != jointValsString && jointValsString.length() > 0) {
            String lookForJointsString = options.get("lookForJoints");
            double jointVals[] = jointValStringToArray(jointValsString);
            double joint0Diff = expectedJoint0Val - jointVals[0];
            logDebug("addGotoToolChangerApproachByName: jointVals[0] = " + jointVals[0]);
            logDebug("addGotoToolChangerApproachByName: expectedJoint0Val = " + expectedJoint0Val);
            logDebug("addGotoToolChangerApproachByName: diff0 = " + joint0Diff);
            logDebug("addGotoToolChangerApproachByName: joint0DiffTolerance = " + joint0DiffTolerance);
            if (!Double.isFinite(expectedJoint0Val) || Math.abs(joint0Diff) > joint0DiffTolerance) {
                if (null != lookForJointsString && lookForJointsString.length() > 0) {
                    double lookForJointVals[] = jointValStringToArray(lookForJointsString);
                    addPrepJointMove(lookForJointVals, out);
                    lookForJointVals[0] = jointVals[0];
                    addMessageCommand(out,
                            "Goto Tool Changer Approach By Name " + toolChangerPosName + " addJointMove(lookForJointVals)");
                    addJointMove(out, lookForJointVals, 1.0);
                }
            }
            addJointMove(out, jointVals, 1.0);
        } else {
            PoseType pose = getToolHolderPose(toolChangerPosName);
            if (null == pose) {
                throw new IllegalStateException("no pose for " + toolChangerPosName);
            }
            gotoToolChangerApproachByPose(pose, out);
        }
        addDwell(out, 0.1);
    }

    private void gotoToolChangerApproachByPose(PoseType pose, List<MiddleCommandType> out) throws CRCLException, PmException {
        PoseType approachPose = approachPoseFromToolChangerPose(pose);
        addSetSlowSpeed(out);
        addMoveTo(out, approachPose, false, "gotoToolChangerApproachByPose");
    }

    public PoseType approachPoseFromToolChangerPose(PoseType pose) throws PmException, CRCLException {
        PoseType approachPose = addZToPose(pose, approachToolChangerZOffset);
        return approachPose;
    }

    private String expectedToolName = "empty";

    /**
     * Get the value of expectedToolName
     *
     * @return the value of expectedToolName
     */
    private String getExpectedToolName() {
        return expectedToolName;
    }

    /**
     * Set the value of expectedToolName
     *
     * @param expectedToolName new value of expectedToolName
     */
    public void setExpectedToolName(String expectedToolName) {
        this.expectedToolName = expectedToolName;
    }

    private PoseType getToolHolderPose(String holderName) {
        PoseType ret = toolHolderPoseMap.get(holderName);
        if (null == ret) {
            throw new IllegalStateException("holderPoseMap has null for " + holderName);
        }
        return ret;
    }

    private void dropToolByHolder(Action action, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {
        String toolHolderName = action.getArgs()[toolHolderNameArgIndex];
        dropToolByHolderName(toolHolderName, out);
    }

    private void dropToolByHolderName(String toolHolderName, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {
        lastTestApproachPose = null;
        checkSettings();
        checkDbReady();
        PoseType pose = getToolHolderPose(toolHolderName);
        if (null == pose) {
            throw new IllegalStateException("no pose for " + toolHolderName);
        }
        addGotoToolChangerApproachByName(out, toolHolderName);
        PoseType prepPose = addZToPose(pose, approachToolChangerZOffset * 0.2);
        addMoveTo(out, prepPose, false, "dropToolByHolderName.prepPose.toolHolderName=" + toolHolderName);
        addSetSlowSpeed(out);
        addMoveTo(out, pose, false, "dropToolByHolderName.pose.toolHolderName=" + toolHolderName);
        addOpenToolChanger(out);
        String toolInRobot = getExpectedToolName();
        if (isEmptyTool(toolInRobot)) {
            throw new IllegalStateException("planning to drop tool when robot expected to be holding " + toolInRobot);
        }
        String toolInHolder = getExpectedToolHolderContents(toolHolderName);
        if (!isEmptyTool(toolInHolder)) {
            throw new IllegalStateException("planning to drop tool when holder expected to be holding " + toolInHolder);
        }
        Set<String> possibleTools = getPossibleToolHolderContents(toolHolderName);
        if (!possibleTools.contains(toolInRobot)) {
            throw new IllegalStateException("planning to drop tool when holder expected to be holding " + toolInHolder + " but " + toolHolderName + " may only store " + possibleTools);
        }
        addMarkerCommand(out,
                "dropTool " + toolInRobot + " in " + toolHolderName,
                (CrclCommandWrapper cmd) -> {
                    String currentToolInRobot = getCurrentToolName();
                    if (isEmptyTool(currentToolInRobot)) {
                        throw new IllegalStateException("dropping tool when robot holding " + currentToolInRobot);
                    }
                    String currentToolInHolder = getCurrentToolHolderContents(toolHolderName);
                    if (!isEmptyTool(currentToolInHolder)) {
                        throw new IllegalStateException("dropping tool when holder holding " + currentToolInHolder);
                    }
                    setCurrentToolName("empty");
                    updateCurrentToolHolderContentsMap(toolHolderName, currentToolInRobot);
                }
        );
        setExpectedToolName("empty");
        expectedToolHolderContentsMap.put(toolHolderName, toolInRobot);
        gotoToolChangerApproachByPose(pose, out);
    }

    private void dropToolAny(Action action, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {
        lastTestApproachPose = null;
        String toolHolderName = null;
        String toolInRobot = getExpectedToolName();
        if (isEmptyTool(toolInRobot)) {
            throw new IllegalStateException("planning to drop tool when robot expected to be holding " + toolInRobot);
        }
        for (Entry<String, String> contentsEntry : getExpectedToolHolderContentsMap().entrySet()) {
            String toolInHolder = contentsEntry.getValue();
            if (!isEmptyTool(toolInHolder)) {
                System.out.println("toolInHolder = " + toolInHolder + " is not empty");
                continue;
            }
            String toolHolderNameToCheck = contentsEntry.getKey();
            Set<String> possibleTools = getPossibleToolHolderContents(toolHolderNameToCheck);
            if (!possibleTools.contains(toolInRobot)) {
                System.out.println("toolInRobot = " + toolInRobot + " can not be added to holder " + toolHolderNameToCheck + ", possibleTools=" + possibleTools);
                continue;
            }
            toolHolderName = toolHolderNameToCheck;
            break;
        }
        if (null == toolHolderName) {
            throw new IllegalStateException("null == toolHolderName");
        }
        dropToolByHolderName(toolHolderName, out);
    }

    private void updateCurrentToolHolderContentsMap(String toolChangerPosName, String toolName) {
        currentToolHolderContentsMap.put(toolChangerPosName, toolName);
        if (null != parentExecutorJPanel) {
            parentExecutorJPanel.updateCurrentToolHolderContentsMap(toolChangerPosName, toolName);
        }
    }

    private void pickupToolByHolder(Action action, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {
        String toolHolderName = action.getArgs()[toolHolderNameArgIndex];
        pickupToolByHolderName(toolHolderName, out);
    }

    private void switchTool(Action action, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {
        String toolInRobot = getExpectedToolName();
        if (!isEmptyTool(toolInRobot)) {
            dropToolAny(action, out);
        }
        String desiredToolName = action.getArgs()[toolHolderNameArgIndex];
        if (!isEmptyTool(desiredToolName)) {
            pickupToolByTool(action, out);
        }
    }

    private void pickupToolByTool(Action action, List<MiddleCommandType> out) throws IllegalStateException, CRCLException, PmException {
        String desiredToolName = action.getArgs()[toolHolderNameArgIndex];
        String toolHolderName = null;
        String toolInRobot = getExpectedToolName();
        if (!isEmptyTool(toolInRobot)) {
            throw new IllegalStateException("planning to pickup tool when tool robot expected to be holding = " + toolInRobot);
        }
        for (Entry<String, String> contentsEntry : getExpectedToolHolderContentsMap().entrySet()) {
            String toolInHolder = contentsEntry.getValue();
            String toolHolderNameToCheck = contentsEntry.getKey();
            if (isEmptyTool(toolInHolder)) {
                System.out.println("toolInHolder = " + toolInHolder + "for " + toolHolderNameToCheck + " is empty");
                continue;
            }

            if (!Objects.equals(toolInHolder, desiredToolName)) {
                System.out.println("toolInHolder = " + toolInHolder + " for " + toolHolderNameToCheck + " does not equal desiredToolName=" + desiredToolName);
                continue;
            }
            toolHolderName = toolHolderNameToCheck;
            break;
        }
        if (null == toolHolderName) {
            throw new IllegalStateException("null == toolHolderName");
        }
        pickupToolByHolderName(toolHolderName, out);
    }

    private void pickupToolByHolderName(String toolHolderName, List<MiddleCommandType> out) throws PmException, IllegalStateException, CRCLException {
        lastTestApproachPose = null;
        checkSettings();
        checkDbReady();
        PoseType pose = getToolHolderPose(toolHolderName);
        if (null == pose) {
            throw new IllegalStateException("no pose for " + toolHolderName);
        }
        addGotoToolChangerApproachByName(out, toolHolderName);
        PoseType prepPose = addZToPose(pose, approachToolChangerZOffset * 0.2);
        addMoveTo(out, prepPose, false, "pickupToolByHolderName.prepPose.toolHolderName=" + toolHolderName);
        addSetVerySlowSpeed(out);
        addMoveTo(out, pose, false, "pickupToolByHolderName.pose.toolHolderName=" + toolHolderName);
        String toolInRobot = getExpectedToolName();
        if (!isEmptyTool(toolInRobot)) {
            throw new IllegalStateException("planning to pickup tool when tool robot expected to be holding = " + toolInRobot);
        }
        String toolInHolder = getExpectedToolHolderContents(toolHolderName);
        if (isEmptyTool(toolInHolder)) {
            throw new IllegalStateException("planning to pukup tool when tool holder expected to contain is " + toolInHolder);
        }
        addMarkerCommand(out,
                "pickupTool " + toolInHolder + " from " + toolHolderName,
                (CrclCommandWrapper cmd) -> {
                    String prevRobotTool = getCurrentToolName();
                    if (!isEmptyTool(prevRobotTool)) {
                        throw new IllegalStateException("pickup tool when currentTool = " + prevRobotTool);
                    }
                    String prevHolderTool = getCurrentToolHolderContents(toolHolderName);
                    if (isEmptyTool(prevHolderTool)) {
                        throw new IllegalStateException("pickup tool when currentTool = " + prevHolderTool);
                    }
                    setCurrentToolName(prevHolderTool);
                    updateCurrentToolHolderContentsMap(toolHolderName, "empty");
                }
        );
        setExpectedToolName(toolInHolder);
        expectedToolHolderContentsMap.put(toolHolderName, "empty");
        addCloseToolChanger(out);
        gotoToolChangerApproachByPose(pose, out);
    }

    private String getExpectedToolHolderContents(String toolHolderName) {
        String toolName = expectedToolHolderContentsMap.get(toolHolderName);
        if (null == toolName) {
            throw new IllegalStateException("expectedToolHolderContentsMap contains no entry for " + toolHolderName);
        }
        return toolName;
    }

    private Set<String> getPossibleToolHolderContents(String toolHolderName) {
        Set<String> tools = possibleToolHolderContentsMap.get(toolHolderName);
        if (null == tools) {
            throw new IllegalStateException("expectedToolHolderContentsMap contains no entry for " + toolHolderName);
        }
        return tools;
    }

    private String getCurrentToolHolderContents(String toolHolderName) {
        String toolName = currentToolHolderContentsMap.get(toolHolderName);
        if (null == toolName) {
            throw new IllegalStateException("currentToolHolderContentsMap has no entry for " + toolHolderName);
        }
        return toolName;
    }

    public static boolean isEmptyTool(@Nullable String toolName) {
        return toolName == null || toolName.length() < 1 || "empty".equals(toolName);
    }

    private void gotoToolChangerPose(Action action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {

        lastTestApproachPose = null;
        String toolHolderName = action.getArgs()[toolHolderNameArgIndex];
        checkSettings();
        checkDbReady();
        PoseType pose = getPose(toolHolderName);
        if (null == pose) {
            throw new IllegalStateException("no pose for " + toolHolderName);
        }
        addSetSlowSpeed(out);
        addMoveTo(out, pose, false, "gotoToolChangerPose.toolHolderName=" + toolHolderName);
    }

    private void checkDbReady() throws IllegalStateException {
        if (null == externalPoseProvider) {
            if (null == qs) {
                throw new IllegalStateException("Database not setup and connected.");
            }
        }
    }

    public void setStepMode(boolean step) {
        if (null == aprsSystem) {
            throw new IllegalStateException("null == aprsSystemInterface");
        }
        aprsSystem.setStepMode(step);
    }

    private final AtomicInteger visionUpdateCount = new AtomicInteger();

    @SuppressWarnings("SameParameterValue")
    private List<PhysicalItem> waitForCompleteVisionUpdates(String prefix, Map<String, Integer> requiredPartsMap, long timeoutMillis)
            throws InterruptedException, ExecutionException {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        String runName = getRunName();
        visionUpdateCount.incrementAndGet();

        XFuture<List<PhysicalItem>> xfl = aprsSystem.getSingleVisionToDbUpdate();
        int startSimViewRefreshCount = aprsSystem.getSimViewRefreshCount();
        int startSimViewPublishCount = aprsSystem.getSimViewPublishCount();
        aprsSystem.refreshSimView();
        long t0 = System.currentTimeMillis();
        int waitCycle = 0;
        long last_t1 = t0;

        while (!xfl.isDone()) {
            waitCycle++;
            long t1 = System.currentTimeMillis();
            if (timeoutMillis > 0 && t1 - t0 > timeoutMillis) {
                System.err.println("waitForCompleteVisionUpdates " + prefix + " timed out");
                System.err.println("runName=" + getRunName());
                long updateTime = aprsSystem.getLastSingleVisionToDbUpdateTime();
                long timeSinceUpdate = t1 - updateTime;
                System.err.println("timeSinceUpdate = " + timeSinceUpdate);
                long notifyTime = aprsSystem.getSingleVisionToDbNotifySingleUpdateListenersTime();
                long timeSinceNotify = t1 - notifyTime;
                System.err.println("timeSinceNotify = " + timeSinceNotify);
                System.err.println("xfl = " + xfl);
                System.err.println("waitCycle = " + waitCycle);
                long lastCycleTime = (t1 - last_t1);
                System.err.println("lastCycleTime = " + lastCycleTime);
                long simViewRefreshTime = aprsSystem.getLastSimViewRefreshTime();
                long simViewPublishTime = aprsSystem.getLastSimViewPublishTime();
                long timeSinceRefresh = simViewRefreshTime - t1;
                System.err.println("timeSinceRefresh = " + timeSinceRefresh);
                long timeSincePublish = simViewPublishTime - t1;
                System.err.println("timeSincePublish = " + timeSincePublish);
                int simViewRefreshCount = aprsSystem.getSimViewRefreshCount();
                int simViewPublishCount = aprsSystem.getSimViewPublishCount();
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
            if (!aprsSystem.isEnableVisionToDatabaseUpdates()) {
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
            aprsSystem.refreshSimView();
            if (aprsSystem.isClosing()) {
                return Collections.emptyList();
            }
        }
        if (aprsSystem.isClosing()) {
            return Collections.emptyList();
        }
        List<PhysicalItem> l = xfl.get();
        if (l.isEmpty()) {
            logger.warning(getRunName() + ": waitForCompleteVisionUpdates returing empty list");
        }
        try {
            if (snapshotsEnabled()) {
                aprsSystem.takeSimViewSnapshot(createImageTempFile(prefix + "_waitForCompleteVisionUpdates"), l);
                takeDatabaseViewSnapshot(createImageTempFile(prefix + "_waitForCompleteVisionUpdates_new_database"));
            }
        } catch (IOException ioException) {
            logger.log(Level.SEVERE, "", ioException);
        }
        clearPoseCache();
        return l;
    }

    private void addSlowLimitedMoveUpFromCurrent(List<MiddleCommandType> out) {
        addMessageCommand(out, "addSlowLimitedMoveUpFromCurrent");
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

        private final Action parentAction;
        private final int parentActionIndex;
        private final Action action;
        private final int pddlActionIndex;
        private final int outIndex;
        @Nullable
        private CrclCommandWrapper wrapper = null;
        private final int startSafeAbortRequestCount;

        PlacePartInfo(Action action, int pddlActionIndex, int outIndex, int startSafeAbortRequestCount, Action parentAction, int parentActionIndex) {
            this.action = action;
            this.pddlActionIndex = pddlActionIndex;
            this.outIndex = outIndex;
            this.startSafeAbortRequestCount = startSafeAbortRequestCount;
            this.parentAction = parentAction;
            this.parentActionIndex = parentActionIndex;
        }

        @Nullable
        public CrclCommandWrapper getWrapper() {
            return wrapper;
        }

        void setWrapper(CrclCommandWrapper wrapper) {
            this.wrapper = wrapper;
        }

        public Action getAction() {
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

        public Action getParentAction() {
            return parentAction;
        }

        public int getParentActionIndex() {
            return parentActionIndex;
        }

        @Override
        public String toString() {
            return "PlacePartInfo{" + "parentAction=" + parentAction + ", parentActionIndex=" + parentActionIndex + ", action=" + action + ", pddlActionIndex=" + pddlActionIndex + ", outIndex=" + outIndex + ", startSafeAbortRequestCount=" + startSafeAbortRequestCount + '}';
        }

    }

    private final ConcurrentLinkedQueue<Consumer<PlacePartInfo>> placePartConsumers = new ConcurrentLinkedQueue<>();

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
    private void notifyPlacePartConsumers(PlacePartInfo ppi) {
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
    private void placePart(Action action, List<MiddleCommandType> out) throws IllegalStateException, SQLException {
        checkDbReady();
        checkSettings();
        String slotName = action.getArgs()[placePartSlotArgIndex];

        placePartBySlotName(slotName, out, action);

    }

    private volatile int startSafeAbortRequestCount;

    private void placePartBySlotName(String slotName, List<MiddleCommandType> out, Action action) throws IllegalStateException, SQLException {
        placePartBySlotName(slotName, out, action, null, -1);
    }

    private void placePartBySlotName(String slotName, List<MiddleCommandType> out, Action action, Action parentAction, int parentActionIndex) throws IllegalStateException, SQLException {
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
                    final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount, parentAction, parentActionIndex);
                    addMarkerCommand(out, msg,
                            ((CrclCommandWrapper wrapper) -> {
                                addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                                logDebug(msg + " completed at " + new Date());
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
        final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount, parentAction, parentActionIndex);
        addMarkerCommand(out, msg,
                ((CrclCommandWrapper wrapper) -> {
                    addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                    logDebug(msg + " completed at " + new Date());
                    ppi.setWrapper(wrapper);
                    notifyPlacePartConsumers(ppi);
                }));
    }

    private void recordSkipPlacePart(String slotName, @Nullable PoseType pose) throws IllegalStateException {
        takeSnapshots("plan", "skipping-place-part-" + getLastTakenPart() + "-in-" + slotName + "", pose, slotName);
//        PoseType poseCheck = getPose(slotName);
//        logDebug("poseCheck = " + poseCheck);
    }

//    private void placePartRecovery(Action action, Slot slot, List<MiddleCommandType> out) throws IllegalStateException {
//        checkDbReady();
//        checkSettings();
//        String slotName = action.getArgs()[0];
//        PoseType pose = slot.getSlotPose();
//
//        final String msg = "placed part (recovery) in " + slotName;
//        if (takeSnapshots) {
//            if (takeSnapshots) {
//                takeSnapshots("plan", "place-part-recovery-in-" + slotName + "", pose, slotName);
//            }
//        }
//        if (pose == null) {
//            throw new IllegalStateException("getPose(" + slotName + ") returned null");
//        }
//
//        pose = visionToRobotPose(pose);
//        pose.setXAxis(xAxis);
//        pose.setZAxis(zAxis);
//        PlacePartSlotPoseList.add(pose);
//        placePartByPose(out, pose);
//        final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount,null,-1);
//        addMarkerCommand(out, msg,
//                ((CrclCommandWrapper wrapper) -> {
//                    logDebug(msg + " completed at " + new Date());
//                    ppi.setWrapper(wrapper);
//                    notifyPlacePartConsumers(ppi);
//                    addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
//                }));
//    }
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

        PoseType poseWithToolOffset = CRCLPosemath.multiply(pose, toolOffsetPose);

        logToolOffsetInfo(cmds, pose, poseWithToolOffset);
        PoseType approachPose = CRCLPosemath.copy(poseWithToolOffset);
        lastTestApproachPose = null;

        //logDebug("Z= " + pose.getPoint().getZ());
        PointType approachPosePoint = requireNonNull(approachPose.getPoint(), "approachPose.getPoint()");
        PointType poseWithToolOffsetPoint = requireNonNull(poseWithToolOffset.getPoint(), "poseWithToolOffset.getPoint()");
        approachPosePoint.setZ(poseWithToolOffsetPoint.getZ() + approachZOffset);

        PoseType placePose = CRCLPosemath.copy(poseWithToolOffset);
        PointType placePosePoint = requireNonNull(placePose.getPoint(), "placePose.getPoint()");
        placePosePoint.setZ(poseWithToolOffsetPoint.getZ() + placeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false, "placePartByPose.approachPose");

//        addSettleDwell(cmds);
        addSetSlowSpeed(cmds);

        addMoveTo(cmds, placePose, true, "placePartByPose.placePose");

        addSettleDwell(cmds);

        addCheckedOpenGripper(cmds);

        addSettleDwell(cmds);
        addSetFastSpeed(cmds);
        addMoveTo(cmds, approachPose, true, "placePartByPose.approachPose.return");

//        addSettleDwell(cmds);
        this.lastTakenPart = null;
    }

    private void logToolOffsetInfo(List<MiddleCommandType> cmds, PoseType pose, PoseType poseWithToolOffset) {
        if (debug && toolOffsetPose != DEFAULT_TOOL_OFFSET_POSE) {
            addMessageCommand(cmds,
                    "pose=" + CRCLPosemath.poseToXyzRpyString(pose) + "\n"
                    + "toolOffsetPose=" + CRCLPosemath.poseToXyzRpyString(toolOffsetPose) + "\n"
                    + "poseWithToolOffset=" + CRCLPosemath.poseToXyzRpyString(poseWithToolOffset) + "\n"
            );
            System.out.println("pose=" + CRCLPosemath.poseToXyzRpyString(pose) + "\n"
                    + "toolOffsetPose=" + CRCLPosemath.poseToXyzRpyString(toolOffsetPose) + "\n"
                    + "poseWithToolOffset=" + CRCLPosemath.poseToXyzRpyString(poseWithToolOffset) + "\n");
            System.out.println("pose=" + CRCLPosemath.poseToString(pose) + "\n"
                    + "toolOffsetPose=" + CRCLPosemath.poseToString(toolOffsetPose) + "\n"
                    + "poseWithToolOffset=" + CRCLPosemath.poseToString(poseWithToolOffset) + "\n");
        }
    }

    @SuppressWarnings("SameParameterValue")
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    @Override
    public void accept(DbSetup setup) {
        this.setDbSetup(setup);

    }

    final private ConcurrentLinkedDeque<Consumer<ActionCallbackInfo>> actionCompletedListeners = new ConcurrentLinkedDeque<>();

    /**
     * Register a listener to be notified when any action is executed.
     *
     * @param listener listner to be added.
     */
    void addActionCompletedListener(Consumer<ActionCallbackInfo> listener) {
        actionCompletedListeners.add(listener);
    }

    /**
     * Remove a previously registered listener
     *
     * @param listener listener to be removed.
     */
    void removeActionCompletedListener(Consumer<ActionCallbackInfo> listener) {
        actionCompletedListeners.remove(listener);
    }

    private final AtomicReference<@Nullable ActionCallbackInfo> lastAcbi = new AtomicReference<>();

    private void notifyActionCompletedListeners(int actionIndex, Action action, CrclCommandWrapper wrapper, List<Action> actions, @Nullable List<Action> origActions) {
        ActionCallbackInfo acbi = new ActionCallbackInfo(actionIndex, action, wrapper, actions, origActions);
        lastAcbi.set(acbi);
        action.setExecTime();
        for (Consumer<ActionCallbackInfo> listener : actionCompletedListeners) {
            listener.accept(acbi);
        }
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
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

}
