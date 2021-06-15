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

import aprs.database.PhysicalItem;
import aprs.database.Slot;
import aprs.misc.PmCartesianMinMaxLimit;
import aprs.misc.SlotOffsetProvider;
import aprs.system.AprsSystem;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.utils.CRCLPosemath;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import rcs.posemath.PmCartesian;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static aprs.database.PhysicalItem.newPhysicalItemNameRotXYScoreType;
import static aprs.misc.AprsCommonLogger.println;
import static aprs.simview.DisplayAxis.POS_X_POS_Y;
import static aprs.simview.Object2DViewDragMode.DO_NOTHING;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class Object2DJPanel extends JPanel {

    private DisplayAxis displayAxis = POS_X_POS_Y;

    private volatile boolean debugTimes = false;

    @UIEffect
    public Object2DJPanel() {

    }

    private Object2DViewDragMode dragMode = DO_NOTHING;

    /**
     * Get the value of dragMode
     *
     * @return the value of dragMode
     */
    public Object2DViewDragMode getDragMode() {
	return dragMode;
    }

    /**
     * Set the value of dragMode
     *
     * @param dragMode new value of dragMode
     */
    public void setDragMode(Object2DViewDragMode dragMode) {
	this.dragMode = dragMode;
    }

    private double senseMinX = Double.NaN;
    private double senseMaxX = Double.NaN;
    private double senseMinY = Double.NaN;
    private double senseMaxY = Double.NaN;

    private boolean showOnlyOverlapping = false;

    /**
     * Get the value of showOnlyOverlapping
     *
     * @return the value of showOnlyOverlapping
     */
    public boolean isShowOnlyOverlapping() {
	return showOnlyOverlapping;
    }

    /**
     * Set the value of showOnlyOverlapping
     *
     * @param showOnlyOverlapping new value of showOnlyOverlapping
     */
    public void setShowOnlyOverlapping(boolean showOnlyOverlapping) {
	this.showOnlyOverlapping = showOnlyOverlapping;
    }

    private boolean showOverlapping = true;

    /**
     * Get the value of showOverlapping
     *
     * @return the value of showOverlapping
     */
    public boolean isShowOverlapping() {
	return showOverlapping;
    }

    /**
     * Set the value of showOverlapping
     *
     * @param showOverlapping new value of showOverlapping
     */
    public void setShowOverlapping(boolean showOverlapping) {
	this.showOverlapping = showOverlapping;
    }

    public double getSenseMinX() {
	return senseMinX;
    }

    public void setSenseMinX(double senseMinX) {
	this.senseMinX = senseMinX;
    }

    public double getSenseMaxX() {
	return senseMaxX;
    }

    public void setSenseMaxX(double senseMaxX) {
	this.senseMaxX = senseMaxX;
    }

    public double getSenseMinY() {
	return senseMinY;
    }

    public void setSenseMinY(double senseMinY) {
	this.senseMinY = senseMinY;
    }

    public double getSenseMaxY() {
	return senseMaxY;
    }

    public void setSenseMaxY(double senseMaxY) {
	this.senseMaxY = senseMaxY;
    }

    private double rotationOffset;

    /**
     * Get the value of rotationOffset
     *
     * @return the value of rotationOffset
     */
    public double getRotationOffset() {
	return rotationOffset;
    }

    /**
     * Set the value of rotationOffset
     *
     * @param rotationOffset new value of rotationOffset
     */
    public void setRotationOffset(double rotationOffset) {
	this.rotationOffset = rotationOffset;
    }

    private boolean viewDetails;

    /**
     * Get the value of viewDetails
     *
     * @return the value of viewDetails
     */
    public boolean isViewDetails() {
	return viewDetails;
    }

    /**
     * Set the value of viewDetails
     *
     * @param viewDetails new value of viewDetails
     */
    void setViewDetails(boolean viewDetails) {
	if (this.viewDetails != viewDetails) {
	    this.viewDetails = viewDetails;
	    this.checkedRepaint();
	}
    }

    /**
     * Get the value of displayAxis
     *
     * @return the value of displayAxis
     */
    DisplayAxis getDisplayAxis() {
	return displayAxis;
    }

    /**
     * Set the value of displayAxis
     *
     * @param displayAxis new value of displayAxis
     */
    void setDisplayAxis(DisplayAxis displayAxis) {
	if (this.displayAxis != displayAxis) {
	    this.displayAxis = displayAxis;
	    this.clearSizes();
	    this.checkedRepaint();
	}
    }

    static final List<PhysicalItem> EXAMPLES_ITEMS_LIST = Arrays.asList(// PhysicalItem(String slotMaxDistExpansion,
									// double rotation, double x, double y, double
									// score, String type)
	    newPhysicalItemNameRotXYScoreType("sku_part_medium_gear", 0.10, 700.45, -140.82, 0.99, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_medium_gear", 0.79, 528.60, -122.51, 0.95, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_medium_gear", -0.60, 529.98, 213.96, 0.94, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_medium_gear", -0.02, 527.61, -205.06, 0.91, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_medium_gear", -0.75, 216.66, 128.56, 0.91, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_small_gear", 0.53, 509.01, -11.83, 0.95, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_small_gear", -0.23, 640.49, 32.88, 0.89, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_small_gear", -0.23, 640.49, 32.88, 0.89, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_small_gear", -0.07, 310.04, -102.02, 0.65, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_part_small_gear", -0.31, 321.38, 177.59, 0.61, "P"),
	    newPhysicalItemNameRotXYScoreType("sku_kit_s2l2_vessel", -0.02, 295.65, -296.90, 0.80, "KT"),
	    newPhysicalItemNameRotXYScoreType("sku_kit_s2l2_vessel", 0.01, 310.90, -20.87, 0.73, "KT"),
	    newPhysicalItemNameRotXYScoreType("sku_small_gear_vessel", -0.03, 609.22, 5.09, 0.95, "PT"),
	    newPhysicalItemNameRotXYScoreType("sku_medium_gear_vessel", 0.00, 569.17, -161.29, 0.67, "PT"),
	    newPhysicalItemNameRotXYScoreType("sku_kit_m2l1_vessel", -1.57, 579.86, 170.14, 0.96, "KT"));
    private volatile List<PhysicalItem> items = EXAMPLES_ITEMS_LIST;

    public void setItems(List<PhysicalItem> items) {
	if (this.items != items) {
	    this.items = items;
	    this.checkedRepaint();
	}
	updateAddedExtras();
    }

    private void clearAddedSlotInfo() {
	if (null == addedSlots || !addedSlots.isEmpty()) {
	    this.addedSlots = Collections.emptyList();
	}
	if (null == addedOutputSlots || !addedOutputSlots.isEmpty()) {
	    this.addedOutputSlots = Collections.emptyList();
	}
	if (null == outputItemsWithAddedExtras || !outputItemsWithAddedExtras.isEmpty()) {
	    this.outputItemsWithAddedExtras = Collections.emptyList();
	}
	if (null == itemsWithAddedExtras || !itemsWithAddedExtras.isEmpty()) {
	    this.itemsWithAddedExtras = Collections.emptyList();
	}
    }

    private boolean showOutputItems = false;

    /**
     * Get the value of showOutputItems
     *
     * @return the value of showOutputItems
     */
    boolean isShowOutputItems() {
	return showOutputItems;
    }

    /**
     * Set the value of showOutputItems
     *
     * @param showOutputItems new value of showOutputItems
     */
    public void setShowOutputItems(boolean showOutputItems) {
	if (this.showOutputItems != showOutputItems) {
	    this.showOutputItems = showOutputItems;
	    this.checkedRepaint();
	}
    }

    private List<PhysicalItem> outputItems = Collections.emptyList();

    /**
     * Get the value of outputItems
     *
     * @return the value of outputItems
     */
    List<PhysicalItem> getOutputItems() {
	if (null != outputItems) {
	    return Collections.unmodifiableList(outputItems);
	}
	return Collections.emptyList();
    }

    /**
     * Set the value of outputItems
     *
     * @param outputItems new value of outputItems
     */
    public void setOutputItems(List<PhysicalItem> outputItems) {
	if (this.outputItems != outputItems) {
	    this.outputItems = outputItems;
	    if (this.showOutputItems) {
		this.checkedRepaint();
	    }
	}
	updateAddedExtras();
    }

    private List<Slot> addedOutputSlots = Collections.emptyList();

    /**
     * Get the value of addedOutputSlots
     *
     * @return the value of addedOutputSlots
     */
    public List<Slot> getAddedOutputSlots() {
	return addedOutputSlots;
    }

    private List<Slot> addedSlots = Collections.emptyList();

    /**
     * Get the value of addedSlots
     *
     * @return the value of addedSlots
     */
    public List<Slot> getAddedSlots() {
	return addedSlots;
    }

    private List<PhysicalItem> addedTools = Collections.emptyList();

    /**
     * Get the value of addedTools
     *
     * @return the value of addedTools
     */
    public List<PhysicalItem> getAddedTools() {
	return addedTools;
    }

    private List<PhysicalItem> itemsWithAddedExtras = Collections.emptyList();

    /**
     * Get the value of itemsWithAddedExtras
     *
     * @return the value of itemsWithAddedExtras
     */
    List<PhysicalItem> getItemsWithAddedExtras() {
	return itemsWithAddedExtras;
    }

    /**
     * Set the value of itemsWithAddedExtras
     *
     * @param itemsWithAddedExtras new value of itemsWithAddedExtras
     */
    public void setItemsWithAddedExtras(List<PhysicalItem> itemsWithAddedExtras) {
	this.itemsWithAddedExtras = itemsWithAddedExtras;
    }

    private List<PhysicalItem> outputItemsWithAddedExtras = Collections.emptyList();

    /**
     * Get the value of outputItemsWithAddedExtras
     *
     * @return the value of outputItemsWithAddedExtras
     */
    public List<PhysicalItem> getOutputItemsWithAddedExtras() {
	return outputItemsWithAddedExtras;
    }

    /**
     * Set the value of outputItemsWithAddedExtras
     *
     * @param outputItemsWithAddedExtras new value of outputItemsWithAddedExtras
     */
    public void setOutputItemsWithAddedExtras(List<PhysicalItem> outputItemsWithAddedExtras) {
	this.outputItemsWithAddedExtras = outputItemsWithAddedExtras;
    }

    /**
     * Set the value of addedSlots
     *
     * @param addedSlots new value of addedSlots
     */
    public void setAddedSlots(List<Slot> addedSlots) {
	this.addedSlots = addedSlots;
    }

    @SuppressWarnings("guieffect")
    public void takeSnapshot(File f, PoseType pose, String label) {
	final int w = this.getWidth();
	final int h = this.getHeight();
	if (w < 1 || h < 1) {
	    System.err.println("Can not take snapshot with sized to " + w + " x " + h);
	    return;
	}
	takeSnapshot(f, imageFileToCsvFile(f), pose, label, w, h);
    }

    @SuppressWarnings("guieffect")
    public void takeSnapshot(File f, File csvFile, PoseType pose, String label) {
	final int w = this.getWidth();
	final int h = this.getHeight();
	if (w < 1 || h < 1) {
	    System.err.println("Can not take snapshot with sized to " + w + " x " + h);
	    return;
	}
	takeSnapshot(f, csvFile, pose, label, w, h);
    }

    public static File imageFileToCsvFile(File f) {
	try {
	    File csvDir = new File(f.getParentFile(), "csv");
	    csvDir.mkdirs();
	    File csvFile = new File(csvDir, f.getName() + ".csv");
	    return csvFile;
	} catch (Exception ex) {
	    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, "", ex);
	    if (ex instanceof RuntimeException) {
		throw (RuntimeException) ex;
	    } else {
		throw new RuntimeException(ex);
	    }
	}
    }

    private int snapshotHeight = -1;
    private int snapshotWidth = -1;

    public int getSnapshotHeight() {
	return snapshotHeight;
    }

    public void setSnapshotHeight(int snapshotHeight) {
	this.snapshotHeight = snapshotHeight;
    }

    public int getSnapshotWidth() {
	return snapshotWidth;
    }

    public void setSnapshotWidth(int snapshotWidth) {
	this.snapshotWidth = snapshotWidth;
    }

    private final static int DEFAULT_SNAPW = 100;

    @SuppressWarnings("guieffect")
    private int snapW() {
	if (snapshotWidth > 0) {
	    return snapshotWidth;
	} else if (this.getWidth() > 0) {
	    return this.getWidth();
	} else {
	    return DEFAULT_SNAPW;
	}
    }

    private final static int DEFAULT_SNAPH = 100;

    @SuppressWarnings("guieffect")
    private int snapH() {
	if (snapshotHeight > 0) {
	    return snapshotHeight;
	} else if (this.getHeight() > 0) {
	    return this.getHeight();
	} else {
	    return DEFAULT_SNAPH;
	}
    }

//    public void takeSnapshot(File f, PointType point, String label) {
//	final int w = snapW();
//	final int h = snapH();
//	if (w < 1 || h < 1) {
//	    System.err.println("Can not take snapshot with sized to " + w + " x " + h);
//	    return;
//	}
//	takeSnapshot(f, imageFileToCsvFile(f), point, label, w, h);
//    }

//    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) {
//	final int w = snapW();
//	final int h = snapH();
//	if (w < 1 || h < 1) {
//	    System.err.println("Can not take snapshot with sized to " + w + " x " + h);
//	    return;
//	}
//	takeSnapshot(f, imageFileToCsvFile(f), point, label, w, h);
//    }

    public void takeSnapshot(File f, File csvFile, @Nullable PmCartesian point, @Nullable String label) {
	final int w = snapW();
	final int h = snapH();
	if (w < 1 || h < 1) {
	    System.err.println("Can not take snapshot with sized to " + w + " x " + h);
	    return;
	}
	takeSnapshot(f, csvFile, point, label, w, h);
    }

    private void takeSnapshot(File f, File csvFile, PoseType pose, String label, final int w, final int h) {
	if (null != pose) {
	    takeSnapshot(f, csvFile, pose.getPoint(), label, w, h);
	} else {
	    takeSnapshot(f, csvFile, (PmCartesian) null, (String) null, w, h);

	}
    }

    private void takeSnapshot(File f, File csvFile, @Nullable PointType point, String label, final int w, final int h) {
	if (null != point) {
	    takeSnapshot(f, csvFile, CRCLPosemath.toPmCartesian(point), label, w, h);
	} else {
	    takeSnapshot(f, csvFile, (PmCartesian) null, (String) null, w, h);
	}
    }

    public void saveCsvItemsFile(File csvFile) throws IOException {
	saveCsvItemsFile(csvFile, getItems());
    }

    public static void saveCsvItemsFile(File csvFile, Collection<? extends PhysicalItem> items) throws IOException {
	try (PrintWriter pw = new PrintWriter(new FileWriter(csvFile))) {
	    pw.println("name,rotation,x,y,score,type");
	    for (PhysicalItem item : items) {
		if (null != item) {
		    pw.println(item.getName() + "," + item.getRotation() + "," + item.x + "," + item.y + ","
			    + item.getScore() + "," + item.getType());
		} else {
		    System.err.println("contains null : items=" + items);
		    Thread.dumpStack();
		}
	    }
	} catch (IOException ex) {
	    System.err.println("csvFile=" + csvFile);
	    throw new IOException("csvFile=" + csvFile + ", length=" + csvFile.getCanonicalPath().length(), ex);
	}
    }

    @SuppressWarnings("guieffect")
    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label, final int w, final int h) {
	takeSnapshot(f, imageFileToCsvFile(f), point, label, w, h);
    }

    @SuppressWarnings("guieffect")
    public void takeSnapshot(File f, File csvFile, @Nullable PmCartesian point, @Nullable String label, final int w,
	    final int h) {
	try {
	    String type = "JPEG";
	    int pindex = f.getName().lastIndexOf('.');
	    if (pindex > 0) {
		type = f.getName().substring(pindex + 1);
	    }
	    ViewOptions opts = currentViewOptions();
	    opts.w = w;
	    opts.h = h;
	    List<PhysicalItem> itemsToPaint = getItemsToPaint();
	    writeImageFile(opts, type, f, csvFile, itemsToPaint, point, label);
//            println("Saved snapshot to " + f.getCanonicalPath());
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
	}
    }

    private List<PhysicalItem> getItemsToPaint() {
	List<PhysicalItem> itemsToPaint = this.items;
	if ((showAddedSlotPositions || showAddedToolsAndToolHolders) && null != this.itemsWithAddedExtras) {
	    itemsToPaint = this.itemsWithAddedExtras;
	}
	if (showOutputItems && null != outputItems) {
	    itemsToPaint = this.outputItems;
	    if ((showAddedSlotPositions || showAddedToolsAndToolHolders) && null != this.outputItemsWithAddedExtras) {
		itemsToPaint = this.outputItemsWithAddedExtras;
	    }
	}
	return itemsToPaint;
    }

    @SuppressWarnings("guieffect")
    public void takeSnapshot(File f, File csvFile, Collection<? extends PhysicalItem> itemsToPaint) {
	final int w = this.getWidth();
	final int h = this.getHeight();
	if (w < 1 || h < 1) {
	    System.err.println("Can not take snapshot with sized to " + w + " x " + h);
	    return;
	}
	takeSnapshot(f, csvFile, itemsToPaint, w, h);
    }

    private static final Executor imageIOWriterService = Executors.newSingleThreadExecutor(new ThreadFactory() {
	@Override
	public Thread newThread(Runnable r) {
	    return new Thread(r, "imageIOWriterService");
	}
    });

    public ViewOptions currentViewOptions() {
	ViewOptions opts = new ViewOptions();
	copyToOpts(opts);
	return opts;
    }

    private void copyToOpts(ViewOptions opts) {
	opts.showCurrentXY = this.showCurrentXY;
	opts.currentX = this.currentX;
	opts.currentY = this.currentY;
	opts.defaultRotationOffset = this.rotationOffset;
	if (opts.w < 1 || opts.h < 1) {
	    opts.w = this.getSize().width;
	    opts.h = this.getSize().height;
	}
	if (null == opts.backgroundColor) {
	    opts.backgroundColor = this.getBackground();
	}
	if (null == opts.foregroundColor) {
	    opts.foregroundColor = this.getForeground();
	}
	opts.defaultAutoscale = this.autoscale;
	opts.useSeparateNames = this.useSeparateNames;
	if (opts.minmax == null) {
	    Point2DMinMax minmax = getMinmax(autoscale);
	    if (autoscale && minmax == null) {
		minmax = computeAutoscaleMinMax(opts, items);
		this.autoScaledMinMax = minmax;
	    }
	    boolean doAutoscale;
	    if (opts.useOverridingAutoscale) {
		doAutoscale = opts.overridingAutoscale;
	    } else {
		doAutoscale = opts.defaultAutoscale;
	    }
	    if (!doAutoscale) {
		if (!Double.isFinite(minmax.max.x) || !Double.isFinite(minmax.min.x) || !Double.isFinite(minmax.min.y)
			|| !Double.isFinite(minmax.max.y)) {
		    throw new IllegalArgumentException("Limits must be finite: (" + minmax.min.x + "," + minmax.min.y
			    + "," + minmax.max.x + "," + minmax.max.y + ")");
		}
	    }
	    if (null == minmax) {
		throw new RuntimeException("minmax=" + minmax);
	    }
	    opts.minmax = new Point2DMinMax();
	    opts.minmax.min.x = minmax.min.x;
	    opts.minmax.min.y = minmax.min.y;
	    opts.minmax.max.x = minmax.max.x;
	    opts.minmax.max.y = minmax.max.y;
	}
	opts.showOnlyOverlapping = this.showOnlyOverlapping;
	opts.showOverlapping = this.showOverlapping;
	if (this.showOverlapping || this.showOnlyOverlapping) {
	    final AprsSystem thisAprsSystemLocal = this.aprsSystem;
	    if (null != thisAprsSystemLocal) {
		opts.aprsSystem = thisAprsSystemLocal;
	    }
	}
	if (null == opts.displayAxis) {
	    opts.displayAxis = this.displayAxis;
	}
	opts.mouseInside = this.mouseInside;
	final Point thisMousePointLocal = this.mousePoint;
	if (null == opts.mousePoint && null != thisMousePointLocal) {
	    opts.mousePoint = thisMousePointLocal;
	}
	if (opts.mouseInside && opts.mousePoint != null) {
	    double scale = getScale(autoscale);
	    if (Double.isFinite(scale) && Math.abs(scale) > 1E-9 && opts.mousePoint != null) {
		opts.worldMousePoint = screenToWorldPoint(opts.mousePoint.x, opts.mousePoint.y, this.autoscale);
	    }
	}
	opts.viewRotationsAndImages = this.viewRotationsAndImages;
	opts.alternativeRotation = this.alternativeRotation;
	opts.viewDetails = this.viewDetails;
	final SlotOffsetProvider thisSlotOffsetProviderLocal = this.slotOffsetProvider;
	if (null == opts.slotOffsetProvider && thisSlotOffsetProviderLocal != null) {
	    opts.slotOffsetProvider = thisSlotOffsetProviderLocal;
	}
	opts.slotMaxDistExpansion = this.slotMaxDistExpansion;
	opts.capturedPartPoint = this.capturedPartPoint;
	opts.endEffectorClosed = this.endEffectorClosed;
	opts.senseMinX = this.senseMinX;
	opts.senseMaxX = this.senseMaxX;
	opts.senseMinY = this.senseMinY;
	opts.senseMaxY = this.senseMaxY;
    }

    public void takeSnapshot(File f, File csvFile, Collection<? extends PhysicalItem> itemsToPaint, final int w,
	    final int h) {
	try {
	    ViewOptions opts = currentViewOptions();
	    opts.h = h;
	    opts.w = w;
	    String type = "JPEG";
	    writeImageFile(opts, type, f, csvFile, itemsToPaint);
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
	}
    }

    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, final int w, final int h) {
	try {
	    ViewOptions opts = currentViewOptions();
	    opts.h = h;
	    opts.w = w;
	    String type = "JPEG";
	    writeImageFile(opts, type, f, imageFileToCsvFile(f), itemsToPaint);
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
	}
    }

    private static final AtomicInteger imageIOWriterServiceSubmittedCount = new AtomicInteger();
    private static final AtomicInteger imageIOWriterServiceFinishCount = new AtomicInteger();
    private static final AtomicInteger imageIOWriterServiceSkipCount = new AtomicInteger();

    private static void writeImageFile(ViewOptions opts, String type, File f, File csvFile,
	    Collection<? extends PhysicalItem> items) {
	writeImageFile(opts, type, f, csvFile, items, null, null);
    }

    private static volatile @Nullable List<PhysicalItem> lastItemsCopy = null;
    private static volatile @Nullable List<PhysicalItem> secondToLastItemsCopy = null;
    private static volatile @Nullable PmCartesian lastPoint = null;
    private static volatile @Nullable String lastLabel = null;

    private static boolean itemListEquals(List<PhysicalItem> l1, List<PhysicalItem> l2) {
	if (null == l1) {
	    if (null == l2) {
		return true;
	    } else {
		return false;
	    }
	} else {
	    if (null == l2) {
		return false;
	    } else if (l1.size() != l2.size()) {
		return false;
	    } else {
		for (int i = 0; i < l1.size(); i++) {
		    PhysicalItem l1ItemI = l1.get(i);
		    PhysicalItem l2ItemI = l2.get(i);
		    if (!Objects.equals((l1ItemI).getName(), l2ItemI.getName())) {
			return false;
		    }
		    double xdiff = l1ItemI.x - l2ItemI.x;
		    if (Math.abs(xdiff) > 0.01) {
			return false;
		    }
		    double ydiff = l1ItemI.y - l2ItemI.y;
		    if (Math.abs(ydiff) > 0.01) {
			return false;
		    }
		}
		return true;
	    }
	}
    }

    private static void writeImageFile(
	    ViewOptions opts,
	    String type,
	    File f,
	    File csvFile,
	    Collection<? extends PhysicalItem> items,
	    @Nullable PmCartesian point,
	    @Nullable String label) {
	boolean doAutoscale;
	if (opts.useOverridingAutoscale) {
	    doAutoscale = opts.overridingAutoscale;
	} else {
	    doAutoscale = opts.defaultAutoscale;
	}
	if (!doAutoscale) {
	    final Point2DMinMax optsMinmaxLocal = opts.minmax;
	    if (null == optsMinmaxLocal) {
		throw new NullPointerException("opts.minmax");
	    }
	    if (!Double.isFinite(optsMinmaxLocal.max.x) || !Double.isFinite(optsMinmaxLocal.min.x)
		    || !Double.isFinite(optsMinmaxLocal.min.y) || !Double.isFinite(optsMinmaxLocal.max.y)) {
		throw new IllegalArgumentException("Limits must be finite: (" + optsMinmaxLocal.min.x + ","
			+ optsMinmaxLocal.min.y + "," + optsMinmaxLocal.max.x + "," + optsMinmaxLocal.max.y + ")");
	    }
	}
	if (null == items || items.isEmpty()) {
	    return;
	}
	List<PhysicalItem> itemsCopy = new ArrayList<>(items);
	Collections.sort(itemsCopy, java.util.Comparator.comparing(PhysicalItem::getName));
	if (point == null || (Objects.equals(lastLabel, label) && Objects.equals(lastPoint, point))) {
	    if (itemListEquals(lastItemsCopy, itemsCopy)) {
		return;
	    }
	    if (itemListEquals(secondToLastItemsCopy, itemsCopy)) {
		return;
	    }
	}
	secondToLastItemsCopy = lastItemsCopy;
	lastItemsCopy = itemsCopy;
	lastPoint = point;
	lastLabel = label;
	int submitCount = imageIOWriterServiceSubmittedCount.get();
	int finishCount = imageIOWriterServiceFinishCount.get();
	if (submitCount - finishCount > 10) {
	    int skipCount = imageIOWriterServiceSkipCount.incrementAndGet();
	    System.err.println("writeImageFile: submitCount = " + submitCount);
	    System.err.println("writeImageFile: finishCount = " + finishCount);
	    System.err.println("writeImageFile: skipCount = " + skipCount);
	    System.err.println("writeImageFile: skipping " + f);
	    return;
	}
	// noinspection UnusedAssignment
	int newSubmitCount = imageIOWriterServiceSubmittedCount.incrementAndGet();
	imageIOWriterService.execute(() -> writeImageFileOnService(opts, type, f, csvFile, itemsCopy, point, label));
    }

    private static void writeImageFileOnService(
	    ViewOptions opts,
	    String type,
	    File f,
	    File csvFile,
	    Collection<? extends PhysicalItem> itemsToPaint,
	    @Nullable PmCartesian point,
	    @Nullable String label) {
	try {

	    saveCsvItemsFile(csvFile, itemsToPaint);
	    BufferedImage img = createSnapshotImage(opts, itemsToPaint);
	    Graphics2D g2d = img.createGraphics();
	    if (null != point) {
		paintHighlightedPose(opts, itemsToPaint.size(), point, g2d, label);
	    }
	    int pindex = f.getName().lastIndexOf('.');
	    if (pindex > 0) {
		type = f.getName().substring(pindex + 1);
	    }
	    if (ImageIO.write(img, type, f)) {
//                println("Saved snapshot to " + f.getCanonicalPath());
	    } else {
		println("Can't take snapshot. ImageIO.write: No approriate writer found for type=" + type + ", f=" + f);
	    }
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
	} finally {
	    int finishCount = imageIOWriterServiceFinishCount.incrementAndGet();
	    int skipCount = imageIOWriterServiceSkipCount.get();
	    int submitCount = imageIOWriterServiceSubmittedCount.get();
	    if (skipCount != lastFinishSkipCount && submitCount == finishCount) {
		System.out.println("finishCount=" + finishCount + ", skipCount = " + skipCount
			+ ", lastFinishSkipCount=" + lastFinishSkipCount);
		lastFinishSkipCount = skipCount;
	    }
	}
    }

    private static volatile int lastFinishSkipCount = -1;

    BufferedImage createSnapshotImage() {
	return createSnapshot(null);
    }

    public BufferedImage createSnapshot(@Nullable ViewOptions opts) {
	if (null == opts) {
	    opts = currentViewOptions();
	} else {
	    copyToOpts(opts);
	}
	if (null != opts && opts.addExtras) {
	    return createSnapshotImage(opts, itemsWithAddedExtras);
	} else {
	    return createSnapshotImage(opts, items);
	}
    }

    public BufferedImage createSnapshot(@Nullable ViewOptions opts, Collection<? extends PhysicalItem> itemsToPaint) {
	if (null != opts) {
	    this.copyToOpts(opts);
	    return Object2DJPanel.createSnapshotImage(opts, itemsToPaint);
	} else {
	    return Object2DJPanel.createSnapshotImage(this.currentViewOptions(), itemsToPaint);
	}
    }

    @SuppressWarnings("guieffect")
    private static BufferedImage createSnapshotImage(@Nullable ViewOptions opts,
	    Collection<? extends PhysicalItem> itemsToPaint) {

	if (null == opts || opts.w < 1 || opts.h < 1) {
	    throw new IllegalArgumentException("opts=" + opts);
	}
	boolean doAutoscale;
	if (opts.useOverridingAutoscale) {
	    doAutoscale = opts.overridingAutoscale;
	} else {
	    doAutoscale = opts.defaultAutoscale;
	}
	final Point2DMinMax optsMinmaxLocal = opts.minmax;
	if (!doAutoscale) {
	    if (null == optsMinmaxLocal) {
		throw new NullPointerException("opts.minmax");
	    }
	    if (!Double.isFinite(optsMinmaxLocal.max.x) || !Double.isFinite(optsMinmaxLocal.min.x)
		    || !Double.isFinite(optsMinmaxLocal.min.y) || !Double.isFinite(optsMinmaxLocal.max.y)) {
		throw new IllegalArgumentException("Limits must be finite: (" + optsMinmaxLocal.min.x + ","
			+ optsMinmaxLocal.min.y + "," + optsMinmaxLocal.max.x + "," + optsMinmaxLocal.max.y + ")");
	    }
	}
	int w = Math.max(opts.w, 100);
	int h = Math.max(opts.h, 100);
	BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
	Graphics2D g2d = img.createGraphics();
	if (null != opts.backgroundColor) {
	    g2d.setColor(opts.backgroundColor);
	}
	g2d.fillRect(0, 0, w, h);
	if (null != opts.foregroundColor) {
	    g2d.setColor(opts.foregroundColor);
	}
	boolean origUseSepNames = opts.useSeparateNames;
//        boolean origViewLimitsLine = this.viewLimitsLine;
//        boolean origAutoscale = autoscale;
	if (doAutoscale) {
	    paintWithAutoScale(itemsToPaint, null, g2d, opts);
	} else {
	    if (null == optsMinmaxLocal) {
		throw new NullPointerException("opts.minmax");
	    }
	    paintItems(g2d, itemsToPaint, null, optsMinmaxLocal, opts);
	}
	return img;
    }

    private volatile double autoScaledScale = -1;

    private volatile @Nullable Point2DMinMax autoScaledMinMax = null;

    @SuppressWarnings("guieffect")
    private static void paintWithAutoScale(
	    Collection<? extends PhysicalItem> itemsToPaint,
	    @Nullable PhysicalItem selectedItem,
	    Graphics2D g2d,
	    @Nullable ViewOptions opts) {
	try {
	    if (itemsToPaint.isEmpty()) {
		return;
	    }
	    if (null == opts || opts.w < 1 || opts.h < 1) {
		throw new IllegalArgumentException("opts=" + opts);
	    }
	    int w = Math.max(opts.w, 100);
	    int h = Math.max(opts.h, 100);

	    Point2DMinMax tempMinMax = computeAutoscaleMinMax(opts, itemsToPaint);
//            if ( opts.paintingComponent) {
//                autoScaledMinMax = tempMinMax;
//                autoScaledScale = computeNewScale(tempMinMax, opts);
//            }
	    paintItems(g2d, itemsToPaint, selectedItem, tempMinMax, opts);
	} catch (Exception e) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", e);
	    g2d.drawString(e.toString(), TO_SCREEN_X_OFFSET, TO_SCREEN_Y_OFFSET);
	}
    }

    private static Point2DMinMax computeAutoscaleMinMax(ViewOptions opts,
	    Collection<? extends PhysicalItem> itemsToPaint) {
	double minX = Double.POSITIVE_INFINITY;
	double maxX = Double.NEGATIVE_INFINITY;
	double minY = Double.POSITIVE_INFINITY;
	double maxY = Double.NEGATIVE_INFINITY;
	if (opts.showCurrentXY && (null == opts || !opts.disableShowCurrent)) {
	    minX = opts.currentX;
	    maxX = opts.currentX;
	    minY = opts.currentY;
	    maxY = opts.currentY;
	}
	for (PhysicalItem item : itemsToPaint) {
	    double itemSize = item.getMaxSlotDist();
	    if (null != partImageMap && !partImageMap.isEmpty()) {
		PartImageInfo info = getPartImageInfo(item);
		if (info != null) {
		    itemSize = Math.max(itemSize, info.width);
		}
	    }
	    if (minX > item.x - itemSize) {
		minX = item.x - itemSize;
	    }
	    if (minY > item.y - itemSize) {
		minY = item.y - itemSize;
	    }
	    if (maxX < item.x + itemSize) {
		maxX = item.x + itemSize;
	    }
	    if (maxY < item.y + itemSize) {
		maxY = item.y + itemSize;
	    }
	}
	Point2DMinMax tempMinMax = new Point2DMinMax();
	tempMinMax.min.x = minX;
	tempMinMax.max.x = maxX;
	tempMinMax.min.y = minY;
	tempMinMax.max.y = maxY;
	return tempMinMax;
    }

    @SuppressWarnings("guieffect")
    private static void paintHighlightedPose(
	    ViewOptions opts,
	    int itemsToPaintSize,
	    @Nullable PmCartesian point,
	    Graphics2D g2d,
	    @Nullable String label) {
	DisplayAxis displayAxis = opts.displayAxis;
	if (null == displayAxis) {
	    throw new NullPointerException("opts.displayAxis");
	}
	boolean useSeparateNames = opts.useSeparateNames;
	final Point2DMinMax optsMinmaxLocal = opts.minmax;
	if (null == optsMinmaxLocal) {
	    throw new NullPointerException("opts.minmax");
	}
	Point2DMinMax minmaxParam = optsMinmaxLocal;
	double minX = minmaxParam.min.x;
	double minY = minmaxParam.min.y;
	double maxX = minmaxParam.max.x;
	double maxY = minmaxParam.max.y;
	int width = opts.w;
	int height = opts.h;
	double currentScale = opts.scale;
	if (null != point) {
	    if (label == null) {
		label = "(null)";
	    }
	    double x = point.getX();
	    double y = point.getY();
	    double displayMaxY = maxY;
	    double displayMinY = minY;
	    double displayMinX = minX;
	    double displayMaxX = maxX;

	    switch (displayAxis) {
	    case POS_X_POS_Y:
		displayMaxX = (width - TO_SCREEN_X_OFFSET) / currentScale + minX;
		displayMinX = (0 - TO_SCREEN_X_OFFSET) / currentScale + minX;

		displayMinY = maxY - (height - TO_SCREEN_Y_OFFSET) / currentScale;
		displayMaxY = maxY - (0 - TO_SCREEN_Y_OFFSET) / currentScale;
		break;

	    case POS_Y_NEG_X:
		displayMaxX = (height - TO_SCREEN_Y_OFFSET) / currentScale + minX;
		displayMinX = (0 - TO_SCREEN_X_OFFSET) / currentScale + minX;

		displayMinY = (width - TO_SCREEN_X_OFFSET) / currentScale + minY;
		displayMaxY = (0 - TO_SCREEN_Y_OFFSET) / currentScale + minY;
		break;

	    case NEG_X_NEG_Y:
		displayMaxX = maxX - (width - TO_SCREEN_X_OFFSET) / currentScale;
		displayMinX = maxX - (0 - TO_SCREEN_X_OFFSET) / currentScale;

		displayMinY = (height - TO_SCREEN_Y_OFFSET) / currentScale + minY;
		displayMaxY = (0 - TO_SCREEN_Y_OFFSET) / currentScale + minY;
		break;

	    case NEG_Y_POS_X:
		displayMaxX = maxX - (height - TO_SCREEN_Y_OFFSET) / currentScale;
		displayMinX = maxX - (0 - TO_SCREEN_X_OFFSET) / currentScale;

		displayMinY = (width - TO_SCREEN_X_OFFSET) / currentScale + minY;
		displayMaxY = (0 - TO_SCREEN_Y_OFFSET) / currentScale + minY;
		break;
	    }
	    if (useSeparateNames) {
		g2d.setColor(Color.BLACK);
		int i = itemsToPaintSize;
		double namex = maxX + (maxX - minX) / 10.0;
		double namey = displayMinY + ((double) (i + 1)) / (itemsToPaintSize + 2) * (displayMaxY - displayMinY);
		switch (displayAxis) {
		case POS_X_POS_Y:
		    break;

		case POS_Y_NEG_X:
		    namex = displayMinX + ((double) (i + 1)) / (itemsToPaintSize + 2) * (displayMaxX - displayMinX);
		    namey = maxY + (maxY - minY) / 10.0;
		    break;

		case NEG_X_NEG_Y:
		    namex = minX - (maxX - minX) / 10.0;
		    break;

		case NEG_Y_POS_X:
		    namex = displayMinX + ((double) (i + 1)) / (itemsToPaintSize + 2) * (displayMaxX - displayMinX);
		    namey = minY - (maxY - minY) / 10.0;
		    break;
		}
		translate(displayAxis, g2d, namex, namey, minmaxParam, width, height, currentScale);
		g2d.setColor(Color.GREEN);
		Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * label.length(), TO_SCREEN_Y_OFFSET);
		g2d.fill(rect);
		g2d.setColor(Color.BLACK);
		g2d.drawString(label, 0, 0);
//                if (null != origTransform) {
//                    g2d.setTransform(origTransform);
//                }
		g2d.draw(
			new Line2D.Double(
				toScreenPoint(displayAxis, namex, namey, minmaxParam, currentScale),
				toScreenPoint(displayAxis, x, y, minmaxParam, currentScale)));
	    }
	    translate(displayAxis, g2d, x, y, minmaxParam, width, height, currentScale);
	    g2d.setColor(Color.GREEN);
	    Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeparateNames ? 1 : label.length()),
		    TO_SCREEN_Y_OFFSET);
	    g2d.fill(rect);
	    g2d.setColor(Color.BLACK);
	    g2d.draw(rect);
	    if (!useSeparateNames) {
		g2d.drawString(label, 0, 0);
	    }
	}
    }

    private boolean autoscale = true;

    /**
     * Get the value of autoscale
     *
     * @return the value of autoscale
     */
    public boolean isAutoscale() {
	return autoscale;
    }

    /**
     * Set the value of autoscale
     *
     * @param autoscale new value of autoscale
     */
    void setAutoscale(boolean autoscale) {
	if (this.autoscale != autoscale) {
	    this.clearSizes();
	    this.autoscale = autoscale;
	    this.scale_set = false;
	    this.checkedRepaint();
	}
    }

    private int selectedItemIndex = -1;

    /**
     * Get the value of selectedItemIndex
     *
     * @return the value of selectedItemIndex
     */
    int getSelectedItemIndex() {
	return selectedItemIndex;
    }

    private boolean viewRotationsAndImages = true;

    /**
     * Get the value of viewRotationsAndImages
     *
     * @return the value of viewRotationsAndImages
     */
    public boolean isViewRotationsAndImages() {
	return viewRotationsAndImages;
    }

    /**
     * Set the value of viewRotationsAndImages
     *
     * @param viewRotationsAndImages new value of viewRotationsAndImages
     */
    void setViewRotationsAndImages(boolean viewRotationsAndImages) {
	if (this.viewRotationsAndImages != viewRotationsAndImages) {
	    this.viewRotationsAndImages = viewRotationsAndImages;
	    this.checkedRepaint();
	}
    }

    /**
     * Set the value of selectedItemIndex
     *
     * @param newsSelectedItemIndex new value of selectedItemIndex
     */
    void setSelectedItemIndex(int newsSelectedItemIndex) {
	int oldSelectedItemIndex = this.selectedItemIndex;
	if (oldSelectedItemIndex != newsSelectedItemIndex) {
	    this.selectedItemIndex = newsSelectedItemIndex;
	    this.checkedRepaint();
	}
    }

    /**
     * Get the value of items
     *
     * @return the value of items
     */
    public List<PhysicalItem> getItems() {
	return items;
    }

    private double scale;

    /**
     * Get the value of scale
     *
     * @return the value of scale
     */
    public double getScale(boolean autoscale) {
	if (autoscale && autoScaledScale > 0) {
	    return autoScaledScale;
	}
	return scale;
    }

//    private Point2D.Double minCorner = new Point2D.Double();
//
//    /**
//     * Get the value of minCorner
//     *
//     * @return the value of minCorner
//     */
//    public Point2D.Double getMinCorner() {
//        return minCorner;
//    }
    private final Point2DMinMax minmax = new Point2DMinMax();

    public Point2DMinMax getMinmax(boolean autoscale) {
	if (autoscale && null != autoScaledMinMax) {
	    return autoScaledMinMax;
	}
	return minmax;
    }

//    public Point2DMinMax getAutoScaledMinmax() {
//        if (null != autoScaledMinMax) {
//            return autoScaledMinMax;
//        }
//        return getMinmax();
//    }
//
//    public double getAutoScaledScale() {
//        if (autoScaledScale > 0) {
//            return autoScaledScale;
//        }
//        return getScale();
//    }
//    @SuppressWarnings("guieffect")
//    private void translate(Graphics2D g2d, double itemx, double itemy, double minX, double minY, double maxX, double maxY, int width, int height, double currentScale) {
//
//        Point2DMinMax tempMinMax = new Point2DMinMax();
//        tempMinMax.min.x = minX;
//        tempMinMax.max.x = maxX;
//        tempMinMax.min.y = minY;
//        tempMinMax.max.y = maxY;
//        translate(this.displayAxis, g2d, itemx, itemy, tempMinMax, width, height, currentScale);
//    }

    @SuppressWarnings("guieffect")
    private static void translate(DisplayAxis displayAxis, Graphics2D g2d, double itemx, double itemy,
	    Point2DMinMax tempMinMax, int width, int height, double currentScale) throws IllegalArgumentException {
	Point2D.Double t = toScreenPoint(displayAxis, itemx, itemy, tempMinMax, currentScale);
	if (width > 0 && height > 0) {
	    if (t.x < 0) {
		throw new IllegalArgumentException("t.x < 0 : t.x =" + t.x + ", width=" + width + ", height=" + height
			+ ", currentScale=" + currentScale);
	    }
	    if (t.y < 0) {
		throw new IllegalArgumentException("t.y < 0 : t.y =" + t.x + ", width=" + width + ", height=" + height
			+ ", currentScale=" + currentScale);
	    }
	    if (t.x > width) {
		throw new IllegalArgumentException("t.x > width : t.x =" + t.x + ", width=" + width + ", height="
			+ height + ", currentScale=" + currentScale);
	    }
	    if (t.y > height) {
		throw new IllegalArgumentException("t.x > width : t.y =" + t.x + ", width=" + width + ", height="
			+ height + ", currentScale=" + currentScale);
	    }
	}
	g2d.translate(t.x, t.y);
    }

    public Point2D.Double worldToScreenPoint(double worldx, double worldy, boolean autoscale) {
	return toScreenPoint(getDisplayAxis(), worldx, worldy, getMinmax(autoscale), getScale(autoscale));
    }

    private static Point2D.Double toScreenPoint(DisplayAxis displayAxis, double worldx, double worldy,
	    Point2DMinMax minmax, double currentScale) {
	double minX = minmax.min.x;
	double maxX = minmax.max.x;
	double minY = minmax.min.y;
	double maxY = minmax.max.y;
	switch (displayAxis) {
	case POS_X_POS_Y:
	    return new Point2D.Double((worldx - minX) * currentScale + TO_SCREEN_X_OFFSET,
		    (maxY - worldy) * currentScale + TO_SCREEN_Y_OFFSET);

	case POS_Y_NEG_X:
	    return new Point2D.Double((worldy - minY) * currentScale + TO_SCREEN_X_OFFSET,
		    (worldx - minX) * currentScale + TO_SCREEN_Y_OFFSET);

	case NEG_X_NEG_Y:
	    return new Point2D.Double((maxX - worldx) * currentScale + TO_SCREEN_X_OFFSET,
		    (worldy - minY) * currentScale + TO_SCREEN_Y_OFFSET);

	case NEG_Y_POS_X:
	    return new Point2D.Double((maxY - worldy) * currentScale + TO_SCREEN_X_OFFSET,
		    (maxX - worldx) * currentScale + TO_SCREEN_Y_OFFSET);
	}
	throw new IllegalStateException("invalid displayAxis");
    }

    public Point2D.Double screenToWorldPoint(double scrrenx, double screeny, boolean autoscale) {
	return toWorldPoint(getDisplayAxis(), scrrenx, screeny, getMinmax(autoscale), getScale(autoscale));
    }

    private static Point2D.Double toWorldPoint(DisplayAxis displayAxis, double screenx, double screeny,
	    Point2DMinMax minmax, double currentScale) {
	double minX = minmax.min.x;
	double maxX = minmax.max.x;
	double minY = minmax.min.y;
	double maxY = minmax.max.y;
	if (!Double.isFinite(minX)) {
	    throw new IllegalArgumentException("minX=" + minX);
	}
	if (!Double.isFinite(maxX)) {
	    throw new IllegalArgumentException("maxX=" + maxX);
	}
	if (!Double.isFinite(minY)) {
	    throw new IllegalArgumentException("minY=" + minY);
	}
	if (!Double.isFinite(maxY)) {
	    throw new IllegalArgumentException("maxY=" + maxY);
	}
	if (!Double.isFinite(screenx)) {
	    throw new IllegalArgumentException("screenx=" + screenx);
	}
	if (!Double.isFinite(screeny)) {
	    throw new IllegalArgumentException("screeny=" + screeny);
	}
	if (!Double.isFinite(currentScale) || Math.abs(currentScale) < 1E-9) {
	    throw new IllegalArgumentException("currentScale=" + currentScale);
	}
	double xi = (screenx - TO_SCREEN_X_OFFSET) / currentScale;
	double yi = (screeny - TO_SCREEN_Y_OFFSET) / currentScale;
	switch (displayAxis) {
	case POS_X_POS_Y:
	    return new Point2D.Double(minX + xi, maxY - yi);

	case POS_Y_NEG_X:
	    return new Point2D.Double(minX + yi, minY + xi);

	case NEG_X_NEG_Y:
	    return new Point2D.Double(maxX - xi, minY + yi);

	case NEG_Y_POS_X:
	    return new Point2D.Double(maxX - yi, maxY - xi);
	}
	throw new IllegalStateException("invalid displayAxis");
    }

    private static final int TO_SCREEN_X_OFFSET = 15;
    private static final int TO_SCREEN_Y_OFFSET = 20;

    private boolean endEffectorClosed;

    /**
     * Get the value of endEffectorClosed
     *
     * @return the value of endEffectorClosed
     */
    public boolean isEndEffectorClosed() {
	return endEffectorClosed;
    }

    /**
     * Set the value of endEffectorClosed
     *
     * @param endEffectorClosed new value of endEffectorClosed
     */
    void setEndEffectorClosed(boolean endEffectorClosed) {
	this.endEffectorClosed = endEffectorClosed;
    }

    private double currentX = 0;

    /**
     * Get the value of currentX
     *
     * @return the value of currentX
     */
    public double getCurrentX() {
	return currentX;
    }

    private long repaintMinMillis = 100;

    /**
     * Get the value of repaintMinMillis
     *
     * @return the value of repaintMinMillis
     */
    public long getRepaintMinMillis() {
	return repaintMinMillis;
    }

    /**
     * Set the value of repaintMinMillis
     *
     * @param repaintMinMillis new value of repaintMinMillis
     */
    public void setRepaintMinMillis(long repaintMinMillis) {
	this.repaintMinMillis = repaintMinMillis;
    }

    private volatile long lastRepaintPaintTime = 0;

//    private int repaintsDone=0;
//    
//    private int repaintsSkipped=0;
    void checkedRepaint() {
	long timeNow = System.currentTimeMillis();
	long diff = timeNow - lastRepaintPaintTime;
	if (diff > repaintMinMillis) {
	    lastRepaintPaintTime = timeNow;
//            repaintsDone++;
//            System.out.println("repaintsDone = " + repaintsDone+", repaintsSkipped = " + repaintsSkipped);
	    super.repaint();
	} else {
//            repaintsSkipped++;
	}
    }

    /**
     * Set the value of currentX
     *
     * @param currentX new value of currentX
     */
    void setCurrentX(double currentX) {
	if (Math.abs(this.currentX - currentX) > 0.0001) {
	    this.currentX = currentX;
	    this.checkedRepaint();
	}
    }

    private double currentY = 0;

    /**
     * Get the value of currentY
     *
     * @return the value of currentY
     */
    public double getCurrentY() {
	return currentY;
    }

    /**
     * Set the value of currentY
     *
     * @param currentY new value of currentY
     */
    void setCurrentY(double currentY) {
	if (Math.abs(this.currentY - currentY) > 0.0001) {
	    this.currentY = currentY;
	    this.checkedRepaint();
	}
    }

    private boolean showCurrentXY;

    /**
     * Get the value of showCurrentXY
     *
     * @return the value of showCurrentXY
     */
    public boolean isShowCurrentXY() {
	return showCurrentXY;
    }

    /**
     * Set the value of showCurrentXY
     *
     * @param showCurrentXY new value of showCurrentXY
     */
    void setShowCurrentXY(boolean showCurrentXY) {
	if (this.showCurrentXY != showCurrentXY) {
	    this.showCurrentXY = showCurrentXY;
	    this.checkedRepaint();
	}
    }

    private boolean useSeparateNames = true;

    /**
     * Get the value of useSeparateNames
     *
     * @return the value of useSeparateNames
     */
    public boolean isUseSeparateNames() {
	return useSeparateNames;
    }

    /**
     * Set the value of useSeparateNames
     *
     * @param useSeparateNames new value of useSeparateNames
     */
    void setUseSeparateNames(boolean useSeparateNames) {
	if (this.useSeparateNames != useSeparateNames) {
	    this.useSeparateNames = useSeparateNames;
	    this.checkedRepaint();
	}
    }

    private int namesXPos = 100;

    /**
     * Get the value of namesXPos
     *
     * @return the value of namesXPos
     */
    public int getNamesXPos() {
	return namesXPos;
    }

    /**
     * Set the value of namesXPos
     *
     * @param namesXPos new value of namesXPos
     */
    public void setNamesXPos(int namesXPos) {
	this.namesXPos = namesXPos;
    }

//    @MonotonicNonNull
//    private AffineTransform origTransform = null;
    private volatile boolean scale_set;

    @Override
    @UIEffect
    public void paintComponent(Graphics g) {
	try {
	    this.lastRepaintPaintTime = System.currentTimeMillis();
	    super.paintComponent(g);

	    Graphics2D g2d = (Graphics2D) g;

	    List<PhysicalItem> itemsToPaint = getItemsToPaint();
	    PhysicalItem selectedItem = null;
	    if (selectedItemIndex >= 0 && selectedItemIndex < itemsToPaint.size()) {
		selectedItem = itemsToPaint.get(selectedItemIndex);
	    }
	    Dimension dim = this.getSize();
	    int w = dim.width;
	    int h = dim.height;
	    if (w < 10 || h < 10) {
		return;
	    }
	    ViewOptions opts = currentViewOptions();
	    opts.w = w;
	    opts.h = h;
	    if (this.scale == 0) {
		opts.overridingAutoscale = true;
	    } else {
		opts.scale = this.scale;
		opts.scale_set = this.scale_set && !this.autoscale;
	    }
	    opts.paintingComponent = true;
	    if (null != itemsToPaint && !itemsToPaint.isEmpty()) {
		if (this.autoscale || scale == 0 || !Double.isFinite(minmax.min.x) || !Double.isFinite(minmax.min.y)
			|| !Double.isFinite(minmax.max.x) || !Double.isFinite(minmax.max.y)) {
		    paintWithAutoScale(itemsToPaint, selectedItem, g2d, opts);
		} else {
		    paintItems(g2d, itemsToPaint, selectedItem, this.minmax, opts);
		}
	    }
	    if (opts.scale_set && opts.scale > 0) {
		this.scale = opts.scale;
		this.scale_set = !this.autoscale;
	    }
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
	    final String exMessageString = ex.getMessage();
	    if (null != exMessageString) {
		g.drawString(exMessageString, 10, 10);
	    }
	} finally {
	    this.lastRepaintPaintTime = System.currentTimeMillis();
	}
    }

    private static String getItemType(PhysicalItem item) {
	if (item.getType() != null) {
	    return item.getType();
	} else if (item.getName().startsWith("part_")) {
	    return "P";
	} else if (item.getName().startsWith("kit_")) {
	    return "KT";
	} else if (item.getName().startsWith("empty_slot_")) {
	    return "S";
	} else if (item.getName().contains("vessel")) {
	    return "PT";
	} else {
	    return item.getName().substring(0, 1);
	}
    }

    private static final Color[] labelColors = new Color[] {
	    Color.BLACK,
	    Color.MAGENTA.darker(),
	    Color.BLUE.darker(),
	    Color.RED.darker(),
	    Color.GREEN.darker()
    };

    private @MonotonicNonNull AprsSystem aprsSystem;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    public @Nullable AprsSystem getAprsSystem() {
	return aprsSystem;
    }

    /**
     * Set the value of aprsSystemInterface
     *
     * @param aprsSystemInterface new value of aprsSystemInterface
     */
    public void setAprsSystem(AprsSystem aprsSystemInterface) {
	this.aprsSystem = aprsSystemInterface;
	setSlotOffsetProvider(aprsSystemInterface);
    }

    private boolean showAddedToolsAndToolHolders = false;

    /**
     * Get the value of showAddedToolsAndToolHolders
     *
     * @return the value of showAddedToolsAndToolHolders
     */
    public boolean isShowAddedToolsAndToolHolders() {
	return showAddedToolsAndToolHolders;
    }

    /**
     * Set the value of showAddedToolsAndToolHolders
     *
     * @param showAddedToolsAndToolHolders new value of showAddedToolsAndToolHolders
     */
    void setShowAddedToolsAndToolHolders(boolean showAddedToolsAndToolHolders) {
	this.showAddedToolsAndToolHolders = showAddedToolsAndToolHolders;
    }

    private boolean showAddedSlotPositions;

    /**
     * Get the value of showAddedSlotPositions
     *
     * @return the value of showAddedSlotPositions
     */
    boolean isShowAddedSlotPositions() {
	return showAddedSlotPositions;
    }

    /**
     * Set the value of showAddedSlotPositions
     *
     * @param showAddedSlotPositions new value of showAddedSlotPositions
     */
    void setShowAddedSlotPositions(boolean showAddedSlotPositions) {
	if (this.showAddedSlotPositions != showAddedSlotPositions) {
	    this.showAddedSlotPositions = showAddedSlotPositions;
	    checkedRepaint();
	}
	updateAddedExtras();
    }

    private List<PhysicalItem> getAvailableToolHolders() {
	if (null == aprsSystem) {
	    throw new IllegalStateException("null == aprsSysInterface");
	}
	return aprsSystem.getAvailableToolHolders();
    }

    private List<PhysicalItem> getToolsInHolders() {
	if (null == aprsSystem) {
	    throw new IllegalStateException("null == aprsSysInterface");
	}
	return aprsSystem.getToolsInHolders();
    }

    private List<PhysicalItem> getToolsAndHolders() {
	List<PhysicalItem> l = new ArrayList<>(getAvailableToolHolders());
	l.addAll(getToolsInHolders());
	return l;
    }

    private void updateAddedExtras() {
	if (showAddedSlotPositions || showAddedToolsAndToolHolders) {
	    if (showAddedToolsAndToolHolders && null != this.aprsSystem) {
		this.addedTools = getToolsAndHolders();

	    } else {
		if (null == this.addedTools || !this.addedTools.isEmpty()) {
		    this.addedTools = Collections.emptyList();
		}
	    }
	    if (null != items) {
		this.itemsWithAddedExtras = new ArrayList<>();
		this.itemsWithAddedExtras.addAll(items);

		if (showAddedToolsAndToolHolders) {
		    this.itemsWithAddedExtras.addAll(this.addedTools);
		}
		if (showAddedSlotPositions) {
		    this.addedSlots = computeAbsSlotPositions(items);
		    this.itemsWithAddedExtras.addAll(this.addedSlots);
		} else {
		    if (null == addedSlots || !addedSlots.isEmpty()) {
			this.addedSlots = Collections.emptyList();
		    }
		    if (null == addedOutputSlots || !addedOutputSlots.isEmpty()) {
			this.addedOutputSlots = Collections.emptyList();
		    }
		}
	    }
	    if (null != outputItems) {

		this.outputItemsWithAddedExtras = new ArrayList<>();
		this.outputItemsWithAddedExtras.addAll(outputItems);
		if (showAddedToolsAndToolHolders) {
		    this.outputItemsWithAddedExtras.addAll(this.addedTools);
		}
		if (showAddedSlotPositions) {
		    this.addedOutputSlots = computeAbsSlotPositions(outputItems);
		    this.outputItemsWithAddedExtras.addAll(this.addedOutputSlots);
		} else {
		    if (null == addedOutputSlots || !addedOutputSlots.isEmpty()) {
			this.addedOutputSlots = Collections.emptyList();
		    }
		}
	    }
	} else {
	    clearAddedSlotInfo();
	}
    }

    List<Slot> computeAbsSlotPositions(List<PhysicalItem> l) {
	List<Slot> absSlotList = new ArrayList<>();
	for (PhysicalItem item : l) {
	    if (null != slotOffsetProvider && ("PT".equals(item.getType()) || "KT".equals(item.getType()))) {
		absSlotList.addAll(computeSlotPositions(item));
	    }
	}
	return absSlotList;
    }

    private @MonotonicNonNull SlotOffsetProvider slotOffsetProvider = null;

    public @Nullable SlotOffsetProvider getSlotOffsetProvider() {
	return slotOffsetProvider;
    }

    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
	this.slotOffsetProvider = slotOffsetProvider;
    }

    private List<Slot> computeSlotPositions(PhysicalItem item) {
	if (null == slotOffsetProvider) {
	    throw new IllegalStateException("slotOffsetProvider is null");
	}
	List<Slot> slotOffsets = slotOffsetProvider.getSlotOffsets(item.getName(), false);
	List<Slot> slotList = new ArrayList<>();
	if (null != slotOffsets) {
	    for (Slot relSlot : slotOffsets) {
		Slot absSlot = slotOffsetProvider.absSlotFromTrayAndOffset(item, relSlot, rotationOffset);
		if (null != absSlot) {
		    String prpName = relSlot.getPrpName();
		    String slotDisplayName = "slot_" + prpName;
		    if (slotDisplayName.startsWith("slot_slot_")) {
			slotDisplayName = slotDisplayName.substring(5);
		    }
		    Slot copySlot = new Slot(slotDisplayName,
			    item.getRotation(),
			    absSlot.x,
			    absSlot.y,
			    item.getScore(),
			    "S");
		    copySlot.setDiameter(relSlot.getDiameter());
		    slotList.add(copySlot);
		}
	    }
	}
	return slotList;
    }

    private @Nullable Image capturedPartImage;

    /**
     * Get the value of capturedPartImage
     *
     * @return the value of capturedPartImage
     */
    public @Nullable Image getCapturedPartImage() {
	return capturedPartImage;
    }

    /**
     * Set the value of capturedPartImage
     *
     * @param capturedPartImage new value of capturedPartImage
     */
    public void setCapturedPartImage(@Nullable Image capturedPartImage) {
	this.capturedPartImage = capturedPartImage;
    }

    private Point2D.@Nullable Double capturedPartPoint;

    /**
     * Get the value of capturedPartPoint
     *
     * @return the value of capturedPartPoint
     */
    public Point2D.@Nullable Double getCapturedPartPoint() {
	return capturedPartPoint;
    }

    /**
     * Set the value of capturedPartPoint
     *
     * @param capturedPartPoint new value of capturedPartPoint
     */
    void setCapturedPartPoint(Point2D.@Nullable Double capturedPartPoint) {
	this.capturedPartPoint = capturedPartPoint;
    }

    private final static Map<String, PartImageInfo> partImageMap = PartImageMapMaker.createPartImageMap();

    private boolean viewLimitsLine = true;

    /**
     * Get the value of viewLimitsLine
     *
     * @return the value of viewLimitsLine
     */
    public boolean isViewLimitsLine() {
	return viewLimitsLine;
    }

    /**
     * Set the value of viewLimitsLine
     *
     * @param viewLimitsLine new value of viewLimitsLine
     */
    public void setViewLimitsLine(boolean viewLimitsLine) {
	this.viewLimitsLine = viewLimitsLine;
    }

    private @Nullable Point mouseDownPoint = null;

    public @Nullable Point getMouseDownPoint() {
	return mouseDownPoint;
    }

    public void setMouseDownPoint(Point mouseDownPoint) {
	this.mouseDownPoint = mouseDownPoint;
    }

    private @Nullable Point mousePoint = null;

    /**
     * Get the value of mousePoint
     *
     * @return the value of mousePoint
     */
    public @Nullable Point getMousePoint() {
	return mousePoint;
    }

    /**
     * Set the value of mousePoint
     *
     * @param mousePoint new value of mousePoint
     */
    public void setMousePoint(@Nullable Point mousePoint) {
	if (this.mousePoint != mousePoint) {
	    this.mousePoint = mousePoint;
	    this.checkedRepaint();
	}
    }

    private boolean mouseInside;

    /**
     * Get the value of mouseInside
     *
     * @return the value of mouseInside
     */
    public boolean isMouseInside() {
	return mouseInside;
    }

    /**
     * Set the value of mouseInside
     *
     * @param mouseInside new value of mouseInside
     */
    public void setMouseInside(boolean mouseInside) {
	this.mouseInside = mouseInside;
    }

    public void clearSizes() {
	if (null != items) {
	    for (int i = 0; i < items.size(); i++) {
		PhysicalItem item = items.get(i);
		item.setDisplayRect(null);
		item.setDisplayTransform(null);
	    }
	}
	autoScaledMinMax = null;
	autoScaledScale = -1;
	scale_set = false;
	scale = 0;
	this.repaint();
    }

    private volatile boolean mouseDown = false;

    public Point2D.Double getMouseWorldMousePoint() {
	if (null == mousePoint) {
	    return new Point2D.Double(java.lang.Double.NaN, java.lang.Double.NaN);
	}
	return screenToWorldPoint(mousePoint.x, mousePoint.y, isAutoscale());
    }

    public boolean isMouseDown() {
	return mouseDown;
    }

    public void setMouseDown(boolean mouseDown) {
	this.mouseDown = mouseDown;
    }

    @SuppressWarnings("guieffect")
    private static void paintItems(Graphics2D g2d,
	    Collection<? extends PhysicalItem> itemsToPaint,
	    @Nullable PhysicalItem selectedItem,
	    Point2DMinMax minmaxParam,
	    ViewOptions opts) {

	try {
	    if (null == itemsToPaint || itemsToPaint.isEmpty()) {
		return;
	    }
	    final DisplayAxis optsDisplayAxisLocal = opts.displayAxis;
	    if (null == optsDisplayAxisLocal) {
		throw new NullPointerException("opts.displayAxis");
	    }
	    Collection<? extends PhysicalItem> origItemsToPaint = itemsToPaint;
	    final AprsSystem aprsSystemFinal = opts.aprsSystem;
	    if (null != aprsSystemFinal) {
		if (!opts.showOverlapping) {
		    itemsToPaint = aprsSystemFinal.filterOverLapping(itemsToPaint);
		} else if (opts.showOnlyOverlapping) {
		    itemsToPaint = aprsSystemFinal.filterNonOverLapping(itemsToPaint);
		}
	    }
	    AffineTransform origTransform = g2d.getTransform();
	    final double currentRotationOffset;

	    if (opts.useOverridingRotationOffset) {
		currentRotationOffset = opts.overridingRotationOffset;
	    } else {
		currentRotationOffset = opts.defaultRotationOffset;
	    }
	    int maxNameLength = itemsToPaint.stream()
		    .mapToInt((PhysicalItem item) -> item.getName().length())
		    .max().orElse(1);

	    if (!Double.isFinite(minmaxParam.max.x) || !Double.isFinite(minmaxParam.min.x)
		    || !Double.isFinite(minmaxParam.min.y) || !Double.isFinite(minmaxParam.max.y)) {
		throw new IllegalArgumentException("Limits must be finite: (" + minmaxParam.min.x + ","
			+ minmaxParam.min.y + "," + minmaxParam.max.x + "," + minmaxParam.max.y + ")");
	    }

	    final Dimension dim = new Dimension(opts.w, opts.h);
	    boolean useSeparateNamesThisTime = opts.useSeparateNames;
	    if (opts.disableLabels) {
		useSeparateNamesThisTime = false;
	    }
	    double new_scale = computeNewScale(minmaxParam, opts);

	    if (!opts.disableLimitsLine) {
		if (opts.mouseInside && null != opts.mousePoint && null != opts.worldMousePoint) {
		    boolean localAutoscale = opts.overridingAutoscale;
		    Point mousePoint = opts.mousePoint;
		    Point2D.Double worldMousePoint = opts.worldMousePoint;
		    if (null != aprsSystemFinal) {
			PmCartesian robotCart = aprsSystemFinal.convertVisionToRobotPmCartesian(
				new PmCartesian(worldMousePoint.x, worldMousePoint.y, 0));
			g2d.drawString(
				String.format(
					"vis(%.2f,%.2f):robot(%.2f,%.2f), scale=%.2f",
					worldMousePoint.x, worldMousePoint.y, robotCart.x, robotCart.y, new_scale),
				10, dim.height - 10);
		    } else {
			g2d.drawString(
				String.format(
					"vis(%.2f,%.2f), scale=%.2f",
					worldMousePoint.x, worldMousePoint.y, new_scale),
				10, dim.height - 10);
		    }
		} else {
		    if (null != aprsSystemFinal) {
			PmCartesian robotMinCart = aprsSystemFinal.convertVisionToRobotPmCartesian(
				new PmCartesian(minmaxParam.min.x, minmaxParam.min.y, 0));
			PmCartesian robotMaxCart = aprsSystemFinal.convertVisionToRobotPmCartesian(
				new PmCartesian(minmaxParam.min.x, minmaxParam.min.y, 0));
			g2d.drawString(
				String.format(
					"MinX,MinY = vis(%.2f,%.2f):robot((%.2f,%.2f), MaxX,MaxY= vis(%.2f,%.2f):robot(%.2f,%.2f), scale=%.2f",
					minmaxParam.min.x, minmaxParam.min.y, robotMinCart.x, robotMinCart.y,
					minmaxParam.max.x, minmaxParam.max.y, robotMaxCart.x, robotMaxCart.y,
					new_scale),
				10, dim.height - 10);
		    } else {
			g2d.drawString(
				String.format(
					"MinX,MinY = vis(%.2f,%.2f), MaxX,MaxY= vis(%.2f,%.2f), scale=%.2f",
					minmaxParam.min.x, minmaxParam.min.y, minmaxParam.max.x, minmaxParam.max.y,
					new_scale),
				10, dim.height - 10);
		    }
		}
	    }
	    Collection<? extends PhysicalItem> displayItems = itemsToPaint;
	    if (useSeparateNamesThisTime) {
		List<? extends PhysicalItem> displayItemsList = new ArrayList<>(itemsToPaint);
		displayItems = displayItemsList;
		if (null == optsDisplayAxisLocal) {
		    throw new NullPointerException("opts.displayAxis");
		}
		switch (optsDisplayAxisLocal) {
		case POS_X_POS_Y:
		    displayItemsList.sort(Comparator.comparing((PhysicalItem item) -> -item.y));
		    break;
		case NEG_X_NEG_Y:
		    displayItemsList.sort(Comparator.comparing((PhysicalItem item) -> item.y));
		    break;

		case POS_Y_NEG_X:
		    displayItemsList.sort(Comparator.comparing((PhysicalItem item) -> item.x));
		    break;
		case NEG_Y_POS_X:
		    displayItemsList.sort(Comparator.comparing((PhysicalItem item) -> -item.x));
		    break;
		}
	    }

	    Font origFont = g2d.getFont();

	    float fsize = opts.w / (2.2f * maxNameLength);
	    if (fsize > 24.f) {
		fsize = 24.f;
	    }
	    if (fsize < 8.f) {
		fsize = 8.f;
	    }
	    if (origFont.getSize() != fsize) {
		Font newFont = origFont.deriveFont(fsize);
		g2d.setFont(newFont);
	    }
	    float newFontSize = g2d.getFont().getSize2D();
	    int i = 0;

	    if (opts.viewRotationsAndImages) {
		for (PhysicalItem item : displayItems) {
		    if (null == item) {
			continue;
		    }
		    if ("P".equals(item.getType())) {
			continue;
		    }
		    paintPartImage(opts, g2d, minmaxParam, item, currentRotationOffset, new_scale);
		    g2d.setTransform(origTransform);
		}
		for (PhysicalItem item : displayItems) {
		    if (null == item) {
			continue;
		    }
		    if (!("P".equals(item.getType()))) {
			continue;
		    }
		    paintPartImage(opts, g2d, minmaxParam, item, currentRotationOffset, new_scale);
		    g2d.setTransform(origTransform);
		}
	    }
	    for (PhysicalItem item : displayItems) {
		if (null == item) {
		    continue;
		}
		++i;
		if (item.getName() == null || item.getName().length() < 1) {
		    continue;
		}
		if (Double.isInfinite(item.x) || Double.isNaN(item.x)) {
		    continue;
		}
		if (Double.isInfinite(item.y) || Double.isNaN(item.y)) {
		    continue;
		}
		if (Double.isInfinite(item.getRotation()) || Double.isNaN(item.getRotation())) {
		    continue;
		}
		if (useSeparateNamesThisTime) {
		    final int itemsToPaintSize = itemsToPaint.size();
		    final boolean itemIsSelected = item == selectedItem;
		    final double itemOffsetRatio = ((double) (i + 1)) / (itemsToPaintSize + 2);
		    if (null == optsDisplayAxisLocal) {
			throw new NullPointerException("opts.displayAxis");
		    }
		    paintItemLabel(item, i, g2d, optsDisplayAxisLocal, minmaxParam, itemOffsetRatio, dim, new_scale,
			    itemIsSelected, origTransform);
		}
	    }
	    g2d.setFont(origFont);

	    for (PhysicalItem item : displayItems) {
		if (null == item) {
		    continue;
		}

		if (null != partImageMap && !partImageMap.isEmpty()) {
		    if (opts.viewRotationsAndImages && null != getPartImageInfo(item)) {
			continue;
		    }
		}
		boolean imageShown = opts.viewRotationsAndImages && null != getPartImageInfo(item);
		translateThenRotateItem(opts, g2d, minmaxParam, item, currentRotationOffset, new_scale, false);
		item.setDisplayTransform(g2d.getTransform());
		item.setOrigTransform(origTransform);
		if (null != opts && opts.paintingComponent) {
		    try {
			AffineTransform itemRelTransform = origTransform.createInverse();
			AffineTransform itemDisplayTransform = item.getDisplayTransform();
			if (null != itemDisplayTransform) {
			    itemRelTransform.concatenate(itemDisplayTransform);
			    item.setRelTransform(itemRelTransform);
			}
		    } catch (NoninvertibleTransformException ex) {
			Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
		    }
		}
		int namelen = useSeparateNamesThisTime ? 1 : item.getName().length();
		Rectangle2D.Double itemDisplayRect = new Rectangle2D.Double(
			-5, -12, // x,y
			10 + 10 * namelen, TO_SCREEN_Y_OFFSET);
		if (opts != null && opts.paintingComponent) {
		    item.setDisplayRect(itemDisplayRect);
		}
		if (null != opts.backgroundColor) {
		    g2d.setColor(opts.backgroundColor);
		    g2d.fill(itemDisplayRect);
		}
		g2d.setTransform(origTransform);
	    }
	    i = 0;
	    for (PhysicalItem item : displayItems) {
		++i;
		if (null == item) {
		    continue;
		}
		boolean imageShown = opts.viewRotationsAndImages && null != getPartImageInfo(item);
		;
		translateThenRotateItem(opts, g2d, minmaxParam, item, currentRotationOffset, new_scale, false);
		g2d.setColor(Color.BLACK);
		if (!imageShown) {
		    if (!useSeparateNamesThisTime) {
			if (item.getName() != null) {
			    g2d.drawString(item.getName(), 0, 0);
			}
		    } else if (item.getType() != null) {
			g2d.drawString(item.getType(), 0, 0);
		    } else if (item.getName().startsWith("part_")) {
			g2d.drawString("P", 0, 0);
		    } else if (item.getName().startsWith("kit_")) {
			g2d.drawString("KT", 0, 0);
		    } else if (item.getName().startsWith("empty_slot_")) {
			g2d.drawString("S", 0, 0);
		    } else if (item.getName().contains("vessel")) {
			g2d.drawString("PT", 0, 0);
		    } else {
			g2d.drawString(item.getName().substring(0, 1), 0, 0);
		    }
		}

		if (null != opts && opts.paintingComponent) {
		    AffineTransform itemDisplayTransform = g2d.getTransform();
		    item.setDisplayTransform(itemDisplayTransform);
		    item.setOrigTransform(origTransform);
		    try {
			AffineTransform itemRelTransform = origTransform.createInverse();
			itemRelTransform.concatenate(itemDisplayTransform);
			item.setRelTransform(itemRelTransform);
		    } catch (NoninvertibleTransformException ex) {
			Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
		    }
		}

		if (!imageShown) {
		    int namelen = useSeparateNamesThisTime ? 1 : item.getName().length();
		    Rectangle2D.Double itemDisplayRect = new Rectangle2D.Double(
			    -5, -12, // x,y
			    10 + 10 * namelen, TO_SCREEN_Y_OFFSET);
		    if (opts != null && opts.paintingComponent) {
			item.setDisplayRect(itemDisplayRect);
		    }
		    g2d.setColor(item.getLabelColor());
		    g2d.draw(itemDisplayRect);
		}

		try {
		    final SlotOffsetProvider optsSlotOffsetProviderLocal = opts.slotOffsetProvider;
		    if (null != optsSlotOffsetProviderLocal
			    && ("PT".equals(item.getType()) || "KT".equals(item.getType()))) {
			List<Slot> offsets = optsSlotOffsetProviderLocal.getSlotOffsets(item.getName(), true);
			if (null != offsets) {
			    for (PhysicalItem offset : offsets) {
				double mag = offset.mag();
				if (item.getMaxSlotDist() < mag) {
				    item.setMaxSlotDist(mag);
				}
			    }
			}
		    }
		    if (opts.viewDetails) {
			if (item.getMaxSlotDist() > 0) {
			    final Arc2D.Double slotDistArc = new Arc2D.Double(
				    -item.getMaxSlotDist() * new_scale * opts.slotMaxDistExpansion, // x
				    -item.getMaxSlotDist() * new_scale * opts.slotMaxDistExpansion, // y
				    item.getMaxSlotDist() * 2.0 * new_scale * opts.slotMaxDistExpansion, // w
				    item.getMaxSlotDist() * 2.0 * new_scale * opts.slotMaxDistExpansion, // h
				    0.0, // start
				    360.0, // extent
				    Arc2D.OPEN);
			    g2d.draw(slotDistArc);
			}
			if (item instanceof Slot) {
			    Slot slot = (Slot) item;
			    double diameter = slot.getDiameter();
			    if (diameter > 0) {
				final Arc2D.Double slotDiameterArc = new Arc2D.Double(
					-diameter * 0.5 * new_scale, // x
					-diameter * 0.5 * new_scale, // y
					diameter * new_scale, // w
					diameter * new_scale, // h
					0.0, // start
					360.0, // extend
					Arc2D.OPEN);
				g2d.draw(slotDiameterArc);
			    }
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		g2d.setTransform(origTransform);
	    }
	    g2d.setColor(Color.BLACK);
	    if (null != selectedItem) {
		boolean imageShown = opts.viewRotationsAndImages && null != getPartImageInfo(selectedItem);
		if (selectedItem.getName() == null || selectedItem.getName().length() < 1) {
		    return;
		}
		if (Double.isInfinite(selectedItem.x) || Double.isNaN(selectedItem.x)) {
		    return;
		}
		if (Double.isInfinite(selectedItem.y) || Double.isNaN(selectedItem.y)) {
		    return;
		}
		if (Double.isInfinite(selectedItem.getRotation()) || Double.isNaN(selectedItem.getRotation())) {
		    return;
		}
		if (!imageShown || opts.viewDetails) {
		    translateThenRotateItem(opts, g2d, minmaxParam, selectedItem, currentRotationOffset, new_scale,
			    false);
		    g2d.setColor(Color.WHITE);
		    String typeString = getItemType(selectedItem);
		    Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10
			    + 10 * (useSeparateNamesThisTime ? typeString.length() : selectedItem.getName().length()),
			    TO_SCREEN_Y_OFFSET);
		    g2d.fill(rect);
		    g2d.setColor(Color.BLACK);
		    g2d.draw(rect);
		    if (!useSeparateNamesThisTime) {
			g2d.drawString(selectedItem.getName(), 0, 0);
		    } else {
			g2d.drawString(selectedItem.getType(), 0, 0);
		    }
		}
		g2d.setTransform(origTransform);
	    }
	    if (opts.showCurrentXY && !opts.disableShowCurrent) {
		Color origColor = g2d.getColor();
		Point2D.Double fromPoint = opts.capturedPartPoint;
		if (null != fromPoint) {
		    g2d.setColor(Color.MAGENTA);
		    Point2D.Double captureScreenPt = toScreenPoint(optsDisplayAxisLocal, fromPoint.x, fromPoint.y,
			    minmaxParam, new_scale);
		    Point2D.Double currentScreenPt = toScreenPoint(optsDisplayAxisLocal, opts.currentX, opts.currentY,
			    minmaxParam, new_scale);
		    g2d.draw(new Line2D.Double(currentScreenPt, captureScreenPt));
		}
		translate(optsDisplayAxisLocal, g2d, opts.currentX, opts.currentY, minmaxParam, -1, -1, new_scale);
		if (opts.endEffectorClosed) {
		    g2d.setColor(Color.black);
		    g2d.fillArc(-5, -5, 10, 10, 0, 360);
		} else {
		    g2d.setColor(Color.white);
		    g2d.drawArc(-10, -10, 10, 10, 0, 360);
		}
		g2d.setColor(Color.red);
		g2d.drawLine(-20, 0, TO_SCREEN_X_OFFSET, 0);
		g2d.drawLine(0, -20, 0, TO_SCREEN_Y_OFFSET);
		g2d.setColor(origColor);
		g2d.setTransform(origTransform);
	    }
	    if (null != opts) {
		if (!opts.disableSensorLimitsRect) {
		    drawSenseLimitsRectangle(opts, g2d, minmaxParam, new_scale);
		}
	    }
	    if (null != aprsSystemFinal && (null == opts || !opts.disableRobotsReachLimitsRect)) {
		drawRobotReachLimitsRectangle(opts, g2d, minmaxParam, new_scale);
	    }
	} catch (Exception exception) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", exception);
	    g2d.drawString(exception.toString(), TO_SCREEN_X_OFFSET, TO_SCREEN_Y_OFFSET);
	}
    }

    @UIEffect
    private static void paintItemLabel(PhysicalItem item, int i, Graphics2D g2d, DisplayAxis displayAxis,
	    Point2DMinMax minmaxParam, double itemOffsetRatio, Dimension dim, double new_scale,
	    final boolean itemIsSelected, AffineTransform origTransform) {

	item.setLabelColor(labelColors[i % labelColors.length]);
	g2d.setColor(item.getLabelColor());
	Point2D.Double namePoint2D = new Point2D.Double(dim.width / 2.0, itemOffsetRatio * dim.height);

	try {
	    g2d.translate(namePoint2D.x, namePoint2D.y);
	} catch (Exception ex) {
	    Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
	    throw new RuntimeException(
		    "i=" + i + ",namePoint2D.x=" + namePoint2D.x + ",namePoint2D.y=" + namePoint2D.y + ",dim.width="
			    + dim.width + ",dim.height=" + dim.height + ",itemOffsetRatio=" + itemOffsetRatio
			    + ",new_scale=" + new_scale + ",minmaxParam=" + minmaxParam + ",displayAxis=" + displayAxis,
		    ex);
	}
	if (itemIsSelected) {
	    g2d.setColor(Color.WHITE);
	    Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.getName().length(),
		    TO_SCREEN_Y_OFFSET);
	    g2d.fill(rect);
	}
	g2d.setColor(item.getLabelColor());
	g2d.drawString(item.getName(), 0, 0);
	g2d.setTransform(origTransform);
	g2d.draw(
		new Line2D.Double(
			namePoint2D,
			toScreenPoint(displayAxis, item.x, item.y, minmaxParam, new_scale)));
    }

    @UIEffect
    private static void drawRobotReachLimitsRectangle(ViewOptions opts, Graphics2D g2d, Point2DMinMax tempMinMax,
	    double new_scale) {

	AprsSystem optsAprsSystem = opts.aprsSystem;
	if (null != optsAprsSystem) {
	    DisplayAxis displayAxis = opts.displayAxis;
	    if (null == displayAxis) {
		return;
	    }
	    for (PmCartesianMinMaxLimit minMax : optsAprsSystem.getLimits()) {

		PmCartesian robotReachMax = minMax.getMax();
		PmCartesian robotReachMin = minMax.getMin();
		if (null != robotReachMax
			&& null != robotReachMin
			&& Double.isFinite(robotReachMax.x)
			&& Double.isFinite(robotReachMax.y)
			&& Double.isFinite(robotReachMin.x)
			&& Double.isFinite(robotReachMin.y)) {
		    g2d.setColor(Color.red);

		    Point2D.Double minSensePoint = toScreenPoint(displayAxis, robotReachMin.x, robotReachMin.y,
			    tempMinMax, new_scale);
		    Point2D.Double maxSensePoint = toScreenPoint(displayAxis, robotReachMax.x, robotReachMax.y,
			    tempMinMax, new_scale);
		    g2d.draw(new Rectangle.Double(
			    Math.min(minSensePoint.x, maxSensePoint.x),
			    Math.min(minSensePoint.y, maxSensePoint.y),
			    Math.abs(minSensePoint.x - maxSensePoint.x),
			    Math.abs(minSensePoint.y - maxSensePoint.y)));
		}
	    }
	}
    }

    @UIEffect
    private static void drawSenseLimitsRectangle(ViewOptions opts, Graphics2D g2d, Point2DMinMax tempMinMax,
	    double new_scale) {
	final DisplayAxis optDisplayAxis = opts.displayAxis;

	if (Double.isFinite(opts.senseMaxX)
		&& Double.isFinite(opts.senseMinX)
		&& Double.isFinite(opts.senseMinY)
		&& Double.isFinite(opts.senseMaxY)
		&& null != optDisplayAxis) {
	    g2d.setColor(Color.black);

	    Point2D.Double minSensePoint = toScreenPoint(optDisplayAxis, opts.senseMinX, opts.senseMinY, tempMinMax,
		    new_scale);
	    Point2D.Double maxSensePoint = toScreenPoint(optDisplayAxis, opts.senseMaxX, opts.senseMaxY, tempMinMax,
		    new_scale);
	    g2d.draw(new Rectangle.Double(
		    Math.min(minSensePoint.x, maxSensePoint.x),
		    Math.min(minSensePoint.y, maxSensePoint.y),
		    Math.abs(minSensePoint.x - maxSensePoint.x),
		    Math.abs(minSensePoint.y - maxSensePoint.y)));
	}
    }

    @SuppressWarnings("guieffect")
    static private double computeNewScale(
	    Point2DMinMax minmax,
	    ViewOptions opts) {
	boolean useSeparateNamesThisTime = opts.useSeparateNames;
	if (opts.disableLabels) {
	    useSeparateNamesThisTime = false;
	}
	final DisplayAxis optsDisplayAxisLocal = opts.displayAxis;
	if (null == optsDisplayAxisLocal) {
	    throw new NullPointerException("opts.displayAxis");
	}
	return computeNewScale(optsDisplayAxisLocal, useSeparateNamesThisTime, minmax, opts);
    }

    private static double computeNewScale(
	    DisplayAxis displayAxis,
	    boolean useSeparateNames,
	    Point2DMinMax minmax,
	    ViewOptions opts) {
	Point2D.Double min = minmax.min;
	Point2D.Double max = minmax.max;
	if (null != opts && opts.scale_set && opts.scale > 0) {
	    return opts.scale;
	}
	int w = opts.w;
	int h = opts.h;
	boolean useSeparateNamesThisTime = useSeparateNames;
	int borderWidth = 30;
	int borderHeight = 60;
	if (null != opts) {
	    if (opts.disableLabels) {
		useSeparateNamesThisTime = false;
		borderWidth = 0;
		borderHeight = 0;
	    }
	}
	double new_scale;
	double scale_x = 1;
	double scale_y = 1;
	int displayWidth = (useSeparateNamesThisTime ? (w / 2) : w);
	double diffx = (max.x - min.x);
	double diffy = (max.y - min.y);
	int diffwidth = (displayWidth - borderWidth - TO_SCREEN_X_OFFSET);
	int diffheight = (h - borderHeight - TO_SCREEN_Y_OFFSET);
	switch (displayAxis) {
	case POS_X_POS_Y:
	case NEG_X_NEG_Y:
	    scale_x = diffwidth / diffx;
	    scale_y = diffheight / diffy;
	    break;
	case POS_Y_NEG_X:
	case NEG_Y_POS_X:
	    scale_x = diffwidth / diffy;
	    scale_y = diffheight / diffx;
	    break;
	}
	if (!Double.isFinite(scale_x) || scale_x <= 0) {
	    throw new RuntimeException("scale_x = " + scale_x + ",minmax=" + minmax + ",w=" + w + ",h=" + h
		    + ",displayAxis=" + displayAxis);
	}
	if (!Double.isFinite(scale_y) || scale_y <= 0) {
	    throw new RuntimeException("scale_y = " + scale_y + ",minmax=" + minmax + ",w=" + w + ",h=" + h
		    + ",displayAxis=" + displayAxis);
	}
	new_scale = Math.min(scale_x, scale_y);

	if (null != opts && new_scale > 0) {
	    opts.scale_set = true;
	    opts.scale = new_scale;
	    opts.minmax = new Point2DMinMax();
	    opts.minmax.min.x = min.x;
	    opts.minmax.min.y = min.y;
	    opts.minmax.max.x = max.x;
	    opts.minmax.max.y = max.y;
	}
	return new_scale;
    }

//    private boolean checkImageShown(PhysicalItem item) {
//        return viewRotationsAndImages
//                && null != partImageMap
//                && !partImageMap.isEmpty()
//                && null != getPartImageInfo(item);
//    }

    @SuppressWarnings("guieffect")
    private static void paintPartImage(ViewOptions opts, Graphics2D g2d, Point2DMinMax tempMinMax, PhysicalItem item,
	    double rotationOffsetParam, double currentScale) {
	if (!opts.viewRotationsAndImages) {
	    return;
	}
	if (null == partImageMap || partImageMap.isEmpty()) {
	    return;
	}
	PartImageInfo info = getPartImageInfo(item);
	if (null != info) {
	    Image img = info.getScaledImage(currentScale);
	    int img_w = info.getScaledImageWidth();
	    int img_h = info.getScaledImageHeight();
	    double infoXOffset = info.xoffset;
	    double infoYOffset = info.yoffset;
	    final double xo = infoXOffset;
	    final double yo = infoYOffset;
	    if (info.ignoreRotations) {
		double cs = Math.cos(opts.alternativeRotation);
		double sn = Math.sin(opts.alternativeRotation);
		infoXOffset = xo * cs + yo * sn;
		infoYOffset = -xo * sn + yo * cs;
	    }

	    translateThenRotate(opts, g2d, item.x - infoXOffset, item.y - infoYOffset, tempMinMax, currentScale,
		    info.ignoreRotations, -rotationOffsetParam - item.getRotation());
//            translateThenRotateItem(g2d, minX, minY, maxX, maxY, item, rotationOffsetParam, currentScale, info.ignoreRotations);
	    g2d.translate(-(img_w / 2.0), -(img_h / 2.0));
	    g2dDrawImageNoTransformNoObserver(g2d, img);
	    if (opts.viewDetails) {
		g2d.translate(-1, -1);
		g2d.setColor(item.getLabelColor());
		g2d.draw(new Rectangle2D.Double(0, 0, img_w + 2, img_h + 2));
	    }
	    g2d.setColor(Color.BLACK);
	}
    }

    @SuppressWarnings({ "nullness", "guieffect" })
    private static void g2dDrawImageNoTransformNoObserver(Graphics2D g2d, Image img) {
	g2d.drawImage(img, (AffineTransform) null, (ImageObserver) null);
    }

    @SuppressWarnings("guieffect")
    static private @Nullable PartImageInfo getPartImageInfo(PhysicalItem item) {
	String name = item.getName();
	PartImageInfo info = partImageMap.get(name);
	if (null == info) {
	    int in_pt_index = name.indexOf("_in_pt");
	    if (in_pt_index > 0) {
		name = name.substring(0, in_pt_index);
		info = partImageMap.get(name);
	    }
	}
	if (null == info) {
	    int in_kt_index = name.indexOf("_in_kt");
	    if (in_kt_index > 0) {
		name = name.substring(0, in_kt_index);
		info = partImageMap.get(name);
	    }
	}
	if (null == info) {
	    if (name.startsWith("sku_part_")) {
		info = partImageMap.get(name.substring("sku_part_".length()));
	    } else {
		info = partImageMap.get("sku_part_" + name);
	    }
	}
	if (null == info) {
	    if (name.startsWith("sku_")) {
		info = partImageMap.get(name.substring("sku_".length()));
	    } else {
		info = partImageMap.get("sku_" + name);
	    }
	}

	if (null != info) {
	    int w = info.getScaledImageWidth();
	    int h = info.getScaledImageHeight();
	    if (w > 0 && h > 0) {
		// noinspection IntegerDivisionInFloatingPointContext
		Rectangle2D.Double itemDisplayRect = new Rectangle2D.Double(
			-w / 2, -h / 2, // x,y
			w, h// w,h
		);
		item.setDisplayRect(itemDisplayRect);
	    }
	}
	return info;
    }

    private static void translateThenRotateItem(ViewOptions opts, Graphics2D g2d, Point2DMinMax minMaxParam,
	    PhysicalItem item, double rotationOffsetParam, double scaleParam, boolean ignoreRotation) {
	double itemx = item.x;
	double itemy = item.y;
	double rot = -rotationOffsetParam - item.getRotation();
	translateThenRotate(opts, g2d, itemx, itemy, minMaxParam, scaleParam, ignoreRotation, rot);
    }

    private double alternativeRotation = 0.0;

    /**
     * Get the value of alternativeRotation
     *
     * @return the value of alternativeRotation
     */
    public double getAlternativeRotation() {
	return alternativeRotation;
    }

    /**
     * Set the value of alternativeRotation
     *
     * @param alternativeRotation new value of alternativeRotation
     */
    public void setAlternativeRotation(double alternativeRotation) {
	if (Math.abs(alternativeRotation - this.alternativeRotation) > 0.0001) {
	    this.alternativeRotation = alternativeRotation;
	    this.checkedRepaint();
	}
    }

    @SuppressWarnings("guieffect")
    private static void translateThenRotate(ViewOptions opts, Graphics2D g2d, double itemx, double itemy,
	    Point2DMinMax minMaxParam, double scaleParam, boolean ignoreRotation, double rot) {
	final DisplayAxis optsDisplayAxisLocal = opts.displayAxis;
	if (null == optsDisplayAxisLocal) {
	    throw new NullPointerException("opts.displayAxis");
	}
	Point2D.Double translatePoint = toScreenPoint(optsDisplayAxisLocal, itemx, itemy, minMaxParam, scaleParam);
	g2d.translate(translatePoint.x, translatePoint.y);
	if (opts.viewRotationsAndImages) {
	    switch (optsDisplayAxisLocal) {
	    case POS_X_POS_Y:
		break;

	    case POS_Y_NEG_X:
		g2d.rotate(Math.PI / 2.0);
		break;

	    case NEG_X_NEG_Y:
		g2d.rotate(Math.PI);
		break;

	    case NEG_Y_POS_X:
		g2d.rotate(3 * Math.PI / 2.0);
		break;
	    }
	    if (!ignoreRotation) {
		g2d.rotate(rot);
	    } else {
		g2d.rotate(opts.alternativeRotation);
	    }
	}
    }

    private double slotMaxDistExpansion = 1.5;

    double getSlotMaxDistExpansion() {
	return slotMaxDistExpansion;
    }

    public void setSlotMaxDistExpansion(double slotMaxDistExpansion) {
	this.slotMaxDistExpansion = slotMaxDistExpansion;
    }

    public double getMaxX() {
	return minmax.max.x;
    }

    public void setMaxX(double maxX) {
	if (Math.abs(this.minmax.max.x - maxX) > 0.0001) {
	    this.clearSizes();
	    this.minmax.max.x = maxX;
	    this.scale_set = false;
	    this.checkedRepaint();
	}
    }

    public double getMinX() {
	return minmax.min.x;
    }

    public void setMinX(double minX) {
	if (Math.abs(this.minmax.min.x - minX) > 0.0001) {
	    this.clearSizes();
	    this.minmax.min.x = minX;
	    this.scale_set = false;
	    this.checkedRepaint();
	}
    }

    public double getMaxY() {
	return minmax.max.y;
    }

    public void setMaxY(double maxY) {
	if (Math.abs(this.minmax.max.y - maxY) > 0.0001) {
	    this.clearSizes();
	    this.minmax.max.y = maxY;
	    this.scale_set = false;
	    this.checkedRepaint();
	}
    }

    public double getMinY() {
	return minmax.min.y;
    }

    public void setMinY(double minY) {
	if (Math.abs(this.minmax.min.y - minY) > 0.0001) {
	    this.clearSizes();
	    this.minmax.min.y = minY;
	    this.scale_set = false;
	    this.checkedRepaint();
	}
    }
}
