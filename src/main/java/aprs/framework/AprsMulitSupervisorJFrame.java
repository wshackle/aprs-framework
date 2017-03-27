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

import aprs.framework.colortextdisplay.ColorTextOptionsJPanel;
import aprs.framework.colortextdisplay.ColorTextOptionsJPanel.ColorTextOptions;
import aprs.framework.colortextdisplay.ColorTextJFrame;
import aprs.framework.colortextdisplay.ColorTextJPanel;
import aprs.framework.pddl.executor.PositionMap;
import aprs.framework.pddl.executor.PositionMapEntry;
import aprs.framework.pddl.executor.PositionMapJPanel;
import aprs.framework.screensplash.SplashScreen;
import com.google.common.base.Objects;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsMulitSupervisorJFrame extends javax.swing.JFrame {

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    public AprsMulitSupervisorJFrame() {

        initComponents();
        jTableRobots.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                try {
                    boolean changeFound = false;
                    for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                        String robotName = (String) jTableRobots.getValueAt(i, 0);
                        Boolean enabled = (Boolean) jTableRobots.getValueAt(i, 1);
                        Boolean wasEnabled = robotEnableMap.get(robotName);

                        if (!Objects.equal(enabled, wasEnabled)) {
                            setRobotEnabled(robotName, enabled);
                        }
                        Utils.autoResizeTableColWidths(jTablePositionMappings);
                        Utils.autoResizeTableRowHeights(jTablePositionMappings);
                    }

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
        jTableTasks.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {

            private final List<JTextArea> areas = new ArrayList<>();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                while (areas.size() <= row) {
                    JTextArea area = new JTextArea();
                    area.setOpaque(true);
                    area.setVisible(true);
                    areas.add(area);
                }
                JTextArea area = areas.get(row);
                if (null != value && null != area) {
                    area.setFont(table.getFont());
                    area.setText(value.toString());
                }
                return area;
            }

        });
        jTableTasks.getColumnModel().getColumn(3).setCellEditor(new TableCellEditor() {

            private final JTextArea editTableArea = new JTextArea();
            private List<CellEditorListener> listeners = new ArrayList<>();

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                editTableArea.setOpaque(true);
                editTableArea.setVisible(true);
                editTableArea.setText(value.toString());
                editTableArea.setFont(table.getFont());
                return editTableArea;
            }

            @Override
            public Object getCellEditorValue() {
                return editTableArea.getText();
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }

            @Override
            public boolean stopCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
                    if (null != l) {
                        l.editingStopped(new ChangeEvent(jTableTasks));
                    }
                }
                return true;
            }

            @Override
            public void cancelCellEditing() {
                for (int i = 0; i < listeners.size(); i++) {
                    CellEditorListener l = listeners.get(i);
                    if (null != l) {
                        l.editingCanceled(new ChangeEvent(jTableTasks));
                    }
                }
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
                listeners.add(l);
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
                listeners.remove(l);
            }
        });
        jTablePositionMappings.getSelectionModel().addListSelectionListener(x -> updateSelectedPosMapFileTable());
        jTableSelectedPosMapFile.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (TableModelEvent.UPDATE == e.getType()) {
                    if (e.getFirstRow() == e.getLastRow()
                            && e.getLastRow() >= 0
                            && e.getColumn() >= 0) {
                        double dval = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), e.getColumn());
                        double other;
                        switch (e.getColumn()) {
                            case 0:
                                other = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), 3);
                                jTableSelectedPosMapFile.setValueAt(other - dval, e.getFirstRow(), 6);
                                break;

                            case 1:
                                other = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), 4);
                                jTableSelectedPosMapFile.setValueAt(other - dval, e.getFirstRow(), 7);
                                break;

                            case 2:
                                other = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), 5);
                                jTableSelectedPosMapFile.setValueAt(other - dval, e.getFirstRow(), 8);
                                break;

                            case 3:
                                other = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), 0);
                                jTableSelectedPosMapFile.setValueAt(dval - other, e.getFirstRow(), 6);
                                break;

                            case 4:
                                other = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), 1);
                                jTableSelectedPosMapFile.setValueAt(dval - other, e.getFirstRow(), 7);
                                break;

                            case 5:
                                other = (double) jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), 2);
                                jTableSelectedPosMapFile.setValueAt(dval - other, e.getFirstRow(), 8);
                                break;

                        }
                    }
                }
            }
        });
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();

        Utils.autoResizeTableColWidths(jTablePositionMappings);
        Utils.autoResizeTableRowHeights(jTablePositionMappings);

        try {
            setIconImage(ImageIO.read(AprsMulitSupervisorJFrame.class
                    .getResource("aprs.png")));

        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void startColorTextReader() {
        this.colorTextJPanel1.startReader();
    }

    public static File getLastSetupFile() throws IOException {
        if (lastSetupFileFile.exists()) {
            return new File(readFirstLine(lastSetupFileFile));
        }
        return null;
    }

    public static File getLastPosMapFile() throws IOException {
        if (lastPosMapFileFile.exists()) {
            return new File(readFirstLine(lastPosMapFileFile));
        }
        return null;
    }

    public void loadPrevSetup() {
        try {
            File setupFile = getLastSetupFile();
            if (null != setupFile && setupFile.exists()) {
                loadSetupFile(setupFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (IOException ex1) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    public void loadPrevPosMapFile() {
        try {
            File posFile = getLastPosMapFile();
            if (null != posFile && posFile.exists()) {
                loadPositionMaps(posFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    JPanel blankPanel = new JPanel();

    private AprsJFrame posMapInSys = null;
    private AprsJFrame posMapOutSys = null;

    private AprsJFrame findSystemWithRobot(String robot) {
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aj = aprsSystems.get(i);
            if (aj.getRobotName() != null && aj.getRobotName().equals(robot)) {
                return aj;
            }
        }
        return null;
    }

    private void updateSelectedPosMapFileTable() {
        int row = jTablePositionMappings.getSelectedRow();
        int col = jTablePositionMappings.getSelectedColumn();
        jButtonSetInFromCurrent.setEnabled(false);
        jButtonSetOutFromCurrent.setEnabled(false);
        if (row >= 0 && row < jTablePositionMappings.getRowCount() && col > 0 && col < jTablePositionMappings.getColumnCount()) {
            try {
                String inSys = (String) jTablePositionMappings.getValueAt(row, 0);
                String outSys = (String) jTablePositionMappings.getColumnName(col);
                posMapInSys = findSystemWithRobot(inSys);

                if (null != posMapInSys) {
                    jButtonSetInFromCurrent.setText("Set In From " + posMapInSys.getRobotName());
                    jButtonSetInFromCurrent.setEnabled(true);
                }
                posMapOutSys = findSystemWithRobot(outSys);
                if (null != posMapOutSys) {
                    jButtonSetOutFromCurrent.setText("Set Out From " + posMapOutSys.getRobotName());
                    jButtonSetOutFromCurrent.setEnabled(true);
                }
                File f = getPosMapFile(inSys, outSys);
                if (f != null) {
                    jTextFieldSelectedPosMapFilename.setText(f.getCanonicalPath());
                    PositionMap pm = new PositionMap(f);
                    DefaultTableModel model = (DefaultTableModel) jTableSelectedPosMapFile.getModel();
                    model.setRowCount(0);
                    for (int i = 0; i < pm.getErrmapList().size(); i++) {
                        PositionMapEntry pme = pm.getErrmapList().get(i);
                        model.addRow(new Object[]{
                            pme.getRobotX(), pme.getRobotY(), pme.getRobotZ(),
                            pme.getRobotX() + pme.getOffsetX(), pme.getRobotY() + pme.getOffsetY(), pme.getRobotZ() + pme.getOffsetZ(),
                            pme.getOffsetX(), pme.getOffsetY(), pme.getOffsetZ()
                        });
                    }
                }
                if (jTableSelectedPosMapFile.getRowCount() > 0) {
                    jTableSelectedPosMapFile.getSelectionModel().setSelectionInterval(0, 0);
                }
            } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
    private List<List<PositionMapJPanel>> positionMapJPanels = new ArrayList<>();

    private List<PositionMapJPanel> getPositionMapRow(int row) {
        while (positionMapJPanels.size() <= row) {
            positionMapJPanels.add(new ArrayList<>());
        }
        return positionMapJPanels.get(row);
    }

    private PositionMapJPanel getPositionMap(int row, int col) {
        List<PositionMapJPanel> lrow = getPositionMapRow(row);
        while (lrow.size() <= col) {
            lrow.add(new PositionMapJPanel());
        }
        return lrow.get(col);
    }

    private void setRobotEnabled(String robotName, Boolean enabled) {
        if (null != robotName && null != enabled) {
            robotEnableMap.put(robotName, enabled);
            if (!enabled) {
                try {
                    this.lastFutureReturned = stealRobot(robotName);
                } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                    Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private XFuture<Void> stealRobot(String robotName) throws IOException, PositionMap.BadErrorMapFormatException {
        for (int i = 0; i < aprsSystems.size() - 1; i++) {
            if (aprsSystems.get(i).getRobotName().equals(robotName)) {
                return stealRobot(aprsSystems.get(i + 1), aprsSystems.get(i));
            }
        }
        return XFuture.completedFuture(null);
    }

    final private static String transferrableOptions[] = new String[]{
        "rpy",
        "lookForXYZ",
        "slowTransSpeed",
        "jointAccel",
        "jointSpeed",
        "rotSpeed",
        "fastTransSpeed",
        "settleDwellTime",
        "lookForJoints",
        "useJointLookFor"
    };

    private void copyOptions(String options[], Map<String, String> mapIn, Map<String, String> mapOut) {
        for (String opt : options) {
            if (mapIn.containsKey(opt)) {
                mapOut.put(opt, mapIn.get(opt));
            }
        }
    }

    private final AtomicReference<Runnable> returnRobotRunnable = new AtomicReference<>();

    private void returnRobots() {
        Runnable r = returnRobotRunnable.getAndSet(null);
        if (r != null) {
            r.run();
        }
    }

    private XFuture<Void> stealRobot(AprsJFrame stealFrom, AprsJFrame stealFor) throws IOException, PositionMap.BadErrorMapFormatException {
        File f = getPosMapFile(stealFor.getRobotName(), stealFrom.getRobotName());
        PositionMap pm = (f != null && !f.getName().equals("null")) ? new PositionMap(f) : PositionMap.emptyPositionMap();

        initColorTextSocket();
        String stealFromOrigCrclHost = stealFrom.getRobotCrclHost();
        int stealFromOrigCrclPort = stealFrom.getRobotCrclPort();
        String stealFromRobotName = stealFrom.getRobotName();

        String stealForOrigCrclHost = stealFor.getRobotCrclHost();
        int stealForOrigCrclPort = stealFor.getRobotCrclPort();
        String stealForRobotName = stealFor.getRobotName();

        Map<String, String> stealFromOptions = new HashMap<>();
        copyOptions(transferrableOptions, stealFrom.getExecutorOptions(), stealFromOptions);

        Map<String, String> stealForOptions = new HashMap<>();
        copyOptions(transferrableOptions, stealFor.getExecutorOptions(), stealForOptions);
        returnRobotRunnable.set(() -> {
            stealFrom.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);

            for (String opt : transferrableOptions) {
                if (stealForOptions.containsKey(opt)) {
                    stealFor.setExecutorOption(opt, stealForOptions.get(opt));
                }
            }
//                    if (null != stealForRpyOption) {
//                        stealFor.setExecutorOption("rpy", stealForRpyOption);
//                    }
//                    if (null != stealForLookForXYZOption) {
//                        stealFor.setExecutorOption("lookForXYZ", stealForLookForXYZOption);
//                    }
            stealFor.removePositionMap(pm);
            stealFor.connectRobot(stealForRobotName, stealForOrigCrclHost, stealForOrigCrclPort);
            stealFor.disconnectRobot();
            stealFor.setRobotName(stealForRobotName);
        }
        );
//        if(null == colorTextSocket) {
//            colorTextSocket = new Socket("localhost",ColorTextJPanel.COLORTEXT_SOCKET_PORT);
//        }

//        String stealFromRpyOption = stealFrom.getExecutorOptions().get("rpy");
//        String stealFromLookForXYZOption = stealFrom.getExecutorOptions().get("lookForXYZ");
//
//        String stealForRpyOption = stealFor.getExecutorOptions().get("rpy");
//        String stealForLookForXYZOption = stealFor.getExecutorOptions().get("lookForXYZ");
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        return XFuture.allOf(
                stealFrom.safeAbortAndDisconnectAsync(),
                stealFor.safeAbort()
                        .thenCompose(x -> {
                            if (null != colorTextSocket) {
                                try {
                                    colorTextSocket.getOutputStream().write("0xFF0000, 0x00FF00\r\n".getBytes());
                                } catch (IOException ex) {
                                    Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            return SplashScreen.showMessageFullScreen(stealForRobotName + "\n Disabled", 80.0f,
                                    SplashScreen.getDisableImageImage(),
                                    SplashScreen.getRedYellowColorList(), gd);
                        }))
                .thenRun(() -> {
                    stealFor.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);
                    stealFor.addPositionMap(pm);
                    for (String opt : transferrableOptions) {
                        if (stealFromOptions.containsKey(opt)) {
                            stealFor.setExecutorOption(opt, stealFromOptions.get(opt));
                        }
                    }
//                    if (null != stealFromRpyOption) {
//                        stealFor.setExecutorOption("rpy", stealFromRpyOption);
//                    }
//                    if (null != stealFromLookForXYZOption) {
//                        stealFor.setExecutorOption("lookForXYZ", stealFromLookForXYZOption);
//                    }
//                    return null;
                })
                .thenCompose(x -> {
                    return SplashScreen.showMessageFullScreen("Switching to \n" + stealFromRobotName, 80.0f,
                            SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                })
                //                        () -> 
                .thenCompose(x -> {
                    return stealFor.continueActionList();
                })
                .thenCompose(x -> {
                    return stealFor.safeAbortAndDisconnectAsync();
                })
                .thenCompose(x -> {
                    return SplashScreen.showMessageFullScreen("Returning \n" + stealFromRobotName, 80.0f,
                            SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                })
                .thenRun(this::returnRobots)
                .thenCompose(x -> {
                    return stealFrom.continueActionList();
                })
                .thenCompose(x -> {
                    return SplashScreen.showMessageFullScreen("All \nTasks \nComplete", 80.0f,
                            null, SplashScreen.getBlueWhiteGreenColorList(), gd);
                });
    }

    private void initColorTextSocket() throws IOException {
        if (null == colorTextSocket) {
            colorTextSocket = new Socket("localhost", ColorTextJPanel.COLORTEXT_SOCKET_PORT);
        }
    }

    private final Map<String, Boolean> robotEnableMap = new HashMap<>();

    private static String readFirstLine(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        }
    }

    private final static File lastSetupFileFile = new File(System.getProperty("aprsLastMultiSystemSetupFile", System.getProperty("user.home") + File.separator + ".lastAprsSetupFile.txt"));
    private final static File lastPosMapFileFile = new File(System.getProperty("aprsLastMultiSystemPosMapFile", System.getProperty("user.home") + File.separator + ".lastAprsPosMapFile.txt"));

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanelTasksAndRobots = new javax.swing.JPanel();
        jPanelTasks = new javax.swing.JPanel();
        jScrollPaneTasks = new javax.swing.JScrollPane();
        jTableTasks = new javax.swing.JTable();
        jPanelRobots = new javax.swing.JPanel();
        jScrollPaneRobots = new javax.swing.JScrollPane();
        jTableRobots = new javax.swing.JTable();
        colorTextJPanel1 = new aprs.framework.colortextdisplay.ColorTextJPanel();
        jPanelPositionMappings = new javax.swing.JPanel();
        jPanelPosMapFiles = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablePositionMappings = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jButtonSetInFromCurrent = new javax.swing.JButton();
        jButtonAddLine = new javax.swing.JButton();
        jButtonDeleteLine = new javax.swing.JButton();
        jButtonSetOutFromCurrent = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableSelectedPosMapFile = new javax.swing.JTable();
        jButtonSaveSelectedPosMap = new javax.swing.JButton();
        jTextFieldSelectedPosMapFilename = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemSaveSetup = new javax.swing.JMenuItem();
        jMenuItemSaveSetupAs = new javax.swing.JMenuItem();
        jMenuItemLoadSetup = new javax.swing.JMenuItem();
        jMenuItemAddExistingSystem = new javax.swing.JMenuItem();
        jMenuItemAddNewSystem = new javax.swing.JMenuItem();
        jMenuItemRemoveSelectedSystem = new javax.swing.JMenuItem();
        jMenuItemSavePosMaps = new javax.swing.JMenuItem();
        jMenuItemLoadPosMaps = new javax.swing.JMenuItem();
        jMenuActions = new javax.swing.JMenu();
        jMenuItemStartAll = new javax.swing.JMenuItem();
        jMenuItemSafeAbortAll = new javax.swing.JMenuItem();
        jMenuItemImmediateAbortAll = new javax.swing.JMenuItem();
        jMenuItemConnectAll = new javax.swing.JMenuItem();
        jMenuItemDbgAction = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenu();
        jCheckBoxMenuItemDisableTextPopups = new javax.swing.JCheckBoxMenuItem();
        jMenuItemStartColorTextDisplay = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Multi Aprs Supervisor");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jPanelTasks.setBorder(null);

        jTableTasks.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jTableTasks.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Priority", "Task(s)", "Robot(s)", "Details", "PropertiesFile"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, true, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableTasks.setRowHeight(30);
        jScrollPaneTasks.setViewportView(jTableTasks);

        javax.swing.GroupLayout jPanelTasksLayout = new javax.swing.GroupLayout(jPanelTasks);
        jPanelTasks.setLayout(jPanelTasksLayout);
        jPanelTasksLayout.setHorizontalGroup(
            jPanelTasksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTasksLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneTasks)
                .addContainerGap())
        );
        jPanelTasksLayout.setVerticalGroup(
            jPanelTasksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneTasks, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
        );

        jPanelRobots.setBorder(javax.swing.BorderFactory.createTitledBorder("Robots"));

        jTableRobots.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jTableRobots.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Robot", "Enabled", "Host", "Port"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTableRobots.setRowHeight(30);
        jScrollPaneRobots.setViewportView(jTableRobots);

        javax.swing.GroupLayout jPanelRobotsLayout = new javax.swing.GroupLayout(jPanelRobots);
        jPanelRobots.setLayout(jPanelRobotsLayout);
        jPanelRobotsLayout.setHorizontalGroup(
            jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelRobotsLayout.setVerticalGroup(
            jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelTasksAndRobotsLayout = new javax.swing.GroupLayout(jPanelTasksAndRobots);
        jPanelTasksAndRobots.setLayout(jPanelTasksAndRobotsLayout);
        jPanelTasksAndRobotsLayout.setHorizontalGroup(
            jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                .addGroup(jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(colorTextJPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanelTasksAndRobotsLayout.setVerticalGroup(
            jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(colorTextJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane2.addTab("Tasks and Robots", jPanelTasksAndRobots);

        jPanelPosMapFiles.setBorder(javax.swing.BorderFactory.createTitledBorder("Files"));

        jTablePositionMappings.setModel(defaultPositionMappingsModel());
        jTablePositionMappings.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTablePositionMappingsMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTablePositionMappingsMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTablePositionMappingsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTablePositionMappings);

        javax.swing.GroupLayout jPanelPosMapFilesLayout = new javax.swing.GroupLayout(jPanelPosMapFiles);
        jPanelPosMapFiles.setLayout(jPanelPosMapFilesLayout);
        jPanelPosMapFilesLayout.setHorizontalGroup(
            jPanelPosMapFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapFilesLayout.createSequentialGroup()
                .addComponent(jScrollPane1)
                .addGap(6, 6, 6))
        );
        jPanelPosMapFilesLayout.setVerticalGroup(
            jPanelPosMapFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapFilesLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected File"));

        jButtonSetInFromCurrent.setText("Set In From Selected Row System");
        jButtonSetInFromCurrent.setEnabled(false);
        jButtonSetInFromCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetInFromCurrentActionPerformed(evt);
            }
        });

        jButtonAddLine.setText("Add Line");
        jButtonAddLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddLineActionPerformed(evt);
            }
        });

        jButtonDeleteLine.setText("Delete Line");
        jButtonDeleteLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteLineActionPerformed(evt);
            }
        });

        jButtonSetOutFromCurrent.setText("Set Out From Selected Column System");
        jButtonSetOutFromCurrent.setEnabled(false);
        jButtonSetOutFromCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetOutFromCurrentActionPerformed(evt);
            }
        });

        jTableSelectedPosMapFile.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0)}
            },
            new String [] {
                "Xin", "Yin", "Zin", "Xout", "Yout", "Zout", "Offset_X", "Offset_Y", "Offset_Z"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, true, true, true, true, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTableSelectedPosMapFile);

        jButtonSaveSelectedPosMap.setText("Save");
        jButtonSaveSelectedPosMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveSelectedPosMapActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonSetInFromCurrent)
                        .addGap(50, 50, 50)
                        .addComponent(jButtonAddLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDeleteLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSaveSelectedPosMap)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 206, Short.MAX_VALUE)
                        .addComponent(jButtonSetOutFromCurrent))
                    .addComponent(jTextFieldSelectedPosMapFilename))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSetInFromCurrent)
                    .addComponent(jButtonAddLine)
                    .addComponent(jButtonDeleteLine)
                    .addComponent(jButtonSetOutFromCurrent)
                    .addComponent(jButtonSaveSelectedPosMap))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldSelectedPosMapFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout jPanelPositionMappingsLayout = new javax.swing.GroupLayout(jPanelPositionMappings);
        jPanelPositionMappings.setLayout(jPanelPositionMappingsLayout);
        jPanelPositionMappingsLayout.setHorizontalGroup(
            jPanelPositionMappingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelPositionMappingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPositionMappingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelPosMapFiles, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelPositionMappingsLayout.setVerticalGroup(
            jPanelPositionMappingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPositionMappingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelPosMapFiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Position Mapping", jPanelPositionMappings);

        jMenuFile.setText("File");

        jMenuItemSaveSetup.setText("Save Setup");
        jMenuItemSaveSetup.setEnabled(false);
        jMenuFile.add(jMenuItemSaveSetup);

        jMenuItemSaveSetupAs.setText("Save Setup As ...");
        jMenuItemSaveSetupAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveSetupAsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveSetupAs);

        jMenuItemLoadSetup.setText("Load Setup ...");
        jMenuItemLoadSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadSetupActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemLoadSetup);

        jMenuItemAddExistingSystem.setText("Add Existing System ...");
        jMenuItemAddExistingSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAddExistingSystemActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemAddExistingSystem);

        jMenuItemAddNewSystem.setText("Add New System ...");
        jMenuItemAddNewSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAddNewSystemActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemAddNewSystem);

        jMenuItemRemoveSelectedSystem.setText("Remove Selected System");
        jMenuItemRemoveSelectedSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRemoveSelectedSystemActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemRemoveSelectedSystem);

        jMenuItemSavePosMaps.setText("Save Position Maps as ...");
        jMenuItemSavePosMaps.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePosMapsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSavePosMaps);

        jMenuItemLoadPosMaps.setText("Load Position Maps ...");
        jMenuItemLoadPosMaps.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadPosMapsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemLoadPosMaps);

        jMenuBar1.add(jMenuFile);

        jMenuActions.setText("Actions");

        jMenuItemStartAll.setText("Start All");
        jMenuItemStartAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemStartAll);

        jMenuItemSafeAbortAll.setText("Safe Abort All");
        jMenuItemSafeAbortAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSafeAbortAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemSafeAbortAll);

        jMenuItemImmediateAbortAll.setText("Immediate Abort All");
        jMenuItemImmediateAbortAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemImmediateAbortAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemImmediateAbortAll);

        jMenuItemConnectAll.setText("Connect All ...");
        jMenuItemConnectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemConnectAll);

        jMenuItemDbgAction.setText("Dbg Action");
        jMenuItemDbgAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDbgActionActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemDbgAction);

        jMenuBar1.add(jMenuActions);

        jMenuOptions.setText("Options");

        jCheckBoxMenuItemDisableTextPopups.setSelected(true);
        jCheckBoxMenuItemDisableTextPopups.setText("Disable Text Popups");
        jCheckBoxMenuItemDisableTextPopups.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDisableTextPopupsActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemDisableTextPopups);

        jMenuItemStartColorTextDisplay.setText("Start ColorText Display ...");
        jMenuItemStartColorTextDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartColorTextDisplayActionPerformed(evt);
            }
        });
        jMenuOptions.add(jMenuItemStartColorTextDisplay);

        jMenuBar1.add(jMenuOptions);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private static class PositionMappingTableModel extends DefaultTableModel {

        public PositionMappingTableModel() {
        }

        public PositionMappingTableModel(int rowCount, int columnCount) {
            super(rowCount, columnCount);
        }

        public PositionMappingTableModel(Vector columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        public PositionMappingTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        public PositionMappingTableModel(Vector data, Vector columnNames) {
            super(data, columnNames);
        }

        public PositionMappingTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }
    }

    private TableModel defaultPositionMappingsModel() {
        return new PositionMappingTableModel(
                new Object[][]{
                    {"System", "Robot1", "Robot2"},
                    {"Robot1", null, new File("R1R2.csv")},
                    {"Robot2", new File("R1R2.csv"), null},}, new Object[]{"", "", ""});

    }

    private void jMenuItemSaveSetupAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSetupAsActionPerformed
        browseSaveSetupAs();
    }//GEN-LAST:event_jMenuItemSaveSetupAsActionPerformed

    public void browseSaveSetupAs() throws HeadlessException {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Choose APRS Multi Supervisor CSV to create (save as).");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma Separated Values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        if (lastSetupFile != null) {
            chooser.setCurrentDirectory(lastSetupFile.getParentFile());
            chooser.setSelectedFile(lastSetupFile);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            try {
                saveSetupFile(chooser.getSelectedFile());

            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jMenuItemLoadSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadSetupActionPerformed
        browseOpenSetup();
    }//GEN-LAST:event_jMenuItemLoadSetupActionPerformed

    public void browseOpenSetup() throws HeadlessException {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Choose APRS Multi Supervisor CSV to Open.");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma Separated Values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        if (lastSetupFile != null) {
            chooser.setCurrentDirectory(lastSetupFile.getParentFile());
            chooser.setSelectedFile(lastSetupFile);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                loadSetupFile(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jMenuItemAddExistingSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddExistingSystemActionPerformed
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Text properties files.", "txt");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Open APRS System properties file to be added to multi-system supervisor.");
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                File propertiesFile = chooser.getSelectedFile();
                AprsJFrame aj = new AprsJFrame(propertiesFile);
//                aj.setPropertiesFile(propertiesFile);
                addAprsSystem(aj);
                saveCurrentSetup();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemAddExistingSystemActionPerformed

    public void addAprsSystem(AprsJFrame aj) throws IOException {
        aj.setPriority(aprsSystems.size() + 1);
        aj.setVisible(true);
        aj.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        aprsSystems.add(aj);
        aj.getTitleUpdateRunnables().add(() -> {
            Utils.runOnDispatchThreadWithCatch(this::updateTasksTable);
        });
        updateTasksTable();
        updateRobotsTable();
    }

    private void jMenuItemRemoveSelectedSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRemoveSelectedSystemActionPerformed
        int selectedIndex = jTableTasks.getSelectedRow();
        if (selectedIndex >= 0 && selectedIndex < aprsSystems.size()) {
            try {
                AprsJFrame aj = aprsSystems.remove(selectedIndex);
                try {
                    aj.close();

                } catch (Exception ex) {
                    Logger.getLogger(AprsMulitSupervisorJFrame.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                updateTasksTable();
                updateRobotsTable();
                saveCurrentSetup();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemRemoveSelectedSystemActionPerformed

    private XFuture<?> lastFutureReturned = null;

    private void jMenuItemStartAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        lastFutureReturned = startAll();
    }//GEN-LAST:event_jMenuItemStartAllActionPerformed

    private void jMenuItemSavePosMapsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePosMapsActionPerformed
        browseAndSavePositionMappings();
    }//GEN-LAST:event_jMenuItemSavePosMapsActionPerformed

    private void browseAndSavePositionMappings() throws HeadlessException {
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Comma-separated values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Choose APRS position mappings csv file to create (save as)");
        if (null != lastPosMapFile) {
            chooser.setCurrentDirectory(lastPosMapFile.getParentFile());
            chooser.setSelectedFile(lastPosMapFile);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            try {
                savePositionMaps(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jTablePositionMappingsMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePositionMappingsMousePressed
        if (evt.isPopupTrigger()) {
            showPosTablePopup(evt.getLocationOnScreen());
        }
    }//GEN-LAST:event_jTablePositionMappingsMousePressed

    private void jTablePositionMappingsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePositionMappingsMouseReleased
        if (evt.isPopupTrigger()) {
            showPosTablePopup(evt.getLocationOnScreen());
        }
    }//GEN-LAST:event_jTablePositionMappingsMouseReleased

    private void jTablePositionMappingsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePositionMappingsMouseClicked
        if (evt.isPopupTrigger()) {
            showPosTablePopup(evt.getLocationOnScreen());
        }
    }//GEN-LAST:event_jTablePositionMappingsMouseClicked

    private void jMenuItemLoadPosMapsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPosMapsActionPerformed
        browseOpenPosMapsFile();
    }//GEN-LAST:event_jMenuItemLoadPosMapsActionPerformed

    public void browseOpenPosMapsFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose APRS Position Maps CSV to Open.");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma Separated Values", "csv");
        chooser.addChoosableFileFilter(filter);
        if (lastPosMapFile != null) {
            chooser.setCurrentDirectory(lastPosMapFile.getParentFile());
            chooser.setSelectedFile(lastPosMapFile);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                loadPositionMaps(chooser.getSelectedFile());

            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jMenuItemSafeAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSafeAbortAllActionPerformed
        lastFutureReturned = safeAbortAll();
    }//GEN-LAST:event_jMenuItemSafeAbortAllActionPerformed

    private void jMenuItemImmediateAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortAllActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        immediateAbortAll();
    }//GEN-LAST:event_jMenuItemImmediateAbortAllActionPerformed

    private void jButtonSetInFromCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetInFromCurrentActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            if (null != posMapInSys) {
                PoseType pose = posMapInSys.getCurrentPose();
                if (null != pose) {
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX().doubleValue(), row, 0);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY().doubleValue(), row, 1);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ().doubleValue(), row, 2);
                    double otherx = (double) jTableSelectedPosMapFile.getValueAt(row, 3);
                    double othery = (double) jTableSelectedPosMapFile.getValueAt(row, 4);
                    double otherz = (double) jTableSelectedPosMapFile.getValueAt(row, 5);
                    jTableSelectedPosMapFile.setValueAt(otherx - pose.getPoint().getX().doubleValue(), row, 6);
                    jTableSelectedPosMapFile.setValueAt(othery - pose.getPoint().getY().doubleValue(), row, 7);
                    jTableSelectedPosMapFile.setValueAt(otherz - pose.getPoint().getZ().doubleValue(), row, 8);
                }
            }
        }
    }//GEN-LAST:event_jButtonSetInFromCurrentActionPerformed

    private void jMenuItemConnectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectAllActionPerformed
        connectAll();
    }//GEN-LAST:event_jMenuItemConnectAllActionPerformed

    private void jButtonSetOutFromCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetOutFromCurrentActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            if (null != posMapOutSys) {
                PoseType pose = posMapOutSys.getCurrentPose();
                if (null != pose) {

                    double otherx = (double) jTableSelectedPosMapFile.getValueAt(row, 0);
                    double othery = (double) jTableSelectedPosMapFile.getValueAt(row, 1);
                    double otherz = (double) jTableSelectedPosMapFile.getValueAt(row, 2);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX().doubleValue(), row, 3);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY().doubleValue(), row, 4);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ().doubleValue(), row, 5);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX().doubleValue() - otherx, row, 6);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY().doubleValue() - othery, row, 7);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ().doubleValue() - otherz, row, 8);
                }
            }
        }
    }//GEN-LAST:event_jButtonSetOutFromCurrentActionPerformed

    private void jButtonAddLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddLineActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTableSelectedPosMapFile.getModel();
        model.addRow(new Double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});
    }//GEN-LAST:event_jButtonAddLineActionPerformed

    private void jButtonDeleteLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteLineActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            DefaultTableModel model = (DefaultTableModel) jTableSelectedPosMapFile.getModel();
            model.removeRow(row);
        }
    }//GEN-LAST:event_jButtonDeleteLineActionPerformed

    private void jButtonSaveSelectedPosMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveSelectedPosMapActionPerformed
        try {
            File f = resolveFile(jTextFieldSelectedPosMapFilename.getText(), lastPosMapFile.getParentFile());
            JFileChooser chooser = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("Comma-separated values", "csv");
            chooser.addChoosableFileFilter(filter);
            chooser.setFileFilter(filter);
            chooser.setDialogTitle("Choose APRS position mapping csv file to create (save as)");
            if (null != f) {
                chooser.setCurrentDirectory(f.getParentFile());
                chooser.setSelectedFile(f);
            }

            if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
                savePosFile(chooser.getSelectedFile());
            }
            int row = jTablePositionMappings.getSelectedRow();
            int col = jTablePositionMappings.getSelectedColumn();
            if (row >= 0 && row < jTablePositionMappings.getRowCount() && col > 0 && col < jTablePositionMappings.getColumnCount()) {
                DefaultTableModel model = (DefaultTableModel) jTablePositionMappings.getModel();
                model.setValueAt(relativeFile(lastPosMapFile.getParentFile(), chooser.getSelectedFile()), row, col);
            }
            jTextFieldSelectedPosMapFilename.setText(chooser.getSelectedFile().getCanonicalPath());
            if (JOptionPane.showConfirmDialog(this, "Also Save files list?") == JOptionPane.YES_OPTION) {
                browseAndSavePositionMappings();
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonSaveSelectedPosMapActionPerformed

    private void jCheckBoxMenuItemDisableTextPopupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed

    private void jMenuItemDbgActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDbgActionActionPerformed
        System.out.println("lastFutureReturned = " + lastFutureReturned);
    }//GEN-LAST:event_jMenuItemDbgActionActionPerformed

    private Socket colorTextSocket = null;
    private ColorTextJFrame colorTextJFrame = null;

    private void jMenuItemStartColorTextDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartColorTextDisplayActionPerformed

        ColorTextOptions options = ColorTextOptionsJPanel.query(this, true);
        if (null != options) {
            try {
                if (null != colorTextSocket) {
                    colorTextSocket.close();
                }
                if (null != colorTextJFrame) {
                    colorTextJFrame.setVisible(false);
                }

                if (options.isStartDisplay()) {
                    colorTextJFrame = new ColorTextJFrame();
                    colorTextJFrame.setVisible(true);
                    colorTextJFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                }
                colorTextSocket = new Socket(options.getHost(), options.getPort());
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemStartColorTextDisplayActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        if (null != colorTextJFrame) {
            colorTextJFrame.setVisible(false);
            colorTextJFrame = null;
        }
        if (null != colorTextSocket) {
            try {
                colorTextSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            colorTextSocket = null;
        }
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (null != colorTextJFrame) {
            colorTextJFrame.setVisible(false);
            colorTextJFrame = null;
        }
        if (null != colorTextSocket) {
            try {
                colorTextSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            colorTextSocket = null;
        }
    }//GEN-LAST:event_formWindowClosing

    private void jMenuItemAddNewSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddNewSystemActionPerformed
        try {
            AprsJFrame aj = new AprsJFrame();
            aj.emptyInit();
            addAprsSystem(aj);
            aj.browseSavePropertiesFileAs();
            saveCurrentSetup();
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemAddNewSystemActionPerformed

    private void savePosFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (int i = 0; i < jTableSelectedPosMapFile.getColumnCount(); i++) {
                pw.print(jTableSelectedPosMapFile.getColumnName(i));
                pw.print(",");
            }
            pw.println();
            for (int i = 0; i < jTableSelectedPosMapFile.getRowCount(); i++) {
                for (int j = 0; j < jTableSelectedPosMapFile.getColumnCount(); j++) {
                    pw.print(jTableSelectedPosMapFile.getValueAt(i, j));
                    pw.print(",");
                }
                pw.println();
            }
        }
    }

    private void clearPosTable() {
        DefaultTableModel tm = (DefaultTableModel) jTablePositionMappings.getModel();
        tm.setRowCount(0);
        tm.setColumnCount(0);
        tm.addColumn("System");
        for (String name : robotEnableMap.keySet()) {
            tm.addColumn(name);
        }
        for (String name : robotEnableMap.keySet()) {
            Object data[] = new Object[robotEnableMap.size()];
            data[0] = name;
            tm.addRow(data);
        }
        Utils.autoResizeTableColWidths(jTablePositionMappings);
        Utils.autoResizeTableRowHeights(jTablePositionMappings);
        if (null != posTablePopupMenu) {
            posTablePopupMenu.setVisible(false);
        }
    }

    private JPopupMenu posTablePopupMenu = null;

    private void showPosTablePopup(Point pt) {
        if (posTablePopupMenu == null) {
            posTablePopupMenu = new JPopupMenu();
            JMenuItem mi = new JMenuItem("Clear");
            mi.addActionListener(l -> clearPosTable());
            posTablePopupMenu.add(mi);
        }
        posTablePopupMenu.setLocation(pt.x, pt.y);
        posTablePopupMenu.setVisible(true);
    }

    public void enableAllRobots() {
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        DefaultTableModel model = (DefaultTableModel) jTableRobots.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(true, i, 1);
        }
        if (null != colorTextSocket) {
            try {
                colorTextSocket.getOutputStream().write("0x00FF00, 0x00FF000\r\n".getBytes());
                colorTextSocket.getOutputStream().flush();
            } catch (IOException ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public XFuture<Void> startAll() {
        returnRobots();
        enableAllRobots();
        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).startActions();
        }
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        return XFuture.allOf(futures);
//                .thenCompose(x -> {
//                    return SplashScreen.showMessageFullScreen("All \nTasks \nComplete", 80.0f,
//                            null, SplashScreen.getBlueWhiteGreenColorList(), gd);
//                });
    }

    public void immediateAbortAll() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).immediateAbort();
        }
    }

    public void connectAll() {
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).setConnected(true);
        }
    }

    public XFuture<Void> safeAbortAll() {
        XFuture<?> prevLastFuture = lastFutureReturned;
        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).safeAbort();
        }
        return XFuture.allOf(futures).thenRun(() -> {
            if (null != prevLastFuture) {
                prevLastFuture.cancelAll(false);
            }
        });
    }

    private File setupFile;

    /**
     * Get the value of setupFile
     *
     * @return the value of setupFile
     */
    public File getSetupFile() {
        return setupFile;
    }

    /**
     * Set the value of setupFile
     *
     * @param f new value of setupFile
     */
    public void setSetupFile(File f) throws IOException {
        if (!Objects.equal(this.setupFile,f)) {
            if (null != f) {
                Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor : " + f));
            } else {
                Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor"));
            }
        }
        if(null != f) {
            saveLastSetupFile(f);
        }
        this.setupFile = f;
        this.jMenuItemSaveSetup.setEnabled(f != null);
    }

    public void saveCurrentSetup() {
        try {
            if(null != setupFile) {
                int response = 
                        JOptionPane.showConfirmDialog(this, "Save Current APRS Supervisor file : "+setupFile);
                if(response == JOptionPane.YES_OPTION) {
                    saveSetupFile(setupFile);   
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsMulitSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveSetupFile(File f) throws IOException {
        saveJTable(f, jTableTasks);
        setSetupFile(f);
    }

    private void saveJTable(File f, JTable jtable) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), CSVFormat.DEFAULT)) {
            TableModel tm = jtable.getModel();
            for (int i = 0; i < tm.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < tm.getColumnCount(); j++) {
                    if (j == 3) {
                        continue;
                    }
                    Object o = tm.getValueAt(i, j);
                    if (o instanceof File) {
                        Path rel = f.getParentFile().toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                        if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                            l.add(rel);
                        } else {
                            l.add(o);
                        }
                    } else {
                        l.add(o);
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    public void savePositionMaps(File f) throws IOException {
        saveJTable(f, jTablePositionMappings);
        saveLastPosMapFile(f);
    }

    private Map<String, Map<String, File>> posMaps = new HashMap<>();

    public File getPosMapFile(String sys1, String sys2) {
        Map<String, File> subMap = posMaps.get(sys1);
        if (null == subMap) {
            return null;
        }
        File f = subMap.get(sys2);
        if (f.exists()) {
            return f;
        }
        File altFile = lastPosMapFile.getParentFile().toPath().resolve(f.toPath()).toFile();
        if (altFile.exists()) {
            return altFile;
        }
        return f;
    }

    public void setPosMapFile(String sys1, String sys2, File f) {
        Map<String, File> subMap = posMaps.get(sys1);
        if (null == subMap) {
            subMap = new HashMap<>();
            posMaps.put(sys1, subMap);
        }
        subMap.put(sys2, f);
    }

    final public void loadPositionMaps(File f) throws IOException {
        System.out.println("Loading position maps  file :" + f.getCanonicalPath());
        DefaultTableModel tm = (DefaultTableModel) jTablePositionMappings.getModel();
        tm.setRowCount(0);
        tm.setColumnCount(0);
        tm.addColumn("System");
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.RFC4180)) {
            String line = null;
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
//                tm.addRow(fields);
                linecount++;
                Object a[] = new Object[csvRecord.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = csvRecord.get(i);
                }
                tm.addColumn(a[0]);
            }
        }
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.RFC4180)) {
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
//                tm.addRow(fields);
                linecount++;
                Object a[] = new Object[csvRecord.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = csvRecord.get(i);
                    if (null != a[i] && !"null".equals(a[i]) && !"".equals(a[i])) {
                        if (i > 0) {
                            String fname = (String) a[i];
                            File fi = resolveFile(fname, f.getParentFile());
                            setPosMapFile((String) a[0], tm.getColumnName(i), fi);
                        }
                    }
                }
                tm.addRow(a);
            }
        }
        saveLastPosMapFile(f);
    }

    private static File resolveFile(String fname, File dir) throws IOException {
        File fi = new File(fname);
        if (!fi.exists() && dir != null && dir.exists() && dir.isDirectory()) {
            File altFile = dir.toPath().toRealPath().resolve(fname).toFile();
            if (altFile.exists()) {
                fi = altFile;
            }
        }
        return fi;
    }

    private static String relativeFile(File dir, File f) throws IOException {
        return dir.toPath().relativize(f.toPath()).toString();
    }

    private File lastSetupFile = null;

    private void saveLastSetupFile(File f) throws IOException {
        lastSetupFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastSetupFileFile))) {
            pw.println(f.getCanonicalPath());
        }

    }

    private File lastPosMapFile = null;

    private void saveLastPosMapFile(File f) throws IOException {
        lastPosMapFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastPosMapFileFile))) {
            pw.println(f.getCanonicalPath());
        }
    }

    private final List<AprsJFrame> aprsSystems = new ArrayList<>();

    /**
     * Get the value of aprsSystems
     *
     * @return the value of aprsSystems
     */
    public List<AprsJFrame> getAprsSystems() {
        return Collections.unmodifiableList(aprsSystems);
    }

    public void closeAllAprsSystems() throws IOException {
        for (AprsJFrame aprsJframe : aprsSystems) {
            try {
                aprsJframe.close();

            } catch (Exception ex) {
                Logger.getLogger(AprsMulitSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        aprsSystems.clear();
        updateTasksTable();
        updateRobotsTable();
    }

    public final void loadSetupFile(File f) throws IOException {
        closeAllAprsSystems();
        System.out.println("Loading setup file :" + f.getCanonicalPath());
        try (CSVParser parser = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.RFC4180)) {
            DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
            String line = null;
            tm.setRowCount(0);
            int linecount = 0;
            for (CSVRecord csvRecord : parser) {
                if (csvRecord.size() < 4) {
                    System.err.println("Bad CSVRecord :" + linecount + " in " + f + "  --> " + csvRecord);
                    System.err.println("csvRecord.size()=" + csvRecord.size());
                    System.err.println("csvRecord.size() must equal 4");
                    System.out.println("");
                    break;
                }
//                tm.addRow(fields);
                int priority = Integer.parseInt(csvRecord.get(0));
                String fileString = csvRecord.get(3);
                File propertiesFile = new File(csvRecord.get(3));
                File altPropFile = f.getParentFile().toPath().toRealPath().resolve(fileString).toFile();
                if (altPropFile.exists()) {
                    propertiesFile = altPropFile;
                }
                AprsJFrame aj = new AprsJFrame(propertiesFile);
                aj.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                aj.setPriority(priority);
                aj.setTaskName(csvRecord.get(1));
                aj.setRobotName(csvRecord.get(2));
                aj.setPropertiesFile(propertiesFile);
                aj.loadProperties();
                aj.setPriority(priority);
                aj.setTaskName(csvRecord.get(1));
                aj.setRobotName(csvRecord.get(2));
                aj.setVisible(true);
                aj.getTitleUpdateRunnables().add(() -> {
                    Utils.runOnDispatchThreadWithCatch(this::updateTasksTable);
                });
                aprsSystems.add(aj);
            }
        }
        Collections.sort(aprsSystems, new Comparator<AprsJFrame>() {
            @Override
            public int compare(AprsJFrame o1, AprsJFrame o2) {
                return Integer.compare(o1.getPriority(), o2.getPriority());
            }
        });
        updateTasksTable();
        updateRobotsTable();
        
        clearPosTable();
        setSetupFile(f);
    }

    public void updateTasksTable() throws IOException {
        DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
        tm.setRowCount(0);
        for (AprsJFrame aprsJframe : aprsSystems) {
            tm.addRow(new Object[]{aprsJframe.getPriority(), aprsJframe.getTaskName(), aprsJframe.getRobotName(), aprsJframe.getDetailsString(), aprsJframe.getPropertiesFile()});
        }
        Utils.autoResizeTableColWidths(jTableTasks);
        Utils.autoResizeTableRowHeights(jTableTasks);
    }

    public void updateRobotsTable() throws IOException {
        Map<String, AprsJFrame> robotMap = new HashMap<>();
        robotEnableMap.clear();
        DefaultTableModel tm = (DefaultTableModel) jTableRobots.getModel();
        tm.setRowCount(0);
        for (AprsJFrame aprsJframe : aprsSystems) {
            robotMap.put(aprsJframe.getRobotName(), aprsJframe);
            robotEnableMap.put(aprsJframe.getRobotName(), true);
        }
        robotMap.forEach((robotName, aprs) -> tm.addRow(new Object[]{robotName, true, aprs.getRobotCrclHost(), aprs.getRobotCrclPort()}));
        Utils.autoResizeTableColWidths(jTableRobots);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AprsMulitSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AprsMulitSupervisorJFrame amsFrame = new AprsMulitSupervisorJFrame();
                amsFrame.startColorTextReader();
                amsFrame.loadPrevSetup();
                amsFrame.loadPrevPosMapFile();
                amsFrame.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.framework.colortextdisplay.ColorTextJPanel colorTextJPanel1;
    private javax.swing.JButton jButtonAddLine;
    private javax.swing.JButton jButtonDeleteLine;
    private javax.swing.JButton jButtonSaveSelectedPosMap;
    private javax.swing.JButton jButtonSetInFromCurrent;
    private javax.swing.JButton jButtonSetOutFromCurrent;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDisableTextPopups;
    private javax.swing.JMenu jMenuActions;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemAddExistingSystem;
    private javax.swing.JMenuItem jMenuItemAddNewSystem;
    private javax.swing.JMenuItem jMenuItemConnectAll;
    private javax.swing.JMenuItem jMenuItemDbgAction;
    private javax.swing.JMenuItem jMenuItemImmediateAbortAll;
    private javax.swing.JMenuItem jMenuItemLoadPosMaps;
    private javax.swing.JMenuItem jMenuItemLoadSetup;
    private javax.swing.JMenuItem jMenuItemRemoveSelectedSystem;
    private javax.swing.JMenuItem jMenuItemSafeAbortAll;
    private javax.swing.JMenuItem jMenuItemSavePosMaps;
    private javax.swing.JMenuItem jMenuItemSaveSetup;
    private javax.swing.JMenuItem jMenuItemSaveSetupAs;
    private javax.swing.JMenuItem jMenuItemStartAll;
    private javax.swing.JMenuItem jMenuItemStartColorTextDisplay;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelPosMapFiles;
    private javax.swing.JPanel jPanelPositionMappings;
    private javax.swing.JPanel jPanelRobots;
    private javax.swing.JPanel jPanelTasks;
    private javax.swing.JPanel jPanelTasksAndRobots;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneRobots;
    private javax.swing.JScrollPane jScrollPaneTasks;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTablePositionMappings;
    private javax.swing.JTable jTableRobots;
    private javax.swing.JTable jTableSelectedPosMapFile;
    private javax.swing.JTable jTableTasks;
    private javax.swing.JTextField jTextFieldSelectedPosMapFilename;
    // End of variables declaration//GEN-END:variables
}
