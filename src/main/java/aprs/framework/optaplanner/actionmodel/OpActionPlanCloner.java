/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpActionPlanCloner implements SolutionCloner<OpActionPlan> {

    @Override
    public OpActionPlan cloneSolution(OpActionPlan original) {
        OpActionPlan newPlan = new OpActionPlan();
        newPlan.setAccelleration(original.getAccelleration());
        newPlan.setMaxSpeed(original.getMaxSpeed());
        newPlan.setStartEndMaxSpeed(original.getStartEndMaxSpeed());
        newPlan.setUseDistForCost(original.isUseDistForCost());
        newPlan.setEndAction(original.getEndAction());
        newPlan.setEndActions(original.getEndActions());
        HardSoftLongScore hardSoftLongScore = original.getScore();
        if (null != hardSoftLongScore) {
            newPlan.setScore(hardSoftLongScore);
        }
        List<OpAction> origActions = original.getActions();
        Map<String, OpActionInterface> actionMap = new HashMap<>();
        actionMap.put(newPlan.getEndAction().getName(), newPlan.getEndAction());
        if (null != origActions) {
            List<OpAction> newActions = new ArrayList<>();
            for (OpAction origAction : origActions) {
                OpAction newAction = new OpAction(
                        origAction.getName(),
                        origAction.getLocation().x,
                        origAction.getLocation().y,
                        origAction.getActionType(),
                        origAction.getPartType(),
                        origAction.isRequired()
                );
                newActions.add(newAction);
                actionMap.put(newAction.getName(), newAction);
            }
            for (OpAction action : newActions) {
                action.addPossibleNextActions(newActions);
            }
            for (OpAction origAction : origActions) {
                OpActionInterface origNextAction = origAction.getNext();
                if (null != origNextAction) {
                    OpAction newAction = (OpAction) actionMap.get(origAction.getName());
                    if (null == newAction) {
                        throw new IllegalStateException("actionMap.get(" + origAction.getName() + ") returned null");
                    }
                    OpActionInterface nxtAction = actionMap.get(origNextAction.getName());
                    if (null == nxtAction) {
                        throw new IllegalStateException("actionMap.get(" + origNextAction.getName() + ") returned null");
                    }
                    newAction.setNext(nxtAction);
                } else {
//                System.out.println("origActon = " + origAction);
                }
            }

            newPlan.setActions(newActions);
        }

//        System.out.println("original = " + original);
//        System.out.println("newPlan = " + newPlan);
        return newPlan;
    }

}
