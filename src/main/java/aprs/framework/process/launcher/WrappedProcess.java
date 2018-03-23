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
package aprs.framework.process.launcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author shackle
 */
public class WrappedProcess {
    
    private final Thread outputReaderThread;
    private final Thread errorReaderThread;
    private final OutputStream outputPrintStream;
    private final OutputStream errorPrintStream;
    private final Process process;
    private volatile boolean closed = false;
    
    private void readOutputStream() {
        try {
            int readRet = -1;
            while(!closed && (-1 != (readRet = process.getInputStream().read()))) {
                outputPrintStream.write(readRet);
            }
        } catch (IOException ex) {
            if(!closed) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void readErrorStream() {
        try {
            int readRet = -1;
            while(!closed && (-1 != (readRet = process.getErrorStream().read()))) {
                errorPrintStream.write(readRet);
            }
        } catch (IOException ex) {
            if(!closed) {
                Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private static final AtomicInteger num = new AtomicInteger();
    private final int myNum;
    
    public WrappedProcess(OutputStream outputPrintStream, OutputStream errorPrintStream, List<String> command ) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        String cmdLine = command.stream().collect(Collectors.joining(" "));
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        this.errorPrintStream = errorPrintStream;
        this.outputPrintStream = outputPrintStream;
        process = pb.start();
        myNum  = num.incrementAndGet();
        outputReaderThread = new Thread(this::readOutputStream, "outputReader:"+myNum+cmdLine);
        outputReaderThread.setDaemon(true);
        errorReaderThread = new Thread(this::readErrorStream, "errorReader:"+myNum+cmdLine);
        errorReaderThread.setDaemon(true);
        outputReaderThread.start();
        errorReaderThread.start();
    }
    
    public WrappedProcess(OutputStream outputPrintStream, OutputStream errorPrintStream, String ...command ) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        String cmdLine = String.join(" ",command);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        this.errorPrintStream = errorPrintStream;
        this.outputPrintStream = outputPrintStream;
        process = pb.start();
        myNum  = num.incrementAndGet();
        outputReaderThread = new Thread(this::readOutputStream, "outputReader:"+myNum+cmdLine);
        outputReaderThread.setDaemon(true);
        errorReaderThread = new Thread(this::readErrorStream, "errorReader:"+myNum+cmdLine);
        errorReaderThread.setDaemon(true);
        outputReaderThread.start();
        errorReaderThread.start();
    }
    
    public WrappedProcess(File directory, OutputStream outputPrintStream, OutputStream errorPrintStream, String ...command ) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command).directory(directory);
        String cmdLine = String.join(" ",command);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        this.errorPrintStream = errorPrintStream;
        this.outputPrintStream = outputPrintStream;
        process = pb.start();
        myNum  = num.incrementAndGet();
        outputReaderThread = new Thread(this::readOutputStream, "outputReader:"+myNum+cmdLine);
        outputReaderThread.setDaemon(true);
        errorReaderThread = new Thread(this::readErrorStream, "errorReader:"+myNum+cmdLine);
        errorReaderThread.setDaemon(true);
        outputReaderThread.start();
        errorReaderThread.start();
    }
    
    public WrappedProcess(File directory, OutputStream outputPrintStream, OutputStream errorPrintStream, List<String> command ) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command).directory(directory);
        String cmdLine = command.stream().collect(Collectors.joining(" "));
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        this.errorPrintStream = errorPrintStream;
        this.outputPrintStream = outputPrintStream;
        process = pb.start();
        myNum  = num.incrementAndGet();
        outputReaderThread = new Thread(this::readOutputStream, "outputReader:"+myNum+cmdLine);
        outputReaderThread.setDaemon(true);
        errorReaderThread = new Thread(this::readErrorStream, "errorReader:"+myNum+cmdLine);
        errorReaderThread.setDaemon(true);
        outputReaderThread.start();
        errorReaderThread.start();
    }
    
    public void close() {
        this.closed = true;
        process.destroyForcibly();
        outputReaderThread.interrupt();
        try {
            outputReaderThread.join(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        errorReaderThread.interrupt();
        try {
            errorReaderThread.join(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            outputPrintStream.close();
        } catch (IOException ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            errorPrintStream.close();
        } catch (IOException ex) {
            Logger.getLogger(WrappedProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
