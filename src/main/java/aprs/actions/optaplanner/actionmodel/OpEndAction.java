/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import static aprs.actions.optaplanner.actionmodel.OpActionType.END;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"SameReturnValue", "unused"})
public class OpEndAction  implements OpActionInterface{

    private Point2D.Double location;
    final int id;
    public final StackTraceElement []createTrace;
    
    public OpEndAction() {
        location = new Point2D.Double();
        this.id = OpActionPlan.newActionPlanId();
        createTrace = Thread.currentThread().getStackTrace();
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

    private static final String END_STRING = "END";
     public String locationString() {
        if(null != location) {
            return String.format("{%.1f,%.1f}", location.x,location.y);
        } else {
            return "";
        }
    }
    
    @Override
    public String shortString() {
        return END_STRING + ":" + getId()+locationString();
    }

    @Override
    public String toString() {
        return shortString();
    }
    
    @Override
    public List<Integer> getPossibleNextIds() {
        return Collections.emptyList();
    }
    
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
       return END_STRING;
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
