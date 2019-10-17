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
import rcs.posemath.PmCartesian;

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

    private PositionMapEntry(double inputX, double inputY, double inputZ, double offsetX, double offsetY, double offsetZ, double minX, double maxX, double minY, double maxY, double minZ, double maxZ, String label, PositionMapEntry ... combined) {
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
        for(PositionMapEntry pme: combined) {
            this.combined.addAll(pme.combined);
        }
    }

    public String getLabel() {
        return label;
    }

    
    private PositionMapEntry(double robotX, double robotY, double robotZ, double offsetX, double offsetY, double offsetZ, String label) {
        this.inputX = robotX;
        this.inputY = robotY;
        this.inputZ = robotZ;
        this.maxX = this.minX = robotX;
        this.maxY = this.minY = robotY;
        this.maxZ = this.minZ = robotZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.label = label;
        combined = Collections.emptyList();
    }

    public static PositionMapEntry pointOffsetEntry(double robotX, double robotY, double robotZ, double offsetX, double offsetY, double offsetZ) {
        return new PositionMapEntry(robotX, robotY, robotZ, offsetX, offsetY, offsetZ,"");
    }

    public static PositionMapEntry pointOffsetLabelEntry(double robotX, double robotY, double robotZ, double offsetX, double offsetY, double offsetZ,String label) {
        return new PositionMapEntry(robotX, robotY, robotZ, offsetX, offsetY, offsetZ,label);
    }

    public static PositionMapEntry pointOffsetEntryCombining(double robotX, double robotY, double robotZ, double offsetX, double offsetY, double offsetZ, PositionMapEntry pme1, PositionMapEntry pme2) {
        return new PositionMapEntry(robotX, robotY, robotZ, offsetX, offsetY, offsetZ,
                Math.min(pme1.minX, pme2.minX),
                Math.max(pme1.maxX, pme2.maxX),
                Math.min(pme1.minY, pme2.minY),
                Math.max(pme1.maxY, pme2.maxY),
                Math.min(pme1.minZ, pme2.minZ),
                Math.max(pme1.maxZ, pme2.maxZ),
                "",
                pme1,pme2
        );
    }

    public static PositionMapEntry pointOffsetLabelEntryCombining(double robotX, double robotY, double robotZ, double offsetX, double offsetY, double offsetZ, String label,
            PositionMapEntry pme1, PositionMapEntry pme2) {
        return new PositionMapEntry(robotX, robotY, robotZ, offsetX, offsetY, offsetZ,
                Math.min(pme1.minX, pme2.minX),
                Math.max(pme1.maxX, pme2.maxX),
                Math.min(pme1.minY, pme2.minY),
                Math.max(pme1.maxY, pme2.maxY),
                Math.min(pme1.minZ, pme2.minZ),
                Math.max(pme1.maxZ, pme2.maxZ),
                label,
                pme1,pme2
        );
    }

    public static PositionMapEntry pointPairEntry(double robotX, double robotY, double robotZ, double otherX, double otherY, double otherZ) {
        return new PositionMapEntry(robotX, robotY, robotZ, otherX - robotX, otherY - robotY, otherZ - robotZ,"");
    }

    public static PositionMapEntry pointPairLabelEntry(double robotX, double robotY, double robotZ, double otherX, double otherY, double otherZ, String label) {
        return new PositionMapEntry(robotX, robotY, robotZ, otherX - robotX, otherY - robotY, otherZ - robotZ,label);
    }
    
    public static PositionMapEntry cartPairEntry(PmCartesian robotCart, PmCartesian otherCart) {
        return pointPairEntry(robotCart.x, robotCart.y, robotCart.z, otherCart.x, otherCart.y, otherCart.z);
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
        return "PositionMapEntry{" + "robot(X,Y,Z)=" + inputX + "," + inputY + "," + inputZ + ", other(X,Y,Z)=" + getOutputX() + "," + getOutputY() + ", " + getOutputZ() + ", offset(X,Y,Z)=" + offsetX + "," + offsetY + "," + offsetZ + ",label="+label+'}';
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

    public List<PositionMapEntry> getCombined() {
        return combined;
    }
    
    

}
