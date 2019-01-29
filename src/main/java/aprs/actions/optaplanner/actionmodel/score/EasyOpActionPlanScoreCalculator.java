/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel.score;

import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import static aprs.actions.optaplanner.actionmodel.OpActionType.END;

import java.util.HashSet;
import java.util.Set;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import aprs.actions.optaplanner.actionmodel.OpActionInterface;
import java.util.List;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("unused")
public class EasyOpActionPlanScoreCalculator implements EasyScoreCalculator<OpActionPlan> {

    private int lastScoreEnds = 0;
    private int lastScoreNulls = 0;
    private int lastScoreBadNexts = 0;
    private int lastStartLength = 0;
    private int lastScoreRepeats = 0;
    
    @Override
    public HardSoftLongScore calculateScore(OpActionPlan solution) {
        double costTotal = 0;
        int ends = 0;
        int nulls = 0;
        int starts = 0;
        int startlength = 0;
        int badNexts = 0;
        int skippedKitTrayAction = 0;
        int repeats = 0;
        List<OpAction> actionsList = solution.getActions();
        double accelleration = solution.getAccelleration();
        double maxSpeed = solution.getMaxSpeed();
        if (null != actionsList) {
            for (OpAction action : actionsList) {
                OpActionInterface nextAction = action.getNext();
                if (nextAction == null) {
                    nulls++;
                } else if (nextAction.getOpActionType() == END) {
                    ends++;
                } else if (!action.checkNextAction(nextAction)) {
                    badNexts++;
                }
            }
            OpAction startAction = solution.findStartAction();

            Set<String> orderedVisited = new HashSet<>();
            List<OpAction> orderedActionsList = solution.getOrderedList(false);
            for (OpAction orderedAct : orderedActionsList) {
                String orderedActName = orderedAct.getName();
                if (orderedVisited.contains(orderedActName)) {
                    repeats++;
                }
                orderedVisited.add(orderedActName);
            }
            Set<String> effectiveOrderedVisited = new HashSet<>();
            List<OpAction> effectiveOrderedActionsList = solution.getEffectiveOrderedList(false);
            for (OpAction orderedAct : effectiveOrderedActionsList) {
                String orderedActName = orderedAct.getName();
                if (effectiveOrderedVisited.contains(orderedActName)) {
                    repeats++;
                }
                effectiveOrderedVisited.add(orderedActName);
            }
            for(OpAction act : effectiveOrderedActionsList) {
                costTotal +=  act.cost(solution);
            }
            lastScoreEnds = ends;
            lastScoreNulls = nulls;
            lastScoreBadNexts = badNexts;
            lastStartLength = startlength;
            lastScoreRepeats = repeats;
//            assert (startlength == actionsList.size()) :"startLength != actionsList.size()";
            long hardScoreLong = -Math.abs(orderedActionsList.size() - actionsList.size()) - Math.abs(1 - ends) - 2 * nulls - badNexts - repeats;
            long softScoreLong = (long) (-1000.0 * costTotal);
            HardSoftLongScore score = HardSoftLongScore.of(hardScoreLong, softScoreLong);
            return score;
        } else {
            return HardSoftLongScore.of(Long.MIN_VALUE, Long.MIN_VALUE);
        }
    }

}
