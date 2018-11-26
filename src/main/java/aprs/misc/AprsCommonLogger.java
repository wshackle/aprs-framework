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
package aprs.misc;

import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
public class AprsCommonLogger {

    private static class AprsCommonLoggerInstanceHolder {

        private static final AprsCommonLogger INSTANCE = new AprsCommonLogger();
    }

    public static AprsCommonLogger instance() {
        return AprsCommonLoggerInstanceHolder.INSTANCE;
    }

    private final AprsCommonPrintStream outStream;
    private final AprsCommonPrintStream errStream;
    private final ConcurrentLinkedDeque<Consumer<String>> stringConsumers
            = new ConcurrentLinkedDeque<>();
    
    @Nullable
    private File logFile = null;
    
    @Nullable 
    private volatile PrintStream auxPrintStream= null;

    @SuppressWarnings("initialization")
    private final Consumer<String> auxConsumer
            = (String s) -> printToAux(s);

    private void printToAux(String s) {
        if (null != auxPrintStream) {
            auxPrintStream.print(s);
        }
    }

    private AprsCommonLogger() {
        outStream = new AprsCommonPrintStream(System.out, stringConsumers);
        errStream = new AprsCommonPrintStream(System.err, stringConsumers);
        System.setOut(outStream);
        System.setErr(errStream);
        try {
            logFile = Utils.createTempFile("aprsPrintLogs_", ".txt");
            auxPrintStream = new PrintStream(logFile);
            stringConsumers.add(auxConsumer);
        } catch (Exception ex) {
            Logger.getLogger(AprsCommonLogger.class.getName()).log(Level.SEVERE, "", ex);
            logFile = null;
            auxPrintStream = null;
        }
    }

    private final AtomicInteger refCount = new AtomicInteger();

    public void addRef() {
        refCount.incrementAndGet();
    }

    public void removeRef() {
        int count = refCount.decrementAndGet();
        if (count == 0) {
            stringConsumers.remove(auxConsumer);
            PrintStream ps = auxPrintStream;
            auxPrintStream = null;
            if (null != ps) {
                ps.close();
            }
        }
    }

    public ConcurrentLinkedDeque<Consumer<String>> getStringConsumers() {
        return stringConsumers;
    }

}
