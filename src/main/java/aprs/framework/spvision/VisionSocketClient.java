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

import aprs.framework.database.Main;
import aprs.framework.database.AcquireEnum;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.SocketLineReader;
import crcl.base.PoseType;
import crcl.utils.CRCLPosemath;
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

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionSocketClient implements AutoCloseable {

    private List<DetectedItem> visionList = null;
    private List<DetectedItem> transformedVisionList = null;
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
    private AcquireEnum acquire = AcquireEnum.ON;

    public List<DetectedItem> getVisionList() {
        return Collections.unmodifiableList(visionList);
    }

    public static interface VisionSocketClientListener {

        public void accept(VisionSocketClient client);
    }

    private List<VisionSocketClientListener> listListeners = null;

    public void addListener(VisionSocketClientListener listener) {
        if (null == listListeners) {
            listListeners = new ArrayList<>();
        }
        listListeners.add(listener);
    }

    public void removeListListener(VisionSocketClientListener listener) {
        if (null != listListeners) {
            listListeners.remove(listener);
        }
    }

    private void updateListeners() {
        if (null != listListeners) {
            for (int i = 0; i < listListeners.size(); i++) {
                VisionSocketClientListener listener = listListeners.get(i);
                if (null != listener) {
                    listener.accept(this);
                }
            }
        }
    }

    public static List<DetectedItem> transformList(List<DetectedItem> in, PoseType transform) {
        List<DetectedItem> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            DetectedItem inItem = in.get(i);
            PoseType newPose = CRCLPosemath.multiply(transform, inItem.toCrclPose());
            DetectedItem outItem = new DetectedItem(inItem.name, newPose);
            outItem.repeats = inItem.repeats;
            outItem.index = inItem.index;
            outItem.fullName = inItem.fullName;
            outItem.name = inItem.name;
            outItem.score = inItem.score;
            out.add(outItem);
        }
        return out;
    }

    public AcquireEnum getAcquire() {
        return acquire;
    }

    public void setAcquire(AcquireEnum acquire) {
        this.acquire = acquire;
    }

    public void start(Map<String, String> argsMap) {
        String host = "HOSTNOTSET";
        short port = -99;
        try {
            acquire = AcquireEnum.valueOf(argsMap.get("--aquirestate"));
            host = argsMap.get("--visionhost");
            port = Short.valueOf(argsMap.get("--visionport"));
            visionSlr = SocketLineReader.start(true,
                    host,
                    port,
                    "visionReader", new SocketLineReader.CallBack() {

                private String lastSkippedLine = null;

                @Override
                public void call(final String line, PrintStream os) {
//                    System.out.println("line = " + line+", parsing_line="+parsing_line);
                    if (null == parsing_line) {
                        parsing_line = line;
                        visionExecServ.execute(new Runnable() {

                            @Override
                            public void run() {
                                parseVisionLine(line);
                                if (null != lastSkippedLine) {
                                    String skippedLine = lastSkippedLine;
                                    lastSkippedLine = null;
                                    parseVisionLine(skippedLine);
                                }
                                parsing_line = null;
                            }
                        });
                    } else {
                        lastSkippedLine = line;
                    }
                }
            });
            final VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
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
        String fa[]=null;
        int i=0;
        try {
            if (null == listOut) {
                listOut = new ArrayList<>();
            }
            fa = line.split(",");
            Map<String, Integer> repeatsMap = new HashMap<String, Integer>();
            int index = 0;
            for (i = 0; i < fa.length - 5; i += 6) {
                DetectedItem ci = (listOut.size() > index) ? listOut.get(index) : new DetectedItem();
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
                ci.type = String.valueOf(fa[i+5]);
                
                ci.index = index;
                ci.repeats = (repeatsMap.containsKey(ci.name)) ? repeatsMap.get(ci.name) : 0;
                if (listOut.size() > index) {
                    listOut.set(index, ci);
                } else {
                    listOut.add(ci);
                }
                index++;
                repeatsMap.put(ci.name, ci.repeats + 1);
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

    private boolean addRepeatCountsToDatabaseNames = false;

    /**
     * Get the value of addRepeatCountsToDatabaseNames
     *
     * @return the value of addRepeatCountsToDatabaseNames
     */
    public boolean isAddRepeatCountsToDatabaseNames() {
        return addRepeatCountsToDatabaseNames;
    }

    /**
     * Set the value of addRepeatCountsToDatabaseNames
     *
     * @param addRepeatCountsToDatabaseNames new value of
     * addRepeatCountsToDatabaseNames
     */
    public void setAddRepeatCountsToDatabaseNames(boolean addRepeatCountsToDatabaseNames) {
        this.addRepeatCountsToDatabaseNames = addRepeatCountsToDatabaseNames;
    }

    public void parseVisionLine(final String line) {
        long t0 = System.nanoTime();
        this.line = line;
        final DatabasePoseUpdater dup = Main.getDatabasePoseUpdater();
        final VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();

//        System.out.println("line = " + line);
        if (visionList == null) {
            visionList = new ArrayList<>();
        }
        visionList = lineToList(line, visionList, displayInterface);
        poseUpdatesParsed += visionList.size();
        publishVisionList(dup, displayInterface);
        if (debug) {
            long t1 = System.nanoTime();
            long time_diff = t1 - t0;
            System.out.printf("parseVisionLine time_diff = %.3f\n", (time_diff * 1e-9));
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

    public void publishVisionList(final DatabasePoseUpdater dup, final VisionToDBJFrameInterface displayInterface) {
        if (null != visionList) {
            if (acquire != AcquireEnum.OFF) {
                if (null != dup) {
                    if (null != transform) {
                        transformedVisionList = transformList(visionList, transform);
                        dup.updateVisionList(transformedVisionList, this.addRepeatCountsToDatabaseNames);
                    } else {
                        dup.updateVisionList(visionList, this.addRepeatCountsToDatabaseNames);
                    }
                }
                if (acquire == AcquireEnum.ONCE) {
                    acquire = AcquireEnum.OFF;
                    if (null != replyPs) {
                        replyPs.println("Acquire Status: " + acquire);
                        replyPs = null;
                    }
                    if (null != displayInterface) {
                        displayInterface.setAquiring(acquire.toString());
                    }
                }
            }
            if (null != displayInterface) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        displayInterface.updateInfo(visionList, line);
                    }
                });
                if (null != Main.getDatabasePoseUpdater()) {
                    Main.queryDatabase();
                }
            }
            updateListeners();
        }
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
