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

import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.base.VectorType;
import crcl.ui.forcetorquesensorsimulator.TrayStack;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.pose;
import static crcl.utils.CRCLPosemath.vector;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PM_CARTESIAN;

/**
 * This is a general holder for anything that might have a position associated
 * with it that comes directly or indirectly from the vision system including
 * parts, kit trays, parts trays, slots in trays, etc.
 *
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class PhysicalItem extends PM_CARTESIAN {

    final public String origName;
    private @Nullable
    String sku;
    private String name;
    private @Nullable
    String fullName;

    public @Nullable
    String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
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
    private String type = "P";
    private @Nullable
    AffineTransform displayTransform;
    private @Nullable
    AffineTransform origTransform;
    private @Nullable
    AffineTransform relTransform;
    private @Nullable
    Rectangle2D displayRect;
    private boolean insideKitTray;
    private boolean insidePartsTray;
    private @Nullable
    String setQuery;
    private long timestamp;
    private @Nullable
    PhysicalItem tray;
    private long emptySlotsCount;
    private long totalSlotsCount;
    private double maxSlotDist;

    private List<PhysicalItem> emptySlotsList = new ArrayList<>();
    private int kitTrayNum;
    private @Nullable
    String slotForSkuName;
    private Color labelColor = Color.BLACK;
    private @Nullable
    String newSlotQuery = null;

    private @Nullable
    String prpName;

    private @Nullable TrayStack trayStack = null;

    public @Nullable
    TrayStack getTrayStack() {
        return trayStack;
    }

    public void setTrayStack(@Nullable TrayStack trayStack) {
        this.trayStack = trayStack;
    }

    private boolean stackable = false;

    /**
     * Get the value of stackable
     *
     * @return the value of stackable
     */
    public boolean isStackable() {
        return stackable;
    }

    /**
     * Set the value of stackable
     *
     * @param stackable new value of stackable
     */
    public void setStackable(boolean stackable) {
        this.stackable = stackable;
    }

    private double height = java.lang.Double.NaN;

    /**
     * Get the value of height
     *
     * @return the value of height
     */
    public double getHeight() {
        return height;
    }

    /**
     * Set the value of height
     *
     * @param height new value of height
     */
    public void setHeight(double height) {
        this.height = height;
    }

    private int count = 1;

    /**
     * Get the value of count
     *
     * @return the value of count
     */
    public int getCount() {
        return count;
    }

    /**
     * Set the value of count
     *
     * @param count new value of count
     */
    public void setCount(int count) {
        this.count = count;
    }

    public PoseType getPose() {
        return pose(point(x, y, z), vector(vxi, vxj, vxk), vector(vzi, vzj, vzk));
    }

    public void setPose(PoseType pose) {
        if (null == pose) {
            throw new IllegalArgumentException("null == pose");
        }
        PointType pt = pose.getPoint();
        if (null != pt) {
            x = pt.getX();
            y = pt.getY();
            z = pt.getZ();
        }
        VectorType xvec = pose.getXAxis();
        if (null != xvec) {
            vxi = xvec.getI();
            vxj = xvec.getJ();
            vxk = xvec.getK();
            rotation = Math.atan2(vxj, vxi);
        }
        VectorType zvec = pose.getZAxis();
        if (null != zvec) {
            vzi = zvec.getI();
            vzj = zvec.getJ();
            vzk = zvec.getK();
        }
    }

    /**
     * Get the value of prpName
     *
     * @return the value of prpName
     */
    public String getPrpName() {
        if (null != prpName) {
            return prpName;
        }
        throw new NullPointerException("prpName is null for PhysicalItem with name=" + name);
    }

    /**
     * Set the value of prpName
     *
     * @param prpName new value of prpName
     */
    public void setPrpName(String prpName) {
        this.prpName = prpName;
    }

    protected PhysicalItem(String name) {
        this.name = name;
        this.origName = name;
    }

    protected PhysicalItem(String name, String type) {
        this(name);
        this.type = type;
    }

    public static PhysicalItem newPhysicalItemNameRotXY(String name, double rotation, double x, double y) {
        return new PhysicalItem(name, rotation, x, y);
    }

    protected PhysicalItem(String name, double rotation, double x, double y) {
        this(name);
        this.rotation = rotation;
        this.vxi = Math.cos(rotation);
        this.vxj = Math.sin(rotation);
        this.x = x;
        this.y = y;
    }

    protected PhysicalItem(String name, double rotation, double x, double y, String type) {
        this(name, rotation, x, y);
        this.type = type;
    }

    public void normalizeRotation() {
        rotation = rotation % (2.0 * Math.PI);
        switch (type) {
            case "PT":
                // FIXME: Determine which parts are symetric.
                if (rotation < -Math.PI / 2.0) {
                    rotation += Math.PI;
                } else if (rotation > Math.PI / 2.0) {
                    rotation -= Math.PI;
                }
                break;

            default:
                break;
        }
    }

    public static PhysicalItem newPhysicalItemNameRotXYScoreType(String name, double rotation, double x, double y, double score, String type) {
        switch (type) {
            case "KT":
            case "PT":
                return new Tray(name, rotation, x, y, score, type);

            case "S":
            case "SLOT":
            case "ES":
                return new Slot(name, rotation, x, y, score, type);

            case "P":
                return new PhysicalItem(name, rotation, x, y, score, type);

            default:
                throw new IllegalArgumentException("type =" + type);
//                return new PhysicalItem(name, rotation, x, y, score, type);
        }
    }

    protected PhysicalItem(String name, double rotation, double x, double y, double score, String type) {
        this(name, rotation, x, y);
        this.score = score;
        this.type = type;
    }

    public static PhysicalItem newPhysicalItemNamePoseVisionCycle(String name, PoseType pose, int visioncycle) {
        return new PhysicalItem(name, pose, visioncycle);
    }

    protected PhysicalItem(String name, PoseType pose, int visioncycle) {
        this(name);
        this.visioncycle = visioncycle;
        setFromCrclPoseTypeInternal(this, pose);
    }

    protected PhysicalItem(String name, PoseType pose, String type, int visioncycle) {
        this.name = name;
        this.origName = name;
        this.type = type;
        this.visioncycle = visioncycle;
        setFromCrclPoseTypeInternal(this, pose);
    }

    public final void setFromCrclPoseType(PoseType pose) {
        setFromCrclPoseTypeInternal(this, pose);
    }

    private static void setFromCrclPoseTypeInternal(@UnknownInitialization PhysicalItem thisItem, PoseType pose) {
        if (null != pose) {
            VectorType xAxis = pose.getXAxis();
            if (null != xAxis) {
                thisItem.rotation = Math.atan2(xAxis.getJ(), xAxis.getI());
                thisItem.vxi = xAxis.getI();
                thisItem.vxj = xAxis.getJ();
                thisItem.vxk = xAxis.getK();
            }
            VectorType zAxis = pose.getZAxis();
            if (null != zAxis) {
                thisItem.vzi = zAxis.getI();
                thisItem.vzj = zAxis.getJ();
                thisItem.vzk = zAxis.getK();
            }
            PointType pt = pose.getPoint();
            if (null != pt) {
                thisItem.x = pt.getX();
                thisItem.y = pt.getY();
                thisItem.z = pt.getZ();
            }
        }
    }

    public double dist(PhysicalItem other) {
        return dist(other.x, other.y);
    }

    public double dist(double otherx, double othery) {
        double dx = x - otherx;
        double dy = y - othery;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        if (null == fullName) {
            return String.format("PhysicalItem{fullName=null(name=%s), x=%.3f, y=%.3f }", name, x, y);
        }
        return String.format("PhysicalItem{fullName=%s, x=%.3f, y=%.3f }", fullName, x, y);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name.contains("in_kt_in_kt") || name.contains("in_pt_in_pt")) {
            throw new IllegalArgumentException("double in_kt_in_kt in name=" + name);
        }
        this.name = name;
    }

    public String getFullName() {
        if (name.contains("in_kt_in_kt") || name.contains("in_pt_in_pt")) {
            throw new IllegalArgumentException("double in_kt_in_kt in name=" + name);
        }
        if (fullName == null) {
            return name;
        }
        return fullName;
    }

    public boolean isFullNameSet() {
        return null != fullName;
    }

    public void setFullName(String fullName) {
        if (fullName.contains("in_kt_in_kt") || fullName.contains("in_pt_in_pt")) {
            throw new IllegalArgumentException("double in_kt_in_kt in fullName=" + fullName);
        }
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
        this.vxi = Math.cos(rotation);
        this.vxj = Math.sin(rotation);
    }

    public double getVxi() {
        return vxi;
    }

    public void setVxi(double vxi) {
        this.vxi = vxi;
        rotation = Math.atan2(vxj, vxi);
    }

    public double getVxj() {
        return vxj;
    }

    public void setVxj(double vxj) {
        this.vxj = vxj;
        rotation = Math.atan2(vxj, vxi);
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

    public @Nullable
    AffineTransform getDisplayTransform() {
        return displayTransform;
    }

    public void setDisplayTransform(@Nullable AffineTransform displayTransform) {
        this.displayTransform = displayTransform;
    }

    public @Nullable
    AffineTransform getOrigTransform() {
        return origTransform;
    }

    public void setOrigTransform(AffineTransform origTransform) {
        this.origTransform = origTransform;
    }

    public @Nullable
    AffineTransform getRelTransform() {
        return relTransform;
    }

    public void setRelTransform(AffineTransform relTransform) {
        this.relTransform = relTransform;
    }

    public @Nullable
    Rectangle2D getDisplayRect() {
        return displayRect;
    }

    public void setDisplayRect(Rectangle2D.@Nullable Double displayRect) {
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

    public @Nullable
    String getSetQuery() {
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

    public @Nullable
    PhysicalItem getTray() {
        return tray;
    }

    public void setTray(PhysicalItem tray) {
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

    public List<PhysicalItem> getEmptySlotsList() {
        return emptySlotsList;
    }

    public void setEmptySlotsList(List<PhysicalItem> emptySlotsList) {
        this.emptySlotsList = emptySlotsList;
    }

    public int getKitTrayNum() {
        return kitTrayNum;
    }

    public void setKitTrayNum(int kitTrayNum) {
        this.kitTrayNum = kitTrayNum;
    }

    public @Nullable
    String getSlotForSkuName() {
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

    public @Nullable
    String getNewSlotQuery() {
        return newSlotQuery;
    }

    public void setNewSlotQuery(String newSlotQuery) {
        this.newSlotQuery = newSlotQuery;
    }

    private @Nullable
    Map<String, String> newSlotOffsetResultMap = null;

    /**
     * Get the value of newSlotOffsetResultMap
     *
     * @return the value of newSlotOffsetResultMap
     */
    public @Nullable
    Map<String, String> getNewSlotOffsetResultMap() {
        return newSlotOffsetResultMap;
    }

    /**
     * Set the value of newSlotOffsetResultMap
     *
     * @param newSlotOffsetResultMap new value of newSlotOffsetResultMap
     */
    public void setNewSlotOffsetResultMap(Map<String, String> newSlotOffsetResultMap) {
        this.newSlotOffsetResultMap = newSlotOffsetResultMap;
    }

    @Override
    public PhysicalItem clone() {
        return (PhysicalItem) super.clone();
    }
}
