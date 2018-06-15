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

import aprs.system.AprsSystemInterface;
import aprs.misc.SlotOffsetProvider;
import aprs.database.PhysicalItem;
import aprs.database.Slot;
import static aprs.simview.DisplayAxis.POS_X_POS_Y;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.utils.CRCLPosemath;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import rcs.posemath.PmCartesian;
import static aprs.database.PhysicalItem.newPhysicalItemNameRotXYScoreType;
import java.awt.image.ImageObserver;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DJPanel extends JPanel {

    private DisplayAxis displayAxis = POS_X_POS_Y;

    public Object2DJPanel() {
        partImageMap = new HashMap<>();
        addPartImage("private", "aprs/partImages/fanuc_tool_holder2.png", 115.0, true, false, -60.0, 10.0);
        addPartImage("shared_gripper", "aprs/partImages/fanuc_tool_holder2.png", 115.0, true, false, -60.0, 10.0);
        addPartImage("small_private_holder", "aprs/partImages/fanuc_tool_holder2.png", 115.0, true, false, 10.0, -60.0);
        addPartImage("pincher", "aprs/partImages/pincher_small_vac_top.png", 115.0, true, false, 0.0, 10.0);
        addPartImage("small_vacuum", "aprs/partImages/pincher_small_vac_top.png", 115.0, true, false, 0.0, 10.0);
        addPartImage("shared_with_fanuc_holder", "aprs/partImages/fanuc_tool_holder2.png", 115.0, true, false, 10.0, -60.0);
        addPartImage("big_gripper_holder", "aprs/partImages/big_gripper_holder2.png", 125.0, true, false, 0.0, 0.0);
        addPartImage("big_vacuum_holder", "aprs/partImages/big_vacuum_holder.png", 215.0, true, false, 0.0, -15.0);
        addPartImage("big_vacuum", "aprs/partImages/big_vacuum.png", 215.0, true, false, 0.0, -15.0);
        addPartImage("big_gripper", "aprs/partImages/big_vacuum.png", 125.0, true, false, 0.0, 0.0);
        addPartImage("sku_part_medium_gear", "aprs/partImages/medium_orange_gear.png", 75.0);
        addPartImage("sku_part_large_gear", "aprs/partImages/large_green_gear.png", 100.0);
        addPartImage("sku_part_small_gear", "aprs/partImages/small_yellow_gear.png", 45.0);
        addPartImage("sku_kit_s2l2_vessel", "aprs/partImages/red_s2l2_kit_tray_up.png", 220.0);
        addPartImage("sku_large_gear_vessel", "aprs/partImages/purple_large_gear_tray_horz.png", 220.0);
        addPartImage("sku_medium_gear_vessel", "aprs/partImages/blue_medium_gear_parts_tray.png", 160.0);
        addPartImage("sku_small_gear_vessel", "aprs/partImages/orange_small_gear_parts_tray.png", 110.0);
        addPartImage("sku_kit_m2l1_vessel", "aprs/partImages/m2l1_kit_tray_right.png", 190.0);
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
        this.viewDetails = viewDetails;
        this.repaint();
    }

    private void addPartImage(@UnknownInitialization Object2DJPanel this,
            String partName, String resName, double realWidth) {
        addPartImage(partName, resName, realWidth, false, false, 0.0, 0.0);
    }

    private void addPartImage(@UnknownInitialization Object2DJPanel this,
            String partName, String resName, double realWidth, boolean ignoreRotations, boolean useHeight, double xoffset, double yoffset) {
        try {
            BufferedImage img = getImageFromSystemResourceName(resName);
            if (null != img) {
                double ratio = useHeight ? realWidth / img.getHeight() : realWidth / img.getWidth();
                if (null != partImageMap) {
                    partImageMap.put(partName, new PartImageInfo(img, ratio, realWidth, ignoreRotations, xoffset, yoffset));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static private BufferedImage getImageFromSystemResourceName(String resName) throws IOException {
        URL url = ClassLoader.getSystemResource(resName);
        if (null == url) {
            throw new IllegalArgumentException("ClassLoader.getSystemResource(" + resName + ") returned null");
        }
        return ImageIO.read(url);
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
        this.displayAxis = displayAxis;
        this.repaint();
    }
    static final List<PhysicalItem> EXAMPLES_ITEMS_LIST = Arrays.asList(// PhysicalItem(String slotMaxDistExpansion, double rotation, double x, double y, double score, String type)
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
            newPhysicalItemNameRotXYScoreType("sku_kit_m2l1_vessel", -1.57, 579.86, 170.14, 0.96, "KT")
    );
    private volatile List<PhysicalItem> items = EXAMPLES_ITEMS_LIST;

    public void setItems(List<PhysicalItem> items) {
        this.items = items;
        updateAddedExtras();
        this.repaint();
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
        this.showOutputItems = showOutputItems;
        this.repaint();
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
        this.outputItems = outputItems;
        updateAddedExtras();
        if (this.showOutputItems) {
            this.repaint();
        }
    }

    private List<PhysicalItem> addedOutputSlots = Collections.emptyList();

    /**
     * Get the value of addedOutputSlots
     *
     * @return the value of addedOutputSlots
     */
    public List<PhysicalItem> getAddedOutputSlots() {
        return addedOutputSlots;
    }

    private List<PhysicalItem> addedSlots = Collections.emptyList();

    /**
     * Get the value of addedSlots
     *
     * @return the value of addedSlots
     */
    public List<PhysicalItem> getAddedSlots() {
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
    public void setAddedSlots(List<PhysicalItem> addedSlots) {
        this.addedSlots = addedSlots;
    }

    public void takeSnapshot(File f, PoseType pose, String label) {
        final int w = this.getWidth();
        final int h = this.getHeight();
        if (w < 1 || h < 1) {
            System.err.println("Can not take snapshot with sized to " + w + " x " + h);
            return;
        }
        takeSnapshot(f, pose, label, w, h);
    }

    public void takeSnapshot(File f, PointType point, String label) {
        final int w = this.getWidth();
        final int h = this.getHeight();
        if (w < 1 || h < 1) {
            System.err.println("Can not take snapshot with sized to " + w + " x " + h);
            return;
        }
        takeSnapshot(f, point, label, w, h);
    }

    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label) {
        final int w = this.getWidth();
        final int h = this.getHeight();
        if (w < 1 || h < 1) {
            System.err.println("Can not take snapshot with sized to " + w + " x " + h);
            return;
        }
        takeSnapshot(f, point, label, w, h);
    }

    private void takeSnapshot(File f, PoseType pose, String label, final int w, final int h) {
        if (null != pose) {
            takeSnapshot(f, pose.getPoint(), label, w, h);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null, w, h);

        }
    }

    private void takeSnapshot(File f, PointType point, String label, final int w, final int h) {
        if (null != point) {
            takeSnapshot(f, CRCLPosemath.toPmCartesian(point), label, w, h);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null, w, h);
        }
    }

    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label, final int w, final int h) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
            int pindex = f.getName().lastIndexOf('.');
            String type = "JPEG";
            if (pindex > 0) {
                type = f.getName().substring(pindex + 1);
            }
            Graphics2D g2d = img.createGraphics();
            AffineTransform origTransform = g2d.getTransform();
            g2d.setColor(this.getBackground());
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(this.getForeground());
            List<PhysicalItem> itemsToPaint = getItemsToPaint();

            if (autoscale) {
                double minX = Double.POSITIVE_INFINITY;
                double maxX = Double.NEGATIVE_INFINITY;
                double minY = Double.POSITIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;
                for (PhysicalItem item : itemsToPaint) {
                    if (minX > item.x) {
                        minX = item.x;
                    }
                    if (minY > item.y) {
                        minY = item.y;
                    }
                    if (maxX < item.x) {
                        maxX = item.x;
                    }
                    if (maxY < item.y) {
                        maxY = item.y;
                    }
                }
                if (null != point) {
                    double x = point.getX();
                    double y = point.getY();
                    if (minX > x) {
                        minX = x;
                    }
                    if (maxX < x) {
                        maxX = x;
                    }
                    if (minY > y) {
                        minY = y;
                    }
                    if (maxY < y) {
                        maxY = y;
                    }
                }
                MinMax tempMinMax = new MinMax();
                tempMinMax.min.x = minX;
                tempMinMax.max.x = maxX;
                tempMinMax.min.y = minY;
                tempMinMax.max.y = maxY;
                ViewOptions opts = new ViewOptions();
                opts.w = w;
                opts.h = h;
                double new_scale = computeNewScale(tempMinMax, opts);
                this.paintItems(g2d, itemsToPaint, null, tempMinMax, opts);
                if (null != point) {
                    paintHighlightedPose(point, g2d, label, tempMinMax, w, h, new_scale, origTransform);
                }
            } else {
                this.paintComponent(g2d);
                if (null != point) {
                    double new_scale = computeNewScale(this.minmax, null);
                    paintHighlightedPose(point, g2d, label, this.minmax, w, h, new_scale, origTransform);
                }
            }
            ImageIO.write(img, type, f);
            System.out.println("Saved snapshot to " + f.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
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

    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        final int w = this.getWidth();
        final int h = this.getHeight();
        if (w < 1 || h < 1) {
            System.err.println("Can not take snapshot with sized to " + w + " x " + h);
            return;
        }
        takeSnapshot(f, itemsToPaint, w, h);
    }

    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, final int w, final int h) {
        try {
            Object2DJPanel.ViewOptions opts = new Object2DJPanel.ViewOptions();
            opts.h = h;
            opts.w = w;
            BufferedImage img = createSnapshotImage(opts, itemsToPaint);
            String type = "JPEG";
            int pindex = f.getName().lastIndexOf('.');
            if (pindex > 0) {
                type = f.getName().substring(pindex + 1);
            }
            ImageIO.write(img, type, f);
            System.out.println("Saved snapshot to " + f.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static class ViewOptions {

        public boolean disableLimitsLine;
        public boolean disableLabels;
        public boolean enableAutoscale;
        public boolean disableShowCurrent;
        public boolean overrideRotationOffset;
        public boolean addExtras;
        public boolean debug;
        public int w;
        public int h;
        public double rotationOffset;
        boolean scale_set;
        double scale;
        boolean paintingComponent;
    }

    BufferedImage createSnapshotImage() {
        return createSnapshotImage(null);
    }

    BufferedImage createSnapshotImage(@Nullable ViewOptions opts) {
        if (null != opts && opts.addExtras) {
            return createSnapshotImage(opts, itemsWithAddedExtras);
        } else {
            return createSnapshotImage(opts, items);
        }
    }

    BufferedImage createSnapshotImage(@Nullable ViewOptions opts, Collection<? extends PhysicalItem> itemsToPaint) {

        Dimension dim = this.getSize();
        int w = (opts != null) ? opts.w : dim.width;
        int h = (opts != null) ? opts.h : dim.height;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(this.getBackground());
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(this.getForeground());
        boolean origUseSepNames = this.useSeparateNames;
//        boolean origViewLimitsLine = this.viewLimitsLine;
//        boolean origAutoscale = autoscale;
        if (null == opts) {
            this.useSeparateNames = true;
        }
        if (autoscale || (null != opts && opts.enableAutoscale)) {
            paintWithAutoScale(itemsToPaint, null, g2d, opts);
        } else {
            paintItems(g2d, itemsToPaint, null, this.minmax, opts);
        }
        if (null == opts) {
            this.useSeparateNames = origUseSepNames;
        }
        return img;
    }

    private void paintWithAutoScale(Collection<? extends PhysicalItem> itemsToPaint, @Nullable PhysicalItem selectedItem, Graphics2D g2d, @Nullable ViewOptions opts) {
        try {
            if (itemsToPaint.isEmpty()) {
                return;
            }
            Dimension dim = getSize();
            int w = (null != opts) ? opts.w : dim.width;
            int h = (null != opts) ? opts.h : dim.height;

            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            if (this.showCurrentXY && (null == opts || !opts.disableShowCurrent)) {
                minX = currentX;
                maxX = currentX;
                minY = currentY;
                maxY = currentY;
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
            MinMax tempMinMax = new MinMax();
            tempMinMax.min.x = minX;
            tempMinMax.max.x = maxX;
            tempMinMax.min.y = minY;
            tempMinMax.max.y = maxY;
            this.paintItems(g2d, itemsToPaint, selectedItem, tempMinMax, opts);
        } catch (Exception e) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, e);
            g2d.drawString(e.toString(), TO_SCREEN_Y_OFFSET, TO_SCREEN_Y_OFFSET);
        }
    }

    public void paintHighlightedPose(PoseType pose, Graphics2D g2d, String label, double minX, double minY, double maxX, double maxY, int w, int h, double currentScale, AffineTransform origTransform) {
        PointType point = pose.getPoint();
        if (null != point) {
            MinMax tempMinMax = new MinMax();
            tempMinMax.min.x = minX;
            tempMinMax.max.x = maxX;
            tempMinMax.min.y = minY;
            tempMinMax.max.y = maxY;
            paintHighlightedPose(CRCLPosemath.toPmCartesian(point), g2d, label, tempMinMax, w, h, currentScale, origTransform);
        }
    }

    private void paintHighlightedPose(@Nullable PmCartesian point, Graphics2D g2d, @Nullable String label, MinMax minmaxParam,
            int width,
            int height,
            double currentScale,
            AffineTransform origTransform) {
        double minX = minmaxParam.min.x;
        double minY = minmaxParam.min.y;
        double maxX = minmaxParam.max.x;
        double maxY = minmaxParam.max.y;
        if (null != point) {
            if (label == null) {
                label = "(null)";
            }
            List<PhysicalItem> itemsToPaint = getItemsToPaint();
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
                int i = itemsToPaint.size();
                double namex = maxX + (maxX - minX) / 10.0;
                double namey = displayMinY + ((double) (i + 1)) / (itemsToPaint.size() + 2) * (displayMaxY - displayMinY);
                switch (displayAxis) {
                    case POS_X_POS_Y:
                        break;

                    case POS_Y_NEG_X:
                        namex = displayMinX + ((double) (i + 1)) / (itemsToPaint.size() + 2) * (displayMaxX - displayMinX);
                        namey = maxY + (maxY - minY) / 10.0;
                        break;

                    case NEG_X_NEG_Y:
                        namex = minX - (maxX - minX) / 10.0;
                        break;

                    case NEG_Y_POS_X:
                        namex = displayMinX + ((double) (i + 1)) / (itemsToPaint.size() + 2) * (displayMaxX - displayMinX);
                        namey = minY - (maxY - minY) / 10.0;
                        break;
                }
                this.translate(g2d, namex, namey, minX, minY, maxX, maxY, width, height, currentScale);
                g2d.setColor(Color.GREEN);
                Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * label.length(), TO_SCREEN_Y_OFFSET);
                g2d.fill(rect);
                g2d.setColor(Color.BLACK);
                g2d.drawString(label, 0, 0);
                if (null != origTransform) {
                    g2d.setTransform(origTransform);
                }
                g2d.draw(
                        new Line2D.Double(
                                toScreenPoint(displayAxis, namex, namey, minmaxParam, currentScale),
                                toScreenPoint(displayAxis, x, y, minmaxParam, currentScale)
                        ));
            }
            this.translate(g2d, x, y, minX, minY, maxX, maxY, width, height, currentScale);
            g2d.setColor(Color.GREEN);
            Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeparateNames ? 1 : label.length()), TO_SCREEN_Y_OFFSET);
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
        this.autoscale = autoscale;
        this.scale_set = false;
        this.repaint();
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
        this.viewRotationsAndImages = viewRotationsAndImages;
        this.repaint();
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
            this.repaint();
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
    public double getScale() {
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
    private MinMax minmax = new MinMax();

    private void translate(Graphics2D g2d, double itemx, double itemy, double minX, double minY, double maxX, double maxY, int width, int height, double currentScale) {

        MinMax tempMinMax = new MinMax();
        tempMinMax.min.x = minX;
        tempMinMax.max.x = maxX;
        tempMinMax.min.y = minY;
        tempMinMax.max.y = maxY;
        Point2D.Double t = toScreenPoint(displayAxis, itemx, itemy, tempMinMax, currentScale);
        if (width > 0 && height > 0) {
            if (t.x < 0) {
                throw new IllegalArgumentException("t.x < 0 : t.x =" + t.x);
            }
            if (t.y < 0) {
                throw new IllegalArgumentException("t.y < 0 : t.y =" + t.x);
            }
            if (t.x > width) {
                throw new IllegalArgumentException("t.x > width : t.x =" + t.x);
            }
            if (t.y > height) {
                throw new IllegalArgumentException("t.x > width : t.y =" + t.x);
            }
        }
        g2d.translate(t.x, t.y);
    }

    private static Point2D.Double toScreenPoint(DisplayAxis displayAxis, double itemx, double itemy, MinMax minmax, double currentScale) {
        double minX = minmax.min.x;
        double maxX = minmax.max.x;
        double minY = minmax.min.y;
        double maxY = minmax.max.y;
        switch (displayAxis) {
            case POS_X_POS_Y:
                return new Point2D.Double((itemx - minX) * currentScale + TO_SCREEN_X_OFFSET, (maxY - itemy) * currentScale + TO_SCREEN_Y_OFFSET);

            case POS_Y_NEG_X:
                return new Point2D.Double((itemy - minY) * currentScale + TO_SCREEN_X_OFFSET, (itemx - minX) * currentScale + TO_SCREEN_Y_OFFSET);

            case NEG_X_NEG_Y:
                return new Point2D.Double((maxX - itemx) * currentScale + TO_SCREEN_X_OFFSET, (itemy - minY) * currentScale + TO_SCREEN_Y_OFFSET);

            case NEG_Y_POS_X:
                return new Point2D.Double((maxY - itemy) * currentScale + TO_SCREEN_X_OFFSET, (maxX - itemx) * currentScale + TO_SCREEN_Y_OFFSET);
        }
        throw new IllegalStateException("invalid displayAxis");
    }
    private static final int TO_SCREEN_Y_OFFSET = 20;
    private static final int TO_SCREEN_X_OFFSET = 15;

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

    /**
     * Set the value of currentX
     *
     * @param currentX new value of currentX
     */
    void setCurrentX(double currentX) {
        this.currentX = currentX;
        this.repaint();
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
        this.currentY = currentY;
        this.repaint();
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
        this.showCurrentXY = showCurrentXY;
        this.repaint();
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
        this.useSeparateNames = useSeparateNames;
        this.repaint();
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

    @SuppressWarnings("WeakerAccess")
    @Override
    public void paintComponent(Graphics g) {
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
        ViewOptions opts = new ViewOptions();
        opts.w = w;
        opts.h = h;
        opts.scale = this.scale;
        opts.scale_set = this.scale_set && !this.autoscale;
        opts.paintingComponent = true;
        if (null != itemsToPaint && !itemsToPaint.isEmpty()) {
            if (this.autoscale || !Double.isFinite(minmax.min.x) || !Double.isFinite(minmax.min.y) || !Double.isFinite(minmax.max.x) || !Double.isFinite(minmax.max.y)) {
                paintWithAutoScale(itemsToPaint, selectedItem, g2d, opts);
            } else {
                paintItems(g2d, itemsToPaint, selectedItem, this.minmax, opts);
            }
        }
        if (opts.scale_set && opts.scale > 0) {
            this.scale = opts.scale;
            this.scale_set = !this.autoscale;
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

    private static final Color[] labelColors = new Color[]{
        Color.BLACK,
        Color.MAGENTA.darker(),
        Color.BLUE.darker(),
        Color.RED.darker(),
        Color.GREEN.darker()
    };

    @MonotonicNonNull
    private AprsSystemInterface aprsSysInterface;

    /**
     * Get the value of aprsSystemInterface
     *
     * @return the value of aprsSystemInterface
     */
    @Nullable
    public AprsSystemInterface getAprsSystemInterface() {
        return aprsSysInterface;
    }

    /**
     * Set the value of aprsSystemInterface
     *
     * @param aprsSystemInterface new value of aprsSystemInterface
     */
    public void setAprsSystemInterface(AprsSystemInterface aprsSystemInterface) {
        this.aprsSysInterface = aprsSystemInterface;
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
     * @param showAddedToolsAndToolHolders new value of
     * showAddedToolsAndToolHolders
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
        this.showAddedSlotPositions = showAddedSlotPositions;
        updateAddedExtras();
        repaint();
    }

    private List<PhysicalItem> getAvailableToolHolders() {
        if (null == aprsSysInterface) {
            throw new IllegalStateException("null == aprsSysInterface");
        }
        return aprsSysInterface.getAvailableToolHolders();
    }
    
    private List<PhysicalItem> getToolsInHolders() {
        if (null == aprsSysInterface) {
            throw new IllegalStateException("null == aprsSysInterface");
        }
        return aprsSysInterface.getToolsInHolders();
    }

    private List<PhysicalItem> getToolsAndHolders() {
        List<PhysicalItem> l = new ArrayList<>(getAvailableToolHolders());
        l.addAll(getToolsInHolders());
        return l;
    }
    
    private void updateAddedExtras() {
        if (showAddedSlotPositions || showAddedToolsAndToolHolders) {
            if (showAddedToolsAndToolHolders) {
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

    List<PhysicalItem> computeAbsSlotPositions(List<PhysicalItem> l) {
        List<PhysicalItem> absSlotList = new ArrayList<>();
        for (PhysicalItem item : l) {
            if (null != slotOffsetProvider && ("PT".equals(item.getType()) || "KT".equals(item.getType()))) {
                absSlotList.addAll(computeSlotPositions(item));
            }
        }
        return absSlotList;
    }

    @MonotonicNonNull
    private SlotOffsetProvider slotOffsetProvider = null;

    @Nullable
    public SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        this.slotOffsetProvider = slotOffsetProvider;
    }

    private List<PhysicalItem> computeSlotPositions(PhysicalItem item) {
        if (null == slotOffsetProvider) {
            throw new IllegalStateException("slotOffsetProvider is null");
        }
        List<Slot> offsets = slotOffsetProvider.getSlotOffsets(item.getName(), false);
        List<PhysicalItem> slotList = new ArrayList<>();
        if (null != offsets) {
            for (PhysicalItem offset : offsets) {
                String prpName = offset.getPrpName();
                String slotDisplayName = "slot_" + prpName;
                if (slotDisplayName.startsWith("slot_slot_")) {
                    slotDisplayName = slotDisplayName.substring(5);
                }
                slotList.add(newPhysicalItemNameRotXYScoreType(slotDisplayName, 0.0,
                        item.x + (offset.x * Math.cos(item.getRotation()) + offset.y * Math.sin(item.getRotation())),
                        item.y + (-offset.x * Math.sin(item.getRotation()) + offset.y * Math.cos(item.getRotation())),
                        item.getScore(), "S"));
            }
        }
        return slotList;
    }

    private static class PartImageInfo {

        final BufferedImage image;
        final double ratio;
        final double width;
        final boolean ignoreRotations;
        final double xoffset;
        final double yoffset;
        double scale;
        Image scaledImage;

        PartImageInfo(BufferedImage image, double ratio, double width, boolean ignoreRotations, double xoffset, double yoffset) {
            this.image = image;
            this.ratio = ratio;
            this.width = width;
            this.xoffset = xoffset;
            this.yoffset = yoffset;
            this.ignoreRotations = ignoreRotations;
            scale = 1.0;
            scaledImage = image.getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_DEFAULT);
        }

        Image getScaledImage(double scale) {
            int old_w = (int) (ratio * this.scale * image.getWidth());
            int old_h = (int) (ratio * this.scale * image.getHeight());
            int new_w = (int) (ratio * scale * image.getWidth());
            int new_h = (int) (ratio * scale * image.getHeight());
            if (new_w == old_w && new_h == old_h) {
                return scaledImage;
            }
            this.scale = scale;
            scaledImage = image.getScaledInstance(new_w, new_h, Image.SCALE_DEFAULT);
            return scaledImage;
        }

    }

    private @Nullable
    Image capturedPartImage;

    /**
     * Get the value of capturedPartImage
     *
     * @return the value of capturedPartImage
     */
    @Nullable
    public Image getCapturedPartImage() {
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

    private final Map<String, PartImageInfo> partImageMap;

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

    private void paintItems(Graphics2D g2d,
            Collection<? extends PhysicalItem> itemsToPaint,
            @Nullable PhysicalItem selectedItem,
            MinMax minmaxParam,
            @Nullable ViewOptions opts) {
        final double minX = minmaxParam.min.x;
        final double minY = minmaxParam.min.y;
        final double maxX = minmaxParam.max.x;
        final double maxY = minmaxParam.max.y;
        try {
            if (itemsToPaint.isEmpty()) {
                return;
            }
            AffineTransform origTransform = g2d.getTransform();
            double currentRotationOffset = this.rotationOffset;
            if (null != opts && opts.overrideRotationOffset) {
                currentRotationOffset = opts.rotationOffset;
            }
            int maxNameLength
                    = itemsToPaint.stream()
                            .mapToInt((PhysicalItem item) -> item.getName().length())
                            .max().orElse(1);

            if (!Double.isFinite(maxX) || !Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxY)) {
                throw new IllegalArgumentException("Limits must be finite: (" + minX + "," + minY + "," + maxX + "," + maxY + ")");
            }

            Dimension dim = getSize();
            int width = (null != opts) ? opts.w : dim.width;
            int height = (null != opts) ? opts.h : dim.height;
            boolean useSeperateNamesThisTime = useSeparateNames;
            if (null != opts) {
                if (opts.disableLabels) {
                    useSeperateNamesThisTime = false;
                }
            }
            double new_scale = computeNewScale(minmaxParam, opts);
//            this.scale = new_scale;
//            if (null == minCorner) {
//                minCorner = new Point2D.Double();
//            }
//            minCorner.x = minX;
//            minCorner.y = minY;
            double displayMaxY = maxY;
            double displayMinY = minY;
            double displayMinX = minX;
            double displayMaxX = maxX;

            switch (displayAxis) {
                case POS_X_POS_Y:
                    displayMaxX = (width - TO_SCREEN_X_OFFSET) / new_scale + minX;
                    displayMinX = (0 - TO_SCREEN_X_OFFSET) / new_scale + minX;

                    displayMinY = maxY - (height - TO_SCREEN_Y_OFFSET) / new_scale;
                    displayMaxY = maxY - (0 - TO_SCREEN_Y_OFFSET) / new_scale;
                    break;

                case POS_Y_NEG_X:
                    displayMaxX = (height - TO_SCREEN_Y_OFFSET) / new_scale + minX;
                    displayMinX = (0 - TO_SCREEN_X_OFFSET) / new_scale + minX;

                    displayMinY = (width - TO_SCREEN_X_OFFSET) / new_scale + minY;
                    displayMaxY = (0 - TO_SCREEN_Y_OFFSET) / new_scale + minY;
                    break;

                case NEG_X_NEG_Y:
                    displayMaxX = maxX - (width - TO_SCREEN_X_OFFSET) / new_scale;
                    displayMinX = maxX - (0 - TO_SCREEN_X_OFFSET) / new_scale;

                    displayMinY = (height - TO_SCREEN_Y_OFFSET) / new_scale + minY;
                    displayMaxY = (0 - TO_SCREEN_Y_OFFSET) / new_scale + minY;
                    break;

                case NEG_Y_POS_X:
                    displayMaxX = maxX - (height - TO_SCREEN_Y_OFFSET) / new_scale;
                    displayMinX = maxX - (0 - TO_SCREEN_X_OFFSET) / new_scale;

                    displayMinY = (width - TO_SCREEN_X_OFFSET) / new_scale + minY;
                    displayMaxY = (0 - TO_SCREEN_Y_OFFSET) / new_scale + minY;
                    break;
            }
            if (viewLimitsLine && (null == opts || !opts.disableLimitsLine)) {
                g2d.drawString(String.format("MinX,MinY = (%.2f,%.2f), MaxX,MaxY= (%.2f,%.2f), scale=%.2f", minX, minY, maxX, maxY, new_scale), 10, height - 10);
            }
            Collection<? extends PhysicalItem> displayItems = itemsToPaint;
            if (useSeperateNamesThisTime) {
                List<? extends PhysicalItem> displayItemsList = new ArrayList<>(itemsToPaint);
                displayItems = displayItemsList;
                switch (displayAxis) {
                    case POS_X_POS_Y:
                        displayItemsList.sort(Comparator.comparing((PhysicalItem item) -> item.y));
                        break;
                    case NEG_X_NEG_Y:
                        displayItemsList.sort(Comparator.comparing((PhysicalItem item) -> -item.y));
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

            float fsize = this.getWidth() / (2.2f * maxNameLength);
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

            if (viewRotationsAndImages) {
                for (PhysicalItem item : displayItems) {
                    if (null == item) {
                        continue;
                    }
                    if ("P".equals(item.getType())) {
                        continue;
                    }
                    paintPartImage(g2d, minX, minY, maxX, maxY, item, currentRotationOffset, new_scale);
                    g2d.setTransform(origTransform);
                }
                for (PhysicalItem item : displayItems) {
                    if (null == item) {
                        continue;
                    }
                    if (!("P".equals(item.getType()))) {
                        continue;
                    }
                    paintPartImage(g2d, minX, minY, maxX, maxY, item, currentRotationOffset, new_scale);
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
                if (useSeperateNamesThisTime) {
                    item.setLabelColor(labelColors[i % labelColors.length]);
                    g2d.setColor(item.getLabelColor());
                    double namex = maxX + (maxX - minX) / 5.0;
                    double namey = displayMinY + ((double) (i + 1)) / (itemsToPaint.size() + 2) * (displayMaxY - displayMinY);
                    switch (displayAxis) {
                        case POS_X_POS_Y:
                            break;

                        case POS_Y_NEG_X:
                            namex = displayMinX + ((double) (i + 1)) / (itemsToPaint.size() + 2) * (displayMaxX - displayMinX);
                            namey = maxY + (maxY - minY) / 5.0;
                            break;

                        case NEG_X_NEG_Y:
                            namex = minX - (maxX - minX) / 5.0;
                            break;

                        case NEG_Y_POS_X:
                            namex = displayMinX + ((double) (i + 1)) / (itemsToPaint.size() + 2) * (displayMaxX - displayMinX);
                            namey = minY - (maxY - minY) / 5.0;
                            break;
                    }
                    this.translate(g2d, namex, namey, minX, minY, maxX, maxY, width, height, new_scale);
                    if (item == selectedItem) {
                        g2d.setColor(Color.WHITE);
                        Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.getName().length(), TO_SCREEN_Y_OFFSET);
                        g2d.fill(rect);
                    }
                    g2d.setColor(item.getLabelColor());
                    g2d.drawString(item.getName(), 0, 0);
                    g2d.setTransform(origTransform);
                    g2d.draw(
                            new Line2D.Double(
                                    toScreenPoint(displayAxis, namex, namey, minmaxParam, new_scale),
                                    toScreenPoint(displayAxis, item.x, item.y, minmaxParam, new_scale)
                            ));
                }
            }
            g2d.setFont(origFont);

            for (PhysicalItem item : displayItems) {
                if (null == item) {
                    continue;
                }

                if (null != partImageMap && !partImageMap.isEmpty()) {
                    if (viewRotationsAndImages && null != getPartImageInfo(item)) {
                        continue;
                    }
                }
                boolean imageShown = checkImageShown(item);
                translateThenRotateItem(g2d, minmaxParam, item, currentRotationOffset, new_scale, false);
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
                        Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                int namelen = useSeperateNamesThisTime ? 1 : item.getName().length();
                Rectangle2D.Double itemDisplayRect
                        = new Rectangle2D.Double(
                                -5, -12, // x,y
                                10 + 10 * namelen, TO_SCREEN_Y_OFFSET);
                if (opts != null && opts.paintingComponent) {
                    item.setDisplayRect(itemDisplayRect);
                }
                g2d.setColor(this.getBackground());
                g2d.fill(itemDisplayRect);
                g2d.setTransform(origTransform);
            }
            i = 0;
            for (PhysicalItem item : displayItems) {
                ++i;
                if (null == item) {
                    continue;
                }
                boolean imageShown = checkImageShown(item);
                translateThenRotateItem(g2d, minmaxParam, item, currentRotationOffset, new_scale, false);
                g2d.setColor(Color.BLACK);
                if (!imageShown) {
                    if (!useSeperateNamesThisTime) {
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
                        Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (!imageShown) {
                    int namelen = useSeperateNamesThisTime ? 1 : item.getName().length();
                    Rectangle2D.Double itemDisplayRect
                            = new Rectangle2D.Double(
                                    -5, -12, // x,y
                                    10 + 10 * namelen, TO_SCREEN_Y_OFFSET);
                    if (opts != null && opts.paintingComponent) {
                        item.setDisplayRect(itemDisplayRect);
                    }
                    g2d.setColor(item.getLabelColor());
                    g2d.draw(itemDisplayRect);
                }

                try {
                    if (null != slotOffsetProvider && ("PT".equals(item.getType()) || "KT".equals(item.getType()))) {
                        List<Slot> offsets = slotOffsetProvider.getSlotOffsets(item.getName(), false);
                        if (null != offsets) {
                            for (PhysicalItem offset : offsets) {
                                double mag = offset.mag();
                                if (item.getMaxSlotDist() < mag) {
                                    item.setMaxSlotDist(mag);
                                }
                            }
                        }
                    }
                    if (viewDetails) {
                        if (item.getMaxSlotDist() > 0) {
                            g2d.draw(new Arc2D.Double(-item.getMaxSlotDist() * new_scale * slotMaxDistExpansion, -item.getMaxSlotDist() * new_scale * slotMaxDistExpansion, item.getMaxSlotDist() * 2.0 * new_scale * slotMaxDistExpansion, item.getMaxSlotDist() * 2.0 * new_scale * slotMaxDistExpansion, 0.0, 360.0, Arc2D.OPEN));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                g2d.setTransform(origTransform);
            }
            g2d.setColor(Color.BLACK);
            if (null != selectedItem) {
                boolean imageShown = checkImageShown(selectedItem);
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
                if (!imageShown || viewDetails) {
                    translateThenRotateItem(g2d, minmaxParam, selectedItem, currentRotationOffset, new_scale, false);
                    g2d.setColor(Color.WHITE);
                    String typeString = getItemType(selectedItem);
                    Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeperateNamesThisTime ? typeString.length() : selectedItem.getName().length()), TO_SCREEN_Y_OFFSET);
                    g2d.fill(rect);
                    g2d.setColor(Color.BLACK);
                    g2d.draw(rect);
                    if (!useSeperateNamesThisTime) {
                        g2d.drawString(selectedItem.getName(), 0, 0);
                    } else {
                        g2d.drawString(selectedItem.getType(), 0, 0);
                    }
                }
                g2d.setTransform(origTransform);
            }
            if (this.showCurrentXY && (null == opts || !opts.disableShowCurrent)) {
                Color origColor = g2d.getColor();
                Point2D.Double fromPoint = capturedPartPoint;
                if (null != fromPoint) {
                    g2d.setColor(Color.MAGENTA);
                    Point2D.Double captureScreenPt = toScreenPoint(displayAxis, fromPoint.x, fromPoint.y, minmaxParam, new_scale);
                    Point2D.Double currentScreenPt = toScreenPoint(displayAxis, currentX, currentY, minmaxParam, new_scale);
                    g2d.draw(new Line2D.Double(currentScreenPt, captureScreenPt));
                }
                this.translate(g2d, currentX, currentY, minX, minY, maxX, maxY, -1, -1, new_scale);
                if (endEffectorClosed) {
                    g2d.setColor(Color.black);
                    g2d.fillArc(-5, -5, 10, 10, 0, 360);
                } else {
                    g2d.setColor(Color.white);
                    g2d.drawArc(-10, -10, TO_SCREEN_Y_OFFSET, TO_SCREEN_Y_OFFSET, 0, 360);
                }
                g2d.setColor(Color.red);
                g2d.drawLine(-20, 0, TO_SCREEN_Y_OFFSET, 0);
                g2d.drawLine(0, -20, 0, TO_SCREEN_Y_OFFSET);
                g2d.setColor(origColor);
                g2d.setTransform(origTransform);
            }
        } catch (Exception exception) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, exception);
            g2d.drawString(exception.toString(), TO_SCREEN_Y_OFFSET, TO_SCREEN_Y_OFFSET);
        }
    }

//    private volatile double last_scale_x;
//    private volatile double last_scale_y;
//    private volatile boolean last_useSeperateNamesThisTime;
//    private volatile int last_width;
//    private volatile int last_borderWidth;
//    private volatile double last_maxX1;
//    private volatile double last_minX1;
//    private volatile int last_height;
//    private volatile int last_borderHeight;
//    private volatile double last_maxY1;
//    private volatile double last_minY1;
    private static class MinMax {

        Point2D.Double min;
        Point2D.Double max;

        MinMax() {
            min = new Point2D.Double(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            max = new Point2D.Double(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        }
    }

    private double computeNewScale(MinMax minmax, @Nullable ViewOptions opts) {
        boolean useSeperateNamesThisTime = useSeparateNames;
        if (null != opts) {
            if (opts.disableLabels) {
                useSeperateNamesThisTime = false;
            }
        }
        return computeNewScale(displayAxis, useSeperateNamesThisTime, getSize(), minmax, opts);
    }

    private static double computeNewScale(DisplayAxis displayAxis, boolean useSeparateNames, Dimension dim, MinMax minmax, @Nullable ViewOptions opts) {
        Point2D.Double min = minmax.min;
        Point2D.Double max = minmax.max;
        if (null != opts && opts.scale_set && opts.scale > 0) {
            return opts.scale;
        }
        int width = (null != opts) ? opts.w : dim.width;
        int height = (null != opts) ? opts.h : dim.height;
        boolean useSeperateNamesThisTime = useSeparateNames;
        int borderWidth = 30;
        int borderHeight = 60;
        if (null != opts) {
            if (opts.disableLabels) {
                useSeperateNamesThisTime = false;
                borderWidth = 0;
                borderHeight = 0;
            }
        }
        double new_scale;
        double scale_x = 1;
        double scale_y = 1;
        int displayWidth = (useSeperateNamesThisTime ? (width / 2) : width);
        double diffx = (max.x - min.x);
        double diffy = (max.y - min.y);
        int diffwidth = (displayWidth - borderWidth - TO_SCREEN_X_OFFSET);
        int diffheight = (height - borderHeight - TO_SCREEN_Y_OFFSET);
        switch (displayAxis) {
            case POS_X_POS_Y:
            case NEG_X_NEG_Y:
                scale_x = diffwidth / diffx ;
                scale_y = diffheight / diffy;
                break;
            case POS_Y_NEG_X:
            case NEG_Y_POS_X:
                scale_x = diffwidth / diffy;
                scale_y = diffheight / diffx;
                break;
        }
        assert (Double.isFinite(scale_x)) :
                ("scale_x = " + scale_x);
        assert (Double.isFinite(scale_y)) :
                ("scale_y = " + scale_y);
        new_scale = Math.min(scale_x, scale_y);
//        if (Math.abs(new_scale - this.scale) > 0.1) {
//            System.out.println("jump in scale");
//        }
//        last_scale_x = scale_x;
//        last_scale_y = scale_y;
//        last_useSeperateNamesThisTime = useSeperateNamesThisTime;
//        last_width = width;
//        last_borderWidth = borderWidth;
//        last_maxX1 = max.x;
//        last_minX1 = minX1;
//        last_height = height;
//        last_borderHeight = borderHeight;
//        last_maxY1 = maxY1;
//        last_minY1 = minY1;
        if (null != opts && new_scale > 0) {
            opts.scale_set = true;
            opts.scale = new_scale;
        }
//        if (opts.debug) {
//            Point2D.Double minXMinYTranslated = toScreenPoint(displayAxis, min.x, min.y, minmax, new_scale);
//            System.out.println("dim = " + dim);
//            System.out.println("minXMinYTranslated = " + minXMinYTranslated.x + "," + minXMinYTranslated.y);
//            assert (minXMinYTranslated.x >= 0) : "minXMinYTranslated.x >= 0";
//            assert (minXMinYTranslated.x <= width) : "minXMinYTranslated.x <= dim.width";
//            assert (minXMinYTranslated.y >= 0) : "minXMinYTranslated.y >= 0";
//            assert (minXMinYTranslated.y <= height) : "minXMinYTranslated.y <= dim.width";
//
//            Point2D.Double minXMaxYTranslated = toScreenPoint(displayAxis, min.x, max.y, minmax, new_scale);
//            System.out.println("minXMaxYTranslated = " + minXMaxYTranslated.x + "," + minXMaxYTranslated.y);
//            assert (minXMaxYTranslated.x >= 0) : "minXMaxYTranslated.x >= 0";
//            assert (minXMaxYTranslated.x <= width) : "minXMaxYTranslated.x <= dim.width";
//            assert (minXMaxYTranslated.y >= 0) : "minXMaxYTranslated.y >= 0";
//            assert (minXMaxYTranslated.y <= height) : "minXMaxYTranslated.y <= dim.width";
//
//            Point2D.Double maxXMinYTranslated = toScreenPoint(displayAxis, max.x, min.y, minmax, new_scale);
//            System.out.println("maxXMinYTranslated = " + maxXMinYTranslated.x + "," + maxXMinYTranslated.y);
//            assert (maxXMinYTranslated.x >= 0) : "maxXMinYTranslated.x >= 0";
//            assert (maxXMinYTranslated.x <= width) : "maxXMinYTranslated.x <= dim.width";
//            assert (maxXMinYTranslated.y >= 0) : "maxXMinYTranslated.y >= 0";
//            assert (maxXMinYTranslated.y <= height) : "maxXMinYTranslated.y <= dim.width";
//
//            Point2D.Double maxXMaxYTranslated = toScreenPoint(displayAxis, max.x, max.y, minmax, new_scale);
//            System.out.println("maxXMinYTranslated = " + maxXMaxYTranslated.x + "," + maxXMaxYTranslated.y);
//            assert (maxXMaxYTranslated.x >= 0) : "maxXMinYTranslated.x >= 0";
//            assert (maxXMaxYTranslated.x <= width) : "maxXMinYTranslated.x <= dim.width";
//            assert (maxXMaxYTranslated.y >= 0) : "maxXMinYTranslated.y >= 0";
//            assert (maxXMaxYTranslated.y <= height) : "maxXMinYTranslated.y <= dim.width";
//        }
        return new_scale;
    }

    private boolean checkImageShown(PhysicalItem item) {
        return viewRotationsAndImages
                && null != partImageMap
                && !partImageMap.isEmpty()
                && null != getPartImageInfo(item);
    }

    private void paintPartImage(Graphics2D g2d, double minX, double minY, double maxX, double maxY, PhysicalItem item, double rotationOffsetParam, double currentScale) {
        if (!viewRotationsAndImages) {
            return;
        }
        if (null == partImageMap || partImageMap.isEmpty()) {
            return;
        }
        PartImageInfo info = getPartImageInfo(item);
        if (null != info) {
            Image img = info.getScaledImage(currentScale);
            MinMax tempMinMax = new MinMax();
            tempMinMax.min.x = minX;
            tempMinMax.max.x = maxX;
            tempMinMax.min.y = minY;
            tempMinMax.max.y = maxY;
            translateThenRotate(g2d, item.x + info.xoffset, item.y + info.yoffset, tempMinMax, currentScale, info.ignoreRotations, -rotationOffsetParam - item.getRotation());
//            translateThenRotateItem(g2d, minX, minY, maxX, maxY, item, rotationOffsetParam, currentScale, info.ignoreRotations);
            g2d.translate(-(img.getWidth(this) / 2.0), -(img.getHeight(this) / 2.0));
            g2d.drawImage(img, null, null);
            if (viewDetails) {
                g2d.translate(-1, -1);
                g2d.setColor(item.getLabelColor());
                g2d.draw(new Rectangle2D.Double(0, 0, img.getWidth(this) + 2, img.getHeight(this) + 2));
            }
            g2d.setColor(Color.BLACK);

        }
    }

    @Nullable private PartImageInfo getPartImageInfo(PhysicalItem item) {
        PartImageInfo info = partImageMap.get(item.getName());
        if (null == info) {
            if (item.getName().startsWith("sku_part_")) {
                info = partImageMap.get(item.getName().substring("sku_part_".length()));
            } else {
                info = partImageMap.get("sku_part_" + item.getName());
            }
        }
        if (null == info) {
            if (item.getName().startsWith("sku_")) {
                info = partImageMap.get(item.getName().substring("sku_".length()));
            } else {
                info = partImageMap.get("sku_" + item.getName());
            }
        }
        if (null == info) {
            if (item.getName().startsWith("part_")) {
                info = partImageMap.get(item.getName().substring("part_".length()));
            } else {
                info = partImageMap.get("part_" + item.getName());
            }
        }
        if (null != info) {
            ImageObserver observer = new ImageObserver() {
                @Override
                public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
                    if ((infoflags & ImageObserver.WIDTH) == ImageObserver.WIDTH
                            && (infoflags & ImageObserver.HEIGHT) == ImageObserver.HEIGHT) {
                        Rectangle2D.Double itemDisplayRect
                                = new Rectangle2D.Double(
                                        -w / 2, -h / 2, // x,y
                                        w, h// w,h
                                );
                        item.setDisplayRect(itemDisplayRect);
                        return false;
                    }
                    return true;
                }
            };
            int w = info.scaledImage.getWidth(observer);
            int h = info.scaledImage.getHeight(observer);
            if (w > 0 && h > 0) {
                Rectangle2D.Double itemDisplayRect
                        = new Rectangle2D.Double(
                                -w / 2, -h / 2, // x,y
                                w, h// w,h
                        );
                item.setDisplayRect(itemDisplayRect);
            }
        }
//        if (null == info) {
//            System.err.println("partImageMap.keySet() = " + partImageMap.keySet());
//            throw new IllegalStateException("Failed to find info for " + item.getName() + " in \r\n " + partImageMap.keySet());
//        }
        return info;
    }

    private void translateThenRotateItem(Graphics2D g2d, MinMax minMaxParam, PhysicalItem item, double rotationOffsetParam, double scaleParam, boolean ignoreRotation) {
        double itemx = item.x;
        double itemy = item.y;
        double rot = -rotationOffsetParam - item.getRotation();
        translateThenRotate(g2d, itemx, itemy, minMaxParam, scaleParam, ignoreRotation, rot);
    }

    private void translateThenRotate(Graphics2D g2d, double itemx, double itemy, MinMax minMaxParam, double scaleParam, boolean ignoreRotation, double rot) {
        Point2D.Double translatePoint = toScreenPoint(displayAxis, itemx, itemy, minMaxParam, scaleParam);
//        switch (displayAxis) {
//            case POS_X_POS_Y:
//                g2d.translate((itemx - minX1) * scaleParam + 15, (maxY1 - itemy) * scaleParam + 20);
//                break;
//            case POS_Y_NEG_X:
//                g2d.translate((itemy - minY1) * scaleParam + 15, (itemx - minX1) * scaleParam + 20);
//                break;
//            case NEG_X_NEG_Y:
//                g2d.translate((maxX1 - itemx) * scaleParam + 15, (itemy - minY1) * scaleParam + 20);
//                break;
//            case NEG_Y_POS_X:
//                g2d.translate((maxY1 - itemy) * scaleParam + 15, (maxX1 - itemx) * scaleParam + 20);
//                break;
//        }
        g2d.translate(translatePoint.x, translatePoint.y);
        if (viewRotationsAndImages) {
            if (!ignoreRotation) {
                switch (displayAxis) {
                    case POS_X_POS_Y:
//                    g2d.rotate(Math.PI/2.0);
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
                g2d.rotate(rot);
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
        this.minmax.max.x = maxX;
        this.scale_set = false;
        this.repaint();
    }

    public double getMinX() {
        return minmax.min.x;
    }

    public void setMinX(double minX) {
        this.minmax.min.x = minX;
        this.scale_set = false;
        this.repaint();
    }

    public double getMaxY() {
        return minmax.max.y;
    }

    public void setMaxY(double maxY) {
        this.minmax.max.y = maxY;
        this.scale_set = false;
        this.repaint();
    }

    public double getMinY() {
        return minmax.min.y;
    }

    public void setMinY(double minY) {
        this.minmax.min.y = minY;
        this.scale_set = false;
        this.repaint();
    }
}
