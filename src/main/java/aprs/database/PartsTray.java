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
package aprs.database;

import crcl.base.PoseType;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author zeid
 */
@SuppressWarnings("WeakerAccess")
public class PartsTray extends Tray {

    private int NodeID;
    private @Nullable String PartsTraySku;
    private @Nullable String PartsTrayName;
    private @Nullable String ExternalShapeModelFileName;
    private @Nullable String ExternalShapeModelFormatName;
    private @Nullable String PartsTrayDesign;
    private @Nullable Boolean PartsTrayComplete;
//    private double Rotation;  now in super class PhysicalItem
    private @Nullable List<Slot> SlotList;
//    private double x; now in super class PhysicalItem
//    private double y; now in super class PhysicalItem
    private @Nullable PoseType PartsTrayPose;

    public static PartsTray newPartsTrayFromSkuIdRotXY(String sku,int id,double rotation, double x, double y) {
        PartsTray pt = new PartsTray(sku,rotation,x,y);
        pt.setSku(sku);
        pt.setPartsTraySku(sku);
        pt.setFullName(sku+"_"+id);
        pt.setIndex(id);
        return pt;
    }
    
    
    public PartsTray(String PartsTrayName) {
        super(PartsTrayName, 0.0, 0.0, 0.0, 1.0, "PT");
        this.PartsTrayName = PartsTrayName;
    }

    public PartsTray(String name, double rotation, double x, double y) {
        super(name, rotation, x, y, 1.0, "PT");
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
    @Nullable public String getPartsTrayName() {
        return PartsTrayName;
    }

    public int getNodeID() {
        return NodeID;
    }

    public void setNodeID(int NodeID) {
        this.NodeID = NodeID;
    }

    @Nullable public String getPartsTrayDesign() {
        return PartsTrayDesign;
    }

    public void setPartsTrayDesign(String PartsTrayDesign) {
        this.PartsTrayDesign = PartsTrayDesign;
    }

    @Nullable public String getPartsTraySku() {
        return PartsTraySku;
    }

    public void setPartsTraySku(String PartsTraySku) {
        setSku(PartsTraySku);
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

    @Nullable public Boolean getPartsTrayComplete() {
        return PartsTrayComplete;
    }

    public void setPartsTrayComplete(Boolean PartsTrayComplete) {
        this.PartsTrayComplete = PartsTrayComplete;
    }

    public List<Slot> getSlotList() {
        if(null != SlotList) {
            return SlotList;
        }
        return Collections.emptyList();
    }

    public void setSlotList(List<Slot> SlotList) {
        this.SlotList = SlotList;
    }

    @Nullable public PoseType getPartsTrayPose() {
        return this.PartsTrayPose;
    }

    public void setpartsTrayPose(PoseType PartsTrayPose) {
        this.PartsTrayPose = PartsTrayPose;
    }

    @Override
    public PartsTray clone() {
        return (PartsTray) super.clone();
    }
}
