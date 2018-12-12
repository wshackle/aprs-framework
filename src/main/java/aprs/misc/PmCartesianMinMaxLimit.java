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
package aprs.misc;

import rcs.posemath.PmCartesian;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PmCartesianMinMaxLimit {

    private final PmCartesian min;
    private final PmCartesian max;
    
    private static double toDouble(Object obj) {
        if(obj instanceof String) {
            return Double.parseDouble((String) obj);
        } 
        return (double) obj;
    }

    
    public PmCartesianMinMaxLimit(Object[] rowData) {
        this(
                new PmCartesian(toDouble(rowData[0]), toDouble(rowData[2]), toDouble(rowData[4])),
                new PmCartesian(toDouble(rowData[1]), toDouble(rowData[3]), toDouble(rowData[5]))
        );
    }

    public PmCartesianMinMaxLimit(PmCartesian min, PmCartesian max) {
        this.min = min;
        this.max = max;
        if (min.x > max.x) {
            throw new IllegalArgumentException("min.x > max.x : min=" + min + ",max=" + max);
        }
        if (min.y > max.y) {
            throw new IllegalArgumentException("min.y > max.y: min=" + min + ",max=" + max);
        }
        if (min.z > max.z) {
            throw new IllegalArgumentException("min.z > max.z: min=" + min + ",max=" + max);
        }
    }

    public PmCartesian getMin() {
        return min;
    }

    public PmCartesian getMax() {
        return max;
    }

    public static String[] getHeaders() {
        return new String[]{"MinX", "MaxX", "MinY", "MaxY", "MinZ", "MaxZ"};
    }

    public Object[] toObjArray() {
        return new Object[]{min.x, max.x, min.y, max.y, min.z, max.z};
    }

}
