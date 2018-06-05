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
package aprs.system;

import aprs.misc.DisplayInterface;
import aprs.misc.SlotOffsetProvider;
import aprs.misc.ActiveWinEnum;
import aprs.actions.executor.Action;
import aprs.database.PhysicalItem;
import aprs.kitinspection.KitInspectionJInternalFrame;
import aprs.database.PartsTray;
import aprs.database.Slot;
import aprs.database.Tray;
import aprs.learninggoals.GoalLearner;
import aprs.actions.executor.CrclGenerator.PoseProvider;
import aprs.actions.executor.PositionMap;
import aprs.database.vision.UpdateResults;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import crcl.base.CRCLProgramType;
import crcl.base.CRCLStatusType;
import crcl.base.MiddleCommandType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import crcl.ui.client.PendantClientInner;
import crcl.ui.client.PendantClientJPanel;
import crcl.utils.outer.interfaces.ProgramRunData;
import java.awt.Image;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import javax.xml.bind.JAXBException;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;
import java.awt.image.BufferedImage;

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
@SuppressWarnings("UnusedReturnValue")
public interface AprsSystemInterface extends DisplayInterface, AutoCloseable, SlotOffsetProvider {

    /**
     * Check if the user has selected a check box asking for snapshot files to
     * be created for logging/debugging.
     *
     * @return Has user enabled snapshots?
     */
    public boolean isSnapshotsEnabled();

    public void setSnapshotsEnabled(boolean enable);

    /**
     * Get the value of origRobotName
     *
     * @return the value of origRobotName
     */
    public String getOrigRobotName();

    /**
     * Get the most recent image created when scanning for desired part
     * locations.
     *
     * @return image of most recent scan
     */
    public Image getScanImage();

    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    @Nullable public PoseProvider getPddlExecExternalPoseProvider();

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalPoseProvider new value of externalGetPoseFunction
     */
    public void setPddlExecExternalPoseProvider(PoseProvider externalPoseProvider);

    /**
     * Get the duration in milliseconds that this system has been in the run
     * state.
     *
     * @return duration in milliseconds or 0 if it has not been started and
     * stopped atleast once.
     */
    public long getRunDuration();

    /**
     * Get the duration in milliseconds that this system has been in the stopped
     * state.
     *
     * @return duration in milliseconds or 0 if it has not been started and
     * stopped atleast once.
     */
    public long getStopDuration();

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system. The list will not be available until after the next frame
     * is received from vision.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    public XFuture<List<PhysicalItem>> getSingleVisionToDbUpdate();

    public long getLastSingleVisionToDbUpdateTime();

    public long getSingleVisionToDbNotifySingleUpdateListenersTime();

    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    public List<PartsTray> getPartsTrayList();

    /**
     * Force the Object 2D Simulation View to refresh, this method has no effect
     * if the view is not visible or is not in simulation mode.
     */
    public void refreshSimView();

    public long getLastSimViewRefreshTime();

    public int getSimViewRefreshCount();

    public long getSimViewLastPublishTime();

    public int getSimViewPublishCount();

    /**
     * Get the value of externalSlotOffsetProvider
     *
     * @return the value of externalSlotOffsetProvider
     */
    @Nullable
    public SlotOffsetProvider getExternalSlotOffsetProvider();

    /**
     * Set the value of externalSlotOffsetProvider
     *
     * @param externalSlotOffsetProvider new value of externalSlotOffsetProvider
     */
    public void setExternalSlotOffsetProvider(SlotOffsetProvider externalSlotOffsetProvider);

    /**
     * Save an image showing the locations of new database items. The database
     * will be queried and the file saved asynchronously in another thread.
     *
     * @param f file to save
     * @return future with information on when the operation is complete.
     */
    public XFuture<Void> startVisionToDbNewItemsImageSave(File f);

    /**
     * Get a list of slots associated with a particular tray.
     *
     * @param tray tray to obtain list of slots
     * @return list of slots
     */
    public List<Slot> getSlots(Tray tray, boolean ignoreEmpty);

    /**
     * Set the preference for displaying a '+' at the current position of the
     * robot tool.
     *
     * @param v should the position be displayed.
     */
    public void setSimViewTrackCurrentPos(boolean v);

    /**
     * Set the simulation view to simulation mode and disconnect from any vision
     * data socket.
     *
     */
    public void simViewSimulateAndDisconnect();

    /**
     * Get a rotational offset in radians between the vision system and the
     * database coordinate system.
     *
     * @return rotational offset in radians
     */
    public double getVisionToDBRotationOffset();

    /**
     * Pause a currently executing CRCL program.
     *
     * No action is taken if the CRCL Client has not been started and connected
     * or not program was currently running.
     */
    public void pauseCrclProgram();

    /**
     * Get the Kit Inspection InternalJFrame if it has been created.
     *
     * @return Kit Inspection InternalJFrame
     */
    @Nullable
    public KitInspectionJInternalFrame getKitInspectionJInternalFrame();

    /**
     * Return the user's preference on whether the stack trace be dumped for
     * more verbose logging.
     *
     * @return current setting of menu item
     */
    public boolean isEnableDebugDumpstacks();

    /**
     * Creates a run name useful for identifying a run in log files or the names
     * of snapshot files. The name is composed of the current robot name, task
     * name and robot number.
     *
     * @return current run name.
     */
    public String getRunName();

    /**
     * Checks whether there is currently a CRCL program that is loaded but
     * paused.
     *
     * @return whether a program is paused
     */
    public boolean isCrclProgramPaused();

    /**
     * Get the thread currently running a CRCL program if it exists and is
     * known. Only useful for debugging.
     *
     * @return thread
     */
    @Nullable
    public Thread getCrclRunProgramThread();

    /**
     * Get the future that can be used to determine when the current CRCL
     * program is finished. Only useful for debugging.
     *
     * @return future or null if no program is running
     */
    @Nullable
    public XFuture<Boolean> getCrclRunProgramFuture();

    /**
     * Checks whether there is currently a CRCL program running.
     *
     * @return whether a CRCL program is running.
     */
    public boolean isRunningCrclProgram();

    /**
     * Set a state where attempts to start a crcl program will be blocked.
     *
     * @return count to pass to stopBlockingCrclProgram when running crcl
     * programs should again be allowed.
     */
    public int startBlockingCrclPrograms();

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
    public void stopBlockingCrclPrograms(int count) throws PendantClientInner.ConcurrentBlockProgramsException;

    /**
     * Get the currently loaded CRCL program.
     *
     * @return current CRCL program or null if no program is loaded.
     */
    @Nullable
    public CRCLProgramType getCrclProgram();

    /**
     * Get the executor options as a map of names to values.
     *
     * @return the current executor options.
     */
    public Map<String, String> getExecutorOptions();

    /**
     * Set an executor option.
     *
     * @param key name of option to set
     * @param value value option should be set to
     */
    public void setExecutorOption(String key, String value);

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
    public void addPositionMap(PositionMap pm);

    /**
     * Remove a previously added position map.
     *
     * @param pm position map to be removed.
     */
    public void removePositionMap(PositionMap pm);

    /**
     * Get the current pose of the robot.
     *
     * @return current pose of the robot.
     */
    @Nullable
    public PoseType getCurrentPose();

    /**
     * Get the current point(translation only) from current pose of the robot.
     *
     * @return current point(translation only) from current pose of the robot.
     */
    @Nullable
    public PointType getCurrentPosePoint();

    /**
     * Check if the CRCL client is connected to the CRCL Server and therefore to
     * the robot.
     *
     * @return if the CRCL client is connected to the server.
     */
    public boolean isConnected();

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
    public void setConnected(boolean connected);

    /**
     * Get the value of taskName
     *
     * @return the value of taskName
     */
    public String getTaskName();

    /**
     * Get the future that can be used to determine when the last requested safe
     * abort is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    @Nullable
    public XFuture<Void> getSafeAbortFuture();

    /**
     * Get the future that can be used to determine when the last requested run
     * program is finished. Only useful for debugging.
     *
     * @return future or null if no safe abort requested
     */
    @Nullable
    public XFuture<Boolean> getLastRunProgramFuture();

    /**
     * Get the future that can be used to determine when the last requested
     * resumed action is finished. Only useful for debugging.
     *
     * @return future or null if no resume requested
     */
    @Nullable
    public XFuture<Boolean> getLastResumeFuture();

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
    public XFuture<Void> startSafeAbort(String comment);

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
    public XFuture<Void> startSafeAbortAndDisconnect(String comment);

    /**
     * Get a map of updates that were attempted the last time data was received
     * from the vision system.
     *
     * Only useful for debugging.
     *
     * @return map of results
     */
    public Map<String, UpdateResults> getDbUpdatesResultMap();

    /**
     * Disconnect from the robot's crcl server and set robotName to null.
     *
     * Note: setConnected(false) also disconnects from the crcl server but
     * leaves the robotName unchanged.
     *
     * @return future providing info on when complete
     */
    public XFuture<Void> disconnectRobot();

    public boolean isClosing();

    /**
     * Get the value of origCrclRobotHost
     *
     * @return the value of origCrclRobotHost
     */
    public String getOrigCrclRobotHost();

    /**
     * Get the value of origCrclRobotPort
     *
     * @return the value of origCrclRobotPort
     */
    public int getOrigCrclRobotPort();

    /**
     * Connect to a given robot with a CRCL server running on the given host and
     * TCP port.
     *
     * @param robotName name of the robot
     * @param host host running robot's CRCL server
     * @param port (TCP) port robot's CRCL server is bound to
     * @return future providing info on when complete
     */
    public XFuture<Void> connectRobot(String robotName, String host, int port);

    /**
     * Get robot's CRCL host.
     *
     * @return robot's CRCL host.
     */
    @Nullable
    public String getRobotCrclHost();

    /**
     * Get robot's CRCL port number.
     *
     * @return robot's CRCL port number.
     */
    public int getRobotCrclPort();

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
    public XFuture<Boolean> continueActionList(String comment);

    /**
     * Get the closest distance between the robot TCP and any part.
     *
     * @return closest distance
     */
    public double getClosestRobotPartDistance();

    /**
     * Get the current setting for whether the object view is using simulation.
     *
     * @return if object view is simulated
     */
    public boolean isObjectViewSimulated();

    /**
     * Set the value of taskName
     *
     * @param taskName new value of taskName
     */
    public void setTaskName(String taskName);

    /**
     * Get the value of robotName
     *
     * @return the value of robotName
     */
    @Nullable
    public String getRobotName();

    /**
     * Set the value of robotName
     *
     * @param robotName new value of robotName
     */
    public void setRobotName(@Nullable String robotName);

    public boolean getUseTeachTable();

    public void setUseTeachTable(boolean useTeachTable);

    /**
     * Register a program line listener that will be notified each time a line
     * of the CRCL program is executed.
     *
     * @param l listener to be registered
     */
    public void addProgramLineListener(PendantClientJPanel.ProgramLineListener l);

    /**
     * Remove a previously added program line listener.
     *
     * @param l listener to be removed
     */
    public void removeProgramLineListener(PendantClientJPanel.ProgramLineListener l);

    /**
     * Modify the given pose by applying all of the currently added position
     * maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PoseType correctPose(PoseType poseIn);

    /**
     * Apply inverses of currently added position maps in reverse order.
     *
     * @param ptIn pose to reverse correction
     * @return pose in original vision/database coordinates
     */
    public PointType reverseCorrectPoint(PointType ptIn);

    /**
     * Register a listener to be notified of every change in the robots pose.
     *
     * @param l listener to register
     */
    public void addCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l);

    /**
     * Remove a previously added listener.
     *
     * @param l listener to be removed
     */
    public void removeCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l);

    /**
     * Set the currently loaded CRCL program
     *
     * @param program CRCL program to load.
     * @throws JAXBException a command within the program violates a constraint
     * in the schema
     */
    public void setCRCLProgram(CRCLProgramType program) throws JAXBException;

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
    public XFuture<Boolean> startCRCLProgram(CRCLProgramType program);

    public void processWrapperCommands(List<MiddleCommandType> cmds);

    /**
     * Run a CRCL program.
     *
     * @param program CRCL program to run
     * @return whether the program completed successfully
     * @throws JAXBException the program did not meet schema requirements
     */
    public boolean runCRCLProgram(CRCLProgramType program) throws JAXBException;

    /**
     * Immediately abort the currently running CRCL program and PDDL action
     * list.
     *
     * NOTE: This may leave the robot in a state with the part held in the
     * gripper or with the robot obstructing the view of the vision system.
     *
     */
    public void immediateAbort();

    /**
     * Create a string with current details for display and/or logging.
     *
     * @return details string
     */
    public String getDetailsString();

    /**
     * Abort the current CRCL program.
     */
    public void abortCrclProgram();

    /**
     * Get the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @return title error string
     */
    @Nullable
    public String getTitleErrorString();

    /**
     * Get the stack trace the last time the title error was set.
     *
     * @return stack trace or null if no error has been set.
     */
    public StackTraceElement @Nullable [] getSetTitleErrorStringTrace();

    /**
     * Set the title error string, which should be a short string identifying
     * the most critical problem if there is one appropriate for displaying in
     * the title.
     *
     * @param newTitleErrorString title error string
     */
    public void setTitleErrorString(@Nullable String newTitleErrorString);

    public long logEvent(String s, @Nullable Object arg);

    /**
     * Get the unique thread id number for this system. It is set only when this
     * object is created.
     *
     * @return thread id
     */
    public int getMyThreadId();

    /**
     * Get the ExecutorService used for running CRCL programs.
     *
     * @return run program service
     */
    public ExecutorService getRunProgramService();

    /**
     * Close all internal windows.
     */
    public void closeAllWindows();


    /**
     * Get a list of items with names and poses from the simulation.
     *
     * @return list of items as generated by the simulation
     *
     * @throws IllegalStateException Object 2D view was not opened.
     *
     */
    public List<PhysicalItem> getSimItemsData() throws IllegalStateException;

    /**
     * Set a list of items with names and poses to be used by the simulation.
     *
     * This has no effect if the Object 2D view has not been opened or it is not
     * in simulation mode.
     *
     * @param items list of items to use
     */
    public void setSimItemsData(List<PhysicalItem> items);

    /**
     * Set the position the robot tool should be moved to ensure the robot is no
     * longer obstructing the vision systems view of the parts and trays.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     */
    public void setLookForXYZ(double x, double y, double z);

    /**
     * Set limits on the area that should be visible in the Object 2D view.
     *
     * @param minX minimum X
     * @param minY minimum Y
     * @param maxX maximum X
     * @param maxY maximum Y
     */
    public void setViewLimits(double minX, double minY, double maxX, double maxY);

    /**
     * Make the Object 2D view visible and create underlying components.
     */
    public void startObject2DJinternalFrame();
    
    /**
     * Get the value of titleUpdateRunnables
     *
     * @return the value of titleUpdateRunnables
     */
    public List<Runnable> getTitleUpdateRunnables();

    /**
     * Get the error string associated with the current CRCL client if one.
     *
     * @return error string or null if no error has occurred.
     */
    @Nullable
    public String getCrclClientErrorString();

    /**
     * Is this system in the process of aborting?
     *
     * @return aborting status
     */
    public boolean isAborting();

    /**
     * Get the current status if available
     *
     * @return current status or null
     */
    @Nullable
    public CRCLStatusType getCurrentStatus();

    /**
     * Update the title based on the current state.
     */
    public void updateTitle();

    /**
     * Create and display the CRCL client frame (aka pendant client)
     */
    public void startCrclClientJInternalFrame();

    /**
     * Create and display the simulation server window and start the simulation
     * server thread.
     */
    public void startSimServerJInternalFrame();
    
    /**
     * Set the menu checkbox item to reflect the val of the whether the vision
     * system is connected. This will not cause the system to connect/disconnect
     * only to show the state the caller already knows.
     *
     * @param val of vision systems connected status to show
     */
    public void setShowVisionConnected(boolean val);

    /**
     * Start the PDDL Executor (aka Actions to CRCL) and create and display the
     * window for displaying its output.
     */
    public void startActionListExecutor();

    /**
     * Query the user to select a properties file to open.
     */
    public void browseOpenPropertiesFile();

    /**
     * Query the user to select a properties file to save.
     */
    public void browseSavePropertiesFileAs();


    public void forceClose();

    /**
     * Start a sequence of actions to move the robot out of the way so the
     * vision system can see the parts and wait for the vision system to provide
     * data and the database to be updated. It will happen asynchronously in
     * another thread.
     *
     * @return future allowing the caller to determine when the actions are
     * complete.
     */
    public XFuture<Boolean> startLookForParts();

       /**
     * Get the value of minLimit
     *
     * @return the value of minLimit
     */
    public PmCartesian getMinLimit();

    /**
     * Set the value of minLimit
     *
     * @param minLimit new value of minLimit
     */
    public void setMinLimit(PmCartesian minLimit);

   /**
     * Get the value of maxLimit
     *
     * @return the value of maxLimit
     */
    public PmCartesian getMaxLimit();

    /**
     * Set the value of maxLimit
     *
     * @param maxLimit new value of maxLimit
     */
    public void setMaxLimit(PmCartesian maxLimit);

    /**
     * Get a list of items seen by the vision system or simulated in the
     * Object2D view and create a set of actions that will fill empty trays to
     * match. Load this list into the PDDL executor.
     *
     */
    public void createActionListFromVision();

    @Nullable
    public BufferedImage getLiveImage();


    /**
     * Get the value of goalLearner
     *
     * @return the value of goalLearner
     */
    public GoalLearner getGoalLearner();

    /**
     * Set the value of goalLearner
     *
     * @param goalLearner new value of goalLearner
     */
    public void setGoalLearner(GoalLearner goalLearner);


    public List<String> getLastCreateActionListFromVisionKitToCheckStrings();

    public void setLastCreateActionListFromVisionKitToCheckStrings(List<String> strings);

    /**
     * Get the value of correctionMode
     *
     * @return the value of correctionMode
     */
    public boolean isCorrectionMode();

    /**
     * Set the value of correctionMode
     *
     * @param correctionMode new value of correctionMode
     */
    public void setCorrectionMode(boolean correctionMode);

    /**
     * Use the provided list of items create a set of actions that will fill
     * empty trays to match.Load this list into the PDDL executor.
     *
     * @param requiredItems list of items that have to be seen before the robot
     * can begin
     * @param teachItems list of trays and items in the trays as they should be
     * when complete.
     */
    public void createActionListFromVision(List<PhysicalItem> requiredItems, List<PhysicalItem> teachItems, boolean overrideRotation, double newRotationOffsetParam);

    /**
     * Get the list of items displayed in the Object 2D view, they may be
     * simulated or received from the vision system.
     *
     * @return list of items displayed in the Object 2D view
     */
    public List<PhysicalItem> getObjectViewItems();

    /**
     * Set the menu checkbox setting to force take operations to be faked so
     * that the gripper will not close, useful for testing.
     *
     * @param val true if take operations should be faked
     */
    public void setForceFakeTakeFlag(boolean val);

    /**
     * Continue operations that were previously paused.
     */
    public void resume();
    /**
     * Check to see if the module responsible for updating the database with
     * data received from the vision system has connected to the database.
     *
     * @return is vision to database system currently connected to the database
     */
    public boolean isVisionToDbConnected();

    public boolean snapshotsEnabled();

    /**
     * Used for logging/debugging. Save a file(s) in the temporary directory
     * with the comment and a timestamp with the current view of the parts and
     * robot.
     *
     * @param comment comment name to include in filename for later analysis
     */
    public void takeSnapshots(String comment);

    /**
     * Print a great deal of debugging info to the console.
     */
    public void debugAction();

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
    public XFuture<Boolean> startContinuousDemo(String comment, boolean reverseFirst);

    /**
     * Get the value of supervisorEventLogger
     *
     * @return the value of supervisorEventLogger
     */
    @Nullable
    public Consumer<String> getSupervisorEventLogger();

    /**
     * Set the value of supervisorEventLogger
     *
     * @param supervisorEventLogger new value of supervisorEventLogger
     */
    public void setSupervisorEventLogger(Consumer<String> supervisorEventLogger);

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
    public XFuture<Boolean> startPreCheckedContinuousDemo(String comment, boolean reverseFirst);

    /**
     * Get the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use.
     *
     * @return reverse flag
     */
    public boolean isReverseFlag();

    /**
     * Set the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use. Reload the simulated object positions.
     *
     * @param reverseFlag new value for reverse flag
     * @return a future object that can be used to determine when setting the
     * reverse flag and all related actions is complete.
     */
    public XFuture<Void> startSetReverseFlag(boolean reverseFlag);

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
    public XFuture<Void> startSetReverseFlag(boolean reverseFlag, boolean reloadSimFiles);

    /**
     * Get the state of whether the system is paused
     *
     * @return paused state
     */
    public boolean isPaused();

    /**
     * Pause any actions currently being performed and set a state that will
     * cause future actions to wait until resume is called.
     */
    public void pause();

 
    /**
     * Reset errors and reload simulation files
     *
     * @return a future object that can be used to determine when setting the
     * reset and all related actions is complete.
     */
    public XFuture<Void> reset();

    /**
     * Reset errors and optionally reload simulation files
     *
     * @param reloadSimFiles whether to reload simulation files
     *
     * @return a future object that can be used to determine when setting the
     * reset and all related actions is complete.
     */
    public XFuture<Void> reset(boolean reloadSimFiles);

    
    /**
     * Clear any error flags / strings set.
     */
    public void clearErrors();

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
    public XFuture<Boolean> startCheckEnabled();

    /**
     * Get the last future created from a startActions request. Only used for
     * debugging.
     *
     * @return future or null if no startActions request has been made.
     */
    @Nullable
    public XFuture<Boolean> getLastStartActionsFuture();

    /**
     * Get the last future created from a continueActions request. Only used for
     * debugging.
     *
     * @return future or null if no continueActions request has been made.
     */
    @Nullable
    public XFuture<Boolean> getContinueActionListFuture();


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
    public XFuture<Boolean> startActionsList(Iterable<Action> actions);

    /**
     * Check to see if the executor is in a state where it could begin working
     * on a new list of actions.
     *
     * @return is executor ready for new actions.
     */
    public boolean readyForNewActionsList();

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
    public XFuture<Boolean> startActions(String comment);

    /**
     * Get the state of whether the PDDL executor is currently doing actions.
     *
     * @return current state
     */
    public boolean isDoingActions();
    /**
     * Get the current line in the CRCL program being run.
     *
     * @return current line
     */
    public int getCrclProgramLine() ;


    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param pose optional pose to mark or null
     * @param label optional label for pose or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, @Nullable PoseType pose, @Nullable String label);

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, PointType point, String label);

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param f file to save snapshot image to
     * @param point optional point to mark or null
     * @param label optional label for pose or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, @Nullable PmCartesian point, @Nullable String label);

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel string that will be included in the image file name
     * @param pose optional pose to mark or null
     * @param poseLabel optional label for pose or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel) throws IOException ;

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel string that will be included in the image file name
     * @param pt optional point to mark or null
     * @param pointLabel optional label for point or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, @Nullable PmCartesian pt, @Nullable String pointLabel) throws IOException ;

    /**
     * Take a snapshot of the view of objects positions and save it in the
     * specified file, optionally highlighting a pose with a label.
     *
     * @param imgLabel string that will be included in the image file name
     * @param pt optional point to mark or null
     * @param pointLabel optional label for point or null
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(String imgLabel, PointType pt, String pointLabel) throws IOException ;

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint);

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param imgLabel string that will be added to the filename along with
     * timestamp and extention
     * @param itemsToPaint list of items to paint
     * @throws java.io.IOException problem writing to the file
     */
    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint) throws IOException;

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
    public void takeSimViewSnapshot(File f, PoseType pose, String label, int w, int h);

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
    public void takeSimViewSnapshot(File f, PointType point, String label, int w, int h);

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
    public void takeSimViewSnapshot(File f, PmCartesian point, String label, int w, int h);

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
    public void takeSimViewSnapshot(String imgLabel, PoseType pose, String poseLabel, int w, int h) throws IOException;

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
    public void takeSimViewSnapshot(String imgLabel, PmCartesian pt, String pointLabel, int w, int h) throws IOException;

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
    public void takeSimViewSnapshot(String imgLabel, PointType pt, String pointLabel, int w, int h) throws IOException;

    /**
     ** Take a snapshot of the view of objects positions passed in the list.
     *
     * @param f file to save snapshot image to
     * @param itemsToPaint list of items to paint
     * @param w width of snapshot image
     * @param h height of snapshot image
     * @throws IOException if writing the file fails
     */
    public void takeSimViewSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h);

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
    public void takeSimViewSnapshot(String imgLabel, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) throws IOException;

 
    /**
     * Get the selected or top window (InternalJFrame).
     *
     * @return active window
     */
    public ActiveWinEnum getActiveWin() ;

    /**
     * Select a window (InternalJFrame)to be shown on top.
     *
     * @param activeWin
     */
    public void setActiveWin(ActiveWinEnum activeWin);

       /**
     * Get the value of priority
     *
     * @return the value of priority
     */
    public int getPriority();

    /**
     * Set the value of priority
     *
     * @param priority new value of priority
     */
    public void setPriority(int priority);

   
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
    public XFuture<Boolean> continueCrclProgram();

    /**
     * Get the value of enableDatabaseUpdates
     *
     * @return current setting for enableDatabaseUpdates
     */
    public boolean isEnableVisionToDatabaseUpdates() ;
    /**
     * Set the value of enableDatabaseUpdates
     *
     * @param enableDatabaseUpdates new value of enableDatabaseUpdates
     * @param requiredParts map of part names to required number of each part
     * type
     */
    public void setEnableVisionToDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts);


    /**
     * Get the current directory for saving log files
     *
     * @return log files directory
     * @throws java.io.IOException file can not be created ie default log
     * directory does not exist.
     */
    public File getlogFileDir() throws IOException;

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
    public File createTempFile(String prefix, String suffix) throws IOException;

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
    public File createTempFile(String prefix, String suffix, File dir) throws IOException ;
    

    /**
     * Get a list with information on how the most recently loaded CRCL program
     * has run so far.
     *
     * @return run data for last program
     */
    public List<ProgramRunData> getLastProgRunDataList();

    /**
     * Save the given run data which contains information on how a given program
     * run went to a CSV file.
     *
     * @param f file to save
     * @param list data to write to file
     * @throws IOException file does not exist or not writeable etc
     */
    public void saveProgramRunDataListToCsv(File f, List<ProgramRunData> list) throws IOException;

    /**
     * Save the current run data which contains information on how a given
     * program run went to a CSV file.
     *
     * @param f file to save
     * @throws IOException file does not exist or not writable etc
     */
    public void saveLastProgramRunDataListToCsv(File f) throws IOException ;
    
    public void setVisible(boolean visible);
    
    public void setDefaultCloseOperation(int operation);
    
    public List<PhysicalItem> getAvailableToolHolders();
    
}
