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
package aprs.framework;

import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupJInternalFrame;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.explore.ExploreGraphDbJInternalFrame;
import aprs.framework.logdisplay.LogDisplayJInternalFrame;
import aprs.framework.pddl.executor.PddlExecutorJInternalFrame;
import aprs.framework.pddl.executor.PositionMap;
import aprs.framework.pddl.planner.PddlPlannerJInternalFrame;
import aprs.framework.simview.Object2DViewJInternalFrame;
import aprs.framework.spvision.VisionToDbJInternalFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import aprs.framework.tomcat.CRCLWebAppRunner;
import com.github.wshackle.fanuccrclservermain.FanucCRCLMain;
import com.github.wshackle.fanuccrclservermain.FanucCRCLServerJInternalFrame;
import com.github.wshackle.crcl4java.motoman.ui.MotomanCrclServerJInternalFrame;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLProgramType;
import crcl.base.CommandStatusType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import crcl.ui.client.PendantClientJInternalFrame;
import crcl.ui.client.PendantClientJPanel;
import crcl.ui.client.UpdateTitleListener;
import crcl.ui.server.SimServerJInternalFrame;
import crcl.utils.CRCLException;
import crcl.utils.CRCLSocket;
import java.awt.Container;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsJFrame extends javax.swing.JFrame implements DisplayInterface, AutoCloseable {

//    private static WeakReference<AprsJFrame> aprsJFrameWeakRef = null;
//
//    public static AprsJFrame getCurrentAprsJFrame() {
//        if (aprsJFrameWeakRef != null) {
//            return aprsJFrameWeakRef.get();
//        } else {
//            return null;
//        }
//    }
    private VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    private PddlExecutorJInternalFrame pddlExecutorJInternalFrame1 = null;
    private Object2DViewJInternalFrame object2DViewJInternalFrame = null;
    private PddlPlannerJInternalFrame pddlPlannerJInternalFrame = null;
    private DbSetupJInternalFrame dbSetupJInternalFrame = null;
    private volatile PendantClientJInternalFrame pendantClientJInternalFrame = null;
    private SimServerJInternalFrame simServerJInternalFrame = null;
    private LogDisplayJInternalFrame logDisplayJInternalFrame = null;
    private FanucCRCLMain fanucCRCLMain = null;
    private FanucCRCLServerJInternalFrame fanucCRCLServerJInternalFrame = null;
    private ExploreGraphDbJInternalFrame exploreGraphDbJInternalFrame = null;
    private MotomanCrclServerJInternalFrame motomanCrclServerJInternalFrame = null;

    private String taskName;

    public Map<String, String> getExecutorOptions() {
        if (null == pddlExecutorJInternalFrame1) {
            return null;
        }
        return pddlExecutorJInternalFrame1.getOptions();
    }

    public void setExecutorOption(String key, String value) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.setOption(key, value);
        }
    }

    public void addPositionMap(PositionMap pm) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.addPositionMap(pm);
        }

    }

    public void removePositionMap(PositionMap pm) {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.removePositionMap(pm);
        }
    }

    public PoseType getCurrentPose() {
        if (null != pendantClientJInternalFrame && pendantClientJInternalFrame.isConnected()) {
            return pendantClientJInternalFrame.getCurrentPose();
        }
        return null;
    }

    public boolean isConnected() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.isConnected();
        }
        return false;
    }

    public void setConnected(boolean connected) {
        if (null != pendantClientJInternalFrame) {
            if (pendantClientJInternalFrame.isConnected() != connected) {
                if (connected) {
                    pendantClientJInternalFrame.connectCurrent();
                } else {
                    pendantClientJInternalFrame.disconnect();
                }
            }
        }
    }

    /**
     * Get the value of taskName
     *
     * @return the value of taskName
     */
    public String getTaskName() {
        return taskName;
    }

    public XFuture<Void> safeAbort() {
        return this.pddlExecutorJInternalFrame1.safeAbort();
    }

    public XFuture<Void> safeAbortAndDisconnectAsync() {
        return this.pddlExecutorJInternalFrame1.safeAbort()
                .thenRunAsync(this::disconnectRobot);
    }

    public void disconnectRobot() {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.disconnect();
        }
        this.setRobotName(null);
    }

    public void connectRobot(String robotName, String host, int port) {
        if (null != pendantClientJInternalFrame) {
            setRobotName(robotName);
            pendantClientJInternalFrame.connect(host, port);
        }
    }

    public String getRobotCrclHost() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getHost();
        }
        return null;
    }

    public int getRobotCrclPort() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.getPort();
        }
        return -1;
    }

    public XFuture<Void> continueActionList() {
        return pddlExecutorJInternalFrame1.continueActionList();
    }

    /**
     * Set the value of taskName
     *
     * @param taskName new value of taskName
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
        updateTitle("", "");
    }

    private String robotName = null;

    /**
     * Get the value of robotName
     *
     * @return the value of robotName
     */
    public String getRobotName() {
        return robotName;
    }

    /**
     * Set the value of robotName
     *
     * @param robotName new value of robotName
     */
    public void setRobotName(String robotName) {
        this.robotName = robotName;
        updateTitle("", "");
    }

    public void addProgramLineListener(PendantClientJPanel.ProgramLineListener l) {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.addProgramLineListener(l);
        }
    }

    public void removeProgramLineListener(PendantClientJPanel.ProgramLineListener l) {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.removeProgramLineListener(l);
        }
    }

    public PoseType correctPose(PoseType poseIn) {
        if (null != pddlExecutorJInternalFrame1) {
            return pddlExecutorJInternalFrame1.correctPose(poseIn);
        }
        return poseIn;
    }

    public PointType reverseCorrectPoint(PointType ptIn) {
        if (null != pddlExecutorJInternalFrame1) {
            return pddlExecutorJInternalFrame1.reverseCorrectPoint(ptIn);
        }
        return ptIn;
    }

    private final List<PendantClientJPanel.CurrentPoseListener> unaddedPoseListeners = new ArrayList<>();

    public void addCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l) {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.addCurrentPoseListener(l);
        } else {
            unaddedPoseListeners.add(l);
        }
    }

    public void removeCurrentPoseListener(PendantClientJPanel.CurrentPoseListener l) {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.removeCurrentPoseListener(l);
        }
    }

    public synchronized void setCRCLProgram(CRCLProgramType program) throws JAXBException {
        if (null != pendantClientJInternalFrame) {
            synchronized (pendantClientJInternalFrame) {
                pendantClientJInternalFrame.setProgram(program);
            }
        }
    }

    public synchronized XFuture<Boolean> startCRCLProgram(CRCLProgramType program) throws JAXBException {
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.setProgram(program);
            return pendantClientJInternalFrame.runCurrentProgram();
        }
        XFuture<Boolean> ret = new XFuture<>();
        ret.complete(false);
        return ret;
    }

    public void immediateAbort() {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.abortProgram();
        } else {
            abortCrclProgram();
        }
    }

    public String getDetailsString() {
        StringBuilder sb = new StringBuilder();
        if (null != pendantClientJInternalFrame) {
            CRCLCommandType cmd = pendantClientJInternalFrame.getCurrentProgramCommand();
            if (null != cmd) {
                try {
                    sb.append("cmd=").append(CRCLSocket.getUtilSocket().commandToSimpleString(cmd)).append("\r\n");
                } catch (ParserConfigurationException | SAXException | IOException ex) {
                    sb.append("cmd= Exception : ").append(ex).append("\r\n");
                    Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            pendantClientJInternalFrame.getCurrentStatus().ifPresent(status -> {
                if (null != status.getCommandStatus()
                        && null != status.getCommandStatus().getStateDescription()
                        && status.getCommandStatus().getStateDescription().length() > 0) {
                    sb.append("state description =").append(status.getCommandStatus().getStateDescription()).append("\r\n");
                }
            });
            pendantClientJInternalFrame.getCurrentState().ifPresent(state -> sb.append("state=").append(state).append("\r\n"));
            sb.append("connected=").append(pendantClientJInternalFrame.isConnected()).append("\r\n");
        }
        if (null != pddlExecutorJInternalFrame1) {
            List<PddlAction> actions = pddlExecutorJInternalFrame1.getActionsList();
            int curActionIndex = pddlExecutorJInternalFrame1.getCurrentActionIndex();
            if (null != actions) {
                sb.append("PDDL curActionIndex=").append(curActionIndex).append("\r\n");
                sb.append("PDDL actions size=").append(actions.size()).append("\r\n");
                if (curActionIndex >= 0 && curActionIndex < actions.size()) {
                    sb.append("PDDL action =").append(actions.get(curActionIndex)).append("\r\n");
                }
            }
        }
        sb.append("robotCrclPort=").append(this.getRobotCrclPort()).append("\r\n");
        return sb.toString();
    }

    public void abortCrclProgram() {
        if (null != pendantClientJInternalFrame && pendantClientJInternalFrame.isConnected()) {
            pendantClientJInternalFrame.abortProgram();
        }
    }

    private PrintStream origOut = null;
    private PrintStream origErr = null;

    static private class MyPrintStream extends PrintStream {

        final private PrintStream ps;
        final private LogDisplayJInternalFrame logDisplayJInternalFrame;

        public MyPrintStream(PrintStream ps, LogDisplayJInternalFrame logDisplayJInternalFrame) {
            super(ps, true);
            this.ps = ps;
            this.logDisplayJInternalFrame = logDisplayJInternalFrame;
            if (null == logDisplayJInternalFrame) {
                throw new IllegalArgumentException("logDisplayJInteralFrame may not be null");
            }
            if (null == ps) {
                throw new IllegalArgumentException("PrintStream ps may not be null");
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (null != logDisplayJInternalFrame) {
                final String s = new String(buf, off, len);
                if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                    logDisplayJInternalFrame.appendText(s);
                } else {

                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            logDisplayJInternalFrame.appendText(s);
                        }
                    });
                }
            }
        }
    }

    private int fanucCrclPort = CRCLSocket.DEFAULT_PORT;
    private int motomanCrclPort = CRCLSocket.DEFAULT_PORT;
    private String fanucNeighborhoodName = "AgilityLabLRMate200iD"; // FIXME hard-coded default
    private boolean fanucPreferRNN = false;
    private String fanucRobotHost = "129.6.78.111"; // FIXME hard-coded default

    private void startFanucCrclServer() {
        try {
            fanucCRCLMain = new FanucCRCLMain();
            if (null == fanucCRCLServerJInternalFrame) {
                fanucCRCLServerJInternalFrame = new FanucCRCLServerJInternalFrame();
                addInternalFrame(fanucCRCLServerJInternalFrame);
            }
            fanucCRCLMain.setDisplayInterface(fanucCRCLServerJInternalFrame);
            fanucCRCLMain.startDisplayInterface();
            fanucCRCLMain.start(fanucPreferRNN, fanucNeighborhoodName, fanucRobotHost, fanucCrclPort);
        } catch (CRCLException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startMotomanCrclServer() {
        try {
            if (null == motomanCrclServerJInternalFrame) {
                motomanCrclServerJInternalFrame = new MotomanCrclServerJInternalFrame();
                updateSubPropertiesFiles();
                motomanCrclServerJInternalFrame.loadProperties();
                motomanCrclServerJInternalFrame.connectCrclMotoplus();
                motomanCrclServerJInternalFrame.setVisible(true);
            }
            addInternalFrame(motomanCrclServerJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addInternalFrame(JInternalFrame internalFrame) {
        internalFrame.pack();
        internalFrame.setVisible(true);
        jDesktopPane1.add(internalFrame);
        internalFrame.getDesktopPane().getDesktopManager().maximizeFrame(internalFrame);
        setupWindowsMenu();
    }

    private ExecutorService connectService = Executors.newSingleThreadExecutor();
    private Future connectDatabaseFuture = null;

    public void startConnectDatabase() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect database when already closing.");
        }
        System.out.println("Starting connect to database ...");
        jCheckBoxMenuItemConnectDatabase.setSelected(true);
        jCheckBoxMenuItemConnectDatabase.setEnabled(true);
        connectDatabaseFuture = connectService.submit(this::connectDatabase);
    }

    public void connectDatabase() {
        List<Future<?>> futures = null;
        try {
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(true).build());
            futures = dbSetupPublisher.notifyAllDbSetupListeners();
            for (Future<?> f : futures) {
                if (!jCheckBoxMenuItemConnectDatabase.isSelected()) {
                    return;
                }
                if (!f.isDone() && !f.isCancelled()) {
                    f.get();
                }
            }
            System.out.println("Finished connect to database.");
        } catch (IOException | InterruptedException | ExecutionException iOException) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, iOException);
            if (null != futures) {
                for (Future<?> f : futures) {
                    f.cancel(true);
                }
            }
        }
    }

    public void connectVision() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect vision when already closing.");
        }
        if (null != visionToDbJInternalFrame) {
            Utils.runOnDispatchThread(() -> visionToDbJInternalFrame.connectVision());
        }
    }
    
    public void disconnectVision() {
        if (closing) {
            throw new IllegalStateException("Attempt to start connect vision when already closing.");
        }
        if (null != visionToDbJInternalFrame) {
            Utils.runOnDispatchThread(() -> visionToDbJInternalFrame.disconnectVision());
        }
    }

    /**
     * Creates new form AprsPddlWrapperJFrame
     */
    public AprsJFrame() {
        try {
            initPropertiesFileInfo();
            initComponents();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        initLoggerWindow();
        try {
//            initPropertiesFile();
            if (propertiesFile.exists()) {
                loadProperties();
            } else {
                saveProperties();
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        commonInit();
    }

    public AprsJFrame(File propertiesFile) {
        try {
            initComponents();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        initLoggerWindow();
        try {
            setPropertiesFile(propertiesFile);
//            initPropertiesFile();
            if (propertiesFile.exists()) {
                loadProperties();
            } else {
                saveProperties();
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        commonInit();
    }

    private void commonInit() {
        startWindowsFromMenuCheckboxes();

        try {
            setIconImage(ImageIO.read(AprsJFrame.class.getResource("aprs.png")));
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != logDisplayJInternalFrame) {
            activateInternalFrame(logDisplayJInternalFrame);
        }
        setupWindowsMenu();
        updateTitle("", "");
//        aprsJFrameWeakRef = new WeakReference<>(this);

    }

    private void startWindowsFromMenuCheckboxes() {
        try {
            if (jCheckBoxMenuItemStartupPDDLPlanner.isSelected()) {
                startPddlPlanner();
            }
            if (jCheckBoxMenuItemStartupPDDLExecutor.isSelected()) {
                startActionsToCrclJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startPendantClientJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
                startSimServerJInternalFrame();
            }
            if (jCheckBoxMenuItemExploreGraphDbStartup.isSelected()) {
                startExploreGraphDb();
            }

            setupWindowsMenu();
            if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
                startFanucCrclServer();
            }
            if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
                startMotomanCrclServer();
            }
            createDbSetupFrame();
            if (jCheckBoxMenuItemShowDatabaseSetup.isSelected()) {
                showDatabaseSetupWindow();
            }
            DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
                pub.addDbSetupListener(toVisListener);
            }
            if (this.jCheckBoxMenuItemStartupCRCLWebApp.isSelected()) {
                startCrclWebApp();
            }
            setupWindowsMenu();
            if (jCheckBoxMenuItemConnectToDatabaseOnStartup.isSelected()) {
                startConnectDatabase();
            }
            if (jCheckBoxMenuItemConnectToVisionOnStartup.isSelected()) {
                connectVision();
            }
            System.out.println("Constructor for AprsJframe complete.");
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initLoggerWindow() {
        try {
            if (null == logDisplayJInternalFrame) {
                logDisplayJInternalFrame = new LogDisplayJInternalFrame();
                logDisplayJInternalFrame.pack();
            }
            logDisplayJInternalFrame.setVisible(true);
            jDesktopPane1.add(logDisplayJInternalFrame);
            System.setOut(new MyPrintStream(System.out, logDisplayJInternalFrame));
            System.setErr(new MyPrintStream(System.err, logDisplayJInternalFrame));
            activateInternalFrame(logDisplayJInternalFrame);

//            Properties buildProperties = null;
//            try (InputStream inputStream = AprsJFrame.class.getResourceAsStream("/build.properties")) {
//                if (null != inputStream) {
//                    buildProperties = new Properties();
//                    buildProperties.load(inputStream);
//                }
//            }
//            if (null != buildProperties) {
//                String revision = buildProperties.getProperty("revision");
//                System.out.println("Build revision = " + revision);
//                String version = buildProperties.getProperty("version");
//                System.out.println("Build version = " + version);
//                String timestamp = buildProperties.getProperty("timestamp");
//                System.out.println("Build timestamp = " + timestamp + "\n Build timestamp as Date = " + new Date(Long.valueOf(timestamp)));
//            }
//            Properties gitProperties = null;
//            try (InputStream inputStream = AprsJFrame.class.getResourceAsStream("git.properties")) {
//                if (null != inputStream) {
//                    gitProperties = new Properties();
//                    gitProperties.load(inputStream);
//                }
//            }
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(AprsJFrame.class.getResourceAsStream("git.properties")))) {
//                String line = null;
//                while(null != (line = br.readLine())) {
//                    System.out.println(line);
//                }
//            }
//            if (null != gitProperties) {
//                String tag = gitProperties.getProperty("git.tag");
//                System.out.println("git.tag = " + tag);
//                String revision = gitProperties.getProperty("git.revision");
//                System.out.println("git.revision = " + revision);
//            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void activateInternalFrame(JInternalFrame internalFrame) {
        try {
            internalFrame.setVisible(true);
            if (null != internalFrame.getDesktopPane()
                    && null != internalFrame.getDesktopPane().getDesktopManager()) {
                internalFrame.getDesktopPane().getDesktopManager().deiconifyFrame(internalFrame);
                internalFrame.getDesktopPane().getDesktopManager().activateFrame(internalFrame);
                internalFrame.getDesktopPane().getDesktopManager().maximizeFrame(internalFrame);
            }
            internalFrame.moveToFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDatabaseSetupWindow() {
        createDbSetupFrame();
        dbSetupJInternalFrame.setVisible(true);
        if (checkInternalFrame(dbSetupJInternalFrame)) {
            jDesktopPane1.getDesktopManager().deiconifyFrame(dbSetupJInternalFrame);
            jDesktopPane1.getDesktopManager().activateFrame(dbSetupJInternalFrame);
        }
        setupWindowsMenu();
    }

    private boolean checkInternalFrame(JInternalFrame frm) {
        try {
            if (frm == null) {
                return false;
            }
            for (JInternalFrame f : jDesktopPane1.getAllFrames()) {
                if (f == frm) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("checkInteralFrame(" + frm + ") failed.");
        return false;
    }

    private void hideDatabaseSetupWindow() {
        if (null != dbSetupJInternalFrame) {
            dbSetupJInternalFrame.setVisible(false);
            if (checkInternalFrame(dbSetupJInternalFrame)) {
                jDesktopPane1.getDesktopManager().iconifyFrame(dbSetupJInternalFrame);
                jDesktopPane1.getDesktopManager().deactivateFrame(dbSetupJInternalFrame);
            }
        }
    }

    private void activateFrame(final JInternalFrame frameToShow) {
        frameToShow.setVisible(true);
        if (checkInternalFrame(frameToShow)) {
            jDesktopPane1.getDesktopManager().deiconifyFrame(frameToShow);
            jDesktopPane1.getDesktopManager().activateFrame(frameToShow);
            frameToShow.moveToFront();
            activeWin = stringToWin(frameToShow.getTitle());
        } else {
            setupWindowsMenu();
        }
    }

    private void setupWindowsMenu() {
        jMenuWindow.removeAll();
        int count = 1;
        for (JInternalFrame f : jDesktopPane1.getAllFrames()) {
            JMenuItem menuItem = new JMenuItem(count + " " + f.getTitle());
            final JInternalFrame frameToShow = f;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activateFrame(frameToShow);
                }
            });
            jMenuWindow.add(menuItem);
            count++;
        }
    }

    private ACTIVE_WINDOW_NAME activeWin = ACTIVE_WINDOW_NAME.OTHER;

    private static enum ACTIVE_WINDOW_NAME {
        CRCL_CLIENT_WINDOW,
        PDDL_EXECUTOR_WINDOW,
        PDDL_PLANNER_WINDOW,
        SIMVIEW_WINDOW,
        DATABASE_SETUP_WINDOW,
        VISION_TO_DB_WINDOW,
        ERRLOG_WINDOW,
        OTHER
    };

    private ACTIVE_WINDOW_NAME stringToWin(String str) {
        if (str.startsWith("CRCL Client")) {
            return ACTIVE_WINDOW_NAME.CRCL_CLIENT_WINDOW;
        }
        if (str.startsWith("Error")) {
            return ACTIVE_WINDOW_NAME.ERRLOG_WINDOW;
        }
        if (str.startsWith("Object2D")) {
            return ACTIVE_WINDOW_NAME.SIMVIEW_WINDOW;
        }
        if (str.startsWith("[Object SP]")) {
            return ACTIVE_WINDOW_NAME.VISION_TO_DB_WINDOW;
        }
        if (str.startsWith("CRCL Simulation Server")) {
            return ACTIVE_WINDOW_NAME.CRCL_CLIENT_WINDOW;
        }
        return ACTIVE_WINDOW_NAME.OTHER;
    }

    private void startObject2DJinternalFrame() {
        try {
            object2DViewJInternalFrame = new Object2DViewJInternalFrame();
            updateSubPropertiesFiles();
            object2DViewJInternalFrame.setAprsJFrame(this);
            object2DViewJInternalFrame.loadProperties();
            object2DViewJInternalFrame.pack();
            object2DViewJInternalFrame.setVisible(true);
            jDesktopPane1.add(object2DViewJInternalFrame);
            object2DViewJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(object2DViewJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    final private List<Runnable> titleUpdateRunnables = Collections.synchronizedList(new ArrayList<>());

    /**
     * Get the value of titleUpdateRunnables
     *
     * @return the value of titleUpdateRunnables
     */
    public List<Runnable> getTitleUpdateRunnables() {
        return titleUpdateRunnables;
    }

    private void updateTitle(String stateString, String stateDescription) {
        String oldTitle = getTitle();
        String newTitle = "APRS : " + ((robotName != null) ? robotName : "NO Robot") + " : " + ((taskName != null) ? taskName : "NO Task") + " : " + stateString + " : " + stateDescription;
        if (newTitle.length() > 70) {
            newTitle = newTitle.substring(0, 70) + " ... ";
        }
        if (!oldTitle.equals(newTitle)) {
            setTitle(newTitle);
            setupWindowsMenu();
        }
        for (Runnable r : titleUpdateRunnables) {
            r.run();
        }
    }

    public void updateTitle() {
        if (null != pendantClientJInternalFrame) {
            CommandStatusType cs = pendantClientJInternalFrame.getCurrentStatus()
                    .map(x -> x.getCommandStatus())
                    .orElse(null);
            if (null == cs || null == cs.getCommandState()) {
                updateTitle("", "");
            } else {
                updateTitle(cs.getCommandState().toString(), cs.getStateDescription());
            }
        } else {
            updateTitle("", "");
        }
    }

    private void startPendantClientJInternalFrame() {
        try {
            pendantClientJInternalFrame = new PendantClientJInternalFrame();
            updateSubPropertiesFiles();
            pendantClientJInternalFrame.loadProperties();
//            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
//            pendantClientJInternalFrame.loadProperties();
            pendantClientJInternalFrame.pack();
            pendantClientJInternalFrame.setVisible(true);
            jDesktopPane1.add(pendantClientJInternalFrame);
            pendantClientJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(pendantClientJInternalFrame);
            pendantClientJInternalFrame.addUpdateTitleListener(new UpdateTitleListener() {
                @Override
                public void titleChanged(CommandStatusType ccst, Container container, String stateString, String stateDescription) {
                    updateTitle(stateString, stateDescription);
                }

            });
            if (null != unaddedPoseListeners) {
                List<PendantClientJPanel.CurrentPoseListener> tempList = new ArrayList<>();
                tempList.addAll(unaddedPoseListeners);
                unaddedPoseListeners.clear();
                for (PendantClientJPanel.CurrentPoseListener l : tempList) {
                    pendantClientJInternalFrame.addCurrentPoseListener(l);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startSimServerJInternalFrame() {
        try {
            if (null == simServerJInternalFrame) {
                simServerJInternalFrame = new SimServerJInternalFrame(false);
//            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
//            pendantClientJInternalFrame.loadProperties();
                updateSubPropertiesFiles();
                simServerJInternalFrame.loadProperties();
                simServerJInternalFrame.pack();
                simServerJInternalFrame.setVisible(true);
                simServerJInternalFrame.restartServer();
                jDesktopPane1.add(simServerJInternalFrame);
                simServerJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(simServerJInternalFrame);

            }
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final Callable<DbSetupPublisher> dbSetupPublisherSupplier = new Callable<DbSetupPublisher>() {
        @Override
        public DbSetupPublisher call() throws Exception {
            createDbSetupFrame();
//            dbSetupJInternalFrame.setVisible(true);
//            jDesktopPane1.getDesktopManager().deiconifyFrame(dbSetupJInternalFrame);
//            jDesktopPane1.getDesktopManager().activateFrame(dbSetupJInternalFrame);
//            dbSetupJInternalFrame.moveToFront();
            return dbSetupJInternalFrame.getDbSetupPublisher();
        }
    };
    
    private void updateDbConnectedCheckBox(DbSetup setup) {
        jCheckBoxMenuItemConnectDatabase.setSelected(setup.isConnected());
    }

    private void createDbSetupFrame() {
        if (null == dbSetupJInternalFrame) {
            dbSetupJInternalFrame = new DbSetupJInternalFrame();
            dbSetupJInternalFrame.pack();
            jDesktopPane1.add(dbSetupJInternalFrame);
            dbSetupJInternalFrame.getDbSetupPublisher().addDbSetupListener(this::updateDbConnectedCheckBox);
        }
    }

    public void setShowVisionConnected(boolean val) {
        jCheckBoxMenuItemConnectVision.setSelected(val);
    }
    
    private void startVisionToDbJinternalFrame() {
        try {
            visionToDbJInternalFrame = new VisionToDbJInternalFrame();
            visionToDbJInternalFrame.setAprsJFrame(this);
            updateSubPropertiesFiles();
            visionToDbJInternalFrame.loadProperties();
            visionToDbJInternalFrame.pack();
            visionToDbJInternalFrame.setVisible(true);
            visionToDbJInternalFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
            jDesktopPane1.add(visionToDbJInternalFrame);
            visionToDbJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(visionToDbJInternalFrame);
            DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.addDbSetupListener(toDbListener);
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startActionsToCrclJInternalFrame() {
        try {
            if (null == pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1 = new PddlExecutorJInternalFrame();
                this.pddlExecutorJInternalFrame1.pack();
            }
            this.pddlExecutorJInternalFrame1.setAprsJFrame(this);
            this.pddlExecutorJInternalFrame1.setVisible(true);
            jDesktopPane1.add(pddlExecutorJInternalFrame1);
            pddlExecutorJInternalFrame1.getDesktopPane().getDesktopManager().maximizeFrame(pddlExecutorJInternalFrame1);
            updateSubPropertiesFiles();
//            this.pddlExecutorJInternalFrame1.setPropertiesFile(new File(propertiesDirectory, "actionsToCrclProperties.txt"));
            this.pddlExecutorJInternalFrame1.loadProperties();
            pddlExecutorJInternalFrame1.setDbSetupSupplier(dbSetupPublisherSupplier);
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

            }

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startPddlPlanner() {
        try {
            //        jDesktopPane1.setDesktopManager(d);
            if (pddlPlannerJInternalFrame == null) {
                pddlPlannerJInternalFrame = new PddlPlannerJInternalFrame();
                pddlPlannerJInternalFrame.pack();
            }
            updateSubPropertiesFiles();
            pddlPlannerJInternalFrame.setVisible(true);
            jDesktopPane1.add(pddlPlannerJInternalFrame);
            pddlPlannerJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(pddlPlannerJInternalFrame);
//            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "pddlPlanner.txt"));
            pddlPlannerJInternalFrame.loadProperties();
            pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDesktopPane1 = new javax.swing.JDesktopPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemLoadProperties = new javax.swing.JMenuItem();
        jMenuItemSaveProperties = new javax.swing.JMenuItem();
        jMenuItemSavePropsAs = new javax.swing.JMenuItem();
        jMenuItemLoadPropertiesFile = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItemStartupPDDLPlanner = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupPDDLExecutor = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObjectSP = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObject2DView = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupRobotCrclGUI = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupRobtCRCLSimServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemShowDatabaseSetup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupFanucCRCLServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupMotomanCRCLServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectToDatabaseOnStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectToVisionOnStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemExploreGraphDbStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupCRCLWebApp = new javax.swing.JCheckBoxMenuItem();
        jMenuWindow = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jCheckBoxMenuItemConnectDatabase = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectVision = new javax.swing.JCheckBoxMenuItem();
        jMenuExecute = new javax.swing.JMenu();
        jMenuItemStartActionList = new javax.swing.JMenuItem();
        jMenuItemContinue = new javax.swing.JMenuItem();
        jMenuItemImmediateAbort = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS");

        jMenu1.setText("File");

        jMenuItemLoadProperties.setText("Reload Property Settings");
        jMenuItemLoadProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadPropertiesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadProperties);

        jMenuItemSaveProperties.setText("Save Properties");
        jMenuItemSaveProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePropertiesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveProperties);

        jMenuItemSavePropsAs.setText("Save Properties As ...");
        jMenuItemSavePropsAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePropsAsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSavePropsAs);

        jMenuItemLoadPropertiesFile.setText("Load Properties File ...");
        jMenuItemLoadPropertiesFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadPropertiesFileActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadPropertiesFile);
        jMenu1.add(jSeparator1);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemExit);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Startup");

        jCheckBoxMenuItemStartupPDDLPlanner.setSelected(true);
        jCheckBoxMenuItemStartupPDDLPlanner.setText("PDDL Planner");
        jCheckBoxMenuItemStartupPDDLPlanner.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupPDDLPlannerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupPDDLPlanner);

        jCheckBoxMenuItemStartupPDDLExecutor.setSelected(true);
        jCheckBoxMenuItemStartupPDDLExecutor.setText("PDDL Executor");
        jCheckBoxMenuItemStartupPDDLExecutor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupPDDLExecutorActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupPDDLExecutor);

        jCheckBoxMenuItemStartupObjectSP.setSelected(true);
        jCheckBoxMenuItemStartupObjectSP.setText("Object SP");
        jCheckBoxMenuItemStartupObjectSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObjectSPActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObjectSP);

        jCheckBoxMenuItemStartupObject2DView.setSelected(true);
        jCheckBoxMenuItemStartupObject2DView.setText("Object 2D View/Simulate");
        jCheckBoxMenuItemStartupObject2DView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObject2DViewActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObject2DView);

        jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(true);
        jCheckBoxMenuItemStartupRobotCrclGUI.setText("Robot CRCL Client Gui");
        jCheckBoxMenuItemStartupRobotCrclGUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobotCrclGUI);

        jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(true);
        jCheckBoxMenuItemStartupRobtCRCLSimServer.setText("Robot CRCL SimServer");
        jCheckBoxMenuItemStartupRobtCRCLSimServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobtCRCLSimServer);

        jCheckBoxMenuItemShowDatabaseSetup.setSelected(true);
        jCheckBoxMenuItemShowDatabaseSetup.setText("Database Setup");
        jCheckBoxMenuItemShowDatabaseSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowDatabaseSetupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemShowDatabaseSetup);

        jCheckBoxMenuItemStartupFanucCRCLServer.setText("Fanuc CRCL Server");
        jCheckBoxMenuItemStartupFanucCRCLServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupFanucCRCLServer);

        jCheckBoxMenuItemStartupMotomanCRCLServer.setText("Motoman CRCL Server");
        jCheckBoxMenuItemStartupMotomanCRCLServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupMotomanCRCLServer);

        jCheckBoxMenuItemConnectToDatabaseOnStartup.setText("Connect To Database On Startup");
        jCheckBoxMenuItemConnectToDatabaseOnStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemConnectToDatabaseOnStartup);

        jCheckBoxMenuItemConnectToVisionOnStartup.setText("Connect To Vision On Startup");
        jCheckBoxMenuItemConnectToVisionOnStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectToVisionOnStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemConnectToVisionOnStartup);

        jCheckBoxMenuItemExploreGraphDbStartup.setText("Explore Graph Database");
        jCheckBoxMenuItemExploreGraphDbStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemExploreGraphDbStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemExploreGraphDbStartup);

        jCheckBoxMenuItemStartupCRCLWebApp.setText("CRCL Web App");
        jCheckBoxMenuItemStartupCRCLWebApp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupCRCLWebAppActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupCRCLWebApp);

        jMenuBar1.add(jMenu3);

        jMenuWindow.setText("Window");
        jMenuBar1.add(jMenuWindow);

        jMenu2.setText("Connections");

        jCheckBoxMenuItemConnectDatabase.setText("Database");
        jCheckBoxMenuItemConnectDatabase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectDatabaseActionPerformed(evt);
            }
        });
        jMenu2.add(jCheckBoxMenuItemConnectDatabase);

        jCheckBoxMenuItemConnectVision.setText("Vision");
        jCheckBoxMenuItemConnectVision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectVisionActionPerformed(evt);
            }
        });
        jMenu2.add(jCheckBoxMenuItemConnectVision);

        jMenuBar1.add(jMenu2);

        jMenuExecute.setText("Execute");

        jMenuItemStartActionList.setText("Start Action List");
        jMenuItemStartActionList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartActionListActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemStartActionList);

        jMenuItemContinue.setText("Continue Actions ");
        jMenuItemContinue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemContinueActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemContinue);

        jMenuItemImmediateAbort.setText("Immediate Abort");
        jMenuItemImmediateAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemImmediateAbortActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemImmediateAbort);

        jMenuBar1.add(jMenuExecute);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1058, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxMenuItemStartupPDDLPlannerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed
        try {
            if (jCheckBoxMenuItemStartupPDDLPlanner.isSelected()) {
                startPddlPlanner();
            } else {
                closePddlPlanner();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed

    private void jCheckBoxMenuItemStartupPDDLExecutorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupPDDLExecutorActionPerformed
        try {
            if (jCheckBoxMenuItemStartupPDDLExecutor.isSelected()) {
                startActionsToCrclJInternalFrame();
            } else {
                closeActionsToCrclJInternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLExecutorActionPerformed

    private void jCheckBoxMenuItemStartupObjectSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed

    private void jCheckBoxMenuItemStartupObject2DViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed

    private void jMenuItemLoadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesActionPerformed
        try {
            this.loadProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemLoadPropertiesActionPerformed

    private void jMenuItemSavePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropertiesActionPerformed
        try {
            this.saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSavePropertiesActionPerformed

    private void jMenuItemLoadPropertiesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesFileActionPerformed
        JFileChooser chooser = new JFileChooser(propertiesDirectory);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setPropertiesFile(chooser.getSelectedFile());
                loadProperties();
//                initPropertiesFile();

            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemLoadPropertiesFileActionPerformed

//    private void initPropertiesFile() throws IOException {
//        if (propertiesFile.exists()) {
//            loadProperties();
//        } else {
//            if (!propertiesDirectory.exists()) {
//                System.out.println("The directory " + propertiesDirectory + " does not exist, it will be created now.");
//            }
//            System.out.println("Properties file " + propertiesFile + " does not exist.");
//            System.out.println("It will be created with the current properties.");
//            saveProperties();
//        }
//    }

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        try {
            close();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed
        try {
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startPendantClientJInternalFrame();
            } else {
//                closePddlPlanner();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed

    private void jCheckBoxMenuItemShowDatabaseSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowDatabaseSetupActionPerformed
        try {
            if (jCheckBoxMenuItemShowDatabaseSetup.isSelected()) {
                showDatabaseSetupWindow();
            } else {
                hideDatabaseSetupWindow();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemShowDatabaseSetupActionPerformed

    private void jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed
        if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
            startSimServerJInternalFrame();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed

    private void jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
            startFanucCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed

    private void jCheckBoxMenuItemExploreGraphDbStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemExploreGraphDbStartupActionPerformed
        if (this.jCheckBoxMenuItemExploreGraphDbStartup.isSelected()) {
            startExploreGraphDb();
        } else {
            closeExploreGraphDb();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemExploreGraphDbStartupActionPerformed

    CRCLWebAppRunner crclWebAppRunner = null;

    public void stopCrclWebApp() {
        if (null != crclWebAppRunner) {
            crclWebAppRunner.stop();
            crclWebAppRunner = null;
        }
    }

    public void startCrclWebApp() {
        crclWebAppRunner = new CRCLWebAppRunner();
        crclWebAppRunner.setHttpPort(crclWebServerHttpPort);
        crclWebAppRunner.start();
    }

    private int crclWebServerHttpPort = 8081;

    /**
     * Get the value of crclWebServerHttpPort
     *
     * @return the value of crclWebServerHttpPort
     */
    public int getCrclWebServerHttpPort() {
        return crclWebServerHttpPort;
    }

    /**
     * Set the value of crclWebServerHttpPort
     *
     * @param crclWebServerHttpPort new value of crclWebServerHttpPort
     */
    public void setCrclWebServerHttpPort(int crclWebServerHttpPort) {
        this.crclWebServerHttpPort = crclWebServerHttpPort;
    }

    private void jCheckBoxMenuItemStartupCRCLWebAppActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupCRCLWebAppActionPerformed
        try {
            stopCrclWebApp();
            if (jCheckBoxMenuItemStartupCRCLWebApp.isSelected()) {
                String portString = JOptionPane.showInputDialog("Http Port?", crclWebServerHttpPort);
                crclWebServerHttpPort = Integer.valueOf(portString);
                startCrclWebApp();
            }
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupCRCLWebAppActionPerformed

    private void jCheckBoxMenuItemConnectDatabaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectDatabaseActionPerformed
        if (this.jCheckBoxMenuItemConnectDatabase.isSelected()) {
            startConnectDatabase();
        } else {
            startDisconnectDatabase();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectDatabaseActionPerformed

    private void jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed
        try {
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectToDatabaseOnStartupActionPerformed

    private void jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
            startMotomanCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed

    private void jMenuItemSavePropsAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropsAsActionPerformed
        JFileChooser chooser = new JFileChooser(propertiesDirectory);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setPropertiesFile(chooser.getSelectedFile());
//                initPropertiesFile();
                this.saveProperties();

            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSavePropsAsActionPerformed

    private void jMenuItemContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueActionPerformed
        this.continueActionList();
    }//GEN-LAST:event_jMenuItemContinueActionPerformed

    private void jMenuItemImmediateAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortActionPerformed
        this.immediateAbort();
    }//GEN-LAST:event_jMenuItemImmediateAbortActionPerformed

    private void jMenuItemStartActionListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartActionListActionPerformed
        this.startActions();
    }//GEN-LAST:event_jMenuItemStartActionListActionPerformed

    private void jCheckBoxMenuItemConnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectVisionActionPerformed
       if(jCheckBoxMenuItemConnectVision.isSelected()) {
           connectVision();
       } else {
           disconnectVision();
       }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectVisionActionPerformed

    private void jCheckBoxMenuItemConnectToVisionOnStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectToVisionOnStartupActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxMenuItemConnectToVisionOnStartupActionPerformed

    public XFuture<Boolean> startActions() {
        if(null != object2DViewJInternalFrame) {
            object2DViewJInternalFrame.refresh();
        }
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.refresh();
            return pddlExecutorJInternalFrame1.startActions();
        } else {
            return XFuture.completedFuture(false);
        }
    }

    public void startExploreGraphDb() {
        try {
            if (null == this.exploreGraphDbJInternalFrame) {
                this.exploreGraphDbJInternalFrame = new ExploreGraphDbJInternalFrame();
                DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
                dbSetupPublisher.addDbSetupListener(exploreGraphDbJInternalFrame);
                exploreGraphDbJInternalFrame.accept(dbSetupPublisher.getDbSetup());
                this.addInternalFrame(exploreGraphDbJInternalFrame);
            }
            activateInternalFrame(this.exploreGraphDbJInternalFrame);

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void closeExploreGraphDb() {
        try {
            if (null != this.exploreGraphDbJInternalFrame) {
                // FIXME decide what to do later
            }
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addAction(PddlAction action) {
        this.pddlExecutorJInternalFrame1.addAction(action);
    }

    public void processActions() {
        this.pddlExecutorJInternalFrame1.processActions();
    }

    private File propertiesFile;
    private File propertiesDirectory;
    public static final String APRS_DEFAULT_PROPERTIES_FILENAME = "aprs_properties.txt";
    private File lastAprsPropertiesFileFile;

    private void initPropertiesFileInfo() {
        String aprsPropsDir = System.getProperty("aprs.properties.dir");
        if (null != aprsPropsDir) {
            propertiesDirectory = new File(aprsPropsDir);
        } else {
            propertiesDirectory = new File(System.getProperty("user.home"), ".aprs");
        }
        lastAprsPropertiesFileFile = new File(propertiesDirectory, "lastPropsFileName.txt");
        if (lastAprsPropertiesFileFile.exists()) {
            try {
                Properties p = new Properties();
                p.load(new FileReader(lastAprsPropertiesFileFile));
                String fname = p.getProperty("lastPropertyFileName");
                if (fname != null && fname.length() > 0) {
                    File f = new File(fname);
                    if (f.exists()) {
                        propertiesFile = f;
                        propertiesDirectory = propertiesFile.getParentFile();
                        return;

                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        propertiesDirectory.mkdirs();
        String aprsPropsFilename = System.getProperty("aprs.properties.filename", APRS_DEFAULT_PROPERTIES_FILENAME);
        propertiesFile = new File(propertiesDirectory, aprsPropsFilename);
    }

    private final DbSetupListener toVisListener = new DbSetupListener() {
        @Override
        public void accept(DbSetup setup) {
//            if (setup.isConnected() == (dbSetup != null && !dbSetup.isConnected())) {
//                dbSetup = setup;
//                DbSetup oldDbSetup = DbSetupBuilder.loadFromPropertiesFile(dbPropertiesFile).build();
//                if (!Objects.equals(oldDbSetup.getDbType(), setup.getDbType())) {
//                    setup = DbSetupBuilder.loadFromPropertiesFile(propertiesFile, setup.getDbType(), null, -1).build();
//                    if (null != dbSetupJInternalFrame) {
//                        DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
//                        if (null != pub) {
//                            DbSetupBuilder.savePropertiesFile(dbPropertiesFile, setup);
//                            pub.setDbSetup(setup);
//                        }
//                    }
//                }
//            }
            Utils.runOnDispatchThread(() -> {
                dbSetup = setup;
                if (null != visionToDbJInternalFrame) {
                    DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
                    if (null != pub) {
                        DbSetupBuilder.savePropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt"), dbSetup);
                        pub.setDbSetup(setup);
                    }
                }
            });
        }
    };

    private final DbSetupListener toDbListener = new DbSetupListener() {
        @Override
        public void accept(DbSetup setup) {
//            if (setup.isConnected() == (dbSetup != null && !dbSetup.isConnected())) {
//                dbSetup = setup;
//
//                DbSetup oldDbSetup = DbSetupBuilder.loadFromPropertiesFile(dbPropertiesFile).build();
//                if (!Objects.equals(oldDbSetup.getDbType(), setup.getDbType())) {
//                    setup = DbSetupBuilder.loadFromPropertiesFile(propertiesFile, setup.getDbType(), null, -1).build();
//                    DbSetupBuilder.savePropertiesFile(dbPropertiesFile, setup);
//                    if (null != visionToDbJInternalFrame) {
//                        DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
//                        if (null != pub) {
//                            pub.setDbSetup(setup);
//                        }
//                    }
//                }
//            }
            Utils.runOnDispatchThread(() -> {
                dbSetup = setup;
                if (null != dbSetupJInternalFrame) {
                    DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
                    if (null != pub) {
                        DbSetupBuilder.savePropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt"), dbSetup);
                        pub.setDbSetup(setup);
                    }
                }
            });
        }
    };

    DbSetup dbSetup = null;

    @Override
    public final void loadProperties() throws IOException {
        Properties props = new Properties();
        System.out.println("AprsJFrame loading properties from " + propertiesFile.getCanonicalPath());
        try (FileReader fr = new FileReader(propertiesFile)) {
            props.load(fr);
        }
        String startPddlPlannerString = props.getProperty(STARTUPPDDLPLANNER);
        if (null != startPddlPlannerString) {
            jCheckBoxMenuItemStartupPDDLPlanner.setSelected(Boolean.valueOf(startPddlPlannerString));
        }
        String startPddlExecutorString = props.getProperty(STARTUPPDDLEXECUTOR);
        if (null != startPddlExecutorString) {
            jCheckBoxMenuItemStartupPDDLExecutor.setSelected(Boolean.valueOf(startPddlExecutorString));
        }
        String startObjectSpString = props.getProperty(STARTUPPDDLOBJECTSP);
        if (null != startObjectSpString) {
            jCheckBoxMenuItemStartupObjectSP.setSelected(Boolean.valueOf(startObjectSpString));
        }

        String startObjectViewString = props.getProperty(STARTUPPDDLOBJECTVIEW);
        if (null != startObjectViewString) {
            jCheckBoxMenuItemStartupObject2DView.setSelected(Boolean.valueOf(startObjectViewString));
        }
        String startCRCLClientString = props.getProperty(STARTUPROBOTCRCLCLIENT);
        if (null != startCRCLClientString) {
            jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(Boolean.valueOf(startCRCLClientString));
        }
        String startCRCLSimServerString = props.getProperty(STARTUPROBOTCRCLSIMSERVER);
        if (null != startCRCLSimServerString) {
            jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(Boolean.valueOf(startCRCLSimServerString));
        }
        String startCRCLFanucServerString = props.getProperty(STARTUPROBOTCRCLFANUCSERVER);
        if (null != startCRCLFanucServerString) {
            jCheckBoxMenuItemStartupFanucCRCLServer.setSelected(Boolean.valueOf(startCRCLFanucServerString));
        }
        String fanucCrclLocalPortString = props.getProperty(FANUC_CRCL_LOCAL_PORT);
        if (null != fanucCrclLocalPortString) {
            this.fanucCrclPort = Integer.valueOf(fanucCrclLocalPortString);
        }
        String fanucRobotHostString = props.getProperty(FANUC_ROBOT_HOST);
        if (null != fanucRobotHostString) {
            this.fanucRobotHost = fanucRobotHostString;
        }

        String startCRCLMotomanServerString = props.getProperty(STARTUPROBOTCRCLMOTOMANSERVER);
        if (null != startCRCLMotomanServerString) {
            jCheckBoxMenuItemStartupMotomanCRCLServer.setSelected(Boolean.valueOf(startCRCLMotomanServerString));
        }
        String startConnectDBString = props.getProperty(STARTUPCONNECTDATABASE);
        if (null != startConnectDBString) {
            jCheckBoxMenuItemConnectToDatabaseOnStartup.setSelected(Boolean.valueOf(startConnectDBString));
            if (jCheckBoxMenuItemConnectToDatabaseOnStartup.isSelected()) {
                jCheckBoxMenuItemShowDatabaseSetup.setSelected(true);
            }
        }
        String startConnectVisionString = props.getProperty(STARTUPCONNECTVISION);
        if (null != startConnectVisionString) {
            jCheckBoxMenuItemConnectToVisionOnStartup.setSelected(Boolean.valueOf(startConnectVisionString));
            if (jCheckBoxMenuItemConnectToVisionOnStartup.isSelected()) {
                jCheckBoxMenuItemStartupObjectSP.setSelected(true);
            }
        }
        String startExploreGraphDbString = props.getProperty(STARTUPEXPLOREGRAPHDB);
        if (null != startExploreGraphDbString) {
            jCheckBoxMenuItemExploreGraphDbStartup.setSelected(Boolean.valueOf(startExploreGraphDbString));
        }
        String crclWebAppPortString = props.getProperty(CRCLWEBAPPPORT);
        if (null != crclWebAppPortString) {
            crclWebServerHttpPort = Integer.valueOf(crclWebAppPortString);
        }
        String startCrclWebAppString = props.getProperty(STARTUPCRCLWEBAPP);
        if (null != startCrclWebAppString) {
            jCheckBoxMenuItemStartupCRCLWebApp.setSelected(Boolean.valueOf(startCrclWebAppString));
        }
//        String executable = props.getProperty(PROGRAMEXECUTABLE);
//        if (null != executable) {
//            jTextFieldPlannerProgramExecutable.setText(executable);
//        }
//        String domain = props.getProperty(PDDLDOMAIN);
//        if (null != domain) {
//            jTextFieldPddlDomainFile.setText(domain);
//        }
//        String problem = props.getProperty(PDDLPROBLEM);
//        if (null != problem) {
//            jTextFieldPddlProblem.setText(problem);
//        }
//        String output = props.getProperty(PDDLOUTPUT);
//        if (null != output) {
//            jTextFieldPddlOutputActions.setText(output);
//        }
//        String addargs = props.getProperty(PDDLADDARGS);
//        if (null != addargs) {
//            jTextFieldAdditionalArgs.setText(addargs);
//        }
        this.updateSubPropertiesFiles();
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.loadProperties();
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.loadProperties();
        }

        if (null != this.object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.loadProperties();
        }
        dbSetup = DbSetupBuilder.loadFromPropertiesFile(new File(propertiesDirectory, propertiesFileBaseString + "_dbsetup.txt")).build();

        if (null != dbSetupJInternalFrame) {
            DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
            }
        }
        if (null != visionToDbJInternalFrame) {
            this.visionToDbJInternalFrame.loadProperties();
            DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
            }
        }
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.loadProperties();
        }
        if (null != this.pendantClientJInternalFrame) {
            pendantClientJInternalFrame.loadProperties();
        }
        if (null != this.simServerJInternalFrame) {
            simServerJInternalFrame.loadProperties();
        }
        if (null != this.motomanCrclServerJInternalFrame) {
            motomanCrclServerJInternalFrame.loadProperties();
        }
        String motomanCrclLocalPortString = props.getProperty(MOTOMAN_CRCL_LOCAL_PORT);
        if (null != motomanCrclLocalPortString) {
            this.motomanCrclPort = Integer.valueOf(motomanCrclLocalPortString);
            if (null != motomanCrclServerJInternalFrame) {
                motomanCrclServerJInternalFrame.setCrclPort(motomanCrclPort);
            }
        }
        String robotNameString = props.getProperty(APRSROBOT_PROPERTY_NAME);
        if (null != robotNameString) {
            setRobotName(robotNameString);
        } else {
            setDefaultRobotName();
        }
        String taskNameString = props.getProperty(APRSTASK_PROPERTY_NAME);
        if (null != taskNameString) {
            setTaskName(taskNameString);
        }
        String startupActiveWinString = props.getProperty(STARTUP_ACTIVE_WIN);
        if (null != startupActiveWinString) {
            activeWin = ACTIVE_WINDOW_NAME.valueOf(startupActiveWinString);
            switch (activeWin) {
                case SIMVIEW_WINDOW:
                    if (null != object2DViewJInternalFrame) {
                        activateFrame(object2DViewJInternalFrame);
                    }
                    break;

                case CRCL_CLIENT_WINDOW:
                    if (null != pendantClientJInternalFrame) {
                        activateFrame(pendantClientJInternalFrame);
                    }
                    break;

                case DATABASE_SETUP_WINDOW:
                    if (null != dbSetupJInternalFrame) {
                        activateFrame(dbSetupJInternalFrame);
                    }
                    break;

                case ERRLOG_WINDOW:
                    if (null != logDisplayJInternalFrame) {
                        activateFrame(logDisplayJInternalFrame);
                    }
                    break;

                case PDDL_EXECUTOR_WINDOW:
                    if (null != pddlExecutorJInternalFrame1) {
                        activateFrame(pddlExecutorJInternalFrame1);
                    }
                    break;

                case PDDL_PLANNER_WINDOW:
                    if (null != pddlPlannerJInternalFrame) {
                        activateFrame(pddlPlannerJInternalFrame);
                    }
                    break;
            }
        }
    }

    @Override
    public void saveProperties() throws IOException {
        File propsParent = propertiesFile.getParentFile();
        if (propsParent == null) {
            System.err.println("propertiesFile.getParentFile() returned null : propertiesFile=" + propertiesFile);
            return;
        }
        if (!propsParent.exists()) {
            System.out.println("Directory " + propsParent + " does not exist. (Creating it now.)");
            propsParent.mkdirs();
        }
        Map<String, String> propsMap = new HashMap<>();
//        propsMap.put(PROGRAMEXECUTABLE, jTextFieldPlannerProgramExecutable.getText());
//        propsMap.put(PDDLDOMAIN, jTextFieldPddlDomainFile.getText());
//        propsMap.put(PDDLPROBLEM, jTextFieldPddlProblem.getText());
////        propsMap.put(PDDLOUTPUT, jTextFieldPddlOutputActions.getText());
//        propsMap.put(PDDLADDARGS, jTextFieldAdditionalArgs.getText());
        propsMap.put(STARTUPPDDLPLANNER, Boolean.toString(jCheckBoxMenuItemStartupPDDLPlanner.isSelected()));
        propsMap.put(STARTUPPDDLEXECUTOR, Boolean.toString(jCheckBoxMenuItemStartupPDDLExecutor.isSelected()));
        propsMap.put(STARTUPPDDLOBJECTSP, Boolean.toString(jCheckBoxMenuItemStartupObjectSP.isSelected()));
        propsMap.put(STARTUPPDDLOBJECTVIEW, Boolean.toString(jCheckBoxMenuItemStartupObject2DView.isSelected()));
        propsMap.put(STARTUPROBOTCRCLCLIENT, Boolean.toString(jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()));
        propsMap.put(STARTUPROBOTCRCLSIMSERVER, Boolean.toString(jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()));
        propsMap.put(STARTUPROBOTCRCLFANUCSERVER, Boolean.toString(jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()));
        propsMap.put(STARTUPROBOTCRCLMOTOMANSERVER, Boolean.toString(jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()));
        propsMap.put(STARTUPCONNECTDATABASE, Boolean.toString(jCheckBoxMenuItemConnectToDatabaseOnStartup.isSelected()));
        propsMap.put(STARTUPCONNECTVISION, Boolean.toString(jCheckBoxMenuItemConnectToVisionOnStartup.isSelected()));
        propsMap.put(STARTUPEXPLOREGRAPHDB, Boolean.toString(jCheckBoxMenuItemExploreGraphDbStartup.isSelected()));
        propsMap.put(STARTUPCRCLWEBAPP, Boolean.toString(jCheckBoxMenuItemStartupCRCLWebApp.isSelected()));
        propsMap.put(CRCLWEBAPPPORT, Integer.toString(crclWebServerHttpPort));
        propsMap.put(STARTUP_ACTIVE_WIN, activeWin.toString());
        setDefaultRobotName();
        propsMap.put(APRSROBOT_PROPERTY_NAME, robotName);
        if (null != taskName) {
            propsMap.put(APRSTASK_PROPERTY_NAME, taskName);
        }
        if (null != fanucCRCLMain) {
            this.fanucCrclPort = fanucCRCLMain.getLocalPort();
            this.fanucRobotHost = fanucCRCLMain.getRemoteRobotHost();
            propsMap.put(FANUC_CRCL_LOCAL_PORT, Integer.toString(fanucCrclPort));
            propsMap.put(FANUC_ROBOT_HOST, fanucRobotHost);
        }
        if (null != motomanCrclServerJInternalFrame) {
            this.motomanCrclPort = motomanCrclServerJInternalFrame.getCrclPort();
            propsMap.put(MOTOMAN_CRCL_LOCAL_PORT, Integer.toString(motomanCrclPort));
        }

        Properties props = new Properties();
        props.putAll(propsMap);
        System.out.println("AprsJFrame saving properties to " + propertiesFile.getCanonicalPath());
        try (FileWriter fw = new FileWriter(propertiesFile)) {
            props.store(fw, "");
        }
        updateSubPropertiesFiles();
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.saveProperties();
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.saveProperties();
        }
        if (null != this.visionToDbJInternalFrame) {
            this.visionToDbJInternalFrame.saveProperties();
        }
        if (null != this.object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.saveProperties();
        }
        if (null != this.pendantClientJInternalFrame) {
            pendantClientJInternalFrame.saveProperties();
        }
        if (null != this.simServerJInternalFrame) {
            simServerJInternalFrame.saveProperties();
        }
        if (null != this.motomanCrclServerJInternalFrame) {
            motomanCrclServerJInternalFrame.saveProperties();
        }
        if (null != dbSetup) {
            File dbPropsFile = new File(propertiesDirectory, this.propertiesFileBaseString + "_dbsetup.txt");
            if (null != dbSetupJInternalFrame) {
                dbSetupJInternalFrame.setPropertiesFile(dbPropsFile);
            }
            DbSetupBuilder.savePropertiesFile(dbPropsFile, dbSetup);
        }
    }
    private static final String STARTUP_ACTIVE_WIN = "STARTUP_ACTIVE_WIN";
    private static final String APRSTASK_PROPERTY_NAME = "aprs.taskName";
    private static final String APRSROBOT_PROPERTY_NAME = "aprs.robotName";

    private void setDefaultRobotName() {
        if (null == robotName) {
            if (fanucCRCLMain != null) {
                setRobotName("Fanuc");
            } else if (motomanCrclServerJInternalFrame != null) {
                setRobotName("Motoman");
            } else if (simServerJInternalFrame != null) {
                setRobotName("Simulated");
            } else if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
                setRobotName("Fanuc");
            } else if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
                setRobotName("Motoman");
            } else if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
                setRobotName("Simulated");
            }
        }
    }

    private static final String STARTUPPDDLPLANNER = "startup.pddl.planner";
    private static final String STARTUPPDDLEXECUTOR = "startup.pddl.executor";
    private static final String STARTUPPDDLOBJECTSP = "startup.pddl.objectsp";
    private static final String STARTUPPDDLOBJECTVIEW = "startup.pddl.objectview";
    private static final String STARTUPROBOTCRCLCLIENT = "startup.robotcrclclient";
    private static final String STARTUPROBOTCRCLSIMSERVER = "startup.robotcrclsimserver";
    private static final String STARTUPROBOTCRCLFANUCSERVER = "startup.robotcrclfanucserver";
    private static final String STARTUPROBOTCRCLMOTOMANSERVER = "startup.robotcrclmotomanserver";

    private static final String STARTUPCONNECTDATABASE = "startup.connectdatabase";
    private static final String STARTUPCONNECTVISION = "startup.connectvision";
    private static final String STARTUPEXPLOREGRAPHDB = "startup.exploreGraphDb";
    private static final String STARTUPCRCLWEBAPP = "startup.crclWebApp";
    private static final String CRCLWEBAPPPORT = "crclWebApp.httpPort";
    private static final String FANUC_CRCL_LOCAL_PORT = "fanuc.crclLocalPort";
    private static final String FANUC_ROBOT_HOST = "fanuc.robotHost";
    private static final String MOTOMAN_CRCL_LOCAL_PORT = "motoman.crclLocalPort";

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        String prefLaf = "Nimbus"; // "GTK+"; // Nimbus, Metal ...
        /* Set the preferred look and feel */

        if (prefLaf != null) {
            /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
             */
            try {
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                    System.out.println("info.getName() = " + info.getName());
                    if (prefLaf.equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;

                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
                java.util.logging.Logger.getLogger(AprsJFrame.class
                        .getName()).log(java.util.logging.Level.SEVERE, null, ex);

            }
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AprsJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectDatabase;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectToDatabaseOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectToVisionOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectVision;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemExploreGraphDbStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowDatabaseSetup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupCRCLWebApp;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupFanucCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupMotomanCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObject2DView;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObjectSP;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLExecutor;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLPlanner;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobotCrclGUI;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobtCRCLSimServer;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuExecute;
    private javax.swing.JMenuItem jMenuItemContinue;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemImmediateAbort;
    private javax.swing.JMenuItem jMenuItemLoadProperties;
    private javax.swing.JMenuItem jMenuItemLoadPropertiesFile;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSavePropsAs;
    private javax.swing.JMenuItem jMenuItemStartActionList;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    // End of variables declaration//GEN-END:variables

    @Override
    public File getPropertiesFile() {
        return propertiesFile;
    }

    @Override
    public final void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
        if (null == propertiesFile) {
            System.err.println("propertiesFile == null");
            return;
        }
        propertiesDirectory = propertiesFile.getParentFile();
        if (null == propertiesDirectory) {
            System.err.println("propertiesFile.getParentFile() returned null : propertiesFile=" + propertiesFile);
            return;
        }
        if (!propertiesDirectory.exists()) {
            try {
                System.out.println("Directory " + propertiesDirectory.getCanonicalPath() + " does not exist. (Creating it now!)");
                propertiesDirectory.mkdirs();

            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            updateSubPropertiesFiles();

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String propertiesFileBaseString = "";

    public void updateSubPropertiesFiles() throws IOException {
        String base = propertiesFile.getName();
        int pindex = base.indexOf('.');
        if (pindex > 0) {
            base = base.substring(0, pindex);
        }
        propertiesFileBaseString = base;
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_pddlPlanner.txt"));
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.setPropertiesFile(new File(propertiesDirectory, base + "_actionsToCrclProperties.txt"));
        }
        if (null != this.visionToDbJInternalFrame) {
            visionToDbJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_visionToDBProperties.txt"));
        }
        if (null != this.object2DViewJInternalFrame) {
            object2DViewJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_object2DViewProperties.txt"));
        }
        if (null != lastAprsPropertiesFileFile) {
            Properties props = new Properties();
            props.put("lastPropertyFileName", propertiesFile.getCanonicalPath());
            try (FileWriter fw = new FileWriter(lastAprsPropertiesFileFile)) {
                props.store(fw, "");
            }
        }
        if (null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_crclPendantClientProperties.txt"));
        }

        if (null != simServerJInternalFrame) {
            simServerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_crclSimServerProperties.txt"));
        }
        if (null != motomanCrclServerJInternalFrame) {
            motomanCrclServerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, base + "_motomanCrclServerProperties.txt"));
        }
        if (null != dbSetupJInternalFrame) {
            File dbPropsFile = new File(propertiesDirectory, this.propertiesFileBaseString + "_dbsetup.txt");
            dbSetupJInternalFrame.setPropertiesFile(dbPropsFile);
        }
    }

    Future disconnectDatabaseFuture = null;

    private void startDisconnectDatabase() {
        jCheckBoxMenuItemConnectDatabase.setSelected(false);
        jCheckBoxMenuItemConnectDatabase.setEnabled(false);
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        disconnectDatabaseFuture = connectService.submit(this::disconnectDatabase);
    }

    private void disconnectDatabase() {

        try {
            if (null != connectDatabaseFuture) {
                connectDatabaseFuture.cancel(true);
                connectDatabaseFuture = null;
            }
            DbSetupPublisher dbSetupPublisher = dbSetupJInternalFrame.getDbSetupPublisher();
            dbSetupPublisher.setDbSetup(new DbSetupBuilder().setup(dbSetupPublisher.getDbSetup()).connected(false).build());
            List<Future<?>> futures = dbSetupPublisher.notifyAllDbSetupListeners();
            for (Future<?> f : futures) {
                if (!f.isDone() && !f.isCancelled()) {
                    try {
                        f.get(100, TimeUnit.MILLISECONDS);

                    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                        Logger.getLogger(AprsJFrame.class
                                .getName()).log(Level.SEVERE, null, ex);

                    }
                }
            }
            System.out.println("Finished disconnect from database.");

        } catch (IOException iOException) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, iOException);
        }
    }

    private int priority;

    /**
     * Get the value of priority
     *
     * @return the value of priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the value of priority
     *
     * @param priority new value of priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    private volatile boolean closing = false;

    @Override
    public void close() throws Exception {
        closing = true;
        System.out.println("AprsJFrame.close()");
        try {
            closePddlPlanner();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        try {
            closeActionsToCrclJInternalFrame();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        try {
            stopCrclWebApp();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        if (null != connectDatabaseFuture) {
            connectDatabaseFuture.cancel(true);
            connectDatabaseFuture = null;
        }
        if (null != disconnectDatabaseFuture) {
            disconnectDatabaseFuture.cancel(true);
            disconnectDatabaseFuture = null;
        }
        if (null != simServerJInternalFrame) {
            simServerJInternalFrame.close();
            simServerJInternalFrame.setVisible(false);
            simServerJInternalFrame = null;
        }
        disconnectDatabase();
        connectService.shutdownNow();
        connectService.awaitTermination(100, TimeUnit.MILLISECONDS);
        this.setVisible(false);
    }

    private void closeActionsToCrclJInternalFrame() throws Exception {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.close();
            pddlExecutorJInternalFrame1.setVisible(false);
            pddlExecutorJInternalFrame1.dispose();
            pddlExecutorJInternalFrame1 = null;
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
            }
        }
    }

    private void closePddlPlanner() throws Exception {
        if (null != pddlPlannerJInternalFrame) {
            pddlPlannerJInternalFrame.close();
            pddlPlannerJInternalFrame.setVisible(false);
            pddlPlannerJInternalFrame.dispose();
            pddlPlannerJInternalFrame = null;
        }
    }

    public XFuture<Boolean> continueCurrentProgram() {
        if (null != pendantClientJInternalFrame) {
            return pendantClientJInternalFrame.continueCurrentProgram();
        } else {
            return XFuture.completedFuture(false);
        }
    }

}
