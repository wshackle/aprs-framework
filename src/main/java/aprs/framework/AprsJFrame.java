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
import aprs.framework.pddl.executor.PddlActionToCrclGenerator;
import aprs.framework.pddl.executor.PddlExecutorJInternalFrame;
import aprs.framework.pddl.executor.PositionMap;
import aprs.framework.pddl.planner.PddlPlannerJInternalFrame;
import aprs.framework.simview.Object2DViewJInternalFrame;
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
import crcl.ui.client.PendantClientJInternalFrame;
import crcl.ui.client.PendantClientJPanel;
import crcl.ui.client.UpdateTitleListener;
import crcl.ui.server.SimServerJInternalFrame;
import crcl.utils.CRCLException;
import crcl.utils.CRCLPosemath;
import crcl.utils.CRCLSocket;
import java.awt.Container;
import java.awt.HeadlessException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import rcs.posemath.PmCartesian;

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
public class AprsJFrame extends javax.swing.JFrame implements DisplayInterface, AutoCloseable {

    private VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    private PddlExecutorJInternalFrame pddlExecutorJInternalFrame1 = null;
    private Object2DViewJInternalFrame object2DViewJInternalFrame = null;
    private PddlPlannerJInternalFrame pddlPlannerJInternalFrame = null;
    private DbSetupJInternalFrame dbSetupJInternalFrame = null;
    private volatile PendantClientJInternalFrame pendantClientJInternalFrame = null;
    private SimServerJInternalFrame simServerJInternalFrame = null;
    private LogDisplayJInternalFrame logDisplayJInternalFrame = null;
    private FanucCRCLMain fanucCRCLMain = null;
    private FanucCRCLServerJInternalFrame fanucCRCLServerJInternalFrame = null;
    private ExploreGraphDbJInternalFrame exploreGraphDbJInternalFrame = null;
    private MotomanCrclServerJInternalFrame motomanCrclServerJInternalFrame = null;
    private KitInspectionJInternalFrame kitInspectionJInternalFrame = null;

    private String taskName;

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame
     * is recieved from vision.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    public XFuture<List<PhysicalItem>> getSingleVisionToDbUpdate() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
        return visionToDbJInternalFrame.getSingleUpdate();
    }

//    public XFuture<List<PhysicalItem>> getNextVisionToDbUpdate() {
//        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
//        return visionToDbJInternalFrame.getNextUpdate();
//    }
    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    public List<PartsTray> getPartsTrayList() {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
        return visionToDbJInternalFrame.getPartsTrayList();
    }

//    public XFuture<Void> getUpdatesFinished() {
//        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
//        return visionToDbJInternalFrame.getUpdatesFinished();
//    }
    public void refreshSimView() {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }

    public List<Slot> getSlotOffsets(String name) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
        return this.visionToDbJInternalFrame.getSlotOffsets(name);
    }

    public XFuture<Void> startVisionToDbNewItemsImageSave(File f) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
        return this.visionToDbJInternalFrame.startNewItemsImageSave(f);
    }

    public List<Slot> getSlots(Tray item) {
        assert (null != visionToDbJInternalFrame) : ("null == visionToDbJInternalFrame");
        return this.visionToDbJInternalFrame.getSlots(item);
    }

    public double getVisionToDBRotationOffset() {
        return this.visionToDbJInternalFrame.getRotationOffset();
    }

    /**
     * Pause a currently executing CRCL program.
     *
     * No action is taken if the CRCL Client has not been started and connected
     * or not program was currently running.
     */
    public void pauseCrclProgram() {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.pauseCrclProgram();
        }
    }

    public KitInspectionJInternalFrame getKitInspectionJInternalFrame() {
        return kitInspectionJInternalFrame;
    }

    private int runNumber = (int) ((System.currentTimeMillis() / 10000) % 1000);

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
                + ((this.robotName != null) ? this.robotName : "");
    }

    /**
     * Checks whether there is currently a CRCL program that is loaded but
     * paused.
     *
     * @return whether a program is paused
     */
    public boolean isCrclProgramPaused() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.isPaused();
        }
        return false;
    }

    public Thread getCrclRunProgramThread() {
        return pendantClientJInternalFrame.getRunProgramThread();
    }

    public XFuture<Boolean> getCrclRunProgramFuture() {
        return pendantClientJInternalFrame.getRunProgramFuture();
    }

    /**
     * Checks whether there is currently a CRCL program running.
     *
     * @return whether a CRCL program is running.
     */
    public boolean isRunningCrclProgram() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.isRunningProgram();
        }
        return false;
    }

    public boolean isBlockCrclPrograms() {
        return pendantClientJInternalFrame.isBlockPrograms();
    }

    public int startBlockingCrclPrograms() {
        return pendantClientJInternalFrame.startBlockingPrograms();
    }

    public int stopBlockingCrclPrograms(int count) {
        return pendantClientJInternalFrame.stopBlockingPrograms(count);
    }

    /**
     * Get the currently loaded CRCL program.
     *
     * @return current CRCL program or null if no program is loaded.
     */
    public CRCLProgramType getCrclProgram() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getProgram();
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
            return null;
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
    public PoseType getCurrentPose() {
        if (null != pendantClientJInternalFrame && pendantClientJInternalFrame.isConnected()) {
            return pendantClientJInternalFrame.getCurrentPose();
        }
        return null;
    }

    /**
     * Get the current point(translation only) from current pose of the robot.
     *
     * @return current point(translation only) from current pose of the robot.
     */
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
    public boolean isConnected() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.isConnected();
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
        if (null == robotName && connected) {
            printNameSetInfo();
            throw new IllegalStateException("Can not connect when robotName is null");
        }
        if (null != pendantClientJInternalFrame) {
            if (pendantClientJInternalFrame.isConnected() != connected) {
                if (connected) {
                    pendantClientJInternalFrame.connectCurrent();
                } else {
                    pendantClientJInternalFrame.disconnect();
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

    public XFuture<Void> getSafeAbortFuture() {
        return safeAbortFuture;
    }

    public XFuture<Boolean> getLastRunProgramFuture() {
        return lastRunProgramFuture;
    }

    public XFuture<Boolean> getLastResumeFuture() {
        return lastResumeFuture;
    }

    private volatile XFuture<Void> safeAbortFuture = null;

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
     * @return a future that can be tested or used to wait until the abort is
     * completed.
     */
    public XFuture<Void> startSafeAbort() {
        takeSnapshots("startSafeAbort");
        safeAbortFuture = this.pddlExecutorJInternalFrame1.startSafeAbort()
                .thenRun(() -> {
                    if (null != continousDemoFuture) {
                        continousDemoFuture.cancelAll(true);
                        continousDemoFuture = null;
                    }
                });
        return safeAbortFuture;
    }

    private volatile Thread startSafeAbortAndDisconnectThread = null;
    private volatile long startSafeAbortAndDisconnectTime = -1;
    private volatile StackTraceElement startSafeAbortAndDisconnectStackTrace[] = null;

    /**
     * Safely abort the current CRCL program and then disconnect from the
     * robot's CRCL server.
     *
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned
     * future. * @return a future that can be tested or used to wait until the
     * abort and disconnect is completed.
     *
     * @return future providing info on when complete
     */
    public XFuture<Void> startSafeAbortAndDisconnect(String name) {
        try {
            startSafeAbortAndDisconnectThread = Thread.currentThread();
            startSafeAbortAndDisconnectTime = System.currentTimeMillis();
            startSafeAbortAndDisconnectStackTrace = startSafeAbortAndDisconnectThread.getStackTrace();

            safeAbortFuture
                    = this.pddlExecutorJInternalFrame1.startSafeAbort()
                            .thenCompose(name + ".disconnect." + robotName, x -> disconnectRobot());
            return safeAbortFuture;
        } catch (Exception e) {
            setTitleErrorString(e.toString());
            XFuture<Void> ret = new XFuture<>("startSafeAbortAndDisconnect." + e.toString());
            ret.completeExceptionally(e);
            return ret;
        }
    }

    private volatile XFuture<Void> disconnectRobotFuture = null;

    /**
     * Disconnect from the robot's crcl server and set robotName to null.
     *
     * Note: setConnected(false) also disconnects from the crcl server but
     * leaves the robotName unchanged.
     */
    public XFuture<Void> disconnectRobot() {
        disconnectRobotFuture = waitForPause().
                thenRunAsync(this::disconnectRobotPrivate, connectService);
        System.out.println("disconnectRobotFuture = " + disconnectRobotFuture);
        System.out.println("connectService = " + connectService);
        return disconnectRobotFuture;
    }

    private void disconnectRobotPrivate() {
        setThreadName();
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.disconnect();
        }
        if (null != getRobotName()) {
            takeSnapshots("disconnectRobot");
        }
        this.setRobotName(null);
        System.out.println("disconnectRobot completed");
    }

    /**
     * Connect to a given robot with a CRCL server running on the given host and
     * TCP port.
     *
     * @param robotName name of the robot
     * @param host host running robot's CRCL server
     * @param port (TCP) port robot's CRCL server is bound to
     */
    public XFuture<Void> connectRobot(String robotName, String host, int port) {
        return waitForPause().
                thenRunAsync(() -> connectRobotPrivate(robotName, host, port), connectService);
    }

    private void connectRobotPrivate(String robotName, String host, int port) {
        setThreadName();
        if (null != pendantClientJInternalFrame) {
            setRobotName(robotName);
            pendantClientJInternalFrame.connect(host, port);
        }
    }

    /**
     * Get robot's CRCL host.
     *
     * @return robot's CRCL host.
     */
    public String getRobotCrclHost() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getHost();
        }
        return null;
    }

    /**
     * Get robot's CRCL port number.
     *
     * @return robot's CRCL port number.
     */
    public int getRobotCrclPort() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getPort();
        }
        return -1;
    }

    final ConcurrentLinkedDeque<XFuture<?>> futuresToCompleteOnUnPause = new ConcurrentLinkedDeque<>();

    private void notifyPauseFutures() {
        for (XFuture<?> f : futuresToCompleteOnUnPause) {
            f.complete(null);
        }
    }

    private volatile XFuture<Void> lastPauseFuture = null;

    public XFuture<Void> waitForPause() {
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

    private volatile XFuture<Void> lastContinueActionListFuture = null;
    private volatile String lastContinueActionListFutureComment = null;

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
    public XFuture<Void> continueActionList(String comment) {
        lastContinueActionListFutureComment = comment;
        lastContinueActionListFuture
                = XFuture.supplyAsync("AprsJFrame.continueActionList",
                        () -> {
                            setThreadName();
                            takeSnapshots("continueActionList" + ((comment != null) ? comment : ""));
                            return null;
                        }, runProgramService)
                        .thenCompose("continueActionList.pauseCheck", x -> waitForPause())
                        .thenRun("pddlExecutorJInternalFrame1.completeActionList", () -> pddlExecutorJInternalFrame1.completeActionList());
        return lastContinueActionListFuture;
    }

    public double getClosestRobotPartDistance() {
        return  this.object2DViewJInternalFrame.getClosestRobotPartDistance();
    }
    
    public boolean isObjectViewSimulated() {
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

    private String robotName = null;

    /**
     * Get the value of robotName
     *
     * @return the value of robotName
     */
    public String getRobotName() {
        return robotName;
    }

    private volatile Thread setRobotNameNullThread = null;
    private volatile StackTraceElement setRobotNameNullStackTrace[] = null;
    private volatile long setRobotNameNullThreadTime = -1;
    private volatile Thread setRobotNameNonNullThread = null;
    private volatile StackTraceElement setRobotNameNonNullStackTrace[] = null;
    private volatile long setRobotNameNonNullThreadTime = -1;

    /**
     * Set the value of robotName
     *
     * @param robotName new value of robotName
     */
    public void setRobotName(String robotName) {
        if (null == robotName) {
            setRobotNameNullThread = Thread.currentThread();
            setRobotNameNullStackTrace = setRobotNameNullThread.getStackTrace();
            setRobotNameNullThreadTime = System.currentTimeMillis();
        } else {
            setRobotNameNonNullThread = Thread.currentThread();
            setRobotNameNonNullStackTrace = setRobotNameNonNullThread.getStackTrace();
            setRobotNameNonNullThreadTime = System.currentTimeMillis();
        }
        this.robotName = robotName;
        Utils.runOnDispatchThread(() -> updateTitle("", ""));
    }

    /**
     * Register a program line listener that will be notified each time a line
     * of the CRCL program is executed.
     *
     * @param l listener to be registered
     */
    public void addProgramLineListener(PendantClientJPanel.ProgramLineListener l) {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.addProgramLineListener(l);
        }
    }

    /**
     * Remove a previously added program line listener.
     *
     * @param l listener to be removed
     */
    public void removeProgramLineListener(PendantClientJPanel.ProgramLineListener l) {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.removeProgramLineListener(l);
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
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.addCurrentPoseListener(l);
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
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.removeCurrentPoseListener(l);
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
        if (null != pendantClientJInternalFrame) {
            synchronized (pendantClientJInternalFrame) {
                pendantClientJInternalFrame.setProgram(program);
            }
        }
    }

    private volatile XFuture<Boolean> lastRunProgramFuture = null;

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
    public synchronized XFuture<Boolean> startCRCLProgram(CRCLProgramType program) throws JAXBException {
        if (null != pendantClientJInternalFrame) {
            lastRunProgramFuture = XFuture.supplyAsync(getRunName() + ".startCRCLProgram(" + program.getName() + ")",
                    () -> {
                        setThreadName();
                        takeSnapshots("startCRCLProgram(" + program.getName() + ")");
                        return null;
                    }, runProgramService)
                    .thenCompose("startCRCLProgram(" + program.getName() + ").pauseCheck", x -> waitForPause())
                    .thenApply("startCRCLProgram(" + program.getName() + ").runProgram", x -> {
                        try {
                            pendantClientJInternalFrame.setProgram(program);
                            return pendantClientJInternalFrame.runCurrentProgram();
                        } catch (JAXBException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
            return lastRunProgramFuture;
        }
        XFuture<Boolean> ret = new XFuture<>("startCRCLProgram.pendantClientJInternalFrame==null");
        ret.completeExceptionally(new IllegalStateException("null != pendantClientJInternalFrame"));
        return ret;
    }

    public boolean runCRCLProgram(CRCLProgramType program) throws JAXBException {
        pendantClientJInternalFrame.setProgram(program);
        return pendantClientJInternalFrame.runCurrentProgram();
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
        if (null != this.continousDemoFuture) {
            this.continousDemoFuture.cancelAll(true);
            this.continousDemoFuture = null;
        }
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.abortProgram();
        } else {
            abortCrclProgram();
        }
        if (null != lastResumeFuture) {
            lastResumeFuture.cancelAll(true);
            lastResumeFuture = null;
        }
        if (null != lastRunProgramFuture) {
            lastRunProgramFuture.cancelAll(true);
            lastRunProgramFuture = null;
        }
        jCheckBoxMenuItemContinousDemo.setSelected(false);
    }

    /**
     * Create a string with current details for display and/or logging.
     *
     * @return details string
     */
    public String getDetailsString() {
        StringBuilder sb = new StringBuilder();
        if (null != pendantClientJInternalFrame) {
            CRCLCommandType cmd = pendantClientJInternalFrame.getCurrentProgramCommand();
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
            if(null != prog) {
                sb.append("crcl_program_index=(").append(getCrclProgramLine()).append(" / ").append(prog.getMiddleCommand().size()).append("), ");
                sb.append("crcl_program_name=").append(prog.getName()).append("\r\n");
            }
            pendantClientJInternalFrame.getCurrentStatus().ifPresent(status -> {
                if (null != status.getCommandStatus()
                        && null != status.getCommandStatus().getStateDescription()
                        && status.getCommandStatus().getStateDescription().length() > 0) {
                    sb.append("state description = ").append(status.getCommandStatus().getStateDescription()).append("\r\n");
                } else {
                    sb.append("state description = \r\n");
                }
            });
            pendantClientJInternalFrame.getCurrentState().ifPresent(state -> sb.append("state=").append(state).append("\r\n"));
        }
        if (null != pddlExecutorJInternalFrame1) {
            List<PddlAction> actions = pddlExecutorJInternalFrame1.getActionsList();
            int curActionIndex = pddlExecutorJInternalFrame1.getCurrentActionIndex();
            if (null != actions) {
                sb.append("PDDL curActionIndex= ").append(curActionIndex);
                sb.append(" out of ").append(actions.size()).append("\r\n");
                if (curActionIndex >= 0 && curActionIndex < actions.size()) {
                    sb.append("PDDL action =").append(actions.get(curActionIndex)).append("\r\n");
                } else {
                    sb.append("PDDL action = \r\n");
                }
            }
            String pddlErrorString = this.pddlExecutorJInternalFrame1.getErrorString();
            if (null != pddlErrorString && pddlErrorString.length() > 0) {
                sb.append("pddlExecutorError=").append(pddlErrorString).append("\r\n");
            }
            sb.append("actionSetsCompleted=").append(pddlExecutorJInternalFrame1.getActionSetsCompleted()).append("\r\n");
        }
        sb.append("robotCrclPort=").append(this.getRobotCrclPort()).append(", ");
        boolean connected = (null != pendantClientJInternalFrame && pendantClientJInternalFrame.isConnected());
        sb.append("connected=").append(connected).append(", ");
        sb.append("reverseFlag=").append(jCheckBoxMenuItemReverse.isSelected()).append(", ");
        sb.append("paused=").append(jCheckBoxMenuItemPause.isSelected()).append("\r\n");
        sb.append("run_name=").append(this.getRunName()).append("\r\n");
        sb.append("crclRunning=").append(this.isRunningCrclProgram()).append("\r\n");
        if (null != this.getCrclProgram()) {
            sb.append("crclProgramName=").append(this.getCrclProgram().getName()).append("\r\n");
        }
        String crclClientErrString = getCrclClientErrorString();
        if (null != crclClientErrString && crclClientErrString.length() > 0
                && !Objects.equals(titleErrorString, crclClientErrString)) {
            sb.append("crclClientErrString=").append(crclClientErrString).append("\r\n");
            if (!isPaused()) {
                pause();
            }
        }
        sb.append("                                                                                                                                                                                                                                                                                        \r\n");

        if (null != titleErrorString && titleErrorString.length() > 0) {
            sb.append("titleErrorString=").append(titleErrorString).append("\r\n");
        }
//        sb.append("1111111111222222222233333333334444444444555555555566666666667777777777788888888899999999990000000000111111111122222222223333333333\r\n");
//        sb.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\r\n");
        return sb.toString();
    }

    /**
     * Abort the current CRCL program.
     */
    public void abortCrclProgram() {
        if (null != pendantClientJInternalFrame && pendantClientJInternalFrame.isConnected()) {
            pendantClientJInternalFrame.abortProgram();
        }
    }

    private PrintStream origOut = null;
    private PrintStream origErr = null;

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
            fanucCRCLMain = new FanucCRCLMain();
            if (null == fanucCRCLServerJInternalFrame) {
                fanucCRCLServerJInternalFrame = new FanucCRCLServerJInternalFrame();
                addInternalFrame(fanucCRCLServerJInternalFrame);
            }
            fanucCRCLMain.setDisplayInterface(fanucCRCLServerJInternalFrame);
            fanucCRCLMain.startDisplayInterface();
            fanucCRCLMain.start(fanucPreferRNN, fanucNeighborhoodName, fanucRobotHost, fanucCrclPort);
        } catch (CRCLException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String titleErrorString = null;

    /**
     * Get the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @return title error string
     */
    public String getTitleErrorString() {
        return titleErrorString;
    }

    private volatile CommandStatusType titleErrorStringCommandStatus = null;

    private volatile String lastNewTitleErrorString = null;

    /**
     * Set the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @param newTitleErrorString title error string
     */
    public void setTitleErrorString(String newTitleErrorString) {
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
                motomanCrclServerJInternalFrame = new MotomanCrclServerJInternalFrame();
                updateSubPropertiesFiles();
                motomanCrclServerJInternalFrame.loadProperties();
                motomanCrclServerJInternalFrame.connectCrclMotoplus();
                motomanCrclServerJInternalFrame.setVisible(true);
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

    public String getThreadName() {
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
    private XFuture connectDatabaseFuture = null;

    public ExecutorService getRunProgramService() {
        return runProgramService;
    }

    private XFuture<Boolean> startConnectDatabase() {
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
            Utils.runOnDispatchThread(() -> visionToDbJInternalFrame.connectVision());
        }
    }

    private void disconnectVision() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect vision when already closing.");
        }
        if (null != visionToDbJInternalFrame) {
            Utils.runOnDispatchThread(() -> visionToDbJInternalFrame.disconnectVision());
        }
    }

    /**
     * Creates new AprsJFrame using a default properties file.
     */
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

    /**
     * Initialize the frame ignoring any previously saved settings.
     */
    final public void emptyInit() {
        initLoggerWindow();
        commonInit();
    }

    /**
     * Creates new AprsJFrame using a specified properties file.
     *
     * @param propertiesFile properties file to be read for initial settings
     */
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

    private void commonInit() {
        startWindowsFromMenuCheckboxes();

        try {
            setIconImage(ImageIO.read(AprsJFrame.class.getResource("aprs.png")));
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
                startActionsToCrclJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startPendantClientJInternalFrame();
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
            createDbSetupFrame();
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
            logDisplayJInternalFrame.setVisible(true);
            jDesktopPane1.add(logDisplayJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            System.setOut(new MyPrintStream(System.out, logDisplayJInternalFrame));
            System.setErr(new MyPrintStream(System.err, logDisplayJInternalFrame));
            activateInternalFrame(logDisplayJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
        if (null != simServerJInternalFrame) {
            try {
                simServerJInternalFrame.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            simServerJInternalFrame.setVisible(false);
            simServerJInternalFrame = null;
        }
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setVisible(false);
            object2DViewJInternalFrame.dispose();
            object2DViewJInternalFrame = null;
        }

        abortCrclProgram();
        disconnectRobot();
        disconnectVision();
        disconnectDatabase();
        if (null != this.dbSetupJInternalFrame) {
            this.dbSetupJInternalFrame.getDbSetupPublisher().removeAllDbSetupListeners();
            this.dbSetupJInternalFrame.setVisible(false);
            this.dbSetupJInternalFrame.dispose();
            this.dbSetupJInternalFrame = null;
        }
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        for (JInternalFrame f : frames) {
            jDesktopPane1.getDesktopManager().closeFrame(f);
            jDesktopPane1.remove(f);
        }
        if (null != this.exploreGraphDbJInternalFrame) {
            this.exploreGraphDbJInternalFrame.setVisible(false);
            this.exploreGraphDbJInternalFrame.dispose();
            this.exploreGraphDbJInternalFrame = null;
        }
        if (null != this.logDisplayJInternalFrame) {
            this.logDisplayJInternalFrame.setVisible(false);
            this.logDisplayJInternalFrame.dispose();
            this.logDisplayJInternalFrame = null;
        }
        this.dbSetup = null;

//        this.pddlExecutorJInternalFrame1 = null;
//        this.pddlPlannerJInternalFrame = null;
//        this.object2DViewJInternalFrame = null;
//        this.dbSetupPublisherSupplier = null;
//        this.fanucCRCLServerJInternalFrame = null;
//        this.pendantClientJInternalFrame = null;
//        this.motomanCrclServerJInternalFrame = null;
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
        dbSetupJInternalFrame.setPropertiesFile(propertiesFile);
        dbSetupJInternalFrame.setVisible(true);
        if (checkInternalFrame(dbSetupJInternalFrame)) {
            jDesktopPane1.getDesktopManager().deiconifyFrame(dbSetupJInternalFrame);
            jDesktopPane1.getDesktopManager().activateFrame(dbSetupJInternalFrame);
            jDesktopPane1.getDesktopManager().maximizeFrame(dbSetupJInternalFrame);
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

    private ACTIVE_WINDOW_NAME activeWin = ACTIVE_WINDOW_NAME.OTHER;

    private static enum ACTIVE_WINDOW_NAME {
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

    private ACTIVE_WINDOW_NAME stringToWin(String str) {
        if (str.startsWith("CRCL Client")) {
            return ACTIVE_WINDOW_NAME.CRCL_CLIENT_WINDOW;
        }
        if (str.startsWith("Error")) {
            return ACTIVE_WINDOW_NAME.ERRLOG_WINDOW;
        }
        if (str.startsWith("Object2D")) {
            return ACTIVE_WINDOW_NAME.SIMVIEW_WINDOW;
        }
        if (str.startsWith("[Object SP]") || str.endsWith("Vision To Database")) {
            return ACTIVE_WINDOW_NAME.VISION_TO_DB_WINDOW;
        }
        if (str.startsWith("CRCL Simulation Server")) {
            return ACTIVE_WINDOW_NAME.CRCL_CLIENT_WINDOW;
        }
        if (str.startsWith("Database Setup")) {
            return ACTIVE_WINDOW_NAME.DATABASE_SETUP_WINDOW;
        }
        if (str.startsWith("PDDL Planner")) {
            return ACTIVE_WINDOW_NAME.PDDL_PLANNER_WINDOW;
        }
        if (str.startsWith("PDDL Actions to CRCL") || str.endsWith("(Executor)")) {
            return ACTIVE_WINDOW_NAME.PDDL_EXECUTOR_WINDOW;
        }
        if (str.startsWith("Kit") || str.endsWith("(Inspection)")) {
            return ACTIVE_WINDOW_NAME.KIT_INSPECTION_WINDOW;
        }
        return ACTIVE_WINDOW_NAME.OTHER;
    }

    private void startObject2DJinternalFrame() {
        try {
            object2DViewJInternalFrame = new Object2DViewJInternalFrame();
            updateSubPropertiesFiles();
            object2DViewJInternalFrame.setAprsJFrame(this);
            object2DViewJInternalFrame.loadProperties();
            object2DViewJInternalFrame.pack();
            object2DViewJInternalFrame.setVisible(true);
            jDesktopPane1.add(object2DViewJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            object2DViewJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(object2DViewJInternalFrame);
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

    public String getCrclClientErrorString() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getCrclClientErrorMessage();
        }
        return null;
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
        String newTitle = "APRS : " + ((robotName != null) ? robotName : "NO Robot") + " : " + ((taskName != null) ? taskName : "NO Task") + " : " + stateString + " : "
                + stateDescription
                + ((titleErrorString != null) ? ": " + titleErrorString : "")
                + ((pddlExecutorJInternalFrame1 != null) ? (" : " + pddlExecutorJInternalFrame1.getActionSetsCompleted()) : "");
        if (newTitle.length() > 90) {
            newTitle = newTitle.substring(0, 90) + " ... ";
        }
        if (!oldTitle.equals(newTitle)) {
            setTitle(newTitle);
            setupWindowsMenu();
        }
        for (Runnable r : titleUpdateRunnables) {
            r.run();
        }
    }

    /**
     * Get the current status if available
     *
     * @return current status or null
     */
    public CRCLStatusType getCurrentStatus() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getCurrentStatus().orElse(null);
        }
        return null;
    }

    /**
     * Update the title based on the current state.
     */
    public void updateTitle() {
        Utils.runOnDispatchThread(this::updateTitleInternal);
    }

    public CommandStatusType getCommandStatus() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getCurrentStatus()
                    .map(x -> x.getCommandStatus())
                    .map(CRCLPosemath::copy)
                    .orElse(null);
        }
        return null;
    }

    private void updateTitleInternal() {
        if (null != pendantClientJInternalFrame) {
            CommandStatusType cs = pendantClientJInternalFrame.getCurrentStatus()
                    .map(x -> x.getCommandStatus())
                    .orElse(null);
            if (null == cs || null == cs.getCommandState()) {
                updateTitle("", "");
            } else {
                if (cs.getCommandState() == CommandStateEnumType.CRCL_DONE) {
                    if (null != titleErrorStringCommandStatus
                            && (titleErrorStringCommandStatus.getCommandID() != cs.getCommandID()
                            || (titleErrorStringCommandStatus.getCommandState() == CommandStateEnumType.CRCL_ERROR))) {
                        titleErrorString = null;
                    }
                }
                updateTitle(cs.getCommandState().toString(), cs.getStateDescription());
            }
        } else {
            updateTitle("", "");
        }
    }

    private void startPendantClientJInternalFrame() {
        try {
            pendantClientJInternalFrame = new PendantClientJInternalFrame();
            pendantClientJInternalFrame.setRunProgramService(runProgramService);
            pendantClientJInternalFrame.setTempLogDir(Utils.getlogFileDir());
            updateSubPropertiesFiles();
            pendantClientJInternalFrame.loadProperties();
//            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
//            pendantClientJInternalFrame.loadProperties();
            pendantClientJInternalFrame.pack();
            pendantClientJInternalFrame.setVisible(true);
            jDesktopPane1.add(pendantClientJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            pendantClientJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(pendantClientJInternalFrame);
            pendantClientJInternalFrame.addUpdateTitleListener(new UpdateTitleListener() {
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
                    pendantClientJInternalFrame.addCurrentPoseListener(l);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startSimServerJInternalFrame() {
        try {
            if (null == simServerJInternalFrame) {
                simServerJInternalFrame = new SimServerJInternalFrame(false);
//            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
//            pendantClientJInternalFrame.loadProperties();
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
//            dbSetupJInternalFrame.setVisible(true);
//            jDesktopPane1.getDesktopManager().deiconifyFrame(dbSetupJInternalFrame);
//            jDesktopPane1.getDesktopManager().activateFrame(dbSetupJInternalFrame);
//            dbSetupJInternalFrame.moveToFront();
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
            dbSetupJInternalFrame.loadRecent();
            jDesktopPane1.add(dbSetupJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            dbSetupJInternalFrame.getDbSetupPublisher().addDbSetupListener(dbSetupListener);
            if (null != dbSetup) {
                DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
                if (null != pub) {
                    pub.setDbSetup(dbSetup);
                }
            }
        }
    }

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
//            DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
//            if (null != pub) {
//                pub.addDbSetupListener(toDbListener);
//            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startActionsToCrclJInternalFrame() {
        try {
            if (null == pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1 = new PddlExecutorJInternalFrame();
                this.pddlExecutorJInternalFrame1.pack();
            }
            this.pddlExecutorJInternalFrame1.setAprsJFrame(this);
            this.pddlExecutorJInternalFrame1.setVisible(true);
            jDesktopPane1.add(pddlExecutorJInternalFrame1, JLayeredPane.DEFAULT_LAYER);
            pddlExecutorJInternalFrame1.getDesktopPane().getDesktopManager().maximizeFrame(pddlExecutorJInternalFrame1);
            updateSubPropertiesFiles();
//            this.pddlExecutorJInternalFrame1.setPropertiesFile(new File(propertiesDirectory, "actionsToCrclProperties.txt"));
            this.pddlExecutorJInternalFrame1.loadProperties();
            pddlExecutorJInternalFrame1.setDbSetupSupplier(dbSetupPublisherSupplier);
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

            }

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
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
        jCheckBoxMenuItemContinousDemo = new javax.swing.JCheckBoxMenuItem();
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

        jCheckBoxMenuItemContinousDemo.setText("Continous Demo");
        jCheckBoxMenuItemContinousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinousDemoActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemContinousDemo);

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
                startActionsToCrclJInternalFrame();
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

    public void browseOpenPropertiesFile() throws HeadlessException {
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
                startPendantClientJInternalFrame();
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

    CRCLWebAppRunner crclWebAppRunner = null;

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
        this.startActions();
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
        setReverseFlag(reverseFlag);
    }//GEN-LAST:event_jCheckBoxMenuItemReverseActionPerformed

    private void jCheckBoxMenuItemContinousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinousDemoActionPerformed
        boolean start = jCheckBoxMenuItemContinousDemo.isSelected();
        boolean reverseFlag = jCheckBoxMenuItemReverse.isSelected();
        setTitleErrorString(null);
        immediateAbort();
        if (start) {
            if (!jCheckBoxMenuItemContinousDemo.isSelected()) {
                jCheckBoxMenuItemContinousDemo.setSelected(true);
            }
            continousDemoFuture = startContinousDemo(reverseFlag);
        } else {
            immediateAbort();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemContinousDemoActionPerformed

    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        System.out.println("jCheckBoxMenuItemPause.isSelected() = " + jCheckBoxMenuItemPause.isSelected());
        if (null != continousDemoFuture) {
            System.out.println("continousDemoFuture.isDone() = " + continousDemoFuture.isDone());
            System.out.println("continousDemoFuture.isCancelled() = " + continousDemoFuture.isCancelled());
        }
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            resume();
        }
        if (null != continousDemoFuture) {
            System.out.println("continousDemoFuture.isDone() = " + continousDemoFuture.isDone());
            System.out.println("continousDemoFuture.isCancelled() = " + continousDemoFuture.isCancelled());
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
        lookForParts();
    }//GEN-LAST:event_jMenuItemLookForActionPerformed

    public XFuture<Boolean> lookForParts() {
        return pddlExecutorJInternalFrame1.lookForParts();
    }

    private void setImageSizeMenuText() {
        jCheckBoxMenuItemSnapshotImageSize.setText(String.format("Snapshot Image size (%d x %d )", snapShotWidth, snapShotHeight));
    }

    public boolean isWithinMaxLimits(PmCartesian cart) {
        return cart != null
                && cart.x <= maxLimit.x
                && cart.y <= maxLimit.y
                && cart.z <= maxLimit.z;
    }

    public boolean isWithinMinLimits(PmCartesian cart) {
        return cart != null
                && cart.x >= minLimit.x
                && cart.y >= minLimit.y
                && cart.z >= minLimit.z;
    }

    public boolean isWithinLimits(PmCartesian cart) {
        return isWithinMaxLimits(cart) && isWithinMinLimits(cart);
    }

    public boolean isWithinLimits(PoseType pose) {
        return null != pose && isWithinLimits(pose.getPoint());
    }

    public boolean isWithinLimits(PointType point) {
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

    private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
    }

//    private static double minDist(double sx, double sy, List<PhysicalItem> items) {
//        return items.stream()
//                .filter(x -> x.getType().equals("P"))
//                .mapToDouble(x -> Math.hypot(x.x - sx, x.y - sy))
//                .min()
//                .orElse(Double.POSITIVE_INFINITY);
//    }
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        return visionToDbJInternalFrame.absSlotFromTrayAndOffset(tray, offsetItem);
    }

    public void createActionListFromVision() {
        try {
            List<PhysicalItem> itemsList = getSimviewItems();
            Map<String, Integer> requiredItemsMap
                    = itemsList.stream()
                            .filter(this::isWithinLimits)
                            .collect(Collectors.toMap(PhysicalItem::getName, x -> 1, (a, b) -> a + b));
            String requiredItemsString
                    = requiredItemsMap
                            .entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining(" "));
            System.out.println("requiredItemsString = " + requiredItemsString);
            List<PhysicalItem> kitTrays = itemsList.stream()
                    .filter(x -> "KT".equals(x.getType()))
                    .collect(Collectors.toList());
            if (kitTrays.isEmpty()) {
                if (JOptionPane.YES_OPTION
                        != JOptionPane.showConfirmDialog(this, "Create action list with no kit trays?")) {
                    setTitleErrorString("createActionListFromVision: No kit trays");
                    throw new IllegalStateException("No kit trays");
                }
            }

            File f = createTempFile("actionList", ".txt");
            boolean allEmpty = true;
            try (PrintStream ps = new PrintStream(new FileOutputStream(f))) {
                ps.println("(clear-kits-to-check)");
                ps.println("(look-for-parts 0 " + requiredItemsString + ")");
                ConcurrentMap<String, Integer> kitUsedMap = new ConcurrentHashMap<>();
                ConcurrentMap<String, Integer> ptUsedMap = new ConcurrentHashMap<>();
                List<String> kitToCheckStrings = new ArrayList<>();
                for (PhysicalItem kit : kitTrays) {
                    Map<String, String> slotPrpToPartSkuMap = new HashMap<>();
                    List<Slot> slotOffsetList = getSlotOffsets(kit.getName());
                    double x = kit.x;
                    double y = kit.y;
                    double rot = kit.getRotation();
                    String shortKitName = kit.getName();
                    if (shortKitName.startsWith("sku_")) {
                        shortKitName = shortKitName.substring(4);
                    }
                    int kitNumber = -1;
                    for (Slot slotOffset : slotOffsetList) {
                        PhysicalItem absSlot = absSlotFromTrayAndOffset(kit, slotOffset);
                        PhysicalItem closestPart = closestPart(absSlot.x, absSlot.y, itemsList);
                        double minDist = Math.hypot(absSlot.x - closestPart.x, absSlot.y - closestPart.y);
                        if (minDist < 20) {
                            int pt_used_num = ptUsedMap.compute(closestPart.getName(), (k, v) -> (v == null) ? 1 : (v + 1));
                            String shortPartName = closestPart.getName();
                            if (shortPartName.startsWith("sku_")) {
                                shortPartName = shortPartName.substring(4);
                            }
                            String partName = shortPartName + "_in_pt_" + pt_used_num;
                            ps.println("(take-part " + partName + ")");
                            String shortSkuName = slotOffset.getSlotForSkuName();
                            if (shortSkuName.startsWith("sku_")) {
                                shortSkuName = shortSkuName.substring(4);
                            }
                            if (shortSkuName.startsWith("part_")) {
                                shortSkuName = shortSkuName.substring(5);
                            }
                            if (kitNumber < 0) {
                                kitNumber = kitUsedMap.compute(kit.getName(), (k, v) -> (v == null) ? 1 : (v + 1));
                            }
                            String slotName = "empty_slot_" + slotOffset.getPrpName().substring(slotOffset.getPrpName().lastIndexOf("_") + 1) + "_for_" + shortSkuName + "_in_" + shortKitName + "_" + kitNumber;
                            ps.println("(place-part " + slotName + ")");
                            slotPrpToPartSkuMap.put(slotOffset.getPrpName(), closestPart.getName());
                            allEmpty = false;
                        } else {
                            slotPrpToPartSkuMap.put(slotOffset.getPrpName(), "empty");
                        }
                    }
                    kitToCheckStrings.add("(add-kit-to-check " + kit.getName() + " "
                            + slotPrpToPartSkuMap.entrySet().stream()
                                    .map(e -> e.getKey() + "=" + e.getValue())
                                    .collect(Collectors.joining(" "))
                            + ")");
                }

                ps.println("(look-for-parts 2)");
                ps.println("(clear-kits-to-check)");
                for (String kitToCheckString : kitToCheckStrings) {
                    ps.println(kitToCheckString);
                }
                ps.println("(check-kits)");
                ps.println("(clear-kits-to-check)");
                ps.println("(end-program)");
            }
            if (allEmpty) {
                System.out.println("itemsList = " + itemsList);
                System.out.println("kitTrays = " + kitTrays);
                System.out.println("requiredItemsString = " + requiredItemsString);
                if (JOptionPane.YES_OPTION
                        != JOptionPane.showConfirmDialog(this, "Create action list with all trays empty?")) {
                    setTitleErrorString("createActionListFromVision: All kit trays empty");
                    throw new IllegalStateException("All kit trays empty");
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

    public List<PhysicalItem> getSimviewItems() {
        if (null != object2DViewJInternalFrame) {
            return object2DViewJInternalFrame.getItems();
        }
        throw new IllegalStateException("object2DViewJInternalFrame is null");
    }

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

    private volatile XFuture<Boolean> lastResumeFuture = null;

    public void resume() {
        if (jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(false);
        }
        notifyPauseFutures();
        String methodName = "resume";
        takeSnapshots(methodName);
//        if(null != pendantClientJInternalFrame
//                && pendantClientJInternalFrame.isPaused()
//                && pendantClientJInternalFrame.isRunningProgram() ) {
//            return continueCrclProgram();
//        }
//        System.err.println("Resume when not paused.");
//        if(null != pddlExecutorJInternalFrame1) {
//            return pddlExecutorJInternalFrame1.continueActionList().thenApply(x -> true);
//        }
//        lastResumeFuture = continueCrclProgram();
//        return lastResumeFuture;
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.unpauseCrclProgram();
        }
        updateTitle("", "");
    }

    public void takeSnapshots(String methodName) {
        try {
//            final String filename = getRunName() + Utils.getDateTimeString() + "_" + methodName + "_";
            takeSimViewSnapshot(createTempFile(methodName, ".PNG"), (PmCartesian) null, (String) null);
            startVisionToDbNewItemsImageSave(createTempFile(methodName + "_new_database_items", ".PNG"));
        } catch (IOException ex) {
            Logger.getLogger(PddlActionToCrclGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

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
        System.out.println("continousDemoFuture = " + continousDemoFuture);
        if (null != continousDemoFuture) {
            continousDemoFuture.printStatus(System.out);
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

    private XFuture<Void> continousDemoFuture = null;

    public XFuture<Void> startContinousDemo(boolean reverseFirst) {
        this.setReverseFlag(reverseFirst);
        return startCheckEnabled()
                .thenCompose("starContinousDemo(task=" + getTaskName() + ")",
                        x -> {
                            if (x) {
                                return startActions()
                                        .thenRun("starContinousDemo(task=" + getTaskName() + ").setReverseFlag",
                                                () -> setReverseFlag(!reverseFirst))
                                        .thenCompose("starContinousDemo(task=" + getTaskName() + ").startActions",
                                                x2 -> startActions())
                                        .thenCompose("starContinousDemo(task=" + getTaskName() + ").recurse",
                                                x2 -> x ? startContinousDemo(reverseFirst) : XFuture.completedFutureWithName("startContinousDemo.completedFutureWithName", null));
                            } else {
                                return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinousDemo.setSelected(false));
                            }
                        });
    }

    public void setReverseFlag(boolean reverseFlag) {
        setReverseFlag(reverseFlag, true);
    }

    public void setReverseFlag(boolean reverseFlag, boolean reloadSimFiles) {
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

    public boolean isPaused() {
        return jCheckBoxMenuItemPause.isSelected();
    }

    public void pause() {
        pauseInternal();
        updateTitle("", "");
    }

    private void pauseInternal() {
        if (!jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(true);
        }
        if (null != pendantClientJInternalFrame && titleErrorString != null && titleErrorString.length() > 0) {
            String lastMessage = pendantClientJInternalFrame.getLastMessage();
            System.out.println("lastMessage = " + lastMessage);
            MiddleCommandType cmd = pendantClientJInternalFrame.getCurrentProgramCommand();
            if (null != cmd) {
                String cmdString = CRCLSocket.cmdToString(cmd);
                System.out.println("cmdString = " + cmdString);
                if (null == lastMessage) {
                    lastMessage = "";
                }
                takeSnapshots("pause :" + lastMessage + ":" + cmdString);
            } else {
                if (null == lastMessage) {
                    takeSnapshots("pause");
                } else {
                    takeSnapshots("pause :" + lastMessage);
                }
            }
        }
        this.pauseCrclProgram();
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean reloadSimFiles) {
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
//        MessageType msgCmd = new MessageType();
//        msgCmd.setMessage("empty program");
//        msgCmd.setCommandID(BigInteger.valueOf(2));
//        prog.getMiddleCommand().add(msgCmd);
        prog.setEndCanon(new EndCanonType());
        setCommandID(prog.getEndCanon());
        return prog;
    }

    public long incrementAndGetCommandId() {
        if (null != this.pddlExecutorJInternalFrame1) {
            return this.pddlExecutorJInternalFrame1.incrementAndGetCommandId();
        } else {
            return System.currentTimeMillis();
        }
    }

    public void clearErrors() {
        this.titleErrorString = null;
        clearCrclClientErrorMessage();
        updateTitle();
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setErrorString(null);
        }
    }

    public void clearCrclClientErrorMessage() {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.clearCrclClientErrorMessage();
        }
    }

    private final AtomicInteger checkEnabledCount = new AtomicInteger();

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
        try {
            System.out.println("startCheckEnabled called.");
            if (!isConnected()) {
                setConnected(true);
            }
            CRCLProgramType emptyProgram = createEmptyProgram();
            setCommandID(emptyProgram.getInitCanon());
            setCommandID(emptyProgram.getEndCanon());
            emptyProgram.setName("checkEnabled." + checkEnabledCount.incrementAndGet());
            return startCRCLProgram(emptyProgram)
                    .thenApply("startCheckEnabled.finish." + robotName + "." + taskName,
                            x -> {
                                System.out.println("startCheckEnabled finishing with " + x);
                                return x;
                            });
        } catch (JAXBException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private final ConcurrentLinkedDeque<XFuture<Boolean>> dbConnectedWaiters = new ConcurrentLinkedDeque<>();

    private XFuture<Boolean> waitDbConnected() {
        XFuture<Boolean> f = new XFuture<>("waitDbConnected");
        dbConnectedWaiters.add(f);
        return f;
    }

    private volatile XFuture<Boolean> lastStartActionsFuture = null;

    public XFuture<Boolean> getLastStartActionsFuture() {
        return lastStartActionsFuture;
    }

    public XFuture<Void> getContinueActionListFuture() {
        return lastContinueActionListFuture;
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
     * @return future of the underlying task to execute the actions.
     */
    public XFuture<Boolean> startActions() {
        runNumber++;
        assert (null != pddlExecutorJInternalFrame1) : "null == pddlExecutorJInternalFrame1";
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(jCheckBoxMenuItemReloadSimFilesOnReverse.isSelected());
        }
        if (null != motomanCrclServerJInternalFrame) {
            if (this.getRobotName().toUpperCase().contains("MOTOMAN")) {
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
        lastStartActionsFuture = XFuture.supplyAsync("AprsJFrame.startActions",
                () -> {
                    setThreadName();
                    takeSnapshots("startActions");
                    return null;
                }, runProgramService)
                .thenCompose("startActions.pauseCheck", x -> waitForPause())
                .thenApply(taskName, x -> {
                    return pddlExecutorJInternalFrame1.doActions();
                }
                );
        return lastStartActionsFuture;
    }

    public void showKitInspection() {

    }

    private void startExploreGraphDb() {
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

    public int getCrclProgramLine() {
        return this.pendantClientJInternalFrame.getCurrentProgramLine();
    }

    private File propertiesFile;
    private File propertiesDirectory;
    public static final String APRS_DEFAULT_PROPERTIES_FILENAME = "aprs_properties.txt";
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
                tempPropDir.mkdirs();
                String aprsPropsFilename = System.getProperty("aprs.properties.filename", APRS_DEFAULT_PROPERTIES_FILENAME);
                tempPropFile = new File(tempPropDir, aprsPropsFilename);
            }
            this.propDir = tempPropDir;
            this.propFile = tempPropFile;
        }
    }

    public static File getDefaultPropertiesDir() {
        return AprsJFramePropDefaults.getSingle().getPropDir();
    }

    public static File getDefaultPropertiesFile() {
        return AprsJFramePropDefaults.getSingle().getPropFile();
    }

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
    public void takeSimViewSnapshot(File f, PoseType pose, String label) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, pose, label, snapShotWidth, snapShotHeight);
        }
    }

    public void takeSimViewSnapshot(File f, PointType point, String label) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, point, label, snapShotWidth, snapShotHeight);
        }
    }

    public void takeSimViewSnapshot(File f, PmCartesian point, String label) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(f, point, label, snapShotWidth, snapShotHeight);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pose, poseLabel, snapShotWidth, snapShotHeight);
        }
    }

    public void takeSimViewSnapshot(String imgLabel, PmCartesian pt, String pointLabel) throws IOException {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), pt, pointLabel, snapShotWidth, snapShotHeight);
        }
    }

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
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        if (null != object2DViewJInternalFrame && jCheckBoxMenuItemSnapshotImageSize.isSelected()) {
            this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint, snapShotWidth, snapShotHeight);
        }
    }

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

    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) throws IOException {
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.takeSnapshot(createTempFile(imgLabel, ".PNG"), itemsToPaint, w, h);
        }
    }

    DbSetup dbSetup = null;

    @Override
    public final void loadProperties() throws IOException {
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
        this.updateSubPropertiesFiles();
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.loadProperties();
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.loadProperties();
        }

        if (null != this.object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.loadProperties();
        }
        dbSetup = DbSetupBuilder.loadFromPropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt")).build();

        if (null != dbSetupJInternalFrame) {
            DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
            }
        }
        if (null != visionToDbJInternalFrame) {
            this.visionToDbJInternalFrame.loadProperties();
//            DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
//            if (null != pub) {
//                pub.setDbSetup(dbSetup);
//            }
        }
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.loadProperties();
        }
        if (null != this.pendantClientJInternalFrame) {
            pendantClientJInternalFrame.loadProperties();
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
            activeWin = ACTIVE_WINDOW_NAME.valueOf(startupActiveWinString);
            switch (activeWin) {
                case SIMVIEW_WINDOW:
                    if (null != object2DViewJInternalFrame) {
                        activateFrame(object2DViewJInternalFrame);
                    }
                    break;

                case CRCL_CLIENT_WINDOW:
                    if (null != pendantClientJInternalFrame) {
                        activateFrame(pendantClientJInternalFrame);
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
        propsMap.put(APRSROBOT_PROPERTY_NAME, robotName);
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
        if (null != this.pendantClientJInternalFrame) {
            pendantClientJInternalFrame.saveProperties();
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
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinousDemo;
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
        propertiesDirectory = propertiesFile.getParentFile();
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
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_crclPendantClientProperties.txt"));
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

    Future disconnectDatabaseFuture = null;

    private void startDisconnectDatabase() {
        jCheckBoxMenuItemConnectDatabase.setEnabled(false);
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        disconnectDatabaseFuture = connectService.submit(this::disconnectDatabase);
    }

    private void disconnectDatabase() {

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
    public void close() throws Exception {
        closing = true;
        System.out.println("AprsJFrame.close()");
        closeAllWindows();
        connectService.shutdownNow();
        connectService.awaitTermination(100, TimeUnit.MILLISECONDS);
        this.setVisible(false);
        this.dispose();
    }

    private void closeActionsToCrclJInternalFrame() throws Exception {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.close();
            pddlExecutorJInternalFrame1.setVisible(false);
            pddlExecutorJInternalFrame1.dispose();
            pddlExecutorJInternalFrame1 = null;
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
            }
        }
    }

    private void closePddlPlanner() throws Exception {
        if (null != pddlPlannerJInternalFrame) {
            pddlPlannerJInternalFrame.close();
            pddlPlannerJInternalFrame.setVisible(false);
            pddlPlannerJInternalFrame.dispose();
            pddlPlannerJInternalFrame = null;
        }
    }

    private volatile boolean lastContinueCrclProgramResult = false;

//    private Boolean completeCrclProgramContinuation(Boolean ok) {
//        lastContinueCrclProgramResult = ok;
//        if (ok) {
//            pddlExecutorJInternalFrame1.checkSafeAbort(() -> null);
//        }
//        return ok;
//    }
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
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.continueCurrentProgram();
//                    .thenApply(this::completeCrclProgramContinuation);
        } else {
            return XFuture.completedFuture(false);
        }
    }

    public boolean isEnableVisionToDatabaseUpdates() {
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
        visionToDbJInternalFrame.setEnableDatabaseUpdates(enableDatabaseUpdates, requiredParts);
        if (enableDatabaseUpdates && null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }

    public File getlogFileDir() {
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

    public File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, getlogFileDir());
    }

    public File createTempFile(String prefix, String suffix, File dir) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, dir);
    }

    @Override
    public String toString() {
        return super.toString() + getTitle();
    }

}
