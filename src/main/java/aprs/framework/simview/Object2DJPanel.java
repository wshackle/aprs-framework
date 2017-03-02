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

import aprs.framework.database.DetectedItem;
import static aprs.framework.simview.DisplayAxis.POS_X_POS_Y;
import crcl.base.PoseType;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DJPanel extends JPanel {

    private DisplayAxis displayAxis = POS_X_POS_Y;

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
    public static final List<DetectedItem> EXAMPLES_ITEMS_LIST = Arrays.asList(
            // DetectedItem(String name, double rotation, double x, double y, double score, String type)
            new DetectedItem("sku_part_medium_gear", 0.10, 700.45, -140.82, 0.99, "P"),
            new DetectedItem("sku_part_medium_gear", 0.79, 528.60, -122.51, 0.95, "P"),
            new DetectedItem("sku_part_medium_gear", -0.60, 529.98, 213.96, 0.94, "P"),
            new DetectedItem("sku_part_medium_gear", -0.02, 527.61, -205.06, 0.91, "P"),
            new DetectedItem("sku_part_medium_gear", -0.75, 216.66, 128.56, 0.91, "P"),
            new DetectedItem("sku_part_small_gear", 0.53, 509.01, -11.83, 0.95, "P"),
            new DetectedItem("sku_part_small_gear", -0.23, 640.49, 32.88, 0.89, "P"),
            new DetectedItem("sku_part_small_gear", -0.23, 640.49, 32.88, 0.89, "P"),
            new DetectedItem("sku_part_small_gear", -0.07, 310.04, -102.02, 0.65, "P"),
            new DetectedItem("sku_part_small_gear", -0.31, 321.38, 177.59, 0.61, "P"),
            new DetectedItem("sku_kit_s2l2_vessel", -0.02, 295.65, -296.90, 0.80, "KT"),
            new DetectedItem("sku_kit_s2l2_vessel", 0.01, 310.90, -20.87, 0.73, "KT"),
            new DetectedItem("sku_small_gear_vessel", -0.03, 609.22, 5.09, 0.95, "PT"),
            new DetectedItem("sku_medium_gear_vessel", 0.00, 569.17, -161.29, 0.67, "PT"),
            new DetectedItem("sku_kit_m2l1_vessel", -1.57, 579.86, 170.14, 0.96, "KT")
    );
    private List<DetectedItem> items = EXAMPLES_ITEMS_LIST;

    public void setItems(List<DetectedItem> items) {
        this.items = items;
        this.repaint();
    }

    public void takeSnapshot(File f, PoseType pose, String label) throws IOException {
        BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        int pindex = f.getName().lastIndexOf('.');
        String type = "JPEG";
        if (pindex > 0) {
            type = f.getName().substring(pindex + 1);
        }
        Graphics2D g2d = img.createGraphics();
        this.paintComponent(g2d);
        if (null != pose && null != pose.getPoint()) {
            double x = pose.getPoint().getX().doubleValue();
            double y = pose.getPoint().getY().doubleValue();
            this.translate(g2d, x, y);
            g2d.setColor(Color.GREEN);
            Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * label.length(), 20);
            g2d.fill(rect);
            g2d.setColor(Color.BLACK);
            g2d.draw(rect);
            g2d.drawString(label, 0, 0);
        }
        ImageIO.write(img, type, f);
        System.out.println("Saved snapshot to "+f.getCanonicalPath());
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

    private boolean viewRotations;

    /**
     * Get the value of viewRotations
     *
     * @return the value of viewRotations
     */
    public boolean isViewRotations() {
        return viewRotations;
    }

    /**
     * Set the value of viewRotations
     *
     * @param viewRotations new value of viewRotations
     */
    public void setViewRotations(boolean viewRotations) {
        this.viewRotations = viewRotations;
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
    public List<DetectedItem> getItems() {
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

    private void translate(Graphics2D g2d, double itemx, double itemy) {
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        AffineTransform origTransform = g2d.getTransform();
//        double min_x = minX;
//        double min_y = minY;
//        double max_x = maxX;
//        double max_y = maxY;
        if (!Double.isFinite(maxX) || !Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxY)) {
//            for (int i = 0; i < items.size(); i++) {
//                DetectedItem item = items.get(i);
//                if (null == item) {
//                    continue;
//                }
//                if (item.name == null || item.name.length() < 1) {
//                    continue;
//                }
//                if (Double.isInfinite(item.x) || Double.isNaN(item.x)) {
//                    continue;
//                }
//                if (Double.isInfinite(item.y) || Double.isNaN(item.y)) {
//                    continue;
//                }
//                if (Double.isInfinite(item.rotation) || Double.isNaN(item.rotation)) {
//                    continue;
//                }
//                int extra = 10 * item.name.length();
//                if (max_x < item.x + extra) {
//                    max_x = item.x + extra;
//                }
//                if (min_x > item.x - extra) {
//                    min_x = item.x - extra;
//                }
//                if (max_y < item.y + extra) {
//                    max_y = item.y + extra;
//                }
//                if (min_y > item.y - extra) {
//                    min_y = item.y - extra;
//                }
//            }
//            maxX = max_x;
//            minX = min_x;
//            maxY = max_y;
//            minY = min_y;
            g2d.drawString("Limits not set", 0, 0);
            return;
        }
        double scale_x = 1;
        double scale_y = 1;
        switch (displayAxis) {
            case POS_X_POS_Y:
            case NEG_X_NEG_Y:
                scale_x = (this.getSize().width - 30) / (maxX - minX);
                scale_y = (this.getSize().height - 50) / (maxY - minY);
                break;

            case POS_Y_NEG_X:
            case NEG_Y_POS_X:
                scale_x = (this.getSize().width - 30) / (maxY - minY);
                scale_y = (this.getSize().height - 50) / (maxX - minX);
                break;
        }

        if (Double.isInfinite(scale_x) || Double.isNaN(scale_x)) {
            return;
        }
        if (Double.isInfinite(scale_y) || Double.isNaN(scale_y)) {
            return;
        }
        scale = Math.min(scale_x, scale_y);
        if (null == minCorner) {
            minCorner = new Point2D.Double();
        }
        minCorner.x = minX;
        minCorner.y = minY;
//        System.out.println("scale = " + scale);
        for (int i = 0; i < items.size(); i++) {
            DetectedItem item = items.get(i);
            if (null == item) {
                continue;
            }
            if (item.name == null || item.name.length() < 1) {
                continue;
            }
            if (Double.isInfinite(item.x) || Double.isNaN(item.x)) {
                continue;
            }
            if (Double.isInfinite(item.y) || Double.isNaN(item.y)) {
                continue;
            }
            if (Double.isInfinite(item.rotation) || Double.isNaN(item.rotation)) {
                continue;
            }
            this.translate(g2d, item.x, item.y);
            if (viewRotations) {
                g2d.rotate(item.rotation);
            }
            g2d.setColor(Color.BLACK);
            g2d.drawString(item.name, 0, 0);
            item.displayTransform = g2d.getTransform();
            item.origTransform = origTransform;
            try {
                item.relTransform = origTransform.createInverse();
                item.relTransform.concatenate(item.displayTransform);
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            item.displayRect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.name.length(), 20);
            g2d.setColor(Color.BLACK);
            g2d.draw(item.displayRect);
            g2d.setTransform(origTransform);
        }
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.format("Offset = %.2f,%.2f scale=%.2f", minX, minY, scale), 10, this.getSize().height - 10);
        if (selectedItemIndex >= 0 && selectedItemIndex < items.size()) {
            DetectedItem item = items.get(selectedItemIndex);
            if (null == item) {
                return;
            }
            if (item.name == null || item.name.length() < 1) {
                return;
            }
            if (Double.isInfinite(item.x) || Double.isNaN(item.x)) {
                return;
            }
            if (Double.isInfinite(item.y) || Double.isNaN(item.y)) {
                return;
            }
            if (Double.isInfinite(item.rotation) || Double.isNaN(item.rotation)) {
                return;
            }
            this.translate(g2d, item.x, item.y);
            if (viewRotations) {
                g2d.rotate(item.rotation);
            }
            g2d.setColor(Color.WHITE);
            Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.name.length(), 20);
            g2d.fill(rect);
            g2d.setColor(Color.BLACK);
            g2d.draw(rect);
            g2d.drawString(item.name, 0, 0);
            g2d.setTransform(origTransform);
        }
        if (this.showCurrentXY) {
            this.translate(g2d, currentX, currentY);
//            g2d.drawString(String.format("CurrentXY = %.2f,%.2f", currentX, currentY), 10, this.getSize().height - 10);
            Color origColor = g2d.getColor();
            g2d.setColor(Color.red);
            g2d.drawLine(-10, 0, 10, 0);
            g2d.drawLine(0, -10, 0, 10);
            g2d.setColor(origColor);
            g2d.setTransform(origTransform);
        }
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
