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
import static aprs.framework.optaplanner.actionmodel.OpActionType.START;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private @Nullable List<OpAction> actions;

    @Nullable public List<OpAction> getActions() {
        return actions;
    }

    public List<OpAction> orderedActions() {
        List<OpAction> orderedActions = new ArrayList<>();
        OpAction start = findStartAction();
        if (null != start) {
            orderedActions.add(start);
            OpActionInterface nxt = start.getNext();
            while (nxt instanceof OpAction) {
                OpAction nxtAction = (OpAction) nxt;
                orderedActions.add(nxtAction);
                nxt = nxtAction.getNext();
            }
        }
        return orderedActions;
    }

    public void setActions(List<OpAction> actions) {
        this.actions = actions;
    }

    public void initNextActions() {

        List<OpAction> origActions = actions;
        if (null == origActions) {
            throw new IllegalStateException("actions not initialized");
        }
        MutableMultimap<String, OpAction> multimapWithList
                = Lists.mutable.ofAll(origActions)
                        .select(a -> a.getActionType() == PICKUP || a.getActionType() == DROPOFF)
                        .groupBy(a -> a.getPartType());
        for (String partType : multimapWithList.keySet()) {
            MutableCollection<OpAction> theseActions = multimapWithList.get(partType);
            long pickupCount = theseActions.count(a -> a.getActionType() == PICKUP);
            long dropoffCount = theseActions.count(a -> a.getActionType() == DROPOFF);
            for (long j = dropoffCount; j < pickupCount; j++) {
                origActions.add(new OpAction("fake_dropoff_" + partType + "_" + j, 0, 0, FAKE_DROPOFF, partType));
            }
        }
        List<OpActionInterface> unusedActions = new ArrayList<>(origActions);
        unusedActions.addAll(endActions);
        List<OpActionInterface> allActions = new ArrayList<>(unusedActions);
        OpAction startAction = findStartAction();
        for (OpAction act : origActions) {
            act.addPossibleNextActions(allActions);
        }
        List<OpAction> newActions = new ArrayList<>();
        if (null != startAction) {
            newActions.add(startAction);
            unusedActions.remove(startAction);
        }
        while (startAction != null) {
            OpAction action = startAction;

            startAction = null;
            for (OpActionInterface nxtAction : action.getPossibleNextActions()) {
                if (unusedActions.contains(nxtAction)) {
                    unusedActions.remove(nxtAction);

                    action.setNext(nxtAction);
                    if (nxtAction.getActionType() != END && nxtAction instanceof OpAction) {
                        newActions.add((OpAction) nxtAction);
                        startAction = (OpAction) nxtAction;
                    }
                    break;
                }
            }
        }
        actions = newActions;
        if (unusedActions.size() > 0) {
            System.out.println("unusedActions = " + unusedActions);
            System.out.println("actions = " + actions);
        }
    }

    private @Nullable HardSoftLongScore score;

    @Nullable @PlanningScore
    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
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
            while (tmp != null && tmp.getActionType() != END) {
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
                    totalCost += actionTmp.cost();
                    sb.append(actionTmp.getName());
                    OpActionInterface effNext = actionTmp.effectiveNext();
                    if (null != effNext && effNext != actionTmp.getNext()) {
                        sb.append("(effectiveNext=");
                        sb.append(effNext.getName());
                        sb.append(")");
                    }
                    sb.append(String.format("(%.2f)", actionTmp.cost()));
                } else {
                    sb.append(tmp.getActionType());
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
                recheckCost += action.cost();
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
        return sb.toString();
    }

    @Nullable public OpAction findStartAction() {
        OpAction startAction = null;
        List<OpAction> l = actions;
        if (null != l) {
            for (OpAction action : l) {
                if (action.getActionType() == START) {
                    startAction = action;
                }
            }
        }
        return startAction;
    }

}
