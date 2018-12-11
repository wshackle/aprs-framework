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
package aprs.conveyor;

import aprs.database.PhysicalItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@UIType
public class InnerConveyorSpeedJPanel extends JPanel {

    private double goalPosition = 450.0;

    /**
     * Get the value of goalPosition
     *
     * @return the value of goalPosition
     */
    public double getGoalPosition() {
        return goalPosition;
    }

    /**
     * Set the value of goalPosition
     *
     * @param goalPosition new value of goalPosition
     */
    public void setGoalPosition(double goalPosition) {
        if(goalPosition < minPosition) {
            this.goalPosition = minPosition;
        } else if(goalPosition > maxPosition) {
            this.goalPosition = maxPosition;
        } else {
            this.goalPosition = goalPosition;
        }
    }

    private double maxPosition = 800;

    /**
     * Get the value of maxPosition
     *
     * @return the value of maxPosition
     */
    public double getMaxPosition() {
        return maxPosition;
    }

    /**
     * Set the value of maxPosition
     *
     * @param maxPosition new value of maxPosition
     */
    public void setMaxPosition(double maxPosition) {
        this.maxPosition = maxPosition;
    }

    private double minPosition = 100;

    /**
     * Get the value of minPosition
     *
     * @return the value of minPosition
     */
    public double getMinPosition() {
        return minPosition;
    }

    /**
     * Set the value of minPosition
     *
     * @param minPosition new value of minPosition
     */
    public void setMinPosition(double minPosition) {
        this.minPosition = minPosition;
    }

    private double scale = 0.00001;

    /**
     * Get the value of scale
     *
     * @return the value of scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Set the value of scale
     *
     * @param scale new value of scale
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    private volatile double estimatedPosition = 450.0;

    /**
     * Get the value of estimatedPosition
     *
     * @return the value of estimatedPosition
     */
    public double getEstimatedPosition() {
        return estimatedPosition;
    }

    /**
     * Set the value of estimatedPosition
     *
     * @param estimatedPosition new value of estimatedPosition
     */
    public void setEstimatedPosition(double estimatedPosition) {
        this.estimatedPosition = estimatedPosition;
        this.repaint();
    }

    private boolean horizontal;

    private volatile List<PhysicalItem> items = Collections.emptyList();

    /**
     * Get the value of items
     *
     * @return the value of items
     */
    public List<PhysicalItem> getItems() {
        return items;
    }

    private volatile List<PhysicalItem> sortedKitTrays = Collections.emptyList();

    /**
     * Get the value of sortedKitTrays
     *
     * @return the value of sortedKitTrays
     */
    public List<PhysicalItem> getSortedKitTrays() {
        return sortedKitTrays;
    }

    /**
     * Set the value of items
     *
     * @param items new value of items
     */
    public void setItems(List<PhysicalItem> items) {
        this.items = items;
        List<PhysicalItem> newSortedTrays = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            PhysicalItem item = items.get(i);
            if (item == null) {
                continue;
            }
            String type = item.getType();
            if ("KT".equals(type)) {
                newSortedTrays.add(item);
            }
        }
        Collections.sort(newSortedTrays, Comparators.byDoubleFunction(this::positionOfItem));
        this.sortedKitTrays = newSortedTrays;
        this.repaint();
    }

    /**
     * Get the value of horizontal
     *
     * @return the value of horizontal
     */
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * Set the value of horizontal
     *
     * @param horizontal new value of horizontal
     */
    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    private boolean forwardDirection;

    /**
     * Get the value of forwardDirection
     *
     * @return the value of forwardDirection
     */
    public boolean isForwardDirection() {
        return forwardDirection;
    }

    /**
     * Set the value of forwardDirection
     *
     * @param forwardDirection new value of forwardDirection
     */
    public void setForwardDirection(boolean forwardDirection) {
        this.forwardDirection = forwardDirection;
    }

    private int maxSpeed = 5000;

    /**
     * Get the value of maxSpeed
     *
     * @return the value of maxSpeed
     */
    public int getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Set the value of maxSpeed
     *
     * @param maxSpeed new value of maxSpeed
     */
    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    private volatile int currentSpeed = 100;

    /**
     * Get the value of currentSpeed
     *
     * @return the value of currentSpeed
     */
    public int getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * Set the value of currentSpeed
     *
     * @param currentSpeed new value of currentSpeed
     */
    public void setCurrentSpeed(int currentSpeed) {
        if (currentSpeed < 0) {
            throw new IllegalArgumentException("currentSpeed must be non-negative: set forwardDirection = false for reverse.");
        }
        if (currentSpeed > maxSpeed) {
            throw new IllegalArgumentException("currentSpeed = " + currentSpeed + ", maxSpeed=" + maxSpeed);
        }
        if (this.currentSpeed != currentSpeed) {
            this.currentSpeed = currentSpeed;
            this.repaint();
        }
    }

    private Color forwardColor = Color.blue;

    /**
     * Get the value of forwardColor
     *
     * @return the value of forwardColor
     */
    public Color getForwardColor() {
        return forwardColor;
    }

    /**
     * Set the value of forwardColor
     *
     * @param forwardColor new value of forwardColor
     */
    public void setForwardColor(Color forwardColor) {
        this.forwardColor = forwardColor;
    }

    public boolean isPointForward(Point point) {
        Dimension dim = super.getSize();
        if (dim.width < 1 || dim.height < 1) {
            throw new IllegalStateException("bad dimensions " + dim);
        }
        if (horizontal) {
            return point.x > dim.width / 2;
        } else {
            return point.y > dim.height / 2;
        }
    }

    public int pointToSpeed(Point point) {
        Dimension dim = super.getSize();
        if (dim.width < 1 || dim.height < 1) {
            throw new IllegalStateException("bad dimensions " + dim);
        }

        if (horizontal) {
            int pointpos = point.x;
            if (pointpos < 1) {
                pointpos = 1;
            }
            if (pointpos > dim.width - 1) {
                pointpos = dim.width - 1;
            }
            return Math.abs(pointpos - dim.width / 2) * maxSpeed / (dim.width / 2);
        } else {
            int pointpos = point.y;
            if (pointpos < 1) {
                pointpos = 1;
            }
            if (pointpos > dim.height - 1) {
                pointpos = dim.height - 1;
            }
            return Math.abs(pointpos - dim.height / 2) * maxSpeed / (dim.height / 2);
        }
    }
    private Color reverseColor = Color.red;

    private boolean goalSet;

    /**
     * Get the value of goalSet
     *
     * @return the value of goalSet
     */
    public boolean isGoalSet() {
        return goalSet;
    }

    /**
     * Set the value of goalSet
     *
     * @param goalSet new value of goalSet
     */
    public void setGoalSet(boolean goalSet) {
        this.goalSet = goalSet;
    }

    /**
     * Get the value of reverseColor
     *
     * @return the value of reverseColor
     */
    public Color getReverseColor() {
        return reverseColor;
    }

    /**
     * Set the value of reverseColor
     *
     * @param reverseColor new value of reverseColor
     */
    public void setReverseColor(Color reverseColor) {
        this.reverseColor = reverseColor;
    }

    private double axisX = 0.0;
    private double axisY = -1.0;

    public double getAxisX() {
        return axisX;
    }

    public void setAxisX(double axisX) {
        this.axisX = axisX;
    }

    public double getAxisY() {
        return axisY;
    }

    public void setAxisY(double axisY) {
        this.axisY = axisY;
    }

    private double itemPostionOffset = 450;

    /**
     * Get the value of itemPostionOffset
     *
     * @return the value of itemPostionOffset
     */
    public double getItemPostionOffset() {
        return itemPostionOffset;
    }

    /**
     * Set the value of itemPostionOffset
     *
     * @param itemPostionOffset new value of itemPostionOffset
     */
    public void setItemPostionOffset(double itemPostionOffset) {
        this.itemPostionOffset = itemPostionOffset;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = super.getSize();
        if (dim.width < 1 || dim.height < 1) {
            return;
        }

        paintSpeedBar(dim, g);
        List<PhysicalItem> itemsToPaint = this.items;
        if (null != itemsToPaint) {
            for (int i = 0; i < itemsToPaint.size(); i++) {
                PhysicalItem item = itemsToPaint.get(i);
                if (item == null) {
                    continue;
                }
                String type = item.getType();
                if ("PT".equals(type) || "KT".equals(type)) {
                    double position = positionOfItem(item);
                    paintPositionMarke(g, Color.LIGHT_GRAY, dim, position, item.getName());
                }
            }
        }
        if(this.estimatedPosition < this.minPosition) {
             paintPositionMarke(g, Color.BLACK, dim, this.minPosition, "min");
        }
        if(this.estimatedPosition > this.maxPosition) {
             paintPositionMarke(g, Color.BLACK, dim, this.maxPosition, "max");
        }
         if (goalSet) {
            paintPositionMarke(g, Color.GREEN, dim, this.goalPosition, "goal");
        }
        paintPositionMarke(g, Color.BLACK, dim, this.estimatedPosition, "current");
       
        
    }

    public double positionOfItem(PhysicalItem item) {
        return axisX * item.x + axisY * item.y + itemPostionOffset;
    }

    private void paintPositionMarke(Graphics g, Color lineColor, Dimension dim, double pos, String label) {
        g.setColor(lineColor);
        double min = Math.min(estimatedPosition, minPosition);
        double max = Math.max(estimatedPosition, maxPosition);
        double diff = (max-min);
        if (horizontal) {
            int xpos = 15 + ((int) ((dim.width - 30) * (pos - min) / diff));
            g.drawLine(xpos, 0, xpos, dim.height);
            g.drawString(String.format("%.0f %s", pos, label), xpos, dim.height / 2);
        } else {
            int ypos = 15 + (int) ((dim.height - 30) * (pos - min) / diff);
            g.drawLine(0, ypos, dim.width, ypos);
            g.drawString(String.format("%.0f %s", pos, label), dim.width / 2, ypos);
        }
    }

    private void paintSpeedBar(Dimension dim, Graphics g) {
        if (horizontal) {
            if (currentSpeed > 0) {
                int barWidth = dim.width * currentSpeed / (2 * maxSpeed);
                if (forwardDirection) {
                    g.setColor(forwardColor);
                    g.fillRect(dim.width / 2, 0, barWidth, dim.height);
                } else {
                    g.setColor(reverseColor);
                    g.fillRect(dim.width / 2 - barWidth, 0, barWidth, dim.height);
                }
            }
        } else {
            if (currentSpeed > 0) {
                int barHeight = dim.height * currentSpeed / (2 * maxSpeed);
                if (forwardDirection) {
                    g.setColor(forwardColor);
                    g.fillRect(0, dim.height / 2, dim.width, barHeight);
                } else {
                    g.setColor(reverseColor);
                    g.fillRect(0, dim.height / 2 - barHeight, dim.width, barHeight);
                }
            }
        }
    }

}
