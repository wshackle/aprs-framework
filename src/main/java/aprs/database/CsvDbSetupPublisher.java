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
package aprs.database;

import crcl.utils.XFutureVoid;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
public class CsvDbSetupPublisher implements DbSetupPublisher {

    public CsvDbSetupPublisher()  throws IOException {
        this.thisDbSetup = new CsvDbSetup();
    }

    @Override
    public XFutureVoid setDbSetup(DbSetup setup) {
        this.thisDbSetup = setup;
        return XFutureVoid.completedFuture();
    }

    @Override
    public DbSetup getDbSetup() {
        return this.thisDbSetup;
    }

    private final Set<DbSetupListener> dbSetupListeners = new HashSet<>();

    @Override
    public void addDbSetupListener(DbSetupListener listener) {
        dbSetupListeners.add(listener);
    }

    @Override
    public void removeDbSetupListener(DbSetupListener listener) {
        dbSetupListeners.remove(listener);
    }

    @Override
    public void removeAllDbSetupListeners() {
        dbSetupListeners.clear();
    }

    private DbSetup thisDbSetup;

    @Override
    public List<XFutureVoid> notifyAllDbSetupListeners(@Nullable ExecutorService notifyService) {
        if (notifyService != null) {
            XFutureVoid future
                    = XFutureVoid.runAsync("broadcastDbSetup",
                            () -> {
                                broadcastDbSetup(thisDbSetup);
                            },
                            notifyService);
            return Collections.singletonList(future);
        } else {
            broadcastDbSetup(thisDbSetup);
            return Collections.emptyList();
        }
    }

    private synchronized void broadcastDbSetup(DbSetup thisDbSetup) {
        for (DbSetupListener listener : dbSetupListeners) {
            if (null != listener) {
                listener.accept(thisDbSetup);
            }
        }
    }

    @Override
    public XFutureVoid disconnect() {
        return XFutureVoid.completedFuture();
    }

}
