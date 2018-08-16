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
import crcl.ui.XFutureVoid;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("WeakerAccess")
public class VisionSocketClient implements AutoCloseable {

    @Nullable
    private volatile List<PhysicalItem> visionList = null;
    
    @Nullable
    private volatile List<PhysicalItem> lastIgnoredVisionList = null;
    
    @Nullable
    private SocketLineReader visionSlr = null;
    @Nullable
    private ExecutorService visionExecServ = Executors.newFixedThreadPool(1);
    @Nullable
    private volatile String parsing_line = null;
    private static final AtomicInteger visioncycle = new AtomicInteger();

    @Nullable
    private PrintStream replyPs;

    @Nullable
    private PoseType transform = null;

    @Nullable
    public PoseType getTransform() {
        return transform;
    }

    public XFutureVoid setTransform(PoseType transform) {
        this.transform = transform;
        return this.updateListeners();
    }

    @Nullable
    public PrintStream getReplyPs() {
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

        public XFutureVoid visionClientUpdateReceived(List<PhysicalItem> list, String line);
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

    public XFutureVoid updateListeners() {
        List<XFutureVoid> futures = new ArrayList<>();
        if (null != visionList) {
            List<PhysicalItem> listToSend = new ArrayList<>(visionList);
            
            synchronized (listListeners) {
                String lineReceived = this.getLine();
                if (null != lineReceived) {
                    for (VisionSocketClientListener listListener : listListeners) {
                        try {
                            VisionSocketClientListener listener = listListener;
                            if (null != listener) {
                                futures.add(listener.visionClientUpdateReceived(listToSend, lineReceived));
                            }
                        } catch (Exception e) {
                            Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", e);
                        }
                    }
                }
            }
        }
        return XFutureVoid.allOf(futures);
    }
    

    @Nullable
    private VisionToDBJFrameInterface displayInterface;

    /**
     * Get the value of displayInterface
     *
     * @return the value of displayInterface
     */
    @Nullable
    public VisionToDBJFrameInterface getDisplayInterface() {
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

    @Nullable
    public String getHost() {
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

    public XFutureVoid start(Map<String, String> argsMap) {
        String host = "HOSTNOTSET";
        short port = -99;
        try {
            String argsMapHost = argsMap.get("--visionhost");
            if (argsMapHost == null) {
                throw new IllegalArgumentException("argsMap does not contain a value for --visionhost");
            }
            host = argsMapHost;
            String argsMapPort = argsMap.get("--visionport");
            if (argsMapPort == null) {
                throw new IllegalArgumentException("argsMap does not contain a value for --visionport");
            }
            port = Short.valueOf(argsMapPort);
            final short portf = port;
            final String hostf = host;
            ExecutorService execSrv = this.visionExecServ;
            if (execSrv == null) {
                throw new IllegalArgumentException("visionExecServ is null, already closed");
            }
            visionSlr = SocketLineReader.startClient(
                    hostf,
                    port,
                    "visionReader_for_" + hostf + ":" + portf,
                    new SocketLineReader.CallBack() {
                @Nullable
                private volatile String lastSkippedLine = null;

                @Override
                public void call(final String line, PrintStream os) {
                    lineCount.incrementAndGet();
//                    System.out.println("line = " + line+", parsing_line="+parsing_line);
                    if (null == parsing_line) {
                        parsing_line = line;
                        if (execSrv == null) {
                            throw new IllegalArgumentException("visionExecServ is null, already closed");
                        }
                        execSrv.execute(new Runnable() {

                            @Override
                            public void run() {
                                String origName = Thread.currentThread().getName();
                                Thread.currentThread().setName("parsingVisionLine from " + hostf + ":" + portf);
                                //System.out.println("visioncycle="+visioncycle);
                                parseVisionLine(line);
                                if (null != lastSkippedLine) {
                                    String skippedLine = lastSkippedLine;
                                    lastSkippedLine = null;
                                    parseVisionLine(skippedLine);
                                }
                                parsing_line = null;
                                Thread.currentThread().setName(origName);
                            }
                        });
                    } else {
                        if (null != lastSkippedLine) {
                            skippedLineCount.incrementAndGet();
                        }
                        lastSkippedLine = line;
                    }
                }
            });
            if (null != displayInterface) {
                displayInterface.setVisionConnected(true);
            }
            return updateListeners();
        } catch (IOException exception) {
            Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, "", exception);
            System.err.println("Connect to vision on host " + host + " with port " + port + " failed with message " + exception);
            Throwable cause = exception.getCause();
            if (null != cause) {
                System.err.println("Caused by " + exception.getCause());
            }
            throw new RuntimeException(exception);
        }
    }

    @Nullable
    private String line;

    @Nullable
    public String getLine() {
        return line;
    }

    public static List<PhysicalItem> lineToList(String line) {
        return lineToList(line, null);
    }

    public static List<PhysicalItem> lineToList(String line, @Nullable VisionToDBJFrameInterface displayInterface) {
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
                double rot = Double.parseDouble(fa[i + 1]);
                if (!Double.isFinite(rot)) {
                    logErr(displayInterface, "Ignoring item with invalid rotation  at position =" + (i + 1) + " of " + (fa[i + 1]) + "in " + line + "\n");
                    continue;
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
                //System.out.println("VisionSocketClient visioncycle-----> "+visioncycle);
                if (fa[i + 4].length() > 0) {
                    ci.setScore(Double.parseDouble(fa[i + 4]));
                }

                //--getting the type
                ci.setType(String.valueOf(fa[i + 5]));
                ci.normalizeRotation();
                ci.setIndex(index);
                if (listOut.size() > index) {
                    listOut.set(index, ci);
                } else {
                    listOut.add(ci);
                }
                index++;
            }
            while (listOut.size() > index) {
                listOut.remove(index);
            }
        } catch (Exception exception) {
            System.err.println("i = " + i+",exception="+exception);
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
    private int parseeVisionLineCount = 0;
    private volatile long prevListSizeSetTime  = -1;

    public void parseVisionLine(final String line) {
        try {
            long t0 = System.nanoTime();
            this.line = line;
            parseeVisionLineCount++;
            if (visionList == null) {
                visionList = new ArrayList<>();
            }
            List<PhysicalItem> newVisionList  = lineToList(line, displayInterface);
            if (null == newVisionList || newVisionList.size() < prevVisionListSize) {
//                if (ignoreCount < 100 || debug) {
//                    System.err.println("ignoring vision list that decreased from " + prevVisionListSize + " to " + visionList.size() + " items");
//                    System.err.println("lineCount=" + lineCount);
//                    System.err.println("ignoreCount=" + ignoreCount);
//                } else if (ignoreCount == 100) {
//                    System.err.println("No more messages about ignored vision lists will be printed");
//                }
                this.lastIgnoredVisionList = newVisionList;
                ignoreCount++;
                consecutiveIgnoreCount++;
                if (consecutiveIgnoreCount > 2 
                        && consecutiveIgnoreCount % 3 == 0
                        && System.currentTimeMillis() - prevListSizeSetTime > 60000) {
                    prevVisionListSize--;
                    prevListSizeSetTime = System.currentTimeMillis();
                }
                return;
            }
            prevListSizeSetTime = System.currentTimeMillis();
            prevVisionListSize = newVisionList.size();
            poseUpdatesParsed += newVisionList.size();
            this.visionList = newVisionList;
            updateListeners();
            consecutiveIgnoreCount = 0;
            if (debug) {
                long t1 = System.nanoTime();
                long time_diff = t1 - t0;
                System.out.println("line = " + line);
                System.out.println("visionList = " + visionList);
                System.out.println("lineCount =" + lineCount);
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
