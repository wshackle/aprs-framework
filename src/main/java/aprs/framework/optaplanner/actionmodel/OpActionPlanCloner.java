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

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpActionPlanCloner implements SolutionCloner<OpActionPlan> {

    @Override
    public OpActionPlan cloneSolution(OpActionPlan original) {
        OpActionPlan newPlan = new OpActionPlan();
        newPlan.setEndAction(original.getEndAction());
        newPlan.setEndActions(original.getEndActions());
        newPlan.setScore(original.getScore());
        List<OpAction> origActions = original.getActions();
        List<OpAction> newActions = new ArrayList<>();
        Map<String, OpActionInterface> actionMap = new HashMap<>();
        actionMap.put(newPlan.getEndAction().getName(), newPlan.getEndAction());
        for (OpAction origAction : origActions) {
            OpAction newAction = new OpAction(
                    origAction.getName(),
                    origAction.getLocation().x,
                    origAction.getLocation().y,
                    origAction.getActionType(),
                    origAction.getPartType()
            );
            newActions.add(newAction);
            actionMap.put(newAction.getName(), newAction);
        }
        for(OpAction action : newActions) {
            action.addPossibleNextActions(newActions);
        }
        for (OpAction origAction : origActions) {
            OpActionInterface origNextAction = origAction.getNext();
            if (null != origNextAction) {
                OpAction newAction = (OpAction) actionMap.get(origAction.getName());
                OpActionInterface nxtAction = actionMap.get(origNextAction.getName());
//                if(!newAction.checkNextAction(nxtAction)) {
//                    System.out.println("origAction = " + origAction);
//                    System.out.println("origNextAction = " + origNextAction);
//                    System.out.println("origAction.getPossibleNextActions = " + origAction.getPossibleNextActions());
//                    System.out.println("newAction = " + newAction);
//                    System.out.println("nxtAction = " + nxtAction);
//                }
                newAction.setNext(nxtAction);
            } else {
//                System.out.println("origActon = " + origAction);
            }
        }
        
        newPlan.setActions(newActions);
//        System.out.println("original = " + original);
//        System.out.println("newPlan = " + newPlan);
        return newPlan;
    }

}
