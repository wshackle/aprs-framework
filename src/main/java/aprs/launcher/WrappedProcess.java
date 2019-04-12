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
package aprs.launcher;

import crcl.ui.XFuture;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class WrappedProcess {

    private final Thread outputReaderThread;
    private final Thread errorReaderThread;
    private final OutputStream outputStream;
    private final OutputStream errorStream;
    private final PrintStream outputPrintStream;
    private final PrintStream errorPrintStream;
    private volatile Process process;
    private volatile boolean closed = false;
    private final XFuture<Process> processStartXFuture;
    private final String cmdLine;
    
    public void printInfo(PrintStream ps) {
        ps.println("cmdLine = " + cmdLine);
        ps.println("closed = " + closed);
        ps.println("process = " + process);
        ps.println("outputReaderThread = " + outputReaderThread);
        ps.println("errorReaderThread = " + errorReaderThread);
        ps.println("processStartXFuture = " + processStartXFuture);
        String errorDebugString = getErrorDebugString();
        ps.println("errorDebugString = " + errorDebugString);
        String outputDebugString = getOutputDebugString();
        ps.println("outputDebugString = " + outputDebugString);
        System.out.println("displayComponent = " + displayComponent);
        if(outputStream instanceof LogDisplayPanelOutputStream) {
            LogDisplayPanelOutputStream ljpos = (LogDisplayPanelOutputStream)
                    outputStream;
            System.out.println("ljpos = " + ljpos);
        }
    }
 
    @MonotonicNonNull
    private Component displayComponent;

    public XFuture<Process> getProcessStartXFuture() {
        return processStartXFuture;
    }

    /**
     * Get the value of displayComponent
     *
     * @return the value of displayComponent
     */
    @Nullable
    public Component getDisplayComponent() {
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

    private final char readOutputDebugBuff[] = new char[256];
    private final AtomicInteger readOutputDebugCount = new AtomicInteger();

    public String getOutputDebugString() {
        int c = readOutputDebugCount.get();
        int index = c % readOutputDebugBuff.length;
        if (c < readOutputDebugBuff.length) {
            if (index < 1) {
                return "";
            } else {
                return new String(readOutputDebugBuff, 0, index);
            }
        } else if (index < 1) {
            return new String(readOutputDebugBuff);
        } else {
            return new String(readOutputDebugBuff, index, (readOutputDebugBuff.length - index))
                    + new String(readOutputDebugBuff, 0, index);
        }
    }

    private void readOutputStream() {
        try {
            int readRet = -1;
            InputStream inputStream = process.getInputStream();
            while (!closed && (-1 != (readRet = inputStream.read()))) {
                int c = readOutputDebugCount.incrementAndGet();
                int index = (c - 1) % readOutputDebugBuff.length;
                readOutputDebugBuff[index] = (char) readRet;
                outputPrintStream.write(readRet);
            }
        } catch (IOException ex) {
            if (!closed) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    private final char readErrorDebugBuff[] = new char[256];
    private final AtomicInteger readErrorDebugCount = new AtomicInteger();

    public String getErrorDebugString() {
        int c = readErrorDebugCount.get();
        int index = c % readErrorDebugBuff.length;
        if (c < readErrorDebugBuff.length) {
            return new String(readErrorDebugBuff, 0, index);
        } else {
            return new String(readErrorDebugBuff, index, (readErrorDebugBuff.length - index))
                    + new String(readErrorDebugBuff, 0, index);
        }
    }

    private void readErrorStream() {
        try {
            int readRet = -1;
            InputStream errorStream = process.getErrorStream();
            while (!closed && (-1 != (readRet = errorStream.read()))) {
                int c = readErrorDebugCount.incrementAndGet();
                int index = (c - 1) % readErrorDebugBuff.length;
                readErrorDebugBuff[index] = (char) readRet;
                errorPrintStream.write(readRet);
            }
        } catch (IOException ex) {
            if (!closed) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
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

    @Nullable
    private volatile Exception processStartException = null;

    private Process internalStart(ProcessBuilder pb) {
        try {
            return pb.start();
        } catch (Exception ex) {
            processStartException = ex;
            ex.printStackTrace(errorPrintStream);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    @SuppressWarnings("initialization")
    private WrappedProcess(ProcessBuilder pb, String cmdLine,
            OutputStream outputStream, OutputStream errorStream) {
        myNum = num.incrementAndGet();
        this.cmdLine = cmdLine;
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        this.outputStream = outputStream;
        this.errorStream = errorStream;
        this.errorPrintStream
                = (errorStream instanceof PrintStream)
                        ? (PrintStream) errorStream
                        : new PrintStream(errorStream);
        this.outputPrintStream
                = (outputStream instanceof PrintStream)
                        ? (PrintStream) outputStream
                        : new PrintStream(outputStream);
        outputReaderThread = new Thread(this::readOutputStream, "outputReader:" + myNum + cmdLine);
        outputReaderThread.setDaemon(true);
        errorReaderThread = new Thread(this::readErrorStream, "errorReader:" + myNum + cmdLine);
        errorReaderThread.setDaemon(true);
        processStartXFuture = XFuture.supplyAsync(cmdLine, () -> internalStart(pb), processStarterService);
        processStartXFuture.thenAccept(this::setProcess);
    }

    public WrappedProcess(OutputStream outputPrintStream, OutputStream errorPrintStream, List<String> command) {
        this(new ProcessBuilder(command),
                command.stream().collect(Collectors.joining(" ")),
                outputPrintStream,
                errorPrintStream);
    }

    public WrappedProcess(OutputStream outputPrintStream, OutputStream errorPrintStream, String... command) {
        this(new ProcessBuilder(command),
                String.join(" ", command),
                outputPrintStream,
                errorPrintStream);
    }

    public WrappedProcess(File directory, OutputStream outputPrintStream, OutputStream errorPrintStream, String... command) {
        this(new ProcessBuilder(command).directory(directory),
                String.join(" ", command),
                outputPrintStream,
                errorPrintStream);
    }

    public WrappedProcess(File directory, OutputStream outputPrintStream, OutputStream errorPrintStream, List<String> command) {
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
        if(null != processStartXFuture && !processStartXFuture.isDone()) {
            Thread.dumpStack();
            System.err.println("WrappedProcess.close : cancelling processStartXFuture="+processStartXFuture);
            processStartXFuture.cancelAll(true);
        }
        if (null != process) {
            try {
                if (!process.waitFor(50, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
                process.destroyForcibly();
            }
        }
        if (null != outputReaderThread) {
            outputReaderThread.interrupt();
            try {
                outputReaderThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        if (null != errorReaderThread) {
            errorReaderThread.interrupt();
            try {
                errorReaderThread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        try {
            outputPrintStream.close();
        } catch (Exception ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
        }
        try {
            errorPrintStream.close();
        } catch (Exception ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

}
