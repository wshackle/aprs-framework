/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import java.awt.geom.Point2D;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface OpActionInterface {

    public int getId();

    public int getOrigId();

    public String getName();

    public OpActionType getOpActionType();

    public String getPartType();

    public @Nullable
    OpActionInterface getNext();

    public @Nullable OpAction getPrevious();

    public void setPrevious(OpAction action);

    public List<Integer> getPossibleNextIds();

    public List<Integer> getPossibleNextOrigIds();

    public Point2D.Double getLocation();

    public @Nullable
    String getTrayType();

    public boolean skipNext();

    public boolean isRequired();

    public int getPriority(boolean prevRequired);

    public @Nullable
    OpActionInterface effectiveNext(boolean quiet);

    public String shortString();

    public boolean isFake();

    public double cost(OpActionPlan plan);
}
