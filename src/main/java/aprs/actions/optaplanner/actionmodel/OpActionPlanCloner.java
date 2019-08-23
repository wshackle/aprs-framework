/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import crcl.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpActionPlanCloner implements SolutionCloner<OpActionPlan> {

    @Override
    public OpActionPlan cloneSolution(OpActionPlan originalPlan) {
        originalPlan.checkActionList();
        OpActionPlan newPlan = new OpActionPlan();
        newPlan.setAccelleration(originalPlan.getAccelleration());
        newPlan.setMaxSpeed(originalPlan.getMaxSpeed());
        newPlan.setStartEndMaxSpeed(originalPlan.getStartEndMaxSpeed());
        newPlan.setUseDistForCost(originalPlan.isUseDistForCost());
        newPlan.setUseStartEndCost(originalPlan.isUseStartEndCost());
        newPlan.getEndAction().getLocation().x = originalPlan.getEndAction().getLocation().x;
        newPlan.getEndAction().getLocation().y = originalPlan.getEndAction().getLocation().y;
        HardSoftLongScore hardSoftLongScore = originalPlan.getScore();
        if (null != hardSoftLongScore) {
            newPlan.setScore(hardSoftLongScore);
        }
        List<OpAction> origActions = originalPlan.getActions();
        Map<OpActionInterface, OpActionInterface> origToNewMap = new IdentityHashMap<>();
        Map<OpActionInterface, OpActionInterface> newToOrigMap = new IdentityHashMap<>();
        origToNewMap.put(originalPlan.getEndAction(), newPlan.getEndAction());
        newToOrigMap.put(newPlan.getEndAction(), originalPlan.getEndAction());
        if (null != origActions) {
            List<OpAction> newActions = new ArrayList<>();
            for (OpAction origAction : origActions) {
                OpAction newAction = new OpAction(
                        origAction.getOrigId(),
                        origAction.getName(),
                        origAction.getLocation().x,
                        origAction.getLocation().y,
                        origAction.getOpActionType(),
                        origAction.getPartType(),
                        origAction.isRequired()
                );
                newActions.add(newAction);
                if (null != origToNewMap.put(origAction, newAction)) {
                    throw new IllegalStateException("origAction=" + origAction);
                }
                if (null != newToOrigMap.put(newAction, origAction)) {
                    throw new IllegalStateException("newAction=" + newAction);
                }
            }
            List<OpActionInterface> newAndEndActions = new ArrayList<>(newActions);
            newAndEndActions.addAll(newPlan.getEndActions());
            for (OpAction action : newActions) {
                action.addPossibleNextActions(newAndEndActions);
            }
            for (OpAction origAction : origActions) {
                OpActionInterface origNextAction = origAction.getNext();
                if (null != origNextAction) {
                    boolean fieldModified = origAction.getSetNextValue() != origNextAction;
                    if (fieldModified) {
                        boolean possible = origAction.getPossibleNextActions().contains(origNextAction);
//                        boolean end = originalPlan.getEndActions().contains(origNextAction);
                        if (!possible /* && !end */) {
                            throw new RuntimeException("fieldModified to impossible");
                        }
                    }
                    final OpAction.CheckNextInfoPair checkNextInfoPair = new OpAction.CheckNextInfoPair(new OpAction.CheckNextInfo(origAction), new OpAction.CheckNextInfo(origNextAction));
                    if (!OpAction.checkNextActionInfoPair(checkNextInfoPair)) {
                        System.err.println("setNextTrace = " + Utils.traceToString(origAction.getSetNextTrace()));
                        System.err.println("setNextInfoPair = " + origAction.getSetNextInfoPair());
                        throw new IllegalStateException("origAction=" + origAction.getName() + " has next set to " + origNextAction.shortString() + " : possibles=" + origAction.getPossibleNextActions());
                    }

                    OpAction newAction = (OpAction) origToNewMap.get(origAction);
                    if (null == newAction) {
                        throw new IllegalStateException("origToNewMap.get(" + origAction + ") returned null");
                    }
                    OpActionInterface nxtAction = origToNewMap.get(origNextAction);
                    if (null == nxtAction) {
                        System.err.println("origToNewMap=" + origToNewMap);
                        System.err.println("origToNewMap.values()=" + origToNewMap.values());
                        throw new IllegalStateException("origToNewMap.get(" + origNextAction + ") returned null");
                    }
                    try {
                        newAction.setNext(nxtAction);
                    } catch (Exception exception) {
                        Logger.getLogger(OpAction.class.getName()).log(Level.SEVERE, "origAction=" + origAction + ",\n origNextAction=" + origNextAction + ",\n newAction=" + newAction + ", \nnxtAction=" + nxtAction, exception);
                        if (exception instanceof RuntimeException) {
                            throw (RuntimeException) exception;
                        } else {
                            throw new RuntimeException(exception);
                        }
                    }
                }
            }
            if (newActions.size() != origActions.size()) {
                throw new IllegalStateException("newActions.size() != origActions.size() : " + newActions.size() + " != " + origActions.size());
            }
            newPlan.setActions(Collections.unmodifiableList(new ArrayList<>(newActions)));
        }
        newPlan.checkActionList();
//        println("original = " + original);
//        println("newPlan = " + newPlan);
        return newPlan;
    }

}
