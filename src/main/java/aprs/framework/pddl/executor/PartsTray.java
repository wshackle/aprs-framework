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

import java.util.List;

/**
 *
 * @author zeid
 */
public class PartsTray {

    private int NodeID;
    private String PartsTraySku;
    private String PartsTrayName;
    private String ExternalShape;
    private String PartsTrayDesign;
    private Boolean PartsTrayComplete;
    private double Rotation;
    private List<Slot> SlotList;
    private double x;
    private double y;

    
    public PartsTray(String PartsTrayName) {
        this.PartsTrayName = PartsTrayName;
    }

    public double getX(){
        return x;
    }
    public void setX(double x){
        this.x=x;
    }
    public double getY(){
        return y;
    }
    public void setY(double y){
        this.y=y;
    }
    public double getRotation(){
        return Rotation;
    }
    
    public void setRotation(double Rotation){
        this.Rotation=Rotation;
    }
    
    public String getPartsTrayName(){
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
    
    public String getExternalShape() {
        return this.ExternalShape;
    }

    public void setExternalShape(String ExternalShape) {
        this.ExternalShape = ExternalShape;
    }

    public Boolean getPartsTrayComplete() {
        return PartsTrayComplete;
    }

    public void setPartsTrayComplete(Boolean PartsTrayComplete) {
        this.PartsTrayComplete = PartsTrayComplete;
    }
    
    public List<Slot> getSlotList(){
        return SlotList;
    }
    
    public void setSlotList(List<Slot> SlotList){
        this.SlotList=SlotList;
    }
}
