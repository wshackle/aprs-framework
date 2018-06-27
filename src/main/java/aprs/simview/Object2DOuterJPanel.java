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
package aprs.simview;

import aprs.system.AprsSystem;
import aprs.misc.SlotOffsetProvider;
import aprs.misc.Utils;
import static aprs.misc.Utils.autoResizeTableColWidths;
import aprs.database.PhysicalItem;
import aprs.database.DbSetupBuilder;
import aprs.database.Slot;
import aprs.database.Tray;
import aprs.database.vision.VisionSocketClient;
import aprs.database.vision.VisionSocketServer;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLStatusType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.client.PendantClientJPanel;
import crcl.utils.CRCLPosemath;
import crcl.utils.CRCLSocket;
import java.awt.event.ActionEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import rcs.posemath.PmCartesian;
import static aprs.database.PhysicalItem.newPhysicalItemNameRotXYScoreType;
import crcl.base.CommandStateEnumType;
import crcl.base.CommandStatusType;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JDialog;
import javax.swing.table.TableModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("CanBeFinal")
public class Object2DOuterJPanel extends javax.swing.JPanel implements Object2DJFrameInterface, VisionSocketClient.VisionSocketClientListener, PendantClientJPanel.CurrentPoseListener {

    public static List<PhysicalItem> showAndModifyData(List<PhysicalItem> itemsIn, SlotOffsetProvider sop, double minX, double minY, double maxX, double maxY) {
        JDialog diag = new JDialog();
        diag.setModal(true);
        Object2DOuterJPanel panel = new Object2DOuterJPanel();
        panel.setViewLimits(minX, minY, maxX, maxY);
        panel.setSlotOffsetProvider(sop);
        panel.setItems(itemsIn);
        panel.setSimulatedAndDisconnect();
        diag.add(panel);
        diag.pack();
        diag.setVisible(true);
        return panel.getItems();
    }

    public BufferedImage createSnapshotImage() {
        return object2DJPanel1.createSnapshotImage();
    }

    public BufferedImage createSnapshotImage(Object2DJPanel.ViewOptions opts) {
        return object2DJPanel1.createSnapshotImage(opts);
    }

    public BufferedImage createSnapshotImage(Object2DJPanel.ViewOptions opts, Collection<? extends PhysicalItem> itemsToPaint) {
        return object2DJPanel1.createSnapshotImage(opts, itemsToPaint);
    }

    public List<PhysicalItem> getItems() {
        return object2DJPanel1.getItems();
    }

    private List<PhysicalItem> getOutputItems() {
        return object2DJPanel1.getOutputItems();
    }

    private volatile boolean settingItems = false;

    @Override
    public void takeSnapshot(File f, PoseType pose, String label) {
        if (null != pose) {
            takeSnapshot(f, pose.getPoint(), label);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null);
        }
    }

    @Override
    public void takeSnapshot(File f, PointType point, String label) {
        if (null != point) {
            takeSnapshot(f, CRCLPosemath.toPmCartesian(point), label);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null);
        }
    }

    @Override
    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) {
        try {
            this.object2DJPanel1.takeSnapshot(f, point, label);
            File csvDir = new File(f.getParentFile(), "csv");
            csvDir.mkdirs();
            saveFile(new File(csvDir, f.getName() + ".csv"));
            if (null != aprsSystem) {
                File xmlDir = new File(f.getParentFile(), "crclStatusXml");
                xmlDir.mkdirs();
                CRCLStatusType status = aprsSystem.getCurrentStatus();
                if (null != status) {
                    String xmlString = CRCLSocket.statusToPrettyString(status);
                    File xmlFile = new File(xmlDir, f.getName() + "-status.xml");
                    try (FileWriter fw = new FileWriter(xmlFile)) {
                        fw.write(xmlString);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void takeSnapshot(File f, @Nullable PoseType pose, @Nullable String label, int w, int h) {
        if (null != pose) {
            takeSnapshot(f, pose.getPoint(), label, w, h);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null, w, h);
        }
    }

    @Override
    public void takeSnapshot(File f, @Nullable PointType point, @Nullable String label, int w, int h) {
        if (null != point) {
            takeSnapshot(f, CRCLPosemath.toPmCartesian(point), label, w, h);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null);
        }
    }

    @Override
    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label, int w, int h) {
        try {
            this.object2DJPanel1.takeSnapshot(f, point, label, w, h);
            File csvDir = new File(f.getParentFile(), "csv");
            csvDir.mkdirs();
            saveFile(new File(csvDir, f.getName() + ".csv"));
            File xmlDir = new File(f.getParentFile(), "crclStatusXml");
            xmlDir.mkdirs();
            AprsSystem af = this.aprsSystem;
            if (null != af) {
                CRCLStatusType status = aprsSystem.getCurrentStatus();
                if (null != status) {
                    String xmlString = CRCLSocket.statusToPrettyString(status);
                    File xmlFile = new File(xmlDir, f.getName() + "-status.xml");
                    try (FileWriter fw = new FileWriter(xmlFile)) {
                        fw.write(xmlString);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean forceOutputFlag;

    /**
     * Get the value of forceOutputFlag
     *
     * @return the value of forceOutputFlag
     */
    public boolean isForceOutputFlag() {
        return forceOutputFlag;
    }

    /**
     * Set the value of forceOutputFlag
     *
     * @param forceOutputFlag new value of forceOutputFlag
     */
    public void setForceOutputFlag(boolean forceOutputFlag) {
        this.forceOutputFlag = forceOutputFlag;
    }

    private volatile long lastRefreshTime;
    private final AtomicInteger refreshCount = new AtomicInteger();

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public int getRefreshCount() {
        return refreshCount.get();
    }

    public long getLastPublishTime() {
        return (null != visionSocketServer) ? visionSocketServer.getLastPublishTime() : -1;
    }

    public int getPublishCount() {
        return (null != visionSocketServer) ? visionSocketServer.getPublishCount() : -1;
    }

    public void refresh(boolean loadFile) {
        try {
            if (jCheckBoxSimulated.isSelected()) {
                boolean fileLoaded = false;
                if (loadFile) {
                    String fname = jTextFieldFilename.getText().trim();
                    File f = new File(fname);
                    if (f.exists() && f.canRead()) {
                        try {
                            loadFile(f);
                            fileLoaded = true;
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                if (!fileLoaded && null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
                    this.setItems(object2DJPanel1.getItems());
                    publishCurrentItems();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lastRefreshTime = System.currentTimeMillis();
            refreshCount.incrementAndGet();
        }
    }

    @Override
    public void setItems(List<PhysicalItem> items) {
        setItems(items, true);
    }

    private volatile @Nullable
    Map<String, Integer> origNamesMap = null;

    private void setItems(List<PhysicalItem> items, boolean publish) {
        settingItems = true;
//        for (PhysicalItem item : items) {
//            if (item.x < 100 || item.y > 300) {
//                System.out.println("item = " + item);
//            }
//        }
        Utils.runOnDispatchThread(() -> {
            setItemsInternal(items);
            settingItems = false;
        });
        if (publish) {
            VisionSocketServer srv = this.visionSocketServer;
            if (null != srv && !this.jCheckBoxPause.isSelected()) {
                srv.publishList(items);
            }
        }
    }

    public void setOutputItems(List<PhysicalItem> items) {
        settingItems = true;
        Utils.runOnDispatchThread(() -> {
            setOutputItemsInternal(items);
            settingItems = false;
        });
    }

    private void setItemsInternal(List<PhysicalItem> items) {
        if (null != aprsSystem && aprsSystem.isVisionToDbConnected()) {
            object2DJPanel1.setRotationOffset(aprsSystem.getVisionToDBRotationOffset());
        }
        object2DJPanel1.setItems(items);
        updateItemsTableInternal(items);
        loadTraySlotInfo(items);
    }

    private void updateItemsTable(List<PhysicalItem> items) {
        settingItems = true;
        Utils.runOnDispatchThread(() -> {
            updateItemsTableInternal(items);
            settingItems = false;
        });
    }

    private void updateItemsTableInternal(List<PhysicalItem> items) {
        if (!object2DJPanel1.isShowOutputItems()) {
            if (object2DJPanel1.isShowAddedSlotPositions()) {
                loadItemsToTable(object2DJPanel1.getItemsWithAddedExtras(), jTableItems);
            } else {
                loadItemsToTable(items, jTableItems);
            }
        }
    }

    private static double minDist(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType().equals("P"))
                .mapToDouble(x -> Math.hypot(x.x - sx, x.y - sy))
                .min()
                .orElse(Double.POSITIVE_INFINITY);
    }

    private boolean slotFilled(double sx, double sy, List<PhysicalItem> items) {
        return minDist(sx, sy, items) < 20.0;
    }

    private void loadTraySlotInfo(List<PhysicalItem> items) {
        int row = jTableItems.getSelectedRow();
        if (row < 0) {
            return;
        }
        DefaultTableModel tm = (DefaultTableModel) jTableTraySlots.getModel();
        tm.setRowCount(0);
        if (object2DJPanel1.isShowOutputItems()) {
            return;
        }

        String type = (String) jTableItems.getValueAt(row, 5);
        if (null == type) {
            return;
        }
        switch (type) {
            case "PT":
            case "KT":
                Object nameObject = jTableItems.getValueAt(row, 1);
                Object xObject = jTableItems.getValueAt(row, 2);
                Object yObject = jTableItems.getValueAt(row, 3);
                Object rotObject = jTableItems.getValueAt(row, 4);
                if (null == nameObject) {
                    return;
                }
                if (null == xObject) {
                    return;
                }
                if (null == yObject) {
                    return;
                }
                if (null == rotObject) {
                    return;
                }
                String name = (String) nameObject;
                double x = (double) xObject;
                double y = (double) yObject;
                double rot = Math.toRadians((double) rotObject);
                if (null != slotOffsetProvider) {
                    Tray trayItem = new Tray(name, rot, x, y);
                    List<Slot> l = slotOffsetProvider.getSlotOffsets(name, true);
                    if (null != l) {
                        for (Slot s : l) {
                            Slot absItem = slotOffsetProvider.absSlotFromTrayAndOffset(trayItem, s);
                            if (null != absItem) {
                                double minDist = minDist(absItem.x, absItem.y, items);
                                tm.addRow(new Object[]{s.getSlotForSkuName(), minDist < 20.0, absItem.x, absItem.y, minDist});
                            }
                        }
                    }
//                    else {
//                        System.err.println("cant' find name");
//                        List<Slot> l2 = slotOffsetProvider.getSlotOffsets(name, true);
//                        System.out.println("l2 = " + l2);
//                    }
                }
                break;

            default:
                tm.setRowCount(0);
        }
        Utils.autoResizeTableColWidths(jTableTraySlots);
    }

    private void setOutputItemsInternal(List<PhysicalItem> items) {
        object2DJPanel1.setOutputItems(items);
        if (object2DJPanel1.isShowOutputItems()) {
            if (object2DJPanel1.isShowAddedSlotPositions()) {
                loadItemsToTable(object2DJPanel1.getOutputItemsWithAddedExtras(), jTableItems);
            } else {
                loadItemsToTable(items, jTableItems);
            }
        }
    }

    private void loadItemsToTable(List<PhysicalItem> items, JTable jtable) {
        boolean origSettingItems = settingItems;
        settingItems = true;
        int origSelectedRow = jtable.getSelectedRow();
        int origSelectedRowIndex = -1;

        if (origSelectedRow >= 0 && origSelectedRow < jtable.getRowCount()) {
            Integer indexFromTable = (Integer) jtable.getValueAt(origSelectedRow, 0);
            if (null != indexFromTable) {
                origSelectedRowIndex = (int) indexFromTable;
            }
        }

        RowSorter<? extends TableModel> rowSorter = jtable.getRowSorter();
        if (null != rowSorter) {
            jtable.setRowSorter(null);
        }
        DefaultTableModel model = (DefaultTableModel) jtable.getModel();
        model.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            PhysicalItem item = items.get(i);
            Object rowObjects[] = new Object[]{i, item.getName(), item.x, item.y, Math.toDegrees(item.getRotation()), item.getType(), item.getScore()};
            model.addRow(rowObjects);
        }
        autoResizeTableColWidths(jtable);
        if (null != rowSorter) {
            jtable.setRowSorter(rowSorter);
            rowSorter.allRowsChanged();
        }
        int newSelectedRowIndex = -1;
        if (origSelectedRow >= 0 && origSelectedRow < jtable.getRowCount()) {
            Integer indexFromTable = (Integer) jtable.getValueAt(origSelectedRow, 0);
            if (null != indexFromTable) {
                newSelectedRowIndex = (int) indexFromTable;
            }
        }
        if (newSelectedRowIndex > 0 && newSelectedRowIndex == origSelectedRowIndex) {
            DefaultListSelectionModel dlsm;
            ListSelectionModel lsm = jtable.getSelectionModel();
            if (lsm instanceof DefaultListSelectionModel) {
                dlsm = (DefaultListSelectionModel) lsm;
            } else {
                dlsm = new DefaultListSelectionModel();
            }
            dlsm.setSelectionInterval(origSelectedRow, origSelectedRow);
            dlsm.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            jtable.setSelectionModel(dlsm);
        }
        settingItems = origSettingItems;
    }

    public List<PhysicalItem> computeAbsSlotPositions(List<PhysicalItem> l) {
        return object2DJPanel1.computeAbsSlotPositions(l);
    }

    /**
     * Creates new form Object2DOuterJPanel
     */
    @SuppressWarnings("initialization")
    public Object2DOuterJPanel() {
        initComponents();
        this.setItemsInternal(object2DJPanel1.getItems());
        jTableItems.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                try {
                    boolean changeFound = false;

                    if (!settingItems && !object2DJPanel1.isShowOutputItems()) {
                        List<PhysicalItem> l = new ArrayList<>(getItems());
                        PhysicalItem item = null;
                        for (int i = 0; i < jTableItems.getRowCount(); i++) {
                            int listIndex = (int) jTableItems.getValueAt(i, 0);
                            if (jTableItems.getValueAt(i, 1) == null || jTableItems.getValueAt(i, 1).toString().length() < 1) {
                                continue;
                            }
                            if (listIndex < l.size()) {
                                item = l.get(listIndex);
                            } else {
                                item = null;
                            }
                            if (item == null || item.getName() == null
                                    || !Objects.equals(item.getType(), jTableItems.getValueAt(i, 5))
                                    || !Objects.equals(item.getName(), jTableItems.getValueAt(i, 1))
                                    || Math.abs(item.x - Double.parseDouble(jTableItems.getValueAt(i, 2).toString())) > 0.001
                                    || Math.abs(item.y - Double.parseDouble(jTableItems.getValueAt(i, 3).toString())) > 0.001
                                    || Math.abs(item.getRotation() - Double.parseDouble(jTableItems.getValueAt(i, 4).toString())) > 0.001
                                    || Math.abs(item.getScore() - Double.parseDouble(jTableItems.getValueAt(i, 6).toString())) > 0.001) {
                                changeFound = true;
                            } else {
                                continue;
                            }
                            String name = Objects.toString(jTableItems.getValueAt(i, 1));
                            if (item == null || !item.getName().equals(name)) {
                                double x = Double.parseDouble(jTableItems.getValueAt(i, 2).toString());
                                double y = Double.parseDouble(jTableItems.getValueAt(i, 3).toString());
                                double rotation = Math.toRadians(Double.parseDouble(jTableItems.getValueAt(i, 4).toString()));
                                String type = Objects.toString(jTableItems.getValueAt(i, 5));
                                double score = Double.parseDouble(jTableItems.getValueAt(i, 6).toString());
                                item = newPhysicalItemNameRotXYScoreType(name, rotation, x, y, score, type);
                            }
                            item.x = Double.parseDouble(jTableItems.getValueAt(i, 2).toString());
                            item.y = Double.parseDouble(jTableItems.getValueAt(i, 3).toString());
                            item.setRotation(Math.toRadians(Double.parseDouble(jTableItems.getValueAt(i, 4).toString())));
                            item.setType(Objects.toString(jTableItems.getValueAt(i, 5)));
                            item.setScore(Double.parseDouble(jTableItems.getValueAt(i, 6).toString()));
                            while (l.size() < listIndex) {
                                l.add(null);
                            }
                            l.set(listIndex, item);
                        }
                        if (changeFound) {
                            setItems(l);
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
        jTableItems.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                int selectedRow = jTableItems.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < jTableItems.getRowCount()) {
                    object2DJPanel1.setSelectedItemIndex(
                            (int) (jTableItems.getValueAt(selectedRow, 0)));
                    if (!object2DJPanel1.isShowOutputItems()) {
                        loadTraySlotInfo(getItems());
                    }
                }
            }
        });
        jTableTraySlots.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (!jCheckBoxSimulated.isSelected()) {
                    return;
                }
                if (e.getColumn() == 1 && e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    if (row == e.getLastRow()) {
                        boolean filled = (boolean) jTableTraySlots.getValueAt(row, 1);
                        List<PhysicalItem> l = new ArrayList<>();
                        int selectedRowIndex = jTableItems.getSelectedRow();
                        if (filled) {
                            l.addAll(getItems());
                            double sx = (double) jTableTraySlots.getValueAt(row, 2);
                            double sy = (double) jTableTraySlots.getValueAt(row, 3);
                            String name = (String) jTableTraySlots.getValueAt(row, 0);
                            PhysicalItem newPart = newPhysicalItemNameRotXYScoreType(name, 0.0, sx, sy, 100.0, "P");
                            l.add(newPart);
                        } else {
                            double sx = (double) jTableTraySlots.getValueAt(row, 2);
                            double sy = (double) jTableTraySlots.getValueAt(row, 3);
                            List<PhysicalItem> items = getItems();
                            for (int i = 0; i < items.size(); i++) {
                                PhysicalItem it = items.get(i);
                                if (Math.hypot(sx - it.x, sy - it.y) > 20.0) {
                                    l.add(it);
                                } else if (i < selectedRowIndex) {
                                    selectedRowIndex--;
                                }
                            }
                        }
                        javax.swing.SwingUtilities.invokeLater(() -> setItemsInternal(l));
                        int newSelectedRowIndex = selectedRowIndex;
                        javax.swing.SwingUtilities.invokeLater(() -> jTableItems.getSelectionModel().setSelectionInterval(newSelectedRowIndex, newSelectedRowIndex));
                    }
                }
            }
        });
        setMaxXMaxYText(jTextFieldMaxXMaxY.getText().trim());
        setMinXMinYText(jTextFieldMinXMinY.getText().trim());
        object2DJPanel1.setShowCurrentXY(jCheckBoxShowCurrent.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "nullness"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        object2DJPanel1 = new aprs.simview.Object2DJPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableItems = new javax.swing.JTable();
        jTextFieldFilename = new javax.swing.JTextField();
        jButtonSave = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelOptionsTab = new javax.swing.JPanel();
        jTextFieldCurrentXY = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jButtonOffsetAll = new javax.swing.JButton();
        jButtonReset = new javax.swing.JButton();
        jCheckBoxShowRotations = new javax.swing.JCheckBox();
        jButtonCurrent = new javax.swing.JButton();
        jCheckBoxShowCurrent = new javax.swing.JCheckBox();
        jComboBoxDisplayAxis = new javax.swing.JComboBox<>();
        jButtonAdd = new javax.swing.JButton();
        jTextFieldMaxXMaxY = new javax.swing.JTextField();
        jCheckBoxSeparateNames = new javax.swing.JCheckBox();
        jCheckBoxAutoscale = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldMinXMinY = new javax.swing.JTextField();
        jButtonDelete = new javax.swing.JButton();
        jCheckBoxAddSlots = new javax.swing.JCheckBox();
        jCheckBoxDetails = new javax.swing.JCheckBox();
        jCheckBoxTools = new javax.swing.JCheckBox();
        jPanelConnectionsTab = new javax.swing.JPanel();
        jCheckBoxSimulated = new javax.swing.JCheckBox();
        jTextFieldHost = new javax.swing.JTextField();
        jTextFieldPort = new javax.swing.JTextField();
        jCheckBoxConnected = new javax.swing.JCheckBox();
        jLabelHost = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jCheckBoxDebug = new javax.swing.JCheckBox();
        jCheckBoxPause = new javax.swing.JCheckBox();
        jButtonRefresh = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaConnectDetails = new javax.swing.JTextArea();
        jPanelSimulationTab = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldSimulationUpdateTime = new javax.swing.JTextField();
        jCheckBoxShuffleSimulatedUpdates = new javax.swing.JCheckBox();
        jCheckBoxSimulationUpdateAsNeeded = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldSimDropRate = new javax.swing.JTextField();
        jCheckBoxAddPosNoise = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldPosNoise = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextFieldRotNoise = new javax.swing.JTextField();
        jCheckBoxViewOutput = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldPickupDist = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldDropOffThreshold = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableTraySlots = new javax.swing.JTable();

        object2DJPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        object2DJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseMoved(evt);
            }
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseDragged(evt);
            }
        });
        object2DJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                object2DJPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseExited(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseEntered(evt);
            }
        });

        javax.swing.GroupLayout object2DJPanel1Layout = new javax.swing.GroupLayout(object2DJPanel1);
        object2DJPanel1.setLayout(object2DJPanel1Layout);
        object2DJPanel1Layout.setHorizontalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 346, Short.MAX_VALUE)
        );
        object2DJPanel1Layout.setVerticalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jTableItems.setAutoCreateRowSorter(true);
        jTableItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Index", "Name", "X", "Y", "Rotation", "Type", "Score"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableItems.setMaximumSize(new java.awt.Dimension(400, 64));
        jScrollPane1.setViewportView(jTableItems);

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jButtonLoad.setText("Load");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });

        jTextFieldCurrentXY.setText("0.0,0.0");
        jTextFieldCurrentXY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldCurrentXYActionPerformed(evt);
            }
        });

        jLabel2.setText("Xmin,Ymin : ");

        jButtonOffsetAll.setText("Offset All");
        jButtonOffsetAll.setEnabled(false);
        jButtonOffsetAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOffsetAllActionPerformed(evt);
            }
        });

        jButtonReset.setText("Reset");
        jButtonReset.setEnabled(false);
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jCheckBoxShowRotations.setSelected(true);
        jCheckBoxShowRotations.setText("Show Rotations/Images");
        jCheckBoxShowRotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowRotationsActionPerformed(evt);
            }
        });

        jButtonCurrent.setText("Current");
        jButtonCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCurrentActionPerformed(evt);
            }
        });

        jCheckBoxShowCurrent.setText("Show");
        jCheckBoxShowCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowCurrentActionPerformed(evt);
            }
        });

        jComboBoxDisplayAxis.setModel(new DefaultComboBoxModel<>(DisplayAxis.values()));
        jComboBoxDisplayAxis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDisplayAxisActionPerformed(evt);
            }
        });

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jTextFieldMaxXMaxY.setText("700.0, 315.0");
        jTextFieldMaxXMaxY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMaxXMaxYActionPerformed(evt);
            }
        });

        jCheckBoxSeparateNames.setSelected(true);
        jCheckBoxSeparateNames.setText("Names");
        jCheckBoxSeparateNames.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSeparateNamesActionPerformed(evt);
            }
        });

        jCheckBoxAutoscale.setSelected(true);
        jCheckBoxAutoscale.setText("Auto Scale");
        jCheckBoxAutoscale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAutoscaleActionPerformed(evt);
            }
        });

        jLabel4.setText("Current");

        jLabel3.setText("Xmax,Ymax");

        jTextFieldMinXMinY.setText("200.0, -315.0");
        jTextFieldMinXMinY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMinXMinYActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jCheckBoxAddSlots.setText("Slots");
        jCheckBoxAddSlots.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAddSlotsActionPerformed(evt);
            }
        });

        jCheckBoxDetails.setText("Details");
        jCheckBoxDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDetailsActionPerformed(evt);
            }
        });

        jCheckBoxTools.setText("Tools");
        jCheckBoxTools.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxToolsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelOptionsTabLayout = new javax.swing.GroupLayout(jPanelOptionsTab);
        jPanelOptionsTab.setLayout(jPanelOptionsTabLayout);
        jPanelOptionsTabLayout.setHorizontalGroup(
            jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                            .addComponent(jCheckBoxShowRotations)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBoxDisplayAxis, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                            .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                                    .addComponent(jCheckBoxShowCurrent)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel4))
                                .addComponent(jLabel3))
                            .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxDetails)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxTools)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxAddSlots)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxSeparateNames)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxAutoscale))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jButtonOffsetAll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCurrent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDelete)
                        .addGap(4, 4, 4)
                        .addComponent(jButtonAdd)))
                .addContainerGap())
        );
        jPanelOptionsTabLayout.setVerticalGroup(
            jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxAutoscale)
                    .addComponent(jCheckBoxSeparateNames)
                    .addComponent(jCheckBoxAddSlots)
                    .addComponent(jCheckBoxDetails)
                    .addComponent(jCheckBoxTools))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxShowRotations)
                    .addComponent(jComboBoxDisplayAxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxShowCurrent))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonDelete)
                    .addComponent(jButtonAdd)
                    .addComponent(jButtonReset)
                    .addComponent(jButtonCurrent)
                    .addComponent(jButtonOffsetAll))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelOptionsTabLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonAdd, jButtonDelete});

        jTabbedPane1.addTab("Options", jPanelOptionsTab);

        jPanelConnectionsTab.setMaximumSize(new java.awt.Dimension(407, 32767));

        jCheckBoxSimulated.setText("Simulated");
        jCheckBoxSimulated.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulatedActionPerformed(evt);
            }
        });

        jTextFieldHost.setText("localhost");

        jTextFieldPort.setText("4000");

        jCheckBoxConnected.setText("Connected");
        jCheckBoxConnected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxConnectedActionPerformed(evt);
            }
        });

        jLabelHost.setText("Host:");

        jLabel1.setText("Port:");

        jCheckBoxDebug.setText("Debug");
        jCheckBoxDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxDebugActionPerformed(evt);
            }
        });

        jCheckBoxPause.setText("Pause");
        jCheckBoxPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxPauseActionPerformed(evt);
            }
        });

        jButtonRefresh.setText("Refresh");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        jLabel11.setText("Details:");

        jTextAreaConnectDetails.setEditable(false);
        jTextAreaConnectDetails.setColumns(20);
        jTextAreaConnectDetails.setRows(5);
        jScrollPane3.setViewportView(jTextAreaConnectDetails);

        javax.swing.GroupLayout jPanelConnectionsTabLayout = new javax.swing.GroupLayout(jPanelConnectionsTab);
        jPanelConnectionsTab.setLayout(jPanelConnectionsTabLayout);
        jPanelConnectionsTabLayout.setHorizontalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addComponent(jScrollPane3)
                        .addContainerGap())
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxSimulated)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxConnected, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                        .addGap(142, 142, 142))
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelHost)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonRefresh))
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxDebug)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxPause)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanelConnectionsTabLayout.setVerticalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulated)
                    .addComponent(jCheckBoxConnected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHost)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonRefresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxDebug)
                    .addComponent(jCheckBoxPause))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Connections", jPanelConnectionsTab);

        jLabel5.setText("Simulated Frequency (in ms)  :");

        jTextFieldSimulationUpdateTime.setEditable(false);
        jTextFieldSimulationUpdateTime.setText("50   ");
        jTextFieldSimulationUpdateTime.setEnabled(false);
        jTextFieldSimulationUpdateTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSimulationUpdateTimeActionPerformed(evt);
            }
        });

        jCheckBoxShuffleSimulatedUpdates.setText("Shuffle simulated updates");
        jCheckBoxShuffleSimulatedUpdates.setEnabled(false);

        jCheckBoxSimulationUpdateAsNeeded.setSelected(true);
        jCheckBoxSimulationUpdateAsNeeded.setText("Simulate Updates only as needed.");
        jCheckBoxSimulationUpdateAsNeeded.setEnabled(false);
        jCheckBoxSimulationUpdateAsNeeded.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulationUpdateAsNeededActionPerformed(evt);
            }
        });

        jLabel6.setText("Simulated Drop Out Rate ( 0.0 to 1.0)  : ");

        jTextFieldSimDropRate.setEditable(false);
        jTextFieldSimDropRate.setText("0.0    ");
        jTextFieldSimDropRate.setEnabled(false);
        jTextFieldSimDropRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSimDropRateActionPerformed(evt);
            }
        });

        jCheckBoxAddPosNoise.setText("Add Position Noise");
        jCheckBoxAddPosNoise.setEnabled(false);
        jCheckBoxAddPosNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxAddPosNoiseActionPerformed(evt);
            }
        });

        jLabel7.setText("Pos Noise: ");

        jTextFieldPosNoise.setEditable(false);
        jTextFieldPosNoise.setText("1.0    ");
        jTextFieldPosNoise.setEnabled(false);
        jTextFieldPosNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPosNoiseActionPerformed(evt);
            }
        });

        jLabel8.setText("Rotation Noise: ");

        jTextFieldRotNoise.setEditable(false);
        jTextFieldRotNoise.setText("1.0         ");
        jTextFieldRotNoise.setEnabled(false);
        jTextFieldRotNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldRotNoiseActionPerformed(evt);
            }
        });

        jCheckBoxViewOutput.setText("View Output");
        jCheckBoxViewOutput.setEnabled(false);
        jCheckBoxViewOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxViewOutputActionPerformed(evt);
            }
        });

        jLabel9.setText("Pickup Dist: ");

        jTextFieldPickupDist.setEditable(false);
        jTextFieldPickupDist.setText("5.0   ");
        jTextFieldPickupDist.setEnabled(false);
        jTextFieldPickupDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPickupDistActionPerformed(evt);
            }
        });

        jLabel10.setText("Drop off Threshold: ");

        jTextFieldDropOffThreshold.setEditable(false);
        jTextFieldDropOffThreshold.setText("25.0     ");
        jTextFieldDropOffThreshold.setEnabled(false);
        jTextFieldDropOffThreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldDropOffThresholdActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSimulationTabLayout = new javax.swing.GroupLayout(jPanelSimulationTab);
        jPanelSimulationTab.setLayout(jPanelSimulationTabLayout);
        jPanelSimulationTabLayout.setHorizontalGroup(
            jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSimulationUpdateTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxViewOutput))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSimDropRate))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addGap(120, 120, 120))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxShuffleSimulatedUpdates)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxAddPosNoise)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPickupDist)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldDropOffThreshold)
                            .addComponent(jTextFieldRotNoise))))
                .addContainerGap())
        );
        jPanelSimulationTabLayout.setVerticalGroup(
            jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextFieldSimulationUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxViewOutput))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldSimDropRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxShuffleSimulatedUpdates)
                    .addComponent(jCheckBoxAddPosNoise))
                .addGap(3, 3, 3)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(jTextFieldRotNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextFieldPickupDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(jTextFieldDropOffThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Simulation", jPanelSimulationTab);

        jTableTraySlots.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Slot Name", "Fill", "sx", "sy", "dist"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTableTraySlots);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Trays", jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextFieldFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoad))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 430, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSave)
                    .addComponent(jButtonLoad))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private double posNoise = 1.0;

    /**
     * Get the value of posNoise
     *
     * @return the value of posNoise
     */
    public double getPosNoise() {
        return posNoise;
    }

    /**
     * Set the value of posNoise
     *
     * @param posNoise new value of posNoise
     */
    private void setPosNoise(double posNoise) {
        updateTextFieldDouble(posNoise, jTextFieldPosNoise, 0.01);
        this.posNoise = posNoise;
    }

    private double rotNoise = 1.0;

    /**
     * Get the value of rotNoise
     *
     * @return the value of rotNoise
     */
    public double getRotNoise() {
        return rotNoise;
    }

    private void updateTextFieldDouble(double value, JTextField textField, double threshold) {
        if (Math.abs(value - Double.parseDouble(textField.getText().trim())) > threshold) {
            textField.setText(String.format("%.3f", value));
        }
    }

    /**
     * Set the value of rotNoise
     *
     * @param rotNoise new value of rotNoise
     */
    private void setRotNoise(double rotNoise) {
        updateTextFieldDouble(rotNoise, jTextFieldRotNoise, 0.01);
        this.rotNoise = rotNoise;
    }

    private void setSimulatedInternal(boolean simulated) {

        jButtonAdd.setEnabled(simulated);
        jButtonDelete.setEnabled(simulated);
        jButtonReset.setEnabled(simulated);
        jButtonOffsetAll.setEnabled(simulated);
        jTextFieldSimulationUpdateTime.setEditable(simulated && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        jTextFieldSimulationUpdateTime.setEnabled(simulated && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        jCheckBoxSimulationUpdateAsNeeded.setEnabled(simulated);
        jPanelSimulationTab.setEnabled(simulated);
        jTextFieldSimDropRate.setEnabled(simulated);
        jTextFieldSimDropRate.setEditable(simulated);
        jCheckBoxShuffleSimulatedUpdates.setEnabled(simulated);
        jCheckBoxAddPosNoise.setEnabled(simulated);
        jCheckBoxViewOutput.setEnabled(simulated);
        jTextFieldPosNoise.setEditable(simulated && jCheckBoxAddPosNoise.isSelected());
        jTextFieldPosNoise.setEnabled(simulated && jCheckBoxAddPosNoise.isSelected());
        jTextFieldRotNoise.setEditable(simulated && jCheckBoxAddPosNoise.isSelected());
        jTextFieldRotNoise.setEnabled(simulated && jCheckBoxAddPosNoise.isSelected());
        object2DJPanel1.setShowOutputItems(simulated && jCheckBoxViewOutput.isSelected());
        if (simulated) {
            jTextFieldHost.setEditable(false);
            jTextFieldHost.setEnabled(false);
            jLabelHost.setEnabled(false);
            setupSimUpdateTimer();
        } else {
            jTextFieldHost.setEditable(true);
            jTextFieldHost.setEnabled(true);
            if (null != visionSocketServer) {
                visionSocketServer.close();
                visionSocketServer = null;
            }
            if (null != simUpdateTimer) {
                simUpdateTimer.stop();
                simUpdateTimer = null;
            }
        }
    }

    private void jCheckBoxSimulatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulatedActionPerformed
        this.jCheckBoxConnected.setSelected(false);
        setSimulatedInternal(this.jCheckBoxSimulated.isSelected());
        disconnect();
    }//GEN-LAST:event_jCheckBoxSimulatedActionPerformed

    public void setSimulatedAndDisconnect() {
        this.jCheckBoxConnected.setSelected(false);
        this.jCheckBoxSimulated.setSelected(true);
        setSimulatedInternal(true);
        disconnect();
    }

    @Nullable
    private VisionSocketServer visionSocketServer = null;
    @Nullable
    private VisionSocketClient visionSocketClient = null;

    private void jCheckBoxConnectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxConnectedActionPerformed
        jButtonReset.setEnabled(false);
        if (this.jCheckBoxConnected.isSelected()) {
            connect();
        } else {
            if (this.jCheckBoxSimulated.isSelected()) {
                jButtonReset.setEnabled(true);
            }
            disconnect();
        }
    }//GEN-LAST:event_jCheckBoxConnectedActionPerformed

    private void disconnect() {
        if (null != visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            visionSocketClient.removeListListener(this);
            visionSocketClient = null;
        }
        if (null != visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }
    }

    private void connect() throws NumberFormatException {
        if (this.jCheckBoxSimulated.isSelected()) {
            try {
                int port = Integer.parseInt(this.jTextFieldPort.getText().trim());
                if (null != visionSocketServer && visionSocketServer.getPort() != port) {
                    disconnect();
                }
                if (null == visionSocketServer) {
                    visionSocketServer = new VisionSocketServer(port);
                }
                visionSocketServer.setDebug(this.jCheckBoxDebug.isSelected());
                publishCurrentItems();
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            int port = Integer.parseInt(jTextFieldPort.getText().trim());
            String host = jTextFieldHost.getText().trim();
            if (null != visionSocketClient) {
                if (visionSocketClient.isConnected()
                        && port == visionSocketClient.getPort()
                        && Objects.equals(visionSocketClient.getHost(), host)) {
                    return;
                }
                try {
                    visionSocketClient.close();
                } catch (Exception ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            VisionSocketClient clnt = new VisionSocketClient();
            this.visionSocketClient = clnt;
            Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();
            argsMap.put("--visionport", jTextFieldPort.getText().trim());
            argsMap.put("--visionhost", host);
            clnt.setDebug(this.jCheckBoxDebug.isSelected());
            clnt.start(argsMap);
            if (!clnt.isConnected()) {
                jCheckBoxConnected.setSelected(false);
                try {
                    clnt.close();
                } catch (Exception ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                visionSocketClient = null;
                return;
            }
            clnt.addListener(this);
        }
    }

    private double simulatedDropRate = 0.0;

    /**
     * Get the value of simulatedDropRate
     *
     * @return the value of simulatedDropRate
     */
    public double getSimulatedDropRate() {
        return simulatedDropRate;
    }

    /**
     * Set the value of simulatedDropRate
     *
     * @param simulatedDropRate new value of simulatedDropRate
     */
    private void setSimulatedDropRate(double simulatedDropRate) {
        if (simulatedDropRate > 1.0 || simulatedDropRate < -Double.MIN_VALUE) {
            throw new IllegalArgumentException("simulatedDropRate must be between 0 and 1.0 but was " + simulatedDropRate);
        }
        if (simulatedDropRate < 0.001) {
            simulatedDropRate = 0;
        }
        updateTextFieldDouble(simulatedDropRate, jTextFieldSimDropRate, 0.001);
        this.simulatedDropRate = simulatedDropRate;
    }

    private final Random dropRandom = new Random();

    private boolean dropFilter(Object x) {
        if (simulatedDropRate < 0.001) {
            return true;
        }
        return dropRandom.nextDouble() > simulatedDropRate;
    }

    private final Random posRandom = new Random();

    
    private PhysicalItem noiseFilter(PhysicalItem in) {
        if (!jCheckBoxAddPosNoise.isSelected()) {
            return in;
        }
        PhysicalItem out = newPhysicalItemNameRotXYScoreType(in.getName(),
                in.getRotation() + nextRotNoise(),
                in.x + nextPosNoise(),
                in.y + nextPosNoise(), in.getScore(), in.getType());
        String fullName = in.getFullName();
        if (null != fullName && in.isFullNameSet()) {
            out.setFullName(fullName);
        }
        return out;
    }

    private double nextLimitedGaussian() {
        double g =  posRandom.nextGaussian() ;
        if(g < -3.5) {
            return -3.5;
        } else if(g > 3.5) {
            return 3.5;
        } else {
            return g;
        }
    }
    
    private double nextPosNoise() {
        return nextLimitedGaussian()*posNoise;
    }

    private double nextRotNoise() {
        return nextLimitedGaussian()* Math.toRadians(rotNoise);
    }

    
    private void publishCurrentItems() {
        if (forceOutputFlag) {
            return;
        }
        VisionSocketServer srv = this.visionSocketServer;
        if (null == srv) {
            throw new IllegalStateException("visionSocketServer is null");
        }
        if (jCheckBoxShuffleSimulatedUpdates.isSelected() || simulatedDropRate > 0.01 || jCheckBoxAddPosNoise.isSelected()) {
            List<PhysicalItem> origList = getItems();
            List<PhysicalItem> l = new ArrayList<>(origList);
            if (simulatedDropRate > 0.01 || jCheckBoxAddPosNoise.isSelected()) {
                l = l.stream()
                        .filter(this::dropFilter)
                        .map(this::noiseFilter)
                        .collect(Collectors.toList());
            }
            if (jCheckBoxShuffleSimulatedUpdates.isSelected()) {
                Collections.shuffle(l);
            }
            srv.publishList(l);
            setOutputItems(l);
        } else {
            srv.publishList(getItems());
        }
    }

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        List<PhysicalItem> l = new ArrayList<>(getItems());
        PhysicalItem item = newPhysicalItemNameRotXYScoreType("item_" + (l.size() + 1), 0,
                (object2DJPanel1.getMaxX() + object2DJPanel1.getMinX()) / 2.0,
                (object2DJPanel1.getMaxY() + object2DJPanel1.getMinY()) / 2.0,
                100.0,
                "P"
        );
        l.add(item);
        setItems(l);
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        int row = jTableItems.getSelectedRow();
        List<PhysicalItem> oldList = getItems();
        if (row >= 0 && row < oldList.size()) {
            List<PhysicalItem> l = new ArrayList<>(getItems());
            l.remove(jTableItems.getSelectedRow());
            setItems(l);
        }
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private volatile double last_drag_scale;
    private volatile double last_drag_min_x;
    private volatile double last_drag_max_x;
    private volatile double last_drag_min_y;
    private volatile double last_drag_max_y;

    private volatile long mouseDragTime = -1;

    public boolean isPartMoving() {
        return null != this.draggedItem || System.currentTimeMillis() - mouseDragTime < 30;
    }

    private void object2DJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseDragged
        double scale = object2DJPanel1.getScale();
        double min_x = object2DJPanel1.getMinX();
        double max_x = object2DJPanel1.getMaxX();
        double min_y = object2DJPanel1.getMinY();
        double max_y = object2DJPanel1.getMaxY();
        PhysicalItem itemToDrag = this.draggedItem;
        if (!evt.isShiftDown() && null != draggedItem && !"P".equals(draggedItem.getType())) {
            System.out.println("Hold SHIFT to move trays : closestItem=" + draggedItem.getFullName());
            draggedItem = null;
            return;
        }
        mouseDragTime = System.currentTimeMillis();
        if (null != itemToDrag) {
            double orig_x = itemToDrag.x;
            double orig_y = itemToDrag.y;
            switch (object2DJPanel1.getDisplayAxis()) {
                case POS_X_POS_Y:
                    itemToDrag.x = ((evt.getX() - 15) / scale) + min_x;
                    itemToDrag.y = max_y - ((evt.getY() - 20) / scale);
                    break;

                case POS_Y_NEG_X:
                    itemToDrag.x = ((evt.getY() - 20) / scale) + min_x;
                    itemToDrag.y = ((evt.getX() - 15) / scale) + min_y;
                    break;

                case NEG_X_NEG_Y:
                    itemToDrag.x = max_x - ((evt.getX() - 15) / scale);
                    itemToDrag.y = ((evt.getY() - 20) / scale) + min_y;
                    break;

                case NEG_Y_POS_X:
                    itemToDrag.x = max_x - ((evt.getY() - 20) / scale);
                    itemToDrag.y = max_y - ((evt.getX() - 15) / scale);
                    break;
            }
//            itemToDrag.x = ((evt.getX() - 15) / scale) + min_x;
//            itemToDrag.y = max_y - ((evt.getY() - 20) / scale);
            double xdiff = itemToDrag.x - orig_x;
            double ydiff = itemToDrag.y - orig_y;
            if (Math.abs(xdiff) > 100 || Math.abs(ydiff) > 100) {
                System.out.println("big drag jump");
                this.draggedItem = null;
                return;
            }
            last_drag_max_x = max_x;
            last_drag_min_x = min_x;
            last_drag_max_y = max_y;
            last_drag_min_y = min_y;
            last_drag_scale = scale;

            if (!evt.isShiftDown() && !evt.isAltDown() && !evt.isControlDown()) {
                List<PhysicalItem> origItems = this.getItems();
                if (itemToDrag.getMaxSlotDist() > 0) {
                    for (int i = 0; i < origItems.size(); i++) {
                        PhysicalItem item = origItems.get(i);
                        if (item == itemToDrag) {
                            continue;
                        }
                        if (item.getMaxSlotDist() > 0) {
                            continue;
                        }
                        if (item.dist(orig_x, orig_y) > itemToDrag.getMaxSlotDist() * object2DJPanel1.getSlotMaxDistExpansion()) {
                            continue;
                        }
                        boolean foundCloser = false;
                        for (int j = 0; j < origItems.size(); j++) {
                            if (j == i) {
                                continue;
                            }
                            PhysicalItem otherItem = origItems.get(j);
                            if (otherItem == itemToDrag) {
                                continue;
                            }
                            if (otherItem == item) {
                                continue;
                            }
                            if (otherItem.getMaxSlotDist() > 1e-6
                                    && item.dist(otherItem) < item.dist(orig_x, orig_y)) {
                                foundCloser = true;
                                break;
                            }
                        }
                        if (foundCloser) {
                            continue;
                        }
                        item.x += xdiff;
                        item.y += ydiff;
                    }
                }
            }
            this.updateItemsTable(getItems());
            object2DJPanel1.repaint();
        }
//        int minIndex = -1;
//        int x = evt.getX();
//        int y = evt.getY();
//        ClosestItemInfo closestItemInfo = new ClosestItemInfo(x, y, minIndex);
//        PhysicalItem closestItem = closestItemInfo.getClosestItem();
//        minIndex = closestItemInfo.getMinIndex();
//        if (minIndex >= 0 && closestItem == itemToDrag) {
//            ListSelectionModel selectModel = jTableItems.getSelectionModel();
//            selectModel.setAnchorSelectionIndex(minIndex);
//            selectModel.setLeadSelectionIndex(minIndex);
//            selectModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//            selectModel.setSelectionInterval(minIndex, minIndex);
//            object2DJPanel1.setSelectedItemIndex(minIndex);
//        }
    }//GEN-LAST:event_object2DJPanel1MouseDragged

    @Nullable
    private volatile PhysicalItem draggedItem = null;

    private boolean insideItem(PhysicalItem item, int x, int y) {
        if (null == item || null == item.getDisplayTransform()) {
            return false;
        }
        Rectangle2D itemDisplayRect = item.getDisplayRect();
        if (null == itemDisplayRect) {
            return false;
        }
        boolean inside = false;
        try {
            AffineTransform relTransform = item.getRelTransform();
            if (null == relTransform) {
                return false;
            }
            Point2D newPoint = relTransform.inverseTransform(new Point2D.Double(x, y), new Point2D.Double());
//            System.out.println("newPoint = " + newPoint.getX() + ", " + newPoint.getY());
            inside = itemDisplayRect.contains(newPoint);
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return inside;
    }

    private void object2DJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MousePressed

        int x = evt.getX();
        int y = evt.getY();
        draggedItem = null;

        int minIndex = -1;
        ClosestItemInfo closestItemInfo = new ClosestItemInfo(x, y, minIndex);
        PhysicalItem closestItem = closestItemInfo.getClosestItem();
        minIndex = closestItemInfo.getMinIndex();
        if (!evt.isShiftDown() && null != closestItem && !"P".equals(closestItem.getType())) {
            draggedItem = null;
            System.out.println("Hold SHIFT to move trays : closestItem=" + closestItem.getFullName());
            return;
        }
        if (minIndex >= 0) {

            ListSelectionModel selectModel = jTableItems.getSelectionModel();
            selectModel.setAnchorSelectionIndex(minIndex);
            selectModel.setLeadSelectionIndex(minIndex);
            selectModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            selectModel.setSelectionInterval(minIndex, minIndex);
            object2DJPanel1.setSelectedItemIndex(minIndex);
        }
        draggedItem = closestItem;
    }//GEN-LAST:event_object2DJPanel1MousePressed

    private void object2DJPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseReleased
        draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseReleased

    private void jTextFieldMaxXMaxYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMaxXMaxYActionPerformed
        String txt = jTextFieldMaxXMaxY.getText().trim();
        setMaxXMaxYText(txt);
    }//GEN-LAST:event_jTextFieldMaxXMaxYActionPerformed

    public void setViewLimits(double minX, double minY, double maxX, double maxY) {
        String minXMinYString = String.format("%.3f,%.3f", minX, minY);
        jTextFieldMinXMinY.setText(minXMinYString);
        jTextFieldCurrentXY.setText(minXMinYString);
        setMinXMinYText(minXMinYString);
        String maxXMaxYString = String.format("%.3f,%.3f", maxX, maxY);
        jTextFieldMaxXMaxY.setText(maxXMaxYString);
        setMaxXMaxYText(maxXMaxYString);
        this.jCheckBoxAutoscale.setSelected(false);
        object2DJPanel1.setAutoscale(this.jCheckBoxAutoscale.isSelected());
    }

    private void setMaxXMaxYText(String txt) throws NumberFormatException {
        String vals[] = txt.split(",");
        if (vals.length == 2) {
            double newMaxX = Double.parseDouble(vals[0]);
            double newMaxY = Double.parseDouble(vals[1]);
            object2DJPanel1.setMaxX(newMaxX);
            object2DJPanel1.setMaxY(newMaxY);
        } else {
            System.err.println("Bad xmax,ymax = " + txt);
        }
    }

    private void jTextFieldMinXMinYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMinXMinYActionPerformed
        String txt = jTextFieldMinXMinY.getText().trim();
        setMinXMinYText(txt);
    }//GEN-LAST:event_jTextFieldMinXMinYActionPerformed

    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        if (null != visionSocketServer) {
            visionSocketServer.setDebug(this.jCheckBoxDebug.isSelected());
        }
        if (null != visionSocketClient) {
            visionSocketClient.setDebug(this.jCheckBoxDebug.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        this.setItems(Object2DJPanel.EXAMPLES_ITEMS_LIST);
    }//GEN-LAST:event_jButtonResetActionPerformed

    private void jCheckBoxShowRotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowRotationsActionPerformed
        object2DJPanel1.setViewRotationsAndImages(this.jCheckBoxShowRotations.isSelected());
    }//GEN-LAST:event_jCheckBoxShowRotationsActionPerformed

    private void jCheckBoxPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPauseActionPerformed
        if (!this.jCheckBoxPause.isSelected()) {
            if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
                publishCurrentItems();
            }
        }
    }//GEN-LAST:event_jCheckBoxPauseActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
            this.setItems(object2DJPanel1.getItems());
            publishCurrentItems();
        }
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    private void jComboBoxDisplayAxisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDisplayAxisActionPerformed
        object2DJPanel1.setDisplayAxis((DisplayAxis) jComboBoxDisplayAxis.getSelectedItem());
    }//GEN-LAST:event_jComboBoxDisplayAxisActionPerformed

    private File createTempFile(String prefix, String suffix) throws IOException {
        if (null == aprsSystem) {
            return Utils.createTempFile(prefix, suffix);
        }

        return aprsSystem.createTempFile(prefix, suffix);
    }

    public File createTempFile(String prefix, String suffix, File dir) throws IOException {
        if (null == aprsSystem) {
            return Utils.createTempFile(prefix, suffix, dir);
        }
        return aprsSystem.createTempFile(prefix, suffix, dir);
    }

    private boolean isSnapshotsEnabled() {
        if (null == aprsSystem) {
            return true;
        }
        return aprsSystem.isSnapshotsEnabled();
    }

    public void loadFile(File f) throws IOException {

        boolean takeSnapshots = isSnapshotsEnabled();
        if (takeSnapshots) {
            try {
                takeSnapshot(createTempFile("before_loadFile_" + f.getName() + "_", ".PNG"), (PmCartesian) null, "");
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (f.isDirectory()) {
            System.err.println("Can not load file \"" + f + "\" : It is a directory when a text/csv file is expected.");
            return;
        }
        String line = Files.lines(f.toPath()).skip(1).map(String::trim).collect(Collectors.joining(","));
        this.setItems(VisionSocketClient.lineToList(line));
        jTextFieldFilename.setText(f.getCanonicalPath());
        if (takeSnapshots) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    takeSnapshot(createTempFile("loadFile_" + f.getName() + "_", ".PNG"), (PmCartesian) null, "");
                } catch (IOException ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    public void saveFile(File f) throws IOException {
        saveFile(f, getItems());
    }

    private void saveFile(File f, Collection<? extends PhysicalItem> items) throws IOException {

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("name,rotation,x,y,score,type");
            for (PhysicalItem item : items) {
                pw.println(item.getName() + "," + item.getRotation() + "," + item.x + "," + item.y + "," + item.getScore() + "," + item.getType());
            }
        }

    }

    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        String fname = jTextFieldFilename.getText().trim();
        File dir = new File(System.getProperty("user.home"));
        File f = null;
        if (null != fname && fname.length() > 0) {
            f = new File(fname);
            File parentFile = f.getParentFile();
            if (null != parentFile && parentFile.exists()) {
                dir = parentFile;
            }
        }
        JFileChooser chooser = new JFileChooser(dir);
        if (null != f && f.exists()) {
            chooser.setSelectedFile(f);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                loadFile(chooser.getSelectedFile());
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        String fname = jTextFieldFilename.getText().trim();
        File dir = new File(System.getProperty("user.home"));
        File f = null;
        if (null != fname && fname.length() > 0) {
            f = new File(fname);
            File parentFile = f.getParentFile();
            if (null != parentFile && parentFile.exists()) {
                dir = parentFile;
            }
        }
        JFileChooser chooser = new JFileChooser(dir);
        if (null != f && f.exists()) {
            chooser.setSelectedFile(f);
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            try {
                File newFile = chooser.getSelectedFile();
                saveFile(newFile);
                jTextFieldFilename.setText(newFile.getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void jTextFieldCurrentXYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldCurrentXYActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldCurrentXYActionPerformed

    private double currentX = 0.0;
    private double currentY = 0.0;

    private AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    /**
     * Set the value of aprsSystemInterface
     *
     * @param aprsSystemInterface new value of aprsSystemInterface
     */
    public void setAprsSystem(AprsSystem aprsSystemInterface) {
        this.aprsSystem = aprsSystemInterface;
        this.object2DJPanel1.setAprsSystem(aprsSystemInterface);
        setSlotOffsetProvider(aprsSystemInterface);
    }

    @MonotonicNonNull
    private SlotOffsetProvider slotOffsetProvider = null;

    @Nullable
    public SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        this.slotOffsetProvider = slotOffsetProvider;
        this.object2DJPanel1.setSlotOffsetProvider(slotOffsetProvider);
    }

    private void connectCurrentPosition() {
        if (null != aprsSystem) {
            aprsSystem.addCurrentPoseListener(this);
        }
    }

    private void disconnectCurrentPosition() {
        if (null != aprsSystem) {
            aprsSystem.removeCurrentPoseListener(this);
        }
    }

    private void jButtonCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCurrentActionPerformed
        List<PhysicalItem> items = this.getItems();
        int selectedIndex = object2DJPanel1.getSelectedItemIndex();
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            PhysicalItem item = items.get(selectedIndex);
            item.x = currentX;
            item.y = currentY;
        }
    }//GEN-LAST:event_jButtonCurrentActionPerformed

    private void jCheckBoxShowCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowCurrentActionPerformed
        setTrackCurrentPos(jCheckBoxShowCurrent.isSelected());
    }//GEN-LAST:event_jCheckBoxShowCurrentActionPerformed

    private void jCheckBoxSeparateNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSeparateNamesActionPerformed
        object2DJPanel1.setUseSeparateNames(jCheckBoxSeparateNames.isSelected());
    }//GEN-LAST:event_jCheckBoxSeparateNamesActionPerformed

    private void jCheckBoxAutoscaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAutoscaleActionPerformed
        object2DJPanel1.setAutoscale(this.jCheckBoxAutoscale.isSelected());
    }//GEN-LAST:event_jCheckBoxAutoscaleActionPerformed

    private PmCartesian getMinOffset() {
        PmCartesian minDiffCart = new PmCartesian();
        PointType current = aprsSystem.getCurrentPosePoint();
        double min_diff = Double.POSITIVE_INFINITY;
        if (null != current) {
            PmCartesian currentCart = CRCLPosemath.toPmCartesian(current);
            for (PhysicalItem item : this.getItems()) {
                PmCartesian diffCart = item.subtract(currentCart);
                diffCart.z = 0;
                double diffMag = diffCart.mag();
                if (min_diff > diffMag) {
                    min_diff = diffMag;
                    minDiffCart = diffCart;
                }
            }
        }
        return minDiffCart;
    }

    private void jButtonOffsetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOffsetAllActionPerformed
        offsetAll();
    }//GEN-LAST:event_jButtonOffsetAllActionPerformed

    private void jTextFieldSimulationUpdateTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSimulationUpdateTimeActionPerformed
        setSimRefreshMillis(Integer.parseInt(jTextFieldSimulationUpdateTime.getText().trim()));
        setupSimUpdateTimer();
    }//GEN-LAST:event_jTextFieldSimulationUpdateTimeActionPerformed

    private void jCheckBoxSimulationUpdateAsNeededActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulationUpdateAsNeededActionPerformed
        jTextFieldSimulationUpdateTime.setEditable(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        jTextFieldSimulationUpdateTime.setEnabled(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
        setupSimUpdateTimer();
    }//GEN-LAST:event_jCheckBoxSimulationUpdateAsNeededActionPerformed

    private void jTextFieldRotNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRotNoiseActionPerformed
        setRotNoise(Double.parseDouble(jTextFieldRotNoise.getText().trim()));
    }//GEN-LAST:event_jTextFieldRotNoiseActionPerformed

    private void jTextFieldPosNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPosNoiseActionPerformed
        setPosNoise(Double.parseDouble(jTextFieldPosNoise.getText().trim()));
    }//GEN-LAST:event_jTextFieldPosNoiseActionPerformed

    private void jCheckBoxAddPosNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddPosNoiseActionPerformed
        final boolean enable = jCheckBoxSimulated.isSelected() && jCheckBoxAddPosNoise.isSelected();
        jTextFieldPosNoise.setEditable(enable);
        jTextFieldPosNoise.setEnabled(enable);
        jTextFieldRotNoise.setEditable(enable);
        jTextFieldRotNoise.setEnabled(enable);
    }//GEN-LAST:event_jCheckBoxAddPosNoiseActionPerformed

    private void jCheckBoxViewOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxViewOutputActionPerformed
        setShowOutputItems(jCheckBoxViewOutput.isSelected());
    }//GEN-LAST:event_jCheckBoxViewOutputActionPerformed

    public void setShowOutputItems(boolean showOutputItems) {
        object2DJPanel1.setShowOutputItems(showOutputItems);
        if (!showOutputItems) {
            setItemsInternal(getItems());
        } else {
            setOutputItemsInternal(getOutputItems());
        }
        if (showOutputItems != jCheckBoxViewOutput.isSelected()) {
            jCheckBoxViewOutput.setSelected(showOutputItems);
        }
    }

    private void jTextFieldSimDropRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSimDropRateActionPerformed
        setSimulatedDropRate(Double.parseDouble(jTextFieldSimDropRate.getText().trim()));
    }//GEN-LAST:event_jTextFieldSimDropRateActionPerformed

    private void jTextFieldPickupDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPickupDistActionPerformed
        setPickupDist(Double.parseDouble(jTextFieldPickupDist.getText().trim()));
    }//GEN-LAST:event_jTextFieldPickupDistActionPerformed

    private void jTextFieldDropOffThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDropOffThresholdActionPerformed
        setDropOffThreshold(Double.parseDouble(jTextFieldDropOffThreshold.getText().trim()));
    }//GEN-LAST:event_jTextFieldDropOffThresholdActionPerformed

    private void jCheckBoxAddSlotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddSlotsActionPerformed
        object2DJPanel1.setShowAddedSlotPositions(jCheckBoxAddSlots.isSelected());
        refresh(false);
    }//GEN-LAST:event_jCheckBoxAddSlotsActionPerformed

    private void jCheckBoxDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDetailsActionPerformed
        object2DJPanel1.setViewDetails(jCheckBoxDetails.isSelected());
    }//GEN-LAST:event_jCheckBoxDetailsActionPerformed

    private void object2DJPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseClicked
        int x = evt.getX();
        int y = evt.getY();
        int minIndex = -1;
        ClosestItemInfo closestItemInfo = new ClosestItemInfo(x, y, minIndex);
//        PhysicalItem closestItem = closestItemInfo.getClosestItem();
        minIndex = closestItemInfo.getMinIndex();
        if (minIndex >= 0) {
            ListSelectionModel selectModel = jTableItems.getSelectionModel();
            selectModel.setAnchorSelectionIndex(minIndex);
            selectModel.setLeadSelectionIndex(minIndex);
            selectModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            selectModel.setSelectionInterval(minIndex, minIndex);
            object2DJPanel1.setSelectedItemIndex(minIndex);
        }
        this.draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseClicked

    private void object2DJPanel1MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseMoved
        this.draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseMoved

    private void object2DJPanel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseEntered
        this.draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseEntered

    private void object2DJPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseExited
        this.draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseExited

    private void jCheckBoxToolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxToolsActionPerformed
        object2DJPanel1.setShowAddedToolsAndToolHolders(jCheckBoxTools.isSelected());
        refresh(false);
    }//GEN-LAST:event_jCheckBoxToolsActionPerformed

    private javax.swing.@Nullable Timer simUpdateTimer = null;

    private int simRefreshMillis = 50;

    /**
     * Get the value of simRefreshMillis
     *
     * @return the value of simRefreshMillis
     */
    public int getSimRefreshMillis() {
        return simRefreshMillis;
    }

    /**
     * Set the value of simRefreshMillis
     *
     * @param simRefreshMillis new value of simRefreshMillis
     */
    private void setSimRefreshMillis(int simRefreshMillis) {
        if (Integer.parseInt(jTextFieldSimulationUpdateTime.getText().trim()) != simRefreshMillis) {
            jTextFieldSimulationUpdateTime.setText(Integer.toString(simRefreshMillis));
        }
        this.simRefreshMillis = simRefreshMillis;
    }

    private void simUpdateAction(ActionEvent evt) {
        if (jCheckBoxSimulationUpdateAsNeeded.isSelected()) {
            return;
        }
        if (!forceOutputFlag) {
            refresh(false);
        }
    }

    public void stopSimUpdateTimer() {
        if (null != simUpdateTimer) {
            simUpdateTimer.stop();
            simUpdateTimer = null;
        }
    }

    private void setupSimUpdateTimer() {
        if (forceOutputFlag) {
            return;
        }
        stopSimUpdateTimer();
        if (jCheckBoxSimulationUpdateAsNeeded.isSelected()) {
            return;
        }
        simUpdateTimer = new javax.swing.Timer(simRefreshMillis, this::simUpdateAction);
        simUpdateTimer.start();
    }

    private void offsetAll() {
        try {
            PmCartesian minOffset = getMinOffset();
            String offsetString = JOptionPane.showInputDialog("Offset to apply to all items:", minOffset.toString());
            if (offsetString != null) {
                String fa[] = offsetString.split("[{} ,]+");
                double x = 0;
                double y = 0;
                for (String s : fa) {
                    if (s.startsWith("x=")) {
                        x = Double.parseDouble(s.substring(2));
                    } else if (s.startsWith("y=")) {
                        y = Double.parseDouble(s.substring(2));
                    }
                }
                if (fa.length >= 2) {
                    List<PhysicalItem> inItems = getItems();
                    List<PhysicalItem> newItems = new ArrayList<>();
                    for (PhysicalItem item : inItems) {
                        PhysicalItem newItem = newPhysicalItemNameRotXYScoreType(item.getName(), item.getRotation(), item.x - x, item.y - y, item.getScore(), item.getType());
                        newItem.setVisioncycle(item.getVisioncycle());
                        newItems.add(newItem);
                    }
                    setItems(newItems, true);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setTrackCurrentPos(boolean v) {
        if (jCheckBoxShowCurrent.isSelected() != v) {
            jCheckBoxShowCurrent.setSelected(v);
        }
        object2DJPanel1.setShowCurrentXY(v);
        if (v) {
            connectCurrentPosition();
        } else {
            disconnectCurrentPosition();
        }
    }

    private void setMinXMinYText(String txt) throws NumberFormatException {
        String vals[] = txt.split(",");
        if (vals.length == 2) {
            double newMinX = Double.parseDouble(vals[0]);
            double newMinY = Double.parseDouble(vals[1]);
            object2DJPanel1.setMinX(newMinX);
            object2DJPanel1.setMinY(newMinY);
        } else {
            System.err.println("Bad MinX,MinY = " + txt);
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonCurrent;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonOffsetAll;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxAddPosNoise;
    private javax.swing.JCheckBox jCheckBoxAddSlots;
    private javax.swing.JCheckBox jCheckBoxAutoscale;
    private javax.swing.JCheckBox jCheckBoxConnected;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxDetails;
    private javax.swing.JCheckBox jCheckBoxPause;
    private javax.swing.JCheckBox jCheckBoxSeparateNames;
    private javax.swing.JCheckBox jCheckBoxShowCurrent;
    private javax.swing.JCheckBox jCheckBoxShowRotations;
    private javax.swing.JCheckBox jCheckBoxShuffleSimulatedUpdates;
    private javax.swing.JCheckBox jCheckBoxSimulated;
    private javax.swing.JCheckBox jCheckBoxSimulationUpdateAsNeeded;
    private javax.swing.JCheckBox jCheckBoxTools;
    private javax.swing.JCheckBox jCheckBoxViewOutput;
    private javax.swing.JComboBox<DisplayAxis> jComboBoxDisplayAxis;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelHost;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelConnectionsTab;
    private javax.swing.JPanel jPanelOptionsTab;
    private javax.swing.JPanel jPanelSimulationTab;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableItems;
    private javax.swing.JTable jTableTraySlots;
    private javax.swing.JTextArea jTextAreaConnectDetails;
    private javax.swing.JTextField jTextFieldCurrentXY;
    private javax.swing.JTextField jTextFieldDropOffThreshold;
    private javax.swing.JTextField jTextFieldFilename;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldMaxXMaxY;
    private javax.swing.JTextField jTextFieldMinXMinY;
    private javax.swing.JTextField jTextFieldPickupDist;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldPosNoise;
    private javax.swing.JTextField jTextFieldRotNoise;
    private javax.swing.JTextField jTextFieldSimDropRate;
    private javax.swing.JTextField jTextFieldSimulationUpdateTime;
    private aprs.simview.Object2DJPanel object2DJPanel1;
    // End of variables declaration//GEN-END:variables

    public void dispose() {
        if (null != this.visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            visionSocketClient = null;
        }
        if (null != this.visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }
    }

    private File propertiesFile;

    public void setPropertiesFile(File f) {
        this.propertiesFile = f;
    }

    private static String makeShortPath(File f, String str) {
        try {
            if (str.startsWith("..")) {
                return str;
            }
            File strFile = new File(str);
            if (!strFile.exists()) {
                return str;
            }
            String canString = strFile.getCanonicalPath();
            File parentFile = f.getParentFile();
            if (null == parentFile) {
                return str;
            }
            String relString = Paths.get(parentFile.getCanonicalPath()).relativize(Paths.get(canString)).toString();
            if (relString.length() <= canString.length()) {
                return relString;
            }
            return canString;
        } catch (Exception exception) {
            Logger.getLogger(Object2DOuterJPanel.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        return str;
    }

    public void saveProperties() {
        if (null != propertiesFile) {
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                parentFile.mkdirs();
            }
            Properties props = new Properties();
            props.put("--visionport", jTextFieldPort.getText().trim());
            props.put("--visionhost", jTextFieldHost.getText().trim());
            props.put("simulated", Boolean.toString(jCheckBoxSimulated.isSelected()));
            props.put("viewOutput", Boolean.toString(jCheckBoxViewOutput.isSelected()));
            props.put("simulationUpdateAsNeeded", Boolean.toString(jCheckBoxSimulationUpdateAsNeeded.isSelected()));
            props.put("shuffleSimulatedUpdates", Boolean.toString(jCheckBoxShuffleSimulatedUpdates.isSelected()));
            props.put("simulatedDropRate", String.format("%.3f", simulatedDropRate));
            props.put("addPosNoise", Boolean.toString(jCheckBoxAddPosNoise.isSelected()));
            props.put("pickupDist", String.format("%.2f", pickupDist));
            props.put("dropOffThreshold", String.format("%.2f", dropOffThreshold));
            props.put("posNoise", String.format("%.2f", posNoise));
            props.put("rotNoise", String.format("%.2f", rotNoise));
            props.put("simRefreshMillis", Integer.toString(simRefreshMillis));
            props.put("connected", Boolean.toString(jCheckBoxConnected.isSelected()));
            props.put("autoscale", Boolean.toString(jCheckBoxAutoscale.isSelected()));
            props.put("tools", Boolean.toString(jCheckBoxTools.isSelected()));
            props.put("trackcurrentpos", Boolean.toString(jCheckBoxShowCurrent.isSelected()));
            props.put("showrotations", Boolean.toString(jCheckBoxShowRotations.isSelected()));
            props.put("viewDetails", Boolean.toString(jCheckBoxDetails.isSelected()));
            props.put("separatenames", Boolean.toString(jCheckBoxSeparateNames.isSelected()));
            props.put("xmaxymax", jTextFieldMaxXMaxY.getText().trim());
            props.put("xminymin", jTextFieldMinXMinY.getText().trim());
            if (reverseFlag) {
                this.reverseDataFileString = jTextFieldFilename.getText().trim();
            } else {
                this.dataFileString = jTextFieldFilename.getText().trim();
            }
            if (null != reverseDataFileString && reverseDataFileString.length() > 0) {
                String datafileShort = makeShortPath(propertiesFile, reverseDataFileString);
                props.put("reverse_datafile", datafileShort);
            }
            if (null != dataFileString && dataFileString.length() > 0) {
                String datafileShort = makeShortPath(propertiesFile, dataFileString);
                props.put("datafile", datafileShort);
            }
            props.put("reverseFlag", Boolean.toString(reverseFlag));
            DisplayAxis displayAxis = object2DJPanel1.getDisplayAxis();
            props.put("displayAxis", displayAxis.toString());
            List<PhysicalItem> l = getItems();
            if (null != l && l.size() > 0) {
                props.put(ITEMS_PROPERTY_NAME, VisionSocketServer.listToLine(l));
            }
//            try (FileWriter fw = new FileWriter(propertiesFile)) {
//                props.store(fw, "");
//            }
            Utils.saveProperties(propertiesFile, props);
        }
    }
    private static final String ITEMS_PROPERTY_NAME = "items";

    private boolean reverseFlag = false;

    /**
     * Get the value of reverseFlag
     *
     * @return the value of reverseFlag
     */
    public boolean isReverseFlag() {
        return reverseFlag;
    }

    /**
     * Set the value of reverseFlag
     *
     * @param reverseFlag new value of reverseFlag
     */
    public void setReverseFlag(boolean reverseFlag) {
        this.reverseFlag = reverseFlag;
    }

    public void loadProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String itemsLine = props.getProperty(ITEMS_PROPERTY_NAME);
            if (null != itemsLine && itemsLine.length() > 0) {
                List<PhysicalItem> l = VisionSocketClient.lineToList(itemsLine);
                if (null != l && l.size() > 0) {
                    setItems(l);
                }
            }
            String portString = props.getProperty("--visionport");
            try {
                if (null != portString && portString.length() > 0) {
                    int port = Integer.parseInt(portString);
                    jTextFieldPort.setText(Integer.toString(port));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String hostString = props.getProperty("--visionhost");
            try {
                if (null != hostString && hostString.length() > 0) {
                    jTextFieldHost.setText(hostString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String simulationUpdateAsNeededString = props.getProperty("simulationUpdateAsNeeded");
            if (null != simulationUpdateAsNeededString && simulationUpdateAsNeededString.length() > 0) {
                boolean simulationUpdateAsNeeded = Boolean.valueOf(simulationUpdateAsNeededString);
                jCheckBoxSimulationUpdateAsNeeded.setSelected(simulationUpdateAsNeeded);
                jTextFieldSimulationUpdateTime.setEditable(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
                jTextFieldSimulationUpdateTime.setEnabled(jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected());
            }

            String shuffleSimulatedUpdatesString = props.getProperty("shuffleSimulatedUpdates");
            if (null != shuffleSimulatedUpdatesString && shuffleSimulatedUpdatesString.length() > 0) {
                boolean shuffleSimulatedUpdates = Boolean.valueOf(shuffleSimulatedUpdatesString);
                jCheckBoxShuffleSimulatedUpdates.setSelected(shuffleSimulatedUpdates);
            }

            String viewOutputString = props.getProperty("viewOutput");
            if (null != viewOutputString && viewOutputString.length() > 0) {
                boolean viewOutput = Boolean.valueOf(viewOutputString);
                jCheckBoxViewOutput.setSelected(viewOutput);
            }

            String addPosNoiseString = props.getProperty("addPosNoise");
            if (null != addPosNoiseString && addPosNoiseString.length() > 0) {
                boolean addPosNoise = Boolean.valueOf(addPosNoiseString);
                jCheckBoxAddPosNoise.setSelected(addPosNoise);
            }
            String simulatedDropRateString = props.getProperty("simulatedDropRate");
            if (null != simulatedDropRateString && simulatedDropRateString.length() > 0) {
                double simDropRate = Double.parseDouble(simulatedDropRateString);
                if (simDropRate < 0.001) {
                    simDropRate = 0;
                }
                setSimulatedDropRate(simDropRate);
            }

            String pickupDistString = props.getProperty("pickupDist");
            if (null != pickupDistString && pickupDistString.length() > 0) {
                double simPickupDist = Double.parseDouble(pickupDistString);
                setPickupDist(simPickupDist);
            }

            String dropOffThresholdString = props.getProperty("dropOffThreshold");
            if (null != dropOffThresholdString && dropOffThresholdString.length() > 0) {
                double simDropOffThreshold = Double.parseDouble(dropOffThresholdString);
                setDropOffThreshold(simDropOffThreshold);
            }

            String posNoiseString = props.getProperty("posNoise");
            if (null != posNoiseString && posNoiseString.length() > 0) {
                double simPosNoise = Double.parseDouble(posNoiseString);
                setPosNoise(simPosNoise);
            }

            String rotNoiseString = props.getProperty("rotNoise");
            if (null != rotNoiseString && rotNoiseString.length() > 0) {
                double simRotNoise = Double.parseDouble(rotNoiseString);
                setRotNoise(simRotNoise);
            }
            String simRefreshMillisString = props.getProperty("simRefreshMillis");
            if (null != simRefreshMillisString && simRefreshMillisString.length() > 0) {
                int simRefreshMs = Integer.parseInt(simRefreshMillisString);
                setSimRefreshMillis(simRefreshMs);
            }

            String simulatedString = props.getProperty("simulated");
            if (null != simulatedString && simulatedString.length() > 0) {
                boolean simulated = Boolean.valueOf(simulatedString);
                jCheckBoxSimulated.setSelected(simulated);
                setSimulatedInternal(simulated);
            }

            String autoscaleString = props.getProperty("autoscale");
            if (null != autoscaleString && autoscaleString.length() > 0) {
                boolean autoscale = Boolean.valueOf(autoscaleString);
                jCheckBoxAutoscale.setSelected(autoscale);
                object2DJPanel1.setAutoscale(autoscale);
            }
            String toolsString = props.getProperty("tools");
            if (null != toolsString && toolsString.length() > 0) {
                boolean tools = Boolean.valueOf(toolsString);
                jCheckBoxTools.setSelected(tools);
                object2DJPanel1.setShowAddedToolsAndToolHolders(tools);
            }
            reverseDataFileString = props.getProperty("reverse_datafile");
            dataFileString = props.getProperty("datafile");

            String xmaxymaxString = props.getProperty("xmaxymax");
            if (null != xmaxymaxString) {
                setMaxXMaxYText(xmaxymaxString);
                jTextFieldMaxXMaxY.setText(xmaxymaxString);
            }
            String xminyminString = props.getProperty("xminymin");
            if (null != xminyminString) {
                setMinXMinYText(xminyminString);
                jTextFieldMinXMinY.setText(xminyminString);
            }
            String connectedString = props.getProperty("connected");
            if (null != connectedString && connectedString.length() > 0) {
                boolean connected = Boolean.valueOf(connectedString);
                jCheckBoxConnected.setSelected(connected);
                if (connected) {
                    connect();
                }
            }
            if (jCheckBoxSimulated.isSelected() || !jCheckBoxConnected.isSelected()) {
                if (needReloadDataFile()) {
                    reloadDataFile();
                }
            }
            String displayAxisString = props.getProperty("displayAxis");
            if (displayAxisString != null && displayAxisString.length() > 0) {
                boolean axisFound = false;
                for (DisplayAxis da : DisplayAxis.values()) {
                    if (Objects.equals(da.toString(), displayAxisString)) {
                        DisplayAxis displayAxis = da;
                        jComboBoxDisplayAxis.setSelectedItem(displayAxis);
                        object2DJPanel1.setDisplayAxis(displayAxis);
                        axisFound = true;
                        break;
                    }
                }
                if (!axisFound) {
                    DisplayAxis displayAxis = DisplayAxis.valueOf(displayAxisString);
                    jComboBoxDisplayAxis.setSelectedItem(displayAxis);
                    object2DJPanel1.setDisplayAxis(displayAxis);
                }
            }
            String trackCurrentPosString = props.getProperty("trackcurrentpos");
            if (trackCurrentPosString != null && trackCurrentPosString.length() > 0) {
                boolean trackCurrentPos = Boolean.valueOf(trackCurrentPosString);
                jCheckBoxShowCurrent.setSelected(trackCurrentPos);
                this.setTrackCurrentPos(trackCurrentPos);
            }
            //showrotations
            String showRotationsString = props.getProperty("showrotations");
            if (showRotationsString != null && showRotationsString.length() > 0) {
                boolean showRotations = Boolean.valueOf(showRotationsString);
                jCheckBoxShowRotations.setSelected(showRotations);
                object2DJPanel1.setViewRotationsAndImages(showRotations);
            }
            String viewDetailsString = props.getProperty("viewDetails");
            if (viewDetailsString != null && viewDetailsString.length() > 0) {
                boolean viewDetails = Boolean.valueOf(viewDetailsString);
                jCheckBoxDetails.setSelected(viewDetails);
                object2DJPanel1.setViewDetails(viewDetails);
            }
            String useSeparateNamesString = props.getProperty("separatenames");
            if (useSeparateNamesString != null && useSeparateNamesString.length() > 0) {
                boolean useSeparateNames = Boolean.valueOf(useSeparateNamesString);
                jCheckBoxSeparateNames.setSelected(useSeparateNames);
                object2DJPanel1.setUseSeparateNames(useSeparateNames);
            }
        }
    }

    public boolean isSimulated() {
        return jCheckBoxSimulated.isSelected();
    }

    public boolean isConnected() {
        return jCheckBoxConnected.isSelected();
    }

    @Nullable
    private String dataFileString = null;
    @Nullable
    private String reverseDataFileString = null;
    @Nullable
    private volatile String loadedDataFileString = null;

    @Nullable
    private String getCurrentDataFileString() {
        return reverseFlag ? this.reverseDataFileString : this.dataFileString;
    }

    private boolean needReloadDataFile() {
        String currentDataFileString = getCurrentDataFileString();
        if (null == currentDataFileString) {
            return false;
        }
        if (currentDataFileString.length() < 1) {
            return false;
        }
        return !Objects.equals(currentDataFileString, loadedDataFileString);
    }

    public void reloadDataFile() throws IOException {
        String currentDataFileString = getCurrentDataFileString();
        if (null != currentDataFileString && currentDataFileString.length() > 0) {
            File f = new File(currentDataFileString);
            if (f.exists() && f.canRead() && !f.isDirectory()) {
                jTextFieldFilename.setText(f.getCanonicalPath());
                loadFile(f);
            } else {
                File parentFile = propertiesFile.getParentFile();
                if (null == parentFile) {
                    throw new IllegalStateException("currentDataFileString = " + currentDataFileString + " does not exist and propertiesFile=" + propertiesFile + " has no parent");
                }
                String fullPath = parentFile.toPath().resolve(currentDataFileString).normalize().toString();
                f = new File(fullPath);
                if (f.exists() && f.canRead()) {
                    jTextFieldFilename.setText(f.getCanonicalPath());
                    loadFile(f);
                } else {
                    String fullPath2 = parentFile.toPath().resolveSibling(currentDataFileString).normalize().toString();
                    f = new File(fullPath2);
                    if (f.exists() && f.canRead()) {
                        jTextFieldFilename.setText(f.getCanonicalPath());
                        loadFile(f);
                    }
                }
            }
            loadedDataFileString = currentDataFileString;
        }
    }

    public File getPropertiesFile() {
        return propertiesFile;
    }

    private volatile long lastVisionUpdateTime = System.currentTimeMillis();

    @Override
    public void visionClientUpdateReceived(List<PhysicalItem> l, String line) {
        long now = System.currentTimeMillis();

        String detailsMessage = null;
        if (null != visionSocketClient) {
            detailsMessage
                    = "size=" + l.size() + "\n"
                    + "count=" + visionSocketClient.getLineCount() + "\n"
                    + "skipped=" + visionSocketClient.getSkippedLineCount() + "\n"
                    + "ignored=" + visionSocketClient.getIgnoreCount() + "\n"
                    + "consecutive=" + visionSocketClient.getConsecutiveIgnoreCount() + "\n"
                    + "time=" + (now - lastVisionUpdateTime) + "\n";
        }
        final String finalDetailsMessage = detailsMessage;
        lastVisionUpdateTime = now;
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            setItems(l);
            if (null != detailsMessage) {
                jTextAreaConnectDetails.setText(detailsMessage);
            }
        } else {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setItems(l);
                    if (null != finalDetailsMessage) {
                        jTextAreaConnectDetails.setText(finalDetailsMessage);
                    }
                }
            });
        }
    }

    private volatile boolean lastIsHoldingObjectExpected = false;
    private volatile int captured_item_index = -1;

    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) {
        this.object2DJPanel1.takeSnapshot(f, itemsToPaint, w, h);
        saveSnapshotCsv(f, itemsToPaint);
    }

    private void saveSnapshotCsv(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        try {
            File csvDir = new File(f.getParentFile(), "csv");
            csvDir.mkdirs();
            saveFile(new File(csvDir, f.getName() + ".csv"), itemsToPaint);
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        this.object2DJPanel1.takeSnapshot(f, itemsToPaint);
        saveSnapshotCsv(f, itemsToPaint);
    }

    private volatile long lastIsHoldingObjectExpectedTime = -1;
    private volatile long lastNotIsHoldingObjectExpectedTime = -1;

    private double pickupDist = 5.0;

    /**
     * Get the value of pickupDist
     *
     * @return the value of pickupDist
     */
    public double getPickupDist() {
        return pickupDist;
    }

    /**
     * Set the value of pickupDist
     *
     * @param pickupDist new value of pickupDist
     */
    private void setPickupDist(double pickupDist) {
        updateTextFieldDouble(pickupDist, jTextFieldPickupDist, 0.005);
        this.pickupDist = pickupDist;
    }

    private double dropOffThreshold = 25.0;

    /**
     * Get the value of dropOffThreshold
     *
     * @return the value of dropOffThreshold
     */
    public double getDropOffThreshold() {
        return dropOffThreshold;
    }

    /**
     * Set the value of dropOffThreshold
     *
     * @param dropOffThreshold new value of dropOffThreshold
     */
    private void setDropOffThreshold(double dropOffThreshold) {
        updateTextFieldDouble(pickupDist, jTextFieldDropOffThreshold, 0.005);
        this.dropOffThreshold = dropOffThreshold;
    }

    static class DistIndex {

        double dist;
        int index;

        DistIndex(double dist, int index) {
            this.dist = dist;
            this.index = index;
        }

        @Override
        public String toString() {
            return "DistIndex{" + "dist=" + dist + ", index=" + index + '}';
        }

    }

    public double getClosestRobotPartDistance() {
        if (null == aprsSystem) {
            return Double.POSITIVE_INFINITY;
        }
        PointType currentPoint = aprsSystem.getCurrentPosePoint();
        if (null == currentPoint) {
            return Double.POSITIVE_INFINITY;
        }
        return getClosestUncorrectedDistance(currentPoint);
    }

    private double getClosestUncorrectedDistance(PointType ptIn) {
        PointType uncorrectedPoint = aprsSystem.reverseCorrectPoint(ptIn);
        List<PhysicalItem> l = new ArrayList<>(getItems());
        return getClosestDistanceIndex(uncorrectedPoint.getX(), uncorrectedPoint.getY(), l).dist;
    }

    private volatile DistIndex lastClosestDistanceIndexRet;
    private volatile double lastClosestDistanceIndexX;
    private volatile double lastClosestDistanceIndexY;
    private volatile List<PhysicalItem> lastClosestDistanceIndexList;
    
    private DistIndex getClosestDistanceIndex(double x, double y, List<PhysicalItem> l) {

        double min_dist = Double.POSITIVE_INFINITY;
        int min_dist_index = -1;
        for (int i = 0; i < l.size(); i++) {
            if (i == captured_item_index) {
                continue;
            }
            PhysicalItem item = l.get(i);
            if (!item.getType().equals("P")) {
                continue;
            }
            double dist = item.dist(x, y);
            if (dist < min_dist) {
                min_dist_index = i;
                min_dist = dist;
            }
        }
        DistIndex ret = new DistIndex(min_dist, min_dist_index);
        lastClosestDistanceIndexX =x;
        lastClosestDistanceIndexY= y;
        this.lastClosestDistanceIndexRet = ret;
        lastClosestDistanceIndexList = l;
        return ret;
    }

    private class PoseUpdateHistoryItem {

        final CRCLStatusType stat;
        final CRCLCommandType cmd;
        final boolean isHoldingObjectExpected;
        final long time;
        final Point2D.@Nullable Double capturedPartPoint;
        final int captured_item_index;
        final DistIndex di;
        final PhysicalItem closestItem;
        final long statReceiveTime;

        PoseUpdateHistoryItem(
                CRCLStatusType stat, 
                CRCLCommandType cmd, 
                boolean isHoldingObjectExpected, 
                long time,
                Point2D.@Nullable Double capturedPartPoint,
                int captured_item_index,
                DistIndex di,
                PhysicalItem closestItem,
                long statRecieveTime) {
            this.stat = stat;
            this.isHoldingObjectExpected = isHoldingObjectExpected;
            this.time = time;
            this.cmd = cmd;
            this.capturedPartPoint = capturedPartPoint;
            this.captured_item_index = captured_item_index;
            this.di = di;
            this.closestItem = closestItem;
            this.statReceiveTime = statRecieveTime;
        }

        @Override
        public String toString() {
            return "\nPoseUpdateHistoryItem{ cmd=" + CRCLSocket.commandToSimpleString(cmd) + ", isHoldingObjectExpected=" + isHoldingObjectExpected + ", time=" + (time - poseUpdateHistoryTime) + ", stat=" + CRCLSocket.statusToPrettyString(stat) + '}';
        }
    }
    
     
    private static String fmtDouble(double d) {
        return String.format("%.3f",d);
    }

    
    private Object[] poseUpdateHistoryRecordItems(PoseUpdateHistoryItem item) throws IOException {
        File cmdFile = null;
        String cmdClassName = null;
        String cmdFileName = null;
        CRCLCommandType cmd = item.cmd;
        long cmdId = -1;
        if(null != cmd) {
            cmdClassName = cmd.getClass().getSimpleName();
            if(cmdClassName.startsWith("crcl.base.")) {
                cmdClassName = cmdClassName.substring("crcl.base.".length());
            }
            cmdFile = aprsSystem.logCrclCommand("puh_"+cmdClassName, cmd);
            cmdFileName = (cmdFile != null)?cmdFile.getCanonicalPath():null;
            cmdId = cmd.getCommandID();
        }
        CRCLStatusType stat = item.stat;
        PointType point = CRCLPosemath.getPoint(stat);
        File statFile = null;
        String statFileName =null;
        long statCmdId = -1;
        CommandStateEnumType state = null;
        if(null != stat) {
            CommandStatusType cs = stat.getCommandStatus();
            if(null != cs) {
                statCmdId = cs.getCommandID();
                state = cs.getCommandState();
            }
            statFile = aprsSystem.logCrclStatus("puh_", stat);
            statFileName = (cmdFile != null)?statFile.getCanonicalPath():null;
        }
        
        return new Object[] {
            Utils.getTimeString(item.time),
            item.time-item.statReceiveTime,
            fmtDouble(point.getX()),
            fmtDouble(point.getY()),
            fmtDouble(point.getZ()),
            item.isHoldingObjectExpected,
            item.di.index,
            fmtDouble(item.di.dist),
            item.captured_item_index,
            (item.closestItem!=null)?fmtDouble(item.closestItem.x):null,
            (item.closestItem!=null)?fmtDouble(item.closestItem.y):null,
            (item.closestItem!=null)?item.closestItem.getFullName():null,
            cmdId,
            statCmdId,
            state,
            (null != item.cmd)?item.cmd.getName():null,
            cmdFileName,
            statFileName
        };
    }
    
    private static final String[] POSE_UPDATE_HISTORY_HEADER =
            new String[]{
                "time",
                "timeSinceStat",
                "X",
                "Y",
                "Z",
                "isHoldingObjectExpected",
                "min_index",
                "min_distance",
                "captured_item_index",
                "closest_item_x",
                "closest_item_y",
                "closest_item_name",
                "cmdId",
                "statEchoCmdId",
                "state",
                "cmdName",
                "cmdFile",
                "statFileName"
            };
    
    
    private File printPoseUpdateHistory(String err) throws IOException {
        File f = createTempFile("puh_"+err, ".csv");
        try(CSVPrinter printer = new CSVPrinter(new FileWriter(f), CSVFormat.DEFAULT.withHeader(POSE_UPDATE_HISTORY_HEADER))){
            for(PoseUpdateHistoryItem item : poseUpdateHistory) {
                printer.printRecord(poseUpdateHistoryRecordItems(item));
            }
        }
        return f;
    }
    
    private final ConcurrentLinkedDeque<PoseUpdateHistoryItem> poseUpdateHistory
            = new ConcurrentLinkedDeque<>();

    private volatile long poseUpdateHistoryTime = System.currentTimeMillis();

    private static Random r = new Random();

    private volatile javax.swing.@Nullable Timer timer = null;

    @Nullable
    private volatile PoseUpdateHistoryItem lastDropUpdate = null;

    @Override
    public void handlePoseUpdate(
            PendantClientJPanel panel,
            CRCLStatusType stat, 
            CRCLCommandType cmd, 
            boolean isHoldingObjectExpected, 
            long statRecievTime) {
        if (!jCheckBoxShowCurrent.isSelected()) {
            return;
        }
        PoseType pose = CRCLPosemath.getPose(stat);
        PointType ptIn = pose.getPoint();

        boolean takeSnapshots = isSnapshotsEnabled();
        PointType uncorrectedPoint = aprsSystem.reverseCorrectPoint(ptIn);
        currentX = uncorrectedPoint.getX();
        currentY = uncorrectedPoint.getY();
        jTextFieldCurrentXY.setText(String.format("%.3f,%.3f", currentX, currentY));
        object2DJPanel1.setCurrentX(currentX);
        object2DJPanel1.setCurrentY(currentY);
        object2DJPanel1.setEndEffectorClosed(isHoldingObjectExpected);
        List<PhysicalItem> l = new ArrayList<>(getItems());
        DistIndex di = getClosestDistanceIndex(currentX, currentY, l);
        double min_dist = di.dist;
        int min_dist_index = di.index;

        long time = System.currentTimeMillis();
        poseUpdateHistoryTime = time;
        PoseUpdateHistoryItem currentUpdate
                = new PoseUpdateHistoryItem(
                        stat, 
                        cmd, 
                        isHoldingObjectExpected, 
                        time,
                        object2DJPanel1.getCapturedPartPoint(),
                        captured_item_index,
                        di,
                        (min_dist_index >=0 && min_dist_index < l.size())?l.get(min_dist_index):null,
                        statRecievTime
                );
        poseUpdateHistory.add(currentUpdate);
        if (poseUpdateHistory.size() > 25) {
            poseUpdateHistory.removeFirst();
        }
        

        if (isHoldingObjectExpected) {
            lastIsHoldingObjectExpectedTime = time;
        } else {
            lastNotIsHoldingObjectExpectedTime = time;
        }

        if (isHoldingObjectExpected && !lastIsHoldingObjectExpected) {
            object2DJPanel1.setCapturedPartPoint(new Point2D.Double(currentX, currentY));
        } else if (!isHoldingObjectExpected && lastIsHoldingObjectExpected) {
            object2DJPanel1.setCapturedPartPoint(null);
        }
        if (this.jCheckBoxSimulated.isSelected()) {
            if (min_dist < dropOffThreshold
                    && lastIsHoldingObjectExpected && !isHoldingObjectExpected
                    && min_dist_index != captured_item_index) {
                PhysicalItem captured_item = (captured_item_index >= 0 && captured_item_index < l.size()) ? l.get(captured_item_index) : null;
                System.out.println("captured_item = " + captured_item);
                System.out.println("(time-lastIsHoldingObjectExpectedTime) = " + (time - lastIsHoldingObjectExpectedTime));
                System.out.println("(time-lastNotIsHoldingObjectExpectedTime) = " + (time - lastNotIsHoldingObjectExpectedTime));
                String errString
                        = "Dropping item on to another item min_dist=" + min_dist
                        + ", min_dist_index=" + min_dist_index
                        + ", captured_item_index=" + captured_item_index
                        + ", bottom item at min_dist_index =" + l.get(min_dist_index)
                        + ", captured_item  =" + captured_item;
                try {
                    printHandlePoseErrorInfo(errString, stat, pose, cmd);
                } catch (IOException ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                this.aprsSystem.setTitleErrorString(errString);
                this.aprsSystem.pause();
            }
            if (isHoldingObjectExpected && !lastIsHoldingObjectExpected) {
                if (min_dist < pickupDist && min_dist_index >= 0) {
                    captured_item_index = min_dist_index;
                    if (true) {
                        try {
                            System.out.println(aprsSystem.getRunName() + " : Captured item with index " + captured_item_index + " at " + currentX + "," + currentY);
                            if (takeSnapshots) {
                                takeSnapshot(createTempFile("capture_" + captured_item_index + "_at_" + currentX + "_" + currentY + "_", ".PNG"), (PmCartesian) null, "");
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    try {
                        System.out.println("di = " + di);
                        if (null != di && di.index >= 0 && di.index < l.size()) {
                            PhysicalItem closestItem = l.get(di.index);
                            System.out.println("closestItem = " + closestItem);
                            System.out.println("closestItem.getFullName() = " + closestItem.getFullName());
                            System.out.println("closestItem.(x,y) = " + closestItem.x + "," + closestItem.y);
                        }
                        String err ="failed_to_capture_part_at_" + currentX + "_" + currentY + "_";
                        printHandlePoseErrorInfo(err, stat, pose, cmd);
                        if (takeSnapshots) {
                            takeSnapshot(createTempFile(err, ".PNG"), (PmCartesian) null, "");
                        }
                        System.err.println("Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);

                    } catch (Exception ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    this.aprsSystem.setTitleErrorString("Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);
                }
            } else if (!isHoldingObjectExpected && lastIsHoldingObjectExpected) {
                if (true) {
                    System.out.println(aprsSystem.getRunName() + " : Dropping item with index " + captured_item_index + " at " + currentX + "," + currentY);
                    if (takeSnapshots) {
                        try {
                            takeSnapshot(createTempFile("dropping_" + captured_item_index + "_at_" + currentX + "_" + currentY + "_", ".PNG"), (PmCartesian) null, "");
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                if (captured_item_index < 0) {
                    String err = "Should be dropping item but no item captured";
                    try {
                        printHandlePoseErrorInfo(err, stat, pose, cmd);
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println("lastDropUpdate = " + lastDropUpdate);
                    this.aprsSystem.setTitleErrorString(err);
                }
            }
        }
        if (!isHoldingObjectExpected && captured_item_index >= 0) {
            lastDropUpdate = currentUpdate;
            captured_item_index = -1;
        }
        if (this.jCheckBoxSimulated.isSelected()) {
            if (captured_item_index >= 0 && captured_item_index < l.size()) {
                PhysicalItem item = l.get(captured_item_index);
                item.x = currentX;
                item.y = currentY;
                setItems(l, (isHoldingObjectExpected != lastIsHoldingObjectExpected) && jCheckBoxSimulationUpdateAsNeeded.isSelected());
            } else if (isHoldingObjectExpected != lastIsHoldingObjectExpected) {
                setItems(l);
            }
        }
        lastIsHoldingObjectExpected = isHoldingObjectExpected;
    }

    private void printHandlePoseErrorInfo(String err, CRCLStatusType stat, PoseType pose, CRCLCommandType cmd) throws IOException {
        System.out.println("poseUpdateHistory = " + printPoseUpdateHistory(err));
        System.out.println("statFile = " + aprsSystem.logCrclStatus(err, stat));
        System.out.println("pose = " + CRCLPosemath.toString(pose));
        System.out.println("cmd = " + CRCLSocket.commandToSimpleString(cmd));
        System.out.println("cmdFile = " + aprsSystem.logCrclCommand(err, cmd));
    }

    private class ClosestItemInfo {

        private final int x;
        private final int y;
        @Nullable
        private PhysicalItem closestItem;
        private int minIndex;

        ClosestItemInfo(int x, int y, int minIndex) {
            this.x = x;
            this.y = y;
            this.minIndex = minIndex;
            List<PhysicalItem> items = Object2DOuterJPanel.this.getItems();
            double scale = object2DJPanel1.getScale();
            double min_x = object2DJPanel1.getMinX();
            double max_y = object2DJPanel1.getMaxY();
            double minDist = Double.POSITIVE_INFINITY;
            PhysicalItem localClosestItem = null;
            for (int i = 0; i < items.size(); i++) {
                PhysicalItem item = items.get(i);
                double rel_x = (item.x - min_x) * scale + 15;
                double rel_y = (max_y - item.y) * scale + 20;

                double diff_x = rel_x - x;
                double diff_y = rel_y - y;
                double dist = Math.sqrt(diff_x * diff_x + diff_y * diff_y);
                if (dist < minDist) {
                    minDist = dist;
                    localClosestItem = item;
                    this.minIndex = i;
                }
            }
            if (null != localClosestItem) {
                if (!insideItem(localClosestItem, x, y)) {
                    System.err.println("insideItem(" + localClosestItem + "," + x + "," + y + ") failed");
                    boolean recheck = insideItem(localClosestItem, x, y);
                    localClosestItem = null;
                    this.minIndex = -1;
                }
            }
            this.closestItem = localClosestItem;
        }

        @Nullable
        PhysicalItem getClosestItem() {
            return closestItem;
        }

        int getMinIndex() {
            return minIndex;
        }
    }
}
