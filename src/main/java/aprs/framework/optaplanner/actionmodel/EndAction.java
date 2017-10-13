/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import static aprs.framework.optaplanner.actionmodel.ActionType.END;
import java.awt.geom.Point2D;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class EndAction  implements ActionInterface{

    private Point2D.Double location;
    
    public EndAction() {
        location = new Point2D.Double();
    }
    
    public void setLocation(Point2D.Double location) {
        this.location = location;
    }
    
    @Override
    public ActionInterface getNext() {
        return this;
    }

    @Override
    public ActionType getActionType() {
        return END;
    }

    @Override
    public String getPartType() {
        return null;
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
    
    
}
