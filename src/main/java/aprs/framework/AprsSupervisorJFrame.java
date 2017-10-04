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
 * This software can be redistributed and/or modified freely provided show
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.framework;

import static aprs.framework.Utils.runTimeToString;
import aprs.framework.colortextdisplay.ColorTextOptionsJPanel;
import aprs.framework.colortextdisplay.ColorTextOptionsJPanel.ColorTextOptions;
import aprs.framework.colortextdisplay.ColorTextJFrame;
import aprs.framework.colortextdisplay.ColorTextJPanel;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.Slot;
import aprs.framework.pddl.executor.PositionMap;
import aprs.framework.pddl.executor.PositionMapEntry;
import aprs.framework.pddl.executor.PositionMapJPanel;
import aprs.framework.screensplash.SplashScreen;
import com.google.gwt.thirdparty.guava.common.io.Files;
import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import crcl.ui.misc.MultiLineStringJPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsSupervisorJFrame extends javax.swing.JFrame {

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    public AprsSupervisorJFrame() {

        try {
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
                            if (!Objects.equals(enabled, wasEnabled)) {
                                final int fi = i;
                                if (togglesAllowed) {
                                    XFuture.runAsync(() -> {
                                        if (togglesAllowed) {
                                            setRobotEnabled(robotName, enabled);
                                        } else {
                                            javax.swing.SwingUtilities.invokeLater(() -> {
                                                logEvent("Attempt to toggle robot enabled ignored.");
                                                jTableRobots.setValueAt(wasEnabled, fi, 1);
                                            });
                                        }
                                    }, supervisorExecutorService);
                                } else {
                                    logEvent("Attempt to toggle robot enabled ignored.");
                                    jTableRobots.setValueAt(wasEnabled, fi, 1);
                                }
                                break;
                            }
                        }

                    } catch (Exception exception) {
                        log(Level.SEVERE, null, exception);
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
                                && e.getColumn() >= 0
                                && e.getColumn() < 6) {
                            Object obj = jTableSelectedPosMapFile.getValueAt(e.getFirstRow(), e.getColumn());
                            if (obj instanceof Double) {
                                double dval = (double) obj;
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
                }
            });
            crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
            Utils.autoResizeTableColWidths(jTablePositionMappings);
            Utils.autoResizeTableRowHeights(jTablePositionMappings);
            try {
                setIconImage(ImageIO.read(AprsSupervisorJFrame.class
                        .getResource("aprs.png")));

            } catch (Exception ex) {
                Logger.getLogger(AprsJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            updateRobotsTable();
            jListFutures.addListSelectionListener(jTableTasks);
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
        }
        jListFutures.addListSelectionListener(jListFuturesSelectionListener);
        jTreeSelectedFuture.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(
                        tree, value, selected,
                        expanded, leaf, row,
                        hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObject instanceof XFuture) {
                        XFuture xf = (XFuture) userObject;
                        if (!jCheckBoxFutureLongForm.isSelected()) {
                            long runTime = xf.getRunTime();
                            setText(xf.getName() + " (" + (runTime / 1000) + " s) ");
                        } else {
                            setText(xf.toString());
                        }
                        setIcon(null);
                        if (xf.isCancelled()) {
                            setBackground(Color.YELLOW);
                        } else if (xf.isCompletedExceptionally()) {
                            setBackground(Color.RED);
                            ((XFuture<Object>) xf).exceptionally((Throwable t) -> {
                                setText(xf.getName() + " : " + t.toString());
                                if (t instanceof RuntimeException) {
                                    throw ((RuntimeException) t);
                                }
                                throw new RuntimeException(t);
                            });
                        } else if (xf.isDone()) {
                            setBackground(Color.CYAN);
                        } else {
                            setBackground(Color.MAGENTA);
                        }
                        setOpaque(true);
                    } else if (userObject instanceof CompletableFuture) {
                        CompletableFuture cf = (CompletableFuture) userObject;
                        setText(cf.toString());
                        setIcon(null);
                        if (cf.isCancelled()) {
                            setBackground(Color.YELLOW);
                        } else if (cf.isCompletedExceptionally()) {
                            setBackground(Color.RED);
                            ((CompletableFuture<Object>) cf).exceptionally((Throwable t) -> {
                                setText(cf.toString() + " : " + t.toString());
                                if (t instanceof RuntimeException) {
                                    throw ((RuntimeException) t);
                                }
                                throw new RuntimeException(t);
                            });
                        } else if (cf.isDone()) {
                            setBackground(Color.CYAN);
                        } else {
                            setBackground(Color.MAGENTA);
                        }
                        setOpaque(true);
                    }
                }
                return this;
            }
        });
        jListFuturesKey.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String str = value.toString();
                setText(str);
                setIcon(null);
                switch (str) {
                    case "EXCEPTION":
                        setBackground(Color.RED);
                        break;

                    case "CANCELLED":
                        setBackground(Color.YELLOW);
                        break;

                    case "DONE":
                        setBackground(Color.CYAN);
                        break;

                    default:
                        setBackground(Color.MAGENTA);
                        break;
                }
                setOpaque(true);
                return this;
            }
        });
//        TableCellEditor tce = jTableRobots.getCellEditor();
        robotsEnableCelEditorCheckbox = new JCheckBox();
        robotsEnableCelRendererComponent = new JCheckBox();
        jTableRobots.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(robotsEnableCelEditorCheckbox) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (column == 1 && c instanceof JCheckBox) {
                    robotsEnableCelEditorCheckbox = (JCheckBox) c;
                }
                return c;
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return super.isCellEditable(anEvent) && togglesAllowed;
            }
        });
        jTableRobots.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//                Component c =  super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//                if(column == 1) {
//                    robotsEnableCelRendererComponent=  c;
//                }
//                return c;
                robotsEnableCelRendererComponent.setSelected((boolean) value);
                return robotsEnableCelRendererComponent;
            }

        });
        object2DOuterJPanel1.setSlotOffsetProvider(slotOffsetProvider);
    }

    private final ConcurrentHashMap<String, AprsJFrame> slotProvidersMap
            = new ConcurrentHashMap<>();
    private final SlotOffsetProvider slotOffsetProvider = new SlotOffsetProvider() {
        @Override
        public List<Slot> getSlotOffsets(String name) {
            for (int i = 0; i < aprsSystems.size(); i++) {
                try {
                    AprsJFrame sys = aprsSystems.get(i);
                    List<Slot> l = sys.getSlotOffsets(name);
                    if (null != l && !l.isEmpty()) {
                        slotProvidersMap.put(name, sys);
                        return l;
                    }
                } catch (IllegalStateException e) {
                    //ignoring trays that can't be found, must be for another system.
                }
            }
            return null;
        }

        @Override
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
            AprsJFrame sys = slotProvidersMap.get(tray.origName);
            if (null != sys) {
                return sys.absSlotFromTrayAndOffset(tray, offsetItem);
            }
            return null;
        }
    };

    private volatile JCheckBox robotsEnableCelEditorCheckbox = null;
    private volatile JCheckBox robotsEnableCelRendererComponent = null;

    private final ListSelectionListener jListFuturesSelectionListener
            = this::handleListFuturesSelectionEvent;

    private void handleListFuturesSelectionEvent(ListSelectionEvent e) {
        String selectedFutureString = jListFutures.getSelectedValue();
        if (null == selectedFutureString) {
            return;
        }
        switch (selectedFutureString) {
            case "Main":
                futureToDisplaySupplier = () -> mainFuture;
                break;

            case "Last":
                futureToDisplaySupplier = () -> lastFutureReturned;
                break;

            case "Resume":
                futureToDisplaySupplier = () -> resumeFuture.get();
                break;

            case "Random":
                futureToDisplaySupplier = () -> randomTest;
                break;

            case "continousDemo":
                futureToDisplaySupplier = () -> continousDemoFuture;
                break;

            case "stealAbort":
                futureToDisplaySupplier = () -> stealAbortFuture;
                break;

            case "unstealAbort":
                futureToDisplaySupplier = () -> unstealAbortFuture;
                break;
        }
        int sindex = selectedFutureString.indexOf('/');
        if (sindex > 0 && sindex < selectedFutureString.length()) {
            String selectedFutureStringBase = selectedFutureString.substring(0, sindex);
            String selectedFutureStringExt = selectedFutureString.substring(sindex + 1);
            for (AprsJFrame sys : aprsSystems) {
                if (sys.getTaskName().equals(selectedFutureStringBase)) {
                    switch (selectedFutureStringExt) {
                        case "actions":
                            futureToDisplaySupplier = () -> sys.getLastStartActionsFuture();
                            break;

                        case "abort":
                            futureToDisplaySupplier = () -> sys.getSafeAbortFuture();
                            break;

                        case "resume":
                            futureToDisplaySupplier = () -> sys.getLastResumeFuture();
                            break;

                        case "program":
                            futureToDisplaySupplier = () -> sys.getLastRunProgramFuture();
                            break;
                    }
                    return;
                }
            }
        }
        updateCurrentFutureDisplay();
    }

    /**
     * Start a reader so that the text and color of the panels at the bottom
     * right showing the status of the robots can be remotely controlled through
     * a simple socket.
     */
    public void startColorTextReader() {
        this.colorTextJPanel1.startReader();
    }

    /**
     * Get the location of the last CSV Setup file used.
     *
     * @return setup file location
     * @throws IOException setup files location can not be read
     */
    public static File getLastSetupFile() throws IOException {
        if (lastSetupFileFile.exists()) {
            return new File(readFirstLine(lastSetupFileFile));
        }
        return null;
    }

    /**
     * Get the location of the last simulated teach file used. The CSV file
     * contains the name, type and position of objects which can be used to
     * create action lists to fill kits in a similar manner.
     *
     * @return last simulate file location
     * @throws IOException file location can not be read
     */
    public static File getLastSimTeachFile() throws IOException {
        if (lastSimTeachFileFile.exists()) {
            return new File(readFirstLine(lastSimTeachFileFile));
        }
        return null;
    }

    /**
     * Get the location of the last teach properties file used. The properties
     * file contains settings on how to display the teach objects.
     *
     * @return last teach properties file
     * @throws IOException file location can not be read
     */
    public static File getLastTeachPropertiesFile() throws IOException {
        if (lastTeachPropertiesFileFile.exists()) {
            return new File(readFirstLine(lastTeachPropertiesFileFile));
        }
        return null;
    }

    /**
     * Get the location of the last posmap file. The posmap file is a CSV file
     * that points to other csv files with infomation needed to transform
     * coordinates from one robot to another.
     *
     * @return last posmap file location
     * @throws IOException file location can not be read
     */
    public static File getLastPosMapFile() throws IOException {
        if (lastPosMapFileFile.exists()) {
            return new File(readFirstLine(lastPosMapFileFile));
        }
        return null;
    }

    /**
     * Reload the last saved/used setup.
     */
    public void loadPrevSetup() {
        try {
            File setupFile = getLastSetupFile();
            if (null != setupFile && setupFile.exists()) {
                loadSetupFile(setupFile);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, null, ex1);
            }
        }
    }

    /**
     * Load the given simulated teach file. The CSV file contains the name, type
     * and position of objects which can be used to create action lists to fill
     * kits in a similar manner.
     *
     * @param f file to read
     * @throws IOException file can not be read
     */
    public void loadSimTeachFile(File f) throws IOException {
        object2DOuterJPanel1.loadFile(f);
    }

    /**
     * Load the last teach properties file. The properties file contains
     * settings on how to display the teach objects.
     *
     * @param f file to read
     * @throws IOException file location can not be read
     */
    public void loadTeachPropertiesFile(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        object2DOuterJPanel1.loadProperties();
    }

    /**
     * Reload the last simulated teach file read/saved.
     */
    public void loadPrevSimTeach() {
        try {
            File simTeach = getLastSimTeachFile();
            if (null != simTeach && simTeach.exists()) {
                loadSimTeachFile(simTeach);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, null, ex1);
            }
        }
    }

    /**
     * Reload the last teach properties file read/saved.
     */
    public void loadPrevTeachProperties() {
        try {
            File teachProps = getLastTeachPropertiesFile();
            if (null != teachProps && teachProps.exists()) {
                loadTeachPropertiesFile(teachProps);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (Exception ex1) {
                log(Level.SEVERE, null, ex1);
            }
        }
    }

    /**
     * Reload the last posmap file read/saved.
     */
    public void loadPrevPosMapFile() {
        try {
            File posFile = getLastPosMapFile();
            if (null != posFile && posFile.exists()) {
                loadPositionMaps(posFile);
            }
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
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
                            pme.getOffsetX(), pme.getOffsetY(), pme.getOffsetZ(),
                            pme.getLabel()
                        });
                    }
                }
                if (jTableSelectedPosMapFile.getRowCount() > 0) {
                    jTableSelectedPosMapFile.getSelectionModel().setSelectionInterval(0, 0);
                }
            } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                log(Level.SEVERE, null, ex);
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

    private final AtomicReference<XFuture<Void>> stealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<XFuture<Void>> unStealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<XFuture<Void>> cancelStealRobotFuture = new AtomicReference<>(null);
    private final AtomicReference<XFuture<Void>> cancelUnStealRobotFuture = new AtomicReference<>(null);

    private XFuture<Void> checkUnstealRobotFuture(XFuture<Void> future) {
        XFuture<Void> currentUnstealRobotFuture = unStealRobotFuture.get();
        if (null != currentUnstealRobotFuture && currentUnstealRobotFuture != future) {
            final XFuture<Void> newFuture = currentUnstealRobotFuture;
            return newFuture
                    .thenCompose("checkUnStealRobotFuture1", x -> checkUnstealRobotFuture(newFuture));
        } else {
            return XFuture.completedFutureWithName("checkUnstealRobotFuture2", null);
        }
    }

//    private XFuture<Void> checkStealRobotFuture(XFuture<Void> future) {
//        XFuture<Void> currentStealRobotFuture = stealRobotFuture.get();
//        if (null != currentStealRobotFuture && currentStealRobotFuture != future) {
//            final XFuture<Void> newFuture = currentStealRobotFuture;
//            return newFuture
//                    .thenCompose("checkStealRobotFuture1",
//                            x -> checkUnstealRobotFuture(newFuture));
//        } else {
//            return XFuture.completedFutureWithName("checkStealRobotFuture2", null);
//        }
//    }
    private XFuture<?> checkLastReturnedFuture(XFuture<?> inFuture) {
        final XFuture<?> lfr = this.lastFutureReturned;
        if (null != lfr && lfr != inFuture) {
            return lfr
                    .thenCompose("checkLastReturnedFuture1",
                            x -> checkLastReturnedFuture(lfr));
        } else {
            return XFuture.completedFutureWithName("checkLastReturnedFuture2", null);
        }
    }

    private List<XFuture> oldLfrs = new ArrayList<>();

    private final ConcurrentHashMap<String, Integer> robotEnableCountMap
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integer> robotDisableCountMap
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> robotDisableStartMap
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> robotDisableTotalTimeMap
            = new ConcurrentHashMap<>();

    private int getAndIncrementEnableCount(String robotName, boolean enabled) {
        try {
            long time = System.currentTimeMillis();
            if (enabled) {
                robotDisableTotalTimeMap.compute(robotName, (k, v) -> {
                    long diff = time - robotDisableStartMap.getOrDefault(robotName, time);
                    if (null == v) {
                        return diff;
                    } else {
                        return v + diff;
                    }
                });
                return robotEnableCountMap.compute(robotName, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
            } else {
                robotDisableStartMap.put(robotName, time);
                return robotDisableCountMap.compute(robotName, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
            }
        } catch (Exception e) {
            log(Level.SEVERE, "", e);
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void setRobotEnabled(String robotName, Boolean enabled) {
        try {
            long time = System.currentTimeMillis();
            logEvent("setEnabled(" + robotName + "," + enabled + ") : count =" + getAndIncrementEnableCount(robotName, enabled) + ", diff=" + runTimeToString(time - robotDisableStartMap.getOrDefault(robotName, time)) + ", totalTime=" + runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)));
            Utils.runOnDispatchThread(() -> {
                refreshRobotsTable();
            });

            if (null != robotName && null != enabled) {
                robotEnableMap.put(robotName, enabled);
                if (!enabled) {
                    if (stealingRobots || stealRobotFuture.get() != null) {
                        return;
                    }
                    try {
                        final XFuture<Void> origUnstealFuture = unStealRobotFuture.getAndSet(null);;
                        final XFuture<Void> origCancelUnstealFuture = cancelUnStealRobotFuture.getAndSet(null);;
                        try {
                            if (null != origUnstealFuture && null != origCancelUnstealFuture) {
                                logEvent("Cancelling future created at " + getTimeString(origUnstealFuture.getStartTime()) + ", origUnstealFuture = " + origUnstealFuture);
                                origUnstealFuture.cancelAll(true);
                                printStatus(origUnstealFuture, System.out);
                            }
                            final XFuture<Void> future = stealRobot(robotName);
                            if (null == future) {
                                System.err.println(" stealRobot(" + robotName + ") returned null");
                                XFuture<Void> future2 = stealRobot(robotName);
                                throw new IllegalStateException("stealRobot(" + robotName + ") returned null");
                            }
                            logEvent("stealRobotFuture set to " + future);
                            this.stealRobotFuture.set(future);
                            final XFuture<Void> cancelFuture = new XFuture<>("cancelStealRobotFuture");
                            if (!this.cancelStealRobotFuture.compareAndSet(null, cancelFuture)) {
                                throw new IllegalStateException("cancelStealRobotFuture already set.");
                            };
                            lastFutureReturned = XFuture.anyOfWithName("setRobotEnabled(" + robotName + "," + enabled + ").anyOf(steal,cancel)",
                                    future.handle(
                                            "setRobotEnabled(" + robotName + "," + enabled + ").anyOf(steal,cancel).handle",
                                            (Void x, Throwable t) -> {
                                                if (t != null) {
                                                    if (!(t instanceof CancellationException)) {
                                                        log(Level.SEVERE, null, t);
                                                        logEvent(t.toString());
                                                        setAbortTimeCurrent();
                                                        pause();
                                                        Utils.runOnDispatchThread(() -> {
                                                            JOptionPane.showMessageDialog(AprsSupervisorJFrame.this, t);
                                                        });
                                                    }
                                                    return t.toString();
                                                } else {
                                                    return null;
                                                }
                                            })
                                            .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").checkForExceptions",
                                                    x -> {
                                                        if (x == null) {
                                                            return XFuture.completedFutureWithName(
                                                                    "setRobotEnabled(" + robotName + "," + enabled + ").alreadyComplete",
                                                                    null);
                                                        } else {
                                                            return new XFuture<>(x + ".neverComplete");
                                                        }
                                                    }),
                                    cancelFuture);
//                                    .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").cancelFuture.thenCompose.checkUnstealRobotFuture",
//                                    x -> checkUnstealRobotFuture(null)));

                        } finally {
                            if (null != origUnstealFuture && null != origCancelUnstealFuture) {
                                System.out.println("Completing origCancelUnstealFuture= " + origCancelUnstealFuture);
                                origCancelUnstealFuture.complete(null);
                            }
                        }
                    } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                        log(Level.SEVERE, null, ex);
                    }
                } else {

                    if (!stealingRobots || unStealRobotFuture.get() != null) {
                        return;
                    }
                    stealingRobots = false;
                    XFuture prevLFR = lastFutureReturned;
                    final XFuture<Void> origStealFuture = stealRobotFuture.getAndSet(null);
                    final XFuture<Void> origCancelStealFuture = cancelStealRobotFuture.getAndSet(null);
                    try {
                        if (null != origStealFuture && null != origCancelStealFuture) {
                            logEvent("Cancelling future created at " + getTimeString(origStealFuture.getStartTime()) + ", origStealFuture = " + origStealFuture);
                            origStealFuture.cancelAll(true);
                            System.out.println("origStealFuture = " + origStealFuture);
                            printStatus(origStealFuture, System.out);
                            System.out.println("lastFutureReturned = " + lastFutureReturned);
                            printStatus(lastFutureReturned, System.out);
                        }
                        final XFuture<Void> future = unStealRobots();
                        if (null == future) {
                            throw new IllegalStateException("unstealRobots() returned null");
                        }
                        logEvent("unStealRobotFuture set to " + future);
                        this.unStealRobotFuture.set(future);
                        final XFuture<Void> cancelFuture = new XFuture<>("cancelUnStealRobotFuture");
                        if (!this.cancelUnStealRobotFuture.compareAndSet(null, cancelFuture)) {
                            throw new IllegalStateException("cancelUnStealRobotFuture already set.");
                        }
                        lastFutureReturned = XFuture.anyOfWithName("setRobotEnabled(" + robotName + "," + enabled + ").anyOf(unsteal,cancel)",
                                future.handle("setRobotEnabled(" + robotName + "," + enabled + ").handle1",
                                        (Void x, Throwable t) -> {
                                            if (t == null) {
                                                return (String) null;
                                            } else {
                                                if (!(t instanceof CancellationException)) {
                                                    logEvent(t.toString());
                                                    log(Level.SEVERE, null, t);
                                                    setAbortTimeCurrent();
                                                    pause();
                                                    Utils.runOnDispatchThread(() -> {
                                                        JOptionPane.showMessageDialog(AprsSupervisorJFrame.this, t);
                                                    });
                                                }
                                                return t.toString();
                                            }
                                        })
                                        .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").handle2",
                                                x -> {
                                                    if (x == null) {
                                                        return XFuture.completedFutureWithName("setRobotEnabled(" + robotName + "," + enabled + ").completedFuture2", null);
                                                    } else {
                                                        return new XFuture<>(x + ".neverComplete");
                                                    }
                                                }),
                                cancelFuture);
                        //.thenCompose(x -> checkStealRobotFuture(null)));
                    } finally {
                        if (null != origStealFuture && null != origCancelStealFuture) {
                            System.out.println("Completing origCancelStealFuture= " + origCancelStealFuture);
                            origCancelStealFuture.complete(null);
                            System.out.println("origCancelStealFuture = " + origCancelStealFuture);
                            printStatus(origCancelStealFuture, System.out);
                            if (origCancelStealFuture.isCompletedExceptionally()) {
                                origCancelStealFuture.exceptionally(t -> {
                                    t.printStackTrace();
                                    if (t.getCause() != null) {
                                        t.getCause().printStackTrace();
                                    }
                                    return null;
                                });
                            }
                        }
                    }
                    System.out.println("prevLFR = " + prevLFR);
                    printStatus(prevLFR, System.out);
//                this.unStealRobotFuture = unStealRobots();
//                this.cancelUnStealRobotFuture = new XFuture<>();
//                final XFuture<Void> origStealFuture = stealRobotFuture;
//                    lastFutureReturned = XFuture.anyOfWithName(this.unStealRobotFuture, 
//                            this.cancelUnStealRobotFuture.thenCompose(x -> checkUnstealRobotFuture(stealRobotFuture)));
//                 lastFutureReturned = XFuture.anyOfWithName(this.stealRobotFuture, 
//                            this.cancelStealRobotFuture.thenCompose(x -> (null != unStealRobotFuture)?unStealRobotFuture:XFuture.completedFuture(null)));
                }
                oldLfrs.add(lastFutureReturned);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "", e);
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void refreshRobotsTable() {
        DefaultTableModel model = (DefaultTableModel) jTableRobots.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            model.setValueAt(robotEnableMap.get(robotName), i, 1);
            model.setValueAt(robotDisableCountMap.getOrDefault(robotName, 0), i, 4);
            model.setValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 5);
        }
        Utils.autoResizeTableColWidths(jTableRobots);
    }

    private void setAbortTimeCurrent() {
        abortEventTime = System.currentTimeMillis();
    }

    private XFuture<Void> stealRobot(String robotName) throws IOException, PositionMap.BadErrorMapFormatException {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < aprsSystems.size() - 1; i++) {
            AprsJFrame sys = aprsSystems.get(i);
            if (null != sys) {
                names.add(sys.getRobotName());
                if (Objects.equals(sys.getRobotName(), robotName)) {
                    return stealRobot(aprsSystems.get(i + 1), aprsSystems.get(i));
                }
            }
        }
        String errMsg = "Robot " + robotName + " not found in " + names;
        System.out.println("aprsSystems = " + aprsSystems);
        System.err.println(errMsg);
        showErrorSplash(errMsg);
        throw new IllegalStateException(errMsg);
//        return XFuture.completedFutureWithName("stealRobot(" + robotName + ").completedFuture", null);
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

    private static class NamedCallable<T> implements Callable<T> {

        private final Callable<T> callable;
        private final String name;
        private final AprsJFrame[] systems;

        public Callable<T> getCallable() {
            return callable;
        }

        public String getName() {
            return name;
        }

        public AprsJFrame[] getSystems() {
            return systems;
        }

        public NamedCallable(Callable<T> r, String name, AprsJFrame... systems) {
            this.callable = r;
            this.name = name;
            this.systems = systems;
            assert (r != null) : "NamedRunnable: Runnable r == null";
        }

        @Override
        public String toString() {
            return "NamedRunnable{" + "r=" + callable + ", name=" + name + '}';
        }

        @Override
        public T call() throws Exception {
            return callable.call();
        }
    }

    private final AtomicReference<NamedCallable<XFuture<?>>> returnRobotRunnable = new AtomicReference<>();

    private <T> NamedCallable<T> setReturnRobotRunnable(String name, Callable<T> r, AprsJFrame... systems) {
        NamedCallable namedR = new NamedCallable(r, name, systems);
        returnRobotRunnable.set(namedR);
        return namedR;
    }

    private void checkRobotsUniquePorts() {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            if (sys.isConnected()) {
                int port = sys.getRobotCrclPort();
                if (set.contains(port)) {
                    debugAction();
                    setAbortTimeCurrent();
                    pause();
                    String msg = "two systems connected to " + port;
                    logEvent(msg);
                    throw new IllegalStateException(msg);
                }
                set.add(port);
            }
        }
    }

    private volatile StackTraceElement returnRobotsStackTrace[] = null;
    private volatile Thread returnRobotsThread = null;
    private volatile long returnRobotsTime = -1;

    private void printReturnRobotTraceInfo() {
        System.out.println("returnRobotsThread = " + returnRobotsThread);
        System.out.println("returnRobotsStackTrace = " + Arrays.toString(returnRobotsStackTrace));
        System.out.println("returnRobotsTime = " + (returnRobotsTime - System.currentTimeMillis()));
    }

    private XFuture<?> returnRobots(String comment) {
        return returnRobots(returnRobotRunnable.getAndSet(null), comment);
    }

    private XFuture<?> returnRobotsDirect(String comment) {
        return returnRobotsDirect(returnRobotRunnable.getAndSet(null), comment);
    }
    private AtomicInteger returnRobotsNumber = new AtomicInteger();

    private XFuture<?> returnRobots(NamedCallable<XFuture<?>> r, String comment) {
        checkRobotsUniquePorts();
        this.stealingRobots = false;
        if (r != null) {
            returnRobotsThread = Thread.currentThread();
            returnRobotsTime = System.currentTimeMillis();
            returnRobotsStackTrace = returnRobotsThread.getStackTrace();
            String blockerName = "returnRobots" + returnRobotsNumber.incrementAndGet();
            disallowToggles(blockerName, r.getSystems());
            logEvent(r.name+", comment="+comment);

            return XFuture.supplyAsync(r.name, r, supervisorExecutorService)
                    .thenCompose(x -> x)
                    .alwaysAsync(() -> allowToggles(blockerName), supervisorExecutorService);
        } else {
            logReturnRobotsNullRunnable(comment);
            return XFuture.completedFuture(null);
        }
    }

    private void logReturnRobotsNullRunnable(String comment) {
        switch(comment) {
            case "prepActions":
            case "enableAndCheckAllRobots":
                break;
                
            default:
                logEvent("returnRobots: runnable=null,comment="+comment);
        }
    }

    private XFuture<?> returnRobotsDirect(NamedCallable<XFuture<?>> r, String comment) {
        checkRobotsUniquePorts();
        this.stealingRobots = false;
        if (r != null) {
            try {
                returnRobotsThread = Thread.currentThread();
                returnRobotsTime = System.currentTimeMillis();
                returnRobotsStackTrace = returnRobotsThread.getStackTrace();
                String blockerName = "returnRobotsDirect" + returnRobotsNumber.incrementAndGet();
                disallowToggles(blockerName, r.getSystems());
                logEvent(r.name +", comment="+comment);
                return r.call()
                        .alwaysAsync(() -> allowToggles(blockerName), supervisorExecutorService);
            } catch (Exception ex) {
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                XFuture<?> ret = new XFuture<>("returnRobotsDirect." + comment);
                ret.completeExceptionally(ex);
                return ret;
            }
        } else {
            logReturnRobotsNullRunnable(comment);
            return XFuture.completedFuture(null);
        }
    }

    private final AtomicReference<Supplier<XFuture<Void>>> unStealRobotsSupplier = new AtomicReference<>(null);

    private XFuture<Void> unStealRobots() {
        Supplier<XFuture<Void>> supplier = unStealRobotsSupplier.getAndSet(null);
        if (null == supplier) {
            return XFuture.completedFutureWithName("unStealRobots.null==supplier", null);
        }
        return supplier.get();
    }

    private XFuture<Void> showCheckEnabledErrorSplash() {

        return showErrorSplash("Not all robots\n could be enabled.")
                .thenRun(() -> {
                    Utils.runOnDispatchThread(() -> {
                        jCheckBoxMenuItemContinousDemo.setSelected(false);
                        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
                        if (null != continousDemoFuture) {
                            continousDemoFuture.cancelAll(true);
                            continousDemoFuture = null;
                        }
                    });
                });
    }

    private XFuture<Void> showErrorSplash(String errMsgString) {
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        return showMessageFullScreen(errMsgString, 80.0f,
                null,
                SplashScreen.getRedYellowColorList(), gd);
    }

    private XFuture<Void> stealAbortFuture = null;
    private XFuture<Void> unstealAbortFuture = null;

    private volatile boolean stealingRobots = false;

    /**
     * Show a message in full screen mode with flashing colors. (It is intended
     * to be visible and attention grabbing across the room.) Note: there is a
     * checkbox in the menu that can be used to disable these messages, in which
     * case the message will only be logged to the events tab.
     *
     * @param message string to display
     * @param fontSize font size
     * @param image image to show under text
     * @param colors colors to flash in order
     * @param graphicsDevice device to display on
     * @return future that can be used to take action after the message has been
     * shown
     */
    public XFuture<Void> showMessageFullScreen(String message, float fontSize, Image image, List<Color> colors, GraphicsDevice graphicsDevice) {

        if (jCheckBoxMenuItemShowSplashMessages.isSelected()) {
            return forceShowMessageFullScreen(message, fontSize, image, colors, graphicsDevice);
        } else {
            logEvent("ignoring showMessageFullScreen " + message.replace('\n', ' '));
            return XFuture.completedFutureWithName("jCheckBoxMenuItemShowSplashMessages.isSelected()== false", null);
        }
    }

    private XFuture<Void> forceShowMessageFullScreen(String message, float fontSize, Image image, List<Color> colors, GraphicsDevice graphicsDevice) {
        logEvent("showMessageFullScreen " + message.replace('\n', ' '));
        return SplashScreen.showMessageFullScreen(message, fontSize, image, colors, graphicsDevice);
    }

    private final AtomicInteger stealRobotNumber = new AtomicInteger();
    private final AtomicInteger reverseRobotTransferNumber = new AtomicInteger();

    private XFuture<Void> stealRobot(AprsJFrame stealFrom, AprsJFrame stealFor) throws IOException, PositionMap.BadErrorMapFormatException {

        final int srn = stealRobotNumber.incrementAndGet();
        logEvent("Transferring " + stealFrom.getRobotName() + " to " + stealFor.getTaskName() + " : srn=" + srn);
        String blocker = "stealRobot" + srn;
        disallowToggles(blocker, stealFrom, stealFor);
        XFuture<Void> origStealRobotFuture = stealRobotFuture.get();
        if (origStealRobotFuture != null) {
            System.out.println("calling stealrRobot when already stealingRobots");
            return origStealRobotFuture;
        }

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
        NamedCallable returnRobot = setupReturnRobots(srn, stealFor, stealFrom, stealForOptions, pm);

        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();

        setupUnstealRobots(srn, stealFor, stealFrom, stealForRobotName, gd);
        stealingRobots = true;
        logEvent("Starting safe abort and disconnect for " + " : srn=" + srn + " " + stealFor);
        logEvent("    and starting safe abort and disconnect for " + " : srn=" + srn + " " + stealFrom);
        stealAbortFuture = XFuture.allOfWithName("stealAbortAllOf",
                stealFrom.startSafeAbortAndDisconnect("stealAbortAllOf.stealFrom" + " : srn=" + srn)
                        .thenRunAsync(() -> logEvent("Safe abort and disconnect completed for " + stealFrom + " " + stealFromRobotName + " needed for " + stealFor + " : srn=" + srn), supervisorExecutorService),
                stealFor.startSafeAbortAndDisconnect("stealAbortAllOf.stealFor" + " : srn=" + srn)
                        .thenComposeAsync("showDisabledMessage." + stealForRobotName,
                                x -> {
                                    logEvent("Safe abort and disconnect completed for " + stealFor + ", " + stealForRobotName + " being disabled. " + " : srn=" + srn);
                                    if (null != colorTextSocket) {
                                        try {
                                            colorTextSocket.getOutputStream().write("0xFF0000, 0x00FF00\r\n".getBytes());
                                        } catch (IOException ex) {
                                            log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    return showMessageFullScreen(stealForRobotName + "\n Disabled", 80.0f,
                                            SplashScreen.getDisableImageImage(),
                                            SplashScreen.getRedYellowColorList(), gd);
                                }, supervisorExecutorService));

        XFuture<Boolean> part1 = stealAbortFuture.thenComposeAsync(
                "transfer" + " : srn=" + srn, x -> {
                    logEvent("transfer : " + stealFor + " connectRobot(" + stealFromRobotName + "," + stealFromOrigCrclHost + "," + stealFromOrigCrclPort + ")" + " : srn=" + srn);
                    stealFor.addPositionMap(pm);
                    for (String opt : transferrableOptions) {
                        if (stealFromOptions.containsKey(opt)) {
                            stealFor.setExecutorOption(opt, stealFromOptions.get(opt));
                        }
                    }
                    return stealFor.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);
                },
                supervisorExecutorService)
                .thenCompose(
                        "showSwitching" + " : srn=" + srn, x -> {
                            return showMessageFullScreen("Switching to \n" + stealFromRobotName, 80.0f,
                                    SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                        }
                )
                .alwaysAsync(() -> {
                    allowToggles(blocker, stealFor);
                }, supervisorExecutorService)
                //                .thenRun(
                //                        () -> this.allowToggles())
                .thenComposeAsync("continueAfterSwitch" + " : srn=" + srn, x -> {
                    int curSrn = stealRobotNumber.get();
                    if (srn != curSrn) {
                        logEvent("continueAfterSwitch srn=" + srn + ", curSrn=" + curSrn);
                        return XFuture.completedFutureWithName("continueAfterSwitch.srn != stealRobotNumber.get()" + " : srn=" + srn, null);
                    }
                    if (stealFor.isAborting()) {
                        logEvent("continueAfterSwitch stealFor.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFor=" + stealFrom);
                        return XFuture.completedFutureWithName("continueAfterSwitch.stealFor.isAborting()" + " : srn=" + srn, null);
                    }
                    logEvent("Continue actions after switch for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn=" + srn);
                    return stealFor.continueActionList("stealFor.continueAfterSwitch" + " : srn=" + srn)
                            .thenComposeAsync(x4 -> {
                                logEvent("continueAfterSwitch " + stealFor.getRunName() + " completed action list after robot switch " + x4 + " : srn=" + srn);
                                return finishAction(stealFor)
                                        .thenApply(x5 -> {
                                            logEvent("finish continueAfterSwitch " + stealFor.getRunName() + " completed action list " + x4 + " : srn=" + srn);
                                            if (x4) {
                                                completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag());
                                            }
                                            return x4;
                                        });
                            }, supervisorExecutorService);
                }, supervisorExecutorService
                );

        if (jCheckBoxMenuItemIndContinousDemo.isSelected()
                || jCheckBoxMenuItemIndRandomToggleTest.isSelected()) {
            return part1
                    .thenRunAsync("stealRobot :  Checking systemContinueMap " + " : srn=" + srn,
                            () -> {
                                logEvent("completing stealRobot: stealingRobots=" + stealingRobots + ", stealFor=" + stealFor + ",srn=" + srn + ", stealRobotNumber=" + stealRobotNumber.get() + "robotEnableMap.get(" + stealForRobotName + ")=" + robotEnableMap.get(stealForRobotName));
                                if (stealingRobots && srn == stealRobotNumber.get() && !robotEnableMap.get(stealForRobotName)) {
                                    completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag());
                                }
                            }, supervisorExecutorService);
        }
        
        
        return part1.thenCompose(
                "startSafeAbortAfterSwitch" + " : srn=" + srn, x -> {
                   return stealFor.startSafeAbort("startSafeAbortAfterSwitch" + " : srn=" + srn)
                            .thenRun(() -> logEvent("Safe abort completed for " + stealFor + " : srn=" + srn));
                })
                .thenComposeAsync(
                        "showReturning" + " : srn=" + srn, x -> {
                            return showMessageFullScreen("Returning \n" + stealFromRobotName, 80.0f,
                                    SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                        }, supervisorExecutorService
                )
                .thenComposeAsync(
                        "returnRobots2" + " : srn=" + srn, x -> returnRobotsDirect("returnRobots2" + " : srn=" + srn),
                        supervisorExecutorService)
                .thenCompose("continueAfterReturn" + " : srn=" + srn, x -> {
                    logEvent("Continue actions for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn=" + srn);
                    return stealFrom.continueActionList("stealFrom.continueAfterReturn" + " : srn=" + srn)
                            .thenComposeAsync((Boolean x5) -> {
                                logEvent("stealFrom.continueAfterReturn " + stealFrom.getRunName() + " completed action list after return " + x5 + " : srn=" + srn);
                                return finishAction(stealFrom)
                                        .thenApply((Void x6) -> {
                                            logEvent("finish stealFrom.continueAfterReturn " + stealFrom.getRunName() + " completed action list " + x5 + " : srn=" + srn);
                                            if (x5) {
                                                completeSystemsContinueIndFuture(stealFrom, !stealFrom.isReverseFlag());
                                            }
                                            return ((Void) null);
                                        });
                            }, supervisorExecutorService);
                });
    }

    private NamedCallable setupReturnRobots(final int srn, AprsJFrame stealFor, AprsJFrame stealFrom, Map<String, String> stealForOptions, PositionMap pm) {
        String stealFromOrigCrclHost = stealFrom.getRobotCrclHost();
        int stealFromOrigCrclPort = stealFrom.getRobotCrclPort();
        String stealFromRobotName = stealFrom.getRobotName();

        String stealForOrigCrclHost = stealFor.getRobotCrclHost();
        int stealForOrigCrclPort = stealFor.getRobotCrclPort();
        String stealForRobotName = stealFor.getRobotName();
        NamedCallable returnRobot = setReturnRobotRunnable("Return  : srn=" + srn + " " + stealForRobotName + "-> " + stealFor.getTaskName() + " , " + stealFromRobotName + "->" + stealFrom.getTaskName(),
                () -> {
                    if (stealFor.isPaused()) {
                        logEvent(stealFor.getTaskName() + " is paused when trying to return robot" + " : srn=" + srn);
                    }
                    if (stealFrom.isPaused()) {
                        logEvent(stealFrom.getTaskName() + " is paused when trying to return robot" + " : srn=" + srn);
                    }
                    if (Objects.equals(stealFrom.getRobotName(), stealFromRobotName)) {
                        logEvent(stealFromRobotName + " already assigned to " + stealFrom + " : srn=" + srn);
                    }
                    if (Objects.equals(stealFor.getRobotName(), stealForRobotName)) {
                        logEvent(stealForRobotName + " already assigned to " + stealFor + " : srn=" + srn);
                    }
                    if (Objects.equals(stealFrom.getRobotName(), stealFromRobotName)
                    && Objects.equals(stealFor.getRobotName(), stealForRobotName)) {
                        return XFuture.completedFutureWithName("returnRobot.alreadyReturned" + " : srn=" + srn, null);
                    }
                    if (stealFor.isRunningCrclProgram()) {
                        String msg = stealFor.getTaskName() + " is running crcl program when trying to return robot" + " : srn=" + srn;
                        logEvent(msg);
                        throw new IllegalStateException(msg);
                    }
                    if (stealFrom.isRunningCrclProgram()) {
                        String msg = stealFrom.getTaskName() + " is running crcl program when trying to return robot" + " : srn=" + srn;
                        logEvent(msg);
                        throw new IllegalStateException(msg);
                    }
                    if (stealFor.isDoingActions()) {
                        String msg = stealFor.getTaskName() + " is doing actions when trying to return robot" + " : srn=" + srn;
                        logEvent(msg);
                        throw new IllegalStateException(msg);
                    }
                    if (stealFrom.isDoingActions()) {
                        String msg = stealFrom.getTaskName() + " is doing actions  program when trying to return robot" + " : srn=" + srn;
                        logEvent(msg);
                        throw new IllegalStateException(msg);
                    }
                    logEvent("Disconnect robot from " + stealFor);
                    return stealFor.disconnectRobot()
                            .thenComposeAsync(
                                    "returnRobot." + stealFrom.getTaskName() + " connect to " + stealFromRobotName + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn,
                                    x -> {
                                        logEvent(stealForRobotName + " disconnnected from " + stealFor + " : srn=" + srn);
                                        logEvent("start returnRobot." + stealFrom.getTaskName() + " connect to " + stealFromRobotName + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn);
                                        return stealFrom.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);
                                    }, supervisorExecutorService)
                            .thenComposeAsync("returnRobot.transferOption",
                                    x -> {
                                        logEvent(stealFrom.getTaskName() + " connected to " + stealFromRobotName + " at " + stealFromOrigCrclHost + ":" + stealFromOrigCrclPort + " : srn=" + srn);
                                        for (String opt : transferrableOptions) {
                                            if (stealForOptions.containsKey(opt)) {
                                                stealFor.setExecutorOption(opt, stealForOptions.get(opt));
                                            }
                                        }
                                        stealFor.removePositionMap(pm);
                                        logEvent("start returnRobot." + stealFor.getTaskName() + " connect to " + stealForRobotName + " at " + stealForOrigCrclHost + ":" + stealForOrigCrclPort + " : srn=" + srn);
                                        return stealFor.connectRobot(stealForRobotName, stealForOrigCrclHost, stealForOrigCrclPort);
                                    }, supervisorExecutorService)
                            .thenRun(() -> {
                                logEvent(stealFor.getTaskName() + " connected to " + stealForRobotName + " at " + stealForOrigCrclHost + ":" + stealForOrigCrclPort + " : srn=" + srn);
                                checkRobotsUniquePorts();
                            });
                }, stealFor, stealFrom);
        return returnRobot;
    }

    private void setupUnstealRobots(final int srn, AprsJFrame stealFor, AprsJFrame stealFrom, String stealForRobotName, final GraphicsDevice gd) {
        unStealRobotsSupplier.set(() -> {
            return executeUnstealRobots(srn, stealFor, stealFrom, stealForRobotName, gd);
        });
    }

    private XFuture<Void> executeUnstealRobots(final int srn, AprsJFrame stealFor, AprsJFrame stealFrom, String stealForRobotName, final GraphicsDevice gd) {
        String revBlocker = "reverseRobotTransfer" + reverseRobotTransferNumber.incrementAndGet();
        logEvent("Reversing robot transfer after robot reenabled." + " : srn=" + srn);
        disallowToggles(revBlocker, stealFor, stealFrom);
        stealingRobots = false;
        logEvent("Starting safe abort and disconnect for " + stealFor + " : srn=" + srn);
        logEvent("    and starting safe abort and disconnect for" + stealFrom + " : srn=" + srn);
        unstealAbortFuture = XFuture.allOfWithName("unStealAbortAllOf",
                stealFrom.startSafeAbortAndDisconnect("unStealAbortAllOf.stealFrom" + stealFrom + " : srn=" + srn)
                        .thenRunAsync(() -> logEvent("Safe abort and disconnect completed for " + stealFrom + " : srn=" + srn), supervisorExecutorService),
                stealFor.startSafeAbortAndDisconnect("unStealAbortAllOf.stealFor " + stealFor + " : srn=" + srn)
                        .thenComposeAsync("unstealShowReenable", x -> {
                            logEvent("Safe abort and disconnect completed for " + stealFor + " : srn=" + srn);
                            boolean stillAborting = stealFor.isAborting();
                            if (stillAborting) {
                                String msg = "still aborting after safe abort and disconnect" + " : srn=" + srn;
                                logEvent(msg);
                                boolean doubleCheck = stealFor.isAborting();
                                throw new IllegalStateException(msg);
                            }
                            if (null != colorTextSocket) {
                                try {
                                    colorTextSocket.getOutputStream().write("0x00FF00, 0x00FF00\r\n".getBytes());
                                } catch (IOException ex) {
                                    log(Level.SEVERE, null, ex);
                                }
                            }
                            return showMessageFullScreen(stealForRobotName + "\n Enabled", 80.0f,
                                    SplashScreen.getDisableImageImage(),
                                    SplashScreen.getBlueWhiteGreenColorList(), gd);
                        }, supervisorExecutorService));
        return unstealAbortFuture
                .thenComposeAsync("unsteal.returnRobots1" + " : srn=" + srn, x -> {
                    return returnRobotsDirect("unsteal.returnRobots1" + " : srn=" + srn);
                }, supervisorExecutorService)
                .thenRun("unsteal.connectAll" + " : srn=" + srn, () -> connectAll())
                .alwaysAsync(() -> {
                    allowToggles(revBlocker, stealFor, stealFrom);
                }, supervisorExecutorService)
                .thenCompose("unsteal.continueAllOf" + " : srn=" + srn, x -> {
                    int curSrn = stealRobotNumber.get();
                    if (srn != curSrn) {
                        logEvent("unsteal.continueAllOf srn=" + srn + ", curSrn=" + curSrn);
                        return XFuture.completedFutureWithName("unsteal.continueAllOf.srn != stealRobotNumber.get()" + " : srn=" + srn, null);
                    }
                    if (stealFrom.isAborting()) {
                        logEvent("unsteal.continueAllOf stealFrom.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFrom=" + stealFrom);
                        return XFuture.completedFutureWithName("unsteal.continueAllOf.stealFrom.isAborting()" + " : srn=" + srn, null);
                    }
                    if (stealFor.isAborting()) {
                        logEvent("unsteal.continueAllOf stealFor.isAborting() : srn=" + srn + ", curSrn=" + curSrn + ", stealFor=" + stealFrom);
                        return XFuture.completedFutureWithName("unsteal.continueAllOf.stealFor.isAborting()" + " : srn=" + srn, null);
                    }
                    logEvent("unsteal.continueAllOf Continue actions for " + stealFrom.getTaskName() + " with " + stealFrom.getRobotName() + " : srn=" + srn);
                    logEvent("unsteal.continueAllOf Continue actions for " + stealFor.getTaskName() + " with " + stealFor.getRobotName() + " : srn=" + srn);
                    return XFuture.allOf(stealFrom.continueActionList("unsteal.stealFrom" + " : srn=" + srn)
                                    .thenComposeAsync(x2 -> {
                                        logEvent("unsteal.stealFrom " + stealFrom.getRunName() + " completed action list after return after robot reenabled. " + x2 + " : srn=" + srn);

                                        return finishAction(stealFrom)
                                                .thenApply(x3 -> {
                                                    logEvent("finish unsteal.stealFrom " + stealFrom.getRunName() + " completed action list " + x2 + " : srn=" + srn);
                                                    if (x2 && !stealFrom.isAborting() && srn == stealRobotNumber.get()) {
                                                        completeSystemsContinueIndFuture(stealFrom, !stealFrom.isReverseFlag());
                                                    }
                                                    return x2;
                                                });
                                    }, supervisorExecutorService),
                            stealFor.continueActionList("unsteal.stealFor" + " : srn=" + srn)
                                    .thenComposeAsync(x3 -> {
                                        logEvent("unsteal.stealFor " + stealFor.getRunName() + " completed action list after return after robot reenabled. " + x3 + " : srn=" + srn);
                                        return finishAction(stealFrom)
                                                .thenApply(x4 -> {
                                                    logEvent("finish unsteal.stealFor " + stealFor.getRunName() + " completed action list " + x3 + " : srn=" + srn);
                                                    if (x3 && !stealFor.isAborting() && srn == stealRobotNumber.get()) {
                                                        completeSystemsContinueIndFuture(stealFor, !stealFor.isReverseFlag());
                                                    }
                                                    return x3;
                                                });
                                    }, supervisorExecutorService)
                    );
                });
    }

    private String assertFail() {
        logEvent("assertFail");
        pause();
        return "";
    }

    private void completeSystemsContinueIndFuture(AprsJFrame sys, boolean value) {
        assert (null != sys) : assertFail() + "sys == null : sys=" + sys;
        assert (sys.getRobotName() != null) : assertFail() + "sys.getRobotName() == null: sys=" + sys;
        assert (sys.getRobotName().length() > 0) : assertFail() + "sys.getRobotName().length() <= 0 : sys=" + sys;
        assert (sys.isConnected()) : assertFail() + "!sys.isConnected() : sys=" + sys;
        assert (!sys.isAborting()) : assertFail() + "sys.isAborting() : sys=" + sys;
        checkRobotsUniquePorts();
        logEvent("Checking systemContinueMap for " + sys);
        AtomicReference<XFuture<Boolean>> ref = new AtomicReference<>();
        XFuture<Boolean> f = systemContinueMap.replace(sys.getMyThreadId(),
                new XFuture("systemContinueMap." + sys));
        if (null != f) {
            logEvent("Completing " + f + " with " + value + " for " + sys);
            f.complete(value);
        }
    }

    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * Convert a timestamp in milliseconds since 1970 to the default time string
     * format.
     *
     * @param ms timestamp in milliseconds
     * @return formatted string
     */
    public static String getTimeString(long ms) {
        Date date = new Date(ms);
        return timeFormat.format(date);
    }

    private volatile PrintStream logPrintStream = null;

    private int eventsDisplayMax = 500;

    /**
     * Get the value of eventsDisplayMax
     *
     * @return the value of eventsDisplayMax
     */
    public int getEventsDisplayMax() {
        return eventsDisplayMax;
    }

    /**
     * Set the value of eventsDisplayMax
     *
     * @param eventsDisplayMax new value of eventsDisplayMax
     */
    public void setEventsDisplayMax(int eventsDisplayMax) {
        this.eventsDisplayMax = eventsDisplayMax;
    }

    volatile javax.swing.Timer runTimeTimer = null;

    volatile int maxEventStringLen = 0;

    private void logEventPrivate(long time, String s, int blockerSize, String threadname) {
        DefaultTableModel tm = (DefaultTableModel) jTableEvents.getModel();
        if (tm.getRowCount() > eventsDisplayMax) {
            tm.removeRow(0);
            maxEventStringLen = 0;
        }
        if (firstEventTime > 0) {
            updateRunningTime();
            startUpdateRunningTimeTimer();
        }
        String timeString = getTimeString(time);
        if (null == logPrintStream) {
            try {
                File logFile = Utils.createTempFile("events_log_", ".txt");
                System.out.println("logFile = " + logFile.getCanonicalPath());
                logPrintStream = new PrintStream(new FileOutputStream(logFile));

            } catch (IOException ex) {
                Logger.getLogger(AprsSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        String fullLogString = timeString + " \t" + blockerSize + " \t" + s + " \t:thread= " + threadname;
        logPrintStream.println(fullLogString);
        System.out.println(fullLogString);
        tm.addRow(new Object[]{getTimeString(time), blockerSize, s, threadname});
        if (tm.getRowCount() % 50 < 2 || s.length() > maxEventStringLen) {
            Utils.autoResizeTableColWidths(jTableEvents);
            maxEventStringLen = s.length();
        } else {
            scrollToEnd(jTableEvents);
        }
    }

    private void startUpdateRunningTimeTimer() {
        if (runTimeTimer == null) {
            runTimeTimer = new Timer(2000, x -> updateRunningTime());
            runTimeTimer.start();
        }
    }

    private void updateRunningTime() {
        if (firstEventTime > 0 && !jCheckBoxMenuItemPause.isSelected()) {

            long runningTimeMillis = System.currentTimeMillis() - firstEventTime;
            if (firstEventTime < abortEventTime) {
                runningTimeMillis = abortEventTime - firstEventTime;
            }
            String s = runTimeToString(runningTimeMillis);
            jTextFieldRunningTime.setText(s);
        }
    }

    private void scrollToEnd(JTable jTable) {
        int index = jTable.getRowCount() - 1;
        jTable.getSelectionModel().setSelectionInterval(index, index);
        jTable.scrollRectToVisible(new Rectangle(jTable.getCellRect(index, 0, true)));
    }

    private volatile long firstEventTime = -1;
    private volatile long abortEventTime = -1;

    /**
     * Log an event string to be displayed with timestamp in event table.
     *
     * @param s string to log
     */
    public void logEvent(String s) {
        long t = System.currentTimeMillis();
        if (firstEventTime < 0) {
            firstEventTime = t;
        }
        int blockersSize = toggleBlockerMap.keySet().size();
        String threadname = Thread.currentThread().getName();
        Utils.runOnDispatchThread(() -> logEventPrivate(t, s, blockersSize, threadname));
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
    private final static File lastSimTeachFileFile = new File(System.getProperty("aprsLastMultiSystemSimTeachFile", System.getProperty("user.home") + File.separator + ".lastAprsSimTeachFile.txt"));
    private final static File lastTeachPropertiesFileFile = new File(System.getProperty("aprsLastMultiSystemTeachPropertiesFile", System.getProperty("user.home") + File.separator + ".lastAprsTeachPropertiesFile.txt"));
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
        jLabel6 = new javax.swing.JLabel();
        jTextFieldRobotEnableToggleBlockers = new javax.swing.JTextField();
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
        jPanelFuture = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListFutures = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTreeSelectedFuture = new javax.swing.JTree();
        jCheckBoxUpdateFutureAutomatically = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jListFuturesKey = new javax.swing.JList<>();
        jCheckBoxShowDoneFutures = new javax.swing.JCheckBox();
        jCheckBoxShowUnnamedFutures = new javax.swing.JCheckBox();
        jButtonFuturesCancelAll = new javax.swing.JButton();
        jCheckBoxFutureLongForm = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTableEvents = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldEventsMax = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldRunningTime = new javax.swing.JTextField();
        jPanelTeachTable = new javax.swing.JPanel();
        object2DOuterJPanel1 = new aprs.framework.simview.Object2DOuterJPanel();
        jComboBoxTeachSystemView = new javax.swing.JComboBox<>();
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
        jMenuItemSaveAll = new javax.swing.JMenuItem();
        jMenuActions = new javax.swing.JMenu();
        jMenuItemStartAll = new javax.swing.JMenuItem();
        jMenuItemSafeAbortAll = new javax.swing.JMenuItem();
        jMenuItemImmediateAbortAll = new javax.swing.JMenuItem();
        jMenuItemContinueAll = new javax.swing.JMenuItem();
        jMenuItemConnectAll = new javax.swing.JMenuItem();
        jMenuItemStartAllReverse = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemContinousDemoRevFirst = new javax.swing.JCheckBoxMenuItem();
        jMenuItemScanAll = new javax.swing.JMenuItem();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jMenuActionsAdditionalTests = new javax.swing.JMenu();
        jCheckBoxMenuItemRandomTest = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPauseResumeTest = new javax.swing.JCheckBoxMenuItem();
        jMenuItemResetAll = new javax.swing.JMenuItem();
        jMenuItemDbgAction = new javax.swing.JMenuItem();
        jCheckBoxMenuItemRandomTestReverseFirst = new javax.swing.JMenuItem();
        jCheckBoxMenuItemIndContinousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemIndRandomToggleTest = new javax.swing.JCheckBoxMenuItem();
        jMenuItemRunCustom = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenu();
        jCheckBoxMenuItemDisableTextPopups = new javax.swing.JCheckBoxMenuItem();
        jMenuItemStartColorTextDisplay = new javax.swing.JMenuItem();
        jCheckBoxMenuItemDebugStartReverse = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemShowSplashMessages = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemFixedRandomTestSeed = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPauseAllForOne = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemContDemoReverseFirst = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemUseTeachCamera = new javax.swing.JCheckBoxMenuItem();

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
                "Robot", "Enabled", "Host", "Port", "Disable Count", "Disable Time"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableRobots.setRowHeight(30);
        jScrollPaneRobots.setViewportView(jTableRobots);

        jLabel6.setText("Robot Enable Toggles Blocked by: ");

        javax.swing.GroupLayout jPanelRobotsLayout = new javax.swing.GroupLayout(jPanelRobots);
        jPanelRobots.setLayout(jPanelRobotsLayout);
        jPanelRobotsLayout.setHorizontalGroup(
            jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.DEFAULT_SIZE, 591, Short.MAX_VALUE)
                    .addGroup(jPanelRobotsLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRobotEnableToggleBlockers)))
                .addContainerGap())
        );
        jPanelRobotsLayout.setVerticalGroup(
            jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldRobotEnableToggleBlockers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                { new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0),  new Double(0.0), null}
            },
            new String [] {
                "Xin", "Yin", "Zin", "Xout", "Yout", "Zout", "Offset_X", "Offset_Y", "Offset_Z", "Label"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, true, true, true, true, false, false, false, true
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
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
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

        jLabel1.setText("Futures");

        jListFutures.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Main" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(jListFutures);

        jLabel2.setText("Details");

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTreeSelectedFuture.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane4.setViewportView(jTreeSelectedFuture);

        jCheckBoxUpdateFutureAutomatically.setText("Update Automatically");
        jCheckBoxUpdateFutureAutomatically.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxUpdateFutureAutomaticallyActionPerformed(evt);
            }
        });

        jLabel3.setText("Key:");

        jListFuturesKey.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "CANCELLED", "DONE", "EXCEPTION", "WORKING" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane5.setViewportView(jListFuturesKey);

        jCheckBoxShowDoneFutures.setText("Show Completed");
        jCheckBoxShowDoneFutures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowDoneFuturesActionPerformed(evt);
            }
        });

        jCheckBoxShowUnnamedFutures.setText("Show Unnamed");
        jCheckBoxShowUnnamedFutures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowUnnamedFuturesActionPerformed(evt);
            }
        });

        jButtonFuturesCancelAll.setText("Cancel All");
        jButtonFuturesCancelAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFuturesCancelAllActionPerformed(evt);
            }
        });

        jCheckBoxFutureLongForm.setText("Long Form");
        jCheckBoxFutureLongForm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxFutureLongFormActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelFutureLayout = new javax.swing.GroupLayout(jPanelFuture);
        jPanelFuture.setLayout(jPanelFutureLayout);
        jPanelFutureLayout.setHorizontalGroup(
            jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFutureLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addComponent(jScrollPane5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(jPanelFutureLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                        .addComponent(jCheckBoxShowUnnamedFutures)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxShowDoneFutures)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxUpdateFutureAutomatically)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxFutureLongForm)
                        .addGap(91, 91, 91)
                        .addComponent(jButtonFuturesCancelAll)))
                .addContainerGap())
        );
        jPanelFutureLayout.setVerticalGroup(
            jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFutureLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jCheckBoxUpdateFutureAutomatically)
                    .addComponent(jCheckBoxShowDoneFutures)
                    .addComponent(jCheckBoxShowUnnamedFutures)
                    .addComponent(jButtonFuturesCancelAll)
                    .addComponent(jCheckBoxFutureLongForm))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
                    .addGroup(jPanelFutureLayout.createSequentialGroup()
                        .addComponent(jScrollPane3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jTabbedPane2.addTab("Futures", jPanelFuture);

        jTableEvents.setAutoCreateRowSorter(true);
        jTableEvents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Time", "Locks", "Event", "Thread"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane6.setViewportView(jTableEvents);

        jLabel4.setText("Max: ");

        jTextFieldEventsMax.setText("500         ");
        jTextFieldEventsMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldEventsMaxActionPerformed(evt);
            }
        });

        jLabel5.setText("Running Time : ");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 1049, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldEventsMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRunningTime)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldEventsMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jTextFieldRunningTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Events", jPanel2);

        jComboBoxTeachSystemView.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxTeachSystemView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxTeachSystemViewActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelTeachTableLayout = new javax.swing.GroupLayout(jPanelTeachTable);
        jPanelTeachTable.setLayout(jPanelTeachTableLayout);
        jPanelTeachTableLayout.setHorizontalGroup(
            jPanelTeachTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTeachTableLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTeachTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1049, Short.MAX_VALUE)
                    .addComponent(jComboBoxTeachSystemView, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelTeachTableLayout.setVerticalGroup(
            jPanelTeachTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTeachTableLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboBoxTeachSystemView, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Teach", jPanelTeachTable);

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

        jMenuItemSaveAll.setText("Save All ... ");
        jMenuItemSaveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAllActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveAll);

        jMenuBar1.add(jMenuFile);

        jMenuActions.setText("Actions");
        jMenuActions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuActionsActionPerformed(evt);
            }
        });

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

        jMenuItemContinueAll.setText("Continue All");
        jMenuItemContinueAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemContinueAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemContinueAll);

        jMenuItemConnectAll.setText("Connect All");
        jMenuItemConnectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemConnectAll);

        jMenuItemStartAllReverse.setText("Start All Reverse");
        jMenuItemStartAllReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartAllReverseActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemStartAllReverse);

        jCheckBoxMenuItemContinousDemo.setText("Continous Demo");
        jCheckBoxMenuItemContinousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinousDemoActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemContinousDemo);

        jCheckBoxMenuItemContinousDemoRevFirst.setText("Continous Demo (Reverse first)");
        jCheckBoxMenuItemContinousDemoRevFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinousDemoRevFirstActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemContinousDemoRevFirst);

        jMenuItemScanAll.setText("Scan All");
        jMenuItemScanAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemScanAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemScanAll);

        jCheckBoxMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAUSE, 0));
        jCheckBoxMenuItemPause.setText("Pause");
        jCheckBoxMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemPause);

        jMenuActionsAdditionalTests.setText("Additional Tests ");

        jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continous Demo");
        jCheckBoxMenuItemRandomTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemRandomTestActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jCheckBoxMenuItemRandomTest);

        jCheckBoxMenuItemPauseResumeTest.setText("Pause Resume Test");
        jCheckBoxMenuItemPauseResumeTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseResumeTestActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jCheckBoxMenuItemPauseResumeTest);

        jMenuItemResetAll.setText("Reset All");
        jMenuItemResetAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemResetAllActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jMenuItemResetAll);

        jMenuItemDbgAction.setText("Dbg Action");
        jMenuItemDbgAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDbgActionActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jMenuItemDbgAction);

        jCheckBoxMenuItemRandomTestReverseFirst.setText("Randomized Enable Toggle Continous Demo (Reverse First) ");
        jCheckBoxMenuItemRandomTestReverseFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemRandomTestReverseFirstActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jCheckBoxMenuItemRandomTestReverseFirst);

        jCheckBoxMenuItemIndContinousDemo.setText("(Independant) Continous Demo");
        jCheckBoxMenuItemIndContinousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIndContinousDemoActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jCheckBoxMenuItemIndContinousDemo);

        jCheckBoxMenuItemIndRandomToggleTest.setText("(Independant) Continous Demo With Randomized Enable Toggle    ");
        jCheckBoxMenuItemIndRandomToggleTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIndRandomToggleTestActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jCheckBoxMenuItemIndRandomToggleTest);

        jMenuItemRunCustom.setText("Run custom code");
        jMenuItemRunCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRunCustomActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jMenuItemRunCustom);

        jMenuActions.add(jMenuActionsAdditionalTests);

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

        jCheckBoxMenuItemDebugStartReverse.setText("Debug Start Reverse");
        jCheckBoxMenuItemDebugStartReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDebugStartReverseActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemDebugStartReverse);

        jCheckBoxMenuItemShowSplashMessages.setSelected(true);
        jCheckBoxMenuItemShowSplashMessages.setText("Show Full Screeen  Messages");
        jMenuOptions.add(jCheckBoxMenuItemShowSplashMessages);

        jCheckBoxMenuItemFixedRandomTestSeed.setText("Fixed Random Test Seed ... ");
        jCheckBoxMenuItemFixedRandomTestSeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemFixedRandomTestSeedActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemFixedRandomTestSeed);

        jCheckBoxMenuItemPauseAllForOne.setSelected(true);
        jCheckBoxMenuItemPauseAllForOne.setText("Pause All when One System Pauses");
        jMenuOptions.add(jCheckBoxMenuItemPauseAllForOne);

        jCheckBoxMenuItemContDemoReverseFirst.setText("Reverse First for Continous Demo");
        jMenuOptions.add(jCheckBoxMenuItemContDemoReverseFirst);

        jCheckBoxMenuItemUseTeachCamera.setSelected(true);
        jCheckBoxMenuItemUseTeachCamera.setText("Use Teach Camera");
        jMenuOptions.add(jCheckBoxMenuItemUseTeachCamera);

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

    /**
     * Query the user to select a file to save setup data in.
     */
    public void browseSaveSetupAs() {
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
                log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jMenuItemLoadSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadSetupActionPerformed
        browseOpenSetup();
    }//GEN-LAST:event_jMenuItemLoadSetupActionPerformed

    /**
     * Query the user to select a setup file to read.
     */
    public void browseOpenSetup() {
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
                log(Level.SEVERE, null, ex);
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
            } catch (Exception ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemAddExistingSystemActionPerformed

    /**
     * Add a system to show and update the tasks and robots tables.
     * @param sys system to add
     */
    public void addAprsSystem(AprsJFrame sys)  {
        sys.setPriority(aprsSystems.size() + 1);
        sys.setVisible(true);
        sys.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        sys.setSupervisorEventLogger(this::logEvent);
        aprsSystems.add(sys);
        sys.getTitleUpdateRunnables().add(() -> {
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
                    log(Level.SEVERE, null, ex);
                }
                updateTasksTable();
                updateRobotsTable();
                saveCurrentSetup();
            } catch (Exception ex) {
                log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemRemoveSelectedSystemActionPerformed

    private volatile XFuture<?> lastFutureReturned = null;

    private void prepAndFinishOnDispatch(Runnable r) {

        prepActions()
                .thenRun(() -> {
                    Utils.runOnDispatchThread(() -> {
                        try {
                            r.run();
                        } catch (Exception e) {
                            log(Level.SEVERE, "", e);
                        }
                    });
                });
    }


    private void jMenuItemStartAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllActionPerformed

        prepAndFinishOnDispatch(() -> {
            immediateAbortAll();
            clearEventLog();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            lastFutureReturned = startAll();
            mainFuture = lastFutureReturned;
        });
    }//GEN-LAST:event_jMenuItemStartAllActionPerformed

    private XFuture<?> prepActions() {
        boolean origIgnoreFlag = ignoreTitleErrors.getAndSet(true);
        if (null != lastSafeAbortAllFuture) {
            lastSafeAbortAllFuture.cancelAll(true);
            lastSafeAbortAllFuture = null;
        }
        if (null != lastSafeAbortAllFuture2) {
            lastSafeAbortAllFuture2.cancelAll(true);
            lastSafeAbortAllFuture2 = null;
        }
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
            lastFutureReturned = null;
        }
        firstEventTime = -1;
        XFuture xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            xf.cancelAll(true);
        }
        clearAllToggleBlockers();
        clearAllErrors();
        jCheckBoxMenuItemPause.setSelected(false);
        immediateAbortAll(true);
        resumeForPrepOnly();
        if (!origIgnoreFlag) {
            ignoreTitleErrors.set(false);
        }
        return returnRobots("prepActions");
    }

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
                log(Level.SEVERE, null, ex);
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

    /**
     * Query the user to select a posmap file to read. The posmap file is a CSV file
     * that points to other csv files with infomation needed to transform
     * coordinates from one robot to another.
     */
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
                log(Level.SEVERE, null, ex);
            }
        }
    }

    private volatile XFuture<Void> lastSafeAbortAllFuture = null;
    private volatile XFuture<Void> lastSafeAbortAllFuture2 = null;

    private void jMenuItemSafeAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSafeAbortAllActionPerformed
        XFuture<Void> f = safeAbortAll();
        lastSafeAbortAllFuture = f;
        lastSafeAbortAllFuture2 = f.thenRun(() -> {
            final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
            immediateAbortAll();
            forceShowMessageFullScreen("Safe Abort Complete", 80.0f,
                    SplashScreen.getRobotArmImage(),
                    SplashScreen.getBlueWhiteGreenColorList(), gd);
            fullAbortAll();
        });
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        jCheckBoxMenuItemPauseResumeTest.setSelected(false);
        jCheckBoxMenuItemPause.setSelected(false);
        jCheckBoxMenuItemIndContinousDemo.setSelected(false);
        jCheckBoxMenuItemIndRandomToggleTest.setSelected(false);
        mainFuture = lastSafeAbortAllFuture2;
    }//GEN-LAST:event_jMenuItemSafeAbortAllActionPerformed

    private final AtomicBoolean ignoreTitleErrors = new AtomicBoolean(false);

    private void jMenuItemImmediateAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortAllActionPerformed
        fullAbortAll();
    }//GEN-LAST:event_jMenuItemImmediateAbortAllActionPerformed

    private void fullAbortAll() {
        ignoreTitleErrors.set(true);
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
            lastFutureReturned = null;
        }
        if (null != lastSafeAbortAllFuture) {
            lastSafeAbortAllFuture.cancelAll(true);
            lastSafeAbortAllFuture = null;
        }
        if (null != lastSafeAbortAllFuture2) {
            lastSafeAbortAllFuture2.cancelAll(true);
            lastSafeAbortAllFuture2 = null;
        }
        XFuture xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            xf.cancelAll(true);
        }
        clearAllToggleBlockers();
        clearAllErrors();
        immediateAbortAll();
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        jCheckBoxMenuItemPauseResumeTest.setSelected(false);
        jCheckBoxMenuItemPause.setSelected(false);
        jCheckBoxMenuItemIndContinousDemo.setSelected(false);
        jCheckBoxMenuItemIndRandomToggleTest.setSelected(false);
        mainFuture = lastFutureReturned;
        ignoreTitleErrors.set(false);
    }

    private void jButtonSetInFromCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetInFromCurrentActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            if (null != posMapInSys) {
                PoseType pose = posMapInSys.getCurrentPose();
                if (null != pose) {
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX(), row, 0);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY(), row, 1);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ(), row, 2);
                    double otherx = (double) jTableSelectedPosMapFile.getValueAt(row, 3);
                    double othery = (double) jTableSelectedPosMapFile.getValueAt(row, 4);
                    double otherz = (double) jTableSelectedPosMapFile.getValueAt(row, 5);
                    jTableSelectedPosMapFile.setValueAt(otherx - pose.getPoint().getX(), row, 6);
                    jTableSelectedPosMapFile.setValueAt(othery - pose.getPoint().getY(), row, 7);
                    jTableSelectedPosMapFile.setValueAt(otherz - pose.getPoint().getZ(), row, 8);
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
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX(), row, 3);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY(), row, 4);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ(), row, 5);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX() - otherx, row, 6);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY() - othery, row, 7);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ() - otherz, row, 8);
                }
            }
        }
    }//GEN-LAST:event_jButtonSetOutFromCurrentActionPerformed

    private void jButtonAddLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddLineActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTableSelectedPosMapFile.getModel();
        model.addRow(new Object[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "label" + model.getRowCount()});
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
            log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonSaveSelectedPosMapActionPerformed

    private void jCheckBoxMenuItemDisableTextPopupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed

    private void jMenuItemDbgActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDbgActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDbgActionActionPerformed

    private static void printStatus(AtomicReference<XFuture<Void>> ref, PrintStream ps) {
        if (null != ref) {
            XFuture<?> xf = ref.get();
            printStatus(xf, ps);
        }
    }

    private static void printStatus(XFuture<?> xf, PrintStream ps) {
        if (null != xf) {
            xf.printStatus(ps);
        }
    }

    private final AtomicInteger debugActionCount = new AtomicInteger();

    private void debugAction() {

        System.out.println("");
        System.err.println("");
        int count = debugActionCount.incrementAndGet();
        System.out.println("Begin AprsSupervisorJFrame.debugAction()" + count);
        System.out.println("waitForTogglesFutures = " + waitForTogglesFutures);
        System.out.println("togglesAllowed = " + togglesAllowed);
        System.out.println("disallowTogglesCount = " + disallowTogglesCount);
        System.out.println("allowTogglesCount = " + allowTogglesCount);
        System.out.println("waitForTogglesFutureCount = " + waitForTogglesFutureCount);

        System.out.println("stealingRobots = " + stealingRobots);
        System.out.println("returnRobotRunnable = " + returnRobotRunnable);

        System.out.println("lastFutureReturned = " + lastFutureReturned);
        printStatus(lastFutureReturned, System.out);
        System.out.println("continousDemoFuture = " + continousDemoFuture);
        printStatus(continousDemoFuture, System.out);

        System.out.println("randomTest = " + randomTest);
        printStatus(randomTest, System.out);

        System.out.println("togglesAllowedXfuture = " + togglesAllowedXfuture);
        printStatus(togglesAllowedXfuture, System.out);

        System.out.println("stealRobotFuture = " + stealRobotFuture);
        printStatus(stealRobotFuture, System.out);

        System.out.println("unStealRobotFuture = " + unStealRobotFuture);
        printStatus(unStealRobotFuture, System.out);
        System.out.println("cancelStealRobotFuture = " + cancelStealRobotFuture);
        printStatus(cancelStealRobotFuture, System.out);

        System.out.println("cancelUnStealRobotFuture = " + cancelUnStealRobotFuture);
        printStatus(cancelUnStealRobotFuture, System.out);

        System.out.println("stealAbortFuture = " + stealAbortFuture);
        printStatus(stealAbortFuture, System.out);

        System.out.println("unstealAbortFuture = " + unstealAbortFuture);
        printStatus(unstealAbortFuture, System.out);

        System.out.println("oldLfrs = " + oldLfrs);
        for (int i = 0; i < oldLfrs.size(); i++) {
            XFuture xf = oldLfrs.get(i);
            if (!xf.isDone() || xf.isCancelled() || xf.isCompletedExceptionally()) {
                System.out.println("oldLfrs.get(" + i + ") = " + xf);
                printStatus(xf, System.out);
            }
        }

        System.out.println("lastStartAllActionsArray = " + lastStartAllActionsArray);
        if (null != lastStartAllActionsArray) {
            for (int i = 0; i < lastStartAllActionsArray.length; i++) {
                XFuture xf = lastStartAllActionsArray[i];
                if (!xf.isDone() || xf.isCancelled() || xf.isCompletedExceptionally()) {
                    System.out.println("oldLfrs.get(" + i + ") = " + xf);
                    printStatus(xf, System.out);
                }
            }
        }

        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).debugAction();
        }

        printReturnRobotTraceInfo();

        System.out.println("End AprsSupervisorJFrame.debugAction()" + count);
        System.out.println("");
        System.err.println("");

    }

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
                log(Level.SEVERE, null, ex);
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
                log(Level.SEVERE, null, ex);
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
                log(Level.SEVERE, null, ex);
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
        } catch (Exception ex) {
            log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemAddNewSystemActionPerformed

    private volatile XFuture<?> continousDemoFuture = null;
    private volatile XFuture<?> mainFuture = null;

    private void jCheckBoxMenuItemContinousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinousDemoActionPerformed
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll();
            clearEventLog();
            clearAllErrors();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            continousDemoCycle.set(0);
            if (jCheckBoxMenuItemContinousDemo.isSelected()) {
                continousDemoFuture = startContinousDemo();
                mainFuture = continousDemoFuture;
            }
        });
    }//GEN-LAST:event_jCheckBoxMenuItemContinousDemoActionPerformed

    private void jMenuActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuActionsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuActionsActionPerformed

    private final AtomicReference<XFuture<Void>> resumeFuture = new AtomicReference<>(null);

    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            clearAllErrors();
            resume();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseActionPerformed

    private volatile XFuture<Void> randomTest = null;

    private void clearEventLog() {
        abortEventTime = -1;
        firstEventTime = -1;
        ((DefaultTableModel) jTableEvents.getModel()).setRowCount(0);
    }


    private void jCheckBoxMenuItemRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRandomTestActionPerformed
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll();
            clearEventLog();
            clearAllErrors();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            continousDemoCycle.set(0);
            randomTestCount.set(0);
            if (jCheckBoxMenuItemRandomTest.isSelected()) {
                startRandomTest();
            }

        });
    }//GEN-LAST:event_jCheckBoxMenuItemRandomTestActionPerformed


    private void jMenuItemStartAllReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllReverseActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll();
            connectAll();
            setAllReverseFlag(true);
            enableAllRobots();
            lastFutureReturned = startReverseActions();
            mainFuture = lastFutureReturned;

        });
    }//GEN-LAST:event_jMenuItemStartAllReverseActionPerformed

    private void jMenuItemResetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetAllActionPerformed
        resetAll();
    }//GEN-LAST:event_jMenuItemResetAllActionPerformed

    /**
     * Reset all systems, clearing errors, resetting states to defaults and reloading simulation files.
     */
    public void resetAll() {
        resetAll(true);
    }

    /**
     * Reset all systems, clearing errors, resetting states to defaults and optionally reloading simulation files.
     * @param reloadSimFiles whether to reload simulation files
     */
    public void resetAll(boolean reloadSimFiles) {
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
            lastFutureReturned = null;
        }
        if (null != randomTest) {
            randomTest.cancelAll(true);
            randomTest = null;
        }
        if (null != continousDemoFuture) {
            continousDemoFuture.cancelAll(true);
            continousDemoFuture = null;
        }
        immediateAbortAll();
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        jCheckBoxMenuItemPause.setSelected(false);
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).reset(reloadSimFiles);
        }
    }

    private void jCheckBoxMenuItemPauseResumeTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed
        prepAndFinishOnDispatch(() -> {
            if (null != continousDemoFuture) {
                continousDemoFuture.cancelAll(true);
                continousDemoFuture = null;
            }
            if (null != randomTest) {
                randomTest.cancelAll(true);
                randomTest = null;
            }
            if (null != pauseTest) {
                pauseTest.cancelAll(true);
                pauseTest = null;
            }
            if (null != lastFutureReturned) {
                lastFutureReturned.cancelAll(true);
                lastFutureReturned = null;
            }
            immediateAbortAll();
            clearAllErrors();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            continousDemoCycle.set(0);
            randomTestCount.set(0);
            jCheckBoxMenuItemContinousDemo.setSelected(false);
            jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
            jCheckBoxMenuItemRandomTest.setSelected(false);
            if (jCheckBoxMenuItemPauseResumeTest.isSelected()) {
                jCheckBoxMenuItemContinousDemo.setSelected(true);
                jCheckBoxMenuItemRandomTest.setSelected(true);
                continousDemoFuture = startContinousDemo();
                randomTest = continueRandomTest();
                pauseTest = continuePauseTest();
                resetMainPauseTestFuture();
            }

        });
    }//GEN-LAST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed

    int resetMainPauseCount = 0;

    private void resetMainPauseTestFuture() {
        resetMainPauseCount++;
        mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainPauseTestFuture" + resetMainPauseCount, continousDemoFuture, randomTest, pauseTest);
    }

    private void jCheckBoxMenuItemDebugStartReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugStartReverseActionPerformed
        debugStartReverseActions = jCheckBoxMenuItemDebugStartReverse.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDebugStartReverseActionPerformed

    private void jMenuItemContinueAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueAllActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        prepAndFinishOnDispatch(() -> {
            if (null != randomTest) {
                randomTest.cancelAll(true);
                randomTest = null;
            }
            if (null != pauseTest) {
                pauseTest.cancelAll(true);
                pauseTest = null;
            }
            if (null != continousDemoFuture) {
                continousDemoFuture.cancelAll(true);
                continousDemoFuture = null;
            }
            immediateAbortAll();
            jCheckBoxMenuItemRandomTest.setSelected(false);
            jCheckBoxMenuItemPause.setSelected(false);
            resume();
            lastFutureReturned = continueAll();
            if (jCheckBoxMenuItemContinousDemo.isSelected()) {
                continousDemoFuture
                        = lastFutureReturned
                                .thenCompose("jMenuItemContinueAllActionPerformed.continueAllActions",
                                        x -> continueAllActions());
                mainFuture = continousDemoFuture;
            }

        });
    }//GEN-LAST:event_jMenuItemContinueAllActionPerformed

    private void jCheckBoxMenuItemContinousDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinousDemoRevFirstActionPerformed
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll();
            clearEventLog();
            clearAllErrors();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            continousDemoCycle.set(0);
            if (jCheckBoxMenuItemContinousDemoRevFirst.isSelected()) {
                continousDemoFuture = startContinuousDemoRevFirst();
                mainFuture = continousDemoFuture;
            }

        });
    }//GEN-LAST:event_jCheckBoxMenuItemContinousDemoRevFirstActionPerformed

    private void jMenuItemScanAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemScanAllActionPerformed
        prepAndFinishOnDispatch(() -> {
            lastFutureReturned = startScanAll();
            mainFuture = lastFutureReturned;
        });
    }//GEN-LAST:event_jMenuItemScanAllActionPerformed

    private void jCheckBoxUpdateFutureAutomaticallyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxUpdateFutureAutomaticallyActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay();
        }
    }//GEN-LAST:event_jCheckBoxUpdateFutureAutomaticallyActionPerformed

    private void jCheckBoxMenuItemRandomTestReverseFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRandomTestReverseFirstActionPerformed
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll();
            resetAll();
            clearAllErrors();
            connectAll();
            setAllReverseFlag(false);
            enableAllRobots();
            continousDemoCycle.set(0);
            randomTestCount.set(0);
            if (jCheckBoxMenuItemRandomTest.isSelected()) {
                startRandomTest();
            }

        });
    }//GEN-LAST:event_jCheckBoxMenuItemRandomTestReverseFirstActionPerformed

    int resetMainRandomTestCount = 0;
    //     private volatile XFuture<?> resetMainAny = null;
    //     private volatile XFuture<?> resetMainRandom = null;
    //     private volatile XFuture<?> resetMainContDemo = null;

    private void resetMainRandomTestFuture() {
        assert (randomTest != null) : "(randomTest == null)";
        assert (continousDemoFuture != null) : "(continousDemoFuture == null)";

        resetMainRandomTestCount++;
        mainFuture = (XFuture<?>) XFuture.allOfWithName("resetMainRandomTestFuture" + resetMainRandomTestCount, randomTest, continousDemoFuture);
        mainFuture.exceptionally((thrown) -> {
            if (thrown != null) {
                log(Level.SEVERE, "", thrown);
            }
            if (thrown instanceof RuntimeException) {
                throw (RuntimeException) thrown;
            }
            throw new RuntimeException(thrown);
        });
//        resetMainRandom = randomTest;
//        resetMainContDemo = continousDemoFuture;
//        resetMainAny = XFuture.anyOfWithName("resetMainAny",resetMainRandom, resetMainContDemo);
//        resetMainAny.thenRun(this::resetMainRandomTestFuture);
    }

    private void jCheckBoxShowUnnamedFuturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowUnnamedFuturesActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay();
        }
    }//GEN-LAST:event_jCheckBoxShowUnnamedFuturesActionPerformed

    private void jCheckBoxShowDoneFuturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowDoneFuturesActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay();
        }
    }//GEN-LAST:event_jCheckBoxShowDoneFuturesActionPerformed

    private void jButtonFuturesCancelAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFuturesCancelAllActionPerformed
        XFuture future = futureToDisplaySupplier.get();
        future.cancelAll(true);
    }//GEN-LAST:event_jButtonFuturesCancelAllActionPerformed

    private void jTextFieldEventsMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldEventsMaxActionPerformed
        setEventsDisplayMax(Integer.parseInt(jTextFieldEventsMax.getText().trim()));
    }//GEN-LAST:event_jTextFieldEventsMaxActionPerformed

    private void jCheckBoxMenuItemIndContinousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemIndContinousDemoActionPerformed
        try {
            ignoreTitleErrors.set(true);
            jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
            jCheckBoxMenuItemContinousDemo.setSelected(false);
            prepAndFinishOnDispatch(() -> {
                immediateAbortAll();
                clearEventLog();
                clearAllErrors();
                connectAll();
                setAllReverseFlag(jCheckBoxMenuItemContDemoReverseFirst.isSelected());
                enableAllRobots();
                continousDemoCycle.set(0);
                if (jCheckBoxMenuItemIndContinousDemo.isSelected()) {
                    continousDemoFuture = startIndependentContinousDemo();
                    mainFuture = continousDemoFuture;
                }
            });
        } finally {
            ignoreTitleErrors.set(false);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemIndContinousDemoActionPerformed

    private void jCheckBoxMenuItemIndRandomToggleTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemIndRandomToggleTestActionPerformed
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemIndContinousDemo.setSelected(false);
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll(true)
                    .thenRun(() -> {
                        clearEventLog();
                        clearAllErrors();
                        connectAll();
                        setAllReverseFlag(false);
                        enableAllRobots();
                        continousDemoCycle.set(0);
                        if (jCheckBoxMenuItemFixedRandomTestSeed.isSelected()) {
                            random = new Random(randomTestSeed);
                        } else {
                            random = new Random(System.currentTimeMillis());
                        }
                        if (jCheckBoxMenuItemIndRandomToggleTest.isSelected()) {
                            continousDemoFuture = startRandomEnableToggleIndependentContinousDemo();
                            mainFuture = continousDemoFuture;
                        }
                    });
        });
    }//GEN-LAST:event_jCheckBoxMenuItemIndRandomToggleTestActionPerformed

    private int randomTestSeed = 959;
    private void jCheckBoxMenuItemFixedRandomTestSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemFixedRandomTestSeedActionPerformed
        if (jCheckBoxMenuItemFixedRandomTestSeed.isSelected()) {
            randomTestSeed = Integer.parseInt(JOptionPane.showInputDialog("Fixed Seed", randomTestSeed));
            jCheckBoxMenuItemFixedRandomTestSeed.setText("Fixed Random Test Seed (" + randomTestSeed + ") ... ");
        }
    }//GEN-LAST:event_jCheckBoxMenuItemFixedRandomTestSeedActionPerformed

    private void jCheckBoxFutureLongFormActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxFutureLongFormActionPerformed
        updateCurrentFutureDisplay();
    }//GEN-LAST:event_jCheckBoxFutureLongFormActionPerformed

    private void jMenuItemSaveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveAllActionPerformed
        try {
            Map<String, String> filesMapIn = new HashMap<>();
            File f = getLastSetupFile();
            String dirName = (null != f) ? f.getParentFile().getCanonicalPath() : System.getProperty("user.home");
            filesMapIn.put("Setup", (null != f) ? f.getCanonicalPath() : dirName + File.separator + "setup.txt");
            f = getLastPosMapFile();
            filesMapIn.put("PosMap", (null != f) ? f.getCanonicalPath() : dirName + File.separator + "posmap.csv");
            f = getLastSimTeachFile();
            filesMapIn.put("SimTeach", (null != f) ? f.getCanonicalPath() : dirName + File.separator + "simTeach.csv");
            f = getLastTeachPropertiesFile();
            filesMapIn.put("TeachProps", (null != f) ? f.getCanonicalPath() : dirName + File.separator + "teachProps.txt");
            Map<String, String> filesMapOut = MultiFileDialogJPanel.showMultiFileDialog(this, "Save All ...", true, filesMapIn);
            if (null != filesMapOut) {
                String setup = filesMapOut.get("Setup");
                if (null != setup) {
                    saveSetupFile(new File(setup));
                }
                String mapsFile = filesMapOut.get("PosMap");
                if (null != mapsFile) {
                    savePositionMaps(new File(mapsFile));
                }

                String simTeach = filesMapOut.get("SimTeach");
                if (null != simTeach) {
                    saveSimTeach(new File(simTeach));
                }

                String teachProps = filesMapOut.get("TeachProps");
                if (null != teachProps) {
                    saveTeachProps(new File(teachProps));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSaveAllActionPerformed

    private void jComboBoxTeachSystemViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxTeachSystemViewActionPerformed
        try {
            String sysString = (String) jComboBoxTeachSystemView.getSelectedItem();
            if (null == sysString || sysString.equals("All")) {
                setTeachSystemFilter(null);
            } else {
                int id = Integer.parseInt(sysString.trim().split("[ \t:]+")[0]);
                for (AprsJFrame sys : aprsSystems) {
                    if (sys.getMyThreadId() == id) {
                        setTeachSystemFilter(sys);
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, exception);
        }
    }//GEN-LAST:event_jComboBoxTeachSystemViewActionPerformed

    /**
     * Get the first system with a task name that starts with the given string.
     * @param s name/prefix of task to look for
     * @return system with given task
     */
    public AprsJFrame getSysByTask(String s) {
        for (AprsJFrame sys : aprsSystems) {
            if (sys.getTaskName().startsWith(s)) {
                return sys;
            }
        }
        return null;
    }

    private final String INIT_CUSTOM_CODE = "package custom;\n"
            + "import aprs.framework.*; \n"
            + "import java.util.function.Consumer;\n\n"
            + "public class Custom\n\timplements Consumer<AprsSupervisorJFrame> {\n"
            + "\tpublic void accept(AprsSupervisorJFrame sup) {\n"
            + "\t\t// PUT YOUR CODE HERE:\n"
            + "\t\tSystem.out.println(\"sys = \"+sup.getSysByTask(\"Fanuc Cart\"));"
            + "\t}\n"
            + "}\n";

    private String customCode = INIT_CUSTOM_CODE;

    private void jMenuItemRunCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRunCustomActionPerformed
        runCustomCode();
    }//GEN-LAST:event_jMenuItemRunCustomActionPerformed

    private void runCustomCode() {
        try {

            customCode = MultiLineStringJPanel.editText(customCode);
            File customDir = Paths.get(System.getProperty("user.home"), ".aprs", "custom").toFile();
            customDir.delete();
            customDir.mkdirs();
            File tmpFile = new File(customDir, "Custom.java");
            System.out.println("tmpFile = " + tmpFile.getCanonicalPath());
            File[] files1 = {tmpFile};

            Files.write(customCode.getBytes(), tmpFile);
            java.util.function.Consumer c = null;
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (null != compiler) {
                ClassLoader cl = ClassLoader.getSystemClassLoader();

                URL[] origUrls = ((URLClassLoader) cl).getURLs();

                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

                Iterable<? extends JavaFileObject> compilationUnits1
                        = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files1));
                String classPath = Arrays.stream(origUrls)
                        .map(Objects::toString)
                        .map(s -> s.startsWith("file:") ? s.substring(4) : s)
                        .collect(Collectors.joining(File.pathSeparator));
                System.out.println("classPath = " + classPath);
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
                compiler.getTask(null, fileManager, diagnostics, Arrays.asList("-cp", classPath), null, compilationUnits1).call();
                StringBuilder errBuilder = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    String err = String.format("%s:%d %s %n",
                            diagnostic.getSource().toUri(),
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(Locale.US));
                    errBuilder.append(err);
                }
                String fullErr = errBuilder.toString();
                boolean origDisableShowText = crcl.ui.misc.MultiLineStringJPanel.disableShowText;
                if (fullErr.length() > 0) {
                    crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
                    MultiLineStringJPanel.showText(fullErr).thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
                    if (!customCode.contains("class Custom")) {
                        customCode = INIT_CUSTOM_CODE;
                    }
                    return;
                }
                URL[] urls = new URL[origUrls.length + 1];
                System.arraycopy(origUrls, 0, urls, 0, origUrls.length);
                urls[urls.length - 1] = tmpFile.getAbsoluteFile().getParentFile().getParentFile().toURI().toURL();
                //tmpFile.getAbsoluteFile().getParentFile().getParentFile().toURI().toURL()};
                System.out.println("urls = " + Arrays.toString(urls));
                ClassLoader loader = new URLClassLoader(urls);
                Class clss = loader.loadClass("custom.Custom");
                Object obj = clss.newInstance();
                Method acceptMethod = clss.getMethod("accept", AprsSupervisorJFrame.class);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream origOut = System.out;

                try (PrintStream ps = new PrintStream(baos)) {
                    System.setOut(ps);
                    acceptMethod.invoke(obj, this);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    System.setOut(origOut);
                    System.out.println("content = " + content);
                    if (content.length() > 0) {
                        crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
                        MultiLineStringJPanel.showText(content).thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);

                    }
                } finally {
                    crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText;
                    System.setOut(origOut);
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, exception);
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            boolean origDisableShowText = crcl.ui.misc.MultiLineStringJPanel.disableShowText;
            crcl.ui.misc.MultiLineStringJPanel.disableShowText = false;
            MultiLineStringJPanel.showText(trace).thenRun(() -> crcl.ui.misc.MultiLineStringJPanel.disableShowText = origDisableShowText);
            if (!customCode.contains("class Custom")) {
                customCode = INIT_CUSTOM_CODE;
            }
        }
    }

    private void setTeachSystemFilter(AprsJFrame sys) {
        if (null == sys) {
            object2DOuterJPanel1.setForceOutputFlag(false);
            object2DOuterJPanel1.setShowOutputItems(false);
            object2DOuterJPanel1.setOutputItems(object2DOuterJPanel1.getItems());
        } else {
            object2DOuterJPanel1.setForceOutputFlag(true);
            object2DOuterJPanel1.setSimulatedAndDisconnect();
            object2DOuterJPanel1.setShowOutputItems(true);
            object2DOuterJPanel1.setOutputItems(filterForSystem(sys, object2DOuterJPanel1.getItems()));
        }
    }

    private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
    }

    double getClosestSlotDist(Collection<PhysicalItem> kitTrays, PhysicalItem item) {
        return kitTrays.stream()
                .flatMap(kit -> slotOffsetProvider.getSlotOffsets(kit.getName()).stream()
                .map(slotOffset -> slotOffsetProvider.absSlotFromTrayAndOffset(kit, slotOffset)))
                .mapToDouble(slot -> item.dist(slot))
                .min().orElse(Double.POSITIVE_INFINITY);
    }

    private List<PhysicalItem> filterForSystem(AprsJFrame sys, List<PhysicalItem> listIn) {

        Set<PhysicalItem> allTrays = listIn.stream()
                .filter(x -> "KT".equals(x.getType()) || "PT".equals(x.getType()))
                .collect(Collectors.toSet());
        Set<PhysicalItem> kitTrays = listIn.stream()
                .filter(x -> "KT".equals(x.getType()))
                .collect(Collectors.toSet());
        Set<PhysicalItem> sysKitTrays = kitTrays.stream()
                .filter(tray -> {
                    List<Slot> l2 = sys.getSlotOffsets(tray.getName());
                    return l2 != null && !l2.isEmpty();
                }).collect(Collectors.toSet());
        Set<PhysicalItem> otherSysTrays = new HashSet<>(allTrays);
        otherSysTrays.removeAll(sysKitTrays);
        List<PhysicalItem> listOut = new ArrayList<>(sysKitTrays);
        for (PhysicalItem item : listIn) {
            if ("P".equals(item.getType())) {
                double sysKitDist = getClosestSlotDist(sysKitTrays, item);
                double otherKitDist = getClosestSlotDist(otherSysTrays, item);
                if (sysKitDist < otherKitDist) {
                    listOut.add(item);
                }
            }
        }
        return listOut;
    }

    private XFuture<Void> scanAllInternal() {
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsSys = aprsSystems.get(i);
            try {
                futures[i] = aprsSys.startLookForParts().thenRun(() -> {
                    if (aprsSys.isReverseFlag()) {
                        logEvent("Set reverse flag false for " + aprsSys);
                        aprsSys.setReverseFlag(false, false);
                    }
                    if (jCheckBoxMenuItemUseTeachCamera.isSelected()) {
                        aprsSys.createActionListFromVision(aprsSys.getObjectViewItems(), filterForSystem(aprsSys, object2DOuterJPanel1.getItems()));
                    } else {
                        aprsSys.createActionListFromVision();
                    }
                });
            } catch (Exception ex) {
                log(Level.SEVERE, null, ex);
            }
        }
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        return XFuture.allOfWithName("scanAllInternall", futures).thenCompose(x -> {
            logEvent("Scans Complete");
            setAbortTimeCurrent();
            return showMessageFullScreen("Scans Complete", 80.0f,
                    SplashScreen.getRobotArmImage(),
                    SplashScreen.getBlueWhiteGreenColorList(), gd);
        });
    }

    /**
     * Have each system scan the parts area to create an action list to fill 
     * kits in a way similar to the current configuration. This may require
     * moving each robot out of the way of the vision system. The scans will happen
     * asynchronously in other threads. 
     * @return future that allows actions to be added after all scans are complete.
     */
    public XFuture<?> startScanAll() {
        resetAll(false);
        logEvent("Scan all started.");
        return startCheckAndEnableAllRobots()
                .thenCompose("scanAll2", ok -> checkOkElse(ok, this::scanAllInternal, this::showCheckEnabledErrorSplash));
    }

    /**
     * Perform a test of the continous demo where the motoman robot will be randomly 
     * enabled and disabled.
     * @return a future that can be used to determine if the test failed or was cancelled.
     * 
     */
    public XFuture<?>  startRandomTest() {
        logEvent("Start Random Test");
        connectAll();
        return startCheckAndEnableAllRobots()
                .thenCompose("startRandomTest.checkOk",
                        ok -> checkOkElse(ok, this::startRandomTestStep2, this::showCheckEnabledErrorSplash));
    }

    private XFuture<Void> startRandomTestStep2() {
        continousDemoFuture = startContinousDemo();
        jCheckBoxMenuItemContinousDemo.setSelected(true);
        randomTest = continueRandomTest();
        resetMainRandomTestFuture();
        return XFuture.allOfWithName("startRandomTestStep2.allOff", randomTest, continousDemoFuture);
    }

    private XFuture<Void> startIndRandomTestStep2() {
        continousDemoFuture = startAllIndContinousDemo();
        jCheckBoxMenuItemContinousDemo.setSelected(true);
        randomTest = continueRandomTest();
        resetMainRandomTestFuture();
        return XFuture.allOfWithName("startRandomTestStep2.allOff", randomTest, continousDemoFuture);
    }

    private Random random = new Random(System.currentTimeMillis());

    private XFuture<Void> startRandomDelay(String name, final int millis, final int min_millis) {
        final long val = random.nextInt(millis) + 10 + min_millis;
        return XFuture.runAsync(name + ".randomDelay(" + millis + ":" + val + ")",
                () -> {
                    try {
                        Thread.sleep(val);
                    } catch (InterruptedException ex) {
                        log(Level.SEVERE, null, ex);
                    }
                }
        );

    }

    private static Logger getLogger() {
        return Logger.getLogger(AprsSupervisorJFrame.class
                .getName());
    }

    private volatile boolean togglesAllowed = false;
    private final AtomicInteger waitForTogglesFutureCount = new AtomicInteger();

    private final ConcurrentLinkedDeque<XFuture<Void>> waitForTogglesFutures = new ConcurrentLinkedDeque<>();

    private XFuture<Void> createWaitForTogglesFuture(XFuture<Void> old) {
        if (null != old) {
            return old;
        }
        XFuture<Void> xf = new XFuture<>("waitForTogglesAllowed" + waitForTogglesFutureCount.incrementAndGet());
        waitForTogglesFutures.add(xf);
        return xf;
    }

    private final AtomicReference<XFuture<Void>> togglesAllowedXfuture = new AtomicReference<>(createWaitForTogglesFuture(null));

    private XFuture<Void> waitTogglesAllowed() {
        XFuture xf = togglesAllowedXfuture.getAndSet(null);
        if (null != xf) {
            return xf;
        }
        return XFuture.completedFutureWithName("completedWaitTogglesAllowed", null);
    }

    private void clearAllToggleBlockers() {
        allowTogglesCount.incrementAndGet();
        for (LockInfo li : toggleBlockerMap.values()) {
            li.future.cancelAll(true);
        }
        toggleBlockerMap.clear();
        togglesAllowed = true;
        String blockerList = toggleBlockerMap.toString();
        Utils.runOnDispatchThread(() -> {
            showTogglesEnabled(true);
            jTextFieldRobotEnableToggleBlockers.setText(blockerList);
        });
        if (togglesAllowed) {
            XFuture xf = togglesAllowedXfuture.get();
            if (null != xf) {
                xf.complete(null);
            }
            while ((xf = waitForTogglesFutures.poll()) != null) {
                xf.complete(null);
            }
        }
    }

    private final AtomicLong totalBlockTime = new AtomicLong();

    private void allowToggles(String blockerName, AprsJFrame... systems) {

        try {
            if (null != systems && systems.length > 0) {
                for (AprsJFrame sys : systems) {
                    if (sys.getRobotName() == null || !sys.isConnected() || sys.isAborting()) {
                        String badStateMsg = "allowToggles(" + blockerName + ") : bad state for " + sys;
                        logEvent(badStateMsg);
                        throw new IllegalStateException(badStateMsg);
                    }
                }
            }
            boolean origTogglesAllowed = togglesAllowed;
            allowTogglesCount.incrementAndGet();
            LockInfo lockInfo = toggleBlockerMap.remove(blockerName);
            String blockerList = toggleBlockerMap.keySet().toString();

            if (null == lockInfo) {
                logEvent("allowToggle called for blocker " + blockerName + " not in toggleBlockerMap " + toggleBlockerMap);
            } else {
                long time = lockInfo.getStartTime();
                long blockTime = (System.currentTimeMillis() - time);
                togglesAllowed = toggleBlockerMap.isEmpty();
                if (togglesAllowed && !origTogglesAllowed) {
                    totalBlockTime.addAndGet(blockTime);
                }
                logEvent("allowToggles(" + blockerName + ") after " + blockTime + "ms : blockers=" + blockerList + ", totalBlockTime=" + (totalBlockTime.get() / 1000) + "s");

            }
//        if(blockTime > 20000) {
//            System.out.println("blockTime = " + blockTime);
//        }

            final boolean showTogglesEnabledArg = togglesAllowed;
            Utils.runOnDispatchThread(() -> {
                showTogglesEnabled(showTogglesEnabledArg);
                jTextFieldRobotEnableToggleBlockers.setText(blockerList);
                XFuture.runAsync("finishAllowToggle." + blockerName, () -> finishAllowToggles(lockInfo), supervisorExecutorService);
            });
        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
        }
    }

    private void finishAllowToggles(LockInfo lockInfo) {
        if (togglesAllowed) {
            XFuture xf = togglesAllowedXfuture.get();
            if (null != xf) {
                xf.complete(null);
            }
            while ((xf = waitForTogglesFutures.poll()) != null) {
                xf.complete(null);
            }
        }
        lockInfo.getFuture().complete(null);
    }

    private final AtomicInteger allowTogglesCount = new AtomicInteger();
    private final AtomicInteger disallowTogglesCount = new AtomicInteger();

    private static final class LockInfo {

        private final long startTime;
        private final XFuture<Void> future;

        public LockInfo(String name) {
            startTime = System.currentTimeMillis();
            future = new XFuture(name);
        }

        public long getStartTime() {
            return startTime;
        }

        public XFuture<Void> getFuture() {
            return future;
        }

        @Override
        public String toString() {
            return "LockInfo:" + getTimeString(startTime) + " (" + (System.currentTimeMillis() - startTime) + " ms ago) : " + future;
        }

    }
    private final ConcurrentHashMap<String, LockInfo> toggleBlockerMap = new ConcurrentHashMap<>();

    private LockInfo disallowToggles(String blockerName, AprsJFrame... systems) {

        disallowTogglesCount.incrementAndGet();
        LockInfo lockInfo = new LockInfo(blockerName);
        toggleBlockerMap.put(blockerName, lockInfo);
        String blockerList = toggleBlockerMap.keySet().toString();
        logEvent("disallowToggles(" + blockerName + ") blockers=" + blockerList);
        togglesAllowed = false;
        togglesAllowedXfuture.updateAndGet(this::createWaitForTogglesFuture);
        if (null != systems) {
            for (AprsJFrame sys : systems) {
                addFinishBlocker(sys.getMyThreadId(), lockInfo.getFuture());
            }
        }
        Utils.runOnDispatchThread(() -> {
            showTogglesEnabled(false);
            jTextFieldRobotEnableToggleBlockers.setText(blockerList);
        });
        return lockInfo;
    }

    private void showTogglesEnabled(boolean enabled) {
        robotsEnableCelEditorCheckbox.setEnabled(enabled);
        robotsEnableCelRendererComponent.setEnabled(enabled);
        jTableRobots.repaint();
    }

    private XFuture<Boolean> toggleRobotEnabled() {
        if (jCheckBoxMenuItemPause.isSelected()) {
            return waitResume().thenApply(x -> false);
        }
        return Utils.supplyOnDispatchThread(
                () -> {
                    if (!jCheckBoxMenuItemPause.isSelected()) {
                        if (togglesAllowed) {
                            for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                                String robotName = (String) jTableRobots.getValueAt(i, 0);
                                if (robotName.toLowerCase().contains("motoman")) { // stupid hard-coded hack to match demo
                                    Boolean enabled = (Boolean) jTableRobots.getValueAt(i, 1);
                                    Boolean wasEnabled = robotEnableMap.get(robotName);
                                    jTableRobots.setValueAt(!wasEnabled, i, 1);
//                    setRobotEnabled(robotName, !wasEnabled);
                                    return true;
                                }
                            }
                        } else {
                            logEvent("Attempt to toggle robot enabled ignored.");
                        }
                    }
                    return false;
                });
    }

    private final AtomicInteger randomTestCount = new AtomicInteger();

    private XFuture<Void> updateRandomTestCount() {
        return Utils.runOnDispatchThread("updateRandomTest.runOnDispatchThread" + randomTestCount.get(),
                () -> {
                    int count = randomTestCount.incrementAndGet();
                    System.out.println("updateRandomTestCount count = " + count);
                    jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continous Demo " + count);
                });
    }

    private boolean allSystemsOk() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            CRCLStatusType status = sys.getCurrentStatus();
            if (status != null
                    && status.getCommandStatus() != null
                    && status.getCommandStatus().getCommandState() == CommandStateEnumType.CRCL_ERROR) {
                System.err.println("allSystemsOk failing: bad status for sys=" + sys);
                return false;
            }
            if (sys.getTitleErrorString() != null && sys.getTitleErrorString().length() > 0) {
                System.err.println("allSystemsOk failing: bad titleErrorString (" + sys.getTitleErrorString() + ") for sys=" + sys);
                return false;
            }
            if (sys.getCrclClientErrorString() != null && sys.getCrclClientErrorString().length() > 0) {
                System.err.println("allSystemsOk failing: bad rclClientErrorString (" + sys.getCrclClientErrorString() + ") for sys=" + sys);
                return false;
            }
        }
        return true;
    }

    private volatile XFuture<Void> pauseTest = null;

    private XFuture<Void> continuePauseTest() {
        if (!allSystemsOk()) {
            System.err.println("allSystemsOk returned false forcing quitRandomTest");
            return quitRandomTest();
        }
        if (!jCheckBoxMenuItemContinousDemo.isSelected() && !jCheckBoxMenuItemContinousDemoRevFirst.isSelected()) {
            System.err.println("jCheckBoxMenuItemContinousDemo.isSelected() returned false forcing quitRandomTest");
            return quitRandomTest();
        }
        if (!jCheckBoxMenuItemRandomTest.isSelected()) {
            System.err.println("jCheckBoxMenuItemRandomTest.isSelected().isSelected() returned false forcing quitRandomTest");
            return quitRandomTest();
        }
        if (null == continousDemoCycle
                || continousDemoFuture.isCancelled()
                || continousDemoFuture.isDone()
                || continousDemoFuture.isCompletedExceptionally()) {
            System.out.println("continousDemoCycle = " + continousDemoCycle + " forcing quitRandomTest");
            printStatus(continousDemoFuture, System.out);
            return quitRandomTest();
        }
        pauseTest = startRandomDelay("pauseTest", 30000, 20000)
                .thenCompose("pauseTest.pause" + pauseCount.get(),
                        x -> Utils.runOnDispatchThread(this::pause))
                .thenCompose(x -> startRandomDelay("pauseTest", 1000, 1000))
                .thenCompose("pauseTest.resume" + pauseCount.get(),
                        x -> Utils.runOnDispatchThread(this::resume));
        resetMainPauseTestFuture();
        pauseTest
                .thenCompose("pauseTest.recurse" + pauseCount.get(),
                        x -> continuePauseTest());
        return pauseTest;
    }

    private XFuture<Void> continueRandomTest() {
        if (!allSystemsOk()) {
            System.err.println("allSystemsOk returned false forcing quitRandomTest");
            return quitRandomTest();
        }
        if (!jCheckBoxMenuItemContinousDemo.isSelected()
                && !jCheckBoxMenuItemContinousDemoRevFirst.isSelected()
                && !jCheckBoxMenuItemIndContinousDemo.isSelected()
                && !jCheckBoxMenuItemIndRandomToggleTest.isSelected()) {
            System.err.println("jCheckBoxMenuItemContinousDemo.isSelected() returned false forcing quitRandomTest");
            return quitRandomTest();
        }
        if (!jCheckBoxMenuItemRandomTest.isSelected()
                && !jCheckBoxMenuItemIndRandomToggleTest.isSelected()) {
            System.err.println("jCheckBoxMenuItemRandomTest.isSelected().isSelected() returned false forcing quitRandomTest");
            return quitRandomTest();
        }
        if (null == continousDemoCycle
                || continousDemoFuture.isCancelled()
                || continousDemoFuture.isDone()
                || continousDemoFuture.isCompletedExceptionally()) {
            System.out.println("continousDemoCycle = " + continousDemoCycle + " forcing quitRandomTest");
            printStatus(continousDemoFuture, System.out);
            return quitRandomTest();
        }
        randomTest = startRandomDelay("enableTest", 30000, 20000)
                .thenCompose("checkForWaitResume1", x -> this.waitResume())
                .thenCompose("waitTogglesAllowed", x -> this.waitTogglesAllowed())
                .thenCompose("toggleRobotEnabled", x -> this.toggleRobotEnabled())
                .thenCompose("updateRandomTestCount" + randomTestCount.get(), x -> {
                    if (x) {
                        return this.updateRandomTestCount();
                    } else {
                        return XFuture.completedFuture(null);
                    }
                })
                .thenCompose("continueRandomTest.recurse" + randomTestCount.get(), x -> continueRandomTest());
        resetMainRandomTestFuture();
        return randomTest;
    }

    private XFuture<Void> quitRandomTest() {
        XFuture<Void> xf = new XFuture<>("cancel_me");
        xf.cancel(false);
        System.out.println("continueRandomTest quit");
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        immediateAbortAll();
        return xf;
    }

    /**
     * Set the reverseFlag for all systems. When the reverseFlag is set
     * systems empty kit trays and put parts back in parts trays.
     * @param reverseFlag
     */
    public void setAllReverseFlag(boolean reverseFlag) {
        logEvent("setAllReverseFlag(" + reverseFlag + ")");
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            if (sys.isReverseFlag() != reverseFlag) {
                logEvent("setting reverseFlag for " + sys + " to " + reverseFlag);
                sys.setReverseFlag(reverseFlag);
            }
        }
    }

    private void disconnectAllNoLog() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).setConnected(false);
        }
    }

    /**
     * Disconnect all systems.
     */
    public void disconnectAll() {
        logEvent("disconnectAll");
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            if (sys.isConnected()) {
                logEvent("Disconnecting " + sys);
                sys.setConnected(false);
            }
        }
    }

    /**
     * Start a continuous demo where kit trays will first be emptied and then 
     * repeatedly filled and emptied indefinitely.
     * @return future that can be used to determine if it fails or is cancelled
     */
    public XFuture<?> startContinuousDemoRevFirst() {
        logEvent("Start Continous Demo (Reverse First)");
        connectAll();
        final XFuture<?> lfr = this.lastFutureReturned;
        continousDemoFuture
                = startCheckAndEnableAllRobots()
                        .thenComposeAsync("startContinousDemoRevFirst.checkLastReturnedFuture1", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst.startReverseActions", x -> startReverseActions(), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst.checkLastReturnedFuture2", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst.enableAndCheckAllRobots", x -> startCheckAndEnableAllRobots(), supervisorExecutorService)
                        .thenComposeAsync("startContinousDemoRevFirst", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash), supervisorExecutorService);
        return continousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then emptied repeatedly.
     * Systems will wait for all systems to be filled before any begin emptying and vice versa.
     * 
     * @return future that can be used to determine if it fails or is cancelled
     */
    public XFuture<?> startContinousDemo() {
        logEvent("Start continous demo");
        connectAll();
        continousDemoFuture
                = startCheckAndEnableAllRobots()
                        .thenCompose("startContinousDemo", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash));
        return continousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then emptied repeatedly.
     * Systems will not wait for all systems to be filled before any begin emptying and vice versa, so
     * one might be emptying while another is filling.
     * 
     * @return future that can be used to determine if it fails or is canceled
     */
    public XFuture<?> startIndependentContinousDemo() {
        logEvent("Start continous demo");
        connectAll();
        continousDemoFuture
                = startCheckAndEnableAllRobots()
                        .thenCompose("startIndContinousDemo", ok -> checkOkElse(ok, this::startAllIndContinousDemo, this::showCheckEnabledErrorSplash));
        if (null != randomTest && jCheckBoxMenuItemIndRandomToggleTest.isSelected()) {
            resetMainRandomTestFuture();
        }
        return continousDemoFuture;
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then emptied repeatedly.
     * Systems will not wait for all systems to be filled before any begin emptying and vice versa, so
     * one might be emptying while another is filling. In addition the motoman will
     * be randomly enabled and disabled for testing purposes.
     * 
     * @return future that can be used to determine if it fails or is canceled
     */
    public XFuture<?> startRandomEnableToggleIndependentContinousDemo() {
        logEvent("Start Independent Random  Enable Toggle Test");
        connectAll();
        return startCheckAndEnableAllRobots()
                .thenCompose("startRandomEnableToggleIndependentContinousDemo.checkOk",
                        ok -> checkOkElse(ok, this::startIndRandomTestStep2, this::showCheckEnabledErrorSplash));
    }

    private final AtomicInteger continousDemoCycle = new AtomicInteger(0);

    private XFuture<Void> incrementContinousDemoCycle() {
        final int c = continousDemoCycle.incrementAndGet();
        System.out.println("incrementContinousDemoCycle : " + c);
        if (jCheckBoxMenuItemContinousDemoRevFirst.isSelected()) {
            return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinousDemoRevFirst.setText("Continous Demo (Reverse First) (" + c + ") "));
        } else {
            return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinousDemo.setText("Continous Demo (" + c + ") "));
        }
    }

    final static private AtomicInteger runProgramThreadCount = new AtomicInteger();

    private final int myThreadId = runProgramThreadCount.incrementAndGet();

    private final ExecutorService defaultSupervisorExecutorService
            = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "AprsSupervisor" + myThreadId);
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private ExecutorService supervisorExecutorService = defaultSupervisorExecutorService;

    private XFuture<?> continueContinousDemo() {
        logEvent("Continue Continous Demo : " + continousDemoCycle.get());
        return continousDemoSetup()
                .thenCompose("continouseDemo.part2", x2 -> {
                    final XFuture<?> lfr = this.lastFutureReturned;
                    continousDemoFuture
                            = startCheckAndEnableAllRobots()
                                    .thenComposeAsync("continueContinousDemo.startAllActions1", x -> startAllActions(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.checkLastReturnedFuture1", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.startReverseActions", x -> startReverseActions(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.checkLastReturnedFuture2", x -> checkLastReturnedFuture(lfr), supervisorExecutorService)
                                    .thenCompose("continueContinousDemo.incrementContinousDemoCycle", x -> incrementContinousDemoCycle())
                                    .thenComposeAsync("continueContinousDemo.enableAndCheckAllRobots", x -> startCheckAndEnableAllRobots(), supervisorExecutorService)
                                    .thenComposeAsync("continueContinousDemo.recurse" + continousDemoCycle.get(), ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash), supervisorExecutorService);
                    if (null != randomTest) {
                        if (jCheckBoxMenuItemRandomTest.isSelected()) {
                            resetMainRandomTestFuture();
                        } else if (jCheckBoxMenuItemRandomTestReverseFirst.isSelected()) {
                            resetMainRandomTestFuture();
                        } else if (jCheckBoxMenuItemPauseResumeTest.isSelected()) {
                            resetMainPauseTestFuture();
                        }
                    }
                    return continousDemoFuture;
                });
    }

    private XFuture<Void> continousDemoSetup() {
        return XFuture.supplyAsync("contiousDemoSetup", () -> {
            System.out.println("stealingRobots = " + stealingRobots);
            System.out.println("returnRobotRunnable = " + returnRobotRunnable);
            return null;
        }, supervisorExecutorService)
                .thenComposeAsync("contiousDemoSetup.part2", x -> {
                    if (this.stealingRobots || null != returnRobotRunnable.get()) {
                        disconnectAll();
                        return returnRobotsDirect("contiousDemoSetup.part2");
                    } else {
                        return XFuture.completedFuture(null);
                    }
                }, supervisorExecutorService)
                .thenRunAsync(() -> {
                    disconnectAll();
                    checkRobotsUniquePorts();
                    System.out.println("stealingRobots = " + stealingRobots);
                    System.out.println("returnRobotRunnable = " + returnRobotRunnable);
                    cancelAllStealUnsteal(false);
                    connectAll();
                    setAllReverseFlag(false);
                }, supervisorExecutorService);
    }

    private volatile boolean debugStartReverseActions = false;

    /**
     * Start actions in reverse mode where kit trays will be emptied rather than filled.
     * @return future that can be used to attach additional actions after this is complete
     */
    public XFuture<?> startReverseActions() {
        logEvent("startReverseActions");
        setAllReverseFlag(true);
        if (debugStartReverseActions) {
            debugAction();
        }
        return startCheckAndEnableAllRobots()
                .thenCompose("startReverseActions.startAllActions", ok -> checkOkElse(ok, this::startAllActions, this::showCheckEnabledErrorSplash));
    }

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

    /**
     * Enable all robots. 
     * (Note: no check is made if the robot is physically in estop and no change to its estop state is made, 
     * only the checkboxes in the robots table are potentially changed.)
     */
    public void enableAllRobots() {
        cancelAllStealUnsteal(false);
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
        }
        DefaultTableModel model = (DefaultTableModel) jTableRobots.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            robotEnableMap.put(robotName, true);
            model.setValueAt(true, i, 1);
            model.setValueAt(robotDisableCountMap.getOrDefault(robotName, 0), i, 4);
            model.setValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 5);
        }
        Utils.autoResizeTableColWidths(jTableRobots);
    }

    private final AtomicInteger enableAndCheckAllRobotsCount = new AtomicInteger();

    /**
     * Enable and check all robots. All of the checkboxes in the robots table will be set and 
     * a short nop program will be run on each robot to ensure they are out of estop 
     * and able to run programs autonomously. A delay of a second or two and the sound of
     * brakes coming off may be heard. The checks will be performed in other threads asynchronously.
     * 
     * @return a future that can be used to determine when and if all the checks succeed.
     */
    public XFuture<Boolean> startCheckAndEnableAllRobots() {

        Utils.runOnDispatchThread(() -> {
            DefaultTableModel model = (DefaultTableModel) jTableRobots.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String robotName = (String) jTableRobots.getValueAt(i, 0);
                robotEnableMap.put(robotName, true);
                model.setValueAt(true, i, 1);
                model.setValueAt(robotDisableCountMap.computeIfAbsent(robotName, (k) -> 0), i, 4);
                model.setValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 5);
            }
            Utils.autoResizeTableColWidths(jTableRobots);
        }).thenRunAsync("sendEnableToColorTextSocket", () -> {
            try {
                initColorTextSocket();
                if (null != colorTextSocket) {
                    colorTextSocket.getOutputStream().write("0x00FF00, 0x00FF000\r\n".getBytes());
                    colorTextSocket.getOutputStream().flush();
                }
            } catch (IOException ex) {
                log(Level.SEVERE, null, ex);
            }
        }, supervisorExecutorService);
        String blockerName = "enableAndCheckAllRobots" + enableAndCheckAllRobotsCount.incrementAndGet();
        AprsJFrame sysArray[] = getAprsSystems().toArray(new AprsJFrame[getAprsSystems().size()]);
        disallowToggles(blockerName, sysArray);
        cancelAllStealUnsteal(false);
        return returnRobots("enableAndCheckAllRobots")
                .thenComposeAsync(x -> checkEnabledAll(), supervisorExecutorService)
                .alwaysAsync(() -> {
                    allowToggles(blockerName, sysArray);
                }, supervisorExecutorService);
    }

    private XFuture<Boolean> checkEnabledAll() {
        logEvent("checkEnabledAll");
        boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
        XFuture<Boolean> futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            futures[i] = sys.startCheckEnabled()
                    .thenApplyAsync(x -> {
                        logEvent(sys.getRobotName() + " checkEnabled returned " + x);
                        return x;
                    }, supervisorExecutorService);
        }
        XFuture<Boolean> ret = XFuture.completedFuture(true);
        BiFunction<Boolean, Boolean, Boolean> andBiFunction = (Boolean ok1, Boolean ok2) -> ok1 && ok2;
        for (int i = 0; i < futures.length; i++) {
            XFuture<Boolean> fi = futures[i];
            if (fi.isCompletedExceptionally()) {
                XFuture<Boolean> newret = new XFuture<>("checkEnabledAll.alreadyFailed." + aprsSystems.get(i).getTaskName());
                newret.completeExceptionally(new IllegalStateException("isCompletedExceptionally() for " + aprsSystems.get(i).getTaskName()));
            }
            ret = (XFuture<Boolean>) ret
                    .thenCombine("checkEnabledAll(" + (i + 1) + "/" + futures.length + ")",
                            fi, andBiFunction);
        }
        return ret.always(() -> {
            if (!origIgnoreTitleErrs) {
                ignoreTitleErrors.set(false);
            }
        });
    }
    AtomicInteger pauseCount = new AtomicInteger();

    private void pause() {
        logEvent("pause");
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
        completeResumeFuture();
        int count = pauseCount.incrementAndGet();
        jCheckBoxMenuItemPause.setText("Pause (" + count + ") ");
        if (!jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(true);
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsSys = aprsSystems.get(i);
            if (aprsSys.isConnected() && !aprsSys.isPaused()) {
                aprsSys.pause();
            }
        }
        resumeFuture.set(new XFuture<>("resume"));
    }

    private void completeResumeFuture() {
        XFuture<Void> rf = resumeFuture.getAndSet(null);
        if (null != rf) {
            rf.complete(null);
        }
    }

    private XFuture<Void> waitResume() {
        XFuture<Void> rf = resumeFuture.get();
        if (null != rf) {
            return rf;
        } else {
            return XFuture.completedFutureWithName("waitResume.rf==null", null);
        }
    }

    private void resume() {
        logEvent("resume");
        resumeForPrepOnly();
        startUpdateRunningTimeTimer();
    }

    private void resumeForPrepOnly() {
        if (jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(false);
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsSys = aprsSystems.get(i);
            if (aprsSys.isPaused()) {
                aprsSys.resume();
            }
        }
        completeResumeFuture();
    }

    private volatile XFuture lastStartAllActionsArray[] = null;

    private final ConcurrentHashMap<Integer, XFuture<Boolean>> systemContinueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, XFuture<Void>> debugSystemContinueMap = new ConcurrentHashMap<>();

    private XFuture<Void> continueSingleContinousDemo(AprsJFrame sys) {
        XFuture<Void> ret = debugSystemContinueMap.compute(sys.getMyThreadId(),
                (k, v) -> {
                    return continueSingleContinousDemoInner(sys);
                });
        StringBuilder tasksNames = new StringBuilder();
        Set<Integer> keySet = debugSystemContinueMap.keySet();
        XFuture futures[] = new XFuture[keySet.size()];
        int i = 0;
        for (int id : keySet) {
            assert (i < futures.length) : "futures=" + futures + ",keySet=" + keySet + ",i=" + i;
            futures[i++] = debugSystemContinueMap.get(id);
        }
        assert (i == futures.length) : "futures=" + futures + ",keySet=" + keySet + ",i=" + i;
        for (int j = 0; j < aprsSystems.size(); j++) {
            AprsJFrame sysTemp = aprsSystems.get(j);
            if (debugSystemContinueMap.containsKey(sysTemp.getMyThreadId())) {
                tasksNames.append(sysTemp.getTaskName()).append(',');
            }
        }
        continousDemoFuture = XFuture.allOfWithName("continueSingleContinousDemo.allOf(" + tasksNames.toString() + ")", futures);
        if (null != randomTest) {
            resetMainRandomTestFuture();
        }
        return ret;
    }

    private XFuture<Void> continueSingleContinousDemoInner(AprsJFrame sys) {
        return systemContinueMap.computeIfAbsent(sys.getMyThreadId(), k -> {
            return new XFuture<>("continueSingleContinousDemo.holder " + sys);
        })
                .thenCompose("continueSingleContinousDemo.continuing:" + sys,
                        x -> {
                            logEvent("startCheckEnabled for " + sys);
                            disallowToggles("continueSingleContinousDemoInner" + sys.getMyThreadId(), sys);
                            return sys.startCheckEnabled().thenApply(y -> x);
                        })
                .thenCompose("continueSingleContinousDemo.continuing:" + sys,
                        x -> {
                            XFuture<Boolean> ret = sys.startPreCheckedContinousDemo("continueSingleContinousDemoInner" + sys, x);
                            logEvent("startContinousDemo(reverseFirst=" + !sys.isReverseFlag() + ") for " + sys);
                            allowToggles("continueSingleContinousDemoInner" + sys.getMyThreadId(), sys);
                            return ret;
                        })
                .thenCompose("continueSingleContinousDemo.recurse" + sys,
                        x -> continueSingleContinousDemo(sys));
    }

    private XFuture<Void> startAllIndContinousDemo() {
        logEvent("startAllIndContinousDemo");
        XFuture futures[] = new XFuture[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        boolean revFirst = jCheckBoxMenuItemContDemoReverseFirst.isSelected();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            logEvent("startContinousDemo(reverseFirst=false) for " + sys);
            futures[i] = sys.startContinousDemo("startAllIndContinousDemo", revFirst)
                    .thenCompose(x -> continueSingleContinousDemo(sys));
            tasksNames.append(aprsSystems.get(i).getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
//        allowToggles();
        return XFuture.allOfWithName("startAllIndContinousDemo.allOf(" + tasksNames.toString() + ")", futures);
    }

    private final AtomicInteger startAllActionsCount = new AtomicInteger();
    
    private XFuture<Void> startAllActions() {
        int saaNumber = startAllActionsCount.incrementAndGet();
        logEvent("startAllActions"+saaNumber);
        XFuture futures[] = new XFuture[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            int sysThreadId = sys.getMyThreadId();
            logEvent("startActions for " + sys);
            futures[i] = sys.startActions("startAllActions"+saaNumber)
                    .thenComposeAsync(x -> {
                        String runName = sys.getRunName();
                        logEvent("startActions " + sys +",saaNumber= "+saaNumber+ " completed action list run " + runName + " : " + x);
                        return finishAction(sysThreadId)
                                .thenApply(x2 -> {
                                    logEvent("finish startActions " + sys +",saaNumber= "+saaNumber+" completed action list run " + runName + " : " + x);
                                    return x;
                                });
                    },
                            supervisorExecutorService);
            tasksNames.append(sys.getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        return XFuture.allOfWithName("startAllActions.allOf(" + tasksNames.toString() + ")"+saaNumber, futures);
    }

    private final ConcurrentHashMap<Integer, XFuture<Void>[]> continueCompletionBlockersMap = new ConcurrentHashMap<>();

    private void addFinishBlocker(int threadId, XFuture<Void> f) {
        continueCompletionBlockersMap.compute(threadId,
                (Integer k, XFuture<Void>[] v) -> {
                    try {
                        if (null != v) {
                            List<XFuture<Void>> l = Arrays.stream(v)
                                    .filter(f2 -> !f2.isDone())
                                    .collect(Collectors.toList());
                            l.add(f);
                            if (l.size() < 1) {
                                return new XFuture[]{f};
                            }
                            XFuture<Void>[] newV = l.toArray(new XFuture[l.size()]);
                            if (newV.length < 1) {
                                return new XFuture[]{f};
                            }
                            return newV;
                        } else {
                            return new XFuture[]{f};
                        }
                    } catch (Throwable e) {
                        log(Level.SEVERE, null, e);
                        throw new RuntimeException(e);
                    }
                });
    }

    private static String shortTrace(StackTraceElement[] trace) {
        String shortTrace = Arrays.toString(trace);
        if (shortTrace.length() > 80) {
            shortTrace = shortTrace.substring(0, 75) + " ... ";
        }
        return shortTrace;
    }

    /**
     * Log an exception to the events table.
     * @param level log severity indicator
     * @param msg   message to show
     * @param thrown exception causing this event
     */
    public void log(Level level, String msg, Throwable thrown) {
        getLogger().log(level, msg, thrown);

        logEvent("Exception thrown : msg=" + msg + ",thrown=" + thrown + ", trace=" + shortTrace(thrown.getStackTrace()));
    }

    private XFuture<Void> finishAction(int threadId) {
        XFuture<Void>[] futures = continueCompletionBlockersMap.compute(threadId,
                (Integer k, XFuture<Void>[] v) -> {
                    try {
                        if (null != v) {
                            List<XFuture<Void>> l = Arrays.stream(v)
                                    .filter(f2 -> !f2.isDone())
                                    .collect(Collectors.toList());
                            if (l.size() < 1) {
                                return null;
                            }
                            XFuture<Void>[] newV = l.toArray(new XFuture[l.size()]);
                            if (newV.length < 1) {
                                return null;
                            }
                            return newV;
                        } else {
                            return null;
                        }
                    } catch (Throwable e) {
                        log(Level.SEVERE, null, e);
                        throw new RuntimeException(e);
                    }
                });
        if (null == futures) {
            return (XFuture<Void>) XFuture.completedFuture((Void) null);
        } else {
            logEvent("finishAction waiting for " + Arrays.toString(futures));
            return XFuture.allOf(futures);
        }
    }

    private XFuture<Void> finishAction(AprsJFrame sys) {
        return finishAction(sys.getMyThreadId());
    }

    private XFuture<Void> continueAllActions() {
        logEvent("continueAllActions");
        XFuture futures[] = new XFuture[aprsSystems.size()];
        StringBuilder tasksNames = new StringBuilder();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            int sysThreadId = sys.getMyThreadId();
            logEvent("Continue actions for " + sys.getTaskName() + " with " + sys.getRobotName());
            futures[i] = aprsSystems.get(i).continueActionList("continueAllActions")
                    .thenComposeAsync(x -> {
                        logEvent("continueAllActions " + sys.getRunName() + " completed action list " + x);
                        return finishAction(sysThreadId)
                                .thenApply(x2 -> {
                                    logEvent("continueAllActions finish " + sys.getRunName() + " completed action list " + x);
                                    return x;
                                });
                    },
                            supervisorExecutorService);
            tasksNames.append(aprsSystems.get(i).getTaskName());
            tasksNames.append(",");
        }
        lastStartAllActionsArray = futures;
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
//        allowToggles();
        return XFuture.allOfWithName("continueAllActions.allOf(" + tasksNames.toString() + ")", futures);
    }

    private XFuture<?> checkOkElse(Boolean ok, Supplier<XFuture<?>> okSupplier, Supplier<XFuture<?>> notOkSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return notOkSupplier.get();
        }
    }

    private XFuture<Void> checkOk(Boolean ok, Supplier<XFuture<Void>> okSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return XFuture.completedFutureWithName("checkOk(false)", null);
        }
    }

    private XFuture<?> continueAll() {
        logEvent("Continoue All : " + continousDemoCycle.get());
        stealingRobots = false;
        XFuture<?> f = startCheckAndEnableAllRobots()
                .thenCompose("continueAll.recurse" + continousDemoCycle.get(),
                        ok -> checkOkElse(ok, this::continueAllActions, this::showCheckEnabledErrorSplash));
        return f;
    }

    /**
     * Have all robots start their already assigned list of actions. These will occur in 
     * other threads asynchronously.
     * @return future allowing caller to determine when  all systems have completed 
     *
     */
    public XFuture<?> startAll() {
        logEvent("Start All ");
        stealingRobots = false;
        XFuture<Boolean> f = startCheckAndEnableAllRobots();
        f
                .thenCompose("startAll.recurse",
                        ok -> checkOkElse(ok, this::startAllActions, this::showCheckEnabledErrorSplash));
        return f;
    }

    /**
     * Clear all previously set errors /error states.
     */
    public void clearAllErrors() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).clearErrors();
        }
    }

    /**
     * Have all systems immediately abort regardless of the robots position or the object in the gripper.
     * Robots that have been tempararily reassigned will be returned. This may require a delay that 
     * can be checked on with the returned future.
     * @return future allowing a check on when the abort is complete.
     */
    public XFuture<?> immediateAbortAll() {
        return immediateAbortAll(false);
    }

    private XFuture<?> immediateAbortAll(boolean skipLog) {
        stealingRobots = false;
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
        cancelAll(true);
        if (null != logPrintStream) {
            logPrintStream.close();
            logPrintStream = null;
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).immediateAbort();
        }
        if (this.stealingRobots || null != returnRobotRunnable.get()) {
            disconnectAllNoLog();
            return returnRobots("immediateAbortAll").thenRun(() -> {
                disconnectAllNoLog();
                if (!skipLog) {
                    logEvent("immediateAbort");
                }
                setAbortTimeCurrent();
                if (null != runTimeTimer) {
                    runTimeTimer.stop();
                    runTimeTimer = null;
                }
            });
        } else {
            checkRobotsUniquePorts();
            if (!skipLog) {
                logEvent("immediateAbort");
            }
            setAbortTimeCurrent();
            if (null != runTimeTimer) {
                runTimeTimer.stop();
                runTimeTimer = null;
            }
            return XFuture.completedFuture(null);
        }
    }

    private void cancelAll(boolean mayInterrupt) {
        if (null != continousDemoFuture) {
            continousDemoFuture.cancelAll(mayInterrupt);
            continousDemoFuture = null;
        }
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(mayInterrupt);
            lastFutureReturned = null;
        }
        if (null != stealAbortFuture) {
            stealAbortFuture.cancelAll(mayInterrupt);
            stealAbortFuture = null;
        }

        if (null != unstealAbortFuture) {
            unstealAbortFuture.cancelAll(mayInterrupt);
            unstealAbortFuture = null;
        }
        cancelAllStealUnsteal(mayInterrupt);
        if (null != randomTest) {
            randomTest.cancelAll(mayInterrupt);
            randomTest = null;
        }
    }

    private void cancelAllStealUnsteal(boolean mayInterrupt) {
        stealingRobots = false;
        XFuture stealFuture = this.stealRobotFuture.getAndSet(null);
        if (null != stealFuture) {
            stealFuture.cancelAll(mayInterrupt);
        }

        XFuture unstealFuture = this.unStealRobotFuture.getAndSet(null);
        if (null != unstealFuture) {
            unstealFuture.cancelAll(mayInterrupt);
        }

        XFuture cancelStealFuture = this.cancelStealRobotFuture.getAndSet(null);
        if (null != cancelStealFuture) {
            cancelStealFuture.cancelAll(mayInterrupt);
        }

        XFuture cancelUnstealFuture = this.cancelUnStealRobotFuture.getAndSet(null);
        if (null != cancelUnstealFuture) {
            cancelUnstealFuture.cancelAll(mayInterrupt);
        }
    }

    /**
     * Connect to all robots.
     */
    public void connectAll() {
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
        }
        boolean globalPause = jCheckBoxMenuItemPause.isSelected();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsSys = aprsSystems.get(i);
            if (!aprsSys.isConnected()) {
                aprsSys.setConnected(true);
            }
            if (aprsSys.isPaused() && !globalPause) {
                aprsSys.resume();
            } else if (!aprsSys.isPaused() && globalPause) {
                aprsSys.pause();
            }
        }
        checkRobotsUniquePorts();
    }

    /**
     * Have all robots abort their actions after any part they are holding has been dropped off and 
     * the robot has been moved out of the way of the vision system.
     * @return future allowing caller to determine when the abort is complete
     */
    public XFuture<Void> safeAbortAll() {
        XFuture<?> prevLastFuture = lastFutureReturned;
        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).startSafeAbort("safeAbortAll");
        }
        return XFuture.allOfWithName("safeAbortAll", futures)
                .thenCompose(x -> {
                    return returnRobots("safeAbortAll");
                })
                .thenRun(() -> {
                    if (null != prevLastFuture) {
                        prevLastFuture.cancelAll(false);
                    }
                    logEvent("safeAbortAll completed");
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

    private void setTitleMessage(String message) {
        if (null != this.setupFile) {
            Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor : " + this.setupFile + " : " + message));
        } else {
            Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor : : " + message));
        }
    }

    /**
     * Set the value of setupFile
     *
     * @param f new value of setupFile
     * @throws java.io.IOException can not save last setup file
     */
    public void setSetupFile(File f) throws IOException {
        if (!Objects.equals(this.setupFile, f)) {
            if (null != f) {
                Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor : " + f));
            } else {
                Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor"));
            }
        }
        if (null != f) {
            saveLastSetupFile(f);
        }
        this.setupFile = f;
        this.jMenuItemSaveSetup.setEnabled(f != null);
    }

    /**
     * Save the current setup to the last saved/read setup file.
     */
    public void saveCurrentSetup() {
        try {
            if (null != setupFile) {
                int response
                        = JOptionPane.showConfirmDialog(this, "Save Current APRS Supervisor file : " + setupFile);
                if (response == JOptionPane.YES_OPTION) {
                    saveSetupFile(setupFile);
                }
            }
        } catch (IOException ex) {
            log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Save the current setup to the given file.
     * @param f file to save setup to
     * @throws IOException file can not be written to
     */
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

    /**
     * Save the posmaps to the given file. The posmap file is a CSV file
     * that points to other csv files with infomation needed to transform
     * coordinates from one robot to another.
     * @param f file to safe posmaps in
     * @throws IOException file could not be written to
     */
    public void savePositionMaps(File f) throws IOException {
        saveJTable(f, jTablePositionMappings);
        saveLastPosMapFile(f);
    }

    private Map<String, Map<String, File>> posMaps = new HashMap<>();

    /**
     * Get the file location where data is stored for converting positions from 
     * sys1 to sys2. The file is a CSV file.
     * 
     * @param sys1 system to convert positions from
     * @param sys2 system to convert positions to
     * @return file for converting positions
     */
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

    /**
     * Set the file location where data is stored for converting positions from 
     * sys1 to sys2. The file is a CSV file.
     * 
     * @param sys1 system to convert positions from
     * @param sys2 system to convert positions to
     * @param f new file location
     */
     public void setPosMapFile(String sys1, String sys2, File f) {
        Map<String, File> subMap = posMaps.get(sys1);
        if (null == subMap) {
            subMap = new HashMap<>();
            posMaps.put(sys1, subMap);
        }
        subMap.put(sys2, f);
    }

    /**
     * Load posmaps from the given file.
     * @param f file to load
     * @throws IOException file could not be read
     */
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

    private File lastSimTeachFile = null;

    private void saveSimTeach(File f) throws IOException {
        object2DOuterJPanel1.saveFile(f);
        saveLastSimTeachFile(f);
    }

    private void saveLastSimTeachFile(File f) throws IOException {
        lastSimTeachFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastSimTeachFileFile))) {
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

    private File lastTeachPropsFile = null;

    private void saveLastTeachPropsFile(File f) throws IOException {
        lastTeachPropsFile = f;
        try (PrintWriter pw = new PrintWriter(new FileWriter(lastTeachPropertiesFileFile))) {
            pw.println(f.getCanonicalPath());
        }
    }

    private void saveTeachProps(File f) throws IOException {
        object2DOuterJPanel1.setPropertiesFile(f);
        object2DOuterJPanel1.saveProperties();
        saveLastTeachPropsFile(f);
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

    /**
     * Close all systems.
     */
    public void closeAllAprsSystems() {
        for (AprsJFrame aprsJframe : aprsSystems) {
            try {
                aprsJframe.close();

            } catch (Exception ex) {
                log(Level.SEVERE, null, ex);
            }
        }
        aprsSystems.clear();
        updateTasksTable();
        updateRobotsTable();
    }

    /**
     * Load the given setup file.
     * @param f setup file to load
     * @throws IOException file could not be read
     */
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
                aj.setSupervisorEventLogger(this::logEvent);
                aprsSystems.add(aj);
            }
        }
        Collections.sort(aprsSystems, 
                (AprsJFrame o1, AprsJFrame o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
        updateTasksTable();
        updateRobotsTable();

        clearPosTable();
        setSetupFile(f);
    }

    private String lastUpdateTaskTableTaskNames[] = null;

    private final ConcurrentHashMap<Integer, String> titleErrorMap = new ConcurrentHashMap<>();

    private void updateTasksTable()  {
        DefaultTableModel tm = (DefaultTableModel) jTableTasks.getModel();
        boolean needSetJListFuturesModel = false;
        tm.setRowCount(0);
        if (lastUpdateTaskTableTaskNames == null
                || lastUpdateTaskTableTaskNames.length != aprsSystems.size()) {
            lastUpdateTaskTableTaskNames = new String[aprsSystems.size()];
            needSetJListFuturesModel = true;
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsJframe = aprsSystems.get(i);
            String taskName = aprsJframe.getTaskName();
            if (!Objects.equals(taskName, lastUpdateTaskTableTaskNames[i])) {
                lastUpdateTaskTableTaskNames[i] = taskName;
                needSetJListFuturesModel = true;
            }
            if (!jCheckBoxMenuItemPause.isSelected() && !ignoreTitleErrors.get()) {
                String titleErr = aprsJframe.getTitleErrorString();
                if (titleErr != null
                        && titleErr.length() > 0
                        && !titleErr.equals(titleErrorMap.replace(aprsJframe.getMyThreadId(), titleErr))) {
                    logEvent(aprsJframe + " has title error " + titleErr);
                    logEvent(aprsJframe + " title error trace=" + shortTrace(aprsJframe.getSetTitleErrorStringTrace()));
                }
                if (aprsJframe.isPaused() && jCheckBoxMenuItemPauseAllForOne.isSelected()) {
                    logEvent(aprsJframe + " is paused");
                    pause();
                }
            }
            tm.addRow(new Object[]{aprsJframe.getPriority(), taskName, aprsJframe.getRobotName(), aprsJframe.getDetailsString(), aprsJframe.getPropertiesFile()});
        }
        if (needSetJListFuturesModel) {
            setJListFuturesModel();
            jListFutures.setSelectedIndex(0);
        }
        Utils.autoResizeTableColWidths(jTableTasks);
        Utils.autoResizeTableRowHeights(jTableTasks);
        DefaultTreeModel model = (DefaultTreeModel) jTreeSelectedFuture.getModel();
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(null);
        model.setRoot(rootTreeNode);
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay();
        }
    }

    private void setJListFuturesModel() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Main");
        listModel.addElement("Last");
        listModel.addElement("Resume");
        listModel.addElement("Random");
        listModel.addElement("continousDemo");
        listModel.addElement("stealAbort");
        listModel.addElement("unstealAbort");
        for (AprsJFrame aprsJframe : aprsSystems) {
            listModel.addElement(aprsJframe.getTaskName() + "/actions");
            listModel.addElement(aprsJframe.getTaskName() + "/abort");
            listModel.addElement(aprsJframe.getTaskName() + "/resume");
            listModel.addElement(aprsJframe.getTaskName() + "/program");
        }
        jListFutures.setModel(listModel);
    }
    private volatile Supplier<XFuture> futureToDisplaySupplier = () -> mainFuture;

    private void updateCurrentFutureDisplay() {
        XFuture<?> xf = futureToDisplaySupplier.get();
        if (null != xf) {
            DefaultTreeModel model = (DefaultTreeModel) jTreeSelectedFuture.getModel();
            DefaultMutableTreeNode rootTreeNode = xfutureToNode(xf);
            model.setRoot(rootTreeNode);
            expandAllNodes(jTreeSelectedFuture, 0, jTreeSelectedFuture.getRowCount());
        }
    }

    private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }

        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    private Field getField(Class clss, String name) {
        Field f = null;
        try {
            f = clss.getField(name);
        } catch (NoSuchFieldException | SecurityException ex) {
//            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (null != f) {
            return f;
        }
//        System.out.println("clss = " + clss);
        Field fields[] = clss.getFields();

//        System.out.println("fields = " + Arrays.toString(fields));
        Field declaredFields[] = clss.getDeclaredFields();
//        System.out.println("declaredFields = " + Arrays.toString(declaredFields));
        for (Field fi : fields) {
            if (fi.getName().equals(name)) {
                return fi;
            }
        }
        for (Field dfi : declaredFields) {
            if (dfi.getName().equals(name)) {
                return dfi;
            }
        }
        try {
            f = clss.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            try {
                f = clss.getField(name);

            } catch (NoSuchFieldException | SecurityException ex1) {
//                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (SecurityException ex) {
//            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (f == null && clss.getSuperclass() != null && !Objects.equals(clss.getSuperclass(), Object.class
        )) {
            return getField(clss.getSuperclass(), name);
        }
        return f;
    }

    private DefaultMutableTreeNode cfutureToNode(CompletableFuture future) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);
        Class clss = future.getClass();
        Field stackField = getField(clss, "stack");
        if (null != stackField) {
            try {
                stackField.setAccessible(true);
                Object stackFieldObject = stackField.get(future);
                addNodesForStackObject(stackFieldObject, future, node);
            } catch (IllegalArgumentException ex) {
                log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                log(Level.SEVERE, null, ex);
            }
        }
        return node;
    }

    private void addNodesForStackObject(Object stackFieldObject, CompletableFuture future, DefaultMutableTreeNode node) throws SecurityException {
        if (null != stackFieldObject) {
            Class stackFieldClass = stackFieldObject.getClass();
            Field depField = getField(stackFieldClass, "dep");
            if (null != depField) {
                try {
                    depField.setAccessible(true);
                    Object depFieldObject = depField.get(stackFieldObject);
                    if (depFieldObject != future) {
                        if (depFieldObject instanceof XFuture) {
                            XFuture xf = (XFuture) depFieldObject;
                            if (jCheckBoxShowDoneFutures.isSelected()
                                    || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                                node.add(xfutureToNode(xf));
                            }
                        } else if (depFieldObject instanceof CompletableFuture) {
                            CompletableFuture cf = (CompletableFuture) depFieldObject;
                            boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                            if (jCheckBoxShowUnnamedFutures.isSelected() || notOk) {
                                if (jCheckBoxShowDoneFutures.isSelected()
                                        || (!cf.isDone() || notOk)) {
                                    node.add(cfutureToNode(cf));
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Field nextField = getField(stackFieldClass, "next");
            if (null != nextField) {
                try {
                    nextField.setAccessible(true);
                    Object nextFieldObject = nextField.get(stackFieldObject);
                    if (null != nextFieldObject) {
                        Class nextFieldClass = nextFieldObject.getClass();
                        Field nextFieldStackField = getField(nextFieldClass, "stack");
                        if (null != nextFieldStackField) {
                            Object nextFielStackObject = nextFieldStackField.get(nextFieldObject);
                            addNodesForStackObject(nextFielStackObject, future, node);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private DefaultMutableTreeNode xfutureToNode(XFuture future) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);
        if (null != future) {
            ConcurrentLinkedDeque deque = future.getAlsoCancel();
            if (null != deque) {
                for (Object o : deque) {
                    if (o instanceof XFuture) {
                        XFuture xf = (XFuture) o;
                        if (jCheckBoxShowDoneFutures.isSelected()
                                || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                            node.add(xfutureToNode(xf));
                        }
                    } else if (o instanceof CompletableFuture) {
                        CompletableFuture cf = (CompletableFuture) o;
                        boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                        if (jCheckBoxShowUnnamedFutures.isSelected() || notOk) {
                            if (jCheckBoxShowDoneFutures.isSelected()
                                    || (!cf.isDone() || notOk)) {
                                node.add(cfutureToNode(cf));
                            }
                        }
                    }
                }
            }
        }
        return node;
    }

    private void updateRobotsTable()  {
        Map<String, AprsJFrame> robotMap = new HashMap<>();
        robotEnableMap.clear();
        DefaultTableModel tm = (DefaultTableModel) jTableRobots.getModel();
        DefaultComboBoxModel<String> cbmModel = (DefaultComboBoxModel<String>) jComboBoxTeachSystemView.getModel();
        cbmModel.removeAllElements();
        cbmModel.addElement("All");
        cbmModel.setSelectedItem("All");
        tm.setRowCount(0);
        for (AprsJFrame aprsJframe : aprsSystems) {
            robotMap.put(aprsJframe.getRobotName(), aprsJframe);
            robotEnableMap.put(aprsJframe.getRobotName(), true);
            cbmModel.addElement(aprsJframe.getMyThreadId() + " : " + aprsJframe.toString());
        }
        robotMap.forEach((robotName, aprs) -> {
            tm.addRow(new Object[]{
                robotName,
                true,
                aprs.getRobotCrclHost(),
                aprs.getRobotCrclPort(),
                robotDisableCountMap.getOrDefault(robotName, 0),
                runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L))
            });
        });
        Utils.autoResizeTableColWidths(jTableRobots);
        if (aprsSystems.size() >= 2) {
            colorTextJPanel1.setLabelsAndIcons(
                    aprsSystems.get(0).getRobotName(),
                    ColorTextJPanel.getRobotIcon(aprsSystems.get(0).getRobotName()),
                    aprsSystems.get(1).getRobotName(),
                    ColorTextJPanel.getRobotIcon(aprsSystems.get(1).getRobotName()));
        } else if (aprsSystems.size() == 1) {
            colorTextJPanel1.setLabelsAndIcons(
                    aprsSystems.get(0).getRobotName(),
                    ColorTextJPanel.getRobotIcon(aprsSystems.get(0).getRobotName()),
                    "",
                    null);
        } else {
            colorTextJPanel1.setLabelsAndIcons(
                    "",
                    null,
                    "",
                    null);
        }
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            getLogger().log(java.util.logging.Level.SEVERE, null, ex);

        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AprsSupervisorJFrame amsFrame = new AprsSupervisorJFrame();
                amsFrame.startColorTextReader();
                amsFrame.loadPrevSetup();
                amsFrame.loadPrevPosMapFile();
                amsFrame.loadPrevSimTeach();
                amsFrame.loadPrevTeachProperties();
                amsFrame.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.framework.colortextdisplay.ColorTextJPanel colorTextJPanel1;
    private javax.swing.JButton jButtonAddLine;
    private javax.swing.JButton jButtonDeleteLine;
    private javax.swing.JButton jButtonFuturesCancelAll;
    private javax.swing.JButton jButtonSaveSelectedPosMap;
    private javax.swing.JButton jButtonSetInFromCurrent;
    private javax.swing.JButton jButtonSetOutFromCurrent;
    private javax.swing.JCheckBox jCheckBoxFutureLongForm;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContDemoReverseFirst;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinousDemoRevFirst;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebugStartReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDisableTextPopups;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemFixedRandomTestSeed;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIndContinousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIndRandomToggleTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseAllForOne;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseResumeTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRandomTest;
    private javax.swing.JMenuItem jCheckBoxMenuItemRandomTestReverseFirst;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowSplashMessages;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemUseTeachCamera;
    private javax.swing.JCheckBox jCheckBoxShowDoneFutures;
    private javax.swing.JCheckBox jCheckBoxShowUnnamedFutures;
    private javax.swing.JCheckBox jCheckBoxUpdateFutureAutomatically;
    private javax.swing.JComboBox<String> jComboBoxTeachSystemView;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JList<String> jListFutures;
    private javax.swing.JList<String> jListFuturesKey;
    private javax.swing.JMenu jMenuActions;
    private javax.swing.JMenu jMenuActionsAdditionalTests;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemAddExistingSystem;
    private javax.swing.JMenuItem jMenuItemAddNewSystem;
    private javax.swing.JMenuItem jMenuItemConnectAll;
    private javax.swing.JMenuItem jMenuItemContinueAll;
    private javax.swing.JMenuItem jMenuItemDbgAction;
    private javax.swing.JMenuItem jMenuItemImmediateAbortAll;
    private javax.swing.JMenuItem jMenuItemLoadPosMaps;
    private javax.swing.JMenuItem jMenuItemLoadSetup;
    private javax.swing.JMenuItem jMenuItemRemoveSelectedSystem;
    private javax.swing.JMenuItem jMenuItemResetAll;
    private javax.swing.JMenuItem jMenuItemRunCustom;
    private javax.swing.JMenuItem jMenuItemSafeAbortAll;
    private javax.swing.JMenuItem jMenuItemSaveAll;
    private javax.swing.JMenuItem jMenuItemSavePosMaps;
    private javax.swing.JMenuItem jMenuItemSaveSetup;
    private javax.swing.JMenuItem jMenuItemSaveSetupAs;
    private javax.swing.JMenuItem jMenuItemScanAll;
    private javax.swing.JMenuItem jMenuItemStartAll;
    private javax.swing.JMenuItem jMenuItemStartAllReverse;
    private javax.swing.JMenuItem jMenuItemStartColorTextDisplay;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelFuture;
    private javax.swing.JPanel jPanelPosMapFiles;
    private javax.swing.JPanel jPanelPositionMappings;
    private javax.swing.JPanel jPanelRobots;
    private javax.swing.JPanel jPanelTasks;
    private javax.swing.JPanel jPanelTasksAndRobots;
    private javax.swing.JPanel jPanelTeachTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPaneRobots;
    private javax.swing.JScrollPane jScrollPaneTasks;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTableEvents;
    private javax.swing.JTable jTablePositionMappings;
    private javax.swing.JTable jTableRobots;
    private javax.swing.JTable jTableSelectedPosMapFile;
    private javax.swing.JTable jTableTasks;
    private javax.swing.JTextField jTextFieldEventsMax;
    private javax.swing.JTextField jTextFieldRobotEnableToggleBlockers;
    private javax.swing.JTextField jTextFieldRunningTime;
    private javax.swing.JTextField jTextFieldSelectedPosMapFilename;
    private javax.swing.JTree jTreeSelectedFuture;
    private aprs.framework.simview.Object2DOuterJPanel object2DOuterJPanel1;
    // End of variables declaration//GEN-END:variables
}
