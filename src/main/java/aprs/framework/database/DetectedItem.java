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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
//TODO-zeid
public class DetectedItem {

    public String name;
    public String fullName;
    public int repeats;
    public int index;
    public double rotation;
    public double x;
    public double y;
    public double z;
    public double vxi = 1;
    public double vxj = 0;
    public double vxk = 0;
    public double vzi = 0;
    public double vzj = 0;
    public double vzk = 1;
    public double score;
    public int visioncycle;
    public String type;
    public AffineTransform displayTransform;
    public AffineTransform origTransform;
    public AffineTransform relTransform;
    public Rectangle2D.Double displayRect;
    public boolean insideKitTray;
    public boolean insidePartsTray;
    
    public DetectedItem() {
        score = 100.0;
    }

    
    public DetectedItem(String name, double rotation, double x, double y) {
        this.name = name;
        this.rotation = rotation;
        this.vxi = Math.cos(rotation);
        this.vxj = Math.sin(rotation);
        this.x = x;
        this.y = y;
        this.score = 100.0;
    }

    public DetectedItem(String name, double rotation, double x, double y, double score, String type) {
        this.name = name;
        this.rotation = rotation;
        this.vxi = Math.cos(rotation);
        this.vxj = Math.sin(rotation);
        this.x = x;
        this.y = y;
        this.score = score;
        this.type = type;
    }
    
    public DetectedItem(String name, PoseType pose, int visioncycle) {
        this.name = name;
        if (null != pose) {
            VectorType xAxis = pose.getXAxis();
            if (null != xAxis) {
                this.rotation = Math.atan2(xAxis.getJ().doubleValue(), xAxis.getI().doubleValue());
                this.vxi = xAxis.getI().doubleValue();
                this.vxj = xAxis.getJ().doubleValue();
                this.vxk = xAxis.getK().doubleValue();
            }
            VectorType zAxis = pose.getZAxis();
            if (null != zAxis) {
                this.vzi = zAxis.getI().doubleValue();
                this.vzj = zAxis.getJ().doubleValue();
                this.vzk = zAxis.getK().doubleValue();
            }
            PointType pt = pose.getPoint();
            if (null != pt) {
                this.x = pt.getX().doubleValue();
                this.y = pt.getY().doubleValue();
                this.z = pt.getZ().doubleValue();
            }
        }
        this.score = 100.0;
        this.visioncycle = visioncycle;
    }
    
    public double dist(DetectedItem other) {
        double dx = x-other.x;
        double dy = y-other.y;
        return Math.sqrt(dx*dx+dy*dy);
    }

    public PoseType toCrclPose() {
        return pose(point(x, y, z), vector(vxi, vxj, vxk), vector(vzi, vzj, vzk));
    }

    @Override
    public String toString() {
        return "DetectedItem{" + "name=" + name + ", fullName=" + fullName + ", repeats=" + repeats + ", index=" + index + ", rotation=" + rotation + ", x=" + x + ", y=" + y + ", z=" + z + ", vxi=" + vxi + ", vxj=" + vxj + ", vxk=" + vxk + ", vzi=" + vzi + ", vzj=" + vzj + ", vzk=" + vzk + ", score=" + score + ", type=" + type + '}';
    }

}
