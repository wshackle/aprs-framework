/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
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
        original.checkActionList();
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
        Map<OpActionInterface, OpActionInterface> origToNewMap = new IdentityHashMap<>();
        Map<OpActionInterface, OpActionInterface> newToOrigMap = new IdentityHashMap<>();
        origToNewMap.put(original.getEndAction(), newPlan.getEndAction());
        newToOrigMap.put(newPlan.getEndAction(),original.getEndAction());
        if (null != origActions) {
            List<OpAction> newActions = new ArrayList<>();
            for (OpAction origAction : origActions) {
                OpAction newAction = new OpAction(
                        origAction.getName(),
                        origAction.getLocation().x,
                        origAction.getLocation().y,
                        origAction.getOpActionType(),
                        origAction.getPartType(),
                        origAction.isRequired()
                );
                newActions.add(newAction);
                if(null != origToNewMap.put(origAction, newAction)) {
                    throw new IllegalStateException("origAction="+origAction);
                }
                if(null != newToOrigMap.put(newAction,origAction)) {
                     throw new IllegalStateException("newAction="+newAction);
                }
            }
            for (OpAction action : newActions) {
                action.addPossibleNextActions(newActions);
            }
            for (OpAction origAction : origActions) {
                OpActionInterface origNextAction = origAction.getNext();
                if (null != origNextAction) {
                    OpAction newAction = (OpAction) origToNewMap.get(origAction);
                    if (null == newAction) {
                        throw new IllegalStateException("actionMap.get(" + origAction + ") returned null");
                    }
                    OpActionInterface nxtAction = origToNewMap.get(origNextAction);
                    if (null == nxtAction) {
                        throw new IllegalStateException("actionMap.get(" + origNextAction + ") returned null");
                    }
                    newAction.setNext(nxtAction);
                }
            }
            newPlan.setActions(newActions);
        }
        newPlan.checkActionList();
//        System.out.println("original = " + original);
//        System.out.println("newPlan = " + newPlan);
        return newPlan;
    }

}
