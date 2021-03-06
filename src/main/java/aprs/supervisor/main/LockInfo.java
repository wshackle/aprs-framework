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
package aprs.supervisor.main;

import static aprs.supervisor.main.AprsSupervisorDisplayJFrame.getTimeString;
import crcl.utils.XFutureVoid;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
class LockInfo {

    private final long startTime;
    private final XFutureVoid future;

    public LockInfo(String name) {
        startTime = System.currentTimeMillis();
        future = new XFutureVoid("toggleLockInfo."+name);
    }

    public long getStartTime() {
        return startTime;
    }

    public XFutureVoid getFuture() {
        return future;
    }

    @Override
    public String toString() {
        return "LockInfo:" + getTimeString(startTime) + " (" + (System.currentTimeMillis() - startTime) + " ms ago) : " + future;
    }

}
