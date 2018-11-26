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

import aprs.logdisplay.LogDisplayJPanel;
import aprs.misc.Utils;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class LaunchFileRunner {

    private volatile boolean stopLineSeen = false;

    private volatile List<String> stopLines = new ArrayList<>();

    @Nullable
    private volatile File processLaunchDirectory = null;

    @Nullable
    private volatile String onFailLine = null;
    @Nullable
    private volatile XFutureVoid waitForFuture = null;

    private final Deque<Boolean> ifStack = new ArrayDeque<>();

    @Nullable
    private final ProcessLauncherJFrame processLauncherJFrame;

    private volatile boolean debug = false;

    private final List<WrappedProcess> processes = new ArrayList<>();

    public Deque<Boolean> getIfStack() {
        return ifStack;
    }

    @Nullable
    public ProcessLauncherJFrame getProcessLauncherJFrame() {
        return processLauncherJFrame;
    }

    public List<WrappedProcess> getProcesses() {
        return processes;
    }

    private volatile List<LineConsumer> lineConsumers = new ArrayList<>();
    private volatile List<LineConsumer> errorLineConsumers = new ArrayList<>();

    public LaunchFileRunner() {
        this.processLauncherJFrame = null;
    }

    public LaunchFileRunner(ProcessLauncherJFrame frm) {
        this.processLauncherJFrame = frm;
    }

    public WrappedProcess addProcess(String... command) {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String cmdLine = String.join(" ", command);
        if (null != processLauncherJFrame) {
            Utils.runOnDispatchThread(() -> {
                if (null != processLauncherJFrame) {
                    processLauncherJFrame.getjTabbedPaneProcesses().add(cmdLine, logPanel);
                }
            });
        }
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(errPrintStream, errPrintStream, command);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    WrappedProcess addProcess(List<String> command) {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String cmdLine = String.join(" ", command);
        if (null != processLauncherJFrame) {
            Utils.runOnDispatchThread(() -> {
                if (null != processLauncherJFrame) {
                    processLauncherJFrame.getjTabbedPaneProcesses().add(cmdLine, logPanel);
                }
            });
        }
        List<LineConsumer> lineConsumers = getLineConsumers();
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(errPrintStream, errPrintStream, command);
        wrappedProcess.setDisplayComponent(logPanel);
        List<WrappedProcess> processes = getProcesses();
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    WrappedProcess addProcess(File directory, String... command) {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String[] command2 = replaceDotDir(directory, command);
        String cmdLine = String.join(" ", command2);
        if (null != processLauncherJFrame) {
            Utils.runOnDispatchThread(() -> {
                if (null != processLauncherJFrame) {
                    processLauncherJFrame.getjTabbedPaneProcesses().add(cmdLine, logPanel);
                }
            });
        }
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(directory, errPrintStream, errPrintStream, command2);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }
    private volatile long timeoutStart = -1;

    public long getTimeoutStart() {
        return timeoutStart;
    }

    public void setTimeoutStart(long timeoutStart) {
        this.timeoutStart = timeoutStart;
    }

    @MonotonicNonNull
    private volatile ScheduledThreadPoolExecutor timeoutScheduledThreadPoolExecutor = null;

    private volatile XFutureVoid lastNewTimeoutFuture = null;
    private volatile javax.swing.Timer lastTimeoutSwingTimer = null;

    private void completeTimeoutFuture(XFutureVoid future, long timeOutStart) {
        long curTime = System.currentTimeMillis();
        long diff = curTime = timeOutStart;
        System.out.println("Completing " + future + " after " + diff + " ms");
        future.complete();
    }

    @SuppressWarnings("guieffect")
    XFutureVoid newTimeoutFuture() {
        int timeoutMillisLocal = timeoutMillis;
        if (timeoutMillisLocal < 1) {
            throw new IllegalStateException("timeoutMillis=" + timeoutMillisLocal);
        }
        XFutureVoid ret = new XFutureVoid("timeoutFuture");
        long timeoutStartLocal = System.currentTimeMillis();
        this.timeoutStart = timeoutStartLocal;
        if (GraphicsEnvironment.isHeadless()) {
            if (timeoutScheduledThreadPoolExecutor == null) {
                timeoutScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
            }
            timeoutScheduledThreadPoolExecutor.schedule(() -> completeTimeoutFuture(ret, timeoutStartLocal), timeoutMillisLocal, TimeUnit.MILLISECONDS);
        } else {
            Utils.runOnDispatchThread(() -> {
                javax.swing.Timer timer = new Timer(timeoutMillisLocal, (evt) -> {
                    completeTimeoutFuture(ret, timeoutStartLocal);
                });
                lastTimeoutSwingTimer = timer;
                timer.setRepeats(false);
                timer.setInitialDelay(timeoutMillisLocal);
                timer.start();
            });
        }
        lastNewTimeoutFuture = ret;
        return ret;
    }

    private static String replaceDotDir(File dir, String in) {
        if (!in.startsWith(".")) {
            return in;
        }
        if (in.startsWith("./") || in.startsWith(".\\")) {
            return dir.toString() + in.substring(1);
        }
        String tmpIn = in;
        File parentFile = dir;
        while ((tmpIn.startsWith("../") || tmpIn.startsWith("..\\")) && parentFile != null) {
            tmpIn = tmpIn.substring(3);
            parentFile = parentFile.getParentFile();
        }
        if (null != parentFile && tmpIn.length() > 0 && tmpIn.length() < in.length()) {
            return parentFile.toString() + File.separator + tmpIn;
        }
        return in;
    }

    private static String[] replaceDotDir(File dir, String in[]) {
        for (int i = 0; i < in.length; i++) {
            in[i] = replaceDotDir(dir, in[i]);
        }
        return in;
    }

    private static List<String> replaceDotDir(File dir, List<String> in) {
        for (int i = 0; i < in.size(); i++) {
            in.set(i, replaceDotDir(dir, in.get(i)));
        }
        return in;
    }

    private WrappedProcess addProcess(File directory, List<String> command) {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        List<String> command2 = replaceDotDir(directory, command);
        String cmdLine = String.join(" ", command2);
        if (null != processLauncherJFrame) {
            Utils.runOnDispatchThread(() -> {
                if (null != processLauncherJFrame) {
                    processLauncherJFrame.getjTabbedPaneProcesses().add(cmdLine, logPanel);
                }
            });
        }
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(directory, errPrintStream, errPrintStream, command2);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    public static String[] parseCommandLineToArray(String line) {
        List<String> args = parseCommandLine(line);
        return args.toArray(new String[0]);
    }

    private static List<String> parseCommandLine(String line) {
        List<String> args = new ArrayList<>();
        int dquotes = 0;
        int squotes = 0;
        StringBuilder sb = new StringBuilder();
        char lastC = 0;
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            switch (c) {
                case ' ':
                case '\t':
                    if (dquotes % 2 == 0 && squotes % 2 == 0 && lastC != '\\') {
                        args.add(sb.toString());
                        sb = new StringBuilder();
                    } else {
                        sb.append(c);
                    }
                    break;

                case '\"':
                    if (squotes % 2 == 0 && lastC != '\\') {
                        dquotes++;
                    } else {
                        sb.append(c);
                    }
                    break;

                case '\'':
                    if (dquotes % 2 == 0 && lastC != '\\') {
                        squotes++;
                    } else {
                        sb.append(c);
                    }
                    break;

                case '\\':
                    if (lastC == '\\' || isWindows) {
                        sb.append(c);
                        c = 0;
                    }
                    break;

                default:
                    sb.append(c);
                    break;
            }
            lastC = c;
        }
        String last = sb.toString();
        if (last.length() > 0) {
            args.add(last);
        }
        return args;
    }

    private static String afterFirstWord(String line, String firstWord) {
        return line.substring(line.indexOf(firstWord) + firstWord.length()).trim();
    }

    private static String replaceVarsInLine(String line, String startString, @Nullable String endString) {
        int varStartIndex = line.indexOf(startString);
        int endStringLength = (null != endString) ? endString.length() : 0;
        int startStringLength = startString.length();
        while (varStartIndex >= 0) {

            int varEndIndex
                    = (endString != null)
                            ? line.indexOf(endString, varStartIndex + startStringLength)
                            : line.length();
            if (varEndIndex <= varStartIndex) {
                break;
            }
            String substring = line.substring(varStartIndex + startStringLength, varEndIndex);
            boolean isidentifier = true;
            for (int i = 0; i < substring.length(); i++) {
                char c = substring.charAt(i);
                if (i == 0 && !Character.isLetter(c)) {
                    isidentifier = false;
                    break;
                }
                if (c != '.' && c != '_' && !Character.isLetterOrDigit(c)) {
                    isidentifier = false;
                    break;
                }
            }
            if (isidentifier) {
                String env = System.getenv(substring);
                if (null == env) {
                    env = System.getProperty(substring);
                }
                if (null != env && env.length() > 0) {
                    String linestart = line.substring(0, varStartIndex);
                    String lineend = line.substring(varEndIndex + endStringLength);
                    line = linestart + env + lineend;
                    varEndIndex = linestart.length() + env.length();
                }
            }
            varStartIndex = line.indexOf(startString, varEndIndex + 1);
        }
        return line;
    }

    private String tabs = "";

    private boolean allIfStack() {
        for (boolean val : ifStack) {
            if (!val) {
                return false;
            }
        }
        return true;
    }

    public boolean isStopLineSeen() {
        return stopLineSeen;
    }

    public void setStopLineSeen(boolean stopLineSeen) {
        this.stopLineSeen = stopLineSeen;
    }

    public List<String> getStopLines() {
        return stopLines;
    }

    public void setStopLines(List<String> stopLines) {
        this.stopLines = stopLines;
    }

    void completeClose() {
        List<WrappedProcess> stopProcesses = new ArrayList<>();
        List<XFuture<?>> futures = new ArrayList<>();
        stopLineSeen = false;
        processLaunchDirectory = null;
        for (String line : stopLines) {
            try {
                WrappedProcess p = parseLaunchFileLine(line, futures, null);
                if (null != p) {
                    stopProcesses.add(p);

                }
            } catch (IOException ex) {
                Logger.getLogger(ProcessLauncherJFrame.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
        for (WrappedProcess p : stopProcesses) {
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.close();

                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ProcessLauncherJFrame.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
        for (WrappedProcess wp : this.processes) {
            wp.close();
        }
        WrappedProcess.shutdownStarterService();

    }

    @Nullable
    public File getProcessLaunchDirectory() {
        return processLaunchDirectory;
    }

    public void setProcessLaunchDirectory(@Nullable File processLaunchDirectory) {
        this.processLaunchDirectory = processLaunchDirectory;
    }

    @Nullable
    public String getOnFailLine() {
        return onFailLine;
    }

    public void setOnFailLine(String onFailLine) {
        this.onFailLine = onFailLine;
    }

    @Nullable
    public XFutureVoid getWaitForFuture() {
        return waitForFuture;
    }

    public void setWaitForFuture(XFutureVoid waitForFuture) {
        this.waitForFuture = waitForFuture;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<LineConsumer> getLineConsumers() {
        return lineConsumers;
    }

    public void setLineConsumers(List<LineConsumer> lineConsumers) {
        this.lineConsumers = lineConsumers;
    }

    public List<LineConsumer> getErrorLineConsumers() {
        return errorLineConsumers;
    }

    public void setErrorLineConsumers(List<LineConsumer> errorLineConsumers) {
        this.errorLineConsumers = errorLineConsumers;
    }

    public String getTabs() {
        return tabs;
    }

    public void setTabs(String tabs) {
        this.tabs = tabs;
    }

    @Nullable
    @SuppressWarnings("nullness")
    private WrappedProcess parseLaunchFileLine(String line, List<? super XFuture<?>> futures, @Nullable StringBuilder stringBuilder) throws IOException {
        if (line.length() < 1) {
            if (null != stringBuilder) {
                stringBuilder.append("\n");
            }
            return null;
        }

        if (debug) {
            System.out.println("line = " + line);
            System.out.println("ifStack.size() = " + ifStack.size());
        }
        String currentOnFailLine = onFailLine;
        XFutureVoid currentWaitForFuture = waitForFuture;
        List<LineConsumer> currentErrorLineConsumers = errorLineConsumers;

        line = line.trim();
        if (line.length() < 1) {
            if (null != stringBuilder) {
                stringBuilder.append("\n");
            }
            return null;
        }
        line = replaceVarsInLine(line, "%", "%");
        line = replaceVarsInLine(line, "${", "}");
        line = replaceVarsInLine(line, "$", " ");
        line = replaceVarsInLine(line, "$", "\n");
        line = replaceVarsInLine(line, "$", "\r");
        line = replaceVarsInLine(line, "$", null);

        if (stopLineSeen) {
            stopLines.add(line);
            if (null != stringBuilder) {
                stringBuilder.append("\t\tSTOP_LINE:\t\t");
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            return null;
        }
        String newTabs = tabs;
        boolean cmdsProcessed = false;
        try {
            String words[] = line.split("[ \t\r\n]+");
            if (words.length < 1) {
                return null;
            }
            String firstWord = words[0];
            if (firstWord.equals("if!connectOK")) {

                String parts[] = Arrays.copyOfRange(words, 1, words.length);
                if (parts.length >= 2) {
                    try {
                        Socket s = new Socket(parts[0], Integer.parseInt(parts[1]));
                        ifStack.push(false);
                    } catch (Exception e) {
//                         Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, "", e);
                        ifStack.push(true);
                    }
                    newTabs = tabs + "    ";
                }
                return null;
            } else if (firstWord.equals("else")) {
                ifStack.push(!ifStack.pop());
                return null;
            } else if (firstWord.equals("endif")) {
                if (tabs.length() > 4) {
                    newTabs = tabs.substring(4);
                    tabs = newTabs;
                } else if (tabs.length() == 4) {
                    newTabs = "";
                    tabs = newTabs;
                }
                ifStack.pop();
                return null;
            }
            cmdsProcessed = true;

            if (!allIfStack()) {
                return null;
            }

            if (firstWord.equals("plj-recoverWaitFor")) {
                String text = afterFirstWord(line, firstWord);
                final List<LineConsumer> containingList = errorLineConsumers;
                LineConsumer consumer = new LineConsumer() {

                    private volatile boolean finished = false;

                    @Override
                    public void accept(String s) {
                        if (s.contains(text)) {
                            if (null != currentWaitForFuture) {
                                currentWaitForFuture.complete();
                            }
                            finished = true;
                        }
                    }

                    @Override
                    public boolean isFinished() {
                        return finished;
                    }
                };
                containingList.add(consumer);
            } else if (firstWord.equals("plj-onfail")) {
                String text = afterFirstWord(line, firstWord);
                onFailLine = text;
            } else if (firstWord.equals("plj-checkfail")) {
                String text = afterFirstWord(line, firstWord);
                final List<LineConsumer> containingList = lineConsumers;
                LineConsumer consumer = new LineConsumer() {

                    private volatile boolean finished = false;

                    @Override
                    public void accept(String s) {
                        if (s.contains(text)) {
                            String line = currentOnFailLine;
                            if (null != line) {
                                String lineToParse = line;
                                Utils.runOnDispatchThread(() -> {
                                    List<LineConsumer> origLineConsumers = lineConsumers;
                                    try {
                                        lineConsumers = currentErrorLineConsumers;
                                        File dir = processLaunchDirectory;
                                        if (null != dir) {
                                            addProcess(dir, parseCommandLine(lineToParse));

                                        } else {
                                            addProcess(parseCommandLine(lineToParse));
                                        }
                                    } catch (Exception ex) {
                                        Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, "", ex);
                                        if (ex instanceof RuntimeException) {
                                            throw (RuntimeException) ex;
                                        } else {
                                            throw new RuntimeException(ex);
                                        }
                                    }
                                    lineConsumers = origLineConsumers;
                                });
                            }
                            finished = true;
                        }
                    }

                    @Override
                    public boolean isFinished() {
                        return finished;
                    }
                };
                containingList.add(consumer);
            } else if (firstWord.equals("plj-timeout")) {
                String text = afterFirstWord(line, firstWord);
                setTimeoutMillis(Integer.parseInt(text.trim()));
            } else if (firstWord.equals("plj-waitfor")) {
                String text = afterFirstWord(line, firstWord);
                XFutureVoid xf = new XFutureVoid("plj-waitfor " + text);
                final List<LineConsumer> containingList = lineConsumers;
                LineConsumer consumer = new LineConsumer() {

                    private volatile boolean finished = false;

                    @Override
                    public void accept(String s) {
                        if (s.contains(text)) {
                            xf.complete();
                            finished = true;
                        }
                    }

                    @Override
                    public boolean isFinished() {
                        return finished;
                    }
                };
                containingList.add(consumer);
                futures.add(xf);
                waitForFuture = xf;
            } else if (firstWord.equals("plj-killNeo4J")) {
                Neo4JKiller.killNeo4J();
            } else if (firstWord.equals("plj-cd") || firstWord.equals("cd") || firstWord.equals("chdir")) {
                String text = afterFirstWord(line, firstWord);
                File dir = new File(text);
                if (!dir.exists()) {
                    if (null != stringBuilder) {
                        stringBuilder.append("Directory \"");
                        stringBuilder.append(text);
                        stringBuilder.append("does not exist");
                    }
                    throw new RuntimeException("Directory " + dir + " does not exist.");
                }
                if (!dir.isDirectory()) {
                    if (null != stringBuilder) {
                        stringBuilder.append("\"");
                        stringBuilder.append(text);
                        stringBuilder.append("\" is not a directory");
                    }
                    throw new RuntimeException(dir + " is not a directory");
                }
                processLaunchDirectory = dir;
            } else if (firstWord.equals("plj-stop")) {
                stopLineSeen = true;
                stopLines = new ArrayList<>();
            } else if (firstWord.equals("plj-debug")) {
                setDebug(true);
            } else if (firstWord.startsWith("plj-")) {
                throw new IllegalArgumentException("line starts with plj- but is not recognized : firstWord=" + firstWord + ", line=" + line);
            } else if (!line.startsWith("#") && !firstWord.startsWith("plj-")) {
                errorLineConsumers = new ArrayList<>();
                waitForFuture = null;
                onFailLine = null;
                if (null != processLaunchDirectory) {
                    return addProcess(processLaunchDirectory, parseCommandLine(line));
                } else {
                    return addProcess(parseCommandLine(line));
                }
            }
        } finally {
            if (null != stringBuilder) {
                stringBuilder.append(allIfStack() || (!cmdsProcessed) ? "" : "//");
                stringBuilder.append(tabs);
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            tabs = newTabs;
        }

        return null;
    }
    private volatile int timeoutMillis;

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    private volatile XFutureVoid lastRunAllOfFuture = null;
    
    @SuppressWarnings({"unchecked", "raw_types"})
    public XFutureVoid run(File f, int timeoutMillis, boolean debug) throws IOException {
        List<XFuture<?>> futures = new ArrayList<>();
        this.timeoutMillis = timeoutMillis;
        File parentFile = f.getParentFile();
        this.processLaunchDirectory = parentFile;
        this.stopLineSeen = false;
        this.debug = debug;
        ProcessLauncherJFrame frm = this.processLauncherJFrame;
        if (null != frm) {
            frm.setStopLineSeen(false);
            frm.setProcessLaunchDirectory(parentFile);
        }

//        File jpsCommandFile = Neo4JKiller.getJpsCommandFile();
//        if (null == jpsCommandFile) {
//            jpsCommandFile = new File(processLauncherJFrame.processLaunchDirectory, JPS_COMMAND_FILENAME_STRING);
//        }
        StringBuilder stringBuilder = new StringBuilder();
        ifStack.clear();
        ifStack.push(true);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while (null != (line = br.readLine())) {
                WrappedProcess p = parseLaunchFileLine(line, futures, stringBuilder);
                if (null != p) {
                    futures.add(p.getProcessStartXFuture());
                }
            }
            if (null != frm) {
                frm.getjTextAreaLauncherFile().setText(stringBuilder.toString());
            }
            stringBuilder = null;
        }
        XFutureVoid allOfXFuture = XFuture.allOf(futures);
        this.lastRunAllOfFuture  = allOfXFuture;
        if (this.timeoutMillis > 0) {
            return XFutureVoid.anyOf(allOfXFuture, newTimeoutFuture())
                    .thenRun(() -> {
                        try {
                            System.out.println(f.getCanonicalPath() + " complete.");
                        } catch (IOException ex) {
                            Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    })
                    .always(WrappedProcess::shutdownStarterService);
        } else {
            return allOfXFuture
                    .always(WrappedProcess::shutdownStarterService);
        }
    }
}
