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

import aprs.cachedcomponents.CachedTable;
import aprs.launcher.LauncherAprsJFrame;
import static aprs.misc.AprsCommonLogger.println;
import aprs.system.AprsSystem;
import crcl.base.CRCLCommandType;
import crcl.utils.XFuture;
import crcl.utils.XFuture.PrintedException;
import crcl.utils.XFutureVoid;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLSocket;
import crcl.utils.CRCLUtils;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class Utils {

    private Utils() {
    }

    public static File file(String path) throws IOException {
        try {
            int index1 = path.indexOf('/');
            int index2 = path.indexOf('\\');
            if (index1 < 0 && index2 < 0) {
                return file(new File(System.getProperty("user.dir")), path);
            } else if (index1 == 0 || index2 == 0) {
                return new File(swapFileSeparators(path));
            } else if (index1 > 0 && (index2 < 0 || index1 < index2)) {
                return file(new File(path.substring(0, index1 + 1)), path.substring(index1 + 1));
            } else {
                return file(new File(path.substring(0, index2 + 1)), path.substring(index2 + 1));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "path=" + path, e);
            throw new RuntimeException("path=" + path, e);
        }
    }

    public static File file(String dirPath, String path) throws IOException {
        return file(new File(swapFileSeparators(dirPath)), path);
    }

    public static File file(File parent, String append) throws IOException {
        if (parent.toString().equals("..")) {
            parent = new File(parent.getCanonicalPath());
        }
        final File origParent = parent;
        final String origAppend = append;
        if (null == parent) {
            throw new NullPointerException("parent");
        }
        if (null == append) {
            throw new NullPointerException("append");
        }
        if (append.isEmpty()) {
            throw new IllegalArgumentException("append.isEmpty()");
        }
        append = swapFileSeparators(append);
        String parentDirPrefix = ".." + File.separator;
        File lastParentFile = parent;
        while (append.startsWith(parentDirPrefix)) {
            lastParentFile = parent;
            File nextParent = parent.getParentFile();
            append = append.substring(parentDirPrefix.length());
            if (nextParent == null) {
                if (append.startsWith(parentDirPrefix)) {
                    throw new RuntimeException("parent=null : append=" + append + ", origParent=" + origParent + ", origParent.getCanonicalPath()=" + origParent.getCanonicalPath() + ", lastParentFile=" + lastParentFile + ", lastParentFile.getCanonicalPath()=" + lastParentFile.getCanonicalPath() + ", origAppend=" + origAppend);
                } else {
                    return new File(append);
                }
            } else {
                parent = nextParent;
            }
        }
        return new File(parent, append);
    }

    private static String swapFileSeparators(String append) {
        if (File.separator.equals("/")) {
            append = append.replace('\\', '/');
        } else if (File.separator.equals("\\")) {
            append = append.replace('/', '\\');
        }
        return append;
    }

    public static String getAprsUserHomeDir() throws IOException {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");

        String dir;
        final String origHomeProperty = System.getProperty("user.home");
        final String aprsUserHomeProperty = System.getProperty("aprs.user.home", origHomeProperty);
        if (isWindows) {
            dir = System.getProperty("windows.aprs.user.home", aprsUserHomeProperty);
        } else {
            dir = System.getProperty("linux.aprs.user.home", aprsUserHomeProperty);
        }
        if (dir != null && dir.length() > 1) {
            File dirFile = file(dir);
            if (dirFile.isDirectory() && dirFile.canWrite() && dirFile.exists()) {
                try {
                    final String dirFileCanonicalPath = dirFile.getCanonicalPath();
                    if (!Objects.equals(dir, origHomeProperty)
                            && !Objects.equals(dirFileCanonicalPath, origHomeProperty)) {
                        System.setProperty("user.home", dirFileCanonicalPath);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return dir;
    }

    public static String traceToString(@Nullable StackTraceElement trace @Nullable []) {
        return XFuture.traceToString(trace);
    }

    public static String traceToShortString(StackTraceElement trace @Nullable [], int clipLen) {
        StringBuilder sb = new StringBuilder();
        if (null != trace) {
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement stackTraceElement = trace[i];
                if (stackTraceElement.getMethodName().contains("logEvent")) {
                    continue;
                }
                if (stackTraceElement.getClassName().contains("Future")) {
                    continue;
                }
                if (stackTraceElement.getClassName().contains("LogEvent")) {
                    continue;
                }
                if (stackTraceElement.getClassName().startsWith("java.")) {
                    continue;
                }
                sb.append(stackTraceElement.getMethodName()).append('(').append(stackTraceElement.getFileName())
                        .append(':').append(stackTraceElement.getLineNumber()).append(')');
                sb.append(", ");
            }
        }
        String out = sb.toString();
        out = clipString(out, clipLen);
        return out;
    }

    public static String clipString(String out, int clipLen) {
        if (out.length() > clipLen) {
            out = out.substring(0, clipLen - 2) + "...";
        }
        return out;
    }

    @UIEffect
    static public void PlayAlert(String resourceName) {
        PlayAlert(resourceName, false, Utils.class);
    }

    private static AtomicInteger playAlertCount = new AtomicInteger();

    static public void tryWithTimeout(Runnable r, long timeout) {
        System.out.println("tryWithTimeout start");
        long time1 = System.currentTimeMillis();
        Thread thread1 = new Thread(() -> {
            try {
                r.run();
            } catch (Exception e) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "", e);
                throw new RuntimeException(e);
            }
        });
        thread1.setDaemon(true);
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(timeout);
                if (thread1.isAlive()) {
                    System.out.println("timeout competed for tryWithTimeout");
                    System.out.println("Interrupting " + thread1);
                }
                thread1.interrupt();
            } catch (InterruptedException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "", ex);
                throw new RuntimeException(ex);
            }
        });
        thread2.setDaemon(true);
        thread2.start();
        thread1.start();
        try {
            thread1.join(timeout);
            long time2 = System.currentTimeMillis();
            long timediff = time2 - time1;
            System.out.println("timediff = " + timediff);
            if (thread2.isAlive()) {
                System.out.println("Interrupting " + thread2);
                thread2.interrupt();
                thread2.join(timeout);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("tryWithTimeout end");
    }

    static public void tryPlayAlert() {
        tryWithTimeout(() -> {
            PlayAlert("alert.wav");
        }, 5000);
    }

    @UIEffect
    static public void PlayAlert(String resourceName, boolean debug, Class<?> aClass) {
        try {
            Toolkit.getDefaultToolkit().beep();
            if (debug) {
                println("PlayAlert : resourceName= " + resourceName);
            }
            Thread.sleep(100);
            if (debug) {
                println("aClass = " + aClass);
            }
            URL url = aClass.getResource(resourceName);
            if (debug) {
                println("PlayAlert: url = " + url);
            }
            if (null != url) {
                Clip clip = AudioSystem.getClip();
                if (debug) {
                    println("PlayAlert: clip = " + clip);
                }
                InputStream inputStream = aClass.getResourceAsStream(resourceName);
                if (null == inputStream) {
                    if (debug) {
                        println("PlayAlert: inputStream = " + inputStream);
                    }
                    return;
                }
                if (!(inputStream instanceof BufferedInputStream)) {
                    inputStream = new BufferedInputStream(inputStream);
                }
                if (debug) {
                    println("PlayAlert: inputStream = " + inputStream);
                }
                if (null != inputStream) {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
                    if (debug) {
                        println("PlayAlert: audioInputStream = " + audioInputStream);
                    }
                    clip.open(audioInputStream);
                    clip.start();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private static final boolean playAlerts = Boolean.getBoolean("aprs.playAlerts");

    static public boolean arePlayAlertsEnabled() {
        final boolean notHeadless = !CRCLUtils.isGraphicsEnvironmentHeadless();
        return notHeadless && playAlerts;
    }

    @UIEffect
    static public void PlayAlert() {
        if (playAlerts) {
            PlayAlert("alert.wav");
        }
    }

    @UIEffect
    static public void PlayAlert2() {
        if (playAlerts) {
            PlayAlert("alert2.wav");
        }
    }

    public static URL getAprsIconUrl() {
        try {
            return CRCLUtils.requireNonNull(Utils.class.getResource("aprs.png"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
            System.out.println("");
            System.err.println("");
            System.out.flush();
            System.err.flush();
            throw new RuntimeException("Utils.class.getResource(\"aprs.png\") threw " + e.getMessage(), e);
        }
    }

    /**
     * A Runnable that may throw a checked exception.
     */
    @SuppressWarnings("RedundantThrows")
    @UIType
    public interface RunnableWithThrow {

        /**
         * Run method to implement.
         *
         * @throws Exception exception occurred
         */
        void run() throws Exception;
    }

    static public @Nullable
    String readFirstLine(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        }
    }

    /**
     * Extension of XFuture which extends CompleteableFuture specifically for
     * operations that are happening on the Swing event dispatch thread.
     *
     * @param <T> type of object that may eventually be returned with get etc.
     */
    @UIType
    public static class SwingFuture<T> extends XFuture<T> {

        /**
         * Complete a new SwingFuture with name attached for later
         * logging/debugging/visualization
         *
         * @param name optional name for tracking futures
         */
        SwingFuture(String name) {
            super(name);
        }

        @Override
        @Deprecated
        @SuppressWarnings("guieffect")
        public T get() throws InterruptedException, ExecutionException {
            if (SwingUtilities.isEventDispatchThread()) {
                throw new IllegalStateException(
                        "One can not get a swing future result on the EventDispatchThread. (getNow can still be used.)");
            }
            return super.get();
        }

        @Override
        @Deprecated
        @SuppressWarnings("guieffect")
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (SwingUtilities.isEventDispatchThread()) {
                throw new IllegalStateException(
                        "One can not get a swing future result on the EventDispatchThread. (getNow can still be used.)");
            }
            return super.get(timeout, unit);
        }

    }

    private static String createAssertErrorString(CRCLCommandType cmd, long id) {
        return "command id being reduced id=" + id + ", cmd=" + CRCLSocket.cmdToString(cmd);
    }

    /**
     * Set the command ID and check that it is at-least as high as current id.
     *
     * @param cmd command to set id of.
     * @param id new id for command.
     */
    public static void setCommandID(CRCLCommandType cmd, long id) {
        assert cmd.getCommandID() <= id : createAssertErrorString(cmd, id);
        cmd.setCommandID(id);
    }

    private static class LogFileDirGetter {

        private static volatile @Nullable
        File logFileDir = createLogFileDir();
        private static @Nullable
        IOException createLogFileException = null;

        private static @Nullable
        File createLogFileDir() {
            try {
                File tmpTest = File.createTempFile("temp_test", "txt");
                File logFileDir = new File(tmpTest.getParentFile(), "aprs_logs_" + getDateTimeString());
                logFileDir.mkdirs();
                tmpTest.delete();
                return logFileDir;
            } catch (IOException exception) {
                createLogFileException = exception;
                exception.printStackTrace();
            }
            return null;
        }

        File getLogFileDir() throws IOException {
            if (null != logFileDir) {
                if (logFileDir.exists() && logFileDir.canWrite() && logFileDir.isDirectory()) {
                    return logFileDir;
                } else {
                    synchronized (this) {
                        File newLogFileDir = createLogFileDir();
                        if (null != newLogFileDir) {
                            logFileDir = newLogFileDir;
                            return newLogFileDir;
                        }
                    }
                }
            }
            if (null != createLogFileException) {
                throw new IOException("Log File Directory was not created.", createLogFileException);
            }
            throw new IOException("Log File Directory was not created.");
        }
    }

    /**
     * Get the current directory used for creating new log files.
     *
     * @return log file directory
     * @throws java.io.IOException file can not be created ie default log
     * directory parent does not exist.
     */
    public static File getlogFileDir() throws IOException {
        return new LogFileDirGetter().getLogFileDir();
    }

    private static String cleanAndLimitFilePrefix(String prefix_in) {
        if (prefix_in.length() > 80) {
            prefix_in = prefix_in.substring(0, 79);
        }
        String prefixOut = prefix_in.replaceAll("[ \t:;-]+", "_").replace('\\', '_').replace('/', '_');
        if (prefixOut.length() > 80) {
            prefixOut = prefixOut.substring(0, 79);
        }
        if (!prefixOut.endsWith("_")) {
            prefixOut = prefixOut + "_";
        }
        return prefixOut;
    }

    /**
     * Create a new temporary file in the current log file directory adding a
     * timestamp and limiting the name length.
     *
     * @param prefix prefix for the new file name
     * @param suffix suffix for the new file name
     * @return new temporary file
     * @throws IOException file can not be created
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        final String fullPrefix = cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix);
        final File logFileDir = getlogFileDir();
        try {
            return File.createTempFile(fullPrefix, suffix, logFileDir);
        } catch (IOException ex) {
            System.err.println("fullPrefix = " + fullPrefix);
            System.err.println("logFileDir = " + logFileDir);
            throw ex;
        }
    }

    /**
     * Create a new temporary file in the given directory adding a timestamp and
     * limiting the name length.
     *
     * @param prefix prefix for the new file name
     * @param suffix suffix for the new file name
     * @param dir directory to store new log file
     * @return new temporary file
     * @throws IOException file can not be created
     */
    public static File createTempFile(String prefix, String suffix, File dir) throws IOException {
        return File.createTempFile(cleanAndLimitFilePrefix(Utils.getTimeString() + "_" + prefix), suffix, dir);
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss.SSS");

    /**
     * Get the current date and time in the default format.
     *
     * @return date and time in formatted string
     */
    public static String getDateTimeString() {
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Get the current date and time in the default format.
     *
     * @return date and time in formatted string
     */
    public static String getDateTimeString(long time) {
        Date date = new Date(time);
        return dateFormat.format(date);
    }

    private static final DateFormat timeFormat = new SimpleDateFormat("HHmmss.SSS");

    /**
     * Get the current time in the default format.
     *
     * @return time in formatted string
     */
    public static String getTimeString() {
        Date date = new Date();
        return timeFormat.format(date);
    }

    /**
     * Get the current time in the default format.
     *
     * @return time in formatted string
     */
    public static String getTimeString(long time) {
        Date date = new Date(time);
        return timeFormat.format(date);
    }

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    @SuppressWarnings("guieffect")
    public static boolean isEventDispatchThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    static final ConcurrentHashMap<String, Integer> onDispatchCallerMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Integer> offDispatchCallerMap = new ConcurrentHashMap<>();

    /**
     * Run something on the dispatch thread that may throw a checked exception.
     *
     * @param r object with run method to call
     * @return future that provides info on when the method completes.
     */
    @SuppressWarnings("guieffect")
    public static SwingFuture<Void> runOnDispatchThreadWithCatch(final RunnableWithThrow r) {
//        assert !SwingUtilities.isEventDispatchThread();
        SwingFuture<Void> ret = new SwingFuture<>("runOnDispatchThreadWithCatch");
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        String callerString = trace[1].toString();
        try {
            if (isEventDispatchThread()) {
                if (RECORD_DISPATCH_CALLERS) {
                    onDispatchCallerMap.putIfAbsent(callerString, 1);
                }
                r.run();
                ret.complete(null);
                return ret;
            } else {
                if (RECORD_DISPATCH_CALLERS) {
                    offDispatchCallerMap.putIfAbsent(callerString, 1);
                }
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        r.run();
                        ret.complete(null);
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "", ex);
                        ret.completeExceptionally(ex);
                    }
                });
                return ret;
            }
        } catch (Throwable exception) {
            LOGGER.log(Level.SEVERE, "", exception);
            ret.completeExceptionally(exception);
            return ret;
        }
    }

    /**
     * Run something on the dispatch thread.
     *
     * @param r object with run method to call
     * @return future that provides info on when the method completes.
     */
    public static XFutureVoid runOnDispatchThread(final @UI Runnable r) {
        return runOnDispatchThread("runOnDispatchThread", r);
    }

    private static final boolean RECORD_DISPATCH_CALLERS = Boolean.getBoolean("aprs.recordDispatchCallers");

    private static final AtomicInteger dispathThreadExceptionCount = new AtomicInteger();

    public static void printOnlyOnDispatchCallers() {
        if (RECORD_DISPATCH_CALLERS) {
            Set<String> onDispatchCallers = onDispatchCallerMap.keySet();
            Set<String> offDispatchCallers = offDispatchCallerMap.keySet();
            Set<String> onlyOnDispatchCallers = new HashSet<>(onDispatchCallers);
            onlyOnDispatchCallers.removeAll(offDispatchCallers);
            System.err.println("");
            println("");
            println("BEGIN printOnlyOnDispatchCallers");
            println("");
            for (String caller : onlyOnDispatchCallers) {
                println("\tat " + caller);
            }
            println("");
            println("END printOnlyOnDispatchCallers");
            println("");
            System.err.println("");
        }
    }

    private final static ExecutorService dispatchThreadExecutorService = new ExecutorService() {
        @Override
        public void shutdown() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isShutdown() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isTerminated() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public <T> Future<T> submit(Callable<T> task) {
            CompletableFuture<T> cf = new CompletableFuture();
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    cf.complete(task.call());
                } catch (Exception ex) {
                    cf.completeExceptionally(ex);
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "", ex);
                    throw new RuntimeException(ex);
                }
            });
            return cf;
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public <T> Future<T> submit(Runnable task, T result) {
            CompletableFuture<T> cf = new CompletableFuture();
            javax.swing.SwingUtilities.invokeLater(() -> {
                task.run();
                cf.complete(result);
            });
            return cf;
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public Future<?> submit(Runnable task) {
            CompletableFuture cf = new CompletableFuture();
            javax.swing.SwingUtilities.invokeLater(() -> {
                task.run();
                cf.complete(null);
            });
            return cf;
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        @SuppressWarnings({"nullness","keyfor"})
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public void execute(Runnable command) {
            javax.swing.SwingUtilities.invokeLater(command);
        }

    };

    public static ExecutorService getDispatchThreadExecutorService() {
        return dispatchThreadExecutorService;
    }

    /**
     * Run something on the dispatch thread and attach a name to it for
     * debugging/logging/visualization.
     *
     * @param name optional name for better debugging/logging/visualization
     * @param r object with run method to call
     * @return future that provides info on when the method completes.
     */
    @SuppressWarnings("guieffect")
    public static XFutureVoid runOnDispatchThread(String name, final @UI Runnable r) {
//        assert !SwingUtilities.isEventDispatchThread();
        XFutureVoid ret = new XFutureVoid(name);
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        String callerString = trace[2].toString();
        if (callerString.contains("runOnDispatchThread")) {
            callerString = trace[3].toString();
        }
        if (isEventDispatchThread()) {
            try {
                if (RECORD_DISPATCH_CALLERS) {
                    onDispatchCallerMap.putIfAbsent(callerString, 1);
                }
                r.run();
                ret.complete();
            } catch (Exception e) {
                int count = dispathThreadExceptionCount.incrementAndGet();
                if (!(e instanceof PrintedException)) {
                    if (count < 2) {
                        LOGGER.log(Level.SEVERE, name, e);
                        showMessageDialog(null, "Exception " + count + " : " + e.getMessage());
                    }
                }
                ret.completeExceptionally(e);
            }
            return ret;
        } else {
            if (RECORD_DISPATCH_CALLERS) {
                offDispatchCallerMap.putIfAbsent(callerString, 1);
            }
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    r.run();
                    ret.complete(null);
                } catch (Exception e) {
                    int count = dispathThreadExceptionCount.incrementAndGet();
                    if (!(e instanceof PrintedException)) {
                        if (count < 2) {
                            LOGGER.log(Level.SEVERE, name, e);
                            showMessageDialog(null, "Exception " + count + " : " + e.getMessage());
                        }
                    }
                    ret.completeExceptionally(e);
                }
            });
            return ret;
        }
    }

    /**
     * Call a method that returns a value on the dispatch thread.
     *
     * @param s supplier object with get method to be called.
     * @return future that will make the return value accessible when the call
     * is complete.
     */
    @SuppressWarnings("guieffect")
    public static SwingFuture<XFutureVoid> supplyXVoidOnDispatchThread(final UiSupplier<XFutureVoid> s) {
        SwingFuture<XFutureVoid> ret = new SwingFuture<>("supplyOnDispatchThread");
        if (isEventDispatchThread()) {
            ret.complete(s.get());
            return ret;
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> ret.complete(s.get()));
            return ret;
        }
    }

    @UI
    @FunctionalInterface
    public static interface UiSupplier<R> {

        public R get();
    }

    /**
     * Call a method that returns a value on the dispatch thread.
     *
     * @param <R> type of return of the caller
     * @param s supplier object with get method to be called.
     * @return future that will make the return value accessible when the call
     * is complete.
     */
    @SuppressWarnings("guieffect")
    public static <R> SwingFuture<R> supplyOnDispatchThread(final UiSupplier<R> s) {
//        assert !SwingUtilities.isEventDispatchThread();
        SwingFuture<R> ret = new SwingFuture<>("supplyOnDispatchThread");
        if (isEventDispatchThread() || !CRCLUtils.isGraphicsEnvironmentHeadless()) {
            try {
                R val = s.get();
                ret.complete(val);
            } catch (Throwable e) {
                int count = dispathThreadExceptionCount.incrementAndGet();
                LOGGER.log(Level.SEVERE, "", e);
                if (count < 2) {
                    showMessageDialog(null, "Exception " + count + " : " + e.getMessage());
                }
                ret.completeExceptionally(e);
            }
            return ret;
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    R val = s.get();
                    ret.complete(val);
                } catch (Throwable e) {
                    int count = dispathThreadExceptionCount.incrementAndGet();
                    LOGGER.log(Level.SEVERE, "", e);
                    if (count < 2) {
                        showMessageDialog(null, "Exception " + count + " : " + e.getMessage());
                    }
                    ret.completeExceptionally(e);
                }
            });
            return ret;
        }
    }

    private static @Nullable
    <R> R unwrap(XFuture<R> f) {
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            return null;
        }
    }

    /**
     * Call a method that returns a future of a value on the dispatch thread.
     *
     * @param <R> type of return of the caller
     * @param s supplier object with get method to be called.
     * @return future that will make the return value accessible when the call
     * is complete.
     */
    @SuppressWarnings({"nullness", "guieffect"})
    public static <R> XFuture<R> composeOnDispatchThread(final UiSupplier<? extends XFuture<R>> s) {
//        assert !SwingUtilities.isEventDispatchThread();
        if (isEventDispatchThread()) {
            return s.get();
        } else {
            XFuture<R> ret = new SwingFuture<>("composeOnDispatchThread");
            javax.swing.SwingUtilities.invokeLater(() -> {
                final XFuture<R> sget = s.get();
                sget.thenAccept((R r) -> {
                    ret.complete(r);
                });
            });
            return ret;
        }
    }

    /**
     * Call a method that returns a future of a void on the dispatch thread.
     *
     * @param s supplier object with get method to be called.
     * @return future that will make the return value accessible when the call
     * is complete.
     */
    @SuppressWarnings({"nullness", "guieffect"})
    public static XFutureVoid composeToVoidOnDispatchThread(final UiSupplier<? extends XFutureVoid> s) {
//        assert !SwingUtilities.isEventDispatchThread();
        if (isEventDispatchThread()) {
            return s.get();
        } else {
            XFutureVoid ret = new XFutureVoid("composeOnDispatchThread");
            javax.swing.SwingUtilities.invokeLater(() -> {
                XFutureVoid sret = s.get();
                sret.thenRun(() -> {
                    ret.complete();
                });
            });
            return ret;
        }
    }

    /**
     * Adjust the widths of each column of a table to match the max width of
     * each value in the table.
     *
     * @param table table to be resized
     */
    public static void autoResizeTableColWidths(@Nullable JTable table) {
        if (null != table) {
            Utils.runOnDispatchThread(() -> autoResizeTableColWidthsOnDisplay(table));
        }
    }

    /**
     * Adjust the widths of each column of a table to match the max width of
     * each value in the table.
     *
     * @param table table to be resized
     */
    public static void autoResizeTableColWidths(CachedTable table) {
        JTable jTable = table.getjTable();
        if (null != jTable) {
            final JTable jTableNonNull = jTable;
            Utils.runOnDispatchThread(() -> autoResizeTableColWidthsOnDisplay(jTableNonNull));
        }
    }

    /**
     * Adjust the widths of each column of a table to match the max width of
     * each value in the table.
     *
     * @param table table to be resized
     */
    @UIEffect
    public static void autoResizeTableColWidthsOnDisplay(@Nullable JTable table) {

        if (null == table) {
            return;
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int fullsize = 0;
        Container parent = table.getParent();
        if (null != parent) {
            fullsize = Math.max(parent.getPreferredSize().width, parent.getSize().width);
        }
        int sumWidths = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn col = colModel.getColumn(i);
            int width = 0;

            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Object colHeaderVal = col.getHeaderValue();
            Component headerComp = renderer.getTableCellRendererComponent(table, colHeaderVal,
                    false, false, 0, i);
            if (null != headerComp && null != headerComp.getPreferredSize()) {
                width = Math.max(width, headerComp.getPreferredSize().width);
            } else {
                System.err.println("table has invalid renderer for header (" + i + ") colHeaderVal=" + colHeaderVal);
            }
            for (int r = 0; r < table.getRowCount(); r++) {
                try {
                    renderer = table.getCellRenderer(r, i);
                    if (r >= table.getRowCount()) {
                        System.err.println("autoResizeTableColWidthsOnDisplay rowCount changed.");
                        return;
                    }
                    if (i >= table.getColumnCount()) {
                        System.err.println("autoResizeTableColWidthsOnDisplay colCount changed.");
                        return;
                    }
                    Object tableValue = table.getValueAt(r, i);
                    if (null != tableValue) {
                        Component comp = renderer.getTableCellRendererComponent(table,
                                tableValue,
                                false, false, r, i);
                        if (null != comp && null != comp.getPreferredSize()) {
                            width = Math.max(width, comp.getPreferredSize().width);
                        } else {
                            System.err.println("table has invalid renderer for cell (" + r + "," + i
                                    + ")  colHeaderVal=" + colHeaderVal + ", tableValue=" + tableValue);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("r=" + r + ",i=" + i + ",table.getRowCount()=" + table.getRowCount()
                            + ",table.getColumnCount()=" + table.getColumnCount(), e);
                }
            }
            if (i == table.getColumnCount() - 1) {
                if (width < fullsize - sumWidths) {
                    width = fullsize - sumWidths;
                }
            }
            col.setPreferredWidth(width + 2);
            sumWidths += width + 2;
        }
    }

    /**
     * Adjust the heights of each row of a table to match the max height of each
     * value in the table.
     *
     * @param table table to be resized
     */
    static public void autoResizeTableRowHeights(CachedTable table) {
        JTable jTable = table.getjTable();
        if (null != jTable) {
            JTable jTableNonNull = jTable;
            runOnDispatchThread(() -> autoResizeTableRowHeightsOnDisplay(jTableNonNull));
        }
    }

    /**
     * Adjust the heights of each row of a table to match the max height of each
     * value in the table.
     *
     * @param table table to be resized
     */
    static public void autoResizeTableRowHeights(JTable table) {
        runOnDispatchThread(() -> autoResizeTableRowHeightsOnDisplay(table));
    }

    /**
     * Adjust the heights of each row of a table to match the max height of each
     * value in the table.
     *
     * @param table table to be resized
     */
    @UIEffect
    static public void autoResizeTableRowHeightsOnDisplay(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
            int height = 0;
            for (int colIndex = 0; colIndex < table.getColumnCount(); colIndex++) {
                DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
                TableCellRenderer renderer = table.getCellRenderer(rowIndex, colIndex);
                Object value = table.getValueAt(rowIndex, colIndex);
                if (null != value) {
                    Component comp = renderer.getTableCellRendererComponent(table, value,
                            false, false, rowIndex, colIndex);
                    Dimension compSize = comp.getPreferredSize();
                    int thisCompHeight = compSize.height;
                    height = Math.max(height, thisCompHeight);
                }
            }
            if (height > 0) {
                table.setRowHeight(rowIndex, height);
            }
        }
    }

    /**
     * Save a set of properties to a file, with a replacement for the
     * backslashes in windows filenames.
     *
     * @param file file to save
     * @param props properties to save
     */
    public static void saveProperties(File file, Properties props) {
        List<Object> names = new ArrayList<>();
        for (Object key : props.keySet()) {
            names.add(key);
        }
        Collections.sort(names, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        StackTraceElement ste[] = Thread.currentThread().getStackTrace();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            if (ste.length > 2) {
                pw.println("#  Automatically saved ");
            }
            for (int i = 0; i < names.size(); i++) {
                Object name = names.get(i);
                Object value = props.get(name);
                if (null != value) {
                    if (value instanceof String) {
                        value = ((String) value).replaceAll("\\\\", Matcher.quoteReplacement("\\\\"));
                    }
                    pw.println(name + "=" + value);
                } else {
                    pw.println("# " + name + "=null");
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    /**
     * Convert the table model column names to an array of strings.
     *
     * @param jTable table to get column names from
     * @return array of strings with column names
     */
    @UIEffect
    public static String[] tableHeaders(JTable jTable) {
        TableModel tm = jTable.getModel();
        return tableHeaders(tm);
    }

    /**
     * Convert the table model column names to an array of strings.
     *
     * @param tm TableModel to get column names from
     * @return array of strings with column names
     */
    @UIEffect
    public static String[] tableHeaders(TableModel tm) {
        String headers[] = new String[tm.getColumnCount()];
        for (int i = 0; i < tm.getColumnCount() && i < headers.length; i++) {
            headers[i] = tm.getColumnName(i);
        }
        return headers;
    }

    /**
     * Convert the table model column names to an array of strings.
     *
     * @param cachedTable table to get column names from
     * @return array of strings with column names
     */
    public static String[] tableHeaders(CachedTable cachedTable) {
        String headers[] = new String[cachedTable.getColumnCount()];
        for (int i = 0; i < cachedTable.getColumnCount() && i < headers.length; i++) {
            headers[i] = cachedTable.getColumnName(i);
        }
        return headers;
    }

    public static String[] tableHeaders(CachedTable cachedTable, Iterable<Integer> columnIndexes) {
        List<String> colNameList = new ArrayList<>();
        for (Integer colIndex : columnIndexes) {
            if (colIndex == null) {
                throw new IllegalArgumentException("columnIndexe contains null : " + columnIndexes);
            }
            int i = colIndex;
            if (i < 0 || i > cachedTable.getColumnCount()) {
                throw new IllegalArgumentException("columnIndexes contains " + i + " outside range 0 to "
                        + cachedTable.getColumnCount() + " : " + columnIndexes);
            }
            colNameList.add(cachedTable.getColumnName(i));
        }
        return colNameList.toArray(new String[0]);
    }

    /**
     * Convert the table model column names to an array of strings.
     *
     * @param jtable table to get column names from
     * @return array of strings with column names
     */
    @UIEffect
    public static String[] tableHeaders(JTable jtable, Iterable<Integer> columnIndexes) {
        TableModel tm = jtable.getModel();
        List<String> colNameList = new ArrayList<>();
        for (Integer colIndex : columnIndexes) {
            if (colIndex == null) {
                throw new IllegalArgumentException("columnIndexe contains null : " + columnIndexes);
            }
            int i = colIndex;
            if (i < 0 || i > tm.getColumnCount()) {
                throw new IllegalArgumentException("columnIndexes contains " + i + " outside range 0 to "
                        + tm.getColumnCount() + " : " + columnIndexes);
            }
            colNameList.add(tm.getColumnName(i));
        }
        return colNameList.toArray(new String[0]);
    }

    /**
     * Save a JTable to a file.
     *
     * @param f file the save table data to
     * @param cachedTable table to get data from
     * @throws IOException file could not be written
     */
    public static void saveCachedTable(File f, CachedTable cachedTable) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)),
                CSVFormat.DEFAULT.withHeader(tableHeaders(cachedTable)))) {
            List<String> colNameList = new ArrayList<>();
            for (int i = 0; i < cachedTable.getColumnCount(); i++) {
                colNameList.add(cachedTable.getColumnName(i));
            }
            for (int i = 0; i < cachedTable.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < cachedTable.getColumnCount(); j++) {
                    Object o = cachedTable.getValueAt(i, j);
                    if (o == null) {
                        l.add("");
                    } else if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath()
                                    .relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else {
                        l.add(o);
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    /**
     * Save a JTable to a file.
     *
     * @param f file the save table data to
     * @param jtable table to get data from
     * @throws IOException file could not be written
     */
    @UIEffect
    public static void saveJTable(File f, JTable jtable) throws IOException {
        TableModel tm = jtable.getModel();
        saveTableModel(f, tm);
    }

    /**
     * Save a JTable to a file.
     *
     * @param f file the save table data to
     * @param tm TableModel to get data from
     * @throws IOException file could not be written
     */
    @UIEffect
    public static void saveTableModel(File f, TableModel tm) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)),
                CSVFormat.DEFAULT.withHeader(tableHeaders(tm)))) {

            List<String> colNameList = new ArrayList<>();
            for (int i = 0; i < tm.getColumnCount(); i++) {
                colNameList.add(tm.getColumnName(i));
            }
            for (int i = 0; i < tm.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < tm.getColumnCount(); j++) {
                    Object o = tm.getValueAt(i, j);
                    if (o == null) {
                        l.add("");
                    }
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath()
                                    .relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else {
                        l.add(o);
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    /**
     * Convert a run time in milliseconds to a formatted string.
     *
     * @param runningTimeMillis time in milliseconds
     * @return formatted string
     */
    static public String runTimeToString(long runningTimeMillis) {
        long runningTimeSecondsTotal = runningTimeMillis / 1000;
        long runningTimeHours = runningTimeSecondsTotal / 3600;
        long runningTimeMinutes = (runningTimeSecondsTotal % 3600) / 60;
        long runningTimeSeconds = (runningTimeSecondsTotal % 60);
        return String.format("%02d:%02d:%02d",
                runningTimeHours,
                runningTimeMinutes,
                runningTimeSeconds)
                + " (" + runningTimeSecondsTotal + " s)";
    }

    /**
     * Convert an array that may contain null to a possibly shorter array with
     * only the nonnull values.
     *
     * @param <T> class of the array members
     * @param clzz class of the array members
     * @param in array to read
     * @return array with only non null values from original array
     */
    public static <T> T[] copyOfNonNullsOnly(Class<T> clzz, T[] in) {
        return copyOfRangeNonNullsOnly(clzz, in, 0, in.length);
    }

    /**
     * Convert a subset of an array that may contain null to a possibly shorter
     * array with only the nonnull values.
     *
     * @param <T> class of the array members
     * @param clzz class of the array members
     * @param in array to read
     * @param start index to start reading input array
     * @param end index to end reading input array
     * @return array with only non null values from original array sub range
     */
    public static <T> T[] copyOfRangeNonNullsOnly(Class<T> clzz, T[] in, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start must be less than or equal to end : start = " + start + ", end = "
                    + end + " for array =" + Arrays.toString(in));
        }
        if (start < 0) {
            throw new IllegalArgumentException("start must not be less 0:  start = " + start + ", end = " + end
                    + " for array =" + Arrays.toString(in));
        }
        if (end > in.length) {
            throw new IllegalArgumentException("end must be less than size start = " + start + ", end = " + end
                    + ", in.length=" + in.length + " for array =" + Arrays.toString(in));
        }
        int nonNulls = 0;
        for (int i = start; i < end; i++) {
            if (in[i] != null) {
                nonNulls++;
            }
        }
        @SuppressWarnings("unchecked")
        T out[] = (T[]) Array.newInstance(clzz, nonNulls);
        int j = 0;
        for (int i = start; i < end; i++) {
            T o = in[i];
            if (o != null) {
                out[j] = o;
                j++;
            }
        }
        return out;
    }

    @SuppressWarnings({"guieffect", "nullness"})
    public static void showMessageDialog(@Nullable Component component, String message) {
        if (null == message || message.trim().length() < 1) {
            throw new IllegalArgumentException("message=" + message);
        }
        String msgCopy = message;
        if (msgCopy.length() > 400) {
            msgCopy = msgCopy.substring(0, 396) + " ...";
        }
        for (int i = 80; i < msgCopy.length(); i += 80) {
            msgCopy = msgCopy.substring(0, i) + "\r\n" + msgCopy.substring(i);
        }
        if (isEventDispatchThread()) {
            MultiLineStringJPanel.showText(msgCopy);
        } else {
            try {
                final String msgCopyFinal = msgCopy;
                javax.swing.SwingUtilities.invokeLater(() -> MultiLineStringJPanel.showText(msgCopyFinal));
            } catch (Exception ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    @UIEffect
    public static void readCsvFileToTable(boolean forceColumns, JTable jtable, File f) {
        if (forceColumns && !(jtable.getModel() instanceof DefaultTableModel)) {
            jtable.setModel(new DefaultTableModel());
        }
        readCsvFileToTableAndMap(forceColumns, (DefaultTableModel) jtable.getModel(), f, null, null, null);
        Utils.autoResizeTableColWidths(jtable);

    }

    @UIEffect
    public static <T> void readCsvFileToTableAndMap(boolean forceColumns, @Nullable DefaultTableModel dtm, File f,
            @Nullable String nameRecord, @Nullable Map<String, T> map, @Nullable Function<CSVRecord, T> recordToValue) {

        if (null != dtm) {
            dtm.setRowCount(0);
        }
        try (CSVParser parser = new CSVParser(new FileReader(f), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (forceColumns && null != dtm) {
                dtm.setRowCount(0);
                dtm.setColumnCount(0);
                for (String key : headerMap.keySet()) {
                    dtm.addColumn(key);
                }
            }
            List<CSVRecord> records = parser.getRecords();
            int skipRows = 0;
            if (null != dtm) {
                for (CSVRecord rec : records) {
                    String colName = dtm.getColumnName(0);
                    Integer colIndex = headerMap.get(colName);
                    if (colIndex == null) {
                        throw new IllegalArgumentException(f + " does not have field " + colName);
                    }
                    String val0 = rec.get(colIndex);
                    if (!val0.equals(colName) && val0.length() > 0) {
                        break;
                    }
                    if (val0.length() < 1) {
                        LOGGER.log(Level.WARNING,
                                "skipping record with empty name field : rec=" + rec + " in file="
                                + f.getCanonicalPath() + ", colName=" + colName + ", val0=" + val0
                                + ",skipRows=" + skipRows);
                    }
                    skipRows++;
                }
                dtm.setRowCount(records.size() - skipRows);
            }
            ROW_LOOP:
            for (int i = skipRows; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                if (null != dtm) {
                    for (int j = 0; j < dtm.getColumnCount(); j++) {
                        String colName = dtm.getColumnName(j);
                        Integer colIndex = headerMap.get(colName);
                        if (colIndex == null) {
                            continue ROW_LOOP;
                        }
                        String val = rec.get(colIndex);
                        try {
                            if (null != val) {
                                if (val.equals(colName) || (j == 0 && val.length() < 1)) {
                                    continue ROW_LOOP;
                                }
                                Class<?> colClass = dtm.getColumnClass(j);
                                if (colClass == Double.class) {
                                    dtm.setValueAt(Double.valueOf(val), i - skipRows, j);
                                } else if (colClass == Boolean.class) {
                                    dtm.setValueAt(Boolean.valueOf(val), i - skipRows, j);
                                } else {
                                    dtm.setValueAt(val, i - skipRows, j);
                                }
                            }
                        } catch (Exception exception) {
                            String msg = "colName=" + colName + ", colIndex=" + colIndex + ", val=" + val + ", rec="
                                    + rec;
                            LOGGER.log(Level.SEVERE, msg, exception);
                            throw new RuntimeException(msg, exception);
                        }
                    }
                }
                try {
                    if (null != nameRecord && null != map && null != recordToValue) {
                        String name = rec.get(nameRecord);
                        T value = recordToValue.apply(rec);
                        map.put(name, value);
                    }
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "rec=" + rec, exception);
                    throw new RuntimeException(exception);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,
                    "forceColumns=" + forceColumns
                    + ", dtm=" + dtm
                    + ", f=" + f
                    + ", nameRecord=" + nameRecord
                    + ", map=" + map
                    + ", recordToValue=" + recordToValue,
                    ex);
            throw new RuntimeException(ex);
        }
    }

    public static void readCsvFileToTable(CachedTable cachedTable, File f) {
        readCsvFileToTableAndMap(cachedTable, f, null, null, null);
    }

    public static void saveTestLogEntry(File f, boolean alreadyExists, int cycle_count, long timeDiff,
            long timeDiffPerCycle, int disableCount, long disableTime, long totalRandomDelays, int ignoredToggles) {
        final CSVFormat format;
        if (alreadyExists) {
            format = CSVFormat.DEFAULT;
        } else {
            format = CSVFormat.DEFAULT.withHeader(
                    "Date",
                    "cycle_count",
                    "timeDiff",
                    "timeDiffPerCycle",
                    "disableCount",
                    "disableTime",
                    "totalRandomDelays",
                    "ignoredToggles");
        }
        try (CSVPrinter printer = new CSVPrinter(
                new FileWriter(f, alreadyExists), format)) {
            printer.printRecord(
                    Utils.getDateTimeString(),
                    cycle_count,
                    timeDiff,
                    timeDiffPerCycle,
                    disableCount,
                    disableTime,
                    totalRandomDelays,
                    ignoredToggles);
        } catch (IOException ex) {
            Logger.getLogger(LauncherAprsJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    public static <T> void readCsvFileToTableAndMap(CachedTable cachedTable, File f, @Nullable String nameRecord,
            @Nullable Map<String, T> map, @Nullable Function<CSVRecord, T> recordToValue) {

        if (null != cachedTable) {
            cachedTable.setRowCount(0);
        }
        try (CSVParser parser = new CSVParser(new FileReader(f), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            List<CSVRecord> records = parser.getRecords();
            int skipRows = 0;
            if (null != cachedTable) {
                for (CSVRecord rec : records) {
                    String colName = cachedTable.getColumnName(0);
                    Integer colIndex = headerMap.get(colName);
                    if (colIndex == null) {
                        throw new IllegalArgumentException(f + " does not have field " + colName);
                    }
                    String val0 = rec.get(colIndex);
                    if (!val0.equals(colName) && val0.length() > 0) {
                        break;
                    }
                    if (val0.length() < 1) {
                        LOGGER.log(Level.WARNING,
                                "skipping record with empty name field : rec=" + rec + " in file="
                                + f.getCanonicalPath() + ", colName=" + colName + ", val0=" + val0
                                + ",skipRows=" + skipRows);
                    }
                    skipRows++;
                }
                cachedTable.setRowCount(records.size() - skipRows);
            }
            ROW_LOOP:
            for (int i = skipRows; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                if (null != cachedTable) {
                    for (int j = 0; j < cachedTable.getColumnCount(); j++) {
                        String colName = cachedTable.getColumnName(j);
                        Integer colIndex = headerMap.get(colName);
                        if (colIndex == null) {
                            continue ROW_LOOP;
                        }
                        String val = rec.get(colIndex);
                        try {
                            if (null != val) {
                                if (val.equals(colName) || (j == 0 && val.length() < 1)) {
                                    continue ROW_LOOP;
                                }
                                Class<?> colClass = cachedTable.getColumnClass(j);
                                if (colClass == Double.class) {
                                    cachedTable.setValueAt(Double.valueOf(val), i - skipRows, j);
                                } else if (colClass == Boolean.class) {
                                    cachedTable.setValueAt(Boolean.valueOf(val), i - skipRows, j);
                                } else {
                                    cachedTable.setValueAt(val, i - skipRows, j);
                                }
                            }
                        } catch (Exception exception) {
                            String msg = "colName=" + colName + ", colIndex=" + colIndex + ", val=" + val + ", rec="
                                    + rec;
                            LOGGER.log(Level.SEVERE, msg, exception);
                            throw new RuntimeException(msg, exception);
                        }
                    }
                }
                try {
                    if (null != nameRecord && null != map && null != recordToValue) {
                        String name = rec.get(nameRecord);
                        T value = recordToValue.apply(rec);
                        map.put(name, value);
                    }
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "rec=" + rec, exception);
                    throw new RuntimeException(exception);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    public static String shortenItemPartName(String itemName) {
        if (null == itemName) {
            throw new NullPointerException("itemName");
        }
        String shortItemName = itemName;
        if (shortItemName.startsWith("sku_")) {
            shortItemName = shortItemName.substring(4);
        }
        if (shortItemName.startsWith("part_")) {
            shortItemName = shortItemName.substring(5);
        }
        return shortItemName;
    }

    private static final String APRS_LOOK_AND_FEEL = System.getProperty("aprs.lookAndFeel", "Nimbus");

    public static void setToAprsLookAndFeel() {
        /* Set the preferred look and feel */

 /*
	 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
	 * look and feel. For details see
	 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                final String infoName = info.getName();
                if (APRS_LOOK_AND_FEEL.equals(infoName)) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AprsSystem.class
                    .getName()).log(java.util.logging.Level.SEVERE, "", ex);

        }
    }
}
