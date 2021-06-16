/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
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
package aprs.system;

import aprs.actions.executor.Action;
import aprs.actions.executor.CrclGenerator;
import aprs.actions.executor.CrclGenerator.PoseProvider;
import aprs.actions.executor.ExecutorJInternalFrame;
import aprs.actions.executor.PositionMap;
import aprs.cachedcomponents.CachedCheckBox;
import aprs.database.*;
import aprs.database.explore.ExploreGraphDbJInternalFrame;
import aprs.database.vision.UpdateResults;
import aprs.database.vision.VisionToDbJInternalFrame;
import aprs.kitinspection.KitInspectionJInternalFrame;
import aprs.learninggoals.GoalLearner;
import aprs.logdisplay.LogDisplayJInternalFrame;
import aprs.misc.*;
import aprs.simview.*;
import aprs.supervisor.main.Supervisor;
import crcl.base.*;
import crcl.copier.CRCLCopier;
import crcl.ui.AutomaticPropertyFileUtils;
import crcl.ui.ConcurrentBlockProgramsException;
import crcl.ui.client.CrclSwingClientJInternalFrame;
import crcl.ui.client.CurrentPoseListener;
import crcl.ui.client.ProgramLineListener;
import crcl.ui.forcetorquesensorsimulator.ForceTorqueSimJInternalFrame;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.ui.server.SimServerJInternalFrame;
import crcl.utils.*;
import crcl.utils.outer.interfaces.ProgramRunData;
import crcl.utils.server.CRCLServerSocket;
import crcl.utils.server.ServerJInternalFrameProviderFinderInterface;
import crcl.utils.server.ServerJInternalFrameProviderInterface;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static aprs.misc.AprsCommonLogger.println;
import static aprs.misc.Utils.shortenItemPartName;

/**
 * AprsSystem is the container for one robotic system in the APRS (Agility
 * Performance of Robotic Systems) framework.
 * <p>
 * Internal windows are used to represent each of the modules within the system.
 * Vision, Sensor Processing, Planning, Execution, and the CRCL (Canonical Robot
 * Command Language) client and server.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsSystem implements SlotOffsetProvider {

    /**
     * Creates new AprsSystem using a default properties file.
     */
    @SuppressWarnings({ "initialization", "guieffect" })
    private AprsSystem(boolean immediate) {
	this(null, AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS());
	if (immediate) {
	    headlessEmptyInit();
	}
    }

    public XFuture<Boolean> gotoPose(PoseType pose) {
	if (null == executorJInternalFrame1) {
	    throw new NullPointerException("pddlExecutorJInternalFrame1");
	}
	final boolean connected = isConnected();
	if (!connected) {
	    throw new IllegalStateException("!isConnected()");
	}
	final boolean doingActions = isDoingActions();
	final String doingActionsInfo = getIsDoingActionsInfo();
	if (doingActions) {
	    throw new IllegalStateException("doingActionsInfo=" + doingActionsInfo);
	}
	if (isPaused()) {
	    throw new IllegalStateException("isPaused()");
	}
	checkFutures();
	String executorReadyString = executorJInternalFrame1.readyForNewActionsListInfoString();
	if (!executorJInternalFrame1.readyForNewActionsList()) {
	    logEvent("executorReadyString", executorReadyString);
	    final String errmsg = "!pddlExecutorJInternalFrame1.readyForNewActionsList()";
	    logToSuper(this.toString() + " throwing " + errmsg);
	    throw new IllegalStateException(errmsg);
	}
	try {
	    CRCLProgramType program = createEmptyProgram();
	    executorJInternalFrame1.testPartPositionByPose(CRCLUtils.middleCommands(program), pose);
	    return startCRCLProgram(program);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
	    setTitleErrorString(ex.getMessage());
	    throw new RuntimeException(ex);
	}
    }

    private boolean useCsvFilesInsteadOfDatabase;

    /**
     * Get the value of useCsvFilesInsteadOfDatabase
     *
     * @return the value of useCsvFilesInsteadOfDatabase
     */
    public boolean isUseCsvFilesInsteadOfDatabase() {
	return useCsvFilesInsteadOfDatabase;
    }

    /**
     * Set the value of useCsvFilesInsteadOfDatabase
     *
     * @param useCsvFilesInsteadOfDatabase new value of useCsvFilesInsteadOfDatabase
     */
    public void setUseCsvFilesInsteadOfDatabase(boolean useCsvFilesInsteadOfDatabase) {
	this.useCsvFilesInsteadOfDatabase = useCsvFilesInsteadOfDatabase;
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setCheckBoxMenuItemUseCsvFilesInsteadOfDatabase(useCsvFilesInsteadOfDatabase);
	}
    }

    public boolean overlaps(PhysicalItem itemA, PhysicalItem itemB) {

	if (itemA == itemB) {
	    return false;
	}
	if (itemA instanceof Tray) {
	    Tray trayA = (Tray) itemA;
	    double dist = trayA.dist(itemB);
	    if (checkTrayOverlap(trayA, itemB)) {
		return true;
	    } else {
		// recheking for debug
		checkTrayOverlap(trayA, itemB);
	    }
	}

	return false;
    }

    @SuppressWarnings("UnusedAssignment")
    private boolean checkTrayOverlap(Tray trayA, PhysicalItem itemB) {
	List<Slot> slotsA = trayA.getAbsSlotList();
	if (null == slotsA || slotsA.isEmpty()) {
	    slotsA = getAbsSlots(trayA, false);
	}
	double minSlotijDist = Double.MAX_VALUE;
	double minSlotijRequiredDist = 0;
	int minSlotijI = -1;
	int minSlotijJ = -1;
	Slot minSlotAi = null;
	Slot minSlotBj = null;
	for (int i = 0; i < slotsA.size(); i++) {
	    Slot slotAi = slotsA.get(i);
	    double slotiDist = slotAi.dist(itemB);
	    if (slotiDist < slotAi.getDiameter() / 2.0) {
		return true;
	    }
	    if (itemB instanceof Tray) {
		Tray trayB = (Tray) itemB;
		List<Slot> slotsB = trayB.getAbsSlotList();
		if (null == slotsB || slotsB.isEmpty()) {
		    slotsB = getAbsSlots(trayB, false);
		}
		for (int j = 0; j < slotsB.size(); j++) {
		    Slot slotBj = slotsB.get(j);
		    double slotjDist = slotBj.dist(trayA);
		    if (slotjDist < slotBj.getDiameter() / 2.0) {
			return true;
		    }
		    double slotijDist = slotBj.dist(slotAi);
		    final double requiredDist = slotBj.getDiameter() / 2.0 + slotAi.getDiameter() / 2.0;
		    if (slotijDist < minSlotijDist) {
			minSlotijDist = slotijDist;
			minSlotijI = i;
			minSlotijJ = j;
			minSlotAi = slotAi;
			minSlotBj = slotBj;
			minSlotijRequiredDist = requiredDist;
		    }
		    if (slotijDist < requiredDist) {
			return true;
		    }
		}
	    }
	}
	return false;
    }

    public boolean overlaps(PhysicalItem item, Collection<? extends PhysicalItem> collection) {
	for (PhysicalItem itemFromCollection : collection) {
	    if (itemFromCollection == item) {
		continue;
	    }
	    if (itemFromCollection.getType().equals("P")) {
		continue;
	    }
	    if (itemFromCollection.getType().equals("S")) {
		continue;
	    }
	    if (overlaps(item, itemFromCollection)) {
		return true;
	    }
	}
	return false;
    }

    public List<PhysicalItem> filterNonOverLapping(Collection<? extends PhysicalItem> items) {
	List<PhysicalItem> ret = new ArrayList<>();
	for (PhysicalItem itemI : items) {
	    if (itemI.getType().equals("P")) {
		ret.add(itemI);
		continue;
	    }
	    if (itemI.getType().equals("S")) {
		ret.add(itemI);
		continue;
	    }
	    if (overlaps(itemI, items)) {
		ret.add(itemI);
	    }
	}
	return ret;
    }

    public List<PhysicalItem> filterOverLapping(Collection<? extends PhysicalItem> items) {
	List<PhysicalItem> ret = new ArrayList<>();
	for (PhysicalItem itemI : items) {
	    if (itemI.getType().equals("P")) {
		ret.add(itemI);
		continue;
	    }
	    if (itemI.getType().equals("S")) {
		ret.add(itemI);
		continue;
	    }
	    if (!overlaps(itemI, items)) {
		ret.add(itemI);
	    }
	}
	return ret;
    }

    public Object2DOuterJPanel getObjectViewPanel() {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	return object2DViewJInternalFrame.getObjectViewPanel();
    }

    /**
     * Creates new AprsSystem using a default properties file.
     */
    @SuppressWarnings({ "guieffect" })
    private AprsSystem() {
	this(null, AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS());
    }

    /**
     * Creates new AprsSystem using a default properties file.
     *
     * @param aprsSystemDisplayJFrame1 swing gui to show edit properties
     */
    @UIEffect
    @SuppressWarnings({ "initialization" })
    private AprsSystem(@Nullable AprsSystemDisplayJFrame aprsSystemDisplayJFrame1,
	    AprsSystemPropDefaults propDefaults) {
	this.aprsSystemDisplayJFrame = aprsSystemDisplayJFrame1;
	visionLineListener = this::visionLineCounter;
	simPublishCountListener = this::setSimPublishCount;
	try {
	    initPropertiesFileInfo(propDefaults);
	    this.asString = getTitle();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
	}
	if (null != propertiesFile) {
	    println("Constructor for AprsSystem with taskName=" + taskName + ", robotName=" + robotName
		    + ", propertiesFile=" + propertiesFile.getName() + " complete.");
	} else {
	    println("Constructor for AprsSystem with taskName=" + taskName + ", robotName=" + robotName + " complete.");
	}
	if (null != aprsSystemDisplayJFrame1) {
	    connectDatabaseCheckBox = aprsSystemDisplayJFrame1.connectDatabaseCheckBox();
	    connectedRobotCheckBox = aprsSystemDisplayJFrame1.connectedRobotCheckBox();
	    connectVisionOnStartupCheckBox = aprsSystemDisplayJFrame1.connectVisionOnStartupCheckBox();
	    continousDemoCheckBox = aprsSystemDisplayJFrame1.continousDemoCheckBox();
	    executorStartupCheckBox = aprsSystemDisplayJFrame1.executorStartupCheckBox();
	    exploreGraphDBStartupCheckBox = aprsSystemDisplayJFrame1.exploreGraphDBStartupCheckBox();
	    kitInspectionStartupCheckBox = aprsSystemDisplayJFrame1.kitInspectionStartupCheckBox();
	    logCrclProgramsCheckBox = aprsSystemDisplayJFrame1.logCrclProgramsCheckBox();
	    object2DViewStartupCheckBox = aprsSystemDisplayJFrame1.object2DViewStartupCheckBox();
	    objectSpStartupCheckBox = aprsSystemDisplayJFrame1.objectSpStartupCheckBox();
	    onStartupConnectDatabaseCheckBox = aprsSystemDisplayJFrame1.onStartupConnectDatabaseCheckBox();
	    pauseCheckBox = aprsSystemDisplayJFrame1.pauseCheckBox();
//            pddlPlannerStartupCheckBox = aprsSystemDisplayJFrame1.pddlPlannerStartupCheckBox();
	    reloadSimFilesOnReverseCheckBox = aprsSystemDisplayJFrame1.reloadSimFilesOnReverseCheckBox();
	    reverseCheckBox = aprsSystemDisplayJFrame1.reverseCheckBox();
	    alertLimitsCheckBox = aprsSystemDisplayJFrame1.alertLimitsCheckBox();
	    robotCrclFanucServerStartupCheckBox = aprsSystemDisplayJFrame1.robotCrclFanucServerStartupCheckBox();
	    robotCrclGUIStartupCheckBox = aprsSystemDisplayJFrame1.robotCrclGUIStartupCheckBox();
	    forceTorqueSimStartupCheckBox = aprsSystemDisplayJFrame1.forceTorqueSimStartupCheckBox();
	    robotCrclMotomanServerStartupCheckBox = aprsSystemDisplayJFrame1.robotCrclMotomanServerStartupCheckBox();
	    robotCrclSimServerStartupCheckBox = aprsSystemDisplayJFrame1.robotCrclSimServerStartupCheckBox();
	    showDatabaseSetupOnStartupCheckBox = aprsSystemDisplayJFrame1.showDatabaseSetupOnStartupCheckBox();
	    snapshotsCheckBox = aprsSystemDisplayJFrame1.snapshotsCheckBox();
	    steppingCheckBox = aprsSystemDisplayJFrame1.steppingCheckBox();
	    useTeachTableCheckBox = aprsSystemDisplayJFrame1.useTeachTableCheckBox();
	    connectVisionCheckBox = aprsSystemDisplayJFrame1.connectVisionCheckBox();

	} else {
	    connectDatabaseCheckBox = new CachedCheckBox();
	    connectedRobotCheckBox = new CachedCheckBox();
	    connectVisionOnStartupCheckBox = new CachedCheckBox();
	    continousDemoCheckBox = new CachedCheckBox();
	    executorStartupCheckBox = new CachedCheckBox();
	    exploreGraphDBStartupCheckBox = new CachedCheckBox();
	    kitInspectionStartupCheckBox = new CachedCheckBox();
	    logCrclProgramsCheckBox = new CachedCheckBox();
	    object2DViewStartupCheckBox = new CachedCheckBox();
	    objectSpStartupCheckBox = new CachedCheckBox();
	    onStartupConnectDatabaseCheckBox = new CachedCheckBox();
	    pauseCheckBox = new CachedCheckBox();
//            pddlPlannerStartupCheckBox = new CachedCheckBox();
	    reloadSimFilesOnReverseCheckBox = new CachedCheckBox();
	    reverseCheckBox = new CachedCheckBox();
	    alertLimitsCheckBox = new CachedCheckBox();
	    robotCrclFanucServerStartupCheckBox = new CachedCheckBox();
	    robotCrclGUIStartupCheckBox = new CachedCheckBox();
	    forceTorqueSimStartupCheckBox = new CachedCheckBox();
	    robotCrclMotomanServerStartupCheckBox = new CachedCheckBox();
	    robotCrclSimServerStartupCheckBox = new CachedCheckBox();
	    showDatabaseSetupOnStartupCheckBox = new CachedCheckBox();
	    snapshotsCheckBox = new CachedCheckBox();
	    steppingCheckBox = new CachedCheckBox();
	    useTeachTableCheckBox = new CachedCheckBox();
	    connectVisionCheckBox = new CachedCheckBox();
	}
	refreshJInternalFrameFinders();
    }

    private final CachedCheckBox connectDatabaseCheckBox;
    private final CachedCheckBox connectedRobotCheckBox;
    private final CachedCheckBox connectVisionOnStartupCheckBox;
    private final CachedCheckBox continousDemoCheckBox;
    private final CachedCheckBox executorStartupCheckBox;
    private final CachedCheckBox exploreGraphDBStartupCheckBox;
    private final CachedCheckBox kitInspectionStartupCheckBox;
    private final CachedCheckBox logCrclProgramsCheckBox;
    private final CachedCheckBox object2DViewStartupCheckBox;
    private final CachedCheckBox objectSpStartupCheckBox;
    private final CachedCheckBox onStartupConnectDatabaseCheckBox;
    private final CachedCheckBox pauseCheckBox;
//    private final CachedCheckBox pddlPlannerStartupCheckBox;
    private final CachedCheckBox reloadSimFilesOnReverseCheckBox;
    private final CachedCheckBox reverseCheckBox;
    private final CachedCheckBox alertLimitsCheckBox;
    private final CachedCheckBox robotCrclFanucServerStartupCheckBox;
    private final CachedCheckBox robotCrclGUIStartupCheckBox;
    private final CachedCheckBox forceTorqueSimStartupCheckBox;
    private final CachedCheckBox robotCrclMotomanServerStartupCheckBox;
    private final CachedCheckBox robotCrclSimServerStartupCheckBox;
    private final CachedCheckBox showDatabaseSetupOnStartupCheckBox;
    private final CachedCheckBox snapshotsCheckBox;
    private final CachedCheckBox steppingCheckBox;
    private final CachedCheckBox useTeachTableCheckBox;
    private final CachedCheckBox connectVisionCheckBox;

    private final @MonotonicNonNull AprsSystemDisplayJFrame aprsSystemDisplayJFrame;

    private @MonotonicNonNull VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    private @MonotonicNonNull ExecutorJInternalFrame executorJInternalFrame1 = null;
    private @MonotonicNonNull Object2DViewJInternalFrame object2DViewJInternalFrame = null;
//    private @MonotonicNonNull
//    PddlPlannerJInternalFrame pddlPlannerJInternalFrame = null;
    private @MonotonicNonNull DbSetupJInternalFrame dbSetupJInternalFrame = null;
    private @MonotonicNonNull ForceTorqueSimJInternalFrame forceTorqueSimJInternalFrame = null;
    private volatile @MonotonicNonNull CrclSwingClientJInternalFrame crclClientJInternalFrame = null;
    private @MonotonicNonNull SimServerJInternalFrame simServerJInternalFrame = null;
    private volatile @MonotonicNonNull LogDisplayJInternalFrame logDisplayJInternalFrame = null;
    private @Nullable ServerJInternalFrameProviderInterface fanucServerProvider = null;
    private @MonotonicNonNull JInternalFrame fanucCRCLServerJInternalFrame = null;
    private @MonotonicNonNull ExploreGraphDbJInternalFrame exploreGraphDbJInternalFrame = null;

    private @Nullable ServerJInternalFrameProviderInterface motomanServerProvider = null;
    private @MonotonicNonNull JInternalFrame motomanCrclServerJInternalFrame = null;
    private @MonotonicNonNull KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

    private String taskName = NO_TASK_NAME;
    private static final String NO_TASK_NAME = "NoTaskName";

    private volatile long lastStartRunTime = -1;
    private volatile long lastStopRunTime = -1;
    private volatile long lastRunDuration = -1;
    private volatile long lastStopDuration = -1;
    private final AtomicLong effectiveStartRunTime = new AtomicLong(-1);
    private final AtomicLong effectiveStopRunTime = new AtomicLong(-1);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicReference<@Nullable Runnable> onCloseRunnable = new AtomicReference<>();

    public void setOnCloseRunnable(@Nullable Runnable r) {
	onCloseRunnable.set(r);
    }

    /**
     * Check if the user has selected a check box asking for snapshot files to be
     * created for logging/debugging.
     *
     * @return Has user enabled snapshots?
     */
    public boolean isSnapshotsSelected() {
	return snapshotsCheckBox.isSelected();
    }

    public void setSnapshotsSelected(boolean selected) {
	snapshotsCheckBox.setSelected(selected);
    }

    private void setStartRunTime() {
	setExecutorForceFakeTakeFlag(false);
	checkReadyToRun();
	long t = System.currentTimeMillis();
	if (running.compareAndSet(false, true)) {

	    lastStartRunTime = t;

	    if (!effectiveStartRunTime.compareAndSet(-1, t) && lastStopRunTime > 0) {
		lastStopDuration = lastStartRunTime - lastStopRunTime;
		effectiveStartRunTime.addAndGet(lastStopDuration);
	    }
	}
    }

    private String origRobotName = "";

    /**
     * Get the value of origRobotName
     *
     * @return the value of origRobotName
     */
    public String getOrigRobotName() {
	maybeSetOrigRobotName(getRobotName());
	return origRobotName;
    }

    private volatile @Nullable Image scanImage = null;

    /**
     * Get the most recent image created when scanning for desired part locations.
     *
     * @return image of most recent scan
     */
    public @Nullable Image getScanImage() {
	return scanImage;
    }

    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    public @Nullable PoseProvider getExecExternalPoseProvider() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	return executorJInternalFrame1.getExternalPoseProvider();
    }

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalPoseProvider new value of externalGetPoseFunction
     */
    public void setExecExternalPoseProvider(PoseProvider externalPoseProvider) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	this.executorJInternalFrame1.setExternalPoseProvider(externalPoseProvider);
    }

    public void reloadErrorMaps() throws IOException {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	this.executorJInternalFrame1.reloadErrorMaps();
    }

    private volatile StackTraceElement restorOrigRobotInfoTrace @Nullable [] = null;

    public XFutureVoid restoreOrigRobotInfo() {
	String origRobotName1 = this.getOrigRobotName();
	if (null == origRobotName1 || origRobotName1.length() < 1) {
	    throw new IllegalStateException("origRobotName=" + origRobotName1);
	}
	String origCrclRobotHost1 = this.getOrigCrclRobotHost();
	if (null == origCrclRobotHost1 || origCrclRobotHost1.length() < 1) {
	    throw new IllegalStateException("origCrclRobotHost1=" + origCrclRobotHost1);
	}
	int origCrclRobotPort1 = this.getOrigCrclRobotPort();
	if (origCrclRobotPort1 < 1) {
	    throw new IllegalStateException("origCrclRobotPort1=" + origCrclRobotPort1);
	}
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	this.restorOrigRobotInfoTrace = trace;
	XFutureVoid immediateAbortFuture = this.immediateAbort();
	boolean wasConnected0 = isConnected();
	return immediateAbortFuture
		.thenRunAsync("disconnectRobot(" + getRobotName() + ")",
			() -> {
			    try {
				disconnectRobotPrivate();
			    } catch (Exception ex) {
				System.err.println("trace = " + Utils.traceToString(trace));
				Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
				if (ex instanceof RuntimeException) {
				    throw (RuntimeException) ex;
				} else {
				    throw new RuntimeException(ex);
				}
			    }
			},
			runProgramService)
		.thenRun(
			"restoreOrigRobotInfo" + getTaskName(),
			() -> {
			    try {
				this.setRobotName(origRobotName1);
				File limitsCsv = getCartLimitsCsvFile();
				if (null != limitsCsv && limitsCsv.exists()) {
				    readLimitsFromCsv(limitsCsv);
				}
				reloadErrorMaps();
				updateRobotLimits();
				resume();
				clearErrors();
			    } catch (Exception ex) {
				Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
				if (ex instanceof RuntimeException) {
				    throw (RuntimeException) ex;
				} else {
				    throw new RuntimeException(ex);
				}
			    }
			})
		.thenRunAsync(
			"restoreOrigRobotInfo.connectRobotPrivate" + origRobotName1,
			() -> connectRobotPrivate(origRobotName1, origCrclRobotHost1, origCrclRobotPort1, wasConnected0,
				trace),
			runProgramService);
    }

    private void setStopRunTime() {
	long t = System.currentTimeMillis();
	if (running.compareAndSet(true, false)) {
	    lastStopRunTime = t;
	    if (!effectiveStopRunTime.compareAndSet(-1, t) && lastStartRunTime > 0) {
		lastRunDuration = lastStopRunTime - lastStartRunTime;
		effectiveStopRunTime.addAndGet(lastRunDuration);
	    }
	}
    }

    /**
     * Get the duration in milliseconds that this system has been in the run state.
     *
     * @return duration in milliseconds or 0 if it has not been started and stopped
     *         at least once.
     */
    public long getRunDuration() {
	long t = effectiveStartRunTime.get();
	if (t < 0) {
	    return 0;
	}
	if (running.get()) {
	    return System.currentTimeMillis() - t;
	} else {
	    if (lastStopRunTime < 0) {
		return 0;
	    }
	    return lastStopRunTime - t;
	}
    }

    /**
     * Get the duration in milliseconds that this system has been in the stopped
     * state.
     *
     * @return duration in milliseconds or 0 if it has not been started and stopped
     *         at least once.
     */
    public long getStopDuration() {
	long t = effectiveStopRunTime.get();
	if (t < 0) {
	    return 0;
	}
	if (!running.get()) {
	    return System.currentTimeMillis() - t;
	} else {
	    if (lastStartRunTime < 0) {
		return 0;
	    }
	    return lastStartRunTime - t;
	}
    }

    private String printListToString(List<String> l, int itemsPerLine) {
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < l.size(); i += itemsPerLine) {
	    int end = Math.min(i + itemsPerLine, l.size());
	    sb.append(i);
	    sb.append("-");
	    sb.append((end - 1));
	    sb.append(": \t");
	    for (int j = i; j < end; j++) {
		sb.append(l.get(j));
		if (j < end - 1) {
		    sb.append(",");
		}
	    }
	    if (end < l.size()) {
		sb.append("\n");
	    }
	}
	return sb.toString();
    }

    public void clearVisionRequiredParts() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.clearPrevVisionListSize();
	}
	visionToDbJInternalFrame.clearVisionRequiredParts();

    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame is
     * received from vision.
     *
     * @return future with list of items updated in the next frame from the vision
     */
    public XFuture<List<PhysicalItem>> getNewSingleVisionToDbUpdate() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	long startTime = logEvent("start getSingleVisionToDbUpdate");
	XFuture<List<PhysicalItem>> ret = visionToDbJInternalFrame.getNewSingleUpdate();
	return ret.thenApply(x -> {
	    logEvent("end getSingleVisionToDbUpdate",
		    printListToString(x.stream().map(PhysicalItem::getFullName).collect(Collectors.toList()), 8)
			    + "\n started at" + startTime
			    + "\n timeDiff=" + (startTime - System.currentTimeMillis()));
	    return x;
	});
    }

    public boolean removeSingleVisionToDbUpdate(XFuture<List<PhysicalItem>> future) {
	if (null == visionToDbJInternalFrame) {
	    throw new NullPointerException("visionToDbJInternalFrame");
	}
	return visionToDbJInternalFrame.removeSingleUpdate(future);
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame is
     * received from vision.
     *
     * @return future with list of items updated in the next frame from the vision
     */
    public XFuture<List<PhysicalItem>> getSingleVisionToDbUpdate() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	long startTime = logEvent("start getSingleVisionToDbUpdate");
	XFuture<List<PhysicalItem>> ret = visionToDbJInternalFrame.getSingleUpdate();
	return ret.thenApply(x -> {
	    logEvent("end getSingleVisionToDbUpdate",
		    printListToString(x.stream().map(PhysicalItem::getFullName).collect(Collectors.toList()), 8)
			    + "\n started at" + startTime
			    + "\n timeDiff=" + (startTime - System.currentTimeMillis()));
	    return x;
	});
    }

    private final AtomicInteger simViewUpdateCount = new AtomicInteger();
    private final AtomicLong simViewUpdateTime = new AtomicLong();

    public XFuture<List<PhysicalItem>> getSimViewUpdate() {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	long startTime = System.currentTimeMillis();
	return object2DViewJInternalFrame.getSimViewUpdate()
		.thenApply(x -> {
		    long timeDiff = System.currentTimeMillis() - startTime;
		    long totalUpdateTime = simViewUpdateTime.addAndGet(timeDiff);
		    int count = simViewUpdateCount.incrementAndGet();
		    String info = "getSimViewUpdate: count=" + count + ",timeDiff=" + timeDiff + ",totalUpdateTime="
			    + totalUpdateTime;
		    logToSuper(info);
		    return x;
		});
    }

    public void clearPrevVisionListSize() {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	object2DViewJInternalFrame.clearPrevVisionListSize();
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame is
     * received from vision.
     *
     * @return future with list of items updated in the next frame from the vision
     */
    public XFuture<List<PhysicalItem>> getSingleRawVisionUpdate() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	long startTime = logEvent("start getSingleVisionToDbUpdate");
	XFuture<List<PhysicalItem>> ret = visionToDbJInternalFrame.getRawUpdate();
	return ret.thenApply(x -> {
	    logEvent("end getSingleVisionToDbUpdate",
		    printListToString(x.stream().map(PhysicalItem::getFullName).collect(Collectors.toList()), 8)
			    + "\n started at" + startTime
			    + "\n timeDiff=" + (startTime - System.currentTimeMillis()));
	    return x;
	});
    }

    public long getLastSingleVisionToDbUpdateTime() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getLastUpdateTime();
    }

    public long getSingleVisionToDbNotifySingleUpdateListenersTime() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getNotifySingleUpdateListenersTime();
    }

    public void setStepMode(boolean stepMode) {
	steppingCheckBox.setSelected(stepMode);
    }

    public boolean isStepMode() {
	return steppingCheckBox.isSelected();
//        if(null == crclClientJInternalFrame) {
//            throw new IllegalStateException("null == crclClientJInternalFrame");
//        }
//        return crclClientJInternalFrame.isStepMode();
    }

    /**
     * Get the most recent list of parts and kit trays from the vision system. This
     * will not block waiting for the vision system or database but could return
     * null or an empty list if the vision system has not been connected or no frame
     * has been received.
     *
     * @return list of trays
     */
    public List<PartsTray> getPartsTrayList() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getPartsTrayList();
    }

    /**
     * Force the Object 2D Simulation View to refresh, this method has no effect if
     * the view is not visible or is not in simulation mode.
     */
    public void refreshSimView() {
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.refresh(false);
	}
    }

    public long getLastSimViewRefreshTime() {
	if (null == object2DViewJInternalFrame) {
	    return -1;
	}
	return object2DViewJInternalFrame.getLastRefreshTime();
    }

    public long getLastSimViewPublishTime() {
	if (null == object2DViewJInternalFrame) {
	    return -1;
	}
	return object2DViewJInternalFrame.getLastPublishTime();
    }

    public int getSimViewRefreshCount() {
	if (null == object2DViewJInternalFrame) {
	    return -1;
	}
	return object2DViewJInternalFrame.getRefreshCount();
    }

    public long getSimViewLastPublishTime() {
	if (null == object2DViewJInternalFrame) {
	    return -1;
	}
	return object2DViewJInternalFrame.getLastPublishTime();
    }

    public int getSimViewPublishCount() {
	if (null == object2DViewJInternalFrame) {
	    return -1;
	}
	return object2DViewJInternalFrame.getPublishCount();
    }

    private @Nullable SlotOffsetProvider externalSlotOffsetProvider = null;

    /**
     * Get the value of externalSlotOffsetProvider
     *
     * @return the value of externalSlotOffsetProvider
     */
    public @Nullable SlotOffsetProvider getExternalSlotOffsetProvider() {
	return externalSlotOffsetProvider;
    }

    /**
     * Set the value of externalSlotOffsetProvider
     *
     * @param externalSlotOffsetProvider new value of externalSlotOffsetProvider
     */
    public void setExternalSlotOffsetProvider(SlotOffsetProvider externalSlotOffsetProvider) {
	this.externalSlotOffsetProvider = externalSlotOffsetProvider;
    }

    /**
     * Get a list of slots with names and relative position offsets for a given kit
     * or parts tray name.
     *
     * @param name        name of the type of kit or slot tray
     * @param ignoreEmpty if false no slots being found logs a verbose error message
     *                    and throws IllegalStateException (good for fail fast) or
     *                    if true simply returns an empty list (good or display or
     *                    when multiple will be checked.
     * @return list of slots with relative position offsets.
     */
    @Override
    public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
	if (null != externalSlotOffsetProvider) {
	    return externalSlotOffsetProvider.getSlotOffsets(name, ignoreEmpty);
	}
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return this.visionToDbJInternalFrame.getSlotOffsets(name, ignoreEmpty);
    }

    /**
     * Save an image showing the locations of new database items. The database will
     * be queried and the file saved asynchronously in another thread.
     *
     * @param f file to save
     * @return future with information on when the operation is complete.
     */
    public XFutureVoid startVisionToDbNewItemsImageSave(File f) {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return this.visionToDbJInternalFrame.startNewItemsImageSave(f);
    }

    /**
     * Get a list of slots associated with a particular tray.
     *
     * @param tray        tray to obtain list of slots
     * @param ignoreEmpty if false no slots being found logs a verbose error message
     *                    and throws IllegalStateException (good for fail fast) or
     *                    if true simply returns an empty list (good or display or
     *                    when multiple will be checked.
     * @return list of slots
     */
    public List<Slot> getAbsSlots(Tray tray, boolean ignoreEmpty) {
	if (null != externalSlotOffsetProvider) {
	    List<Slot> offsets = externalSlotOffsetProvider.getSlotOffsets(tray.getName(), ignoreEmpty);
	    List<Slot> absSlots = new ArrayList<>();
	    for (Slot offset : offsets) {
		Slot absSlot = absSlotFromTrayAndOffset(tray, offset);
		absSlots.add(absSlot);
	    }
	    return absSlots;
	}
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return this.visionToDbJInternalFrame.getSlots(tray);
    }

    /**
     * Set the preference for displaying a '+' at the current position of the robot
     * tool.
     *
     * @param v should the position be displayed.
     */
    public void setSimViewTrackCurrentPos(boolean v) {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function.");
	}
	this.object2DViewJInternalFrame.setTrackCurrentPos(v);
    }

    /**
     * Set the simulation view to simulation mode and disconnect from any vision
     * data socket.
     */
    public void simViewSimulateAndDisconnect() {
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.setSimulatedAndDisconnect();
	}
    }

    /**
     * Get a rotational offset in radians between the vision system and the database
     * coordinate system.
     *
     * @return rotational offset in radians
     */
    public double getVisionToDBRotationOffset() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return this.visionToDbJInternalFrame.getRotationOffset();
    }

    /**
     * Pause a currently executing CRCL program.
     * <p>
     * No action is taken if the CRCL Client has not been started and connected or
     * not program was currently running.
     */
    public void pauseCrclProgram() {
	if (null != crclClientJInternalFrame) {
	    debugDumpStack();
	    crclClientJInternalFrame.pauseCrclProgram();
	    if (Utils.arePlayAlertsEnabled()) {
		runOnDispatchThread(Utils::PlayAlert2);
	    }
	}
    }

    private void debugDumpStack() {
	if (debug) {
	    Thread.dumpStack();
	}
    }

    /**
     * Get the Kit Inspection InternalJFrame if it has been created.
     *
     * @return Kit Inspection InternalJFrame
     */
    public @Nullable KitInspectionJInternalFrame getKitInspectionJInternalFrame() {
	return kitInspectionJInternalFrame;
    }

    private final AtomicLong runNumber = new AtomicLong((System.currentTimeMillis() / 10000) % 1000);

    public long getRunNumber() {
	return runNumber.get();
    }

    /**
     * Return the user's preference on whether the stack trace be dumped for more
     * verbose logging.
     *
     * @return current setting of menu item
     */
    public boolean isEnableDebugDumpStacks() {
	return debug;
    }

    private volatile @Nullable String runName = null;

    /**
     * Creates a run name useful for identifying a run in log files or the names of
     * snapshot files. The name is composed of the current robot name, task name and
     * robot number.
     *
     * @return current run name.
     */
    public String getRunName() {
	String rn = this.runName;
	if (null != rn) {
	    return rn;
	}
	String ret = ((this.taskName != null) ? this.taskName.replace(" ", "-") : "") + "-run-"
		+ String.format("%03d", runNumber.get()) + "-"
		+ ((this.robotName != null) ? this.robotName : "") + (isReverseFlag() ? "-Reverse" : "");
	this.runName = ret;
	return ret;
    }

    /**
     * Checks whether there is currently a CRCL program that is loaded but paused.
     *
     * @return whether a program is paused
     */
    public boolean isCrclProgramPaused() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.isPaused();
	}
	return false;
    }

    /**
     * Get the thread currently running a CRCL program if it exists and is known.
     * Only useful for debugging.
     *
     * @return thread
     */
    public @Nullable Thread getCrclRunProgramThread() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getRunProgramThread();
	}
	return null;
    }

    /**
     * Get the future that can be used to determine when the current CRCL program is
     * finished. Only useful for debugging.
     *
     * @return future or null if no program is running
     */
    public @Nullable XFuture<Boolean> getCrclRunProgramFuture() {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("CRCL Client View must be open to use this function.");
	}
	return crclClientJInternalFrame.getRunProgramFuture();
    }

    /**
     * Checks whether there is currently a CRCL program running.
     *
     * @return whether a CRCL program is running.
     */
    public boolean isRunningCrclProgram() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.isRunningProgram();
	}
	return false;
    }

    public @Nullable String getLastRunningProgramTrueInfo() {
	if (null == crclClientJInternalFrame) {
	    throw new NullPointerException("crclClientJInternalFrame");
	}
	return crclClientJInternalFrame.getLastRunningProgramTrueInfo();
    }

    /**
     * Set a state where attempts to start a crcl program will be blocked.
     *
     * @return count to pass to stopBlockingCrclProgram when running crcl programs
     *         should again be allowed.
     */
    public int startBlockingCrclPrograms() {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("pendantClientJInternalFrame is null");
	}
	return crclClientJInternalFrame.startBlockingPrograms();
    }

    /**
     * Allow crcl programs to be started again after a previous call to
     * startBlockingCrclPrograms
     *
     * @param count obtained from previous call to startBlockingCrclPrograms
     * @throws crcl.ui.ConcurrentBlockProgramsException if another call to has
     *                                                  occurred
     *                                                  start/stopBlockingCrclPrograms
     *                                                  since the corresponding call
     *                                                  to startBlockingCrclProgram
     */
    public void stopBlockingCrclPrograms(int count) throws ConcurrentBlockProgramsException {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("pendantClientJInternalFrame is null");
	}
	crclClientJInternalFrame.stopBlockingPrograms(count);
    }

    /**
     * Get the currently loaded CRCL program.
     *
     * @return current CRCL program or null if no program is loaded.
     */
    public @Nullable CRCLProgramType getCrclProgram() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getProgram();
	}
	return null;
    }

    /**
     * Get the executor options as a map of names to values.
     *
     * @return the current executor options.
     */
    public Map<String, String> getExecutorOptions() {
	if (null == executorJInternalFrame1) {
	    return Collections.emptyMap();
	}
	return executorJInternalFrame1.getOptions();
    }

    /**
     * Set an executor option.
     *
     * @param key   name of option to set
     * @param value value option should be set to
     */
    public void setExecutorOption(String key, String value) {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.setOption(key, value);
	}
    }

    public void setToolHolderOperationEnabled(boolean enable) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	executorJInternalFrame1.setToolHolderOperationEnabled(enable);
    }

    public boolean isToolHolderOperationEnabled() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	return executorJInternalFrame1.isToolHolderOperationEnabled();
    }

    /**
     * Add a position map.
     * <p>
     * The position map is similar to a transform in that it may offset positions
     * output by the executor but may also be used to change scaling or correct for
     * non uniform distortions from the sensor system or imperfect kinematic
     * functions in the robot. Multiple position maps may be stacked to account for
     * different sources of error or transformation.
     *
     * @param pm position map to be added
     */
    public void addPositionMap(PositionMap pm) {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.addPositionMap(pm);
	}

    }

    /**
     * Remove a previously added position map.
     *
     * @param pm position map to be removed.
     */
    public void removePositionMap(PositionMap pm) {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.removePositionMap(pm);
	}
    }

    /**
     * Get the current pose of the robot.
     *
     * @return current pose of the robot.
     */
    public @Nullable PoseType getCurrentPose() {

	if (null != crclClientJInternalFrame) {
//            try {
//                if (!crclClientJInternalFrame.isConnected()) {
//                    crclClientJInternalFrame.connectCurrent();
//                    crclClientJInternalFrame.requestAndReadStatus();
//                }
//            } catch (Exception e) {
//                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", e);
//            }
	    if (!crclClientJInternalFrame.isConnected()) {
		return null;
	    }
	    Optional<CRCLStatusType> curStatusOptional = crclClientJInternalFrame.getCurrentStatus();
	    if (!curStatusOptional.isPresent() || curStatusOptional.get() == null) {
		crclClientJInternalFrame.requestAndReadStatus();
	    }
	    return crclClientJInternalFrame.currentStatusPose();
	}
	return null;
    }

    public XFuture<CRCLStatusType> getNewStatus() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getNewStatus();
	}
	throw new NullPointerException("crclClientJInternalFrame");
    }

    private volatile boolean enforceMinMaxLimits = true;

    /**
     * Get the value of enforceMinMaxLimits
     *
     * @return the value of enforceMinMaxLimits
     */
    public boolean isEnforceMinMaxLimits() {
	return enforceMinMaxLimits;
    }

    /**
     * Set the value of enforceMinMaxLimits
     *
     * @param enforceMinMaxLimits new value of enforceMinMaxLimits
     */
    public void setEnforceMinMaxLimits(boolean enforceMinMaxLimits) {
	this.enforceMinMaxLimits = enforceMinMaxLimits;
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setEnforceMinMaxLimitsSelected(enforceMinMaxLimits);
	}
    }

    /**
     * Get the current point(translation only) from current pose of the robot.
     *
     * @return current point(translation only) from current pose of the robot.
     */
    public @Nullable PointType getCurrentPosePoint() {
	PoseType pose = getCurrentPose();
	if (null != pose) {
	    return pose.getPoint();
	}
	return null;
    }

    /**
     * Check if the CRCL client is connected to the CRCL Server and therefore to the
     * robot.
     *
     * @return if the CRCL client is connected to the server.
     */
    public boolean isConnected() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.isConnected();
	}
	return false;
    }

    private volatile StackTraceElement setConnectedTrace @Nullable [] = null;

    /**
     * Attempt to connect or disconnect the CRCL client from the CRCL server by
     * opening or closed the appropriate socket.
     * <p>
     * NOTE: for setConnected(true) to succeed the robot port and host name must
     * have previously been set or read from the property file.
     * <p>
     * NOTE: disconnectRobot() is the same as setConnected(false) except it also
     * sets the robot name to null.
     *
     * @param connected the new desired connected state.
     */
    public void setConnected(boolean connected) {
	logEvent("setConnected", connected);
	setConnectedTrace = Thread.currentThread().getStackTrace();
	if (!connected || robotName == null || !isConnected()) {
	    enableCheckedAlready = false;
	}
	if (null == robotName && connected) {
	    printNameSetInfo();
	    setTitleErrorString("Can not connect when robotName is null");
	    throw new IllegalStateException("Can not connect when robotName is null");
	}
	if (null != crclClientJInternalFrame) {
	    if (crclClientJInternalFrame.isConnected() != connected) {
		if (connected) {
		    crclClientJInternalFrame.connectCurrent();
		    crclClientJInternalFrame.requestAndReadStatus();
		} else {
		    crclClientJInternalFrame.disconnect();
		}
	    }
	    if (crclClientJInternalFrame.isConnected() != connected) {
		setTitleErrorString("setConnected(" + connected + ") but isConnected() = "
			+ crclClientJInternalFrame.isConnected());
		throw new IllegalStateException("setConnected(" + connected + ") but isConnected() = "
			+ crclClientJInternalFrame.isConnected());
	    }
	}
	takeSnapshots("setConnected_" + connected);
	updateConnectedRobotDisplay();
    }

    private void updateConnectedRobotDisplay() {
	if (null != this.aprsSystemDisplayJFrame) {
	    updateConnectedRobotDisplay(isConnected(), getRobotName(), getRobotCrclHost(), getRobotCrclPort());
	}
    }

    private void updateConnectedRobotDisplay(boolean connected, @Nullable String robotName, @Nullable String crclHost,
	    int crclPort) {
	AprsSystemDisplayJFrame displayFrame = this.aprsSystemDisplayJFrame;
	if (null != displayFrame) {
	    final AprsSystemDisplayJFrame displayFrameFinal = displayFrame;
	    runOnDispatchThread(() -> {
		displayFrameFinal.updateConnectedRobotDisplay(connected, robotName, crclHost, crclPort);
	    });
	}
    }

    /**
     * Get the value of taskName
     *
     * @return the value of taskName
     */
    public String getTaskName() {
	return taskName;
    }

    public @Nullable String getSelectedToolName() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	return executorJInternalFrame1.getSelectedToolName();
    }

    public Set<String> getPossibleToolNames() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	return executorJInternalFrame1.getPossibleToolNames();
    }

    public Map<String, String> getCurrentToolHolderContentsMap() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	return executorJInternalFrame1.getCurrentToolHolderContentsMap();
    }

    public void putInToolHolderContentsMap(String holder, String contents) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	executorJInternalFrame1.putInToolHolderContentsMap(holder, contents);
    }

    public Map<String, Set<String>> getPossibleToolHolderContentsMap() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == executorJInternalFrame1");
	}
	return executorJInternalFrame1.getPossibleToolHolderContentsMap();
    }

    /**
     * Get the future that can be used to determine when the last requested safe
     * abort is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    public @Nullable XFutureVoid getSafeAbortFuture() {
	return safeAbortFuture;
    }

    private volatile @Nullable XFutureVoid lastClearWayToHoldersFuture = null;

    public boolean isStandAlone() {
	if (null != aprsSystemDisplayJFrame) {
	    return aprsSystemDisplayJFrame.isStandAlone();
	} else {
	    return Boolean.getBoolean("aprs.standalone");
	}
    }

    public XFutureVoid clearWayToHolders(String holderName) {
	if (isStandAlone()) {
	    return XFutureVoid.completedFutureWithName("standAlondeSkipClearWayToHolders(" + holderName + ")");
	}
	if (null == supervisor) {
	    throw new IllegalStateException("null == supervisor");
	}
	XFutureVoid ret = supervisor.clearWayToHolders(this, holderName);
	lastClearWayToHoldersFuture = ret;
	return ret;
    }

    /**
     * Get the future that can be used to determine when the last requested run
     * program is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    public @Nullable XFuture<Boolean> getLastRunProgramFuture() {
	return lastRunProgramFuture;
    }

    /**
     * Get the future that can be used to determine when the last requested resumed
     * action is finished. Only useful for debugging.
     *
     * @return future or null if no resume requested
     */
    public @Nullable XFuture<Boolean> getLastResumeFuture() {
	return lastResumeFuture;
    }

    private volatile @Nullable XFutureVoid safeAbortFuture = null;

    private volatile @Nullable XFutureVoid safeAbortAndDisconnectFuture = null;

    private volatile @Nullable XFutureVoid safeAbortAndDisconnectFutureWaitAll1 = null;

    private volatile @Nullable XFutureVoid safeAbortAndDisconnectFutureWaitAll2 = null;

    private volatile @Nullable XFutureVoid safeAbortAndDisconnectFutureDisconnect2 = null;

    private volatile @Nullable String startSafeAbortComment = null;

    /**
     * Attempt to safely abort the current CRCL program in a way that does not leave
     * the part in the gripper nor in a position obstructing the vision sensor. If
     * the robot currently has a part in the gripper the program will continue until
     * the part is placed and the robot is moved out of the way of the vision
     * system.
     * <p>
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned future.
     *
     * @param comment optional comment is used when debugging to track which future
     *                was created by which caller and to improve log messages
     * @return a future that can be tested or used to wait until the abort is
     *         completed.
     */
    public XFutureVoid startSafeAbort(String comment) {

	if (isAborting()) {
	    String errMsg = "startSafeAbort(" + comment + ") called when already aborting";
	    setTitleErrorString(errMsg);
	    throw new IllegalStateException(errMsg);
	}
	Thread curThread = Thread.currentThread();
	startSafeAbortThread = curThread;
	startSafeAbortTime = System.currentTimeMillis();
	startSafeAbortStackTrace = curThread.getStackTrace();
	startSafeAbortComment = comment;
	takeSnapshots("startSafeAbort." + comment);
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("Exectutor View must be open to use this function.");
	}
	safeAbortFuture = this.executorJInternalFrame1.startSafeAbort(comment)
		.thenRun(() -> {
		    final XFuture<Boolean> continuousDemoFutureFinal = continuousDemoFuture;
		    if (null != continuousDemoFutureFinal) {
			if (!continuousDemoFutureFinal.isDone()) {
			    System.err.println("AprsSystem.startSafeAbort : cancelling continuousDemoFuture="
				    + continuousDemoFutureFinal);
			    continuousDemoFutureFinal.cancelAll(true);
			}
			continuousDemoFuture = null;
		    }
		    setStopRunTime();
		}).thenComposeAsyncToVoid(x -> waitAllLastFutures(), runProgramService);
	return safeAbortFuture;
    }

    private volatile @Nullable Thread startSafeAbortAndDisconnectThread = null;
    private volatile long startSafeAbortAndDisconnectTime = -1;
    private volatile StackTraceElement startSafeAbortAndDisconnectStackTrace @Nullable [] = null;

    private volatile @Nullable Thread startSafeAbortThread = null;

    private volatile long startSafeAbortTime = -1;

    private volatile StackTraceElement startSafeAbortStackTrace @Nullable [] = null;

    private volatile @Nullable String startSafeAbortAndDisconnectComment = null;
    private final AtomicInteger startSafeAbortAndDisconnectCount = new AtomicInteger();

    /**
     * Safely abort the current CRCL program and then disconnect from the robot's
     * CRCL server.
     * <p>
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned future.
     * * @return a future that can be tested or used to wait until the abort and
     * disconnect is completed.
     *
     * @param comment optional comment is used when debugging to track which future
     *                was created by which caller and to improve log messages
     * @return future providing info on when complete
     */
    public XFutureVoid startSafeAbortAndDisconnect(String comment) {

	startingCheckEnabledCheck();
	if (isAborting()) {
	    String errMsg = "startSafeAbort(" + comment + ") called when already aborting";
	    setTitleErrorString(errMsg);
	    throw new IllegalStateException(errMsg);
	}
	XFutureVoid ret;
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	try {

	    Thread curThread = Thread.currentThread();
	    startSafeAbortAndDisconnectComment = comment;
	    startSafeAbortAndDisconnectThread = curThread;
	    startSafeAbortAndDisconnectTime = System.currentTimeMillis();
	    startSafeAbortAndDisconnectStackTrace = curThread.getStackTrace();
	    final boolean connected = isConnected();
	    final boolean doingActons = isDoingActions();
	    final String doingActionsInfo = getIsDoingActionsInfo();
	    int count = startSafeAbortAndDisconnectCount.incrementAndGet();
	    logEvent("START startSafeAbortAndDisconnect", comment, connected, doingActons, count);
	    logToSuper("START startSafeAbortAndDisconnect " + comment + ",connected=" + connected + ",doingActons="
		    + doingActons + ",count=" + count);
	    takeSnapshots("START startSafeAbortAndDisconnect " + comment + ",connected=" + connected + ",doingActons="
		    + doingActons + ",count=" + count);
	    if (connected) {
		if (null == executorJInternalFrame1) {
		    throw new IllegalStateException("Exectutor View must be open to use this function.");
		}

		if (!doingActons) {
		    return disconnectRobot()
			    .alwaysRunAsync(() -> {
				synchronized (this) {
				    logEvent("END startSafeAbortAndDisconnect", comment, connected, doingActons, count);
				    logToSuper("END startSafeAbortAndDisconnect " + comment + ",connected=" + connected
					    + ",doingActons=" + doingActons + ",count=" + count);
				    takeSnapshots("END startSafeAbortAndDisconnect " + comment + ",connected="
					    + connected + ",doingActons=" + doingActons + ",count=" + count);
				}
			    }, runProgramService);
		}
		XFutureVoid localSafeAbortFuture = this.executorJInternalFrame1.startSafeAbort(comment);
		safeAbortFuture = localSafeAbortFuture;

		XFutureVoid localsafeAbortAndDisconnectFutureWaitAll1 = localSafeAbortFuture
			.thenComposeToVoid(
				"startSafeAbortAndDisconnect(" + comment + ") waitAllLastFutures",
				x -> {
				    setStopRunTime();
				    return waitAllLastFutures();
				});
		this.safeAbortAndDisconnectFutureWaitAll1 = localsafeAbortAndDisconnectFutureWaitAll1;
		XFutureVoid localsafeAbortAndDisconnectFutureDisconnect2 = localsafeAbortAndDisconnectFutureWaitAll1
			.thenRunAsync(localSafeAbortFuture.getName() + ".disconnect." + robotName,
				() -> {
				    try {
					disconnectRobotPrivate();
				    } catch (Exception ex) {
					System.err.println("trace = " + Utils.traceToString(trace));
					Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
					if (ex instanceof RuntimeException) {
					    throw (RuntimeException) ex;
					} else {
					    throw new RuntimeException(ex);
					}
				    }
				},
				runProgramService);
		this.safeAbortAndDisconnectFutureDisconnect2 = localsafeAbortAndDisconnectFutureDisconnect2;
		XFutureVoid localsafeAbortAndDisconnectFutureWaitAll2 = localsafeAbortAndDisconnectFutureDisconnect2
			.thenComposeToVoid(x -> waitAllLastFutures())
			.alwaysRunAsync(() -> {
			    synchronized (this) {
				logEvent("END startSafeAbortAndDisconnect", comment, connected, doingActons, count);
				logToSuper("END startSafeAbortAndDisconnect " + comment + ",connected=" + connected
					+ ",doingActons=" + doingActons + ",count=" + count);
				takeSnapshots("END startSafeAbortAndDisconnect " + comment + ",connected=" + connected
					+ ",doingActons=" + doingActons + ",count=" + count);
			    }
			}, runProgramService);
		this.safeAbortAndDisconnectFutureWaitAll2 = localsafeAbortAndDisconnectFutureWaitAll2;
		ret = localsafeAbortAndDisconnectFutureWaitAll2;
	    } else {
		logEvent("startSafeAbortAndDisconnect : already not connected.", comment);
		XFutureVoid localSafeAbortFuture = XFutureVoid
			.completedFutureWithName("startSafeAbortAndDisconnect(" + comment + ").alreadyDisconnected");
		safeAbortFuture = localSafeAbortFuture;
		ret = localSafeAbortFuture;
		logEvent("END startSafeAbortAndDisconnect", comment, connected, doingActons, count);
		logToSuper("END startSafeAbortAndDisconnect " + comment + ",connected=" + connected + ",doingActons="
			+ doingActons + ",count=" + count);
		takeSnapshots("END startSafeAbortAndDisconnect " + comment + ",connected=" + connected + ",doingActons="
			+ doingActons + ",count=" + count);
	    }
	    safeAbortAndDisconnectFuture = ret;
	    return ret;
	} catch (Exception e) {
	    setTitleErrorString(e.toString());
	    ret = new XFutureVoid("startSafeAbortAndDisconnect." + e.toString());
	    ret.completeExceptionally(e);
	    return ret;
	}
    }

    @SuppressWarnings({ "unchecked", "nullness" })
    private XFuture<?> wait(@Nullable XFuture<?> f) {
	if (null == f || f.isCancelled() || f.isCompletedExceptionally() || f.isDone()) {
	    return XFutureVoid.completedFutureWithName("waitReady f=" + f);
	} else {
	    return f.handle((x, t) -> null);
	}
    }

    private volatile @Nullable XFutureVoid lastWaitAllFuturesRet = null;

    private synchronized XFutureVoid waitAllLastFutures() {
	XFutureVoid ret = XFutureVoid.allOf(wait(lastContinueActionListFuture),
		wait(lastRunProgramFuture),
		wait(lastStartActionsFuture));
	lastWaitAllFuturesRet = ret;
	return ret;
    }

    /**
     * Get a map of updates that were attempted the last time data was received from
     * the vision system.
     * <p>
     * Only useful for debugging.
     *
     * @return map of results
     */
    public Map<String, UpdateResults> getDbUpdatesResultMap() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getUpdatesResultMap();
    }

    public int getVisionClientSkippedCount() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getVisionClientSkippedCount();
    }

    public int getVisionClientIgnoreCount() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getVisionClientIgnoreCount();
    }

    public @Nullable String getVisionToDbPerformanceLine() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getPerformanceLine();
    }

    public int getVisionClientUpdateCount() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getVisionClientUpdateCount();
    }

    public int getVisionClientUpdateAquireOffCount() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getVisionClientUpdateAquireOffCount();
    }

    public int getVisionClientUpdateNoCheckRequiredPartsCount() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getVisionClientUpdateNoCheckRequiredPartsCount();
    }

    public int getVisionClientUpdateSingleUpdateListenersEmptyCount() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.getVisionClientUpdateSingleUpdateListenersEmptyCount();
    }

    private volatile @Nullable XFutureVoid disconnectRobotFuture = null;

    private volatile boolean debug = false;

    /**
     * Disconnect from the robot's crcl server and set robotName to null.
     * <p>
     * Note: setConnected(false) also disconnects from the crcl server but leaves
     * the robotName unchanged.
     *
     * @return future providing info on when complete
     */
    public synchronized XFutureVoid disconnectRobot() {
	if (null == robotName
		&& !isConnected()) {
	    return XFutureVoid.completedFutureWithName("alreadyDisconnected");
	}
	startingCheckEnabledCheck();
	disconnectRobotCount.incrementAndGet();
	checkReadyToDisconnect();
	enableCheckedAlready = false;
	XFutureVoid waitForPauseFuture = waitForPause();
	if (runProgramService == null || runProgramService.isShutdown() || !isConnected()) {
	    return waitForPauseFuture;
	}
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	XFutureVoid ret = waitForPauseFuture
		.thenRunAsync("disconnectRobot(" + getRobotName() + ")",
			() -> {
			    try {
				disconnectRobotPrivate();
			    } catch (Exception ex) {
				System.err.println("trace = " + Utils.traceToString(trace));
				Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
				if (ex instanceof RuntimeException) {
				    throw (RuntimeException) ex;
				} else {
				    throw new RuntimeException(ex);
				}
			    }
			},
			runProgramService);
	this.disconnectRobotFuture = ret;
	if (debug) {
	    println("disconnectRobotFuture = " + disconnectRobotFuture);
	    println("runProgramService = " + runProgramService);
	}
	return ret;
    }

    private final AtomicInteger disconnectRobotCount = new AtomicInteger();

    public boolean isClosing() {
	return closing;
    }

    private synchronized void disconnectRobotPrivate() {
	boolean startingIsConnected = isConnected();
	String startingRobotName = this.getRobotName();
	String startingCrclHost = this.getRobotCrclHost();
	int startingCrclPort = this.getRobotCrclPort();
	startingCheckEnabledCheck();
	if (!closing) {
	    checkReadyToDisconnect();
	}
	enableCheckedAlready = false;
	disconnectRobotCount.incrementAndGet();
//        if (null != ContinuousDemoFuture) {
//            ContinuousDemoFuture.cancelAll(true);
//        }
	setStopRunTime();
	setThreadName();
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.disconnect();
	} else {
	    throw new NullPointerException("crclClientJInternalFrame");
	}
	if (isConnected()) {
	    throw new RuntimeException("still connected after disconnect : this=" + this);
	}
	if (null != startingRobotName) {
	    takeSnapshots("disconnectRobot");
	}
	this.setRobotName(null);
	println("AprsSystem with taskName=" + taskName
		+ " disconnectRobot completed from robotNeme=" + startingRobotName
		+ ", host=" + startingCrclHost + ":" + startingCrclPort
		+ ", disconnectRobotCount=" + disconnectRobotCount
		+ ",startingIsConnected=" + startingIsConnected);

	connectedRobotCheckBox.setSelected(false);
	if (isConnected()) {
	    throw new RuntimeException("still connected after disconnect : this=" + this);
	}
    }

    private void startingCheckEnabledCheck() throws IllegalStateException {
	if (startingCheckEnabled && !closing) {
	    String errMsg = "trying to change robot name while starting a check enabled";
	    System.err.println("startingCheckEnabledTrace=" + Utils.traceToString(startingCheckEnabledTrace));
	    setTitleErrorString(errMsg);
	    throw new IllegalStateException(errMsg);
	}
    }

    private volatile String origCrclRobotHost = "";

    /**
     * Get the value of origCrclRobotHost
     *
     * @return the value of origCrclRobotHost
     */
    public String getOrigCrclRobotHost() {
	maybeSetOrigCrclRobotHost(getRobotCrclHost());
	return origCrclRobotHost;
    }

    private volatile int origCrclRobotPort = -1;

    /**
     * Get the value of origCrclRobotPort
     *
     * @return the value of origCrclRobotPort
     */
    public int getOrigCrclRobotPort() {
	maybeSetOrigCrclRobotPort(getRobotCrclPort());
	return origCrclRobotPort;
    }

    public XFutureVoid connectRobot() {
	final String robotNameFinal = getRobotName();
	if (null == robotNameFinal) {
	    throw new NullPointerException("getRobotName() returned null");
	}
	final String robotCrclHostFinal = getRobotCrclHost();
	if (null == robotCrclHostFinal) {
	    throw new NullPointerException("getRobotCrclHost() returned null");
	}
	return connectRobot(robotNameFinal, robotCrclHostFinal, getRobotCrclPort());
    }

    private volatile StackTraceElement connectRobotTrace @Nullable [] = null;

    /**
     * Connect to a given robot with a CRCL server running on the given host and TCP
     * port.
     *
     * @param robotNameArg name of the robot
     * @param hostArg      host running robot's CRCL server
     * @param portArg      (TCP) port robot's CRCL server is bound to
     * @return future providing info on when complete
     */
    public XFutureVoid connectRobot(String robotNameArg, String hostArg, int portArg) {
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	this.connectRobotTrace = trace;
	maybeSetOrigRobotName(robotNameArg);
	maybeSetOrigCrclRobotHost(hostArg);
	maybeSetOrigCrclRobotPort(portArg);
	boolean wasConnected0 = isConnected();
	final boolean wasPaused0 = isPaused();
	boolean nameOk = null != this.robotName
		&& Objects.equals(this.robotName, robotNameArg);
	final int startPort = this.getRobotCrclPort();
	boolean portMatch = startPort == portArg;
	if (wasConnected0
		&& !wasPaused0
		&& nameOk
		&& Objects.equals(this.getRobotCrclHost(), hostArg)
		&& portMatch) {
	    updateConnectedRobotDisplay(wasConnected0, robotNameArg, hostArg, portArg);
	    return XFutureVoid.completedFuture();
	} else if (wasConnected0) {
	    System.out.println("wasConnected0 = " + wasConnected0);
	    if (wasPaused0) {
		System.out.println("wasPaused0 = " + wasPaused0);
	    }
	    if (!nameOk) {
		System.out.println("nameOk = " + nameOk);
		System.out.println("this.robotName = " + this.robotName);
		System.out.println("robotNameArg = " + robotNameArg);
	    }
	    if (!portMatch) {
		System.out.println("portMatch = " + portMatch);
		System.out.println("startPort = " + startPort);
		System.out.println("portArg = " + portArg);
	    }
	    debugDumpStack();
	}
	logEvent("connectRobot", robotNameArg + " -> " + hostArg + ":" + portArg);
	enableCheckedAlready = false;
	return waitForPause()
		.thenRunAsync(() -> connectRobotPrivate(robotNameArg, hostArg, portArg, wasConnected0, trace),
			runProgramService)
		.alwaysRun(() -> logEvent("finished connectRobot", robotNameArg + " -> " + hostArg + ":" + portArg));
    }

    private void maybeSetOrigCrclRobotPort(int port) {
	if (-1 == this.origCrclRobotPort) {
	    if (port > 0) {
		this.origCrclRobotPort = port;
	    }
	}
    }

    private void maybeSetOrigCrclRobotHost(@Nullable String host) {
	if (null == this.origCrclRobotHost || this.origCrclRobotHost.length() < 1) {
	    if (null != host && host.length() > 0) {
		this.origCrclRobotHost = host;
	    }
	}
    }

    private void connectRobotPrivate(String robotName, String host, int port, boolean wasConnected0,
	    StackTraceElement trace[]) {
	setThreadName();
	enableCheckedAlready = false;
	if (this.closing) {
	    return;
	}
	int oldPort = getRobotCrclPort();
	String oldRobotName = getRobotName();
	boolean wasConnected1 = isConnected();
	try {
	    CrclSwingClientJInternalFrame frm = crclClientJInternalFrame;
	    if (null != frm) {
		setRobotName(robotName);
		frm.connect(host, port);
	    }
	    maybeSetOrigCrclRobotHost(host);
	    maybeSetOrigCrclRobotPort(port);
	    updateConnectedRobotDisplay(isConnected(), robotName, host, port);
	} catch (Exception e) {
	    System.out.println("");
	    System.out.flush();
	    System.err.println("");
	    final String traceString = "trace = " + CRCLUtils.traceToString(trace);
	    System.err.println(traceString);
	    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE,
		    "robotName=" + robotName + ",host=" + host + ",port=" + port + ",oldRobotName=" + oldRobotName
			    + ",oldPort=" + oldPort + ",wasConnected0=" + wasConnected0 + ",wasConnected1="
			    + wasConnected1 + "," + traceString,
		    e);
	    throw new RuntimeException(e);
	}
    }

    /**
     * Get robot's CRCL host.
     *
     * @return robot's CRCL host.
     */
    public @Nullable String getRobotCrclHost() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getHost();
	}
	return null;
    }

    /**
     * Get robot's CRCL port number.
     *
     * @return robot's CRCL port number.
     */
    public int getRobotCrclPort() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getPort();
	}
	return -1;
    }

    private final ConcurrentLinkedDeque<XFuture<@Nullable ?>> futuresToCompleteOnUnPause = new ConcurrentLinkedDeque<>();

    public void notifyPauseFutures() {
	for (XFuture<@Nullable ?> f : futuresToCompleteOnUnPause) {
	    f.complete(null);
	}
    }

    private void cancelPauseFutures() {
	for (XFuture<@Nullable ?> f : futuresToCompleteOnUnPause) {
	    f.cancelAll(true);
	}
    }

    private volatile @Nullable XFutureVoid lastPauseFuture = null;

    private XFutureVoid waitForPause() {
	boolean paused = isPaused();
	XFutureVoid pauseFuture = new XFutureVoid("pauseFuture." + paused);
	if (paused) {
	    println("adding " + pauseFuture + " to " + futuresToCompleteOnUnPause);
	    futuresToCompleteOnUnPause.add(pauseFuture);
	} else {
	    pauseFuture.complete(null);
	}
	lastPauseFuture = pauseFuture;
	return pauseFuture;
    }

    private volatile @Nullable XFuture<Boolean> lastContinueActionListFuture = null;

    private volatile @Nullable XFuture<Boolean> lastPrivateContinueActionListFuture = null;

    private volatile @Nullable String lastContinueActionListFutureComment = null;

    private volatile int lastContinueStartAbortCount = -1;

    private volatile StackTraceElement continueActionListTrace @Nullable [] = null;
    private volatile StackTraceElement privateContinueActionListTrace @Nullable [] = null;
    private volatile @Nullable String continueActionsComment = null;
    private final AtomicInteger continueActionsListCount = new AtomicInteger();

    /**
     * Continue or start executing the currently loaded set of PDDL actions.
     * <p>
     * The actions will be executed in another thread after this method returns. The
     * task can be monitored or canceled using the returned future.
     *
     * @param comment optional comment for why continueActionList is needed used for
     *                logs, and/or snapshots
     * @return a future that can be used to monitor, extend or cancel the underlying
     *         task.
     */
    public XFuture<Boolean> continueActionList(String comment) {
	try {
	    final ExecutorJInternalFrame pddlExecutorJInternalFrame1Final = executorJInternalFrame1;
	    if (null == pddlExecutorJInternalFrame1Final) {
		throw new NullPointerException("pddlExecutorJInternalFrame1");
	    }
	    if (pddlExecutorJInternalFrame1Final.getActionsList().isEmpty()) {
//                System.out.println("pddlExecutorJInternalFrame1Final.getActionsList().isEmpty()");
		return XFuture.completedFutureWithName("emptyActionsList." + comment, true);
//                throw new IllegalStateException("pddlExecutorJInternalFrame1Final.getActionsList().isEmpty(), comment="+comment);
	    }
	    checkFutures();
	    boolean connected0 = isConnected();
	    continueActionsComment = comment;
	    StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	    continueActionListTrace = trace;
	    XFuture<Boolean> ret;
	    final boolean revFlag = this.isReverseFlag();
	    int caCount = continueActionsListCount.incrementAndGet();
	    int saCount = this.startActionsCount.get();
	    int startAbortCount = getSafeAbortRequestCount();
	    if (revFlag != lastStartActionsReverseFlag) {
		printSetReverseTraces();
		throw new IllegalStateException(
			"revFlag=" + revFlag + ",lastStartActionsReverseFlag=" + lastStartActionsReverseFlag);
	    }
	    lastContinueStartAbortCount = startAbortCount;
	    logEvent("START continueActionList", comment, revFlag, caCount, saCount, startAbortCount);
	    logToSuper("START continueActionList \"" + toString() + "\" " + saCount + " " + caCount + " " + revFlag
		    + " " + startAbortCount + " " + comment);
	    takeSnapshots("START continueActionList " + saCount + " " + caCount + " " + revFlag + " " + startAbortCount
		    + " " + comment);
	    endLogged = false;
	    if (!revFlag) {
		if (alternativeForwardContinueActions != null) {
		    ret = alternativeForwardContinueActions.doActions();
		} else {
		    ret = privateContinueActionList(comment, startAbortCount, connected0);
		}
	    } else if (alternativeReverseContinueActions != null) {
		ret = alternativeReverseContinueActions.doActions();
	    } else {
		ret = privateContinueActionList(comment, startAbortCount, connected0);
	    }
	    ret = ret
		    .peekException(this::logException)
		    .alwaysRunAsync(() -> {
			synchronized (this) {
			    setEndLogged(trace, comment);
			    logEvent("END continueActionList", comment, revFlag, caCount, saCount);
			    boolean isAborting = isAborting() || startAbortCount != this.getSafeAbortRequestCount();
			    logToSuper("END continueActionList \"" + toString() + "\" " + saCount + " " + caCount + " "
				    + revFlag + " " + startAbortCount + " " + isAborting + " " + comment);
			    takeSnapshots("END continueActionList " + saCount + " " + caCount + " " + revFlag + " "
				    + startAbortCount + " " + isAborting + " " + comment);
			    if (isAborting && !isDoingActions()) {
				pddlExecutorJInternalFrame1Final.completeSafeAbort();
			    }
			}
		    }, runProgramService);
	    lastContinueActionListFuture = ret;
	    return ret;
	} catch (Exception e) {
	    System.err.println("comment=" + comment);
	    setTitleErrorString(e.getMessage());
	    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", e);
	    if (e instanceof RuntimeException) {
		throw (RuntimeException) e;
	    } else {
		throw new RuntimeException(e);
	    }
	}
    }

    public void printSetReverseTraces() {
	println("startActionsTrace = " + Utils.traceToString(startActionsTrace));
	System.err.println(
		"setReverseCheckBoxSelectedTrueTrace = " + Utils.traceToString(setReverseCheckBoxSelectedTrueTrace));
	System.err.println(
		"setReverseCheckBoxSelectedFalseTrace = " + Utils.traceToString(setReverseCheckBoxSelectedFalseTrace));
	System.err.println("setReverseFlagTrueTrace = " + Utils.traceToString(setReverseFlagTrueTrace));
	System.err.println("setReverseFlagFalseTrace = " + Utils.traceToString(setReverseFlagFalseTrace));
    }

    private void logException(Throwable e) throws RuntimeException {
	if (!closing) {
	    setTitleErrorString(e.getMessage());
	    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE,
		    "logException:" + getTaskName() + ":" + e.getMessage(), e);
	    if (e instanceof RuntimeException) {
		throw (RuntimeException) e;
	    } else {
		throw new RuntimeException(e);
	    }
	}
    }

    private volatile StackTraceElement setEndLogCallerTrace @Nullable [] = null;
    private volatile @Nullable String setEndLogCallerComment = null;
    private volatile StackTraceElement setEndLogTrace @Nullable [] = null;
    private volatile @Nullable Thread setEndLogThread = null;

    private void setEndLogged(StackTraceElement callerTrace[], String comment) {
	setEndLogTrace = Thread.currentThread().getStackTrace();
	setEndLogCallerComment = comment;
	setEndLogCallerTrace = callerTrace;
	setEndLogThread = Thread.currentThread();
	endLogged = true;
    }

    private final AtomicInteger privateContinueActionListCount = new AtomicInteger();
    private volatile boolean runningPrivateContinueActionList = false;

    private synchronized XFuture<Boolean> privateContinueActionList(String comment, int startAbortCount,
	    boolean connected0) throws IllegalStateException {
	setStartRunTime();
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	boolean connected1 = isConnected();
	privateContinueActionListTrace = trace;
	lastContinueActionListFutureComment = comment;
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	if (endLogged) {
	    throw new IllegalStateException("endLogged");
	}
	runningPrivateContinueActionList = true;
	int count = privateContinueActionListCount.incrementAndGet();
	logEvent("START privateContinueActionList", comment, startAbortCount, count);
	takeSnapshots("START privateContinueActionList" + comment + "," + startAbortCount + "," + count);
	logToSuper("START privateContinueActionList" + comment + "," + startAbortCount + "," + count);

	XFuture<Boolean> ret = waitForPause()
		.thenApplyAsync("AprsSystem.continueActionList" + comment,
			x -> {
			    try {
				if (endLogged) {
				    throw new IllegalStateException("endLogged");
				}
				setThreadName();
				takeSnapshots("continueActionList" + ((comment != null) ? comment : "") + " "
					+ startAbortCount);
				updateRobotLimits();
				if (null == executorJInternalFrame1) {
				    throw new IllegalStateException(
					    "PDDL Exectutor View must be open to use this function.");
				}
				if (getSafeAbortRequestCount() == startAbortCount) {
				    boolean completActionListRet = executorJInternalFrame1
					    .completeActionList("continueActionList" + comment, startAbortCount, trace);
				    logEvent("completeActionListRet", completActionListRet);
				    takeSnapshots("after continueActionList" + ((comment != null) ? comment : "") + " "
					    + startAbortCount);
				    return completActionListRet && (getSafeAbortRequestCount() == startAbortCount);
				}
				return false;
			    } catch (Exception ex) {
				System.out.println();
				System.out.flush();
				System.err.println("connected0 = " + connected0);
				System.err.println("connected1 = " + connected1);
				System.err.println("count = " + count);
				System.err.println("trace = " + Utils.traceToString(trace));
				System.err.println("comment = " + comment);
				System.err
					.println("setEndLogCallerTrace = " + Utils.traceToString(setEndLogCallerTrace));
				System.err.println("setEndLogTrace = " + Utils.traceToString(setEndLogTrace));
				System.err.println("setEndLogCallerComment = " + setEndLogCallerComment);
				System.err.println("setEndLogThread = " + setEndLogThread);
				System.err.println();
				System.err.flush();
				println("lastPrivateContinueActionListFuture = " + lastPrivateContinueActionListFuture);
				if (null != lastPrivateContinueActionListFuture) {
				    lastPrivateContinueActionListFuture.printStatus(System.err);
				    lastPrivateContinueActionListFuture.printProfile(System.err);
				}
				Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
				setTitleErrorString(ex.getMessage());
				showException(ex);
				if (ex instanceof RuntimeException) {
				    throw (RuntimeException) ex;
				} else {
				    throw new RuntimeException(ex);
				}
			    }
			}, runProgramService);
	XFuture<Boolean> ret2 = ret
		.alwaysRunAsync(() -> {
		    synchronized (this) {
			logEvent("END privateContinueActionList", comment, startAbortCount, count, ret);
			takeSnapshots("END privateContinueActionList" + comment + "," + startAbortCount + "," + count
				+ "," + ret);
			logToSuper("END privateContinueActionList" + comment + "," + startAbortCount + "," + count + ","
				+ ret);
			setEndLogged(trace, comment + ",count=" + count);
			runningPrivateContinueActionList = false;
		    }
		}, runProgramService);
	lastPrivateContinueActionListFuture = ret2;
	return ret2;
    }

    /**
     * Get the closest distance between the robot TCP and any part.
     *
     * @return closest distance
     */
    public double getClosestRobotPartDistance() {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	return this.object2DViewJInternalFrame.getClosestRobotPartDistance();
    }

    /**
     * Get the closest distance between the robot TCP and any part.
     *
     * @return closest distance
     */
    public @Nullable PhysicalItem getClosestRobotPart() {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	return this.object2DViewJInternalFrame.getClosestRobotPart();
    }

    /**
     * Get the current setting for whether the object view is using simulation.
     *
     * @return if object view is simulated
     */
    public boolean isObjectViewSimulated() {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	return this.object2DViewJInternalFrame.isSimulated();
    }

    public XFutureVoid loadObjectViewSimulatedFile(File f) throws IOException {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	return this.object2DViewJInternalFrame.loadFile(f);
    }

    public void loadObjectViewSimulatedFile(File f, boolean convertRotToRad, boolean zeroRotations) throws IOException {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	this.object2DViewJInternalFrame.loadFile(f, convertRotToRad, zeroRotations);
    }

    /**
     * Set the value of taskName
     *
     * @param taskName new value of taskName
     */
    public void setTaskName(String taskName) {
	this.taskName = taskName;
	clearLogDirCache();
	submitUpdateTitle();
    }

    private void clearLogDirCache() {
	this.runName = null;
	this.logDir = null;
	this.logCrclProgramDir = null;
	this.logImageDir = null;
    }

    private @Nullable String robotName = null;

    /**
     * Get the value of robotName
     *
     * @return the value of robotName
     */
    public @Nullable String getRobotName() {
	return robotName;
    }

    private volatile @Nullable Thread setRobotNameNullThread = null;
    private volatile StackTraceElement setRobotNameNullStackTrace @Nullable [] = null;
    private volatile long setRobotNameNullThreadTime = -1;
    private volatile @Nullable Thread setRobotNameNonNullThread = null;
    private volatile StackTraceElement setRobotNameNonNullStackTrace @Nullable [] = null;
    private volatile long setRobotNameNonNullThreadTime = -1;

    public void printRobotNameActivy(PrintStream ps) {
	ps.println("robotName = " + robotName);
	ps.println("setRobotNameNullThreadTime = " + setRobotNameNullThreadTime);
	long t0 = System.currentTimeMillis();
	ps.println("System.currentTimeMillis() = " + t0);
	long td1 = t0 - setRobotNameNullThreadTime;
	ps.println("(System.currentTimeMillis() - setRobotNameNullThreadTime) = " + td1);
	ps.println("setRobotNameNullThreadTime = " + setRobotNameNullThreadTime);
	long td2 = t0 - setRobotNameNonNullThreadTime;
	ps.println("(System.currentTimeMillis() - setRobotNameNonNullThreadTime) = " + td2);
	ps.println("setRobotNameNonNullStackTrace = " + Utils.traceToString(setRobotNameNonNullStackTrace));
	ps.println("setRobotNameNullStackTrace = " + Utils.traceToString(setRobotNameNullStackTrace));
	ps.println("setConnectedTrace = " + Utils.traceToString(setConnectedTrace));
    }

    /**
     * Set the value of robotName
     *
     * @param robotName new value of robotName
     */
    public void setRobotName(@Nullable String robotName) {
	startingCheckEnabledCheck();
	maybeSetOrigRobotName(robotName);
	if (null == robotName) {
	    disconnectRobotCount.incrementAndGet();
	    enableCheckedAlready = false;
	    checkReadyToDisconnect();
	    setRobotNameNullThread = Thread.currentThread();
	    setRobotNameNullStackTrace = setRobotNameNullThread.getStackTrace();
	    setRobotNameNullThreadTime = System.currentTimeMillis();
	} else {
	    if (!robotName.equals(this.robotName)) {
		enableCheckedAlready = false;
	    }
	    setRobotNameNonNullThread = Thread.currentThread();
	    setRobotNameNonNullStackTrace = setRobotNameNonNullThread.getStackTrace();
	    setRobotNameNonNullThreadTime = System.currentTimeMillis();
	}
	this.robotName = robotName;
	clearLogDirCache();
	submitUpdateTitle();
    }

    private void submitUpdateTitle() {
	if (closing) {
	    return;
	}
	if (null == runProgramServiceThread
		|| runProgramServiceThread == Thread.currentThread()
		|| runProgramService.isShutdown()
		|| runProgramService.isTerminated()) {
	    updateTitle();
	} else {
	    runProgramService.submit(() -> updateTitle());
	}
    }

    private void maybeSetOrigRobotName(@Nullable String robotName1) {
	if (null == this.origRobotName || this.origRobotName.length() < 1) {
	    if (null != robotName1 && robotName1.length() > 0) {
		this.origRobotName = robotName1;
	    }
	}
    }

    private volatile boolean useTeachTableChecked = false;

    void setUseTeachTableChecked(boolean useTeachTableChecked) {
	this.useTeachTableChecked = useTeachTableChecked;
    }

    public boolean getUseTeachTable() {
	return useTeachTableCheckBox.isSelected();
    }

    public void setUseTeachTable(boolean useTeachTable) {
	useTeachTableCheckBox.setSelected(useTeachTable);
    }

    private void checkReadyToDisconnect() throws IllegalStateException {
	if (closing || !isConnected()) {
	    return;
	}
	if (isRunningCrclProgram()) {
	    String msg = "setRobotName(null)/disconnect() called when running crcl program";
	    System.err.println("sys.getLastRunningProgramTrueInfo=" + this.getLastRunningProgramTrueInfo());
	    setTitleErrorString(msg);
	    boolean chk = isRunningCrclProgram();
	    throw new IllegalStateException(msg);
	}
	if (isDoingActions()) {
	    String msg = "setRobotName(null)/disconnect() called when running doing actions";
	    String info = getIsDoingActionsInfo();
	    logEvent("checkReadyToDisconnect:isDoingActionsInfo", info);
	    logToSuper("checkReadyToDisconnect:isDoingActionsInfo" + info);
	    setTitleErrorString(msg);
	    boolean chk = isDoingActions();
	    throw new IllegalStateException(msg);
	}
    }

    /**
     * Register a program line listener that will be notified each time a line of
     * the CRCL program is executed.
     *
     * @param l listener to be registered
     */
    public void addProgramLineListener(ProgramLineListener l) {
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.addProgramLineListener(l);
	}
    }

    /**
     * Remove a previously added program line listener.
     *
     * @param l listener to be removed
     */
    public void removeProgramLineListener(ProgramLineListener l) {
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.removeProgramLineListener(l);
	}
    }

    /**
     * Modify the given pose by applying all of the currently added position maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PoseType convertVisionToRobotPose(PoseType poseIn) {
	if (null != executorJInternalFrame1) {
	    return executorJInternalFrame1.correctPose(poseIn);
	}
	return poseIn;
    }

    /**
     * Modify the given pose by applying all of the currently added position maps.
     *
     * @param pointIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PointType convertVisionToRobotPointType(PointType pointIn) {
	if (null != executorJInternalFrame1) {
	    return executorJInternalFrame1.correctPoint(pointIn);
	}
	return pointIn;
    }

    /**
     * Modify the given pose by applying all of the currently added position maps.
     *
     * @param cartIN the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PmCartesian convertVisionToRobotPmCartesian(PmCartesian cartIN) {
	return CRCLPosemath.toPmCartesian(convertVisionToRobotPointType(CRCLPosemath.toPointType(cartIN)));
    }

    /**
     * Apply inverses of currently added position maps in reverse order.
     *
     * @param ptIn pose to reverse correction
     * @return pose in original vision/database coordinates
     */
    public PointType convertRobotToVisionPoint(PointType ptIn) {
	if (null != executorJInternalFrame1) {
	    return executorJInternalFrame1.reverseCorrectPoint(ptIn);
	}
	return ptIn;
    }

    private final List<CurrentPoseListener> unaddedPoseListeners = new ArrayList<>();

    /**
     * Register a listener to be notified of every change in the robots pose.
     *
     * @param l listener to register
     */
    public void addCurrentPoseListener(CurrentPoseListener l) {
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.addCurrentPoseListener(l);
	} else {
	    unaddedPoseListeners.add(l);
	}
    }

    /**
     * Remove a previously added listener.
     *
     * @param l listener to be removed
     */
    public void removeCurrentPoseListener(CurrentPoseListener l) {
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.removeCurrentPoseListener(l);
	}
    }

    /**
     * Set the currently loaded CRCL program
     *
     * @param program CRCL program to load. in the schema
     * @throws javax.xml.bind.JAXBException if the program is missing a required
     *                                      element needed to generate a valid xml
     *                                      file.
     */
    public synchronized void setCRCLProgram(CRCLProgramType program) throws JAXBException {
	if (null != crclClientJInternalFrame) {
	    setProgram(program);
	}
    }

    private volatile @Nullable XFuture<Boolean> lastRunProgramFuture = null;

    private volatile StackTraceElement startCrclProgramTrace @Nullable [] = null;

    /**
     * Load the given program an begin running it.
     * <p>
     * The program will be run asynchronously in another thread after this method
     * has returned. The task can be modified, canceled or extended with the
     * returned future. The boolean contained within the future will be true if the
     * program completed successfully and false for non exceptional errors.
     *
     * @param program program to be loaded
     * @return future that can be used to monitor, cancel or extend the underlying
     *         task
     */
    public XFuture<Boolean> startCRCLProgram(CRCLProgramType program) {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException(
		    "Must show CRCL client frame before starting CRCL program: crclClientJInternalFrame==null");
	}
	setStartRunTime();
	StackTraceElement thisTrace[] = Thread.currentThread().getStackTrace();
	startCrclProgramTrace = thisTrace;
	lastRunProgramFuture = waitForPause()
		.thenApplyAsync("startCRCLProgram(" + program.getName() + ").runProgram", x -> {
		    try {
			return runCRCLProgram(program);
		    } catch (Exception ex) {
			System.out.println("startCrclProgramTrace = " + Utils.traceToString(thisTrace));
			if (ex instanceof RuntimeException) {
			    throw (RuntimeException) ex;
			} else {
			    throw new RuntimeException(ex);
			}
		    }
		}, runProgramService);
	return lastRunProgramFuture;
    }

    private final List<PmCartesianMinMaxLimit> limits = new ArrayList<>();

    public List<PmCartesianMinMaxLimit> getLimits() {
	return limits;
    }

    public void readLimitsFromCsv(File csvFile) throws IOException {
	try (CSVParser parser = new CSVParser(new FileReader(csvFile), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
	    Map<String, Integer> headerMap = parser.getHeaderMap();
	    if (null == headerMap) {
		throw new IllegalArgumentException(csvFile.getCanonicalPath() + " does not have header");
	    }
	    List<CSVRecord> records = parser.getRecords();
	    int skipRows = 0;
	    limits.clear();
	    for (CSVRecord rec : records) {
		PmCartesian max = new PmCartesian(
			Double.parseDouble(rec.get("MaxX")),
			Double.parseDouble(rec.get("MaxY")),
			Double.parseDouble(rec.get("MaxZ")));
		PmCartesian min = new PmCartesian(
			Double.parseDouble(rec.get("MinX")),
			Double.parseDouble(rec.get("MinY")),
			Double.parseDouble(rec.get("MinZ")));
		limits.add(new PmCartesianMinMaxLimit(min, max));
	    }
	}
    }

    public void writeLimitsFromCsv(File csvFile) throws IOException {
	try (CSVPrinter printer = new CSVPrinter(new FileWriter(csvFile),
		CSVFormat.DEFAULT.withHeader(PmCartesianMinMaxLimit.getHeaders()))) {
	    for (int i = 0; i < limits.size(); i++) {
		PmCartesianMinMaxLimit minMax = limits.get(i);
		printer.printRecord(minMax.toObjArray());
	    }
	}
    }

    private boolean isMove(MiddleCommandType midCmd) {
	return (midCmd instanceof MoveToType
		|| midCmd instanceof MoveThroughToType
		|| midCmd instanceof ActuateJointsType
		|| midCmd instanceof SetEndEffectorType);
    }

    private boolean checkNoMoves(CRCLProgramType program) {
	final List<MiddleCommandType> middleCommandsList = CRCLUtils.middleCommands(program);
	for (MiddleCommandType midCmd : middleCommandsList) {
	    if (isMove(midCmd)) {
		return false;
	    }
	    if (midCmd instanceof CRCLCommandWrapper) {
		CRCLCommandWrapper wrapper = (CRCLCommandWrapper) midCmd;
		if (isMove(wrapper.getWrappedCommand())) {
		    return false;
		}
	    }
	}
	return true;
    }

    private void processWrapperCommands(CRCLProgramType program) {
	final List<MiddleCommandType> middleCommandsList = CRCLUtils.middleCommands(program);
	for (MiddleCommandType cmd : middleCommandsList) {
	    if (cmd instanceof CRCLCommandWrapper) {
		CRCLCommandWrapper wrapper = (CRCLCommandWrapper) cmd;
		wrapper.setCurProgram(program);
		wrapper.notifyOnStartListeners();
		wrapper.notifyOnDoneListeners();
	    }
	}
    }

    public XFutureVoid runOnDispatchThread(final @UI Runnable r) {
	return runOnDispatchThread("runOnDispatchThread", r);
    }

    private void logAndRethrowException(Throwable ex) {
	Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", ex);
	setTitleErrorString(ex.getMessage());
	showException(ex);
	if (ex instanceof RuntimeException) {
	    throw (RuntimeException) ex;
	} else {
	    throw new RuntimeException(ex);
	}
    }

    public XFutureVoid runOnDispatchThread(String name, final @UI Runnable r) {
	if (null != supervisor) {
	    return supervisor.runOnDispatchThread(name, r)
		    .peekNoCancelException(this::logAndRethrowException);
	} else {
	    return aprs.misc.Utils.runOnDispatchThread(name, r)
		    .peekNoCancelException(this::logAndRethrowException);
	}
    }

    public <T> XFutureVoid submitDisplayConsumer(Consumer<T> consumer, T value) {
	if (null != supervisor) {
	    return supervisor.submitDisplayConsumer(consumer, value);
	}
	return runOnDispatchThread(() -> {
	    consumer.accept(value);
	});
    }

    private volatile StackTraceElement runCRCLProgramTrace @Nullable [] = null;

    private final AtomicInteger runProgramCount = new AtomicInteger();
    private final AtomicLong runProgramTime = new AtomicLong();

    /**
     * Run a CRCL program.
     *
     * @param program CRCL program to run
     * @return whether the program completed successfully
     */
    public boolean runCRCLProgram(CRCLProgramType program) {
	boolean ret;
	runCRCLProgramTrace = Thread.currentThread().getStackTrace();
	String programString;
	try {
	    programString = CRCLSocket.getUtilSocket().programToPrettyString(program, false);
	} catch (CRCLException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	    programString = null;
	}
	File programFile = null;
	try {
	    if (null != programString) {
		programFile = Utils.createTempFile("CRCLProgram", ".xml");
		try (PrintWriter pw = new PrintWriter(new FileWriter(programFile))) {
		    pw.println(programString);
		}
	    }
	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	    programFile = null;
	}
	if (enableCheckedAlready && checkNoMoves(program)) {
	    logEvent("skipping runCrclProgram", programFile);
	    processWrapperCommands(program);
	    return true;
	}
	long startTime = logEvent("start runCrclProgram", programFile);
	final CrclSwingClientJInternalFrame crclClientJInternalFrameFinal = crclClientJInternalFrame;
	if (null == crclClientJInternalFrameFinal) {
	    throw new IllegalStateException("CRCL Client View must be open to use this function.");
	}
	final List<MiddleCommandType> middleCommandsList = CRCLUtils.middleCommands(program);
	int origSize = middleCommandsList.size();
	try {
	    setProgram(program);
	    updateRobotLimits();
	    ret = crclClientJInternalFrameFinal.runCurrentProgram(isStepMode());
	    if (!ret) {
		println("crclClientJInternalFrame.getRunProgramReturnFalseTrace() = "
			+ Utils.traceToString(crclClientJInternalFrameFinal.getRunProgramReturnFalseTrace()));
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString(ex.getMessage());
	    showException(ex);

	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	} finally {
	    int curSize = middleCommandsList.size();
	    String origSizeString = (origSize != curSize) ? ("\n origSize=" + origSize) : "";
	    final long timeDiff = System.currentTimeMillis() - startTime;
	    int count = runProgramCount.incrementAndGet();
	    long totalRunProgramTime = runProgramTime.addAndGet(timeDiff);
	    logEvent("end runCrclProgram",
		    "(" + crclClientJInternalFrameFinal.getCurrentProgramLine() + "/" + curSize + ")"
			    + origSizeString
			    + "\n started at" + startTime
			    + "\n timeDiff=" + timeDiff
			    + "\n runProgramCount=" + count
			    + "\n totalRunProgramTime=" + totalRunProgramTime);
	    logToSuper("end runCrclProgram: count=" + count + ",timeDiff=" + timeDiff + ",totalRunProgramTime="
		    + totalRunProgramTime);
	}
	return ret;
    }

    public void updateRobotLimits() {
	if (null != crclClientJInternalFrame) {
	    if (!limits.isEmpty() && isEnforceMinMaxLimits()) {
		PmCartesian maxLimit = new PmCartesian(-100000.0, -100000.0, -100000.0);
		PmCartesian minLimit = new PmCartesian(+100000.0, +100000.0, +100000.0);
		for (PmCartesianMinMaxLimit minMaxLimit : limits) {
		    PmCartesian submaxVis = minMaxLimit.getMax();
		    PmCartesian submaxRobot = convertVisionToRobotPmCartesian(submaxVis);

		    PmCartesian subminVis = minMaxLimit.getMin();
		    PmCartesian subminRobot = convertVisionToRobotPmCartesian(subminVis);
		    maxLimit.x = Math.max(submaxRobot.x, maxLimit.x);
		    maxLimit.y = Math.max(submaxRobot.y, maxLimit.y);
		    maxLimit.z = Math.max(submaxRobot.z, maxLimit.z);
		    maxLimit.x = Math.max(subminRobot.x, maxLimit.x);
		    maxLimit.y = Math.max(subminRobot.y, maxLimit.y);
		    maxLimit.z = Math.max(subminRobot.z, maxLimit.z);

		    minLimit.x = Math.min(submaxRobot.x, minLimit.x);
		    minLimit.y = Math.min(submaxRobot.y, minLimit.y);
		    minLimit.z = Math.min(submaxRobot.z, minLimit.z);
		    minLimit.x = Math.min(subminRobot.x, minLimit.x);
		    minLimit.y = Math.min(subminRobot.y, minLimit.y);
		    minLimit.z = Math.min(subminRobot.z, minLimit.z);
		}
		crclClientJInternalFrame.setMaxLimit(maxLimit);
		crclClientJInternalFrame.setMinLimit(minLimit);
	    } else {
		crclClientJInternalFrame.setMaxLimit(null);
		crclClientJInternalFrame.setMinLimit(null);
	    }
	}
    }

    public XFutureVoid prepGuiCmd() {
	return immediateAbort()
		.thenRun(() -> {
		    clearErrors();
		    reset();
		    resume();
		});
    }

    /**
     * Immediately abort the currently running CRCL program and PDDL action list.
     * <p>
     * NOTE: This may leave the robot in a state with the part held in the gripper
     * or with the robot obstructing the view of the vision system.
     *
     * @return a future object for determining when the abort is completed.
     */
    public XFutureVoid immediateAbort() {
	if (null != this.continuousDemoFuture) {
	    this.continuousDemoFuture.cancelAll(true);
	    this.continuousDemoFuture = null;
	}
	XFutureVoid abort1;
	if (null != executorJInternalFrame1) {
	    abort1 = executorJInternalFrame1.abortProgram();
	} else {
	    abort1 = abortCrclProgram();
	}
	return abort1
		.thenRun(() -> {
		    cancelPauseFutures();
		    if (null != lastResumeFuture) {
			lastResumeFuture.cancelAll(true);
			lastResumeFuture = null;
		    }
		    if (null != lastPauseFuture) {
			lastPauseFuture.cancelAll(true);
			lastPauseFuture = null;
		    }
		    if (null != lastRunProgramFuture) {
			lastRunProgramFuture.cancelAll(true);
			lastRunProgramFuture = null;
		    }
		    if (null != lastStartActionsFuture) {
			lastStartActionsFuture.cancelAll(true);
			lastStartActionsFuture = null;
		    }
		    final XFuture<Boolean> lastContinueActionListFutureFinal = lastContinueActionListFuture;
		    if (null != lastContinueActionListFutureFinal) {
			if (!lastContinueActionListFutureFinal.isDone()) {
			    System.err.println("immediateAbort : cancelling lastContinueActionListFuture="
				    + lastContinueActionListFutureFinal);
			    System.err.println(
				    "continueActionListTrace = " + Utils.traceToString(continueActionListTrace));
			}
			lastContinueActionListFutureFinal.cancelAll(true);
			lastContinueActionListFuture = null;
		    }
		    if (null != disconnectRobotFuture) {
			disconnectRobotFuture.cancelAll(true);
			disconnectRobotFuture = null;
		    }
		    if (null != safeAbortAndDisconnectFuture) {
			safeAbortAndDisconnectFuture.cancelAll(true);
			safeAbortAndDisconnectFuture = null;
		    }
		    if (null != safeAbortFuture) {
			safeAbortFuture.cancelAll(true);
			safeAbortFuture = null;
		    }
		    if (null != lastStartCheckEnabledFuture1) {
			lastStartCheckEnabledFuture1.cancelAll(true);
			lastStartCheckEnabledFuture1 = null;
		    }
		    if (null != lastStartCheckEnabledFuture2) {
			lastStartCheckEnabledFuture2.cancelAll(true);
			lastStartCheckEnabledFuture2 = null;
		    }
		    if (null != lastPrivateStartActionsFuture) {
			lastPrivateStartActionsFuture.cancelAll(false);
			lastPrivateStartActionsFuture = null;
		    }
		    if (null != startLookForPartsFuture) {
			startLookForPartsFuture.cancelAll(false);
			startLookForPartsFuture = null;
		    }
		    if (null != lastPrivateContinueActionListFuture) {
			lastPrivateContinueActionListFuture.cancelAll(false);
			lastPrivateContinueActionListFuture = null;
		    }
		    continousDemoCheckBox.setSelected(false);
		    doingLookForParts = false;
		    runningPrivateContinueActionList = false;
		    runningPrivateStartActions = false;
		});
    }

    private volatile String detailsString = "";

    /**
     * Create a string with current details for display and/or logging.
     *
     * @return details string
     */
    public String getDetailsString() {
	return detailsString;

    }

    private String updateDetailsString() {
	try {
	    if (runProgramServiceThread != null && runProgramServiceThread != Thread.currentThread()) {
		throw new RuntimeException("updateDetailsString called from wrong thread " + Thread.currentThread()
			+ " instead of " + runProgramServiceThread);
	    }
	    StringBuilder sb = new StringBuilder();
	    sb.append(this.toString()).append("\r\n");
	    if (null != crclClientJInternalFrame) {
		CRCLCommandType cmd = crclClientJInternalFrame.currentProgramCommand();
		if (null != cmd) {
		    final String commandSimpleString = CRCLSocket.commandToSimpleString(cmd);
		    sb.append("crcl_cmd=").append(commandSimpleString).append("\r\n");
		} else {
		    sb.append("crcl_cmd= \r\n");
		}
		CRCLProgramType prog = getCrclProgram();
		if (null != prog) {
		    final List<MiddleCommandType> middleCommandsList = CRCLUtils.middleCommands(prog);
		    sb.append("crcl_program_index=(").append(getCrclProgramLine()).append(" / ")
			    .append(middleCommandsList.size()).append("), ");
		    sb.append("crcl_program_name=").append(prog.getName()).append("\r\n");
		} else {
		    sb.append("crcl_program_index=").append(getCrclProgramLine()).append(", ");
		    sb.append("crcl_program_name=\r\n");
		}
		if (null != crclClientJInternalFrame) {
		    crclClientJInternalFrame.getCurrentStatus().ifPresent(status -> {
			CommandStatusType commandStatus = status.getCommandStatus();
			if (null != commandStatus) {
			    String stateDescrition = commandStatus.getStateDescription();
			    if (null != stateDescrition
				    && stateDescrition.length() > 0) {
				sb.append("state description = ").append(stateDescrition).append("\r\n");
			    }
			}
		    });
		}
	    }
	    ExecutorJInternalFrame execFrame = this.executorJInternalFrame1;
	    if (null != execFrame) {
		List<Action> actions = execFrame.getActionsList();
		int curActionIndex = execFrame.getCurrentActionIndex();
		if (null != actions) {
		    sb.append("PDDL curActionIndex= ").append(curActionIndex);
		    sb.append(" out of ").append(actions.size()).append(", ");
		    if (curActionIndex >= 0 && curActionIndex < actions.size()) {
			sb.append("PDDL action =").append(actions.get(curActionIndex)).append("\r\n");
		    } else {
			sb.append("PDDL action = \r\n");
		    }
		}
		String pddlErrorString = execFrame.getErrorString();
		if (null != pddlErrorString && pddlErrorString.length() > 0) {
		    sb.append("pddlExecutorError=").append(pddlErrorString).append("\r\n");
		}
		sb.append("actionSetsCompleted=").append(execFrame.getActionSetsCompleted()).append(", ");
	    }
	    sb.append("robotCrclPort=").append(this.getRobotCrclPort()).append(", ");
	    boolean connected = (null != crclClientJInternalFrame && crclClientJInternalFrame.isConnected());
	    sb.append("connected=").append(connected).append(", ");
	    sb.append("reverseFlag=").append(isReverseFlag()).append(", ");
	    sb.append("paused=").append(isPaused()).append("\r\n");
	    sb.append("run_name=").append(this.getRunName()).append("\r\n");
	    CRCLProgramType crclProgram = this.getCrclProgram();

	    sb.append("crclRunning=").append(this.isRunningCrclProgram()).append(", ");
	    sb.append("isDoingActions=").append(isDoingActions()).append(", ");
	    if (null != crclProgram) {
		sb.append("crclProgramName=").append(crclProgram.getName()).append(", ");
	    }
	    sb.append("isRunning=").append(running.get()).append("\r\n");
	    long runDuration = getRunDuration();
	    long stopDuration = getStopDuration();
	    long totalTime = runDuration + stopDuration;
	    sb.append("runDuration=").append(Utils.runTimeToString(runDuration)).append(", ");
	    sb.append("stopDuration=").append(Utils.runTimeToString(stopDuration)).append(", ");
	    sb.append("totalDuration=").append(Utils.runTimeToString(totalTime)).append("\r\n");
	    String crclClientErrString = getCrclClientErrorString();
	    if (null != crclClientErrString && crclClientErrString.length() > 0
		    && !Objects.equals(titleErrorString, crclClientErrString)) {
		sb.append("crclClientErrString=").append(crclClientErrString).append("\r\n");
		if (!isPaused()) {
		    pause();
		}
	    }
	    if (isVisionToDbConnected() && isObjectViewSimulated()) {
		sb.append("simVisionDiff=").append(simVisionDiff).append(", ");
		sb.append("simVisionTimeDiff=").append(simVisionTimeDiff).append(", ");
		sb.append("maxSimVisionDiff=").append(maxSimVisionDiff).append("\r\n");
	    }
//        sb.append("                                                                                                                                                                                                                                                                                        \r\n");

	    String currentTitleErrorString = this.titleErrorString;
	    if (null != currentTitleErrorString && currentTitleErrorString.length() > 0) {
		sb.append("titleErrorString=").append(currentTitleErrorString).append("\r\n");
	    }
//        sb.append("1111111111222222222233333333334444444444555555555566666666667777777777788888888899999999990000000000111111111122222222223333333333\r\n");
//        sb.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\r\n");
	    String ret = sb.toString().trim();
	    detailsString = ret;
	    return ret;
	} catch (Exception e) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", e);
	    String ret = e.getMessage();
	    if (null == ret) {
		throw e;
	    }
	    detailsString = ret;
	    return ret;
	}
    }

    /**
     * Abort the current CRCL program.
     *
     * @return future object for determining when the abort is complete.
     *
     */
    public XFutureVoid abortCrclProgram() {
	if (null != crclClientJInternalFrame && crclClientJInternalFrame.isConnected()) {
	    return crclClientJInternalFrame.abortProgram();
	} else {
	    return XFutureVoid.completedFuture();
	}
    }

    private @Nullable PrintStream origOut = null;
    private @Nullable PrintStream origErr = null;

    public void setVisible(boolean visible) {
	if (null != aprsSystemDisplayJFrame) {
	    runOnDispatchThread(() -> setVisibleOnDisplay(visible));
	}
    }

    @UIEffect
    private void setVisibleOnDisplay(boolean visible) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setVisible(visible);
	}
    }

    public void setDefaultCloseOperation(int operation) {
	if (null != aprsSystemDisplayJFrame) {
	    runOnDispatchThread(() -> setDefaultCloseOperationOnDisplay(operation));
	}
    }

    @UIEffect
    public void setDefaultCloseOperationOnDisplay(int operation) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setDefaultCloseOperation(operation);
	}
    }

    public List<PhysicalItem> getAvailableToolHolders() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame");
	}
	return executorJInternalFrame1.getAvailableToolHolders();
    }

    public List<PhysicalItem> getToolsInHolders() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame");
	}
	return executorJInternalFrame1.getToolsInHolders();
    }

    @UIEffect
    private void appendLogDisplayOnDisplay(String text) {
	if (null != logDisplayJInternalFrame) {
	    logDisplayJInternalFrame.appendTextOnDisplay(text);
	}
    }

    private final ConcurrentLinkedDeque<String> logDeque = new ConcurrentLinkedDeque<>();

    @UIEffect
    private void consumeLogDeque(ConcurrentLinkedDeque<String> logDeque) {
	String text = logDeque.pollFirst();
	while (null != text) {
	    appendLogDisplayOnDisplay(text);
	    text = logDeque.pollFirst();
	}
    }

    private void appendLogDisplay(String text) {
	if (null != logDisplayJInternalFrame) {
	    logDeque.add(text);
	    submitDisplayConsumer(this::consumeLogDeque, logDeque);
	}
    }

    private int fanucCrclPort = CRCLSocket.DEFAULT_PORT;
    private int motomanCrclPort = CRCLSocket.DEFAULT_PORT;
    private final String fanucNeighborhoodName = "AgilityLabLRMate200iD"; // FIXME hard-coded default
    private final boolean fanucPreferRNN = false;
    private String fanucRobotHost = System.getProperty("fanucRobotHost", "192.168.1.34");// "129.6.78.111"; // FIXME
											 // hard-coded default

    private final List<ServerJInternalFrameProviderFinderInterface> serverJInternalFrameProviderFinders = new ArrayList<>();

    public void addJInternalFrameFinder(ServerJInternalFrameProviderFinderInterface finder) {
	serverJInternalFrameProviderFinders.add(finder);
    }

    public void removeJInternalFrameFinder(ServerJInternalFrameProviderFinderInterface finder) {
	serverJInternalFrameProviderFinders.remove(finder);
    }

    public void clearJInternalFrameFinders() {
	serverJInternalFrameProviderFinders.clear();
    }

    public final void refreshJInternalFrameFinders() {

	try {
	    clearJInternalFrameFinders();
//        try {
//            ClassLoader cl = Thread.currentThread().getContextClassLoader();
//            System.out.println("cl = " + cl);
//            Class clzz = cl.loadClass("com.github.wshackle.atinetft_proxy.ATIForceTorqueSensorFinder");
//            System.out.println("clzz = " + clzz);
//            ProtectionDomain protDom = clzz.getProtectionDomain();
//            System.out.println("protDom = " + protDom);
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(CRCLServerSocket.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        try {
//            Class clzz = Class.forName("com.github.wshackle.atinetft_proxy.ATIForceTorqueSensorFinder");
//            System.out.println("clzz = " + clzz);
//            ProtectionDomain protDom = clzz.getProtectionDomain();
//            System.out.println("protDom = " + protDom);
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(CRCLServerSocket.class.getName()).log(Level.SEVERE, null, ex);
//        }

	    ServiceLoader<ServerJInternalFrameProviderFinderInterface> loader = ServiceLoader
		    .load(ServerJInternalFrameProviderFinderInterface.class);

	    Iterator<ServerJInternalFrameProviderFinderInterface> it = loader.iterator();
//        System.out.println("it = " + it);
	    while (it.hasNext()) {
		ServerJInternalFrameProviderFinderInterface finder = it.next();
//            System.out.println("finder = " + finder);
		addJInternalFrameFinder(finder);
	    }
	} catch (Throwable e) {
	    String msg = e.getMessage();
	    String classpathStr = System.getProperty("java.class.path");
	    msg = msg + "\nclasspath=" + classpathStr;
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
//            System.out.println("cl = " + cl);
//            Class clzz = cl.loadClass("com.github.wshackle.atinetft_proxy.ATIForceTorqueSensorFinder");
//            System.out.println("clzz = " + clzz);
//            ProtectionDomain protDom = clzz.getProtectionDomain();
//            System.out.println("protDom = " + protDom);
	    if (cl instanceof URLClassLoader) {
		@SuppressWarnings("resource")
		URLClassLoader ucl = (URLClassLoader) cl;
		URL[] urls = ucl.getURLs();
		StringBuilder msgBuilder = new StringBuilder(msg);
		for (int i = 0; i < urls.length; i++) {
		    URL url = urls[i];
		    msgBuilder.append("\nurl[").append(i).append("]=").append(urls[i]);
		}
		msg = msgBuilder.toString();
	    }
	    ClassLoader sysCl = ClassLoader.getSystemClassLoader();
	    if (sysCl != cl) {
		if (sysCl instanceof URLClassLoader) {
		    @SuppressWarnings("resource")
		    URLClassLoader ucl = (URLClassLoader) sysCl;
		    URL[] urls = ucl.getURLs();
		    StringBuilder msgBuilder = new StringBuilder(msg);
		    for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			msgBuilder.append("\nurl[").append(i).append("]=").append(urls[i]);
		    }
		    msg = msgBuilder.toString();
		}
	    }
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, msg, e);
	    throw new RuntimeException(msg, e);
	}
//        System.out.println("this.sensorFinders = " + this.sensorFinders);

    }

    public @Nullable ServerJInternalFrameProviderInterface getServerProvider(String name, Object... args) {
	for (ServerJInternalFrameProviderFinderInterface finder : serverJInternalFrameProviderFinders) {
	    ServerJInternalFrameProviderInterface provider = finder.findJInternalFrameProvider(name, args);
	    if (null != provider) {
		return provider;
	    }
	}
	return null;
    }

    public XFutureVoid startFanucCrclServer() {
	try {
	    return runOnDispatchThread(() -> {
		fanucServerProvider = getServerProvider("FanucCRCLServer");
		if (null != fanucServerProvider) {
		    fanucServerProvider.start(fanucPreferRNN, fanucNeighborhoodName, fanucRobotHost, fanucCrclPort);
		    fanucCRCLServerJInternalFrame = fanucServerProvider.getJInternalFrame();
		    addInternalFrame(fanucCRCLServerJInternalFrame);
		} else {
		    System.out.println("");
		    System.out.flush();
		    System.err.println("");
		    System.err.flush();
		    System.err.println("startFanucCrclServer: serverJInternalFrameProviderFinders = "
			    + serverJInternalFrameProviderFinders);
		    final String[] classpaths = System.getProperty("java.class.path")
			    .split(System.getProperty("path.separator"));
		    System.err.println("startFanucCrclServer: classpaths = " + Arrays.toString(classpaths));
		    for (int i = 0; i < classpaths.length; i++) {
			String classpath = classpaths[i];
			System.err.println("   \tstartFanucCrclServer: classpath[" + i + " of " + classpaths.length
				+ "] = " + classpath);
		    }
		    System.out.println("");
		    System.out.flush();
		    System.err.println("");
		    System.err.flush();
		    try {
			Class<?> clzz = Class.forName(
				"com.github.wshackle.fanuccrclservermain.FanucCRCLServerJInternalFrameProviderFinder");
			System.out.println("clzz = " + clzz);
			ProtectionDomain protDom = clzz.getProtectionDomain();
			System.out.println("protDom = " + protDom);
		    } catch (ClassNotFoundException ex) {
			Logger.getLogger(CRCLServerSocket.class
				.getName()).log(Level.SEVERE, null, ex);
		    }
		    // cd C:\Users\shackle\Documents\NetBeansProjects\aprs-framework;
		    // JAVA_HOME=C:\\Users\\Public\\Documents\\Downloaded_Tools\\AdoptOpenJDK\\jdk-11.0.2+7
		    // cmd /c
		    // "\"C:\\Users\\shackle\\Documents\\NetBeans-dev-dev-8a79dae337616d662b21f63284e03ec9954355ed-release\\netbeans\\java\\maven\\bin\\mvn.cmd\"
		    // -Dproject.basedir=C:\\Users\\shackle\\Documents\\NetBeansProjects\\aprs-framework
		    // -Dexec.args=\"-XX:+HeapDumpOnOutOfMemoryError
		    // -XX:HeapDumpPath=aprs_heapdump.bin
		    // -Dlinux.crcl.user.home=linux_netbeans_run_user_home
		    // -Dlinux.aprs.user.home=linux_netbeans_run_user_home
		    // -Dcrcl.user.home=C:\\Users\\shackle\\Documents\\NetBeansProjects\\aprs-framework\\netbeans_run_user_home
		    // -Daprs.user.home=C:\\Users\\shackle\\Documents\\NetBeansProjects\\aprs-framework\\netbeans_run_user_home
		    // -Duser.home=C:\\Users\\shackle\\Documents\\NetBeansProjects\\aprs-framework\\netbeans_run_user_home
		    // -Dcrcl.schemaChangeTime=-1 -Dcrcl.resourceChangeTime=-1 -classpath %classpath
		    // aprs.launcher.LauncherAprsJFrame\"
		    // -Dexec.executable=C:\\Users\\Public\\Documents\\Downloaded_Tools\\AdoptOpenJDK\\jdk-11.0.2+7\\bin\\java.exe
		    // -Dexec.workingdir=
		    // -Dmaven.ext.class.path=C:\\Users\\shackle\\Documents\\NetBeans-dev-dev-8a79dae337616d662b21f63284e03ec9954355ed-release\\netbeans\\java\\maven-nblib\\netbeans-eventspy.jar;C:\\Users\\shackle\\Documents\\NetBeans-dev-dev-8a79dae337616d662b21f63284e03ec9954355ed-release\\netbeans\\java\\maven-nblib\\netbeans-cos.jar
		    // -Dfile.encoding=UTF-8 -Padd_run_deps
		    // org.codehaus.mojo:exec-maven-plugin:1.2.1:exec"
		    final String errMsg = "no fanucServerProvider (Please add  -Padd_run_deps  to mvn command line or fanucCRCLServer classes/jar to classpath)";
		    setTitleErrorString(errMsg);
		    throw new RuntimeException(errMsg);
		}
	    });
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString(ex.getMessage());
	    throw new RuntimeException(ex);
	}
    }

    private @Nullable String titleErrorString = null;

    /**
     * Get the title error string, which should be a short string identifying the
     * most critical problem if there is one appropriate for displaying in the
     * title.
     *
     * @return title error string
     */
    public @Nullable String getTitleErrorString() {
	return titleErrorString;
    }

    private volatile @Nullable CommandStatusType titleErrorStringCommandStatus = null;

    private volatile @Nullable String lastNewTitleErrorString = null;

    private volatile StackTraceElement setTitleErrorStringTrace @Nullable [] = null;

    /**
     * Get the stack trace the last time the title error was set.
     *
     * @return stack trace or null if no error has been set.
     */
    public StackTraceElement @Nullable [] getSetTitleErrorStringTrace() {
	return setTitleErrorStringTrace;
    }

    private final AtomicInteger setTitleErrorStringCount = new AtomicInteger();
    private static volatile long lastForceShowErrorTime = -1;

    /**
     * Set the title error string, which should be a short string identifying the
     * most critical problem if there is one appropriate for displaying in the
     * title.
     *
     * @param newTitleErrorString title error string
     */
    public void setTitleErrorString(@Nullable String newTitleErrorString) {

	try {
	    if (!Objects.equals(lastNewTitleErrorString, newTitleErrorString)) {
		if (!Objects.equals(this.titleErrorString, newTitleErrorString)) {
		    int count = setTitleErrorStringCount.incrementAndGet();
		    System.err.println("setTitleErrorStringCount=" + count);
		    String details;
		    if (Thread.currentThread() == runProgramServiceThread) {
			details = updateDetailsString();
		    } else {
			details = getDetailsString();
		    }
		    System.err.println("details=" + details);
		    if (null != newTitleErrorString) {
			logEvent("setTitleErrorString", newTitleErrorString, count);
		    } else {
			logEvent("setTitleErrorStringNull", count);
		    }
		    if (count < 2 || System.currentTimeMillis() - lastForceShowErrorTime > 5000) {
			if (System.currentTimeMillis() - lastForceShowErrorTime > 5000) {
			    MultiLineStringJPanel.closeAllPanels();
			}
			lastForceShowErrorTime = System.currentTimeMillis();
			if (null != aprsSystemDisplayJFrame) {
			    MultiLineStringJPanel.forceShowText(
				    newTitleErrorString + "\n\n" + "count=" + count + "\n\ndetails=" + details,
				    aprsSystemDisplayJFrame);
			}
			lastForceShowErrorTime = System.currentTimeMillis();
		    }
		    titleErrorStringCommandStatus = getCommandStatus();
		    if (null != this.titleErrorString
			    && null != newTitleErrorString
			    && this.titleErrorString.length() > 0
			    && newTitleErrorString.length() > 0) {
			if (this.titleErrorString.length() > 200) {
			    this.titleErrorString = this.titleErrorString.substring(0, 200) + " ...\n"
				    + newTitleErrorString;
			} else {
			    this.titleErrorString = this.titleErrorString + " ...\n" + newTitleErrorString;
			}
		    } else {
			this.titleErrorString = newTitleErrorString;
		    }
		    submitUpdateTitle();
		    if (null != newTitleErrorString && newTitleErrorString.length() > 0) {
			setTitleErrorStringTrace = Thread.currentThread().getStackTrace();
			boolean snapshotsEnabled = this.isSnapshotsSelected();
			if (!closing) {
			    System.err.println("RunName=" + getRunName());
			    System.err.println(newTitleErrorString);
			    debugDumpStack();
			    if (Utils.arePlayAlertsEnabled()) {
				runOnDispatchThread(Utils::PlayAlert2);
			    }
			    if (!snapshotsEnabled) {
				snapshotsCheckBox.setSelected(true);
			    }
			    takeSnapshots("setTitleError_" + newTitleErrorString + "_");
			}
			pause();
		    }
		}
		lastNewTitleErrorString = newTitleErrorString;
	    }
	    this.asString = getTitle();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    public XFutureVoid startMotomanCrclServer() {
	if (noMotomanServerProviderErrorOccured) {
	    throw new RuntimeException("noMotomanServerProviderErrorOccured");
	}
	return runOnDispatchThread(this::startMotomanCrclServerOnDisplay);
    }

    private volatile boolean noMotomanServerProviderErrorOccured = false;

    @UIEffect
    private void startMotomanCrclServerOnDisplay() {
	try {
	    if (noMotomanServerProviderErrorOccured) {
		return;
	    }
	    if (null == motomanCrclServerJInternalFrame) {
		motomanServerProvider = getServerProvider("MotomanCRCLServer");
		if (null != motomanServerProvider) {
		    JInternalFrame newMotomanCrclServerJInternalFrame = motomanServerProvider.getJInternalFrame();
		    this.motomanCrclServerJInternalFrame = newMotomanCrclServerJInternalFrame;
		    motomanServerProvider.setPropertiesFile(motomanPropertiesFile());
		    motomanServerProvider.loadProperties();
		    motomanServerProvider.start();
		    newMotomanCrclServerJInternalFrame.setVisible(true);
		} else {
		    noMotomanServerProviderErrorOccured = true;
		    System.out.println("");
		    System.out.flush();
		    System.err.println("");
		    System.err.flush();
		    System.err.println("startMotomanCrclServerOnDisplay: serverJInternalFrameProviderFinders = "
			    + serverJInternalFrameProviderFinders);
		    final String[] classpaths = System.getProperty("java.class.path")
			    .split(System.getProperty("path.separator"));
		    System.err.println("startMotomanCrclServerOnDisplay: classpaths = " + Arrays.toString(classpaths));
		    for (int i = 0; i < classpaths.length; i++) {
			String classpath = classpaths[i];
			System.err.println("   \tstartMotomanCrclServerOnDisplay: classpath[" + i + " of "
				+ classpaths.length + "] = " + classpath);
		    }
		    System.out.println("");
		    System.out.flush();
		    System.err.println("");
		    System.err.flush();
		    final String noClassErrMsg = "no motomanServerProvider (Please add -Padd_run_deps  to mvn command line or crcl4java-motoman classes/jar to classpath)";
		    try {
			Class<?> clzz = Class.forName(
				"com.github.wshackle.crcl4java.motoman.ui.MotomanCRCLServerJInternalFrameProviderFinder");
			System.out.println("clzz = " + clzz);
			ProtectionDomain protDom = clzz.getProtectionDomain();
			System.out.println("protDom = " + protDom);
		    } catch (ClassNotFoundException ex) {
//                        setTitleErrorString(ex.getMessage());
			Logger.getLogger(CRCLServerSocket.class
				.getName()).log(Level.SEVERE, null, ex);
			setTitleErrorString(noClassErrMsg);
			throw new RuntimeException(noClassErrMsg);
		    }
		    setTitleErrorString(noClassErrMsg);
		    throw new RuntimeException(noClassErrMsg);
		}
	    }
	    addInternalFrame(motomanCrclServerJInternalFrame);
	} catch (Exception ex) {
	    if (!noMotomanServerProviderErrorOccured) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		setTitleErrorString("Error starting motoman crcl server:" + ex.getMessage());
	    } else if (null != motomanCrclServerJInternalFrame) {
		motomanCrclServerJInternalFrame.setVisible(true);
		addInternalFrame(motomanCrclServerJInternalFrame);
	    }
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new XFuture.PrintedException(ex);
	    }
	}
    }

    private File motomanPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return motomanPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

    @UIEffect
    private void addInternalFrame(JInternalFrame internalFrame) {
	if (null != aprsSystemDisplayJFrame) {
	    internalFrame.pack();
	    internalFrame.setVisible(true);
	    addToDesktopPane(internalFrame);
	    maximizeJInteralFrame(internalFrame);
	}
    }

    @UIEffect
    private void addToDesktopPane(JInternalFrame internalFrame) {
	try {
	    if (null != aprsSystemDisplayJFrame) {
		aprsSystemDisplayJFrame.addToDesktopPane(internalFrame);
	    }
	} catch (Exception e) {
	    println("Thread.currentThread() = " + Thread.currentThread());
	    println("internalFrame = " + internalFrame);
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", e);
	    throw new RuntimeException(e);
	}
    }

    final static private AtomicInteger RUN_PROGRAM_THREAD_COUNT = new AtomicInteger();

    private final int myThreadId = RUN_PROGRAM_THREAD_COUNT.incrementAndGet();

    private volatile @Nullable CSVPrinter eventLogPrinter = null;

    private volatile long lastTime = -1;
    private final AtomicInteger logNumber = new AtomicInteger();

    private volatile @Nullable String lastLogEvent = null;

    public long logEvent(String s, Object... args) {
	if (closing) {
	    return -1;
	}
	return logEvent(s, Arrays.toString(args));
    }

    private long firstlogTime = -1;

    @SuppressWarnings("nullness")
    public long logEvent(String s, @Nullable Object arg) {
	try {
	    if (closing) {
		return -1;
	    }
	    if (null == taskName || taskName.equals(NO_TASK_NAME)) {
		return -1;
	    }
	    if (null == executorJInternalFrame1) {
		return -1;
	    }
	    String prevLastLogEvent = lastLogEvent;
	    println("logEvent(" + s + "," + arg + ")");
	    lastLogEvent = s;
	    CSVPrinter printer = eventLogPrinter;
	    if (null == printer) {
		printer = new CSVPrinter(new FileWriter(Utils.createTempFile(getRunName() + "_events_log_", ".csv")),
			CSVFormat.DEFAULT.withHeader("number", "time", "timediff", "runProgamCount", "abortCount",
				"runProgramTime", "timeSinceLogStart", "runDuration", "stopDuration", "lastLogEvent",
				"event", "arg", "thread", "runName"));
		eventLogPrinter = printer;
	    }
	    long curTime = System.currentTimeMillis();
	    long diff = lastTime > 0 ? curTime - lastTime : -1;
	    lastTime = curTime;
	    String argString = "";
	    if (null != arg) {
		argString = arg.toString().trim();
	    }
	    String argStringSplit[] = argString.split("\n");
	    synchronized (printer) {
		final int thisLogNumber = logNumber.incrementAndGet();
		if (thisLogNumber < 2) {
		    firstlogTime = curTime;
		}
		int currentRunProgramCount = this.runProgramCount.get();
		int currentAbortCount = this.getSafeAbortRequestCount();
		long totalRunProgramTime = runProgramTime.get();
		long timeSinceLogStart = curTime - firstlogTime;
		String diffString = String.format("%07d", diff);
		if (argStringSplit.length < 2) {
		    printer.printRecord(thisLogNumber, curTime, diffString, currentRunProgramCount, currentAbortCount,
			    totalRunProgramTime, timeSinceLogStart, getRunDuration(), getStopDuration(),
			    prevLastLogEvent, s, argString.trim(), Thread.currentThread(), getRunName());
		} else {
		    printer.printRecord(thisLogNumber, curTime, diffString, currentRunProgramCount, currentAbortCount,
			    totalRunProgramTime, timeSinceLogStart, getRunDuration(), getStopDuration(),
			    prevLastLogEvent, s, argStringSplit[0].trim(), Thread.currentThread(), getRunName());
		    for (int i = 1; i < argStringSplit.length; i++) {
			printer.printRecord("", "", "", "", "", "", "", "", "", "", "", argStringSplit[i].trim(), "",
				"");
		    }
		}
	    }
	    return curTime;
	} catch (IOException ex) {
	    if (closing) {
		return -1;
	    }
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    return -1;
	}
    }

//    private String programToString(CRCLProgramType prog) {
//        StringBuilder sb = new StringBuilder();
//        List<MiddleCommandType> midCmds
//                = CRCLUtils.middleCommands(prog);
//        for (int i = 0; i < midCmds.size(); i++) {
//            sb.append(String.format("%03d", i));
//            sb.append(" \t");
//            sb.append(CRCLSocket.commandToSimpleString(midCmds.get(i)));
//            sb.append("\r\n");
//        }
//        return sb.toString();
//    }

    /**
     * Get the unique thread id number for this system. It is set only when this
     * object is created.
     *
     * @return thread id
     */
    public int getMyThreadId() {
	return myThreadId;
    }

    private String getThreadName() {
	return "AprsJframe." + myThreadId + "." + getRunName();
    }

    @SuppressWarnings("guieffect")
    private void setThreadName() {
	if (!SwingUtilities.isEventDispatchThread()) {
	    Thread.currentThread().setName(getThreadName());
	}
    }

    private volatile @Nullable Thread runProgramServiceThread = null;

    private final ExecutorService runProgramService = Executors.newSingleThreadExecutor(new ThreadFactory() {

	@Override
	public Thread newThread(Runnable r) {
	    Thread thread = new Thread(r, getThreadName());
	    runProgramServiceThread = thread;
	    thread.setDaemon(true);
	    return thread;
	}
    });

    private void updateRunProgramServiceThread() {
	if (Thread.currentThread() != runProgramServiceThread) {
	    try {
		runProgramService.submit(() -> runProgramServiceThread = Thread.currentThread())
			.get(1000, TimeUnit.MILLISECONDS);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
	    }
	}
    }

    private @Nullable XFutureVoid connectDatabaseFuture = null;

    /**
     * Get the ExecutorService used for running CRCL programs.
     *
     * @return run program service
     */
    public ExecutorService getRunProgramService() {
	return runProgramService;
    }

    public XFuture<Boolean> startConnectDatabase() {
	if (!this.useCsvFilesInsteadOfDatabase && null == dbSetupJInternalFrame) {
	    throw new IllegalStateException("DB Setup View must be open to use this function.");
	}
	XFuture<Boolean> f = waitDbConnected();
	if (closing) {
	    throw new IllegalStateException("Attempt to start connect database when already closing.");
	}
	final File dbSetupPropertiesFile = (dbSetupJInternalFrame != null) ? dbSetupJInternalFrame.getPropertiesFile()
		: null;
	println("Starting connect to database ...   : propertiesFile=" + dbSetupPropertiesFile);
	DbSetupPublisher dbSetupPublisher;
	if (!this.useCsvFilesInsteadOfDatabase && null != dbSetupJInternalFrame) {
	    dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
	} else {
	    try {
		dbSetupPublisher = this.dbSetupPublisherSupplier.call();
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE,
				"startConnectDatabase failed for " + getTaskName(), ex);
		if (ex instanceof RuntimeException) {
		    throw ((RuntimeException) ex);
		} else {
		    throw new RuntimeException(ex);
		}
	    }
	}
	DbSetup setup = dbSetupPublisher.getDbSetup();
	connectDatabaseFuture = connectDatabase();
	// runProgramService.submit(this::connectDatabase);
	return f;
    }

    private XFutureVoid connectDatabase() {
	List<XFutureVoid> futures = null;
	try {
//            if (!isConnectDatabaseCheckBoxSelected()) {
//                XFutureVoid xf = new XFutureVoid("connectDatabase!isConnectDatabaseCheckBoxSelected()");
//                xf.cancelAll(false);
//                return xf;
//            }

	    DbSetupPublisher dbSetupPublisher;
	    if (null != dbSetupJInternalFrame) {
		dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
		String startScript = dbSetupJInternalFrame.getStartScript();
		if (null != startScript && startScript.length() > 0) {
		    println();
		    System.err.println();
		    System.out.flush();
		    System.err.flush();
		    println("Excecuting Database startScript=\r\n\"" + startScript + "\"");
		    println();
		    System.err.println();
		    System.out.flush();
		    System.err.flush();
		    ProcessBuilder pb = new ProcessBuilder(startScript.split("[ ]+"));
		    pb.inheritIO().start().waitFor();
		    println();
		    System.err.println();
		    System.out.flush();
		    System.err.flush();
		}
	    } else {
		dbSetupPublisher = this.dbSetupPublisherSupplier.call();
	    }

	    DbSetup setup = dbSetupPublisher.getDbSetup();
	    if (setup.getDbType() == null || setup.getDbType() == DbType.NONE) {
		throw new IllegalStateException(
			"Can not connect to database with setup.getDbType() = " + setup.getDbType());
	    }
	    if (setup.getQueriesMap().isEmpty()) {
		throw new IllegalStateException("Can not connect to database with getQueriesMap().isEmpty()");
	    }
	    dbSetupPublisher
		    .setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
	    futures = dbSetupPublisher.notifyAllDbSetupListeners(null);
	    XFutureVoid futuresArray[] = futures.toArray(new XFutureVoid[0]);
	    XFutureVoid xfAll = XFutureVoid.allOf(futuresArray);
	    return xfAll.thenRun(() -> {
		setConnectDatabaseCheckBoxSelected(true);
		setConnectDatabaseCheckBoxEnabled(true);
		println("Finished connect to database.");
	    });
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    if (null != futures) {
		for (Future<?> f : futures) {
		    f.cancel(true);
		}
	    }
	    throw new RuntimeException(ex);
	}
    }

    public @Nullable File getPropertiesDirectory() {
	return propertiesDirectory;
    }

    private void setConnectDatabaseCheckBoxEnabled(boolean enable) {
	connectDatabaseCheckBox.setEnabled(enable);
    }

    public void stopSimUpdateTimer() {
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.stopSimUpdateTimer();
	}
    }

    private void setConnectDatabaseCheckBoxSelected(boolean selected) {
	connectDatabaseCheckBox.setSelected(selected);
    }

    private volatile int visionLineCount = 0;
    private volatile int simVisionDiff = 0;
    private volatile int maxSimVisionDiff = -99;
    private volatile int simLineCount = 0;
    private volatile long simVisionTimeDiff = -1;

    public int getVisionLineCount() {
	return visionLineCount;
    }

    public int getSimLineCount() {
	return simLineCount;
    }

    public long getSimVisionTimeDiff() {
	return simVisionTimeDiff;
    }

    synchronized private void visionLineCounter(int count) {
	this.visionLineCount = count;
	simVisionDiff = simLineCount - count;
	if (simVisionDiff >= 0) {
	    while (publishTimes.size() > simVisionDiff + 1 && !publishTimes.isEmpty()) {
		publishTimes.remove(0);
	    }
	    if (publishTimes.size() > 0) {
		long pubTime = publishTimes.remove(0);
		long visTime = System.currentTimeMillis();
		simVisionTimeDiff = visTime - pubTime;
	    }
	    if (maxSimVisionDiff < simVisionDiff) {
		maxSimVisionDiff = simVisionDiff;
		println("maxSimVisionDiff = " + maxSimVisionDiff);
	    }
	} else {
	    simVisionTimeDiff = -1;
	}

    }

    private final Consumer<Integer> visionLineListener;

    public XFutureVoid connectVision() {
	if (closing) {
	    throw new IllegalStateException("Attempt to start connect vision when already closing.");
	}
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("null == visionToDbJInternalFrame");
	}
	return Utils.supplyOnDispatchThread(visionToDbJInternalFrame::connectVision)
		.thenComposeToVoid(x -> x)
		.thenRun(() -> {
		    final Object2DViewJInternalFrame object2DViewJInternalFrameLocal = object2DViewJInternalFrame;
		    if (null != object2DViewJInternalFrameLocal) {
			object2DViewJInternalFrameLocal.addPublishCountListener(simPublishCountListener);
		    }
		    final VisionToDbJInternalFrame visionToDbJInternalFrameLocal = visionToDbJInternalFrame;
		    if (null != visionToDbJInternalFrameLocal) {
			visionToDbJInternalFrameLocal.addLineCountListener(visionLineListener);
		    }
		});
    }

    public void disconnectVision() {
	if (null != visionToDbJInternalFrame) {
	    runOnDispatchThread(visionToDbJInternalFrame::disconnectVision);
	}
    }

    private volatile String title = "";

    @UIEffect
    private void newTitleConsumer(String newTitle) {
	setTitleOnDisplay(newTitle);
    }

    @SuppressWarnings("UnusedReturnValue")
    private XFutureVoid setTitle(String newTitle) {
	this.title = newTitle;
	if (null != aprsSystemDisplayJFrame) {
	    return submitDisplayConsumer(this::newTitleConsumer, newTitle);
	} else {
	    return XFutureVoid.completedFuture();
	}
    }

    @UIEffect
    private void setTitleOnDisplay(String newTitle) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setTitle(newTitle);
	}
    }

    private String getTitle() {
	return title;
    }

    /**
     * Initialize the frame with the previously saved settings if available.
     */
    private XFutureVoid defaultInit() {
	return initLoggerWindow()
		.thenComposeToVoid(this::commonInit);
    }

    private void clearStartCheckBoxes() {
	onStartupConnectDatabaseCheckBox.setSelected(false);
	connectDatabaseCheckBox.setSelected(false);
//        if (null != aprsSystemDisplayJFrame) {
//            runOnDispatchThread(this::clearStartCheckBoxesOnDisplay);
//        }
    }

//    @UIEffect
//    private void clearStartCheckBoxesOnDisplay() {
//        if (null != aprsSystemDisplayJFrame) {
//            aprsSystemDisplayJFrame.clearStartCheckBoxes();
//        }
//    }
    /**
     * Initialize the frame ignoring any previously saved settings.
     */
    private XFutureVoid emptyInit() {
	skipCreateDbSetupFrame = true;
	clearStartCheckBoxes();
	return initLoggerWindow()
		.thenComposeToVoid(this::commonInit);
    }

    private void headlessEmptyInit() {
	skipCreateDbSetupFrame = true;
	clearStartCheckBoxes();
	headlessEmptyCommonInit();
    }

    @SuppressWarnings("guieffect")
    private static boolean isHeadless() {
	return GraphicsEnvironment.isHeadless();
    }

    public static XFuture<AprsSystem> createSystem(File propertiesFile) {
	if (isHeadless()) {
	    try {
		return createAprsSystemHeadless(propertiesFile);
	    } catch (IOException e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	    }
	} else {
	    return createAprsSystemWithSwingDisplay(propertiesFile);
	}
    }

    public static XFuture<AprsSystem> createPrevSystem() throws IOException {
	if (isHeadless()) {
	    return createPrevAprsSystemHeadless();
	} else {
	    return createPrevAprsSystemWithSwingDisplay();
	}
    }

    public static XFuture<AprsSystem> createEmptySystem() {
	if (isHeadless()) {
	    return createEmptyAprsSystemHeadless();
	} else {
	    return createEmptyAprsSystemWithSwingDisplay();
	}
    }

    private static XFuture<AprsSystem> createEmptyAprsSystemWithSwingDisplay() {
	return Utils.supplyOnDispatchThread(AprsSystem::createEmptyAprsSystemWithSwingDisplay2)
		.thenCompose(x -> x);
    }

    private static XFuture<AprsSystem> createPrevAprsSystemWithSwingDisplay() {
	return Utils.supplyOnDispatchThread(AprsSystem::createPrevAprsSystemWithSwingDisplay2)
		.thenCompose(x -> x);
    }

    private static XFuture<AprsSystem> createAprsSystemWithSwingDisplay(File propertiesFile) {
	return Utils.supplyOnDispatchThread(() -> createAprsSystemWithSwingDisplay2(propertiesFile))
		.thenCompose(x -> x);
    }

    private static XFuture<AprsSystem> createEmptyAprsSystemWithSwingDisplay2() {
	try {
	    AprsSystemPropDefaults emptyTemp = AprsSystemPropDefaults.getEmptyTemp();
	    AprsSystemDisplayJFrame aprsSystemDisplayJFrame1 = new AprsSystemDisplayJFrame();
	    AprsSystem system = new AprsSystem(aprsSystemDisplayJFrame1, emptyTemp);
	    aprsSystemDisplayJFrame1.setAprsSystem(system);
	    system.setVisible(true);
	    return system.emptyInit().thenSupply(() -> {
		aprsSystemDisplayJFrame1.setAprsSystem(system);
		return system;
	    });
	} catch (Exception exception) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", exception);
	    if (exception instanceof RuntimeException) {
		throw (RuntimeException) exception;
	    } else {
		throw new RuntimeException(exception);
	    }
	}
    }

    private static XFuture<AprsSystem> createPrevAprsSystemWithSwingDisplay2() {
	AprsSystemDisplayJFrame aprsSystemDisplayJFrame1 = new AprsSystemDisplayJFrame();
	AprsSystem system = new AprsSystem(aprsSystemDisplayJFrame1,
		AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS());
	try {
	    return system
		    .loadProperties()
		    .thenComposeToVoid(() -> system.defaultInit())
		    .thenApply(x -> {
			aprsSystemDisplayJFrame1.setAprsSystem(system);
			return system;
		    });
	} catch (IOException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	}
    }

    private static XFuture<AprsSystem> createAprsSystemWithSwingDisplay2(File propertiesFile) {
	try {
	    AprsSystemDisplayJFrame aprsSystemDisplayJFrame1 = new AprsSystemDisplayJFrame();
	    AprsSystem system = new AprsSystem(aprsSystemDisplayJFrame1,
		    AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS());
	    aprsSystemDisplayJFrame1.setAprsSystem(system);
	    system.setVisible(true);
	    if (null != propertiesFile) {
		system.setPropertiesFile(propertiesFile);
		return system.loadProperties()
			.thenComposeToVoid(() -> system.defaultInit())
			.thenApply(x -> {
			    aprsSystemDisplayJFrame1.setAprsSystem(system);
			    return system;
			});
	    }
	    return system.defaultInit().thenApply(x -> {
		aprsSystemDisplayJFrame1.setAprsSystem(system);
		return system;
	    });
	} catch (Throwable e) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", e);
	    MultiLineStringJPanel.showException(e);
	    throw new RuntimeException(e);
	}
    }

    private static XFuture<AprsSystem> createEmptyAprsSystemHeadless() {
	AprsSystem system = new AprsSystem();
	return system.emptyInit().thenApply(x -> {
	    return system;
	});
    }

    public static AprsSystem emptyHeadlessAprsSystem() {
	return new AprsSystem(true);
    }

    private static XFuture<AprsSystem> createPrevAprsSystemHeadless() throws IOException {
	AprsSystem system = new AprsSystem();
	return system.loadProperties()
		.thenComposeToVoid(() -> system.defaultInit())
		.thenApply(x -> {
		    return system;
		});
    }

    private static XFuture<AprsSystem> createAprsSystemHeadless(File propertiesFile) throws IOException {
	AprsSystem system = new AprsSystem(null, AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS());
	if (null != propertiesFile) {
	    system.setPropertiesFile(propertiesFile);
	    return system.loadProperties()
		    .thenComposeToVoid(() -> system.defaultInit())
		    .thenSupply(() -> {
			return system;
		    });
	}
	return system.defaultInit()
		.thenSupply(() -> {
		    return system;
		});
    }

    private boolean skipCreateDbSetupFrame = false;

    private final AtomicInteger commonInitCount = new AtomicInteger();

    private XFutureVoid commonInit() {
	int currentCommonInitCout = commonInitCount.incrementAndGet();
	if (debug) {
	    println("commonInitCount = " + currentCommonInitCout);
	}
	return startWindowsFromMenuCheckBoxes()
		.thenRun(this::completeCommonInit)
		.peekNoCancelException((Throwable t) -> {
		    Utils.runOnDispatchThread(() -> {
			setupWindowsMenuOnDisplay();
		    });
		});
    }

    private void headlessEmptyCommonInit() {
	int currentCommonInitCout = commonInitCount.incrementAndGet();
	if (debug) {
	    println("commonInitCount = " + currentCommonInitCout);
	}
	this.completeCommonInit();
    }

    private final AtomicInteger completeCommonInitCount = new AtomicInteger();

    @UIEffect
    private void setIconImageURL(URL url) {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		setIconImage(ImageIO.read(url));
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
	    }
	}
    }

    @UIEffect
    private void completeCommonInitOnDisplay() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		setIconImage(IconImages.BASE_IMAGE);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
	    }
	    aprsSystemDisplayJFrame.hideAllInternalFrames();
	    showActiveWinOnDisplay();
	    setupWindowsMenuOnDisplay();
	}
    }

    private void completeCommonInit() {
	if (null != aprsSystemDisplayJFrame) {
	    runOnDispatchThread(this::completeCommonInitOnDisplay);
	}
	submitUpdateTitle();
	this.asString = getTitle();
    }

    @UIEffect
    private void setIconImage(Image image) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setIconImage(image);
	}
    }

    private boolean isKitInspectionStartupSelected() {
	return kitInspectionStartupCheckBox.isSelected();
    }

    private void setKitInspectionStartupSelected(boolean selected) {
	kitInspectionStartupCheckBox.setSelected(selected);
    }

    private boolean isPddlPlannerStartupSelected() {
	return false; // pddlPlannerStartupCheckBox.isSelected();
    }

    private boolean isExecutorStartupSelected() {
	return executorStartupCheckBox.isSelected();
    }

    private void setExecutorStartupSelected(boolean selected) {
	executorStartupCheckBox.setSelected(selected);
    }

    private boolean isObjectSpStartupSelected() {
	return objectSpStartupCheckBox.isSelected();
    }

    private void setObjectSpStartupSelected(boolean selected) {
	objectSpStartupCheckBox.setSelected(selected);
    }

    private boolean isObject2DViewStartupSelected() {
	return object2DViewStartupCheckBox.isSelected();
    }

    private void setObject2DViewStartupSelected(boolean selected) {
	object2DViewStartupCheckBox.setSelected(selected);
    }

    private boolean isRobotCrclGUIStartupSelected() {
	return robotCrclGUIStartupCheckBox.isSelected();
    }

    private boolean isForceTorqueSimStartupSelected() {
	return forceTorqueSimStartupCheckBox.isSelected();
    }

    private void setRobotCrclGUIStartupSelected(boolean selected) {
	robotCrclGUIStartupCheckBox.setSelected(selected);
    }

    private void setForceTorqueSimStartupSelected(boolean selected) {
	forceTorqueSimStartupCheckBox.setSelected(selected);
    }

    private boolean isRobotCrclSimServerStartupSelected() {
	return robotCrclSimServerStartupCheckBox.isSelected();
    }

    private void setRobotCrclSimServerStartupSelected(boolean selected) {
	robotCrclSimServerStartupCheckBox.setSelected(selected);
    }

    private boolean isRobotCrclFanucServerStartupSelected() {
	return robotCrclFanucServerStartupCheckBox.isSelected();
    }

    private void setRobotCrclFanucServerStartupSelected(boolean selected) {
	robotCrclFanucServerStartupCheckBox.setSelected(selected);
    }

    private boolean isRobotCrclMotomanServerStartupSelected() {
	return robotCrclMotomanServerStartupCheckBox.isSelected();
    }

    private void setRobotCrclMotomanServerStartupSelected(boolean selected) {
	robotCrclMotomanServerStartupCheckBox.setSelected(selected);
    }

    private boolean isExploreGraphDBStartupSelected() {
	return exploreGraphDBStartupCheckBox.isSelected();
    }

    private void setExploreGraphDBStartupSelected(boolean selected) {
	exploreGraphDBStartupCheckBox.setSelected(selected);
    }

    private boolean isShowDatabaseSetupStartupSelected() {
	return showDatabaseSetupOnStartupCheckBox.isSelected();
    }

    private void setShowDatabaseSetupStartupSelected(boolean selected) {
	showDatabaseSetupOnStartupCheckBox.setSelected(selected);

    }

    @UI
    private static interface UISupplier<R> {

	public R get();
    }

    // @SuppressWarnings("guieffect")
//    private static boolean isEventDispatchThread() {
//        return SwingUtilities.isEventDispatchThread();
//    }
//    /**
//     * Call a method that returns a value on the dispatch thread.
//     *
//     * @param <R> type of return of the caller
//     * @param s supplier object with get method to be called.
//     * @return future that will make the return value accessible when the call
//     * is complete.
//     */
//    @SuppressWarnings("guieffect")
//    private static <R> R supplyAndWaitOnDispatchThread(final UISupplier<R> s) {
//
//        try {
//            if (isEventDispatchThread()) {
//                return s.get();
//            } else {
//                AtomicReference<R> ret = new AtomicReference<>();
//                javax.swing.SwingUtilities.invokeAndWait(() -> ret.set(s.get()));
//                return ret.get();
//            }
//        } catch (InterruptedException | InvocationTargetException ex) {
//            Logger.getLogger(AprsSystem.class
//                    .getName()).log(Level.SEVERE, "", ex);
//            throw new RuntimeException(ex);
//        }
//    }
    private boolean isConnectDatabaseOnSetupStartupSelected() {
	return onStartupConnectDatabaseCheckBox.isSelected();
    }

    private void setConnectDatabaseOnStartupSelected(boolean selected) {
	onStartupConnectDatabaseCheckBox.setSelected(selected);
    }

    private boolean isConnectVisionOnStartupSelected() {
	return connectVisionOnStartupCheckBox.isSelected();
    }

    private void setConnectVisionOnStartupSelected(boolean selected) {
	connectVisionOnStartupCheckBox.setSelected(selected);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private XFutureVoid startWindowsFromMenuCheckBoxes() {
	if (null == aprsSystemDisplayJFrame) {
	    return startWindowsFromMenuCheckBoxesInternal();
	} else {
	    return Utils.supplyOnDispatchThread(this::startWindowsFromMenuCheckBoxesInternal)
		    .thenComposeToVoid(x -> x);
	}
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @UIEffect
    private XFutureVoid startWindowsFromMenuCheckBoxesInternal() {
	try {
	    boolean onDispatchThread = SwingUtilities.isEventDispatchThread();
	    if (!onDispatchThread) {
		throw new RuntimeException("Thread.currentThread()=" + Thread.currentThread());
	    }
	    List<XFuture<?>> futures = new ArrayList<>();
	    if (isKitInspectionStartupSelected()) {
		XFutureVoid startKitInspectionFuture = startKitInspection();
		futures.add(startKitInspectionFuture);
	    }
//            if (isPddlPlannerStartupSelected()) {
//                XFutureVoid startPddlPlannerFuture = startPddlPlanner();
//                futures.add(startPddlPlannerFuture);
//            }
	    if (isExecutorStartupSelected()) {
		XFutureVoid startExecutorFuture = startActionListExecutor();
		futures.add(startExecutorFuture);
	    }
	    XFutureVoid startVisionToDbFuture = null;
	    if (isObjectSpStartupSelected()) {
		startVisionToDbFuture = startVisionToDbJinternalFrame();
		futures.add(startVisionToDbFuture);
	    }
	    XFutureVoid object2DViewFuture = null;
	    if (isObject2DViewStartupSelected()) {
		object2DViewFuture = startObject2DJinternalFrame();
		futures.add(object2DViewFuture);
	    }
	    XFutureVoid serverFuture = null;
	    if (isRobotCrclSimServerStartupSelected()) {
		XFutureVoid startSimServerFuture = startSimServerJInternalFrame();
		serverFuture = startSimServerFuture;
		futures.add(startSimServerFuture);
	    }

	    if (isExploreGraphDBStartupSelected()) {
		XFutureVoid startExploreGraphDbFuture = startExploreGraphDb();
		futures.add(startExploreGraphDbFuture);
	    }
	    if (isRobotCrclFanucServerStartupSelected()) {
		XFutureVoid startFanucFuture = startFanucCrclServer();
		serverFuture = startFanucFuture;
		futures.add(startFanucFuture);
	    }
	    if (isRobotCrclMotomanServerStartupSelected() && !noMotomanServerProviderErrorOccured) {
		XFutureVoid startMotomanFuture = startMotomanCrclServer();
		serverFuture = startMotomanFuture;
		futures.add(startMotomanFuture);
	    }
	    if (isRobotCrclGUIStartupSelected()) {
		if (null != serverFuture) {
		    XFutureVoid startCrclClientFuture = serverFuture
			    .thenComposeToVoid(x -> startCrclClientJInternalFrame());
		    futures.add(startCrclClientFuture);
		} else {
		    XFutureVoid startCrclClientFuture = startCrclClientJInternalFrame();
		    futures.add(startCrclClientFuture);
		}
	    }
	    if (isForceTorqueSimStartupSelected()) {
		if (null != serverFuture) {
		    XFutureVoid startForceTorqueSimFuture = serverFuture.thenComposeToVoid(x -> startForceTorqueSim());
		    futures.add(startForceTorqueSimFuture);
		} else {
		    XFutureVoid startForceTorqueSimFuture = startForceTorqueSim();
		    futures.add(startForceTorqueSimFuture);
		}
	    }
	    if (isShowDatabaseSetupStartupSelected() && (!skipCreateDbSetupFrame)) {
		createDbSetupFrame();
	    }
	    if (isShowDatabaseSetupStartupSelected()) {
		if (onDispatchThread) {
		    showDatabaseSetupWindowOnDisplay();
		} else {

		    XFutureVoid setupDatabaseFuture = showDatabaseSetupWindow();
		    futures.add(setupDatabaseFuture);
		}
	    } else {
		setConnectDatabaseOnStartupSelected(false);
	    }
	    updateSubPropertiesFiles();

	    boolean startConnectDatabaseSelected = isConnectDatabaseOnSetupStartupSelected();
	    if (startConnectDatabaseSelected) {
		XFuture<Boolean> startConnectDatabaseFuture = startConnectDatabase();
		futures.add(startConnectDatabaseFuture);
	    }
	    if (isConnectVisionOnStartupSelected() && null != startVisionToDbFuture) {
		if (null != object2DViewFuture) {
		    XFutureVoid connectVisionFuture = XFutureVoid.allOf(object2DViewFuture, startVisionToDbFuture)
			    .thenComposeToVoid(this::connectVision);
		    futures.add(connectVisionFuture);
		} else {
		    XFutureVoid connectVisionFuture = startVisionToDbFuture
			    .thenComposeToVoid(this::connectVision);
		    futures.add(connectVisionFuture);
		}
	    }
	    if (null != customWindowsFile) {
		loadCustomWindowsFile();
	    }
	    if (futures.isEmpty()) {
		return XFutureVoid.completedFutureWithName("startWindowsFromMenuCheckBoxes");
	    } else {
		XFuture<?> futuresArray[] = futures.toArray(new XFuture[0]);
		return XFutureVoid.allOfWithName("startWindowsFromMenuCheckBoxes", futuresArray);
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private void loadCustomWindowsFile() {
	final File fileToLoad = customWindowsFile;
	if (null != fileToLoad) {
	    try (BufferedReader br = new BufferedReader(new FileReader(fileToLoad))) {
		String line = br.readLine();
		while (line != null) {
		    line = line.trim();
		    if (line.length() < 1) {
			line = br.readLine();
			continue;
		    }
		    loadWindowFile(new File(fileToLoad.getParentFile(), line));
		    line = br.readLine();
		}
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "customWindowsFile=" + fileToLoad, ex);
	    }
	}
    }

    private final Map<File, JInternalFrame> customFileWindowMap = new IdentityHashMap<>();

    private void loadWindowFile(File winFile) throws Exception {
	Properties props = new Properties();
	try (BufferedReader br = new BufferedReader(new FileReader(winFile))) {
	    props.load(br);
	}
	String classnameString = props.getProperty("classname");
	if (null == classnameString || classnameString.length() < 1) {
	    throw new RuntimeException("no classname property in " + winFile);
	}
	Class<?> clss = Class.forName(classnameString);
	String constArgsNumberString = props.getProperty("constArgsNumber", "0");
	final Constructor<?> constructor = clss.getConstructor();
	if (null == constructor) {
	    throw new RuntimeException("class doesn't have no args constructor  ,winFile=" + winFile);
	}
	Object newObject = constructor.newInstance();
	AutomaticPropertyFileUtils.loadPropertyFile(winFile, Collections.emptyMap(), newObject);
	if (newObject instanceof JInternalFrame) {
	    final JInternalFrame jinternalFrame = (JInternalFrame) newObject;
	    addInternalFrame(jinternalFrame);
	    customFileWindowMap.put(winFile, jinternalFrame);
	}
    }

    public final XFutureVoid initLoggerWindow() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		return runOnDispatchThread("initLoggerWindowOnDisplay", this::initLoggerWindowOnDisplay);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	}
	return XFutureVoid.completedFuture();
    }

    @SuppressWarnings({ "nullness", "initialization" })
    private final Consumer<String> loggerFrameStringConsumer = (String s) -> appendLogDisplay(s);

    @UIEffect
    private void initLoggerWindowOnDisplay() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		synchronized (this) {
		    if (null == logDisplayJInternalFrame) {
			LogDisplayJInternalFrame logFrame = new LogDisplayJInternalFrame();
			addInternalFrame(logFrame);
			AprsCommonLogger.instance().getStringConsumers()
				.add(loggerFrameStringConsumer);
			AprsCommonLogger.instance().addRef();
			this.logDisplayJInternalFrame = logFrame;
		    }
		}
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
	    }
	}
    }

    private XFutureVoid closeAllInternalFrames() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		return runOnDispatchThread("closeAllInternalFramesOnDisplay", this::closeAllInternalFramesOnDisplay);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	}
	return XFutureVoid.completedFuture();
    }

    @UIEffect
    private void closeAllInternalFramesOnDisplay() {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.closeAllInternalFrames();
	}
    }

    /**
     * Close all internal windows.
     */
    public void closeAllWindows() {
	startingCheckEnabled = false;
	try {
	    closeActionsListExcecutor();

	} catch (Exception exception) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", exception);
	}
	if (null != connectDatabaseFuture) {
	    connectDatabaseFuture.cancel(true);
	    connectDatabaseFuture = null;
	}
	if (null != disconnectDatabaseFuture) {
	    disconnectDatabaseFuture.cancel(true);
	    disconnectDatabaseFuture = null;
	}

	try {
	    immediateAbort();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    disconnectVision();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    setConnectDatabaseCheckBoxEnabled(false);
	    if (null != connectDatabaseFuture) {
		connectDatabaseFuture.cancel(true);
		connectDatabaseFuture = null;
	    }
	    disconnectDatabase();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    abortCrclProgram();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    startingCheckEnabled = false;
	    if (isConnected()) {
		disconnectRobotPrivate();
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    abortCrclProgram();
	    disconnectRobot();
	    disconnectVision();
	    disconnectDatabase();
	    closeDbSetupFrame();

	    disposeInternalFrame(exploreGraphDbJInternalFrame);
	    disposeInternalFrame(logDisplayJInternalFrame);
	    SimServerJInternalFrame simServerFrame = this.simServerJInternalFrame;
	    if (null != simServerFrame) {
		try {
		    simServerFrame.close();
		} catch (Exception e) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, "", e);
		}
	    }
	    closeAllInternalFrames();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	AprsCommonLogger.instance().removeRef();
	AprsCommonLogger.instance().getStringConsumers()
		.remove(loggerFrameStringConsumer);
    }

    private void closeDbSetupFrame() {
	DbSetupJInternalFrame dbSetupFrame = this.dbSetupJInternalFrame;
	if (null != dbSetupFrame) {
	    dbSetupFrame.getDbSetupPublisher().removeAllDbSetupListeners();
	    disposeInternalFrame(dbSetupFrame);
	}
    }

    private void disposeInternalFrame(@Nullable JInternalFrame frame) {
	if (null != frame) {
	    runOnDispatchThread(() -> disposeInternalFrameOnDisplay(frame));
	}
    }

    @UIEffect
    private void disposeInternalFrameOnDisplay(@Nullable JInternalFrame frame) {
	if (null != frame) {
	    frame.setVisible(false);
	    frame.dispose();
	}
    }

    // private void activateInternalFrame(JInternalFrame internalFrame) {
//        try {
//            internalFrame.setVisible(true);
//            if (null != internalFrame.getDesktopPane()
//                    && null != internalFrame.getDesktopPane().getDesktopManager()) {
//                internalFrame.getDesktopPane().getDesktopManager().deiconifyFrame(internalFrame);
//                internalFrame.getDesktopPane().getDesktopManager().activateFrame(internalFrame);
//                maximizeJInteralFrame(internalFrame);
//            }
//            internalFrame.moveToFront();
//        } catch (Exception e) {
//            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "", e);
//        }
//    }
    public XFutureVoid showDatabaseSetupWindow() {
	return runOnDispatchThread(this::showDatabaseSetupWindowOnDisplay);
    }

    @UIEffect
    private void showDatabaseSetupWindowOnDisplay() {
	if (null == propertiesFile) {
	    throw new NullPointerException("propertiesFile");
	}
	createDbSetupFrame();
	DbSetupJInternalFrame dbSetupFrame = this.dbSetupJInternalFrame;
	if (null != dbSetupFrame) {
	    dbSetupFrame.setPropertiesFile(propertiesFile);
	}
    }

    @UIEffect
    private boolean checkInternalFrame(JInternalFrame frm) {
	if (null != aprsSystemDisplayJFrame) {
	    return aprsSystemDisplayJFrame.checkInternalFrame(frm);
	}
	return false;
    }

    public void hideDatabaseSetupWindow() {
	if (null != dbSetupJInternalFrame) {
	    runOnDispatchThread(this::hideDatabaseSetupWindowOnDisplay);
	}
    }

    @UIEffect
    public void hideDatabaseSetupWindowOnDisplay() {
	if (null != dbSetupJInternalFrame) {
	    dbSetupJInternalFrame.setVisible(false);
	    checkIconifyAndDeactivate(dbSetupJInternalFrame);
	}
    }

    @UIEffect
    private void checkIconifyAndDeactivate(JInternalFrame internalFrame) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.checkIconifyAndDeactivate(internalFrame);
	}
    }

    @UIEffect
    private void deiconifyAndActivate(final JInternalFrame frameToShow) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.deiconifyAndActivate(frameToShow);
	}
    }

    @UIEffect
    private void activateFrame(final JInternalFrame frameToShow) {
	try {
	    frameToShow.setVisible(true);
	    if (checkInternalFrame(frameToShow)) {
		deiconifyAndActivate(frameToShow);
		frameToShow.moveToFront();
		activeWin = stringToWin(frameToShow.getTitle());
	    } else {
		setupWindowsMenuOnDisplay();
	    }
	} catch (Exception e) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", e);
	}
    }

    private XFutureVoid setupWindowsMenu() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		return runOnDispatchThread("setupWindowsMenuOnDisplay", this::setupWindowsMenuOnDisplay);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	}
	return XFutureVoid.completedFuture();
    }

    @UIEffect
    private void setupWindowsMenuOnDisplay() {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setupWindowsMenu();
	}
    }

    private ActiveWinEnum activeWin = ActiveWinEnum.OTHER;

    private ActiveWinEnum stringToWin(String str) {
	if (str.startsWith("CRCL Client")) {
	    return ActiveWinEnum.CRCL_CLIENT_WINDOW;
	}
	if (str.startsWith("Error")) {
	    return ActiveWinEnum.ERRLOG_WINDOW;
	}
	if (str.startsWith("Object2D")) {
	    return ActiveWinEnum.SIMVIEW_WINDOW;
	}
	if (str.startsWith("[Object SP]") || str.endsWith("Vision To Database")) {
	    return ActiveWinEnum.VISION_TO_DB_WINDOW;
	}
	if (str.startsWith("CRCL Simulation Server")) {
	    return ActiveWinEnum.CRCL_CLIENT_WINDOW;
	}
	if (str.startsWith("Database Setup")) {
	    return ActiveWinEnum.DATABASE_SETUP_WINDOW;
	}
//        if (str.startsWith("PDDL Planner")) {
//            return ActiveWinEnum.PDDL_PLANNER_WINDOW;
//        }
	if (str.startsWith("PDDL Actions to CRCL") || str.endsWith("(Executor)")) {
	    return ActiveWinEnum.PDDL_EXECUTOR_WINDOW;
	}
	if (str.startsWith("Kit") || str.endsWith("(Inspection)")) {
	    return ActiveWinEnum.KIT_INSPECTION_WINDOW;
	}
	return ActiveWinEnum.OTHER;
    }

    /**
     * Get a list of items with names and poses from the simulation.
     *
     * @return list of items as generated by the simulation
     * @throws IllegalStateException Object 2D view was not opened.
     */
    public List<PhysicalItem> getSimItemsData() throws IllegalStateException {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("null == object2DViewJInternalFrame)");
	}
	return object2DViewJInternalFrame.getItems();
    }

    /**
     * Get a list of items with names and poses from the simulation.
     *
     * @return list of items as generated by the simulation
     * @throws IllegalStateException Object 2D view was not opened.
     */
    public List<PhysicalItem> getLastVisItemsData() throws IllegalStateException {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("null == visionToDbJInternalFrame)");
	}
	return visionToDbJInternalFrame.getLastVisItemsData();
    }

    /**
     * Set a list of items with names and poses to be used by the simulation.
     * <p>
     * This has no effect if the Object 2D view has not been opened or it is not in
     * simulation mode.
     *
     * @param items list of items to use
     */
    public void setSimItemsData(List<PhysicalItem> items) {
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.setItems(items);
	}
    }

    /**
     * Set the position the robot tool should be moved to ensure the robot is no
     * longer obstructing the vision systems view of the parts and trays.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return a future object for determining when set is complete.
     */
    public XFutureVoid setLookForXYZ(double x, double y, double z) {
	if (null != executorJInternalFrame1) {
	    return executorJInternalFrame1.setLookForXYZ(x, y, z);
	}
	return XFutureVoid.completedFuture();
    }

    /**
     * Set limits on the area that should be visible in the Object 2D view.
     *
     * @param minX minimum X
     * @param minY minimum Y
     * @param maxX maximum X
     * @param maxY maximum Y
     */
    public void setViewLimits(double minX, double minY, double maxX, double maxY) {
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.setViewLimits(minX, minY, maxX, maxY);
	}
    }

    /**
     * Set limits on the area that should be visible in the Object 2D view.
     *
     * @param minX minimum X
     * @param minY minimum Y
     * @param maxX maximum X
     * @param maxY maximum Y
     */
    public void setSimSenseLimits(double minX, double minY, double maxX, double maxY) {
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.setViewLimits(minX, minY, maxX, maxY);
	}
    }

    private final AtomicInteger so2dCount = new AtomicInteger();

    private volatile @Nullable XFutureVoid startObject2DJinternalFrameFuture = null;

    /**
     * Make the Object 2D view visible and create underlying components.
     *
     * @return used to determine when the frame is started.
     */
    public XFutureVoid startObject2DJinternalFrame() {
	XFutureVoid ret = Utils.composeToVoidOnDispatchThread(this::startObject2DJinternalFrameOnDisplay);
	startObject2DJinternalFrameFuture = ret;
	return ret;
    }

    private volatile @Nullable XFutureVoid startObject2DJinternalFrameOnDisplayFuture = null;

    @UIEffect
    private XFutureVoid startObject2DJinternalFrameOnDisplay() {
	try {

	    boolean alreadySelected = isObject2DViewStartupSelected();
	    if (!alreadySelected) {
		setObject2DViewStartupSelected(true);
	    }
	    Object2DViewJInternalFrame newObject2DViewJInternalFrame = new Object2DViewJInternalFrame();
	    newObject2DViewJInternalFrame.setAprsSystem(this);
	    if (null != externalSlotOffsetProvider) {
		newObject2DViewJInternalFrame.setSlotOffsetProvider(externalSlotOffsetProvider);
	    }
	    File object2DViewPropertiesFile = object2DViewPropertiesFile();
	    newObject2DViewJInternalFrame.setPropertiesFile(object2DViewPropertiesFile);

	    addInternalFrame(newObject2DViewJInternalFrame);
	    this.object2DViewJInternalFrame = newObject2DViewJInternalFrame;
	    if (object2DViewPropertiesFile.exists()) {
		XFutureVoid ret = newObject2DViewJInternalFrame.loadProperties();
		startObject2DJinternalFrameOnDisplayFuture = ret;
		return ret;
	    } else if (newPropertiesFile) {
		return XFutureVoid.completedFutureWithName(object2DViewPropertiesFile + " does not exist");
	    } else {
		throw new IllegalStateException(object2DViewPropertiesFile + " does not exist");
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private File object2DViewPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return object2DViewPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

    final private List<Runnable> titleUpdateRunnables = Collections.synchronizedList(new ArrayList<>());

    /**
     * Get the value of titleUpdateRunnables
     *
     * @return the value of titleUpdateRunnables
     */
    public List<Runnable> getTitleUpdateRunnables() {
	return titleUpdateRunnables;
    }

    /**
     * Get the error string associated with the current CRCL client if one.
     *
     * @return error string or null if no error has occurred.
     */
    public @Nullable String getCrclClientErrorString() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getCrclClientErrorMessage();
	}
	return null;
    }

    /**
     * Is this system in the process of aborting?
     *
     * @return aborting status
     */
    public boolean isAborting() {
	return (null != safeAbortAndDisconnectFuture && !safeAbortAndDisconnectFuture.isDone())
		|| (null != safeAbortFuture && !safeAbortFuture.isDone());
    }

    public void printAbortingInfo(PrintStream ps) {
	ps.println("safeAbortAndDisconnectFuture = " + safeAbortAndDisconnectFuture);
	if (null != safeAbortAndDisconnectFuture) {
	    safeAbortAndDisconnectFuture.printStatus(ps);
	    safeAbortAndDisconnectFuture.printProfile(ps);
	}
	ps.println("safeAbortFuture = " + safeAbortFuture);
	if (null != safeAbortFuture) {
	    safeAbortFuture.printStatus(ps);
	    safeAbortFuture.printProfile(ps);
	}
	ps.println("lastRunProgramFuture = " + lastRunProgramFuture);
	if (null != lastRunProgramFuture) {
	    lastRunProgramFuture.printStatus(ps);
	    lastRunProgramFuture.printProfile(ps);
	}
	final XFuture<Boolean> lastContinueActionListFutureFinal = lastContinueActionListFuture;
	if (null != lastContinueActionListFutureFinal) {
	    System.err.println("continueActionsComment = " + continueActionsComment);
	    System.err.println("lastContinueActionListFutureComment=" + lastContinueActionListFutureComment);
	    System.err.println("continueActionListTrace=" + Utils.traceToString(continueActionListTrace));
	    System.err.println("lastContinueActionListFuture=" + lastContinueActionListFutureFinal);
	    lastContinueActionListFutureFinal.printStatus(System.err);
	    lastContinueActionListFutureFinal.printProfile(System.err);
	}
	final XFuture<Boolean> lastStartActionsFutureFinal = lastStartActionsFuture;
	if (null != lastStartActionsFutureFinal) {
	    System.err.println("startActionsComment = " + startActionsComment);
	    System.err.println("startActionsTrace=" + Utils.traceToString(startActionsTrace));
	    System.err.println("startActionsInternalTrace=" + Utils.traceToString(startActionsInternalTrace));
	    System.err.println("startActionsFuture=" + lastStartActionsFutureFinal);
	    lastStartActionsFutureFinal.printStatus(System.err);
	    lastStartActionsFutureFinal.printProfile(System.err);
	}
	if (isRunningCrclProgram()) {
	    ps.println("sys.getLastRunningProgramTrueInfo=" + this.getLastRunningProgramTrueInfo());
	    if (null != startCrclProgramTrace) {
		ps.println("startCrclProgramTrace = " + Utils.traceToString(startCrclProgramTrace));
	    }
	    if (null != runCRCLProgramTrace) {
		ps.println("runCRCLProgramTrace = " + Utils.traceToString(runCRCLProgramTrace));
	    }
	}
	ps.println("startSafeAbortAndDisconnectComment = " + startSafeAbortAndDisconnectComment);
	ps.println("startSafeAbortStackTrace = " + Utils.traceToString(startSafeAbortStackTrace));
	ps.println("startSafeAbortAndDisconnectStackTrace = "
		+ Utils.traceToString(startSafeAbortAndDisconnectStackTrace));
	debugAction();
    }

    private long lastRunAllUpdateRunnableTime = System.currentTimeMillis();

    private volatile String lastTitleStateString = "";
    private volatile String lastTitleStateDescription = "";

    private volatile String lastRobotString = "";
    private volatile String lastTaskString = "";
    private volatile String lastCrclClientString = "";

    private void updateTitleStateDescription(String stateString, String stateDescription) {
	try {
	    if (runProgramServiceThread != null && runProgramServiceThread != Thread.currentThread()) {
		throw new RuntimeException("updateTitleStateDescription called from wrong thread "
			+ Thread.currentThread() + " instead of " + runProgramServiceThread);
	    }
	    String oldTitle = getTitle();
	    String crclClientError = getCrclClientErrorString();
	    if (null != crclClientError && crclClientError.length() > 0
		    && (null == titleErrorString || titleErrorString.length() < 1)) {
		this.titleErrorString = crclClientError;
		privatInternalPause();
	    }
	    if (isPaused()) {
		stateString = "PAUSED";
	    }
	    final String robotString = (robotName != null) ? robotName + (isConnected() ? "" : "(Disconnected)")
		    : "NO Robot";
	    final String taskString = (taskName != null) ? taskName : "NO Task";
	    final String actionSetsCompletedString = (executorJInternalFrame1 != null)
		    ? (" : " + executorJInternalFrame1.getActionSetsCompleted())
		    : "";
	    String newTitle = "APRS : "
		    + robotString + " : "
		    + taskString + " : "
		    + stateDescription
		    + ((titleErrorString != null) ? ": " + titleErrorString : "")
		    + actionSetsCompletedString
		    + (isAborting() ? " : Aborting" : "")
		    + (isReverseFlag() ? " : Reverse" : "")
		    + (getExcutorForceFakeTakeFlag() ? " : Force-Fake-Take" : "");
	    if (newTitle.length() > 100) {
		newTitle = newTitle.substring(0, 100) + " ... ";
	    }
	    if (!oldTitle.equals(newTitle)) {
		logEvent("newTitle", newTitle);
		updateDetailsString();
		setTitle(newTitle);
		this.asString = newTitle;
		runAllUpdateRunnables();
	    } else {
		long time = System.currentTimeMillis();
		if (time - lastRunAllUpdateRunnableTime > 500) {
		    updateDetailsString();
		    runAllUpdateRunnables();
		}
	    }
	    if (null != crclClientJInternalFrame) {
		String crclClientString = crclClientJInternalFrame.getTitle();
		if (!Objects.equals(crclClientString, lastCrclClientString)) {
		    if (!Objects.equals(lastRobotString, robotString)
			    || !Objects.equals(lastTaskString, taskString)
			    || !Objects.equals(lastTitleStateDescription, stateDescription)) {
			setupWindowsMenu();
		    }
		}
		lastCrclClientString = crclClientString;
	    }
	    lastRobotString = robotString;
	    lastTaskString = taskString;
	    lastTitleStateString = stateString;
	    lastTitleStateDescription = stateDescription;
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    private void runAllUpdateRunnables() {
	for (Runnable r : titleUpdateRunnables) {
	    r.run();
	}
	lastRunAllUpdateRunnableTime = System.currentTimeMillis();
    }

//    private String pddlActionString() {
//        if (null == executorJInternalFrame1) {
//            return "";
//        }
//        List<Action> actionList = executorJInternalFrame1.getActionsList();
//        if (null == actionList || actionList.isEmpty()) {
//            return "";
//        }
//        int curActionIndex = executorJInternalFrame1.getCurrentActionIndex();
//        if (curActionIndex < 0 || curActionIndex >= actionList.size()) {
//            return " : (" + curActionIndex + "/" + actionList.size() + ")";
//        }
//        return " : (" + curActionIndex + "/" + actionList.size() + "):" + actionList.get(curActionIndex) + " : ";
//    }

    /**
     * Get the current status if available
     *
     * @return current status or null
     */
    public @Nullable CRCLStatusType getCurrentStatus() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getCurrentStatus().orElse(null);
	}
	return null;
    }

    private @Nullable CommandStatusType getCommandStatus() {
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.getCurrentStatus()
		    .map(CRCLStatusType::getCommandStatus)
		    .map(CRCLCopier::copy)
		    .orElse(null);
	}
	return null;
    }

    public void updateTitle() {
	try {
	    if (null != crclClientJInternalFrame) {
		CommandStatusType cs = crclClientJInternalFrame.getCurrentStatus()
			.map(CRCLStatusType::getCommandStatus)
			.orElse(null);
		if (null == cs) {
		    updateTitleStateDescriptionEmpty();
		} else {
		    final CommandStateEnumType commandState = cs.getCommandState();
		    if (null == commandState) {
			updateTitleStateDescriptionEmpty();
		    } else {
			if (commandState == CommandStateEnumType.CRCL_DONE) {
			    CommandStatusType errCmdStatus = titleErrorStringCommandStatus;
			    if (null != errCmdStatus
				    && (errCmdStatus.getCommandID() != cs.getCommandID()
					    || (errCmdStatus.getCommandState() == CommandStateEnumType.CRCL_ERROR))) {
				titleErrorString = null;
			    }
			}
			String description = Objects.toString(cs.getStateDescription(), "");
			updateTitleStateDescription(commandState.toString(), description);
		    }
		}
	    } else {
		updateTitleStateDescriptionEmpty();
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    private void updateTitleStateDescriptionEmpty() {
	updateTitleStateDescription("", "");
    }

    /**
     * Create and display the CRCL client frame (aka pendant client)
     *
     * @return a future object for determining when the frame is visible and ready.
     *
     */
    public XFutureVoid startCrclClientJInternalFrame() {
	return runOnDispatchThread(this::startCrclClientJInternalFrameOnDisplay);
    }

    private void handleCrclClientTitleChanged(CommandStatusType ccst, @Nullable Container container, String stateString,
	    String stateDescription) {
	if (!Objects.equals(stateString, lastTitleStateString)
		|| !Objects.equals(stateDescription, lastTitleStateDescription)) {
	    if (null == runProgramServiceThread || runProgramServiceThread == Thread.currentThread()) {
		updateTitleStateDescription(stateString, stateDescription);
	    } else {
		runProgramService.submit(() -> {
		    if (!Objects.equals(stateString, lastTitleStateString)
			    || !Objects.equals(stateDescription, lastTitleStateDescription)) {
			updateTitleStateDescription(stateString, stateDescription);
		    }
		});
	    }
	}
    }

    @UIEffect
    @SuppressWarnings("nullness")
    private void startCrclClientJInternalFrameOnDisplay() {
	try {
	    boolean alreadySelected = isRobotCrclGUIStartupSelected();
	    if (!alreadySelected) {
		setRobotCrclGUIStartupSelected(true);
	    }
	    if (null == runProgramServiceThread) {
		updateRunProgramServiceThread();
	    }
	    CrclSwingClientJInternalFrame newCrclClientJInternalFrame = new CrclSwingClientJInternalFrame(
		    this.aprsSystemDisplayJFrame);
	    newCrclClientJInternalFrame.setCrclSocketActionExecutorServiceAndThread(runProgramService,
		    runProgramServiceThread);
	    newCrclClientJInternalFrame.setTempLogDir(Utils.getlogFileDir());
	    newCrclClientJInternalFrame.setPropertiesFile(crclClientPropertiesFile());
	    newCrclClientJInternalFrame.loadProperties();
	    System.out.println("getRobotName() = " + getRobotName());
	    System.out.println("newCrclClientJInternalFrame.getPort() = " + newCrclClientJInternalFrame.getPort());
	    addInternalFrame(newCrclClientJInternalFrame);
	    newCrclClientJInternalFrame.addUpdateTitleListener(this::handleCrclClientTitleChanged);
	    if (null != unaddedPoseListeners) {
		List<CurrentPoseListener> tempList = new ArrayList<>(unaddedPoseListeners);
		unaddedPoseListeners.clear();
		for (CurrentPoseListener l : tempList) {
		    newCrclClientJInternalFrame.addCurrentPoseListener(l);
		}
	    }
	    this.crclClientJInternalFrame = newCrclClientJInternalFrame;
	    if (null != this.forceTorqueSimJInternalFrame) {
		forceTorqueSimJInternalFrame.setCrclClientPanel(crclClientJInternalFrame.getPendantClientJPanel1());
	    }
	    updateRobotLimits();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private File crclClientPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return crclClientPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

    /**
     * Create and display the simulation server window and start the simulation
     * server thread.
     *
     * @return a future object for determining when the frame is visible and ready.
     */
    public XFutureVoid startSimServerJInternalFrame() {
	return runOnDispatchThread(this::startSimServerJInternalFrameOnDisplay);
    }

    private static final boolean DEBUG_START_CRCL_SERVER = Boolean.parseBoolean("aprs.debugStartServer");

    /**
     * Create and display the simulation server window and start the simulation
     * server thread.
     */
    @UIEffect
    private void startSimServerJInternalFrameOnDisplay() {
	File propsFile = crclSimServerPropertiesFile();
	try {
	    if (DEBUG_START_CRCL_SERVER) {
		ProtectionDomain simServProt = SimServerJInternalFrame.class
			.getProtectionDomain();
		System.out.println("simServProt = " + simServProt);
		ProtectionDomain crclSocketProt = CRCLSocket.class
			.getProtectionDomain();
		System.out.println("crclSocketProt = " + simServProt);
	    }
	    if (null == simServerJInternalFrame) {
		boolean alreadySelected = isRobotCrclSimServerStartupSelected();
		if (!alreadySelected) {
		    setRobotCrclSimServerStartupSelected(true);
		}
		SimServerJInternalFrame newSimServerJInternalFrame = new SimServerJInternalFrame();
		newSimServerJInternalFrame.setPropertiesFile(propsFile);
		newSimServerJInternalFrame.loadProperties();
		addInternalFrame(newSimServerJInternalFrame);
		if (!newSimServerJInternalFrame.isRunning()) {
		    newSimServerJInternalFrame.restartServer();
		}
		this.simServerJInternalFrame = newSimServerJInternalFrame;
	    }
	} catch (Exception ex) {
	    System.err.println("propsFile=" + propsFile);
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private File crclSimServerPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return crclSimServerPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

    private @Nullable CsvDbSetupPublisher csvDbSetupPublisher = null;

    private final Callable<DbSetupPublisher> dbSetupPublisherSupplier = new Callable<DbSetupPublisher>() {

	@Override
	public DbSetupPublisher call() throws IOException {

	    if (useCsvFilesInsteadOfDatabase) {
		if (null == csvDbSetupPublisher) {
		    csvDbSetupPublisher = new CsvDbSetupPublisher();
		}
		return csvDbSetupPublisher;
	    }
	    createDbSetupFrame();
	    if (null == dbSetupJInternalFrame) {
		throw new IllegalStateException("DB Setup View must be open to use this function.");
	    }
	    return dbSetupJInternalFrame.getDbSetupPublisher();
	}
    };

    private volatile boolean dbConnected = false;

    private void updateDbConnectedCheckBox(DbSetup setup) {
	dbConnected = setup.isConnected();
	setConnectDatabaseCheckBoxEnabled(true);
	XFuture<Boolean> f = dbConnectedWaiters.poll();
	while (f != null) {
	    f.complete(dbConnected);
	    f = dbConnectedWaiters.poll();
	}
    }

    @SuppressWarnings({ "nullness", "initialization" })
    private final DbSetupListener dbSetupListener = this::updateDbConnectedCheckBox;

    private void createDbSetupFrame() {
	if (null == dbSetupJInternalFrame) {
	    runOnDispatchThread(this::createDbSetupFrameOnDisplay);
	}
    }

    @UIEffect
    private void createDbSetupFrameOnDisplay() {
	if (null == dbSetupJInternalFrame) {
	    DbSetupJInternalFrame newDbSetupJInternalFrame = new DbSetupJInternalFrame(this);
	    addInternalFrame(newDbSetupJInternalFrame);
	    newDbSetupJInternalFrame.loadRecentSettings();
	    newDbSetupJInternalFrame.getDbSetupPublisher().addDbSetupListener(dbSetupListener);
	    DbSetup dbs = this.dbSetup;
	    if (null != dbs) {
		DbSetupPublisher pub = newDbSetupJInternalFrame.getDbSetupPublisher();
		if (null != pub) {
		    pub.setDbSetup(dbs);
		}
	    }
	    this.dbSetupJInternalFrame = newDbSetupJInternalFrame;
	}
    }

    /**
     * Set the menu checkbox item to reflect the val of the whether the vision
     * system is connected. This will not cause the system to connect/disconnect
     * only to show the state the caller already knows.
     *
     * @param val of vision systems connected status to show
     */
    public void setShowVisionConnected(boolean val) {
	connectVisionCheckBox.setSelected(val);
    }

    /**
     * Create and show the internal frame for monitoring/modifying the vision to
     * database connection.
     *
     * @return a future object for determining when the frame is visible and ready.
     */
    public XFutureVoid startVisionToDbJinternalFrame() {
	return runOnDispatchThread(this::startVisionToDbJinternalFrameOnDisplay);
    }

    @UIEffect
    private void startVisionToDbJinternalFrameOnDisplay() {
	try {
	    VisionToDbJInternalFrame newVisionToDbJInternalFrame = new VisionToDbJInternalFrame(this);
	    newVisionToDbJInternalFrame.setPropertiesFile(visionToDbPropertiesFile());
	    newVisionToDbJInternalFrame.loadProperties();
	    addInternalFrame(newVisionToDbJInternalFrame);
	    newVisionToDbJInternalFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
	    this.visionToDbJInternalFrame = newVisionToDbJInternalFrame;
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private File visionToDbPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return visionToDbPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

    private volatile @Nullable XFutureVoid xf1 = null;
    private volatile @Nullable XFutureVoid xf2 = null;

    /**
     * Start the PDDL Executor (aka Actions to CRCL) and create and display the
     * window for displaying its output.
     *
     * @return a future object for determining when the frame is visible and ready.
     */
    public XFutureVoid startActionListExecutor() {
	try {
	    return Utils.composeToVoidOnDispatchThread(
		    this::startActionsToCrclJInternalFrame);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    @UIEffect
    public XFutureVoid startActionsToCrclJInternalFrame() {
	try {
	    boolean alreadySelected = isExecutorStartupSelected();
	    if (!alreadySelected) {
		setExecutorStartupSelected(true);
	    }
	    ExecutorJInternalFrame newExecFrame = this.executorJInternalFrame1;
	    if (null == newExecFrame) {
		newExecFrame = new ExecutorJInternalFrame(this);
		this.executorJInternalFrame1 = newExecFrame;
		addInternalFrame(newExecFrame);
	    }

	    assert (null != newExecFrame) : "@AssumeAssertion(nullness)";

	    newExecFrame.setPropertiesFile(actionsToCrclPropertiesFile());
	    newExecFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
//            if (null != pddlPlannerJInternalFrame) {
//                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(newExecFrame);
//            }
	    this.executorJInternalFrame1 = newExecFrame;
	    ExecutorJInternalFrame newExecFrameCopy = newExecFrame;
	    XFutureVoid loadPropertiesFuture = XFutureVoid
		    .runAsync("startActionsToCrclJInternalFrame.loadProperties", () -> {
			try {
			    newExecFrameCopy.loadProperties();
			} catch (IOException ex) {
			    Logger.getLogger(AprsSystem.class
				    .getName()).log(Level.SEVERE, "", ex);
			    throw new RuntimeException(ex);
			}
		    }, runProgramService)
		    .thenComposeToVoid(() -> {
			return syncPauseRecoverCheckbox();
		    });
	    this.xf1 = loadPropertiesFuture;
	    XFutureVoid setupWindowsFuture = loadPropertiesFuture
		    .thenComposeToVoid(() -> {
			return runOnDispatchThread(() -> {
			    if (null != aprsSystemDisplayJFrame) {
				aprsSystemDisplayJFrame.addMenu(newExecFrameCopy.getToolMenu());
			    }
			});
		    });
	    this.xf2 = setupWindowsFuture;
	    return setupWindowsFuture;
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private File actionsToCrclPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return actionsToCrclPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

//    public XFutureVoid startPddlPlanner() {
//        return runOnDispatchThread(this::startPddlPlannerOnDisplay);
//    }
//    @UIEffect
//    private void startPddlPlannerOnDisplay() {
//        try {
//            PddlPlannerJInternalFrame newPddlPlannerJInternalFrame = this.pddlPlannerJInternalFrame;
//            if (newPddlPlannerJInternalFrame == null) {
//                newPddlPlannerJInternalFrame = new PddlPlannerJInternalFrame();
//            }
//            newPddlPlannerJInternalFrame.setPropertiesFile(pddlPlannerPropertiesFile());
//            newPddlPlannerJInternalFrame.loadProperties();
//            newPddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(executorJInternalFrame1);
//            addInternalFrame(newPddlPlannerJInternalFrame);
//            this.pddlPlannerJInternalFrame = newPddlPlannerJInternalFrame;
//        } catch (IOException ex) {
//            Logger.getLogger(AprsSystem.class
//                    .getName()).log(Level.SEVERE, "", ex);
//        }
//    }
//    private File pddlPlannerPropertiesFile() {
//        if (null == propertiesDirectory) {
//            throw new NullPointerException("propertiesDirectory");
//        }
//        return pddlPlannerPropertiesFile(propertiesDirectory, propertiesFileBaseString());
//    }
    private File forceTorqueSimPropertiesFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return forceTorqueSimPropertiesFile(propertiesDirectory, propertiesFileBaseString());
    }

    @UIEffect
    private void maximizeJInteralFrame(JInternalFrame internalFrame) {
	if (null != internalFrame && !isHeadless()) {
	    JDesktopPane desktopPane = internalFrame.getDesktopPane();
	    if (null != desktopPane) {
		DesktopManager desktopManager = desktopPane.getDesktopManager();
		if (null != desktopManager) {
		    desktopManager.maximizeFrame(internalFrame);
		}
	    }
	}
    }

    public XFutureVoid startKitInspection() {
	return runOnDispatchThread(this::startKitInspectionOnDisplay);
    }

    @UIEffect
    private void startKitInspectionOnDisplay() {
	try {
	    if (kitInspectionJInternalFrame == null) {
		kitInspectionJInternalFrame = new KitInspectionJInternalFrame();
	    }
	    updateSubPropertiesFiles();
	    kitInspectionJInternalFrame.loadProperties();
	    addInternalFrame(kitInspectionJInternalFrame);
	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    public int getSnapShotWidth() {
	return snapShotWidth;
    }

    public void setSnapShotWidth(int width) {
	this.snapShotWidth = width;
    }

    public int getSnapShotHeight() {
	return snapShotHeight;
    }

    public void setSnapShotHeight(int height) {
	this.snapShotHeight = height;
    }

    public XFuture<Boolean> queryConnect() {
	final AprsSystemDisplayJFrame aprsSystemDisplayJFrameFinal = aprsSystemDisplayJFrame;
	if (null == aprsSystemDisplayJFrameFinal) {
	    throw new NullPointerException("aprsSystemDisplayJFrame");
	}
	return Utils.composeOnDispatchThread(() -> aprsSystemDisplayJFrameFinal.queryConnect());
    }

    /**
     * Query the user to select a properties file to open.
     *
     * @return a future object for determining when user finished browsing and
     *         opening the properties file
     *
     */
    public XFutureVoid browseOpenPropertiesFile() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		return runOnDispatchThread(
			"browseOpenPropertiesFileOnDisplay",
			this::browseOpenPropertiesFileOnDisplay);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	} else {
	    throw new IllegalStateException("can't browse for files when aprsSystemDisplayJFrame == null");
	}
    }

    @UIEffect
    private void browseOpenPropertiesFileOnDisplay() {
	if (null != aprsSystemDisplayJFrame) {
	    File selectedFile = aprsSystemDisplayJFrame.choosePropertiesFileToOpen();
	    if (null != selectedFile) {
		try {
		    loadSelectedPropertiesFile(selectedFile);

		} catch (IOException ex) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, "", ex);
		}
		this.initLoggerWindowOnDisplay();
		this.commonInit();
	    }
	} else {
	    throw new IllegalStateException("can't browse for files when aprsSystemDisplayJFrame == null");
	}
    }

    public void loadSelectedPropertiesFile(File selectedFile) throws IOException {
	closeAllWindows();
	setPropertiesFile(selectedFile);
	loadProperties();
    }

    private int crclWebServerHttpPort = 8081;

    /**
     * Query the user to select a properties file to save.
     *
     * @return a future object for determining when the user has finished browsing
     *         and saving the properties file.
     */
    public XFutureVoid browseSavePropertiesFileAs() {
	if (null != aprsSystemDisplayJFrame) {
	    try {
		return runOnDispatchThread(
			"browseSavePropertiesFileAsOnDisplay",
			this::browseSavePropertiesFileAsOnDisplay);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	} else {
	    throw new IllegalStateException("can't browse for files when aprsSystemDisplayJFrame == null");
	}
    }

    @UIEffect
    private XFutureVoid browseSavePropertiesFileAsOnDisplay() {
	if (null != aprsSystemDisplayJFrame) {
	    File selectedFile = aprsSystemDisplayJFrame.choosePropertiesFileToOpen();
	    if (null != selectedFile) {
		try {
		    setPropertiesFile(selectedFile);
		    return this.saveProperties();
		} catch (IOException ex) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, "", ex);
		    throw new RuntimeException(ex);
		}
	    } else {
		return XFutureVoid.completedFuture();
	    }
	} else {
	    throw new IllegalStateException("can't browse for files when aprsSystemDisplayJFrame == null");
	}
    }

    @UIEffect
    private void windowClosed() {
	setCrclClientPreClosing(true);
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setVisible(false);
	}
	disconnectVision();
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.stopSimUpdateTimer();
	}
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void windowClosing() {
	setCrclClientPreClosing(true);
	closing = true;
	startingCheckEnabled = false;
	try {
	    immediateAbort();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    disconnectVision();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    startDisconnectDatabase();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    startingCheckEnabled = false;
	    if (isConnected()) {
		disconnectRobotPrivate();
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	try {
	    if (connectDatabaseFuture != null) {
		connectDatabaseFuture.cancelAll(true);
		connectDatabaseFuture = null;
	    }
	    if (continuousDemoFuture != null) {
		continuousDemoFuture.cancelAll(true);
		continuousDemoFuture = null;
	    }
	    if (null != object2DViewJInternalFrame) {
		object2DViewJInternalFrame.stopSimUpdateTimer();
	    }
	    try {
		this.runProgramService.shutdownNow();
		this.runProgramService.awaitTermination(100, TimeUnit.MILLISECONDS);
	    } catch (InterruptedException ignored) {
	    }
	    this.close();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    @SuppressWarnings("guieffect")
    public void forceClose() {
	try {
	    windowClosing();
	} catch (Throwable ignored) {
	}
	try {
	    windowClosed();
	} catch (Throwable ignored) {
	}
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.removeAll();
	    aprsSystemDisplayJFrame.dispose();
	}
    }

    private volatile @Nullable XFuture<Boolean> startLookForPartsFuture = null;
    private volatile StackTraceElement startLookForPartsTrace @Nullable [] = null;

    private volatile boolean doingLookForParts = false;
    private final AtomicInteger lookForPartsCount = new AtomicInteger();

    /**
     * Start a sequence of actions to move the robot out of the way so the vision
     * system can see the parts and wait for the vision system to provide data and
     * the database to be updated. It will happen asynchronously in another thread.
     *
     * @return future allowing the caller to determine when the actions are
     *         complete.
     */
    public synchronized XFuture<Boolean> startLookForParts() {
	try {
	    final ExecutorJInternalFrame pddlExecutorJInternalFrame1Final = executorJInternalFrame1;
	    if (null == pddlExecutorJInternalFrame1Final) {
		throw new NullPointerException("pddlExecutorJInternalFrame1");
	    }
	    checkFutures();
	    doingLookForParts = true;
	    int count = lookForPartsCount.incrementAndGet();
	    long t0 = logEvent("START startLookForParts " + count);
	    logToSuper("START startLookForParts " + count);
	    takeSnapshots("START startLookForParts " + count);
	    endLogged = false;
	    StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	    startLookForPartsTrace = trace;
	    XFuture<Boolean> ret = waitForPause()
		    .thenCompose("startLookForParts.checkDbConnected", x -> checkDBConnected())
		    .thenApplyAsync("startLookForParts.lookForPartsInternal",
			    (Boolean checkDbConnectedRet) -> {
				try {
				    return lookForPartsOnDisplay();
				} catch (Exception exception) {
				    System.out.println("exception = " + exception);
				    System.out.println("startLookForPartsTrace = " + Utils.traceToString(trace));
				    throw new RuntimeException(exception);
				}
			    },
			    runProgramService)
		    .peekException((Throwable throwable) -> {
			if (null != throwable) {
			    System.out.println("throwable = " + throwable);
			    System.out.println("startLookForPartsTrace = " + Utils.traceToString(trace));
			}
		    })
		    .alwaysRunAsync(() -> {
			synchronized (this) {
			    setEndLogged(trace, "lookForParts");
			    logEvent("END startLookForParts", (System.currentTimeMillis() - t0));
			    logToSuper("END startLookForParts " + count);
			    takeSnapshots("END startLookForParts " + count);
			    if (isAborting() && !isDoingActions()) {
				pddlExecutorJInternalFrame1Final.completeSafeAbort();
			    }
			}
			doingLookForParts = false;
		    }, runProgramService);
	    startLookForPartsFuture = ret;
	    return ret;
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString(ex.getMessage());
	    doingLookForParts = false;
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}
    }

    private XFuture<Boolean> checkDBConnected() {
	XFutureVoid cdf = connectDatabaseFuture;
	if (null == cdf) {
	    return startConnectDatabase();
	}
	return XFuture.completedFuture(true);
    }

    private boolean lookForPartsOnDisplay() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	try {
	    CRCLProgramType lfpProgram = executorJInternalFrame1.createLookForPartsProgram();
	    boolean ret = runCRCLProgram(lfpProgram);
	    if (ret) {
		enableCheckedAlready = true;
	    }
	    return ret;
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString(ex.getMessage());
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}
    }

    private XFutureVoid setImageSizeMenuText() {
	if (null != aprsSystemDisplayJFrame) {
	    return runOnDispatchThread(() -> setImageSizeMenuTextOnDisplay(snapShotWidth, snapShotHeight));
	} else {
	    return XFutureVoid.completedFuture();
	}
    }

    @UIEffect
    private void setImageSizeMenuTextOnDisplay(int w, int h) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setImageSizeMenuText(w, h);
	}
    }

    public void setDebug(boolean debug) {
	this.debug = debug;
    }

    @SuppressWarnings("unused")
    public boolean getDebug() {
	return debug;
    }

    private boolean isWithinMaxLimits(PmCartesian cart, PmCartesianMinMaxLimit minMax) {
	PmCartesian maxLimit = minMax.getMax();
	boolean ret = cart != null
		&& cart.x <= maxLimit.x
		&& cart.y <= maxLimit.y
		&& cart.z <= maxLimit.z;
//        if (!ret && isAlertLimitsCheckBoxSelected()) {
//            setTitleErrorString("Position is not within max limits : cart =" + cart + ", maxLimit=" + maxLimit);
//            throw new IllegalStateException("Position is not within max limits : cart =" + cart + ", maxLimit=" + maxLimit);
//        }
	return ret;
    }

    private boolean isWithinMinLimits(PmCartesian cart, PmCartesianMinMaxLimit minMax) {
	PmCartesian minLimit = minMax.getMin();
	boolean ret = cart != null
		&& cart.x >= minLimit.x
		&& cart.y >= minLimit.y
		&& cart.z >= minLimit.z;
//        if (!ret && isAlertLimitsCheckBoxSelected()) {
//            setTitleErrorString("Position is not within min limits : cart =" + cart + ", minLimit=" + minLimit);
//            throw new IllegalStateException("Position is not within min limits : cart =" + cart + ", minLimit=" + minLimit);
//        }
	return ret;
    }

    public boolean isWithinLimits(PmCartesian cart, PmCartesianMinMaxLimit minMax) {
	boolean ret = isWithinMaxLimits(cart, minMax) && isWithinMinLimits(cart, minMax);
//        if (!ret && isAlertLimitsCheckBoxSelected()) {
//            setTitleErrorString("Position is not within limits : cart =" + cart);
//            throw new IllegalStateException("Position is not within limits : cart =" + cart);
//        }
	return ret;
    }

    public boolean isPointWithinLimits(PointType point) {
	return isWithinLimits(CRCLPosemath.toPmCartesian(point));
    }

    public boolean isWithinLimits(PmCartesian cart) {
	if (!enforceMinMaxLimits) {
	    return true;
	}
	if (checkLimitsNoAlert(cart)) {
	    return true;
	}
	if (!limits.isEmpty() && isAlertLimitsCheckBoxSelected()) {
	    PmCartesianMinMaxLimit badLimit = null;
	    for (int i = 0; i < limits.size(); i++) {
		PmCartesianMinMaxLimit lim = limits.get(i);
		if (!isWithinMaxLimits(cart, lim)) {
		    System.err.println(cart + " cver maxlimit " + lim.getMax());
		}
		if (!isWithinMinLimits(cart, lim)) {
		    System.err.println(cart + "under maxlimit " + lim.getMin());
		}
	    }
	    final String errmsg = "Position is not within limits : cart =" + cart + ", b";
	    try {
		takeSimViewSnapshot(errmsg, cart, "bad point");
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
	    }
	    setTitleErrorString(errmsg);
	    throw new IllegalStateException(errmsg);
	}
	return limits.isEmpty();
    }

    public boolean checkLimitsNoAlert(PmCartesian cart) {
	for (int i = 0; i < limits.size(); i++) {
	    PmCartesianMinMaxLimit lim = limits.get(i);
	    if (isWithinLimits(cart, lim)) {
		return true;
	    }
	}
	return false;
    }

    private boolean isWithinMaxLimits(PmCartesian cart, double radius, PmCartesianMinMaxLimit minMax) {
	if (!enforceMinMaxLimits) {
	    return true;
	}
	PmCartesian maxLimit = minMax.getMax();
	boolean ret = cart != null
		&& cart.x + radius <= maxLimit.x
		&& cart.y + radius <= maxLimit.y
		&& cart.z <= maxLimit.z;
	return ret;
    }

    private boolean isWithinMinLimits(PmCartesian cart, double radius, PmCartesianMinMaxLimit minMax) {
	if (!enforceMinMaxLimits) {
	    return true;
	}
	PmCartesian minLimit = minMax.getMin();
	boolean ret = cart != null
		&& cart.x - radius >= minLimit.x
		&& cart.y - radius >= minLimit.y
		&& cart.z >= minLimit.z;
	return ret;
    }

    public boolean isWithinLimits(PmCartesian cart, double radius, PmCartesianMinMaxLimit minMax) {
	if (!enforceMinMaxLimits) {
	    return true;
	}
	boolean ret = isWithinMaxLimits(cart, radius, minMax) && isWithinMinLimits(cart, radius, minMax);
	return ret;
    }

    private boolean isItemWithinLimits(PhysicalItem item, PmCartesianMinMaxLimit minMax) {
	if (!enforceMinMaxLimits) {
	    return true;
	}
	boolean ret = isWithinMaxLimits(item, minMax) && isWithinMinLimits(item, minMax);
	if (!ret) {
	    return false;
	}
	if (item instanceof Tray) {
	    Tray tray = (Tray) item;
	    List<Slot> slots = tray.getAbsSlotList();
	    if (null == slots || slots.isEmpty()) {
		slots = getAbsSlots(tray, false);
	    }
	    for (int i = 0; i < slots.size(); i++) {
		Slot sloti = slots.get(i);
		if (!isWithinLimits(sloti, sloti.getDiameter() / 2.0, minMax)) {
		    return false;
		}
	    }
	} else if (null != item) {
	    final PhysicalItem itemTray = item.getTray();
	    if (null != itemTray) {
		if (!isWithinLimits(itemTray, minMax)) {
		    return false;
		}
	    }
	}
	return ret;
    }

    public void filterTest(String filename) {
	try {
	    final File file = new File(filename);
	    List<PhysicalItem> list = csvFileToItemsList(file);
	    takeSimViewSnapshot("filterTest input " + filename, list);
	    List<PhysicalItem> filteredList = filterListItemWithinLimits(list);
	    takeSimViewSnapshot("filterTest output " + filename, filteredList);
	    saveCsvItemsFile(createTempFile("filterTest", ".csv"));
	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
    }

    public void saveCsvItemsFile(File f) throws IOException {
	if (null == object2DViewJInternalFrame) {
	    throw new NullPointerException("object2DViewJInternalFrame");
	}
	object2DViewJInternalFrame.saveCsvItemsFile(f);
    }

    public void saveCsvItemsFile(File f, Collection<? extends PhysicalItem> items) throws IOException {
	if (null == object2DViewJInternalFrame) {
	    throw new NullPointerException("object2DViewJInternalFrame");
	}
	object2DViewJInternalFrame.saveCsvItemsFile(f);
    }

    public List<PhysicalItem> csvFileToItemsList(File f) throws IOException {
	if (null == object2DViewJInternalFrame) {
	    throw new NullPointerException("object2DViewJInternalFrame");
	}
	return object2DViewJInternalFrame.csvFileToItemsList(f);
    }

    public List<PhysicalItem> csvFileToItemsList(File f, boolean convertRotToRad, boolean zeroRotations)
	    throws IOException {
	if (null == object2DViewJInternalFrame) {
	    throw new NullPointerException("object2DViewJInternalFrame");
	}
	return object2DViewJInternalFrame.csvFileToItemsList(f, convertRotToRad, zeroRotations);
    }

    public boolean isItemWithinLimits(PhysicalItem item) {
	if (!enforceMinMaxLimits) {
	    return true;
	}
	for (int i = 0; i < limits.size(); i++) {
	    PmCartesianMinMaxLimit minMax = limits.get(i);
	    if (isItemWithinLimits(item, minMax)) {
		return true;
	    }
	}
	if (!limits.isEmpty() && isAlertLimitsCheckBoxSelected()) {
	    List<PhysicalItem> limitItems = new ArrayList<>();
	    for (int i = 0; i < limits.size(); i++) {
		PmCartesianMinMaxLimit lim = limits.get(i);
		if (!isWithinMaxLimits(item, lim)) {
		    System.err.println(item + " cver maxlimit " + lim.getMax());
		    limitItems.add(new Part("max" + i, 0, lim.getMax().x, lim.getMax().y));
		}
		if (!isWithinMinLimits(item, lim)) {
		    System.err.println(item + "under minlimit " + lim.getMin());
		    limitItems.add(new Part("min" + i, 0, lim.getMin().x, lim.getMin().y));
		}
	    }
	    if (item instanceof Tray) {
		Tray tray = (Tray) item;
		List<Slot> slots = tray.getAbsSlotList();
		for (int i = 0; i < slots.size(); i++) {
		    Slot sloti = slots.get(i);
		    System.err.println("slot " + i + " = " + sloti);
		    limitItems.add(sloti);
		}
	    } else if (null != item) {
		final PhysicalItem itemTray = item.getTray();
		if (null != itemTray) {
		    if (!isWithinLimits(itemTray)) {
			limitItems.add(itemTray);
		    }
		}
	    }
	    final String errmsg = "Position is not within limits : item =" + item;
	    try {
		if (null != item) {
		    takeSimViewSnapshot(errmsg, item, item.getFullName());
		}
		takeSimViewSnapshot("limitItems" + errmsg, limitItems);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
	    }
	    setTitleErrorString(errmsg);
	    throw new IllegalStateException(errmsg);
	}
	return limits.isEmpty();
    }

    /**
     * Get a Slot with an absolute position from the slot offset and a tray.
     *
     * @param tray           slot is within
     * @param offsetItem     slot with relative position offset for this type of
     *                       tray
     * @param rotationOffset offset to add to the tray rotation
     * @return slot with absolute position
     */
    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
	if (null != externalSlotOffsetProvider) {
	    return externalSlotOffsetProvider.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
	}
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
    }

    public void showFilledKitTrays(boolean useUnassignedParts) {
	try {
	    fillKitTrays(false, 0, true, useUnassignedParts);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystemDisplayJFrame.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
    }

    public void showEmptiedKitTrays() {
	try {
	    emptyKitTrays(false, 0, true);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystemDisplayJFrame.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
    }

    public XFuture<Boolean> fillKitTrays(boolean useUnassignedParts) {
	return fillKitTrays(true, getVisionToDBRotationOffset(), false, useUnassignedParts);
    }

    public XFuture<Boolean> fillKitTraysWithItemList(List<PhysicalItem> items, boolean useUnassignedParts) {
	return fillKitTraysWithItemList(items, true, getVisionToDBRotationOffset(), false, useUnassignedParts);
    }

    public XFuture<Boolean> fillKitTrays(boolean overrideRotationOffset, double newRotationOffset,
	    boolean showFilledListOnly, boolean useUnassignedParts) {

	setCorrectionMode(true);
	XFuture<List<PhysicalItem>> itemsFuture = getSingleRawVisionUpdate();
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.refresh(false);
	}
	return itemsFuture
		.thenCompose((List<PhysicalItem> l) -> {
		    return fillKitTraysWithItemList(l, overrideRotationOffset, newRotationOffset, showFilledListOnly,
			    useUnassignedParts);
		});
    }

    private XFuture<Boolean> fillKitTraysWithItemList(List<PhysicalItem> l, boolean overrideRotationOffset,
	    double newRotationOffset, boolean showFilledListOnly, boolean useUnassignedParts) {
	if (l.isEmpty()) {
	    logToSuper("fillKitTraysWithItemList : l.isEmpty()");
	    return XFuture.completedFuture(true);
	}
	List<PhysicalItem> filteredItems = filterListItemWithinLimits(l);
	try {
	    takeSimViewSnapshot("fillKitTraysWithItemList.filteredItems", filteredItems);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	return fillKitTrays(filteredItems, overrideRotationOffset, newRotationOffset, showFilledListOnly,
		useUnassignedParts);
    }

    private XFuture<Boolean> fillKitTrays(List<PhysicalItem> items, boolean overrideRotationOffset,
	    double newRotationOffset, boolean showFilledListOnly, boolean useUnassignedParts) throws RuntimeException {
	if (items.isEmpty()) {
	    logToSuper("fillKitTraysWithItemList : l.isEmpty()");
	    return XFuture.completedFuture(true);
	}
	try {
	    takeSimViewSnapshot("fillKitTrays", items);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	TrayFillInfo fillInfo = new TrayFillInfo(items, this, overrideRotationOffset, newRotationOffset);
	if (fillInfo.getKitTrays().isEmpty()) {
	    logToSuper("fillKitTraysWithItemList : fillInfo.getKitTrays().isEmpty()");
	    return XFuture.completedFuture(true);
	}
	if (fillInfo.getEmptyKitSlots().isEmpty()) {
	    logToSuper("fillKitTraysWithItemList : fillInfo.getEmptyKitSlots().isEmpty()");
	    return XFuture.completedFuture(true);
	}
	List<PhysicalItem> filledkitTraysList = createFilledKitsListFromFillInfo(fillInfo, useUnassignedParts);

	if (filledkitTraysList == null || filledkitTraysList.isEmpty()) {
	    logToSuper("fillKitTraysWithItemList : filledkitTraysList == null || filledkitTraysList.isEmpty()");
	    return XFuture.completedFuture(true);
	}

	if (showFilledListOnly) {
	    XFuture<Boolean> filledListShowFuture = Utils.supplyOnDispatchThread(() -> {
		if (null == object2DViewJInternalFrame) {
		    throw new NullPointerException("object2DViewJInternalFrame");
		}
		final Properties viewProperties = object2DViewJInternalFrame.getPropertiesOnDisplay();
		boolean userCancelled = Object2DOuterDialogPanel
			.showObject2DDialog(
				aprsSystemDisplayJFrame, // owner
				"Filled Kit Items", // title
				true, // modal
				viewProperties, // props
				filledkitTraysList // items
		);
		return userCancelled;
	    });
	    return filledListShowFuture;
	} else {
	    return fillKitTraysInternal(filledkitTraysList, overrideRotationOffset, newRotationOffset);
	}

    }

    private final AtomicInteger fillKitTraysInternalCount = new AtomicInteger();

    private XFuture<Boolean> fillKitTraysInternal(List<PhysicalItem> filledkitTraysList, boolean overrideRotationOffset,
	    double newRotationOffset) throws RuntimeException {
	try {
	    File actionFile = createActionListFromVision(filledkitTraysList, filledkitTraysList, overrideRotationOffset,
		    newRotationOffset, false, true, false, true);
	    if (null == actionFile) {
		logToSuper("fillKitTraysInternal : null == actionFile");
		return XFuture.completedFuture(true);
	    }
	    noWarnClearActionsList(false);
	    int startAbortCount = getSafeAbortRequestCount();
	    clearKitsToCheck(startAbortCount);
	    List<Action> loadedActions = loadActionsFile(
		    actionFile, // File f,
		    false, // boolean showInOptaPlanner,
		    false, // newReverseFlag
		    true // boolean forceNameChange
	    );
	    int fktic = fillKitTraysInternalCount.incrementAndGet();
	    XFuture<Boolean> psaFuture = privateStartActions("fillKitTrays" + fktic, false, null);
	    XFuture<Boolean> psaClearFuture = psaFuture
		    .alwaysRun(() -> {
			logEvent("Finished fillKitTrays" + fktic + " psaFuture= " + psaFuture);
			logToSuper("Finished fillKitTrays" + fktic + " psaFuture= " + psaFuture);
			noWarnClearActionsList(false);
			clearKitsToCheck(startAbortCount);

		    });
	    return psaClearFuture;
//                    .exceptionally((Throwable throwable) -> {
//                        Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, 
//                                "handling ex returned from privateStartActions(fillKitTrayse)", 
//                                throwable);
//                        System.err.println("fillKitTraysTrace = " + Utils.traceToString(fillKitTraysTrace));
//                        System.err.println("actionFile = " + actionFile);
//                        System.err.println("filledkitTraysList.size() = " + filledkitTraysList.size());
//                        System.err.println("filledkitTraysList = " + filledkitTraysList);
//                        showException(throwable);
//                        if (throwable instanceof RuntimeException) {
//                            throw (RuntimeException) throwable;
//                        } else {
//                            throw new RuntimeException(throwable);
//                        }
//                    });
	} catch (Exception ex) {

	    String errMsg = ex.getMessage() + "\n"
		    + "filledkitTraysList=" + filledkitTraysList + ",\n"
		    + "overrideRotationOffset=" + overrideRotationOffset + ",\n"
		    + "newRotationOffset=" + newRotationOffset + ",\n";
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, errMsg, ex);
	    try {
		takeSimViewSnapshot("fillKitTraysInternal" + ex.getMessage(), filledkitTraysList);
	    } catch (Exception ex1) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex1);
	    }
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}

    }

    private List<PhysicalItem> createFilledKitsListFromFillInfo(TrayFillInfo fillInfo, boolean useUnassignedParts)
	    throws IllegalStateException {
	List<PhysicalItem> outputList = new ArrayList<>();
	outputList.addAll(fillInfo.getKitTrays());
	outputList.addAll(fillInfo.getPartTrays());
	outputList.addAll(fillInfo.getPartsInKit());
	List<PhysicalItem> partsInPartsTrays = new ArrayList<>(fillInfo.getPartsInPartsTrays());
	List<PhysicalItem> availableParts = new ArrayList<>();
	if (useUnassignedParts) {
	    List<PhysicalItem> unassignedParts = new ArrayList<>(fillInfo.getUnassignedParts());
	    try {
		takeSimViewSnapshot("createFilledKitsList_unassignedParts", unassignedParts);
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
	    }
	    availableParts.addAll(unassignedParts);
	}
	availableParts.addAll(partsInPartsTrays);
	List<TraySlotListItem> emptyKitSlots = fillInfo.getEmptyKitSlots();
	try {
	    List<PhysicalItem> emptyKitSlotsList = new ArrayList<>();
	    for (TraySlotListItem emptySlotItem : emptyKitSlots) {
		final PhysicalItem emptySlotItemAbsSlot = emptySlotItem.getAbsSlot();
		if (null != emptySlotItemAbsSlot) {
		    emptyKitSlotsList.add(emptySlotItemAbsSlot);
		}
		final PhysicalItem emptySlotItemClosestPart = emptySlotItem.getClosestPart();
		if (null != emptySlotItemClosestPart) {
		    emptyKitSlotsList.add(emptySlotItemClosestPart);
		}
	    }
	    if (emptyKitSlotsList.isEmpty()) {
		return Collections.emptyList();
	    }
	    takeSimViewSnapshot("createFilledKitsList_emptyKitSlotsList", emptyKitSlotsList);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	List<PhysicalItem> movedPartsList = new ArrayList<>();
	for (TraySlotListItem emptySlotItem : emptyKitSlots) {
	    int itemFoundIndex = -1;
	    for (int i = 0; i < availableParts.size(); i++) {
		PhysicalItem item = availableParts.get(i);
		String shortItemName = shortenItemPartName(item.getName());

		String slotName = emptySlotItem.getSlotOffset().getSlotName();
		if (!Objects.equals(shortItemName, slotName)) {
		    int in_pt_index = shortItemName.indexOf("_in_pt");
		    if (in_pt_index > 0) {
			throw new IllegalStateException("bad itemName for item=" + item);
		    }
		    int in_kt_index = shortItemName.indexOf("_in_kt");
		    if (in_kt_index > 0) {
			throw new IllegalStateException("bad itemName for item=" + item);
		    }
		}
		if (Objects.equals(shortItemName, slotName)) {
		    PhysicalItem newItem = PhysicalItem.newPhysicalItemNameRotXYScoreType(item.getName(),
			    item.getRotation(), emptySlotItem.getAbsSlot().x, emptySlotItem.getAbsSlot().y,
			    item.getScore(), item.getType());
		    movedPartsList.add(newItem);
		    itemFoundIndex = i;
		    break;
		}
	    }
	    if (-1 != itemFoundIndex) {
		availableParts.remove(itemFoundIndex);
	    }
	}
	outputList.addAll(movedPartsList);
	try {
	    if (movedPartsList.isEmpty()) {
		return Collections.emptyList();
	    }
	    takeSimViewSnapshot("createFilledKitsList_movedPartsList", movedPartsList);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	outputList.addAll(availableParts);
	try {
	    takeSimViewSnapshot("createFilledKitsList_outputList", outputList);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	final int outputListSize = outputList.size();
	final int fillOrigItemsSize = fillInfo.getOrigItems().size();
	final boolean outputSizeIncorrect;
	if (useUnassignedParts) {
	    outputSizeIncorrect = outputListSize != fillOrigItemsSize;
	} else {
	    outputSizeIncorrect = outputListSize != (fillOrigItemsSize - fillInfo.getUnassignedParts().size());
	}
	if (outputSizeIncorrect) {
	    try {
		takeSimViewSnapshot("createFilledKitsList_fillInfo.partsInPartsTrays", partsInPartsTrays);
		takeSimViewSnapshot("createFilledKitsList_fillInfo.getUnassignedParts()",
			fillInfo.getUnassignedParts());
		takeSimViewSnapshot("createFilledKitsList_fillInfo.getOrigItems()", fillInfo.getOrigItems());
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
	    }
	    final String errMsg = outputListSize + " != " + fillOrigItemsSize
		    + " : outputList.size() != fillInfo.getOrigItems().size()";
	    setTitleErrorString(errMsg);
	    throw new IllegalStateException(errMsg);
	}
	return outputList;
    }

    public XFuture<Boolean> emptyKitTrays() {
	return emptyKitTrays(true, getVisionToDBRotationOffset(), false);
    }

    public XFuture<Boolean> emptyKitTraysWithItemList(List<PhysicalItem> items) {
	return emptyKitTraysWithItemList(items, true, getVisionToDBRotationOffset(), false);
    }

    public XFuture<Boolean> emptyKitTrays(boolean overrideRotationOffset, double newRotationOffset,
	    boolean showEmptiedListOnly) {

	setCorrectionMode(true);
	XFuture<List<PhysicalItem>> itemsFuture = getSingleRawVisionUpdate();
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.refresh(false);
	}
	return itemsFuture
		.thenCompose((List<PhysicalItem> l) -> {
		    return emptyKitTraysWithItemList(l, overrideRotationOffset, newRotationOffset, showEmptiedListOnly);
		});
    }

    private XFuture<Boolean> emptyKitTraysWithItemList(List<PhysicalItem> l, boolean overrideRotationOffset,
	    double newRotationOffset, boolean showEmptiedListOnly) {
	if (l.isEmpty()) {
	    logToSuper("emptyKitTraysWithItemList : l.isEmpty() ");
	    return XFuture.completedFuture(true);
	}
	List<PhysicalItem> filteredItems = filterListItemWithinLimits(l);
	try {
	    takeSimViewSnapshot("emptyKitTraysWithItemList.filteredItems", filteredItems);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	return emptyKitTrays(filteredItems, overrideRotationOffset, newRotationOffset, showEmptiedListOnly);
    }

    public List<PhysicalItem> filterListItemWithinLimits(List<PhysicalItem> l) {
	return l.stream().filter(this::isItemWithinLimits).collect(Collectors.toList());
    }

    private XFuture<Boolean> emptyKitTrays(List<PhysicalItem> items, boolean overrideRotationOffset,
	    double newRotationOffset, boolean showEmptiedListOnly) throws RuntimeException {
	if (items.isEmpty()) {
	    logToSuper("emptyKitTrays : l.isEmpty() ");
	    return XFuture.completedFuture(true);
	}
	try {
	    takeSimViewSnapshot("emptyKitTrays", items);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	TrayFillInfo emptyInfo = new TrayFillInfo(items, this, overrideRotationOffset, newRotationOffset);
	if (emptyInfo.getKitTrays().isEmpty()) {
	    logToSuper("emptyKitTrays : emptyInfo.getKitTrays().isEmpty() ");
	    return XFuture.completedFuture(true);
	}
	List<PhysicalItem> emptiedkitTraysList = createEmptiedKitsListFromFillInfo(emptyInfo);

	if (emptiedkitTraysList.isEmpty()) {
	    logToSuper("emptyKitTrays : emptiedkitTraysList.isEmpty()");
	    return XFuture.completedFuture(true);
	}

	if (showEmptiedListOnly) {
	    XFuture<Boolean> emptiedListShowFuture = Utils.supplyOnDispatchThread(() -> {
		if (null == object2DViewJInternalFrame) {
		    throw new NullPointerException("object2DViewJInternalFrame");
		}
		return Object2DOuterDialogPanel.showObject2DDialog(
			aprsSystemDisplayJFrame,
			"Emptied Kit Items",
			true,
			object2DViewJInternalFrame.getPropertiesOnDisplay(),
			emptiedkitTraysList);
	    });
	    return emptiedListShowFuture;
//                    .thenCompose(x -> {
//                        return emptyKitTraysInternal(emptiedkitTraysList, overrideRotationOffset, newRotationOffset);
//                    });
	} else {
	    return emptyKitTraysInternal(emptiedkitTraysList, overrideRotationOffset, newRotationOffset);
	}

    }

    private final AtomicInteger emptyKitTraysInternalCount = new AtomicInteger();

    private XFuture<Boolean> emptyKitTraysInternal(List<PhysicalItem> emptiedkitTraysList,
	    boolean overrideRotationOffset, double newRotationOffset) throws RuntimeException {
	try {
	    File actionFile = createActionListFromVision(emptiedkitTraysList, emptiedkitTraysList,
		    overrideRotationOffset, newRotationOffset, true, true, true, true);
	    if (null == actionFile) {
		logToSuper("emptyKitTraysInternal: null == actionFile");
		return XFuture.completedFuture(true);
	    }
	    noWarnClearActionsList(true);
	    int startAbortCount = getSafeAbortRequestCount();
	    clearKitsToCheck(startAbortCount);
	    List<Action> loadedActions = loadActionsFile(
		    actionFile, // File f,
		    false, // boolean showInOptaPlanner,
		    true, // newReverseFlag
		    true // boolean forceNameChange
	    );
//            loadActionsFile(actionFile, true);
	    int ektic = emptyKitTraysInternalCount.incrementAndGet();
	    XFuture<Boolean> psaFuture = privateStartActions("emptyKitTrays" + ektic, true, null);
	    XFuture<Boolean> psaClearFuture = psaFuture
		    .alwaysRun(() -> {
			logEvent("Finished emptyKitTrays" + ektic + " psaFuture=" + psaFuture);
			logToSuper("Finished emptyKitTrays" + ektic + " psaFuture=" + psaFuture);
			noWarnClearActionsList(true);
			clearKitsToCheck(startAbortCount);
		    });
	    return psaClearFuture;
	} catch (Exception ex) {

	    String errMsg = ex.getMessage() + "\n"
		    + "emptiedkitTraysList=" + emptiedkitTraysList + ",\n"
		    + "overrideRotationOffset=" + overrideRotationOffset + ",\n"
		    + "newRotationOffset=" + newRotationOffset + ",\n";
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, errMsg, ex);
	    try {
		takeSimViewSnapshot("emptyKitTraysInternal" + ex.getMessage(), emptiedkitTraysList);
	    } catch (Exception ex1) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex1);
	    }
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}

    }

    private List<PhysicalItem> createEmptiedKitsListFromFillInfo(TrayFillInfo trayFillInfo)
	    throws IllegalStateException {
	List<PhysicalItem> outputList = new ArrayList<>();
	outputList.addAll(trayFillInfo.getKitTrays());
	outputList.addAll(trayFillInfo.getPartTrays());
	outputList.addAll(trayFillInfo.getPartsInPartsTrays());
	List<PhysicalItem> partsInKit = new ArrayList<>(trayFillInfo.getPartsInKit());
	List<TraySlotListItem> emptyPartsTraySlots = trayFillInfo.getEmptyPartTraySlots();
	List<PhysicalItem> movedPartsList = new ArrayList<>();
	for (TraySlotListItem emptySlotItem : emptyPartsTraySlots) {
	    int itemFoundIndex = -1;
	    for (int i = 0; i < partsInKit.size(); i++) {
		PhysicalItem item = partsInKit.get(i);
		String itemName = shortenItemPartName(item.getName());

		String slotName = emptySlotItem.getSlotOffset().getSlotName();
		if (!Objects.equals(itemName, slotName)) {
		    int in_pt_index = itemName.indexOf("_in_pt");
		    if (in_pt_index > 0) {
			throw new IllegalStateException("bad itemName for item=" + item);
		    }
		    int in_kt_index = itemName.indexOf("_in_kt");
		    if (in_kt_index > 0) {
			throw new IllegalStateException("bad itemName for item=" + item);
		    }
		}
		if (Objects.equals(itemName, slotName)) {
		    PhysicalItem newItem = PhysicalItem.newPhysicalItemNameRotXYScoreType(item.getName(),
			    item.getRotation(), emptySlotItem.getAbsSlot().x, emptySlotItem.getAbsSlot().y,
			    item.getScore(), item.getType());
		    movedPartsList.add(newItem);
		    itemFoundIndex = i;
		    break;
		}
	    }
	    if (-1 != itemFoundIndex) {
		partsInKit.remove(itemFoundIndex);
	    }
	}
	outputList.addAll(movedPartsList);
	outputList.addAll(partsInKit);
	final int outputListSize = outputList.size();
	final int origItemsSize = trayFillInfo.getOrigItems().size();
	final boolean outputSizeIncorrect = outputListSize != (origItemsSize
		- trayFillInfo.getUnassignedParts().size());

	try {
	    takeSimViewSnapshot("createEmptiedKitsList_outputList", outputList);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, null, ex);
	}
	if (outputSizeIncorrect) {
	    try {
		takeSimViewSnapshot("createEmptiedKitsList_movedPartsList", movedPartsList);
		takeSimViewSnapshot("createEmptiedKitsList_partsInKit", partsInKit);
		takeSimViewSnapshot("createEmptiedKitsList_trayFillInfo.getPartsInKit()", trayFillInfo.getPartsInKit());
		takeSimViewSnapshot("createEmptiedKitsList_fillInfo.getUnassignedParts()",
			trayFillInfo.getUnassignedParts());
		takeSimViewSnapshot("createEmptiedKitsList_fillInfo.getOrigItems()", trayFillInfo.getOrigItems());
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
	    }
	    final String errMsg = outputListSize + " != " + origItemsSize
		    + " : outputList.size() != fillInfo.getOrigItems().size()";
	    setTitleErrorString(errMsg);
	    throw new IllegalStateException(errMsg);
	}

	return outputList;
    }

    /**
     * Get a list of items seen by the vision system or simulated in the Object2D
     * view and create a set of actions that will fill empty trays to match.Load
     * this list into the PDDL executor.
     *
     * @return the file created
     */
    public @Nullable File createActionListFromVision() {
	try {
	    List<PhysicalItem> requiredItems = getObjectViewItems();
	    List<PhysicalItem> teachItems = requiredItems;
	    updateScanImage(requiredItems, false);
	    takeSimViewSnapshot("createActionListFromVision", requiredItems);
	    return createActionListFromVision(requiredItems, teachItems, false, 0, false, false, true, true);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString("createActionListFromVision: " + ex.getMessage());
	    throw new RuntimeException(ex);
	}
    }

    private void updateScanImage(List<PhysicalItem> requiredItems, boolean autoScale) {
	runOnDispatchThread(() -> {
	    updateScanImageOnDisplay(requiredItems, autoScale);
	});
    }

    private void updateScanImageWithRotationOffset(List<PhysicalItem> requiredItems, boolean autoScale,
	    double rotationOffset) {
	runOnDispatchThread(() -> {
	    updateScanImageWithRotationOffsetOnDisplay(requiredItems, autoScale, rotationOffset);
	});
    }

    public @Nullable BufferedImage getLiveImage() {
	if (!isConnected()) {
	    return null;
	}
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	ViewOptions opts = new ViewOptions();
	opts.h = 170;
	opts.w = 170;
	opts.disableLabels = true;
	opts.useOverridingAutoscale = false;
	opts.disableLimitsLine = true;
	opts.disableShowCurrent = false;
	opts.addExtras = true;
//        opts.debug = true;
	return object2DViewJInternalFrame.createSnapshotImage(opts);
    }

    private void updateScanImageOnDisplay(List<PhysicalItem> requiredItems, boolean autoScale) {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	ViewOptions opts = new ViewOptions();
	opts.h = 170;
	opts.w = 170;
	opts.disableLabels = true;
	opts.useOverridingAutoscale = true;
	opts.overridingAutoscale = autoScale;
	opts.disableLimitsLine = true;
	opts.disableShowCurrent = true;
	opts.disableRobotsReachLimitsRect = true;
	opts.disableSensorLimitsRect = true;
	Image img = createSnapshotImage(opts, requiredItems);
	this.scanImage = img;
    }

    public void saveScanStyleImage(File f, Collection<? extends PhysicalItem> itemsToPaint) {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	ViewOptions opts = new ViewOptions();
	opts.h = -1;
	opts.w = -1;
	opts.disableLabels = true;
	opts.useOverridingAutoscale = true;
	opts.overridingAutoscale = true;
	opts.disableLimitsLine = true;
	opts.disableShowCurrent = true;
	opts.disableRobotsReachLimitsRect = true;
	opts.disableSensorLimitsRect = true;
	BufferedImage img = createSnapshotImage(opts, itemsToPaint);
	String type = "JPEG";
	int pindex = f.getName().lastIndexOf('.');
	if (pindex > 0) {
	    type = f.getName().substring(pindex + 1);
	}
	try {
	    if (ImageIO.write(img, type, f)) {
//                println("Saved snapshot to " + f.getCanonicalPath());
	    } else {
		println("Can't take snapshot. ImageIO.write: No approriate writer found for type=" + type + ", f=" + f);
	    }
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    public BufferedImage createSnapshotImage(ViewOptions opts, Collection<? extends PhysicalItem> itemsToPaint) {
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	return object2DViewJInternalFrame.createSnapshotImage(opts, itemsToPaint);
    }

    private void updateScanImageWithRotationOffsetOnDisplay(List<PhysicalItem> requiredItems, boolean autoScale,
	    double rotationOffset) {
	if (requiredItems.isEmpty()) {
	    return;
	}
	if (null == object2DViewJInternalFrame) {
	    throw new IllegalStateException("Object 2D View must be open to use this function");
	}
	ViewOptions opts = new ViewOptions();
	opts.h = 170;
	opts.w = 170;
	opts.disableLabels = true;
	opts.useOverridingAutoscale = true;
	opts.overridingAutoscale = autoScale;
	opts.useOverridingRotationOffset = true;
	opts.disableLimitsLine = true;
	opts.disableRobotsReachLimitsRect = true;
	opts.disableSensorLimitsRect = true;
	opts.disableShowCurrent = true;
	opts.overridingRotationOffset = rotationOffset;
	scanImage = object2DViewJInternalFrame.createSnapshotImage(opts, requiredItems);
    }

    private volatile @Nullable GoalLearner goalLearner = null;

    /**
     * Get the value of goalLearner
     *
     * @return the value of goalLearner
     */
    public @Nullable GoalLearner getGoalLearner() {
	return goalLearner;
    }

    /**
     * Set the value of goalLearner
     *
     * @param goalLearner new value of goalLearner
     */
    public void setGoalLearner(GoalLearner goalLearner) {
	this.goalLearner = goalLearner;
    }

    @SuppressWarnings("guieffect")
    private int queryUser(String message) {
	try {
	    if (null == aprsSystemDisplayJFrame) {
		return JOptionPane.CANCEL_OPTION;
	    }
	    AtomicInteger ai = new AtomicInteger();
	    SwingUtilities.invokeAndWait(() -> ai.set(queryUserOnDisplay(message)));
	    return ai.get();
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    @UIEffect
    private int queryUserOnDisplay(String message) {
	if (null == aprsSystemDisplayJFrame) {
	    return JOptionPane.CANCEL_OPTION;
	}
	return JOptionPane.showConfirmDialog(aprsSystemDisplayJFrame, message);
    }

    private boolean checkKitTrays(List<PhysicalItem> kitTrays) {
	if (kitTrays.isEmpty()) {
	    debugDumpStack();
	    if (null == aprsSystemDisplayJFrame
		    || JOptionPane.YES_OPTION != queryUser("Create action list with no kit trays?")) {
		setTitleErrorString("createActionListFromVision: No kit trays");
		throw new IllegalStateException("No kit trays");
	    }
	}
	return true;
    }

    public List<String> getLastCreateActionListFromVisionKitToCheckStrings() {
	if (null == goalLearner) {
	    return Collections.emptyList();
	}
	return goalLearner.getLastCreateActionListFromVisionKitToCheckStrings();
    }

    public void setLastCreateActionListFromVisionKitToCheckStrings(List<String> strings) {
	if (null == goalLearner) {
	    goalLearner = new GoalLearner();
	}
	goalLearner.setLastCreateActionListFromVisionKitToCheckStrings(strings);
    }

    /**
     * Get the value of correctionMode
     *
     * @return the value of correctionMode
     */
    public boolean isCorrectionMode() {
	return (null != goalLearner && goalLearner.isCorrectionMode());
    }

    /**
     * Set the value of correctionMode
     *
     * @param correctionMode new value of correctionMode
     */
    public void setCorrectionMode(boolean correctionMode) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setCheckBoxMenuItemCorrectionModeSelected(correctionMode);
	}
	if (goalLearner == null) {
	    goalLearner = new GoalLearner();
	}
	goalLearner.setCorrectionMode(correctionMode);
    }

    /**
     * Use the provided list of items create a set of actions that will fill empty
     * trays to match.Load this list into the PDDL executor.
     *
     * @param requiredItems          list of items that have to be seen before the
     *                               robot can begin
     * @param teachItems             list of trays and items in the trays as they
     *                               should be when complete.
     * @param overrideRotation       specifies whether to add to
     *                               newRotationOffsetParam to tray rotations in
     *                               determining absolute slot positions
     * @param newRotationOffsetParam offset to tray rotations in determining
     *                               absolute slot positions
     * @param newReverseFlag         should the list move parts from kits to parts
     *                               trays
     * @param allowEmptyKits         allow empty kits tray list even though it
     *                               generally indicates bad vision
     * @param alwaysLoad             load even when no change is detected
     * @param checkLimits            check that positions are not outside of
     *                               cartesian limits
     * @return the file created or null if an error occured
     */
    public @Nullable File createActionListFromVision(
	    List<PhysicalItem> requiredItems,
	    List<PhysicalItem> teachItems,
	    boolean overrideRotation,
	    double newRotationOffsetParam,
	    boolean newReverseFlag,
	    boolean alwaysLoad,
	    boolean allowEmptyKits,
	    boolean checkLimits) {

	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Executor View must be open to use this function.");
	}
	if (teachItems.isEmpty()) {
	    return null;
	}
	long t0 = System.currentTimeMillis();
	long t1 = t0;
	File ret = null;
	try {
	    List<String> startingList = this.getLastCreateActionListFromVisionKitToCheckStrings();
	    GoalLearner goalLearnerLocal = this.goalLearner;
	    if (goalLearnerLocal == null) {
		goalLearnerLocal = new GoalLearner();
		this.goalLearner = goalLearnerLocal;
	    }

	    if (checkLimits) {
		goalLearnerLocal.setItemPredicate(this::isWithinLimits);
	    } else {
		goalLearnerLocal.setItemPredicate(null);
	    }
	    if (goalLearnerLocal.isCorrectionMode() || allowEmptyKits) {
		goalLearnerLocal.setKitTrayListPredicate(null);
	    } else {
		goalLearnerLocal.setKitTrayListPredicate(this::checkKitTrays);
	    }
	    goalLearnerLocal.setSlotOffsetProvider(visionToDbJInternalFrame);

	    boolean allEmptyA[] = new boolean[1];
//            takeSimViewSnapshot("createActionListFromVision:teachItems", teachItems);
	    goalLearnerLocal.setAprsSystem(this);
	    List<Action> actions = goalLearnerLocal.createActionListFromVision(requiredItems, teachItems, allEmptyA,
		    overrideRotation, newRotationOffsetParam);
	    // noinspection UnusedAssignment
	    t1 = System.currentTimeMillis();
	    boolean allEmpty = allEmptyA[0];
	    if (!goalLearnerLocal.isCorrectionMode() && !allowEmptyKits) {
		if (allEmpty || actions == null || actions.isEmpty()) {
		    println("requiredItems = " + requiredItems);
		    println("teachItems = " + teachItems);
		    debugDumpStack();
		    if (null == aprsSystemDisplayJFrame
			    || JOptionPane.YES_OPTION != queryUser("Load action list with all trays empty?")) {
			setTitleErrorString("createActionListFromVision: All kit trays empty");
			throw new IllegalStateException("All kit trays empty");
		    }
		}
	    }

	    List<String> endingList = goalLearnerLocal.getLastCreateActionListFromVisionKitToCheckStrings();
	    String diff = GoalLearner.kitToCheckStringsEqual(startingList, endingList);
	    boolean equal = (diff == null);
	    if (!equal || !goalLearnerLocal.isCorrectionMode() || alwaysLoad) {
		boolean startingReverseFlag = isReverseFlag();
		File f = createTempFile("actionList", ".txt");
		if (startingReverseFlag != newReverseFlag) {
		    setReverseFlag(newReverseFlag, false, false);
		}
		saveActionsListToFile(f, actions);
		ret = f;
	    }
	    if (requiredItems != teachItems) {
		if (overrideRotation) {
		    updateScanImageWithRotationOffset(teachItems, true, newRotationOffsetParam);
		} else {
		    updateScanImage(teachItems, true);
		}
	    }

	    if (!equal || !"createActionListFromVision".equals(lastLogEvent)) {
		logEvent("createActionListFromVision",
			equal + "\n"
				+ String.join("\n", endingList));
	    }
	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString("createActionListFromVision: " + ex.getMessage());
	}
	long t2 = System.currentTimeMillis();
	return ret;
    }

    public static void saveActionsListToFile(File f, List<Action> actions) throws FileNotFoundException {
	try (PrintStream ps = new PrintStream(new FileOutputStream(f))) {
	    for (Action act : actions) {
		ps.println(act.asPddlLine());
	    }
	}
    }

    public List<Action> loadActionsFile(File f, boolean showInOptaPlanner, boolean newReverseFlag,
	    boolean forceNameChange) throws IOException {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Executor View must be open to use this function.");
	}
	return executorJInternalFrame1.loadActionsFile(f, showInOptaPlanner, newReverseFlag, forceNameChange);
    }

    /**
     * Get the list of items displayed in the Object 2D view, they may be simulated
     * or received from the vision system.
     *
     * @return list of items displayed in the Object 2D view
     */
    public List<PhysicalItem> getObjectViewItems() {
	if (null != object2DViewJInternalFrame) {
	    return object2DViewJInternalFrame.getItems();
	}
	throw new IllegalStateException("object2DViewJInternalFrame is null");
    }

    /**
     * Get the list of items displayed in the Object 2D view, they may be simulated
     * or received from the vision system.
     *
     * @return list of items displayed in the Object 2D view
     */
    public List<PhysicalItem> getObjectViewOutputItems() {
	if (null != object2DViewJInternalFrame) {
	    return object2DViewJInternalFrame.getOutputItems();
	}
	throw new IllegalStateException("object2DViewJInternalFrame is null");
    }

    public boolean getExcutorForceFakeTakeFlag() {
	if (executorJInternalFrame1 != null) {
	    return executorJInternalFrame1.getForceFakeTakeFlag();
	}
	return false;
    }

    /**
     * Set the menu checkbox setting to force take operations to be faked so that
     * the gripper will not close, useful for testing.
     *
     * @param val true if take operations should be faked
     */
    public void setExecutorForceFakeTakeFlag(boolean val) {
	if (executorJInternalFrame1 != null) {
	    if (executorJInternalFrame1.getForceFakeTakeFlag() != val) {
		executorJInternalFrame1.setForceFakeTakeFlag(val);
	    }
	}
    }

    private volatile @Nullable XFuture<Boolean> lastResumeFuture = null;

    private volatile @Nullable Thread resumingThread = null;
    private volatile StackTraceElement resumingTrace @Nullable [] = null;
    private volatile boolean resuming = false;

    private boolean isPauseCheckBoxSelected() {
	return pauseCheckBox.isSelected();
    }

    private void setPauseCheckBoxSelected(boolean val) {
	pauseCheckBox.setSelected(val);
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setPaused(val);
	}
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.setPaused(val);
	}
    }

    private boolean isCrclClientJInternalFramePaused() {
	if (crclClientJInternalFrame == null) {
	    return false;
	}
	return crclClientJInternalFrame.isPaused();
    }

    /**
     * Continue operations that were previously paused.
     */
    public void resume() {

	logEvent("resume");
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("null == crclClientJInternalFrame");
	}
	resumingThread = Thread.currentThread();
	resumingTrace = resumingThread.getStackTrace();
	resuming = true;
	boolean badState = pausing;
	try {
	    String startPauseInfo = crclClientJInternalFrame.pauseInfoString();
	    if (this.titleErrorString != null && this.titleErrorString.length() > 0) {
		throw new IllegalStateException("Can't resume when titleErrorString set to " + titleErrorString);
	    }
	    badState = badState || pausing;
	    String crclClientErrString = getCrclClientErrorString();
	    if (crclClientErrString != null && crclClientErrString.length() > 0) {
		throw new IllegalStateException("Can't resume when crclClientErrString set to " + crclClientErrString);
	    }
	    if (isPauseCheckBoxSelected()) {
		setPauseCheckBoxSelected(false);
	    }
	    badState = badState || pausing;
	    clearErrors();
	    badState = badState || pausing;
	    setPauseCheckBoxSelected(false);
	    crclClientJInternalFrame.unpauseCrclProgram();
	    setPauseCheckBoxSelected(false);
	    notifyPauseFutures();
	    badState = badState || pausing;
	    clearErrors();
	    badState = badState || pausing;
	    String methodName = "resume";
	    takeSnapshots(methodName);
	    badState = badState || pausing;
	    setPauseCheckBoxSelected(false);
	    crclClientJInternalFrame.unpauseCrclProgram();
	    setPauseCheckBoxSelected(false);
	    badState = badState || pausing;
	    submitUpdateTitle();
	    badState = badState || pausing;
	    boolean currentPaused = isPaused();
	    if (null != executorJInternalFrame1) {
		executorJInternalFrame1.showPaused(currentPaused);
	    }
	    if (currentPaused) {
		String currentPauseString = crclClientJInternalFrame.pauseInfoString();
		System.err.println("Still paused after resume");
		System.err.println("startPauseInfo = " + startPauseInfo);
		System.err.println("currentPauseString = " + currentPauseString);
		System.err.println("runName=" + getRunName());
		setPauseCheckBoxSelected(currentPaused);
		throw new IllegalStateException("Still paused after resume. crclClientJInternalFrame.isPaused()="
			+ isCrclClientJInternalFramePaused());
	    }
	} finally {
	    resuming = false;
	}
	badState = badState || pausing;
	if (badState) {
	    System.err.println("pauseThread = " + pauseThread);
	    System.err.println("pauseTrace = " + Utils.traceToString(pauseTrace));
	    throw new IllegalStateException("Attempt to resume while pausing");
	}
    }

    /**
     * Check to see if the module responsible for updating the database with data
     * received from the vision system has connected to the database.
     *
     * @return is vision to database system currently connected to the database
     */
    public boolean isVisionToDbConnected() {
	return null != visionToDbJInternalFrame && visionToDbJInternalFrame.isDbConnected();
    }

    public boolean snapshotsEnabled() {
	return null != object2DViewJInternalFrame && isSnapshotsSelected();
    }

    /**
     * Used for logging/debugging. Save a file(s) in the temporary directory with
     * the comment and a timestamp with the current view of the parts and robot.
     *
     * @param comment comment name to include in filename for later analysis
     */
    public void takeSnapshots(String comment) {
	try {
	    if (snapshotsEnabled()) {
		takeSimViewSnapshot(snapshotImageFile(comment), (PmCartesian) null, (String) null);
		if (null != visionToDbJInternalFrame
			&& visionToDbJInternalFrame.isDbConnected()
			&& visionToDbJInternalFrame.databasesUpdatesEnabled()) {
		    startVisionToDbNewItemsImageSave(
			    createTempFile(
				    comment + "_new_database_items",
				    ".PNG",
				    getLogImageDir()));
		}
	    }
	} catch (IOException ex) {
	    Logger.getLogger(CrclGenerator.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

    /**
     * Print a great deal of debugging info to the console.
     */
    public void debugAction() {

	println();
	System.err.println();
	debugAction(System.out);
	println();
	System.err.println();
    }

    public void debugAction(PrintStream ps) {

	ps.println();
	int count = debugActionCount.incrementAndGet();
	ps.println("Begin AprsSystem.debugAction()" + count);
	String details;
	if (Thread.currentThread() == runProgramServiceThread) {
	    ps.println("detailsString = " + detailsString);
	    details = updateDetailsString();
	} else {
	    details = getDetailsString();
	}
	ps.println("details = " + details);
	ps.println("lastContinueCrclProgramResult = " + lastContinueCrclProgramResult);
	ps.println("lastStartActionsFuture = " + lastStartActionsFuture);
	if (null != lastStartActionsFuture) {
	    lastStartActionsFuture.printStatus(System.out);
	}
	ps.println("ContinuousDemoFuture = " + continuousDemoFuture);
	if (null != continuousDemoFuture) {
	    continuousDemoFuture.printStatus(System.out);
	}
	ps.println("safeAbortFuture = " + safeAbortFuture);
	if (null != safeAbortFuture) {
	    safeAbortFuture.printStatus(System.out);
	}
	ps.println("safeAbortAndDisconnectFuture = " + safeAbortAndDisconnectFuture);
	if (null != safeAbortAndDisconnectFuture) {
	    safeAbortAndDisconnectFuture.printStatus(System.out);
	}
	ps.println("lastResumeFuture = " + lastResumeFuture);
	if (null != lastResumeFuture) {
	    lastResumeFuture.printStatus(System.out);
	}

	ps.println("lastPauseFuture = " + lastPauseFuture);
	if (null != lastPauseFuture) {
	    lastPauseFuture.printStatus(System.out);
	}

	ps.println("lastContinueActionListFuture = " + lastContinueActionListFuture);
	if (null != lastContinueActionListFuture) {
	    lastContinueActionListFuture.printStatus(System.out);
	}

	ps.println("lastResumeFuture = " + lastRunProgramFuture);
	if (null != lastRunProgramFuture) {
	    lastRunProgramFuture.printStatus(System.out);
	}
	ps.println("isConnected = " + isConnected());
	ps.println("isPaused = " + isPaused());
	ps.println("getRobotCrclPort = " + getRobotCrclPort());
	ps.println("isCrclProgramPaused() = " + isCrclProgramPaused());
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.debugAction();
	}

	ps.println("End AprsSystem.debugAction()" + count);

	printNameSetInfo();
    }

    private void printNameSetInfo() {
	long curTime = System.currentTimeMillis();
	println("setRobotNameNullThread = " + setRobotNameNullThread);
	println("setRobotNameNullStackTrace = " + Utils.traceToString(setRobotNameNullStackTrace));
	println("setRobotNameNullThreadTime = " + (curTime - setRobotNameNullThreadTime));

	println("setRobotNameNonNullThread = " + setRobotNameNonNullThread);
	println("setRobotNameNonNullStackTrace = " + Utils.traceToString(setRobotNameNonNullStackTrace));
	println("setRobotNameNonNullThreadTime = " + (curTime - setRobotNameNonNullThreadTime));

	println("startSafeAbortAndDisconnectThread = " + startSafeAbortAndDisconnectThread);
	println("startSafeAbortAndDisconnectStackTrace = "
		+ Utils.traceToString(startSafeAbortAndDisconnectStackTrace));
	println("startSafeAbortAndDisconnectTime = " + (curTime - startSafeAbortAndDisconnectTime));
    }

    private volatile @Nullable XFuture<Boolean> continuousDemoFuture = null;

    private final AtomicInteger cdStart = new AtomicInteger();

    /**
     * Start a continuous demo where kits will be built , emptied and built again
     * repeating indefinitely. The demo will begin by checking if the robot can be
     * enabled first. (This may cause a second or two of delay and break clicking
     * sound.)
     *
     * @param comment      optional string used for logging/debugging or tracing
     *                     which caller started the demo.
     * @param reverseFirst begin by emptying the kits
     * @return future that can be used to add actions if the demo is canceled or
     *         fails.
     */
    public XFuture<Boolean> startContinuousDemo(String comment, boolean reverseFirst) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	logEvent("startContinuousDemo");
	int startAbortCount = getSafeAbortRequestCount();
	int startDisconnectCount = disconnectRobotCount.get();
	continuousDemoFuture = startContinuousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount,
		cdStart.incrementAndGet());
	return continuousDemoFuture.alwaysRun(() -> logEvent("finished startContinuousDemo"));
    }

    private XFuture<Boolean> startContinuousDemo(String comment, boolean reverseFirst, int startAbortCount,
	    int startDisconnectCount, int cdStart) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	String executorReadyString = executorJInternalFrame1.readyForNewActionsListInfoString();
	if (!executorJInternalFrame1.readyForNewActionsList()) {
	    logEvent(executorReadyString, executorReadyString);
	    throw new IllegalStateException("!pddlExecutorJInternalFrame1.readyForNewActionsList() with comment=\""
		    + comment + "\" reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount
		    + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart + ",cdCur=" + 1);
	}
	if (startAbortCount != getSafeAbortRequestCount()) {
	    return XFuture.completedFuture(false);
	}
	setStartRunTime();
	String logLabel = "startContinuousDemo(task=" + getTaskName() + ")." + comment + "." + startAbortCount + "."
		+ startDisconnectCount + "." + cdStart + "." + 1;
	logToSuper(logLabel);
	takeSnapshots(logLabel);
	String startRobotName = this.robotName;
	if (null == startRobotName) {
	    throw new IllegalStateException("startContinuousDemo with robotName ==null");
	}
	final String checkedRobotName = startRobotName;
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	continuousDemoFuture = XFuture.supplyAsync("startContinuousDemo(task=" + getTaskName() + ") comment=" + comment,
		new Callable<Boolean>() {

		    @Override
		    public Boolean call() {
			if (null == executorJInternalFrame1) {
			    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
			}
			AprsSystem.this.setReverseFlag(reverseFirst, true, true);
			boolean r0 = executorJInternalFrame1.readyForNewActionsList();
			if (!r0) {
			    System.err.println("starting Continuous demo with comment=\"" + comment
				    + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst
				    + ", startAbortCount=" + startAbortCount + ", startDisconnectCount="
				    + startDisconnectCount + ",cdStart=" + cdStart);
			}
			boolean enabledOk = doCheckEnabled(startAbortCount, checkedRobotName);
			boolean r1 = executorJInternalFrame1.readyForNewActionsList();
			if (!r1) {
			    System.err.println("starting Continuous demo with comment=\"" + comment
				    + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst
				    + ", startAbortCount=" + startAbortCount + ", startDisconnectCount="
				    + startDisconnectCount + ",cdStart=" + cdStart);
			}
			return repeatDoActionWithReverse(enabledOk, comment, reverseFirst, startAbortCount,
				startDisconnectCount, cdStart, trace);
		    }
		},
		XFuture::rethrow,
		runProgramService);
	return continuousDemoFuture;
    }

    private boolean repeatDoActionWithReverse(Boolean x, String comment, boolean reverseFirst, int startAbortCount,
	    int startDisconnectCount, int cdStart, StackTraceElement[] trace) throws IllegalStateException {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	int cdCurLocal = 1;
	while (x && !isAborting()
		&& getSafeAbortRequestCount() == startAbortCount
		&& startDisconnectCount == disconnectRobotCount.get()) {
	    String logLabel2 = "startContinuousDemo(task=" + getTaskName() + ")." + comment + "." + startAbortCount
		    + "." + startDisconnectCount + "." + cdStart + "." + cdCurLocal;
	    logToSuper(logLabel2);
	    takeSnapshots("doActions." + logLabel2);
	    boolean doActionWithReverseOk = doActionsWithReverse(comment, x, reverseFirst, startAbortCount,
		    startDisconnectCount, trace);
	    cdCurLocal++;
	    if (!doActionWithReverseOk) {
		return false;
	    }
	}
	return false;
    }

    private volatile @Nullable Consumer<String> supervisorEventLogger = null;

    /**
     * Get the value of supervisorEventLogger
     *
     * @return the value of supervisorEventLogger
     */
    public @Nullable Consumer<String> getSupervisorEventLogger() {
	return supervisorEventLogger;
    }

    private volatile @MonotonicNonNull Supervisor supervisor = null;

    /**
     * Get the value of supervisor
     *
     * @return the value of supervisorEventLogger
     */
    public @Nullable Supervisor getSupervisor() {
	return supervisor;
    }

    /**
     * Set the value of supervisor
     *
     * @param supervisor new value of supervisorEventLogger
     */
    public void setSupervisor(Supervisor supervisor) {
	this.supervisor = supervisor;
    }

    /**
     * Set the value of supervisorEventLogger
     *
     * @param supervisorEventLogger new value of supervisorEventLogger
     */
    public void setSupervisorEventLogger(Consumer<String> supervisorEventLogger) {
	this.supervisorEventLogger = supervisorEventLogger;
    }

    public void logToSuper(String s) {
	Consumer<String> c = this.supervisorEventLogger;
	if (null != c) {
	    c.accept(getTaskName() + ":" + getMyThreadId() + ":" + s);
	}
    }

    private volatile StackTraceElement doActionsWithReverseTrace @Nullable [] = null;

    private boolean doActionsWithReverse(String comment, Boolean x, boolean reverseFirst, int startAbortCount,
	    int startDisconnectCount, StackTraceElement[] trace) throws IllegalStateException {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	if (isReverseFlag() != reverseFirst) {
	    System.err.println("Reverse flag changed as starting continuous demo.");
	    setTitleErrorString("Reverse flag changed as starting continuous demo.");
	    throw new IllegalStateException("Reverse flag changed as starting continuous demo.");
	}
	doActionsWithReverseTrace = trace;
	if (x
		&& !isAborting()
		&& getSafeAbortRequestCount() == startAbortCount
		&& startDisconnectCount == disconnectRobotCount.get()) {
	    logToSuper("doActionsWithReverse-" + comment + "_" + reverseFirst + "_" + startAbortCount + "_"
		    + startDisconnectCount);
	    long doActions1TimeStart = System.currentTimeMillis();
	    boolean actionsOk = executorJInternalFrame1.doActions(
		    comment + "_" + reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount, startAbortCount,
		    trace);
	    long doActions1TimeEnd = System.currentTimeMillis();
	    logToSuper("actionsOk=" + actionsOk + " time to complete = " + (doActions1TimeEnd - doActions1TimeStart)
		    + " ms");
	    if (isReverseFlag() != reverseFirst) {
		System.err.println("Reverse flag changed as starting continuous demo.");
		setTitleErrorString("Reverse flag changed as starting continuous demo.");
		throw new IllegalStateException("Reverse flag changed as starting continuous demo.");
	    }
	    if (actionsOk
		    && !isAborting()
		    && getSafeAbortRequestCount() == startAbortCount
		    && startDisconnectCount == disconnectRobotCount.get()) {
		setReverseFlag(!reverseFirst, true, true);
		logToSuper("reverseFlag=" + isReverseFlag() + " step 2 doActionsWithReverse-" + comment + "_"
			+ reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount);
		long doActions2TimeStart = System.currentTimeMillis();
		boolean actionsOk2 = executorJInternalFrame1.doActions(
			"2" + comment + "_" + reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount,
			startAbortCount, trace);
		long doActions2TimeEnd = System.currentTimeMillis();
		logToSuper("actionsOk2=" + actionsOk2 + " time to complete = "
			+ (doActions2TimeEnd - doActions2TimeStart) + " ms");
		return actionsOk2;
	    } else {
		return false;
	    }
	}
	return false;
    }

    /**
     * Start a continuous demo where kits will be built , emptied and built again
     * repeating indefinitely. The demo will not begin by checking if the robot can
     * be enabled first. (This may avoid a second or two of delay and break clicking
     * sound but mean that if it can't be enabled the error display may be less
     * clear.)
     *
     * @param comment      optional string used for logging/debugging or tracing
     *                     which caller started the demo.
     * @param reverseFirst begin by emptying the kits
     * @return future that can be used to add actions if the demo is canceled or
     *         fails.
     */
    public XFuture<Boolean> startPreCheckedContinuousDemo(String comment, boolean reverseFirst) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	int startAbortCount = getSafeAbortRequestCount();
	int startDisconnectCount = disconnectRobotCount.get();
	continuousDemoFuture = startPreCheckedContinuousDemo(comment, reverseFirst, startAbortCount,
		startDisconnectCount);
	return continuousDemoFuture;
    }

    private XFuture<Boolean> startPreCheckedContinuousDemo(String comment, boolean reverseFirst, int startAbortCount,
	    int startDisconnectCount) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	String executorReadyString = executorJInternalFrame1.readyForNewActionsListInfoString();
	if (!executorJInternalFrame1.readyForNewActionsList()) {
	    logEvent(executorReadyString, executorReadyString);
	    throw new IllegalStateException("!pddlExecutorJInternalFrame1.readyForNewActionsList() comment=\"" + comment
		    + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst
		    + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount
		    + ",cdStart=" + cdStart + ",cdCur=" + 1);
	}
	int safeAbortRequestCount = getSafeAbortRequestCount();
	if (startAbortCount != safeAbortRequestCount || isAborting()) {
	    continuousDemoFuture = XFuture.completedFutureWithName("startPreCheckedContinuousDemo(" + reverseFirst + ","
		    + startAbortCount + ").safeAbortRequestCount=" + safeAbortRequestCount, false);
	    return continuousDemoFuture;
	}
	setStartRunTime();
	if (!enableCheckedAlready) {
	    continuousDemoFuture = XFuture.completedFutureWithName(
		    "startPreCheckedContinuousDemo(" + reverseFirst + "," + startAbortCount + ").!enableCheckedAlready",
		    false);
	    return continuousDemoFuture;
	}
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	continuousDemoFuture = XFuture.supplyAsync("startPreCheckedContinuousDemo(task=" + getTaskName() + ")",
		() -> {
		    if (null == executorJInternalFrame1) {
			throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
		    }
		    this.setReverseFlag(reverseFirst, true, true);
		    if (startAbortCount != getSafeAbortRequestCount() || isAborting()) {
			return false;
		    }
		    return doActionsWithReverse(comment, true, reverseFirst, startAbortCount, startDisconnectCount,
			    trace);
		}, runProgramService)
		.thenComposeAsync("startPreCheckedContinuousDemo(task=" + getTaskName() + ").recurse",
			x2 -> x2 ? startPreCheckedContinuousDemo(comment, reverseFirst, startAbortCount,
				startDisconnectCount)
				: XFuture.completedFutureWithName("startContinuousDemo.completedFutureWithName", false),
			runProgramService);
	return continuousDemoFuture;
    }

    public @Nullable XFuture<Boolean> getContinuousDemoFuture() {
	return continuousDemoFuture;
    }

    private boolean isReverseCheckBoxSelected() {
	return reverseCheckBox.isSelected();
    }

    public boolean isAlertLimitsCheckBoxSelected() {
	return alertLimitsCheckBox.isSelected();
    }

    /**
     * Get the value of pauseInsteadOfRecover
     *
     * @return the value of pauseInsteadOfRecover
     */
    public boolean isPauseInsteadOfRecover() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	return executorJInternalFrame1.isPauseInsteadOfRecover();
    }

    private volatile StackTraceElement setReverseCheckBoxSelectedTrueTrace @Nullable [] = null;
    private volatile StackTraceElement setReverseCheckBoxSelectedFalseTrace @Nullable [] = null;

    @SuppressWarnings("guieffect")
    private void setReverseCheckBoxSelected(boolean val) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.updateForceFakeTakeState(val);
	}
	reverseCheckBox.setSelected(val);
	logToSuper("setReverseCheckBoxSelected(" + val + ")");
	if (val) {
	    setReverseCheckBoxSelectedTrueTrace = Thread.currentThread().getStackTrace();
	} else {
	    setReverseCheckBoxSelectedFalseTrace = Thread.currentThread().getStackTrace();
	}
    }

    @SuppressWarnings("guieffect")
    public void setAlertLimitsCheckBoxSelected(boolean val) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.updateForceFakeTakeState(val);
	}
	alertLimitsCheckBox.setSelected(val);
    }

    /**
     * Get the state of the reverse flag. It is set to indicate that an alternative
     * set of actions that empty rather than fill the kit trays is in use.
     *
     * @return reverse flag
     */
    public boolean isReverseFlag() {
	return isReverseCheckBoxSelected();
    }

    // /**
//     * Set the state of the reverse flag. It is set to indicate that an
//     * alternative set of actions that empty rather than fill the kit trays is
//     * in use. Reload the simulated object positions.
//     *
//     * @param reverseFlag new value for reverse flag
//     * @return a future object that can be used to determine when setting the
//     * reverse flag and all related actions is complete.
//     */
//    public XFutureVoid startSetReverseFlag(boolean reverseFlag) {
//        clearLogDirCache();
//        return startSetReverseFlag(reverseFlag, true);
//    }
//    /**
//     * Set the state of the reverse flag. It is set to indicate that an
//     * alternative set of actions that empty rather than fill the kit trays is
//     * in use. Optionally reload the simulated object positions.
//     *
//     * @param reverseFlag new value for reverse flag
//     * @param reloadSimFiles whether to load simulated object position files
//     * first
//     * @return a future object that can be used to determine when setting the
//     * reverse flag and all related actions is complete.
//     */
//    
//    public XFutureVoid startSetReverseFlag(boolean reverseFlag, boolean reloadSimFiles) {
//        logEvent("startSetReverseFlag", reverseFlag);
//        if (isDoingActions()) {
//            throw new IllegalStateException("setting reverse flag while doing actions");
//        }
//        return XFuture.runAsync("startSetReverseFlag(" + reverseFlag + "," + reloadSimFiles + ")",
//                () -> {
//                    if (isDoingActions()) {
//                        throw new IllegalStateException("setting reverse flag while doing actions");
//                    }
//                    setReverseFlag(reverseFlag, reloadSimFiles,false);
//                },
//                runProgramService).always(() -> logEvent("finished startSetReverseFlag", reverseFlag));
//    }
    private boolean isReloadSimFilesOnReverseCheckBoxSelected() {
	return reloadSimFilesOnReverseCheckBox.isSelected();
    }

    private void setReloadSimFilesOnReverseCheckBoxSelected(boolean val) {
	reloadSimFilesOnReverseCheckBox.setSelected(val);
    }

    private volatile StackTraceElement setReverseFlagTrueTrace @Nullable [] = null;
    private volatile StackTraceElement setReverseFlagFalseTrace @Nullable [] = null;

    private void setReverseFlag(boolean reverseFlag, boolean reloadSimFiles, boolean reloadActionsFile) {
	if (reverseFlag) {
	    setReverseFlagTrueTrace = Thread.currentThread().getStackTrace();
	} else {
	    setReverseFlagFalseTrace = Thread.currentThread().getStackTrace();
	}
	clearLogDirCache();
	if (isReverseCheckBoxSelected() != reverseFlag) {
	    setExecutorForceFakeTakeFlag(false);
	    setReverseCheckBoxSelected(reverseFlag);
	}
	if (null != object2DViewJInternalFrame) {
	    try {
		object2DViewJInternalFrame.setReverseFlag(reverseFlag);
		if (reloadSimFiles && isReloadSimFilesOnReverseCheckBoxSelected()) {
		    if (object2DViewJInternalFrame.isSimulated() || !object2DViewJInternalFrame.isConnected()) {
			object2DViewJInternalFrame.reloadDataFile();
		    }
		}
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	}
	if (null != executorJInternalFrame1 && reloadActionsFile) {
	    try {
		executorJInternalFrame1.reloadActionsFile(reverseFlag);
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
		throw new RuntimeException(ex);
	    }
	}
	clearLogDirCache();
    }

    /**
     * Get the state of whether the system is paused
     *
     * @return paused state
     */
    public boolean isPaused() {
	return isPauseCheckBoxSelected();
    }

    /**
     * Pause any actions currently being performed and set a state that will cause
     * future actions to wait until resume is called.
     */
    public void pause() {
	checkResuming();
	boolean badState = checkResuming();

	privatInternalPause();
	badState = badState || checkResuming();
	submitUpdateTitle();
	badState = badState || checkResuming();
	if (Utils.arePlayAlertsEnabled()) {
	    runOnDispatchThread(Utils::PlayAlert2);
	}
	badState = badState || checkResuming();
	if (badState) {
	    throw new IllegalStateException("Attempt to pause while resuming:");
	}
    }

    private volatile boolean pausing = false;
    private volatile @Nullable Thread pauseThread = null;
    private volatile StackTraceElement pauseTrace @Nullable [] = null;

    private void privatInternalPause() {
	logEvent("pause");
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setPaused(true);
	}
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.setPaused(true);
	}
	pauseThread = Thread.currentThread();
	pauseTrace = pauseThread.getStackTrace();
	debugDumpStack();
	pausing = true;
	boolean badState = checkResuming();
	try {
	    if (!isPauseCheckBoxSelected()) {
		setPauseCheckBoxSelected(true);
	    }
	    badState = badState || checkResuming();
	    if (null != crclClientJInternalFrame && titleErrorString != null && titleErrorString.length() > 0) {
		String lastMessage = crclClientJInternalFrame.getLastMessage();
		println("lastMessage = " + lastMessage);
		MiddleCommandType cmd = crclClientJInternalFrame.currentProgramCommand();
		if (null != cmd) {
		    String cmdString = CRCLSocket.cmdToString(cmd);
		    println("cmdString = " + cmdString);
		    if (null == lastMessage) {
			lastMessage = "";
		    }
		    takeSnapshots("pause :" + lastMessage + ":" + cmdString);
		} else if (null == lastMessage) {
		    takeSnapshots("pause");
		} else {
		    takeSnapshots("pause :" + lastMessage);
		}
	    }
	    badState = badState || checkResuming();
	    if (null != executorJInternalFrame1) {
		executorJInternalFrame1.showPaused(true);
	    }
	    this.pauseCrclProgram();
	    badState = badState || checkResuming();
	    if (badState) {
		throw new IllegalStateException("Attempt to pause while resuming");
	    }
	} finally {
	    pausing = false;
	    resuming = false;
	}
    }

    private boolean checkResuming() throws IllegalStateException {
	if (resuming) {
	    System.err.println("Attempt to pause while resuming");
	    System.err.println("resumingThread = " + resumingThread);
	    System.err.println("resumingTrace = " + Utils.traceToString(resumingTrace));
	    System.err.println("currentThread=" + Thread.currentThread());
	    System.err.println("currentTrace = " + Utils.traceToString(Thread.currentThread().getStackTrace()));
	    return true;
	}
	return false;
    }

    /**
     * Reset errors and reload simulation files
     *
     * @return a future object that can be used to determine when setting the reset
     *         and all related actions is complete.
     */
    public XFutureVoid reset() {
	return reset(true);
    }

    /**
     * Reset errors and optionally reload simulation files
     *
     * @param reloadSimFiles whether to reload simulation files
     * @return a future object that can be used to determine when setting the reset
     *         and all related actions is complete.
     */
    public XFutureVoid reset(boolean reloadSimFiles) {
	return XFuture.runAsync("reset",
		() -> {
		    resetOnDisplay(reloadSimFiles);
		}, runProgramService);

    }

    private void resetOnDisplay(boolean reloadSimFiles) {
	clearErrors();
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.refresh(reloadSimFiles);
	}
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.refresh();
	}
	clearErrors();
    }

    public void clearKitsToCheck(int startAbortCount) {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.clearKitsToCheck(startAbortCount);
	}
    }

    public void clearActionsList() {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.clearActionsList();
	}
    }

    public void noWarnClearActionsList(boolean revFlag) {
	setReverseCheckBoxSelected(revFlag);
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.noWarnClearActionsList(revFlag);
	}
    }

    private void setCommandID(CRCLCommandType cmd) {
	Utils.setCommandID(cmd, incrementAndGetCommandId());
    }

    private final AtomicInteger createEmptyProgramCount = new AtomicInteger();

    private CRCLProgramType createEmptyProgram() {
	CRCLProgramType prog = new CRCLProgramType();
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	final int c = createEmptyProgramCount.incrementAndGet();
	if (trace.length > 3) {
	    prog.setName(c + "_" + trace[2].toString());
	}
	final InitCanonType initCmd = new InitCanonType();
	prog.setInitCanon(initCmd);

	final EndCanonType endCmd = new EndCanonType();
	prog.setEndCanon(endCmd);
	setCommandID(initCmd);
	setCommandID(endCmd);
	return prog;
    }

    private long incrementAndGetCommandId() {
	if (null != this.executorJInternalFrame1) {
	    return this.executorJInternalFrame1.incrementAndGetCommandId();
	} else {
	    return System.currentTimeMillis();
	}
    }

    /**
     * Clear any error flags / strings set.
     */
    public void clearErrors() {
	this.titleErrorString = null;
	clearCrclClientErrorMessage();
	submitUpdateTitle();
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.setErrorString(null);
	}
	String crclClientErrString = getCrclClientErrorString();
	if (crclClientErrString != null && crclClientErrString.length() > 0) {
	    throw new IllegalStateException("Clear errors failed to clear crclErrorString =  " + crclClientErrString);
	}
    }

    private void clearCrclClientErrorMessage() {
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.clearCrclClientErrorMessage();
	}
    }

    private final AtomicInteger checkEnabledCount = new AtomicInteger();

    private volatile boolean enableCheckedAlready = false;

    public boolean isEnableCheckedAlready() {
	return enableCheckedAlready;
    }

    private volatile @Nullable XFuture<Boolean> lastStartCheckEnabledFuture1 = null;
    private volatile @Nullable XFuture<Boolean> lastStartCheckEnabledFuture2 = null;
    private volatile boolean startingCheckEnabled = false;
    private volatile StackTraceElement startingCheckEnabledTrace @Nullable [] = null;

    /**
     * Test that the robot can be connected by running an empty program.
     * <p>
     * The actions will be executed in another thread after this method returns. The
     * returned future can be used to monitor, cancel or extend the underlying task.
     * The boolean contained in the future will be true only if all actions appear
     * to succeed.
     *
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startCheckEnabled() {
	if (null != disconnectRobotFuture && !disconnectRobotFuture.isDone()) {
	    throw new IllegalStateException(
		    "trying to startCheckEnabled when disconnecRobotFuture is still not complete");
	}
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
	}
	int startAbortCount = getSafeAbortRequestCount();
	String startRobotName = this.robotName;
	if (null == startRobotName) {
	    throw new IllegalStateException("null == startRobotName");
	}
	final String checkedRobotName = startRobotName;
	String startTaskName = this.taskName;
	if (null == startTaskName) {
	    throw new IllegalStateException("null == startTaskName");
	}
	final String checkedTaskName = startTaskName;
	startingCheckEnabled = true;
	startingCheckEnabledTrace = Thread.currentThread().getStackTrace();
	String logString = "startCheckEnabled robotName=" + checkedRobotName + ",task=" + checkedTaskName;
	long t0 = logEvent(logString);
	setStartRunTime();
	if (enableCheckedAlready) {
	    logEvent("startCheckEnabled enableCheckedAlready");
	    startingCheckEnabled = false;
	    return XFuture.completedFutureWithName("startCheckEnabled.enableCheckedAlready", true);
	}

	XFuture<Boolean> doCheckEnabledFuture = XFuture.supplyAsync(logString,
		() -> this.doCheckEnabled(startAbortCount, checkedRobotName), runProgramService);
	this.lastStartCheckEnabledFuture1 = doCheckEnabledFuture;
	XFuture<Boolean> alwaysLogFuture = doCheckEnabledFuture
		.alwaysRun(() -> logEvent("finished " + logString, (System.currentTimeMillis() - t0)));
	this.lastStartCheckEnabledFuture2 = alwaysLogFuture;
	return alwaysLogFuture;
    }

    public int getSafeAbortRequestCount() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException(
		    "PDDL Exectutor View must be open to use this function. : null == pddlExecutorJInternalFrame1");
	}
	return executorJInternalFrame1.getSafeAbortRequestCount();
    }

    private boolean doCheckEnabled(int startAbortCount, String startRobotName) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Executor View must be open to use this function.");
	}
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("CRCL Client View must be open to use this function.");
	}
	if (getSafeAbortRequestCount() != startAbortCount) {
	    startingCheckEnabled = false;
	    return false;
	}
	if (!Objects.equals(this.robotName, startRobotName)) {
	    println("startRobotName = " + startRobotName);
	    println("this.robotName = " + this.robotName);
	    println("setRobotNameNullThread = " + setRobotNameNullThread);
	    println("setRobotNameNullStackTrace = " + Utils.traceToString(setRobotNameNullStackTrace));
	    println("setRobotNameNullThreadTime = " + setRobotNameNullThreadTime);
	    println("setRobotNameNonNullThread = " + setRobotNameNonNullThread);
	    println("setRobotNameNonNullStackTrace = " + Utils.traceToString(setRobotNameNonNullStackTrace));
	    println("setRobotNameNonNullThreadTime = " + setRobotNameNonNullThreadTime);
	    startingCheckEnabled = false;
	    return false;
	}
	setStartRunTime();
	startingCheckEnabled = false;
	if (enableCheckedAlready) {
	    return true;
	}
	try {
	    if (!isConnected()) {
		setConnected(true);
	    }
	    CRCLProgramType emptyProgram = createEmptyProgram();
	    emptyProgram.setName("checkEnabled." + checkEnabledCount.incrementAndGet());
	    setProgram(emptyProgram);
	    boolean progRunRet = crclClientJInternalFrame.runCurrentProgram(isStepMode());

//            println("startCheckEnabled finishing with " + progRunRet);
	    enableCheckedAlready = progRunRet;
	    return progRunRet;
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString(ex.getMessage());
	    showException(ex);
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}
    }

    public boolean isLogCrclProgramsSelected() {
	return logCrclProgramsCheckBox.isSelected();
    }

    public void setLogCrclProgramsSelected(boolean selected) {
	logCrclProgramsCheckBox.setSelected(selected);
    }

    private final AtomicInteger emptyProgramCount = new AtomicInteger();
    private final AtomicInteger consecutiveEmptyProgramCount = new AtomicInteger();

    private void setProgram(CRCLProgramType program) throws JAXBException {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("CRCL Client View must be open to use this function.");
	}
	CrclSwingClientJInternalFrame clntJiF = crclClientJInternalFrame;
	synchronized (clntJiF) {
	    if (CRCLUtils.middleCommands(program).isEmpty()) {
		emptyProgramCount.incrementAndGet();
		int cepCount = consecutiveEmptyProgramCount.incrementAndGet();
		if (cepCount > 1 && debug) {
		    println("emptyProgramCount=" + emptyProgramCount);
		    println("consecutiveEmptyProgramCount=" + cepCount);
		}
	    } else {
		consecutiveEmptyProgramCount.set(0);
	    }
	    clntJiF.setProgram(program);
	}
	logCrclProgFile(program);
    }

    public void logCrclProgFile(CRCLProgramType program) {
	if (isLogCrclProgramsSelected()) {
	    try {
		String progString = CRCLSocket.getUtilSocket().programToPrettyString(program, false);
		File progFile = createTempFile("prog", ".xml", getLogCrclProgramDir());
		try (PrintWriter writer = new PrintWriter(new FileWriter(progFile))) {
		    writer.print(progString);
		}
	    } catch (Exception ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
	    }
	}
    }

    private final ConcurrentLinkedDeque<XFuture<Boolean>> dbConnectedWaiters = new ConcurrentLinkedDeque<>();

    private XFuture<Boolean> waitDbConnected() {
	XFuture<Boolean> f = new XFuture<>("waitDbConnected");
	dbConnectedWaiters.add(f);
	return f;
    }

    private volatile @Nullable XFuture<Boolean> lastStartActionsFuture = null;

    private volatile @Nullable XFuture<Boolean> lastPrivateStartActionsFuture = null;

    /**
     * Get the last future created from a startActions request. Only used for
     * debugging.
     *
     * @return future or null if no startActions request has been made.
     */
    public @Nullable XFuture<Boolean> getLastStartActionsFuture() {
	return lastStartActionsFuture;
    }

    /**
     * Get the last future created from a continueActions request. Only used for
     * debugging.
     *
     * @return future or null if no continueActions request has been made.
     */
    public @Nullable XFuture<Boolean> getContinueActionListFuture() {
	return lastContinueActionListFuture;
    }

    private final ConcurrentLinkedDeque<String> startActionsStartComments = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<String> startActionsFinishComments = new ConcurrentLinkedDeque<>();

    /**
     * Start the PDDL actions currently loaded in the executor from the beginning.
     * <p>
     * The actions will be executed in another thread after this method returns. The
     * returned future can be used to monitor, cancel or extend the underlying task.
     * The boolean contained in the future will be true only if all actions appear
     * to succeed.
     *
     * @param comment        used to track failures and snapshots
     * @param actions        list of actions to execute
     * @param newReverseFlag are the actions used to empty kit trays
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActionsList(String comment, Iterable<Action> actions, boolean newReverseFlag) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	List<Action> actionsCopy = new ArrayList<>();
	for (Action action : actions) {
	    actionsCopy.add(action);
	}
	return privateStartActions(comment, newReverseFlag, actionsCopy);
    }

    /**
     * Check to see if the executor is in a state where it could begin working on a
     * new list of actions.
     *
     * @return is executor ready for new actions.
     */
    public boolean readyForNewActionsList() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	return executorJInternalFrame1.readyForNewActionsList();

    }

    @FunctionalInterface
    public static interface AlternativeActionsInterface<T> {

	public XFuture<T> doActions();
    }

    private volatile @Nullable AlternativeActionsInterface<Boolean> alternativeForwardStartActions = null;

    private volatile @Nullable AlternativeActionsInterface<Boolean> alternativeReverseStartActions = null;

    public @Nullable AlternativeActionsInterface<Boolean> getAlternativeForwardStartActions() {
	return alternativeForwardStartActions;
    }

    public void setAlternativeForwardStartActions(AlternativeActionsInterface<Boolean> alternativeForwardStartActions) {
	this.alternativeForwardStartActions = alternativeForwardStartActions;
    }

    public @Nullable AlternativeActionsInterface<Boolean> getAlternativeReverseStartActions() {
	return alternativeReverseStartActions;
    }

    public void setAlternativeReverseStartActions(AlternativeActionsInterface<Boolean> alternativeReverseStartActions) {
	this.alternativeReverseStartActions = alternativeReverseStartActions;
    }

    private volatile @Nullable AlternativeActionsInterface<Boolean> alternativeForwardContinueActions = null;

    private volatile @Nullable AlternativeActionsInterface<Boolean> alternativeReverseContinueActions = null;

    public @Nullable AlternativeActionsInterface<Boolean> getAlternativeForwardContinueActions() {
	return alternativeForwardContinueActions;
    }

    public void setAlternativeForwardContinueActions(
	    AlternativeActionsInterface<Boolean> alternativeForwardContinueActions) {
	this.alternativeForwardContinueActions = alternativeForwardContinueActions;
    }

    public @Nullable AlternativeActionsInterface<Boolean> getAlternativeReverseContinueActions() {
	return alternativeReverseContinueActions;
    }

    public void setAlternativeReverseContinueActions(
	    AlternativeActionsInterface<Boolean> alternativeReverseContinueActions) {
	this.alternativeReverseContinueActions = alternativeReverseContinueActions;
    }

    private volatile StackTraceElement startActionsTrace @Nullable [] = null;
    private volatile @Nullable String startActionsComment = null;
    private final AtomicInteger startActionsCount = new AtomicInteger();

    private volatile boolean endLogged = false;

    public boolean isEndLogged() {
	return endLogged;
    }

    private volatile boolean lastStartActionsReverseFlag = false;

    /**
     * Start the PDDL actions currently loaded in the executor from the beginning.
     * <p>
     * The actions will be executed in another thread after this method returns. The
     * returned future can be used to monitor, cancel or extend the underlying task.
     * The boolean contained in the future will be true only if all actions appear
     * to succeed.
     *
     * @param comment     comment used for tracking/logging tasks starting the
     *                    actions
     * @param reverseFlag are the actions reversed (take parts from kit trays and
     *                    put back in part trays)
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActions(String comment, boolean reverseFlag) {
	checkFutures();
	final ExecutorJInternalFrame pddlExecutorJInternalFrame1Final = executorJInternalFrame1;
	if (null == pddlExecutorJInternalFrame1Final) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	String executorReadyString = pddlExecutorJInternalFrame1Final.readyForNewActionsListInfoString();
	if (!pddlExecutorJInternalFrame1Final.readyForNewActionsList()) {
	    logEvent("executorReadyString", executorReadyString);
	    final String errmsg = "!pddlExecutorJInternalFrame1.readyForNewActionsList() with comment=\"" + comment
		    + "\" ";
	    logToSuper(this.toString() + " throwing " + errmsg);
	    throw new IllegalStateException(errmsg);
	}
	XFuture<Boolean> ret;
	startActionsComment = comment;
	StackTraceElement trace1[] = Thread.currentThread().getStackTrace();

	startActionsTrace = trace1;
	int saCount = startActionsCount.incrementAndGet();

	logEvent("START startActions", comment, reverseFlag, saCount);
	takeSnapshots("START startActions" + saCount + " " + comment);
	logToSuper("START startActions\"" + toString() + "\" " + saCount + " " + comment);
	lastStartActionsReverseFlag = reverseFlag;
	setReverseCheckBoxSelected(reverseFlag);
	endLogged = false;
	if (!reverseFlag) {
	    if (alternativeForwardStartActions != null) {
		ret = alternativeForwardStartActions.doActions();
	    } else {
		ret = privateStartActions(comment, reverseFlag, null);
	    }
	} else if (alternativeReverseStartActions != null) {
	    ret = alternativeReverseStartActions.doActions();
	} else {
	    ret = privateStartActions(comment, reverseFlag, null);
	}
	ret = ret.alwaysRunAsync(() -> {
	    synchronized (this) {
		setEndLogged(trace1, comment);
		logEvent("END startActions", comment, reverseFlag, saCount);
		takeSnapshots("END startActions" + saCount + " " + comment);
		logToSuper("END startActions\"" + toString() + "\" " + saCount + " " + comment);
		if (isAborting() && !isDoingActions()) {
		    pddlExecutorJInternalFrame1Final.completeSafeAbort();
		}
	    }
	}, runProgramService);
	lastStartActionsFuture = ret;
	return ret;
    }

    public synchronized void checkFutures() throws IllegalStateException {
	final XFuture<Boolean> startLookForPartsFutureFinal = startLookForPartsFuture;
	if (doingLookForParts) {
	    debugDumpStack();
	    System.err.println("startLookForPartsTrace=" + Utils.traceToString(startLookForPartsTrace));
	    if (null != startLookForPartsFutureFinal) {
		startLookForPartsFutureFinal.printStatus(System.err);
	    }
	    throw new IllegalStateException("doingLookForParts");
	}
	final XFuture<Boolean> lastPrivateContinueActionListFutureFinal = lastPrivateContinueActionListFuture;
	if (runningPrivateContinueActionList) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("lastContinueActionListFutureComment=" + lastContinueActionListFutureComment);
	    System.err.println("privateContinueActionListTrace=" + Utils.traceToString(privateContinueActionListTrace));
	    if (null != lastPrivateContinueActionListFutureFinal) {
		lastPrivateContinueActionListFutureFinal.printStatus(System.err);
	    }
	    throw new IllegalStateException("runningPrivateContinueActionList");
	}
	if (runningPrivateStartActions) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("lastContinueActionListFutureComment=" + lastContinueActionListFutureComment);
	    System.err.println("privateContinueActionListTrace=" + Utils.traceToString(privateContinueActionListTrace));
	    if (null != lastPrivateContinueActionListFutureFinal) {
		lastPrivateContinueActionListFutureFinal.printStatus(System.err);
	    }
	    throw new IllegalStateException("runningPrivateContinueActionList");
	}
	final XFuture<Boolean> lastPrivateStartActionsFutureFinal = lastPrivateStartActionsFuture;
	if (null != lastPrivateStartActionsFutureFinal
		&& !lastPrivateStartActionsFutureFinal.isDone()
		&& !lastPrivateStartActionsFutureFinal.isCompletedExceptionally()
		&& !lastPrivateStartActionsFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("privateStartActionsComment = " + privateStartActionsComment);
	    System.err.println("privateStartActionsTrace=" + Utils.traceToString(privateStartActionsTrace));
	    System.err.println("startActionsInternalTrace=" + Utils.traceToString(startActionsInternalTrace));
	    lastPrivateStartActionsFutureFinal.printStatus(System.err);
	    throw new IllegalStateException("lastPrivateStartActionsFuture=" + lastPrivateStartActionsFutureFinal);
	}
	if (null != startLookForPartsFutureFinal
		&& !startLookForPartsFutureFinal.isDone()
		&& !startLookForPartsFutureFinal.isCompletedExceptionally()
		&& !startLookForPartsFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("startLookForPartsTrace=" + Utils.traceToString(startLookForPartsTrace));
	    startLookForPartsFutureFinal.printStatus(System.err);
	    throw new IllegalStateException("startLookForPartsFuture=" + startLookForPartsFutureFinal);
	}
	final XFuture<Boolean> lastStartActionsFutureFinal = lastStartActionsFuture;
	if (null != lastStartActionsFutureFinal
		&& !lastStartActionsFutureFinal.isDone()
		&& !lastStartActionsFutureFinal.isCompletedExceptionally()
		&& !lastStartActionsFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("startActionsComment = " + startActionsComment);
	    System.err.println("startActionsTrace=" + Utils.traceToString(startActionsTrace));
	    System.err.println("startActionsInternalTrace=" + Utils.traceToString(startActionsInternalTrace));
	    lastStartActionsFutureFinal.printStatus(System.err);
	    throw new IllegalStateException("lastStartActionsFuture=" + lastStartActionsFutureFinal);
	}
	final XFuture<Boolean> lastContinueActionListFutureFinal = lastContinueActionListFuture;
	if (null != lastContinueActionListFutureFinal
		&& !lastContinueActionListFutureFinal.isDone()
		&& !lastContinueActionListFutureFinal.isCompletedExceptionally()
		&& !lastContinueActionListFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("continueActionsComment = " + continueActionsComment);
	    System.err.println("lastContinueActionListFutureComment=" + lastContinueActionListFutureComment);
	    System.err.println("continueActionListTrace=" + Utils.traceToString(continueActionListTrace));
	    lastContinueActionListFutureFinal.printStatus(System.err);
	    throw new IllegalStateException("lastContinueActionListFuture=" + lastContinueActionListFutureFinal);
	}
	if (null != lastPrivateContinueActionListFutureFinal
		&& !lastPrivateContinueActionListFutureFinal.isDone()
		&& !lastPrivateContinueActionListFutureFinal.isCompletedExceptionally()
		&& !lastPrivateContinueActionListFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("lastContinueActionListFutureComment=" + lastContinueActionListFutureComment);
	    System.err.println("privateContinueActionListTrace=" + Utils.traceToString(privateContinueActionListTrace));
	    lastPrivateContinueActionListFutureFinal.printStatus(System.err);
	    throw new IllegalStateException(
		    "lastPrivateContinueActionListFuture=" + lastPrivateContinueActionListFutureFinal);
	}
	final XFutureVoid safeAbortAndDisconnectFutureFinal = safeAbortAndDisconnectFuture;
	if (null != safeAbortAndDisconnectFutureFinal
		&& !safeAbortAndDisconnectFutureFinal.isDone()
		&& !safeAbortAndDisconnectFutureFinal.isCompletedExceptionally()
		&& !safeAbortAndDisconnectFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("startSafeAbortAndDisconnectComment = " + startSafeAbortAndDisconnectComment);
	    System.err.println("startSafeAbortAndDisconnectThread = " + startSafeAbortAndDisconnectThread);
	    System.err.println("startSafeAbortAndDisconnectTime = " + startSafeAbortAndDisconnectTime);
	    System.err.println("(System.currentTimeMillis()-startSafeAbortAndDisconnectTime) = "
		    + (System.currentTimeMillis() - startSafeAbortAndDisconnectTime));
	    println("startSafeAbortAndDisconnectStackTrace = "
		    + Utils.traceToString(startSafeAbortAndDisconnectStackTrace));
	    System.err.println("safeAbortAndDisconnectFuture=" + safeAbortAndDisconnectFutureFinal);
	    System.err.println("privateContinueActionListTrace=" + Utils.traceToString(privateContinueActionListTrace));
	    safeAbortAndDisconnectFutureFinal.printStatus(System.err);
	    throw new IllegalStateException("safeAbortAndDisconnectFuture=" + safeAbortAndDisconnectFutureFinal);
	}
	final XFutureVoid safeAbortFutureFinal = safeAbortFuture;

	if (null != safeAbortFutureFinal
		&& !safeAbortFutureFinal.isDone()
		&& !safeAbortFutureFinal.isCompletedExceptionally()
		&& !safeAbortFutureFinal.isCancelled()) {
	    debugDumpStack();
	    System.err.println("isDoingActions() = " + isDoingActions());
	    System.err.println("isRunningCrclProgram() = " + isRunningCrclProgram());
	    System.err.println("isAborting()() = " + isAborting());
	    System.err.println("startSafeAbortComment = " + startSafeAbortComment);
	    System.err.println("startSafeAbortThread = " + startSafeAbortThread);
	    System.err.println("startSafeAbortTime = " + startSafeAbortTime);
	    System.err.println("(System.currentTimeMillis()-startSafeAbortTime) = "
		    + (System.currentTimeMillis() - startSafeAbortTime));
	    println("startSafeAbortStackTrace = " + Utils.traceToString(startSafeAbortStackTrace));
	    System.err.println("safeAbortFuture=" + safeAbortFutureFinal);
	    System.err.println("privateContinueActionListTrace=" + Utils.traceToString(privateContinueActionListTrace));
	    safeAbortFutureFinal.printStatus(System.err);
	    throw new IllegalStateException("safeAbortFuture=" + safeAbortFutureFinal);
	}
	if (isAborting()) {
	    System.err.println("safeAbortFuture = " + safeAbortFutureFinal);
	    if (null != safeAbortFutureFinal) {
		safeAbortFutureFinal.printStatus(System.err);
	    }
	    System.err.println("safeAbortAndDisconnectFuture = " + safeAbortAndDisconnectFutureFinal);
	    if (null != safeAbortAndDisconnectFutureFinal) {
		safeAbortAndDisconnectFutureFinal.printStatus(System.err);
	    }
	    String msg = "startActions called with safeAbortFuture= " + safeAbortFutureFinal
		    + ",safeAbortAndDisconnectFuture=" + safeAbortAndDisconnectFutureFinal;
	    setTitleErrorString(msg);
	    throw new IllegalStateException(msg);
	}
	safeAbortFuture = null;
	safeAbortAndDisconnectFuture = null;
	lastPrivateStartActionsFuture = null;
	lastContinueActionListFuture = null;
	lastPrivateContinueActionListFuture = null;
	lastStartActionsFuture = null;
	startLookForPartsFuture = null;
	startSafeAbortStackTrace = null;
	startSafeAbortAndDisconnectStackTrace = null;
	startActionsInternalTrace = null;
	privateStartActionsTrace = null;
	privateContinueActionListTrace = null;
	continueActionListTrace = null;
	startActionsTrace = null;
	safeAbortFuture = null;
	startLookForPartsTrace = null;
	safeAbortAndDisconnectFuture = null;
	startSafeAbortComment = null;
	startSafeAbortAndDisconnectComment = null;
	continueActionsComment = null;
	privateStartActionsComment = null;
	startActionsComment = null;
	startSafeAbortThread = null;
	startSafeAbortAndDisconnectThread = null;
    }

    private volatile StackTraceElement privateStartActionsTrace @Nullable [] = null;
    private volatile @Nullable String privateStartActionsComment = null;
    private final AtomicInteger privateStartActionsCount = new AtomicInteger();

    public String getPrivateStartActionsTraceString() {
	return Utils.traceToString(privateStartActionsTrace);
    }

    public @Nullable String getPrivateStartActionsCommentString() {
	return privateStartActionsComment;
    }

    public String getPrivateContinueActionsTraceString() {
	return Utils.traceToString(privateContinueActionListTrace);
    }

    public @Nullable String getPrivateContinueActionsCommentString() {
	return lastContinueActionListFutureComment;
    }

    private volatile boolean runningPrivateStartActions = false;

    private synchronized XFuture<Boolean> privateStartActions(String comment, boolean reverseFlag,
	    @Nullable List<Action> actionsToLoad) {
	StackTraceElement trace1[] = Thread.currentThread().getStackTrace();
	final ExecutorJInternalFrame pddlExecutorJInternalFrame1Final = executorJInternalFrame1;
	if (null == pddlExecutorJInternalFrame1Final) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	final CrclSwingClientJInternalFrame crclClientJInternalFrameFinal = crclClientJInternalFrame;
	if (null == crclClientJInternalFrameFinal) {
	    throw new IllegalStateException("CRCL Client View must be open to use this function.");
	}
	String executorReadyString = pddlExecutorJInternalFrame1Final.readyForNewActionsListInfoString();
	if (!pddlExecutorJInternalFrame1Final.readyForNewActionsList()) {
	    logEvent("executorReadyString", executorReadyString);
	    throw new IllegalStateException(
		    "!pddlExecutorJInternalFrame1.readyForNewActionsList() with comment=\"" + comment + "\" ");
	}
	if (!isConnected()) {
	    throw new IllegalStateException("!isConnected() : robotName=" + this.robotName + ",host="
		    + crclClientJInternalFrameFinal.getHost() + ", port=" + crclClientJInternalFrameFinal.getPort());
	}
	runningPrivateStartActions = true;
	privateStartActionsTrace = trace1;
	privateStartActionsComment = comment;

	setStartRunTime();

	long startRunNumber = runNumber.incrementAndGet();
	clearLogDirCache();
	startActionsStartComments.add(comment + ",startRunNumber=" + startRunNumber + ",runNumber=" + runNumber);
	int startAbortCount = getSafeAbortRequestCount();
	lastContinueStartAbortCount = startAbortCount;
	boolean reloadSimFiles = isReloadSimFilesOnReverseCheckBoxSelected();
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.refresh(reloadSimFiles);
	}
	int psc = privateStartActionsCount.incrementAndGet();
	if (null != actionsToLoad) {
	    logEvent("START privateStartActions", comment, reverseFlag, actionsToLoad, psc);
	} else {
	    logEvent("START privateStartActions (actionsToLoad=null)", comment, reverseFlag, psc);
	}
	takeSnapshots("START privateStartActions " + psc + " " + comment);

	logToSuper("START privateStartActions \"" + getTaskName() + "\" " + psc + ",startAbortCount=" + startAbortCount
		+ "\"" + toString() + "\" " + comment);
	endLogged = false;
	XFuture<Boolean> ret = XFuture.runAsync("AprsSystem.startActions",
		() -> {
		    setThreadName();
		    takeSnapshots("startActions." + comment);
		}, runProgramService)
		.thenCompose("startActions.pauseCheck.comment=" + comment + ", startRunNumber" + startRunNumber,
			x -> waitForPause())
		.thenApplyAsync(taskName, x -> {
		    try {
			return startActionsInternal(comment, startRunNumber, startAbortCount, reverseFlag,
				reloadSimFiles, actionsToLoad);
		    } catch (Exception exception) {
			Logger.getLogger(AprsSystem.class
				.getName()).log(Level.SEVERE, "", exception);
			System.err.println("privateStartActions : comment=" + comment);
			System.err.println("privateStartActions : trace1=" + Utils.traceToString(trace1));
			setTitleErrorString(exception.getMessage());
			showException(exception);
			if (exception instanceof RuntimeException) {
			    throw (RuntimeException) exception;
			} else {
			    throw new RuntimeException(exception);
			}
		    }
		}, runProgramService)
		.alwaysRunAsync(() -> {
		    synchronized (this) {
			setEndLogged(trace1, comment);
			boolean ready = pddlExecutorJInternalFrame1Final.readyForNewActionsList();
			logEvent("END privateStartActions", comment, reverseFlag, ready, psc);
			takeSnapshots("END privateStartActions " + psc + " " + comment);
			String newExecutorReadyString = pddlExecutorJInternalFrame1Final
				.readyForNewActionsListInfoString();
			logToSuper("END privateStartActions \"" + getTaskName() + "\" " + psc + ",startAbortCount="
				+ startAbortCount + ",currentAbortCount=" + getSafeAbortRequestCount()
				+ ", readyForNewActionsList=" + ready + "\"" + toString() + "\" " + comment);
			if (!ready) {
			    logEvent("newExecutorReadyString", newExecutorReadyString);
			}
			runningPrivateStartActions = false;
		    }
		}, runProgramService);
	lastPrivateStartActionsFuture = ret;
	return ret;
    }

    private volatile StackTraceElement startActionsInternalTrace @Nullable [] = null;

    private boolean startActionsInternal(String comment,
	    long startRunNumber,
	    int startAbortCount,
	    boolean newReverseFlag,
	    boolean reloadSimFiles,
	    @Nullable List<Action> actionsToLoad) throws IllegalStateException {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	long currentRunNumber = runNumber.get();
	if (currentRunNumber != startRunNumber) {
	    throw new IllegalStateException("runNumbeChanged");
	}
	StackTraceElement trace[] = Thread.currentThread().getStackTrace();
	startActionsInternalTrace = trace;
	if (currentRunNumber != startRunNumber
		|| getSafeAbortRequestCount() != startAbortCount) {
	    return false;
	}
	this.setReverseFlag(newReverseFlag, reloadSimFiles, false);
	List<Action> reloadedActions = null;
	boolean ret;
	try {
	    updateRobotLimits();
	    if (null != actionsToLoad) {
		File f = createTempFile("actionsList_" + comment, ".txt");
		try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
		    for (Action action : actionsToLoad) {
			pw.println(action.asPddlLine());
		    }
		}
		executorJInternalFrame1.loadActionsFile(
			f,
			false,
			newReverseFlag,
			false);
	    } else {
		reloadedActions = executorJInternalFrame1.reloadActionsFile(newReverseFlag);
	    }
	    ret = executorJInternalFrame1.doActions("startActions." + comment + ", startRunNumber" + startRunNumber,
		    startAbortCount, trace);
	    startActionsFinishComments
		    .add(comment + ",startRunNumber=" + startRunNumber + ",runNumber=" + currentRunNumber);
	    if (currentRunNumber != startRunNumber) {
		System.err.println("startActionsStartComments=" + startActionsStartComments);
		System.err.println("startActionsFinishComments=" + startActionsFinishComments);
		System.err.println("reloadedActions = " + reloadedActions);
		System.err.println("actionsToLoad = " + actionsToLoad);
		System.err.println("newReverseFlag = " + newReverseFlag);
		throw new IllegalStateException("runNumbeChanged from " + startRunNumber + " to " + currentRunNumber);
	    }
	} catch (Exception ex) {

	    System.err.println("reloadedActions = " + reloadedActions);
	    System.err.println("actionsToLoad = " + actionsToLoad);
	    System.err.println("newReverseFlag = " + newReverseFlag);
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    setTitleErrorString(ex.getMessage());
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}
	String tes = this.getTitleErrorString();
	if (null != tes && tes.length() > 1) {
	    throw new RuntimeException(tes);
	}
	return ret;
    }

    private void checkReadyToRun() throws IllegalStateException {
	if (null == robotName || robotName.length() < 1) {
	    String msg = "startActions called with robotName= " + robotName;
	    setTitleErrorString(msg);
	    throw new IllegalStateException(msg);
	}
	if (isAborting()) {
	    System.err.println("safeAbortFuture = " + safeAbortFuture);
	    if (null != safeAbortFuture) {
		safeAbortFuture.printStatus(System.err);
	    }
	    System.err.println("safeAbortAndDisconnectFuture = " + safeAbortAndDisconnectFuture);
	    if (null != safeAbortAndDisconnectFuture) {
		safeAbortAndDisconnectFuture.printStatus(System.err);
	    }
	    String msg = "startActions called with safeAbortFuture= " + safeAbortFuture
		    + ",safeAbortAndDisconnectFuture=" + safeAbortAndDisconnectFuture;
	    setTitleErrorString(msg);
	    throw new IllegalStateException(msg);
	}
    }

    private volatile StackTraceElement lastIsDoingActionsTrueTrace @Nullable [] = null;

    /**
     * Get the state of whether the PDDL executor is currently doing actions.
     *
     * @return current state
     */
    public synchronized boolean isDoingActions() {
	if (doingLookForParts || runningPrivateContinueActionList || runningPrivateStartActions) {
	    lastIsDoingActionsTrueTrace = Thread.currentThread().getStackTrace();
	    return true;
	} else if (null != lastPrivateStartActionsFuture
		&& !lastPrivateStartActionsFuture.isDone()
		&& !lastPrivateStartActionsFuture.isCompletedExceptionally()
		&& !lastPrivateStartActionsFuture.isCancelled()) {
	    lastIsDoingActionsTrueTrace = Thread.currentThread().getStackTrace();
	    return true;
	} else if (null != startLookForPartsFuture
		&& !startLookForPartsFuture.isDone()
		&& !startLookForPartsFuture.isCompletedExceptionally()
		&& !startLookForPartsFuture.isCancelled()) {
	    lastIsDoingActionsTrueTrace = Thread.currentThread().getStackTrace();
	    return true;
	} else if (null != lastPrivateContinueActionListFuture
		&& !lastPrivateContinueActionListFuture.isDone()
		&& !lastPrivateContinueActionListFuture.isCompletedExceptionally()
		&& !lastPrivateContinueActionListFuture.isCancelled()) {
	    lastIsDoingActionsTrueTrace = Thread.currentThread().getStackTrace();
	    return true;
	} else if (null == executorJInternalFrame1) {
	    lastIsDoingActionsTrueTrace = null;
	    return false;
	} else if (executorJInternalFrame1.isDoingActions()) {
	    lastIsDoingActionsTrueTrace = Thread.currentThread().getStackTrace();
	    return true;
	} else {
	    lastIsDoingActionsTrueTrace = null;
	    return false;
	}
    }

    public @Nullable String getIsDoingActionsInfo() {
	if (null == executorJInternalFrame1) {
	    throw new NullPointerException("pddlExecutorJInternalFrame1");
	}
	String lastPrivateStartActionsFutureInfoString;
	if (lastPrivateStartActionsFuture != null) {
	    lastPrivateStartActionsFutureInfoString = "lastPrivateStartActionsFuture=" + lastPrivateStartActionsFuture
		    + ",\n";
	} else {
	    lastPrivateStartActionsFutureInfoString = "";
	}

	String startLookForPartsFutureInfoString;
	if (startLookForPartsFuture != null) {
	    startLookForPartsFutureInfoString = "startLookForPartsFuture=" + startLookForPartsFuture + ",\n";
	} else {
	    startLookForPartsFutureInfoString = "";
	}
	String lastPrivateContinueActionListFutureInfoString;
	if (lastPrivateContinueActionListFuture != null) {
	    lastPrivateContinueActionListFutureInfoString = "lastPrivateContinueActionListFuture="
		    + lastPrivateContinueActionListFuture + ",\n";
	} else {
	    lastPrivateContinueActionListFutureInfoString = "";
	}

	return lastPrivateStartActionsFutureInfoString
		+ startLookForPartsFutureInfoString
		+ lastPrivateContinueActionListFutureInfoString
		+ "doingLookForParts=" + doingLookForParts + ",\n"
		+ "runningPrivateContinueActionList=" + runningPrivateContinueActionList + ",\n"
		+ "runningPrivateStartActions=" + runningPrivateStartActions + ",\n"
		+ "lastIsDoingActionsTrueTrace=" + Utils.traceToString(lastIsDoingActionsTrueTrace) + "\n"
		+ "pddlExecutorJInternalFrame1.getIsDoingActionsInfo()="
		+ executorJInternalFrame1.getIsDoingActionsInfo();
    }

    public XFutureVoid startExploreGraphDb() {
	return runOnDispatchThread(this::startExploreGraphDbOnDisplay);
    }

    public XFutureVoid startForceTorqueSim() {
	return runOnDispatchThread(this::startForceTorqueSimOnDisplay);
    }

    @UIEffect
    private void startForceTorqueSimOnDisplay() {

	try {
	    boolean alreadySelected = isForceTorqueSimStartupSelected();
	    if (!alreadySelected) {
		setForceTorqueSimStartupSelected(true);
	    }
	    if (null == this.forceTorqueSimJInternalFrame) {
		ForceTorqueSimJInternalFrame newForceTorqueSimJInternalFrame = new ForceTorqueSimJInternalFrame();
		this.forceTorqueSimJInternalFrame = newForceTorqueSimJInternalFrame;
		newForceTorqueSimJInternalFrame.setPropertiesFile(forceTorqueSimPropertiesFile());
		newForceTorqueSimJInternalFrame.loadProperties();
		this.addInternalFrame(newForceTorqueSimJInternalFrame);
		if (null != this.crclClientJInternalFrame) {
		    newForceTorqueSimJInternalFrame
			    .setCrclClientPanel(crclClientJInternalFrame.getPendantClientJPanel1());
		}
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    @UIEffect
    private void startExploreGraphDbOnDisplay() {
	if (null == dbSetupJInternalFrame) {
	    throw new IllegalStateException("DB Setup View must be open to use this function.");
	}
	try {
	    if (null == this.exploreGraphDbJInternalFrame) {
		this.exploreGraphDbJInternalFrame = new ExploreGraphDbJInternalFrame();
		this.exploreGraphDbJInternalFrame.setAprsSystem(this);
		DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
		dbSetupPublisher.addDbSetupListener(exploreGraphDbJInternalFrame);
		exploreGraphDbJInternalFrame.accept(dbSetupPublisher.getDbSetup());
		this.addInternalFrame(exploreGraphDbJInternalFrame);
	    }
//            activateInternalFrame(this.exploreGraphDbJInternalFrame);
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    public void closeForceTorqeSim() {
	try {
	    System.out.println("");
	    System.err.println("");
	    System.out.flush();
	    System.err.flush();
	    Thread.dumpStack();
	    System.out.println("crclClientJInternalFrame = " + this.crclClientJInternalFrame);
	    System.out.println("forceTorqueSimJInternalFrame = " + forceTorqueSimJInternalFrame);
	    System.out.println("");
	    System.err.println("");
	    System.out.flush();
	    System.err.flush();
	    if (null != this.forceTorqueSimJInternalFrame) {
		forceTorqueSimJInternalFrame.setVisible(false);
	    }
	    saveProperties();

	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    public void closeExploreGraphDb() {
	try {
	    if (null != this.exploreGraphDbJInternalFrame) {
		exploreGraphDbJInternalFrame.setVisible(false);
	    }
	    saveProperties();

	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    /**
     * Get the current line in the CRCL program being run.
     *
     * @return current line
     */
    public int getCrclProgramLine() {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("CRCL Client View must be open to use this function.");
	}
	return this.crclClientJInternalFrame.getCurrentProgramLine();
    }

    private @MonotonicNonNull File propertiesFile = null;
    private @MonotonicNonNull File propertiesDirectory = null;

    public void setPropertiesDirectory(File propertiesDirectory) {
	this.propertiesDirectory = propertiesDirectory;
    }

    private static final String APRS_DEFAULT_PROPERTIES_FILENAME = "aprs_properties.txt";
    private @MonotonicNonNull File lastAprsPropertiesFileFile = null;

    public @Nullable File getLastAprsPropertiesFileFile() {
	return lastAprsPropertiesFileFile;
    }

    public void setLastAprsPropertiesFileFile(File lastAprsPropertiesFileFile) {
	this.lastAprsPropertiesFileFile = lastAprsPropertiesFileFile;
    }

    void reloadForReverse(boolean reverseFlag) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	boolean reloadActionsFile = executorJInternalFrame1.getActionsFileString(reverseFlag) != null;
	setReverseFlag(reverseFlag, true, reloadActionsFile);

    }

    private static class AprsSystemPropDefaults {

	private final File propDir;
	private final File propFile;
	private final File lastAprsPropertiesFileFile;

	// private int screenDownX = 0;
//        private int screenDownY = 0;
//        private int screenDragX = 0;
//        private int screenDragY = 0;
//        private boolean mouseDown = false;
//        public boolean isMouseDown() {
//            return mouseDown;
//        }
//
//        public void setMouseDown(boolean mouseDown) {
//            this.mouseDown = mouseDown;
//        }
//
//        public int getScreenDownX() {
//            return screenDownX;
//        }
//
//        public void setScreenDownX(int screenDownX) {
//            this.screenDownX = screenDownX;
//        }
//
//        public int getScreenDownY() {
//            return screenDownY;
//        }
//
//        public void setScreenDownY(int screenDownY) {
//            this.screenDownY = screenDownY;
//        }
//
//        public int getScreenDragX() {
//            return screenDragX;
//        }
//
//        public void setScreenDragX(int screenDragX) {
//            this.screenDragX = screenDragX;
//        }
//
//        public int getScreenDragY() {
//            return screenDragY;
//        }
//
//        public void setScreenDragY(int screenDragY) {
//            this.screenDragY = screenDragY;
//        }
	private static final AprsSystemPropDefaults SINGLE_PROPERTY_DEFAULTS = new AprsSystemPropDefaults();

	private AprsSystemPropDefaults(File propDir, File propFile, File lastAprsPropertiesFileFile) {
	    this.propDir = propDir;
	    this.propFile = propFile;
	    this.lastAprsPropertiesFileFile = lastAprsPropertiesFileFile;
	}

	static AprsSystemPropDefaults getEmptyTemp() throws IOException {
	    File base = File.createTempFile("empty_aprs_props", ".base");
	    File dir = new File(base.getParentFile(), base.getName().replace('.', '_') + "_dir");
	    dir.mkdirs();
	    return new AprsSystemPropDefaults(dir, new File(dir, "empty_aprs_props.txt"),
		    new File(dir, "lastAprsPropertiesFileFile.txt"));
	}

	static AprsSystemPropDefaults getSINGLE_PROPERTY_DEFAULTS() {
	    return SINGLE_PROPERTY_DEFAULTS;
	}

	File getPropDir() {
	    return propDir;
	}

	File getPropFile() {
	    return propFile;
	}

	File getLastAprsPropertiesFileFile() {
	    return lastAprsPropertiesFileFile;
	}

	private AprsSystemPropDefaults() {
	    String aprsPropsDir = System.getProperty("aprs.properties.dir");
	    File tempPropDir;
	    if (null != aprsPropsDir) {
		tempPropDir = new File(aprsPropsDir);
	    } else {
		tempPropDir = new File(Utils.getAprsUserHomeDir(), ".aprs");
	    }
	    lastAprsPropertiesFileFile = new File(tempPropDir, "lastPropsFileName.txt");
	    File tempPropFile = null;
	    if (lastAprsPropertiesFileFile.exists()) {
		try {
		    Properties p = new Properties();
		    p.load(new FileReader(lastAprsPropertiesFileFile));
		    String fname = p.getProperty("lastPropertyFileName");
		    if (fname != null && fname.length() > 0) {
			File f = new File(fname);
			if (f.exists()) {
			    tempPropFile = f;
			    tempPropDir = f.getParentFile();
			}
		    }
		} catch (IOException ex) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, "", ex);
		}
	    }
	    if (null == tempPropFile || !tempPropFile.exists()) {
		if (null != tempPropDir) {
		    tempPropDir.mkdirs();
		}
		String aprsPropsFilename = System.getProperty("aprs.properties.filename",
			APRS_DEFAULT_PROPERTIES_FILENAME);
		tempPropFile = new File(tempPropDir, aprsPropsFilename);
	    }
	    if (null != tempPropDir) {
		this.propDir = tempPropDir;
	    } else {
		this.propDir = new File(Utils.getAprsUserHomeDir());
	    }
	    this.propFile = tempPropFile;
	}
    }

    public static File getDefaultPropertiesDir() {
	return AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS().getPropDir();
    }

    /**
     * Get the default file location for saved properties.
     *
     * @return default file location
     */
    public static File getDefaultPropertiesFile() {
	return AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS().getPropFile();
    }

    /**
     * Get the default file location for a file that will could contain a reference
     * to the last properties file used.
     *
     * @return last file location
     */
    public static File getDefaultLastPropertiesFileFile() {
	return AprsSystemPropDefaults.getSINGLE_PROPERTY_DEFAULTS().getLastAprsPropertiesFileFile();
    }

    private void initPropertiesFileInfo() {
	propertiesDirectory = getDefaultPropertiesDir();
	propertiesFile = getDefaultPropertiesFile();
	lastAprsPropertiesFileFile = getDefaultLastPropertiesFileFile();
    }

    private void initPropertiesFileInfo(AprsSystemPropDefaults propDefaults) {
	if (null == propDefaults) {
	    initPropertiesFileInfo();
	}
	if (null == propDefaults) {
	    throw new NullPointerException("propDefaults");
	}
	propertiesDirectory = propDefaults.getPropDir();
	propertiesFile = propDefaults.getPropFile();
	lastAprsPropertiesFileFile = propDefaults.getLastAprsPropertiesFileFile();
    }

    private int snapShotWidth = 800;
    private int snapShotHeight = 600;

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param f     file to save snapshot image to
     * @param pose  optional pose to mark or null
     * @param label optional label for pose or null
     * @return array of filenames logged first image file, then csv file
     */
    public @Nullable File[] takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(f, pose, label, snapShotWidth, snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param f     file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @return array of filenames logged first image file, then csv file
     *
     */
    public @Nullable File[] takeSimViewSnapshot(File f, PointType point, String label) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(f, point, label, snapShotWidth, snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param f     file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @return array of filenames logged first image file, then csv file
     */
    public @Nullable File[] takeSimViewSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(f, point, label, snapShotWidth, snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param imgLabel  string that will be included in the image file name
     * @param pose      optional pose to mark or null
     * @param poseLabel optional label for pose or null
     * @return array of filenames logged first image file, then csv file
     * @throws IOException if writing the file fails
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel) throws IOException {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(snapshotImageFile(imgLabel), pose, poseLabel, snapShotWidth,
		    snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param imgLabel   string that will be included in the image file name
     * @param pt         optional point to mark or null
     * @param pointLabel optional label for point or null
     * @return array of filenames logged first image file, then csv file
     * @throws IOException if writing the file fails
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, @Nullable PmCartesian pt, @Nullable String pointLabel)
	    throws IOException {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(snapshotImageFile(imgLabel), pt, pointLabel, snapShotWidth,
		    snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param imgLabel   string that will be included in the image file name
     * @param pt         optional point to mark or null
     * @param pointLabel optional label for point or null
     * @return array of filenames logged first image file, then csv file
     *
     * @throws IOException if writing the file fails
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, PointType pt, String pointLabel) throws IOException {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(snapshotImageFile(imgLabel), pt, pointLabel, snapShotWidth,
		    snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f            file to save snapshot image to
     * @param itemsToPaint list of items to paint
     * @return array of filenames logged first image file, then csv file
     *
     */
    public @Nullable File[] takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
	checkPhysicalItemCollectionNames(itemsToPaint);
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, snapShotWidth, snapShotHeight);
	} else {
	    return new File[2];
	}
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param imgLabel     string that will be added to the filename along with
     *                     timestamp and extention
     * @param itemsToPaint list of items to paint
     * @return array of filenames logged first image file, then csv file
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel,
	    @Nullable Collection<? extends PhysicalItem> itemsToPaint) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    if (null == itemsToPaint) {
		final File imgFile;
		try {
		    imgFile = snapshotImageFile("nullItems_" + imgLabel);
		} catch (IOException ex) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, null, ex);
		    return new File[2];
		}
		return this.object2DViewJInternalFrame.takeSnapshot(imgFile, itemsToPaint, snapShotWidth,
			snapShotHeight);
	    } else if (itemsToPaint.isEmpty()) {
		final File imgFile;
		try {
		    imgFile = snapshotImageFile("emptyItems_" + imgLabel);
		} catch (IOException ex) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, null, ex);
		    return new File[2];
		}
		return this.object2DViewJInternalFrame.takeSnapshot(imgFile, itemsToPaint, snapShotWidth,
			snapShotHeight);
	    } else {
		final File imgFile;
		try {
		    imgFile = snapshotImageFile(imgLabel);
		} catch (IOException ex) {
		    Logger.getLogger(AprsSystem.class
			    .getName()).log(Level.SEVERE, null, ex);
		    return new File[2];
		}
		return this.object2DViewJInternalFrame.takeSnapshot(imgFile, itemsToPaint, snapShotWidth,
			snapShotHeight);
	    }
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param f     file to save snapshot image to
     * @param pose  optional pose to mark or null
     * @param label optional label for pose or null
     * @param w     width of snapshot image
     * @param h     height of snapshot image
     * @return array of filenames logged first image file, then csv file
     *
     */
    public @Nullable File[] takeSimViewSnapshot(File f, PoseType pose, String label, int w, int h) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(f, pose, label, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param f     file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @param w     width of snapshot image
     * @param h     height of snapshot image
     * @return array of filenames logged first image file, then csv file
     *
     */
    public @Nullable File[] takeSimViewSnapshot(File f, PointType point, String label, int w, int h) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(f, point, label, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param f     file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @param w     width of snapshot image
     * @param h     height of snapshot image
     * @return array of filenames logged first image file, then csv file
     *
     */
    public @Nullable File[] takeSimViewSnapshot(File f, PmCartesian point, String label, int w, int h) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    return object2DViewJInternalFrame.takeSnapshot(f, point, label, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param imgLabel  label to included in filename
     * @param pose      optional pose to mark or null
     * @param poseLabel optional label for pose or null
     * @param w         width of snapshot image
     * @param h         height of snapshot image
     * @return array of filenames logged first image file, then csv file
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel, int w, int h) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    final File imgFile;
	    try {
		imgFile = snapshotImageFile(imgLabel);
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
		return new File[2];
	    }
	    return object2DViewJInternalFrame.takeSnapshot(imgFile, pose, poseLabel, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param imgLabel   label to included in filename
     * @param pt         optional point to mark or null
     * @param pointLabel optional label for point or null
     * @param w          width of snapshot image
     * @param h          height of snapshot image
     * @return array of filenames logged first image file, then csv file
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, PmCartesian pt, String pointLabel, int w, int h) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    final File imgFile;
	    try {
		imgFile = snapshotImageFile(imgLabel);
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
		return new File[2];
	    }
	    return object2DViewJInternalFrame.takeSnapshot(imgFile, pt, pointLabel, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the specified
     * file, optionally highlighting a pose with a label.
     *
     * @param imgLabel   label to included in filename
     * @param pt         optional point to mark or null
     * @param pointLabel optional label for point or null
     * @param w          width of snapshot image
     * @param h          height of snapshot image
     * @return array of filenames logged first image file, then csv file
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, PointType pt, String pointLabel, int w, int h) {
	if (null != object2DViewJInternalFrame && isSnapshotsSelected()) {
	    final File imgFile;
	    try {
		imgFile = snapshotImageFile(imgLabel);
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, null, ex);
		return new File[2];
	    }
	    return object2DViewJInternalFrame.takeSnapshot(imgFile, pt, pointLabel, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list. NOTE:
     * Files may not be created if the Object View is not initialized or the IO
     * thread is busy.
     *
     * @param f            file to save snapshot image to
     * @param itemsToPaint list of items to paint
     * @param w            width of snapshot image
     * @param h            height of snapshot image
     * @return array of files if they were created.
     */
    public @Nullable File[] takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) {
	checkPhysicalItemCollectionNames(itemsToPaint);
	if (null != object2DViewJInternalFrame) {
	    return this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, w, h);
	} else {
	    return new File[2];
	}
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param imgLabel     string that will be added to the filename along with
     *                     timestamp and extention
     * @param itemsToPaint list of items to paint
     * @param w            width of image to create
     * @param h            height of image to create
     * @return array of filenames logged first image file, then csv file
     *
     * @throws java.io.IOException problem writing to the file
     */
    public @Nullable File[] takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint, int w,
	    int h) throws IOException {
	checkPhysicalItemCollectionNames(itemsToPaint);
	if (null != object2DViewJInternalFrame) {
	    return this.object2DViewJInternalFrame.takeSnapshot(snapshotImageFile(imgLabel), itemsToPaint, w, h);
	} else {
	    return new File[2];
	}
    }

    private void checkPhysicalItemCollectionNames(Collection<? extends PhysicalItem> itemsToPaint)
	    throws RuntimeException {
	for (PhysicalItem pi : itemsToPaint) {
	    if (pi.getName().contains("_in_pt_in_pt")) {
		throw new RuntimeException("bad name for item in collection : " + pi);
	    }
	    if (pi.getFullName().contains("_in_pt_in_pt")) {
		throw new RuntimeException("bad name for item in collection : " + pi);
	    }
	    if (pi.getName().contains("_in_kt_in_kt")) {
		throw new RuntimeException("bad name for item in collection : " + pi);
	    }
	    if (pi.getFullName().contains("_in_kt_in_kt")) {
		throw new RuntimeException("bad name for item in collection : " + pi);
	    }
	}
    }

    private @MonotonicNonNull DbSetup dbSetup = null;

    public final XFutureVoid loadProperties() throws IOException {
	if (null == propertiesFile) {
	    throw new NullPointerException("propertiesFile");
	}
	if (!propertiesFile.exists()) {
	    throw new IllegalStateException("File " + propertiesFile + " does not exist");
	}
	newPropertiesFile = false;
	IOException exA[] = new IOException[1];
	Properties props = new Properties();
	newPropertiesFile = false;
	println("AprsSystem loading properties from " + propertiesFile.getCanonicalPath());
	try (FileReader fr = new FileReader(propertiesFile)) {
	    props.load(fr);
	}
	try {
	    Utils.SwingFuture<XFutureVoid> ret = Utils.supplyOnDispatchThread(
		    () -> {
			return loadPropertiesOnDisplay(exA, props);
		    });
	    if (null != exA[0]) {
		throw new IOException(exA[0]);
	    }
	    return ret.thenComposeToVoid(x -> {
		if (null != exA[0]) {
		    throw new RuntimeException(exA[0]);
		}
		if (null != object2DViewJInternalFrame) {
		    object2DViewJInternalFrame.addPublishCountListener(simPublishCountListener);
		}
		if (null != visionToDbJInternalFrame) {
		    visionToDbJInternalFrame.addLineCountListener(visionLineListener);
		}
		return x;
	    });
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(ex);
	}
    }

    private final List<Long> publishTimes = new ArrayList<>();

    synchronized private void setSimPublishCount(int count) {
	this.simLineCount = count;
	simVisionDiff = count - visionLineCount;
	publishTimes.add(System.currentTimeMillis());
	while (publishTimes.size() > 100) {
	    publishTimes.remove(0);
	}
	if (simVisionDiff > maxSimVisionDiff) {
	    maxSimVisionDiff = simVisionDiff;
	    println("maxSimVisionDiff = " + maxSimVisionDiff);
	}
    }

    private final Consumer<Integer> simPublishCountListener;

    private static void propertyToCheckBox(Properties props, String propertyName, CachedCheckBox checkbox) {
	String valueString = props.getProperty(propertyName);
	if (null != valueString && valueString.trim().length() > 0) {
	    checkbox.setSelected(Boolean.parseBoolean(valueString));
	}
    }

    public void setPauseInsteadOfRecover(boolean val) {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.setPauseInsteadOfRecover(val);
	}
    }

    private @Nullable File customWindowsFile = null;

    public @Nullable File getCustomWindowsFile() {
	return customWindowsFile;
    }

    public void setCustomWindowsFile(File customWindowsFile) {
	this.customWindowsFile = customWindowsFile;
    }

    private XFutureVoid loadPropertiesOnDisplay(IOException exA[], Properties props) {

	try {
	    if (null == propertiesFile) {
		throw new NullPointerException("propertiesFile");
	    }
	    List<XFuture<?>> futures = new ArrayList<>();

	    if (null != this.executorJInternalFrame1) {
		String alertLimitsString = props.getProperty(ALERT_LIMITS);
		if (null != alertLimitsString) {
		    setAlertLimitsCheckBoxSelected(Boolean.parseBoolean(alertLimitsString));
		}
	    }
	    String useTeachTableString = props.getProperty(USETEACHTABLE);
	    if (null != useTeachTableString) {
		setUseTeachTable(Boolean.parseBoolean(useTeachTableString));
	    }

	    String startExecutorString = props.getProperty(STARTUPPDDLEXECUTOR);
	    if (null != startExecutorString) {
		setExecutorStartupSelected(Boolean.parseBoolean(startExecutorString));
	    }
	    String startObjectSpString = props.getProperty(STARTUPPDDLOBJECTSP);
	    if (null != startObjectSpString) {
		setObjectSpStartupSelected(Boolean.parseBoolean(startObjectSpString));
	    }

	    String startObjectViewString = props.getProperty(STARTUPPDDLOBJECTVIEW);
	    if (null != startObjectViewString) {
		setObject2DViewStartupSelected(Boolean.parseBoolean(startObjectViewString));
	    }
	    String startCRCLClientString = props.getProperty(STARTUPROBOTCRCLCLIENT);
	    if (null != startCRCLClientString) {
		setRobotCrclGUIStartupSelected(Boolean.parseBoolean(startCRCLClientString));
	    }
	    String startForceTorqueSimString = props.getProperty(STARTUPFORCETORQUESIM);
	    if (null != startForceTorqueSimString) {
		setForceTorqueSimStartupSelected(Boolean.parseBoolean(startForceTorqueSimString));
	    }
	    String startCRCLSimServerString = props.getProperty(STARTUPROBOTCRCLSIMSERVER);
	    if (null != startCRCLSimServerString) {
		setRobotCrclSimServerStartupSelected(Boolean.parseBoolean(startCRCLSimServerString));
	    }
	    String startCRCLFanucServerString = props.getProperty(STARTUPROBOTCRCLFANUCSERVER);
	    if (null != startCRCLFanucServerString) {
		setRobotCrclFanucServerStartupSelected(Boolean.parseBoolean(startCRCLFanucServerString));
	    }
	    String fanucCrclLocalPortString = props.getProperty(FANUC_CRCL_LOCAL_PORT);
	    if (null != fanucCrclLocalPortString) {
		this.fanucCrclPort = Integer.parseInt(fanucCrclLocalPortString);
	    }
	    String fanucRobotHostString = props.getProperty(FANUC_ROBOT_HOST);
	    if (null != fanucRobotHostString) {
		this.fanucRobotHost = fanucRobotHostString;
	    }

	    String startCRCLMotomanServerString = props.getProperty(STARTUPROBOTCRCLMOTOMANSERVER);
	    if (null != startCRCLMotomanServerString) {
		setRobotCrclMotomanServerStartupSelected(Boolean.parseBoolean(startCRCLMotomanServerString));
	    }
	    String startConnectDBString = props.getProperty(STARTUPCONNECTDATABASE);
	    if (null != startConnectDBString) {
		setConnectDatabaseOnStartupSelected(Boolean.parseBoolean(startConnectDBString));
		if (isConnectDatabaseOnSetupStartupSelected()) {
		    setShowDatabaseSetupStartupSelected(true);
		}
	    }
	    String startConnectVisionString = props.getProperty(STARTUPCONNECTVISION);
	    if (null != startConnectVisionString) {
		setConnectVisionOnStartupSelected(Boolean.parseBoolean(startConnectVisionString));
		if (isConnectVisionOnStartupSelected()) {
		    setObjectSpStartupSelected(true);
		}
	    }
	    String startExploreGraphDbString = props.getProperty(STARTUPEXPLOREGRAPHDB);
	    if (null != startExploreGraphDbString) {
		setExploreGraphDBStartupSelected(Boolean.parseBoolean(startExploreGraphDbString));
	    }

	    String customWindowsFileString = props.getProperty(CUSTOM_WINDOWS_FILE);
	    if (null != customWindowsFileString) {
		setCustomWindowsFile(new File(propertiesDirectory, customWindowsFileString));
	    }
	    String crclWebAppPortString = props.getProperty(CRCLWEBAPPPORT);
	    if (null != crclWebAppPortString) {
		crclWebServerHttpPort = Integer.parseInt(crclWebAppPortString);
	    }

	    String startKitInspetion = props.getProperty(STARTUPKITINSPECTION);
	    if (null != startKitInspetion) {
		setKitInspectionStartupSelected(Boolean.parseBoolean(startKitInspetion));
	    }
	    String useCsvFilesInsteadOfDatabaseString = props.getProperty(USE_CSV_FILES_INSTEAD_OF_DATABASE);
	    if (null != useCsvFilesInsteadOfDatabaseString) {
		this.setUseCsvFilesInsteadOfDatabase(Boolean.parseBoolean(useCsvFilesInsteadOfDatabaseString));
	    }
	    this.updateSubPropertiesFiles();
//            if (null != this.pddlPlannerJInternalFrame) {
//                this.pddlPlannerJInternalFrame.loadProperties();
//            }
	    if (null != this.executorJInternalFrame1) {
		XFutureVoid loadPropertiesFuture = XFuture.runAsync("loadProperties",
			() -> {
			    try {
				if (null != this.executorJInternalFrame1) {
				    this.executorJInternalFrame1.loadProperties();
				}
			    } catch (IOException ex) {
				Logger.getLogger(AprsSystem.class
					.getName()).log(Level.SEVERE, "", ex);
			    }
			}, runProgramService)
			.thenComposeToVoid(() -> {
			    String alertLimitsString = props.getProperty(ALERT_LIMITS);
			    if (null != alertLimitsString) {
				setAlertLimitsCheckBoxSelected(Boolean.parseBoolean(alertLimitsString));
			    }
			    return syncPauseRecoverCheckbox();
			});
		futures.add(loadPropertiesFuture);
	    }

	    if (null != this.object2DViewJInternalFrame) {
		this.object2DViewJInternalFrame.loadProperties();
	    }
	    dbSetup = DbSetupBuilder
		    .loadFromPropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt"))
		    .build();

	    if (null != dbSetupJInternalFrame && null != dbSetup) {
		DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
		if (null != pub) {
		    pub.setDbSetup(dbSetup);
		}
	    }
	    if (null != visionToDbJInternalFrame) {
		this.visionToDbJInternalFrame.loadProperties();
	    }
	    if (null != object2DViewJInternalFrame) {
		this.object2DViewJInternalFrame.loadProperties();
	    }
	    if (null != this.crclClientJInternalFrame) {
		crclClientJInternalFrame.loadProperties();
	    }
	    if (null != this.simServerJInternalFrame) {
		simServerJInternalFrame.loadProperties();
	    }
	    if (null != this.motomanServerProvider) {
		motomanServerProvider.loadProperties();
	    }
	    if (null != this.fanucServerProvider) {
		fanucServerProvider.loadProperties();
	    }
	    if (null != this.forceTorqueSimJInternalFrame) {
		forceTorqueSimJInternalFrame.loadProperties();
	    }
	    String motomanCrclLocalPortString = props.getProperty(MOTOMAN_CRCL_LOCAL_PORT);
	    if (null != motomanCrclLocalPortString) {
		this.motomanCrclPort = Integer.parseInt(motomanCrclLocalPortString);
		if (null != motomanServerProvider) {
		    motomanServerProvider.setCrclPort(motomanCrclPort);
		}
	    }
	    String robotNameString = props.getProperty(APRSROBOT_PROPERTY_NAME);
	    if (null != robotNameString) {
		setRobotName(robotNameString);
	    } else {
		setDefaultRobotName();
	    }
	    File limitsCsv = getCartLimitsCsvFile();
	    if (limitsCsv.exists()) {
		readLimitsFromCsv(limitsCsv);
		updateRobotLimits();
	    }

	    String reloadSimFilesOnReverseString = props.getProperty(RELOAD_SIM_FILES_ON_REVERSE_PROP);
	    if (null != reloadSimFilesOnReverseString && reloadSimFilesOnReverseString.trim().length() > 0) {
		setReloadSimFilesOnReverseCheckBoxSelected(Boolean.parseBoolean(reloadSimFilesOnReverseString));
	    }
	    String logCrclProgramsEnabledString = props.getProperty(LOG_CRCL_PROGRAMS_ENABLED);
	    if (null != logCrclProgramsEnabledString && logCrclProgramsEnabledString.trim().length() > 0) {
		setLogCrclProgramsSelected(Boolean.parseBoolean(logCrclProgramsEnabledString));
	    }
	    propertyToCheckBox(props, SNAP_SHOT_ENABLE_PROP, snapshotsCheckBox);
	    String snapShotWidthString = props.getProperty(SNAP_SHOT_WIDTH_PROP);
	    if (null != snapShotWidthString && snapShotWidthString.trim().length() > 0) {
		snapShotWidth = Integer.parseInt(snapShotWidthString);
	    }
	    String snapShotHeightString = props.getProperty(SNAP_SHOT_HEIGHT_PROP);
	    if (null != snapShotHeightString && snapShotHeightString.trim().length() > 0) {
		snapShotHeight = Integer.parseInt(snapShotHeightString);
	    }
	    XFutureVoid setMenuTextFuture = setImageSizeMenuText();
	    futures.add(setMenuTextFuture);
	    String taskNameString = props.getProperty(APRSTASK_PROPERTY_NAME);
	    if (null != taskNameString) {
		setTaskName(taskNameString);
	    }
	    String startupActiveWinString = props.getProperty(STARTUP_ACTIVE_WIN);
	    if (null != startupActiveWinString) {
		activeWin = ActiveWinEnum.valueOf(startupActiveWinString);
		showActiveWin();
	    }
	    if (futures.isEmpty()) {
		return XFutureVoid.completedFutureWithName("loadPropertiesOnDisplay_allComplete");
	    } else {
		return XFutureVoid.allOf(futures);
	    }
	} catch (IOException exception) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", exception);
	    exA[0] = exception;
	    XFutureVoid xfv = new XFutureVoid("loadPropertiesOnDisplay IOException");
	    xfv.completeExceptionally(exception);
	    return xfv;
	} catch (Exception exception) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", exception);
	    XFutureVoid xfv = new XFutureVoid("loadPropertiesOnDisplay Exception");
	    xfv.completeExceptionally(exception);
	    return xfv;
	}
    }

    private static final String USE_CSV_FILES_INSTEAD_OF_DATABASE = "UseCsvFilesInsteadOfDatabase";
    private static final String ALERT_LIMITS = "alertLimits";

    private XFutureVoid syncPauseRecoverCheckbox() {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
	}
	if (null != aprsSystemDisplayJFrame) {
	    return runOnDispatchThread(() -> {
		if (null == executorJInternalFrame1) {
		    throw new IllegalStateException("PDDL Exectutor View must be open to use this function.");
		}
		if (null != aprsSystemDisplayJFrame) {
		    aprsSystemDisplayJFrame
			    .setPauseInsteadOfRecoverMenuCheckbox(executorJInternalFrame1.isPauseInsteadOfRecover());
		}
	    });
	} else {
	    return XFutureVoid.completedFuture();
	}
    }

    private void showActiveWin() {
	runOnDispatchThread(this::showActiveWinOnDisplay);
    }

    @UIEffect
    private void showActiveWinOnDisplay() {
	switch (activeWin) {
	case SIMVIEW_WINDOW:
	    if (null != object2DViewJInternalFrame) {
		activateFrame(object2DViewJInternalFrame);
	    }
	    break;

	case CRCL_CLIENT_WINDOW:
	    if (null != crclClientJInternalFrame) {
		activateFrame(crclClientJInternalFrame);
	    }
	    break;

	case DATABASE_SETUP_WINDOW:
	    if (null != dbSetupJInternalFrame) {
		activateFrame(dbSetupJInternalFrame);
	    }
	    break;

	case ERRLOG_WINDOW:
	    if (null != logDisplayJInternalFrame) {
		activateFrame(logDisplayJInternalFrame);
	    }
	    break;

	case PDDL_EXECUTOR_WINDOW:
	    if (null != executorJInternalFrame1) {
		activateFrame(executorJInternalFrame1);
	    }
	    break;

//            case PDDL_PLANNER_WINDOW:
//                if (null != pddlPlannerJInternalFrame) {
//                    activateFrame(pddlPlannerJInternalFrame);
//                }
//                break;
	case VISION_TO_DB_WINDOW:
	    if (null != visionToDbJInternalFrame) {
		activateFrame(visionToDbJInternalFrame);
	    }
	    break;

	case KIT_INSPECTION_WINDOW:
	    if (null != kitInspectionJInternalFrame) {
		activateFrame(kitInspectionJInternalFrame);
	    }
	    break;
	}
    }

    /**
     * Get the selected or top window (InternalJFrame).
     *
     * @return active window
     */
    public ActiveWinEnum getActiveWin() {
	return activeWin;
    }

    /**
     * Select a window (InternalJFrame)to be shown on top.
     *
     * @param activeWin enumerated window to make active
     */
    public void setActiveWin(ActiveWinEnum activeWin) {
	this.activeWin = activeWin;
	showActiveWin();
    }

    public XFutureVoid saveProperties() throws IOException {
	newPropertiesFile = false;
	if (null == propertiesFile) {
	    throw new NullPointerException("propertiesFile");
	}
	File propsParent = propertiesFile.getParentFile();
	List<XFutureVoid> futures = new ArrayList<>();
	if (propsParent == null) {
	    System.err.println("propertiesFile.getParentFile() returned null : propertiesFile=" + propertiesFile);
	    return XFutureVoid.completedFuture();
	}
	if (!propsParent.exists()) {
	    println("Directory " + propsParent + " does not exist. (Creating it now.)");
	    propsParent.mkdirs();
	}
	Map<String, String> propsMap = new HashMap<>();

	propsMap.put(ALERT_LIMITS, Boolean.toString(isAlertLimitsCheckBoxSelected()));
	propsMap.put(USETEACHTABLE, Boolean.toString(getUseTeachTable()));
	propsMap.put(STARTUPPDDLPLANNER, Boolean.toString(isPddlPlannerStartupSelected()));
	propsMap.put(STARTUPPDDLEXECUTOR, Boolean.toString(isExecutorStartupSelected()));
	propsMap.put(STARTUPPDDLOBJECTSP, Boolean.toString(isObjectSpStartupSelected()));
	propsMap.put(STARTUPPDDLOBJECTVIEW, Boolean.toString(isObject2DViewStartupSelected()));
	propsMap.put(STARTUPROBOTCRCLCLIENT, Boolean.toString(isRobotCrclGUIStartupSelected()));
	propsMap.put(STARTUPFORCETORQUESIM, Boolean.toString(isForceTorqueSimStartupSelected()));
	propsMap.put(STARTUPROBOTCRCLSIMSERVER, Boolean.toString(isRobotCrclSimServerStartupSelected()));
	propsMap.put(STARTUPROBOTCRCLFANUCSERVER, Boolean.toString(isRobotCrclFanucServerStartupSelected()));
	propsMap.put(STARTUPROBOTCRCLMOTOMANSERVER, Boolean.toString(isRobotCrclMotomanServerStartupSelected()));
	propsMap.put(STARTUPCONNECTDATABASE, Boolean.toString(isConnectDatabaseOnSetupStartupSelected()));
	propsMap.put(STARTUPCONNECTVISION, Boolean.toString(isConnectVisionOnStartupSelected()));
	propsMap.put(STARTUPEXPLOREGRAPHDB, Boolean.toString(isExploreGraphDBStartupSelected()));
	if (null != customWindowsFile) {
	    propsMap.put(CUSTOM_WINDOWS_FILE, customWindowsFile.getName());
	}

//        propsMap.put(STARTUPCRCLWEBAPP, Boolean.toString(jCheckBoxMenuItemStartupCRCLWebApp.isSelected()));
	propsMap.put(CRCLWEBAPPPORT, Integer.toString(crclWebServerHttpPort));
	propsMap.put(STARTUP_ACTIVE_WIN, activeWin.toString());
	propsMap.put(STARTUPKITINSPECTION, Boolean.toString(isKitInspectionStartupSelected()));
//        propsMap.put(MAX_LIMIT_PROP, maxLimit.toString());
//        propsMap.put(MIN_LIMIT_PROP, minLimit.toString());
	propsMap.put(SNAP_SHOT_ENABLE_PROP, Boolean.toString(isSnapshotsSelected()));
	propsMap.put(SNAP_SHOT_WIDTH_PROP, Integer.toString(snapShotWidth));
	propsMap.put(SNAP_SHOT_HEIGHT_PROP, Integer.toString(snapShotHeight));
	propsMap.put(RELOAD_SIM_FILES_ON_REVERSE_PROP, Boolean.toString(isReloadSimFilesOnReverseCheckBoxSelected()));
	propsMap.put(LOG_CRCL_PROGRAMS_ENABLED, Boolean.toString(isLogCrclProgramsSelected()));
	propsMap.put(USE_CSV_FILES_INSTEAD_OF_DATABASE, Boolean.toString(this.isUseCsvFilesInsteadOfDatabase()));
	setDefaultRobotName();
	if (null != robotName) {
	    propsMap.put(APRSROBOT_PROPERTY_NAME, robotName);
	}
	if (null != taskName) {
	    propsMap.put(APRSTASK_PROPERTY_NAME, taskName);
	}
	if (null != fanucServerProvider) {
	    final String providerRemoteRobotHost = fanucServerProvider.getRemoteRobotHost();
	    this.fanucCrclPort = fanucServerProvider.getCrclPort();
	    propsMap.put(FANUC_CRCL_LOCAL_PORT, Integer.toString(fanucCrclPort));
	    if (null != providerRemoteRobotHost) {
		this.fanucRobotHost = providerRemoteRobotHost;
		propsMap.put(FANUC_ROBOT_HOST, fanucRobotHost);
	    }
	}
	if (null != motomanServerProvider) {
	    this.motomanCrclPort = motomanServerProvider.getCrclPort();
	    propsMap.put(MOTOMAN_CRCL_LOCAL_PORT, Integer.toString(motomanCrclPort));
	}

	Properties props = new Properties();
	props.putAll(propsMap);
	println("AprsSystem saving properties to " + propertiesFile.getCanonicalPath());
//        try (FileWriter fw = new FileWriter(propertiesFile)) {
//            props.store(fw, "");
//        }
	Utils.saveProperties(propertiesFile, props);
	updateSubPropertiesFiles();
	if (null != this.kitInspectionJInternalFrame) {
	    this.kitInspectionJInternalFrame.saveProperties();
	}
//        if (null != this.pddlPlannerJInternalFrame) {
//            this.pddlPlannerJInternalFrame.saveProperties();
//        }
	if (null != this.executorJInternalFrame1) {
	    this.executorJInternalFrame1.saveProperties();
	}
	if (null != this.visionToDbJInternalFrame) {
	    this.visionToDbJInternalFrame.saveProperties();
	}
	if (null != this.object2DViewJInternalFrame) {
	    futures.add(this.object2DViewJInternalFrame.saveProperties());
	}
	if (null != this.crclClientJInternalFrame) {
	    crclClientJInternalFrame.saveProperties();
	}
	if (null != this.simServerJInternalFrame) {
	    simServerJInternalFrame.saveProperties();
	}
	if (null != this.motomanServerProvider) {
	    motomanServerProvider.saveProperties();
	}
	if (null != this.fanucServerProvider) {
	    fanucServerProvider.saveProperties();
	}
	if (null != this.forceTorqueSimJInternalFrame) {
	    forceTorqueSimJInternalFrame.saveProperties();
	}
	if (null != dbSetup) {
	    File dbPropsFile = new File(propertiesDirectory, this.propertiesFileBaseString + "_dbsetup.txt");
	    if (null != dbSetupJInternalFrame) {
		dbSetupJInternalFrame.setPropertiesFile(dbPropsFile);
	    }
	    DbSetupBuilder.savePropertiesFile(dbPropsFile, dbSetup);
	}
	for (Map.Entry<File, JInternalFrame> customWindowEntry : customFileWindowMap.entrySet()) {
	    AutomaticPropertyFileUtils.saveObjectProperties(customWindowEntry.getKey(), customWindowEntry.getValue());
	}
	return XFutureVoid.allOf(futures);
    }

//    private File getForceTorqueSimPropsFile() {
//        return new File(propertiesDirectory, this.propertiesFileBaseString + "_force_torque_sim.txt");
//    }
    public boolean checkPose(
	    PoseType goalPose,
	    boolean ignoreCartTran,
	    boolean reverseCorrectPoint) {
	if (null == crclClientJInternalFrame) {
	    throw new IllegalStateException("null == crclClientJInternalFrame");
	}

	if (!ignoreCartTran) {
	    PointType point = goalPose.getPoint();
	    if (null == point) {
		throw new NullPointerException("goalPose.getPoint()");
	    }
	    if (reverseCorrectPoint) {
		PointType reversedPoint = convertRobotToVisionPoint(point);
		point = reversedPoint;
	    }
	    PmCartesian cart = CRCLPosemath.toPmCartesian(point);
	    if (!isWithinLimits(cart)) {
		return false;
	    }
	}
	boolean firstCheckPoseRet = crclClientJInternalFrame.checkPose(goalPose, ignoreCartTran, true);
	if (!firstCheckPoseRet) {
	    boolean secondCheckPoseRet = crclClientJInternalFrame.checkPose(goalPose, ignoreCartTran, true);
	    updateRobotLimits();
	    boolean thirdCheck = crclClientJInternalFrame.checkPose(goalPose, ignoreCartTran, true);
	    return thirdCheck;
	}
	return true;
    }

    private static final String LOG_CRCL_PROGRAMS_ENABLED = "LogCrclProgramsEnabled";

    private static final String USETEACHTABLE = "USETEACHTABLE";
    private static final String RELOAD_SIM_FILES_ON_REVERSE_PROP = "reloadSimFilesOnReverse";
    private static final String SNAP_SHOT_HEIGHT_PROP = "snapShotHeight";
    private static final String SNAP_SHOT_WIDTH_PROP = "snapShotWidth";
    private static final String SNAP_SHOT_ENABLE_PROP = "snapShotEnable";
    private static final String MIN_LIMIT_PROP = "minLimit";
    private static final String MAX_LIMIT_PROP = "maxLimit";
    private static final String STARTUP_ACTIVE_WIN = "STARTUP_ACTIVE_WIN";
    private static final String APRSTASK_PROPERTY_NAME = "aprs.taskName";
    private static final String APRSROBOT_PROPERTY_NAME = "aprs.robotName";

    private void setDefaultRobotName() {
	if (null == robotName) {
	    if (fanucServerProvider != null) {
		setRobotName("Fanuc");
	    } else if (motomanServerProvider != null) {
		setRobotName("Motoman");
	    } else if (simServerJInternalFrame != null) {
		setRobotName("Simulated");
	    } else if (isRobotCrclFanucServerStartupSelected()) {
		setRobotName("Fanuc");
	    } else if (isRobotCrclMotomanServerStartupSelected()) {
		setRobotName("Motoman");
	    } else if (isRobotCrclSimServerStartupSelected()) {
		setRobotName("Simulated");
	    }
	}
    }

    private static final String STARTUPPDDLPLANNER = "startup.pddl.planner";
    private static final String STARTUPPDDLEXECUTOR = "startup.pddl.executor";
    private static final String STARTUPPDDLOBJECTSP = "startup.pddl.objectsp";
    private static final String STARTUPPDDLOBJECTVIEW = "startup.pddl.objectview";
    private static final String STARTUPROBOTCRCLCLIENT = "startup.robotcrclclient";
    private static final String STARTUPFORCETORQUESIM = "startup.forceTorqueSim";
    private static final String STARTUPROBOTCRCLSIMSERVER = "startup.robotcrclsimserver";
    private static final String STARTUPROBOTCRCLFANUCSERVER = "startup.robotcrclfanucserver";
    private static final String STARTUPROBOTCRCLMOTOMANSERVER = "startup.robotcrclmotomanserver";
    private static final String STARTUPKITINSPECTION = "startup.kitinspection";
    private static final String STARTUPCONNECTDATABASE = "startup.connectdatabase";
    private static final String STARTUPCONNECTVISION = "startup.connectvision";
    private static final String STARTUPEXPLOREGRAPHDB = "startup.exploreGraphDb";
    private static final String CRCLWEBAPPPORT = "crclWebApp.httpPort";
    private static final String FANUC_CRCL_LOCAL_PORT = "fanuc.crclLocalPort";
    private static final String FANUC_ROBOT_HOST = "fanuc.robotHost";
    private static final String MOTOMAN_CRCL_LOCAL_PORT = "motoman.crclLocalPort";

    private static final String CUSTOM_WINDOWS_FILE = "aprs.customWindowsFile";


    /**
     * Get the file where properties are read from and written to.
     *
     * @return properties file
     */
    public @Nullable File getPropertiesFile() {
	return propertiesFile;
    }

    private boolean newPropertiesFile = true;

    public final void setPropertiesFile(File propertiesFile) {
	this.propertiesFile = propertiesFile;
	if (null == propertiesFile) {
	    System.err.println("propertiesFile == null");
	    return;
	}
	if (propertiesFile.exists()) {
	    newPropertiesFile = false;
	}
	File propertiesFileParent = propertiesFile.getParentFile();
	if (null == propertiesFileParent) {
	    System.err.println("propertiesFile " + propertiesFile + " has no parent");
	    return;
	}
	propertiesDirectory = propertiesFileParent;
	if (null == propertiesDirectory) {
	    System.err.println("propertiesFile.getParentFile() returned null : propertiesFile=" + propertiesFile);
	    return;
	}
	if (!propertiesDirectory.exists()) {
	    try {
		println("Directory " + propertiesDirectory.getCanonicalPath() + " does not exist. (Creating it now!)");
		propertiesDirectory.mkdirs();

	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
	    }
	}
	try {
	    updateSubPropertiesFiles();

	} catch (IOException ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	submitUpdateTitle();
    }

    private String propertiesFileBaseString = "";

    private void updateSubPropertiesFiles() throws IOException {
	String base = propertiesFileBaseString();
	updateSubPropertiesFiles(base);
    }

    private void updateSubPropertiesFiles(String base) throws IOException {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	if (null == propertiesFile) {
	    throw new NullPointerException("propertiesFile");
	}
	propertiesFileBaseString = base;
//        if (null != this.pddlPlannerJInternalFrame) {
//            this.pddlPlannerJInternalFrame.setPropertiesFile(pddlPlannerPropertiesFile(propertiesDirectory, base));
//        }
	if (null != this.forceTorqueSimJInternalFrame) {
	    this.forceTorqueSimJInternalFrame
		    .setPropertiesFile(forceTorqueSimPropertiesFile(propertiesDirectory, base));
	}
	if (null != this.kitInspectionJInternalFrame) {
	    this.kitInspectionJInternalFrame
		    .setPropertiesFile(new File(propertiesDirectory, base + "_kitInspection.txt"));
	}
	if (null != this.executorJInternalFrame1) {
	    this.executorJInternalFrame1.setPropertiesFile(actionsToCrclPropertiesFile(propertiesDirectory, base));
	}
	if (null != this.visionToDbJInternalFrame) {
	    visionToDbJInternalFrame.setPropertiesFile(visionToDbPropertiesFile(propertiesDirectory, base));
	}
	if (null != this.object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.setPropertiesFile(object2DViewPropertiesFile(propertiesDirectory, base));
	}
	if (null != lastAprsPropertiesFileFile) {
	    Properties props = new Properties();
	    props.put("lastPropertyFileName", propertiesFile.getCanonicalPath());
//            try (FileWriter fw = new FileWriter(lastAprsPropertiesFileFile)) {
//                props.store(fw, "");
//            }
	    Utils.saveProperties(lastAprsPropertiesFileFile, props);
	}
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.setPropertiesFile(crclClientPropertiesFile(propertiesDirectory, base));
	}

	if (null != simServerJInternalFrame) {
	    simServerJInternalFrame.setPropertiesFile(crclSimServerPropertiesFile(propertiesDirectory, base));
	}
	if (null != motomanServerProvider) {
	    motomanServerProvider.setPropertiesFile(motomanPropertiesFile(propertiesDirectory, base));
	}
	if (null != dbSetupJInternalFrame) {
	    File dbPropsFile = new File(propertiesDirectory, this.propertiesFileBaseString + "_dbsetup.txt");
	    dbSetupJInternalFrame.setPropertiesFile(dbPropsFile);
	}
	if (null != fanucServerProvider) {
	    fanucServerProvider
		    .setPropertiesFile(new File(propertiesDirectory, base + "_fanucCrclServerProperties.txt"));
	}
    }

    private static File actionsToCrclPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_actionsToCrclProperties.txt");
    }

    private static File visionToDbPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_visionToDBProperties.txt");
    }

    private static File crclSimServerPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_crclSimServerProperties.txt");
    }

    private static File crclClientPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_crclSwingClientProperties.txt");
    }

    private static File object2DViewPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_object2DViewProperties.txt");
    }

    private static File motomanPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_motomanCrclServerProperties.txt");
    }

    // private static File pddlPlannerPropertiesFile(File propertiesDirectory,
    // String base) {
//        return new File(propertiesDirectory, base + "_pddlPlanner.txt");
//    }
    private static File forceTorqueSimPropertiesFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_forceTorqueSim.txt");
    }

    private String propertiesFileBaseString() {
	if (null == propertiesFile) {
	    throw new NullPointerException("propertiesFile");
	}
	String base = propertiesFile.getName();
	int pindex = base.indexOf('.');
	if (pindex > 0) {
	    base = base.substring(0, pindex);
	}
	return base;
    }

    public File getCartLimitsCsvFile() {
	if (null == propertiesDirectory) {
	    throw new NullPointerException("propertiesDirectory");
	}
	return cartLimitsCsvFile(propertiesDirectory, propertiesFileBaseString());
    }

    private static File cartLimitsCsvFile(File propertiesDirectory, String base) {
	return new File(propertiesDirectory, base + "_cartLimits.csv");
    }

    private volatile @Nullable XFuture<?> disconnectDatabaseFuture = null;

    public XFuture<?> startDisconnectDatabase() {
	setConnectDatabaseCheckBoxEnabled(false);
	if (null != connectDatabaseFuture) {
	    connectDatabaseFuture.cancel(true);
	    connectDatabaseFuture = null;
	}
	XFuture<?> ret = XFuture.runAsync("disconnectDatabase",
		this::disconnectDatabase, runProgramService);
	disconnectDatabaseFuture = ret;
	return ret;
    }

    private void disconnectDatabase() {

	if (null == dbSetupJInternalFrame) {
	    return;
	}
//        if (null == dbSetupJInternalFrame) {            throw new IllegalStateException("DB Setup View must be open to use this function.");        }
	try {
	    dbConnected = false;
	    if (null != connectDatabaseFuture) {
		connectDatabaseFuture.cancel(true);
		connectDatabaseFuture = null;
	    }
	    DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
	    DbSetup dbsetup = dbSetupPublisher.getDbSetup();
	    String dbsetuphost;
	    int port;
	    if (null != dbsetup) {
		dbsetuphost = dbsetup.getHost();
		port = dbsetup.getPort();
	    } else {
		port = -1;
		dbsetuphost = null;
	    }
	    // dbSetupPublisher.setDbSetup(new
	    // DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(false).build());
	    dbSetupPublisher.disconnect();

	    List<XFutureVoid> futures = dbSetupPublisher.notifyAllDbSetupListeners(null);
	    for (Future<?> f : futures) {
		if (!f.isDone() && !f.isCancelled()) {
		    try {
			f.get(100, TimeUnit.MILLISECONDS);

		    } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException ex) {
			Logger.getLogger(AprsSystem.class
				.getName()).log(Level.FINE, null, ex);
		    }
		}
	    }
	    setConnectDatabaseCheckBoxSelected(false);
	    if (null != dbsetuphost) {
		println("Finished disconnect from database. " + dbsetuphost + ":" + port);
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
    }

    private int priority;

    /**
     * Get the value of priority
     *
     * @return the value of priority
     */
    public int getPriority() {
	return priority;
    }

    /**
     * Set the value of priority
     *
     * @param priority new value of priority
     */
    public void setPriority(int priority) {
	this.priority = priority;
    }

    private volatile boolean closing = false;

    public void close() {
	String task = getTaskName();
	int count = runProgramCount.get();
	long totalRunProgramTime = runProgramTime.get();
	final String closingRunProgramInfo = "closing ... runProgramCount=" + count
		+ ", totalRunProgramTime=" + totalRunProgramTime;
	logEvent(closingRunProgramInfo);
	logToSuper(closingRunProgramInfo);
	closing = true;
	if (null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.stopSimUpdateTimer();
	}
	if (null != eventLogPrinter) {
	    try {
		eventLogPrinter.close();
	    } catch (IOException ex) {
		Logger.getLogger(AprsSystem.class
			.getName()).log(Level.SEVERE, "", ex);
	    }
	    eventLogPrinter = null;
	}
	if (null != task) {
	    println("AprsSystem.close() : task=" + task);
	}
	try {
	    if (null != crclClientJInternalFrame) {
		File profileMapFile = crclClientJInternalFrame.writeCommandProfileMap();
		System.out.println("profileMapFile = " + profileMapFile);
		File commandStatusLogFile = crclClientJInternalFrame.writeCommandStatusLogFile();
		System.out.println("commandStatusLogFile = " + commandStatusLogFile);
		crclClientJInternalFrame.printPerfInfo(System.out, task);
		try (PrintStream ps = new PrintStream(
			new FileOutputStream(Utils.createTempFile(task + "crcLClientPerfInfo", ".txt")))) {
		    crclClientJInternalFrame.printPerfInfo(ps, task);
		}
	    }
	} catch (IOException iOException) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", iOException);
	}
	closeAllWindows();
	runProgramService.shutdownNow();
	if (null != aprsSystemDisplayJFrame) {
	    runOnDispatchThread(this::closeAprsSystemDisplayJFrame);
	}
	Runnable r = onCloseRunnable.getAndSet(null);
	if (null != r) {
	    r.run();
	}

    }

    @UIEffect
    private void closeAprsSystemDisplayJFrame() {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.setVisible(false);
	    aprsSystemDisplayJFrame.dispose();
	}
    }

    public void closeActionsListExcecutor() {
	if (null != executorJInternalFrame1) {
	    executorJInternalFrame1.close();
//            if (null != pddlPlannerJInternalFrame) {
//                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(null);
//            }
	}
    }

//    public void closePddlPlanner() {
//        if (null != pddlPlannerJInternalFrame) {
//            pddlPlannerJInternalFrame.close();
//        }
//    }
    private final boolean lastContinueCrclProgramResult = false;

    /**
     * Continue or start executing the currently loaded CRCL program.
     * <p>
     * The actions will be executed in another thread after this method returns. The
     * task can be monitored or canceled using the returned future.
     *
     * @return a future that can be used to monitor, extend or cancel the underlying
     *         task.
     */
    public XFuture<Boolean> continueCrclProgram() {
	setStartRunTime();
	if (null != crclClientJInternalFrame) {
	    return crclClientJInternalFrame.continueCurrentProgram(isStepMode());
	} else {
	    return XFuture.completedFuture(false);
	}
    }

    /**
     * Get the value of enableDatabaseUpdates
     *
     * @return current setting for enableDatabaseUpdates
     */
    public boolean isEnableVisionToDatabaseUpdates() {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	return visionToDbJInternalFrame.isEnableDatabaseUpdates();
    }

    /**
     * Set the value of enableDatabaseUpdates
     *
     * @param enableDatabaseUpdates new value of enableDatabaseUpdates
     * @param requiredParts         map of part names to required number of each
     *                              part type
     */
    public void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
	if (null == visionToDbJInternalFrame) {
	    throw new IllegalStateException("[Object SP] Vision To Database View must be open to use this function.");
	}
	visionToDbJInternalFrame.setEnableDatabaseUpdates(enableDatabaseUpdates, requiredParts);
	if (enableDatabaseUpdates && null != object2DViewJInternalFrame) {
	    object2DViewJInternalFrame.refresh(false);
	}
    }

    private volatile @Nullable File logDir = null;

    /**
     * Get the current directory for saving log files
     *
     * @return log files directory
     * @throws java.io.IOException file can not be created ie default log directory
     *                             does not exist.
     */
    public File getLogDir() throws IOException {
	File f = this.logDir;
	if (f != null) {
	    return f;
	}
	f = new File(Utils.getlogFileDir(), getRunName());
	f.mkdirs();
	this.logDir = f;
	return f;
    }

    private volatile @Nullable File logImageDir = null;

    public File getLogImageDir() throws IOException {
	File f = logImageDir;
	if (f != null) {
	    return f;
	}
	f = new File(getLogDir(), "images");
	f.mkdirs();
	this.logImageDir = f;
	return f;
    }

    private volatile @Nullable File logCrclProgramDir = null;

    public File getLogCrclProgramDir() throws IOException {
	File f = logCrclProgramDir;
	if (f != null) {
	    return f;
	}
	f = new File(getLogDir(), "crclProgram");
	f.mkdirs();
	this.logCrclProgramDir = f;
	return f;
    }

    private volatile @Nullable File logCrclStatusDir = null;

    public File getLogCrclStatusDir() throws IOException {
	File f = logCrclStatusDir;
	if (f != null) {
	    return f;
	}
	f = new File(getLogDir(), "crclStatus");
	f.mkdirs();
	this.logCrclStatusDir = f;
	return f;
    }

    private volatile @Nullable File logCrclCommandDir = null;

    public File getLogCrclCommandDir() throws IOException {
	File f = logCrclCommandDir;
	if (f != null) {
	    return f;
	}
	f = new File(getLogDir(), "crclCommand");
	f.mkdirs();
	this.logCrclCommandDir = f;
	return f;
    }

    public @Nullable File logCrclCommand(String prefix, CRCLCommandType cmd) {
	File f = null;
	try {
	    String xmlString = CRCLSocket.getUtilSocket().commandToPrettyString(cmd);
	    f = createTempFile(prefix, ".xml", getLogCrclCommandDir());
	    try (PrintWriter printer = new PrintWriter(new FileWriter(f))) {
		printer.print(xmlString);
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	return f;
    }

    public @Nullable File logCrclStatus(String prefix, CRCLStatusType stat) {
	File f = null;
	try {
	    String xmlString = CRCLSocket.statusToPrettyString(stat);
	    f = createTempFile(prefix, ".xml", getLogCrclStatusDir());
	    try (PrintWriter printer = new PrintWriter(new FileWriter(f))) {
		printer.print(xmlString);
	    }
	} catch (Exception ex) {
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, "", ex);
	}
	return f;
    }

    private static String cleanAndLimitFilePrefix(String prefix_in) {
	if (prefix_in.length() > 80) {
	    prefix_in = prefix_in.substring(0, 79);
	}
	String prefixOut = prefix_in
		.replace("\r", "")
		.replace("\n", "")
		.replaceAll("[ \t:;-=]+", "_").replace('\\', '_').replace('/', '_');
	if (prefixOut.length() > 80) {
	    prefixOut = prefixOut.substring(0, 79);
	}
	if (!prefixOut.endsWith("_")) {
	    prefixOut = prefixOut + "_";
	}
	return prefixOut;
    }

    /**
     * Create a temporary file in the current log file directory with the standard
     * timestamp string.
     *
     * @param prefix string filename will begin with
     * @param suffix string filename will end with (typically an extention eg
     *               ".csv")
     * @return reference to created file
     * @throws IOException directory doesn't exist etc.
     */
    public File createTempFile(String prefix, String suffix) throws IOException {
	return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, getLogDir());
    }

    /**
     * Create a temporary file in the given directory with the standard timestamp
     * string.
     *
     * @param prefix string filename will begin with
     * @param suffix string filename will end with (typically an extention eg
     *               ".csv")
     * @param dir    directory to create file in
     * @return reference to created file
     * @throws IOException directory doesn't exist etc.
     */
    public File createTempFile(String prefix, String suffix, File dir) throws IOException {
	String cleanedPrefix = cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix);
	try {

	    return File.createTempFile(cleanedPrefix, suffix, dir);

	} catch (IOException exception) {
	    String errInfo = "cleanedPrefix=" + cleanedPrefix + ",suffix=" + suffix + ",dir=" + dir;
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, errInfo, exception);
	    throw new IOException(errInfo, exception);
	}
    }

    private static final AtomicInteger SIF_INT = new AtomicInteger();

    public File snapshotImageFile(String prefix, String suffix, File dir) throws IOException {
	String cleanedPrefix = cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix);
	try {
	    return new File(dir, cleanedPrefix + String.format("%06d", SIF_INT.incrementAndGet()) + suffix);
	} catch (Exception exception) {
	    String errInfo = "cleanedPrefix=" + cleanedPrefix + ",suffix=" + suffix + ",dir=" + dir;
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, errInfo, exception);
	    throw new IOException(errInfo, exception);
	}
    }

    public File snapshotImageFile(String prefix) throws IOException {
	File logImageDir1 = getLogImageDir();
	try {
	    return snapshotImageFile(prefix, ".PNG", logImageDir1);
	} catch (IOException iOException) {
	    String errInfo = "prefix=" + prefix + ",logImageDir1=" + logImageDir1;
	    System.err.println(errInfo);
	    Logger.getLogger(AprsSystem.class
		    .getName()).log(Level.SEVERE, errInfo, iOException);
	    throw new IOException(errInfo, iOException);
	}
    }

    private volatile String asString = "";

    @Override
    public String toString() {
	return asString;
    }

    public void showException(Throwable ex) {
	if (null != aprsSystemDisplayJFrame) {
	    aprsSystemDisplayJFrame.showException(ex);
	}
    }

    /**
     * Get a list with information on how the most recently loaded CRCL program has
     * run so far.
     *
     * @return run data for last program
     */
    public @Nullable List<ProgramRunData> getLastProgRunDataList() {
	if (null == crclClientJInternalFrame) {
	    return Collections.emptyList();
	}
	return crclClientJInternalFrame.getLastProgRunDataList();
    }

    /**
     * Save the given run data which contains information on how a given program run
     * went to a CSV file.
     *
     * @param f    file to save
     * @param list data to write to file
     * @throws IOException file does not exist or not writeable etc
     */
    public void saveProgramRunDataListToCsv(File f, List<ProgramRunData> list) throws IOException {
	if (null == crclClientJInternalFrame) {
	    return;
	}
	crclClientJInternalFrame.saveProgramRunDataListToCsv(f, list);
    }

    /**
     * Save the current run data which contains information on how a given program
     * run went to a CSV file.
     *
     * @param f file to save
     * @throws IOException file does not exist or not writeable etc
     */
    public void saveLastProgramRunDataListToCsv(File f) throws IOException {
	if (null == crclClientJInternalFrame) {
	    return;
	}
	logEvent("saveLastProgramRunDataListToCsv", f.getCanonicalPath());
	crclClientJInternalFrame.saveLastProgramRunDataListToCsv(f);
    }

    public void addSelectedToolNameListener(Consumer<String> listener) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
	}
	executorJInternalFrame1.addSelectedToolNameListener(listener);
    }

    public void removeSelectedToolNameListener(Consumer<String> listener) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
	}
	executorJInternalFrame1.removeSelectedToolNameListener(listener);
    }

    public void addToolHolderContentsListener(BiConsumer<String, String> listener) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
	}
	executorJInternalFrame1.addToolHolderContentsListener(listener);
    }

    public void removeToolHolderContentsListener(BiConsumer<String, String> listener) {
	if (null == executorJInternalFrame1) {
	    throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
	}
	executorJInternalFrame1.removeToolHolderContentsListener(listener);
    }

    /**
     * Get the value of preClosing
     *
     * @return the value of preClosing
     */
    public boolean isCrclClientPreClosing() {
	if (null == crclClientJInternalFrame) {
	    return false;
	}
	return crclClientJInternalFrame.isPreClosing();
    }

    /**
     * Set the value of preClosing
     *
     * @param preClosing new value of preClosing
     */
    public void setCrclClientPreClosing(boolean preClosing) {
	if (null != crclClientJInternalFrame) {
	    crclClientJInternalFrame.setPreClosing(preClosing);
	}
    }

    public @Nullable File getObject2DViewLogLinesFile() {
	if (null == object2DViewJInternalFrame) {
	    throw new NullPointerException("object2DViewJInternalFrame");
	}
	return this.object2DViewJInternalFrame.getLogLinesFile();
    }

    public @Nullable File getObject2DViewPropertiesFile() {
	if (null == object2DViewJInternalFrame) {
	    throw new NullPointerException("object2DViewJInternalFrame");
	}
	return this.object2DViewJInternalFrame.getPropertiesFile();
    }
}
