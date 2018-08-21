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
package aprs.system;

import aprs.cachedcomponents.CachedCheckBox;
import aprs.misc.ActiveWinEnum;
import aprs.misc.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import crcl.ui.misc.MultiLineStringJPanel;
import java.awt.Container;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;
import javax.swing.DesktopManager;
import javax.swing.JMenu;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;

/**
 * AprsSystemInterface is the container for one robotic system in the APRS
 * (Agility Performance of Robotic Systems) framework.
 *
 * Internal windows are used to represent each of the modules within the system.
 * Vision, Sensor Processing, Planning, Execution, and the CRCL (Canonical Robot
 * Command Language) client and server.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("unused")
class AprsSystemDisplayJFrame extends javax.swing.JFrame {

    @MonotonicNonNull private volatile AprsSystem aprsSystem = null;

    @SafeEffect
    public void setAprsSystem(AprsSystem aprsSystem) {
        this.aprsSystem = aprsSystem;
    }

    private volatile boolean showingException = false;

    @SafeEffect
    public void showException(Exception ex) {
        if (!showingException) {
            showingException = true;
            javax.swing.SwingUtilities.invokeLater(() -> showExceptionInternal(ex));
        }
    }

    @UIEffect
    private void showExceptionInternal(Exception ex) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw, true)) {
            ex.printStackTrace(pw);
        } catch (Exception ex1) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex1);
        }
        String exText = sw.toString();
        MultiLineStringJPanel.showText(exText, this, "Exception from " + this.getTitle(), false).thenRun(() -> showingException = false);
    }

    CachedCheckBox logCrclProgramsCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemLogCrclPrograms);
    }

    CachedCheckBox steppingCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStepping);
    }

    @UIEffect
    public boolean addMenu(JMenu menu) {
        if (null == menu) {
            return false;
        }
        for (int i = 0; i < jMenuBar1.getMenuCount(); i++) {
            JMenu menuFromBar = jMenuBar1.getMenu(i);
            if (menuFromBar == null) {
                continue; // paranoid
            }
            if (menuFromBar == menu) {
                return false;
            }
            if (menuFromBar.getText().equals(menu.getText())) {
                return false;
            }
        }
        this.jMenuBar1.add(menu);
        return true;
    }

    CachedCheckBox snapshotsCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemSnapshotImageSize);
    }

    @UIEffect
    public void updateConnectedRobotDisplay(boolean connected, @Nullable String robotName, @Nullable String crclHost, int crclPort) {
        jCheckBoxMenuItemConnectedRobot.setSelected(connected);
        jCheckBoxMenuItemConnectedRobot.setText("Robot (CRCL " + robotName + " " + crclHost + ":" + crclPort + " )");
    }

    CachedCheckBox connectedRobotCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemConnectedRobot);
    }

    CachedCheckBox useTeachTableCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemUseTeachTable);
    }

    CachedCheckBox continousDemoCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemContinuousDemo);
    }

    @UIEffect
    public void addToDesktopPane(JInternalFrame internalFrame) {
        JInternalFrame[] prevFrames = jDesktopPane1.getAllFramesInLayer(JLayeredPane.DEFAULT_LAYER);
        for (JInternalFrame prevFrame : prevFrames) {
            if (internalFrame == prevFrame) {
                throw new IllegalStateException("internalFrame=" + internalFrame + " already in prevFrames=" + Arrays.toString(prevFrames) + " of jDesktopPane1=" + jDesktopPane1);
            }
        }
        Container internalFrameParent = internalFrame.getParent();
        if (internalFrameParent != null) {
            throw new IllegalStateException("internalFrame=" + internalFrame + " already hasParent=" + internalFrameParent + "  ((jDesktopPane1=" + jDesktopPane1 + ")==internalFrameParent) =" + (jDesktopPane1 == internalFrameParent));
        }
        jDesktopPane1.add(internalFrame, JLayeredPane.DEFAULT_LAYER);
    }

    /**
     * Creates new AprsSystemInterface using a default properties file.
     */
    @SuppressWarnings({"initialization", "nullness"})
    @UIEffect
    public AprsSystemDisplayJFrame() {
        try {
            initComponents();
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    CachedCheckBox kitInspectionStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemKitInspectionStartup);
    }

    CachedCheckBox pddlPlannerStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupPDDLPlanner);
    }

    CachedCheckBox executorStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupExecutor);
    }

    CachedCheckBox objectSpStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupObjectSP);
    }

    CachedCheckBox object2DViewStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupObject2DView);
    }

    CachedCheckBox robotCrclGUIStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupRobotCrclGUI);
    }

    CachedCheckBox robotCrclSimServerStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupRobtCRCLSimServer);
    }

    CachedCheckBox robotCrclFanucServerStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupFanucCRCLServer);
    }

    CachedCheckBox robotCrclMotomanServerStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemStartupMotomanCRCLServer);
    }

    CachedCheckBox exploreGraphDBStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemExploreGraphDbStartup);
    }

    CachedCheckBox showDatabaseSetupOnStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemShowDatabaseSetupOnStartup);
    }

    CachedCheckBox onStartupConnectDatabaseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemConnectDatabaseOnStartup);
    }

    CachedCheckBox connectVisionOnStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemConnectVisionOnStartup);
    }

    @UIEffect
    private void commonInit() {
        try {
            URL aprsPngUrl = AprsSystemDisplayJFrame.class.getResource("aprs.png");
            if (null != aprsPngUrl) {
                setIconImage(ImageIO.read(aprsPngUrl));
            } else {
                Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.WARNING, "getResource(\"aprs.png\") returned null");
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
        setupWindowsMenu();
    }

    @UIEffect
    void closeAllInternalFrames() {
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        for (JInternalFrame f : frames) {
            jDesktopPane1.getDesktopManager().closeFrame(f);
            f.setVisible(false);
            jDesktopPane1.remove(f);
        }
    }

    @UIEffect
    void checkDeiconifyActivateAndMaximize(JInternalFrame internalFrame) {
        internalFrame.setVisible(true);
        if (checkInternalFrame(internalFrame)) {
            DesktopManager desktopManager = jDesktopPane1.getDesktopManager();
            desktopManager.deiconifyFrame(internalFrame);
            desktopManager.activateFrame(internalFrame);
            desktopManager.maximizeFrame(internalFrame);
        }
    }

    @UIEffect
    boolean checkInternalFrame(JInternalFrame frm) {
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

    @UIEffect
    void checkIconifyAndDeactivate(JInternalFrame internalFrame) {
        if (checkInternalFrame(internalFrame)) {
            DesktopManager desktopManager = jDesktopPane1.getDesktopManager();
            desktopManager.iconifyFrame(internalFrame);
            desktopManager.deactivateFrame(internalFrame);
        }
    }

    @UIEffect
    private void activateFrame(final JInternalFrame frameToShow) {
        frameToShow.setVisible(true);
        if (checkInternalFrame(frameToShow)) {
            deiconifyAndActivate(frameToShow);
            frameToShow.moveToFront();
            activeWin = stringToWin(frameToShow.getTitle());
        } else {
            setupWindowsMenu();
        }
    }

    @UIEffect
    void deiconifyAndActivate(final JInternalFrame frameToShow) {
        DesktopManager desktopManager = jDesktopPane1.getDesktopManager();
        desktopManager.deiconifyFrame(frameToShow);
        desktopManager.activateFrame(frameToShow);
    }

    private String lastFrameTitles[] = new String[0];

    @UIEffect
    void setupWindowsMenu() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            return;
        }
        if (jMenuWindow.isSelected()) {
            return;
        }
        int count = 1;
        ArrayList<JInternalFrame> framesList = new ArrayList<>(Arrays.asList(jDesktopPane1.getAllFrames()));
        framesList.sort(Comparator.comparing(JInternalFrame::getTitle));
        boolean framesChanged = false;
        if (lastFrameTitles.length != framesList.size()) {
            framesChanged = true;
            lastFrameTitles = new String[framesList.size()];
        }
        for (int i = 0; i < framesList.size(); i++) {
            JInternalFrame f = framesList.get(i);
            String title = f.getTitle();
            if (!Objects.equals(lastFrameTitles[i], title)) {
                lastFrameTitles[i] = title;
                framesChanged = true;
            }
        }
        if (!framesChanged) {
            return;
        }
        List<JMenuItem> menuItems = new ArrayList<>();
        int framesListSize = framesList.size();
        for (JInternalFrame f : framesList) {
            JMenuItem menuItem = new JMenuItem(count + " " + f.getTitle());
            final JInternalFrame frameToShow = f;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activateFrame(frameToShow);
                }
            });
            count++;
            menuItems.add(menuItem);
        }
        jMenuWindow.removeAll();
        for (JMenuItem menuItem : menuItems) {
            jMenuWindow.add(menuItem);
        }
        assert (framesListSize == menuItems.size()) :
                ("menuItems = " + menuItems + " does not match framesList = " + framesList);

        int menuItemCount = jMenuWindow.getItemCount();
        assert (framesListSize == menuItemCount) :
                ("framesListSize = " + framesListSize + " does not match menuItemCount = " + menuItemCount
                + "with framesList=" + framesList + ", menuItems=" + menuItems);

    }

    private ActiveWinEnum activeWin = ActiveWinEnum.OTHER;

    private ActiveWinEnum stringToWin(String str) {
        if (str.startsWith("CRCL Client")) {
            return ActiveWinEnum.CRCL_CLIENT_WINDOW;
        }
        if (str.startsWith("Error")) {
            return ActiveWinEnum.ERRLOG_WINDOW;
        }
        if (str.startsWith("Object2D")) {
            return ActiveWinEnum.SIMVIEW_WINDOW;
        }
        if (str.startsWith("[Object SP]") || str.endsWith("Vision To Database")) {
            return ActiveWinEnum.VISION_TO_DB_WINDOW;
        }
        if (str.startsWith("CRCL Simulation Server")) {
            return ActiveWinEnum.CRCL_CLIENT_WINDOW;
        }
        if (str.startsWith("Database Setup")) {
            return ActiveWinEnum.DATABASE_SETUP_WINDOW;
        }
        if (str.startsWith("PDDL Planner")) {
            return ActiveWinEnum.PDDL_PLANNER_WINDOW;
        }
        if (str.startsWith("PDDL Actions to CRCL") || str.endsWith("(Executor)")) {
            return ActiveWinEnum.PDDL_EXECUTOR_WINDOW;
        }
        if (str.startsWith("Kit") || str.endsWith("(Inspection)")) {
            return ActiveWinEnum.KIT_INSPECTION_WINDOW;
        }
        return ActiveWinEnum.OTHER;
    }

    CachedCheckBox connectVisionCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemConnectVision);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    @UIEffect
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
        jCheckBoxMenuItemReverse = new javax.swing.JCheckBoxMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItemStartupPDDLPlanner = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupExecutor = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObjectSP = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObject2DView = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupRobotCrclGUI = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemShowDatabaseSetupOnStartup = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemStartupRobtCRCLSimServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupFanucCRCLServer = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupMotomanCRCLServer = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemConnectDatabaseOnStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectVisionOnStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemExploreGraphDbStartup = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemKitInspectionStartup = new javax.swing.JCheckBoxMenuItem();
        jMenuWindow = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jCheckBoxMenuItemConnectDatabase = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectVision = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemConnectedRobot = new javax.swing.JCheckBoxMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jCheckBoxMenuItemEnableDebugDumpstacks = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetPoseMaxLimits = new javax.swing.JMenuItem();
        jMenuItemSetPoseMinLimits = new javax.swing.JMenuItem();
        jCheckBoxMenuItemSnapshotImageSize = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemReloadSimFilesOnReverse = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemUseTeachTable = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemLogCrclPrograms = new javax.swing.JCheckBoxMenuItem();
        jMenuExecute = new javax.swing.JMenu();
        jMenuItemStartActionList = new javax.swing.JMenuItem();
        jMenuItemImmediateAbort = new javax.swing.JMenuItem();
        jMenuItemContinueActionList = new javax.swing.JMenuItem();
        jMenuItemReset = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinuousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jMenuItemDebugAction = new javax.swing.JMenuItem();
        jCheckBoxMenuItemForceFakeTake = new javax.swing.JCheckBoxMenuItem();
        jMenuItemCreateActionListFromVision = new javax.swing.JMenuItem();
        jMenuItemLookFor = new javax.swing.JMenuItem();
        jMenuItemClearErrors = new javax.swing.JMenuItem();
        jCheckBoxMenuItemStepping = new javax.swing.JCheckBoxMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

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

        jCheckBoxMenuItemReverse.setText("Reverse");
        jCheckBoxMenuItemReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemReverseActionPerformed(evt);
            }
        });
        jMenu1.add(jCheckBoxMenuItemReverse);

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

        jCheckBoxMenuItemStartupExecutor.setSelected(true);
        jCheckBoxMenuItemStartupExecutor.setText("Executor");
        jCheckBoxMenuItemStartupExecutor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupExecutorActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupExecutor);

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

        jCheckBoxMenuItemShowDatabaseSetupOnStartup.setSelected(true);
        jCheckBoxMenuItemShowDatabaseSetupOnStartup.setText("Database Setup");
        jCheckBoxMenuItemShowDatabaseSetupOnStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowDatabaseSetupOnStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemShowDatabaseSetupOnStartup);
        jMenu3.add(jSeparator2);

        jCheckBoxMenuItemStartupRobtCRCLSimServer.setText("Robot CRCL SimServer");
        jCheckBoxMenuItemStartupRobtCRCLSimServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobtCRCLSimServer);

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
        jMenu3.add(jSeparator3);

        jCheckBoxMenuItemConnectDatabaseOnStartup.setText("Connect To Database On Startup");
        jCheckBoxMenuItemConnectDatabaseOnStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectDatabaseOnStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemConnectDatabaseOnStartup);

        jCheckBoxMenuItemConnectVisionOnStartup.setText("Connect To Vision On Startup");
        jMenu3.add(jCheckBoxMenuItemConnectVisionOnStartup);

        jCheckBoxMenuItemExploreGraphDbStartup.setText("Explore Graph Database");
        jCheckBoxMenuItemExploreGraphDbStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemExploreGraphDbStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemExploreGraphDbStartup);

        jCheckBoxMenuItemKitInspectionStartup.setText("Kit Inspection");
        jCheckBoxMenuItemKitInspectionStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemKitInspectionStartupActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemKitInspectionStartup);

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

        jCheckBoxMenuItemConnectedRobot.setText("Robot (CRCL ... )");
        jCheckBoxMenuItemConnectedRobot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemConnectedRobotActionPerformed(evt);
            }
        });
        jMenu2.add(jCheckBoxMenuItemConnectedRobot);

        jMenuBar1.add(jMenu2);

        jMenu4.setText("Options");

        jCheckBoxMenuItemEnableDebugDumpstacks.setText("Enable Debug DumpStacks");
        jCheckBoxMenuItemEnableDebugDumpstacks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed(evt);
            }
        });
        jMenu4.add(jCheckBoxMenuItemEnableDebugDumpstacks);

        jMenuItemSetPoseMaxLimits.setText("Set Pose Max Limits ... (+10000,+10000,+10000)    ...");
        jMenuItemSetPoseMaxLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetPoseMaxLimitsActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItemSetPoseMaxLimits);

        jMenuItemSetPoseMinLimits.setText("Set Pose Min Limits ... (-10000,-10000,-10000)    ...");
        jMenuItemSetPoseMinLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetPoseMinLimitsActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItemSetPoseMinLimits);

        jCheckBoxMenuItemSnapshotImageSize.setText("Snapshot Image size (800 x 600 )");
        jCheckBoxMenuItemSnapshotImageSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSnapshotImageSizeActionPerformed(evt);
            }
        });
        jMenu4.add(jCheckBoxMenuItemSnapshotImageSize);

        jCheckBoxMenuItemReloadSimFilesOnReverse.setText("Reload Sim Files on Reverse");
        jMenu4.add(jCheckBoxMenuItemReloadSimFilesOnReverse);

        jCheckBoxMenuItemUseTeachTable.setText("Use Teach Table");
        jCheckBoxMenuItemUseTeachTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemUseTeachTableActionPerformed(evt);
            }
        });
        jMenu4.add(jCheckBoxMenuItemUseTeachTable);

        jCheckBoxMenuItemLogCrclPrograms.setSelected(true);
        jCheckBoxMenuItemLogCrclPrograms.setText("Log CRCL Programs");
        jMenu4.add(jCheckBoxMenuItemLogCrclPrograms);

        jMenuBar1.add(jMenu4);

        jMenuExecute.setText("Execute");

        jMenuItemStartActionList.setText("Start Action List");
        jMenuItemStartActionList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartActionListActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemStartActionList);

        jMenuItemImmediateAbort.setText("Immediate Abort");
        jMenuItemImmediateAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemImmediateAbortActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemImmediateAbort);

        jMenuItemContinueActionList.setText("Continue Action List");
        jMenuItemContinueActionList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemContinueActionListActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemContinueActionList);

        jMenuItemReset.setText("Reset");
        jMenuItemReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemResetActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemReset);

        jCheckBoxMenuItemContinuousDemo.setText("Continuous Demo");
        jCheckBoxMenuItemContinuousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinuousDemoActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemContinuousDemo);

        jCheckBoxMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAUSE, 0));
        jCheckBoxMenuItemPause.setText("Pause");
        jCheckBoxMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemPause);

        jMenuItemDebugAction.setText("Debug Action");
        jMenuItemDebugAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDebugActionActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemDebugAction);

        jCheckBoxMenuItemForceFakeTake.setText("Force Fake Take");
        jCheckBoxMenuItemForceFakeTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemForceFakeTakeActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemForceFakeTake);

        jMenuItemCreateActionListFromVision.setText("Create Action List From Vision");
        jMenuItemCreateActionListFromVision.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCreateActionListFromVisionActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemCreateActionListFromVision);

        jMenuItemLookFor.setText("Look For");
        jMenuItemLookFor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLookForActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemLookFor);

        jMenuItemClearErrors.setText("Clear Errors");
        jMenuItemClearErrors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemClearErrorsActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemClearErrors);

        jCheckBoxMenuItemStepping.setText("Single Stepping");
        jCheckBoxMenuItemStepping.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSteppingActionPerformed(evt);
            }
        });
        jMenuExecute.add(jCheckBoxMenuItemStepping);

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

    
    @SafeEffect
    private void startPddlPlanner() {
        if (null != aprsSystem) {
            aprsSystem.startPddlPlanner();
        }
    }

    @SafeEffect
    private void saveProperties() throws IOException {
        if (null != aprsSystem) {
            aprsSystem.saveProperties();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @SafeEffect
    private void closePddlPlanner() {
        if (null != aprsSystem) {
            aprsSystem.closePddlPlanner();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }
    
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
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed

    private void startActionListExecutor() {
        if (null != aprsSystem) {
            aprsSystem.startActionListExecutor();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void closeActionsListExcecutor() {
        if (null != aprsSystem) {
            aprsSystem.closeActionsListExcecutor();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemStartupExecutorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupExecutorActionPerformed
        try {
            if (jCheckBoxMenuItemStartupExecutor.isSelected()) {
                startActionListExecutor();
            } else {
                closeActionsListExcecutor();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupExecutorActionPerformed

    private void startVisionToDbJinternalFrame() {
        if (null != aprsSystem) {
            aprsSystem.startVisionToDbJinternalFrame();
        }
    }

    private void jCheckBoxMenuItemStartupObjectSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed

    private void startObject2DJinternalFrame() {
        if (null != aprsSystem) {
            aprsSystem.startObject2DJinternalFrame();
        }
    }


    private void jCheckBoxMenuItemStartupObject2DViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed

    private void loadProperties() throws IOException {
        if (null != aprsSystem) {
            aprsSystem.loadProperties();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jMenuItemLoadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesActionPerformed
        try {
            this.loadProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemLoadPropertiesActionPerformed

    private void jMenuItemSavePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropertiesActionPerformed
        try {
            this.saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemSavePropertiesActionPerformed

    private void jMenuItemLoadPropertiesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesFileActionPerformed
        browseOpenPropertiesFile();
    }//GEN-LAST:event_jMenuItemLoadPropertiesFileActionPerformed

    private File getPropertiesDirectory() {
        if (null != aprsSystem) {
            return aprsSystem.getPropertiesDirectory();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @Nullable public File choosePropertiesFileToOpen() {
        JFileChooser chooser = new JFileChooser(getPropertiesDirectory());
        FileFilter filter = new FileNameExtensionFilter("Text properties files.", "txt");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Open APRS System properties file.");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private void loadSelectedPropertiesFile(File selectedFile) throws IOException {
        if (null != aprsSystem) {
            aprsSystem.loadSelectedPropertiesFile(selectedFile);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private XFutureVoid initLoggerWindow() {
        if (null != aprsSystem) {
            return aprsSystem.initLoggerWindow();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    /**
     * Query the user to select a properties file to open.
     */
    private void browseOpenPropertiesFile() {
        File selectedFile = choosePropertiesFileToOpen();
        if (null != selectedFile) {
            try {
                loadSelectedPropertiesFile(selectedFile);
//                initPropertiesFile();

            } catch (IOException ex) {
                Logger.getLogger(AprsSystemDisplayJFrame.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
            this.initLoggerWindow()
                    .thenRun(this::commonInit);
        }
    }

    private void close() {
        if (null != aprsSystem) {
            aprsSystem.close();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        try {
            close();

        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
        System.exit(0);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void startCrclClientJInternalFrame() {
        if (null != aprsSystem) {
            aprsSystem.startCrclClientJInternalFrame();
        }
    }

    private void jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed
        try {
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startCrclClientJInternalFrame();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed

    private void showDatabaseSetupWindow() {
        if (null != aprsSystem) {
            aprsSystem.showDatabaseSetupWindow();
        }
    }

    private void hideDatabaseSetupWindow() {
        if (null != aprsSystem) {
            aprsSystem.hideDatabaseSetupWindow();
        }
    }

    private void jCheckBoxMenuItemShowDatabaseSetupOnStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowDatabaseSetupOnStartupActionPerformed
        try {
            if (jCheckBoxMenuItemShowDatabaseSetupOnStartup.isSelected()) {
                showDatabaseSetupWindow();
            } else {
                hideDatabaseSetupWindow();
            }
            setupWindowsMenu();
            saveProperties();

        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemShowDatabaseSetupOnStartupActionPerformed

    private void startSimServerJInternalFrame() {
        if (null != aprsSystem) {
            aprsSystem.startSimServerJInternalFrame();
        }
    }
    private void jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed
        if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
            startSimServerJInternalFrame();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobtCRCLSimServerActionPerformed

    private void startFanucCrclServer() {
        if (null != aprsSystem) {
            aprsSystem.startFanucCrclServer();
        }
    }

    private void jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupFanucCRCLServer.isSelected()) {
            startFanucCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupFanucCRCLServerActionPerformed

    private void startExploreGraphDb() {
        if (null != aprsSystem) {
            aprsSystem.startExploreGraphDb();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void closeExploreGraphDb() {
        if (null != aprsSystem) {
            aprsSystem.closeExploreGraphDb();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemExploreGraphDbStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemExploreGraphDbStartupActionPerformed
        if (this.jCheckBoxMenuItemExploreGraphDbStartup.isSelected()) {
            startExploreGraphDb();
        } else {
            closeExploreGraphDb();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemExploreGraphDbStartupActionPerformed

    private XFuture<Boolean> startConnectDatabase() {
        if (null != aprsSystem) {
            return aprsSystem.startConnectDatabase();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private XFuture<?> startDisconnectDatabase() {
        if (null != aprsSystem) {
            return aprsSystem.startDisconnectDatabase();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemConnectDatabaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectDatabaseActionPerformed
        if (this.jCheckBoxMenuItemConnectDatabase.isSelected()) {
            this.jCheckBoxMenuItemConnectDatabase.setEnabled(false);
            startConnectDatabase()
                    .always(() -> safeSetMenuItemConnectDatabaseEnabled(true));
        } else {
            this.jCheckBoxMenuItemConnectDatabase.setEnabled(false);
            startDisconnectDatabase()
                    .always(() -> safeSetMenuItemConnectDatabaseEnabled(true));
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectDatabaseActionPerformed

    @SafeEffect
    private void safeSetMenuItemConnectDatabaseEnabled(boolean enabled) {
        Utils.runOnDispatchThread(() -> this.jCheckBoxMenuItemConnectDatabase.setEnabled(enabled));
    }


    private void jCheckBoxMenuItemConnectDatabaseOnStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectDatabaseOnStartupActionPerformed
        try {
            saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectDatabaseOnStartupActionPerformed

    private void startMotomanCrclServer() {
        if (null != aprsSystem) {
            aprsSystem.startMotomanCrclServer();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
            startMotomanCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed

    private void jMenuItemSavePropsAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropsAsActionPerformed
        browseSavePropertiesFileAs();
    }//GEN-LAST:event_jMenuItemSavePropsAsActionPerformed

    @Nullable
    private File choosePropertiesFileToSaveAs() {
        JFileChooser chooser = new JFileChooser(getPropertiesDirectory());
        FileFilter filter = new FileNameExtensionFilter("Text properties files.", "txt");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Choose new APRS System properties file to create (save as).");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private void setPropertiesFile(File f) {
        if (null != aprsSystem) {
            aprsSystem.setPropertiesFile(f);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    /**
     * Query the user to select a properties file to save.
     */
    private void browseSavePropertiesFileAs() {
        File selectedFile = choosePropertiesFileToSaveAs();
        if (null != selectedFile) {
            try {
                setPropertiesFile(selectedFile);
                this.saveProperties();
            } catch (IOException ex) {
                Logger.getLogger(AprsSystemDisplayJFrame.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
    }

    private void immediateAbort() {
        if (null != aprsSystem) {
            aprsSystem.immediateAbort();
        }
    }

    private void jMenuItemImmediateAbortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortActionPerformed
        this.immediateAbort();
    }//GEN-LAST:event_jMenuItemImmediateAbortActionPerformed

    private void setTitleErrorString(@Nullable String titleError) {
        if (null != aprsSystem) {
            aprsSystem.setTitleErrorString(titleError);
        }
    }

    private void notifyPauseFutures() {
        if (null != aprsSystem) {
            aprsSystem.notifyPauseFutures();
        } else {
            throw new IllegalStateException("this=" + this + ", aprsSystem==null");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void startActions(String label) {
        if (null != aprsSystem) {
            aprsSystem.startActions(label, jCheckBoxMenuItemReverse.isSelected());
        } else {
            throw new IllegalStateException("this=" + this + ", aprsSystem==null");
        }
    }

    private void jMenuItemStartActionListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartActionListActionPerformed
        setTitleErrorString(null);
        jCheckBoxMenuItemPause.setSelected(false);
        notifyPauseFutures();
        this.startActions("user");
    }//GEN-LAST:event_jMenuItemStartActionListActionPerformed

    private void connectVision() {
        if (null != aprsSystem) {
            aprsSystem.connectVision();
        }
    }

    @Override
    public void setVisible(boolean visible) {
//        System.out.println("visible = " + visible);
        super.setVisible(visible);
    }

    private void disconnectVision() {
        if (null != aprsSystem) {
            aprsSystem.disconnectVision();
        }
    }
    private void jCheckBoxMenuItemConnectVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectVisionActionPerformed
        if (jCheckBoxMenuItemConnectVision.isSelected()) {
            connectVision();
        } else {
            disconnectVision();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectVisionActionPerformed

    private void startKitInspection() {
        if (null != aprsSystem) {
            aprsSystem.startKitInspection();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void reset() {
        if (null != aprsSystem) {
            aprsSystem.reset();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }


    private void jCheckBoxMenuItemKitInspectionStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemKitInspectionStartupActionPerformed
        this.startKitInspection();
    }//GEN-LAST:event_jCheckBoxMenuItemKitInspectionStartupActionPerformed

    private void jMenuItemResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetActionPerformed
        reset();
    }//GEN-LAST:event_jMenuItemResetActionPerformed

    private void jCheckBoxMenuItemReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemReverseActionPerformed
        boolean reverseFlag = jCheckBoxMenuItemReverse.isSelected();
        reloadForReverse(reverseFlag);
    }//GEN-LAST:event_jCheckBoxMenuItemReverseActionPerformed

    private void reloadForReverse(boolean reverseFlag) {
        if (null != aprsSystem) {
            aprsSystem.reloadForReverse(reverseFlag);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private XFuture<Boolean> startContinuousDemo(String label, boolean reverseFlag) {
        if (null != aprsSystem) {
            return aprsSystem.startContinuousDemo(label, reverseFlag);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemContinuousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinuousDemoActionPerformed
        boolean start = jCheckBoxMenuItemContinuousDemo.isSelected();
        boolean reverseFlag = jCheckBoxMenuItemReverse.isSelected();
        setTitleErrorString(null);
        immediateAbort();
        if (start) {
            if (!jCheckBoxMenuItemContinuousDemo.isSelected()) {
                jCheckBoxMenuItemContinuousDemo.setSelected(true);
            }
            XFuture<Boolean> future = startContinuousDemo("user", reverseFlag);
        } else {
            immediateAbort();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemContinuousDemoActionPerformed

    @Nullable private XFuture<Boolean> getContinuousDemoFuture() {
        if (null != aprsSystem) {
            return aprsSystem.getContinuousDemoFuture();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void pause() {
        if (null != aprsSystem) {
            aprsSystem.pause();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void clearErrors() {
        if (null != aprsSystem) {
            aprsSystem.clearErrors();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void resume() {
        if (null != aprsSystem) {
            aprsSystem.resume();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        if (null == aprsSystem) {
            throw new IllegalStateException("null == aprsSystem");
        }
        System.out.println("jCheckBoxMenuItemPause.isSelected() = " + jCheckBoxMenuItemPause.isSelected());
        XFuture<Boolean> cdf = getContinuousDemoFuture();
        if (null != cdf) {
            System.out.println("ContinuousDemoFuture.isDone() = " + cdf.isDone());
            System.out.println("ContinuousDemoFuture.isCancelled() = " + cdf.isCancelled());
        }
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            clearErrors();
            resume();
        }
        cdf = getContinuousDemoFuture();
        if (null != cdf) {
            System.out.println("ContinuousDemoFuture.isDone() = " + cdf.isDone());
            System.out.println("ContinuousDemoFuture.isCancelled() = " + cdf.isCancelled());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseActionPerformed

    private void debugAction() {
        if (null != aprsSystem) {
            aprsSystem.debugAction();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jMenuItemDebugActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDebugActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDebugActionActionPerformed

    private void jCheckBoxMenuItemForceFakeTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemForceFakeTakeActionPerformed
        if (aprsSystem != null) {
            boolean val = jCheckBoxMenuItemForceFakeTake.isSelected();
            if (aprsSystem.getExcutorForceFakeTakeFlag() != val) {
                aprsSystem.setExecutorForceFakeTakeFlag(val);
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemForceFakeTakeActionPerformed

    @SuppressWarnings("SameParameterValue")
    private void continueActionList(String label) {
        if (null != aprsSystem) {
            aprsSystem.continueActionList(label);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jMenuItemContinueActionListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueActionListActionPerformed
        continueActionList("user");
    }//GEN-LAST:event_jMenuItemContinueActionListActionPerformed

    private void createActionListFromVision() {
        if (null != aprsSystem) {
            aprsSystem.createActionListFromVision();
        } else {
            throw new IllegalStateException("aprsSystem == null, this=" + this);
        }
    }

    private void jMenuItemCreateActionListFromVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCreateActionListFromVisionActionPerformed
        createActionListFromVision();
    }//GEN-LAST:event_jMenuItemCreateActionListFromVisionActionPerformed

    private void setMinLimit(PmCartesian cart) {
        if (null != aprsSystem) {
            aprsSystem.setMinLimit(cart);
        } else {
            throw new IllegalStateException("aprsSystem == null, this=" + this);
        }
    }

    private PmCartesian getMinLimit() {
        if (null != aprsSystem) {
            return aprsSystem.getMinLimit();
        } else {
            throw new IllegalStateException("aprsSystem == null, this=" + this);
        }
    }

    private void jMenuItemSetPoseMinLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetPoseMinLimitsActionPerformed
        PmCartesian minLimit = getMinLimit();
        String newMinLimitsString = JOptionPane.showInputDialog(this, "New Min Pose Limits",
                String.format("%+.3f,%.3f,%+.3f", minLimit.x, minLimit.y, minLimit.z));
        if (newMinLimitsString != null && newMinLimitsString.length() > 0) {
            PmCartesian cart = PmCartesian.valueOf(newMinLimitsString);
            setMinLimit(cart);
        }
    }//GEN-LAST:event_jMenuItemSetPoseMinLimitsActionPerformed

    private void setMaxLimit(PmCartesian cart) {
        if (null != aprsSystem) {
            aprsSystem.setMaxLimit(cart);
        } else {
            throw new IllegalStateException("aprsSystem == null, this=" + this);
        }
    }

    private PmCartesian getMaxLimit() {
        if (null != aprsSystem) {
            return aprsSystem.getMaxLimit();
        } else {
            throw new IllegalStateException("aprsSystem == null, this=" + this);
        }
    }

    private void jMenuItemSetPoseMaxLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetPoseMaxLimitsActionPerformed
        PmCartesian maxLimit = getMaxLimit();
        String newMaxLimitsString = JOptionPane.showInputDialog(this, "New Max Pose Limits",
                String.format("%+.3f,%.3f,%+.3f", maxLimit.x, maxLimit.y, maxLimit.z));
        if (newMaxLimitsString != null && newMaxLimitsString.length() > 0) {
            PmCartesian cart = PmCartesian.valueOf(newMaxLimitsString);
            setMaxLimit(cart);
        }
    }//GEN-LAST:event_jMenuItemSetPoseMaxLimitsActionPerformed

    private int getSnapShotWidth() {
        if (null != aprsSystem) {
            return aprsSystem.getSnapShotWidth();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private int getSnapShotHeight() {
        if (null != aprsSystem) {
            return aprsSystem.getSnapShotHeight();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemSnapshotImageSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSnapshotImageSizeActionPerformed
        boolean snapshotsEnabled = jCheckBoxMenuItemSnapshotImageSize.isSelected();
        if (snapshotsEnabled) {
            String newSnapshotImageSize = JOptionPane.showInputDialog(this, "New Snapshot Image Size",
                    String.format("%d x %d ", getSnapShotWidth(), getSnapShotHeight()));
            if (newSnapshotImageSize != null && newSnapshotImageSize.length() > 0) {
                String sa[] = newSnapshotImageSize.split("[ \tx,]+");
                if (sa.length == 2) {
                    int newSnapShotWidth = Integer.parseInt(sa[0]);
                    int newSapShotHeight = Integer.parseInt(sa[1]);
                    setImageSizeMenuText(newSnapShotWidth, newSapShotHeight);
                }
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemSnapshotImageSizeActionPerformed

    private void startLookForParts() {
        if (null != aprsSystem) {
            aprsSystem.startLookForParts();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }
    private void jMenuItemLookForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLookForActionPerformed
        startLookForParts();
    }//GEN-LAST:event_jMenuItemLookForActionPerformed

    @SuppressWarnings("unused")
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        windowClosing();
    }//GEN-LAST:event_formWindowClosing

    @SuppressWarnings("unused")
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        windowClosed();
    }//GEN-LAST:event_formWindowClosed

    private void windowClosed() {
        if (isVisible()) {
            setVisible(false);
        }
        disconnectVision();
        if (null != aprsSystem) {
            aprsSystem.stopSimUpdateTimer();
        }
    }

    private void windowClosing() {
        try {
            if (null != aprsSystem) {
                aprsSystem.windowClosing();
            }
            this.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void disconnectRobot() {
        if (null != aprsSystem) {
            aprsSystem.disconnectRobot();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @SafeEffect
    private XFuture<Boolean> startCheckEnabled() {
        if (null != aprsSystem) {
            return aprsSystem.startCheckEnabled();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    private void jCheckBoxMenuItemConnectedRobotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectedRobotActionPerformed
        boolean selected = jCheckBoxMenuItemConnectedRobot.isSelected();
        if (selected && null != aprsSystem) {
            String name = aprsSystem.getRobotName();
            if (name == null || name.length() < 1) {
                String origRobotName = aprsSystem.getOrigRobotName();
                name = JOptionPane.showInputDialog("Robot name?", origRobotName);
            }
            String host = aprsSystem.getRobotCrclHost();
            if (host == null || host.length() < 1) {
                String origCrclRobotHost = aprsSystem.getOrigCrclRobotHost();
                host = JOptionPane.showInputDialog("Robot host?", origCrclRobotHost);
            }
            int port = aprsSystem.getRobotCrclPort();
            if (port < 1) {
                int origCrclRobotPort = aprsSystem.getOrigCrclRobotPort();
                String portString = JOptionPane.showInputDialog("Robot port?", origCrclRobotPort);
                port = Integer.parseInt(portString);
            }
            clearErrors();
            resume();
            jCheckBoxMenuItemPause.setSelected(false);
            aprsSystem.connectRobot(name, host, port)
                    .thenCompose(x -> startCheckEnabled())
                    .thenApply((Boolean success) -> {
                        if (!success) {
                            clearRobotConnectedCheckBox();
                        }
                        return success;
                    })
                    .exceptionally(x -> {
                        clearRobotConnectedCheckBox();
                        return false;
                    });
        } else {
            this.disconnectRobot();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectedRobotActionPerformed

    @SafeEffect
    private void clearRobotConnectedCheckBox() {
        Utils.runOnDispatchThread(this::clearRobotConnectedCheckBoxOnDisplay);
    }

    private void clearRobotConnectedCheckBoxOnDisplay() {
        jCheckBoxMenuItemConnectedRobot.setSelected(false);
    }

    private void jMenuItemClearErrorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemClearErrorsActionPerformed
        this.clearErrors();
    }//GEN-LAST:event_jMenuItemClearErrorsActionPerformed

    private void setDebug(boolean debug) {
        if (null != aprsSystem) {
            aprsSystem.setDebug(debug);
        }
    }

    private void jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed
        boolean selected = jCheckBoxMenuItemEnableDebugDumpstacks.isSelected();
        setDebug(selected);
    }//GEN-LAST:event_jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed

    private void jCheckBoxMenuItemSteppingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSteppingActionPerformed
        if (null != aprsSystem) {
            aprsSystem.setStepMode(jCheckBoxMenuItemStepping.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemSteppingActionPerformed

    private void jCheckBoxMenuItemUseTeachTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemUseTeachTableActionPerformed
        if (null != aprsSystem) {
            aprsSystem.setUseTeachTableChecked(false);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemUseTeachTableActionPerformed

    CachedCheckBox connectDatabaseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemConnectDatabase);
    }

    public void setImageSizeMenuText(int snapShotWidth, int snapShotHeight) {
        jCheckBoxMenuItemSnapshotImageSize.setText(String.format("Snapshot Image size (%d x %d )", snapShotWidth, snapShotHeight));
    }

    CachedCheckBox pauseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemPause);
    }

    CachedCheckBox reverseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemReverse);
    }

    CachedCheckBox reloadSimFilesOnReverseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemReloadSimFilesOnReverse);
    }

    CachedCheckBox forceFakeTakeCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemForceFakeTake);
    }

    private void logEvent(String string, Object o) {
        if (null != aprsSystem) {
            aprsSystem.logEvent(string, o);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    /**
     * Get the state of the reverse flag. It is set to indicate that an
     * alternative set of actions that empty rather than fill the kit trays is
     * in use.
     *
     * @return reverse flag
     */
    public boolean isReverseFlag() {
        return jCheckBoxMenuItemReverse.isSelected();
    }

    /**
     * Get the state of whether the system is paused
     *
     * @return paused state
     */
    public boolean isPaused() {
        return jCheckBoxMenuItemPause.isSelected();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectDatabase;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectDatabaseOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectVision;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectVisionOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectedRobot;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemEnableDebugDumpstacks;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemExploreGraphDbStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemForceFakeTake;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemKitInspectionStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemLogCrclPrograms;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReloadSimFilesOnReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowDatabaseSetupOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSnapshotImageSize;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupExecutor;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupFanucCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupMotomanCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObject2DView;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObjectSP;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLPlanner;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobotCrclGUI;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobtCRCLSimServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStepping;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemUseTeachTable;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuExecute;
    private javax.swing.JMenuItem jMenuItemClearErrors;
    private javax.swing.JMenuItem jMenuItemContinueActionList;
    private javax.swing.JMenuItem jMenuItemCreateActionListFromVision;
    private javax.swing.JMenuItem jMenuItemDebugAction;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemImmediateAbort;
    private javax.swing.JMenuItem jMenuItemLoadProperties;
    private javax.swing.JMenuItem jMenuItemLoadPropertiesFile;
    private javax.swing.JMenuItem jMenuItemLookFor;
    private javax.swing.JMenuItem jMenuItemReset;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSavePropsAs;
    private javax.swing.JMenuItem jMenuItemSetPoseMaxLimits;
    private javax.swing.JMenuItem jMenuItemSetPoseMinLimits;
    private javax.swing.JMenuItem jMenuItemStartActionList;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    // End of variables declaration//GEN-END:variables

}