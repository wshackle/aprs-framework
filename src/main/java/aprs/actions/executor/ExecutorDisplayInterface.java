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
package aprs.actions.executor;

import aprs.system.AprsSystem;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"all", "serial"})
public interface ExecutorDisplayInterface {

    /**
     * Get the current list of actions.
     * @return list of actions
     */
    public List<Action> getActionsList();

    /**
     * Clear the actions list.
     */
    public void clearActionsList();
    
    /**
     * Add the action to the list of actions.
     * @param action action to add.
     */
    public void addAction(Action action);

//    /**
//     * Process the current list of actions.
//     */
//    public void processActions();

//    /**
//     * Get the current aprsSystemInterface
//     * @return aprsSystemInterface
//     */
//    public AprsSystem getAprsSystem();

    
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
     * Add a listener to be called from setSelectedToolName.
     * 
     * @param listener listener to be stored in collection
     */
    public void addSelectedToolNameListener(Consumer<String> listener);

    /**
     * Add a listener to be no longer called from setSelectedToolName.
     * 
     * @param listener listener to be removed from collection
     */
    public void removeSelectedToolNameListener(Consumer<String> listener);
    
    /**
     * Sets the current tool that is assumed to be attached to the robot. The
     * robot will not move to get the tool. This may change the tool offset pose.
     * 
     * @param newToolName new tool to be associated with the robot and key for tool offset map
     */
    public void setSelectedToolName(String newToolName);
    
    /**
     * Abort the currently running CRCL program.
     *
     * @return future to determine when the abort completes etc.
     */
    public XFutureVoid abortProgram();
    
    
    /**
     * Add a listener to be called from updateCurrentToolHolderContentsMap
     * 
     * @param listener a listener to be added to a collection to be notified with new info on what holder holds which tool
     */
    public void addToolHolderContentsListener(BiConsumer<String, String> listener);

    /**
     * Remove a listener to be called from updateCurrentToolHolderContentsMap
     * 
     * @param listener a listener to be removed a collection to be notified with new info on what holder holds which tool
     */
    public void removeToolHolderContentsListener(BiConsumer<String, String> listener);
    
    /**
     * Perform a cartesian move to a previously recorded and named position.
     * The move may be executed asynchronously in another thread.
     * Any actions currently in progress will be aborted first.
     * 
     * @param recordedPoseName name of previously recorded pose
     * @return future indicating if/when the move is completed.
     */
    public XFuture<Boolean> cartesianMoveToRecordedPosition(String recordedPoseName);
    
    
    /**
     * Perform a joint move to a previously recorded and named set of joint positions.
     * The move may be executed asynchronously in another thread.
     * Any actions currently in progress will be aborted first.
     * 
     * @param recordedJointsName name of previously recorded set of joint positions
     * @return future indicating if/when the move is completed.
     */
    public XFuture<Boolean> jointMoveToNamedPosition(String recordedJointsName);
}
