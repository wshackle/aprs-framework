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
 * This software can be redistributed and/or modified freely provided show
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.supervisor.main;

import aprs.launcher.LauncherAprsJFrame;
import aprs.misc.Utils;
import aprs.misc.MultiFileDialogJPanel;
import static aprs.misc.Utils.runTimeToString;
import static aprs.misc.Utils.tableHeaders;

import aprs.supervisor.colortextdisplay.ColorTextOptionsJPanel;
import aprs.supervisor.colortextdisplay.ColorTextOptionsJPanel.ColorTextOptions;
import aprs.supervisor.colortextdisplay.ColorTextJFrame;
import aprs.supervisor.colortextdisplay.ColorTextJPanel;
import aprs.database.PhysicalItem;
import aprs.actions.executor.PositionMap;
import aprs.actions.executor.PositionMapEntry;
import aprs.launcher.ProcessLauncherJFrame;
import aprs.misc.Utils.UiSupplier;
import aprs.supervisor.screensplash.SplashScreen;
import aprs.simview.Object2DOuterJPanel;
import aprs.system.AprsSystem;

import crcl.base.PoseType;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import crcl.ui.misc.MultiLineStringJPanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
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
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("ALL")
class AprsSupervisorDisplayJFrame extends javax.swing.JFrame {

    @MonotonicNonNull
    Supervisor supervisor = null;

    @Nullable
    public Supervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;

    }

    private ExecutorService getSupervisorExecutorService() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getSupervisorExecutorService();
    }

    private boolean isTogglesAllowed() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.isTogglesAllowed();
    }

    private class RobotTableModelListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            try {
                handleRobotTableChange(e.getFirstRow(), e.getLastRow(), e.getColumn(), e.getType(), e.getSource());
            } catch (Exception exception) {
                log(Level.SEVERE, "", exception);
            }
        }
    }

    private volatile int ignoreRobotTableChangesCount = 0;
    private volatile int handleRobotTableChangesCount = 0;

    private void handleRobotTableChange(int firstRow, int lastRow, int col, int type, Object source) {

        if (null == robotEnableMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }
//        if (jTableRobots.getRowCount() > 0) {
//            System.out.println("handleRobotTableChange: firstRow=" + firstRow + ",lastRow=" + lastRow + ",jTableRobots.getValueAt(" + firstRow + ",1) = " + jTableRobots.getValueAt(firstRow, 1));
//        }
        if (type != TableModelEvent.UPDATE) {
            System.out.println("handleRobotTableChange: ignoring event of type = " + type);
        }
        if (col != 1) {
            System.out.println("handleRobotTableChange: ignoring event for col=  " + col);
        }
        if (ignoreRobotTableChanges) {
            ignoreRobotTableChangesCount++;
//            System.out.println("ignoreRobotTableChangesCount = " + ignoreRobotTableChangesCount);
//            flushTableRobotEventDeque(false);
            return;
        }
        handleRobotTableChangesCount++;
        boolean changeFound = false;
        disableRobotTableModelListener();
        for (int i = firstRow; i < jTableRobots.getRowCount() && i <= lastRow; i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            if (null == robotName) {
                throw new IllegalStateException("null == robotName : jTableRobots.getValueAt(" + i + ", 0)");
            }
            boolean enabled = getEnableFromRobotsTable(i);
            boolean wasEnabled = robotEnableMap.getOrDefault(robotName, enabled);
            final String checkedRobotName = robotName;
            System.out.println("handleRobotTableChange: i=" + i + ",robotName=" + robotName + ",enabled=" + enabled + ",wasEnabled=" + wasEnabled);
            if (enabled != wasEnabled) {
                final int fi = i;
                if (isTogglesAllowed()) {
                    XFuture.runAsync(() -> {
                        if (isTogglesAllowed()) {
                            setRobotEnabled(checkedRobotName, enabled);
//                            flushTableRobotEventDeque(true);
                        } else {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                logEvent("Attempt to toggle robot enabled ignored.");
                                disableRobotTableModelListener();
                                System.out.println("handleRobotTableChange calling jTableRobots.setValueAt(" + wasEnabled + "," + fi + ", 1)");
                                jTableRobots.setValueAt(wasEnabled, fi, 1);
//                                flushTableRobotEventDeque(false);
                                enableRobotTableModelListener();
                            });
                        }
                    }, getSupervisorExecutorService());
                } else {
                    logEvent("Attempt to toggle robot enabled ignored.");
                    disableRobotTableModelListener();
                    System.out.println("handleRobotTableChange calling jTableRobots.setValueAt(" + wasEnabled + "," + fi + ", 1)");
                    jTableRobots.setValueAt(wasEnabled, fi, 1);
//                    flushTableRobotEventDeque(false);
                    enableRobotTableModelListener();
                }
                break;
            } else {
//                System.err.println("event triggered no change.");
            }
        }
//        flushTableRobotEventDeque(false);
        enableRobotTableModelListener();
    }

    private final RobotTableModelListener robotTableModelListener = new RobotTableModelListener();

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    @SuppressWarnings("initialization")
    public AprsSupervisorDisplayJFrame() {
        initComponents();
        tasksTableRobotImageCellRenderer = new LabelledImagePanelTableCellRenderer();
        jTableTasks.getColumnModel().getColumn(2).setCellRenderer(tasksTableRobotImageCellRenderer);
        tasksTableScanImageCellRenderer = new ImagePanelTableCellRenderer();
        jTableTasks.getColumnModel().getColumn(3).setCellRenderer(tasksTableScanImageCellRenderer);
        tasksTableLiveImageCellRenderer = new ImagePanelTableCellRenderer();
        jTableTasks.getColumnModel().getColumn(4).setCellRenderer(tasksTableLiveImageCellRenderer);
        tasksTableDetailsCellRenderer = new TextAreaTableCellRenderer();
        jTableTasks.getColumnModel().getColumn(5).setCellRenderer(tasksTableDetailsCellRenderer);
        tasksTableDetailsCellEditor = new TextAreaTableCellEditor(jTableTasks);
        jTableTasks.getColumnModel().getColumn(5).setCellEditor(tasksTableDetailsCellEditor);
        jTablePositionMappings.getSelectionModel().addListSelectionListener(x -> updateSelectedPosMapFileTable());
        jTableSelectedPosMapFile.getModel().addTableModelListener(tableSelectedPosMapFileModelListener);
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
        Utils.autoResizeTableColWidths(jTablePositionMappings);
        Utils.autoResizeTableRowHeights(jTablePositionMappings);

        jListFutures.addListSelectionListener(jTableTasks);
        jListFutures.addListSelectionListener(jListFuturesSelectionListener);
        jTreeSelectedFuture.setCellRenderer(treeSelectedFutureCellRenderer);
        jListFuturesKey.setCellRenderer(listFuturesKeyCellRenderer);
        jTableRobots.getColumnModel().getColumn(1).setCellRenderer(robotsTableEnableColumnCellRenderer);
        enableSharedToolTableModelListener();
    }

    private volatile boolean sharedToolTableModelListenerEnabled = false;

    public boolean isSharedToolTableModelListenerEnabled() {
        return sharedToolTableModelListenerEnabled;
    }

    public void enableSharedToolTableModelListener() {
        jTableSharedTools.getModel().addTableModelListener(sharedToolsTableModelListener);
    }

    public void disableSharedToolTableModelListener() {
        jTableSharedTools.getModel().removeTableModelListener(sharedToolsTableModelListener);
    }

    private final TableModelListener sharedToolsTableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            try {
//                Utils.saveTableModel(supervisor.getSharedToolsFile(), jTableSharedTools);
                Utils.autoResizeTableColWidths(jTableSharedTools);
            } catch (Exception ex) {
                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    };

    private static class LabelledImagePanelTableCellRenderer extends DefaultTableCellRenderer {

        private final List<ImagePanel> areas = new ArrayList<>();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (value instanceof String) {
                try {
                    String robotName = (String) value;
                    BufferedImage image = ColorTextJPanel.getRobotImage(robotName);
                    while (areas.size() <= row) {
                        ImagePanel area = new ImagePanel(image, robotName);
                        area.setOpaque(true);
                        area.setVisible(true);
                        areas.add(area);
                    }
                    ImagePanel area = areas.get(row);
                    if (null != area) {
                        area.setImage(image);
                        area.setLabel(robotName);
                    }
                    return area;
                } catch (Exception ex) {
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private final LabelledImagePanelTableCellRenderer tasksTableRobotImageCellRenderer;

    private static class ImagePanelTableCellRenderer extends DefaultTableCellRenderer {

        private final List<ImagePanel> areas = new ArrayList<>();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof BufferedImage) {
                while (areas.size() <= row) {
                    ImagePanel area = new ImagePanel((BufferedImage) value);
                    area.setOpaque(true);
                    area.setVisible(true);
                    areas.add(area);
                }
                ImagePanel area = areas.get(row);
                if (null != value && null != area) {
                    area.setImage((BufferedImage) value);
                }
                return area;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private final ImagePanelTableCellRenderer tasksTableScanImageCellRenderer;
    private final ImagePanelTableCellRenderer tasksTableLiveImageCellRenderer;

    private static class TextAreaTableCellRenderer extends DefaultTableCellRenderer {

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
    }

    private final TextAreaTableCellRenderer tasksTableDetailsCellRenderer;

    private static class TextAreaTableCellEditor implements TableCellEditor {

        public TextAreaTableCellEditor(JTable jTable) {
            this.jTable = jTable;
        }

        private final JTable jTable;
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
                    l.editingStopped(new ChangeEvent(jTable));
                }
            }
            return true;
        }

        @Override
        public void cancelCellEditing() {
            for (int i = 0; i < listeners.size(); i++) {
                CellEditorListener l = listeners.get(i);
                if (null != l) {
                    l.editingCanceled(new ChangeEvent(jTable));
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
    }

    private final TextAreaTableCellEditor tasksTableDetailsCellEditor;

    private final TableModelListener tableSelectedPosMapFileModelListener
            = new TableModelListener() {
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
    };
    private final DefaultTreeCellRenderer treeSelectedFutureCellRenderer
            = new DefaultTreeCellRenderer() {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(
                    tree, value, selected,
                    expanded, leaf, row,
                    hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof XFuture) {
                    @SuppressWarnings("unchecked")
                    XFuture<?> xf = (XFuture<?>) userObject;
                    if (!jCheckBoxFutureLongForm.isSelected()) {
                        long runTime = xf.getRunTime();
                        setText(xf.getName() + " (" + (runTime / 1000) + " s) ");
                    } else {
                        setText(xf.toString());
                    }
                    setIcon(null);
                    XFuture<?> cancelledDependant = xf.getCanceledDependant();
                    if (xf.isCancelled() || cancelledDependant != null) {
                        setBackground(Color.YELLOW);
                        if (null != cancelledDependant) {
                            setText(xf.getName() + " : " + cancelledDependant.cancelString());
                        }
                    } else if (xf.isCompletedExceptionally()) {
                        setBackground(Color.RED);
                        Throwable t = ((XFuture<?>) xf).getThrowable();
                        setText(xf.getName() + " : " + t.toString());
                    } else if (xf.isDone()) {
                        setBackground(Color.CYAN);
                    } else {
                        setBackground(Color.MAGENTA);
                    }
                    setOpaque(true);
                } else if (userObject instanceof CompletableFuture) {
                    CompletableFuture<?> cf = (CompletableFuture<?>) userObject;
                    setText(cf.toString());
                    setIcon(null);
                    if (cf.isCancelled()) {
                        setBackground(Color.YELLOW);
                    } else if (cf.isCompletedExceptionally()) {
                        setBackground(Color.RED);
                        cf.exceptionally((Throwable t) -> {
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
    };
    private final DefaultListCellRenderer listFuturesKeyCellRenderer
            = new DefaultListCellRenderer() {
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
    };

    private final DefaultTableCellRenderer robotsTableEnableColumnCellRenderer
            = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Boolean) {
                while (robotsEnableCelRendererComponentList.size() < row + 1) {
                    JCheckBox chkboxToAdd = new JCheckBox();

                    chkboxToAdd.setSelected(true);
                    robotsEnableCelRendererComponentList.add(chkboxToAdd);
                }
                JCheckBox chkbox = robotsEnableCelRendererComponentList.get(row);
                boolean val = (value instanceof Boolean)
                        ? (Boolean) value
                        : true;
                chkbox.setSelected(val);
                return chkbox;
            } else {
                throw new IllegalArgumentException("value=" + value);
            }
        }
    };

    private void enableRobotTableModelListener() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            ignoreRobotTableChanges = false;
        });
        //jTableRobots.getModel().addTableModelListener(robotTableModelListener);
    }

//    private void flushTableRobotEventDeque(boolean val) {
//        SetTableRobotEnabledEvent setTableRobotEnabledEvent = null;
//        while (null != (setTableRobotEnabledEvent = setTableRobotEnabledEventDeque.pollFirst())) {
//            setTableRobotEnabledEvent.getFuture().complete(val);
//        }
//    }
    public void setDefaultIconImage() {
        try {
            URL url = LauncherAprsJFrame.class
                    .getResource("aprs.png");
            if (null != url) {
                BufferedImage iconImg
                        = ImageIO.read(url);
                if (null != iconImg) {
                    setIconImage(iconImg);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class
                    .getName()).log(Level.SEVERE, "", ex);
        }
    }

//    private final List<JCheckBox> robotsEnableCelEditorCheckBoxList = new ArrayList<>();
    private final List<JCheckBox> robotsEnableCelRendererComponentList = new ArrayList<>();

    private final ListSelectionListener jListFuturesSelectionListener
            = this::handleListFuturesSelectionEvent;

    List<AprsSystem> getAprsSystems() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getAprsSystems();
    }

    private void handleListFuturesSelectionEvent(@UnknownInitialization AprsSupervisorDisplayJFrame this,ListSelectionEvent e) {
        if (null == jListFutures) {
            return;
        }

        String selectedFutureString = jListFutures.getSelectedValue();
        if (null == selectedFutureString) {
            return;
        }
        if (null == supervisor) {
            return;
        }
        Supervisor sup2 = supervisor; // needed for CheckerFramework, supervisor might be null in lambdas, CheckerFramework knows sup2 is not null because of previous check.
        switch (selectedFutureString) {
            case "Main":
                futureToDisplaySupplier = () -> sup2.getMainFuture();
                break;

            case "Last":
                futureToDisplaySupplier = () -> lastFutureReturned;
                break;

            case "Resume":
                futureToDisplaySupplier = () -> sup2.getResumeFuture();
                break;

            case "Random":
                futureToDisplaySupplier = () -> sup2.getRandomTestFuture();
                break;

            case "ContinuousDemo":
                futureToDisplaySupplier = () -> sup2.getContinuousDemoFuture();
                break;

            case "stealAbort":
                futureToDisplaySupplier = () -> sup2.getStealAbortFuture();
                break;

            case "unstealAbort":
                futureToDisplaySupplier = () -> sup2.getUnstealAbortFuture();
                break;
        }
        List<AprsSystem> aprsSystems = sup2.getAprsSystems();
        int sindex = selectedFutureString.indexOf('/');
        if (sindex > 0 && sindex < selectedFutureString.length()) {
            String selectedFutureStringBase = selectedFutureString.substring(0, sindex);
            String selectedFutureStringExt = selectedFutureString.substring(sindex + 1);
            for (AprsSystem sys : aprsSystems) {
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
        boolean showDoneFutures = false;
        if (jCheckBoxShowDoneFutures != null) {
            showDoneFutures = jCheckBoxShowDoneFutures.isSelected();
        }
        boolean showUnnamedFutures = false;
        if (null != jCheckBoxShowUnnamedFutures) {
            showUnnamedFutures = jCheckBoxShowUnnamedFutures.isSelected();
        }
        updateCurrentFutureDisplay(showDoneFutures, showUnnamedFutures);
    }

    @Nullable
    private XFutureVoid getRandomTestFuture() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getRandomTestFuture();
    }

    public void setRandomTestFuture(@Nullable XFutureVoid randomTestFuture) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setRandomTestFuture(randomTestFuture);
    }

    @Nullable
    private XFutureVoid getResumeFuture() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getResumeFuture();
    }

    /**
     * Start a reader so that the text and color of the panels at the bottom
     * right showing the status of the robots can be remotely controlled through
     * a simple socket.
     */
    public void startColorTextReader() {
        this.colorTextJPanel1.startReader();
    }

    public void stopColorTextReader() {
        this.colorTextJPanel1.stopReader();
    }

    /**
     * Reload the last saved/used setup.
     */
    private XFutureVoid loadAllPrevFiles() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.loadAllPrevFiles();
    }

    JPanel blankPanel = new JPanel();

    @Nullable
    private AprsSystem findSystemWithRobot(String robot) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.findSystemWithRobot(robot);
    }

    private void updateSelectedPosMapFileTable() {
        int row = jTablePositionMappings.getSelectedRow();
        int col = jTablePositionMappings.getSelectedColumn();
        jButtonSetInFromCurrent.setEnabled(false);
        jButtonSetOutFromCurrent.setEnabled(false);
        if (row >= 0 && row < jTablePositionMappings.getRowCount() && col > 0 && col < jTablePositionMappings.getColumnCount()) {
            try {
                String inSys = (String) jTablePositionMappings.getValueAt(row, 0);
                if (null == inSys || inSys.length() < 1) {
                    logEventErr("No inSys entry in jTablePositionMappings for row=" + row);
                    return;
                }
                String outSys = jTablePositionMappings.getColumnName(col);
                if (null == outSys || outSys.length() < 1) {
                    logEventErr("No outSys entry in jTablePositionMappings for row=" + row);
                    return;
                }
                AprsSystem posMapInSys = findSystemWithRobot(inSys);

                if (null == posMapInSys) {
                    throw new IllegalStateException("findSystemWithRobot(" + inSys + ") returned null");
                }
                jButtonSetInFromCurrent.setText("Set In From " + posMapInSys.getRobotName());
                jButtonSetInFromCurrent.setEnabled(true);
                setPosMapInSys(posMapInSys);
                AprsSystem posMapOutSys = findSystemWithRobot(outSys);
                if (null == posMapOutSys) {
                    throw new IllegalStateException("findSystemWithRobot(" + outSys + ") returned null");
                }
                jButtonSetOutFromCurrent.setText("Set Out From " + posMapOutSys.getRobotName());
                jButtonSetOutFromCurrent.setEnabled(true);
                setPosMapOutSys(posMapOutSys);
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
                log(Level.SEVERE, "", ex);
            }

        }
    }

    private void setRobotEnabled(String robotName, Boolean enabled) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setRobotEnabled(robotName, enabled);
    }

    public void setPauseSelected(boolean selected) {
        jCheckBoxMenuItemPause.setSelected(selected);
    }

    public boolean isPauseSelected() {
        return jCheckBoxMenuItemPause.isSelected();
    }

    public void setPauseAllForOneSelected(boolean selected) {
        jCheckBoxMenuItemPauseAllForOne.setSelected(selected);
    }

    public boolean isPauseAllForOneSelected() {
        return jCheckBoxMenuItemPauseAllForOne.isSelected();
    }

    public void setContinuousDemoSelected(boolean selected) {
        jCheckBoxMenuItemContinuousDemo.setSelected(selected);
    }

    public boolean isContinuousDemoSelected() {
        return jCheckBoxMenuItemContinuousDemo.isSelected();
    }

    public void setUseTeachCameraSelected(boolean selected) {
        jCheckBoxMenuItemUseTeachCamera.setSelected(selected);
    }

    public boolean isUseTeachCameraSelected() {
        return jCheckBoxMenuItemUseTeachCamera.isSelected();
    }

    public void setIndContinuousDemoSelected(boolean selected) {
        jCheckBoxMenuItemIndContinuousDemo.setSelected(selected);
    }

    public boolean isIndContinuousDemoSelected() {
        return jCheckBoxMenuItemIndContinuousDemo.isSelected();
    }

    public void setIndRandomToggleTestSelected(boolean selected) {
        jCheckBoxMenuItemIndRandomToggleTest.setSelected(selected);
    }

    public boolean isIndRandomToggleTestSelected() {
        return jCheckBoxMenuItemIndRandomToggleTest.isSelected();
    }

    public void setRandomTestSelected(boolean selected) {
        jCheckBoxMenuItemRandomTest.setSelected(selected);
    }

    public boolean isRandomTestSelected() {
        return jCheckBoxMenuItemRandomTest.isSelected();
    }

    public void setPauseResumeTestSelected(boolean selected) {
        jCheckBoxMenuItemPauseResumeTest.setSelected(selected);
    }

    public boolean isPauseResumeTestSelected() {
        return jCheckBoxMenuItemPauseResumeTest.isSelected();
    }
    //jCheckBoxMenuItemKeepAndDisplayXFutureProfiles

    public void setContinuousDemoRevFirstSelected(boolean selected) {
        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(selected);
    }

    public boolean isContinuousDemoRevFirstSelected() {
        return jCheckBoxMenuItemContinuousDemoRevFirst.isSelected();
    }

    public void setKeepAndDisplayXFutureProfilesSelected(boolean selected) {
        jCheckBoxMenuItemKeepAndDisplayXFutureProfiles.setSelected(selected);
    }

    public boolean isKeepAndDisplayXFutureProfilesSelected() {
        return jCheckBoxMenuItemKeepAndDisplayXFutureProfiles.isSelected();
    }

    public void setPauseCount(int count) {
        jCheckBoxMenuItemPause.setText("Pause (" + count + ") ");
    }

//    private int getRobotDisableCount(String robotName) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.getRobotDisableCount(robotName);
//    }
//
//    private long getRobotDisableTotalTime(String robotName) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.getRobotDisableTotalTime(robotName);
//    }
    public void refreshRobotsTable(Map<String, Boolean> robotEnableMap, Map<String, Integer> robotDisableCountMap, Map<String, Long> robotDisableTotalTimeMap) {
        if (null == robotEnableMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }

        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            if (null != robotName) {
                boolean enabledInMap = robotEnableMap.getOrDefault(robotName, true);
                boolean enabledInTable = getEnableFromRobotsTable(i);
                if (enabledInMap != enabledInTable) {
                    System.out.println("refreshRobotTable setValueAt(" + enabledInMap + "," + i + ",1) robotName=" + robotName);
                    disableRobotTableModelListener();
                    jTableRobots.setValueAt(enabledInMap, i, 1);
                    enableRobotTableModelListener();
                }
                int mapDisableCount = robotDisableCountMap.getOrDefault(robotName, 0);
                int tableDisableCount = getDisableCountFromRobotsTable(i);
                if (mapDisableCount != tableDisableCount) {
                    System.out.println("refreshRobotTable setValueAt(" + mapDisableCount + "," + i + ",4) robotName=" + robotName);
                    disableRobotTableModelListener();
                    jTableRobots.setValueAt(mapDisableCount, i, 4);
                    enableRobotTableModelListener();
                }
                if (mapDisableCount != tableDisableCount || !enabledInMap || !enabledInTable) {
                    disableRobotTableModelListener();
                    jTableRobots.setValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 5);
                    enableRobotTableModelListener();
                }
            }
        }
        Utils.autoResizeTableColWidths(jTableRobots);
    }

    private int getDisableCountFromRobotsTable(int row) {
        Object o = jTableRobots.getValueAt(row, 4);
        if (o instanceof Integer) {
            return (Integer) o;
        } else {
            throw new IllegalStateException("jTableRobots.getValueAt(" + row + ", 4) contains " + o);
        }
    }

    private boolean getEnableFromRobotsTable(int row) {
        Object o = jTableRobots.getValueAt(row, 1);
        if (!(o instanceof Boolean)) {
            throw new IllegalStateException("jTableRobots.getValueAt(" + row + ", 1) returned " + o);
        }
        boolean enabledInTable = (Boolean) o;
        return enabledInTable;
    }

    private void setAbortTimeCurrent() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setAbortTimeCurrent();
    }

    private void checkRobotsUniquePorts() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.checkRobotsUniquePorts();
    }

    private void printReturnRobotTraceInfo() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.printReturnRobotTraceInfo();
    }

    private XFuture<@Nullable Void> returnRobots(String comment) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.returnRobots(comment);
    }

    private XFuture<@Nullable Void> returnRobotsDirect(String comment) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.returnRobotsDirect(comment);
    }

    private void logReturnRobotsNullRunnable(String comment) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.logReturnRobotsNullRunnable(comment);
    }

    private @Nullable
    XFutureVoid getContinuousDemoFuture() {
        if (null != supervisor) {
            return supervisor.getContinuousDemoFuture();
        } else {
            return null;
        }
    }

    private @Nullable
    XFuture<?> getMainFuture() {
        if (null != supervisor) {
            return supervisor.getMainFuture();
        } else {
            return null;
        }
    }

    private void setContinuousDemoFuture(XFutureVoid ContinuousDemoFuture) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setContinuousDemoFuture(ContinuousDemoFuture);
    }

    private void setMainFuture(XFuture<?> mainFuture) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setMainFuture(mainFuture);
    }

    private XFutureVoid showCheckEnabledErrorSplash() {
        return showErrorSplash("Not all robots\n could be enabled.")
                .thenRun(() -> {
                    Utils.runOnDispatchThread(() -> {
                        jCheckBoxMenuItemContinuousDemo.setSelected(false);
                        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
                        XFutureVoid ContinuousDemoFuture = getContinuousDemoFuture();
                        if (null != ContinuousDemoFuture) {
                            ContinuousDemoFuture.cancelAll(true);
                            ContinuousDemoFuture = null;
                        }
                    });
                });
    }

    public XFutureVoid showErrorSplash(String errMsgString) {
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        return showMessageFullScreen(errMsgString, 80.0f,
                null,
                SplashScreen.getRedYellowColorList(), gd);
    }

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
    public XFutureVoid showMessageFullScreen(String message, float fontSize, @Nullable Image image, List<Color> colors, GraphicsDevice graphicsDevice) {

        if (jCheckBoxMenuItemShowSplashMessages.isSelected()) {
            return forceShowMessageFullScreen(message, fontSize, image, colors, graphicsDevice);
        } else {
            logEvent("ignoring showMessageFullScreen " + message.replace('\n', ' '));
            return XFutureVoid.completedFutureWithName("jCheckBoxMenuItemShowSplashMessages.isSelected()== false");
        }
    }

    public XFutureVoid forceShowMessageFullScreen(String message, float fontSize, @Nullable Image image, List<Color> colors, GraphicsDevice graphicsDevice) {
        logEvent("showMessageFullScreen " + message.replace('\n', ' '));
        return SplashScreen.showMessageFullScreen(message, fontSize, image, colors, graphicsDevice);
    }

    private XFutureVoid stealRobot(AprsSystem stealFrom, AprsSystem stealFor) throws IOException, PositionMap.BadErrorMapFormatException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.stealRobot(stealFrom, stealFor);
    }

    private String assertFail() {
        logEvent("assertFail");
        pause();
        return "";
    }

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * Convert a timestamp in milliseconds since 1970 to the default time string
     * format.
     *
     * @param ms timestamp in milliseconds
     * @return formatted string
     */
    public static String getTimeString(long ms) {
        Date date = new Date(ms);
        return DEFAULT_DATE_FORMAT.format(date);
    }

    @Nullable
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

    volatile javax.swing.@Nullable Timer runTimeTimer = null;

    volatile int maxEventStringLen = 0;

    private long getFirstEventTime() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getFirstEventTime();
    }

    private void setFirstEventTime(long firstEventTime) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setFirstEventTime(firstEventTime);
    }

    private long getAbortEventTime() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getFirstEventTime();
    }

    private void setAbortEventTime(long abortEventTime) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setAbortEventTime(abortEventTime);
    }

    private void logEventPrivate(long time, String s, int blockerSize, String threadname) {

        if (getFirstEventTime() > 0) {
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
                Logger.getLogger(AprsSupervisorDisplayJFrame.class
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
        String fullLogString = timeString + " \t" + blockerSize + " \t" + s + " \t:thread= " + threadname;
        if (null != logPrintStream) {
            logPrintStream.println(fullLogString);
        }
        System.out.println(fullLogString);
        addEventToTable(time, blockerSize, s, threadname);
    }

    public void addEventToTable(long time, int blockerSize, String s, String threadname) {
        DefaultTableModel tm = (DefaultTableModel) jTableEvents.getModel();
        if (tm.getRowCount() > eventsDisplayMax) {
            tm.removeRow(0);
            maxEventStringLen = 0;
        }
        tm.addRow(new Object[]{getTimeString(time), blockerSize, s, threadname});
        if (tm.getRowCount() % 50 < 2 || s.length() > maxEventStringLen) {
            Utils.autoResizeTableColWidths(jTableEvents);
            maxEventStringLen = s.length();
        } else {
            scrollToEnd(jTableEvents);
        }
    }

    private void startUpdateRunningTimeTimer() {
        if (closing) {
            return;
        }
        if (runTimeTimer == null) {
            runTimeTimer = new Timer(2000, x -> updateRunningTime());
            runTimeTimer.start();
        }
    }

    public void updateRunningTime() {
        if (getFirstEventTime() > 0 && !jCheckBoxMenuItemPause.isSelected()) {

            long runningTimeMillis = System.currentTimeMillis() - getFirstEventTime();
            if (getFirstEventTime() < getAbortEventTime()) {
                runningTimeMillis = getAbortEventTime() - getFirstEventTime();
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

    /**
     * Log an event string to be displayed with timestamp in event table.
     *
     * @param s string to log
     */
    private void logEvent(String s) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.logEvent(s);
    }

    public void initColorTextSocket() throws IOException {
        if (null == colorTextSocket) {
            colorTextSocket = new Socket("localhost", ColorTextJPanel.COLORTEXT_SOCKET_PORT);
        }
    }

    public void writeToColorTextSocket(byte[] bytes) {
        if (null != colorTextSocket) {
            try {
                colorTextSocket.getOutputStream().write(bytes);
            } catch (Exception ex) {
                log(Level.SEVERE, "", ex);
            }
        }
    }
    @MonotonicNonNull private Map<String, Boolean> robotEnableMap;

    @Nullable
    public Map<String, Boolean> getRobotEnableMap() {
        return robotEnableMap;
    }

    public void setRobotEnableMap(Map<String, Boolean> robotEnableMap) {
        this.robotEnableMap = robotEnableMap;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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
        colorTextJPanel1 = new aprs.supervisor.colortextdisplay.ColorTextJPanel();
        jPanelPositionMappings = new javax.swing.JPanel();
        jPanelPosMapFiles = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablePositionMappings = new javax.swing.JTable();
        jPanelPosMapSelectedFile = new javax.swing.JPanel();
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
        jScrollPaneListFutures = new javax.swing.JScrollPane();
        jListFutures = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPaneTreeSelectedFuture = new javax.swing.JScrollPane();
        jTreeSelectedFuture = new javax.swing.JTree();
        jCheckBoxUpdateFutureAutomatically = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jScrollPaneListFuturesKey = new javax.swing.JScrollPane();
        jListFuturesKey = new javax.swing.JList<>();
        jCheckBoxShowDoneFutures = new javax.swing.JCheckBox();
        jCheckBoxShowUnnamedFutures = new javax.swing.JCheckBox();
        jButtonFuturesCancelAll = new javax.swing.JButton();
        jCheckBoxFutureLongForm = new javax.swing.JCheckBox();
        jPanelEvents = new javax.swing.JPanel();
        jScrollPaneEventsTable = new javax.swing.JScrollPane();
        jTableEvents = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldEventsMax = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldRunningTime = new javax.swing.JTextField();
        jPanelTeachTable = new javax.swing.JPanel();
        object2DOuterJPanel1 = new aprs.simview.Object2DOuterJPanel();
        jComboBoxTeachSystemView = new javax.swing.JComboBox<>();
        jPanelTools = new javax.swing.JPanel();
        jButtonAddSharedToolsRow = new javax.swing.JButton();
        jButtonDeleteSharedToolsRow = new javax.swing.JButton();
        jScrollPaneSharedToolsTable = new javax.swing.JScrollPane();
        jTableSharedTools = new javax.swing.JTable();
        jButtonSyncToolsFromRobots = new javax.swing.JButton();
        jButtonSyncToolsToRobots = new javax.swing.JButton();
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
        jCheckBoxMenuItemContinuousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemContinuousDemoRevFirst = new javax.swing.JCheckBoxMenuItem();
        jMenuItemScanAll = new javax.swing.JMenuItem();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jMenuActionsAdditionalTests = new javax.swing.JMenu();
        jCheckBoxMenuItemRandomTest = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPauseResumeTest = new javax.swing.JCheckBoxMenuItem();
        jMenuItemResetAll = new javax.swing.JMenuItem();
        jMenuItemDbgAction = new javax.swing.JMenuItem();
        jMenuItemRandomTestReverseFirst = new javax.swing.JMenuItem();
        jCheckBoxMenuItemIndContinuousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemIndRandomToggleTest = new javax.swing.JCheckBoxMenuItem();
        jMenuItemRunCustom = new javax.swing.JMenuItem();
        jMenuItemStartContinuousScanAndRun = new javax.swing.JMenuItem();
        jMenuItemStartScanAllThenContinuousDemoRevFirst = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenu();
        jCheckBoxMenuItemDisableTextPopups = new javax.swing.JCheckBoxMenuItem();
        jMenuItemStartColorTextDisplay = new javax.swing.JMenuItem();
        jCheckBoxMenuItemDebug = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemShowSplashMessages = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemFixedRandomTestSeed = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPauseAllForOne = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemContDemoReverseFirstOption = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemUseTeachCamera = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemKeepAndDisplayXFutureProfiles = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetMaxCycles = new javax.swing.JMenuItem();
        jCheckBoxMenuItemRecordLiveImageMovie = new javax.swing.JCheckBoxMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Multi Aprs Supervisor");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jTableTasks.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Priority", "Task(s)", "Robot(s)", "Scan Image", "Live Image", "Details", "PropertiesFile"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
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
                .addComponent(jScrollPaneTasks))
        );
        jPanelTasksLayout.setVerticalGroup(
            jPanelTasksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneTasks, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
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
                    .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE)
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
                .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldRobotEnableToggleBlockers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelTasksAndRobotsLayout = new javax.swing.GroupLayout(jPanelTasksAndRobots);
        jPanelTasksAndRobots.setLayout(jPanelTasksAndRobotsLayout);
        jPanelTasksAndRobotsLayout.setHorizontalGroup(
            jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                .addGroup(jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(colorTextJPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                        .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(1, 1, 1)))
                .addContainerGap())
        );
        jPanelTasksAndRobotsLayout.setVerticalGroup(
            jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelTasksAndRobotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(colorTextJPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
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

        jPanelPosMapSelectedFile.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected File"));

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

        javax.swing.GroupLayout jPanelPosMapSelectedFileLayout = new javax.swing.GroupLayout(jPanelPosMapSelectedFile);
        jPanelPosMapSelectedFile.setLayout(jPanelPosMapSelectedFileLayout);
        jPanelPosMapSelectedFileLayout.setHorizontalGroup(
            jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapSelectedFileLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanelPosMapSelectedFileLayout.createSequentialGroup()
                        .addComponent(jButtonSetInFromCurrent)
                        .addGap(50, 50, 50)
                        .addComponent(jButtonAddLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDeleteLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSaveSelectedPosMap)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 208, Short.MAX_VALUE)
                        .addComponent(jButtonSetOutFromCurrent))
                    .addComponent(jTextFieldSelectedPosMapFilename))
                .addContainerGap())
        );
        jPanelPosMapSelectedFileLayout.setVerticalGroup(
            jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapSelectedFileLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSetInFromCurrent)
                    .addComponent(jButtonAddLine)
                    .addComponent(jButtonDeleteLine)
                    .addComponent(jButtonSetOutFromCurrent)
                    .addComponent(jButtonSaveSelectedPosMap))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
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
                    .addComponent(jPanelPosMapSelectedFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelPosMapFiles, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelPositionMappingsLayout.setVerticalGroup(
            jPanelPositionMappingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPositionMappingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelPosMapFiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelPosMapSelectedFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Position Mapping", jPanelPositionMappings);

        jLabel1.setText("Futures");

        jListFutures.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Main" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPaneListFutures.setViewportView(jListFutures);

        jLabel2.setText("Details");

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTreeSelectedFuture.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPaneTreeSelectedFuture.setViewportView(jTreeSelectedFuture);

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
        jScrollPaneListFuturesKey.setViewportView(jListFuturesKey);

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
                    .addComponent(jScrollPaneListFutures, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addComponent(jScrollPaneListFuturesKey))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneTreeSelectedFuture)
                    .addGroup(jPanelFutureLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
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
                    .addComponent(jScrollPaneTreeSelectedFuture, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
                    .addGroup(jPanelFutureLayout.createSequentialGroup()
                        .addComponent(jScrollPaneListFutures)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPaneListFuturesKey, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
        jScrollPaneEventsTable.setViewportView(jTableEvents);

        jLabel4.setText("Max: ");

        jTextFieldEventsMax.setText("500         ");
        jTextFieldEventsMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldEventsMaxActionPerformed(evt);
            }
        });

        jLabel5.setText("Running Time : ");

        javax.swing.GroupLayout jPanelEventsLayout = new javax.swing.GroupLayout(jPanelEvents);
        jPanelEvents.setLayout(jPanelEventsLayout);
        jPanelEventsLayout.setHorizontalGroup(
            jPanelEventsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEventsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelEventsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneEventsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 1051, Short.MAX_VALUE)
                    .addGroup(jPanelEventsLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldEventsMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRunningTime)))
                .addContainerGap())
        );
        jPanelEventsLayout.setVerticalGroup(
            jPanelEventsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelEventsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelEventsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldEventsMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jTextFieldRunningTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneEventsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Events", jPanelEvents);

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
                    .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1051, Short.MAX_VALUE)
                    .addComponent(jComboBoxTeachSystemView, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelTeachTableLayout.setVerticalGroup(
            jPanelTeachTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTeachTableLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboBoxTeachSystemView, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Teach", jPanelTeachTable);

        jButtonAddSharedToolsRow.setText("Add Row");
        jButtonAddSharedToolsRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddSharedToolsRowActionPerformed(evt);
            }
        });

        jButtonDeleteSharedToolsRow.setText("Delete Row");
        jButtonDeleteSharedToolsRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteSharedToolsRowActionPerformed(evt);
            }
        });

        jTableSharedTools.setAutoCreateRowSorter(true);
        jTableSharedTools.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "System", "Holder/Current", "Contents", "PossibleContents", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneSharedToolsTable.setViewportView(jTableSharedTools);

        jButtonSyncToolsFromRobots.setText("Sync From Robots");
        jButtonSyncToolsFromRobots.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSyncToolsFromRobotsActionPerformed(evt);
            }
        });

        jButtonSyncToolsToRobots.setText("Sync To Robots");

        javax.swing.GroupLayout jPanelToolsLayout = new javax.swing.GroupLayout(jPanelTools);
        jPanelTools.setLayout(jPanelToolsLayout);
        jPanelToolsLayout.setHorizontalGroup(
            jPanelToolsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneSharedToolsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 1076, Short.MAX_VALUE)
                    .addGroup(jPanelToolsLayout.createSequentialGroup()
                        .addComponent(jButtonAddSharedToolsRow)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDeleteSharedToolsRow)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSyncToolsFromRobots)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSyncToolsToRobots)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelToolsLayout.setVerticalGroup(
            jPanelToolsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAddSharedToolsRow)
                    .addComponent(jButtonDeleteSharedToolsRow)
                    .addComponent(jButtonSyncToolsFromRobots)
                    .addComponent(jButtonSyncToolsToRobots))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneSharedToolsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Shared Tools", jPanelTools);

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

        jCheckBoxMenuItemContinuousDemo.setText("Continuous Demo");
        jCheckBoxMenuItemContinuousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinuousDemoActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemContinuousDemo);

        jCheckBoxMenuItemContinuousDemoRevFirst.setText("Continuous Demo (Reverse first)");
        jCheckBoxMenuItemContinuousDemoRevFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemContinuousDemoRevFirst);

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

        jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continuous Demo");
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

        jMenuItemRandomTestReverseFirst.setText("Randomized Enable Toggle Continuous Demo (Reverse First) ");
        jMenuItemRandomTestReverseFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRandomTestReverseFirstActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jMenuItemRandomTestReverseFirst);

        jCheckBoxMenuItemIndContinuousDemo.setText("(Independant) Continuous Demo");
        jCheckBoxMenuItemIndContinuousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIndContinuousDemoActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jCheckBoxMenuItemIndContinuousDemo);

        jCheckBoxMenuItemIndRandomToggleTest.setText("(Independant) Continuous Demo With Randomized Enable Toggle    ");
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

        jMenuItemStartContinuousScanAndRun.setText("Start Continuous Scan and Run");
        jMenuItemStartContinuousScanAndRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartContinuousScanAndRunActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jMenuItemStartContinuousScanAndRun);

        jMenuItemStartScanAllThenContinuousDemoRevFirst.setText("Start Scan All Then Continuous Demo Rev First");
        jMenuItemStartScanAllThenContinuousDemoRevFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed(evt);
            }
        });
        jMenuActionsAdditionalTests.add(jMenuItemStartScanAllThenContinuousDemoRevFirst);

        jMenuActions.add(jMenuActionsAdditionalTests);

        jMenuBar1.add(jMenuActions);

        jMenuOptions.setText("Options");

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

        jCheckBoxMenuItemDebug.setText("Debug");
        jCheckBoxMenuItemDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDebugActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemDebug);

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

        jCheckBoxMenuItemContDemoReverseFirstOption.setText("Reverse First for Continuous Demo");
        jMenuOptions.add(jCheckBoxMenuItemContDemoReverseFirstOption);

        jCheckBoxMenuItemUseTeachCamera.setSelected(true);
        jCheckBoxMenuItemUseTeachCamera.setText("Use Teach Camera");
        jMenuOptions.add(jCheckBoxMenuItemUseTeachCamera);

        jCheckBoxMenuItemKeepAndDisplayXFutureProfiles.setText("Keep and Display XFuture Profiles");
        jMenuOptions.add(jCheckBoxMenuItemKeepAndDisplayXFutureProfiles);

        jMenuItemSetMaxCycles.setText("Set max cycles (-1) ...");
        jMenuItemSetMaxCycles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetMaxCyclesActionPerformed(evt);
            }
        });
        jMenuOptions.add(jMenuItemSetMaxCycles);

        jCheckBoxMenuItemRecordLiveImageMovie.setText("Record Live Images Movie");
        jCheckBoxMenuItemRecordLiveImageMovie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemRecordLiveImageMovieActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemRecordLiveImageMovie);

        jMenuBar1.add(jMenuOptions);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2)
                .addContainerGap())
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


    private void jMenuItemSaveSetupAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSetupAsActionPerformed
        try {
            browseSaveSetupAs();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemSaveSetupAsActionPerformed

    /**
     * Query the user to select a file to save setup data in.
     */
    private void browseSaveSetupAs() throws IOException {
        File chosenFile = chooseFileForSaveAs(Supervisor.getLastSetupFile());
        if (null != chosenFile) {
            saveSetupFile(chosenFile);
        }
    }

    @Nullable
    public File chooseFileForSaveAs(@Nullable File prevChooserFile) throws HeadlessException {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Choose APRS Multi Supervisor CSV to create (save as).");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma Separated Values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        setChooserFile(prevChooserFile, chooser);
        int chooserRet = chooser.showSaveDialog(this);
        File chosenFile = (JFileChooser.APPROVE_OPTION == chooserRet)
                ? chooser.getSelectedFile()
                : null;
        return chosenFile;
    }

    private void jMenuItemLoadSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadSetupActionPerformed
        try {
            browseOpenSetup();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemLoadSetupActionPerformed

    /**
     * Query the user to select a setup file to read.
     */
    private void browseOpenSetup() throws IOException {
        File prevChosenFile = Supervisor.getLastSetupFile();
        File chosenFile = chooseSetupFileToOpen(prevChosenFile);
        if (null != chosenFile) {
            loadSetupFile(chosenFile);
        }
    }

    @Nullable
    public File chooseSetupFileToOpen(@Nullable File prevChosenFile) throws HeadlessException {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setDialogTitle("Choose APRS Multi Supervisor CSV to Open.");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma Separated Values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        setChooserFile(prevChosenFile, chooser);
        int chooserRet = chooser.showOpenDialog(this);
        File chosenFile = (JFileChooser.APPROVE_OPTION == chooserRet)
                ? chooser.getSelectedFile()
                : null;
        return chosenFile;
    }

    private void jMenuItemAddExistingSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddExistingSystemActionPerformed
        File chosenFile = chooseSystemPropertiesFileToOpen();
        if (chosenFile != null) {
            try {
                File propertiesFile = chosenFile;
                AprsSystem.createSystem(propertiesFile)
                        .thenAccept((AprsSystem sys) -> {
                            sys.setVisible(true);
                            addAprsSystem(sys);
                            updateRobotsTable();
                            saveCurrentSetup();
                        });
            } catch (Exception ex) {
                log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jMenuItemAddExistingSystemActionPerformed

    @Nullable
    public File chooseSystemPropertiesFileToOpen() throws HeadlessException {
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Text properties files.", "txt");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Open APRS System properties file to be added to multi-system supervisor.");
        int chooserRet = chooser.showOpenDialog(this);
        File chosenFile = (chooserRet == JFileChooser.APPROVE_OPTION)
                ? chooser.getSelectedFile()
                : null;
        return chosenFile;
    }

    /**
     * Add a system to show and update the tasks and robots tables.
     *
     * @param sys system to add
     */
    private void addAprsSystem(AprsSystem sys) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.addAprsSystem(sys);
    }

    private void jMenuItemRemoveSelectedSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRemoveSelectedSystemActionPerformed
        int selectedIndex = jTableTasks.getSelectedRow();
        performRemoveSelectedSystemAction(selectedIndex);
    }//GEN-LAST:event_jMenuItemRemoveSelectedSystemActionPerformed

    private void performRemoveSelectedSystemAction(int selectedIndex) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.performRemoveSelectedSystemAction(selectedIndex);
    }

    @Nullable
    private volatile XFuture<?> lastFutureReturned = null;

    private XFutureVoid prepAndFinishOnDispatch(Runnable r) {
        return prepActions()
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

    private <T> XFuture<T> prepAndFinishOnDispatch(UiSupplier<XFuture<T>> supplier) {
        return prepActions()
                .thenCompose(x -> Utils.supplyOnDispatchThread(supplier))
                .thenCompose(x -> x);
    }

    private XFutureVoid prepAndFinishToXFutureVoidOnDispatch(UiSupplier<XFutureVoid> supplier) {
        return prepActions()
                .thenCompose(x -> Utils.supplyOnDispatchThread(supplier))
                .thenComposeToVoid(x -> x);
    }

    private void jMenuItemStartAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllActionPerformed
        performStartAllAction();
    }//GEN-LAST:event_jMenuItemStartAllActionPerformed

    private void performStartAllAction() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.performStartAllAction();
    }

    private XFutureVoid prepActions() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.prepActions();
    }

    private void jMenuItemSavePosMapsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePosMapsActionPerformed
        try {
            browseAndSavePositionMappings();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemSavePosMapsActionPerformed

    private void browseAndSavePositionMappings() throws HeadlessException, IOException {
        File chosenFile = choosePositionMappingsFileForSaveAs(Supervisor.getLastPosMapFile());
        if (null != chosenFile) {
            savePositionMaps(chosenFile);
        }
    }

    @Nullable
    public File choosePositionMappingsFileForSaveAs(@Nullable File prevChosenFile) throws HeadlessException {
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Comma-separated values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Choose APRS position mappings csv file to create (save as)");
        setChooserFile(prevChosenFile, chooser);
        int chooserRet = chooser.showSaveDialog(this);
        File chosenFile = (JFileChooser.APPROVE_OPTION == chooserRet)
                ? chooser.getSelectedFile()
                : null;
        return chosenFile;
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
        try {
            browseOpenPosMapsFile();
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemLoadPosMapsActionPerformed

    /**
     * Query the user to select a posmap file to read. The posmap file is a CSV
     * file that points to other csv files with infomation needed to transform
     * coordinates from one robot to another.
     */
    public void browseOpenPosMapsFile() throws IOException {
        File chosenFile = choosePosMapsFileToOpen(Supervisor.getLastPosMapFile());
        if (null != chosenFile) {
            loadPositionMaps(chosenFile);
        }
    }

    @Nullable
    public File choosePosMapsFileToOpen(@Nullable File prevChosenFile) throws HeadlessException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose APRS Position Maps CSV to Open.");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma Separated Values", "csv");
        chooser.addChoosableFileFilter(filter);
        setChooserFile(prevChosenFile, chooser);
        int chooserRet = chooser.showOpenDialog(this);
        File chosenFile = (JFileChooser.APPROVE_OPTION == chooserRet)
                ? chooser.getSelectedFile()
                : null;
        return chosenFile;
    }

    private static void setChooserFile(@Nullable File file, JFileChooser chooser) {
        if (file != null) {
            File parentFile = file.getParentFile();
            if (null != parentFile) {
                chooser.setCurrentDirectory(parentFile);
                chooser.setSelectedFile(file);
            }
        }
    }

    private void jMenuItemSafeAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSafeAbortAllActionPerformed

        performSafeAbortAllAction();
    }//GEN-LAST:event_jMenuItemSafeAbortAllActionPerformed

    private void performSafeAbortAllAction() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.performSafeAbortAllAction();
    }

    public void showSafeAbortComplete() {
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        immediateAbortAll("showSafeAbortComplete");
        fullAbortAll();
        forceShowMessageFullScreen("Safe Abort Complete", 80.0f,
                SplashScreen.getRobotArmImage(),
                SplashScreen.getBlueWhiteGreenColorList(), gd);
    }

    private final AtomicBoolean ignoreTitleErrors = new AtomicBoolean(false);

    private void jMenuItemImmediateAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortAllActionPerformed
        fullAbortAll();
    }//GEN-LAST:event_jMenuItemImmediateAbortAllActionPerformed

    private void fullAbortAll() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.fullAbortAll();
    }

    public void clearCheckBoxes() {
        jCheckBoxMenuItemContinuousDemo.setSelected(false);
        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        jCheckBoxMenuItemPauseResumeTest.setSelected(false);
        jCheckBoxMenuItemPause.setSelected(false);
        jCheckBoxMenuItemIndContinuousDemo.setSelected(false);
        jCheckBoxMenuItemIndRandomToggleTest.setSelected(false);
    }

    @Nullable
    private AprsSystem getPosMapInSys() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getPosMapInSys();
    }

    private void setPosMapInSys(AprsSystem posMapInSys) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setPosMapInSys(posMapInSys);
    }

    @Nullable
    private AprsSystem getPosMapOutSys() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getPosMapOutSys();
    }

    private void setPosMapOutSys(AprsSystem posMapOutSys) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setPosMapInSys(posMapOutSys);
    }


    private void jButtonSetInFromCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetInFromCurrentActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            AprsSystem posMapInSys = getPosMapInSys();
            if (null != posMapInSys) {
                PoseType pose = posMapInSys.getCurrentPose();
                if (null != pose) {
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX(), row, 0);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY(), row, 1);
                    jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ(), row, 2);
                    Object otherXObject = jTableSelectedPosMapFile.getValueAt(row, 3);
                    Object otherYObject = jTableSelectedPosMapFile.getValueAt(row, 4);
                    Object otherZObject = jTableSelectedPosMapFile.getValueAt(row, 5);
                    if (otherXObject instanceof Double
                            && otherYObject instanceof Double
                            && otherZObject instanceof Double) {
                        double otherx = (double) otherXObject;
                        double othery = (double) otherYObject;
                        double otherz = (double) otherZObject;
                        jTableSelectedPosMapFile.setValueAt(otherx - pose.getPoint().getX(), row, 6);
                        jTableSelectedPosMapFile.setValueAt(othery - pose.getPoint().getY(), row, 7);
                        jTableSelectedPosMapFile.setValueAt(otherz - pose.getPoint().getZ(), row, 8);
                    }
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
            AprsSystem posMapOutSys = getPosMapOutSys();
            if (null != posMapOutSys) {
                PoseType pose = posMapOutSys.getCurrentPose();
                if (null != pose) {
                    Object otherXObject = jTableSelectedPosMapFile.getValueAt(row, 0);
                    Object otherYObject = jTableSelectedPosMapFile.getValueAt(row, 1);
                    Object otherZObject = jTableSelectedPosMapFile.getValueAt(row, 2);
                    if (otherXObject instanceof Double
                            && otherYObject instanceof Double
                            && otherZObject instanceof Double) {
                        double otherx = (double) otherXObject;
                        double othery = (double) otherYObject;
                        double otherz = (double) otherZObject;
                        jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX(), row, 3);
                        jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY(), row, 4);
                        jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ(), row, 5);
                        jTableSelectedPosMapFile.setValueAt(pose.getPoint().getX() - otherx, row, 6);
                        jTableSelectedPosMapFile.setValueAt(pose.getPoint().getY() - othery, row, 7);
                        jTableSelectedPosMapFile.setValueAt(pose.getPoint().getZ() - otherz, row, 8);
                    }
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
            selectAndSavePosMapFile();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonSaveSelectedPosMapActionPerformed

    @Nullable
    private File getLastPosMapParent() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getLastPosMapParent();
    }

    private void selectAndSavePosMapFile() throws IOException, HeadlessException {

        File parentFile = getLastPosMapParent();
        File f = resolveFile(jTextFieldSelectedPosMapFilename.getText(), parentFile);
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Comma-separated values", "csv");
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Choose APRS position mapping csv file to create (save as)");
        if (null != f) {
            File resolvedParentFile = f.getParentFile();
            if (null != resolvedParentFile) {
                chooser.setCurrentDirectory(resolvedParentFile);
                chooser.setSelectedFile(f);
            }
        }
        int ret = chooser.showSaveDialog(this);
        File chosenFile = (JFileChooser.APPROVE_OPTION == ret)
                ? chooser.getSelectedFile()
                : null;
        if (null != chosenFile) {
            savePosFile(chosenFile);
        }
        if (null != chosenFile && null != parentFile) {
            int row = jTablePositionMappings.getSelectedRow();
            int col = jTablePositionMappings.getSelectedColumn();
            if (row >= 0 && row < jTablePositionMappings.getRowCount() && col > 0 && col < jTablePositionMappings.getColumnCount()) {
                jTablePositionMappings.setValueAt(relativeFile(parentFile, chosenFile), row, col);
            }
            jTextFieldSelectedPosMapFilename.setText(chosenFile.getCanonicalPath());
            if (JOptionPane.showConfirmDialog(this, "Also Save files list?") == JOptionPane.YES_OPTION) {
                browseAndSavePositionMappings();
            }
        }
    }

    private void jCheckBoxMenuItemDisableTextPopupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed

    private void jMenuItemDbgActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDbgActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDbgActionActionPerformed

    private static void printStatus(AtomicReference<@Nullable XFutureVoid> ref, PrintStream ps) {
        if (null != ref) {
            XFuture<?> xf = ref.get();
            printStatus(xf, ps);
        }
    }

    private static void printStatus(@Nullable XFuture<?> xf, PrintStream ps) {
        if (null != xf) {
            xf.printStatus(ps);
        }
    }

    private void debugAction() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.debugAction();
    }

    @Nullable
    private Socket colorTextSocket = null;
    @Nullable
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
                log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jMenuItemStartColorTextDisplayActionPerformed

    private volatile boolean closing = false;

    private void close() {
        closing = true;
        if (null != runTimeTimer) {
            runTimeTimer.stop();
            runTimeTimer = null;
        }
        this.colorTextJPanel1.stopReader();
        if (null != colorTextJFrame) {
            colorTextJFrame.setVisible(false);
            colorTextJFrame = null;
        }
        if (null != colorTextSocket) {
            try {
                colorTextSocket.close();
            } catch (IOException ex) {
                log(Level.SEVERE, "", ex);
            }
            colorTextSocket = null;
        }
        this.setVisible(false);
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.close();
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        close();
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    private void jMenuItemAddNewSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAddNewSystemActionPerformed
        try {
            AprsSystem.createEmptySystem()
                    .thenAccept((AprsSystem sys) -> {
                        try {
                            sys.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            sys.setOnCloseRunnable(this::close);
                            sys.setVisible(true);
                            addAprsSystem(sys);
                            updateRobotsTable();
                            sys.browseSavePropertiesFileAs()
                                    .thenRun(this::saveCurrentSetup);
                        } catch (Exception ex) {
                            log(Level.SEVERE, "", ex);
                        }
                    });
        } catch (Exception ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemAddNewSystemActionPerformed

    private void clearContinuousDemoCycle() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.clearContinuousDemoCycle();
    }

    private void jCheckBoxMenuItemContinuousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinuousDemoActionPerformed
        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll("jCheckBoxMenuItemContinuousDemoActionPerformed");
            privateClearEventLog();
            clearAllErrors();
            connectAll();
            jCheckBoxMenuItemContinuousDemoActionPerformed2();
        });
    }//GEN-LAST:event_jCheckBoxMenuItemContinuousDemoActionPerformed

//    private XFutureVoid startSetAllReverseFlag(boolean flag) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.startSetAllReverseFlag(flag);
//
//    }
    private void jCheckBoxMenuItemContinuousDemoActionPerformed2() {
        Utils.runOnDispatchThread(this::jCheckBoxMenuItemContinuousDemoActionPerformed2OnDisplay);
    }

    private void jCheckBoxMenuItemContinuousDemoActionPerformed2OnDisplay() {
        enableAllRobots();
        clearContinuousDemoCycle();
        if (jCheckBoxMenuItemContinuousDemo.isSelected()) {
            XFutureVoid ContinuousDemoFuture = startContinuousDemo();
            setMainFuture(ContinuousDemoFuture);
        }
    }

    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
            clearAllErrors();
            resume();
            if (!origIgnoreTitleErrs) {
                ignoreTitleErrors.set(false);
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseActionPerformed

    public void clearEventLog() {
        ((DefaultTableModel) jTableEvents.getModel()).setRowCount(0);
    }

    private void privateClearEventLog() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.clearEventLog();
    }

    public void clearRandomTestCount() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.clearRandomTestCount();
    }


    private void jCheckBoxMenuItemRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRandomTestActionPerformed
        prepAndFinishOnDispatch(this::jCheckBoxMenuItemRandomTestActionPerformed2OnDisplay);
    }//GEN-LAST:event_jCheckBoxMenuItemRandomTestActionPerformed

    private void jCheckBoxMenuItemRandomTestActionPerformed2OnDisplay() {
        immediateAbortAll("jCheckBoxMenuItemRandomTestActionPerformed");
        privateClearEventLog();
        clearAllErrors();
        connectAll();
        enableAllRobots();
        clearContinuousDemoCycle();
        clearRandomTestCount();
        if (jCheckBoxMenuItemRandomTest.isSelected()) {
            lastFutureReturned = startRandomTest();
            setMainFuture(lastFutureReturned);
        }
    }

    @SuppressWarnings({"rawtypes", "nullness"})
    public static TableModel defaultPositionMappingsModel() {
        return Supervisor.defaultPositionMappingsModel();
    }

    private void jMenuItemStartAllReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllReverseActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll("jMenuItemStartAllReverseActionPerformed");
            connectAll();
            jMenuItemStartAllReverseActionPerformed2OnDisplay();
        });
    }//GEN-LAST:event_jMenuItemStartAllReverseActionPerformed


    private void jMenuItemStartAllReverseActionPerformed2OnDisplay() {
        enableAllRobots();
        lastFutureReturned = startReverseActions();
        setMainFuture(lastFutureReturned);
    }

    private void jMenuItemResetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetAllActionPerformed
        try {
            boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
            boolean reloadSimFiles = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Reload sim files?"));
            resetAll(reloadSimFiles);
            resetAll(false)
                    .thenCompose(x -> {
                        return Utils.runOnDispatchThread(() -> {
                            boolean origIgnoreTitleErrs2 = ignoreTitleErrors.getAndSet(true);
                            try {
                                cancelAll(true);
                                cancelAllStealUnsteal(true);
                                jCheckBoxMenuItemPause.setSelected(false);
                                jCheckBoxMenuItemContDemoReverseFirstOption.setSelected(false);
                                jCheckBoxMenuItemContinuousDemo.setSelected(false);
                                jCheckBoxMenuItemIndContinuousDemo.setSelected(false);
                                jCheckBoxMenuItemIndRandomToggleTest.setSelected(false);
                                jCheckBoxMenuItemRandomTest.setSelected(false);
                                jCheckBoxMenuItemPauseResumeTest.setSelected(false);
                                clearAllErrors();
                                resume();
                                jCheckBoxMenuItemPause.setSelected(false);
                                resume();
                                resetAll(reloadSimFiles);
                                restoreRobotNames();
                                connectAll();
                                enableAllRobots();
                            } catch (Exception e) {
                                logEvent("Exception occurred: " + e);
                                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                                JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
                            } finally {
                                if (!origIgnoreTitleErrs) {
                                    ignoreTitleErrors.set(false);
                                }
                            }
                        });
                    });
        } catch (Exception e) {
            logEvent("Exception occurred: " + e);
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
            JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
        }
    }//GEN-LAST:event_jMenuItemResetAllActionPerformed

    /**
     * Reset all systems, clearing errors, resetting states to defaults and
     * optionally reloading simulation files. This may occur in another thread.
     *
     * @param reloadSimFiles whether to reload simulation files
     * @return a future which can be used to determine when the resetAll action
     * is complete.
     */
    private XFutureVoid resetAll(boolean reloadSimFiles) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.resetAll(reloadSimFiles);
    }

    @Nullable
    private XFutureVoid getPauseTestFuture() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getPauseTestFuture();
    }

    private void setPauseTestFuture(@Nullable XFutureVoid pauseTestFuture) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setPauseTestFuture(pauseTestFuture);
    }

    private void jCheckBoxMenuItemPauseResumeTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed
        prepAndFinishOnDispatch(() -> {
            XFutureVoid ContinuousDemoFuture = getContinuousDemoFuture();
            if (null != ContinuousDemoFuture) {
                ContinuousDemoFuture.cancelAll(true);
                ContinuousDemoFuture = null;
            }
            XFutureVoid randomTestFuture = getRandomTestFuture();
            if (null != randomTestFuture) {
                randomTestFuture.cancelAll(true);
                setRandomTestFuture(null);
            }
            XFutureVoid pauseTestFuture = getPauseTestFuture();
            if (null != pauseTestFuture) {
                pauseTestFuture.cancelAll(true);
                setPauseTestFuture(null);
            }
            if (null != lastFutureReturned) {
                lastFutureReturned.cancelAll(true);
                lastFutureReturned = null;
            }
            immediateAbortAll("jCheckBoxMenuItemPauseResumeTestActionPerformed");
            clearAllErrors();
            connectAll();
            jCheckBoxMenuItemPauseResumeTestActionPerformed2OnDisplay();
        });
    }//GEN-LAST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed


    private void jCheckBoxMenuItemPauseResumeTestActionPerformed2OnDisplay() {
        XFutureVoid ContinuousDemoFuture;
        XFutureVoid randomTestFuture;
        XFutureVoid pauseTestFuture;
        enableAllRobots();
        clearContinuousDemoCycle();
        clearRandomTestCount();
        jCheckBoxMenuItemContinuousDemo.setSelected(false);
        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        if (jCheckBoxMenuItemPauseResumeTest.isSelected()) {
            jCheckBoxMenuItemContinuousDemo.setSelected(true);
            jCheckBoxMenuItemRandomTest.setSelected(true);
            ContinuousDemoFuture = startContinuousDemo();
            randomTestFuture = continueRandomTest();
            pauseTestFuture = continuePauseTest();
            resetMainPauseTestFuture();
        }
    }

    private void resetMainPauseTestFuture() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.resetMainPauseTestFuture();
    }

    private void setDebug(boolean dbg) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setDebug(dbg);
    }

    private void jCheckBoxMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugActionPerformed
        setDebug(jCheckBoxMenuItemDebug.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItemDebugActionPerformed

    private void jMenuItemContinueAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueAllActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        prepAndFinishOnDispatch(() -> {
            XFutureVoid randomTestFuture = getRandomTestFuture();
            if (null != randomTestFuture) {
                randomTestFuture.cancelAll(true);
                setRandomTestFuture(null);
            }
            XFutureVoid pauseTestFuture = getPauseTestFuture();
            if (null != pauseTestFuture) {
                pauseTestFuture.cancelAll(true);
                setPauseTestFuture(null);
            }
            XFutureVoid ContinuousDemoFuture = getContinuousDemoFuture();
            if (null != ContinuousDemoFuture) {
                ContinuousDemoFuture.cancelAll(true);
                ContinuousDemoFuture = null;
            }
            immediateAbortAll("jMenuItemContinueAllActionPerformed");
            jCheckBoxMenuItemRandomTest.setSelected(false);
            jCheckBoxMenuItemPause.setSelected(false);
            resume();
            XFutureVoid continueAllXF = continueAll();
            lastFutureReturned = continueAllXF;
            if (jCheckBoxMenuItemContinuousDemo.isSelected()) {
                ContinuousDemoFuture
                        = continueAllXF
                                .thenComposeToVoid("jMenuItemContinueAllActionPerformed.continueAllActions",
                                        x -> continueAllActions());
                setMainFuture(ContinuousDemoFuture);
            }
        });
    }//GEN-LAST:event_jMenuItemContinueAllActionPerformed

    private void jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed
        if (jCheckBoxMenuItemContinuousDemoRevFirst.isSelected()) {
            startContinuousDemoRevFirst();
        } else {
            immediateAbortAll("jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed");
        }
    }//GEN-LAST:event_jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed

    private XFutureVoid startContinuousDemoRevFirst() {
        jCheckBoxMenuItemContinuousDemo.setSelected(false);
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startContinuousDemoRevFirst();
    }

    private void jMenuItemScanAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemScanAllActionPerformed
        try {
            prepAndFinishOnDispatch(() -> {
                try {
                    restoreRobotNames();
                    lastFutureReturned = startScanAll();
                    setMainFuture(lastFutureReturned);
                } catch (Exception e) {
                    logEvent("Exception occurred: " + e);
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                    JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
                }
            });
        } catch (Exception e) {
            logEvent("Exception occurred: " + e);
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
            JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
        }
    }//GEN-LAST:event_jMenuItemScanAllActionPerformed

    private void jCheckBoxUpdateFutureAutomaticallyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxUpdateFutureAutomaticallyActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxUpdateFutureAutomaticallyActionPerformed

    private void jMenuItemRandomTestReverseFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRandomTestReverseFirstActionPerformed
        startRandomTestFirstActionReversed();
    }//GEN-LAST:event_jMenuItemRandomTestReverseFirstActionPerformed

    private XFutureVoid startRandomTestFirstActionReversed() {
        try {
            jCheckBoxMenuItemContDemoReverseFirstOption.setSelected(true);
            jCheckBoxMenuItemRandomTest.setSelected(true);
            return prepAndFinishToXFutureVoidOnDispatch(() -> {
                try {
                    immediateAbortAll("jMenuItemRandomTestReverseFirstActionPerformed");
                    XFutureVoid outerRet
                            = resetAll(false)
                                    .thenComposeToVoid(x -> {
                                        XFutureVoid innerRet = Utils.supplyOnDispatchThread(() -> {
                                            try {
                                                clearAllErrors();
                                                connectAll();
                                                jCheckBoxMenuItemPause.setSelected(false);
                                                resume();
                                                return startRandomTestFirstActionReversed2();
                                            } catch (Exception e) {
                                                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                                                JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
                                                XFutureVoid ret = new XFutureVoid("internal startRandomTestFirstActionReversed with exception " + e);
                                                ret.completeExceptionally(e);
                                                return ret;
                                            }
                                        }).thenComposeToVoid(x3 -> x3);
                                        return innerRet;
                                    });
                    return outerRet;
                } catch (Exception e) {
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                    JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
                    XFutureVoid ret = new XFutureVoid("internal startRandomTestFirstActionReversed with exception " + e);
                    ret.completeExceptionally(e);
                    return ret;
                }
            });
        } catch (Exception e) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
            JOptionPane.showMessageDialog(this, "Exception occurred: " + e);
            XFutureVoid ret = new XFutureVoid("startRandomTestFirstActionReversed with exception " + e);
            ret.completeExceptionally(e);
            return ret;
        }
    }

    private XFutureVoid startRandomTestFirstActionReversed2() {
        return Utils.supplyOnDispatchThread(this::startRandomTestFirstActionReversed2OnDisplay)
                .thenComposeToVoid(x -> x);
    }

    private XFutureVoid startRandomTestFirstActionReversed2OnDisplay() {
        enableAllRobots();
        clearContinuousDemoCycle();
        clearRandomTestCount();
        jCheckBoxMenuItemContDemoReverseFirstOption.setSelected(true);
        jCheckBoxMenuItemRandomTest.setSelected(true);
        lastFutureReturned = null;
        XFutureVoid ret = startRandomTest();
        setMainFuture(ret);
        return ret;
    }

    private void jCheckBoxShowUnnamedFuturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowUnnamedFuturesActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxShowUnnamedFuturesActionPerformed

    private void jCheckBoxShowDoneFuturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowDoneFuturesActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxShowDoneFuturesActionPerformed

    private void jButtonFuturesCancelAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFuturesCancelAllActionPerformed
        if (null == futureToDisplaySupplier) {
            return;
        }
        XFuture<?> future = futureToDisplaySupplier.get();
        if (null != future) {
            future.cancelAll(true);
        }
    }//GEN-LAST:event_jButtonFuturesCancelAllActionPerformed

    private void jTextFieldEventsMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldEventsMaxActionPerformed
        setEventsDisplayMax(Integer.parseInt(jTextFieldEventsMax.getText().trim()));
    }//GEN-LAST:event_jTextFieldEventsMaxActionPerformed

    private void jCheckBoxMenuItemIndContinuousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemIndContinuousDemoActionPerformed
        try {
            ignoreTitleErrors.set(true);
            jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
            jCheckBoxMenuItemContinuousDemo.setSelected(false);
            prepAndFinishOnDispatch(() -> {
                immediateAbortAll("jCheckBoxMenuItemIndContinuousDemoActionPerformed");
                privateClearEventLog();
                clearAllErrors();
                connectAll();
                jCheckBoxMenuItemPause.setSelected(false);
                resume();
                jCheckBoxMenuItemIndContinuousDemoActionPerformed2OnDisplay();
            });
        } finally {
            ignoreTitleErrors.set(false);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemIndContinuousDemoActionPerformed

    private void jCheckBoxMenuItemIndContinuousDemoActionPerformed2OnDisplay() {
        enableAllRobots();
        clearContinuousDemoCycle();
        if (jCheckBoxMenuItemIndContinuousDemo.isSelected()) {
            resetAll(false)
                    .thenCompose(x -> {
                        return Utils.runOnDispatchThread(() -> {
                            jCheckBoxMenuItemIndContinuousDemo.setSelected(true);
                            XFutureVoid future = startIndependentContinuousDemo();
                            setMainFuture(future);
                            setContinuousDemoFuture(future);
                        });

                    });
        }
    }

    private Random getRandom() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getRandom();
    }

    private void setRandom(Random random) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setRandom(random);
    }

    private void jCheckBoxMenuItemIndRandomToggleTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemIndRandomToggleTestActionPerformed
        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemContinuousDemo.setSelected(false);
        jCheckBoxMenuItemIndContinuousDemo.setSelected(false);
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll("jCheckBoxMenuItemIndRandomToggleTestActionPerformed", true)
                    .thenRun(() -> {
                        privateClearEventLog();
                        clearAllErrors();
                        connectAll();
                        jCheckBoxMenuItemPause.setSelected(false);
                        resume();
                        jCheckBoxMenuItemIndRandomToggleTestActionPerformed2OnDisplay();
                    });
        });
    }//GEN-LAST:event_jCheckBoxMenuItemIndRandomToggleTestActionPerformed


    private void jCheckBoxMenuItemIndRandomToggleTestActionPerformed2OnDisplay() {
        enableAllRobots();
        clearContinuousDemoCycle();
        if (jCheckBoxMenuItemFixedRandomTestSeed.isSelected()) {
            Random newRandom = new Random(getRandomTestSeed());
            setRandom(newRandom);
        } else {
            Random newRandom = new Random(System.currentTimeMillis());
            setRandom(newRandom);
        }
        if (jCheckBoxMenuItemIndRandomToggleTest.isSelected()) {
            resetAll(false)
                    .thenCompose(x -> {
                        return Utils.runOnDispatchThread(() -> {
                            jCheckBoxMenuItemIndRandomToggleTest.setSelected(true);
                            XFutureVoid future = startRandomEnableToggleIndependentContinuousDemo();
                            setContinuousDemoFuture(future);
                            setMainFuture(future);
                        });

                    });
        }
    }

    private int getRandomTestSeed() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getRandomTestSeed();
    }

    private void setRandomTestSeed(int randomTestSeed) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setRandomTestSeed(randomTestSeed);
    }

    private void jCheckBoxMenuItemFixedRandomTestSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemFixedRandomTestSeedActionPerformed
        if (jCheckBoxMenuItemFixedRandomTestSeed.isSelected()) {
            int randomTestSeed = Integer.parseInt(JOptionPane.showInputDialog("Fixed Seed", getRandomTestSeed()));
            jCheckBoxMenuItemFixedRandomTestSeed.setText("Fixed Random Test Seed (" + randomTestSeed + ") ... ");
            setRandomTestSeed(randomTestSeed);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemFixedRandomTestSeedActionPerformed

    private void jCheckBoxFutureLongFormActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxFutureLongFormActionPerformed
        updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
    }//GEN-LAST:event_jCheckBoxFutureLongFormActionPerformed

    private static String getDirNameOrHome(@Nullable File f) throws IOException {
        if (f != null) {
            File parentFile = f.getParentFile();
            if (null != parentFile) {
                return parentFile.getCanonicalPath();
            }
        }
        return System.getProperty("user.home");
    }


    private void jMenuItemSaveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveAllActionPerformed
        try {
            if (null == supervisor) {
                throw new IllegalStateException("null == supervisor");
            }
            Map<String, String> filesMapIn = new HashMap<>();
            filesMapIn.put("Setup", supervisor.getSetupFilePathString());
            filesMapIn.put("PosMap", supervisor.getPosMapFilePathString());
            filesMapIn.put("SimTeach", supervisor.getSimTeachFilePathString());
            filesMapIn.put("TeachProps", supervisor.getTeachPropsFilePathString());
            filesMapIn.put("SharedTools", supervisor.getSharedToolsFilePathString());

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
                String sharedTools = filesMapOut.get("SharedTools");
                if (null != sharedTools) {
                    saveSharedTools(new File(sharedTools));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemSaveAllActionPerformed

    private void jComboBoxTeachSystemViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxTeachSystemViewActionPerformed
        try {
            List<AprsSystem> aprsSystems = getAprsSystems();
            String sysString = (String) jComboBoxTeachSystemView.getSelectedItem();
            if (null == sysString || sysString.equals("All")) {
                setTeachSystemFilter(null);
            } else {
                int id = Integer.parseInt(sysString.trim().split("[ \t:]+")[0]);
                for (AprsSystem sys : aprsSystems) {
                    if (sys.getMyThreadId() == id) {
                        setTeachSystemFilter(sys);
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", exception);
        }
    }//GEN-LAST:event_jComboBoxTeachSystemViewActionPerformed

    public void saveSharedToolsTable(File f) throws IOException {
        Utils.saveJTable(f, jTableSharedTools);
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


    private void jMenuItemSetMaxCyclesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetMaxCyclesActionPerformed
        String newMaxCycles = JOptionPane.showInputDialog("Maximum cycles for Continuous demo", getMax_cycles());
        if (null != newMaxCycles && newMaxCycles.length() > 0) {
            setMax_cycles(Integer.parseInt(newMaxCycles));
        }
    }//GEN-LAST:event_jMenuItemSetMaxCyclesActionPerformed

    private void jMenuItemStartContinuousScanAndRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartContinuousScanAndRunActionPerformed
        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
        prepAndFinishOnDispatch(() -> {
            immediateAbortAll("jMenuItemStartContinuousScanAndRunActionPerformed");
            privateClearEventLog();
            clearAllErrors();
            connectAll();
            jMenuItemStartContinuousScanAndRunActionPerformed2OnDisplay();
        });
    }//GEN-LAST:event_jMenuItemStartContinuousScanAndRunActionPerformed

    private void jMenuItemStartContinuousScanAndRunActionPerformed2OnDisplay() {
        enableAllRobots();
        clearContinuousDemoCycle();
        jCheckBoxMenuItemShowSplashMessages.setSelected(false);
        jCheckBoxMenuItemContinuousDemo.setSelected(true);
        XFutureVoid future = startContinuousScanAndRun();
        setMainFuture(future);
        setContinuousDemoFuture(future);
    }

    private void jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed
        setMainFuture(startScanAllThenContinuousDemoRevFirst());
    }//GEN-LAST:event_jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed

    public boolean isRecordLiveImageMovieSelected() {
        return jCheckBoxMenuItemRecordLiveImageMovie.isSelected();
    }

    public void setRecordLiveImageMovieSelected(boolean selected) {
        jCheckBoxMenuItemRecordLiveImageMovie.setSelected(selected);
    }

    private void jCheckBoxMenuItemRecordLiveImageMovieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRecordLiveImageMovieActionPerformed
        if (!jCheckBoxMenuItemRecordLiveImageMovie.isSelected()) {
            if (null != supervisor) {
                supervisor.finishEncodingLiveImageMovie();
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemRecordLiveImageMovieActionPerformed

    private void jButtonAddSharedToolsRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddSharedToolsRowActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableSharedTools.getModel();
        dtm.addRow(new String[]{"", "", "", "", ""});

    }//GEN-LAST:event_jButtonAddSharedToolsRowActionPerformed

    private void jButtonDeleteSharedToolsRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteSharedToolsRowActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableSharedTools.getModel();
        int row = jTableSharedTools.getSelectedRow();
        if (row >= 0 && row < jTableSharedTools.getRowCount()) {
            dtm.removeRow(row);
        }
    }//GEN-LAST:event_jButtonDeleteSharedToolsRowActionPerformed

    private void jButtonSyncToolsFromRobotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSyncToolsFromRobotsActionPerformed
        syncToolsFromRobots();
    }//GEN-LAST:event_jButtonSyncToolsFromRobotsActionPerformed

    public void setShowFullScreenMessages(boolean showFullScreenMessages) {
        jCheckBoxMenuItemShowSplashMessages.setSelected(showFullScreenMessages);
    }

    private void syncToolsFromRobots() {
        supervisor.syncToolsFromRobots();
    }

    @SuppressWarnings("nullness")
    private void runCustomCode() {
        try {

            customCode = MultiLineStringJPanel.editText(customCode);
            File customDir = Paths.get(System.getProperty("user.home"), ".aprs", "custom").toFile();
            customDir.delete();
            customDir.mkdirs();
            File tmpFile = new File(customDir, "Custom.java");
            System.out.println("tmpFile = " + tmpFile.getCanonicalPath());
            File[] files1 = {tmpFile};

            Files.write(tmpFile.toPath(), customCode.getBytes());
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
                File parentFile = tmpFile.getAbsoluteFile().getParentFile();
                if (null == parentFile) {
                    logEventErr("Temporary file " + tmpFile + " does not have parent.");
                    return;
                }
                File grandParentFile = parentFile.getParentFile();
                if (null == grandParentFile) {
                    logEventErr("Temporary file " + tmpFile + " does not have grandparent.");
                    return;
                }
                urls[urls.length - 1] = grandParentFile.toURI().toURL();
                //tmpFile.getAbsoluteFile().getParentFile().getParentFile().toURI().toURL()};
                System.out.println("urls = " + Arrays.toString(urls));
                ClassLoader loader = new URLClassLoader(urls);
                Class<?> clss = loader.loadClass("custom.Custom");
                @SuppressWarnings("deprecation")
                Object obj = clss.newInstance();
                Method acceptMethod = clss.getMethod("accept", AprsSupervisorDisplayJFrame.class);
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
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", exception);
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

    private void setTeachSystemFilter(@Nullable AprsSystem sys) {
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

    @Nullable
    private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
    }

    private List<PhysicalItem> filterForSystem(AprsSystem sys, List<PhysicalItem> listIn) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.filterForSystem(sys, listIn);
    }

    private XFutureVoid lookForPartsAll() {
        List<AprsSystem> aprsSystems = getAprsSystems();
        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsSystem aprsSys = aprsSystems.get(i);
            futures[i] = aprsSys.startLookForParts();
        }
        return XFuture.allOfWithName("lookForPartsAll", futures);
    }

//    private XFutureVoid clearReverseAll() {
//        List<AprsSystem> aprsSystems = getAprsSystems();
//        XFuture<?> futures[] = new XFuture<?>[aprsSystems.size()];
//        for (int i = 0; i < aprsSystems.size(); i++) {
//            AprsSystem aprsSys = aprsSystems.get(i);
//            if (aprsSys.isReverseFlag()) {
//                logEvent("Set reverse flag false for " + aprsSys);
//                futures[i] = aprsSys.startSetReverseFlag(false, false);
//            } else {
//                futures[i] = XFuture.completedFuture(null);
//            }
//        }
//        return XFuture.allOfWithName("clearReverseAll", futures);
//    }
    public XFutureVoid showScanCompleteDisplay() {
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        logEvent("Scans Complete");
        setAbortTimeCurrent();
        return showMessageFullScreen("Scans Complete", 80.0f,
                null,
                SplashScreen.getBlueWhiteGreenColorList(), gd);
    }

    public void setRobotEnableToggleBlockerText(String text) {
        jTextFieldRobotEnableToggleBlockers.setText(text);
    }

    public XFutureVoid showAllTasksCompleteDisplay() {
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        logEvent("All Tasks Complete");
        setAbortTimeCurrent();
        return showMessageFullScreen("All Tasks Complete", 80.0f,
                null,
                SplashScreen.getBlueWhiteGreenColorList(), gd);
    }

    private XFuture<?> startScanAllThenContinuousDemoRevFirst() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startScanAllThenContinuousDemoRevFirst();
    }

    /**
     * Have each system scan the parts area to create an action list to fill
     * kits in a way similar to the current configuration. This may require
     * moving each robot out of the way of the vision system. The scans will
     * happen asynchronously in other threads.
     *
     * @return future that allows actions to be added after all scans are
     * complete.
     */
    private XFuture<?> startScanAll() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startScanAll();
    }

    /**
     * Perform a test of the Continuous demo where the motoman robot will be
     * randomly enabled and disabled.
     *
     * @return a future that can be used to determine if the test failed or was
     * cancelled.
     */
    private XFutureVoid startRandomTest() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startRandomTest();
    }

    private static Logger getLogger() {
        return Logger.getLogger(AprsSupervisorDisplayJFrame.class
                .getName());
    }

    public void showTogglesEnabled(boolean enabled) {
//        for(JCheckBox chkbox : robotsEnableCelEditorCheckBoxList) {
//            chkbox.setEnabled(enabled);
//        }
        for (JCheckBox chkbox : robotsEnableCelRendererComponentList) {
            chkbox.setEnabled(enabled);
        }
//        jTableRobots.getColumnModel().getColumn(1).getCellEditor().
        jTableRobots.repaint();
    }

    // stupid hard-coded hack to match demo
    private static final String MOTOMAN_NAME = "motoman";
    private static final Set<String> robotsThatCanBeDisabled
            = Collections.singleton(MOTOMAN_NAME);

    public JList<String> getFuturesList() {
        return jListFutures;
    }

    public JTable getRobotsTable() {
        return jTableRobots;
    }

    public JTable getSelectedPosMapFileTable() {
        return jTableSelectedPosMapFile;
    }

    public JTable getPositionMappingsTable() {
        return jTablePositionMappings;
    }

    public JTable getTasksTable() {
        return jTableTasks;
    }

    private void logEventErr(String err) {
        System.err.println(err);
        logEvent("ERROR: " + err);
    }

    public int getContiousDemoCycleCount() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getContiousDemoCycleCount();
    }

    private XFutureVoid continuePauseTest() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.continuePauseTest();
    }

    private XFutureVoid continueRandomTest() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.continueRandomTest();
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will wait for all systems to be filled before
     * any begin emptying and vice versa.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    private XFutureVoid startContinuousScanAndRun() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startContinuousScanAndRun();
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will wait for all systems to be filled before
     * any begin emptying and vice versa.
     *
     * @return future that can be used to determine if it fails or is cancelled
     */
    private XFutureVoid startContinuousDemo() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startContinuousDemo();
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will not wait for all systems to be filled
     * before any begin emptying and vice versa, so one might be emptying while
     * another is filling.
     *
     * @return future that can be used to determine if it fails or is canceled
     */
    private XFutureVoid startIndependentContinuousDemo() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startIndependentContinuousDemo();
    }

    /**
     * Start a continuous demo where kit trays will first be filled and then
     * emptied repeatedly. Systems will not wait for all systems to be filled
     * before any begin emptying and vice versa, so one might be emptying while
     * another is filling. In addition the motoman will be randomly enabled and
     * disabled for testing purposes.
     *
     * @return future that can be used to determine if it fails or is canceled
     */
    private XFutureVoid startRandomEnableToggleIndependentContinuousDemo() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startRandomEnableToggleIndependentContinuousDemo();
    }

    private int incrementAndGetContinuousDemoCycle() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.incrementAndGetContinuousDemoCycle();
    }

    public XFutureVoid incrementContinuousDemoCycle() {
        final int c = incrementAndGetContinuousDemoCycle();
        System.out.println("incrementContinuousDemoCycle : " + c);
        if (jCheckBoxMenuItemContinuousDemoRevFirst.isSelected()) {
            return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinuousDemoRevFirst.setText("Continuous Demo (Reverse First) (" + c + ") "));
        } else {
            return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinuousDemo.setText("Continuous Demo (" + c + ") "));
        }
    }

    private int getMax_cycles() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getMax_cycles();
    }

    private void setMax_cycles(int max_cycles) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setMax_cycles(max_cycles);
    }

    private boolean checkMaxCycles() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.checkMaxCycles();
    }

    /**
     * Start actions in reverse mode where kit trays will be emptied rather than
     * filled.
     *
     * @return future that can be used to attach additional actions after this
     * is complete
     */
    private XFutureVoid startReverseActions() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startReverseActions();
    }

    private void savePosFile(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.savePosFile(f);
    }

    private void clearPosTable() {
        if (null == robotEnableMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }
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

    @Nullable
    private JPopupMenu posTablePopupMenu = null;

    private void showPosTablePopup(Point pt) {
        JPopupMenu menu = posTablePopupMenu;
        if (menu == null) {
            menu = new JPopupMenu();
            JMenuItem mi = new JMenuItem("Clear");
            mi.addActionListener(l -> clearPosTable());
            menu.add(mi);
            posTablePopupMenu = menu;
        }
        menu.setLocation(pt.x, pt.y);
        menu.setVisible(true);
    }

    /**
     * Enable all robots. (Note: no check is made if the robot is physically in
     * estop and no change to its estop state is made, only the checkboxes in
     * the robots table are potentially changed.)
     */
    private void enableAllRobots() {
        if (null == robotEnableMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }
        if (null != supervisor) {
            supervisor.enableAllRobots();
        }
        cancelAllStealUnsteal(false);
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
        Utils.autoResizeTableColWidths(jTableRobots);
    }

    private void pause() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.pause();
    }

    private void resume() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.resume();
    }

    private static String shortTrace(StackTraceElement @Nullable [] trace) {
        if (null == trace) {
            return "";
        }
        String shortTrace = Arrays.toString(trace);
        if (shortTrace.length() > 80) {
            shortTrace = shortTrace.substring(0, 75) + " ... ";
        }
        return shortTrace;
    }

    /**
     * Log an exception to the events table.
     *
     * @param level log severity indicator
     * @param msg message to show
     * @param thrown exception causing this event
     */
    public void log(Level level, @Nullable String msg, Throwable thrown) {
        getLogger().log(level, msg, thrown);
        logEvent("Exception thrown : msg=" + msg + ",thrown=" + thrown + ", trace=" + shortTrace(thrown.getStackTrace()));
    }

    private XFutureVoid continueAllActions() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.continueAllActions();
    }

    private <T> XFuture<T> checkOkElse(Boolean ok, Supplier<XFuture<T>> okSupplier, Supplier<XFuture<T>> notOkSupplier) {
        if (ok) {
            return okSupplier.get();
        } else {
            return notOkSupplier.get();
        }
    }

    private XFutureVoid continueAll() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.continueAll();
    }

    /**
     * Clear all previously set errors /error states.
     */
    private void clearAllErrors() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.clearAllErrors();
    }

    /**
     * Have all systems immediately abort regardless of the robots position or
     * the object in the gripper. Robots that have been temporarily reassigned
     * will be returned. This may require a delay that can be checked on with
     * the returned future.
     *
     * @param comment used to identify the call location information in
     * displays/logs
     *
     * @return future allowing a check on when the abort is complete.
     */
    private XFuture<?> immediateAbortAll(String comment) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.immediateAbortAll(comment);
    }

    private XFuture<?> immediateAbortAll(String comment, boolean skipLog) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.immediateAbortAll(comment, skipLog);
    }

    private void cancelAll(boolean mayInterrupt) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.cancelAll(mayInterrupt);
    }

    private void cancelAllStealUnsteal(boolean mayInterrupt) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.cancelAllStealUnsteal(mayInterrupt);
    }

    private void restoreRobotNames() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.restoreRobotNames();
    }

    /**
     * Connect to all robots.
     */
    private void connectAll() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.connectAll();
    }

    /**
     * Get the value of setupFile
     *
     * @return the value of setupFile
     */
    @Nullable
    private File getSetupFile() throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getSetupFile();
    }

    public void setSaveSetupEnabled(boolean enabled) {
        this.jMenuItemSaveSetup.setEnabled(enabled);
    }

    public void setTitleMessage(String message, @Nullable File currentSetupFile) {
        if (null != currentSetupFile) {
            Utils.runOnDispatchThread(() -> setTitle("Multi Aprs Supervisor : " + currentSetupFile + " : " + message));
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
    private void setSetupFile(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setSetupFile(f);
    }

    public JTable getSharedToolsTable() {
        return this.jTableSharedTools;
    }

    /**
     * Save the current setup to the last saved/read setup file.
     */
    private void saveCurrentSetup() {
        try {
            File fileToSave = getSetupFile();
            if (null != fileToSave) {
                int response
                        = JOptionPane.showConfirmDialog(this, "Save Current APRS Supervisor file : " + fileToSave);
                if (response == JOptionPane.YES_OPTION) {
                    saveSetupFile(fileToSave);
                }
            }
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }

    /**
     * Save the current setup to the given file.
     *
     * @param f file to save setup to
     * @throws IOException file can not be written to
     */
    public void saveSetupFile(File f) throws IOException {
        saveJTable(f, jTableTasks, Arrays.asList(0, 1, 2, 6));
        setSetupFile(f);
    }

    private void saveJTable(File f, JTable jtable, Iterable<Integer> columnIndexes) throws IOException {
        String headers[] = tableHeaders(jtable, columnIndexes);
        CSVFormat format = CSVFormat.DEFAULT.withHeader(headers);
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), format)) {
            for (int i = 0; i < jtable.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (Integer colIndex : columnIndexes) {
                    if (null == colIndex) {
                        continue;
                    }
                    int j = (int) colIndex;
                    if (j > jtable.getColumnCount()) {
                        break;
                    }
                    Object o = jtable.getValueAt(i, j);
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else {
                        if (o == null) {
                            l.add("");
                        } else {
                            l.add(o);
                        }
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    private void saveJTable(File f, JTable jtable, CSVFormat csvFormat) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), csvFormat)) {
            for (int i = 0; i < jtable.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < jtable.getColumnCount(); j++) {
                    if (j == 3) {
                        continue;
                    }
                    Object o = jtable.getValueAt(i, j);
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else {
                        if (null != o) {
                            l.add(o);
                        } else {
                            l.add("");
                        }
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    /**
     * Save the posmaps to the given file. The posmap file is a CSV file that
     * points to other csv files with infomation needed to transform coordinates
     * from one robot to another.
     *
     * @param f file to safe posmaps in
     * @throws IOException file could not be written to
     */
    public void savePositionMaps(File f) throws IOException {
        saveJTable(f, jTablePositionMappings, CSVFormat.RFC4180);
        saveLastPosMapFile(f);
    }

    /**
     * Get the file location where data is stored for converting positions from
     * sys1 to sys2. The file is a CSV file.
     *
     * @param sys1 system to convert positions from
     * @param sys2 system to convert positions to
     * @return file for converting positions
     */
    private File getPosMapFile(String sys1, String sys2) throws FileNotFoundException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getPosMapFile(sys1, sys2);
    }

    /**
     * Load posmaps from the given file.
     *
     * @param f file to load
     * @throws IOException file could not be read
     */
    private void loadPositionMaps(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.loadPositionMaps(f);
    }

    private static File resolveFile(String fname, @Nullable File dir) throws IOException {
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

    /**
     * Get the value of processLauncher
     *
     * @return the value of processLauncher
     */
    private ProcessLauncherJFrame getProcessLauncher() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getProcessLauncher();
    }

    private void saveSimTeach(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.saveSimTeach(f);
    }

    private void saveLastPosMapFile(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.saveLastPosMapFile(f);
    }

    private void saveTeachProps(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.saveTeachProps(f);
    }

    private void saveSharedTools(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.saveSharedTools(f);
    }

    /**
     * Load the given setup file.
     *
     * @param f setup file to load
     * @throws IOException file could not be read
     */
    private void loadSetupFile(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.loadSetupFile(f);
    }

    private String lastUpdateTaskTableTaskNames @Nullable []  = null;

    private final ConcurrentHashMap<Integer, String> titleErrorMap = new ConcurrentHashMap<>();

    public void completeUpdateTasksTable(boolean needSetJListFuturesModel) {
        if (needSetJListFuturesModel) {
            setJListFuturesModel();
            jListFutures.setSelectedIndex(0);
        }
        Utils.autoResizeTableColWidths(jTableTasks);
        Utils.autoResizeTableRowHeights(jTableTasks);
        DefaultTreeModel model = (DefaultTreeModel) jTreeSelectedFuture.getModel();
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
        model.setRoot(rootTreeNode);
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }

    private void setJListFuturesModel() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Main");
        listModel.addElement("Last");
        listModel.addElement("Resume");
        listModel.addElement("Random");
        listModel.addElement("ContinuousDemo");
        listModel.addElement("stealAbort");
        listModel.addElement("unstealAbort");
        List<AprsSystem> aprsSystems = getAprsSystems();

        for (AprsSystem aprsSystemInterface : aprsSystems) {
            listModel.addElement(aprsSystemInterface.getTaskName() + "/actions");
            listModel.addElement(aprsSystemInterface.getTaskName() + "/abort");
            listModel.addElement(aprsSystemInterface.getTaskName() + "/resume");
            listModel.addElement(aprsSystemInterface.getTaskName() + "/program");
        }
        jListFutures.setModel(listModel);
    }

    @Nullable
    private volatile Supplier<@Nullable XFuture<?>> futureToDisplaySupplier = null;

    private void updateCurrentFutureDisplay(
            @UnknownInitialization AprsSupervisorDisplayJFrame this,
            boolean showDoneFutures,
            boolean showUnnamedFutures) {
        if (null == futureToDisplaySupplier) {
            return;
        }
        if (null == jTreeSelectedFuture) {
            return;
        }
        XFuture<?> xf = futureToDisplaySupplier.get();
        if (null != xf) {
            DefaultTreeModel model = (DefaultTreeModel) jTreeSelectedFuture.getModel();
            DefaultMutableTreeNode rootTreeNode = xfutureToNode(xf, showDoneFutures, showUnnamedFutures, 1);
            model.setRoot(rootTreeNode);
            expandAllNodes(jTreeSelectedFuture, 0, jTreeSelectedFuture.getRowCount(), 0, 0);
        }
    }

    private static final int MAX_EXPAND_NODE_COUNT = 2000;
    private static final int MAX_RECURSE_DEPTH = 100;

    public void loadRobotsTableFromSystemsList(List<AprsSystem> aprsSystems) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("call me from AWT event thread.");
        }
        synchronized (jTableRobots) {
            DefaultTableModel tm = (DefaultTableModel) jTableRobots.getModel();
            tm.setRowCount(0);
            for (AprsSystem aprsSys : aprsSystems) {
                String robotname = aprsSys.getRobotName();
                tm.addRow(new Object[]{
                    robotname,
                    true,
                    aprsSys.getRobotCrclHost(),
                    aprsSys.getRobotCrclPort(),
                    0,
                    runTimeToString(0)
                });
            }
            setupRobotTableListener();
        }
    }

    private volatile boolean robotTableListenerSetup = false;

    private void setupRobotTableListener() {
        if (!robotTableListenerSetup) {
            robotTableListenerSetup = true;
            jTableRobots.getModel().addTableModelListener(robotTableModelListener);
            for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                jTableRobots.setValueAt(true, i, 1);
            }
        }
        enableRobotTableModelListener();
    }

    public void updateRobotsTableFromMapsAndEnableAll(Map<String, Integer> robotDisableCountMap, Map<String, Long> robotDisableTotalTimeMap) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("call me from AWT event thread.");
        }
        logEvent("updateRobotsTableFromMapsAndEnableAll called.");
        disableRobotTableModelListener();
        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            boolean enableFromTable = getEnableFromRobotsTable(i);
            if (!enableFromTable) {
                System.out.println("updateRobotsTableFromMapsAndEnableAll jTableRobots.setValueAt(true," + i + ", 1)");
                jTableRobots.setValueAt(true, i, 1);
            }
            if (null != robotName) {
                int countFromTable = getDisableCountFromRobotsTable(i);
                int countFromMap = robotDisableCountMap.getOrDefault(robotName, 0);
                if (countFromTable != countFromMap) {
                    System.out.println("updateRobotsTableFromMapsAndEnableAll jTableRobots.setValueAt(" + countFromMap + "," + i + ", 4)");
                    jTableRobots.setValueAt(countFromMap, i, 4);
                }
                if (countFromTable != countFromMap || !enableFromTable) {
                    jTableRobots.setValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 5);
                }
            } else {
                logEventErr("jTableRobots.getValueAt(i=" + i + ", 0) returned null");
            }
        }
        Utils.autoResizeTableColWidths(jTableRobots);
        enableRobotTableModelListener();
    }

    private volatile boolean ignoreRobotTableChanges = false;

    private void disableRobotTableModelListener() {
        ignoreRobotTableChanges = true;

//        jTableRobots.getModel().removeTableModelListener(robotTableModelListener);
    }

    private static void expandAllNodes(JTree tree, int startingIndex, int rowCount, int startExpandCount, int recurseDepth) {
        if (recurseDepth > MAX_RECURSE_DEPTH) {
            return;
        }
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }

        if (tree.getRowCount() != rowCount && startExpandCount < MAX_EXPAND_NODE_COUNT) {
            expandAllNodes(tree, rowCount, tree.getRowCount(), startExpandCount + (rowCount > startingIndex ? (rowCount - startingIndex) : 0), recurseDepth + 1);
        }
    }

    @Nullable
    private static Field getField(Class<?> clss, String name) {
        Field f = null;
        try {
            f = clss.getField(name);
        } catch (NoSuchFieldException | SecurityException ex) {
        }
        if (null != f) {
            return f;
        }
        Field fields[] = clss.getFields();

        Field declaredFields[] = clss.getDeclaredFields();
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
                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.WARNING, null, ex1);
            }
        } catch (SecurityException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.WARNING, null, ex);
        }
        if (f == null && clss.getSuperclass() != null && !Objects.equals(clss.getSuperclass(), Object.class
        )) {
            return getField(clss.getSuperclass(), name);
        }
        return f;
    }

    static private DefaultMutableTreeNode cfutureToNode(
            CompletableFuture<?> future,
            boolean showDoneFutures,
            boolean showUnnamedFutures) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);
        Class<?> clss = future.getClass();
        Field stackField = getField(clss, "stack");
        if (null != stackField) {
            try {
                stackField.setAccessible(true);
                Object stackFieldObject = stackField.get(future);
                addNodesForStackObject(stackFieldObject, future, node, showDoneFutures, showUnnamedFutures);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                getLogger().log(Level.SEVERE, "", ex);
            }
        }
        return node;
    }

    private static void addNodesForStackObject(
            @Nullable Object stackFieldObject,
            CompletableFuture<?> future,
            DefaultMutableTreeNode node,
            boolean showDoneFutures,
            boolean showUnnamedFutures)
            throws SecurityException {
        if (null != stackFieldObject) {
            Class<?> stackFieldClass = stackFieldObject.getClass();
            Field depField = getField(stackFieldClass, "dep");
            if (null != depField) {
                try {
                    depField.setAccessible(true);
                    Object depFieldObject = depField.get(stackFieldObject);
                    if (depFieldObject != future) {
                        if (depFieldObject instanceof XFuture) {
                            XFuture<?> xf = (XFuture<?>) depFieldObject;
                            if (showDoneFutures
                                    || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                                node.add(xfutureToNode(xf, showDoneFutures, showUnnamedFutures, 1));
                            }
                        } else if (depFieldObject instanceof CompletableFuture) {
                            CompletableFuture<?> cf = (CompletableFuture<?>) depFieldObject;
                            boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                            if (showUnnamedFutures || notOk) {
                                if (showDoneFutures
                                        || (!cf.isDone() || notOk)) {
                                    node.add(cfutureToNode(cf, showDoneFutures, showUnnamedFutures));
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            Field nextField = getField(stackFieldClass, "next");
            if (null != nextField) {
                try {
                    nextField.setAccessible(true);
                    Object nextFieldObject = nextField.get(stackFieldObject);
                    if (null != nextFieldObject) {
                        Class<?> nextFieldClass = nextFieldObject.getClass();
                        Field nextFieldStackField = getField(nextFieldClass, "stack");
                        if (null != nextFieldStackField) {
                            Object nextFielStackObject = nextFieldStackField.get(nextFieldObject);
                            addNodesForStackObject(nextFielStackObject, future, node, showDoneFutures, showUnnamedFutures);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
//                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
        }
    }

    private static final int XFUTURE_MAX_DEPTH = 100;
    static private boolean firstDepthOverOccured = false;

    static private DefaultMutableTreeNode xfutureToNode(XFuture<?> future, boolean showDoneFutures, boolean showUnnamedFutures, int depth) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(future);
        if (depth >= XFUTURE_MAX_DEPTH) {
            if (!firstDepthOverOccured) {
                Logger.getLogger(AprsSystem.class
                        .getName()).log(Level.SEVERE, "xfutureToNode : depth >= XFUTURE_MAX_DEPTH");
                firstDepthOverOccured = true;
            }
            return node;
        }
        if (null != future && depth < XFUTURE_MAX_DEPTH) {
            ConcurrentLinkedDeque<?> deque = future.getAlsoCancel();
            if (null != deque) {
                for (Object o : deque) {
                    if (o instanceof XFuture) {
                        XFuture<?> xf = (XFuture<?>) o;
                        if (showDoneFutures
                                || (!xf.isDone() || xf.isCompletedExceptionally() || xf.isCancelled())) {
                            node.add(xfutureToNode(xf, showDoneFutures, showUnnamedFutures, (depth + 1)));
                        }
                    } else if (o instanceof CompletableFuture) {
                        CompletableFuture<?> cf = (CompletableFuture<?>) o;
                        boolean notOk = cf.isCompletedExceptionally() || cf.isCancelled();
                        if (showUnnamedFutures || notOk) {
                            if (showDoneFutures
                                    || (!cf.isDone() || notOk)) {
                                node.add(cfutureToNode(cf, showDoneFutures, showUnnamedFutures));
                            }
                        }
                    }
                }
            }
        }
        return node;
    }

    public XFutureVoid updateRandomTestCount(int count) {
        return Utils.runOnDispatchThread("updateRandomTest.runOnDispatchThread" + count,
                () -> {
//                    int count = randomTestCount.incrementAndGet();
//                    System.out.println("updateRandomTestCount count = " + count);
                    jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continuous Demo " + count);
                });
    }

    private static class SetTableRobotEnabledEvent {

        private final boolean enable;
        private final String robotName;
        private final XFuture<Boolean> future;

        public SetTableRobotEnabledEvent(boolean enable, String robotName, XFuture<Boolean> future) {
            this.enable = enable;
            this.robotName = robotName;
            this.future = future;
        }

        public boolean isEnable() {
            return enable;
        }

        public String getRobotName() {
            return robotName;
        }

        public XFuture<Boolean> getFuture() {
            return future;
        }

    };

//    private final ConcurrentLinkedDeque<SetTableRobotEnabledEvent> setTableRobotEnabledEventDeque = new ConcurrentLinkedDeque<>();
//    public XFuture<Boolean> setTableRobotEnabled(String robotName, boolean enable) {
//        XFuture<Boolean> f1 = new XFuture<>("setTableRobotEnabled");
//        SetTableRobotEnabledEvent setTableRobotEnabledEvent
//                = new SetTableRobotEnabledEvent(enable, robotName, f1);
//        XFuture<Boolean> f2 = Utils.supplyOnDispatchThread(() -> {
//            if (isTogglesAllowed()) {
//                for (int i = 0; i < jTableRobots.getRowCount(); i++) {
//                    String tableRobotName = (String) jTableRobots.getValueAt(i, 0);
//                    if (null != tableRobotName && Objects.equals(tableRobotName, robotName)) {
//
////                    Boolean enabled = (Boolean) jTableRobots.getValueAt(i, 1);
////                        Boolean wasEnabled = robotEnableMap.get(robotName);
//                        setTableRobotEnabledEventDeque.add(setTableRobotEnabledEvent);
//                        boolean enableFromTable = (Boolean) jTableRobots.getValueAt(i, 1);
//                        if (enableFromTable != enable) {
//                            disableRobotTableModelListener();
//                            System.out.println("setTableRobotEnabled(" + robotName + "," + enable + ") calling jTableRobots.setValueAt(" + enable + "," + i + ", 1)");
//                            jTableRobots.setValueAt(enable, i, 1);
//                            enableRobotTableModelListener();
//                        }
//                        return true;
//                    }
//                }
//            }
//            f1.complete(false);
//            return false;
//        });
//        return f2.thenCompose(x -> f1);
//    }
    public void updateRobotsTable() {
        if (closing) {
            return;
        }
        List<AprsSystem> aprsSystems = getAprsSystems();
        updateTeachSystemsComboBoxFromSystemsList(aprsSystems);
//        loadRobotsTableFromSystemsList(aprsSystems);
        Utils.autoResizeTableColWidths(jTableRobots);
        if (aprsSystems.size() >= 2) {
            String robot0Name = aprsSystems.get(0).getRobotName();
            String robot1Name = aprsSystems.get(1).getRobotName();
            setColorTextPanelLabelsAndIcons(robot0Name, robot1Name);
        } else if (aprsSystems.size() == 1) {
            String robot0Name = aprsSystems.get(0).getRobotName();
            if (null != robot0Name) {
                colorTextJPanel1.setLabelsAndIcons(
                        robot0Name,
                        ColorTextJPanel.getRobotIcon(robot0Name),
                        "",
                        null);
            }
        } else {
            colorTextJPanel1.setLabelsAndIcons(
                    "",
                    null,
                    "",
                    null);
        }
    }

    public void updateTeachSystemsComboBoxFromSystemsList(List<AprsSystem> aprsSystems) {
        DefaultComboBoxModel<String> cbmModel = (DefaultComboBoxModel<String>) jComboBoxTeachSystemView.getModel();
        cbmModel.removeAllElements();
        cbmModel.addElement("All");
        cbmModel.setSelectedItem("All");
        for (AprsSystem aprsSystemInterface : aprsSystems) {
            cbmModel.addElement(aprsSystemInterface.getMyThreadId() + " : " + aprsSystemInterface.toString());
        }
    }

    public void resetTeachSystemViewComboBox() {
        DefaultComboBoxModel<String> cbmModel = (DefaultComboBoxModel<String>) jComboBoxTeachSystemView.getModel();
        cbmModel.removeAllElements();
        cbmModel.addElement("All");
        cbmModel.setSelectedItem("All");
    }

    public void addTeachSystemViewComboBoxElement(String el) {
        DefaultComboBoxModel<String> cbmModel = (DefaultComboBoxModel<String>) jComboBoxTeachSystemView.getModel();
        cbmModel.addElement(el);
    }

    public void setColorTextPanelLabelsAndIcons(@Nullable String robot0Name, @Nullable String robot1Name) {
        if (null != robot0Name && null != robot1Name) {
            colorTextJPanel1.setLabelsAndIcons(
                    robot0Name,
                    ColorTextJPanel.getRobotIcon(robot0Name),
                    robot1Name,
                    ColorTextJPanel.getRobotIcon(robot1Name));
        } else if (null != robot0Name) {
            colorTextJPanel1.setLabelsAndIcons(
                    robot0Name,
                    ColorTextJPanel.getRobotIcon(robot0Name),
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
            getLogger().log(java.util.logging.Level.SEVERE, "", ex);

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
                AprsSupervisorDisplayJFrame amsFrame = new AprsSupervisorDisplayJFrame();
                amsFrame.setDefaultIconImage();
                amsFrame.startColorTextReader();
                amsFrame.loadAllPrevFiles()
                        .thenRun(() -> amsFrame.setVisible(true));
            }
        });
    }

    public Object2DOuterJPanel getObject2DOuterJPanel1() {
        return object2DOuterJPanel1;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.supervisor.colortextdisplay.ColorTextJPanel colorTextJPanel1;
    private javax.swing.JButton jButtonAddLine;
    private javax.swing.JButton jButtonAddSharedToolsRow;
    private javax.swing.JButton jButtonDeleteLine;
    private javax.swing.JButton jButtonDeleteSharedToolsRow;
    private javax.swing.JButton jButtonFuturesCancelAll;
    private javax.swing.JButton jButtonSaveSelectedPosMap;
    private javax.swing.JButton jButtonSetInFromCurrent;
    private javax.swing.JButton jButtonSetOutFromCurrent;
    private javax.swing.JButton jButtonSyncToolsFromRobots;
    private javax.swing.JButton jButtonSyncToolsToRobots;
    private javax.swing.JCheckBox jCheckBoxFutureLongForm;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContDemoReverseFirstOption;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemoRevFirst;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebug;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDisableTextPopups;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemFixedRandomTestSeed;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIndContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIndRandomToggleTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemKeepAndDisplayXFutureProfiles;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseAllForOne;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseResumeTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRandomTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRecordLiveImageMovie;
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
    private javax.swing.JMenuItem jMenuItemRandomTestReverseFirst;
    private javax.swing.JMenuItem jMenuItemRemoveSelectedSystem;
    private javax.swing.JMenuItem jMenuItemResetAll;
    private javax.swing.JMenuItem jMenuItemRunCustom;
    private javax.swing.JMenuItem jMenuItemSafeAbortAll;
    private javax.swing.JMenuItem jMenuItemSaveAll;
    private javax.swing.JMenuItem jMenuItemSavePosMaps;
    private javax.swing.JMenuItem jMenuItemSaveSetup;
    private javax.swing.JMenuItem jMenuItemSaveSetupAs;
    private javax.swing.JMenuItem jMenuItemScanAll;
    private javax.swing.JMenuItem jMenuItemSetMaxCycles;
    private javax.swing.JMenuItem jMenuItemStartAll;
    private javax.swing.JMenuItem jMenuItemStartAllReverse;
    private javax.swing.JMenuItem jMenuItemStartColorTextDisplay;
    private javax.swing.JMenuItem jMenuItemStartContinuousScanAndRun;
    private javax.swing.JMenuItem jMenuItemStartScanAllThenContinuousDemoRevFirst;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JPanel jPanelEvents;
    private javax.swing.JPanel jPanelFuture;
    private javax.swing.JPanel jPanelPosMapFiles;
    private javax.swing.JPanel jPanelPosMapSelectedFile;
    private javax.swing.JPanel jPanelPositionMappings;
    private javax.swing.JPanel jPanelRobots;
    private javax.swing.JPanel jPanelTasks;
    private javax.swing.JPanel jPanelTasksAndRobots;
    private javax.swing.JPanel jPanelTeachTable;
    private javax.swing.JPanel jPanelTools;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneEventsTable;
    private javax.swing.JScrollPane jScrollPaneListFutures;
    private javax.swing.JScrollPane jScrollPaneListFuturesKey;
    private javax.swing.JScrollPane jScrollPaneRobots;
    private javax.swing.JScrollPane jScrollPaneSharedToolsTable;
    private javax.swing.JScrollPane jScrollPaneTasks;
    private javax.swing.JScrollPane jScrollPaneTreeSelectedFuture;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTable jTableEvents;
    private javax.swing.JTable jTablePositionMappings;
    private javax.swing.JTable jTableRobots;
    private javax.swing.JTable jTableSelectedPosMapFile;
    private javax.swing.JTable jTableSharedTools;
    private javax.swing.JTable jTableTasks;
    private javax.swing.JTextField jTextFieldEventsMax;
    private javax.swing.JTextField jTextFieldRobotEnableToggleBlockers;
    private javax.swing.JTextField jTextFieldRunningTime;
    private javax.swing.JTextField jTextFieldSelectedPosMapFilename;
    private javax.swing.JTree jTreeSelectedFuture;
    private aprs.simview.Object2DOuterJPanel object2DOuterJPanel1;
    // End of variables declaration//GEN-END:variables
}
