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

import aprs.framework.database.DetectedItem;
import aprs.framework.database.SocketLineReader;
import crcl.base.PoseType;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionSocketClient implements AutoCloseable {

    private List<DetectedItem> visionList = null;
    private SocketLineReader visionSlr = null;
    private ExecutorService visionExecServ = Executors.newFixedThreadPool(1);
    private volatile String parsing_line = null;
    private static int visioncycle = 0;

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

    public List<DetectedItem> getVisionList() {
        return Collections.unmodifiableList(visionList);
    }

    public static interface VisionSocketClientListener {

        public void visionClientUpdateRecieved(List<DetectedItem> list, String line);
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
            List<DetectedItem> listToSend = new ArrayList<>();
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
        if(null == visionSlr) {
            return -1;
        }
        return visionSlr.getPort();
    }
    
    public String getHost() {
        if(null == visionSlr) {
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
            visionSlr = SocketLineReader.start(true,
                    host,
                    port,
                    "visionReader_for_"+hostf+":"+portf, new SocketLineReader.CallBack() {

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
                                Thread.currentThread().setName("parsingVisionLine from "+hostf+":"+portf);
                                //System.out.println("visioncycle="+visioncycle);
                                parseVisionLine(line);
                                if (null != lastSkippedLine) {
                                    String skippedLine = lastSkippedLine;
                                    lastSkippedLine = null;
                                    parseVisionLine(skippedLine);
                                }
                                parsing_line = null;
                                visioncycle++;
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

    public static List<DetectedItem> lineToList(String line) {
        return lineToList(line, null, null);
    }

    public static List<DetectedItem> lineToList(String line, List<DetectedItem> listIn, final VisionToDBJFrameInterface displayInterface) {
        List<DetectedItem> listOut = listIn;
        String fa[] = null;
        int i = 0;
        try {
            if (null == listOut) {
                listOut = new ArrayList<>();
            }
            fa = line.split(",");
           
            int index = 0;
            long timestamp = System.currentTimeMillis();
            for (i = 0; i < fa.length - 5; i += 6) {
                DetectedItem ci = (listOut.size() > index) ? listOut.get(index) : new DetectedItem(fa[i]);
                ci.timestamp = timestamp;
                if (fa[i].length() < 1) {
                    continue;
                }
                ci.name = fa[i];
                if (ci.name == null || ci.name.length() < 1 || "*".equals(ci.name)) {
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("Ignoring item with name=" + ci.name + " in field " + (i) + " in " + line + "\n");
                    }
                    continue;
                }
                if (fa[i + 1].length() < 1) {
                    continue;
                }
                ci.rotation = Double.valueOf(fa[i + 1]);
                if (Double.isInfinite(ci.rotation) || Double.isNaN(ci.rotation)) {
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("Ignoring item with rotation=" + ci.rotation + " in field " + (i + 1) + " in " + line + "\n");
                    }
                    continue;
                }
//                ci.rotation = Math.toRadians(ci.rotation);
                if (fa[i + 2].length() < 1) {
                    continue;
                }
                ci.x = Double.valueOf(fa[i + 2]);
                if (Double.isInfinite(ci.x) || Double.isNaN(ci.x)) {
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("Ignoring item with x=" + ci.x + " in field " + (i + 2) + " in " + line + "\n");
                    }
                    continue;
                }
                if (fa[i + 3].length() < 1) {
                    continue;
                }
                ci.y = Double.valueOf(fa[i + 3]);
                if (Double.isInfinite(ci.y) || Double.isNaN(ci.y)) {
                    if (null != displayInterface && displayInterface.isDebug()) {
                        displayInterface.addLogMessage("Ignoring item with y=" + ci.y + " in field " + (i + 3) + " in " + line + "\n");
                    }
                    continue;
                }

                ci.visioncycle = visioncycle;
                //System.out.println("VisionSocketClient visioncycle-----> "+visioncycle);

                if (fa[i + 4].length() > 0) {

                    ci.score = Double.valueOf(fa[i + 4]);
                    if (ci.score < 0.01) {
                        if (null != displayInterface && displayInterface.isDebug()) {
                            displayInterface.addLogMessage("Ignoring item with score=" + ci.score + " in field " + (i + 4) + " in " + line + "\n");
                        }
                        continue;
                    }
                }

                //--getting the type
                ci.type = String.valueOf(fa[i + 5]);

                ci.index = index;
//                ci.repeats = (repeatsMap.containsKey(ci.name)) ? repeatsMap.get(ci.name) : 0;
                if (listOut.size() > index) {
                    listOut.set(index, ci);
                } else {
                    listOut.add(ci);
                }
                index++;
//                repeatsMap.put(ci.name, ci.repeats + 1);
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

//    private boolean addRepeatCountsToDatabaseNames = false;
//
//    /**
//     * Get the value of addRepeatCountsToDatabaseNames
//     *
//     * @return the value of addRepeatCountsToDatabaseNames
//     */
//    public boolean isAddRepeatCountsToDatabaseNames() {
//        return addRepeatCountsToDatabaseNames;
//    }
//
//    /**
//     * Set the value of addRepeatCountsToDatabaseNames
//     *
//     * @param addRepeatCountsToDatabaseNames new value of
//     * addRepeatCountsToDatabaseNames
//     */
//    public void setAddRepeatCountsToDatabaseNames(boolean addRepeatCountsToDatabaseNames) {
//        this.addRepeatCountsToDatabaseNames = addRepeatCountsToDatabaseNames;
//    }
//    private DatabasePoseUpdater dpu;
//
//    /**
//     * Get the value of dpu
//     *
//     * @return the value of dpu
//     */
//    public DatabasePoseUpdater getDpu() {
//        return dpu;
//    }
//
//    /**
//     * Set the value of dpu
//     *
//     * @param dpu new value of dpu
//     */
//    public void setDpu(DatabasePoseUpdater dpu) {
//        this.dpu = dpu;
//    }
    public void parseVisionLine(final String line) {
        try {
            long t0 = System.nanoTime();
            this.line = line;

//        System.out.println("line = " + line);
            if (visionList == null) {
                visionList = new ArrayList<>();
            }
            visionList = lineToList(line, visionList, displayInterface);
            poseUpdatesParsed += visionList.size();
//            publishVisionList(dpu, displayInterface);
            updateListeners();
            if (debug) {
                long t1 = System.nanoTime();
                long time_diff = t1 - t0;
                System.out.println("line = " + line);
                System.out.println("visionList = " + visionList);
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
