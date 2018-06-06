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

import crcl.base.PoseType;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class KitTray extends Tray {

    public static KitTray newKitTrayFromSkuIdRotXY(String sku,int id,double rotation, double x, double y) {
        KitTray kt = new KitTray(sku,rotation,x,y);
        kt.setFullName(sku+"_"+id);
        kt.setSku(sku);
        kt.setIndex(id);
        return kt;
    }
    
    public KitTray(String PartsTrayName) {
        super(PartsTrayName,0.0,0.0,0.0,1.0,"KT");
    }

    private KitTray(String name, double rotation, double x, double y) {
        super(name, rotation, x, y,1.0,"KT");
    }

    public KitTray(String name, double rotation, double x, double y, double score, String type) {
        super(name, rotation, x, y, score, type);
    }

    public KitTray(String name, PoseType pose, int visioncycle) {
        super(name, pose, visioncycle);
    }
    
}
