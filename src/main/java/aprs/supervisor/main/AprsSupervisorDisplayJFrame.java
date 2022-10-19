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

import aprs.actions.executor.PositionMap;
import aprs.actions.executor.PositionMapEntry;
import aprs.conveyor.ConveyorVisJPanel;
import aprs.database.PhysicalItem;
import aprs.misc.IconImages;
import aprs.misc.MultiFileDialogInputFileInfo;
import aprs.misc.MultiFileDialogJPanel;
import aprs.misc.Utils;
import aprs.misc.Utils.UiSupplier;
import aprs.simview.Object2DOuterJPanel;
import aprs.supervisor.colortextdisplay.ColorTextJFrame;
import aprs.supervisor.colortextdisplay.ColorTextJPanel;
import aprs.supervisor.colortextdisplay.ColorTextOptionsJPanel;
import aprs.supervisor.colortextdisplay.ColorTextOptionsJPanel.ColorTextOptions;
import aprs.supervisor.screensplash.SplashScreen;
import aprs.system.AprsSystem;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLPosemath;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.tools.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static aprs.misc.AprsCommonLogger.println;
import static aprs.misc.Utils.getAprsIconUrl;
import static aprs.misc.Utils.runTimeToString;
import aprs.remote.AprsRemoteConsoleServerSocket;
import aprs.remote.Scriptable;
import static aprs.remote.Scriptable.scriptableOf;
import static crcl.utils.CRCLUtils.requireNonNull;
import static aprs.remote.Scriptable.scriptableOfStatic;
import crcl.ui.misc.NotificationsJPanel;
import crcl.utils.CRCLUtils;
import crcl.utils.server.CRCLServerSocket;

/**
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"all", "serial"})
public class AprsSupervisorDisplayJFrame extends javax.swing.JFrame {

    @MonotonicNonNull
    Supervisor supervisor = null;

    private GraphicsDevice graphicsDevice;

    @SafeEffect
    public GraphicsDevice getGraphicsDevice() {
        return graphicsDevice;
    }

    public @Nullable
    Supervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public void setSupervisorAndShow(Supervisor supervisor) {
        setSupervisor(supervisor);
        setVisible(true);
        updateRobotsTable();
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

    private final AtomicInteger robotTableIgnoreCount = new AtomicInteger();

    private class RobotTableModelListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            synchronized (jTableRobots) {
                try {
                    if (injTableRobotsSetValueAtCall) {
                        return;
                    }
                    handleRobotTableChange(e.getFirstRow(), e.getLastRow(), e.getColumn(), e.getType(), e.getSource());
                } catch (Exception exception) {
                    log(Level.SEVERE, "", exception);
                }
            }
        }
    }

    private volatile int ignoreRobotTableChangesCount = 0;
    private volatile int handleRobotTableChangesCount = 0;
    private final AtomicInteger tableChangeDisableCount = new AtomicInteger();
    private final AtomicInteger tableChangeEnableCount = new AtomicInteger();

    private volatile boolean lastTableChangeEnabledRobot = false;
    private volatile int lastTableChangeEnabledRobotRow = -1;

    @UIEffect
    private void handleRobotTableChange(int firstRow, int lastRow, int col, int type, Object source) {

        if (null == supervisor || supervisor.isResetting()) {
            return;
        }
        if (null == robotTaskMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }
        lastTableChangeEnabledRobot = false;
        lastTableChangeEnabledRobotRow = -1;
        if (type != TableModelEvent.UPDATE) {
            boolean enabled0 = getEnableFromRobotsTable(0);
            String rname = (String) jTableRobots.getValueAt(0, 0);
            boolean wasEnabled0 = (robotTaskMap.get(rname) != null);
            if (enabled0 != wasEnabled0) {
                println("wasEnabled = " + wasEnabled0);
                println("enabled = " + enabled0);
            }
            println("handleRobotTableChange: ignoring event of type = " + type);
            return;
        }
        if (col != 1) {
            String rname = (String) jTableRobots.getValueAt(0, 0);
            boolean enabled0 = getEnableFromRobotsTable(0);
            boolean wasEnabled0 = robotTaskMap.get(rname) != null;
            if (enabled0 != wasEnabled0) {
                println("wasEnabled = " + wasEnabled0);
                println("enabled = " + enabled0);
            }
            println("handleRobotTableChange: ignoring event for col=  " + col);
            return;
        }
        if (ignoreRobotTableChanges) {
            ignoreRobotTableChangesCount++;

            String rname = (String) jTableRobots.getValueAt(0, 0);
            boolean enabled0 = getEnableFromRobotsTable(0);
            boolean wasEnabled0 = robotTaskMap.get(rname) != null;
            if (enabled0 != wasEnabled0) {
                println("handleRobotTableChange: ignoreRobotTableChangesCount = " + ignoreRobotTableChangesCount);
                println("handleRobotTableChange: wasEnabled = " + wasEnabled0);
                println("handleRobotTableChange: enabled = " + enabled0);
                if (null != disableRobotTableModelListenerTrace) {
                    println("disableRobotTableModelListenerTrace = " + Arrays.toString(disableRobotTableModelListenerTrace));
                }
                jTableRobotsSetValueAt(wasEnabled0, 0, 1);
            }
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
            boolean wasEnabled = robotTaskMap.get(robotName) != null;
            final String checkedRobotName = robotName;
            println("handleRobotTableChange: i=" + i + ",robotName=" + robotName + ",enabled=" + enabled + ",wasEnabled=" + wasEnabled);
            if (enabled != wasEnabled) {
                final int fi = i;
                boolean togglesAllowed = isTogglesAllowed();
                if (enabled) {
                    int enableCount = tableChangeEnableCount.incrementAndGet();
                    supervisor.logEvent("handleRobotTableChange: enableCount=" + enableCount + ",togglesAllowed=" + togglesAllowed);
                } else {
                    int disableCount = tableChangeDisableCount.incrementAndGet();
                    supervisor.logEvent("handleRobotTableChange: disableCount=" + disableCount + ",togglesAllowed=" + togglesAllowed);
                }

                if (togglesAllowed) {
                    XFuture.runAsync(() -> {
                        if (isTogglesAllowed()) {
                            lastTableChangeEnabledRobot = enabled;
                            lastTableChangeEnabledRobotRow = fi;
                            setRobotEnabled(checkedRobotName, enabled);
                        } else {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                logEvent("Attempt to toggle robot enabled ignored.");
                                disableRobotTableModelListener();
                                println("handleRobotTableChange calling jTableRobotsSetValueAt(" + wasEnabled + "," + fi + ", 1)");
                                jTableRobotsSetValueAt(wasEnabled, fi, 1);
                                enableRobotTableModelListener();
                            });
                        }
                    }, getSupervisorExecutorService());
                } else {
                    logEvent("Attempt to toggle robot enabled ignored.");
                    disableRobotTableModelListener();
                    println("handleRobotTableChange calling jTableRobotsSetValueAt(" + wasEnabled + "," + fi + ", 1)");
                    jTableRobotsSetValueAt(wasEnabled, fi, 1);
                    enableRobotTableModelListener();
                }
                break;
            } else if (i == 0) {
                println("no enable change :handleRobotTableChange: i=" + i + ",robotName=" + robotName + ",enabled=" + enabled + ",wasEnabled=" + wasEnabled);
            }
        }
        enableRobotTableModelListener();
    }

    private final RobotTableModelListener robotTableModelListener = new RobotTableModelListener();

    /**
     * Creates new form AprsMulitSupervisorJFrame
     */
    @SuppressWarnings({"nullness", "initialization"})
    public AprsSupervisorDisplayJFrame() {
        initComponents();
        graphicsDevice = getGraphicsConfiguration().getDevice();
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
        Utils.autoResizeTableColWidthsOnDisplay(jTablePositionMappings);
        Utils.autoResizeTableRowHeightsOnDisplay(jTablePositionMappings);

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
                if (null != jTableSharedTools) {
                    Utils.autoResizeTableColWidths(jTableSharedTools);
                }
            } catch (Exception ex) {
                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    };

    @SuppressWarnings({"serial", "nullness"})
    private static class LabelledImagePanelTableCellRenderer extends DefaultTableCellRenderer {

        private final List<ImagePanel> areas = new ArrayList<>();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (value instanceof String) {
                try {
                    String robotName = (String) value;
                    BufferedImage image = ColorTextJPanel.getRobotImage(robotName);
                    if (null != image) {
                        while (areas.size() <= row) {
                            ImagePanel area = new ImagePanel(image, robotName);
                            area.setOpaque(true);
                            area.setVisible(true);
                            areas.add(area);
                        }
                    }
                    ImagePanel area = areas.get(row);
                    if (null != area) {
                        if (null != image) {
                            area.setImage(image);
                        }
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

    @SuppressWarnings({"serial", "nullness"})
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

    @SuppressWarnings({"serial", "nullness"})
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
                final Font font = table.getFont();
                if (null != font) {
                    area.setFont(font);
                }
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
            final Font font = table.getFont();
            if (null != font) {
                editTableArea.setFont(font);
            }
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

    @SuppressWarnings({"initialization", "nullness"})
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

    @SuppressWarnings({"initialization", "nullness"})
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
                            setText(xf.getName() + " : " + cancelledDependant.shortCancelString());
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

    @SuppressWarnings({"nullness"})
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

    @SuppressWarnings({"nullness"})
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

    @UIEffect
    private void enableRobotTableModelListener() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("call me from AWT event thread.");
        }
//        if (!ignoreRobotTableChanges && !resetting) {
//            throw new IllegalThreadStateException("ignoreRobotTableChanges=" + ignoreRobotTableChanges);
//        }
//        jTableRobots.getModel().addTableModelListener(robotTableModelListener);
        ignoreRobotTableChanges = false;
    }

    public void setDefaultIconImage() {
        try {
            URL url = getAprsIconUrl();
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

    private final List<JCheckBox> robotsEnableCelRendererComponentList = new ArrayList<>();

    private final ListSelectionListener jListFuturesSelectionListener
            = this::handleListFuturesSelectionEvent;

    List<AprsSystem> getAprsSystems() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.systems();
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

            case "MultiCycleTest":
                futureToDisplaySupplier = () -> sup2.getLastCompleteMultiCycleTestFuture();
                break;

            case "Conveyor":
                futureToDisplaySupplier = () -> sup2.getConveyorTestFuture();
                break;

            case "Last":
                futureToDisplaySupplier = () -> sup2.getLFR();
                break;

            case "Gui.Last":
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

            case "prepStart":
                futureToDisplaySupplier = () -> interactivStartFuture;
                break;

            case "prepReset":
                futureToDisplaySupplier = () -> internalInteractiveResetAllFuture;
                break;

            default:
                List<AprsSystem> aprsSystems = sup2.systems();
                boolean taskFound = false;
                int sindex = selectedFutureString.indexOf('/');
                if (sindex > 0 && sindex < selectedFutureString.length()) {
                    String selectedFutureStringBase = selectedFutureString.substring(0, sindex);
                    String selectedFutureStringExt = selectedFutureString.substring(sindex + 1);
                    for (AprsSystem sys : aprsSystems) {

                        if (sys.getTaskName().equals(selectedFutureStringBase)) {
                            taskFound = true;
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

                                default:
                                    System.err.println("bad selectedFutureString =" + selectedFutureString);
                                    futureToDisplaySupplier = () -> new XFutureVoid("bad selectedFutureString =" + selectedFutureString);
                                    break;
                            }
                            return;
                        }
                    }
                    if (taskFound) {
                        break;
                    }
                }
                if (!taskFound) {
                    System.err.println("bad selectedFutureString =" + selectedFutureString);
                    futureToDisplaySupplier = () -> new XFutureVoid("bad selectedFutureString =" + selectedFutureString);
                }
                break;
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

    private @Nullable
    XFutureVoid getRandomTestFuture() {
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

    private @Nullable
    XFutureVoid getResumeFuture() {
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
        return supervisor.loadAllPrevFiles(null);
    }

    JPanel blankPanel = new JPanel();

    private @Nullable
    AprsSystem findSystemWithRobot(String robot) {
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
                            pme.getInputX(), pme.getInputY(), pme.getInputZ(),
                            pme.getInputX() + pme.getOffsetX(), pme.getInputY() + pme.getOffsetY(), pme.getInputZ() + pme.getOffsetZ(),
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
        if (supervisor.isResetting()) {
            return;
        }
        supervisor.setRobotEnabled(robotName, enabled, null);
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

    public void refreshRobotsTable(Map<String, String> robotTaskMap, Map<String, Integer> robotDisableCountMap, Map<String, Long> robotDisableTotalTimeMap, StackTraceElement callerTrace[]) {
        if (null == robotTaskMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }

        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            if (null != robotName) {
                final String taskName = robotTaskMap.get(robotName);
                boolean enabledInMap = taskName != null;
                boolean enabledInTable = getEnableFromRobotsTable(i);
                if (enabledInMap != enabledInTable) {
//                    println("callerTrace = " + XFuture.traceToString(callerTrace));
//                    println("refreshRobotTable setValueAt(" + enabledInMap + "," + i + ",1) robotName=" + robotName);
                    disableRobotTableModelListener();
                    jTableRobotsSetValueAt(enabledInMap, i, 1);
                    enableRobotTableModelListener();
                }
                jTableRobotsSetValueAt(taskName, i, 4);
                int mapDisableCount = robotDisableCountMap.getOrDefault(robotName, 0);
                int tableDisableCount = getDisableCountFromRobotsTable(i);
                if (mapDisableCount != tableDisableCount) {
//                    println("refreshRobotTable setValueAt(" + mapDisableCount + "," + i + ",4) robotName=" + robotName);
                    disableRobotTableModelListener();
                    jTableRobotsSetValueAt(mapDisableCount, i, 5);
                    enableRobotTableModelListener();
                }
                if (mapDisableCount != tableDisableCount || !enabledInMap || !enabledInTable) {
                    disableRobotTableModelListener();
                    jTableRobotsSetValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 6);
                    enableRobotTableModelListener();
                }
            }
        }
        Utils.autoResizeTableColWidths(jTableRobots);
    }

    private int getDisableCountFromRobotsTable(int row) {
        Object o = jTableRobots.getValueAt(row, 5);
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

//    private void printReturnRobotTraceInfo() {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.printReturnRobotTraceInfo();
//    }
    //    private XFuture<@Nullable Void> returnRobots(String comment) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.returnRobotsAsyncOnSupervisorExecutor(comment);
//    }
    private @Nullable
    XFutureVoid getContinuousDemoFuture() {
        if (null != supervisor) {
            return supervisor.getContinuousDemoFuture();
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

    public XFutureVoid showErrorSplash(String errMsgString) {
        return fullAbortAll()
                .thenComposeAsyncToVoid(() -> {
                    final GraphicsConfiguration graphicsConfiguration
                            = requireNonNull(
                                    this.getGraphicsConfiguration(),
                                    "this.getGraphicsConfiguration() : this=" + this);
                    final GraphicsDevice gd = graphicsConfiguration.getDevice();
                    return showMessageFullScreen(errMsgString, 80.0f,
                            null,
                            SplashScreen.getRedYellowColorList(), gd);
                }, getSupervisorExecutorService());
    }

    public boolean isShowSplashMessagesSelected() {
        return jCheckBoxMenuItemShowSplashMessages.isSelected();
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

        if (jCheckBoxMenuItemShowSplashMessages.isSelected() && null != supervisor && !supervisor.isResetting()) {
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

    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public static long parseTimeString(String timeString) throws ParseException {
//        String dateString = getDateTimeString(date.getTime());
//        System.out.println("dateString = " + dateString);
//        String dateOnlyString = dateString.substring(0,dateString.length()-timeString.length());
//        System.out.println("dateOnlyString = " + dateOnlyString);
//        String fullString = dateOnlyString+timeString;
        return DEFAULT_DATE_FORMAT.parse(timeString).getTime();
    }

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

    private volatile @Nullable
    PrintStream logPrintStream = null;

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
    volatile int maxThreadNameStringLen = 0;

    private long getFirstEventTime() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getFirstEventTime();
    }

//    private void setFirstEventTime(long firstEventTime) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.setFirstEventTime(firstEventTime);
//    }
    private long getAbortEventTime() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getFirstEventTime();
    }

    //    private void setAbortEventTime(long abortEventTime) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.setAbortEventTime(abortEventTime);
//    }
//
//    private void logEventPrivate(long time, String s, int blockerSize, int ecc, int cdc, int errs, String threadname,StackTraceElement trace[]) {
//
//        if (getFirstEventTime() > 0) {
//            updateRunningTime();
//            startUpdateRunningTimeTimer();
//        }
//        String timeString = getTimeString(time);
//        if (null == logPrintStream) {
//            try {
//                File logFile = Utils.createTempFile("events_log_", ".txt");
//                println("logFile = " + logFile.getCanonicalPath());
//                logPrintStream = new PrintStream(new FileOutputStream(logFile));
//
//            } catch (IOException ex) {
//                Logger.getLogger(AprsSupervisorDisplayJFrame.class
//                        .getName()).log(Level.SEVERE, "", ex);
//            }
//        }
//        String fullLogString = timeString + " \t" + blockerSize + " \t" + s + " \t:thread= " + threadname;
//        if (null != logPrintStream) {
//            logPrintStream.println(fullLogString);
//        }
//        println(fullLogString);
//        addOldEventToTable(time, blockerSize, ecc, cdc, errs, s, threadname,Utils.traceToString(trace));
//    }
    private int addOldEventToTableCount = 0;
    private int addOldEventToTableResizeCount = 0;
    private long addOldEventToTableTime = 0;

    public void addOldEventToTable(long time, int blockerSize, int ecc, int cdc, int errs, String s, String threadname, String traceString) {
        DefaultTableModel tm = (DefaultTableModel) jTableEvents.getModel();
        if (tm.getRowCount() > eventsDisplayMax && eventsDisplayMax > 0) {
            if (!jCheckBoxScrollEvents.isSelected()) {
                return;
            }
            tm.removeRow(0);
        }
        addOldEventToTableCount++;
        long timediff = time - addOldEventToTableTime;
        addOldEventToTableTime = time;
        tm.addRow(new Object[]{addOldEventToTableCount, getTimeString(time), timediff, blockerSize, ecc, cdc, errs, s, threadname, traceString});
        final int sLength = s.length();
        final int threadNameLength = threadname.length();
        if (addOldEventToTableCount % 50 == 1 || sLength > maxEventStringLen || threadNameLength > maxThreadNameStringLen) {
            if (jCheckBoxScrollEvents.isSelected()) {
                addOldEventToTableResizeCount++;
                Utils.autoResizeTableColWidths(jTableEvents);
            }
            if (sLength > maxEventStringLen) {
                maxEventStringLen = sLength;
            }
            if (threadNameLength > maxThreadNameStringLen) {
                maxThreadNameStringLen = threadNameLength;
            }
        } else {
            if (jCheckBoxScrollEvents.isSelected()) {
                scrollToEnd(jTableEvents);
            }
        }
    }

    public void updateRunningTime() {
        try {
            if (getFirstEventTime() > 0 && !jCheckBoxMenuItemPause.isSelected()) {

                long runningTimeMillis = System.currentTimeMillis() - getFirstEventTime();
                if (getFirstEventTime() < getAbortEventTime()) {
                    runningTimeMillis = getAbortEventTime() - getFirstEventTime();
                }
                String s = runTimeToString(runningTimeMillis);
                jTextFieldRunningTime.setText(s);
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
            NotificationsJPanel.showException(ex);
            if (null != runTimeTimer) {
                runTimeTimer.stop();
                runTimeTimer = null;
            }
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
    private @MonotonicNonNull
    Map<String, String> robotTaskMap;

    public @Nullable
    Map<String, String> getRobotTaskMap() {
        return robotTaskMap;
    }

    public void setRobotTaskMap(Map<String, String> robotTaskMap) {
        this.robotTaskMap = robotTaskMap;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPaneMain = new javax.swing.JTabbedPane();
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
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableSelectedPosMapFile = new javax.swing.JTable();
        jTextFieldSelectedPosMapFilename = new javax.swing.JTextField();
        jPanelSelectedPosMapFileTopButtons = new javax.swing.JPanel();
        jButtonGoOut = new javax.swing.JButton();
        jButtonSetOutFromCurrent = new javax.swing.JButton();
        jButtonAddLine = new javax.swing.JButton();
        jButtonDeleteLine = new javax.swing.JButton();
        jButtonSaveSelectedPosMap = new javax.swing.JButton();
        jButtonPlotPositionMap = new javax.swing.JButton();
        jButtonSetInFromCurrent = new javax.swing.JButton();
        jButtonGoIn = new javax.swing.JButton();
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
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaFutureDetails = new javax.swing.JTextArea();
        jPanelEvents = new javax.swing.JPanel();
        jScrollPaneEventsTable = new javax.swing.JScrollPane();
        jTableEvents = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldEventsMax = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldRunningTime = new javax.swing.JTextField();
        jCheckBoxScrollEvents = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldEventsLogFile = new javax.swing.JTextField();
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
        jMenuItemLoadEventsLog = new javax.swing.JMenuItem();
        jMenuItemSaveAll = new javax.swing.JMenuItem();
        jMenuItemOpenAll = new javax.swing.JMenuItem();
        jMenuActions = new javax.swing.JMenu();
        jMenuItemStartAll = new javax.swing.JMenuItem();
        jMenuItemStartAllReverse = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSafeAbortAll = new javax.swing.JMenuItem();
        jMenuItemImmediateAbortAll = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemContinueAll = new javax.swing.JMenuItem();
        jMenuItemReturnRobot = new javax.swing.JMenuItem();
        jMenuItemEnableAndContinueAll = new javax.swing.JMenuItem();
        jMenuItemConnectAll = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinuousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemContinuousDemoRevFirst = new javax.swing.JCheckBoxMenuItem();
        jMenuItemScanAll = new javax.swing.JMenuItem();
        jMenuItemLookForPartsAll = new javax.swing.JMenuItem();
        jMenuItemStep = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
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
        jMenuItemSetConveyorViewCloneSystem = new javax.swing.JMenuItem();
        jCheckBoxMenuItemUseCorrectionModeByDefault = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemEnableConveyorControlView = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemKeepDisabled = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemSkipDisabled = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemBlockTransfers = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemSingleStep = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemEnableRemoteConsole = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetGlobalSpeedOverride = new javax.swing.JMenuItem();
        jMenuSpecialTests = new javax.swing.JMenu();
        jMenuItemMultiCycleTest = new javax.swing.JMenuItem();
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
        jMenuItemConveyorTest = new javax.swing.JMenuItem();
        jMenuItemReloadSimFiles = new javax.swing.JMenuItem();
        jMenuItemRestoreOrigRobotInfo = new javax.swing.JMenuItem();
        jMenuItemStartScanAllThenContinuousConveyorDemoRevFirst = new javax.swing.JMenuItem();
        jMenuItemSpecialTestFlipFanucPartWithMotomanHelp = new javax.swing.JMenuItem();
        jMenuItemSpecialTestFlipMotomanPartWithFanucHelp = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Multi Aprs Supervisor");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

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
                .addComponent(jScrollPaneTasks)
                .addContainerGap())
        );
        jPanelTasksLayout.setVerticalGroup(
            jPanelTasksLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneTasks, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
        );

        jPanelRobots.setBorder(javax.swing.BorderFactory.createTitledBorder("Robots"));

        jTableRobots.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jTableRobots.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Robot", "Enabled", "Host", "Port", "Task", "Disable Count", "Disable Time"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, false, false
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
                    .addComponent(jScrollPaneRobots, javax.swing.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
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
                .addContainerGap()
                .addComponent(jPanelRobots, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorTextJPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanelTasksAndRobotsLayout.createSequentialGroup()
                .addComponent(jPanelTasks, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(1, 1, 1))
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

        jTabbedPaneMain.addTab("Tasks and Robots", jPanelTasksAndRobots);

        jPanelPositionMappings.setLayout(new java.awt.GridLayout(2, 1));

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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 942, Short.MAX_VALUE)
                .addGap(6, 6, 6))
        );
        jPanelPosMapFilesLayout.setVerticalGroup(
            jPanelPosMapFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapFilesLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelPositionMappings.add(jPanelPosMapFiles);

        jPanelPosMapSelectedFile.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected File"));

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

        jButtonGoOut.setText("Go Out");
        jButtonGoOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGoOutActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonGoOut);

        jButtonSetOutFromCurrent.setText("Set Out From Selected Column System");
        jButtonSetOutFromCurrent.setEnabled(false);
        jButtonSetOutFromCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetOutFromCurrentActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonSetOutFromCurrent);

        jButtonAddLine.setText("Add Line");
        jButtonAddLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddLineActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonAddLine);

        jButtonDeleteLine.setText("Delete Line");
        jButtonDeleteLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteLineActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonDeleteLine);

        jButtonSaveSelectedPosMap.setText("Save");
        jButtonSaveSelectedPosMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveSelectedPosMapActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonSaveSelectedPosMap);

        jButtonPlotPositionMap.setText("Plot");
        jButtonPlotPositionMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlotPositionMapActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonPlotPositionMap);

        jButtonSetInFromCurrent.setText("Set In From Selected Row System");
        jButtonSetInFromCurrent.setEnabled(false);
        jButtonSetInFromCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetInFromCurrentActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonSetInFromCurrent);

        jButtonGoIn.setText("Go In");
        jButtonGoIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGoInActionPerformed(evt);
            }
        });
        jPanelSelectedPosMapFileTopButtons.add(jButtonGoIn);

        javax.swing.GroupLayout jPanelPosMapSelectedFileLayout = new javax.swing.GroupLayout(jPanelPosMapSelectedFile);
        jPanelPosMapSelectedFile.setLayout(jPanelPosMapSelectedFileLayout);
        jPanelPosMapSelectedFileLayout.setHorizontalGroup(
            jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapSelectedFileLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jTextFieldSelectedPosMapFilename)
                    .addComponent(jPanelSelectedPosMapFileTopButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelPosMapSelectedFileLayout.setVerticalGroup(
            jPanelPosMapSelectedFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPosMapSelectedFileLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelectedPosMapFileTopButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldSelectedPosMapFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanelPositionMappings.add(jPanelPosMapSelectedFile);

        jTabbedPaneMain.addTab("Position Mapping", jPanelPositionMappings);

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
        jTreeSelectedFuture.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTreeSelectedFutureValueChanged(evt);
            }
        });
        jScrollPaneTreeSelectedFuture.setViewportView(jTreeSelectedFuture);

        jCheckBoxUpdateFutureAutomatically.setText("Update");
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

        jCheckBoxShowDoneFutures.setText("Completed");
        jCheckBoxShowDoneFutures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowDoneFuturesActionPerformed(evt);
            }
        });

        jCheckBoxShowUnnamedFutures.setText("Unnamed");
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

        jCheckBoxFutureLongForm.setText("Long");
        jCheckBoxFutureLongForm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxFutureLongFormActionPerformed(evt);
            }
        });

        jTextAreaFutureDetails.setColumns(20);
        jTextAreaFutureDetails.setRows(5);
        jScrollPane3.setViewportView(jTextAreaFutureDetails);

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelFutureLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxShowUnnamedFutures)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxShowDoneFutures)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxUpdateFutureAutomatically)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxFutureLongForm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 266, Short.MAX_VALUE)
                        .addComponent(jButtonFuturesCancelAll))
                    .addComponent(jScrollPaneTreeSelectedFuture, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3))
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
                    .addComponent(jScrollPaneTreeSelectedFuture, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPaneListFutures, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelFutureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3)
                    .addComponent(jScrollPaneListFuturesKey, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("Futures", jPanelFuture);

        jTableEvents.setAutoCreateRowSorter(true);
        jTableEvents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Index", "Time", "TimeDiff", "Locks", "Enable Changes", "Cycles", "Errs", "Event", "Thread", "Trace"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Long.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, true, false, true
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

        jCheckBoxScrollEvents.setSelected(true);
        jCheckBoxScrollEvents.setText("Scroll");

        jLabel7.setText("File:");

        javax.swing.GroupLayout jPanelEventsLayout = new javax.swing.GroupLayout(jPanelEvents);
        jPanelEvents.setLayout(jPanelEventsLayout);
        jPanelEventsLayout.setHorizontalGroup(
            jPanelEventsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEventsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelEventsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneEventsTable)
                    .addGroup(jPanelEventsLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldEventsMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxScrollEvents)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldRunningTime, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldEventsLogFile, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)))
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
                    .addComponent(jTextFieldRunningTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxScrollEvents)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldEventsLogFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneEventsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("Events", jPanelEvents);

        object2DOuterJPanel1.setMinimumSize(new java.awt.Dimension(0, 0));

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
                    .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 946, Short.MAX_VALUE)
                    .addComponent(jComboBoxTeachSystemView, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelTeachTableLayout.setVerticalGroup(
            jPanelTeachTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTeachTableLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboBoxTeachSystemView, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE))
        );

        jTabbedPaneMain.addTab("Teach", jPanelTeachTable);

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
                    .addComponent(jScrollPaneSharedToolsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 946, Short.MAX_VALUE)
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
                .addComponent(jScrollPaneSharedToolsTable, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("Shared Tools", jPanelTools);

        getContentPane().add(jTabbedPaneMain);

        jMenuFile.setText("File");

        jMenuItemSaveSetup.setText("Save Setup");
        jMenuItemSaveSetup.setEnabled(false);
        jMenuItemSaveSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveSetupActionPerformed(evt);
            }
        });
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

        jMenuItemLoadEventsLog.setText("Load Events Log ...");
        jMenuItemLoadEventsLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadEventsLogActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemLoadEventsLog);

        jMenuItemSaveAll.setText("Save All ... ");
        jMenuItemSaveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAllActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveAll);

        jMenuItemOpenAll.setText("Open All ... ");
        jMenuItemOpenAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenAllActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpenAll);

        jMenuBar1.add(jMenuFile);

        jMenuActions.setText("Actions");

        jMenuItemStartAll.setText("Start All");
        jMenuItemStartAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemStartAll);

        jMenuItemStartAllReverse.setText("Start All Reverse");
        jMenuItemStartAllReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartAllReverseActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemStartAllReverse);
        jMenuActions.add(jSeparator1);

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
        jMenuActions.add(jSeparator2);

        jMenuItemContinueAll.setText("Continue All");
        jMenuItemContinueAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemContinueAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemContinueAll);

        jMenuItemReturnRobot.setText("Return Robot");
        jMenuItemReturnRobot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemReturnRobotActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemReturnRobot);

        jMenuItemEnableAndContinueAll.setText("Enable And Continue All");
        jMenuItemEnableAndContinueAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEnableAndContinueAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemEnableAndContinueAll);

        jMenuItemConnectAll.setText("Connect All");
        jMenuItemConnectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemConnectAll);

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

        jMenuItemLookForPartsAll.setText("Look ForParts All");
        jMenuItemLookForPartsAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLookForPartsAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemLookForPartsAll);

        jMenuItemStep.setText("Step");
        jMenuItemStep.setEnabled(false);
        jMenuItemStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStepActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemStep);
        jMenuActions.add(jSeparator3);

        jCheckBoxMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAUSE, 0));
        jCheckBoxMenuItemPause.setText("Pause");
        jCheckBoxMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemPause);

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

        jMenuItemSetConveyorViewCloneSystem.setText("Set Conveyor View Clone System ...");
        jMenuItemSetConveyorViewCloneSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetConveyorViewCloneSystemActionPerformed(evt);
            }
        });
        jMenuOptions.add(jMenuItemSetConveyorViewCloneSystem);

        jCheckBoxMenuItemUseCorrectionModeByDefault.setSelected(true);
        jCheckBoxMenuItemUseCorrectionModeByDefault.setText("Use Correction Mode By Default");
        jCheckBoxMenuItemUseCorrectionModeByDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemUseCorrectionModeByDefaultActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemUseCorrectionModeByDefault);

        jCheckBoxMenuItemEnableConveyorControlView.setText("Enable Conveyor Control View");
        jCheckBoxMenuItemEnableConveyorControlView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemEnableConveyorControlViewActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemEnableConveyorControlView);

        jCheckBoxMenuItemKeepDisabled.setText("Keep disabled systems disabled.");
        jCheckBoxMenuItemKeepDisabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemKeepDisabledActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemKeepDisabled);

        jCheckBoxMenuItemSkipDisabled.setText("Skip disabled systems.");
        jCheckBoxMenuItemSkipDisabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSkipDisabledActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemSkipDisabled);

        jCheckBoxMenuItemBlockTransfers.setText("Block Robot Transfers");
        jCheckBoxMenuItemBlockTransfers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemBlockTransfersActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemBlockTransfers);

        jCheckBoxMenuItemSingleStep.setText("Single Step");
        jCheckBoxMenuItemSingleStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemSingleStepActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemSingleStep);

        jCheckBoxMenuItemEnableRemoteConsole.setText("Enable Remotely Accessible Console");
        jCheckBoxMenuItemEnableRemoteConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemEnableRemoteConsoleActionPerformed(evt);
            }
        });
        jMenuOptions.add(jCheckBoxMenuItemEnableRemoteConsole);

        jMenuItemSetGlobalSpeedOverride.setText("Set Global CRCL Server Socket Speed Override . . . ");
        jMenuItemSetGlobalSpeedOverride.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetGlobalSpeedOverrideActionPerformed(evt);
            }
        });
        jMenuOptions.add(jMenuItemSetGlobalSpeedOverride);

        jMenuBar1.add(jMenuOptions);

        jMenuSpecialTests.setText("Special Tests");

        jMenuItemMultiCycleTest.setText("Multi Cycle Test");
        jMenuItemMultiCycleTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMultiCycleTestActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemMultiCycleTest);

        jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continuous Demo");
        jCheckBoxMenuItemRandomTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemRandomTestActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jCheckBoxMenuItemRandomTest);

        jCheckBoxMenuItemPauseResumeTest.setText("Pause Resume Test");
        jCheckBoxMenuItemPauseResumeTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseResumeTestActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jCheckBoxMenuItemPauseResumeTest);

        jMenuItemResetAll.setText("Reset All");
        jMenuItemResetAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemResetAllActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemResetAll);

        jMenuItemDbgAction.setText("Dbg Action");
        jMenuItemDbgAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDbgActionActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemDbgAction);

        jMenuItemRandomTestReverseFirst.setText("Randomized Enable Toggle Continuous Demo (Reverse First) ");
        jMenuItemRandomTestReverseFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRandomTestReverseFirstActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemRandomTestReverseFirst);

        jCheckBoxMenuItemIndContinuousDemo.setText("(Independant) Continuous Demo");
        jCheckBoxMenuItemIndContinuousDemo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIndContinuousDemoActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jCheckBoxMenuItemIndContinuousDemo);

        jCheckBoxMenuItemIndRandomToggleTest.setText("(Independant) Continuous Demo With Randomized Enable Toggle    ");
        jCheckBoxMenuItemIndRandomToggleTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIndRandomToggleTestActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jCheckBoxMenuItemIndRandomToggleTest);

        jMenuItemRunCustom.setText("Run custom code");
        jMenuItemRunCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRunCustomActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemRunCustom);

        jMenuItemStartContinuousScanAndRun.setText("Start Continuous Scan and Run");
        jMenuItemStartContinuousScanAndRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartContinuousScanAndRunActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemStartContinuousScanAndRun);

        jMenuItemStartScanAllThenContinuousDemoRevFirst.setText("Start Scan All Then Continuous Demo Rev First");
        jMenuItemStartScanAllThenContinuousDemoRevFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemStartScanAllThenContinuousDemoRevFirst);

        jMenuItemConveyorTest.setText("Conveyor Test");
        jMenuItemConveyorTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConveyorTestActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemConveyorTest);

        jMenuItemReloadSimFiles.setText("Reload Sim Files");
        jMenuItemReloadSimFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemReloadSimFilesActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemReloadSimFiles);

        jMenuItemRestoreOrigRobotInfo.setText("Restore Original Robot Info");
        jMenuItemRestoreOrigRobotInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRestoreOrigRobotInfoActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemRestoreOrigRobotInfo);

        jMenuItemStartScanAllThenContinuousConveyorDemoRevFirst.setText("Start Scan All Then Continuous Conveyor Demo Rev First");
        jMenuItemStartScanAllThenContinuousConveyorDemoRevFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartScanAllThenContinuousConveyorDemoRevFirstActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemStartScanAllThenContinuousConveyorDemoRevFirst);

        jMenuItemSpecialTestFlipFanucPartWithMotomanHelp.setText("Flip Fanuc Part with Motoman Help");
        jMenuItemSpecialTestFlipFanucPartWithMotomanHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSpecialTestFlipFanucPartWithMotomanHelpActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemSpecialTestFlipFanucPartWithMotomanHelp);

        jMenuItemSpecialTestFlipMotomanPartWithFanucHelp.setText("Flip Motoman Part with Fanuc Help");
        jMenuItemSpecialTestFlipMotomanPartWithFanucHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSpecialTestFlipMotomanPartWithFanucHelpActionPerformed(evt);
            }
        });
        jMenuSpecialTests.add(jMenuItemSpecialTestFlipMotomanPartWithFanucHelp);

        jMenuBar1.add(jMenuSpecialTests);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
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
        File chosenFile = chooseFileForSaveAs(Supervisor.getLastSetupFile(null));
        if (null != chosenFile) {
            saveSetupFile(chosenFile);
        }
    }

    public @Nullable
    File chooseFileForSaveAs(@Nullable File prevChooserFile) throws HeadlessException, IOException {
        JFileChooser chooser = new JFileChooser(Utils.getAprsUserHomeDir());
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

    @UIEffect
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
    private XFutureVoid browseOpenSetup() throws IOException {
        File prevChosenFile = Supervisor.getLastSetupFile(null);
        File chosenFile = chooseSetupFileToOpen(prevChosenFile);
        if (null != chosenFile) {
            return loadSetupFile(chosenFile);
        } else {
            throw new RuntimeException("User cancelled choosing file.");
        }
    }

    public @Nullable
    File chooseSetupFileToOpen(@Nullable File prevChosenFile) throws HeadlessException, IOException {
        JFileChooser chooser = new JFileChooser(Utils.getAprsUserHomeDir());
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

    @UIEffect
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

    public @Nullable
    File chooseSystemPropertiesFileToOpen() throws HeadlessException {
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

    @UIEffect
    private void jMenuItemRemoveSelectedSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRemoveSelectedSystemActionPerformed
        try {
            int selectedIndex = jTableTasks.getSelectedRow();
            performRemoveSelectedSystemAction(selectedIndex);
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemRemoveSelectedSystemActionPerformed

    private void performRemoveSelectedSystemAction(int selectedIndex) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        File fileToSave = supervisor.getSetupFile();
        int response
                = JOptionPane.showConfirmDialog(this, "Save Current APRS Supervisor file : " + fileToSave);
        if (response == JOptionPane.YES_OPTION) {
            supervisor.performRemoveSelectedSystemAction(selectedIndex);
        }
    }

    private volatile @Nullable
    XFuture<?> lastFutureReturned = null;

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
                .thenComposeAsyncToVoid(x -> x, supervisor.getSupervisorExecutorService());
    }

    @UIEffect
    private void jMenuItemStartAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllActionPerformed
        interactivStartFutureSupplier(() -> performStartAllAction(),
                jMenuItemStartAll.getText())
                .thenCompose(x -> Utils.runOnDispatchThread(() -> JOptionPane.showMessageDialog(this, "Start all done.")));
    }//GEN-LAST:event_jMenuItemStartAllActionPerformed

    private XFutureVoid performStartAllAction() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.performStartAllAction();
    }

    private XFutureVoid prepActions() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.prepActions();
    }

    @UIEffect
    private void jMenuItemSavePosMapsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePosMapsActionPerformed
        try {
            browseAndSavePositionMappings();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jMenuItemSavePosMapsActionPerformed

    private void browseAndSavePositionMappings() throws HeadlessException, IOException {
        File chosenFile = choosePositionMappingsFileForSaveAs(Supervisor.getLastPositionMappingsFilesFile(null));
        if (null != chosenFile) {
            savePositionMaps(chosenFile);
        }
    }

    public @Nullable
    File choosePositionMappingsFileForSaveAs(@Nullable File prevChosenFile) throws HeadlessException {
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

    @UIEffect
    private void jTablePositionMappingsMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePositionMappingsMousePressed
        if (evt.isPopupTrigger()) {
            showPosTablePopup(evt.getLocationOnScreen());
        }
    }//GEN-LAST:event_jTablePositionMappingsMousePressed

    @UIEffect
    private void jTablePositionMappingsMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePositionMappingsMouseReleased
        if (evt.isPopupTrigger()) {
            showPosTablePopup(evt.getLocationOnScreen());
        }
    }//GEN-LAST:event_jTablePositionMappingsMouseReleased

    @UIEffect
    private void jTablePositionMappingsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePositionMappingsMouseClicked
        if (evt.isPopupTrigger()) {
            showPosTablePopup(evt.getLocationOnScreen());
        }
    }//GEN-LAST:event_jTablePositionMappingsMouseClicked

    @UIEffect
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
        File chosenFile = choosePosMapsFileToOpen(Supervisor.getLastPositionMappingsFilesFile(null));
        if (null != chosenFile) {
            loadPositionMaps(chosenFile);
        }
    }

    public @Nullable
    File choosePosMapsFileToOpen(@Nullable File prevChosenFile) throws HeadlessException {
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

    @UIEffect
    private void jMenuItemSafeAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSafeAbortAllActionPerformed

        jCheckBoxMenuItemContinuousDemo.setSelected(false);
        performSafeAbortAllAction();
    }//GEN-LAST:event_jMenuItemSafeAbortAllActionPerformed

    private void performSafeAbortAllAction() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.performSafeAbortAllAction();
    }

    public XFutureVoid showSafeAbortComplete() {
        final GraphicsConfiguration graphicsConfiguration
                = this.getGraphicsConfiguration();
        final GraphicsDevice gd = (graphicsConfiguration != null) ? graphicsConfiguration.getDevice() : null;
        XFutureVoid immediateAbortAllFuture
                = immediateAbortAll("showSafeAbortComplete");
        XFutureVoid fullAbortAllFuture
                = immediateAbortAllFuture.thenComposeAsyncToVoid(this::fullAbortAll, supervisor.getSupervisorExecutorService());
        return fullAbortAllFuture.thenRun(() -> {
            if (null != supervisor && !supervisor.isResetting() && null != gd) {
                forceShowMessageFullScreen("Safe Abort Complete", 80.0f,
                        SplashScreen.getRobotArmImage(),
                        SplashScreen.getBlueWhiteGreenColorList(), gd);
            }
        });
    }

    private final AtomicBoolean ignoreTitleErrors = new AtomicBoolean(false);

    @UIEffect
    private void jMenuItemImmediateAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortAllActionPerformed
        fullAbortAll();
    }//GEN-LAST:event_jMenuItemImmediateAbortAllActionPerformed

    private XFutureVoid fullAbortAll() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.fullAbortAll();
    }

    private XFutureVoid returnRobot() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.publicReturnRobot();
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

    private @Nullable
    AprsSystem getPosMapInSys() {
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

    private @Nullable
    AprsSystem getPosMapOutSys() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getPosMapOutSys();
    }

    private void setPosMapOutSys(AprsSystem posMapOutSys) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setPosMapOutSys(posMapOutSys);
    }

    @UIEffect
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

    @UIEffect
    private void jMenuItemConnectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectAllActionPerformed
        connectAll();
    }//GEN-LAST:event_jMenuItemConnectAllActionPerformed

    @UIEffect
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

    @UIEffect
    private void jButtonAddLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddLineActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTableSelectedPosMapFile.getModel();
        model.addRow(new Object[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "label" + model.getRowCount()});
    }//GEN-LAST:event_jButtonAddLineActionPerformed

    @UIEffect
    private void jButtonDeleteLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteLineActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            DefaultTableModel model = (DefaultTableModel) jTableSelectedPosMapFile.getModel();
            model.removeRow(row);
        }
    }//GEN-LAST:event_jButtonDeleteLineActionPerformed

    @UIEffect
    private void jButtonSaveSelectedPosMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveSelectedPosMapActionPerformed
        try {
            selectAndSavePositionMappingsFilesFile();
        } catch (IOException ex) {
            log(Level.SEVERE, "", ex);
        }
    }//GEN-LAST:event_jButtonSaveSelectedPosMapActionPerformed

    private @Nullable
    File getLastPosMapParent() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getLastPosMapParent();
    }

    private void selectAndSavePositionMappingsFilesFile() throws IOException, HeadlessException {

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

    @UIEffect
    private void jCheckBoxMenuItemDisableTextPopupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed

    @UIEffect
    private void jMenuItemDbgActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDbgActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDbgActionActionPerformed

//    private static void printStatus(AtomicReference<@Nullable XFutureVoid> ref, PrintStream ps) {
//        if (null != ref) {
//            XFuture<?> xf = ref.get();
//            printStatus(xf, ps);
//        }
//    }
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

    private @Nullable
    Socket colorTextSocket = null;
    private @Nullable
    ColorTextJFrame colorTextJFrame = null;

    @UIEffect
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

    public void close() {
        try {
            closing = true;
            // force github actions after CRCL deploy
            NotificationsJPanel.closeNotifications();
            try {
                final File supervisorEventsCsvFile = Utils.createTempFile("supervisorEventsTable", ".csv");
                System.out.println("Closing " + this);
                System.out.println("Saving events table in " + supervisorEventsCsvFile);
                System.out.println("");
                Utils.saveJTable(supervisorEventsCsvFile, jTableEvents);
            } catch (IOException iOException) {
                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", iOException);
            }

            if (null != runTimeTimer) {
                runTimeTimer.stop();
                runTimeTimer = null;
            }
            if (null != conveyorVisJPanel1) {
                this.conveyorVisJPanel1.disconnect();
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
            System.out.println("addOldEventToTableCount = " + this.addOldEventToTableCount);
            System.out.println("addOldEventToTableResizeCount = " + this.addOldEventToTableResizeCount);
            aprs.simview.Object2DJPanel.shutdownImageIOWriterService();
            Thread.sleep(3000);
            final Map<Thread, StackTraceElement[]> allStackTracesMap = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTracesMap.entrySet()) {
                Thread thread = entry.getKey();
                StackTraceElement[] trace = entry.getValue();
                if (!thread.isDaemon() && thread.isAlive() && thread != Thread.currentThread()) {
                    System.out.println("thread = " + thread);
                    System.out.println("trace = " + XFuture.traceToString(trace));
                }
            }
            CRCLUtils.systemExit(0);
        } catch (InterruptedException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        if (null != supervisor) {
            supervisor.close();
        }
        close();
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (null != supervisor) {
            supervisor.close();
        }
        close();
    }//GEN-LAST:event_formWindowClosing

    @UIEffect
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

    @UIEffect
    private void jCheckBoxMenuItemContinuousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinuousDemoActionPerformed
        interactivStartRunnable(() -> {
            jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
            prepAndFinishOnDispatch(() -> {
                immediateAbortAll("jCheckBoxMenuItemContinuousDemoActionPerformed");
                privateClearEventLog();
                clearAllErrors();
                connectAll();
                jCheckBoxMenuItemContinuousDemoActionPerformed2();
            });
        }, jCheckBoxMenuItemContinuousDemo.getText());
    }//GEN-LAST:event_jCheckBoxMenuItemContinuousDemoActionPerformed

//    private XFutureVoid startSetAllReverseFlag(boolean flag) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.startSetAllReverseFlag(flag);
//
//    }
    @UIEffect
    private void jCheckBoxMenuItemContinuousDemoActionPerformed2() {
        Utils.runOnDispatchThread(this::jCheckBoxMenuItemContinuousDemoActionPerformed2OnDisplay);
    }

    @UIEffect
    private void jCheckBoxMenuItemContinuousDemoActionPerformed2OnDisplay() {
        enableAllRobots()
                .thenRun(() -> {
                    clearContinuousDemoCycle();
                    if (jCheckBoxMenuItemContinuousDemo.isSelected()) {
                        XFutureVoid ContinuousDemoFuture = startContinuousDemo();
                        setMainFuture(ContinuousDemoFuture);
                    }
                });
    }

    @UIEffect
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

    @UIEffect
    private void jCheckBoxMenuItemRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRandomTestActionPerformed
        interactivStartRunnable(() -> {
            prepAndFinishOnDispatch(this::jCheckBoxMenuItemRandomTestActionPerformed2OnDisplay);
        }, jCheckBoxMenuItemRandomTest.getText());
    }//GEN-LAST:event_jCheckBoxMenuItemRandomTestActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemRandomTestActionPerformed2OnDisplay() {
        immediateAbortAll("jCheckBoxMenuItemRandomTestActionPerformed");
        privateClearEventLog();
        clearAllErrors();
        connectAll();
        enableAllRobots()
                .thenRun(() -> {
                    clearContinuousDemoCycle();
                    clearRandomTestCount();
                    if (jCheckBoxMenuItemRandomTest.isSelected()) {
                        lastFutureReturned = startRandomTest();
                        setMainFuture(lastFutureReturned);
                    }
                });
    }

    @SuppressWarnings({"rawtypes", "nullness"})
    public static TableModel defaultPositionMappingsModel() {
        return Supervisor.defaultPositionMappingsModel();
    }

    @UIEffect
    private void jMenuItemStartAllReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllReverseActionPerformed
        interactivStartRunnable(() -> {
            if (null != lastFutureReturned) {
                if (!lastFutureReturned.isDone()) {
                    lastFutureReturned.cancelAll(true);
                }
                lastFutureReturned = null;
            }
            prepAndFinishOnDispatch(() -> {
                immediateAbortAll("jMenuItemStartAllReverseActionPerformed");
                if (null == supervisor) {
                    throw new NullPointerException("supervisor");
                }
                supervisor.performReverseStartAllAction();
            });
        }, jMenuItemStartAllReverse.getText())
                .thenCompose(x -> Utils.runOnDispatchThread(() -> JOptionPane.showMessageDialog(this, "Start all reverse done.")));

    }//GEN-LAST:event_jMenuItemStartAllReverseActionPerformed

    @UIEffect
    private void jMenuItemResetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetAllActionPerformed
        try {
            internalInteractiveResetAll();
        } catch (Exception e) {
            logEvent("Exception occurred: " + e);
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
            NotificationsJPanel.showText("Exception occurred: " + e);
        }
    }//GEN-LAST:event_jMenuItemResetAllActionPerformed

    @UIEffect
    private XFutureVoid internalInteractiveResetAll() {
        if (null == supervisor || supervisor.isKeepDisabled() || jCheckBoxMenuItemKeepDisabled.isSelected()) {
            return XFutureVoid.completedFuture();
        }
        boolean origIgnoreTitleErrs = ignoreTitleErrors.getAndSet(true);
//        boolean reloadSimFiles = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Reload sim files?"));
        resetting = true;
        return resetAll(false)
                .thenComposeAsyncToVoid(x -> {
                    return Utils.composeToVoidOnDispatchThread(() -> {
                        boolean origIgnoreTitleErrs2 = ignoreTitleErrors.getAndSet(true);
                        try {
                            cancelAll(true);
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
                            resetAll(false);
                            return restoreOrigRobotInfo()
                                    .thenComposeAsyncToVoid(() -> {
                                        connectAll();
                                        return enableAllRobots();
                                    }, supervisor.getSupervisorExecutorService())
                                    .alwaysRun(() -> {
                                        resetting = false;
                                        if (!origIgnoreTitleErrs) {
                                            ignoreTitleErrors.set(false);
                                        }
                                    });

                        } catch (Exception e) {
                            logEvent("Exception occurred: " + e);
                            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                            NotificationsJPanel.showText("Exception occurred: " + e);
                            throw e;
                        }
                    });
                }, supervisor.getSupervisorExecutorService());
    }

    private volatile boolean resetting = false;

    /**
     * Reset all systems, clearing errors, resetting states to defaults and
     * optionally reloading simulation files. This may occur in another thread.
     *
     * @param reloadSimFiles whether to reload simulation files
     * @return a future which can be used to determine when the resetAll action
     * is complete.
     */
    private XFutureVoid resetAll(boolean reloadSimFiles) {
        boolean alreadyResetting = resetting;
        resetting = true;
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.resetAll(reloadSimFiles)
                .thenRun(() -> resetting = alreadyResetting);
    }

    private @Nullable
    XFutureVoid getPauseTestFuture() {
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

    @UIEffect
    private void jCheckBoxMenuItemPauseResumeTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed
        interactivStartRunnable(() -> {
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
        }, jCheckBoxMenuItemPauseResumeTest.getText());
    }//GEN-LAST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed

    @UIEffect
    private void jCheckBoxMenuItemPauseResumeTestActionPerformed2OnDisplay() {

        if (null != supervisor) {
            int startingAbortCount = supervisor.getAbortCount();
            enableAllRobots()
                    .thenRun(() -> {
                        clearContinuousDemoCycle();
                        clearRandomTestCount();
                        jCheckBoxMenuItemContinuousDemo.setSelected(false);
                        jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
                        jCheckBoxMenuItemRandomTest.setSelected(false);
                        if (jCheckBoxMenuItemPauseResumeTest.isSelected()) {
                            jCheckBoxMenuItemContinuousDemo.setSelected(true);
                            jCheckBoxMenuItemRandomTest.setSelected(true);
                            startContinuousDemo();
                            continueRandomTest(startingAbortCount);
                            continuePauseTest();
                            resetMainPauseTestFuture();
                        }
                    });
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

    @UIEffect
    private void jCheckBoxMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugActionPerformed
        setDebug(jCheckBoxMenuItemDebug.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItemDebugActionPerformed

    @UIEffect
    private void jMenuItemEnableAndContinueAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemEnableAndContinueAllActionPerformed

        enableAndContinueAll();
    }//GEN-LAST:event_jMenuItemEnableAndContinueAllActionPerformed

    public XFutureVoid enableAndContinueAll() {
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        return prepAndFinishOnDispatch(() -> {
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
            XFutureVoid continueAllXF = continueAllActions();
            lastFutureReturned = continueAllXF;
            if (jCheckBoxMenuItemContinuousDemo.isSelected()) {
                ContinuousDemoFuture
                        = continueAllXF
                                .thenComposeAsyncToVoid("jMenuItemContinueAllActionPerformed.continueAllActions",
                                        x -> enableAndContinueAllActions(), supervisor.getSupervisorExecutorService());
                setMainFuture(ContinuousDemoFuture);
            }
        });
    }

    public XFutureVoid continueAll() {
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        return Utils.composeToVoidOnDispatchThread(() -> {
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
            jCheckBoxMenuItemRandomTest.setSelected(false);
            jCheckBoxMenuItemPause.setSelected(false);
            resume();
            XFutureVoid continueAllXF = continueAllActions();
            lastFutureReturned = continueAllXF;
            if (jCheckBoxMenuItemContinuousDemo.isSelected()) {
                ContinuousDemoFuture
                        = continueAllXF
                                .thenComposeAsyncToVoid("jMenuItemContinueAllActionPerformed.continueAllActions",
                                        x -> continueAllActions(),
                                        supervisor.getSupervisorExecutorService());
                setMainFuture(ContinuousDemoFuture);
            }
            return continueAllXF;
        });
    }

    @UIEffect
    private void jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed
        interactivStartRunnable(() -> {
            if (jCheckBoxMenuItemContinuousDemoRevFirst.isSelected()) {
                startContinuousDemoRevFirst();
            } else {
                immediateAbortAll("jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed");
            }
        }, jCheckBoxMenuItemContinuousDemoRevFirst.getText());
    }//GEN-LAST:event_jCheckBoxMenuItemContinuousDemoRevFirstActionPerformed

    private XFutureVoid startContinuousDemoRevFirst() {
        jCheckBoxMenuItemContinuousDemo.setSelected(false);
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startContinuousDemoRevFirst();
    }

    @UIEffect
    private void jMenuItemScanAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemScanAllActionPerformed
        try {
            interactivStartFutureSupplier(() -> {
                return prepAndFinishOnDispatch(() -> {
                    try {
                        XFutureVoid ret = startScanAll();
                        lastFutureReturned = ret;
                        setMainFuture(lastFutureReturned);
                        return ret;
                    } catch (Exception e) {
                        logEvent("Exception occurred: " + e);
                        Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                        NotificationsJPanel.showText("Exception occurred: " + e);
                        throw new RuntimeException(e);
                    }
                });
            }, jMenuItemScanAll.getText())
                    .thenCompose(x -> Utils.runOnDispatchThread(() -> JOptionPane.showMessageDialog(this, "Scan all done.")));
        } catch (Exception e) {
            logEvent("Exception occurred: " + e);
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
            NotificationsJPanel.showText("Exception occurred: " + e);
        }
    }//GEN-LAST:event_jMenuItemScanAllActionPerformed

    @UIEffect
    private void jCheckBoxUpdateFutureAutomaticallyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxUpdateFutureAutomaticallyActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxUpdateFutureAutomaticallyActionPerformed

    @UIEffect
    private void jMenuItemRandomTestReverseFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRandomTestReverseFirstActionPerformed
        interactivStartRunnable(() -> startRandomTestFirstActionReversed(),
                jMenuItemRandomTestReverseFirst.getText());
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
                                    .thenComposeAsyncToVoid(
                                            x -> {
                                                XFutureVoid innerRet = Utils.supplyOnDispatchThread(
                                                        () -> {
                                                            try {
                                                                clearAllErrors();
                                                                connectAll();
                                                                jCheckBoxMenuItemPause.setSelected(false);
                                                                resume();
                                                                return startRandomTestFirstActionReversed2();
                                                            } catch (Exception e) {
                                                                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                                                                NotificationsJPanel.showText("Exception occurred: " + e);
                                                                XFutureVoid ret = new XFutureVoid("internal startRandomTestFirstActionReversed with exception " + e);
                                                                ret.completeExceptionally(e);
                                                                return ret;
                                                            }
                                                        })
                                                        .thenComposeAsyncToVoid(x3 -> x3, supervisor.getSupervisorExecutorService());
                                                return innerRet;
                                            }, supervisor.getSupervisorExecutorService());
                    return outerRet;
                } catch (Exception e) {
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
                    NotificationsJPanel.showText("Exception occurred: " + e);
                    XFutureVoid ret = new XFutureVoid("internal startRandomTestFirstActionReversed with exception " + e);
                    ret.completeExceptionally(e);
                    return ret;
                }
            });
        } catch (Exception e) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", e);
            NotificationsJPanel.showText("Exception occurred: " + e);
            XFutureVoid ret = new XFutureVoid("startRandomTestFirstActionReversed with exception " + e);
            ret.completeExceptionally(e);
            return ret;
        }
    }

    private XFutureVoid startRandomTestFirstActionReversed2() {
        return Utils.supplyOnDispatchThread(this::startRandomTestFirstActionReversed2OnDisplay)
                .thenComposeAsyncToVoid(x -> x, supervisor.getSupervisorExecutorService());
    }

    private XFutureVoid startRandomTestFirstActionReversed2OnDisplay() {
        return enableAllRobots()
                .thenComposeAsyncToVoid(() -> {
                    clearContinuousDemoCycle();
                    clearRandomTestCount();
                    jCheckBoxMenuItemContDemoReverseFirstOption.setSelected(true);
                    jCheckBoxMenuItemRandomTest.setSelected(true);
                    lastFutureReturned = null;
                    XFutureVoid ret = startRandomTest();
                    setMainFuture(ret);
                    return ret;
                }, supervisor.getSupervisorExecutorService());
    }

    @UIEffect
    private void jCheckBoxShowUnnamedFuturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowUnnamedFuturesActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxShowUnnamedFuturesActionPerformed

    @UIEffect
    private void jCheckBoxShowDoneFuturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowDoneFuturesActionPerformed
        if (jCheckBoxUpdateFutureAutomatically.isSelected()) {
            updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxShowDoneFuturesActionPerformed

    @UIEffect
    private void jButtonFuturesCancelAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFuturesCancelAllActionPerformed
        if (null == futureToDisplaySupplier) {
            return;
        }
        XFuture<?> future = futureToDisplaySupplier.get();
        if (null != future) {
            future.cancelAll(true);
        }
    }//GEN-LAST:event_jButtonFuturesCancelAllActionPerformed

    @UIEffect
    private void jTextFieldEventsMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldEventsMaxActionPerformed
        setEventsDisplayMax(Integer.parseInt(jTextFieldEventsMax.getText().trim()));
    }//GEN-LAST:event_jTextFieldEventsMaxActionPerformed

    @UIEffect
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

    @UIEffect
    private void jCheckBoxMenuItemIndContinuousDemoActionPerformed2OnDisplay() {
        enableAllRobots()
                .thenRun(() -> {
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
                });
    }

//    private Random getRandom() {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.getRandom();
//    }
    private void setRandom(Random random) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.setRandom(random);
    }

    @UIEffect
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

    @UIEffect
    private void jCheckBoxMenuItemIndRandomToggleTestActionPerformed2OnDisplay() {
        enableAllRobots()
                .thenRun(() -> {
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
                });
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

    @UIEffect
    private void jCheckBoxMenuItemFixedRandomTestSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemFixedRandomTestSeedActionPerformed
        if (jCheckBoxMenuItemFixedRandomTestSeed.isSelected()) {
            int randomTestSeed = Integer.parseInt(JOptionPane.showInputDialog("Fixed Seed", getRandomTestSeed()));
            jCheckBoxMenuItemFixedRandomTestSeed.setText("Fixed Random Test Seed (" + randomTestSeed + ") ... ");
            setRandomTestSeed(randomTestSeed);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemFixedRandomTestSeedActionPerformed

    @UIEffect
    private void jCheckBoxFutureLongFormActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxFutureLongFormActionPerformed
        updateCurrentFutureDisplay(jCheckBoxShowDoneFutures.isSelected(), jCheckBoxShowUnnamedFutures.isSelected());
    }//GEN-LAST:event_jCheckBoxFutureLongFormActionPerformed

//    private static String getDirNameOrHome(@Nullable File f) throws IOException {
//        if (f != null) {
//            File parentFile = f.getParentFile();
//            if (null != parentFile) {
//                return parentFile.getCanonicalPath();
//            }
//        }
//        return Utils.getAprsUserHomeDir();
//    }
    @UIEffect
    private void jMenuItemSaveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveAllActionPerformed
        saveAll();
    }//GEN-LAST:event_jMenuItemSaveAllActionPerformed

    private void saveAll() throws IllegalStateException {
        try {
            if (null == supervisor) {
                throw new IllegalStateException("null == supervisor");
            }
            Map<String, MultiFileDialogInputFileInfo> filesMapIn = new HashMap<>();
            filesMapIn.put("Setup", new MultiFileDialogInputFileInfo(supervisor.getSetupFilePathString()));
            filesMapIn.put("PosMap", new MultiFileDialogInputFileInfo(supervisor.getPosMapFilePathString()));
            filesMapIn.put("SimTeach", new MultiFileDialogInputFileInfo(supervisor.getSimTeachFilePathString()));
            filesMapIn.put("TeachProps", new MultiFileDialogInputFileInfo(supervisor.getTeachPropsFilePathString()));
            filesMapIn.put("SharedTools", new MultiFileDialogInputFileInfo(supervisor.getSharedToolsFilePathString()));

            Map<String, String> filesMapOut = MultiFileDialogJPanel.showMultiFileDialog(this, "Save All ...", true, filesMapIn);
            if (null != filesMapOut) {
                String setup = filesMapOut.get("Setup");
                if (null != setup) {
                    saveSetupFile(Utils.file(setup));
                }
                String mapsFile = filesMapOut.get("PosMap");
                if (null != mapsFile) {
                    savePositionMaps(Utils.file(mapsFile));
                }

                String simTeach = filesMapOut.get("SimTeach");
                if (null != simTeach) {
                    saveSimTeach(Utils.file(simTeach));
                }

                String teachProps = filesMapOut.get("TeachProps");
                if (null != teachProps) {
                    saveTeachProps(Utils.file(teachProps));
                }
                String sharedTools = filesMapOut.get("SharedTools");
                if (null != sharedTools) {
                    saveSharedTools(Utils.file(sharedTools));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    @UIEffect
    private void jComboBoxTeachSystemViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxTeachSystemViewActionPerformed
        try {
            List<AprsSystem> aprsSystems = getAprsSystems();
            String sysString = (String) jComboBoxTeachSystemView.getSelectedItem();
            if (null == sysString || sysString.equals("All")) {
                setTeachSystemFilterOnDisplay(null);
            } else {
                int id = Integer.parseInt(sysString.trim().split("[ \t:]+")[0]);
                for (AprsSystem sys : aprsSystems) {
                    if (sys.getMyThreadId() == id) {
                        setTeachSystemFilterOnDisplay(sys);
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", exception);
        }
    }//GEN-LAST:event_jComboBoxTeachSystemViewActionPerformed

    private final static String INIT_CUSTOM_CODE = getCustomCodeInitDefault();

    private static InputStream getResourceStream(String name) throws IOException {
        InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(name);
        if (null == systemResourceAsStream) {
            systemResourceAsStream = new FileInputStream("src/main/resources/" + name);
        }
        return systemResourceAsStream;
    }

    private static String getCustomCodeInitDefault() {
        StringBuilder sb = new StringBuilder();
        try ( InputStream systemResourceAsStream = getResourceStream("custom/Custom.java")) {
            if (null != systemResourceAsStream) {
                try ( BufferedReader br = new BufferedReader(new InputStreamReader(systemResourceAsStream))) {
                    String line = null;
                    while (null != (line = br.readLine())) {
                        sb.append(line);
                        sb.append("\n");
                    }
                } catch (Exception ex) {
                    Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    //            = "package custom;\n"
    //            + "import aprs.supervisor.main.*; \n"
    //            + "import java.util.function.Consumer;\n\n"
    //            + "public class Custom\n\timplements Consumer<AprsSupervisorDisplayJFrame> {\n"
    //            + "\tpublic void accept(AprsSupervisorDisplayJFrame supDisplay) {\n"
    //            + "\t\t// PUT YOUR CODE HERE:\n"
    //            + "\t\tSystem.out.println(\"sys = \"+supDisplay.getSupervisor().getSysByTask(\"Fanuc Cart\"));"
    //            + "\t}\n"
    //            + "}\n";
    private String customCode = INIT_CUSTOM_CODE;

    @UIEffect
    private void jMenuItemRunCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRunCustomActionPerformed
        runCustomCode();
    }//GEN-LAST:event_jMenuItemRunCustomActionPerformed

    @UIEffect
    private void jMenuItemSetMaxCyclesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetMaxCyclesActionPerformed
        String newMaxCycles = JOptionPane.showInputDialog("Maximum cycles for Continuous demo", getMax_cycles());
        if (null != newMaxCycles && newMaxCycles.length() > 0) {
            setMax_cycles(Integer.parseInt(newMaxCycles));
        }
    }//GEN-LAST:event_jMenuItemSetMaxCyclesActionPerformed

    @UIEffect
    private void jMenuItemStartContinuousScanAndRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartContinuousScanAndRunActionPerformed
        interactivStartRunnable(() -> {
            jCheckBoxMenuItemContinuousDemoRevFirst.setSelected(false);
            prepAndFinishOnDispatch(() -> {
                immediateAbortAll("jMenuItemStartContinuousScanAndRunActionPerformed");
                privateClearEventLog();
                clearAllErrors();
                connectAll();
                jMenuItemStartContinuousScanAndRunActionPerformed2OnDisplay();
            });
        }, jMenuItemStartContinuousScanAndRun.getText());
    }//GEN-LAST:event_jMenuItemStartContinuousScanAndRunActionPerformed

    @UIEffect
    private void jMenuItemStartContinuousScanAndRunActionPerformed2OnDisplay() {
        enableAllRobots()
                .thenRun(() -> {
                    clearContinuousDemoCycle();
                    jCheckBoxMenuItemShowSplashMessages.setSelected(false);
                    jCheckBoxMenuItemContinuousDemo.setSelected(true);
                    XFutureVoid future = startContinuousScanAndRun();
                    setMainFuture(future);
                    setContinuousDemoFuture(future);
                });
    }

    private volatile @Nullable XFutureVoid internalInteractiveResetAllFuture = null;
    private volatile @Nullable XFutureVoid interactivStartFuture = null;
    private final static AtomicInteger INTERACTIVE_START_ATOMIC = new AtomicInteger();

    @UIEffect
    private XFutureVoid interactivStartRunnable(Runnable runnable, String actionName) {
        int isn = INTERACTIVE_START_ATOMIC.incrementAndGet();
        final String blockerName = "interactiveStart." + actionName + ",isn=" + isn;
        final Map<String, LockInfo> afterDisableToggleBlockerMap = new TreeMap<>();
        try {
            Supervisor supervisorLocal = this.supervisor;
            if (null == supervisorLocal) {
                throw new NullPointerException("supervisor");
            }
            XFutureVoid prep1Future = prepInteractiveStart(actionName, isn, blockerName);
            final XFutureVoid dispatchRunnableFuture = prep1Future
                    .thenComposeAsyncToVoid("interactivStart(" + actionName + ",isn=" + isn + ")afterLookForParts",
                            () -> dispatchAction(actionName, runnable), supervisor.getSupervisorExecutorService());
            XFutureVoid ret = dispatchRunnableFuture
                    .alwaysComposeAsyncToVoid(() -> supervisorLocal.allowTogglesNoCheck(blockerName),
                            supervisorLocal.getSupervisorExecutorService()
                    )
                    .thenRun(() -> {
                        logEvent("Completed interactiveStart actionName=" + actionName + ",isn=" + isn);
                        if (null != actionName && !actionName.toLowerCase().startsWith("start")) {
                            JOptionPane.showMessageDialog(AprsSupervisorDisplayJFrame.this, actionName + " completed.");
                        }
                    });
            interactivStartFuture = ret;
            return ret;
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "actionName=" + actionName + ",isn=" + isn, exception);
            XFutureVoid ret = new XFutureVoid("interactivStart.exception actionName=" + actionName + ",isn=" + isn);
            ret.completeExceptionally(exception);
            return ret;
        }
    }

    @UIEffect
    private <T> XFutureVoid interactivStartFutureSupplier(
            Supplier<XFuture<Void>> supplier,
            String actionName) {
        int isn = INTERACTIVE_START_ATOMIC.incrementAndGet();
        final String blockerName = "interactiveStart." + actionName + ",isn=" + isn;
        final Map<String, LockInfo> afterDisableToggleBlockerMap = new TreeMap<>();
        try {
            Supervisor supervisorLocal = this.supervisor;
            if (null == supervisorLocal) {
                throw new NullPointerException("supervisor");
            }
            XFutureVoid prep1Future = prepInteractiveStart(actionName, isn, blockerName);
            final XFutureVoid dispatchRunnableFuture = prep1Future
                    .thenComposeAsyncToVoid("interactivStart(" + actionName + ",isn=" + isn + ")afterLookForParts",
                            () -> dispatchFutureSupplierAction(actionName, supplier, Void.class),
                            supervisor.getSupervisorExecutorService());
            XFutureVoid allowToggleFuture = prep1Future
                    .alwaysComposeAsyncToVoid(() -> supervisorLocal.allowTogglesNoCheck(blockerName),
                            supervisorLocal.getSupervisorExecutorService()
                    )
                    .thenRun(() -> logEvent("Completed interactiveStart actionName=" + actionName + ",isn=" + isn));
            XFutureVoid ret = XFutureVoid.allOf(allowToggleFuture, dispatchRunnableFuture);
            interactivStartFuture = ret;
            return ret;
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "actionName=" + actionName + ",isn=" + isn, exception);
            XFutureVoid ret = new XFutureVoid("interactivStart.exception actionName=" + actionName + ",isn=" + isn);
            ret.completeExceptionally(exception);
            return ret;
        }
    }

    private XFutureVoid prepInteractiveStart(String actionName, int isn, final String blockerName) {
        final Supervisor supervisorLocal = requireNonNull(supervisor, "supervisor");
        AprsSystem sysArray[] = supervisorLocal.systems().toArray(new AprsSystem[0]);
        logEvent("Staring interactiveStart." + actionName + ",isn=" + isn);
        supervisorLocal.setResetting(true);
        final XFutureVoid internalInteractiveResetAllFutureLocal = internalInteractiveResetAllFuture;
        if (null != internalInteractiveResetAllFutureLocal) {
            if (!internalInteractiveResetAllFutureLocal.isDone()) {
                System.err.println("Cancelling internalInteractiveResetAllFuture= " + internalInteractiveResetAllFutureLocal);
                internalInteractiveResetAllFutureLocal.cancelAll(false);
            }
            internalInteractiveResetAllFuture = null;
        }
        final XFutureVoid interactivStartFutureLocal = interactivStartFuture;
        if (null != interactivStartFutureLocal) {
            if (!interactivStartFutureLocal.isDone()) {
                System.err.println("Cancelling interactivStartFuture= " + interactivStartFutureLocal);
                interactivStartFutureLocal.cancelAll(false);
            }
            interactivStartFuture = null;
        }

        MultiLineStringJPanel.setIgnoreForceShow(true);
        MultiLineStringJPanel.closeAllPanels();
        final XFutureVoid fullAbortFuture;
        if (SwingUtilities.isEventDispatchThread()) {
            supervisorLocal.setIconImageOnDisplay(IconImages.BASE_IMAGE);
            supervisorLocal.setTitleMessageOnDisplay("starting action ...");
            fullAbortFuture = fullAbortAll();
        } else {
            final XFutureVoid setTitleMessageFuture
                    = Utils.runOnDispatchThread(() -> {
                        supervisorLocal.setIconImageOnDisplay(IconImages.BASE_IMAGE);
                        supervisorLocal.setTitleMessageOnDisplay("starting action ...");
                    });
            fullAbortFuture
                    = setTitleMessageFuture
                            .thenComposeAsyncToVoid(() -> fullAbortAll(),
                                    supervisorLocal.getSupervisorExecutorService());
        }
        XFuture<LockInfo> disallowTogglesFuture
                = fullAbortFuture
                        .alwaysComposeAsyncToOutput("interactiveStart.disableToggles",
                                () -> supervisorLocal.disallowToggles(blockerName, sysArray),
                                supervisorLocal.getSupervisorExecutorService());
        XFutureVoid iiraFuture
                = disallowTogglesFuture
                        .thenComposeAsyncToVoid(
                                "interactivStart(" + actionName + ",isn=" + isn + ")internalInteractiveResetAll",
                                x -> internalInteractiveResetAll(),
                                supervisor.getSupervisorExecutorService());
        internalInteractiveResetAllFuture = iiraFuture;
        final XFutureVoid prep1Future = iiraFuture
                .thenRun(() -> {
                    MultiLineStringJPanel.closeAllPanels();
                    MultiLineStringJPanel.setIgnoreForceShow(false);
                })
                .alwaysRun(() -> supervisorLocal.setResetting(false))
                .thenComposeAsyncToVoid("interactivStart(" + actionName + ",isn=" + isn + ")LookForParts",
                        x -> {
                            List<AprsSystem> aprsSystemsToReEnableLimits = new ArrayList<>();
                            List<AprsSystem> aprsSystems = supervisorLocal.systems();
                            List<XFutureVoid> sysFuturesList = new ArrayList<>();
                            for (int i = 0; i < aprsSystems.size(); i++) {
                                AprsSystem sys = aprsSystems.get(i);
                                boolean limitsEnforced = sys.isEnforceMinMaxLimits();
                                logEvent("sys=" + sys + ", limitsEnforced=" + limitsEnforced);
                                if (!limitsEnforced) {
                                    continue;
                                }
                                final XFutureVoid sysConnectFuture;
                                if (!sys.isConnected()) {
                                    if (supervisorLocal.isRobotEnabled(sys.getRobotName())
                                    || !supervisorLocal.isKeepDisabled()) {
                                        sysConnectFuture = sys.connectRobot();
                                    } else {
                                        continue;
                                    }
                                } else {
                                    sysConnectFuture = XFutureVoid.completedFuture();
                                }
                                XFutureVoid checkPointFuture = sysConnectFuture.thenRun(() -> {
                                    PointType currentPoint = sys.getCurrentPosePoint();
//                                if (null == currentPoint && supervisorLocal.isKeepDisabled()) {
//                                    continue;
//                                }
                                    if (null == currentPoint) {
                                        JOptionPane.showMessageDialog(this, "Can't get current position for " + sys);
                                        throw new RuntimeException("Can't get current position for " + sys);
                                    }
                                    boolean inLimits = sys.checkLimitsNoAlert(CRCLPosemath.toPmCartesian(currentPoint));
                                    if (inLimits) {
                                        return;
                                    }
                                    int confirmRet
                                            = JOptionPane.showConfirmDialog(this, "Disable cartesian limits on " + sys);
                                    if (confirmRet == JOptionPane.YES_OPTION) {
                                        sys.setEnforceMinMaxLimits(false);
                                        sys.updateRobotLimits();
                                        aprsSystemsToReEnableLimits.add(sys);
                                    }
                                });
                                sysFuturesList.add(checkPointFuture);
                            }
                            return XFutureVoid.allOf(sysFuturesList)
                                    .thenComposeAsyncToVoid(() -> lookForPartsAll(), supervisorLocal.getSupervisorExecutorService())
                                    .thenRun(() -> {
                                        for (int i = 0; i < aprsSystemsToReEnableLimits.size(); i++) {
                                            AprsSystem sys = aprsSystemsToReEnableLimits.get(i);
                                            sys.setEnforceMinMaxLimits(true);
                                            sys.updateRobotLimits();
                                        }
                                    });
                        },
                        getSupervisorExecutorService());
        return prep1Future;
    }

    private <T> XFuture<@Nullable T> dispatchFutureSupplierAction(String actionName, Supplier<XFuture<T>> supplier, Class<T> clzz) {
        final UiSupplier<XFuture<T>> function = () -> {
            return supplyActionFuture(actionName, supplier);
        };
        return Utils.composeOnDispatchThread(function);
    }

    private <T> XFuture<@Nullable T> supplyActionFuture(String actionName, Supplier<XFuture<T>> supplier) throws HeadlessException {
        final Supervisor supervisorLocal = requireNonNull(supervisor, "supervisor");
        supervisorLocal.setResetting(true);
        disableRobotTableModelListener();
        if (!supervisorLocal.isKeepDisabled() && !jCheckBoxMenuItemKeepDisabled.isSelected()) {
            for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                jTableRobotsSetValueAt(true, i, 1);
            }
        }
        enableRobotTableModelListener();
        Utils.autoResizeTableColWidths(jTableRobots);
        MultiLineStringJPanel.setIgnoreForceShow(false);
        MultiLineStringJPanel.closeAllPanels();
        supervisorLocal.setResetting(false);
        if (null != actionName && null != supplier) {
            String userCheckMessage = "Confirm continue with \"" + actionName + "\"? " + INTERACTIVE_CHECK_INSTRUCTIONS;
            boolean confirmed = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, userCheckMessage));
            if (confirmed) {
                supervisorLocal.setTitleMessage(actionName);
                this.interactivStartFuture = null;
                this.internalInteractiveResetAllFuture = null;
                return supplier.get();
            } else {
                this.interactivStartFuture = null;
                this.internalInteractiveResetAllFuture = null;
            }
        }
        return XFuture.completedFuture(null);
    }

    private XFutureVoid dispatchAction(String actionName, Runnable runnable) {
        final Supervisor supervisorLocal = requireNonNull(supervisor, "supervisor");
        return Utils.runOnDispatchThread(
                "interactivStart(" + actionName + ")confirmContinue",
                () -> {
                    supervisorLocal.setResetting(true);
                    if (!supervisorLocal.isKeepDisabled() && !jCheckBoxMenuItemKeepDisabled.isSelected()) {
                        disableRobotTableModelListener();
                        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                            jTableRobotsSetValueAt(true, i, 1);
                        }
                        enableRobotTableModelListener();
                    }
                    Utils.autoResizeTableColWidths(jTableRobots);
                    MultiLineStringJPanel.setIgnoreForceShow(false);
                    MultiLineStringJPanel.closeAllPanels();
                    supervisorLocal.setResetting(false);
                    if (null != actionName && null != runnable) {
                        String userCheckMessage = "Confirm continue with \"" + actionName + "\"? " + INTERACTIVE_CHECK_INSTRUCTIONS;
                        boolean confirmed = (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, userCheckMessage));
                        if (confirmed) {
                            supervisorLocal.setTitleMessage(actionName);
                            this.interactivStartFuture = null;
                            this.internalInteractiveResetAllFuture = null;
                            runnable.run();
                        } else {
                            this.interactivStartFuture = null;
                            this.internalInteractiveResetAllFuture = null;
                        }
                    }
                });
    }

    private static final String INTERACTIVE_CHECK_INSTRUCTIONS
            = " \r\n"
            + " All parts in slots. \r\n"
            + " All trays in red rectangle in each live view. \r\n"
            + " Robots at home. \r\n "
            + " Gripper's empty.";

    @UIEffect
    private void jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed
        interactiveStartScanAllThenContinuousDemoRevFirst();
    }//GEN-LAST:event_jMenuItemStartScanAllThenContinuousDemoRevFirstActionPerformed

    @UIEffect
    private XFutureVoid interactiveStartScanAllThenContinuousDemoRevFirst() {
        return interactivStartRunnable(() -> setMainFuture(startScanAllThenContinuousDemoRevFirst()),
                jMenuItemStartScanAllThenContinuousDemoRevFirst.getText());
    }

    public XFutureVoid dispatchStartScanAllThenContinuousDemoRevFirst() {
        if (!Utils.isEventDispatchThread()) {
            return Utils.composeToVoidOnDispatchThread(this::interactiveStartScanAllThenContinuousDemoRevFirst);
        } else {
            return interactiveStartScanAllThenContinuousDemoRevFirst();
        }
    }

    public boolean isRecordLiveImageMovieSelected() {
        return jCheckBoxMenuItemRecordLiveImageMovie.isSelected();
    }

    public void setRecordLiveImageMovieSelected(boolean selected) {
        jCheckBoxMenuItemRecordLiveImageMovie.setSelected(selected);
    }

    @UIEffect
    private void jCheckBoxMenuItemRecordLiveImageMovieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRecordLiveImageMovieActionPerformed
        if (!jCheckBoxMenuItemRecordLiveImageMovie.isSelected()) {
            if (null != supervisor) {
                supervisor.finishEncodingLiveImageMovie();
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemRecordLiveImageMovieActionPerformed

    @UIEffect
    private void jButtonAddSharedToolsRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddSharedToolsRowActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableSharedTools.getModel();
        dtm.addRow(new String[]{"", "", "", "", ""});

    }//GEN-LAST:event_jButtonAddSharedToolsRowActionPerformed

    @UIEffect
    private void jButtonDeleteSharedToolsRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteSharedToolsRowActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) jTableSharedTools.getModel();
        int row = jTableSharedTools.getSelectedRow();
        if (row >= 0 && row < jTableSharedTools.getRowCount()) {
            dtm.removeRow(row);
        }
    }//GEN-LAST:event_jButtonDeleteSharedToolsRowActionPerformed

    @UIEffect
    private void jButtonSyncToolsFromRobotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSyncToolsFromRobotsActionPerformed
        syncToolsFromRobots();
    }//GEN-LAST:event_jButtonSyncToolsFromRobotsActionPerformed

    @UIEffect
    private void jMenuItemOpenAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenAllActionPerformed
        try {
            openAll(supervisor, this, null);
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemOpenAllActionPerformed

    private void jTreeSelectedFutureValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTreeSelectedFutureValueChanged
        TreePath path = evt.getNewLeadSelectionPath();
        if (null != path) {
            Object o = path.getLastPathComponent();

            if (o instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) o;
                Object userObject = tn.getUserObject();
                if (userObject instanceof XFuture) {
                    XFuture<?> xf = (XFuture) userObject;
                    String xffes = xf.forExceptionString();
                    if (!(xf.getCompletedExceptionallyThrowable() instanceof XFuture.PrintedException)) {
                        System.err.println(xffes);
                    }
                    println("xf = " + xf);
                    printStatus(xf, System.out);
                    jTextAreaFutureDetails.setText(xffes);
                }
            }
        }
    }//GEN-LAST:event_jTreeSelectedFutureValueChanged

    private void jMenuItemSetConveyorViewCloneSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetConveyorViewCloneSystemActionPerformed
        Supervisor supervisorLocal = requireNonNull(this.supervisor, "supervisor");
        final String[] taskArray
                = supervisorLocal.getTaskArray();
        if (taskArray.length < 1) {
            JOptionPane.showMessageDialog(this, "No tasks available");
            return;
        }
        int taskIndex
                = (taskArray.length == 1)
                        ? 0
                        : JOptionPane.showOptionDialog(
                                this, // paparentComponent
                                "System View to clone for conveyor", // message
                                "", // title
                                JOptionPane.DEFAULT_OPTION, // optionType
                                JOptionPane.QUESTION_MESSAGE, // messageType
                                null, //icon
                                taskArray, // options);
                                taskArray[0] // initialValue
                        );
        if (taskIndex < 0 || taskIndex >= taskArray.length) {
            return;
        }
        String taskName = taskArray[taskIndex];
        supervisorLocal.setConveyorClonedViewSystemTaskName(taskName);
        setConveyorClonedViewSystemTaskName(taskName);
        final AprsSystem conveyorCloneSys = supervisorLocal.getSysByTask(taskName);
        if (conveyorCloneSys != null) {
            if (JOptionPane.showConfirmDialog(this, "Do conveyor prep for " + taskName) == JOptionPane.YES_OPTION) {
                conveyorTestPrep(conveyorCloneSys);
            }
        }
    }//GEN-LAST:event_jMenuItemSetConveyorViewCloneSystemActionPerformed

    private void jMenuItemSaveSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSetupActionPerformed
        Supervisor supervisorLocal = requireNonNull(this.supervisor, "supervisor");
        try {
            supervisorLocal.saveSetupFile(supervisor.getSetupFile());
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSaveSetupActionPerformed

    private void jMenuItemConveyorTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConveyorTestActionPerformed
        AprsSystem sys = this.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        if (sys.isAlertLimitsCheckBoxSelected()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Disable Alert Limits on " + sys);
            if (confirm == JOptionPane.YES_OPTION) {
                sys.setAlertLimitsCheckBoxSelected(false);
            }
        }

        interactivStartRunnable(() -> conveyorTest(),
                jMenuItemConveyorTest.getText());
    }//GEN-LAST:event_jMenuItemConveyorTestActionPerformed

    private void jMenuItemReloadSimFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReloadSimFilesActionPerformed
        interactivStartRunnable(() -> resetAll(true),
                jMenuItemReloadSimFiles.getText());
    }//GEN-LAST:event_jMenuItemReloadSimFilesActionPerformed

    private void jMenuItemMultiCycleTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMultiCycleTestActionPerformed
        Supervisor supervisorLocal = requireNonNull(this.supervisor, "supervisor");
        int numCycles
                = Integer.parseInt(JOptionPane.showInputDialog(this, "Number of cycles?", 10));
        boolean useConveyor
                = JOptionPane.showConfirmDialog(this, "Use Conveyor") == JOptionPane.YES_OPTION;
        if (numCycles > 0) {
            interactivStartRunnable(() -> supervisorLocal.completeMultiCycleTest(System.currentTimeMillis(), numCycles, useConveyor),
                    jMenuItemMultiCycleTest.getText());
        }
    }//GEN-LAST:event_jMenuItemMultiCycleTestActionPerformed

    private void jMenuItemLookForPartsAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLookForPartsAllActionPerformed
        interactivStartRunnable(() -> lookForPartsAll(),
                jMenuItemLookForPartsAll.getText());
    }//GEN-LAST:event_jMenuItemLookForPartsAllActionPerformed

    private void jCheckBoxMenuItemUseCorrectionModeByDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemUseCorrectionModeByDefaultActionPerformed
        if (null != supervisor) {
            supervisor.setCorrectionMode(jCheckBoxMenuItemUseCorrectionModeByDefault.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxMenuItemUseCorrectionModeByDefaultActionPerformed

    private void jMenuItemRestoreOrigRobotInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRestoreOrigRobotInfoActionPerformed
        restoreOrigRobotInfo();
    }//GEN-LAST:event_jMenuItemRestoreOrigRobotInfoActionPerformed

    private void jMenuItemStartScanAllThenContinuousConveyorDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartScanAllThenContinuousConveyorDemoRevFirstActionPerformed

        AprsSystem sys = this.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        if (sys.isAlertLimitsCheckBoxSelected()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Disable Alert Limits on " + sys);
            if (confirm == JOptionPane.YES_OPTION) {
                sys.setAlertLimitsCheckBoxSelected(false);
            }
        }

        supervisor.setupSystemForConveyorTest(sys);
        interactivStartRunnable(()
                -> setMainFuture(conveyorTestPrep(sys)
                        .thenCompose(x -> startScanAllThenContinuousDemoRevFirst())),
                jMenuItemStartScanAllThenContinuousConveyorDemoRevFirst.getText());
    }//GEN-LAST:event_jMenuItemStartScanAllThenContinuousConveyorDemoRevFirstActionPerformed

    private void jMenuItemLoadEventsLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadEventsLogActionPerformed
        try {
            JFileChooser chooser = new JFileChooser(Utils.getlogFileDir());
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File eventsFile = chooser.getSelectedFile();
                jTabbedPaneMain.setSelectedComponent(jPanelEvents);
                loadEventsFile(eventsFile);
            }
        } catch (IOException | ParseException ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemLoadEventsLogActionPerformed

    private void jButtonGoOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGoOutActionPerformed
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
                        double x = (Double) jTableSelectedPosMapFile.getValueAt(row, 3);
                        double y = (Double) jTableSelectedPosMapFile.getValueAt(row, 4);
                        double z = (Double) jTableSelectedPosMapFile.getValueAt(row, 5);

                        PoseType newPose = CRCLPosemath.pose(CRCLPosemath.point(x, y, z), pose.getXAxis(), pose.getZAxis());
                        interactivStartRunnable(() -> lookForPartsAll(), "Position Maps: Go Out")
                                .thenCompose(() -> posMapOutSys.gotoPose(newPose, "Out"));
                    }
                }
            }
        }
    }//GEN-LAST:event_jButtonGoOutActionPerformed

    private void jButtonGoInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGoInActionPerformed
        int row = jTableSelectedPosMapFile.getSelectedRow();
        if (row >= 0 && row < jTableSelectedPosMapFile.getRowCount()) {
            AprsSystem posMapInSys = getPosMapInSys();
            if (null != posMapInSys) {
                PoseType pose = posMapInSys.getCurrentPose();
                if (null != pose) {
                    double x = (Double) jTableSelectedPosMapFile.getValueAt(row, 0);
                    double y = (Double) jTableSelectedPosMapFile.getValueAt(row, 1);
                    double z = (Double) jTableSelectedPosMapFile.getValueAt(row, 2);
                    Object otherXObject = jTableSelectedPosMapFile.getValueAt(row, 3);
                    Object otherYObject = jTableSelectedPosMapFile.getValueAt(row, 4);
                    Object otherZObject = jTableSelectedPosMapFile.getValueAt(row, 5);
                    if (otherXObject instanceof Double
                            && otherYObject instanceof Double
                            && otherZObject instanceof Double) {
                        double otherx = (double) otherXObject;
                        double othery = (double) otherYObject;
                        double otherz = (double) otherZObject;
                        PoseType newPose = CRCLPosemath.pose(CRCLPosemath.point(x, y, z), pose.getXAxis(), pose.getZAxis());
                        interactivStartRunnable(() -> lookForPartsAll(), "PositionMaps: Go In")
                                .thenCompose(() -> posMapInSys.gotoPose(newPose, "In"));
                    }
                }
            }
        }
    }//GEN-LAST:event_jButtonGoInActionPerformed

    private void jButtonPlotPositionMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlotPositionMapActionPerformed
        supervisor.plotLastPosMapFile();
    }//GEN-LAST:event_jButtonPlotPositionMapActionPerformed

    private void jCheckBoxMenuItemEnableConveyorControlViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemEnableConveyorControlViewActionPerformed
        final boolean selected = jCheckBoxMenuItemEnableConveyorControlView.isSelected();
        enableConveyor(selected);
        supervisor.setEnableConveyor(selected);
    }//GEN-LAST:event_jCheckBoxMenuItemEnableConveyorControlViewActionPerformed

    private void jMenuItemContinueAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueAllActionPerformed
        continueAll()
                .thenCompose(x -> Utils.runOnDispatchThread(() -> JOptionPane.showMessageDialog(this, "Continue all done.")));
    }//GEN-LAST:event_jMenuItemContinueAllActionPerformed

    private void jMenuItemReturnRobotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemReturnRobotActionPerformed
        returnRobot()
                .thenCompose(x -> Utils.runOnDispatchThread(() -> JOptionPane.showMessageDialog(this, "Return robot done.")));
    }//GEN-LAST:event_jMenuItemReturnRobotActionPerformed

    private void jCheckBoxMenuItemKeepDisabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemKeepDisabledActionPerformed
        final boolean selected = jCheckBoxMenuItemKeepDisabled.isSelected();
        keepDisabled(selected);
        supervisor.setKeepDisabled(selected);
    }//GEN-LAST:event_jCheckBoxMenuItemKeepDisabledActionPerformed

    private void jCheckBoxMenuItemSkipDisabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSkipDisabledActionPerformed
        final boolean selected = jCheckBoxMenuItemSkipDisabled.isSelected();
        skipDisabled(selected);
        supervisor.setSkipDisabled(selected);
    }//GEN-LAST:event_jCheckBoxMenuItemSkipDisabledActionPerformed

    private void jCheckBoxMenuItemBlockTransfersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemBlockTransfersActionPerformed
        final boolean selected = jCheckBoxMenuItemBlockTransfers.isSelected();
        blockTransfers(selected);
        supervisor.setBlockRobotTransfers(selected);
    }//GEN-LAST:event_jCheckBoxMenuItemBlockTransfersActionPerformed

    private void jCheckBoxMenuItemSingleStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemSingleStepActionPerformed
        supervisor.setSingleStepping(jCheckBoxMenuItemSingleStep.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItemSingleStepActionPerformed

    private void jMenuItemStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStepActionPerformed
//        supervisor.advanceSingleStep();
    }//GEN-LAST:event_jMenuItemStepActionPerformed

    private volatile AprsRemoteConsoleServerSocket aprsRemoteConsoleServerSocket = null;
    private volatile Thread aprsRemoteConsoleServerSocketThread = null;

    private void jCheckBoxMenuItemEnableRemoteConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemEnableRemoteConsoleActionPerformed
        try {
            if (jCheckBoxMenuItemEnableRemoteConsole.isSelected()) {
                String portString = JOptionPane.showInputDialog(this, "Port for console service?", 7000);
                int port = Integer.parseInt(portString);
                Map<String, Scriptable<?>> scriptablesMap = new TreeMap<>();
                scriptablesMap.put("display", scriptableOf(AprsSupervisorDisplayJFrame.class, this));
                scriptablesMap.put("sup", scriptableOf(Supervisor.class, this.supervisor));
                scriptablesMap.put("CRCLPosemath", scriptableOfStatic(CRCLPosemath.class));
                scriptablesMap.put("Utils", scriptableOfStatic(Utils.class));
                aprsRemoteConsoleServerSocket = new AprsRemoteConsoleServerSocket(port, scriptablesMap);
                aprsRemoteConsoleServerSocketThread = new Thread(aprsRemoteConsoleServerSocket, "AprsRemoteConsole:" + port);
                aprsRemoteConsoleServerSocketThread.start();
            } else {
                closeAprsRemoteConsoleService();
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
            JOptionPane.showMessageDialog(this, "Excetption occurred:" + ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemEnableRemoteConsoleActionPerformed

    private void jMenuItemSpecialTestFlipFanucPartWithMotomanHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSpecialTestFlipFanucPartWithMotomanHelpActionPerformed
        interactivStartRunnable(() -> setMainFuture(startFlipFM()),
                jMenuItemSpecialTestFlipFanucPartWithMotomanHelp.getText());
    }//GEN-LAST:event_jMenuItemSpecialTestFlipFanucPartWithMotomanHelpActionPerformed

    private void jMenuItemSpecialTestFlipMotomanPartWithFanucHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSpecialTestFlipMotomanPartWithFanucHelpActionPerformed
        interactivStartRunnable(() -> setMainFuture(startFlipMF()),
                jMenuItemSpecialTestFlipMotomanPartWithFanucHelp.getText());
    }//GEN-LAST:event_jMenuItemSpecialTestFlipMotomanPartWithFanucHelpActionPerformed

    private void jMenuItemSetGlobalSpeedOverrideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetGlobalSpeedOverrideActionPerformed
        try {
            String overrideString = JOptionPane.showInputDialog("New Global CRCL Server Socket Speed Override?", 1.0);
            double override = Double.parseDouble(overrideString);
            CRCLServerSocket.setGlobalSpeedOverride(override);
        } catch (Exception ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
            JOptionPane.showMessageDialog(this, "Excetption occurred:" + ex);
        }

    }//GEN-LAST:event_jMenuItemSetGlobalSpeedOverrideActionPerformed

    public void closeAprsRemoteConsoleService() {
        try {
            AprsRemoteConsoleServerSocket ss = this.aprsRemoteConsoleServerSocket;
            Thread thread = this.aprsRemoteConsoleServerSocketThread;
            this.aprsRemoteConsoleServerSocket = null;
            this.aprsRemoteConsoleServerSocketThread = null;
            if (null != ss) {
                ss.close();
            }
            if (null != thread) {
                thread.interrupt();
            }
        } catch (Exception ex) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", ex);
            JOptionPane.showMessageDialog(this, "Excetption occurred:" + ex);
        }
    }

    public void keepDisabled(final boolean selected) {
        if (jCheckBoxMenuItemKeepDisabled.isSelected() != selected) {
            jCheckBoxMenuItemKeepDisabled.setSelected(selected);
        }
    }

    public void skipDisabled(final boolean selected) {
        if (jCheckBoxMenuItemSkipDisabled.isSelected() != selected) {
            jCheckBoxMenuItemSkipDisabled.setSelected(selected);
        }
    }

    public void blockTransfers(final boolean selected) {
        if (jCheckBoxMenuItemBlockTransfers.isSelected() != selected) {
            jCheckBoxMenuItemBlockTransfers.setSelected(selected);
        }
    }

    public void enableConveyor(final boolean selected) {
        if (selected) {
            if (null == conveyorVisJPanel1) {
                conveyorVisJPanel1 = new ConveyorVisJPanel();
                for (int i = 0; i < jTabbedPaneMain.getComponentCount(); i++) {
                    String titleI = jTabbedPaneMain.getTitleAt(i);
                    if (titleI.startsWith("Conveyor")) {
                        final Component compI = jTabbedPaneMain.getComponentAt(i);
                        if (compI instanceof ConveyorVisJPanel) {
                            ConveyorVisJPanel visJPanelI = (ConveyorVisJPanel) compI;
                            jTabbedPaneMain.remove(visJPanelI);
                            visJPanelI.disconnect();
                            break;
                        }
                    }
                }
                jTabbedPaneMain.addTab("Conveyor", conveyorVisJPanel1);
                if (null != supervisor) {
                    String convTaskName = supervisor.getConveyorClonedViewSystemTaskName();
                    if (null != convTaskName && convTaskName.length() > 0) {
                        setConveyorClonedViewSystemTaskName(convTaskName);
                    }
                }
            }
        } else {
            if (null != conveyorVisJPanel1) {
                jTabbedPaneMain.remove(conveyorVisJPanel1);
                conveyorVisJPanel1.disconnect();
                conveyorVisJPanel1 = null;
            }
        }
    }

    private String getRecordString(CSVRecord record, Map<String, Integer> headerMap, String header) {
        Integer index = headerMap.get(header);
        if (index == null || index < 0 || index > record.size()) {
            return null;
        }
        return record.get(index);
    }

    private void loadEventsFile(File eventsFile) throws IOException, ParseException {
        jTextFieldEventsLogFile.setText(eventsFile.getCanonicalPath());
        jTextFieldEventsMax.setText("-1");
        boolean scrollEventsOrig = jCheckBoxScrollEvents.isSelected();
        jCheckBoxScrollEvents.setSelected(false);
        setEventsDisplayMax(-1);
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        try ( CSVParser parser = new CSVParser(new FileReader(eventsFile), CSVFormat.TDF.withAllowMissingColumnNames().withFirstRecordAsHeader())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            for (CSVRecord record : parser) {
                String timeString = getRecordString(record, headerMap, "timeString");
                String blockersString = getRecordString(record, headerMap, "blockers");
                String eccString = getRecordString(record, headerMap, "ecc");
                String cdcString = getRecordString(record, headerMap, "cdc");
                String errsString = getRecordString(record, headerMap, "errs");
                String s = getRecordString(record, headerMap, "s");
                String threadname = getRecordString(record, headerMap, "threadname");
                String traceString = getRecordString(record, headerMap, "trace");
                long time = parseTimeString(timeString);
                if (time < minTime) {
                    minTime = time;
                }
                if (time > maxTime) {
                    maxTime = time;
                }
                int blockerSize = Integer.parseInt(blockersString);
                int ecc = Integer.parseInt(eccString);
                int cdc = Integer.parseInt(cdcString);
                int errs = Integer.parseInt(errsString);
                addOldEventToTable(time, blockerSize, ecc, cdc, errs, s, threadname, traceString);
            }
        }
        long runTimeMillis = maxTime - minTime;
        String s = runTimeToString(runTimeMillis);
        jTextFieldRunningTime.setText(s);
        jCheckBoxScrollEvents.setSelected(scrollEventsOrig);
        Utils.autoResizeTableColWidths(jTableEvents);
    }

    XFutureVoid setCheckBoxMenuItemUseCorrectionModeByDefaultSelected(boolean selected) {
        return Utils.runOnDispatchThread(() -> {
            if (jCheckBoxMenuItemUseCorrectionModeByDefault.isSelected() != selected) {
                jCheckBoxMenuItemUseCorrectionModeByDefault.setSelected(selected);
            }
        });
    }

    private XFutureVoid conveyorTest() {
        final Supervisor supervisorLocal = requireNonNull(supervisor, "supervisor");
        AprsSystem sys = this.getConveyorVisClonedSystem();
        if (null == sys) {
            throw new NullPointerException("displayJFrame.getConveyorVisClonedSystem()");
        }
        if (sys.isAlertLimitsCheckBoxSelected()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Disable Alert Limits on " + sys);
            if (confirm == JOptionPane.YES_OPTION) {
                sys.setAlertLimitsCheckBoxSelected(false);
            } else {
                XFutureVoid xfv = new XFutureVoid("conveyortTest.confirm=" + confirm);
                xfv.cancelAll(false);
                return xfv;
            }
        }

        Boolean reverseConvTest = (Boolean) JOptionPane.showInputDialog(this,
                "Reverse Conveyor Test?", "",
                JOptionPane.QUESTION_MESSAGE, null,
                new Boolean[]{Boolean.FALSE, Boolean.TRUE},
                Boolean.FALSE);
        Boolean repeating = (Boolean) JOptionPane.showInputDialog(this,
                "Repeating?", "",
                JOptionPane.QUESTION_MESSAGE, null,
                new Boolean[]{Boolean.FALSE, Boolean.TRUE},
                Boolean.FALSE);

        XFutureVoid conveyorTestPrep = conveyorTestPrep(sys);
        ExecutorService es = supervisorLocal.getSupervisorExecutorService();
        if (repeating) {
            String repeatCountText
                    = JOptionPane.showInputDialog(this,
                            "Repeating Count?",
                            "10");
            int repeatCount = Integer.parseInt(repeatCountText);

            if (reverseConvTest) {
                return conveyorTestPrep
                        .thenComposeAsyncToVoid(() -> supervisorLocal.reverseRepeatingConveyorTest(repeatCount), es);
            } else {
                return conveyorTestPrep
                        .thenComposeAsyncToVoid(() -> supervisorLocal.repeatingConveyorTest(repeatCount), es);
            }
        } else {
            if (reverseConvTest) {
                return conveyorTestPrep
                        .thenComposeAsyncToVoid(supervisorLocal::reverseConveyorTest, es);
            } else {
                return conveyorTestPrep
                        .thenComposeAsyncToVoid(supervisorLocal::conveyorTest, es);
            }
        }

    }

    public XFutureVoid conveyorTestPrep(AprsSystem sys) throws RuntimeException {
        List<XFutureVoid> futuresList = new ArrayList<>();
        if (sys.isAlertLimitsCheckBoxSelected()) {
            if (JOptionPane.showConfirmDialog(this, "Disable alert limits for " + sys) == JOptionPane.YES_OPTION) {
                sys.setAlertLimitsCheckBoxSelected(false);
            }
        }
        if (sys.isObjectViewSimulated()) {
            try {
                JFileChooser chooser = new JFileChooser();
                File parentFile = supervisor.getPropertiesFile().getParentFile();
                String oldPath = supervisor.getConveyorTestObjectViewSimulatedFilePath();
                String parentPath = parentFile.getCanonicalPath();
                if (null != oldPath && oldPath.length() > 0) {
                    File oldFile = Utils.file(parentPath + File.separator + oldPath);
                    if (oldFile.exists()) {
                        chooser.setCurrentDirectory(oldFile.getParentFile());
                        chooser.setSelectedFile(oldFile);
                    } else {
                        chooser.setCurrentDirectory(parentFile);
                    }
                } else {
                    chooser.setCurrentDirectory(parentFile);
                }
                if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {

                    XFutureVoid loadSimFileFuture
                            = sys.loadObjectViewSimulatedFile(chooser.getSelectedFile());
                    futuresList.add(loadSimFileFuture);
                    String path = chooser.getSelectedFile().getCanonicalPath();

                    if (path.startsWith(parentPath)) {
                        path = path.substring(parentPath.length());
                        if (path.startsWith("/") || path.startsWith("\\")) {
                            path = path.substring(1);
                        }
                    }
                    supervisor.setConveyorTestObjectViewSimulatedFilePath(path);
                }
            } catch (IOException ex) {
                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        } else {
            if (null != conveyorVisJPanel1) {
                conveyorVisJPanel1.connectMasterOnDisplay();
            }
        }
        XFutureVoid conveyorTestPrep = XFutureVoid.allOf(futuresList);
        return conveyorTestPrep;
    }

    private static final String titleStart = "Multi Aprs Supervisor";

    public @Nullable
    AprsSystem getConveyorVisClonedSystem() {
        if (null == conveyorVisJPanel1) {
            return null;
        }
        return conveyorVisJPanel1.getClonedSystem();
    }

    public double conveyorPos() {
        if (null == conveyorVisJPanel1) {
            return 0;
        }
        return conveyorVisJPanel1.getEstimatedPosition();
    }

    public XFutureVoid conveyorVisNextTray() {
        if (null == conveyorVisJPanel1) {
            throw new NullPointerException("conveyorVisJPanel1");
        }
        try {
            return conveyorVisJPanel1.nextTray();
        } catch (Exception e) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, e);
            fullAbortAll();
            showErrorSplash(e.getMessage());
            if (e instanceof RuntimeException) {
                throw e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public XFutureVoid conveyorVisPrevTray() {
        if (null == conveyorVisJPanel1) {
            throw new NullPointerException("conveyorVisJPanel1");
        }
        try {
            return conveyorVisJPanel1.prevTray();
        } catch (Exception e) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, null, e);
            showErrorSplash(e.getMessage());
            if (e instanceof RuntimeException) {
                throw e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public void setConveyorClonedViewSystemTaskName(String taskName) {
        if (null != conveyorVisJPanel1) {
            AprsSystem sys = supervisor.getSysByTaskOrThrow(taskName);
            conveyorVisJPanel1.setClonedSystem(sys);
        }
    }

    public XFutureVoid loadProperties(Properties props) {
        return Utils.runOnDispatchThread(() -> {
            loadPropertiesDirect(props);
        });
    }

    public void loadPropertiesDirect(Properties props) throws NumberFormatException {
        setCheckboxFromProperty(props, APRSSUPERVISORDISPLAYDEBUG, jCheckBoxMenuItemDebug);
        setDebug(jCheckBoxMenuItemDebug.isSelected());
        setCheckboxFromProperty(props, APRSSUPERVISORDISPLAYRECORD_LIVE_IMAGE_MOVIE, jCheckBoxMenuItemRecordLiveImageMovie);
        setCheckboxFromProperty(props, APRSSUPERVISORDISPLAYSHOW_SPLASH_MESSAGES, jCheckBoxMenuItemShowSplashMessages);
        setCheckboxFromProperty(props, APRSSUPERVISORDISPLAYUSE_TEACH_CAMERA, jCheckBoxMenuItemUseTeachCamera);
        if (null != conveyorVisJPanel1) {
            Map<String, String> convMap = new TreeMap<>();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(APRSSUPEVISORDISPLAYCONVEYOR)) {
                    convMap.put(key.substring(APRSSUPEVISORDISPLAYCONVEYOR.length()), props.getProperty(key));
                }
            }
            if (!convMap.isEmpty()) {
                conveyorVisJPanel1.mapToProperties(convMap);
            }
        }
    }

    private void setCheckboxFromProperty(Properties props, String propName, JCheckBoxMenuItem checkBoxMenuItem) {
        String propValue = props.getProperty(propName);
        if (null != propValue && propValue.length() > 0) {
            checkBoxMenuItem.setSelected(Boolean.valueOf(propValue));
        }
    }

    public Map<String, String> getPropertiesMap() {
        Map<String, String> map = new TreeMap<>();
        mapPutCheckBox(map, APRSSUPERVISORDISPLAYDEBUG, jCheckBoxMenuItemDebug);
        mapPutCheckBox(map, APRSSUPERVISORDISPLAYRECORD_LIVE_IMAGE_MOVIE, jCheckBoxMenuItemRecordLiveImageMovie);
        mapPutCheckBox(map, APRSSUPERVISORDISPLAYSHOW_SPLASH_MESSAGES, jCheckBoxMenuItemShowSplashMessages);
        mapPutCheckBox(map, APRSSUPERVISORDISPLAYUSE_TEACH_CAMERA, jCheckBoxMenuItemUseTeachCamera);
        if (null != conveyorVisJPanel1) {
            Map<String, String> convMap = conveyorVisJPanel1.propertiesToMap();
            for (Entry<String, String> entry : convMap.entrySet()) {
                map.put(APRSSUPEVISORDISPLAYCONVEYOR + entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    private void mapPutCheckBox(Map<String, String> map, String propName, JCheckBoxMenuItem checkBoxMenuItem) {
        map.put(propName, Boolean.toString(checkBoxMenuItem.isSelected()));
    }

    private static final String APRSSUPERVISORDISPLAYDEBUG = "aprs.supervisor.display.debug";
    private static final String APRSSUPERVISORDISPLAYRECORD_LIVE_IMAGE_MOVIE = "aprs.supervisor.display.recordLiveImageMovie";
    private static final String APRSSUPERVISORDISPLAYSHOW_SPLASH_MESSAGES = "aprs.supervisor.display.showSplashMessages";
    private static final String APRSSUPERVISORDISPLAYUSE_TEACH_CAMERA = "aprs.supervisor.display.useTeachCamera";
    private static final String APRSSUPEVISORDISPLAYCONVEYOR = "aprs.supevisor.display.conveyor.";

    @UIEffect
    public static XFuture<Supervisor> openAll(@Nullable Supervisor supervisor, Frame owner, @Nullable String dirName) throws IOException {
        return Supervisor.openAll(supervisor, owner, dirName);
    }

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
            File customDir = Paths.get(Utils.getAprsUserHomeDir(), ".aprs", "custom").toFile();
            customDir.delete();
            customDir.mkdirs();
            File tmpFile = Utils.file(customDir, "Custom.java");
            println("tmpFile = " + tmpFile.getCanonicalPath());
            File[] files1 = {tmpFile};

            Files.write(tmpFile.toPath(), customCode.getBytes());
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (null != compiler) {
                ClassLoader cl = ClassLoader.getSystemClassLoader();

                final URL[] origUrls;
                if (cl instanceof URLClassLoader) {
                    origUrls = ((URLClassLoader) cl).getURLs();

                } else {
                    origUrls = new URL[]{
                        AprsSupervisorDisplayJFrame.class.getProtectionDomain().getCodeSource().getLocation(),
                        crcl.utils.XFuture.class.getProtectionDomain().getCodeSource().getLocation()
                    };
                }
                String classPath = Arrays.stream(origUrls)
                        .map(Objects::toString)
                        .map(s -> s.startsWith("file:") ? s.substring(4) : s)
                        .collect(Collectors.joining(File.pathSeparator));
                println("classPath = " + classPath);
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

                Iterable<? extends JavaFileObject> compilationUnits1
                        = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files1));
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
                compiler.getTask(null, fileManager, diagnostics, Arrays.asList("-cp", classPath), null, compilationUnits1).call();
                StringBuilder errBuilder = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    String err = String.format("%s:%d %s %n",
                            diagnostic.getSource().toUri(),
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(Locale.US));
                    println("err = " + err);
                    errBuilder.append(err);
                }
                String fullErr = errBuilder.toString();
                println("fullErr = " + fullErr);
                if (fullErr.length() > 0) {
                    NotificationsJPanel.showText(fullErr);
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
                println("urls = " + Arrays.toString(urls));
                Class<?> clss;
                try ( URLClassLoader loader = new URLClassLoader(urls)) {
                    clss = loader.loadClass("custom.Custom");
                }
                @SuppressWarnings("deprecation")
                Object obj = clss.newInstance();
                Method acceptMethod = clss.getMethod("accept", AprsSupervisorDisplayJFrame.class);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream origOut = System.out;

                try ( PrintStream ps = new PrintStream(baos)) {
                    System.setOut(ps);
                    acceptMethod.invoke(obj, this);
                    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    System.setOut(origOut);
                    println("content = " + content);
                    if (content.length() > 0) {
                        NotificationsJPanel.showText(content);
                    }
                } finally {
                    System.setOut(origOut);
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.SEVERE, "", exception);
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            NotificationsJPanel.showText(trace);
            if (!customCode.contains("class Custom")) {
                customCode = INIT_CUSTOM_CODE;
            }
        }
    }

    @UIEffect
    private void setTeachSystemFilterOnDisplay(@Nullable AprsSystem sys) {
        if (null == sys) {
            object2DOuterJPanel1.setForceOutputFlag(false);
            object2DOuterJPanel1.setShowOutputItemsOnDisplay(false);
            object2DOuterJPanel1.setOutputItems(object2DOuterJPanel1.getItems());
        } else {
            object2DOuterJPanel1.setForceOutputFlag(true);
            object2DOuterJPanel1.setSimulated(true);
            object2DOuterJPanel1.setShowOutputItemsOnDisplay(true);
            object2DOuterJPanel1.setOutputItems(filterForSystem(sys, object2DOuterJPanel1.getItems()));
        }
    }

//    @Nullable
//    private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
//        return items.stream()
//                .filter(x -> x.getType().equals("P"))
//                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
//                .orElse(null);
//    }
    private List<PhysicalItem> filterForSystem(AprsSystem sys, List<PhysicalItem> listIn) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.filterForSystem(sys, listIn);
    }

    private XFutureVoid lookForPartsAll() {
        if (jCheckBoxMenuItemKeepDisabled.isSelected() != supervisor.isKeepDisabled()) {
            supervisor.setKeepDisabled(jCheckBoxMenuItemKeepDisabled.isSelected());
        }
        return supervisor.lookForPartsAll();
    }

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
        return supervisor.startScanAllThenContinuousDemoRevFirstOnSupervisorService();
    }

    private XFuture<?> startFlipMF() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startFlipMFOnSupervisorService(
                "part_black_gear_in_pt_1" /* partToFlip */,
                (PointType) null // "empty_slot_for_large_gear_in_large_gear_vessel_1" /* finalEmptySlot */
                );
    }

    private XFuture<?> startFlipFM() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startFlipFMOnSupervisorService(
                "part_black_gear_in_pt_1" /* partToFlip */,
                (PointType) null // "empty_slot_for_large_gear_in_large_gear_vessel_1" /* finalEmptySlot */
                );
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
    private XFutureVoid startScanAll() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.startScanAll()
                .thenComposeAsyncToVoid(
                        supervisor::updateTasksTableOnSupervisorService,
                        supervisor.getSupervisorExecutorService());
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
        for (JCheckBox chkbox : robotsEnableCelRendererComponentList) {
            chkbox.setEnabled(enabled);
        }
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

    public JTable getPositionMappingsFilesTable() {
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

    private XFutureVoid continueRandomTest(int startingAbortCount) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.continueRandomTest(startingAbortCount);
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

    public XFutureVoid setContinuousDemoCycle(int c) {
        println("incrementContinuousDemoCycle : " + c);
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

//    /**
//     * Start actions in reverse mode where kit trays will be emptied rather than
//     * filled.
//     *
//     * @return future that can be used to attach additional actions after this
//     * is complete
//     */
//    private XFutureVoid startReverseActions(int startingAbortCount) {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.startReverseActions(null, startingAbortCount);
//    }
    private void savePosFile(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.savePosFile(f);
    }

    @UIEffect
    private void clearPosTableOnDisplay() {
        if (null == robotTaskMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }
        DefaultTableModel tm = (DefaultTableModel) jTablePositionMappings.getModel();
        tm.setRowCount(0);
        tm.setColumnCount(0);
        tm.addColumn("System");
        for (String name : robotTaskMap.keySet()) {
            tm.addColumn(name);
        }
        for (String name : robotTaskMap.keySet()) {
            Object data[] = new Object[robotTaskMap.size()];
            data[0] = name;
            tm.addRow(data);
        }
        Utils.autoResizeTableColWidthsOnDisplay(jTablePositionMappings);
        Utils.autoResizeTableRowHeightsOnDisplay(jTablePositionMappings);
        if (null != posTablePopupMenu) {
            posTablePopupMenu.setVisible(false);
        }
    }

    private @Nullable
    JPopupMenu posTablePopupMenu = null;

    private void showPosTablePopup(Point pt) {
        JPopupMenu menu = posTablePopupMenu;
        if (menu == null) {
            menu = new JPopupMenu();
            JMenuItem mi = new JMenuItem("Clear");
            mi.addActionListener(l -> clearPosTableOnDisplay());
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
    private XFutureVoid enableAllRobots() {
        if (null == robotTaskMap) {
            throw new IllegalStateException("null == robotEnableMap");
        }
        if (null != supervisor) {
            return supervisor.enableAllRobotsOnSupervisorService()
                    .thenComposeAsyncToVoid(this::completeEnableAllRobots, supervisor.getSupervisorExecutorService());
        }
        return completeEnableAllRobots();
    }

    private XFutureVoid completeEnableAllRobots() {
        return Utils.runOnDispatchThread(() -> {
            try {
                initColorTextSocket();
            } catch (IOException ex) {
                log(Level.SEVERE, "", ex);
            }
            Utils.autoResizeTableColWidths(jTableRobots);
        });
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

    private XFutureVoid enableAndContinueAllActions() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.enableAndContinueAllActions();
    }

    private XFutureVoid continueAllActions() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.continueAllActions();
    }

//    private <T> XFuture<T> checkOkElse(Boolean ok, Supplier<XFuture<T>> okSupplier, Supplier<XFuture<T>> notOkSupplier) {
//        if (ok) {
//            return okSupplier.get();
//        } else {
//            return notOkSupplier.get();
//        }
//    }
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
    private XFutureVoid immediateAbortAll(String comment) {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.immediateAbortAll(comment);
    }

    private XFutureVoid immediateAbortAll(String comment, boolean skipLog) {
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

    private XFutureVoid restoreOrigRobotInfo() {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.restoreOrigRobotInfo();
    }

    /**
     * Connect to all robots.
     */
    private XFutureVoid connectAll() {
        if (null == supervisor) {
            JOptionPane.showMessageDialog(this, "null == supervisor");
            throw new IllegalStateException("null == supervisor");
        }
        try {
            return supervisor.connectAll();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            if (ex instanceof RuntimeException) {
                throw ((RuntimeException) ex);
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Get the value of setupFile
     *
     * @return the value of setupFile
     */
    private @Nullable
    File getSetupFile() throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.getSetupFile();
    }

    public void setSaveSetupEnabled(boolean enabled) {
        this.jMenuItemSaveSetup.setEnabled(enabled);
    }

    public XFutureVoid setTitleMessage(String message, @Nullable File currentSetupFile) {
        return Utils.runOnDispatchThread(() -> setTitleMessageOnDisplay(message, currentSetupFile));
    }

    @UIEffect
    public void setTitleMessageOnDisplay(String message, @Nullable File currentSetupFile) {
        assert SwingUtilities.isEventDispatchThread();
        if (null != currentSetupFile) {
            String path;
            try {
                path = currentSetupFile.getCanonicalPath();
                if (path.length() > 40) {
                    path = currentSetupFile.getParentFile().getCanonicalPath().substring(0, 30) + "..." + File.separator + "..." + File.separator + currentSetupFile.getName();
                }
            } catch (Exception exception) {
                Logger.getLogger(AprsSupervisorDisplayJFrame.class.getName()).log(Level.WARNING, null, exception);
                path = currentSetupFile.getName();
            }
            final String finalPath = path;
            setTitle("Multi Aprs Supervisor : " + message + " :  setupFile=" + finalPath);
        } else {
            setTitle("Multi Aprs Supervisor : : " + message);
        }
    }

//    /**
//     * Set the value of setupFile
//     *
//     * @param f new value of setupFile
//     * @throws java.io.IOException can not save last setup file
//     */
//    private void setSetupFile(File f) throws IOException {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.setSetupFile(f);
//    }
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
        supervisor.saveSetupFile(f);
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
        supervisor.savePositionMaps(f);
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
        supervisor.loadPositionMappingsFilesFile(f);
    }

    private static File resolveFile(String fname, @Nullable File dir) throws IOException {
        File fi = Utils.file(fname);
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

    private void saveSimTeach(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.saveSimTeach(f);
    }

//    private void loadSimTeach(File f) throws IOException {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.loadSimTeach(f);
//    }
//    private void saveLastPosMapFile(File f) throws IOException {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.saveLastPositionMappingsFilesFile(f);
//    }
    private XFutureVoid saveTeachProps(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.saveTeachProps(f);
    }

//    private XFutureVoid loadTeachProps(File f) throws IOException {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        return supervisor.loadTeachProps(f);
//    }
    private void saveSharedTools(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        supervisor.saveSharedTools(f);
    }

//    private void loadSharedTools(File f) throws IOException {
//        if (null == supervisor) {
//            throw new IllegalStateException("null == supervisor");
//        }
//        supervisor.loadSharedTools(f);
//    }
    /**
     * Load the given setup file.
     *
     * @param f setup file to load
     * @throws IOException file could not be read
     */
    private XFutureVoid loadSetupFile(File f) throws IOException {
        if (null == supervisor) {
            throw new IllegalStateException("null == supervisor");
        }
        return supervisor.loadSetupFile(f);
    }

    private String lastUpdateTaskTableTaskNames @Nullable []  = null;

    private final ConcurrentHashMap<Integer, String> titleErrorMap = new ConcurrentHashMap<>();

    public void completeUpdateTasksTable(boolean needSetJListFuturesModel, boolean needResize) {
        if (needSetJListFuturesModel) {
            setJListFuturesModel();
            jListFutures.setSelectedIndex(0);
        }
        if (needResize) {
            Utils.autoResizeTableColWidths(jTableTasks);
            Utils.autoResizeTableRowHeights(jTableTasks);
        }
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
        listModel.addElement("MultiCycleTest");
        listModel.addElement("Last");
        listModel.addElement("Gui.Last");
        listModel.addElement("Resume");
        listModel.addElement("prepReset");
        listModel.addElement("prepStart");
        listModel.addElement("Conveyor");
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

    private volatile @Nullable
    Supplier<@Nullable XFuture<?>> futureToDisplaySupplier = null;

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
                    aprsSys.getTaskName(),
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
            ignoreRobotTableChanges = true;
            for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                jTableRobotsSetValueAt(true, i, 1);
            }
            jTableRobots.getModel().addTableModelListener(robotTableModelListener);
        }
        enableRobotTableModelListener();
    }

    private volatile boolean injTableRobotsSetValueAtCall = false;

    private volatile StackTraceElement set01TrueTrace[] = null;
    private volatile StackTraceElement set01FalseTrace[] = null;

    public StackTraceElement[] getSet01TrueTrace() {
        return set01TrueTrace;
    }

    public StackTraceElement[] getSet01FalseTrace() {
        return set01FalseTrace;
    }

    private void jTableRobotsSetValueAt(@Nullable Object val, int row, int col) {
        synchronized (jTableRobots) {
            if (row == 0 && col == 1 && val instanceof Boolean) {
                Boolean bval = (Boolean) val;
                if (bval) {
                    set01TrueTrace = Thread.currentThread().getStackTrace();
                } else {
                    set01FalseTrace = Thread.currentThread().getStackTrace();
                }
            }
            injTableRobotsSetValueAtCall = true;
            try {
                if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
                    throw new IllegalStateException("called from wrong thread");
                }
                if (!ignoreRobotTableChanges && !resetting && col == 1) {
                    throw new IllegalStateException("ignoreRobotTableChanges=" + ignoreRobotTableChanges);
                }
                if (col == 1 && val instanceof Boolean) {
                    Boolean bval = (Boolean) val;
                    Object oldVal = jTableRobots.getValueAt(row, col);
                    final boolean keepDisabled
                            = !jCheckBoxMenuItemRandomTest.isSelected()
                            && (jCheckBoxMenuItemKeepDisabled.isSelected() || supervisor.isKeepDisabled());
                    if (keepDisabled) {
                        if (bval && oldVal != null && !bval.equals(oldVal)) {
                            if (!lastTableChangeEnabledRobot || lastTableChangeEnabledRobotRow != row) {
                                lastTableChangeEnabledRobot = false;
                                lastTableChangeEnabledRobotRow = -1;
                                throw new RuntimeException("Enabling robot when keep disabled set. row=" + row + ",col=" + col + ",oldVal=" + oldVal + ",bval=" + bval);
                            }
                        }
                    }
                    if (lastTableChangeEnabledRobotRow == row) {
                        lastTableChangeEnabledRobot = false;
                        lastTableChangeEnabledRobotRow = -1;
                    }
                }

                jTableRobots.setValueAt(val, row, col);
                Object chkVal = jTableRobots.getValueAt(row, col);
                if (val != chkVal) {
                    println("jTableRobotsSetValueAt: val  = " + val + ", row=" + row + ",col=" + col);
                    println("chkVal = " + chkVal);
                }
                if (col == 1) {
                    String robotName = (String) jTableRobots.getValueAt(row, 0);
                    boolean enabledInMap = robotTaskMap.get(robotName) != null;
                    if (enabledInMap != ((boolean) val)) {
                        println("jTableRobotsSetValueAt: val  = " + val + ", row=" + row + ",col=" + col);
                        System.err.println("robotEnableMap=" + robotTaskMap);
                        throw new IllegalStateException("setting robots table value not to match map ");
                    }
//                    println("jTableRobotsSetValueAt: val  = " + val + ", row=" + row + ",col=" + col);
                }
            } finally {
                injTableRobotsSetValueAtCall = false;
            }
        }
    }

    public void updateRobotsTableFromMapsAndEnableAll(Map<String, String> robotTaskMap, Map<String, Integer> robotDisableCountMap, Map<String, Long> robotDisableTotalTimeMap) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("call me from AWT event thread.");
        }
        logEvent("updateRobotsTableFromMapsAndEnableAll called.");
        disableRobotTableModelListener();
        boolean valchanged = false;
        boolean enableSets[] = new boolean[jTableRobots.getRowCount()];
        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            boolean enableFromTable = getEnableFromRobotsTable(i);
            if (!enableFromTable && !supervisor.isKeepDisabled() && !jCheckBoxMenuItemKeepDisabled.isSelected()) {
//                println("updateRobotsTableFromMapsAndEnableAll jTableRobotsSetValueAt(true," + i + ", 1)");
                jTableRobotsSetValueAt(true, i, 1);
                jTableRobotsSetValueAt(robotTaskMap.get(robotName), i, 4);
                valchanged = true;
                enableSets[i] = true;
            }
            if (null != robotName) {
                int countFromTable = getDisableCountFromRobotsTable(i);
                int countFromMap = robotDisableCountMap.getOrDefault(robotName, 0);
                if (countFromTable != countFromMap) {
                    println("updateRobotsTableFromMapsAndEnableAll jTableRobotsSetValueAt(" + countFromMap + "," + i + ", 4)");
                    jTableRobotsSetValueAt(countFromMap, i, 5);
                    valchanged = true;
                }
                if (countFromTable != countFromMap || !enableFromTable) {
                    jTableRobotsSetValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 6);
                    valchanged = true;
                }
            } else {
                logEventErr("jTableRobots.getValueAt(i=" + i + ", 0) returned null");
            }
        }
        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            if (!((boolean) jTableRobots.getValueAt(i, 1))) {
                println("enableSets = " + Arrays.toString(enableSets));
                System.err.println("bad value in row i=" + i + " jTableRobots.getValueAt(i, 1)=" + jTableRobots.getValueAt(i, 1));
            }
        }
        if (valchanged) {
            Utils.autoResizeTableColWidths(jTableRobots);
        }
        enableRobotTableModelListener();
    }

    public void updateRobotsTableFromMaps(Map<String, String> robotTaskMap, Map<String, Integer> robotDisableCountMap, Map<String, Long> robotDisableTotalTimeMap) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("call me from AWT event thread.");
        }
        logEvent("updateRobotsTableFromMapsAndEnableAll called.");
        disableRobotTableModelListener();
        boolean valchanged = false;
        boolean enableSets[] = new boolean[jTableRobots.getRowCount()];
        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            boolean enableFromTable = getEnableFromRobotsTable(i);
            final String robotTaskName = robotTaskMap.get(robotName);
            if (!enableFromTable && robotTaskName != null && !supervisor.isKeepDisabled() && !jCheckBoxMenuItemKeepDisabled.isSelected()) {
//                println("updateRobotsTableFromMapsAndEnableAll jTableRobotsSetValueAt(true," + i + ", 1)");
                jTableRobotsSetValueAt(true, i, 1);
                valchanged = true;
                enableSets[i] = true;
            }
            if (null == robotTaskName && enableFromTable) {
                jTableRobotsSetValueAt(false, i, 1);
                valchanged = true;
                enableSets[i] = true;
            }
            jTableRobotsSetValueAt(robotTaskName, i, 4);
            if (null != robotName) {
                int countFromTable = getDisableCountFromRobotsTable(i);
                int countFromMap = robotDisableCountMap.getOrDefault(robotName, 0);
                if (countFromTable != countFromMap) {
                    println("updateRobotsTableFromMapsAndEnableAll jTableRobotsSetValueAt(" + countFromMap + "," + i + ", 4)");
                    jTableRobotsSetValueAt(countFromMap, i, 5);
                    valchanged = true;
                }
                if (countFromTable != countFromMap || !enableFromTable) {
                    jTableRobotsSetValueAt(runTimeToString(robotDisableTotalTimeMap.getOrDefault(robotName, 0L)), i, 6);
                    valchanged = true;
                }
            } else {
                logEventErr("jTableRobots.getValueAt(i=" + i + ", 0) returned null");
            }
        }
        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
            if (!((boolean) jTableRobots.getValueAt(i, 1))) {
                println("enableSets = " + Arrays.toString(enableSets));
                System.err.println("bad value in row i=" + i + " jTableRobots.getValueAt(i, 1)=" + jTableRobots.getValueAt(i, 1));
            }
        }
        if (valchanged) {
            Utils.autoResizeTableColWidths(jTableRobots);
        }
        enableRobotTableModelListener();
    }

    private volatile boolean ignoreRobotTableChanges = false;
    private volatile StackTraceElement disableRobotTableModelListenerTrace[] = null;

    @UIEffect
    private void disableRobotTableModelListener() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException("call me from AWT event thread.");
        }
        if (ignoreRobotTableChanges && !resetting) {
            System.err.println("ignoreRobotTableChanges set twice");
            System.err.println("disableRobotTableModelListenerTrace="
                    + Utils.traceToString(disableRobotTableModelListenerTrace));
            Thread.dumpStack();
        }
//        jTableRobots.getModel().removeTableModelListener(robotTableModelListener);
        ignoreRobotTableChanges = true;
        disableRobotTableModelListenerTrace = Thread.currentThread().getStackTrace();
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

    private static @Nullable
    Field getField(Class<?> clss, String name) {
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

    private static final int XFUTURE_MAX_DEPTH = 30;
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
            Collection<?> cancelFutures = future.getAlsoCancel();
            if (null != cancelFutures) {
                for (Object o : cancelFutures) {
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
//                    println("updateRandomTestCount count = " + count);
                    jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continuous Demo " + count);
                });
    }

//    private static class SetTableRobotEnabledEvent {
//
//        private final boolean enable;
//        private final String robotName;
//        private final XFuture<Boolean> future;
//
//        public SetTableRobotEnabledEvent(boolean enable, String robotName, XFuture<Boolean> future) {
//            this.enable = enable;
//            this.robotName = robotName;
//            this.future = future;
//        }
//
//        public boolean isEnable() {
//            return enable;
//        }
//
//        public String getRobotName() {
//            return robotName;
//        }
//
//        public XFuture<Boolean> getFuture() {
//            return future;
//        }
//
//    };
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
////                        Boolean wasEnabled = robotTaskMap.get(robotName);
//                        setTableRobotEnabledEventDeque.add(setTableRobotEnabledEvent);
//                        boolean enableFromTable = (Boolean) jTableRobots.getValueAt(i, 1);
//                        if (enableFromTable != enable) {
//                            disableRobotTableModelListener();
//                            println("setTableRobotEnabled(" + robotName + "," + enable + ") calling jTableRobotsSetValueAt(" + enable + "," + i + ", 1)");
//                            jTableRobotsSetValueAt(enable, i, 1);
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
        Utils.setToAprsLookAndFeel();

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

    private aprs.conveyor.ConveyorVisJPanel conveyorVisJPanel1;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.supervisor.colortextdisplay.ColorTextJPanel colorTextJPanel1;
    private javax.swing.JButton jButtonAddLine;
    private javax.swing.JButton jButtonAddSharedToolsRow;
    private javax.swing.JButton jButtonDeleteLine;
    private javax.swing.JButton jButtonDeleteSharedToolsRow;
    private javax.swing.JButton jButtonFuturesCancelAll;
    private javax.swing.JButton jButtonGoIn;
    private javax.swing.JButton jButtonGoOut;
    private javax.swing.JButton jButtonPlotPositionMap;
    private javax.swing.JButton jButtonSaveSelectedPosMap;
    private javax.swing.JButton jButtonSetInFromCurrent;
    private javax.swing.JButton jButtonSetOutFromCurrent;
    private javax.swing.JButton jButtonSyncToolsFromRobots;
    private javax.swing.JButton jButtonSyncToolsToRobots;
    private javax.swing.JCheckBox jCheckBoxFutureLongForm;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemBlockTransfers;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContDemoReverseFirstOption;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinuousDemoRevFirst;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebug;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDisableTextPopups;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemEnableConveyorControlView;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemEnableRemoteConsole;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemFixedRandomTestSeed;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIndContinuousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIndRandomToggleTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemKeepAndDisplayXFutureProfiles;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemKeepDisabled;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseAllForOne;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseResumeTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRandomTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRecordLiveImageMovie;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowSplashMessages;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSingleStep;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSkipDisabled;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemUseCorrectionModeByDefault;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemUseTeachCamera;
    private javax.swing.JCheckBox jCheckBoxScrollEvents;
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
    private javax.swing.JLabel jLabel7;
    private javax.swing.JList<String> jListFutures;
    private javax.swing.JList<String> jListFuturesKey;
    private javax.swing.JMenu jMenuActions;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemAddExistingSystem;
    private javax.swing.JMenuItem jMenuItemAddNewSystem;
    private javax.swing.JMenuItem jMenuItemConnectAll;
    private javax.swing.JMenuItem jMenuItemContinueAll;
    private javax.swing.JMenuItem jMenuItemConveyorTest;
    private javax.swing.JMenuItem jMenuItemDbgAction;
    private javax.swing.JMenuItem jMenuItemEnableAndContinueAll;
    private javax.swing.JMenuItem jMenuItemImmediateAbortAll;
    private javax.swing.JMenuItem jMenuItemLoadEventsLog;
    private javax.swing.JMenuItem jMenuItemLoadPosMaps;
    private javax.swing.JMenuItem jMenuItemLoadSetup;
    private javax.swing.JMenuItem jMenuItemLookForPartsAll;
    private javax.swing.JMenuItem jMenuItemMultiCycleTest;
    private javax.swing.JMenuItem jMenuItemOpenAll;
    private javax.swing.JMenuItem jMenuItemRandomTestReverseFirst;
    private javax.swing.JMenuItem jMenuItemReloadSimFiles;
    private javax.swing.JMenuItem jMenuItemRemoveSelectedSystem;
    private javax.swing.JMenuItem jMenuItemResetAll;
    private javax.swing.JMenuItem jMenuItemRestoreOrigRobotInfo;
    private javax.swing.JMenuItem jMenuItemReturnRobot;
    private javax.swing.JMenuItem jMenuItemRunCustom;
    private javax.swing.JMenuItem jMenuItemSafeAbortAll;
    private javax.swing.JMenuItem jMenuItemSaveAll;
    private javax.swing.JMenuItem jMenuItemSavePosMaps;
    private javax.swing.JMenuItem jMenuItemSaveSetup;
    private javax.swing.JMenuItem jMenuItemSaveSetupAs;
    private javax.swing.JMenuItem jMenuItemScanAll;
    private javax.swing.JMenuItem jMenuItemSetConveyorViewCloneSystem;
    private javax.swing.JMenuItem jMenuItemSetGlobalSpeedOverride;
    private javax.swing.JMenuItem jMenuItemSetMaxCycles;
    private javax.swing.JMenuItem jMenuItemSpecialTestFlipFanucPartWithMotomanHelp;
    private javax.swing.JMenuItem jMenuItemSpecialTestFlipMotomanPartWithFanucHelp;
    private javax.swing.JMenuItem jMenuItemStartAll;
    private javax.swing.JMenuItem jMenuItemStartAllReverse;
    private javax.swing.JMenuItem jMenuItemStartColorTextDisplay;
    private javax.swing.JMenuItem jMenuItemStartContinuousScanAndRun;
    private javax.swing.JMenuItem jMenuItemStartScanAllThenContinuousConveyorDemoRevFirst;
    private javax.swing.JMenuItem jMenuItemStartScanAllThenContinuousDemoRevFirst;
    private javax.swing.JMenuItem jMenuItemStep;
    private javax.swing.JMenu jMenuOptions;
    private javax.swing.JMenu jMenuSpecialTests;
    private javax.swing.JPanel jPanelEvents;
    private javax.swing.JPanel jPanelFuture;
    private javax.swing.JPanel jPanelPosMapFiles;
    private javax.swing.JPanel jPanelPosMapSelectedFile;
    private javax.swing.JPanel jPanelPositionMappings;
    private javax.swing.JPanel jPanelRobots;
    private javax.swing.JPanel jPanelSelectedPosMapFileTopButtons;
    private javax.swing.JPanel jPanelTasks;
    private javax.swing.JPanel jPanelTasksAndRobots;
    private javax.swing.JPanel jPanelTeachTable;
    private javax.swing.JPanel jPanelTools;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPaneEventsTable;
    private javax.swing.JScrollPane jScrollPaneListFutures;
    private javax.swing.JScrollPane jScrollPaneListFuturesKey;
    private javax.swing.JScrollPane jScrollPaneRobots;
    private javax.swing.JScrollPane jScrollPaneSharedToolsTable;
    private javax.swing.JScrollPane jScrollPaneTasks;
    private javax.swing.JScrollPane jScrollPaneTreeSelectedFuture;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPaneMain;
    private javax.swing.JTable jTableEvents;
    private javax.swing.JTable jTablePositionMappings;
    private javax.swing.JTable jTableRobots;
    private javax.swing.JTable jTableSelectedPosMapFile;
    private javax.swing.JTable jTableSharedTools;
    private javax.swing.JTable jTableTasks;
    private javax.swing.JTextArea jTextAreaFutureDetails;
    private javax.swing.JTextField jTextFieldEventsLogFile;
    private javax.swing.JTextField jTextFieldEventsMax;
    private javax.swing.JTextField jTextFieldRobotEnableToggleBlockers;
    private javax.swing.JTextField jTextFieldRunningTime;
    private javax.swing.JTextField jTextFieldSelectedPosMapFilename;
    private javax.swing.JTree jTreeSelectedFuture;
    private aprs.simview.Object2DOuterJPanel object2DOuterJPanel1;
    // End of variables declaration//GEN-END:variables
}
