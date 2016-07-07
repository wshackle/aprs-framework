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

    private List<DetectedItem> items = Arrays.asList(
            new DetectedItem("A", Math.toRadians(45.0), 10.0, 50.0),
            new DetectedItem("B", Math.toRadians(0.0), 50.0, 150.0),
            new DetectedItem("C", Math.toRadians(0.0), 20.0, 70.0)
    );

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        AffineTransform origTransform = g2d.getTransform();
        double min_x = Double.POSITIVE_INFINITY;
        double min_y = Double.POSITIVE_INFINITY;
        double max_x = Double.NEGATIVE_INFINITY;
        double max_y = Double.NEGATIVE_INFINITY;
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
            if (max_x < item.x) {
                max_x = item.x;
            }
            if (min_x > item.x) {
                min_x = item.x;
            }
            if (max_y < item.y) {
                max_y = item.y;
            }
            if (min_y > item.y) {
                min_y = item.y;
            }
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
         if(null == minCorner) {
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
            g2d.translate((item.x - min_x) * scale + 15, (item.y - min_y) * scale + 20);
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
            g2d.translate((item.x - min_x) * scale + 15, (item.y - min_y) * scale + 20);
            g2d.rotate(item.rotation);
            g2d.setColor(Color.WHITE);
            Rectangle2D.Double  rect = new Rectangle2D.Double(-5, -12, 10 + 10 * item.name.length(), 20);
            g2d.fill(rect);
            g2d.setColor(Color.BLACK);
            g2d.draw(rect);
            g2d.drawString(item.name, 0, 0);
            g2d.setTransform(origTransform);
        }
    }
}
