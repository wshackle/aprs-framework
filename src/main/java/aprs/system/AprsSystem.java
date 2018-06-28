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

import aprs.launcher.LauncherAprsJFrame;
import aprs.misc.SlotOffsetProvider;
import aprs.misc.Utils;
import aprs.misc.ActiveWinEnum;
import aprs.actions.executor.Action;
import aprs.database.DbSetup;
import aprs.database.DbSetupBuilder;
import aprs.database.DbSetupJInternalFrame;
import aprs.database.DbSetupListener;
import aprs.database.DbSetupPublisher;
import aprs.database.DbType;
import aprs.database.PhysicalItem;
import aprs.database.explore.ExploreGraphDbJInternalFrame;
import aprs.kitinspection.KitInspectionJInternalFrame;
import aprs.logdisplay.LogDisplayJInternalFrame;
import aprs.database.PartsTray;
import aprs.database.Slot;
import aprs.database.Tray;
import aprs.learninggoals.GoalLearner;
import aprs.actions.executor.CrclGenerator;
import aprs.actions.executor.CrclGenerator.PoseProvider;
import aprs.actions.executor.ExecutorJInternalFrame;
import aprs.actions.executor.PositionMap;
import aprs.pddl_planner.PddlPlannerJInternalFrame;
import aprs.simview.Object2DJPanel;
import aprs.simview.Object2DViewJInternalFrame;
import aprs.database.vision.UpdateResults;
import aprs.database.vision.VisionToDbJInternalFrame;
import aprs.supervisor.main.Supervisor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JInternalFrame;

import com.github.wshackle.fanuccrclservermain.FanucCRCLMain;
import com.github.wshackle.fanuccrclservermain.FanucCRCLServerJInternalFrame;
import com.github.wshackle.crcl4java.motoman.ui.MotomanCrclServerJInternalFrame;
import crcl.base.ActuateJointsType;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLProgramType;
import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
import crcl.base.CommandStatusType;
import crcl.base.EndCanonType;
import crcl.base.InitCanonType;
import crcl.base.MiddleCommandType;
import crcl.base.MoveThroughToType;
import crcl.base.MoveToType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.SetEndEffectorType;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import crcl.ui.client.PendantClientInner;
import crcl.ui.client.PendantClientJPanel;
import crcl.ui.client.UpdateTitleListener;
import crcl.ui.server.SimServerJInternalFrame;
import crcl.utils.CRCLException;
import crcl.utils.CRCLPosemath;
import crcl.utils.CRCLSocket;
import crcl.utils.outer.interfaces.ProgramRunData;

import java.awt.Container;
import java.awt.Image;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;
import crcl.ui.client.PendantClientJInternalFrame;
import crcl.utils.CrclCommandWrapper;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.swing.DesktopManager;
import javax.swing.JDesktopPane;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

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
@SuppressWarnings("SameReturnValue")
public class AprsSystem implements SlotOffsetProvider, AprsSystemInterface {

    @MonotonicNonNull
    private final AprsSystemDisplayJFrame aprsSystemDisplayJFrame;

    @MonotonicNonNull
    private VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    @MonotonicNonNull
    private ExecutorJInternalFrame pddlExecutorJInternalFrame1 = null;
    @MonotonicNonNull
    private Object2DViewJInternalFrame object2DViewJInternalFrame = null;
    @MonotonicNonNull
    private PddlPlannerJInternalFrame pddlPlannerJInternalFrame = null;
    @MonotonicNonNull
    private DbSetupJInternalFrame dbSetupJInternalFrame = null;
    @MonotonicNonNull
    private volatile PendantClientJInternalFrame crclClientJInternalFrame = null;
    @MonotonicNonNull
    private SimServerJInternalFrame simServerJInternalFrame = null;
    @MonotonicNonNull
    private volatile LogDisplayJInternalFrame logDisplayJInternalFrame = null;
    @MonotonicNonNull
    private FanucCRCLMain fanucCRCLMain = null;
    @MonotonicNonNull
    private FanucCRCLServerJInternalFrame fanucCRCLServerJInternalFrame = null;
    @MonotonicNonNull
    private ExploreGraphDbJInternalFrame exploreGraphDbJInternalFrame = null;
    @MonotonicNonNull
    private MotomanCrclServerJInternalFrame motomanCrclServerJInternalFrame = null;
    @MonotonicNonNull
    private KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

    private String taskName;

    private volatile long lastStartRunTime = -1;
    private volatile long lastStopRunTime = -1;
    private volatile long lastRunDuration = -1;
    private volatile long lastStopDuration = -1;
    private final AtomicLong effectiveStartRunTime = new AtomicLong(-1);
    private final AtomicLong effectiveStopRunTime = new AtomicLong(-1);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean snapshotsEnabled = false;

    private final AtomicReference<@Nullable Runnable> onCloseRunnable = new AtomicReference<>();

    @Override
    public void setOnCloseRunnable(@Nullable Runnable r) {
        onCloseRunnable.set(r);
    }

    /**
     * Check if the user has selected a check box asking for snapshot files to
     * be created for logging/debugging.
     *
     * @return Has user enabled snapshots?
     */
    @Override
    public boolean isSnapshotsEnabled() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isSnapshotsEnabled();
            this.snapshotsEnabled = ret;
            return ret;
        }
        return snapshotsEnabled;
    }

    @Override
    public void setSnapshotsEnabled(boolean enable) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setSnapshotsEnabled(enable);
        }
        this.snapshotsEnabled = enable;
    }

    private void setStartRunTime() {
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

    private String origRobotName;

    /**
     * Get the value of origRobotName
     *
     * @return the value of origRobotName
     */
    public String getOrigRobotName() {
        maybeSetOrigRobotName(getRobotName());
        return origRobotName;
    }

    private Image scanImage;

    /**
     * Get the most recent image created when scanning for desired part
     * locations.
     *
     * @return image of most recent scan
     */
    @Override
    public Image getScanImage() {
        return scanImage;
    }

    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    @Nullable
    @Override
    public PoseProvider getPddlExecExternalPoseProvider() {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        return pddlExecutorJInternalFrame1.getExternalPoseProvider();
    }

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalPoseProvider new value of externalGetPoseFunction
     */
    @Override
    public void setPddlExecExternalPoseProvider(PoseProvider externalPoseProvider) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1  ";
        this.pddlExecutorJInternalFrame1.setExternalPoseProvider(externalPoseProvider);
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
     * Get the duration in milliseconds that this system has been in the run
     * state.
     *
     * @return duration in milliseconds or 0 if it has not been started and
     * stopped at least once.
     */
    @Override
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
     * @return duration in milliseconds or 0 if it has not been started and
     * stopped at least once.
     */
    @Override
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

    @SuppressWarnings("SameParameterValue")
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

//    private volatile StackTraceElement lastGetSingleVisionToDbUpdateStackTrace[] = null;
    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame
     * is received from vision.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @Override
    public XFuture<List<PhysicalItem>> getSingleVisionToDbUpdate() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        long startTime = logEvent("start getSingleVisionToDbUpdate", null);
        XFuture<List<PhysicalItem>> ret = visionToDbJInternalFrame.getSingleUpdate();
        return ret.thenApply(x -> {
            logEvent("end getSingleVisionToDbUpdate",
                    printListToString(x.stream().map(PhysicalItem::getFullName).collect(Collectors.toList()), 8)
                    + "\n started at" + startTime
                    + "\n timeDiff=" + (startTime - System.currentTimeMillis())
            );
            return x;
        });
    }

    @Override
    public long getLastSingleVisionToDbUpdateTime() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getLastUpdateTime();
    }

    @Override
    public long getSingleVisionToDbNotifySingleUpdateListenersTime() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getNotifySingleUpdateListenersTime();
    }

    private volatile boolean stepMode = false;

    public void setStepMode(boolean stepMode) {
        if (null == crclClientJInternalFrame) {
            throw new IllegalStateException("null == crclClientJInternalFrame");
        }
        crclClientJInternalFrame.setStepMode(stepMode);
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setStepping(stepMode);
        }
        this.stepMode = stepMode;
    }

    public boolean isStepMode() {
        return this.stepMode;
//        if(null == crclClientJInternalFrame) {
//            throw new IllegalStateException("null == crclClientJInternalFrame");
//        }
//        return crclClientJInternalFrame.isStepMode();
    }

    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    @Override
    public List<PartsTray> getPartsTrayList() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getPartsTrayList();
    }

    /**
     * Force the Object 2D Simulation View to refresh, this method has no effect
     * if the view is not visible or is not in simulation mode.
     */
    @Override
    public void refreshSimView() {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }

    @Override
    public long getLastSimViewRefreshTime() {
        if (null == object2DViewJInternalFrame) {
            return -1;
        }
        return object2DViewJInternalFrame.getLastRefreshTime();
    }

    @Override
    public long getLastSimViewPublishTime() {
        if (null == object2DViewJInternalFrame) {
            return -1;
        }
        return object2DViewJInternalFrame.getLastPublishTime();
    }

    @Override
    public int getSimViewRefreshCount() {
        if (null == object2DViewJInternalFrame) {
            return -1;
        }
        return object2DViewJInternalFrame.getRefreshCount();
    }

    @Override
    public long getSimViewLastPublishTime() {
        if (null == object2DViewJInternalFrame) {
            return -1;
        }
        return object2DViewJInternalFrame.getLastPublishTime();
    }

    @Override
    public int getSimViewPublishCount() {
        if (null == object2DViewJInternalFrame) {
            return -1;
        }
        return object2DViewJInternalFrame.getPublishCount();
    }

    private @Nullable
    SlotOffsetProvider externalSlotOffsetProvider = null;

    /**
     * Get the value of externalSlotOffsetProvider
     *
     * @return the value of externalSlotOffsetProvider
     */
    @Nullable
    @Override
    public SlotOffsetProvider getExternalSlotOffsetProvider() {
        return externalSlotOffsetProvider;
    }

    /**
     * Set the value of externalSlotOffsetProvider
     *
     * @param externalSlotOffsetProvider new value of externalSlotOffsetProvider
     */
    @Override
    public void setExternalSlotOffsetProvider(SlotOffsetProvider externalSlotOffsetProvider) {
        this.externalSlotOffsetProvider = externalSlotOffsetProvider;
    }

    /**
     * Get a list of slots with names and relative position offsets for a given
     * kit or parts tray name.
     *
     * @param name name of the type of kit or slot tray
     * @param ignoreEmpty if false no slots being found logs a verbose error
     * message and throws IllegalStateException (good for fail fast) or if true
     * simply returns an empty list (good or display or when multiple will be
     * checked.
     * @return list of slots with relative position offsets.
     */
    @Override
    public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.getSlotOffsets(name, ignoreEmpty);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.getSlotOffsets(name, ignoreEmpty);
    }

    /**
     * Save an image showing the locations of new database items. The database
     * will be queried and the file saved asynchronously in another thread.
     *
     * @param f file to save
     * @return future with information on when the operation is complete.
     */
    @Override
    public XFutureVoid startVisionToDbNewItemsImageSave(File f) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.startNewItemsImageSave(f);
    }

    /**
     * Get a list of slots associated with a particular tray.
     *
     * @param tray tray to obtain list of slots
     * @return list of slots
     */
    @Override
    public List<Slot> getSlots(Tray tray, boolean ignoreEmpty) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.getSlotOffsets(tray.getName(), ignoreEmpty);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.getSlots(tray);
    }

    /**
     * Set the preference for displaying a '+' at the current position of the
     * robot tool.
     *
     * @param v should the position be displayed.
     */
    @Override
    public void setSimViewTrackCurrentPos(boolean v) {
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        this.object2DViewJInternalFrame.setTrackCurrentPos(v);
    }

    /**
     * Set the simulation view to simulation mode and disconnect from any vision
     * data socket.
     */
    @Override
    public void simViewSimulateAndDisconnect() {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setSimulatedAndDisconnect();
        }
    }

    /**
     * Get a rotational offset in radians between the vision system and the
     * database coordinate system.
     *
     * @return rotational offset in radians
     */
    @Override
    public double getVisionToDBRotationOffset() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.getRotationOffset();
    }

    /**
     * Pause a currently executing CRCL program.
     * <p>
     * No action is taken if the CRCL Client has not been started and connected
     * or not program was currently running.
     */
    @Override
    public void pauseCrclProgram() {
        if (null != crclClientJInternalFrame) {
            crclClientJInternalFrame.pauseCrclProgram();
            LauncherAprsJFrame.PlayAlert2();
        }
    }

    /**
     * Get the Kit Inspection InternalJFrame if it has been created.
     *
     * @return Kit Inspection InternalJFrame
     */
    @Nullable
    @Override
    public KitInspectionJInternalFrame getKitInspectionJInternalFrame() {
        return kitInspectionJInternalFrame;
    }

    private final AtomicLong runNumber = new AtomicLong((System.currentTimeMillis() / 10000) % 1000);

    /**
     * Return the user's preference on whether the stack trace be dumped for
     * more verbose logging.
     *
     * @return current setting of menu item
     */
    @Override
    public boolean isEnableDebugDumpStacks() {
        return debug;
    }

    @Nullable private volatile String runName = null;

    /**
     * Creates a run name useful for identifying a run in log files or the names
     * of snapshot files. The name is composed of the current robot name, task
     * name and robot number.
     *
     * @return current run name.
     */
    @Override
    public String getRunName() {
        String rn = this.runName;
        if (null != rn) {
            return rn;
        }
        String ret = ((this.taskName != null) ? this.taskName.replace(" ", "-") : "") + "-run-" + String.format("%03d", runNumber.get()) + "-"
                + ((this.robotName != null) ? this.robotName : "") + (isReverseFlag() ? "-Reverse" : "");
        this.runName = ret;
        return ret;
    }

    /**
     * Checks whether there is currently a CRCL program that is loaded but
     * paused.
     *
     * @return whether a program is paused
     */
    @Override
    public boolean isCrclProgramPaused() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.isPaused();
        }
        return false;
    }

    /**
     * Get the thread currently running a CRCL program if it exists and is
     * known. Only useful for debugging.
     *
     * @return thread
     */
    @Nullable
    @Override
    public Thread getCrclRunProgramThread() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.getRunProgramThread();
        }
        return null;
    }

    /**
     * Get the future that can be used to determine when the current CRCL
     * program is finished. Only useful for debugging.
     *
     * @return future or null if no program is running
     */
    @Nullable
    @Override
    public XFuture<Boolean> getCrclRunProgramFuture() {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
        return crclClientJInternalFrame.getRunProgramFuture();
    }

    /**
     * Checks whether there is currently a CRCL program running.
     *
     * @return whether a CRCL program is running.
     */
    @Override
    public boolean isRunningCrclProgram() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.isRunningProgram();
        }
        return false;
    }

    /**
     * Set a state where attempts to start a crcl program will be blocked.
     *
     * @return count to pass to stopBlockingCrclProgram when running crcl
     * programs should again be allowed.
     */
    @Override
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
     * @throws
     * crcl.ui.client.PendantClientInner.ConcurrentBlockProgramsException if
     * another call to has occurred start/stopBlockingCrclPrograms since the
     * corresponding call to startBlockingCrclProgram
     */
    public void stopBlockingCrclPrograms(int count) throws PendantClientInner.ConcurrentBlockProgramsException {
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
    @Nullable
    @Override
    public CRCLProgramType getCrclProgram() {
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
    @Override
    public Map<String, String> getExecutorOptions() {
        if (null == pddlExecutorJInternalFrame1) {
            return Collections.emptyMap();
        }
        return pddlExecutorJInternalFrame1.getOptions();
    }

    /**
     * Set an executor option.
     *
     * @param key name of option to set
     * @param value value option should be set to
     */
    @Override
    public void setExecutorOption(String key, String value) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setOption(key, value);
        }
    }

    public void setToolHolderOperationEnabled(boolean enable) {
        if (null == pddlExecutorJInternalFrame1) {
            throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
        }
        pddlExecutorJInternalFrame1.setToolHolderOperationEnabled(enable);
    }

    public boolean isToolHolderOperationEnabled() {
        if (null == pddlExecutorJInternalFrame1) {
            throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
        }
        return pddlExecutorJInternalFrame1.isToolHolderOperationEnabled();
    }

    /**
     * Add a position map.
     * <p>
     * The position map is similar to a transform in that it may offset
     * positions output by the executor but may also be used to change scaling
     * or correct for non uniform distortions from the sensor system or
     * imperfect kinematic functions in the robot. Multiple position maps may be
     * stacked to account for different sources of error or transformation.
     *
     * @param pm position map to be added
     */
    @Override
    public void addPositionMap(PositionMap pm) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.addPositionMap(pm);
        }

    }

    /**
     * Remove a previously added position map.
     *
     * @param pm position map to be removed.
     */
    @Override
    public void removePositionMap(PositionMap pm) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.removePositionMap(pm);
        }
    }

    /**
     * Get the current pose of the robot.
     *
     * @return current pose of the robot.
     */
    @Nullable
    @Override
    public PoseType getCurrentPose() {

        if (null != crclClientJInternalFrame) {
            try {
                if (!crclClientJInternalFrame.isConnected()) {
                    crclClientJInternalFrame.connectCurrent();
                }
            } catch (Exception e) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, e);
            }
            if (!crclClientJInternalFrame.isConnected()) {
                return null;
            }
            return crclClientJInternalFrame.getCurrentPose();
        }
        return null;
    }

    /**
     * Get the current point(translation only) from current pose of the robot.
     *
     * @return current point(translation only) from current pose of the robot.
     */
    @Nullable
    @Override
    public PointType getCurrentPosePoint() {
        PoseType pose = getCurrentPose();
        if (null != pose) {
            return pose.getPoint();
        }
        return null;
    }

    /**
     * Check if the CRCL client is connected to the CRCL Server and therefore to
     * the robot.
     *
     * @return if the CRCL client is connected to the server.
     */
    @Override
    public boolean isConnected() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.isConnected();
        }
        return false;
    }

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
    @Override
    public void setConnected(boolean connected) {
        logEvent("setConnected", connected);
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
                } else {
                    crclClientJInternalFrame.disconnect();
                }
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

    private void updateConnectedRobotDisplay(boolean connected, @Nullable String robotName, @Nullable String crclHost, int crclPort) {
        AprsSystemDisplayJFrame displayFrame = this.aprsSystemDisplayJFrame;
        if (null != displayFrame) {
            final AprsSystemDisplayJFrame displayFrameFinal = displayFrame;
            Utils.runOnDispatchThread(() -> {
                displayFrameFinal.updateConnectedRobotDisplay(connected, robotName, crclHost, crclPort);
            });
        }
    }

    /**
     * Get the value of taskName
     *
     * @return the value of taskName
     */
    @Override
    public String getTaskName() {
        return taskName;
    }

    /**
     * Get the future that can be used to determine when the last requested safe
     * abort is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    @Nullable
    @Override
    public XFutureVoid getSafeAbortFuture() {
        return safeAbortFuture;
    }

    @Nullable private volatile XFutureVoid lastClearWayToHoldersFuture = null;

    public XFutureVoid clearWayToHolders(String holderName) {
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
    @Nullable
    @Override
    public XFuture<Boolean> getLastRunProgramFuture() {
        return lastRunProgramFuture;
    }

    /**
     * Get the future that can be used to determine when the last requested
     * resumed action is finished. Only useful for debugging.
     *
     * @return future or null if no resume requested
     */
    @Nullable
    @Override
    public XFuture<Boolean> getLastResumeFuture() {
        return lastResumeFuture;
    }

    @Nullable
    private volatile XFutureVoid safeAbortFuture = null;
    @Nullable
    private volatile XFutureVoid safeAbortAndDisconnectFuture = null;

    /**
     * Attempt to safely abort the current CRCL program in a way that does not
     * leave the part in the gripper nor in a position obstructing the vision
     * sensor. If the robot currently has a part in the gripper the program will
     * continue until the part is placed and the robot is moved out of the way
     * of the vision system.
     * <p>
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned
     * future.
     *
     * @param comment optional comment is used when debugging to track which
     * future was created by which caller and to improve log messages
     * @return a future that can be tested or used to wait until the abort is
     * completed.
     */
    @Override
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

        takeSnapshots("startSafeAbort." + comment);
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        safeAbortFuture = this.pddlExecutorJInternalFrame1.startSafeAbort(comment)
                .thenRun(() -> {
                    if (null != continuousDemoFuture) {
                        continuousDemoFuture.cancelAll(true);
                        continuousDemoFuture = null;
                    }
                    setStopRunTime();
                }).thenComposeAsyncToVoid(x -> waitAllLastFutures(), runProgramService);
        return safeAbortFuture;
    }

    @Nullable
    private volatile Thread startSafeAbortAndDisconnectThread = null;
    private volatile long startSafeAbortAndDisconnectTime = -1;
    private volatile StackTraceElement startSafeAbortAndDisconnectStackTrace@Nullable []  = null;

    @Nullable
    @SuppressWarnings({"unused"})
    private volatile Thread startSafeAbortThread = null;

    @SuppressWarnings({"unused"})
    private volatile long startSafeAbortTime = -1;

    @SuppressWarnings({"unused"})
    private volatile StackTraceElement startSafeAbortStackTrace@Nullable []  = null;

    /**
     * Safely abort the current CRCL program and then disconnect from the
     * robot's CRCL server.
     * <p>
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned
     * future. * @return a future that can be tested or used to wait until the
     * abort and disconnect is completed.
     *
     * @param comment optional comment is used when debugging to track which
     * future was created by which caller and to improve log messages
     * @return future providing info on when complete
     */
    @Override
    public XFutureVoid startSafeAbortAndDisconnect(String comment) {

        startingCheckEnabledCheck();
        if (isAborting()) {
            String errMsg = "startSafeAbort(" + comment + ") called when already aborting";
            setTitleErrorString(errMsg);
            throw new IllegalStateException(errMsg);
        }
        try {
            logEvent("startSafeAbortAndDisconnect", comment);
            Thread curThread = Thread.currentThread();
            startSafeAbortAndDisconnectThread = curThread;
            startSafeAbortAndDisconnectTime = System.currentTimeMillis();
            startSafeAbortAndDisconnectStackTrace = curThread.getStackTrace();
            if (isConnected()) {
                assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";

                XFutureVoid localSafeAbortFuture
                        = this.pddlExecutorJInternalFrame1.startSafeAbort(comment);
                safeAbortFuture
                        = localSafeAbortFuture;

                //                                    if (null != ContinuousDemoFuture) {
//                                        ContinuousDemoFuture.cancelAll(true);
//                                        ContinuousDemoFuture = null;
//                                    }
                safeAbortAndDisconnectFuture
                        = localSafeAbortFuture
                                .thenRun(this::setStopRunTime)
                                .thenCompose(x -> waitAllLastFutures())
                                .thenRunAsync(localSafeAbortFuture.getName() + ".disconnect." + robotName, this::disconnectRobotPrivate, runProgramService)
                                .thenComposeAsyncToVoid(x -> waitAllLastFutures(), runProgramService);
            } else {
                safeAbortFuture = XFutureVoid.completedFutureWithName("startSafeAbortAndDisconnect(" + comment + ").alreadyDisconnected");
                safeAbortAndDisconnectFuture = safeAbortFuture;
            }
            return safeAbortAndDisconnectFuture.always(() -> logEvent("finished startSafeAbortAndDisconnect", comment));
        } catch (Exception e) {
            setTitleErrorString(e.toString());
            XFutureVoid ret = new XFutureVoid("startSafeAbortAndDisconnect." + e.toString());
            ret.completeExceptionally(e);
            return ret;
        }
    }

    @SuppressWarnings({"unchecked", "nullness"})
    private XFuture<?> wait(@Nullable XFuture<?> f) {
        if (null == f || f.isCancelled() || f.isCompletedExceptionally() || f.isDone()) {
            return XFutureVoid.completedFutureWithName("waitReady f=" + f);
        } else {
            return f.handle((x, t) -> null);
        }
    }

    private XFutureVoid waitAllLastFutures() {
        return XFutureVoid.allOf(wait(lastContinueActionListFuture),
                wait(lastRunProgramFuture),
                wait(lastStartActionsFuture));
    }

    /**
     * Get a map of updates that were attempted the last time data was received
     * from the vision system.
     * <p>
     * Only useful for debugging.
     *
     * @return map of results
     */
    @Override
    public Map<String, UpdateResults> getDbUpdatesResultMap() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getUpdatesResultMap();
    }

    @Nullable
    private volatile XFutureVoid disconnectRobotFuture = null;

    private volatile boolean debug = false;

    /**
     * Disconnect from the robot's crcl server and set robotName to null.
     * <p>
     * Note: setConnected(false) also disconnects from the crcl server but
     * leaves the robotName unchanged.
     *
     * @return future providing info on when complete
     */
    public XFutureVoid disconnectRobot() {
        startingCheckEnabledCheck();
        disconnectRobotCount.incrementAndGet();
        checkReadyToDisconnect();
        enableCheckedAlready = false;
        XFutureVoid ret = waitForPause().
                thenRunAsync("disconnectRobot(" + getRobotName() + ")", this::disconnectRobotPrivate, connectService);
        this.disconnectRobotFuture = ret;
        if (debug) {
            System.out.println("disconnectRobotFuture = " + disconnectRobotFuture);
            System.out.println("connectService = " + connectService);
        }
        return ret;
    }

    private final AtomicInteger disconnectRobotCount = new AtomicInteger();

    @Override
    public boolean isClosing() {
        return closing;
    }

    private void disconnectRobotPrivate() {
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
        }
        if (null != getRobotName()) {
            takeSnapshots("disconnectRobot");
        }
        this.setRobotName(null);
        System.out.println("disconnectRobot completed");
        AprsSystemDisplayJFrame displayFrame = this.aprsSystemDisplayJFrame;
        if (null != displayFrame) {
            final AprsSystemDisplayJFrame displayFrameFinal = displayFrame;
            Utils.runOnDispatchThread(() -> displayFrameFinal.setConnectedRobotCheckBox(isConnected()));
        }
    }

    private void startingCheckEnabledCheck() throws IllegalStateException {
        if (startingCheckEnabled && !closing) {
            String errMsg = "trying to change robot name while starting a check enabled";
            setTitleErrorString(errMsg);
            throw new IllegalStateException(errMsg);
        }
    }

    private volatile String origCrclRobotHost;

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

    /**
     * Connect to a given robot with a CRCL server running on the given host and
     * TCP port.
     *
     * @param robotName name of the robot
     * @param host host running robot's CRCL server
     * @param port (TCP) port robot's CRCL server is bound to
     * @return future providing info on when complete
     */
    public XFutureVoid connectRobot(String robotName, String host, int port) {
        maybeSetOrigRobotName(robotName);
        maybeSetOrigCrclRobotHost(host);
        maybeSetOrigCrclRobotPort(port);
        if (isConnected()
                && !isPaused()
                && null != this.robotName
                && Objects.equals(robotName, robotName)
                && Objects.equals(this.getRobotCrclHost(), host)
                && this.getRobotCrclPort() == port) {
            updateConnectedRobotDisplay(isConnected(), robotName, host, port);
            return XFutureVoid.completedFuture();
        }
        logEvent("connectRobot", robotName + " -> " + host + ":" + port);
        enableCheckedAlready = false;
        return waitForPause().
                thenRunAsync(() -> connectRobotPrivate(robotName, host, port), connectService)
                .always(() -> logEvent("finished connectRobot", robotName + " -> " + host + ":" + port));
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

    private void connectRobotPrivate(String robotName, String host, int port) {
        setThreadName();
        enableCheckedAlready = false;
        PendantClientJInternalFrame frm = crclClientJInternalFrame;
        if (null != frm) {
            setRobotName(robotName);
            frm.connect(host, port);
        }
        maybeSetOrigCrclRobotHost(getRobotCrclHost());
        maybeSetOrigCrclRobotPort(getRobotCrclPort());
        updateConnectedRobotDisplay(isConnected(), robotName, host, port);
    }

    /**
     * Get robot's CRCL host.
     *
     * @return robot's CRCL host.
     */
    @Nullable
    public String getRobotCrclHost() {
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

    @Nullable
    private volatile XFutureVoid lastPauseFuture = null;

    private XFutureVoid waitForPause() {
        boolean paused = isPaused();
        XFutureVoid pauseFuture = new XFutureVoid("pauseFuture." + paused);
        if (paused) {
            System.out.println("adding " + pauseFuture + " to " + futuresToCompleteOnUnPause);
            futuresToCompleteOnUnPause.add(pauseFuture);
        } else {
            pauseFuture.complete(null);
        }
        lastPauseFuture = pauseFuture;
        return pauseFuture;
    }

    @Nullable
    private volatile XFuture<Boolean> lastContinueActionListFuture = null;

    @Nullable
    @SuppressWarnings({"unused"})
    private volatile String lastContinueActionListFutureComment = null;

    @SuppressWarnings({"unused"})
    private volatile int lastContinueStartAbortCount = -1;

    /**
     * Continue or start executing the currently loaded set of PDDL actions.
     * <p>
     * The actions will be executed in another thread after this method returns.
     * The task can be monitored or canceled using the returned future.
     *
     * @param comment optional comment for why continueActionList is needed used
     * for logs, and/or snapshots
     * @return a future that can be used to monitor, extend or cancel the
     * underlying task.
     */
    public XFuture<Boolean> continueActionList(String comment) {
        setStartRunTime();
        lastContinueActionListFutureComment = comment;
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        lastContinueStartAbortCount = startAbortCount;
        logEvent("continueActionList", comment);
        lastContinueActionListFuture
                = waitForPause()
                        .thenApplyAsync("AprsSystem.continueActionList" + comment,
                                x -> {
                                    setThreadName();
                                    takeSnapshots("continueActionList" + ((comment != null) ? comment : ""));
                                    assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                                    if (pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount) {
                                        return pddlExecutorJInternalFrame1.completeActionList("continueActionList" + comment, startAbortCount) && (pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount);
//                                        (Boolean calRet) -> calRet && (pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount));
                                    }
                                    return false;
                                }, runProgramService);
        return lastContinueActionListFuture.always(() -> logEvent("finished continueActionList", comment));
    }

    /**
     * Get the closest distance between the robot TCP and any part.
     *
     * @return closest distance
     */
    @Override
    public double getClosestRobotPartDistance() {
        assert (null != object2DViewJInternalFrame) : "null == object2DViewJInternalFrame ";
        return this.object2DViewJInternalFrame.getClosestRobotPartDistance();
    }

    /**
     * Get the current setting for whether the object view is using simulation.
     *
     * @return if object view is simulated
     */
    @Override
    public boolean isObjectViewSimulated() {
        assert (null != object2DViewJInternalFrame) : "null == object2DViewJInternalFrame ";
        return this.object2DViewJInternalFrame.isSimulated();
    }

    /**
     * Set the value of taskName
     *
     * @param taskName new value of taskName
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
        clearLogDirCache();
        updateTitle("", "");
    }

    private void clearLogDirCache() {
        this.runName = null;
        this.logDir = null;
        this.logCrclProgramDir = null;
        this.logImageDir = null;
    }

    @Nullable
    private String robotName = null;

    /**
     * Get the value of robotName
     *
     * @return the value of robotName
     */
    @Nullable
    public String getRobotName() {
        return robotName;
    }

    @Nullable
    private volatile Thread setRobotNameNullThread = null;
    private volatile StackTraceElement setRobotNameNullStackTrace@Nullable []  = null;
    private volatile long setRobotNameNullThreadTime = -1;
    @Nullable
    private volatile Thread setRobotNameNonNullThread = null;
    private volatile StackTraceElement setRobotNameNonNullStackTrace@Nullable []  = null;
    private volatile long setRobotNameNonNullThreadTime = -1;

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
        Utils.runOnDispatchThread(() -> updateTitle("", ""));
    }

    private void maybeSetOrigRobotName(@Nullable String robotName1) {
        if (null == this.origRobotName || this.origRobotName.length() < 1) {
            if (null != robotName1 && robotName1.length() > 0) {
                this.origRobotName = robotName1;
            }
        }
    }

    private volatile boolean useTeachTable = false;

    @Override
    public boolean getUseTeachTable() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.getUseTeachTable();
            this.useTeachTable = ret;
            return ret;
        }
        return useTeachTable;
    }

    @Override
    public void setUseTeachTable(boolean useTeachTable) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setUseTeachTable(useTeachTable);
        }
        this.useTeachTable = useTeachTable;
    }

    @SuppressWarnings("unused")
    private void checkReadyToDisconnect() throws IllegalStateException {
        if (closing) {
            return;
        }
        if (isRunningCrclProgram()) {
            String msg = "setRobotName(null)/disconnect() called when running crcl program";
            setTitleErrorString(msg);
            boolean chk = isRunningCrclProgram();
            throw new IllegalStateException(msg);
        }
        if (isDoingActions()) {
            String msg = "setRobotName(null)/disconnect() called when running doing actions";
            setTitleErrorString(msg);
            boolean chk = isDoingActions();
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Register a program line listener that will be notified each time a line
     * of the CRCL program is executed.
     *
     * @param l listener to be registered
     */
    @Override
    public void addProgramLineListener(PendantClientJPanel.ProgramLineListener l) {
        if (null != crclClientJInternalFrame) {
            crclClientJInternalFrame.addProgramLineListener(l);
        }
    }

    /**
     * Remove a previously added program line listener.
     *
     * @param l listener to be removed
     */
    @Override
    public void removeProgramLineListener(PendantClientJPanel.ProgramLineListener l) {
        if (null != crclClientJInternalFrame) {
            crclClientJInternalFrame.removeProgramLineListener(l);
        }
    }

    /**
     * Modify the given pose by applying all of the currently added position
     * maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    @Override
    public PoseType correctPose(PoseType poseIn) {
        if (null != pddlExecutorJInternalFrame1) {
            return pddlExecutorJInternalFrame1.correctPose(poseIn);
        }
        return poseIn;
    }

    /**
     * Apply inverses of currently added position maps in reverse order.
     *
     * @param ptIn pose to reverse correction
     * @return pose in original vision/database coordinates
     */
    @Override
    public PointType reverseCorrectPoint(PointType ptIn) {
        if (null != pddlExecutorJInternalFrame1) {
            return pddlExecutorJInternalFrame1.reverseCorrectPoint(ptIn);
        }
        return ptIn;
    }

    private final List<PendantClientJPanel.CurrentPoseListener> unaddedPoseListeners = new ArrayList<>();

    /**
     * Register a listener to be notified of every change in the robots pose.
     *
     * @param l listener to register
     */
    @Override
    public void addCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l) {
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
    @Override
    public void removeCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l) {
        if (null != crclClientJInternalFrame) {
            crclClientJInternalFrame.removeCurrentPoseListener(l);
        }
    }

    /**
     * Set the currently loaded CRCL program
     *
     * @param program CRCL program to load. in the schema
     */
    @Override
    public synchronized void setCRCLProgram(CRCLProgramType program) throws JAXBException {
        if (null != crclClientJInternalFrame) {
            setProgram(program);
        }
    }

    @Nullable
    private volatile XFuture<Boolean> lastRunProgramFuture = null;

    /**
     * Load the given program an begin running it.
     * <p>
     * The program will be run asynchronously in another thread after this
     * method has returned. The task can be modified, canceled or extended with
     * the returned future. The boolean contained within the future will be true
     * if the program completed successfully and false for non exceptional
     * errors.
     *
     * @param program program to be loaded
     * @return future that can be used to monitor, cancel or extend the
     * underlying task
     */
    @Override
    public XFuture<Boolean> startCRCLProgram(CRCLProgramType program) {
        setStartRunTime();
        if (null != crclClientJInternalFrame) {
            lastRunProgramFuture
                    = waitForPause()
                            .thenApplyAsync("startCRCLProgram(" + program.getName() + ").runProgram", x -> {
                                try {
                                    return runCrclProgram(program);
                                } catch (JAXBException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }, runProgramService);
            return lastRunProgramFuture;
        }
        XFuture<Boolean> ret = new XFuture<>("startCRCLProgram.pendantClientJInternalFrame==null");
        ret.completeExceptionally(new IllegalStateException("null != pendantClientJInternalFrame"));
        return ret;
    }

    private boolean runCrclProgram(CRCLProgramType program) throws JAXBException {
        setThreadName();
        takeSnapshots("startCRCLProgram(" + program.getName() + ")");
        setProgram(program);
        assert (null != crclClientJInternalFrame) : "null == pendantClientJInternalFrame ";
        return crclClientJInternalFrame.runCurrentProgram(stepMode);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isMove(MiddleCommandType midCmd) {
        if (midCmd instanceof MoveToType) {
            return true;
        }
        if (midCmd instanceof MoveThroughToType) {
            return true;
        }
        if (midCmd instanceof ActuateJointsType) {
            return true;
        }
        if (midCmd instanceof SetEndEffectorType) {
            return true;
        }
        return false;
    }

    private boolean checkNoMoves(CRCLProgramType program) {
        for (MiddleCommandType midCmd : program.getMiddleCommand()) {
            if (isMove(midCmd)) {
                return false;
            }
            if (midCmd instanceof CrclCommandWrapper) {
                CrclCommandWrapper wrapper = (CrclCommandWrapper) midCmd;
                if (isMove(wrapper.getWrappedCommand())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void processWrapperCommands(CRCLProgramType program) {
        List<MiddleCommandType> cmds = program.getMiddleCommand();
        processWrapperCommands(cmds);
    }

    @Override
    public void processWrapperCommands(List<MiddleCommandType> cmds) {
        for (MiddleCommandType cmd : cmds) {
            if (cmd instanceof CrclCommandWrapper) {
                CrclCommandWrapper wrapper = (CrclCommandWrapper) cmd;
                wrapper.notifyOnStartListeners();
                wrapper.notifyOnDoneListeners();
            }
        }
    }

    /**
     * Run a CRCL program.
     *
     * @param program CRCL program to run
     * @return whether the program completed successfully
     * @throws JAXBException the program did not meet schema requirements
     */
    @Override
    public boolean runCRCLProgram(CRCLProgramType program) throws JAXBException {
        if (enableCheckedAlready && checkNoMoves(program)) {
            logEvent("skipping runCrclProgram", programToString(program));
            processWrapperCommands(program);
            return true;
        }
        long startTime = logEvent("start runCrclProgram", programToString(program));
        setProgram(program);
        assert (null != crclClientJInternalFrame) : "null == pendantClientJInternalFrame ";
        boolean ret = crclClientJInternalFrame.runCurrentProgram(stepMode);
        logEvent("end runCrclProgram",
                "(" + crclClientJInternalFrame.getCurrentProgramLine() + "/" + program.getMiddleCommand().size() + ")"
                + "\n started at" + startTime
                + "\n timeDiff=" + (startTime - System.currentTimeMillis())
        );
        return ret;
    }

    /**
     * Immediately abort the currently running CRCL program and PDDL action
     * list.
     * <p>
     * NOTE: This may leave the robot in a state with the part held in the
     * gripper or with the robot obstructing the view of the vision system.
     */
    public void immediateAbort() {
        if (null != this.continuousDemoFuture) {
            this.continuousDemoFuture.cancelAll(true);
            this.continuousDemoFuture = null;
        }
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.abortProgram();
        } else {
            abortCrclProgram();
        }
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
        if (null != lastContinueActionListFuture) {
            lastContinueActionListFuture.cancelAll(true);
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
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setContinuousDemoCheckbox(false);
        }
    }

    /**
     * Create a string with current details for display and/or logging.
     *
     * @return details string
     */
    @Override
    public String getDetailsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.toString()).append("\r\n");
        if (null != crclClientJInternalFrame) {
            CRCLCommandType cmd = crclClientJInternalFrame.getCurrentProgramCommand();
            if (null != cmd) {
                sb.append("crcl_cmd=").append(CRCLSocket.commandToSimpleString(cmd)).append("\r\n");
            } else {
                sb.append("crcl_cmd= \r\n");
            }
            CRCLProgramType prog = getCrclProgram();
            if (null != prog) {
                sb.append("crcl_program_index=(").append(getCrclProgramLine()).append(" / ").append(prog.getMiddleCommand().size()).append("), ");
                sb.append("crcl_program_name=").append(prog.getName()).append("\r\n");
            } else {
                sb.append("crcl_program_index=").append(getCrclProgramLine()).append(", ");
                sb.append("crcl_program_name=\r\n");
            }
            if (null != crclClientJInternalFrame) {
                crclClientJInternalFrame.getCurrentStatus().ifPresent(status -> {
                    if (null != status.getCommandStatus()
                            && null != status.getCommandStatus().getStateDescription()
                            && status.getCommandStatus().getStateDescription().length() > 0) {
                        sb.append("state description = ").append(status.getCommandStatus().getStateDescription()).append("\r\n");
                    }
                });
            }
//            pendantClientJInternalFrame.getCurrentState().ifPresent(state -> sb.append("state=").append(state).append("\r\n"));
        }
        ExecutorJInternalFrame execFrame = this.pddlExecutorJInternalFrame1;
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
//        sb.append("                                                                                                                                                                                                                                                                                        \r\n");

        String currentTitleErrorString = this.titleErrorString;
        if (null != currentTitleErrorString && currentTitleErrorString.length() > 0) {
            sb.append("titleErrorString=").append(currentTitleErrorString).append("\r\n");
        }
//        sb.append("1111111111222222222233333333334444444444555555555566666666667777777777788888888899999999990000000000111111111122222222223333333333\r\n");
//        sb.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\r\n");
        return sb.toString().trim();
    }

    /**
     * Abort the current CRCL program.
     */
    @Override
    public void abortCrclProgram() {
        if (null != crclClientJInternalFrame && crclClientJInternalFrame.isConnected()) {
            crclClientJInternalFrame.abortProgram();
        }
    }

    @Nullable
    private PrintStream origOut = null;
    @Nullable
    private PrintStream origErr = null;

    @Override
    public void setVisible(boolean visible) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setVisible(visible);
        }
    }

    @Override
    public void setDefaultCloseOperation(int operation) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setDefaultCloseOperation(operation);
        }
    }

    @Override
    public List<PhysicalItem> getAvailableToolHolders() {
        if (null == pddlExecutorJInternalFrame1) {
            throw new IllegalStateException("null == pddlExecutorJInternalFrame");
        }
        return pddlExecutorJInternalFrame1.getAvailableToolHolders();
    }

    @Override
    public List<PhysicalItem> getToolsInHolders() {
        if (null == pddlExecutorJInternalFrame1) {
            throw new IllegalStateException("null == pddlExecutorJInternalFrame");
        }
        return pddlExecutorJInternalFrame1.getToolsInHolders();
    }

    static private class MyPrintStream extends PrintStream {

        final private PrintStream ps;
        final private LogDisplayJInternalFrame logDisplayJInternalFrame;

        MyPrintStream(PrintStream ps, LogDisplayJInternalFrame logDisplayJInternalFrame) {
            super(ps, true);
            this.ps = ps;
            this.logDisplayJInternalFrame = logDisplayJInternalFrame;
            if (null == logDisplayJInternalFrame) {
                throw new IllegalArgumentException("logDisplayJInteralFrame may not be null");
            }
            if (null == ps) {
                throw new IllegalArgumentException("PrintStream ps may not be null");
            }
        }

        private StringBuffer sb = new StringBuffer();

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (null != logDisplayJInternalFrame) {
                final String s = new String(buf, off, len);
                sb.append(s);
                if (s.contains("\n")) {
                    String fullString = sb.toString();
                    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                        logDisplayJInternalFrame.appendText(fullString);
                    } else {

                        javax.swing.SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                logDisplayJInternalFrame.appendText(fullString);
                            }
                        });
                    }
                    sb = new StringBuffer();
                }
            }
        }
    }

    private int fanucCrclPort = CRCLSocket.DEFAULT_PORT;
    private int motomanCrclPort = CRCLSocket.DEFAULT_PORT;
    private final String fanucNeighborhoodName = "AgilityLabLRMate200iD"; // FIXME hard-coded default
    private final boolean fanucPreferRNN = false;
    private String fanucRobotHost = System.getProperty("fanucRobotHost", "192.168.1.34");// "129.6.78.111"; // FIXME hard-coded default

    public XFutureVoid startFanucCrclServer() {
        try {
            FanucCRCLMain newFanucCrclMain = new FanucCRCLMain();
            if (null == fanucCRCLServerJInternalFrame) {
                fanucCRCLServerJInternalFrame = new FanucCRCLServerJInternalFrame();
                addInternalFrame(fanucCRCLServerJInternalFrame);
            }
            newFanucCrclMain.setDisplayInterface(fanucCRCLServerJInternalFrame);
            return newFanucCrclMain.startDisplayInterface()
                    .thenCompose(() -> newFanucCrclMain.start(fanucPreferRNN, fanucNeighborhoodName, fanucRobotHost, fanucCrclPort))
                    .thenRun(() -> {
                        this.fanucCRCLMain = newFanucCrclMain;
                    });
        } catch (CRCLException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @Nullable
    private String titleErrorString = null;

    /**
     * Get the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @return title error string
     */
    @Nullable
    @Override
    public String getTitleErrorString() {
        return titleErrorString;
    }

    @Nullable
    private volatile CommandStatusType titleErrorStringCommandStatus = null;

    @Nullable
    private volatile String lastNewTitleErrorString = null;

    private volatile StackTraceElement setTitleErrorStringTrace@Nullable []  = null;

    /**
     * Get the stack trace the last time the title error was set.
     *
     * @return stack trace or null if no error has been set.
     */
    @Override
    public StackTraceElement @Nullable [] getSetTitleErrorStringTrace() {
        return setTitleErrorStringTrace;
    }

    /**
     * Set the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @param newTitleErrorString title error string
     */
    public void setTitleErrorString(@Nullable String newTitleErrorString) {

        if (!Objects.equals(lastNewTitleErrorString, newTitleErrorString)) {
            if (!Objects.equals(this.titleErrorString, newTitleErrorString)) {
                logEvent("setTitleErrorString", newTitleErrorString);
                titleErrorStringCommandStatus = getCommandStatus();
                if (null != this.titleErrorString
                        && null != newTitleErrorString
                        && this.titleErrorString.length() > 0
                        && newTitleErrorString.length() > 0) {
                    if (this.titleErrorString.length() > 200) {
                        this.titleErrorString = this.titleErrorString.substring(0, 200) + " ...\n" + newTitleErrorString;
                    } else {
                        this.titleErrorString = this.titleErrorString + " ...\n" + newTitleErrorString;
                    }
                } else {
                    this.titleErrorString = newTitleErrorString;
                }
                updateTitle();
                if (null != newTitleErrorString && newTitleErrorString.length() > 0) {
                    setTitleErrorStringTrace = Thread.currentThread().getStackTrace();
                    System.err.println("RunName=" + getRunName());
                    System.err.println(newTitleErrorString);
                    Thread.dumpStack();
                    LauncherAprsJFrame.PlayAlert2();
                    boolean snapshotsEnabled = this.isSnapshotsEnabled();
                    if (!snapshotsEnabled) {
                        setSnapshotsEnabled(true);
                    }
                    takeSnapshots("setTitleError_" + newTitleErrorString + "_");
                    pause();
                    if (!snapshotsEnabled) {
                        setSnapshotsEnabled(false);
                    }
                }
            }
            lastNewTitleErrorString = newTitleErrorString;
        }
        this.asString = getTitle();
    }

    public void startMotomanCrclServer() {
        try {
            if (null == motomanCrclServerJInternalFrame) {
                MotomanCrclServerJInternalFrame newMotomanCrclServerJInternalFrame = new MotomanCrclServerJInternalFrame();
                this.motomanCrclServerJInternalFrame = newMotomanCrclServerJInternalFrame;
                updateSubPropertiesFiles();
                newMotomanCrclServerJInternalFrame.loadProperties();
                newMotomanCrclServerJInternalFrame.connectCrclMotoplus();
                newMotomanCrclServerJInternalFrame.setVisible(true);
            }
            addInternalFrame(motomanCrclServerJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            setTitleErrorString("Error starting motoman crcl server:" + ex.getMessage());
            if (null != motomanCrclServerJInternalFrame) {
                motomanCrclServerJInternalFrame.setVisible(true);
                addInternalFrame(motomanCrclServerJInternalFrame);
            }
        }
    }

    private void addInternalFrame(JInternalFrame internalFrame) {
        internalFrame.pack();
        internalFrame.setVisible(true);
        addToDesktopPane(internalFrame);
        maximizeJInteralFrame(internalFrame);
        setupWindowsMenu();
    }

    private void addToDesktopPane(JInternalFrame internalFrame) {
        try {
            if (null != aprsSystemDisplayJFrame) {
                aprsSystemDisplayJFrame.addToDesktopPane(internalFrame);
            }
        } catch (Exception e) {
            System.out.println("Thread.currentThread() = " + Thread.currentThread());
            System.out.println("internalFrame = " + internalFrame);
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
    }

    final static private AtomicInteger runProgramThreadCount = new AtomicInteger();

    private final int myThreadId = runProgramThreadCount.incrementAndGet();

    @Nullable
    private volatile CSVPrinter eventLogPrinter = null;

    private volatile long lastTime = -1;
    private final AtomicInteger logNumber = new AtomicInteger();

    @Nullable
    private volatile String lastLogEvent = null;

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public long logEvent(String s, @Nullable Object arg) {
        try {
            if (closing) {
                return -1;
            }
            lastLogEvent = s;
            CSVPrinter printer = eventLogPrinter;

            if (null == printer) {

                printer = new CSVPrinter(new FileWriter(Utils.createTempFile(getRunName(), ".csv")),
                        CSVFormat.DEFAULT.withHeader("number", "time", "timediff", "event", "arg", "runDuration", "stopDuration", "thread", "runName"));
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
                if (argStringSplit.length < 2) {
                    printer.printRecord(logNumber.incrementAndGet(), curTime, diff, s, argString.trim(), getRunDuration(), getStopDuration(), Thread.currentThread(), getRunName());
                } else {
                    printer.printRecord(logNumber.incrementAndGet(), curTime, diff, s, argStringSplit[0].trim(), getRunDuration(), getStopDuration(), Thread.currentThread(), getRunName());
                    for (int i = 1; i < argStringSplit.length; i++) {
                        printer.printRecord("", "", "", "", argStringSplit[i].trim(), "", "", "", "");
                    }
                }
            }
            return curTime;
        } catch (IOException ex) {
            if (closing) {
                return -1;
            }
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    private String programToString(CRCLProgramType prog) {
        StringBuilder sb = new StringBuilder();
        List<MiddleCommandType> midCmds = prog.getMiddleCommand();
        for (int i = 0; i < midCmds.size(); i++) {
            sb.append(String.format("%03d", i));
            sb.append(" \t");
            sb.append(CRCLSocket.commandToSimpleString(midCmds.get(i)));
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Get the unique thread id number for this system. It is set only when this
     * object is created.
     *
     * @return thread id
     */
    @Override
    public int getMyThreadId() {
        return myThreadId;
    }

    private String getThreadName() {
        return "AprsJframe." + myThreadId + "." + getRunName();
    }

    private void setThreadName() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            Thread.currentThread().setName(getThreadName());
        }
    }

    private final ExecutorService defaultRunProgramService
            = Executors.newSingleThreadExecutor(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, getThreadName());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private final ExecutorService runProgramService = defaultRunProgramService;
    private final ExecutorService connectService = defaultRunProgramService;
    @Nullable
    private XFutureVoid connectDatabaseFuture = null;

    /**
     * Get the ExecutorService used for running CRCL programs.
     *
     * @return run program service
     */
    @Override
    public ExecutorService getRunProgramService() {
        return runProgramService;
    }

    public XFuture<Boolean> startConnectDatabase() {
        assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
        XFuture<Boolean> f = waitDbConnected();
        if (closing) {
            throw new IllegalStateException("Attempt to start connect database when already closing.");
        }
        System.out.println("Starting connect to database ...   : propertiesFile=" + dbSetupJInternalFrame.getPropertiesFile());
        DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
        DbSetup setup = dbSetupPublisher.getDbSetup();
        if (setup.getDbType() == null || setup.getDbType() == DbType.NONE) {
            throw new IllegalStateException("Can not connect to database with setup.getDbType() = " + setup.getDbType());
        }
        connectDatabaseFuture = connectDatabase();
        //connectService.submit(this::connectDatabase);
        return f;
    }

    private XFutureVoid connectDatabase() {
        List<XFutureVoid> futures = null;
        try {
//            if (!isConnectDatabaseCheckboxSelected()) {
//                XFutureVoid xf = new XFutureVoid("connectDatabase!isConnectDatabaseCheckboxSelected()");
//                xf.cancelAll(false);
//                return xf;
//            }
            assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            String startScript = dbSetupJInternalFrame.getStartScript();
            if (null != startScript && startScript.length() > 0) {
                System.out.println();
                System.err.println();
                System.out.flush();
                System.err.flush();
                System.out.println("Excecuting Database startScript=\r\n\"" + startScript + "\"");
                System.out.println();
                System.err.println();
                System.out.flush();
                System.err.flush();
                ProcessBuilder pb = new ProcessBuilder(startScript.split("[ ]+"));
                pb.inheritIO().start().waitFor();
                System.out.println();
                System.err.println();
                System.out.flush();
                System.err.flush();
            }
            DbSetup setup = dbSetupPublisher.getDbSetup();
            if (setup.getDbType() == null || setup.getDbType() == DbType.NONE) {
                throw new IllegalStateException("Can not connect to database with setup.getDbType() = " + setup.getDbType());
            }
            if (setup.getQueriesMap().isEmpty()) {
                throw new IllegalStateException("Can not connect to database with getQueriesMap().isEmpty()");
            }
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
            futures = dbSetupPublisher.notifyAllDbSetupListeners(null);
            XFutureVoid futuresArray[] = futures.toArray(new XFutureVoid[0]);
            XFutureVoid xfAll = XFutureVoid.allOf(futuresArray);
            return xfAll.thenRun(() -> {
                setConnectDatabaseCheckboxSelected(true);
                setConnectDatabaseCheckboxEnabled(true);
                System.out.println("Finished connect to database.");
            });
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            if (null != futures) {
                for (Future<?> f : futures) {
                    f.cancel(true);
                }
            }
            throw new RuntimeException(ex);
        }
    }

    public File getPropertiesDirectory() {
        return propertiesDirectory;
    }

    private void setConnectDatabaseCheckboxEnabled(boolean enable) {
        if (null != this.aprsSystemDisplayJFrame) {
            this.aprsSystemDisplayJFrame.setConnectDatabaseCheckboxEnabled(enable);
        }
    }

    public void stopSimUpdateTimer() {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.stopSimUpdateTimer();
        }
    }

    private volatile boolean connectDatabaseCheckboxSelected = false;

    private boolean isConnectDatabaseCheckboxSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isConnectDatabaseCheckboxSelected();
            this.connectDatabaseCheckboxSelected = ret;
            return ret;
        }
        return connectDatabaseCheckboxSelected;
    }

    private void setConnectDatabaseCheckboxSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setConnectDatabaseCheckboxSelected(selected);
        }
        this.connectDatabaseCheckboxSelected = selected;
    }

    public XFutureVoid connectVision() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect vision when already closing.");
        }
        if (null == visionToDbJInternalFrame) {
            throw new IllegalStateException("null == visionToDbJInternalFrame");

        }
        return Utils.runOnDispatchThread(visionToDbJInternalFrame::connectVision);
    }

    public void disconnectVision() {
        if (null != visionToDbJInternalFrame) {
            Utils.runOnDispatchThread(visionToDbJInternalFrame::disconnectVision);
        }
    }

    /**
     * Creates new AprsSystem using a default properties file.
     */
    @SuppressWarnings("initialization")
    private AprsSystem() {
        this(null);
    }

    /**
     * Creates new AprsSystem using a default properties file.
     *
     * @param aprsSystemDisplayJFrame1 swing gui to show edit properties
     */
    @SuppressWarnings("initialization")
    private AprsSystem(@Nullable AprsSystemDisplayJFrame aprsSystemDisplayJFrame1) {
        this.aprsSystemDisplayJFrame = aprsSystemDisplayJFrame1;
        try {
            initPropertiesFileInfo();
            this.asString = getTitle();
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setTitle(String newTitle) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setTitle(newTitle);
        }
    }

    private String getTitle() {
        if (null != aprsSystemDisplayJFrame) {
            return aprsSystemDisplayJFrame.getTitle();
        }
        return "";
    }

    /**
     * Initialize the frame with the previously saved settings if available.
     */
    private XFutureVoid defaultInit() {
        initLoggerWindow();
        try {
//            initPropertiesFile();
            if (propertiesFile.exists()) {
                loadProperties();
            } else {
                saveProperties();
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        return commonInit();
    }

    private void clearStartCheckBoxes() {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.clearStartCheckBoxes();
        }
    }

    /**
     * Initialize the frame ignoring any previously saved settings.
     */
    private XFutureVoid emptyInit() {
        skipCreateDbSetupFrame = true;
        clearStartCheckBoxes();
        initLoggerWindow();
        return commonInit();
    }

    public static XFuture<AprsSystem> createSystem(File propertiesFile) {
        if (GraphicsEnvironment.isHeadless()) {
            return createAprsSystemHeadless(propertiesFile);
        } else {
            return createAprsSystemWithSwingDisplay(propertiesFile);
        }
    }

    public static XFuture<AprsSystem> createPrevSystem() {
        if (GraphicsEnvironment.isHeadless()) {
            return createPrevAprsSystemHeadless();
        } else {
            return createPrevAprsSystemWithSwingDisplay();
        }
    }

    public static XFuture<AprsSystem> createEmptySystem() {
        if (GraphicsEnvironment.isHeadless()) {
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
        AprsSystemDisplayJFrame aprsSystemDisplayJFrame1 = new AprsSystemDisplayJFrame();
        AprsSystem system = new AprsSystem(aprsSystemDisplayJFrame1);
        return system.emptyInit().
                thenSupply(() -> {
                    aprsSystemDisplayJFrame1.setAprsSystem(system);
                    return system;
                });
    }

    private static XFuture<AprsSystem> createPrevAprsSystemWithSwingDisplay2() {
        AprsSystemDisplayJFrame aprsSystemDisplayJFrame1 = new AprsSystemDisplayJFrame();
        AprsSystem system = new AprsSystem(aprsSystemDisplayJFrame1);
        return system.defaultInit().
                thenApply(x -> {
                    aprsSystemDisplayJFrame1.setAprsSystem(system);
                    return system;
                });
    }

    private static XFuture<AprsSystem> createAprsSystemWithSwingDisplay2(File propertiesFile) {
        AprsSystemDisplayJFrame aprsSystemDisplayJFrame1 = new AprsSystemDisplayJFrame();
        AprsSystem system = new AprsSystem(aprsSystemDisplayJFrame1, propertiesFile);
        return system.defaultInit().
                thenApply(x -> {
                    aprsSystemDisplayJFrame1.setAprsSystem(system);
                    return system;
                });
    }

    private static XFuture<AprsSystem> createEmptyAprsSystemHeadless() {
        AprsSystem system = new AprsSystem();
        return system.emptyInit().
                thenApply(x -> {
                    return system;
                });
    }

    private static XFuture<AprsSystem> createPrevAprsSystemHeadless() {
        AprsSystem system = new AprsSystem();
        return system.defaultInit().
                thenApply(x -> {
                    return system;
                });
    }

    private static XFuture<AprsSystem> createAprsSystemHeadless(File propertiesFile) {
        AprsSystem system = new AprsSystem(null, propertiesFile);
        return system.defaultInit().
                thenSupply(() -> {
                    return system;
                });
    }

    /**
     * Creates new AprsSystem using a specified properties file.
     *
     * @param propertiesFile properties file to be read for initial settings
     */
    @SuppressWarnings("initialization")
    private AprsSystem(@Nullable AprsSystemDisplayJFrame aprsSystemDisplayJFrame1, File propertiesFile) {
        this.aprsSystemDisplayJFrame = aprsSystemDisplayJFrame1;
        initLoggerWindow();
        try {
            setPropertiesFile(propertiesFile);
//            initPropertiesFile();
            if (propertiesFile.exists()) {
                loadProperties();
            } else {
                saveProperties();
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean skipCreateDbSetupFrame = false;

    private final AtomicInteger commonInitCount = new AtomicInteger();

    private XFutureVoid commonInit() {
        System.out.println("commonInitCount = " + commonInitCount.incrementAndGet());
        return startWindowsFromMenuCheckboxes()
                .thenRun(this::completeCommonInit);
    }

    private final AtomicInteger completeCommonInitCount = new AtomicInteger();

    private void completeCommonInit() {
        System.out.println("completeCommonInitCount = " + completeCommonInitCount.incrementAndGet());
        if (null != aprsSystemDisplayJFrame) {
            try {
                URL aprsPngUrl = AprsSystem.class.getResource("aprs.png");
                if (null != aprsPngUrl) {
                    setIconImage(ImageIO.read(aprsPngUrl));
                } else {
                    Logger.getLogger(AprsSystem.class.getName()).log(Level.WARNING, "getResource(\"aprs.png\") returned null");
                }
            } catch (Exception ex) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        if (null != logDisplayJInternalFrame) {
//            activateFrame(logDisplayJInternalFrame);
//        }
        setupWindowsMenu();
        updateTitle("", "");
        this.asString = getTitle();
    }

    private void setIconImage(Image image) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setIconImage(image);
        }
    }

    private volatile boolean kitInspectionStartupSelected = false;

    private boolean isKitInspectionStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isKitInspectionStartupSelected();
            kitInspectionStartupSelected = ret;
            return ret;
        }
        return kitInspectionStartupSelected;
    }

    private void setKitInspectionStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setKitInspectionStartupSelected(selected);
        }
        this.kitInspectionStartupSelected = selected;
    }

    private volatile boolean pddlPlannerStartupSelected = false;

    private boolean isPddlPlannerStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isPddlPlannerStartupSelected();
            pddlPlannerStartupSelected = ret;
            return ret;
        }
        return pddlPlannerStartupSelected;
    }

    private void setPddlPlannerStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setPddlPlannerStartupSelected(selected);
        }
        this.pddlPlannerStartupSelected = selected;
    }

    private volatile boolean pddlExecutorStartupSelected = false;

    private boolean isExecutorStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isExecutorStartupSelected();
            pddlExecutorStartupSelected = ret;
            return ret;
        }
        return pddlExecutorStartupSelected;
    }

    private void setExecutorStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setExecutorStartupSelected(selected);
        }
        this.pddlExecutorStartupSelected = selected;
    }

    private volatile boolean objectSpStartupSelected = false;

    private boolean isObjectSpStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isObjectSpStartupSelected();
            objectSpStartupSelected = ret;
            return ret;
        }
        return objectSpStartupSelected;
    }

    private void setObjectSpStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setObjectSpStartupSelected(selected);
        }
        this.objectSpStartupSelected = selected;
    }

    private volatile boolean object2DViewStartupSelected = false;

    private boolean isObject2DViewStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isObject2DViewStartupSelected();
            object2DViewStartupSelected = ret;
            return ret;
        }
        return object2DViewStartupSelected;
    }

    private void setObject2DViewStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setObject2DViewStartupSelected(selected);
        }
        this.object2DViewStartupSelected = selected;
    }

    private volatile boolean robotCrclGUIStartupSelected = false;

    private boolean isRobotCrclGUIStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isRobotCrclGUIStartupSelected();
            robotCrclGUIStartupSelected = ret;
            return ret;
        }
        return robotCrclGUIStartupSelected;
    }

    private void setRobotCrclGUIStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setRobotCrclGUIStartupSelected(selected);
        }
        this.robotCrclGUIStartupSelected = selected;
    }

    private volatile boolean robotCrclSimServerStartupSelected = false;

    private boolean isRobotCrclSimServerStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isRobotCrclSimServerStartupSelected();
            robotCrclSimServerStartupSelected = ret;
            return ret;
        }
        return robotCrclSimServerStartupSelected;
    }

    private void setRobotCrclSimServerStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setRobotCrclSimServerStartupSelected(selected);
        }
        this.robotCrclSimServerStartupSelected = selected;
    }

    private volatile boolean robotCrclFanucServerStartupSelected = false;

    private boolean isRobotCrclFanucServerStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isRobotCrclFanucServerStartupSelected();
            robotCrclFanucServerStartupSelected = ret;
            return ret;
        }
        return robotCrclFanucServerStartupSelected;
    }

    private void setRobotCrclFanucServerStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setRobotCrclFanucServerStartupSelected(selected);
        }
        this.robotCrclFanucServerStartupSelected = selected;
    }

    private volatile boolean robotCrclMotomanServerStartupSelected = false;

    private boolean isRobotCrclMotomanServerStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isRobotCrclMotomanServerStartupSelected();
            robotCrclMotomanServerStartupSelected = ret;
            return ret;
        }
        return robotCrclMotomanServerStartupSelected;
    }

    private void setRobotCrclMotomanServerStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setRobotCrclMotomanServerStartupSelected(selected);
        }
        this.robotCrclMotomanServerStartupSelected = selected;
    }

    private volatile boolean exploreGraphDBStartupSelected = false;

    private boolean isExploreGraphDBStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isExploreGraphDBStartupSelected();
            exploreGraphDBStartupSelected = ret;
            return ret;
        }
        return exploreGraphDBStartupSelected;
    }

    private void setExploreGraphDBStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setExploreGraphDBStartupSelected(selected);
        }
        this.exploreGraphDBStartupSelected = selected;
    }

    private volatile boolean showDatabaseSetupStartupSelected = false;

    private boolean isShowDatabaseSetupStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isShowDatabaseSetupSelected();
            showDatabaseSetupStartupSelected = ret;
            return ret;
        }
        return showDatabaseSetupStartupSelected;
    }

    private void setShowDatabaseSetupStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setShowDatabaseSetupSelected(selected);
        }
        this.showDatabaseSetupStartupSelected = selected;
    }

    private volatile boolean connectDatabaseOnSetupStartupSelected = false;

    private boolean isConnectDatabaseOnSetupStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isConnectDatabaseOnStartupSelected();
            connectDatabaseOnSetupStartupSelected = ret;
            return ret;
        }
        return connectDatabaseOnSetupStartupSelected;
    }

    private void setConnectDatabaseOnStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setConnectDatabaseOnStartupSelected(selected);
        }
        this.connectDatabaseOnSetupStartupSelected = selected;
    }

    private volatile boolean connectVisionOnSetupStartupSelected = false;

    private boolean isConnectVisionOnStartupSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = this.aprsSystemDisplayJFrame.isConnectVisionOnStartupSelected();
            connectVisionOnSetupStartupSelected = ret;
            return ret;
        }
        return connectVisionOnSetupStartupSelected;
    }

    private void setConnectVisionOnStartupSelected(boolean selected) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setConnectVisionOnStartupSelected(selected);
        }
        this.connectVisionOnSetupStartupSelected = selected;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private XFutureVoid startWindowsFromMenuCheckboxes() {
        try {
            List<XFuture<?>> futures = new ArrayList<>();
            if (isKitInspectionStartupSelected()) {
                startKitInspection();
            }
            if (isPddlPlannerStartupSelected()) {
                startPddlPlanner();
            }
            if (isExecutorStartupSelected()) {
                startActionListExecutor();
            }
            if (isObjectSpStartupSelected()) {
                startVisionToDbJinternalFrame();
            }
            if (isObject2DViewStartupSelected()) {
                startObject2DJinternalFrame();
            }
            if (isRobotCrclGUIStartupSelected()) {
                startCrclClientJInternalFrame();
            }
            if (isRobotCrclSimServerStartupSelected()) {
                startSimServerJInternalFrame();
            }
            if (isExploreGraphDBStartupSelected()) {
                startExploreGraphDb();
            }
            if (isRobotCrclFanucServerStartupSelected()) {
                XFutureVoid startFanucFuture = startFanucCrclServer();
                futures.add(startFanucFuture);
            }
            if (isRobotCrclMotomanServerStartupSelected()) {
                startMotomanCrclServer();
            }
            if (!skipCreateDbSetupFrame || isShowDatabaseSetupStartupSelected()) {
                createDbSetupFrame();
            }
            if (isShowDatabaseSetupStartupSelected()) {
                showDatabaseSetupWindow();
            } else {
                setConnectDatabaseOnStartupSelected(false);
            }
            updateSubPropertiesFiles();
            setupWindowsMenu();
            boolean startConnectDatabaseSelected = isConnectDatabaseOnSetupStartupSelected();
            System.out.println("In propertiesFile=" + getPropertiesFile() + ", startConnectDatabaseSelected=" + startConnectDatabaseSelected);
            if (startConnectDatabaseSelected) {
                XFuture<Boolean> startConnectDatabaseFuture = startConnectDatabase();
                futures.add(startConnectDatabaseFuture);
            }
            if (isConnectVisionOnStartupSelected()) {
                XFutureVoid connectVisionFuture = connectVision();
                futures.add(connectVisionFuture);
            }
            System.out.println("Constructor for AprsSystem complete.");
            if (futures.isEmpty()) {
                return XFutureVoid.completedFutureWithName("startWindowsFromMenuCheckboxes");
            } else {
                XFuture<?> futuresArray[] = futures.toArray(new XFuture[0]);
                return XFutureVoid.allOfWithName("startWindowsFromMenuCheckboxes", futuresArray);
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    @MonotonicNonNull
    private volatile MyPrintStream outLogStream = null;
    @MonotonicNonNull
    private volatile MyPrintStream errLogStream = null;

    public final void initLoggerWindow() {
        try {
            synchronized (this) {
                if (null == logDisplayJInternalFrame) {
                    LogDisplayJInternalFrame logFrame = new LogDisplayJInternalFrame();
                    logFrame.pack();
                    logFrame.setVisible(true);
                    addToDesktopPane(logFrame);
                    if (null == outLogStream) {
                        outLogStream = new MyPrintStream(System.out, logFrame);
                    }
                    if (null == errLogStream) {
                        errLogStream = new MyPrintStream(System.err, logFrame);
                    }
                    this.logDisplayJInternalFrame = logFrame;
                }
            }
            LogDisplayJInternalFrame logFrame = this.logDisplayJInternalFrame;
            if (null != logFrame) {
                if (null != outLogStream) {
                    System.setOut(outLogStream);
                }
                if (null != errLogStream) {
                    System.setErr(errLogStream);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void closeAllInternalFrames() {
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
            closePddlPlanner();
        } catch (Exception exception) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        try {
            closeActionsListExcecutor();

        } catch (Exception exception) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
//        try {
//            stopCrclWebApp();
//        } catch (Exception exception) {
//            Logger.getLogger(AprsSystem.class
//                    .getName()).log(Level.SEVERE, null, exception);
//        }
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        if (null != disconnectDatabaseFuture) {
            disconnectDatabaseFuture.cancel(true);
            disconnectDatabaseFuture = null;
        }

        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setVisible(false);
            object2DViewJInternalFrame.dispose();
        }

        try {
            immediateAbort();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            disconnectVision();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            setConnectDatabaseCheckboxEnabled(false);
            if (null != connectDatabaseFuture) {
                connectDatabaseFuture.cancel(true);
                connectDatabaseFuture = null;
            }
            disconnectDatabase();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            abortCrclProgram();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            startingCheckEnabled = false;
            disconnectRobotPrivate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            abortCrclProgram();
            disconnectRobot();
            disconnectVision();
            disconnectDatabase();
            DbSetupJInternalFrame dbSetupFrame = this.dbSetupJInternalFrame;
            if (null != dbSetupFrame) {
                dbSetupFrame.getDbSetupPublisher().removeAllDbSetupListeners();
                dbSetupFrame.setVisible(false);
//                dbSetupFrame.shutDownNotifyService();
                dbSetupFrame.dispose();
            }
            closeAllInternalFrames();
            if (null != this.exploreGraphDbJInternalFrame) {
                this.exploreGraphDbJInternalFrame.setVisible(false);
                this.exploreGraphDbJInternalFrame.dispose();
            }
            if (null != this.logDisplayJInternalFrame) {
                this.logDisplayJInternalFrame.setVisible(false);
                this.logDisplayJInternalFrame.dispose();
            }
            SimServerJInternalFrame simServerFrame = this.simServerJInternalFrame;
            if (null != simServerFrame) {
                try {
                    simServerFrame.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                simServerFrame.setVisible(false);
                simServerFrame = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //    private void activateInternalFrame(JInternalFrame internalFrame) {
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
//            e.printStackTrace();
//        }
//    }
    public void showDatabaseSetupWindow() {
        createDbSetupFrame();
        DbSetupJInternalFrame dbSetupFrame = this.dbSetupJInternalFrame;
        if (null != dbSetupFrame) {
            dbSetupFrame.setPropertiesFile(propertiesFile);
            checkDeiconifyActivateAndMaximize(dbSetupFrame);
        }

        setupWindowsMenu();
    }

    private void checkDeiconifyActivateAndMaximize(JInternalFrame internalFrame) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.checkDeiconifyActivateAndMaximize(internalFrame);
        }
    }

    private boolean checkInternalFrame(JInternalFrame frm) {
        if (null != aprsSystemDisplayJFrame) {
            return aprsSystemDisplayJFrame.checkInternalFrame(frm);
        }
        return false;
    }

    public void hideDatabaseSetupWindow() {
        if (null != dbSetupJInternalFrame) {
            dbSetupJInternalFrame.setVisible(false);
            checkIconifyAndDeactivate(dbSetupJInternalFrame);
        }
    }

    private void checkIconifyAndDeactivate(JInternalFrame internalFrame) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.checkIconifyAndDeactivate(internalFrame);
        }
    }

    private void deiconifyAndActivate(final JInternalFrame frameToShow) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.deiconifyAndActivate(frameToShow);
        }
    }

    private void activateFrame(final JInternalFrame frameToShow) {
        frameToShow.setVisible(true);
        if (checkInternalFrame(frameToShow)) {
            deiconifyAndActivate(frameToShow);
            frameToShow.moveToFront();
            activeWin = stringToWin(frameToShow.getTitle());
        } else {
            setupWindowsMenu();
        }
    }

    private void setupWindowsMenu() {
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
        if (str.startsWith("PDDL Planner")) {
            return ActiveWinEnum.PDDL_PLANNER_WINDOW;
        }
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
    @Override
    public List<PhysicalItem> getSimItemsData() throws IllegalStateException {
        if (null == object2DViewJInternalFrame) {
            throw new IllegalStateException("null == object2DViewJInternalFrame)");
        }
        return object2DViewJInternalFrame.getItems();
    }

    /**
     * Set a list of items with names and poses to be used by the simulation.
     * <p>
     * This has no effect if the Object 2D view has not been opened or it is not
     * in simulation mode.
     *
     * @param items list of items to use
     */
    @Override
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
     */
    @Override
    public void setLookForXYZ(double x, double y, double z) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setLookForXYZ(x, y, z);
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
    @Override
    public void setViewLimits(double minX, double minY, double maxX, double maxY) {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setViewLimits(minX, minY, maxX, maxY);
        }
    }

    /**
     * Make the Object 2D view visible and create underlying components.
     */
    public void startObject2DJinternalFrame() {
        try {
            boolean alreadySelected = isObject2DViewStartupSelected();
            if (!alreadySelected) {
                setObject2DViewStartupSelected(true);
            }
            object2DViewJInternalFrame = new Object2DViewJInternalFrame();
            updateSubPropertiesFiles();
            object2DViewJInternalFrame.setAprsSystem(this);
            if (null != externalSlotOffsetProvider) {
                object2DViewJInternalFrame.setSlotOffsetProvider(externalSlotOffsetProvider);
            }
            object2DViewJInternalFrame.loadProperties();
            object2DViewJInternalFrame.pack();
            object2DViewJInternalFrame.setVisible(true);
            addToDesktopPane(object2DViewJInternalFrame);
            maximizeJInteralFrame(object2DViewJInternalFrame);
            if (!alreadySelected) {
                setupWindowsMenu();
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
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
    @Nullable
    @Override
    public String getCrclClientErrorString() {
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
    @Override
    public boolean isAborting() {
        return (null != safeAbortAndDisconnectFuture && !safeAbortAndDisconnectFuture.isDone())
                || (null != safeAbortFuture && !safeAbortFuture.isDone());
    }

    private long lastRunAllUpdateRunnableTime = System.currentTimeMillis();

    private void updateTitle(String stateString, String stateDescription) {
        String oldTitle = getTitle();
        String crclClientError = getCrclClientErrorString();
        if (null != crclClientError && crclClientError.length() > 0
                && (null == titleErrorString || titleErrorString.length() < 1)) {
            this.titleErrorString = crclClientError;
            pauseInternal();
        }
        if (isPaused()) {
            stateString = "PAUSED";
        }
        String newTitle = "APRS : " + ((robotName != null) ? robotName + (isConnected() ? "" : "(Disconnected)") : "NO Robot") + " : " + ((taskName != null) ? taskName : "NO Task") + " : " + stateString + " : "
                + stateDescription
                + ((titleErrorString != null) ? ": " + titleErrorString : "")
                + ((pddlExecutorJInternalFrame1 != null) ? (" : " + pddlExecutorJInternalFrame1.getActionSetsCompleted()) : "")
                + (isAborting() ? " : Aborting" : "")
                + (isReverseFlag() ? " : Reverse" : "")
                + pddlActionString();
        if (newTitle.length() > 100) {
            newTitle = newTitle.substring(0, 100) + " ... ";
        }
        if (!oldTitle.equals(newTitle)) {
            setTitle(newTitle);
            this.asString = newTitle;
            setupWindowsMenu();
            runAllUpdateRunnables();
        } else {
            long time = System.currentTimeMillis();
            if (time - lastRunAllUpdateRunnableTime > 100) {
                runAllUpdateRunnables();
            }
        }
    }

    private void runAllUpdateRunnables() {
        for (Runnable r : titleUpdateRunnables) {
            r.run();
        }
        lastRunAllUpdateRunnableTime = System.currentTimeMillis();
    }

    private String pddlActionString() {
        if (null == pddlExecutorJInternalFrame1) {
            return "";
        }
        List<Action> actionList = pddlExecutorJInternalFrame1.getActionsList();
        if (null == actionList || actionList.isEmpty()) {
            return "";
        }
        int curActionIndex = pddlExecutorJInternalFrame1.getCurrentActionIndex();
        if (curActionIndex < 0 || curActionIndex >= actionList.size()) {
            return " : (" + curActionIndex + "/" + actionList.size() + ")";
        }
        return " : (" + curActionIndex + "/" + actionList.size() + "):" + actionList.get(curActionIndex) + " : ";
    }

    /**
     * Get the current status if available
     *
     * @return current status or null
     */
    @Nullable
    @Override
    public CRCLStatusType getCurrentStatus() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.getCurrentStatus().orElse(null);
        }
        return null;
    }

    /**
     * Update the title based on the current state.
     */
    @Override
    public void updateTitle() {
        Utils.runOnDispatchThread(this::updateTitleInternal);
    }

    @Nullable
    private CommandStatusType getCommandStatus() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.getCurrentStatus()
                    .map(CRCLStatusType::getCommandStatus)
                    .map(CRCLPosemath::copy)
                    .orElse(null);
        }
        return null;
    }

    private void updateTitleInternal() {
        if (null != crclClientJInternalFrame) {
            CommandStatusType cs = crclClientJInternalFrame.getCurrentStatus()
                    .map(CRCLStatusType::getCommandStatus)
                    .orElse(null);
            if (null == cs || null == cs.getCommandState()) {
                updateTitle("", "");
            } else {
                if (cs.getCommandState() == CommandStateEnumType.CRCL_DONE) {
                    CommandStatusType errCmdStatus = titleErrorStringCommandStatus;
                    if (null != errCmdStatus
                            && (errCmdStatus.getCommandID() != cs.getCommandID()
                            || (errCmdStatus.getCommandState() == CommandStateEnumType.CRCL_ERROR))) {
                        titleErrorString = null;
                    }
                }
                updateTitle(cs.getCommandState().toString(), cs.getStateDescription());
            }
        } else {
            updateTitle("", "");
        }
    }

    /**
     * Create and display the CRCL client frame (aka pendant client)
     */
    public void startCrclClientJInternalFrame() {
        try {
            boolean alreadySelected = isRobotCrclGUIStartupSelected();
            if (!alreadySelected) {
                setRobotCrclGUIStartupSelected(true);
            }
            crclClientJInternalFrame = new PendantClientJInternalFrame();
            crclClientJInternalFrame.setRunProgramService(runProgramService);
            crclClientJInternalFrame.setTempLogDir(Utils.getlogFileDir());
            updateSubPropertiesFiles();
            crclClientJInternalFrame.loadProperties();
            crclClientJInternalFrame.pack();
            crclClientJInternalFrame.setVisible(true);
            addToDesktopPane(crclClientJInternalFrame);
            maximizeJInteralFrame(crclClientJInternalFrame);
            crclClientJInternalFrame.addUpdateTitleListener(new UpdateTitleListener() {

                @Override
                public void titleChanged(CommandStatusType ccst, Container container, String stateString, String stateDescription) {
                    updateTitle(stateString, stateDescription);
                }
            });
            if (null != unaddedPoseListeners) {
                List<PendantClientJPanel.CurrentPoseListener> tempList = new ArrayList<>(unaddedPoseListeners);
                unaddedPoseListeners.clear();
                for (PendantClientJPanel.CurrentPoseListener l : tempList) {
                    crclClientJInternalFrame.addCurrentPoseListener(l);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Create and display the simulation server window and start the simulation
     * server thread.
     */
    public void startSimServerJInternalFrame() {
        try {
            if (null == simServerJInternalFrame) {
                boolean alreadySelected = isRobotCrclSimServerStartupSelected();
                if (!alreadySelected) {
                    setRobotCrclSimServerStartupSelected(true);
                }
                simServerJInternalFrame = new SimServerJInternalFrame(false);
                updateSubPropertiesFiles();
                simServerJInternalFrame.loadProperties();
                simServerJInternalFrame.pack();
                simServerJInternalFrame.setVisible(true);
                simServerJInternalFrame.restartServer();
                addToDesktopPane(simServerJInternalFrame);
                maximizeJInteralFrame(simServerJInternalFrame);

            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final Callable<DbSetupPublisher> dbSetupPublisherSupplier = new Callable<DbSetupPublisher>() {

        @Override
        public DbSetupPublisher call() {
            createDbSetupFrame();
            assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
            return dbSetupJInternalFrame.getDbSetupPublisher();
        }
    };

    private volatile boolean dbConnected = false;

    private void updateDbConnectedCheckBox(DbSetup setup) {
        dbConnected = setup.isConnected();
        setConnectDatabaseCheckboxEnabled(true);
        XFuture<Boolean> f = dbConnectedWaiters.poll();
        while (f != null) {
            f.complete(dbConnected);
            f = dbConnectedWaiters.poll();
        }
    }

    private final DbSetupListener dbSetupListener = new DbSetupListener() {
        @Override
        public void accept(DbSetup setup) {
            updateDbConnectedCheckBox(setup);
        }
    };

    private void createDbSetupFrame() {
        if (null == dbSetupJInternalFrame) {
            dbSetupJInternalFrame = new DbSetupJInternalFrame();
            dbSetupJInternalFrame.setAprsSystem(this);
            dbSetupJInternalFrame.pack();
            dbSetupJInternalFrame.loadRecentSettings();
            addToDesktopPane(dbSetupJInternalFrame);
            dbSetupJInternalFrame.getDbSetupPublisher().addDbSetupListener(dbSetupListener);
            DbSetup dbs = this.dbSetup;
            if (null != dbs) {
                DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
                if (null != pub) {
                    pub.setDbSetup(dbs);
                }
            }
        }
    }

    private volatile boolean showVisionConnected;

    /**
     * Set the menu checkbox item to reflect the val of the whether the vision
     * system is connected. This will not cause the system to connect/disconnect
     * only to show the state the caller already knows.
     *
     * @param val of vision systems connected status to show
     */
    @Override
    public void setShowVisionConnected(boolean val) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setShowVisionConnected(val);
        }
        this.showVisionConnected = val;
    }

    public void startVisionToDbJinternalFrame() {
        try {
            visionToDbJInternalFrame = new VisionToDbJInternalFrame();
            visionToDbJInternalFrame.setAprsSystem(this);
            updateSubPropertiesFiles();
            visionToDbJInternalFrame.loadProperties();
            visionToDbJInternalFrame.pack();
            visionToDbJInternalFrame.setVisible(true);
            visionToDbJInternalFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
            addToDesktopPane(visionToDbJInternalFrame);
            maximizeJInteralFrame(visionToDbJInternalFrame);
        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Nullable
    private volatile XFuture<?> xf1 = null;
    @Nullable
    private volatile XFuture<?> xf2 = null;
    @Nullable
    private volatile XFutureVoid xf3 = null;

    /**
     * Start the PDDL Executor (aka Actions to CRCL) and create and display the
     * window for displaying its output.
     */
    public void startActionListExecutor() {
        try {
            Utils.runAndWaitOnDispatchThread("startActionsToCrclJInternalFrame",
                    () -> {
                        try {
                            boolean alreadySelected = isExecutorStartupSelected();
                            if (!alreadySelected) {
                                setExecutorStartupSelected(true);
                            }
                            if (null == pddlExecutorJInternalFrame1) {
                                pddlExecutorJInternalFrame1 = new ExecutorJInternalFrame();
                                this.pddlExecutorJInternalFrame1.pack();
                            }
                            ExecutorJInternalFrame execFrame = this.pddlExecutorJInternalFrame1;
                            assert (null != execFrame) : "";
                            execFrame.setAprsSystem(this);
                            execFrame.setVisible(true);
                            addToDesktopPane(execFrame);
                            maximizeJInteralFrame(execFrame);
                            updateSubPropertiesFiles();
                            execFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
                            if (null != pddlPlannerJInternalFrame) {
                                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(execFrame);
                            }
                            xf1 = XFuture.runAsync("startActionsToCrclJInternalFrame.loadProperties", () -> {
                                try {
                                    execFrame.loadProperties();
                                } catch (IOException ex) {
                                    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }, runProgramService);
                            xf2 = xf1
                                    .thenRun(() -> {
                                        xf3 = Utils.runOnDispatchThread(() -> {
                                            if (!alreadySelected) {
                                                setupWindowsMenu();

                                            }
                                            if (null != aprsSystemDisplayJFrame) {
                                                aprsSystemDisplayJFrame.addMenu(execFrame.getToolMenu());
                                            }
                                        });
                                    });

                        } catch (IOException ex) {
                            Logger.getLogger(AprsSystem.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    });
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void startPddlPlanner() {
        try {
            //        jDesktopPane1.setDesktopManager(d);
            if (pddlPlannerJInternalFrame == null) {
                pddlPlannerJInternalFrame = new PddlPlannerJInternalFrame();
                pddlPlannerJInternalFrame.pack();
            }
            updateSubPropertiesFiles();
            pddlPlannerJInternalFrame.setVisible(true);
            addToDesktopPane(pddlPlannerJInternalFrame);
            maximizePddlPlannerJInternalFrame();
//            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "pddlPlanner.txt"));
            pddlPlannerJInternalFrame.loadProperties();
            pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void maximizePddlPlannerJInternalFrame() {
        if (null != pddlPlannerJInternalFrame) {
            JInternalFrame internalFrame = pddlPlannerJInternalFrame;
            maximizeJInteralFrame(internalFrame);
        }
    }

    private void maximizeJInteralFrame(JInternalFrame internalFrame) {
        try {
            if (null != internalFrame && !GraphicsEnvironment.isHeadless()) {
                JDesktopPane desktopPane = internalFrame.getDesktopPane();
                if (null != desktopPane) {
                    DesktopManager desktopManager = desktopPane.getDesktopManager();
                    if (null != desktopManager) {
                        desktopManager.maximizeFrame(internalFrame);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, e);
        }
    }

    public void startKitInspection() {
        try {
            //        jDesktopPane1.setDesktopManager(d);
            if (kitInspectionJInternalFrame == null) {
                kitInspectionJInternalFrame = new KitInspectionJInternalFrame();
                kitInspectionJInternalFrame.pack();
            }
            updateSubPropertiesFiles();
            kitInspectionJInternalFrame.setVisible(true);
            addToDesktopPane(kitInspectionJInternalFrame);
            maximizeJInteralFrame(kitInspectionJInternalFrame);
//            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "pddlPlanner.txt"));
            kitInspectionJInternalFrame.loadProperties();
            //kitInspectionJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, ex);
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

    /**
     * Query the user to select a properties file to open.
     */
    public void browseOpenPropertiesFile() {
        if (null != aprsSystemDisplayJFrame) {
            File selectedFile = aprsSystemDisplayJFrame.choosePropertiesFileToOpen();
            if (null != selectedFile) {
                try {
                    loadSelectedPropertiesFile(selectedFile);

                } catch (IOException ex) {
                    Logger.getLogger(AprsSystem.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                this.initLoggerWindow();
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
     */
    public void browseSavePropertiesFileAs() {
        if (null != aprsSystemDisplayJFrame) {
            File selectedFile = aprsSystemDisplayJFrame.choosePropertiesFileToOpen();
            if (null != selectedFile) {
                try {
                    setPropertiesFile(selectedFile);
                    this.saveProperties();
                } catch (IOException ex) {
                    Logger.getLogger(AprsSystem.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            throw new IllegalStateException("can't browse for files when aprsSystemDisplayJFrame == null");
        }
    }

    private void windowClosed() {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setVisible(false);
        }
        disconnectVision();
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.stopSimUpdateTimer();
        }
    }

    public void windowClosing() {
        startingCheckEnabled = false;
        try {
            immediateAbort();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            disconnectVision();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            startDisconnectDatabase();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            startingCheckEnabled = false;
            disconnectRobotPrivate();
        } catch (Exception ex) {
            ex.printStackTrace();
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
            ex.printStackTrace();
        }
    }

    @Override
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

    /**
     * Start a sequence of actions to move the robot out of the way so the
     * vision system can see the parts and wait for the vision system to provide
     * data and the database to be updated. It will happen asynchronously in
     * another thread.
     *
     * @return future allowing the caller to determine when the actions are
     * complete.
     */
    public XFuture<Boolean> startLookForParts() {
        long t0 = logEvent("startLookForParts", null);
        return waitForPause()
                .thenCompose("startLookForParts.checkDbConnected", x -> checkDBConnected())
                .thenApplyAsync("startLookForParts.lookForPartsInternal",
                        x -> lookForPartsInternal(),
                        runProgramService)
                .always(() -> logEvent("finished startLookForParts", (System.currentTimeMillis() - t0)));
    }

    private XFuture<Boolean> checkDBConnected() {
        XFutureVoid cdf = connectDatabaseFuture;
        if (null == cdf) {
            return startConnectDatabase();
        }
        return XFuture.completedFuture(true);
    }

    private boolean lookForPartsInternal() {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        try {
            CRCLProgramType lfpProgram = pddlExecutorJInternalFrame1.createLookForPartsProgram();
            boolean ret = runCRCLProgram(lfpProgram);
            if (ret) {
                enableCheckedAlready = true;
            }
            return ret;
        } catch (JAXBException jAXBException) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, jAXBException);
            throw new RuntimeException(jAXBException);
        }
    }

    private void setImageSizeMenuText() {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setImageSizeMenuText(snapShotWidth, snapShotHeight);
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @SuppressWarnings("unused")
    public boolean getDebug() {
        return debug;
    }

    private boolean isWithinMaxLimits(PmCartesian cart) {
        return cart != null
                && cart.x <= maxLimit.x
                && cart.y <= maxLimit.y
                && cart.z <= maxLimit.z;
    }

    private boolean isWithinMinLimits(PmCartesian cart) {
        return cart != null
                && cart.x >= minLimit.x
                && cart.y >= minLimit.y
                && cart.z >= minLimit.z;
    }

    private boolean isWithinLimits(PmCartesian cart) {
        return isWithinMaxLimits(cart) && isWithinMinLimits(cart);
    }

    private volatile PmCartesian minLimit = new PmCartesian(-10000, -10000, -10000);

    /**
     * Get the value of minLimit
     *
     * @return the value of minLimit
     */
    public PmCartesian getMinLimit() {
        return new PmCartesian(minLimit.x, minLimit.y, minLimit.z);
    }

    /**
     * Set the value of minLimit
     *
     * @param minLimit new value of minLimit
     */
    public void setMinLimit(PmCartesian minLimit) {
        this.minLimit = minLimit;
    }

    private volatile PmCartesian maxLimit = new PmCartesian(10000, 10000, 10000);

    /**
     * Get the value of maxLimit
     *
     * @return the value of maxLimit
     */
    public PmCartesian getMaxLimit() {
        return new PmCartesian(maxLimit.x, maxLimit.y, maxLimit.z);
    }

    /**
     * Set the value of maxLimit
     *
     * @param maxLimit new value of maxLimit
     */
    public void setMaxLimit(PmCartesian maxLimit) {
        this.maxLimit = maxLimit;
    }

    /**
     * Get a Slot with an absolute position from the slot offset and a tray.
     *
     * @param tray slot is within
     * @param offsetItem slot with relative position offset for this type of
     * tray
     * @return slot with absolute position
     */
    @Override
    @Nullable
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.absSlotFromTrayAndOffset(tray, offsetItem);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.absSlotFromTrayAndOffset(tray, offsetItem);
    }

    /**
     * Get a Slot with an absolute position from the slot offset and a tray.
     *
     * @param tray slot is within
     * @param offsetItem slot with relative position offset for this type of
     * tray
     * @return slot with absolute position
     */
    @Override
    @Nullable
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
    }

    /**
     * Get a list of items seen by the vision system or simulated in the
     * Object2D view and create a set of actions that will fill empty trays to
     * match. Load this list into the PDDL executor.
     */
    @Override
    public void createActionListFromVision() {
        try {
            List<PhysicalItem> requiredItems = getObjectViewItems();
            List<PhysicalItem> teachItems = requiredItems;
            updateScanImage(requiredItems, false);
            takeSimViewSnapshot("createActionListFromVision", requiredItems);
            createActionListFromVision(requiredItems, teachItems, false, 0);
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            setTitleErrorString("createActionListFromVision: " + ex.getMessage());
        }
    }

    private void updateScanImage(List<PhysicalItem> requiredItems, boolean autoScale) {
        Utils.runOnDispatchThread(() -> {
            updateScanImageInternal(requiredItems, autoScale);
        });
    }

    @SuppressWarnings("SameParameterValue")
    private void updateScanImageWithRotationOffset(List<PhysicalItem> requiredItems, boolean autoScale, double rotationOffset) {
        Utils.runOnDispatchThread(() -> {
            updateScanImageWithRotationOffsetInternal(requiredItems, autoScale, rotationOffset);
        });
    }

    @Nullable
    @Override
    public BufferedImage getLiveImage() {
        if (!isConnected()) {
            return null;
        }
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        Object2DJPanel.ViewOptions opts = new Object2DJPanel.ViewOptions();
        opts.h = 170;
        opts.w = 170;
        opts.disableLabels = true;
        opts.enableAutoscale = false;
        opts.disableLimitsLine = true;
        opts.disableShowCurrent = false;
        opts.addExtras = true;
//        opts.debug = true;
        return object2DViewJInternalFrame.createSnapshotImage(opts);
    }

    private void updateScanImageInternal(List<PhysicalItem> requiredItems, boolean autoScale) {
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        Object2DJPanel.ViewOptions opts = new Object2DJPanel.ViewOptions();
        opts.h = 170;
        opts.w = 170;
        opts.disableLabels = true;
        opts.enableAutoscale = autoScale;
        opts.disableLimitsLine = true;
        opts.disableShowCurrent = true;
        scanImage = object2DViewJInternalFrame.createSnapshotImage(opts, requiredItems);
    }

    private void updateScanImageWithRotationOffsetInternal(List<PhysicalItem> requiredItems, boolean autoScale, double rotationOffset) {
        if (requiredItems.isEmpty()) {
            return;
        }
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        Object2DJPanel.ViewOptions opts = new Object2DJPanel.ViewOptions();
        opts.h = 170;
        opts.w = 170;
        opts.disableLabels = true;
        opts.enableAutoscale = autoScale;
        opts.overrideRotationOffset = true;
        opts.disableLimitsLine = true;
        opts.disableShowCurrent = true;
        opts.rotationOffset = rotationOffset;
        scanImage = object2DViewJInternalFrame.createSnapshotImage(opts, requiredItems);
    }

    private GoalLearner goalLearner;

    /**
     * Get the value of goalLearner
     *
     * @return the value of goalLearner
     */
    @Override
    public GoalLearner getGoalLearner() {
        return goalLearner;
    }

    /**
     * Set the value of goalLearner
     *
     * @param goalLearner new value of goalLearner
     */
    @Override
    public void setGoalLearner(GoalLearner goalLearner) {
        this.goalLearner = goalLearner;
    }

    @SuppressWarnings("SameReturnValue")
    private boolean checkKitTrays(List<PhysicalItem> kitTrays) {
        if (kitTrays.isEmpty()) {
            Thread.dumpStack();
            if (null == aprsSystemDisplayJFrame
                    || JOptionPane.YES_OPTION
                    != JOptionPane.showConfirmDialog(aprsSystemDisplayJFrame, "Create action list with no kit trays?")) {
                setTitleErrorString("createActionListFromVision: No kit trays");
                throw new IllegalStateException("No kit trays");
            }
        }
        return true;
    }

    @Override
    public List<String> getLastCreateActionListFromVisionKitToCheckStrings() {
        if (null == goalLearner) {
            return Collections.emptyList();
        }
        return goalLearner.getLastCreateActionListFromVisionKitToCheckStrings();
    }

    @Override
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
    @Override
    public boolean isCorrectionMode() {
        return (null != goalLearner && goalLearner.isCorrectionMode());
    }

    /**
     * Set the value of correctionMode
     *
     * @param correctionMode new value of correctionMode
     */
    @Override
    public void setCorrectionMode(boolean correctionMode) {
        if (goalLearner == null) {
            goalLearner = new GoalLearner();
        }
        goalLearner.setCorrectionMode(correctionMode);
    }

    /**
     * Use the provided list of items create a set of actions that will fill
     * empty trays to match.Load this list into the PDDL executor.
     *
     * @param requiredItems list of items that have to be seen before the robot
     * can begin
     * @param teachItems list of trays and items in the trays as they should be
     * when complete.
     */
    @SuppressWarnings("unused")
    public void createActionListFromVision(List<PhysicalItem> requiredItems, List<PhysicalItem> teachItems, boolean overrideRotation, double newRotationOffsetParam) {

        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        try {
            List<String> startingList = this.getLastCreateActionListFromVisionKitToCheckStrings();
            if (goalLearner == null) {
                goalLearner = new GoalLearner();
            }
            if (teachItems.isEmpty()) {
                return;
            }
            goalLearner.setItemPredicate(this::isWithinLimits);
            if (goalLearner.isCorrectionMode()) {
                goalLearner.setKitTrayListPredicate(null);
            } else {
                goalLearner.setKitTrayListPredicate(this::checkKitTrays);
            }
            goalLearner.setSlotOffsetProvider(visionToDbJInternalFrame);

            boolean allEmptyA[] = new boolean[1];
            List<Action> actions = goalLearner.createActionListFromVision(requiredItems, teachItems, allEmptyA, overrideRotation, newRotationOffsetParam);
            t1 = System.currentTimeMillis();
            boolean allEmpty = allEmptyA[0];
            if (!goalLearner.isCorrectionMode()) {
                if (allEmpty || actions == null || actions.isEmpty()) {
                    System.out.println("requiredItems = " + requiredItems);
                    System.out.println("teachItems = " + teachItems);
                    Thread.dumpStack();
                    if (null == aprsSystemDisplayJFrame
                            || JOptionPane.YES_OPTION
                            != JOptionPane.showConfirmDialog(aprsSystemDisplayJFrame, "Load action list with all trays empty?")) {
                        setTitleErrorString("createActionListFromVision: All kit trays empty");
                        throw new IllegalStateException("All kit trays empty");
                    }
                }
            }
            File f = createTempFile("actionList", ".txt");
            try (PrintStream ps = new PrintStream(new FileOutputStream(f))) {
                assert actions != null;
                for (Action act : actions) {
                    ps.println(act.asPddlLine());
                }
            }
            List<String> endingList = goalLearner.getLastCreateActionListFromVisionKitToCheckStrings();
            boolean equal = GoalLearner.kitToCheckStringsEqual(startingList, endingList);
            if (null != pddlExecutorJInternalFrame1 && (!equal || !goalLearner.isCorrectionMode())) {
                boolean startingReverseFlag = isReverseFlag();
                pddlExecutorJInternalFrame1.setReverseFlag(false);
                pddlExecutorJInternalFrame1.loadActionsFile(f);
                pddlExecutorJInternalFrame1.setReverseFlag(startingReverseFlag);
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
                        + endingList
                                .stream()
                                .collect(Collectors.joining("\n")));
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            setTitleErrorString("createActionListFromVision: " + ex.getMessage());
        }
        long t2 = System.currentTimeMillis();
//        System.out.println("createActionListFromVision: (t1-t0) = " +(t1-t0));
//        System.out.println("createActionListFromVision: (t2-t0) = " +(t2-t0));
    }

    /**
     * Get the list of items displayed in the Object 2D view, they may be
     * simulated or received from the vision system.
     *
     * @return list of items displayed in the Object 2D view
     */
    @Override
    public List<PhysicalItem> getObjectViewItems() {
        if (null != object2DViewJInternalFrame) {
            return object2DViewJInternalFrame.getItems();
        }
        throw new IllegalStateException("object2DViewJInternalFrame is null");
    }

    public boolean getForceFakeTakeFlag() {
        if (pddlExecutorJInternalFrame1 != null) {
            return pddlExecutorJInternalFrame1.getForceFakeTakeFlag();
        }
        return false;
    }

    /**
     * Set the menu checkbox setting to force take operations to be faked so
     * that the gripper will not close, useful for testing.
     *
     * @param val true if take operations should be faked
     */
    public void setForceFakeTakeFlag(boolean val) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setForceFakeTakeFlag(val);
        }
        if (pddlExecutorJInternalFrame1 != null) {
            if (pddlExecutorJInternalFrame1.getForceFakeTakeFlag() != val) {
                pddlExecutorJInternalFrame1.setForceFakeTakeFlag(val);
            }
        }
    }

    @Nullable
    private volatile XFuture<Boolean> lastResumeFuture = null;

    @Nullable
    private volatile Thread resumingThread = null;
    private volatile StackTraceElement resumingTrace@Nullable []  = null;
    private volatile boolean resuming = false;

    private volatile boolean pauseCheckboxSelected = false;

    private boolean isPauseCheckboxSelected() {
        if (null != crclClientJInternalFrame) {
            if (crclClientJInternalFrame.isPaused()) {
                setPauseCheckboxSelected(true);
                return true;
            }
        }
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isPauseCheckboxSelected();
            this.pauseCheckboxSelected = ret;
            return ret;
        }
        return pauseCheckboxSelected;
    }

    private void setPauseCheckboxSelected(boolean val) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setPauseCheckboxSelected(val);
        }
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.showPaused(val);
        }
        this.pauseCheckboxSelected = val;
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

        logEvent("resume", null);
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
            if (isPauseCheckboxSelected()) {
                setPauseCheckboxSelected(false);
            }
            badState = badState || pausing;
            clearErrors();
            badState = badState || pausing;
            setPauseCheckboxSelected(false);
            crclClientJInternalFrame.unpauseCrclProgram();
            setPauseCheckboxSelected(false);
            notifyPauseFutures();
            badState = badState || pausing;
            clearErrors();
            badState = badState || pausing;
            String methodName = "resume";
            takeSnapshots(methodName);
            badState = badState || pausing;
            setPauseCheckboxSelected(false);
            crclClientJInternalFrame.unpauseCrclProgram();
            setPauseCheckboxSelected(false);
            badState = badState || pausing;
            updateTitle("", "");
            badState = badState || pausing;
            boolean currentPaused = isPaused();
            if (null != pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1.showPaused(currentPaused);
            }
            if (currentPaused) {
                String currentPauseString = crclClientJInternalFrame.pauseInfoString();
                System.err.println("Still paused after resume");
                System.err.println("startPauseInfo = " + startPauseInfo);
                System.err.println("currentPauseString = " + currentPauseString);
                System.err.println("runName=" + getRunName());
                setPauseCheckboxSelected(currentPaused);
                throw new IllegalStateException("Still paused after resume. crclClientJInternalFrame.isPaused()="
                        + isCrclClientJInternalFramePaused());
            }
        } finally {
            resuming = false;
        }
        badState = badState || pausing;
        if (badState) {
            System.err.println("pauseThread = " + pauseThread);
            System.err.println("pauseTrace = " + Arrays.toString(pauseTrace));
            throw new IllegalStateException("Attempt to resume while pausing");
        }
    }

    /**
     * Check to see if the module responsible for updating the database with
     * data received from the vision system has connected to the database.
     *
     * @return is vision to database system currently connected to the database
     */
    @Override
    public boolean isVisionToDbConnected() {
        return null != visionToDbJInternalFrame && visionToDbJInternalFrame.isDbConnected();
    }

    private volatile boolean snapshotCheckboxSelected = false;

    private boolean isSnapshotCheckboxSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isSnapshotCheckboxSelected();
            this.snapshotCheckboxSelected = ret;
            return ret;
        }
        return snapshotCheckboxSelected;
    }

    private void setSnapshotCheckboxSelected(boolean val) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setSnapshotCheckboxSelected(val);
        }
        this.snapshotCheckboxSelected = val;
    }

    @Override
    public boolean snapshotsEnabled() {
        return null != object2DViewJInternalFrame && isSnapshotCheckboxSelected();
    }

    /**
     * Used for logging/debugging. Save a file(s) in the temporary directory
     * with the comment and a timestamp with the current view of the parts and
     * robot.
     *
     * @param comment comment name to include in filename for later analysis
     */
    @Override
    public void takeSnapshots(String comment) {
        try {
            if (snapshotsEnabled()) {
                takeSimViewSnapshot(createImageTempFile(comment), (PmCartesian) null, (String) null);
                if (null != visionToDbJInternalFrame && visionToDbJInternalFrame.isDbConnected()) {
                    startVisionToDbNewItemsImageSave(createTempFile(comment + "_new_database_items", ".PNG"));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

    /**
     * Print a great deal of debugging info to the console.
     */
    public void debugAction() {

        System.out.println();
        System.err.println();
        int count = debugActionCount.incrementAndGet();
        System.out.println("Begin AprsSystem.debugAction()" + count);
        String details = getDetailsString();
        System.out.println("details = " + details);
        System.out.println("lastContinueCrclProgramResult = " + lastContinueCrclProgramResult);
        System.out.println("lastStartActionsFuture = " + lastStartActionsFuture);
        if (null != lastStartActionsFuture) {
            lastStartActionsFuture.printStatus(System.out);
        }
        System.out.println("ContinuousDemoFuture = " + continuousDemoFuture);
        if (null != continuousDemoFuture) {
            continuousDemoFuture.printStatus(System.out);
        }
        System.out.println("safeAbortFuture = " + safeAbortFuture);
        if (null != safeAbortFuture) {
            safeAbortFuture.printStatus(System.out);
        }
        System.out.println("lastResumeFuture = " + lastResumeFuture);
        if (null != lastResumeFuture) {
            lastResumeFuture.printStatus(System.out);
        }

        System.out.println("lastPauseFuture = " + lastPauseFuture);
        if (null != lastPauseFuture) {
            lastPauseFuture.printStatus(System.out);
        }

        System.out.println("lastContinueActionListFuture = " + lastContinueActionListFuture);
        if (null != lastContinueActionListFuture) {
            lastContinueActionListFuture.printStatus(System.out);
        }

        System.out.println("lastResumeFuture = " + lastRunProgramFuture);
        if (null != lastRunProgramFuture) {
            lastRunProgramFuture.printStatus(System.out);
        }
        System.out.println("isConnected = " + isConnected());
        System.out.println("isPaused = " + isPaused());
        System.out.println("getRobotCrclPort = " + getRobotCrclPort());
        System.out.println("isCrclProgramPaused() = " + isCrclProgramPaused());
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.debugAction();
        }
        System.out.println();
        System.err.println();
        System.out.println("Begin AprsSystem.debugAction()" + count);

        printNameSetInfo();

    }

    private void printNameSetInfo() {
        long curTime = System.currentTimeMillis();
        System.out.println("setRobotNameNullThread = " + setRobotNameNullThread);
        System.out.println("setRobotNameNullStackTrace = " + Arrays.toString(setRobotNameNullStackTrace));
        System.out.println("setRobotNameNullThreadTime = " + (curTime - setRobotNameNullThreadTime));

        System.out.println("setRobotNameNonNullThread = " + setRobotNameNonNullThread);
        System.out.println("setRobotNameNonNullStackTrace = " + Arrays.toString(setRobotNameNonNullStackTrace));
        System.out.println("setRobotNameNonNullThreadTime = " + (curTime - setRobotNameNonNullThreadTime));

        System.out.println("startSafeAbortAndDisconnectThread = " + startSafeAbortAndDisconnectThread);
        System.out.println("startSafeAbortAndDisconnectStackTrace = " + Arrays.toString(startSafeAbortAndDisconnectStackTrace));
        System.out.println("startSafeAbortAndDisconnectTime = " + (curTime - startSafeAbortAndDisconnectTime));
    }

    @Nullable
    private volatile XFuture<Boolean> continuousDemoFuture = null;

    private final AtomicInteger cdStart = new AtomicInteger();

    /**
     * Start a continuous demo where kits will be built , emptied and built
     * again repeating indefinitely. The demo will begin by checking if the
     * robot can be enabled first. (This may cause a second or two of delay and
     * break clicking sound.)
     *
     * @param comment optional string used for logging/debugging or tracing
     * which caller started the demo.
     * @param reverseFirst begin by emptying the kits
     * @return future that can be used to add actions if the demo is canceled or
     * fails.
     */
    public XFuture<Boolean> startContinuousDemo(String comment, boolean reverseFirst) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        logEvent("startContinuousDemo", null);
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        int startDisconnectCount = disconnectRobotCount.get();
        continuousDemoFuture = startContinuousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount, cdStart.incrementAndGet());
        return continuousDemoFuture.always(() -> logEvent("finished startContinuousDemo", null));
    }

    private XFuture<Boolean> startContinuousDemo(String comment, boolean reverseFirst, int startAbortCount, int startDisconnectCount, int cdStart) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        if (!pddlExecutorJInternalFrame1.readyForNewActionsList()) {
            System.err.println("starting Continuous demo with comment=\"" + comment + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart + ",cdCur=" + 1);
        }
        if (startAbortCount != pddlExecutorJInternalFrame1.getSafeAbortRequestCount()) {
            return XFuture.completedFuture(false);
        }
        setStartRunTime();
        String logLabel = "startContinuousDemo(task=" + getTaskName() + ")." + comment + "." + startAbortCount + "." + startDisconnectCount + "." + cdStart + "." + 1;
        logToSuper(logLabel);
        takeSnapshots(logLabel);
        String startRobotName = this.robotName;
        if (null == startRobotName) {
            throw new IllegalStateException("startContinuousDemo with robotName ==null");
        }
        final String checkedRobotName = startRobotName;
        continuousDemoFuture
                = XFuture.supplyAsync("startContinuousDemo(task=" + getTaskName() + ") comment=" + comment,
                        new Callable<Boolean>() {

                    @Override
                    public Boolean call() {
                        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                        AprsSystem.this.setReverseFlag(reverseFirst, true);
                        boolean r0 = pddlExecutorJInternalFrame1.readyForNewActionsList();
                        if (!r0) {
                            System.err.println("starting Continuous demo with comment=\"" + comment + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart);
                        }
                        boolean enabledOk = doCheckEnabled(startAbortCount, checkedRobotName);
                        boolean r1 = pddlExecutorJInternalFrame1.readyForNewActionsList();
                        if (!r1) {
                            System.err.println("starting Continuous demo with comment=\"" + comment + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart);
                        }
                        return repeatDoActionWithReverse(enabledOk, comment, reverseFirst, startAbortCount, startDisconnectCount, cdStart);
                    }
                },
                        runProgramService);
        return continuousDemoFuture;
    }

    private boolean repeatDoActionWithReverse(Boolean x, String comment, boolean reverseFirst, int startAbortCount, int startDisconnectCount, int cdStart) throws IllegalStateException {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int cdCurLocal = 1;
        while (x && !isAborting()
                && pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount
                && startDisconnectCount == disconnectRobotCount.get()) {
            String logLabel2 = "startContinuousDemo(task=" + getTaskName() + ")." + comment + "." + startAbortCount + "." + startDisconnectCount + "." + cdStart + "." + cdCurLocal;
            logToSuper(logLabel2);
            takeSnapshots("doActions." + logLabel2);
            boolean doActionWithReverseOk = doActionsWithReverse(comment, x, reverseFirst, startAbortCount, startDisconnectCount);
            cdCurLocal++;
            if (!doActionWithReverseOk) {
                return false;
            }
        }
        return false;
    }

    @Nullable
    private volatile transient Consumer<String> supervisorEventLogger = null;

    /**
     * Get the value of supervisorEventLogger
     *
     * @return the value of supervisorEventLogger
     */
    @Nullable
    @Override
    public Consumer<String> getSupervisorEventLogger() {
        return supervisorEventLogger;
    }

    @MonotonicNonNull private volatile Supervisor supervisor = null;

    /**
     * Get the value of supervisor
     *
     * @return the value of supervisorEventLogger
     */
    @Nullable
    public Supervisor getSupervisor() {
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

    private void logToSuper(String s) {
        Consumer<String> c = this.supervisorEventLogger;
        if (null != c) {
            c.accept(s);
        }
    }

    private boolean doActionsWithReverse(String comment, Boolean x, boolean reverseFirst, int startAbortCount, int startDisconnectCount) throws IllegalStateException {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        if (isReverseFlag() != reverseFirst) {
            System.err.println("Reverse flag changed as starting continuous demo.");
            setTitleErrorString("Reverse flag changed as starting continuous demo.");
            throw new IllegalStateException("Reverse flag changed as starting continuous demo.");
        }
        if (x
                && !isAborting()
                && pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount
                && startDisconnectCount == disconnectRobotCount.get()) {
            logToSuper(getTaskName() + ": doActionsWithReverse-" + comment + "_" + reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount);
            long doActions1TimeStart = System.currentTimeMillis();
            boolean actionsOk = pddlExecutorJInternalFrame1.doActions(comment + "_" + reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount, startAbortCount);
            long doActions1TimeEnd = System.currentTimeMillis();
            logToSuper(getTaskName() + ": actionsOk=" + actionsOk + " time to complete = " + (doActions1TimeEnd - doActions1TimeStart) + " ms");
            if (isReverseFlag() != reverseFirst) {
                System.err.println("Reverse flag changed as starting continuous demo.");
                setTitleErrorString("Reverse flag changed as starting continuous demo.");
                throw new IllegalStateException("Reverse flag changed as starting continuous demo.");
            }
            if (actionsOk
                    && !isAborting()
                    && pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount
                    && startDisconnectCount == disconnectRobotCount.get()) {
                setReverseFlag(!reverseFirst, true);
                logToSuper(getTaskName() + ": reverseFlag=" + isReverseFlag() + " step 2 doActionsWithReverse-" + comment + "_" + reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount);
                long doActions2TimeStart = System.currentTimeMillis();
                boolean actionsOk2 = pddlExecutorJInternalFrame1.doActions("2" + comment + "_" + reverseFirst + "_" + startAbortCount + "_" + startDisconnectCount, startAbortCount);
                long doActions2TimeEnd = System.currentTimeMillis();
                logToSuper(getTaskName() + ": actionsOk2=" + actionsOk2 + " time to complete = " + (doActions2TimeEnd - doActions2TimeStart) + " ms");
                return actionsOk2;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Start a continuous demo where kits will be built , emptied and built
     * again repeating indefinitely. The demo will not begin by checking if the
     * robot can be enabled first. (This may avoid a second or two of delay and
     * break clicking sound but mean that if it can't be enabled the error
     * display may be less clear.)
     *
     * @param comment optional string used for logging/debugging or tracing
     * which caller started the demo.
     * @param reverseFirst begin by emptying the kits
     * @return future that can be used to add actions if the demo is canceled or
     * fails.
     */
    @Override
    public XFuture<Boolean> startPreCheckedContinuousDemo(String comment, boolean reverseFirst) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        int startDisconnectCount = disconnectRobotCount.get();
        continuousDemoFuture = startPreCheckedContinuousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount);
        return continuousDemoFuture;
    }

    private XFuture<Boolean> startPreCheckedContinuousDemo(String comment, boolean reverseFirst, int startAbortCount, int startDisconnectCount) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int safeAbortRequestCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        if (startAbortCount != safeAbortRequestCount || isAborting()) {
            continuousDemoFuture = XFuture.completedFutureWithName("startPreCheckedContinuousDemo(" + reverseFirst + "," + startAbortCount + ").safeAbortRequestCount=" + safeAbortRequestCount, false);
            return continuousDemoFuture;
        }
        setStartRunTime();
        if (!enableCheckedAlready) {
            continuousDemoFuture = XFuture.completedFutureWithName("startPreCheckedContinuousDemo(" + reverseFirst + "," + startAbortCount + ").!enableCheckedAlready", false);
            return continuousDemoFuture;
        }
        if (!pddlExecutorJInternalFrame1.readyForNewActionsList()) {
            System.err.println("Call to startPreCheckedContinuousDemo when not ready");
        }
        continuousDemoFuture = XFuture.supplyAsync("startPreCheckedContinuousDemo(task=" + getTaskName() + ")",
                () -> {
                    assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                    this.setReverseFlag(reverseFirst, true);
                    if (startAbortCount != pddlExecutorJInternalFrame1.getSafeAbortRequestCount() || isAborting()) {
                        return false;
                    }
                    return doActionsWithReverse(comment, true, reverseFirst, startAbortCount, startDisconnectCount);
                }, runProgramService)
                .thenComposeAsync("startPreCheckedContinuousDemo(task=" + getTaskName() + ").recurse",
                        x2 -> x2 ? startPreCheckedContinuousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount) : XFuture.completedFutureWithName("startContinuousDemo.completedFutureWithName", false),
                        runProgramService);
        return continuousDemoFuture;
    }

    @Nullable
    public XFuture<Boolean> getContinuousDemoFuture() {
        return continuousDemoFuture;
    }

    private volatile boolean reverseCheckboxSelected = false;

    private boolean isReverseCheckboxSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isReverseCheckboxSelected();
            this.reverseCheckboxSelected = ret;
            return ret;
        }
        return this.reverseCheckboxSelected;
    }

    private void setReverseCheckboxSelected(boolean val) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setReverseCheckboxSelected(val);
        }
        this.reverseCheckboxSelected = val;
    }

    /**
     * Get the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use.
     *
     * @return reverse flag
     */
    @Override
    public boolean isReverseFlag() {
        return isReverseCheckboxSelected();
    }

    /**
     * Set the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use. Reload the simulated object positions.
     *
     * @param reverseFlag new value for reverse flag
     * @return a future object that can be used to determine when setting the
     * reverse flag and all related actions is complete.
     */
    public XFutureVoid startSetReverseFlag(boolean reverseFlag) {
        clearLogDirCache();
        return startSetReverseFlag(reverseFlag, true);
    }

    /**
     * Set the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use. Optionally reload the simulated object positions.
     *
     * @param reverseFlag new value for reverse flag
     * @param reloadSimFiles whether to load simulated object position files
     * first
     * @return a future object that can be used to determine when setting the
     * reverse flag and all related actions is complete.
     */
    @Override
    public XFutureVoid startSetReverseFlag(boolean reverseFlag, boolean reloadSimFiles) {
        logEvent("startSetReverseFlag", reverseFlag);
        return XFuture.runAsync("startSetReverseFlag(" + reverseFlag + "," + reloadSimFiles + ")",
                () -> {
                    setReverseFlag(reverseFlag, reloadSimFiles);
                },
                runProgramService).always(() -> logEvent("finished startSetReverseFlag", reverseFlag));
    }

    private volatile boolean reloadSimFilesOnReverse = false;

    private boolean isReloadSimFilesOnReverseCheckboxSelected() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isReloadSimFilesOnReverseCheckboxSelected();
            this.reloadSimFilesOnReverse = ret;
            return ret;
        }
        return this.reloadSimFilesOnReverse;
    }

    private void setReloadSimFilesOnReverseCheckboxSelected(boolean val) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setReloadSimFilesOnReverseCheckboxSelected(val);
        }
        this.reloadSimFilesOnReverse = val;
    }

    private void setReverseFlag(boolean reverseFlag, boolean reloadSimFiles) {
        clearLogDirCache();
        if (isReverseCheckboxSelected() != reverseFlag) {
            setReverseCheckboxSelected(reverseFlag);
        }
        if (null != object2DViewJInternalFrame) {
            try {
                object2DViewJInternalFrame.setReverseFlag(reverseFlag);
                if (reloadSimFiles && isReloadSimFilesOnReverseCheckboxSelected()) {
                    if (object2DViewJInternalFrame.isSimulated() || !object2DViewJInternalFrame.isConnected()) {
                        object2DViewJInternalFrame.reloadDataFile();
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != pddlExecutorJInternalFrame1) {
            try {
                pddlExecutorJInternalFrame1.setReverseFlag(reverseFlag);
                pddlExecutorJInternalFrame1.reloadActionsFile();
            } catch (IOException ex) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        clearLogDirCache();
    }

    /**
     * Get the state of whether the system is paused
     *
     * @return paused state
     */
    @Override
    public boolean isPaused() {
        return isPauseCheckboxSelected();
    }

    /**
     * Pause any actions currently being performed and set a state that will
     * cause future actions to wait until resume is called.
     */
    public void pause() {
        LauncherAprsJFrame.PlayAlert2();
        boolean badState = resuming;
        pauseInternal();
        badState = badState || resuming;
        updateTitle("", "");
        badState = badState || resuming;
        if (badState) {
            System.err.println("resumingThread = " + resumingThread);
            System.err.println("resumingTrace = " + Arrays.toString(resumingTrace));
            throw new IllegalStateException("Attempt to pause while resuming:");
        }
    }

    private volatile boolean pausing = false;
    @Nullable
    private volatile Thread pauseThread = null;
    private volatile StackTraceElement pauseTrace@Nullable []  = null;

    private void pauseInternal() {
        logEvent("pause", null);
        pauseThread = Thread.currentThread();
        pauseTrace = pauseThread.getStackTrace();
        pausing = true;
        boolean badState = resuming;
        try {
            if (!isPauseCheckboxSelected()) {
                setPauseCheckboxSelected(true);
            }
            badState = badState || resuming;
            if (null != crclClientJInternalFrame && titleErrorString != null && titleErrorString.length() > 0) {
                String lastMessage = crclClientJInternalFrame.getLastMessage();
                System.out.println("lastMessage = " + lastMessage);
                MiddleCommandType cmd = crclClientJInternalFrame.getCurrentProgramCommand();
                if (null != cmd) {
                    String cmdString = CRCLSocket.cmdToString(cmd);
                    System.out.println("cmdString = " + cmdString);
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
            badState = badState || resuming;
            if (null != pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1.showPaused(true);
            }
            this.pauseCrclProgram();

        } finally {
            pausing = false;
        }
        badState = badState || resuming;
        if (badState) {
            System.err.println("resumingThread = " + resumingThread);
            System.err.println("resumingTrace = " + Arrays.toString(resumingTrace));
            throw new IllegalStateException("Attempt to pause while resuming");
        }

    }

    /**
     * Reset errors and reload simulation files
     *
     * @return a future object that can be used to determine when setting the
     * reset and all related actions is complete.
     */
    public XFutureVoid reset() {
        return reset(true);
    }

    /**
     * Reset errors and optionally reload simulation files
     *
     * @param reloadSimFiles whether to reload simulation files
     * @return a future object that can be used to determine when setting the
     * reset and all related actions is complete.
     */
    @Override
    public XFutureVoid reset(boolean reloadSimFiles) {
        return XFuture.runAsync("reset",
                () -> {
                    resetInternal(reloadSimFiles);
                }, runProgramService);

    }

    private void resetInternal(boolean reloadSimFiles) {
        clearErrors();
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(reloadSimFiles);
        }
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.refresh();
        }
        clearErrors();
    }

    private void setCommandID(CRCLCommandType cmd) {
        Utils.setCommandID(cmd, incrementAndGetCommandId());
    }

    private CRCLProgramType createEmptyProgram() {
        CRCLProgramType prog = new CRCLProgramType();
        prog.setInitCanon(new InitCanonType());
        setCommandID(prog.getInitCanon());
        prog.getMiddleCommand().clear();
        prog.setEndCanon(new EndCanonType());
        setCommandID(prog.getEndCanon());
        return prog;
    }

    private long incrementAndGetCommandId() {
        if (null != this.pddlExecutorJInternalFrame1) {
            return this.pddlExecutorJInternalFrame1.incrementAndGetCommandId();
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
        updateTitle();
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setErrorString(null);
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

    @Nullable
    private volatile XFuture<Boolean> lastStartCheckEnabledFuture1 = null;
    @Nullable
    private volatile XFuture<Boolean> lastStartCheckEnabledFuture2 = null;
    private volatile boolean startingCheckEnabled = false;

    /**
     * Test that the robot can be connected by running an empty program.
     * <p>
     * The actions will be executed in another thread after this method returns.
     * The returned future can be used to monitor, cancel or extend the
     * underlying task. The boolean contained in the future will be true only if
     * all actions appear to succeed.
     *
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startCheckEnabled() {
        if (null != disconnectRobotFuture && !disconnectRobotFuture.isDone()) {
            throw new IllegalStateException("trying to startCheckEnabled when disconnecRobotFuture is still not complete");
        }
        if (null == pddlExecutorJInternalFrame1) {
            throw new IllegalStateException("null == pddlExecutorJInternalFrame1");
        }
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
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
        String logString = "startCheckEnabled robotName=" + checkedRobotName + ",task=" + checkedTaskName;
        long t0 = logEvent(logString, null);
        setStartRunTime();
        if (enableCheckedAlready) {
            logEvent("startCheckEnabled enableCheckedAlready", null);
            startingCheckEnabled = false;
            return XFuture.completedFutureWithName("startCheckEnabled.enableCheckedAlready", true);
        }

        XFuture<Boolean> xf1 = XFuture.supplyAsync(logString, () -> this.doCheckEnabled(startAbortCount, checkedRobotName), runProgramService);
        this.lastStartCheckEnabledFuture1 = xf1;
        XFuture<Boolean> xf2
                = xf1
                        .always(() -> logEvent("finished " + logString, (System.currentTimeMillis() - t0)));
        this.lastStartCheckEnabledFuture2 = xf2;
        return xf2;
    }

    private boolean doCheckEnabled(int startAbortCount, String startRobotName) {
        assert (null != pddlExecutorJInternalFrame1) : ("null == pendantClientJInternalFrame  ");
        assert (null != crclClientJInternalFrame) : ("null == crclClientJInternalFrame  ");
        if (pddlExecutorJInternalFrame1.getSafeAbortRequestCount() != startAbortCount) {
            startingCheckEnabled = false;
            return false;
        }
        if (!Objects.equals(this.robotName, startRobotName)) {
            System.out.println("startRobotName = " + startRobotName);
            System.out.println("this.robotName = " + this.robotName);
            System.out.println("setRobotNameNullThread = " + setRobotNameNullThread);
            System.out.println("setRobotNameNullStackTrace = " + Arrays.toString(setRobotNameNullStackTrace));
            System.out.println("setRobotNameNullThreadTime = " + setRobotNameNullThreadTime);
            System.out.println("setRobotNameNonNullThread = " + setRobotNameNonNullThread);
            System.out.println("setRobotNameNonNullStackTrace = " + Arrays.toString(setRobotNameNonNullStackTrace));
            System.out.println("setRobotNameNonNullThreadTime = " + setRobotNameNonNullThreadTime);
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
            setCommandID(emptyProgram.getInitCanon());
            setCommandID(emptyProgram.getEndCanon());
            emptyProgram.setName("checkEnabled." + checkEnabledCount.incrementAndGet());
            setProgram(emptyProgram);
            boolean progRunRet = crclClientJInternalFrame.runCurrentProgram(stepMode);

//            System.out.println("startCheckEnabled finishing with " + progRunRet);
            enableCheckedAlready = progRunRet;
            return progRunRet;
        } catch (JAXBException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private volatile boolean logCrclProgramsEnabled = true;

    public boolean isLogCrclProgramsEnabled() {
        if (null != aprsSystemDisplayJFrame) {
            boolean ret = aprsSystemDisplayJFrame.isLogCrclProgramsEnabled();
            this.logCrclProgramsEnabled = ret;
            return ret;
        }
        return logCrclProgramsEnabled;
    }

    public void setLogCrclProgramsEnabled(boolean logCrclProgramsEnabled) {
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setLogCrclProgramsEnabled(logCrclProgramsEnabled);
        }
        this.logCrclProgramsEnabled = logCrclProgramsEnabled;
    }

    private final AtomicInteger emptyProgramCount = new AtomicInteger();
    private final AtomicInteger consecutiveEmptyProgramCount = new AtomicInteger();

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void setProgram(CRCLProgramType program) throws JAXBException {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
        PendantClientJInternalFrame clntJiF = crclClientJInternalFrame;
        synchronized (clntJiF) {
            if (program.getMiddleCommand().isEmpty()) {
                emptyProgramCount.incrementAndGet();
                int cepCount = consecutiveEmptyProgramCount.incrementAndGet();
                if (cepCount > 1 && debug) {
                    System.out.println("emptyProgramCount=" + emptyProgramCount);
                    System.out.println("consecutiveEmptyProgramCount=" + cepCount);
                }
            } else {
                consecutiveEmptyProgramCount.set(0);
            }
            clntJiF.setProgram(program);
        }
        logCrclProgFile(program);
    }

    public void logCrclProgFile(CRCLProgramType program) {
        if (isLogCrclProgramsEnabled()) {
            try {
                String progString = CRCLSocket.getUtilSocket().programToPrettyString(program, false);
                File progFile = createTempFile("prog", ".xml", getLogCrclProgramDir());
                try (PrintWriter writer = new PrintWriter(new FileWriter(progFile))) {
                    writer.print(progString);
                }
            } catch (Exception ex) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private final ConcurrentLinkedDeque<XFuture<Boolean>> dbConnectedWaiters = new ConcurrentLinkedDeque<>();

    private XFuture<Boolean> waitDbConnected() {
        XFuture<Boolean> f = new XFuture<>("waitDbConnected");
        dbConnectedWaiters.add(f);
        return f;
    }

    @Nullable
    private volatile XFuture<Boolean> lastStartActionsFuture = null;

    /**
     * Get the last future created from a startActions request. Only used for
     * debugging.
     *
     * @return future or null if no startActions request has been made.
     */
    @Nullable
    @Override
    public XFuture<Boolean> getLastStartActionsFuture() {
        return lastStartActionsFuture;
    }

    /**
     * Get the last future created from a continueActions request. Only used for
     * debugging.
     *
     * @return future or null if no continueActions request has been made.
     */
    @Nullable
    @Override
    public XFuture<Boolean> getContinueActionListFuture() {
        return lastContinueActionListFuture;
    }

    private final ConcurrentLinkedDeque<String> startActionsStartComments
            = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<String> startActionsFinishComments
            = new ConcurrentLinkedDeque<>();

    /**
     * Start the PDDL actions currently loaded in the executor from the
     * beginning.
     * <p>
     * The actions will be executed in another thread after this method returns.
     * The returned future can be used to monitor, cancel or extend the
     * underlying task. The boolean contained in the future will be true only if
     * all actions appear to succeed.
     *
     * @param actions list of actions to execute
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActionsList(Iterable<Action> actions) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        this.pddlExecutorJInternalFrame1.loadActionsList(actions);
        return startActions("");
    }

    /**
     * Check to see if the executor is in a state where it could begin working
     * on a new list of actions.
     *
     * @return is executor ready for new actions.
     */
    @Override
    public boolean readyForNewActionsList() {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        return pddlExecutorJInternalFrame1.readyForNewActionsList();
    }

    /**
     * Start the PDDL actions currently loaded in the executor from the
     * beginning.
     * <p>
     * The actions will be executed in another thread after this method returns.
     * The returned future can be used to monitor, cancel or extend the
     * underlying task. The boolean contained in the future will be true only if
     * all actions appear to succeed.
     *
     * @param comment comment used for tracking/logging tasks starting the
     * actions
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActions(String comment) {

        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        setStartRunTime();
        long startRunNumber = runNumber.incrementAndGet();
        clearLogDirCache();
        startActionsStartComments.add(comment + ",startRunNumber=" + startRunNumber + ",runNumber=" + runNumber);
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        lastContinueStartAbortCount = startAbortCount;
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(isReloadSimFilesOnReverseCheckboxSelected());
        }
        if (null != motomanCrclServerJInternalFrame) {
            String robotName = getRobotName();
            if (null != robotName) {
                if (robotName.toUpperCase().contains("MOTOMAN")) {
                    if (!motomanCrclServerJInternalFrame.isCrclMotoplusConnected()) {
                        try {
                            motomanCrclServerJInternalFrame.connectCrclMotoplus();
                        } catch (IOException ex) {
                            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    if (!motomanCrclServerJInternalFrame.isCrclMotoplusConnected()) {
                        setTitleErrorString("Motoman CRCL Server not connected.");
                    }
                }
            }
        }
        lastStartActionsFuture = XFuture.runAsync("AprsSystem.startActions",
                () -> {
                    setThreadName();
                    takeSnapshots("startActions." + comment);
                }, runProgramService)
                .thenCompose("startActions.pauseCheck.comment=" + comment + ", startRunNumber" + startRunNumber, x -> waitForPause())
                .thenApplyAsync(taskName, x -> {
                    assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                    long currentRunNumber = runNumber.get();
                    if (currentRunNumber != startRunNumber) {
                        throw new IllegalStateException("runNumbeChanged");
                    }
                    if (currentRunNumber != startRunNumber
                            || pddlExecutorJInternalFrame1.getSafeAbortRequestCount() != startAbortCount) {
                        return false;
                    }
                    boolean ret = pddlExecutorJInternalFrame1.doActions("startActions." + comment + ", startRunNumber" + startRunNumber, startAbortCount);
                    startActionsFinishComments.add(comment + ",startRunNumber=" + startRunNumber + ",runNumber=" + currentRunNumber);
                    if (currentRunNumber != startRunNumber) {
                        System.err.println("startActionsStartComments=" + startActionsStartComments);
                        System.err.println("startActionsFinishComments=" + startActionsFinishComments);
                        throw new IllegalStateException("runNumbeChanged");
                    }
                    return ret;
                }, runProgramService
                );
        return lastStartActionsFuture;
    }

    private void checkReadyToRun() throws IllegalStateException {
        if (null == robotName || robotName.length() < 1) {
            String msg = "startActions called with robotName= " + robotName;
            setTitleErrorString(msg);
            throw new IllegalStateException(msg);
        }
        if (isAborting()) {
            String msg = "startActions called with safeAbortFuture= " + safeAbortFuture;
            setTitleErrorString(msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Get the state of whether the PDDL executor is currently doing actions.
     *
     * @return current state
     */
    @Override
    public boolean isDoingActions() {
        if (null == pddlExecutorJInternalFrame1) {
            return false;
        }
        return pddlExecutorJInternalFrame1.isDoingActions();
    }

    public void startExploreGraphDb() {
        assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
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
                    .getName()).log(Level.SEVERE, null, ex);
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
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get the current line in the CRCL program being run.
     *
     * @return current line
     */
    @Override
    public int getCrclProgramLine() {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
        return this.crclClientJInternalFrame.getCurrentProgramLine();
    }

    private File propertiesFile;
    private File propertiesDirectory;
    private static final String APRS_DEFAULT_PROPERTIES_FILENAME = "aprs_properties.txt";
    private File lastAprsPropertiesFileFile;

    private static class AprsSystemPropDefaults {

        private final File propDir;
        private final File propFile;
        private final File lastAprsPropertiesFileFile;

        private static final AprsSystemPropDefaults single = new AprsSystemPropDefaults();

        static AprsSystemPropDefaults getSingle() {
            return single;
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
            File tempPropDir = null;
            File tempPropFile = null;
            if (null != aprsPropsDir) {
                tempPropDir = new File(aprsPropsDir);
            } else {
                tempPropDir = new File(System.getProperty("user.home"), ".aprs");
            }
            lastAprsPropertiesFileFile = new File(tempPropDir, "lastPropsFileName.txt");
            if (lastAprsPropertiesFileFile.exists()) {
                try {
                    Properties p = new Properties();
                    p.load(new FileReader(lastAprsPropertiesFileFile));
                    String fname = p.getProperty("lastPropertyFileName");
                    if (fname != null && fname.length() > 0) {
                        File f = new File(fname);
                        if (f.exists()) {
                            tempPropFile = f;
                            tempPropDir = tempPropFile.getParentFile();
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(AprsSystem.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (null == tempPropFile || !tempPropFile.exists()) {
                if (null != tempPropDir) {
                    tempPropDir.mkdirs();
                }
                String aprsPropsFilename = System.getProperty("aprs.properties.filename", APRS_DEFAULT_PROPERTIES_FILENAME);
                tempPropFile = new File(tempPropDir, aprsPropsFilename);
            }
            if (null != tempPropDir) {
                this.propDir = tempPropDir;
            } else {
                this.propDir = new File(System.getProperty("user.home"));
            }
            this.propFile = tempPropFile;
        }
    }

    private static File getDefaultPropertiesDir() {
        return AprsSystemPropDefaults.getSingle().getPropDir();
    }

    /**
     * Get the default file location for saved properties.
     *
     * @return default file location
     */
    public static File getDefaultPropertiesFile() {
        return AprsSystemPropDefaults.getSingle().getPropFile();
    }

    /**
     * Get the default file location for a file that will could contain a
     * reference to the last properties file used.
     *
     * @return last file location
     */
    private static File getDefaultLastPropertiesFileFile() {
        return AprsSystemPropDefaults.getSingle().getLastAprsPropertiesFileFile();
    }

    private void initPropertiesFileInfo() {
        propertiesDirectory = getDefaultPropertiesDir();
        propertiesFile = getDefaultPropertiesFile();
        lastAprsPropertiesFileFile = getDefaultLastPropertiesFileFile();
    }

    private int snapShotWidth = 800;
    private int snapShotHeight = 600;

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param pose optional pose to mark or null
     * @param label optional label for pose or null
     */
    public void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, pose, label, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     */
    public void takeSimViewSnapshot(File f, PointType point, String label) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, point, label, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     */
    @Override
    public void takeSimViewSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, point, label, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel string that will be included in the image file name
     * @param pose optional pose to mark or null
     * @param poseLabel optional label for pose or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), pose, poseLabel, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel string that will be included in the image file name
     * @param pt optional point to mark or null
     * @param pointLabel optional label for point or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, @Nullable PmCartesian pt, @Nullable String pointLabel) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), pt, pointLabel, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel string that will be included in the image file name
     * @param pt optional point to mark or null
     * @param pointLabel optional label for point or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PointType pt, String pointLabel) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), pt, pointLabel, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param imgLabel string that will be added to the filename along with
     * timestamp and extention
     * @param itemsToPaint list of items to paint
     * @throws java.io.IOException problem writing to the file
     */
    @Override
    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            this.object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), itemsToPaint, snapShotWidth, snapShotHeight);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param pose optional pose to mark or null
     * @param label optional label for pose or null
     * @param w width of snapshot image
     * @param h height of snapshot image
     */
    public void takeSimViewSnapshot(File f, PoseType pose, String label, int w, int h) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, pose, label, w, h);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @param w width of snapshot image
     * @param h height of snapshot image
     */
    public void takeSimViewSnapshot(File f, PointType point, String label, int w, int h) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, point, label, w, h);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @param w width of snapshot image
     * @param h height of snapshot image
     */
    public void takeSimViewSnapshot(File f, PmCartesian point, String label, int w, int h) {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, point, label, w, h);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel label to included in filename
     * @param pose optional pose to mark or null
     * @param poseLabel optional label for pose or null
     * @param w width of snapshot image
     * @param h height of snapshot image
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), pose, poseLabel, w, h);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel label to included in filename
     * @param pt optional point to mark or null
     * @param pointLabel optional label for point or null
     * @param w width of snapshot image
     * @param h height of snapshot image
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PmCartesian pt, String pointLabel, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), pt, pointLabel, w, h);
        }
    }

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel label to included in filename
     * @param pt optional point to mark or null
     * @param pointLabel optional label for point or null
     * @param w width of snapshot image
     * @param h height of snapshot image
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PointType pt, String pointLabel, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame && isSnapshotCheckboxSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), pt, pointLabel, w, h);
        }
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     * @param w width of snapshot image
     * @param h height of snapshot image
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) {
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, w, h);
        }
    }

    /**
     * * Take a snapshot of the view of objects positions passed in the list.
     *
     * @param imgLabel string that will be added to the filename along with
     * timestamp and extention
     * @param itemsToPaint list of items to paint
     * @param w width of image to create
     * @param h height of image to create
     * @throws java.io.IOException problem writing to the file
     */
    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.takeSnapshot(createImageTempFile(imgLabel), itemsToPaint, w, h);
        }
    }

    @MonotonicNonNull
    private DbSetup dbSetup = null;

    @Override
    public final void loadProperties() throws IOException {

        IOException exA[] = new IOException[1];
        try {
            Utils.runAndWaitOnDispatchThread("loadProperties",
                    () -> {
                        loadPropertiesInternal(exA);
                    });
        } catch (InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != exA[0]) {
            throw new IOException(exA[0]);
        }
    }

    private void loadPropertiesInternal(IOException exA[]) {
        try {
            Properties props = new Properties();
            System.out.println("AprsSystem loading properties from " + propertiesFile.getCanonicalPath());
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String useTeachTableString = props.getProperty(USETEACHTABLE);
            if (null != useTeachTableString) {
                setUseTeachTable(Boolean.valueOf(useTeachTableString));
            }
            String startPddlPlannerString = props.getProperty(STARTUPPDDLPLANNER);
            if (null != startPddlPlannerString) {
                setPddlPlannerStartupSelected(Boolean.valueOf(startPddlPlannerString));
            }
            String startExecutorString = props.getProperty(STARTUPPDDLEXECUTOR);
            if (null != startExecutorString) {
                setExecutorStartupSelected(Boolean.valueOf(startExecutorString));
            }
            String startObjectSpString = props.getProperty(STARTUPPDDLOBJECTSP);
            if (null != startObjectSpString) {
                setObjectSpStartupSelected(Boolean.valueOf(startObjectSpString));
            }

            String startObjectViewString = props.getProperty(STARTUPPDDLOBJECTVIEW);
            if (null != startObjectViewString) {
                setObject2DViewStartupSelected(Boolean.valueOf(startObjectViewString));
            }
            String startCRCLClientString = props.getProperty(STARTUPROBOTCRCLCLIENT);
            if (null != startCRCLClientString) {
                setRobotCrclGUIStartupSelected(Boolean.valueOf(startCRCLClientString));
            }
            String startCRCLSimServerString = props.getProperty(STARTUPROBOTCRCLSIMSERVER);
            if (null != startCRCLSimServerString) {
                setRobotCrclSimServerStartupSelected(Boolean.valueOf(startCRCLSimServerString));
            }
            String startCRCLFanucServerString = props.getProperty(STARTUPROBOTCRCLFANUCSERVER);
            if (null != startCRCLFanucServerString) {
                setRobotCrclFanucServerStartupSelected(Boolean.valueOf(startCRCLFanucServerString));
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
                setRobotCrclMotomanServerStartupSelected(Boolean.valueOf(startCRCLMotomanServerString));
            }
            String startConnectDBString = props.getProperty(STARTUPCONNECTDATABASE);
            if (null != startConnectDBString) {
                setConnectDatabaseOnStartupSelected(Boolean.valueOf(startConnectDBString));
                if (isConnectDatabaseOnSetupStartupSelected()) {
                    setShowDatabaseSetupStartupSelected(true);
                }
            }
            String startConnectVisionString = props.getProperty(STARTUPCONNECTVISION);
            if (null != startConnectVisionString) {
                setConnectVisionOnStartupSelected(Boolean.valueOf(startConnectVisionString));
                if (isConnectVisionOnStartupSelected()) {
                    setObjectSpStartupSelected(true);
                }
            }
            String startExploreGraphDbString = props.getProperty(STARTUPEXPLOREGRAPHDB);
            if (null != startExploreGraphDbString) {
                setExploreGraphDBStartupSelected(Boolean.valueOf(startExploreGraphDbString));
            }
            String crclWebAppPortString = props.getProperty(CRCLWEBAPPPORT);
            if (null != crclWebAppPortString) {
                crclWebServerHttpPort = Integer.parseInt(crclWebAppPortString);
            }

            String startKitInspetion = props.getProperty(STARTUPKITINSPECTION);
            if (null != startKitInspetion) {
                setKitInspectionStartupSelected(Boolean.valueOf(startKitInspetion));
            }
            this.updateSubPropertiesFiles();
            if (null != this.pddlPlannerJInternalFrame) {
                this.pddlPlannerJInternalFrame.loadProperties();
            }
            if (null != this.pddlExecutorJInternalFrame1) {
                XFutureVoid loadPropertiesFuture
                        = XFuture.runAsync("loadProperties",
                                () -> {
                                    try {
                                        if (null != this.pddlExecutorJInternalFrame1) {
                                            this.pddlExecutorJInternalFrame1.loadProperties();
                                        }
                                    } catch (IOException ex) {
                                        Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }, runProgramService);
                loadPropertiesFuture.join();
            }

            if (null != this.object2DViewJInternalFrame) {
                this.object2DViewJInternalFrame.loadProperties();
            }
            dbSetup = DbSetupBuilder.loadFromPropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt")).build();

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
            if (null != this.motomanCrclServerJInternalFrame) {
                motomanCrclServerJInternalFrame.loadProperties();
            }
            if (null != this.fanucCRCLServerJInternalFrame) {
                fanucCRCLServerJInternalFrame.loadProperties();
            }
            String motomanCrclLocalPortString = props.getProperty(MOTOMAN_CRCL_LOCAL_PORT);
            if (null != motomanCrclLocalPortString) {
                this.motomanCrclPort = Integer.parseInt(motomanCrclLocalPortString);
                if (null != motomanCrclServerJInternalFrame) {
                    motomanCrclServerJInternalFrame.setCrclPort(motomanCrclPort);
                }
            }
            String robotNameString = props.getProperty(APRSROBOT_PROPERTY_NAME);
            if (null != robotNameString) {
                setRobotName(robotNameString);
            } else {
                setDefaultRobotName();
            }
            String maxLimitString = props.getProperty(MAX_LIMIT_PROP);
            if (null != maxLimitString && maxLimitString.trim().length() > 0) {
                setMaxLimit(PmCartesian.valueOf(maxLimitString));
            }
            String minLimitString = props.getProperty(MIN_LIMIT_PROP);
            if (null != minLimitString && minLimitString.trim().length() > 0) {
                setMinLimit(PmCartesian.valueOf(minLimitString));
            }

            String reloadSimFilesOnReverseString = props.getProperty(RELOAD_SIM_FILES_ON_REVERSE_PROP);
            if (null != reloadSimFilesOnReverseString && reloadSimFilesOnReverseString.trim().length() > 0) {
                setReloadSimFilesOnReverseCheckboxSelected(Boolean.valueOf(reloadSimFilesOnReverseString));
            }
            String logCrclProgramsEnabledString = props.getProperty(LOG_CRCL_PROGRAMS_ENABLED);
            if (null != logCrclProgramsEnabledString && logCrclProgramsEnabledString.trim().length() > 0) {
                setLogCrclProgramsEnabled(Boolean.valueOf(logCrclProgramsEnabledString));
            }
            String snapShotEnableString = props.getProperty(SNAP_SHOT_ENABLE_PROP);
            if (null != snapShotEnableString && snapShotEnableString.trim().length() > 0) {
                setSnapshotCheckboxSelected(Boolean.valueOf(snapShotEnableString));
            }
            String snapShotWidthString = props.getProperty(SNAP_SHOT_WIDTH_PROP);
            if (null != snapShotWidthString && snapShotWidthString.trim().length() > 0) {
                snapShotWidth = Integer.parseInt(snapShotWidthString);
            }
            String snapShotHeightString = props.getProperty(SNAP_SHOT_HEIGHT_PROP);
            if (null != snapShotHeightString && snapShotHeightString.trim().length() > 0) {
                snapShotHeight = Integer.parseInt(snapShotHeightString);
            }
            setImageSizeMenuText();
            String taskNameString = props.getProperty(APRSTASK_PROPERTY_NAME);
            if (null != taskNameString) {
                setTaskName(taskNameString);
            }
            String startupActiveWinString = props.getProperty(STARTUP_ACTIVE_WIN);
            if (null != startupActiveWinString) {
                activeWin = ActiveWinEnum.valueOf(startupActiveWinString);
                showActiveWin();
            }
        } catch (IOException exception) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, exception);
            exA[0] = exception;
        } catch (Exception exception) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, exception);
        }
    }

    private void showActiveWin() {
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
                if (null != pddlExecutorJInternalFrame1) {
                    activateFrame(pddlExecutorJInternalFrame1);
                }
                break;

            case PDDL_PLANNER_WINDOW:
                if (null != pddlPlannerJInternalFrame) {
                    activateFrame(pddlPlannerJInternalFrame);
                }
                break;

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
    @Override
    public ActiveWinEnum getActiveWin() {
        return activeWin;
    }

    /**
     * Select a window (InternalJFrame)to be shown on top.
     *
     * @param activeWin enumerated window to make active
     */
    @Override
    public void setActiveWin(ActiveWinEnum activeWin) {
        this.activeWin = activeWin;
        showActiveWin();
    }

    @Override
    public void saveProperties() throws IOException {
        File propsParent = propertiesFile.getParentFile();
        if (propsParent == null) {
            System.err.println("propertiesFile.getParentFile() returned null : propertiesFile=" + propertiesFile);
            return;
        }
        if (!propsParent.exists()) {
            System.out.println("Directory " + propsParent + " does not exist. (Creating it now.)");
            propsParent.mkdirs();
        }
        Map<String, String> propsMap = new HashMap<>();

        propsMap.put(USETEACHTABLE, Boolean.toString(getUseTeachTable()));
        propsMap.put(STARTUPPDDLPLANNER, Boolean.toString(isPddlPlannerStartupSelected()));
        propsMap.put(STARTUPPDDLEXECUTOR, Boolean.toString(isExecutorStartupSelected()));
        propsMap.put(STARTUPPDDLOBJECTSP, Boolean.toString(isObjectSpStartupSelected()));
        propsMap.put(STARTUPPDDLOBJECTVIEW, Boolean.toString(isObject2DViewStartupSelected()));
        propsMap.put(STARTUPROBOTCRCLCLIENT, Boolean.toString(isRobotCrclGUIStartupSelected()));
        propsMap.put(STARTUPROBOTCRCLSIMSERVER, Boolean.toString(isRobotCrclSimServerStartupSelected()));
        propsMap.put(STARTUPROBOTCRCLFANUCSERVER, Boolean.toString(isRobotCrclFanucServerStartupSelected()));
        propsMap.put(STARTUPROBOTCRCLMOTOMANSERVER, Boolean.toString(isRobotCrclMotomanServerStartupSelected()));
        propsMap.put(STARTUPCONNECTDATABASE, Boolean.toString(isConnectDatabaseCheckboxSelected()));
        propsMap.put(STARTUPCONNECTVISION, Boolean.toString(isConnectVisionOnStartupSelected()));
        propsMap.put(STARTUPEXPLOREGRAPHDB, Boolean.toString(isExploreGraphDBStartupSelected()));
//        propsMap.put(STARTUPCRCLWEBAPP, Boolean.toString(jCheckBoxMenuItemStartupCRCLWebApp.isSelected()));
        propsMap.put(CRCLWEBAPPPORT, Integer.toString(crclWebServerHttpPort));
        propsMap.put(STARTUP_ACTIVE_WIN, activeWin.toString());
        propsMap.put(STARTUPKITINSPECTION, Boolean.toString(isKitInspectionStartupSelected()));
        propsMap.put(MAX_LIMIT_PROP, maxLimit.toString());
        propsMap.put(MIN_LIMIT_PROP, minLimit.toString());
        propsMap.put(SNAP_SHOT_ENABLE_PROP, Boolean.toString(isSnapshotCheckboxSelected()));
        propsMap.put(SNAP_SHOT_WIDTH_PROP, Integer.toString(snapShotWidth));
        propsMap.put(SNAP_SHOT_HEIGHT_PROP, Integer.toString(snapShotHeight));
        propsMap.put(RELOAD_SIM_FILES_ON_REVERSE_PROP, Boolean.toString(isReloadSimFilesOnReverseCheckboxSelected()));
        propsMap.put(LOG_CRCL_PROGRAMS_ENABLED, Boolean.toString(isLogCrclProgramsEnabled()));
        setDefaultRobotName();
        if (null != robotName) {
            propsMap.put(APRSROBOT_PROPERTY_NAME, robotName);
        }
        if (null != taskName) {
            propsMap.put(APRSTASK_PROPERTY_NAME, taskName);
        }
        if (null != fanucCRCLMain) {
            this.fanucCrclPort = fanucCRCLMain.getLocalPort();
            this.fanucRobotHost = fanucCRCLMain.getRemoteRobotHost();
            propsMap.put(FANUC_CRCL_LOCAL_PORT, Integer.toString(fanucCrclPort));
            propsMap.put(FANUC_ROBOT_HOST, fanucRobotHost);
        }
        if (null != motomanCrclServerJInternalFrame) {
            this.motomanCrclPort = motomanCrclServerJInternalFrame.getCrclPort();
            propsMap.put(MOTOMAN_CRCL_LOCAL_PORT, Integer.toString(motomanCrclPort));
        }

        Properties props = new Properties();
        props.putAll(propsMap);
        System.out.println("AprsSystem saving properties to " + propertiesFile.getCanonicalPath());
//        try (FileWriter fw = new FileWriter(propertiesFile)) {
//            props.store(fw, "");
//        }
        Utils.saveProperties(propertiesFile, props);
        updateSubPropertiesFiles();
        if (null != this.kitInspectionJInternalFrame) {
            this.kitInspectionJInternalFrame.saveProperties();
        }
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.saveProperties();
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.saveProperties();
        }
        if (null != this.visionToDbJInternalFrame) {
            this.visionToDbJInternalFrame.saveProperties();
        }
        if (null != this.object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.saveProperties();
        }
        if (null != this.crclClientJInternalFrame) {
            crclClientJInternalFrame.saveProperties();
        }
        if (null != this.simServerJInternalFrame) {
            simServerJInternalFrame.saveProperties();
        }
        if (null != this.motomanCrclServerJInternalFrame) {
            motomanCrclServerJInternalFrame.saveProperties();
        }
        if (null != this.fanucCRCLServerJInternalFrame) {
            fanucCRCLServerJInternalFrame.saveProperties();
        }
        if (null != dbSetup) {
            File dbPropsFile = new File(propertiesDirectory, this.propertiesFileBaseString + "_dbsetup.txt");
            if (null != dbSetupJInternalFrame) {
                dbSetupJInternalFrame.setPropertiesFile(dbPropsFile);
            }
            DbSetupBuilder.savePropertiesFile(dbPropsFile, dbSetup);
        }
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
            if (fanucCRCLMain != null) {
                setRobotName("Fanuc");
            } else if (motomanCrclServerJInternalFrame != null) {
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

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        String prefLaf = "Nimbus"; // "GTK+"; // Nimbus, Metal ...
        /* Set the preferred look and feel */

        if (prefLaf != null) {
            /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
             * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
             */
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                    System.out.println("info.getName() = " + info.getName());
                    if (prefLaf.equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;

                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
                java.util.logging.Logger.getLogger(AprsSystem.class
                        .getName()).log(java.util.logging.Level.SEVERE, null, ex);

            }
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AprsSystem aFrame = new AprsSystem();
                aFrame.defaultInit()
                        .thenRun(() -> aFrame.setVisible(true));
            }
        });
    }

    /**
     * Get the file where properties are read from and written to.
     *
     * @return properties file
     */
    @Override
    public File getPropertiesFile() {
        return propertiesFile;
    }

    @Override
    public final void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
        if (null == propertiesFile) {
            System.err.println("propertiesFile == null");
            return;
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
                System.out.println("Directory " + propertiesDirectory.getCanonicalPath() + " does not exist. (Creating it now!)");
                propertiesDirectory.mkdirs();

            } catch (IOException ex) {
                Logger.getLogger(AprsSystem.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            updateSubPropertiesFiles();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        updateTitle("", "");
    }

    private String propertiesFileBaseString = "";

    private void updateSubPropertiesFiles() throws IOException {
        String base = propertiesFile.getName();
        int pindex = base.indexOf('.');
        if (pindex > 0) {
            base = base.substring(0, pindex);
        }
        propertiesFileBaseString = base;
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_pddlPlanner.txt"));
        }
        if (null != this.kitInspectionJInternalFrame) {
            this.kitInspectionJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_kitInspection.txt"));
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.setPropertiesFile(new File(propertiesDirectory, base + "_actionsToCrclProperties.txt"));
        }
        if (null != this.visionToDbJInternalFrame) {
            visionToDbJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_visionToDBProperties.txt"));
        }
        if (null != this.object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_object2DViewProperties.txt"));
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
            crclClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_crclPendantClientProperties.txt"));
        }

        if (null != simServerJInternalFrame) {
            simServerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_crclSimServerProperties.txt"));
        }
        if (null != motomanCrclServerJInternalFrame) {
            motomanCrclServerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_motomanCrclServerProperties.txt"));
        }
        if (null != dbSetupJInternalFrame) {
            File dbPropsFile = new File(propertiesDirectory, this.propertiesFileBaseString + "_dbsetup.txt");
            dbSetupJInternalFrame.setPropertiesFile(dbPropsFile);
        }
        if (null != fanucCRCLServerJInternalFrame) {
            fanucCRCLServerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_fanucCrclServerProperties.txt"));
        }
    }

    @Nullable
    private volatile XFuture<?> disconnectDatabaseFuture = null;

    public XFuture<?> startDisconnectDatabase() {
        setConnectDatabaseCheckboxEnabled(false);
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        XFuture<?> ret = XFuture.runAsync("disconnectDatabase",
                this::disconnectDatabase, connectService);
        disconnectDatabaseFuture = ret;
        return ret;
    }

    private void disconnectDatabase() {

        if (null == dbSetupJInternalFrame) {
            return;
        }
//        assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
        try {
            dbConnected = false;
            if (null != connectDatabaseFuture) {
                connectDatabaseFuture.cancel(true);
                connectDatabaseFuture = null;
            }
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            //dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(false).build());
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
            setConnectDatabaseCheckboxSelected(false);
            System.out.println("Finished disconnect from database.");
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int priority;

    /**
     * Get the value of priority
     *
     * @return the value of priority
     */
    @Override
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

    @Override
    public void close() {
        closing = true;
        if (null != eventLogPrinter) {
            try {
                eventLogPrinter.close();
            } catch (IOException ex) {
                Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
            eventLogPrinter = null;
        }
        System.out.println("AprsSystem.close()");
        closeAllWindows();
        connectService.shutdownNow();
        if (null != aprsSystemDisplayJFrame) {
            aprsSystemDisplayJFrame.setVisible(false);
            aprsSystemDisplayJFrame.dispose();
        }
        Runnable r = onCloseRunnable.getAndSet(null);
        if (null != r) {
            r.run();
        }
    }

    public void closeActionsListExcecutor() {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.close();
            pddlExecutorJInternalFrame1.setVisible(false);
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
            }
        }
    }

    public void closePddlPlanner() {
        if (null != pddlPlannerJInternalFrame) {
            pddlPlannerJInternalFrame.close();
            pddlPlannerJInternalFrame.setVisible(false);
        }
    }

    private final boolean lastContinueCrclProgramResult = false;

    /**
     * Continue or start executing the currently loaded CRCL program.
     * <p>
     * The actions will be executed in another thread after this method returns.
     * The task can be monitored or canceled using the returned future.
     *
     * @return a future that can be used to monitor, extend or cancel the
     * underlying task.
     */
    @Override
    public XFuture<Boolean> continueCrclProgram() {
        setStartRunTime();
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.continueCurrentProgram(stepMode);
        } else {
            return XFuture.completedFuture(false);
        }
    }

    /**
     * Get the value of enableDatabaseUpdates
     *
     * @return current setting for enableDatabaseUpdates
     */
    @Override
    public boolean isEnableVisionToDatabaseUpdates() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.isEnableDatabaseUpdates();
    }

    /**
     * Set the value of enableDatabaseUpdates
     *
     * @param enableDatabaseUpdates new value of enableDatabaseUpdates
     * @param requiredParts map of part names to required number of each part
     * type
     */
    @Override
    public void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        visionToDbJInternalFrame.setEnableDatabaseUpdates(enableDatabaseUpdates, requiredParts);
        if (enableDatabaseUpdates && null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }

    @Nullable private volatile File logDir = null;

    /**
     * Get the current directory for saving log files
     *
     * @return log files directory
     * @throws java.io.IOException file can not be created ie default log
     * directory does not exist.
     */
    @Override
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

    @Nullable private volatile File logImageDir = null;

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

    @Nullable private volatile File logCrclProgramDir = null;

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

    @Nullable private volatile File logCrclStatusDir = null;

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

    @Nullable private volatile File logCrclCommandDir = null;

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

    @Nullable public File logCrclCommand(String prefix, CRCLCommandType cmd) {
        File f = null;
        try {
            String xmlString = CRCLSocket.getUtilSocket().commandToPrettyString(cmd);
            f = createTempFile(prefix, ".xml", getLogCrclCommandDir());
            try (PrintWriter printer = new PrintWriter(new FileWriter(f))) {
                printer.print(xmlString);
            }
        } catch (JAXBException | CRCLException | IOException ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        return f;
    }

    @Nullable public File logCrclStatus(String prefix, CRCLStatusType stat) {
        File f = null;
        try {
            String xmlString = CRCLSocket.getUtilSocket().statusToPrettyString(stat);
            f = createTempFile(prefix, ".xml", getLogCrclStatusDir());
            try (PrintWriter printer = new PrintWriter(new FileWriter(f))) {
                printer.print(xmlString);
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        return f;
    }

    private static String cleanAndLimitFilePrefix(String prefix_in) {
        if (prefix_in.length() > 80) {
            prefix_in = prefix_in.substring(0, 79);
        }
        String prefixOut = prefix_in.replaceAll("[ \t:;-]+", "_").replace('\\', '_').replace('/', '_');
        if (prefixOut.length() > 80) {
            prefixOut = prefixOut.substring(0, 79);
        }
        if (!prefixOut.endsWith("_")) {
            prefixOut = prefixOut + "_";
        }
        return prefixOut;
    }

    /**
     * Create a temporary file in the current log file directory with the
     * standard timestamp string.
     *
     * @param prefix string filename will begin with
     * @param suffix string filename will end with (typically an extention eg
     * ".csv")
     * @return reference to created file
     * @throws IOException directory doesn't exist etc.
     */
    @Override
    public File createTempFile(String prefix, String suffix) throws IOException {
        if (suffix.endsWith(".PNG")) {
            System.out.println("suffix = " + suffix);
        }
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, getLogDir());
    }

    /**
     * Create a temporary file in the given directory with the standard
     * timestamp string.
     *
     * @param prefix string filename will begin with
     * @param suffix string filename will end with (typically an extention eg
     * ".csv")
     * @param dir directory to create file in
     * @return reference to created file
     * @throws IOException directory doesn't exist etc.
     */
    @Override
    public File createTempFile(String prefix, String suffix, File dir) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, dir);
    }

    public File createImageTempFile(String prefix) throws IOException {
        return createTempFile(prefix, ".PNG", getLogImageDir());
    }

    private volatile String asString = "";

    @Override
    public String toString() {
        return asString;
    }

    /**
     * Get a list with information on how the most recently loaded CRCL program
     * has run so far.
     *
     * @return run data for last program
     */
    @Override
    public List<ProgramRunData> getLastProgRunDataList() {
        if (null == crclClientJInternalFrame) {
            return Collections.emptyList();
        }
        return crclClientJInternalFrame.getLastProgRunDataList();
    }

    /**
     * Save the given run data which contains information on how a given program
     * run went to a CSV file.
     *
     * @param f file to save
     * @param list data to write to file
     * @throws IOException file does not exist or not writeable etc
     */
    @Override
    public void saveProgramRunDataListToCsv(File f, List<ProgramRunData> list) throws IOException {
        if (null == crclClientJInternalFrame) {
            return;
        }
        crclClientJInternalFrame.saveProgramRunDataListToCsv(f, list);
    }

    /**
     * Save the current run data which contains information on how a given
     * program run went to a CSV file.
     *
     * @param f file to save
     * @throws IOException file does not exist or not writeable etc
     */
    @Override
    public void saveLastProgramRunDataListToCsv(File f) throws IOException {
        if (null == crclClientJInternalFrame) {
            return;
        }
        logEvent("saveLastProgramRunDataListToCsv", f.getCanonicalPath());
        crclClientJInternalFrame.saveLastProgramRunDataListToCsv(f);
    }
}