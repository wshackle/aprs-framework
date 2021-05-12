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
package aprs.simview;

import java.awt.geom.Point2D;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Point2DMinMax {

    final Point2D.Double min;
    final Point2D.Double max;

    Point2DMinMax() {
        min = new Point2D.Double(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        max = new Point2D.Double(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Override
    public String toString() {
        return "Point2DMinMax{" + "min=" + min.toString() + ", max=" + max.toString() + '}';
    }

}
