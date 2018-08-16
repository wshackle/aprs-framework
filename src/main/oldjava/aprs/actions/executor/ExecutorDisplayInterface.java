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
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("ALL")
interface ExecutorDisplayInterface {

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

    /**
     * Process the current list of actions.
     */
    public void processActions();

    /**
     * Get the current aprsSystemInterface
     * @return aprsSystemInterface
     */
    public AprsSystem getAprsSystem();

}
