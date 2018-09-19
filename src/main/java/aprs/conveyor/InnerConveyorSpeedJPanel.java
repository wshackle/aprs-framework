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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JPanel;
import org.checkerframework.checker.guieffect.qual.UIType;

/**
 *
 * @author shackle
 */
@UIType
public class InnerConveyorSpeedJPanel extends JPanel {

    private double maxPosition = 700;

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

    private double minPosition = 200;

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

    private double estimatedPosition = 450.0;

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

    private int currentSpeed = 100;

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
            int pointpos =  point.x;
            if(pointpos <1) {
                pointpos = 1;
            }
            if(pointpos > dim.width-1) {
                pointpos = dim.width-1;
            }
            return Math.abs(pointpos - dim.width / 2) * maxSpeed / (dim.width / 2);
        } else {
            int pointpos =  point.y;
            if(pointpos <1) {
                pointpos = 1;
            }
            if(pointpos > dim.height-1) {
                pointpos = dim.height-1;
            }
            return Math.abs(pointpos - dim.height / 2) * maxSpeed / (dim.height / 2);
        }
    }
    private Color reverseColor = Color.red;

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = super.getSize();
        if (dim.width < 1 || dim.height < 1) {
            return;
        }

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
            g.setColor(Color.black);

            int xpos = 15 + ((int) ((dim.width - 30) * (estimatedPosition - minPosition) / (maxPosition - minPosition)));
            g.drawLine(xpos, 0, xpos, dim.height);
            g.drawString(String.format("%.0f", estimatedPosition), xpos, dim.height / 2);
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
            g.setColor(Color.black);
            int ypos = 15 + (int) ((dim.height - 30) * (estimatedPosition - minPosition) / (maxPosition - minPosition));
            g.drawLine(0, ypos, dim.width, ypos);
            g.drawString(String.format("%.0f", estimatedPosition), dim.width / 2, ypos);
        }
    }

}
