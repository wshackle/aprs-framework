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

package aprs.framework.database;


import java.io.PrintStream;
import java.util.ArrayList;
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

    private  List<DetectedItem> visionList = null;
    private  SocketLineReader visionSlr = null;
    private  ExecutorService visionExecServ = Executors.newFixedThreadPool(1);
    private  volatile boolean parsing_line = false;
    
    private PrintStream replyPs;

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
        if(null == listListeners) {
            listListeners = new ArrayList<>();
        }
        listListeners.add(listener);
    }
    public void removeListListener(VisionSocketClientListener listener) {
        if(null != listListeners) {
            listListeners.remove(listener);
        }
    }
    
    private void updateListeners() {
        if(null != listListeners) {
            for (int i = 0; i < listListeners.size(); i++) {
                VisionSocketClientListener listener = listListeners.get(i);
                if(null != listener) {
                    listener.accept(this);
                }
            }
        }
    }

    public AcquireEnum getAcquire() {
        return acquire;
    }

    public void setAcquire(AcquireEnum acquire) {
        this.acquire = acquire;
    }
    
    
    public void start(Map<String, String> argsMap) {
        try {
            acquire = AcquireEnum.valueOf(argsMap.get("--aquirestate"));
            visionSlr = SocketLineReader.start(true,
                    argsMap.get("--visionhost"),
                    Short.valueOf(argsMap.get("--visionport")),
                    "visionReader", new SocketLineReader.CallBack() {

                @Override
                public void call(final String line, PrintStream os) {
                    if (!parsing_line) {
                        parsing_line = true;
                        visionExecServ.execute(new Runnable() {

                            @Override
                            public void run() {
                                parseVisionLine(line);
                                parsing_line = false;
                            }
                        });
                    }
                }
            });
            final VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
            if (null != displayInterface) {
                displayInterface.setVisionConnected(true);
            }
            updateListeners();
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Connect to vision failed.");
        }
    }
    
    private String line;
    
    public String getLine() {
        return line;
    }
    
    public static List<DetectedItem> lineToList(String line) {
        return lineToList(line,null,null);
    }
    
    public static List<DetectedItem> lineToList(String line, List<DetectedItem> listIn, final VisionToDBJFrameInterface displayInterface) {
        List<DetectedItem> listOut = listIn;
        if(null == listOut) {
            listOut = new ArrayList<>();
        }
        String fa[] = line.split(",");
        Map<String, Integer> repeatsMap = new HashMap<String, Integer>();
        int index = 0;
        for (int i = 0; i < fa.length-4; i += 5) {
            DetectedItem ci = (listOut.size() > index) ? listOut.get(index) : new DetectedItem();

            ci.name = fa[i];
            if(ci.name == null || ci.name.length() < 1 || "*".equals(ci.name)) {
                if(null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Ignoring item with name="+ci.name+" in field "+(i)+" in "+line+"\n");
                }
                continue;
            }
            ci.rotation = Double.valueOf(fa[i + 1]);
            if(Double.isInfinite(ci.rotation) || Double.isNaN(ci.rotation)) {
                if(null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Ignoring item with rotation="+ci.rotation+" in field "+(i+1)+" in "+line+"\n");
                }
                continue;
            }
            ci.x = Double.valueOf(fa[i + 2]);
            if(Double.isInfinite(ci.x) || Double.isNaN(ci.x)) {
                if(null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Ignoring item with x="+ci.x+" in field "+(i+2)+" in "+line+"\n");
                }
                continue;
            }
            ci.y = Double.valueOf(fa[i + 3]);
            if(Double.isInfinite(ci.y) || Double.isNaN(ci.y)) {
                if(null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Ignoring item with y="+ci.y+" in field "+(i+3)+" in "+line+"\n");
                }
                continue;
            }
            ci.score = Double.valueOf(fa[i + 4]);
            if(ci.score < 0.01) {
                if(null != displayInterface && displayInterface.isDebug()) {
                    displayInterface.addLogMessage("Ignoring item with score="+ci.score+" in field "+(i+4)+" in "+line+"\n");
                }
                continue;
            }
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
        return listOut;
    }
    
    public void parseVisionLine(final String line) {
        this.line = line;
        final DatabasePoseUpdater dup = Main.getDatabasePoseUpdater();
        final VisionToDBJFrameInterface displayInterface = Main.getDisplayInterface();
        
//        System.out.println("line = " + line);
        if (visionList == null) {
            visionList = new ArrayList<>();
        }
        visionList = lineToList(line, visionList, displayInterface);
        poseUpdatesParsed += visionList.size();
        if (acquire != AcquireEnum.OFF) {
            if (null != dup) {
                dup.updateVisionList(visionList);
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
            Main.queryDatabase();
        }
        updateListeners();
    }

    @Override
    public void close() throws Exception {
        
        if(null != visionSlr) {
            visionSlr.close();
            visionSlr = null;
        }
        if(null != visionExecServ) {
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
