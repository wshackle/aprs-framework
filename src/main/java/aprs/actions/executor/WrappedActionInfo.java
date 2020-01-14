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

import crcl.utils.CRCLCommandWrapper;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holds information associated with an action created by the CRCL Generator and
 * passed back via wrapped commands to the Executor.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class WrappedActionInfo {

    private final @Nullable
    Action parentAction;
    
    private final int parentActionIndex;

    private final Action action;
    private final int pddlActionIndex;
    private final int outIndex;
    private @Nullable
    CRCLCommandWrapper wrapper = null;
    private final int startSafeAbortRequestCount;

    public WrappedActionInfo(Action action, int pddlActionIndex, int outIndex, int startSafeAbortRequestCount, @Nullable Action parentAction, int parentActionIndex) {
        this.action = action;
        this.pddlActionIndex = pddlActionIndex;
        this.outIndex = outIndex;
        this.startSafeAbortRequestCount = startSafeAbortRequestCount;
        this.parentAction = parentAction;
        this.parentActionIndex = parentActionIndex;
    }

    public @Nullable
    CRCLCommandWrapper getWrapper() {
        return wrapper;
    }

    void setWrapper(CRCLCommandWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public Action getAction() {
        return action;
    }

    public int getPddlActionIndex() {
        return pddlActionIndex;
    }

    public int getOutIndex() {
        return outIndex;
    }

    public int getStartSafeAbortRequestCount() {
        return startSafeAbortRequestCount;
    }

    public @Nullable
    Action getParentAction() {
        return parentAction;
    }

    public int getParentActionIndex() {
        return parentActionIndex;
    }

    @Override
    public String toString() {
        return "WrappedActionInfo{" + "parentAction=" + parentAction + ", parentActionIndex=" + parentActionIndex + ", action=" + action + ", pddlActionIndex=" + pddlActionIndex + ", outIndex=" + outIndex + ", startSafeAbortRequestCount=" + startSafeAbortRequestCount + '}';
    }

}
