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

import aprs.cachedcomponents.CachedCheckBox;
import aprs.cachedcomponents.CachedTextField;
import aprs.conveyor.ConveyorPosition;
import aprs.system.AprsSystem;
import aprs.misc.SlotOffsetProvider;
import aprs.misc.Utils;
import static aprs.misc.Utils.autoResizeTableColWidths;
import aprs.database.PhysicalItem;
import aprs.database.DbSetupBuilder;
import aprs.database.Part;
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
import crcl.ui.XFutureVoid;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import static java.lang.Double.parseDouble;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import java.util.Collection;
import java.util.Enumeration;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.swing.JDialog;
import javax.swing.table.TableModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DOuterJPanel extends javax.swing.JPanel implements Object2DJFrameInterface, VisionSocketClient.VisionSocketClientListener, PendantClientJPanel.CurrentPoseListener {

    @UIEffect
    public static List<PhysicalItem> showAndModifyData(List<PhysicalItem> itemsIn, SlotOffsetProvider sop, double minX, double minY, double maxX, double maxY) {
        JDialog diag = new JDialog();
        diag.setModal(true);
        Object2DOuterJPanel panel = new Object2DOuterJPanel();
        panel.setViewLimits(minX, minY, maxX, maxY);
        panel.setSlotOffsetProvider(sop);
        panel.setItems(itemsIn);
        panel.setSimulated(true);
        diag.add(panel);
        diag.pack();
        diag.setVisible(true);
        return panel.getItems();
    }

    private final List<Consumer<List<PhysicalItem>>> setItemsListeners
            = Collections.synchronizedList(new ArrayList<>());

    public void addSetItemsListener(Consumer<List<PhysicalItem>> listener) {
        setItemsListeners.add(listener);
    }

    public void removeSetItemsListener(Consumer<List<PhysicalItem>> listener) {
        setItemsListeners.remove(listener);
    }

    private final AtomicInteger notifyItemsTableCount = new AtomicInteger();
    private final AtomicLong notifyItemsTableTime = new AtomicLong();
    private final AtomicLong notifyItemsTableMaxTime = new AtomicLong();

    private void notifySetItemsListeners(List<PhysicalItem> itemsList) {
        if (setItemsListeners.isEmpty()) {
            return;
        }
        if (captured_item_index > 0) {
            return;
        }
        if (null != draggedItem) {
            return;
        }
        if (null != draggedItemsList && !draggedItemsList.isEmpty()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        int c = notifyItemsTableCount.incrementAndGet();
        List<Consumer<List<PhysicalItem>>> notifyList
                = new ArrayList<>(setItemsListeners);
        List<PhysicalItem> itemsListCopy
                = Collections.unmodifiableList(new ArrayList<>(itemsList));
        for (Consumer<List<PhysicalItem>> consumer : notifyList) {
            consumer.accept(itemsListCopy);
        }
        if (debugTimes) {
            long endTime = System.currentTimeMillis();
            long diff = endTime - startTime;
            long total = notifyItemsTableTime.addAndGet(diff);
            long max = notifyItemsTableMaxTime.getAndAccumulate(diff, Math::max);
            System.out.println("notifyItemsTable count = " + c);
            System.out.println("notifyItemsTable max = " + max);
            System.out.println("notifyItemsTable avg = " + total / c);
        }
    }

    private volatile boolean debugTimes = false;

//    public boolean isDebugTimes() {
//        return debugTimes;
//    }
//
//    public void setDebugTimes(boolean debugTimes) {
//        this.debugTimes = debugTimes;
//        object2DJPanel1.setDebugTimes(debugTimes);
//    }
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
    public void takeSnapshot(File f, @Nullable PointType point, String label) {
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
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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

    private final CachedCheckBox simulatedCachedCheckBox;
    private final CachedTextField filenameCachedTextField;
    private final CachedCheckBox pauseCachedCheckBox;

    public void refresh(boolean loadFile) {
        try {
            if (simulatedCachedCheckBox.isSelected()) {
                boolean fileLoaded = false;
                if (loadFile) {
                    String fname = filenameCachedTextField.getText().trim();
                    File f = new File(fname);
                    if (f.exists() && f.canRead()) {
                        try {
                            loadFile(f);
                            fileLoaded = true;
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                        }
                    }
                }
                if (!fileLoaded && null != visionSocketServer && !pauseCachedCheckBox.isSelected()) {
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

    public void loadFile(File f) throws IOException {
        loadFile(f, handleRotationEnum == HandleRotationEnum.DEGREES, handleRotationEnum == HandleRotationEnum.IGNORE);
    }

    @Override
    public void setItems(List<PhysicalItem> items) {
        setItems(items, !pauseCachedCheckBox.isSelected());
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
        XFutureVoid future = Utils.runOnDispatchThread(() -> {
            setItemsInternal(items);
            settingItems = false;
        });
        if (captured_item_index > 0) {
            return;
        }
        if (null != draggedItem) {
            return;
        }
        if (null != draggedItemsList && !draggedItemsList.isEmpty()) {
            return;
        }
        if (publish) {
            VisionSocketServer srv = this.visionSocketServer;
            if (null != srv && !pauseCachedCheckBox.isSelected()) {
                srv.publishList(items);
            }
        }
        future.thenRun(() -> notifySetItemsListeners(items));
    }

    public void setOutputItems(List<PhysicalItem> items) {
        settingItems = true;
        Utils.runOnDispatchThread(() -> {
            setOutputItemsInternal(items);
            settingItems = false;
        });
    }

    @UIEffect
    private void setItemsInternal(List<PhysicalItem> items) {
        if (null != aprsSystem && aprsSystem.isVisionToDbConnected()) {
            double visionToDBRotationOffset = aprsSystem.getVisionToDBRotationOffset();
            if (Math.abs(object2DJPanel1.getRotationOffset() - visionToDBRotationOffset) > 0.001) {
                jTextFieldRotationOffset.setText(String.format("%.1f", toDegrees(visionToDBRotationOffset)));
            }
            object2DJPanel1.setRotationOffset(visionToDBRotationOffset);
        }
        object2DJPanel1.setItems(items);
        updateItemsTableOnDisplay(items);
        loadTraySlotInfo(items);
    }

    private final AtomicInteger updateItemsTableCount = new AtomicInteger();
    private final AtomicLong updateItemsTableTime = new AtomicLong();
    private final AtomicLong updateItemsTableMaxTime = new AtomicLong();

    private void updateItemsTable(List<PhysicalItem> items) {
        settingItems = true;
        long startTime = System.currentTimeMillis();
        int c = updateItemsTableCount.incrementAndGet();
        Utils.runOnDispatchThread(() -> {
            updateItemsTableOnDisplay(items);
            settingItems = false;
            if (debugTimes) {
                long endTime = System.currentTimeMillis();
                long diff = endTime - startTime;
                long total = updateItemsTableTime.addAndGet(diff);
                long max = updateItemsTableMaxTime.getAndAccumulate(diff, Math::max);
                System.out.println("updateItemsTable count = " + c);
                System.out.println("updateItemsTable max = " + max);
                System.out.println("updateItemsTable avg = " + total / c);
            }
        });
    }

    private int updateItemsTableOnDisplayCount = 0;
    private long updateItemsTableOnDisplayCountTotalTime = 0;
    private long updateItemsTableOnDisplayCountMaxTime = 0;

    @UIEffect
    private void updateItemsTableOnDisplay(List<PhysicalItem> items) {
        long start = System.currentTimeMillis();
        updateItemsTableOnDisplayCount++;
        if (!object2DJPanel1.isShowOutputItems()) {
            if (object2DJPanel1.isShowAddedSlotPositions()) {
                loadItemsToTable(object2DJPanel1.getItemsWithAddedExtras(), jTableItems);
            } else {
                loadItemsToTable(items, jTableItems);
            }
        }
        if (debugTimes) {
            long end = System.currentTimeMillis();
            long diff = end - start;
            updateItemsTableOnDisplayCountTotalTime += diff;
            updateItemsTableOnDisplayCountMaxTime = Math.max(updateItemsTableOnDisplayCountMaxTime, diff);
            System.out.println("updateItemsTableOnDisplayCount = " + updateItemsTableOnDisplayCount);
            System.out.println("updateItemsTableOnDisplayCountMaxTime = " + updateItemsTableOnDisplayCountMaxTime);
            long averageUpdateItemsTableOnDisplay = updateItemsTableOnDisplayCountTotalTime / updateItemsTableOnDisplayCount;
            System.out.println("averageUpdateItemsTableOnDisplay = " + averageUpdateItemsTableOnDisplay);
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

    @UIEffect
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
                double rot = toRadians((double) rotObject);
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
                }
                break;

            default:
                tm.setRowCount(0);
        }
        Utils.autoResizeTableColWidths(jTableTraySlots);
    }

    @UIEffect
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

    @UIEffect
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
            Object rowObjects[] = new Object[]{i, item.getName(), item.x, item.y, toDegrees(item.getRotation()), item.getType(), item.getScore()};
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
    @UIEffect
    public Object2DOuterJPanel() {
        initComponents();
        this.setItemsInternal(object2DJPanel1.getItems());
        jTableItems.getModel().addTableModelListener(itemsTableModelListener);
        jTableItems.getSelectionModel().addListSelectionListener(itemsTableListSelectionListener);
        jTableTraySlots.getModel().addTableModelListener(traySlotsTableModelListener);
        setMaxXMaxYText(jTextFieldMaxXMaxY.getText().trim());
        setMinXMinYText(jTextFieldMinXMinY.getText().trim());
        object2DJPanel1.setShowCurrentXY(jCheckBoxShowCurrent.isSelected());
        simulatedCachedCheckBox = new CachedCheckBox(jCheckBoxSimulated);
        filenameCachedTextField = new CachedTextField(jTextFieldFilename);
        pauseCachedCheckBox = new CachedCheckBox(jCheckBoxPause);
        portCachedTextField = new CachedTextField(jTextFieldPort);
        hostCachedTextField = new CachedTextField(jTextFieldHost);
        debugCachedCheckBox = new CachedCheckBox(jCheckBoxDebug);
        addPosNoiseCachedCheckBox = new CachedCheckBox(jCheckBoxAddPosNoise);
        shuffleSimulatedUpdatesCachedCheckBox = new CachedCheckBox(jCheckBoxShuffleSimulatedUpdates);
        posNoiseCachedTextField = new CachedTextField(jTextFieldPosNoise);
        rotNoiseCachedTextField = new CachedTextField(jTextFieldRotNoise);
        connectedCachedCheckBox = new CachedCheckBox(jCheckBoxConnected);
        simDropRateCachedTextField = new CachedTextField(jTextFieldSimDropRate);
        simulationUpdateTimeCachedTextField = new CachedTextField(jTextFieldSimulationUpdateTime);
        simulationUpdateAsNeededCachedCheckBox = new CachedCheckBox(jCheckBoxSimulationUpdateAsNeeded);
        showCurrentCachedCheckBox = new CachedCheckBox(jCheckBoxShowCurrent);
        viewOutputCachedCheckBox = new CachedCheckBox(jCheckBoxViewOutput);
        autoscaleCachedCheckBox = new CachedCheckBox(jCheckBoxAutoscale);
        pickupDistCachedTextField = new CachedTextField(jTextFieldPickupDist);
        dropOffThresholdCachedTextField = new CachedTextField(jTextFieldDropOffThreshold);
        jTextFieldRotationOffset.setText(String.format("%.1f", toDegrees(object2DJPanel1.getRotationOffset())));
    }

    private final TableModelListener itemsTableModelListener = new TableModelListener() {
        @Override
        @UIEffect
        public void tableChanged(TableModelEvent e) {
            try {
                boolean changeFound = false;

                if (!settingItems && !object2DJPanel1.isShowOutputItems()) {
                    List<PhysicalItem> l = new ArrayList<>(getItems());
                    PhysicalItem item;
                    for (int i = 0; i < jTableItems.getRowCount(); i++) {
                        Object listIndexObject = jTableItems.getValueAt(i, 0);
                        if (!(listIndexObject instanceof Integer)) {
                            throw new IllegalStateException("bad listIndexObject in table at(" + i + ",0) :" + listIndexObject);
                        }
                        int listIndex = (int) listIndexObject;
                        Object valueAtI1 = jTableItems.getValueAt(i, 1);
                        if (!(valueAtI1 instanceof String)) {
                            throw new IllegalStateException("bad value in table at(" + i + ",1) :" + valueAtI1);
                        }
                        String nameValue = (String) valueAtI1;
                        if (nameValue.length() < 1) {
                            continue;
                        }
                        if (listIndex < l.size()) {
                            item = l.get(listIndex);
                        } else {
                            item = null;
                        }
                        Object valueAtI2 = jTableItems.getValueAt(i, 2);
                        if (valueAtI2 instanceof String) {
                            valueAtI2 = Double.valueOf((String) valueAtI2);
                        } else if (!(valueAtI2 instanceof Double)) {
                            throw new IllegalStateException("bad value in table at(" + i + ",2) :" + valueAtI2);
                        }
                        Double xValue = (Double) valueAtI2;
                        Object valueAtI3 = jTableItems.getValueAt(i, 3);
                        if (valueAtI3 instanceof String) {
                            valueAtI3 = Double.valueOf((String) valueAtI3);
                        } else if (!(valueAtI3 instanceof Double)) {
                            throw new IllegalStateException("bad value in table at(" + i + ",3) :" + valueAtI3);
                        }
                        Double yValue = (Double) valueAtI3;
                        Object valueAtI4 = jTableItems.getValueAt(i, 4);
                        if (valueAtI4 instanceof String) {
                            valueAtI4 = Double.valueOf((String) valueAtI4);
                        } else if (!(valueAtI4 instanceof Double)) {
                            throw new IllegalStateException("bad value in table at(" + i + ",4) :" + valueAtI4);
                        }
                        Double rotationValue = (Double) valueAtI4;
                        Object valueAtI5 = jTableItems.getValueAt(i, 5);
                        if (null == valueAtI5) {
                            throw new IllegalStateException("bad value in table at(" + i + ",5) :" + valueAtI5);
                        }
                        Object valueAtI6 = jTableItems.getValueAt(i, 6);
                        if (valueAtI6 instanceof String) {
                            valueAtI6 = Double.valueOf((String) valueAtI6);
                        } else if (!(valueAtI6 instanceof Double)) {
                            throw new IllegalStateException("bad value in table at(" + i + ",6) :" + valueAtI6);
                        }
                        Double scoreValue = (Double) valueAtI6;
                        if (item == null || item.getName() == null
                                || !Objects.equals(item.getType(), valueAtI5)
                                || !Objects.equals(item.getName(), nameValue)
                                || Math.abs(item.x - xValue) > 0.001
                                || Math.abs(item.y - yValue) > 0.001
                                || Math.abs(item.getRotation() - rotationValue) > 0.001
                                || Math.abs(item.getScore() - scoreValue) > 0.001) {
                            changeFound = true;
                        } else {
                            continue;
                        }
                        String name = Objects.toString(nameValue);
                        if (item == null || !item.getName().equals(name)) {
                            double x = xValue;
                            double y = yValue;
                            double rotation = rotationValue;
                            String type = Objects.toString(valueAtI5);
                            double score = scoreValue;
                            item = newPhysicalItemNameRotXYScoreType(name, rotation, x, y, score, type);
                        }
                        item.x = parseDouble(xValue.toString());
                        item.y = parseDouble(yValue.toString());
                        item.setRotation(toRadians(rotationValue));
                        item.setType(Objects.toString(valueAtI5));
                        item.setScore(parseDouble(scoreValue.toString()));
                        while (l.size() < listIndex) {
                            l.add(new Part("placeHolder" + l.size()));
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
    };

    private final TableModelListener traySlotsTableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (!jCheckBoxSimulated.isSelected()) {
                return;
            }
            if (e.getColumn() == 1 && e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row == e.getLastRow()) {
                    Object traySlotValue1 = jTableTraySlots.getValueAt(row, 1);
                    if (!(traySlotValue1 instanceof Boolean)) {
                        throw new IllegalStateException("bad value in table at " + row + ",1 :" + traySlotValue1);
                    }
                    boolean filled = (boolean) traySlotValue1;
                    List<PhysicalItem> l = new ArrayList<>();
                    int selectedRowIndex = jTableItems.getSelectedRow();
                    Object traySlotValue2 = jTableTraySlots.getValueAt(row, 2);
                    if (!(traySlotValue2 instanceof Double)) {
                        throw new IllegalStateException("bad value in table at " + row + ",2 :" + traySlotValue2);
                    }
                    Object traySlotValue3 = jTableTraySlots.getValueAt(row, 3);
                    if (!(traySlotValue3 instanceof Double)) {
                        throw new IllegalStateException("bad value in table at " + row + ",3 :" + traySlotValue3);
                    }
                    if (filled) {
                        l.addAll(getItems());
                        double sx = (double) traySlotValue2;
                        double sy = (double) traySlotValue3;
                        Object traySlotValue0 = jTableTraySlots.getValueAt(row, 0);
                        if (!(traySlotValue0 instanceof String)) {
                            throw new IllegalStateException("bad value in table at " + row + ",0 :" + traySlotValue0);
                        }
                        String name = (String) traySlotValue0;
                        PhysicalItem newPart = newPhysicalItemNameRotXYScoreType(name, 0.0, sx, sy, 100.0, "P");
                        l.add(newPart);
                    } else {
                        double sx = (double) traySlotValue2;
                        double sy = (double) traySlotValue3;
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
    };

    private final ListSelectionListener itemsTableListSelectionListener = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent event) {
            int selectedRow = jTableItems.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < jTableItems.getRowCount()) {
                Object itemValue0 = jTableItems.getValueAt(selectedRow, 0);
                if (!(itemValue0 instanceof Integer)) {
                    throw new IllegalStateException("Bad value in table at " + selectedRow + ",0 :" + itemValue0);
                }
                object2DJPanel1.setSelectedItemIndex((int) (itemValue0));
                if (!object2DJPanel1.isShowOutputItems()) {
                    loadTraySlotInfo(getItems());
                }
            }
        }
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "nullness", "guieffect"})
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelTopRow = new javax.swing.JPanel();
        jTextFieldFilename = new javax.swing.JTextField();
        jButtonSave = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jPanelBottomMain = new javax.swing.JPanel();
        jPanelRightSide = new javax.swing.JPanel();
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
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaConnectDetails = new javax.swing.JTextArea();
        jLabel12 = new javax.swing.JLabel();
        jComboBoxHandleRotationsEnum = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        jTextFieldRotationOffset = new javax.swing.JTextField();
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
        jPanelTrays = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableTraySlots = new javax.swing.JTable();
        jScrollPaneProperties = new javax.swing.JScrollPane();
        jTableProperties = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableItems = new javax.swing.JTable();
        object2DJPanel1 = new aprs.simview.Object2DJPanel();

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

        javax.swing.GroupLayout jPanelTopRowLayout = new javax.swing.GroupLayout(jPanelTopRow);
        jPanelTopRow.setLayout(jPanelTopRowLayout);
        jPanelTopRowLayout.setHorizontalGroup(
            jPanelTopRowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTopRowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldFilename, javax.swing.GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonLoad)
                .addContainerGap())
        );
        jPanelTopRowLayout.setVerticalGroup(
            jPanelTopRowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTopRowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTopRowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSave)
                    .addComponent(jButtonLoad))
                .addContainerGap())
        );

        jPanelRightSide.setMinimumSize(new java.awt.Dimension(350, 0));

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

        jTextAreaConnectDetails.setEditable(false);
        jTextAreaConnectDetails.setColumns(20);
        jTextAreaConnectDetails.setRows(5);
        jScrollPane3.setViewportView(jTextAreaConnectDetails);

        jLabel12.setText("Rotations:");

        jComboBoxHandleRotationsEnum.setModel(getHandleRotationComboBoxModel());
        jComboBoxHandleRotationsEnum.setSelectedItem(getHandleRotationEnum());
        jComboBoxHandleRotationsEnum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxHandleRotationsEnumActionPerformed(evt);
            }
        });

        jLabel13.setText("Rot Offset: ");

        jTextFieldRotationOffset.setText("0");
        jTextFieldRotationOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldRotationOffsetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelConnectionsTabLayout = new javax.swing.GroupLayout(jPanelConnectionsTab);
        jPanelConnectionsTab.setLayout(jPanelConnectionsTabLayout);
        jPanelConnectionsTabLayout.setHorizontalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
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
                        .addComponent(jButtonRefresh)
                        .addGap(0, 35, Short.MAX_VALUE))
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3)
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxSimulated)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxConnected, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxDebug)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxPause)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBoxHandleRotationsEnum, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        jPanelConnectionsTabLayout.setVerticalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulated)
                    .addComponent(jCheckBoxConnected)
                    .addComponent(jLabel13)
                    .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                    .addComponent(jCheckBoxPause)
                    .addComponent(jLabel12)
                    .addComponent(jComboBoxHandleRotationsEnum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
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
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPickupDist, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldDropOffThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8))
                            .addComponent(jCheckBoxShuffleSimulatedUpdates))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxAddPosNoise)
                            .addComponent(jTextFieldRotNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelSimulationTabLayout.createSequentialGroup()
                            .addComponent(jLabel6)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextFieldSimDropRate))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelSimulationTabLayout.createSequentialGroup()
                            .addComponent(jLabel5)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextFieldSimulationUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jCheckBoxViewOutput))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        javax.swing.GroupLayout jPanelTraysLayout = new javax.swing.GroupLayout(jPanelTrays);
        jPanelTrays.setLayout(jPanelTraysLayout);
        jPanelTraysLayout.setHorizontalGroup(
            jPanelTraysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTraysLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelTraysLayout.setVerticalGroup(
            jPanelTraysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTraysLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 209, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Trays", jPanelTrays);

        jTableProperties.setAutoCreateRowSorter(true);
        jTableProperties.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneProperties.setViewportView(jTableProperties);

        jTabbedPane1.addTab("Properties", jScrollPaneProperties);

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

        javax.swing.GroupLayout jPanelRightSideLayout = new javax.swing.GroupLayout(jPanelRightSide);
        jPanelRightSide.setLayout(jPanelRightSideLayout);
        jPanelRightSideLayout.setHorizontalGroup(
            jPanelRightSideLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRightSideLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelRightSideLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelRightSideLayout.setVerticalGroup(
            jPanelRightSideLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRightSideLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        object2DJPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        object2DJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseDragged(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseMoved(evt);
            }
        });
        object2DJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                object2DJPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseReleased(evt);
            }
        });

        javax.swing.GroupLayout object2DJPanel1Layout = new javax.swing.GroupLayout(object2DJPanel1);
        object2DJPanel1.setLayout(object2DJPanel1Layout);
        object2DJPanel1Layout.setHorizontalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 395, Short.MAX_VALUE)
        );
        object2DJPanel1Layout.setVerticalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 546, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanelBottomMainLayout = new javax.swing.GroupLayout(jPanelBottomMain);
        jPanelBottomMain.setLayout(jPanelBottomMainLayout);
        jPanelBottomMainLayout.setHorizontalGroup(
            jPanelBottomMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomMainLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPanelRightSide, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelBottomMainLayout.setVerticalGroup(
            jPanelBottomMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBottomMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelRightSide, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelTopRow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelBottomMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelTopRow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBottomMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private final DefaultComboBoxModel<HandleRotationEnum> handleRotationComboBoxModel
            = new javax.swing.DefaultComboBoxModel<>(HandleRotationEnum.values());

    public DefaultComboBoxModel<HandleRotationEnum> getHandleRotationComboBoxModel() {
        return handleRotationComboBoxModel;
    }
    private final CachedTextField posNoiseCachedTextField;

    /**
     * Set the value of posNoise
     *
     * @param posNoise new value of posNoise
     */
    private void setPosNoise(double posNoise) {
        updateTextFieldDouble(posNoise, posNoiseCachedTextField, 0.01);
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

    @UIEffect
    private void updateTextFieldDouble(double value, JTextField textField, double threshold) {
        if (Math.abs(value - parseDouble(textField.getText().trim())) > threshold) {
            textField.setText(String.format("%.3f", value));
        }
    }

    private void updateTextFieldDouble(double value, CachedTextField textField, double threshold) {
        if (Math.abs(value - parseDouble(textField.getText().trim())) > threshold) {
            textField.setText(String.format("%.3f", value));
        }
    }

    private final CachedTextField rotNoiseCachedTextField;

    /**
     * Set the value of rotNoise
     *
     * @param rotNoise new value of rotNoise
     */
    private void setRotNoise(double rotNoise) {
        updateTextFieldDouble(rotNoise, rotNoiseCachedTextField, 0.01);
        this.rotNoise = rotNoise;
    }

    private void setSimulatedInternal(boolean simulated) {
        Utils.runOnDispatchThread(() -> setSimulatedInternalOnDisplay(simulated));
    }

    @UIEffect
    private void setSimulatedInternalOnDisplay(boolean simulated) {

        jButtonAdd.setEnabled(simulated);
        jButtonDelete.setEnabled(simulated);
        jButtonReset.setEnabled(simulated);
        jButtonOffsetAll.setEnabled(simulated);
        boolean simulationUpdateAsNeeded = simulationUpdateAsNeededCachedCheckBox.isSelected();
        jTextFieldSimulationUpdateTime.setEditable(simulated && !simulationUpdateAsNeeded);
        jTextFieldSimulationUpdateTime.setEnabled(simulated && !simulationUpdateAsNeeded);
        simulationUpdateAsNeededCachedCheckBox.setEnabled(simulated);
        jPanelSimulationTab.setEnabled(simulated);
        jTextFieldSimDropRate.setEnabled(simulated);
        jTextFieldSimDropRate.setEditable(simulated);
        shuffleSimulatedUpdatesCachedCheckBox.setEnabled(simulated);
        addPosNoiseCachedCheckBox.setEnabled(simulated);
        jCheckBoxViewOutput.setEnabled(simulated);
        jTextFieldPosNoise.setEditable(simulated && addPosNoiseCachedCheckBox.isSelected());
        jTextFieldPosNoise.setEnabled(simulated && addPosNoiseCachedCheckBox.isSelected());
        jTextFieldRotNoise.setEditable(simulated && addPosNoiseCachedCheckBox.isSelected());
        jTextFieldRotNoise.setEnabled(simulated && addPosNoiseCachedCheckBox.isSelected());
        object2DJPanel1.setShowOutputItems(simulated && viewOutputCachedCheckBox.isSelected());
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

    @UIEffect
    private void jCheckBoxSimulatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulatedActionPerformed
        connectedCachedCheckBox.setSelected(false);
        setSimulatedInternal(this.jCheckBoxSimulated.isSelected());
        disconnect();
    }//GEN-LAST:event_jCheckBoxSimulatedActionPerformed

    private final CachedCheckBox connectedCachedCheckBox;

    public void setSimulated(boolean simulated) {
        connectedCachedCheckBox.setSelected(false);
        simulatedCachedCheckBox.setSelected(simulated);
        setSimulatedInternal(simulated);
        disconnect();
    }

    @Nullable
    private VisionSocketServer visionSocketServer = null;
    @Nullable
    private VisionSocketClient visionSocketClient = null;

    @UIEffect
    private void jCheckBoxConnectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxConnectedActionPerformed
        try {
            jButtonReset.setEnabled(false);
            if (this.jCheckBoxConnected.isSelected()) {
                connect();
            } else {
                if (simulatedCachedCheckBox.isSelected()) {
                    jButtonReset.setEnabled(true);
                }
                disconnect();
            }
        } catch (Exception exception) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", exception);
            String message = exception.getMessage();
            if (null != message) {
                JOptionPane.showMessageDialog(this, message);
            }
            if (null == visionSocketClient || !visionSocketClient.isConnected()) {
                jCheckBoxConnected.setSelected(false);
            }
        }
    }//GEN-LAST:event_jCheckBoxConnectedActionPerformed

    private void disconnect() {
        if (null != visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
            visionSocketClient.removeListListener(this);
            visionSocketClient = null;
        }
        if (null != visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }
    }

    private final CachedTextField hostCachedTextField;
    private final CachedTextField portCachedTextField;
    private final CachedCheckBox debugCachedCheckBox;

    private void connect() throws NumberFormatException {
        if (simulatedCachedCheckBox.isSelected()) {
            try {
                int port = Integer.parseInt(portCachedTextField.getText().trim());
                if (null != visionSocketServer && visionSocketServer.getPort() != port) {
                    disconnect();
                }
                if (null == visionSocketServer) {
                    visionSocketServer = new VisionSocketServer(port);
                }
                visionSocketServer.setDebug(debugCachedCheckBox.isSelected());
                publishCurrentItems();
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        } else {
            int port = Integer.parseInt(portCachedTextField.getText().trim());
            String host = hostCachedTextField.getText().trim();
            if (null != visionSocketClient) {
                if (visionSocketClient.isConnected()
                        && port == visionSocketClient.getPort()
                        && Objects.equals(visionSocketClient.getHost(), host)) {
                    return;
                }
                try {
                    visionSocketClient.close();
                } catch (Exception ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
            VisionSocketClient clnt = new VisionSocketClient();
            clnt.setIgnoreLosingItemsLists(ignoreLosingItemsLists);
            this.visionSocketClient = clnt;
            Map<String, String> argsMap = DbSetupBuilder.getDefaultArgsMap();
            argsMap.put("--visionport", portCachedTextField.getText().trim());
            argsMap.put("--visionhost", host);
            argsMap.put("handleRotationEnum", handleRotationEnum.toString());
            clnt.setDebug(debugCachedCheckBox.isSelected());
            clnt.start(argsMap);
            if (!clnt.isConnected()) {
                connectedCachedCheckBox.setSelected(false);
                try {
                    clnt.close();
                } catch (Exception ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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

    private final CachedTextField simDropRateCachedTextField;

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
        updateTextFieldDouble(simulatedDropRate, simDropRateCachedTextField, 0.001);
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
    private final CachedCheckBox addPosNoiseCachedCheckBox;

    private PhysicalItem noiseFilter(PhysicalItem in) {
        if (!addPosNoiseCachedCheckBox.isSelected()) {
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
        double g = posRandom.nextGaussian();
        if (g < -3.5) {
            return -3.5;
        } else if (g > 3.5) {
            return 3.5;
        } else {
            return g;
        }
    }

    private double nextPosNoise() {
        return nextLimitedGaussian() * posNoise;
    }

    private double nextRotNoise() {
        return nextLimitedGaussian() * toRadians(rotNoise);
    }

    private final CachedCheckBox shuffleSimulatedUpdatesCachedCheckBox;

    private void publishCurrentItems() {
        if (forceOutputFlag) {
            return;
        }
        if (captured_item_index > 0) {
            return;
        }
        if (null != draggedItem) {
            return;
        }
        if (null != draggedItemsList && !draggedItemsList.isEmpty()) {
            return;
        }
        VisionSocketServer srv = this.visionSocketServer;
        if (null == srv) {
            throw new IllegalStateException("visionSocketServer is null");
        }
        boolean addPosNoise = addPosNoiseCachedCheckBox.isSelected();
        if (shuffleSimulatedUpdatesCachedCheckBox.isSelected() || simulatedDropRate > 0.01 || addPosNoise) {
            List<PhysicalItem> origList = getItems();
            List<PhysicalItem> l = new ArrayList<>(origList);
            if (simulatedDropRate > 0.01 || addPosNoise) {
                l = l.stream()
                        .filter(this::dropFilter)
                        .map(this::noiseFilter)
                        .collect(Collectors.toList());
            }
            if (shuffleSimulatedUpdatesCachedCheckBox.isSelected()) {
                Collections.shuffle(l);
            }
            srv.publishList(l);
            setOutputItems(l);
        } else {
            srv.publishList(getItems());
        }
    }

    @UIEffect
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

    @UIEffect
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
    private volatile boolean mouseDown = false;

    public boolean isUserMouseDown() {
        return null != this.draggedItem || mouseDown;
    }

    private volatile long mouseDraggedUpdateTableTime = -1;

    @UIEffect
    private void object2DJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseDragged
        mouseDown = true;
        double scale = object2DJPanel1.getScale();
        double min_x = object2DJPanel1.getMinX();
        double max_x = object2DJPanel1.getMaxX();
        double min_y = object2DJPanel1.getMinY();
        double max_y = object2DJPanel1.getMaxY();
        PhysicalItem itemToDrag = this.draggedItem;
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
//            if (Math.abs(xdiff) > 100 || Math.abs(ydiff) > 100) {
//                System.out.println("big drag jump");
//                this.draggedItem = null;
//                return;
//            }
            last_drag_max_x = max_x;
            last_drag_min_x = min_x;
            last_drag_max_y = max_y;
            last_drag_min_y = min_y;
            last_drag_scale = scale;

            List<PhysicalItem> includedItemsToDrag = this.draggedItemsList;
            if (null != includedItemsToDrag) {
                for (PhysicalItem item : includedItemsToDrag) {
                    item.x += xdiff;
                    item.y += ydiff;
                }
            }
            long t = System.currentTimeMillis();
            if (t - mouseDraggedUpdateTableTime > 200) {
                this.updateItemsTable(getItems());
                mouseDraggedUpdateTableTime = System.currentTimeMillis();
                object2DJPanel1.repaint();
            }
            mouseDown = true;
        }
    }//GEN-LAST:event_object2DJPanel1MouseDragged

    @Nullable
    private volatile PhysicalItem draggedItem = null;

    @Nullable
    private volatile List<PhysicalItem> draggedItemsList = null;

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
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
        return inside;
    }

    @UIEffect
    private void object2DJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MousePressed
        mouseDown = true;
        int x = evt.getX();
        int y = evt.getY();
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
        if (null != closestItem) {
            if (!evt.isShiftDown() && !evt.isAltDown() && !evt.isControlDown()) {
                List<PhysicalItem> newDragItems = findIncludedItems(closestItem);
                this.draggedItemsList = newDragItems;
            } else if (!"P".equals(closestItem.getType())) {
                System.out.println("Hold SHIFT to move trays : closestItem=" + closestItem.getFullName());
                draggedItem = null;
                this.draggedItemsList = null;
                return;
            }
        }
        draggedItem = closestItem;
        mouseDown = true;

    }//GEN-LAST:event_object2DJPanel1MousePressed

    private List<PhysicalItem> findIncludedItems(PhysicalItem closestItem) {
        List<PhysicalItem> origItems = this.getItems();
        List<PhysicalItem> newDragItems = new ArrayList<>();
        if (closestItem.getMaxSlotDist() > 0) {
            double orig_x = closestItem.x;
            double orig_y = closestItem.y;
            for (int i = 0; i < origItems.size(); i++) {
                PhysicalItem item = origItems.get(i);
                if (item == closestItem) {
                    continue;
                }
                if (item.getMaxSlotDist() > 0) {
                    continue;
                }
                if (item.dist(orig_x, orig_y) > closestItem.getMaxSlotDist() * object2DJPanel1.getSlotMaxDistExpansion()) {
                    continue;
                }
                boolean foundCloser = false;
                for (int j = 0; j < origItems.size(); j++) {
                    if (j == i) {
                        continue;
                    }
                    PhysicalItem otherItem = origItems.get(j);
                    if (otherItem == closestItem) {
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
                newDragItems.add(item);
            }
        }
        return newDragItems;
    }

    @UIEffect
    private void object2DJPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseReleased
        mouseDown = false;
        draggedItemsList = null;
        if (null != draggedItem) {
            draggedItem = null;
            List<PhysicalItem> itemsList = getItems();
            this.updateItemsTable(itemsList);
            if (!setItemsListeners.isEmpty()) {
                notifySetItemsListeners(itemsList);
            }
        }
        draggedItem = null;
        draggedItemsList = null;
        mouseDown = false;
    }//GEN-LAST:event_object2DJPanel1MouseReleased

    @UIEffect
    private void jTextFieldMaxXMaxYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMaxXMaxYActionPerformed
        String txt = jTextFieldMaxXMaxY.getText().trim();
        setMaxXMaxYText(txt);
    }//GEN-LAST:event_jTextFieldMaxXMaxYActionPerformed

    public void setViewLimits(double minX, double minY, double maxX, double maxY) {
        Utils.runOnDispatchThread(() -> setViewLimitsOnDisplay(minX, minY, maxX, maxY));
    }

    @UIEffect
    private void setViewLimitsOnDisplay(double minX, double minY, double maxX, double maxY) {
        String minXMinYString = String.format("%.3f,%.3f", minX, minY);
        jTextFieldMinXMinY.setText(minXMinYString);
        jTextFieldCurrentXY.setText(minXMinYString);
        setMinXMinYText(minXMinYString);
        String maxXMaxYString = String.format("%.3f,%.3f", maxX, maxY);
        jTextFieldMaxXMaxY.setText(maxXMaxYString);
        setMaxXMaxYText(maxXMaxYString);
        autoscaleCachedCheckBox.setSelected(false);
        object2DJPanel1.setAutoscale(false);
    }

    private void setMaxXMaxYText(String txt) throws NumberFormatException {
        String vals[] = txt.split(",");
        if (vals.length == 2) {
            double newMaxX = parseDouble(vals[0]);
            double newMaxY = parseDouble(vals[1]);
            object2DJPanel1.setMaxX(newMaxX);
            object2DJPanel1.setMaxY(newMaxY);
        } else {
            System.err.println("Bad xmax,ymax = " + txt);
        }
    }

    @UIEffect
    private void jTextFieldMinXMinYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMinXMinYActionPerformed
        String txt = jTextFieldMinXMinY.getText().trim();
        setMinXMinYText(txt);
    }//GEN-LAST:event_jTextFieldMinXMinYActionPerformed

    @UIEffect
    private void jCheckBoxDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDebugActionPerformed
        boolean debugSelected = this.jCheckBoxDebug.isSelected();
        if (null != visionSocketServer) {
            visionSocketServer.setDebug(debugSelected);
        }
        if (null != visionSocketClient) {
            visionSocketClient.setDebug(debugSelected);
            visionSocketClient.setUpdateListenersOnIgnoredLine(debugSelected);
        }
    }//GEN-LAST:event_jCheckBoxDebugActionPerformed

    @UIEffect
    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        this.setItems(Object2DJPanel.EXAMPLES_ITEMS_LIST);
    }//GEN-LAST:event_jButtonResetActionPerformed

    @UIEffect
    private void jCheckBoxShowRotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowRotationsActionPerformed
        object2DJPanel1.setViewRotationsAndImages(this.jCheckBoxShowRotations.isSelected());
    }//GEN-LAST:event_jCheckBoxShowRotationsActionPerformed

    @UIEffect
    private void jCheckBoxPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxPauseActionPerformed
        boolean wasPaused = this.jCheckBoxPause.isSelected();
        if (!wasPaused) {
            if (null != visionSocketServer) {
                publishCurrentItems();
                if (jCheckBoxSimulated.isSelected() && !jCheckBoxSimulationUpdateAsNeeded.isSelected()) {
                    setupSimUpdateTimer();
                }
            }
        } else {
            stopSimUpdateTimerOnDisplay();
        }
    }//GEN-LAST:event_jCheckBoxPauseActionPerformed

    @UIEffect
    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        if (null != visionSocketServer && !this.jCheckBoxPause.isSelected()) {
            this.setItems(object2DJPanel1.getItems());
            publishCurrentItems();
        }
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    @UIEffect
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
        return aprsSystem.isSnapshotsSelected();
    }

    public void loadFile(File f, boolean convertRotToRad, boolean zeroRotations) throws IOException {

        boolean takeSnapshots = isSnapshotsEnabled();
        if (takeSnapshots) {
            try {
                takeSnapshot(createTempFile("before_loadFile_" + f.getName() + "_", ".PNG"), (PmCartesian) null, "");
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        if (f.isDirectory()) {
            System.err.println("Can not load file \"" + f + "\" : It is a directory when a text/csv file is expected.");
            return;
        }
        String line = Files.lines(f.toPath()).skip(1).map(String::trim).collect(Collectors.joining(","));
        this.setItems(VisionSocketClient.lineToList(line, convertRotToRad, zeroRotations));
        filenameCachedTextField.setText(f.getCanonicalPath());
        if (takeSnapshots) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    takeSnapshot(createTempFile("loadFile_" + f.getName() + "_", ".PNG"), (PmCartesian) null, "");
                } catch (IOException ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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

    @UIEffect
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
                Object selectedItemHandleRotations = jComboBoxHandleRotationsEnum.getSelectedItem();
                loadFile(chooser.getSelectedFile(), selectedItemHandleRotations == HandleRotationEnum.DEGREES, selectedItemHandleRotations == HandleRotationEnum.IGNORE);
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonLoadActionPerformed

    @UIEffect
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
                filenameCachedTextField.setText(newFile.getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
    }//GEN-LAST:event_jButtonSaveActionPerformed

    @UIEffect
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

    @UIEffect
    private void jButtonCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCurrentActionPerformed
        List<PhysicalItem> items = this.getItems();
        int selectedIndex = object2DJPanel1.getSelectedItemIndex();
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            PhysicalItem item = items.get(selectedIndex);
            item.x = currentX;
            item.y = currentY;
        }
    }//GEN-LAST:event_jButtonCurrentActionPerformed

    @UIEffect
    private void jCheckBoxShowCurrentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowCurrentActionPerformed
        setTrackCurrentPos(jCheckBoxShowCurrent.isSelected());
    }//GEN-LAST:event_jCheckBoxShowCurrentActionPerformed

    @UIEffect
    private void jCheckBoxSeparateNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSeparateNamesActionPerformed
        object2DJPanel1.setUseSeparateNames(jCheckBoxSeparateNames.isSelected());
    }//GEN-LAST:event_jCheckBoxSeparateNamesActionPerformed

    @UIEffect
    private void jCheckBoxAutoscaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAutoscaleActionPerformed
        object2DJPanel1.setAutoscale(this.jCheckBoxAutoscale.isSelected());
    }//GEN-LAST:event_jCheckBoxAutoscaleActionPerformed

    
    public boolean isAutoscale() {
        return object2DJPanel1.isAutoscale() && jCheckBoxAutoscale.isSelected();
    }
    
    public void setAutoscale(boolean autoscale) {
        object2DJPanel1.setAutoscale(autoscale);
        jCheckBoxAutoscale.setSelected(autoscale);
    }
    
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

    @UIEffect
    private void jButtonOffsetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOffsetAllActionPerformed
        offsetAll();
    }//GEN-LAST:event_jButtonOffsetAllActionPerformed

    @UIEffect
    private void jTextFieldSimulationUpdateTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSimulationUpdateTimeActionPerformed
        setSimRefreshMillis(Integer.parseInt(jTextFieldSimulationUpdateTime.getText().trim()));
        setupSimUpdateTimer();
    }//GEN-LAST:event_jTextFieldSimulationUpdateTimeActionPerformed

    @UIEffect
    private void jCheckBoxSimulationUpdateAsNeededActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulationUpdateAsNeededActionPerformed
        boolean simulationUpdateAsNeeded = jCheckBoxSimulationUpdateAsNeeded.isSelected();
        boolean simulated = jCheckBoxSimulated.isSelected();
        jTextFieldSimulationUpdateTime.setEditable(simulated && !simulationUpdateAsNeeded);
        jTextFieldSimulationUpdateTime.setEnabled(simulated && !simulationUpdateAsNeeded);
        setupSimUpdateTimer();
    }//GEN-LAST:event_jCheckBoxSimulationUpdateAsNeededActionPerformed

    @UIEffect
    private void jTextFieldRotNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRotNoiseActionPerformed
        setRotNoise(parseDouble(jTextFieldRotNoise.getText().trim()));
    }//GEN-LAST:event_jTextFieldRotNoiseActionPerformed

    @UIEffect
    private void jTextFieldPosNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPosNoiseActionPerformed
        setPosNoise(parseDouble(jTextFieldPosNoise.getText().trim()));
    }//GEN-LAST:event_jTextFieldPosNoiseActionPerformed

    @UIEffect
    private void jCheckBoxAddPosNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddPosNoiseActionPerformed
        final boolean enable = jCheckBoxSimulated.isSelected() && jCheckBoxAddPosNoise.isSelected();
        jTextFieldPosNoise.setEditable(enable);
        jTextFieldPosNoise.setEnabled(enable);
        jTextFieldRotNoise.setEditable(enable);
        jTextFieldRotNoise.setEnabled(enable);
    }//GEN-LAST:event_jCheckBoxAddPosNoiseActionPerformed

    @UIEffect
    private void jCheckBoxViewOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxViewOutputActionPerformed
        setShowOutputItemsOnDisplay(jCheckBoxViewOutput.isSelected());
    }//GEN-LAST:event_jCheckBoxViewOutputActionPerformed

    public void setShowOutputItems(boolean showOutputItems) {
        Utils.runOnDispatchThread(() -> setShowOutputItemsOnDisplay(showOutputItems));
    }

    @UIEffect
    private void setShowOutputItemsOnDisplay(boolean showOutputItems) {
        object2DJPanel1.setShowOutputItems(showOutputItems);
        if (!showOutputItems) {
            setItemsInternal(getItems());
        } else {
            setOutputItemsInternal(getOutputItems());
        }
        if (showOutputItems != viewOutputCachedCheckBox.isSelected()) {
            viewOutputCachedCheckBox.setSelected(showOutputItems);
        }
    }

    @UIEffect
    private void jTextFieldSimDropRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSimDropRateActionPerformed
        setSimulatedDropRate(parseDouble(jTextFieldSimDropRate.getText().trim()));
    }//GEN-LAST:event_jTextFieldSimDropRateActionPerformed

    @UIEffect
    private void jTextFieldPickupDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPickupDistActionPerformed
        setPickupDist(parseDouble(jTextFieldPickupDist.getText().trim()));
    }//GEN-LAST:event_jTextFieldPickupDistActionPerformed

    @UIEffect
    private void jTextFieldDropOffThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDropOffThresholdActionPerformed
        setDropOffThreshold(parseDouble(jTextFieldDropOffThreshold.getText().trim()));
    }//GEN-LAST:event_jTextFieldDropOffThresholdActionPerformed

    @UIEffect
    private void jCheckBoxAddSlotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxAddSlotsActionPerformed
        object2DJPanel1.setShowAddedSlotPositions(jCheckBoxAddSlots.isSelected());
        refresh(false);
    }//GEN-LAST:event_jCheckBoxAddSlotsActionPerformed

    @UIEffect
    private void jCheckBoxDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxDetailsActionPerformed
        object2DJPanel1.setViewDetails(jCheckBoxDetails.isSelected());
    }//GEN-LAST:event_jCheckBoxDetailsActionPerformed

    @UIEffect
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
        this.draggedItemsList = null;
    }//GEN-LAST:event_object2DJPanel1MouseClicked

    private void object2DJPanel1MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseMoved
        this.draggedItem = null;
        this.draggedItemsList = null;
    }//GEN-LAST:event_object2DJPanel1MouseMoved

    private void object2DJPanel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseEntered
        this.draggedItem = null;
        this.draggedItemsList = null;
    }//GEN-LAST:event_object2DJPanel1MouseEntered

    private void object2DJPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseExited
        this.draggedItem = null;
        this.draggedItemsList = null;
    }//GEN-LAST:event_object2DJPanel1MouseExited

    @UIEffect
    private void jCheckBoxToolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxToolsActionPerformed
        object2DJPanel1.setShowAddedToolsAndToolHolders(jCheckBoxTools.isSelected());
        refresh(false);
    }//GEN-LAST:event_jCheckBoxToolsActionPerformed

    @UIEffect
    private void jComboBoxHandleRotationsEnumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxHandleRotationsEnumActionPerformed
        Object selectedObject = jComboBoxHandleRotationsEnum.getSelectedItem();
        HandleRotationEnum newHandleRotationEnum;
        if (selectedObject instanceof HandleRotationEnum) {
            newHandleRotationEnum = (HandleRotationEnum) selectedObject;
        } else if (selectedObject != null) {
            newHandleRotationEnum = HandleRotationEnum.valueOf(selectedObject.toString());
        } else {
            newHandleRotationEnum = HandleRotationEnum.DEGREES;
        }
        setHandleRotationEnum(newHandleRotationEnum);
    }//GEN-LAST:event_jComboBoxHandleRotationsEnumActionPerformed

    @UIEffect
    private void jTextFieldRotationOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRotationOffsetActionPerformed
        object2DJPanel1.setRotationOffset(toRadians(parseDouble(jTextFieldRotationOffset.getText().trim())));
    }//GEN-LAST:event_jTextFieldRotationOffsetActionPerformed

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

    private final CachedTextField simulationUpdateTimeCachedTextField;

    /**
     * Set the value of simRefreshMillis
     *
     * @param simRefreshMillis new value of simRefreshMillis
     */
    private void setSimRefreshMillis(int simRefreshMillis) {
        if (Integer.parseInt(simulationUpdateTimeCachedTextField.getText().trim()) != simRefreshMillis) {
            simulationUpdateTimeCachedTextField.setText(Integer.toString(simRefreshMillis));
        }
        this.simRefreshMillis = simRefreshMillis;
    }

    private final CachedCheckBox simulationUpdateAsNeededCachedCheckBox;

    private void simUpdateAction(ActionEvent evt) {
        if (simulationUpdateAsNeededCachedCheckBox.isSelected()) {
            return;
        }
        if (!forceOutputFlag) {
            refresh(false);
        }
    }

    public void stopSimUpdateTimer() {
        if (null != simUpdateTimer) {
            Utils.runOnDispatchThread(this::stopSimUpdateTimerOnDisplay);
        }
    }

    @UIEffect
    private void stopSimUpdateTimerOnDisplay() {
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
        if (simulationUpdateAsNeededCachedCheckBox.isSelected() || pauseCachedCheckBox.isSelected()) {
            return;
        }
        Utils.runOnDispatchThread(this::setupSimUpdateTimerOnDisplay);
    }

    @UIEffect
    private void setupSimUpdateTimerOnDisplay() {
        simUpdateTimer = new javax.swing.Timer(simRefreshMillis, this::simUpdateAction);
        simUpdateTimer.start();
    }

    @UIEffect
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
                        x = parseDouble(s.substring(2));
                    } else if (s.startsWith("y=")) {
                        y = parseDouble(s.substring(2));
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
                    setItems(newItems);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private final CachedCheckBox showCurrentCachedCheckBox;

    public void setTrackCurrentPos(boolean v) {
        if (showCurrentCachedCheckBox.isSelected() != v) {
            showCurrentCachedCheckBox.setSelected(v);
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
            double newMinX = parseDouble(vals[0]);
            double newMinY = parseDouble(vals[1]);
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
    private javax.swing.JComboBox<Object2DOuterJPanel.HandleRotationEnum> jComboBoxHandleRotationsEnum;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelHost;
    private javax.swing.JPanel jPanelBottomMain;
    private javax.swing.JPanel jPanelConnectionsTab;
    private javax.swing.JPanel jPanelOptionsTab;
    private javax.swing.JPanel jPanelRightSide;
    private javax.swing.JPanel jPanelSimulationTab;
    private javax.swing.JPanel jPanelTopRow;
    private javax.swing.JPanel jPanelTrays;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPaneProperties;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableItems;
    private javax.swing.JTable jTableProperties;
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
    private javax.swing.JTextField jTextFieldRotationOffset;
    private javax.swing.JTextField jTextFieldSimDropRate;
    private javax.swing.JTextField jTextFieldSimulationUpdateTime;
    private aprs.simview.Object2DJPanel object2DJPanel1;
    // End of variables declaration//GEN-END:variables

    public void dispose() {
        if (null != this.visionSocketClient) {
            try {
                visionSocketClient.close();
            } catch (Exception ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
                    .getName()).log(Level.SEVERE, "", exception);
        }
        return str;
    }

    private final CachedCheckBox viewOutputCachedCheckBox;
    private final CachedCheckBox autoscaleCachedCheckBox;

    public XFutureVoid saveProperties() {
        return Utils.runOnDispatchThread(this::savePropertiesOnDisplay);
    }

    @UIEffect
    private void savePropertiesOnDisplay() {
        if (null != propertiesFile) {
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                parentFile.mkdirs();
            }
            Properties props = new Properties();
            props.put("alternativeRotation", String.format("%.2f", toDegrees(object2DJPanel1.getAlternativeRotation())));
            props.put("--visionport", portCachedTextField.getText().trim());
            props.put("--visionhost", hostCachedTextField.getText().trim());
            props.put("simulated", Boolean.toString(simulatedCachedCheckBox.isSelected()));
            props.put("viewOutput", Boolean.toString(viewOutputCachedCheckBox.isSelected()));
            props.put("simulationUpdateAsNeeded", Boolean.toString(simulationUpdateAsNeededCachedCheckBox.isSelected()));
            props.put("shuffleSimulatedUpdates", Boolean.toString(shuffleSimulatedUpdatesCachedCheckBox.isSelected()));
            props.put("simulatedDropRate", String.format("%.3f", simulatedDropRate));
            props.put("addPosNoise", Boolean.toString(addPosNoiseCachedCheckBox.isSelected()));
            props.put("pickupDist", String.format("%.2f", pickupDist));
            props.put("dropOffThreshold", String.format("%.2f", dropOffThreshold));
            props.put("posNoise", String.format("%.2f", posNoise));
            props.put("rotNoise", String.format("%.2f", rotNoise));
            props.put("simRefreshMillis", Integer.toString(simRefreshMillis));
            props.put("connected", Boolean.toString(connectedCachedCheckBox.isSelected()));
            props.put("autoscale", Boolean.toString(autoscaleCachedCheckBox.isSelected()));
            props.put("tools", Boolean.toString(jCheckBoxTools.isSelected()));
            props.put("trackcurrentpos", Boolean.toString(showCurrentCachedCheckBox.isSelected()));
            props.put("showrotations", Boolean.toString(jCheckBoxShowRotations.isSelected()));
            props.put("viewDetails", Boolean.toString(jCheckBoxDetails.isSelected()));
            props.put("separatenames", Boolean.toString(jCheckBoxSeparateNames.isSelected()));
            props.put("xmaxymax", jTextFieldMaxXMaxY.getText().trim());
            props.put("xminymin", jTextFieldMinXMinY.getText().trim());
            if (reverseFlag) {
                this.reverseDataFileString = filenameCachedTextField.getText().trim();
            } else {
                this.dataFileString = filenameCachedTextField.getText().trim();
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

    public XFutureVoid loadProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.exists()) {
            loadingProperties = true;
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            return Utils.runOnDispatchThread(() -> {
                completeLoadPropertiesOnDisplay(props);
            });
        }
        return XFutureVoid.completedFutureWithName("Object2D.propertiesFile=" + propertiesFile);
    }

    @UIEffect
    private void completeLoadPropertiesOnDisplay(Properties props) {
        updateDisplayFromProperties(props);
        loadPropertiesTableOnDisplay(props);
        loadingProperties = false;
    }

    private volatile boolean loadingProperties = false;
    private volatile boolean loadingTableProperties = false;
    private volatile boolean updatingDisplayFromProperties = false;

    private final TableModelListener propertiesTableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (loadingProperties) {
                return;
            }
            Properties props = tableLoadedProperties;
            if (null == props) {
                return;
            }
            boolean propschanged = false;
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 1) {
                int firstRow = e.getFirstRow();
                int lastRow = e.getLastRow();
                DefaultTableModel model = (DefaultTableModel) jTableProperties.getModel();
                for (int i = Math.max(firstRow, 0); i < Math.min(lastRow + 1, model.getRowCount()); i++) {
                    Object key = model.getValueAt(i, 0);
                    Object value = model.getValueAt(i, 1);
                    if (null != key && null != value) {
                        props.put(key, value);
                    }
                    propschanged = true;
                }
                if (!propschanged) {
                    return;
                }
                if (loadingProperties || loadingTableProperties || updatingDisplayFromProperties) {
                    return;
                }
                updateDisplayFromProperties(props);
            }
        }
    };

    @MonotonicNonNull
    private volatile Properties tableLoadedProperties = null;

    @UIEffect
    private void loadPropertiesTableOnDisplay(Properties props) {
        loadingTableProperties = true;
        DefaultTableModel model = (DefaultTableModel) jTableProperties.getModel();
        model.removeTableModelListener(propertiesTableModelListener);
        model.setRowCount(0);
        Enumeration<?> propsEnum = props.propertyNames();
        while (propsEnum.hasMoreElements()) {
            Object keyObject = propsEnum.nextElement();
            if (keyObject instanceof String) {
                String key = (String) keyObject;
                if (key.length() > 0) {
                    Object valueObject = props.get(key);
                    model.addRow(new Object[]{key, valueObject});
                }
            }
        }
        tableLoadedProperties = props;
        loadingTableProperties = false;
        model.addTableModelListener(propertiesTableModelListener);
    }

    public static enum HandleRotationEnum {
        IGNORE,
        DEGREES,
        RADIANS
    };

//    private volatile HandleRotationEnum handleRotationEnum = HandleRotationEnum.RADIANS;
//
//    public HandleRotationEnum getHandleRotationEnum() {
//        return handleRotationEnum;
//    }
//
//    public void setHandleRotationEnum(HandleRotationEnum handleRotationEnum) {
//        this.handleRotationEnum = handleRotationEnum;
//        if(null != visionSocketClient) {
//            visionSocketClient.setConvertRotToRadians(handleRotationEnum == HandleRotationEnum.DEGREES);
//            visionSocketClient.setZeroRotations(handleRotationEnum == HandleRotationEnum.IGNORE);
//        }
//    }
    private HandleRotationEnum handleRotationEnum = HandleRotationEnum.RADIANS;

    /**
     * Get the value of handleRotationEnum
     *
     * @return the value of handleRotationEnum
     */
    public HandleRotationEnum getHandleRotationEnum() {
        return handleRotationEnum;
    }

    /**
     * Get the value of handleRotationEnum
     *
     * @return the value of handleRotationEnum
     */
    public String getHandleRotationEnumName() {
        return handleRotationEnum.name();
    }

    public void setHandleRotationEnumName(String handleRotationEnumName) {
        handleRotationEnum = HandleRotationEnum.valueOf(handleRotationEnumName);
    }

    /**
     * Set the value of handleRotationEnum
     *
     * @param handleRotationEnum new value of handleRotationEnum
     */
    public void setHandleRotationEnum(HandleRotationEnum handleRotationEnum) {
        this.handleRotationEnum = handleRotationEnum;
        if (null != visionSocketClient) {
            visionSocketClient.setConvertRotToRadians(handleRotationEnum == HandleRotationEnum.DEGREES);
            visionSocketClient.setZeroRotations(handleRotationEnum == HandleRotationEnum.IGNORE);
        }
    }

    @UIEffect
    private void updateDisplayFromProperties(Properties props) {
        updatingDisplayFromProperties = true;
        String itemsLine = props.getProperty(ITEMS_PROPERTY_NAME);
        String handleRotationEnumString = props.getProperty("handleRotationEnum");
        if (null != handleRotationEnumString) {
            this.handleRotationEnum = HandleRotationEnum.valueOf(handleRotationEnumString);
        }
        if (null != itemsLine && itemsLine.length() > 0) {
            List<PhysicalItem> l = VisionSocketClient.lineToList(itemsLine,
                    handleRotationEnum == HandleRotationEnum.DEGREES,
                    handleRotationEnum == HandleRotationEnum.IGNORE);
            if (null != l && l.size() > 0) {
                setItems(l);
            }
        }
        String alternativeRotationString = props.getProperty("alternativeRotation");
        if (null != alternativeRotationString) {
            object2DJPanel1.setAlternativeRotation(toRadians(parseDouble(alternativeRotationString)));
        }

        String portString = props.getProperty("--visionport");
        try {
            if (null != portString && portString.length() > 0) {
                int port = Integer.parseInt(portString);
                portCachedTextField.setText(Integer.toString(port));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String hostString = props.getProperty("--visionhost");
        try {
            if (null != hostString && hostString.length() > 0) {
                hostCachedTextField.setText(hostString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String simulationUpdateAsNeededString = props.getProperty("simulationUpdateAsNeeded");
        if (null != simulationUpdateAsNeededString && simulationUpdateAsNeededString.length() > 0) {
            boolean simulationUpdateAsNeeded = Boolean.valueOf(simulationUpdateAsNeededString);
            simulationUpdateAsNeededCachedCheckBox.setSelected(simulationUpdateAsNeeded);
            boolean simulated = simulatedCachedCheckBox.isSelected();
            jTextFieldSimulationUpdateTime.setEditable(simulated && !simulationUpdateAsNeeded);
            jTextFieldSimulationUpdateTime.setEnabled(simulated && !simulationUpdateAsNeeded);
        }

        String shuffleSimulatedUpdatesString = props.getProperty("shuffleSimulatedUpdates");
        if (null != shuffleSimulatedUpdatesString && shuffleSimulatedUpdatesString.length() > 0) {
            boolean shuffleSimulatedUpdates = Boolean.valueOf(shuffleSimulatedUpdatesString);
            shuffleSimulatedUpdatesCachedCheckBox.setSelected(shuffleSimulatedUpdates);
        }

        String viewOutputString = props.getProperty("viewOutput");
        if (null != viewOutputString && viewOutputString.length() > 0) {
            boolean viewOutput = Boolean.valueOf(viewOutputString);
            viewOutputCachedCheckBox.setSelected(viewOutput);
        }

        String addPosNoiseString = props.getProperty("addPosNoise");
        if (null != addPosNoiseString && addPosNoiseString.length() > 0) {
            boolean addPosNoise = Boolean.valueOf(addPosNoiseString);
            addPosNoiseCachedCheckBox.setSelected(addPosNoise);
        }
        String simulatedDropRateString = props.getProperty("simulatedDropRate");
        if (null != simulatedDropRateString && simulatedDropRateString.length() > 0) {
            double simDropRate = parseDouble(simulatedDropRateString);
            if (simDropRate < 0.001) {
                simDropRate = 0;
            }
            setSimulatedDropRate(simDropRate);
        }

        String pickupDistString = props.getProperty("pickupDist");
        if (null != pickupDistString && pickupDistString.length() > 0) {
            double simPickupDist = parseDouble(pickupDistString);
            setPickupDist(simPickupDist);
        }

        String dropOffThresholdString = props.getProperty("dropOffThreshold");
        if (null != dropOffThresholdString && dropOffThresholdString.length() > 0) {
            double simDropOffThreshold = parseDouble(dropOffThresholdString);
            setDropOffThreshold(simDropOffThreshold);
        }

        String posNoiseString = props.getProperty("posNoise");
        if (null != posNoiseString && posNoiseString.length() > 0) {
            double simPosNoise = parseDouble(posNoiseString);
            setPosNoise(simPosNoise);
        }

        String rotNoiseString = props.getProperty("rotNoise");
        if (null != rotNoiseString && rotNoiseString.length() > 0) {
            double simRotNoise = parseDouble(rotNoiseString);
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
            simulatedCachedCheckBox.setSelected(simulated);
            setSimulatedInternal(simulated);
        }

        String autoscaleString = props.getProperty("autoscale");
        if (null != autoscaleString && autoscaleString.length() > 0) {
            boolean autoscale = Boolean.valueOf(autoscaleString);
            autoscaleCachedCheckBox.setSelected(autoscale);
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
            connectedCachedCheckBox.setSelected(connected);
            if (connected) {
                connect();
            }
        }
        if (simulatedCachedCheckBox.isSelected() || !connectedCachedCheckBox.isSelected()) {
            if (needReloadDataFile()) {
                try {
                    reloadDataFile();
                } catch (IOException ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                }
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
            showCurrentCachedCheckBox.setSelected(trackCurrentPos);
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
        updatingDisplayFromProperties = false;
    }

    public boolean isSimulated() {
        return simulatedCachedCheckBox.isSelected();
    }

    public boolean isConnected() {
        return connectedCachedCheckBox.isSelected();
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
                filenameCachedTextField.setText(f.getCanonicalPath());
                loadFile(f);
            } else {
                File parentFile = propertiesFile.getParentFile();
                if (null == parentFile) {
                    throw new IllegalStateException("currentDataFileString = " + currentDataFileString + " does not exist and propertiesFile=" + propertiesFile + " has no parent");
                }
                String fullPath = parentFile.toPath().resolve(currentDataFileString).normalize().toString();
                f = new File(fullPath);
                if (f.exists() && f.canRead()) {
                    filenameCachedTextField.setText(f.getCanonicalPath());
                    loadFile(f);
                } else {
                    String fullPath2 = parentFile.toPath().resolveSibling(currentDataFileString).normalize().toString();
                    f = new File(fullPath2);
                    if (f.exists() && f.canRead()) {
                        filenameCachedTextField.setText(f.getCanonicalPath());
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

    private boolean ignoreLosingItemsLists = true;

    /**
     * Get the value of ignoreLosingItemsLists
     *
     * @return the value of ignoreLosingItemsLists
     */
    public boolean isIgnoreLosingItemsLists() {
        if (null != visionSocketClient) {
            boolean ret = visionSocketClient.isIgnoreLosingItemsLists();
            this.ignoreLosingItemsLists = ret;
            return ret;
        }
        return ignoreLosingItemsLists;
    }

    /**
     * Set the value of ignoreLosingItemsLists
     *
     * @param ignoreLosingItemsLists new value of ignoreLosingItemsLists
     */
    public void setIgnoreLosingItemsLists(boolean ignoreLosingItemsLists) {
        if (null != visionSocketClient) {
            visionSocketClient.setIgnoreLosingItemsLists(ignoreLosingItemsLists);
        }
        this.ignoreLosingItemsLists = ignoreLosingItemsLists;
    }

    private volatile long lastVisionUpdateTime = System.currentTimeMillis();

    @Override
    public XFutureVoid visionClientUpdateReceived(List<PhysicalItem> l, String line) {
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
        return Utils.runOnDispatchThread(() -> handleClientUpdateOnDisplay(l, finalDetailsMessage));
    }

    @UIEffect
    private void handleClientUpdateOnDisplay(List<PhysicalItem> l, @Nullable String detailsMessage) {
        setItems(l);
        if (null != detailsMessage) {
            jTextAreaConnectDetails.setText(detailsMessage);
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
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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

    private final CachedTextField pickupDistCachedTextField;

    /**
     * Set the value of pickupDist
     *
     * @param pickupDist new value of pickupDist
     */
    private void setPickupDist(double pickupDist) {
        updateTextFieldDouble(pickupDist, pickupDistCachedTextField, 0.005);
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

    private final CachedTextField dropOffThresholdCachedTextField;

    /**
     * Set the value of dropOffThreshold
     *
     * @param dropOffThreshold new value of dropOffThreshold
     */
    private void setDropOffThreshold(double dropOffThreshold) {
        updateTextFieldDouble(pickupDist, dropOffThresholdCachedTextField, 0.005);
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
        lastClosestDistanceIndexX = x;
        lastClosestDistanceIndexY = y;
        this.lastClosestDistanceIndexRet = ret;
        lastClosestDistanceIndexList = l;
        return ret;
    }

    private class PoseUpdateHistoryItem {

        final CRCLStatusType stat;
        @Nullable
        final CRCLCommandType cmd;
        final boolean isHoldingObjectExpected;
        final long time;
        final Point2D.@Nullable Double capturedPartPoint;
        final int captured_item_index;
        final DistIndex di;
        @Nullable
        final PhysicalItem closestItem;
        final long statReceiveTime;

        PoseUpdateHistoryItem(
                CRCLStatusType stat,
                @Nullable CRCLCommandType cmd,
                boolean isHoldingObjectExpected,
                long time,
                Point2D.@Nullable Double capturedPartPoint,
                int captured_item_index,
                DistIndex di,
                @Nullable PhysicalItem closestItem,
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
            return "\nPoseUpdateHistoryItem{ cmd=" + ((null != cmd) ? CRCLSocket.commandToSimpleString(cmd) : "null") + ", isHoldingObjectExpected=" + isHoldingObjectExpected + ", time=" + (time - poseUpdateHistoryTime) + ", stat=" + CRCLSocket.statusToPrettyString(stat) + '}';
        }
    }

    private static String fmtDouble(double d) {
        return String.format("%.3f", d);
    }

    private Object @Nullable [] poseUpdateHistoryRecordItems(PoseUpdateHistoryItem item) throws IOException {
        File cmdFile = null;
        String cmdClassName = "";
        String cmdFileName = "";
        CRCLCommandType cmd = item.cmd;
        long cmdId = -1;
        String cmdName = "";
        if (null != cmd) {
            cmdClassName = cmd.getClass().getSimpleName();
            if (cmdClassName.startsWith("crcl.base.")) {
                cmdClassName = cmdClassName.substring("crcl.base.".length());
            }
            cmdFile = aprsSystem.logCrclCommand("puh_" + cmdClassName, cmd);
            if (null != cmdFile) {
                cmdFileName = cmdFile.getCanonicalPath();
            }
            cmdId = cmd.getCommandID();
            String itemCmdName = cmd.getName();
            if (null != itemCmdName) {
                cmdName = itemCmdName;
            }
        }
        CRCLStatusType stat = item.stat;
        PointType point = CRCLPosemath.getPoint(stat);
        if (null == point) {
            return null;
        }
        File statFile = null;
        String statFileName = "";
        long statCmdId = -1;
        CommandStateEnumType state = CommandStateEnumType.CRCL_READY;
        if (null != stat) {
            CommandStatusType cs = stat.getCommandStatus();
            if (null != cs) {
                statCmdId = cs.getCommandID();
                state = cs.getCommandState();
            }
            statFile = aprsSystem.logCrclStatus("puh_", stat);
            if (null != statFile) {
                statFileName = statFile.getCanonicalPath();
            }
        }

        String closestItemXString = "NaN";
        String closestItemYString = "NaN";
        String closestItemName = "";
        PhysicalItem closestItem = item.closestItem;
        if (null != closestItem) {
            closestItemXString = fmtDouble(closestItem.x);
            closestItemYString = fmtDouble(closestItem.y);
            closestItemName = closestItem.getFullName();
        }
        return new Object[]{
            Utils.getTimeString(item.time),
            item.time - item.statReceiveTime,
            fmtDouble(point.getX()),
            fmtDouble(point.getY()),
            fmtDouble(point.getZ()),
            item.isHoldingObjectExpected,
            item.di.index,
            fmtDouble(item.di.dist),
            item.captured_item_index,
            closestItemXString,
            closestItemYString,
            closestItemName,
            cmdId,
            statCmdId,
            state,
            cmdName,
            cmdFileName,
            statFileName
        };
    }

    private static final String[] POSE_UPDATE_HISTORY_HEADER
            = new String[]{
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
        File f = createTempFile("puh_" + err, ".csv");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(f), CSVFormat.DEFAULT.withHeader(POSE_UPDATE_HISTORY_HEADER))) {
            for (PoseUpdateHistoryItem item : poseUpdateHistory) {
                Object array[] = poseUpdateHistoryRecordItems(item);
                if (null != array) {
                    printer.printRecord(array);
                }
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

    @Nullable
    private volatile ConveyorPosition lastConveyorPosition = null;

    public synchronized void handleConveyorPositionUpdate(ConveyorPosition newConveyorPosition) {
        if (null != lastConveyorPosition
                && Double.isFinite(lastConveyorPosition.x) 
                && Double.isFinite(lastConveyorPosition.y) 
                && null != newConveyorPosition
                && Double.isFinite(newConveyorPosition.x)
                && Double.isFinite(newConveyorPosition.y)
                && isSimulated()) {
            double xinc = newConveyorPosition.x - lastConveyorPosition.x;
            double yinc = newConveyorPosition.y - lastConveyorPosition.y;
            List<PhysicalItem> itemsCopy = new ArrayList<>(getItems());
            for (int i = 0; i < itemsCopy.size(); i++) {
                if (i != captured_item_index) {
                    PhysicalItem item = itemsCopy.get(i);
                    item.x += xinc;
                    item.y += yinc;
                }
            }
            setItems(itemsCopy, true);
        }
        lastConveyorPosition = newConveyorPosition;
    }

    @Override
    public void handlePoseUpdate(
            PendantClientJPanel panel,
            CRCLStatusType stat,
            @Nullable CRCLCommandType cmd,
            boolean isHoldingObjectExpected,
            long statRecievTime) {
        if (!showCurrentCachedCheckBox.isSelected()) {
            return;
        }
        PoseType pose = CRCLPosemath.getPose(stat);
        if (null == pose) {
            return;
        }
        PointType ptIn = requireNonNull(pose.getPoint(), "pose.getPoint()");

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
                        (min_dist_index >= 0 && min_dist_index < l.size()) ? l.get(min_dist_index) : null,
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
        if (simulatedCachedCheckBox.isSelected()) {
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
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
                        String err = "failed_to_capture_part_at_" + currentX + "_" + currentY + "_";
                        printHandlePoseErrorInfo(err, stat, pose, cmd);
                        if (takeSnapshots) {
                            takeSnapshot(createTempFile(err, ".PNG"), (PmCartesian) null, "");
                        }
                        System.err.println("Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);

                    } catch (Exception ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                        }
                    }
                }
                if (captured_item_index < 0) {
                    String err = "Should be dropping item but no item captured";
                    try {
                        printHandlePoseErrorInfo(err, stat, pose, cmd);
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
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
        if (simulatedCachedCheckBox.isSelected()) {
            if (captured_item_index >= 0 && captured_item_index < l.size()) {
                PhysicalItem item = l.get(captured_item_index);
                item.x = currentX;
                item.y = currentY;
                setItems(l, (isHoldingObjectExpected != lastIsHoldingObjectExpected) && simulationUpdateAsNeededCachedCheckBox.isSelected());
            } else if (isHoldingObjectExpected != lastIsHoldingObjectExpected) {
                setItems(l);
            }
        }
        lastIsHoldingObjectExpected = isHoldingObjectExpected;
    }

    private void printHandlePoseErrorInfo(String err, CRCLStatusType stat, PoseType pose, @Nullable CRCLCommandType cmd) throws IOException {
        System.out.println("poseUpdateHistory = " + printPoseUpdateHistory(err));
        System.out.println("statFile = " + aprsSystem.logCrclStatus(err, stat));
        System.out.println("pose = " + CRCLPosemath.toString(pose));
        if (null != cmd) {
            System.out.println("cmd = " + CRCLSocket.commandToSimpleString(cmd));
            System.out.println("cmdFile = " + aprsSystem.logCrclCommand(err, cmd));
        }
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
