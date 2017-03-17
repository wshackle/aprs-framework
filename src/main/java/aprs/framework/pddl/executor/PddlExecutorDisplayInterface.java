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
package aprs.framework.pddl.executor;

import aprs.framework.AprsJFrame;
import aprs.framework.AprsJFrame;
import aprs.framework.DisplayInterface;
import aprs.framework.DisplayInterface;
import aprs.framework.PddlAction;
import aprs.framework.PddlAction;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface PddlExecutorDisplayInterface extends DisplayInterface {

    /**
     * Show File chooser to select a new actions file and load it.
     * @throws IOException if selected file can not be read
     */
    public void browseActionsFile() throws IOException;

    /**
     * Get the current list of actions.
     * @return list of actions
     */
    public List<PddlAction> getActionsList();

    /**
     * Set the current list of actions.
     * @param actionsList new list of actions to use
     */
    public void setActionsList(List<PddlAction> actionsList);

    /**
     * Add the action to the list of actions.
     * @param action action to add.
     */
    public void addAction(PddlAction action);

    /**
     * Process the current list of actions.
     */
    public void processActions();
    
    /**
     * Auto resize the columns in the pddl output table.
     */
    public void autoResizeTableColWidthsPddlOutput();

    
    /**
     * Set the AprsJFrame 
     * @param aprsJFrame new value of aprsJFrame.
     */
    public void setAprsJFrame(AprsJFrame aprsJFrame);
    
    /**
     * Get the current aprsJFrame
     * @return aprsJFrame
     */
    public AprsJFrame getAprsJFrame();

}
