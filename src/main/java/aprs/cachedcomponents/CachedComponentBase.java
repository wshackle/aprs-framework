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
package aprs.cachedcomponents;

import static aprs.misc.Utils.isEventDispatchThread;
import crcl.ui.XFutureVoid;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.guieffect.qual.UI;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CachedComponentBase {

    protected CachedComponentBase() {
    }

    /**
     * Run something on the dispatch thread.
     *
     * @param r object with run method to call
     * @return future that provides info on when the method completes.
     */
    protected XFutureVoid runOnDispatchThread(final @UI Runnable r) {
        return runOnDispatchThread("runOnDispatchThread", r);
    }

    private final AtomicInteger dispatchCount = new AtomicInteger();
    private final AtomicInteger startCount = new AtomicInteger();
    private final AtomicInteger finishCount = new AtomicInteger();

    /**
     * Run something on the dispatch thread and attach a name to it for
     * debugging/logging/visualization.
     *
     * @param name optional name for better debugging/logging/visualization
     * @param r object with run method to call
     * @return future that provides info on when the method completes.
     */
    @SuppressWarnings("guieffect")
    protected XFutureVoid runOnDispatchThread(String name, final @UI Runnable r) {
        int dc = dispatchCount.incrementAndGet();
        XFutureVoid ret = new XFutureVoid(name);
        if (isEventDispatchThread()) {
            try {
                int sc = startCount.incrementAndGet();
                r.run();
                int fc = finishCount.incrementAndGet();
                ret.complete(null);
            } catch (Exception e) {
                ret.completeExceptionally(e);
                LOGGER.log(Level.SEVERE, name, e);
            }

            return ret;
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    int sc = startCount.incrementAndGet();
                    r.run();
                    int fc = finishCount.incrementAndGet();
                    ret.complete(null);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, name, e);
                    ret.completeExceptionally(e);
                }
            });
            return ret;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CachedTextField.class.getName());

    protected int getDispatchCount() {
        return dispatchCount.get();
    }

    protected int getFinishCount() {
        return finishCount.get();
    }

    protected int getStartCount() {
        return startCount.get();
    }
}
