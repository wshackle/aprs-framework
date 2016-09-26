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

import aprs.framework.spvision.DatabasePoseUpdater;
import aprs.framework.spvision.VisionSocketClient;
import aprs.framework.spvision.VisionToDBJFrameInterface;
import aprs.framework.spvision.VisionToDbMainJFrame;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides some static utility functions and the main class used when launching
 * via java -jar ...
 *
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Main {

    private static Map<String, String> argsMap = null;

    public static Map<String, String> getArgsMap() {
        if (null != argsMap) {
            return argsMap;
        } else {

            initDefaultArgsMap();
            return argsMap;
        }
    }

    private static DatabasePoseUpdater dup = null;
    private static Socket vision_socket = null;
    private static Socket cmd_socket = null;
    private static BufferedReader br = null;
    private static VisionToDBJFrameInterface displayInterface = null;
    private static PrintStream replyPs = null;

    public static VisionSocketClient getVisionSocketClient() {
        return visionSocketClient;
    }
    
    public static int getPoseUpdatesParsed() {
        if(null != visionSocketClient) {
            return visionSocketClient.getPoseUpdatesParsed();
        }
        return 0;
    }
    
    public static DatabasePoseUpdater getDatabasePoseUpdater() {
        return dup;
    }

    public static void closeDatabasePoseUpdater() {
        if (null != dup) {
            dup.close();
        }
        dup = null;
    }

    public static void setDatabasePoseUpdater(DatabasePoseUpdater _dup) {
        closeDatabasePoseUpdater();
        dup = _dup;
    }

    public static void setDisplayInterface(VisionToDBJFrameInterface _displayInterface) {
        displayInterface = _displayInterface;
    }

    public static VisionToDBJFrameInterface getDisplayInterface() {
        return displayInterface;
    }
    private static Thread visionReaderThread = null;
    private static Thread cmdReaderThread = null;
    
    private static SocketLineReader commandSlr = null;
    

    private static ExecutorService pqExecServ = Executors.newFixedThreadPool(1);
    private static volatile boolean updating_pose_query = false;

    private static AcquireEnum defaultAcquireState = AcquireEnum.ON;
    
    public static AcquireEnum getAquire() {
        if(null != visionSocketClient) {
            return visionSocketClient.getAcquire();
        }
        return defaultAcquireState;
    }
    
    public static void setAquire(AcquireEnum aquire) {
        if(null != visionSocketClient) {
            visionSocketClient.setAcquire(aquire);
        }
        defaultAcquireState  = aquire;
    }

    public static void queryDatabase() {
        if (null != dup && getAquire() != AcquireEnum.OFF
                && !updating_pose_query) {
            updating_pose_query = true;
            pqExecServ.execute(new Runnable() {

                @Override
                public void run() {
                    updating_pose_query = true;
                    //System.out.println("----> updating_pose_query is true");
                    final List<PoseQueryElem> l = dup.getDirectPoseList();
                    if (null != l) {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                displayInterface.updataPoseQueryInfo(l);
                            }
                        });
                    }
                    updating_pose_query = false;
                }
            });
        } else {
            if(!updating_pose_query) {
                throw new IllegalStateException("PoseDatabaseUpdater: "+dup+" getAquire()="+getAquire());
            }
        }
    }

    public static void handleCommand(String line, PrintStream os) {
        if (null != displayInterface) {
            displayInterface.setLastCommand(line);
        }
        if (null == dup) {
            os.println("Database not connected.");
            return;
        }
        if (null == visionSocketClient) {
            os.println("Vision not connected.");
            return;
        }
        String fa[] = line.trim().split(" ");
        if (fa.length < 1) {
            os.println("Not recognized: " + line);
            return;
        }
        if (fa[0].trim().toUpperCase().compareTo("ON") == 0) {
            setAquire(AcquireEnum.ON);
            if (null != displayInterface) {
                displayInterface.setAquiring(AcquireEnum.ON.toString());
            }
            os.println("Acquire Status: " + getAquire());
        } else if (fa[0].trim().toUpperCase().compareTo("ONCE") == 0) {
            setAquire(AcquireEnum.ONCE);
            if (null != displayInterface) {
                displayInterface.setAquiring(AcquireEnum.ONCE.toString());
            }
            os.println("Acquire Status: " + AcquireEnum.ONCE);
        } else if (fa[0].trim().toUpperCase().compareTo("OFF") == 0) {
            setAquire(AcquireEnum.OFF);
            if (null != displayInterface) {
                displayInterface.setAquiring(AcquireEnum.OFF.toString());
            }
            os.println("Acquire Status: " + AcquireEnum.OFF);
            replyPs = os;
            if(null != visionSocketClient) {
                visionSocketClient.setReplyPs(replyPs);
            }
        } else {
            os.println("Not recognized: " + line);
            return;
        }
    }

    public static void startCommand(Map<String, String> argsMap) {
        try {
            closeCommand();
            commandSlr = SocketLineReader.start(false,
                    null, // ignored ... argsMap.get("--visionhost"), 
                    Short.valueOf(argsMap.get("--commandport")),
                    "commandReader", new SocketLineReader.CallBack() {

                @Override
                public void call(String line, PrintStream os) {
                    handleCommand(line, os);
                }
            });
            if (null != displayInterface) {
                displayInterface.setCommandConnected(true);
            }
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Starting server for command port failed.");
        }
    }

    public static void connectDB(Map<String, String> argsMap,
                                 Map<DbQueryEnum,DbQueryInfo> queriesMap) {
        try {
            closeDB();
            DbType type =  DbType.valueOf(argsMap.get("--dbtype"));
            dup = new DatabasePoseUpdater(argsMap.get("--dbhost"),
                    Short.valueOf(argsMap.get("--dbport")),
                    argsMap.get("--dbname"),
                    argsMap.get("--dbuser"),
                    argsMap.get("--dbpasswd"),
                    type, 
                    queriesMap,
                    (null != displayInterface && displayInterface.isDebug())
            );
            if (null != displayInterface) {
                displayInterface.setDBConnected(true);
            }
        } catch (Exception exception) {
            if (null != displayInterface) {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                displayInterface.addLogMessage("connectDB failed :" + System.lineSeparator() + sw);
            }
            System.err.println(exception.getLocalizedMessage());
            System.err.println("Connect to database failed.");
        }
    }

    public static void guiShow() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        displayInterface = new VisionToDbMainJFrame();
                        try {
                            displayInterface.updateFromArgs(argsMap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            displayInterface.addLogMessage(e);
                        }
                        displayInterface.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (null != displayInterface) {
                            displayInterface.addLogMessage(e);
                        }
                    }
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void guiClose() {
//        System.out.println("guiClose() called.");
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (null != displayInterface) {
                    displayInterface.setVisible(false);
                    displayInterface.dispose();
                    displayInterface = null;
                }
            }
        });

    }

    public static void closeVision() {
        try {
//            System.out.println("closeVision() called.");
            if (null != displayInterface) {
                displayInterface.setVisionConnected(false);
            }
            if(null != visionSocketClient) {
                visionSocketClient.close();
            }
            visionSocketClient = null;
        } catch (Exception exception) {
            //exception.printStackTrace();
        }
    }

    public static void closeDB() {
//        System.out.println("closeDB() called.");
        if (null != displayInterface) {
            displayInterface.setDBConnected(false);
        }
        if (null != dup) {
            dup.close();
            dup = null;
        }
    }

    public static void closeCommand() {
//        System.out.println("closeCommand() called.");
        if (null != displayInterface) {
            displayInterface.setCommandConnected(false);
        }
        if (null != commandSlr) {
            commandSlr.close();
        }
        commandSlr = null;
    }

    public static void closeAll() {
//        System.out.println("closeAll() called.");
        closeVision();
        closeCommand();
        guiClose();
        closeDB();
    }

    public static Set<String> getArgsSet() {
        Set<String> argsSet = new HashSet<>();
        argsSet.addAll(Arrays.asList(
                "--dbhost",
                "--dbport",
                "--dbname",
                "--dbuser",
                "--dbpasswd",
                "--visionhost",
                "--visionport",
                "--commandport",
                "--aquirestate",
                "--showgui",
                "--dbtype"
        ));
        return argsSet;
    }

    public static void main(String args[]) {
        try {

            initDefaultArgsMap();
            parseArgs(args, getArgsSet());
            System.out.println("argsMap = " + argsMap);
            System.out.println();
            System.out.println("Current Options:");
            Set<String> keySet = argsMap.keySet();
            TreeSet<String> sortedKeySet = new TreeSet<String>();
            sortedKeySet.addAll(keySet);
            for (String k : sortedKeySet) {
                System.out.println("\t" + k + "\t" + argsMap.get(k));
            }
            System.out.println("");

            startCommand(argsMap);
            if (Boolean.valueOf(argsMap.get("--showgui"))) {
                guiShow();
            }
            DbType type =  DbType.valueOf(argsMap.get("--dbtype"));
            connectDB(argsMap,DbSetupBuilder.getDefaultQueriesMap(type));
            startVision(argsMap);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
        }
    }
    private static VisionSocketClient visionSocketClient = null;
    
    public void setVisionSocketClient(VisionSocketClient _newVisionSocketClient) {
        if(null != visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        visionSocketClient = _newVisionSocketClient;
    }

    public static void startVision(Map<String,String> argsMap) {
        Main.closeVision();
        if(null == visionSocketClient) {
            visionSocketClient = new VisionSocketClient();
        }
        if(null != displayInterface) {
            visionSocketClient.setDebug(displayInterface.isDebug());
        }
        visionSocketClient.setReplyPs(replyPs);
        visionSocketClient.start(argsMap);
    }
    
    public static void parseArgs(String[] args, Set<String> argsSet) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("-")) {
                if (!argsSet.contains(args[i])) {
                    System.err.println("Unrecognize argument at position " + i + " of " + args[i] + " in args=" + Arrays.toString(args));
                    System.err.println("Allowed arguments : " + argsSet);
                    System.exit(1);
                }
                argsMap.put(args[i], args[i + 1]);
                i++;
            }
        }
    }

    public static void initDefaultArgsMap() {
        if (null == argsMap) {
            argsMap = new HashMap<String, String>();
        }
        argsMap.put("--dbhost", "localhost");
        argsMap.put("--dbport", "7480");
        argsMap.put("--dbname", "");
        argsMap.put("--dbuser", "neo4j");
        argsMap.put("--dbpasswd", "password");
        argsMap.put("--visionhost", "localhost");
        argsMap.put("--visionport", "4000");
        argsMap.put("--commandport", "4001");
        argsMap.put("--aquirestate", AcquireEnum.ON.toString());
        argsMap.put("--showgui", "true");
        argsMap.put("--dbtype", DbType.NEO4J.toString());
    }
}
