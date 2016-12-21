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
package aprs.framework.pddl.executor;

/**
 *
 * @author shackle
 */
public class PositionMapEntry {

    private final double robotX;
    private final double robotY;
    private final double robotZ;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    public PositionMapEntry(double robotX, double robotY, double robotZ, double offsetX, double offsetY, double offsetZ) {
        this.robotX = robotX;
        this.robotY = robotY;
        this.robotZ = robotZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }
    

    public double getRobotX() {
        return robotX;
    }

    public double getRobotY() {
        return robotY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getRobotZ() {
        return robotZ;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    @Override
    public String toString() {
        return "PositionMapEntry{" + "robotX=" + robotX + ", robotY=" + robotY + ", robotZ=" + robotZ + ", offsetX=" + offsetX + ", offsetY=" + offsetY + ", offsetZ=" + offsetZ + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.robotX) ^ (Double.doubleToLongBits(this.robotX) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.robotY) ^ (Double.doubleToLongBits(this.robotY) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.robotZ) ^ (Double.doubleToLongBits(this.robotZ) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.offsetX) ^ (Double.doubleToLongBits(this.offsetX) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.offsetY) ^ (Double.doubleToLongBits(this.offsetY) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.offsetZ) ^ (Double.doubleToLongBits(this.offsetZ) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PositionMapEntry other = (PositionMapEntry) obj;
        if (Double.doubleToLongBits(this.robotX) != Double.doubleToLongBits(other.robotX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.robotY) != Double.doubleToLongBits(other.robotY)) {
            return false;
        }
        if (Double.doubleToLongBits(this.robotZ) != Double.doubleToLongBits(other.robotZ)) {
            return false;
        }
        if (Double.doubleToLongBits(this.offsetX) != Double.doubleToLongBits(other.offsetX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.offsetY) != Double.doubleToLongBits(other.offsetY)) {
            return false;
        }
        if (Double.doubleToLongBits(this.offsetZ) != Double.doubleToLongBits(other.offsetZ)) {
            return false;
        }
        return true;
    }

    

}
