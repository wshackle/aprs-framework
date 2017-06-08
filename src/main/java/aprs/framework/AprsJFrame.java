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
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.DbType;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.explore.ExploreGraphDbJInternalFrame;
import aprs.framework.kitinspection.KitInspectionJInternalFrame;
import aprs.framework.logdisplay.LogDisplayJInternalFrame;
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
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import crcl.ui.client.PendantClientJInternalFrame;
import crcl.ui.client.PendantClientJPanel;
import crcl.ui.client.UpdateTitleListener;
import crcl.ui.server.SimServerJInternalFrame;
import crcl.utils.CRCLException;
import crcl.utils.CRCLSocket;
import java.awt.Container;
import java.awt.HeadlessException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

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

    public XFuture<List<DetectedItem>> getNextUpdate() {
        if(null == visionToDbJInternalFrame) {
            throw new IllegalStateException("null == visionToDbJInternalFrame");
        }
        return visionToDbJInternalFrame.getNextUpdate();
    }
    
    public void refreshSimView() {
        if(null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }
    
    public List<DetectedItem> getSlotOffsets(String name) {
        if (null == visionToDbJInternalFrame) {
            throw new IllegalStateException("visionToDbJInternalFrame == null");
        }
        return this.visionToDbJInternalFrame.getSlotOffsets(name);
    }

    public XFuture<Void> startVisionToDbNewItemsImageSave(File f) {
        if (null == visionToDbJInternalFrame) {
            throw new IllegalStateException("visionToDbJInternalFrame == null");
        }
        return this.visionToDbJInternalFrame.startNewItemsImageSave(f);
    }

    public List<DetectedItem> getSlots(DetectedItem item) {
        if (null == visionToDbJInternalFrame) {
            throw new IllegalStateException("visionToDbJInternalFrame == null");
        }
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
        safeAbortFuture = this.pddlExecutorJInternalFrame1.startSafeAbort()
                .thenRun(() -> {
                    if (null != continousDemoFuture) {
                        continousDemoFuture.cancelAll(true);
                        continousDemoFuture = null;
                    }
                });
        takeSnapshots("startSafeAbort");
        return safeAbortFuture;
    }

    private volatile XFuture<Void> startSafeAbortAndDisconnectAsyncFuture = null;

    /**
     * Safely abort the current CRCL program and then disconnect from the
     * robot's CRCL server.
     *
     * The abort will occur asynchronously in another thread after this method
     * returns. The status of this action can be monitored with the returned
     * future. * @return a future that can be tested or used to wait until the
     * abort and disconnect is completed.
     */
    public XFuture<Void> startSafeAbortAndDisconnectAsync() {
        startSafeAbortAndDisconnectAsyncFuture
                = this.pddlExecutorJInternalFrame1.startSafeAbort()
                        .thenRunAsync(this::disconnectRobot);
        return startSafeAbortAndDisconnectAsyncFuture;
    }

    /**
     * Disconnect from the robot's crcl server and set robotName to null.
     *
     * Note: setConnected(false) also disconnects from the crcl server but
     * leaves the robotName unchanged.
     */
    public void disconnectRobot() {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.disconnect();
        }
        takeSnapshots("disconnectRobot");
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
    public void connectRobot(String robotName, String host, int port) {
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

    /**
     * Continue or start executing the currently loaded set of PDDL actions.
     *
     * The actions will be executed in another thread after this method returns.
     * The task can be monitored or canceled using the returned future.
     *
     * @return a future that can be used to monitor, extend or cancel the
     * underlying task.
     *
     */
    public XFuture<Void> continueActionList() {
        if (jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(false);
        }
        takeSnapshots("continueActionList");
        return pddlExecutorJInternalFrame1.continueActionList();
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

    /**
     * Set the value of robotName
     *
     * @param robotName new value of robotName
     */
    public void setRobotName(String robotName) {
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
            pendantClientJInternalFrame.setProgram(program);
            lastRunProgramFuture = pendantClientJInternalFrame.runCurrentProgram();
            return lastRunProgramFuture;
        }
        XFuture<Boolean> ret = new XFuture<>("startCRCLProgram");
        ret.complete(false);
        return ret;
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
        jCheckBoxMenuItemPause.setSelected(false);
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
        }
        sb.append("robotCrclPort=").append(this.getRobotCrclPort()).append(", ");
        boolean connected = (null != pendantClientJInternalFrame && pendantClientJInternalFrame.isConnected());
        sb.append("connected=").append(connected).append(", ");
        sb.append("reverseFlag=").append(jCheckBoxMenuItemReverse.isSelected()).append(", ");
        sb.append("paused=").append(jCheckBoxMenuItemPause.isSelected()).append("\r\n");
        sb.append("run_name=").append(this.getRunName()).append("\r\n");
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

    /**
     * Set the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @param titleErrorString title error string
     */
    public void setTitleErrorString(String titleErrorString) {
        if (!Objects.equals(this.titleErrorString, titleErrorString)) {
            updateTitle();
            this.titleErrorString = titleErrorString;
            setTitle((null != titleErrorString) ? titleErrorString : "APRS");
            if (null != titleErrorString && titleErrorString.length() > 0) {
                System.err.println(titleErrorString);
                pause();
            }
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
        }
    }

    private void addInternalFrame(JInternalFrame internalFrame) {
        internalFrame.pack();
        internalFrame.setVisible(true);
        jDesktopPane1.add(internalFrame, JLayeredPane.DEFAULT_LAYER);
        internalFrame.getDesktopPane().getDesktopManager().maximizeFrame(internalFrame);
        setupWindowsMenu();
    }

    private ExecutorService connectService = Executors.newSingleThreadExecutor();
    private Future connectDatabaseFuture = null;

    private void startConnectDatabase() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect database when already closing.");
        }
        System.out.println("Starting connect to database ...");
        jCheckBoxMenuItemConnectDatabase.setSelected(true);
        jCheckBoxMenuItemConnectDatabase.setEnabled(true);
        DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
        DbSetup setup = dbSetupPublisher.getDbSetup();
        if (setup.getDbType() == null || setup.getDbType() == DbType.NONE) {
            throw new IllegalStateException("Can not connect to database with setup.getDbType() = " + setup.getDbType());
        }
        connectDatabaseFuture = connectService.submit(this::connectDatabase);
    }

    private void connectDatabase() {
        List<Future<?>> futures = null;
        try {
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            DbSetup setup = dbSetupPublisher.getDbSetup();
            if (setup.getDbType() == null || setup.getDbType() == DbType.NONE) {
                throw new IllegalStateException("Can not connect to database with setup.getDbType() = " + setup.getDbType());
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

    private void closeAllWindows() {
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
        if (framesListSize != menuItems.size()) {
            throw new IllegalStateException("menuItems = " + menuItems + " does not match framesList = " + framesList);
        }
        int menuItemCount = jMenuWindow.getItemCount();
        if (framesListSize != menuItemCount) {
            throw new IllegalStateException("framesListSize = " + framesListSize + " does not match menuItemCount = " + menuItemCount
                    + "with framesList=" + framesList + ", menuItems=" + menuItems);
        }
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
        if(isPaused()) {
            stateString = "PAUSED";
        }
        String newTitle = "APRS : " + ((robotName != null) ? robotName : "NO Robot") + " : " + ((taskName != null) ? taskName : "NO Task") + " : " + stateString + " : "
                + stateDescription
                + ((titleErrorString != null) ? ": " + titleErrorString : "");
        if (newTitle.length() > 70) {
            newTitle = newTitle.substring(0, 70) + " ... ";
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
        if (null != pendantClientJInternalFrame) {
            CommandStatusType cs = pendantClientJInternalFrame.getCurrentStatus()
                    .map(x -> x.getCommandStatus())
                    .orElse(null);
            if (null == cs || null == cs.getCommandState()) {
                updateTitle("", "");
            } else {
                if (cs.getCommandState() == CommandStateEnumType.CRCL_DONE) {
                    titleErrorString = null;
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

    private void updateDbConnectedCheckBox(DbSetup setup) {
        jCheckBoxMenuItemConnectDatabase.setSelected(setup.isConnected());
    }

    private void createDbSetupFrame() {
        if (null == dbSetupJInternalFrame) {
            dbSetupJInternalFrame = new DbSetupJInternalFrame();
            dbSetupJInternalFrame.setAprsJframe(this);
            dbSetupJInternalFrame.pack();
            dbSetupJInternalFrame.loadRecent();
            jDesktopPane1.add(dbSetupJInternalFrame, JLayeredPane.DEFAULT_LAYER);
            dbSetupJInternalFrame.getDbSetupPublisher().addDbSetupListener(this::updateDbConnectedCheckBox);
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
        jMenuExecute = new javax.swing.JMenu();
        jMenuItemStartActionList = new javax.swing.JMenuItem();
        jMenuItemImmediateAbort = new javax.swing.JMenuItem();
        jMenuItemReset = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jMenuItemDebugAction = new javax.swing.JMenuItem();
        jCheckBoxMenuItemForceFakeTake = new javax.swing.JCheckBoxMenuItem();

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
        jCheckBoxMenuItemPause.setSelected(false);
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
        immediateAbort();
        setReverseFlag(false);
        if (jCheckBoxMenuItemContinousDemo.isSelected()) {
            continousDemoFuture = startContinousDemo();
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
        if(null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.unpauseCrclProgram();
        }
        updateTitle("", "");
    }

    public void takeSnapshots(String methodName) {
        try {
//            final String filename = getRunName() + Utils.getDateTimeString() + "_" + methodName + "_";
            takeSimViewSnapshot(createTempFile(methodName, ".PNG"), null, null);
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
        System.out.println("lastResumeFuture = " + lastRunProgramFuture);
        if (null != lastRunProgramFuture) {
            lastRunProgramFuture.printStatus(System.out);
        }
        System.out.println("startSafeAbortAndDisconnectAsyncFuture = " + startSafeAbortAndDisconnectAsyncFuture);
        if (null != startSafeAbortAndDisconnectAsyncFuture) {
            startSafeAbortAndDisconnectAsyncFuture.printStatus(System.out);
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
    }

    private XFuture<Void> continousDemoFuture = null;

    public XFuture<Void> startContinousDemo() {
        this.setReverseFlag(false);
        return startCheckEnabled()
                .thenCompose(x -> {
                    if (x) {
                        return startActions()
                                .thenRun(() -> setReverseFlag(true))
                                .thenCompose(x2 -> startActions())
                                .thenCompose(x2 -> x ? startContinousDemo() : XFuture.completedFutureWithName("startContinousDemo.completedFutureWithName", null));
                    } else {
                        return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinousDemo.setSelected(false));
                    }
                });
    }

    public void setReverseFlag(boolean reverseFlag) {
        if (jCheckBoxMenuItemReverse.isSelected() != reverseFlag) {
            jCheckBoxMenuItemReverse.setSelected(reverseFlag);
        }
        if (null != object2DViewJInternalFrame) {
            try {
                object2DViewJInternalFrame.setReverseFlag(reverseFlag);
                object2DViewJInternalFrame.reloadDataFile();
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
        takeSnapshots("pause");
        this.pauseCrclProgram();
    }

    public void reset() {
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(true);
        }
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.refresh();
        }
    }

    private CRCLProgramType createEmptyProgram() {
        CRCLProgramType prog = new CRCLProgramType();
        prog.setInitCanon(new InitCanonType());
        prog.getInitCanon().setCommandID(incrementAndGetCommandId());
        prog.getMiddleCommand().clear();
//        MessageType msgCmd = new MessageType();
//        msgCmd.setMessage("empty program");
//        msgCmd.setCommandID(BigInteger.valueOf(2));
//        prog.getMiddleCommand().add(msgCmd);
        prog.setEndCanon(new EndCanonType());
        prog.getEndCanon().setCommandID(incrementAndGetCommandId());
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
            emptyProgram.getInitCanon().setCommandID(incrementAndGetCommandId());
            emptyProgram.getEndCanon().setCommandID(incrementAndGetCommandId());
            return startCRCLProgram(emptyProgram)
                    .thenApply(x -> {
                        System.out.println("startCheckEnabled finishing with " + x);
                        return x;
                    });
        } catch (JAXBException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            return XFuture.completedFuture(false);
        }
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
        if (jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(false);
        }
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(true);
        }
        takeSnapshots("startActions");
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.refresh();
            return pddlExecutorJInternalFrame1.startActions();
        } else {
            return XFuture.completedFuture(false);
        }
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

//    private final DbSetupListener toVisListener = new DbSetupListener() {
//        @Override
//        public void accept(DbSetup setup) {
//            Utils.runOnDispatchThread(() -> {
//                dbSetup = setup;
//                if (null != visionToDbJInternalFrame) {
//                    DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
//                    publishDbSetup(pub, setup);
//                }
//            });
//        }
//    };
//    private final DbSetupListener toDbListener = new DbSetupListener() {
//        @Override
//        public void accept(DbSetup setup) {
//            Utils.runOnDispatchThread(() -> {
//                dbSetup = setup;
//                if (null != dbSetupJInternalFrame) {
//                    DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
//                    publishDbSetup(pub, setup);
//                }
//            });
//        }
//
//    };
    private void publishDbSetup(DbSetupPublisher pub, DbSetup setup) {
        if (null != pub && setup.getDbType() != DbType.NONE && setup.getDbType() != null) {
//            saveDbSetup();
            pub.setDbSetup(setup);
        }
    }

//    private void saveDbSetup() {
//        DbSetupBuilder.savePropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt"), dbSetup);
//    }
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
        if (null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.takeSnapshot(f, pose, label);
        }
    }

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, List<DetectedItem> itemsToPaint) throws IOException {
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.takeSnapshot(f, itemsToPaint);
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
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowDatabaseSetup;
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
    private javax.swing.JMenuItem jMenuItemDebugAction;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemImmediateAbort;
    private javax.swing.JMenuItem jMenuItemLoadProperties;
    private javax.swing.JMenuItem jMenuItemLoadPropertiesFile;
    private javax.swing.JMenuItem jMenuItemReset;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSavePropsAs;
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
        jCheckBoxMenuItemConnectDatabase.setSelected(false);
        jCheckBoxMenuItemConnectDatabase.setEnabled(false);
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        disconnectDatabaseFuture = connectService.submit(this::disconnectDatabase);
    }

    private void disconnectDatabase() {

        try {
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

    private Boolean completeCrclProgramContinuation(Boolean ok) {
        lastContinueCrclProgramResult = ok;
        if (ok) {
            pddlExecutorJInternalFrame1.checkSafeAbort(() -> null);
        }
        return ok;
    }

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
     */
    public void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        visionToDbJInternalFrame.setEnableDatabaseUpdates(enableDatabaseUpdates, requiredParts);
        if (enableDatabaseUpdates && null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh(false);
        }
    }
    
    
    
    public File getlogFileDir() {
        File f =   new File(Utils.getlogFileDir(),getRunName());
        f.mkdirs();
        return f;
    }

    public File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(Utils.getTimeString()+"_"+prefix, suffix, getlogFileDir());
    }

    public File createTempFile(String prefix, String suffix, File dir) throws IOException {
        return File.createTempFile(prefix, suffix, dir);
    }


}
