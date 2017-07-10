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
package aprs.framework.spvision;

import aprs.framework.database.PhysicalItem;
import aprs.framework.database.SocketLineReader;
import crcl.base.PoseType;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionSocketClient implements AutoCloseable {

    private List<PhysicalItem> visionList = null;
    private SocketLineReader visionSlr = null;
    private ExecutorService visionExecServ = Executors.newFixedThreadPool(1);
    private volatile String parsing_line = null;
    private static final AtomicInteger visioncycle = new AtomicInteger();

    private PrintStream replyPs;

    private PoseType transform = null;

    public PoseType getTransform() {
        return transform;
    }

    public void setTransform(PoseType transform) {
        this.transform = transform;
        this.updateListeners();
    }

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
        return Collections.unmodifiableList(visionList);
    }

    public static interface VisionSocketClientListener {

        public void visionClientUpdateRecieved(List<PhysicalItem> list, String line);
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

    public void updateListeners() {
        if (null != visionList) {
            List<PhysicalItem> listToSend = new ArrayList<>();
            listToSend.addAll(visionList);
            synchronized (listListeners) {
                for (int i = 0; i < listListeners.size(); i++) {
                    VisionSocketClientListener listener = listListeners.get(i);
                    if (null != listener) {
                        listener.visionClientUpdateRecieved(listToSend, this.getLine());
                    }
                }
            }
        }
    }

    private VisionToDBJFrameInterface displayInterface;

    /**
     * Get the value of displayInterface
     *
     * @return the value of displayInterface
     */
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

    public String getHost() {
        if (null == visionSlr) {
            return null;
        }
        return visionSlr.getHost();
    }

    public void start(Map<String, String> argsMap) {
        String host = "HOSTNOTSET";
        short port = -99;
        try {
            host = argsMap.get("--visionhost");
            port = Short.valueOf(argsMap.get("--visionport"));
            final short portf = port;
            final String hostf = host;
            visionSlr = SocketLineReader.startClient(
                    host,
                    port,
                    "visionReader_for_" + hostf + ":" + portf,
                    new SocketLineReader.CallBack() {
                private String lastSkippedLine = null;

                @Override
                public void call(final String line, PrintStream os) {
//                    System.out.println("line = " + line+", parsing_line="+parsing_line);
                    if (null == parsing_line) {
                        parsing_line = line;
                        visionExecServ.execute(new Runnable() {

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
                        lastSkippedLine = line;
                    }
                }
            });
            if (null != displayInterface) {
                displayInterface.setVisionConnected(true);
            }
            updateListeners();
        } catch (IOException exception) {
            System.err.println("Connect to vision on host " + host + " with port " + port + " failed with message " + exception);
            Throwable cause = exception.getCause();
            if (null != cause) {
                System.err.println("Caused by " + exception.getCause());
            }
        }
    }

    private String line;

    public String getLine() {
        return line;
    }

    public static List<PhysicalItem> lineToList(String line) {
        return lineToList(line, null);
    }

    public static List<PhysicalItem> lineToList(String line, final VisionToDBJFrameInterface displayInterface) {
        List<PhysicalItem> listOut = new ArrayList<>();
        String fa[] = null;
        int i = 0;
        final int cur_visioncycle = visioncycle.incrementAndGet();
        try {
            fa = line.split(",");

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
                    logErr(displayInterface,"Ignoring item with invalid y  at position =" + (i + 3) + " of " + (fa[i + 3]) + "in " + line + "\n");
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
                String type = fa[i + 5];
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
            System.out.println("i = " + i);
            System.out.println("fa = " + Arrays.toString(fa));
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

    private static void logErr(final VisionToDBJFrameInterface displayInterface1, String errMsg) {
        if (null != displayInterface1 && displayInterface1.isDebug()) {
            displayInterface1.addLogMessage(errMsg);
        }
//        else {
//            System.err.println(errMsg);
//        }
    }

    private int prevVisionListSize = -1;
    private int ignoreCount = 0;
    private int lineCount = 0;

    public void parseVisionLine(final String line) {
        try {
            long t0 = System.nanoTime();
            this.line = line;
            lineCount++;
            if (visionList == null) {
                visionList = new ArrayList<>();
            }
            visionList = lineToList(line, displayInterface);
            if (visionList.size() < prevVisionListSize - 2) {
                if (ignoreCount < 100 || debug) {
                    System.err.println("ignoring vision list that decreased from " + prevVisionListSize + " to " + visionList.size() + " items");
                    System.err.println("lineCount=" + lineCount);
                    System.err.println("ignoreCount=" + ignoreCount);
                } else if (ignoreCount == 100) {
                    System.err.println("No more messages about ignored vision lists will be printed");
                }
                ignoreCount++;
                prevVisionListSize--;
                return;
            }
            prevVisionListSize = visionList.size();
            poseUpdatesParsed += visionList.size();
            updateListeners();
            if (debug) {
                long t1 = System.nanoTime();
                long time_diff = t1 - t0;
                System.out.println("line = " + line);
                System.out.println("visionList = " + visionList);
                System.out.println("lineCount =" + lineCount);
                System.out.printf("parseVisionLine time_diff = %.3f\n", (time_diff * 1e-9));
            }
        } catch (Exception ex) {
            Logger.getLogger(VisionSocketClient.class.getName()).log(Level.SEVERE, null, ex);
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

//    public void publishVisionList(final DatabasePoseUpdater dpu, final VisionToDBJFrameInterface displayInterface) throws InterruptedException, ExecutionException {
//        if (null != visionList) {
//            if (acquire != AcquireEnum.OFF) {
//                if (null != dpu) {
//                    if (null != transform) {
//                        transformedVisionList = transformList(visionList, transform);
//                        dpu.updateVisionList(transformedVisionList, this.addRepeatCountsToDatabaseNames);
//                    } else {
//                        dpu.updateVisionList(visionList, this.addRepeatCountsToDatabaseNames);
//                    }
//                }
//                if (acquire == AcquireEnum.ONCE) {
//                    acquire = AcquireEnum.OFF;
//                    if (null != replyPs) {
//                        replyPs.println("Acquire Status: " + acquire);
//                        replyPs = null;
//                    }
//                    if (null != displayInterface) {
//                        displayInterface.setAquiring(acquire.toString());
//                    }
//                }
//            }
//            if (null != displayInterface) {
//                java.awt.EventQueue.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        displayInterface.updateInfo(visionList, line);
//                    }
//                });
//                if (null != dpu) {
//                    dpu.queryDatabase();
//                }
//            }
//            updateListeners();
//        }
//    }
    public boolean isConnected() {
        return null != visionSlr && visionSlr.isConnected();
    }

    @Override
    public void close() throws Exception {

        if (null != visionSlr) {
            visionSlr.close();
            visionSlr = null;
        }
        if (null != visionExecServ) {
            visionExecServ.shutdownNow();
            visionExecServ.awaitTermination(100, TimeUnit.MILLISECONDS);
            visionExecServ = null;
        }

    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
