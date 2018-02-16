/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import java.awt.geom.Point2D;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface OpActionInterface {

    public int getId();
    
    public String getName();
    
    public OpActionType getActionType();

    public String getPartType();

    public @Nullable OpActionInterface getNext();
    
    public Point2D.Double getLocation();
}
