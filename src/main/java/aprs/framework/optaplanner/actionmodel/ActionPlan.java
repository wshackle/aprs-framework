/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import static aprs.framework.optaplanner.actionmodel.ActionType.END;
import static aprs.framework.optaplanner.actionmodel.ActionType.START;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@PlanningSolution(solutionCloner = ActionPlanCloner.class)
public class ActionPlan {

    @ProblemFactProperty
    EndAction endAction = new EndAction();

    public EndAction getEndAction() {
        return endAction;
    }

    public void setEndAction(EndAction endAction) {
        this.endAction = endAction;
    }

    @ProblemFactCollectionProperty
    private List<EndAction> endActions = Collections.singletonList(endAction);

    @ValueRangeProvider(id = "endActions")
    public List<EndAction> getEndActions() {
        return endActions;
    }

    public void setEndActions(List<EndAction> endActions) {
        this.endActions = endActions;
    }
    

    @PlanningEntityCollectionProperty
    private List<Action> actions;

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public void initNextActions() {
        List<ActionInterface> unusedActions = new ArrayList<>(actions);
        unusedActions.addAll(endActions);
        List<ActionInterface> allActions = new ArrayList<>(unusedActions);
        Action startAction = null;
        for (Action action : actions) {
            action.addPossibleNextActions(allActions);
            if (action.getActionType() == START) {
                startAction = action;
            }
        }
        List<Action> newActions = new ArrayList<>();
        if (null != startAction) {
            newActions.add(startAction);
            unusedActions.remove(startAction);
        }
        while (startAction != null) {
            Action action = startAction;

            startAction = null;
            for (ActionInterface nxtAction : action.getPossibleNextActions()) {
                if (unusedActions.contains(nxtAction)) {
                    unusedActions.remove(nxtAction);

                    action.setNext(nxtAction);
                    if (nxtAction.getActionType() != END && nxtAction instanceof Action) {
                        newActions.add((Action) nxtAction);
                        startAction = (Action) nxtAction;
                    } 
                    break;
                }
            }
        }
        actions = newActions;
        System.out.println("unusedActions = " + unusedActions);
        System.out.println("actions = " + actions);
    }

    private HardSoftBigDecimalScore score;

    @PlanningScore
    public HardSoftBigDecimalScore getScore() {
        return score;
    }

    public void setScore(HardSoftBigDecimalScore score) {
        this.score = score;
    }

    @Override
    public String toString() {
        Action startAction = null;
        double totalCost = 0;
        for (Action action : actions) {
            if (action.getActionType() == START) {
                startAction = action;
            }
        }
        StringBuilder sb = new StringBuilder();
        ActionInterface tmp = startAction;
        Set<String> visited = new HashSet<>();
        List<ActionInterface> notInList = new ArrayList<>();
        while (tmp != null && tmp.getActionType() != END) {
            if (tmp != startAction) {
                sb.append(" -> ");
            }
            if (tmp instanceof Action) {
                boolean inList = false;
                for (Action action : actions) {
                    if(action == tmp) {
                        inList = true;
                        break;
                    }
                }
                if(!inList) {
                    notInList.add(tmp);
                }
                Action actionTmp = (Action) tmp;
                visited.add(actionTmp.getName());
                totalCost += actionTmp.cost();
                sb.append(actionTmp.getName());
                sb.append(String.format("(%.2f)", actionTmp.cost()));
            } else {
                sb.append(tmp.getActionType());
            }
            tmp = tmp.getNext();
        }
        sb.append(": ");
        sb.append(totalCost);
        if(notInList.size() > 0) {
            sb.append(" notInList(").append(notInList).append(")");
        }
        double recheckCost = 0;
        for (Action action : this.getActions()) {
            recheckCost += action.cost();
            if (!visited.contains(action.getName())) {
                sb.append(" NOT_VISITED(").append(action.getName()).append(") ");
            }
        }
        if (visited.size() != actions.size()) {
            sb.append(" visitedSize(").append(visited.size()).append(") ");
        }
        if (Math.abs(recheckCost - totalCost) > 1e-6) {
            sb.append(" recheckCost(").append(recheckCost).append(") ");
        }
        return sb.toString();
    }

}
