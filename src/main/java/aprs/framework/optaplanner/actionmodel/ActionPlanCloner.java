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
public class ActionPlanCloner implements SolutionCloner<ActionPlan> {

    @Override
    public ActionPlan cloneSolution(ActionPlan original) {
        ActionPlan newPlan = new ActionPlan();
        newPlan.setEndAction(original.getEndAction());
        newPlan.setEndActions(original.getEndActions());
        newPlan.setScore(original.getScore());
        List<Action> origActions = original.getActions();
        List<Action> newActions = new ArrayList<>();
        Map<String, ActionInterface> actionMap = new HashMap<>();
        actionMap.put(newPlan.getEndAction().getName(), newPlan.getEndAction());
        for (Action origAction : origActions) {
            Action newAction = new Action(
                    origAction.getName(),
                    origAction.getLocation().x,
                    origAction.getLocation().y,
                    origAction.getActionType(),
                    origAction.getPartType()
            );
            newActions.add(newAction);
            actionMap.put(newAction.getName(), newAction);
        }
        for(Action action : newActions) {
            action.addPossibleNextActions(newActions);
        }
        for (Action origAction : origActions) {
            ActionInterface origNextAction = origAction.getNext();
            if (null != origNextAction) {
                Action newAction = (Action) actionMap.get(origAction.getName());
                ActionInterface nxtAction = actionMap.get(origNextAction.getName());
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
