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

import crcl.utils.CRCLUtils;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsCommonLogger {

    private static class AprsCommonLoggerInstanceHolder {

        private static final AprsCommonLogger INSTANCE = new AprsCommonLogger();
    }

    public static AprsCommonLogger instance() {
        return AprsCommonLoggerInstanceHolder.INSTANCE;
    }

    public static PrintStream out() {
        return instance().outStream;
    }

    public static PrintStream err() {
        return instance().errStream;
    }

    public static class System {

        public static final PrintStream out = out();
        public static final PrintStream err = err();
    }

    public static void println(String string) {
        instance().outStream.println(string);
    }

    public static void println() {
        instance().outStream.println();
    }

    public static void printErrln(String string) {
        instance().outStream.println(string);
    }

    private final AprsCommonPrintStream outStream;
    private final AprsCommonPrintStream errStream;

    public PrintStream getOrigSystemOut() {
        return origSystemOut;
    }

    public PrintStream getOrigSystemErr() {
        return origSystemErr;
    }

    private final PrintStream origSystemOut;
    private final PrintStream origSystemErr;

    private final ConcurrentLinkedDeque<Consumer<String>> stringConsumers
            = new ConcurrentLinkedDeque<>();

    private @Nullable File logFile;

    private volatile @Nullable PrintStream auxPrintStream;

    @SuppressWarnings({"nullness", "initialization"})
    private final Consumer<String> auxConsumer
            = (String s) -> printToAux(s);

    private void printToAux(String s) {
        if (null != auxPrintStream) {
            auxPrintStream.print(s);
        }
    }

    private AprsCommonLogger() {
        origSystemOut = java.lang.System.out;
        origSystemErr = java.lang.System.err;
        outStream = new AprsCommonPrintStream(origSystemOut, stringConsumers);
        errStream = new AprsCommonPrintStream(origSystemErr, stringConsumers);
        if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
            java.lang.System.setOut(outStream);
            java.lang.System.setErr(errStream);
        }
        try {
            File newLogFile = Utils.createTempFile("aprsPrintLogs_", ".txt");
            this.logFile = newLogFile;
            origSystemOut.println("logging to " + newLogFile);
            origSystemErr.println("logging to " + newLogFile);
            auxPrintStream = new PrintStream(newLogFile);
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
