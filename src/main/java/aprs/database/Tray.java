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
package aprs.database;

import crcl.base.PoseType;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class Tray extends PhysicalItem {

    public Tray(String name) {
        super(name);
    }

    public Tray(String name, double rotation, double x, double y) {
        super(name, rotation, x, y);
    }

    public Tray(String name, double rotation, double x, double y, double score, String type) {
        super(name, rotation, x, y, score, type);
    }

    public Tray(String name, PoseType pose, int visioncycle) {
        super(name, pose, visioncycle);
    }

    private volatile @Nullable
    List<Slot> absSlotList = null;
    private volatile double setAbsSlotListX = java.lang.Double.NaN;
    private volatile double setAbsSlotListY = java.lang.Double.NaN;
    private volatile double setAbsSlotListRotation = java.lang.Double.NaN;

    /**
     * Get the value of absSlotList
     *
     * @return the value of absSlotList
     */
    public List<Slot> getAbsSlotList() {
        List<Slot> ret = this.absSlotList;
        if (null != ret
                && !ret.isEmpty()) {
            if (!java.lang.Double.isFinite(setAbsSlotListX)
                    || !java.lang.Double.isFinite(setAbsSlotListY)
                    || Math.abs(x - setAbsSlotListX) > ABS_SLOT_LIST_CHANGE_TOLERANCE
                    || Math.abs(y - setAbsSlotListY) > ABS_SLOT_LIST_CHANGE_TOLERANCE
                    || Math.abs(this.getRotation() - setAbsSlotListRotation) > ABS_SLOT_LIST_CHANGE_TOLERANCE) {
                ret = Collections.emptyList();
                this.absSlotList = ret;
                return ret;
            }
        }
        if (null == ret) {
            ret = Collections.emptyList();
            this.absSlotList = ret;
        }
        return ret;
    }
    private static final double ABS_SLOT_LIST_CHANGE_TOLERANCE = 2 * java.lang.Double.MIN_NORMAL;

    /**
     * Set the value of absSlotList
     *
     * @param absSlotList new value of absSlotList
     */
    public void setAbsSlotList(List<Slot> absSlotList) {
        this.absSlotList = absSlotList;
        setAbsSlotListX = x;
        setAbsSlotListY = y;
        setAbsSlotListRotation = getRotation();
    }

    public double distFromAbsSlot(PmCartesian cart) {
        if (null == absSlotList) {
            return java.lang.Double.POSITIVE_INFINITY;
        }
        return absSlotList.stream()
                .mapToDouble(cart::distFromXY)
                .min()
                .orElse(java.lang.Double.POSITIVE_INFINITY);
    }

    public @Nullable
    Slot closestAbsSlot(PmCartesian cart) {
        if (null == absSlotList) {
            return null;
        }
        return absSlotList.stream()
                .min(Comparator.comparing(cart::distFromXY))
                .orElse(null);
    }

    public boolean insideAbsSlot(PmCartesian cart, double threshold) {
        Slot slot = closestAbsSlot(cart);
        if (null != slot) {
            return slot.distFrom(cart) < (slot.getDiameter() / 2.0 + threshold);
        }
        return false;
    }

    @Override
    public Tray clone() {
        return (Tray) super.clone();
    }

}
