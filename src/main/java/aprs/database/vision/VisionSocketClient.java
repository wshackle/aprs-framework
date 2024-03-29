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
package aprs.database.vision;

import aprs.database.PhysicalItem;
import aprs.database.SocketLineReader;
import crcl.base.PoseType;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static aprs.misc.AprsCommonLogger.println;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("WeakerAccess")
public class VisionSocketClient implements AutoCloseable {

    private volatile @Nullable
    List<PhysicalItem> visionList = null;

    private volatile @Nullable
    List<PhysicalItem> lastIgnoredVisionList = null;

    private @Nullable
    SocketLineReader visionSlr = null;
    private @Nullable
    ExecutorService visionExecServ = Executors.newSingleThreadExecutor();
    private volatile @Nullable
    String parsing_line = null;
    private static final AtomicInteger visioncycle = new AtomicInteger();

    private @Nullable
    PrintStream replyPs;

    private @Nullable
    PoseType transform = null;

    public @Nullable
    PoseType getTransform() {
        return transform;
    }

    public XFutureVoid setTransform(PoseType transform) {
        this.transform = transform;
        final String lineFinal = line;
        if (null == lineFinal) {
            return XFutureVoid.completedFuture();
        }
        return this.updateListeners(visionList, lineFinal, false);
    }

    public @Nullable
    PrintStream getReplyPs() {
        return replyPs;
    }

    public void setReplyPs(PrintStream replyPs) {
        this.replyPs = replyPs;
    }

    private int poseUpdatesParsed = 0;

    public int getPoseUpdatesParsed() {
        return poseUpdatesParsed;
    }

    public List<PhysicalItem> getVisionList() {
        if (null == visionList) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(visionList);
    }

    public static interface VisionSocketClientListener {

        public XFutureVoid visionClientUpdateReceived(List<PhysicalItem> list, String line, boolean ignored);
    }

    private final List<VisionSocketClientListener> listListeners = new ArrayList<>();

    public void addListener(VisionSocketClientListener listener) {
        synchronized (listListeners) {
            listListeners.add(listener);
        }
    }

    public void removeListListener(VisionSocketClientListener listener) {
        synchronized (listListeners) {
            listListeners.remove(listener);
        }
    }

    private final List<VisionSocketClientListener> ignoredLineListListeners = new ArrayList<>();

    public void addIgnoredLineListener(VisionSocketClientListener listener) {
        synchronized (ignoredLineListListeners) {
            ignoredLineListListeners.add(listener);
        }
    }

    public void removeIgnoredLineListListener(VisionSocketClientListener listener) {
        synchronized (ignoredLineListListeners) {
            ignoredLineListListeners.remove(listener);
        }
    }

    public XFutureVoid updateListeners() {
        final String lineFinal = line;
        if (null == lineFinal) {
            return XFutureVoid.completedFuture();
        }
        return this.updateListeners(visionList, lineFinal, false);
    }

    private boolean combineSuccessiveNonBlankLines = false;

    /**
     * Get the value of combineSuccessiveNonBlankLines
     *
     * @return the value of combineSuccessiveNonBlankLines
     */
    public boolean isCombineSuccessiveNonBlankLines() {
        return combineSuccessiveNonBlankLines;
    }

    /**
     * Set the value of combineSuccessiveNonBlankLines
     *
     * @param combineSuccessiveNonBlankLines new value of
     * combineSuccessiveNonBlankLines
     */
    public void setCombineSuccessiveNonBlankLines(boolean combineSuccessiveNonBlankLines) {
        this.combineSuccessiveNonBlankLines = combineSuccessiveNonBlankLines;
    }

    public XFutureVoid updateListeners(@Nullable List<PhysicalItem> localVisionList, String localLineRecieved, boolean ignored) {
        List<XFutureVoid> futures = new ArrayList<>();
        if (null != localVisionList && null != localLineRecieved && localLineRecieved.length() > 0) {
            List<PhysicalItem> listToSend = new ArrayList<>(localVisionList);

            synchronized (listListeners) {
                int i = 0;
                List<VisionSocketClientListener> tmpListListeners = new ArrayList<>(listListeners);
                try {
                    for (i = 0; i < tmpListListeners.size(); i++) {
                        VisionSocketClientListener listener = tmpListListeners.get(i);
                        if (null != listener) {
                            futures.add(listener.visionClientUpdateReceived(listToSend, localLineRecieved, ignored));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("i=" + i + ", tmpListListeners=" + tmpListListeners);
                    Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", e);
                }
            }
        }
        return XFutureVoid.allOf(futures);
    }

    public XFutureVoid updateIgnoredLineListeners(@Nullable List<PhysicalItem> localVisionList, String localLineRecieved) {
        List<XFutureVoid> futures = new ArrayList<>();
        if (null != localVisionList && null != localLineRecieved && localLineRecieved.length() > 0) {
            List<PhysicalItem> listToSend = new ArrayList<>(localVisionList);

            synchronized (ignoredLineListListeners) {
                List<VisionSocketClientListener> tmpIgnoredLineListListeners = new ArrayList<>(ignoredLineListListeners);
                for (int i = 0; i < tmpIgnoredLineListListeners.size(); i++) {
                    VisionSocketClientListener listListener = tmpIgnoredLineListListeners.get(i);
                    try {
                        VisionSocketClientListener listener = listListener;
                        if (null != listener) {
                            futures.add(listener.visionClientUpdateReceived(listToSend, localLineRecieved, true));
                        }
                    } catch (Exception e) {
                        System.err.println("tmpIgnoredLineListListeners=" + tmpIgnoredLineListListeners);
                        Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", e);
                    }
                }
            }
        }
        return XFutureVoid.allOf(futures);
    }

    private @Nullable
    VisionToDBJFrameInterface displayInterface;

    /**
     * Get the value of displayInterface
     *
     * @return the value of displayInterface
     */
    public @Nullable
    VisionToDBJFrameInterface getDisplayInterface() {
        return displayInterface;
    }

    /**
     * Set the value of displayInterface
     *
     * @param displayInterface new value of displayInterface
     */
    public void setDisplayInterface(VisionToDBJFrameInterface displayInterface) {
        this.displayInterface = displayInterface;
    }

    public int getPort() {
        if (null == visionSlr) {
            return -1;
        }
        return visionSlr.getPort();
    }

    public @Nullable
    String getHost() {
        if (null == visionSlr) {
            return null;
        }
        return visionSlr.getHost();
    }

    private final AtomicInteger skippedLineCount = new AtomicInteger();
    private final AtomicInteger lineCount = new AtomicInteger();

    public int getSkippedLineCount() {
        return skippedLineCount.get();
    }

    public int getLineCount() {
        return lineCount.get();
    }

    public int getIgnoreCount() {
        return ignoreCount;
    }

    public int getConsecutiveIgnoreCount() {
        return consecutiveIgnoreCount;
    }

    public int getMaxConsecutiveIgnoreCount() {
        return maxConsecutiveIgnoreCount;
    }
    private final ConcurrentLinkedDeque<Consumer<Integer>> incrementCountListeners = new ConcurrentLinkedDeque<>();

    public void addCountListener(Consumer<Integer> l) {
        incrementCountListeners.add(l);
    }

    public void removeCountListener(Consumer<Integer> l) {
        incrementCountListeners.remove(l);
    }

    private volatile int emptyLines = 0;

    public void start(Map<String, String> argsMap) {
        String host = "HOSTNOTSET";
        short port = -99;
        try {
            String handleRotationEnumString = argsMap.get("handleRotationEnum");
            this.convertRotToRadians = false;
            if (null != handleRotationEnumString) {
                this.convertRotToRadians = handleRotationEnumString.equals("DEGREES");
                this.zeroRotations = handleRotationEnumString.equals("IGNORE");
            }

            String argsMapHost = argsMap.get("--visionhost");
            if (argsMapHost == null) {
                throw new IllegalArgumentException("argsMap does not contain a value for --visionhost");
            }
            String ignoreLosingItemsListsString = argsMap.get("ignoreLosingItemsLists");
            if (ignoreLosingItemsListsString != null && ignoreLosingItemsListsString.length() > 0) {
                setIgnoreLosingItemsLists(Boolean.parseBoolean(ignoreLosingItemsListsString));
            }

            host = argsMapHost;
            String argsMapPort = argsMap.get("--visionport");
            if (argsMapPort == null) {
                throw new IllegalArgumentException("argsMap does not contain a value for --visionport");
            }
            port = Short.parseShort(argsMapPort);
            int connectTimeout = 0;
            String argsMapConnectTimeout = argsMap.get("connectTimeout");
            if (argsMapConnectTimeout != null) {
                connectTimeout = Integer.parseInt(argsMapConnectTimeout.trim());
            }
            int readSoTimeout = 0;
            String argsMapReadSoTimeout = argsMap.get("readSoTimeout");
            if (argsMapReadSoTimeout != null) {
                readSoTimeout = Integer.parseInt(argsMapReadSoTimeout.trim());
            }
            port = Short.parseShort(argsMapPort);
            final short portf = port;
            final String hostf = host;
            final int connectTimeoutF = connectTimeout;
            final int readSoTimeoutF = readSoTimeout;
            ExecutorService execSrv = this.visionExecServ;
            if (execSrv == null) {
                throw new IllegalArgumentException("visionExecServ is null, already closed");
            }
            StackTraceElement startTrace[] = Thread.currentThread().getStackTrace();
            final SocketLineReader.CallBack callBack = new SocketLineReader.CallBack() {
                private volatile @Nullable
                String lastSkippedLine = null;
                private volatile StringBuffer lineCombinerSB = new StringBuffer();

                @Override
                public void call(final String lineIn, @Nullable PrintStream os) {
                    final boolean lineIsBlank = lineIn.length() < 1 || lineIn.trim().length() < 1;
                    final String line;
                    if (lineIsBlank) {
                        if (!combineSuccessiveNonBlankLines) {
                            //noinspection NonAtomicOperationOnVolatileField
                            emptyLines++;
                            return;
                        } else {
                            line = lineCombinerSB.toString();
                            lineCombinerSB = new StringBuffer();
                        }
                    } else if(combineSuccessiveNonBlankLines) {
                        lineCombinerSB.append(lineIn);
                        return;
                    } else {
                        line = lineIn;
                    }
                    incrementLineCount();
//                    println("line = " + line+", parsing_line="+parsing_line);
                    if (null == parsing_line) {
                        parsing_line = line;
                        if (execSrv == null) {
                            throw new IllegalArgumentException("visionExecServ is null, already closed");
                        }
                        execSrv.execute(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    String origName = Thread.currentThread().getName();
                                    Thread.currentThread().setName("parsingVisionLine from " + hostf + ":" + portf);
                                    //println("visioncycle="+visioncycle);
                                    if (line.startsWith("EXCEPTION")) {
                                        final SocketLineReader thisVisionSlrLocal = VisionSocketClient.this.visionSlr;
                                        final String errmsg;
                                        if (null != thisVisionSlrLocal) {
                                            errmsg = "EXCEPTION: VisionSocket host=" + thisVisionSlrLocal.getHost() + ",port=" + thisVisionSlrLocal.getPort() + line.substring("EXCEPTION".length());
                                        } else {
                                            errmsg = "EXCEPTION: VisionSocket " + line.substring("EXCEPTION".length());
                                        }
                                        updateIgnoredLineListeners(Collections.emptyList(), errmsg);
                                        updateListeners(Collections.emptyList(), errmsg, false);
                                        throw new RuntimeException(line);
                                    }
                                    parseVisionLine(line);
                                    if (null != lastSkippedLine) {
                                        String skippedLine = lastSkippedLine;
                                        lastSkippedLine = null;
                                        parseVisionLine(skippedLine);
                                    }
                                    parsing_line = null;
                                    Thread.currentThread().setName(origName);
                                } catch (Exception exception) {
                                    System.err.println("Failure in vision client for " + hostf + ":" + portf + " with startTrace=" + XFuture.traceToString(startTrace));
                                    Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", exception);
                                    System.err.println("Connect to vision on host " + hostf + " with port " + portf + " failed with message " + exception);
                                    throw new RuntimeException("Failed to run client to vision " + hostf + ":" + portf + " : " + exception.getMessage(), exception);
                                }
                            }
                        });
                    } else {
                        if (null != lastSkippedLine) {
                            skippedLineCount.incrementAndGet();
                        }
                        lastSkippedLine = line;
                    }
                }
            };
            visionSlr = SocketLineReader
                    .startClient(
                            hostf,
                            port,
                            "visionReader_for_" + hostf + ":" + portf,
                            callBack,
                            connectTimeoutF,
                            readSoTimeoutF);
            if (null != displayInterface) {
                displayInterface.setVisionConnected(true);
            }
//            final String lineFinal = line;
//            if (null == lineFinal) {
//                return XFutureVoid.completedFuture();
//            }
//            return this.updateListeners(visionList, lineFinal, false);
        } catch (Exception exception) {
            Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", exception);
            System.err.println("Connect to vision on host " + host + " with port " + port + " failed with message " + exception);
            Throwable cause = exception.getCause();
            if (null != cause) {
                System.err.println("Caused by " + exception.getCause());
            }
            try {
                updateListeners(null, "EXCEPTION: " + exception.getMessage(), false);
                updateIgnoredLineListeners(null, "EXCEPTION: " + exception.getMessage());
            } catch (Exception ex2) {
                // ignoring exceptions when handling exceptions
            }
            throw new RuntimeException("Failed to connect to vision " + host + ":" + port + " : " + exception.getMessage(), exception);
        }
    }

    private void incrementLineCount() {
        int count = lineCount.incrementAndGet();
        for (Consumer<Integer> l : incrementCountListeners) {
            l.accept(count);
        }
    }

    private @Nullable
    String line;

    public @Nullable
    String getLine() {
        return line;
    }

    private boolean convertRotToRadians;

    /**
     * Get the value of convertRotToRadians
     *
     * @return the value of convertRotToRadians
     */
    public boolean isConvertRotToRadians() {
        return convertRotToRadians;
    }

    /**
     * Set the value of convertRotToRadians
     *
     * @param convertRotToRadians new value of convertRotToRadians
     */
    public void setConvertRotToRadians(boolean convertRotToRadians) {
        this.convertRotToRadians = convertRotToRadians;
    }

    public static List<PhysicalItem> lineToList(String line, boolean convertRotToRadians, boolean zeroRotations, boolean traysLocked) {
        return lineToList(line, null, convertRotToRadians, zeroRotations, traysLocked);
    }

    public static String lineToHeading(String prefix, String line) {
        String fa[] = line.split(",[ ]*");
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (int i = 0; i < fa.length - 5; i += 6) {
            sb.append("name").append(i).append(",");
            sb.append("rot").append(i).append(",");
            sb.append("x").append(i).append(",");
            sb.append("y").append(i).append(",");
            sb.append("score").append(i).append(",");
            sb.append("type").append(i).append(",");
        }
        return sb.toString();
    }

    public static List<PhysicalItem> lineToList(String line, @Nullable VisionToDBJFrameInterface displayInterface, boolean convertRotToRadians, boolean zeroRotations, boolean traysLocked) {
        List<PhysicalItem> listOut = new ArrayList<>();
        String fa[] = null;
        int i = 0;
        final int cur_visioncycle = visioncycle.incrementAndGet();
        try {
            fa = line.split(",[ ]*");

            int index = 0;
            long timestamp = System.currentTimeMillis();
            for (i = 0; i < fa.length - 5; i += 6) {
//                PhysicalItem ci = new PhysicalItem(fa[i]);
                String name = fa[i];
                if ("*".equals(name)) {
                    String errMsg = "Ignoring item with name=" + name + " in field " + (i) + " in " + line + "\n";
                    logErr(displayInterface, errMsg);
                    continue;
                }
                name = name.trim();
                boolean missingVal = false;
                for (int j = 0; j < 5; j++) {
                    if (fa[i + j].length() < 1) {
                        logErr(displayInterface, "Ignoring item with empty field  at position =" + (i + j) + " in " + line + "\n");
                        missingVal = true;
                        break;
                    }
                }
                if (missingVal) {
                    continue;
                }
                double rot;
                if (zeroRotations) {
                    rot = 0;
                } else {
                    rot = Double.parseDouble(fa[i + 1]);
                    if (!Double.isFinite(rot)) {
                        logErr(displayInterface, "Ignoring item with invalid rotation  at position =" + (i + 1) + " of " + (fa[i + 1]) + "in " + line + "\n");
                        continue;
                    }
                    if (convertRotToRadians) {
                        rot = Math.toRadians(rot);
                    }
                }

                double x = Double.parseDouble(fa[i + 2]);
                if (!Double.isFinite(x)) {
                    logErr(displayInterface, "Ignoring item with invalid x  at position =" + (i + 2) + " of " + (fa[i + 2]) + "in " + line + "\n");
                    continue;
                }
                double y = Double.parseDouble(fa[i + 3]);
                if (!Double.isFinite(y)) {
                    logErr(displayInterface, "Ignoring item with invalid y  at position =" + (i + 3) + " of " + (fa[i + 3]) + "in " + line + "\n");
                    continue;
                }
                double score = 0.0;
                if (fa[i + 4].length() > 0) {
                    score = Double.parseDouble(fa[i + 4]);
                    if (!Double.isFinite(score)) {
                        if (null != displayInterface && displayInterface.isDebug()) {
                            displayInterface.addLogMessage("Ignoring item with invalid score  at position =" + (i + 4) + " of " + (fa[i + 4]) + "in " + line + "\n");
                        }
                        continue;
                    }
                }
                String type = fa[i + 5].trim();
                PhysicalItem ci = PhysicalItem.newPhysicalItemNameRotXYScoreType(name, rot, x, y, score, type);
                ci.setVisioncycle(cur_visioncycle);
                //println("VisionSocketClient visioncycle-----> "+visioncycle);
                if (fa[i + 4].length() > 0) {
                    ci.setScore(Double.parseDouble(fa[i + 4]));
                }

                //--getting the type
                ci.setType(String.valueOf(fa[i + 5]));
                ci.normalizeRotation();
                ci.setIndex(index);
                if (!traysLocked || (!type.equalsIgnoreCase("PT") && !type.equalsIgnoreCase("KT"))) {
                    if (listOut.size() > index) {
                        listOut.set(index, ci);
                    } else {
                        listOut.add(ci);
                    }
                }
                index++;
            }
            while (listOut.size() > index) {
                listOut.remove(index);
            }
        } catch (Exception exception) {
            System.err.println("i = " + i + ",exception=" + exception);
            System.err.println("fa = " + Arrays.toString(fa));
            String msg = "Failed to parse line \"" + line + "\" : " + exception.getMessage() + System.lineSeparator();
            if (null != displayInterface) {
                displayInterface.addLogMessage(msg);
                displayInterface.addLogMessage(exception);
            } else {
                System.err.println(msg);
                exception.printStackTrace();
            }
        }
        return listOut;
    }

    private static void logErr(@Nullable VisionToDBJFrameInterface displayInterface1, String errMsg) {
        if (null != displayInterface1 && displayInterface1.isDebug()) {
            displayInterface1.addLogMessage(errMsg);
        }
    }

    private int prevVisionListSize = -1;
    private int ignoreCount = 0;
    private int consecutiveIgnoreCount = 0;
    private int maxConsecutiveIgnoreCount = 0;
    private int parseeVisionLineCount = 0;
    private volatile long prevListSizeSetTime = -1;

    private boolean zeroRotations;

    /**
     * Get the value of zeroRotations
     *
     * @return the value of zeroRotations
     */
    public boolean isZeroRotations() {
        return zeroRotations;
    }

    /**
     * Set the value of zeroRotations
     *
     * @param zeroRotations new value of zeroRotations
     */
    public void setZeroRotations(boolean zeroRotations) {
        this.zeroRotations = zeroRotations;
    }

    private boolean updateListenersOnIgnoredLine = false;

    /**
     * Get the value of updateListenersOnIgnoredLine
     *
     * @return the value of updateListenersOnIgnoredLine
     */
    public boolean isUpdateListenersOnIgnoredLine() {
        return updateListenersOnIgnoredLine;
    }

    /**
     * Set the value of updateListenersOnIgnoredLine
     *
     * @param updateListenersOnIgnoredLine new value of
     * updateListenersOnIgnoredLine
     */
    public void setUpdateListenersOnIgnoredLine(boolean updateListenersOnIgnoredLine) {
        this.updateListenersOnIgnoredLine = updateListenersOnIgnoredLine;
    }

    private int prevListSizeDecrementInterval = 2000;

    /**
     * Get the value of prevListSizeDecrementInterval
     *
     * @return the value of prevListSizeDecrementInterval
     */
    public int getPrevListSizeDecrementInterval() {
        return prevListSizeDecrementInterval;
    }

    private final List<PhysicalItem> lockedTrays = new ArrayList<>();

    private boolean lockTrays = false;
    private boolean traysLocked = false;

    public boolean isLockTrays() {
        return lockTrays;
    }

    public boolean isTraysLocked() {
        return traysLocked;
    }

    public void setLockTrays(boolean lockTrays) {
        this.lockTrays = lockTrays;
        traysLocked = false;
    }

    /**
     * Set the value of prevListSizeDecrementInterval
     *
     * @param prevListSizeDecrementInterval new value of
     * prevListSizeDecrementInterval
     */
    public void setPrevListSizeDecrementInterval(int prevListSizeDecrementInterval) {
        this.prevListSizeDecrementInterval = prevListSizeDecrementInterval;
    }

    private boolean ignoreLosingItemsLists = true;

    /**
     * Get the value of ignoreLosingItemsLists
     *
     * @return the value of ignoreLosingItemsLists
     */
    public boolean isIgnoreLosingItemsLists() {
        return ignoreLosingItemsLists;
    }

    /**
     * Set the value of ignoreLosingItemsLists
     *
     * @param ignoreLosingItemsLists new value of ignoreLosingItemsLists
     */
    public void setIgnoreLosingItemsLists(boolean ignoreLosingItemsLists) {
        this.ignoreLosingItemsLists = ignoreLosingItemsLists;
    }

    public void clearPrevVisionListSize() {
        prevVisionListSize = 0;
    }

    public void parseVisionLine(final String line) {
        try {
            long t0 = System.nanoTime();
            this.line = line;
            parseeVisionLineCount++;
            if (visionList == null) {
                visionList = new ArrayList<>();
            }
            List<PhysicalItem> newVisionList = lineToList(line, displayInterface, convertRotToRadians, zeroRotations, traysLocked);
            if (null == newVisionList || (newVisionList.size() < prevVisionListSize && ignoreLosingItemsLists)) {
                this.lastIgnoredVisionList = newVisionList;
                ignoreCount++;
                consecutiveIgnoreCount++;
                if (consecutiveIgnoreCount > 2
                        && System.currentTimeMillis() - prevListSizeSetTime > prevListSizeDecrementInterval) {
                    prevVisionListSize--;
                    prevListSizeSetTime = System.currentTimeMillis();
                }
                if (updateListenersOnIgnoredLine) {
                    updateListeners(newVisionList, line, true);
                }
                updateIgnoredLineListeners(newVisionList, line);
                return;
            }
            prevListSizeSetTime = System.currentTimeMillis();
            prevVisionListSize = newVisionList.size();
            poseUpdatesParsed += newVisionList.size();

            if (lockTrays && !traysLocked) {
                List<PhysicalItem> newLockedTraysList = new ArrayList<>();
                for (int i = 0; i < newVisionList.size(); i++) {
                    PhysicalItem itemI = newVisionList.get(i);
                    if (itemI.getType().equalsIgnoreCase("PT") || itemI.getType().equalsIgnoreCase("KT")) {
                        newLockedTraysList.add(itemI);
                    }
                }
                if (!newLockedTraysList.isEmpty()) {
                    lockedTrays.clear();
                    lockedTrays.addAll(newLockedTraysList);
                    traysLocked = true;
                    prevVisionListSize -= newLockedTraysList.size();
                    if (prevVisionListSize < 0) {
                        prevVisionListSize = 0;
                    }
                }
            } else if (traysLocked) {
                newVisionList.addAll(lockedTrays);
            }
            this.visionList = newVisionList;
            updateListeners(newVisionList, line, false);
            if (consecutiveIgnoreCount > maxConsecutiveIgnoreCount) {
                maxConsecutiveIgnoreCount = consecutiveIgnoreCount;
            }
            consecutiveIgnoreCount = 0;
            if (debug) {
                long t1 = System.nanoTime();
                long time_diff = t1 - t0;
                println("line = " + line);
                println("visionList = " + visionList);
                println("lineCount =" + lineCount);
                System.out.printf("parseVisionLine time_diff = %.3f\n", (time_diff * 1e-9));
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private boolean debug;

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isConnected() {
        return null != visionSlr && visionSlr.isConnected();
    }

    @Override
    public void close() {
        if (null != visionSlr) {
            visionSlr.close();
            visionSlr = null;
        }
        if (null != visionExecServ) {
            visionExecServ.shutdownNow();
            visionExecServ = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public String toString() {
        return "" + visionSlr;
    }

}
