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
package aprs.framework;

import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupJInternalFrame;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.DbType;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.explore.ExploreGraphDbJInternalFrame;
import aprs.framework.kitinspection.KitInspectionJInternalFrame;
import aprs.framework.logdisplay.LogDisplayJInternalFrame;
import aprs.framework.database.PartsTray;
import aprs.framework.database.Slot;
import aprs.framework.database.Tray;
import aprs.framework.learninggoals.GoalLearner;
import aprs.framework.pddl.executor.PddlActionToCrclGenerator;
import aprs.framework.pddl.executor.PddlActionToCrclGenerator.PoseProvider;
import aprs.framework.pddl.executor.PddlExecutorJInternalFrame;
import aprs.framework.pddl.executor.PositionMap;
import aprs.framework.pddl.planner.PddlPlannerJInternalFrame;
import aprs.framework.simview.Object2DJPanel;
import aprs.framework.simview.Object2DViewJInternalFrame;
import aprs.framework.spvision.UpdateResults;
import aprs.framework.spvision.VisionToDbJInternalFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import aprs.framework.tomcat.CRCLWebAppRunner;
import com.github.wshackle.fanuccrclservermain.FanucCRCLMain;
import com.github.wshackle.fanuccrclservermain.FanucCRCLServerJInternalFrame;
import com.github.wshackle.crcl4java.motoman.ui.MotomanCrclServerJInternalFrame;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLProgramType;
import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
import crcl.base.CommandStatusType;
import crcl.base.EndCanonType;
import crcl.base.InitCanonType;
import crcl.base.MiddleCommandType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
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
import java.util.Comparator;
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
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.xml.sax.SAXException;
import rcs.posemath.PmCartesian;
import crcl.ui.client.PendantClientJInternalFrame;


/**
 * AprsJFrame is the container for one robotic system in the APRS (Agility
 * Performance of Robotic Systems) framework.
 *
 * Internal windows are used to represent each of the modules within the system.
 * Vision, Sensor Processing, Planning, Execution, and the CRCL (Canonical Robot
 * Command Language) client and server.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsJFrame extends javax.swing.JFrame implements DisplayInterface, AutoCloseable, SlotOffsetProvider {

    @MonotonicNonNull private VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    @MonotonicNonNull private PddlExecutorJInternalFrame pddlExecutorJInternalFrame1 = null;
    @MonotonicNonNull private Object2DViewJInternalFrame object2DViewJInternalFrame = null;
    @MonotonicNonNull private PddlPlannerJInternalFrame pddlPlannerJInternalFrame = null;
    @MonotonicNonNull private DbSetupJInternalFrame dbSetupJInternalFrame = null;
    @MonotonicNonNull private volatile PendantClientJInternalFrame crclClientJInternalFrame = null;
    @MonotonicNonNull private SimServerJInternalFrame simServerJInternalFrame = null;
    @MonotonicNonNull private LogDisplayJInternalFrame logDisplayJInternalFrame = null;
    @MonotonicNonNull private FanucCRCLMain fanucCRCLMain = null;
    @MonotonicNonNull private FanucCRCLServerJInternalFrame fanucCRCLServerJInternalFrame = null;
    @MonotonicNonNull private ExploreGraphDbJInternalFrame exploreGraphDbJInternalFrame = null;
    @MonotonicNonNull private MotomanCrclServerJInternalFrame motomanCrclServerJInternalFrame = null;
    @MonotonicNonNull private KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

    private String taskName;

    private volatile long lastStartRunTime = -1;
    private volatile long lastStopRunTime = -1;
    private volatile long lastRunDuration = -1;
    private volatile long lastStopDuration = -1;
    private final AtomicLong effectiveStartRunTime = new AtomicLong(-1);
    private final AtomicLong effectiveStopRunTime = new AtomicLong(-1);
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Check if the user has selected a check box asking for snapshot files
     * to be created for logging/debugging.
     * 
     * @return Has user enabled snapshots?
     */
    public boolean isSnapshotsEnabled() {
        return jCheckBoxMenuItemSnapshotImageSize.isSelected();
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
     * Get the most recent image created when scanning for desired part locations.
     * 
     * @return image of most recent scan
     */
    public Image getScanImage() {
        return scanImage;
    }

    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    @Nullable public PoseProvider getPddlExecExternalPoseProvider() {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        return pddlExecutorJInternalFrame1.getExternalPoseProvider();
    }

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalPoseProvider new value of externalGetPoseFunction
     */
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
     * stopped atleast once.
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
     * @return duration in milliseconds or 0 if it has not been started and
     * stopped atleast once.
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

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame
     * is received from vision.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    public XFuture<List<PhysicalItem>> getSingleVisionToDbUpdate() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getSingleUpdate();
    }

    public long getLastSingleVisionToDbUpdateTime() {
        return visionToDbJInternalFrame.getLastUpdateTime();
    }
    
    
    public long getSingleVisionToDbNotifySingleUpdateListenersTime() {
        return visionToDbJInternalFrame.getNotifySingleUpdateListenersTime();
    }

    
    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    public List<PartsTray> getPartsTrayList() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getPartsTrayList();
    }

    /**
     * Force the Object 2D Simulation View to refresh, this method has no effect
     * if the view is not visible or is not in simulation mode.
     */
    public void refreshSimView() {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }

    private @Nullable SlotOffsetProvider externalSlotOffsetProvider = null;

    /**
     * Get the value of externalSlotOffsetProvider
     *
     * @return the value of externalSlotOffsetProvider
     */
    @Nullable public SlotOffsetProvider getExternalSlotOffsetProvider() {
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
     * Get a list of slots with names and relative position offsets for a given
     * kit or parts tray name.
     *
     * @param name name of the type of kit or slot tray
     * @param ignoreEmpty if false  
     *          no slots being found logs a verbose error message 
     *          and throws IllegalStateException (good for fail fast) or
     *  if true 
     *          simply returns an empty list (good or display or when multiple 
     *          will be checked.
     * 
     * @return list of slots with relative position offsets.
     */
    @Override
    public List<Slot> getSlotOffsets(String name,boolean ignoreEmpty) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.getSlotOffsets(name,ignoreEmpty);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.getSlotOffsets(name,ignoreEmpty);
    }

    /**
     * Save an image showing the locations of new database items. The database
     * will be queried and the file saved asynchronously in another thread.
     *
     * @param f file to save
     * @return future with information on when the operation is complete.
     */
    public XFuture<Void> startVisionToDbNewItemsImageSave(File f) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.startNewItemsImageSave(f);
    }

    /**
     * Get a list of slots associated with a particular tray.
     *
     * @param tray tray to obtain list of slots
     * @return list of slots
     */
    public List<Slot> getSlots(Tray tray,boolean ignoreEmpty) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.getSlotOffsets(tray.getName(),ignoreEmpty);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.getSlots(tray);
    }

    /**
     * Set the preference for displaying a '+' at the current position of the robot
     * tool.
     * 
     * @param v should the position be displayed.
     */
    public void setSimViewTrackCurrentPos(boolean v) {
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        this.object2DViewJInternalFrame.setTrackCurrentPos(v);
    }

    /**
     * Set the simulation view to simulation mode and disconnect from any
     * vision data socket.
     * 
     */
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
    public double getVisionToDBRotationOffset() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return this.visionToDbJInternalFrame.getRotationOffset();
    }

    /**
     * Pause a currently executing CRCL program.
     *
     * No action is taken if the CRCL Client has not been started and connected
     * or not program was currently running.
     */
    public void pauseCrclProgram() {
        if (null != crclClientJInternalFrame) {
            crclClientJInternalFrame.pauseCrclProgram();
        }
    }

    /**
     * Get the Kit Inspection InternalJFrame if it has been created.
     *
     * @return Kit Inspection InternalJFrame
     */
    @Nullable public KitInspectionJInternalFrame getKitInspectionJInternalFrame() {
        return kitInspectionJInternalFrame;
    }

    private volatile int runNumber = (int) ((System.currentTimeMillis() / 10000) % 1000);

    /**
     * Return the user's preference on whether the stack trace be dumped for
     * more verbose logging.
     *
     * @return current setting of menu item
     */
    public boolean isEnableDebugDumpstacks() {
        return jCheckBoxMenuItemEnableDebugDumpstacks.isSelected();
    }

    /**
     * Creates a run name useful for identifying a run in log files or the names
     * of snapshot files. The name is composed of the current robot name, task
     * name and robot number.
     *
     * @return current run name.
     */
    public String getRunName() {
        return ((this.taskName != null) ? this.taskName.replace(" ", "-") : "") + "-run-" + String.format("%03d", runNumber) + "-"
                + ((this.robotName != null) ? this.robotName : "") + (isReverseFlag() ? "-Reverse" : "");
    }

    /**
     * Checks whether there is currently a CRCL program that is loaded but
     * paused.
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
     * Get the thread currently running a CRCL program if it exists and is
     * known. Only useful for debugging.
     *
     * @return thread
     */
    @Nullable public Thread getCrclRunProgramThread() {
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
    @Nullable public XFuture<Boolean> getCrclRunProgramFuture() {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
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

    /**
     * Set a state where attempts to start a crcl program will be blocked.
     *
     * @return count to pass to stopBlockingCrclProgram when running crcl
     * programs should again be allowed.
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
    @Nullable public CRCLProgramType getCrclProgram() {
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
    public void setExecutorOption(String key, String value) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setOption(key, value);
        }
    }

    /**
     * Add a position map.
     *
     * The position map is similar to a transform in that it may offset
     * positions output by the executor but may also be used to change scaling
     * or correct for non uniform distortions from the sensor system or
     * imperfect kinematic functions in the robot. Multiple position maps may be
     * stacked to account for different sources of error or transformation.
     *
     * @param pm position map to be added
     */
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
    @Nullable public PoseType getCurrentPose() {

        if (null != crclClientJInternalFrame) {
            try {
                if (!crclClientJInternalFrame.isConnected()) {
                    crclClientJInternalFrame.connectCurrent();
                }
            } catch (Exception e) {
                Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, e);
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
    @Nullable public PointType getCurrentPosePoint() {
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
    public boolean isConnected() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.isConnected();
        }
        return false;
    }

    /**
     * Attempt to connect or disconnect the CRCL client from the CRCL server by
     * opening or closed the appropriate socket.
     *
     * NOTE: for setConnected(true) to succeed the robot port and host name must
     * have previously been set or read from the property file.
     *
     * NOTE: disconnectRobot() is the same as setConnected(false) except it also
     * sets the robot name to null.
     *
     * @param connected the new desired connected state.
     */
    public void setConnected(boolean connected) {
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
    }

    /**
     * Get the value of taskName
     *
     * @return the value of taskName
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Get the future that can be used to determine when the last requested safe
     * abort is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    @Nullable public XFuture<Void> getSafeAbortFuture() {
        return safeAbortFuture;
    }

    /**
     * Get the future that can be used to determine when the last requested run
     * program is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    @Nullable public XFuture<Boolean> getLastRunProgramFuture() {
        return lastRunProgramFuture;
    }

    /**
     * Get the future that can be used to determine when the last requested
     * resumed action is finished. Only useful for debugging.
     *
     * @return future or null if no resume requested
     */
    @Nullable public XFuture<Boolean> getLastResumeFuture() {
        return lastResumeFuture;
    }

    @Nullable private volatile XFuture<Void> safeAbortFuture = null;
    @Nullable private volatile XFuture<Void> safeAbortAndDisconnectFuture = null;

    /**
     * Attempt to safely abort the current CRCL program in a way that does not
     * leave the part in the gripper nor in a position obstructing the vision
     * sensor. If the robot currently has a part in the gripper the program will
     * continue until the part is placed and the robot is moved out of the way
     * of the vision system.
     *
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned
     * future.
     *
     * @param comment optional comment is used when debugging to track which
     * future was created by which caller and to improve log messages
     * @return a future that can be tested or used to wait until the abort is
     * completed.
     */
    public XFuture<Void> startSafeAbort(String comment) {

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
                }).thenComposeAsync(x -> waitAllLastFutures(), runProgramService);
        return safeAbortFuture;
    }

    @Nullable private volatile Thread startSafeAbortAndDisconnectThread = null;
    private volatile long startSafeAbortAndDisconnectTime = -1;
    private volatile StackTraceElement startSafeAbortAndDisconnectStackTrace @Nullable []  = null;

    @Nullable private volatile Thread startSafeAbortThread = null;
    private volatile long startSafeAbortTime = -1;
    private volatile StackTraceElement startSafeAbortStackTrace @Nullable []  = null;

    /**
     * Safely abort the current CRCL program and then disconnect from the
     * robot's CRCL server.
     *
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned
     * future. * @return a future that can be tested or used to wait until the
     * abort and disconnect is completed.
     *
     * @param comment optional comment is used when debugging to track which
     * future was created by which caller and to improve log messages
     * @return future providing info on when complete
     */
    public XFuture<Void> startSafeAbortAndDisconnect(String comment) {
        if (isAborting()) {
            String errMsg = "startSafeAbort(" + comment + ") called when already aborting";
            setTitleErrorString(errMsg);
            throw new IllegalStateException(errMsg);
        }
        try {
            Thread curThread = Thread.currentThread();
            startSafeAbortAndDisconnectThread = curThread;
            startSafeAbortAndDisconnectTime = System.currentTimeMillis();
            startSafeAbortAndDisconnectStackTrace = curThread.getStackTrace();

            if (isConnected()) {
                assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";

                XFuture<Void> localSafeAbortFuture
                        = this.pddlExecutorJInternalFrame1.startSafeAbort(comment);
                safeAbortFuture
                        = localSafeAbortFuture;

                safeAbortAndDisconnectFuture
                        = localSafeAbortFuture
                                .thenRun(() -> {
//                                    if (null != continousDemoFuture) {
//                                        continousDemoFuture.cancelAll(true);
//                                        continousDemoFuture = null;
//                                    }
                                    setStopRunTime();
                                })
                                .thenCompose(x -> waitAllLastFutures())
                                .thenRunAsync(localSafeAbortFuture.getName() + ".disconnect." + robotName, this::disconnectRobotPrivate, runProgramService)
                                .thenComposeAsync(x -> waitAllLastFutures(), runProgramService);
            } else {
                safeAbortFuture = XFuture.completedFutureWithName("startSafeAbortAndDisconnect(" + comment + ").alreadyDisconnected", null);
                safeAbortAndDisconnectFuture = safeAbortFuture;
            }
            return safeAbortAndDisconnectFuture;
        } catch (Exception e) {
            setTitleErrorString(e.toString());
            XFuture<Void> ret = new XFuture<>("startSafeAbortAndDisconnect." + e.toString());
            ret.completeExceptionally(e);
            return ret;
        }
    }

    @SuppressWarnings({"unchecked", "nullness"})
    private XFuture<Void> wait(@Nullable XFuture< ?> f) {
        if (null == f || f.isCancelled() || f.isCompletedExceptionally() || f.isDone()) {
            return XFuture.completedFutureWithName("waitReady f=" + f, null);
        } else {
            return f.handle((x, t) -> null);
        }
    }

    private XFuture<Void> waitAllLastFutures() {
        return XFuture.allOf(wait(lastContinueActionListFuture),
                wait(lastRunProgramFuture),
                wait(lastStartActionsFuture));
    }

    /**
     * Get a map of updates that were attempted the last time data was received
     * from the vision system.
     *
     * Only useful for debugging.
     *
     * @return map of results
     */
    public Map<String, UpdateResults> getDbUpdatesResultMap() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.getUpdatesResultMap();
    }

    @Nullable private volatile XFuture<Void> disconnectRobotFuture = null;

    /**
     * Disconnect from the robot's crcl server and set robotName to null.
     *
     * Note: setConnected(false) also disconnects from the crcl server but
     * leaves the robotName unchanged.
     *
     * @return future providing info on when complete
     */
    public XFuture<Void> disconnectRobot() {
        disconnectRobotCount.incrementAndGet();
        checkReadyToDisconnect();
        enableCheckedAlready = false;
        XFuture<Void> ret = waitForPause().
                thenRunAsync("disconnectRobot(" + getRobotName() + ")", this::disconnectRobotPrivate, connectService);
        this.disconnectRobotFuture = ret;
        System.out.println("disconnectRobotFuture = " + disconnectRobotFuture);
        System.out.println("connectService = " + connectService);
        return ret;
    }

    private final AtomicInteger disconnectRobotCount = new AtomicInteger();

    private void disconnectRobotPrivate() {
        checkReadyToDisconnect();
        enableCheckedAlready = false;
        disconnectRobotCount.incrementAndGet();
//        if (null != continousDemoFuture) {
//            continousDemoFuture.cancelAll(true);
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
    public XFuture<Void> connectRobot(String robotName, String host, int port) {
        maybeSetOrigRobotName(robotName);
        maybeSetOrigCrclRobotHost(host);
        maybeSetOrigCrclRobotPort(port);
        if (isConnected()
                && !isPaused()
                && null != this.robotName
                && Objects.equals(robotName, robotName)
                && Objects.equals(this.getRobotCrclHost(), host)
                && this.getRobotCrclPort() == port) {
            return XFuture.completedFuture(null);
        }
        enableCheckedAlready = false;
        return waitForPause().
                thenRunAsync(() -> connectRobotPrivate(robotName, host, port), connectService);
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
    }

    /**
     * Get robot's CRCL host.
     *
     * @return robot's CRCL host.
     */
    @Nullable public String getRobotCrclHost() {
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

    final ConcurrentLinkedDeque<XFuture<@Nullable ?>> futuresToCompleteOnUnPause = new ConcurrentLinkedDeque<>();

    private void notifyPauseFutures() {
        for (XFuture<@Nullable ?> f : futuresToCompleteOnUnPause) {
            f.complete(null);
        }
    }

    private void cancelPauseFutures() {
        for (XFuture<@Nullable ?> f : futuresToCompleteOnUnPause) {
            f.cancelAll(true);
        }
    }

    @Nullable private volatile XFuture<Void> lastPauseFuture = null;

    private XFuture<Void> waitForPause() {
        boolean paused = isPaused();
        XFuture<Void> pauseFuture = new XFuture<>("pauseFuture." + paused);
        if (paused) {
            System.out.println("adding " + pauseFuture + " to " + futuresToCompleteOnUnPause);
            futuresToCompleteOnUnPause.add(pauseFuture);
        } else {
            pauseFuture.complete(null);
        }
        lastPauseFuture = pauseFuture;
        return pauseFuture;
    }

    @Nullable private volatile XFuture<Boolean> lastContinueActionListFuture = null;
    @Nullable private volatile String lastContinueActionListFutureComment = null;
    private volatile int lastContinueStartAbortCount = -1;

    /**
     * Continue or start executing the currently loaded set of PDDL actions.
     *
     * The actions will be executed in another thread after this method returns.
     * The task can be monitored or canceled using the returned future.
     *
     * @param comment optional comment for why continueActionList is needed used
     * for logs, and/or snapshots
     *
     * @return a future that can be used to monitor, extend or cancel the
     * underlying task.
     *
     */
    public XFuture<Boolean> continueActionList(String comment) {
        setStartRunTime();
        lastContinueActionListFutureComment = comment;
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        lastContinueStartAbortCount = startAbortCount;
        lastContinueActionListFuture
                = waitForPause()
                        .thenApplyAsync("AprsJFrame.continueActionList" + comment,
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
        return lastContinueActionListFuture;
    }

    /**
     * Get the closest distance between the robot TCP and any part.
     *
     * @return closest distance
     */
    public double getClosestRobotPartDistance() {
        assert (null != object2DViewJInternalFrame) : "null == object2DViewJInternalFrame ";
        return this.object2DViewJInternalFrame.getClosestRobotPartDistance();
    }

    /**
     * Get the current setting for whether the object view is using simulation.
     *
     * @return if object view is simulated
     */
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
        updateTitle("", "");
    }

    @Nullable private String robotName = null;

    /**
     * Get the value of robotName
     *
     * @return the value of robotName
     */
    @Nullable public String getRobotName() {
        return robotName;
    }

    @Nullable private volatile Thread setRobotNameNullThread = null;
    private volatile StackTraceElement setRobotNameNullStackTrace @Nullable []  = null;
    private volatile long setRobotNameNullThreadTime = -1;
    @Nullable private volatile Thread setRobotNameNonNullThread = null;
    private volatile StackTraceElement setRobotNameNonNullStackTrace @Nullable []  = null;
    private volatile long setRobotNameNonNullThreadTime = -1;

    /**
     * Set the value of robotName
     *
     * @param robotName new value of robotName
     */
    public void setRobotName(@Nullable String robotName) {
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
        Utils.runOnDispatchThread(() -> updateTitle("", ""));
    }

    private void maybeSetOrigRobotName(@Nullable String robotName1) {
        if (null == this.origRobotName || this.origRobotName.length() < 1) {
            if (null != robotName1 && robotName1.length() > 0) {
                this.origRobotName = robotName1;
            }
        }
    }

    private void checkReadyToDisconnect() throws IllegalStateException {
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
    public void removeCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l) {
        if (null != crclClientJInternalFrame) {
            crclClientJInternalFrame.removeCurrentPoseListener(l);
        }
    }

    /**
     * Set the currently loaded CRCL program
     *
     * @param program CRCL program to load.
     * @throws JAXBException a command within the program violates a constraint
     * in the schema
     */
    public synchronized void setCRCLProgram(CRCLProgramType program) throws JAXBException {
        if (null != crclClientJInternalFrame) {
            synchronized (crclClientJInternalFrame) {
                setProgram(program);
            }
        }
    }

    @Nullable private volatile XFuture<Boolean> lastRunProgramFuture = null;

    /**
     * Load the given program an begin running it.
     *
     * The program will be run asynchronously in another thread after this
     * method has returned. The task can be modified, canceled or extended with
     * the returned future. The boolean contained within the future will be true
     * if the program completed successfully and false for non exceptional
     * errors.
     *
     * @param program program to be loaded
     * @return future that can be used to monitor, cancel or extend the
     * underlying task
     * @throws JAXBException a command within the program violates a constraint
     * in the schema
     */
    public XFuture<Boolean> startCRCLProgram(CRCLProgramType program) throws JAXBException {
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
        return crclClientJInternalFrame.runCurrentProgram();
    }

    /**
     * Run a CRCL program.
     *
     * @param program CRCL program to run
     * @return whether the program completed successfully
     * @throws JAXBException the program did not meet schema requirements
     */
    public boolean runCRCLProgram(CRCLProgramType program) throws JAXBException {
        setProgram(program);
        assert (null != crclClientJInternalFrame) : "null == pendantClientJInternalFrame ";
        return crclClientJInternalFrame.runCurrentProgram();
    }

    /**
     * Immediately abort the currently running CRCL program and PDDL action
     * list.
     *
     * NOTE: This may leave the robot in a state with the part held in the
     * gripper or with the robot obstructing the view of the vision system.
     *
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
        jCheckBoxMenuItemContinuousDemo.setSelected(false);
    }

    /**
     * Create a string with current details for display and/or logging.
     *
     * @return details string
     */
    public String getDetailsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.toString()).append("\r\n");
        if (null != crclClientJInternalFrame) {
            CRCLCommandType cmd = crclClientJInternalFrame.getCurrentProgramCommand();
            if (null != cmd) {
                try {
                    sb.append("crcl_cmd=").append(CRCLSocket.getUtilSocket().commandToSimpleString(cmd)).append("\r\n");
                } catch (ParserConfigurationException | SAXException | IOException ex) {
                    sb.append("crcl_cmd= Exception : ").append(ex).append("\r\n");
                    Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
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
        PddlExecutorJInternalFrame execFrame = this.pddlExecutorJInternalFrame1;
        if (null != execFrame) {
            List<PddlAction> actions = execFrame.getActionsList();
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
        sb.append("reverseFlag=").append(jCheckBoxMenuItemReverse.isSelected()).append(", ");
        sb.append("paused=").append(jCheckBoxMenuItemPause.isSelected()).append("\r\n");
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

        if (null != titleErrorString && titleErrorString.length() > 0) {
            sb.append("titleErrorString=").append(titleErrorString).append("\r\n");
        }
//        sb.append("1111111111222222222233333333334444444444555555555566666666667777777777788888888899999999990000000000111111111122222222223333333333\r\n");
//        sb.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\r\n");
        return sb.toString().trim();
    }

    /**
     * Abort the current CRCL program.
     */
    public void abortCrclProgram() {
        if (null != crclClientJInternalFrame && crclClientJInternalFrame.isConnected()) {
            crclClientJInternalFrame.abortProgram();
        }
    }

    @Nullable private PrintStream origOut = null;
    @Nullable private PrintStream origErr = null;

    static private class MyPrintStream extends PrintStream {

        final private PrintStream ps;
        final private LogDisplayJInternalFrame logDisplayJInternalFrame;

        public MyPrintStream(PrintStream ps, LogDisplayJInternalFrame logDisplayJInternalFrame) {
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
    private String fanucNeighborhoodName = "AgilityLabLRMate200iD"; // FIXME hard-coded default
    private boolean fanucPreferRNN = false;
    private String fanucRobotHost = System.getProperty("fanucRobotHost", "192.168.1.34");// "129.6.78.111"; // FIXME hard-coded default

    private void startFanucCrclServer() {
        try {
            FanucCRCLMain newFanucCrclMain = new FanucCRCLMain();
            if (null == fanucCRCLServerJInternalFrame) {
                fanucCRCLServerJInternalFrame = new FanucCRCLServerJInternalFrame();
                addInternalFrame(fanucCRCLServerJInternalFrame);
            }
            newFanucCrclMain.setDisplayInterface(fanucCRCLServerJInternalFrame);
            newFanucCrclMain.startDisplayInterface();
            newFanucCrclMain.start(fanucPreferRNN, fanucNeighborhoodName, fanucRobotHost, fanucCrclPort);
            this.fanucCRCLMain = newFanucCrclMain;
        } catch (CRCLException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Nullable private String titleErrorString = null;

    /**
     * Get the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @return title error string
     */
    @Nullable public String getTitleErrorString() {
        return titleErrorString;
    }

    @Nullable private volatile CommandStatusType titleErrorStringCommandStatus = null;

    @Nullable private volatile String lastNewTitleErrorString = null;

    private volatile StackTraceElement setTitleErrorStringTrace @Nullable []  = null;

    /**
     * Get the stack trace the last time the title error was set.
     *
     * @return stack trace or null if no error has been set.
     */
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
                    System.err.println(newTitleErrorString);
                    takeSnapshots("setTitleError_" + newTitleErrorString + "_");
                    pause();
                }
            }
            lastNewTitleErrorString = newTitleErrorString;
        }
    }

    private void startMotomanCrclServer() {
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
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
        jDesktopPane1.add(internalFrame, JLayeredPane.DEFAULT_LAYER);
        internalFrame.getDesktopPane().getDesktopManager().maximizeFrame(internalFrame);
        setupWindowsMenu();
    }

    final static private AtomicInteger runProgramThreadCount = new AtomicInteger();

    private final int myThreadId = runProgramThreadCount.incrementAndGet();

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

    private void setThreadName() {
        Thread.currentThread().setName(getThreadName());
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

    private ExecutorService runProgramService = defaultRunProgramService;
    private ExecutorService connectService = defaultRunProgramService;
    @Nullable private XFuture<Void> connectDatabaseFuture = null;

    /**
     * Get the ExecutorService used for running CRCL programs.
     *
     * @return run program service
     */
    public ExecutorService getRunProgramService() {
        return runProgramService;
    }

    private XFuture<Boolean> startConnectDatabase() {
        assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
        XFuture<Boolean> f = waitDbConnected();
        if (closing) {
            throw new IllegalStateException("Attempt to start connect database when already closing.");
        }
        System.out.println("Starting connect to database ...");
        DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
        DbSetup setup = dbSetupPublisher.getDbSetup();
        if (setup.getDbType() == null || setup.getDbType() == DbType.NONE) {
            throw new IllegalStateException("Can not connect to database with setup.getDbType() = " + setup.getDbType());
        }
        connectDatabaseFuture = XFuture.runAsync("connectDatabase", this::connectDatabase, connectService);
        //connectService.submit(this::connectDatabase);
        return f;
    }

    private void connectDatabase() {
        List<Future<?>> futures = null;
        try {
            assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            String startScript = dbSetupJInternalFrame.getStartScript();
            if (null != startScript && startScript.length() > 0) {
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.err.flush();
                System.out.println("Excecuting Database startScript=\r\n\"" + startScript + "\"");
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.err.flush();
                ProcessBuilder pb = new ProcessBuilder(startScript.split("[ ]+"));
                pb.inheritIO().start().waitFor();
                System.out.println("");
                System.err.println("");
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
            futures = dbSetupPublisher.notifyAllDbSetupListeners();
            for (Future<?> f : futures) {
                if (!jCheckBoxMenuItemConnectDatabase.isSelected()) {
                    return;
                }
                if (!f.isDone() && !f.isCancelled()) {
                    f.get();
                }
            }
            jCheckBoxMenuItemConnectDatabase.setEnabled(true);
            System.out.println("Finished connect to database.");
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            if (null != futures) {
                for (Future<?> f : futures) {
                    f.cancel(true);
                }
            }
        }
    }

    private void connectVision() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect vision when already closing.");
        }
        if (null != visionToDbJInternalFrame) {
            Utils.runOnDispatchThread(visionToDbJInternalFrame::connectVision);
        }
    }

    private void disconnectVision() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect vision when already closing.");
        }
        if (null != visionToDbJInternalFrame) {
            Utils.runOnDispatchThread(visionToDbJInternalFrame::disconnectVision);
        }
    }

    /**
     * Creates new AprsJFrame using a default properties file.
     */
    @SuppressWarnings("initialization")
    public AprsJFrame() {
        try {
            initPropertiesFileInfo();
            initComponents();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Initialize the frame with the previously saved settings if available.
     */
    final public void defaultInit() {
        initLoggerWindow();
        try {
//            initPropertiesFile();
            if (propertiesFile.exists()) {
                loadProperties();
            } else {
                saveProperties();
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        commonInit();
    }

    private void clearStartCheckBoxes() {
        jCheckBoxMenuItemKitInspectionStartup.setSelected(false);
        jCheckBoxMenuItemStartupPDDLPlanner.setSelected(false);
        jCheckBoxMenuItemStartupPDDLExecutor.setSelected(false);
        jCheckBoxMenuItemStartupObjectSP.setSelected(false);
        jCheckBoxMenuItemStartupObject2DView.setSelected(false);
        jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(false);
        jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(false);
        jCheckBoxMenuItemExploreGraphDbStartup.setSelected(false);
        jCheckBoxMenuItemStartupFanucCRCLServer.setSelected(false);
        jCheckBoxMenuItemStartupMotomanCRCLServer.setSelected(false);
        jCheckBoxMenuItemShowDatabaseSetup.setSelected(false);
        jCheckBoxMenuItemStartupCRCLWebApp.setSelected(false);
        jCheckBoxMenuItemConnectToDatabaseOnStartup.setSelected(false);
        jCheckBoxMenuItemConnectToVisionOnStartup.setSelected(false);
    }

    /**
     * Initialize the frame ignoring any previously saved settings.
     */
    final public void emptyInit() {
        skipCreateDbSetupFrame = true;
        clearStartCheckBoxes();
        initLoggerWindow();
        commonInit();
    }

    /**
     * Creates new AprsJFrame using a specified properties file.
     *
     * @param propertiesFile properties file to be read for initial settings
     */
    @SuppressWarnings("initialization")
    public AprsJFrame(File propertiesFile) {
        try {
            initComponents();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        commonInit();
    }

    private boolean skipCreateDbSetupFrame = false;

    private void commonInit() {
        startWindowsFromMenuCheckboxes();

        try {
            URL aprsPngUrl = AprsJFrame.class.getResource("aprs.png");
            if (null != aprsPngUrl) {
                setIconImage(ImageIO.read(aprsPngUrl));
            } else {
                Logger.getLogger(AprsJFrame.class.getName()).log(Level.WARNING, "getResource(\"aprs.png\") returned null");
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != logDisplayJInternalFrame) {
            activateInternalFrame(logDisplayJInternalFrame);
        }
        setupWindowsMenu();
        updateTitle("", "");
    }

    private void startWindowsFromMenuCheckboxes() {
        try {
            if (jCheckBoxMenuItemKitInspectionStartup.isSelected()) {
                startKitInspection();
            }
            if (jCheckBoxMenuItemStartupPDDLPlanner.isSelected()) {
                startPddlPlanner();
            }
            if (jCheckBoxMenuItemStartupPDDLExecutor.isSelected()) {
                startExecutorJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startCrclClientJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
                startSimServerJInternalFrame();
            }
            if (jCheckBoxMenuItemExploreGraphDbStartup.isSelected()) {
                startExploreGraphDb();
            }
            if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
                startFanucCrclServer();
            }
            if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
                startMotomanCrclServer();
            }
            if (!skipCreateDbSetupFrame || jCheckBoxMenuItemShowDatabaseSetup.isSelected()) {
                createDbSetupFrame();
            }
            if (jCheckBoxMenuItemShowDatabaseSetup.isSelected()) {
                showDatabaseSetupWindow();
            } else {
                jCheckBoxMenuItemConnectToDatabaseOnStartup.setSelected(false);
            }
            updateSubPropertiesFiles();
//            DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
//            if (null != pub) {
//                pub.setDbSetup(dbSetup);
//                pub.addDbSetupListener(toVisListener);
//            }
            if (this.jCheckBoxMenuItemStartupCRCLWebApp.isSelected()) {
                startCrclWebApp();
            }
            setupWindowsMenu();
            if (jCheckBoxMenuItemConnectToDatabaseOnStartup.isSelected()) {
                startConnectDatabase();
            }
            if (jCheckBoxMenuItemConnectToVisionOnStartup.isSelected()) {
                connectVision();
            }
            System.out.println("Constructor for AprsJframe complete.");
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initLoggerWindow() {
        try {
            if (null == logDisplayJInternalFrame) {
                logDisplayJInternalFrame = new LogDisplayJInternalFrame();
                logDisplayJInternalFrame.pack();
            }
            LogDisplayJInternalFrame logFrame = this.logDisplayJInternalFrame;
            if (null != logFrame) {
                logFrame.setVisible(true);
                jDesktopPane1.add(logFrame, JLayeredPane.DEFAULT_LAYER);
                System.setOut(new MyPrintStream(System.out, logFrame));
                System.setErr(new MyPrintStream(System.err, logFrame));
                activateInternalFrame(logFrame);
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Close all internal windows.
     */
    public void closeAllWindows() {
        try {
            closePddlPlanner();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        try {
            closeActionsToCrclJInternalFrame();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        try {
            stopCrclWebApp();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        if (null != disconnectDatabaseFuture) {
            disconnectDatabaseFuture.cancel(true);
            disconnectDatabaseFuture = null;
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
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setVisible(false);
            object2DViewJInternalFrame.dispose();
        }

        abortCrclProgram();
        disconnectRobot();
        disconnectVision();
        disconnectDatabase();
        DbSetupJInternalFrame dbSetupFrame = this.dbSetupJInternalFrame;
        if (null != dbSetupFrame) {
            dbSetupFrame.getDbSetupPublisher().removeAllDbSetupListeners();
            dbSetupFrame.setVisible(false);
            dbSetupFrame.dispose();
        }
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        for (JInternalFrame f : frames) {
            jDesktopPane1.getDesktopManager().closeFrame(f);
            jDesktopPane1.remove(f);
        }
        if (null != this.exploreGraphDbJInternalFrame) {
            this.exploreGraphDbJInternalFrame.setVisible(false);
            this.exploreGraphDbJInternalFrame.dispose();
        }
        if (null != this.logDisplayJInternalFrame) {
            this.logDisplayJInternalFrame.setVisible(false);
            this.logDisplayJInternalFrame.dispose();
        }
    }

    private void activateInternalFrame(JInternalFrame internalFrame) {
        try {
            internalFrame.setVisible(true);
            if (null != internalFrame.getDesktopPane()
                    && null != internalFrame.getDesktopPane().getDesktopManager()) {
                internalFrame.getDesktopPane().getDesktopManager().deiconifyFrame(internalFrame);
                internalFrame.getDesktopPane().getDesktopManager().activateFrame(internalFrame);
                internalFrame.getDesktopPane().getDesktopManager().maximizeFrame(internalFrame);
            }
            internalFrame.moveToFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDatabaseSetupWindow() {
        createDbSetupFrame();
        DbSetupJInternalFrame dbSetupFrame = this.dbSetupJInternalFrame;
        if (null != dbSetupFrame) {
            dbSetupFrame.setPropertiesFile(propertiesFile);
            dbSetupFrame.setVisible(true);
            if (checkInternalFrame(dbSetupFrame)) {
                jDesktopPane1.getDesktopManager().deiconifyFrame(dbSetupFrame);
                jDesktopPane1.getDesktopManager().activateFrame(dbSetupFrame);
                jDesktopPane1.getDesktopManager().maximizeFrame(dbSetupFrame);
            }
        }

        setupWindowsMenu();
    }

    private boolean checkInternalFrame(JInternalFrame frm) {
        try {
            if (frm == null) {
                return false;
            }
            for (JInternalFrame f : jDesktopPane1.getAllFrames()) {
                if (f == frm) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("checkInteralFrame(" + frm + ") failed.");
        return false;
    }

    private void hideDatabaseSetupWindow() {
        if (null != dbSetupJInternalFrame) {
            dbSetupJInternalFrame.setVisible(false);
            if (checkInternalFrame(dbSetupJInternalFrame)) {
                jDesktopPane1.getDesktopManager().iconifyFrame(dbSetupJInternalFrame);
                jDesktopPane1.getDesktopManager().deactivateFrame(dbSetupJInternalFrame);
            }
        }
    }

    private void activateFrame(final JInternalFrame frameToShow) {
        frameToShow.setVisible(true);
        if (checkInternalFrame(frameToShow)) {
            jDesktopPane1.getDesktopManager().deiconifyFrame(frameToShow);
            jDesktopPane1.getDesktopManager().activateFrame(frameToShow);
            frameToShow.moveToFront();
            activeWin = stringToWin(frameToShow.getTitle());
        } else {
            setupWindowsMenu();
        }
    }

    private void setupWindowsMenu() {
//        jMenuWindow.removeAll();

        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            return;
        }
        if (jMenuWindow.isSelected()) {
            return;
        }
        int count = 1;
        ArrayList<JInternalFrame> framesList = new ArrayList<>();
        framesList.addAll(Arrays.asList(jDesktopPane1.getAllFrames()));
        Collections.sort(framesList, Comparator.comparing(JInternalFrame::getTitle));
        List<JMenuItem> menuItems = new ArrayList<>();
        int framesListSize = framesList.size();
        for (JInternalFrame f : framesList) {
            JMenuItem menuItem = new JMenuItem(count + " " + f.getTitle());
            final JInternalFrame frameToShow = f;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activateFrame(frameToShow);
                }
            });
//            jMenuWindow.add(menuItem);
            count++;
            menuItems.add(menuItem);
        }
        jMenuWindow.removeAll();
        for (JMenuItem menuItem : menuItems) {
            jMenuWindow.add(menuItem);
        }
        assert (framesListSize == menuItems.size()) :
                ("menuItems = " + menuItems + " does not match framesList = " + framesList);

        int menuItemCount = jMenuWindow.getItemCount();
        assert (framesListSize == menuItemCount) :
                ("framesListSize = " + framesListSize + " does not match menuItemCount = " + menuItemCount
                + "with framesList=" + framesList + ", menuItems=" + menuItems);

    }

    private ActiveWinEnum activeWin = ActiveWinEnum.OTHER;

    /**
     * Enumeration class for setting/getting which  window (InternalJFrame)  is/should be displayed on top.
     */
    public static enum ActiveWinEnum {
        CRCL_CLIENT_WINDOW,
        PDDL_EXECUTOR_WINDOW,
        PDDL_PLANNER_WINDOW,
        SIMVIEW_WINDOW,
        DATABASE_SETUP_WINDOW,
        VISION_TO_DB_WINDOW,
        ERRLOG_WINDOW,
        KIT_INSPECTION_WINDOW,
        OTHER
    };

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
     * 
     * @throws IllegalStateException Object 2D view was not opened.
     * 
     */
    public List<PhysicalItem> getSimItemsData() throws IllegalStateException {
        if (null == object2DViewJInternalFrame) {
            throw new IllegalStateException("null == object2DViewJInternalFrame)");
        }
        return object2DViewJInternalFrame.getItems();
    }

    /**
     * Set a list of items with names and poses to be used by the simulation.
     * 
     *  This has no effect if the Object 2D view has not been opened or it is
     * not in simulation mode.
     * 
     * @param items list of items to use
     */
    public void setSimItemsData(List<PhysicalItem> items) {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setItems(items);
        }
    }
    
    /**
     * Set the position the robot tool should be moved to ensure
     * the robot is no longer obstructing the vision systems view of 
     * the parts and trays.
     * 
     * @param x x coordinate 
     * @param y y coordinate 
     * @param z z coordinate 
     */
    public void setLookForXYZ(double x, double y, double z) {
        if(null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setLookForXYZ(x,y,z);
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
            boolean alreadySelected = jCheckBoxMenuItemStartupObject2DView.isSelected();
            if (!alreadySelected) {
                jCheckBoxMenuItemStartupObject2DView.setSelected(true);
            }
            object2DViewJInternalFrame = new Object2DViewJInternalFrame();
            updateSubPropertiesFiles();
            object2DViewJInternalFrame.setAprsJFrame(this);
            if (null != externalSlotOffsetProvider) {
                object2DViewJInternalFrame.setSlotOffsetProvider(externalSlotOffsetProvider);
            }
            object2DViewJInternalFrame.loadProperties();
            object2DViewJInternalFrame.pack();
            object2DViewJInternalFrame.setVisible(true);
            jDesktopPane1.add(object2DViewJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            object2DViewJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(object2DViewJInternalFrame);
            if (!alreadySelected) {
                setupWindowsMenu();
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
    @Nullable public String getCrclClientErrorString() {
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
            setupWindowsMenu();
        }
        for (Runnable r : titleUpdateRunnables) {
            r.run();
        }
    }

    private String pddlActionString() {
        if (null == pddlExecutorJInternalFrame1) {
            return "";
        }
        List<PddlAction> actionList = pddlExecutorJInternalFrame1.getActionsList();
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
    @Nullable public CRCLStatusType getCurrentStatus() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.getCurrentStatus().orElse(null);
        }
        return null;
    }

    /**
     * Update the title based on the current state.
     */
    public void updateTitle() {
        Utils.runOnDispatchThread(this::updateTitleInternal);
    }

    @Nullable private CommandStatusType getCommandStatus() {
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.getCurrentStatus()
                    .map(x -> x.getCommandStatus())
                    .map(CRCLPosemath::copy)
                    .orElse(null);
        }
        return null;
    }

    private void updateTitleInternal() {
        if (null != crclClientJInternalFrame) {
            CommandStatusType cs = crclClientJInternalFrame.getCurrentStatus()
                    .map(x -> x.getCommandStatus())
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
            boolean alreadySelected = jCheckBoxMenuItemStartupRobotCrclGUI.isSelected();
            if (!alreadySelected) {
                jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(true);
            }
            crclClientJInternalFrame = new PendantClientJInternalFrame();
            crclClientJInternalFrame.setRunProgramService(runProgramService);
            crclClientJInternalFrame.setTempLogDir(Utils.getlogFileDir());
            updateSubPropertiesFiles();
            crclClientJInternalFrame.loadProperties();
            crclClientJInternalFrame.pack();
            crclClientJInternalFrame.setVisible(true);
            jDesktopPane1.add(crclClientJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            crclClientJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(crclClientJInternalFrame);
            crclClientJInternalFrame.addUpdateTitleListener(new UpdateTitleListener() {
                @Override
                public void titleChanged(CommandStatusType ccst, Container container, String stateString, String stateDescription) {
                    updateTitle(stateString, stateDescription);
                }

            });
            if (null != unaddedPoseListeners) {
                List<PendantClientJPanel.CurrentPoseListener> tempList = new ArrayList<>();
                tempList.addAll(unaddedPoseListeners);
                unaddedPoseListeners.clear();
                for (PendantClientJPanel.CurrentPoseListener l : tempList) {
                    crclClientJInternalFrame.addCurrentPoseListener(l);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Create and display the simulation server window and start
     * the simulation server thread.
     */
    public void startSimServerJInternalFrame() {
        try {
            if (null == simServerJInternalFrame) {
                boolean alreadySelected = jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected();
                if (!alreadySelected) {
                    jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(true);
                }
                simServerJInternalFrame = new SimServerJInternalFrame(false);
                updateSubPropertiesFiles();
                simServerJInternalFrame.loadProperties();
                simServerJInternalFrame.pack();
                simServerJInternalFrame.setVisible(true);
                simServerJInternalFrame.restartServer();
                jDesktopPane1.add(simServerJInternalFrame, JLayeredPane.DEFAULT_LAYER);
                simServerJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(simServerJInternalFrame);

            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final Callable<DbSetupPublisher> dbSetupPublisherSupplier = new Callable<DbSetupPublisher>() {
        @Override
        public DbSetupPublisher call() throws Exception {
            createDbSetupFrame();
            assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
            return dbSetupJInternalFrame.getDbSetupPublisher();
        }
    };

    private volatile boolean dbConnected = false;

    private void updateDbConnectedCheckBox(DbSetup setup) {
        dbConnected = setup.isConnected();
        jCheckBoxMenuItemConnectDatabase.setSelected(setup.isConnected());
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
            dbSetupJInternalFrame.setAprsJframe(this);
            dbSetupJInternalFrame.pack();
            dbSetupJInternalFrame.loadRecentSettings();
            jDesktopPane1.add(dbSetupJInternalFrame, JLayeredPane.DEFAULT_LAYER);
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

    /**
     * Set the menu checkbox item to reflect the val of the whether the vision
     * system is connected. This will not cause the system to connect/disconnect
     * only to show the state the caller already knows.
     *
     * @param val of vision systems connected status to show
     */
    public void setShowVisionConnected(boolean val) {
        jCheckBoxMenuItemConnectVision.setSelected(val);
    }

    private void startVisionToDbJinternalFrame() {
        try {
            visionToDbJInternalFrame = new VisionToDbJInternalFrame();
            visionToDbJInternalFrame.setAprsJFrame(this);
            updateSubPropertiesFiles();
            visionToDbJInternalFrame.loadProperties();
            visionToDbJInternalFrame.pack();
            visionToDbJInternalFrame.setVisible(true);
            visionToDbJInternalFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
            jDesktopPane1.add(visionToDbJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            visionToDbJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(visionToDbJInternalFrame);
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Start the PDDL Executor (aka Actions to CRCL) and create
     * and display the window for displaying its output.
     */
    public void startExecutorJInternalFrame() {
        try {
            Utils.runAndWaitOnDispatchThread("startActionsToCrclJInternalFrame",
                    () -> {
                        try {
                            boolean alreadySelected = jCheckBoxMenuItemStartupPDDLExecutor.isSelected();
                            if (!alreadySelected) {
                                jCheckBoxMenuItemStartupPDDLExecutor.setSelected(true);
                            }
                            if (null == pddlExecutorJInternalFrame1) {
                                pddlExecutorJInternalFrame1 = new PddlExecutorJInternalFrame();
                                this.pddlExecutorJInternalFrame1.pack();
                            }
                            PddlExecutorJInternalFrame execFrame = this.pddlExecutorJInternalFrame1;
                            assert (null != execFrame) : "";
                            execFrame.setAprsJFrame(this);
                            execFrame.setVisible(true);
                            jDesktopPane1.add(execFrame, JLayeredPane.DEFAULT_LAYER);
                            execFrame.getDesktopPane().getDesktopManager().maximizeFrame(execFrame);
                            updateSubPropertiesFiles();
                            XFuture.runAsync("startActionsToCrclJInternalFrame.loadProperties", () -> {
                                try {
                                    execFrame.loadProperties();
                                } catch (IOException ex) {
                                    Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }, runProgramService).join();
                            execFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
                            if (null != pddlPlannerJInternalFrame) {
                                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(execFrame);
                            }
                            if (!alreadySelected) {
                                setupWindowsMenu();
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(AprsJFrame.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    });
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startPddlPlanner() {
        try {
            //        jDesktopPane1.setDesktopManager(d);
            if (pddlPlannerJInternalFrame == null) {
                pddlPlannerJInternalFrame = new PddlPlannerJInternalFrame();
                pddlPlannerJInternalFrame.pack();
            }
            updateSubPropertiesFiles();
            pddlPlannerJInternalFrame.setVisible(true);
            jDesktopPane1.add(pddlPlannerJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            pddlPlannerJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(pddlPlannerJInternalFrame);
//            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "pddlPlanner.txt"));
            pddlPlannerJInternalFrame.loadProperties();
            pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startKitInspection() {
        try {
            //        jDesktopPane1.setDesktopManager(d);
            if (kitInspectionJInternalFrame == null) {
                kitInspectionJInternalFrame = new KitInspectionJInternalFrame();
                kitInspectionJInternalFrame.pack();
            }
            updateSubPropertiesFiles();
            kitInspectionJInternalFrame.setVisible(true);
            jDesktopPane1.add(kitInspectionJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            kitInspectionJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(kitInspectionJInternalFrame);
//            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "pddlPlanner.txt"));
            kitInspectionJInternalFrame.loadProperties();
            //kitInspectionJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDesktopPane1 = new javax.swing.JDesktopPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemLoadProperties = new javax.swing.JMenuItem();
        jMenuItemSaveProperties = new javax.swing.JMenuItem();
        jMenuItemSavePropsAs = new javax.swing.JMenuItem();
        jMenuItemLoadPropertiesFile = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemReverse = new javax.swing.JCheckBoxMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItemStartupPDDLPlanner = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupPDDLExecutor = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObjectSP = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObject2DView = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupRobotCrclGUI = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemShowDatabaseSetup = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemStartupRobtCRCLSimServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupFanucCRCLServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupMotomanCRCLServer = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemConnectToDatabaseOnStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectToVisionOnStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemExploreGraphDbStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupCRCLWebApp = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemKitInspectionStartup = new javax.swing.JCheckBoxMenuItem();
        jMenuWindow = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jCheckBoxMenuItemConnectDatabase = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectVision = new javax.swing.JCheckBoxMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jCheckBoxMenuItemEnableDebugDumpstacks = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetPoseMaxLimits = new javax.swing.JMenuItem();
        jMenuItemSetPoseMinLimits = new javax.swing.JMenuItem();
        jCheckBoxMenuItemSnapshotImageSize = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemReloadSimFilesOnReverse = new javax.swing.JCheckBoxMenuItem();
        jMenuExecute = new javax.swing.JMenu();
        jMenuItemStartActionList = new javax.swing.JMenuItem();
        jMenuItemImmediateAbort = new javax.swing.JMenuItem();
        jMenuItemContinueActionList = new javax.swing.JMenuItem();
        jMenuItemReset = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinuousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jMenuItemDebugAction = new javax.swing.JMenuItem();
        jCheckBoxMenuItemForceFakeTake = new javax.swing.JCheckBoxMenuItem();
        jMenuItemCreateActionListFromVision = new javax.swing.JMenuItem();
        jMenuItemLookFor = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS");

        jMenu1.setText("File");

        jMenuItemLoadProperties.setText("Reload Property Settings");
        jMenuItemLoadProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadPropertiesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadProperties);

        jMenuItemSaveProperties.setText("Save Properties");
        jMenuItemSaveProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePropertiesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveProperties);

        jMenuItemSavePropsAs.setText("Save Properties As ...");
        jMenuItemSavePropsAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePropsAsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSavePropsAs);

        jMenuItemLoadPropertiesFile.setText("Load Properties File ...");
        jMenuItemLoadPropertiesFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadPropertiesFileActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadPropertiesFile);
        jMenu1.add(jSeparator1);

        jCheckBoxMenuItemReverse.setText("Reverse");
        jCheckBoxMenuItemReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemReverseActionPerformed(evt);
            }
        });
        jMenu1.add(jCheckBoxMenuItemReverse);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemExit);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Startup");

        jCheckBoxMenuItemStartupPDDLPlanner.setSelected(true);
        jCheckBoxMenuItemStartupPDDLPlanner.setText("PDDL Planner");
        jCheckBoxMenuItemStartupPDDLPlanner.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupPDDLPlannerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupPDDLPlanner);

        jCheckBoxMenuItemStartupPDDLExecutor.setSelected(true);
        jCheckBoxMenuItemStartupPDDLExecutor.setText("PDDL Executor");
        jCheckBoxMenuItemStartupPDDLExecutor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupPDDLExecutorActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupPDDLExecutor);

        jCheckBoxMenuItemStartupObjectSP.setSelected(true);
        jCheckBoxMenuItemStartupObjectSP.setText("Object SP");
        jCheckBoxMenuItemStartupObjectSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObjectSPActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObjectSP);

        jCheckBoxMenuItemStartupObject2DView.setSelected(true);
        jCheckBoxMenuItemStartupObject2DView.setText("Object 2D View/Simulate");
        jCheckBoxMenuItemStartupObject2DView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObject2DViewActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObject2DView);

        jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(true);
        jCheckBoxMenuItemStartupRobotCrclGUI.setText("Robot CRCL Client Gui");
        jCheckBoxMenuItemStartupRobotCrclGUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobotCrclGUI);

        jCheckBoxMenuItemShowDatabaseSetup.setSelected(true);
        jCheckBoxMenuItemShowDatabaseSetup.setText("Database Setup");
        jCheckBoxMenuItemShowDatabaseSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowDatabaseSetupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemShowDatabaseSetup);
        jMenu3.add(jSeparator2);

        jCheckBoxMenuItemStartupRobtCRCLSimServer.setText("Robot CRCL SimServer");
        jCheckBoxMenuItemStartupRobtCRCLSimServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobtCRCLSimServer);

        jCheckBoxMenuItemStartupFanucCRCLServer.setText("Fanuc CRCL Server");
        jCheckBoxMenuItemStartupFanucCRCLServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupFanucCRCLServer);

        jCheckBoxMenuItemStartupMotomanCRCLServer.setText("Motoman CRCL Server");
        jCheckBoxMenuItemStartupMotomanCRCLServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupMotomanCRCLServer);
        jMenu3.add(jSeparator3);

        jCheckBoxMenuItemConnectToDatabaseOnStartup.setText("Connect To Database On Startup");
        jCheckBoxMenuItemConnectToDatabaseOnStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemConnectToDatabaseOnStartup);

        jCheckBoxMenuItemConnectToVisionOnStartup.setText("Connect To Vision On Startup");
        jMenu3.add(jCheckBoxMenuItemConnectToVisionOnStartup);

        jCheckBoxMenuItemExploreGraphDbStartup.setText("Explore Graph Database");
        jCheckBoxMenuItemExploreGraphDbStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemExploreGraphDbStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemExploreGraphDbStartup);

        jCheckBoxMenuItemStartupCRCLWebApp.setText("CRCL Web App");
        jCheckBoxMenuItemStartupCRCLWebApp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupCRCLWebAppActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupCRCLWebApp);

        jCheckBoxMenuItemKitInspectionStartup.setSelected(true);
        jCheckBoxMenuItemKitInspectionStartup.setText("Kit Inspection");
        jCheckBoxMenuItemKitInspectionStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemKitInspectionStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemKitInspectionStartup);

        jMenuBar1.add(jMenu3);

        jMenuWindow.setText("Window");
        jMenuBar1.add(jMenuWindow);

        jMenu2.setText("Connections");

        jCheckBoxMenuItemConnectDatabase.setText("Database");
        jCheckBoxMenuItemConnectDatabase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectDatabaseActionPerformed(evt);
            }
        });
        jMenu2.add(jCheckBoxMenuItemConnectDatabase);

        jCheckBoxMenuItemConnectVision.setText("Vision");
        jCheckBoxMenuItemConnectVision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectVisionActionPerformed(evt);
            }
        });
        jMenu2.add(jCheckBoxMenuItemConnectVision);

        jMenuBar1.add(jMenu2);

        jMenu4.setText("Options");

        jCheckBoxMenuItemEnableDebugDumpstacks.setText("Enable Debug DumpStacks");
        jMenu4.add(jCheckBoxMenuItemEnableDebugDumpstacks);

        jMenuItemSetPoseMaxLimits.setText("Set Pose Max Limits ... (+10000,+10000,+10000)    ...");
        jMenuItemSetPoseMaxLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetPoseMaxLimitsActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItemSetPoseMaxLimits);

        jMenuItemSetPoseMinLimits.setText("Set Pose Min Limits ... (-10000,-10000,-10000)    ...");
        jMenuItemSetPoseMinLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetPoseMinLimitsActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItemSetPoseMinLimits);

        jCheckBoxMenuItemSnapshotImageSize.setText("Snapshot Image size (800 x 600 )");
        jCheckBoxMenuItemSnapshotImageSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSnapshotImageSizeActionPerformed(evt);
            }
        });
        jMenu4.add(jCheckBoxMenuItemSnapshotImageSize);

        jCheckBoxMenuItemReloadSimFilesOnReverse.setText("Reload Sim Files on Reverse");
        jMenu4.add(jCheckBoxMenuItemReloadSimFilesOnReverse);

        jMenuBar1.add(jMenu4);

        jMenuExecute.setText("Execute");

        jMenuItemStartActionList.setText("Start Action List");
        jMenuItemStartActionList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartActionListActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemStartActionList);

        jMenuItemImmediateAbort.setText("Immediate Abort");
        jMenuItemImmediateAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemImmediateAbortActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemImmediateAbort);

        jMenuItemContinueActionList.setText("Continue Action List");
        jMenuItemContinueActionList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemContinueActionListActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemContinueActionList);

        jMenuItemReset.setText("Reset");
        jMenuItemReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemResetActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemReset);

        jCheckBoxMenuItemContinuousDemo.setText("Continuous Demo");
        jCheckBoxMenuItemContinuousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinuousDemoActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemContinuousDemo);

        jCheckBoxMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAUSE, 0));
        jCheckBoxMenuItemPause.setText("Pause");
        jCheckBoxMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemPause);

        jMenuItemDebugAction.setText("Debug Action");
        jMenuItemDebugAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDebugActionActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemDebugAction);

        jCheckBoxMenuItemForceFakeTake.setText("Force Fake Take");
        jCheckBoxMenuItemForceFakeTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemForceFakeTakeActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemForceFakeTake);

        jMenuItemCreateActionListFromVision.setText("Create Action List From Vision");
        jMenuItemCreateActionListFromVision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCreateActionListFromVisionActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemCreateActionListFromVision);

        jMenuItemLookFor.setText("Look For");
        jMenuItemLookFor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLookForActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemLookFor);

        jMenuBar1.add(jMenuExecute);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1058, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxMenuItemStartupPDDLPlannerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed
        try {
            if (jCheckBoxMenuItemStartupPDDLPlanner.isSelected()) {
                startPddlPlanner();
            } else {
                closePddlPlanner();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed

    private void jCheckBoxMenuItemStartupPDDLExecutorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupPDDLExecutorActionPerformed
        try {
            if (jCheckBoxMenuItemStartupPDDLExecutor.isSelected()) {
                startExecutorJInternalFrame();
            } else {
                closeActionsToCrclJInternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLExecutorActionPerformed

    private void jCheckBoxMenuItemStartupObjectSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed

    private void jCheckBoxMenuItemStartupObject2DViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed

    private void jMenuItemLoadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesActionPerformed
        try {
            this.loadProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemLoadPropertiesActionPerformed

    private void jMenuItemSavePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropertiesActionPerformed
        try {
            this.saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSavePropertiesActionPerformed

    private void jMenuItemLoadPropertiesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesFileActionPerformed
        browseOpenPropertiesFile();
    }//GEN-LAST:event_jMenuItemLoadPropertiesFileActionPerformed

    /**
     * Query the user to select a properties file to open.
     */
    public void browseOpenPropertiesFile() {
        JFileChooser chooser = new JFileChooser(propertiesDirectory);
        FileFilter filter = new FileNameExtensionFilter("Text properties files.", "txt");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Open APRS System properties file.");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                closeAllWindows();
                setPropertiesFile(chooser.getSelectedFile());
                loadProperties();
//                initPropertiesFile();

            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            this.initLoggerWindow();
            this.commonInit();
        }

    }


    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        try {
            close();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        System.exit(0);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed
        try {
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startCrclClientJInternalFrame();
            } else {
//                closePddlPlanner();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed

    private void jCheckBoxMenuItemShowDatabaseSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowDatabaseSetupActionPerformed
        try {
            if (jCheckBoxMenuItemShowDatabaseSetup.isSelected()) {
                showDatabaseSetupWindow();
            } else {
                hideDatabaseSetupWindow();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemShowDatabaseSetupActionPerformed

    private void jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed
        if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
            startSimServerJInternalFrame();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed

    private void jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
            startFanucCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed

    private void jCheckBoxMenuItemExploreGraphDbStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemExploreGraphDbStartupActionPerformed
        if (this.jCheckBoxMenuItemExploreGraphDbStartup.isSelected()) {
            startExploreGraphDb();
        } else {
            closeExploreGraphDb();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemExploreGraphDbStartupActionPerformed

    @Nullable CRCLWebAppRunner crclWebAppRunner = null;

    private void stopCrclWebApp() {
        if (null != crclWebAppRunner) {
            crclWebAppRunner.stop();
            crclWebAppRunner = null;
        }
    }

    private void startCrclWebApp() {
        crclWebAppRunner = new CRCLWebAppRunner();
        crclWebAppRunner.setHttpPort(crclWebServerHttpPort);
        crclWebAppRunner.start();
    }

    private int crclWebServerHttpPort = 8081;


    private void jCheckBoxMenuItemStartupCRCLWebAppActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupCRCLWebAppActionPerformed
        try {
            stopCrclWebApp();
            if (jCheckBoxMenuItemStartupCRCLWebApp.isSelected()) {
                String portString = JOptionPane.showInputDialog("Http Port?", crclWebServerHttpPort);
                crclWebServerHttpPort = Integer.parseInt(portString);
                startCrclWebApp();
            }
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupCRCLWebAppActionPerformed

    private void jCheckBoxMenuItemConnectDatabaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectDatabaseActionPerformed
        if (this.jCheckBoxMenuItemConnectDatabase.isSelected()) {
            startConnectDatabase();
        } else {
            startDisconnectDatabase();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectDatabaseActionPerformed

    private void jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed
        try {
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed

    private void jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
            startMotomanCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed

    private void jMenuItemSavePropsAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropsAsActionPerformed
        browseSavePropertiesFileAs();
    }//GEN-LAST:event_jMenuItemSavePropsAsActionPerformed

    /**
     * Query the user to select a properties file to save.
     */
    public void browseSavePropertiesFileAs() {
        JFileChooser chooser = new JFileChooser(propertiesDirectory);
        FileFilter filter = new FileNameExtensionFilter("Text properties files.", "txt");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Choose new APRS System properties file to create (save as).");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setPropertiesFile(chooser.getSelectedFile());
                this.saveProperties();
            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jMenuItemImmediateAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortActionPerformed
        this.immediateAbort();
    }//GEN-LAST:event_jMenuItemImmediateAbortActionPerformed

    private void jMenuItemStartActionListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartActionListActionPerformed
        setTitleErrorString(null);
        jCheckBoxMenuItemPause.setSelected(false);
        notifyPauseFutures();
        this.startActions("user");
    }//GEN-LAST:event_jMenuItemStartActionListActionPerformed

    private void jCheckBoxMenuItemConnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectVisionActionPerformed
        if (jCheckBoxMenuItemConnectVision.isSelected()) {
            connectVision();
        } else {
            disconnectVision();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectVisionActionPerformed

    private void jCheckBoxMenuItemKitInspectionStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemKitInspectionStartupActionPerformed
        this.showKitInspection();
    }//GEN-LAST:event_jCheckBoxMenuItemKitInspectionStartupActionPerformed

    private void jMenuItemResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetActionPerformed
        reset();
    }//GEN-LAST:event_jMenuItemResetActionPerformed

    private void jCheckBoxMenuItemReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemReverseActionPerformed
        boolean reverseFlag = jCheckBoxMenuItemReverse.isSelected();
        startSetReverseFlag(reverseFlag);
    }//GEN-LAST:event_jCheckBoxMenuItemReverseActionPerformed

    private void jCheckBoxMenuItemContinuousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinuousDemoActionPerformed
        boolean start = jCheckBoxMenuItemContinuousDemo.isSelected();
        boolean reverseFlag = jCheckBoxMenuItemReverse.isSelected();
        setTitleErrorString(null);
        immediateAbort();
        if (start) {
            if (!jCheckBoxMenuItemContinuousDemo.isSelected()) {
                jCheckBoxMenuItemContinuousDemo.setSelected(true);
            }
            continuousDemoFuture = startContinousDemo("user", reverseFlag);
        } else {
            immediateAbort();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemContinuousDemoActionPerformed

    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        System.out.println("jCheckBoxMenuItemPause.isSelected() = " + jCheckBoxMenuItemPause.isSelected());
        XFuture<Boolean> cdf = this.continuousDemoFuture;
        if (null != cdf) {
            System.out.println("continousDemoFuture.isDone() = " + cdf.isDone());
            System.out.println("continousDemoFuture.isCancelled() = " + cdf.isCancelled());
        }
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            clearErrors();
            resume();
        }
        cdf = this.continuousDemoFuture;
        if (null != cdf) {
            System.out.println("continousDemoFuture.isDone() = " + cdf.isDone());
            System.out.println("continousDemoFuture.isCancelled() = " + cdf.isCancelled());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseActionPerformed

    private void jMenuItemDebugActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDebugActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDebugActionActionPerformed

    private void jCheckBoxMenuItemForceFakeTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemForceFakeTakeActionPerformed
        if (pddlExecutorJInternalFrame1 != null) {
            boolean val = jCheckBoxMenuItemForceFakeTake.isSelected();
            if (pddlExecutorJInternalFrame1.getForceFakeTakeFlag() != val) {
                pddlExecutorJInternalFrame1.setForceFakeTakeFlag(val);
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemForceFakeTakeActionPerformed

    private void jMenuItemContinueActionListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueActionListActionPerformed
        continueActionList("user");
    }//GEN-LAST:event_jMenuItemContinueActionListActionPerformed

    private void jMenuItemCreateActionListFromVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCreateActionListFromVisionActionPerformed
        createActionListFromVision();
    }//GEN-LAST:event_jMenuItemCreateActionListFromVisionActionPerformed

    private void jMenuItemSetPoseMinLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetPoseMinLimitsActionPerformed
        String newMinLimitsString = JOptionPane.showInputDialog(this, "New Min Pose Limits",
                String.format("%+.3f,%.3f,%+.3f", minLimit.x, minLimit.y, minLimit.z));
        if (newMinLimitsString != null && newMinLimitsString.length() > 0) {
            PmCartesian cart = PmCartesian.valueOf(newMinLimitsString);
            setMinLimit(cart);
        }
    }//GEN-LAST:event_jMenuItemSetPoseMinLimitsActionPerformed

    private void jMenuItemSetPoseMaxLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetPoseMaxLimitsActionPerformed
        String newMaxLimitsString = JOptionPane.showInputDialog(this, "New Max Pose Limits",
                String.format("%+.3f,%.3f,%+.3f", maxLimit.x, maxLimit.y, maxLimit.z));
        if (newMaxLimitsString != null && newMaxLimitsString.length() > 0) {
            PmCartesian cart = PmCartesian.valueOf(newMaxLimitsString);
            setMaxLimit(cart);
        }
    }//GEN-LAST:event_jMenuItemSetPoseMaxLimitsActionPerformed

    private void jCheckBoxMenuItemSnapshotImageSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSnapshotImageSizeActionPerformed
        if (jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            String newSnapshotImageSize = JOptionPane.showInputDialog(this, "New Snapshot Image Size",
                    String.format("%d x %d ", snapShotWidth, snapShotHeight));
            if (newSnapshotImageSize != null && newSnapshotImageSize.length() > 0) {
                String sa[] = newSnapshotImageSize.split("[ \tx,]+");
                if (sa.length == 2) {
                    snapShotWidth = Integer.parseInt(sa[0]);
                    snapShotHeight = Integer.parseInt(sa[1]);
                    setImageSizeMenuText();
                }
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemSnapshotImageSizeActionPerformed

    private void jMenuItemLookForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLookForActionPerformed
        startLookForParts();
    }//GEN-LAST:event_jMenuItemLookForActionPerformed

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
        return XFuture.supplyAsync("startLookForParts", () -> {
            return lookForPartsInternal();
        }, runProgramService);
    }

    private boolean lookForPartsInternal() throws JAXBException {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        CRCLProgramType lfpProgram = pddlExecutorJInternalFrame1.createLookForPartsProgram();
        setProgram(lfpProgram);
        assert (null != crclClientJInternalFrame) : "null == pendantClientJInternalFrame ";
        boolean ret = crclClientJInternalFrame.runCurrentProgram();
        if (ret) {
            enableCheckedAlready = true;
        }
        return ret;
    }

    private void setImageSizeMenuText() {
        jCheckBoxMenuItemSnapshotImageSize.setText(String.format("Snapshot Image size (%d x %d )", snapShotWidth, snapShotHeight));
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

    private boolean isWithinLimits(PointType point) {
        return null != point && isWithinLimits(CRCLPosemath.toPmCartesian(point));
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
        String menuString = String.format("Set Pose Min Limits ... (%+.3f,%.3f,%+.3f)    ...", minLimit.x, minLimit.y, minLimit.z);
        Utils.runOnDispatchThread(() -> {
            if (!jMenuItemSetPoseMinLimits.getText().equals(menuString)) {
                jMenuItemSetPoseMinLimits.setText(menuString);
            }
        });
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
        String menuString = String.format("Set Pose Max Limits ... (%+.3f,%.3f,%+.3f)    ...", maxLimit.x, maxLimit.y, maxLimit.z);
        Utils.runOnDispatchThread(() -> {
            if (!jMenuItemSetPoseMaxLimits.getText().equals(menuString)) {
                jMenuItemSetPoseMaxLimits.setText(menuString);
            }
        });
    }

    @Nullable private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
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
    @Nullable public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        if (null != externalSlotOffsetProvider) {
            return externalSlotOffsetProvider.absSlotFromTrayAndOffset(tray, offsetItem);
        }
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        return visionToDbJInternalFrame.absSlotFromTrayAndOffset(tray, offsetItem);
    }

    /**
     * Get a list of items seen by the vision system or simulated in the
     * Object2D view and create a set of actions that will fill empty trays to
     * match. Load this list into the PDDL executor.
     *
     */
    public void createActionListFromVision() {
        try {
            List<PhysicalItem> requiredItems = getObjectViewItems();
            List<PhysicalItem> teachItems = requiredItems;
            updateScanImage(requiredItems);
            takeSimViewSnapshot("createActionListFromVision", requiredItems);
            createActionListFromVision(requiredItems, teachItems);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            setTitleErrorString("createActionListFromVision: " + ex.getMessage());
        }
    }

    private void updateScanImage(List<PhysicalItem> requiredItems) {
        Utils.runOnDispatchThread(() -> {
            updateScanImageInternal(requiredItems);
        });
    }

    public Image getLiveImage() {
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        Object2DJPanel.ViewOptions opts = new Object2DJPanel.ViewOptions();
        opts.h = 170;
        opts.w = 170;
        opts.disableLabels = true;
        opts.enableAutoscale = false;
        opts.disableLimitsLine = true;
        opts.disableShowCurrent = false;
        return object2DViewJInternalFrame.createSnapshotImage(opts);
    }
    
    private void updateScanImageInternal(List<PhysicalItem> requiredItems) {
        assert (null != object2DViewJInternalFrame) : ("null == object2DViewJInternalFrame  ");
        Object2DJPanel.ViewOptions opts = new Object2DJPanel.ViewOptions();
        opts.h = 170;
        opts.w = 170;
        opts.disableLabels = true;
        opts.enableAutoscale = false;
        opts.disableLimitsLine = true;
        opts.disableShowCurrent = true;
        scanImage = object2DViewJInternalFrame.createSnapshotImage(opts, requiredItems);
    }

    private GoalLearner goalLearner;

    /**
     * Get the value of goalLearner
     *
     * @return the value of goalLearner
     */
    public GoalLearner getGoalLearner() {
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

    private boolean checkKitTrays(List<PhysicalItem> kitTrays) {
        if (kitTrays.isEmpty()) {
            if (JOptionPane.YES_OPTION
                    != JOptionPane.showConfirmDialog(this, "Create action list with no kit trays?")) {
                setTitleErrorString("createActionListFromVision: No kit trays");
                throw new IllegalStateException("No kit trays");
            }
        }
        return true;
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
    public void createActionListFromVision(List<PhysicalItem> requiredItems, List<PhysicalItem> teachItems) {

        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        try {
            if (goalLearner == null) {
                goalLearner = new GoalLearner();
            }
            goalLearner.setItemPredicate(this::isWithinLimits);
            goalLearner.setKitTrayListPredicate(this::checkKitTrays);
            goalLearner.setSlotOffsetProvider(visionToDbJInternalFrame);

            boolean allEmptyA[] = new boolean[1];
            List<PddlAction> actions = goalLearner.createActionListFromVision(requiredItems, teachItems, allEmptyA);
            boolean allEmpty = allEmptyA[0];
            if (allEmpty || actions == null || actions.isEmpty()) {
                System.out.println("requiredItems = " + requiredItems);
                System.out.println("teachItems = " + teachItems);
                if (JOptionPane.YES_OPTION
                        != JOptionPane.showConfirmDialog(this, "Load action list with all trays empty?")) {
                    setTitleErrorString("createActionListFromVision: All kit trays empty");
                    throw new IllegalStateException("All kit trays empty");
                }
            }
            File f = createTempFile("actionList", ".txt");
            try (PrintStream ps = new PrintStream(new FileOutputStream(f))) {
                for (PddlAction act : actions) {
                    ps.println(act.asPddlLine());
                }
            }
            if (null != pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1.setReverseFlag(false);
                pddlExecutorJInternalFrame1.loadActionsFile(f);
                pddlExecutorJInternalFrame1.setReverseFlag(jCheckBoxMenuItemReverse.isSelected());
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            setTitleErrorString("createActionListFromVision: " + ex.getMessage());
        }
    }

    /**
     * Get the list of items displayed in the Object 2D view, they may be
     * simulated or received from the vision system.
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
     * Set the menu checkbox setting to force take operations to be faked so
     * that the gripper will not close, useful for testing.
     *
     * @param val true if take operations should be faked
     */
    public void setForceFakeTakeFlag(boolean val) {
        if (val != jCheckBoxMenuItemForceFakeTake.isSelected()) {
            jCheckBoxMenuItemForceFakeTake.setSelected(val);
        }
        if (pddlExecutorJInternalFrame1 != null) {
            if (pddlExecutorJInternalFrame1.getForceFakeTakeFlag() != val) {
                pddlExecutorJInternalFrame1.setForceFakeTakeFlag(val);
            }
        }
    }

    @Nullable private volatile XFuture<Boolean> lastResumeFuture = null;

    @Nullable private volatile Thread resumingThread = null;
    private volatile StackTraceElement resumingTrace @Nullable []  = null;
    private volatile boolean resuming = false;

    /**
     * Continue operations that were previously paused.
     */
    public void resume() {
        resumingThread = Thread.currentThread();
        resumingTrace = resumingThread.getStackTrace();
        resuming = true;
        boolean badState = pausing;
        try {
            if (this.titleErrorString != null && this.titleErrorString.length() > 0) {
                throw new IllegalStateException("Can't resume when titleErrorString set to " + titleErrorString);
            }
            badState = badState || pausing;
            String crclClientErrString = getCrclClientErrorString();
            if (crclClientErrString != null && crclClientErrString.length() > 0) {
                throw new IllegalStateException("Can't resume when crclClientErrString set to " + crclClientErrString);
            }
            if (jCheckBoxMenuItemPause.isSelected()) {
                jCheckBoxMenuItemPause.setSelected(false);
            }
            badState = badState || pausing;
            clearErrors();
            badState = badState || pausing;
            if (null != crclClientJInternalFrame) {
                crclClientJInternalFrame.unpauseCrclProgram();
            }
            notifyPauseFutures();
            badState = badState || pausing;
            clearErrors();
            badState = badState || pausing;
            String methodName = "resume";
            takeSnapshots(methodName);
            badState = badState || pausing;
            if (null != crclClientJInternalFrame) {
                crclClientJInternalFrame.unpauseCrclProgram();
            }
            badState = badState || pausing;
            updateTitle("", "");
            badState = badState || pausing;
            if (null != pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1.showPaused(false);
            }
            if (isPaused()) {
                throw new IllegalStateException("Still paused after resume.");
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
    public boolean isVisionToDbConnected() {
        return null != visionToDbJInternalFrame && visionToDbJInternalFrame.isDbConnected();
    }

    /**
     * Used for logging/debugging. Save a file(s) in the temporary directory
     * with the comment and a timestamp with the current view of the parts and
     * robot.
     *
     * @param comment comment name to include in filename for later analysis
     */
    public void takeSnapshots(String comment) {
        try {
            takeSimViewSnapshot(createTempFile(comment, ".PNG"), (PmCartesian) null, (String) null);
            if (null != visionToDbJInternalFrame && visionToDbJInternalFrame.isDbConnected()) {
                startVisionToDbNewItemsImageSave(createTempFile(comment + "_new_database_items", ".PNG"));
            }
        } catch (IOException ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

    /**
     * Print a great deal of debugging info to the console.
     */
    public void debugAction() {

        System.out.println("");
        System.err.println("");
        int count = debugActionCount.incrementAndGet();
        System.out.println("Begin AprsJFrame.debugAction()" + count);
        String details = getDetailsString();
        System.out.println("details = " + details);
        System.out.println("lastContinueCrclProgramResult = " + lastContinueCrclProgramResult);
        System.out.println("lastStartActionsFuture = " + lastStartActionsFuture);
        if (null != lastStartActionsFuture) {
            lastStartActionsFuture.printStatus(System.out);
        }
        System.out.println("continousDemoFuture = " + continuousDemoFuture);
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
        System.out.println("");
        System.err.println("");
        System.out.println("Begin AprsJFrame.debugAction()" + count);

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

    @Nullable private volatile XFuture<Boolean> continuousDemoFuture = null;

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
    public XFuture<Boolean> startContinousDemo(String comment, boolean reverseFirst) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        int startDisconnectCount = disconnectRobotCount.get();
        continuousDemoFuture = startContinousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount, cdStart.incrementAndGet(), 1);
        return continuousDemoFuture;
    }

    private XFuture<Boolean> startContinousDemo(String comment, boolean reverseFirst, int startAbortCount, int startDisconnectCount, int cdStart, int cdCur) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        if (!pddlExecutorJInternalFrame1.readyForNewActionsList()) {
            System.err.println("starting continous demo with comment=\"" + comment + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart + ",cdCur=" + cdCur);
        }
        if (startAbortCount != pddlExecutorJInternalFrame1.getSafeAbortRequestCount()) {
            return XFuture.completedFuture(false);
        }
        setStartRunTime();
        String logLabel = "startContinousDemo(task=" + getTaskName() + ")." + comment + "." + startAbortCount + "." + startDisconnectCount + "." + cdStart + "." + cdCur;
        logToSuper(logLabel);
        takeSnapshots(logLabel);
        continuousDemoFuture
                = XFuture.supplyAsync("startContinousDemo(task=" + getTaskName() + ") comment=" + comment,
                        new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                        AprsJFrame.this.setReverseFlag(reverseFirst, true);
                        boolean r0 = pddlExecutorJInternalFrame1.readyForNewActionsList();
                        if (!r0) {
                            System.err.println("starting continous demo with comment=\"" + comment + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart + ",cdCur=" + cdCur);
                        }
                        boolean enabledOk = doCheckEnabled();
                        boolean r1 = pddlExecutorJInternalFrame1.readyForNewActionsList();
                        if (!r1) {
                            System.err.println("starting continous demo with comment=\"" + comment + "\" when executor not ready for new actions. : reverseFirst=" + reverseFirst + ", startAbortCount=" + startAbortCount + ", startDisconnectCount=" + startDisconnectCount + ",cdStart=" + cdStart + ",cdCur=" + cdCur);
                        }
                        return repeatDoActionWithReverse(enabledOk, comment, reverseFirst, startAbortCount, startDisconnectCount, cdStart, cdCur);
                    }
                },
                        runProgramService);
        return continuousDemoFuture;
    }

    private boolean repeatDoActionWithReverse(Boolean x, String comment, boolean reverseFirst, int startAbortCount, int startDisconnectCount, int cdStart, int cdCur) throws IllegalStateException {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int cdCurLocal = cdCur;
        while (x && !isAborting()
                && pddlExecutorJInternalFrame1.getSafeAbortRequestCount() == startAbortCount
                && startDisconnectCount == disconnectRobotCount.get()) {
            String logLabel2 = "startContinousDemo(task=" + getTaskName() + ")." + comment + "." + startAbortCount + "." + startDisconnectCount + "." + cdStart + "." + cdCurLocal;
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

    @Nullable private volatile transient Consumer<String> supervisorEventLogger = null;

    /**
     * Get the value of supervisorEventLogger
     *
     * @return the value of supervisorEventLogger
     */
    @Nullable public Consumer<String> getSupervisorEventLogger() {
        return supervisorEventLogger;
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
    public XFuture<Boolean> startPreCheckedContinousDemo(String comment, boolean reverseFirst) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        int startDisconnectCount = disconnectRobotCount.get();
        continuousDemoFuture = startPreCheckedContinousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount);
        return continuousDemoFuture;
    }

    private XFuture<Boolean> startPreCheckedContinousDemo(String comment, boolean reverseFirst, int startAbortCount, int startDisconnectCount) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        int safeAbortRequestCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        if (startAbortCount != safeAbortRequestCount || isAborting()) {
            continuousDemoFuture = XFuture.completedFutureWithName("startPreCheckedContinousDemo(" + reverseFirst + "," + startAbortCount + ").safeAbortRequestCount=" + safeAbortRequestCount, false);
            return continuousDemoFuture;
        }
        setStartRunTime();
        if (!enableCheckedAlready) {
            continuousDemoFuture = XFuture.completedFutureWithName("startPreCheckedContinousDemo(" + reverseFirst + "," + startAbortCount + ").!enableCheckedAlready", false);
            return continuousDemoFuture;
        }
        if (!pddlExecutorJInternalFrame1.readyForNewActionsList()) {
            System.err.println("Call to startPreCheckedContinousDemo when not ready");
        }
        continuousDemoFuture = XFuture.supplyAsync("startPreCheckedContinousDemo(task=" + getTaskName() + ")",
                () -> {
                    assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                    this.setReverseFlag(reverseFirst, true);
                    if (startAbortCount != pddlExecutorJInternalFrame1.getSafeAbortRequestCount() || isAborting()) {
                        return false;
                    }
                    return doActionsWithReverse(comment, true, reverseFirst, startAbortCount, startDisconnectCount);
                }, runProgramService)
                .thenComposeAsync("startPreCheckedContinousDemo(task=" + getTaskName() + ").recurse",
                        x2 -> x2 ? startPreCheckedContinousDemo(comment, reverseFirst, startAbortCount, startDisconnectCount) : XFuture.completedFutureWithName("startContinousDemo.completedFutureWithName", false),
                        runProgramService);
        return continuousDemoFuture;
    }

    /**
     * Get the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use.
     *
     * @return reverse flag
     */
    public boolean isReverseFlag() {
        return jCheckBoxMenuItemReverse.isSelected();
    }

    /**
     * Set the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use. Reload the simulated object positions.
     *
     * @param reverseFlag new value for reverse flag
     * @return  a future object that can be used to determine when
     *  setting the reverse flag and all related actions is complete.
     */
    public XFuture<Void> startSetReverseFlag(boolean reverseFlag) {
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
     * @return a future object that can be used to determine when
     *  setting the reverse flag and all related actions is complete.
     */
    public XFuture<Void> startSetReverseFlag(boolean reverseFlag, boolean reloadSimFiles) {
        return XFuture.runAsync("startSetReverseFlag(" + reverseFlag + "," + reloadSimFiles + ")",
                () -> {
                    setReverseFlag(reverseFlag, reloadSimFiles);
                },
                runProgramService);
    }

    private void setReverseFlag(boolean reverseFlag, boolean reloadSimFiles) {
        if (jCheckBoxMenuItemReverse.isSelected() != reverseFlag) {
            jCheckBoxMenuItemReverse.setSelected(reverseFlag);
        }
        if (null != object2DViewJInternalFrame) {
            try {
                object2DViewJInternalFrame.setReverseFlag(reverseFlag);
                if (reloadSimFiles && jCheckBoxMenuItemReloadSimFilesOnReverse.isSelected()) {
                    if (object2DViewJInternalFrame.isSimulated() || !object2DViewJInternalFrame.isConnected()) {
                        object2DViewJInternalFrame.reloadDataFile();
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != pddlExecutorJInternalFrame1) {
            try {
                pddlExecutorJInternalFrame1.setReverseFlag(reverseFlag);
                pddlExecutorJInternalFrame1.reloadActionsFile();
            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Get the state of whether the system is paused
     *
     * @return paused state
     */
    public boolean isPaused() {
        return jCheckBoxMenuItemPause.isSelected();
    }

    /**
     * Pause any actions currently being performed and set a state that will
     * cause future actions to wait until resume is called.
     */
    public void pause() {
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
    @Nullable private volatile Thread pauseThread = null;
    private volatile StackTraceElement pauseTrace @Nullable []  = null;

    private void pauseInternal() {
        pauseThread = Thread.currentThread();
        pauseTrace = pauseThread.getStackTrace();
        pausing = true;
        boolean badState = resuming;
        try {
            if (!jCheckBoxMenuItemPause.isSelected()) {
                jCheckBoxMenuItemPause.setSelected(true);
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
     * @return a future object that can be used to determine when
     *  setting the reset and all related actions is complete.
     */
    public XFuture<Void> reset() {
        return reset(true);
    }

    /**
     * Reset errors and optionally reload simulation files
     *
     * @param reloadSimFiles whether to reload simulation files
     * 
     * @return a future object that can be used to determine when
     *  setting the reset and all related actions is complete.
     */
    public XFuture<Void> reset(boolean reloadSimFiles) {
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

    /**
     * Test that the robot can be connected by running an empty program.
     *
     * The actions will be executed in another thread after this method returns.
     * The returned future can be used to monitor, cancel or extend the
     * underlying task. The boolean contained in the future will be true only if
     * all actions appear to succeed.
     *
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startCheckEnabled() {
        setStartRunTime();
        if (enableCheckedAlready) {
            return XFuture.completedFutureWithName("startCheckEnabled.enableCheckedAlready", true);
        }
        return XFuture.supplyAsync("startCheckEnabled", this::doCheckEnabled, runProgramService);
//        try {
//            System.out.println("startCheckEnabled called.");
//            if (!isConnected()) {
//                setConnected(true);
//            }
//            CRCLProgramType emptyProgram = createEmptyProgram();
//            setCommandID(emptyProgram.getInitCanon());
//            setCommandID(emptyProgram.getEndCanon());
//            emptyProgram.setName("checkEnabled." + checkEnabledCount.incrementAndGet());
//            return startCRCLProgram(emptyProgram)
//                    .thenApply("startCheckEnabled.finish." + robotName + "." + taskName,
//                            x -> {
//                                System.out.println("startCheckEnabled finishing with " + x);
//                                enableCheckedAlready = x;
//                                return x;
//                            });
//        } catch (JAXBException ex) {
//            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException(ex);
//        }
    }

    private boolean doCheckEnabled() {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
        setStartRunTime();
        if (enableCheckedAlready) {
            return true;
        }
        try {
//            System.out.println("startCheckEnabled called.");
            if (!isConnected()) {
                setConnected(true);
            }
            CRCLProgramType emptyProgram = createEmptyProgram();
            setCommandID(emptyProgram.getInitCanon());
            setCommandID(emptyProgram.getEndCanon());
            emptyProgram.setName("checkEnabled." + checkEnabledCount.incrementAndGet());
            setProgram(emptyProgram);
            boolean progRunRet = crclClientJInternalFrame.runCurrentProgram();

//            System.out.println("startCheckEnabled finishing with " + progRunRet);
            enableCheckedAlready = progRunRet;
            return progRunRet;
        } catch (JAXBException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private volatile int emptyProgramCount = 0;
    private volatile int consecutiveEmptyProgramCount = 0;

    private void setProgram(CRCLProgramType program) throws JAXBException {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
        if (program.getMiddleCommand().isEmpty()) {
            emptyProgramCount++;
            consecutiveEmptyProgramCount++;
            if (consecutiveEmptyProgramCount > 1) {
                System.out.println("emptyProgramCount=" + emptyProgramCount);
                System.out.println("consecutiveEmptyProgramCount=" + consecutiveEmptyProgramCount);
            }
        } else {
            consecutiveEmptyProgramCount = 0;
        }
        crclClientJInternalFrame.setProgram(program);
    }

    private final ConcurrentLinkedDeque<XFuture<Boolean>> dbConnectedWaiters = new ConcurrentLinkedDeque<>();

    private XFuture<Boolean> waitDbConnected() {
        XFuture<Boolean> f = new XFuture<>("waitDbConnected");
        dbConnectedWaiters.add(f);
        return f;
    }

    @Nullable private volatile XFuture<Boolean> lastStartActionsFuture = null;

    /**
     * Get the last future created from a startActions request. Only used for
     * debugging.
     *
     * @return future or null if no startActions request has been made.
     */
    @Nullable public XFuture<Boolean> getLastStartActionsFuture() {
        return lastStartActionsFuture;
    }

    /**
     * Get the last future created from a continueActions request. Only used for
     * debugging.
     *
     * @return future or null if no continueActions request has been made.
     */
    @Nullable public XFuture<Boolean> getContinueActionListFuture() {
        return lastContinueActionListFuture;
    }

    private final ConcurrentLinkedDeque<String> startActionsStartComments
            = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<String> startActionsFinishComments
            = new ConcurrentLinkedDeque<>();

    /**
     * Start the PDDL actions currently loaded in the executor from the
     * beginning.
     *
     * The actions will be executed in another thread after this method returns.
     * The returned future can be used to monitor, cancel or extend the
     * underlying task. The boolean contained in the future will be true only if
     * all actions appear to succeed.
     *
     * @param actions list of actions to execute
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActionsList(Iterable<PddlAction> actions) {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        this.pddlExecutorJInternalFrame1.loadActionsList(actions);
        return startActions("");
    }

    /**
     * Check to see if the executor is in a state where it could begin working
     * on a new list of actions.
     * @return is executor ready for new actions.
     */
    public boolean readyForNewActionsList() {
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        return pddlExecutorJInternalFrame1.readyForNewActionsList();
    }

    /**
     * Start the PDDL actions currently loaded in the executor from the
     * beginning.
     *
     * The actions will be executed in another thread after this method returns.
     * The returned future can be used to monitor, cancel or extend the
     * underlying task. The boolean contained in the future will be true only if
     * all actions appear to succeed.
     *
     * @param comment comment used for tracking/logging tasks starting the
     * actions
     *
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActions(String comment) {

        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
        setStartRunTime();
        runNumber++;
        int startRunNumber = runNumber;
        startActionsStartComments.add(comment + ",startRunNumber=" + startRunNumber + ",runNumber=" + runNumber);
        int startAbortCount = pddlExecutorJInternalFrame1.getSafeAbortRequestCount();
        lastContinueStartAbortCount = startAbortCount;
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(jCheckBoxMenuItemReloadSimFilesOnReverse.isSelected());
        }
        if (null != motomanCrclServerJInternalFrame) {
            String robotName = getRobotName();
            if (null != robotName) {
                if (robotName.toUpperCase().contains("MOTOMAN")) {
                    if (!motomanCrclServerJInternalFrame.isCrclMotoplusConnected()) {
                        try {
                            motomanCrclServerJInternalFrame.connectCrclMotoplus();
                        } catch (IOException ex) {
                            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    if (!motomanCrclServerJInternalFrame.isCrclMotoplusConnected()) {
                        setTitleErrorString("Motoman CRCL Server not connected.");
                    }
                }
            }
        }
        lastStartActionsFuture = XFuture.runAsync("AprsJFrame.startActions",
                () -> {
                    setThreadName();
                    takeSnapshots("startActions." + comment);
                }, runProgramService)
                .thenCompose("startActions.pauseCheck.comment=" + comment + ", startRunNumber" + startRunNumber, x -> waitForPause())
                .thenApplyAsync(taskName, x -> {
                    assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1 ";
                    if (runNumber != startRunNumber) {
                        throw new IllegalStateException("runNumbeChanged");
                    }
                    if (runNumber != startRunNumber
                            || pddlExecutorJInternalFrame1.getSafeAbortRequestCount() != startAbortCount) {
                        return false;
                    }
                    boolean ret = pddlExecutorJInternalFrame1.doActions("startActions." + comment + ", startRunNumber" + startRunNumber, startAbortCount);
                    startActionsFinishComments.add(comment + ",startRunNumber=" + startRunNumber + ",runNumber=" + runNumber);
                    if (runNumber != startRunNumber) {
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
    public boolean isDoingActions() {
        if (null == pddlExecutorJInternalFrame1) {
            return false;
        }
        return pddlExecutorJInternalFrame1.isDoingActions();
    }

    private void showKitInspection() {

    }

    private void startExploreGraphDb() {
        assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
        try {
            if (null == this.exploreGraphDbJInternalFrame) {
                this.exploreGraphDbJInternalFrame = new ExploreGraphDbJInternalFrame();
                this.exploreGraphDbJInternalFrame.setAprsJFrame(this);
                DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
                dbSetupPublisher.addDbSetupListener(exploreGraphDbJInternalFrame);
                exploreGraphDbJInternalFrame.accept(dbSetupPublisher.getDbSetup());
                this.addInternalFrame(exploreGraphDbJInternalFrame);
            }
            activateInternalFrame(this.exploreGraphDbJInternalFrame);

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void closeExploreGraphDb() {
        try {
            if (null != this.exploreGraphDbJInternalFrame) {
                // FIXME decide what to do later
            }
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get the current line in the CRCL program being run.
     *
     * @return current line
     */
    public int getCrclProgramLine() {
        assert (null != crclClientJInternalFrame) : ("null == pendantClientJInternalFrame  ");
        return this.crclClientJInternalFrame.getCurrentProgramLine();
    }

    private File propertiesFile;
    private File propertiesDirectory;
    private static final String APRS_DEFAULT_PROPERTIES_FILENAME = "aprs_properties.txt";
    private File lastAprsPropertiesFileFile;

    private static class AprsJFramePropDefaults {

        private final File propDir;
        private final File propFile;
        private final File lastAprsPropertiesFileFile;

        private static final AprsJFramePropDefaults single = new AprsJFramePropDefaults();

        public static AprsJFramePropDefaults getSingle() {
            return single;
        }

        public File getPropDir() {
            return propDir;
        }

        public File getPropFile() {
            return propFile;
        }

        public File getLastAprsPropertiesFileFile() {
            return lastAprsPropertiesFileFile;
        }

        private AprsJFramePropDefaults() {
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
                    Logger.getLogger(AprsJFrame.class
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
        return AprsJFramePropDefaults.getSingle().getPropDir();
    }

    /**
     * Get the default file location for saved properties.
     *
     * @return default file location
     */
    public static File getDefaultPropertiesFile() {
        return AprsJFramePropDefaults.getSingle().getPropFile();
    }

    /**
     * Get the default file location for a file that will could contain a
     * reference to the last properties file used.
     *
     * @return last file location
     */
    public static File getDefaultLastPropertiesFileFile() {
        return AprsJFramePropDefaults.getSingle().getLastAprsPropertiesFileFile();
    }

    private void initPropertiesFileInfo() {
        propertiesDirectory = getDefaultPropertiesDir();
        propertiesFile = getDefaultPropertiesFile();
        lastAprsPropertiesFileFile = getDefaultLastPropertiesFileFile();
    }

    private void publishDbSetup(DbSetupPublisher pub, DbSetup setup) {
        if (null != pub && setup.getDbType() != DbType.NONE && setup.getDbType() != null) {
            pub.setDbSetup(setup);
        }
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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, PointType point, String label) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
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
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pose, poseLabel, snapShotWidth, snapShotHeight);
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
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pt, pointLabel, snapShotWidth, snapShotHeight);
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
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pt, pointLabel, snapShotWidth, snapShotHeight);
        }
    }

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, snapShotWidth, snapShotHeight);
        }
    }

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param imgLabel string that will be added to the filename along with
     * timestamp and extention
     * @param itemsToPaint list of items to paint
     * @throws java.io.IOException problem writing to the file
     */
    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            this.object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), itemsToPaint, snapShotWidth, snapShotHeight);
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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, PoseType pose, String label, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, PointType point, String label, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, PmCartesian point, String label, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
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
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pose, poseLabel, w, h);
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
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pt, pointLabel, w, h);
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
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pt, pointLabel, w, h);
        }
    }

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     * @param w width of snapshot image
     * @param h height of snapshot image
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, w, h);
        }
    }

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
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
            this.object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), itemsToPaint, w, h);
        }
    }

    @MonotonicNonNull DbSetup dbSetup = null;

    @Override
    public final void loadProperties() throws IOException {

        IOException exA[] = new IOException[1];
        try {
            Utils.runAndWaitOnDispatchThread("loadProperties",
                    () -> {
                        loadPropertiesInternal(exA);
                    });
        } catch (InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != exA[0]) {
            throw new IOException(exA[0]);
        }
    }

    private void loadPropertiesInternal(IOException exA[]) {
        try {
            Properties props = new Properties();
            System.out.println("AprsJFrame loading properties from " + propertiesFile.getCanonicalPath());
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String startPddlPlannerString = props.getProperty(STARTUPPDDLPLANNER);
            if (null != startPddlPlannerString) {
                jCheckBoxMenuItemStartupPDDLPlanner.setSelected(Boolean.valueOf(startPddlPlannerString));
            }
            String startPddlExecutorString = props.getProperty(STARTUPPDDLEXECUTOR);
            if (null != startPddlExecutorString) {
                jCheckBoxMenuItemStartupPDDLExecutor.setSelected(Boolean.valueOf(startPddlExecutorString));
            }
            String startObjectSpString = props.getProperty(STARTUPPDDLOBJECTSP);
            if (null != startObjectSpString) {
                jCheckBoxMenuItemStartupObjectSP.setSelected(Boolean.valueOf(startObjectSpString));
            }

            String startObjectViewString = props.getProperty(STARTUPPDDLOBJECTVIEW);
            if (null != startObjectViewString) {
                jCheckBoxMenuItemStartupObject2DView.setSelected(Boolean.valueOf(startObjectViewString));
            }
            String startCRCLClientString = props.getProperty(STARTUPROBOTCRCLCLIENT);
            if (null != startCRCLClientString) {
                jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(Boolean.valueOf(startCRCLClientString));
            }
            String startCRCLSimServerString = props.getProperty(STARTUPROBOTCRCLSIMSERVER);
            if (null != startCRCLSimServerString) {
                jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(Boolean.valueOf(startCRCLSimServerString));
            }
            String startCRCLFanucServerString = props.getProperty(STARTUPROBOTCRCLFANUCSERVER);
            if (null != startCRCLFanucServerString) {
                jCheckBoxMenuItemStartupFanucCRCLServer.setSelected(Boolean.valueOf(startCRCLFanucServerString));
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
                jCheckBoxMenuItemStartupMotomanCRCLServer.setSelected(Boolean.valueOf(startCRCLMotomanServerString));
            }
            String startConnectDBString = props.getProperty(STARTUPCONNECTDATABASE);
            if (null != startConnectDBString) {
                jCheckBoxMenuItemConnectToDatabaseOnStartup.setSelected(Boolean.valueOf(startConnectDBString));
                if (jCheckBoxMenuItemConnectToDatabaseOnStartup.isSelected()) {
                    jCheckBoxMenuItemShowDatabaseSetup.setSelected(true);
                }
            }
            String startConnectVisionString = props.getProperty(STARTUPCONNECTVISION);
            if (null != startConnectVisionString) {
                jCheckBoxMenuItemConnectToVisionOnStartup.setSelected(Boolean.valueOf(startConnectVisionString));
                if (jCheckBoxMenuItemConnectToVisionOnStartup.isSelected()) {
                    jCheckBoxMenuItemStartupObjectSP.setSelected(true);
                }
            }
            String startExploreGraphDbString = props.getProperty(STARTUPEXPLOREGRAPHDB);
            if (null != startExploreGraphDbString) {
                jCheckBoxMenuItemExploreGraphDbStartup.setSelected(Boolean.valueOf(startExploreGraphDbString));
            }
            String crclWebAppPortString = props.getProperty(CRCLWEBAPPPORT);
            if (null != crclWebAppPortString) {
                crclWebServerHttpPort = Integer.parseInt(crclWebAppPortString);
            }
            String startCrclWebAppString = props.getProperty(STARTUPCRCLWEBAPP);
            if (null != startCrclWebAppString) {
                jCheckBoxMenuItemStartupCRCLWebApp.setSelected(Boolean.valueOf(startCrclWebAppString));
            }
            String startKitInspetion = props.getProperty(STARTUPKITINSPECTION);
            if (null != startKitInspetion) {
                jCheckBoxMenuItemKitInspectionStartup.setSelected(Boolean.valueOf(startKitInspetion));
            }
            this.updateSubPropertiesFiles();
            if (null != this.pddlPlannerJInternalFrame) {
                this.pddlPlannerJInternalFrame.loadProperties();
            }
            if (null != this.pddlExecutorJInternalFrame1) {
                XFuture.runAsync("loadProperties", () -> {
                    try {
                        if (null != this.pddlExecutorJInternalFrame1) {
                            this.pddlExecutorJInternalFrame1.loadProperties();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }, runProgramService).join();
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
                jCheckBoxMenuItemSnapshotImageSize.setSelected(Boolean.valueOf(reloadSimFilesOnReverseString));
            }
            String snapShotEnableString = props.getProperty(SNAP_SHOT_ENABLE_PROP);
            if (null != snapShotEnableString && snapShotEnableString.trim().length() > 0) {
                jCheckBoxMenuItemSnapshotImageSize.setSelected(Boolean.valueOf(snapShotEnableString));
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
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, exception);
            exA[0] = exception;
        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, exception);
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
     * @return active window
     */
    public ActiveWinEnum getActiveWin() {
        return activeWin;
    }

    /**
     * Select a window (InternalJFrame)to be shown on top.
     * 
     * @param activeWin
     */
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

        propsMap.put(STARTUPPDDLPLANNER, Boolean.toString(jCheckBoxMenuItemStartupPDDLPlanner.isSelected()));
        propsMap.put(STARTUPPDDLEXECUTOR, Boolean.toString(jCheckBoxMenuItemStartupPDDLExecutor.isSelected()));
        propsMap.put(STARTUPPDDLOBJECTSP, Boolean.toString(jCheckBoxMenuItemStartupObjectSP.isSelected()));
        propsMap.put(STARTUPPDDLOBJECTVIEW, Boolean.toString(jCheckBoxMenuItemStartupObject2DView.isSelected()));
        propsMap.put(STARTUPROBOTCRCLCLIENT, Boolean.toString(jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()));
        propsMap.put(STARTUPROBOTCRCLSIMSERVER, Boolean.toString(jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()));
        propsMap.put(STARTUPROBOTCRCLFANUCSERVER, Boolean.toString(jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()));
        propsMap.put(STARTUPROBOTCRCLMOTOMANSERVER, Boolean.toString(jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()));
        propsMap.put(STARTUPCONNECTDATABASE, Boolean.toString(jCheckBoxMenuItemConnectToDatabaseOnStartup.isSelected()));
        propsMap.put(STARTUPCONNECTVISION, Boolean.toString(jCheckBoxMenuItemConnectToVisionOnStartup.isSelected()));
        propsMap.put(STARTUPEXPLOREGRAPHDB, Boolean.toString(jCheckBoxMenuItemExploreGraphDbStartup.isSelected()));
        propsMap.put(STARTUPCRCLWEBAPP, Boolean.toString(jCheckBoxMenuItemStartupCRCLWebApp.isSelected()));
        propsMap.put(CRCLWEBAPPPORT, Integer.toString(crclWebServerHttpPort));
        propsMap.put(STARTUP_ACTIVE_WIN, activeWin.toString());
        propsMap.put(STARTUPKITINSPECTION, Boolean.toString(jCheckBoxMenuItemKitInspectionStartup.isSelected()));
        propsMap.put(MAX_LIMIT_PROP, maxLimit.toString());
        propsMap.put(MIN_LIMIT_PROP, minLimit.toString());
        propsMap.put(SNAP_SHOT_ENABLE_PROP, Boolean.toString(jCheckBoxMenuItemSnapshotImageSize.isSelected()));
        propsMap.put(SNAP_SHOT_WIDTH_PROP, Integer.toString(snapShotWidth));
        propsMap.put(SNAP_SHOT_HEIGHT_PROP, Integer.toString(snapShotHeight));
        propsMap.put(RELOAD_SIM_FILES_ON_REVERSE_PROP, Boolean.toString(jCheckBoxMenuItemReloadSimFilesOnReverse.isSelected()));
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
        System.out.println("AprsJFrame saving properties to " + propertiesFile.getCanonicalPath());
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
            } else if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
                setRobotName("Fanuc");
            } else if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
                setRobotName("Motoman");
            } else if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
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
    private static final String STARTUPCRCLWEBAPP = "startup.crclWebApp";
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
                java.util.logging.Logger.getLogger(AprsJFrame.class
                        .getName()).log(java.util.logging.Level.SEVERE, null, ex);

            }
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AprsJFrame aFrame = new AprsJFrame();
                aFrame.defaultInit();
                aFrame.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectDatabase;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectToDatabaseOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectToVisionOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectVision;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemEnableDebugDumpstacks;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemExploreGraphDbStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemForceFakeTake;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemKitInspectionStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReloadSimFilesOnReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowDatabaseSetup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSnapshotImageSize;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupCRCLWebApp;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupFanucCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupMotomanCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObject2DView;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObjectSP;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLExecutor;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLPlanner;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobotCrclGUI;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobtCRCLSimServer;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuExecute;
    private javax.swing.JMenuItem jMenuItemContinueActionList;
    private javax.swing.JMenuItem jMenuItemCreateActionListFromVision;
    private javax.swing.JMenuItem jMenuItemDebugAction;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemImmediateAbort;
    private javax.swing.JMenuItem jMenuItemLoadProperties;
    private javax.swing.JMenuItem jMenuItemLoadPropertiesFile;
    private javax.swing.JMenuItem jMenuItemLookFor;
    private javax.swing.JMenuItem jMenuItemReset;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSavePropsAs;
    private javax.swing.JMenuItem jMenuItemSetPoseMaxLimits;
    private javax.swing.JMenuItem jMenuItemSetPoseMinLimits;
    private javax.swing.JMenuItem jMenuItemStartActionList;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    // End of variables declaration//GEN-END:variables

    /**
     * Get the file where properties are read from and written to.
     * 
     * @return properties file
     */
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
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            updateSubPropertiesFiles();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
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

    @Nullable Future<?> disconnectDatabaseFuture = null;

    private void startDisconnectDatabase() {
        jCheckBoxMenuItemConnectDatabase.setEnabled(false);
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        disconnectDatabaseFuture = connectService.submit(this::disconnectDatabase);
    }

    private void disconnectDatabase() {

        assert (null != dbSetupJInternalFrame) : "null == dbSetupJInternalFrame ";
        try {
            dbConnected = false;
            if (null != connectDatabaseFuture) {
                connectDatabaseFuture.cancel(true);
                connectDatabaseFuture = null;
            }
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(false).build());
            List<Future<?>> futures = dbSetupPublisher.notifyAllDbSetupListeners();
            for (Future<?> f : futures) {
                if (!f.isDone() && !f.isCancelled()) {
                    try {
                        f.get(100, TimeUnit.MILLISECONDS);

                    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                        Logger.getLogger(AprsJFrame.class
                                .getName()).log(Level.SEVERE, null, ex);

                    }
                }
            }
            System.out.println("Finished disconnect from database.");

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
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

    @Override
    public void close() {
        closing = true;
        System.out.println("AprsJFrame.close()");
        closeAllWindows();
        connectService.shutdownNow();
        this.setVisible(false);
        this.dispose();
    }

    private void closeActionsToCrclJInternalFrame() throws Exception {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.close();
            pddlExecutorJInternalFrame1.setVisible(false);
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
            }
        }
    }

    private void closePddlPlanner() throws Exception {
        if (null != pddlPlannerJInternalFrame) {
            pddlPlannerJInternalFrame.close();
            pddlPlannerJInternalFrame.setVisible(false);
        }
    }

    private volatile boolean lastContinueCrclProgramResult = false;

    /**
     * Continue or start executing the currently loaded CRCL program.
     *
     * The actions will be executed in another thread after this method returns.
     * The task can be monitored or canceled using the returned future.
     *
     * @return a future that can be used to monitor, extend or cancel the
     * underlying task.
     *
     */
    public XFuture<Boolean> continueCrclProgram() {
        setStartRunTime();
        if (null != crclClientJInternalFrame) {
            return crclClientJInternalFrame.continueCurrentProgram();
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
    public void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame  ");
        visionToDbJInternalFrame.setEnableDatabaseUpdates(enableDatabaseUpdates, requiredParts);
        if (enableDatabaseUpdates && null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }

    /**
     * Get the current directory for saving log files
     *
     * @return log files directory
     * @throws java.io.IOException file can not be created ie default log directory does not exist.
     */
    public File getlogFileDir() throws IOException {
        File f = new File(Utils.getlogFileDir(), getRunName());
        f.mkdirs();
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
     *
     * @return reference to created file
     * @throws IOException directory doesn't exist etc.
     *
     */
    public File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, getlogFileDir());
    }

    /**
     * Create a temporary file in the given directory with the standard
     * timestamp string.
     *
     * @param prefix string filename will begin with
     * @param suffix string filename will end with (typically an extention eg
     * ".csv")
     *
     *
     * @param dir directory to create file in
     * @return reference to created file
     *
     * @throws IOException directory doesn't exist etc.
     */
    public File createTempFile(String prefix, String suffix, File dir) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, dir);
    }

    @Override
    public String toString() {
        return getTitle();
    }

    /**
     * Get a list with information on how the most recently loaded CRCL program
     * has run so far.
     * 
     * @return run data for last program
     */
    public List<ProgramRunData> getLastProgRunDataList() {
        if (null == crclClientJInternalFrame) {
            return Collections.emptyList();
        }
        return crclClientJInternalFrame.getLastProgRunDataList();
    }

    /**
     * Save the given run data which contains information on how a given program run went to 
     * a CSV file.
     * @param f file to save
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
     * Save the current run data which contains information on how a given program run went to 
     * a CSV file.
     * @param f file to save
     * @throws IOException file does not exist or not writeable etc
     */
    public void saveLastProgramRunDataListToCsv(File f) throws IOException {
        if (null == crclClientJInternalFrame) {
            return;
        }
        crclClientJInternalFrame.saveLastProgramRunDataListToCsv(f);
    }
}
