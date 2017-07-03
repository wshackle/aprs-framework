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
import java.util.List;

/**
 *
 * @author zeid
 */
public class PartsTray extends Tray {

    private int NodeID;
    private String PartsTraySku;
    private String PartsTrayName;
    private String ExternalShapeModelFileName;
    private String ExternalShapeModelFormatName;
    private String PartsTrayDesign;
    private Boolean PartsTrayComplete;
//    private double Rotation;  now in super class PhysicalItem
    private List<Slot> SlotList;
//    private double x; now in super class PhysicalItem
//    private double y; now in super class PhysicalItem
    private PoseType PartsTrayPose;

    public PartsTray(String PartsTrayName) {
        super(PartsTrayName);
        this.PartsTrayName = PartsTrayName;
    }

    public PartsTray(String name, double rotation, double x, double y) {
        super(name, rotation, x, y);
    }

    public PartsTray(String name, double rotation, double x, double y, double score, String type) {
        super(name, rotation, x, y, score, type);
    }

    public PartsTray(String name, PoseType pose, int visioncycle) {
        super(name, pose, visioncycle);
    }
    
    
    
//    These are now inherited from super class PhysicalItem
//    public double getX() {
//        return x;
//    }
//
//    public void setX(double x) {
//        this.x = x;
//    }
//
//    public double getY() {
//        return y;
//    }
//
//    public void setY(double y) {
//        this.y = y;
//    }
//
//    public double getRotation() {
//        return Rotation;
//    }
//
//    public void setRotation(double Rotation) {
//        this.Rotation = Rotation;
//    }

    public String getPartsTrayName() {
        return PartsTrayName;
    }

    public int getNodeID() {
        return NodeID;
    }

    public void setNodeID(int NodeID) {
        this.NodeID = NodeID;
    }

    public String getPartsTrayDesign() {
        return PartsTrayDesign;
    }

    public void setPartsTrayDesign(String PartsTrayDesign) {
        this.PartsTrayDesign = PartsTrayDesign;
    }

    public String getPartsTraySku() {
        return PartsTraySku;
    }

    public void setPartsTraySku(String PartsTraySku) {
        this.PartsTraySku = PartsTraySku;
    }
//
//    public String getExternalShapeModelFileName() {
//        return this.ExternalShapeModelFileName;
//    }
//
//    public void setExternalShapeModelFileName(String ExternalShapeModelFileName) {
//        this.ExternalShapeModelFileName = ExternalShapeModelFileName;
//    }
//
//    public String getExternalShapeModelFormatName() {
//        return this.ExternalShapeModelFormatName;
//    }
//
//    public void setExternalShapeModelFormatName(String ExternalShapeModelFormatName) {
//        this.ExternalShapeModelFormatName = ExternalShapeModelFormatName;
//    }

    public Boolean getPartsTrayComplete() {
        return PartsTrayComplete;
    }

    public void setPartsTrayComplete(Boolean PartsTrayComplete) {
        this.PartsTrayComplete = PartsTrayComplete;
    }

    public List<Slot> getSlotList() {
        return SlotList;
    }

    public void setSlotList(List<Slot> SlotList) {
        this.SlotList = SlotList;
    }

    public PoseType getPartsTrayPose() {
        return this.PartsTrayPose;
    }

    public void setpartsTrayPose(PoseType PartsTrayPose) {
        this.PartsTrayPose = PartsTrayPose;
    }
    
    @Override
    public PartsTray clone() {
        return  (PartsTray) super.clone();
    }
}
