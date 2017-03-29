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

import crcl.base.PoseType;

/**
 *
 * @author zeid
 */
public class Slot {

    private int ID;
    private String SlotName;
    private String PartSKU;
    private String ExternalShapeModelFileName;
    private String ExternalShapeModelFormatName;
    private double X_OFFSET;
    private double Y_OFFSET;
    private Boolean SlotOccupied;
    private PoseType SlotPose;

    public Slot(String SlotName) {
        this.SlotName = SlotName;
    }

    public PoseType getSlotPose() {
        return this.SlotPose;
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

    public String getPartSKU() {
        return PartSKU;
    }

    public void setPartSKU(String PartSKU) {
        this.PartSKU = PartSKU;
    }

//    public String getExternalShapeModelFileName() {
//        return ExternalShapeModelFileName;
//    }
//
//    public void setExternalShapeModelFileName(String ExternalShapeModelFileName) {
//        this.ExternalShapeModelFileName = ExternalShapeModelFileName;
//    }

//    public String getExternalShapeModelFormatName() {
//        return ExternalShapeModelFormatName;
//    }
//
//    public void setExternalShapeModelFormatName(String ExternalShapeModelFormatName) {
//        this.ExternalShapeModelFormatName = ExternalShapeModelFormatName;
//    }

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
        return SlotOccupied;
    }

    public void setSlotOccupied(Boolean SlotOccupied) {
        this.SlotOccupied = SlotOccupied;
    }
}
