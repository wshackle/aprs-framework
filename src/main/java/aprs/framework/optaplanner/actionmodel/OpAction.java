/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import static aprs.framework.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.END;
import static aprs.framework.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.framework.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import static aprs.framework.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.framework.optaplanner.actionmodel.OpActionType.START;
import aprs.framework.optaplanner.actionmodel.score.DistToTime;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    public OpAction(String name, double x, double y, OpActionType actionType, String partType, boolean required) {
        this.name = name;
        this.location = new Point2D.Double(x, y);
        this.actionType = actionType;
        this.partType = partType;
        this.id = idCounter.incrementAndGet() + 1;
        this.required = required;
        switch (actionType) {
            case PICKUP:
            case DROPOFF:
                if (name.contains("_in_pt_") || name.endsWith("in_pt")) {
                    this.trayType = "PT";
                } else if (name.contains("_in_kit_")) {
                    this.trayType = "KT";
                }
                break;

            default:
                break;
        }
    }

    private final boolean required;

    @Override
    public boolean isRequired() {
        return required;
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
    private @Nullable OpActionInterface next;

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
    public @Nullable
    OpActionInterface getNext() {
        return next;
    }

    public void setNext(OpActionInterface next) {
        if (!checkNextAction(next)) {
            throw new IllegalStateException("this=" + name + " Settting next for " + this + " to " + next + " : possibles=" + getPossibleNextActions());
        }
        this.next = next;
    }

    public boolean actionRequired() {
        return Objects.equals(trayType, "KT");
    }

    public boolean checkNextAction(OpActionInterface possibleNextAction) {
        switch (actionType) {
            case START:
                return (possibleNextAction.getActionType() == PICKUP);
            case FAKE_DROPOFF:
            case DROPOFF:
                return (possibleNextAction.getActionType() == PICKUP)
                        || (possibleNextAction.getActionType() == FAKE_PICKUP)
                        || possibleNextAction.getActionType() == END;

            case PICKUP:
                return (Objects.equals(partType, possibleNextAction.getPartType())
                        && (possibleNextAction.getActionType() == DROPOFF)
                        || ((!this.actionRequired() && possibleNextAction.getActionType() == FAKE_DROPOFF)));

            case FAKE_PICKUP:
                return (Objects.equals(partType, possibleNextAction.getPartType())
                        && ((!possibleNextAction.isRequired() && possibleNextAction.getActionType() == DROPOFF)
                        || possibleNextAction.getActionType() == FAKE_DROPOFF));

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

    @Nullable private String trayType = null;

    @Nullable @Override
    public String getTrayType() {
        return this.trayType;
    }

    public void setTrayType(String trayType) {
        this.trayType = trayType;
    }

    @Override
    public boolean skipNext() {
        if (null == next) {
            return false;
        }
        if (this.actionType == FAKE_PICKUP) {
            return true;
        }
        if (this.actionType != PICKUP) {
            return false;
        }
        if (next.getActionType() == FAKE_DROPOFF) {
            return true;
        }
        return !this.isRequired() && !next.isRequired();
    }

    @Override @Nullable
    public OpActionInterface effectiveNext(boolean quiet) {
        if (getActionType() == FAKE_DROPOFF) {
            return next;
        }
        if (null == next) {
            if (quiet) {
                return null;
            }
            throw new IllegalStateException("this=" + name + ", next=" + next + " next.getNext() ==null");
        }
//        List<OpActionInterface> nxts = new ArrayList<>();
        switch (next.getActionType()) {
            case FAKE_DROPOFF:
                return null;

            case PICKUP:
            case FAKE_PICKUP:
                OpActionInterface nextNext = next.getNext();
                if (null == nextNext) {
                    if (quiet) {
                        return next;
                    }
                    throw new IllegalStateException("this=" + name + ", next=" + next + " next.getNext() ==null");
                }
                if (next.skipNext()) {

//                    nxts.add(next);
//                    nxts.add(nextNext);
                    OpActionInterface nxt2 = nextNext.getNext();
                    if (null == nxt2) {
                        throw new IllegalStateException("this=" + name + " nxt2 ==null");
                    }
//                    nxts.add(nxt2);
                    OpActionInterface nxt2Next = nxt2.getNext();
                    if (null == nxt2Next) {
                        throw new IllegalStateException("this=" + name + " nxt2.getNext() ==null");
                    }
                    if (nxt2 == next) {
                        throw new IllegalStateException("this=" + name + " nxt2 == next");
                    }
                    if (nxt2 == this) {
                        throw new IllegalStateException("this=" + name + " nxt2 == this");
                    }
//                    nxts.add(nxt2Next);
                    int count = 0;
                    while (nxt2.skipNext()) {
                        count++;
                        if (count > maxNextEffectiveCount) {
                            throw new IllegalStateException("this=" + name + " count > maxCount");//,nxts=" + nxts);
                        }

                        nxt2 = nxt2Next.getNext();
                        if (null == nxt2) {
                            throw new IllegalStateException("this=" + name + " nxt2Next.getNext() ==null");
                        }
//                        nxts.add(nxt2);
                        nxt2Next = nxt2.getNext();
                        if (null == nxt2Next) {
                            throw new IllegalStateException("this=" + name + " nxt2="+nxt2+" nxt2.getNext() ==null");
                        }
//                        nxts.add(nxt2Next);
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

    public double cost(OpActionPlan plan) {
        if (actionType == FAKE_PICKUP || actionType == FAKE_DROPOFF || next.getActionType() == FAKE_DROPOFF) {
            return 0;
        }
        if (this.skipNext()) {
            return 0;
        }
        if (plan.isUseDistForCost()) {
            return distance(false);
        }
        if (this.actionType == START) {
            return DistToTime.distToTime(this.distance(false), plan.getAccelleration(), plan.getStartEndMaxSpeed());
        }
        OpActionInterface effNext = effectiveNext(false);
        if (null == effNext) {
            return Double.POSITIVE_INFINITY;
        }
        if (effNext.getActionType() == END) {
            return DistToTime.distToTime(this.distance(false), plan.getAccelleration(), plan.getStartEndMaxSpeed());
        }
        return DistToTime.distToTime(this.distance(false), plan.getAccelleration(), plan.getMaxSpeed());
    }

    public double distance(boolean quiet) {
        if (null == next) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " next==null");
        }
        if (null == next.getLocation()) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " next.getLocation()==null");
        }
        if (null == location) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " location ==null");
        }
        OpActionInterface effNext = effectiveNext(quiet);
        if (null == effNext) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " effNext == null, next =" + next);
        }
        return effNext.getLocation().distance(location);
    }

    @Override
    public String toString() {
        String effNextString = "";
        OpActionInterface effNext = null;
        try {
            effNext = effectiveNext(true);
        } catch (Exception e) {
        }
        if (effNext != next && null != effNext) {
            effNextString = "(effectiveNext=" + effNext.getName() + ")";
        }
        String partTypeString = "";
        if (null != this.partType && this.partType.length() > 0) {
            partTypeString = "(partType=" + this.partType + ")";
        }
        String infoString = partTypeString + effNextString;
        if (partTypeString.endsWith(")") && effNextString.startsWith("(")) {
            infoString = partTypeString.substring(0, partTypeString.length() - 1) + "," + effNextString.substring(1);
        }
        OpActionInterface localNext = this.next;
        if (localNext instanceof OpAction) {
            OpActionType nextActionType = localNext.getActionType();
            OpActionType actionType = getActionType();
            if (null != localNext.getNext() && actionType != FAKE_DROPOFF && nextActionType != FAKE_DROPOFF) {
                double dist = Double.POSITIVE_INFINITY;
                try {
                    dist = distance(true);
                } catch (Exception e) {
                }
                return name + infoString + " -> " + ((OpAction) localNext).name + "(cost=" + String.format("%.3f", dist) + ")";
            } else {
                return name + infoString + " -> " + ((OpAction) localNext).name;
            }
        } else if (null != localNext) {
            return name + infoString + " -> " + localNext.getActionType();
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
    public boolean equals(@Nullable Object obj) {
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

    @Override
    public int getPriority(boolean prevRequired) {
        switch (actionType) {
            case START:
                return 0;

            case END:
                return 10;

            case PICKUP:
            case DROPOFF:
                return required ? 1 : 2;

            case FAKE_DROPOFF:
            case FAKE_PICKUP:
                return 3;

            default:
                return 3;
        }
    }

}
