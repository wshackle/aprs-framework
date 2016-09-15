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

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class TraySlotDesign {
    
    private final int ID;
    private  String TrayDesignName;
    private  String PartDesignName;
    private  double X_OFFSET;
    private  double Y_OFFSET;

    public TraySlotDesign(int ID) {
        this.ID = ID;
    }

    public int getID() {
        return ID;
    }

    public String getTrayDesignName() {
        return TrayDesignName;
    }

    public void setTrayDesignName(String TrayDesignName) {
        this.TrayDesignName = TrayDesignName;
    }

    public String getPartDesignName() {
        return PartDesignName;
    }

    public void setPartDesignName(String PartDesignName) {
        this.PartDesignName = PartDesignName;
    }

    public double getX_OFFSET() {
        return X_OFFSET;
    }

    public void setX_OFFSET(double X_OFFSET) {
        this.X_OFFSET = X_OFFSET;
    }

    public double getY_OFFSET() {
        return Y_OFFSET;
    }

    public void setY_OFFSET(double Y_OFFSET) {
        this.Y_OFFSET = Y_OFFSET;
    }
    
    
}
