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
package aprs.launcher;

import crcl.ui.XFuture;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
public class WrappedProcess {

    private final Thread outputReaderThread;
    private final Thread errorReaderThread;
    private final PrintStream outputPrintStream;
    private final PrintStream errorPrintStream;
    private volatile Process process;
    private volatile boolean closed = false;
    private final XFuture<Process> xf;
    private final String cmdLine;

    @MonotonicNonNull private Component displayComponent;

    /**
     * Get the value of displayComponent
     *
     * @return the value of displayComponent
     */
    @Nullable public Component getDisplayComponent() {
        return displayComponent;
    }

    /**
     * Set the value of displayComponent
     *
     * @param displayComponent new value of displayComponent
     */
    public void setDisplayComponent(Component displayComponent) {
        this.displayComponent = displayComponent;
    }

    private void readOutputStream() {
        try {
            int readRet = -1;
            while (!closed && (-1 != (readRet = process.getInputStream().read()))) {
                outputPrintStream.write(readRet);
            }
        } catch (IOException ex) {
            if (!closed) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void readErrorStream() {
        try {
            int readRet = -1;
            while (!closed && (-1 != (readRet = process.getErrorStream().read()))) {
                errorPrintStream.write(readRet);
            }
        } catch (IOException ex) {
            if (!closed) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private static final AtomicInteger num = new AtomicInteger();
    private final int myNum;

    private static final ThreadFactory threadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "processStarterThread");
            t.setDaemon(true);
            return t;
        }
    };

    private static final ExecutorService processStarterService = Executors.newSingleThreadExecutor(threadFactory);

    public static void shutdownStarterService() {
        processStarterService.shutdown();
    }

    private void setProcess(Process p) {
        this.process = p;
        outputReaderThread.start();
        errorReaderThread.start();
    }

    private Process internalStart(ProcessBuilder pb) {
        try {
            return pb.start();
        } catch (Exception ex) {
            ex.printStackTrace(errorPrintStream);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("initialization")
    private WrappedProcess(ProcessBuilder pb, String cmdLine,
            OutputStream outputPrintStream, OutputStream errorPrintStream) {
        myNum = num.incrementAndGet();
        this.cmdLine = cmdLine;
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        this.errorPrintStream
                = (errorPrintStream instanceof PrintStream)
                        ? (PrintStream) errorPrintStream
                        : new PrintStream(errorPrintStream);
        this.outputPrintStream
                = (outputPrintStream instanceof PrintStream)
                        ? (PrintStream) outputPrintStream
                        : new PrintStream(outputPrintStream);
        outputReaderThread = new Thread(this::readOutputStream, "outputReader:" + myNum + cmdLine);
        outputReaderThread.setDaemon(true);
        errorReaderThread = new Thread(this::readErrorStream, "errorReader:" + myNum + cmdLine);
        errorReaderThread.setDaemon(true);
        xf = XFuture.supplyAsync(cmdLine, ()->internalStart(pb), processStarterService);
        xf.thenAccept(this::setProcess);
    }

    public WrappedProcess(OutputStream outputPrintStream, OutputStream errorPrintStream, List<String> command) throws IOException {
        this(new ProcessBuilder(command),
                command.stream().collect(Collectors.joining(" ")),
                outputPrintStream,
                errorPrintStream);
    }

    public WrappedProcess(OutputStream outputPrintStream, OutputStream errorPrintStream, String... command) throws IOException {
        this(new ProcessBuilder(command),
                String.join(" ", command),
                outputPrintStream,
                errorPrintStream);
    }

    public WrappedProcess(File directory, OutputStream outputPrintStream, OutputStream errorPrintStream, String... command) throws IOException {
        this(new ProcessBuilder(command).directory(directory),
                String.join(" ", command),
                outputPrintStream,
                errorPrintStream);
    }

    public WrappedProcess(File directory, OutputStream outputPrintStream, OutputStream errorPrintStream, List<String> command) throws IOException {
        this(new ProcessBuilder(command).directory(directory),
                command.stream().collect(Collectors.joining(" ")),
                outputPrintStream,
                errorPrintStream);
    }

    public boolean waitFor(long timeout,
            TimeUnit unit) throws InterruptedException {
        System.out.println("waiting For cmdLine=" + cmdLine + ", process=" + process + " ...");
        if (null != process) {
            boolean ret = process.waitFor(timeout, unit);
            System.out.println("waitFor cmdLine=" + cmdLine + " returning " + ret);
            return ret;
        }
        return true;
    }

    public void close() {
        this.closed = true;
        xf.cancelAll(true);
        if (null != process) {
            try {
                if (!process.waitFor(50, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
                process.destroyForcibly();
            }
        }
        if (null != outputReaderThread) {
            outputReaderThread.interrupt();
            try {
                outputReaderThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != errorReaderThread) {
            errorReaderThread.interrupt();
            try {
                errorReaderThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            outputPrintStream.close();
        } catch (Exception ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            errorPrintStream.close();
        } catch (Exception ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
