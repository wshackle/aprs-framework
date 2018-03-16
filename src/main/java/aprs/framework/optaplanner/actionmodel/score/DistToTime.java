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
package aprs.framework.optaplanner.actionmodel.score;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DistToTime {
    
    private static String badTime(double time) {
        return "time =" + time;
    }
    public static double distToTime(double dist, double accel, double speed) {
        if(dist < 0) {
            throw new IllegalArgumentException("dist="+dist);
        }
        if(accel < Double.MIN_NORMAL) {
            throw new IllegalArgumentException("accel="+accel);
        }
        if(speed < Double.MIN_NORMAL) {
            throw new IllegalArgumentException("dist="+speed);
        }
        if(dist < Double.MIN_NORMAL) {
            return 0;
        }
        double timeToMaxSpeed = 2*speed/accel; // time to accel and decel
        double distToMaxSpeed = speed*timeToMaxSpeed/2.0; // (average speed when accel or decel is half max) * time.
        if(dist <  distToMaxSpeed) {
            return Math.sqrt(dist/accel);
        }
        double time = (dist-distToMaxSpeed)/speed+timeToMaxSpeed;
        assert time > 0 : badTime(time);
        return time;
    }
}
