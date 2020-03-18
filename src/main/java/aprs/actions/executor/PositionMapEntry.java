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
package aprs.actions.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;
import rcs.posemath.Posemath;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PositionMapEntry {

    private final double inputX;
    private final double inputY;
    private final double inputZ;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;
    private final String label;
    private final List<PositionMapEntry> combined;

    private PositionMapEntry(double inputX, double inputY, double inputZ, double offsetX, double offsetY, double offsetZ, double minX, double maxX, double minY, double maxY, double minZ, double maxZ, String label, PositionMapEntry... combined) {
        this.inputX = inputX;
        this.inputY = inputY;
        this.inputZ = inputZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.label = label;
        this.combined = new ArrayList<>();
        this.combined.addAll(Arrays.asList(combined));
        for (PositionMapEntry pme : combined) {
            this.combined.addAll(pme.combined);
        }
    }

    /**
     * Get the label.
     *
     * @return label used for display and debug logs
     */
    public String getLabel() {
        return label;
    }

    private PositionMapEntry(double inputX, double inputY, double inputZ, double offsetX, double offsetY, double offsetZ, String label) {
        this.inputX = inputX;
        this.inputY = inputY;
        this.inputZ = inputZ;
        this.maxX = this.minX = inputX;
        this.maxY = this.minY = inputY;
        this.maxZ = this.minZ = inputZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.label = label;
        combined = Collections.emptyList();
    }

    /**
     * Create a new entry given and input and offset with empty label.
     *
     * @param inputX
     * @param inputY
     * @param inputZ
     * @param offsetX
     * @param offsetY
     * @param offsetZ
     * @return newly created entry
     */
    public static PositionMapEntry pointOffsetEntry(
            double inputX, double inputY, double inputZ,
            double offsetX, double offsetY, double offsetZ) {
        return new PositionMapEntry(inputX, inputY, inputZ, offsetX, offsetY, offsetZ, "");
    }

    /**
     * Create a new entry given and input and offset with provided label.
     * 
     * @param inputX
     * @param inputY
     * @param inputZ
     * @param offsetX
     * @param offsetY
     * @param offsetZ
     * @param label label used for display and debug logging
     * @return
     */
    public static PositionMapEntry pointOffsetLabelEntry(
                double inputX, double inputY, double inputZ,
                double offsetX, double offsetY, double offsetZ, String label) {
        return new PositionMapEntry(inputX, inputY, inputZ, offsetX, offsetY, offsetZ, label);
    }

    /**
     * Create a new entry given and input and offset with empty label.
     * @param inputX
     * @param inputY
     * @param inputZ
     * @param offsetX
     * @param offsetY
     * @param offsetZ
     * @param pme1
     * @param pme2
     * @return
     */
    public static PositionMapEntry pointOffsetEntryCombining(
            double inputX, double inputY, double inputZ, 
            double offsetX, double offsetY, double offsetZ, 
            PositionMapEntry pme1, PositionMapEntry pme2) {
        return new PositionMapEntry(inputX, inputY, inputZ, offsetX, offsetY, offsetZ,
                Math.min(pme1.minX, pme2.minX),
                Math.max(pme1.maxX, pme2.maxX),
                Math.min(pme1.minY, pme2.minY),
                Math.max(pme1.maxY, pme2.maxY),
                Math.min(pme1.minZ, pme2.minZ),
                Math.max(pme1.maxZ, pme2.maxZ),
                "",
                pme1, pme2
        );
    }

    
    public static PositionMapEntry averageTwoEntrees(PositionMapEntry e1, PositionMapEntry e2) {
        return PositionMapEntry.pointOffsetEntryCombining(
                (e1.getInputX() + e2.getInputX()) / 2.0,
                (e1.getInputY() + e2.getInputY()) / 2.0,
                (e1.getInputZ() + e2.getInputZ()) / 2.0,
                (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0,
                e1, e2);
    }
    
    public static PositionMapEntry pointOffsetLabelEntryCombining(double inputX, double inputY, double inputZ, double offsetX, double offsetY, double offsetZ, String label,
            PositionMapEntry pme1, PositionMapEntry pme2) {
        return new PositionMapEntry(inputX, inputY, inputZ, offsetX, offsetY, offsetZ,
                Math.min(pme1.minX, pme2.minX),
                Math.max(pme1.maxX, pme2.maxX),
                Math.min(pme1.minY, pme2.minY),
                Math.max(pme1.maxY, pme2.maxY),
                Math.min(pme1.minZ, pme2.minZ),
                Math.max(pme1.maxZ, pme2.maxZ),
                label,
                pme1, pme2
        );
    }
    
    
    static public PositionMapEntry combine(PositionMapEntry e1, PositionMapEntry e2, double x, double y, double z) {
        if (null == e1) {
            return e2;
        }
        if (null == e2) {
            return e1;
        }
        PmCartesian c1 = new PmCartesian(e1.getInputX(), e1.getInputY(), e1.getInputZ());
        PmCartesian c2 = new PmCartesian(e2.getInputX(), e2.getInputY(), e2.getInputZ());
        PmCartesian diff = c2.subtract(c1);
        if (diff.mag() < 1e-6) {
            return averageTwoEntrees(e1, e2);
        }
        PmCartesian xy = new PmCartesian(x, y, z);
        PmCartesian diff2 = xy.subtract(c1);
        double d = Posemath.pmCartCartDot(diff, diff2) / (diff.mag() * diff.mag());
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return PositionMapEntry.pointOffsetEntryCombining(
                center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1,
                e1, e2
        );
    }

    public static @Nullable
    PositionMapEntry combineX(
            @Nullable PositionMapEntry e1,
            @Nullable PositionMapEntry e2,
            double x) {
        if (null == e1) {
            if (null != e2 && Math.abs(e2.getInputX() - x) < 1e-6) {
                return e2;
            } else {
                return null;
            }
        }
        if (null == e2) {
            if (Math.abs(e1.getInputX() - x) < 1e-6) {
                return e1;
            } else {
                return null;
            }
        }
        PmCartesian c1 = new PmCartesian(e1.getInputX(), e1.getInputY(), e1.getInputZ());
        PmCartesian c2 = new PmCartesian(e2.getInputX(), e2.getInputY(), e2.getInputZ());
        PmCartesian diff = c2.subtract(c1);
        if (Math.abs(diff.x) < 1e-6) {
            if (Math.abs(e1.getInputX() - x) > 1e-6) {
                return null;
            }
            return averageTwoEntrees(e1, e2);
        }

        double d = (x - c1.x) / diff.x;
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return PositionMapEntry.pointOffsetEntryCombining(center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1,
                e1, e2
        );
    }

    public static @Nullable
    PositionMapEntry combineY(PositionMapEntry e1, PositionMapEntry e2, double y) {
        if (null == e1) {
            if (null != e2 && Math.abs(e2.getInputY() - y) < 1e-6) {
                return e2;
            } else {
                return null;
            }
        }
        if (null == e2) {
            if (Math.abs(e1.getInputY() - y) < 1e-6) {
                return e1;
            } else {
                return null;
            }
        }
        PmCartesian c1 = new PmCartesian(e1.getInputX(), e1.getInputY(), e1.getInputZ());
        PmCartesian c2 = new PmCartesian(e2.getInputX(), e2.getInputY(), e2.getInputZ());
        PmCartesian diff = c2.subtract(c1);
        if (Math.abs(diff.y) < 1e-6) {
            if (Math.abs(e1.getInputY() - y) > 1e-6) {
                return null;
            }
            return averageTwoEntrees(e1, e2);
        }

        double d = (y - c1.y) / diff.y;
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return PositionMapEntry.pointOffsetEntryCombining(center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1,
                e1, e2
        );
    }

    public static PositionMapEntry pointPairEntry(double inputX, double inputY, double inputZ, double otherX, double otherY, double otherZ) {
        return new PositionMapEntry(inputX, inputY, inputZ, otherX - inputX, otherY - inputY, otherZ - inputZ, "");
    }

    public static PositionMapEntry pointPairLabelEntry(double inputX, double inputY, double inputZ, double otherX, double otherY, double otherZ, String label) {
        return new PositionMapEntry(inputX, inputY, inputZ, otherX - inputX, otherY - inputY, otherZ - inputZ, label);
    }

    public static PositionMapEntry cartPairEntry(PmCartesian inputCart, PmCartesian otherCart) {
        return pointPairEntry(inputCart.x, inputCart.y, inputCart.z, otherCart.x, otherCart.y, otherCart.z);
    }

    public double getInputX() {
        return inputX;
    }

    public double getInputY() {
        return inputY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getInputZ() {
        return inputZ;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public double getOutputX() {
        return inputX + offsetX;
    }

    public double getOutputY() {
        return inputY + offsetY;
    }

    public double getOutputZ() {
        return inputZ + offsetZ;
    }

    @Override
    public String toString() {
        return "PositionMapEntry{" + "input(X,Y,Z)=" + inputX + "," + inputY + "," + inputZ + ", other(X,Y,Z)=" + getOutputX() + "," + getOutputY() + ", " + getOutputZ() + ", offset(X,Y,Z)=" + offsetX + "," + offsetY + "," + offsetZ + ",label=" + label + '}';
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxZ() {
        return maxZ;
    }

//    public List<PositionMapEntry> getCombined() {
//        return combined;
//    }

}
