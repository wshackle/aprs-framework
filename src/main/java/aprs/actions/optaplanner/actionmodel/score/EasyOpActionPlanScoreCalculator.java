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
import aprs.actions.optaplanner.actionmodel.OpActionInterface;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("unused")
public class EasyOpActionPlanScoreCalculator implements EasyScoreCalculator<OpActionPlan,HardSoftLongScore> {

//    private int lastScoreEnds = 0;
//    private int lastScoreNulls = 0;
//    private int lastScoreBadNexts = 0;
//    private int lastStartLength = 0;
//    private int lastScoreRepeats = 0;
    
    final private Map<Integer,Double> costMap = new TreeMap<>();

    public Map<Integer, Double> getCostMap() {
        return costMap;
    }
    
    
    @Override
    public HardSoftLongScore calculateScore(OpActionPlan solution) {
        long costTotal = 0;
        int ends = 0;
        int nulls = 0;
//        int starts = 0;
//        int startlength = 0;
        int badNexts = 0;
//        int skippedKitTrayAction = 0;
        int repeats = 0;
        final List<OpAction> solutionActions = solution.getActions();
//        double accelleration = solution.getAccelleration();
//        double maxSpeed = solution.getMaxSpeed();
        costMap.clear();
        if (null != solutionActions) {
            for (OpAction action : solutionActions) {
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
                final double costInc = act.cost(solution);
                costMap.put(solutionActions.indexOf(act), costInc);
                costTotal +=  (long) (-1000.0 * costInc);
            }
//            lastScoreEnds = ends;
//            lastScoreNulls = nulls;
//            lastScoreBadNexts = badNexts;
//            lastStartLength = startlength;
//            lastScoreRepeats = repeats;
//            assert (startlength == actionsList.size()) :"startLength != actionsList.size()";
            long hardScoreLong = -Math.abs(orderedActionsList.size() - solutionActions.size()) - Math.abs(1 - ends) - 2 * nulls - badNexts - repeats;
//            long softScoreLong = (long) (-1000.0 * costTotal);
            HardSoftLongScore score = HardSoftLongScore.of(hardScoreLong, costTotal);
            return score;
        } else {
            return HardSoftLongScore.of(Long.MIN_VALUE, Long.MIN_VALUE);
        }
    }

}
