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
import crcl.base.CRCLStatusType;
import crcl.base.CommandStateEnumType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
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
                            if (!Objects.equal(enabled, wasEnabled)) {
                                final int fi = i;
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    if (togglesAllowed) {
                                        setRobotEnabled(robotName, enabled);
                                    } else {
                                        jTableRobots.setValueAt(wasEnabled, fi, 1);
                                    }
                                });
                                break;
                            }
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

        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            try {
                closeAllAprsSystems();
            } catch (IOException ex1) {
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex1);
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
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
                    .thenCompose(x -> checkUnstealRobotFuture(newFuture));
        } else {
            return XFuture.completedFutureWithName("checkUnstealRobotFuture2", null);
        }
    }

    private XFuture<Void> checkStealRobotFuture(XFuture<Void> future) {
        XFuture<Void> currentStealRobotFuture = stealRobotFuture.get();
        if (null != currentStealRobotFuture && currentStealRobotFuture != future) {
            final XFuture<Void> newFuture = currentStealRobotFuture;
            return newFuture
                    .thenCompose(x -> checkUnstealRobotFuture(newFuture));
        } else {
            return XFuture.completedFutureWithName("checkStealRobotFuture2", null);
        }
    }

    private XFuture<?> checkLastReturnedFuture(XFuture<?> inFuture) {
        final XFuture<?> lfr = this.lastFutureReturned;
        if (null != lfr && lfr != inFuture) {
            return lfr
                    .thenCompose(x -> checkLastReturnedFuture(lfr));
        } else {
            return XFuture.completedFutureWithName("checkLastReturnedFuture2", null);
        }
    }

    private List<XFuture> oldLfrs = new ArrayList<>();

    private void setRobotEnabled(String robotName, Boolean enabled) {
        if (null != robotName && null != enabled) {
            robotEnableMap.put(robotName, enabled);
            if (!enabled) {
                if (stealingRobots || stealRobotFuture.get() != null) {
                    return;
                }
                try {
                    final XFuture<Void> origUnstealFuture = unStealRobotFuture.getAndSet(null);;
                    final XFuture<Void> origCancelUnstealFuture = cancelUnStealRobotFuture.getAndSet(null);;
                    if (null != origUnstealFuture && null != origCancelUnstealFuture) {
                        System.out.println("Cancelling origUnstealFuture = " + origUnstealFuture);
                        origUnstealFuture.cancelAll(true);
                        printStatus(origUnstealFuture, System.out);
                    }
                    final XFuture<Void> future = stealRobot(robotName);
                    this.stealRobotFuture.set(future);
                    final XFuture<Void> cancelFuture = new XFuture<>("cancelStealRobotFuture");
                    this.cancelStealRobotFuture.set(cancelFuture);
                    lastFutureReturned = XFuture.anyOf("setRobotEnabled(" + robotName + "," + enabled + ").anyOf",
                            future.handle((x, t) -> (t == null))
                                    .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").future.thenCompose",
                                            x -> {
                                                if (x) {
                                                    return XFuture.completedFutureWithName(
                                                            "setRobotEnabled(" + robotName + "," + enabled + ").neverComplete",
                                                            null);
                                                } else {
                                                    return new XFuture<>("setRobotEnabled(" + robotName + "," + enabled + ").neverComplete");
                                                }
                                            }),
                            cancelFuture);
//                                    .thenCompose("setRobotEnabled(" + robotName + "," + enabled + ").cancelFuture.thenCompose.checkUnstealRobotFuture",
//                                    x -> checkUnstealRobotFuture(null)));

                    if (null != origUnstealFuture && null != origCancelUnstealFuture) {
                        System.out.println("Completing origCancelUnstealFuture= " + origCancelUnstealFuture);
                        origCancelUnstealFuture.complete(null);
                    }
                } catch (IOException | PositionMap.BadErrorMapFormatException ex) {
                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {

                if (!stealingRobots || unStealRobotFuture.get() != null) {
                    return;
                }
                stealingRobots = false;
                XFuture prevLFR = lastFutureReturned;
                final XFuture<Void> origStealFuture = stealRobotFuture.getAndSet(null);
                final XFuture<Void> origCancelStealFuture = cancelStealRobotFuture.getAndSet(null);
                if (null != origStealFuture && null != origCancelStealFuture) {
                    System.out.println("Cancelling origStealFuture= " + origStealFuture);
                    origStealFuture.cancelAll(true);
                    System.out.println("origStealFuture = " + origStealFuture);
                    printStatus(origStealFuture, System.out);
                    System.out.println("lastFutureReturned = " + lastFutureReturned);
                    printStatus(lastFutureReturned, System.out);
                }
                final XFuture<Void> future = unStealRobots();
                this.unStealRobotFuture.set(future);
                final XFuture<Void> cancelFuture = new XFuture<>("cancelUnStealRobotFuture");
                this.cancelUnStealRobotFuture.set(cancelFuture);
                lastFutureReturned = XFuture.anyOf("setRobotEnabled(" + robotName + "," + enabled + ").anyOf",
                        future.handle((x, t) -> (t == null))
                                .thenCompose(x -> {
                                    if (x) {
                                        return XFuture.completedFutureWithName("setRobotEnabled(" + robotName + "," + enabled + ").completedFuture2", null);
                                    } else {
                                        return new XFuture<>("neverComplete");
                                    }
                                }),
                        cancelFuture);
                //.thenCompose(x -> checkStealRobotFuture(null)));

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
                System.out.println("prevLFR = " + prevLFR);
                printStatus(prevLFR, System.out);
//                this.unStealRobotFuture = unStealRobots();
//                this.cancelUnStealRobotFuture = new XFuture<>();
//                final XFuture<Void> origStealFuture = stealRobotFuture;
//                    lastFutureReturned = XFuture.anyOf(this.unStealRobotFuture, 
//                            this.cancelUnStealRobotFuture.thenCompose(x -> checkUnstealRobotFuture(stealRobotFuture)));
//                 lastFutureReturned = XFuture.anyOf(this.stealRobotFuture, 
//                            this.cancelStealRobotFuture.thenCompose(x -> (null != unStealRobotFuture)?unStealRobotFuture:XFuture.completedFuture(null)));
            }
            oldLfrs.add(lastFutureReturned);
        }
    }

    private XFuture<Void> stealRobot(String robotName) throws IOException, PositionMap.BadErrorMapFormatException {
        for (int i = 0; i < aprsSystems.size() - 1; i++) {
            AprsJFrame sys = aprsSystems.get(i);
            if (sys != null && Objects.equal(sys.getRobotName(), robotName)) {
                return stealRobot(aprsSystems.get(i + 1), aprsSystems.get(i));
            }
        }
        return XFuture.completedFutureWithName("stealRobot(" + robotName + ").completedFuture", null);
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

    private void checkRobotsUniquePorts() {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame sys = aprsSystems.get(i);
            if (sys.isConnected()) {
                int port = sys.getRobotCrclPort();
                if (set.contains(port)) {
                    debugAction();
                    pause();
                    throw new IllegalStateException("two systems conneced to " + port);
                }
                set.add(port);
            }
        }
    }

    private void returnRobots1() {
        returnRobots();
    }

    private void returnRobots2() {
        returnRobots();
    }

    private void returnRobots3() {
        returnRobots();
    }

    private void returnRobots4() {
        returnRobots();
    }

    private void returnRobots() {
        checkRobotsUniquePorts();
        this.stealingRobots = false;
        Runnable r = returnRobotRunnable.getAndSet(null);
        if (r != null) {
            r.run();
        }
        checkRobotsUniquePorts();
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
        return SplashScreen.showMessageFullScreen(errMsgString, 80.0f,
                null,
                SplashScreen.getRedYellowColorList(), gd);
    }

    private XFuture<Void> stealAbortFuture = null;
    private XFuture<Void> unstealAbortFuture = null;

    private volatile boolean stealingRobots = false;

    private XFuture<Void> stealRobot(AprsJFrame stealFrom, AprsJFrame stealFor) throws IOException, PositionMap.BadErrorMapFormatException {

        disallowToggles();
        if (returnRobotRunnable.get() != null || stealingRobots) {
            System.out.println("calling stealrRobot when already stealingRobots");
            return stealRobotFuture.get();
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
        returnRobotRunnable.set(() -> {
            disallowToggles();
            stealFrom.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);

            for (String opt : transferrableOptions) {
                if (stealForOptions.containsKey(opt)) {
                    stealFor.setExecutorOption(opt, stealForOptions.get(opt));
                }
            }
            stealFor.removePositionMap(pm);
            stealFor.connectRobot(stealForRobotName, stealForOrigCrclHost, stealForOrigCrclPort);
            checkRobotsUniquePorts();
            stealFor.disconnectRobot();
            stealFor.setRobotName(stealForRobotName);
        }
        );

        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();

        unStealRobotsSupplier.set(() -> {
            disallowToggles();
            stealingRobots = false;
            unstealAbortFuture = XFuture.allOf("unStealAbortAllOf",
                    stealFrom.startSafeAbortAndDisconnectAsync(),
                    stealFor.startSafeAbortAndDisconnectAsync()
                            .thenCompose("unstealShowReenable", x -> {
                                if (null != colorTextSocket) {
                                    try {
                                        colorTextSocket.getOutputStream().write("0x00FF00, 0x00FF00\r\n".getBytes());
                                    } catch (IOException ex) {
                                        Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                return SplashScreen.showMessageFullScreen(stealForRobotName + "\n Enabled", 80.0f,
                                        SplashScreen.getDisableImageImage(),
                                        SplashScreen.getBlueWhiteGreenColorList(), gd);
                            }));
            return unstealAbortFuture
                    .thenRun("unsteal.returnRobots1", this::returnRobots1)
                    .thenRun("unsteal.connectAll", () -> connectAll())
                    .thenRun(this::allowToggles)
                    .thenCompose("unsteal.continueAllOf", x -> {
                        return XFuture.allOf(
                                stealFrom.continueActionList(),
                                stealFor.continueActionList()
                                        .thenRun(this::disallowToggles)
                        );
                    });
        });
        stealingRobots = true;
        stealAbortFuture = XFuture.allOf("stealAbortAllOf",
                stealFrom.startSafeAbortAndDisconnectAsync(),
                stealFor.startSafeAbortAndDisconnectAsync()
                        .thenCompose(x -> {
                            if (null != colorTextSocket) {
                                try {
                                    colorTextSocket.getOutputStream().write("0xFF0000, 0x00FF00\r\n".getBytes());
                                } catch (IOException ex) {
                                    Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            return SplashScreen.showMessageFullScreen(stealForRobotName + "\n Disabled", 80.0f,
                                    SplashScreen.getDisableImageImage(),
                                    SplashScreen.getRedYellowColorList(), gd);
                        }));
        return stealAbortFuture
                .thenRun("transfer", () -> {
                    stealFor.connectRobot(stealFromRobotName, stealFromOrigCrclHost, stealFromOrigCrclPort);
                    stealFor.addPositionMap(pm);
                    for (String opt : transferrableOptions) {
                        if (stealFromOptions.containsKey(opt)) {
                            stealFor.setExecutorOption(opt, stealFromOptions.get(opt));
                        }
                    }
                })
                .thenCompose("showSwitching", x -> {
                    return SplashScreen.showMessageFullScreen("Switching to \n" + stealFromRobotName, 80.0f,
                            SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                })
                .thenRun(() -> this.allowToggles())
                .thenCompose("continueAfterSwitch", x -> {
                    return stealFor.continueActionList();
                })
                .thenRun(() -> {
                    this.disallowToggles();
                })
                .thenCompose("startSafeAbortAndDisconnectAsyncAfterSwitch", x -> {
                    return stealFor.startSafeAbortAndDisconnectAsync();
                })
                .thenCompose("showReturning", x -> {
                    return SplashScreen.showMessageFullScreen("Returning \n" + stealFromRobotName, 80.0f,
                            SplashScreen.getRobotArmImage(), SplashScreen.getBlueWhiteGreenColorList(), gd);
                })
                .thenRun("returnRobots2", this::returnRobots2)
                .thenCompose("continueAfterReturn", x -> {
                    return stealFrom.continueActionList();
                });
//                .thenCompose(x -> {
//                    return SplashScreen.showMessageFullScreen("All \nTasks \nComplete", 80.0f,
//                            null, SplashScreen.getBlueWhiteGreenColorList(), gd);
//                });
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
        jMenuItemContinueAll = new javax.swing.JMenuItem();
        jMenuItemConnectAll = new javax.swing.JMenuItem();
        jMenuItemDbgAction = new javax.swing.JMenuItem();
        jMenuItemResetAll = new javax.swing.JMenuItem();
        jMenuItemStartAllReverse = new javax.swing.JMenuItem();
        jCheckBoxMenuItemContinousDemo = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemContinousDemoRevFirst = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPause = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemRandomTest = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPauseResumeTest = new javax.swing.JCheckBoxMenuItem();
        jMenuOptions = new javax.swing.JMenu();
        jCheckBoxMenuItemDisableTextPopups = new javax.swing.JCheckBoxMenuItem();
        jMenuItemStartColorTextDisplay = new javax.swing.JMenuItem();
        jCheckBoxMenuItemDebugStartReverse = new javax.swing.JCheckBoxMenuItem();

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

        jMenuItemResetAll.setText("Reset All");
        jMenuItemResetAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemResetAllActionPerformed(evt);
            }
        });
        jMenuActions.add(jMenuItemResetAll);

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

        jCheckBoxMenuItemPause.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAUSE, 0));
        jCheckBoxMenuItemPause.setText("Pause");
        jCheckBoxMenuItemPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemPause);

        jCheckBoxMenuItemRandomTest.setText("Randomized Enable Toggle Continous Demo");
        jCheckBoxMenuItemRandomTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemRandomTestActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemRandomTest);

        jCheckBoxMenuItemPauseResumeTest.setText("Pause Resume Test");
        jCheckBoxMenuItemPauseResumeTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPauseResumeTestActionPerformed(evt);
            }
        });
        jMenuActions.add(jCheckBoxMenuItemPauseResumeTest);

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
                Logger.getLogger(AprsSupervisorJFrame.class
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
                Logger.getLogger(AprsSupervisorJFrame.class
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
                Logger.getLogger(AprsSupervisorJFrame.class
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
                    Logger.getLogger(AprsSupervisorJFrame.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                updateTasksTable();
                updateRobotsTable();
                saveCurrentSetup();
            } catch (IOException ex) {
                Logger.getLogger(AprsSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemRemoveSelectedSystemActionPerformed

    private volatile XFuture<?> lastFutureReturned = null;

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
                Logger.getLogger(AprsSupervisorJFrame.class
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
                Logger.getLogger(AprsSupervisorJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void jMenuItemSafeAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSafeAbortAllActionPerformed
        lastFutureReturned = safeAbortAll();
        lastFutureReturned.thenRun(() -> immediateAbortAll());
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemPauseResumeTest.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
    }//GEN-LAST:event_jMenuItemSafeAbortAllActionPerformed

    private void jMenuItemImmediateAbortAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemImmediateAbortAllActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        immediateAbortAll();
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        jCheckBoxMenuItemRandomTest.setSelected(false);
        jCheckBoxMenuItemPauseResumeTest.setSelected(false);
        jCheckBoxMenuItemPause.setSelected(false);
    }//GEN-LAST:event_jMenuItemImmediateAbortAllActionPerformed

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
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonSaveSelectedPosMapActionPerformed

    private void jCheckBoxMenuItemDisableTextPopupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed
        crcl.ui.misc.MultiLineStringJPanel.disableShowText = jCheckBoxMenuItemDisableTextPopups.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDisableTextPopupsActionPerformed

    private void jMenuItemDbgActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDbgActionActionPerformed
        debugAction();
    }//GEN-LAST:event_jMenuItemDbgActionActionPerformed

    private void printStatus(AtomicReference<XFuture<Void>> ref, PrintStream ps) {
        if (null != ref) {
            XFuture<?> xf = ref.get();
            printStatus(xf, ps);
        }
    }

    public void printStatus(XFuture<?> xf, PrintStream ps) {
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
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemAddNewSystemActionPerformed

    private volatile XFuture<Void> continousDemoFuture = null;

    private void jCheckBoxMenuItemContinousDemoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinousDemoActionPerformed
        jCheckBoxMenuItemContinousDemoRevFirst.setSelected(false);
        immediateAbortAll();
        clearAllErrors();
        connectAll();
        setReverseFlag(false);
        enableAllRobots();
        continousDemoCycle.set(0);
        if (jCheckBoxMenuItemContinousDemo.isSelected()) {
            continousDemoFuture = startContinousDemo();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemContinousDemoActionPerformed

    private void jMenuActionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuActionsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuActionsActionPerformed

    private final AtomicReference<XFuture<Void>> resumeFuture = new AtomicReference<>(null);

    private void jCheckBoxMenuItemPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseActionPerformed
        if (jCheckBoxMenuItemPause.isSelected()) {
            pause();
        } else {
            resume();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseActionPerformed

    private volatile XFuture<Void> randomTest = null;

    private void jCheckBoxMenuItemRandomTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRandomTestActionPerformed
        immediateAbortAll();
        clearAllErrors();
        connectAll();
        setReverseFlag(false);
        enableAllRobots();
        continousDemoCycle.set(0);
        randomTestCount.set(0);
        if (jCheckBoxMenuItemRandomTest.isSelected()) {
            randomTest = startRandomTest();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemRandomTestActionPerformed

    private void jMenuItemStartAllReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStartAllReverseActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
        lastFutureReturned = startReverseActions();
    }//GEN-LAST:event_jMenuItemStartAllReverseActionPerformed

    private void jMenuItemResetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetAllActionPerformed
        resetAll();
    }//GEN-LAST:event_jMenuItemResetAllActionPerformed

    public void resetAll() {
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
            aprsSystems.get(i).reset();
        }
    }

    private void jCheckBoxMenuItemPauseResumeTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed
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
        setReverseFlag(false);
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
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPauseResumeTestActionPerformed

    private void jCheckBoxMenuItemDebugStartReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugStartReverseActionPerformed
        debugStartReverseActions = jCheckBoxMenuItemDebugStartReverse.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDebugStartReverseActionPerformed

    private void jMenuItemContinueAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemContinueAllActionPerformed
        if (null != lastFutureReturned) {
            lastFutureReturned.cancelAll(true);
        }
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
        if(jCheckBoxMenuItemContinousDemo.isSelected()) {
            continousDemoFuture = 
                    lastFutureReturned
                    .thenCompose("jMenuItemContinueAllActionPerformed.continueAllActions",
                            x -> continueAllActions());
                    
        }
    }//GEN-LAST:event_jMenuItemContinueAllActionPerformed

    private void jCheckBoxMenuItemContinousDemoRevFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemContinousDemoRevFirstActionPerformed
        jCheckBoxMenuItemContinousDemo.setSelected(false);
        immediateAbortAll();
        clearAllErrors();
        connectAll();
        setReverseFlag(false);
        enableAllRobots();
        continousDemoCycle.set(0);
        if (jCheckBoxMenuItemContinousDemoRevFirst.isSelected()) {
            continousDemoFuture = startContinousDemoRevFirst();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemContinousDemoRevFirstActionPerformed

    public XFuture<Void> startRandomTest() {
        connectAll();
        return checkEnabledAll()
                .thenCompose("startRandomTest.checkOk",
                        ok -> checkOkElse(ok, this::startRandomTestStep2, this::showCheckEnabledErrorSplash));
    }

    private XFuture<Void> startRandomTestStep2() {
        continousDemoFuture = startContinousDemo();
        jCheckBoxMenuItemContinousDemo.setSelected(true);
        randomTest = continueRandomTest();
        return randomTest;
    }

    private Random random = new Random(System.currentTimeMillis());

    private XFuture<Void> startRandomDelay(String name, final int millis, final int min_millis) {
        final long val = random.nextInt(millis) + 10 + min_millis;
        return XFuture.runAsync(name + ".randomDelay(" + millis + ":" + val + ")",
                () -> {
                    try {
                        Thread.sleep(val);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
        );
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

    private void allowToggles() {
        allowTogglesCount.incrementAndGet();
        togglesAllowed = true;
        XFuture xf = togglesAllowedXfuture.get();
        if (null != xf) {
            xf.complete(null);
        }
        while ((xf = waitForTogglesFutures.poll()) != null) {
            xf.complete(null);
        }
    }

    private final AtomicInteger allowTogglesCount = new AtomicInteger();
    private final AtomicInteger disallowTogglesCount = new AtomicInteger();

    private void disallowToggles() {
        disallowTogglesCount.incrementAndGet();
        togglesAllowed = false;
        togglesAllowedXfuture.updateAndGet(this::createWaitForTogglesFuture);
    }

//    private XFuture<Void> toggleRobotEnabledWithWait() {
//        return waitTogglesAllowed()
//                .thenCompose(x -> toggleRobotEnabled());
//    }
    private XFuture<Void> toggleRobotEnabled() {
        if (jCheckBoxMenuItemPause.isSelected()) {
            return waitResume();
        }
        return Utils.runOnDispatchThread("toggleRobotEnabled.runOnDispatchThread",
                () -> {
                    if (!jCheckBoxMenuItemPause.isSelected() && togglesAllowed) {
                        for (int i = 0; i < jTableRobots.getRowCount(); i++) {
                            String robotName = (String) jTableRobots.getValueAt(i, 0);
                            if (robotName.toLowerCase().contains("motoman")) { // stupid hard-coded hack to match demo
                                Boolean enabled = (Boolean) jTableRobots.getValueAt(i, 1);
                                Boolean wasEnabled = robotEnableMap.get(robotName);
                                jTableRobots.setValueAt(!wasEnabled, i, 1);
//                    setRobotEnabled(robotName, !wasEnabled);
                                break;
                            }
                        }
                    }
                });
    }

    private final AtomicInteger randomTestCount = new AtomicInteger();

    private XFuture<Void> updateRandomTestCount() {
        return Utils.runOnDispatchThread("updateRandomTest.runOnDispatchThread",
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
                .thenCompose(x -> Utils.runOnDispatchThread(this::pause))
                .thenCompose(x -> startRandomDelay("pauseTest", 1000, 1000))
                .thenCompose(x -> Utils.runOnDispatchThread(this::resume))
                .thenCompose(x -> continuePauseTest());
        return pauseTest;
    }

    private XFuture<Void> continueRandomTest() {
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
        randomTest = startRandomDelay("enableTest", 30000, 20000)
                .thenCompose("checkForWaitResume1", x -> this.waitResume())
                .thenCompose("waitTogglesAllowed", x -> this.waitTogglesAllowed())
                .thenCompose("toggleRobotEnabled", x -> this.toggleRobotEnabled())
                .thenCompose("updateRandomTestCount", x -> this.updateRandomTestCount())
                .thenCompose("continueRandomTest", x -> continueRandomTest());
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

    public void setReverseFlag(boolean reverseFlag) {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).setReverseFlag(reverseFlag);
        }
    }

    public void disconnectAll() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).setConnected(false);
        }
    }

    public XFuture<Void> startContinousDemoRevFirst() {
        connectAll();
        continousDemoFuture
                = checkEnabledAll()
                        .thenCompose("startContinousDemoRevFirst.startReverseActions", x -> startReverseActions())
                        .thenCompose("continueContinousDeomo.checkEnabledAll", x -> checkEnabledAll())
                        .thenCompose("startContinousDemoRevFirst", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash));
        return continousDemoFuture;
    }
    
    public XFuture<Void> startContinousDemo() {
        connectAll();
        continousDemoFuture
                = checkEnabledAll()
                        .thenCompose("startContinousDemo", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash));
        return continousDemoFuture;
    }

    private final AtomicInteger continousDemoCycle = new AtomicInteger(0);

    private XFuture<Void> incrementContinousDemoCycle() {
        final int c = continousDemoCycle.incrementAndGet();
        System.out.println("incrementContinousDemoCycle : " + c);
        if(jCheckBoxMenuItemContinousDemoRevFirst.isSelected()) {
            return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinousDemoRevFirst.setText("Continous Demo (Reverse First) (" + c + ") "));
        } else {
            return Utils.runOnDispatchThread(() -> jCheckBoxMenuItemContinousDemo.setText("Continous Demo (" + c + ") "));
        }
    }

    private XFuture<Void> continueContinousDemo() {
        System.out.println("stealingRobots = " + stealingRobots);
        System.out.println("returnRobotRunnable = " + returnRobotRunnable);
        disallowToggles();
        if (this.stealingRobots || null != returnRobotRunnable.get()) {
            disconnectAll();
            returnRobots3();
            disconnectAll();
        } else {
            checkRobotsUniquePorts();
        }
        System.out.println("stealingRobots = " + stealingRobots);
        System.out.println("returnRobotRunnable = " + returnRobotRunnable);
        cancelAllStealUnsteal(false);
        connectAll();
        setReverseFlag(false);
        final XFuture<?> lfr = this.lastFutureReturned;
        continousDemoFuture
                = Utils.runOnDispatchThread(this::enableAllRobots)
                        .thenCompose(x -> startAllActions())
                        .thenCompose("continueContinousDeomo.checkLastReturnedFuture1", x -> checkLastReturnedFuture(lfr))
                        .thenCompose("continueContinousDeomo.startReverseActions", x -> startReverseActions())
                        .thenCompose("continueContinousDeomo.checkLastReturnedFuture2", x -> checkLastReturnedFuture(lfr))
                        .thenCompose("continueContinousDeomo.incrementContinousDemoCycle", x -> incrementContinousDemoCycle())
                        .thenRun(this::disallowToggles)
                        .thenCompose("continueContinousDeomo.checkEnabledAll", x -> checkEnabledAll())
                        .thenCompose("continueContinousDeomo.recurse", ok -> checkOkElse(ok, this::continueContinousDemo, this::showCheckEnabledErrorSplash));
        return continousDemoFuture;
    }

    private volatile boolean debugStartReverseActions = false;

    public XFuture<Void> startReverseActions() {
        disallowToggles();
        setReverseFlag(true);
        if (debugStartReverseActions) {
            debugAction();
        }
        return Utils.runOnDispatchThread(this::enableAllRobots)
                .thenCompose("startReverseActions.checkAllEnabled", x -> checkEnabledAll())
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

    public void enableAllRobots() {
        cancelAllStealUnsteal(false);
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        DefaultTableModel model = (DefaultTableModel) jTableRobots.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String robotName = (String) jTableRobots.getValueAt(i, 0);
            robotEnableMap.put(robotName, true);
            model.setValueAt(true, i, 1);
        }
        Utils.autoResizeTableColWidths(jTableRobots);
        if (null != colorTextSocket) {
            try {
                colorTextSocket.getOutputStream().write("0x00FF00, 0x00FF000\r\n".getBytes());
                colorTextSocket.getOutputStream().flush();
            } catch (IOException ex) {
                Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public XFuture<Boolean> checkEnabledAll() {
        XFuture<Boolean> futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).startCheckEnabled();
        }
        XFuture<Boolean> ret = XFuture.completedFuture(true);
        BiFunction<Boolean, Boolean, Boolean> andBiFunction = (Boolean ok1, Boolean ok2) -> ok1 && ok2;
        for (int i = 0; i < futures.length; i++) {
            ret = (XFuture<Boolean>) ret.thenCombine(futures[i], andBiFunction);
        }
        return ret;
    }
    AtomicInteger pauseCount = new AtomicInteger();

    private void pause() {
        completeResumeFuture();
        int count = pauseCount.incrementAndGet();
        jCheckBoxMenuItemPause.setText("Pause (" + count + ") ");
        if (!jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(true);
        }
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsSys = aprsSystems.get(i);
            if (aprsSys.isConnected()) {
                aprsSys.pause();
            }
//            aprsSys.pause();
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
        if (jCheckBoxMenuItemPause.isSelected()) {
            jCheckBoxMenuItemPause.setSelected(false);
        }
//        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            AprsJFrame aprsSys = aprsSystems.get(i);
            if (aprsSys.isPaused()) {
                aprsSys.resume();
            }
//            else {
//                futures[i] = XFuture.completedFuture(null);
//            }
//            futures[i] = aprsSys.resume();
        }
//        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
//        XFuture<Void> ret = XFuture.allOf("resumeAllOf", futures);
//        completeResumeFuture();
//        return ret;
    }

    private volatile XFuture lastStartAllActionsArray[] = null;

    private XFuture<Void> startAllActions() {
        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).startActions();
        }
        lastStartAllActionsArray = futures;
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        allowToggles();
        return XFuture.allOf("startAll", futures);
    }

    private XFuture<Void> continueAllActions() {
        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).continueActionList();
        }
        lastStartAllActionsArray = futures;
        final GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        allowToggles();
        return XFuture.allOf("startAll", futures);
    }

    private XFuture<Void> checkOkElse(Boolean ok, Supplier<XFuture<Void>> okSupplier, Supplier<XFuture<Void>> notOkSupplier) {
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

    public XFuture<Void> continueAll() {
        stealingRobots = false;
        returnRobots3();
        return Utils.runOnDispatchThread(this::enableAllRobots)
                .thenCompose("startAll.checkEnabledAll", x -> checkEnabledAll())
                .thenCompose(ok -> checkOkElse(ok, this::continueAllActions, this::showCheckEnabledErrorSplash));
    }

    public XFuture<Void> startAll() {
        stealingRobots = false;
        returnRobots3();
        return Utils.runOnDispatchThread(this::enableAllRobots)
                .thenCompose("startAll.checkEnabledAll", x -> checkEnabledAll())
                .thenCompose(ok -> checkOkElse(ok, this::startAllActions, this::showCheckEnabledErrorSplash));
    }

    public void clearAllErrors() {
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).clearErrors();
        }
    }

    public void immediateAbortAll() {
        stealingRobots = false;
        cancelAll(true);
        for (int i = 0; i < aprsSystems.size(); i++) {
            aprsSystems.get(i).immediateAbort();
        }
        if (this.stealingRobots || null != returnRobotRunnable.get()) {
            disconnectAll();
            returnRobots();
            disconnectAll();
        } else {
            checkRobotsUniquePorts();
        }
    }

    private void cancelAll(boolean mayInterrupt) {
        disallowToggles();
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

    public void connectAll() {
        try {
            initColorTextSocket();
        } catch (IOException ex) {
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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

    public XFuture<Void> safeAbortAll() {
        XFuture<?> prevLastFuture = lastFutureReturned;
        XFuture futures[] = new XFuture[aprsSystems.size()];
        for (int i = 0; i < aprsSystems.size(); i++) {
            futures[i] = aprsSystems.get(i).startSafeAbort();
        }
        return XFuture.allOf("safeAbortAll", futures).thenRun(() -> {
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
     */
    public void setSetupFile(File f) throws IOException {
        if (!Objects.equal(this.setupFile, f)) {
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
            Logger.getLogger(AprsSupervisorJFrame.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(AprsSupervisorJFrame.class
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AprsSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AprsSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AprsSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AprsSupervisorJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
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
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinousDemo;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemContinousDemoRevFirst;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebugStartReverse;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDisableTextPopups;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPause;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPauseResumeTest;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRandomTest;
    private javax.swing.JMenu jMenuActions;
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
    private javax.swing.JMenuItem jMenuItemSafeAbortAll;
    private javax.swing.JMenuItem jMenuItemSavePosMaps;
    private javax.swing.JMenuItem jMenuItemSaveSetup;
    private javax.swing.JMenuItem jMenuItemSaveSetupAs;
    private javax.swing.JMenuItem jMenuItemStartAll;
    private javax.swing.JMenuItem jMenuItemStartAllReverse;
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
