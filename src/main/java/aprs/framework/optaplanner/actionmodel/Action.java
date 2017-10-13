/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import static aprs.framework.optaplanner.actionmodel.ActionType.DROPOFF;
import static aprs.framework.optaplanner.actionmodel.ActionType.END;
import static aprs.framework.optaplanner.actionmodel.ActionType.PICKUP;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableGraphType;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@PlanningEntity
public class Action implements ActionInterface {

    private final static AtomicInteger idCounter = new AtomicInteger();
    
    public Action(String name,double x, double y, ActionType actionType, String partType) {
        this.name = name;
        this.location = new Point2D.Double(x, y);
        this.actionType = actionType;
        this.partType = partType;
        this.id = idCounter.incrementAndGet()+1;
    }

    public Action() {
        this.id = idCounter.incrementAndGet()+1;
    }

    private String name;

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }

    private Point2D.Double location;

    public Point2D.Double getLocation() {
        return location;
    }

    public void setLocation(Point2D.Double location) {
        this.location = location;
    }

    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {"possibleNextActions", "endActions"})
    private ActionInterface next;

    private ActionType actionType;

    @Override
    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    private String partType;

    @Override
    public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    private final List<ActionInterface> possibleNextActions = new ArrayList<>();

    @ValueRangeProvider(id = "possibleNextActions")
    public List<ActionInterface> getPossibleNextActions() {
        return possibleNextActions;
    }

    @Override
    public ActionInterface getNext() {
        return next;
    }

    public void setNext(ActionInterface next) {
//        if(!checkNextAction(next)) {
//            throw new IllegalStateException("Settting next for "+this+" to "+next+" : possibles="+getPossibleNextActions());
//        }
        this.next = next;
    }

    public boolean checkNextAction(ActionInterface possibleNextAction) {
        switch (actionType) {
            case START:
                return (possibleNextAction.getActionType() == PICKUP);
            case DROPOFF:
                return (possibleNextAction.getActionType() == PICKUP)
                        || possibleNextAction.getActionType() == END;

            case PICKUP:
                return (Objects.equals(partType, possibleNextAction.getPartType()) && possibleNextAction.getActionType() == DROPOFF);
            case END:
            default:
                return false;
        }
    }

    public void addPossibleNextActions(List<? extends ActionInterface> allActions) {
        for (ActionInterface action : allActions) {
            if (checkNextAction(action)) {
                possibleNextActions.add(action);
            }
        }
        if (actionType == END) {
            if (possibleNextActions.isEmpty()) {
                possibleNextActions.add(this);
            }
        }
    }

    public double cost() {
        if (null == next || null == next.getLocation() || null == location) {
            return Double.POSITIVE_INFINITY;
        }
        return next.getLocation().distance(location);
    }

    @Override
    public String toString() {
        if(next instanceof Action) {
            return name +" -> "+((Action)next).name+"("+cost()+")";
        } else if(null != next) {
            return name +" -> "+next.getActionType()+"("+cost()+")";
        }
        return name + "-> null";
    }

    final int id;
    
    @Override
    public int getId() {
       return id;
    }
    
    

}
