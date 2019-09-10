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
import static aprs.misc.AprsCommonLogger.println;
import aprs.misc.CsvTableJPanel;
import aprs.misc.IconImages;
import aprs.misc.PmCartesianMinMaxLimit;
import aprs.misc.Utils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import crcl.ui.misc.MultiLineStringJPanel;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
class AprsSystemDisplayJFrame extends javax.swing.JFrame {

    private volatile @MonotonicNonNull
    AprsSystem aprsSystem = null;

    @SafeEffect
    public void setAprsSystem(AprsSystem aprsSystem) {
        this.aprsSystem = aprsSystem;
    }

    @UIEffect
    public boolean getPauseInsteadOfRecoverMenuCheckbox() {
        return jCheckBoxMenuItemPauseInsteadOfRecover.isSelected();
    }

    @UIEffect
    public void setPauseInsteadOfRecoverMenuCheckbox(boolean selected) {
        jCheckBoxMenuItemPauseInsteadOfRecover.setSelected(selected);
    }

    private volatile boolean showingException = false;

    @SafeEffect
    public XFutureVoid showException(Throwable ex) {
        if (!showingException) {
            showingException = true;
            return Utils.composeToVoidOnDispatchThread(() -> showExceptionInternal(ex));
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    private final AtomicInteger exceptionCount = new AtomicInteger();

    @UIEffect
    private XFutureVoid showExceptionInternal(Throwable ex) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw, true)) {
            ex.printStackTrace(pw);
        } catch (Exception ex1) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex1);
        }
        String exText = sw.toString();
        boolean forceShow = exceptionCount.incrementAndGet() < 2;
        String dialogTitle = "Exception from " + this.getTitle();
        XFuture<Boolean> showTextFuture
                = MultiLineStringJPanel.showText(exText, this, dialogTitle, false, forceShow);
        return showTextFuture
                .thenRun(() -> showingException = false);
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
        checkFrames();
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
        checkFrames();
    }

    /**
     * Creates new AprsSystemInterface using a default properties file.
     */
    @SuppressWarnings({"initialization", "nullness"})
    @UIEffect
    public AprsSystemDisplayJFrame() {
        try {
            initComponents();
            checkFrames();
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private volatile int prevFrameCount = 0;
    private volatile int prevFramesLength = 0;
    private static final boolean DO_CHECK_JINTERNALFRAMES
            = Boolean.getBoolean("aprs.check.jinternalframes");

    void checkFrames() {
        if (DO_CHECK_JINTERNALFRAMES) {
            JInternalFrame frames[] = jDesktopPane1.getAllFrames();
            int frameCount = frames.length + iconifiedFramesMap.size();
            if (frameCount < prevFrameCount && frames.length < prevFramesLength) {
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.err.flush();
                System.out.println("iconifiedFramesMap.size() = " + iconifiedFramesMap.size());
                System.out.println("frames.length = " + frames.length);
                System.out.println("prevFramesLength = " + prevFramesLength);
                System.out.println("frames = " + Arrays.toString(frames));
                System.out.println("prevFrameCount = " + prevFrameCount);
                System.out.println("frameCount = " + frameCount);
                Thread.dumpStack();
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.err.flush();
            }
            prevFrameCount = frameCount;
            prevFramesLength = frames.length;
        }
    }

    CachedCheckBox kitInspectionStartupCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemKitInspectionStartup);
    }

//    CachedCheckBox pddlPlannerStartupCheckBox() {
//        return new CachedCheckBox(jCheckBoxMenuItemStartupPDDLPlanner);
//    }
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
            setIconImage(IconImages.BASE_IMAGE);
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
        setupWindowsMenu();
    }

    @UIEffect
    void closeAllInternalFrames() {
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        checkFrames();
        for (JInternalFrame f : frames) {
            jDesktopPane1.getDesktopManager().closeFrame(f);
            f.setVisible(false);
            jDesktopPane1.remove(f);
        }
        prevFrameCount = jDesktopPane1.getAllFrames().length;
    }

    private class LocationSize {

        final Point location;
        final Dimension size;

        public LocationSize(Point location, Dimension size) {
            this.location = location;
            this.size = size;
        }

        @Override
        public String toString() {
            return "LocationSize{" + "location=" + location + ", size=" + size + '}';
        }

    }
    private final Map<JInternalFrame, LocationSize> iconifiedFramesMap = new IdentityHashMap<>();

    @UIEffect
    void hideAllInternalFrames() {
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        checkFrames();
        final DesktopManager desktopManager = jDesktopPane1.getDesktopManager();
        for (JInternalFrame f : frames) {
            iconifyInternalFrame(desktopManager, f);
//            f.setVisible(false);
        }
        checkFrames();
    }

    @UIEffect
    void showAllInternalFrames() {
        checkFrames();
        final DesktopManager desktopManager = jDesktopPane1.getDesktopManager();
        final Set<JInternalFrame> keySet = iconifiedFramesMap.keySet();
        List<JInternalFrame> keySetList = new ArrayList<>(keySet);
        for (int i = 0; i < keySetList.size(); i++) {
            JInternalFrame internalFrame = keySetList.get(i);
            if (null != internalFrame) {
                deiconifyInternalFrame(internalFrame, desktopManager);
            }
        }
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        checkFrames();
        for (JInternalFrame f : frames) {
            deiconifyInternalFrame(f, desktopManager);
            f.setVisible(true);
        }
        checkFrames();
    }

    @UIEffect
    void checkDeiconifyActivateAndMaximize(JInternalFrame internalFrame) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Thread.currentThread()=" + Thread.currentThread());
        }
        checkFrames();
        internalFrame.setVisible(true);
        if (checkInternalFrame(internalFrame)) {
            checkFrames();
            DesktopManager desktopManager = jDesktopPane1.getDesktopManager();
            Point p = internalFrame.getLocation();
            System.out.println("p = " + p);
            internalFrame.setLocation(0, 0);
            JInternalFrame.JDesktopIcon desktopIcon = internalFrame.getDesktopIcon();
            System.out.println("desktopIcon = " + desktopIcon);
            Container c = desktopIcon.getParent();
            System.out.println("c = " + c);
            deiconifyInternalFrame(internalFrame, desktopManager);
            checkFrames();
            try {
                desktopManager.activateFrame(internalFrame);
            } catch (Exception e) {
                e.printStackTrace();
            }
            checkFrames();
            try {
                desktopManager.maximizeFrame(internalFrame);
            } catch (Exception e) {
                e.printStackTrace();
            }
            checkFrames();
        }
    }

    @UIEffect
    boolean checkInternalFrame(JInternalFrame frm) {
        try {
            if (iconifiedFramesMap.keySet().contains(frm)) {
                return true;
            }
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
            checkFrames();
            iconifyInternalFrame(desktopManager, internalFrame);
            checkFrames();
            desktopManager.deactivateFrame(internalFrame);
            checkFrames();
        }
    }

    private void iconifyInternalFrame(DesktopManager desktopManager, JInternalFrame internalFrame) {
        LocationSize ls = new LocationSize(internalFrame.getLocation(), internalFrame.getSize());
        try {
            desktopManager.iconifyFrame(internalFrame);
        } catch (Exception e) {
            System.err.println("internalFrame=" + internalFrame);
            System.err.println("ls=" + ls);
            e.printStackTrace();
            try {
                desktopManager.minimizeFrame(internalFrame);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        iconifiedFramesMap.put(internalFrame, ls);
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
        checkFrames();
        deiconifyInternalFrame(frameToShow, desktopManager);
        checkFrames();
        try {
            desktopManager.activateFrame(frameToShow);
        } catch (Exception e) {
            System.out.println("frameToShow = " + frameToShow);
            System.out.println("desktopManager = " + desktopManager);
            e.printStackTrace();
        }
        checkFrames();
    }

    private void deiconifyInternalFrame(final JInternalFrame frameToShow, DesktopManager desktopManager) {
        try {
            LocationSize ls = iconifiedFramesMap.get(frameToShow);
            if (null != ls) {
                try {
                    desktopManager.deiconifyFrame(frameToShow);
                } catch (Exception e) {
                    System.out.println("frameToShow = " + frameToShow);
                    System.out.println("desktopManager = " + desktopManager);
                    System.out.println("ls = " + ls);
                    e.printStackTrace();
                }
                if (!Arrays.asList(jDesktopPane1.getAllFrames()).contains(frameToShow)) {
                    jDesktopPane1.add(frameToShow);
                }
                desktopManager.resizeFrame(frameToShow, ls.location.x, ls.location.y, ls.size.width, ls.size.height);
                iconifiedFramesMap.remove(frameToShow);
            }
        } catch (Throwable e) {
            System.out.println("frameToShow = " + frameToShow);
            System.out.println("desktopManager = " + desktopManager);
            e.printStackTrace();
        }
    }

    private String lastFrameTitles[] = new String[0];

    private final AtomicInteger setupWindowMenuCount = new AtomicInteger();

    @UIEffect
    void setupWindowsMenu() {

        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Thread.currentThread()=" + Thread.currentThread());
        }
        if (jMenuWindow.isSelected()) {
            throw new RuntimeException("jMenuWindow.isSelected()");
        }
        checkFrames();
        int count = 1;
        JInternalFrame frames[] = jDesktopPane1.getAllFrames();
        ArrayList<JInternalFrame> framesList = new ArrayList<>(Arrays.asList(frames));
        for (JInternalFrame frame : iconifiedFramesMap.keySet()) {
            if (!framesList.contains(frame)) {
                framesList.add(frame);
            }
        }
        framesList.sort(Comparator.comparing(JInternalFrame::getTitle));
//        Thread.dumpStack();
//        int swmCount = setupWindowMenuCount.incrementAndGet();
//        System.out.println("aprsSystem = " + aprsSystem);
//        System.out.println("swmCount = " + swmCount);
//        System.out.println("framesList = " + framesList);
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
//        System.out.println("framesChanged = " + framesChanged);
        int menuItemCount = jMenuWindow.getItemCount();
//        System.out.println("menuItemCount = " + menuItemCount);
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

        menuItemCount = jMenuWindow.getItemCount();
//        System.out.println("menuItemCount = " + menuItemCount);
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
    @SuppressWarnings({"rawtypes", "unchecked"})
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
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItemHideAllWindows = new javax.swing.JMenuItem();
        jMenuItemShowAllWindows = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
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
        jMenuOptions = new javax.swing.JMenu();
        jCheckBoxMenuItemEnableDebugDumpstacks = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetPoseMinMaxLimits = new javax.swing.JMenuItem();
        jCheckBoxMenuItemEnforceMinMaxLimits = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemAlertLimits = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemSnapshotImageSize = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemReloadSimFilesOnReverse = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemUseTeachTable = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemLogCrclPrograms = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPauseInsteadOfRecover = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStepping = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemCorrectionMode = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemAllowForceFakeTakeAnyTime = new javax.swing.JCheckBoxMenuItem();
        jMenuItemCustomWindowStartup = new javax.swing.JMenuItem();
        jMenuExecute = new javax.swing.JMenu();
        jMenuItemStartActionList = new javax.swing.JMenuItem();
        jMenuItemImmediateAbort = new javax.swing.JMenuItem();
        jMenuItemContinueActionList = new javax.swing.JMenuItem();
        jMenuItemReset = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinuousDemo = new javax.swing.JCheckBoxMenuItem();
        jMenuItemDebugAction = new javax.swing.JMenuItem();
        jMenuItemCreateActionListFromVision = new javax.swing.JMenuItem();
        jMenuItemLookFor = new javax.swing.JMenuItem();
        jMenuItemClearErrors = new javax.swing.JMenuItem();
        jMenuItemFillKitTrays = new javax.swing.JMenuItem();
        jMenuItemShowFilledKitTrays = new javax.swing.JMenuItem();
        jMenuItemEmptyKitTrays = new javax.swing.JMenuItem();
        jMenuItemShowEmptiedKitTrays = new javax.swing.JMenuItem();
        jMenuItemRestoreOriginalRobotConnection = new javax.swing.JMenuItem();
        jMenuPause = new javax.swing.JMenu();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jMenuTests = new javax.swing.JMenu();
        jCheckBoxMenuItemForceFakeTake = new javax.swing.JCheckBoxMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
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

        jMenuItem1.setText("FilterTest");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItemHideAllWindows.setText("Hide All Windows");
        jMenuItemHideAllWindows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHideAllWindowsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemHideAllWindows);

        jMenuItemShowAllWindows.setText("Show All Windows");
        jMenuItemShowAllWindows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowAllWindowsActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemShowAllWindows);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Startup");

        jCheckBoxMenuItemStartupExecutor.setText("Executor");
        jCheckBoxMenuItemStartupExecutor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupExecutorActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupExecutor);

        jCheckBoxMenuItemStartupObjectSP.setText("Object SP");
        jCheckBoxMenuItemStartupObjectSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObjectSPActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObjectSP);

        jCheckBoxMenuItemStartupObject2DView.setText("Object 2D View/Simulate");
        jCheckBoxMenuItemStartupObject2DView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObject2DViewActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObject2DView);

        jCheckBoxMenuItemStartupRobotCrclGUI.setText("Robot CRCL Client Gui");
        jCheckBoxMenuItemStartupRobotCrclGUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobotCrclGUI);

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

        jMenuOptions.setText("Options");

        jCheckBoxMenuItemEnableDebugDumpstacks.setText("Enable Debug DumpStacks");
        jCheckBoxMenuItemEnableDebugDumpstacks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemEnableDebugDumpstacks);

        jMenuItemSetPoseMinMaxLimits.setText("Set Pose Min/Max Limits ");
        jMenuItemSetPoseMinMaxLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetPoseMinMaxLimitsActionPerformed(evt);
            }
        });
        jMenuOptions.add(jMenuItemSetPoseMinMaxLimits);

        jCheckBoxMenuItemEnforceMinMaxLimits.setSelected(true);
        jCheckBoxMenuItemEnforceMinMaxLimits.setText("Enforce Min/Max Limits");
        jCheckBoxMenuItemEnforceMinMaxLimits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemEnforceMinMaxLimitsActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemEnforceMinMaxLimits);

        jCheckBoxMenuItemAlertLimits.setSelected(true);
        jCheckBoxMenuItemAlertLimits.setText("Alert Limits");
        jMenuOptions.add(jCheckBoxMenuItemAlertLimits);

        jCheckBoxMenuItemSnapshotImageSize.setText("Snapshot Image size (800 x 600 )");
        jCheckBoxMenuItemSnapshotImageSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSnapshotImageSizeActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemSnapshotImageSize);

        jCheckBoxMenuItemReloadSimFilesOnReverse.setText("Reload Sim Files on Reverse");
        jMenuOptions.add(jCheckBoxMenuItemReloadSimFilesOnReverse);

        jCheckBoxMenuItemUseTeachTable.setText("Use Teach Table");
        jCheckBoxMenuItemUseTeachTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemUseTeachTableActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemUseTeachTable);

        jCheckBoxMenuItemLogCrclPrograms.setSelected(true);
        jCheckBoxMenuItemLogCrclPrograms.setText("Log CRCL Programs");
        jMenuOptions.add(jCheckBoxMenuItemLogCrclPrograms);

        jCheckBoxMenuItemPauseInsteadOfRecover.setText("Executor Check Kits : Pause instead of Recover");
        jCheckBoxMenuItemPauseInsteadOfRecover.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseInsteadOfRecoverActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemPauseInsteadOfRecover);

        jCheckBoxMenuItemStepping.setText("Single Stepping");
        jCheckBoxMenuItemStepping.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSteppingActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemStepping);

        jCheckBoxMenuItemCorrectionMode.setSelected(true);
        jCheckBoxMenuItemCorrectionMode.setText("Correction Mode");
        jCheckBoxMenuItemCorrectionMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemCorrectionModeActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemCorrectionMode);

        jCheckBoxMenuItemAllowForceFakeTakeAnyTime.setText("Allow Force Fake Take Any  Time");
        jCheckBoxMenuItemAllowForceFakeTakeAnyTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemAllowForceFakeTakeAnyTimeActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemAllowForceFakeTakeAnyTime);

        jMenuItemCustomWindowStartup.setText("Custom Window Startup ...");
        jMenuItemCustomWindowStartup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCustomWindowStartupActionPerformed(evt);
            }
        });
        jMenuOptions.add(jMenuItemCustomWindowStartup);

        jMenuBar1.add(jMenuOptions);

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

        jMenuItemDebugAction.setText("Debug Action");
        jMenuItemDebugAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDebugActionActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemDebugAction);

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

        jMenuItemFillKitTrays.setText("Fill Kit Trays");
        jMenuItemFillKitTrays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFillKitTraysActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemFillKitTrays);

        jMenuItemShowFilledKitTrays.setText("Show Filled Kit Trays");
        jMenuItemShowFilledKitTrays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowFilledKitTraysActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemShowFilledKitTrays);

        jMenuItemEmptyKitTrays.setText("Empty Kit Trays");
        jMenuItemEmptyKitTrays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEmptyKitTraysActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemEmptyKitTrays);

        jMenuItemShowEmptiedKitTrays.setText("Show Emptied Kit Trays");
        jMenuItemShowEmptiedKitTrays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowEmptiedKitTraysActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemShowEmptiedKitTrays);

        jMenuItemRestoreOriginalRobotConnection.setText("Restore Original Robot Connection");
        jMenuItemRestoreOriginalRobotConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRestoreOriginalRobotConnectionActionPerformed(evt);
            }
        });
        jMenuExecute.add(jMenuItemRestoreOriginalRobotConnection);

        jMenuBar1.add(jMenuExecute);

        jMenuPause.setText("Pause");

        jCheckBoxMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAUSE, 0));
        jCheckBoxMenuItemPause.setText("Pause");
        jCheckBoxMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseActionPerformed(evt);
            }
        });
        jMenuPause.add(jCheckBoxMenuItemPause);

        jMenuBar1.add(jMenuPause);

        jMenuTests.setText("Tests");

        jCheckBoxMenuItemForceFakeTake.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jCheckBoxMenuItemForceFakeTake.setText("Force Fake Take");
        jCheckBoxMenuItemForceFakeTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemForceFakeTakeActionPerformed(evt);
            }
        });
        jMenuTests.add(jCheckBoxMenuItemForceFakeTake);

        jMenuBar1.add(jMenuTests);

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

    @UIEffect
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

    @UIEffect
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

    private XFutureVoid startObject2DJinternalFrame() {
        if (null != aprsSystem) {
            return aprsSystem.startObject2DJinternalFrame();
        }
        return XFutureVoid.completedFuture();
    }

    @UIEffect
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

    @UIEffect
    private void jMenuItemLoadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesActionPerformed
        try {
            this.loadProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemLoadPropertiesActionPerformed

    @UIEffect
    private void jMenuItemSavePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropertiesActionPerformed
        try {
            this.saveProperties();

        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemSavePropertiesActionPerformed

    @UIEffect
    private void jMenuItemLoadPropertiesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesFileActionPerformed
        browseOpenPropertiesFile();
    }//GEN-LAST:event_jMenuItemLoadPropertiesFileActionPerformed

    private @Nullable
    File getPropertiesDirectory() {
        if (null != aprsSystem) {
            return aprsSystem.getPropertiesDirectory();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @UIEffect
    @Nullable
    File choosePropertiesFileToOpen() {
        File dir = getPropertiesDirectory();
        JFileChooser chooser;
        if (null != dir) {
            chooser = new JFileChooser(dir);
        } else {
            chooser = new JFileChooser();
        }
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
    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
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

    @UIEffect
    private void jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed
        if (jCheckBoxMenuItemStartupMotomanCRCLServer.isSelected()) {
            startMotomanCrclServer();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupMotomanCRCLServerActionPerformed

    @UIEffect
    private void jMenuItemSavePropsAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropsAsActionPerformed
        browseSavePropertiesFileAs();
    }//GEN-LAST:event_jMenuItemSavePropsAsActionPerformed

    @Nullable
    @UIEffect
    private File choosePropertiesFileToSaveAs() {
        File dir = getPropertiesDirectory();
        JFileChooser chooser;
        if (null != dir) {
            chooser = new JFileChooser(dir);
        } else {
            chooser = new JFileChooser();
        }
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
    @UIEffect
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

    @UIEffect
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

    private XFuture<Boolean> startActions(String label, boolean reverseFlag) {
        if (null != aprsSystem) {
            return aprsSystem.startActions(label, reverseFlag);
        } else {
            throw new IllegalStateException("this=" + this + ", aprsSystem==null");
        }
    }

    @UIEffect
    @SuppressWarnings("guieffect")
    private void jMenuItemStartActionListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartActionListActionPerformed
        setTitleErrorString(null);
        jCheckBoxMenuItemPause.setSelected(false);
        notifyPauseFutures();
        boolean connectedRobotSelected = jCheckBoxMenuItemConnectedRobot.isSelected();
        if (null == aprsSystem) {
            return;
        }
        if (!connectedRobotSelected || !aprsSystem.isConnected()) {
            jCheckBoxMenuItemConnectedRobot.setSelected(true);
            queryConnect()
                    .thenCompose(x -> this.startActions("user", jCheckBoxMenuItemReverse.isSelected()));
        } else {
            this.startActions("user", jCheckBoxMenuItemReverse.isSelected());
            aprsSystem.fillKitTrays(true);
        }
    }//GEN-LAST:event_jMenuItemStartActionListActionPerformed

    private void connectVision() {
        if (null != aprsSystem) {
            aprsSystem.connectVision();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        Utils.runOnDispatchThread(() -> super.setVisible(visible));
    }

    private void disconnectVision() {
        if (null != aprsSystem) {
            aprsSystem.disconnectVision();
        }
    }

    @UIEffect
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

    @UIEffect
    private void jCheckBoxMenuItemKitInspectionStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemKitInspectionStartupActionPerformed
        this.startKitInspection();
    }//GEN-LAST:event_jCheckBoxMenuItemKitInspectionStartupActionPerformed

    @UIEffect
    private void jMenuItemResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetActionPerformed
        reset();
    }//GEN-LAST:event_jMenuItemResetActionPerformed

    private volatile boolean reverseFlag;

    @UIEffect
    private void jCheckBoxMenuItemReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemReverseActionPerformed
        reverseFlag = jCheckBoxMenuItemReverse.isSelected();
        reloadForReverse(reverseFlag);
    }//GEN-LAST:event_jCheckBoxMenuItemReverseActionPerformed

    @SuppressWarnings("guieffect")
    private void reloadForReverse(boolean reverseFlag) {
        updateForceFakeTakeState(reverseFlag);
        if (null != aprsSystem) {
            aprsSystem.reloadForReverse(reverseFlag);
        }
    }

    @UIEffect
    public void updateForceFakeTakeState(boolean reverseFlag1) {
        boolean forceFakeTakeOk = !reverseFlag1 && null != aprsSystem
                && (aprsSystem.isCorrectionMode() || !aprsSystem.isPauseInsteadOfRecover());
        if (jCheckBoxMenuItemAllowForceFakeTakeAnyTime.isSelected()) {
            forceFakeTakeOk = true;
        }
        if (!forceFakeTakeOk) {
            setForceFakeTakeSelected(false);
        }
        setForceFakeTakeEnabled(forceFakeTakeOk);
    }

    @UIEffect
    public void setForceFakeTakeSelected(boolean selected) {
        jCheckBoxMenuItemForceFakeTake.setSelected(selected);
    }

    @UIEffect
    public void setForceFakeTakeEnabled(boolean enabled) {
        jCheckBoxMenuItemForceFakeTake.setEnabled(enabled);
    }

    private XFuture<Boolean> startContinuousDemo(String label, boolean reverseFlag) {
        if (null != aprsSystem) {
            return aprsSystem.startContinuousDemo(label, reverseFlag);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @UIEffect
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

    private @Nullable
    XFuture<Boolean> getContinuousDemoFuture() {
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

    @UIEffect
    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        if (null == aprsSystem) {
            throw new IllegalStateException("null == aprsSystem");
        }
        println("jCheckBoxMenuItemPause.isSelected() = " + jCheckBoxMenuItemPause.isSelected());
        XFuture<Boolean> cdf = getContinuousDemoFuture();
        if (null != cdf) {
            println("ContinuousDemoFuture.isDone() = " + cdf.isDone());
            println("ContinuousDemoFuture.isCancelled() = " + cdf.isCancelled());
        }
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            clearErrors();
            resume();
        }
        cdf = getContinuousDemoFuture();
        if (null != cdf) {
            println("ContinuousDemoFuture.isDone() = " + cdf.isDone());
            println("ContinuousDemoFuture.isCancelled() = " + cdf.isCancelled());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseActionPerformed

    private void debugAction() {
        if (null != aprsSystem) {
            aprsSystem.debugAction();
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @UIEffect
    private void jMenuItemDebugActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDebugActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDebugActionActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemForceFakeTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemForceFakeTakeActionPerformed
        if (aprsSystem != null) {
            boolean val = jCheckBoxMenuItemForceFakeTake.isSelected();
            if (aprsSystem.getExcutorForceFakeTakeFlag() != val) {
                aprsSystem.setExecutorForceFakeTakeFlag(val);
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemForceFakeTakeActionPerformed

    private void continueActionList(String label) {
        if (null != aprsSystem) {
            aprsSystem.continueActionList(label);
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @UIEffect
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

    @UIEffect
    private void jMenuItemCreateActionListFromVisionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCreateActionListFromVisionActionPerformed
        createActionListFromVision();
    }//GEN-LAST:event_jMenuItemCreateActionListFromVisionActionPerformed

//    private void setMinLimit(PmCartesian cart) {
//        if (null != aprsSystem) {
//            aprsSystem.setMinLimit(cart);
//        } else {
//            throw new IllegalStateException("aprsSystem == null, this=" + this);
//        }
//    }
//    private PmCartesian getMinLimit() {
//        if (null != aprsSystem) {
//            return aprsSystem.getMinLimit();
//        } else {
//            throw new IllegalStateException("aprsSystem == null, this=" + this);
//        }
//    }
//    private void setMaxLimit(PmCartesian cart) {
//        if (null != aprsSystem) {
//            aprsSystem.setMaxLimit(cart);
//        } else {
//            throw new IllegalStateException("aprsSystem == null, this=" + this);
//        }
//    }
    @UIEffect
    public void setMaxLimitMenuDisplay(PmCartesian cart) {
        String txt
                = String.format(
                        "Set Pose Max Limits ... (%+.0f,%+.0f,%+.0f)    ...",
                        cart.x, cart.y, cart.z);
        jMenuItemSetPoseMinMaxLimits.setText(txt);
    }

//    @UIEffect
//    public void setMinLimitMenuDisplay(PmCartesian cart) {
//        String txt
//                = String.format(
//                        "Set Pose Min Limits ... (%+.0f,%+.0f,%+.0f)    ...",
//                        cart.x, cart.y, cart.z);
//        jMenuItemSetPoseMinLimitsLimits.setText(txt);
//    }
//    private PmCartesian getMaxLimit() {
//        if (null != aprsSystem) {
//            return aprsSystem.getMaxLimit();
//        } else {
//            throw new IllegalStateException("aprsSystem == null, this=" + this);
//        }
//    }
    @UIEffect
    private void jMenuItemSetPoseMinMaxLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetPoseMinMaxLimitsActionPerformed
        try {
            if (null == aprsSystem) {
                throw new NullPointerException("aprsSystem");
            }
            File csvFile = aprsSystem.getCartLimitsCsvFile();
            if (csvFile.exists()) {
                aprsSystem.readLimitsFromCsv(csvFile);
            }
            Object dataIn[][] = new Object[aprsSystem.getLimits().size()][];
            for (int i = 0; i < aprsSystem.getLimits().size(); i++) {
                dataIn[i] = aprsSystem.getLimits().get(i).toObjArray();
            }
            Object dataOut[][] = CsvTableJPanel.editTable(this, csvFile.getCanonicalPath(), true, PmCartesianMinMaxLimit.getHeaders(), dataIn);
            if (null != dataOut) {
                aprsSystem.getLimits().clear();
                for (int i = 0; i < dataOut.length; i++) {
                    aprsSystem.getLimits().add(new PmCartesianMinMaxLimit(dataOut[i]));
                }
                aprsSystem.writeLimitsFromCsv(csvFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSetPoseMinMaxLimitsActionPerformed

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

    @UIEffect
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

    @SuppressWarnings("guieffect")
    private void startLookForParts() {
        final AprsSystem aprsSystemFinal = aprsSystem;
        if (null != aprsSystemFinal) {
            boolean connectedRobotSelected = jCheckBoxMenuItemConnectedRobot.isSelected();
            if (!connectedRobotSelected || !aprsSystemFinal.isConnected()) {
                jCheckBoxMenuItemConnectedRobot.setSelected(true);
                queryConnect()
                        .thenCompose(x -> aprsSystemFinal.startLookForParts());
            } else {
                aprsSystemFinal.startLookForParts();
            }
        } else {
            throw new IllegalStateException("aprsSystem ==null, this=" + this);
        }
    }

    @UIEffect
    private void jMenuItemLookForActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLookForActionPerformed
        immediateAbort();
        clearErrors();
        reset();
        resume();
        startLookForParts();
    }//GEN-LAST:event_jMenuItemLookForActionPerformed

    @UIEffect
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        windowClosing();
    }//GEN-LAST:event_formWindowClosing

    @UIEffect
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        windowClosed();
    }//GEN-LAST:event_formWindowClosed

    @UIEffect
    private void windowClosed() {
        if (null != aprsSystem) {
            aprsSystem.setCrclClientPreClosing(true);
        }
        if (isVisible()) {
            setVisible(false);
        }
        disconnectVision();
        if (null != aprsSystem) {
            aprsSystem.stopSimUpdateTimer();
        }
    }

    @UIEffect
    private void windowClosing() {
        try {
            if (null != aprsSystem) {
                aprsSystem.setCrclClientPreClosing(true);
            }
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

    @UIEffect
    private void jCheckBoxMenuItemConnectedRobotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemConnectedRobotActionPerformed
        boolean selected = jCheckBoxMenuItemConnectedRobot.isSelected();
        if (selected && null != aprsSystem) {
            queryConnect();
        } else {
            this.disconnectRobot();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemConnectedRobotActionPerformed

    @SuppressWarnings("guieffect")
    public XFuture<Boolean> queryConnect() {
        final AprsSystem aprsSystemFinal = aprsSystem;
        if (null == aprsSystemFinal) {
            throw new NullPointerException("aprsSystem");
        }
        String name = aprsSystemFinal.getRobotName();
        if (name == null || name.length() < 1) {
            String origRobotName = aprsSystemFinal.getOrigRobotName();
            name = JOptionPane.showInputDialog("Robot name?", origRobotName);
        }
        String host = aprsSystemFinal.getRobotCrclHost();
        if (host == null || host.length() < 1) {
            String origCrclRobotHost = aprsSystemFinal.getOrigCrclRobotHost();
            host = JOptionPane.showInputDialog("Robot host?", origCrclRobotHost);
        }
        int port = aprsSystemFinal.getRobotCrclPort();
        if (port < 1) {
            int origCrclRobotPort = aprsSystemFinal.getOrigCrclRobotPort();
            String portString = JOptionPane.showInputDialog("Robot port?", origCrclRobotPort);
            port = Integer.parseInt(portString);
        }
        clearErrors();
        resume();
        jCheckBoxMenuItemPause.setSelected(false);
        return aprsSystemFinal.connectRobot(name, host, port)
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
    }

    @SafeEffect
    private void clearRobotConnectedCheckBox() {
        Utils.runOnDispatchThread(this::clearRobotConnectedCheckBoxOnDisplay);
    }

    @UIEffect
    private void clearRobotConnectedCheckBoxOnDisplay() {
        jCheckBoxMenuItemConnectedRobot.setSelected(false);
    }

    @UIEffect
    private void jMenuItemClearErrorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemClearErrorsActionPerformed
        this.clearErrors();
    }//GEN-LAST:event_jMenuItemClearErrorsActionPerformed

    private void setDebug(boolean debug) {
        if (null != aprsSystem) {
            aprsSystem.setDebug(debug);
        }
    }

    @UIEffect
    private void jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed
        boolean selected = jCheckBoxMenuItemEnableDebugDumpstacks.isSelected();
        setDebug(selected);
    }//GEN-LAST:event_jCheckBoxMenuItemEnableDebugDumpstacksActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemSteppingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSteppingActionPerformed
        if (null != aprsSystem) {
            aprsSystem.setStepMode(jCheckBoxMenuItemStepping.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemSteppingActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemUseTeachTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemUseTeachTableActionPerformed
        if (null != aprsSystem) {
            aprsSystem.setUseTeachTableChecked(false);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemUseTeachTableActionPerformed

    void setCheckBoxMenuItemCorrectionModeSelected(boolean selected) {
        Utils.runOnDispatchThread(() -> {
            jCheckBoxMenuItemCorrectionMode.setSelected(selected);
        });
    }

    @UIEffect
    private void jMenuItemFillKitTraysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFillKitTraysActionPerformed
        final AprsSystem aprsSystemFinal = aprsSystem;
        if (null == aprsSystemFinal) {
            System.err.println("aprsSystem==null");
            return;
        }
        try {
            jCheckBoxMenuItemPause.setSelected(false);
            jCheckBoxMenuItemCorrectionMode.setSelected(true);
            clearErrors();
            resume();
            jCheckBoxMenuItemReverse.setSelected(false);
            reverseFlag = false;
            reloadForReverse(false);
            boolean connectedRobotSelected = jCheckBoxMenuItemConnectedRobot.isSelected();
            if (!connectedRobotSelected || !aprsSystemFinal.isConnected()) {
                jCheckBoxMenuItemConnectedRobot.setSelected(true);
                queryConnect()
                        .thenCompose(x -> aprsSystemFinal.fillKitTrays(true));
            } else {
                aprsSystemFinal.fillKitTrays(true);
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemFillKitTraysActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemPauseInsteadOfRecoverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseInsteadOfRecoverActionPerformed
        if (null != aprsSystem) {
            aprsSystem.setPauseInsteadOfRecover(jCheckBoxMenuItemPauseInsteadOfRecover.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseInsteadOfRecoverActionPerformed

    private void jMenuItemShowFilledKitTraysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowFilledKitTraysActionPerformed
        try {
            if (null != aprsSystem) {
                aprsSystem.showFilledKitTrays(true);
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemShowFilledKitTraysActionPerformed

    @UIEffect
    private void jMenuItemEmptyKitTraysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemEmptyKitTraysActionPerformed
        try {
            final AprsSystem aprsSystemFinal = aprsSystem;
            if (null == aprsSystemFinal) {
                return;
            }
            jCheckBoxMenuItemPause.setSelected(false);
            jCheckBoxMenuItemCorrectionMode.setSelected(true);
            clearErrors();
            resume();
            jCheckBoxMenuItemReverse.setSelected(true);
            reverseFlag = true;
            reloadForReverse(true);
            boolean connectedRobotSelected = jCheckBoxMenuItemConnectedRobot.isSelected();
            if (!connectedRobotSelected || !aprsSystemFinal.isConnected()) {
                jCheckBoxMenuItemConnectedRobot.setSelected(true);
                queryConnect()
                        .thenCompose(x -> aprsSystemFinal.emptyKitTrays());
            } else {
                aprsSystemFinal.emptyKitTrays();
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemEmptyKitTraysActionPerformed

    private void jMenuItemShowEmptiedKitTraysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowEmptiedKitTraysActionPerformed
        try {
            if (null != aprsSystem) {
                aprsSystem.showEmptiedKitTrays();
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemShowEmptiedKitTraysActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemCorrectionModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemCorrectionModeActionPerformed
        try {
            if (null != aprsSystem) {
                aprsSystem.setCorrectionMode(jCheckBoxMenuItemCorrectionMode.isSelected());
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemCorrectionModeActionPerformed

    private void jMenuItemRestoreOriginalRobotConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRestoreOriginalRobotConnectionActionPerformed
        try {
            if (null != aprsSystem) {
                aprsSystem.disconnectRobot();
                aprsSystem.setRobotName(aprsSystem.getOrigRobotName());
                aprsSystem.connectRobot(aprsSystem.getOrigRobotName(), aprsSystem.getOrigCrclRobotHost(), aprsSystem.getOrigCrclRobotPort());
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSystemDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemRestoreOriginalRobotConnectionActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemAllowForceFakeTakeAnyTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemAllowForceFakeTakeAnyTimeActionPerformed
        if (jCheckBoxMenuItemAllowForceFakeTakeAnyTime.isSelected()) {
            setForceFakeTakeEnabled(true);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemAllowForceFakeTakeAnyTimeActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemEnforceMinMaxLimitsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemEnforceMinMaxLimitsActionPerformed
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        aprsSystem.setEnforceMinMaxLimits(jCheckBoxMenuItemEnforceMinMaxLimits.isSelected());
        aprsSystem.updateRobotLimits();
    }//GEN-LAST:event_jCheckBoxMenuItemEnforceMinMaxLimitsActionPerformed

    @UIEffect
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        String filename = JOptionPane.showInputDialog("csv items file to filter");
        if (null != filename && null != aprsSystem) {
            aprsSystem.filterTest(filename);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItemHideAllWindowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHideAllWindowsActionPerformed
        hideAllInternalFrames();
    }//GEN-LAST:event_jMenuItemHideAllWindowsActionPerformed

    private void jMenuItemShowAllWindowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowAllWindowsActionPerformed
        showAllInternalFrames();
    }//GEN-LAST:event_jMenuItemShowAllWindowsActionPerformed

    private void jMenuItemCustomWindowStartupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCustomWindowStartupActionPerformed
        JFileChooser chooser = new JFileChooser(aprsSystem.getPropertiesDirectory());
        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            aprsSystem.setCustomWindowsFile(chooser.getSelectedFile());
        }
    }//GEN-LAST:event_jMenuItemCustomWindowStartupActionPerformed

    public XFutureVoid setEnforceMinMaxLimitsSelected(boolean selected) {
        return Utils.runOnDispatchThread(() -> {
            jCheckBoxMenuItemEnforceMinMaxLimits.setSelected(selected);
        });
    }

    CachedCheckBox connectDatabaseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemConnectDatabase);
    }

    @UIEffect
    void setImageSizeMenuText(int snapShotWidth, int snapShotHeight) {
        jCheckBoxMenuItemSnapshotImageSize.setText(String.format("Snapshot Image size (%d x %d )", snapShotWidth, snapShotHeight));
    }

    CachedCheckBox pauseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemPause);
    }

    CachedCheckBox reverseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemReverse);
    }

    CachedCheckBox alertLimitsCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemAlertLimits);
    }

    CachedCheckBox reloadSimFilesOnReverseCheckBox() {
        return new CachedCheckBox(jCheckBoxMenuItemReloadSimFilesOnReverse);
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
    @UIEffect
    boolean isReverseFlag() {
        return jCheckBoxMenuItemReverse.isSelected();
    }

    /**
     * Get the state of whether the system is paused
     *
     * @return paused state
     */
    @UIEffect
    boolean isPaused() {
        return jCheckBoxMenuItemPause.isSelected();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemAlertLimits;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemAllowForceFakeTakeAnyTime;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectDatabase;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectDatabaseOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectVision;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectVisionOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemConnectedRobot;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemCorrectionMode;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemEnableDebugDumpstacks;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemEnforceMinMaxLimits;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemExploreGraphDbStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemForceFakeTake;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemKitInspectionStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemLogCrclPrograms;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseInsteadOfRecover;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReloadSimFilesOnReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowDatabaseSetupOnStartup;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSnapshotImageSize;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupExecutor;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupFanucCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupMotomanCRCLServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObject2DView;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObjectSP;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobotCrclGUI;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobtCRCLSimServer;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStepping;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemUseTeachTable;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuExecute;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItemClearErrors;
    private javax.swing.JMenuItem jMenuItemContinueActionList;
    private javax.swing.JMenuItem jMenuItemCreateActionListFromVision;
    private javax.swing.JMenuItem jMenuItemCustomWindowStartup;
    private javax.swing.JMenuItem jMenuItemDebugAction;
    private javax.swing.JMenuItem jMenuItemEmptyKitTrays;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemFillKitTrays;
    private javax.swing.JMenuItem jMenuItemHideAllWindows;
    private javax.swing.JMenuItem jMenuItemImmediateAbort;
    private javax.swing.JMenuItem jMenuItemLoadProperties;
    private javax.swing.JMenuItem jMenuItemLoadPropertiesFile;
    private javax.swing.JMenuItem jMenuItemLookFor;
    private javax.swing.JMenuItem jMenuItemReset;
    private javax.swing.JMenuItem jMenuItemRestoreOriginalRobotConnection;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSavePropsAs;
    private javax.swing.JMenuItem jMenuItemSetPoseMinMaxLimits;
    private javax.swing.JMenuItem jMenuItemShowAllWindows;
    private javax.swing.JMenuItem jMenuItemShowEmptiedKitTrays;
    private javax.swing.JMenuItem jMenuItemShowFilledKitTrays;
    private javax.swing.JMenuItem jMenuItemStartActionList;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JMenu jMenuPause;
    private javax.swing.JMenu jMenuTests;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    // End of variables declaration//GEN-END:variables

}
