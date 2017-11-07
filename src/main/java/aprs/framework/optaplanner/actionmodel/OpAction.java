/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import static aprs.framework.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.END;
import static aprs.framework.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.PICKUP;
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
public class OpAction implements OpActionInterface {

    private final static AtomicInteger idCounter = new AtomicInteger();

    public OpAction(String name, double x, double y, OpActionType actionType, String partType) {
        this.name = name;
        this.location = new Point2D.Double(x, y);
        this.actionType = actionType;
        this.partType = partType;
        this.id = idCounter.incrementAndGet() + 1;
    }

    public OpAction() {
        this.id = idCounter.incrementAndGet() + 1;
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
    private OpActionInterface next;

    private OpActionType actionType;

    @Override
    public OpActionType getActionType() {
        return actionType;
    }

    public void setActionType(OpActionType actionType) {
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

    private final List<OpActionInterface> possibleNextActions = new ArrayList<>();

    @ValueRangeProvider(id = "possibleNextActions")
    public List<OpActionInterface> getPossibleNextActions() {
        return possibleNextActions;
    }

    @Override
    public OpActionInterface getNext() {
        return next;
    }

    public void setNext(OpActionInterface next) {
//        if(!checkNextAction(next)) {
//            throw new IllegalStateException("this="+name+" Settting next for "+this+" to "+next+" : possibles="+getPossibleNextActions());
//        }
        this.next = next;
    }

    public boolean checkNextAction(OpActionInterface possibleNextAction) {
        switch (actionType) {
            case START:
                return (possibleNextAction.getActionType() == PICKUP);
            case DROPOFF:
            case FAKE_DROPOFF:
                return (possibleNextAction.getActionType() == PICKUP)
                        || possibleNextAction.getActionType() == END;

            case PICKUP:
                return (Objects.equals(partType, possibleNextAction.getPartType())
                        && (possibleNextAction.getActionType() == DROPOFF || possibleNextAction.getActionType() == FAKE_DROPOFF));

            case END:
            default:
                return false;
        }
    }

    private int maxNextEffectiveCount = 0;
    
    public void addPossibleNextActions(List<? extends OpActionInterface> allActions) {
        maxNextEffectiveCount = allActions.size();
        for (OpActionInterface action : allActions) {
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

    public OpActionInterface effectiveNext() {
        if (getActionType() == FAKE_DROPOFF) {
            return null;
        }
        if (null == next) {
            return null;
        }
        List<OpActionInterface> nxts = new ArrayList<>();
        switch (next.getActionType()) {
            case FAKE_DROPOFF:
                return null;

            case PICKUP:
                if (null == next.getNext()) {
                    throw new IllegalStateException("this=" + name + ", next=" + next + " next.getNext() ==null");
                }
                if (next.getNext().getActionType() == FAKE_DROPOFF) {
                    if (null == next.getNext()) {
                        throw new IllegalStateException("this=" + name + " next.getNext() ==null");
                    }
                    OpActionInterface nxt2 = next.getNext().getNext();
                    nxts.add(next);
                    nxts.add(next.getNext());
                    nxts.add(nxt2);
                    if (null == nxt2) {
                        throw new IllegalStateException("this=" + name + " nxt2 ==null");
                    }
                    if (null == nxt2.getNext()) {
                        throw new IllegalStateException("this=" + name + " nxt2.getNext() ==null");
                    }
                    if (nxt2 == next) {
                        throw new IllegalStateException("this=" + name + " nxt2 == next");
                    }
                    if (nxt2 == this) {
                        throw new IllegalStateException("this=" + name + " nxt2 == this");
                    }
                    nxts.add(nxt2.getNext());
                    int count = 0;
                    while (nxt2.getActionType() == PICKUP && nxt2.getNext().getActionType() == FAKE_DROPOFF) {
                        count++;
                        if (count > maxNextEffectiveCount) {
                            throw new IllegalStateException("this=" + name + " count > maxCount,nxts=" + nxts);
                        }
                        
                        nxt2 = nxt2.getNext().getNext();
                        nxts.add(nxt2);
                        if (null == nxt2.getNext()) {
                            throw new IllegalStateException("this=" + name + " nxt2.getNext() ==null");
                        }
                        nxts.add(nxt2.getNext());
                        if (nxt2 == next) {
                            throw new IllegalStateException("this=" + name + " nxt2 == next");
                        }
                        if (nxt2 == this) {
                            throw new IllegalStateException("this=" + name + " nxt2 == this");
                        }
                    }
                    return nxt2;
                }
        }
        return next;
    }

    public double cost() {
        if (null == next) {
            throw new IllegalStateException("this=" + name + " next==null");
        }
        if (null == next.getLocation()) {
            throw new IllegalStateException("this=" + name + " next.getLocation()==null");
        }
        if (null == location) {
            throw new IllegalStateException("this=" + name + " location ==null");
        }
        OpActionInterface effNext = effectiveNext();
        if (null == effNext) {
            return 0;
        }
        return effNext.getLocation().distance(location);
    }

    @Override
    public String toString() {
        String effNextString = "";
        OpActionInterface effNext = effectiveNext();
        if (effNext != next && null != effNext) {
            effNextString = "(effectiveNext=" + effNext.getName() + ")";
        }
        String partTypeString = "";
        if(null != this.partType && this.partType.length() > 0) {
            partTypeString = "(partType=" + this.partType + ")";
        }
        String infoString = partTypeString + effNextString;
        if(partTypeString.endsWith(")") && effNextString.startsWith("(")) {
            infoString = partTypeString.substring(0,partTypeString.length()-1)+","+effNextString.substring(1);
        }
        if (next instanceof OpAction) {
            if (null != next.getNext() && getActionType() != FAKE_DROPOFF && next.getActionType() != FAKE_DROPOFF) {
                return name+ infoString  + " -> " + ((OpAction) next).name + "(cost=" + String.format("%.3f", cost()) + ")";
            } else {
                return name + infoString  + " -> " + ((OpAction) next).name;
            }
        } else if (null != next) {
            return name+ infoString + " -> " + next.getActionType();
        }
        return name + "-> null";
    }

    final int id;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.location);
        hash = 97 * hash + Objects.hashCode(this.actionType);
        hash = 97 * hash + Objects.hashCode(this.partType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OpAction other = (OpAction) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.partType, other.partType)) {
            return false;
        }
        if (!Objects.equals(this.location, other.location)) {
            return false;
        }
        if (this.actionType != other.actionType) {
            return false;
        }
        return true;
    }

    
}
