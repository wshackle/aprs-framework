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
@SuppressWarnings("serial")
public class ToolHolder extends PhysicalItem {

    public ToolHolder(String PartsTrayName) {
        super(PartsTrayName,0.0,0.0,0.0,1.0,"H");
    }

    public ToolHolder(String name, double rotation, double x, double y) {
        super(name, rotation, x, y,1.0,"H");
    }

    public ToolHolder(String name, double rotation, double x, double y, double score, String type) {
        super(name, rotation, x, y, score, type);
    }

    public ToolHolder(String name, PoseType pose, int visioncycle) {
        super(name, pose, "H",visioncycle);
    }
    
    public ToolHolder(String name, PoseType pose) {
        super(name, pose, "H",-1);
    }
}
