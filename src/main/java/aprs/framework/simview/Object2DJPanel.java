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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DJPanel extends JPanel {

    public static final List<DetectedItem> EXAMPLES_ITEMS_LIST = Arrays.asList(
            new DetectedItem("part_medium_gear_tray", Math.toRadians(45.0), 250, -250.0),
            new DetectedItem("part_large_gear_tray", Math.toRadians(0.0), 300, -125.0),
            new DetectedItem("part_small_gear_tray", Math.toRadians(-45.0), 400, 125.0),
            new DetectedItem("large_gear_1", Math.toRadians(0.0), 275, -200.0),
            new DetectedItem("large_gear_2", Math.toRadians(0.0), 350, 60.0),
            new DetectedItem("medium_gear_1", Math.toRadians(0.0), 275, -100.0),
            new DetectedItem("medium_gear_2", Math.toRadians(0.0), 375, 85.0),
            new DetectedItem("small_gear_1", Math.toRadians(0.0), 275, -75.0),
            new DetectedItem("small_gear_2", Math.toRadians(0.0), 380, 115.0),
            new DetectedItem("kit_tray_a", Math.toRadians(-45.0), 600, 125.0),
            new DetectedItem("kit_tray_b", Math.toRadians(0.0), 600, 0.0),
            new DetectedItem("kit_tray_c", Math.toRadians(45.0), 650, -200.0)
    );
    private List<DetectedItem> items = EXAMPLES_ITEMS_LIST;

    public void setItems(List<DetectedItem> items) {
        this.items = items;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        AffineTransform origTransform = g2d.getTransform();
        double min_x = minX;
        double min_y = minY;
        double max_x = maxX;
        double max_y = maxY;
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
            g2d.drawString("Limits not set",0,0);
            return;
        }
        double scale_x = (this.getSize().width - 30) / (max_x - min_x);
        double scale_y = (this.getSize().height - 50) / (max_y - min_y);
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
        minCorner.x = min_x;
        minCorner.y = min_y;
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
            g2d.translate((item.x - min_x) * scale + 15, (max_y - item.y) * scale + 20);
            g2d.rotate(item.rotation);
            g2d.drawString(item.name, 0, 0);
            g2d.draw(new Rectangle2D.Double(-5, -12, 10 + 10 * item.name.length(), 20));
            g2d.setTransform(origTransform);
        }
        g2d.drawString(String.format("Offset = %.2f,%.2f scale=%.2f", min_x, min_y, scale), 10, this.getSize().height - 10);
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
            g2d.translate((item.x - min_x) * scale + 15, (max_y - item.y) * scale + 20);
            g2d.rotate(item.rotation);
            g2d.setColor(Color.WHITE);
            Rectangle2D.Double rect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.name.length(), 20);
            g2d.fill(rect);
            g2d.setColor(Color.BLACK);
            g2d.draw(rect);
            g2d.drawString(item.name, 0, 0);
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
