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
package aprs.framework.database;

import crcl.base.PoseType;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 *
 * @author zeid
 */
public class Slot extends PhysicalItem {

    private int ID;
    private String SlotName;
    private @MonotonicNonNull String PartSKU;
    private @Nullable String ExternalShapeModelFileName;
    private @Nullable String ExternalShapeModelFormatName;
    private double X_OFFSET;
    private double Y_OFFSET;
    private @Nullable Boolean SlotOccupied;
    private @Nullable PoseType SlotPose;
    private double diameter;

    private @Nullable String slotIndexString;

    /**
     * Get the value of slotIndexString
     *
     * @return the value of slotIndexString
     */
    public @Nullable String getSlotIndexString() {
        return slotIndexString;
    }

    /**
     * Set the value of slotIndexString
     *
     * @param slotIndexString new value of slotIndexString
     */
    public void setSlotIndexString(String slotIndexString) {
        this.slotIndexString = slotIndexString;
    }

    public static Slot slotFromTrayPartNameIndexRotationXY(Tray tray,String partName,int index, double rotation, double x, double y) {
        String slotName = "slot_"+index+"_for_"+partName;
        Slot slot = new Slot(partName,rotation,x,y);
        slot.setIndex(index);
        slot.setSlotIndexString(""+index);
        slot.setSlotForSkuName(partName);
        slot.setFullName(slotName);
        slot.setPrpName(slotName);
        slot.setTray(tray);
        return slot;
    }
    
    public Slot(String SlotName) {
        super(SlotName);
        this.SlotName = SlotName;
    }

    public Slot(String name, double rotation, double x, double y) {
        super(name, rotation, x, y);
        X_OFFSET = x;
        Y_OFFSET = y;
        this.SlotName = name;
    }

    public Slot(String name, double rotation, double x, double y, double score, String type) {
        super(name, rotation, x, y, score, type);
        X_OFFSET = x;
        Y_OFFSET = y;
        this.SlotName = name;
    }

    public Slot(String name, PoseType pose, int visioncycle) {
        super(name, pose, visioncycle);
        X_OFFSET = x;
        Y_OFFSET = y;
        this.SlotName = name;
    }

    public PoseType getSlotPose() {
        if(null != SlotPose) {
            return SlotPose;
        }
        throw new NullPointerException("SlotPose is null in Slot with name="+getName());
    }

    public void setSlotPose(PoseType SlotPose) {
        this.SlotPose = SlotPose;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getSlotName() {
        return SlotName;
    }

    public void setSlotName(String SlotName) {
        this.SlotName = SlotName;
    }

    @Nullable public String getPartSKU() {
            return PartSKU;
    }
    
    @EnsuresNonNull("this.PartSKU")
    public void setPartSKU(String PartSKU) {
        this.PartSKU = PartSKU;
    }

    public double getX_OFFSET() {
        return X_OFFSET;
    }

    public void setX_OFFSET(double X_OFFSET) {
        this.X_OFFSET = X_OFFSET;
    }

    public double getY_OFFSET() {
        return Y_OFFSET;
    }

    public void setY_OFFSET(double Y_OFFSET) {
        this.Y_OFFSET = Y_OFFSET;
    }

    public Boolean getSlotOccupied() {
        if(null != SlotOccupied) {
            return SlotOccupied;
        }
        throw new NullPointerException("SlotOccupied is null in Slot with name="+getName());
    }
    

    public void setSlotOccupied(Boolean SlotOccupied) {
        this.SlotOccupied = SlotOccupied;
    }

    public double getDiameter() {
        return diameter;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    @Override
    public Slot clone() {
        return (Slot) super.clone();
    }

}
