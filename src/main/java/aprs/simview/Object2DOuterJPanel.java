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
import static aprs.misc.AprsCommonLogger.println;
import static aprs.misc.Utils.autoResizeTableColWidthsOnDisplay;
import crcl.base.CommandStateEnumType;
import crcl.base.CommandStatusType;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import crcl.ui.client.CrclSwingClientJPanel;
import crcl.ui.client.CurrentPoseListener;
import crcl.ui.misc.MultiLineStringJPanel;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import static java.lang.Double.parseDouble;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;
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
import static java.util.Objects.requireNonNull;
import org.checkerframework.checker.guieffect.qual.UI;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DOuterJPanel extends javax.swing.JPanel implements Object2DJFrameInterface, VisionSocketClient.VisionSocketClientListener {

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

    private final ConcurrentLinkedDeque<XFuture<List<PhysicalItem>>> futuresDeque = new ConcurrentLinkedDeque<>();

    public XFuture<List<PhysicalItem>> getSimViewUpdate() {

        XFuture<List<PhysicalItem>> xfl = new XFuture<>("getSimViewUpate");
        futuresDeque.add(xfl);
        refresh(false);
        return xfl;
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
        XFuture<List<PhysicalItem>> xfl = futuresDeque.poll();
        while (null != xfl) {
            xfl.complete(itemsList);
            xfl = futuresDeque.poll();
        }
        if (debugTimes) {
            long endTime = System.currentTimeMillis();
            long diff = endTime - startTime;
            long total = notifyItemsTableTime.addAndGet(diff);
            long max = notifyItemsTableMaxTime.getAndAccumulate(diff, Math::max);
            println("notifyItemsTable count = " + c);
            println("notifyItemsTable max = " + max);
            println("notifyItemsTable avg = " + total / c);
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

    public BufferedImage createSnapshotImage(ViewOptions opts) {
        return object2DJPanel1.createSnapshot(opts);
    }

    public BufferedImage createSnapshotImage(ViewOptions opts, Collection<? extends PhysicalItem> itemsToPaint) {
        return object2DJPanel1.createSnapshot(opts, itemsToPaint);
    }

    public List<PhysicalItem> getItems() {
        return object2DJPanel1.getItems();
    }

    public List<PhysicalItem> getOutputItems() {
        if (!isSimulated()) {
            return object2DJPanel1.getItems();
        }
        return object2DJPanel1.getOutputItems();
    }

    private volatile boolean settingItems = false;

    @Override
    public @Nullable
    File[] takeSnapshot(File f, PoseType pose, String label) {
        if (null != pose) {
            return takeSnapshot(f, pose.getPoint(), label);
        } else {
            return takeSnapshot(f, (PmCartesian) null, (String) null);
        }
    }

    @Override
    public @Nullable
    File[] takeSnapshot(File f, @Nullable PointType point, String label) {
        if (null != point) {
            return takeSnapshot(f, CRCLPosemath.toPmCartesian(point), label);
        } else {
            return takeSnapshot(f, (PmCartesian) null, (String) null);
        }
    }

    @Override
    @Nullable
    public File[] takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) {
        File csvFile = null;
        try {
            File csvDir = new File(f.getParentFile(), "csv");
            csvDir.mkdirs();
            csvFile = new File(csvDir, f.getName() + ".csv");
            this.object2DJPanel1.takeSnapshot(f,csvFile, point, label);
            
//            Object2DOuterJPanel.this.saveCsvItemsFile(csvFile);
            final File csvFileFinal = csvFile;
            runOnDispatchThread(() -> {
                updateSnapshotsTable(f, csvFileFinal);
            });
            AprsSystem aprsSystemLocal = aprsSystem;
            if (null != aprsSystemLocal) {
                File xmlDir = new File(f.getParentFile(), "crclStatusXml");
                xmlDir.mkdirs();
                CRCLStatusType status = aprsSystemLocal.getCurrentStatus();
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
        return new File[]{f, csvFile};
    }

    private final AtomicInteger snapshotsCount = new AtomicInteger();

    @UIEffect
    private void updateSnapshotsTable(File f, File csvFile) {
        if (null != f && null != csvFile) {
            try {
                DefaultTableModel model = (DefaultTableModel) jTableSnapshotFiles.getModel();
                while (model.getRowCount() > 200) {
                    model.removeRow(0);
                }
                String name = f.getName();
                int uindex1 = name.indexOf('_');
                if (uindex1 > 0 && uindex1 < (name.length() - 1)) {
                    name = name.substring(uindex1 + 1);
                }
                int uindex2 = name.lastIndexOf('_');
                if (uindex2 > 0) {
                    name = name.substring(0, uindex2);
                }
                int count = snapshotsCount.incrementAndGet();
                model.addRow(new Object[]{count, Utils.getTimeString(), name, f.getCanonicalPath(), csvFile.getCanonicalPath()});
                if (count % 20 < 2) {
                    Utils.autoResizeTableColWidths(jTableSnapshotFiles);
                }
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public @Nullable
    File[] takeSnapshot(File f, @Nullable PoseType pose, @Nullable String label, int w, int h) {
        if (null != pose) {
            return takeSnapshot(f, pose.getPoint(), label, w, h);
        } else {
            return takeSnapshot(f, (PmCartesian) null, (String) null, w, h);
        }
    }

    @Override
    public @Nullable
    File[] takeSnapshot(File f, @Nullable PointType point, @Nullable String label, int w, int h) {
        if (null != point) {
            return takeSnapshot(f, CRCLPosemath.toPmCartesian(point), label, w, h);
        } else {
            return takeSnapshot(f, (PmCartesian) null, (String) null);
        }
    }

    @Override
    public @Nullable
    File[] takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label, int w, int h) {
        File csvFile = null;
        try {
            File csvDir = new File(f.getParentFile(), "csv");
            csvDir.mkdirs();
            csvFile = new File(csvDir, f.getName() + ".csv");
            this.object2DJPanel1.takeSnapshot(f,csvFile, point, label, w, h);
            final File csvFileFinal = csvFile;
            runOnDispatchThread(() -> {
                updateSnapshotsTable(f, csvFileFinal);
            });
            File xmlDir = new File(f.getParentFile(), "crclStatusXml");
            xmlDir.mkdirs();
            AprsSystem aprsSystemLocal = this.aprsSystem;
            if (null != aprsSystemLocal) {
                CRCLStatusType status = aprsSystemLocal.getCurrentStatus();
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
        return new File[]{f, csvFile};
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

    private final ConcurrentLinkedDeque<Consumer<Integer>> incrementPublishCountListeners = new ConcurrentLinkedDeque<>();

    public void addPublishCountListener(Consumer<Integer> l) {
        if (null != visionSocketServer) {
            visionSocketServer.addPublishCountListener(l);
        } else {
            incrementPublishCountListeners.add(l);
        }
    }

    public void removePublishCountListener(Consumer<Integer> l) {
        if (null != visionSocketServer) {
            visionSocketServer.removePublishCountListener(l);
        }
    }

    private final CachedCheckBox simulatedCachedCheckBox;
    private final CachedTextField filenameCachedTextField;
    private final CachedCheckBox pauseCachedCheckBox;

    public XFutureVoid refresh(boolean loadFile) {
        try {
            if (simulatedCachedCheckBox.isSelected()) {
                boolean fileLoaded = false;
                XFutureVoid loadFileFuture = null;
                if (loadFile) {
                    String fname = filenameCachedTextField.getText().trim();
                    File f = new File(fname);
                    if (f.exists() && f.canRead()) {
                        try {
                            loadFileFuture = loadFile(f);
                            fileLoaded = true;
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                        }
                    }
                }
                if (!fileLoaded && null != visionSocketServer && !pauseCachedCheckBox.isSelected()) {
                    if (null != loadFileFuture) {
                        return loadFileFuture
                                .thenComposeToVoid(() -> this.setItems(object2DJPanel1.getItems()));
                    } else {
                        return this.setItems(object2DJPanel1.getItems());
                    }
                } else {
                    return XFutureVoid.completedFuture();
                }
            } else {
                return XFutureVoid.completedFuture();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            lastRefreshTime = System.currentTimeMillis();
            refreshCount.incrementAndGet();
        }
    }

    public XFutureVoid loadFile(File f) throws IOException {
        return loadFile(f, handleRotationEnum == HandleRotationEnum.DEGREES, handleRotationEnum == HandleRotationEnum.IGNORE);
    }

    public boolean isViewingOutput() {
        return viewOutputCachedCheckBox.isSelected();
    }

    public void setViewingOutput(boolean viewingOutput) {
        viewOutputCachedCheckBox.setSelected(viewingOutput);
    }

    private void setItemsFromClone(List<PhysicalItem> items) {
        Object2DOuterJPanel objectPanelToCloneLocal = objectPanelToClone;
        if (null == objectPanelToCloneLocal) {
            throw new NullPointerException("objectToClone");
        }
        XFutureVoid setItemsFuture
                = setItems(items, !pauseCachedCheckBox.isSelected());
        XFutureVoid ret = setItemsFuture;
        if (isViewingOutput()) {
            ret = setItemsFuture
                    .thenRun(() -> setOutputItems(objectPanelToCloneLocal.getOutputItems()));
        }
    }

    public XFutureVoid setItems(List<PhysicalItem> items) {
        return setItems(items, !pauseCachedCheckBox.isSelected());
    }

    private volatile @Nullable
    Map<String, Integer> origNamesMap = null;

    private volatile long lastSetItemsInternalTime = 0;
    private volatile @Nullable
    XFutureVoid lastSetItemsInternalFuture = null;

    public <T> XFutureVoid submitDisplayConsumer(Consumer<T> consumer, T value) {
        if (null != aprsSystem) {
            return aprsSystem.submitDisplayConsumer(consumer, value);
        }
        return runOnDispatchThread(() -> {
            consumer.accept(value);
        });
    }

    @UIEffect
    private void consumeItemList(List<PhysicalItem> items) {
        setItemsInternal(items);
        settingItems = false;
    }

    private XFutureVoid setItems(List<PhysicalItem> items, boolean publish) {
        if (!this.isSimulated() || !object2DJPanel1.isShowOutputItems() || !this.isConnected()) {
            notifySetItemsListeners(items);
        }
        long now = System.currentTimeMillis();
        XFutureVoid future = XFutureVoid.completedFuture();
        if (null == lastSetItemsInternalFuture
                || lastSetItemsInternalFuture.isDone()
                || (now - lastSetItemsInternalTime) > 500) {
            settingItems = true;
            lastSetItemsInternalTime = now;
            future = submitDisplayConsumer(this::consumeItemList, items);
            lastSetItemsInternalFuture = future;
        }
        if (captured_item_index > 0) {
            return XFutureVoid.completedFutureWithName("captured_item_index > 0");
        }
        if (null != draggedItem) {
            return XFutureVoid.completedFutureWithName("null != draggedItem");
        }
        if (null != draggedItemsList && !draggedItemsList.isEmpty()) {
            return XFutureVoid.completedFutureWithName("null != draggedItemsList && !draggedItemsList.isEmpty()");
        }
        if (publish) {
            VisionSocketServer srv = this.visionSocketServer;
            if (!pauseCachedCheckBox.isSelected()) {
                if (null != srv) {
                    publishCurrentItems();
                } else {
                    List<PhysicalItem> newOutputItems = computeNewOutputList(items);
                    future
                            = future
                                    .thenComposeToVoid(() -> setOutputItems(newOutputItems));
                    ;
                }
            }
        }
        return future;
    }

    private volatile long lastSetOutputItemsInternalTime = 0;
    private volatile @Nullable
    XFutureVoid lastSetOutputItemsInternalFuture = null;

    @UIEffect
    private void outputItemsListConsumer(List<PhysicalItem> items) {
        lastSetOutputItemsInternalTime = System.currentTimeMillis();
        setOutputItemsInternal(items);
        settingItems = false;
    }

    public XFutureVoid setOutputItems(List<PhysicalItem> items) {
        settingItems = true;
        long now = System.currentTimeMillis();
        if (null == lastSetOutputItemsInternalFuture
                || lastSetOutputItemsInternalFuture.isDone()
                || (now - lastSetOutputItemsInternalTime) > 500) {
            lastSetOutputItemsInternalTime = now;
            XFutureVoid ret = submitDisplayConsumer(this::outputItemsListConsumer, items);
            lastSetOutputItemsInternalFuture = ret;
            return ret;
        } else {
            settingItems = false;
            return XFutureVoid.completedFuture();
        }
    }

    @UIEffect
    private void setItemsInternal(List<PhysicalItem> items) {
        try {
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
            lastSetItemsInternalTime = System.currentTimeMillis();
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            showException(ex);
            try {
                disconnect();
            } catch (Exception ex2) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex2);
            }
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private void showException(Exception ex) {
        if (null != aprsSystem) {
            aprsSystem.showException(ex);
        }
    }

    private final AtomicInteger updateItemsTableCount = new AtomicInteger();
    private final AtomicLong updateItemsTableTime = new AtomicLong();
    private final AtomicLong updateItemsTableMaxTime = new AtomicLong();

    private void updateItemsTable(List<PhysicalItem> items) {
        settingItems = true;
        long startTime = System.currentTimeMillis();
        int c = updateItemsTableCount.incrementAndGet();
        runOnDispatchThread(() -> {
            updateItemsTableOnDisplay(items);
            settingItems = false;
            if (debugTimes) {
                long endTime = System.currentTimeMillis();
                long diff = endTime - startTime;
                long total = updateItemsTableTime.addAndGet(diff);
                long max = updateItemsTableMaxTime.getAndAccumulate(diff, Math::max);
                println("updateItemsTable count = " + c);
                println("updateItemsTable max = " + max);
                println("updateItemsTable avg = " + total / c);
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
            println("updateItemsTableOnDisplayCount = " + updateItemsTableOnDisplayCount);
            println("updateItemsTableOnDisplayCountMaxTime = " + updateItemsTableOnDisplayCountMaxTime);
            long averageUpdateItemsTableOnDisplay = updateItemsTableOnDisplayCountTotalTime / updateItemsTableOnDisplayCount;
            println("averageUpdateItemsTableOnDisplay = " + averageUpdateItemsTableOnDisplay);
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

    private final DefaultTableModel nonEditableTraySlotsTableModel;
    private final DefaultTableModel origTraySlotsTableModel;

    @UIEffect
    private void loadTraySlotInfo(List<PhysicalItem> items) {
        DefaultTableModel tm = (DefaultTableModel) jTableTraySlots.getModel();
        if (jTableItems.getRowCount() < 1) {
            if (tm != nonEditableTraySlotsTableModel) {
                jTableTraySlots.setEnabled(false);
                jLabelTraySlotInfoStatus.setText("Slots info disabled when row items table empty.");
                tm = nonEditableTraySlotsTableModel;
                jTableTraySlots.setModel(tm);
            }
            return;
        }

        if (object2DJPanel1.isShowOutputItems()) {
            if (tm != nonEditableTraySlotsTableModel) {
                jTableTraySlots.setEnabled(false);
                jLabelTraySlotInfoStatus.setText("Slots info disabled when Showing Output Items");
                tm = nonEditableTraySlotsTableModel;
                jTableTraySlots.setModel(tm);
            }
            return;
        } else {
            if (tm != origTraySlotsTableModel) {
                jTableTraySlots.setEnabled(true);
                jLabelTraySlotInfoStatus.setText("Tray Slot Information");
                tm = origTraySlotsTableModel;
                jTableTraySlots.setModel(tm);
            }
        }

        tm.setRowCount(0);
        for (int row = 0; row < jTableItems.getRowCount(); row++) {

            String type = (String) jTableItems.getValueAt(row, 5);
            Object nameObject = jTableItems.getValueAt(row, 1);
            if (null == type) {
                continue;
            }
            switch (type) {
                case "PT":
                case "KT":

                    Object xObject = jTableItems.getValueAt(row, 2);
                    Object yObject = jTableItems.getValueAt(row, 3);
                    Object rotObject = jTableItems.getValueAt(row, 4);
                    if (null != nameObject
                            && null != xObject
                            && null != yObject
                            && null != rotObject) {
                        String name = (String) nameObject;
                        double x = (double) xObject;
                        double y = (double) yObject;
                        double rot = toRadians((double) rotObject);
                        String trayInfo = String.format("%d,%s", row, name);
                        if (null != slotOffsetProvider) {
                            Tray trayItem = new Tray(name, rot, x, y);
                            jLabelTraySlotInfoStatus.setText("Tray: row=" + row + ", name=" + name + ", rot=" + rot + ", x=" + x + ", y=" + y);
                            List<Slot> l = slotOffsetProvider.getSlotOffsets(name, true);
                            if (null != l) {
                                for (Slot s : l) {
                                    Slot absItem = slotOffsetProvider.absSlotFromTrayAndOffset(trayItem, s);
                                    if (null != absItem) {
                                        double minDist = minDist(absItem.x, absItem.y, items);
                                        tm.addRow(new Object[]{minDist < 20.0, row, name, s.getSlotForSkuName(), absItem.x, absItem.y, minDist});
                                    }
                                }
                            }
                        }
                    }
                    break;

                default:
                    break;
            }
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
            if (isSimulated() && isConnected()) {
                notifySetItemsListeners(items);
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
        autoResizeTableColWidthsOnDisplay(jtable);
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

    public List<Slot> computeAbsSlotPositions(List<PhysicalItem> l) {
        return object2DJPanel1.computeAbsSlotPositions(l);
    }

    /**
     * Creates new form Object2DOuterJPanel
     */
    @SuppressWarnings({"initialization", "rawtypes", "unchecked"})
    @UIEffect
    public Object2DOuterJPanel() {
        initComponents();

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
        enforceSensorLimitsCachedCheckBox = new CachedCheckBox(jCheckBoxEnforceSensorLimits);
        shuffleSimulatedUpdatesCachedCheckBox = new CachedCheckBox(jCheckBoxShuffleSimulatedUpdates);
        posNoiseCachedTextField = new CachedTextField(jTextFieldPosNoise);
        mouseHideDistCachedTextField = new CachedTextField(jTextFieldHideNearMouseDist);
        robotHideDistCachedTextField = new CachedTextField(jTextFieldHideNearRobotDist);
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
        origTraySlotsTableModel = (DefaultTableModel) jTableTraySlots.getModel();
        Vector columnIdentifiers = new Vector();
        for (int i = 0; i < origTraySlotsTableModel.getColumnCount(); i++) {
            String string = POSE_UPDATE_HISTORY_HEADER[i];
            columnIdentifiers.add(origTraySlotsTableModel.getColumnName(i));
        }
        nonEditableTraySlotsTableModel = new DefaultTableModel(origTraySlotsTableModel.getDataVector(), columnIdentifiers) {
            @Override
            public void setValueAt(Object aValue, int row, int column) {
                origTraySlotsTableModel.setValueAt(aValue, row, column); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Object getValueAt(int row, int column) {
                return origTraySlotsTableModel.getValueAt(row, column); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getColumnName(int column) {
                return origTraySlotsTableModel.getColumnName(column); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getColumnCount() {
                return origTraySlotsTableModel.getColumnCount(); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getRowCount() {
                return origTraySlotsTableModel.getRowCount(); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void addColumn(Object columnName, Object[] columnData) {
                origTraySlotsTableModel.addColumn(columnName, columnData); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void addColumn(Object columnName, Vector columnData) {
                origTraySlotsTableModel.addColumn(columnName, columnData); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void addColumn(Object columnName) {
                origTraySlotsTableModel.addColumn(columnName); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setColumnCount(int columnCount) {
                origTraySlotsTableModel.setColumnCount(columnCount); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setColumnIdentifiers(Object[] newIdentifiers) {
                origTraySlotsTableModel.setColumnIdentifiers(newIdentifiers); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void setColumnIdentifiers(Vector columnIdentifiers) {
                origTraySlotsTableModel.setColumnIdentifiers(columnIdentifiers); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void removeRow(int row) {
                origTraySlotsTableModel.removeRow(row); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void moveRow(int start, int end, int to) {
                origTraySlotsTableModel.moveRow(start, end, to); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void insertRow(int row, Object[] rowData) {
                origTraySlotsTableModel.insertRow(row, rowData); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void insertRow(int row, Vector rowData) {
                origTraySlotsTableModel.insertRow(row, rowData); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void addRow(Object[] rowData) {
                origTraySlotsTableModel.addRow(rowData); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void addRow(Vector rowData) {
                origTraySlotsTableModel.addRow(rowData); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setRowCount(int rowCount) {
                origTraySlotsTableModel.setRowCount(rowCount); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setNumRows(int rowCount) {
                origTraySlotsTableModel.setNumRows(rowCount); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void rowsRemoved(TableModelEvent event) {
                origTraySlotsTableModel.rowsRemoved(event); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void newRowsAdded(TableModelEvent e) {
                origTraySlotsTableModel.newRowsAdded(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void newDataAvailable(TableModelEvent event) {
                origTraySlotsTableModel.newDataAvailable(event); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setDataVector(Object[][] dataVector, Object[] columnIdentifiers) {
                origTraySlotsTableModel.setDataVector(dataVector, columnIdentifiers); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void setDataVector(Vector dataVector, Vector columnIdentifiers) {
                origTraySlotsTableModel.setDataVector(dataVector, columnIdentifiers); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Vector getDataVector() {
                return origTraySlotsTableModel.getDataVector(); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
                return origTraySlotsTableModel.getListeners(listenerType); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableChanged(TableModelEvent e) {
                origTraySlotsTableModel.fireTableChanged(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableCellUpdated(int row, int column) {
                origTraySlotsTableModel.fireTableCellUpdated(row, column); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableRowsDeleted(int firstRow, int lastRow) {
                origTraySlotsTableModel.fireTableRowsDeleted(firstRow, lastRow); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableRowsUpdated(int firstRow, int lastRow) {
                origTraySlotsTableModel.fireTableRowsUpdated(firstRow, lastRow); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableRowsInserted(int firstRow, int lastRow) {
                origTraySlotsTableModel.fireTableRowsInserted(firstRow, lastRow); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableStructureChanged() {
                origTraySlotsTableModel.fireTableStructureChanged(); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void fireTableDataChanged() {
                origTraySlotsTableModel.fireTableDataChanged(); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public TableModelListener[] getTableModelListeners() {
                return origTraySlotsTableModel.getTableModelListeners(); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void removeTableModelListener(TableModelListener l) {
                origTraySlotsTableModel.removeTableModelListener(l); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void addTableModelListener(TableModelListener l) {
                origTraySlotsTableModel.addTableModelListener(l); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int findColumn(String columnName) {
                return origTraySlotsTableModel.findColumn(columnName); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Class getColumnClass(int columnIndex) {
                return origTraySlotsTableModel.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
        this.setItemsInternal(object2DJPanel1.getItems());
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
            if (object2DJPanel1.isShowOutputItems()) {
                return;
            }
            if (e.getColumn() == 0 && e.getType() == TableModelEvent.UPDATE) {
                int eventRowIndex = e.getFirstRow();
//                int row = jTableTraySlots.convertRowIndexToModel(eventRowIndex);
                int row = jTableTraySlots.convertRowIndexToView(eventRowIndex);
//                    String trayInfo = (String) jTableTraySlots.getValueAt(row, 0);
//                    String stringInfoFields[] = trayInfo.split(",");
                List<PhysicalItem> newItemsList = new ArrayList<>();
                Object traySlotValueFilled = jTableTraySlots.getValueAt(row, 0);
                if (!(traySlotValueFilled instanceof Boolean)) {
                    throw new IllegalStateException("bad value in table at " + row + ",1 :" + traySlotValueFilled);
                }
                boolean filled = (boolean) traySlotValueFilled;

                Object traySlotValueSx = jTableTraySlots.getValueAt(row, 4);
                if (!(traySlotValueSx instanceof Double)) {
                    throw new IllegalStateException("bad value in table at " + row + ",2 :" + traySlotValueSx);
                }
                Object traySlotValueSy = jTableTraySlots.getValueAt(row, 5);
                if (!(traySlotValueSy instanceof Double)) {
                    throw new IllegalStateException("bad value in table at " + row + ",3 :" + traySlotValueSy);
                }
                Object traySlotValueName = jTableTraySlots.getValueAt(row, 3);
                if (!(traySlotValueName instanceof String)) {
                    throw new IllegalStateException("bad value in table at " + row + ",0 :" + traySlotValueName);
                }
                String name = (String) traySlotValueName;
                List<PhysicalItem> origItems = getItems();
                if (filled) {
                    newItemsList.addAll(origItems);
                    double sx = (double) traySlotValueSx;
                    double sy = (double) traySlotValueSy;
                    PhysicalItem newPart = newPhysicalItemNameRotXYScoreType(name, 0.0, sx, sy, 100.0, "P");
                    newItemsList.add(newPart);
                } else {
                    double sx = (double) traySlotValueSx;
                    double sy = (double) traySlotValueSy;
                    for (int i = 0; i < origItems.size(); i++) {
                        PhysicalItem it = origItems.get(i);
                        if (Math.hypot(sx - it.x, sy - it.y) > 20.0 || !it.getName().contains(name)) {
                            newItemsList.add(it);
                        }
                    }
                }
                if (newItemsList.size() != origItems.size()) {
                    runOnDispatchThread(() -> {
                        setItemsInternal(newItemsList);
                        notifySetItemsListeners(newItemsList);
                    });
                } else {
                    System.err.println("List size " + newItemsList.size() + " not changed on jTableTraySlots event " + e);
                    println("newItemsList = " + newItemsList);
                    println("origItems = " + origItems);
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

        jPanelBottomMain = new javax.swing.JPanel();
        object2DJPanel1 = new aprs.simview.Object2DJPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
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
        jCheckBoxCloning = new javax.swing.JCheckBox();
        jCheckBoxLockTrays = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jTextFieldPrevListSizeDecrementInterval = new javax.swing.JTextField();
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
        jLabel9 = new javax.swing.JLabel();
        jTextFieldPickupDist = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldDropOffThreshold = new javax.swing.JTextField();
        jButtonForceUpdate = new javax.swing.JButton();
        jCheckBoxEnforceSensorLimits = new javax.swing.JCheckBox();
        jCheckBoxHideItemsNearMouse = new javax.swing.JCheckBox();
        jLabel11 = new javax.swing.JLabel();
        jTextFieldHideNearMouseDist = new javax.swing.JTextField();
        jCheckBoxHideItemsNearRobot = new javax.swing.JCheckBox();
        jLabel14 = new javax.swing.JLabel();
        jTextFieldHideNearRobotDist = new javax.swing.JTextField();
        jPanelTrays = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableTraySlots = new javax.swing.JTable();
        jLabelTraySlotInfoStatus = new javax.swing.JLabel();
        jPanelTopRow = new javax.swing.JPanel();
        jTextFieldFilename = new javax.swing.JTextField();
        jButtonSave = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jPanelItems = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableItems = new javax.swing.JTable();
        jPanelItemsButtons = new javax.swing.JPanel();
        jButtonAdd = new javax.swing.JButton();
        jButtonCurrent = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jCheckBoxViewOutput = new javax.swing.JCheckBox();
        jPanelOptionsTab = new javax.swing.JPanel();
        jTextFieldCurrentXY = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jButtonOffsetAll = new javax.swing.JButton();
        jButtonReset = new javax.swing.JButton();
        jCheckBoxShowRotations = new javax.swing.JCheckBox();
        jCheckBoxShowCurrent = new javax.swing.JCheckBox();
        jComboBoxDisplayAxis = new javax.swing.JComboBox<>();
        jTextFieldMaxXMaxY = new javax.swing.JTextField();
        jCheckBoxSeparateNames = new javax.swing.JCheckBox();
        jCheckBoxAutoscale = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldMinXMinY = new javax.swing.JTextField();
        jCheckBoxAddSlots = new javax.swing.JCheckBox();
        jCheckBoxDetails = new javax.swing.JCheckBox();
        jCheckBoxTools = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jCheckBoxShowOverlapping = new javax.swing.JCheckBox();
        jCheckBoxShowOnlyOverlapping = new javax.swing.JCheckBox();
        jPanelProperties = new javax.swing.JPanel();
        jScrollPaneProperties = new javax.swing.JScrollPane();
        jTableProperties = new javax.swing.JTable();
        jPanelBottomPropertiesButtons = new javax.swing.JPanel();
        jButtonReadProperties = new javax.swing.JButton();
        jButtonSetProperies = new javax.swing.JButton();
        jPanelSnapshots = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTableSnapshotFiles = new javax.swing.JTable();
        jButtonViewSnapshotImage = new javax.swing.JButton();
        jButtonViewSnapshotCsv = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTableLineLog = new javax.swing.JTable();
        jCheckBoxRecordLines = new javax.swing.JCheckBox();
        jTextFieldRecordLinesFile = new javax.swing.JTextField();
        jButtonOpenLogLinesFile = new javax.swing.JButton();
        jButtonShowSelectedLogLine = new javax.swing.JButton();
        jButtonLineLogPrev = new javax.swing.JButton();
        jButtonLineLogNext = new javax.swing.JButton();
        jButtonSeperateLineLogWindow = new javax.swing.JButton();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

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
        object2DJPanel1.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                object2DJPanel1ComponentResized(evt);
            }
        });

        javax.swing.GroupLayout object2DJPanel1Layout = new javax.swing.GroupLayout(object2DJPanel1);
        object2DJPanel1.setLayout(object2DJPanel1Layout);
        object2DJPanel1Layout.setHorizontalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 466, Short.MAX_VALUE)
        );
        object2DJPanel1Layout.setVerticalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

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

        jCheckBoxCloning.setText("Cloning");
        jCheckBoxCloning.setEnabled(false);

        jCheckBoxLockTrays.setText("Lock Trays");
        jCheckBoxLockTrays.setEnabled(false);
        jCheckBoxLockTrays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxLockTraysActionPerformed(evt);
            }
        });

        jLabel15.setText("Wait For All Parts Time(ms): ");

        jTextFieldPrevListSizeDecrementInterval.setText("2000  ");
        jTextFieldPrevListSizeDecrementInterval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPrevListSizeDecrementIntervalActionPerformed(evt);
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
                        .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelHost)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldHost))
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonRefresh)
                                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                        .addComponent(jLabel12)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jComboBoxHandleRotationsEnum, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel15)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldPrevListSizeDecrementInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(12, 12, 12))
                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                        .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3)
                            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                        .addComponent(jCheckBoxSimulated)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jCheckBoxConnected, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jCheckBoxCloning))
                                    .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                                        .addComponent(jCheckBoxDebug)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jCheckBoxPause)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel13)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jCheckBoxLockTrays)))
                                .addGap(0, 125, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        jPanelConnectionsTabLayout.setVerticalGroup(
            jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConnectionsTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulated)
                    .addComponent(jCheckBoxConnected)
                    .addComponent(jCheckBoxCloning))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHost)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxDebug)
                    .addComponent(jCheckBoxPause)
                    .addComponent(jLabel13)
                    .addComponent(jTextFieldRotationOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxLockTrays))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConnectionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jComboBoxHandleRotationsEnum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(jTextFieldPrevListSizeDecrementInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRefresh)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Connections", jPanelConnectionsTab);

        jLabel5.setText("Frequency (in ms)  :");

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
        jCheckBoxSimulationUpdateAsNeeded.setText("Update only as needed.");
        jCheckBoxSimulationUpdateAsNeeded.setEnabled(false);
        jCheckBoxSimulationUpdateAsNeeded.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulationUpdateAsNeededActionPerformed(evt);
            }
        });

        jLabel6.setText("Drop Out Rate  : ");

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

        jButtonForceUpdate.setText("Force Update");
        jButtonForceUpdate.setEnabled(false);
        jButtonForceUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonForceUpdateActionPerformed(evt);
            }
        });

        jCheckBoxEnforceSensorLimits.setText("Enforce Sensor Limits");
        jCheckBoxEnforceSensorLimits.setEnabled(false);

        jCheckBoxHideItemsNearMouse.setText("Hide Items Near Mouse");
        jCheckBoxHideItemsNearMouse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxHideItemsNearMouseActionPerformed(evt);
            }
        });

        jLabel11.setText("Near Mouse Dist: ");

        jTextFieldHideNearMouseDist.setText("100.0 ");
        jTextFieldHideNearMouseDist.setEnabled(false);
        jTextFieldHideNearMouseDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldHideNearMouseDistActionPerformed(evt);
            }
        });

        jCheckBoxHideItemsNearRobot.setText("Hide Items Near Robot");
        jCheckBoxHideItemsNearRobot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxHideItemsNearRobotActionPerformed(evt);
            }
        });

        jLabel14.setText("Near Robot Dist: ");

        jTextFieldHideNearRobotDist.setText("100.0 ");
        jTextFieldHideNearRobotDist.setEnabled(false);
        jTextFieldHideNearRobotDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldHideNearRobotDistActionPerformed(evt);
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
                        .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonForceUpdate))
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldSimulationUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jCheckBoxShuffleSimulatedUpdates)
                            .addComponent(jCheckBoxAddPosNoise)
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldRotNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldDropOffThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldPickupDist, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldSimDropRate, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jCheckBoxEnforceSensorLimits)
                            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldHideNearMouseDist, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldHideNearRobotDist, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxHideItemsNearMouse)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxHideItemsNearRobot)
                        .addGap(54, 54, 54))))
        );
        jPanelSimulationTabLayout.setVerticalGroup(
            jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSimulationTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextFieldSimulationUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulationUpdateAsNeeded)
                    .addComponent(jButtonForceUpdate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxShuffleSimulatedUpdates)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxAddPosNoise)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPosNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextFieldRotNoise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTextFieldDropOffThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jTextFieldPickupDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldSimDropRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxEnforceSensorLimits)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxHideItemsNearMouse)
                    .addComponent(jCheckBoxHideItemsNearRobot))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel14)
                        .addComponent(jTextFieldHideNearRobotDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelSimulationTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel11)
                        .addComponent(jTextFieldHideNearMouseDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Simulation", jPanelSimulationTab);

        jTableTraySlots.setAutoCreateRowSorter(true);
        jTableTraySlots.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Fill", "Tray Index", "TrayName", "Slot Name", "sx", "sy", "dist"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, true, true, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTableTraySlots);

        jLabelTraySlotInfoStatus.setText("Tray Slot Info Status");

        javax.swing.GroupLayout jPanelTraysLayout = new javax.swing.GroupLayout(jPanelTrays);
        jPanelTrays.setLayout(jPanelTraysLayout);
        jPanelTraysLayout.setHorizontalGroup(
            jPanelTraysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTraysLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTraysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jLabelTraySlotInfoStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelTraysLayout.setVerticalGroup(
            jPanelTraysLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTraysLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelTraySlotInfoStatus)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Trays", jPanelTrays);

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
                .addGroup(jPanelTopRowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldFilename, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                    .addGroup(jPanelTopRowLayout.createSequentialGroup()
                        .addComponent(jButtonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLoad)))
                .addGap(15, 15, 15))
        );
        jPanelTopRowLayout.setVerticalGroup(
            jPanelTopRowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTopRowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTextFieldFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelTopRowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSave)
                    .addComponent(jButtonLoad))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Files", jPanelTopRow);

        jTableItems.setAutoCreateRowSorter(true);
        jTableItems.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

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

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jButtonCurrent.setText("Current");
        jButtonCurrent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCurrentActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelItemsButtonsLayout = new javax.swing.GroupLayout(jPanelItemsButtons);
        jPanelItemsButtons.setLayout(jPanelItemsButtonsLayout);
        jPanelItemsButtonsLayout.setHorizontalGroup(
            jPanelItemsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelItemsButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonCurrent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDelete)
                .addGap(4, 4, 4)
                .addComponent(jButtonAdd)
                .addContainerGap())
        );
        jPanelItemsButtonsLayout.setVerticalGroup(
            jPanelItemsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelItemsButtonsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelItemsButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonDelete)
                    .addComponent(jButtonAdd)
                    .addComponent(jButtonCurrent))
                .addContainerGap())
        );

        jPanelItemsButtonsLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonAdd, jButtonDelete});

        jCheckBoxViewOutput.setText("View Output");
        jCheckBoxViewOutput.setEnabled(false);
        jCheckBoxViewOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxViewOutputActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelItemsLayout = new javax.swing.GroupLayout(jPanelItems);
        jPanelItems.setLayout(jPanelItemsLayout);
        jPanelItemsLayout.setHorizontalGroup(
            jPanelItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelItemsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanelItemsLayout.createSequentialGroup()
                        .addComponent(jPanelItemsButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBoxViewOutput)))
                .addContainerGap())
        );
        jPanelItemsLayout.setVerticalGroup(
            jPanelItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelItemsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelItemsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelItemsButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelItemsLayout.createSequentialGroup()
                        .addComponent(jCheckBoxViewOutput)
                        .addContainerGap())))
        );

        jTabbedPane1.addTab("Items", jPanelItems);

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
        jCheckBoxShowRotations.setText("Show Rots/Images");
        jCheckBoxShowRotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowRotationsActionPerformed(evt);
            }
        });

        jCheckBoxShowCurrent.setText("Show Current: ");
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

        jLabel3.setText("Xmax,Ymax");

        jTextFieldMinXMinY.setText("200.0, -315.0");
        jTextFieldMinXMinY.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldMinXMinYActionPerformed(evt);
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

        jLabel4.setText("Axis: ");

        jCheckBoxShowOverlapping.setSelected(true);
        jCheckBoxShowOverlapping.setText("Show Overlapping");
        jCheckBoxShowOverlapping.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowOverlappingActionPerformed(evt);
            }
        });

        jCheckBoxShowOnlyOverlapping.setText("Show Only Overlapping");
        jCheckBoxShowOnlyOverlapping.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxShowOnlyOverlappingActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelOptionsTabLayout = new javax.swing.GroupLayout(jPanelOptionsTab);
        jPanelOptionsTab.setLayout(jPanelOptionsTabLayout);
        jPanelOptionsTabLayout.setHorizontalGroup(
            jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxDetails)
                        .addGap(3, 3, 3)
                        .addComponent(jCheckBoxTools)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxAddSlots)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxSeparateNames)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxShowOverlapping))
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(12, 12, 12)
                        .addComponent(jComboBoxDisplayAxis, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxShowCurrent)
                        .addGap(6, 6, 6)
                        .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(49, 49, 49)
                        .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(46, 46, 46)
                        .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jButtonOffsetAll)
                        .addGap(6, 6, 6)
                        .addComponent(jButtonReset))
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addComponent(jCheckBoxShowRotations)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxAutoscale)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxShowOnlyOverlapping)))
                .addContainerGap(43, Short.MAX_VALUE))
        );
        jPanelOptionsTabLayout.setVerticalGroup(
            jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxDetails)
                    .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCheckBoxTools)
                        .addComponent(jCheckBoxAddSlots)
                        .addComponent(jCheckBoxSeparateNames)
                        .addComponent(jCheckBoxShowOverlapping)))
                .addGap(6, 6, 6)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxShowRotations)
                    .addComponent(jCheckBoxAutoscale)
                    .addComponent(jCheckBoxShowOnlyOverlapping))
                .addGap(6, 6, 6)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel4))
                    .addComponent(jComboBoxDisplayAxis, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jCheckBoxShowCurrent))
                    .addComponent(jTextFieldCurrentXY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel3))
                    .addComponent(jTextFieldMaxXMaxY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelOptionsTabLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel2))
                    .addComponent(jTextFieldMinXMinY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanelOptionsTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonOffsetAll)
                    .addComponent(jButtonReset)))
        );

        jTabbedPane1.addTab("Options", jPanelOptionsTab);

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

        jPanelBottomPropertiesButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButtonReadProperties.setText("Read");
        jButtonReadProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReadPropertiesActionPerformed(evt);
            }
        });
        jPanelBottomPropertiesButtons.add(jButtonReadProperties);

        jButtonSetProperies.setText("Set");
        jButtonSetProperies.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetProperiesActionPerformed(evt);
            }
        });
        jPanelBottomPropertiesButtons.add(jButtonSetProperies);

        javax.swing.GroupLayout jPanelPropertiesLayout = new javax.swing.GroupLayout(jPanelProperties);
        jPanelProperties.setLayout(jPanelPropertiesLayout);
        jPanelPropertiesLayout.setHorizontalGroup(
            jPanelPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPropertiesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneProperties)
                    .addComponent(jPanelBottomPropertiesButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelPropertiesLayout.setVerticalGroup(
            jPanelPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPropertiesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneProperties, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBottomPropertiesButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Properties", jPanelProperties);

        jTableSnapshotFiles.setAutoCreateRowSorter(true);
        jTableSnapshotFiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Index", "Time", "Name", "Image File", "CSV File"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(jTableSnapshotFiles);

        jButtonViewSnapshotImage.setText("View Image");
        jButtonViewSnapshotImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonViewSnapshotImageActionPerformed(evt);
            }
        });

        jButtonViewSnapshotCsv.setText("View CSV");
        jButtonViewSnapshotCsv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonViewSnapshotCsvActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelSnapshotsLayout = new javax.swing.GroupLayout(jPanelSnapshots);
        jPanelSnapshots.setLayout(jPanelSnapshotsLayout);
        jPanelSnapshotsLayout.setHorizontalGroup(
            jPanelSnapshotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSnapshotsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSnapshotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(jPanelSnapshotsLayout.createSequentialGroup()
                        .addComponent(jButtonViewSnapshotImage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonViewSnapshotCsv)))
                .addContainerGap())
        );
        jPanelSnapshotsLayout.setVerticalGroup(
            jPanelSnapshotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSnapshotsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelSnapshotsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonViewSnapshotImage)
                    .addComponent(jButtonViewSnapshotCsv))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Snapshots", jPanelSnapshots);

        jTableLineLog.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane5.setViewportView(jTableLineLog);

        jCheckBoxRecordLines.setText("Record Lines");
        jCheckBoxRecordLines.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxRecordLinesActionPerformed(evt);
            }
        });

        jButtonOpenLogLinesFile.setText("Open");
        jButtonOpenLogLinesFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenLogLinesFileActionPerformed(evt);
            }
        });

        jButtonShowSelectedLogLine.setText("Show");
        jButtonShowSelectedLogLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonShowSelectedLogLineActionPerformed(evt);
            }
        });

        jButtonLineLogPrev.setText("Prev");
        jButtonLineLogPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLineLogPrevActionPerformed(evt);
            }
        });

        jButtonLineLogNext.setText("Next");
        jButtonLineLogNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLineLogNextActionPerformed(evt);
            }
        });

        jButtonSeperateLineLogWindow.setText("Separate");
        jButtonSeperateLineLogWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSeperateLineLogWindowActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jCheckBoxRecordLines)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonOpenLogLinesFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonShowSelectedLogLine)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLineLogPrev)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLineLogNext)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSeperateLineLogWindow)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jTextFieldRecordLinesFile, javax.swing.GroupLayout.PREFERRED_SIZE, 452, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonOpenLogLinesFile)
                    .addComponent(jCheckBoxRecordLines)
                    .addComponent(jButtonShowSelectedLogLine)
                    .addComponent(jButtonLineLogPrev)
                    .addComponent(jButtonLineLogNext)
                    .addComponent(jButtonSeperateLineLogWindow))
                .addGap(9, 9, 9)
                .addComponent(jTextFieldRecordLinesFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Line Log", jPanel1);

        javax.swing.GroupLayout jPanelBottomMainLayout = new javax.swing.GroupLayout(jPanelBottomMain);
        jPanelBottomMain.setLayout(jPanelBottomMainLayout);
        jPanelBottomMainLayout.setHorizontalGroup(
            jPanelBottomMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBottomMainLayout.createSequentialGroup()
                .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 468, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanelBottomMainLayout.setVerticalGroup(
            jPanelBottomMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelBottomMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelBottomMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
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

    private double mouseHideDist = 100.0;

    /**
     * Get the value of mouseHideDist
     *
     * @return the value of mouseHideDist
     */
    public double getMouseHideDist() {
        return mouseHideDist;
    }

    private final CachedTextField mouseHideDistCachedTextField;

    /**
     * Set the value of mouseHideDist
     *
     * @param mouseHideDist new value of mouseHideDist
     */
    private void setMouseHideDist(double mouseHideDist) {
        updateTextFieldDouble(mouseHideDist, mouseHideDistCachedTextField, 0.01);
        this.mouseHideDist = mouseHideDist;
    }

    private double robotHideDist = 100.0;

    /**
     * Get the value of robotHideDist
     *
     * @return the value of robotHideDist
     */
    public double getRobotHideDist() {
        return robotHideDist;
    }

    private final CachedTextField robotHideDistCachedTextField;

    /**
     * Set the value of robotHideDist
     *
     * @param robotHideDist new value of robotHideDist
     */
    private void setRobotHideDist(double robotHideDist) {
        updateTextFieldDouble(robotHideDist, robotHideDistCachedTextField, 0.01);
        this.robotHideDist = robotHideDist;
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

    private XFutureVoid runOnDispatchThread(@UI Runnable r) {
        if (null != aprsSystem) {
            return aprsSystem.runOnDispatchThread(r);
        } else {
            return Utils.runOnDispatchThread(r);
        }
    }

    private void setSimulatedInternal(boolean simulated) {
        runOnDispatchThread(() -> setSimulatedInternalOnDisplay(simulated));
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
        jButtonForceUpdate.setEnabled(simulated);
        jPanelSimulationTab.setEnabled(simulated);
        jTextFieldSimDropRate.setEnabled(simulated);
        jTextFieldSimDropRate.setEditable(simulated);
        shuffleSimulatedUpdatesCachedCheckBox.setEnabled(simulated);
        addPosNoiseCachedCheckBox.setEnabled(simulated);
        enforceSensorLimitsCachedCheckBox.setEnabled(simulated);
        jCheckBoxViewOutput.setEnabled(simulated);
        jCheckBoxHideItemsNearMouse.setEnabled(simulated);
        jCheckBoxHideItemsNearRobot.setEnabled(simulated);
        jTextFieldHideNearMouseDist.setEnabled(simulated && jCheckBoxHideItemsNearMouse.isSelected());
        jTextFieldHideNearRobotDist.setEnabled(simulated && jCheckBoxHideItemsNearRobot.isSelected());
        jTextFieldPosNoise.setEditable(simulated && addPosNoiseCachedCheckBox.isSelected());
        jTextFieldPosNoise.setEnabled(simulated && addPosNoiseCachedCheckBox.isSelected());
        jTextFieldRotNoise.setEditable(simulated && addPosNoiseCachedCheckBox.isSelected());
        jTextFieldRotNoise.setEnabled(simulated && addPosNoiseCachedCheckBox.isSelected());
        object2DJPanel1.setShowOutputItems(simulated && viewOutputCachedCheckBox.isSelected());
        final boolean canLockTrays = !simulated && isConnected();
        jCheckBoxLockTrays.setEnabled(canLockTrays);
        if (!canLockTrays) {
            jCheckBoxLockTrays.setSelected(false);
        }
        if (simulated) {
            jTextFieldHost.setEditable(false);
            jTextFieldHost.setEnabled(false);
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

    private @Nullable
    Object2DOuterJPanel objectPanelToClone = null;

    /**
     * Get the value of objectPanelToClone
     *
     * @return the value of objectPanelToClone
     */
    public @Nullable
    Object2DOuterJPanel getObjectPanelToClone() {
        return objectPanelToClone;
    }

    /**
     * Set the value of objectPanelToClone
     *
     * @param objectPanelToClone new value of objectPanelToClone
     */
    @UIEffect
    public XFutureVoid setObjectPanelToClone(Object2DOuterJPanel objectPanelToClone) {
        this.objectPanelToClone = objectPanelToClone;
        boolean cloning = null != objectPanelToClone;
        jCheckBoxCloning.setSelected(cloning);
        jCheckBoxConnected.setEnabled(!cloning);
        jCheckBoxSimulated.setEnabled(!cloning);
        if (null != objectPanelToClone) {
            disconnect();
            jCheckBoxConnected.setSelected(objectPanelToClone.isConnected());
            jCheckBoxSimulated.setSelected(objectPanelToClone.isSimulated());
            hostCachedTextField.setText(objectPanelToClone.getHost());
            portCachedTextField.setText(Integer.toString(objectPanelToClone.getPort()));
            AprsSystem copiedSys = objectPanelToClone.getAprsSystem();
            if (null != copiedSys) {
                setAprsSystem(copiedSys);
            }
            setPropertiesFile(objectPanelToClone.getPropertiesFile());
            setViewingOutput(objectPanelToClone.isViewingOutput());
            return objectPanelToClone.getProperties()
                    .thenAccept(this::loadProperties)
                    .thenRun(() -> {
                        objectPanelToClone.addSetItemsListener(this::setItemsFromClone);
                    });
        } else {
            return XFutureVoid.completedFuture();
        }
    }

    private @Nullable
    VisionSocketServer visionSocketServer = null;
    private @Nullable
    VisionSocketClient visionSocketClient = null;

    @UIEffect
    private void jCheckBoxConnectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxConnectedActionPerformed
        try {
            hostCachedTextField.syncText();
            portCachedTextField.syncText();
            jButtonReset.setEnabled(false);
            if (this.jCheckBoxConnected.isSelected()) {
                connect();
                final boolean canLockTrays = !isSimulated() && isConnected();
                jCheckBoxLockTrays.setEnabled(canLockTrays);
                if (!canLockTrays) {
                    jCheckBoxLockTrays.setSelected(false);
                    if (null != visionSocketClient) {
                        visionSocketClient.setLockTrays(false);
                    }
                }

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
                MultiLineStringJPanel.showText(message);
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
        if (null != connectedCachedCheckBox && connectedCachedCheckBox.isSelected()) {
            connectedCachedCheckBox.setSelected(false);
        }
        max_time_diff = 0;
    }

    private final CachedTextField hostCachedTextField;
    private final CachedTextField portCachedTextField;
    private final CachedCheckBox debugCachedCheckBox;

    public String getHost() {
        return hostCachedTextField.getText();
    }

    public int getPort() {
        return Integer.parseInt(portCachedTextField.getText());
    }

    private void connect() {
        if (simulatedCachedCheckBox.isSelected()) {
            try {
                if (getObjectPanelToClone() != null) {
                    return;
                }
                int port = Integer.parseInt(portCachedTextField.getText().trim());
                VisionSocketServer visionSocketServerLocal = visionSocketServer;
                if (null != visionSocketServerLocal && visionSocketServerLocal.getPort() != port) {
                    disconnect();
                    visionSocketServerLocal = null;
                }
                if (null == visionSocketServerLocal) {
                    visionSocketServerLocal = new VisionSocketServer(port);
                    for (Consumer<Integer> l : this.incrementPublishCountListeners) {
                        visionSocketServerLocal.addPublishCountListener(l);
                    }
                    this.visionSocketServer = visionSocketServerLocal;
                }
                visionSocketServerLocal.setDebug(debugCachedCheckBox.isSelected());
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
            clnt.setPrevListSizeDecrementInterval(prevListSizeDecrementInterval);
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
        max_time_diff = 0;
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

    private int dropCount = 0;

    private boolean dropFilter(PhysicalItem physicalItem) {
        if (simulatedDropRate < 0.001) {
            return true;
        }
        boolean ret = dropRandom.nextDouble() > simulatedDropRate;
        if (!ret) {
            dropCount++;
        }
        return ret;
    }

    private boolean mouseDistFilter(PhysicalItem physicalItem) {
        if (!jCheckBoxHideItemsNearMouse.isSelected()) {
            return true;
        } else {
            Point2D.Double mouseWorldPoint = object2DJPanel1.getMouseWorldMousePoint();
            if (!Double.isFinite(mouseWorldPoint.x) || !Double.isFinite(mouseWorldPoint.y)) {
                return true;
            }
            final double distToMousePoint = physicalItem.dist(mouseWorldPoint.x, mouseWorldPoint.y);
            if (distToMousePoint < mouseHideDist) {
                return false;
            } else {
                return true;
            }
        }
    }

    private boolean robotDistFilter(PhysicalItem physicalItem) {
        if (!jCheckBoxHideItemsNearRobot.isSelected()) {
            return true;
        } else {
            Point2D.Double robotWorldPoint = new Point2D.Double(object2DJPanel1.getCurrentX(), object2DJPanel1.getCurrentY());
            if (!Double.isFinite(robotWorldPoint.x) || !Double.isFinite(robotWorldPoint.y)) {
                return true;
            }
            final double distToMousePoint = physicalItem.dist(robotWorldPoint.x, robotWorldPoint.y);
            if (distToMousePoint < robotHideDist) {
                return false;
            } else {
                return true;
            }
        }
    }

    private boolean limitsFilter(PhysicalItem physicalItem) {
        double itemX = physicalItem.x;
        double minX = object2DJPanel1.getSenseMinX();
        if (Double.isFinite(minX) && itemX < minX) {
            return false;
        }
        double maxX = object2DJPanel1.getSenseMaxX();
        if (Double.isFinite(maxX) && itemX > maxX) {
            return false;
        }
        double itemY = physicalItem.y;
        double minY = object2DJPanel1.getSenseMinY();
        if (Double.isFinite(minY) && itemY < minY) {
            return false;
        }
        double maxY = object2DJPanel1.getSenseMaxY();
        if (Double.isFinite(maxY) && itemY > maxY) {
            return false;
        }
        return true;
    }

    private final Random posRandom = new Random();
    private final CachedCheckBox addPosNoiseCachedCheckBox;

    private final CachedCheckBox enforceSensorLimitsCachedCheckBox;

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

    /**
     * Get the value of enforceSensorLimits
     *
     * @return the value of enforceSensorLimits
     */
    public boolean isEnforceSensorLimits() {
        return enforceSensorLimitsCachedCheckBox.isSelected();
    }

    /**
     * Set the value of enforceSensorLimits
     *
     * @param enforceSensorLimits new value of enforceSensorLimits
     */
    public void setEnforceSensorLimits(boolean enforceSensorLimits) {
        enforceSensorLimitsCachedCheckBox.setSelected(enforceSensorLimits);
    }

    private final AtomicInteger simUpdateCount = new AtomicInteger();

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
        if (null != getObjectPanelToClone()) {
            return;
        }
        VisionSocketServer srv = this.visionSocketServer;
        if (null == srv) {
            throw new IllegalStateException("visionSocketServer is null");
        }

        List<PhysicalItem> origList = getItems();
        List<PhysicalItem> newOutputList = computeNewOutputList(origList);
        srv.publishList("simupdate" + simUpdateCount.incrementAndGet() + ",,,,,P,", newOutputList);
        setOutputItems(newOutputList);
    }

    private boolean isOutputFilteringNeeded() {
        boolean addPosNoise = addPosNoiseCachedCheckBox.isSelected();
        boolean shuffleSimulatedUpdates = shuffleSimulatedUpdatesCachedCheckBox.isSelected();
        boolean needOutputFiltering = shuffleSimulatedUpdates
                || isEnforceSensorLimits()
                || simulatedDropRate > 0.01 || addPosNoise;
        return needOutputFiltering;
    }

    private List<PhysicalItem> computeNewOutputList(List<PhysicalItem> origList) {
        List<PhysicalItem> newOutputList = new ArrayList<>(origList);
        boolean addPosNoise = addPosNoiseCachedCheckBox.isSelected();
        boolean shuffleSimulatedUpdates = shuffleSimulatedUpdatesCachedCheckBox.isSelected();
        boolean needOutputFiltering = shuffleSimulatedUpdates
                || isEnforceSensorLimits()
                || simulatedDropRate > 0.01 || addPosNoise;
        if (needOutputFiltering) {
            newOutputList = newOutputList.stream()
                    .filter(this::dropFilter)
                    .filter(this::mouseDistFilter)
                    .filter(this::robotDistFilter)
                    .filter(this::limitsFilter)
                    .map(this::noiseFilter)
                    .collect(Collectors.toList());
            if (shuffleSimulatedUpdates) {
                Collections.shuffle(newOutputList);
            }
        }
        return newOutputList;
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

    public boolean isUserMouseDown() {
        return null != this.draggedItem || object2DJPanel1.isMouseDown();
    }

    private volatile long mouseDraggedUpdateTableTime = -1;

    @UIEffect
    private void object2DJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseDragged
        object2DJPanel1.setMouseDown(true);
        double scale = object2DJPanel1.getScale(object2DJPanel1.isAutoscale());
        double min_x = object2DJPanel1.getMinX();
        double max_x = object2DJPanel1.getMaxX();
        double min_y = object2DJPanel1.getMinY();
        double max_y = object2DJPanel1.getMaxY();
        PhysicalItem itemToDrag = this.draggedItem;
        mouseDragTime = System.currentTimeMillis();
        if (null != itemToDrag) {
            double orig_x = itemToDrag.x;
            double orig_y = itemToDrag.y;
            Point2D.Double worldPoint = object2DJPanel1.screenToWorldPoint(evt.getX(), evt.getY(), object2DJPanel1.isAutoscale());
            itemToDrag.x = worldPoint.x;
            itemToDrag.y = worldPoint.y;
//            switch (object2DJPanel1.getDisplayAxis()) {
//                case POS_X_POS_Y:
//                    itemToDrag.x = ((evt.getX() - 15) / scale) + min_x;
//                    itemToDrag.y = max_y - ((evt.getY() - 20) / scale);
//                    break;
//
//                case POS_Y_NEG_X:
//                    itemToDrag.x = ((evt.getY() - 20) / scale) + min_x;
//                    itemToDrag.y = ((evt.getX() - 15) / scale) + min_y;
//                    break;
//
//                case NEG_X_NEG_Y:
//                    itemToDrag.x = max_x - ((evt.getX() - 15) / scale);
//                    itemToDrag.y = ((evt.getY() - 20) / scale) + min_y;
//                    break;
//
//                case NEG_Y_POS_X:
//                    itemToDrag.x = max_x - ((evt.getY() - 20) / scale);
//                    itemToDrag.y = max_y - ((evt.getX() - 15) / scale);
//                    break;
//            }
//            itemToDrag.x = ((evt.getX() - 15) / scale) + min_x;
//            itemToDrag.y = max_y - ((evt.getY() - 20) / scale);
            double xdiff = itemToDrag.x - orig_x;
            double ydiff = itemToDrag.y - orig_y;
//            if (Math.abs(xdiff) > 100 || Math.abs(ydiff) > 100) {
//                println("big drag jump");
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
                object2DJPanel1.checkedRepaint();
            }
            object2DJPanel1.setMouseDown(true);
        }
        object2DJPanel1.setMousePoint(evt.getPoint());
    }//GEN-LAST:event_object2DJPanel1MouseDragged

    private volatile @Nullable
    PhysicalItem draggedItem = null;

    private volatile @Nullable
    List<PhysicalItem> draggedItemsList = null;

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
//            println("newPoint = " + newPoint.getX() + ", " + newPoint.getY());
            inside = itemDisplayRect.contains(newPoint);
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
        return inside;
    }

    @UIEffect
    private void object2DJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MousePressed
        object2DJPanel1.setMouseDown(true);
        this.object2DJPanel1.setMouseDownPoint(evt.getPoint());
        int x = evt.getX();
        int y = evt.getY();
        int minIndex = -1;
        boolean includeTrays = evt.isShiftDown();
        ClosestItemInfo closestItemInfo = new ClosestItemInfo(x, y, minIndex, includeTrays);
        PhysicalItem closestItem = closestItemInfo.getClosestItem();
        minIndex = closestItemInfo.getMinIndex();
        if (!includeTrays && null != closestItem && !"P".equals(closestItem.getType())) {
            draggedItem = null;
            println("Hold SHIFT to move trays : closestItem=" + closestItem.getFullName());
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
            if (includeTrays) {
                List<PhysicalItem> newDragItems = findIncludedItems(closestItem);
                this.draggedItemsList = newDragItems;
            } else if (!"P".equals(closestItem.getType())) {
                println("Hold SHIFT to move trays : closestItem=" + closestItem.getFullName());
                draggedItem = null;
                this.draggedItemsList = null;
                return;
            }
        }
        draggedItem = closestItem;
        object2DJPanel1.setMouseDown(true);

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
        object2DJPanel1.setMouseDown(false);
        draggedItemsList = null;
        if (null != draggedItem) {
            draggedItem = null;
            List<PhysicalItem> itemsList = getItems();
            this.updateItemsTable(itemsList);
            if (!setItemsListeners.isEmpty()) {
                if (!isSimulated() || !object2DJPanel1.isShowOutputItems() || !isConnected()) {
                    notifySetItemsListeners(itemsList);
                } else {
                    notifySetItemsListeners(getOutputItems());
                }
            }
            if (jCheckBoxDetails.isSelected() || jCheckBoxAddSlots.isSelected()) {
                object2DJPanel1.setItems(itemsList);
            }
        }
        draggedItem = null;
        draggedItemsList = null;
        object2DJPanel1.setMouseDown(false);
    }//GEN-LAST:event_object2DJPanel1MouseReleased

    @UIEffect
    private void jTextFieldMaxXMaxYActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldMaxXMaxYActionPerformed
        object2DJPanel1.clearSizes();
        String txt = jTextFieldMaxXMaxY.getText().trim();
        setMaxXMaxYText(txt);
    }//GEN-LAST:event_jTextFieldMaxXMaxYActionPerformed

    public void setViewLimits(double minX, double minY, double maxX, double maxY) {
        runOnDispatchThread(() -> setViewLimitsOnDisplay(minX, minY, maxX, maxY));
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

    public double getSenseMinX() {
        return object2DJPanel1.getSenseMinX();
    }

    public void setSenseMinX(double senseMinX) {
        object2DJPanel1.setSenseMinX(senseMinX);
    }

    public double getSenseMaxX() {
        return object2DJPanel1.getSenseMaxX();
    }

    public void setSenseMaxX(double senseMaxX) {
        object2DJPanel1.setSenseMaxX(senseMaxX);
    }

    public double getSenseMinY() {
        return object2DJPanel1.getSenseMinY();
    }

    public void setSenseMinY(double senseMinY) {
        object2DJPanel1.setSenseMinY(senseMinY);
    }

    public double getSenseMaxY() {
        return object2DJPanel1.getSenseMaxY();
    }

    public void setSenseMaxY(double senseMaxY) {
        object2DJPanel1.setSenseMaxY(senseMaxY);
    }

    public void setSimSenseLimits(double minX, double minY, double maxX, double maxY) {
        setSenseMinX(minX);
        setSenseMaxX(maxX);
        setSenseMinY(minY);
        setSenseMaxY(maxY);
        setEnforceSensorLimits(true);
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
        object2DJPanel1.clearSizes();
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

    public XFutureVoid loadFile(File f, boolean convertRotToRad, boolean zeroRotations) throws IOException {

        List<XFutureVoid> futuresList = new ArrayList<>();

        boolean takeSnapshots = isSnapshotsEnabled();
        if (takeSnapshots) {
            try {
                takeSnapshot(createTempFile("before_loadFile_" + f.getName() + "_", ".PNG"), (PmCartesian) null, "");
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        if (f.isDirectory()) {
            throw new RuntimeException("Can not load file \"" + f + "\" : It is a directory when a text/csv file is expected.");
        }
        List<PhysicalItem> newItemsList = csvFileToItemsList(f, convertRotToRad, zeroRotations);
        XFutureVoid setItemsFuture = this.setItems(newItemsList);
        futuresList.add(setItemsFuture);
        filenameCachedTextField.setText(f.getCanonicalPath());
        if (takeSnapshots) {
            try {
                takeSnapshot(createTempFile("loadFile_" + f.getName() + "_", ".PNG"), (PmCartesian) null, "");
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        return XFutureVoid.allOf(futuresList)
                .thenComposeToVoid(() -> {
                    return runOnDispatchThread(() -> {
                        this.repaint();
                        object2DJPanel1.repaint();
                    });
                });
    }

    @UIEffect
    public List<PhysicalItem> csvFileToItemsList(File f) throws IOException {
        Object selectedItemHandleRotations = jComboBoxHandleRotationsEnum.getSelectedItem();
        return csvFileToItemsList(f, selectedItemHandleRotations == HandleRotationEnum.DEGREES, selectedItemHandleRotations == HandleRotationEnum.IGNORE);
    }

    @SuppressWarnings("guieffect")
    public List<PhysicalItem> csvFileToItemsList(File f, boolean convertRotToRad, boolean zeroRotations) throws IOException {
        String line = Files.lines(f.toPath()).skip(1).map(String::trim).collect(Collectors.joining(","));
        final List<PhysicalItem> newItemsList = VisionSocketClient.lineToList(line, convertRotToRad, zeroRotations, false);
        return newItemsList;
    }

    @UIEffect
    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
        String fname = jTextFieldFilename.getText().trim();
        File dir = new File(Utils.getAprsUserHomeDir());
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
        File dir = new File(Utils.getAprsUserHomeDir());
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
                Object2DJPanel.saveCsvItemsFile(newFile, getItems());
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

    private @MonotonicNonNull
    AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public @Nullable
    AprsSystem getAprsSystem() {
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

    private @MonotonicNonNull
    SlotOffsetProvider slotOffsetProvider = null;

    public @Nullable
    SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        this.slotOffsetProvider = slotOffsetProvider;
        this.object2DJPanel1.setSlotOffsetProvider(slotOffsetProvider);
    }

    private void connectCurrentPosition() {
        if (null != aprsSystem) {
            aprsSystem.addCurrentPoseListener(this.currentPoseListener);
        }
    }

    private void disconnectCurrentPosition() {
        if (null != aprsSystem) {
            aprsSystem.removeCurrentPoseListener(this.currentPoseListener);
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

    @UIEffect
    public boolean isAutoscale() {
        return object2DJPanel1.isAutoscale() && jCheckBoxAutoscale.isSelected();
    }

    @UIEffect
    public void setAutoscale(boolean autoscale) {
        object2DJPanel1.setAutoscale(autoscale);
        jCheckBoxAutoscale.setSelected(autoscale);
    }

    private PmCartesian getMinOffset() {
        PmCartesian minDiffCart = new PmCartesian();
        if (null != aprsSystem) {
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

    public XFutureVoid setShowOutputItems(boolean showOutputItems) {
        return runOnDispatchThread(() -> setShowOutputItemsOnDisplay(showOutputItems));
    }

    @UIEffect
    public void setShowOutputItemsOnDisplay(boolean showOutputItems) {
        object2DJPanel1.setShowOutputItems(showOutputItems);
        if (!showOutputItems) {
            List<PhysicalItem> items = getItems();
            setItemsInternal(items);
            notifySetItemsListeners(items);
        } else {
            final List<PhysicalItem> outputItems = getOutputItems();
            setOutputItemsInternal(outputItems);
            if (this.isSimulated()) {
                notifySetItemsListeners(outputItems);
            }
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
        boolean includeTrays = evt.isShiftDown();
        this.object2DJPanel1.setMouseDownPoint(evt.getPoint());
        ClosestItemInfo closestItemInfo = new ClosestItemInfo(x, y, minIndex, includeTrays);
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

    @UIEffect
    private void object2DJPanel1MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseMoved
        this.draggedItem = null;
        this.draggedItemsList = null;
        object2DJPanel1.setMousePoint(evt.getPoint());
    }//GEN-LAST:event_object2DJPanel1MouseMoved

    @UIEffect
    @SuppressWarnings("nullness")
    private void object2DJPanel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseEntered
        this.draggedItem = null;
        this.draggedItemsList = null;
        this.object2DJPanel1.setMouseInside(true);
        this.object2DJPanel1.setMousePoint(null);
        this.object2DJPanel1.setMouseDownPoint(null);
    }//GEN-LAST:event_object2DJPanel1MouseEntered

    @UIEffect
    @SuppressWarnings("nullness")
    private void object2DJPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseExited
        this.draggedItem = null;
        this.draggedItemsList = null;
        this.object2DJPanel1.setMouseInside(false);
        this.object2DJPanel1.setMousePoint(null);
        this.object2DJPanel1.setMouseDownPoint(null);
    }//GEN-LAST:event_object2DJPanel1MouseExited

    /**
     * Get the value of showAddedToolsAndToolHolders
     *
     * @return the value of showAddedToolsAndToolHolders
     */
    @UIEffect
    public boolean isShowAddedToolsAndToolHolders() {
        return jCheckBoxTools.isSelected();
    }

    /**
     * Set the value of showAddedToolsAndToolHolders
     *
     * @param showAddedToolsAndToolHolders new value of
     * showAddedToolsAndToolHolders
     */
    @UIEffect
    void setShowAddedToolsAndToolHolders(boolean showAddedToolsAndToolHolders) {
        jCheckBoxTools.setSelected(showAddedToolsAndToolHolders);
        object2DJPanel1.setShowAddedToolsAndToolHolders(showAddedToolsAndToolHolders);
    }

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

    @UIEffect
    private void jButtonReadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReadPropertiesActionPerformed
        Properties props = getPropertiesOnDisplay();
        loadPropertiesTableOnDisplay(props);
    }//GEN-LAST:event_jButtonReadPropertiesActionPerformed

    @UIEffect
    private void jButtonSetProperiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetProperiesActionPerformed
        Properties props = this.tableLoadedProperties;
        if (null == props) {
            props = new Properties();
        }

        updateDisplayFromPropsTable(0, jTableProperties.getRowCount(), props, true);
    }//GEN-LAST:event_jButtonSetProperiesActionPerformed

    @UIEffect
    private void jButtonForceUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonForceUpdateActionPerformed
        refresh(false);
    }//GEN-LAST:event_jButtonForceUpdateActionPerformed

    @UIEffect
    private void jButtonViewSnapshotImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewSnapshotImageActionPerformed
        int selectedRow = jTableSnapshotFiles.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < jTableSnapshotFiles.getRowCount()) {
            try {
                DefaultTableModel dtm = (DefaultTableModel) jTableSnapshotFiles.getModel();
                String filename = (String) dtm.getValueAt(selectedRow, 3);
                if (null == filename) {
                    throw new NullPointerException("jTableSnapshotFiles.getValueAt(" + selectedRow + ", 3)");
                }
                Desktop.getDesktop().open(new File(filename));
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonViewSnapshotImageActionPerformed

    @UIEffect
    private void jButtonViewSnapshotCsvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewSnapshotCsvActionPerformed
        int selectedRow = jTableSnapshotFiles.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < jTableSnapshotFiles.getRowCount()) {
            try {
                DefaultTableModel dtm = (DefaultTableModel) jTableSnapshotFiles.getModel();
                String filename = (String) dtm.getValueAt(selectedRow, 4);
                if (null == filename) {
                    throw new NullPointerException("jTableSnapshotFiles.getValueAt(" + selectedRow + ", 4)");
                }
                Desktop.getDesktop().open(new File(filename));
            } catch (IOException ex) {
                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButtonViewSnapshotCsvActionPerformed

    @UIEffect
    private void object2DJPanel1ComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_object2DJPanel1ComponentResized
        object2DJPanel1.clearSizes();
    }//GEN-LAST:event_object2DJPanel1ComponentResized

    @UIEffect
    private void jCheckBoxRecordLinesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxRecordLinesActionPerformed
        if (jCheckBoxRecordLines.isSelected()) {
            recordLines = true;
        } else {
            recordLines = false;
            if (null != lineCsvWriter) {
                lineCsvWriter.close();
                lineCsvWriter = null;
            }
        }
    }//GEN-LAST:event_jCheckBoxRecordLinesActionPerformed

    @UIEffect
    private void jButtonOpenLogLinesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenLogLinesFileActionPerformed
        try {
            Desktop.getDesktop().open(new File(jTextFieldRecordLinesFile.getText()));
        } catch (IOException ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonOpenLogLinesFileActionPerformed

    @UIEffect
    private void jButtonShowSelectedLogLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonShowSelectedLogLineActionPerformed
        int selectedRow = jTableLineLog.getSelectedRow();
        showSelectedLogLine(selectedRow);
    }//GEN-LAST:event_jButtonShowSelectedLogLineActionPerformed

    @UIEffect
    private void showSelectedLogLine(int selectedRow) throws NumberFormatException {
        StringBuilder sb = new StringBuilder();
        for (int i = 5; i < jTableLineLog.getColumnCount(); i++) {
            final Object valueAtI = jTableLineLog.getValueAt(selectedRow, i);
            if (valueAtI == null || valueAtI.toString().equals("null")) {
                break;
            }
            sb.append(valueAtI).append(",");
        }
        String line = sb.toString();
        List<PhysicalItem> l = VisionSocketClient.lineToList(line,
                handleRotationEnum == HandleRotationEnum.DEGREES,
                handleRotationEnum == HandleRotationEnum.IGNORE,
                false/* dont lock trays */);
        setItems(l);
        Object xobj = jTableLineLog.getValueAt(selectedRow, 2);
        if (null != xobj) {
            if (xobj instanceof Double) {
                object2DJPanel1.setCurrentX((Double) xobj);
            } else {
                object2DJPanel1.setCurrentX(Double.parseDouble(xobj.toString()));
            }
        }
        Object yobj = jTableLineLog.getValueAt(selectedRow, 3);
        if (null != yobj) {
            if (yobj instanceof Double) {
                object2DJPanel1.setCurrentY((Double) yobj);
            } else {
                object2DJPanel1.setCurrentY(Double.parseDouble(yobj.toString()));
            }
        }
    }

    @UIEffect
    @SuppressWarnings("nullness")
    private void jButtonSeperateLineLogWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSeperateLineLogWindowActionPerformed
        File f = logLinesFile;
        if (null != f) {
            String name = f.getName();
            logLinesFile = null;
            if (null != lineCsvWriter) {
                lineCsvWriter.close();
                lineCsvWriter = null;
            }
            Object2DOuterDialogPanel.showObject2DDialog(null, name, false, propertiesFile, null, f);
        }
    }//GEN-LAST:event_jButtonSeperateLineLogWindowActionPerformed

    @UIEffect
    private void jButtonLineLogPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLineLogPrevActionPerformed
        int selectedRow = jTableLineLog.getSelectedRow();
        final int rowCount = jTableLineLog.getRowCount();
        if (selectedRow > 0) {
            showSelectedLogLine(selectedRow - 1);
            jTableLineLog.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTableLineLog.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
            jTableLineLog.scrollRectToVisible(new Rectangle(jTableLineLog.getCellRect(selectedRow - 1, 0, true)));
        } else if (rowCount > 0) {
            showSelectedLogLine(rowCount - 1);
            jTableLineLog.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTableLineLog.getSelectionModel().setSelectionInterval(rowCount - 1, rowCount - 1);
            jTableLineLog.scrollRectToVisible(new Rectangle(jTableLineLog.getCellRect(rowCount - 1, 0, true)));
        }
    }//GEN-LAST:event_jButtonLineLogPrevActionPerformed

    @UIEffect
    private void jButtonLineLogNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLineLogNextActionPerformed
        int selectedRow = jTableLineLog.getSelectedRow();
        final int rowCount = jTableLineLog.getRowCount();
        if (selectedRow > -1 && selectedRow < rowCount) {
            showSelectedLogLine(selectedRow + 1);
            jTableLineLog.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTableLineLog.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
            jTableLineLog.scrollRectToVisible(new Rectangle(jTableLineLog.getCellRect(selectedRow + 1, 0, true)));
        } else if (rowCount > 0) {
            showSelectedLogLine(0);
            jTableLineLog.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jTableLineLog.getSelectionModel().setSelectionInterval(0, 0);
            jTableLineLog.scrollRectToVisible(new Rectangle(jTableLineLog.getCellRect(0, 0, true)));
        }
    }//GEN-LAST:event_jButtonLineLogNextActionPerformed

    @UIEffect
    private void jCheckBoxShowOnlyOverlappingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowOnlyOverlappingActionPerformed
        if (jCheckBoxShowOnlyOverlapping.isSelected()) {
            jCheckBoxShowOverlapping.setSelected(true);
            jCheckBoxShowOverlapping.setEnabled(false);
            object2DJPanel1.setShowOnlyOverlapping(true);
            object2DJPanel1.setShowOverlapping(true);
        } else {
            jCheckBoxShowOverlapping.setEnabled(true);
            object2DJPanel1.setShowOnlyOverlapping(false);
        }
        object2DJPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxShowOnlyOverlappingActionPerformed

    @UIEffect
    private void jCheckBoxShowOverlappingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxShowOverlappingActionPerformed
        if (jCheckBoxShowOverlapping.isSelected()) {
            jCheckBoxShowOnlyOverlapping.setEnabled(true);
            object2DJPanel1.setShowOverlapping(true);
        } else {
            object2DJPanel1.setShowOnlyOverlapping(false);
            object2DJPanel1.setShowOverlapping(false);
            jCheckBoxShowOnlyOverlapping.setSelected(false);
            jCheckBoxShowOnlyOverlapping.setEnabled(false);
        }
        object2DJPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxShowOverlappingActionPerformed

    @UIEffect
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        object2DJPanel1.clearSizes();
        setMaxXMaxYText(jTextFieldMaxXMaxY.getText().trim());
        setMinXMinYText(jTextFieldMinXMinY.getText().trim());
    }//GEN-LAST:event_formComponentResized

    private void jCheckBoxHideItemsNearMouseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxHideItemsNearMouseActionPerformed
        jTextFieldHideNearMouseDist.setEnabled(jCheckBoxHideItemsNearMouse.isSelected() && isSimulated());
    }//GEN-LAST:event_jCheckBoxHideItemsNearMouseActionPerformed

    private void jTextFieldHideNearMouseDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldHideNearMouseDistActionPerformed
        setMouseHideDist(parseDouble(jTextFieldHideNearMouseDist.getText().trim()));
    }//GEN-LAST:event_jTextFieldHideNearMouseDistActionPerformed

    private void jCheckBoxHideItemsNearRobotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxHideItemsNearRobotActionPerformed
        jTextFieldHideNearRobotDist.setEnabled(jCheckBoxHideItemsNearRobot.isSelected() && isSimulated());
    }//GEN-LAST:event_jCheckBoxHideItemsNearRobotActionPerformed

    private void jTextFieldHideNearRobotDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldHideNearRobotDistActionPerformed
        setRobotHideDist(parseDouble(jTextFieldHideNearRobotDist.getText().trim()));
    }//GEN-LAST:event_jTextFieldHideNearRobotDistActionPerformed

    private void jCheckBoxLockTraysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxLockTraysActionPerformed
        if (null != visionSocketClient) {
            visionSocketClient.setLockTrays(jCheckBoxLockTrays.isSelected());
        }
    }//GEN-LAST:event_jCheckBoxLockTraysActionPerformed

    private void jTextFieldPrevListSizeDecrementIntervalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPrevListSizeDecrementIntervalActionPerformed
        final int newInterval = Integer.parseInt(jTextFieldPrevListSizeDecrementInterval.getText().trim());
        setPrevListSizeDecrementInterval(newInterval);
    }//GEN-LAST:event_jTextFieldPrevListSizeDecrementIntervalActionPerformed

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

    @UIEffect
    private void simUpdateAction(ActionEvent evt) {
        try {
            if (simulationUpdateAsNeededCachedCheckBox.isSelected()) {
                return;
            }
            if (!forceOutputFlag) {
                refresh(false);
            }
        } catch (Exception e) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", e);
            stopSimUpdateTimerOnDisplay();
            showException(e);
            disconnect();
        }
    }

    public void stopSimUpdateTimer() {
        if (null != simUpdateTimer) {
            runOnDispatchThread(this::stopSimUpdateTimerOnDisplay);
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
        runOnDispatchThread(this::setupSimUpdateTimerOnDisplay);
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
    private javax.swing.JButton jButtonForceUpdate;
    private javax.swing.JButton jButtonLineLogNext;
    private javax.swing.JButton jButtonLineLogPrev;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonOffsetAll;
    private javax.swing.JButton jButtonOpenLogLinesFile;
    private javax.swing.JButton jButtonReadProperties;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JButton jButtonSeperateLineLogWindow;
    private javax.swing.JButton jButtonSetProperies;
    private javax.swing.JButton jButtonShowSelectedLogLine;
    private javax.swing.JButton jButtonViewSnapshotCsv;
    private javax.swing.JButton jButtonViewSnapshotImage;
    private javax.swing.JCheckBox jCheckBoxAddPosNoise;
    private javax.swing.JCheckBox jCheckBoxAddSlots;
    private javax.swing.JCheckBox jCheckBoxAutoscale;
    private javax.swing.JCheckBox jCheckBoxCloning;
    private javax.swing.JCheckBox jCheckBoxConnected;
    private javax.swing.JCheckBox jCheckBoxDebug;
    private javax.swing.JCheckBox jCheckBoxDetails;
    private javax.swing.JCheckBox jCheckBoxEnforceSensorLimits;
    private javax.swing.JCheckBox jCheckBoxHideItemsNearMouse;
    private javax.swing.JCheckBox jCheckBoxHideItemsNearRobot;
    private javax.swing.JCheckBox jCheckBoxLockTrays;
    private javax.swing.JCheckBox jCheckBoxPause;
    private javax.swing.JCheckBox jCheckBoxRecordLines;
    private javax.swing.JCheckBox jCheckBoxSeparateNames;
    private javax.swing.JCheckBox jCheckBoxShowCurrent;
    private javax.swing.JCheckBox jCheckBoxShowOnlyOverlapping;
    private javax.swing.JCheckBox jCheckBoxShowOverlapping;
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
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelHost;
    private javax.swing.JLabel jLabelTraySlotInfoStatus;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelBottomMain;
    private javax.swing.JPanel jPanelBottomPropertiesButtons;
    private javax.swing.JPanel jPanelConnectionsTab;
    private javax.swing.JPanel jPanelItems;
    private javax.swing.JPanel jPanelItemsButtons;
    private javax.swing.JPanel jPanelOptionsTab;
    private javax.swing.JPanel jPanelProperties;
    private javax.swing.JPanel jPanelSimulationTab;
    private javax.swing.JPanel jPanelSnapshots;
    private javax.swing.JPanel jPanelTopRow;
    private javax.swing.JPanel jPanelTrays;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPaneProperties;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableItems;
    private javax.swing.JTable jTableLineLog;
    private javax.swing.JTable jTableProperties;
    private javax.swing.JTable jTableSnapshotFiles;
    private javax.swing.JTable jTableTraySlots;
    private javax.swing.JTextArea jTextAreaConnectDetails;
    private javax.swing.JTextField jTextFieldCurrentXY;
    private javax.swing.JTextField jTextFieldDropOffThreshold;
    private javax.swing.JTextField jTextFieldFilename;
    private javax.swing.JTextField jTextFieldHideNearMouseDist;
    private javax.swing.JTextField jTextFieldHideNearRobotDist;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldMaxXMaxY;
    private javax.swing.JTextField jTextFieldMinXMinY;
    private javax.swing.JTextField jTextFieldPickupDist;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldPosNoise;
    private javax.swing.JTextField jTextFieldPrevListSizeDecrementInterval;
    private javax.swing.JTextField jTextFieldRecordLinesFile;
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
        return runOnDispatchThread(this::savePropertiesOnDisplay);
    }

    @UIEffect
    private void savePropertiesOnDisplay() {
        if (null != propertiesFile) {
            File parentFile = propertiesFile.getParentFile();
            if (null != parentFile) {
                parentFile.mkdirs();
            }
            Properties props = getPropertiesOnDisplay();
//            try (FileWriter fw = new FileWriter(propertiesFile)) {
//                props.store(fw, "");
//            }
            Utils.saveProperties(propertiesFile, props);
        }
    }

    public XFuture<Properties> getProperties() {
        return Utils.supplyOnDispatchThread(this::getPropertiesOnDisplay);
    }

    private int prevListSizeDecrementInterval = 2000;

    public int getPrevListSizeDecrementInterval() {
        if (null != visionSocketClient) {
            int ret = visionSocketClient.getPrevListSizeDecrementInterval();
            this.prevListSizeDecrementInterval = ret;
            return ret;
        }
        return prevListSizeDecrementInterval;
    }

    public void setPrevListSizeDecrementInterval(int prevListSizeDecrementInterval) {
        if (null != visionSocketClient) {
            visionSocketClient.setPrevListSizeDecrementInterval(prevListSizeDecrementInterval);
        }
        this.prevListSizeDecrementInterval = prevListSizeDecrementInterval;
    }

    @UIEffect
    Properties getPropertiesOnDisplay() {
        Properties props = new Properties();
        loadPropsFromTable(props);
        props.setProperty("alternativeRotation", String.format("%.2f", toDegrees(object2DJPanel1.getAlternativeRotation())));
        props.setProperty("--visionport", portCachedTextField.getText().trim());
        props.setProperty("--visionhost", hostCachedTextField.getText().trim());
        props.setProperty("simulated", Boolean.toString(simulatedCachedCheckBox.isSelected()));
        props.setProperty("viewOutput", Boolean.toString(viewOutputCachedCheckBox.isSelected()));
        props.setProperty("simulationUpdateAsNeeded", Boolean.toString(simulationUpdateAsNeededCachedCheckBox.isSelected()));
        props.setProperty("shuffleSimulatedUpdates", Boolean.toString(shuffleSimulatedUpdatesCachedCheckBox.isSelected()));
        props.setProperty("simulatedDropRate", String.format("%.3f", simulatedDropRate));
        props.setProperty("addPosNoise", Boolean.toString(addPosNoiseCachedCheckBox.isSelected()));
        props.setProperty("pickupDist", String.format("%.2f", pickupDist));
        props.setProperty("dropOffThreshold", String.format("%.2f", dropOffThreshold));
        props.setProperty("posNoise", String.format("%.2f", posNoise));
        props.setProperty("rotNoise", String.format("%.2f", rotNoise));
        props.setProperty("simRefreshMillis", Integer.toString(simRefreshMillis));
        props.setProperty("connected", Boolean.toString(connectedCachedCheckBox.isSelected()));
        props.setProperty("autoscale", Boolean.toString(autoscaleCachedCheckBox.isSelected()));
        props.setProperty("tools", Boolean.toString(jCheckBoxTools.isSelected()));
        props.setProperty("trackcurrentpos", Boolean.toString(showCurrentCachedCheckBox.isSelected()));
        props.setProperty("showrotations", Boolean.toString(jCheckBoxShowRotations.isSelected()));
        props.setProperty("viewDetails", Boolean.toString(jCheckBoxDetails.isSelected()));
        props.setProperty("separatenames", Boolean.toString(jCheckBoxSeparateNames.isSelected()));
        props.setProperty("xmaxymax", jTextFieldMaxXMaxY.getText().trim());
        props.setProperty("xminymin", jTextFieldMinXMinY.getText().trim());
        props.setProperty("senseMinX", Double.toString(getSenseMinX()));
        props.setProperty("senseMinY", Double.toString(getSenseMinY()));
        props.setProperty("senseMaxX", Double.toString(getSenseMaxX()));
        props.setProperty("senseMaxY", Double.toString(getSenseMaxY()));
        props.setProperty("recordLines", Boolean.toString(jCheckBoxRecordLines.isSelected()));
        props.setProperty("enforceSensorLimits", Boolean.toString(isEnforceSensorLimits()));
        props.setProperty("prevListSizeDecrementInterval", Integer.toString(getPrevListSizeDecrementInterval()));
        props.setProperty("repaintMinMillis", Long.toString(object2DJPanel1.getRepaintMinMillis()));
        if (null != aprsSystem && aprsSystem.isVisionToDbConnected()) {
            double visionToDBRotationOffset = aprsSystem.getVisionToDBRotationOffset();
            props.setProperty("visionToDBRotationOffset", Double.toString(visionToDBRotationOffset));
            if (Math.abs(object2DJPanel1.getRotationOffset() - visionToDBRotationOffset) > 0.001) {
                jTextFieldRotationOffset.setText(String.format("%.1f", toDegrees(visionToDBRotationOffset)));
            }
            object2DJPanel1.setRotationOffset(visionToDBRotationOffset);
        }
        props.setProperty("rotationOffset", Double.toString(object2DJPanel1.getRotationOffset()));
        if (reverseFlag) {
            this.reverseDataFileString = filenameCachedTextField.getText().trim();
        } else {
            this.dataFileString = filenameCachedTextField.getText().trim();
        }
        if (null != reverseDataFileString && reverseDataFileString.length() > 0) {
            String datafileShort = makeShortPath(propertiesFile, reverseDataFileString);
            props.setProperty("reverse_datafile", datafileShort);
        }
        if (null != dataFileString && dataFileString.length() > 0) {
            String datafileShort = makeShortPath(propertiesFile, dataFileString);
            props.setProperty("datafile", datafileShort);
        }
        props.setProperty("reverseFlag", Boolean.toString(reverseFlag));
        DisplayAxis displayAxis = object2DJPanel1.getDisplayAxis();
        props.setProperty("displayAxis", displayAxis.toString());
        props.setProperty("hideNearMouse", Boolean.toString(jCheckBoxHideItemsNearMouse.isSelected()));
        props.setProperty("hideNearMouseDist", jTextFieldHideNearMouseDist.getText());
        props.setProperty("hideNearRobot", Boolean.toString(jCheckBoxHideItemsNearRobot.isSelected()));
        props.setProperty("hideNearRobotDist", jTextFieldHideNearRobotDist.getText());
        List<PhysicalItem> l = getItems();
        if (null != l && l.size() > 0) {
            props.setProperty(ITEMS_PROPERTY_NAME, VisionSocketServer.listToLine(l));
        }
        return props;
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
            return loadProperties(props);
        }
        if (null == propertiesFile) {
            throw new IllegalStateException("propertiesFile=" + propertiesFile);
        }
        throw new IllegalStateException("propertiesFile=" + propertiesFile + " does not exist");
    }

    private volatile @Nullable
    XFutureVoid loadPropertiesFuture2 = null;

    public XFutureVoid loadProperties(Properties props) {
        XFutureVoid ret = runOnDispatchThread(() -> {
            loadPropertiesOnDisplay(props);
        });
        loadPropertiesFuture2 = ret;
        return ret;
    }

    @UIEffect
    private void loadPropertiesOnDisplay(Properties props) {
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
                updateDisplayFromPropsTable(firstRow, lastRow, props, propschanged);
            }
        }

    };

    @UIEffect
    private void updateDisplayFromPropsTable(int firstRow, int lastRow, Properties props, boolean propschanged) {
        propschanged = loadPropsFromTable(firstRow, lastRow, props, propschanged);
        if (!propschanged) {
            return;
        }
        if (loadingProperties || loadingTableProperties || updatingDisplayFromProperties) {
            return;
        }
        updateDisplayFromProperties(props);
    }

    @UIEffect
    private boolean loadPropsFromTable(Properties props) {
        return loadPropsFromTable(0, jTableProperties.getRowCount(), props, true);
    }

    @UIEffect
    private boolean loadPropsFromTable(int firstRow, int lastRow, Properties props, boolean propschanged) {
        DefaultTableModel model = (DefaultTableModel) jTableProperties.getModel();
        for (int i = Math.max(firstRow, 0); i < Math.min(lastRow + 1, model.getRowCount()); i++) {
            Object key = model.getValueAt(i, 0);
            Object value = model.getValueAt(i, 1);
            if (key instanceof String && null != value) {
                props.setProperty((String) key, value.toString());
            }
            propschanged = true;
        }
        return propschanged;
    }

    private volatile @MonotonicNonNull
    Properties tableLoadedProperties = null;

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
                    handleRotationEnum == HandleRotationEnum.IGNORE,
                    false/* dont lock trays */);
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

        String senseMaxXString = props.getProperty("senseMaxX");
        if (null != senseMaxXString && senseMaxXString.length() > 0) {
            double senseMaxX = parseDouble(senseMaxXString);
            setSenseMaxX(senseMaxX);
        }

        String senseMinXString = props.getProperty("senseMinX");
        if (null != senseMinXString && senseMinXString.length() > 0) {
            double senseMinX = parseDouble(senseMinXString);
            setSenseMinX(senseMinX);
        }

        String senseMaxYString = props.getProperty("senseMaxY");
        if (null != senseMaxYString && senseMaxYString.length() > 0) {
            double senseMaxY = parseDouble(senseMaxYString);
            setSenseMaxY(senseMaxY);
        }

        String enforceSensorLimitsString = props.getProperty("enforceSensorLimits");
        if (null != enforceSensorLimitsString && enforceSensorLimitsString.length() > 0) {
            boolean enforceSensorLimits = Boolean.parseBoolean(enforceSensorLimitsString);
            setEnforceSensorLimits(enforceSensorLimits);
        }

        String senseMinYString = props.getProperty("senseMinY");
        if (null != senseMinYString && senseMinYString.length() > 0) {
            double senseMinY = parseDouble(senseMinYString);
            setSenseMinY(senseMinY);
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

        String recordLinesString = props.getProperty("recordLines");
        if (null != recordLinesString && recordLinesString.length() > 0) {
            boolean recordLinesLocal = Boolean.valueOf(recordLinesString);
            setRecordLines(recordLinesLocal);
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
        String repaintMinMillisString = props.getProperty("repaintMinMillis");
        if (null != repaintMinMillisString) {
            object2DJPanel1.setRepaintMinMillis(Long.parseLong(repaintMinMillisString));
        }

        String connectedString = props.getProperty("connected");
        if (null != connectedString && connectedString.length() > 0) {
            boolean connected = Boolean.valueOf(connectedString);
            connectedCachedCheckBox.setSelected(connected);
            if (connected) {
                connect();
                final boolean canLockTrays = !isSimulated() && isConnected();
                jCheckBoxLockTrays.setEnabled(canLockTrays);
                if (!canLockTrays) {
                    jCheckBoxLockTrays.setSelected(false);
                    if (null != visionSocketClient) {
                        visionSocketClient.setLockTrays(false);
                    }
                }
            }
        } else {
            throw new IllegalStateException("connected property not set");
        }
        if (!dialogMode) {
            if (simulatedCachedCheckBox.isSelected() || !connectedCachedCheckBox.isSelected()) {
                if (needReloadDataFile()) {
                    try {
                        reloadDataFile();
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                    }
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
        String prevListSizeDecrementIntervalString = props.getProperty("prevListSizeDecrementInterval");
        if (null != prevListSizeDecrementIntervalString) {
            jTextFieldPrevListSizeDecrementInterval.setText(prevListSizeDecrementIntervalString);
            setPrevListSizeDecrementInterval(Integer.parseInt(prevListSizeDecrementIntervalString));
        }
        String rotationOffsetString = props.getProperty("rotationOffset");
        if (null != rotationOffsetString) {
            object2DJPanel1.setRotationOffset(Double.parseDouble(rotationOffsetString));
        }
        String hideNearMouseString = props.getProperty("hideNearMouse");
        if (null != hideNearMouseString) {
            final boolean hidePartsNearMousePosition = Boolean.parseBoolean(hideNearMouseString);
            jCheckBoxHideItemsNearMouse.setSelected(hidePartsNearMousePosition);
            jTextFieldHideNearMouseDist.setEnabled(isSimulated() && hidePartsNearMousePosition);
        }
        String hideNearMouseDistString = props.getProperty("hideNearMouseDist");
        if (null != hideNearMouseDistString) {
            jTextFieldHideNearMouseDist.setText(hideNearMouseDistString);
            this.setMouseHideDist(Double.parseDouble(hideNearMouseDistString));
        }
        String hideNearRobotString = props.getProperty("hideNearRobot");
        if (null != hideNearRobotString) {
            final boolean hidePartsNearRobotPosition = Boolean.parseBoolean(hideNearRobotString);
            jCheckBoxHideItemsNearRobot.setSelected(hidePartsNearRobotPosition);
            jTextFieldHideNearRobotDist.setEnabled(isSimulated() && hidePartsNearRobotPosition);
        }
        String hideNearRobotDistString = props.getProperty("hideNearRobotDist");
        if (null != hideNearRobotDistString) {
            jTextFieldHideNearRobotDist.setText(hideNearRobotDistString);
            this.setRobotHideDist(Double.parseDouble(hideNearRobotDistString));
        }
        updatingDisplayFromProperties = false;
    }

    public boolean isSimulated() {
        return simulatedCachedCheckBox.isSelected();
    }

    public boolean isConnected() {
        if (!connectedCachedCheckBox.isSelected()) {
            return false;
        }
        if (simulatedCachedCheckBox.isSelected()) {
            return visionSocketServer != null;
        } else {
            return visionSocketClient != null && visionSocketClient.isConnected();
        }
    }

    private @Nullable
    String dataFileString = null;
    private @Nullable
    String reverseDataFileString = null;
    private volatile @Nullable
    String loadedDataFileString = null;

    private @Nullable
    String getCurrentDataFileString() {
        return reverseFlag ? this.reverseDataFileString : this.dataFileString;
    }

    private boolean dialogMode;

    /**
     * Get the value of dialogMode
     *
     * @return the value of dialogMode
     */
    public boolean isDialogMode() {
        return dialogMode;
    }

    /**
     * Set the value of dialogMode
     *
     * @param dialogMode new value of dialogMode
     */
    public void setDialogMode(boolean dialogMode) {
        this.dialogMode = dialogMode;
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
                if (null == propertiesFile) {
                    throw new IllegalStateException("currentDataFileString = " + currentDataFileString + " does not exist and propertiesFile=" + propertiesFile);
                }
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
    private volatile long max_time_diff = 0;

    public void clearPrevVisionListSize() {
        if (null != visionSocketClient) {
            visionSocketClient.clearPrevVisionListSize();
        }
    }

    private volatile boolean recordLines = false;

    public boolean isRecordLines() {
        return recordLines;
    }

    @UIEffect
    public void setRecordLines(boolean recordLines) {
        this.recordLines = recordLines;
        jCheckBoxRecordLines.setSelected(recordLines);
    }

    private volatile @Nullable
    PrintWriter lineCsvWriter = null;
    private final AtomicInteger lineCount = new AtomicInteger();

    @SuppressWarnings("guieffect")
    public void loadLogFile(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            while (line != null && line.trim().length() < 2) {
                line = br.readLine();
            }
            if (null == line) {
                throw new RuntimeException("File " + f + " seems empty.");
            }
            String headersArray[] = line.split(",[ ]*");
            jTableLineLog.setModel(new DefaultTableModel(new Object[0][0], headersArray));
            line = br.readLine();
            while (line != null) {
                String fullLineArray[] = line.split(",[ ]*");
                ((DefaultTableModel) jTableLineLog.getModel()).addRow(fullLineArray);
                line = br.readLine();
            }
            Utils.autoResizeTableColWidths(jTableLineLog);
            jTextFieldRecordLinesFile.setText(f.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            showException(ex);
            disconnect();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private volatile @Nullable
    File logLinesFile = null;

    public @Nullable
    File getLogLinesFile() {
        return logLinesFile;
    }

    private String getTaskName() {
        if (null != aprsSystem) {
            return aprsSystem.getTaskName();
        } else {
            return "";
        }
    }

    @Override
    @SuppressWarnings("guieffect")
    public XFutureVoid visionClientUpdateReceived(List<PhysicalItem> l, String line, boolean ignored) {
        try {
            long now = System.currentTimeMillis();
            if (recordLines) {
                int lc = lineCount.incrementAndGet();
                final PrintWriter origLineCsvWriter = lineCsvWriter;
                if (null == origLineCsvWriter || lc < 2) {
                    File f = Utils.createTempFile("vision_lines_" + getTaskName() + "_" + getHost() + "_" + getPort(), ".csv");
                    println("Recording vision lines to  " + f.getCanonicalPath());
                    String headingLine = VisionSocketClient.lineToHeading("count,time,currentX,currentY,ignored,", line);

                    String headersArray[] = headingLine.split(",[ ]*");
                    jTableLineLog.setModel(new DefaultTableModel(new Object[0][0], headersArray));
                    PrintWriter lineCsvWriterNew = new PrintWriter(new FileWriter(f));
                    String fullLine = lc + "," + now + "," + object2DJPanel1.getCurrentX() + "," + object2DJPanel1.getCurrentY() + "," + ignored + "," + line;
                    String fullLineArray[] = fullLine.split(",[ ]*");
                    ((DefaultTableModel) jTableLineLog.getModel()).addRow(fullLineArray);
                    lineCsvWriterNew.println(headingLine);
                    lineCsvWriterNew.println(fullLine);
                    lineCsvWriter = lineCsvWriterNew;
                    Utils.autoResizeTableColWidths(jTableLineLog);
                    jTextFieldRecordLinesFile.setText(f.getCanonicalPath());
                    logLinesFile = f;
                } else {
                    String fullLine = lc + "," + now + "," + object2DJPanel1.getCurrentX() + "," + object2DJPanel1.getCurrentY() + "," + ignored + "," + line;
                    String fullLineArray[] = fullLine.split(",[ ]*");
                    ((DefaultTableModel) jTableLineLog.getModel()).addRow(fullLineArray);
                    origLineCsvWriter.println(fullLine);
                }
            }
            String detailsMessage = null;
            if (null != visionSocketClient) {
                long timediff = now - lastVisionUpdateTime;
                if (timediff > max_time_diff) {
                    max_time_diff = timediff;
                }
                detailsMessage
                        = "size=" + l.size() + "\n"
                        + "count=" + visionSocketClient.getLineCount() + "\n"
                        + "skipped=" + visionSocketClient.getSkippedLineCount() + "\n"
                        + "ignored=" + visionSocketClient.getIgnoreCount() + "\n"
                        + "consecutive=" + visionSocketClient.getConsecutiveIgnoreCount() + "\n"
                        + "max_consecutive=" + visionSocketClient.getMaxConsecutiveIgnoreCount() + "\n"
                        + "time=" + (timediff) + "\n"
                        + "max_time=" + (max_time_diff) + "\n";
            }
            final String finalDetailsMessage = detailsMessage;
            lastVisionUpdateTime = now;
            setItems(l);
            return runOnDispatchThread(() -> handleClientUpdateOnDisplay(l, finalDetailsMessage));
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            showException(ex);
            disconnect();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    @UIEffect
    private void handleClientUpdateOnDisplay(List<PhysicalItem> l, @Nullable String detailsMessage) {
        try {
            if (null != detailsMessage) {
                jTextAreaConnectDetails.setText(detailsMessage);
            }
        } catch (Exception ex) {
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
            showException(ex);
            disconnect();
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    private volatile boolean lastIsHoldingObjectExpected = false;
    private volatile int captured_item_index = -1;

    private final ConcurrentLinkedDeque<File[]> fileArrayDeque = new ConcurrentLinkedDeque<>();

    @UIEffect
    private void fileArrayDequeConsumer(ConcurrentLinkedDeque<File[]> fileArrayDeque) {
        File fa[] = this.fileArrayDeque.pollFirst();
        while (null != fa) {
            updateSnapshotsTable(fa[0], fa[1]);
            fa = this.fileArrayDeque.pollFirst();
        }
    }

    @Override
    public @Nullable
    File[] takeSnapshot(File f, @Nullable Collection<? extends PhysicalItem> itemsToPaint, int w, int h) {

        if (null != itemsToPaint && !itemsToPaint.isEmpty()) {
            this.object2DJPanel1.takeSnapshot(f, itemsToPaint, w, h);
            File csvFile = imageFileToCsvFile(f);
            final File[] fileArray = new File[]{f, csvFile};
            fileArrayDeque.add(fileArray);
            if (null != aprsSystem) {
                aprsSystem.submitDisplayConsumer(this::fileArrayDequeConsumer, fileArrayDeque);
            }
            return fileArray;
        } else {
            List<PhysicalItem> items = getItems();
            File csvFile = imageFileToCsvFile(f);
            this.object2DJPanel1.takeSnapshot(f, items, w, h);
            final File[] fileArray = new File[]{f, csvFile};
            fileArrayDeque.add(fileArray);
            if (null != aprsSystem) {
                aprsSystem.submitDisplayConsumer(this::fileArrayDequeConsumer, fileArrayDeque);
            }
            return fileArray;
        }
    }

    public static File imageFileToCsvFile(File f) {
        return Object2DJPanel.imageFileToCsvFile(f);
    }

    public void saveCsvItemsFile(File f) throws IOException {
        Object2DJPanel.saveCsvItemsFile(f, getItems());
    }
    public void saveCsvItemsFile(File f, Collection<? extends PhysicalItem> items) throws IOException {
        Object2DJPanel.saveCsvItemsFile(f, items);
    }
    public @Nullable
    File[] takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        File csvFile = imageFileToCsvFile(f);
        this.object2DJPanel1.takeSnapshot(f,csvFile, itemsToPaint);
        runOnDispatchThread(() -> {
            updateSnapshotsTable(f, csvFile);
        });
        return new File[]{f, csvFile};
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

    public @Nullable
    PhysicalItem getClosestRobotPart() {
        if (null == aprsSystem) {
            return null;
        }
        PointType currentPoint = aprsSystem.getCurrentPosePoint();
        if (null == currentPoint) {
            return null;
        }
        return getClosestUncorrectedPart(currentPoint);
    }

    private PhysicalItem getClosestUncorrectedPart(PointType ptIn) {
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        PointType uncorrectedPoint = aprsSystem.convertRobotToVisionPoint(ptIn);
        List<PhysicalItem> l = new ArrayList<>(getItems());
        DistIndex di = getClosestDistanceIndex(uncorrectedPoint.getX(), uncorrectedPoint.getY(), l);
        return l.get(di.index);
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
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
        PointType uncorrectedPoint = aprsSystem.convertRobotToVisionPoint(ptIn);
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
        final int poseUpdateCount;

        PoseUpdateHistoryItem(
                CRCLStatusType stat,
                @Nullable CRCLCommandType cmd,
                boolean isHoldingObjectExpected,
                int poseUpdateCount,
                long time,
                Point2D.@Nullable Double capturedPartPoint,
                int captured_item_index,
                DistIndex di,
                @Nullable PhysicalItem closestItem,
                long statRecieveTime) {
            this.poseUpdateCount = poseUpdateCount;
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
        if (null == aprsSystem) {
            throw new NullPointerException("aprsSystem");
        }
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
            item.poseUpdateCount,
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
                "poseUpdateCount",
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

    private volatile @Nullable
    PoseUpdateHistoryItem lastDropUpdate = null;

    private volatile @Nullable
    ConveyorPosition lastConveyorPosition = null;

    public synchronized void handleConveyorPositionUpdate(ConveyorPosition newConveyorPosition) {
        final ConveyorPosition oldConveyorPosition = this.lastConveyorPosition;
        if (null != oldConveyorPosition
                && Double.isFinite(oldConveyorPosition.x)
                && Double.isFinite(oldConveyorPosition.y)
                && null != newConveyorPosition
                && Double.isFinite(newConveyorPosition.x)
                && Double.isFinite(newConveyorPosition.y)
                && isSimulated()) {
            double xinc = newConveyorPosition.x - oldConveyorPosition.x;
            double yinc = newConveyorPosition.y - oldConveyorPosition.y;
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
        this.lastConveyorPosition = newConveyorPosition;
        if (objectPanelToClone != null) {
            objectPanelToClone.handleConveyorPositionUpdate(newConveyorPosition);
        }
    }

    public static class Object2DOuterJPanelCurrentPoseListener implements CurrentPoseListener {

        final StackTraceElement[] createTrace;
        private Object2DOuterJPanel object2DOuterJPanel;
        private final int index;
        private static final List<Object2DOuterJPanelCurrentPoseListener> items = new ArrayList<>();
        private static final AtomicInteger itemCounter = new AtomicInteger();

        public Object2DOuterJPanelCurrentPoseListener(Object2DOuterJPanel object2DOuterJPanel) {
            createTrace = Thread.currentThread().getStackTrace();
            this.object2DOuterJPanel = object2DOuterJPanel;
            this.index = itemCounter.incrementAndGet();
            items.add(this);
        }

        @Override
        public void handlePoseUpdate(CrclSwingClientJPanel panel, CRCLStatusType stat, CRCLCommandType cmd, boolean isHoldingObjectExpected, long statRecieveTime) {
            object2DOuterJPanel.handlePoseUpdate(panel, stat, cmd, isHoldingObjectExpected, statRecieveTime);
        }

        public String getTaskName() {
            return object2DOuterJPanel.getTaskName();
        }

        @Override
        public String toString() {
            return "Object2DOuterJPanelCurrentPoseListener{\n\tindex=" + index + "\n\t task=" + getTaskName() + ", \n\tcreateTrace=" + Utils.traceToString(createTrace) + ",\n\t object2DOuterJPanel=" + object2DOuterJPanel + "\n}\n";
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + this.index;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Object2DOuterJPanelCurrentPoseListener other = (Object2DOuterJPanelCurrentPoseListener) obj;
            if (this.index != other.index) {
                return false;
            }
            return true;
        }

    }

    @SuppressWarnings("initialization")
    private final Object2DOuterJPanelCurrentPoseListener currentPoseListener
            = new Object2DOuterJPanelCurrentPoseListener(this);

    private volatile @Nullable
    Thread handlePoseUpdateThread = null;
    private final AtomicInteger poseUpdateCount = new AtomicInteger();
    private volatile StackTraceElement lastPoseUpdateTrace @Nullable []  = null;

    private void handlePoseUpdate(
            CrclSwingClientJPanel panel,
            CRCLStatusType stat,
            @Nullable CRCLCommandType cmd,
            boolean isHoldingObjectExpected,
            long statRecievTime) {
        try {
            if (null == aprsSystem) {
                throw new NullPointerException("aprsSystem");
            }
            int puc = poseUpdateCount.incrementAndGet();
            if (null == handlePoseUpdateThread) {
                handlePoseUpdateThread = Thread.currentThread();
            } else {
                if (Thread.currentThread() != handlePoseUpdateThread) {
                    System.out.println("handlePoseUpdate: lastPoseUpdateTrace = " + Utils.traceToString(lastPoseUpdateTrace));
                    throw new RuntimeException("handlePoseUpdateThread=" + handlePoseUpdateThread + ", Thread.currentThread()=" + Thread.currentThread());
                }
            }
            lastPoseUpdateTrace = Thread.currentThread().getStackTrace();
            if (!showCurrentCachedCheckBox.isSelected()) {
                return;
            }
            PoseType pose = CRCLPosemath.getPose(stat);
            if (null == pose) {
                return;
            }
            PointType ptIn = requireNonNull(pose.getPoint(), "pose.getPoint()");

            PointType uncorrectedPoint = aprsSystem.convertRobotToVisionPoint(ptIn);
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
                            puc,
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
                    println("handlePoseUpdate: captured_item = " + captured_item);
                    println("handlePoseUpdate: (time-lastIsHoldingObjectExpectedTime) = " + (time - lastIsHoldingObjectExpectedTime));
                    println("handlePoseUpdate: (time-lastNotIsHoldingObjectExpectedTime) = " + (time - lastNotIsHoldingObjectExpectedTime));
                    String errString
                            = "handlePoseUpdate: Dropping item on to another item min_dist=" + min_dist
                            + ", min_dist_index=" + min_dist_index
                            + ", captured_item_index=" + captured_item_index
                            + ", bottom item at min_dist_index =" + l.get(min_dist_index)
                            + ", captured_item  =" + captured_item;
                    try {
                        printHandlePoseInfo(errString, stat, pose, cmd);
                    } catch (IOException ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                    }
                    this.aprsSystem.setTitleErrorString(errString);
                    this.aprsSystem.pause();
                }
                if (isHoldingObjectExpected && !lastIsHoldingObjectExpected) {
                    if (min_dist < pickupDist && min_dist_index >= 0) {
                        captured_item_index = min_dist_index;
                        if (null == objectPanelToClone) {
                            try {
                                println(aprsSystem.getRunName() + " : Captured item with index " + captured_item_index + " at " + currentX + "," + currentY);
                                boolean takeSnapshots = isSnapshotsEnabled();
                                if (takeSnapshots) {
                                    final String captureMsg = "capture_" + captured_item_index + "_at_" + currentX + "_" + currentY + "_";
                                    if (isSimulated()) {
                                        takeSnapshot(createTempFile("input_" + captureMsg, ".PNG"), l);
                                        takeSnapshot(createTempFile("output_" + captureMsg, ".PNG"), getOutputItems());
                                    } else {
                                        takeSnapshot(createTempFile(captureMsg, ".PNG"), l);
                                    }
                                    printHandlePoseInfo(captureMsg, stat, pose, cmd);
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                            }
                        }
                    } else {
                        try {
                            println("handlePoseUpdate: currentThread() = " + Thread.currentThread());
                            System.out.println("");
                            System.err.println("");
                            System.out.flush();
                            System.err.flush();
                            Thread.dumpStack();
                            System.out.println("");
                            System.err.println("");
                            System.out.flush();
                            System.err.flush();
                            println("handlePoseUpdate: di = " + di);
                            if (null != di && di.index >= 0 && di.index < l.size()) {
                                PhysicalItem closestItem = l.get(di.index);
                                println("handlePoseUpdate: closestItem = " + closestItem);
                                println("handlePoseUpdate: closestItem.getFullName() = " + closestItem.getFullName());
                                println("handlePoseUpdate: closestItem.(x,y) = " + closestItem.x + "," + closestItem.y);
                            }
                            String err = "failed_to_capture_part_at_" + currentX + "_" + currentY + "_";
                            printHandlePoseInfo(err, stat, pose, cmd);
                            boolean takeSnapshots = isSnapshotsEnabled();
                            if (takeSnapshots) {
                                if (isSimulated()) {
                                    takeSnapshot(createTempFile("input_" + err, ".PNG"), l);
                                    takeSnapshot(createTempFile("output_" + err, ".PNG"), getOutputItems());
                                } else {
                                    takeSnapshot(createTempFile(err, ".PNG"), l);
                                }
                            }
                            System.err.println("handlePoseUpdate: Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);

                        } catch (Exception ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                        }
                        this.aprsSystem.setTitleErrorString("handlePoseUpdate: Tried to capture item but min_dist=" + min_dist + ", min_dist_index=" + min_dist_index);
                    }
                } else if (!isHoldingObjectExpected && lastIsHoldingObjectExpected) {
                    if (null == objectPanelToClone) {
                        println(aprsSystem.getRunName() + " : Dropping item with index " + captured_item_index + " at " + currentX + "," + currentY);
                        boolean takeSnapshots = isSnapshotsEnabled();
                        if (takeSnapshots) {
                            try {
                                takeSnapshot(createTempFile("dropping_" + captured_item_index + "_at_" + currentX + "_" + currentY + "_", ".PNG"), (PmCartesian) null, "");
                            } catch (IOException ex) {
                                Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                            }
                        }
                    }
                    if (captured_item_index < 0) {
                        String err = "handlePoseUpdate: Should be dropping item but no item captured";
                        try {
                            printHandlePoseInfo(err, stat, pose, cmd);
                        } catch (IOException ex) {
                            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
                        }
                        println("handlePoseUpdate: lastDropUpdate = " + lastDropUpdate);
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
        } catch (Exception exception) {
            disconnectCurrentPosition();
            if (showCurrentCachedCheckBox.isSelected()) {
                showCurrentCachedCheckBox.setSelected(false);
            }
            disconnect();
            Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", exception);
            showException(exception);
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        } finally {
            lastIsHoldingObjectExpected = isHoldingObjectExpected;
        }
    }

    private void printHandlePoseInfo(String infoMsg, CRCLStatusType stat, PoseType pose, @Nullable CRCLCommandType cmd) throws IOException {
        println("poseUpdateHistory = " + printPoseUpdateHistory(infoMsg));
        if (null == aprsSystem) {
            println("aprsSystem = null");
        } else {
            println("statFile = " + aprsSystem.logCrclStatus(infoMsg, stat));
            println("pose = " + CRCLPosemath.toString(pose));
            if (null != cmd) {
                println("cmd = " + CRCLSocket.commandToSimpleString(cmd));
                println("cmdFile = " + aprsSystem.logCrclCommand(infoMsg, cmd));
            }
        }
    }

    private class ClosestItemInfo {

        private final int x;
        private final int y;
        private final boolean includeTrays;

        private @Nullable
        PhysicalItem closestItem;
        private int minIndex;

        ClosestItemInfo(int x, int y, int minIndex, boolean includeTrays) {
            this.x = x;
            this.y = y;
            this.minIndex = minIndex;
            this.includeTrays = includeTrays;
            List<PhysicalItem> items = Object2DOuterJPanel.this.getItems();
            double minDist = Double.POSITIVE_INFINITY;
            PhysicalItem localClosestItem = null;
            for (int i = 0; i < items.size(); i++) {
                PhysicalItem item = items.get(i);
                if (!includeTrays) {
                    if (item.getType().equals("PT") || item.getType().equals("KT")) {
                        continue;
                    }
                }
                Point2D.Double screenItemPoint = object2DJPanel1.worldToScreenPoint(item.x, item.y, object2DJPanel1.isAutoscale());
                double diff_x = screenItemPoint.x - x;
                double diff_y = screenItemPoint.y - y;
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
