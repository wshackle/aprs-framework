/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import java.awt.geom.Point2D;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface ActionInterface {

    public int getId();
    
    public String getName();
    
    public ActionType getActionType();

    public String getPartType();

    public ActionInterface getNext();
    
    public Point2D.Double getLocation();
}
