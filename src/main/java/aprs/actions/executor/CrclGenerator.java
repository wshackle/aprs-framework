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

import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.OpActionType;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.actions.optaplanner.display.OpDisplayJPanel;
import aprs.database.*;
import aprs.kitinspection.KitInspectionJInternalFrame;
import aprs.misc.Utils;
import aprs.system.AprsSystem;
import crcl.base.*;
import crcl.ui.ConcurrentBlockProgramsException;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import rcs.posemath.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static aprs.actions.executor.ActionType.*;
import static aprs.actions.optaplanner.actionmodel.OpAction.allowedPartTypes;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.misc.AprsCommonLogger.println;
import static crcl.copier.CRCLCopier.copy;
import static crcl.utils.CRCLPosemath.*;
import static crcl.utils.CRCLUtils.requireNonNull;

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
        partName = partNameToPartType(partName);
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

    public static String partNameToPartType(String partName) {
        if (partName.startsWith("part_")) {
            partName = partName.substring("part_".length());
        }
        if (partName.startsWith("sku_")) {
            partName = partName.substring("sku_".length());
        }
        if (partName.startsWith("part_")) {
            partName = partName.substring("part_".length());
        }
        if (partName.length() > 3) {
            String tail = partName.substring(partName.length() - 2);
            if (tail.charAt(0) == '_' && Character.isDigit(tail.charAt(1))) {
                partName = partName.substring(0, partName.length() - 2);
            }
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
            if (null != executorActionType
                    && executorActionType != ActionType.INVALID_ACTION_TYPE
                    && executorActionType != ActionType.UNINITIALIZED) {
                Action act = new Action.ActionBuilder()
                        .type(executorActionType)
                        .args(opa.getExecutorArgs())
                        .partType(opa.getPartType())
                        .cost(opa.cost(plan))
                        .build();
                ret.add(act);
            }
        }
        return ret;
    }

    private @Nullable
    Solver<OpActionPlan> solver;

    /**
     * Get the value of solver
     *
     * @return the value of solver
     */
    public @Nullable
    Solver<OpActionPlan> getSolver() {
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
    List<OpAction> pddlActionsToOpActions(List<? extends Action> listIn, int start) throws Exception {
        return pddlActionsToOpActions(listIn, start, null, null, null);
    }

    private final AtomicInteger skippedActions = new AtomicInteger();

//    private boolean getReverseFlag() {
//        if (aprsSystem != null) {
//            return aprsSystem.isReverseFlag();
//        }
//        return false;
//    }

    private List<OpAction> pddlActionsToOpActions(
            List<? extends Action> listIn,
            int start,
            int endl@Nullable[],
            @Nullable List<OpAction> skippedOpActionsList,
            @Nullable List<Action> skippedPddlActionsList
    ) throws SQLException {
        return pddlActionsToOpActions(
                listIn,
                start,
                endl,
                skippedOpActionsList,
                skippedPddlActionsList,
                getLookForXYZ(),
                skipMissingParts);
    }

    private volatile StackTraceElement @Nullable [] setLastTakenPartTrace = null;

    public void setLastTakenPart(@Nullable String lastTakenPart) {
        this.lastTakenPart = lastTakenPart;
        setLastTakenPartTrace = Thread.currentThread().getStackTrace();
    }

//    private interface GetPoseFunction {
//
//        @Nullable
//        PoseType apply(String name, boolean ignoreNull) throws SQLException;
//    }

    private boolean inKitTrayByName(String name) {
        return !name.endsWith("_in_pt") && !name.contains("_in_pt_")
                && (name.contains("_in_kit_") || name.contains("_in_kt_") || name.endsWith("in_kt"));
    }

    private List<OpAction> pddlActionsToOpActions(
            List<? extends Action> listIn,
            int start,
            int endl@Nullable[],
            @Nullable List<OpAction> skippedOpActionsList,
            @Nullable List<Action> skippedPddlActionsList,
            @Nullable PointType lookForPt,
            boolean skipMissingParts) throws SQLException {
        List<OpAction> ret = new ArrayList<>();
        boolean moveOccurred = false;
        if (null == lookForPt) {
            throw new IllegalStateException("lookForPt == null");
        }
        ret.add(new OpAction("start", lookForPt.getX(), lookForPt.getY(), OpActionType.START, "NONE", true));
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
                    takePartOpAction = takePartPddlActionToOpAction(pa, skipMissingParts, skippedPddlActionsList, start, i);
                    takePartPddlAction = pa;
                    break;

                case TAKE_PART_BY_TYPE_AND_POSITION:
                    if (!moveOccurred) {
                        if (null != endl && endl.length > 0) {
                            endl[0] = i;
                        }
                        moveOccurred = true;
                    }
                    takePartOpAction = takePartByPosAndTypePddlActionToOpAction(pa);
                    takePartPddlAction = pa;
                    break;

                case PLACE_PART:
                    if (!moveOccurred) {
                        if (null != endl && endl.length > 0) {
                            endl[0] = i;
                        }
                        moveOccurred = true;
                    }
                    final List<OpAction> convertedTakePlaceActions
                            = opActionForTakePlaceToPddlActions(
                                    takePartOpAction,
                                    takePartPddlAction,
                                    skipMissingParts,
                                    skippedPddlActionsList,
                                    pa,
                                    start,
                                    i,
                                    skippedOpActionsList);
                    ret.addAll(convertedTakePlaceActions);
                    takePartPddlAction = null;
                    takePartOpAction = null;
                    break;

                case PLACE_PART_BY_POSITION:
                    if (!moveOccurred) {
                        if (null != endl && endl.length > 0) {
                            endl[0] = i;
                        }
                        moveOccurred = true;
                    }
                    final List<OpAction> takePlaceByPosActions
                            = opActionForTakePlaceToPddlActions(takePartOpAction, takePartPddlAction, skipMissingParts, skippedPddlActionsList, pa, start, i, skippedOpActionsList);
                    ret.addAll(takePlaceByPosActions);
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

//    private List<OpAction> opActionForTakePlaceByPosToPddlActions(OpAction takePartOpAction, Action takePartPddlAction, boolean skipMissingParts1, List<Action> skippedPddlActionsList, Action pa, int start, int i, List<OpAction> skippedOpActionsList) throws RuntimeException {
//        List<OpAction> ret = new ArrayList<>();
//        if (null == takePartOpAction
//                || null == takePartPddlAction) {
//            if (skipMissingParts1) {
//                skippedActions.incrementAndGet();
//                if (null != skippedPddlActionsList) {
//                    skippedPddlActionsList.add(pa);
//                }
//            } else {
//                throw new IllegalStateException("takePart not set :,start=" + start + ",i=" + i + ",pa=" + pa);
//            }
//        } else {
//            String xString = pa.getArgs()[0];
//            String yString = pa.getArgs()[1];
//            double x = Double.parseDouble(xString);
//            double y = Double.parseDouble(yString);
//            final String slotPartType = pa.getPartType();
//            if (null != slotPartType) {
//                if (!allowedPartTypes.contains(slotPartType)) {
//                    throw new RuntimeException("slotPartType=" + slotPartType);
//                }
//            }
//            OpAction placePartOpAction = new OpAction(pa.getType(), pa.getArgs(), x, y, slotPartType, true);
//            ret.add(takePartOpAction);
//            ret.add(placePartOpAction);
//        }
//        return ret;
//    }
    private List<OpAction> opActionForTakePlaceToPddlActions(
            @Nullable OpAction takePartOpAction,
            @Nullable Action takePartPddlAction,
            boolean skipMissingParts1,
            @Nullable List<Action> skippedPddlActionsList,
            Action pa,
            int start,
            int i,
            @Nullable List<OpAction> skippedOpActionsList) throws RuntimeException {
        List<OpAction> ret = new ArrayList<>();
        if (null == takePartOpAction
                || null == takePartPddlAction) {
            if (skipMissingParts1) {
                skippedActions.incrementAndGet();
                if (null != skippedPddlActionsList) {
                    skippedPddlActionsList.add(pa);
                }
            } else {
                throw new IllegalStateException("takePart not set :,start=" + start + ",i=" + i + ",pa=" + pa);
            }
        } else {
            String slotName = pa.getArgs()[placePartSlotArgIndex];
            PoseType slotPose = getPose(slotName);
            if (null == slotPose) {
                if (skipMissingParts1) {
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
                final String slotPartType;
                final String origPaPartType = pa.getPartType();
                if (null == origPaPartType) {
                    slotPartType = posNameToType(slotName);
                } else {
                    slotPartType = origPaPartType;
                }
                if (!allowedPartTypes.contains(slotPartType)) {
                    throw new RuntimeException("slotPartType=" + slotPartType + ", origPaPartType= " + origPaPartType);
                }
                OpAction placePartOpAction = new OpAction(pa.getType(), pa.getArgs(), slotPt.getX(), slotPt.getY(), slotPartType, inKitTrayByName(slotName));
                ret.add(takePartOpAction);
                ret.add(placePartOpAction);
            }
        }
        return ret;
    }

    private OpAction takePartByPosAndTypePddlActionToOpAction(Action pa) throws IllegalStateException, NumberFormatException {
        OpAction takePartOpAction;
        String partBtapName = pa.getArgs()[takePartArgIndex];
        String xargString = pa.getArgs()[takePartArgIndex + 1];
        String yargString = pa.getArgs()[takePartArgIndex + 2];
        final double x = Double.parseDouble(xargString);
        final double y = Double.parseDouble(yargString);
        PointType partBtapPt = getClosestPoint(partBtapName, x, y);
        if (null == partBtapPt) {
            throw new IllegalStateException("point for " + partBtapName + " is null");
        }
        takePartOpAction = new OpAction(pa.getType(), pa.getArgs(), partBtapPt.getX(), partBtapPt.getY(), posNameToType(partBtapName), true);
        return takePartOpAction;
    }

    private @Nullable
    OpAction takePartPddlActionToOpAction(Action pa, boolean skipMissingParts1, @Nullable List<Action> skippedPddlActionsList, int start, int i) throws IllegalStateException {
        String partName = pa.getArgs()[takePartArgIndex];
        PoseType partPose = getPose(partName);
//                    PoseType partPose = getPose.apply(partName, reverseFlag);
        if (null == partPose) {
            if (skipMissingParts1) {
                skippedActions.incrementAndGet();
                if (null != skippedPddlActionsList) {
                    skippedPddlActionsList.add(pa);
                }
                return null;
            } else {
                throw new IllegalStateException("null == partPose: partName=" + partName + ",start=" + start + ",i=" + i + ",pa=" + pa);
            }
        } else {
            PointType partPt = partPose.getPoint();
            if (null == partPt) {
                throw new IllegalStateException("pose for " + partName + " has null point property" + ",start=" + start + ",i=" + i + ",pa=" + pa);
            }
            return new OpAction(pa.getType(), pa.getArgs(), partPt.getX(), partPt.getY(), posNameToType(partName), inKitTrayByName(partName));
        }
    }

    private java.sql.@MonotonicNonNull Connection dbConnection;
    private @MonotonicNonNull
    DbSetup dbSetup;

    private @MonotonicNonNull
    QuerySet qs;
    private final List<String> TakenPartList = new ArrayList<>();
    private @MonotonicNonNull
    Set<Slot> EmptySlotSet;
    private final List<PoseType> PlacePartSlotPoseList = new ArrayList<>();
    private boolean takeSnapshots = false;
    private final AtomicInteger crclNumber = new AtomicInteger();
    private final ConcurrentMap<String, PoseType> poseCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PhysicalItem> itemCache = new ConcurrentHashMap<>();
    private volatile @MonotonicNonNull
    KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

    private volatile boolean pauseInsteadOfRecover;

    public @Nullable
    PhysicalItem getClosestItem(String name, double x, double y) {
        PhysicalItem closestItem = null;
        double closestDist = Double.POSITIVE_INFINITY;
        for (Entry<String, PhysicalItem> entry : itemCache.entrySet()) {
            PhysicalItem item = entry.getValue();
            if (null != item && item.getName().contains(name)) {
                double dist = Math.hypot((item.x - x), (item.y - y));
                if (dist < closestDist) {
                    closestDist = dist;
                    closestItem = item;
                }
            }
        }
        return closestItem;
    }

    public double getMinZ(double x, double y, double maxDist) {
        double minZ = Double.POSITIVE_INFINITY;
        double minAnyItemZ = Double.POSITIVE_INFINITY;
        for (Entry<String, PhysicalItem> entry : itemCache.entrySet()) {
            PhysicalItem item = entry.getValue();
            if (null != item) {
                double dist = Math.hypot((item.x - x), (item.y - y));
                if (dist < maxDist) {
                    if (item.z < minZ) {
                        minZ = item.z;
                    }
                }
                if (item.z < minAnyItemZ) {
                    minAnyItemZ = item.z;
                }
            }
        }
        for (Entry<String, PoseType> entry : poseCache.entrySet()) {
            PoseType pose = entry.getValue();
            String key = entry.getKey();
            if (null != pose) {
                final PointType point = pose.getPoint();
                if (null != point) {
                    double dist = Math.hypot((point.getX() - x), (point.getY() - y));
                    if (dist < maxDist) {
                        if (point.getZ() < minZ) {
                            minZ = point.getZ();
                        }
                    }
                    if (point.getZ() < minZ) {
                        minAnyItemZ = point.getZ();
                    }
                }
            }
        }
        if (!Double.isFinite(minZ)) {
            return minAnyItemZ;
        }
        return minZ;
    }

    public @Nullable
    PointType getClosestPoint(String name, double x, double y) {
        PoseType closestPose = null;
        double closestDist = Double.POSITIVE_INFINITY;
        if (name.startsWith("sku_part_")) {
            name = name.substring("sku_part_".length());
        }
        if (name.startsWith("part_")) {
            name = name.substring("part_".length());
        }
        if (name.startsWith("sku_")) {
            name = name.substring("sku_".length());
        }

        for (Entry<String, PoseType> entry : poseCache.entrySet()) {
            PoseType pose = entry.getValue();
            String key = entry.getKey();
            if (null != pose) {
                final PointType point = pose.getPoint();
                if (null != point && key.contains(name)) {
                    double dist = Math.hypot((point.getX() - x), (point.getY() - y));
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestPose = pose;
                    }
                }
            }
        }
        if (null == closestPose) {
            throw new RuntimeException("no match for name=\"" + name + "\" in " + poseCache.keySet());
        }
        if (closestDist > 50.0) {
            return point(x, y, closestPose.getPoint().getZ());
        }
        return closestPose.getPoint();
    }

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
        String itemHaveName;

        final private @Nullable
        String itemNeededName;

        final private @Nullable
        PhysicalItem failedClosestItem;

        final private @Nullable
        Slot failedAbsSlot;

        private final double failedClosestItemDist;

        public KitToCheckFailedItemInfo(String failedAbsSlotPrpName, String itemHaveName, @Nullable String itemNeededName, PhysicalItem failedClosestItem, Slot failedAbsSlot, double failedClosestItemDist) {
            this.failedAbsSlotPrpName = failedAbsSlotPrpName;
            this.itemHaveName = itemHaveName;
            this.failedClosestItem = failedClosestItem;
            this.failedAbsSlot = failedAbsSlot;
            this.failedClosestItemDist = failedClosestItemDist;
            this.itemNeededName = itemNeededName;
        }

        @Override
        public String toString() {
            return "KitToCheckFailedItemInfo{" + "failedAbsSlotPrpName=" + failedAbsSlotPrpName + ", itemHaveName=" + itemHaveName + ", itemNeededName=" + itemNeededName + ", failedClosestItem=" + failedClosestItem + ", failedAbsSlot=" + failedAbsSlot + ", failedClosestItemDist=" + failedClosestItemDist + '}';
        }

    };

    static private class KitToCheckInstanceInfo {

        final String instanceName;
        final Map<String, PhysicalItem> closestItemMap;
        final Map<String, String> closestItemNameMap;
        final List<Slot> absSlots;
        final Map<String, String> itemSkuMap;
        final PoseType pose;

        final List<KitToCheckFailedItemInfo> failedItems;

        int failedSlots;

        int getFailedSlots() {
            return failedSlots;
        }

        KitToCheckInstanceInfo(String instanceName, List<Slot> absSlots, PoseType pose) {
            this.instanceName = instanceName;
            this.absSlots = absSlots;
            this.pose = pose;
            closestItemMap = new HashMap<>();
            itemSkuMap = new HashMap<>();
            closestItemNameMap = new HashMap<>();
            failedItems = new ArrayList<>();
        }

        @Override
        public String toString() {
            if (null == pose) {
                return "KitToCheckInstanceInfo{" + "instanceName=" + instanceName + ", pose=null" + '}';
            }
            if (absSlots.isEmpty()) {
                return "KitToCheckInstanceInfo{" + "instanceName=" + instanceName + ", absSlots.isEmpty()" + '}';
            }
            return "KitToCheckInstanceInfo{" + "instanceName=" + instanceName + ", closestItemNameMap=" + closestItemNameMap + ", itemSkuMap=" + itemSkuMap + ", failedSlots=" + failedSlots + '}';
        }

        public void print(PrintWriter pw, String prefix) {
            pw.println(prefix + "class=" + this.getClass().getName());
            pw.println(prefix + "instanceName=" + instanceName);
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
            return diff;
        }

        public void print(PrintWriter pw, String prefix) {
            pw.println(prefix + "class=" + this.getClass().getName());
            pw.println(prefix + "name=" + name);
            if (null != kitInstanceNames) {
                saveSimpleList(pw, prefix + "kitInstanceNames", kitInstanceNames);
            }
            saveSimpleMap(pw, prefix + "slotMap", slotMap);
            saveComplexValueMap(pw, prefix + "instanceInfoMap", instanceInfoMap, KIT_TO_CHECK_INSTANCE_PRINTER);
        }

    }

    static void saveSimpleList(PrintWriter pw, String listPrefix, List<?> list) {
        pw.println(listPrefix + ".size()=" + list.size());
        for (int i = 0; i < list.size(); i++) {
            pw.println(listPrefix + ".get(" + i + ")=" + list.get(i));
        }
    }

    static <V> void saveSimpleMap(PrintWriter pw, String mapPrefix, Map<?, ?> map) {
        pw.println(mapPrefix + ".size()=" + map.size());
        for (Entry<?, ?> entry : map.entrySet()) {
            String entryPrefix = mapPrefix + ".get(" + entry.getKey() + ")";
            pw.println(entryPrefix + "=" + entry.getValue());
        }
    }

    private static interface Printer<V> {

        public void print(PrintWriter pw, String prefix, V value);
    }
    // KitToCheckInstanceInfo
    private static final Printer<KitToCheckInstanceInfo> KIT_TO_CHECK_INSTANCE_PRINTER
            = (PrintWriter pw, String prefix, KitToCheckInstanceInfo kit) -> kit.print(pw, prefix);

    private static final Printer<KitToCheck> KIT_TO_CHECK_PRINTER
            = (PrintWriter pw, String prefix, KitToCheck kit) -> kit.print(pw, prefix);

    <V> void saveComplexValueList(String label, String listPrefix, List<V> list, Printer<V> printer) throws IOException {
        final AprsSystem aprsSystem1 = requireNonNull(this.aprsSystem,"aprsSystem");
        try (PrintWriter pw = new PrintWriter(aprsSystem1.createTempFile(label, ".txt"))) {
            saveComplexValueList(pw, listPrefix, list, printer);
        }
    }

    static <V> void saveComplexValueList(PrintWriter pw, String listPrefix, List<V> list, Printer<V> printer) {
        pw.println(listPrefix + ".size()=" + list.size());
        for (int i = 0; i < list.size(); i++) {
            String entryPrefix = listPrefix + ".get(" + i + ")";
            printer.print(pw, entryPrefix, list.get(i));
        }
    }

    static <V> void saveComplexValueMap(PrintWriter pw, String mapPrefix, Map<?, V> map, Printer<V> printer) {
        pw.println(mapPrefix + ".size()=" + map.size());
        for (Entry<?, V> entry : map.entrySet()) {
            String entryPrefix = mapPrefix + ".get(" + entry.getKey() + ")";
            printer.print(pw, entryPrefix, entry.getValue());
        }
    }

    private final ConcurrentLinkedDeque<KitToCheck> kitsToCheck = new ConcurrentLinkedDeque<>();

    private @Nullable
    PartsTray correctPartsTray = null;

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
            if (null == aprsSystem) {
                return false;
            }
            if (!this.aprsSystem.isUseCsvFilesInsteadOfDatabase()) {
                if (null == dbConnection) {
                    return false;
                }
                if (dbConnection.isClosed()) {
                    return false;
                }
            }
            if (null == qs) {
                return false;
            }
            if (dbConnection != qs.getDbConnection()) {
                return false;
            }
            return qs.isConnected();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
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
            final AprsSystem aprsSystem1 = aprsSystem;
            if (null == aprsSystem1) {
                throw new NullPointerException("aprsSystem");
            }
            final DbSetup dbSetup1 = dbSetup;
            if (null == dbSetup1) {
                throw new NullPointerException("dbSetup");
            }
            if (aprsSystem1.isUseCsvFilesInsteadOfDatabase()) {
                if (qs != null) {
                    qs.close();
                }
                qs = new QuerySet(DbType.NONE, null, dbSetup1.getQueriesMap(), aprsSystem1.getTaskName(), true);
                return;
            }
            if (null != this.dbConnection && dbConnection != this.dbConnection) {
                try {
                    if (!this.dbConnection.isClosed()) {
                        this.dbConnection.close();
                    }
                    if (null != qs) {
                        qs.close();
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            }
            if (null != dbConnection) {
                this.dbConnection = dbConnection;
            }
            if (null != dbConnection && null != dbSetup1) {
                qs = new QuerySet(dbSetup1.getDbType(), dbConnection, dbSetup1.getQueriesMap(), aprsSystem1.getTaskName(), false);
            } else if (qs != null) {
                qs.close();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }

    }

    /**
     * Get the database setup object.
     *
     * @return database setup object.
     */
    public @Nullable
    DbSetup getDbSetup() {
        return dbSetup;
    }

    private boolean dbConnectionIsClosedOrNull() {
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        if (this.aprsSystem.isUseCsvFilesInsteadOfDatabase()) {
            return false;
        }
        try {
            return dbConnection == null || dbConnection.isClosed();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
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

        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        if (this.aprsSystem.isUseCsvFilesInsteadOfDatabase()) {
            try {
                if (qs != null) {
                    qs.close();
                }
                qs = new QuerySet(DbType.NONE, null, dbSetup.getQueriesMap(), aprsSystem.getTaskName(), this.aprsSystem.isUseCsvFilesInsteadOfDatabase());
            } catch (Exception ex) {
                System.err.println("dbSetup=" + dbSetup);
                System.err.println("dbSetup.getQueriesMap()=" + dbSetup.getQueriesMap());
                System.err.println("aprsSystem.getTaskName()=" + aprsSystem.getTaskName());
                throw new RuntimeException(
                        "aprsSystem.getTaskName()=" + aprsSystem.getTaskName() + ",dbSetup.getQueriesMap()=" + dbSetup.getQueriesMap(),
                        ex);
            }
            return XFutureVoid.completedFuture();
        }
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
                                    LOGGER.log(Level.SEVERE, "", ex);
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
                    LOGGER.log(Level.SEVERE, "", ex);
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
    private volatile @Nullable
    List<Action> lastActionsList = null;

    private volatile int lastAtLastIndexIdx = -1;
    private volatile @Nullable
    List<Action> lastAtLastIndexList = null;
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

    private volatile @Nullable
    Thread setLastActionsIndexThread = null;

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
            LOGGER.log(Level.FINE, "setLastActionsIndexThread changed from {0} to {1} ", new Object[]{setLastActionsIndexThread, curThread});
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

    private @Nullable
    Action getNextPlacePartAction(int lastIndex, List<Action> actions) {
        for (int i = lastIndex + 1; i < actions.size(); i++) {
            Action action = actions.get(i);
            switch (action.getType()) {
                case PLACE_PART:
                case PLACE_PART_BY_POSITION:
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
        this.endPoseTolerancesChecked = false;
        this.jointTolerancesChecked = false;
        setLastActionsIndex(null, 0);
        clearLastRequiredPartsMap();
    }

    /**
     * Set state/history/cache variables back to their initial values.
     */
    public void reset() {
        partialReset();
        setLastTakenPart(null);
        clearPoseCache();
    }

    private @MonotonicNonNull
    OpDisplayJPanel opDisplayJPanelSolution;

    /**
     * Get the value of opDisplayJPanelSolution
     *
     * @return the value of opDisplayJPanelSolution
     */
    public @Nullable
    OpDisplayJPanel getOpDisplayJPanelSolution() {
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

    private @MonotonicNonNull
    OpDisplayJPanel opDisplayJPanelInput;

    /**
     * Get the value of opDisplayJPanelInput
     *
     * @return the value of opDisplayJPanelInput
     */
    public @Nullable
    OpDisplayJPanel getOpDisplayJPanelInput() {
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
     * @throws Exception with cause: IllegalStateException if database not
     * connected SQLException if query of the database failed
     * java.lang.InterruptedException another thread called Thread.interrupt()
     * while retrieving data from database
     * java.util.concurrent.ExecutionException exception occurred in another
     * thread servicing the waitForCompleteVisionUpdates
     * crcl.utils.CRCLException a failure occurred while composing or sending a
     * CRCL command. rcs.posemath.PmException failure occurred while computing a
     * pose such as an invalid transform
     */
    public List<MiddleCommandType> generate(List<Action> actions, int startingIndex, Map<String, String> options, int startSafeAbortRequestCount)
            throws Exception {
        final AprsSystem localAprsSystem = requireNonNull(this.aprsSystem,"aprsSystem");
        GenerateParams gparams = new GenerateParams();
        gparams.actions = actions;
        gparams.startingIndex = startingIndex;
        gparams.options = options;
        gparams.startSafeAbortRequestCount = startSafeAbortRequestCount;
        gparams.replan = !localAprsSystem.isCorrectionMode() && !lastProgramAborted;
        generateCount.incrementAndGet();
        if (gparams.startingIndex == 0) {
            generateFromZeroCount.incrementAndGet();
            generateSinceZeroCount.set(0);
        } else {
            generateSinceZeroCount.incrementAndGet();
        }
        final Thread curThread = Thread.currentThread();
        if (null == genThread) {
            genThread = curThread;
            genThreadSetTrace = curThread.getStackTrace();
        } else if (genThread != curThread) {
            logError("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
            throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
        }
        List<MiddleCommandType> cmds = privateGenerate(gparams);
        this.lastProgramAborted = false;
        return cmds;
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

    private volatile @Nullable
    Thread genThread = null;
    private volatile StackTraceElement genThreadSetTrace@Nullable []  = null;

    private static boolean cmdsContainNonWrapper(List<MiddleCommandType> cmds) {
        for (MiddleCommandType cmd : cmds) {
            if (cmd instanceof SetLengthUnitsType) {
                continue;
            }
            if (cmd instanceof SetAngleUnitsType) {
                continue;
            }
            if (!(cmd instanceof CRCLCommandWrapper)) {
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
            if (!(cmd instanceof CRCLCommandWrapper)) {
                throw new IllegalArgumentException("list contains non wrapper commands");
            }
            CRCLCommandWrapper wrapper = (CRCLCommandWrapper) cmd;
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
    class GenerateParams {

        List<Action> actions = Collections.emptyList();
        int startingIndex;
        Map<String, String> options = Collections.emptyMap();
        int startSafeAbortRequestCount;
        boolean replan;
        boolean optiplannerUsed;

        final List<Action> origActions = Collections.emptyList();

//        @MonotonicNonNull
//        List<PhysicalItem> newItems;
        @Nullable
        RunOptoToGenerateReturn runOptoToGenerateReturn;

        boolean newItemsReceived;
        final int startingVisionUpdateCount;

        private GenerateParams() {
            this.startingVisionUpdateCount = visionUpdateCount.get();
            int ssarc0;
            final AprsSystem aprsSystemFinal = aprsSystem;
            if (null != aprsSystemFinal) {
                ssarc0 = aprsSystemFinal.getSafeAbortRequestCount();
            } else {
                ssarc0 = 0;
            }
            this.startSafeAbortRequestCount = ssarc0;
        }

        @Override
        public String toString() {
            return "GenerateParams{" + "actions.size()=" + ((null != actions) ? actions.size() : -1) + ", startingIndex=" + startingIndex + ",\n options=" + options + ",\n startSafeAbortRequestCount=" + startSafeAbortRequestCount + ",\n  replan=" + replan + ", optiplannerUsed=" + optiplannerUsed + ",\n origActions=" + origActions + ",\n runOptoToGenerateReturn=" + runOptoToGenerateReturn + ",\n newItemsReceived=" + newItemsReceived + ",\n startingVisionUpdateCount=" + startingVisionUpdateCount + '}';
        }

    }

    GenerateParams newGenerateParams() {
        return new GenerateParams();
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

    private volatile @MonotonicNonNull
    List<List<Action>> takePlaceActions = null;

    private final AtomicInteger generateCount = new AtomicInteger();
    private final AtomicInteger generateFromZeroCount = new AtomicInteger();
    private final AtomicInteger generateSinceZeroCount = new AtomicInteger();

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
    private synchronized List<MiddleCommandType> privateGenerate(GenerateParams gparams)
            throws Exception {

        final List<PhysicalItem> physicalItemsFinal = this.physicalItems;
        boolean disableOptimization = false;
        final List<Action> gparamsActions = gparams.actions;
        if (null != gparamsActions) {
            for (Action act : gparamsActions) {
                if (act.getType() == ActionType.DISABLE_OPTIMIZATION) {
                    gparams.replan = false;
                    disableOptimization = true;
                    break;
                }
            }
        }
        if (null != solver
                && !disableOptimization
                && gparams.replan
                && null != physicalItemsFinal
                && null != gparamsActions
                && gparamsActions.size() > gparams.startingIndex + 1) {
            return runOptaPlanner(gparamsActions,
                    gparams.startingIndex,
                    gparams.options,
                    gparams.startSafeAbortRequestCount,
                    physicalItemsFinal);
        }
        return privateGenerateNoReplan(
                gparams,
                physicalItemsFinal);
    }

    private List<MiddleCommandType> privateGenerateNoReplan(
            GenerateParams gparams,
            @Nullable List<PhysicalItem> physicalItemsParam) throws ConcurrentBlockProgramsException,  IllegalStateException, IllegalArgumentException {
        final Map<String, String> gparamsOptionsLocalCopy = gparams.options;
        if (null == gparamsOptionsLocalCopy) {
            throw new NullPointerException("null == gparams.options");
        }
        final int startingIndex = gparams.startingIndex;
        final List<Action> gParamsActions = gparams.actions;
        if (null == gParamsActions) {
            throw new NullPointerException("null == gparams.actions");
        }

        AprsSystem localAprsSystem = this.aprsSystem;
        if (null == localAprsSystem) {
            throw new NullPointerException("aprsJframe is null");
        }
        if (gParamsActions.isEmpty()) {
            throw new IllegalArgumentException("gparams.actions.isEmpty()");
        }
        if (gParamsActions.size() <= gparams.startingIndex) {
            throw new IllegalArgumentException("gparams.actions.size()=" + gParamsActions.size() + ", gparams.startingIndex=" + gparams.startingIndex);
        }
        int blockingCount = localAprsSystem.startBlockingCrclPrograms();
        List<MiddleCommandType> cmds = new ArrayList<>();

        Action startingAction = gParamsActions.get(startingIndex);

        try {
            localAprsSystem.logEvent("CrclGenerator.generate: gparams", gparams);
            final int gparamsActionsSize = gParamsActions.size();

            String sublistString;
            if (!gParamsActions.isEmpty()
                    && startingIndex < gparamsActionsSize) {
                println("startingIndex = " + startingIndex);
                println("gparamsActionsSize = " + gparamsActionsSize);
                final List<Action> subList = gParamsActions.subList(startingIndex, gparamsActionsSize);
                sublistString = "gparams.actions.subList(gparams.startingIndex, gparams.actions.size())=" + subList;
            } else {
                sublistString = "";
            }
            String messageString = "\n"
                    + "gparams.startingIndex=" + startingIndex + "\n"
                    + "gparams.replan=" + gparams.replan + "\n"
                    + "gparams.actions.size()=" + gparamsActionsSize + "\n"
                    + "gparams.actions=" + gParamsActions + "\n"
                    + sublistString
                    + "\n";
            addMessageCommand(cmds, messageString);
            this.startSafeAbortRequestCount = gparams.startSafeAbortRequestCount;
            checkDbReady();
            if (localAprsSystem.isRunningCrclProgram()) {
                System.err.println("sys.getLastRunningProgramTrueInfo=" + localAprsSystem.getLastRunningProgramTrueInfo());
                throw new IllegalStateException("already running crcl while trying to generate it");
            }

            ActionCallbackInfo acbi = lastAcbi.get();
            if (null != acbi && includeEndProgramMarker && includeSkipNotifyMarkers) {
                int actionIndex = acbi.getActionIndex();
                if (startingIndex < actionIndex) {
                    if (startingIndex != 0 || actionIndex < acbi.getActionsSize() - 2) {
                        logDebug("Thread.currentThread() = " + Thread.currentThread());
                        List<Action> actions = acbi.getActions();
                        boolean actionsChanged = diffActions(gParamsActions, actions);
                        logDebug("actionsChanged = " + actionsChanged);
                        String errString = "generate called with startingIndex=" + startingIndex + ",acbi.getActionsSize()=" + acbi.getActionsSize() + " and acbi.actionIndex=" + actionIndex + ", lastIndex=" + lastIndex + ", acbi.action.=" + acbi.getAction();
                        System.err.println(errString);
                        System.err.println("acbi = " + acbi.getAction());
                        localAprsSystem.setTitleErrorString(errString);
                        throw new IllegalStateException(errString);
                    }
                }
            }

            setOptions(gparamsOptionsLocalCopy);
            final int currentCrclNumber = this.crclNumber.incrementAndGet();

            if (null == actionToCrclIndexes || actionToCrclIndexes.length != gparamsActionsSize) {
                actionToCrclIndexes = new int[gparamsActionsSize];
            }
            for (int i = startingIndex; i < actionToCrclIndexes.length; i++) {
                actionToCrclIndexes[i] = -1;
            }
            if (null == actionToCrclLabels || actionToCrclLabels.length != gparamsActionsSize) {
                actionToCrclLabels = new String[gparamsActionsSize];
            }
            for (int i = startingIndex; i < actionToCrclLabels.length; i++) {
                actionToCrclLabels[i] = "UNDEFINED";
            }
            if (null == actionToCrclTakenPartsNames || actionToCrclTakenPartsNames.length != gparamsActionsSize) {
                actionToCrclTakenPartsNames = new String[gparamsActionsSize];
            }
            if (startingIndex == 0) {
                if (!manualAction) {
                    setLastTakenPart(null);
                }
                takePlaceActions = new ArrayList<>();
            }
            List<Action> newTakePlaceList = new ArrayList<>();
            if (null != takePlaceActions) {
                takePlaceActions.add(newTakePlaceList);
            }
            addSetUnits(cmds);

            String waitForCompleteVisionUpdatesCommentString = "generate(start=" + startingIndex + ",crclNumber=" + currentCrclNumber + ")";
            boolean isNewRetArray[] = new boolean[1];
            updateStalePoseCache(startingIndex, acbi, waitForCompleteVisionUpdatesCommentString, isNewRetArray, gparams.startSafeAbortRequestCount);
            if (isNewRetArray[0]) {
                gparams.newItemsReceived = true;
            }
//            if (aprsSystem.isEndLogged()) {
//                throw new IllegalStateException("aprsSystem.isEndLogged()");
//            }
            takeSnapshots("plan", "generate(start=" + startingIndex + ",crclNumber=" + currentCrclNumber + ")", null, null);
            final List<Action> fixedActionsCopy = Collections.unmodifiableList(new ArrayList<>(gParamsActions));
            final List<Action> fixedOrigActionsCopy = (gparams.origActions == null) ? null : Collections.unmodifiableList(new ArrayList<>(gParamsActions));

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
            Action lastAction = null;
            if (null != physicalItemsParam) {
                takeSimViewSnapshot("generate.startingIndex=" + startingIndex, physicalItemsParam);
            }

            for (this.setLastActionsIndex(gParamsActions, startingIndex); getLastIndex() < gparamsActionsSize; incLastActionsIndex()) {

                final int idx = getLastIndex();
                Action action = gParamsActions.get(idx);
                if (debug) {
                    logDebug("action[" + idx + "] = " + action);
                }
                if (skipMissingParts) {
                    boolean needSkip = false;
                    switch (action.getType()) {
                        case TAKE_PART:
                            if (poseCache.isEmpty()) {
                                System.err.println("physicalItems = " + physicalItemsParam);
                                System.err.println("clearPoseCacheTrace = " + Utils.traceToString(clearPoseCacheTrace));
                                throw new IllegalStateException("TAKE_PART when poseCache is empty");
                            }
                            String partName = action.getArgs()[takePartArgIndex];
                            PoseType pose = getPose(partName);
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

                        case TAKE_PART_BY_TYPE_AND_POSITION:
                            if (poseCache.isEmpty()) {
                                System.err.println("physicalItems = " + physicalItemsParam);
                                System.err.println("clearPoseCacheTrace = " + Utils.traceToString(clearPoseCacheTrace));
                                throw new IllegalStateException("TAKE_PART when poseCache is empty");
                            }
                            String partBtapName = action.getArgs()[takePartArgIndex];
                            String xargString = action.getArgs()[takePartArgIndex + 1];
                            String yargString = action.getArgs()[takePartArgIndex + 2];

                            PointType partBtapPoint = getClosestPoint(partBtapName, Double.parseDouble(xargString), Double.parseDouble(yargString));
                            if (null == partBtapPoint) {
                                throw new IllegalStateException("point for " + partBtapName + " is null");
                            } else {
                                foundParts.add(partBtapName);
                                skipStartIndex = -1;
                                needSkip = false;
                            }
                            break;

                        case PLACE_PART:
                            if (poseCache.isEmpty()) {
                                LOGGER.log(Level.WARNING, "newItems.isEmpty() on place-part for run {0}", getRunName());
                            }
                            String slotName = action.getArgs()[placePartSlotArgIndex];
                            if (null == lastTakenPart) {
                                PoseType slotPose = getPose(slotName);//getPose(slotName, getReverseFlag());
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
                        if (poseCache.isEmpty()) {
                            LOGGER.log(Level.WARNING, "newItems.isEmpty() on take-part for run {0}", getRunName());
                        }
                        takePart(action, cmds, getNextPlacePartAction(idx, gParamsActions));
                        break;

                    case TAKE_PART_BY_TYPE_AND_POSITION: {
                        newTakePlaceList.add(action);
                        if (poseCache.isEmpty()) {
                            LOGGER.log(Level.WARNING, "newItems.isEmpty() on take-part for run {0}", getRunName());
                        }
                        String partBtapName = action.getArgs()[takePartArgIndex];
                        String xargString = action.getArgs()[takePartArgIndex + 1];
                        String yargString = action.getArgs()[takePartArgIndex + 2];
                        final double x = Double.parseDouble(xargString);
                        final double y = Double.parseDouble(yargString);
                        PointType partBtapPoint = requireNonNull(getClosestPoint(partBtapName, x, y), "partBtapPoint");
                        final PhysicalItem closestItem = getClosestItem(partBtapName, x, y);
                        if (null != closestItem) {
                            lastTakenPart = closestItem.getFullName();
                        } else {
                            lastTakenPart = partBtapName;
                        }
                        takePartByPose(cmds, visionToRobotPose(pose(partBtapPoint, xAxis, zAxis)), partBtapName);
                    }
                    break;

                    case FAKE_TAKE_PART:
                        fakeTakePart(action, cmds);
                        break;

                    case TEST_PART_POSITION:
                        testPartPosition(action, cmds);
                        break;
                    case LOOK_FOR_PARTS:
                        lookForParts(action, cmds, (idx < 2),
                                doInspectKit ? (idx == gparamsActionsSize - 1) : (idx >= gparamsActionsSize - 2),
                                gparams.startSafeAbortRequestCount
                        );
                        List<MiddleCommandType> ret = endSection(
                                action,
                                idx,
                                cmds,
                                fixedActionsCopy,
                                fixedOrigActionsCopy,
                                skippedParts,
                                foundParts,
                                gparams,
                                waitForCompleteVisionUpdatesCommentString);
                        if (null != ret) {
                            return ret;
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
                        if (poseCache.isEmpty()) {
                            throw new IllegalStateException("poseCache.isEmpty() on place-part for run " + getRunName());
                        }
                        if (null == lastTakenPart) {
                            System.err.println("newTakePlaceList = " + newTakePlaceList);
                            throw new IllegalStateException("null == lastTakenPart when PLACE_PART encountered: idx=" + idx + ",gparams.startingIndex=" + startingIndex + ",lastAction=" + lastAction);
                        }
                        placePart(action, cmds);
                        break;

                    case PLACE_PART_BY_POSITION: {
                        newTakePlaceList.add(action);
                        String xargString = action.getArgs()[0];
                        String yargString = action.getArgs()[1];
                        double x = Double.parseDouble(xargString);
                        double y = Double.parseDouble(yargString);
                        placePartByPose(cmds, pose(point(x, y, getMinZ(x, y, 25.0)), xAxis, zAxis));
                    }
                    break;

                    case PAUSE:
                        pause(action, cmds);
                        break;

                    case CLEAR_KITS_TO_CHECK:
                        localAprsSystem.logEvent("CLEAR_KITS_TO_CHECK startingIndex=" + startingIndex + ",idx=" + idx, action);
                        clearKitsToCheck(action, cmds, gparams);
                        break;

                    case ADD_KIT_TO_CHECK:
                        localAprsSystem.logEvent("ADD_KIT_TO_CHECK startingIndex=" + startingIndex + ",idx=" + idx, action);
                        addKitToCheck(action, cmds, gparams);
                        localAprsSystem.logEvent("ADD_KIT_TO_CHECK kitsToCheck.size()=", kitsToCheck.size());
                        break;

                    case SET_CORRECTION_MODE:
                        localAprsSystem.setCorrectionMode(Boolean.parseBoolean(action.getArgs()[0]));
                        break;

                    case CHECK_KITS:
                        boolean lookForPartsNeeded = true;
                        int lastLookForIndex = startingIndex > 0 ? startingIndex - 1 : 0;
                        if (null == acbi) {
                            throw new NullPointerException("acbi == null");
                        }
                        if (acbi.getAction().getType() == LOOK_FOR_PARTS) {
                            lookForPartsNeeded = false;
                        }
                        for (int i = startingIndex; i < idx; i++) {
                            if (gParamsActions.get(i).getType() == LOOK_FOR_PARTS) {
                                lastLookForIndex = i;
                                lookForPartsNeeded = false;
                            }
                        }
                        final int finalLookForIndex = lastLookForIndex;
                        if (lookForPartsNeeded) {
                            System.err.println("gParamsActions = " + gParamsActions);
                            System.err.println("gParamsActions.get(lastCheckKitsLookForIndex) = " + gParamsActions.get(lastCheckKitsLookForIndex));
                            System.err.println("lastCheckKitsLookForIndex = " + lastCheckKitsLookForIndex);
                            System.err.println("lastCheckKitsLookForIndexAction = " + lastCheckKitsLookForIndexAction);
                            System.err.println("gparams = " + gparams);
                            System.err.println("acbi = " + acbi);
                            System.err.println("startingIndex=" + startingIndex);
                            System.err.println("startingAction=" + startingAction);
                            throw new RuntimeException("lookForPartsNeeded gparams=" + gparams.toString() + ", acbi=" + acbi);

                        } else {
                            final Action lookForIndexAction = gParamsActions.get(finalLookForIndex);
                            lastCheckKitsLookForIndexAction = lookForIndexAction;
                            boolean correctionDone = checkKits(action, cmds, idx, finalLookForIndex, gparams);
                            kitsToCheck.clear();
                            if (correctionDone) {
                                addNotifyMarker(cmds, "checkKitsNeedCorrection", idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                                lookForParts(lookForIndexAction, cmds, false,
                                        false,
                                        gparams.startSafeAbortRequestCount
                                );
                                addNotifyMarker(cmds, "checkKitsNeedCorrection.repeatLookForParts", finalLookForIndex, lookForIndexAction, fixedActionsCopy, fixedOrigActionsCopy);
                                setLastActionsIndex(gParamsActions, finalLookForIndex);
                                return cmds;
                            }
                        }
                        break;
                    case DISABLE_OPTIMIZATION:
                        break;

                    default:
                        throw new IllegalArgumentException("unrecognized action " + action + " at index " + idx);
                }
                lastAction = action;
                updateActionToCrclArrays(idx, cmds);
                action.setPlanTime();
                String end_action_string = "end_" + idx + "_" + action.getType() + "_" + Arrays.toString(action.getArgs());
                addNotifyMarker(cmds, end_action_string, idx, action, fixedActionsCopy, fixedOrigActionsCopy);
                addTakeSnapshots(cmds, end_action_string, null, null, this.crclNumber.get());
                if (!skippedParts.isEmpty()) {
                    logDebug("foundParts = " + foundParts);
                    logDebug("skippedParts = " + skippedParts);
                    logDebug("poseCache.keySet() = " + poseCache.keySet());
                }
            }
            if (localAprsSystem.isRunningCrclProgram()) {
                System.err.println("sys.getLastRunningProgramTrueInfo=" + localAprsSystem.getLastRunningProgramTrueInfo());
                throw new IllegalStateException("already running crcl while trying to generate it");
            }
        } catch (Exception ex) {

            localAprsSystem.logEvent("ex", ex);
            System.err.println("gparams=" + gparams);
            System.err.println("startingIndex=" + startingIndex);
            System.err.println("startingAction=" + startingAction);
            System.err.println("getRunName() = " + getRunName());
            System.err.println("poseCache.keySet() = " + poseCache.keySet());
            List<PhysicalItem> physicalItemsLocal = physicalItemsParam;
            if (null != physicalItemsLocal) {
                System.err.println("physicalItems.size()=" + physicalItemsLocal.size());
                System.err.println("physicalItems=" + physicalItemsLocal);
                for (PhysicalItem newItem : physicalItemsLocal) {
                    System.err.println(newItem.getName() + " : " + newItem.x + "," + newItem.y);
                }
            }
            LOGGER.log(Level.SEVERE, "", ex);
            System.err.println();
            localAprsSystem.setTitleErrorString(ex.getMessage());
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

    private @Nullable
    List<MiddleCommandType> endSection(Action action,
            int idx,
            List<MiddleCommandType> cmds,
            List<Action> fixedActionsCopy,
            @Nullable List<Action> fixedOrigActionsCopy,
            List<String> skippedParts,
            List<String> foundParts,
            GenerateParams gparams,
            String waitForCompleteVisionUpdatesCommentString) throws Exception {
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
                    LOGGER.log(Level.SEVERE, "action=" + action + ",idx=" + idx + ",cmds=" + cmds, thrown);
                    throw thrown;
                }
            }
            if (!gparams.newItemsReceived && visionUpdateCount.get() == gparams.startingVisionUpdateCount) {
                checkNewItems(waitForCompleteVisionUpdatesCommentString, gparams.startSafeAbortRequestCount);
            }
            LOGGER.log(Level.FINE, "Processed wrapper only commands without sending to robot.");
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
        return null;
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
                (CRCLCommandWrapper wrapper) -> {
                    notifyActionCompletedListeners(idx, end_action_string, action, wrapper, fixedActionsCopy, fixedOrigActionsCopy);
                });
    }

    private volatile boolean lastProgramAborted = false;

    public boolean isLastProgramAborted() {
        return lastProgramAborted;
    }

    public void setLastProgramAborted(boolean lastProgramAborted) {
        this.lastProgramAborted = lastProgramAborted;
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

    private void updateStalePoseCache(
            int startingIndex,
            @Nullable ActionCallbackInfo acbi,
            String label,
            boolean isNewRetArray[],
            int startAbortCount)
            throws Exception {

        if (startingIndex > 0 && null != acbi) {
            Action acbiAction = acbi.getAction();
            if (null != acbiAction) {
                switch (acbiAction.getType()) {

                    case LOOK_FOR_PARTS:
                        if (null == physicalItems) {
                            checkNewItems(label, startAbortCount);
                            if (null != isNewRetArray && isNewRetArray.length == 1) {
                                isNewRetArray[0] = true;
                            }
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private void loadNewItemsIntoPoseCache(List<PhysicalItem> newItems) {
        synchronized (poseCache) {
            for (PhysicalItem item : newItems) {
                String fullName = item.getFullName();
                if (null != fullName) {
                    poseCache.put(fullName, item.getPose());
                    itemCache.put(fullName, item);
                }
            }
        }
    }

    private @Nullable
    PoseType getPose(String fullName, List<PhysicalItem> localPhysicalItems) {
        synchronized (poseCache) {
            for (PhysicalItem item : localPhysicalItems) {
                String itemFullName = item.getFullName();
                if (fullName.equals(itemFullName)) {
                    return item.getPose();
                }
            }
        }
        return null;
    }

    public @Nullable
    List<PhysicalItem> getPhysicalItems() {
        return physicalItems;
    }

    public List<PhysicalItem> checkNewItems(String label, int startAbortCount) throws InterruptedException, ExecutionException {
        if (null == externalPoseProvider) {
            return waitForCompleteVisionUpdates(label, lastRequiredPartsMap, WAIT_FOR_VISION_TIMEOUT, startAbortCount);
        } else {
            List<PhysicalItem> newPhysicalItems = externalPoseProvider.getNewPhysicalItems();
            this.physicalItems = newPhysicalItems;
            takeSimViewSnapshot("crclGenerator.externalPoseProvider.getNewPhysicalItems", newPhysicalItems);
            loadNewItemsIntoPoseCache(newPhysicalItems);
            return newPhysicalItems;
        }
    }

    public static final long WAIT_FOR_VISION_TIMEOUT
            = getLongProperty("aprs.waitForVisionTimeout", 15_000);

//    private static double getDoubleProperty(String propName, double defaultValue) {
//        try {
//            String propValueString = System.getProperty(propName);
//            if (null != propValueString && propValueString.length() > 0) {
//                return Double.parseDouble(propValueString);
//            } else {
//                return defaultValue;
//            }
//        } catch (Exception exception) {
//            LOGGER.log(Level.SEVERE, "", exception);
//            return defaultValue;
//        }
//    }

    private static long getLongProperty(String propName, long defaultValue) {
        try {
            String propValueString = System.getProperty(propName);
            if (null != propValueString && propValueString.length() > 0) {
                return Long.parseLong(propValueString);
            } else {
                return defaultValue;
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "", exception);
            return defaultValue;
        }
    }

    private static final AtomicInteger RUN_OPTO_COUNT = new AtomicInteger();

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
            case PLACE_PART_BY_POSITION:
            case TAKE_PART:
            case TAKE_PART_BY_TYPE_AND_POSITION:
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
            int startSafeAbortRequestCount1,
            List<PhysicalItem> physicalItemsLocal)
            throws Exception {
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
            List<MiddleCommandType> l = privateGenerateNoReplan(gparams, physicalItemsLocal);
            if (!l.isEmpty()
                    || null == gparams.runOptoToGenerateReturn
                    || gparams.runOptoToGenerateReturn.newIndex <= startingIndex) {
                return l;
            }
            startingIndex = gparams.runOptoToGenerateReturn.newIndex;
            gparams.startingIndex = startingIndex;
        }
        gparams.runOptoToGenerateReturn = null;
        int rc = RUN_OPTO_COUNT.incrementAndGet();
        long t0 = System.currentTimeMillis();
        setOptions(options1);
        if (actions.size() < 1) {
            throw new IllegalArgumentException("actions.size() = " + actions.size() + ",rc=" + rc);
        }
        List<PhysicalItem> newItems = null;

        ActionCallbackInfo acbi = lastAcbi.get();
        String waitForCompleteVisionUpdatesCommentString = "runOptaPlanner(start=" + startingIndex + ",crclNumber=" + crclNumber + ")";
        boolean isNewRetArray[] = new boolean[1];
        updateStalePoseCache(startingIndex, acbi, waitForCompleteVisionUpdatesCommentString, isNewRetArray, gparams.startSafeAbortRequestCount);
        if (isNewRetArray[0]) {
            gparams.newItemsReceived = true;
        }
        List<Action> origActions = new ArrayList<>(actions);
        List<Action> fullReplanPddlActions = optimizePddlActionsWithOptaPlanner(actions, startingIndex, physicalItemsLocal);
        int skippedActionsCount = skippedActions.get();
        if (Math.abs(fullReplanPddlActions.size() - actions.size()) > skippedActionsCount || fullReplanPddlActions.size() < 1) {
            throw new IllegalStateException("fullReplanPddlActions.size() = " + fullReplanPddlActions.size() + ",actions.size() = " + actions.size() + ",rc=" + rc + ", skippedActions=" + skippedActionsCount);
        }
        if (fullReplanPddlActions == actions) {
            gparams.replan = false;
            return privateGenerateNoReplan(gparams, physicalItemsLocal);
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

        List<MiddleCommandType> newCmds = privateGenerateNoReplan(gparams, physicalItemsLocal);
        addMessageCommand(newCmds, messageString);
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
            LOGGER.log(Level.SEVERE, "", ex);
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
            LOGGER.log(Level.SEVERE, "", ex);
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

    private volatile int lastgfzc = -1;

    public static class OptiplannerLogEntry {

        private final int index;
        private final int startingIndex;
        private final String runName;
        private final boolean reverse;
        private final int actionsInSize;
        private final int actionsOutSize;
        private final long solveTime;
        private final @Nullable
        File actionsInFile;
        private final @Nullable
        File actionsOutFile;
        private final @Nullable
        File itemsImageFile;
        private final @Nullable
        File itemsCsvFile;
        private final @Nullable
        File object2DLogLinesFile;
        private final @Nullable
        File object2DPropertiesFile;
        private final @Nullable
        File inputOpPlanFile;
        private final @Nullable
        File outputOpPlanFile;
        private final double inScore;
        private final double outScore;

        public OptiplannerLogEntry(int index, int startingIndex, String runName, boolean reverse, int actionsInSize, int actionsOutSize, long solveTime, @Nullable File actionsInFile, @Nullable File actionsOutFile, @Nullable File itemsImageFile, @Nullable File itemsCsvFile, @Nullable File object2DLogLinesFile, @Nullable File object2DPropertiesFile, @Nullable File inputOpPlanFile, @Nullable File outputOpPlanFile, double inScore, double outScore) {
            this.index = index;
            this.startingIndex = startingIndex;
            this.runName = runName;
            this.reverse = reverse;
            this.actionsInSize = actionsInSize;
            this.actionsOutSize = actionsOutSize;
            this.solveTime = solveTime;
            this.actionsInFile = actionsInFile;
            this.actionsOutFile = actionsOutFile;
            this.itemsImageFile = itemsImageFile;
            this.itemsCsvFile = itemsCsvFile;
            this.object2DLogLinesFile = object2DLogLinesFile;
            this.object2DPropertiesFile = object2DPropertiesFile;
            this.inputOpPlanFile = inputOpPlanFile;
            this.outputOpPlanFile = outputOpPlanFile;
            this.inScore = inScore;
            this.outScore = outScore;
        }

        public @Nullable
        Object[] toArray() {
            return new Object[]{
                index, startingIndex, runName, reverse,
                inScore, outScore,
                actionsInSize, actionsOutSize,
                solveTime,
                actionsInFile, actionsOutFile,
                itemsImageFile, itemsCsvFile,
                object2DLogLinesFile, object2DPropertiesFile,
                inputOpPlanFile, outputOpPlanFile
            };
        }

        public static final String[] HEADERS
                = new String[]{
                    "index", "startingIndex", "run", "reverse",
                    "inScore", "outScore",
                    "sizeIn", "sizeOut",
                    "solveTime",
                    "actionsIn", "actionsOut",
                    "itemsImage", "itemsCsv",
                    "object2DLogLines", "object2DProperties",
                    "inputPlan", "outputPlan"
                };
    }

    private final List<OptiplannerLogEntry> optiplannerLogEntrys = new ArrayList<>();

    public synchronized List<OptiplannerLogEntry> getOptiplannerLogEntrys() {
        return new ArrayList<>(optiplannerLogEntrys);
    }

    private volatile @Nullable
    File logOptaPlannerResultsFile = null;

    @SuppressWarnings("nullness")
    private synchronized void logOptaPlannerResult(
            int solveCount,
            int startingIndex,
            int sizeIn,
            int sizeOut,
            long solveTime,
            File actionsInFile,
            File actionsOutFile,
            @Nullable File[] snapsFiles,
            double inScore,
            double outScore,
            OpActionPlan inPlan,
            OpActionPlan outPlan) {
        try {
            if (null == aprsSystem) {
                return;
            }
            String runName = aprsSystem.getRunName();
            File inPlanFile = aprsSystem.createTempFile("inPlan", ".csv");
            inPlan.saveActionList(inPlanFile);
            File outPlanFile = aprsSystem.createTempFile("outPlan", ".csv");
            outPlan.saveActionList(outPlanFile);
            final File resultsFile;
            final boolean newFile;
            if (null == logOptaPlannerResultsFile) {
                resultsFile = Utils.createTempFile("OptaPlannerResults" + aprsSystem.getTaskName(), ".csv");
                this.logOptaPlannerResultsFile = resultsFile;
                newFile = true;
            } else {
                if (optiplannerLogEntrys.isEmpty()) {
                    resultsFile = Utils.createTempFile("OptaPlannerResults" + aprsSystem.getTaskName(), ".csv");
                    this.logOptaPlannerResultsFile = resultsFile;
                    newFile = true;
                } else {
                    resultsFile = logOptaPlannerResultsFile;
                    newFile = false;
                }
            }
            final File logLinesFile = aprsSystem.getObject2DViewLogLinesFile();
            if (null == logLinesFile) {
                return;
            }
            final File propertiesFile = aprsSystem.getObject2DViewPropertiesFile();
            if (null == propertiesFile) {
                return;
            }
            final OptiplannerLogEntry logEntry
                    = new OptiplannerLogEntry(
                            solveCount, startingIndex, runName, aprsSystem.isReverseFlag(),
                            sizeIn, sizeOut,
                            solveTime,
                            actionsInFile, actionsOutFile,
                            snapsFiles[0], snapsFiles[1],
                            logLinesFile, propertiesFile,
                            inPlanFile, outPlanFile,
                            inScore, outScore);
            CSVFormat format = CSVFormat.DEFAULT;
            if (newFile) {
                format = format.withHeader(OptiplannerLogEntry.HEADERS);
            }
            final List<Object> logEntryList = Arrays.asList(logEntry.toArray());
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(resultsFile, !newFile), format)) {
                printer.printRecord(logEntryList);
            }
            optiplannerLogEntrys.add(logEntry);
            aprsSystem.logEvent("logOptaPlannerResult", "solveCount=" + solveCount + ",solveTime=" + solveTime + ",inScore=" + inScore + ", outScore=" + outScore + ", logOptaPlannerResultsFile=" + logOptaPlannerResultsFile);
        } catch (IOException ex) {
            Logger.getLogger(CrclGenerator.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<Action> optimizePddlActionsWithOptaPlanner(
            List<Action> actions,
            int startingIndex,
            List<PhysicalItem> physicalItemsLocal) {
        final AprsSystem aprsSystemLocal = requireNonNull(this.aprsSystem,"aprsSystem");
        if (null == physicalItemsLocal) {
            throw new IllegalArgumentException("physicalItemsLocal=" + physicalItemsLocal);
        }

        try {

            Solver<OpActionPlan> solverToRun = this.solver;
            if (null == solverToRun) {
                return actions;
            }
            @Nullable
            File[] takeSnapsPhysicalItemsFiles
                    = takeSimViewSnapshot(
                            "optimizePddlActionsWithOptaPlanner.physicalItemsLocal",
                            physicalItemsLocal);

            File actionsInFile = aprsSystemLocal.createTempFile("actionsIn", ".txt");
            aprsSystemLocal.logEvent("optimizePddlActionsWithOptaPlanner", "takeSnapsPhysicalItemsFiles=" + Arrays.toString(takeSnapsPhysicalItemsFiles) + ", actionsInFile=" + actionsInFile);
            int sizeIn = 0;
            try (PrintStream ps = new PrintStream(new FileOutputStream(actionsInFile))) {
                for (int i = startingIndex; i < actions.size(); i++) {
                    ps.println(actions.get(i).asPddlLine());
                    sizeIn++;
                }
            }
            int endl[] = new int[2];
            PointType lookForPt = getLookForXYZ();
            if (null == lookForPt) {
                throw new IllegalStateException("null == lookForPT, startingIndex=" + startingIndex + ", actions=" + actions);
            }
            List<OpAction> skippedOpActionsList = new ArrayList<>();
            List<Action> skippedPddlActionsList = new ArrayList<>();
            List<OpAction> opActions = pddlActionsToOpActions(actions, startingIndex, endl, skippedOpActionsList, skippedPddlActionsList);

            List<String> physicalItemNames = getPhysicalItemNames();
            lastOptimizePddlItemNames = physicalItemNames;
            if (optoThread == null) {
                optoThread = Thread.currentThread();
            }
            if (Thread.currentThread() != optoThread) {
                throw new IllegalStateException("!Thread.currentThread() != optoThread: optoThread=" + optoThread + ", Thread.currentThread() =" + Thread.currentThread());
            }

            if (true /*!getReverseFlag() */) {
                MutableMultimap<String, PhysicalItem> availItemsMap
                        = Lists.mutable.ofAll(physicalItemsLocal)
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
                Set<String> typeSet
                        = physicalItemsLocal
                                .stream()
                                .map(PhysicalItem::getType)
                                .collect(Collectors.toSet());
                if (debug) {
                    logDebug("typeSet = " + typeSet);
                }
                MutableMultimap<String, PhysicalItem> availSlotsMap
                        = Lists.mutable.ofAll(physicalItemsLocal)
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
                logDebug("physicalItems = " + physicalItemsLocal);
                logDebug("itemNames = " + physicalItemNames);
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
            inputPlan.setUseStartEndCost(false);
            inputPlan.setAccelleration(fastTransSpeed);
            inputPlan.setMaxSpeed(fastTransSpeed);
            inputPlan.setStartEndMaxSpeed(2 * fastTransSpeed);
            inputPlan.setActions(opActions);
            int inputRequiredCount = 0;
            List<OpAction> inputRequiredActions = new ArrayList<>();
            for (int i = 0; i < opActions.size(); i++) {
                OpAction opActI = opActions.get(i);
                if (opActI.isRequired()) {
                    inputRequiredCount++;
                    inputRequiredActions.add(opActI);
                }
            }
            inputPlan.getEndAction().setLocation(new Point2D.Double(lookForPt.getX(), lookForPt.getY()));
            inputPlan.initNextActions();
            EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
            HardSoftLongScore score = calculator.calculateScore(inputPlan);
            double inScore = (score.getSoftScore() / 1000.0);
            int solveCount = solverRunCount.incrementAndGet();
            int gfzc = generateFromZeroCount.get();
            if (lastgfzc == gfzc) {
                println("solveCount = " + solveCount);
                println("generateFromZeroCount.get() = " + gfzc);
                println("lastgfzc = " + lastgfzc);
                println("generateSinceZeroCount.get() = " + generateSinceZeroCount.get());
            }
            lastgfzc = gfzc;
            OpActionPlan solvedPlan;
            double solveScore;
            long solveTimeStart = System.currentTimeMillis();

            try {
                inputPlan.checkActionList();
                synchronized (solverToRun) {
                    solvedPlan = solverToRun.solve(inputPlan);
                }
                long solveTimeEnd = System.currentTimeMillis();
                long solveTime = solveTimeEnd - solveTimeStart;
                int outputRequiredCount = 0;
                List<OpAction> outputRequiredActions = new ArrayList<>();
                List<OpAction> outputActions = solvedPlan.getOrderedList(false);
                for (int i = 0; i < outputActions.size(); i++) {
                    OpAction opActI = outputActions.get(i);
                    if (opActI.isRequired()) {
                        outputRequiredCount++;
                        outputRequiredActions.add(opActI);
                    }
                }
                println("inputRequiredCount = " + inputRequiredCount);
                println("inputRequiredActions = " + inputRequiredActions);
                println("outputRequiredCount = " + outputRequiredCount);
                println("outputRequiredActions = " + outputRequiredActions);
                HardSoftLongScore hardSoftLongScore = solvedPlan.getScore();
                assert (null != hardSoftLongScore) : "solvedPlan.getScore() returned null";
                solveScore = (hardSoftLongScore.getSoftScore() / 1000.0);
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
                    List<Action> preStartPddlActions = new ArrayList<>(actions.subList(0, Math.max(startingIndex, endl[0])));
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
                    lastOptimizePddlItems = physicalItemsLocal;
                    lastOptimizePreStartPddlActions = preStartPddlActions;
                    lastOptimizeReplacedPddlActions = replacedPddlActions;
                    lastOptimizeNewPddlActions = newPddlActions;
                    lastOptimizeLaterPddlActions = laterPddlActions;
                    File actionsOutFile = aprsSystemLocal.createTempFile("actionsOut", ".txt");
                    int sizeOut = 0;
                    try (PrintStream ps = new PrintStream(new FileOutputStream(actionsOutFile))) {
                        for (int i = startingIndex; i < fullReplanPddlActions.size(); i++) {
                            ps.println(fullReplanPddlActions.get(i).asPddlLine());
                            sizeOut++;
                        }
                    }
                    logOptaPlannerResult(solveCount, startingIndex, sizeIn, sizeOut, solveTime, actionsInFile, actionsOutFile, takeSnapsPhysicalItemsFiles, inScore, solveScore, inputPlan, solvedPlan);
                    return fullReplanPddlActions;
                }
            } catch (Exception ex) {
//            System.err.println("opActionsCopy = " + opActionsCopy);
                System.err.println("actions = " + actions);
                System.err.println("startingIndex = " + startingIndex);
                System.err.println("physicalItems = " + physicalItemsLocal);
                System.err.println("solverToRun = " + solverToRun);
                System.err.println("solveCount = " + solveCount);
                System.err.println("solverRunCount.get() = " + solverRunCount.get());
                LOGGER.log(Level.SEVERE, "", ex);
                takeSimViewSnapshot(ex.toString(), physicalItemsLocal);
                throw new RuntimeException(ex);
            }
            File actionsOutFile = aprsSystemLocal.createTempFile("actionsOut", ".txt");
            int sizeOut = 0;
            try (PrintStream ps = new PrintStream(new FileOutputStream(actionsOutFile))) {
                for (int i = startingIndex; i < actions.size(); i++) {
                    ps.println(actions.get(i).asPddlLine());
                    sizeOut++;
                }
            }
            long solveTimeEnd = System.currentTimeMillis();
            long solveTime = solveTimeEnd - solveTimeStart;
            logOptaPlannerResult(solveCount, startingIndex, sizeIn, sizeOut, solveTime, actionsInFile, actionsOutFile, takeSnapsPhysicalItemsFiles, inScore, solveScore, inputPlan, solvedPlan);
            return actions;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private List<String> getPhysicalItemNames() {
        if (null == physicalItems) {
            return Collections.emptyList();
        }
        return physicalItems.stream()
                .flatMap(nonNullFunction(PhysicalItem::getFullName))
                .collect(Collectors.toList());
    }

    private volatile @Nullable
    GenerateParams lastClearKitsToCheckGenerateParams = null;
    private volatile int lastClearKitsToCheckGenerateParamsIndex = -1;
    private final AtomicInteger clearKitsToCheckCount = new AtomicInteger();

    private void clearKitsToCheck(Action action, List<MiddleCommandType> cmds, GenerateParams gparams)
            throws InterruptedException, IOException, ExecutionException {

        if (!kitsToCheck.isEmpty()) {
            if (null == lastKitsToCheckCopy) {
                lastKitsToCheckCopy = new ArrayList<>(kitsToCheck);
            }
            if (!recheckKitsOnly(gparams)) {
                System.err.println("lastClearKitsToCheckGenerateParams = " + lastClearKitsToCheckGenerateParams);
                System.err.println("lastClearKitsToCheckGenerateParamsIndex = " + lastClearKitsToCheckGenerateParamsIndex);
                System.err.println("lastAddKitToCheckGenerateParams = " + lastAddKitToCheckGenerateParams);
                System.err.println("lastAddKitToCheckGenerateIndex = " + lastAddKitToCheckGenerateIndex);
                System.err.println("kitsToCheck = " + kitsToCheck);
                System.err.println("gparams = " + gparams);
                System.err.println("getLastIndex() = " + getLastIndex());
                lastClearKitsToCheckGenerateParams = gparams;
                lastClearKitsToCheckGenerateParamsIndex = getLastIndex();
                throw new IllegalStateException("clearing kits to check that do not recheck");
            }
            lastClearKitsToCheckGenerateParams = gparams;
            lastClearKitsToCheckGenerateParamsIndex = getLastIndex();
            kitsToCheck.clear();
            clearKitsToCheckCount.incrementAndGet();
            clearKitsToCheckExternalGparams = gparams;
            clearKitsToCheckExternalTrace = Thread.currentThread().getStackTrace();
        }
    }

    private volatile @Nullable
    GenerateParams clearKitsToCheckExternalGparams = null;
    private volatile StackTraceElement clearKitsToCheckExternalTrace @Nullable []  = null;

    void clearKitsToCheckExternal(boolean withRecheck, GenerateParams gparams)
            throws InterruptedException, IOException, ExecutionException {

        if (!kitsToCheck.isEmpty()) {
            if (withRecheck) {
                if (!recheckKitsOnly(false, gparams)) {
                    System.err.println("lastClearKitsToCheckGenerateParams = " + lastClearKitsToCheckGenerateParams);
                    System.err.println("lastClearKitsToCheckGenerateParamsIndex = " + lastClearKitsToCheckGenerateParamsIndex);
                    System.err.println("lastAddKitToCheckGenerateParams = " + lastAddKitToCheckGenerateParams);
                    System.err.println("lastAddKitToCheckGenerateIndex = " + lastAddKitToCheckGenerateIndex);
                    System.err.println("kitsToCheck = " + kitsToCheck);
                    throw new IllegalStateException("clearing kits to check that do not recheck");
                }
            }
            kitsToCheck.clear();
            clearKitsToCheckCount.incrementAndGet();
            clearKitsToCheckExternalGparams = gparams;
            clearKitsToCheckExternalTrace = Thread.currentThread().getStackTrace();
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

            aprsSystem.clearWayToHolders(holder)
                    .thenRun(() -> completeClearWayToHolder());
        }
    }

    private void clearWayToHolder(List<MiddleCommandType> cmds, String holder) {
        addMarkerCommand(cmds, "clearWayToHolder" + holder, x -> clearWayToHolder(holder));
    }

    private volatile @Nullable
    GenerateParams lastAddKitToCheckGenerateParams = null;
    private volatile int lastAddKitToCheckGenerateIndex = -1;

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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }

    private final ConcurrentMap<String, List<String>> skuNameToInstanceNamesMap = new ConcurrentHashMap<>();

    private List<String> getKitInstanceNames(String kitName) {
        return skuNameToInstanceNamesMap.computeIfAbsent(kitName, this::getPartTrayInstancesFromSkuName);
    }

    @SuppressWarnings("nullness")
    private List<PoseType> getKitInstancePoses(String kitName) {
        return getKitInstanceNames(kitName)
                .stream()
                .map(this::getPose)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("nullness")
    private @Nullable
    PhysicalItem getItemByName(List<PhysicalItem> localPhysicalItems, String name) {
        return localPhysicalItems
                .stream()
                .filter(Objects::nonNull)
                .filter((PhysicalItem item) -> item.getFullName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("nullness")
    private List<PhysicalItem> getKitInstanceItems(List<PhysicalItem> localPhysicalItems, String kitName) {
        return getKitInstanceNames(kitName)
                .stream()
                .map((String instanceName) -> getItemByName(localPhysicalItems, instanceName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static <T> List<T> listFilter(List<T> listIn, Predicate<T> predicate) {
        return listIn
                .stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private List<Slot> getAbsSlotListForKitInstance(String kitSkuName, String kitInstanceName, PoseType pose) {
        try {
            if (debug) {
                try {
                    if (null != pose) {
                        logDebug("getAbsSlotListForKitInstance(" + kitSkuName + "," + kitInstanceName + ") : pose = " + CRCLPosemath.poseToXyzRpyString(pose));
                    } else {
                        logDebug("getAbsSlotListForKitInstance(" + kitSkuName + "," + kitInstanceName + ") : pose = null");

                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            }
            if (null == pose) {
                logError("pose=null for kitInstanceName=" + kitInstanceName);
                return Collections.emptyList();
            }
            Tray tray = new Tray(kitSkuName, pose, 0);
            tray.setType("KT");
            if (null != aprsSystem) {
                List<Slot> allSlots = aprsSystem.getAbsSlots(tray, false);
                if (allSlots.isEmpty()) {
                    logError("tray=" + tray + " has no slots");
                    return allSlots;
                }
                return allSlots
                        .stream()
                        .filter(slot -> slot.getType().equals("S"))
                        .peek(slot -> {
                            slot.setVxi(xAxis.getI());
                            slot.setVxj(xAxis.getJ());
                            slot.setVxk(xAxis.getK());
                            slot.setVzi(zAxis.getI());
                            slot.setVzj(zAxis.getJ());
                            slot.setVzk(zAxis.getK());
                            slot.setRotation(Math.atan2(xAxis.getJ(), xAxis.getI()));
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "kitSkuName=" + kitSkuName + ", kitInstanceName=" + kitInstanceName, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
        return Collections.emptyList();
    }

    private static final boolean IGNORE_KIT_CHECK_FAILURES = Boolean.getBoolean("aprs.ignoreKitCheckFailures");

    boolean recheckKitsOnly(GenerateParams gparams) throws InterruptedException, ExecutionException, IOException {
        boolean check1 = recheckKitsOnly(true, gparams);
        if (check1) {
            return true;
        }
        boolean check2 = recheckKitsOnly(true, gparams);
        return check2;
    }

    private boolean recheckKitsOnly(boolean getNewItems, GenerateParams gparams) {
        try {
            if (IGNORE_KIT_CHECK_FAILURES) {
                return true;
            }
            if (getNewItems) {
                checkNewItems("recheckKitsOnly", gparams.startSafeAbortRequestCount);
            }
            List<PhysicalItem> physicalItemsLocal = requireNonNull(physicalItems);
            List<PhysicalItem> parts = checkKitsItemsToParts(physicalItemsLocal);
            Map<String, List<Slot>> kitInstanceAbsSlotMap = new HashMap<>();

            Set<String> matchedKitInstanceNames = new HashSet<>();
            List<KitToCheck> kitsToFix = scanKitsToCheck(false, kitInstanceAbsSlotMap, matchedKitInstanceNames, parts, physicalItemsLocal, gparams);
            boolean empty = kitsToFix.isEmpty();
            if (!empty) {
                saveComplexValueList("kitsToFix", "kitsToFix", kitsToFix, KIT_TO_CHECK_PRINTER);
                takeSimViewSnapshot("recheckKitsOnly: physicalItems", physicalItemsLocal);
                takeSimViewSnapshot("recheckKitsOnly: parts", parts);
                logError("recheckKitsOnly: kitsToFix = " + kitsToFix);
                logError("recheckKitsOnly: matchedKitInstanceNames = " + matchedKitInstanceNames);
                logError("recheckKitsOnly: kitInstanceAbsSlotMap = " + kitInstanceAbsSlotMap);
                for (KitToCheck kit : kitsToFix) {
                    Map<String, KitToCheckInstanceInfo> kitInstanceInfoMap = kit.instanceInfoMap;
                    for (Entry<String, KitToCheckInstanceInfo> entry : kitInstanceInfoMap.entrySet()) {
                        KitToCheckInstanceInfo info = entry.getValue();
                        takeSimViewSnapshot("recheckKitsOnly", info.pose, info.instanceName);
                        for (KitToCheckFailedItemInfo failedItemInfo : info.failedItems) {
                            logError("recheckKitsOnly: failedItemInfo = " + failedItemInfo);
                            logError("recheckKitsOnly: failedItemInfo.failedAbsSlotPrpName = " + failedItemInfo.failedAbsSlotPrpName);
                            logError("recheckKitsOnly: failedItemInfo.failedItemSkuName = " + failedItemInfo.itemHaveName);
                            takeSimViewSnapshot("recheckKitsOnly", failedItemInfo.failedAbsSlot, "failedAbsSlot");
                            if (null != failedItemInfo.failedClosestItem) {
                                takeSimViewSnapshot("recheckKitsOnly", failedItemInfo.failedClosestItem, "failedClosestItem");
                            }
                        }
                        logError("recheckKitsOnly: info.failedSlots = " + info.failedSlots);
                    }
                }
                kitsToFix = scanKitsToCheck(false, kitInstanceAbsSlotMap, matchedKitInstanceNames, parts, physicalItemsLocal, gparams);
                return kitsToFix.isEmpty();
            }
            return empty;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            throw new RuntimeException(ex);
        }
    }
    private volatile int lastCheckKitsLookForIndex = -1;

    private volatile @Nullable
    Action lastCheckKitsLookForIndexAction = null;

    private final List<Action> lastCheckKitsCorrectiveActions = new ArrayList<>();
    private final List<Action> lastCheckKitsOptimizedCorrectiveActions = new ArrayList<>();

    public List<Action> getLastCheckKitsCorrectiveActions() {
        return lastCheckKitsCorrectiveActions;
    }

    public List<Action> getLastCheckKitsOptimizedCorrectiveActions() {
        return lastCheckKitsOptimizedCorrectiveActions;
    }

    private boolean checkKits(Action action, List<MiddleCommandType> cmds, int origIndex, int lastLookForIndex, GenerateParams gparams) {

        lastCheckKitsLookForIndex = lastLookForIndex;
        final Thread curThread = Thread.currentThread();
        int startingClearKitsToCheckCount = clearKitsToCheckCount.get();
        if (null != genThread
                && genThread != curThread) {
            logError("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
            throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
        }
        final AprsSystem localAprsSystem = requireNonNull(aprsSystem);
        if (!localAprsSystem.isDoingActions()) {
            localAprsSystem.logEvent("IsDoingActionsInfo", localAprsSystem.getIsDoingActionsInfo());
            throw new IllegalStateException("!aprsSystem.isDoingActions()");
        }
        List<PhysicalItem> origPhysicalItemsLocal = physicalItems;

        try {

            if (gparams.startSafeAbortRequestCount != localAprsSystem.getSafeAbortRequestCount()) {
                takeSimViewSnapshot("checkKits.aborting_" + gparams.startSafeAbortRequestCount + "_" + localAprsSystem.getSafeAbortRequestCount(), origPhysicalItemsLocal);
                localAprsSystem.logEvent("checkKits:aborting", action, origIndex, lastLookForIndex, gparams.startSafeAbortRequestCount, localAprsSystem.getSafeAbortRequestCount());
                setLastProgramAborted(true);
                addMoveToLookForPosition(cmds, false);
                return true;
            }
            checkSettings();
            boolean correctionMode = localAprsSystem.isCorrectionMode();
            if (IGNORE_KIT_CHECK_FAILURES) {
                if (!correctionMode && pauseInsteadOfRecover) {
                    return false;
                }
            }
            int prePubs = localAprsSystem.getSimLineCount();
            int preVis = localAprsSystem.getVisionLineCount();
            long preTimeDiff = localAprsSystem.getSimVisionTimeDiff();
            long preCheckKitsTime = System.currentTimeMillis();
            List<PhysicalItem> physicalItemsLocal
                    = checkNewItems("checkKits", gparams.startSafeAbortRequestCount);
            boolean breakActionsSetNeeded = false;
            try {

                long postCheckKitsTime = System.currentTimeMillis();
                long checkKitsTimeDiff = postCheckKitsTime - preCheckKitsTime;
                int postPubs = localAprsSystem.getSimLineCount();
                int postVis = localAprsSystem.getVisionLineCount();
                long postTimeDiff = localAprsSystem.getSimVisionTimeDiff();

                takeSnapshots("plan", "checkKits-", null, "");

                List<PhysicalItem> parts = checkKitsItemsToParts(physicalItemsLocal);
                synchronized (this) {
                    List<String> newItemsFullNames = physicalItemsLocal
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

                    Map<String, List<Slot>> kitInstanceAbsSlotMap = new HashMap<>();

                    Set<String> matchedKitInstanceNames = new HashSet<>();
                    List<KitToCheck> kitsToFix = scanKitsToCheck(true, kitInstanceAbsSlotMap, matchedKitInstanceNames, parts, physicalItemsLocal, gparams);

                    if (optoThread == null) {
                        optoThread = Thread.currentThread();
                    }
                    if (Thread.currentThread() != optoThread) {
                        throw new IllegalStateException("!Thread.currentThread() != optoThread: optoThread=" + optoThread + ", Thread.currentThread() =" + Thread.currentThread());
                    }
                    if (!kitsToFix.isEmpty()) {
                        logDebug("kitsToFix = " + kitsToFix);
                        printLastOptoInfo();
                        if (pauseInsteadOfRecover && !correctionMode) {
                            String errMsg = showKitToFixErrors(physicalItemsLocal, kitInstanceAbsSlotMap, parts, matchedKitInstanceNames, kitsToFix, prePubs, preVis, preTimeDiff, postPubs, postVis, postTimeDiff, checkKitsTimeDiff, cmds);
                            throw new IllegalStateException(errMsg);
                        } else {
                            Map<String, Integer> prefixCountMap = new HashMap<>();
                            Map<String, List<String>> itemsNameMap = new HashMap<>();
                            Map<String, List<String>> removedItemsNameMap = new HashMap<>();
                            kitsToFix.sort(Comparators.byIntFunction(KitToCheck::getMaxDiffFailedSlots));
                            List<Action> correctiveActions = new ArrayList<>();
                            List<PhysicalItem> correctivedItems = new ArrayList<>();
                            List<String> fixLogList = new ArrayList<>();
                            for (KitToCheck kit : kitsToFix) {
                                List<KitToCheckInstanceInfo> infoList = new ArrayList<>(kit.instanceInfoMap.values());
                                infoList.sort(Comparators.byIntFunction(KitToCheckInstanceInfo::getFailedSlots));
//                                for (KitToCheckInstanceInfo info : infoList) {
                                for (int infoListIndex = 0; infoListIndex < infoList.size(); infoListIndex++) {
                                    KitToCheckInstanceInfo info = infoList.get(infoListIndex);
                                    String kitInstanceName = info.instanceName;
                                    if (matchedKitInstanceNames.contains(kitInstanceName)) {
                                        continue;
                                    }
                                    if (null == info.pose) {
                                        continue;
                                    }
                                    PoseType pose = info.pose;

                                    List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                                            (String n) -> getAbsSlotListForKitInstance(kit.name, n, pose));

                                    if (snapshotsEnabled()) {
                                        takeSimViewSnapshot(createImageTempFile("absSlots_" + kitInstanceName), absSlots);
                                    }
                                    int brokenAbsSlotsChecked = 0;
                                    for (int absSlotIndex = 0; absSlotIndex < absSlots.size(); absSlotIndex++) {
                                        Slot absSlot = absSlots.get(absSlotIndex);
                                        String absSlotPrpName = absSlot.getPrpName();
                                        PhysicalItem closestItem = parts.stream()
                                                .min(Comparator.comparing(absSlot::distFromXY))
                                                .orElse(null);
                                        if (null == closestItem) {
                                            LOGGER.log(Level.SEVERE, "closetItem == null in checkKits");
                                            break;
                                        }
                                        String itemNowInSlotSkuName = "empty";
                                        if (closestItem.distFromXY(absSlot) < absSlot.getDiameter() / 2.0) {
                                            itemNowInSlotSkuName = closestItem.origName;
                                        }
                                        String itemNeededInSlotSkuName = kit.slotMap.get(absSlotPrpName);
                                        fixLogList.add("kitInstanceName=" + kitInstanceName + ",absSlotPrpName=" + absSlotPrpName + ",itemsNameMap=" + itemsNameMap + "\r\n");
                                        if (null != itemNeededInSlotSkuName && !itemNeededInSlotSkuName.equals(itemNowInSlotSkuName)) {

                                            brokenAbsSlotsChecked++;
                                            if (!itemNowInSlotSkuName.equals("empty")) {
                                                String shortNowInSlotSkuName = Utils.shortenItemPartName(itemNowInSlotSkuName);
                                                String slotPrefix = "empty_slot_for_" + shortNowInSlotSkuName + "_in_" + shortNowInSlotSkuName + "_vessel";
                                                int count = prefixCountMap.compute(slotPrefix,
                                                        (String prefix, Integer c) -> (c == null) ? 1 : (c + 1));
                                                lastTakenPart = closestItem.getName();
                                                correctivedItems.add(closestItem);
                                                correctiveActions.add(Action.newTakePartAction(closestItem.getFullName()));
                                                correctivedItems.add(absSlot);
                                                correctiveActions.add(Action.newPlacePartAction(slotPrefix + "_" + count, shortNowInSlotSkuName));
                                            }
                                            if (!itemNeededInSlotSkuName.equals("empty")) {
                                                if (!itemNowInSlotSkuName.equals("empty")) {
                                                    breakActionsSetNeeded = true;
                                                }
                                                if (breakActionsSetNeeded) {
                                                    break;
                                                }
                                                final String shortItemNeededInSlotSkuName = Utils.shortenItemPartName(itemNeededInSlotSkuName);
                                                List<String> partNames
                                                        = itemsNameMap.computeIfAbsent(shortItemNeededInSlotSkuName,
                                                                k -> listFilter(
                                                                        partsInPartsTrayFullNames,
                                                                        name2 -> name2.contains(k)));
                                                logDebug("checkKits: partNames = " + partNames);
                                                if (partNames.isEmpty()) {
                                                    if (!correctiveActions.isEmpty() || infoListIndex < infoList.size() - 1 || brokenAbsSlotsChecked < info.getFailedSlots()) {
                                                        breakActionsSetNeeded = true;
                                                        continue;
                                                    }
                                                    logError("No partnames for shortItemNeededInSlotSkuName=" + shortItemNeededInSlotSkuName);
                                                    logDebug("checkKits: partNames = " + partNames);
                                                    logError("fixLogList=" + fixLogList);
                                                    logError("correctiveActions.isEmpty()=" + correctiveActions.isEmpty());
                                                    logError("infoListIndex=" + infoListIndex);
                                                    logError("infoList.size()=" + infoList.size());
                                                    logError("brokenAbsSlotsChecked=" + brokenAbsSlotsChecked);
                                                    logError("info.getFailedSlots()=" + info.getFailedSlots());
                                                    logError("kit.slotMap = " + kit.slotMap);
                                                    logError("newItems = " + physicalItemsLocal);
                                                    logError("newItemsFullNames = " + newItemsFullNames);
                                                    logError("itemsNameMap = " + itemsNameMap);
                                                    logError("removedItemsMap=" + removedItemsNameMap);
                                                    logError("slotItemSkuName = " + itemNeededInSlotSkuName);
                                                    logError("itemSkuName = " + itemNowInSlotSkuName);
                                                    logError("partsInPartsTrayFullNames = " + partsInPartsTrayFullNames);
                                                    List<String> recalcPartNames
                                                            = listFilter(
                                                                    partsInPartsTrayFullNames,
                                                                    name2 -> name2.contains(shortItemNeededInSlotSkuName));
                                                    logError("recalcPartNames = " + recalcPartNames);
                                                    localAprsSystem.setSnapshotsSelected(true);
                                                    takeSimViewSnapshot("checkKits : no partnames ", physicalItemsLocal);
                                                    throw new IllegalStateException("No partnames for finalShortSkuName=" + shortItemNeededInSlotSkuName
                                                            + ", absSlotPrpName=" + absSlotPrpName
                                                            + ", slotItemSkuName=" + itemNeededInSlotSkuName
                                                            + ", itemSkuName=" + itemNowInSlotSkuName);
                                                }
                                                String partName = partNames.remove(0);
                                                removedItemsNameMap.compute(shortItemNeededInSlotSkuName, (k, v) -> {
                                                    if (v == null) {
                                                        v = new ArrayList<>();
                                                    }
                                                    v.add(partName);
                                                    return v;
                                                });
                                                correctiveActions.add(Action.newTakePartAction(partName));
                                                for (int i = 0; i < parts.size(); i++) {
                                                    PhysicalItem partI = parts.get(i);
                                                    if (partI.getFullName().equals(partName)) {
                                                        correctivedItems.add(partI);
                                                        break;
                                                    }
                                                }
                                                String slotName = absSlot.getFullName();
                                                logDebug("checkKits: slotName = " + slotName);
                                                PoseType absSlotPose = absSlot.getPose();
                                                correctivedItems.add(absSlot);
                                                if (!itemNowInSlotSkuName.equals("empty")) {
                                                    breakActionsSetNeeded = true;
                                                    if (!correctiveActions.isEmpty()) {
                                                        break;
                                                    }
                                                    poseCache.put(slotName, absSlotPose);
                                                    correctiveActions.add(Action.newPlacePartAction(slotName, shortItemNeededInSlotSkuName));
                                                } else {

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
                                                        takeSimViewSnapshot("checkKits : physicalItemsLocal", physicalItemsLocal);
                                                        takeSimViewSnapshot("checkKits : absSlots", absSlots);
                                                        List<PhysicalItem> itemsPlusSlots = new ArrayList<>();
                                                        itemsPlusSlots.addAll(physicalItemsLocal);
                                                        itemsPlusSlots.addAll(absSlots);
                                                        takeSimViewSnapshot("checkKits : itemsPlusSlots", itemsPlusSlots);
                                                        takeSimViewSnapshot("checkKits : absSlotPose", absSlotPose, slotName);
                                                        takeSimViewSnapshot("checkKits : slotPose", slotPose, slotName);
                                                        takeSimViewSnapshot("checkKits : kitPose", pose, kitInstanceName);
                                                        throw new IllegalStateException("absSlotPose for " + slotName + " not in poseCache min_dist=" + min_dist + ", closestKet=" + closestKey + ", keys=" + poseCache.keySet());
                                                    }
                                                    correctiveActions.add(Action.newPlacePartAction(closestKey, shortItemNeededInSlotSkuName));
                                                }
                                            }
                                        }
                                    }
                                    matchedKitInstanceNames.add(kitInstanceName);
                                    break;
                                }
                                logDebug("matchedKitInstanceNames = " + matchedKitInstanceNames);
                                logDebug("kitsToFix = " + kitsToFix);
                            }
                            lastCheckKitsCorrectiveActions.clear();
                            lastCheckKitsCorrectiveActions.addAll(correctiveActions);
                            if (!correctiveActions.isEmpty()) {
                                List<Action> optimizedCorrectiveActions
                                        = optimizePddlActionsWithOptaPlanner(
                                                correctiveActions,
                                                0, // starting index
                                                physicalItemsLocal);
                                lastCheckKitsOptimizedCorrectiveActions.clear();
                                lastCheckKitsOptimizedCorrectiveActions.addAll(correctiveActions);
                                lastIndex.compareAndSet(origIndex, lastLookForIndex);
                                boolean placedPart = false;
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
                                                        continue;
                                                    }
                                                }
                                            }
                                            takePartByName(partName, null, cmds);
                                            break;

                                        case PLACE_PART:
                                            placedPart = true;
                                            String slotName = correctiveAction.getArgs()[placePartSlotArgIndex];
                                            placePartBySlotName(slotName, cmds, correctiveAction, action, lastLookForIndex);  //ByName(slotName, null, cmds);
                                            break;
                                    }
                                }
                                if (placedPart) {
                                    takeSimViewSnapshot("checkKits:correctivedItems", correctivedItems);
                                    takeSimViewSnapshot("checkKits:correctivedItems.physicalItemsLocal", physicalItemsLocal);
                                    return true;
                                } else {
                                    System.err.println("correctiveActions = " + correctiveActions);
                                    System.err.println("optimizedCorrectiveActions = " + optimizedCorrectiveActions);
                                    logError("checkKits : !placedPart  kitsToFix = " + kitsToFix);
                                    lastIndex.set(lastLookForIndex);
                                    String errMsg = showKitToFixErrors(physicalItemsLocal, kitInstanceAbsSlotMap, parts, matchedKitInstanceNames, kitsToFix, prePubs, preVis, preTimeDiff, postPubs, postVis, postTimeDiff, checkKitsTimeDiff, cmds);
                                    throw new IllegalStateException(errMsg);
                                }
                            }
                        }
                    }

                }
                try {
                    takeSimViewSnapshot("checkKitsReturningFalse" + origIndex, physicalItemsLocal);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                takeSimViewSnapshot("checkKits:" + ex.getMessage() + ".physicalItemsLocal", physicalItemsLocal);
                throw new RuntimeException(ex);
            }
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                LOGGER.log(Level.SEVERE, "", ex);
                throw new RuntimeException(ex);
            }
        }

        return false;
    }

    private String
            showKitToFixErrors(
                    List<PhysicalItem> physicalItemsLocal,
                    Map<String, List<Slot>> kitInstanceAbsSlotMap,
                    List<PhysicalItem> parts,
                    Set<String> matchedKitInstanceNames,
                    List<KitToCheck> kitsToFix,
                    int prePubs,
                    int preVis,
                    long preTimeDiff,
                    int postPubs,
                    int postVis,
                    long postTimeDiff,
                    long checkKitsTimeDiff,
                    List<MiddleCommandType> cmds
            ) throws IOException {
        StringBuilder errMsgSb = new StringBuilder();
        final AprsSystem localAprsSystem = requireNonNull(this.aprsSystem,"aprsSystem");
        localAprsSystem.setSnapshotsSelected(true);
        takeSimViewSnapshot("checkKitsFailed", physicalItemsLocal);
        String errMsgStart = localAprsSystem.getRunName();
        errMsgSb.append(errMsgStart);
        logError("checkKits: errMsgStart=" + errMsgStart);
        logError("checkKits: newItems = " + physicalItemsLocal);
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
                takeSimViewSnapshot("checkKitsFailed: info", info.pose, info.instanceName);
                takeSimViewSnapshot("checkKitsFailed: info.absSlots", info.absSlots);
                List<PhysicalItem> itemsPlusAbsSlots = new ArrayList<>();
                itemsPlusAbsSlots.addAll(physicalItemsLocal);
                itemsPlusAbsSlots.addAll(info.absSlots);
                takeSimViewSnapshot("checkKitsFailed: itemsPlusAbsSlots", itemsPlusAbsSlots);
                logError("checkKits: info = " + info);
                logError("checkKits: info.failedItems.size()=" + info.failedItems.size());
                for (int i = 0; i < info.failedItems.size(); i++) {
                    KitToCheckFailedItemInfo failedItemInfo = info.failedItems.get(i);
                    takeSimViewSnapshot("checkKitsFailed: failedItemInfo.failedAbsSlot", failedItemInfo.failedAbsSlot, failedItemInfo.failedAbsSlotPrpName);
                    final PhysicalItem failedClosestItem = failedItemInfo.failedClosestItem;
                    if (null != failedClosestItem) {
                        takeSimViewSnapshot("checkKitsFailed: failedItemInfo.failedClosestItem", failedClosestItem, failedClosestItem.getFullName());
                    }
                    String fiString = " " + kit.slotMap.get(failedItemInfo.failedAbsSlotPrpName) + " instead of " + failedItemInfo.itemHaveName + " in " + failedItemInfo.failedAbsSlotPrpName;
                    errMsgSb.append(fiString);
                    if (i < info.failedItems.size() - 1) {
                        errMsgSb.append(", \n");
                    }
                    logError("checkKits: failedItemInfo = " + failedItemInfo);
                    logError("checkKits: failedItemInfo.failedAbsSlotPrpName = " + failedItemInfo.failedAbsSlotPrpName);
                    logError("checkKits: failedItemInfo.failedClosestItem = " + failedItemInfo.failedClosestItem);
                    logError("checkKits: failedItemInfo.failedClosestItemDist = " + failedItemInfo.failedClosestItemDist);
                    logError("checkKits: failedItemInfo.failedAbsSlot = " + failedItemInfo.failedAbsSlot);
                }
            }
        }
        logError("prePubs = " + prePubs);
        logError("preVis = " + preVis);
        logError("preTimeDiff = " + preTimeDiff);
        logError("postPubs = " + postPubs);
        logError("postVis = " + postVis);
        logError("postTimeDiff = " + postTimeDiff);
        logError("checkKitsTimeDiff = " + checkKitsTimeDiff);
        CRCLProgramType program = new CRCLProgramType();
        program.setInitCanon(new InitCanonType());
        program.setEndCanon(new EndCanonType());
        final List<MiddleCommandType> middleCommandsList
                = CRCLUtils.middleCommands(program);
        middleCommandsList.addAll(cmds);
        localAprsSystem.logCrclProgFile(program);
        String errMsg = errMsgSb.toString();
        takeSimViewSnapshot(errMsg, physicalItemsLocal);
        localAprsSystem.setTitleErrorString(errMsg);
        return errMsg;
    }

    private List<PhysicalItem> checkKitsItemsToParts(List<PhysicalItem> newItems) {
        List<PhysicalItem> parts = newItems.stream()
                .filter(x -> !x.getName().startsWith("empty_slot"))
                .filter(x -> !x.getName().contains("vessel"))
                .collect(Collectors.toList());
        return parts;
    }

    private volatile @Nullable
    List<KitToCheck> lastKitsToCheckCopy
            = null;

    public static class ScanKitsToCheckInfo {

        int kitsToCheckIndex = 0;
        String kitName = "";
        String kitInstanceName = "";
        String absSlotPrpName = "";
        String itemHaveName = "";
        String itemNeededName = "";
        boolean match = false;

        public Object[] toTableArray() {
            return new Object[]{kitsToCheckIndex, kitName, kitInstanceName, absSlotPrpName, itemHaveName, itemNeededName, match};
        }
    }

    private final List<ScanKitsToCheckInfo> lastScanKitsToCheckInfoList = new ArrayList<>();

    public List<ScanKitsToCheckInfo> getLastScanKitsToCheckInfoList() {
        return lastScanKitsToCheckInfoList;
    }

    private final AtomicInteger lastScanKitsToCheckInfoListCount = new AtomicInteger();

    public int getLastScanKitsToCheckInfoListCount() {
        return lastScanKitsToCheckInfoListCount.get();
    }

    private List<KitToCheck> scanKitsToCheck(
            boolean newCheck,
            Map<String, List<Slot>> kitInstanceAbsSlotMap,
            Set<String> matchedKitInstanceNames,
            List<PhysicalItem> parts,
            List<PhysicalItem> physicalItemsLocal,
            GenerateParams gparams) throws IOException {

        final Thread curThread = Thread.currentThread();
        if (null != genThread
                && genThread != curThread) {
            logError("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
            throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
        }
        final AprsSystem aprsSystemFinal = aprsSystem;
        if (null == aprsSystemFinal) {
            throw new NullPointerException("aprsSystem");
        }
        if (!aprsSystemFinal.isDoingActions()) {
            aprsSystemFinal.logEvent("IsDoingActionsInfo", aprsSystemFinal.getIsDoingActionsInfo());
            throw new IllegalStateException("!aprsSystem.isDoingActions() ");
        }
        List<KitToCheck> kitsToFix = new ArrayList<>(kitsToCheck);
        checkKitsToCheckInstanceCounts(physicalItemsLocal);
        if (kitsToFix.isEmpty()) {
            if (!newCheck && null != lastKitsToCheckCopy && !lastKitsToCheckCopy.isEmpty()) {
                kitsToFix = new ArrayList<>(lastKitsToCheckCopy);
            } else {
                System.err.println("getPrivateStartActionsTraceString() = " + aprsSystemFinal.getPrivateStartActionsTraceString());
                System.err.println("getPrivateStartActionsCommentString() = " + aprsSystemFinal.getPrivateStartActionsCommentString());
                System.err.println("getPrivateContinueActionsTraceString() = " + aprsSystemFinal.getPrivateContinueActionsTraceString());
                System.err.println("getPrivateContinueActionsCommentString() = " + aprsSystemFinal.getPrivateContinueActionsCommentString());
                System.err.println("lastClearKitsToCheckGenerateParams = " + lastClearKitsToCheckGenerateParams);
                System.err.println("lastClearKitsToCheckGenerateParamsIndex = " + lastClearKitsToCheckGenerateParamsIndex);
                System.err.println("clearKitsToCheckExternalGparams = " + clearKitsToCheckExternalGparams);
                System.err.println("clearKitsToCheckExternalTrace = " + Utils.traceToString(clearKitsToCheckExternalTrace));
                takeSimViewSnapshot("kitsToFix is empty:physicalItemsLocal", physicalItemsLocal);
                throw new IllegalStateException("kitsToCheck is empty : gparams=" + gparams);
            }
        }
        List<KitToCheck> kitsToCheckCopy;
        if (newCheck) {
            kitsToCheckCopy = new ArrayList<>(kitsToFix);
            lastKitsToCheckCopy = kitsToCheckCopy;
        } else if (lastKitsToCheckCopy != null) {
            kitsToCheckCopy = lastKitsToCheckCopy;
        } else {
            takeSimViewSnapshot("no lastKitsToCheckCopy to reuse:physicalItemsLocal", physicalItemsLocal);
            throw new IllegalStateException("no lastKitsToCheckCopy to reuse");
        }

        List<PhysicalItem> allItemsToFix = new ArrayList<>();
        lastScanKitsToCheckInfoList.clear();
        lastScanKitsToCheckInfoListCount.incrementAndGet();
        for (int kitsToCheckIndex = 0; kitsToCheckIndex < kitsToCheckCopy.size(); kitsToCheckIndex++) {
            KitToCheck kit = kitsToCheckCopy.get(kitsToCheckIndex);
            kit.kitInstanceNames = getKitInstanceNames(kit.name);
            Map<String, KitToCheckInstanceInfo> kitInstanceInfoMap = new HashMap<>();
            List<PhysicalItem> kitItemsToFix = new ArrayList<>();
            for (String kitInstanceName : kit.kitInstanceNames) {
                PoseType pose = getPose(kitInstanceName, physicalItemsLocal);
                if (pose == null) {
                    continue;
                }
                List<Slot> absSlots = kitInstanceAbsSlotMap.computeIfAbsent(kitInstanceName,
                        (String n) -> getAbsSlotListForKitInstance(kit.name, n, pose));
                KitToCheckInstanceInfo info = new KitToCheckInstanceInfo(kitInstanceName, absSlots, pose);
                kitInstanceInfoMap.put(kitInstanceName, info);
                if (absSlots.isEmpty()) {
                    continue;
                }
                if (matchedKitInstanceNames.contains(kitInstanceName)) {
                    continue;
                }
                if (snapshotsEnabled()) {
                    takeSimViewSnapshot("absSlots_" + kitInstanceName, absSlots);
                }
                boolean allSlotsCorrect = true;
                List<PhysicalItem> itemsToFix = new ArrayList<>();
                List<ScanKitsToCheckInfo> instanceInfoSubList = new ArrayList<>();
                for (Slot absSlot : absSlots) {
                    String absSlotPrpName = absSlot.getPrpName();
                    PhysicalItem closestItem = parts.stream()
                            .min(Comparator.comparing(absSlot::distFromXY))
                            .orElse(null);
                    if (null == closestItem) {
                        LOGGER.log(Level.SEVERE, "closetItem == null in checkKits");
                        break;
                    }
                    info.closestItemMap.put(absSlotPrpName, closestItem);
                    info.closestItemNameMap.put(absSlotPrpName, closestItem.getFullName());
                    String itemHaveName = "empty";
                    double closestItemDist = closestItem.distFromXY(absSlot);
                    if (closestItemDist < absSlot.getDiameter() / 2.0) {
                        itemHaveName = closestItem.origName;
                    }
                    info.itemSkuMap.put(absSlotPrpName, itemHaveName);
                    final String itemNeededName = kit.slotMap.get(absSlotPrpName);
                    if (null == itemNeededName) {
                        throw new RuntimeException("kit.slotMap.get(" + absSlotPrpName + ") returned null : absSlot=" + absSlot + ", kit.slotMap=" + kit.slotMap);
                    }
                    ScanKitsToCheckInfo scanKitsToCheckInfo = new ScanKitsToCheckInfo();
                    scanKitsToCheckInfo.kitsToCheckIndex = kitsToCheckIndex;
                    scanKitsToCheckInfo.kitName = kit.name;
                    scanKitsToCheckInfo.kitInstanceName = kitInstanceName;
                    scanKitsToCheckInfo.absSlotPrpName = absSlotPrpName;
                    scanKitsToCheckInfo.itemHaveName = itemHaveName;
                    scanKitsToCheckInfo.itemNeededName = itemNeededName;
                    instanceInfoSubList.add(scanKitsToCheckInfo);
                    if (!Objects.equals(itemNeededName, itemHaveName)) {
                        KitToCheckFailedItemInfo failedItemInfo
                                = new KitToCheckFailedItemInfo(
                                        absSlotPrpName,
                                        itemHaveName,
                                        itemNeededName,
                                        closestItem,
                                        absSlot,
                                        closestItemDist);
                        info.failedItems.add(failedItemInfo);
                        info.failedSlots++;
                        if (itemNeededName.equals("empty")) {
                            itemsToFix.add(closestItem);
                        }
                        itemsToFix.add(absSlot);
                        allSlotsCorrect = false;
                    }
                }
                for (ScanKitsToCheckInfo scanKitsToCheckInfo : instanceInfoSubList) {
                    scanKitsToCheckInfo.match = allSlotsCorrect;
                }
                lastScanKitsToCheckInfoList.addAll(instanceInfoSubList);
                if (allSlotsCorrect) {
                    kitsToFix.remove(kit);
                    matchedKitInstanceNames.add(kitInstanceName);
                    break;
                } else {
                    kitItemsToFix.addAll(itemsToFix);
                    takeSimViewSnapshot("itemsToFix:" + kitsToCheckIndex + ":" + kitInstanceName, itemsToFix);
                }
            }
            if (!kitInstanceInfoMap.isEmpty()) {
                kit.instanceInfoMap = kitInstanceInfoMap;
                allItemsToFix.addAll(kitItemsToFix);
            } else {
                kitsToFix.remove(kit);
            }
            if (debug) {
                logDebug("matchedKitInstanceNames = " + matchedKitInstanceNames);
                logDebug("kitsToFix = " + kitsToFix);
            }
        }
        if (!allItemsToFix.isEmpty()) {
            takeSimViewSnapshot("scanKitsToCheck.allItemsToFix", allItemsToFix);
            takeSimViewSnapshot("scanKitsToCheck.physicalItemsLocal", physicalItemsLocal);
            takeSimViewSnapshot("scanKitsToCheck.itemCache", getItemCache().values());
        }
        return kitsToFix;
    }

    private synchronized void checkKitsToCheckInstanceCounts(
            List<PhysicalItem> physicalItemsLocal) throws IllegalStateException {
        ConcurrentHashMap<String, Integer> kitNameCountMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<PhysicalItem>> kitNameItemListMap = new ConcurrentHashMap<>();
        List<KitToCheck> kitsToChecksList = new ArrayList<>(kitsToCheck);
        if (physicalItemsLocal.isEmpty() && !kitsToChecksList.isEmpty()) {
            throw new IllegalArgumentException("physicalItemsLocal.isEmpty()");
        }
        for (KitToCheck kit : kitsToChecksList) {
            kitNameCountMap.compute(kit.name, (String name, Integer v) -> ((v != null) ? v + 1 : 1));
            kitNameItemListMap.compute(kit.name, (String name, List<PhysicalItem> v) -> ((v != null) ? v : getKitInstanceItems(physicalItemsLocal, name)));
        }
        for (Entry<String, Integer> entry : kitNameCountMap.entrySet()) {
            List<PhysicalItem> items = kitNameItemListMap.get(entry.getKey());
            if (null == items) {
                System.err.println("lastRequiredPartsMap = " + lastRequiredPartsMap);
                final String errmsg = "checkKitsToCheckInstanceCounts: need " + entry.getValue() + " kits of " + entry.getKey() + " but poses == null ";
                takeSimViewSnapshot(errmsg + "physicalItemsLocal", physicalItemsLocal);
                throw new IllegalStateException(errmsg);
            } else if (items.size() < entry.getValue()) {
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
                final List<String> kitInstanceNames = getKitInstanceNames(entry.getKey());
                System.err.println("kitInstanceNames = " + kitInstanceNames);
                System.err.println("skuNameToInstanceNamesMap = " + skuNameToInstanceNamesMap);
                List<PhysicalItem> recheckList = getKitInstanceItems(physicalItemsLocal, entry.getKey());
                System.err.println("recheckList = " + recheckList);
                System.err.println("entry = " + entry);
                System.err.println("kitNameCountMap = " + kitNameCountMap);
                System.err.println("kitsToChecksList = " + kitsToChecksList);
                System.err.println("lastRequiredPartsMap = " + lastRequiredPartsMap);
                System.err.println("items = " + items);
                System.err.println("physicalItemsLocal = " + physicalItemsLocal);
                System.err.println("kitNameItemListMap = " + kitNameItemListMap);
                println("getKitInstancePoses(name) = " + getKitInstancePoses(entry.getKey()));
                final String errmsg = "checkKitsToCheckInstanceCounts: need " + entry.getValue() + " kits of " + entry.getKey() + " but only have " + items.size();
                takeSimViewSnapshot(errmsg + "physicalItemsLocal", physicalItemsLocal);
                List<String> trayInstances = getPartTrayInstancesFromSkuName(entry.getKey());
                System.out.println("trayInstances = " + trayInstances);
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
                throw new IllegalStateException(errmsg);
            }
        }
    }

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
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    private final Map<String, PoseType> returnPosesByName = new HashMap<>();

    private @Nullable
    String lastTakenPart = null;

    private @Nullable
    String getLastTakenPart() {
        return lastTakenPart;
    }

    private double verySlowTransSpeed = 20.0;

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

    private final @Nullable
    AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public @Nullable
    AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    final private ExecutorJPanel parentExecutorJPanel;

    /**
     * Get the value of parentExecutorJPanel
     *
     * @return the value of parentExecutorJPanel
     */
    public @Nullable
    ExecutorJPanel getParentExecutorJPanel() {
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
    private @Nullable
    File[] takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(f, pose, label);
        } else {
            return new File[2];
        }
    }

    public @Nullable
    File[] takeSimViewSnapshot(File f, PointType point, String label) {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(f, point, label);
        } else {
            return new File[2];
        }
    }

    public @Nullable
    File[] takeSimViewSnapshot(File f, PmCartesian pt, String label) {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(f, pt, label);
        } else {
            return new File[2];
        }
    }

    public @Nullable
    File[] takeSimViewSnapshot(String imgLabel, PoseType pose, String label) throws IOException {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(imgLabel, pose, label);
        } else {
            return new File[2];
        }
    }

    public @Nullable
    File[] takeSimViewSnapshot(String imgLabel, PointType point, String label) throws IOException {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(imgLabel, point, label);
        } else {
            return new File[2];
        }
    }

    private @Nullable
    File[] takeSimViewSnapshot(String imgLabel, @Nullable PmCartesian pt, @Nullable String label) throws IOException {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(imgLabel, pt, label);
        } else {
            return new File[2];
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint items to paint in the snapshot image
     */
    private @Nullable
    File[] takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(f, itemsToPaint);
        } else {
            return new File[2];
        }
    }

    private @Nullable
    File[] takeSimViewSnapshot(String imgLabel, @Nullable Collection<? extends PhysicalItem> itemsToPaint) {
        if (null != aprsSystem) {
            return aprsSystem.takeSimViewSnapshot(imgLabel, itemsToPaint);
        } else {
            return new File[2];
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

//    private List<PhysicalItem> posesToDetectedItemList(@Nullable Collection<PoseType> poses) {
//        List<PhysicalItem> l = new ArrayList<>();
//        int i = 0;
//        if (null != poses) {
//            for (PoseType pose : poses) {
//                i++;
//                l.add(PhysicalItem.newPhysicalItemNamePoseVisionCycle("pose_" + i, pose, 0));
//            }
//        }
//        l.addAll(poseCacheToDetectedItemList());
//        return Collections.unmodifiableList(l);
//    }

    private XFutureVoid takeDatabaseViewSnapshot(File f) {
        if (null != aprsSystem) {
            return aprsSystem.startVisionToDbNewItemsImageSave(f);
        } else {
            return XFutureVoid.completedFuture();
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
                File imageTempFile = createImageTempFile(prefix + "_pc_" + fullTitle);
                if (null == externalPoseProvider) {
                    takeDatabaseViewSnapshot(createImageTempFile(prefix + "_db_" + fullTitle))
                            .thenRun(() -> {
                                takeSimViewSnapshot(imageTempFile, poseCacheToDetectedItemList());
                            });
                }  else {
                    takeSimViewSnapshot(imageTempFile, poseCacheToDetectedItemList());
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
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

    public @Nullable
    PoseType getPose(String name) {
        return poseCache.get(name);
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

            LOGGER.log(Level.WARNING,
                    "no pose for {0} poseCache.keySet() = {1}, clearPoseCacheTrace={2}",
                    new Object[]{partName, poseCache.keySet(), Utils.traceToString(clearPoseCacheTrace)});
            return;
        }
        pose = visionToRobotPose(pose);
        returnPosesByName.put(partName, pose);
        pose.setXAxis(xAxis);
        pose.setZAxis(zAxis);
        testPartPositionByPose(out, pose);
        setLastTakenPart(partName);
    }

    private void setCommandId(CRCLCommandType cmd) {
        Utils.setCommandID(cmd, incrementAndGetCommandId());
    }

    private void setCorrectKitImage() {
        if (null != kitInspectionJInternalFrame && null != aprsSystem) {
            String kitinspectionImageKitPath = kitInspectionJInternalFrame.getKitinspectionImageKitPath();
            String kitImage = kitInspectionJInternalFrame.getKitImage();
            String kitStatusImage = kitinspectionImageKitPath + "/" + kitImage + ".png";
            logDebug("kitStatusImage " + kitStatusImage);
            if (null != aprsSystem) {
                aprsSystem.runOnDispatchThread(() -> setKitStatusIcon(kitStatusImage));
            }
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
                aprsSystem.runOnDispatchThread(() -> {
                    try {
                        kitInspectionJInternalFrame.getKitTitleLabel().setText(text);
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "", ex);
                    }
                });
            }
        }
    }

    @SuppressWarnings("nullness")
    private void addToInspectionResultJTextPane(String text) {
        if (null != kitInspectionJInternalFrame) {
            aprsSystem.runOnDispatchThread(() -> {
                try {
                    kitInspectionJInternalFrame.addToInspectionResultJTextPane(text);
                } catch (BadLocationException ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                }
            });
        }
    }

//    private double getVisionToDBRotationOffset() {
//        assert (null != this.aprsSystem) : "null == this.aprsSystemInterface: @AssumeAssertion(nullness)";
//        return this.aprsSystem.getVisionToDBRotationOffset();
//    }


//    private double normAngle(double angleIn) {
//        double angleOut = angleIn;
//        if (angleOut > Math.PI) {
//            angleOut -= 2 * Math.PI * ((int) (angleIn / Math.PI));
//        } else if (angleOut < -Math.PI) {
//            angleOut += 2 * Math.PI * ((int) (-1.0 * angleIn / Math.PI));
//        }
//        return angleOut;
//    }

    //    private int checkPartTypeInSlot(String partInKt, Slot slot) throws SQLException, IOException {
//        int nbOfOccupiedSlots = 0;
//        List<String> allPartsInKt = new ArrayList<>();
//        //-- queries the database 10 times to make sure we are not missing some part_in_kt
//        for (int i = 0; i < 20; i++) {
//            if (allPartsInKt.isEmpty()) {
//
//                allPartsInKt = getAllPartsInKt(partInKt);
//            }
//        }
//        if (!allPartsInKt.isEmpty()) {
//            for (String newPartInKt : allPartsInKt) {
//                System.out.print("-------- " + newPartInKt);
//                if (checkPartInSlot(newPartInKt, slot)) {
//                    logDebug("-------- Located in slot");
//                    nbOfOccupiedSlots++;
//                } else {
//                    logDebug("-------- Not located in slot");
//                }
//            }
//            //part_in_kt_found=true;
//        } else {
//            addToInspectionResultJTextPane("&nbsp;&nbsp;No part_in_kt of type " + partInKt + " was found in the database<br>");
//            //part_in_kt_found=false;
//        }
//        return nbOfOccupiedSlots;
//    }
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

//    private Boolean checkPartInSlot(String partName, Slot slot) throws SQLException {
//        Boolean isPartInSlot = false;
//        PoseType posePart = getPose(partName);
//        if (null == posePart) {
//            throw new IllegalStateException("getPose(" + partName + ") returned null");
//        }
//        PointType posePartPoint = posePart.getPoint();
//        if (null == posePartPoint) {
//            throw new IllegalStateException("getPose(" + partName + ") returned pose with null point property");
//        }
////        posePart = visionToRobotPose(posePart);
//        double partX = posePartPoint.getX();
//        double partY = posePartPoint.getY();
//        PointType slotPosePoint = slot.getSlotPose().getPoint();
//        if (null == slotPosePoint) {
//            throw new IllegalStateException("slot has pose with null point property : slot=" + slot);
//        }
//        double slotX = slotPosePoint.getX();
//        double slotY = slotPosePoint.getY();
//        logDebug(":(" + partX + "," + partY + ")");
//        double distance = Math.hypot(partX - slotX, partY - slotY);
//        System.out.print("-------- Distance = " + distance);
//        // compare finalres with a specified tolerance value of 6.5 mm
//        if (distance < kitInspectDistThreshold) {
//            isPartInSlot = true;
//            // logDebug("---- Part " + partName + " : (" + partX + "," + partY + ")");
//            // logDebug("---- Slot " + slot.getSlotName() + " : (" + slotX + "," + slotY + ")");
//            // logDebug("---- Distance between part and slot = " + dist);
//        }
//        return isPartInSlot;
//    }

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

//    @SuppressWarnings("SameParameterValue")
//    private void setFakeTakePart(boolean _newValue) {
//        if (null != parentExecutorJPanel) {
//            this.parentExecutorJPanel.setForceFakeTakeFlag(_newValue);
//        }
//    }

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
        if (null != kitInspectionJInternalFrame && null != aprsSystem) {
            aprsSystem.runOnDispatchThread(() -> updateKitImageLabel("init", "Building kit"));
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
        final String partTool;
        if (partToolMap.containsKey(partName)) {
            partTool = partToolMap.get(partName);
        } else {
            String partTypeName = partNameToPartType(partName);
            if (partToolMap.containsKey(partTypeName)) {
                partTool = partToolMap.get(partTypeName);
            } else if (partTypeName.length() > 6 && (partTypeName.endsWith("_in_pt") || partTypeName.endsWith("_in_kt"))) {
                String trimmedPartTypeName = partTypeName.substring(0, partTypeName.length() - 6);
                if (partToolMap.containsKey(trimmedPartTypeName)) {
                    partTool = partToolMap.get(trimmedPartTypeName);
                } else {
                    partTool = null;
                }
            } else {
                partTool = null;
            }
        }
        if (null != partTool && !Objects.equals(partTool, getExpectedToolName())) {
            switchTool(Action.newSingleArgAction(
                    SWITCH_TOOL,
                    partTool
            ), out);
            addMoveToLookForPosition(out, true);
        }
        if (skipMissingParts) {
            if (null != nextPlacePartAction) {
                String slot = nextPlacePartAction.getArgs()[0];
                PoseType slotPose = getPose(slot);
                if (null == slotPose) {
                    setLastTakenPart(null);
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
        addMarkerCommand(out, markerMsg, (CRCLCommandWrapper ccw) -> {
            logDebug(markerMsg + " at " + new Date());
            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
        });
        setLastTakenPart(partName);
        //inspectionList.add(partName);
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
    }

    private void recordSkipTakePart(String partName, @Nullable PoseType pose) throws IllegalStateException {
        setLastTakenPart(null);
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
        addMessageCommand(out, "fake-take-part " + partName + " action=" + lastIndex + " crclNumber=" + crclNumber.get());

        PoseType pose = getPose(partName);
        if (null == pose) {
            LOGGER.log(Level.WARNING,
                    "no pose for {0} poseCache.keySet() = {1}, clearPoseCacheTrace={2}",
                    new Object[]{partName, poseCache.keySet(), Utils.traceToString(clearPoseCacheTrace)});
//            LOGGER.log(Level.WARNING, 
//                    "no pose for " + partName + " poseCache.keySet() =" + poseCache.keySet() + ", clearPoseCacheTrace=" + Utils.traceToString(clearPoseCacheTrace));
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
        setLastTakenPart(partName);
        if (partName.indexOf('_') > 0) {
            TakenPartList.add(partName);
        }
    }

//    private void takePartRecovery(String partName, List<MiddleCommandType> out) throws SQLException, CRCLException, PmException {
//        checkDbReady();
//
//        if (partName.indexOf('_') < 0) {
//            throw new IllegalArgumentException("partName must contain an underscore: partName=" + partName);
//        }
//        checkSettings();
//        addMessageCommand(out, "take-part-recovery " + partName + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
//
//        PoseType pose = getPose(partName);
//        if (takeSnapshots) {
//            takeSnapshots("plan", "take-part-recovery-" + partName + "", pose, partName);
//        }
//        if (null == pose) {
//            if (skipMissingParts) {
//                setLastTakenPart(null);
//                return;
//            } else {
//                throw new IllegalStateException("getPose(" + partName + ") returned null");
//            }
//        }
//
//        pose = visionToRobotPose(pose);
//        returnPosesByName.put(partName, pose);
//        pose.setXAxis(xAxis);
//        pose.setZAxis(zAxis);
//        takePartByPose(out, pose, partName);
//
//        String markerMsg = "took part " + partName;
//        addMarkerCommand(out, markerMsg, x -> {
//            logDebug(markerMsg + " at " + new Date());
//            addToInspectionResultJTextPane("&nbsp;&nbsp;" + markerMsg + " at " + new Date() + "<br>");
//        });
//        setLastTakenPart(partName);
//        if (partName.indexOf('_') > 0) {
//            TakenPartList.add(partName);
//        }
//    }

    private volatile boolean getPoseFailedOnce = false;


//    private List<String> getAllPartsInKt(String name) throws SQLException, IOException {
//
//        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
//
//        if (null == qs) {
//            throw new IllegalStateException("QuerySet for database not initialized.(null)");
//        }
//        List<String> partsInKtList = new ArrayList<>(qs.getAllPartsInKt(name));
//
//        return partsInKtList;
//    }

    private volatile @Nullable
    PoseType lastTestApproachPose = null;

    private final ConcurrentMap<String, String> toolChangerJointValsMap
            = new ConcurrentHashMap<>();

    public void putToolChangerJointVals(String key, String value) {
        toolChangerJointValsMap.put(key, value);
    }

    private @Nullable
    String getToolChangerJointVals(String key) {
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
    public void testPartPositionByPose(List<MiddleCommandType> cmds, PoseType pose) {

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

        PoseType approachPose2 = addZToPose(pose, approachZOffset / 5.0);

        lastTestApproachPose = approachPose;

        final PoseType takePose = requireNonNull(copy(pose), "copy(pose)");
        final PointType posePoint = requireNonNull(pose.getPoint(), "posePoint");
        final PointType takePosePoint = requireNonNull(takePose.getPoint(), "takePosePoint");
        takePosePoint.setZ(posePoint.getZ() + takeZOffset);

        addSetFastTestSpeed(cmds);

        addMoveTo(cmds, approachPose, false, "testPartPositionByPose.approachPose");

        addSettleDwell(cmds);

        addSetSlowTestSpeed(cmds);

        addMoveTo(cmds, approachPose2, true, "testPartPositionByPose.approachPose2");

        addSettleDwell(cmds);

    }

    private static PoseType addZToPose(PoseType pose, double zOffset) {
        try {
            PmCartesian cart = new PmCartesian(0, 0, -zOffset);
            PmPose pmPose = CRCLPosemath.toPmPose(pose);
            PmCartesian newTranPos = new PmCartesian();
            Posemath.pmPoseCartMult(pmPose, cart, newTranPos);
            PmPose approachPmPose = new PmPose(newTranPos, pmPose.rot);
            PoseType approachPose = CRCLPosemath.toPose(approachPmPose);
            return approachPose;
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                LOGGER.log(Level.SEVERE, "pose=" + CRCLPosemath.poseToString(pose) + ",zOffset=" + zOffset, ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private void addCheckedOpenGripper(List<MiddleCommandType> cmds) {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        final String addCheckedOpenGripperLastPartTaken = getLastTakenPart();
        int addCheckedOpenGripperLastIndex = getLastIndex();

        String imgLabel;
        if (null != lastActionsList) {
            Action act = lastActionsList.get(addCheckedOpenGripperLastIndex);
            imgLabel = "openGripper" + addCheckedOpenGripperLastIndex + act.asPddlLine() + "partTaken=" + addCheckedOpenGripperLastPartTaken;
        } else {
            imgLabel = "openGripper" + addCheckedOpenGripperLastIndex + "partTaken=" + addCheckedOpenGripperLastPartTaken;
        }
        addOptionalOpenGripper(cmds, (CRCLCommandWrapper ccw) -> {
            AprsSystem af = aprsSystem;
            assert (af != null) : "af == null : @AssumeAssertion(nullness)";

            openGripperCheck(af, imgLabel, addCheckedOpenGripperLastPartTaken, dropOffMin, debug);
        });
    }

    private static void openGripperCheck(AprsSystem af, String imgLabel, @Nullable String addCheckedOpenGripperLastPartTaken, final double dropOffMin, boolean debug) throws IllegalStateException {
        if (af.isObjectViewSimulated()) {
            double distToPart = af.getClosestRobotPartDistance();
            if (distToPart < dropOffMin) {
                PhysicalItem closestPart = af.getClosestRobotPart();
                af.takeSnapshots(imgLabel);
                String errString
                        = "Can't drop off part " + addCheckedOpenGripperLastPartTaken + " when distance to another part (" + (closestPart != null ? closestPart.getFullName() : null) + ") of " + distToPart + "  less than  " + dropOffMin;
                double recheckDistance = af.getClosestRobotPartDistance();
                if (debug) {
                    LOGGER.log(Level.INFO, "recheckDistance = {0}", recheckDistance);
                }
                af.setTitleErrorString(errString);
                af.pause();
                throw new IllegalStateException(errString);
            }
        }
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

    private Map<String, String> partToolMap = new ConcurrentHashMap<>();

    /**
     * Get the value of partToolMap
     *
     * @return the value of partToolMap
     */
    public Map<String, String> getPartToolMap() {
        return partToolMap;
    }

    /**
     * Set the value of partToolMap
     *
     * @param partToolMap new value of partToolMap
     */
    public void setPartToolMap(Map<String, String> partToolMap) {
        this.partToolMap = partToolMap;
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
     */
    public void takePartByPose(List<MiddleCommandType> cmds, PoseType pose, @Nullable String name) {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        try {

            PoseType poseWithToolOffset = CRCLPosemath.multiply(pose, toolOffsetPose);

            logToolOffsetInfo(cmds, pose, poseWithToolOffset);

            addOpenGripper(cmds);

            checkSettings();
            PoseType approachPose = addZToPose(poseWithToolOffset, approachZOffset);
            lastTestApproachPose = null;

            final PoseType takePose = requireNonNull(copy(poseWithToolOffset), "takePose");
            final PointType poseWithToolOffsetPoint = requireNonNull(poseWithToolOffset.getPoint(), "poseWithToolOffsetPoint");
            final PointType takePosePoint = requireNonNull(takePose.getPoint(), "takePosePoint");
            takePosePoint.setZ(poseWithToolOffsetPoint.getZ() + takeZOffset);

            addSetFastSpeed(cmds);

            addMoveTo(cmds, approachPose, false, "takePartByPose.approachPose." + name);

            addSetSlowSpeed(cmds);

            addMoveTo(cmds, takePose, true, "takePartByPose.takePose." + name);

            addSettleDwell(cmds);

            addOptionalCloseGripper(cmds, (CRCLCommandWrapper ccw) -> {
                AprsSystem af = aprsSystem;
                assert (af != null) : "af == null : @AssumeAssertion(nullness)";
                if (af.isObjectViewSimulated()) {
                    double distToPart = af.getClosestRobotPartDistance();
                    if (distToPart > pickupDistMax) {
                        PointType currentPoint = af.getCurrentPosePoint();
                        if (null == currentPoint) {
                            throw new IllegalStateException("null == currentPoint");
                        }
                        PointType uncorrectedPoint = af.convertRobotToVisionPoint(currentPoint);
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
//                    setFakeTakePart(false);
                        return;
                    }
                }
                if (getForceFakeTakeFlag()) {
                    SetEndEffectorType seeCmd = (SetEndEffectorType) ccw.getWrappedCommand();
                    seeCmd.setSetting(1.0);
//                setFakeTakePart(false);
                }
            }, "takePartByPose." + name);

            addSettleDwell(cmds);
            addSetFastSpeed(cmds);
            addMoveTo(cmds, approachPose, true, "takePartByPose.approachPose.return." + name);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "pose=" + CRCLPosemath.poseToString(pose) + ",name=" + name, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
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

        final PoseType takePose = requireNonNull(copy(pose), "takePose");
        final PointType takePosePoint = requireNonNull(takePose.getPoint(), "takePosePoint");
        final PointType posePoint = requireNonNull(pose.getPoint(), "posePoint");
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
        return checkPose(pose, false);
    }

    private boolean checkPose(PoseType pose, boolean ignoreCartTran) {
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        return aprsSystem.checkPose(pose, ignoreCartTran, true);
    }

    private void checkSettings() {
        if (settingsChecked || options == null) {
            return;
        }
        settingsChecked = true;
        Map<String, String> optionsMap = this.options;
        loadOptionsMap(optionsMap, true);
    }

    public void loadOptionsMap(Map<String, String> optionsMap, boolean doCheckPose) throws NumberFormatException {
        String rpyString = optionsMap.get("rpy");
        if (null != rpyString && rpyString.length() > 0) {
            try {
                String rpyFields[] = rpyString.split("[, \t]+");
                if (rpyFields.length == 3) {
                    rpy = new PmRpy();
                    rpy.r = Math.toRadians(Double.parseDouble(rpyFields[0]));
                    rpy.p = Math.toRadians(Double.parseDouble(rpyFields[1]));
                    rpy.y = Math.toRadians(Double.parseDouble(rpyFields[2]));
                    PoseType pose = CRCLPosemath.toPoseType(new PmCartesian(), rpy);
                    if (doCheckPose) {
                        if (!checkPose(pose, true)) {
                            throw new RuntimeException("invalid pose passed with rpy setting :" + CRCLPosemath.poseToString(pose));
                        }
                    }
                    VectorType xAxisVector = pose.getXAxis();
                    if (null == xAxisVector) {
                        throw new IllegalStateException("null == xAxisVector");
                    }
                    xAxis = xAxisVector;
                    VectorType zAxisVector = pose.getZAxis();
                    if (null == zAxisVector) {
                        throw new IllegalStateException("null == zAxisVector");
                    }
                    zAxis = zAxisVector;
                } else {
                    throw new Exception("bad rpyString = \"" + rpyString + "\", rpyFields=" + Arrays.toString(rpyFields));
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
        if (xAxis == null) {
            xAxis = vector(1.0, 0.0, 0.0);
            zAxis = vector(0.0, 0.0, -1.0);
        }
        String approachZOffsetString = optionsMap.get("approachZOffset");
        if (null != approachZOffsetString && approachZOffsetString.length() > 0) {
            try {
                approachZOffset = Double.parseDouble(approachZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String approachToolChangerZOffsetString = optionsMap.get("approachToolChangerZOffset");
        if (null != approachToolChangerZOffsetString && approachToolChangerZOffsetString.length() > 0) {
            try {
                approachToolChangerZOffset = Double.parseDouble(approachToolChangerZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String placeZOffsetString = optionsMap.get("placeZOffset");
        if (null != placeZOffsetString && placeZOffsetString.length() > 0) {
            try {
                placeZOffset = Double.parseDouble(placeZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String takeZOffsetString = optionsMap.get("takeZOffset");
        if (null != takeZOffsetString && takeZOffsetString.length() > 0) {
            try {
                takeZOffset = Double.parseDouble(takeZOffsetString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String joint0DiffToleranceString = optionsMap.get("joint0DiffTolerance");
        if (null != joint0DiffToleranceString && joint0DiffToleranceString.length() > 0) {
            try {
                joint0DiffTolerance = Double.parseDouble(joint0DiffToleranceString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String settleDwellTimeString = optionsMap.get("settleDwellTime");
        if (null != settleDwellTimeString && settleDwellTimeString.length() > 0) {
            try {
                settleDwellTime = Double.parseDouble(settleDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String toolChangerDwellTimeString = optionsMap.get("toolChangerDwellTime");
        if (null != toolChangerDwellTimeString && toolChangerDwellTimeString.length() > 0) {
            try {
                toolChangerDwellTime = Double.parseDouble(toolChangerDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String lookDwellTimeString = optionsMap.get("lookDwellTime");
        if (null != lookDwellTimeString && lookDwellTimeString.length() > 0) {
            try {
                lookDwellTime = Double.parseDouble(lookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String skipLookDwellTimeString = optionsMap.get("skipLookDwellTime");
        if (null != skipLookDwellTimeString && skipLookDwellTimeString.length() > 0) {
            try {
                skipLookDwellTime = Double.parseDouble(skipLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String afterMoveToLookForDwellTimeString = optionsMap.get("afterMoveToLookForDwellTime");
        if (null != afterMoveToLookForDwellTimeString && afterMoveToLookForDwellTimeString.length() > 0) {
            try {
                afterMoveToLookForDwellTime = Double.parseDouble(afterMoveToLookForDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        // afterMoveToLookForDwellTime
        String firstLookDwellTimeString = optionsMap.get("firstLookDwellTime");
        if (null != firstLookDwellTimeString && firstLookDwellTimeString.length() > 0) {
            try {
                firstLookDwellTime = Double.parseDouble(firstLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String lastLookDwellTimeString = optionsMap.get("lastLookDwellTime");
        if (null != lastLookDwellTimeString && lastLookDwellTimeString.length() > 0) {
            try {
                lastLookDwellTime = Double.parseDouble(lastLookDwellTimeString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String fastTransSpeedString = optionsMap.get("fastTransSpeed");
        if (null != fastTransSpeedString && fastTransSpeedString.length() > 0) {
            try {
                fastTransSpeed = Double.parseDouble(fastTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String testTransSpeedString = optionsMap.get("testTransSpeed");
        if (null != testTransSpeedString && testTransSpeedString.length() > 0) {
            try {
                testTransSpeed = Double.parseDouble(testTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String rotSpeedString = optionsMap.get("rotSpeed");
        if (null != rotSpeedString && rotSpeedString.length() > 0) {
            try {
                rotSpeed = Double.parseDouble(rotSpeedString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String jointSpeedString = optionsMap.get("jointSpeed");
        if (null != jointSpeedString && jointSpeedString.length() > 0) {
            try {
                jointSpeed = Double.parseDouble(jointSpeedString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String jointAccelString = optionsMap.get("jointAccel");
        if (null != jointAccelString && jointAccelString.length() > 0) {
            try {
                jointAccel = Double.parseDouble(jointAccelString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String kitInspectDistThresholdString = optionsMap.get("kitInspectDistThreshold");
        if (null != kitInspectDistThresholdString && kitInspectDistThresholdString.length() > 0) {
            try {
                kitInspectDistThreshold = Double.parseDouble(kitInspectDistThresholdString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }

        String slowTransSpeedString = optionsMap.get("slowTransSpeed");
        if (null != slowTransSpeedString && slowTransSpeedString.length() > 0) {
            try {
                slowTransSpeed = Double.parseDouble(slowTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String verySlowTransSpeedString = optionsMap.get("verySlowTransSpeed");
        if (null != verySlowTransSpeedString && verySlowTransSpeedString.length() > 0) {
            try {
                verySlowTransSpeed = Double.parseDouble(verySlowTransSpeedString);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.SEVERE, "", numberFormatException);
            }
        }
        String takePartArgIndexString = optionsMap.get("takePartArgIndex");
        if (null != takePartArgIndexString && takePartArgIndexString.length() > 0) {
            int newTakePartArgIndex = Integer.parseInt(takePartArgIndexString);
            this.setTakePartArgIndex(newTakePartArgIndex);
        }
        String placePartSlotArgIndexString = optionsMap.get("placePartSlotArgIndex");
        if (null != placePartSlotArgIndexString && placePartSlotArgIndexString.length() > 0) {
            this.placePartSlotArgIndex = Integer.parseInt(placePartSlotArgIndexString);
        }
        String takeSnapshotsString = optionsMap.get("takeSnapshots");
        if (null != takeSnapshotsString && takeSnapshotsString.length() > 0) {
            takeSnapshots = Boolean.parseBoolean(takeSnapshotsString);
        }
        String pauseInsteadOfRecoverString = optionsMap.get("pauseInsteadOfRecover");
        if (null != pauseInsteadOfRecoverString && pauseInsteadOfRecoverString.length() > 0) {
            pauseInsteadOfRecover = Boolean.parseBoolean(pauseInsteadOfRecoverString);
        }
        String doInspectKitString = optionsMap.get("doInspectKit");
        if (null != doInspectKitString && doInspectKitString.length() > 0) {
            doInspectKit = Boolean.parseBoolean(doInspectKitString);
        }
        String requireNewPosesString = optionsMap.get("requireNewPoses");
        if (null != requireNewPosesString && requireNewPosesString.length() > 0) {
            requireNewPoses = Boolean.parseBoolean(requireNewPosesString);
        }
        String skipMissingPartsString = optionsMap.get("skipMissingParts");
        if (null != skipMissingPartsString && skipMissingPartsString.length() > 0) {
            skipMissingParts = Boolean.parseBoolean(skipMissingPartsString);
        }
        String useJointMovesForToolHolderApproachString = optionsMap.get("useJointMovesForToolHolderApproach");
        if (null != useJointMovesForToolHolderApproachString && useJointMovesForToolHolderApproachString.length() > 0) {
            useJointMovesForToolHolderApproach = Boolean.parseBoolean(useJointMovesForToolHolderApproachString);
        }
        String visionCycleNewDiffThresholdString = optionsMap.get("visionCycleNewDiffThreshold");
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
        final PoseType poseOut = requireNonNull(copy(poseIn), "copy(poseIn)");
        final PointType outPoint = requireNonNull(poseOut.getPoint(), "outPoint");
        outPoint.setZ(Math.min(limit, outPoint.getZ() + offset));
        return poseOut;
    }

    private void addMoveUpFromCurrent(List<MiddleCommandType> cmds, double offset, double limit) {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        MessageType origMessageCmd = new MessageType();
        origMessageCmd.setMessage("moveUpFromCurrent" + " action=" + lastIndex + " crclNumber=" + crclNumber.get() + ",offset=" + offset + ",limit=" + limit);
        addOptionalCommand(origMessageCmd, cmds, (CRCLCommandWrapper wrapper) -> {
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

    private double toolChangerDwellTime = 0.40;

    private void addOpenToolChanger(List<MiddleCommandType> cmds) {
        addSettleDwell(cmds);
        addDwell(cmds, toolChangerDwellTime);
        OpenToolChangerType openToolChangerCmd = new OpenToolChangerType();
        setCommandId(openToolChangerCmd);
        cmds.add(openToolChangerCmd);
        addDwell(cmds, toolChangerDwellTime);
//        setCurrentToolName(currentToolName);
    }

    private void addCloseToolChanger(List<MiddleCommandType> cmds) {
        addSettleDwell(cmds);
        addDwell(cmds, toolChangerDwellTime);
        CloseToolChangerType openToolChangerCmd = new CloseToolChangerType();
        setCommandId(openToolChangerCmd);
        cmds.add(openToolChangerCmd);
        addDwell(cmds, toolChangerDwellTime);
    }

    private void addMoveTo(List<MiddleCommandType> cmds, PoseType pose, boolean straight, String message) {
        addMessageCommand(cmds, message);
        checkEndPoseToleranceSetting(cmds);
        MoveToType moveCmd = new MoveToType();
        setCommandId(moveCmd);
        if (!checkPose(pose)) {
            try {
                takeSimViewSnapshot("invalid pose", pose, message);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            System.err.println("message=" + message);
            boolean recheckPose = checkPose(pose);
            println("recheckPose = " + recheckPose);
            boolean recheckPose2 = checkPose(pose);
            println("recheckPose2 = " + recheckPose2);
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
            LOGGER.warning("getLookForXYZ : null == options");
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
        PmCartesian cart = new PmCartesian(Double.parseDouble(lookForXYZFields[0]), Double.parseDouble(lookForXYZFields[1]), Double.parseDouble(lookForXYZFields[2]));
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        if (!aprsSystem.isWithinLimits(cart)) {

            final String errmsg = "lookforXYZSring=" + lookforXYZSring + ", cart=" + cart + " not within limits";
            try {
                takeSimViewSnapshot(errmsg, cart, "lookForXYZ");
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            throw new IllegalStateException(errmsg);
        }
        return CRCLPosemath.toPointType(cart);
    }

    private volatile boolean atLookForPosition = false;

    public void addMoveToLookForPosition(List<MiddleCommandType> out, boolean firstAction) {

        String useLookForJointString = options.get("useJointLookFor");
        boolean useLookForJoint = (null != useLookForJointString && useLookForJointString.length() > 0 && Boolean.parseBoolean(useLookForJointString));
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
            double lookForJointVals[] = jointValStringToArray(lookForJointsString);
            checkJointToleranceSetting(out);
//            addPrepJointMove(lookForJointVals, out);
            addJointMove(out, lookForJointVals, 1.0, 1, lookForJointVals.length);
            addJointMove(out, lookForJointVals, 1.0, 0, 1);
        }
        addMarkerCommand(out, "set atLookForPosition true", x -> {
            atLookForPosition = true;
        });
    }

    private @Nullable
    JointPositionsTolerancesType jointPositionsTolerancesType = null;

    public @Nullable
    JointPositionsTolerancesType getJointPositionsTolerancesType() {
        return jointPositionsTolerancesType;
    }

    public void setJointPositionsTolerancesType(JointPositionsTolerancesType jointPositionsTolerancesType) {
        this.jointPositionsTolerancesType = jointPositionsTolerancesType;
    }

    private volatile boolean jointTolerancesChecked = false;

    private void checkJointToleranceSetting(List<MiddleCommandType> out) {
        if (!jointTolerancesChecked) {
            if (null == jointPositionsTolerancesType) {
                String jointTolerancesString = options.get("jointTolerances");
                if (null != jointTolerancesString) {
                    double jointTolerancesVals[] = jointValStringToArray(jointTolerancesString);
                    JointPositionsTolerancesType newJointTolerances = new JointPositionsTolerancesType();
                    List<JointPositionToleranceSettingType> newSettingsList = new ArrayList<>();
                    for (int i = 0; i < jointTolerancesVals.length; i++) {
                        double jointTolerancesVal = jointTolerancesVals[i];
                        final JointPositionToleranceSettingType settingI = new JointPositionToleranceSettingType();
                        settingI.setJointNumber(i + 1);
                        settingI.setJointPositionTolerance(jointTolerancesVals[i]);
                        newSettingsList.add(settingI);
                    }
                    final List<JointPositionToleranceSettingType> newJointTolerancesSetting
                            = newJointTolerances.getSetting();
                    newJointTolerancesSetting.clear();
                    newJointTolerancesSetting.addAll(newSettingsList);
                    final SetDefaultJointPositonsTolerancesType setDefaultJointPositonsTolerancesCommand
                            = new SetDefaultJointPositonsTolerancesType();
                    setDefaultJointPositonsTolerancesCommand.setJointTolerances(newJointTolerances);
                    setCommandId(setDefaultJointPositonsTolerancesCommand);
                    out.add(setDefaultJointPositonsTolerancesCommand);
                    this.jointPositionsTolerancesType = newJointTolerances;
                }
            } else {
                final SetDefaultJointPositonsTolerancesType setDefaultJointPositonsTolerancesCommand
                        = new SetDefaultJointPositonsTolerancesType();
                setDefaultJointPositonsTolerancesCommand.setJointTolerances(jointPositionsTolerancesType);
                setCommandId(setDefaultJointPositonsTolerancesCommand);
                out.add(setDefaultJointPositonsTolerancesCommand);
            }
            jointTolerancesChecked = true;
        }
    }

    private volatile boolean endPoseTolerancesChecked = false;

    private @Nullable
    PoseToleranceType endPoseTolerance = null;

    public @Nullable
    PoseToleranceType getEndPoseTolerance() {
        return endPoseTolerance;
    }

    public void setEndPoseTolerance(PoseToleranceType endPoseTolerance) {
        this.endPoseTolerance = endPoseTolerance;
    }

    private void checkEndPoseToleranceSetting(List<MiddleCommandType> out) {
        if (!endPoseTolerancesChecked) {
            if (null == endPoseTolerance) {
                String useEndPoseToleranceString = options.get("useEndPoseTolerance");
                boolean useEndPoseTolerance = Boolean.parseBoolean(useEndPoseToleranceString);
                if (useEndPoseTolerance) {
                    PoseToleranceType newEndPoseTolerance = new PoseToleranceType();
                    String endPoseXPointToleranceString = options.getOrDefault("endPoseXPointTolerance", "10.0");
                    double endPoseXPointToleranceValue = Double.parseDouble(endPoseXPointToleranceString);
                    newEndPoseTolerance.setXPointTolerance(endPoseXPointToleranceValue);

                    String endPoseYPointToleranceString = options.getOrDefault("endPoseYPointTolerance", "10.0");
                    double endPoseYPointToleranceValue = Double.parseDouble(endPoseXPointToleranceString);
                    newEndPoseTolerance.setYPointTolerance(endPoseYPointToleranceValue);

                    String endPoseZPointToleranceString = options.getOrDefault("endPoseZPointTolerance", "10.0");
                    double endPoseZPointToleranceValue = Double.parseDouble(endPoseZPointToleranceString);
                    newEndPoseTolerance.setZPointTolerance(endPoseZPointToleranceValue);

                    String endPoseXAxisToleranceString = options.getOrDefault("endPoseXAxisTolerance", "10.0");
                    double endPoseXAxisToleranceValue = Double.parseDouble(endPoseXAxisToleranceString);
                    newEndPoseTolerance.setXAxisTolerance(endPoseZPointToleranceValue);

                    String endPoseYAxisToleranceString = options.getOrDefault("endPoseZAxisTolerance", "10.0");
                    double endPoseYAxisToleranceValue = Double.parseDouble(endPoseXAxisToleranceString);
                    newEndPoseTolerance.setZAxisTolerance(endPoseZPointToleranceValue);

                    final SetEndPoseToleranceType setEndPoseToleranceCommand
                            = new SetEndPoseToleranceType();
                    setEndPoseToleranceCommand.setTolerance(newEndPoseTolerance);
                    setCommandId(setEndPoseToleranceCommand);
                    out.add(setEndPoseToleranceCommand);
                    this.endPoseTolerance = newEndPoseTolerance;
                }
            } else {
                final SetEndPoseToleranceType setEndPoseToleranceCommand
                        = new SetEndPoseToleranceType();
                setEndPoseToleranceCommand.setTolerance(endPoseTolerance);
                setCommandId(setEndPoseToleranceCommand);
                out.add(setEndPoseToleranceCommand);
            }
            endPoseTolerancesChecked = true;
        }
    }

//    private void addPrepJointMove(double[] jointVals, List<MiddleCommandType> out) {
//        double joint0 = jointVals[0];
//        double joint0diff = joint0 - expectedJoint0Val;
//        checkJointToleranceSetting(out);
//        if (toolHolderOperationEnabled
//                && Double.isFinite(expectedJoint0Val)
//                && joint0DiffTolerance > 0
//                && Math.abs(joint0diff) > joint0DiffTolerance) {
//            double jointValsCopy[] = Arrays.copyOf(jointVals, jointVals.length);
//            jointValsCopy[0] = expectedJoint0Val;
//            addMessageCommand(out, "addJointPrepMove:joint0=" + joint0 + ",expectedJoint0Val=" + expectedJoint0Val + ",joint0diff=" + joint0diff + ",joint0DiffTolerance=" + joint0DiffTolerance);
//            addJointMove(out, jointValsCopy, 1.0);
//        }
//    }

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

//    private void addJointMove(List<MiddleCommandType> out, double jointVals[], double speedScale) {
//        addJointMove(out, jointVals, speedScale, 0, jointVals.length);
//    }

    private void addJointMove(List<MiddleCommandType> out, double jointVals[], double speedScale, int start, int end) {
        checkJointToleranceSetting(out);
        ActuateJointsType ajCmd = new ActuateJointsType();
        setCommandId(ajCmd);
        if (start < 0 || start > jointVals.length || start >= end) {
            throw new IllegalArgumentException("start=" + start + ", jointVals.length=" + jointVals.length + ",end=" + end);
        }
        if (end < 0 || end > jointVals.length) {
            throw new IllegalArgumentException("jointVals.length=" + jointVals.length + ",end=" + end);
        }
        List<ActuateJointType> newActuateJointsList = new ArrayList<>();
        for (int i = start; i < end && i < jointVals.length; i++) {
            ActuateJointType aj = new ActuateJointType();
            JointSpeedAccelType jsa = new JointSpeedAccelType();
            jsa.setJointAccel(jointAccel * speedScale);
            jsa.setJointSpeed(jointSpeed * speedScale);
            aj.setJointDetails(jsa);
            aj.setJointNumber(i + 1);
            aj.setJointPosition(jointVals[i]);
            newActuateJointsList.add(aj);
        }
        final List<ActuateJointType> ajCmdActuateJoint = ajCmd.getActuateJoint();
        ajCmdActuateJoint.clear();
        ajCmdActuateJoint.addAll(newActuateJointsList);
        out.add(ajCmd);
        atLookForPosition = false;
        if (toolHolderOperationEnabled) {
            expectedJoint0Val = jointVals[0];
            logDebug("addJointMove: expectedJoint0Val = " + expectedJoint0Val);
        }
    }

    private volatile @Nullable
    Thread clearPoseCacheThread = null;
    private volatile StackTraceElement clearPoseCacheTrace@Nullable []  = null;

    public void clearPoseCache() {
        clearPoseCacheThread = Thread.currentThread();
        clearPoseCacheTrace = clearPoseCacheThread.getStackTrace();
        synchronized (poseCache) {
            poseCache.clear();
            itemCache.clear();
        }
    }

    private volatile @Nullable
    Thread putPoseCacheThread = null;
    private volatile StackTraceElement putPoseCacheTrace@Nullable []  = null;

    public void putPoseCache(String name, PoseType pose) {
        putPoseCacheThread = Thread.currentThread();
        putPoseCacheTrace = putPoseCacheThread.getStackTrace();
        synchronized (poseCache) {
            final PoseType poseCopy = requireNonNull(copy(pose), "poseCopy");
            poseCache.put(name, poseCopy);
            PhysicalItem itemCopy = PhysicalItem.newPhysicalItemNamePoseVisionCycle(name, poseCopy, 0);
            itemCache.put(name, itemCopy);
        }
    }

    public Map<String, PoseType> getPoseCache() {
        synchronized (poseCache) {
            return Collections.unmodifiableMap(poseCache);
        }
    }

    public Map<String, PhysicalItem> getItemCache() {
        synchronized (itemCache) {
            return Collections.unmodifiableMap(new HashMap<>(itemCache));
        }
    }

    private boolean checkAtLookForPosition() {
        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        checkSettings();
        String useLookForJointString = options.get("useJointLookFor");
        boolean useLookForJoint = (null != useLookForJointString && useLookForJointString.length() > 0 && Boolean.parseBoolean(useLookForJointString));
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
            Iterable<JointStatusType> jointStatusIterable = CRCLUtils.getNonNullJointStatusIterable(jss);
            assert (null != lookForJointsString) : "@AssumeAssertion(nullness)";
            String jointPosStrings[] = lookForJointsString.split("[,]+");
            for (int i = 0; i < jointPosStrings.length; i++) {
                final int number = i + 1;
                JointStatusType js
                        = StreamSupport.stream(jointStatusIterable.spliterator(), false)
                                .filter(x -> x.getJointNumber() == number)
                                .findFirst()
                                .orElse(null);
                if (null == js) {
                    return false;
                }
                double jpos = Double.parseDouble(jointPosStrings[i]);
                double statusJpos = requireNonNull(js.getJointPosition(), "js.getJointPosition()");
                if (Math.abs(jpos - statusJpos) > 2.0) {
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

    private void lookForParts(Action action, List<MiddleCommandType> out, boolean firstAction, boolean lastAction, int startAbortCount) {

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
        final String actionsString = (action.getArgs().length == 1) ? action.getArgs()[0] : "";
        if (!atLookForPosition) {
            addMarkerCommand(out, "beforeMoveToLookPosition.lookForParts-" + actionsString, x -> {
                takeSimViewSnapshot("beforeMoveToLookPosition.lookForParts-" + actionsString, null);
            });
            addMoveToLookForPosition(out, firstAction);
            addMarkerCommand(out, "afterMoveToLookPosition.lookForParts-" + actionsString, x -> {
                takeSimViewSnapshot("afterMoveToLookPosition.lookForParts-" + actionsString, null);
            });
            addAfterMoveToLookForDwell(out, firstAction, lastAction);
            if (null == externalPoseProvider) {
                addMarkerCommand(out, "enableVisionToDatabaseUpdates", x -> {
                    setEnableVisionToDatabaseUpdates(
                            enableDatabaseUpdates,
                            immutableRequiredPartsMap);
                    takeSimViewSnapshot("afterMoveToLookDwell.lookForParts-" + actionsString, null);
                });
            }
        } else if (null == externalPoseProvider) {
            addMarkerCommand(out, "enableVisionToDatabaseUpdates", x -> {
                setEnableVisionToDatabaseUpdates(
                        enableDatabaseUpdates,
                        immutableRequiredPartsMap);
            });
        } //            addSkipLookDwell(out, firstAction, lastAction);
        if (null == externalPoseProvider) {
            addMarkerCommand(out, "lookForParts.waitForCompleteVisionUpdates", x -> {
                try {
                    waitForCompleteVisionUpdates("lookForParts", immutableRequiredPartsMap, WAIT_FOR_VISION_TIMEOUT, startAbortCount);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                    throw new RuntimeException(ex);
                }
            });
        }
        addMarkerCommand(out, "lookForParts-" + actionsString, x -> {
            takeSimViewSnapshot("lookForParts-" + actionsString + ".physicalItems", physicalItems);
        });
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
//            double joint0Diff = expectedJoint0Val - jointVals[0];
//            logDebug("addGotoToolChangerApproachByName: jointVals[0] = " + jointVals[0]);
//            logDebug("addGotoToolChangerApproachByName: expectedJoint0Val = " + expectedJoint0Val);
//            logDebug("addGotoToolChangerApproachByName: diff0 = " + joint0Diff);
//            logDebug("addGotoToolChangerApproachByName: joint0DiffTolerance = " + joint0DiffTolerance);
//            if (!Double.isFinite(expectedJoint0Val) || Math.abs(joint0Diff) > joint0DiffTolerance) {
//                if (null != lookForJointsString && lookForJointsString.length() > 0) {
//                    double lookForJointVals[] = jointValStringToArray(lookForJointsString);
//                    addPrepJointMove(lookForJointVals, out);
//                    lookForJointVals[0] = jointVals[0];
//                    addMessageCommand(out,
//                            "Goto Tool Changer Approach By Name " + toolChangerPosName + " addJointMove(lookForJointVals)");
//
//                }
//            }
//            addJointMove(out, lookForJointVals, 1.0, 0, 1);
            if (null != lookForJointsString && lookForJointsString.length() > 0) {
                double lookForJointVals[] = jointValStringToArray(lookForJointsString);
                addJointMove(out, lookForJointVals, 1.0, 1, lookForJointVals.length);
            }
            addJointMove(out, jointVals, 1.0, 0, 1);
            addJointMove(out, jointVals, 1.0, 1, jointVals.length);
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

    public PoseType approachPoseFromToolChangerPose(PoseType pose) {
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
        PoseType newPose = toolOffsetMap.get(expectedToolName);
        if (null != newPose) {
            setToolOffsetPose(newPose);
        }
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

    private void dropToolByHolderName(String toolHolderName, List<MiddleCommandType> out) {

        try {
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
                    (CRCLCommandWrapper cmd) -> {
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "toolHolderName=" + toolHolderName, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
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
                println("toolInHolder = " + toolInHolder + " is not empty");
                continue;
            }
            String toolHolderNameToCheck = contentsEntry.getKey();
            Set<String> possibleTools = getPossibleToolHolderContents(toolHolderNameToCheck);
            if (!possibleTools.contains(toolInRobot)) {
                println("toolInRobot = " + toolInRobot + " can not be added to holder " + toolHolderNameToCheck + ", possibleTools=" + possibleTools);
                continue;
            }
            toolHolderName = toolHolderNameToCheck;
            break;
        }
        if (null == toolHolderName) {
            throw new IllegalStateException("No tool holder is empty. null == toolHolderName,toolInRobot=" + toolInRobot + ",expectedContents=" + getExpectedToolHolderContentsMap());
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
        final boolean emptyToolInRobot = isEmptyTool(toolInRobot);
        addMessageCommand(out, "switchTool: toolInRobot=getExpectedToolName()=" + toolInRobot + ", emptyTool(toolInRobot)=" + emptyToolInRobot);
        if (!emptyToolInRobot) {
            dropToolAny(action, out);
        }
        String desiredToolName = action.getArgs()[toolHolderNameArgIndex];
        final boolean emptyToolDesired = isEmptyTool(desiredToolName);
        addMessageCommand(out, "switchTool: desiredToolName=" + desiredToolName + ", emptyToolDesired=" + emptyToolDesired);
        if (!emptyToolDesired) {
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
                println("toolInHolder = " + toolInHolder + "for " + toolHolderNameToCheck + " is empty");
                continue;
            }

            if (!Objects.equals(toolInHolder, desiredToolName)) {
                println("toolInHolder = " + toolInHolder + " for " + toolHolderNameToCheck + " does not equal desiredToolName=" + desiredToolName);
                continue;
            }
            toolHolderName = toolHolderNameToCheck;
            break;
        }
        if (null == toolHolderName) {
            throw new IllegalStateException("null == toolHolderName, desiredToolName=" + desiredToolName + ",expectedContents=" + getExpectedToolHolderContentsMap());
        }
        pickupToolByHolderName(toolHolderName, out);
    }

    private void pickupToolByHolderName(String toolHolderName, List<MiddleCommandType> out) {
        try {
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
                    (CRCLCommandWrapper cmd) -> {
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
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "toolHolderName=" + toolHolderName, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
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

    private final boolean enableDatabaseUpdates = false;
    private volatile @Nullable
    List<PhysicalItem> physicalItems
            = null;

    private class WaitForCompleteVisionUpdatesStartInfo implements AutoCloseable {

        final String runName; //  = getRunName();
        final XFuture<List<PhysicalItem>> xfl; //  = aprsSystem.getNewSingleVisionToDbUpdate();
        final int simViewRefreshCount; //  = aprsSystem.getSimViewRefreshCount();
        final int simViewPublishCount; //  = aprsSystem.getSimViewPublishCount();
        final int visLineCount; //  = aprsSystem.getVisionLineCount();

        final long t0; //  = System.currentTimeMillis();

        final int visClientUpdateCount; //  = aprsSystem.getVisionClientUpdateCount();
        final int visClientUpdateAquireOffCount; //  = aprsSystem.getVisionClientUpdateAquireOffCount();
        final int visClientUpdateNoCheckRequiredPartsCount; //  = aprsSystem.getVisionClientUpdateNoCheckRequiredPartsCount();
        final int visClientUpdateSingleUpdateListenersEmptyCount; //  = aprsSystem.getVisionClientUpdateSingleUpdateListenersEmptyCount();
        final int visClientIgnoreCount; //  = aprsSystem.getVisionClientIgnoreCount();
        final int visClientSkippedCount; //  = aprsSystem.getVisionClientSkippedCount();
        final AprsSystem aprsSystem;

        WaitForCompleteVisionUpdatesStartInfo(AprsSystem aprsSystemParam) {
            this.runName = getRunName();
            this.xfl = aprsSystemParam.getNewSingleVisionToDbUpdate();
            this.aprsSystem = aprsSystemParam;
            this.simViewRefreshCount = aprsSystemParam.getSimViewRefreshCount();
            this.simViewPublishCount = aprsSystemParam.getSimViewPublishCount();
            this.visLineCount = aprsSystemParam.getVisionLineCount();
            this.t0 = System.currentTimeMillis();

            this.visClientUpdateCount = aprsSystemParam.getVisionClientUpdateCount();
            this.visClientUpdateAquireOffCount = aprsSystemParam.getVisionClientUpdateAquireOffCount();
            this.visClientUpdateNoCheckRequiredPartsCount = aprsSystemParam.getVisionClientUpdateNoCheckRequiredPartsCount();
            this.visClientUpdateSingleUpdateListenersEmptyCount = aprsSystemParam.getVisionClientUpdateSingleUpdateListenersEmptyCount();
            this.visClientIgnoreCount = aprsSystemParam.getVisionClientIgnoreCount();
            this.visClientSkippedCount = aprsSystemParam.getVisionClientSkippedCount();
        }

        @Override
        public void close() {
            if (!xfl.isDone() && null != aprsSystem) {
                if (!aprsSystem.removeSingleVisionToDbUpdate(xfl)) {
                    throw new RuntimeException("failed to remove update future");
                }
            }
        }

    }

    private List<PhysicalItem> waitForCompleteVisionUpdates(String prefix, Map<String, Integer> requiredPartsMap, long timeoutMillis, int startAbortCount) {

        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";

        final Thread curThread = Thread.currentThread();
        if (null != genThread
                && genThread != curThread) {
            logError("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
            throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
        }

        try (WaitForCompleteVisionUpdatesStartInfo startInfo = new WaitForCompleteVisionUpdatesStartInfo(aprsSystem);) {
            if (startAbortCount != aprsSystem.getSafeAbortRequestCount()) {
                takeSimViewSnapshot("waitForCompleteVisionUpdates.aborting_" + startAbortCount + "_" + aprsSystem.getSafeAbortRequestCount(), this.physicalItems);
                aprsSystem.logEvent("waitForCompleteVisionUpdates:aborting" + prefix, startAbortCount, aprsSystem.getSafeAbortRequestCount(), requiredPartsMap);
                setLastProgramAborted(true);
                return Collections.emptyList();
            }
            visionUpdateCount.incrementAndGet();
            aprsSystem.refreshSimView();
            int waitCycle = 0;
            long last_t1 = startInfo.t0;

            while (!startInfo.xfl.isDone()) {
                waitCycle++;
                long t1 = System.currentTimeMillis();
                if (startAbortCount != aprsSystem.getSafeAbortRequestCount()) {
                    takeSimViewSnapshot("waitForCompleteVisionUpdates.aborting_" + startAbortCount + "_" + aprsSystem.getSafeAbortRequestCount(), this.physicalItems);
                    aprsSystem.logEvent("waitForCompleteVisionUpdates:aborting" + prefix, startAbortCount, aprsSystem.getSafeAbortRequestCount(), requiredPartsMap);
                    setLastProgramAborted(true);
                    return Collections.emptyList();
                }
                if (timeoutMillis > 0 && t1 - startInfo.t0 > timeoutMillis) {
                    handleWaitForVisionUpdatesTimeout(prefix, t1, startInfo, waitCycle, last_t1, timeoutMillis);
                }
                last_t1 = t1;
                if (startInfo.xfl.isDone()) {
                    break;
                }
                if (enableDatabaseUpdates && !aprsSystem.isEnableVisionToDatabaseUpdates()) {
                    if (startInfo.xfl.isDone()) {
                        break;
                    }
                    System.err.println("VisionToDatabaseUpdates not enabled as expected.");
                    setEnableVisionToDatabaseUpdates(
                            enableDatabaseUpdates,
                            requiredPartsMap);
                }
                if (startInfo.xfl.isCompletedExceptionally()) {
                    Throwable[] ta = new Throwable[1];
                    startInfo.xfl.exceptionally((Throwable t) -> {
                        ta[0] = t;
                        throw (RuntimeException) t;
                    });
                    if (null != ta[0]) {
                        String errMsg = startInfo.runName + " : waitForCompleteVisionUpdates(" + prefix + ",..." + timeoutMillis + ") timedout. xfl=" + startInfo.xfl;
                        throw new RuntimeException(ta[0].getMessage() + " causing " + errMsg, ta[0]);
                    }
                }
                String titleErrorString = aprsSystem.getTitleErrorString();
                if (null != titleErrorString && titleErrorString.length() > 1) {
                    throw new RuntimeException("waitForCompleteVisionUpdates: titleErrorString=" + titleErrorString);
                }
                if (startInfo.xfl.isDone()) {
                    break;
                }
                //noinspection BusyWait
                Thread.sleep(50);
                if (startInfo.xfl.isCompletedExceptionally()) {
                    Throwable[] ta = new Throwable[1];
                    startInfo.xfl.exceptionally((Throwable t) -> {
                        ta[0] = t;
                        throw (RuntimeException) t;
                    });
                    if (null != ta[0]) {
                        String errMsg = startInfo.runName + " : waitForCompleteVisionUpdates(" + prefix + ",..." + timeoutMillis + ") timedout. xfl=" + startInfo.xfl;
                        throw new RuntimeException(ta[0].getMessage() + " causing " + errMsg, ta[0]);
                    }
                }
                titleErrorString = aprsSystem.getTitleErrorString();
                if (null != titleErrorString && titleErrorString.length() > 1) {
                    throw new RuntimeException("waitForCompleteVisionUpdates: titleErrorString=" + titleErrorString);
                }
                if (startInfo.xfl.isDone()) {
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
            if (startInfo.xfl.isCompletedExceptionally()) {
                Throwable[] ta = new Throwable[1];
                startInfo.xfl.exceptionally((Throwable t) -> {
                    ta[0] = t;
                    throw (RuntimeException) t;
                });
                if (null != ta[0]) {
                    String errMsg = startInfo.runName + " : waitForCompleteVisionUpdates(" + prefix + ",..." + timeoutMillis + ") timedout. xfl=" + startInfo.xfl;
                    throw new RuntimeException(ta[0].getMessage() + " causing " + errMsg, ta[0]);
                }
            }
            List<PhysicalItem> l = startInfo.xfl.get();
            if (l.isEmpty()) {
                LOGGER.log(Level.WARNING, "{0}: waitForCompleteVisionUpdates returing empty list", getRunName());
            }
            try {
                if (snapshotsEnabled()) {
                    aprsSystem.takeSimViewSnapshot(createImageTempFile(prefix + "_waitForCompleteVisionUpdates"), l);
                    takeDatabaseViewSnapshot(createImageTempFile(prefix + "_waitForCompleteVisionUpdates_new_database"));
                }
            } catch (IOException ioException) {
                LOGGER.log(Level.SEVERE, "", ioException);
            }
            List<PhysicalItem> filteredList
                    = aprsSystem.filterListItemWithinLimits(l);
            synchronized (this) {
                clearPoseCache();
                takeSimViewSnapshot("unfiltered.waitForCompleteVisionUpdates" + prefix, l);
                takeSimViewSnapshot("filtered.waitForCompleteVisionUpdates" + prefix, filteredList);
                physicalItems = filteredList;
                loadNewItemsIntoPoseCache(filteredList);
                return filteredList;
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "", exception);
            throw new RuntimeException(exception);
        }
    }

    private void handleWaitForVisionUpdatesTimeout(String prefix, long t1, WaitForCompleteVisionUpdatesStartInfo startInfo, int waitCycle, long last_t1, long timeoutMillis) throws RuntimeException {
        assert (aprsSystem != null) : "aprsSystemInterface == null : @AssumeAssertion(nullness)";
        System.err.println("waitForCompleteVisionUpdates " + prefix + " timed out");
        System.err.println("runName=" + getRunName());
        long updateTime = aprsSystem.getLastSingleVisionToDbUpdateTime();
        long timeSinceUpdate = t1 - updateTime;
        System.err.println("timeSinceUpdate = " + timeSinceUpdate);
        long notifyTime = aprsSystem.getSingleVisionToDbNotifySingleUpdateListenersTime();
        long timeSinceNotify = t1 - notifyTime;
        System.err.println("timeSinceNotify = " + timeSinceNotify);
        System.err.println("xfl = " + startInfo.xfl);
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
        int simViewRefreshCountDiff = simViewRefreshCount - startInfo.simViewRefreshCount;
        System.err.println("simViewRefreshCountDiff = " + simViewRefreshCountDiff);
        int simViewPublishCountDiff = simViewPublishCount - startInfo.simViewPublishCount;
        System.err.println("simViewPublishCountDiff = " + simViewPublishCountDiff);
        int visLineCount = aprsSystem.getVisionLineCount();
        int visLineCountDiff = visLineCount - startInfo.visLineCount;
        System.err.println("visLineCountDiff = " + visLineCountDiff);
        boolean completedExceptionally = startInfo.xfl.isCompletedExceptionally();

        System.err.println("startInfo.visClientUpdateCount = " + startInfo.visClientUpdateCount);
        System.err.println("startInfo.visClientUpdateAquireOffCount = " + startInfo.visClientUpdateAquireOffCount);
        System.err.println("startInfo.visClientUpdateNoCheckRequiredPartsCount = " + startInfo.visClientUpdateNoCheckRequiredPartsCount);
        System.err.println("startInfo.visClientUpdateSingleUpdateListenersEmptyCount = " + startInfo.visClientUpdateSingleUpdateListenersEmptyCount);
        System.err.println("startInfo.visClientIgnoreCount = " + startInfo.visClientIgnoreCount);
        System.err.println("startInfo.visClientSkippedCount = " + startInfo.visClientSkippedCount);

        int visClientUpdateCount = aprsSystem.getVisionClientUpdateCount();
        System.err.println("visClientUpdateCount = " + visClientUpdateCount);
        int visClientUpdateAquireOffCount = aprsSystem.getVisionClientUpdateAquireOffCount();
        System.err.println("visClientUpdateAquireOffCount = " + visClientUpdateAquireOffCount);
        int visClientUpdateNoCheckRequiredPartsCount = aprsSystem.getVisionClientUpdateNoCheckRequiredPartsCount();
        System.err.println("visClientUpdateNoCheckRequiredPartsCount = " + visClientUpdateNoCheckRequiredPartsCount);
        int visClientUpdateSingleUpdateListenersEmptyCount = aprsSystem.getVisionClientUpdateSingleUpdateListenersEmptyCount();
        System.err.println("visClientUpdateSingleUpdateListenersEmptyCount = " + visClientUpdateSingleUpdateListenersEmptyCount);
        int visClientIgnoreCount = aprsSystem.getVisionClientIgnoreCount();
        println("visClientIgnoreCount = " + visClientIgnoreCount);
        int visClientSkippedCount = aprsSystem.getVisionClientSkippedCount();
        println("visClientSkippedCount = " + visClientSkippedCount);

        System.err.println("startInfo.xfl.isCompletedExceptionally() = " + completedExceptionally);
        String errMsg = startInfo.runName + " : waitForCompleteVisionUpdates(" + prefix + ",..." + timeoutMillis + ") timedout. xfl=" + startInfo.xfl;
        System.err.println(errMsg);
        String visionToDbPerformanceLine = aprsSystem.getVisionToDbPerformanceLine();
        if (null != visionToDbPerformanceLine) {
            System.err.println(visionToDbPerformanceLine);
        }
        if (completedExceptionally) {
            Throwable[] ta = new Throwable[1];
            startInfo.xfl.exceptionally((Throwable t) -> {
                ta[0] = t;
                throw (RuntimeException) t;
            });
            if (null != ta[0]) {
                throw new RuntimeException(ta[0].getMessage() + " causing " + errMsg, ta[0]);
            }
        }
        String titleErrorString = aprsSystem.getTitleErrorString();
        if (null != titleErrorString && titleErrorString.length() > 1) {
            throw new RuntimeException("waitForCompleteVisionUpdates: titleErrorString=" + titleErrorString);
        }
        throw new RuntimeException(errMsg);
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

    private final ConcurrentLinkedQueue<Consumer<WrappedActionInfo>> wrappedActionConsumers = new ConcurrentLinkedQueue<>();

    /**
     * Register a consumer to be notified when parts are placed.
     *
     * @param consumer consumer to be notified
     */
    public void addWrappedActionConsumer(Consumer<WrappedActionInfo> consumer) {
        wrappedActionConsumers.add(consumer);
    }

    /**
     * Remove a previously registered consumer.
     *
     * @param consumer consumer to be removed
     */
    public void removeWrappedActionConsumer(Consumer<WrappedActionInfo> consumer) {
        wrappedActionConsumers.remove(consumer);
    }

//    /**
//     * Notify all consumers that a place-part action has been executed.
//     *
//     * @param wai info to be passed to consumers
//     */
//    private void notifyWrappedActionConsumers(WrappedActionInfo wai) {
//        wrappedActionConsumers.forEach(consumer -> consumer.accept(wai));
//    }

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

    private void placePartBySlotName(String slotName, List<MiddleCommandType> out, Action action, @Nullable Action parentAction, int parentActionIndex) throws IllegalStateException, SQLException {
        PoseType pose = getPose(slotName);
        if (null != pose) {
            PlacePartSlotPoseList.add(pose);
        }
        String lastTakenPartLocal = lastTakenPart;

        if (skipMissingParts && lastTakenPartLocal == null) {
            recordSkipPlacePart(slotName, pose);
            return;
        }
        if (null == lastTakenPartLocal) {
            throw new IllegalStateException("null == lastTakenPart");
        }
        final String msg = "placed part " + getLastTakenPart() + " in " + slotName;
        if (takeSnapshots) {
            takeSnapshots("plan", "place-part-" + getLastTakenPart() + "in-" + slotName + "", pose, slotName);
        }
        if (pose == null) {
            if (skipMissingParts) {
                PoseType origPose = poseCache.get(lastTakenPartLocal);
                if (null != origPose) {
                    origPose = visionToRobotPose(origPose);
                    origPose.setXAxis(xAxis);
                    origPose.setZAxis(zAxis);
                    placePartByPose(out, origPose);
                    takeSnapshots("plan", "returning-" + getLastTakenPart() + "_no_pose_for_" + slotName, origPose, lastTakenPartLocal);
                    final PlacePartInfo ppi = new PlacePartInfo(action, getLastIndex(), out.size(), startSafeAbortRequestCount, parentAction, parentActionIndex, "returned." + lastTakenPartLocal, "skipped." + slotName);
                    addMarkerCommand(out, msg,
                            ((CRCLCommandWrapper wrapper) -> {
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
        final PlacePartInfo ppi = new PlacePartInfo(
                action,
                getLastIndex(),
                out.size(),
                startSafeAbortRequestCount,
                parentAction,
                parentActionIndex,
                lastTakenPartLocal,
                slotName);
        placePartByPose(out, pose);
        addMarkerCommand(out, msg,
                ((CRCLCommandWrapper wrapper) -> {
                    addToInspectionResultJTextPane("&nbsp;&nbsp;" + msg + " completed at " + new Date() + "<br>");
                    logDebug(msg + " completed at " + new Date());
                    ppi.setWrapper(wrapper);
                    notifyPlacePartConsumers(ppi);
                }));
    }

    private void recordSkipPlacePart(String slotName, @Nullable PoseType pose) throws IllegalStateException {
        takeSnapshots("plan", "skipping-place-part-" + getLastTakenPart() + "-in-" + slotName + "", pose, slotName);
    }

    public static @Nullable
    <T, E> T getKeyByValue(Map<T, E> map, E value) {
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

        final PoseType poseWithToolOffset = CRCLPosemath.multiply(pose, toolOffsetPose);

        logToolOffsetInfo(cmds, pose, poseWithToolOffset);
        final PoseType approachPose = requireNonNull(copy(poseWithToolOffset), "approachPose");
        lastTestApproachPose = null;

        //logDebug("Z= " + pose.getPoint().getZ());
        final PointType approachPosePoint = requireNonNull(approachPose.getPoint(), "approachPose.getPoint()");
        final PointType poseWithToolOffsetPoint = requireNonNull(poseWithToolOffset.getPoint(), "poseWithToolOffset.getPoint()");
        approachPosePoint.setZ(poseWithToolOffsetPoint.getZ() + approachZOffset);

        final PoseType placePose = requireNonNull(copy(poseWithToolOffset), "placePose");
        final PointType placePosePoint = requireNonNull(placePose.getPoint(), "placePose.getPoint()");
        placePosePoint.setZ(poseWithToolOffsetPoint.getZ() + placeZOffset);

        addSetFastSpeed(cmds);

        addMoveTo(cmds, approachPose, false, "placePartByPose.approachPose");

        addSetSlowSpeed(cmds);

        addMoveTo(cmds, placePose, true, "placePartByPose.placePose");

        addSettleDwell(cmds);

        addCheckedOpenGripper(cmds);

        addSettleDwell(cmds);
        addSetFastSpeed(cmds);
        addMoveTo(cmds, approachPose, true, "placePartByPose.approachPose.return");

        setLastTakenPart(null);
    }

    private void logToolOffsetInfo(List<MiddleCommandType> cmds, PoseType pose, PoseType poseWithToolOffset) {
        if (debug && toolOffsetPose != DEFAULT_TOOL_OFFSET_POSE) {
            addMessageCommand(cmds,
                    "pose=" + CRCLPosemath.poseToXyzRpyString(pose) + "\n"
                    + "toolOffsetPose=" + CRCLPosemath.poseToXyzRpyString(toolOffsetPose) + "\n"
                    + "poseWithToolOffset=" + CRCLPosemath.poseToXyzRpyString(poseWithToolOffset) + "\n"
            );
            println("pose=" + CRCLPosemath.poseToXyzRpyString(pose) + "\n"
                    + "toolOffsetPose=" + CRCLPosemath.poseToXyzRpyString(toolOffsetPose) + "\n"
                    + "poseWithToolOffset=" + CRCLPosemath.poseToXyzRpyString(poseWithToolOffset) + "\n");
            println("pose=" + CRCLPosemath.poseToString(pose) + "\n"
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

    private class CRCLCommandWrapperConsumerChecker implements CRCLCommandWrapperConsumer {

        private final CRCLCommandWrapperConsumer consumer;

        public CRCLCommandWrapperConsumerChecker(CRCLCommandWrapperConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(CRCLCommandWrapper wrapper) {

            final Thread curThread = Thread.currentThread();
            if (null != genThread
                    && genThread != curThread) {
                logError("genThreadSetTrace = " + Arrays.toString(genThreadSetTrace));
                throw new IllegalStateException("genThread != curThread : genThread=" + genThread + ",curThread=" + curThread);
            }
            try {
                consumer.accept(wrapper);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "wrapper=" + wrapper, e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void addMarkerCommand(List<MiddleCommandType> cmds, String message, CRCLCommandWrapperConsumer cb) {
        MessageType messageCmd = new MessageType();
        messageCmd.setMessage(message + " action=" + lastIndex + " crclNumber=" + crclNumber.get());
        setCommandId(messageCmd);
        CRCLCommandWrapper wrapper = CRCLCommandWrapper.wrapWithOnDone(messageCmd, new CRCLCommandWrapperConsumerChecker(cb));
        cmds.add(wrapper);
    }

    private boolean useMessageCommands = false;

    public boolean isUseMessageCommands() {
        return useMessageCommands;
    }

    public void setUseMessageCommands(boolean useMessageCommands) {
        this.useMessageCommands = useMessageCommands;
    }

    private void addMessageCommand(List<MiddleCommandType> cmds, String message) {
        String useMessageCommandsString = options.get("useMessageCommands");
        useMessageCommands = (null != useMessageCommandsString
                && useMessageCommandsString.length() > 0
                && Boolean.parseBoolean(useMessageCommandsString));

        if (useMessageCommands) {
            MessageType messageCmd = new MessageType();
            messageCmd.setMessage(message);
            setCommandId(messageCmd);
            cmds.add(messageCmd);
        }
    }

    private void addOptionalCommand(MiddleCommandType optCmd, List<MiddleCommandType> cmds, CRCLCommandWrapperConsumer cb) {
        setCommandId(optCmd);
        CRCLCommandWrapper wrapper = CRCLCommandWrapper.wrapWithOnStart(optCmd, cb);
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

    private void notifyActionCompletedListeners(int actionIndex, String comment, Action action, CRCLCommandWrapper wrapper, List<Action> actions, @Nullable List<Action> origActions) {
        ActionCallbackInfo acbi = new ActionCallbackInfo(actionIndex, comment, action, wrapper, actions, origActions);
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
            if (null != dbConnection) {
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
