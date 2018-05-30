/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import static aprs.actions.optaplanner.actionmodel.OpActionType.END;
import java.awt.geom.Point2D;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpEndAction  implements OpActionInterface{

    private Point2D.Double location;
    
    public OpEndAction() {
        location = new Point2D.Double();
    }
    
    public void setLocation(Point2D.Double location) {
        this.location = location;
    }
    
    @Override
    public OpActionInterface getNext() {
        return this;
    }

    @Override
    public OpActionType getOpActionType() {
        return END;
    }

    @Override
    public String getPartType() {
        return "INVALID_END_PART_TYPE";
    }

    @Override
    public Point2D.Double getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "END";
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getName() {
       return "END";
    }

    @Override
    @Nullable public String getTrayType() {
        return null;
    }

    @Override
    public boolean skipNext() {
        return false;
    }
    
    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public int getPriority(boolean prevRequired) {
        return 10;
    }
    
    @Nullable public OpActionInterface effectiveNext(boolean quiet) {
        return null;
    }
}
