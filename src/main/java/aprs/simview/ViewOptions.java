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

import aprs.misc.SlotOffsetProvider;
import aprs.system.AprsSystem;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class ViewOptions {

    public boolean disableLimitsLine;
    public boolean disableLabels;
    public boolean enableAutoscale;
    public boolean disableShowCurrent;
    public boolean overrideRotationOffset;
    public boolean disableSensorLimitsRect;
    public boolean disableRobotsReachLimitsRect;

    public boolean addExtras;
    public boolean debug;
    public int w;
    public int h;
    public double rotationOffset;
    boolean scale_set;
    double scale;
    boolean paintingComponent;
    boolean useSeparateNames;
    Color backgroundColor;
    Color foregroundColor;
    boolean showCurrentXY;
    double currentX;
    double currentY;
    Point2DMinMax minmax;
    boolean showOverlapping;
    boolean showOnlyOverlapping;
    DisplayAxis displayAxis;
    Point mousePoint;
    boolean mouseInside;
    Point2D.Double worldMousePoint;
    boolean viewRotationsAndImages;
    double alternativeRotation;
    boolean viewDetails;
    AprsSystem aprsSystem;
    SlotOffsetProvider slotOffsetProvider;
    double slotMaxDistExpansion;
    Point2D.@Nullable Double capturedPartPoint;
    boolean endEffectorClosed;
    double senseMinX;
    double senseMaxX;
    double senseMinY;
    double senseMaxY;

    @Override
    public String toString() {
        return "ViewOptions{" + "disableLimitsLine=" + disableLimitsLine + ", disableLabels=" + disableLabels + ", enableAutoscale=" + enableAutoscale + ", disableShowCurrent=" + disableShowCurrent + ", overrideRotationOffset=" + overrideRotationOffset + ", disableSensorLimitsRect=" + disableSensorLimitsRect + ", disableRobotsReachLimitsRect=" + disableRobotsReachLimitsRect + ", addExtras=" + addExtras + ", debug=" + debug + ", w=" + w + ", h=" + h + ", rotationOffset=" + rotationOffset + ", scale_set=" + scale_set + ", scale=" + scale + ", paintingComponent=" + paintingComponent + ", useSeparateNames=" + useSeparateNames + ", backgroundColor=" + backgroundColor + ", foregroundColor=" + foregroundColor + ", showCurrentXY=" + showCurrentXY + ", currentX=" + currentX + ", currentY=" + currentY + ", minmax=" + minmax + ", showOverlapping=" + showOverlapping + ", showOnlyOverlapping=" + showOnlyOverlapping + ", displayAxis=" + displayAxis + ", mousePoint=" + mousePoint + ", mouseInside=" + mouseInside + ", worldMousePoint=" + worldMousePoint + ", viewRotationsAndImages=" + viewRotationsAndImages + ", alternativeRotation=" + alternativeRotation + ", viewDetails=" + viewDetails + ", aprsSystem=" + aprsSystem + ", slotOffsetProvider=" + slotOffsetProvider + ", slotMaxDistExpansion=" + slotMaxDistExpansion + ", capturedPartPoint=" + capturedPartPoint + ", endEffectorClosed=" + endEffectorClosed + ", senseMinX=" + senseMinX + ", senseMaxX=" + senseMaxX + ", senseMinY=" + senseMinY + ", senseMaxY=" + senseMaxY + '}';
    }
    
    
}
