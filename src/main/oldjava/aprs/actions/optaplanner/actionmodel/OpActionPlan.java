/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import static aprs.actions.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.END;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.START;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("WeakerAccess")
@PlanningSolution(solutionCloner = OpActionPlanCloner.class)
public class OpActionPlan {

    @ProblemFactProperty
    OpEndAction endAction = new OpEndAction();

    public OpEndAction getEndAction() {
        return endAction;
    }

    public void setEndAction(OpEndAction endAction) {
        this.endAction = endAction;
    }

    @ProblemFactCollectionProperty
    private List<OpEndAction> endActions = Collections.singletonList(endAction);

    @ValueRangeProvider(id = "endActions")
    public List<OpEndAction> getEndActions() {
        return endActions;
    }

    public void setEndActions(List<OpEndAction> endActions) {
        this.endActions = endActions;
    }

    @PlanningEntityCollectionProperty
    private @Nullable
    List<OpAction> actions;

    @Nullable
    public List<OpAction> getActions() {
        return actions;
    }

    public static void checkActionsList(@Nullable  List<OpAction> actionsToCheck) {
        if(null != actionsToCheck) {
            IdentityHashMap<OpActionInterface, OpActionInterface> inverseEntityMap = new IdentityHashMap<>();
            for (OpAction opAction : actionsToCheck) {
                OpActionInterface nxtAction = opAction.getNext();
                if (null != nxtAction) {
                    OpActionInterface old = inverseEntityMap.put(nxtAction, opAction);
                    if (null != old) {
                        throw new IllegalStateException("opAction=" + opAction + ",nxtAction=" + nxtAction + ",old=" + old);
                    }
                }
            }
        }
    }
    
    public void checkActionList() {
        checkActionsList(getActions());
    }
    
    private boolean useDistForCost = true;

    /**
     * Get the value of useDistForCost
     *
     * @return the value of useDistForCost
     */
    public boolean isUseDistForCost() {
        return useDistForCost;
    }

    /**
     * Set the value of useDistForCost
     *
     * @param useDistForCost new value of useDistForCost
     */
    public void setUseDistForCost(boolean useDistForCost) {
        this.useDistForCost = useDistForCost;
    }

    private boolean debug;

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setActions(List<OpAction> actions) {
        this.actions = actions;
    }

    @SuppressWarnings("unused")
    public void initNextActions() {

        if (null == actions) {
            throw new IllegalStateException("actions not initialized");
        }
        List<OpAction> tmpActions = new ArrayList<>(actions);
        List<OpAction> origActions = new ArrayList<>(actions);

        if (debug) {
            System.out.println("origActions = " + origActions);
        }
        for (OpAction act : tmpActions) {
            if (act.getOpActionType() == FAKE_PICKUP || act.getOpActionType() == FAKE_DROPOFF) {
                throw new IllegalStateException("input list should not have fake actions : act =" + act + ",\norigActions=" + origActions);
            }
        }
        MutableMultimap<String, OpAction> multimapWithList
                = Lists.mutable.ofAll(tmpActions)
                        .select(a -> a.getOpActionType() == PICKUP || a.getOpActionType() == DROPOFF)
                        .groupBy(OpAction::getPartType);
        for (String partType : multimapWithList.keySet()) {
            MutableCollection<OpAction> theseActions = multimapWithList.get(partType);
            long pickupCount = theseActions.count(a -> a.getOpActionType() == PICKUP);
            long dropoffCount = theseActions.count(a -> a.getOpActionType() == DROPOFF);
            if (debug) {
                System.out.println("partType = " + partType);
                System.out.println("dropoffCount = " + dropoffCount);
                System.out.println("pickupCount = " + pickupCount);
                System.out.println("theseActions = " + theseActions);
            }
            if (dropoffCount < pickupCount) {
                for (long j = dropoffCount; j < pickupCount; j++) {
                    tmpActions.add(new OpAction("fake_dropoff_" + partType + "_" + j, 0, 0, FAKE_DROPOFF, partType, false));
                }
            } else if (pickupCount < dropoffCount) {
                for (long j = pickupCount; j < dropoffCount; j++) {
                    tmpActions.add(new OpAction("fake_pickup_" + partType + "_" + j, 0, 0, FAKE_PICKUP, partType, false));
                }
            }
        }
        List<OpActionInterface> unusedActions = new ArrayList<>(tmpActions);
        unusedActions.addAll(endActions);
        List<OpActionInterface> allActions = new ArrayList<>(unusedActions);
        OpAction startAction = findStartAction();
        for (OpAction act : tmpActions) {
            act.addPossibleNextActions(allActions);
            if (act.getPossibleNextActions().isEmpty()) {
                throw new IllegalStateException("action has no possible next action=" + act + ", act.getPartType()=" + act.getPartType() + ",\norigActions=" + origActions + ",\nallActions=" + allActions + ",\nmultimapWithList=" + multimapWithList);
            }
        }
        for (OpAction act : tmpActions) {
            boolean actRequired = act.isRequired();
            act.getPossibleNextActions().sort(new Comparator<OpActionInterface>() {
                @Override
                public int compare(OpActionInterface o1, OpActionInterface o2) {
                    return Integer.compare(o1.getPriority(actRequired), o2.getPriority(actRequired));
                }
            });
        }
        List<OpAction> newActions = new ArrayList<>();
        if (null != startAction) {
            newActions.add(startAction);
            unusedActions.remove(startAction);
        }
        OpAction lastStartAction = startAction;
        OpActionInterface lastNextAction = null;
        while (startAction != null) {
            OpAction action = startAction;
            lastStartAction = startAction;
            startAction = null;
            for (OpActionInterface nxtAction : action.getPossibleNextActions()) {
                lastNextAction = nxtAction;
                if (unusedActions.contains(nxtAction)) {
                    unusedActions.remove(nxtAction);

                    action.setNext(nxtAction);
                    if (nxtAction.getOpActionType() != END && nxtAction instanceof OpAction) {
                        newActions.add((OpAction) nxtAction);
                        startAction = (OpAction) nxtAction;
                    } else if (debug) {
                        System.out.println("Ending with action =" + action);
                        System.out.println("nxtAction = " + nxtAction);
                    }
                    break;
                }
            }
            if (action.getNext() == null) {
                throw new IllegalStateException("action has null next:action=" + action + ",\norigActions=" + origActions + ",\nnewActions=" + newActions + ",\naction.getPossibleNextActions()" + action.getPossibleNextActions() + ",\nunusedActions=" + unusedActions);
            }
        }
        if (debug) {
            System.out.println("origActions = " + origActions);
            System.out.println("newActions.size() = " + newActions.size());
            for (int i = 0; i < newActions.size(); i++) {
                OpAction act = newActions.get(i);
                if (act.getNext() == null) {
                    throw new IllegalStateException("action has null next:i=" + i + ",act=" + act + ",\norigActions=" + tmpActions + ",\nnewActions=" + newActions);
                }
                if (act.getPossibleNextActions() == null || act.getPossibleNextActions().isEmpty()) {
                    throw new IllegalStateException("action has no possibleNextAction :" + act);
                }
                System.out.println("i="+i+", "+act + ".getPossibleNextActions() = "
                        + act.getPossibleNextActions()
                                .stream()
                                .map(OpActionInterface::getName)
                                .collect(Collectors.joining(",")));
            }
        }
        actions = newActions;
        if (debug && unusedActions.size() > 0) {
            System.out.println("unusedActions = " + unusedActions);
            System.out.println("actions = " + actions);
        }

        if (debug) {
            List<OpAction> effectiveOrderedList = getEffectiveOrderedList(true);
            System.out.println("effectiveOrderedList = " + effectiveOrderedList);
        }
    }

    private @Nullable
    HardSoftLongScore score;

    @Nullable
    @PlanningScore
    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }

    private volatile String asString = "";

    @Override
    public String toString() {
        return asString;
    }

    private double maxSpeed = 1.0;

    /**
     * Get the value of maxSpeed
     *
     * @return the value of maxSpeed
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Set the value of maxSpeed
     *
     * @param maxSpeed new value of maxSpeed
     */
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    private double startEndMaxSpeed = 2 * maxSpeed;

    /**
     * Get the value of startEndMaxSpeed
     *
     * @return the value of startEndMaxSpeed
     */
    public double getStartEndMaxSpeed() {
        return startEndMaxSpeed;
    }

    /**
     * Set the value of startEndMaxSpeed
     *
     * @param startEndMaxSpeed new value of startEndMaxSpeed
     */
    public void setStartEndMaxSpeed(double startEndMaxSpeed) {
        this.startEndMaxSpeed = startEndMaxSpeed;
    }

    private double accelleration = 1.0;

    /**
     * Get the value of accelleration
     *
     * @return the value of accelleration
     */
    public double getAccelleration() {
        return accelleration;
    }

    /**
     * Set the value of accelleration
     *
     * @param accelleration new value of accelleration
     */
    public void setAccelleration(double accelleration) {
        this.accelleration = accelleration;
    }

    public String computeString() {
        double totalCost = 0;
        OpAction startAction = findStartAction();
        StringBuilder sb = new StringBuilder();
        OpActionInterface tmp = startAction;
        Set<String> visited = new HashSet<>();
        List<OpActionInterface> notInList = new ArrayList<>();
        List<OpAction> localActionsList = getActions();
        if (null != localActionsList) {
            int maxCount = localActionsList.size();
            int count = 0;
            while (tmp != null && tmp.getOpActionType() != END) {
                count++;
                if (count >= maxCount) {
                    break;
                }
                if (tmp != startAction) {
                    sb.append(" -> ");
                }
                if (tmp instanceof OpAction) {
                    boolean inList = false;
                    for (OpAction action : localActionsList) {
                        if (action == tmp) {
                            inList = true;
                            break;
                        }
                    }
                    if (!inList) {
                        notInList.add(tmp);
                    }
                    OpAction actionTmp = (OpAction) tmp;
                    visited.add(actionTmp.getName());
                    if (actionTmp.getNext() == null) {
                        sb.append(" -> ");
                        tmp = null;
                        break;
                    }
                    totalCost += actionTmp.cost(this);
                    sb.append(actionTmp.getName());
                    OpActionInterface effNext = actionTmp.effectiveNext(true);
                    if (null != effNext && effNext != actionTmp.getNext()) {
                        sb.append("(effectiveNext=");
                        sb.append(effNext.getName());
                        sb.append(")");
                    }
                    sb.append(String.format("(%.2f)", actionTmp.distance(true)));
                } else {
                    sb.append(tmp.getOpActionType());
                }
                tmp = tmp.getNext();
            }

            sb.append(": ");
            sb.append(totalCost);
            if (notInList.size() > 0) {
                sb.append(" notInList(").append(notInList).append(")");
            }
            double recheckCost = 0;
            count = 0;
            for (OpAction action : localActionsList) {
                count++;
                if (count >= maxCount) {
                    break;
                }
                if (action.getNext() == null) {
                    sb.append(" NULL_NEXT");
                    if (!visited.contains(action.getName())) {
                        sb.append(" NOT_VISITED(").append(action.getName()).append(") ");
                    }
                    continue;
                }
                recheckCost += action.cost(this);
                if (!visited.contains(action.getName())) {
                    sb.append(" NOT_VISITED(").append(action.getName()).append(") ");
                }
            }
            if (visited.size() != localActionsList.size()) {
                sb.append(" visitedSize(").append(visited.size()).append(") ");
            }
            if (Math.abs(recheckCost - totalCost) > 1e-6) {
                sb.append(" recheckCost(").append(recheckCost).append(") ");
            }
        }
        String ret = sb.toString();
        this.asString = ret;
        return ret;
    }

    public List<OpAction> getOrderedList(boolean quiet) {
        List<OpAction> l = new ArrayList<>();
        OpAction startAction = findStartAction();
        if (null == startAction) {
            throw new IllegalStateException("findStartAction returned null");
        }
        l.add(startAction);
        OpActionInterface tmp = startAction;
        while (null != tmp) {
            tmp = tmp.getNext();
            if (!(tmp instanceof OpAction)) {
                return l;
            }
            OpAction tmpAction = (OpAction) tmp;
            if (null == tmp) {
                return l;
            }
            if (tmpAction.getOpActionType() == END) {
                return l;
            }
            if (l.contains(tmpAction)) {
                if (quiet) {
                    return l;
                }
                throw new IllegalStateException("loop found: tmp=" + tmp + ",l=" + l);
            }
            l.add(tmpAction);
        }
        return l;
    }

    public List<OpAction> getEffectiveOrderedList(boolean quiet) {
        List<OpAction> l = new ArrayList<>();
        try {
            OpAction startAction = findStartAction();
            if (null == startAction) {
                throw new IllegalStateException("findStartAction returned null");
            }
            l.add(startAction);
            OpActionInterface tmp = startAction;
            while (null != tmp) {
                tmp = tmp.effectiveNext(quiet);
                if (!(tmp instanceof OpAction)) {
                    return l;
                }
                OpAction tmpAction = (OpAction) tmp;
                if (null == tmp) {
                    return l;
                }
                if (tmpAction.getOpActionType() == END) {
                    return l;
                }
                if (l.contains(tmpAction)) {
                    if (quiet) {
                        return l;
                    }
                    throw new IllegalStateException("loop found: tmp=" + tmp);
                }
                l.add(tmpAction);
            }
        } catch (Exception illegalStateException) {
            System.err.println("actions = " + actions);
            throw new IllegalStateException("l =" + l, illegalStateException);
        }
        return l;
    }

    @Nullable
    public OpAction findStartAction() {
        OpAction startAction = null;
        List<OpAction> l = actions;
        if (null != l) {
            for (OpAction action : l) {
                if (action.getOpActionType() == START) {
                    startAction = action;
                }
            }
        }
        return startAction;
    }

}
