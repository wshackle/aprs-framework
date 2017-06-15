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

import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.VectorType;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.vector;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import rcs.posemath.PM_CARTESIAN;

/**
 * This is a general holder for anything that might have a position associated
 * with it that comes directly or indirectly from the vision system including
 * parts, kit trays, parts trays, slots in trays, etc.
 *
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
//TODO-zeid
public class DetectedItem extends PM_CARTESIAN {

    
    final public String origName;
    private String name;
    private String fullName;
    private int repeats;
    private int index;
    private double rotation;
    private double vxi = 1;
    private double vxj = 0;
    private double vxk = 0;
    private double vzi = 0;
    private double vzj = 0;
    private double vzk = 1;
    private double score = 100.0;
    private int visioncycle;
    private String type;
    private AffineTransform displayTransform;
    private AffineTransform origTransform;
    private AffineTransform relTransform;
    private Rectangle2D.Double displayRect;
    private boolean insideKitTray;
    private boolean insidePartsTray;
    private String setQuery;
    private long timestamp;
    private DetectedItem tray;
    private long emptySlotsCount;
    private long totalSlotsCount;
    private double maxSlotDist;
    private List<DetectedItem> emptySlotsList = new ArrayList<>();
    private int kitTrayNum;
    private String slotForSkuName;
    private Color labelColor = Color.BLACK;
    private String newSlotQuery = null;
    

    public DetectedItem(String name) {
        this.name = name;
        this.origName = name;
    }

    public DetectedItem(String name, double rotation, double x, double y) {
        this(name);
        this.rotation = rotation;
        this.vxi = Math.cos(rotation);
        this.vxj = Math.sin(rotation);
        this.x = x;
        this.y = y;
    }

    public void normalizeRotation() {
        rotation = rotation % (2.0 * Math.PI);
        switch (type) {
            case "PT":
                // FIXME: Determine which parts are symetric.
                if (rotation < -Math.PI/2.0) {
                    rotation += Math.PI;
                } else if (rotation > Math.PI/2.0) {
                    rotation -= Math.PI;
                }
                break;

            default:
                break;
        }
//        if(Math.abs(rotation) > Math.PI/2.0) {
//            System.out.println("this = " + this);
//        }
    }

    public DetectedItem(String name, double rotation, double x, double y, double score, String type) {
        this(name, rotation, x, y);
        this.score = score;
        this.type = type;
    }

    public DetectedItem(String name, PoseType pose, int visioncycle) {
        this(name);
        if (null != pose) {
            VectorType xAxis = pose.getXAxis();
            if (null != xAxis) {
                this.rotation = Math.atan2(xAxis.getJ(), xAxis.getI());
                this.vxi = xAxis.getI();
                this.vxj = xAxis.getJ();
                this.vxk = xAxis.getK();
            }
            VectorType zAxis = pose.getZAxis();
            if (null != zAxis) {
                this.vzi = zAxis.getI();
                this.vzj = zAxis.getJ();
                this.vzk = zAxis.getK();
            }
            PointType pt = pose.getPoint();
            if (null != pt) {
                this.x = pt.getX();
                this.y = pt.getY();
                this.z = pt.getZ();
            }
        }
        this.visioncycle = visioncycle;
    }

    public double dist(DetectedItem other) {
        return dist(other.x, other.y);
    }

    public double dist(double otherx, double othery) {
        double dx = x - otherx;
        double dy = y - othery;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public PoseType toCrclPose() {
        return pose(point(x, y, z), vector(vxi, vxj, vxk), vector(vzi, vzj, vzk));
    }

    @Override
    public String toString() {
        return "DetectedItem{" + "name=" + name + ", fullName=" + fullName + ", repeats=" + repeats + ", index=" + index + ", rotation=" + rotation + ", x=" + x + ", y=" + y + ", z=" + z + ", vxi=" + vxi + ", vxj=" + vxj + ", vxk=" + vxk + ", vzi=" + vzi + ", vzj=" + vzj + ", vzk=" + vzk + ", score=" + score + ", type=" + type + '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getRepeats() {
        return repeats;
    }

    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public double getVxi() {
        return vxi;
    }

    public void setVxi(double vxi) {
        this.vxi = vxi;
    }

    public double getVxj() {
        return vxj;
    }

    public void setVxj(double vxj) {
        this.vxj = vxj;
    }

    public double getVxk() {
        return vxk;
    }

    public void setVxk(double vxk) {
        this.vxk = vxk;
    }

    public double getVzi() {
        return vzi;
    }

    public void setVzi(double vzi) {
        this.vzi = vzi;
    }

    public double getVzj() {
        return vzj;
    }

    public void setVzj(double vzj) {
        this.vzj = vzj;
    }

    public double getVzk() {
        return vzk;
    }

    public void setVzk(double vzk) {
        this.vzk = vzk;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getVisioncycle() {
        return visioncycle;
    }

    public void setVisioncycle(int visioncycle) {
        this.visioncycle = visioncycle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AffineTransform getDisplayTransform() {
        return displayTransform;
    }

    public void setDisplayTransform(AffineTransform displayTransform) {
        this.displayTransform = displayTransform;
    }

    public AffineTransform getOrigTransform() {
        return origTransform;
    }

    public void setOrigTransform(AffineTransform origTransform) {
        this.origTransform = origTransform;
    }

    public AffineTransform getRelTransform() {
        return relTransform;
    }

    public void setRelTransform(AffineTransform relTransform) {
        this.relTransform = relTransform;
    }

    public Rectangle2D.Double getDisplayRect() {
        return displayRect;
    }

    public void setDisplayRect(Rectangle2D.Double displayRect) {
        this.displayRect = displayRect;
    }

    public boolean isInsideKitTray() {
        return insideKitTray;
    }

    public void setInsideKitTray(boolean insideKitTray) {
        this.insideKitTray = insideKitTray;
    }

    public boolean isInsidePartsTray() {
        return insidePartsTray;
    }

    public void setInsidePartsTray(boolean insidePartsTray) {
        this.insidePartsTray = insidePartsTray;
    }

    public String getSetQuery() {
        return setQuery;
    }

    public void setSetQuery(String setQuery) {
        this.setQuery = setQuery;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public DetectedItem getTray() {
        return tray;
    }

    public void setTray(DetectedItem tray) {
        this.tray = tray;
    }

    public long getEmptySlotsCount() {
        return emptySlotsCount;
    }

    public void setEmptySlotsCount(long emptySlotsCount) {
        this.emptySlotsCount = emptySlotsCount;
    }

    public long getTotalSlotsCount() {
        return totalSlotsCount;
    }

    public void setTotalSlotsCount(long totalSlotsCount) {
        this.totalSlotsCount = totalSlotsCount;
    }

    public double getMaxSlotDist() {
        return maxSlotDist;
    }

    public void setMaxSlotDist(double maxSlotDist) {
        this.maxSlotDist = maxSlotDist;
    }

    public List<DetectedItem> getEmptySlotsList() {
        return emptySlotsList;
    }

    public void setEmptySlotsList(List<DetectedItem> emptySlotsList) {
        this.emptySlotsList = emptySlotsList;
    }

    public int getKitTrayNum() {
        return kitTrayNum;
    }

    public void setKitTrayNum(int kitTrayNum) {
        this.kitTrayNum = kitTrayNum;
    }

    public String getSlotForSkuName() {
        return slotForSkuName;
    }

    public void setSlotForSkuName(String slotForSkuName) {
        this.slotForSkuName = slotForSkuName;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    public String getNewSlotQuery() {
        return newSlotQuery;
    }

    public void setNewSlotQuery(String newSlotQuery) {
        this.newSlotQuery = newSlotQuery;
    }

}
