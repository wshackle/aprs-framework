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
package aprs.framework.simview;

import aprs.framework.AprsJFrame;
import aprs.framework.SlotOffsetProvider;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.Slot;
import static aprs.framework.simview.DisplayAxis.POS_X_POS_Y;
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
import static aprs.framework.database.PhysicalItem.newPhysicalItemNameRotXYScoreType;

import java.util.stream.StreamSupport;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DJPanel extends JPanel {

    private DisplayAxis displayAxis = POS_X_POS_Y;

    public Object2DJPanel() {
        partImageMap = new HashMap<>();
        addPartImage("sku_part_medium_gear", "aprs/framework/partImages/medium_orange_gear.png", 75.0);
        addPartImage("sku_part_large_gear", "aprs/framework/partImages/large_green_gear.png", 100.0);
        addPartImage("sku_part_small_gear", "aprs/framework/partImages/small_yellow_gear.png", 45.0);
        addPartImage("sku_kit_s2l2_vessel", "aprs/framework/partImages/red_s2l2_kit_tray_up.png", 220.0);
        addPartImage("sku_large_gear_vessel", "aprs/framework/partImages/purple_large_gear_tray_horz.png", 220.0);
        addPartImage("sku_medium_gear_vessel", "aprs/framework/partImages/blue_medium_gear_parts_tray.png", 160.0);
        addPartImage("sku_small_gear_vessel", "aprs/framework/partImages/orange_small_gear_parts_tray.png", 110.0);
        addPartImage("sku_kit_m2l1_vessel", "aprs/framework/partImages/m2l1_kit_tray_right.png", 190.0);
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
    public void setViewDetails(boolean viewDetails) {
        this.viewDetails = viewDetails;
        this.repaint();
    }

    private void addPartImage(String partName, String resName, double realWidth) {
        try {
            BufferedImage img = getImageFromSystemResourceName(resName);
            if (null != img) {
                double ratio = realWidth / img.getWidth();
                partImageMap.put(partName, new PartImageInfo(img, ratio));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private BufferedImage getImageFromSystemResourceName(String resName) throws IOException {
        URL url = ClassLoader.getSystemResource(resName);
        if (null == resName) {
            throw new IllegalArgumentException("ClassLoader.getSystemResource(" + resName + ") returned null");
        }
        BufferedImage image = ImageIO.read(url);
        return image;
    }

    /**
     * Get the value of displayAxis
     *
     * @return the value of displayAxis
     */
    public DisplayAxis getDisplayAxis() {
        return displayAxis;
    }

    /**
     * Set the value of displayAxis
     *
     * @param displayAxis new value of displayAxis
     */
    public void setDisplayAxis(DisplayAxis displayAxis) {
        this.displayAxis = displayAxis;
        this.repaint();
    }
    public static final List<PhysicalItem> EXAMPLES_ITEMS_LIST = Arrays.asList(// PhysicalItem(String slotMaxDistExpansion, double rotation, double x, double y, double score, String type)
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
        this.addedSlots = computeAbsSlotPositions(items);
        this.itemsWithAddedSlots = new ArrayList<>();
        this.itemsWithAddedSlots.addAll(items);
        this.itemsWithAddedSlots.addAll(this.addedSlots);
        this.repaint();
    }

    private boolean showOutputItems = false;

    /**
     * Get the value of showOutputItems
     *
     * @return the value of showOutputItems
     */
    public boolean isShowOutputItems() {
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

    private List<PhysicalItem> outputItems;

    /**
     * Get the value of outputItems
     *
     * @return the value of outputItems
     */
    public List<PhysicalItem> getOutputItems() {
        return ((null != outputItems) ? Collections.unmodifiableList(outputItems) : null);
    }

    /**
     * Set the value of outputItems
     *
     * @param outputItems new value of outputItems
     */
    public void setOutputItems(List<PhysicalItem> outputItems) {
        this.outputItems = outputItems;
        this.addedOutputSlots = computeAbsSlotPositions(outputItems);
        this.outputItemsWithAddedSlots = new ArrayList<>();
        this.outputItemsWithAddedSlots.addAll(outputItems);
        this.outputItemsWithAddedSlots.addAll(this.addedOutputSlots);
        if (this.showOutputItems) {
            this.repaint();
        }
    }

    private List<PhysicalItem> addedOutputSlots;

    /**
     * Get the value of addedOutputSlots
     *
     * @return the value of addedOutputSlots
     */
    public List<PhysicalItem> getAddedOutputSlots() {
        return addedOutputSlots;
    }

    /**
     * Set the value of addedOutputSlots
     *
     * @param addedOutputSlots new value of addedOutputSlots
     */
    public void setAddedOutputSlots(List<PhysicalItem> addedOutputSlots) {
        this.addedOutputSlots = addedOutputSlots;
    }

    private List<PhysicalItem> addedSlots;

    /**
     * Get the value of addedSlots
     *
     * @return the value of addedSlots
     */
    public List<PhysicalItem> getAddedSlots() {
        return addedSlots;
    }

    private List<PhysicalItem> itemsWithAddedSlots;

    /**
     * Get the value of itemsWithAddedSlots
     *
     * @return the value of itemsWithAddedSlots
     */
    public List<PhysicalItem> getItemsWithAddedSlots() {
        return itemsWithAddedSlots;
    }

    /**
     * Set the value of itemsWithAddedSlots
     *
     * @param itemsWithAddedSlots new value of itemsWithAddedSlots
     */
    public void setItemsWithAddedSlots(List<PhysicalItem> itemsWithAddedSlots) {
        this.itemsWithAddedSlots = itemsWithAddedSlots;
    }

    private List<PhysicalItem> outputItemsWithAddedSlots;

    /**
     * Get the value of outputItemsWithAddedSlots
     *
     * @return the value of outputItemsWithAddedSlots
     */
    public List<PhysicalItem> getOutputItemsWithAddedSlots() {
        return outputItemsWithAddedSlots;
    }

    /**
     * Set the value of outputItemsWithAddedSlots
     *
     * @param outputItemsWithAddedSlots new value of outputItemsWithAddedSlots
     */
    public void setOutputItemsWithAddedSlots(List<PhysicalItem> outputItemsWithAddedSlots) {
        this.outputItemsWithAddedSlots = outputItemsWithAddedSlots;
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

    public void takeSnapshot(File f, PmCartesian point, String label) {
        final int w = this.getWidth();
        final int h = this.getHeight();
        if (w < 1 || h < 1) {
            System.err.println("Can not take snapshot with sized to " + w + " x " + h);
            return;
        }
        takeSnapshot(f, point, label, w, h);
    }

    public void takeSnapshot(File f, PoseType pose, String label, final int w, final int h) {
        if (null != pose) {
            takeSnapshot(f, pose.getPoint(), label, w, h);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null, w, h);

        }
    }

    public void takeSnapshot(File f, PointType point, String label, final int w, final int h) {
        if (null != point) {
            takeSnapshot(f, CRCLPosemath.toPmCartesian(point), label, w, h);
        } else {
            takeSnapshot(f, (PmCartesian) null, (String) null, w, h);
        }
    }

    public void takeSnapshot(File f, PmCartesian point, String label, final int w, final int h) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
            int pindex = f.getName().lastIndexOf('.');
            String type = "JPEG";
            if (pindex > 0) {
                type = f.getName().substring(pindex + 1);
            }
            Graphics2D g2d = img.createGraphics();
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
                this.paintItems(g2d, itemsToPaint, null, minX, minY, maxX, maxY, w, h);
                paintHighlightedPose(point, g2d, label, minX, minY, maxX, maxY, w, h);
            } else {
                this.paintComponent(g2d);
                paintHighlightedPose(point, g2d, label, this.minX, this.minY, this.maxX, this.maxY, w, h);
            }
            ImageIO.write(img, type, f);
            System.out.println("Saved snapshot to " + f.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<PhysicalItem> getItemsToPaint() {
        List<PhysicalItem> itemsToPaint = this.items;
        if (showAddedSlotPositions && null != this.itemsWithAddedSlots) {
            itemsToPaint = this.itemsWithAddedSlots;
        }
        if (showOutputItems && null != outputItems) {
            itemsToPaint = this.outputItems;
            if (showAddedSlotPositions && null != this.outputItemsWithAddedSlots) {
                itemsToPaint = this.outputItemsWithAddedSlots;
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
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
            int pindex = f.getName().lastIndexOf('.');
            String type = "JPEG";
            if (pindex > 0) {
                type = f.getName().substring(pindex + 1);
            }
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(this.getBackground());
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(this.getForeground());
            boolean origUseSepNames = this.useSeparateNames;
            this.useSeparateNames = true;
            if (autoscale) {
                paintWithAutoScale(itemsToPaint, null, g2d, w, h);
            } else {
                paintItems(g2d, itemsToPaint, null, minX, minY, maxX, maxY, w, h);
            }
            this.useSeparateNames = origUseSepNames;
            ImageIO.write(img, type, f);
            System.out.println("Saved snapshot to " + f.getCanonicalPath());
        } catch (Exception ex) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void paintWithAutoScale(Collection<? extends PhysicalItem> itemsToPaint, PhysicalItem selectedItem, Graphics2D g2d, int w, int h) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (PhysicalItem item : itemsToPaint) {
            double itemSize = item.getMaxSlotDist();
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
        this.paintItems(g2d, itemsToPaint, selectedItem, minX, minY, maxX, maxY, w, h);
    }

    public void paintHighlightedPose(PoseType pose, Graphics2D g2d, String label, double minX, double minY, double maxX, double maxY, int w, int h) {
        PointType point = pose.getPoint();
        if (null != point) {
            paintHighlightedPose(CRCLPosemath.toPmCartesian(point), g2d, label, minX, minY, maxX, maxY, w, h);
        }
    }

    public void paintHighlightedPose(PointType point, Graphics2D g2d, String label, double minX, double minY, double maxX, double maxY, int w, int h) {
        paintHighlightedPose(CRCLPosemath.toPmCartesian(point), g2d, label, minX, minY, maxX, maxY, w, h);
    }

    public void paintHighlightedPose(PmCartesian point, Graphics2D g2d, String label, double minX, double minY, double maxX, double maxY,
            int width,
            int height) {
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
                    displayMaxX = (width - 15) / scale + minX;
                    displayMinX = (0 - 15) / scale + minX;

                    displayMinY = maxY - (height - 20) / scale;
                    displayMaxY = maxY - (0 - 20) / scale;
//                g2d.translate((itemx - minX) * scale + 15, (maxY - itemy) * scale + 20);
                    break;

                case POS_Y_NEG_X:
                    displayMaxX = (height - 20) / scale + minX;
                    displayMinX = (0 - 15) / scale + minX;

                    displayMinY = (width - 15) / scale + minY;
                    displayMaxY = (0 - 20) / scale + minY;
//                g2d.translate((itemy - minY) * scale + 15, (itemx - minX) * scale + 20);
                    break;

                case NEG_X_NEG_Y:
                    displayMaxX = maxX - (width - 15) / scale;
                    displayMinX = maxX - (0 - 15) / scale;

                    displayMinY = (height - 20) / scale + minY;
                    displayMaxY = (0 - 20) / scale + minY;
//                g2d.translate((maxX - itemx) * scale + 15, (itemy - minY) * scale + 20);
                    break;

                case NEG_Y_POS_X:
                    displayMaxX = maxX - (height - 20) / scale;
                    displayMinX = maxX - (0 - 15) / scale;

                    displayMinY = (width - 15) / scale + minY;
                    displayMaxY = (0 - 20) / scale + minY;
//                g2d.translate((maxY - itemy) * scale + 15, (maxX - itemx) * scale + 20);
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
                this.translate(g2d, namex, namey, minX, minY, maxX, maxY);
                g2d.setColor(Color.GREEN);
                Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * label.length(), 20);
                g2d.fill(rect);
                g2d.setColor(Color.BLACK);
                g2d.drawString(label, 0, 0);
                g2d.setTransform(origTransform);
                g2d.draw(new Line2D.Double(toScreenPoint(namex, namey, minX, minY, maxX, maxY), toScreenPoint(x, y, minX, minY, maxX, maxY)));
            }
            this.translate(g2d, x, y, minX, minY, maxX, maxY);
            g2d.setColor(Color.GREEN);
            Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeparateNames ? 1 : label.length()), 20);
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
    public void setAutoscale(boolean autoscale) {
        this.autoscale = autoscale;
        this.repaint();
    }

    private int selectedItemIndex = -1;

    /**
     * Get the value of selectedItemIndex
     *
     * @return the value of selectedItemIndex
     */
    public int getSelectedItemIndex() {
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
    public void setViewRotationsAndImages(boolean viewRotationsAndImages) {
        this.viewRotationsAndImages = viewRotationsAndImages;
        this.repaint();
    }

    /**
     * Set the value of selectedItemIndex
     *
     * @param selectedItemIndex new value of selectedItemIndex
     */
    public void setSelectedItemIndex(int selectedItemIndex) {
        this.selectedItemIndex = selectedItemIndex;
        this.repaint();
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

    private Point2D.Double minCorner;

    /**
     * Get the value of minCorner
     *
     * @return the value of minCorner
     */
    public Point2D.Double getMinCorner() {
        return minCorner;
    }

    private double maxX = Double.NEGATIVE_INFINITY;
    private double minX = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;

    private void translate(Graphics2D g2d, double itemx, double itemy, double minX, double minY, double maxX, double maxY) {
        switch (displayAxis) {
            case POS_X_POS_Y:
                g2d.translate((itemx - minX) * scale + 15, (maxY - itemy) * scale + 20);
                break;

            case POS_Y_NEG_X:
                g2d.translate((itemy - minY) * scale + 15, (itemx - minX) * scale + 20);
                break;

            case NEG_X_NEG_Y:
                g2d.translate((maxX - itemx) * scale + 15, (itemy - minY) * scale + 20);
                break;

            case NEG_Y_POS_X:
                g2d.translate((maxY - itemy) * scale + 15, (maxX - itemx) * scale + 20);
                break;
        }
    }

    private Point2D.Double toScreenPoint(double itemx, double itemy, double minX, double minY, double maxX, double maxY) {
        switch (displayAxis) {
            case POS_X_POS_Y:
                return new Point2D.Double((itemx - minX) * scale + 15, (maxY - itemy) * scale + 20);

            case POS_Y_NEG_X:
                return new Point2D.Double((itemy - minY) * scale + 15, (itemx - minX) * scale + 20);

            case NEG_X_NEG_Y:
                return new Point2D.Double((maxX - itemx) * scale + 15, (itemy - minY) * scale + 20);

            case NEG_Y_POS_X:
                return new Point2D.Double((maxY - itemy) * scale + 15, (maxX - itemx) * scale + 20);
        }
        throw new IllegalStateException("invalid displayAxis");
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
    public void setCurrentX(double currentX) {
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
    public void setCurrentY(double currentY) {
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
    public void setShowCurrentXY(boolean showCurrentXY) {
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
    public void setUseSeparateNames(boolean useSeparateNames) {
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

    private AffineTransform origTransform = null;

    @Override
    protected void paintComponent(Graphics g) {
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
        if (null != itemsToPaint && !itemsToPaint.isEmpty()) {
            if (this.autoscale || !Double.isFinite(this.minX) || !Double.isFinite(this.minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)) {
                paintWithAutoScale(itemsToPaint, selectedItem, g2d, w, h);
            } else {
                paintItems(g2d, itemsToPaint, selectedItem, this.minX, this.minY, this.maxX, this.maxY, w, h);
            }
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

    private static Color labelColors[] = new Color[]{
        Color.BLACK,
        Color.MAGENTA.darker(),
        Color.BLUE.darker(),
        Color.RED.darker(),
        Color.GREEN.darker()
    };

    private AprsJFrame aprsJFrame;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    public AprsJFrame getAprsJFrame() {
        return aprsJFrame;
    }

    /**
     * Set the value of aprsJFrame
     *
     * @param aprsJFrame new value of aprsJFrame
     */
    public void setAprsJFrame(AprsJFrame aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
        setSlotOffsetProvider(aprsJFrame);
    }

    private boolean showAddedSlotPositions;

    /**
     * Get the value of showAddedSlotPositions
     *
     * @return the value of showAddedSlotPositions
     */
    public boolean isShowAddedSlotPositions() {
        return showAddedSlotPositions;
    }

    /**
     * Set the value of showAddedSlotPositions
     *
     * @param showAddedSlotPositions new value of showAddedSlotPositions
     */
    public void setShowAddedSlotPositions(boolean showAddedSlotPositions) {
        this.showAddedSlotPositions = showAddedSlotPositions;
        if (showAddedSlotPositions) {
            if (null != items) {
                this.addedSlots = computeAbsSlotPositions(items);
                this.itemsWithAddedSlots = new ArrayList<>();
                this.itemsWithAddedSlots.addAll(items);
                this.itemsWithAddedSlots.addAll(this.addedSlots);
            }
            if (null != outputItems) {
                this.addedOutputSlots = computeAbsSlotPositions(outputItems);
                this.outputItemsWithAddedSlots = new ArrayList<>();
                this.outputItemsWithAddedSlots.addAll(outputItems);
                this.outputItemsWithAddedSlots.addAll(this.addedOutputSlots);
            }
        } else {
            this.addedSlots = null;
            this.addedOutputSlots = null;
            this.outputItemsWithAddedSlots = null;
            this.itemsWithAddedSlots = null;
        }
        repaint();
    }

    public List<PhysicalItem> computeAbsSlotPositions(List<PhysicalItem> l) {
        List<PhysicalItem> absSlotList = new ArrayList<>();
        for (PhysicalItem item : l) {
            if (null != slotOffsetProvider && ("PT".equals(item.getType()) || "KT".equals(item.getType()))) {
                absSlotList.addAll(computeSlotPositions(item));
            }
        }
        return absSlotList;
    }

    private SlotOffsetProvider slotOffsetProvider = null;

    public SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        this.slotOffsetProvider = slotOffsetProvider;
    }

    public List<PhysicalItem> computeSlotPositions(PhysicalItem item) {
        List<Slot> offsets = slotOffsetProvider.getSlotOffsets(item.getName());
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
        double scale;
        Image scaledImage;

        public PartImageInfo(BufferedImage image, double ratio) {
            this.image = image;
            this.ratio = ratio;
            scale = 1.0;
            scaledImage = image.getScaledInstance(image.getWidth(), image.getHeight(), Image.SCALE_DEFAULT);
        }

        public Image getScaledImage(double scale) {
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

    private final Map<String, PartImageInfo> partImageMap;

    public void paintItems(Graphics2D g2d,
            Collection<? extends PhysicalItem> itemsToPaint,
            PhysicalItem selectedItem,
            double minX,
            double minY,
            double maxX,
            double maxY,
            int width,
            int height) {
        origTransform = g2d.getTransform();

        int maxNameLength
                = StreamSupport.stream(itemsToPaint.spliterator(), false)
                        .mapToInt((PhysicalItem item) -> item.getName().length())
                        .max().orElse(1);

        if (!Double.isFinite(maxX) || !Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxY)) {
            throw new IllegalArgumentException("Limits must be finite: (" + minX + "," + minY + "," + maxX + "," + maxY + ")");
        }
        double scale_x = 1;
        double scale_y = 1;
        switch (displayAxis) {
            case POS_X_POS_Y:
            case NEG_X_NEG_Y:
                scale_x = (width / 2 - 30) / (maxX - minX);
                scale_y = (height - 60) / (maxY - minY);
                break;

            case POS_Y_NEG_X:
            case NEG_Y_POS_X:
                scale_x = (width / 2 - 30) / (maxY - minY);
                scale_y = (height - 60) / (maxX - minX);
                break;
        }
//        if (useSeparateNames) {
//            scale_x = scale_x / 2.0;
//        }
        assert (Double.isFinite(scale_x)) :
                ("scale_x = " + scale_x);

        assert (Double.isFinite(scale_y)) :
                ("scale_y = " + scale_y);

        scale = Math.min(scale_x, scale_y);
        if (null == minCorner) {
            minCorner = new Point2D.Double();
        }
        minCorner.x = minX;
        minCorner.y = minY;
        double displayMaxY = maxY;
        double displayMinY = minY;
        double displayMinX = minX;
        double displayMaxX = maxX;

        switch (displayAxis) {
            case POS_X_POS_Y:
                displayMaxX = (width - 15) / scale + minX;
                displayMinX = (0 - 15) / scale + minX;

                displayMinY = maxY - (height - 20) / scale;
                displayMaxY = maxY - (0 - 20) / scale;
//                g2d.translate((itemx - minX) * scale + 15, (maxY - itemy) * scale + 20);
                break;

            case POS_Y_NEG_X:
                displayMaxX = (height - 20) / scale + minX;
                displayMinX = (0 - 15) / scale + minX;

                displayMinY = (width - 15) / scale + minY;
                displayMaxY = (0 - 20) / scale + minY;
//                g2d.translate((itemy - minY) * scale + 15, (itemx - minX) * scale + 20);
                break;

            case NEG_X_NEG_Y:
                displayMaxX = maxX - (width - 15) / scale;
                displayMinX = maxX - (0 - 15) / scale;

                displayMinY = (height - 20) / scale + minY;
                displayMaxY = (0 - 20) / scale + minY;
//                g2d.translate((maxX - itemx) * scale + 15, (itemy - minY) * scale + 20);
                break;

            case NEG_Y_POS_X:
                displayMaxX = maxX - (height - 20) / scale;
                displayMinX = maxX - (0 - 15) / scale;

                displayMinY = (width - 15) / scale + minY;
                displayMaxY = (0 - 20) / scale + minY;
//                g2d.translate((maxY - itemy) * scale + 15, (maxX - itemx) * scale + 20);
                break;
        }
        g2d.drawString(String.format("MinX,MinY = (%.2f,%.2f), MaxX,MaxY= (%.2f,%.2f), scale=%.2f", minX, minY, maxX, maxY, scale), 10, height - 10);
        //        System.out.println("scale = " + scale);
        Collection<? extends PhysicalItem> displayItems = itemsToPaint;
        if (useSeparateNames) {
            displayItems = new ArrayList<>(itemsToPaint);
            switch (displayAxis) {
                case POS_X_POS_Y:
                    Collections.sort((List) displayItems, Comparator.comparing((PhysicalItem item) -> item.y));
                    break;
                case NEG_X_NEG_Y:
                    Collections.sort((List) displayItems, Comparator.comparing((PhysicalItem item) -> -item.y));
                    break;

                case POS_Y_NEG_X:
                    Collections.sort((List) displayItems, Comparator.comparing((PhysicalItem item) -> item.x));
                    break;
                case NEG_Y_POS_X:
                    Collections.sort((List) displayItems, Comparator.comparing((PhysicalItem item) -> -item.x));
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
                paintPartImage(g2d, minX, minY, maxX, maxY, item);
                g2d.setTransform(origTransform);
            }
            for (PhysicalItem item : displayItems) {
                if (null == item) {
                    continue;
                }
                if (!("P".equals(item.getType()))) {
                    continue;
                }
                paintPartImage(g2d, minX, minY, maxX, maxY, item);
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
            if (useSeparateNames) {
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
                this.translate(g2d, namex, namey, minX, minY, maxX, maxY);
                if (item == selectedItem) {
                    g2d.setColor(Color.WHITE);
                    Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.getName().length(), 20);
                    g2d.fill(rect);
                }
                g2d.setColor(item.getLabelColor());
                g2d.drawString(item.getName(), 0, 0);
                g2d.setTransform(origTransform);
                g2d.draw(new Line2D.Double(toScreenPoint(namex, namey, minX, minY, maxX, maxY), toScreenPoint(item.x, item.y, minX, minY, maxX, maxY)));
            }
        }
        g2d.setFont(origFont);

        for (PhysicalItem item : displayItems) {
            if (null == item) {
                continue;
            }
            if (viewRotationsAndImages && null != getPartImageInfo(item)) {
                continue;
            }
            translateThenRotateItem(g2d, minX, minY, maxX, maxY, item);
            item.setDisplayTransform(g2d.getTransform());
            item.setOrigTransform(origTransform);
            try {
                item.setRelTransform(origTransform.createInverse());
                item.getRelTransform().concatenate(item.getDisplayTransform());
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            item.setDisplayRect(new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeparateNames ? 1 : item.getName().length()), 20));
            g2d.setColor(this.getBackground());
            g2d.fill(item.getDisplayRect());
            g2d.setTransform(origTransform);
        }
        i = 0;
        for (PhysicalItem item : displayItems) {
            ++i;
            if (null == item) {
                continue;
            }
            boolean imageShown = viewRotationsAndImages && null != getPartImageInfo(item);
            translateThenRotateItem(g2d, minX, minY, maxX, maxY, item);
            g2d.setColor(Color.BLACK);
            if (!imageShown) {
                if (!useSeparateNames) {
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
            item.setDisplayTransform(g2d.getTransform());
            item.setOrigTransform(origTransform);
            try {
                item.setRelTransform(origTransform.createInverse());
                item.getRelTransform().concatenate(item.getDisplayTransform());
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            item.setDisplayRect(new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeparateNames ? 1 : item.getName().length()), 20));
            if (!imageShown) {
                g2d.setColor(item.getLabelColor());
                g2d.draw(item.getDisplayRect());
            }

            try {
                if (null != slotOffsetProvider && ("PT".equals(item.getType()) || "KT".equals(item.getType()))) {
                    List<Slot> offsets = slotOffsetProvider.getSlotOffsets(item.getName());
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
                        g2d.draw(new Arc2D.Double(-item.getMaxSlotDist() * scale * slotMaxDistExpansion, -item.getMaxSlotDist() * scale * slotMaxDistExpansion, item.getMaxSlotDist() * 2.0 * scale * slotMaxDistExpansion, item.getMaxSlotDist() * 2.0 * scale * slotMaxDistExpansion, 0.0, 360.0, Arc2D.OPEN));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            g2d.setTransform(origTransform);
        }
        g2d.setColor(Color.BLACK);
        if (null != selectedItem) {
            boolean imageShown = viewRotationsAndImages && null != getPartImageInfo(selectedItem);

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
                translateThenRotateItem(g2d, minX, minY, maxX, maxY, selectedItem);
                g2d.setColor(Color.WHITE);
                String typeString = getItemType(selectedItem);
                Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * (useSeparateNames ? typeString.length() : selectedItem.getName().length()), 20);
                g2d.fill(rect);
                g2d.setColor(Color.BLACK);
                g2d.draw(rect);
                if (!useSeparateNames) {
                    g2d.drawString(selectedItem.getName(), 0, 0);
                } else {
                    g2d.drawString(selectedItem.getType(), 0, 0);
                }
            }
            g2d.setTransform(origTransform);
        }
        if (this.showCurrentXY) {
            this.translate(g2d, currentX, currentY, minX, minY, maxX, maxY);
//            g2d.drawString(String.format("CurrentXY = %.2f,%.2f", currentX, currentY), 10, height - 10);
            Color origColor = g2d.getColor();
            g2d.setColor(Color.red);
            g2d.drawLine(-10, 0, 10, 0);
            g2d.drawLine(0, -10, 0, 10);
            g2d.setColor(origColor);
            g2d.setTransform(origTransform);
        }

    }

    private void paintPartImage(Graphics2D g2d, double minX, double minY, double maxX, double maxY, PhysicalItem item) {
        if (!viewRotationsAndImages) {
            return;
        }
        PartImageInfo info = getPartImageInfo(item);
        if (null != info) {

            Image img = info.getScaledImage(scale);
            translateThenRotateItem(g2d, minX, minY, maxX, maxY, item);
            g2d.translate(-(img.getWidth(this) / 2.0), -(img.getHeight(this) / 2.0));
            g2d.drawImage(img, null, null);
            if (viewDetails) {
                g2d.translate(-1, -1);
                g2d.setColor(item.getLabelColor());
                g2d.draw(new Rectangle2D.Double(0, 0, img.getWidth(this) + 2, img.getHeight(this) + 2));
            }
            g2d.setColor(Color.BLACK);

        } else {
            System.out.println("no image for " + item.getName());
        }
    }

    private PartImageInfo getPartImageInfo(PhysicalItem item) {
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
        return info;
    }

    private void translateThenRotateItem(Graphics2D g2d, double minX, double minY, double maxX, double maxY, PhysicalItem item) {
        double itemx = item.x;
        double itemy = item.y;
        switch (displayAxis) {
            case POS_X_POS_Y:
                g2d.translate((itemx - minX) * scale + 15, (maxY - itemy) * scale + 20);
                break;

            case POS_Y_NEG_X:
                g2d.translate((itemy - minY) * scale + 15, (itemx - minX) * scale + 20);
                break;

            case NEG_X_NEG_Y:
                g2d.translate((maxX - itemx) * scale + 15, (itemy - minY) * scale + 20);
                break;

            case NEG_Y_POS_X:
                g2d.translate((maxY - itemy) * scale + 15, (maxX - itemx) * scale + 20);
                break;
        }
        if (viewRotationsAndImages) {
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
            g2d.rotate(-item.getRotation());
        }
    }

    public double slotMaxDistExpansion = 1.5;

    public double getSlotMaxDistExpansion() {
        return slotMaxDistExpansion;
    }

    public void setSlotMaxDistExpansion(double slotMaxDistExpansion) {
        this.slotMaxDistExpansion = slotMaxDistExpansion;
    }

    public double getMaxX() {
        return maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
        this.repaint();
    }

    public double getMinX() {
        return minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
        this.repaint();
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
        this.repaint();
    }

    public double getMinY() {
        return minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
        this.repaint();
    }
}
